import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;

public class Main {
    public static void main(@NotNull String[] args) throws IOException, InterruptedException {
        System.out.println("Hello there");
        TestServer localServer = new TestServer(args[0],args[1]);
        localServer.startServer();
        Runtime.getRuntime().addShutdownHook(new Thread(localServer::stopServe));
    }
}
