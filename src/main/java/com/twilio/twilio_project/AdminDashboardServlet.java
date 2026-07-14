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

@WebServlet(name = "adminDashboardServlet", value = "/admin/dashboard")
public class AdminDashboardServlet extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || !"administrator".equals(session.getAttribute("userRole"))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"status\":\"error\",\"message\":\"Access Denied: Admins Only\"}");
            return;
        }

        try {
            List<Map<String, Object>> customers = UserRepository.findAllCustomers();
            List<Map<String, Object>> stats = UserRepository.getCustomerSmsStats();

            long grandTotalSentSms = stats.stream()
                    .mapToLong(stat -> (long) stat.get("sentCount"))
                    .sum();

            JsonObject payload = new JsonObject();
            payload.addProperty("status", "success");
            payload.addProperty("totalCustomers", customers.size());
            payload.addProperty("totalSentSms", grandTotalSentSms);
            payload.add("customers", gson.toJsonTree(customers));
            payload.add("stats", gson.toJsonTree(stats));

            response.getWriter().write(gson.toJson(payload));

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"status\":\"error\",\"message\":\"Database error loading statistics\"}");
        }
    }
}
