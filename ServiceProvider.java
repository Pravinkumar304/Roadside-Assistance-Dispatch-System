public class ServiceProvider {

    // Code shown to people, e.g. "M01", "T01", "F01" (column provider_code)
    protected String providerId;
    protected String name;
    protected String location;      // the provider's base area
    protected boolean available;

    // The primary key in the database (column provider_id)
    protected int dbId;

    public ServiceProvider(String providerId, String name, String location, boolean available) {

        this.providerId = providerId;
        this.name = name;
        this.location = location;
        this.available = available;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public int getDbId() {
        return dbId;
    }

    public void setDbId(int dbId) {
        this.dbId = dbId;
    }

    public void acceptRequest() {
        System.out.println(name + " accepted the request.");
    }

    public void completeRequest() {
        System.out.println(name + " completed the request.");
    }

    /**
     * One line describing this provider for the screen.
     * Each child class overrides it to add its own detail (polymorphism).
     */
    public String describe() {
        return name + " (" + providerId + ", base: " + location + ")";
    }
}
