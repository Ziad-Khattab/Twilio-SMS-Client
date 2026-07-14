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
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "profileServlet", value = "/profile")
public class ProfileServlet extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
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
            Map<String, String> profile = UserRepository.getUserProfile(userId);
            if (profile != null) {
                response.getWriter().write(gson.toJson(profile));
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"status\":\"error\",\"message\":\"Profile not found\"}");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"status\":\"error\",\"message\":\"Server database failure\"}");
        }
    }

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

            Map<String, String> profile = new HashMap<>();
            profile.put("fullName", json.get("fullName").getAsString());
            profile.put("birthday", json.get("birthday").getAsString());
            profile.put("msisdn", json.get("msisdn").getAsString());
            profile.put("job", json.get("job").getAsString());
            profile.put("email", json.get("email").getAsString());
            profile.put("address", json.get("address").getAsString());
            profile.put("twilioSid", json.get("twilioSid").getAsString());
            profile.put("twilioSender", json.get("twilioSender").getAsString());

            if (json.has("password") && !json.get("password").getAsString().trim().isEmpty()) {
                profile.put("passwordHash", PasswordUtil.hash(json.get("password").getAsString()));
            }
            if (json.has("twilioToken") && !json.get("twilioToken").getAsString().trim().isEmpty()) {
                profile.put("twilioToken", json.get("twilioToken").getAsString());
            }

            UserRepository.updateUserProfile(userId, profile);
            response.getWriter().write("{\"status\":\"success\"}");

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"status\":\"error\",\"message\":\"Failed to commit profile updates\"}");
        }
    }
}
