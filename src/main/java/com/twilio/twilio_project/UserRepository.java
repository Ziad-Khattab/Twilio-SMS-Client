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

    // Private constructor prevents instantiation (static utility class pattern)
    private UserRepository() {
    }

    /**
     * Reads the raw body of an HTTP request as a string.
     */
    public static String readRequestBody(jakarta.servlet.http.HttpServletRequest request) throws java.io.IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        try (java.io.BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    /**
     * Checks if a user already exists with the given username, email, or phone number.
     * Prevents duplicate registration.
     */
    public static boolean existsByUsernameEmailOrMsisdn(String username, String email, String msisdn)
            throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ? OR email = ? OR msisdn = ? LIMIT 1";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, msisdn);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next(); // Returns true if a record exists
            }
        }
    }

    /**
     * Loads the Twilio credentials associated with a specific customer.
     */
    public static CustomerTwilioConfig findTwilioConfigByUserId(int userId) throws SQLException {
        String sql = "SELECT twilio_account_sid, twilio_auth_token, twilio_sender_id FROM users WHERE id = ?";
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

    /**
     * Inserts a new Customer account into the database upon successful registration.
     */
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

    /**
     * Loads outbound SMS history for a specific customer, sorted descending by date.
     */
    public static List<Map<String, Object>> findSmsHistoryByUserId(int userId) {
        List<Map<String, Object>> history = new ArrayList<>();
        String sql = "SELECT id, from_phone, to_phone, message, status, sent_at FROM sms_history "
                + "WHERE user_id = ? AND direction = 'outbound'::sms_direction ORDER BY sent_at DESC";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> sms = new HashMap<>();
                    sms.put("id", rs.getInt("id"));
                    sms.put("from", rs.getString("from_phone"));
                    sms.put("recipient", rs.getString("to_phone"));
                    sms.put("message", rs.getString("message"));
                    sms.put("status", rs.getString("status"));
                    sms.put("sentAt", rs.getTimestamp("sent_at").toString());
                    history.add(sms);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return history;
    }

    /**
     * Saves a newly sent SMS into the database.
     */
    public static void recordSms(int userId, String fromPhone, String toPhone, String message, String status)
            throws SQLException {
        String sql = "INSERT INTO sms_history (user_id, from_phone, to_phone, message, status, direction) "
                + "VALUES (?, ?, ?, ?, ?::message_status, 'outbound'::sms_direction)";
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

    /**
     * Deletes a specific SMS record. Secures the delete by verifying the user owns the record!
     */
    public static void deleteSmsByIdAndUserId(int smsId, int userId) throws SQLException {
        String sql = "DELETE FROM sms_history WHERE id = ? AND user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, smsId);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    /**
     * Loads the profile details of an authenticated user.
     */
    public static Map<String, String> getUserProfile(int userId) throws SQLException {
        String sql = "SELECT username, full_name, birthday, msisdn, job, email, address, "
                   + "twilio_account_sid, twilio_auth_token, twilio_sender_id "
                   + "FROM users WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, String> profile = new HashMap<>();
                    profile.put("username", rs.getString("username"));
                    profile.put("fullName", rs.getString("full_name"));
                    if (rs.getDate("birthday") != null) {
                        profile.put("birthday", rs.getDate("birthday").toString());
                    }
                    profile.put("msisdn", rs.getString("msisdn"));
                    profile.put("job", rs.getString("job"));
                    profile.put("email", rs.getString("email"));
                    profile.put("address", rs.getString("address"));
                    profile.put("twilioSid", rs.getString("twilio_account_sid"));
                    profile.put("twilioToken", rs.getString("twilio_auth_token"));
                    profile.put("twilioSender", rs.getString("twilio_sender_id"));
                    profile.put("twilioSenderId", rs.getString("twilio_sender_id")); // Redundant bridge key to support frontend 'twilioSenderId' property!
                    return profile;
                }
            }
        }
        return null;
    }

    /**
     * Updates profile values inside the database.
     */
    public static void updateUserProfile(int userId, Map<String, String> profile) throws SQLException {
        StringBuilder sql = new StringBuilder("UPDATE users SET ");
        List<Object> params = new ArrayList<>();

        sql.append("full_name = ?, "); params.add(profile.get("fullName"));

        if (profile.get("birthday") != null && !profile.get("birthday").isEmpty()) {
            sql.append("birthday = ?::date, "); params.add(profile.get("birthday"));
        } else {
            sql.append("birthday = NULL, ");
        }

        sql.append("msisdn = ?, "); params.add(profile.get("msisdn"));
        sql.append("job = ?, "); params.add(profile.get("job"));
        sql.append("email = ?, "); params.add(profile.get("email"));
        sql.append("address = ?, "); params.add(profile.get("address"));
        sql.append("twilio_account_sid = ?, "); params.add(profile.get("twilioSid"));
        
        // If password was edited, append it reactively
        if (profile.containsKey("passwordHash") && profile.get("passwordHash") != null) {
            sql.append("password_hash = ?, "); params.add(profile.get("passwordHash"));
        }
        
        if (profile.get("twilioToken") != null && !profile.get("twilioToken").trim().isEmpty()) {
            sql.append("twilio_auth_token = ?, "); params.add(profile.get("twilioToken"));
        }
        
        sql.append("twilio_sender_id = ? "); params.add(profile.get("twilioSender"));
        sql.append("WHERE id = ?");
        params.add(userId);

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            stmt.executeUpdate();
        }
    }

    /**
     * Retrieves all customer users from the database (excluding administrators).
     */
    public static List<Map<String, Object>> findAllCustomers() throws SQLException {
        List<Map<String, Object>> customers = new ArrayList<>();
        String sql = "SELECT id, username, full_name, msisdn, email, job, created_at " +
                     "FROM users WHERE role = 'customer'::user_role ORDER BY created_at DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> customer = new HashMap<>();
                customer.put("id", rs.getInt("id"));
                customer.put("username", rs.getString("username"));
                customer.put("fullName", rs.getString("full_name"));
                customer.put("msisdn", rs.getString("msisdn"));
                customer.put("email", rs.getString("email"));
                customer.put("job", rs.getString("job"));
                customer.put("createdAt", rs.getTimestamp("created_at").toString());
                customers.add(customer);
            }
        }
        return customers;
    }

    /**
     * Deletes a customer account by their unique database ID.
     */
    public static void deleteUserById(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }

    /**
     * Fetches analytical SMS statistics for all customers.
     */
    public static List<Map<String, Object>> getCustomerSmsStats() throws SQLException {
        List<Map<String, Object>> stats = new ArrayList<>();
        String sql = "SELECT u.id, u.username, u.full_name, COUNT(s.id) AS sent_count " +
                     "FROM users u " +
                     "LEFT JOIN sms_history s ON u.id = s.user_id AND s.direction = 'outbound'::sms_direction " +
                     "WHERE u.role = 'customer'::user_role " +
                     "GROUP BY u.id, u.username, u.full_name " +
                     "ORDER BY sent_count DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> stat = new HashMap<>();
                stat.put("userId", rs.getInt("id"));
                stat.put("username", rs.getString("username"));
                stat.put("fullName", rs.getString("full_name"));
                stat.put("sentCount", rs.getLong("sent_count"));
                stats.add(stat);
            }
        }
        return stats;
    }

    /**
     * Admin capability to create a Customer account directly.
     */
    public static void createCustomerByAdmin(String username, String passwordHash, String fullName, 
                                             java.sql.Date birthday, String msisdn, String job, 
                                             String email, String address, String twilioSid, 
                                             String twilioToken, String twilioSender) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, role, full_name, birthday, msisdn, job, " +
                     "email, address, twilio_account_sid, twilio_auth_token, twilio_sender_id, msisdn_validated) " +
                     "VALUES (?, ?, 'customer'::user_role, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, passwordHash);
            stmt.setString(3, fullName);
            stmt.setDate(4, birthday);
            stmt.setString(5, msisdn);
            stmt.setString(6, job);
            stmt.setString(7, email);
            stmt.setString(8, address);
            stmt.setString(9, twilioSid);
            stmt.setString(10, twilioToken);
            stmt.setString(11, twilioSender);
            stmt.executeUpdate();
        }
    }

    /**
     * Retrieves all inbound (received) SMS messages for a specific user ID.
     */
    public static List<Map<String, Object>> findInboundSmsByUserId(int userId) {
        List<Map<String, Object>> inboundList = new ArrayList<>();
        String sql = "SELECT id, from_phone, to_phone, message, sent_at FROM sms_history " +
                     "WHERE user_id = ? AND direction = 'inbound'::sms_direction ORDER BY sent_at DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> sms = new HashMap<>();
                    sms.put("id", rs.getInt("id"));
                    sms.put("from", rs.getString("from_phone"));
                    sms.put("recipient", rs.getString("to_phone"));
                    sms.put("message", rs.getString("message"));
                    sms.put("sentAt", rs.getTimestamp("sent_at").toString());
                    inboundList.add(sms);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return inboundList;
    }
}
