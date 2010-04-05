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

import java.sql.Types;

import org.hibernate.Hibernate;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;

/**
 * An SQL dialect for Ingres 9.2.
 * <p/>
 * Known limitations:
 * <ul>
 * <li> Only supports simple constants or columns on the left side of an IN, making (1,2,3) in (...) or (&lt;subselect&gt;) in (...) non-supported.
 * <li> Supports only 39 digits in decimal.
 * <li> Explicitly set USE_GET_GENERATED_KEYS property to false.
 * <li> Perform string casts to varchar; removes space padding.
 * </ul>
 * 
 * @author Ian Booth, Bruce Lunsford, Max Rydahl Andersen, Raymond Fan
 */
public class IngresDialect extends Dialect {

	public IngresDialect() {
		super();
		registerColumnType( Types.BIT, "tinyint" );
		registerColumnType( Types.TINYINT, "tinyint" );
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.INTEGER, "integer" );
		registerColumnType( Types.BIGINT, "bigint" );
		registerColumnType( Types.REAL, "real" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.DOUBLE, "float" );
		registerColumnType( Types.NUMERIC, "decimal($p, $s)" );
		registerColumnType( Types.DECIMAL, "decimal($p, $s)" );
		registerColumnType( Types.BINARY, 32000, "byte($l)" );
		registerColumnType( Types.BINARY, "long byte" );
		registerColumnType( Types.VARBINARY, 32000, "varbyte($l)" );
		registerColumnType( Types.VARBINARY, "long byte" );
		registerColumnType( Types.LONGVARBINARY, "long byte" );
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.VARCHAR, 32000, "varchar($l)" );
		registerColumnType( Types.VARCHAR, "long varchar" );
		registerColumnType( Types.LONGVARCHAR, "long varchar" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "time with time zone" );
		registerColumnType( Types.TIMESTAMP, "timestamp with time zone" );
		registerColumnType( Types.BLOB, "blob" );
		registerColumnType( Types.CLOB, "clob" );

		registerFunction( "abs", new StandardSQLFunction( "abs" ) );
		registerFunction( "atan", new StandardSQLFunction( "atan", Hibernate.DOUBLE ) );
		registerFunction( "bit_add", new StandardSQLFunction( "bit_add" ) );
		registerFunction( "bit_and", new StandardSQLFunction( "bit_and" ) );
		registerFunction( "bit_length", new SQLFunctionTemplate( Hibernate.INTEGER, "octet_length(hex(?1))*4" ) );
		registerFunction( "bit_not", new StandardSQLFunction( "bit_not" ) );
		registerFunction( "bit_or", new StandardSQLFunction( "bit_or" ) );
		registerFunction( "bit_xor", new StandardSQLFunction( "bit_xor" ) );
		registerFunction( "character_length", new StandardSQLFunction( "character_length", Hibernate.LONG ) );
		registerFunction( "charextract", new StandardSQLFunction( "charextract", Hibernate.STRING ) );
		registerFunction( "concat", new VarArgsSQLFunction( Hibernate.STRING, "(", "+", ")" ) );
		registerFunction( "cos", new StandardSQLFunction( "cos", Hibernate.DOUBLE ) );
		registerFunction( "current_user", new NoArgSQLFunction( "current_user", Hibernate.STRING, false ) );
		registerFunction( "current_time", new NoArgSQLFunction( "date('now')", Hibernate.TIMESTAMP, false ) );
		registerFunction( "current_timestamp", new NoArgSQLFunction( "date('now')", Hibernate.TIMESTAMP, false ) );
		registerFunction( "current_date", new NoArgSQLFunction( "date('now')", Hibernate.TIMESTAMP, false ) );
		registerFunction( "date_trunc", new StandardSQLFunction( "date_trunc", Hibernate.TIMESTAMP ) );
		registerFunction( "day", new StandardSQLFunction( "day", Hibernate.INTEGER ) );
		registerFunction( "dba", new NoArgSQLFunction( "dba", Hibernate.STRING, true ) );
		registerFunction( "dow", new StandardSQLFunction( "dow", Hibernate.STRING ) );
		registerFunction( "extract", new SQLFunctionTemplate( Hibernate.INTEGER, "date_part('?1', ?3)" ) );
		registerFunction( "exp", new StandardSQLFunction( "exp", Hibernate.DOUBLE ) );
		registerFunction( "gmt_timestamp", new StandardSQLFunction( "gmt_timestamp", Hibernate.STRING ) );
		registerFunction( "hash", new StandardSQLFunction( "hash", Hibernate.INTEGER ) );
		registerFunction( "hex", new StandardSQLFunction( "hex", Hibernate.STRING ) );
		registerFunction( "hour", new StandardSQLFunction( "hour", Hibernate.INTEGER ) );
		registerFunction( "initial_user", new NoArgSQLFunction( "initial_user", Hibernate.STRING, false ) );
		registerFunction( "intextract", new StandardSQLFunction( "intextract", Hibernate.INTEGER ) );
		registerFunction( "left", new StandardSQLFunction( "left", Hibernate.STRING ) );
		registerFunction( "locate", new SQLFunctionTemplate( Hibernate.LONG, "locate(?1, ?2)" ) );
		registerFunction( "length", new StandardSQLFunction( "length", Hibernate.LONG ) );
		registerFunction( "ln", new StandardSQLFunction( "ln", Hibernate.DOUBLE ) );
		registerFunction( "log", new StandardSQLFunction( "log", Hibernate.DOUBLE ) );
		registerFunction( "lower", new StandardSQLFunction( "lower" ) );
		registerFunction( "lowercase", new StandardSQLFunction( "lowercase" ) );
		registerFunction( "minute", new StandardSQLFunction( "minute", Hibernate.INTEGER ) );
		registerFunction( "month", new StandardSQLFunction( "month", Hibernate.INTEGER ) );
		registerFunction( "octet_length", new StandardSQLFunction( "octet_length", Hibernate.LONG ) );
		registerFunction( "pad", new StandardSQLFunction( "pad", Hibernate.STRING ) );
		registerFunction( "position", new StandardSQLFunction( "position", Hibernate.LONG ) );
		registerFunction( "power", new StandardSQLFunction( "power", Hibernate.DOUBLE ) );
		registerFunction( "random", new NoArgSQLFunction( "random", Hibernate.LONG, true ) );
		registerFunction( "randomf", new NoArgSQLFunction( "randomf", Hibernate.DOUBLE, true ) );
		registerFunction( "right", new StandardSQLFunction( "right", Hibernate.STRING ) );
		registerFunction( "session_user", new NoArgSQLFunction( "session_user", Hibernate.STRING, false ) );
		registerFunction( "second", new StandardSQLFunction( "second", Hibernate.INTEGER ) );
		registerFunction( "size", new NoArgSQLFunction( "size", Hibernate.LONG, true ) );
		registerFunction( "squeeze", new StandardSQLFunction( "squeeze" ) );
		registerFunction( "sin", new StandardSQLFunction( "sin", Hibernate.DOUBLE ) );
		registerFunction( "soundex", new StandardSQLFunction( "soundex", Hibernate.STRING ) );
		registerFunction( "sqrt", new StandardSQLFunction( "sqrt", Hibernate.DOUBLE ) );
		registerFunction( "substring", new SQLFunctionTemplate( Hibernate.STRING, "substring(?1 FROM ?2 FOR ?3)" ) );
		registerFunction( "system_user", new NoArgSQLFunction( "system_user", Hibernate.STRING, false ) );
		//registerFunction( "trim", new StandardSQLFunction( "trim", Hibernate.STRING ) );
		registerFunction( "unhex", new StandardSQLFunction( "unhex", Hibernate.STRING ) );
		registerFunction( "upper", new StandardSQLFunction( "upper" ) );
		registerFunction( "uppercase", new StandardSQLFunction( "uppercase" ) );
		registerFunction( "user", new NoArgSQLFunction( "user", Hibernate.STRING, false ) );
		registerFunction( "usercode", new NoArgSQLFunction( "usercode", Hibernate.STRING, true ) );
		registerFunction( "username", new NoArgSQLFunction( "username", Hibernate.STRING, true ) );
		registerFunction( "uuid_create", new StandardSQLFunction( "uuid_create", Hibernate.BYTE ) );
		registerFunction( "uuid_compare", new StandardSQLFunction( "uuid_compare", Hibernate.INTEGER ) );
		registerFunction( "uuid_from_char", new StandardSQLFunction( "uuid_from_char", Hibernate.BYTE ) );
		registerFunction( "uuid_to_char", new StandardSQLFunction( "uuid_to_char", Hibernate.STRING ) );
		registerFunction( "year", new StandardSQLFunction( "year", Hibernate.INTEGER ) );
		// Casting to char of numeric values introduces space padding up to the
		// maximum width of a value for that return type.  Casting to varchar
		// does not introduce space padding.
		registerFunction( "str", new SQLFunctionTemplate(Hibernate.STRING, "cast(?1 as varchar)") );
        // Ingres driver supports getGeneratedKeys but only in the following
        // form:
        // The Ingres DBMS returns only a single table key or a single object
        // key per insert statement. Ingres does not return table and object
        // keys for INSERT AS SELECT statements. Depending on the keys that are
        // produced by the statement executed, auto-generated key parameters in
        // execute(), executeUpdate(), and prepareStatement() methods are
        // ignored and getGeneratedKeys() returns a result-set containing no
        // rows, a single row with one column, or a single row with two columns.
        // Ingres JDBC Driver returns table and object keys as BINARY values.
        getDefaultProperties().setProperty(Environment.USE_GET_GENERATED_KEYS, "false");
        // There is no support for a native boolean type that accepts values
        // of true, false or unknown. Using the tinyint type requires
        // substitions of true and false.
        getDefaultProperties().setProperty(Environment.QUERY_SUBSTITUTIONS, "true=1,false=0");
	}

	/**
	 * Do we need to drop constraints before dropping tables in this dialect?
	 *
	 * @return boolean
	 */
	public boolean dropConstraints() {
		return false;
	}

	/**
	 * Does this dialect support <tt>FOR UPDATE OF</tt>, allowing
	 * particular rows to be locked?
	 *
	 * @return True (Ingres does support "for update of" syntax...)
	 */
	public boolean supportsForUpdateOf() {
		return true;
	}

	/**
	 * The syntax used to add a column to a table (optional).
	 */
	public String getAddColumnString() {
		return "add column";
	}

	/**
	 * The keyword used to specify a nullable column.
	 *
	 * @return String
	 */
	public String getNullColumnString() {
		return " with null";
	}

	/**
	 * Does this dialect support sequences?
	 *
	 * @return boolean
	 */
	public boolean supportsSequences() {
		return true;
	}

	/**
	 * The syntax that fetches the next value of a sequence, if sequences are supported.
	 *
	 * @param sequenceName the name of the sequence
	 *
	 * @return String
	 */
	public String getSequenceNextValString(String sequenceName) {
		return "select nextval for " + sequenceName;
	}

	public String getSelectSequenceNextValString(String sequenceName) {
		return sequenceName + ".nextval";
	}

	/**
	 * The syntax used to create a sequence, if sequences are supported.
	 *
	 * @param sequenceName the name of the sequence
	 *
	 * @return String
	 */
	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName;
	}

	/**
	 * The syntax used to drop a sequence, if sequences are supported.
	 *
	 * @param sequenceName the name of the sequence
	 *
	 * @return String
	 */
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName + " restrict";
	}

	/**
	 * A query used to find all sequences
	 */
	public String getQuerySequencesString() {
		return "select seq_name from iisequence";
	}

	/**
	 * The name of the SQL function that transforms a string to
	 * lowercase
	 *
	 * @return String
	 */
	public String getLowercaseFunction() {
		return "lowercase";
	}

	/**
	 * Does this <tt>Dialect</tt> have some kind of <tt>LIMIT</tt> syntax?
	 */
	public boolean supportsLimit() {
		return true;
	}

	/**
	 * Does this dialect support an offset?
	 */
	public boolean supportsLimitOffset() {
		return false;
	}

	/**
	 * Add a <tt>LIMIT</tt> clause to the given SQL <tt>SELECT</tt>
	 *
	 * @return the modified SQL
	 */
	public String getLimitString(String querySelect, int offset, int limit) {
		if ( offset > 0 ) {
			throw new UnsupportedOperationException( "query result offset is not supported" );
		}
		return new StringBuffer( querySelect.length() + 16 )
				.append( querySelect )
				.insert( 6, " first " + limit )
				.toString();
	}

	public boolean supportsVariableLimit() {
		return false;
	}

	/**
	 * Does the <tt>LIMIT</tt> clause take a "maximum" row number instead
	 * of a total number of returned rows?
	 */
	public boolean useMaxForLimit() {
		return true;
	}

	/**
	 * Ingres explicitly needs "unique not null", because "with null" is default
	 */
	public boolean supportsNotNullUnique() {
		return false;
	}

	/**
	 * Does this dialect support temporary tables?
	 */
	public boolean supportsTemporaryTables() {
		return true;
	}

	public String getCreateTemporaryTableString() {
		return "declare global temporary table";
	}

	public String getCreateTemporaryTablePostfix() {
		return "on commit preserve rows with norecovery";
	}

	public String generateTemporaryTableName(String baseTableName) {
		return "session." + super.generateTemporaryTableName( baseTableName );
	}


	/**
	 * Expression for current_timestamp
	 */
	public String getCurrentTimestampSQLFunctionName() {
		return "date(now)";
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public boolean supportsSubselectAsInPredicateLHS() {
		return false;
	}

	public boolean supportsEmptyInList() {
		return false;
	}

	public boolean supportsExpectedLobUsagePattern () {
		return false;
	}
}

