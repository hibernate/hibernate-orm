/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.identity.FirebirdIdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.OffsetFetchLimitHandler;
import org.hibernate.dialect.pagination.SkipFirstLimitHandler;
import org.hibernate.dialect.sequence.FirebirdSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.metamodel.model.relational.spi.Size;
import org.hibernate.naming.Identifier;
import org.hibernate.query.CastType;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.mutation.spi.SqmMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.idtable.GlobalTempTableExporter;
import org.hibernate.query.sqm.mutation.spi.idtable.GlobalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.spi.idtable.IdTable;
import org.hibernate.query.sqm.mutation.spi.idtable.IdTableSupport;
import org.hibernate.query.sqm.mutation.spi.idtable.StandardIdTableSupport;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorFirebirdDatabaseImpl;
import org.hibernate.tool.schema.extract.internal.SequenceNameExtractorImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.spi.StandardSpiBasicTypes;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hibernate.query.CastType.BOOLEAN;
import static org.hibernate.query.CastType.INTEGER;
import static org.hibernate.query.CastType.LONG;
import static org.hibernate.query.CastType.STRING;
import static org.hibernate.type.descriptor.internal.DateTimeUtils.formatAsTimestampWithMillis;

/**
 * An SQL dialect for Firebird 2.5 and above.
 *
 * @author Reha CENANI
 * @author Gavin King
 * @author Mark Rotteveel
 */
public class FirebirdDialect extends Dialect {

	private final int version;

	public int getVersion() {
		return version;
	}

	public FirebirdDialect() {
		this(250);
	}

	public FirebirdDialect(DialectResolutionInfo info) {
		this( info.getDatabaseMajorVersion() * 100 + info.getDatabaseMinorVersion() * 10 );
	}

	// KNOWN LIMITATIONS:

	// * no support for format()
	// * extremely low maximum decimal precision (18)
	//   making BigInteger/BigDecimal support useless
	// * can't select a parameter unless wrapped in a
	//   cast (not even when wrapped in a function call)

	public FirebirdDialect(int version) {
		super();
		this.version = version;

		registerColumnType( Types.BIT, 1, "smallint" );
		registerColumnType( Types.BIT, "smallint" );

		if ( getVersion() < 300 ) {
			//'boolean' type introduced in 3.0
			registerColumnType( Types.BOOLEAN, "smallint" );
		}

		registerColumnType( Types.TINYINT, "smallint" );

		//Firebird has a fixed-single-precision 'float'
		//type instead of a 'real' type together with
		//a variable-precision 'float(p)' type
		registerColumnType( Types.REAL, "float");
		registerColumnType( Types.FLOAT, "double precision");

		//no precision for 'timestamp' type
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		if ( getVersion() < 400 ) {
			registerColumnType( Types.TIMESTAMP_WITH_TIMEZONE, "timestamp" );
		}
		else {
			registerColumnType( Types.TIMESTAMP_WITH_TIMEZONE, "timestamp with time zone" );
		}
		registerColumnType( Types.VARCHAR, 8_191, "varchar($l)" );
		registerColumnType( Types.VARCHAR, "blob sub_type text" );

		registerColumnType( Types.BINARY, 32_767, "char($l) character set octets" );
		registerColumnType( Types.BINARY, "blob sub_type binary" );

		registerColumnType( Types.VARBINARY, 32_765, "varchar($l) character set octets" );
		registerColumnType( Types.VARBINARY, "blob sub_type binary" );

		registerColumnType( Types.BLOB, "blob sub_type binary" );
		registerColumnType( Types.CLOB, "blob sub_type text" );
		registerColumnType( Types.NCLOB, "blob sub_type text" ); // Firebird doesn't have NCLOB, but Jaybird emulates NCLOB support

		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, NO_BATCH );
		getDefaultProperties().setProperty( Environment.QUERY_LITERAL_RENDERING, "literal" );
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return getVersion() < 300
				? Types.BIT
				: super.getPreferredSqlTypeCodeForBoolean();
	}

	@Override
	public String getTypeName(int code, Size size) throws HibernateException {
		//precision of a Firebird 'float(p)' represents
		//decimal digits instead of binary digits
		return super.getTypeName( code, binaryToDecimalPrecision( code, size ) );
	}

	@Override
	public int getFloatPrecision() {
		return 21; // -> 7 decimal digits
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		CommonFunctionFactory.concat_pipeOperator( queryEngine );
		CommonFunctionFactory.cot( queryEngine );
		CommonFunctionFactory.log( queryEngine );
		CommonFunctionFactory.log10( queryEngine );
		CommonFunctionFactory.pi( queryEngine );
		CommonFunctionFactory.rand( queryEngine );
		CommonFunctionFactory.sinh( queryEngine );
		CommonFunctionFactory.tanh( queryEngine );
		CommonFunctionFactory.cosh( queryEngine );
		CommonFunctionFactory.trunc( queryEngine );
		CommonFunctionFactory.octetLength( queryEngine );
		CommonFunctionFactory.bitLength( queryEngine );
		CommonFunctionFactory.substringFromFor( queryEngine );
		CommonFunctionFactory.overlay( queryEngine );
		CommonFunctionFactory.position( queryEngine );
		CommonFunctionFactory.reverse( queryEngine );
		CommonFunctionFactory.bitandorxornot_binAndOrXorNot( queryEngine );
		CommonFunctionFactory.leastGreatest_minMaxValue( queryEngine );
		CommonFunctionFactory.stddevPopSamp( queryEngine );
		CommonFunctionFactory.varPopSamp( queryEngine );
		//TODO: lots more statistical functions

		//TODO: gen_uid() and friends, gen_id()

		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern(
				"locate",
				StandardSpiBasicTypes.INTEGER,
				"position(?1 in ?2)",
				"(position(?1 in substring(?2 from ?3)) + (?3) - 1)"
		).setArgumentListSignature("(pattern, string[, start])");
	}

	/**
	 * Firebird 2.5 doesn't have a real {@link java.sql.Types#BOOLEAN}
	 * type, so...
	 */
	@Override
	public String castPattern(CastType from, CastType to) {
		if ( to==BOOLEAN
				&& (from==LONG || from==INTEGER)) {
			return "(0<>?1)";
		}
		if ( getVersion() < 300 ) {
			if ( to==BOOLEAN && from==STRING ) {
//				return "iif(lower(?1) similar to 't|f|true|false', lower(?1) like 't%', null)";
				return "decode(lower(?1),'t',1,'f',0,'true',1,'false',0)";
			}
			if ( to==STRING && from==BOOLEAN ) {
				return "trim(decode(?1,0,'false','true'))";
			}
		}
		else {
			if ( from==BOOLEAN
					&& (to==LONG || to==INTEGER)) {
				return "decode(?1,false,0,true,1)";
			}
		}
		return super.castPattern( from, to );
	}

	@Override
	public long getFractionalSecondPrecisionInNanos() {
		return 1_000_000; //milliseconds
	}

	/**
	 * Firebird extract() function returns {@link TemporalUnit#DAY_OF_WEEK}
	 * numbered from 0 to 6, and {@link TemporalUnit#DAY_OF_YEAR} numbered
	 * for 0. This isn't consistent with what most other databases do, so
	 * here we adjust the result by generating {@code (extract(unit,arg)+1))}.
	 */
	@Override
	public String extractPattern(TemporalUnit unit) {
		switch ( unit ) {
			case DAY_OF_WEEK:
			case DAY_OF_YEAR:
				return "(" + super.extractPattern(unit) + "+1)";
			default:
				return super.extractPattern(unit);
		}
	}

	@Override
	public String timestampaddPattern(TemporalUnit unit, boolean timestamp) {
		switch (unit) {
			case NATIVE:
				return "dateadd((?2) millisecond to ?3)";
			case NANOSECOND:
				return "dateadd((?2)/1e6 millisecond to ?3)";
			case WEEK:
				return "dateadd((?2)*7 day to ?3)";
			case QUARTER:
				return "dateadd((?2)*4 month to ?3)";
			default:
				return "dateadd(?2 ?1 to ?3)";
		}
	}

	@Override
	public String timestampdiffPattern(TemporalUnit unit, boolean fromTimestamp, boolean toTimestamp) {
		switch (unit) {
			case NATIVE:
				return "datediff(millisecond from ?2 to ?3)";
			case NANOSECOND:
				return "datediff(millisecond from ?2 to ?3)*1e6";
			case WEEK:
				return "datediff(day from ?2 to ?3)/7";
			case QUARTER:
				return "datediff(month from ?2 to ?3)/4";
			default:
				return "datediff(?1 from ?2 to ?3)";
		}
	}

	@Override
	public int getDefaultDecimalPrecision() {
		//the extremely low maximum
		return 18;
	}

	@Override
	public String getAddColumnString() {
		return "add";
	}

	@Override
	public boolean supportsUnionAll() {
		return true;
	}

	@Override
	public String getNoColumnsInsertString() {
		return "default values";
	}

	@Override
	public int getMaxAliasLength() {
		return 20;
	}

	public IdentifierHelper buildIdentifierHelper(
			IdentifierHelperBuilder builder,
			DatabaseMetaData dbMetaData) throws SQLException {
		// Any use of keywords as identifiers will result in token unknown error, so enable auto quote always
		builder.setAutoQuoteKeywords( true );

		return super.buildIdentifierHelper( builder, dbMetaData );
	}

	@Override
	public boolean canCreateSchema() {
		return false;
	}

	@Override
	public String[] getCreateSchemaCommand(String schemaName) {
		throw new UnsupportedOperationException( "No create schema syntax supported by " + getClass().getName() );
	}

	@Override
	public String[] getDropSchemaCommand(String schemaName) {
		throw new UnsupportedOperationException( "No drop schema syntax supported by " + getClass().getName() );
	}

	@Override
	public boolean qualifyIndexName() {
		return false;

	}
	@Override
	public boolean supportsCommentOn() {
		return true;
	}

	@Override
	public boolean supportsEmptyInList() {
		return false;
	}

	@Override
	public boolean requiresCastingOfParametersInSelectClause() {
		return true;
	}

	@Override
	public boolean supportsLobValueChangePropogation() {
		// May need changes in Jaybird for this to work
		return false;
	}

	@Override
	public boolean supportsUnboundedLobLocatorMaterialization() {
		// Blob ids are only guaranteed to work in the same transaction
		return false;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		return false;
	}

	@Override
	public int getInExpressionCountLimit() {
		// see http://www.firebirdsql.org/file/documentation/reference_manuals/fblangref25-en/html/fblangref25-commons-predicates.html#fblangref25-commons-in
		return 1500;
	}

	@Override
	public boolean supportsTuplesInSubqueries() {
		return false;
	}

	@Override
	public boolean supportsExistsInSelect() {
		return getVersion() >= 300;
	}

	@Override
	public boolean supportsPartitionBy() {
		return getVersion() >= 300;
	}

	@Override
	public String toBooleanValueString(boolean bool) {
		if ( getVersion() < 300 ) {
			return super.toBooleanValueString( bool );
		}
		else {
			//'boolean' type introduced in 3.0
			return bool ? "true" : "false";
		}
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return getVersion() < 300
				? super.getIdentityColumnSupport()
				: new FirebirdIdentityColumnSupport();
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return getVersion() < 300
				? FirebirdSequenceSupport.LEGACY_INSTANCE
				: FirebirdSequenceSupport.INSTANCE;
	}

	@Override
	public String getQuerySequencesString() {
		return getVersion() < 300
				? "select rdb$generator_name from rdb$generators"
				// Note: currently has an 'off by increment' bug, see
				// http://tracker.firebirdsql.org/browse/CORE-6084
				// May need revision depending on the final solution
				// The second column might need to be changed to
				//   rdb$initial_value + rdb$generator_increment
				: "select rdb$generator_name, rdb$initial_value, rdb$generator_increment from rdb$generators";
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return getVersion() < 300
				? SequenceNameExtractorImpl.INSTANCE
				: SequenceInformationExtractorFirebirdDatabaseImpl.INSTANCE;
	}

	@Override
	public String getForUpdateString() {
		// locking only happens on fetch
		// ('for update' would force Firebird to return a single row per fetch)
		return " with lock";
	}

	@Override
	public LimitHandler getLimitHandler() {
		return version < 300
				? SkipFirstLimitHandler.INSTANCE
				: OffsetFetchLimitHandler.INSTANCE;
	}


	@Override
	public String getSelectGUIDString() {
		return "select uuid_to_char(gen_uuid()) " + getFromDual();
	}

	@Override
	public boolean supportsLockTimeouts() {
		// Lock timeouts are only supported when specified as part of the transaction
		return false;
	}

	@Override
	public boolean supportsOuterJoinForUpdate() {
		// "WITH LOCK can only be used with a top-level, single-table SELECT statement"
		// https://www.firebirdsql.org/file/documentation/reference_manuals/fblangref25-en/html/fblangref25-dml-select.html#fblangref25-dml-with-lock
		return false;
	}

	@Override
	public SqmMutationStrategy getDefaultIdTableStrategy() {
		return new GlobalTemporaryTableStrategy( generateIdTableSupport() );
	}

	private IdTableSupport generateIdTableSupport() {
		return new StandardIdTableSupport( generateIdTableExporter() ) {
			@Override
			protected Identifier determineIdTableName(Identifier baseName) {
				final Identifier name = super.determineIdTableName( baseName );
				return name.getText().length() > 31
						? new Identifier( name.getText().substring( 0, 31 ), false )
						: name;
			}
		};
	}

	private Exporter<IdTable> generateIdTableExporter() {
		return new GlobalTempTableExporter() {
			@Override
			public String getCreateOptions() {
				return "on commit delete rows";
			}
		};
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "select current_timestamp " + getFromDual();
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public String getFromDual() {
		return "from rdb$database";
	}

	@Override
	public String translateExtractField(TemporalUnit unit) {
		switch ( unit ) {
			case DAY_OF_MONTH: return "day";
			case DAY_OF_YEAR: return "yearday";
			case DAY_OF_WEEK: return "weekday";
			default: return super.translateExtractField( unit );
		}
	}

	@Override
	protected String formatAsTimestamp(Date date) {
		return formatAsTimestampWithMillis(date);
	}

	@Override
	protected String formatAsTimestamp(Calendar calendar) {
		return formatAsTimestampWithMillis(calendar);
	}

	@Override
	protected String formatAsTimestamp(TemporalAccessor temporalAccessor) {
		return formatAsTimestampWithMillis(temporalAccessor);
	}

	@Override
	public String translateDatetimeFormat(String format) {
		throw new NotYetImplementedFor6Exception("format() function not supported on Firebird");
	}

	@Override
	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
		return EXTRACTER;
	}

	private static final ViolatedConstraintNameExtracter EXTRACTER = new ViolatedConstraintNameExtracter() {

		final Pattern foreignUniqueOrPrimaryKeyPattern = Pattern.compile( "violation of .+? constraint \"([^\"]+)\"" );
		final Pattern checkConstraintPattern = Pattern.compile(
				"Operation violates CHECK constraint (.+?) on view or table" );

		@Override
		public String extractConstraintName(SQLException sqle) {
			String message = sqle.getMessage();
			if ( message != null ) {
				Matcher foreignUniqueOrPrimaryKeyMatcher =
						foreignUniqueOrPrimaryKeyPattern.matcher( message );
				if ( foreignUniqueOrPrimaryKeyMatcher.find() ) {
					return foreignUniqueOrPrimaryKeyMatcher.group( 1 );
				}

				Matcher checkConstraintMatcher = checkConstraintPattern.matcher( message );
				if ( checkConstraintMatcher.find() ) {
					return checkConstraintMatcher.group( 1 );
				}
			}
			return null;
		}
	};

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return new SQLExceptionConversionDelegate() {
			@Override
			public JDBCException convert(SQLException sqlException, String message, String sql) {
				final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );
				final String sqlExceptionMessage = sqlException.getMessage();
				//final String sqlState = JdbcExceptionHelper.extractSqlState( sqlException );

				// Some of the error codes will only surface in Jaybird 3 or higher, as older versions return less specific error codes first
				switch ( errorCode ) {
					case 335544336:
						// isc_deadlock (deadlock, note: not necessarily a deadlock, can also be an update conflict)
						if (sqlExceptionMessage != null
								&& sqlExceptionMessage.contains( "update conflicts with concurrent update" )) {
							return new LockTimeoutException( message, sqlException, sql );
						}
						return new LockAcquisitionException( message, sqlException, sql );
					case 335544345:
						// isc_lock_conflict (lock conflict on no wait transaction)
					case 335544510:
						// isc_lock_timeout (lock time-out on wait transaction)
						return new LockTimeoutException( message, sqlException, sql );
					case 335544474:
						// isc_bad_lock_level (invalid lock level {0})
					case 335544475:
						// isc_relation_lock (lock on table {0} conflicts with existing lock)
					case 335544476:
						// isc_record_lock (requested record lock conflicts with existing lock)
						return new LockAcquisitionException( message, sqlException, sql );
					case 335544466:
						// isc_foreign_key (violation of FOREIGN KEY constraint "{0}" on table "{1}")
					case 336396758:
						// *no error name* (violation of FOREIGN KEY constraint "{0}")
					case 335544558:
						// isc_check_constraint (Operation violates CHECK constraint {0} on view or table {1})
					case 336396991:
						// *no error name* (Operation violates CHECK constraint {0} on view or table)
					case 335544665:
						// isc_unique_key_violation (violation of PRIMARY or UNIQUE KEY constraint "{0}" on table "{1}")
						final String constraintName = getViolatedConstraintNameExtracter().extractConstraintName(
								sqlException );
						return new ConstraintViolationException( message, sqlException, sql, constraintName );
				}

				// Apply heuristics based on exception message
				String exceptionMessage = sqlException.getMessage();
				if ( exceptionMessage != null ) {
					if ( exceptionMessage.contains( "violation of " )
							|| exceptionMessage.contains( "violates CHECK constraint" ) ) {
						final String constraintName = getViolatedConstraintNameExtracter().extractConstraintName(
								sqlException );
						return new ConstraintViolationException( message, sqlException, sql, constraintName );
					}
				}

				return null;
			}
		};
	}

}
