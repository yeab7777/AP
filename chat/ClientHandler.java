import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Locale;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;

    private String username;
    private String mode;

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @Override
    public void run() {
        try {
            String login = in.readLine();
            if (login == null || !login.startsWith("LOGIN|")) {
                sendSystem("Invalid login. Disconnecting.");
                return;
            }

            String[] loginParts = login.split("\\|", 3);
            username = sanitize(loginParts, 1, "Guest");
            mode = sanitize(loginParts, 2, "SERVER").toUpperCase(Locale.ROOT);

            if (!ChatServer.registerClient(username, this)) {
                sendSystem("Username already in use. Disconnecting.");
                return;
            }

            sendSystem("Welcome, " + username + ".");
            sendUserList(String.join(",", ChatServer.onlineUsernames()));

            String line;
            while ((line = in.readLine()) != null) {
                if (!line.startsWith("MSG|")) {
                    continue;
                }

                String[] parts = line.split("\\|", 3);
                String target = sanitize(parts, 1, "SERVER");
                String text = sanitize(parts, 2, "");
                if (text.isBlank()) {
                    continue;
                }

                ChatServer.routeMessage(username, target, text);
            }
        } catch (IOException e) {
            ChatServer.appendToServerChat("Connection lost for " + username + ".");
        } finally {
            ChatServer.unregisterClient(username);
            close();
        }
    }

    public void sendMessage(String from, String text) {
        out.println("MSG|" + from + "|" + text);
    }

    public void sendSystem(String text) {
        out.println("SYS|" + text);
    }

    public void sendUserList(String csv) {
        if ("PEER".equalsIgnoreCase(mode)) {
            out.println("USERLIST|" + csv);
        }
    }

    public void close() {
        try {
            in.close();
        } catch (IOException ignored) {
        }
        out.close();
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    private String sanitize(String[] parts, int index, String fallback) {
        if (parts.length <= index) {
            return fallback;
        }
        String value = parts[index] == null ? "" : parts[index].trim();
        return value.isEmpty() ? fallback : value;
    }
}