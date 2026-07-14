package com.twilio.twilio_project;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        HttpSession session = request.getSession(false);
        
        // 1. Authenticated Verification Check
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"status\":\"error\",\"message\":\"Unauthorized\"}");
            return;
        }
        
        String requestURI = request.getRequestURI();
        String userRole = (String) session.getAttribute("userRole");
        
        // 2. Privilege Boundary Check
        if (requestURI.startsWith(request.getContextPath() + "/admin/")) {
            if (!"administrator".equals(userRole)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"status\":\"error\",\"message\":\"Access Denied: Admins Only\"}");
                return;
            }
        }
        
        // Pass-through: safe to continue!
        chain.doFilter(req, res);
    }

    @Override
    public void destroy() {}
}
