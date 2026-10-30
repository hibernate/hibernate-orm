package org.hibernate.engine.jdbc.connections.spi;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;

import jakarta.annotation.Nonnull;
import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.context.spi.MultiTenancy;
import org.hibernate.context.spi.TenantCredentialsMapper;
import org.hibernate.context.spi.TenantDataSourceMapper;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;

import static org.hibernate.cfg.JdbcSettings.PASS;
import static org.hibernate.cfg.JdbcSettings.USER;

/**
 * A concrete implementation of the {@link MultiTenantConnectionProvider} contract based
 * on a number of reasonable assumptions. A configured {@link TenantDataSourceMapper}
 * supplies full JNDI names for tenant data sources. Without a mapper, or with the
 * {@code default} strategy, we assume that:
 * <ul>
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
 * <p>
 * The configured {@link TenantCredentialsMapper}, if any, supplies tenant-specific credentials.
 * Otherwise, the default configured connection credentials are used for bootstrap connections
 * and as a fallback for tenant connections.
 *
 * @author Steve Ebersole
 *
 * @deprecated Configure a {@link TenantDataSourceMapper} with
 * {@link org.hibernate.engine.jdbc.connections.internal.DataSourceConnectionProvider} instead.
 */
@Deprecated(since = "8.0")
public class DataSourceBasedMultiTenantConnectionProviderImpl<T>
		extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl<T>
		implements ServiceRegistryAwareService, Stoppable {

	private final Map<T, DataSource> dataSourceMap = new ConcurrentHashMap<>();
	private JndiService jndiService;
	private String jndiName;
	private MultiTenancy multiTenancy;
	private String user;
	private String password;
	private DataSource anyDataSource;

	public DataSourceBasedMultiTenantConnectionProviderImpl() {
	}

	@Override
	public Connection getAnyConnection() throws SQLException {
		return getConnection( selectAnyDataSource() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Connection getConnection(T tenantIdentifier) throws SQLException {
		final var dataSource = selectDataSource( tenantIdentifier );
		final var tenantCredentialsMapper =
				(TenantCredentialsMapper<T>)
						multiTenancy.getTenantCredentialsMapper();
		return tenantCredentialsMapper == null
				? getConnection( dataSource )
				: dataSource.getConnection(
						tenantCredentialsMapper.user( tenantIdentifier ),
						tenantCredentialsMapper.password( tenantIdentifier ) );
	}

	private Connection getConnection(DataSource dataSource) throws SQLException {
		return user != null || password != null
				? dataSource.getConnection( user, password )
				: dataSource.getConnection();
	}

	@Override
	protected DataSource selectAnyDataSource() {
		return anyDataSource;
	}

	@Override
	protected DataSource selectDataSource(T tenantIdentifier) {
		DataSource dataSource = dataSourceMap().get( tenantIdentifier );
		if ( dataSource == null ) {
			final String tenantDataSourceJndiName = multiTenancy.getDataSourceJndiName( tenantIdentifier );
			if ( tenantDataSourceJndiName == null ) {
				throw new HibernateException( "No tenant data source mapper was configured" );
			}
			dataSource = locateDataSource( tenantDataSourceJndiName );
			dataSourceMap().put( tenantIdentifier, dataSource );
		}
		return dataSource;
	}

	private Map<T, DataSource> dataSourceMap() {
		return dataSourceMap;
	}

	@Override
	public void injectServices(@Nonnull ServiceRegistryImplementor serviceRegistry) {
		final var configurationService = serviceRegistry.requireService( ConfigurationService.class );
		final var settings = configurationService.getSettings();
		multiTenancy = serviceRegistry.requireService( MultiTenancy.class );
		user = (String) settings.get( USER );
		password = (String) settings.get( PASS );
		jndiService = serviceRegistry.getService( JndiService.class );
		if ( jndiService == null ) {
			throw new HibernateException(
					"Could not locate JndiService from DataSourceBasedMultiTenantConnectionProviderImpl" );
		}
		jndiName = multiTenancy.getBootstrapDataSourceJndiName();
		if ( jndiName == null ) {
			throw new HibernateException( "Could not determine the bootstrap DataSource JNDI name" );
		}
		anyDataSource = locateDataSource( jndiName );
	}

	private DataSource locateDataSource(String jndiName) {
		final Object namedObject = jndiService.locate( jndiName );
		if ( namedObject instanceof DataSource dataSource ) {
			return dataSource;
		}
		throw new HibernateException( "JNDI name [" + jndiName + "] does not refer to a DataSource" );
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
}
