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
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.dialect.function.AnsiTrimFunction;
import org.hibernate.dialect.function.DerbyConcatFunction;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.jdbc.env.spi.AnsiSqlKeywords;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.DerbyCaseFragment;

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
			return "select SEQUENCENAME from SYS.SYSSEQUENCES";
		}
		else {
			throw new MappingException( "Derby does not support sequence prior to release 10.6.1.0" );
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
		builder.applyReservedWords( getDerbyKeywords() );
		builder.applyReservedWords( getKeywords() );

		builder.setNameQualifierSupport( getNameQualifierSupport() );

		return builder.build();
	}

	protected Set<String> getDerbyKeywords() {
		final Set<String> derbyKeyword = new HashSet<String>(  );
		derbyKeyword.add( "ADD" );
		derbyKeyword.add( "ALL" );
		derbyKeyword.add( "ALLOCATE" );
		derbyKeyword.add( "ALTER" );
		derbyKeyword.add( "AND" );
		derbyKeyword.add( "ANY" );
		derbyKeyword.add( "ARE" );
		derbyKeyword.add( "AS" );
		derbyKeyword.add( "ASC" );
		derbyKeyword.add( "ASSERTION" );
		derbyKeyword.add( "AT" );
		derbyKeyword.add( "AUTHORIZATION" );
		derbyKeyword.add( "AVG" );
		derbyKeyword.add( "BEGIN" );
		derbyKeyword.add( "BETWEEN" );
		derbyKeyword.add( "BIT" );
		derbyKeyword.add( "BOOLEAN" );
		derbyKeyword.add( "BOTH" );
		derbyKeyword.add( "BY" );
		derbyKeyword.add( "CALL" );
		derbyKeyword.add( "CASCADE" );
		derbyKeyword.add( "CASCADED" );
		derbyKeyword.add( "CASE" );
		derbyKeyword.add( "CAST" );
		derbyKeyword.add( "CHAR" );
		derbyKeyword.add( "CHARACTER" );
		derbyKeyword.add( "CHECK" );
		derbyKeyword.add( "CLOSE" );
		derbyKeyword.add( "COLLATE" );
		derbyKeyword.add( "COLLATION" );
		derbyKeyword.add( "COLUMN" );
		derbyKeyword.add( "COMMIT" );
		derbyKeyword.add( "CONNECT" );
		derbyKeyword.add( "CONNECTION" );
		derbyKeyword.add( "CONSTRAINT" );
		derbyKeyword.add( "CONSTRAINTS" );
		derbyKeyword.add( "CONTINUE" );
		derbyKeyword.add( "CONVERT" );
		derbyKeyword.add( "CORRESPONDING" );
		derbyKeyword.add( "COUNT" );
		derbyKeyword.add( "CREATE" );
		derbyKeyword.add( "CURRENT" );
		derbyKeyword.add( "CURRENT_DATE" );
		derbyKeyword.add( "CURRENT_TIME" );
		derbyKeyword.add( "CURRENT_TIMESTAMP" );
		derbyKeyword.add( "CURRENT_USER" );
		derbyKeyword.add( "CURSOR" );
		derbyKeyword.add( "DEALLOCATE" );
		derbyKeyword.add( "DEC" );
		derbyKeyword.add( "DECIMAL" );
		derbyKeyword.add( "DECLARE" );
		derbyKeyword.add( "DEFERRABLE" );
		derbyKeyword.add( "DEFERRED" );
		derbyKeyword.add( "DELETE" );
		derbyKeyword.add( "DESC" );
		derbyKeyword.add( "DESCRIBE" );
		derbyKeyword.add( "DIAGNOSTICS" );
		derbyKeyword.add( "DISCONNECT" );
		derbyKeyword.add( "DISTINCT" );
		derbyKeyword.add( "DOUBLE" );
		derbyKeyword.add( "DROP" );
		derbyKeyword.add( "ELSE" );
		derbyKeyword.add( "END" );
		derbyKeyword.add( "ENDEXEC" );
		derbyKeyword.add( "ESCAPE" );
		derbyKeyword.add( "EXCEPT" );
		derbyKeyword.add( "EXCEPTION" );
		derbyKeyword.add( "EXEC" );
		derbyKeyword.add( "EXECUTE" );
		derbyKeyword.add( "EXISTS" );
		derbyKeyword.add( "EXPLAIN" );
		derbyKeyword.add( "EXTERNAL" );
		derbyKeyword.add( "FALSE" );
		derbyKeyword.add( "FETCH" );
		derbyKeyword.add( "FIRST" );
		derbyKeyword.add( "FLOAT" );
		derbyKeyword.add( "FOR" );
		derbyKeyword.add( "FOREIGN" );
		derbyKeyword.add( "FOUND" );
		derbyKeyword.add( "FROM" );
		derbyKeyword.add( "FULL" );
		derbyKeyword.add( "FUNCTION" );
		derbyKeyword.add( "GET" );
		derbyKeyword.add( "GET_CURRENT_CONNECTION" );
		derbyKeyword.add( "GLOBAL" );
		derbyKeyword.add( "GO" );
		derbyKeyword.add( "GOTO" );
		derbyKeyword.add( "GRANT" );
		derbyKeyword.add( "GROUP" );
		derbyKeyword.add( "HAVING" );
		derbyKeyword.add( "HOUR" );
		derbyKeyword.add( "IDENTITY" );
		derbyKeyword.add( "IMMEDIATE" );
		derbyKeyword.add( "IN" );
		derbyKeyword.add( "INDICATOR" );
		derbyKeyword.add( "INITIALLY" );
		derbyKeyword.add( "INNER" );
		derbyKeyword.add( "INOUT" );
		derbyKeyword.add( "INPUT" );
		derbyKeyword.add( "INSENSITIVE" );
		derbyKeyword.add( "INSERT" );
		derbyKeyword.add( "INT" );
		derbyKeyword.add( "INTEGER" );
		derbyKeyword.add( "INTERSECT" );
		derbyKeyword.add( "INTO" );
		derbyKeyword.add( "IS" );
		derbyKeyword.add( "ISOLATION" );
		derbyKeyword.add( "JOIN" );
		derbyKeyword.add( "KEY" );
		derbyKeyword.add( "LAST" );
		derbyKeyword.add( "LEFT" );
		derbyKeyword.add( "LIKE" );
		derbyKeyword.add( "LONGINT" );
		derbyKeyword.add( "LOWER" );
		derbyKeyword.add( "LTRIM" );
		derbyKeyword.add( "MATCH" );
		derbyKeyword.add( "MAX" );
		derbyKeyword.add( "MIN" );
		derbyKeyword.add( "MINUTE" );
		derbyKeyword.add( "NATIONAL" );
		derbyKeyword.add( "NATURAL" );
		derbyKeyword.add( "NCHAR" );
		derbyKeyword.add( "NVARCHAR" );
		derbyKeyword.add( "NEXT" );
		derbyKeyword.add( "NO" );
		derbyKeyword.add( "NOT" );
		derbyKeyword.add( "NULL" );
		derbyKeyword.add( "NULLIF" );
		derbyKeyword.add( "NUMERIC" );
		derbyKeyword.add( "OF" );
		derbyKeyword.add( "ON" );
		derbyKeyword.add( "ONLY" );
		derbyKeyword.add( "OPEN" );
		derbyKeyword.add( "OPTION" );
		derbyKeyword.add( "OR" );
		derbyKeyword.add( "ORDER" );
		derbyKeyword.add( "OUT" );
		derbyKeyword.add( "OUTER" );
		derbyKeyword.add( "OUTPUT" );
		derbyKeyword.add( "OVERLAPS" );
		derbyKeyword.add( "PAD" );
		derbyKeyword.add( "PARTIAL" );
		derbyKeyword.add( "PREPARE" );
		derbyKeyword.add( "PRESERVE" );
		derbyKeyword.add( "PRIMARY" );
		derbyKeyword.add( "PRIOR" );
		derbyKeyword.add( "PRIVILEGES" );
		derbyKeyword.add( "PROCEDURE" );
		derbyKeyword.add( "PUBLIC" );
		derbyKeyword.add( "READ" );
		derbyKeyword.add( "REAL" );
		derbyKeyword.add( "REFERENCES" );
		derbyKeyword.add( "RELATIVE" );
		derbyKeyword.add( "RESTRICT" );
		derbyKeyword.add( "REVOKE" );
		derbyKeyword.add( "RIGHT" );
		derbyKeyword.add( "ROLLBACK" );
		derbyKeyword.add( "ROWS" );
		derbyKeyword.add( "RTRIM" );
		derbyKeyword.add( "SCHEMA" );
		derbyKeyword.add( "SCROLL" );
		derbyKeyword.add( "SECOND" );
		derbyKeyword.add( "SELECT" );
		derbyKeyword.add( "SESSION_USER" );
		derbyKeyword.add( "SET" );
		derbyKeyword.add( "SMALLINT" );
		derbyKeyword.add( "SOME" );
		derbyKeyword.add( "SPACE" );
		derbyKeyword.add( "SQL" );
		derbyKeyword.add( "SQLCODE" );
		derbyKeyword.add( "SQLERROR" );
		derbyKeyword.add( "SQLSTATE" );
		derbyKeyword.add( "SUBSTR" );
		derbyKeyword.add( "SUBSTRING" );
		derbyKeyword.add( "SUM" );
		derbyKeyword.add( "SYSTEM_USER" );
		derbyKeyword.add( "TABLE" );
		derbyKeyword.add( "TEMPORARY" );
		derbyKeyword.add( "TIMEZONE_HOUR" );
		derbyKeyword.add( "TIMEZONE_MINUTE" );
		derbyKeyword.add( "TO" );
		derbyKeyword.add( "TRAILING" );
		derbyKeyword.add( "TRANSACTION" );
		derbyKeyword.add( "TRANSLATE" );
		derbyKeyword.add( "TRANSLATION" );
		derbyKeyword.add( "TRUE" );
		derbyKeyword.add( "UNION" );
		derbyKeyword.add( "UNIQUE" );
		derbyKeyword.add( "UNKNOWN" );
		derbyKeyword.add( "UPDATE" );
		derbyKeyword.add( "UPPER" );
		derbyKeyword.add( "USER" );
		derbyKeyword.add( "USING" );
		derbyKeyword.add( "VALUES" );
		derbyKeyword.add( "VARCHAR" );
		derbyKeyword.add( "VARYING" );
		derbyKeyword.add( "VIEW" );
		derbyKeyword.add( "WHENEVER" );
		derbyKeyword.add( "WHERE" );
		derbyKeyword.add( "WITH" );
		derbyKeyword.add( "WORK" );
		derbyKeyword.add( "WRITE" );
		derbyKeyword.add( "XML" );
		derbyKeyword.add( "XMLEXISTS" );
		derbyKeyword.add( "XMLPARSE" );
		derbyKeyword.add( "XMLSERIALIZE" );
		derbyKeyword.add( "YEAR" );
		return derbyKeyword;
	}

}
