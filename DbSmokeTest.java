import java.sql.SQLException;
import java.util.List;

/**
 * Quick check WITHOUT the screen: runs the whole workflow against the
 * database and prints PASS / FAIL for each step.
 *
 * Run it after setting up the database to be sure everything is wired:
 *     java -cp ".;lib/*" DbSmokeTest        (Windows)
 *     java -cp ".:lib/*" DbSmokeTest        (Linux / Mac)
 */
public class DbSmokeTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {

        RequestRepository repository = new RequestRepository();
        DispatchManager manager = new DispatchManager(repository);

        check("database reachable", () -> Database.testConnection());

        // A driver whose car has run out of fuel on OMR Highway
        Vehicle vehicle = new Vehicle("TN05SM0001", "Car");
        Driver driver = new Driver("Smoke Test", "9000011111", vehicle);

        // Find the id of the "OMR Highway" area
        Integer omr = null;
        for (Area a : repository.loadAreas()) {
            if (a.getName().equals("OMR Highway")) omr = a.getId();
        }

        AssistanceRequest request = new AssistanceRequest(
            driver, "Near the toll plaza", omr, "Empty Fuel Tank", true);

        request.setRequestId(repository.submit(request));
        System.out.println("Created request #" + request.getRequestId());
        expect("request id is generated", request.getRequestId() >= 1001);

        ServiceProvider provider = manager.dispatch(request);
        expect("a provider was assigned", provider != null);
        expect("fuel problem -> FuelSupplier object", provider instanceof FuelSupplier);
        System.out.println("Provider: " + provider.describe());

        manager.markArrived(request);
        expect("status is Provider Arrived", "Provider Arrived".equals(request.getStatus()));

        manager.complete(request);
        expect("status is Completed", "Completed".equals(request.getStatus()));

        List<String> timeline = repository.loadTimeline(request.getRequestId());
        timeline.forEach(System.out::println);
        expect("timeline has 4 steps", timeline.size() == 4);

        // The database must refuse an illegal step
        try {
            manager.markArrived(request);
            expect("illegal step is refused", false);
        } catch (SQLException e) {
            expect("illegal step is refused: " + e.getMessage(), true);
        }

        System.out.println(failures == 0 ? "\nALL CHECKS PASSED" : "\n" + failures + " CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private interface Step { void run() throws Exception; }

    private static void check(String label, Step step) {
        try { step.run(); expect(label, true); }
        catch (Exception e) { System.out.println("      " + e.getMessage()); expect(label, false); }
    }

    private static void expect(String label, boolean ok) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + label);
        if (!ok) failures++;
    }
}
