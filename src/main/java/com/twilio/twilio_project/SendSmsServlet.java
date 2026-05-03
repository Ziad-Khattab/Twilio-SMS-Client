package com.twilio.twilio_project;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@WebServlet(name = "sendSmsServlet", value = "/send-sms")
public class SendSmsServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect("login");
            return;
        }
        String recipient = request.getParameter("recipient");
        String message = request.getParameter("message");
        if (recipient == null || recipient.trim().isEmpty() || message == null || message.trim().isEmpty()) {
            request.setAttribute("smsError", "Recipient and message are required");
            request.getRequestDispatcher("/dashboard").forward(request, response);
            return;
        }

        String ACCOUNT_SID = EnvLoader.get("TWILIO_ACCOUNT_SID");
        String AUTH_TOKEN = EnvLoader.get("TWILIO_AUTH_TOKEN");
        String TWILIO_PHONE = EnvLoader.get("TWILIO_PHONE_NUMBER");

        try {
            Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
            Message msg = Message.creator(
                    new com.twilio.type.PhoneNumber("+201011360055"),
                    new com.twilio.type.PhoneNumber("+13613210804"),
                    message)
                    .create();

            String twilioStatus = msg.getStatus().toString().toLowerCase();
            String dbStatus;
            if (twilioStatus.equals("delivered")) {
                dbStatus = "delivered";
            } else if (twilioStatus.equals("failed") || twilioStatus.equals("undelivered")) {
                dbStatus = "failed";
            } else {
                dbStatus = "pending";
            }

            String sql = "INSERT INTO sms_history (user_id, to_phone, message, status) VALUES (?, ?, ?, ?::message_status)";
            try (Connection conn = DBUtil.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, (int) session.getAttribute("userId"));
                stmt.setString(2, recipient);
                stmt.setString(3, message);
                stmt.setString(4, dbStatus);
                stmt.executeUpdate();
            }
            request.getSession().setAttribute("smsSuccess", "SMS sent successfully!");
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        } catch (Exception e) {
            e.printStackTrace();
            request.getSession().setAttribute("smsError", "Failed to send SMS: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/dashboard");
        }

        response.sendRedirect(request.getContextPath() + "/dashboard");
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

    }
}