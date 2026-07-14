package com.twilio.twilio_project;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@WebServlet(name = "wiresharkServlet", value = "/admin/wireshark/*")
public class WiresharkServlet extends HttpServlet {

    private static final Gson gson = new Gson();
    private static final Path PCAP_DIR = Paths.get("/tmp");
    private static final String PCAP_FILE = "smpp_capture.pcap";

    private static final ConcurrentHashMap<String, CaptureState> captures = new ConcurrentHashMap<>();

    private static class CaptureState {
        final Process process;
        final long startTime;
        final AtomicBoolean stopped = new AtomicBoolean(false);

        CaptureState(Process process) {
            this.process = process;
            this.startTime = System.currentTimeMillis();
        }

        boolean isRunning() {
            return !stopped.get() && process.isAlive();
        }

        int getPacketCount() {
            Path pcap = PCAP_DIR.resolve(PCAP_FILE);
            if (!Files.exists(pcap)) return 0;
            try {
                String out = exec("tshark", "-r", pcap.toString(), "-T", "fields", "-e", "frame.number");
                if (out == null) return 0;
                return (int) out.lines().filter(l -> !l.isEmpty()).count();
            } catch (Exception e) {
                return -1;
            }
        }

        long getFileSize() {
            try {
                return Files.size(PCAP_DIR.resolve(PCAP_FILE));
            } catch (IOException e) {
                return 0;
            }
        }
    }

    private static String exec(String... cmd) {
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes());
            p.waitFor();
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");

        HttpSession session = req.getSession(false);
        if (session == null || !"administrator".equals(session.getAttribute("userRole"))) {
            resp.setStatus(403);
            resp.getWriter().write("{\"status\":\"error\",\"message\":\"Admins only\"}");
            return;
        }

        String path = req.getPathInfo();
        if (path == null) path = "/";

        try {
            switch (path) {
                case "/start" -> handleStart(resp);
                case "/stop" -> handleStop(resp);
                case "/status" -> handleStatus(resp);
                case "/packets" -> handlePackets(resp);
                case "/download" -> handleDownload(req, resp);
                default -> {
                    resp.setStatus(404);
                    resp.getWriter().write("{\"status\":\"error\",\"message\":\"Unknown action\"}");
                }
            }
        } catch (Exception e) {
            resp.setStatus(500);
            JsonObject err = new JsonObject();
            err.addProperty("status", "error");
            err.addProperty("message", e.getMessage());
            resp.getWriter().write(gson.toJson(err));
        }
    }

    private void handleStart(HttpServletResponse resp) throws IOException {
        CaptureState existing = captures.get(PCAP_FILE);
        if (existing != null && existing.isRunning()) {
            resp.getWriter().write("{\"status\":\"error\",\"message\":\"Capture already running\"}");
            return;
        }

        Path pcap = PCAP_DIR.resolve(PCAP_FILE);
        Files.deleteIfExists(pcap);

        // dumpcap needs wireshark group — use sg
        ProcessBuilder pb = new ProcessBuilder(
            "sg", "wireshark", "-c",
            "/usr/bin/dumpcap -i lo -f \"port 2776\" -w " + pcap + " -P"
        );
        pb.redirectErrorStream(true);
        Process proc = pb.start();

        CaptureState state = new CaptureState(proc);
        captures.put(PCAP_FILE, state);

        JsonObject res = new JsonObject();
        res.addProperty("status", "success");
        res.addProperty("message", "Capture started on lo:2776");
        resp.getWriter().write(gson.toJson(res));
    }

    private void handleStop(HttpServletResponse resp) throws IOException {
        CaptureState state = captures.get(PCAP_FILE);
        if (state == null || !state.isRunning()) {
            resp.getWriter().write("{\"status\":\"error\",\"message\":\"No active capture\"}");
            return;
        }
        state.stopped.set(true);
        state.process.destroyForcibly();
        captures.remove(PCAP_FILE);

        JsonObject res = new JsonObject();
        res.addProperty("status", "success");
        res.addProperty("message", "Capture stopped");
        long elapsed = (System.currentTimeMillis() - state.startTime) / 1000;
        res.addProperty("durationSec", elapsed);
        res.addProperty("fileSize", state.getFileSize());
        resp.getWriter().write(gson.toJson(res));
    }

    private void handleStatus(HttpServletResponse resp) throws IOException {
        CaptureState state = captures.get(PCAP_FILE);
        boolean running = state != null && state.isRunning();
        Path pcap = PCAP_DIR.resolve(PCAP_FILE);
        boolean fileExists = Files.exists(pcap);

        JsonObject res = new JsonObject();
        res.addProperty("status", "success");
        res.addProperty("running", running);
        res.addProperty("fileExists", fileExists);
        if (state != null) {
            long elapsed = (System.currentTimeMillis() - state.startTime) / 1000;
            res.addProperty("durationSec", elapsed);
            res.addProperty("packetCount", state.getPacketCount());
            res.addProperty("fileSize", state.getFileSize());
        } else if (fileExists) {
            res.addProperty("fileSize", Files.size(pcap));
        }
        resp.getWriter().write(gson.toJson(res));
    }

    private void handlePackets(HttpServletResponse resp) throws IOException {
        Path pcap = PCAP_DIR.resolve(PCAP_FILE);
        if (!Files.exists(pcap)) {
            resp.getWriter().write("{\"status\":\"success\",\"packets\":[]}");
            return;
        }

        String tsharkOut = exec("tshark", "-r", pcap.toString(), "-T", "json");

        JsonArray raw = new JsonArray();
        if (tsharkOut != null && !tsharkOut.isEmpty()) {
            try {
                raw = gson.fromJson(tsharkOut, JsonArray.class);
            } catch (Exception ignored) {}
        }

        JsonArray packets = new JsonArray();
        for (int i = 0; i < raw.size(); i++) {
            JsonObject pkt = new JsonObject();
            JsonObject src = raw.get(i).getAsJsonObject().getAsJsonObject("_source");
            if (src == null) continue;
            JsonObject layers = src.getAsJsonObject("layers");
            if (layers == null) continue;

            // time
            JsonObject frame = layers.getAsJsonObject("frame");
            if (frame != null) {
                String rel = getFirst(frame, "frame.time_relative");
                if (rel != null) pkt.addProperty("time", Double.parseDouble(rel));
            }

            // IP
            JsonObject ip = layers.getAsJsonObject("ip");
            if (ip != null) {
                pkt.addProperty("src", getFirst(ip, "ip.src"));
                pkt.addProperty("dst", getFirst(ip, "ip.dst"));
            }

            // SMPP
            JsonObject smpp = layers.getAsJsonObject("smpp");
            if (smpp != null) {
                String cmd = getFirst(smpp, "smpp.command");
                if (cmd != null) {
                    String name = smppCommandName(cmd);
                    pkt.addProperty("cmd", name);
                    pkt.addProperty("cmdRaw", cmd);
                }
                String srcAddr = getFirst(smpp, "smpp.source_addr");
                if (srcAddr != null) pkt.addProperty("srcAddr", srcAddr);
                String dstAddr = getFirst(smpp, "smpp.destination_addr");
                if (dstAddr != null) pkt.addProperty("dstAddr", dstAddr);
                String msg = getFirst(smpp, "smpp.short_message");
                if (msg != null) pkt.addProperty("message", msg.length() > 80 ? msg.substring(0, 80) + "..." : msg);
            }

            packets.add(pkt);
        }

        JsonObject res = new JsonObject();
        res.addProperty("status", "success");
        res.add("packets", packets);
        resp.getWriter().write(gson.toJson(res));
    }

    private static String getFirst(JsonObject obj, String key) {
        if (!obj.has(key)) return null;
        JsonElement el = obj.get(key);
        if (el.isJsonArray()) {
            JsonArray arr = el.getAsJsonArray();
            return arr.size() > 0 && !arr.get(0).isJsonNull() ? arr.get(0).getAsString() : null;
        }
        return el.isJsonNull() ? null : el.getAsString();
    }

    private static String smppCommandName(String hex) {
        return switch (hex) {
            case "0x00000001" -> "BIND_RECEIVER";
            case "0x80000001" -> "BIND_RECEIVER_RESP";
            case "0x00000002" -> "BIND_TRANSMITTER";
            case "0x80000002" -> "BIND_TRANSMITTER_RESP";
            case "0x00000009" -> "BIND_TRX";
            case "0x80000009" -> "BIND_TRX_RESP";
            case "0x00000004" -> "SUBMIT_SM";
            case "0x80000004" -> "SUBMIT_SM_RESP";
            case "0x00000005" -> "DELIVER_SM";
            case "0x80000005" -> "DELIVER_SM_RESP";
            case "0x00000015" -> "ENQUIRE_LINK";
            case "0x80000015" -> "ENQUIRE_LINK_RESP";
            case "0x00000021" -> "UNBIND";
            case "0x80000021" -> "UNBIND_RESP";
            default -> hex;
        };
    }

    private void handleDownload(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Path pcap = PCAP_DIR.resolve(PCAP_FILE);
        if (!Files.exists(pcap)) {
            resp.setStatus(404);
            resp.getWriter().write("{\"status\":\"error\",\"message\":\"No capture file\"}");
            return;
        }
        resp.setContentType("application/vnd.tcpdump.pcap");
        resp.setHeader("Content-Disposition", "attachment; filename=\"smpp_capture.pcap\"");
        Files.copy(pcap, resp.getOutputStream());
    }
}
