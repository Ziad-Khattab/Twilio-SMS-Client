package com.twilio.twilio_project;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "dashboardServlet", value = "/dashboard")
public class DashboardServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect("login.jsp");
            return;
        }
        // At the top of doGet, after the session check:
        String smsSuccess = (String) session.getAttribute("smsSuccess");
        String smsError = (String) session.getAttribute("smsError");
        if (smsSuccess != null) {
            request.setAttribute("smsSuccess", smsSuccess);
            session.removeAttribute("smsSuccess");
        }
        if (smsError != null) {
            request.setAttribute("smsError", smsError);
            session.removeAttribute("smsError");
        }
        int userId = (int) session.getAttribute("userId");
        List<Map<String, Object>> smsHistory = getSmsHistory(userId);
        request.setAttribute("smsHistory", smsHistory);
        request.getRequestDispatcher("dashboard.jsp").forward(request, response);
    }

    private List<Map<String, Object>> getSmsHistory(int userId) {
        List<Map<String, Object>> history = new ArrayList<>();
        String sql = "SELECT to_phone, message, status, sent_at FROM sms_history WHERE user_id = ? ORDER BY sent_at DESC";

        try (Connection conn = DBUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> sms = new HashMap<>();
                    sms.put("recipient", rs.getString("to_phone"));
                    sms.put("message", rs.getString("message"));
                    sms.put("status", rs.getString("status"));
                    sms.put("sentAt", rs.getTimestamp("sent_at"));
                    history.add(sms);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return history;
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect("login");
        }
    }
}