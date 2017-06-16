/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.IngresSubstringFunction;
import org.hibernate.dialect.function.LocateEmulationUsingPositionAndSubstring;
import org.hibernate.dialect.pagination.FirstLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.hql.spi.id.IdTableSupportStandardImpl;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.global.GlobalTemporaryTableBulkIdStrategy;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.AfterUseAction;
import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.spi.FunctionAsExpressionTemplate;
import org.hibernate.query.sqm.produce.function.spi.NamedSqmFunctionTemplate;
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
	public void initializeFunctionRegistry(SqmFunctionRegistry registry) {
		super.initializeFunctionRegistry( registry );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Standard function overrides

		registry.patternTemplateBuilder( "bit_length", "octet_length(hex(?1))*4" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.register();

		registry.register(
				"concat",
				new FunctionAsExpressionTemplate(
						"(",
						"+",
						")",
						StandardFunctionReturnTypeResolvers.invariant( StandardSpiBasicTypes.STRING )
				)
		);

		registry.namedTemplateBuilder( "day" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.register();

		registry.namedTemplateBuilder( "hour" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.register();

		registry.patternTemplateBuilder( "extract", "date_part('?1', ?2)" )
				.setExactArgumentCount( 2 )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.register();

		registry.register(
				"locate",
				new LocateEmulationUsingPositionAndSubstring(
						(type, arguments) -> IngresSubstringFunction.INSTANCE.makeSqmFunctionExpression(
								arguments,
								type
						)
				)
		);

		registry.namedTemplateBuilder( "minute" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.register();

		registry.namedTemplateBuilder( "month" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.register();

		registry.namedTemplateBuilder( "second" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.register();

		registry.register( "substring", IngresSubstringFunction.INSTANCE );

		registry.namedTemplateBuilder( "year" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.register();

		// Casting to char of numeric values introduces space padding up to the
		// maximum width of a value for that return type.  Casting to varchar
		// does not introduce space padding.
		registry.patternTemplateBuilder( "str", "cast(?1 as varchar)" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Common functions

		CommonFunctionFactory.atan( registry );
		CommonFunctionFactory.cos( registry );
		CommonFunctionFactory.exp( registry );
		CommonFunctionFactory.ln( registry );
		CommonFunctionFactory.log( registry );
		CommonFunctionFactory.position( registry );
		CommonFunctionFactory.sin( registry );
		CommonFunctionFactory.soundex( registry );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Connection/database info functions

		registry.noArgsBuilder( "current_user" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();

		registry.noArgsBuilder( "dba" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setUseParenthesesWhenNoArgs( true )
				.register();

		registry.registerNoArgs( "gmt_timestamp", StandardSpiBasicTypes.STRING );

		registry.registerNoArgs( "initial_user", StandardSpiBasicTypes.STRING );
		registry.registerNoArgs( "session_user", StandardSpiBasicTypes.STRING );
		registry.registerNoArgs( "system_user", StandardSpiBasicTypes.STRING );
		registry.registerNoArgs( "user", StandardSpiBasicTypes.STRING );
		registry.noArgsBuilder( "usercode" ).setInvariantType( StandardSpiBasicTypes.STRING ).setUseParenthesesWhenNoArgs( true ).register();
		registry.noArgsBuilder( "username" ).setInvariantType( StandardSpiBasicTypes.STRING ).setUseParenthesesWhenNoArgs( true ).register();


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// UUID functions
		registry.noArgsBuilder( "uuid_create" ).setUseParenthesesWhenNoArgs( true ).setInvariantType( StandardSpiBasicTypes.BYTE );

		registry.namedTemplateBuilder( "uuid_compare" )
				.setExactArgumentCount( 2 )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.register();

		registry.namedTemplateBuilder( "uuid_from_char" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.BYTE )
				.register();

		registry.namedTemplateBuilder( "uuid_to_char" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Bitwise functions

		registry.namedTemplateBuilder( "bit_and" )
				.setExactArgumentCount( 2 )
				.register();

		registry.namedTemplateBuilder( "bit_add" )
				.setExactArgumentCount( 2 )
				.register();

		registry.namedTemplateBuilder( "bit_or" )
				.setExactArgumentCount( 2 )
				.register();

		registry.namedTemplateBuilder( "bit_xor" )
				.setExactArgumentCount( 2 )
				.register();

		registry.namedTemplateBuilder( "bit_not" )
				.setExactArgumentCount( 1 )
				.register();


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// String functions

		registry.registerAlternateKey( "character_length", "length" );

		registry.namedTemplateBuilder( "charextract" )
				.setExactArgumentCount( 2 )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();

		registry.registerAlternateKey( "lowercase", "lower" );

		registry.registerAlternateKey( "uppercase", "upper" );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Date functions

		registry.namedTemplateBuilder( "date_trunc" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.register();

		registry.namedTemplateBuilder( "dow" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Numeric functions

		registry.namedTemplateBuilder( "power" )
				.setExactArgumentCount( 2 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();

		registry.noArgsBuilder( "random" )
				.setUseParenthesesWhenNoArgs( true )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.register();

		registry.noArgsBuilder( "randomf" )
				.setUseParenthesesWhenNoArgs( true )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();




		// uncategorized
		registerFunction( "hash", new NamedSqmFunctionTemplate( "hash", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "hex", new NamedSqmFunctionTemplate( "hex", StandardSpiBasicTypes.STRING ) );




		registerFunction( "intextract", new NamedSqmFunctionTemplate( "intextract", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "left", new NamedSqmFunctionTemplate( "left", StandardSpiBasicTypes.STRING ) );



		registerFunction( "octet_length", new NamedSqmFunctionTemplate( "octet_length", StandardSpiBasicTypes.LONG ) );
		registerFunction( "pad", new NamedSqmFunctionTemplate( "pad", StandardSpiBasicTypes.STRING ) );



		registerFunction( "right", new NamedSqmFunctionTemplate( "right", StandardSpiBasicTypes.STRING ) );



		registry.namedTemplateBuilder( "size" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.register();

		registerFunction( "squeeze", new NamedSqmFunctionTemplate( "squeeze" ) );

		registerFunction( "unhex", new NamedSqmFunctionTemplate( "unhex", StandardSpiBasicTypes.STRING ) );

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
