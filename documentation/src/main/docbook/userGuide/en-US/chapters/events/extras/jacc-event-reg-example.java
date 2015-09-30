import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.secure.internal.JACCPreDeleteEventListener;
import org.hibernate.secure.internal.JACCPreInsertEventListener;
import org.hibernate.secure.internal.JACCPreLoadEventListener;
import org.hibernate.secure.internal.JACCPreUpdateEventListener;
import org.hibernate.secure.internal.JACCSecurityListener;

public class JaccEventListenerIntegrator implements Integrator {

	private static final DuplicationStrategy JACC_DUPLICATION_STRATEGY = new DuplicationStrategy() {
		@Override
		public boolean areMatch(Object listener, Object original) {
			return listener.getClass().equals( original.getClass() ) &&
					JACCSecurityListener.class.isInstance( original );
		}

		@Override
		public Action getAction() {
			return Action.KEEP_ORIGINAL;
		}
	};

	@Override
	@SuppressWarnings( {"unchecked"})
	public void integrate(
			Configuration configuration,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
		boolean isSecurityEnabled = configuration.getProperties().containsKey( AvailableSettings.JACC_ENABLED );
		if ( !isSecurityEnabled ) {
			return;
		}

		final EventListenerRegistry eventListenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );
		eventListenerRegistry.addDuplicationStrategy( JACC_DUPLICATION_STRATEGY );

		final String jaccContextId = configuration.getProperty( Environment.JACC_CONTEXTID );
		eventListenerRegistry.prependListeners( EventType.PRE_DELETE, new JACCPreDeleteEventListener(jaccContextId) );
		eventListenerRegistry.prependListeners( EventType.PRE_INSERT, new JACCPreInsertEventListener(jaccContextId) );
		eventListenerRegistry.prependListeners( EventType.PRE_UPDATE, new JACCPreUpdateEventListener(jaccContextId) );
		eventListenerRegistry.prependListeners( EventType.PRE_LOAD, new JACCPreLoadEventListener(jaccContextId) );
	}
}