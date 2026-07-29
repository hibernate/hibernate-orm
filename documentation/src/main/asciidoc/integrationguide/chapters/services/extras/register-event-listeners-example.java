public class MyIntegrator implements org.hibernate.integrator.spi.Integrator {

    public void integrate(
            Metadata metadata,
            Integrator.Context context,
            SessionFactoryImplementor sessionFactory) {
        // EventListenerRegistry is the factory-owned component with which
        // event listeners are registered.
        final EventListenerRegistry eventListenerRegistry = sessionFactory.getEventListenerRegistry();

        // If you wish to have custom determination and handling of "duplicate" listeners, you would have to add an
        // implementation of the org.hibernate.event.service.spi.DuplicationStrategy contract like this
        eventListenerRegistry.addDuplicationStrategy( myDuplicationStrategy );

        // EventListenerRegistry defines 3 ways to register listeners:
        //     1) This form overrides any existing registrations with
        eventListenerRegistry.setListeners( EventType.AUTO_FLUSH, myCompleteSetOfListeners );
        //     2) This form adds the specified listener(s) to the beginning of the listener chain
        eventListenerRegistry.prependListeners( EventType.AUTO_FLUSH, myListenersToBeCalledFirst );
        //     3) This form adds the specified listener(s) to the end of the listener chain
        eventListenerRegistry.appendListeners( EventType.AUTO_FLUSH, myListenersToBeCalledLast );
    }
}
