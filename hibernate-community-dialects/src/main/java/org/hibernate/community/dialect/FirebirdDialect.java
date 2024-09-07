/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.community.dialect.identity.FirebirdIdentityColumnSupport;
import org.hibernate.community.dialect.pagination.FirstSkipLimitHandler;
import org.hibernate.community.dialect.sequence.FirebirdSequenceSupport;
import org.hibernate.community.dialect.sequence.InterbaseSequenceSupport;
import org.hibernate.community.dialect.sequence.SequenceInformationExtractorFirebirdDatabaseImpl;
import org.hibernate.dialect.BooleanDecoder;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.NationalizationSupport;
import org.hibernate.dialect.TimeZoneSupport;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.OffsetFetchLimitHandler;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableKind;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Index;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.dialect.NullOrdering;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.internal.SequenceNameExtractorImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.internal.StandardIndexExporter;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.DateTimeUtils;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.internal.BinaryFloatDdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import jakarta.persistence.TemporalType;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.INTEGER;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsDate;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsLocalTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithMillis;

/**
 * An SQL dialect for Firebird 2.0 and above.
 *
 * @author Reha CENANI
 * @author Gavin King
 * @author Mark Rotteveel
 */
public class FirebirdDialect extends Dialect {

	private static final DatabaseVersion DEFAULT_VERSION = DatabaseVersion.make( 2, 5 );

	@SuppressWarnings("unused")
	public FirebirdDialect() {
		this( DEFAULT_VERSION );
	}

	public FirebirdDialect(DialectResolutionInfo info) {
		this( info.makeCopyOrDefault( DEFAULT_VERSION ) );
		registerKeywords( info );
	}

	// KNOWN LIMITATIONS:

	// * no support for format()
	// * (Firebird 3 and earlier) extremely low maximum decimal precision (18)
	//   making BigInteger/BigDecimal support useless
	// * can't select a parameter unless wrapped in a
	//   cast (not even when wrapped in a function call)

	public FirebirdDialect(DatabaseVersion version) {
		super( version );
	}

	@Override
	protected String columnType(int sqlTypeCode) {
		switch ( sqlTypeCode ) {
			//'boolean' type introduced in 3.0
			case BOOLEAN:
				return getVersion().isBefore( 3, 0 ) ? "smallint" : super.columnType( sqlTypeCode );
			case TINYINT:
				return "smallint";
			//no precision for 'timestamp' type
			case TIMESTAMP:
				return "timestamp";
			case TIME_WITH_TIMEZONE:
				return getVersion().isBefore( 4, 0 ) ? "time" : "time with time zone";
			case TIMESTAMP_WITH_TIMEZONE:
				return getVersion().isBefore( 4, 0 ) ? "timestamp" : "timestamp with time zone";
			case BINARY:
				return getVersion().isBefore( 4, 0 ) ? "char($l) character set octets" : super.columnType( sqlTypeCode );
			case VARBINARY:
				return getVersion().isBefore( 4, 0 ) ? "varchar($l) character set octets" : super.columnType( sqlTypeCode );
			case BLOB:
				return "blob sub_type binary";
			case CLOB:
			case NCLOB:
				return "blob sub_type text";
			default:
				return super.columnType( sqlTypeCode );
		}
	}

	@Override
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		if ( getVersion().isBefore( 4, 0 ) ) {
			//precision of a Firebird 3 and earlier 'float(p)' represents
			//decimal digits instead of binary digits
			ddlTypeRegistry.addDescriptor( new BinaryFloatDdlType( this ) );
		}

		// Note: according to the documentation, Firebird has
		// just two floating point types:
		// - single precision 'float' (32 bit), and
		// - 'double precision' (64 bit).
		// However, it turns out that Firebird actually supports
		// the ANSI types 'real', 'float(p)', 'double precision'.
		// So we don't override anything here.
	}

	@Override
	public int getMaxVarcharLength() {
		// Single byte character sets can be 32_765
		// characters, but assume use of UTF8
		return 8_191;
	}

	@Override
	public int getMaxVarbinaryLength() {
		return 32_765;
	}

	@Override
	public int getDefaultStatementBatchSize() {
		return 0;
	}

	@Override
	public TimeZoneSupport getTimeZoneSupport() {
		return getVersion().isSameOrAfter( 4, 0 ) ? TimeZoneSupport.NATIVE : TimeZoneSupport.NONE;
	}

	@Override
	public JdbcType resolveSqlTypeDescriptor(
			String columnTypeName,
			int jdbcTypeCode,
			int precision,
			int scale,
			JdbcTypeRegistry jdbcTypeRegistry) {
		if ( jdbcTypeCode == Types.BIT ) {
			return jdbcTypeRegistry.getDescriptor( Types.BOOLEAN );
		}
		return super.resolveSqlTypeDescriptor(
				columnTypeName,
				jdbcTypeCode,
				precision,
				scale,
				jdbcTypeRegistry
		);
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return getVersion().isBefore( 3, 0 )
				? Types.BIT
				: super.getPreferredSqlTypeCodeForBoolean();
	}

	@Override
	public int getFloatPrecision() {
		return getVersion().isBefore( 4, 0 )
				? 21 // -> 7 decimal digits (actually 24, but needed for Dialect#binaryToDecimalPrecision(int,size))
				: 24;
	}

	@Override
	public int getDefaultTimestampPrecision() {
		// Formally, Firebird has a (fixed) precision of 4 (100 microseconds),
		// but things like CURRENT_TIMESTAMP produce values with a maximum of 3, so report that
		return 3;
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		final BasicTypeRegistry basicTypeRegistry = functionContributions.getTypeConfiguration().getBasicTypeRegistry();
		final BasicType<byte[]> byteArrayType = basicTypeRegistry.resolve( StandardBasicTypes.BINARY );
		final BasicType<Integer> integerType = basicTypeRegistry.resolve( StandardBasicTypes.INTEGER );
		final BasicType<Short> shortType = basicTypeRegistry.resolve( StandardBasicTypes.SHORT );
		final BasicType<Double> doubleType = basicTypeRegistry.resolve( StandardBasicTypes.DOUBLE );
		final BasicType<Character> characterType = basicTypeRegistry.resolve( StandardBasicTypes.CHARACTER );

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);

		// Firebird needs an actual argument type for aggregates like SUM, AVG, MIN, MAX to determine the result type
		functionFactory.aggregates( this, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		// AVG by default uses the input type, so we possibly need to cast the argument type, hence a special function
		functionFactory.avg_castingNonDoubleArguments( this, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );

		functionFactory.concat_pipeOperator();
		functionFactory.cot();
		functionFactory.cosh();
		functionFactory.sinh();
		functionFactory.tanh();
		if ( getVersion().isSameOrAfter( 3, 0 ) ) {
			functionFactory.moreHyperbolic();
			functionFactory.stddevPopSamp();
			functionFactory.varPopSamp();
			functionFactory.covarPopSamp();
			functionFactory.corr();
			functionFactory.regrLinearRegressionAggregates();
		}
		functionFactory.log();
		functionFactory.log10();
		functionFactory.pi();
		functionFactory.rand();
		functionFactory.sinh();
		functionFactory.tanh();
		functionFactory.cosh();
		functionFactory.trunc();
		functionFactory.octetLength();
		functionFactory.bitLength();
		functionFactory.substringFromFor();
		functionFactory.overlay();
		functionFactory.position();
		functionFactory.reverse();
		functionFactory.bitandorxornot_binAndOrXorNot();
		functionFactory.leastGreatest_minMaxValue();

		SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();
		functionRegistry.registerBinaryTernaryPattern(
				"locate",
				integerType,
				"position(?1 in ?2)",
				"position(?1,?2,?3)",
				STRING, STRING, INTEGER,
				functionContributions.getTypeConfiguration()
		).setArgumentListSignature( "(pattern, string[, start])" );
		functionRegistry.namedDescriptorBuilder( "ascii_val" )
				.setExactArgumentCount( 1 )
				.setInvariantType( shortType )
				.register();
		functionRegistry.registerAlternateKey( "ascii", "ascii_val" );
		functionRegistry.namedDescriptorBuilder( "ascii_char" )
				.setExactArgumentCount( 1 )
				.setInvariantType( characterType )
				.register();
		functionRegistry.registerAlternateKey( "chr", "ascii_char" );
		functionRegistry.registerAlternateKey( "char", "ascii_char" );
		functionRegistry.registerPattern(
				"radians",
				"((?1)*pi()/180e0)",
				doubleType
		);
		functionRegistry.registerPattern(
				"degrees",
				"((?1)*180e0/pi())",
				doubleType
		);

		if ( getVersion().isSameOrAfter( 3 ) ) {
			functionFactory.windowFunctions();
			if ( getVersion().isSameOrAfter( 4, 0 ) ) {
				Arrays.asList( "md5", "sha1", "sha256", "sha512" )
						.forEach( hash -> functionRegistry.registerPattern(
								hash,
								"crypt_hash(?1 using " + hash + ")",
								byteArrayType
						) );
				functionRegistry.registerAlternateKey( "sha", "sha1" );
				functionRegistry.registerPattern(
						"crc32",
						"hash(?1 using crc32)",
						integerType
				);
			}
		}

		functionFactory.listagg_list( "varchar" );
	}

	@Override
	public String currentLocalTime() {
		if ( getTimeZoneSupport() == TimeZoneSupport.NATIVE ) {
			return "localtime";
		}
		else {
			return super.currentLocalTime();
		}
	}

	@Override
	public String currentLocalTimestamp() {
		if ( getTimeZoneSupport() == TimeZoneSupport.NATIVE ) {
			return "localtimestamp";
		}
		else {
			return super.currentLocalTimestamp();
		}
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new FirebirdSqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	@Override
	public boolean supportsTruncateWithCast(){
		return false;
	}

	/**
	 * Firebird 2.5 doesn't have a real {@link Types#BOOLEAN}
	 * type, so...
	 */
	@Override
	public String castPattern(CastType from, CastType to) {
		String result;
		switch ( to ) {
			case INTEGER:
			case LONG:
				result = BooleanDecoder.toInteger( from );
				if ( result != null ) {
					return result;
				}
				break;
			case BOOLEAN:
				result = BooleanDecoder.toBoolean( from );
				if ( result != null ) {
					return result;
				}
				break;
			case INTEGER_BOOLEAN:
				result = BooleanDecoder.toIntegerBoolean( from );
				if ( result != null ) {
					return result;
				}
				break;
			case YN_BOOLEAN:
				result = BooleanDecoder.toYesNoBoolean( from );
				if ( result != null ) {
					return result;
				}
				break;
			case TF_BOOLEAN:
				result = BooleanDecoder.toTrueFalseBoolean( from );
				if ( result != null ) {
					return result;
				}
				break;
			case STRING:
				result = BooleanDecoder.toString( from );
				if ( result != null ) {
					// trim converts to varchar to prevent padding with spaces
					return "trim(" + result + ")";
				}
				break;
		}
		return super.castPattern( from, to );
	}

	@Override
	public long getFractionalSecondPrecisionInNanos() {
		// Formally, Firebird can store values with 100 microsecond precision (100_000 nanoseconds).
		// However, some functions (e.g. CURRENT_TIMESTAMP) will only return values with millisecond precision
		// So, we report millisecond precision
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
				return "(" + super.extractPattern( unit ) + "+1)";
			case QUARTER:
				return "((extract(month from ?2)+2)/3)";
			default:
				return super.extractPattern( unit );
		}
	}

	@Override
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		switch ( unit ) {
			case NATIVE:
				return "dateadd((?2) millisecond to ?3)";
			case NANOSECOND:
				return "dateadd((?2)/1e6 millisecond to ?3)";
			case WEEK:
				return "dateadd((?2)*7 day to ?3)";
			case QUARTER:
				return "dateadd((?2)*3 month to ?3)";
			default:
				return "dateadd(?2 ?1 to ?3)";
		}
	}

	@Override
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		switch ( unit ) {
			case NATIVE:
				return "datediff(millisecond from ?2 to ?3)";
			case NANOSECOND:
				return "datediff(millisecond from ?2 to ?3)*1e6";
			case WEEK:
				return "datediff(day from ?2 to ?3)/7";
			case QUARTER:
				return "datediff(month from ?2 to ?3)/3";
			default:
				return "datediff(?1 from ?2 to ?3)";
		}
	}

	@Override
	public boolean supportsTemporalLiteralOffset() {
		return getVersion().isSameOrAfter( 4, 0 );
	}

	@Override
	public int getDefaultDecimalPrecision() {
		return getVersion().isBefore( 4, 0 ) ? 18 : 38;
	}

	@Override
	public String getAddColumnString() {
		return "add";
	}

	@Override
	public String getNoColumnsInsertString() {
		return "default values";
	}

	@Override
	public int getMaxAliasLength() {
		return getVersion().isBefore( 4, 0 ) ? 20 : 52;
	}

	@Override
	public int getMaxIdentifierLength() {
		return getVersion().isBefore( 4 ) ? 31 : 63;
	}

	public IdentifierHelper buildIdentifierHelper(
			IdentifierHelperBuilder builder,
			DatabaseMetaData dbMetaData) throws SQLException {
		// Any use of keywords as identifiers will result in token unknown error, so enable auto quote always
		builder.setAutoQuoteKeywords( true );
		builder.setAutoQuoteInitialUnderscore( true );

		// Additional reserved words
		// The Hibernate list of SQL:2003 reserved words doesn't contain all SQL:2003 reserved words,
		// and Firebird is finicky when it comes to reserved words
		if ( getVersion().isSameOrAfter( 3, 0 ) ) {
			builder.applyReservedWords(
					"AVG", "BOOLEAN", "CHARACTER_LENGTH", "CHAR_LENGTH", "CORR", "COUNT",
					"COVAR_POP", "COVAR_SAMP", "EXTRACT", "LOWER", "MAX", "MIN", "OCTET_LENGTH", "POSITION",
					"REGR_AVGX", "REGR_AVGY", "REGR_COUNT", "REGR_INTERCEPT", "REGR_R2", "REGR_SLOPE", "REGR_SXX",
					"REGR_SXY", "REGR_SYY", "STDDEV_POP", "STDDEV_SAMP", "SUM", "TRIM", "UPPER", "VAR_POP",
					"VAR_SAMP" );
		}
		else {
			builder.applyReservedWords(
					"AVG", "CHARACTER_LENGTH", "CHAR_LENGTH", "COUNT", "EXTRACT", "LOWER", "MAX", "MIN", "OCTET_LENGTH",
					"POSITION", "SUM", "TRIM", "UPPER" );
		}

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
		return getVersion().isSameOrAfter( 2, 0 );
	}

	@Override
	public boolean supportsLobValueChangePropagation() {
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
		// see https://www.firebirdsql.org/file/documentation/html/en/refdocs/fblangref25/firebird-25-language-reference.html#fblangref25-commons-in
		return 1500;
	}

	@Override
	public boolean supportsExistsInSelect() {
		return getVersion().isSameOrAfter( 3, 0 );
	}

	@Override
	public boolean supportsPartitionBy() {
		return getVersion().isSameOrAfter( 3, 0 );
	}

	@Override
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		//'boolean' type introduced in 3.0
		if ( getVersion().isBefore( 3 ) ) {
			appender.appendSql( bool ? '1' : '0' );
		}
		else {
			appender.appendSql( bool );
		}
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return getVersion().isBefore( 3, 0 )
				? super.getIdentityColumnSupport()
				: FirebirdIdentityColumnSupport.INSTANCE;
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		DatabaseVersion version = getVersion();
		if ( version.isSameOrAfter( 4 ) ) {
			return FirebirdSequenceSupport.INSTANCE;
		}
		else if ( version.isSame( 3 ) ) {
			return FirebirdSequenceSupport.FB3_INSTANCE;
		}
		else if ( version.isSame( 2 ) ) {
			return FirebirdSequenceSupport.LEGACY_INSTANCE;
		}
		else {
			return InterbaseSequenceSupport.INSTANCE;
		}
	}

	@Override
	public String getQuerySequencesString() {
		return getVersion().isBefore( 3, 0 )
				? "select rdb$generator_name from rdb$generators"
				// Note: Firebird 3 has an 'off by increment' bug (fixed in Firebird 4), see
				// http://tracker.firebirdsql.org/browse/CORE-6084
				: "select rdb$generator_name,rdb$initial_value,rdb$generator_increment from rdb$generators where coalesce(rdb$system_flag,0)=0";
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return getVersion().isBefore( 3, 0 )
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
		return getVersion().isBefore( 3, 0 )
				? FirstSkipLimitHandler.INSTANCE
				: OffsetFetchLimitHandler.INSTANCE;
	}

	@Override
	public String getSelectGUIDString() {
		return getVersion().isBefore( 2, 1 )
				? super.getSelectGUIDString()
				: "select uuid_to_char(gen_uuid()) from rdb$database";
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
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "select current_timestamp from rdb$database";
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public NullOrdering getNullOrdering() {
		return getVersion().isSameOrAfter( 2, 0 ) ? NullOrdering.SMALLEST : NullOrdering.LAST;
	}

	@Override
	public boolean supportsNullPrecedence() {
		return getVersion().isSameOrAfter( 1, 5 );
	}

	@Override
	public boolean supportsOffsetInSubquery() {
		return true;
	}

	@Override
	public boolean supportsFetchClause(FetchClauseType type) {
		return type == FetchClauseType.ROWS_ONLY && getVersion().isSameOrAfter( 3 );
	}

	@Override
	public boolean supportsValuesListForInsert() {
		return false;
	}

	@Override
	public boolean supportsWindowFunctions() {
		return getVersion().isSameOrAfter( 3, 0 );
	}

	@Override
	public boolean supportsLateral() {
		return getVersion().isSameOrAfter( 4, 0 );
	}

	@Override
	public NationalizationSupport getNationalizationSupport() {
		return NationalizationSupport.IMPLICIT;
	}

	@Override
	public boolean supportsDistinctFromPredicate() {
		return true;
	}

	@Override
	public boolean supportsRecursiveCTE() {
		// Since Firebird 2.1
		return true;
	}

	@Override
	protected boolean supportsPredicateAsExpression() {
		return getVersion().isSameOrAfter( 3 );
	}

	@Override
	public String generatedAs(String generatedAs) {
		return " generated always as (" + generatedAs + ")";
	}

	@Override
	public boolean hasDataTypeBeforeGeneratedAs() {
		// data type is optional
		return false;
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

	public void appendDateTimeLiteral(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( "date '" );
				appendAsDate( appender, temporalAccessor );
				appender.appendSql( '\'' );
				break;
			case TIME:
				appender.appendSql( "time '" );
				FirebirdDateTimeUtils.appendAsTime( appender, temporalAccessor, supportsTemporalLiteralOffset(), jdbcTimeZone );
				appender.appendSql( '\'' );
				break;
			case TIMESTAMP:
				appender.appendSql( "timestamp '" );
				FirebirdDateTimeUtils.appendAsTimestampWithMillis( appender, temporalAccessor, supportsTemporalLiteralOffset(), jdbcTimeZone );
				appender.appendSql( '\'' );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	public void appendDateTimeLiteral(SqlAppender appender, Date date, TemporalType precision, TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( "date '" );
				appendAsDate( appender, date );
				appender.appendSql( '\'' );
				break;
			case TIME:
				appender.appendSql( "time '" );
				appendAsLocalTime( appender, date );
				appender.appendSql( '\'' );
				break;
			case TIMESTAMP:
				appender.appendSql( "timestamp '" );
				appendAsTimestampWithMillis( appender, date, jdbcTimeZone );
				appender.appendSql( '\'' );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	public void appendDateTimeLiteral(
			SqlAppender appender,
			Calendar calendar,
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( "date '" );
				appendAsDate( appender, calendar );
				appender.appendSql( '\'' );
				break;
			case TIME:
				appender.appendSql( "time '" );
				appendAsLocalTime( appender, calendar );
				appender.appendSql( '\'' );
				break;
			case TIMESTAMP:
				appender.appendSql( "timestamp '" );
				appendAsTimestampWithMillis( appender, calendar, jdbcTimeZone );
				appender.appendSql( '\'' );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
		throw new UnsupportedOperationException( "format() function not supported on Firebird" );
	}

	@Override
	public void appendUUIDLiteral(SqlAppender appender, UUID literal) {
		appender.appendSql( "char_to_uuid('" );
		appender.appendSql( literal.toString() );
		appender.appendSql( "')" );
	}

	@Override
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}

	private static final Pattern FOREIGN_UNIQUE_OR_PRIMARY_KEY_PATTERN =
			Pattern.compile( "violation of .+? constraint \"([^\"]+)\"" );
	private static final Pattern CHECK_CONSTRAINT_PATTERN =
			Pattern.compile( "Operation violates CHECK constraint (.+?) on view or table" );

	private static final ViolatedConstraintNameExtractor EXTRACTOR = sqle -> {
		String message = sqle.getMessage();
		if ( message != null ) {
			Matcher foreignUniqueOrPrimaryKeyMatcher =
					FOREIGN_UNIQUE_OR_PRIMARY_KEY_PATTERN.matcher( message );
			if ( foreignUniqueOrPrimaryKeyMatcher.find() ) {
				return foreignUniqueOrPrimaryKeyMatcher.group( 1 );
			}

			Matcher checkConstraintMatcher = CHECK_CONSTRAINT_PATTERN.matcher( message );
			if ( checkConstraintMatcher.find() ) {
				return checkConstraintMatcher.group( 1 );
			}
		}
		return null;
	};

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );
			final String sqlExceptionMessage = sqlException.getMessage();
			//final String sqlState = JdbcExceptionHelper.extractSqlState( sqlException );

			// Some of the error codes will only surface in Jaybird 3 or higher, as older versions return less specific error codes first
			switch ( errorCode ) {
				case 335544336:
					// isc_deadlock (deadlock, note: not necessarily a deadlock, can also be an update conflict)
					if ( sqlExceptionMessage != null
							&& sqlExceptionMessage.contains( "update conflicts with concurrent update" ) ) {
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
					final String constraintName = getViolatedConstraintNameExtractor().extractConstraintName(
							sqlException );
					return new ConstraintViolationException( message, sqlException, sql, constraintName );
			}

			// Apply heuristics based on exception message
			String exceptionMessage = sqlException.getMessage();
			if ( exceptionMessage != null ) {
				if ( exceptionMessage.contains( "violation of " )
						|| exceptionMessage.contains( "violates CHECK constraint" ) ) {
					final String constraintName = getViolatedConstraintNameExtractor().extractConstraintName(
							sqlException );
					return new ConstraintViolationException( message, sqlException, sql, constraintName );
				}
			}

			return null;
		};
	}

	@Override
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(EntityMappingType entityDescriptor, RuntimeModelCreationContext runtimeModelCreationContext) {
		return getVersion().isBefore( 2,1  )
				? super.getFallbackSqmMutationStrategy( entityDescriptor, runtimeModelCreationContext )
				: new GlobalTemporaryTableMutationStrategy(
					TemporaryTable.createIdTable(
							entityDescriptor,
							name -> TemporaryTable.ID_TABLE_PREFIX + name,
							this,
							runtimeModelCreationContext
					),
					runtimeModelCreationContext.getSessionFactory()
				);
	}

	@Override
	public SqmMultiTableInsertStrategy getFallbackSqmInsertStrategy(EntityMappingType entityDescriptor, RuntimeModelCreationContext runtimeModelCreationContext) {
		return getVersion().isBefore( 2, 1 )
				? super.getFallbackSqmInsertStrategy( entityDescriptor, runtimeModelCreationContext )
				: new GlobalTemporaryTableInsertStrategy(
				TemporaryTable.createEntityTable(
						entityDescriptor,
						name -> TemporaryTable.ENTITY_TABLE_PREFIX + name,
						this,
						runtimeModelCreationContext
				),
				runtimeModelCreationContext.getSessionFactory()
		);
	}

	@Override
	public TemporaryTableKind getSupportedTemporaryTableKind() {
		return TemporaryTableKind.GLOBAL;
	}

	@Override
	public String getTemporaryTableCreateOptions() {
		return "on commit delete rows";
	}

	private final FirebirdIndexExporter indexExporter = new FirebirdIndexExporter( this );

	@Override
	public Exporter<Index> getIndexExporter() {
		return indexExporter;
	}

	private static class FirebirdIndexExporter extends StandardIndexExporter {

		public FirebirdIndexExporter(Dialect dialect) {
			super( dialect );
		}

		@Override
		public String[] getSqlCreateStrings(Index index, Metadata metadata, SqlStringGenerationContext context) {
			final String tableName = context.format( index.getTable().getQualifiedTableName() );
			final Dialect dialect = getDialect();
			final String indexNameForCreation = index.getQuotedName( dialect );
			// In firebird the index is only sortable on top-level, not per column, use the first column to decide
			final String sortOrder = index.getColumnOrderMap().getOrDefault( index.getColumns().get( 0 ), "asc" );
			final StringBuilder buf = new StringBuilder()
					// Although `create asc index` is valid, generate without (some tests check for a specific syntax prefix)
					.append( "desc".equalsIgnoreCase( sortOrder ) || "descending".equalsIgnoreCase( sortOrder ) ? "create desc index " : "create index " )
					.append( indexNameForCreation )
					.append( " on " )
					.append( tableName )
					.append( " (" );
			boolean first = true;
			for ( Column column : index.getColumns() ) {
				if ( first ) {
					first = false;
				}
				else {
					buf.append( ", " );
				}
				buf.append( ( column.getQuotedName( dialect ) ) );
			}
			buf.append( ')' );

			return new String[] { buf.toString() };
		}
	}

	private static final class FirebirdDateTimeUtils {

		// Default formatting of DateTimeUtils renders UTC as Z, while Firebird expects +00:00

		private static final DateTimeFormatter OFFSET_TIME = new DateTimeFormatterBuilder()
				.parseCaseInsensitive()
				.append( DateTimeUtils.DATE_TIME_FORMATTER_TIME )
				.parseLenient()
				.appendOffset( "+HH:MM", "+00:00" )
				.parseStrict()
				.toFormatter( Locale.ENGLISH );

		private static final DateTimeFormatter OFFSET_DATE_TIME_MILLIS = new DateTimeFormatterBuilder()
				.parseCaseInsensitive()
				.append( DateTimeUtils.DATE_TIME_FORMATTER_TIMESTAMP_WITH_MILLIS )
				.parseLenient()
				.appendOffset( "+HH:MM", "+00:00" )
				.parseStrict()
				.toFormatter( Locale.ENGLISH );

		private static void appendAsTime(
				SqlAppender appender,
				TemporalAccessor temporalAccessor,
				boolean supportsOffset,
				TimeZone jdbcTimeZone) {
			if ( supportsOffset && temporalAccessor.isSupported( ChronoField.OFFSET_SECONDS ) ) {
				OFFSET_TIME.formatTo( temporalAccessor, appender );
			}
			else {
				DateTimeUtils.appendAsTime( appender, temporalAccessor, supportsOffset, jdbcTimeZone );
			}
		}

		public static void appendAsTimestampWithMillis(
				SqlAppender appender,
				TemporalAccessor temporalAccessor,
				boolean supportsOffset,
				TimeZone jdbcTimeZone) {
			if ( supportsOffset && temporalAccessor.isSupported( ChronoField.OFFSET_SECONDS ) ) {
				OFFSET_DATE_TIME_MILLIS.formatTo( temporalAccessor, appender );
			}
			else if ( supportsOffset && temporalAccessor instanceof Instant ) {
				OFFSET_DATE_TIME_MILLIS.formatTo(
						( (Instant) temporalAccessor ).atZone( jdbcTimeZone.toZoneId() ),
						appender
				);
			}
			else {
				DateTimeUtils.appendAsTimestampWithMillis( appender, temporalAccessor, supportsOffset, jdbcTimeZone );
			}
		}

	}
}
