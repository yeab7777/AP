import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    public static Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/chatapp", "root", ""
            );
        } catch (SQLException e) {
            System.out.println("❌ DB Connection failed: " + e.getMessage());
        }
        return conn;
    }
}
