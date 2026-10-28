package com.skillswap.filter;

import com.skillswap.model.User;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebFilter(filterName = "AuthFilter", urlPatterns = {"*.xhtml", "/faces/*"})
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String requestURI = req.getRequestURI();
        String contextPath = req.getContextPath();

        // Allow static resources and JSF resource requests
        if (requestURI.contains("/javax.faces.resource/") ||
            requestURI.contains("/css/") ||
            requestURI.contains("/resources/") ||
            requestURI.endsWith(".css") ||
            requestURI.endsWith(".js") ||
            requestURI.endsWith(".png") ||
            requestURI.endsWith(".jpg") ||
            requestURI.endsWith(".svg") ||
            requestURI.endsWith(".ico")) {
            chain.doFilter(request, response);
            return;
        }

        // Identify current session and user
        HttpSession session = req.getSession(false);
        User currentUser = null;
        if (session != null) {
            currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null) {
                // Also check if stored inside userBean session bean
                Object ubObj = session.getAttribute("userBean");
                if (ubObj instanceof com.skillswap.bean.UserBean) {
                    currentUser = ((com.skillswap.bean.UserBean) ubObj).getCurrentUser();
                }
            }
        }

        boolean isLoggedIn = (currentUser != null);
        boolean isAdmin = isLoggedIn && currentUser.isAdmin();

        // Check page category
        boolean isLoginPage = requestURI.endsWith("/login.xhtml");
        boolean isRegisterPage = requestURI.endsWith("/register.xhtml");
        boolean isIndexPage = requestURI.endsWith("/index.xhtml") || requestURI.equals(contextPath + "/") || requestURI.equals(contextPath);
        boolean isAdminPage = requestURI.contains("/admin_");

        // Prevent browser caching on authenticated pages to avoid back-button leakage after logout
        if (isLoggedIn) {
            res.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            res.setHeader("Pragma", "no-cache");
            res.setDateHeader("Expires", 0);
        }

        // 1. Admin Pages Access Control: Must be logged in AND have ADMIN role
        if (isAdminPage) {
            if (!isLoggedIn) {
                res.sendRedirect(contextPath + "/login.xhtml");
                return;
            } else if (!isAdmin) {
                // Student trying to access admin pages -> Redirect to student dashboard
                res.sendRedirect(contextPath + "/dashboard.xhtml");
                return;
            }
        }

        // 2. Protected Student Pages Access Control
        boolean isPublicPage = isIndexPage || isLoginPage || isRegisterPage || requestURI.endsWith("/search.xhtml");
        if (!isPublicPage && !isAdminPage) {
            if (!isLoggedIn) {
                res.sendRedirect(contextPath + "/login.xhtml");
                return;
            }
        }

        // 3. If logged-in user visits login or register with logout/switch parameter, invalidate session
        if (isLoggedIn && (isLoginPage || isRegisterPage)) {
            if ("true".equalsIgnoreCase(req.getParameter("logout")) || "switch".equalsIgnoreCase(req.getParameter("action"))) {
                if (session != null) {
                    session.invalidate();
                }
                chain.doFilter(request, response);
                return;
            }
            if (isAdmin) {
                res.sendRedirect(contextPath + "/admin_dashboard.xhtml");
                return;
            } else {
                res.sendRedirect(contextPath + "/dashboard.xhtml");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}
