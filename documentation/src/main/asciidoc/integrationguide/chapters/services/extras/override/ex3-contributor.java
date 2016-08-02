public class LatestAndGreatestConnectionProviderImplContributor
    implements ServiceContributor {

    @Override
    public void contribute(
            standardserviceregistrybuilder serviceregistrybuilder) {

        // here we will register a short-name for our service strategy
        strategyselector selector = serviceregistrybuilder
            .getbootstrapserviceregistry().
            .getservice( strategyselector.class );

        selector.registerstrategyimplementor(
            connectionprovider.class,
            "lag"
            latestandgreatestconnectionproviderimpl.class
        );
    }
}