package pse.trippy.chatservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads the {@code X-User-Id} and {@code X-User-Role} headers injected by the
 * API gateway and populates the Spring Security context so that method-level
 * {@code @PreAuthorize} / HTTP {@code hasRole()} rules work correctly without
 * a full JWT re-validation on every HTTP request.
 *
 * <p>This filter is safe only because all inbound HTTP traffic is routed
 * through the API gateway, which strips client-supplied headers and injects
 * verified ones.  Direct access to the chat-service port is blocked at the
 * network level in production.
 */
@Component
public class GatewayHeaderAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String role   = request.getHeader("X-User-Role");

        if (userId != null && !userId.isBlank() && role != null && !role.isBlank()) {
            List<SimpleGrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
        SecurityContextHolder.clearContext();
    }
}
