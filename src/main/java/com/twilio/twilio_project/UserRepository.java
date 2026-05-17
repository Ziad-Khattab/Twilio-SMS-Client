package com.twilio.twilio_project;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class UserRepository {

    private UserRepository() {
    }

    public static boolean existsByUsernameEmailOrMsisdn(String username, String email, String msisdn)
            throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ? OR email = ? OR msisdn = ? LIMIT 1";
        try (Connection conn = DBUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, msisdn);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static CustomerTwilioConfig findTwilioConfigByUserId(int userId) throws SQLException {
        String sql = "SELECT twilio_account_sid, twilio_auth_token, twilio_sender_id "
                + "FROM users WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new CustomerTwilioConfig(
                            rs.getString("twilio_account_sid"),
                            rs.getString("twilio_auth_token"),
                            rs.getString("twilio_sender_id"));
                }
            }
        }
        return null;
    }

    public static void createCustomer(PendingRegistration pending) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, role, full_name, birthday, msisdn, job, email, "
                + "address, twilio_account_sid, twilio_auth_token, twilio_sender_id, msisdn_validated) "
                + "VALUES (?, ?, 'customer'::user_role, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)";

        try (Connection conn = DBUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, pending.getUsername());
            stmt.setString(2, pending.getPasswordHash());
            stmt.setString(3, pending.getFullName());
            stmt.setDate(4, pending.getBirthday());
            stmt.setString(5, pending.getMsisdn());
            stmt.setString(6, pending.getJob());
            stmt.setString(7, pending.getEmail());
            stmt.setString(8, pending.getAddress());
            stmt.setString(9, pending.getTwilioAccountSid());
            stmt.setString(10, pending.getTwilioAuthToken());
            stmt.setString(11, pending.getTwilioSenderId());
            stmt.executeUpdate();
        }
    }

    public static List<Map<String, Object>> findSmsHistoryByUserId(int userId) {
        List<Map<String, Object>> history = new ArrayList<>();
        String sql = "SELECT from_phone, to_phone, message, status, sent_at FROM sms_history "
                + "WHERE user_id = ? ORDER BY sent_at DESC";

        try (Connection conn = DBUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> sms = new HashMap<>();
                    sms.put("from", rs.getString("from_phone"));
                    sms.put("recipient", rs.getString("to_phone"));
                    sms.put("message", rs.getString("message"));
                    sms.put("status", rs.getString("status"));
                    sms.put("sentAt", rs.getTimestamp("sent_at"));
                    history.add(sms);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return history;
    }

    public static void recordSms(int userId, String fromPhone, String toPhone, String message, String status)
            throws SQLException {
        String sql = "INSERT INTO sms_history (user_id, from_phone, to_phone, message, status) "
                + "VALUES (?, ?, ?, ?, ?::message_status)";
        try (Connection conn = DBUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, fromPhone);
            stmt.setString(3, toPhone);
            stmt.setString(4, message);
            stmt.setString(5, status);
            stmt.executeUpdate();
        }
    }
}
