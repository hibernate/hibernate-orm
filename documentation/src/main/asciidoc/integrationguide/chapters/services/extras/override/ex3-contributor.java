public class LatestAndGreatestConnectionProviderImplContributor
    implements ServiceContributor {

    @Override
    public void contribute(
            StandardServiceRegistryBuilder serviceRegistryBuilder) {

        // here we will register a short name for our service strategy
        StrategySelector selector = serviceRegistryBuilder
            .getBootstrapServiceRegistry().
            .requireService( StrategySelector.class );

        selector.registerStrategyImplementor(
            ConnectionProvider.class,
            "lag",
            LatestAndGreatestConnectionProviderImpl.class
        );
    }
}