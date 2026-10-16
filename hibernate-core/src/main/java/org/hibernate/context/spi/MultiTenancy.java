package org.hibernate.context.spi;

import jakarta.annotation.Nullable;
import org.hibernate.Incubating;
import org.hibernate.MultiTenantConnectionStrategy;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProviderConfigurationException;
import org.hibernate.resource.beans.internal.Helper;
import org.hibernate.service.ServiceRegistry;

import java.util.Map;
import java.util.Locale;

import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_CREDENTIALS_MAPPER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_CONNECTION_STRATEGY;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_SCHEMA_MAPPER;

/**
 * Exposes useful multitenancy-related strategy objects to user-written components.
 * <p>
 * The operation {@link #getTenantSchemaMapper} is especially useful in any custom
 * implementation of {@link MultiTenantConnectionProvider} which takes on responsibility
 * for {@linkplain MultiTenantConnectionProvider#handlesConnectionSchema setting the schema}.
 *
 * @since 7.1
 *
 * @author Gavin King
 */
@Incubating(since = "7.2")
public class MultiTenancy {

	/**
	 * Obtain the explicitly configured connection strategy, if any.
	 */
	@Nullable
	public static MultiTenantConnectionStrategy getConnectionStrategy(Map<String, Object> settings) {
		final Object value = settings.get( MULTI_TENANT_CONNECTION_STRATEGY );
		if ( value == null ) {
			return null;
		}
		if ( value instanceof MultiTenantConnectionStrategy strategy ) {
			return strategy;
		}
		if ( value instanceof String string ) {
			try {
				return MultiTenantConnectionStrategy.valueOf( string.strip().toUpperCase( Locale.ROOT ) );
			}
			catch (IllegalArgumentException ignored) {
				// Report the setting and its accepted values below.
			}
		}
		throw new ConnectionProviderConfigurationException( "Invalid value for '"
				+ MULTI_TENANT_CONNECTION_STRATEGY + "': expected DATABASE or SCHEMA" );
	}

	/**
	 * Is a {@link MultiTenantConnectionProvider} available?
	 */
	public static boolean isMultiTenancyEnabled(ServiceRegistry serviceRegistry) {
		return serviceRegistry.getService( MultiTenantConnectionProvider.class ) != null;
	}

	/**
	 * Obtain the configured {@link CurrentTenantIdentifierResolver}.
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static CurrentTenantIdentifierResolver<Object> getTenantIdentifierResolver(
			Map<String,Object> settings, StandardServiceRegistry registry) {
		final var currentTenantIdentifierResolver =
				registry.requireService( StrategySelector.class )
						.resolveStrategy( CurrentTenantIdentifierResolver.class,
								settings.get( MULTI_TENANT_IDENTIFIER_RESOLVER ) );
		if ( currentTenantIdentifierResolver == null ) {
			return Helper.getBean(
					Helper.getBeanContainer( registry ),
					CurrentTenantIdentifierResolver.class,
					true,
					false,
					null
			);
		}
		else {
			return currentTenantIdentifierResolver;
		}
	}

	/**
	 * Obtain the configured {@link TenantSchemaMapper}.
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static TenantSchemaMapper<Object> getTenantSchemaMapper(
			Map<String,Object> settings, StandardServiceRegistry registry) {
		final var tenantSchemaMapper =
				registry.requireService( StrategySelector.class )
						.resolveStrategy( TenantSchemaMapper.class,
								settings.get( MULTI_TENANT_SCHEMA_MAPPER ) );
		if ( tenantSchemaMapper == null ) {
			return Helper.getBean(
					Helper.getBeanContainer( registry ),
					TenantSchemaMapper.class,
					true,
					false,
					null
			);
		}
		return tenantSchemaMapper;
	}

	/**
	 * Obtain the configured {@link TenantCredentialsMapper}.
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static TenantCredentialsMapper<Object> getTenantCredentialsMapper(
			Map<String,Object> settings, StandardServiceRegistry registry) {
		final var tenantCredentialsMapper =
				registry.requireService( StrategySelector.class )
						.resolveStrategy( TenantCredentialsMapper.class,
								settings.get( MULTI_TENANT_CREDENTIALS_MAPPER ) );
		if ( tenantCredentialsMapper == null ) {
			return Helper.getBean(
					Helper.getBeanContainer( registry ),
					TenantCredentialsMapper.class,
					true,
					false,
					null
			);
		}
		return tenantCredentialsMapper;
	}
}
