/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.orchestration;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.StatementObserver;
import org.hibernate.annotations.TenantId;
import org.hibernate.binder.internal.TenantIdBinder;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.internal.SessionFactoryOptionsCollector;
import org.hibernate.boot.pipeline.internal.SessionFactoryPipeline;
import org.hibernate.boot.pipeline.spi.SessionFactoryConstructionRequest;
import org.hibernate.boot.pipeline.spi.SessionFactoryProducer;
import org.hibernate.boot.pipeline.spi.ResolvedSessionFactorySettings;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.PersistenceSettings;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry.JavaService;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the final SessionFactory construction producer seam.
 *
 * @author Steve Ebersole
 */
@ServiceRegistry(settings = {
		@Setting(name = JdbcSettings.URL, value = "jdbc:h2:mem:session-factory-producer;DB_CLOSE_DELAY=-1"),
		@Setting(name = JdbcSettings.USER, value = "sa"),
		@Setting(name = JdbcSettings.PASS, value = "")
})
@SuppressWarnings("JUnitMalformedDeclaration")
class SessionFactoryProducerTests {
	private static final AtomicReference<SessionFactoryConstructionRequest> PRODUCER_REQUEST = new AtomicReference<>();
	private static final AtomicReference<String> PRODUCER_NAME = new AtomicReference<>();
	private static final AtomicBoolean RECORDING_INTEGRATED = new AtomicBoolean();
	private static final AtomicBoolean RECORDING_DISINTEGRATED = new AtomicBoolean();
	private static final AtomicBoolean FAILING_INTEGRATED = new AtomicBoolean();
	private static final AtomicBoolean FAILING_DISINTEGRATED = new AtomicBoolean();

	@BeforeEach
	void resetProducerState() {
		PRODUCER_REQUEST.set( null );
		PRODUCER_NAME.set( null );
		RECORDING_INTEGRATED.set( false );
		RECORDING_DISINTEGRATED.set( false );
		FAILING_INTEGRATED.set( false );
		FAILING_DISINTEGRATED.set( false );
	}

	@Test
	@BootstrapServiceRegistry(
			javaServices = @JavaService(
					role = SessionFactoryProducer.class,
					impl = CustomSessionFactoryProducer.class
			)
	)
	void serviceLoadedProducerCanReturnCustomSessionFactory(ServiceRegistryScope registryScope) {
		try (SessionFactory sessionFactory = buildMetadata( registryScope ).buildSessionFactory()) {
			assertThat( sessionFactory ).isInstanceOf( MarkerSessionFactory.class );
		}

		final var request = PRODUCER_REQUEST.get();
		assertThat( request ).isNotNull();
		assertThat( request.getMetadata() ).isInstanceOf( MetadataImplementor.class );
		assertThat( request.getOptions() ).isInstanceOf( SessionFactoryOptions.class );
		assertThat( request.getServiceRegistry() ).isSameAs( registryScope.getRegistry() );
	}

	@Test
	@BootstrapServiceRegistry(javaServices = {
			@JavaService(
					role = SessionFactoryProducer.class,
					impl = CustomSessionFactoryProducer.class
			),
			@JavaService(
					role = SessionFactoryProducer.class,
					impl = OtherSessionFactoryProducer.class
			)
	})
	void multipleServiceLoadedProducersFail(ServiceRegistryScope registryScope) {
		assertThatThrownBy( () -> buildMetadata( registryScope ).buildSessionFactory() )
				.isInstanceOf( HibernateException.class )
				.hasMessageContaining( "Multiple active SessionFactoryProducer definitions were discovered" );
	}

	@Test
	@ServiceRegistry(settings = {
			@Setting(name = JdbcSettings.URL, value = "jdbc:h2:mem:session-factory-producer;DB_CLOSE_DELAY=-1"),
			@Setting(name = JdbcSettings.USER, value = "sa"),
			@Setting(name = JdbcSettings.PASS, value = ""),
			@Setting(name = PersistenceSettings.SESSION_FACTORY_PRODUCER, value = "other")
	})
	@BootstrapServiceRegistry(javaServices = {
			@JavaService(
					role = SessionFactoryProducer.class,
					impl = CustomSessionFactoryProducer.class
			),
			@JavaService(
					role = SessionFactoryProducer.class,
					impl = OtherSessionFactoryProducer.class
			)
	})
	void selectedProducerResolvesMultipleServiceLoadedProducers(ServiceRegistryScope registryScope) {
		try (SessionFactory sessionFactory = buildMetadata( registryScope ).buildSessionFactory()) {
			assertThat( sessionFactory ).isInstanceOf( MarkerSessionFactory.class );
		}

		assertThat( PRODUCER_NAME.get() ).isEqualTo( "other" );
	}

	@Test
	@ServiceRegistry(settings = {
			@Setting(name = JdbcSettings.URL, value = "jdbc:h2:mem:session-factory-producer;DB_CLOSE_DELAY=-1"),
			@Setting(name = JdbcSettings.USER, value = "sa"),
			@Setting(name = JdbcSettings.PASS, value = ""),
			@Setting(name = PersistenceSettings.SESSION_FACTORY_PRODUCER, value = "missing")
	})
	@BootstrapServiceRegistry(
			javaServices = @JavaService(
					role = SessionFactoryProducer.class,
					impl = CustomSessionFactoryProducer.class
			)
	)
	void unknownSelectedProducerFails(ServiceRegistryScope registryScope) {
		assertThatThrownBy( () -> buildMetadata( registryScope ).buildSessionFactory() )
				.isInstanceOf( HibernateException.class )
				.hasMessageContaining( "No SessionFactoryProducer named 'missing' was discovered" );
	}

	@Test
	void constructionRequestDoesNotExposeBootContexts() {
		for ( Method method : SessionFactoryConstructionRequest.class.getMethods() ) {
			assertThat( method.getReturnType() )
					.isNotEqualTo( BootstrapContext.class )
					.isNotEqualTo( MetadataBuildingContext.class )
					.isNotEqualTo( ResolvedSessionFactorySettings.class );
		}
	}

	@Test
	void preparedOptionsBridgeObserversAreHonored(ServiceRegistryScope registryScope) {
		final var observer = new RecordingSessionFactoryObserver();
		final StatementObserver statementObserver = (sql, batchPosition) -> {
		};
			final var optionsCollector = new SessionFactoryOptionsCollector();
			optionsCollector.addSessionFactoryObservers( observer );
			optionsCollector.applyStatementObserver( statementObserver );

			try (SessionFactory sessionFactory = SessionFactoryPipeline.build(
					buildMetadata( registryScope ),
					optionsCollector
			)) {
			assertThat( sessionFactory ).isInstanceOf( SessionFactoryImplementor.class );
			assertThat( sessionFactory.unwrap( SessionFactoryImplementor.class ).getStatementObserver() )
					.isSameAs( statementObserver );
			assertThat( observer.created.get() ).isTrue();
			assertThat( observer.closed.get() ).isFalse();
		}

		assertThat( observer.closed.get() ).isTrue();
	}

	@Test
	void preparedFilterAndTenantValuesMatchExistingBehavior(ServiceRegistryScope registryScope) {
		final var metadata = buildMetadata( registryScope, TenantEntity.class );
		metadata.getFilterDefinitions().put(
				"activeFilter",
				new FilterDefinition( "activeFilter", "", true, false, null, null )
		);

		try (SessionFactory sessionFactory = metadata.buildSessionFactory()) {
			final var sessionFactoryImplementor = sessionFactory.unwrap( SessionFactoryImplementor.class );

			assertThat( sessionFactoryImplementor.getAutoEnabledFilters() )
					.extracting( "filterName" )
					.contains( "activeFilter" );
			assertThat( sessionFactoryImplementor.getFilterDefinition( TenantIdBinder.FILTER_NAME ) ).isNotNull();
			assertThat( sessionFactoryImplementor.getTenantIdentifierJavaType().getJavaTypeClass() )
					.isEqualTo( String.class );
		}
	}

	@Test
	@BootstrapServiceRegistry(integrators = RecordingIntegrator.class)
	void integratorDisintegratesOnClose(ServiceRegistryScope registryScope) {
		try (SessionFactory ignored = buildMetadata( registryScope ).buildSessionFactory()) {
			assertThat( RECORDING_INTEGRATED.get() ).isTrue();
			assertThat( RECORDING_DISINTEGRATED.get() ).isFalse();
		}

		assertThat( RECORDING_DISINTEGRATED.get() ).isTrue();
	}

	@Test
	@BootstrapServiceRegistry(integrators = {
			RecordingIntegrator.class,
			FailingIntegrator.class
	})
	void startupFailureDisintegratesOnlySuccessfullyIntegratedIntegrators(ServiceRegistryScope registryScope) {
		assertThatThrownBy( () -> buildMetadata( registryScope ).buildSessionFactory() )
				.isInstanceOf( RuntimeException.class )
				.hasMessageContaining( "failing integrator" );

		assertThat( RECORDING_INTEGRATED.get() ).isTrue();
		assertThat( RECORDING_DISINTEGRATED.get() ).isTrue();
		assertThat( FAILING_INTEGRATED.get() ).isTrue();
		assertThat( FAILING_DISINTEGRATED.get() ).isFalse();
	}

	private static MetadataImplementor buildMetadata(ServiceRegistryScope registryScope) {
		return buildMetadata( registryScope, ProducerEntity.class );
	}

	private static MetadataImplementor buildMetadata(ServiceRegistryScope registryScope, Class<?>... annotatedClasses) {
		final var metadataSources = new MetadataSources( registryScope.getRegistry() );
		for ( var annotatedClass : annotatedClasses ) {
			metadataSources.addAnnotatedClass( annotatedClass );
		}
		return (MetadataImplementor) metadataSources.buildMetadata();
	}

	public interface MarkerSessionFactory extends SessionFactoryImplementor {
	}

	public static class CustomSessionFactoryProducer implements SessionFactoryProducer {
		@Override
		public String getProducerName() {
			return "custom";
		}

		@Override
		public SessionFactoryImplementor buildSessionFactory(SessionFactoryConstructionRequest request) {
			PRODUCER_REQUEST.set( request );
			PRODUCER_NAME.set( getProducerName() );
			return customSessionFactory();
		}
	}

	public static class OtherSessionFactoryProducer implements SessionFactoryProducer {
		@Override
		public String getProducerName() {
			return "other";
		}

		@Override
		public SessionFactoryImplementor buildSessionFactory(SessionFactoryConstructionRequest request) {
			PRODUCER_NAME.set( getProducerName() );
			return customSessionFactory();
		}
	}

	private static class RecordingSessionFactoryObserver implements SessionFactoryObserver {
		private final AtomicBoolean created = new AtomicBoolean();
		private final AtomicBoolean closed = new AtomicBoolean();

		@Override
		public void sessionFactoryCreated(SessionFactory factory) {
			created.set( true );
		}

		@Override
		public void sessionFactoryClosed(SessionFactory factory) {
			closed.set( true );
		}
	}

	public static class RecordingIntegrator implements Integrator {
		@Override
		public void integrate(
				Metadata metadata,
				BootstrapContext bootstrapContext,
				SessionFactoryImplementor sessionFactory) {
			RECORDING_INTEGRATED.set( true );
		}

		@Override
		public void disintegrate(
				SessionFactoryImplementor sessionFactory,
				SessionFactoryServiceRegistry serviceRegistry) {
			RECORDING_DISINTEGRATED.set( true );
		}
	}

	public static class FailingIntegrator implements Integrator {
		@Override
		public void integrate(
				Metadata metadata,
				BootstrapContext bootstrapContext,
				SessionFactoryImplementor sessionFactory) {
			FAILING_INTEGRATED.set( true );
			throw new RuntimeException( "failing integrator" );
		}

		@Override
		public void disintegrate(
				SessionFactoryImplementor sessionFactory,
				SessionFactoryServiceRegistry serviceRegistry) {
			FAILING_DISINTEGRATED.set( true );
		}
	}

	private static SessionFactoryImplementor customSessionFactory() {
		return (SessionFactoryImplementor) Proxy.newProxyInstance(
				SessionFactoryProducerTests.class.getClassLoader(),
				new Class<?>[] { MarkerSessionFactory.class },
				new CustomSessionFactoryInvocationHandler()
		);
	}

	private static class CustomSessionFactoryInvocationHandler implements InvocationHandler {
		private boolean open = true;

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) {
			return switch ( method.getName() ) {
				case "toString" -> "CustomSessionFactory";
				case "hashCode" -> System.identityHashCode( proxy );
				case "equals" -> proxy == args[0];
				case "close" -> {
					open = false;
					yield null;
				}
				case "isOpen" -> open;
				case "unwrap" -> {
					final var type = (Class<?>) args[0];
					yield type.isInstance( proxy ) ? proxy : null;
				}
				default -> defaultValue( method.getReturnType() );
			};
		}

		private static Object defaultValue(Class<?> returnType) {
			if ( !returnType.isPrimitive() || returnType == void.class ) {
				return null;
			}
			if ( returnType == boolean.class ) {
				return false;
			}
			if ( returnType == char.class ) {
				return '\0';
			}
			if ( returnType == byte.class ) {
				return (byte) 0;
			}
			if ( returnType == short.class ) {
				return (short) 0;
			}
			if ( returnType == int.class ) {
				return 0;
			}
			if ( returnType == long.class ) {
				return 0L;
			}
			if ( returnType == float.class ) {
				return 0f;
			}
			if ( returnType == double.class ) {
				return 0d;
			}
			return 0;
		}
	}

	@Entity(name = "ProducerEntity")
	@Table(name = "producer_entity")
	public static class ProducerEntity {
		@Id
		private Integer id;

		@Basic
		private String name;

		protected ProducerEntity() {
		}
	}

	@Entity(name = "TenantEntity")
	@Table(name = "producer_tenant_entity")
	public static class TenantEntity {
		@Id
		private Integer id;

		@TenantId
		private String tenantId;

		protected TenantEntity() {
		}
	}
}
