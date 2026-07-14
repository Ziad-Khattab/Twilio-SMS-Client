package com.twilio.twilio_project;

import com.twilio.security.RequestValidator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

@WebServlet(name = "twilioWebhookServlet", value = "/webhook/sms")
public class TwilioWebhookServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String signature = request.getHeader("X-Twilio-Signature");
        String authToken = EnvLoader.get("TWILIO_AUTH_TOKEN");

        if (authToken != null && !authToken.isEmpty()) {
            if (signature == null || signature.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("Missing X-Twilio-Signature");
                return;
            }

            String fullUrl = request.getRequestURL().toString();
            if (request.getQueryString() != null) {
                fullUrl += "?" + request.getQueryString();
            }

            java.util.HashMap<String, String> flatParams = new java.util.HashMap<>();
            for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
                if (entry.getValue() != null && entry.getValue().length > 0) {
                    flatParams.put(entry.getKey(), entry.getValue()[0]);
                }
            }
            RequestValidator validator = new RequestValidator(authToken);
            if (!validator.validate(fullUrl, flatParams, signature)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("Invalid webhook signature");
                return;
            }
        }

        String from = request.getParameter("From");
        String to = request.getParameter("To");
        String body = request.getParameter("Body");

        if (from == null || to == null || body == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            int userId = findUserIdBySenderId(PhoneUtil.normalize(to));
            
            if (userId > 0) {
                saveInboundSms(userId, from, to, body);
                
                response.setContentType("application/xml");
                response.getWriter().write("<Response></Response>");
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private int findUserIdBySenderId(String senderId) throws SQLException {
        String sql = "SELECT id FROM users WHERE twilio_sender_id = ? OR msisdn = ? LIMIT 1";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, senderId);
            stmt.setString(2, senderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) { return rs.getInt("id"); }
            }
        }
        return -1;
    }

    private void saveInboundSms(int userId, String from, String to, String body) throws SQLException {
        String sql = "INSERT INTO sms_history (user_id, from_phone, to_phone, message, status, direction) " +
                     "VALUES (?, ?, ?, ?, 'delivered'::message_status, 'inbound'::sms_direction)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, from);
            stmt.setString(3, to);
            stmt.setString(4, body);
            stmt.executeUpdate();
        }
    }
}
