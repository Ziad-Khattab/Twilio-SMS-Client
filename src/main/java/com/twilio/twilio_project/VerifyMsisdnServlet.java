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

@WebServlet(name = "verifyMsisdnServlet", value = "/verify-msisdn")
public class VerifyMsisdnServlet extends HttpServlet {

    private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();
    private static final long VERIFICATION_TTL_MS = java.util.concurrent.TimeUnit.MINUTES.toMillis(10);
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        PendingRegistration pending = null;
        if (session != null) {
            pending = (PendingRegistration) session.getAttribute(RegisterServlet.SESSION_PENDING_REGISTRATION);
        }

        if (pending == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"status\":\"error\",\"message\":\"No pending registration session found\"}");
            return;
        }

        // Support OTP Resend from Svelte Fetch Request
        String action = request.getParameter("action");
        if ("resend".equals(action)) {
            try {
                String newCode = String.format("%06d", RANDOM.nextInt(1_000_000));
                String smsBody = "Your new Twilio SMS verification code is: " + newCode;

                // Send PIN
                TwilioSmsService.send(
                        pending.getTwilioAccountSid(),
                        pending.getTwilioAuthToken(),
                        pending.getTwilioSenderId(),
                        pending.getMsisdn(),
                        smsBody
                );

                // Update Session state variables reactively
                pending.setVerificationCode(newCode);
                pending.setVerificationExpiresAt(System.currentTimeMillis() + VERIFICATION_TTL_MS);
                session.setAttribute(RegisterServlet.SESSION_PENDING_REGISTRATION, pending);

                response.getWriter().write("{\"status\":\"success\",\"message\":\"A new code has been sent\"}");
            } catch (Exception e) {
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"status\":\"error\",\"message\":\"Failed to resend code\"}");
            }
            return;
        }

        try {
            String body = UserRepository.readRequestBody(request);
            JsonObject json = gson.fromJson(body, JsonObject.class);
            String code = json.get("code").getAsString().trim();

            if (pending.isVerificationExpired()) {
                session.removeAttribute(RegisterServlet.SESSION_PENDING_REGISTRATION);
                response.setStatus(HttpServletResponse.SC_GONE);
                response.getWriter().write("{\"status\":\"error\",\"message\":\"Verification code has expired\"}");
                return;
            }

            if (!pending.getVerificationCode().equals(code)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"status\":\"error\",\"message\":\"Invalid verification code\"}");
                return;
            }

            // Create account!
            UserRepository.createCustomer(pending);
            session.removeAttribute(RegisterServlet.SESSION_PENDING_REGISTRATION);
            response.getWriter().write("{\"status\":\"success\"}");

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"status\":\"error\",\"message\":\"Server write error\"}");
        }
    }
}
