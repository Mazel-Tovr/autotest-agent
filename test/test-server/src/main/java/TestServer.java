import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class TestServer {

    private HttpServer httpServer;
    private final String host;
    private final int port;

    public TestServer(String host, String port) {
        if (host.isEmpty() || port.isEmpty()) {
            throw new RuntimeException("Host or Port is not present. Application could not start");
        }
        this.host = host;
        this.port = Integer.parseInt(port);
    }

    public void startServer() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(host, port), 0);

        httpServer.createContext("/test", httpExchange -> {
            String response = "This is the response";
            httpExchange.sendResponseHeaders(200, response.length());
            OutputStream responseBody = httpExchange.getResponseBody();
            responseBody.write(response.getBytes());
            responseBody.close();
        });
        httpServer.createContext("/api/agents/test-pet-standalone/plugins/test2code/dispatch-action", httpExchange -> {
            String response = "OK";
            httpExchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream responseBody = httpExchange.getResponseBody();
            responseBody.write(response.getBytes());
            responseBody.close();
        });
        httpServer.createContext("/api/login", httpExchange -> {
            if (httpExchange.getRequestMethod().equals("POST")) {
                String response = "OK";
                httpExchange.getResponseHeaders().add("authorization", "token");
                httpExchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream responseBody = httpExchange.getResponseBody();
                responseBody.write(response.getBytes());
                responseBody.close();
            }
        });

        httpServer.start();
        System.out.println("Server host" + httpServer.getAddress());
    }

    public void stopServe() {
        System.out.println("I am stopped");
        httpServer.stop(1);
    }

}
