package com.twilio.twilio_project;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@WebServlet(name = "dashboardServlet", value = "/dashboard")
public class DashboardServlet extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"status\":\"error\",\"message\":\"Unauthorized\"}");
            return;
        }

        int userId = (int) session.getAttribute("userId");

        try {
            Map<String, String> profile = UserRepository.getUserProfile(userId);
            List<Map<String, Object>> outboundHistory = UserRepository.findSmsHistoryByUserId(userId);
            List<Map<String, Object>> inboundHistory = UserRepository.findInboundSmsByUserId(userId);

            JsonObject responseData = new JsonObject();
            responseData.addProperty("status", "success");
            responseData.add("profile", gson.toJsonTree(profile));
            responseData.add("outboundHistory", gson.toJsonTree(outboundHistory));
            responseData.add("inboundHistory", gson.toJsonTree(inboundHistory));

            response.getWriter().write(gson.toJson(responseData));

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"status\":\"error\",\"message\":\"Failed to load dashboard data\"}");
        }
    }
}
