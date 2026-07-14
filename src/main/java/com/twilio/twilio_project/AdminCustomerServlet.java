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
import java.sql.Date;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "adminCustomerServlet", value = "/admin/customer")
public class AdminCustomerServlet extends HttpServlet {

    private final Gson gson = new Gson();

    /**
     * GET loads a specific customer profile.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || !"administrator".equals(session.getAttribute("userRole"))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String idStr = request.getParameter("id");
        if (idStr == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // Support customer deletion triggered by Svelte's GET fetch action
        String action = request.getParameter("action");
        if ("delete".equals(action)) {
            try {
                int id = Integer.parseInt(idStr.trim());
                UserRepository.deleteUserById(id);
                response.getWriter().write("{\"status\":\"success\"}");
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"status\":\"error\",\"message\":\"Deletion failure\"}");
            }
            return;
        }

        try {
            int id = Integer.parseInt(idStr.trim());
            Map<String, String> profile = UserRepository.getUserProfile(id);
            if (profile != null) {
                JsonObject data = new JsonObject();
                data.addProperty("status", "success");
                data.add("custProfile", gson.toJsonTree(profile));
                response.getWriter().write(gson.toJson(data));
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * POST handles CRUD commits (Add, Edit, Delete).
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || !"administrator".equals(session.getAttribute("userRole"))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            String body = UserRepository.readRequestBody(request);
            JsonObject json = gson.fromJson(body, JsonObject.class);
            if (json == null || !json.has("actionType")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"status\":\"error\",\"message\":\"Missing actionType\"}");
                return;
            }
            String action = json.get("actionType").getAsString();

            // Handle DELETE action
            if ("delete".equals(action)) {
                int id = json.has("customerId") ? json.get("customerId").getAsInt() : 0;
                if (id <= 0) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("{\"status\":\"error\",\"message\":\"Invalid customer ID\"}");
                    return;
                }
                UserRepository.deleteUserById(id);
                response.getWriter().write("{\"status\":\"success\"}");
                return;
            }

            // Gather profile variables
            String username = json.has("username") ? json.get("username").getAsString().trim() : "";
            String fullName = json.has("fullName") ? json.get("fullName").getAsString().trim() : "";
            String birthdayRaw = json.has("birthday") ? json.get("birthday").getAsString().trim() : "";
            String msisdn = json.has("msisdn") ? PhoneUtil.normalize(json.get("msisdn").getAsString().trim()) : "";
            String job = json.has("job") ? json.get("job").getAsString().trim() : "";
            String email = json.has("email") ? json.get("email").getAsString().trim() : "";
            String address = json.has("address") ? json.get("address").getAsString().trim() : "";
            String twilioSid = json.has("twilioSid") ? json.get("twilioSid").getAsString().trim() : "";
            String twilioSender = json.has("twilioSender") ? PhoneUtil.normalize(json.get("twilioSender").getAsString().trim()) : "";

            String smsProvider = json.has("smsProvider") ? json.get("smsProvider").getAsString().trim().toUpperCase() : "";
            String smppHost = json.has("smppHost") ? json.get("smppHost").getAsString().trim() : "";
            String smppPort = json.has("smppPort") ? json.get("smppPort").getAsString().trim() : "";
            String smppSystemId = json.has("smppSystemId") ? json.get("smppSystemId").getAsString().trim() : "";
            String smppPassword = json.has("smppPassword") ? json.get("smppPassword").getAsString() : "";
            String smppAddressRange = json.has("smppAddressRange") ? json.get("smppAddressRange").getAsString().trim() : "";

            // Validate MSISDN format (E.164)
            if (msisdn.isEmpty() && "create".equals(action)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"status\":\"error\",\"message\":\"MSISDN is required for new customers.\"}");
                return;
            }
            if (!msisdn.isEmpty() && !PhoneUtil.validateE164(msisdn)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"status\":\"error\",\"message\":\"Invalid MSISDN format. Must be E.164 (e.g. +1234567890).\"}");
                return;
            }

            if ("create".equals(action)) {
                if (UserRepository.existsByUsernameEmailOrMsisdn(username, email, msisdn)) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("{\"status\":\"error\",\"message\":\"Username, email, or phone already exists\"}");
                    return;
                }

                String password = json.has("password") ? json.get("password").getAsString() : "";
                String twilioToken = json.has("twilioToken") ? json.get("twilioToken").getAsString() : "";
                String passHash = PasswordUtil.hash(password);
                java.sql.Date birthday = null;
                if (birthdayRaw != null && !birthdayRaw.trim().isEmpty()) {
                    try {
                        birthday = java.sql.Date.valueOf(birthdayRaw);
                    } catch (IllegalArgumentException e) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        response.getWriter().write("{\"status\":\"error\",\"message\":\"Invalid birthday format. Expected YYYY-MM-DD.\"}");
                        return;
                    }
                }

                UserRepository.createCustomerByAdmin(username, passHash, fullName, birthday, msisdn, 
                        job, email, address, twilioSid, twilioToken, twilioSender,
                        smsProvider, smppHost, smppPort, smppSystemId, smppPassword, smppAddressRange);

                response.getWriter().write("{\"status\":\"success\"}");

            } else if ("edit".equals(action)) {
                int userId = json.has("customerId") ? json.get("customerId").getAsInt() : 0;
                if (userId <= 0) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("{\"status\":\"error\",\"message\":\"Invalid customer ID\"}");
                    return;
                }
                
                Map<String, String> profile = new HashMap<>();
                profile.put("fullName", fullName);
                profile.put("birthday", birthdayRaw);
                profile.put("msisdn", msisdn);
                profile.put("job", job);
                profile.put("email", email);
                profile.put("address", address);
                if (!twilioSid.isEmpty()) {
                    profile.put("twilioSid", twilioSid);
                }
                if (!twilioSender.isEmpty()) {
                    profile.put("twilioSender", twilioSender);
                }

                if (json.has("password") && !json.get("password").getAsString().trim().isEmpty()) {
                    profile.put("passwordHash", PasswordUtil.hash(json.get("password").getAsString()));
                }
                if (json.has("twilioToken") && !json.get("twilioToken").getAsString().trim().isEmpty()) {
                    profile.put("twilioToken", json.get("twilioToken").getAsString());
                }

                if (!smsProvider.isEmpty()) profile.put("smsProvider", smsProvider);
                if (!smppHost.isEmpty()) profile.put("smppHost", smppHost);
                if (!smppPort.isEmpty()) profile.put("smppPort", smppPort);
                if (!smppSystemId.isEmpty()) profile.put("smppSystemId", smppSystemId);
                if (!smppPassword.isEmpty()) profile.put("smppPassword", smppPassword);
                if (!smppAddressRange.isEmpty()) profile.put("smppAddressRange", smppAddressRange);

                UserRepository.updateUserProfile(userId, profile);
                response.getWriter().write("{\"status\":\"success\"}");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"status\":\"error\",\"message\":\"Server compilation error committing records\"}");
        }
    }
}
