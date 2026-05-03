package com.twilio.twilio_project;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.twilio.twilio_project.DBUtil;

@WebServlet(name = "registerServlet", value = "/register")
public class RegisterServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // TODO: Handle registration logic

        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String confirm_password = request.getParameter("confirm_password");
        String msisdn = request.getParameter("msisdn");

        if (confirm_password == null || username == null || password == null || username.trim().isEmpty()
                || password.trim().isEmpty() || confirm_password.trim().isEmpty() || msisdn == null
                || msisdn.trim().isEmpty()) {
            request.setAttribute("error", "Username, password and msisdn are required");
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }

        if (password.equals(confirm_password)) {
            boolean stored = store_credentials(username, password, msisdn);
            if (stored) {
                response.sendRedirect("login");
                return;
            } else {
                request.setAttribute("error", "Error storing credentials");
                request.getRequestDispatcher("register.jsp").forward(request, response);
            }
        } else {
            request.setAttribute("error", "Passwords do not match");
            request.getRequestDispatcher("register.jsp").forward(request, response);
        }
    }

    private static boolean store_credentials(String username, String password, String msisdn) {
        String sql = "INSERT INTO users (username, password, msisdn) VALUES (?, ?, ?)";

        try (Connection conn = DBUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, msisdn);

            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}