package org.hibernate.orm.test.events;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.StatelessSessionImplementor;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.orm.test.events.EventsGuideTests.PlainRecord;
import org.hibernate.orm.test.cdi.testsupport.CdiContainer;
import org.hibernate.orm.test.cdi.testsupport.CdiContainerScope;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.type.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies the documented interceptor configuration against a real CDI container.
///
/// @author Steve Ebersole
@BaseUnitTest
class CdiInterceptorGuideTests {

	@ParameterizedTest
	@ValueSource(strings = { "immediate", "delayed", "extended" })
	@CdiContainer(beanClasses = { AuditingInterceptor.class, AuditService.class })
	void sharedInterceptorInjectionAndShutdown(String mode, CdiContainerScope cdi) {
		Object beanManager = mode.equals( "extended" ) ? cdi.getExtendedBeanManager() : cdi.getBeanManager();
		// tag::events-cdi-shared-configuration[]
		var configuration = new HibernatePersistenceConfiguration( "shared-cdi-interceptor" )
				.managedClasses( PlainRecord.class )
				.property( "jakarta.persistence.bean.manager", beanManager )
				.property( "hibernate.session_factory.interceptor", AuditingInterceptor.class );
		// end::events-cdi-shared-configuration[]
		configuration.properties( ServiceRegistryUtil.createBaseSettingsWithExport() );
		configuration.property( "hibernate.delay_cdi_access", mode.equals( "delayed" ) );
		final AuditingInterceptor instance;
		try ( var factory = configuration.createEntityManagerFactory() ) {
			if ( mode.equals( "extended" ) ) {
				assertThat( cdi.isContainerAvailable() ).isFalse();
				cdi.triggerReadyForUse();
			}
			try ( var first = factory.openSession(); var second = factory.openSession() ) {
				instance = interceptor( first );
				assertThat( interceptor( second ) ).isSameAs( instance );
				assertInjected( instance );
				persist( first, 1 );
				persist( second, 2 );
				assertThat( instance.audit.creations ).hasValue( 2 );
			}
			assertThat( instance.destroyed ).isFalse();
		}
		assertThat( instance.destroyed ).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { "immediate", "delayed", "extended" })
	@CdiContainer(beanClasses = { AuditingInterceptor.class, AuditService.class })
	void perSessionInterceptorInjectionAndRelease(String mode, CdiContainerScope cdi) {
		Object beanManager = mode.equals( "extended" ) ? cdi.getExtendedBeanManager() : cdi.getBeanManager();
		// tag::events-cdi-session-configuration[]
		var configuration = new HibernatePersistenceConfiguration( "session-cdi-interceptor" )
				.managedClasses( PlainRecord.class )
				.property( "jakarta.persistence.bean.manager", beanManager )
				.property( "hibernate.session_factory.session_scoped_interceptor", AuditingInterceptor.class );
		// end::events-cdi-session-configuration[]
		configuration.properties( ServiceRegistryUtil.createBaseSettingsWithExport() );
		configuration.property( "hibernate.delay_cdi_access", mode.equals( "delayed" ) );
		try ( var factory = configuration.createEntityManagerFactory() ) {
			if ( mode.equals( "extended" ) ) {
				cdi.triggerReadyForUse();
			}
			final AuditingInterceptor firstInstance;
			final AuditingInterceptor secondInstance;
			try ( var second = factory.openSession() ) {
				secondInstance = interceptor( second );
				try ( var first = factory.openSession() ) {
					firstInstance = interceptor( first );
					assertThat( firstInstance ).isNotSameAs( secondInstance );
					assertInjected( firstInstance );
					assertInjected( secondInstance );
					persist( first, 1 );
					assertThat( firstInstance.audit.creations ).hasValue( 1 );
					assertThat( secondInstance.audit.creations ).hasValue( 0 );
				}
				assertThat( firstInstance.destroyed ).isTrue();
				assertThat( secondInstance.destroyed ).isFalse();
			}
			assertThat( secondInstance.destroyed ).isTrue();
		}
	}

	@Test
	@CdiContainer(beanClasses = { AuditingInterceptor.class, AuditService.class })
	void statelessSessionReleasesItsManagedInterceptor(CdiContainerScope cdi) {
		try ( var factory = EventsGuideTests.configuration( PlainRecord.class )
				.property( "jakarta.persistence.bean.manager", cdi.getBeanManager() )
				.property( "hibernate.session_factory.session_scoped_interceptor", AuditingInterceptor.class )
				.createEntityManagerFactory() ) {
			final AuditingInterceptor instance;
			try ( var session = factory.openStatelessSession() ) {
				instance = (AuditingInterceptor) ((StatelessSessionImplementor) session).getInterceptor();
				assertInjected( instance );
			}
			assertThat( instance.destroyed ).isTrue();
		}
	}

	private static void assertInjected(AuditingInterceptor interceptor) {
		assertThat( interceptor.audit ).isNotNull();
		assertThat( interceptor.initialized ).isTrue();
		assertThat( interceptor.destroyed ).isFalse();
	}

	private static AuditingInterceptor interceptor(Session session) {
		return (AuditingInterceptor) ((SessionImplementor) session).getInterceptor();
	}

	private static void persist(Session session, int id) {
		var transaction = session.beginTransaction();
		session.persist( new PlainRecord( id, "created" ) );
		transaction.commit();
	}

	// tag::events-cdi-interceptor[]
	@Dependent
	public static class AuditingInterceptor implements Interceptor {
		@Inject
		AuditService audit;

		private boolean initialized;
		private boolean destroyed;

		@PostConstruct
		void initialize() {
			initialized = true;
		}

		@PreDestroy
		void destroy() {
			destroyed = true;
		}

		@Override
		public boolean onPersist(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
			audit.recordCreation();
			return false;
		}
	}
	// end::events-cdi-interceptor[]

	@Dependent
	public static class AuditService {
		final AtomicInteger creations = new AtomicInteger();

		public void recordCreation() {
			creations.incrementAndGet();
		}
	}
}
