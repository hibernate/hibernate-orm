/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.NoArgsSqmFunctionTemplate;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.query.sqm.produce.function.spi.NamedSqmFunctionTemplate;
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

		registerFunction( "abs", new NamedSqmFunctionTemplate( "abs" ) );
		registerFunction( "atan", new NamedSqmFunctionTemplate( "atan", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "bit_add", new NamedSqmFunctionTemplate( "bit_add" ) );
		registerFunction( "bit_and", new NamedSqmFunctionTemplate( "bit_and" ) );
		registerFunction( "bit_length", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "octet_length(hex(?1))*4" ) );
		registerFunction( "bit_not", new NamedSqmFunctionTemplate( "bit_not" ) );
		registerFunction( "bit_or", new NamedSqmFunctionTemplate( "bit_or" ) );
		registerFunction( "bit_xor", new NamedSqmFunctionTemplate( "bit_xor" ) );
		registerFunction( "character_length", new NamedSqmFunctionTemplate( "character_length", StandardSpiBasicTypes.LONG ) );
		registerFunction( "charextract", new NamedSqmFunctionTemplate( "charextract", StandardSpiBasicTypes.STRING ) );
		registerFunction( "concat", new VarArgsSQLFunction( StandardSpiBasicTypes.STRING, "(", "+", ")" ) );
		registerFunction( "cos", new NamedSqmFunctionTemplate( "cos", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "current_user", new NoArgsSqmFunctionTemplate( "current_user", StandardSpiBasicTypes.STRING, false ) );
		registerFunction( "current_time", new NoArgsSqmFunctionTemplate( "date('now')", StandardSpiBasicTypes.TIMESTAMP, false ) );
		registerFunction( "current_timestamp", new NoArgsSqmFunctionTemplate( "date('now')", StandardSpiBasicTypes.TIMESTAMP, false ) );
		registerFunction( "current_date", new NoArgsSqmFunctionTemplate( "date('now')", StandardSpiBasicTypes.TIMESTAMP, false ) );
		registerFunction( "date_trunc", new NamedSqmFunctionTemplate( "date_trunc", StandardSpiBasicTypes.TIMESTAMP ) );
		registerFunction( "day", new NamedSqmFunctionTemplate( "day", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "dba", new NoArgsSqmFunctionTemplate( "dba", StandardSpiBasicTypes.STRING, true ) );
		registerFunction( "dow", new NamedSqmFunctionTemplate( "dow", StandardSpiBasicTypes.STRING ) );
		registerFunction( "extract", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "date_part('?1', ?3)" ) );
		registerFunction( "exp", new NamedSqmFunctionTemplate( "exp", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "gmt_timestamp", new NamedSqmFunctionTemplate( "gmt_timestamp", StandardSpiBasicTypes.STRING ) );
		registerFunction( "hash", new NamedSqmFunctionTemplate( "hash", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "hex", new NamedSqmFunctionTemplate( "hex", StandardSpiBasicTypes.STRING ) );
		registerFunction( "hour", new NamedSqmFunctionTemplate( "hour", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "initial_user", new NoArgsSqmFunctionTemplate( "initial_user", StandardSpiBasicTypes.STRING, false ) );
		registerFunction( "intextract", new NamedSqmFunctionTemplate( "intextract", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "left", new NamedSqmFunctionTemplate( "left", StandardSpiBasicTypes.STRING ) );
		registerFunction( "locate", new SQLFunctionTemplate( StandardSpiBasicTypes.LONG, "locate(?1, ?2)" ) );
		registerFunction( "length", new NamedSqmFunctionTemplate( "length", StandardSpiBasicTypes.LONG ) );
		registerFunction( "ln", new NamedSqmFunctionTemplate( "ln", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "log", new NamedSqmFunctionTemplate( "log", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "lower", new NamedSqmFunctionTemplate( "lower" ) );
		registerFunction( "lowercase", new NamedSqmFunctionTemplate( "lowercase" ) );
		registerFunction( "minute", new NamedSqmFunctionTemplate( "minute", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "month", new NamedSqmFunctionTemplate( "month", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "octet_length", new NamedSqmFunctionTemplate( "octet_length", StandardSpiBasicTypes.LONG ) );
		registerFunction( "pad", new NamedSqmFunctionTemplate( "pad", StandardSpiBasicTypes.STRING ) );
		registerFunction( "position", new NamedSqmFunctionTemplate( "position", StandardSpiBasicTypes.LONG ) );
		registerFunction( "power", new NamedSqmFunctionTemplate( "power", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "random", new NoArgsSqmFunctionTemplate( "random", StandardSpiBasicTypes.LONG, true ) );
		registerFunction( "randomf", new NoArgsSqmFunctionTemplate( "randomf", StandardSpiBasicTypes.DOUBLE, true ) );
		registerFunction( "right", new NamedSqmFunctionTemplate( "right", StandardSpiBasicTypes.STRING ) );
		registerFunction( "session_user", new NoArgsSqmFunctionTemplate( "session_user", StandardSpiBasicTypes.STRING, false ) );
		registerFunction( "second", new NamedSqmFunctionTemplate( "second", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "size", new NoArgsSqmFunctionTemplate( "size", StandardSpiBasicTypes.LONG, true ) );
		registerFunction( "squeeze", new NamedSqmFunctionTemplate( "squeeze" ) );
		registerFunction( "sin", new NamedSqmFunctionTemplate( "sin", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "soundex", new NamedSqmFunctionTemplate( "soundex", StandardSpiBasicTypes.STRING ) );
		registerFunction( "sqrt", new NamedSqmFunctionTemplate( "sqrt", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "substring", new SQLFunctionTemplate( StandardSpiBasicTypes.STRING, "substring(?1 FROM ?2 FOR ?3)" ) );
		registerFunction( "system_user", new NoArgsSqmFunctionTemplate( "system_user", StandardSpiBasicTypes.STRING, false ) );
		//registerFunction( "trim", new StandardSQLFunction( "trim", StandardBasicTypes.STRING ) );
		registerFunction( "unhex", new NamedSqmFunctionTemplate( "unhex", StandardSpiBasicTypes.STRING ) );
		registerFunction( "upper", new NamedSqmFunctionTemplate( "upper" ) );
		registerFunction( "uppercase", new NamedSqmFunctionTemplate( "uppercase" ) );
		registerFunction( "user", new NoArgsSqmFunctionTemplate( "user", StandardSpiBasicTypes.STRING, false ) );
		registerFunction( "usercode", new NoArgsSqmFunctionTemplate( "usercode", StandardSpiBasicTypes.STRING, true ) );
		registerFunction( "username", new NoArgsSqmFunctionTemplate( "username", StandardSpiBasicTypes.STRING, true ) );
		registerFunction( "uuid_create", new NamedSqmFunctionTemplate( "uuid_create", StandardSpiBasicTypes.BYTE ) );
		registerFunction( "uuid_compare", new NamedSqmFunctionTemplate( "uuid_compare", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "uuid_from_char", new NamedSqmFunctionTemplate( "uuid_from_char", StandardSpiBasicTypes.BYTE ) );
		registerFunction( "uuid_to_char", new NamedSqmFunctionTemplate( "uuid_to_char", StandardSpiBasicTypes.STRING ) );
		registerFunction( "year", new NamedSqmFunctionTemplate( "year", StandardSpiBasicTypes.INTEGER ) );
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
