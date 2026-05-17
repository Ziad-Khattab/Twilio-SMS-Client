package com.twilio.twilio_project;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

@WebServlet(name = "verifyMsisdnServlet", value = "/verify-msisdn")
public class VerifyMsisdnServlet extends HttpServlet {

    private static final long VERIFICATION_TTL_MS = TimeUnit.MINUTES.toMillis(10);
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PendingRegistration pending = getPendingRegistration(request);
        if (pending == null) {
            response.sendRedirect(request.getContextPath() + "/register");
            return;
        }
        request.setAttribute("msisdn", pending.getMsisdn());
        request.getRequestDispatcher("verify-msisdn.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PendingRegistration pending = getPendingRegistration(request);
        if (pending == null) {
            response.sendRedirect(request.getContextPath() + "/register");
            return;
        }

        String action = request.getParameter("action");
        if ("resend".equals(action)) {
            handleResend(request, response, pending);
            return;
        }

        if ("cancel".equals(action)) {
            clearPendingRegistration(request);
            response.sendRedirect(request.getContextPath() + "/register");
            return;
        }

        String code = request.getParameter("code");
        if (code == null || code.trim().isEmpty()) {
            request.setAttribute("error", "Verification code is required");
            request.setAttribute("msisdn", pending.getMsisdn());
            request.getRequestDispatcher("verify-msisdn.jsp").forward(request, response);
            return;
        }

        if (pending.isVerificationExpired()) {
            request.setAttribute("error", "Verification code has expired. Please register again.");
            clearPendingRegistration(request);
            request.getRequestDispatcher("verify-msisdn.jsp").forward(request, response);
            return;
        }

        if (!pending.getVerificationCode().equals(code.trim())) {
            request.setAttribute("error", "Invalid verification code");
            request.setAttribute("msisdn", pending.getMsisdn());
            request.getRequestDispatcher("verify-msisdn.jsp").forward(request, response);
            return;
        }

        try {
            UserRepository.createCustomer(pending);
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("error", "Could not create account. The username, email, or phone may already exist.");
            request.setAttribute("msisdn", pending.getMsisdn());
            request.getRequestDispatcher("verify-msisdn.jsp").forward(request, response);
            return;
        }

        clearPendingRegistration(request);
        request.getSession().setAttribute("registrationMessage",
                "Account created successfully. You can now log in.");
        response.sendRedirect(request.getContextPath() + "/login");
    }

    private void handleResend(HttpServletRequest request, HttpServletResponse response, PendingRegistration pending)
            throws ServletException, IOException {
        String verificationCode = String.format("%06d", RANDOM.nextInt(1_000_000));
        String smsBody = "Your Twilio SMS verification code is: " + verificationCode;

        try {
            TwilioSmsService.send(
                    pending.getTwilioAccountSid(),
                    pending.getTwilioAuthToken(),
                    pending.getTwilioSenderId(),
                    pending.getMsisdn(),
                    smsBody);
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "Failed to resend verification SMS. Please try again.");
            request.setAttribute("msisdn", pending.getMsisdn());
            request.getRequestDispatcher("verify-msisdn.jsp").forward(request, response);
            return;
        }

        pending.setVerificationCode(verificationCode);
        pending.setVerificationExpiresAt(System.currentTimeMillis() + VERIFICATION_TTL_MS);
        request.setAttribute("message", "A new verification code has been sent.");
        request.setAttribute("msisdn", pending.getMsisdn());
        request.getRequestDispatcher("verify-msisdn.jsp").forward(request, response);
    }

    private static PendingRegistration getPendingRegistration(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(RegisterServlet.SESSION_PENDING_REGISTRATION);
        if (value instanceof PendingRegistration) {
            return (PendingRegistration) value;
        }
        return null;
    }

    private static void clearPendingRegistration(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(RegisterServlet.SESSION_PENDING_REGISTRATION);
        }
    }
}
