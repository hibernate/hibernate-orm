/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Locale;

import org.hibernate.MappingException;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.mutation.spi.SqmMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.idtable.AfterUseAction;
import org.hibernate.query.sqm.mutation.spi.idtable.LocalTemporaryTableStrategy;
import org.hibernate.dialect.function.DerbyConcatEmulation;
import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.DerbyCaseFragment;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorDerbyDatabaseImpl;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

/**
 * Hibernate Dialect for Apache Derby / Cloudscape 10
 *
 * @author Simon Johnston
 *
 */
public class DerbyDialect extends DB2Dialect {

	private final int version;

	int getDerbyVersion() {
		return version;
	}

	private final LimitHandler limitHandler = new DerbyLimitHandler();

	public DerbyDialect(DialectResolutionInfo info) {
		this( info.getDatabaseMajorVersion() * 100 + info.getDatabaseMinorVersion() * 10 );
	}

	public DerbyDialect() {
		this(1000);
	}

	public DerbyDialect(int version) {
		super();
		this.version = version;

		registerDerbyKeywords();
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );
		queryEngine.getSqmFunctionRegistry().register( "concat", new DerbyConcatEmulation() );
	}

	@Override
	public String toBooleanValueString(boolean bool) {
		return getDerbyVersion() < 1070
				? super.toBooleanValueString( bool )
				: String.valueOf( bool );
	}

	@Override
	public String getCrossJoinSeparator() {
		return ", ";
	}

	@Override
	@SuppressWarnings("deprecation")
	public CaseFragment createCaseFragment() {
		return new DerbyCaseFragment();
	}

	@Override
	public boolean dropConstraints() {
		return true;
	}

	@Override
	public boolean supportsSequences() {
		return getDerbyVersion() >= 1060;
	}

	@Override
	public String getQuerySequencesString() {
		if ( supportsSequences() ) {
			return "select sys.sysschemas.schemaname as sequence_schema, sys.syssequences.* from sys.syssequences left join sys.sysschemas on sys.syssequences.schemaid = sys.sysschemas.schemaid";
		}
		else {
			return null;
		}
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		if ( getQuerySequencesString() == null ) {
			return SequenceInformationExtractorNoOpImpl.INSTANCE;
		}
		else {
			return SequenceInformationExtractorDerbyDatabaseImpl.INSTANCE;
		}
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		if ( supportsSequences() ) {
			return "values next value for " + sequenceName;
		}
		else {
			throw new MappingException( "Derby does not support sequence prior to release 10.6.1.0" );
		}
	}

	@Override
	public boolean supportsLimit() {
		return getDerbyVersion() >= 1050;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean supportsLimitOffset() {
		return getDerbyVersion() >= 1050;
	}

	@Override
	public String getFromDual() {
		return "from sysibm.sysdummy1";
	}

	@Override
	public boolean supportsCommentOn() {
		//HHH-4531
		return false;
	}

	@Override
	public String getForUpdateString() {
		return " for update with rs";
	}

	@Override
	public String getWriteLockString(int timeout) {
		return " for update with rs";
	}

	@Override
	public String getReadLockString(int timeout) {
		return " for read only with rs";
	}


	@Override
	public LimitHandler getLimitHandler() {
		return limitHandler;
	}

	@Override
	public boolean supportsTuplesInSubqueries() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * From Derby 10.5 Docs:
	 * <pre>
	 * Query
	 * [ORDER BY clause]
	 * [result offset clause]
	 * [fetch first clause]
	 * [FOR UPDATE clause]
	 * [WITH {RR|RS|CS|UR}]
	 * </pre>
	 */
	@Override
	public String getLimitString(String query, final int offset, final int limit) {
		final StringBuilder sb = new StringBuilder(query.length() + 50);
		final String normalizedSelect = query.toLowerCase(Locale.ROOT).trim();
		final int forUpdateIndex = normalizedSelect.lastIndexOf( "for update") ;

		if ( hasForUpdateClause( forUpdateIndex ) ) {
			sb.append( query.substring( 0, forUpdateIndex-1 ) );
		}
		else if ( hasWithClause( normalizedSelect ) ) {
			sb.append( query.substring( 0, getWithIndex( query ) - 1 ) );
		}
		else {
			sb.append( query );
		}

		if ( offset == 0 ) {
			sb.append( " fetch first " );
		}
		else {
			sb.append( " offset " ).append( offset ).append( " rows fetch next " );
		}

		sb.append( limit ).append( " rows only" );

		if ( hasForUpdateClause( forUpdateIndex ) ) {
			sb.append( ' ' );
			sb.append( query.substring( forUpdateIndex ) );
		}
		else if ( hasWithClause( normalizedSelect ) ) {
			sb.append( ' ' ).append( query.substring( getWithIndex( query ) ) );
		}
		return sb.toString();
	}

	@Override
	public boolean supportsVariableLimit() {
		// we bind the limit and offset values directly into the sql...
		return false;
	}

	private static boolean hasForUpdateClause(int forUpdateIndex) {
		return forUpdateIndex >= 0;
	}

	private static boolean hasWithClause(String normalizedSelect){
		return normalizedSelect.startsWith( "with ", normalizedSelect.length() - 7 );
	}

	private static int getWithIndex(String querySelect) {
		int i = querySelect.lastIndexOf( "with " );
		if ( i < 0 ) {
			i = querySelect.lastIndexOf( "WITH " );
		}
		return i;
	}


	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsLobValueChangePropogation() {
		return false;
	}

	@Override
	public boolean supportsUnboundedLobLocatorMaterialization() {
		return false;
	}

	private class DerbyLimitHandler extends AbstractLimitHandler {
		/**
		 * {@inheritDoc}
		 * <p/>
		 * From Derby 10.5 Docs:
		 * <pre>
		 * Query
		 * [ORDER BY clause]
		 * [result offset clause]
		 * [fetch first clause]
		 * [FOR UPDATE clause]
		 * [WITH {RR|RS|CS|UR}]
		 * </pre>
		 */
		@Override
		public String processSql(String sql, RowSelection selection) {
			final StringBuilder sb = new StringBuilder( sql.length() + 50 );
			final String normalizedSelect = sql.toLowerCase(Locale.ROOT).trim();
			final int forUpdateIndex = normalizedSelect.lastIndexOf( "for update" );

			if (hasForUpdateClause( forUpdateIndex )) {
				sb.append( sql.substring( 0, forUpdateIndex - 1 ) );
			}
			else if (hasWithClause( normalizedSelect )) {
				sb.append( sql.substring( 0, getWithIndex( sql ) - 1 ) );
			}
			else {
				sb.append( sql );
			}

			if (LimitHelper.hasFirstRow( selection )) {
				sb.append( " offset " ).append( selection.getFirstRow() ).append( " rows fetch next " );
			}
			else {
				sb.append( " fetch first " );
			}

			sb.append( getMaxOrLimit( selection ) ).append(" rows only" );

			if (hasForUpdateClause( forUpdateIndex )) {
				sb.append( ' ' );
				sb.append( sql.substring( forUpdateIndex ) );
			}
			else if (hasWithClause( normalizedSelect )) {
				sb.append( ' ' ).append( sql.substring( getWithIndex( sql ) ) );
			}
			return sb.toString();
		}

		@Override
		public boolean supportsLimit() {
			return getDerbyVersion() >= 1050;
		}

		@Override
		@SuppressWarnings("deprecation")
		public boolean supportsLimitOffset() {
			return getDerbyVersion() >= 1050;
		}

		@Override
		public boolean supportsVariableLimit() {
			return false;
		}
	}

	@Override
	public IdentifierHelper buildIdentifierHelper(
			IdentifierHelperBuilder builder, DatabaseMetaData dbMetaData) throws SQLException {
		builder.applyIdentifierCasing( dbMetaData );

		builder.applyReservedWords( dbMetaData );

		builder.applyReservedWords( getKeywords() );

		builder.setNameQualifierSupport( getNameQualifierSupport() );

		return builder.build();
	}

	private void registerDerbyKeywords() {
		registerKeyword( "ADD" );
		registerKeyword( "ALL" );
		registerKeyword( "ALLOCATE" );
		registerKeyword( "ALTER" );
		registerKeyword( "AND" );
		registerKeyword( "ANY" );
		registerKeyword( "ARE" );
		registerKeyword( "AS" );
		registerKeyword( "ASC" );
		registerKeyword( "ASSERTION" );
		registerKeyword( "AT" );
		registerKeyword( "AUTHORIZATION" );
		registerKeyword( "AVG" );
		registerKeyword( "BEGIN" );
		registerKeyword( "BETWEEN" );
		registerKeyword( "BIT" );
		registerKeyword( "BOOLEAN" );
		registerKeyword( "BOTH" );
		registerKeyword( "BY" );
		registerKeyword( "CALL" );
		registerKeyword( "CASCADE" );
		registerKeyword( "CASCADED" );
		registerKeyword( "CASE" );
		registerKeyword( "CAST" );
		registerKeyword( "CHAR" );
		registerKeyword( "CHARACTER" );
		registerKeyword( "CHECK" );
		registerKeyword( "CLOSE" );
		registerKeyword( "COLLATE" );
		registerKeyword( "COLLATION" );
		registerKeyword( "COLUMN" );
		registerKeyword( "COMMIT" );
		registerKeyword( "CONNECT" );
		registerKeyword( "CONNECTION" );
		registerKeyword( "CONSTRAINT" );
		registerKeyword( "CONSTRAINTS" );
		registerKeyword( "CONTINUE" );
		registerKeyword( "CONVERT" );
		registerKeyword( "CORRESPONDING" );
		registerKeyword( "COUNT" );
		registerKeyword( "CREATE" );
		registerKeyword( "CURRENT" );
		registerKeyword( "CURRENT_DATE" );
		registerKeyword( "CURRENT_TIME" );
		registerKeyword( "CURRENT_TIMESTAMP" );
		registerKeyword( "CURRENT_USER" );
		registerKeyword( "CURSOR" );
		registerKeyword( "DEALLOCATE" );
		registerKeyword( "DEC" );
		registerKeyword( "DECIMAL" );
		registerKeyword( "DECLARE" );
		registerKeyword( "DEFERRABLE" );
		registerKeyword( "DEFERRED" );
		registerKeyword( "DELETE" );
		registerKeyword( "DESC" );
		registerKeyword( "DESCRIBE" );
		registerKeyword( "DIAGNOSTICS" );
		registerKeyword( "DISCONNECT" );
		registerKeyword( "DISTINCT" );
		registerKeyword( "DOUBLE" );
		registerKeyword( "DROP" );
		registerKeyword( "ELSE" );
		registerKeyword( "END" );
		registerKeyword( "ENDEXEC" );
		registerKeyword( "ESCAPE" );
		registerKeyword( "EXCEPT" );
		registerKeyword( "EXCEPTION" );
		registerKeyword( "EXEC" );
		registerKeyword( "EXECUTE" );
		registerKeyword( "EXISTS" );
		registerKeyword( "EXPLAIN" );
		registerKeyword( "EXTERNAL" );
		registerKeyword( "FALSE" );
		registerKeyword( "FETCH" );
		registerKeyword( "FIRST" );
		registerKeyword( "FLOAT" );
		registerKeyword( "FOR" );
		registerKeyword( "FOREIGN" );
		registerKeyword( "FOUND" );
		registerKeyword( "FROM" );
		registerKeyword( "FULL" );
		registerKeyword( "FUNCTION" );
		registerKeyword( "GET" );
		registerKeyword( "GET_CURRENT_CONNECTION" );
		registerKeyword( "GLOBAL" );
		registerKeyword( "GO" );
		registerKeyword( "GOTO" );
		registerKeyword( "GRANT" );
		registerKeyword( "GROUP" );
		registerKeyword( "HAVING" );
		registerKeyword( "HOUR" );
		registerKeyword( "IDENTITY" );
		registerKeyword( "IMMEDIATE" );
		registerKeyword( "IN" );
		registerKeyword( "INDICATOR" );
		registerKeyword( "INITIALLY" );
		registerKeyword( "INNER" );
		registerKeyword( "INOUT" );
		registerKeyword( "INPUT" );
		registerKeyword( "INSENSITIVE" );
		registerKeyword( "INSERT" );
		registerKeyword( "INT" );
		registerKeyword( "INTEGER" );
		registerKeyword( "INTERSECT" );
		registerKeyword( "INTO" );
		registerKeyword( "IS" );
		registerKeyword( "ISOLATION" );
		registerKeyword( "JOIN" );
		registerKeyword( "KEY" );
		registerKeyword( "LAST" );
		registerKeyword( "LEFT" );
		registerKeyword( "LIKE" );
		registerKeyword( "LONGINT" );
		registerKeyword( "LOWER" );
		registerKeyword( "LTRIM" );
		registerKeyword( "MATCH" );
		registerKeyword( "MAX" );
		registerKeyword( "MIN" );
		registerKeyword( "MINUTE" );
		registerKeyword( "NATIONAL" );
		registerKeyword( "NATURAL" );
		registerKeyword( "NCHAR" );
		registerKeyword( "NVARCHAR" );
		registerKeyword( "NEXT" );
		registerKeyword( "NO" );
		registerKeyword( "NOT" );
		registerKeyword( "NULL" );
		registerKeyword( "NULLIF" );
		registerKeyword( "NUMERIC" );
		registerKeyword( "OF" );
		registerKeyword( "ON" );
		registerKeyword( "ONLY" );
		registerKeyword( "OPEN" );
		registerKeyword( "OPTION" );
		registerKeyword( "OR" );
		registerKeyword( "ORDER" );
		registerKeyword( "OUT" );
		registerKeyword( "OUTER" );
		registerKeyword( "OUTPUT" );
		registerKeyword( "OVERLAPS" );
		registerKeyword( "PAD" );
		registerKeyword( "PARTIAL" );
		registerKeyword( "PREPARE" );
		registerKeyword( "PRESERVE" );
		registerKeyword( "PRIMARY" );
		registerKeyword( "PRIOR" );
		registerKeyword( "PRIVILEGES" );
		registerKeyword( "PROCEDURE" );
		registerKeyword( "PUBLIC" );
		registerKeyword( "READ" );
		registerKeyword( "REAL" );
		registerKeyword( "REFERENCES" );
		registerKeyword( "RELATIVE" );
		registerKeyword( "RESTRICT" );
		registerKeyword( "REVOKE" );
		registerKeyword( "RIGHT" );
		registerKeyword( "ROLLBACK" );
		registerKeyword( "ROWS" );
		registerKeyword( "RTRIM" );
		registerKeyword( "SCHEMA" );
		registerKeyword( "SCROLL" );
		registerKeyword( "SECOND" );
		registerKeyword( "SELECT" );
		registerKeyword( "SESSION_USER" );
		registerKeyword( "SET" );
		registerKeyword( "SMALLINT" );
		registerKeyword( "SOME" );
		registerKeyword( "SPACE" );
		registerKeyword( "SQL" );
		registerKeyword( "SQLCODE" );
		registerKeyword( "SQLERROR" );
		registerKeyword( "SQLSTATE" );
		registerKeyword( "SUBSTR" );
		registerKeyword( "SUBSTRING" );
		registerKeyword( "SUM" );
		registerKeyword( "SYSTEM_USER" );
		registerKeyword( "TABLE" );
		registerKeyword( "TEMPORARY" );
		registerKeyword( "TIMEZONE_HOUR" );
		registerKeyword( "TIMEZONE_MINUTE" );
		registerKeyword( "TO" );
		registerKeyword( "TRAILING" );
		registerKeyword( "TRANSACTION" );
		registerKeyword( "TRANSLATE" );
		registerKeyword( "TRANSLATION" );
		registerKeyword( "TRUE" );
		registerKeyword( "UNION" );
		registerKeyword( "UNIQUE" );
		registerKeyword( "UNKNOWN" );
		registerKeyword( "UPDATE" );
		registerKeyword( "UPPER" );
		registerKeyword( "USER" );
		registerKeyword( "USING" );
		registerKeyword( "VALUES" );
		registerKeyword( "VARCHAR" );
		registerKeyword( "VARYING" );
		registerKeyword( "VIEW" );
		registerKeyword( "WHENEVER" );
		registerKeyword( "WHERE" );
		registerKeyword( "WITH" );
		registerKeyword( "WORK" );
		registerKeyword( "WRITE" );
		registerKeyword( "XML" );
		registerKeyword( "XMLEXISTS" );
		registerKeyword( "XMLPARSE" );
		registerKeyword( "XMLSERIALIZE" );
		registerKeyword( "YEAR" );
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * From Derby docs:
	 * <pre>
	 *     The DECLARE GLOBAL TEMPORARY TABLE statement defines a temporary table for the current connection.
	 * </pre>
	 *
	 * Here we return a local-temp-table strategy even though we are creating global temp tables.
	 * See HHH-10238 for details why...
	 * </p>
     */
	@Override
	public SqmMutationStrategy getDefaultIdTableStrategy() {
		return new LocalTemporaryTableStrategy( generateIdTableSupport() ) {
			@Override
			public AfterUseAction getAfterUseAction() {
				return AfterUseAction.CLEAN;
			}
		};
	}
}
