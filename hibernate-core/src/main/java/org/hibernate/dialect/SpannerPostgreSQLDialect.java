/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupport;

import org.hibernate.dialect.temporaltype.spi.CurrentTemporalSupport;

import org.hibernate.SPI;
import static org.hibernate.SPI.Role.USE;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;

import org.hibernate.dialect.type.spi.StandardDdlTypes;

import org.hibernate.dialect.type.spi.TypeSizingProfile;
import org.hibernate.dialect.type.spi.EnumSupport;
import org.hibernate.dialect.type.internal.SpannerPostgreSQLEnumSupport;

import org.hibernate.dialect.mutation.spi.MultiTableMutationSupport;
import org.hibernate.dialect.jdbc.spi.ParameterLimits;

import org.hibernate.dialect.schema.internal.SpannerPostgreSQLTableExporter;

import org.hibernate.dialect.function.spi.Replacer;

import org.hibernate.dialect.lock.spi.RowLockStrategy;

import org.hibernate.dialect.aggregate.spi.FunctionalDependencyAnalysisSupport;

import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxSupport;
import org.hibernate.dialect.sql.ast.spi.PredicateSupport;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.SingleRowTableSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;
import org.hibernate.dialect.sql.ast.spi.ValuesListSupport;

import jakarta.persistence.TemporalType;
import jakarta.annotation.Nullable;
import org.hibernate.LockOptions;
import org.hibernate.JDBCException;
import org.hibernate.ScrollMode;
import org.hibernate.Timeouts;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.aggregate.internal.SpannerPostgreSQLAggregateSupport;
import org.hibernate.dialect.sequence.internal.SpannerPostgreSQLSequenceSupport;
import org.hibernate.dialect.sql.ast.internal.SpannerPostgreSQLSqlAstTranslator;
import org.hibernate.dialect.function.CountFunction;
import org.hibernate.dialect.function.InsertSubstringOverlayEmulation;
import org.hibernate.dialect.function.array.ArrayContainsOperatorFunction;
import org.hibernate.dialect.function.array.ArrayIncludesOperatorFunction;
import org.hibernate.dialect.function.json.SpannerPostgreSQLJsonArrayFunction;
import org.hibernate.dialect.function.json.SpannerPostgreSQLJsonObjectFunction;
import org.hibernate.query.sqm.CastType;
import org.hibernate.dialect.aggregate.spi.AggregateSupport;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.spi.TupleCountSupport;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;
import org.hibernate.dialect.function.SpannerConcatFunction;
import org.hibernate.dialect.function.array.SpannerPostgreSQLArrayConcatElementFunction;
import org.hibernate.dialect.function.array.SpannerPostgreSQLArrayTrimEmulation;
import org.hibernate.dialect.function.array.SpannerPostgreSQLArrayReplaceFunction;
import org.hibernate.dialect.function.array.SpannerPostgreSQLArrayRemoveFunction;
import org.hibernate.dialect.function.array.SpannerPostgreSQLArrayRemoveIndexFunction;
import org.hibernate.dialect.function.SpannerPostgreSQLRegexpLikeFunction;
import org.hibernate.dialect.function.SpannerPostgreSQLTruncFunction;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.internal.NoLockingSupport;
import org.hibernate.dialect.lock.internal.LockingSupportSimple;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.LimitOffsetLimitHandler;
import org.hibernate.dialect.rowsecurity.spi.RowLevelSecurity;
import org.hibernate.dialect.rowid.spi.RowIdSupport;
import org.hibernate.dialect.rowid.spi.RowIdSupports;
import org.hibernate.dialect.schema.spi.ConstraintDropMode;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.schema.spi.IfExistsSupport;
import org.hibernate.dialect.schema.spi.SchemaDropSupport;
import org.hibernate.dialect.schema.spi.TruncateMode;
import org.hibernate.dialect.schema.spi.TruncateRequest;
import org.hibernate.dialect.rowsecurity.spi.RowLevelSecurityStrategies;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.temptable.spi.PersistentTemporaryTableStrategy;
import org.hibernate.dialect.temptable.spi.TemporaryTableStrategy;
import org.hibernate.dialect.type.spi.PostgreSQLJdbcTypes;
import org.hibernate.dialect.unique.spi.UniqueDelegates;
import org.hibernate.dialect.unique.spi.UniqueDelegate;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;

import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.procedure.spi.CallableStatementSupports;
import org.hibernate.dialect.sql.ast.spi.FetchClauseSupport;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.dialect.temporaltype.spi.IntervalType;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.dialect.lock.spi.LockingClauseStrategy;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.InformationExtractor;
import org.hibernate.tool.schema.extract.spi.InformationExtractors;
import org.hibernate.tool.schema.spi.StandardTableExporter;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.BlobJdbcType;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;
import org.hibernate.type.descriptor.jdbc.SpannerLocalDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.SpannerLocalTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.SpannerTimeJdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static org.hibernate.dialect.lock.internal.NonLockingClauseStrategy.NON_CLAUSE_STRATEGY;
import static org.hibernate.type.SqlTypes.BIGINT;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.DECIMAL;
import static org.hibernate.type.SqlTypes.FLOAT;
import static org.hibernate.type.SqlTypes.INTEGER;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.NUMERIC;
import static org.hibernate.type.SqlTypes.SMALLINT;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_UTC;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_UTC;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.UUID;
import static org.hibernate.type.SqlTypes.VARCHAR;

public class SpannerPostgreSQLDialect extends PostgreSQLDialect implements CurrentTemporalSupport, TemporalOperationSupport {
	private IfExistsSupport ifExistsSupport;
	private SchemaDropSupport schemaDropSupport;


	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalOperationSupport getTemporalOperationSupport() {
		return this;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public CurrentTemporalSupport getCurrentTemporalSupport() {
		return this;
	}
	private final TypeSizingProfile typeSizingProfile = TypeSizingProfile.builder( super.getTypeSizingProfile() )
			.maxVarcharLength( 2_621_440 )
			.maxNVarcharLength( 2_621_440 ).maxNVarcharCapacity( 2_621_440 )
			.maxVarbinaryLength( 10_485_760 ).maxVarbinaryCapacity( 10_485_760 )
			.build();

	@Override
	public TypeSizingProfile getTypeSizingProfile() {
		return typeSizingProfile;
	}

	private final UniqueDelegate SPANNER_UNIQUE_DELEGATE = UniqueDelegates.alwaysIndex( this );
	private final StandardTableExporter SPANNER_TABLE_EXPORTER = new SpannerPostgreSQLTableExporter( this );
	private final SequenceSupport SPANNER_SEQUENCE_SUPPORT = new SpannerPostgreSQLSequenceSupport(this);

	// This will use a monotonically increasing value that is within the range of a 32-bit integer
	// as the primary key value. Since Spanner only supports bit-reversed sequences, this option
	// range of a 32-bit integer.
	// This workaround that is only intended for testing, and should not be used for primary key
	// values in production.
	private static final String USE_INTEGER_FOR_PRIMARY_KEY = "hibernate.dialect.spanner.use_integer_for_primary_key";
	private static final String USE_EMULATOR = "hibernate.dialect.spanner.use_emulator";

	private boolean useIntegerForPrimaryKey;
	private boolean useEmulator;

	private final LockingSupport SPANNER_LOCKING_SUPPORT = new LockingSupportSimple(
			PessimisticLockStyle.CLAUSE,
			RowLockStrategy.NONE,
			LockTimeoutType.NONE,
			OuterJoinLockingType.FULL,
			ConnectionLockTimeoutStrategy.NONE
	);

	protected final static DatabaseVersion MINIMUM_POSTGRES_VERSION = DatabaseVersion.make( 15 );

	public SpannerPostgreSQLDialect() {
		super();
	}

	public SpannerPostgreSQLDialect(DialectResolutionInfo info) {
		super( info );
	}

	public SpannerPostgreSQLDialect(DatabaseVersion version) {
		super( MINIMUM_POSTGRES_VERSION );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry( functionContributions );

		final var functionFactory = new CommonFunctionFactory( functionContributions );
		final var functionRegistry = functionContributions.getFunctionRegistry();

		functionFactory.leftRight_substr();
		functionFactory.pi_acos();
		functionFactory.log_ln();
		functionFactory.degrees_acos();
		functionFactory.radians_acos();
		functionFactory.bitandorxornot_operator();
		functionFactory.characterLength_length( SqlAstNodeRenderingMode.DEFAULT);
		functionFactory.dateTrunc();
		functionRegistry.registerAlternateKey("log10", "log");
		functionFactory.power_spanner();
		functionFactory.sqrt_spanner();
		functionFactory.substr();
		functionFactory.position_locate_spanner();
		functionFactory.round_spanner();
		functionFactory.log_spanner();
		functionFactory.sinh_exp();
		functionFactory.cosh_exp();
		functionFactory.tanh_exp();
		functionRegistry.register(
				"count",
				new CountFunction(
						this,
						functionContributions.getTypeConfiguration(),
						SqlAstNodeRenderingMode.DEFAULT,
						"||",
						"varchar",
						true
				)
		);
		functionRegistry.registerPattern(
				"chr",
				"'~'",
				functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
		);
		functionRegistry.registerPattern(
				"var_pop",
				"(avg(?1 * ?1)-power(cast(avg(?1) as float8),cast(2 as float8)))" );
		functionRegistry.registerPattern(
				"stddev_pop",
				"sqrt(avg(?1 * ?1)-power(cast(avg(?1) as float8),cast(2 as float8)))" );

		functionFactory.varSamp_sumCount_spanner();
		functionFactory.stddevSamp_sumCount_spanner();

		functionFactory.octetLength_pattern("length(?1)");
		functionFactory.bitLength_pattern("length(?1)*8");
		functionFactory.sha("sha256(?1)");

		functionRegistry.register( "concat",
				new SpannerConcatFunction( functionContributions.getTypeConfiguration()) );
		functionRegistry.register( "regexp_like",
				new SpannerPostgreSQLRegexpLikeFunction(functionContributions.getTypeConfiguration()));
		functionRegistry.register( "trunc",
				new SpannerPostgreSQLTruncFunction(functionContributions.getTypeConfiguration()));
		functionRegistry.registerAlternateKey("truncate", "trunc");
		functionRegistry.register( "overlay",
				new InsertSubstringOverlayEmulation(functionContributions.getTypeConfiguration(), false));

		// Postgres uses # instead of ^ for XOR
		functionRegistry.patternDescriptorBuilder( "bitxor", "(?1#?2)" )
				.setExactArgumentCount( 2 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();

		functionRegistry.register( "json_array",
				new SpannerPostgreSQLJsonArrayFunction( functionContributions.getTypeConfiguration() ) );
		functionRegistry.register( "json_object",
				new SpannerPostgreSQLJsonObjectFunction( functionContributions.getTypeConfiguration() ) );

		functionFactory.unnest_postgresql( false );
		functionFactory.arrayLength_spannerpg();

		functionRegistry.register( "array_prepend", new SpannerPostgreSQLArrayConcatElementFunction( true ) );
		functionRegistry.register( "array_append", new SpannerPostgreSQLArrayConcatElementFunction( false ) );
		functionRegistry.register( "array_trim", new SpannerPostgreSQLArrayTrimEmulation() );
		functionRegistry.register( "array_replace", new SpannerPostgreSQLArrayReplaceFunction() );
		functionRegistry.register( "array_remove", new SpannerPostgreSQLArrayRemoveFunction() );
		functionRegistry.register( "array_remove_index", new SpannerPostgreSQLArrayRemoveIndexFunction( true ) );
		functionRegistry.register( "array_contains", new ArrayContainsOperatorFunction( false, functionContributions.getTypeConfiguration() ) );
		functionRegistry.register( "array_includes", new ArrayIncludesOperatorFunction( false, functionContributions.getTypeConfiguration() ) );
		functionRegistry.register( "array_includes_nullable", new ArrayIncludesOperatorFunction( true, functionContributions.getTypeConfiguration() ) );
	}

	@Override
	protected void registerJsonFunction(CommonFunctionFactory functionFactory) {
		functionFactory.jsonObject_postgresql( false );
		functionFactory.jsonArray_postgresql( false );
		functionFactory.jsonSet_postgresql();
		functionFactory.jsonRemove_postgresql();
		functionFactory.jsonReplace_postgresql();
		functionFactory.jsonArrayInsert_postgresql();
	}

	@Override
	protected void registerArrayFunctions(CommonFunctionFactory functionFactory) {
		functionFactory.array_postgresql();
		functionFactory.arrayAggregate();
		functionFactory.arrayConcat_postgresql();
		functionFactory.arrayPrepend_postgresql();
		functionFactory.arrayAppend_postgresql();
		functionFactory.arrayIntersects_postgresql();
		functionFactory.arrayGet_bracket();
		functionFactory.arraySlice_operator();
		functionFactory.arrayReplace();
		functionFactory.arrayReverse_unnest();
		functionFactory.arraySort_unnest();
		functionFactory.arrayToString_postgresql();
	}

	@Override
	protected void registerXmlFunctions(CommonFunctionFactory functionFactory) {
	}

	@Override
	protected void registerUtilityFunctions(FunctionContributions functionContributions) {
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void contributeDefaultProperties(java.util.Properties properties) {
		super.contributeDefaultProperties( properties );
		properties.setProperty( AvailableSettings.PREFERRED_POOLED_OPTIMIZER, "none" );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String getArrayTypeName(String javaElementTypeName, String elementTypeName, Integer maxLength) {
		if ( elementTypeName != null && elementTypeName.equals( "varchar" ) ) {
			elementTypeName = "text";
		}
		return super.getArrayTypeName( javaElementTypeName, elementTypeName, maxLength );
	}

	@Override
	public StandardTableExporter getTableExporter() {
		return SPANNER_TABLE_EXPORTER;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public UniqueDelegate getUniqueDelegate() {
		return SPANNER_UNIQUE_DELEGATE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		return SPANNER_SEQUENCE_SUPPORT;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public RowLevelSecurity getRowLevelSecurity() {
		return RowLevelSecurityStrategies.none();
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LimitOffsetLimitHandler.INSTANCE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public EnumSupport getEnumSupport() {
		return SpannerPostgreSQLEnumSupport.INSTANCE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public AggregateSupport getAggregateSupport() {
		return SpannerPostgreSQLAggregateSupport.INSTANCE;
	}

	@Override
	public LockingSupport getLockingSupport() {
		return useEmulator ? NoLockingSupport.NO_LOCKING_SUPPORT : SPANNER_LOCKING_SUPPORT;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected Integer resolveSqlTypeCode(String columnTypeName, TypeConfiguration typeConfiguration) {
		return switch (columnTypeName) {
			case "character varying" -> Types.VARCHAR;
			case "timestamp with time zone" -> Types.TIMESTAMP_WITH_TIMEZONE;
			case "bigint" -> Types.BIGINT;
			case "real" -> Types.REAL; // Use REAL instead of FLOAT to get Float as recommended Java type
			default -> super.resolveSqlTypeCode( columnTypeName, typeConfiguration );
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public FetchClauseSupport getFetchClauseSupport() {
		return FetchClauseSupport.NONE;
	}















	@Override
	public LockingClauseStrategy getLockingClauseStrategy(
			QuerySpec querySpec, LockOptions lockOptions) {
		if ( lockOptions == null ) {
			return NON_CLAUSE_STRATEGY;
		}
		validateSpannerLockTimeout( lockOptions.getTimeOut() );
		return super.getLockingClauseStrategy( querySpec, lockOptions );
	}

	private static void validateSpannerLockTimeout(int millis) {
		if ( Timeouts.isRealTimeout( millis ) ) {
			throw new UnsupportedOperationException( "Spanner does not support lock timeout." );
		}
		if ( millis == Timeouts.SKIP_LOCKED_MILLI ) {
			throw new UnsupportedOperationException( "Spanner does not support skip locked." );
		}
		if ( millis == Timeouts.NO_WAIT_MILLI ) {
			throw new UnsupportedOperationException( "Spanner does not support no wait." );
		}
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );

		final var configurationService = serviceRegistry.requireService( ConfigurationService.class );

		this.useIntegerForPrimaryKey = configurationService.getSetting(
				USE_INTEGER_FOR_PRIMARY_KEY,
				StandardConverters.BOOLEAN,
				false
		);

		this.useEmulator = configurationService.getSetting(
				USE_EMULATOR,
				StandardConverters.BOOLEAN,
				false
		);
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public FunctionalDependencyAnalysisSupport getFunctionalDependencyAnalysisSupport() {
		return FunctionalDependencyAnalysisSupport.NONE;
	}

	@Override
	protected void contributePostgreSQLTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		final var jdbcTypeRegistry = typeContributions.getTypeConfiguration()
				.getJdbcTypeRegistry();

		jdbcTypeRegistry.addDescriptor( SpannerLocalDateTimeJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( SpannerLocalTimeJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( SpannerTimeJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( Types.BLOB, BlobJdbcType.BLOB_BINDING );
		jdbcTypeRegistry.addDescriptor( Types.CLOB, ClobJdbcType.CLOB_BINDING );
		jdbcTypeRegistry.addDescriptor( PostgreSQLJdbcTypes.uuid() );

		// Replace the standard array constructor
		jdbcTypeRegistry.addTypeConstructor( PostgreSQLJdbcTypes.arrayConstructor() );

		jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.castingJsonb() );
		jdbcTypeRegistry.addTypeConstructorIfAbsent( PostgreSQLJdbcTypes.castingJsonbArrayConstructor() );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new SpannerPostgreSQLSqlAstTranslator<T>( request );
			}
		};
	}

	@Override
	protected void registerPostgreSQLColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		final var ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		// We need to configure that the array type uses the raw element type for casts
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.standardArray( this, true ) );
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( UUID, "uuid", this ) );
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( JSON, "jsonb", this ) );

		ddlTypeRegistry.addDescriptor(
				StandardDdlTypes.builder( FLOAT, columnType( FLOAT ), this ).castTypeName( castType( FLOAT ) )
						.withTypeCapacity( 24, "real" )
						.withTypeCapacity( 53, "double precision" )
						.build()
		);
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendDateTimeLiteral(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			@SuppressWarnings("deprecation")
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		if ( precision == TemporalType.TIME || (precision == TemporalType.TIMESTAMP && !temporalAccessor.isSupported( ChronoField.OFFSET_SECONDS ))) {
			precision = TemporalType.TIMESTAMP;
			if ( temporalAccessor instanceof LocalTime localTime) {
				temporalAccessor = localTime.atDate( LocalDate.of( 1970, 1, 1 ) )
						.atOffset( ZoneOffset.UTC );
			}
			else if ( temporalAccessor instanceof OffsetTime offsetTime ) {
				temporalAccessor = offsetTime.atDate( LocalDate.of( 1970, 1, 1 ) );
			}
			else if ( temporalAccessor instanceof LocalDateTime localDateTime) {
				temporalAccessor = localDateTime.atOffset(  ZoneOffset.UTC );
			}
			else if ( temporalAccessor instanceof Instant instant) {
				temporalAccessor = instant.atOffset(  ZoneOffset.UTC );
			}
			else {
				throw new UnsupportedOperationException( "Unsupported temporal type: " + temporalAccessor.getClass().getName() );
			}
		}

		super.appendDateTimeLiteral(  appender, temporalAccessor, precision, jdbcTimeZone );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendDateTimeLiteral(
			SqlAppender appender,
			Date date,
			@SuppressWarnings("deprecation")
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		if ( precision == TemporalType.TIME ) {
			precision = TemporalType.TIMESTAMP;
		}
		super.appendDateTimeLiteral( appender, date, precision, jdbcTimeZone );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendDateTimeLiteral(
			SqlAppender appender,
			Calendar calendar,
			@SuppressWarnings("deprecation")
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		if ( precision == TemporalType.TIME ) {
			precision = TemporalType.TIMESTAMP;
		}

		super.appendDateTimeLiteral( appender, calendar, precision, jdbcTimeZone );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String castType(int sqlTypeCode) {
		return switch (sqlTypeCode) {
			case TIME, TIME_UTC, TIMESTAMP, TIMESTAMP_UTC -> columnType(TIMESTAMP_WITH_TIMEZONE);
			default -> super.castType(sqlTypeCode);
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		final String temporal = temporalType == TemporalType.DATE ? "cast(?3 as " + castType(TIMESTAMP) + ")" : "?3";
		return intervalType != null
				? "(?2+" + temporal + ")"
				: "cast(" + temporal + "+" + intervalPattern(unit) + " as " + castTemporalType(temporalType) + ")";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		final String pattern = switch (unit) {
			case YEAR -> "extract(year from ?3)-extract(year from ?2)";
			// For month, we also need to account for years
			case MONTH -> "(extract(year from ?3)-extract(year from ?2))*12+(extract(month from ?3)-extract(month from ?2))";
			// Quarter is month diff / 3
			case QUARTER ->
				"((extract(year from ?3)-extract(year from ?2))*12+(extract(month from ?3)-extract(month from ?2)))/3";
			case WEEK -> "(extract(epoch from ?3)-extract(epoch from ?2))/604800";
			case DAY -> "(extract(epoch from ?3)-extract(epoch from ?2))/86400";
			case HOUR -> "(extract(epoch from ?3)-extract(epoch from ?2))/3600";
			case MINUTE -> "(extract(epoch from ?3)-extract(epoch from ?2))/60";
			case SECOND -> "extract(epoch from ?3)-extract(epoch from ?2)";
			case NANOSECOND -> "(extract(epoch from ?3)-extract(epoch from ?2))*1e9";
			case NATIVE -> "extract(epoch from ?3)-extract(epoch from ?2)";
			default -> "extract(epoch from ?3)-extract(epoch from ?2)";
		};

		return "cast(" + pattern + " as bigint)";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String castPattern(CastType from, CastType to) {
		if (from == CastType.STRING && to == CastType.TIME) {
			return "cast('1970-01-01 ' || ?1 as timestamp with time zone)";
		}
		if (from == CastType.TIME && to == CastType.STRING) {
			return "to_char(?1, 'HH24:MI:SS.MS')";
		}
		return super.castPattern(from, to);
	}

	private static String intervalPattern(TemporalUnit unit) {
		return switch (unit) {
			case NANOSECOND -> "cast(concat(cast((?2)/1e3 as text), ' microsecond') as interval)";
			case NATIVE -> "cast(concat(cast((?2) as text), ' second') as interval)";
			case QUARTER -> "cast(concat(cast((?2)*3 as text), ' month') as interval)";
			case WEEK -> "cast(concat(cast((?2) as text), ' week') as interval)";
			default -> "cast(concat(cast((?2) as text), ' " + unit + "') as interval)";
		};
	}

	private String castTemporalType(TemporalType temporalType) {
		return switch (temporalType) {
			case TIME, TIMESTAMP -> castType( TIMESTAMP );
			default -> temporalType.name().toLowerCase();
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		return switch (sqlTypeCode) {
			// Spanner doesn't support precision with the timestamp
			case TIME, TIME_UTC, TIMESTAMP, TIMESTAMP_UTC, TIMESTAMP_WITH_TIMEZONE -> "timestamp with time zone";
			case BLOB -> "bytea";
			case CLOB, NCLOB -> "character varying";
			// Spanner doesn't support NUMERIC with precision and scale
			case NUMERIC ->  "numeric";
			case DECIMAL ->  "decimal";
			// Spanner doesn't support CHAR so we should use VARCHAR
			case CHAR -> columnType( VARCHAR );
			case SMALLINT, INTEGER, TINYINT ->  columnType( BIGINT );
			default -> super.columnType(sqlTypeCode);
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public ScrollMode defaultScrollMode() {
		return ScrollMode.FORWARD_ONLY;
	}

	@Override
	public TupleCountSupport getTupleCountSupport() {
		return TupleCountSupport.NONE;
	}

	@Override
	public WindowFunctionSupport getWindowFunctionSupport() {
		return WindowFunctionSupport.NONE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public boolean supportsUserDefinedTypes() {
		return false;
	}

	@Override
	public boolean supportsFilterClause() {
		return false;
	}

	@Override
	public RowValueSupport getRowValueSupport() {
		return RowValueSupport.builder( super.getRowValueSupport() )
				.feature( RowValueSupport.Feature.ROW_CONSTRUCTOR, false )
				.feature( RowValueSupport.Feature.ORDERING_COMPARISON, false )
				.feature( RowValueSupport.Feature.DISTINCTNESS_COMPARISON, false )
				.feature( RowValueSupport.Feature.EQUALITY_COMPARISON, false )
				.feature( RowValueSupport.Feature.IN_SUBQUERY, false )
				.feature( RowValueSupport.Feature.QUANTIFIED_COMPARISON, false )
				.build();
	}

	@Override
	public ValuesListSupport getValuesListSupport() {
		return ValuesListSupport.INSERT_ONLY;
	}

	@Override
	public PredicateSupport getPredicateSupport() {
		return PredicateSupport.builder( super.getPredicateSupport() )
				.noCaseInsensitiveLikeOperator()
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTimestamp() {
		return currentTimestampWithTimeZone();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTime() {
		return currentTimestampWithTimeZone();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentLocalTimestamp() {
		return currentTimestampWithTimeZone();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentLocalTime() {
		return currentTimestampWithTimeZone();
	}

	@Override
	public SubquerySupport getSubquerySupport() {
		return SubquerySupport.builder( super.getSubquerySupport() )
				.feature( SubquerySupport.Feature.LATERAL, false )
				.build();
	}

	@Override
	public MutationSyntaxSupport getMutationSyntaxSupport() {
		return MutationSyntaxSupport.NONE;
	}

	@Override
	public ParameterLimits getParameterLimits() {
		return ParameterLimits.of( 100 );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String renderAddConstraint(
			org.hibernate.dialect.constraint.spi.ForeignKeyConstraintRequest request) {
		// Cloud Spanner requires the referenced columns to specified in all cases, including
		// if the foreign key references the primary key of the referenced table.
		return request.isExplicitDefinition()
				? super.renderAddConstraint( request )
				: super.renderAddConstraint( new org.hibernate.dialect.constraint.spi.ForeignKeyConstraintRequest(
						request.constraintName(),
						request.sourceColumnNames(),
						request.referencedTableName(),
						request.targetColumnNames(),
						false,
						null
				) );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public TruncateMode truncateMode() {
		return TruncateMode.PER_TABLE;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public List<String> renderCommands(TruncateRequest request) {
		return request.tableNames().stream().map( name -> "delete from " + name ).toList();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized SchemaDropSupport getSchemaDropSupport() {
		if ( schemaDropSupport == null ) {
			schemaDropSupport = new SchemaDropSupport( List.of(), ConstraintDropMode.EXPLICIT, "" );
		}
		return schemaDropSupport;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized IfExistsSupport getIfExistsSupport() {
		if ( ifExistsSupport == null ) {
			ifExistsSupport = new IfExistsSupport(
				ExistenceCheckPlacement.BEFORE_NAME,
				ExistenceCheckPlacement.BEFORE_NAME,
				ExistenceCheckPlacement.NONE,
				ExistenceCheckPlacement.BEFORE_NAME
		);
		}
		return ifExistsSupport;
	}


	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean addPartitionKeyToPrimaryKey() {
		return false;
	}

	@Override
	public SingleRowTableSupport getSingleRowTableSupport() {
		final String tableExpression = "unnest(ARRAY[1])";
		return SingleRowTableSupport.builder( super.getSingleRowTableSupport() )
				.tableExpression( tableExpression )
				.selectOnlyFromClause( " from " + tableExpression + " dual" )
				.build();
	}

	public Replacer datetimeFormat(String format) {
		return org.hibernate.dialect.OracleDialect.datetimeFormat(format, true, false)
				.replace("SSSSSS", "US")
				.replace("SSSSS", "US")
				.replace("SSSS", "US")
				.replace("SSS", "MS")
				.replace("SS", "MS")
				.replace("S", "MS")
				// use ISO day in week, as per DateTimeFormatter
				.replace("ee", "ID")
				.replace("e", "fmID")
				// TZR is TZ in Postgres
				.replace("zzz", "TZ")
				.replace("zz", "TZ")
				.replace("z", "TZ")
				.replace("ZZZ", "OF")
				.replace("ZZ", "OF")
				.replace("Z", "OF")
				.replace("xxx", "OF")
				.replace("xx", "OF")
				.replace("x", "OF")
				.replace("a", "AM")
				// Spanner-specific overrides
				.replace("hh", "HH12")
				.replace("h", "HH12");
	}

	@Override
	public CteSupport getCteSupport() {
		return CteSupport.builder( super.getCteSupport() )
				.placement( CteSupport.Placement.TOP_LEVEL )
				.recursiveFeatures()
				.supportsCteHeaderColumnList( false )
				.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public InformationExtractor getInformationExtractor(ExtractionContext extractionContext) {
		return InformationExtractors.jdbcMetadata( extractionContext );
	}

	@Override
	public MultiTableMutationSupport getMultiTableMutationSupport() {
		return MultiTableMutationSupport.PERSISTENT_TABLE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public RowIdSupport getRowIdSupport() {
		return RowIdSupports.none();
	}

	@Override
	public TemporaryTableStrategy getLocalTemporaryTableStrategy() {
		return null;
	}

	@Override
	public @Nullable TemporaryTableStrategy getGlobalTemporaryTableStrategy() {
		return null;
	}

	@Override
	public TemporaryTableStrategy getPersistentTemporaryTableStrategy() {
		return new PersistentTemporaryTableStrategy( this );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return this::handleConstraintViolatedException;
	}

	private @Nullable JDBCException handleConstraintViolatedException(SQLException sqlException, String message, String sql) {
		if ( sqlException.getErrorCode() == 6 || ( message != null && isAlreadyExists( message ) ) ) {
			return new ConstraintViolationException( message, sqlException, ConstraintViolationException.ConstraintKind.UNIQUE, null );
		}
		else if ( sqlException.getErrorCode() == 5 || ( message != null && isTableDoesNotExist( message ) ) ) {
			return new SQLGrammarException( message, sqlException );
		}
		else if ( message != null && isNotNullConstraint( message ) ) {
			return new ConstraintViolationException( message, sqlException, ConstraintViolationException.ConstraintKind.NOT_NULL, null );
		}
		else if ( message != null && message.contains( "Check constraint" ) ) {
			return new ConstraintViolationException( message, sqlException, ConstraintViolationException.ConstraintKind.CHECK, null );
		}
		else if ( message != null && isForeignKeyConstraint( message ) ) {
			return new ConstraintViolationException( message, sqlException, ConstraintViolationException.ConstraintKind.FOREIGN_KEY, null );
		}
		else {
			return null;
		}
	}

	private boolean isAlreadyExists(String message) {
		return ( message.contains( "Failed to insert row with primary key" ) && message.contains( "due to previously existing row" ) )
				|| ( message.contains( "UNIQUE violation on index" ) && message.contains( "duplicate key" ) && message.contains( "in this transaction" ) );
	}

	private boolean isTableDoesNotExist(String message) {
		return message.contains( "relation" ) && message.contains( "does not exist" );
	}

	private boolean isNotNullConstraint(String message) {
		return message.contains( "must not be NULL in table" )
				|| message.contains( "does not specify a non-null value for NOT NULL column" )
				|| message.contains( "Cannot specify a null value for column" );
	}

	private boolean isForeignKeyConstraint(String message) {
		return message.contains( "Foreign key" )
				&& ( message.contains( "constraint violation on table" )
						|| message.contains( "constraint violation when deleting or updating referenced key" )
						|| message.contains( "violated on table" ) );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public CallableStatementSupport getCallableStatementSupport() {
		return CallableStatementSupports.standard();
	}

	public boolean useIntegerForPrimaryKey() {
		return useIntegerForPrimaryKey;
	}
}
