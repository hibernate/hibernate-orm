/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.cfg.Environment;
import org.hibernate.query.sqm.produce.function.spi.NamedSqmFunctionTemplate;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.MimerSQLIdentityColumnSupport;
import org.hibernate.type.StandardBasicTypes;

/**
 * An Hibernate 3 SQL dialect for Mimer SQL. This dialect requires Mimer SQL 9.2.1 or later
 * because of the mappings to NCLOB, BINARY, and BINARY VARYING.
 *
 * @author Fredrik lund <fredrik.alund@mimer.se>
 */
@SuppressWarnings("deprecation")
public class MimerSQLDialect extends Dialect {

	private static final int NATIONAL_CHAR_LENGTH = 2000;
	private static final int BINARY_MAX_LENGTH = 2000;

	/**
	 * Even thoug Mimer SQL supports character and binary columns up to 15 000 in lenght,
	 * this is also the maximum width of the table (exluding LOBs). To avoid breaking the limit all the
	 * time we limit the length of the character columns to CHAR_MAX_LENTH, NATIONAL_CHAR_LENGTH for national
	 * characters, and BINARY_MAX_LENGTH for binary types.
	 */
	public MimerSQLDialect() {
		super();
		registerColumnType( Types.BIT, "ODBC.BIT" );
		registerColumnType( Types.BIGINT, "BIGINT" );
		registerColumnType( Types.SMALLINT, "SMALLINT" );
		registerColumnType( Types.TINYINT, "ODBC.TINYINT" );
		registerColumnType( Types.INTEGER, "INTEGER" );
		registerColumnType( Types.CHAR, "NCHAR(1)" );
		registerColumnType( Types.VARCHAR, NATIONAL_CHAR_LENGTH, "NATIONAL CHARACTER VARYING($l)" );
		registerColumnType( Types.VARCHAR, "NCLOB($l)" );
		registerColumnType( Types.LONGVARCHAR, "CLOB($1)" );
		registerColumnType( Types.FLOAT, "FLOAT" );
		registerColumnType( Types.DOUBLE, "DOUBLE PRECISION" );
		registerColumnType( Types.DATE, "DATE" );
		registerColumnType( Types.TIME, "TIME" );
		registerColumnType( Types.TIMESTAMP, "TIMESTAMP" );
		registerColumnType( Types.VARBINARY, BINARY_MAX_LENGTH, "BINARY VARYING($l)" );
		registerColumnType( Types.VARBINARY, "BLOB($1)" );
		registerColumnType( Types.LONGVARBINARY, "BLOB($1)" );
		registerColumnType( Types.BINARY, BINARY_MAX_LENGTH, "BINARY" );
		registerColumnType( Types.BINARY, "BLOB($1)" );
		registerColumnType( Types.NUMERIC, "NUMERIC(19, $l)" );
		registerColumnType( Types.BLOB, "BLOB($l)" );
		registerColumnType( Types.CLOB, "NCLOB($l)" );

		registerFunction( "abs", new NamedSqmFunctionTemplate( "abs" ) );
		registerFunction( "sign", new NamedSqmFunctionTemplate( "sign", StandardBasicTypes.INTEGER ) );
		registerFunction( "ceiling", new NamedSqmFunctionTemplate( "ceiling" ) );
		registerFunction( "floor", new NamedSqmFunctionTemplate( "floor" ) );
		registerFunction( "round", new NamedSqmFunctionTemplate( "round" ) );

		registerFunction( "dacos", new NamedSqmFunctionTemplate( "dacos", StandardBasicTypes.DOUBLE ) );
		registerFunction( "acos", new NamedSqmFunctionTemplate( "dacos", StandardBasicTypes.DOUBLE ) );
		registerFunction( "dasin", new NamedSqmFunctionTemplate( "dasin", StandardBasicTypes.DOUBLE ) );
		registerFunction( "asin", new NamedSqmFunctionTemplate( "dasin", StandardBasicTypes.DOUBLE ) );
		registerFunction( "datan", new NamedSqmFunctionTemplate( "datan", StandardBasicTypes.DOUBLE ) );
		registerFunction( "atan", new NamedSqmFunctionTemplate( "datan", StandardBasicTypes.DOUBLE ) );
		registerFunction( "datan2", new NamedSqmFunctionTemplate( "datan2", StandardBasicTypes.DOUBLE ) );
		registerFunction( "atan2", new NamedSqmFunctionTemplate( "datan2", StandardBasicTypes.DOUBLE ) );
		registerFunction( "dcos", new NamedSqmFunctionTemplate( "dcos", StandardBasicTypes.DOUBLE ) );
		registerFunction( "cos", new NamedSqmFunctionTemplate( "dcos", StandardBasicTypes.DOUBLE ) );
		registerFunction( "dcot", new NamedSqmFunctionTemplate( "dcot", StandardBasicTypes.DOUBLE ) );
		registerFunction( "cot", new NamedSqmFunctionTemplate( "dcot", StandardBasicTypes.DOUBLE ) );
		registerFunction( "ddegrees", new NamedSqmFunctionTemplate( "ddegrees", StandardBasicTypes.DOUBLE ) );
		registerFunction( "degrees", new NamedSqmFunctionTemplate( "ddegrees", StandardBasicTypes.DOUBLE ) );
		registerFunction( "dexp", new NamedSqmFunctionTemplate( "dexp", StandardBasicTypes.DOUBLE ) );
		registerFunction( "exp", new NamedSqmFunctionTemplate( "dexp", StandardBasicTypes.DOUBLE ) );
		registerFunction( "dlog", new NamedSqmFunctionTemplate( "dlog", StandardBasicTypes.DOUBLE ) );
		registerFunction( "log", new NamedSqmFunctionTemplate( "dlog", StandardBasicTypes.DOUBLE ) );
		registerFunction( "dlog10", new NamedSqmFunctionTemplate( "dlog10", StandardBasicTypes.DOUBLE ) );
		registerFunction( "log10", new NamedSqmFunctionTemplate( "dlog10", StandardBasicTypes.DOUBLE ) );
		registerFunction( "dradian", new NamedSqmFunctionTemplate( "dradian", StandardBasicTypes.DOUBLE ) );
		registerFunction( "radian", new NamedSqmFunctionTemplate( "dradian", StandardBasicTypes.DOUBLE ) );
		registerFunction( "dsin", new NamedSqmFunctionTemplate( "dsin", StandardBasicTypes.DOUBLE ) );
		registerFunction( "sin", new NamedSqmFunctionTemplate( "dsin", StandardBasicTypes.DOUBLE ) );
		registerFunction( "soundex", new NamedSqmFunctionTemplate( "soundex", StandardBasicTypes.STRING ) );
		registerFunction( "dsqrt", new NamedSqmFunctionTemplate( "dsqrt", StandardBasicTypes.DOUBLE ) );
		registerFunction( "sqrt", new NamedSqmFunctionTemplate( "dsqrt", StandardBasicTypes.DOUBLE ) );
		registerFunction( "dtan", new NamedSqmFunctionTemplate( "dtan", StandardBasicTypes.DOUBLE ) );
		registerFunction( "tan", new NamedSqmFunctionTemplate( "dtan", StandardBasicTypes.DOUBLE ) );
		registerFunction( "dpower", new NamedSqmFunctionTemplate( "dpower" ) );
		registerFunction( "power", new NamedSqmFunctionTemplate( "dpower" ) );

		registerFunction( "date", new NamedSqmFunctionTemplate( "date", StandardBasicTypes.DATE ) );
		registerFunction( "dayofweek", new NamedSqmFunctionTemplate( "dayofweek", StandardBasicTypes.INTEGER ) );
		registerFunction( "dayofyear", new NamedSqmFunctionTemplate( "dayofyear", StandardBasicTypes.INTEGER ) );
		registerFunction( "time", new NamedSqmFunctionTemplate( "time", StandardBasicTypes.TIME ) );
		registerFunction( "timestamp", new NamedSqmFunctionTemplate( "timestamp", StandardBasicTypes.TIMESTAMP ) );
		registerFunction( "week", new NamedSqmFunctionTemplate( "week", StandardBasicTypes.INTEGER ) );


		registerFunction( "varchar", new NamedSqmFunctionTemplate( "varchar", StandardBasicTypes.STRING ) );
		registerFunction( "real", new NamedSqmFunctionTemplate( "real", StandardBasicTypes.FLOAT ) );
		registerFunction( "bigint", new NamedSqmFunctionTemplate( "bigint", StandardBasicTypes.LONG ) );
		registerFunction( "char", new NamedSqmFunctionTemplate( "char", StandardBasicTypes.CHARACTER ) );
		registerFunction( "integer", new NamedSqmFunctionTemplate( "integer", StandardBasicTypes.INTEGER ) );
		registerFunction( "smallint", new NamedSqmFunctionTemplate( "smallint", StandardBasicTypes.SHORT ) );

		registerFunction( "ascii_char", new NamedSqmFunctionTemplate( "ascii_char", StandardBasicTypes.CHARACTER ) );
		registerFunction( "ascii_code", new NamedSqmFunctionTemplate( "ascii_code", StandardBasicTypes.STRING ) );
		registerFunction( "unicode_char", new NamedSqmFunctionTemplate( "unicode_char", StandardBasicTypes.LONG ) );
		registerFunction( "unicode_code", new NamedSqmFunctionTemplate( "unicode_code", StandardBasicTypes.STRING ) );
		registerFunction( "upper", new NamedSqmFunctionTemplate( "upper" ) );
		registerFunction( "lower", new NamedSqmFunctionTemplate( "lower" ) );
		registerFunction( "char_length", new NamedSqmFunctionTemplate( "char_length", StandardBasicTypes.LONG ) );
		registerFunction( "bit_length", new NamedSqmFunctionTemplate( "bit_length", StandardBasicTypes.STRING ) );

		getDefaultProperties().setProperty( Environment.USE_STREAMS_FOR_BINARY, "true" );
		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, "50" );
	}


	@Override
	public String getAddColumnString() {
		return "add column";
	}

	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "select next_value of " + sequenceName + " from system.onerow";
	}

	@Override
	public String getCreateSequenceString(String sequenceName) {
		return "create unique sequence " + sequenceName;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName + " restrict";
	}

	@Override
	public boolean supportsLimit() {
		return false;
	}

	@Override
	public String getCascadeConstraintsString() {
		return " cascade";
	}

	@Override
	public String getQuerySequencesString() {
		return "select sequence_schema || '.' || sequence_name from information_schema.ext_sequences";
	}

	@Override
	public boolean forUpdateOfColumns() {
		return false;
	}

	@Override
	public boolean supportsOuterJoinForUpdate() {
		return false;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new MimerSQLIdentityColumnSupport();
	}
}
