import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class MainFrame extends JFrame {

    private JTextField nameField;
    private JTextField phoneField;
    private JTextField vehicleNumberField;
    private JTextField locationField;

    private JComboBox<String> vehicleTypeBox;
    private JComboBox<Area> areaBox;
    private JComboBox<String> problemBox;

    private JCheckBox drivableBox;

    private JTextArea resultArea;

    // The two helpers that talk to the database
    private final RequestRepository repository = new RequestRepository();
    private final DispatchManager manager = new DispatchManager(repository);

    // The request created most recently. The Arrived / Complete / Retry /
    // Cancel buttons act on this one.
    private AssistanceRequest currentRequest;

    // Throws SQLException because the drop-downs are filled from the database
    public MainFrame() throws SQLException {

        setTitle("Roadside Assistance Dispatch System");
        setSize(700, 780);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(11, 2, 10, 10));
        panel.setBorder(
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        );

        panel.add(new JLabel("Driver Name:"));
        nameField = new JTextField();
        panel.add(nameField);

        panel.add(new JLabel("Phone Number (10 digits):"));
        phoneField = new JTextField();
        panel.add(phoneField);

        panel.add(new JLabel("Vehicle Number:"));
        vehicleNumberField = new JTextField();
        panel.add(vehicleNumberField);

        // Vehicle types now come from the vehicle_type table
        panel.add(new JLabel("Vehicle Type:"));
        vehicleTypeBox = new JComboBox<>(
            repository.loadVehicleTypes().toArray(new String[0])
        );
        panel.add(vehicleTypeBox);

        // NEW: nearest known area. The first choice means "not sure", which
        // is stored as NULL - a remote driver may not know the area name.
        panel.add(new JLabel("Nearest Area:"));
        areaBox = new JComboBox<>();
        areaBox.addItem(new Area(null, "Not sure / remote area"));
        for (Area area : repository.loadAreas()) {
            areaBox.addItem(area);
        }
        panel.add(areaBox);

        // The old "Location" box is now the landmark description
        panel.add(new JLabel("Exact Location / Landmark:"));
        locationField = new JTextField();
        panel.add(locationField);

        // Problems now come from the problem_type table
        panel.add(new JLabel("Problem:"));
        problemBox = new JComboBox<>(
            repository.loadProblems().toArray(new String[0])
        );
        panel.add(problemBox);

        panel.add(new JLabel("Vehicle Drivable:"));
        drivableBox = new JCheckBox("Yes");
        panel.add(drivableBox);

        JButton requestButton =
            new JButton("Request Assistance");

        JButton clearButton =
            new JButton("Clear");

        panel.add(requestButton);
        panel.add(clearButton);

        // NEW buttons for the steps after dispatch (tracking)
        JButton arrivedButton = new JButton("Provider Arrived");
        JButton completeButton = new JButton("Complete Job");
        JButton retryButton = new JButton("Retry Dispatch");
        JButton cancelButton = new JButton("Cancel Request");

        panel.add(arrivedButton);
        panel.add(completeButton);
        panel.add(retryButton);
        panel.add(cancelButton);

        add(panel, BorderLayout.NORTH);

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Arial", Font.PLAIN, 14));

        add(new JScrollPane(resultArea), BorderLayout.CENTER);

        requestButton.addActionListener(e -> createRequest());

        clearButton.addActionListener(e -> clearFields());

        arrivedButton.addActionListener(e -> markArrived());
        completeButton.addActionListener(e -> completeJob());
        retryButton.addActionListener(e -> retryDispatch());
        cancelButton.addActionListener(e -> cancelRequest());
    }

    // ------------------------------------------------------------------
    // "Request Assistance" button
    // ------------------------------------------------------------------
    private void createRequest() {

        // trim() removes spaces typed by mistake before/after the text
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();

        // Vehicle numbers are always stored in capitals: tn05bm1773 -> TN05BM1773
        String vehicleNumber =
            vehicleNumberField.getText().trim().toUpperCase();

        String vehicleType =
            (String) vehicleTypeBox.getSelectedItem();

        Area area = (Area) areaBox.getSelectedItem();

        String location =
            locationField.getText().trim();

        String problem =
            (String) problemBox.getSelectedItem();

        boolean vehicleDrivable =
            drivableBox.isSelected();

        // ---- Check the input BEFORE going to the database ----
        if (name.isEmpty() || vehicleNumber.isEmpty() || location.isEmpty()) {
            warn("Please fill in the name, vehicle number and location.");
            return;
        }

        // matches() with a regular expression: exactly 10 digits, nothing else
        if (!phone.matches("[0-9]{10}")) {
            warn("Phone number must be exactly 10 digits.");
            return;
        }

        Vehicle vehicle =
            new Vehicle(vehicleNumber, vehicleType);

        Driver driver =
            new Driver(name, phone, vehicle);

        AssistanceRequest request =
            new AssistanceRequest(
                driver,
                location,
                area.getId(),
                problem,
                vehicleDrivable
            );

        try {
            // 1. Save driver + vehicle + request. The database returns the
            //    request number (it replaces the old hard-coded 1001).
            request.setRequestId(repository.submit(request));
            currentRequest = request;

            // 2. Ask the dispatch manager to find a provider
            manager.dispatch(request);

            // 3. Show everything
            showSummary("Request saved.");

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    // ------------------------------------------------------------------
    // The other four buttons
    // ------------------------------------------------------------------
    private void markArrived() {
        if (!haveRequest()) return;

        try {
            manager.markArrived(currentRequest);
            showSummary("The provider has reached the driver.");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void completeJob() {
        if (!haveRequest()) return;

        try {
            manager.complete(currentRequest);
            showSummary("Job completed. The provider is free again.");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    // For a request that found nobody free the first time
    private void retryDispatch() {
        if (!haveRequest()) return;

        try {
            manager.dispatch(currentRequest);
            showSummary("Dispatch retried.");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void cancelRequest() {
        if (!haveRequest()) return;

        try {
            manager.cancel(currentRequest);
            showSummary("Request cancelled.");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    // ------------------------------------------------------------------
    // Showing results
    // ------------------------------------------------------------------

    // Writes the whole picture of the current request into the text area
    private void showSummary(String headline) throws SQLException {

        AssistanceRequest request = currentRequest;
        Driver driver = request.getDriver();
        Vehicle vehicle = driver.getVehicle();

        resultArea.setText("");

        resultArea.append(headline + "\n\n");

        resultArea.append(
            "===== ASSISTANCE REQUEST =====\n\n"
        );

        resultArea.append(
            "Request ID: " + request.getRequestId() + "\n"
        );

        resultArea.append(
            "Driver: " + driver.getName() + "\n"
        );

        resultArea.append(
            "Phone: " + driver.getPhone() + "\n"
        );

        resultArea.append(
            "Vehicle: " + vehicle.getVehicleNumber()
            + " (" + vehicle.getVehicleType() + ")\n"
        );

        resultArea.append(
            "Location: " + request.getLocation() + "\n"
        );

        resultArea.append(
            "Problem: " + request.getProblem() + "\n"
        );

        resultArea.append(
            "Vehicle Drivable: "
            + (request.isVehicleDrivable() ? "Yes" : "No") + "\n"
        );

        resultArea.append(
            "\nStatus: " + request.getStatus() + "\n"
        );

        // If a provider is assigned, describe() prints the details that fit
        // its kind (mechanic speciality, tow capacity or fuel capacity).
        if (request.getProvider() != null) {

            resultArea.append(
                "\n===== DISPATCH =====\n"
            );

            resultArea.append(
                "Provider Assigned: "
                + request.getProvider().describe() + "\n"
            );

        } else if ("Request Received".equals(request.getStatus())) {

            resultArea.append(
                "\nNo suitable service provider is free right now.\n"
                + "The request is saved. Press \"Retry Dispatch\" in a moment.\n"
            );
        }

        // The tracking timeline comes from the request_status_history table
        resultArea.append("\n===== TRACKING =====\n");

        List<String> timeline = repository.loadTimeline(request.getRequestId());
        for (String line : timeline) {
            resultArea.append(line + "\n");
        }
    }

    // A friendly pop-up for the checks done on the screen
    private void warn(String message) {
        JOptionPane.showMessageDialog(
            this, message, "Check your input", JOptionPane.WARNING_MESSAGE
        );
    }

    // A pop-up for messages that come back from the database.
    // Our procedures raise clear messages such as
    // "Only an assigned request can be marked as arrived".
    private void showError(SQLException ex) {
        JOptionPane.showMessageDialog(
            this, ex.getMessage(), "Database message", JOptionPane.ERROR_MESSAGE
        );
    }

    // The four tracking buttons need a request to work on
    private boolean haveRequest() {
        if (currentRequest == null) {
            warn("Please create a request first.");
            return false;
        }
        return true;
    }

    private void clearFields() {

        nameField.setText("");
        phoneField.setText("");
        vehicleNumberField.setText("");
        locationField.setText("");

        areaBox.setSelectedIndex(0);

        drivableBox.setSelected(false);

        resultArea.setText("");

        // Forget the request: the buttons must not act on a cleared screen
        currentRequest = null;
    }
}
