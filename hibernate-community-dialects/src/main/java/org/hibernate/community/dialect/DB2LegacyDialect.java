/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.identifier.spi.KeywordRegistration;

import org.hibernate.dialect.temporaltype.spi.TemporalValueSemantics;

import org.hibernate.dialect.temporaltype.spi.CurrentTimestampSelection;

import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupport;
import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupports;

import org.hibernate.dialect.temporaltype.spi.TemporalFormatSupport;

import org.hibernate.dialect.temporaltype.spi.CurrentTemporalSupport;

import org.hibernate.dialect.type.spi.StandardDdlTypes;

import org.hibernate.dialect.type.spi.TypeSizingProfile;

import org.hibernate.dialect.mutation.spi.MultiTableMutationSupport;
import org.hibernate.SPI;
import org.hibernate.dialect.function.spi.ExpressionCoercionSupport;
import org.hibernate.dialect.function.spi.TupleCountSupport;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;

import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.NullOrderingSupport;
import org.hibernate.dialect.sql.ast.spi.MutationKind;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxCapability;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxSupport;
import org.hibernate.dialect.sql.ast.spi.PredicateSupport;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.SingleRowTableSupport;
import org.hibernate.dialect.sql.ast.spi.ValuesListSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;

import jakarta.persistence.TemporalType;
import jakarta.annotation.Nullable;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.community.dialect.sequence.LegacyDB2SequenceSupport;
import org.hibernate.community.dialect.temptable.internal.DB2LegacyLocalTemporaryTableStrategy;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.dialect.jdbc.spi.ParameterLimits;
import org.hibernate.dialect.jdbc.spi.JdbcMetadataOverrides;
import org.hibernate.dialect.sql.ast.spi.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.aggregate.spi.AggregateSupport;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.function.CastingConcatFunction;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.CountFunction;
import org.hibernate.dialect.function.DB2FormatEmulation;
import org.hibernate.dialect.function.DB2PositionFunction;
import org.hibernate.dialect.function.DB2SubstringFunction;
import org.hibernate.dialect.function.TrimFunction;
import org.hibernate.community.dialect.identity.internal.DB2IdentityColumnSupport;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.generated.spi.GeneratedValuesSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.StandardLockingSupports;
import org.hibernate.dialect.namespace.spi.NamespaceSupport;
import org.hibernate.dialect.rowid.spi.RowIdSupport;
import org.hibernate.dialect.rowid.spi.RowIdSupports;
import org.hibernate.dialect.pagination.spi.DB2LimitHandler;
import org.hibernate.dialect.pagination.spi.LegacyDB2LimitHandler;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.schema.spi.AlterColumnTypeRequest;
import org.hibernate.dialect.schema.spi.ColumnDefinitionRequest;
import org.hibernate.dialect.schema.spi.ConstraintControlMode;
import org.hibernate.dialect.schema.spi.ConstraintControlRequest;
import org.hibernate.dialect.schema.spi.ConstraintDropMode;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.schema.spi.IfExistsSupport;
import org.hibernate.dialect.schema.spi.IndexDdlRequest;
import org.hibernate.dialect.schema.spi.SchemaDropSupport;
import org.hibernate.dialect.schema.spi.TruncateRequest;
import org.hibernate.dialect.temptable.spi.TemporaryTableStrategies;
import org.hibernate.dialect.temptable.spi.TemporaryTableStrategy;
import org.hibernate.dialect.type.spi.DB2JdbcTypes;
import org.hibernate.dialect.unique.spi.UniqueDelegates;
import org.hibernate.dialect.unique.spi.UniqueDelegate;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupportFactory;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupports;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.dialect.identifier.spi.IdentifierHelperBuildRequest;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.jdbc.spi.JdbcExceptionHelper;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UserDefinedType;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.procedure.spi.CallableStatementSupports;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.CastType;
import org.hibernate.dialect.temporaltype.spi.IntervalType;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
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
import org.hibernate.tool.schema.spi.StandardTableExporter;
import org.hibernate.tool.schema.spi.StandardUserDefinedTypeExporter;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.dialect.type.spi.UserDefinedTypeDdlSupport;
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.CharJdbcType;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.ObjectNullResolvingJdbcType;
import org.hibernate.type.descriptor.jdbc.SmallIntJdbcType;
import org.hibernate.type.descriptor.jdbc.VarbinaryJdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.type.descriptor.jdbc.XmlJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.Types;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.DECIMAL;
import static org.hibernate.type.SqlTypes.NUMERIC;
import static org.hibernate.type.SqlTypes.SQLXML;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.SqlTypes.VARCHAR;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsDate;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsLocalTime;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTimestampWithMillis;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTimestampWithNanos;

/**
 * A {@linkplain Dialect SQL dialect} for DB2.
 *
 * @author Gavin King
 */
public class DB2LegacyDialect extends Dialect implements CurrentTemporalSupport, TemporalFormatSupport, TemporalOperationSupport {
	private IfExistsSupport ifExistsSupport;
	private SchemaDropSupport schemaDropSupport;


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
	private static final NamespaceSupport NAMESPACE_SUPPORT = new CommunityNamespaceSupport(
			false,
			CommunityNamespaceSupport::unsupportedCatalog,
			CommunityNamespaceSupport::unsupportedCatalog,
			true,
			name -> new String[] { "create schema " + name },
			name -> new String[] { "drop schema " + name + " restrict" }
	);
	private static final JdbcMetadataOverrides JDBC_METADATA_OVERRIDES =
			JdbcMetadataOverrides.builder()
					.standardRefCursorSupport( JdbcMetadataOverrides.SupportOverride.UNSUPPORTED )
					.build();
	private static final RefCursorSupportFactory REF_CURSOR_SUPPORT_FACTORY =
			RefCursorSupports.jdbcType( Types.REF_CURSOR );

	private final TypeSizingProfile typeSizingProfile = TypeSizingProfile.builder( super.getTypeSizingProfile() )
			.defaultDecimalPrecision( 31 )
			.maxVarcharLength( 32_672 ).maxVarcharCapacity( 32_672 )
			.maxNVarcharLength( 32_672 ).maxNVarcharCapacity( 32_672 )
			.maxVarbinaryLength( 32_672 ).maxVarbinaryCapacity( 32_672 )
			.build();

	@Override public TypeSizingProfile getTypeSizingProfile() { return typeSizingProfile; }

	private static final int BIND_PARAMETERS_NUMBER_LIMIT = 32_767;

	private static final String FOR_READ_ONLY_SQL = " for read only with rs";
	private static final String FOR_SHARE_SQL = FOR_READ_ONLY_SQL + " use and keep share locks";
	private static final String FOR_UPDATE_SQL = FOR_READ_ONLY_SQL + " use and keep update locks";
	private static final String SKIP_LOCKED_SQL = " skip locked data";
	private static final String FOR_SHARE_SKIP_LOCKED_SQL = FOR_SHARE_SQL + SKIP_LOCKED_SQL;
	private static final String FOR_UPDATE_SKIP_LOCKED_SQL = FOR_UPDATE_SQL + SKIP_LOCKED_SQL;

	private final LimitHandler limitHandler = getDB2Version().isBefore( 11, 1 )
			? LegacyDB2LimitHandler.INSTANCE
			: DB2LimitHandler.INSTANCE;
	private final UniqueDelegate uniqueDelegate = createUniqueDelegate();
	private final StandardTableExporter db2TableExporter = new StandardTableExporter( this ) {
		@Override
		protected void applyAggregateColumnCheck(StringBuilder buf, AggregateColumn aggregateColumn) {
			final JdbcType jdbcType = aggregateColumn.getType().getJdbcType();
			if ( jdbcType.isLob() || jdbcType.isXml() ) {
				// LOB or XML columns can't have check constraints
				return;
			}
			super.applyAggregateColumnCheck( buf, aggregateColumn );
		}
	};
	private final Exporter<UserDefinedType> userDefinedTypeExporter = new StandardUserDefinedTypeExporter(
			this,
			new UserDefinedTypeDdlSupport(
					"",
					" instantiable mode db2sql",
					org.hibernate.dialect.schema.spi.ExistenceCheckPlacement.NONE
			)
	);

	private LockingSupport lockingSupport;

	public DB2LegacyDialect() {
		this( DatabaseVersion.make( 9, 0 ) );
	}

	public DB2LegacyDialect(DialectResolutionInfo info) {
		this( DB2Dialect.determinFullDatabaseVersion( info ) );
		lockingSupport = buildLockingSupport();
	}

	public DB2LegacyDialect(DatabaseVersion version) {
		super( version );
		lockingSupport = buildLockingSupport();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public DatabaseVersion determineDatabaseVersion(DialectResolutionInfo info) {
		return DB2Dialect.determinFullDatabaseVersion( info );
	}

	protected LockingSupport buildLockingSupport() {
		return StandardLockingSupports.db2( getVersion() );
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	protected void contributeKeywords(KeywordRegistration registration) {
		super.contributeKeywords( registration );
		//not keywords, at least not in DB2 11,
		//but perhaps they were in older versions?
		registration.registerKeyword( "current" );
		registration.registerKeyword( "date" );
		registration.registerKeyword( "time" );
		registration.registerKeyword( "timestamp" );
		registration.registerKeyword( "fetch" );
		registration.registerKeyword( "first" );
		registration.registerKeyword( "rows" );
		registration.registerKeyword( "only" );
	}

	/**
	 * DB2 LUW Version
	 */
	public DatabaseVersion getDB2Version() {
		return this.getVersion();
	}

	@Override
	public Exporter<Table> getTableExporter() {
		return this.db2TableExporter;
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public Exporter<UserDefinedType> getUserDefinedTypeExporter() {
		return userDefinedTypeExporter;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void contributeDefaultProperties(java.util.Properties properties) {
		super.contributeDefaultProperties( properties );
		properties.setProperty( org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE, Integer.toString( 0 ) );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public int getPreferredSqlTypeCodeForBoolean() {
		return getDB2Version().isBefore( 11 ) ? Types.SMALLINT : Types.BOOLEAN;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		return switch ( sqlTypeCode ) {
			// prior to DB2 11, the 'boolean' type existed,
			// but was not allowed as a column type
			case BOOLEAN -> getDB2Version().isBefore( 11 ) ? "smallint" : super.columnType( sqlTypeCode );
			// no tinyint
			case TINYINT -> "smallint";
			// HHH-12827: map them both to the same type to avoid problems with schema update
			// Note that 31 is the maximum precision DB2 supports
			case NUMERIC -> columnType( DECIMAL );
			case BLOB -> "blob";
			case CLOB -> "clob";
			case TIMESTAMP_WITH_TIMEZONE -> "timestamp($p)";
			case TIME, TIME_WITH_TIMEZONE -> "time";
			// should use 'binary' since version 11
			case BINARY -> getDB2Version().isBefore( 11 ) ? "char($l) for bit data" : super.columnType( sqlTypeCode );
			// should use 'varbinary' since version 11
			case VARBINARY -> getDB2Version().isBefore( 11 ) ? "varchar($l) for bit data" : super.columnType( sqlTypeCode );
			default -> super.columnType( sqlTypeCode );
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( SQLXML, "xml", this ) );

		if ( getDB2Version().isBefore( 11 ) ) {
			// should use 'binary' since version 11
			ddlTypeRegistry.addDescriptor(
					StandardDdlTypes.builder( BINARY, columnType( VARBINARY ), this )
							.withTypeCapacity( 254, columnType( BINARY ) )
							.build()
			);
		}
	}

	@SPI(IMPLEMENT)
	protected UniqueDelegate createUniqueDelegate() {
		return getDB2Version().isSameOrAfter(10,5)
				//use 'create unique index ... exclude null keys'
				? UniqueDelegates.nullableIndex( this )
				//ignore unique keys on nullable columns in earlier versions
				: UniqueDelegates.skipNullable( this );
	}

	@Override
	public PredicateSupport getPredicateSupport() {
		return PredicateSupport.builder( super.getPredicateSupport() )
				.capability(
						PredicateSupport.Capability.EXPRESSION_PLACEMENT,
						getDB2Version().isSameOrAfter( 11 )
				)
				.capability(
						PredicateSupport.Capability.DISTINCT_FROM,
						getDB2Version().isSameOrAfter( 11, 1 )
				)
				.capability(
						PredicateSupport.Capability.TRUTHNESS,
						getDB2Version().isSameOrAfter( 11 )
				)
				.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		final DdlTypeRegistry ddlTypeRegistry = functionContributions.getTypeConfiguration().getDdlTypeRegistry();
		final CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);
		// AVG by default uses the input type, so we possibly need to cast the argument type, hence a special function
		functionFactory.avg_castingNonDoubleArguments( this, SqlAstNodeRenderingMode.DEFAULT );

		functionFactory.cot();
		functionFactory.sinh();
		functionFactory.cosh();
		functionFactory.tanh();
		functionFactory.degrees();
		functionFactory.log10();
		functionFactory.radians();
		functionFactory.rand();
		functionFactory.soundex();
		functionFactory.trim2();
		functionFactory.space();
		functionFactory.repeat();
		functionContributions.getFunctionRegistry().namedDescriptorBuilder( "substr" )
				.setInvariantType(
						functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
				.setArgumentCountBetween( 2, 3 )
				.setParameterTypes(FunctionParameterType.STRING, FunctionParameterType.INTEGER, FunctionParameterType.INTEGER)
				.setArgumentListSignature( "(STRING string, INTEGER start[, INTEGER length])" )
				.register();
		functionContributions.getFunctionRegistry().register(
				"substring",
				new DB2SubstringFunction( functionContributions.getTypeConfiguration() )
		);
		functionFactory.translate();
		functionFactory.bitand();
		functionFactory.bitor();
		functionFactory.bitxor();
		functionFactory.bitnot();
		functionFactory.yearMonthDay();
		functionFactory.hourMinuteSecond();
		functionFactory.dayofweekmonthyear();
		functionFactory.weekQuarter();
		functionFactory.daynameMonthname();
		functionFactory.lastDay();
		functionFactory.toCharNumberDateTimestamp();
		functionFactory.dateTimeTimestamp();
		functionFactory.concat_pipeOperator();
		functionFactory.octetLength();
		functionFactory.ascii();
		functionFactory.char_chr();
		functionFactory.insert();
		functionFactory.characterLength_length( SqlAstNodeRenderingMode.DEFAULT );
		functionFactory.stddev();
		functionFactory.regrLinearRegressionAggregates();
		functionFactory.variance();
		functionFactory.hypotheticalOrderedSetAggregates_windowEmulation();
		if ( getDB2Version().isSameOrAfter( 11 ) ) {
			functionFactory.position();
			functionFactory.overlayLength_overlay( false );
			functionFactory.median();
			functionFactory.inverseDistributionOrderedSetAggregates();
			functionFactory.stddevPopSamp();
			functionFactory.varPopSamp();
			functionFactory.varianceSamp();
			functionFactory.dateTrunc();
			functionFactory.trunc_dateTrunc();
		}
		else {
			// Before version 11, the position function required the use of the code units
			functionContributions.getFunctionRegistry().register(
					"position",
					new DB2PositionFunction( functionContributions.getTypeConfiguration() )
			);
			// Before version 11, the overlay function required the use of the code units
			functionFactory.overlayLength_overlay( true );
			// ordered set aggregate functions are only available as of version 11, and we can't reasonably emulate them
			// so no percent_rank, cume_dist, median, mode, percentile_cont or percentile_disc
			functionContributions.getFunctionRegistry().registerAlternateKey( "stddev_pop", "stddev" );
			functionFactory.stddevSamp_sumCount();
			functionContributions.getFunctionRegistry().registerAlternateKey( "var_pop", "variance" );
			functionFactory.varSamp_sumCount();
			functionFactory.trunc_dateTrunc_trunc();
		}

		functionFactory.addYearsMonthsDaysHoursMinutesSeconds();
		functionFactory.yearsMonthsDaysHoursMinutesSecondsBetween();
		functionFactory.bitLength_pattern( "length(?1)*8" );

		// DB2 wants parameter operands to be casted to allow lengths bigger than 255
		functionContributions.getFunctionRegistry().register(
				"concat",
				new CastingConcatFunction(
						this,
						"||",
						true,
						SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER,
						functionContributions.getTypeConfiguration()
				)
		);
		// For the count distinct emulation distinct
		functionContributions.getFunctionRegistry().register(
				"count",
				new CountFunction(
						this,
						functionContributions.getTypeConfiguration(),
						SqlAstNodeRenderingMode.DEFAULT,
						"||",
						ddlTypeRegistry.getDescriptor( VARCHAR )
								.getCastTypeName(
										Size.nil(),
										functionContributions.getTypeConfiguration()
												.getBasicTypeRegistry()
												.resolve( StandardBasicTypes.STRING ),
										ddlTypeRegistry
								),
						true
				)
		);

		functionContributions.getFunctionRegistry().register(
				"format",
				new DB2FormatEmulation( functionContributions.getTypeConfiguration() )
		);

		functionContributions.getFunctionRegistry().patternDescriptorBuilder( "atan2", "atan2(?2,?1)" )
				.setInvariantType(
						functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE )
				)
				.setExactArgumentCount( 2 )
				.setParameterTypes( FunctionParameterType.NUMERIC, FunctionParameterType.NUMERIC )
				.register();

		functionContributions.getFunctionRegistry().namedDescriptorBuilder( "posstr" )
				.setInvariantType(
						functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER )
				)
				.setExactArgumentCount( 2 )
				.setParameterTypes(FunctionParameterType.STRING, FunctionParameterType.STRING)
				.setArgumentListSignature("(STRING string, STRING pattern)")
				.register();

		//trim() requires trim characters to be constant literals
		functionContributions.getFunctionRegistry().register( "trim", new TrimFunction(
				this,
				functionContributions.getTypeConfiguration(),
				SqlAstNodeRenderingMode.INLINE_PARAMETERS
		) );

		functionFactory.windowFunctions();
		if ( getDB2Version().isSameOrAfter( 9, 5 ) ) {
			functionFactory.listagg( null );

			if ( getDB2Version().isSameOrAfter( 11 ) ) {
				functionFactory.jsonValue_db2();
				functionFactory.jsonQuery_no_passing();
				functionFactory.jsonExists_no_passing();
				functionFactory.jsonObject_db2();
				functionFactory.jsonArray_db2();
				functionFactory.jsonArrayAgg_db2();
				functionFactory.jsonObjectAgg_db2();
				functionFactory.jsonTable_db2( getMaximumSeriesSize() );
			}
		}

		functionFactory.xmlelement();
		functionFactory.xmlcomment();
		functionFactory.xmlforest();
		functionFactory.xmlconcat();
		functionFactory.xmlpi();
		if ( getDB2Version().isSameOrAfter( 11 ) ) {
			functionFactory.xmlquery_db2();
			functionFactory.xmlexists();
		}
		else {
			functionFactory.xmlquery_db2_legacy();
			functionFactory.xmlexists_db2_legacy();
		}
		functionFactory.xmlagg();
		functionFactory.xmltable_db2();

		functionFactory.unnest_db2( getMaximumSeriesSize() );
		if ( getCteSupport().supports( CteSupport.RecursiveFeature.RECURSIVE ) ) {
			functionFactory.generateSeries_recursive( getMaximumSeriesSize(), false, true );
		}

		functionFactory.hex( "hex(?1)" );
		if ( getDB2Version().isSameOrAfter( 11 ) ) {
			functionFactory.sha( "hash(?1, 2)" );
			functionFactory.md5( "hash(?1, 0)" );

			functionFactory.regexpLike();
		}
	}

	/**
	 * DB2 doesn't support the {@code generate_series} function or {@code lateral} recursive CTEs,
	 * so it has to be emulated with a top level recursive CTE which requires an upper bound on the amount
	 * of elements that the series can return.
	 */
	protected int getMaximumSeriesSize() {
		return 10000;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public int getPreferredSqlTypeCodeForArray() {
		// Even if DB2 11 supports JSON functions, it's not possible to unnest a JSON array to rows, so stick to XML
		return SqlTypes.XML_ARRAY;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NamespaceSupport getNamespaceSupport() {
		return NAMESPACE_SUPPORT;
	}

	/**
	 * Since we're using {@code seconds_between()} and
	 * {@code add_seconds()}, it makes sense to use
	 * seconds as the "native" precision.
	 */
	@Override
	@SPI({ USE, IMPLEMENT })
	public long fractionalSecondPrecisionInNanos() {
		//Note that DB2 actually supports all the way up to
		//thousands-of-nanoseconds precision for timestamps!
		//i.e. timestamp(12)
		return 1_000_000_000; //seconds
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		if ( getDB2Version().isBefore( 11 ) ) {
			return timestampdiffPatternV10( unit, fromTemporalType, toTemporalType );
		}
		final StringBuilder pattern = new StringBuilder();
		final String fromExpression;
		final String toExpression;
		if ( unit.isDateUnit() ) {
			fromExpression = "?2";
			toExpression = "?3";
		}
		else {
			fromExpression = switch ( fromTemporalType ) {
				case DATE -> "cast(?2 as timestamp)";
				case TIME -> "timestamp('1970-01-01',?2)";
				default -> "?2";
			};
			toExpression = switch ( toTemporalType ) {
				case DATE -> "cast(?3 as timestamp)";
				case TIME -> "timestamp('1970-01-01',?3)";
				default -> "?3";
			};
		}
		switch ( unit ) {
			case NATIVE:
			case NANOSECOND:
				pattern.append( "(seconds_between(date_trunc('second'," );
				pattern.append( toExpression );
				pattern.append( "),date_trunc('second'," );
				pattern.append( fromExpression );
				pattern.append( "))" );
				break;
			//note: DB2 does have weeks_between()
			case MONTH:
			case QUARTER:
				// the months_between() function results
				// in a non-integral value, so trunc() it
				pattern.append( "trunc(months_between(" );
				pattern.append( toExpression );
				pattern.append( ',' );
				pattern.append( fromExpression );
				pattern.append( ')' );
				break;
			default:
				pattern.append( "?1s_between(" );
				pattern.append( toExpression );
				pattern.append( ',' );
				pattern.append( fromExpression );
				pattern.append( ')' );
		}
		switch ( unit ) {
			case NATIVE:
				pattern.append( "+(microsecond(");
				pattern.append( toExpression );
				pattern.append(")-microsecond(");
				pattern.append( fromExpression );
				pattern.append("))/1e6)" );
				break;
			case NANOSECOND:
				pattern.append( "*1e9+(microsecond(");
				pattern.append( toExpression );
				pattern.append(")-microsecond(");
				pattern.append( fromExpression );
				pattern.append("))*1e3)" );
				break;
			case MONTH:
				pattern.append( ')' );
				break;
			case QUARTER:
				pattern.append( "/3)" );
				break;
		}
		return pattern.toString();
	}

	@SuppressWarnings("deprecation")
	public static String timestampdiffPatternV10(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		final boolean isTime = fromTemporalType == TemporalType.TIME || toTemporalType == TemporalType.TIME;
		final String fromExpression;
		final String toExpression;
		if ( unit.isDateUnit() ) {
			if ( fromTemporalType == TemporalType.TIME ) {
				fromExpression = "timestamp('1970-01-01',?2)";
			}
			else {
				fromExpression = "?2";
			}
			if ( toTemporalType == TemporalType.TIME ) {
				toExpression = "timestamp('1970-01-01',?3)";
			}
			else {
				toExpression = "?3";
			}
		}
		else {
			if ( fromTemporalType == TemporalType.DATE ) {
				fromExpression = "cast(?2 as timestamp)";
			}
			else {
				fromExpression = "?2";
			}
			if ( toTemporalType == TemporalType.DATE ) {
				toExpression = "cast(?3 as timestamp)";
			}
			else {
				toExpression = "?3";
			}
		}
		switch ( unit ) {
			case NATIVE:
				if ( isTime ) {
					return "(midnight_seconds(" + toExpression + ")-midnight_seconds(" + fromExpression + "))";
				}
				else {
					return "(select (days(t2)-days(t1))*86400+(midnight_seconds(t2)-midnight_seconds(t1))+(microsecond(t2)-microsecond(t1))/1e6 " +
						"from lateral(values(" + fromExpression + ',' + toExpression + ")) as temp(t1,t2))";
				}
			case NANOSECOND:
				if ( isTime ) {
					return "(midnight_seconds(" + toExpression + ")-midnight_seconds(" + fromExpression + "))*1e9";
				}
				else {
					return "(select (days(t2)-days(t1))*86400+(midnight_seconds(t2)-midnight_seconds(t1))*1e9+(microsecond(t2)-microsecond(t1))*1e3 " +
						"from lateral(values(" + fromExpression + ',' + toExpression + ")) as temp(t1,t2))";
				}
			case SECOND:
				if ( isTime ) {
					return "(midnight_seconds(" + toExpression + ")-midnight_seconds(" + fromExpression + "))";
				}
				else {
					return "(select (days(t2)-days(t1))*86400+(midnight_seconds(t2)-midnight_seconds(t1)) " +
						"from lateral(values(" + fromExpression + ',' + toExpression + ")) as temp(t1,t2))";
				}
			case MINUTE:
				if ( isTime ) {
					return "(midnight_seconds(" + toExpression + ")-midnight_seconds(" + fromExpression + "))/60";
				}
				else {
					return "(select (days(t2)-days(t1))*1440+(midnight_seconds(t2)-midnight_seconds(t1))/60 from " +
						"lateral(values(" + fromExpression + ',' + toExpression + ")) as temp(t1,t2))";
				}
			case HOUR:
				if ( isTime ) {
					return "(midnight_seconds(" + toExpression + ")-midnight_seconds(" + fromExpression + "))/3600";
				}
				else {
					return "(select (days(t2)-days(t1))*24+(midnight_seconds(t2)-midnight_seconds(t1))/3600 " +
						"from lateral(values(" + fromExpression + ',' + toExpression + ")) as temp(t1,t2))";
				}
			case YEAR:
				return "(year(" + toExpression + ")-year(" + fromExpression + "))";
			// the months_between() function results
			// in a non-integral value, so trunc() it
			case MONTH:
				return "trunc(months_between(" + toExpression + ',' + fromExpression + "))";
			case QUARTER:
				return "trunc(months_between(" + toExpression + ',' + fromExpression + ")/3)";
			case WEEK:
				return "int((days" + toExpression + ")-days(" + fromExpression + "))/7)";
			case DAY:
				return "(days(" + toExpression + ")-days(" + fromExpression + "))";
			default:
				throw new UnsupportedOperationException( "Unsupported unit: " + unit );
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		final StringBuilder pattern = new StringBuilder();
		final String timestampExpression;
		if ( unit.isDateUnit() ) {
			if ( temporalType == TemporalType.TIME ) {
				timestampExpression = "timestamp('1970-01-01',?3)";
			}
			else {
				timestampExpression = "?3";
			}
		}
		else {
			if ( temporalType == TemporalType.DATE ) {
				timestampExpression = "cast(?3 as timestamp)";
			}
			else {
				timestampExpression = "?3";
			}
		}
		pattern.append(timestampExpression);
		pattern.append("+(");
		// DB2 supports temporal arithmetic. See https://www.ibm.com/support/knowledgecenter/en/SSEPGG_9.7.0/com.ibm.db2.luw.sql.ref.doc/doc/r0023457.html
		switch (unit) {
			case NATIVE:
				// AFAICT the native format is seconds with fractional parts after the decimal point
				pattern.append("?2) seconds");
				break;
			case NANOSECOND:
				pattern.append("(?2)/1e9) seconds");
				break;
			case WEEK:
				pattern.append("(?2)*7) days");
				break;
			case QUARTER:
				pattern.append("(?2)*3) months");
				break;
			default:
				pattern.append("?2) ?1s");
		}
		return pattern.toString();
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
				appendAsLocalTime( appender, temporalAccessor );
				appender.appendSql( '\'' );
				break;
			case TIMESTAMP:
				appender.appendSql( "timestamp '" );
				appendAsTimestampWithNanos( appender, temporalAccessor, false, jdbcTimeZone );
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
				appendAsTimestampWithNanos( appender, date, jdbcTimeZone );
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
	public String getLowercaseFunction() {
		return getDB2Version().isBefore( 9, 7 ) ? "lcase" : super.getLowercaseFunction();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized SchemaDropSupport getSchemaDropSupport() {
		if ( schemaDropSupport == null ) {
			schemaDropSupport = new SchemaDropSupport( List.of(), ConstraintDropMode.IMPLICIT, "" );
		}
		return schemaDropSupport;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String createTail(IndexDdlRequest request) {
		// we only create unique indexes, as opposed to unique constraints,
		// when the column is nullable, so safe to infer unique => nullable
		return request.unique() ? " exclude null keys" : "";
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		return getDB2Version().isBefore( 9, 7 )
				? LegacyDB2SequenceSupport.INSTANCE
				: org.hibernate.dialect.sequence.spi.SequenceSupports.db2();
	}

	private static final SequenceInformationExtractor SEQUENCE_INFORMATION_EXTRACTOR =
			SequenceInformationExtractors.builder( "select * from syscat.sequences" )
					.sequenceNameColumn( "seqname" )
					.withoutCatalog()
					.schemaColumn( "seqschema" )
					.startValueColumn( "start" )
					.minimumValueColumn( "minvalue" )
					.maximumValueColumn( "maxvalue" )
					.build();

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SEQUENCE_INFORMATION_EXTRACTOR;
	}


	@Override
	public LockingSupport getLockingSupport() {
		return lockingSupport;
	}







	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SubquerySupport getSubquerySupport() {
		return SubquerySupport.builder()
				.feature( SubquerySupport.Feature.EXISTS_IN_SELECT, false )
				.feature( SubquerySupport.Feature.OFFSET, true )
				.feature( SubquerySupport.Feature.LATERAL, getDB2Version().isSameOrAfter( 9, 1 ) )
				.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public ExpressionCoercionSupport getExpressionCoercionSupport() {
		return ExpressionCoercionSupport.builder()
				.requirements( ExpressionCoercionSupport.Requirement.CAST_NON_STRING_CONCATENATION_ARGUMENTS )
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String getSelectClauseNullString(SqlTypedMapping sqlTypeMapping, TypeConfiguration typeConfiguration) {
		return selectNullString( sqlTypeMapping.getJdbcMapping().getJdbcType().getDdlTypeCode() );
	}

	static String selectNullString(int sqlType) {
		String literal;
		switch ( sqlType ) {
			case Types.VARCHAR:
			case Types.CHAR:
				literal = "''";
				break;
			case Types.DATE:
				literal = "'2000-1-1'";
				break;
			case Types.TIME:
				literal = "'00:00:00'";
				break;
			case Types.TIMESTAMP:
			case Types.TIMESTAMP_WITH_TIMEZONE:
				literal = "'2000-1-1 00:00:00'";
				break;
			default:
				literal = "0";
		}
		return "nullif(" + literal + "," + literal + ')';
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public JdbcMetadataOverrides getJdbcMetadataOverrides() {
		return JDBC_METADATA_OVERRIDES;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public RefCursorSupportFactory getRefCursorSupportFactory() {
		return REF_CURSOR_SUPPORT_FACTORY;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public org.hibernate.dialect.schema.spi.SchemaCommentSupport getSchemaCommentSupport() {
		return org.hibernate.dialect.schema.spi.SchemaCommentSupports.commentOn();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String alterColumnType(AlterColumnTypeRequest request) {
		// would need multiple statements to 'set not null'/'drop not null', 'set default'/'drop default', 'set generated', etc
		return getVersion().isSameOrAfter( 10, 5 )
				? "alter column " + request.columnName() + " set data type " + request.columnType()
				: null;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized IfExistsSupport getIfExistsSupport() {
		if ( ifExistsSupport == null ) {
			ifExistsSupport = new IfExistsSupport(
				ExistenceCheckPlacement.NONE,
				getVersion().isSameOrAfter( 11, 5 )
						? ExistenceCheckPlacement.BEFORE_NAME
						: ExistenceCheckPlacement.NONE,
				ExistenceCheckPlacement.NONE,
				ExistenceCheckPlacement.NONE
		);
		}
		return ifExistsSupport;
	}

	@Override
	public MultiTableMutationSupport getMultiTableMutationSupport() {
		return MultiTableMutationSupport.CTE;
	}

	@Override
	public @Nullable TemporaryTableStrategy getGlobalTemporaryTableStrategy() {
		// Starting in DB2 9.7, "real" global temporary tables that can be shared between sessions
		// are supported; (obviously) data is not shared between sessions.
		return getDB2Version().isBefore( 9, 7 ) ? null : TemporaryTableStrategies.db2Global();
	}

	@Override
	public @Nullable TemporaryTableStrategy getLocalTemporaryTableStrategy() {
		// Prior to DB2 9.7, "real" global temporary tables that can be shared between sessions
		// are *not* supported; even though the DB2 command says to declare a "global" temp table
		// Hibernate treats it as a "local" temp table.
		return getDB2Version().isBefore( 9, 7 ) ? DB2LegacyLocalTemporaryTableStrategy.INSTANCE : null;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public CurrentTimestampSelection getCurrentTimestampSelection() {
		return CurrentTimestampSelection.prepared( "values current timestamp" );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalValueSemantics getTemporalValueSemantics() {
		return TemporalValueSemantics.TRUNCATING;
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public boolean supportsNationalizedMethods() {
		// See HHH-12753, HHH-18314, HHH-19201
		// Old DB2 JDBC drivers do not support setNClob, setNCharcterStream or setNString.
		// In more recent driver versions, some methods just delegate to the non-N variant, but others still fail.
		// Ultimately, let's just avoid the N variant methods on DB2 altogether
		return false;
	}

	@Override
	public TupleCountSupport getTupleCountSupport() {
		return TupleCountSupport.NONE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );

		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();

		if ( getDB2Version().isBefore( 11 ) ) {
			jdbcTypeRegistry.addDescriptor( Types.BOOLEAN, SmallIntJdbcType.INSTANCE );
			// Binary literals were only added in 11. See https://www.ibm.com/support/knowledgecenter/SSEPGG_11.1.0/com.ibm.db2.luw.sql.ref.doc/doc/r0000731.html#d79816e393
			jdbcTypeRegistry.addDescriptor( Types.VARBINARY, VarbinaryJdbcType.INSTANCE_WITHOUT_LITERALS );
		}
		// See HHH-12753
		// It seems that DB2's JDBC 4.0 support as of 9.5 does not
		// support the N-variant methods like NClob or NString.
		// Therefore here we overwrite the sql type descriptors to
		// use the non-N variants which are supported.
		jdbcTypeRegistry.addDescriptor( Types.NCHAR, CharJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( Types.NCLOB, ClobJdbcType.STREAM_BINDING );
		jdbcTypeRegistry.addDescriptor( Types.NVARCHAR, VarcharJdbcType.INSTANCE );

		jdbcTypeRegistry.addDescriptor( XmlJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( DB2JdbcTypes.struct() );

		// DB2 requires a custom binder for binding untyped nulls that resolves the type through the statement
		typeContributions.contributeJdbcType( ObjectNullResolvingJdbcType.INSTANCE );

		// Until we remove StandardBasicTypes, we have to keep this
		typeContributions.contributeType(
				new JavaObjectType(
						ObjectNullResolvingJdbcType.INSTANCE,
						typeContributions.getTypeConfiguration()
								.getJavaTypeRegistry()
								.resolveDescriptor( Object.class )
				)
		);

		typeContributions.contributeJdbcType( DB2JdbcTypes.instant() );
		typeContributions.contributeJdbcType( DB2JdbcTypes.localDate() );
		typeContributions.contributeJdbcType( DB2JdbcTypes.localTime() );
		typeContributions.contributeJdbcType( DB2JdbcTypes.localDateTime() );
		typeContributions.contributeJdbcType( DB2JdbcTypes.offsetTime() );
		typeContributions.contributeJdbcType( DB2JdbcTypes.offsetDateTime() );
		typeContributions.contributeJdbcType( DB2JdbcTypes.zonedDateTime() );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public AggregateSupport getAggregateSupport() {
		return new DB2Dialect( getDB2Version() ).getAggregateSupport();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public CallableStatementSupport getCallableStatementSupport() {
		return CallableStatementSupports.db2();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendBinaryLiteral(SqlAppender appender, byte[] bytes) {
		if ( getDB2Version().isSameOrAfter( 11 ) ) {
			appender.appendSql( "BX'" );
		}
		else {
			// This should be fine on DB2 prior to 10
			appender.appendSql( "X'" );
		}
		PrimitiveByteArrayJavaType.INSTANCE.appendString( appender, bytes );
		appender.appendSql( '\'' );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return new TemplatedViolatedConstraintNameExtractor(
				sqle -> {
					switch ( JdbcExceptionHelper.extractErrorCode( sqle ) ) {
						case -803:
							return extractUsingTemplate( "SQLERRMC=1;", ",", sqle.getMessage() );
						default:
							return null;
					}
				}
		);
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );
			switch ( errorCode ) {
				case -952:
					return new LockTimeoutException( message, sqlException, sql );
				case -803:
					return new ConstraintViolationException(
							message,
							sqlException,
							sql,
							ConstraintViolationException.ConstraintKind.UNIQUE,
							getViolatedConstraintNameExtractor().extractConstraintName( sqlException )
					);
			}
			return null;
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public int getMaxIdentifierLength() {
		return 128;
	}

	@Override
	public LimitHandler getLimitHandler() {
		return limitHandler;
	}

	@Override
	public NullOrderingSupport getNullOrderingSupport() {
		return NullOrderingSupport.builder( super.getNullOrderingSupport() )
				.capability( NullOrderingSupport.Capability.NULLS_FIRST_LAST, false )
				.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new DB2LegacySqlAstTranslator<>( request );
			}
		};
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return DB2IdentityColumnSupport.INSTANCE;
	}

	/**
	 * @return {@code true} because we can use {@code select ... from new table (insert .... )}
	 */
	@Override
	public GeneratedValuesSupport getGeneratedValuesSupport() {
		return GeneratedValuesSupport.builder( super.getGeneratedValuesSupport() )
				.enable(
						GeneratedValuesSupport.Capability.INSERT_RETURNING,
						GeneratedValuesSupport.Capability.UPDATE_RETURNING
				)
				.build();
	}

	@Override
	public ValuesListSupport getValuesListSupport() {
		return ValuesListSupport.STANDARD;
	}

	@Override
	public CteSupport getCteSupport() {
		// Recursive CTEs are supported at last since 9.7
		return CteSupport.builder()
				.placement( CteSupport.Placement.TOP_LEVEL )
				.recursiveFeature(
						CteSupport.RecursiveFeature.RECURSIVE,
						getDB2Version().isSameOrAfter( 9, 7 )
				)
				.mutationFeatures( CteSupport.MutationFeature.NON_QUERY )
				.requiresRecursiveKeyword( false )
				.build();
	}

	@Override
	public WindowFunctionSupport getWindowFunctionSupport() {
		return WindowFunctionSupport.builder()
				.features(
						WindowFunctionSupport.Feature.WINDOW_FUNCTIONS,
						WindowFunctionSupport.Feature.PARTITION_BY,
						WindowFunctionSupport.Feature.ROWS_FRAME,
						WindowFunctionSupport.Feature.RANGE_FRAME
				)
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendFormat(SqlAppender appender, String format) {
		//DB2 does not need nor support FM
		appender.appendSql( OracleDialect.datetimeFormat( format, false, false ).result() );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String translateExtractField(TemporalUnit unit) {
		return switch ( unit ) {
			//WEEK means the ISO week number on DB2
			case DAY_OF_MONTH -> "day";
			case DAY_OF_YEAR -> "doy";
			case DAY_OF_WEEK -> "dow";
			default -> TemporalOperationSupports.standard().translateExtractField( unit );
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		if ( getDB2Version().isBefore( 11 ) ) {
			appender.appendSql( bool ? '1' : '0' );
		}
		else {
			appender.appendSql( bool );
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String extractPattern(TemporalUnit unit) {
		switch ( unit ) {
			case WEEK:
				// Not sure why, but `extract(week from '2019-05-27')` wrongly returns 21 and week_iso behaves correct
				return "week_iso(?2)";
			case DAY_OF_YEAR:
				return "dayofyear(?2)";
			case DAY_OF_WEEK:
				return "dayofweek(?2)";
			case QUARTER:
				return "quarter(?2)";
			case EPOCH:
				if ( getDB2Version().isBefore( 11 ) ) {
					return timestampdiffPattern( TemporalUnit.SECOND, TemporalType.TIMESTAMP, TemporalType.TIMESTAMP )
							.replace( "?2", "'1970-01-01 00:00:00'" )
							.replace( "?3", "?2" );
				}
		}
		return TemporalOperationSupports.standard().extractPattern( unit );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String castPattern(CastType from, CastType to) {
		if ( from == CastType.STRING && to == CastType.BOOLEAN ) {
			return "cast(?1 as ?2)";
		}
		else {
			return super.castPattern( from, to );
		}
	}

	@Override
	public ParameterLimits getParameterLimits() {
		return ParameterLimits.of( BIND_PARAMETERS_NUMBER_LIMIT );
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
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuildRequest request) {
		final var builder = request.builder();
		builder.setAutoQuoteInitialUnderscore(true);
		return super.buildIdentifierHelper( request );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public ConstraintControlMode constraintControlMode() {
		return ConstraintControlMode.PER_CONSTRAINT;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public List<String> disableConstraintCommands(ConstraintControlRequest request) {
		return List.of( "alter table " + request.tableName() + " alter foreign key "
				+ request.constraintName() + " not enforced" );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public List<String> enableConstraintCommands(ConstraintControlRequest request) {
		return List.of( "alter table " + request.tableName() + " alter foreign key "
				+ request.constraintName() + " enforced" );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public List<String> renderCommands(TruncateRequest request) {
		return request.tableNames().stream().map( name -> "truncate table " + name + " immediate" ).toList();
	}

	/**
	 * The more "standard" syntax is {@code rid_bit(alias)} but here we use {@code alias.rowid}.
	 * <p>
	 * There is also an alternative {@code rid()} of type {@code bigint}, but it cannot be used
	 * with partitioning.
	 */
	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public RowIdSupport getRowIdSupport() {
		return RowIdSupports.fixed( "rowid", VARBINARY );
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}

	@Override
	public MutationSyntaxSupport getMutationSyntaxSupport() {
		return MutationSyntaxSupport.builder()
				.capability(
						MutationKind.UPDATE,
						MutationSyntaxCapability.FROM_CLAUSE,
						getDB2Version().isSameOrAfter( 11 )
				)
				.build();
	}

	@Override
	public SingleRowTableSupport getSingleRowTableSupport() {
		final String tableExpression = "sysibm.sysdummy1";
		return SingleRowTableSupport.builder( super.getSingleRowTableSupport() )
				.tableExpression( tableExpression )
				.selectOnlyFromClause( " from " + tableExpression )
				.build();
	}

	@Override
	public RowValueSupport getRowValueSupport() {
		return RowValueSupport.builder( RowValueSupport.NONE )
				.feature( RowValueSupport.Feature.IN_SUBQUERY, true )
				.build();
	}

}
