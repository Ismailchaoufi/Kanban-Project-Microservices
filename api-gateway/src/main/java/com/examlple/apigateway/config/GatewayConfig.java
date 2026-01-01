package com.examlple.apigateway.config;


import com.examlple.apigateway.filter.JwtAuthenticationFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public GatewayConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                // AUTH SERVICE
                .route("auth-service", r -> r.path("/api/v1/auth/**", "/api/v1/users/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://AUTH-SERVICE"))

                // PROJECT SERVICE
                .route("project-service", r -> r.path("/api/v1/projects/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://PROJECT-SERVICE"))

                // TASK SERVICE
                .route("task-service", r -> r.path("/api/v1/tasks/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://TASK-SERVICE"))

                .build();
    }
}
