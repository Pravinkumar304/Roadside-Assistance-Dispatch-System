import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * The ONLY class that contains SQL. The screen (MainFrame) and the
 * DispatchManager never write SQL; they call the methods below.
 *
 * Keeping all SQL in one place means: if a table or procedure changes,
 * this is the one file you edit.
 *
 * Two JDBC tools are used:
 *   - PreparedStatement : runs a SELECT. The "?" marks are filled in with
 *                         setXxx(...) so user text can never break the SQL.
 *   - CallableStatement : runs a stored procedure with "{CALL name(?, ?)}".
 *                         OUT parameters bring answers back (e.g. the new id).
 */
public class RequestRepository {

    // ------------------------------------------------------------------
    // Lists for the drop-down boxes
    // ------------------------------------------------------------------

    public List<String> loadVehicleTypes() throws SQLException {
        return loadNames("SELECT type_name FROM vehicle_type ORDER BY type_id");
    }

    public List<String> loadProblems() throws SQLException {
        return loadNames("SELECT problem_name FROM problem_type ORDER BY problem_id");
    }

    // Helper: runs a one-column SELECT and returns the values as a list
    private List<String> loadNames(String sql) throws SQLException {
        List<String> names = new ArrayList<>();

        try (Connection con = Database.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {                 // move to the next row
                names.add(rs.getString(1));     // column number 1
            }
        }
        return names;
    }

    public List<Area> loadAreas() throws SQLException {
        List<Area> areas = new ArrayList<>();

        try (Connection con = Database.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT area_id, area_name FROM service_area ORDER BY area_name")) {

            while (rs.next()) {
                areas.add(new Area(rs.getInt("area_id"), rs.getString("area_name")));
            }
        }
        return areas;
    }

    // ------------------------------------------------------------------
    // Saving a new request
    // ------------------------------------------------------------------

    /**
     * Saves driver + vehicle + request in one database transaction by
     * calling sp_submit_request. Returns the new request number.
     */
    public int submit(AssistanceRequest request) throws SQLException {

        Driver driver = request.getDriver();
        Vehicle vehicle = driver.getVehicle();

        // 11 question marks = 10 IN parameters + 1 OUT parameter
        String call = "{CALL sp_submit_request(?,?,?,?,?,?,?,?,?,?,?)}";

        try (Connection con = Database.getConnection();
             CallableStatement cs = con.prepareCall(call)) {

            cs.setString(1, driver.getName());
            cs.setString(2, driver.getPhone());
            cs.setString(3, vehicle.getVehicleNumber());
            cs.setString(4, vehicle.getVehicleType());

            // The nearest area is optional: null means "SQL NULL"
            if (request.getAreaId() == null) {
                cs.setNull(5, Types.INTEGER);
            } else {
                cs.setInt(5, request.getAreaId());
            }

            cs.setString(6, request.getLocation());
            cs.setString(7, request.getProblem());
            cs.setBoolean(8, request.isVehicleDrivable());

            // GPS columns exist for the future live-location feature.
            // The screen does not collect them yet, so send NULL.
            cs.setNull(9, Types.DECIMAL);
            cs.setNull(10, Types.DECIMAL);

            // Tell JDBC parameter 11 is an OUTPUT that will hold an integer
            cs.registerOutParameter(11, Types.INTEGER);

            cs.execute();                       // run the procedure

            return cs.getInt(11);               // read the new request id
        }
    }

    // ------------------------------------------------------------------
    // Workflow steps: each one is a stored procedure (= one transaction)
    // ------------------------------------------------------------------

    /**
     * Asks the database to pick and assign a provider.
     * Returns the provider's database id, or 0 if nobody suitable is free.
     */
    public int dispatch(int requestId) throws SQLException {

        try (Connection con = Database.getConnection();
             CallableStatement cs = con.prepareCall("{CALL sp_dispatch_request(?, ?)}")) {

            cs.setInt(1, requestId);
            cs.registerOutParameter(2, Types.INTEGER);
            cs.execute();

            int providerId = cs.getInt(2);
            // getInt returns 0 for SQL NULL, so wasNull() tells them apart
            return cs.wasNull() ? 0 : providerId;
        }
    }

    public void markArrived(int requestId) throws SQLException {
        callWithRequestId("{CALL sp_mark_arrived(?)}", requestId);
    }

    public void complete(int requestId) throws SQLException {
        callWithRequestId("{CALL sp_complete_request(?)}", requestId);
    }

    public void cancel(int requestId) throws SQLException {
        callWithRequestId("{CALL sp_cancel_request(?)}", requestId);
    }

    // Helper for procedures that take just the request id
    private void callWithRequestId(String call, int requestId) throws SQLException {
        try (Connection con = Database.getConnection();
             CallableStatement cs = con.prepareCall(call)) {

            cs.setInt(1, requestId);
            cs.execute();
        }
    }

    // ------------------------------------------------------------------
    // Reading things back
    // ------------------------------------------------------------------

    public String loadStatus(int requestId) throws SQLException {
        String sql = "SELECT status FROM assistance_request WHERE request_id = ?";

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, requestId);            // fills the "?"

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /** The tracking timeline: one text line per status the request went through. */
    public List<String> loadTimeline(int requestId) throws SQLException {
        String sql = "SELECT status, changed_at FROM request_status_history "
                   + "WHERE request_id = ? ORDER BY history_id";

        SimpleDateFormat format = new SimpleDateFormat("dd-MMM HH:mm:ss");
        List<String> lines = new ArrayList<>();

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, requestId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lines.add(format.format(rs.getTimestamp("changed_at"))
                              + "   " + rs.getString("status"));
                }
            }
        }
        return lines;
    }

    /** Loads ONE provider (any availability) as a Mechanic, TowTruck or FuelSupplier. */
    public ServiceProvider loadProvider(int providerId) throws SQLException {
        String sql =
            "SELECT sp.provider_id, sp.provider_code, sp.name, sp.provider_type, "
          + "       sp.is_available, a.area_name AS base_area, "
          + "       m.specialization, t.towing_capacity_kg, f.capacity_litres "
          + "FROM service_provider sp "
          + "JOIN service_area a ON a.area_id = sp.base_area_id "
          + "LEFT JOIN mechanic m      ON m.provider_id = sp.provider_id "
          + "LEFT JOIN tow_truck t     ON t.provider_id = sp.provider_id "
          + "LEFT JOIN fuel_supplier f ON f.provider_id = sp.provider_id "
          + "WHERE sp.provider_id = ?";

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, providerId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? buildProvider(rs, rs.getBoolean("is_available")) : null;
            }
        }
    }

    /** All providers that are free right now (reads the view vw_available_providers). */
    public List<ServiceProvider> loadAvailableProviders() throws SQLException {
        List<ServiceProvider> providers = new ArrayList<>();

        try (Connection con = Database.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM vw_available_providers")) {

            while (rs.next()) {
                providers.add(buildProvider(rs, true));    // the view only lists free ones
            }
        }
        return providers;
    }

    /**
     * Turns the current database row into the right Java object.
     * This is where polymorphism meets the database: the list holds
     * ServiceProvider references, but each one is really a Mechanic,
     * TowTruck or FuelSupplier.
     */
    private ServiceProvider buildProvider(ResultSet rs, boolean available) throws SQLException {

        String code = rs.getString("provider_code");
        String name = rs.getString("name");
        String baseArea = rs.getString("base_area");
        String type = rs.getString("provider_type");

        ServiceProvider provider;

        if ("MECHANIC".equals(type)) {
            provider = new Mechanic(code, name, baseArea, available,
                                    rs.getString("specialization"));
        } else if ("TOW_TRUCK".equals(type)) {
            provider = new TowTruck(code, name, baseArea, available,
                                    rs.getInt("towing_capacity_kg"));
        } else {
            provider = new FuelSupplier(code, name, baseArea, available,
                                        rs.getInt("capacity_litres"));
        }

        provider.setDbId(rs.getInt("provider_id"));
        return provider;
    }
}
