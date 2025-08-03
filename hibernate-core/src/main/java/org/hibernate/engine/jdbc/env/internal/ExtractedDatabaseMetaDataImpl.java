/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.env.internal;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.cursor.internal.StandardRefCursorSupport;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.SQLStateType;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;

import static java.util.Collections.emptyList;
import static java.util.stream.StreamSupport.stream;
import static org.hibernate.engine.jdbc.JdbcLogging.JDBC_MESSAGE_LOGGER;

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
	//when there is actual need for these details.
	private List<SequenceInformation> sequenceInformationList;

	private ExtractedDatabaseMetaDataImpl(
			JdbcEnvironment jdbcEnvironment,
			JdbcConnectionAccess connectionAccess,
			String connectionCatalogName,
			String connectionSchemaName,
			String databaseProductName,
			String databaseProductVersion,
			boolean supportsRefCursors,
			boolean supportsNamedParameters,
			boolean supportsScrollableResults,
			boolean supportsGetGeneratedKeys,
			boolean supportsBatchUpdates,
			boolean supportsDataDefinitionInTransaction,
			boolean doesDataDefinitionCauseTransactionCommit,
			SQLStateType sqlStateType,
			int transactionIsolation,
			int defaultTransactionIsolation,
			int defaultFetchSize,
			String url,
			String driver,
			boolean jdbcMetadataIsAccessible) {
		this.jdbcEnvironment = jdbcEnvironment;
		this.connectionAccess = connectionAccess;
		this.connectionCatalogName = connectionCatalogName;
		this.connectionSchemaName = connectionSchemaName;
		this.databaseProductName = databaseProductName;
		this.databaseProductVersion = databaseProductVersion;
		this.supportsRefCursors = supportsRefCursors;
		this.supportsNamedParameters = supportsNamedParameters;
		this.supportsScrollableResults = supportsScrollableResults;
		this.supportsGetGeneratedKeys = supportsGetGeneratedKeys;
		this.supportsBatchUpdates = supportsBatchUpdates;
		this.supportsDataDefinitionInTransaction = supportsDataDefinitionInTransaction;
		this.doesDataDefinitionCauseTransactionCommit = doesDataDefinitionCauseTransactionCommit;
		this.sqlStateType = sqlStateType;
		this.transactionIsolation = transactionIsolation;
		this.defaultTransactionIsolation = defaultTransactionIsolation;
		this.defaultFetchSize = defaultFetchSize;
		this.url = url;
		this.driver = driver;
		this.jdbcMetadataAccessible = jdbcMetadataIsAccessible;
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

	public static class Builder {
		private final JdbcEnvironment jdbcEnvironment;
		private final boolean jdbcMetadataIsAccessible;
		private final JdbcConnectionAccess connectionAccess;

		private String connectionSchemaName;
		private String connectionCatalogName;

		private String databaseProductName;
		private String databaseProductVersion;

		private boolean supportsRefCursors;
		private boolean supportsNamedParameters;
		private boolean supportsScrollableResults;
		private boolean supportsGetGeneratedKeys;
		// In absence of DatabaseMetaData batching updates is assumed to be supported
		private boolean supportsBatchUpdates = true;
		private boolean supportsDataDefinitionInTransaction;
		private boolean doesDataDefinitionCauseTransactionCommit;
		private SQLStateType sqlStateType;
		private String url;
		private String driver;
		private int defaultTransactionIsolation;
		private int transactionIsolation;
		private int defaultFetchSize;

		public Builder(JdbcEnvironment jdbcEnvironment, boolean jdbcMetadataIsAccessible, JdbcConnectionAccess connectionAccess) {
			this.jdbcEnvironment = jdbcEnvironment;
			this.jdbcMetadataIsAccessible = jdbcMetadataIsAccessible;
			this.connectionAccess = connectionAccess;
		}

		public Builder apply(DatabaseMetaData databaseMetaData) throws SQLException {
			connectionCatalogName = databaseMetaData.getConnection().getCatalog();
			connectionSchemaName = databaseMetaData.getConnection().getSchema();
			databaseProductName = databaseMetaData.getDatabaseProductName();
			databaseProductVersion = databaseMetaData.getDatabaseProductVersion();
			supportsRefCursors = StandardRefCursorSupport.supportsRefCursors( databaseMetaData );
			supportsNamedParameters = databaseMetaData.supportsNamedParameters();
			supportsScrollableResults = databaseMetaData.supportsResultSetType( ResultSet.TYPE_SCROLL_INSENSITIVE );
			supportsGetGeneratedKeys = databaseMetaData.supportsGetGeneratedKeys();
			supportsBatchUpdates = databaseMetaData.supportsBatchUpdates();
			supportsDataDefinitionInTransaction = !databaseMetaData.dataDefinitionIgnoredInTransactions();
			doesDataDefinitionCauseTransactionCommit = databaseMetaData.dataDefinitionCausesTransactionCommit();
			sqlStateType = SQLStateType.interpretReportedSQLStateType( databaseMetaData.getSQLStateType() );
			url = databaseMetaData.getURL();
			driver = databaseMetaData.getDriverName();
			defaultTransactionIsolation = databaseMetaData.getDefaultTransactionIsolation();
			transactionIsolation = databaseMetaData.getConnection().getTransactionIsolation();
			try ( Statement statement = databaseMetaData.getConnection().createStatement() ) {
				defaultFetchSize = statement.getFetchSize();
			}
			catch (SQLException sqle) {
				defaultFetchSize = -1;
			}
			return this;
		}

		public Builder setConnectionSchemaName(String connectionSchemaName) {
			this.connectionSchemaName = connectionSchemaName;
			return this;
		}

		public Builder setConnectionCatalogName(String connectionCatalogName) {
			this.connectionCatalogName = connectionCatalogName;
			return this;
		}

		public Builder setSupportsRefCursors(boolean supportsRefCursors) {
			this.supportsRefCursors = supportsRefCursors;
			return this;
		}

		public Builder setSupportsNamedParameters(boolean supportsNamedParameters) {
			this.supportsNamedParameters = supportsNamedParameters;
			return this;
		}

		public Builder setSupportsScrollableResults(boolean supportsScrollableResults) {
			this.supportsScrollableResults = supportsScrollableResults;
			return this;
		}

		public Builder setSupportsGetGeneratedKeys(boolean supportsGetGeneratedKeys) {
			this.supportsGetGeneratedKeys = supportsGetGeneratedKeys;
			return this;
		}

		public Builder setSupportsBatchUpdates(boolean supportsBatchUpdates) {
			this.supportsBatchUpdates = supportsBatchUpdates;
			return this;
		}

		public Builder setSupportsDataDefinitionInTransaction(boolean supportsDataDefinitionInTransaction) {
			this.supportsDataDefinitionInTransaction = supportsDataDefinitionInTransaction;
			return this;
		}

		public Builder setDoesDataDefinitionCauseTransactionCommit(boolean doesDataDefinitionCauseTransactionCommit) {
			this.doesDataDefinitionCauseTransactionCommit = doesDataDefinitionCauseTransactionCommit;
			return this;
		}

		public Builder setSqlStateType(SQLStateType sqlStateType) {
			this.sqlStateType = sqlStateType;
			return this;
		}

		public ExtractedDatabaseMetaDataImpl build() {
			return new ExtractedDatabaseMetaDataImpl(
					jdbcEnvironment,
					connectionAccess,
					connectionCatalogName,
					connectionSchemaName,
					databaseProductName,
					databaseProductVersion,
					supportsRefCursors,
					supportsNamedParameters,
					supportsScrollableResults,
					supportsGetGeneratedKeys,
					supportsBatchUpdates,
					supportsDataDefinitionInTransaction,
					doesDataDefinitionCauseTransactionCommit,
					sqlStateType,
					transactionIsolation,
					defaultTransactionIsolation,
					defaultFetchSize,
					url,
					driver,
					jdbcMetadataIsAccessible
			);
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
