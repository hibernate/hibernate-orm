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
import java.sql.Types;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StandardBasicTypes;

/**
 * An Hibernate 3 SQL dialect for Mimer SQL. This dialect requires Mimer SQL 9.2.1 or later
 * because of the mappings to NCLOB, BINARY, and BINARY VARYING.
 * @author Fredrik lund <fredrik.alund@mimer.se>
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
		registerFunction("sign", new StandardSQLFunction("sign", StandardBasicTypes.INTEGER) );
		registerFunction("ceiling", new StandardSQLFunction("ceiling") );
		registerFunction("floor", new StandardSQLFunction("floor") );
		registerFunction("round", new StandardSQLFunction("round") );

		registerFunction("dacos", new StandardSQLFunction("dacos", StandardBasicTypes.DOUBLE) );
		registerFunction("acos", new StandardSQLFunction("dacos", StandardBasicTypes.DOUBLE) );
		registerFunction("dasin", new StandardSQLFunction("dasin", StandardBasicTypes.DOUBLE) );
		registerFunction("asin", new StandardSQLFunction("dasin", StandardBasicTypes.DOUBLE) );
		registerFunction("datan", new StandardSQLFunction("datan", StandardBasicTypes.DOUBLE) );
		registerFunction("atan", new StandardSQLFunction("datan", StandardBasicTypes.DOUBLE) );
		registerFunction("datan2", new StandardSQLFunction("datan2", StandardBasicTypes.DOUBLE) );
		registerFunction("atan2", new StandardSQLFunction("datan2", StandardBasicTypes.DOUBLE) );
		registerFunction("dcos", new StandardSQLFunction("dcos", StandardBasicTypes.DOUBLE) );
		registerFunction("cos", new StandardSQLFunction("dcos", StandardBasicTypes.DOUBLE) );
		registerFunction("dcot", new StandardSQLFunction("dcot", StandardBasicTypes.DOUBLE) );
		registerFunction("cot", new StandardSQLFunction("dcot", StandardBasicTypes.DOUBLE) );
		registerFunction("ddegrees", new StandardSQLFunction("ddegrees", StandardBasicTypes.DOUBLE) );
		registerFunction("degrees", new StandardSQLFunction("ddegrees", StandardBasicTypes.DOUBLE) );
		registerFunction("dexp", new StandardSQLFunction("dexp", StandardBasicTypes.DOUBLE) );
		registerFunction("exp", new StandardSQLFunction("dexp", StandardBasicTypes.DOUBLE) );
		registerFunction("dlog", new StandardSQLFunction("dlog", StandardBasicTypes.DOUBLE) );
		registerFunction("log", new StandardSQLFunction("dlog", StandardBasicTypes.DOUBLE) );
		registerFunction("dlog10", new StandardSQLFunction("dlog10", StandardBasicTypes.DOUBLE) );
		registerFunction("log10", new StandardSQLFunction("dlog10", StandardBasicTypes.DOUBLE) );
		registerFunction("dradian", new StandardSQLFunction("dradian", StandardBasicTypes.DOUBLE) );
		registerFunction("radian", new StandardSQLFunction("dradian", StandardBasicTypes.DOUBLE) );
		registerFunction("dsin", new StandardSQLFunction("dsin", StandardBasicTypes.DOUBLE) );
		registerFunction("sin", new StandardSQLFunction("dsin", StandardBasicTypes.DOUBLE) );
		registerFunction("soundex", new StandardSQLFunction("soundex", StandardBasicTypes.STRING) );
		registerFunction("dsqrt", new StandardSQLFunction("dsqrt", StandardBasicTypes.DOUBLE) );
		registerFunction("sqrt", new StandardSQLFunction("dsqrt", StandardBasicTypes.DOUBLE) );
		registerFunction("dtan", new StandardSQLFunction("dtan", StandardBasicTypes.DOUBLE) );
		registerFunction("tan", new StandardSQLFunction("dtan", StandardBasicTypes.DOUBLE) );
		registerFunction("dpower", new StandardSQLFunction("dpower") );
		registerFunction("power", new StandardSQLFunction("dpower") );

		registerFunction("date", new StandardSQLFunction("date", StandardBasicTypes.DATE) );
		registerFunction("dayofweek", new StandardSQLFunction("dayofweek", StandardBasicTypes.INTEGER) );
		registerFunction("dayofyear", new StandardSQLFunction("dayofyear", StandardBasicTypes.INTEGER) );
		registerFunction("time", new StandardSQLFunction("time", StandardBasicTypes.TIME) );
		registerFunction("timestamp", new StandardSQLFunction("timestamp", StandardBasicTypes.TIMESTAMP) );
		registerFunction("week", new StandardSQLFunction("week", StandardBasicTypes.INTEGER) );


		registerFunction("varchar", new StandardSQLFunction("varchar", StandardBasicTypes.STRING) );
		registerFunction("real", new StandardSQLFunction("real", StandardBasicTypes.FLOAT) );
		registerFunction("bigint", new StandardSQLFunction("bigint", StandardBasicTypes.LONG) );
		registerFunction("char", new StandardSQLFunction("char", StandardBasicTypes.CHARACTER) );
		registerFunction("integer", new StandardSQLFunction("integer", StandardBasicTypes.INTEGER) );
		registerFunction("smallint", new StandardSQLFunction("smallint", StandardBasicTypes.SHORT) );

		registerFunction("ascii_char", new StandardSQLFunction("ascii_char", StandardBasicTypes.CHARACTER) );
		registerFunction("ascii_code", new StandardSQLFunction("ascii_code", StandardBasicTypes.STRING));
		registerFunction("unicode_char", new StandardSQLFunction("unicode_char", StandardBasicTypes.LONG));
		registerFunction("unicode_code", new StandardSQLFunction("unicode_code", StandardBasicTypes.STRING));
		registerFunction("upper", new StandardSQLFunction("upper") );
		registerFunction("lower", new StandardSQLFunction("lower") );
		registerFunction("char_length", new StandardSQLFunction("char_length", StandardBasicTypes.LONG) );
		registerFunction("bit_length", new StandardSQLFunction("bit_length", StandardBasicTypes.STRING));

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






