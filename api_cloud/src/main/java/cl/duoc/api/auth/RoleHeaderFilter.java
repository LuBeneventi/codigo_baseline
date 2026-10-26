package cl.duoc.api.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// BASELINE-VULN [SEC-REQ-005]: el rol lo declara el cliente, no se saca del token
@Component
public class RoleHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        boolean isProtectedAction = ("POST".equalsIgnoreCase(method) && "/api/catalog/products".equals(path))
                || ("PUT".equalsIgnoreCase(method) && path.matches("^/api/orders/[^/]+/status/?$"));

        if (isProtectedAction) {
            // BASELINE-VULN [SEC-REQ-005]: el rol lo declara el cliente, no se saca del token
            String roleHeader = request.getHeader("X-User-Role");
            if (roleHeader == null || !"ADMIN".equalsIgnoreCase(roleHeader.trim())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Acceso denegado: se requiere rol ADMIN en cabecera X-User-Role\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
