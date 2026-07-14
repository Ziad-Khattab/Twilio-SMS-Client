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
import java.security.SecureRandom;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.concurrent.TimeUnit;

@WebServlet(name = "registerServlet", value = "/register")
public class RegisterServlet extends HttpServlet {

    static final String SESSION_PENDING_REGISTRATION = "pendingRegistration";
    private static final long VERIFICATION_TTL_MS = java.util.concurrent.TimeUnit.MINUTES.toMillis(10);
    private static final SecureRandom RANDOM = new SecureRandom();
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            String body = UserRepository.readRequestBody(request);
            JsonObject json = gson.fromJson(body, JsonObject.class);

            // Fetch inputs
            String username = json.has("username") ? json.get("username").getAsString().trim() : "";
            String password = json.has("password") ? json.get("password").getAsString() : "";
            String fullName = json.has("fullName") ? json.get("fullName").getAsString().trim() : "";
            String birthdayRaw = json.has("birthday") ? json.get("birthday").getAsString().trim() : "";
            String msisdn = json.has("msisdn") ? PhoneUtil.normalize(json.get("msisdn").getAsString().trim()) : "";
            String job = json.has("job") ? json.get("job").getAsString().trim() : "";
            String email = json.has("email") ? json.get("email").getAsString().trim() : "";
            String address = json.has("address") ? json.get("address").getAsString().trim() : "";
            String twilioAccountSid = json.has("twilioSid") ? json.get("twilioSid").getAsString().trim() : "";
            String twilioAuthToken = json.has("twilioToken") ? json.get("twilioToken").getAsString() : "";
            String twilioSenderId = json.has("twilioSender") ? PhoneUtil.normalize(json.get("twilioSender").getAsString().trim()) : "";

            if (username.isEmpty() || password.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"status\":\"error\",\"message\":\"Username and password are required\"}");
                return;
            }

            if (msisdn.isEmpty() || !PhoneUtil.validateE164(msisdn)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"status\":\"error\",\"message\":\"Valid MSISDN (E.164 format) is required\"}");
                return;
            }

            if (UserRepository.existsByUsernameEmailOrMsisdn(username, email, msisdn)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"status\":\"error\",\"message\":\"Account values already registered\"}");
                return;
            }

            // Generate 6-Digit PIN
            String verificationCode = String.format("%06d", RANDOM.nextInt(1_000_000));
            String smsBody = "Your Twilio SMS verification code is: " + verificationCode;

            // Transmit validation PIN via Svelte credentials
            TwilioSmsService.send(twilioAccountSid, twilioAuthToken, twilioSenderId, msisdn, smsBody);

            // Package Pending Registration
            PendingRegistration pending = new PendingRegistration();
            pending.setUsername(username);
            pending.setPasswordHash(PasswordUtil.hash(password));
            pending.setFullName(fullName);
            java.sql.Date birthday = null;
            if (birthdayRaw != null && !birthdayRaw.isEmpty()) {
                try {
                    birthday = java.sql.Date.valueOf(java.time.LocalDate.parse(birthdayRaw));
                } catch (java.time.format.DateTimeParseException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("{\"status\":\"error\",\"message\":\"Invalid birthday format. Expected YYYY-MM-DD.\"}");
                    return;
                }
            }
            pending.setBirthday(birthday);
            pending.setMsisdn(msisdn);
            pending.setJob(job);
            pending.setEmail(email);
            pending.setAddress(address);
            pending.setTwilioAccountSid(twilioAccountSid);
            pending.setTwilioAuthToken(twilioAuthToken);
            pending.setTwilioSenderId(twilioSenderId);
            pending.setVerificationCode(verificationCode);
            pending.setVerificationExpiresAt(System.currentTimeMillis() + VERIFICATION_TTL_MS);

            HttpSession session = request.getSession(true);
            session.setAttribute(SESSION_PENDING_REGISTRATION, pending);

            response.getWriter().write("{\"status\":\"success\",\"message\":\"Verification PIN sent successfully!\"}");

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"status\":\"error\",\"message\":\"Gateway execution error\"}");
        }
    }
}
