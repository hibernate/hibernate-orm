/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.env.internal;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.model.source.internal.hbm.CommaSeparatedStringHelper;
import org.hibernate.engine.jdbc.cursor.internal.StandardRefCursorSupport;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.SQLStateType;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;

/**
 * Standard implementation of ExtractedDatabaseMetaData
 *
 * @author Steve Ebersole
 */
public class ExtractedDatabaseMetaDataImpl implements ExtractedDatabaseMetaData {
	private final JdbcEnvironment jdbcEnvironment;

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

	private final Set<String> extraKeywords;
	private final List<SequenceInformation> sequenceInformationList;

	private ExtractedDatabaseMetaDataImpl(
			JdbcEnvironment jdbcEnvironment,
			String connectionCatalogName,
			String connectionSchemaName,
			Set<String> extraKeywords,
			boolean supportsRefCursors,
			boolean supportsNamedParameters,
			boolean supportsScrollableResults,
			boolean supportsGetGeneratedKeys,
			boolean supportsBatchUpdates,
			boolean supportsDataDefinitionInTransaction,
			boolean doesDataDefinitionCauseTransactionCommit,
			SQLStateType sqlStateType,
			List<SequenceInformation> sequenceInformationList) {
		this.jdbcEnvironment = jdbcEnvironment;

		this.connectionCatalogName = connectionCatalogName;
		this.connectionSchemaName = connectionSchemaName;

		this.extraKeywords = extraKeywords != null
				? extraKeywords
				: Collections.<String>emptySet();

		this.supportsRefCursors = supportsRefCursors;
		this.supportsNamedParameters = supportsNamedParameters;
		this.supportsScrollableResults = supportsScrollableResults;
		this.supportsGetGeneratedKeys = supportsGetGeneratedKeys;
		this.supportsBatchUpdates = supportsBatchUpdates;
		this.supportsDataDefinitionInTransaction = supportsDataDefinitionInTransaction;
		this.doesDataDefinitionCauseTransactionCommit = doesDataDefinitionCauseTransactionCommit;
		this.sqlStateType = sqlStateType;
		this.sequenceInformationList = sequenceInformationList;
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
	public Set<String> getExtraKeywords() {
		return extraKeywords;
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
	public List<SequenceInformation> getSequenceInformationList() {
		return sequenceInformationList;
	}

	public static class Builder {
		private final JdbcEnvironment jdbcEnvironment;

		private String connectionSchemaName;
		private String connectionCatalogName;

		private Set<String> extraKeywords;

		private boolean supportsRefCursors;
		private boolean supportsNamedParameters;
		private boolean supportsScrollableResults;
		private boolean supportsGetGeneratedKeys;
		private boolean supportsBatchUpdates;
		private boolean supportsDataDefinitionInTransaction;
		private boolean doesDataDefinitionCauseTransactionCommit;
		private SQLStateType sqlStateType;
		private List<SequenceInformation> sequenceInformationList = Collections.emptyList();

		public Builder(JdbcEnvironment jdbcEnvironment) {
			this.jdbcEnvironment = jdbcEnvironment;
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
			extraKeywords = parseKeywords( databaseMetaData.getSQLKeywords() );
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

		public Builder setExtraKeywords(Set<String> extraKeywords) {
			if ( this.extraKeywords == null ) {
				this.extraKeywords = extraKeywords;
			}
			else {
				this.extraKeywords.addAll( extraKeywords );
			}
			return this;
		}

		public Builder addExtraKeyword(String keyword) {
			if ( this.extraKeywords == null ) {
				this.extraKeywords = new HashSet<String>();
			}
			this.extraKeywords.add( keyword );
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

		public Builder setSequenceInformationList(List<SequenceInformation> sequenceInformationList) {
			this.sequenceInformationList = sequenceInformationList;
			return this;
		}

		public ExtractedDatabaseMetaDataImpl build() {
			return new ExtractedDatabaseMetaDataImpl(
					jdbcEnvironment,
					connectionCatalogName,
					connectionSchemaName,
					extraKeywords,
					supportsRefCursors,
					supportsNamedParameters,
					supportsScrollableResults,
					supportsGetGeneratedKeys,
					supportsBatchUpdates,
					supportsDataDefinitionInTransaction,
					doesDataDefinitionCauseTransactionCommit,
					sqlStateType,
					sequenceInformationList
			);
		}
	}
}
