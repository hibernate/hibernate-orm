public interface EventPublishingService extends Service {

    void publish(Event theEvent);
}