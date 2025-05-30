@Component
@Order(-1) 
public class AuthenticationFilter implements GlobalFilter {

    @Value("${scp.http.security.authenticated:}")
    private List<String> authenticatedEndpoints;

    private final ScpAuthServerClient scpAuthServerClient;

    public AuthenticationFilter(ScpAuthServerClient scpAuthServerClient) {
        this.scpAuthServerClient = scpAuthServerClient;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        boolean secured = authenticatedEndpoints.stream().anyMatch(path::startsWith);
        if (!secured) {
            return chain.filter(exchange);
        }

        List<String> authHeaders = exchange.getRequest().getHeaders().getOrEmpty(HttpHeaders.AUTHORIZATION);
        if (authHeaders.isEmpty() || !authHeaders.get(0).startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeaders.get(0).substring(7);

        try {
            CheckedToken checkedToken = scpAuthServerClient.checkToken(new UserToken(token));
            exchange.getRequest().mutate()
                    .header("X-User-Id", checkedToken.getUserId())
                    .build();
        } catch (Exception e) {
            return unauthorized(exchange, "Invalid token");
        }

        return chain.filter(exchange);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        byte[] bytes = ("{\"error\": \"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        exchange.getResponse().getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
