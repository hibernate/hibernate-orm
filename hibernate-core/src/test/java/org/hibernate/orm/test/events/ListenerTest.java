package org.hibernate.orm.test.events;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.Interceptor;
import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.orm.test.events.EventsGuideTests.PlainRecord;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.type.Type;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vlad Mihalcea
 */
@BaseUnitTest
public class ListenerTest {
	@Test
	void nativeListenersPreserveLoadingAndInterceptors() {
		var observed = new ArrayList<String>();
		// tag::events-native-registration[]
		var configuration = new HibernatePersistenceConfiguration( "native-events" )
				.managedClasses( PlainRecord.class )
				.property( "hibernate.integrator_provider",
						(IntegratorProvider) () -> List.of( new LoadIntegrator( observed::add ) ) );
		// end::events-native-registration[]
		configuration.properties( ServiceRegistryUtil.createBaseSettingsWithExport() );
		configuration.property( "hibernate.session_factory.interceptor", new Interceptor() {
			@Override
			public boolean onLoad(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
				observed.add( "interceptor-load" );
				return false;
			}
		} );
		try ( var factory = configuration.createEntityManagerFactory() ) {
			factory.inTransaction( session -> session.persist( new PlainRecord( 1, "loaded" ) ) );
			observed.clear();
			try ( var session = factory.openSession() ) {
				assertThat( session.find( PlainRecord.class, 1 ).name ).isEqualTo( "loaded" );
			}
			assertThat( observed ).containsExactly( "load-request", "interceptor-load", "post-load" );
		}
	}

	// tag::events-interceptors-load-listener-example-part1[]
	public static class LoadIntegrator implements Integrator {
		private final Consumer<String> observer;

		public LoadIntegrator(Consumer<String> observer) {
			this.observer = observer;
		}

		@Override
		public void integrate(Metadata metadata, Context context, SessionFactoryImplementor factory) {
			var listeners = factory.getEventListenerRegistry();
			listeners.prependListeners( EventType.LOAD, new ObservingLoadListener( observer ) );
			listeners.appendListeners( EventType.POST_LOAD, event -> observer.accept( "post-load" ) );
		}
	}
	// end::events-interceptors-load-listener-example-part1[]

	// tag::events-interceptors-load-listener-example-part2[]
	public static class ObservingLoadListener implements LoadEventListener {
		private final Consumer<String> observer;

		public ObservingLoadListener(Consumer<String> observer) {
			this.observer = observer;
		}

		@Override
		public void onLoad(LoadEvent event, LoadType loadType) {
			observer.accept( "load-request" );
		}
	}
	// end::events-interceptors-load-listener-example-part2[]
}
