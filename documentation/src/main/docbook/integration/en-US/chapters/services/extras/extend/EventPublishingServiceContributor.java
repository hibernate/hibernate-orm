public class EventPublishingServiceContributor
		implements ServiceContributor {
	@Override
	public void contribute(StandardServiceRegistryBuilder builder) {
		builder.addInitiator( EventPublishingServiceInitiator.INSTANCE );

		// if we wanted to allow other strategies (e.g. a JMS
		// Queue publisher) we might also register short names
		// here with the StrategySelector.  The initiator would
		// then need to accept the strategy as a config setting
	}
}