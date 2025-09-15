/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.connections.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;

import org.hibernate.HibernateException;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProviderConfigurationException;
import org.hibernate.engine.jdbc.connections.spi.DatabaseConnectionInfo;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.InjectService;
import org.hibernate.service.spi.Stoppable;

import static org.hibernate.cfg.JdbcSettings.DATASOURCE;
import static org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator.toIsolationNiceName;
import static org.hibernate.internal.log.ConnectionInfoLogger.CONNECTION_INFO_LOGGER;

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
		implements ConnectionProvider, Configurable, Stoppable {

	private DataSource dataSource;
	private String user;
	private String pass;
	private boolean useCredentials;
	private JndiService jndiService;
	private String dataSourceJndiName;

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
	public boolean isUnwrappableAs(Class<?> unwrapType) {
		return unwrapType.isAssignableFrom( DataSourceConnectionProvider.class )
			|| unwrapType.isAssignableFrom( DataSource.class);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> unwrapType) {
		if ( unwrapType.isAssignableFrom( DataSourceConnectionProvider.class ) ) {
			return (T) this;
		}
		else if ( unwrapType.isAssignableFrom( DataSource.class) ) {
			return (T) getDataSource();
		}
		else {
			throw new UnknownUnwrapTypeException( unwrapType );
		}
	}

	@Override
	public void configure(Map<String, Object> configuration) {
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
				dataSource = (DataSource) jndiService.locate( jndiName );
			}
			else {
				throw new ConnectionProviderConfigurationException(
						"DataSource to use was not injected nor specified by '" + DATASOURCE + "'" );
			}
		}
		if ( dataSource == null ) {
			throw new ConnectionProviderConfigurationException( "Unable to determine appropriate DataSource to use" );
		}

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

	@Override
	public void stop() {
		available = false;
		dataSource = null;
	}

	@Override
	public Connection getConnection() throws SQLException {
		if ( !available ) {
			throw new HibernateException( "Provider is closed" );
		}
		return useCredentials ? dataSource.getConnection( user, pass ) : dataSource.getConnection();
	}

	@Override
	public void closeConnection(Connection connection) throws SQLException {
		connection.close();
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return true;
	}

	@Override
	public DatabaseConnectionInfo getDatabaseConnectionInfo(Dialect dialect) {
		return getDatabaseConnectionInfo( dialect, null );
	}

	@Override
	public DatabaseConnectionInfo getDatabaseConnectionInfo(Dialect dialect, ExtractedDatabaseMetaData metaData) {
		return new DatabaseConnectionInfoImpl(
				DataSourceConnectionProvider.class,
				metaData == null ? null : metaData.getUrl(),
				metaData == null ? null : metaData.getDriver(),
				dialect.getClass(),
				dialect.getVersion(),
				metaData == null || metaData.supportsSchemas(),
				metaData == null || metaData.supportsCatalogs(),
				metaData == null ? null : metaData.getConnectionSchemaName(),
				metaData == null ? null : metaData.getConnectionCatalogName(),
				null,
				metaData == null ? null : isolationString( metaData ),
				null,
				null,
				metaData != null ? fetchSize( metaData ) : null
		) {
			@Override
			public String toInfoString() {
				return dataSourceJndiName != null
						? "\tDataSource JNDI name [" + dataSourceJndiName + "]\n" + super.toInfoString()
						: super.toInfoString();
			}
		};
	}

	private static Integer fetchSize(ExtractedDatabaseMetaData metaData) {
		final int defaultFetchSize = metaData.getDefaultFetchSize();
		return defaultFetchSize == -1 ? null : defaultFetchSize;
	}

	private String isolationString(ExtractedDatabaseMetaData metaData) {
		return toIsolationNiceName( metaData.getTransactionIsolation() )
			+ " [default " + toIsolationNiceName( metaData.getDefaultTransactionIsolation() ) + "]";
	}
}
