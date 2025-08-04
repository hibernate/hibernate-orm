/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.env.internal;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.cursor.internal.StandardRefCursorSupport;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.SQLStateType;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;

import static java.sql.ResultSet.TYPE_SCROLL_INSENSITIVE;
import static java.util.Collections.emptyList;
import static java.util.stream.StreamSupport.stream;
import static org.hibernate.engine.jdbc.JdbcLogging.JDBC_MESSAGE_LOGGER;
import static org.hibernate.engine.jdbc.env.spi.SQLStateType.interpretReportedSQLStateType;

/**
 * Standard implementation of {@link ExtractedDatabaseMetaData}
 *
 * @author Steve Ebersole
 */
public class ExtractedDatabaseMetaDataImpl implements ExtractedDatabaseMetaData {

	private final JdbcEnvironment jdbcEnvironment;
	private final JdbcConnectionAccess connectionAccess;

	private final String connectionCatalogName;
	private final String connectionSchemaName;

	private final String databaseProductName;
	private final String databaseProductVersion;

	private final boolean supportsRefCursors;
	private final boolean supportsNamedParameters;
	private final boolean supportsScrollableResults;
	private final boolean supportsGetGeneratedKeys;
	private final boolean supportsBatchUpdates;
	private final boolean supportsDataDefinitionInTransaction;
	private final boolean doesDataDefinitionCauseTransactionCommit;
	private final SQLStateType sqlStateType;
	private final int transactionIsolation;
	private final int defaultTransactionIsolation;
	private final String url;
	private final String driver;
	private final boolean jdbcMetadataAccessible;
	private final int defaultFetchSize;

	//Lazily initialized: loading all sequence information upfront has been
	//shown to be too slow in some cases. In this way we only load it
	//when there is an actual need for these details.
	private List<SequenceInformation> sequenceInformationList;

	ExtractedDatabaseMetaDataImpl(JdbcEnvironment environment) {
		jdbcEnvironment = environment;
		connectionAccess = null;
		jdbcMetadataAccessible = false;
		connectionSchemaName = null;
		connectionCatalogName = null;
		databaseProductName = null;
		databaseProductVersion = null;
		supportsRefCursors = false;
		supportsNamedParameters = false;
		supportsScrollableResults = false;
		supportsGetGeneratedKeys = false;
		supportsBatchUpdates = true;
		supportsDataDefinitionInTransaction = false;
		doesDataDefinitionCauseTransactionCommit = false;
		sqlStateType = null;
		url = null;
		driver = null;
		defaultTransactionIsolation = 0;
		transactionIsolation = 0;
		defaultFetchSize = -1;
	}

	ExtractedDatabaseMetaDataImpl(
			JdbcEnvironment environment,
			JdbcConnectionAccess connections,
			DatabaseMetaData metaData)
			throws SQLException {
		jdbcEnvironment = environment;
		connectionAccess = connections;
		jdbcMetadataAccessible = true;
		final Dialect dialect = environment.getDialect();
		final Connection connection = metaData.getConnection();
		connectionSchemaName = dialect.getSchemaNameResolver().resolveSchemaName( connection, dialect );
		connectionCatalogName = connection.getCatalog();
		databaseProductName = metaData.getDatabaseProductName();
		databaseProductVersion = metaData.getDatabaseProductVersion();
		supportsRefCursors = StandardRefCursorSupport.supportsRefCursors( metaData );
		supportsNamedParameters = dialect.supportsNamedParameters( metaData );
		supportsScrollableResults = metaData.supportsResultSetType( TYPE_SCROLL_INSENSITIVE );
		supportsGetGeneratedKeys = metaData.supportsGetGeneratedKeys();
		supportsBatchUpdates = metaData.supportsBatchUpdates();
		supportsDataDefinitionInTransaction = !metaData.dataDefinitionIgnoredInTransactions();
		doesDataDefinitionCauseTransactionCommit = metaData.dataDefinitionCausesTransactionCommit();
		sqlStateType = interpretReportedSQLStateType( metaData.getSQLStateType() );
		url = metaData.getURL();
		driver = metaData.getDriverName();
		defaultTransactionIsolation = metaData.getDefaultTransactionIsolation();
		transactionIsolation = connection.getTransactionIsolation();
		defaultFetchSize = defaultFetchSize( connection );
	}

	@Override
	public boolean supportsRefCursors() {
		return supportsRefCursors;
	}

	@Override
	public JdbcEnvironment getJdbcEnvironment() {
		return jdbcEnvironment;
	}

	@Override
	public boolean supportsNamedParameters() {
		return supportsNamedParameters;
	}

	@Override
	public boolean supportsScrollableResults() {
		return supportsScrollableResults;
	}

	@Override
	public boolean supportsGetGeneratedKeys() {
		return supportsGetGeneratedKeys;
	}

	@Override
	public boolean supportsBatchUpdates() {
		return supportsBatchUpdates;
	}

	@Override
	public boolean supportsDataDefinitionInTransaction() {
		return supportsDataDefinitionInTransaction;
	}

	@Override
	public boolean doesDataDefinitionCauseTransactionCommit() {
		return doesDataDefinitionCauseTransactionCommit;
	}

	@Override
	public SQLStateType getSqlStateType() {
		return sqlStateType;
	}

	@Override
	public String getConnectionCatalogName() {
		return connectionCatalogName;
	}

	@Override
	public String getConnectionSchemaName() {
		return connectionSchemaName;
	}

	@Override
	public String getDatabaseProductName() {
		return databaseProductName;
	}

	@Override
	public String getDatabaseProductVersion() {
		return databaseProductVersion;
	}

	@Override
	public String getUrl() {
		return url;
	}

	@Override
	public String getDriver() {
		return driver;
	}

	@Override
	public int getTransactionIsolation() {
		return transactionIsolation;
	}

	@Override
	public int getDefaultTransactionIsolation() {
		return defaultTransactionIsolation;
	}

	@Override
	public int getDefaultFetchSize() {
		return defaultFetchSize;
	}

	@Override
	public synchronized List<SequenceInformation> getSequenceInformationList() {
		if ( jdbcMetadataAccessible ) {
			//Loading the sequence information can take a while on large databases,
			//even minutes in some cases.
			//We trigger this lazily as only certain combinations of configurations,
			//mappings and used features actually trigger any use of such details.
			if ( sequenceInformationList == null ) {
				sequenceInformationList = sequenceInformationList();
			}
			return sequenceInformationList;
		}
		else {
			return emptyList();
		}
	}

	// For tests
	public boolean isJdbcMetadataAccessible() {
		return jdbcMetadataAccessible;
	}

	private static int defaultFetchSize(Connection connection) {
		try ( var statement = connection.createStatement() ) {
			return statement.getFetchSize();
		}
		catch (SQLException ignore) {
			return  -1;
		}
	}

	/**
	 * Get the sequence information List from the database.
	 *
	 * @return sequence information List
	 */
	private List<SequenceInformation> sequenceInformationList() {
		Connection connection = null;
		try {
			connection = connectionAccess.obtainConnection();
			return stream( sequenceInformation( connection, jdbcEnvironment ).spliterator(), false )
					.toList();
		}
		catch (SQLException e) {
			throw new HibernateException( "Could not fetch the SequenceInformation from the database", e );
		}
		finally {
			if ( connection != null ) {
				try {
					connectionAccess.releaseConnection( connection );
				}
				catch (SQLException exception) {
					JDBC_MESSAGE_LOGGER.unableToReleaseConnection( exception );
				}
			}
		}
	}

	private static Iterable<SequenceInformation> sequenceInformation(Connection connection, JdbcEnvironment jdbcEnvironment)
			throws SQLException {
		return jdbcEnvironment.getDialect().getSequenceInformationExtractor().extractMetadata(
				new ExtractionContext.EmptyExtractionContext() {
					@Override
					public Connection getJdbcConnection() {
						return connection;
					}
					@Override
					public JdbcEnvironment getJdbcEnvironment() {
						return jdbcEnvironment;
					}
				}
		);
	}
}
