package org.app;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.fastcgi.FCGIInterface;


public class Main {

    //Шаблон ответа
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

        //создание интерфейса
        FCGIInterface fcgi = new FCGIInterface();
        System.err.println("FastCGI server started, waiting for GET requests...");
        System.err.flush();
        //System.setProperty("FCGI_PORT", "");
        //Цикл приема запросов
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
                // если не получилось принять соединение ждём и пробуем снова
                System.err.println("-1");
                sleepMillis(200);
                continue;
            }

            try {
                if (FCGIInterface.request == null) {
                    System.err.println("FCGIInterface.request == null => пропускаем итерацию");
                    System.err.flush();
                    continue;
                }

                if (FCGIInterface.request.params == null) {
                    System.err.println("FCGIInterface.request.params == null => пропускаем итерацию");
                    System.err.flush();
                    continue;
                }

                //Получение параметров запроса
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

                //Проверка на попадание
                PointChecker checker = new PointChecker(new HashMap<>(params));
                if (!checker.validate()) {
                    sendResponse(errorJson("invalid parameters"));
                    continue;
                }
                

                long startNs = System.nanoTime();
                boolean hit = checker.isHit();
                long elapsedUs = (System.nanoTime() - startNs) / 1000;
                

                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("x", checker.getX());
                entry.put("y", checker.getY());
                entry.put("r", checker.getR());
                entry.put("result", hit);
                entry.put("time_us", elapsedUs);
                

                HISTORY.add(entry);

                // Формируем ответ из последнего результата и истории результатов
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

    /**
     * Метод для парсинга query_string -> map
     * @param qs query_string
     * @return map
     */
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

    /**
     * Метод для конвертации словаря в json строку
     * @param m словарь
     * @return json-строка
     */
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


    /**
     * Метод для экранирования символов
     * @param s json строка
     * @return Преобразованная строка
     */
    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
    /**
     * Метод для отправки ответа
     * @param body тело ответа
     */
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


    /**
     * Метод для формирования JSON ошибок
     * @param msg Сообщение об ошибке 
     * @return Ответ в виде JSON-строки
     */
    private static String errorJson(String msg) {
        return String.format("{\"status\":\"error\",\"message\":\"%s\"}", escapeJson(msg));
    }

    private static void sleepMillis(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
