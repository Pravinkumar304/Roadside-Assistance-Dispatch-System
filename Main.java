import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            try {
                // Fail early, with a clear message, if the database is not reachable
                Database.testConnection();

                MainFrame frame = new MainFrame();

                frame.setVisible(true);

            } catch (Exception e) {

                JOptionPane.showMessageDialog(
                    null,
                    "Could not connect to the database.\n\n"
                    + "1. Is MySQL running?\n"
                    + "2. Did you run roadside_db.sql?\n"
                    + "3. Do the settings in db.properties match?\n\n"
                    + "Details: " + e.getMessage(),
                    "Database error",
                    JOptionPane.ERROR_MESSAGE
                );

                System.exit(1);
            }
        });
    }
}
