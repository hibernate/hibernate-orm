/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.dialect;

import java.io.Serializable;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.AvgWithArgumentCastFunction;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.PessimisticReadSelectLockingStrategy;
import org.hibernate.dialect.lock.PessimisticWriteSelectLockingStrategy;
import org.hibernate.dialect.lock.SelectLockingStrategy;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.type.StandardBasicTypes;
import org.jboss.logging.Logger;

/**
 * An SQL dialect compatible with HSQLDB (HyperSQL).
 * <p/>
 * Note this version supports HSQLDB version 1.8 and higher, only.
 * <p/>
 * Enhancements to version 3.5.0 GA to provide basic support for both HSQLDB 1.8.x and 2.x
 * Does not works with Hibernate 3.2 - 3.4 without alteration.
 *
 * @author Christoph Sturm
 * @author Phillip Baird
 * @author Fred Toussi
 */
public class HSQLDialect extends Dialect {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			HSQLDialect.class.getName()
	);

	/**
	 * version is 18 for 1.8 or 20 for 2.0
	 */
	private int hsqldbVersion = 18;


	/**
	 * Constructs a HSQLDialect
	 */
	public HSQLDialect() {
		super();

		try {
			final Class props = ReflectHelper.classForName( "org.hsqldb.persist.HsqlDatabaseProperties" );
			final String versionString = (String) props.getDeclaredField( "THIS_VERSION" ).get( null );

			hsqldbVersion = Integer.parseInt( versionString.substring( 0, 1 ) ) * 10;
			hsqldbVersion += Integer.parseInt( versionString.substring( 2, 3 ) );
		}
		catch ( Throwable e ) {
			// must be a very old version
		}

		registerColumnType( Types.BIGINT, "bigint" );
		registerColumnType( Types.BINARY, "binary($l)" );
		registerColumnType( Types.BIT, "bit" );
		registerColumnType( Types.BOOLEAN, "boolean" );
		registerColumnType( Types.CHAR, "char($l)" );
		registerColumnType( Types.DATE, "date" );

		registerColumnType( Types.DECIMAL, "decimal($p,$s)" );
		registerColumnType( Types.DOUBLE, "double" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.INTEGER, "integer" );
		registerColumnType( Types.LONGVARBINARY, "longvarbinary" );
		registerColumnType( Types.LONGVARCHAR, "longvarchar" );
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.TINYINT, "tinyint" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.VARBINARY, "varbinary($l)" );

		if ( hsqldbVersion < 20 ) {
			registerColumnType( Types.NUMERIC, "numeric" );
		}
		else {
			registerColumnType( Types.NUMERIC, "numeric($p,$s)" );
		}

		//HSQL has no Blob/Clob support .... but just put these here for now!
		if ( hsqldbVersion < 20 ) {
			registerColumnType( Types.BLOB, "longvarbinary" );
			registerColumnType( Types.CLOB, "longvarchar" );
		}
		else {
			registerColumnType( Types.BLOB, "blob($l)" );
			registerColumnType( Types.CLOB, "clob($l)" );
		}

		// aggregate functions
		registerFunction( "avg", new AvgWithArgumentCastFunction( "double" ) );

		// string functions
		registerFunction( "ascii", new StandardSQLFunction( "ascii", StandardBasicTypes.INTEGER ) );
		registerFunction( "char", new StandardSQLFunction( "char", StandardBasicTypes.CHARACTER ) );
		registerFunction( "lower", new StandardSQLFunction( "lower" ) );
		registerFunction( "upper", new StandardSQLFunction( "upper" ) );
		registerFunction( "lcase", new StandardSQLFunction( "lcase" ) );
		registerFunction( "ucase", new StandardSQLFunction( "ucase" ) );
		registerFunction( "soundex", new StandardSQLFunction( "soundex", StandardBasicTypes.STRING ) );
		registerFunction( "ltrim", new StandardSQLFunction( "ltrim" ) );
		registerFunction( "rtrim", new StandardSQLFunction( "rtrim" ) );
		registerFunction( "reverse", new StandardSQLFunction( "reverse" ) );
		registerFunction( "space", new StandardSQLFunction( "space", StandardBasicTypes.STRING ) );
		registerFunction( "str", new SQLFunctionTemplate( StandardBasicTypes.STRING, "cast(?1 as varchar(256))" ) );
		registerFunction( "to_char", new StandardSQLFunction( "to_char", StandardBasicTypes.STRING ) );
		registerFunction( "rawtohex", new StandardSQLFunction( "rawtohex" ) );
		registerFunction( "hextoraw", new StandardSQLFunction( "hextoraw" ) );

		// system functions
		registerFunction( "user", new NoArgSQLFunction( "user", StandardBasicTypes.STRING ) );
		registerFunction( "database", new NoArgSQLFunction( "database", StandardBasicTypes.STRING ) );

		// datetime functions
		if ( hsqldbVersion < 20 ) {
			registerFunction( "sysdate", new NoArgSQLFunction( "sysdate", StandardBasicTypes.DATE, false ) );
		}
		else {
			registerFunction( "sysdate", new NoArgSQLFunction( "sysdate", StandardBasicTypes.TIMESTAMP, false ) );
		}
		registerFunction( "current_date", new NoArgSQLFunction( "current_date", StandardBasicTypes.DATE, false ) );
		registerFunction( "curdate", new NoArgSQLFunction( "curdate", StandardBasicTypes.DATE ) );
		registerFunction(
				"current_timestamp", new NoArgSQLFunction( "current_timestamp", StandardBasicTypes.TIMESTAMP, false )
		);
		registerFunction( "now", new NoArgSQLFunction( "now", StandardBasicTypes.TIMESTAMP ) );
		registerFunction( "current_time", new NoArgSQLFunction( "current_time", StandardBasicTypes.TIME, false ) );
		registerFunction( "curtime", new NoArgSQLFunction( "curtime", StandardBasicTypes.TIME ) );
		registerFunction( "day", new StandardSQLFunction( "day", StandardBasicTypes.INTEGER ) );
		registerFunction( "dayofweek", new StandardSQLFunction( "dayofweek", StandardBasicTypes.INTEGER ) );
		registerFunction( "dayofyear", new StandardSQLFunction( "dayofyear", StandardBasicTypes.INTEGER ) );
		registerFunction( "dayofmonth", new StandardSQLFunction( "dayofmonth", StandardBasicTypes.INTEGER ) );
		registerFunction( "month", new StandardSQLFunction( "month", StandardBasicTypes.INTEGER ) );
		registerFunction( "year", new StandardSQLFunction( "year", StandardBasicTypes.INTEGER ) );
		registerFunction( "week", new StandardSQLFunction( "week", StandardBasicTypes.INTEGER ) );
		registerFunction( "quarter", new StandardSQLFunction( "quarter", StandardBasicTypes.INTEGER ) );
		registerFunction( "hour", new StandardSQLFunction( "hour", StandardBasicTypes.INTEGER ) );
		registerFunction( "minute", new StandardSQLFunction( "minute", StandardBasicTypes.INTEGER ) );
		registerFunction( "second", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "cast(second(?1) as int)" ) );
		registerFunction( "dayname", new StandardSQLFunction( "dayname", StandardBasicTypes.STRING ) );
		registerFunction( "monthname", new StandardSQLFunction( "monthname", StandardBasicTypes.STRING ) );

		// numeric functions
		registerFunction( "abs", new StandardSQLFunction( "abs" ) );
		registerFunction( "sign", new StandardSQLFunction( "sign", StandardBasicTypes.INTEGER ) );

		registerFunction( "acos", new StandardSQLFunction( "acos", StandardBasicTypes.DOUBLE ) );
		registerFunction( "asin", new StandardSQLFunction( "asin", StandardBasicTypes.DOUBLE ) );
		registerFunction( "atan", new StandardSQLFunction( "atan", StandardBasicTypes.DOUBLE ) );
		registerFunction( "cos", new StandardSQLFunction( "cos", StandardBasicTypes.DOUBLE ) );
		registerFunction( "cot", new StandardSQLFunction( "cot", StandardBasicTypes.DOUBLE ) );
		registerFunction( "exp", new StandardSQLFunction( "exp", StandardBasicTypes.DOUBLE ) );
		registerFunction( "log", new StandardSQLFunction( "log", StandardBasicTypes.DOUBLE ) );
		registerFunction( "log10", new StandardSQLFunction( "log10", StandardBasicTypes.DOUBLE ) );
		registerFunction( "sin", new StandardSQLFunction( "sin", StandardBasicTypes.DOUBLE ) );
		registerFunction( "sqrt", new StandardSQLFunction( "sqrt", StandardBasicTypes.DOUBLE ) );
		registerFunction( "tan", new StandardSQLFunction( "tan", StandardBasicTypes.DOUBLE ) );
		registerFunction( "pi", new NoArgSQLFunction( "pi", StandardBasicTypes.DOUBLE ) );
		registerFunction( "rand", new StandardSQLFunction( "rand", StandardBasicTypes.FLOAT ) );

		registerFunction( "radians", new StandardSQLFunction( "radians", StandardBasicTypes.DOUBLE ) );
		registerFunction( "degrees", new StandardSQLFunction( "degrees", StandardBasicTypes.DOUBLE ) );
		registerFunction( "round", new StandardSQLFunction( "round" ) );
		registerFunction( "roundmagic", new StandardSQLFunction( "roundmagic" ) );
		registerFunction( "truncate", new StandardSQLFunction( "truncate" ) );

		registerFunction( "ceiling", new StandardSQLFunction( "ceiling" ) );
		registerFunction( "floor", new StandardSQLFunction( "floor" ) );

		// special functions
		// from v. 2.2.0 ROWNUM() is supported in all modes as the equivalent of Oracle ROWNUM
		if ( hsqldbVersion > 21 ) {
			registerFunction( "rownum", new NoArgSQLFunction( "rownum", StandardBasicTypes.INTEGER ) );
		}

		// function templates
		registerFunction( "concat", new VarArgsSQLFunction( StandardBasicTypes.STRING, "(", "||", ")" ) );

		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );
	}

	@Override
	public String getAddColumnString() {
		return "add column";
	}

	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public String getIdentityColumnString() {
		//not null is implicit
		return "generated by default as identity (start with 1)";
	}

	@Override
	public String getIdentitySelectString() {
		return "call identity()";
	}

	@Override
	public String getIdentityInsertString() {
		return hsqldbVersion < 20 ? "null" : "default";
	}

	@Override
	public boolean supportsLockTimeouts() {
		return false;
	}

	@Override
	public String getForUpdateString() {
		if ( hsqldbVersion >= 20 ) {
			return " for update";
		}
		else {
			return "";
		}
	}

	@Override
    public LimitHandler buildLimitHandler(String sql, RowSelection selection) {
        return new AbstractLimitHandler(sql, selection) {
        	@Override
        	public String getProcessedSql() {
        		boolean hasOffset = LimitHelper.hasFirstRow(selection);
        		if ( hsqldbVersion < 20 ) {
        			return new StringBuilder( sql.length() + 10 )
        					.append( sql )
        					.insert(
        							sql.toLowerCase().indexOf( "select" ) + 6,
        							hasOffset ? " limit ? ?" : " top ?"
        					)
        					.toString();
        		}
        		else {
        			return sql + (hasOffset ? " offset ? limit ?" : " limit ?");
        		}
        	}

        	@Override
        	public boolean supportsLimit() {
        		return true;
        	}

        	@Override
        	public boolean bindLimitParametersFirst() {
        		return hsqldbVersion < 20;
        	}
        };
    }

	// Note : HSQLDB actually supports [IF EXISTS] before AND after the <tablename>
	// But as CASCADE has to be AFTER IF EXISTS in case it's after the tablename, 
	// We put the IF EXISTS before the tablename to be able to add CASCADE after.
	@Override
	public boolean supportsIfExistsAfterTableName() {
		return false;
	}

	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return true;
	}

	@Override
	public boolean supportsColumnCheck() {
		return hsqldbVersion >= 20;
	}

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public boolean supportsPooledSequences() {
		return true;
	}

	/**
	 * HSQL will start with 0, by default.  In order for Hibernate to know that this not transient,
	 * manually start with 1.
	 */
	@Override
	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName + " start with 1";
	}
	
	/**
	 * Because of the overridden {@link #getCreateSequenceString(String)}, we must also override
	 * {@link #getCreateSequenceString(String, int, int)} to prevent 2 instances of "start with".
	 */
	@Override
	protected String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize) throws MappingException {
		if ( supportsPooledSequences() ) {
			return "create sequence " + sequenceName + " start with " + initialValue + " increment by " + incrementSize;
		}
		throw new MappingException( getClass().getName() + " does not support pooled sequences" );
	}

	@Override
	protected String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName;
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return "next value for " + sequenceName;
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "call next value for " + sequenceName;
	}

	@Override
	public String getQuerySequencesString() {
		// this assumes schema support, which is present in 1.8.0 and later...
		return "select sequence_name from information_schema.system_sequences";
	}

	@Override
	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
		return hsqldbVersion < 20 ? EXTRACTER_18 : EXTRACTER_20;
	}

	private static final ViolatedConstraintNameExtracter EXTRACTER_18 = new TemplatedViolatedConstraintNameExtracter() {
		@Override
		public String extractConstraintName(SQLException sqle) {
			String constraintName = null;

			final int errorCode = JdbcExceptionHelper.extractErrorCode( sqle );

			if ( errorCode == -8 ) {
				constraintName = extractUsingTemplate(
						"Integrity constraint violation ", " table:", sqle.getMessage()
				);
			}
			else if ( errorCode == -9 ) {
				constraintName = extractUsingTemplate(
						"Violation of unique index: ", " in statement [", sqle.getMessage()
				);
			}
			else if ( errorCode == -104 ) {
				constraintName = extractUsingTemplate(
						"Unique constraint violation: ", " in statement [", sqle.getMessage()
				);
			}
			else if ( errorCode == -177 ) {
				constraintName = extractUsingTemplate(
						"Integrity constraint violation - no parent ", " table:",
						sqle.getMessage()
				);
			}
			return constraintName;
		}

	};

	/**
	 * HSQLDB 2.0 messages have changed
	 * messages may be localized - therefore use the common, non-locale element " table: "
	 */
	private static final ViolatedConstraintNameExtracter EXTRACTER_20 = new TemplatedViolatedConstraintNameExtracter() {
		@Override
		public String extractConstraintName(SQLException sqle) {
			String constraintName = null;

			final int errorCode = JdbcExceptionHelper.extractErrorCode( sqle );

			if ( errorCode == -8 ) {
				constraintName = extractUsingTemplate(
						"; ", " table: ", sqle.getMessage()
				);
			}
			else if ( errorCode == -9 ) {
				constraintName = extractUsingTemplate(
						"; ", " table: ", sqle.getMessage()
				);
			}
			else if ( errorCode == -104 ) {
				constraintName = extractUsingTemplate(
						"; ", " table: ", sqle.getMessage()
				);
			}
			else if ( errorCode == -177 ) {
				constraintName = extractUsingTemplate(
						"; ", " table: ", sqle.getMessage()
				);
			}
			return constraintName;
		}
	};

	@Override
	public String getSelectClauseNullString(int sqlType) {
		String literal;
		switch ( sqlType ) {
			case Types.LONGVARCHAR:
			case Types.VARCHAR:
			case Types.CHAR:
				literal = "cast(null as varchar(100))";
				break;
			case Types.LONGVARBINARY:
			case Types.VARBINARY:
			case Types.BINARY:
				literal = "cast(null as varbinary(100))";
				break;
			case Types.CLOB:
				literal = "cast(null as clob)";
				break;
			case Types.BLOB:
				literal = "cast(null as blob)";
				break;
			case Types.DATE:
				literal = "cast(null as date)";
				break;
			case Types.TIMESTAMP:
				literal = "cast(null as timestamp)";
				break;
			case Types.BOOLEAN:
				literal = "cast(null as boolean)";
				break;
			case Types.BIT:
				literal = "cast(null as bit)";
				break;
			case Types.TIME:
				literal = "cast(null as time)";
				break;
			default:
				literal = "cast(null as int)";
		}
		return literal;
	}

	@Override
	public boolean supportsUnionAll() {
		return true;
	}

	// temporary table support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	// Hibernate uses this information for temporary tables that it uses for its own operations
	// therefore the appropriate strategy is taken with different versions of HSQLDB

	// All versions of HSQLDB support GLOBAL TEMPORARY tables where the table
	// definition is shared by all users but data is private to the session
	// HSQLDB 2.0 also supports session-based LOCAL TEMPORARY tables where
	// the definition and data is private to the session and table declaration
	// can happen in the middle of a transaction

	@Override
	public boolean supportsTemporaryTables() {
		return true;
	}

	@Override
	public String generateTemporaryTableName(String baseTableName) {
		if ( hsqldbVersion < 20 ) {
			return "HT_" + baseTableName;
		}
		else {
			// With HSQLDB 2.0, the table name is qualified with MODULE to assist the drop
			// statement (in-case there is a global name beginning with HT_)
			return "MODULE.HT_" + baseTableName;
		}
	}

	@Override
	public String getCreateTemporaryTableString() {
		if ( hsqldbVersion < 20 ) {
			return "create global temporary table";
		}
		else {
			return "declare local temporary table";
		}
	}

	@Override
	public String getCreateTemporaryTablePostfix() {
		return "";
	}

	@Override
	public String getDropTemporaryTableString() {
		return "drop table";
	}

	@Override
	public Boolean performTemporaryTableDDLInIsolation() {
		// Different behavior for GLOBAL TEMPORARY (1.8) and LOCAL TEMPORARY (2.0)
		if ( hsqldbVersion < 20 ) {
			return Boolean.TRUE;
		}
		else {
			return Boolean.FALSE;
		}
	}

	@Override
	public boolean dropTemporaryTableAfterUse() {
		// Version 1.8 GLOBAL TEMPORARY table definitions persist beyond the end
		// of the session (by default, data is cleared at commit).<p>
		//
		// Version 2.x LOCAL TEMPORARY table definitions do not persist beyond
		// the end of the session (by default, data is cleared at commit).
		return true;
	}

	// current timestamp support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * HSQLDB 1.8.x requires CALL CURRENT_TIMESTAMP but this should not
	 * be treated as a callable statement. It is equivalent to
	 * "select current_timestamp from dual" in some databases.
	 * HSQLDB 2.0 also supports VALUES CURRENT_TIMESTAMP
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "call current_timestamp";
	}

	@Override
	public String getCurrentTimestampSQLFunctionName() {
		// the standard SQL function name is current_timestamp...
		return "current_timestamp";
	}

	/**
	 * For HSQLDB 2.0, this is a copy of the base class implementation.
	 * For HSQLDB 1.8, only READ_UNCOMMITTED is supported.
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		if ( lockMode == LockMode.PESSIMISTIC_FORCE_INCREMENT ) {
			return new PessimisticForceIncrementLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode == LockMode.PESSIMISTIC_WRITE ) {
			return new PessimisticWriteSelectLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode == LockMode.PESSIMISTIC_READ ) {
			return new PessimisticReadSelectLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode == LockMode.OPTIMISTIC ) {
			return new OptimisticLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode == LockMode.OPTIMISTIC_FORCE_INCREMENT ) {
			return new OptimisticForceIncrementLockingStrategy( lockable, lockMode );
		}

		if ( hsqldbVersion < 20 ) {
			return new ReadUncommittedLockingStrategy( lockable, lockMode );
		}
		else {
			return new SelectLockingStrategy( lockable, lockMode );
		}
	}

	private static class ReadUncommittedLockingStrategy extends SelectLockingStrategy {
		public ReadUncommittedLockingStrategy(Lockable lockable, LockMode lockMode) {
			super( lockable, lockMode );
		}

		public void lock(Serializable id, Object version, Object object, int timeout, SessionImplementor session)
				throws StaleObjectStateException, JDBCException {
			if ( getLockMode().greaterThan( LockMode.READ ) ) {
				LOG.hsqldbSupportsOnlyReadCommittedIsolation();
			}
			super.lock( id, version, object, timeout, session );
		}
	}

	@Override
	public boolean supportsCommentOn() {
		return hsqldbVersion >= 20;
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsEmptyInList() {
		return false;
	}

	@Override
	public boolean requiresCastingOfParametersInSelectClause() {
		return true;
	}

	@Override
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		return hsqldbVersion >= 20;
	}

	@Override
	public boolean doesRepeatableReadCauseReadersToBlockWriters() {
		return hsqldbVersion >= 20;
	}

	@Override
	public boolean supportsLobValueChangePropogation() {
		return false;
	}

	@Override
	public String toBooleanValueString(boolean bool) {
		return String.valueOf( bool );
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		return false;
	}

	// Do not drop constraints explicitly, just do this by cascading instead.
	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public String getCascadeConstraintsString() {
		return " CASCADE ";
	}

	@Override
	public boolean qualifyIndexName() {
		return false;
	}
}
