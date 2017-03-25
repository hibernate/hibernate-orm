public class EventPublishingServiceContributor
    implements ServiceContributor {

    @Override
    public void contribute(StandardServiceRegistryBuilder builder) {
        builder.addinitiator( eventpublishingserviceinitiator.instance );

        // if we wanted to allow other strategies (e.g. a jms
        // queue publisher) we might also register short names
        // here with the strategyselector.  the initiator would
        // then need to accept the strategy as a config setting
    }
}