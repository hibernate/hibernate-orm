/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.temporaltype.spi.TemporalValueSemantics;

import org.hibernate.dialect.temporaltype.spi.CurrentTimestampSelection;

import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupport;
import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupports;

import org.hibernate.dialect.temporaltype.spi.TemporalFormatSupport;

import org.hibernate.dialect.temporaltype.spi.CurrentTemporalSupport;

import org.hibernate.SPI;
import static org.hibernate.SPI.Role.USE;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import org.hibernate.dialect.type.spi.StandardDdlTypes;

import org.hibernate.dialect.type.spi.TypeSizingProfile;

import org.hibernate.dialect.mutation.spi.MultiTableMutationSupport;
import org.hibernate.dialect.function.spi.TupleCountSupport;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;

import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.NullOrderingSupport;
import org.hibernate.dialect.sql.ast.spi.PredicateSupport;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.SingleRowTableSupport;
import org.hibernate.dialect.sql.ast.spi.SetOperationSupport;
import org.hibernate.dialect.sql.ast.spi.ValuesListSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;

import jakarta.persistence.TemporalType;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.community.dialect.identity.internal.FirebirdIdentityColumnSupport;
import org.hibernate.community.dialect.lock.internal.FirebirdLockingSupport;
import org.hibernate.community.dialect.pagination.FirstSkipLimitHandler;
import org.hibernate.community.dialect.sequence.FirebirdSequenceSupport;
import org.hibernate.community.dialect.sequence.InterbaseSequenceSupport;
import org.hibernate.dialect.type.spi.BooleanDecoder;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.jdbc.spi.ParameterLimits;
import org.hibernate.dialect.sql.ast.spi.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.type.spi.NationalizationSupport;
import org.hibernate.dialect.sql.ast.spi.NullOrdering;
import org.hibernate.dialect.type.spi.TimeZoneSupport;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.HypotheticalSetWindowEmulation;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.namespace.spi.NamespaceSupport;
import org.hibernate.dialect.namespace.spi.NamespaceSupports;
import org.hibernate.dialect.schema.spi.ColumnDefinitionRequest;
import org.hibernate.dialect.schema.spi.IndexNameQualification;
import org.hibernate.dialect.schema.spi.TruncateRequest;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.OffsetFetchLimitHandler;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.temptable.spi.StandardGlobalTemporaryTableStrategy;
import org.hibernate.dialect.temptable.spi.TemporaryTableStrategy;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.dialect.identifier.spi.IdentifierHelperBuildRequest;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.jdbc.spi.JdbcExceptionHelper;
import org.hibernate.mapping.Index;
import org.hibernate.dialect.sql.ast.spi.FetchClauseSupport;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.dialect.temporaltype.spi.IntervalType;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractors;
import org.hibernate.tool.schema.spi.StandardIndexExporter;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.Types;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.INTEGER;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsDate;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsLocalTime;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTime;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTimestampWithMillis;
import static org.hibernate.dialect.literal.spi.ZeroOffsetLiteralStyle.NUMERIC_OFFSET;

/**
 * An SQL dialect for Firebird 2.0 and above.
 *
 * @author Reha CENANI
 * @author Gavin King
 * @author Mark Rotteveel
 */
public class FirebirdDialect extends Dialect implements CurrentTemporalSupport, TemporalFormatSupport, TemporalOperationSupport {

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalOperationSupport getTemporalOperationSupport() {
		return this;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalFormatSupport getTemporalFormatSupport() {
		return this;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public CurrentTemporalSupport getCurrentTemporalSupport() {
		return this;
	}
	private final TypeSizingProfile typeSizingProfile = TypeSizingProfile.builder( super.getTypeSizingProfile() )
			.defaultDecimalPrecision( getVersion().isBefore( 4, 0 ) ? 18 : 38 )
			.defaultTimestampPrecision( 3 )
			.floatPrecision( getVersion().isBefore( 4, 0 ) ? 21 : 24 )
			.maxVarcharLength( 8191 ).maxVarcharCapacity( 8191 )
			.maxNVarcharLength( 8191 ).maxNVarcharCapacity( 8191 )
			.maxVarbinaryLength( 32_765 ).maxVarbinaryCapacity( 32_765 )
			.build();

	@Override public TypeSizingProfile getTypeSizingProfile() { return typeSizingProfile; }

	private static final DatabaseVersion DEFAULT_VERSION = DatabaseVersion.make( 3 );
	private final LockingSupport lockingSupport;

	@SuppressWarnings("unused")
	public FirebirdDialect() {
		this( DEFAULT_VERSION );
	}

	public FirebirdDialect(DialectResolutionInfo info) {
		this( info.makeCopyOrDefault( DEFAULT_VERSION ) );
	}

	// KNOWN LIMITATIONS:

	// * no support for format()
	// * (Firebird 3 and earlier) extremely low maximum decimal precision (18)
	//   making BigInteger/BigDecimal support useless
	// * can't select a parameter unless wrapped in a
	//   cast (not even when wrapped in a function call)

	public FirebirdDialect(DatabaseVersion version) {
		super( version );
		lockingSupport = new FirebirdLockingSupport( version );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		return switch ( sqlTypeCode ) {
			//'boolean' type introduced in 3.0
			case BOOLEAN -> getVersion().isBefore( 3 ) ? "smallint" : "boolean";
			case TINYINT -> "smallint";
			//no precision for 'timestamp' type
			case TIMESTAMP -> "timestamp";
			//no precision for 'time' type
			case TIME -> "time";
			//no precision for 'time with time zone' type
			case TIME_WITH_TIMEZONE -> getVersion().isBefore( 4 ) ? "time" : "time with time zone";
			//no precision for 'timestamp with time zone' type
			case TIMESTAMP_WITH_TIMEZONE -> getVersion().isBefore( 4 ) ? "timestamp" : "timestamp with time zone";
			case BINARY -> getVersion().isBefore( 4 ) ? "char($l) character set octets" : "binary($l)";
			case VARBINARY -> getVersion().isBefore( 4 ) ? "varchar($l) character set octets" : "varbinary($l)";
			case BLOB -> "blob sub_type binary";
			case CLOB, NCLOB -> "blob sub_type text";
			default -> super.columnType( sqlTypeCode );
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		if ( getVersion().isBefore( 4, 0 ) ) {
			//precision of a Firebird 3 and earlier 'float(p)' represents
			//decimal digits instead of binary digits
			ddlTypeRegistry.addDescriptor( StandardDdlTypes.binaryFloat( this ) );
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
	@SPI({ IMPLEMENT, SUPPLY })
	protected void contributeDefaultProperties(java.util.Properties properties) {
		super.contributeDefaultProperties( properties );
		properties.setProperty( org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE, Integer.toString( 0 ) );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TimeZoneSupport getTimeZoneSupport() {
		return getVersion().isSameOrAfter( 4, 0 ) ? TimeZoneSupport.NATIVE : TimeZoneSupport.NONE;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTime() {
		return getVersion().isSameOrAfter( 4 ) ? "localtime" : "current_time";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTimestamp() {
		return getVersion().isSameOrAfter( 4 ) ? "localtimestamp" : "current_timestamp";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTimestampWithTimeZone() {
		return "current_timestamp";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
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
	@SPI({ IMPLEMENT, SUPPLY })
	public int getPreferredSqlTypeCodeForBoolean() {
		return getVersion().isBefore( 3, 0 )
				? Types.BIT
				: super.getPreferredSqlTypeCodeForBoolean();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public long fractionalSecondPrecisionInNanos() {
		// Formally, Firebird can store values with 100 microsecond precision (100_000 nanoseconds).
		// However, some functions (e.g. CURRENT_TIMESTAMP) will only return values with millisecond precision
		// So, we report millisecond precision
		return 1_000_000; //milliseconds
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalValueSemantics getTemporalValueSemantics() {
		return getVersion().isSameOrAfter( 4, 0 )
				? TemporalValueSemantics.TRUNCATING_WITH_OFFSET_LITERALS
				: TemporalValueSemantics.TRUNCATING;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		final BasicTypeRegistry basicTypeRegistry = functionContributions.getTypeConfiguration().getBasicTypeRegistry();
		final BasicType<byte[]> byteArrayType = basicTypeRegistry.resolve( StandardBasicTypes.BINARY );
		final BasicType<Integer> integerType = basicTypeRegistry.resolve( StandardBasicTypes.INTEGER );
		final BasicType<Double> doubleType = basicTypeRegistry.resolve( StandardBasicTypes.DOUBLE );
		final BasicType<Character> characterType = basicTypeRegistry.resolve( StandardBasicTypes.CHARACTER );

		final DatabaseVersion version = getVersion();

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);
		TypeConfiguration typeConfiguration = functionContributions.getTypeConfiguration();

		// Firebird needs an actual argument type for aggregates like SUM, AVG, MIN, MAX to determine the result type
		functionFactory.aggregates( this, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		// AVG by default uses the input type, so we possibly need to cast the argument type, hence a special function
		functionFactory.avg_castingNonDoubleArguments( this, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );

		functionFactory.concat_pipeOperator();
		functionFactory.cot();
		functionFactory.cosh();
		functionFactory.sinh();
		functionFactory.tanh();
		if ( version.isSameOrAfter( 3 ) ) {
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
		functionFactory.insert_overlay();
		functionFactory.reverse();
		functionFactory.bitandorxornot_binAndOrXorNot();
		functionFactory.leastGreatest_minMaxValue();
		if ( version.isSameOrAfter( 3, 0, 4 )
			|| version.isBefore( 3 ) && version.isSameOrAfter( 2, 5, 9 ) ) {
			functionFactory.localtimeLocaltimestamp();
		}

		SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();
		functionRegistry.registerBinaryTernaryPattern(
				"position",
				integerType,
				"position(?1 in ?2)",
				"position(?1,?2,?3)",
				STRING, STRING, INTEGER,
				typeConfiguration
		).setArgumentListSignature( "(STRING pattern, STRING string[, INTEGER start])" );
		functionRegistry.registerAlternateKey( "locate", "position" );
		functionRegistry.namedDescriptorBuilder( "ascii_val" )
				.setExactArgumentCount( 1 )
				.setInvariantType( integerType )
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
		functionFactory.repeat_rpad( "char_length" );

		if ( version.isSameOrAfter( 3 ) ) {
			functionFactory.windowFunctions();
			functionFactory.hypotheticalOrderedSetAggregates();
			if ( version.isBefore( 4 )) {
				// percent_rank and cume_dist introduced in Firebird 4.0, emulate
				// see hypotheticalOrderedSetAggregates_windowEmulation
				functionRegistry.register(
						"percent_rank",
						new HypotheticalSetWindowEmulation( "percent_rank", StandardBasicTypes.DOUBLE, typeConfiguration )
				);
				functionRegistry.register(
						"cume_dist",
						new HypotheticalSetWindowEmulation( "cume_dist", StandardBasicTypes.DOUBLE, typeConfiguration )
				);
			}
			if ( version.isSameOrAfter( 4 ) ) {
				functionFactory.sha( "crypt_hash(?1 using sha256)" );
				functionFactory.md5( "crypt_hash(?1 using md5)" );
				Arrays.asList( "sha1", "sha256", "sha512" )
						.forEach( hash -> functionRegistry.registerPattern(
								hash,
								"crypt_hash(?1 using " + hash + ")",
								byteArrayType
						) );
				functionRegistry.registerPattern(
						"crc32",
						"hash(?1 using crc32)",
						integerType
				);
				functionFactory.hex( "hex_encode(?1)" );
			}
		}

		functionFactory.listagg_list( "varchar" );

		functionFactory.generateSeries_recursive( getMaximumSeriesSize(), false, false );
	}

	/**
	 * Firebird doesn't support the {@code generate_series} function or {@code lateral} recursive CTEs,
	 * so it has to be emulated with a top level recursive CTE which requires an upper bound on the amount
	 * of elements that the series can return.
	 */
	protected int getMaximumSeriesSize() {
		// The maximum recursion depth of Firebird
		return 1024;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new FirebirdSqlAstTranslator<>( request );
			}
		};
	}

	/**
	 * Firebird 2.5 doesn't have a real {@link Types#BOOLEAN}
	 * type, so...
	 */
	@Override
	@SPI({ USE, IMPLEMENT })
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
				result = from == CastType.STRING
						? buildStringToBooleanCastDecode( "true", "false" )
						: BooleanDecoder.toBoolean( from );
				if ( result != null ) {
					return result;
				}
				break;
			case INTEGER_BOOLEAN:
				result = from == CastType.STRING
						? buildStringToBooleanCastDecode( "1", "0" )
						: BooleanDecoder.toIntegerBoolean( from );
				if ( result != null ) {
					return result;
				}
				break;
			case YN_BOOLEAN:
				result = from == CastType.STRING
						? buildStringToBooleanCastDecode( "'Y'", "'N'" )
						: BooleanDecoder.toYesNoBoolean( from );
				if ( result != null ) {
					return result;
				}
				break;
			case TF_BOOLEAN:
				result = from == CastType.STRING
						? buildStringToBooleanCastDecode( "'T'", "'F'" )
						: BooleanDecoder.toTrueFalseBoolean( from );
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

	/**
	 * Firebird extract() function returns {@link TemporalUnit#DAY_OF_WEEK}
	 * numbered from 0 to 6, and {@link TemporalUnit#DAY_OF_YEAR} numbered
	 * for 0. This isn't consistent with what most other databases do, so
	 * here we adjust the result by generating {@code (extract(unit,arg)+1))}.
	 */
	@Override
	@SPI({ USE, IMPLEMENT })
	public String extractPattern(TemporalUnit unit) {
		return switch ( unit ) {
			case DAY_OF_WEEK, DAY_OF_YEAR -> "(" + TemporalOperationSupports.standard().extractPattern( unit ) + "+1)";
			case QUARTER -> "((extract(month from ?2)+2)/3)";
			case EPOCH -> "datediff(second from timestamp '1970-01-01 00:00:00' to ?2)";
			default -> TemporalOperationSupports.standard().extractPattern( unit );
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		return switch ( unit ) {
			case NATIVE -> "dateadd((?2) millisecond to ?3)";
			case NANOSECOND -> "dateadd((?2)/1e6 millisecond to ?3)";
			case WEEK -> "dateadd((?2)*7 day to ?3)";
			case QUARTER -> "dateadd((?2)*3 month to ?3)";
			default -> "dateadd(?2 ?1 to ?3)";
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		return switch ( unit ) {
			case NATIVE -> "datediff(millisecond from ?2 to ?3)";
			case NANOSECOND -> "datediff(millisecond from ?2 to ?3)*1e6";
			case WEEK -> "datediff(day from ?2 to ?3)/7";
			case QUARTER -> "datediff(month from ?2 to ?3)/3";
			default -> "datediff(?1 from ?2 to ?3)";
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String addColumnPrefix() {
		return "add";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public int getMaxAliasLength() {
		return getVersion().isBefore( 4, 0 ) ? 20 : 52;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public int getMaxIdentifierLength() {
		return getVersion().isBefore( 4 ) ? 31 : 63;
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuildRequest request) {
		final var builder = request.builder();
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

		return super.buildIdentifierHelper( request );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NamespaceSupport getNamespaceSupport() {
		return NamespaceSupports.none();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public IndexNameQualification nameQualification() {
		return IndexNameQualification.UNQUALIFIED;

	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public org.hibernate.dialect.schema.spi.SchemaCommentSupport getSchemaCommentSupport() {
		return getVersion().isSameOrAfter( 2, 0 )
				? org.hibernate.dialect.schema.spi.SchemaCommentSupports.commentOn()
				: org.hibernate.dialect.schema.spi.SchemaCommentSupports.none();
	}

	@Override
	public TupleCountSupport getTupleCountSupport() {
		return TupleCountSupport.NONE;
	}

	@Override
	public ParameterLimits getParameterLimits() {
		// see https://firebirdsql.org/file/documentation/html/en/refdocs/fblangref25/firebird-25-language-reference.html#fblangref25-commons-in
		// and https://firebirdsql.org/file/documentation/html/en/refdocs/fblangref50/firebird-50-language-reference.html#fblangref50-commons-in
		return ParameterLimits.of( getVersion().isBefore( 5 ) ? 1500 : 65535 );
	}

	@Override
	public SubquerySupport getSubquerySupport() {
		return SubquerySupport.builder()
				.feature( SubquerySupport.Feature.EXISTS_IN_SELECT, getVersion().isSameOrAfter( 3, 0 ) )
				.feature( SubquerySupport.Feature.OFFSET, true )
				.feature( SubquerySupport.Feature.LATERAL, getVersion().isSameOrAfter( 4, 0 ) )
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
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
	@SPI({ IMPLEMENT, SUPPLY })
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

	private static final SequenceInformationExtractor LEGACY_SEQUENCE_INFORMATION_EXTRACTOR =
			SequenceInformationExtractors.builder( "select rdb$generator_name from rdb$generators" )
					.sequenceNameColumn( 1 )
					.withoutCatalog()
					.withoutSchema()
					.withoutStartValue()
					.withoutMinimumValue()
					.withoutMaximumValue()
					.withoutIncrementValue()
					.build();

	// Firebird 3 has an 'off by increment' bug, fixed in Firebird 4.
	private static final SequenceInformationExtractor SEQUENCE_INFORMATION_EXTRACTOR =
			SequenceInformationExtractors.builder(
					"select rdb$generator_name,rdb$initial_value,rdb$generator_increment from rdb$generators where coalesce(rdb$system_flag,0)=0"
			)
			.sequenceNameColumn( "rdb$generator_name" )
			.withoutCatalog()
			.withoutSchema()
			.startValueColumn( "rdb$initial_value" )
			.withoutMinimumValue()
			.withoutMaximumValue()
			.incrementValueColumn( "rdb$generator_increment" )
			.build();

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return getVersion().isBefore( 3, 0 )
				? LEGACY_SEQUENCE_INFORMATION_EXTRACTOR
				: SEQUENCE_INFORMATION_EXTRACTOR;
	}




	@Override
	public LimitHandler getLimitHandler() {
		return getVersion().isBefore( 3, 0 )
				? FirstSkipLimitHandler.INSTANCE
				: OffsetFetchLimitHandler.INSTANCE;
	}

	@Override
	public LockingSupport getLockingSupport() {
		return lockingSupport;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public CurrentTimestampSelection getCurrentTimestampSelection() {
		return CurrentTimestampSelection.prepared( "select current_timestamp from rdb$database" );
	}

	@Override
	public NullOrderingSupport getNullOrderingSupport() {
		return NullOrderingSupport.builder( super.getNullOrderingSupport() )
				.defaultOrdering(
						getVersion().isSameOrAfter( 2, 0 ) ? NullOrdering.SMALLEST : NullOrdering.LAST
				)
				.capability(
						NullOrderingSupport.Capability.NULLS_FIRST_LAST,
						getVersion().isSameOrAfter( 1, 5 )
				)
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public FetchClauseSupport getFetchClauseSupport() {
		return getVersion().isSameOrAfter( 3 )
				? FetchClauseSupport.ROWS_ONLY
				: FetchClauseSupport.NONE;
	}

	@Override
	public ValuesListSupport getValuesListSupport() {
		return ValuesListSupport.NONE;
	}

	@Override
	public WindowFunctionSupport getWindowFunctionSupport() {
		if ( getVersion().isBefore( 3 ) ) {
			return WindowFunctionSupport.NONE;
		}
		final WindowFunctionSupport.Builder builder = WindowFunctionSupport.builder()
				.features(
						WindowFunctionSupport.Feature.WINDOW_FUNCTIONS,
						WindowFunctionSupport.Feature.PARTITION_BY
				);
		if ( getVersion().isSameOrAfter( 4 ) ) {
			builder.features(
					WindowFunctionSupport.Feature.ROWS_FRAME,
					WindowFunctionSupport.Feature.RANGE_FRAME
			);
		}
		return builder.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NationalizationSupport getNationalizationSupport() {
		return NationalizationSupport.IMPLICIT;
	}

	@Override
	public PredicateSupport getPredicateSupport() {
		return PredicateSupport.builder( super.getPredicateSupport() )
				.capability( PredicateSupport.Capability.DISTINCT_FROM, true )
				.capability(
						PredicateSupport.Capability.EXPRESSION_PLACEMENT,
						getVersion().isSameOrAfter( 3 )
				)
				.build();
	}

	@Override
	public CteSupport getCteSupport() {
		// Recursive CTEs are supported since Firebird 2.1
		return CteSupport.builder()
				.placement( CteSupport.Placement.SUBQUERY )
				.recursiveFeatures( CteSupport.RecursiveFeature.RECURSIVE )
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendDefinition(SqlAppender appender, ColumnDefinitionRequest request) {
		if ( request.generatedExpression() == null ) {
			appender.appendSql( ' ' );
			appender.appendSql( request.sqlType() );
		}
		if ( request.renderedCollation() != null ) {
			appender.appendSql( " collate " );
			appender.appendSql( request.renderedCollation() );
		}
		if ( request.defaultExpression() != null ) {
			appender.appendSql( " default " );
			appender.appendSql( request.defaultExpression() );
		}
		if ( request.generatedExpression() != null ) {
			appender.appendSql( " generated always as (" );
			appender.appendSql( request.generatedExpression() );
			appender.appendSql( ')' );
		}
		if ( !request.nullable() ) {
			appender.appendSql( " not null" );
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String translateExtractField(TemporalUnit unit) {
		return switch ( unit ) {
			case DAY_OF_MONTH -> "day";
			case DAY_OF_YEAR -> "yearday";
			case DAY_OF_WEEK -> "weekday";
			default -> TemporalOperationSupports.standard().translateExtractField( unit );
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
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
				appendAsTime(
						appender,
						temporalAccessor,
						getTemporalValueSemantics().supportsLiteralOffset(),
						jdbcTimeZone,
						NUMERIC_OFFSET
				);
				appender.appendSql( '\'' );
				break;
			case TIMESTAMP:
				appender.appendSql( "timestamp '" );
				appendAsTimestampWithMillis(
						appender,
						temporalAccessor,
						getTemporalValueSemantics().supportsLiteralOffset(),
						jdbcTimeZone,
						NUMERIC_OFFSET
				);
				appender.appendSql( '\'' );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
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

	@Override
	@SPI({ USE, IMPLEMENT })
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
	@SPI({ USE, IMPLEMENT })
	public void appendFormat(SqlAppender appender, String format) {
		throw new UnsupportedOperationException( "format() function not supported on Firebird" );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendUUIDLiteral(SqlAppender appender, UUID literal) {
		appender.appendSql( "char_to_uuid('" );
		appender.appendSql( literal.toString() );
		appender.appendSql( "')" );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
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
	@SPI({ IMPLEMENT, SUPPLY })
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
				case 336396758: {
					// *no error name* (violation of FOREIGN KEY constraint "{0}")
					final String constraintName = getViolatedConstraintNameExtractor().extractConstraintName(
							sqlException );
					return new ConstraintViolationException(
							message,
							sqlException,
							sql,
							ConstraintViolationException.ConstraintKind.FOREIGN_KEY,
							constraintName
					);
				}
				case 335544558: {
					// isc_check_constraint (Operation violates CHECK constraint {0} on view or table {1})
					final String constraintName = getViolatedConstraintNameExtractor().extractConstraintName(
							sqlException );
					return new ConstraintViolationException(
							message,
							sqlException,
							sql,
							ConstraintViolationException.ConstraintKind.CHECK,
							constraintName
					);
				}
				case 336396991:
					// *no error name* (Operation violates CHECK constraint {0} on view or table)
				case 335544665: {
					// isc_unique_key_violation (violation of PRIMARY or UNIQUE KEY constraint "{0}" on table "{1}")
					final String constraintName = getViolatedConstraintNameExtractor().extractConstraintName(
							sqlException );
					return new ConstraintViolationException(
							message,
							sqlException,
							sql,
							ConstraintViolationException.ConstraintKind.UNIQUE,
							constraintName
					);
				}
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
	public MultiTableMutationSupport getMultiTableMutationSupport() {
		return getVersion().isBefore( 2, 1 )
				? super.getMultiTableMutationSupport()
				: MultiTableMutationSupport.GLOBAL_TEMPORARY_TABLE;
	}

	@Override
	public TemporaryTableStrategy getGlobalTemporaryTableStrategy() {
		return StandardGlobalTemporaryTableStrategy.INSTANCE;
	}

	private final FirebirdIndexExporter indexExporter = new FirebirdIndexExporter( this );

	@Override
	public Exporter<Index> getIndexExporter() {
		return indexExporter;
	}

	private static final class FirebirdIndexExporter implements Exporter<Index> {
		private final Dialect dialect;
		private final StandardIndexExporter standardExporter;

		public FirebirdIndexExporter(Dialect dialect) {
			this.dialect = dialect;
			this.standardExporter = new StandardIndexExporter( dialect );
		}

		@Override
		public String[] getSqlCreateStrings(Index index, Metadata metadata, SqlStringGenerationContext context) {
			final String tableName = context.format( index.getTable().getQualifiedTableName() );
			final String indexNameForCreation = index.getQuotedName( dialect );
			// In firebird the index is only sortable on top-level, not per column, use the first column to decide
			final String sortOrder = index.getSelectableOrderMap().getOrDefault( index.getSelectables().get( 0 ), "asc" );
			final StringBuilder buf = new StringBuilder()
					// Although `create asc index` is valid, generate without (some tests check for a specific syntax prefix)
					.append( "desc".equalsIgnoreCase( sortOrder ) || "descending".equalsIgnoreCase( sortOrder ) ? "create desc index " : "create index " )
					.append( indexNameForCreation )
					.append( " on " )
					.append( tableName )
					.append( " (" );
			boolean first = true;
			for ( var selectable : index.getSelectables() ) {
				if ( first ) {
					first = false;
				}
				else {
					buf.append( ", " );
				}
				buf.append( selectable.getText( dialect ) );
			}
			buf.append( ')' );

			return new String[] { buf.toString() };
		}

		@Override
		public String[] getSqlDropStrings(Index index, Metadata metadata, SqlStringGenerationContext context) {
			return standardExporter.getSqlDropStrings( index, metadata, context );
		}
	}

	@Override
	public SingleRowTableSupport getSingleRowTableSupport() {
		final String tableExpression = "rdb$database";
		return SingleRowTableSupport.builder( super.getSingleRowTableSupport() )
				.tableExpression( tableExpression )
				.selectOnlyFromClause( " from " + tableExpression )
				.build();
	}

	@Override
	public SetOperationSupport getSetOperationSupport() {
		// Firebird 4 and earlier are quite strict i.e. it requires `select ... union all select * from (select ...)`
		// rather than `select ... union all (select ...)`
		return SetOperationSupport.builder()
				.operator( SetOperator.INTERSECT, false )
				.operator( SetOperator.INTERSECT_ALL, false )
				.operator( SetOperator.EXCEPT, false )
				.operator( SetOperator.EXCEPT_ALL, false )
				.capability(
						SetOperationSupport.Capability.SIMPLE_QUERY_GROUPING,
						getVersion().isSameOrAfter( 5 )
				)
				.build();
	}

	@Override
	public RowValueSupport getRowValueSupport() {
		return RowValueSupport.NONE;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public List<String> renderCommands(TruncateRequest request) {
		// Firebird doesn't have truncate table; https://github.com/FirebirdSQL/firebird/issues/2892
		return request.tableNames().stream().map( name -> "delete from " + name ).toList();
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}

}
