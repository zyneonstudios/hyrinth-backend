package com.hyrinth.backend.webserver.controller;

import com.alibaba.fastjson2.JSONObject;
import com.hyrinth.backend.webserver.resolver.ProjectResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProjectController {

    @GetMapping({"/project/**","/projects/**"})
    public static ResponseEntity<Object> handleGetProjectRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        String slug = path.split("/")[2];
        JSONObject project = ProjectResolver.getProject(slug);
        if (project != null) {
            return ResponseEntity.ok(project);
        }
        return ErrorController.handleError404(request);
    }

    @GetMapping({"/project","/projects","/project/","/projects/"})
    public static ResponseEntity<Object> handleListProjectsRequest(HttpServletRequest request) {
        String limitParam = request.getParameter("limit");
        String offsetParam = request.getParameter("offset");
        if (limitParam == null) limitParam = "25";
        if (offsetParam == null) offsetParam = "0";
        return ResponseEntity.ok(ProjectResolver.getProjects(Integer.parseInt(limitParam), Integer.parseInt(offsetParam)));
    }
}