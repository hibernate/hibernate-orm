package org.hibernate.engine.jdbc.connections.internal;

import java.util.Map;

import jakarta.annotation.Nonnull;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.context.spi.MultiTenancy;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProviderConfigurationException;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.DataSourceBasedMultiTenantConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.resource.beans.internal.Helper;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_CONNECTION_PROVIDER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_CONNECTION_STRATEGY;
import static org.hibernate.MultiTenantConnectionStrategy.DATABASE;

/**
 * A service initiator for the {@link MultiTenantConnectionProvider} service.
 *
 * @author Steve Ebersole
 */
public class MultiTenantConnectionProviderInitiator implements StandardServiceInitiator<MultiTenantConnectionProvider<?>> {

	/**
	 * Singleton access
	 */
	public static final MultiTenantConnectionProviderInitiator INSTANCE = new MultiTenantConnectionProviderInitiator();

	@Nonnull
	@Override
	public Class<MultiTenantConnectionProvider<?>> getServiceInitiated() {
		//noinspection unchecked
		return (Class<MultiTenantConnectionProvider<?>>) (Class<?>) MultiTenantConnectionProvider.class;
	}

	@Override
	public MultiTenantConnectionProvider<?> initiateService(@Nonnull Map<String, Object> configurationValues, @Nonnull ServiceRegistryImplementor registry) {
		final var strategy = MultiTenancy.getConnectionStrategy( configurationValues );
		if ( !configurationValues.containsKey( MULTI_TENANT_CONNECTION_PROVIDER ) ) {
			final var bean = Helper.getBean(
				Helper.getBeanContainer( registry ),
				MultiTenantConnectionProvider.class,
				true,
				true,
				null
			);
			return bean != null ? bean : strategy == DATABASE ? databaseProvider( configurationValues, registry ) : null;
		}

		final Object configValue = configurationValues.get( MULTI_TENANT_CONNECTION_PROVIDER );
		if ( configValue == null ) {
			if ( strategy == DATABASE ) {
				return databaseProvider( configurationValues, registry );
			}
			if ( strategy != null ) {
				return null;
			}
			// if they also specified the data source *name*, then let's assume they want
			// DataSourceBasedMultiTenantConnectionProviderImpl
			final Object dataSourceConfigValue = configurationValues.get( JdbcSettings.DATASOURCE );
			return dataSourceConfigValue instanceof String
					? new DataSourceBasedMultiTenantConnectionProviderImpl<>()
					: null;
		}
		else if ( configValue instanceof MultiTenantConnectionProvider<?> multiTenantConnectionProvider ) {
			return multiTenantConnectionProvider;
		}
		else {
			final var providerClass = providerClass( registry, configValue );
			try {
				return providerClass.newInstance();
			}
			catch (Exception e) {
				throw new ServiceException( "Unable to instantiate specified multi-tenant connection provider [" + providerClass.getName() + "]", e );
			}
		}
	}

	private static MultiTenantConnectionProvider<?> databaseProvider(
			Map<String, Object> settings, ServiceRegistryImplementor registry) {
		// Inspect the binding without initiating ConnectionProvider, whose initiator
		// itself needs to determine whether a MultiTenantConnectionProvider exists.
		final var ordinaryProviderBinding = registry.locateServiceBinding( ConnectionProvider.class );
		if ( settings.get( JdbcSettings.CONNECTION_PROVIDER ) != null
				|| ordinaryProviderBinding != null && ordinaryProviderBinding.getService() != null ) {
			throw new ConnectionProviderConfigurationException( "'" + MULTI_TENANT_CONNECTION_STRATEGY
					+ "=DATABASE' conflicts with an explicitly configured or registered ConnectionProvider; "
					+ "supply a MultiTenantConnectionProvider instead" );
		}
		if ( !(settings.get( JdbcSettings.DATASOURCE ) instanceof String name) || name.isBlank() ) {
			throw new ConnectionProviderConfigurationException( "'" + MULTI_TENANT_CONNECTION_STRATEGY
					+ "=DATABASE' requires '" + JdbcSettings.DATASOURCE
					+ "' to name a datasource or context in JNDI, or a custom MultiTenantConnectionProvider" );
		}
		return new DataSourceBasedMultiTenantConnectionProviderImpl<>();
	}

	private static Class<? extends MultiTenantConnectionProvider<?>> providerClass(
			ServiceRegistryImplementor registry, Object configValue) {
		if ( configValue instanceof Class<?> configType ) {
			if ( !MultiTenantConnectionProvider.class.isAssignableFrom( configType ) ) {
				throw new ConnectionProviderConfigurationException( "Class '" + configType.getName()
								+ "' does not implement 'MultiTenantConnectionProvider'" );
			}
			@SuppressWarnings("unchecked") // Safe, we just checked
			final var providerClass = (Class<? extends MultiTenantConnectionProvider<?>>) configType;
			return providerClass;
		}
		else {
			final String className = configValue.toString();
			final var classLoaderService = registry.requireService( ClassLoaderService.class );
			try {
				return classLoaderService.classForName( className );
			}
			catch (ClassLoadingException cle) {
				throw new ServiceException( "Unable to locate specified multi-tenant connection provider [" + className + "]", cle );
			}
		}
	}
}
