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
            throw new RuntimeException("Database error loading SMS history", e);
        }
        return history;
    }

    /**
     * Saves a newly sent SMS into the database.
     */
    public static void recordSms(int userId, String fromPhone, String toPhone, String message, String status)
            throws SQLException {
        recordSms(userId, fromPhone, toPhone, message, status, null);
    }

    public static void recordSms(int userId, String fromPhone, String toPhone, String message, String status, String providerRefId)
            throws SQLException {
        String sql = "INSERT INTO sms_history (user_id, from_phone, to_phone, message, status, direction, provider_ref_id) "
                + "VALUES (?, ?, ?, ?, ?::message_status, 'outbound'::sms_direction, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, fromPhone);
            stmt.setString(3, toPhone);
            stmt.setString(4, message);
            stmt.setString(5, status);
            stmt.setString(6, providerRefId);
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

    public static void saveInboundSms(int userId, String from, String to, String message) throws SQLException {
        String sql = "INSERT INTO sms_history (user_id, from_phone, to_phone, message, status, direction) "
                + "VALUES (?, ?, ?, ?, 'delivered'::message_status, 'inbound'::sms_direction)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, from);
            stmt.setString(3, to);
            stmt.setString(4, message);
            stmt.executeUpdate();
        }
    }

    public static void updateSmsStatusByProviderRefId(String providerRefId, String status) throws SQLException {
        String mapped = mapSmppStatus(status);
        String sql = "UPDATE sms_history SET status = ?::message_status WHERE provider_ref_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, mapped);
            stmt.setString(2, providerRefId);
            stmt.executeUpdate();
        }
    }

    private static String mapSmppStatus(String smppStatus) {
        if (smppStatus == null) return "delivered";
        return switch (smppStatus.toUpperCase()) {
            case "DELIVRD", "ACCEPTD", "0" -> "delivered";
            case "EXPIRED", "DELETED", "UNDELIV", "UNKNOWN", "REJECTD" -> "failed";
            default -> "delivered";
        };
    }

    public static int findUserIdByPhone(String phone) throws SQLException {
        String sql = "SELECT id FROM users WHERE msisdn = ? LIMIT 1";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, phone);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        return -1;
    }

    /**
     * Loads the profile details of an authenticated user.
     */
    public static Map<String, String> getUserProfile(int userId) throws SQLException {
        String sql = "SELECT username, full_name, birthday, msisdn, job, email, address, "
                   + "twilio_account_sid, twilio_sender_id, role, "
                   + "sms_provider, smpp_host, smpp_port, smpp_system_id, smpp_address_range "
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
                    profile.put("twilioSender", rs.getString("twilio_sender_id"));
                    profile.put("twilioSenderId", rs.getString("twilio_sender_id")); // Redundant bridge key to support frontend 'twilioSenderId' property!
                    profile.put("role", rs.getString("role"));
                    profile.put("smsProvider", rs.getString("sms_provider"));
                    profile.put("smppHost", rs.getString("smpp_host"));
                    int smppPort = rs.getInt("smpp_port");
                    if (!rs.wasNull()) {
                        profile.put("smppPort", String.valueOf(smppPort));
                    }
                    profile.put("smppSystemId", rs.getString("smpp_system_id"));
                    profile.put("smppAddressRange", rs.getString("smpp_address_range"));
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
        
        sql.append("twilio_sender_id = ?, "); params.add(profile.get("twilioSender"));

        if (profile.get("smsProvider") != null && !profile.get("smsProvider").isEmpty()) {
            sql.append("sms_provider = ?, "); params.add(profile.get("smsProvider"));
        }
        if (profile.get("smppHost") != null) {
            sql.append("smpp_host = ?, "); params.add(profile.get("smppHost"));
        }
        if (profile.get("smppPort") != null && !profile.get("smppPort").isEmpty()) {
            sql.append("smpp_port = ?, "); params.add(Integer.parseInt(profile.get("smppPort")));
        }
        if (profile.get("smppSystemId") != null) {
            sql.append("smpp_system_id = ?, "); params.add(profile.get("smppSystemId"));
        }
        if (profile.get("smppPassword") != null) {
            sql.append("smpp_password = ?, "); params.add(profile.get("smppPassword"));
        }
        if (profile.get("smppAddressRange") != null) {
            sql.append("smpp_address_range = ?, "); params.add(profile.get("smppAddressRange"));
        }

        // Remove trailing comma+space
        int len = sql.length();
        if (sql.charAt(len - 2) == ',') {
            sql.setLength(len - 2);
        }

        sql.append(" WHERE id = ?");
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
                                             String twilioToken, String twilioSender,
                                             String smsProvider, String smppHost, String smppPort,
                                             String smppSystemId, String smppPassword, String smppAddressRange) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, role, full_name, birthday, msisdn, job, " +
                     "email, address, twilio_account_sid, twilio_auth_token, twilio_sender_id, msisdn_validated, " +
                     "sms_provider, smpp_host, smpp_port, smpp_system_id, smpp_password, smpp_address_range) " +
                     "VALUES (?, ?, 'customer'::user_role, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, ?, ?, ?, ?, ?, ?)";
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
            stmt.setString(12, smsProvider != null && !smsProvider.isEmpty() ? smsProvider : null);
            stmt.setString(13, smppHost != null && !smppHost.isEmpty() ? smppHost : null);
            if (smppPort != null && !smppPort.isEmpty()) {
                stmt.setObject(14, Integer.parseInt(smppPort));
            } else {
                stmt.setObject(14, null);
            }
            stmt.setString(15, smppSystemId != null && !smppSystemId.isEmpty() ? smppSystemId : null);
            stmt.setString(16, smppPassword != null && !smppPassword.isEmpty() ? smppPassword : null);
            stmt.setString(17, smppAddressRange != null && !smppAddressRange.isEmpty() ? smppAddressRange : null);
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
            throw new RuntimeException("Database error loading SMS history", e);
        }
        return inboundList;
    }

    // ── SMS Provider config ──

    public static String findSmsProvider(int userId) {
        String sql = "SELECT sms_provider FROM users WHERE id = ?";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("sms_provider");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error reading sms_provider", e);
        }
        return "TWILIO";
    }

    public static String findSmppConfig(int userId, String field) {
        String sql = "SELECT " + field + " FROM users WHERE id = ?";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString(field);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error reading " + field, e);
        }
        return "";
    }

    // ── Internal chat ──

    public static List<Map<String, Object>> findAllUsers(int excludeUserId) {
        List<Map<String, Object>> users = new ArrayList<>();
        String sql = "SELECT id, username, full_name, msisdn FROM users WHERE id != ? ORDER BY username";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, excludeUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> u = new HashMap<>();
                    u.put("id", rs.getInt("id"));
                    u.put("username", rs.getString("username"));
                    u.put("fullName", rs.getString("full_name"));
                    u.put("msisdn", rs.getString("msisdn"));
                    users.add(u);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error loading users", e);
        }
        return users;
    }

    public static int insertInternalMessage(int senderId, int recipientId, String content) throws SQLException {
        String sql = "INSERT INTO internal_messages (sender_id, recipient_id, content) VALUES (?, ?, ?) RETURNING id";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, senderId);
            stmt.setInt(2, recipientId);
            stmt.setString(3, content);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public static List<Map<String, Object>> getInternalMessages(int userId1, int userId2, int limit, int beforeId) {
        List<Map<String, Object>> msgs = new ArrayList<>();
        String sql = "SELECT im.id, im.sender_id, im.recipient_id, im.content, im.status, im.created_at, im.read_at, " +
                     "s.username AS sender_username, r.username AS recipient_username " +
                     "FROM internal_messages im " +
                     "JOIN users s ON s.id = im.sender_id " +
                     "JOIN users r ON r.id = im.recipient_id " +
                     "WHERE ((im.sender_id = ? AND im.recipient_id = ?) OR (im.sender_id = ? AND im.recipient_id = ?)) " +
                     (beforeId > 0 ? "AND im.id < ? " : "") +
                     "ORDER BY im.created_at DESC LIMIT ?";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            int idx = 1;
            stmt.setInt(idx++, userId1);
            stmt.setInt(idx++, userId2);
            stmt.setInt(idx++, userId2);
            stmt.setInt(idx++, userId1);
            if (beforeId > 0) stmt.setInt(idx++, beforeId);
            stmt.setInt(idx, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", rs.getInt("id"));
                    m.put("senderId", rs.getInt("sender_id"));
                    m.put("recipientId", rs.getInt("recipient_id"));
                    m.put("content", rs.getString("content"));
                    m.put("status", rs.getString("status"));
                    m.put("createdAt", rs.getTimestamp("created_at").toString());
                    if (rs.getTimestamp("read_at") != null) m.put("readAt", rs.getTimestamp("read_at").toString());
                    m.put("senderUsername", rs.getString("sender_username"));
                    m.put("recipientUsername", rs.getString("recipient_username"));
                    msgs.add(m);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error loading internal messages", e);
        }
        return msgs;
    }

    public static int getUnreadInternalCount(int userId) {
        String sql = "SELECT COUNT(*) FROM internal_messages WHERE recipient_id = ? AND read_at IS NULL";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error counting unread messages", e);
        }
        return 0;
    }

    public static void markInternalRead(int messageId, int userId) {
        String sql = "UPDATE internal_messages SET read_at = NOW() WHERE id = ? AND recipient_id = ? AND read_at IS NULL";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, messageId);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database error marking message read", e);
        }
    }

    // ── System broadcast ──

    public static int insertSystemMessage(String content) throws SQLException {
        String sql = "INSERT INTO system_messages (content) VALUES (?) RETURNING id";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, content);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public static List<Map<String, Object>> getSystemMessages(int userId, int limit) {
        List<Map<String, Object>> msgs = new ArrayList<>();
        String sql = "SELECT sm.id, sm.content, sm.created_at, COALESCE(smr.last_read_id, 0) AS last_read_id " +
                     "FROM system_messages sm " +
                     "LEFT JOIN system_message_reads smr ON smr.user_id = ? " +
                     "ORDER BY sm.id DESC LIMIT ?";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", rs.getInt("id"));
                    m.put("content", rs.getString("content"));
                    m.put("createdAt", rs.getTimestamp("created_at").toString());
                    m.put("read", rs.getLong("last_read_id") >= rs.getLong("id"));
                    msgs.add(m);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error loading system messages", e);
        }
        return msgs;
    }

    public static int getUnreadSystemCount(int userId) {
        String sql = "SELECT COUNT(*) FROM system_messages sm " +
                     "LEFT JOIN system_message_reads smr ON smr.user_id = ? " +
                     "WHERE smr.last_read_id IS NULL OR sm.id > smr.last_read_id";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error counting system messages", e);
        }
        return 0;
    }

    public static void markSystemRead(int userId, long lastReadId) {
        String sql = "INSERT INTO system_message_reads (user_id, last_read_id) VALUES (?, ?) " +
                     "ON CONFLICT (user_id) DO UPDATE SET last_read_id = ?";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setLong(2, lastReadId);
            stmt.setLong(3, lastReadId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database error marking system read", e);
        }
    }

    public static List<Integer> getAllCustomerUserIds() {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT id FROM users WHERE role = 'customer'::user_role";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) ids.add(rs.getInt(1));
        } catch (SQLException e) {
            throw new RuntimeException("Database error loading customer IDs", e);
        }
        return ids;
    }
}
