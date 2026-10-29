/**
 * NEW: a fuel van that brings fuel to a car that has run dry.
 * Same pattern as Mechanic and TowTruck: it extends ServiceProvider
 * and adds one field of its own.
 */
public class FuelSupplier extends ServiceProvider {

    private int capacityLitres;

    public FuelSupplier(String providerId, String name,
                        String location, boolean available,
                        int capacityLitres) {

        super(providerId, name, location, available);
        this.capacityLitres = capacityLitres;
    }

    public int getCapacityLitres() {
        return capacityLitres;
    }

    @Override
    public void acceptRequest() {
        System.out.println("Fuel van " + name + " is bringing fuel to the vehicle.");
    }

    @Override
    public String describe() {
        return "Fuel van " + name + " (" + providerId + ", " + capacityLitres
                + " L, base: " + location + ")";
    }
}
