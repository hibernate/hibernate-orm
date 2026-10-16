package org.hibernate.orm.test.events;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.Interceptor;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.type.Type;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vlad Mihalcea
 */
@BaseUnitTest
public class InterceptorTest {
	private static final Logger LOGGER = Logger.getLogger( InterceptorTest.class );

	@Test
	public void testSessionInterceptor() {
		try ( var sessionFactory = configuration().createEntityManagerFactory() ) {
			sessionFactory.inTransaction( session -> session.persist( new Customer( 1L, "John Doe" ) ) );
			var interceptor = new LoggingInterceptor();
			// tag::events-interceptors-session-scope-example[]
			try ( var session = sessionFactory.withOptions().interceptor( interceptor ).openSession() ) {
				var transaction = session.beginTransaction();
				Customer customer = session.find( Customer.class, 1L );
				customer.name = "Mr. John Doe";
				transaction.commit();
			}
			// end::events-interceptors-session-scope-example[]
			assertThat( interceptor.changes ).hasValue( 1 );
			try ( var session = sessionFactory.openSession() ) {
				assertThat( session.find( Customer.class, 1L ).name ).isEqualTo( "Mr. John Doe" );
			}
		}
	}

	@Test
	public void testSessionFactoryInterceptor() {
		// tag::events-interceptors-session-factory-scope-example[]
		var interceptor = new LoggingInterceptor();
		var configuration = new HibernatePersistenceConfiguration( "intercepted" )
				.managedClasses( Customer.class )
				.property( "hibernate.session_factory.interceptor", interceptor );
		// end::events-interceptors-session-factory-scope-example[]
		configuration.properties( ServiceRegistryUtil.createBaseSettingsWithExport() );
		try ( var factory = configuration.createEntityManagerFactory() ) {
			factory.inTransaction( session -> session.persist( new Customer( 1L, "John Doe" ) ) );
			factory.inTransaction( session -> session.find( Customer.class, 1L ).name = "First change" );
			factory.inTransaction( session -> session.find( Customer.class, 1L ).name = "Second change" );
			assertThat( interceptor.changes ).hasValue( 2 );
		}
	}

	private static HibernatePersistenceConfiguration configuration() {
		return new HibernatePersistenceConfiguration( "interceptor" )
				.managedClasses( Customer.class )
				.properties( ServiceRegistryUtil.createBaseSettingsWithExport() );
	}

	@Entity(name = "Customer")
	public static class Customer {
		@Id
		Long id;
		String name;

		public Customer() {}

		Customer(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	// tag::events-interceptors-example[]
	public static class LoggingInterceptor implements Interceptor {
		// end::events-interceptors-example[]
		final AtomicInteger changes = new AtomicInteger();

		// tag::events-interceptors-example[]
		@Override
		public boolean onFlushDirty(
				Object entity, Object id, Object[] currentState, Object[] previousState,
				String[] propertyNames, Type[] types) {
			LOGGER.debugv( "Dirty entity {0}#{1}", entity.getClass().getSimpleName(), id );
			// end::events-interceptors-example[]
			changes.incrementAndGet();
			// tag::events-interceptors-example[]
			return false; // observing state, without changing it
		}
	}
	// end::events-interceptors-example[]
}
