import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;

import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

class Card {
    private final String suit;
    private final String rank;

    public Card(String suit, String rank) {
        this.suit = suit;
        this.rank = rank;
    }

    public String getSuit() {
        return suit;
    }

    public String getRank() {
        return rank;
    }

    public int getRankValue() {
        switch (rank) {
            case "J":
                return 11;
            case "Q":
                return 12;
            case "K":
                return 13;
            case "A":
                return 14;
            default:
                return Integer.parseInt(rank);
        }
    }

    @Override
    public String toString() {
        return rank + " of " + suit;
    }
}

class Deck {
    private final List<Card> cards;

    public Deck() {
        String[] suits = {"Hearts", "Diamonds", "Clubs", "Spades"};
        String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"};
        cards = new ArrayList<>();
        for (String suit : suits) {
            for (String rank : ranks) {
                cards.add(new Card(suit, rank));
            }
        }
        Collections.shuffle(cards);
    }

    public Card dealCard() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("No cards left in the deck.");
        }
        return cards.remove(0);
    }
}

class Player {
    private final String name;
    private final List<Card> hand;
    private int chips;

    public Player(String name, int chips) {
        this.name = name;
        this.chips = chips;
        this.hand = new ArrayList<>();
    }

    public void addCard(Card card) {
        hand.add(card);
    }

    public void clearHand() {
        hand.clear();
    }

    public void adjustChips(int delta) {
        this.chips += delta;
    }

    public String getName() {
        return name;
    }

    public int getChips() {
        return chips;
    }

    public String handText() {
        return hand.toString();
    }

    public List<Card> getHand() {
        return hand;
    }
}

class HandValue implements Comparable<HandValue> {
    private final int category;
    private final List<Integer> tiebreakers;
    private final String description;

    public HandValue(int category, List<Integer> tiebreakers, String description) {
        this.category = category;
        this.tiebreakers = tiebreakers;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public int compareTo(HandValue other) {
        if (category != other.category) {
            return Integer.compare(category, other.category);
        }

        int limit = Math.min(tiebreakers.size(), other.tiebreakers.size());
        for (int i = 0; i < limit; i++) {
            int comparison = Integer.compare(tiebreakers.get(i), other.tiebreakers.get(i));
            if (comparison != 0) {
                return comparison;
            }
        }

        return Integer.compare(tiebreakers.size(), other.tiebreakers.size());
    }
}

class HandEvaluator {
    public static HandValue evaluateBestHand(List<Card> cards) {
        if (cards.size() < 5) {
            throw new IllegalArgumentException("Need at least 5 cards to evaluate a hand.");
        }

        HandValue best = null;
        int size = cards.size();
        for (int a = 0; a < size - 4; a++) {
            for (int b = a + 1; b < size - 3; b++) {
                for (int c = b + 1; c < size - 2; c++) {
                    for (int d = c + 1; d < size - 1; d++) {
                        for (int e = d + 1; e < size; e++) {
                            List<Card> fiveCards = Arrays.asList(
                                    cards.get(a),
                                    cards.get(b),
                                    cards.get(c),
                                    cards.get(d),
                                    cards.get(e));
                            HandValue current = evaluateFiveCards(fiveCards);
                            if (best == null || current.compareTo(best) > 0) {
                                best = current;
                            }
                        }
                    }
                }
            }
        }

        return best;
    }

    private static HandValue evaluateFiveCards(List<Card> cards) {
        List<Integer> ranks = new ArrayList<>();
        Map<Integer, Integer> counts = new HashMap<>();
        Map<String, Integer> suitCounts = new HashMap<>();

        for (Card card : cards) {
            int value = card.getRankValue();
            ranks.add(value);
            counts.put(value, counts.getOrDefault(value, 0) + 1);
            suitCounts.put(card.getSuit(), suitCounts.getOrDefault(card.getSuit(), 0) + 1);
        }

        ranks.sort(Collections.reverseOrder());
        boolean flush = suitCounts.containsValue(5);
        List<Integer> straightRanks = straightHighCards(new HashSet<>(ranks));
        Integer straightHigh = straightRanks.isEmpty() ? null : straightRanks.get(0);

        List<Map.Entry<Integer, Integer>> grouped = new ArrayList<>(counts.entrySet());
        grouped.sort((left, right) -> {
            int byCount = Integer.compare(right.getValue(), left.getValue());
            if (byCount != 0) {
                return byCount;
            }
            return Integer.compare(right.getKey(), left.getKey());
        });

        if (flush && straightHigh != null) {
            return new HandValue(8, Collections.singletonList(straightHigh), "Straight Flush");
        }

        if (grouped.get(0).getValue() == 4) {
            int fourKind = grouped.get(0).getKey();
            int kicker = grouped.get(1).getKey();
            return new HandValue(7, Arrays.asList(fourKind, kicker), "Four of a Kind");
        }

        if (grouped.get(0).getValue() == 3 && grouped.size() > 1 && grouped.get(1).getValue() == 2) {
            return new HandValue(6, Arrays.asList(grouped.get(0).getKey(), grouped.get(1).getKey()), "Full House");
        }

        if (flush) {
            return new HandValue(5, ranks, "Flush");
        }

        if (straightHigh != null) {
            return new HandValue(4, Collections.singletonList(straightHigh), "Straight");
        }

        if (grouped.get(0).getValue() == 3) {
            int trips = grouped.get(0).getKey();
            List<Integer> kickers = new ArrayList<>();
            for (int i = 1; i < grouped.size(); i++) {
                kickers.add(grouped.get(i).getKey());
            }
            kickers.sort(Collections.reverseOrder());
            List<Integer> values = new ArrayList<>();
            values.add(trips);
            values.addAll(kickers);
            return new HandValue(3, values, "Three of a Kind");
        }

        if (grouped.get(0).getValue() == 2 && grouped.size() > 1 && grouped.get(1).getValue() == 2) {
            int highPair = Math.max(grouped.get(0).getKey(), grouped.get(1).getKey());
            int lowPair = Math.min(grouped.get(0).getKey(), grouped.get(1).getKey());
            int kicker = 0;
            for (Map.Entry<Integer, Integer> entry : grouped) {
                if (entry.getValue() == 1) {
                    kicker = entry.getKey();
                    break;
                }
            }
            return new HandValue(2, Arrays.asList(highPair, lowPair, kicker), "Two Pair");
        }

        if (grouped.get(0).getValue() == 2) {
            int pair = grouped.get(0).getKey();
            List<Integer> kickers = new ArrayList<>();
            for (int i = 1; i < grouped.size(); i++) {
                kickers.add(grouped.get(i).getKey());
            }
            kickers.sort(Collections.reverseOrder());
            List<Integer> values = new ArrayList<>();
            values.add(pair);
            values.addAll(kickers);
            return new HandValue(1, values, "One Pair");
        }

        return new HandValue(0, ranks, "High Card");
    }

    private static List<Integer> straightHighCards(Set<Integer> values) {
        List<Integer> sorted = new ArrayList<>(values);
        Collections.sort(sorted);

        if (sorted.contains(14)) {
            sorted.add(1);
            Collections.sort(sorted);
        }

        int run = 1;
        int bestHigh = -1;
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).equals(sorted.get(i - 1))) {
                continue;
            }

            if (sorted.get(i) == sorted.get(i - 1) + 1) {
                run++;
                if (run >= 5) {
                    bestHigh = sorted.get(i);
                }
            } else {
                run = 1;
            }
        }

        if (bestHigh == -1) {
            return Collections.emptyList();
        }

        return Collections.singletonList(bestHigh == 1 ? 5 : bestHigh);
    }
}

class DatabaseService {
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/poker_db?createDatabaseIfNotExist=true&serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";
    private boolean available = false;
    private String lastError = "";
    private String dbUrl = DEFAULT_URL;
    private String dbUser = DEFAULT_USER;
    private String dbPassword = DEFAULT_PASSWORD;

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        if (value != null && !value.isBlank()) {
            return value;
        }
        String property = System.getProperty(name);
        if (property != null && !property.isBlank()) {
            return property;
        }
        return fallback;
    }

    public void initialize() {
        connect();
    }

    public boolean connect() {
        return connectWith(DEFAULT_URL, DEFAULT_USER, DEFAULT_PASSWORD);
    }

    public String defaultUrl() {
        return DEFAULT_URL;
    }

    public String defaultUser() {
        return DEFAULT_USER;
    }

    public String defaultPassword() {
        return DEFAULT_PASSWORD;
    }

    public boolean connectWith(String url, String user, String password) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            dbUrl = normalizeMysqlUrl((url == null || url.isBlank()) ? DEFAULT_URL : url);
            dbUser = (user == null || user.isBlank()) ? DEFAULT_USER : user;
            dbPassword = (password == null) ? DEFAULT_PASSWORD : password;

            try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS hands ("
                                + "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                                + "played_at VARCHAR(64) NOT NULL,"
                                + "player1_name VARCHAR(100) NOT NULL,"
                                + "player1_cards TEXT NOT NULL,"
                                + "player1_hand VARCHAR(50) NOT NULL,"
                                + "player2_name VARCHAR(100) NOT NULL,"
                                + "player2_cards TEXT NOT NULL,"
                                + "player2_hand VARCHAR(50) NOT NULL,"
                                + "community_cards TEXT NOT NULL,"
                                + "winner_name VARCHAR(100) NOT NULL,"
                                + "winner_hand VARCHAR(50) NOT NULL,"
                                + "result_text TEXT NOT NULL"
                                + ")");
                available = true;
                lastError = "";
                return true;
            }
        } catch (Exception e) {
            available = false;
            lastError = safeMessage(e);
            return false;
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public String getLastError() {
        return lastError;
    }

    public void setOfflineMode(String reason) {
        available = false;
        lastError = (reason == null || reason.isBlank()) ? "Offline mode selected." : reason;
    }

    public boolean saveHand(Player player1, Player player2, List<Card> communityCards, HandValue player1Value, HandValue player2Value, String winnerText, String resultText) {
        if (!available) {
            return false;
        }

        String sql = "INSERT INTO hands(played_at, player1_name, player1_cards, player1_hand, player2_name, player2_cards, player2_hand, community_cards, winner_name, winner_hand, result_text) "
                + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        if (insertHand(sql, player1, player2, communityCards, player1Value, player2Value, winnerText, resultText)) {
            return true;
        }

        // Reconnect once and retry insert when the first write fails.
        if (connectWith(dbUrl, dbUser, dbPassword)) {
            return insertHand(sql, player1, player2, communityCards, player1Value, player2Value, winnerText, resultText);
        }

        return false;
    }

    public List<SavedHandRecord> loadRecentHands(int limit) {
        List<SavedHandRecord> hands = new ArrayList<>();
        if (!available) {
            return hands;
        }

        String sql = "SELECT id, played_at, player1_name, player1_hand, player2_name, player2_hand, winner_name, result_text "
                + "FROM hands ORDER BY id DESC LIMIT ?";

        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, Math.max(1, limit));

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    hands.add(new SavedHandRecord(
                            resultSet.getLong("id"),
                            resultSet.getString("played_at"),
                            resultSet.getString("player1_name"),
                            resultSet.getString("player1_hand"),
                            resultSet.getString("player2_name"),
                            resultSet.getString("player2_hand"),
                            resultSet.getString("winner_name"),
                            resultSet.getString("result_text")));
                }
            }
        } catch (SQLException e) {
            lastError = safeMessage(e);
            available = false;
        }

        return hands;
    }

    private boolean insertHand(String sql, Player player1, Player player2, List<Card> communityCards, HandValue player1Value, HandValue player2Value, String winnerText, String resultText) {
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            preparedStatement.setString(2, player1.getName());
            preparedStatement.setString(3, player1.handText());
            preparedStatement.setString(4, player1Value.getDescription());
            preparedStatement.setString(5, player2.getName());
            preparedStatement.setString(6, player2.handText());
            preparedStatement.setString(7, player2Value.getDescription());
            preparedStatement.setString(8, communityCards.toString());
            preparedStatement.setString(9, winnerText);
            preparedStatement.setString(10, player1Value.compareTo(player2Value) >= 0 ? player1Value.getDescription() : player2Value.getDescription());
            preparedStatement.setString(11, resultText);
            preparedStatement.executeUpdate();
            return true;
        } catch (SQLException e) {
            lastError = safeMessage(e);
            available = false;
            return false;
        }
    }

    private String normalizeMysqlUrl(String rawUrl) {
        String normalized = rawUrl.trim();
        if (!normalized.startsWith("jdbc:mysql://")) {
            return DEFAULT_URL;
        }

        normalized = ensureUrlParam(normalized, "serverTimezone=UTC");
        normalized = ensureUrlParam(normalized, "allowPublicKeyRetrieval=true");
        normalized = ensureUrlParam(normalized, "useSSL=false");
        normalized = ensureUrlParam(normalized, "createDatabaseIfNotExist=true");
        return normalized;
    }

    private String ensureUrlParam(String url, String param) {
        String key = param.substring(0, param.indexOf('='));
        if (url.toLowerCase().contains((key + "=").toLowerCase())) {
            return url;
        }
        if (url.contains("?")) {
            return url + "&" + param;
        }
        return url + "?" + param;
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }
}

class SavedHandRecord {
    private final long id;
    private final String playedAt;
    private final String player1Name;
    private final String player1Hand;
    private final String player2Name;
    private final String player2Hand;
    private final String winnerName;
    private final String resultText;

    public SavedHandRecord(long id, String playedAt, String player1Name, String player1Hand, String player2Name, String player2Hand, String winnerName, String resultText) {
        this.id = id;
        this.playedAt = playedAt;
        this.player1Name = player1Name;
        this.player1Hand = player1Hand;
        this.player2Name = player2Name;
        this.player2Hand = player2Hand;
        this.winnerName = winnerName;
        this.resultText = resultText;
    }

    public long getId() {
        return id;
    }

    public String getPlayedAt() {
        return playedAt;
    }

    public String getPlayer1Name() {
        return player1Name;
    }

    public String getPlayer1Hand() {
        return player1Hand;
    }

    public String getPlayer2Name() {
        return player2Name;
    }

    public String getPlayer2Hand() {
        return player2Hand;
    }

    public String getWinnerName() {
        return winnerName;
    }

    public String getResultText() {
        return resultText;
    }
}

public class PokerGame extends Application {
    private static final int TOTAL_ROUNDS = 5;
    private static final int ROUND_POT = 100;
    private static final int SMALL_BLIND = 10;
    private static final int BIG_BLIND = 20;

    private final DatabaseService databaseService = new DatabaseService();
    private final Label player1Label = new Label();
    private final Label player2Label = new Label();
    private final Label communityLabel = new Label();
    private final Label winnerLabel = new Label();
    private final Label roundLabel = new Label("Round: 0 / " + TOTAL_ROUNDS);
    private final Label potLabel = new Label("Pot: " + ROUND_POT);
    private final Label actionLabel = new Label("Action: Waiting to start match");
    private final Label statusLabel = new Label("Ready");
    private final TextArea historyArea = new TextArea();
    private final TableView<SavedHandRecord> savedHandsTable = new TableView<>();
    private final HBox player1CardsBox = createCardsRow();
    private final HBox player2CardsBox = createCardsRow();
    private final HBox communityCardsBox = createCardsRow();
    private final Button startMatchButton = new Button("Start 5-Round Match");
    private final Button resetTableButton = new Button("Reset Table");

    private String localPlayerName = "Player1";
    private int localPlayerChips = 1000;
    private Player currentPlayer1;
    private Player currentPlayer2;
    private int currentRound = 0;
    private boolean matchRunning = false;
    private Stage primaryStage;
    private Scene gameScene;

    private enum BettingAction {
        FOLD,
        CHECK,
        CALL,
        RAISE
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("Poker Game");
        databaseService.initialize();
        gameScene = buildGameScene();
        primaryStage.setScene(buildLoginScene());
        primaryStage.show();
    }

    private Scene buildGameScene() {
        Label title = new Label("Poker Game - Texas Hold'em Arena");

        startMatchButton.setOnAction(event -> playMatch());
        resetTableButton.setOnAction(event -> resetTable());

        historyArea.setEditable(false);
        historyArea.setPrefRowCount(10);
        historyArea.setWrapText(true);
        savedHandsTable.setPrefHeight(180);
        savedHandsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        savedHandsTable.getColumns().add(createTableColumn("ID", record -> new SimpleStringProperty(String.valueOf(record.getId()))));
        savedHandsTable.getColumns().add(createTableColumn("Played At", record -> new SimpleStringProperty(record.getPlayedAt())));
        savedHandsTable.getColumns().add(createTableColumn("P1", record -> new SimpleStringProperty(record.getPlayer1Name() + " - " + record.getPlayer1Hand())));
        savedHandsTable.getColumns().add(createTableColumn("P2", record -> new SimpleStringProperty(record.getPlayer2Name() + " - " + record.getPlayer2Hand())));
        savedHandsTable.getColumns().add(createTableColumn("Winner", record -> new SimpleStringProperty(record.getWinnerName())));
        savedHandsTable.getColumns().add(createTableColumn("Result", record -> new SimpleStringProperty(record.getResultText())));

        updateDatabaseStatusText();

        VBox player1Section = new VBox(6, player1Label, player1CardsBox);
        VBox player2Section = new VBox(6, player2Label, player2CardsBox);
        VBox communitySection = new VBox(6, communityLabel, communityCardsBox);

        HBox topButtons = new HBox(10, startMatchButton, resetTableButton);
        topButtons.setAlignment(Pos.CENTER_LEFT);

        HBox matchInfo = new HBox(14, roundLabel, potLabel, statusLabel);
        matchInfo.setAlignment(Pos.CENTER_LEFT);
        matchInfo.setPadding(new Insets(8, 10, 8, 10));
        matchInfo.setStyle("-fx-background-color: rgba(6, 28, 20, 0.72); -fx-background-radius: 10;");

        Label savedHandsLabel = new Label("Saved Hands");
        savedHandsLabel.setStyle("-fx-text-fill: #ffe6b5; -fx-font-size: 16px; -fx-font-weight: bold;");

        VBox tablePanel = new VBox(12, title, topButtons, matchInfo, actionLabel, player1Section, player2Section, communitySection, winnerLabel, historyArea, savedHandsLabel, savedHandsTable);
        tablePanel.setPadding(new Insets(20));
        tablePanel.setStyle("-fx-background-color: linear-gradient(to bottom right, #0f6a3f, #08482e 55%, #1a2233);"
                + "-fx-background-radius: 18;"
                + "-fx-border-color: rgba(255, 215, 135, 0.5);"
                + "-fx-border-radius: 18;"
                + "-fx-border-width: 1.2;");

        VBox root = new VBox(tablePanel);
        root.setPadding(new Insets(18));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #22190e, #132018 45%, #0d1724); -fx-font-size: 14px;");

        title.setStyle("-fx-text-fill: #ffe6b5; -fx-font-size: 26px; -fx-font-weight: bold;");
        startMatchButton.setStyle("-fx-background-color: linear-gradient(to bottom, #ffd784, #d18d1d); -fx-font-weight: bold;");
        resetTableButton.setStyle("-fx-background-color: linear-gradient(to bottom, #b8dfff, #5f95c7); -fx-font-weight: bold;");
        player1Label.setStyle("-fx-text-fill: #fff4dc; -fx-font-size: 15px; -fx-font-weight: bold;");
        player2Label.setStyle("-fx-text-fill: #fff4dc; -fx-font-size: 15px; -fx-font-weight: bold;");
        communityLabel.setStyle("-fx-text-fill: #fff4dc; -fx-font-size: 15px; -fx-font-weight: bold;");
        winnerLabel.setStyle("-fx-text-fill: #ffe48b; -fx-font-size: 15px; -fx-font-weight: bold;");
        roundLabel.setStyle("-fx-text-fill: #f4f0ff; -fx-font-weight: bold;");
        potLabel.setStyle("-fx-text-fill: #f4f0ff; -fx-font-weight: bold;");
        actionLabel.setStyle("-fx-text-fill: #fbe6b0; -fx-font-weight: bold;");
        statusLabel.setStyle("-fx-text-fill: #b8f3ca; -fx-font-weight: bold;");
        historyArea.setStyle("-fx-control-inner-background: #f8f3e7; -fx-font-family: Consolas; -fx-font-size: 12px;");

        player1Label.setText(localPlayerName + " (" + localPlayerChips + " chips)");
        player2Label.setText("Dealer Bot (1000 chips)");
        communityLabel.setText("Community Cards");
        winnerLabel.setText("Winner: Start the match to deal Round 1.");
        roundLabel.setText("Round: 0 / " + TOTAL_ROUNDS);
        potLabel.setText("Pot: 0");
        refreshSavedHandsTable();

        return new Scene(root, 920, 620);
    }

    private Scene buildLoginScene() {
        Label title = new Label("Welcome To The Poker Arena");
        Label subtitle = new Label("Set up your profile. The game will connect directly to local XAMPP MySQL database poker_db.");

        TextField nameField = new TextField();
        nameField.setText(localPlayerName);
        nameField.setPromptText("Player name");

        TextField coinsField = new TextField();
        coinsField.setText(String.valueOf(localPlayerChips));
        coinsField.setPromptText("Starting chips");

        Label loginStatus = new Label("Enter details, then connect directly to poker_db.");
        loginStatus.setStyle("-fx-text-fill: #dbe8ff;");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        Label nameLabel = new Label("Name");
        Label chipsLabel = new Label("Starting chips");
        nameLabel.setStyle("-fx-text-fill: #eef3ff;");
        chipsLabel.setStyle("-fx-text-fill: #eef3ff;");

        form.add(nameLabel, 0, 0);
        form.add(nameField, 1, 0);
        form.add(chipsLabel, 0, 1);
        form.add(coinsField, 1, 1);
        form.setStyle("-fx-background-color: rgba(14, 25, 44, 0.68); -fx-background-radius: 12; -fx-padding: 16;");

        Button connectAndPlayButton = new Button("Connect To poker_db And Enter Game");
        Button playOfflineButton = new Button("Enter Game Offline");

        connectAndPlayButton.setOnAction(e -> {
            if (!applyLoginInputs(nameField.getText(), coinsField.getText(), loginStatus)) {
                return;
            }

            boolean connected = databaseService.connect();
            if (!connected) {
                loginStatus.setText("Database error: " + databaseService.getLastError() + " | You can still use Offline mode.");
                return;
            }

            startGameScene();
        });

        playOfflineButton.setOnAction(e -> {
            if (!applyLoginInputs(nameField.getText(), coinsField.getText(), loginStatus)) {
                return;
            }
            databaseService.setOfflineMode("Offline mode selected by player.");
            startGameScene();
        });

        HBox buttons = new HBox(10, connectAndPlayButton, playOfflineButton);
        buttons.setAlignment(Pos.CENTER_LEFT);

        VBox leftPanel = new VBox(12, title, subtitle, form, buttons, loginStatus);
        leftPanel.setPrefWidth(620);

        VBox rightPanel = new VBox(10,
                new Label("Game Features"),
                new Label("- Full 5-round Texas Hold'em match"),
                new Label("- Hand evaluator with tie breakers"),
                new Label("- Per-round history and winner tracking"),
                new Label("- MySQL hand logging when connected"));
        rightPanel.setPadding(new Insets(18));
        rightPanel.setStyle("-fx-background-color: rgba(255, 233, 179, 0.14); -fx-background-radius: 12;");
        rightPanel.getChildren().forEach(node -> {
            if (node instanceof Label) {
                ((Label) node).setStyle("-fx-text-fill: #f4f2dd; -fx-font-size: 14px;");
            }
        });

        HBox content = new HBox(18, leftPanel, rightPanel);
        content.setPadding(new Insets(26));
        content.setAlignment(Pos.CENTER);

        VBox root = new VBox(content);
        root.setStyle("-fx-background-color: radial-gradient(radius 130%, #253d63, #1c2f4f 30%, #1f2a32 72%, #130f0b 100%);");

        title.setStyle("-fx-text-fill: #ffe6b5; -fx-font-size: 34px; -fx-font-weight: bold;");
        subtitle.setStyle("-fx-text-fill: #c7d7ff; -fx-font-size: 15px;");
        connectAndPlayButton.setStyle("-fx-background-color: linear-gradient(to bottom, #ffd697, #cf8e2a); -fx-font-weight: bold;");
        playOfflineButton.setStyle("-fx-background-color: linear-gradient(to bottom, #a8daf2, #5f8fb8); -fx-font-weight: bold;");

        return new Scene(root, 920, 620);
    }

    private boolean applyLoginInputs(String nameInput, String chipsInput, Label feedbackLabel) {
        String name = nameInput == null ? "" : nameInput.trim();
        if (name.isEmpty()) {
            feedbackLabel.setText("Please enter a player name.");
            return false;
        }

        int chips;
        try {
            chips = Integer.parseInt(chipsInput.trim());
        } catch (NumberFormatException ex) {
            feedbackLabel.setText("Starting chips must be a number.");
            return false;
        }

        if (chips < 100) {
            feedbackLabel.setText("Starting chips must be at least 100.");
            return false;
        }

        localPlayerName = name;
        localPlayerChips = chips;
        return true;
    }

    private void startGameScene() {
        player1Label.setText(localPlayerName + " (" + localPlayerChips + " chips)");
        player2Label.setText("Dealer Bot (1000 chips)");
        resetTable();
        updateDatabaseStatusText();
        refreshSavedHandsTable();
        primaryStage.setScene(gameScene);
    }

    private void resetTable() {
        if (matchRunning) {
            return;
        }

        currentRound = 0;
        roundLabel.setText("Round: 0 / " + TOTAL_ROUNDS);
        potLabel.setText("Pot: 0");
        actionLabel.setText("Action: Waiting to start match");
        communityLabel.setText("Community Cards");
        winnerLabel.setText("Winner: Start the match to deal Round 1.");
        player1CardsBox.getChildren().clear();
        player2CardsBox.getChildren().clear();
        communityCardsBox.getChildren().clear();
        historyArea.clear();
        updateDatabaseStatusText();
    }

    private void playMatch() {
        if (matchRunning) {
            return;
        }

        currentPlayer1 = new Player(localPlayerName, localPlayerChips);
        currentPlayer2 = new Player("Dealer Bot", 1000);
        currentRound = 1;
        matchRunning = true;
        startMatchButton.setDisable(true);
        resetTableButton.setDisable(true);

        player1Label.setText(currentPlayer1.getName() + " (" + currentPlayer1.getChips() + " chips)");
        player2Label.setText(currentPlayer2.getName() + " (" + currentPlayer2.getChips() + " chips)");
        statusLabel.setText("Match running...");
        actionLabel.setText("Action: Posting blinds");
        historyArea.clear();

        playNextRound();
    }

    private void playNextRound() {
        if (currentPlayer1.getChips() <= 0 || currentPlayer2.getChips() <= 0) {
            finishMatch();
            return;
        }

        if (currentRound > TOTAL_ROUNDS) {
            finishMatch();
            return;
        }

        playRound(currentPlayer1, currentPlayer2, currentRound);

        currentRound++;
        PauseTransition pause = new PauseTransition(Duration.seconds(1.35));
        pause.setOnFinished(event -> playNextRound());
        pause.play();
    }

    private void finishMatch() {
        matchRunning = false;
        startMatchButton.setDisable(false);
        resetTableButton.setDisable(false);

        String finalWinner;
        if (currentPlayer1.getChips() > currentPlayer2.getChips()) {
            finalWinner = currentPlayer1.getName() + " wins the match (" + currentPlayer1.getChips() + " vs " + currentPlayer2.getChips() + ")";
        } else if (currentPlayer2.getChips() > currentPlayer1.getChips()) {
            finalWinner = currentPlayer2.getName() + " wins the match (" + currentPlayer2.getChips() + " vs " + currentPlayer1.getChips() + ")";
        } else {
            finalWinner = "The match is a tie (" + currentPlayer1.getChips() + " vs " + currentPlayer2.getChips() + ")";
        }

        winnerLabel.setText(finalWinner);
        roundLabel.setText("Round: " + TOTAL_ROUNDS + " / " + TOTAL_ROUNDS);
        historyArea.appendText("MATCH RESULT: " + finalWinner + System.lineSeparator());
        updateDatabaseStatusText();
    }

    private void updateDatabaseStatusText() {
        if (databaseService.isAvailable()) {
            statusLabel.setText("Database connected: MySQL");
        } else {
            String reason = databaseService.getLastError();
            if (reason == null || reason.isBlank()) {
                reason = "No active database connection.";
            }
            statusLabel.setText("Offline mode. DB status: " + reason + " | Use Connect DB on login screen.");
        }
    }

    private void playRound(Player player1, Player player2, int roundNumber) {
        Deck deck = new Deck();
        player1.clearHand();
        player2.clearHand();

        int pot = 0;
        boolean player1IsSmallBlind = roundNumber % 2 == 1;

        int p1Blind = player1IsSmallBlind ? SMALL_BLIND : BIG_BLIND;
        int p2Blind = player1IsSmallBlind ? BIG_BLIND : SMALL_BLIND;
        pot += moveToPot(player1, p1Blind);
        pot += moveToPot(player2, p2Blind);

        int highestBet = Math.max(p1Blind, p2Blind);
        int p1Contribution = p1Blind;
        int p2Contribution = p2Blind;

        for (int i = 0; i < 2; i++) {
            player1.addCard(deck.dealCard());
            player2.addCard(deck.dealCard());
        }

        int p1HoleStrength = estimateHoleStrength(player1.getHand());
        int p2HoleStrength = estimateHoleStrength(player2.getHand());

        StringBuilder actionLog = new StringBuilder();
        actionLog.append("Blinds posted - ")
                .append(player1.getName()).append(": ").append(p1Blind)
                .append(", ").append(player2.getName()).append(": ").append(p2Blind);

        // Pre-flop action for player 1
        int p1ToCall = Math.max(0, highestBet - p1Contribution);
        BettingAction p1Action = chooseAction(p1HoleStrength, p1ToCall, player1.getChips());
        if (p1Action == BettingAction.FOLD) {
            player2.adjustChips(pot);
            finalizeFoldRound(player1, player2, roundNumber, pot, player1.getName(), actionLog + " | " + player1.getName() + " folds pre-flop");
            return;
        }
        if (p1Action == BettingAction.CALL) {
            int paid = moveToPot(player1, p1ToCall);
            p1Contribution += paid;
            pot += paid;
            actionLog.append(" | ").append(player1.getName()).append(" calls ").append(paid);
        } else if (p1Action == BettingAction.RAISE) {
            int raiseTarget = highestBet + BIG_BLIND;
            int p1RaiseToCall = Math.max(0, raiseTarget - p1Contribution);
            int paid = moveToPot(player1, p1RaiseToCall);
            p1Contribution += paid;
            pot += paid;
            highestBet = p1Contribution;
            actionLog.append(" | ").append(player1.getName()).append(" raises");
        } else {
            actionLog.append(" | ").append(player1.getName()).append(" checks");
        }

        // Pre-flop response for player 2
        int p2ToCall = Math.max(0, highestBet - p2Contribution);
        BettingAction p2Action = chooseAction(p2HoleStrength, p2ToCall, player2.getChips());
        if (p2Action == BettingAction.FOLD) {
            player1.adjustChips(pot);
            finalizeFoldRound(player1, player2, roundNumber, pot, player2.getName(), actionLog + " | " + player2.getName() + " folds pre-flop");
            return;
        }
        if (p2Action == BettingAction.CALL || p2ToCall > 0) {
            int paid = moveToPot(player2, p2ToCall);
            p2Contribution += paid;
            pot += paid;
            actionLog.append(" | ").append(player2.getName()).append(" calls ").append(paid);
        } else if (p2Action == BettingAction.RAISE) {
            int raiseTarget = highestBet + BIG_BLIND;
            int p2RaiseToCall = Math.max(0, raiseTarget - p2Contribution);
            int paid = moveToPot(player2, p2RaiseToCall);
            p2Contribution += paid;
            pot += paid;
            highestBet = p2Contribution;
            actionLog.append(" | ").append(player2.getName()).append(" raises");

            int p1ReCall = Math.max(0, highestBet - p1Contribution);
            int p1Paid = moveToPot(player1, p1ReCall);
            p1Contribution += p1Paid;
            pot += p1Paid;
            actionLog.append(" | ").append(player1.getName()).append(" calls ").append(p1Paid);
        } else {
            actionLog.append(" | ").append(player2.getName()).append(" checks");
        }

        List<Card> community = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            community.add(deck.dealCard());
        }

        List<Card> player1Seven = new ArrayList<>(player1.getHand());
        player1Seven.addAll(community);
        List<Card> player2Seven = new ArrayList<>(player2.getHand());
        player2Seven.addAll(community);

        HandValue player1Value = HandEvaluator.evaluateBestHand(player1Seven);
        HandValue player2Value = HandEvaluator.evaluateBestHand(player2Seven);

        String winnerText;
        String resultText;

        if (player1Value.compareTo(player2Value) > 0) {
            winnerText = player1.getName() + " wins round " + roundNumber;
            resultText = winnerText + " with " + player1Value.getDescription() + " against " + player2Value.getDescription();
            player1.adjustChips(pot);
        } else if (player2Value.compareTo(player1Value) > 0) {
            winnerText = player2.getName() + " wins round " + roundNumber;
            resultText = winnerText + " with " + player2Value.getDescription() + " against " + player1Value.getDescription();
            player2.adjustChips(pot);
        } else {
            winnerText = "Tie round " + roundNumber;
            resultText = "It is a tie. Both players have " + player1Value.getDescription();
            int splitA = pot / 2;
            int splitB = pot - splitA;
            player1.adjustChips(splitA);
            player2.adjustChips(splitB);
        }

        player1Label.setText(player1.getName() + " (" + player1.getChips() + " chips)");
        player2Label.setText(player2.getName() + " (" + player2.getChips() + " chips)");
        roundLabel.setText("Round: " + roundNumber + " / " + TOTAL_ROUNDS);
        potLabel.setText("Pot: " + pot);
        communityLabel.setText("Community Cards");
        actionLabel.setText("Action: " + actionLog);
        renderCards(player1CardsBox, player1.getHand());
        renderCards(player2CardsBox, player2.getHand());
        renderCards(communityCardsBox, community);
        winnerLabel.setText("Round " + roundNumber + ": " + winnerText + " | P1: " + player1Value.getDescription() + " | P2: " + player2Value.getDescription());

        boolean saved = databaseService.saveHand(player1, player2, community, player1Value, player2Value, winnerText, resultText);
        if (saved) {
            statusLabel.setText("Round " + roundNumber + " saved to database.");
            refreshSavedHandsTable();
        } else {
            statusLabel.setText("Round " + roundNumber + " dealt (not saved): " + databaseService.getLastError());
        }

        String historyLine = "Round " + roundNumber + ": " + resultText + " | Actions=" + actionLog + " | P1=" + player1.handText() + " | P2=" + player2.handText() + " | Board=" + community + System.lineSeparator();
        historyArea.appendText(historyLine);
    }

    private int moveToPot(Player player, int requestedAmount) {
        if (requestedAmount <= 0) {
            return 0;
        }
        int actual = Math.min(requestedAmount, Math.max(0, player.getChips()));
        player.adjustChips(-actual);
        return actual;
    }

    private int estimateHoleStrength(List<Card> holeCards) {
        if (holeCards.size() < 2) {
            return 0;
        }

        Card c1 = holeCards.get(0);
        Card c2 = holeCards.get(1);
        int v1 = c1.getRankValue();
        int v2 = c2.getRankValue();

        int score = 0;
        if (v1 == v2) {
            score += 5;
        }
        if (Math.max(v1, v2) >= 13) {
            score += 2;
        }
        if (Math.min(v1, v2) >= 10) {
            score += 2;
        }
        if (c1.getSuit().equals(c2.getSuit())) {
            score += 1;
        }
        if (Math.abs(v1 - v2) <= 2) {
            score += 1;
        }
        return score;
    }

    private BettingAction chooseAction(int strength, int toCall, int chipsRemaining) {
        if (chipsRemaining <= 0) {
            return BettingAction.CHECK;
        }
        if (toCall > chipsRemaining) {
            return BettingAction.FOLD;
        }
        if (strength >= 7) {
            return toCall == 0 ? BettingAction.RAISE : BettingAction.RAISE;
        }
        if (strength >= 4) {
            return toCall == 0 ? BettingAction.CHECK : BettingAction.CALL;
        }
        if (toCall <= SMALL_BLIND) {
            return toCall == 0 ? BettingAction.CHECK : BettingAction.CALL;
        }
        return BettingAction.FOLD;
    }

    private void finalizeFoldRound(Player player1, Player player2, int roundNumber, int pot, String foldedPlayerName, String actionText) {
        String winnerName = player1.getName().equals(foldedPlayerName) ? player2.getName() : player1.getName();

        player1Label.setText(player1.getName() + " (" + player1.getChips() + " chips)");
        player2Label.setText(player2.getName() + " (" + player2.getChips() + " chips)");
        roundLabel.setText("Round: " + roundNumber + " / " + TOTAL_ROUNDS);
        potLabel.setText("Pot: " + pot);
        actionLabel.setText("Action: " + actionText);
        communityLabel.setText("Community Cards (not dealt - fold)");
        communityCardsBox.getChildren().clear();
        renderCards(player1CardsBox, player1.getHand());
        renderCards(player2CardsBox, player2.getHand());

        String winnerText = winnerName + " wins round " + roundNumber + " by fold";
        String resultText = foldedPlayerName + " folded pre-flop. " + winnerName + " wins " + pot + " chips.";
        winnerLabel.setText("Round " + roundNumber + ": " + winnerText);

        boolean saved = databaseService.saveHand(
                player1,
                player2,
                Collections.emptyList(),
                new HandValue(0, Collections.singletonList(0), "Fold Win"),
                new HandValue(0, Collections.singletonList(0), "Fold Loss"),
                winnerText,
                resultText + " Actions=" + actionText);

        if (saved) {
            statusLabel.setText("Round " + roundNumber + " saved to database.");
            refreshSavedHandsTable();
        } else {
            statusLabel.setText("Round " + roundNumber + " dealt (not saved): " + databaseService.getLastError());
        }

        historyArea.appendText("Round " + roundNumber + ": " + resultText + " | Actions=" + actionText + System.lineSeparator());
    }

    public static void main(String[] args) {
        launch(args);
    }

    private HBox createCardsRow() {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMinHeight(118);
        row.setPadding(new Insets(4, 0, 4, 0));
        return row;
    }

    private void renderCards(HBox target, List<Card> cards) {
        List<StackPane> cardViews = new ArrayList<>();
        for (Card card : cards) {
            cardViews.add(createCardView(card));
        }
        target.getChildren().setAll(cardViews);
    }

    private StackPane createCardView(Card card) {
        Rectangle background = new Rectangle(72, 104);
        background.setArcWidth(14);
        background.setArcHeight(14);
        background.setFill(Color.WHITE);
        background.setStroke(Color.web("#2b2b2b"));
        background.setStrokeWidth(1.5);

        String suitSymbol = suitSymbol(card.getSuit());
        Color suitColor = isRedSuit(card.getSuit()) ? Color.web("#c62828") : Color.web("#1f1f1f");

        Text rankTop = new Text(card.getRank());
        rankTop.setFill(suitColor);
        rankTop.setFont(Font.font("System", FontWeight.BOLD, 16));
        StackPane.setAlignment(rankTop, Pos.TOP_LEFT);
        StackPane.setMargin(rankTop, new Insets(8, 0, 0, 8));

        Text suitCenter = new Text(suitSymbol);
        suitCenter.setFill(suitColor);
        suitCenter.setFont(Font.font("System", FontWeight.BOLD, 30));

        Text rankBottom = new Text(card.getRank());
        rankBottom.setFill(suitColor);
        rankBottom.setFont(Font.font("System", FontWeight.BOLD, 16));
        rankBottom.setRotate(180);
        StackPane.setAlignment(rankBottom, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(rankBottom, new Insets(0, 8, 8, 0));

        StackPane cardPane = new StackPane(background, suitCenter, rankTop, rankBottom);
        cardPane.setAlignment(Pos.CENTER);
        cardPane.setPrefSize(72, 104);
        cardPane.setMaxSize(72, 104);
        cardPane.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 8, 0.15, 2, 3);");
        return cardPane;
    }

    private String suitSymbol(String suit) {
        switch (suit) {
            case "Hearts":
                return "♥";
            case "Diamonds":
                return "♦";
            case "Clubs":
                return "♣";
            case "Spades":
                return "♠";
            default:
                return "?";
        }
    }

    private boolean isRedSuit(String suit) {
        return "Hearts".equals(suit) || "Diamonds".equals(suit);
    }

    private void refreshSavedHandsTable() {
        if (!databaseService.isAvailable()) {
            savedHandsTable.setItems(FXCollections.observableArrayList());
            return;
        }

        ObservableList<SavedHandRecord> rows = FXCollections.observableArrayList(databaseService.loadRecentHands(25));
        savedHandsTable.setItems(rows);
    }

    private TableColumn<SavedHandRecord, String> createTableColumn(String title, java.util.function.Function<SavedHandRecord, SimpleStringProperty> valueProvider) {
        TableColumn<SavedHandRecord, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cellData -> valueProvider.apply(cellData.getValue()));
        return column;
    }
}