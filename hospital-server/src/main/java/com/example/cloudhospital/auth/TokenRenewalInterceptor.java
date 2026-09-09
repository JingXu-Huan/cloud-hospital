package com.example.cloudhospital.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Renews an authenticated browser credential before a state-changing request so the new value can be returned in its response. */
@Component
public class TokenRenewalInterceptor implements HandlerInterceptor {
    private final AuthService auth;

    public TokenRenewalInterceptor(AuthService auth) {
        this.auth = auth;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!isStateChanging(request.getMethod())) return true;
        AuthService.Session session = (AuthService.Session) request.getAttribute("session");
        if (session == null) return true;

        TokenCookie.write(response, auth.renew(session));
        return true;
    }

    private boolean isStateChanging(String method) {
        return "POST".equals(method) || "PUT".equals(method)
                || "PATCH".equals(method) || "DELETE".equals(method);
    }
}
