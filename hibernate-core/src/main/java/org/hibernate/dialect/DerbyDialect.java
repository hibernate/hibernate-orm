/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.lang.reflect.Method;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;

import org.hibernate.MappingException;
import org.hibernate.dialect.function.AnsiTrimFunction;
import org.hibernate.dialect.function.DerbyConcatFunction;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.hql.spi.id.IdTableSupportStandardImpl;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.local.AfterUseAction;
import org.hibernate.hql.spi.id.local.LocalTemporaryTableBulkIdStrategy;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.DerbyCaseFragment;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorDerbyDatabaseImpl;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

import org.jboss.logging.Logger;

/**
 * Hibernate Dialect for Cloudscape 10 - aka Derby. This implements both an
 * override for the identity column generator as well as for the case statement
 * issue documented at:
 * http://www.jroller.com/comments/kenlars99/Weblog/cloudscape_soon_to_be_derby
 *
 * @author Simon Johnston
 *
 * @deprecated HHH-6073
 */
@Deprecated
public class DerbyDialect extends DB2Dialect {
	@SuppressWarnings("deprecation")
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			DerbyDialect.class.getName()
	);

	private int driverVersionMajor;
	private int driverVersionMinor;
	private final LimitHandler limitHandler;

	/**
	 * Constructs a DerbyDialect
	 */
	@SuppressWarnings("deprecation")
	public DerbyDialect() {
		super();
		if ( this.getClass() == DerbyDialect.class ) {
			LOG.deprecatedDerbyDialect();
		}

		registerFunction( "concat", new DerbyConcatFunction() );
		registerFunction( "trim", new AnsiTrimFunction() );
		registerColumnType( Types.BLOB, "blob" );
		registerDerbyKeywords();
		determineDriverVersion();

		if ( driverVersionMajor > 10 || ( driverVersionMajor == 10 && driverVersionMinor >= 7 ) ) {
			registerColumnType( Types.BOOLEAN, "boolean" );
		}

		this.limitHandler = new DerbyLimitHandler();
	}

	private void determineDriverVersion() {
		try {
			// locate the derby sysinfo class and query its version info
			final Class sysinfoClass = ReflectHelper.classForName( "org.apache.derby.tools.sysinfo", this.getClass() );
			final Method majorVersionGetter = sysinfoClass.getMethod( "getMajorVersion", ReflectHelper.NO_PARAM_SIGNATURE );
			final Method minorVersionGetter = sysinfoClass.getMethod( "getMinorVersion", ReflectHelper.NO_PARAM_SIGNATURE );
			driverVersionMajor = (Integer) majorVersionGetter.invoke( null, ReflectHelper.NO_PARAMS );
			driverVersionMinor = (Integer) minorVersionGetter.invoke( null, ReflectHelper.NO_PARAMS );
		}
		catch ( Exception e ) {
			LOG.unableToLoadDerbyDriver( e.getMessage() );
			driverVersionMajor = -1;
			driverVersionMinor = -1;
		}
	}

	private boolean isTenPointFiveReleaseOrNewer() {
		return driverVersionMajor > 10 || ( driverVersionMajor == 10 && driverVersionMinor >= 5 );
	}

	@Override
	public String getCrossJoinSeparator() {
		return ", ";
	}

	@Override
	public CaseFragment createCaseFragment() {
		return new DerbyCaseFragment();
	}

	@Override
	public boolean dropConstraints() {
		return true;
	}

	@Override
	public boolean supportsSequences() {
		// technically sequence support was added in 10.6.1.0...
		//
		// The problem though is that I am not exactly sure how to differentiate 10.6.1.0 from any other 10.6.x release.
		//
		// http://db.apache.org/derby/docs/10.0/publishedapi/org/apache/derby/tools/sysinfo.html seems incorrect.  It
		// states that derby's versioning scheme is major.minor.maintenance, but obviously 10.6.1.0 has 4 components
		// to it, not 3.
		//
		// Let alone the fact that it states that versions with the matching major.minor are 'feature
		// compatible' which is clearly not the case here (sequence support is a new feature...)
		return driverVersionMajor > 10 || ( driverVersionMajor == 10 && driverVersionMinor >= 6 );
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
		return isTenPointFiveReleaseOrNewer();
	}

	@Override
	public boolean supportsCommentOn() {
		//HHH-4531
		return false;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean supportsLimitOffset() {
		return isTenPointFiveReleaseOrNewer();
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

	private boolean hasForUpdateClause(int forUpdateIndex) {
		return forUpdateIndex >= 0;
	}

	private boolean hasWithClause(String normalizedSelect){
		return normalizedSelect.startsWith( "with ", normalizedSelect.length() - 7 );
	}

	private int getWithIndex(String querySelect) {
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

	private final class DerbyLimitHandler extends AbstractLimitHandler {
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
			return isTenPointFiveReleaseOrNewer();
		}

		@Override
		@SuppressWarnings("deprecation")
		public boolean supportsLimitOffset() {
			return isTenPointFiveReleaseOrNewer();
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

	protected void registerDerbyKeywords() {
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
	 * {@link DB2Dialect} returns a {@link org.hibernate.hql.spi.id.global.GlobalTemporaryTableBulkIdStrategy} that
	 * will make temporary tables created at startup and hence unavailable for subsequent connections.<br/>
	 * see HHH-10238.
	 * </p>
     */
	@Override
	public MultiTableBulkIdStrategy getDefaultMultiTableBulkIdStrategy() {
		return new LocalTemporaryTableBulkIdStrategy(new IdTableSupportStandardImpl() {
			@Override
			public String generateIdTableName(String baseName) {
				return "session." + super.generateIdTableName( baseName );
			}

			@Override
			public String getCreateIdTableCommand() {
				return "declare global temporary table";
			}

			@Override
			public String getCreateIdTableStatementOptions() {
				return "not logged";
			}
		}, AfterUseAction.CLEAN, null);
	}
}
