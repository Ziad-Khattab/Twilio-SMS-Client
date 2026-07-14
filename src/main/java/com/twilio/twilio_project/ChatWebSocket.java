package com.twilio.twilio_project;

import jakarta.servlet.http.HttpSession;
import jakarta.websocket.*;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/ws/chat", configurator = ChatWebSocket.ChatConfigurator.class)
public class ChatWebSocket {
    private static final Logger log = LoggerFactory.getLogger(ChatWebSocket.class);
    private static final Map<Integer, Set<Session>> userSessions = new ConcurrentHashMap<>();

    public static class ChatConfigurator extends ServerEndpointConfig.Configurator {
        @Override
        public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
            HttpSession httpSession = (HttpSession) request.getHttpSession();
            if (httpSession != null) {
                Object uid = httpSession.getAttribute("userId");
                if (uid != null) {
                    sec.getUserProperties().put("userId", uid);
                }
            }
        }
    }

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        Object uid = config.getUserProperties().get("userId");
        if (uid == null) {
            try { session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Not authenticated")); } catch (Exception ignored) {}
            return;
        }
        int userId = (Integer) uid;
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.debug("WS open for user {}", userId);
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        removeSession(session);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        removeSession(session);
    }

    private void removeSession(Session session) {
        userSessions.forEach((userId, sessions) -> {
            if (sessions.remove(session)) {
                if (sessions.isEmpty()) userSessions.remove(userId);
            }
        });
    }

    public static void pushToUser(int userId, String json) {
        Set<Session> sessions = userSessions.get(userId);
        if (sessions == null) return;
        for (Session s : sessions) {
            if (s.isOpen()) {
                try {
                    s.getBasicRemote().sendText(json);
                } catch (IOException e) {
                    log.warn("WS send fail to user {}: {}", userId, e.getMessage());
                }
            }
        }
    }

    public static int getConnectedCount() {
        return userSessions.size();
    }
}
