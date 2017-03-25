public class EventPublishingServiceInitiator
    implements StandardServiceInitiator<EventPublishingService> {

    public static EventPublishingServiceInitiator INSTANCE =
            new EventPublishingServiceInitiator();

    public static final String ENABLE_PUBLISHING_SETTING =
            "com.acme.EventPublishingService.enabled";

    @Override
    public Class<R> getServiceInitiated() {
        return EventPublishingService.class;
    }

    @Override
    public R initiateService(
            Map configurationValues,
            ServiceRegistryImplementor registry) {

        final boolean enabled = extractBoolean(
                configurationValues,
                ENABLE_PUBLISHING_SETTING
        );
        if ( enabled ) {
            return new EventPublishingServiceImpl();
        }
        else {
            return DisabledEventPublishingServiceImpl.INSTANCE;
        }
    }

    ...
}