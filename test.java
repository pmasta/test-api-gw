import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class RetryingEurekaRouterGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RetryingEurekaRouterGlobalFilter.class);

    private final DiscoveryClient discoveryClient;
    private static final int MAX_RETRIES = 3;

    public RetryingEurekaRouterGlobalFilter(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        String originalPath = exchange.getRequest().getURI().getPath();
        String[] segments = originalPath.split("/");
        if (segments.length < 2) {
            log.warn("Invalid path, cannot extract serviceId: {}", originalPath);
            return chain.filter(exchange); // nie ruszaj jeśli nie da się wyciągnąć serviceId
        }

        String serviceId = segments[1]; // pierwszy segment to serviceId (np. /service-A/...)

        String newPath = originalPath.substring(serviceId.length() + 1); // obetnij /serviceId
        log.info("Routing request to serviceId '{}' with rewritten path '{}'", serviceId, newPath);

        return routeWithRetry(exchange, chain, serviceId, "/" + newPath, MAX_RETRIES, new HashSet<>());
    }

    private Mono<Void> routeWithRetry(ServerWebExchange exchange,
                                      org.springframework.cloud.gateway.filter.GatewayFilterChain chain,
                                      String serviceId,
                                      String newPath,
                                      int remainingRetries,
                                      Set<String> triedInstanceIds) {

        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);

        if (instances.isEmpty()) {
            log.error("No instances found for service '{}'", serviceId);
            return Mono.error(new IllegalStateException("No available instances"));
        }

        List<ServiceInstance> candidates = instances.stream()
                .filter(i -> !triedInstanceIds.contains(i.getInstanceId()))
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            log.warn("All instances of '{}' already tried and failed", serviceId);
            return Mono.error(new IllegalStateException("No more instances to try"));
        }

        ServiceInstance chosen = candidates.get(new Random().nextInt(candidates.size()));
        triedInstanceIds.add(chosen.getInstanceId());

        log.info("Routing to instance: {}:{} (id: {}, metadata: {})",
                chosen.getHost(), chosen.getPort(), chosen.getInstanceId(), chosen.getMetadata());

        URI newUri = UriComponentsBuilder.newInstance()
                .scheme("http")
                .host(chosen.getHost())
                .port(chosen.getPort())
                .path(newPath)
                .query(exchange.getRequest().getURI().getQuery())
                .build(true).toUri();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(r -> r.uri(newUri))
                .build();

        return chain.filter(mutatedExchange)
                .onErrorResume(ex -> {
                    log.warn("Error routing to instance {} ({} retries left): {}", chosen.getInstanceId(), remainingRetries, ex.getMessage());
                    if (remainingRetries > 0) {
                        return routeWithRetry(exchange, chain, serviceId, newPath, remainingRetries - 1, triedInstanceIds);
                    }
                    return Mono.error(ex);
                })
                .flatMap(voidResp -> {
                    HttpStatus code = mutatedExchange.getResponse().getStatusCode();
                    if (code != null && code.is5xxServerError() && remainingRetries > 0) {
                        log.warn("Got {} from instance {} — retrying ({} retries left)", code, chosen.getInstanceId(), remainingRetries);
                        return mutatedExchange.getResponse().setComplete()
                                .then(routeWithRetry(exchange, chain, serviceId, newPath, remainingRetries - 1, triedInstanceIds));
                    }
                    return Mono.empty();
                });
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
