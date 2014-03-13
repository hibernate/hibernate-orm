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
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.dialect.pagination.FirstLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.type.StandardBasicTypes;

/**
 * An SQL dialect for Ingres 9.2.
 * <p/>
 * Known limitations: <ul>
 *     <li>
 *         Only supports simple constants or columns on the left side of an IN,
 *         making {@code (1,2,3) in (...)} or {@code (subselect) in (...)} non-supported.
 *     </li>
 *     <li>
 *         Supports only 39 digits in decimal.
 *     </li>
 *     <li>
 *         Explicitly set USE_GET_GENERATED_KEYS property to false.
 *     </li>
 *     <li>
 *         Perform string casts to varchar; removes space padding.
 *     </li>
 * </ul>
 * 
 * @author Ian Booth
 * @author Bruce Lunsford
 * @author Max Rydahl Andersen
 * @author Raymond Fan
 */
public class IngresDialect extends Dialect {
	/**
	 * Constructs a IngresDialect
	 */
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
		registerColumnType( Types.CHAR, 32000, "char($l)" );
		registerColumnType( Types.VARCHAR, 32000, "varchar($l)" );
		registerColumnType( Types.VARCHAR, "long varchar" );
		registerColumnType( Types.LONGVARCHAR, "long varchar" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "time with time zone" );
		registerColumnType( Types.TIMESTAMP, "timestamp with time zone" );
		registerColumnType( Types.BLOB, "blob" );
		registerColumnType( Types.CLOB, "clob" );

		registerFunction( "abs", new StandardSQLFunction( "abs" ) );
		registerFunction( "atan", new StandardSQLFunction( "atan", StandardBasicTypes.DOUBLE ) );
		registerFunction( "bit_add", new StandardSQLFunction( "bit_add" ) );
		registerFunction( "bit_and", new StandardSQLFunction( "bit_and" ) );
		registerFunction( "bit_length", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "octet_length(hex(?1))*4" ) );
		registerFunction( "bit_not", new StandardSQLFunction( "bit_not" ) );
		registerFunction( "bit_or", new StandardSQLFunction( "bit_or" ) );
		registerFunction( "bit_xor", new StandardSQLFunction( "bit_xor" ) );
		registerFunction( "character_length", new StandardSQLFunction( "character_length", StandardBasicTypes.LONG ) );
		registerFunction( "charextract", new StandardSQLFunction( "charextract", StandardBasicTypes.STRING ) );
		registerFunction( "concat", new VarArgsSQLFunction( StandardBasicTypes.STRING, "(", "+", ")" ) );
		registerFunction( "cos", new StandardSQLFunction( "cos", StandardBasicTypes.DOUBLE ) );
		registerFunction( "current_user", new NoArgSQLFunction( "current_user", StandardBasicTypes.STRING, false ) );
		registerFunction( "current_time", new NoArgSQLFunction( "date('now')", StandardBasicTypes.TIMESTAMP, false ) );
		registerFunction( "current_timestamp", new NoArgSQLFunction( "date('now')", StandardBasicTypes.TIMESTAMP, false ) );
		registerFunction( "current_date", new NoArgSQLFunction( "date('now')", StandardBasicTypes.TIMESTAMP, false ) );
		registerFunction( "date_trunc", new StandardSQLFunction( "date_trunc", StandardBasicTypes.TIMESTAMP ) );
		registerFunction( "day", new StandardSQLFunction( "day", StandardBasicTypes.INTEGER ) );
		registerFunction( "dba", new NoArgSQLFunction( "dba", StandardBasicTypes.STRING, true ) );
		registerFunction( "dow", new StandardSQLFunction( "dow", StandardBasicTypes.STRING ) );
		registerFunction( "extract", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "date_part('?1', ?3)" ) );
		registerFunction( "exp", new StandardSQLFunction( "exp", StandardBasicTypes.DOUBLE ) );
		registerFunction( "gmt_timestamp", new StandardSQLFunction( "gmt_timestamp", StandardBasicTypes.STRING ) );
		registerFunction( "hash", new StandardSQLFunction( "hash", StandardBasicTypes.INTEGER ) );
		registerFunction( "hex", new StandardSQLFunction( "hex", StandardBasicTypes.STRING ) );
		registerFunction( "hour", new StandardSQLFunction( "hour", StandardBasicTypes.INTEGER ) );
		registerFunction( "initial_user", new NoArgSQLFunction( "initial_user", StandardBasicTypes.STRING, false ) );
		registerFunction( "intextract", new StandardSQLFunction( "intextract", StandardBasicTypes.INTEGER ) );
		registerFunction( "left", new StandardSQLFunction( "left", StandardBasicTypes.STRING ) );
		registerFunction( "locate", new SQLFunctionTemplate( StandardBasicTypes.LONG, "locate(?1, ?2)" ) );
		registerFunction( "length", new StandardSQLFunction( "length", StandardBasicTypes.LONG ) );
		registerFunction( "ln", new StandardSQLFunction( "ln", StandardBasicTypes.DOUBLE ) );
		registerFunction( "log", new StandardSQLFunction( "log", StandardBasicTypes.DOUBLE ) );
		registerFunction( "lower", new StandardSQLFunction( "lower" ) );
		registerFunction( "lowercase", new StandardSQLFunction( "lowercase" ) );
		registerFunction( "minute", new StandardSQLFunction( "minute", StandardBasicTypes.INTEGER ) );
		registerFunction( "month", new StandardSQLFunction( "month", StandardBasicTypes.INTEGER ) );
		registerFunction( "octet_length", new StandardSQLFunction( "octet_length", StandardBasicTypes.LONG ) );
		registerFunction( "pad", new StandardSQLFunction( "pad", StandardBasicTypes.STRING ) );
		registerFunction( "position", new StandardSQLFunction( "position", StandardBasicTypes.LONG ) );
		registerFunction( "power", new StandardSQLFunction( "power", StandardBasicTypes.DOUBLE ) );
		registerFunction( "random", new NoArgSQLFunction( "random", StandardBasicTypes.LONG, true ) );
		registerFunction( "randomf", new NoArgSQLFunction( "randomf", StandardBasicTypes.DOUBLE, true ) );
		registerFunction( "right", new StandardSQLFunction( "right", StandardBasicTypes.STRING ) );
		registerFunction( "session_user", new NoArgSQLFunction( "session_user", StandardBasicTypes.STRING, false ) );
		registerFunction( "second", new StandardSQLFunction( "second", StandardBasicTypes.INTEGER ) );
		registerFunction( "size", new NoArgSQLFunction( "size", StandardBasicTypes.LONG, true ) );
		registerFunction( "squeeze", new StandardSQLFunction( "squeeze" ) );
		registerFunction( "sin", new StandardSQLFunction( "sin", StandardBasicTypes.DOUBLE ) );
		registerFunction( "soundex", new StandardSQLFunction( "soundex", StandardBasicTypes.STRING ) );
		registerFunction( "sqrt", new StandardSQLFunction( "sqrt", StandardBasicTypes.DOUBLE ) );
		registerFunction( "substring", new SQLFunctionTemplate( StandardBasicTypes.STRING, "substring(?1 FROM ?2 FOR ?3)" ) );
		registerFunction( "system_user", new NoArgSQLFunction( "system_user", StandardBasicTypes.STRING, false ) );
		//registerFunction( "trim", new StandardSQLFunction( "trim", StandardBasicTypes.STRING ) );
		registerFunction( "unhex", new StandardSQLFunction( "unhex", StandardBasicTypes.STRING ) );
		registerFunction( "upper", new StandardSQLFunction( "upper" ) );
		registerFunction( "uppercase", new StandardSQLFunction( "uppercase" ) );
		registerFunction( "user", new NoArgSQLFunction( "user", StandardBasicTypes.STRING, false ) );
		registerFunction( "usercode", new NoArgSQLFunction( "usercode", StandardBasicTypes.STRING, true ) );
		registerFunction( "username", new NoArgSQLFunction( "username", StandardBasicTypes.STRING, true ) );
		registerFunction( "uuid_create", new StandardSQLFunction( "uuid_create", StandardBasicTypes.BYTE ) );
		registerFunction( "uuid_compare", new StandardSQLFunction( "uuid_compare", StandardBasicTypes.INTEGER ) );
		registerFunction( "uuid_from_char", new StandardSQLFunction( "uuid_from_char", StandardBasicTypes.BYTE ) );
		registerFunction( "uuid_to_char", new StandardSQLFunction( "uuid_to_char", StandardBasicTypes.STRING ) );
		registerFunction( "year", new StandardSQLFunction( "year", StandardBasicTypes.INTEGER ) );
		// Casting to char of numeric values introduces space padding up to the
		// maximum width of a value for that return type.  Casting to varchar
		// does not introduce space padding.
		registerFunction( "str", new SQLFunctionTemplate(StandardBasicTypes.STRING, "cast(?1 as varchar)") );
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
		getDefaultProperties().setProperty( Environment.USE_GET_GENERATED_KEYS, "false" );
		// There is no support for a native boolean type that accepts values
		// of true, false or unknown. Using the tinyint type requires
		// substitions of true and false.
		getDefaultProperties().setProperty( Environment.QUERY_SUBSTITUTIONS, "true=1,false=0" );
	}

	@Override
	public String getSelectGUIDString() {
		return "select uuid_to_char(uuid_create())";
	}

	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public String getAddColumnString() {
		return "add column";
	}

	@Override
	public String getNullColumnString() {
		return " with null";
	}

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "select nextval for " + sequenceName;
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return sequenceName + ".nextval";
	}

	@Override
	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName + " restrict";
	}

	@Override
	public String getQuerySequencesString() {
		return "select seq_name from iisequence";
	}

	@Override
	public String getLowercaseFunction() {
		return "lowercase";
	}

	@Override
    public LimitHandler buildLimitHandler(String sql, RowSelection selection) {
        return new FirstLimitHandler(sql, selection);
    }

	@Override
	public boolean supportsTemporaryTables() {
		return true;
	}

	@Override
	public String getCreateTemporaryTableString() {
		return "declare global temporary table";
	}

	@Override
	public String getCreateTemporaryTablePostfix() {
		return "on commit preserve rows with norecovery";
	}

	@Override
	public String generateTemporaryTableName(String baseTableName) {
		return "session." + super.generateTemporaryTableName( baseTableName );
	}

	@Override
	public String getCurrentTimestampSQLFunctionName() {
		return "date(now)";
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsSubselectAsInPredicateLHS() {
		return false;
	}

	@Override
	public boolean supportsEmptyInList() {
		return false;
	}

	@Override
	public boolean supportsExpectedLobUsagePattern() {
		return false;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		return false;
	}
}

