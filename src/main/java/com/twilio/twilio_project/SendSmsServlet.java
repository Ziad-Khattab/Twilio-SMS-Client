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
            String recipient = json.has("recipient") ? PhoneUtil.normalize(json.get("recipient").getAsString().trim()) : "";
            String message = json.has("message") ? json.get("message").getAsString().trim() : "";

            if (recipient.isEmpty() || message.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"status\":\"error\",\"message\":\"recipient and message are required\"}");
                return;
            }

            SmsResult result = SmsRouter.send(recipient, message, userId);

            if (!result.isSuccess()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"status\":\"error\",\"message\":\"" + result.getError() + "\"}");
                return;
            }

            String fromPhone = "";
            try {
                CustomerTwilioConfig cfg = UserRepository.findTwilioConfigByUserId(userId);
                if (cfg != null && cfg.isComplete()) {
                    fromPhone = PhoneUtil.normalize(cfg.getSenderId());
                }
            } catch (Exception ignored) {}
            String status = result.getMessageId() != null ? "delivered" : "pending";
            String providerRefId = result.getProviderRefId();
            UserRepository.recordSms(userId, fromPhone, recipient, message, status, providerRefId);

            JsonObject success = new JsonObject();
            success.addProperty("status", "success");
            success.addProperty("smsStatus", status);
            response.getWriter().write(gson.toJson(success));

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"status\":\"error\",\"message\":\"Failed to dispatch SMS. Check server logs.\"}");
        }
    }
}
