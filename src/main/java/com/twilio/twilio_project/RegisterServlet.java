package com.twilio.twilio_project;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.concurrent.TimeUnit;

@WebServlet(name = "registerServlet", value = "/register")
public class RegisterServlet extends HttpServlet {

    static final String SESSION_PENDING_REGISTRATION = "pendingRegistration";
    private static final long VERIFICATION_TTL_MS = TimeUnit.MINUTES.toMillis(10);
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = trim(request.getParameter("username"));
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirm_password");
        String fullName = trim(request.getParameter("full_name"));
        String birthdayRaw = trim(request.getParameter("birthday"));
        String msisdn = PhoneUtil.normalize(trim(request.getParameter("msisdn")));
        String job = trim(request.getParameter("job"));
        String email = trim(request.getParameter("email"));
        String address = trim(request.getParameter("address"));
        String twilioAccountSid = trim(request.getParameter("twilio_account_sid"));
        String twilioAuthToken = request.getParameter("twilio_auth_token");
        String twilioSenderId = PhoneUtil.normalize(trim(request.getParameter("twilio_sender_id")));

        preserveForm(request, username, fullName, birthdayRaw, msisdn, job, email, address,
                twilioAccountSid, twilioSenderId);

        if (isBlank(username) || isBlank(password) || isBlank(confirmPassword) || isBlank(fullName)
                || isBlank(birthdayRaw) || isBlank(msisdn) || isBlank(job) || isBlank(email)
                || isBlank(address) || isBlank(twilioAccountSid) || isBlank(twilioAuthToken)
                || isBlank(twilioSenderId)) {
            request.setAttribute("error", "All fields are required");
            forwardRegister(request, response);
            return;
        }

        if (!password.equals(confirmPassword)) {
            request.setAttribute("error", "Passwords do not match");
            forwardRegister(request, response);
            return;
        }

        if (!email.contains("@")) {
            request.setAttribute("error", "Please enter a valid email address");
            forwardRegister(request, response);
            return;
        }

        Date birthday;
        try {
            birthday = Date.valueOf(LocalDate.parse(birthdayRaw));
        } catch (DateTimeParseException e) {
            request.setAttribute("error", "Please enter a valid birthday");
            forwardRegister(request, response);
            return;
        }

        try {
            if (UserRepository.existsByUsernameEmailOrMsisdn(username, email, msisdn)) {
                request.setAttribute("error", "Username, email, or phone number is already registered");
                forwardRegister(request, response);
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("error", "Unable to verify account details. Please try again.");
            forwardRegister(request, response);
            return;
        }

        String verificationCode = String.format("%06d", RANDOM.nextInt(1_000_000));
        String smsBody = "Your Twilio SMS verification code is: " + verificationCode;

        try {
            TwilioSmsService.send(twilioAccountSid, twilioAuthToken, twilioSenderId, msisdn, smsBody);
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error",
                    "Failed to send verification SMS. Check your Twilio credentials and sender ID.");
            forwardRegister(request, response);
            return;
        }

        PendingRegistration pending = new PendingRegistration();
        pending.setUsername(username);
        pending.setPasswordHash(PasswordUtil.hash(password));
        pending.setFullName(fullName);
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

        response.sendRedirect(request.getContextPath() + "/verify-msisdn");
    }

    private static void preserveForm(HttpServletRequest request, String username, String fullName,
            String birthday, String msisdn, String job, String email, String address,
            String twilioAccountSid, String twilioSenderId) {
        request.setAttribute("username", username);
        request.setAttribute("full_name", fullName);
        request.setAttribute("birthday", birthday);
        request.setAttribute("msisdn", msisdn);
        request.setAttribute("job", job);
        request.setAttribute("email", email);
        request.setAttribute("address", address);
        request.setAttribute("twilio_account_sid", twilioAccountSid);
        request.setAttribute("twilio_sender_id", twilioSenderId);
    }

    private static void forwardRegister(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("register.jsp").forward(request, response);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isEmpty();
    }

}
