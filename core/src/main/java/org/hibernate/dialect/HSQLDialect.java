/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.dialect;

import java.sql.SQLException;
import java.sql.Types;
import java.io.Serializable;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.JDBCException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.dialect.lock.*;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.exception.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.ViolatedConstraintNameExtracter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An SQL dialect compatible with HSQLDB (Hypersonic SQL).
 * <p/>
 * Note this version supports HSQLDB version 1.8 and higher, only.
 *
 * @author Christoph Sturm
 * @author Phillip Baird
 */
public class HSQLDialect extends Dialect {

	private static final Logger log = LoggerFactory.getLogger( HSQLDialect.class );

	public HSQLDialect() {
		super();
		registerColumnType( Types.BIGINT, "bigint" );
		registerColumnType( Types.BINARY, "binary" );
		registerColumnType( Types.BIT, "bit" );
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.DECIMAL, "decimal" );
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
		registerColumnType( Types.NUMERIC, "numeric" );
		//HSQL has no Blob/Clob support .... but just put these here for now!
		registerColumnType( Types.BLOB, "longvarbinary" );
		registerColumnType( Types.CLOB, "longvarchar" );
		registerColumnType( Types.LONGVARBINARY, "longvarbinary" );
		registerColumnType( Types.LONGVARCHAR, "longvarchar" );

		registerFunction( "ascii", new StandardSQLFunction( "ascii", Hibernate.INTEGER ) );
		registerFunction( "char", new StandardSQLFunction( "char", Hibernate.CHARACTER ) );
		registerFunction( "lower", new StandardSQLFunction( "lower" ) );
		registerFunction( "upper", new StandardSQLFunction( "upper" ) );
		registerFunction( "lcase", new StandardSQLFunction( "lcase" ) );
		registerFunction( "ucase", new StandardSQLFunction( "ucase" ) );
		registerFunction( "soundex", new StandardSQLFunction( "soundex", Hibernate.STRING ) );
		registerFunction( "ltrim", new StandardSQLFunction( "ltrim" ) );
		registerFunction( "rtrim", new StandardSQLFunction( "rtrim" ) );
		registerFunction( "reverse", new StandardSQLFunction( "reverse" ) );
		registerFunction( "space", new StandardSQLFunction( "space", Hibernate.STRING ) );
		registerFunction( "rawtohex", new StandardSQLFunction( "rawtohex" ) );
		registerFunction( "hextoraw", new StandardSQLFunction( "hextoraw" ) );

		registerFunction( "user", new NoArgSQLFunction( "user", Hibernate.STRING ) );
		registerFunction( "database", new NoArgSQLFunction( "database", Hibernate.STRING ) );

		registerFunction( "current_date", new NoArgSQLFunction( "current_date", Hibernate.DATE, false ) );
		registerFunction( "curdate", new NoArgSQLFunction( "curdate", Hibernate.DATE ) );
		registerFunction( "current_timestamp", new NoArgSQLFunction( "current_timestamp", Hibernate.TIMESTAMP, false ) );
		registerFunction( "now", new NoArgSQLFunction( "now", Hibernate.TIMESTAMP ) );
		registerFunction( "current_time", new NoArgSQLFunction( "current_time", Hibernate.TIME, false ) );
		registerFunction( "curtime", new NoArgSQLFunction( "curtime", Hibernate.TIME ) );
		registerFunction( "day", new StandardSQLFunction( "day", Hibernate.INTEGER ) );
		registerFunction( "dayofweek", new StandardSQLFunction( "dayofweek", Hibernate.INTEGER ) );
		registerFunction( "dayofyear", new StandardSQLFunction( "dayofyear", Hibernate.INTEGER ) );
		registerFunction( "dayofmonth", new StandardSQLFunction( "dayofmonth", Hibernate.INTEGER ) );
		registerFunction( "month", new StandardSQLFunction( "month", Hibernate.INTEGER ) );
		registerFunction( "year", new StandardSQLFunction( "year", Hibernate.INTEGER ) );
		registerFunction( "week", new StandardSQLFunction( "week", Hibernate.INTEGER ) );
		registerFunction( "quater", new StandardSQLFunction( "quater", Hibernate.INTEGER ) );
		registerFunction( "hour", new StandardSQLFunction( "hour", Hibernate.INTEGER ) );
		registerFunction( "minute", new StandardSQLFunction( "minute", Hibernate.INTEGER ) );
		registerFunction( "second", new StandardSQLFunction( "second", Hibernate.INTEGER ) );
		registerFunction( "dayname", new StandardSQLFunction( "dayname", Hibernate.STRING ) );
		registerFunction( "monthname", new StandardSQLFunction( "monthname", Hibernate.STRING ) );

		registerFunction( "abs", new StandardSQLFunction( "abs" ) );
		registerFunction( "sign", new StandardSQLFunction( "sign", Hibernate.INTEGER ) );

		registerFunction( "acos", new StandardSQLFunction( "acos", Hibernate.DOUBLE ) );
		registerFunction( "asin", new StandardSQLFunction( "asin", Hibernate.DOUBLE ) );
		registerFunction( "atan", new StandardSQLFunction( "atan", Hibernate.DOUBLE ) );
		registerFunction( "cos", new StandardSQLFunction( "cos", Hibernate.DOUBLE ) );
		registerFunction( "cot", new StandardSQLFunction( "cot", Hibernate.DOUBLE ) );
		registerFunction( "exp", new StandardSQLFunction( "exp", Hibernate.DOUBLE ) );
		registerFunction( "log", new StandardSQLFunction( "log", Hibernate.DOUBLE ) );
		registerFunction( "log10", new StandardSQLFunction( "log10", Hibernate.DOUBLE ) );
		registerFunction( "sin", new StandardSQLFunction( "sin", Hibernate.DOUBLE ) );
		registerFunction( "sqrt", new StandardSQLFunction( "sqrt", Hibernate.DOUBLE ) );
		registerFunction( "tan", new StandardSQLFunction( "tan", Hibernate.DOUBLE ) );
		registerFunction( "pi", new NoArgSQLFunction( "pi", Hibernate.DOUBLE ) );
		registerFunction( "rand", new StandardSQLFunction( "rand", Hibernate.FLOAT ) );

		registerFunction( "radians", new StandardSQLFunction( "radians", Hibernate.DOUBLE ) );
		registerFunction( "degrees", new StandardSQLFunction( "degrees", Hibernate.DOUBLE ) );
		registerFunction( "roundmagic", new StandardSQLFunction( "roundmagic" ) );

		registerFunction( "ceiling", new StandardSQLFunction( "ceiling" ) );
		registerFunction( "floor", new StandardSQLFunction( "floor" ) );

		// Multi-param dialect functions...
		registerFunction( "mod", new StandardSQLFunction( "mod", Hibernate.INTEGER ) );

		// function templates
		registerFunction( "concat", new VarArgsSQLFunction( Hibernate.STRING, "(", "||", ")" ) );

		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );
	}

	public String getAddColumnString() {
		return "add column";
	}

	public boolean supportsIdentityColumns() {
		return true;
	}

	public String getIdentityColumnString() {
		return "generated by default as identity (start with 1)"; //not null is implicit
	}

	public String getIdentitySelectString() {
		return "call identity()";
	}

	public String getIdentityInsertString() {
		return "null";
	}

	public boolean supportsLockTimeouts() {
		return false;
	}

	public String getForUpdateString() {
		return "";
	}

	public boolean supportsUnique() {
		return false;
	}

	public boolean supportsLimit() {
		return true;
	}

	public String getLimitString(String sql, boolean hasOffset) {
		return new StringBuffer( sql.length() + 10 )
				.append( sql )
				.insert( sql.toLowerCase().indexOf( "select" ) + 6, hasOffset ? " limit ? ?" : " top ?" )
				.toString();
	}

	public boolean bindLimitParametersFirst() {
		return true;
	}

	public boolean supportsIfExistsAfterTableName() {
		return true;
	}

	public boolean supportsColumnCheck() {
		return false;
	}

	public boolean supportsSequences() {
		return true;
	}

	public boolean supportsPooledSequences() {
		return true;
	}

	protected String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName;
	}

	protected String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName;
	}

	public String getSelectSequenceNextValString(String sequenceName) {
		return "next value for " + sequenceName;
	}

	public String getSequenceNextValString(String sequenceName) {
		return "call next value for " + sequenceName;
	}

	public String getQuerySequencesString() {
		// this assumes schema support, which is present in 1.8.0 and later...
		return "select sequence_name from information_schema.system_sequences";
	}

	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
		return EXTRACTER;
	}

	private static ViolatedConstraintNameExtracter EXTRACTER = new TemplatedViolatedConstraintNameExtracter() {

		/**
		 * Extract the name of the violated constraint from the given SQLException.
		 *
		 * @param sqle The exception that was the result of the constraint violation.
		 * @return The extracted constraint name.
		 */
		public String extractConstraintName(SQLException sqle) {
			String constraintName = null;

			int errorCode = JDBCExceptionHelper.extractErrorCode( sqle );

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
						"Integrity constraint violation - no parent ", " table:", sqle.getMessage()
				);
			}

			return constraintName;
		}

	};

	/**
	 * HSQL does not really support temp tables; just take advantage of the
	 * fact that it is a single user db...
	 */
	public boolean supportsTemporaryTables() {
		return true;
	}

	public boolean supportsCurrentTimestampSelection() {
		return false;
	}

	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		// HSQLDB only supports READ_UNCOMMITTED transaction isolation
		if ( lockMode==LockMode.PESSIMISTIC_FORCE_INCREMENT) {
			return new PessimisticForceIncrementLockingStrategy( lockable, lockMode);
		}
		else if ( lockMode==LockMode.OPTIMISTIC) {
			return new OptimisticLockingStrategy( lockable, lockMode);
		}
		else if ( lockMode==LockMode.OPTIMISTIC_FORCE_INCREMENT) {
			return new OptimisticForceIncrementLockingStrategy( lockable, lockMode);
		}
		return new ReadUncommittedLockingStrategy( lockable, lockMode );
		
	}

	public static class ReadUncommittedLockingStrategy extends SelectLockingStrategy {
		public ReadUncommittedLockingStrategy(Lockable lockable, LockMode lockMode) {
			super( lockable, lockMode );
		}

		public void lock(Serializable id, Object version, Object object, int timeout, SessionImplementor session)
				throws StaleObjectStateException, JDBCException {
			if ( getLockMode().greaterThan( LockMode.READ ) ) {
				log.warn( "HSQLDB supports only READ_UNCOMMITTED isolation" );
			}
			super.lock( id, version, object, timeout, session );
		}
	}


	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public boolean supportsEmptyInList() {
		return false;
	}

	public boolean supportsLobValueChangePropogation() {
		return false;
	}
}
