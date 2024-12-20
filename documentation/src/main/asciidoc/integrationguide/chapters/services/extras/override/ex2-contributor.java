public class LatestAndGreatestConnectionProviderImplContributor
        implements ServiceContributor {

    @Override
    public void contribute(StandardServiceRegistryBuilder serviceRegistryBuilder) {
        serviceRegistryBuilder.addService(
            ConnectionProvider.class,
            new LatestAndGreatestConnectionProviderImpl()
        );
    }
}