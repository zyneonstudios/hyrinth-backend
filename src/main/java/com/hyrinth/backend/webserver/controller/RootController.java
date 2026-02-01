package com.hyrinth.backend.webserver.controller;

import com.alibaba.fastjson2.JSONObject;
import com.hyrinth.backend.Main;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @RequestMapping(value="/api/test",method = RequestMethod.POST)
    public ResponseEntity<JSONObject> getJsonObject(@RequestHeader("x-api-key") String apiKey, @RequestBody JSONObject jsonObject) {

        return new ResponseEntity<>(new JSONObject(), HttpStatus.OK);
    }
}
