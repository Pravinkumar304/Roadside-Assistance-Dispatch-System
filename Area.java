/**
 * One row of the service_area table (a city area or highway stretch).
 *
 * The drop-down on the screen shows area names, but the database needs the
 * area's number (its id). This tiny class carries both together.
 * toString() decides what text the drop-down displays.
 */
public class Area {

    // Integer (not int) so it can be null = "driver is not sure"
    private final Integer id;
    private final String name;

    public Area(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
