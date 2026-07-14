package com.twilio.twilio_project;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
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

    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
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
            String body = UserRepository.readRequestBody(request);
            JsonObject json = gson.fromJson(body, JsonObject.class);
            String recipient = PhoneUtil.normalize(json.get("recipient").getAsString().trim());
            String message = json.get("message").getAsString().trim();

            CustomerTwilioConfig twilio = UserRepository.findTwilioConfigByUserId(userId);
            if (twilio == null || !twilio.isComplete()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"status\":\"error\",\"message\":\"Twilio credentials missing\"}");
                return;
            }

            String fromPhone = PhoneUtil.normalize(twilio.getSenderId());

            // Dispatch SMS via the official SDK wrapper!
            Message msg = TwilioSmsService.send(
                    twilio.getAccountSid(),
                    twilio.getAuthToken(),
                    fromPhone,
                    recipient,
                    message);

            String status = mapTwilioStatus(msg);
            UserRepository.recordSms(userId, fromPhone, recipient, message, status);

            JsonObject success = new JsonObject();
            success.addProperty("status", "success");
            success.addProperty("smsStatus", status);
            response.getWriter().write(gson.toJson(success));

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"status\":\"error\",\"message\":\"Failed to send: " + e.getMessage() + "\"}");
        }
    }

    private static String mapTwilioStatus(Message msg) {
        String twilioStatus = msg.getStatus().toString().toLowerCase();
        if ("delivered".equals(twilioStatus)) { return "delivered"; }
        if ("failed".equals(twilioStatus) || "undelivered".equals(twilioStatus)) { return "failed"; }
        return "pending";
    }
}
