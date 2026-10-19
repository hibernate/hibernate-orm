package org.hibernate.service.internal;

import jakarta.annotation.Nonnull;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.jdbc.connections.spi.DataSourceBasedMultiTenantConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;
import org.hibernate.service.spi.SessionFactoryServiceInitiatorContext;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceContributor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.hibernate.service.spi.SessionFactoryServiceRegistryFactory;

/**
 * Acts as a service in the {@link org.hibernate.boot.registry.internal.StandardServiceRegistryImpl} whose
 * function is to act as a factory for {@link SessionFactoryServiceRegistryImpl} implementations.
 *
 * @author Steve Ebersole
 */
public class SessionFactoryServiceRegistryFactoryImpl implements SessionFactoryServiceRegistryFactory {
	private final ServiceRegistryImplementor theBasicServiceRegistry;

	public SessionFactoryServiceRegistryFactoryImpl(@Nonnull ServiceRegistryImplementor theBasicServiceRegistry) {
		this.theBasicServiceRegistry = theBasicServiceRegistry;
	}

	@Override
	@Nonnull
	public SessionFactoryServiceRegistry buildServiceRegistry(
			@Nonnull SessionFactoryImplementor sessionFactory,
			@Nonnull SessionFactoryOptions options) {
		final var classLoaderService = options.getServiceRegistry().requireService( ClassLoaderService.class );
		final var builder = new SessionFactoryServiceRegistryBuilderImpl( theBasicServiceRegistry );
		// A builder-supplied mapper arrives after the standard registry is initialized.
		// Scope its provider to this factory so other factories sharing that registry
		// keep their own tenant mappings.
		final var mapper = options.getTenantDataSourceMapper();
		if ( mapper != null ) {
			final var provider = theBasicServiceRegistry.getService( MultiTenantConnectionProvider.class );
			if ( provider == null || provider.getClass() == DataSourceBasedMultiTenantConnectionProviderImpl.class ) {
				builder.addInitiator( new SessionFactoryServiceInitiator<MultiTenantConnectionProvider<?>>() {
					@Override
					@SuppressWarnings({ "unchecked", "rawtypes" })
					public Class<MultiTenantConnectionProvider<?>> getServiceInitiated() {
						return (Class) MultiTenantConnectionProvider.class;
					}

					@Override
					public MultiTenantConnectionProvider<?> initiateService(SessionFactoryServiceInitiatorContext context) {
						return new DataSourceBasedMultiTenantConnectionProviderImpl<>( mapper );
					}
				} );
			}
		}
		for ( var contributor : classLoaderService.loadJavaServices( SessionFactoryServiceContributor.class ) ) {
			contributor.contribute( builder );
		}
		return builder.buildSessionFactoryServiceRegistry( sessionFactory, options );
	}
}
