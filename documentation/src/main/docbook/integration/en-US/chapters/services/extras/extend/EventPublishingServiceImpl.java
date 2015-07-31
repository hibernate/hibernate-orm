public class EventPublishingServiceImpl
		implements EventPublishingService,
				Configurable,
				Startable,
				Stoppable,
				ServiceRegistryAwareService {

	private ServiceRegistryImplementor serviceRegistry;
	private String jmsConnectionFactoryName;
	private String destinationName;

	private Connection jmsConnection;
	private Session jmsSession;
	private MessageProducer publisher;

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public void configure(Map configurationValues) {
		this.jmsConnectionFactoryName = configurationValues.get( JMS_CONNECTION_FACTORY_NAME_SETTING );
		this.destinationName = configurationValues.get( JMS_DESTINATION_NAME_SETTING );
	}

	@Override
	public void start() {
		final JndiService jndiService = serviceRegistry.getService( JndiService.class );
		final ConnectionFactory jmsConnectionFactory = jndiService.locate( jmsConnectionFactoryName );

		this.jmsConnection = jmsConnectionFactory.createConnection();
		this.jmsSession = jmsConnection.createSession( true,  Session.AUTO_ACKNOWLEDGE );

		final Destination destination = jndiService.locate( destinationName );

		this.publisher = jmsSession.createProducer( destination );
	}

	@Override
	public void publish(Event theEvent) {
		publisher.send( theEvent );
	}

	@Override
	public void stop() {
		publisher.close();
		jmsSession.close();
		jmsConnection.close();
	}
}