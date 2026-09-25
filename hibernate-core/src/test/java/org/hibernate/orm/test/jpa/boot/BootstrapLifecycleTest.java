/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.boot;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.boot.spi.MetadataBuilderImplementor;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.PersistenceConfigurationDescriptor;
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
import static org.hibernate.jpa.boot.spi.JpaSettings.METADATA_BUILDER_CONTRIBUTOR;

/// Verifies cleanup before factory ownership and registry lifetime after handoff.
///
/// @author Steve Ebersole
@BaseUnitTest
class BootstrapLifecycleTest {
	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void cancellationClosesRegistriesExactlyOnce(boolean hibernate) {
		final var provider = new CountingProvider();
		final var builder = builder( configuration( hibernate, provider ) );
		final var registry = registry( builder );
		final var parent = (ServiceRegistryImplementor) registry.getParentServiceRegistry();
		registry.requireService( ConnectionProvider.class );
		builder.cancel();
		builder.cancel();
		assertThat( provider.stops ).isEqualTo( 1 );
		assertThat( registry.isActive() ).isFalse();
		assertThat( parent.isActive() ).isFalse();
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void factoryOwnsRegistriesAfterBuild(boolean hibernate) {
		final var provider = new CountingProvider();
		final var builder = builder( configuration( hibernate, provider ) );
		final var registry = registry( builder );
		registry.requireService( ConnectionProvider.class );
		try ( var factory = builder.build() ) {
			builder.cancel();
			builder.cancel();
			assertThat( registry.isActive() ).isTrue();
			assertThat( provider.stops ).isZero();
			assertThat( factory.isOpen() ).isTrue();
			assertThat( builder.metadata() ).isSameAs( builder.metadata() );
		}
		assertThat( registry.isActive() ).isFalse();
		assertThat( provider.stops ).isEqualTo( 1 );
		builder.cancel();
		assertThat( provider.stops ).isEqualTo( 1 );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void preparationFailureClosesRegistries(boolean hibernate) {
		final var provider = new CountingProvider();
		final var registry = new AtomicReference<ServiceRegistryImplementor>();
		final var failure = new IllegalStateException( "preparation" );
		final var configuration = configuration( hibernate, provider )
				.property( METADATA_BUILDER_CONTRIBUTOR, (MetadataBuilderContributor) metadataBuilder -> {
					final var services = ((MetadataBuilderImplementor) metadataBuilder).getBootstrapContext().getServiceRegistry();
					registry.set( (ServiceRegistryImplementor) services );
					services.requireService( ConnectionProvider.class );
					throw failure;
				} );
		assertThatThrownBy( () -> builder( configuration ) ).isSameAs( failure );
		assertThat( registry.get().isActive() ).isFalse();
		assertThat( provider.stops ).isEqualTo( 1 );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void metadataFailureDuringBuildClosesRegistries(boolean hibernate) {
		final var provider = new CountingProvider();
		final var builder = builder( configuration( hibernate, provider ).managedClass( InvalidEntity.class ) );
		registry( builder ).requireService( ConnectionProvider.class );
		assertThatThrownBy( builder::metadata ).isInstanceOf( org.hibernate.AnnotationException.class );
		assertThat( registry( builder ).isActive() ).isTrue();
		assertThat( provider.stops ).isZero();
		assertThatThrownBy( builder::build ).isInstanceOf( org.hibernate.AnnotationException.class );
		assertThat( registry( builder ).isActive() ).isFalse();
		assertThat( provider.stops ).isEqualTo( 1 );
		builder.cancel();
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void factoryConstructionFailureRetainsWrappingAndCleansUp(boolean hibernate) {
		final var provider = new CountingProvider();
		final var failure = new IllegalStateException( "factory creation" );
		final var configuration = configuration( hibernate, provider ).property( SESSION_FACTORY_OBSERVER,
				new org.hibernate.SessionFactoryObserver() {
					@Override
					public void sessionFactoryCreated(org.hibernate.SessionFactory factory) {
						throw failure;
					}
				} );
		final var builder = builder( configuration );
		registry( builder ).requireService( ConnectionProvider.class );
		assertThatThrownBy( builder::build ).isInstanceOf( PersistenceException.class ).hasCause( failure );
		assertThat( registry( builder ).isActive() ).isFalse();
		assertThat( provider.stops ).isEqualTo( 1 );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void schemaCompletionClosesRegistries(boolean hibernate) {
		final var provider = new CountingProvider();
		final var builder = builder( configuration( hibernate, provider ) );
		registry( builder ).requireService( ConnectionProvider.class );
		builder.generateSchema();
		assertThat( registry( builder ).isActive() ).isFalse();
		assertThat( provider.stops ).isEqualTo( 1 );
		builder.cancel();
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void schemaFailureClosesRegistries(boolean hibernate) {
		final var provider = new CountingProvider();
		final var builder = builder( configuration( hibernate, provider )
				.property( JAKARTA_HBM2DDL_DATABASE_ACTION, "invalid-action" ) );
		registry( builder ).requireService( ConnectionProvider.class );
		assertThatThrownBy( builder::generateSchema ).isInstanceOf( PersistenceException.class );
		assertThat( registry( builder ).isActive() ).isFalse();
		assertThat( provider.stops ).isEqualTo( 1 );
	}

	@Test
	void settingsAssemblyFailureClosesBootstrapRegistry() {
		final var stops = new java.util.concurrent.atomic.AtomicInteger();
		final var loader = new org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl() {
			@Override
			public void stop() {
				stops.incrementAndGet();
				super.stop();
			}
		};
		assertThatThrownBy( () -> new EntityManagerFactoryBuilderImpl(
				new PersistenceConfigurationDescriptor( configuration( false, new CountingProvider() ) ),
				Map.of( org.hibernate.cfg.AvailableSettings.JAKARTA_TRANSACTION_TYPE, "invalid" ), loader ) )
				.isInstanceOf( PersistenceException.class ).hasMessageContaining( "Unknown TransactionType" );
		assertThat( stops.get() ).isEqualTo( 1 );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void subsequentFailureCannotReclaimFactoryOwnership(boolean hibernate) {
		final var provider = new CountingProvider();
		final var cfg = configuration( hibernate, provider );
		final var builder = cfg instanceof HibernatePersistenceConfiguration h
				? new FailingBuilder( h ) : new FailingBuilder( new PersistenceConfigurationDescriptor( cfg ) );
		registry( builder ).requireService( ConnectionProvider.class );
		try ( var factory = builder.build() ) {
			builder.failure = new IllegalStateException( "later operation" );
			assertThatThrownBy( builder::build ).isSameAs( builder.failure );
			assertThat( registry( builder ).isActive() ).isTrue();
			assertThat( provider.stops ).isZero();
			assertThat( factory.isOpen() ).isTrue();
		}
		assertThat( provider.stops ).isEqualTo( 1 );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void schemaFailureSurvivesCancellationFailure(boolean hibernate) {
		final var cfg = configuration( hibernate, new CountingProvider() );
		final var builder = cfg instanceof HibernatePersistenceConfiguration h
				? new FailingBuilder( h ) : new FailingBuilder( new PersistenceConfigurationDescriptor( cfg ) );
		builder.failure = new IllegalStateException( "schema setup" );
		builder.cancelFailure = new IllegalStateException( "cancel" );
		assertThatThrownBy( builder::generateSchema ).isInstanceOf( PersistenceException.class )
				.hasCause( builder.failure ).satisfies( failure ->
						assertThat( failure.getSuppressed() ).containsExactly( builder.cancelFailure ) );
		assertThat( registry( builder ).isActive() ).isFalse();
	}

	private static class FailingBuilder extends EntityManagerFactoryBuilderImpl {
		RuntimeException failure;
		RuntimeException cancelFailure;

		FailingBuilder(HibernatePersistenceConfiguration cfg) {
			super( cfg );
		}

		FailingBuilder(PersistenceConfigurationDescriptor descriptor) {
			super( descriptor, Map.of() );
		}

		@Override
		protected org.hibernate.boot.SessionFactoryBuilder populateSessionFactoryBuilder() {
			if ( failure != null ) {
				throw failure;
			}
			return super.populateSessionFactoryBuilder();
		}

		@Override
		public void cancel() {
			super.cancel();
			if ( cancelFailure != null ) {
				throw cancelFailure;
			}
		}
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

	private static EntityManagerFactoryBuilderImpl builder(PersistenceConfiguration cfg) {
		return cfg instanceof HibernatePersistenceConfiguration hibernate ? new EntityManagerFactoryBuilderImpl( hibernate )
				: new EntityManagerFactoryBuilderImpl( new PersistenceConfigurationDescriptor( cfg ), Map.of() );
	}

	private static ServiceRegistryImplementor registry(EntityManagerFactoryBuilderImpl builder) {
		return (ServiceRegistryImplementor) builder.getStandardServiceRegistry();
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
