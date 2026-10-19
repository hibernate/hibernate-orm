package org.hibernate.engine.jdbc.connections.spi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.naming.Context;
import javax.sql.DataSource;

import jakarta.annotation.Nonnull;
import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.context.spi.MultiTenancy;
import org.hibernate.context.spi.TenantDataSourceMapper;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;

import static org.hibernate.cfg.JdbcSettings.DATASOURCE;
import static org.hibernate.cfg.MultiTenancySettings.TENANT_IDENTIFIER_TO_USE_FOR_ANY_KEY;

/**
 * A concrete implementation of the {@link MultiTenantConnectionProvider} contract based
 * on a number of reasonable assumptions. A configured {@link TenantDataSourceMapper}
 * supplies full JNDI names for tenant data sources. Without a mapper, or with the
 * {@code default} strategy, we assume that:<ul>
 *     <li>
 *         The {@link DataSource} instances are all available from JNDI named by the tenant
 *         identifier relative to a single base JNDI context.
 *     </li>
 *     <li>
 *         {@value AvailableSettings#DATASOURCE} is a string naming either the {@literal any}
 *         data source or the base JNDI context. If the latter,
 *         {@link MultiTenancySettings#TENANT_IDENTIFIER_TO_USE_FOR_ANY_KEY} must also be set.
 *     </li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class DataSourceBasedMultiTenantConnectionProviderImpl<T>
		extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl<T>
		implements ServiceRegistryAwareService, Stoppable {

	private final Map<T, DataSource> dataSourceMap = new ConcurrentHashMap<>();
	private JndiService jndiService;
	private T tenantIdentifierForAny;
	private String baseJndiNamespace;
	private String jndiName;
	private TenantDataSourceMapper<T> tenantDataSourceMapper;
	private DataSource anyDataSource;

	public DataSourceBasedMultiTenantConnectionProviderImpl() {
	}

	/**
	 * Construct a provider using the given mapper for tenant data source names.
	 * The mapper is retained for the lifetime of this provider.
	 *
	 * @see TenantDataSourceMapper
	 * @since 8.0
	 */
	@Incubating(since = "8.0")
	public DataSourceBasedMultiTenantConnectionProviderImpl(TenantDataSourceMapper<T> mapper) {
		tenantDataSourceMapper = mapper;
	}

	@Override
	protected DataSource selectAnyDataSource() {
		return anyDataSource == null ? selectDataSource( tenantIdentifierForAny ) : anyDataSource;
	}

	@Override
	protected DataSource selectDataSource(T tenantIdentifier) {
		DataSource dataSource = dataSourceMap().get( tenantIdentifier );
		if ( dataSource == null ) {
			dataSource = (DataSource)
					jndiService.locate( tenantDataSourceMapper.dataSourceName( tenantIdentifier ) );
			dataSourceMap().put( tenantIdentifier, dataSource );
		}
		return dataSource;
	}

	private Map<T, DataSource> dataSourceMap() {
		return dataSourceMap;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void injectServices(@Nonnull ServiceRegistryImplementor serviceRegistry) {
		final var configurationService = serviceRegistry.requireService( ConfigurationService.class );
		final var settings = configurationService.getSettings();
		if ( tenantDataSourceMapper == null ) {
			tenantDataSourceMapper = (TenantDataSourceMapper<T>)
					MultiTenancy.getTenantDataSourceMapper( settings, serviceRegistry );
		}
		final boolean customMapper = tenantDataSourceMapper != null;
		final Object dataSourceConfigValue = settings.get( DATASOURCE );
		if ( customMapper && dataSourceConfigValue == null ) {
			jndiService = serviceRegistry.requireService( JndiService.class );
			tenantIdentifierForAny = (T) settings.get( TENANT_IDENTIFIER_TO_USE_FOR_ANY_KEY );
			if ( tenantIdentifierForAny == null ) {
				throw new HibernateException( "A tenant data source mapper requires '" + DATASOURCE
						+ "' or '" + TENANT_IDENTIFIER_TO_USE_FOR_ANY_KEY + "'" );
			}
			return;
		}
		if ( !(dataSourceConfigValue instanceof String configuredJndiName) ) {
			throw new HibernateException( "illegal value for configuration setting '" + DATASOURCE + "'" );
		}
		jndiName = configuredJndiName;

		jndiService = serviceRegistry.getService( JndiService.class );
		if ( jndiService == null ) {
			throw new HibernateException( "Could not locate JndiService from DataSourceBasedMultiTenantConnectionProviderImpl" );
		}

		final Object namedObject = jndiService.locate( this.jndiName );
		if ( namedObject == null ) {
			throw new HibernateException( "JNDI name [" + this.jndiName + "] could not be resolved" );
		}
		else if ( namedObject instanceof DataSource datasource ) {
			if ( customMapper ) {
				// The bootstrap data source need not belong to a tenant. In particular,
				// its JNDI suffix is not necessarily a tenant identifier.
				anyDataSource = datasource;
				return;
			}
			final int loc = jndiName.lastIndexOf( '/' );
			baseJndiNamespace = jndiName.substring( 0, loc );
			tenantDataSourceMapper = new DefaultTenantDataSourceMapper();
			final String prefix = jndiName.substring( loc + 1);
			tenantIdentifierForAny = (T) prefix;
			dataSourceMap().put( tenantIdentifierForAny, datasource );
		}
		else if ( namedObject instanceof Context ) {
			if ( !customMapper ) {
				baseJndiNamespace = jndiName;
				tenantDataSourceMapper = new DefaultTenantDataSourceMapper();
			}
			final Object configuredTenantId =
					configurationService.getSettings()
							.get( TENANT_IDENTIFIER_TO_USE_FOR_ANY_KEY );
			tenantIdentifierForAny = (T) configuredTenantId;
			if ( tenantIdentifierForAny == null ) {
				throw new HibernateException( "JNDI name named a Context, but tenant identifier to use for ANY was not specified" );
			}
		}
		else {
			throw new HibernateException(
					"Unknown object type [" + namedObject.getClass().getName() +
					"] found in JNDI location [" + this.jndiName + "]"
			);
		}
	}

	@Override
	public void stop() {
		dataSourceMap.clear();
		anyDataSource = null;
	}

	@Override
	public DatabaseConnectionInfo getDatabaseConnectionInfo(Dialect dialect) {
		return new DatabaseConnectionInfoImpl(
				null,
				null,
				null,
				dialect.getClass(),
				dialect.getVersion(),
				true,
				true,
				null,
				null,
				null,
				null,
				null,
				null,
				null
		) {
			@Override
			public String toInfoString() {
				return "\tMulti-tenant datasource JNDI name [" + jndiName + ']';
			}
		};
	}

	/**
	 * Selects the conventional tenant names relative to the provider's base JNDI context.
	 */
	private final class DefaultTenantDataSourceMapper implements TenantDataSourceMapper<T> {
		@Override
		public @Nonnull String dataSourceName(@Nonnull T tenantIdentifier) {
			return baseJndiNamespace + '/' + tenantIdentifier;
		}
	}
}
