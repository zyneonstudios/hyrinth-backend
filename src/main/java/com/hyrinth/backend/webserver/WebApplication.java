package com.hyrinth.backend.webserver;

import com.hyrinth.backend.HyrinthBackend;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication(scanBasePackages = "com.hyrinth.backend")
public class WebApplication {

    private int port;
    private final String[] args;
    private HyrinthBackend backend;
    private boolean started = false;

    public WebApplication() {
        this(8085, new String[]{});
    }

    public WebApplication(HyrinthBackend backend) {
        this(8085, backend.getArgs());
        this.backend = backend;
    }

    public WebApplication(int port) {
        this(port, new String[]{});
    }

    public WebApplication(String[] args) {
        this(8085, args);
    }

    public WebApplication(int port, String[] args) {
        this.port = port;
        this.args = args;
    }

    public void start() {
        if(!started) {
            started = true;
            if (port > 65535) {
                throw new RuntimeException("Port range exceeded, cannot launch application web server. Try to restart your computer or stopping port using applications and try again.");
            }
            try {
                new SpringApplicationBuilder(WebApplication.class)
                        .properties("logging.level.root=WARN", "logging.pattern.console=", "server.port=" + port)
                        .run(getArgs());
            } catch (Exception e) {
                port++;
            }
        }
    }

    public String[] getArgs() {
        if(backend != null) {
            return backend.getArgs();
        }
        return args;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public HyrinthBackend getBackend() {
        return backend;
    }

    public void setBackend(HyrinthBackend backend) {
        if(this.backend == null) {
            this.backend = backend;
        }
    }
}
