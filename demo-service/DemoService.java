import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class DemoService {
  static final int DB_LATENCY = Integer.parseInt(System.getenv().getOrDefault("DEMO_DB_LATENCY_MS", "80"));
  static final int EXTERNAL_LATENCY = Integer.parseInt(System.getenv().getOrDefault("DEMO_EXTERNAL_LATENCY_MS", "180"));
  public static void main(String[] args) throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
    server.createContext("/health", e -> respond(e, 200, "{\"status\":\"ok\"}"));
    server.createContext("/api/checkout", e -> { sleep(DB_LATENCY); respond(e, 200, "{\"checkoutId\":\"demo-42\",\"status\":\"ready\"}"); });
    server.createContext("/api/n-plus-one", e -> { for (int i=0;i<8;i++) sleep(DB_LATENCY); respond(e, 200, "{\"items\":8,\"warning\":\"intentional N+1 query\"}"); });
    server.createContext("/api/slow-external", e -> { sleep(EXTERNAL_LATENCY); respond(e, 200, "{\"provider\":\"demo-external\",\"warning\":\"intentional downstream delay\"}"); });
    server.createContext("/api/inefficient", e -> { sleep(DB_LATENCY*3); respond(e, 200, "{\"warning\":\"intentional inefficient endpoint\"}"); });
    server.createContext("/api/pool-exhaustion", e -> { sleep(DB_LATENCY*5); respond(e, 503, "{\"error\":\"connection pool exhausted\"}"); });
    server.setExecutor(Executors.newVirtualThreadPerTaskExecutor()); server.start(); System.out.println("Demo service listening on http://localhost:8081");
  }
  static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
  static void respond(HttpExchange e, int code, String body) throws IOException { byte[] b=body.getBytes(StandardCharsets.UTF_8); e.getResponseHeaders().set("Content-Type","application/json"); e.sendResponseHeaders(code,b.length); try(OutputStream out=e.getResponseBody()){out.write(b);} }
}
