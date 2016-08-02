public class LatestAndGreatestConnectionProviderImplContributor1
        implements ServiceContributor {

    @Override
    public void contribute(StandardServiceRegistryBuilder serviceRegistryBuilder) {
        serviceRegistryBuilder.addService(
            ConnectionProvider.class,
            new LatestAndGreatestConnectionProviderImpl()
        );
    }
}