package com.hyrinth.backend.webserver.controller;

import com.alibaba.fastjson2.JSONObject;
import com.hyrinth.backend.Main;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

public class ErrorController {

    public static ResponseEntity<Object> handleError(HttpServletRequest request, int status) {
        return handleError(request, status, "An error occurred.");
    }

    public static ResponseEntity<Object> handleError(HttpServletRequest request, int status, String message) {
        String path = request.getRequestURI();

        JSONObject error = new JSONObject();
        error.put("status", status);
        error.put("message", message);

        JSONObject connection = new JSONObject();
        connection.put("url", request.getRequestURL().toString());
        connection.put("protocol", request.getScheme());
        connection.put("host", request.getHeader("Host"));
        connection.put("path", path);
        connection.put("method", request.getMethod());

        JSONObject response = new JSONObject();
         response.put("error", error);
         response.put("connection", connection);
         response.put("description", "Error "+status+" - "+message);
        response.put("documentation", Main.getHyrinthBackend().getSettings().getDocs());
         response.put("name", Main.getHyrinthBackend().getSettings().getName());
        response.put("version", Main.getHyrinthBackend().getVersion());

        return ResponseEntity.ok(response);
    }

    public static ResponseEntity<Object> handleError(HttpServletRequest request, Exception e) {
        return handleError(request, 500, e.getMessage());
    }

    public static ResponseEntity<Object> handleError400(HttpServletRequest request) {
        return handleError(request, 400, "Bad request");
    }

    public static ResponseEntity<Object> handleError401(HttpServletRequest request) {
        return handleError(request, 401, "Unauthorized");
    }

    public static ResponseEntity<Object> handleError403(HttpServletRequest request) {
        return handleError(request, 403, "No permission");
    }

    public static ResponseEntity<Object> handleError404(HttpServletRequest request) {
        return handleError(request, 404, "Resource not found");
    }

    public static ResponseEntity<Object> handleError500(HttpServletRequest request) {
        return handleError(request, 500, "Internal server error");
    }

    public static ResponseEntity<Object> handleError503(HttpServletRequest request) {
        return handleError(request, 503, "Service unavailable");
    }
}