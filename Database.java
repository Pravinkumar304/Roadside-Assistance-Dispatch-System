import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;


public class Database {

    private static final String CONFIG_FILE = "db.properties";

    
    private static final String url;
    private static final String user;
    private static final String password;

    
    static {
        Properties props = new Properties();

       
        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
            props.load(in);                       
        } catch (IOException e) {
            // No db.properties found: carry on with the defaults below.
        }

        // getProperty(key, default) -> the file's value, or the default
        url = props.getProperty("db.url",
                "jdbc:mysql://localhost:3306/roadside_db"
                + "?allowPublicKeyRetrieval=true&useSSL=false");
        user = props.getProperty("db.user", "roadside_app");
        password = props.getProperty("db.password", "Roadside@123");
    }

    /**
     * Opens a NEW connection each time. For a small desktop app this is
     * simple and safe. The caller closes it (we always use try-with-resources).
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /** Used at start-up: opens a connection and closes it again at once. */
    public static void testConnection() throws SQLException {
        try (Connection c = getConnection()) {
            // nothing to do: if we got here, the database answered
        }
    }
}
