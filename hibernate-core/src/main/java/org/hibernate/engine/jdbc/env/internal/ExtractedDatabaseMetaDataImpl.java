/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine.jdbc.env.internal;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.hibernate.engine.jdbc.cursor.internal.StandardRefCursorSupport;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.SQLStateType;
import org.hibernate.engine.jdbc.spi.TypeInfo;

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
	private final boolean lobLocatorUpdateCopy;

	private final Set<String> extraKeywords;
	private final LinkedHashSet<TypeInfo> typeInfoSet;

	private ExtractedDatabaseMetaDataImpl(
			JdbcEnvironment jdbcEnvironment,
			String connectionCatalogName,
			String connectionSchemaName,
			Set<String> extraKeywords,
			LinkedHashSet<TypeInfo> typeInfoSet,
			boolean supportsRefCursors,
			boolean supportsNamedParameters,
			boolean supportsScrollableResults,
			boolean supportsGetGeneratedKeys,
			boolean supportsBatchUpdates,
			boolean supportsDataDefinitionInTransaction,
			boolean doesDataDefinitionCauseTransactionCommit,
			SQLStateType sqlStateType,
			boolean lobLocatorUpdateCopy) {
		this.jdbcEnvironment = jdbcEnvironment;

		this.connectionCatalogName = connectionCatalogName;
		this.connectionSchemaName = connectionSchemaName;

		this.extraKeywords = extraKeywords != null
				? extraKeywords
				: Collections.<String>emptySet();
		this.typeInfoSet = typeInfoSet != null
				? typeInfoSet
				: new LinkedHashSet<TypeInfo>();

		this.supportsRefCursors = supportsRefCursors;
		this.supportsNamedParameters = supportsNamedParameters;
		this.supportsScrollableResults = supportsScrollableResults;
		this.supportsGetGeneratedKeys = supportsGetGeneratedKeys;
		this.supportsBatchUpdates = supportsBatchUpdates;
		this.supportsDataDefinitionInTransaction = supportsDataDefinitionInTransaction;
		this.doesDataDefinitionCauseTransactionCommit = doesDataDefinitionCauseTransactionCommit;
		this.sqlStateType = sqlStateType;
		this.lobLocatorUpdateCopy = lobLocatorUpdateCopy;
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
	public boolean doesLobLocatorUpdateCopy() {
		return lobLocatorUpdateCopy;
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
	public LinkedHashSet<TypeInfo> getTypeInfoSet() {
		return typeInfoSet;
	}

	public static class Builder {
		private final JdbcEnvironment jdbcEnvironment;

		private String connectionSchemaName;
		private String connectionCatalogName;

		private Set<String> extraKeywords;
		private LinkedHashSet<TypeInfo> typeInfoSet;

		private boolean supportsRefCursors;
		private boolean supportsNamedParameters;
		private boolean supportsScrollableResults;
		private boolean supportsGetGeneratedKeys;
		private boolean supportsBatchUpdates;
		private boolean supportsDataDefinitionInTransaction;
		private boolean doesDataDefinitionCauseTransactionCommit;
		private SQLStateType sqlStateType;
		private boolean lobLocatorUpdateCopy;

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
			lobLocatorUpdateCopy = databaseMetaData.locatorsUpdateCopy();
			typeInfoSet = new LinkedHashSet<TypeInfo>();
			typeInfoSet.addAll( TypeInfo.extractTypeInfo( databaseMetaData ) );

			return this;
		}

		private Set<String> parseKeywords(String extraKeywordsString) {
			final Set<String> keywordSet = new HashSet<String>();
			for ( String keyword : extraKeywordsString.split( "," ) ) {
				keywordSet.add( keyword.toUpperCase(Locale.ROOT) );
			}
			return keywordSet;
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

		public Builder setTypeInfoSet(LinkedHashSet<TypeInfo> typeInfoSet) {
			if ( this.typeInfoSet == null ) {
				this.typeInfoSet = typeInfoSet;
			}
			else {
				this.typeInfoSet.addAll( typeInfoSet );
			}
			return this;
		}

		public Builder addTypeInfo(TypeInfo typeInfo) {
			if ( this.typeInfoSet == null ) {
				this.typeInfoSet = new LinkedHashSet<TypeInfo>();
			}
			typeInfoSet.add( typeInfo );
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

		public Builder setLobLocatorUpdateCopy(boolean lobLocatorUpdateCopy) {
			this.lobLocatorUpdateCopy = lobLocatorUpdateCopy;
			return this;
		}

		public ExtractedDatabaseMetaDataImpl build() {
			return new ExtractedDatabaseMetaDataImpl(
					jdbcEnvironment,
					connectionCatalogName,
					connectionSchemaName,
					extraKeywords,
					typeInfoSet,
					supportsRefCursors,
					supportsNamedParameters,
					supportsScrollableResults,
					supportsGetGeneratedKeys,
					supportsBatchUpdates,
					supportsDataDefinitionInTransaction,
					doesDataDefinitionCauseTransactionCommit,
					sqlStateType,
					lobLocatorUpdateCopy
			);
		}
	}
}
