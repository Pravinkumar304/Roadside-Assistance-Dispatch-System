public class Mechanic extends ServiceProvider {

    private String specialization;

    public Mechanic(String providerId, String name,
                    String location, boolean available,
                    String specialization) {

        super(providerId, name, location, available);
        this.specialization = specialization;
    }

    public String getSpecialization() {
        return specialization;
    }

    @Override
    public void acceptRequest() {
        System.out.println("Mechanic " + name + " is heading to the vehicle.");
    }

    @Override
    public String describe() {
        return "Mechanic " + name + " (" + providerId + ", " + specialization
                + ", base: " + location + ")";
    }
}
