package org.hibernate.orm.test.jpa.boot;

import java.util.List;
import org.hibernate.boot.pipeline.internal.BootstrapPipeline;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.jpa.boot.spi.TypeContributorList;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import static org.hibernate.jpa.boot.spi.JpaSettings.TYPE_CONTRIBUTORS;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.Entity;
import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.PersistenceException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.AvailableSettings.ALLOW_METADATA_ON_BOOT;
import static org.hibernate.cfg.AvailableSettings.CONNECTION_PROVIDER;
import static org.hibernate.cfg.AvailableSettings.DIALECT;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION;
import static org.hibernate.cfg.AvailableSettings.SESSION_FACTORY_OBSERVER;

/// Verifies cleanup before factory ownership and registry lifetime after handoff.
///
/// @author Steve Ebersole
@BaseUnitTest
class BootstrapLifecycleTest {
	@Test
	void configurationCreatesExactlyOneFactory() {
		final var created = new java.util.ArrayList<org.hibernate.SessionFactory>();
		final var closed = new java.util.ArrayList<org.hibernate.SessionFactory>();
		final var provider = new CountingProvider();
		final var configuration = (HibernatePersistenceConfiguration) configuration( true, provider );
		configuration.property( SESSION_FACTORY_OBSERVER, new org.hibernate.SessionFactoryObserver() {
			@Override
			public void sessionFactoryCreated(org.hibernate.SessionFactory factory) {
				created.add( factory );
			}

			@Override
			public void sessionFactoryClosed(org.hibernate.SessionFactory factory) {
				closed.add( factory );
			}
		} );
		try {
			try ( var factory = configuration.createEntityManagerFactory() ) {
				assertThat( factory.isOpen() ).isTrue();
				assertThat( created ).containsExactly( factory );
				assertThat( closed ).isEmpty();
			}
			assertThat( closed ).containsExactlyElementsOf( created );
			assertThat( provider.stops ).isEqualTo( 1 );
		}
		finally {
			// Also clean up an accidentally discarded factory if the regression returns.
			created.stream().filter( org.hibernate.SessionFactory::isOpen ).forEach( org.hibernate.SessionFactory::close );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void metadataResultClosesRegistriesExactlyOnce(boolean hibernate) {
		final var provider = new CountingProvider();
		final var result = BootstrapPipeline.resolveMetadata( configuration( hibernate, provider ) );
		final var registry = (ServiceRegistryImplementor) result.metadata().getMappingResolutionOptions().getServiceRegistry();
		final var parent = (ServiceRegistryImplementor) registry.getParentServiceRegistry();
		registry.requireService( ConnectionProvider.class );
		assertThat( registry.isActive() ).isTrue();
		result.close();
		result.close();
		assertThat( provider.stops ).isEqualTo( 1 );
		assertThat( registry.isActive() ).isFalse();
		assertThat( parent.isActive() ).isFalse();
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void factoryOwnsRegistriesAfterBuild(boolean hibernate) {
		final var provider = new CountingProvider();
		ServiceRegistryImplementor registry;
		try ( var factory = BootstrapPipeline.build( configuration( hibernate, provider ) ) ) {
			registry = (ServiceRegistryImplementor) factory.unwrap( SessionFactoryImplementor.class ).getSessionFactoryOptions().getServiceRegistry();
			registry.requireService( ConnectionProvider.class );
			assertThat( registry.isActive() ).isTrue();
			assertThat( provider.stops ).isZero();
		}
		assertThat( registry.isActive() ).isFalse();
		assertThat( provider.stops ).isEqualTo( 1 );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void contributionFailureClosesRegistries(boolean hibernate) {
		final var provider = new CountingProvider();
		final var registry = new AtomicReference<ServiceRegistryImplementor>();
		final var failure = new IllegalStateException( "preparation" );
		final var configuration = trackedConfiguration( hibernate, provider, registry, failure );
		assertThatThrownBy( () -> BootstrapPipeline.resolveMetadata( configuration ) )
				.isInstanceOf( PersistenceException.class ).hasCause( failure );
		assertClosed( registry, provider );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void metadataFailureDuringBuildClosesRegistries(boolean hibernate) {
		final var provider = new CountingProvider();
		final var registry = new AtomicReference<ServiceRegistryImplementor>();
		final var configuration = trackedConfiguration( hibernate, provider, registry, null ).managedClass( InvalidEntity.class );
		assertThatThrownBy( () -> BootstrapPipeline.build( configuration ) ).isInstanceOf( PersistenceException.class );
		assertClosed( registry, provider );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void factoryConstructionFailureRetainsWrappingAndCleansUp(boolean hibernate) {
		final var provider = new CountingProvider();
		final var registry = new AtomicReference<ServiceRegistryImplementor>();
		final var failure = new IllegalStateException( "factory creation" );
		final var configuration = trackedConfiguration( hibernate, provider, registry, null )
				.property( SESSION_FACTORY_OBSERVER, new org.hibernate.SessionFactoryObserver() {
					@Override public void sessionFactoryCreated(org.hibernate.SessionFactory factory) { throw failure; }
				} );
		assertThatThrownBy( () -> BootstrapPipeline.build( configuration ) ).isInstanceOf( PersistenceException.class ).hasCause( failure );
		assertClosed( registry, provider );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void schemaCompletionClosesRegistries(boolean hibernate) {
		final var provider = new CountingProvider();
		final var registry = new AtomicReference<ServiceRegistryImplementor>();
		BootstrapPipeline.generateSchema( trackedConfiguration( hibernate, provider, registry, null ) );
		assertClosed( registry, provider );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void schemaFailureClosesRegistries(boolean hibernate) {
		final var provider = new CountingProvider();
		final var registry = new AtomicReference<ServiceRegistryImplementor>();
		final var configuration = trackedConfiguration( hibernate, provider, registry, null )
				.property( JAKARTA_HBM2DDL_DATABASE_ACTION, "invalid-action" );
		assertThatThrownBy( () -> BootstrapPipeline.generateSchema( configuration ) ).isInstanceOf( PersistenceException.class );
		assertClosed( registry, provider );
	}

	private static PersistenceConfiguration trackedConfiguration(boolean hibernate, CountingProvider provider,
			AtomicReference<ServiceRegistryImplementor> registry, RuntimeException failure) {
		final TypeContributor contributor = (contributions, services) -> {
			registry.set( (ServiceRegistryImplementor) services );
			services.requireService( ConnectionProvider.class );
			if ( failure != null ) { throw failure; }
		};
		return configuration( hibernate, provider ).property( TYPE_CONTRIBUTORS, (TypeContributorList) () -> List.of( contributor ) );
	}

	private static void assertClosed(AtomicReference<ServiceRegistryImplementor> registry, CountingProvider provider) {
		assertThat( registry.get() ).isNotNull();
		assertThat( registry.get().isActive() ).isFalse();
		assertThat( provider.stops ).isEqualTo( 1 );
	}

	@Test
	void nativeOwnedRegistryIsClosedOnFailure() {
		final var registry = new AtomicReference<ServiceRegistryImplementor>();
		final var failure = new IllegalStateException( "native build" );
		final var configuration = new Configuration() {
			@Override
			public org.hibernate.SessionFactory buildSessionFactory(ServiceRegistry services) {
				registry.set( (ServiceRegistryImplementor) services );
				throw failure;
			}
		};
		assertThatThrownBy( configuration::buildSessionFactory ).isSameAs( failure );
		assertThat( registry.get().isActive() ).isFalse();
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void nativeSuppliedRegistryRetainsItsOwnPolicy(boolean autoClose) {
		final var registryBuilder = new StandardServiceRegistryBuilder()
				.applySetting( DIALECT, H2Dialect.class.getName() ).applySetting( ALLOW_METADATA_ON_BOOT, false );
		if ( !autoClose ) {
			registryBuilder.disableAutoClose();
		}
		try ( var registry = registryBuilder.build() ) {
			final var configuration = new Configuration().addAnnotatedClass( InvalidEntity.class );
			assertThatThrownBy( () -> configuration.buildSessionFactory( registry ) )
					.isInstanceOf( org.hibernate.AnnotationException.class );
			assertThat( ((ServiceRegistryImplementor) registry).isActive() ).isTrue();
		}
	}

	private static PersistenceConfiguration configuration(boolean hibernate, CountingProvider provider) {
		final PersistenceConfiguration cfg = hibernate ? new HibernatePersistenceConfiguration( "lifecycle" )
				: new PersistenceConfiguration( "lifecycle" );
		return cfg.property( DIALECT, H2Dialect.class.getName() ).property( ALLOW_METADATA_ON_BOOT, false )
				.property( CONNECTION_PROVIDER, provider ).property( HBM2DDL_AUTO, "none" );
	}

	private static class CountingProvider extends UserSuppliedConnectionProviderImpl implements Stoppable {
		int stops;

		@Override
		public void stop() {
			stops++;
		}
	}

	@Entity
	static class InvalidEntity {
		String noIdentifier;
	}
}
