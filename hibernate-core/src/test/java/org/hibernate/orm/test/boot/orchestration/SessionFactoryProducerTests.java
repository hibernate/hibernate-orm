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
import org.hibernate.annotations.TenantId;
import org.hibernate.binder.internal.TenantIdBinder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.pipeline.spi.SessionFactoryConstructionRequest;
import org.hibernate.boot.pipeline.spi.SessionFactoryProducer;
import org.hibernate.boot.pipeline.spi.ResolvedSessionFactorySettings;
import org.hibernate.boot.spi.AbstractDelegatingSessionFactoryBuilder;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryBuilderFactory;
import org.hibernate.boot.spi.SessionFactoryBuilderImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.PersistenceSettings;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
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

	@BeforeEach
	void resetProducerState() {
		PRODUCER_REQUEST.set( null );
		PRODUCER_NAME.set( null );
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
	@BootstrapServiceRegistry(javaServices = {
			@JavaService(
					role = SessionFactoryBuilderFactory.class,
					impl = NamingSessionFactoryBuilderFactory.class
			),
			@JavaService(
					role = SessionFactoryProducer.class,
					impl = CustomSessionFactoryProducer.class
			)
	})
	void builderFactoryCustomizationsReachProducerRequest(ServiceRegistryScope registryScope) {
		try (SessionFactory sessionFactory = buildMetadata( registryScope )
				.getSessionFactoryBuilder()
				.build()) {
			assertThat( sessionFactory ).isInstanceOf( MarkerSessionFactory.class );
		}

		final var request = PRODUCER_REQUEST.get();
		assertThat( request ).isNotNull();
		assertThat( request.getOptions().getSessionFactoryName() ).isEqualTo( "producer-builder-factory" );
	}

	@Test
	void preparedSessionFactoryObserversAreHonored(ServiceRegistryScope registryScope) {
		final var observer = new RecordingSessionFactoryObserver();

		try (SessionFactory sessionFactory = buildMetadata( registryScope )
				.getSessionFactoryBuilder()
				.addSessionFactoryObservers( observer )
				.build()) {
			assertThat( sessionFactory ).isInstanceOf( SessionFactoryImplementor.class );
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

	public static class NamingSessionFactoryBuilderFactory implements SessionFactoryBuilderFactory {
		@Override
		public SessionFactoryBuilder getSessionFactoryBuilder(
				MetadataImplementor metadata,
				SessionFactoryBuilderImplementor defaultBuilder) {
			return new NamingSessionFactoryBuilder( defaultBuilder );
		}
	}

	private static class NamingSessionFactoryBuilder
			extends AbstractDelegatingSessionFactoryBuilder<NamingSessionFactoryBuilder> {
		private NamingSessionFactoryBuilder(SessionFactoryBuilderImplementor delegate) {
			super( delegate );
			delegate.applyName( "producer-builder-factory" );
		}

		@Override
		protected NamingSessionFactoryBuilder getThis() {
			return this;
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
