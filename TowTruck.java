public class TowTruck extends ServiceProvider {

    private int towingCapacity;

    public TowTruck(String providerId, String name,
                    String location, boolean available,
                    int towingCapacity) {

        super(providerId, name, location, available);
        this.towingCapacity = towingCapacity;
    }

    public int getTowingCapacity() {
        return towingCapacity;
    }

    @Override
    public void acceptRequest() {
        System.out.println("Tow truck " + name + " is heading to the vehicle.");
    }

    @Override
    public String describe() {
        return "Tow truck " + name + " (" + providerId + ", " + towingCapacity
                + " kg, base: " + location + ")";
    }
}
