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
 */
package org.hibernate.dialect;

import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.Hibernate;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.exception.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.ViolatedConstraintNameExtracter;
import org.hibernate.util.ReflectHelper;

/**
 * A dialect compatible with the H2 database.
 *
 * @author Thomas Mueller
 */
public class H2Dialect extends Dialect {

	private String querySequenceString;

	public H2Dialect() {
		super();

		querySequenceString = "select sequence_name from information_schema.sequences";
		try {
			// HHH-2300
			Class constants = ReflectHelper.classForName( "org.h2.engine.Constants" );
			Integer build = ( Integer ) constants.getDeclaredField( "BUILD_ID" ).get( null );
			int buildid = build.intValue();
			if ( buildid < 32 ) {
				querySequenceString = "select name from information_schema.sequences";
			}
		}
		catch ( Throwable e ) {
			// ignore (probably H2 not in the classpath)
		}

		registerColumnType( Types.BOOLEAN, "boolean" );
		registerColumnType( Types.BIGINT, "bigint" );
		registerColumnType( Types.BINARY, "binary" );
		registerColumnType( Types.BIT, "boolean" );
		registerColumnType( Types.CHAR, "char($l)" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.DECIMAL, "decimal($p,$s)" );
		registerColumnType( Types.DOUBLE, "double" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.INTEGER, "integer" );
		registerColumnType( Types.LONGVARBINARY, "longvarbinary" );
		registerColumnType( Types.LONGVARCHAR, "longvarchar" );
		registerColumnType( Types.REAL, "real" );
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.TINYINT, "tinyint" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.VARBINARY, "binary($l)" );
		registerColumnType( Types.NUMERIC, "numeric" );
		registerColumnType( Types.BLOB, "blob" );
		registerColumnType( Types.CLOB, "clob" );

		// select topic, syntax from information_schema.help
		// where section like 'Function%' order by section, topic
		//
		// see also ->  http://www.h2database.com/html/functions.html

		// Numeric Functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		registerFunction( "acos", new StandardSQLFunction( "acos", Hibernate.DOUBLE ) );
		registerFunction( "asin", new StandardSQLFunction( "asin", Hibernate.DOUBLE ) );
		registerFunction( "atan", new StandardSQLFunction( "atan", Hibernate.DOUBLE ) );
		registerFunction( "atan2", new StandardSQLFunction( "atan2", Hibernate.DOUBLE ) );
		registerFunction( "bitand", new StandardSQLFunction( "bitand", Hibernate.INTEGER ) );
		registerFunction( "bitor", new StandardSQLFunction( "bitor", Hibernate.INTEGER ) );
		registerFunction( "bitxor", new StandardSQLFunction( "bitxor", Hibernate.INTEGER ) );
		registerFunction( "ceiling", new StandardSQLFunction( "ceiling", Hibernate.DOUBLE ) );
		registerFunction( "cos", new StandardSQLFunction( "cos", Hibernate.DOUBLE ) );
		registerFunction( "compress", new StandardSQLFunction( "compress", Hibernate.BINARY ) );
		registerFunction( "cot", new StandardSQLFunction( "cot", Hibernate.DOUBLE ) );
		registerFunction( "decrypt", new StandardSQLFunction( "decrypt", Hibernate.BINARY ) );
		registerFunction( "degrees", new StandardSQLFunction( "degrees", Hibernate.DOUBLE ) );
		registerFunction( "encrypt", new StandardSQLFunction( "encrypt", Hibernate.BINARY ) );
		registerFunction( "exp", new StandardSQLFunction( "exp", Hibernate.DOUBLE ) );
		registerFunction( "expand", new StandardSQLFunction( "compress", Hibernate.BINARY ) );
		registerFunction( "floor", new StandardSQLFunction( "floor", Hibernate.DOUBLE ) );
		registerFunction( "hash", new StandardSQLFunction( "hash", Hibernate.BINARY ) );
		registerFunction( "log", new StandardSQLFunction( "log", Hibernate.DOUBLE ) );
		registerFunction( "log10", new StandardSQLFunction( "log10", Hibernate.DOUBLE ) );
		registerFunction( "pi", new NoArgSQLFunction( "pi", Hibernate.DOUBLE ) );
		registerFunction( "power", new StandardSQLFunction( "power", Hibernate.DOUBLE ) );
		registerFunction( "radians", new StandardSQLFunction( "radians", Hibernate.DOUBLE ) );
		registerFunction( "rand", new NoArgSQLFunction( "rand", Hibernate.DOUBLE ) );
		registerFunction( "round", new StandardSQLFunction( "round", Hibernate.DOUBLE ) );
		registerFunction( "roundmagic", new StandardSQLFunction( "roundmagic", Hibernate.DOUBLE ) );
		registerFunction( "sign", new StandardSQLFunction( "sign", Hibernate.INTEGER ) );
		registerFunction( "sin", new StandardSQLFunction( "sin", Hibernate.DOUBLE ) );
		registerFunction( "tan", new StandardSQLFunction( "tan", Hibernate.DOUBLE ) );
		registerFunction( "truncate", new StandardSQLFunction( "truncate", Hibernate.DOUBLE ) );

		// String Functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		registerFunction( "ascii", new StandardSQLFunction( "ascii", Hibernate.INTEGER ) );
		registerFunction( "char", new StandardSQLFunction( "char", Hibernate.CHARACTER ) );
		registerFunction( "concat", new VarArgsSQLFunction( Hibernate.STRING, "(", "||", ")" ) );
		registerFunction( "difference", new StandardSQLFunction( "difference", Hibernate.INTEGER ) );
		registerFunction( "hextoraw", new StandardSQLFunction( "hextoraw", Hibernate.STRING ) );
		registerFunction( "insert", new StandardSQLFunction( "lower", Hibernate.STRING ) );
		registerFunction( "left", new StandardSQLFunction( "left", Hibernate.STRING ) );
		registerFunction( "lcase", new StandardSQLFunction( "lcase", Hibernate.STRING ) );
		registerFunction( "ltrim", new StandardSQLFunction( "ltrim", Hibernate.STRING ) );
		registerFunction( "octet_length", new StandardSQLFunction( "octet_length", Hibernate.INTEGER ) );
		registerFunction( "position", new StandardSQLFunction( "position", Hibernate.INTEGER ) );
		registerFunction( "rawtohex", new StandardSQLFunction( "rawtohex", Hibernate.STRING ) );
		registerFunction( "repeat", new StandardSQLFunction( "repeat", Hibernate.STRING ) );
		registerFunction( "replace", new StandardSQLFunction( "replace", Hibernate.STRING ) );
		registerFunction( "right", new StandardSQLFunction( "right", Hibernate.STRING ) );
		registerFunction( "rtrim", new StandardSQLFunction( "rtrim", Hibernate.STRING ) );
		registerFunction( "soundex", new StandardSQLFunction( "soundex", Hibernate.STRING ) );
		registerFunction( "space", new StandardSQLFunction( "space", Hibernate.STRING ) );
		registerFunction( "stringencode", new StandardSQLFunction( "stringencode", Hibernate.STRING ) );
		registerFunction( "stringdecode", new StandardSQLFunction( "stringdecode", Hibernate.STRING ) );
		registerFunction( "stringtoutf8", new StandardSQLFunction( "stringtoutf8", Hibernate.BINARY ) );
		registerFunction( "ucase", new StandardSQLFunction( "ucase", Hibernate.STRING ) );
		registerFunction( "utf8tostring", new StandardSQLFunction( "utf8tostring", Hibernate.STRING ) );

		// Time and Date Functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		registerFunction( "curdate", new NoArgSQLFunction( "curdate", Hibernate.DATE ) );
		registerFunction( "curtime", new NoArgSQLFunction( "curtime", Hibernate.TIME ) );
		registerFunction( "curtimestamp", new NoArgSQLFunction( "curtimestamp", Hibernate.TIME ) );
		registerFunction( "current_date", new NoArgSQLFunction( "current_date", Hibernate.DATE ) );
		registerFunction( "current_time", new NoArgSQLFunction( "current_time", Hibernate.TIME ) );
		registerFunction( "current_timestamp", new NoArgSQLFunction( "current_timestamp", Hibernate.TIMESTAMP ) );
		registerFunction( "datediff", new StandardSQLFunction( "datediff", Hibernate.INTEGER ) );
		registerFunction( "dayname", new StandardSQLFunction( "dayname", Hibernate.STRING ) );
		registerFunction( "dayofmonth", new StandardSQLFunction( "dayofmonth", Hibernate.INTEGER ) );
		registerFunction( "dayofweek", new StandardSQLFunction( "dayofweek", Hibernate.INTEGER ) );
		registerFunction( "dayofyear", new StandardSQLFunction( "dayofyear", Hibernate.INTEGER ) );
		registerFunction( "monthname", new StandardSQLFunction( "monthname", Hibernate.STRING ) );
		registerFunction( "now", new NoArgSQLFunction( "now", Hibernate.TIMESTAMP ) );
		registerFunction( "quarter", new StandardSQLFunction( "quarter", Hibernate.INTEGER ) );
		registerFunction( "week", new StandardSQLFunction( "week", Hibernate.INTEGER ) );

		// System Functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		registerFunction( "database", new NoArgSQLFunction( "database", Hibernate.STRING ) );
		registerFunction( "user", new NoArgSQLFunction( "user", Hibernate.STRING ) );

		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );
	}

	public String getAddColumnString() {
		return "add column";
	}

	public boolean supportsIdentityColumns() {
		return true;
	}

	public String getIdentityColumnString() {
		return "generated by default as identity"; // not null is implicit
	}

	public String getIdentitySelectString() {
		return "call identity()";
	}

	public String getIdentityInsertString() {
		return "null";
	}

	public String getForUpdateString() {
		return " for update";
	}

	public boolean supportsUnique() {
		return true;
	}

	public boolean supportsLimit() {
		return true;
	}

	public String getLimitString(String sql, boolean hasOffset) {
		return new StringBuffer( sql.length() + 20 )
				.append( sql )
				.append( hasOffset ? " limit ? offset ?" : " limit ?" )
				.toString();
	}

	public boolean bindLimitParametersInReverseOrder() {
		return true;
	}

	public boolean bindLimitParametersFirst() {
		return false;
	}

	public boolean supportsIfExistsAfterTableName() {
		return true;
	}

	public boolean supportsSequences() {
		return true;
	}

	public boolean supportsPooledSequences() {
		return true;
	}

	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName;
	}

	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName;
	}

	public String getSelectSequenceNextValString(String sequenceName) {
		return "next value for " + sequenceName;
	}

	public String getSequenceNextValString(String sequenceName) {
		return "call next value for " + sequenceName;
	}

	public String getQuerySequencesString() {
		return querySequenceString;
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
			// 23000: Check constraint violation: {0}
			// 23001: Unique index or primary key violation: {0}
			if ( sqle.getSQLState().startsWith( "23" ) ) {
				final String message = sqle.getMessage();
				int idx = message.indexOf( "violation: " );
				if ( idx > 0 ) {
					constraintName = message.substring( idx + "violation: ".length() );
				}
			}
			return constraintName;
		}
	};

	public boolean supportsTemporaryTables() {
		return true;
	}

	public String getCreateTemporaryTableString() {
		return "create temporary table if not exists";
	}

	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	public String getCurrentTimestampSelectString() {
		return "call current_timestamp()";
	}

	public boolean supportsUnionAll() {
		return true;
	}


	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public boolean supportsLobValueChangePropogation() {
		return false;
	}
}
