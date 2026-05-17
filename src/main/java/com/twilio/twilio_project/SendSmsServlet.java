package com.twilio.twilio_project;

import com.twilio.rest.api.v2010.account.Message;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
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

        int userId = (int) session.getAttribute("userId");
        String recipient = PhoneUtil.normalize(trim(request.getParameter("recipient")));
        String message = trim(request.getParameter("message"));

        if (isBlank(recipient) || isBlank(message)) {
            session.setAttribute("smsError", "Recipient and message are required");
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        CustomerTwilioConfig twilio;
        try {
            twilio = UserRepository.findTwilioConfigByUserId(userId);
        } catch (SQLException e) {
            e.printStackTrace();
            session.setAttribute("smsError", "Unable to load your Twilio credentials");
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        if (twilio == null || !twilio.isComplete()) {
            session.setAttribute("smsError",
                    "Twilio credentials are missing on your account. Re-register or update your profile.");
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        String fromPhone = PhoneUtil.normalize(twilio.getSenderId());

        try {
            Message msg = TwilioSmsService.send(
                    twilio.getAccountSid(),
                    twilio.getAuthToken(),
                    fromPhone,
                    recipient,
                    message);

            UserRepository.recordSms(userId, fromPhone, recipient, message, mapTwilioStatus(msg));
            session.setAttribute("smsSuccess", "SMS sent successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("smsError", "Failed to send SMS: " + e.getMessage());
        }

        response.sendRedirect(request.getContextPath() + "/dashboard");
    }

    private static String mapTwilioStatus(Message msg) {
        String twilioStatus = msg.getStatus().toString().toLowerCase();
        if ("delivered".equals(twilioStatus)) {
            return "delivered";
        }
        if ("failed".equals(twilioStatus) || "undelivered".equals(twilioStatus)) {
            return "failed";
        }
        return "pending";
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isEmpty();
    }
}
