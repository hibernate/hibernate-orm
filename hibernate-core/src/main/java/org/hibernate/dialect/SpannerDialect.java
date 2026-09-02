/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

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


import org.hibernate.dialect.array.spi.ArraySupport;
import org.hibernate.dialect.function.spi.Replacer;

import org.hibernate.dialect.lock.spi.RowLockStrategy;

import org.hibernate.dialect.sql.ast.spi.NullOrdering;

import org.hibernate.dialect.sql.ast.spi.DmlTargetColumnQualifierSupport;

import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.MutationKind;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxCapability;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxSupport;
import org.hibernate.dialect.sql.ast.spi.NullOrderingSupport;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.SingleRowTableSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;

import jakarta.persistence.GenerationType;
import jakarta.persistence.TemporalType;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.spi.TupleCountSupport;
import org.hibernate.dialect.function.array.SpannerArrayConcatElementFunction;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.dialect.identifier.spi.IdentifierHelperBuildRequest;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.dialect.function.InsertSubstringOverlayEmulation;
import org.hibernate.dialect.function.CastingConcatFunction;
import org.hibernate.dialect.function.SpannerFormatFunction;
import org.hibernate.dialect.function.SpannerExtractFunction;
import org.hibernate.dialect.function.SpannerTruncFunction;
import org.hibernate.dialect.function.array.ArrayAggFunction;
import org.hibernate.dialect.function.array.ArrayToStringFunction;
import org.hibernate.dialect.identity.internal.SpannerIdentityColumnSupport;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.namespace.spi.NamespaceSupport;
import org.hibernate.dialect.namespace.spi.NamespaceSupports;
import org.hibernate.dialect.schema.spi.ColumnDefinitionRequest;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.schema.spi.IfExistsSupport;
import org.hibernate.dialect.schema.spi.IndexDdlRequest;
import org.hibernate.dialect.schema.spi.IndexNameQualification;
import org.hibernate.dialect.schema.spi.TruncateRequest;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.LimitOffsetLimitHandler;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.sequence.internal.SpannerSequenceSupport;
import org.hibernate.dialect.schema.internal.SpannerDialectTableExporter;
import org.hibernate.dialect.type.spi.SpannerJdbcTypes;
import org.hibernate.dialect.function.json.SpannerJsonValueFunction;
import org.hibernate.dialect.function.json.SpannerJsonQueryFunction;
import org.hibernate.dialect.sql.ast.internal.SpannerSqlAstTranslator;
import org.hibernate.dialect.unique.spi.UniqueDelegates;
import org.hibernate.dialect.unique.spi.UniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.SchemaNameResolver;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.dialect.sql.ast.spi.OptionalTableUpdateOperationRequest;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.mapping.Table;
import org.hibernate.query.SemanticException;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.CastType;
import org.hibernate.dialect.temporaltype.spi.IntervalType;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.query.sqm.TrimSpec;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.internal.LockingSupportSimple;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.dialect.lock.spi.LockingClauseStrategy;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;

import static org.hibernate.dialect.array.spi.ArraySupport.Capability.STANDARD_ARRAY;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.dialect.function.CountFunction;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractors;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.hibernate.Timeouts;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hibernate.dialect.SimpleDatabaseVersion.ZERO_VERSION;
import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.useArgType;
import static org.hibernate.dialect.lock.internal.NonLockingClauseStrategy.NON_CLAUSE_STRATEGY;
import static org.hibernate.type.SqlTypes.ARRAY;
import static org.hibernate.type.SqlTypes.BIGINT;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.JSON;
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
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.SqlTypes.VARCHAR;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsDate;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTimestampWithMillis;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTimestampWithNanos;

/**
 * A {@linkplain Dialect SQL dialect} for Cloud Spanner.
 *
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 * @author Daniel Zou
 * @author Dmitry Solomakha
 * @author Rayudu Abbireddy
 */
public class SpannerDialect extends Dialect implements CurrentTemporalSupport, TemporalFormatSupport, TemporalOperationSupport {
	private IfExistsSupport ifExistsSupport;


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
			.maxVarcharLength( 2_621_440 ).maxVarcharCapacity( 2_621_440 )
			.maxNVarcharLength( 2_621_440 ).maxNVarcharCapacity( 2_621_440 )
			.maxVarbinaryLength( 10_485_760 ).maxVarbinaryCapacity( 10_485_760 )
			.build();

	@Override
	public TypeSizingProfile getTypeSizingProfile() {
		return typeSizingProfile;
	}
	private static final ArraySupport ARRAY_SUPPORT = ArraySupport.builder()
			.capabilities( STANDARD_ARRAY )
			.multiValuedParameterStrategy( ArraySupport.MultiValuedParameterStrategy.ARRAY )
			.build();

	private final UniqueDelegate SPANNER_UNIQUE_DELEGATE = UniqueDelegates.alwaysIndex( this );
	private final Exporter<Table> SPANNER_TABLE_EXPORTER = new SpannerDialectTableExporter( this );
	private final SequenceSupport SPANNER_SEQUENCE_SUPPORT = new SpannerSequenceSupport(this);

	private static final Pattern NOT_NULL_PATTERN = Pattern.compile( ".*Cannot specify a null value for column(?:[:]? (.*?) in table|: (.*?(?=$))).*" );
	private static final Pattern NOT_NULL_PATTERN_2 = Pattern.compile( ".*A new row in table .* does not specify a non-null value for NOT NULL column: (.*?)\\s*" );
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
	@SPI({ IMPLEMENT, SUPPLY })
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );
		final var configurationService = serviceRegistry.requireService( ConfigurationService.class );
		this.useIntegerForPrimaryKey = configurationService.getSetting(
				USE_INTEGER_FOR_PRIMARY_KEY,
				StandardConverters.BOOLEAN,
				false
		);
		final var jdbcTypeRegistry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();
		jdbcTypeRegistry.addDescriptor( SpannerJdbcTypes.json() );
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public MutationOperation createOptionalTableUpdateOperation(
			OptionalTableUpdateOperationRequest request) {
		final var optionalTableUpdate = request.update();
		final var factory = request.sessionFactory();
		final boolean hasUpdatableBindings = optionalTableUpdate.getValueBindings().stream()
				.anyMatch( ColumnValueBinding::isAttributeUpdatable );
		if ( hasUpdatableBindings ) {
			// If an entity contains BOTH updatable properties and read-only properties
			// (like `@Column(updatable = false)`), we MUST fall back to Hibernate's core UPDATE-then-INSERT
			// mutation workflow to protect the immutable state from being unintentionally overwritten,
			// as Spanner's native `INSERT OR UPDATE` statement updates all columns.
			// Spanner's native `INSERT OR UPDATE` statement does not support a `WHERE` clause,
			// so optimistic locking checks cannot be applied there.
			final boolean hasNonUpdatableBindings = optionalTableUpdate.getValueBindings().stream()
					.anyMatch( binding -> !binding.isAttributeUpdatable() );
			if ( hasNonUpdatableBindings || optionalTableUpdate.getNumberOfOptimisticLockBindings() > 0 ) {
				return super.createOptionalTableUpdateOperation( request );
			}
		}
		return new SpannerSqlAstTranslator<>( new SqlAstTranslationRequest.ModelMutation<>( factory, optionalTableUpdate ) )
				.createMergeOperation( optionalTableUpdate, hasUpdatableBindings );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void contributeDefaultProperties(java.util.Properties properties) {
		super.contributeDefaultProperties( properties );
		properties.setProperty( AvailableSettings.PREFERRED_POOLED_OPTIMIZER, "none" );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public JdbcType resolveSqlTypeDescriptor(
			String columnTypeName,
			int jdbcTypeCode,
			int precision,
			int scale,
			JdbcTypeRegistry jdbcTypeRegistry) {
		if ( jdbcTypeCode == ARRAY ) {
			final int startIndex = columnTypeName.indexOf( '<' ) + 1;
			final int endIndex = columnTypeName.lastIndexOf( '>' );
			final String componentTypeName = columnTypeName.substring( startIndex, endIndex ).trim();
			// Spanner uses STRING for VARCHAR/CLOB. DdlTypeRegistry prefers CLOB for "string".
			final Integer sqlTypeCode = componentTypeName.equalsIgnoreCase( "STRING" )
					? VARCHAR
					: resolveSqlTypeCode( componentTypeName, jdbcTypeRegistry.getTypeConfiguration() );
			if ( sqlTypeCode != null ) {
				return jdbcTypeRegistry.resolveTypeConstructorDescriptor(
						jdbcTypeCode,
						jdbcTypeRegistry.getDescriptor( sqlTypeCode ),
						ColumnTypeInformation.EMPTY
				);
			}
		}
		return super.resolveSqlTypeDescriptor( columnTypeName, jdbcTypeCode, precision, scale, jdbcTypeRegistry );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( JSON, columnType( JSON ),this ));
		ddlTypeRegistry.addDescriptor(
				StandardDdlTypes.builder( VARCHAR, columnType( VARCHAR ), this )
						.castTypeNamePattern( castType( VARCHAR ) )
						.castTypeName( castType( VARCHAR ) )
				.withTypeCapacity( getTypeSizingProfile().maxVarcharLength(), columnType( VARCHAR ) )
				.build()
		);
		ddlTypeRegistry.addDescriptor(
				StandardDdlTypes.builder( NVARCHAR, columnType( NVARCHAR ), this )
						.castTypeNamePattern( castType( NVARCHAR ) )
						.castTypeName( castType( NVARCHAR ) )
				.withTypeCapacity( getTypeSizingProfile().maxNVarcharLength(), columnType( NVARCHAR ) )
				.build()
		);
		ddlTypeRegistry.addDescriptor(
				StandardDdlTypes.builder( VARBINARY, columnType( VARBINARY ), this )
						.castTypeNamePattern( castType( VARBINARY ) )
						.castTypeName( castType( VARBINARY ) )
				.withTypeCapacity( getTypeSizingProfile().maxVarbinaryLength(), columnType( VARBINARY ) )
				.build()
		);
	}

	@Override
	@SPI({ USE, IMPLEMENT })
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
			case JSON -> "json";
			default -> super.columnType( sqlTypeCode );
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
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
	@SPI({ USE, IMPLEMENT })
	protected String castType(int sqlTypeCode) {
		return switch ( sqlTypeCode ) {
			case CHAR, NCHAR, VARCHAR, NVARCHAR, LONG32VARCHAR, LONG32NVARCHAR, CLOB, NCLOB -> "string";
			case BINARY, VARBINARY, LONG32VARBINARY, BLOB -> "bytes";
			default -> super.castType( sqlTypeCode );
		};
	}

	@Override
	public ArraySupport getArraySupport() {
		return ARRAY_SUPPORT;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String getArrayTypeName(String javaElementTypeName, String elementTypeName, Integer maxLength) {
		return "ARRAY<" + elementTypeName + ">";
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
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
		functionRegistry.register(
				"count",
				new CountFunction(
						this,
						functionContributions.getTypeConfiguration(),
						SqlAstNodeRenderingMode.DEFAULT,
						"||",
						"string",
						false
				)
		);
		functionRegistry.register( ArrayAggFunction.FUNCTION_NAME, new ArrayAggFunction( "array_agg", false, true ) );
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

		functionFactory.bitandorxornot_operator();

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
		functionFactory.octetLength();
		functionFactory.bitLength_pattern( "(octet_length(?1) * 8)" );
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
		functionRegistry.register(
				"json_value",
				new SpannerJsonValueFunction( functionContributions.getTypeConfiguration() ) );
		functionRegistry.register(
				"json_query",
				new SpannerJsonQueryFunction( functionContributions.getTypeConfiguration() ) );

		// Array Functions
		functionRegistry.namedDescriptorBuilder( "array" )
				.setExactArgumentCount( 1 )
				.register();
		functionFactory.arrayConcat_operator();
		functionRegistry.register( "array_append", new SpannerArrayConcatElementFunction( false ) );
		functionRegistry.register( "array_prepend", new SpannerArrayConcatElementFunction( true ) );
		functionFactory.arrayLength_spanner();
		functionRegistry.register( "array_to_string", new ArrayToStringFunction( functionContributions.getTypeConfiguration() ) );
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
		functionFactory.array_spanner();

		functionRegistry.register(
				"extract",
				new SpannerExtractFunction( this, functionContributions.getTypeConfiguration() )
		);

		functionRegistry.register(
				"format",
				new SpannerFormatFunction( functionContributions.getTypeConfiguration() )
		);

		functionRegistry.register(
				"concat",
				new CastingConcatFunction(
						this,
						"||",
						false,
						SqlAstNodeRenderingMode.DEFAULT,
						functionContributions.getTypeConfiguration()
				)
		);

		functionRegistry.register( "trunc", new SpannerTruncFunction() );
		functionRegistry.registerAlternateKey( "truncate", "trunc" );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new SpannerSqlAstTranslator<>( request );
			}
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized IfExistsSupport getIfExistsSupport() {
		if ( ifExistsSupport == null ) {
			ifExistsSupport = new IfExistsSupport(
				ExistenceCheckPlacement.BEFORE_NAME,
				ExistenceCheckPlacement.BEFORE_NAME,
				ExistenceCheckPlacement.NONE,
				ExistenceCheckPlacement.NONE
		);
		}
		return ifExistsSupport;
	}

	/* SELECT-related functions */

	@Override
	@SPI({ USE, IMPLEMENT })
	public CurrentTimestampSelection getCurrentTimestampSelection() {
		return CurrentTimestampSelection.prepared( "select current_timestamp() as now" );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		appender.appendSql( bool );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendBinaryLiteral(SqlAppender appender, byte[] bytes) {
		appender.appendSql( "FROM_HEX('" );
		PrimitiveByteArrayJavaType.INSTANCE.appendString( appender, bytes );
		appender.appendSql( "')" );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
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
	@SPI({ USE, IMPLEMENT })
	public String currentTime() {
		return currentTimestamp();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalValueSemantics getTemporalValueSemantics() {
		return TemporalValueSemantics.OFFSET_LITERALS;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendDateTimeLiteral(
			org.hibernate.sql.spi.SqlAppender appender,
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
							getTemporalValueSemantics().supportsLiteralOffset(),
							jdbcTimeZone
					);
				}
				else {
					appendAsTimestampWithNanos(
							appender,
							temporalAccessor,
							getTemporalValueSemantics().supportsLiteralOffset(),
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
	@SPI({ USE, IMPLEMENT })
	public void appendDateTimeLiteral(
			org.hibernate.sql.spi.SqlAppender appender,
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
						getTemporalValueSemantics().supportsLiteralOffset(),
						jdbcTimeZone
				);
				appender.appendSql( "'" );
				break;
			default:
				throw new IllegalArgumentException( "Unsupported TemporalType: " + precision );
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendDateTimeLiteral(
			org.hibernate.sql.spi.SqlAppender appender,
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
						getTemporalValueSemantics().supportsLiteralOffset(),
						jdbcTimeZone
				);
				appender.appendSql( "'" );
				break;
			default:
				throw new IllegalArgumentException( "Unsupported TemporalType: " + precision );
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
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
				return TemporalOperationSupports.standard().translateExtractField(unit);
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
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
	@SPI({ USE, IMPLEMENT })
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
	@SPI({ USE, IMPLEMENT })
	public void appendFormat(SqlAppender appender, String format) {
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
	@SPI({ USE, IMPLEMENT })
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
	@SPI({ USE, IMPLEMENT })
	public boolean supports(org.hibernate.dialect.constraint.spi.CheckConstraintPlacement placement) {
		return placement == org.hibernate.dialect.constraint.spi.CheckConstraintPlacement.TABLE;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String createCommand(IndexDdlRequest request) {
		return request.unique() ? "create unique null_filtered index" : "create index";
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public UniqueDelegate getUniqueDelegate() {
		return SPANNER_UNIQUE_DELEGATE;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String renderAddConstraint(
			org.hibernate.dialect.constraint.spi.ForeignKeyConstraintRequest request) {
		// Spanner requires the referenced columns to specify in all cases, including
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
	public void appendDefinition(SqlAppender appender, ColumnDefinitionRequest request) {
		appender.appendSql( ' ' );
		appender.appendSql( request.sqlType() );
		if ( request.renderedCollation() != null ) {
			appender.appendSql( " collate " );
			appender.appendSql( request.renderedCollation() );
		}
		if ( !request.nullable() ) {
			appender.appendSql( " not null" );
		}
		if ( request.defaultExpression() != null ) {
			appender.appendSql( " default " );
			if ( !request.defaultExpression().startsWith( "(" ) ) {
				appender.appendSql( '(' );
				appender.appendSql( request.defaultExpression() );
				appender.appendSql( ')' );
			}
			else {
				appender.appendSql( request.defaultExpression() );
			}
		}
		if ( request.generatedExpression() != null ) {
			appender.appendSql( " as (" );
			appender.appendSql( request.generatedExpression() );
			appender.appendSql( ") stored" );
		}
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return SpannerIdentityColumnSupport.INSTANCE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		return SPANNER_SEQUENCE_SUPPORT;
	}

	private static final SequenceInformationExtractor SEQUENCE_INFORMATION_EXTRACTOR =
			SequenceInformationExtractors.builder( """
				select seq.CATALOG as sequence_catalog,
					seq.SCHEMA as sequence_schema,
					seq.NAME as sequence_name,
					coalesce(kind.OPTION_VALUE, 'bit_reversed_positive') as KIND,
					coalesce(safe_cast(initial.OPTION_VALUE AS INT64),
						case coalesce(kind.OPTION_VALUE, 'bit_reversed_positive')
							when 'bit_reversed_positive' then 1
							when 'bit_reversed_signed' then -pow(2, 63)
							else 1
						end
					) as start_value, 1 as minimum_value, 9223372036854775807 as maximum_value,
					1 as increment,
					safe_cast(skip_range_min.OPTION_VALUE as int64) as skip_range_min,
					safe_cast(skip_range_max.OPTION_VALUE as int64) as skip_range_max
				from INFORMATION_SCHEMA.SEQUENCES seq
				left outer join INFORMATION_SCHEMA.SEQUENCE_OPTIONS kind
					on seq.CATALOG=kind.CATALOG and seq.SCHEMA=kind.SCHEMA and seq.NAME=kind.NAME and kind.OPTION_NAME='sequence_kind'
				left outer join INFORMATION_SCHEMA.SEQUENCE_OPTIONS initial
					on seq.CATALOG=initial.CATALOG and seq.SCHEMA=initial.SCHEMA and seq.NAME=initial.NAME and initial.OPTION_NAME='start_with_counter'
				left outer join INFORMATION_SCHEMA.SEQUENCE_OPTIONS skip_range_min
					on seq.CATALOG=skip_range_min.CATALOG and seq.SCHEMA=skip_range_min.SCHEMA and seq.NAME=skip_range_min.NAME and skip_range_min.OPTION_NAME='skip_range_min'
				left outer join INFORMATION_SCHEMA.SEQUENCE_OPTIONS skip_range_max
					on seq.CATALOG=skip_range_max.CATALOG and seq.SCHEMA=skip_range_max.SCHEMA and seq.NAME=skip_range_max.NAME and skip_range_max.OPTION_NAME='skip_range_max'
				""" ).build();

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SEQUENCE_INFORMATION_EXTRACTOR;
	}

	@Override
	public GenerationType getNativeValueGenerationStrategy() {
		return GenerationType.SEQUENCE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NamespaceSupport getNamespaceSupport() {
		return NamespaceSupports.none();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SchemaNameResolver getSchemaNameResolver() {
		// Spanner does not have a notion of database name schemas, so return "".
		return (connection, dialect) -> "";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public IndexNameQualification nameQualification() {
		return IndexNameQualification.UNQUALIFIED;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Lock acquisition functions

	@Override
	public LockingSupport getLockingSupport() {
		return SPANNER_LOCKING_SUPPORT;
	}

	@Override
	public LockingClauseStrategy getLockingClauseStrategy(QuerySpec querySpec, LockOptions lockOptions) {
		if ( getLockingSupport().getMetadata().getPessimisticLockStyle() != PessimisticLockStyle.CLAUSE
				|| lockOptions == null ) {
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
	public SubquerySupport getSubquerySupport() {
		// Spanner does not support the LATERAL keyword natively, but its
		// translator emulates lateral joins with UNNEST(ARRAY(select as struct ...)).
		return SubquerySupport.builder()
				.features( SubquerySupport.Feature.OFFSET, SubquerySupport.Feature.LATERAL )
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public char openQuote() {
		return '`';
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public char closeQuote() {
		return '`';
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LimitOffsetLimitHandler.INSTANCE;
	}

	@Override
	public RowValueSupport getRowValueSupport() {
		return RowValueSupport.NONE;
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuildRequest request) {
		final var builder = request.builder();
		if ( !request.jdbcMetadata().isJdbcMetadataAccessible() ) {
			builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		}
		builder.setAutoQuoteKeywords( true );
		builder.setAutoQuoteDollar( true );
		return super.buildIdentifierHelper( request );
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public ScrollMode defaultScrollMode() {
		return ScrollMode.FORWARD_ONLY;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public List<String> renderCommands(TruncateRequest request) {
		// Spanner doesn't have a truncate command, so delete every table.
		return request.tableNames().stream().map( name -> "delete from " + name + " where true" ).toList();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String getSetOperatorSqlString(SetOperator operator) {
		return switch ( operator ) {
			case UNION -> "union distinct";
			case INTERSECT -> "intersect distinct";
			case EXCEPT -> "except distinct";
			default -> super.getSetOperatorSqlString( operator );
		};
	}

	@Override
	public SingleRowTableSupport getSingleRowTableSupport() {
		final String tableExpression = "unnest([1])";
		return SingleRowTableSupport.builder( super.getSingleRowTableSupport() )
				.tableExpression( tableExpression )
				.selectOnlyFromClause( " from " + tableExpression + " dual" )
				.build();
	}

	@Override
	public NullOrderingSupport getNullOrderingSupport() {
		return NullOrderingSupport.builder( super.getNullOrderingSupport() )
				.defaultOrdering( NullOrdering.SMALLEST )
				.capability( NullOrderingSupport.Capability.NULLS_FIRST_LAST, false )
				.build();
	}

	@Override
	public CteSupport getCteSupport() {
		return CteSupport.builder()
				.placement( CteSupport.Placement.TOP_LEVEL )
				.supportsCteHeaderColumnList( false )
				.build();
	}

	@Override
	public MutationSyntaxSupport getMutationSyntaxSupport() {
		return MutationSyntaxSupport.builder()
				.capability( MutationKind.UPDATE, MutationSyntaxCapability.REQUIRES_WHERE )
				.capability( MutationKind.DELETE, MutationSyntaxCapability.REQUIRES_WHERE )
				.build();
	}

	@Override
	public TupleCountSupport getTupleCountSupport() {
		return TupleCountSupport.NONE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			final String sqlMessage = sqlException.getMessage();
			if ( sqlMessage != null ) {
				Matcher matcher = NOT_NULL_PATTERN.matcher( sqlMessage );
				if ( matcher.matches() ) {
					String group = matcher.group( 1 ) != null ? matcher.group( 1 ) : matcher.group( 2 );
					return new ConstraintViolationException( message, sqlException, sql, ConstraintViolationException.ConstraintKind.NOT_NULL, extractConstraintName( group ) );
				}

				matcher = NOT_NULL_PATTERN_2.matcher( sqlMessage );
				if ( matcher.matches() ) {
					return new ConstraintViolationException( message, sqlException, sql, ConstraintViolationException.ConstraintKind.NOT_NULL, extractConstraintName( matcher.group( 1 ) ) );
				}

				matcher = UNIQUE_INDEX_PATTERN.matcher( sqlMessage );
				if ( matcher.matches() ) {
					return new ConstraintViolationException( message, sqlException, sql, ConstraintViolationException.ConstraintKind.UNIQUE, extractConstraintName( matcher.group( 1 ) ) );
				}

				if ( sqlMessage.contains( "Failed to insert row with primary key" ) ) {
					return new ConstraintViolationException( message, sqlException, sql, ConstraintViolationException.ConstraintKind.UNIQUE, null );
				}

				if ( sqlMessage.contains( "Table not found" ) ) {
					return new SQLGrammarException( message, sqlException, sql );
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
