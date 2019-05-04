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
import org.hibernate.dialect.function.LocateEmulation;
import org.hibernate.dialect.pagination.FirstLimitHandler;
import org.hibernate.dialect.pagination.LegacyFirstLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.naming.Identifier;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.mutation.spi.idtable.StandardIdTableSupport;
import org.hibernate.query.sqm.mutation.spi.SqmMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.idtable.GlobalTempTableExporter;
import org.hibernate.query.sqm.mutation.spi.idtable.GlobalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.spi.idtable.IdTable;
import org.hibernate.query.sqm.mutation.spi.idtable.IdTableSupport;
import org.hibernate.tool.schema.extract.internal.SequenceNameExtractorImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.spi.Exporter;
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
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Standard function overrides

		queryEngine.getSqmFunctionRegistry().registerPattern( "bit_length", "octet_length(hex(?1))*4", StandardSpiBasicTypes.INTEGER );

		queryEngine.getSqmFunctionRegistry().registerVarArgs( "concat", StandardSpiBasicTypes.STRING, "(", "+", ")" );

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "day" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "hour" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.register();

		queryEngine.getSqmFunctionRegistry().registerPattern( "extract", "date_part('?1', ?2)", StandardSpiBasicTypes.INTEGER );

		queryEngine.getSqmFunctionRegistry().register(
				"substring",
				new LocateEmulation(
						queryEngine.getSqmFunctionRegistry()
								.patternTemplateBuilder( "substring/2", "substring(?1 from ?2)" )
								.setExactArgumentCount( 2 )
								.setInvariantType( StandardSpiBasicTypes.INTEGER )
								.register(),
						queryEngine.getSqmFunctionRegistry()
								.patternTemplateBuilder( "substring/3", "substring(?1 from ?2 for ?3)" )
								.setExactArgumentCount( 3 )
								.setInvariantType( StandardSpiBasicTypes.INTEGER )
								.register()
				)
		);

		queryEngine.getSqmFunctionRegistry().register(
				"locate",
				new LocateEmulation(
						queryEngine.getSqmFunctionRegistry()
								.patternTemplateBuilder( "locate/2", "position(?1 in ?2)" )
								.setExactArgumentCount( 2 )
								.setInvariantType( StandardSpiBasicTypes.INTEGER )
								.register(),
						queryEngine.getSqmFunctionRegistry()
								.patternTemplateBuilder( "locate/3", "(position(?1 in substring(?2 from ?3)) + (?3) - 1)" )
								.setExactArgumentCount( 3 )
								.setInvariantType( StandardSpiBasicTypes.INTEGER )
								.register()
				)
		);

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "minute" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "month" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "second" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "year" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.register();

		// Casting to char of numeric values introduces space padding up to the
		// maximum width of a value for that return type.  Casting to varchar
		// does not introduce space padding.
		queryEngine.getSqmFunctionRegistry().registerPattern( "str", "cast(?1 as varchar)", StandardSpiBasicTypes.STRING );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Common functions

		CommonFunctionFactory.atan( queryEngine );
		CommonFunctionFactory.cos( queryEngine );
		CommonFunctionFactory.exp( queryEngine );
		CommonFunctionFactory.ln( queryEngine );
		CommonFunctionFactory.log( queryEngine );
		CommonFunctionFactory.sin( queryEngine );
		CommonFunctionFactory.soundex( queryEngine );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Connection/database info functions

		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "current_user" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();

		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "dba" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setUseParenthesesWhenNoArgs( true )
				.register();

		queryEngine.getSqmFunctionRegistry().registerNoArgs( "gmt_timestamp", StandardSpiBasicTypes.STRING );

		queryEngine.getSqmFunctionRegistry().registerNoArgs( "initial_user", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "session_user", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "system_user", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "user", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "usercode" ).setInvariantType( StandardSpiBasicTypes.STRING ).setUseParenthesesWhenNoArgs( true ).register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "username" ).setInvariantType( StandardSpiBasicTypes.STRING ).setUseParenthesesWhenNoArgs( true ).register();


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// UUID functions
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "uuid_create" ).setUseParenthesesWhenNoArgs( true ).setInvariantType( StandardSpiBasicTypes.BYTE );

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "uuid_compare" )
				.setExactArgumentCount( 2 )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "uuid_from_char" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.BYTE )
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "uuid_to_char" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Bitwise functions

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "bit_and" )
				.setExactArgumentCount( 2 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "bit_add" )
				.setExactArgumentCount( 2 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "bit_or" )
				.setExactArgumentCount( 2 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "bit_xor" )
				.setExactArgumentCount( 2 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "bit_not" )
				.setExactArgumentCount( 1 )
				.register();


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// String functions

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "charextract" )
				.setExactArgumentCount( 2 )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();

		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "lowercase", "lower" );

		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "uppercase", "upper" );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Date functions

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "date_trunc" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "dow" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Numeric functions

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "power" )
				.setExactArgumentCount( 2 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();

		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "random" )
				.setUseParenthesesWhenNoArgs( true )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.register();

		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "randomf" )
				.setUseParenthesesWhenNoArgs( true )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();




		// uncategorized
		queryEngine.getSqmFunctionRegistry().registerNamed( "hash", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "hex", StandardSpiBasicTypes.STRING );




		queryEngine.getSqmFunctionRegistry().registerNamed( "intextract", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "left", StandardSpiBasicTypes.STRING );



		queryEngine.getSqmFunctionRegistry().registerNamed( "octet_length", StandardSpiBasicTypes.LONG );
		queryEngine.getSqmFunctionRegistry().registerNamed( "pad", StandardSpiBasicTypes.STRING );



		queryEngine.getSqmFunctionRegistry().registerNamed( "right", StandardSpiBasicTypes.STRING );



		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "size" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.register();

		queryEngine.getSqmFunctionRegistry().registerNamed( "squeeze" );

		queryEngine.getSqmFunctionRegistry().registerNamed( "unhex", StandardSpiBasicTypes.STRING );

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
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceNameExtractorImpl.INSTANCE;
	}

	@Override
	public String getLowercaseFunction() {
		return "lowercase";
	}

	@Override
	public LimitHandler getLimitHandler() {
		if ( isLegacyLimitHandlerBehaviorEnabled() ) {
			return LegacyFirstLimitHandler.INSTANCE;
		}
		return getDefaultLimitHandler();
	}

	protected LimitHandler getDefaultLimitHandler() {
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
	public SqmMutationStrategy getDefaultIdTableStrategy() {
		return new GlobalTemporaryTableStrategy( generateIdTableSupport() );
	}

	private IdTableSupport generateIdTableSupport() {
		return new StandardIdTableSupport( generateIdTableExporter() ) {
			@Override
			protected Identifier determineIdTableName(Identifier baseName) {
				return new Identifier( "session." + super.determineIdTableName( baseName ).getText(), false );
			}
		};
	}

	private Exporter<IdTable> generateIdTableExporter() {
		return new GlobalTempTableExporter() {
			@Override
			public String getCreateCommand() {
				return "declare global temporary table";
			}

			@Override
			public String getCreateOptions() {
				return "on commit preserve rows with norecovery";
			}
		};
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
