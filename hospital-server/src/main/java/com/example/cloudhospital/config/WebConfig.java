package com.example.cloudhospital.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import com.example.cloudhospital.auth.AuthInterceptor;
import com.example.cloudhospital.auth.TokenRenewalInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AuthInterceptor auth; private final TokenRenewalInterceptor tokenRenewal;
    public WebConfig(AuthInterceptor auth,TokenRenewalInterceptor tokenRenewal){this.auth=auth;this.tokenRenewal=tokenRenewal;}
    @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(auth).addPathPatterns("/api/v1/**");registry.addInterceptor(tokenRenewal).addPathPatterns("/api/v1/**");}
    @Override public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS").allowedHeaders("*").exposedHeaders("X-Auth-Token");
    }
}
