
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
            return chain.filter(exchange); // kontynuuj bez zmiany
        }

        String serviceId = segments[1]; // pierwszy segment (np. /orders/** → serviceId=orders)
        String newPath = originalPath.substring(serviceId.length() + 1); // usuń /serviceId

        log.info("[GATEWAY] Incoming request → serviceId='{}', newPath='{}'", serviceId, newPath);
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
            log.error("❌ No instances found for service '{}'", serviceId);
            return Mono.error(new IllegalStateException("No available instances"));
        }

        List<ServiceInstance> candidates = instances.stream()
                .filter(i -> !triedInstanceIds.contains(i.getInstanceId()))
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            log.warn("⚠️ All instances of '{}' already tried and failed", serviceId);
            return Mono.error(new IllegalStateException("No more instances to try"));
        }

        ServiceInstance chosen = candidates.get(new Random().nextInt(candidates.size()));
        triedInstanceIds.add(chosen.getInstanceId());

        // Determine scheme
        String scheme = Optional.ofNullable(chosen.getScheme())
                .orElse(Optional.ofNullable(chosen.getMetadata().get("scheme")).orElse("http"));

        // Determine basepath
        String basePath = Optional.ofNullable(chosen.getMetadata().get("basepath"))
                .filter(p -> !p.isBlank())
                .map(p -> p.startsWith("/") ? p : "/" + p)
                .orElse("");

        // Final path: basepath + newPath
        String fullPath = (basePath + newPath).replaceAll("//+", "/");

        URI newUri = UriComponentsBuilder.newInstance()
                .scheme(scheme)
                .host(chosen.getHost())
                .port(chosen.getPort())
                .path(fullPath)
                .query(exchange.getRequest().getURI().getQuery())
                .build(true)
                .toUri();

        log.info("➡️ Routing to instance: [{}:{}], scheme={}, path={}, retries left={}",
                chosen.getHost(), chosen.getPort(), scheme, fullPath, remainingRetries);

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(r -> r.uri(newUri))
                .build();

        return chain.filter(mutatedExchange)
                .onErrorResume(ex -> {
                    log.warn("🔥 Exception from instance {}: {} ({} retries left)", chosen.getInstanceId(), ex.getMessage(), remainingRetries);
                    if (remainingRetries > 0) {
                        return routeWithRetry(exchange, chain, serviceId, newPath, remainingRetries - 1, triedInstanceIds);
                    }
                    return Mono.error(ex);
                })
                .flatMap(voidResp -> {
                    HttpStatus code = mutatedExchange.getResponse().getStatusCode();
                    if (code != null && code.is5xxServerError() && remainingRetries > 0) {
                        log.warn("🔁 Retry: got {} from {}, retrying ({} left)", code, chosen.getInstanceId(), remainingRetries);
                        return mutatedExchange.getResponse().setComplete()
                                .then(routeWithRetry(exchange, chain, serviceId, newPath, remainingRetries - 1, triedInstanceIds));
                    }
                    return Mono.empty();
                });
    }

    @Override
    public int getOrder() {
        return -1; // wykonuje się wcześnie
    }
}
