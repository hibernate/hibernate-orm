package org.hibernate.context.spi;

import jakarta.annotation.Nullable;
import org.hibernate.Incubating;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.resource.beans.internal.Helper;
import org.hibernate.service.ServiceRegistry;

import java.util.Map;

import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_CREDENTIALS_MAPPER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_DATASOURCE_MAPPER;
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

	/**
	 * Obtain the configured {@link TenantDataSourceMapper}.
	 * Returns {@code null} for {@code "default"}, which requests the connection
	 * provider's conventional JNDI naming rules.
	 *
	 * @since 8.0
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static TenantDataSourceMapper<Object> getTenantDataSourceMapper(
			Map<String,Object> settings, ServiceRegistry registry) {
		final Object setting = settings.get( MULTI_TENANT_DATASOURCE_MAPPER );
		if ( "default".equals( setting ) ) {
			// The provider constructs its mapper once the base JNDI namespace is known.
			return null;
		}
		final var tenantDataSourceMapper =
				registry.requireService( StrategySelector.class )
						.resolveStrategy( TenantDataSourceMapper.class, setting );
		if ( tenantDataSourceMapper == null ) {
			return Helper.getBean(
					Helper.getBeanContainer( registry ),
					TenantDataSourceMapper.class,
					true,
					false,
					null
			);
		}
		return tenantDataSourceMapper;
	}
}
