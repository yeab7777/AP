import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NP extends Application {
    private TextArea textArea = new TextArea();
    private String currentTitle = "Untitled";
    private Connection conn;
    private boolean highlightMode;
    private boolean underlineMode;
    private static final String DB_HOST = envOrDefault("NP_DB_HOST", "localhost");
    private static final String DB_PORT = envOrDefault("NP_DB_PORT", "3306");
    private static final String DB_NAME = envOrDefault("NP_DB_NAME", "notesdb");
    private static final String DB_USER = envOrDefault("NP_DB_USER", "root");
    private static final String DB_PASSWORD = envOrDefault("NP_DB_PASSWORD", "");
    private static final String DB_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME + "?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    @Override
    public void start(Stage stage) {
        connectDBAsync();

        // Menu bar
        MenuBar menuBar = new MenuBar();

        // File menu
        Menu fileMenu = new Menu("File");
        MenuItem openItem = new MenuItem("Open Note");
        openItem.setOnAction(e -> showOpenDialog());
        MenuItem saveItem = new MenuItem("Save Note");
        saveItem.setOnAction(e -> saveNote());
        fileMenu.getItems().addAll(openItem, saveItem);

        // Format menu
        Menu formatMenu = new Menu("Format");
        MenuItem highlightItem = new MenuItem("Highlight");
        highlightItem.setOnAction(e -> {
            highlightMode = !highlightMode;
            refreshTextAreaStyle();
        });
        formatMenu.getItems().addAll(highlightItem);

        // Toolbar with save icon, font selector, size and style toggles
        ToolBar toolBar = new ToolBar();
        Button saveBtn = new Button("Save");
        saveBtn.setOnAction(e -> saveNote());

        ComboBox<String> fontBox = new ComboBox<>();
        fontBox.getItems().addAll("Arial", "Times New Roman", "Courier New", "Verdana", "Georgia");
        fontBox.setValue("Arial");

        ComboBox<Integer> sizeBox = new ComboBox<>();
        sizeBox.getItems().addAll(10, 12, 14, 16, 18, 20);
        sizeBox.setValue(12);

        ToggleButton boldBtn = new ToggleButton("B");
        boldBtn.setStyle("-fx-font-weight: bold;");
        ToggleButton italicBtn = new ToggleButton("I");
        italicBtn.setStyle("-fx-font-style: italic;");
        ToggleButton underlineBtn = new ToggleButton("U");

        // when any style control changes, update the TextArea font/style
        Runnable apply = () -> applyTextStyle(fontBox, sizeBox, boldBtn, italicBtn, underlineBtn);
        fontBox.setOnAction(e -> apply.run());
        sizeBox.setOnAction(e -> apply.run());
        boldBtn.setOnAction(e -> apply.run());
        italicBtn.setOnAction(e -> apply.run());
        underlineBtn.setOnAction(e -> apply.run());

        toolBar.getItems().addAll(saveBtn, new Separator(), fontBox, sizeBox, new Separator(), boldBtn, italicBtn, underlineBtn);

        // Bookmark menu
        Menu bookmarkMenu = new Menu("Bookmark");
        MenuItem bookmarkItem = new MenuItem("Add Bookmark");
        bookmarkItem.setOnAction(e -> bookmarkNote());
        bookmarkMenu.getItems().add(bookmarkItem);

        menuBar.getMenus().addAll(fileMenu, formatMenu, bookmarkMenu);

        BorderPane root = new BorderPane();
        VBox topContainer = new VBox(menuBar, toolBar);
        root.setTop(topContainer);

        // Create ruled lines canvas behind the TextArea to mimic notebook paper
        Canvas linesCanvas = new Canvas(700, 400);
        StackPane centerStack = new StackPane();
        centerStack.getChildren().addAll(linesCanvas, textArea);
        root.setCenter(centerStack);

        root.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #d9e7f2 0%, #f2e6d8 45%, #f6f0df 100%);" +
            "-fx-padding: 12;"
        );
        menuBar.setStyle("-fx-background-color: linear-gradient(to bottom, #214a63 0%, #1d3f54 100%);");
        toolBar.setStyle("-fx-background-color: rgba(255, 255, 255, 0.82); -fx-border-color: #adc4d4; -fx-border-width: 0 0 1 0;");
        saveBtn.setStyle("-fx-background-color: #2f7f6f; -fx-text-fill: white; -fx-font-weight: bold;");
        textArea.setWrapText(true);
        refreshTextAreaStyle();

        // Status bar with current title and save status
        HBox statusBar = new HBox(12);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setStyle("-fx-background-color: linear-gradient(to right, rgba(255,255,255,0.6), rgba(255,255,255,0.4)); -fx-padding: 6; -fx-border-color: #d2dfe6; -fx-border-width: 1 0 0 0;");
        Label titleLabel = new Label("Title: " + currentTitle);
        Label statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-text-fill: #2f7f6f; -fx-font-weight: bold;");
        statusBar.getChildren().addAll(titleLabel, statusLabel);
        root.setBottom(statusBar);

        Scene scene = new Scene(root, 700, 500);
        // Ctrl+S shortcut
        scene.addEventHandler(KeyEvent.KEY_PRESSED, ev -> {
            if (ev.isControlDown() && ev.getCode() == KeyCode.S) {
                saveNote();
                ev.consume();
            }
        });
        stage.setScene(scene);
        stage.setTitle("JavaFX Notepad");
        stage.show();

        // Draw ruled lines and handle resizing
        drawLines(linesCanvas, centerStack.getWidth(), centerStack.getHeight());
        centerStack.widthProperty().addListener((obs, oldV, newV) -> {
            linesCanvas.setWidth(newV.doubleValue());
            drawLines(linesCanvas, newV.doubleValue(), centerStack.getHeight());
        });
        centerStack.heightProperty().addListener((obs, oldV, newV) -> {
            linesCanvas.setHeight(newV.doubleValue());
            drawLines(linesCanvas, centerStack.getWidth(), newV.doubleValue());
        });
    }

    private void connectDBAsync() {
        new Thread(() -> connectDB(), "db-connect-thread").start();
    }

    private void showOpenDialog() {
        new Thread(() -> {
            try {
                if (conn == null) connectDB();
                if (conn == null) return;
                PreparedStatement stmt = conn.prepareStatement("SELECT title FROM notes");
                ResultSet rs = stmt.executeQuery();
                List<String> titles = new ArrayList<>();
                while (rs.next()) titles.add(rs.getString("title"));
                rs.close();
                stmt.close();
                Platform.runLater(() -> {
                    Dialog<String> dialog = new Dialog<>();
                    dialog.setTitle("Open Note");
                    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
                    ListView<String> listView = new ListView<>();
                    listView.getItems().addAll(titles);
                    dialog.getDialogPane().setContent(listView);
                    Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
                    okButton.setDisable(true);
                    listView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> okButton.setDisable(newV == null));
                    dialog.setResultConverter(bt -> bt == ButtonType.OK ? listView.getSelectionModel().getSelectedItem() : null);
                    Optional<String> res = dialog.showAndWait();
                    res.ifPresent(t -> loadContentForTitle(t));
                });
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadContentForTitle(String title) {
        new Thread(() -> {
            try {
                if (conn == null) connectDB();
                if (conn == null) return;
                PreparedStatement ps = conn.prepareStatement("SELECT content FROM notes WHERE title = ?");
                ps.setString(1, title);
                ResultSet r2 = ps.executeQuery();
                String content = "";
                if (r2.next()) content = r2.getString("content");
                r2.close();
                ps.close();
                final String finalContent = content;
                Platform.runLater(() -> {
                    currentTitle = title;
                    textArea.setText(finalContent);
                });
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void connectDB() {
        try {
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            try (Statement s = conn.createStatement()) {
                s.executeUpdate("CREATE TABLE IF NOT EXISTS notes (id INT AUTO_INCREMENT PRIMARY KEY, title VARCHAR(255) UNIQUE, content TEXT, bookmarked BOOLEAN DEFAULT FALSE)");
            } catch (SQLException ex) {
                ex.printStackTrace();
                showError("Database Setup Error", "Could not create notes table.", ex.getMessage());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError(
                "Database Connection Error",
                "Could not connect to MySQL.",
                "Check MySQL server and credentials. You can set these environment variables:\n" +
                    "NP_DB_HOST, NP_DB_PORT, NP_DB_NAME, NP_DB_USER, NP_DB_PASSWORD\n\n" +
                    "Current target: " + DB_HOST + ":" + DB_PORT + "/" + DB_NAME + " as user '" + DB_USER + "'\n\n" +
                    e.getMessage()
            );
        }
    }

    private static String envOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        if (value == null) return fallback;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private synchronized void saveNote() {
        try {
            if (conn == null || conn.isClosed()) connectDB();
            if (conn == null) {
                showError("Save Failed", "Database connection unavailable.", "Note was not saved.");
                return;
            }
            String suggestedTitle = (currentTitle == null || currentTitle.isBlank()) ? "Untitled" : currentTitle;
            TextInputDialog dialog = new TextInputDialog(suggestedTitle);
            dialog.setTitle("Save Note");
            dialog.setHeaderText("Name your note");
            dialog.setContentText("Title:");
            Optional<String> result = dialog.showAndWait();
            if (result.isEmpty()) return;

            String title = result.get().trim();
            if (title.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Please enter a valid note title.", ButtonType.OK);
                alert.setHeaderText("Cannot save note");
                alert.showAndWait();
                return;
            }

            String upsertSql = "INSERT INTO notes (title, content) VALUES (?, ?) ON DUPLICATE KEY UPDATE content = VALUES(content)";
            try (PreparedStatement stmt = conn.prepareStatement(upsertSql)) {
                stmt.setString(1, title);
                stmt.setString(2, textArea.getText());
                stmt.executeUpdate();
            }

            currentTitle = title;

            // update status bar (run on UI thread)
            Platform.runLater(() -> {
                Stage s = (Stage) textArea.getScene().getWindow();
                BorderPane root = (BorderPane) s.getScene().getRoot();
                HBox statusBar = (HBox) root.getBottom();
                Label titleLabel = (Label) statusBar.getChildren().get(0);
                Label statusLabel = (Label) statusBar.getChildren().get(1);
                titleLabel.setText("Title: " + title);
                statusLabel.setText("Saved");
            });
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Saved to database as: " + title, ButtonType.OK);
            alert.setHeaderText("Note saved");
            alert.showAndWait();
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Save Failed", "Could not save note to database.", e.getMessage());
        }
    }

    private void drawLines(Canvas canvas, double width, double height) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, width, height);
        g.setStroke(Color.web("#e0d6b5"));
        g.setLineWidth(1);
        double lineHeight = 22; // distance between lines
        for (double y = lineHeight; y < height; y += lineHeight) {
            g.strokeLine(8, y, Math.max(8, width - 8), y);
        }
        // left margin vertical line
        g.setStroke(Color.web("#d6c79a"));
        g.setLineWidth(2);
        g.strokeLine(40, 6, 40, height - 6);
    }

    private void applyTextStyle(ComboBox<String> fontBox, ComboBox<Integer> sizeBox,
                                ToggleButton boldBtn, ToggleButton italicBtn, ToggleButton underlineBtn) {
        String family = fontBox.getValue();
        int size = sizeBox.getValue() == null ? 12 : sizeBox.getValue();
        FontWeight weight = boldBtn.isSelected() ? FontWeight.BOLD : FontWeight.NORMAL;
        FontPosture posture = italicBtn.isSelected() ? FontPosture.ITALIC : FontPosture.REGULAR;
        textArea.setFont(Font.font(family, weight, posture, size));
        underlineMode = underlineBtn.isSelected();
        refreshTextAreaStyle();
    }

    private void bookmarkNote() {
        try {
            if (conn == null || conn.isClosed()) connectDB();
            if (conn == null) {
                System.err.println("DB connection unavailable; cannot bookmark.");
                return;
            }
            if (currentTitle == null || currentTitle.isBlank() || "Untitled".equalsIgnoreCase(currentTitle)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Save the note first so it has a title.", ButtonType.OK);
                alert.setHeaderText("No saved note selected");
                alert.showAndWait();
                return;
            }
            String sql = "UPDATE notes SET bookmarked = TRUE WHERE title = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, currentTitle);
            stmt.executeUpdate();
            System.out.println("Note bookmarked!");
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Bookmark Failed", "Could not bookmark this note.", e.getMessage());
        }
    }

    private void refreshTextAreaStyle() {
        String background = highlightMode ? "#fff3b2" : "#fffdf3";
        String style =
            "-fx-control-inner-background: " + background + ";" +
            "-fx-background-color: " + background + ";" +
            "-fx-background-insets: 0;" +
            "-fx-border-color: #d1b87a;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-font-family: 'Consolas';" +
            "-fx-font-size: 14px;" +
            "-fx-text-fill: #2d2c28;" +
            (underlineMode ? "-fx-underline: true;" : "");
        textArea.setStyle(style);
    }

    private void showError(String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, content, ButtonType.OK);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.showAndWait();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
