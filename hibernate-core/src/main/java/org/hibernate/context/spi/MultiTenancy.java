package org.hibernate.context.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.Incubating;
import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.resource.beans.internal.Helper;
import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;

import java.util.Map;
import javax.naming.Context;
import javax.sql.DataSource;

import static org.hibernate.cfg.JdbcSettings.DATASOURCE;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_CREDENTIALS_MAPPER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_DATASOURCE_MAPPER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_SCHEMA_MAPPER;
import static org.hibernate.cfg.MultiTenancySettings.TENANT_IDENTIFIER_TO_USE_FOR_ANY_KEY;

/**
 * Exposes useful multitenancy-related strategy objects.
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
public class MultiTenancy implements Service {
	private final boolean multiTenancyEnabled;
	private final Map<String, Object> settings;
	private final Object tenantIdentifierForAny;
	private final JndiService jndiService;
	private CurrentTenantIdentifierResolver<Object> tenantIdentifierResolver;
	private TenantSchemaMapper<Object> tenantSchemaMapper;
	private TenantCredentialsMapper<Object> tenantCredentialsMapper;
	private TenantDataSourceMapper<Object> tenantDataSourceMapper;

	@Internal
	public MultiTenancy(
			@Nonnull Map<String, Object> settings,
			@Nonnull ServiceRegistry serviceRegistry) {
		this.settings = settings;
		multiTenancyEnabled = serviceRegistry.getService( MultiTenantConnectionProvider.class ) != null;
		tenantIdentifierForAny = settings.get( TENANT_IDENTIFIER_TO_USE_FOR_ANY_KEY );
		jndiService = serviceRegistry.requireService( JndiService.class );
		tenantIdentifierResolver = getTenantIdentifierResolver( settings, serviceRegistry );
		tenantSchemaMapper = getTenantSchemaMapper( settings, serviceRegistry );
		tenantCredentialsMapper = getTenantCredentialsMapper( settings, serviceRegistry );
		tenantDataSourceMapper = getTenantDataSourceMapper( settings, serviceRegistry );
	}

	/**
	 * Is a {@link MultiTenantConnectionProvider} available?
	 */
	public static boolean isMultiTenancyEnabled(@Nonnull ServiceRegistry serviceRegistry) {
		return serviceRegistry.getService( MultiTenantConnectionProvider.class ) != null;
	}

	/**
	 * Obtain the configured {@link CurrentTenantIdentifierResolver}.
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static CurrentTenantIdentifierResolver<Object> getTenantIdentifierResolver(
			@Nonnull Map<String,Object> settings, @Nonnull ServiceRegistry registry) {
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
			@Nonnull Map<String,Object> settings, @Nonnull ServiceRegistry registry) {
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
			@Nonnull Map<String,Object> settings, @Nonnull ServiceRegistry registry) {
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
	 * For {@code "default"}, resolves the common JNDI namespace from the configured
	 * data source or context and returns a mapper using conventional tenant names.
	 *
	 * @since 8.0
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static TenantDataSourceMapper<Object> getTenantDataSourceMapper(
			@Nonnull Map<String,Object> settings, @Nonnull ServiceRegistry registry) {
		final Object setting = settings.get( MULTI_TENANT_DATASOURCE_MAPPER );
		if ( "default".equals( setting ) ) {
			// An explicitly supplied multitenant provider retains its own naming rules.
			return isMultiTenancyEnabled( registry )
					? null
					: defaultDataSourceMapper( settings, registry );
		}
		else {
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

	public boolean isMultiTenancyEnabled() {
		return multiTenancyEnabled || tenantDataSourceMapper != null;
	}

	@Nullable
	public String getBootstrapDataSourceJndiName() {
		if ( settings.get( DATASOURCE ) instanceof String jndiName
				&& jndiService.locate( jndiName ) instanceof DataSource ) {
			return jndiName;
		}
		else {
			return tenantIdentifierForAny == null
					? null
					: getDataSourceJndiName( tenantIdentifierForAny );
		}
	}

	@Nullable
	public String getDataSourceJndiName(Object tenantIdentifier) {
		if ( tenantDataSourceMapper == null
				&& settings.get( DATASOURCE ) instanceof String jndiName ) {
			tenantDataSourceMapper = defaultDataSourceMapper( jndiName, jndiService );
		}
		return tenantDataSourceMapper == null
				? null
				: tenantDataSourceMapper.dataSourceName( tenantIdentifier );
	}

	@Nullable
	public CurrentTenantIdentifierResolver<Object> getTenantIdentifierResolver() {
		return tenantIdentifierResolver;
	}

	@Nullable
	public TenantSchemaMapper<Object> getTenantSchemaMapper() {
		return tenantSchemaMapper;
	}

	@Nullable
	public TenantCredentialsMapper<Object> getTenantCredentialsMapper() {
		return tenantCredentialsMapper;
	}

	@Nullable
	public TenantDataSourceMapper<Object> getTenantDataSourceMapper() {
		return tenantDataSourceMapper;
	}

	@Internal
	public void setCurrentTenantIdentifierResolver(@Nullable CurrentTenantIdentifierResolver<?> resolver) {
		//noinspection unchecked
		tenantIdentifierResolver = (CurrentTenantIdentifierResolver<Object>) resolver;
	}

	@Internal
	public void setTenantSchemaMapper(@Nullable TenantSchemaMapper<?> mapper) {
		//noinspection unchecked
		tenantSchemaMapper = (TenantSchemaMapper<Object>) mapper;
	}

	@Internal
	public void setTenantCredentialsMapper(@Nullable TenantCredentialsMapper<?> mapper) {
		//noinspection unchecked
		tenantCredentialsMapper = (TenantCredentialsMapper<Object>) mapper;
	}

	@Nonnull
	private static TenantDataSourceMapper<Object> defaultDataSourceMapper(
			@Nonnull Map<String, Object> settings, @Nonnull ServiceRegistry registry) {
		if ( !(settings.get( DATASOURCE ) instanceof String jndiName) ) {
			throw new HibernateException( "The default tenant data source mapper requires a JNDI name in '"
					+ DATASOURCE + "'" );
		}
		else {
			return defaultDataSourceMapper( jndiName, registry.requireService( JndiService.class ) );
		}
	}

	@Nonnull
	private static TenantDataSourceMapper<Object> defaultDataSourceMapper(
			@Nonnull String jndiName, @Nonnull JndiService jndiService) {
		final Object namedObject = jndiService.locate( jndiName );
		if ( namedObject instanceof Context ) {
			return new DefaultTenantDataSourceMapper( jndiName );
		}
		else if ( namedObject instanceof DataSource
				&& jndiName.lastIndexOf( '/' ) > 0 ) {
			return new DefaultTenantDataSourceMapper(
					jndiName.substring( 0, jndiName.lastIndexOf( '/' ) ) );
		}
		else {
			throw new HibernateException( "Cannot determine the tenant JNDI namespace from '" + jndiName + "'" );
		}
	}

	private record DefaultTenantDataSourceMapper(String baseJndiNamespace)
			implements TenantDataSourceMapper<Object> {
		@Override
		@Nonnull
		public String dataSourceName(@Nonnull Object tenantIdentifier) {
			return baseJndiNamespace + '/' + tenantIdentifier;
		}
	}
}
