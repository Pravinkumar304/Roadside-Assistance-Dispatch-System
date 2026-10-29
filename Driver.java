public class Driver {
    private String name;
    private String phone;
    private Vehicle vehicle;

    public Driver(String name, String phone, Vehicle vehicle) {
        this.name = name;
        this.phone = phone;
        this.vehicle = vehicle;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }
}