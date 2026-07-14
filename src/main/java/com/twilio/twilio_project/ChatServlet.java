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
import java.util.List;
import java.util.Map;

@WebServlet(name = "chatServlet", value = "/api/chat/*")
public class ChatServlet extends HttpServlet {

    private final Gson gson = new Gson();

    private int requireUserId(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.setStatus(401);
            resp.getWriter().write("{\"status\":\"error\",\"message\":\"Unauthorized\"}");
            return -1;
        }
        return (int) session.getAttribute("userId");
    }

    private void json(HttpServletResponse resp, Object data) throws IOException {
        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(data));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path == null) { resp.setStatus(404); return; }

        if ("/send".equals(path)) {
            handleSend(req, resp);
        } else {
            resp.setStatus(404);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path == null) { resp.setStatus(404); return; }

        switch (path) {
            case "/history" -> handleHistory(req, resp);
            case "/users" -> handleUsers(req, resp);
            case "/unread" -> handleUnread(req, resp);
            case "/system" -> handleSystemMessages(req, resp);
            default -> resp.setStatus(404);
        }
    }

    // POST /api/chat/send
    private void handleSend(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int userId = requireUserId(req, resp);
        if (userId < 0) return;

        try {
            String body = UserRepository.readRequestBody(req);
            JsonObject json = gson.fromJson(body, JsonObject.class);
            if (json == null || !json.has("recipientId") || !json.has("content")) {
                resp.setStatus(400);
                json(resp, Map.of("status", "error", "message", "recipientId and content required"));
                return;
            }
            int recipientId = json.get("recipientId").getAsInt();
            String content = json.get("content").getAsString().trim();
            if (content.isEmpty()) {
                resp.setStatus(400);
                json(resp, Map.of("status", "error", "message", "content cannot be empty"));
                return;
            }
            if (recipientId == userId) {
                resp.setStatus(400);
                json(resp, Map.of("status", "error", "message", "Cannot message yourself"));
                return;
            }

            int msgId = UserRepository.insertInternalMessage(userId, recipientId, content);

            // Push via WebSocket to recipient
            JsonObject push = new JsonObject();
            push.addProperty("type", "new_message");
            push.addProperty("messageId", msgId);
            push.addProperty("senderId", userId);
            push.addProperty("content", content);
            ChatWebSocket.pushToUser(recipientId, push.toString());

            json(resp, Map.of("status", "success", "messageId", msgId));
        } catch (SQLException e) {
            resp.setStatus(500);
            json(resp, Map.of("status", "error", "message", "Database error"));
        }
    }

    // GET /api/chat/history?with=<userId>&before=<id>&limit=50
    private void handleHistory(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int userId = requireUserId(req, resp);
        if (userId < 0) return;

        String withParam = req.getParameter("with");
        if (withParam == null) {
            resp.setStatus(400);
            json(resp, Map.of("status", "error", "message", "Missing 'with' parameter"));
            return;
        }
        int withId = Integer.parseInt(withParam);
        int before = req.getParameter("before") != null ? Integer.parseInt(req.getParameter("before")) : 0;
        int limit = req.getParameter("limit") != null ? Integer.parseInt(req.getParameter("limit")) : 50;

        List<Map<String, Object>> msgs = UserRepository.getInternalMessages(userId, withId, limit, before);

        // Mark received messages as read
        for (Map<String, Object> m : msgs) {
            if (((Number) m.get("recipientId")).intValue() == userId) {
                UserRepository.markInternalRead(((Number) m.get("id")).intValue(), userId);
            }
        }

        json(resp, Map.of("status", "success", "messages", msgs));
    }

    // GET /api/chat/users
    private void handleUsers(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int userId = requireUserId(req, resp);
        if (userId < 0) return;
        List<Map<String, Object>> users = UserRepository.findAllUsers(userId);
        json(resp, Map.of("status", "success", "users", users));
    }

    // GET /api/chat/unread
    private void handleUnread(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int userId = requireUserId(req, resp);
        if (userId < 0) return;
        int internalUnread = UserRepository.getUnreadInternalCount(userId);
        int systemUnread = UserRepository.getUnreadSystemCount(userId);
        json(resp, Map.of("status", "success", "internalUnread", internalUnread, "systemUnread", systemUnread));
    }

    // GET /api/chat/system
    private void handleSystemMessages(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int userId = requireUserId(req, resp);
        if (userId < 0) return;
        int limit = req.getParameter("limit") != null ? Integer.parseInt(req.getParameter("limit")) : 50;
        List<Map<String, Object>> msgs = UserRepository.getSystemMessages(userId, limit);
        if (!msgs.isEmpty()) {
            long lastId = ((Number) msgs.get(0).get("id")).longValue();
            UserRepository.markSystemRead(userId, lastId);
        }
        json(resp, Map.of("status", "success", "messages", msgs));
    }
}
