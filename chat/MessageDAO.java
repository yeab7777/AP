import java.sql.*;

public class MessageDAO {
    public static void saveMessage(String sender, String receiver, String content) {
        try (Connection conn = DBConnection.connect()) {
            if (conn == null) {
                return;
            }
            String sql = "INSERT INTO messages(sender, receiver, content) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, sender);
            stmt.setString(2, receiver);
            stmt.setString(3, content);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void getMessages() {
        try (Connection conn = DBConnection.connect()) {
            String sql = "SELECT sender, content, timestamp FROM messages";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                System.out.println(rs.getString("sender") + ": " +
                                   rs.getString("content") + " (" +
                                   rs.getTimestamp("timestamp") + ")");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
