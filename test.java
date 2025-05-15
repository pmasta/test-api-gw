@Component
public class BasepathAwareGatewayFilterFactory extends AbstractGatewayFilterFactory<BasepathAwareGatewayFilterFactory.Config> {

    private static final Logger log = LoggerFactory.getLogger(BasepathAwareGatewayFilterFactory.class);
    private final DiscoveryClient discoveryClient;

    public BasepathAwareGatewayFilterFactory(DiscoveryClient discoveryClient) {
        super(Config.class);
        this.discoveryClient = discoveryClient;
    }

    public static class Config {
        private String serviceId;
        private String stripPrefix; // np. "/auth-server"

        public String getServiceId() {
            return serviceId;
        }

        public void setServiceId(String serviceId) {
            this.serviceId = serviceId;
        }

        public String getStripPrefix() {
            return stripPrefix;
        }

        public void setStripPrefix(String stripPrefix) {
            this.stripPrefix = stripPrefix;
        }
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> buildUriWithBasePath(exchange, config).flatMap(uri -> {
            log.info("🔀 Routing to URI: {}", uri);
            exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, uri);
            return chain.filter(exchange);
        });
    }

    private Mono<URI> buildUriWithBasePath(ServerWebExchange exchange, Config config) {
        List<ServiceInstance> instances = discoveryClient.getInstances(config.getServiceId());
        if (instances.isEmpty()) {
            return Mono.error(new IllegalStateException("No instances for service " + config.getServiceId()));
        }

        ServiceInstance instance = instances.get(new Random().nextInt(instances.size()));

        String scheme = Optional.ofNullable(instance.getScheme())
                .orElse(Optional.ofNullable(instance.getMetadata().get("scheme")).orElse("http"));

        String basePath = Optional.ofNullable(instance.getMetadata().get("basepath")).orElse("");
        if (!basePath.isEmpty() && !basePath.startsWith("/")) {
            basePath = "/" + basePath;
        }

        String requestPath = exchange.getRequest().getURI().getRawPath();
        String strippedPath = requestPath;

        if (config.getStripPrefix() != null && !config.getStripPrefix().isBlank()) {
            strippedPath = requestPath.replaceFirst("^" + config.getStripPrefix(), "");
        }

        String fullPath = (basePath + strippedPath).replaceAll("//+", "/");

        URI uri = UriComponentsBuilder.newInstance()
                .scheme(scheme)
                .host(instance.getHost())
                .port(instance.getPort())
                .replacePath(fullPath)
                .query(exchange.getRequest().getURI().getRawQuery())
                .build(true)
                .toUri();

        return Mono.just(uri);
    }
}
