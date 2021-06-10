/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.env.internal;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.source.internal.hbm.CommaSeparatedStringHelper;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.cursor.internal.StandardRefCursorSupport;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.SQLStateType;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;

/**
 * Standard implementation of ExtractedDatabaseMetaData
 *
 * @author Steve Ebersole
 */
public class ExtractedDatabaseMetaDataImpl implements ExtractedDatabaseMetaData {

	private final JdbcEnvironment jdbcEnvironment;
	private final JdbcConnectionAccess connectionAccess;

	private final String connectionCatalogName;
	private final String connectionSchemaName;

	private final boolean supportsRefCursors;
	private final boolean supportsNamedParameters;
	private final boolean supportsScrollableResults;
	private final boolean supportsGetGeneratedKeys;
	private final boolean supportsBatchUpdates;
	private final boolean supportsDataDefinitionInTransaction;
	private final boolean doesDataDefinitionCauseTransactionCommit;
	private final SQLStateType sqlStateType;
	private final boolean jdbcMetadataAccessible;

	//Lazily initialized: loading all sequence information upfront has been
	//shown to be too slow in some cases. In this way we only load it
	//when there is actual need for these details.
	private List<SequenceInformation> sequenceInformationList;

	private ExtractedDatabaseMetaDataImpl(
			JdbcEnvironment jdbcEnvironment,
			JdbcConnectionAccess connectionAccess,
			String connectionCatalogName,
			String connectionSchemaName,
			boolean supportsRefCursors,
			boolean supportsNamedParameters,
			boolean supportsScrollableResults,
			boolean supportsGetGeneratedKeys,
			boolean supportsBatchUpdates,
			boolean supportsDataDefinitionInTransaction,
			boolean doesDataDefinitionCauseTransactionCommit,
			SQLStateType sqlStateType,
			boolean jdbcMetadataIsAccessible) {
		this.jdbcEnvironment = jdbcEnvironment;
		this.connectionAccess = connectionAccess;
		this.connectionCatalogName = connectionCatalogName;
		this.connectionSchemaName = connectionSchemaName;
		this.supportsRefCursors = supportsRefCursors;
		this.supportsNamedParameters = supportsNamedParameters;
		this.supportsScrollableResults = supportsScrollableResults;
		this.supportsGetGeneratedKeys = supportsGetGeneratedKeys;
		this.supportsBatchUpdates = supportsBatchUpdates;
		this.supportsDataDefinitionInTransaction = supportsDataDefinitionInTransaction;
		this.doesDataDefinitionCauseTransactionCommit = doesDataDefinitionCauseTransactionCommit;
		this.sqlStateType = sqlStateType;
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
			return Collections.emptyList();
		}
	}

	public static class Builder {
		private final JdbcEnvironment jdbcEnvironment;
		private final boolean jdbcMetadataIsAccessible;
		private final JdbcConnectionAccess connectionAccess;

		private String connectionSchemaName;
		private String connectionCatalogName;

		private boolean supportsRefCursors;
		private boolean supportsNamedParameters;
		private boolean supportsScrollableResults;
		private boolean supportsGetGeneratedKeys;
		private boolean supportsBatchUpdates;
		private boolean supportsDataDefinitionInTransaction;
		private boolean doesDataDefinitionCauseTransactionCommit;
		private SQLStateType sqlStateType;

		public Builder(JdbcEnvironment jdbcEnvironment, boolean jdbcMetadataIsAccessible, JdbcConnectionAccess connectionAccess) {
			this.jdbcEnvironment = jdbcEnvironment;
			this.jdbcMetadataIsAccessible = jdbcMetadataIsAccessible;
			this.connectionAccess = connectionAccess;
		}

		public Builder apply(DatabaseMetaData databaseMetaData) throws SQLException {
			connectionCatalogName = databaseMetaData.getConnection().getCatalog();
			// NOTE : databaseMetaData.getConnection().getSchema() would require java 1.7 as baseline
			supportsRefCursors = StandardRefCursorSupport.supportsRefCursors( databaseMetaData );
			supportsNamedParameters = databaseMetaData.supportsNamedParameters();
			supportsScrollableResults = databaseMetaData.supportsResultSetType( ResultSet.TYPE_SCROLL_INSENSITIVE );
			supportsGetGeneratedKeys = databaseMetaData.supportsGetGeneratedKeys();
			supportsBatchUpdates = databaseMetaData.supportsBatchUpdates();
			supportsDataDefinitionInTransaction = !databaseMetaData.dataDefinitionIgnoredInTransactions();
			doesDataDefinitionCauseTransactionCommit = databaseMetaData.dataDefinitionCausesTransactionCommit();
			sqlStateType = SQLStateType.interpretReportedSQLStateType( databaseMetaData.getSQLStateType() );
			return this;
		}

		private Set<String> parseKeywords(String extraKeywordsString) {
			return CommaSeparatedStringHelper.split( extraKeywordsString );
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
					supportsRefCursors,
					supportsNamedParameters,
					supportsScrollableResults,
					supportsGetGeneratedKeys,
					supportsBatchUpdates,
					supportsDataDefinitionInTransaction,
					doesDataDefinitionCauseTransactionCommit,
					sqlStateType,
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
		final JdbcEnvironment jdbcEnvironment = this.jdbcEnvironment;
		final Dialect dialect = this.jdbcEnvironment.getDialect();

		Connection connection = null;
		try {
			connection = connectionAccess.obtainConnection();
			final Connection c = connection;
			Iterable<SequenceInformation> sequenceInformationIterable = dialect
					.getSequenceInformationExtractor()
					.extractMetadata( new ExtractionContext.EmptyExtractionContext() {
										@Override
										public Connection getJdbcConnection() {
											return c;
										}

										@Override
										public JdbcEnvironment getJdbcEnvironment() {
											return jdbcEnvironment;
										}
									}
					);
			return StreamSupport.stream( sequenceInformationIterable.spliterator(), false )
					.collect( Collectors.toList() );
		}
		catch (SQLException e) {
			throw new HibernateException( "Could not fetch the SequenceInformation from the database", e );
		}
		finally {
			if ( connection != null ) {
				try {
					connectionAccess.releaseConnection( connection );
				}
				catch (SQLException throwables) {
					//ignored
				}
			}
		}
	}

}
