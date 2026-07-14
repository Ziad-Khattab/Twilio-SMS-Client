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

@WebServlet(name = "broadcastServlet", value = "/admin/broadcast")
public class BroadcastServlet extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null || !"administrator".equals(session.getAttribute("userRole"))) {
            resp.setStatus(403);
            resp.getWriter().write("{\"status\":\"error\",\"message\":\"Admins only\"}");
            return;
        }

        try {
            String body = UserRepository.readRequestBody(req);
            JsonObject json = gson.fromJson(body, JsonObject.class);
            String content = json != null && json.has("content") ? json.get("content").getAsString().trim() : "";
            boolean sendSms = json != null && json.has("sendSms") && json.get("sendSms").getAsBoolean();
            String target = json != null && json.has("target") ? json.get("target").getAsString() : "customers";

            if (content.isEmpty()) {
                resp.setStatus(400);
                resp.getWriter().write("{\"status\":\"error\",\"message\":\"content is required\"}");
                return;
            }

            // Insert system message
            int msgId = UserRepository.insertSystemMessage(content);

            // Push via WebSocket to all connected customers
            List<Integer> customerIds = UserRepository.getAllCustomerUserIds();
            JsonObject push = new JsonObject();
            push.addProperty("type", "system_message");
            push.addProperty("messageId", msgId);
            push.addProperty("content", content);

            int pushedCount = 0;
            for (int uid : customerIds) {
                ChatWebSocket.pushToUser(uid, push.toString());
                pushedCount++;

                // Optionally send as real SMS
                if (sendSms) {
                    try {
                        SmsRouter.send("broadcast", content, uid);
                    } catch (Exception ignored) {}
                }
            }

            // Also push to admin WebSocket connections
            push.addProperty("type", "broadcast_log");
            ChatWebSocket.pushToUser((int) session.getAttribute("userId"), push.toString());

            JsonObject res = new JsonObject();
            res.addProperty("status", "success");
            res.addProperty("messageId", msgId);
            res.addProperty("pushedCount", pushedCount);
            resp.getWriter().write(gson.toJson(res));

        } catch (SQLException e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"status\":\"error\",\"message\":\"Database error\"}");
        }
    }
}
