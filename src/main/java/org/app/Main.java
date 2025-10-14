package org.app;

import com.fastcgi.FCGIInterface;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import java.time.format.DateTimeFormatter;
import java.util.*;


public class Main {

    private static final String BASE_RESPONSE_TEMPLATE = ""
            + "Status: 200 OK\r\n"
            + "Content-Type: application/json\r\n"
            + "Access-Control-Allow-Origin: *\r\n"
            + "Connection: keep-alive\r\n"
            + "Content-Length: %d\r\n"
            + "\r\n"
            + "%s";

    // История результатов в памяти
    private static final List<Map<String, Object>> HISTORY = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        FCGIInterface fcgi = new FCGIInterface();
        System.err.println("FastCGI server started, waiting for GET requests...");
        System.err.flush();

        while (true) {
            int ret;
            try {
                ret = fcgi.FCGIaccept();
            } catch (Throwable t) {
                System.err.println("FCGIaccept threw exception: " + t.getMessage());
                System.err.flush();
                t.printStackTrace();
                sleepMillis(200);
                continue;
            }
            System.err.println("FCGIaccept called");


            if (ret < 0) {
                // если не получилось принять соединение — ждём и пробуем снова
                sleepMillis(200);
                continue;
            }

            try {
                if (FCGIInterface.request == null) {
                    System.err.println("Warning: FCGIInterface.request == null — пропускаем итерацию");
                    System.err.flush();
                    continue;
                }

                if (FCGIInterface.request.params == null) {
                    System.err.println("Warning: FCGIInterface.request.params == null — пропускаем итерацию");
                    System.err.flush();
                    continue;
                }

                
                Properties props = FCGIInterface.request.params;
                String query = props.getProperty("QUERY_STRING", "");
                String method = props.getProperty("REQUEST_METHOD", "GET");
                String uri = props.getProperty("REQUEST_URI", props.getProperty("SCRIPT_NAME", "/check"));
                System.err.println("Incoming request: method=" + method + ", uri=" + uri + ", QUERY_STRING=" + query);

                if (query == null || query.trim().isEmpty()) {
                    sendResponse(errorJson("empty query"));
                    continue;
                }

                Map<String, String> params = parseQueryString(query);

                
                PointChecker checker = new PointChecker(new HashMap<>(params));
                if (!checker.validate()) {
                    sendResponse(errorJson("invalid parameters"));
                    continue;
                }

                long startNs = System.nanoTime();
                boolean hit = checker.isHit();
                long elapsedUs = (System.nanoTime() - startNs) / 1000;
                String serverTime = DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now());


                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("x", checker.getX());
                entry.put("y", checker.getY());
                entry.put("r", checker.getR());
                entry.put("result", hit);
                entry.put("time_us", elapsedUs);
                //entry.put("server_time", serverTime);

                HISTORY.add(entry);

                // Формируем ответ: last + history
                StringBuilder sb = new StringBuilder();
                sb.append("{");
                sb.append("\"status\":\"ok\",");
                sb.append("\"last\":").append(mapToJson(entry)).append(",");
                sb.append("\"history\":[");
                synchronized (HISTORY) {
                    boolean first = true;
                    for (Map<String, Object> e : HISTORY) {
                        if (!first) sb.append(",");
                        sb.append(mapToJson(e));
                        first = false;
                    }
                }
                sb.append("]}");

                String body = sb.toString();
                System.err.println("Responding (len=" + body.getBytes(StandardCharsets.UTF_8).length + "): " + body);
                System.err.flush();
                sendResponse(body);

            } catch (Throwable t) {
                t.printStackTrace();
                sendResponse(errorJson("internal server error"));
            }
        }
    }

    // Парсинг QUERY_STRING (url-decode ключей и значений)
    private static Map<String, String> parseQueryString(String qs) {
        Map<String, String> map = new HashMap<>();
        if (qs == null || qs.isEmpty()) return map;
        String[] pairs = qs.split("&");
        for (String p : pairs) {
            String[] kv = p.split("=", 2);
            if (kv.length == 2) {
                try {
                    String k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8.name());
                    String v = URLDecoder.decode(kv[1], StandardCharsets.UTF_8.name());
                    map.put(k, v);
                } catch (Exception ignored) {}
            }
        }
        return map;
    }

    private static String mapToJson(Map<String, Object> m) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : m.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escapeJson(e.getKey())).append("\":");
            Object v = e.getValue();
            if (v == null) {
                sb.append("null");
            } else if (v instanceof Number || v instanceof Boolean) {
                sb.append(v.toString());
            } else {
                sb.append("\"").append(escapeJson(v.toString())).append("\"");
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static void sendResponse(String body) {
        try {
            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
            String resp = String.format(BASE_RESPONSE_TEMPLATE, bodyBytes.length, body);
            System.out.print(resp);
            System.out.flush();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static String errorJson(String msg) {
        return String.format("{\"status\":\"error\",\"message\":\"%s\"}", escapeJson(msg));
    }

    private static void sleepMillis(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
