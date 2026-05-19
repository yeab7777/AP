import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ChatServer extends Application {
    private static final int PORT = 12345;
    private static final Map<String, ClientHandler> CLIENTS = new LinkedHashMap<>();
    private static ChatServer instance;

    private ServerSocket serverSocket;
    private Thread acceptThread;
    private final ObservableList<String> onlineUsers = FXCollections.observableArrayList();
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");
    private VBox chatBox;
    private Label statusLabel;

    private enum Side { LEFT, CENTER }

    @Override
    public void start(Stage stage) {
        instance = this;

        ListView<String> usersView = new ListView<>(onlineUsers);
        usersView.setPrefWidth(170);

        chatBox = new VBox(10);
        chatBox.setPadding(new Insets(14));

        ScrollPane chatScroll = new ScrollPane(chatBox);
        chatScroll.setFitToWidth(true);
        chatScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        Rectangle accent = new Rectangle(12, 12, Color.web("#7c3aed"));
        Label title = new Label("Server Bot Hub");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        HBox header = new HBox(10, accent, title);
        header.setAlignment(Pos.CENTER_LEFT);

        statusLabel = new Label("Starting server...");
        statusLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        VBox leftPanel = new VBox(12, new Label("Online Users"), usersView);
        leftPanel.setPadding(new Insets(14));
        leftPanel.setPrefWidth(200);
        leftPanel.setStyle("-fx-background-color: rgba(15, 23, 42, 0.9); -fx-background-radius: 18;");

        VBox centerPanel = new VBox(12, header, chatScroll, statusLabel);
        VBox.setVgrow(chatScroll, Priority.ALWAYS);
        centerPanel.setPadding(new Insets(16));
        centerPanel.setStyle("-fx-background-color: linear-gradient(to bottom right, #0f172a, #111827); -fx-background-radius: 22;");

        BorderPane root = new BorderPane();
        root.setLeft(leftPanel);
        root.setCenter(centerPanel);
        BorderPane.setMargin(leftPanel, new Insets(16, 0, 16, 16));
        BorderPane.setMargin(centerPanel, new Insets(16));

        Scene scene = new Scene(root, 940, 640);
        scene.setFill(Color.web("#020617"));
        stage.setScene(scene);
        stage.setTitle("Chat Server");
        stage.show();

        appendRow(Side.CENTER, "System", "Server ready.", now());
        startAcceptLoop();
    }

    private void startAcceptLoop() {
        acceptThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                setStatus("Listening on port " + PORT);
                while (!Thread.currentThread().isInterrupted()) {
                    Socket socket = serverSocket.accept();
                    new Thread(new ClientHandler(socket), "client-handler").start();
                }
            } catch (IOException e) {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    appendRow(Side.CENTER, "System", "Server error: " + e.getMessage(), now());
                    setStatus("Server stopped");
                }
            }
        }, "accept-loop");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public static synchronized boolean registerClient(String username, ClientHandler handler) {
        if (CLIENTS.containsKey(username)) {
            return false;
        }
        CLIENTS.put(username, handler);
        refreshOnlineUsers();
        broadcastSystem(username + " joined the chat.");
        return true;
    }

    public static synchronized void unregisterClient(String username) {
        if (username == null) {
            return;
        }
        CLIENTS.remove(username);
        refreshOnlineUsers();
        broadcastSystem(username + " left the chat.");
    }

    public static synchronized Set<String> onlineUsernames() {
        return Set.copyOf(CLIENTS.keySet());
    }

    public static synchronized void routeMessage(String from, String to, String text) {
        MessageDAO.saveMessage(from, to, text);

        if ("SERVER".equalsIgnoreCase(to)) {
            appendRow(Side.LEFT, from + " → Server", text, now());
            String reply = BotBrain.reply(text);
            appendRow(Side.LEFT, "Server Bot", reply, now());

            ClientHandler sender = CLIENTS.get(from);
            if (sender != null) {
                sender.sendMessage("Server Bot", reply);
            }

            MessageDAO.saveMessage("SERVER", from, reply);
            return;
        }

        ClientHandler target = CLIENTS.get(to);
        if (target == null) {
            ClientHandler sender = CLIENTS.get(from);
            if (sender != null) {
                sender.sendSystem("User \"" + to + "\" is not online.");
            }
            appendRow(Side.CENTER, "System", from + " tried to reach " + to + ", but they were offline.", now());
            return;
        }

        appendRow(Side.LEFT, from + " → " + to, text, now());
        target.sendMessage(from, text);
    }

    private static void refreshOnlineUsers() {
        if (instance != null) {
            Platform.runLater(() -> instance.onlineUsers.setAll(CLIENTS.keySet()));
        }

        String csv = String.join(",", CLIENTS.keySet());
        for (ClientHandler handler : CLIENTS.values()) {
            handler.sendUserList(csv);
        }
    }

    public static void appendToServerChat(String message) {
        appendRow(Side.CENTER, "System", message, now());
    }

    private static void broadcastSystem(String text) {
        appendRow(Side.CENTER, "System", text, now());
        for (ClientHandler handler : CLIENTS.values()) {
            handler.sendSystem(text);
        }
    }

    private static void appendRow(Side side, String title, String text, String time) {
        if (instance == null || instance.chatBox == null) {
            System.out.println(title + ": " + text);
            return;
        }

        Platform.runLater(() -> {
            HBox wrapper = new HBox();
            VBox bubble = new VBox(4);
            bubble.setMaxWidth(420);
            bubble.setPadding(new Insets(10, 12, 10, 12));
            bubble.setStyle("-fx-background-color: rgba(148, 163, 184, 0.18); -fx-background-radius: 18;");

            if (side == Side.LEFT) {
                bubble.setStyle("-fx-background-color: linear-gradient(to right, #0f766e, #0891b2); -fx-background-radius: 18;");
            }
            if ("Server Bot".equals(title)) {
                bubble.setStyle("-fx-background-color: linear-gradient(to right, #7c3aed, #2563eb); -fx-background-radius: 18;");
            }

            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #e2e8f0;");

            Label body = new Label(text);
            body.setWrapText(true);
            body.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 14px;");

            Label timeLabel = new Label(time);
            timeLabel.setStyle("-fx-text-fill: rgba(248, 250, 252, 0.65); -fx-font-size: 10px;");

            bubble.getChildren().addAll(titleLabel, body, timeLabel);
            wrapper.setAlignment(side == Side.CENTER ? Pos.CENTER : Pos.CENTER_LEFT);
            wrapper.getChildren().add(bubble);
            instance.chatBox.getChildren().add(wrapper);
        });
    }

    private void setStatus(String text) {
        Platform.runLater(() -> statusLabel.setText(text));
    }

    private static String now() {
        return LocalTime.now().format(instance.timeFmt);
    }

    @Override
    public void stop() throws Exception {
        if (acceptThread != null) {
            acceptThread.interrupt();
        }
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        synchronized (ChatServer.class) {
            for (ClientHandler handler : CLIENTS.values()) {
                handler.close();
            }
            CLIENTS.clear();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static final class BotBrain {
        static String reply(String message) {
            String normalized = message == null ? "" : message.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();

            if (normalized.contains("good bye") || normalized.contains("goodbye")) {
                return "Bye";
            }
            if (normalized.contains("how are you")) {
                return "Good, what about you?";
            }
            if (normalized.contains("hi")) {
                return "Hi";
            }
            return "I’m here. Try saying hi, how are you, or good bye.";
        }
    }
}