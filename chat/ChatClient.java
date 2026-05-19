import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ChatClient extends Application {
    private Socket socket;
    private PrintWriter serverOut;
    private BufferedReader serverIn;
    private Thread incomingMessages;

    private final ObservableList<String> onlineUsers = FXCollections.observableArrayList();
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

    private VBox chatBox;
    private Label statusLabel;
    private ComboBox<String> recipientBox;
    private TextField messageField;
    private String username;
    private ChatMode mode = ChatMode.SERVER;

    private enum ChatMode { SERVER, PEER }
    private enum Side { LEFT, RIGHT, CENTER }

    @Override
    public void start(Stage stage) {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #020617, #111827);");
        root.getChildren().add(buildLoginView(root));

        Scene scene = new Scene(root, 960, 660);
        scene.setFill(Color.web("#020617"));
        stage.setScene(scene);
        stage.setTitle("GlowChat");
        stage.show();
    }

    private VBox buildLoginView(StackPane root) {
        Label brand = new Label("GlowChat");
        brand.setStyle("-fx-font-size: 34px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label subtitle = new Label("Choose who you want to chat with.");
        subtitle.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 14px;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(280);

        ComboBox<ChatMode> modeBox = new ComboBox<>(FXCollections.observableArrayList(ChatMode.SERVER, ChatMode.PEER));
        modeBox.setValue(ChatMode.SERVER);
        modeBox.setMaxWidth(280);

        Label hint = new Label("Server Bot = chat with the server bot. Peer Chat = talk with another online user.");
        hint.setWrapText(true);
        hint.setMaxWidth(300);
        hint.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        Button startButton = new Button("Start Chatting");
        startButton.setMaxWidth(280);
        startButton.setDefaultButton(true);

        VBox card = new VBox(14, brand, subtitle, usernameField, modeBox, hint, startButton);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(28));
        card.setMaxWidth(360);
        card.setStyle("-fx-background-color: rgba(15, 23, 42, 0.88); -fx-background-radius: 24; -fx-border-color: rgba(148, 163, 184, 0.2); -fx-border-radius: 24;");

        Rectangle glow = new Rectangle(160, 160, Color.web("#7c3aed", 0.22));
        glow.setArcWidth(32);
        glow.setArcHeight(32);

        VBox wrapper = new VBox(18, glow, card);
        wrapper.setAlignment(Pos.CENTER);

        startButton.setOnAction(event -> {
            String name = usernameField.getText().trim();
            if (name.isEmpty()) {
                subtitle.setText("Enter a username first.");
                return;
            }
            username = name;
            mode = modeBox.getValue();
            connectAndOpenChat(root);
        });

        return wrapper;
    }

    private void connectAndOpenChat(StackPane root) {
        try {
            socket = new Socket("localhost", 12345);
            serverOut = new PrintWriter(socket.getOutputStream(), true);
            serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            serverOut.println("LOGIN|" + username + "|" + mode.name());

            root.getChildren().setAll(buildChatView());

            incomingMessages = new Thread(this::readIncoming, "client-incoming");
            incomingMessages.setDaemon(true);
            incomingMessages.start();
        } catch (IOException e) {
            Label error = new Label("Unable to connect to server: " + e.getMessage());
            error.setStyle("-fx-text-fill: #fecaca; -fx-font-size: 14px;");
            root.getChildren().setAll(error);
        }
    }

    private VBox buildChatView() {
        chatBox = new VBox(10);
        chatBox.setPadding(new Insets(14));

        ScrollPane chatScroll = new ScrollPane(chatBox);
        chatScroll.setFitToWidth(true);
        chatScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        Label chatTitle = new Label(mode == ChatMode.SERVER ? "Server Bot" : "Peer Chat");
        chatTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        statusLabel = new Label("Connected as " + username);
        statusLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        recipientBox = new ComboBox<>(onlineUsers);
        recipientBox.setPromptText("Choose user");
        recipientBox.setVisible(mode == ChatMode.PEER);
        recipientBox.setManaged(mode == ChatMode.PEER);

        TextField recipientHint = new TextField(mode == ChatMode.SERVER ? "Server Bot" : "Select peer user");
        recipientHint.setDisable(true);
        recipientHint.setPrefWidth(160);

        messageField = new TextField();
        messageField.setPromptText("Type a message");

        Button sendButton = new Button("Send");
        sendButton.setDefaultButton(true);

        HBox emojiBar = new HBox(6);
        String[] emojis = {"😀", "😂", "❤️", "👍", "😮", "😢", "🔥", "🙏"};
        for (String emoji : emojis) {
            Button button = new Button(emoji);
            button.setStyle("-fx-background-color: rgba(30, 41, 59, 0.8); -fx-text-fill: white; -fx-background-radius: 14;");
            button.setOnAction(ev -> messageField.appendText(emoji));
            emojiBar.getChildren().add(button);
        }

        Runnable sendMessage = () -> {
            String text = messageField.getText().trim();
            if (text.isEmpty()) {
                return;
            }

            if (mode == ChatMode.SERVER) {
                serverOut.println("MSG|SERVER|" + text);
                appendRow(Side.RIGHT, username, text);
            } else {
                String target = recipientBox.getValue();
                if (target == null || target.isBlank()) {
                    statusLabel.setText("Choose a user to talk to.");
                    return;
                }
                serverOut.println("MSG|" + target + "|" + text);
                appendRow(Side.RIGHT, username + " → " + target, text);
            }

            messageField.clear();
        };

        sendButton.setOnAction(event -> sendMessage.run());
        messageField.setOnAction(event -> sendMessage.run());

        HBox titleBar = new HBox(12, chatTitle, recipientHint, recipientBox);
        titleBar.setAlignment(Pos.CENTER_LEFT);

        HBox composer = new HBox(10, messageField, sendButton);
        HBox.setHgrow(messageField, Priority.ALWAYS);

        VBox controls = new VBox(10, titleBar, emojiBar, composer, statusLabel);
        controls.setPadding(new Insets(16));
        controls.setStyle("-fx-background-color: rgba(15, 23, 42, 0.88); -fx-background-radius: 20;");

        VBox chatPane = new VBox(14, controls, chatScroll);
        VBox.setVgrow(chatScroll, Priority.ALWAYS);
        chatPane.setPadding(new Insets(16));
        chatPane.setStyle("-fx-background-color: linear-gradient(to bottom right, #020617, #111827);");

        appendRow(Side.CENTER, "System", mode == ChatMode.SERVER ? "Connected to server bot mode." : "Connected to peer chat mode.");
        return chatPane;
    }

    private void readIncoming() {
        try {
            String line;
            while ((line = serverIn.readLine()) != null) {
                handleIncoming(line);
            }
        } catch (IOException e) {
            Platform.runLater(() -> statusLabel.setText("Disconnected"));
        }
    }

    private void handleIncoming(String line) {
        String[] parts = line.split("\\|", 3);
        if (parts.length == 0) {
            return;
        }

        switch (parts[0]) {
            case "SYS" -> appendRow(Side.CENTER, "System", safePart(parts, 1));
            case "USERLIST" -> updateUsers(safePart(parts, 1));
            case "MSG" -> appendRow(Side.LEFT, safePart(parts, 1), safePart(parts, 2));
            default -> appendRow(Side.CENTER, "System", line);
        }
    }

    private void updateUsers(String csv) {
        Platform.runLater(() -> {
            onlineUsers.clear();
            if (csv != null && !csv.isBlank()) {
                for (String user : csv.split(",")) {
                    String trimmed = user.trim();
                    if (!trimmed.isEmpty() && !trimmed.equals(username)) {
                        onlineUsers.add(trimmed);
                    }
                }
            }
            if (recipientBox != null) {
                recipientBox.setItems(onlineUsers);
                if (!onlineUsers.isEmpty() && recipientBox.getValue() == null) {
                    recipientBox.setValue(onlineUsers.get(0));
                }
            }
        });
    }

    private void appendRow(Side side, String title, String text) {
        if (chatBox == null) {
            return;
        }

        String time = LocalTime.now().format(timeFmt);
        Platform.runLater(() -> {
            HBox wrapper = new HBox();
            VBox bubble = new VBox(4);
            bubble.setMaxWidth(430);
            bubble.setPadding(new Insets(10, 12, 10, 12));
            bubble.setStyle(side == Side.CENTER
                ? "-fx-background-color: rgba(148, 163, 184, 0.18); -fx-background-radius: 18;"
                : "-fx-background-color: linear-gradient(to right, #0f766e, #0891b2); -fx-background-radius: 18;");

            if (side == Side.RIGHT) {
                bubble.setStyle("-fx-background-color: linear-gradient(to right, #2563eb, #7c3aed); -fx-background-radius: 18;");
            }

            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #e2e8f0;");

            Label body = new Label(text);
            body.setWrapText(true);
            body.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 14px;");

            Label timeLabel = new Label(time);
            timeLabel.setStyle("-fx-text-fill: rgba(248, 250, 252, 0.65); -fx-font-size: 10px;");

            bubble.getChildren().addAll(titleLabel, body, timeLabel);
            wrapper.setAlignment(side == Side.RIGHT ? Pos.CENTER_RIGHT : side == Side.CENTER ? Pos.CENTER : Pos.CENTER_LEFT);
            wrapper.getChildren().add(bubble);
            chatBox.getChildren().add(wrapper);
        });
    }

    private String safePart(String[] parts, int index) {
        return parts.length > index ? parts[index] : "";
    }

    @Override
    public void stop() {
        try {
            if (incomingMessages != null) {
                incomingMessages.interrupt();
            }
            if (serverIn != null) {
                serverIn.close();
            }
            if (serverOut != null) {
                serverOut.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}