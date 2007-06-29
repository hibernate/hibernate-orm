//$Id: MimerSQLDialect.java 7822 2005-08-10 19:49:36Z oneovthafew $
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.Hibernate;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.*;

/**
 * An Hibernate 3 SQL dialect for Mimer SQL. This dialect requires Mimer SQL 9.2.1 or later
 * because of the mappings to NCLOB, BINARY, and BINARY VARYING.
 * @author Fredrik Ålund <fredrik.alund@mimer.se>
 */
public class MimerSQLDialect extends Dialect {

	private static final int NATIONAL_CHAR_LENGTH = 2000;
	private static final int BINARY_MAX_LENGTH = 2000;

	/**
	 * Even thoug Mimer SQL supports character and binary columns up to 15 000 in lenght,
	 * this is also the maximum width of the table (exluding LOBs). To avoid breaking the limit all the
	 * time we limit the length of the character columns to CHAR_MAX_LENTH, NATIONAL_CHAR_LENGTH for national
	 * characters, and BINARY_MAX_LENGTH for binary types.
	 *
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
		registerColumnType( Types.LONGVARCHAR, "CLOB($1)");
		registerColumnType( Types.FLOAT, "FLOAT" );
		registerColumnType( Types.DOUBLE, "DOUBLE PRECISION" );
		registerColumnType( Types.DATE, "DATE" );
		registerColumnType( Types.TIME, "TIME" );
		registerColumnType( Types.TIMESTAMP, "TIMESTAMP" );
		registerColumnType( Types.VARBINARY, BINARY_MAX_LENGTH, "BINARY VARYING($l)" );
		registerColumnType( Types.VARBINARY, "BLOB($1)" );
		registerColumnType( Types.LONGVARBINARY, "BLOB($1)");
		registerColumnType( Types.BINARY, BINARY_MAX_LENGTH, "BINARY" );
		registerColumnType( Types.BINARY, "BLOB($1)" );
		registerColumnType( Types.NUMERIC, "NUMERIC(19, $l)" );
		registerColumnType( Types.BLOB, "BLOB($l)" );
		registerColumnType( Types.CLOB, "NCLOB($l)" );

		registerFunction("abs", new StandardSQLFunction("abs") );
		registerFunction("sign", new StandardSQLFunction("sign", Hibernate.INTEGER) );
		registerFunction("ceiling", new StandardSQLFunction("ceiling") );
		registerFunction("floor", new StandardSQLFunction("floor") );
		registerFunction("round", new StandardSQLFunction("round") );

		registerFunction("dacos", new StandardSQLFunction("dacos", Hibernate.DOUBLE) );
		registerFunction("acos", new StandardSQLFunction("dacos", Hibernate.DOUBLE) );
		registerFunction("dasin", new StandardSQLFunction("dasin", Hibernate.DOUBLE) );
		registerFunction("asin", new StandardSQLFunction("dasin", Hibernate.DOUBLE) );
		registerFunction("datan", new StandardSQLFunction("datan", Hibernate.DOUBLE) );
		registerFunction("atan", new StandardSQLFunction("datan", Hibernate.DOUBLE) );
		registerFunction("datan2", new StandardSQLFunction("datan2", Hibernate.DOUBLE) );
		registerFunction("atan2", new StandardSQLFunction("datan2", Hibernate.DOUBLE) );
		registerFunction("dcos", new StandardSQLFunction("dcos", Hibernate.DOUBLE) );
		registerFunction("cos", new StandardSQLFunction("dcos", Hibernate.DOUBLE) );
		registerFunction("dcot", new StandardSQLFunction("dcot", Hibernate.DOUBLE) );
		registerFunction("cot", new StandardSQLFunction("dcot", Hibernate.DOUBLE) );
		registerFunction("ddegrees", new StandardSQLFunction("ddegrees", Hibernate.DOUBLE) );
		registerFunction("degrees", new StandardSQLFunction("ddegrees", Hibernate.DOUBLE) );
		registerFunction("dexp", new StandardSQLFunction("dexp", Hibernate.DOUBLE) );
		registerFunction("exp", new StandardSQLFunction("dexp", Hibernate.DOUBLE) );
		registerFunction("dlog", new StandardSQLFunction("dlog", Hibernate.DOUBLE) );
		registerFunction("log", new StandardSQLFunction("dlog", Hibernate.DOUBLE) );
		registerFunction("dlog10", new StandardSQLFunction("dlog10", Hibernate.DOUBLE) );
		registerFunction("log10", new StandardSQLFunction("dlog10", Hibernate.DOUBLE) );
		registerFunction("dradian", new StandardSQLFunction("dradian", Hibernate.DOUBLE) );
		registerFunction("radian", new StandardSQLFunction("dradian", Hibernate.DOUBLE) );
		registerFunction("dsin", new StandardSQLFunction("dsin", Hibernate.DOUBLE) );
		registerFunction("sin", new StandardSQLFunction("dsin", Hibernate.DOUBLE) );
		registerFunction("soundex", new StandardSQLFunction("soundex", Hibernate.STRING) );
		registerFunction("dsqrt", new StandardSQLFunction("dsqrt", Hibernate.DOUBLE) );
		registerFunction("sqrt", new StandardSQLFunction("dsqrt", Hibernate.DOUBLE) );
		registerFunction("dtan", new StandardSQLFunction("dtan", Hibernate.DOUBLE) );
		registerFunction("tan", new StandardSQLFunction("dtan", Hibernate.DOUBLE) );
		registerFunction("dpower", new StandardSQLFunction("dpower") );
		registerFunction("power", new StandardSQLFunction("dpower") );

		registerFunction("date", new StandardSQLFunction("date", Hibernate.DATE) );
		registerFunction("dayofweek", new StandardSQLFunction("dayofweek", Hibernate.INTEGER) );
		registerFunction("dayofyear", new StandardSQLFunction("dayofyear", Hibernate.INTEGER) );
		registerFunction("time", new StandardSQLFunction("time", Hibernate.TIME) );
		registerFunction("timestamp", new StandardSQLFunction("timestamp", Hibernate.TIMESTAMP) );
		registerFunction("week", new StandardSQLFunction("week", Hibernate.INTEGER) );


		registerFunction("varchar", new StandardSQLFunction("varchar", Hibernate.STRING) );
		registerFunction("real", new StandardSQLFunction("real", Hibernate.FLOAT) );
		registerFunction("bigint", new StandardSQLFunction("bigint", Hibernate.LONG) );
		registerFunction("char", new StandardSQLFunction("char", Hibernate.CHARACTER) );
		registerFunction("integer", new StandardSQLFunction("integer", Hibernate.INTEGER) );
		registerFunction("smallint", new StandardSQLFunction("smallint", Hibernate.SHORT) );

		registerFunction("ascii_char", new StandardSQLFunction("ascii_char", Hibernate.CHARACTER) );
		registerFunction("ascii_code", new StandardSQLFunction("ascii_code", Hibernate.STRING));
		registerFunction("unicode_char", new StandardSQLFunction("unicode_char", Hibernate.LONG));
		registerFunction("unicode_code", new StandardSQLFunction("unicode_code", Hibernate.STRING));
		registerFunction("upper", new StandardSQLFunction("upper") );
		registerFunction("lower", new StandardSQLFunction("lower") );
		registerFunction("char_length", new StandardSQLFunction("char_length", Hibernate.LONG) );
		registerFunction("bit_length", new StandardSQLFunction("bit_length", Hibernate.STRING));

		getDefaultProperties().setProperty(Environment.USE_STREAMS_FOR_BINARY, "true");
		getDefaultProperties().setProperty(Environment.STATEMENT_BATCH_SIZE, "50");
	}

	/**
	 * The syntax used to add a column to a table
	 */
	public String getAddColumnString() {
		return "add column";
	}

	/**
	 * We do not have to drop constraints before we drop the table
	 */
	public boolean dropConstraints() {
		return false;
	}

	/**
	 * TODO: Check if Mimer SQL cannot handle the way DB2 does
	 */
	public boolean supportsIdentityColumns() {
		return false;
	}

	/**
	 * Mimer SQL supports sequences
	 * @return boolean
	 */
	public boolean supportsSequences() {
		return true;
	}

	/**
	 * The syntax used to get the next value of a sequence in Mimer SQL
	 */
	public String getSequenceNextValString(String sequenceName) {
		return "select next_value of " + sequenceName + " from system.onerow";
	}

	/**
	 * The syntax used to create a sequence. Since we presume the sequences will be used as keys,
	 * we make them unique.
	 */
	public String getCreateSequenceString(String sequenceName) {
		return "create unique sequence " + sequenceName;
	}

	/**
	* The syntax used to drop sequences
	*/
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName + " restrict";
	}

	/**
	* Mimer SQL does not support limit
	*/
	public boolean supportsLimit() {
		return false;
	}

	/**
	* The syntax for using cascade on constraints
	*/
	public String getCascadeConstraintsString() {
		return " cascade";
	}

	/**
	* The syntax for fetching all sequnces avialable in the current schema.
	*/
	public String getQuerySequencesString() {
		return "select sequence_schema || '.' || sequence_name from information_schema.ext_sequences";
	}

	/**
	 * Does the <tt>FOR UPDATE OF</tt> syntax specify particular
	 * columns?
	 */
	public boolean forUpdateOfColumns() {
		return false;
	}

	/**
	 * Support the FOR UPDATE syntax? For now, returns false since
	 * the current version of the Mimer SQL JDBC Driver does not support
	 * updatable resultsets. Otherwise, Mimer SQL actually supports the for update syntax.
	 * @return boolean
	 */
	public boolean supportsForUpdate() {
		return false;
	}


	/**
	 * For now, simply return false since we don't updatable result sets.
	 */
	public boolean supportsOuterJoinForUpdate() {
		return false;
	}
}






