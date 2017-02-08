/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.hql.spi.id.IdTableSupportStandardImpl;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.global.GlobalTemporaryTableBulkIdStrategy;
import org.hibernate.hql.spi.id.local.AfterUseAction;
import org.hibernate.type.spi.StandardSpiBasicTypes;

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
@SuppressWarnings("deprecation")
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
		registerFunction( "atan", new StandardSQLFunction( "atan", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "bit_add", new StandardSQLFunction( "bit_add" ) );
		registerFunction( "bit_and", new StandardSQLFunction( "bit_and" ) );
		registerFunction( "bit_length", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "octet_length(hex(?1))*4" ) );
		registerFunction( "bit_not", new StandardSQLFunction( "bit_not" ) );
		registerFunction( "bit_or", new StandardSQLFunction( "bit_or" ) );
		registerFunction( "bit_xor", new StandardSQLFunction( "bit_xor" ) );
		registerFunction( "character_length", new StandardSQLFunction( "character_length", StandardSpiBasicTypes.LONG ) );
		registerFunction( "charextract", new StandardSQLFunction( "charextract", StandardSpiBasicTypes.STRING ) );
		registerFunction( "concat", new VarArgsSQLFunction( StandardSpiBasicTypes.STRING, "(", "+", ")" ) );
		registerFunction( "cos", new StandardSQLFunction( "cos", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "current_user", new NoArgSQLFunction( "current_user", StandardSpiBasicTypes.STRING, false ) );
		registerFunction( "current_time", new NoArgSQLFunction( "date('now')", StandardSpiBasicTypes.TIMESTAMP, false ) );
		registerFunction( "current_timestamp", new NoArgSQLFunction( "date('now')", StandardSpiBasicTypes.TIMESTAMP, false ) );
		registerFunction( "current_date", new NoArgSQLFunction( "date('now')", StandardSpiBasicTypes.TIMESTAMP, false ) );
		registerFunction( "date_trunc", new StandardSQLFunction( "date_trunc", StandardSpiBasicTypes.TIMESTAMP ) );
		registerFunction( "day", new StandardSQLFunction( "day", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "dba", new NoArgSQLFunction( "dba", StandardSpiBasicTypes.STRING, true ) );
		registerFunction( "dow", new StandardSQLFunction( "dow", StandardSpiBasicTypes.STRING ) );
		registerFunction( "extract", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "date_part('?1', ?3)" ) );
		registerFunction( "exp", new StandardSQLFunction( "exp", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "gmt_timestamp", new StandardSQLFunction( "gmt_timestamp", StandardSpiBasicTypes.STRING ) );
		registerFunction( "hash", new StandardSQLFunction( "hash", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "hex", new StandardSQLFunction( "hex", StandardSpiBasicTypes.STRING ) );
		registerFunction( "hour", new StandardSQLFunction( "hour", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "initial_user", new NoArgSQLFunction( "initial_user", StandardSpiBasicTypes.STRING, false ) );
		registerFunction( "intextract", new StandardSQLFunction( "intextract", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "left", new StandardSQLFunction( "left", StandardSpiBasicTypes.STRING ) );
		registerFunction( "locate", new SQLFunctionTemplate( StandardSpiBasicTypes.LONG, "locate(?1, ?2)" ) );
		registerFunction( "length", new StandardSQLFunction( "length", StandardSpiBasicTypes.LONG ) );
		registerFunction( "ln", new StandardSQLFunction( "ln", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "log", new StandardSQLFunction( "log", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "lower", new StandardSQLFunction( "lower" ) );
		registerFunction( "lowercase", new StandardSQLFunction( "lowercase" ) );
		registerFunction( "minute", new StandardSQLFunction( "minute", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "month", new StandardSQLFunction( "month", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "octet_length", new StandardSQLFunction( "octet_length", StandardSpiBasicTypes.LONG ) );
		registerFunction( "pad", new StandardSQLFunction( "pad", StandardSpiBasicTypes.STRING ) );
		registerFunction( "position", new StandardSQLFunction( "position", StandardSpiBasicTypes.LONG ) );
		registerFunction( "power", new StandardSQLFunction( "power", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "random", new NoArgSQLFunction( "random", StandardSpiBasicTypes.LONG, true ) );
		registerFunction( "randomf", new NoArgSQLFunction( "randomf", StandardSpiBasicTypes.DOUBLE, true ) );
		registerFunction( "right", new StandardSQLFunction( "right", StandardSpiBasicTypes.STRING ) );
		registerFunction( "session_user", new NoArgSQLFunction( "session_user", StandardSpiBasicTypes.STRING, false ) );
		registerFunction( "second", new StandardSQLFunction( "second", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "size", new NoArgSQLFunction( "size", StandardSpiBasicTypes.LONG, true ) );
		registerFunction( "squeeze", new StandardSQLFunction( "squeeze" ) );
		registerFunction( "sin", new StandardSQLFunction( "sin", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "soundex", new StandardSQLFunction( "soundex", StandardSpiBasicTypes.STRING ) );
		registerFunction( "sqrt", new StandardSQLFunction( "sqrt", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "substring", new SQLFunctionTemplate( StandardSpiBasicTypes.STRING, "substring(?1 FROM ?2 FOR ?3)" ) );
		registerFunction( "system_user", new NoArgSQLFunction( "system_user", StandardSpiBasicTypes.STRING, false ) );
		//registerFunction( "trim", new StandardSQLFunction( "trim", StandardBasicTypes.STRING ) );
		registerFunction( "unhex", new StandardSQLFunction( "unhex", StandardSpiBasicTypes.STRING ) );
		registerFunction( "upper", new StandardSQLFunction( "upper" ) );
		registerFunction( "uppercase", new StandardSQLFunction( "uppercase" ) );
		registerFunction( "user", new NoArgSQLFunction( "user", StandardSpiBasicTypes.STRING, false ) );
		registerFunction( "usercode", new NoArgSQLFunction( "usercode", StandardSpiBasicTypes.STRING, true ) );
		registerFunction( "username", new NoArgSQLFunction( "username", StandardSpiBasicTypes.STRING, true ) );
		registerFunction( "uuid_create", new StandardSQLFunction( "uuid_create", StandardSpiBasicTypes.BYTE ) );
		registerFunction( "uuid_compare", new StandardSQLFunction( "uuid_compare", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "uuid_from_char", new StandardSQLFunction( "uuid_from_char", StandardSpiBasicTypes.BYTE ) );
		registerFunction( "uuid_to_char", new StandardSQLFunction( "uuid_to_char", StandardSpiBasicTypes.STRING ) );
		registerFunction( "year", new StandardSQLFunction( "year", StandardSpiBasicTypes.INTEGER ) );
		// Casting to char of numeric values introduces space padding up to the
		// maximum width of a value for that return type.  Casting to varchar
		// does not introduce space padding.
		registerFunction( "str", new SQLFunctionTemplate( StandardSpiBasicTypes.STRING, "cast(?1 as varchar)") );
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
	public LimitHandler getLimitHandler() {
		return FirstLimitHandler.INSTANCE;
	}

	@Override
	public boolean supportsLimit() {
		return true;
	}

	@Override
	public boolean supportsLimitOffset() {
		return false;
	}

	@Override
	public String getLimitString(String querySelect, int offset, int limit) {
		if ( offset > 0 ) {
			throw new UnsupportedOperationException( "query result offset is not supported" );
		}
		return new StringBuilder( querySelect.length() + 16 )
				.append( querySelect )
				.insert( 6, " first " + limit )
				.toString();
	}

	@Override
	public boolean supportsVariableLimit() {
		return false;
	}

	@Override
	public boolean useMaxForLimit() {
		return true;
	}

	@Override
	public MultiTableBulkIdStrategy getDefaultMultiTableBulkIdStrategy() {
		return new GlobalTemporaryTableBulkIdStrategy(
				new IdTableSupportStandardImpl() {
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
						return "on commit preserve rows with norecovery";
					}
				},
				AfterUseAction.CLEAN
		);
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
