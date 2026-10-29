import java.sql.SQLException;
import java.util.List;

/**
 * The dispatch manager from the project story.
 *
 * Before: it kept an ArrayList of providers in memory and handed out the
 * first free one. Now the providers live in the database and the real
 * decision ("which provider fits this problem?") is made by the stored
 * procedure sp_dispatch_request, inside one safe transaction.
 *
 * This class turns the database's answer back into Java objects and keeps
 * the AssistanceRequest object in step with what the database says.
 */
public class DispatchManager {

    private final RequestRepository repository;

    public DispatchManager(RequestRepository repository) {
        this.repository = repository;
    }

    /**
     * Finds and assigns a provider.
     * Returns the provider, or null if nobody suitable is free right now
     * (the request stays saved with status "Request Received").
     */
    public ServiceProvider dispatch(AssistanceRequest request) throws SQLException {

        // The database picks the provider and marks him busy
        int providerId = repository.dispatch(request.getRequestId());

        if (providerId == 0) {
            return null;                              // nobody free
        }

        // Build the matching Mechanic / TowTruck / FuelSupplier object
        ServiceProvider provider = repository.loadProvider(providerId);

        provider.acceptRequest();                     // polymorphism: prints a different line per type
        provider.setAvailable(false);

        request.setProvider(provider);
        request.updateStatus("Provider Assigned");
        return provider;
    }

    /** The provider has reached the driver. */
    public void markArrived(AssistanceRequest request) throws SQLException {
        repository.markArrived(request.getRequestId());
        refreshStatus(request);
    }

    /** The job is finished: the provider becomes free again. */
    public void complete(AssistanceRequest request) throws SQLException {
        repository.complete(request.getRequestId());
        refreshStatus(request);

        if (request.getProvider() != null) {
            request.getProvider().completeRequest();
            request.getProvider().setAvailable(true);
        }
    }

    /** The driver cancels before the provider arrives. */
    public void cancel(AssistanceRequest request) throws SQLException {
        repository.cancel(request.getRequestId());
        refreshStatus(request);

        if (request.getProvider() != null) {
            request.getProvider().setAvailable(true);
        }
    }

    /** Providers that are free right now (read from the database). */
    public List<ServiceProvider> getAvailableProviders() throws SQLException {
        return repository.loadAvailableProviders();
    }

    // After any step, copy the status the database holds into the Java object
    private void refreshStatus(AssistanceRequest request) throws SQLException {
        request.updateStatus(repository.loadStatus(request.getRequestId()));
    }
}
