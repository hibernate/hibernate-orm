public class DisabledEventPublishingServiceImpl implements EventPublishingService {
	public static DisabledEventPublishingServiceImpl INSTANCE = new DisabledEventPublishingServiceImpl();

	private DisabledEventPublishingServiceImpl() {
	}

	@Override
	public void publish(Event theEvent) {
		// nothing to do...
	}
}