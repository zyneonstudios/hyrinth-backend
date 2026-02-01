package com.hyrinth.backend.webserver.controller;

import com.alibaba.fastjson2.JSONObject;
import com.hyrinth.backend.Main;
import com.hyrinth.backend.storage.session.AccountSessionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    @PostMapping({"/auth/logout", "/auth/logout/"})
    public static ResponseEntity<Object> handleLogout(HttpServletRequest request) {
        String token = request.getHeader("X-API-Key");
        if (token == null || token.isBlank()) {
            token = readCookieToken(request);
        }
        if (token == null || token.isBlank()) {
            return ErrorController.handleError401(request);
        }
        boolean deleted = Main.getHyrinthBackend().getAccountSessionService().deleteSession(token);
        if (!deleted) {
            return ErrorController.handleError400(request);
        }
        JSONObject response = new JSONObject();
        response.put("status", 200);
        response.put("message", "success");
        return ResponseEntity.ok(response);
    }

    private static String readCookieToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (AccountSessionService.COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
