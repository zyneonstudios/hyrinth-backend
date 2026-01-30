package com.hyrinth.backend.webserver.controller;

import com.alibaba.fastjson2.JSONObject;
import com.hyrinth.backend.Main;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    @GetMapping("/**")
    public ResponseEntity<Object> handleRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        try {
            if(path.equals("/")||path.equals("/index")||path.equals("/index/")) {
                JSONObject response = new JSONObject();
                response.put("about", "Welcome to the Hyrinth Backend API!");
                response.put("documentation",Main.getHyrinthBackend().getSettings().getDocs());
                response.put("name",Main.getHyrinthBackend().getSettings().getName());
                response.put("version", Main.getHyrinthBackend().getVersion());
                return ResponseEntity.ok(response);
            } else if(path.startsWith("/user")) {
                if(path.equals("/user")||path.equals("/user/")||path.equals("/users")||path.equals("/users/")) {
                    return UserController.handleListUsersRequest(request);
                } else if(path.startsWith("/user/")||path.startsWith("/users/")) {
                    return UserController.handleGetUserRequest(request);
                }
            }
        } catch (Exception e) {
            if(Main.getHyrinthBackend().isDebug()) {
                Main.getLogger().err("Failed to resolve " + path + ": " + e.getMessage());
                return ErrorController.handleError(request,e);
            }
            return ErrorController.handleError500(request);
        }

        return ErrorController.handleError404(request);
    }
}
