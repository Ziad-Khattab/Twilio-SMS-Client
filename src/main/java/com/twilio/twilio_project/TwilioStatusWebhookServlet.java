package com.twilio.twilio_project; // Twilio status callback — handles delivery receipt webhooks from Twilio

import com.twilio.security.RequestValidator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

@WebServlet(name = "twilioStatusWebhookServlet", value = "/webhook/sms-status")
public class TwilioStatusWebhookServlet extends HttpServlet {

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

        String messageSid = request.getParameter("MessageSid");
        String messageStatus = request.getParameter("MessageStatus");
        String errorCode = request.getParameter("ErrorCode");

        if (messageSid == null || messageStatus == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            UserRepository.updateSmsStatusBySid(messageSid, messageStatus, errorCode);
            response.setContentType("application/xml");
            response.getWriter().write("<Response></Response>");
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
