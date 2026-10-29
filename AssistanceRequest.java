public class AssistanceRequest {

    // 0 until the database gives the request its number (1001, 1002, ...)
    private int requestId;

    private Driver driver;
    private String location;        // landmark text typed by the driver
    private Integer areaId;         // nearest known area, or null if not sure
    private String problem;
    private boolean vehicleDrivable;
    private String status;

    // Set after dispatch: who is coming to help
    private ServiceProvider provider;

    public AssistanceRequest(Driver driver, String location, Integer areaId,
                             String problem, boolean vehicleDrivable) {

        this.driver = driver;
        this.location = location;
        this.areaId = areaId;
        this.problem = problem;
        this.vehicleDrivable = vehicleDrivable;
        this.status = "Request Received";
    }

    public int getRequestId() {
        return requestId;
    }

    // Called once, right after the request is saved in the database
    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public Driver getDriver() {
        return driver;
    }

    public String getLocation() {
        return location;
    }

    public Integer getAreaId() {
        return areaId;
    }

    public String getProblem() {
        return problem;
    }

    public boolean isVehicleDrivable() {
        return vehicleDrivable;
    }

    public String getStatus() {
        return status;
    }

    public void updateStatus(String status) {
        this.status = status;
    }

    public ServiceProvider getProvider() {
        return provider;
    }

    public void setProvider(ServiceProvider provider) {
        this.provider = provider;
    }
}
