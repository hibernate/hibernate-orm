/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import jakarta.persistence.GenerationType;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.dialect.function.InsertSubstringOverlayEmulation;
import org.hibernate.dialect.function.SpannerFormatFunction;
import org.hibernate.dialect.function.SpannerExtractFunction;
import org.hibernate.dialect.function.SpannerTruncFunction;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.SpannerIdentityColumnSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitOffsetLimitHandler;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.sequence.SpannerSequenceSupport;
import org.hibernate.dialect.sql.ast.SpannerSqlAstTranslator;
import org.hibernate.dialect.unique.AlterTableUniqueIndexDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.SchemaNameResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Table;
import org.hibernate.query.SemanticException;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.query.sqm.TrimSpec;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.internal.PessimisticLockKind;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.internal.LockingSupportSimple;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.sql.ast.spi.LockingClauseStrategy;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.hibernate.Timeouts;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hibernate.dialect.SimpleDatabaseVersion.ZERO_VERSION;
import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.useArgType;
import static org.hibernate.sql.ast.internal.NonLockingClauseStrategy.NON_CLAUSE_STRATEGY;
import static org.hibernate.type.SqlTypes.BIGINT;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.DECIMAL;
import static org.hibernate.type.SqlTypes.DOUBLE;
import static org.hibernate.type.SqlTypes.FLOAT;
import static org.hibernate.type.SqlTypes.INTEGER;
import static org.hibernate.type.SqlTypes.LONG32NVARCHAR;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.LONG32VARCHAR;
import static org.hibernate.type.SqlTypes.NCHAR;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.NUMERIC;
import static org.hibernate.type.SqlTypes.NVARCHAR;
import static org.hibernate.type.SqlTypes.REAL;
import static org.hibernate.type.SqlTypes.SMALLINT;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.SqlTypes.VARCHAR;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsDate;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithMillis;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithNanos;

/**
 * A {@linkplain Dialect SQL dialect} for Cloud Spanner.
 *
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 * @author Daniel Zou
 * @author Dmitry Solomakha
 */
public class SpannerDialect extends Dialect {

	private final UniqueDelegate SPANNER_UNIQUE_DELEGATE = new AlterTableUniqueIndexDelegate( this );
	private final Exporter<Table> SPANNER_TABLE_EXPORTER = new SpannerDialectTableExporter( this );
	private final SequenceSupport SPANNER_SEQUENCE_SUPPORT = new SpannerSequenceSupport(this);

	private static final Pattern NOT_NULL_PATTERN = Pattern.compile( ".*Cannot specify a null value for column(?:[:]? (.*?) in table|: (.*?(?=$))).*" );
	private static final Pattern UNIQUE_INDEX_PATTERN = Pattern.compile( ".*UNIQUE violation on index (.*?)(?:,|$).*" );
	private static final Pattern CHECK_PATTERN = Pattern.compile( ".*Check constraint (.*?) is violated.*" );
	private static final Pattern FK_PATTERN = Pattern.compile( ".*Foreign key (.*?) constraint violation.*" );

	private static final String USE_INTEGER_FOR_PRIMARY_KEY = "hibernate.dialect.spanner.use_integer_for_primary_key";
	private boolean useIntegerForPrimaryKey;

	private static final LockingSupport SPANNER_LOCKING_SUPPORT = new LockingSupportSimple(
			PessimisticLockStyle.CLAUSE,
			RowLockStrategy.NONE,
			LockTimeoutType.NONE,
			OuterJoinLockingType.FULL,
			ConnectionLockTimeoutStrategy.NONE
	);

	public SpannerDialect() {
		super( ZERO_VERSION );
	}

	public SpannerDialect(DialectResolutionInfo info) {
		super(info);
	}

	public boolean useIntegerForPrimaryKey() {
		return useIntegerForPrimaryKey;
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );
		final var configurationService = serviceRegistry.requireService( ConfigurationService.class );
		this.useIntegerForPrimaryKey = configurationService.getSetting(
				USE_INTEGER_FOR_PRIMARY_KEY,
				StandardConverters.BOOLEAN,
				false
		);
	}

	@Override
	protected void initDefaultProperties() {
		super.initDefaultProperties();
		getDefaultProperties().setProperty( AvailableSettings.PREFERRED_POOLED_OPTIMIZER, "none" );
	}

	@Override
	protected String columnType(int sqlTypeCode) {
		return switch ( sqlTypeCode ) {
			case BOOLEAN -> "bool";
			case TINYINT, SMALLINT, INTEGER, BIGINT -> "int64";
			case REAL -> "float32";
			case FLOAT, DOUBLE -> "float64";
			case DECIMAL, NUMERIC -> "numeric";
			//there is no time type of any kind
			//timestamp does not accept precision
			case TIME, TIMESTAMP, TIMESTAMP_WITH_TIMEZONE -> "timestamp";
			case CHAR, NCHAR, VARCHAR, NVARCHAR -> "string($l)";
			case BINARY, VARBINARY -> "bytes($l)";
			case CLOB, NCLOB -> "string(max)";
			case BLOB -> "bytes(max)";
			default -> super.columnType( sqlTypeCode );
		};
	}

	@Override
	public String castPattern(CastType from, CastType to) {
		if ( to == CastType.TIME && from == CastType.STRING ) {
			return "cast('1970-01-01 ' || ?1 as timestamp)";
		}
		if ( to == CastType.STRING && from == CastType.TIME ) {
			return "format_timestamp('%H:%M:%E*S', ?1)";
		}
		return super.castPattern( from, to );
	}

	@Override
	protected String castType(int sqlTypeCode) {
		return switch ( sqlTypeCode ) {
			case CHAR, NCHAR, VARCHAR, NVARCHAR, LONG32VARCHAR, LONG32NVARCHAR -> "string";
			case BINARY, VARBINARY, LONG32VARBINARY -> "bytes";
			default -> super.castType( sqlTypeCode );
		};
	}

	@Override
	public boolean supportsTruncateWithCast() {
		return false;
	}

	@Override
	public int getMaxVarcharLength() {
		//max is equivalent to 2_621_440
		return 2_621_440;
	}

	@Override
	public int getMaxVarbinaryLength() {
		//max is equivalent to 10_485_760
		return 10_485_760;
	}

	@Override
	public boolean supportsStandardArrays() {
		return true;
	}

	@Override
	public String getArrayTypeName(String javaElementTypeName, String elementTypeName, Integer maxLength) {
		return "ARRAY<" + elementTypeName + ">";
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry( functionContributions );
		final var basicTypeRegistry = functionContributions.getTypeConfiguration().getBasicTypeRegistry();
		final var byteArrayType = basicTypeRegistry.resolve( StandardBasicTypes.BINARY );
		final var intType = basicTypeRegistry.resolve( StandardBasicTypes.INTEGER );
		final var longType = basicTypeRegistry.resolve( StandardBasicTypes.LONG );
		final var doubleType = basicTypeRegistry.resolve( StandardBasicTypes.DOUBLE );
		final var booleanType = basicTypeRegistry.resolve( StandardBasicTypes.BOOLEAN );
		final var charType = basicTypeRegistry.resolve( StandardBasicTypes.CHARACTER );
		final var stringType = basicTypeRegistry.resolve( StandardBasicTypes.STRING );
		final var dateType = basicTypeRegistry.resolve( StandardBasicTypes.DATE );
		final var timestampType = basicTypeRegistry.resolve( StandardBasicTypes.TIMESTAMP );

		final var functionRegistry = functionContributions.getFunctionRegistry();

		// Aggregate Functions
		functionRegistry.namedAggregateDescriptorBuilder( "any_value" )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedAggregateDescriptorBuilder( "array_agg" )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedAggregateDescriptorBuilder( "countif" )
				.setInvariantType( longType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedAggregateDescriptorBuilder( "logical_and" )
				.setInvariantType( booleanType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedAggregateDescriptorBuilder( "logical_or" )
				.setInvariantType( booleanType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedAggregateDescriptorBuilder( "string_agg" )
				.setInvariantType( stringType )
				.setArgumentCountBetween( 1, 2 )
				.register();

		final var functionFactory = new CommonFunctionFactory( functionContributions );

		// Mathematical Functions
		functionFactory.log();
		functionFactory.log10();
		functionFactory.trunc();
		functionFactory.ceiling_ceil();
		functionFactory.cosh();
		functionFactory.sinh();
		functionFactory.tanh();
		functionFactory.moreHyperbolic();

		functionRegistry.registerPattern(
				"var_pop",
				"(avg(?1 * ?1)-power(avg(?1),2))" );
		functionRegistry.registerPattern(
				"stddev_pop",
				"sqrt(avg(?1 * ?1)-power(avg(?1),2))" );

		functionFactory.bitandorxornot_bitAndOrXorNot();

		functionRegistry.namedDescriptorBuilder( "is_inf" )
				.setInvariantType( booleanType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "is_nan" )
				.setInvariantType( booleanType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "ieee_divide" )
				.setInvariantType( booleanType )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "div" )
				.setInvariantType( longType )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.registerPattern(
				"degrees",
				"(?1 * 180 / acos(-1))",
				doubleType );
		functionRegistry.registerPattern(
				"radians",
				"(?1 * acos(-1) / 180)",
				doubleType );
		functionRegistry.registerPattern(
				"log",
				"log(?2, ?1)",
				doubleType );

		functionFactory.sha1();

		// Hash Functions
		functionRegistry.namedDescriptorBuilder( "farm_fingerprint" )
				.setInvariantType( longType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "sha256" )
				.setInvariantType( byteArrayType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "sha512" )
				.setInvariantType( byteArrayType )
				.setExactArgumentCount( 1 )
				.register();

		// String Functions
		functionFactory.concat_pipeOperator();
		functionFactory.trim2();
		functionFactory.reverse();
		functionFactory.repeat();
		functionFactory.substr();
		functionFactory.substring_substr();
		functionRegistry.namedDescriptorBuilder( "byte_length" )
				.setInvariantType( longType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "code_points_to_bytes" )
				.setInvariantType( byteArrayType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "code_points_to_string" )
				.setInvariantType( stringType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "ends_with" )
				.setInvariantType( booleanType )
				.setExactArgumentCount( 2 )
				.register();
//		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "format" )
//				.setInvariantType( StandardBasicTypes.STRING )
//				.register();
		functionRegistry.namedDescriptorBuilder( "from_base64" )
				.setInvariantType( byteArrayType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "from_hex" )
				.setInvariantType( byteArrayType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "regexp_contains" )
				.setInvariantType( booleanType )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "regexp_extract" )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "regexp_extract_all" )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "regexp_replace" )
				.setExactArgumentCount( 3 )
				.register();
		functionRegistry.namedDescriptorBuilder( "safe_convert_bytes_to_string" )
				.setInvariantType( stringType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "split" )
				.setArgumentCountBetween( 1, 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "starts_with" )
				.setInvariantType( booleanType )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "strpos" )
				.setInvariantType( longType )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "to_base64" )
				.setInvariantType( stringType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "to_code_points" )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "to_hex" )
				.setInvariantType( stringType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.registerPattern(
				"hex",
				"to_hex(cast(?1 as bytes))",
				stringType );
		functionRegistry.registerPattern(
				"ascii",
				"to_code_points(?1)[offset(0)]",
				intType );
		functionRegistry.registerPattern(
				"chr",
				"code_points_to_string([?1])",
				charType );
		functionRegistry.registerPattern(
				"left",
				"substr(?1, 1, ?2)",
				stringType );
		functionRegistry.registerPattern(
				"right",
				"substr(?1, -?2)",
				stringType );
		functionRegistry.register(
				"overlay",
				new InsertSubstringOverlayEmulation( functionContributions.getTypeConfiguration(), false ) );
		functionRegistry.registerBinaryTernaryPattern(
						"locate",
						intType,
						"strpos(?2,?1)",
						"(strpos(substr(?2,?3),?1)+case when strpos(substr(?2,?3),?1)>0 then ?3-1 else 0 end)",
						FunctionParameterType.STRING, FunctionParameterType.STRING, FunctionParameterType.INTEGER,
						functionContributions.getTypeConfiguration()
				)
				.setArgumentListSignature( "(STRING pattern, STRING string[, INTEGER start])" );

		// JSON Functions
		functionRegistry.namedDescriptorBuilder( "json_query" )
				.setInvariantType( stringType )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "json_value" )
				.setInvariantType( stringType )
				.setExactArgumentCount( 2 )
				.register();

		// Array Functions
		functionRegistry.namedDescriptorBuilder( "array" )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "array_concat" )
				.register();
		functionRegistry.namedDescriptorBuilder( "array_length" )
				.setInvariantType( longType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "array_to_string" )
				.setInvariantType( stringType )
				.setArgumentCountBetween( 2, 3 )
				.register();
		functionRegistry.namedDescriptorBuilder( "array_reverse" )
				.setExactArgumentCount( 1 )
				.register();

		// Date functions
		functionRegistry.namedDescriptorBuilder( "date" )
				.setInvariantType( dateType )
				.setArgumentCountBetween( 1, 3 )
				.register();
		functionRegistry.namedDescriptorBuilder( "date_add" )
				.setInvariantType( dateType )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "date_sub" )
				.setInvariantType( dateType )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "date_diff" )
				.setInvariantType( longType )
				.setExactArgumentCount( 3 )
				.register();
		functionRegistry.namedDescriptorBuilder( "date_trunc" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "date_from_unix_date" )
				.setInvariantType( dateType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "format_date" )
				.setInvariantType( stringType )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "parse_date" )
				.setInvariantType( dateType )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "unix_date" )
				.setInvariantType( longType )
				.setExactArgumentCount( 1 )
				.register();

		// Timestamp functions
		functionRegistry.namedDescriptorBuilder( "string" )
				.setInvariantType( stringType )
				.setArgumentCountBetween( 1, 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "timestamp" )
				.setInvariantType( timestampType )
				.setArgumentCountBetween( 1, 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "timestamp_add" )
				.setInvariantType( timestampType )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "timestamp_sub" )
				.setInvariantType( timestampType )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "timestamp_diff" )
				.setInvariantType( longType )
				.setExactArgumentCount( 3 )
				.register();
		functionRegistry.namedDescriptorBuilder( "timestamp_trunc" )
				.setInvariantType( timestampType )
				.setArgumentCountBetween( 2, 3 )
				.register();
		functionRegistry.namedDescriptorBuilder( "format_timestamp" )
				.setInvariantType( stringType )
				.setArgumentCountBetween( 2, 3 )
				.register();
		functionRegistry.namedDescriptorBuilder( "parse_timestamp" )
				.setInvariantType( timestampType )
				.setArgumentCountBetween( 2, 3 )
				.register();
		functionRegistry.namedDescriptorBuilder( "timestamp_seconds" )
				.setInvariantType( timestampType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "timestamp_millis" )
				.setInvariantType( timestampType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "timestamp_micros" )
				.setInvariantType( timestampType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "unix_seconds" )
				.setInvariantType( longType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "unix_millis" )
				.setInvariantType( longType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "unix_micros" )
				.setInvariantType( longType )
				.setExactArgumentCount( 1 )
				.register();
		functionFactory.listagg_stringAgg( "string" );
		functionFactory.inverseDistributionOrderedSetAggregates();
		functionFactory.hypotheticalOrderedSetAggregates();
		functionFactory.array_spanner();

		functionRegistry.register(
				"extract",
				new SpannerExtractFunction( this, functionContributions.getTypeConfiguration() )
		);

		functionRegistry.register(
				"format",
				new SpannerFormatFunction( functionContributions.getTypeConfiguration() )
		);

		functionRegistry.register( "trunc", new SpannerTruncFunction() );
		functionRegistry.registerAlternateKey( "truncate", "trunc" );
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new SpannerSqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return true;
	}

	/* SELECT-related functions */

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "select current_timestamp() as now";
	}

	@Override
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		appender.appendSql( bool );
	}

	@Override
	public void appendBinaryLiteral(SqlAppender appender, byte[] bytes) {
		appender.appendSql( "FROM_HEX('" );
		PrimitiveByteArrayJavaType.INSTANCE.appendString( appender, bytes );
		appender.appendSql( "')" );
	}

	@Override
	public void appendLiteral(SqlAppender appender, String literal) {
		// Spanner uses backslash escaping, so escape single quotes and backslashes with \
		// We also explicitly escape newlines (\n) because Spanner forbids raw line breaks
		// inside standard quoted strings
		StringBuilder builder = new StringBuilder( literal.length() + 2 );
		builder.append( '\'' );
		for ( int i = 0; i < literal.length(); i++ ) {
			final char c = literal.charAt( i );
			switch ( c ) {
				case '\'':
				case '\\':
					builder.append( '\\' );
					builder.append( c );
					break;
				case '\n':
					builder.append( "\\n" );
					break;
				default:
					builder.append( c );
			}
		}
		builder.append( '\'' );
		appender.appendSql( builder.toString() );
	}

	@Override
	public String currentTime() {
		return currentTimestamp();
	}

	@Override
	public boolean supportsTemporalLiteralOffset() {
		return true;
	}

	@Override
	public void appendDateTimeLiteral(
			org.hibernate.sql.ast.spi.SqlAppender appender,
			java.time.temporal.TemporalAccessor temporalAccessor,
			jakarta.persistence.TemporalType precision,
			java.util.TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( "DATE '" );
				appendAsDate( appender, temporalAccessor );
				appender.appendSql( "'" );
				break;
			case TIME:
				appender.appendSql( "TIMESTAMP '" );
				if ( temporalAccessor instanceof java.time.LocalTime localTime ) {
					final OffsetDateTime offsetDateTime = localTime
							.atDate( LocalDate.of( 1970, 1, 1 ) )
							.atOffset( ZoneOffset.UTC );
					appendAsTimestampWithNanos( appender, offsetDateTime, true, jdbcTimeZone );
				}
				else if ( temporalAccessor instanceof java.time.OffsetTime offsetTime ) {
					OffsetDateTime offsetDateTime =
							offsetTime.atDate( LocalDate.of( 1970, 1, 1 ) );
					appendAsTimestampWithNanos( appender, offsetDateTime, true, jdbcTimeZone );
				}
				appender.appendSql( "'" );
				break;
			case TIMESTAMP:
				appender.appendSql( "TIMESTAMP '" );
				if ( temporalAccessor instanceof java.time.LocalDateTime ldt ) {
					appendAsTimestampWithNanos(
							appender,
							ldt.atOffset( ZoneOffset.UTC ),
							supportsTemporalLiteralOffset(),
							jdbcTimeZone
					);
				}
				else {
					appendAsTimestampWithNanos(
							appender,
							temporalAccessor,
							supportsTemporalLiteralOffset(),
							jdbcTimeZone
					);
				}
				appender.appendSql( "'" );
				break;
			default:
				throw new IllegalArgumentException( "Unsupported TemporalType: " + precision );
		}
	}

	@Override
	public void appendDateTimeLiteral(
			org.hibernate.sql.ast.spi.SqlAppender appender,
			java.util.Date date,
			jakarta.persistence.TemporalType precision,
			java.util.TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( "DATE '" );
				appendAsDate( appender, date );
				appender.appendSql( "'" );
				break;
			case TIME:
				appender.appendSql( "TIMESTAMP '" );
				if ( date instanceof java.sql.Time time ) {
					final OffsetDateTime offsetDateTime = time.toLocalTime()
							.atDate( LocalDate.of( 1970, 1, 1 ) )
							.atOffset( ZoneOffset.UTC );
					appendAsTimestampWithNanos( appender, offsetDateTime, true, jdbcTimeZone );
				}
				appender.appendSql( "'" );
				break;
			case TIMESTAMP:
				appender.appendSql( "TIMESTAMP '" );
				appendAsTimestampWithNanos(
						appender,
						date.toInstant(),
						supportsTemporalLiteralOffset(),
						jdbcTimeZone
				);
				appender.appendSql( "'" );
				break;
			default:
				throw new IllegalArgumentException( "Unsupported TemporalType: " + precision );
		}
	}

	@Override
	public void appendDateTimeLiteral(
			org.hibernate.sql.ast.spi.SqlAppender appender,
			java.util.Calendar calendar,
			jakarta.persistence.TemporalType precision,
			java.util.TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( "DATE '" );
				appendAsDate( appender, calendar );
				appender.appendSql( "'" );
				break;
			case TIME:
				appender.appendSql( "TIMESTAMP '" );
				final OffsetDateTime offsetDateTime = Instant.EPOCH.atOffset( ZoneOffset.UTC )
						.withHour( calendar.get( Calendar.HOUR_OF_DAY ) )
						.withMinute( calendar.get( Calendar.MINUTE ) )
						.withSecond( calendar.get( Calendar.SECOND ) )
						.withNano( calendar.get( Calendar.MILLISECOND ) * 1_000_000 );
				appendAsTimestampWithMillis( appender, offsetDateTime, true, jdbcTimeZone );
				appender.appendSql( "'" );
				break;
			case TIMESTAMP:
				appender.appendSql( "TIMESTAMP '" );
				final OffsetDateTime odt = OffsetDateTime.ofInstant(
						calendar.toInstant(),
						calendar.getTimeZone().toZoneId() );
				appendAsTimestampWithMillis(
						appender,
						odt,
						supportsTemporalLiteralOffset(),
						jdbcTimeZone
				);
				appender.appendSql( "'" );
				break;
			default:
				throw new IllegalArgumentException( "Unsupported TemporalType: " + precision );
		}
	}

	@Override
	public String translateExtractField(TemporalUnit unit) {
		switch (unit) {
			case WEEK:
				return "isoweek";
			case DAY_OF_MONTH:
				return "day";
			case DAY_OF_WEEK:
				return "dayofweek";
			case DAY_OF_YEAR:
				return "dayofyear";
			default:
				return super.translateExtractField(unit);
		}
	}

	@Override
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		if ( temporalType == TemporalType.TIMESTAMP || temporalType == TemporalType.TIME ) {
			switch ( unit ) {
				case YEAR:
				case QUARTER:
				case MONTH:
					throw new SemanticException( "Illegal unit for timestamp_add(): " + unit );
				case WEEK:
					return "timestamp_add(?3, interval cast(?2 * 7 as int64) day)";
				case SECOND:
					return "timestamp_add(?3, interval cast(?2 * 1000000000 as int64) nanosecond)";
				default:
					return "timestamp_add(?3, interval cast(?2 as int64) ?1)";
			}
		}
		else {
			switch ( unit ) {
				case NANOSECOND:
				case SECOND:
				case MINUTE:
				case HOUR:
				case NATIVE:
					throw new SemanticException( "Illegal unit for date_add(): " + unit );
				default:
					return "date_add(?3, interval cast(?2 as int64) ?1)";
			}
		}
	}

	@Override
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		if ( toTemporalType == TemporalType.TIMESTAMP || fromTemporalType == TemporalType.TIMESTAMP
			|| toTemporalType == TemporalType.TIME || fromTemporalType == TemporalType.TIME ) {
			switch ( unit ) {
				case YEAR:
				case QUARTER:
				case MONTH:
					throw new SemanticException( "Illegal unit for timestamp_diff(): " + unit );
				case WEEK:
					return "div(timestamp_diff(?3, ?2, day), 7)";
				case NATIVE:
					return "timestamp_diff(?3, ?2, nanosecond)";
				default:
					return "timestamp_diff(?3, ?2, ?1)";
			}
		}
		else {
			switch ( unit ) {
				case NANOSECOND:
				case NATIVE:
					return "(date_diff(?3, ?2, day) * 86400000000000)";
				case SECOND:
					return "(date_diff(?3, ?2, day) * 86400)";
				case MINUTE:
					return "(date_diff(?3, ?2, day) * 1440)";
				case HOUR:
					return "(date_diff(?3, ?2, day) * 24)";
				default:
					return "date_diff(?3, ?2, ?1)";
			}
		}
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
		appender.appendSql( datetimeFormat( format ).result() );
	}

	public static Replacer datetimeFormat(String format) {
		return MySQLDialect.datetimeFormat(format)

				//day of week
				.replace("EEEE", "%A")
				.replace("EEE", "%a")

				//minute
				.replace("mm", "%M")
				.replace("m", "%M")

				//month of year
				.replace("MMMM", "%B")
				.replace("MMM", "%b")
				.replace("MM", "%m")
				.replace("M", "%m")

				//week of year
				.replace("ww", "%V")
				.replace("w", "%V")
				//year for week
				.replace("YYYY", "%G")
				.replace("YYY", "%G")
				.replace("YY", "%g")
				.replace("Y", "%g")

				//timezones
				.replace("zzz", "%Z")
				.replace("zz", "%Z")
				.replace("z", "%Z")
				.replace("ZZZ", "%z")
				.replace("ZZ", "%z")
				.replace("Z", "%z")
				.replace("xxx", "%Ez")
				.replace("xx", "%z"); //note special case
	}

	@Override
	public String trimPattern(TrimSpec specification, boolean isWhitespace) {
		return switch ( specification ) {
			case LEADING -> isWhitespace ? "ltrim(?1)" : "ltrim(?1, ?2)";
			case TRAILING -> isWhitespace ? "rtrim(?1)" : "rtrim(?1, ?2)";
			default -> isWhitespace ? "trim(?1)" : "trim(?1, ?2)";
		};
	}

	/* DDL-related functions */

	@Override
	public Exporter<Table> getTableExporter() {
		return SPANNER_TABLE_EXPORTER;
	}

	@Override
	public boolean supportsColumnCheck() {
		return false;
	}

	@Override
	public String getCreateIndexString(boolean unique) {
		return unique ? "create unique null_filtered index" : "create index";
	}

	@Override
	public boolean supportsUniqueConstraints() {
		return false;
	}

	@Override
	public UniqueDelegate getUniqueDelegate() {
		return SPANNER_UNIQUE_DELEGATE;
	}

	@Override
	public String getAddForeignKeyConstraintString(
			String constraintName,
			String[] foreignKey,
			String referencedTable,
			String[] primaryKey,
			boolean referencesPrimaryKey) {
		// Spanner requires the referenced columns to specify in all cases, including
		// if the foreign key is referencing the primary key of the referenced table. Setting referencesPrimaryKey to
		// false will add all the referenced columns.
		return super.getAddForeignKeyConstraintString( constraintName, foreignKey, referencedTable, primaryKey, false );
	}

	@Override
	public boolean supportsCircularCascadeDeleteConstraints() {
		return false;
	}

	@Override
	public String getColumnDefaultString(String defaultValue) {
		if ( defaultValue != null && !defaultValue.startsWith( "(" ) ) {
			return "(" + defaultValue + ")";
		}
		return defaultValue;
	}

	@Override
	public boolean requiresNotNullBeforeDefault() {
		return true;
	}

	@Override
	public String generatedAs(String generatedAs) {
		return " as (" + generatedAs + ") stored";
	}

	@Override
	public boolean supportsNoColumnsInsert() {
		return false;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return SpannerIdentityColumnSupport.INSTANCE;
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return SPANNER_SEQUENCE_SUPPORT;
	}

	@Override
	public GenerationType getNativeValueGenerationStrategy() {
		return GenerationType.SEQUENCE;
	}

	@Override
	public boolean canCreateSchema() {
		return false;
	}

	@Override
	public String[] getCreateSchemaCommand(String schemaName) {
		throw new UnsupportedOperationException(
				"No create schema syntax supported by " + getClass().getName() );
	}

	@Override
	public String[] getDropSchemaCommand(String schemaName) {
		throw new UnsupportedOperationException(
				"No drop schema syntax supported by " + getClass().getName() );
	}

	@Override
	public String getCurrentSchemaCommand() {
		throw new UnsupportedOperationException(
				"No current schema syntax supported by " + getClass().getName() );
	}

	@Override
	public SchemaNameResolver getSchemaNameResolver() {
		// Spanner does not have a notion of database name schemas, so return "".
		return (connection, dialect) -> "";
	}

	@Override
	public boolean qualifyIndexName() {
		return false;
	}

	@Override
	public String getAddPrimaryKeyConstraintString(String constraintName) {
		throw new UnsupportedOperationException( "Cannot add primary key constraint in Cloud Spanner." );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Lock acquisition functions

	@Override
	public LockingSupport getLockingSupport() {
		return SPANNER_LOCKING_SUPPORT;
	}

	@Override
	public LockingClauseStrategy getLockingClauseStrategy(QuerySpec querySpec, LockOptions lockOptions) {
		if ( getPessimisticLockStyle() != PessimisticLockStyle.CLAUSE || lockOptions == null ) {
			return NON_CLAUSE_STRATEGY;
		}
		final var lockKind = PessimisticLockKind.interpret( lockOptions.getLockMode() );
		if ( lockKind == PessimisticLockKind.NONE ) {
			return NON_CLAUSE_STRATEGY;
		}
		if ( lockOptions.getTimeout() != null ) {
			validateSpannerLockTimeout( lockOptions.getTimeout().milliseconds() );
		}
		return buildLockingClauseStrategy(
				lockKind, RowLockStrategy.NONE, lockOptions, querySpec.getRootPathsForLocking() );
	}

	@Override
	public String getForUpdateString(LockOptions lockOptions) {
		if ( lockOptions != null && lockOptions.getTimeout() != null ) {
			validateSpannerLockTimeout( lockOptions.getTimeout().milliseconds() );
		}
		return getForUpdateString();
	}

	@Override
	public String getWriteLockString(int timeout) {
		validateSpannerLockTimeout( timeout );
		return getForUpdateString();
	}

	@Override
	public String getWriteLockString(Timeout timeout) {
		return getWriteLockString( timeout.milliseconds() );
	}

	@Override
	public String getReadLockString(int timeout) {
		validateSpannerLockTimeout( timeout );
		return getForUpdateString();
	}

	@Override
	public String getReadLockString(Timeout timeout) {
		return getReadLockString( timeout.milliseconds() );
	}

	@Override
	public String getForUpdateNowaitString() {
		throw new UnsupportedOperationException( "Spanner does not support no wait." );
	}

	@Override
	public String getForUpdateNowaitString(String aliases) {
		throw new UnsupportedOperationException( "Spanner does not support no wait." );
	}

	@Override
	public String getForUpdateSkipLockedString() {
		throw new UnsupportedOperationException( "Spanner does not support skip locked." );
	}

	@Override
	public String getForUpdateSkipLockedString(String aliases) {
		throw new UnsupportedOperationException( "Spanner does not support skip locked." );
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
	public boolean supportsOffsetInSubquery() {
		return true;
	}

	@Override
	public char openQuote() {
		return '`';
	}

	@Override
	public char closeQuote() {
		return '`';
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LimitOffsetLimitHandler.INSTANCE;
	}

	@Override
	public boolean supportsRowValueConstructorSyntax() {
		return false;
	}

	@Override
	public boolean supportsRowValueConstructorSyntaxInQuantifiedPredicates() {
		return false;
	}

	@Override
	public boolean supportsRowValueConstructorSyntaxInInList() {
		return false;
	}

	@Override
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}

	@Override
	public IdentifierHelper buildIdentifierHelper(
			IdentifierHelperBuilder builder,
			DatabaseMetaData metadata) throws SQLException {
		builder.applyReservedWords( metadata );
		builder.setAutoQuoteKeywords( true );
		builder.setAutoQuoteDollar( true );
		return super.buildIdentifierHelper( builder, metadata );
	}

	@Override
	public ScrollMode defaultScrollMode() {
		return ScrollMode.FORWARD_ONLY;
	}

	@Override
	public String getTruncateTableStatement(String tableName) {
		// spanner doesn't have truncate command, so we delete
		return "delete from " + tableName + " where true";
	}

	@Override
	public String getSetOperatorSqlString(SetOperator operator) {
		return switch ( operator ) {
			case UNION -> "union distinct";
			case INTERSECT -> "intersect distinct";
			case EXCEPT -> "except distinct";
			default -> super.getSetOperatorSqlString( operator );
		};
	}

	@Override
	public String getDual() {
		return "unnest([1])";
	}

	@Override
	public String getFromDualForSelectOnly() {
		return " from " + getDual() + " dual";
	}

	@Override
	public boolean supportsLateral() {
		// Spanner does not support the `LATERAL` keyword natively.
		// However, we return true here because `SpannerSqlAstTranslator` emulates
		// lateral joins using the `UNNEST(ARRAY(select as struct..)) alias` syntax.
		return true;
	}

	@Override
	public boolean supportsLobValueChangePropagation() {
		return false;
	}

	@Override
	public NullOrdering getNullOrdering() {
		return NullOrdering.SMALLEST;
	}

	@Override
	public boolean supportsNullPrecedence() {
		return false;
	}

	@Override
	public boolean supportsWithClauseInSubquery() {
		return false;
	}

	@Override
	public boolean supportsCteHeaderColumnList() {
		return false;
	}

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			final String sqlMessage = sqlException.getMessage();
			if ( sqlMessage != null ) {
				Matcher matcher = NOT_NULL_PATTERN.matcher( sqlMessage );
				if ( matcher.matches() ) {
					String group = matcher.group( 1 ) != null ? matcher.group( 1 ) : matcher.group( 2 );
					return new ConstraintViolationException( message, sqlException, sql, ConstraintViolationException.ConstraintKind.NOT_NULL, extractConstraintName( group ) );
				}

				matcher = UNIQUE_INDEX_PATTERN.matcher( sqlMessage );
				if ( matcher.matches() ) {
					return new ConstraintViolationException( message, sqlException, sql, ConstraintViolationException.ConstraintKind.UNIQUE, extractConstraintName( matcher.group( 1 ) ) );
				}

				if ( sqlMessage.contains( "Failed to insert row with primary key" ) ) {
					return new ConstraintViolationException( message, sqlException, sql, ConstraintViolationException.ConstraintKind.UNIQUE, null );
				}

				matcher = CHECK_PATTERN.matcher( sqlMessage );
				if ( matcher.matches() ) {
					return new ConstraintViolationException( message, sqlException, sql, ConstraintViolationException.ConstraintKind.CHECK, extractConstraintName( matcher.group( 1 ) ) );
				}

				matcher = FK_PATTERN.matcher( sqlMessage );
				if ( matcher.matches() ) {
					return new ConstraintViolationException( message, sqlException, sql, ConstraintViolationException.ConstraintKind.FOREIGN_KEY, extractConstraintName( matcher.group( 1 ) ) );
				}
			}
			return null;
		};
	}

	private String extractConstraintName(String name) {
		if ( name == null ) {
			return null;
		}
		name = name.replace( "`", "" ).replace( "\"", "" ).replace( "'", "" ).trim();
		int dotIndex = name.lastIndexOf( '.' );
		if ( dotIndex > -1 ) {
			name = name.substring( dotIndex + 1 );
		}
		return name;
	}
}
