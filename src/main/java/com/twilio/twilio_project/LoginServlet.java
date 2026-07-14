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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet(name = "loginServlet", value = "/login")
public class LoginServlet extends HttpServlet {

    private final Gson gson = new Gson();

    private static class AuthResult {
        int id;
        String role;
        AuthResult(int id, String role) { this.id = id; this.role = role; }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            String body = UserRepository.readRequestBody(request);
            JsonObject json = gson.fromJson(body, JsonObject.class);
            
            if (json == null || !json.has("username") || !json.has("password")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"status\":\"error\",\"message\":\"Missing credentials\"}");
                return;
            }

            String username = json.get("username").getAsString().trim();
            String password = json.get("password").getAsString();

            AuthResult result = validateUser(username, password);

            if (result != null) {
                HttpSession session = request.getSession(true);
                session.setAttribute("userId", result.id);
                session.setAttribute("userRole", result.role);

                JsonObject success = new JsonObject();
                success.addProperty("status", "success");
                success.addProperty("role", result.role);
                success.addProperty("userId", result.id);
                response.getWriter().write(gson.toJson(success));
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"status\":\"error\",\"message\":\"Invalid username or password\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"status\":\"error\",\"message\":\"Server error occurred\"}");
        }
    }

    private AuthResult validateUser(String username, String password) {
        String sql = "SELECT id, role, password_hash FROM users WHERE username = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && PasswordUtil.matches(password, rs.getString("password_hash"))) {
                    return new AuthResult(rs.getInt("id"), rs.getString("role"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
