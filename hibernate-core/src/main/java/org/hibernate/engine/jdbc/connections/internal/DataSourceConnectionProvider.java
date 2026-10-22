package org.hibernate.engine.jdbc.connections.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.naming.Context;
import javax.sql.DataSource;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.HibernateException;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.context.spi.MultiTenancy;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProviderConfigurationException;
import org.hibernate.engine.jdbc.connections.spi.DatabaseConnectionInfo;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.InjectService;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;

import static org.hibernate.cfg.JdbcSettings.DATASOURCE;
import static org.hibernate.cfg.JdbcSettings.LOGIN_TIMEOUT;
import static org.hibernate.cfg.MultiTenancySettings.TENANT_IDENTIFIER_TO_USE_FOR_ANY_KEY;
import static org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator.toIsolationNiceName;
import static org.hibernate.internal.log.ConnectionInfoLogger.CONNECTION_INFO_LOGGER;
import static org.hibernate.internal.util.config.ConfigurationHelper.getInteger;

/**
 * A {@link ConnectionProvider} that manages connections from an underlying {@link DataSource}.
 * <p>
 * The {@link DataSource} to use may be specified by either:<ul>
 * <li>injection using {@link #setDataSource},
 * <li>passing the {@link DataSource} instance using {@value JdbcSettings#DATASOURCE},
 *     {@value JdbcSettings#JAKARTA_JTA_DATASOURCE}, or {@value JdbcSettings#JAKARTA_NON_JTA_DATASOURCE}, or
 * <li>declaring the JNDI name under which the {@link DataSource} is found via {@value JdbcSettings#DATASOURCE},
 *     {@value JdbcSettings#JAKARTA_JTA_DATASOURCE}, or {@value JdbcSettings#JAKARTA_NON_JTA_DATASOURCE}.
 * </ul>
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class DataSourceConnectionProvider
		implements ConnectionProvider, Configurable, Stoppable, ServiceRegistryAwareService {

	private final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();

	private DataSource dataSource;
	private String user;
	private String pass;
	private boolean useCredentials;
	private JndiService jndiService;
	private String dataSourceJndiName;
	private Integer loginTimeout;

	private transient ServiceRegistryImplementor serviceRegistry;

	private boolean available;

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@InjectService( required = false )
	@SuppressWarnings("unused")
	public void setJndiService(JndiService jndiService) {
		this.jndiService = jndiService;
	}

	@Override
	public void injectServices(@Nonnull ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public boolean isUnwrappableAs(@Nonnull Class<?> unwrapType) {
		return unwrapType.isAssignableFrom( DataSourceConnectionProvider.class )
			|| unwrapType.isAssignableFrom( DataSource.class);
	}

	@Override
	public <T> T unwrap(@Nonnull Class<T> unwrapType) {
		if ( unwrapType.isAssignableFrom( DataSourceConnectionProvider.class ) ) {
			return unwrapType.cast( this );
		}
		else if ( unwrapType.isAssignableFrom( DataSource.class) ) {
			return unwrapType.cast( getDataSource() );
		}
		else {
			throw new UnknownUnwrapTypeException( unwrapType );
		}
	}

	@Override
	public void configure(@Nonnull Map<String, Object> configuration) {
		loginTimeout = getInteger( LOGIN_TIMEOUT, configuration );
		if ( dataSource == null ) {
			final Object dataSourceSetting = configuration.get( DATASOURCE );
			if ( dataSourceSetting instanceof DataSource instance ) {
				dataSource = instance;
			}
			else if ( dataSourceSetting instanceof String jndiName ) {
				dataSourceJndiName = jndiName;
				if ( jndiService == null ) {
					throw new ConnectionProviderConfigurationException( "Unable to locate JndiService to lookup Datasource" );
				}
				final Object namedObject = jndiService.locate( jndiName );
				if ( namedObject instanceof DataSource instance ) {
					dataSource = instance;
					dataSources.put( jndiName, instance );
				}
				else if ( namedObject instanceof Context ) {
					dataSource = bootstrapDataSource( configuration );
				}
				else {
					throw new ConnectionProviderConfigurationException( "JNDI name does not refer to a DataSource or Context: " + jndiName );
				}
			}
			else if ( dataSourceSetting == null && serviceRegistry != null ) {
				dataSource = bootstrapDataSource( configuration );
			}
			else {
				throw new ConnectionProviderConfigurationException(
						"DataSource to use was not injected nor specified by '" + DATASOURCE + "'" );
			}
		}
		if ( dataSource == null ) {
			throw new ConnectionProviderConfigurationException( "Unable to determine appropriate DataSource to use" );
		}

		configureDataSource( dataSource );

		if ( configuration.containsKey( JdbcSettings.AUTOCOMMIT ) ) {
			CONNECTION_INFO_LOGGER.ignoredSetting( JdbcSettings.AUTOCOMMIT,
					DataSourceConnectionProvider.class );
		}
		if ( configuration.containsKey( JdbcSettings.ISOLATION ) ) {
			CONNECTION_INFO_LOGGER.ignoredSetting( JdbcSettings.ISOLATION,
					DataSourceConnectionProvider.class );
		}

		user = (String) configuration.get( JdbcSettings.USER );
		pass = (String) configuration.get( JdbcSettings.PASS );
		useCredentials = user != null || pass != null;
		available = true;
	}

	private DataSource bootstrapDataSource(Map<String, Object> configuration) {
		final var mapper = MultiTenancy.getTenantDataSourceMapper( configuration, serviceRegistry );
		final Object tenantIdentifier = configuration.get( TENANT_IDENTIFIER_TO_USE_FOR_ANY_KEY );
		if ( mapper == null || tenantIdentifier == null ) {
			throw new ConnectionProviderConfigurationException( "A tenant data source mapper and '"
					+ TENANT_IDENTIFIER_TO_USE_FOR_ANY_KEY + "' are required when no bootstrap DataSource is specified" );
		}
		dataSourceJndiName = mapper.dataSourceName( tenantIdentifier );
		return namedDataSource( dataSourceJndiName );
	}

	private DataSource namedDataSource(String name) {
		return dataSources.computeIfAbsent( name, jndiName -> {
			if ( jndiService == null ) {
				throw new ConnectionProviderConfigurationException( "Unable to locate JndiService to lookup Datasource" );
			}
			if ( !(jndiService.locate( jndiName ) instanceof DataSource source) ) {
				throw new ConnectionProviderConfigurationException( "JNDI name does not refer to a DataSource: " + jndiName );
			}
			configureDataSource( source );
			return source;
		} );
	}

	private void configureDataSource(DataSource dataSource) {
		if ( loginTimeout != null ) {
			try {
				dataSource.setLoginTimeout( loginTimeout );
			}
			catch (SQLException e) {
				CONNECTION_INFO_LOGGER.couldNotSetLoginTimeout( e );
			}
		}
	}

	@Override
	public void stop() {
		available = false;
		dataSource = null;
		dataSources.clear();
	}

	@Override
	@Nonnull
	public Connection getConnection() throws SQLException {
		if ( !available ) {
			throw new HibernateException( "Provider is closed" );
		}
		return useCredentials
				? dataSource.getConnection( user, pass )
				: dataSource.getConnection();
	}

	@Override
	@Nonnull
	public Connection getConnection(String user, String password) throws SQLException {
		if ( !available ) {
			throw new HibernateException( "Provider is closed" );
		}
		return dataSource.getConnection( user, password );
	}

	@Override
	@Nonnull
	public Connection getConnection(String dataSourceName) throws SQLException {
		if ( !available ) {
			throw new HibernateException( "Provider is closed" );
		}
		final var source = namedDataSource( dataSourceName );
		return useCredentials
				? source.getConnection( user, pass )
				: source.getConnection();
	}

	@Override
	@Nonnull
	public Connection getConnection(String dataSourceName, String user, String password) throws SQLException {
		if ( !available ) {
			throw new HibernateException( "Provider is closed" );
		}
		return namedDataSource( dataSourceName )
				.getConnection( user, password );
	}

	@Override
	public void closeConnection(@Nonnull Connection connection) throws SQLException {
		connection.close();
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return true;
	}

	@Override
	@Nonnull
	public DatabaseConnectionInfo getDatabaseConnectionInfo(@Nonnull Dialect dialect) {
		return new DatabaseConnectionInfoImpl(
				DataSourceConnectionProvider.class,
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
				return dataSourceJndiName != null
						? "\tDataSource JNDI name [" + dataSourceJndiName + "]\n" + super.toInfoString()
						: super.toInfoString();
			}
		};
	}

	@Override
	@Nonnull
	public DatabaseConnectionInfo getDatabaseConnectionInfo(@Nonnull Dialect dialect, @Nonnull ExtractedDatabaseMetaData metaData) {
		return new DatabaseConnectionInfoImpl(
				DataSourceConnectionProvider.class,
				metaData.getUrl(),
				metaData.getDriver(),
				dialect.getClass(),
				dialect.getVersion(),
				metaData.supportsSchemas(),
				metaData.supportsCatalogs(),
				metaData.getConnectionSchemaName(),
				metaData.getConnectionCatalogName(),
				null,
				isolationString( metaData ),
				null,
				null,
				fetchSize( metaData )
		) {
			@Override
			public String toInfoString() {
				return dataSourceJndiName != null
						? "\tDataSource JNDI name [" + dataSourceJndiName + "]\n" + super.toInfoString()
						: super.toInfoString();
			}
		};
	}

	@Nullable
	private static Integer fetchSize(@Nonnull ExtractedDatabaseMetaData metaData) {
		final int defaultFetchSize = metaData.getDefaultFetchSize();
		return defaultFetchSize == -1 ? null : defaultFetchSize;
	}

	@Nonnull
	private String isolationString(@Nonnull ExtractedDatabaseMetaData metaData) {
		return toIsolationNiceName( metaData.getTransactionIsolation() )
			+ " [default " + toIsolationNiceName( metaData.getDefaultTransactionIsolation() ) + "]";
	}
}
