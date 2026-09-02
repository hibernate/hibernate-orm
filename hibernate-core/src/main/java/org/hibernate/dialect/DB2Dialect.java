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

import org.hibernate.dialect.type.spi.StandardDdlTypes;

import org.hibernate.dialect.type.spi.TypeSizingProfile;
import org.hibernate.dialect.type.spi.UserDefinedTypeDdlSupport;
import org.hibernate.dialect.rowid.spi.RowIdSupport;
import org.hibernate.dialect.rowid.spi.RowIdSupports;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.schema.spi.AlterColumnTypeRequest;
import org.hibernate.dialect.schema.spi.ColumnDefinitionRequest;
import org.hibernate.dialect.schema.spi.ConstraintControlMode;
import org.hibernate.dialect.schema.spi.ConstraintControlRequest;
import org.hibernate.dialect.schema.spi.ConstraintDropMode;
import org.hibernate.dialect.schema.spi.IfExistsSupport;
import org.hibernate.dialect.schema.spi.IndexDdlRequest;
import org.hibernate.dialect.schema.spi.SchemaDropSupport;
import org.hibernate.dialect.schema.spi.TruncateRequest;

import org.hibernate.dialect.mutation.spi.MultiTableMutationSupport;
import org.hibernate.dialect.jdbc.spi.JdbcMetadataOverrides;
import org.hibernate.dialect.jdbc.spi.ParameterLimits;


import org.hibernate.dialect.sql.ast.spi.DmlTargetColumnQualifierSupport;

import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.MutationKind;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxCapability;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxSupport;
import org.hibernate.dialect.sql.ast.spi.PredicateSupport;
import org.hibernate.dialect.sql.ast.spi.NullOrderingSupport;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.SingleRowTableSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;
import org.hibernate.dialect.sql.ast.spi.ValuesListSupport;

import jakarta.persistence.TemporalType;
import jakarta.annotation.Nullable;
import org.hibernate.SPI;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.aggregate.spi.AggregateSupport;
import org.hibernate.dialect.aggregate.internal.DB2AggregateSupport;
import org.hibernate.dialect.function.CastingConcatFunction;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;
import org.hibernate.dialect.function.spi.ExpressionCoercionSupport;
import org.hibernate.dialect.function.spi.TupleCountSupport;
import org.hibernate.dialect.function.CountFunction;
import org.hibernate.dialect.function.DB2FormatEmulation;
import org.hibernate.dialect.function.DB2SubstringFunction;
import org.hibernate.dialect.function.TrimFunction;
import org.hibernate.dialect.generated.spi.GeneratedValuesSupport;
import org.hibernate.dialect.identity.internal.DB2IdentityColumnSupport;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.lock.internal.DB2LockingSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.namespace.internal.StandardNamespaceSupport;
import org.hibernate.dialect.namespace.spi.NamespaceSupport;
import org.hibernate.dialect.pagination.spi.DB2LimitHandler;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.rowsecurity.internal.DB2RowLevelSecurity;
import org.hibernate.dialect.rowsecurity.spi.RowLevelSecurity;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.sql.ast.spi.DB2SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.PostgreSQLSqlAstTranslator;
import org.hibernate.dialect.temporal.internal.DB2TemporalTableSupport;
import org.hibernate.dialect.temporal.spi.TemporalTableSupport;
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
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UserDefinedType;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
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
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.dialect.sql.ast.spi.OptionalTableUpdateOperationRequest;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractors;
import org.hibernate.tool.schema.spi.StandardTableExporter;
import org.hibernate.tool.schema.spi.StandardUserDefinedTypeExporter;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.ObjectNullResolvingJdbcType;
import org.hibernate.type.descriptor.jdbc.XmlJdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.SQLException;
import java.sql.Types;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;
import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.jdbc.spi.JdbcExceptionHelper.extractErrorCode;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BLOB;
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

/// A {@linkplain Dialect SQL dialect} for Db2 for LUW (Linux, Unix, and Windows)
/// version 11.1 and above.
///
/// Please refer to the <a href="https://www.ibm.com/docs/en/db2/12.1">Db2 documentation</a>.
///
/// This class is also the supported family base for provider Dialects derived
/// from Db2. Provider subclasses must invoke a constructor classified
/// {@link SPI.Role#IMPLEMENT IMPLEMENT}. The generated SPI inventory identifies
/// the constructors and members covered by this type-level implementation
/// contract; unclassified implementation details are not provider extension
/// points.
///
/// @see DB2iDialect
/// @see DB2zDialect
///
/// @author Gavin King
@SPI({ USE, IMPLEMENT })
public class DB2Dialect extends Dialect implements CurrentTemporalSupport, TemporalFormatSupport, TemporalOperationSupport {
	private IfExistsSupport ifExistsSupport;
	private SchemaDropSupport schemaDropSupport;


	@Override
	@SPI(IMPLEMENT)
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
	private static final NamespaceSupport NAMESPACE_SUPPORT = new StandardNamespaceSupport(
			false,
			name -> { throw new UnsupportedOperationException( "Catalog lifecycle is not supported" ); },
			name -> { throw new UnsupportedOperationException( "Catalog lifecycle is not supported" ); },
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

	@Override
	public TypeSizingProfile getTypeSizingProfile() {
		return typeSizingProfile;
	}

	final static DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 11, 1 );

	private static final Pattern DB2_VERSION_PATTERN = Pattern.compile( "(?:ARI|DSN|QSQ|SQL)(\\d\\d)(\\d\\d)(\\d)\\d?" );
	private static final int BIND_PARAMETERS_NUMBER_LIMIT = 32_767;

	private static final String FOR_READ_ONLY_SQL = " for read only with rs";
	private static final String FOR_SHARE_SQL = FOR_READ_ONLY_SQL + " use and keep share locks";
	private static final String FOR_UPDATE_SQL = FOR_READ_ONLY_SQL + " use and keep update locks";
	private static final String SKIP_LOCKED_SQL = " skip locked data";
	private static final String FOR_SHARE_SKIP_LOCKED_SQL = FOR_SHARE_SQL + SKIP_LOCKED_SQL;
	private static final String FOR_UPDATE_SKIP_LOCKED_SQL = FOR_UPDATE_SQL + SKIP_LOCKED_SQL;
	private static final SequenceInformationExtractor SEQUENCE_INFORMATION_EXTRACTOR =
			SequenceInformationExtractors.builder( "select * from syscat.sequences" )
					.sequenceNameColumn( "seqname" )
					.withoutCatalog()
					.schemaColumn( "seqschema" )
					.startValueColumn( "start" )
					.minimumValueColumn( "minvalue" )
					.maximumValueColumn( "maxvalue" )
					.build();

	private final LimitHandler limitHandler = DB2LimitHandler.INSTANCE;
	private final UniqueDelegate uniqueDelegate = createUniqueDelegate();
	private final StandardTableExporter db2TableExporter = new StandardTableExporter( this ) {
		@Override
		protected void applyAggregateColumnCheck(StringBuilder buf, AggregateColumn aggregateColumn) {
			final JdbcType jdbcType = aggregateColumn.getType().getJdbcType();
			if ( !jdbcType.isLob() && !jdbcType.isXml() ) { // LOB or XML columns can't have check constraints
				super.applyAggregateColumnCheck( buf, aggregateColumn );
			}
		}
	};
	private final Exporter<UserDefinedType> userDefinedTypeExporter = new StandardUserDefinedTypeExporter(
			this,
			new UserDefinedTypeDdlSupport(
					"",
					" instantiable mode db2sql",
					ExistenceCheckPlacement.NONE
			)
	);

	private final LockingSupport lockingSupport;

	@SPI( IMPLEMENT )
	public DB2Dialect() {
		this( MINIMUM_VERSION );
	}

	@SPI( IMPLEMENT )
	public DB2Dialect(DialectResolutionInfo info) {
		this( determinFullDatabaseVersion( info ) );
	}

	@SPI( IMPLEMENT )
	public DB2Dialect(DatabaseVersion version) {
		super( version );
		lockingSupport = buildLockingSupport();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public DatabaseVersion determineDatabaseVersion(DialectResolutionInfo info) {
		return determinFullDatabaseVersion( info );
	}

	public static DatabaseVersion determinFullDatabaseVersion(DialectResolutionInfo info) {
		String versionString = null;
		final var databaseMetadata = info.getDatabaseMetadata();
		if ( databaseMetadata != null ) {
			try {
				versionString = databaseMetadata.getDatabaseProductVersion();
			}
			catch (SQLException ex) {
				// Ignore
			}
		}
		final var databaseVersion = versionString == null ? null : parseVersion( versionString );
		return databaseVersion != null ? databaseVersion : info.makeCopyOrDefault( MINIMUM_VERSION );
	}

	public static @Nullable DatabaseVersion parseVersion(String versionString) {
		if ( versionString.length() != 9 ) {
			// The default format
			return null;
		}
		else {
			final var matcher = DB2_VERSION_PATTERN.matcher( versionString );
			if ( matcher.find() ) {
				final int majorVersion = parseInt( matcher.group( 1 ) );
				final int minorVersion = parseInt( matcher.group( 2 ) );
				final int microVersion = parseInt( matcher.group( 3 ) );
				return new SimpleDatabaseVersion( majorVersion, minorVersion, microVersion );
			}
			else {
				return null;
			}
		}
	}

	protected LockingSupport buildLockingSupport() {
		// Introduced in 11.5: https://www.ibm.com/docs/en/db2/11.5?topic=statement-concurrent-access-resolution-clause
		final boolean supportsSkipLocked = getVersion().isSameOrAfter( 11, 5 );
		return DB2LockingSupport.forDB2( supportsSkipLocked );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_VERSION;
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
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		return switch (sqlTypeCode) {
			case TINYINT -> "smallint"; // no tinyint

			// HHH-12827: map them both to the same type to avoid problems with schema update
			// Note that 31 is the maximum precision DB2 supports
			case NUMERIC -> columnType( DECIMAL );

			case BLOB -> "blob";
			case CLOB -> "clob";

			case TIMESTAMP_WITH_TIMEZONE -> "timestamp($p)";
			case TIME, TIME_WITH_TIMEZONE -> "time";

			default -> super.columnType( sqlTypeCode );
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final var ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( SQLXML, "xml", this ) );
		ddlTypeRegistry.addDescriptor(
				StandardDdlTypes.builder( BINARY, columnType( VARBINARY ), this )
						.withTypeCapacity( 254, columnType( BINARY ) )
						.build()
		);
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public MutationOperation createOptionalTableUpdateOperation(
			OptionalTableUpdateOperationRequest request) {
		final var optionalTableUpdate = request.update();
		final var factory = request.sessionFactory();
		return new PostgreSQLSqlAstTranslator<>( new SqlAstTranslationRequest.ModelMutation<>( factory, optionalTableUpdate ) )
				.createMergeOperation( optionalTableUpdate );
	}
	/// Create the unique-key strategy used by this Dialect instance.
	///
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	protected UniqueDelegate createUniqueDelegate() {
		return UniqueDelegates.nullableIndex( this );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public boolean supportsUserDefinedTypes() {
		return true;
	}

	@Override
	public PredicateSupport getPredicateSupport() {
		return PredicateSupport.builder( super.getPredicateSupport() )
				.capabilities(
						PredicateSupport.Capability.DISTINCT_FROM,
						PredicateSupport.Capability.TRUTHNESS
				)
				.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry( functionContributions );

		final var ddlTypeRegistry = functionContributions.getTypeConfiguration().getDdlTypeRegistry();
		final var functionFactory = new CommonFunctionFactory( functionContributions );
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
		functionFactory.position();
		functionFactory.overlayLength_overlay( false );
		functionFactory.median();
		functionFactory.inverseDistributionOrderedSetAggregates();
		functionFactory.stddevPopSamp();
		functionFactory.varPopSamp();
		functionFactory.varianceSamp();
		functionFactory.dateTrunc();
		functionFactory.trunc_dateTrunc();

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
		functionFactory.listagg( null );

		registerJsonFunctions( functionFactory );
		registerXmlFunctions( functionFactory );

		functionFactory.unnest_db2( getMaximumSeriesSize() );
		functionFactory.generateSeries_recursive( getMaximumSeriesSize(), false, true );

		functionFactory.hex( "hex(?1)" );
		functionFactory.sha( "hash(?1, 2)" );
		functionFactory.md5( "hash(?1, 0)" );

		functionFactory.regexpLike();
	}

	protected static void registerXmlFunctions(CommonFunctionFactory functionFactory) {
		functionFactory.xmlelement();
		functionFactory.xmlcomment();
		functionFactory.xmlforest();
		functionFactory.xmlconcat();
		functionFactory.xmlpi();
		functionFactory.xmlquery_db2();
		functionFactory.xmlexists();
		functionFactory.xmlagg();
		functionFactory.xmltable_db2();
	}

	protected void registerJsonFunctions(CommonFunctionFactory functionFactory) {
		functionFactory.jsonValue_db2();
		functionFactory.jsonQuery_no_passing();
		functionFactory.jsonExists_no_passing();
		functionFactory.jsonObject_db2();
		functionFactory.jsonArray_db2();
		functionFactory.jsonArrayAgg_db2();
		functionFactory.jsonObjectAgg_db2();
		functionFactory.jsonTable_db2( getMaximumSeriesSize() );
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

	@Override @SuppressWarnings("deprecation")
	@SPI({ USE, IMPLEMENT })
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		final var pattern = new StringBuilder();
		final String fromExpression;
		final String toExpression;
		if ( unit.isDateUnit() ) {
			fromExpression = "?2";
			toExpression = "?3";
		}
		else {
			fromExpression = switch (fromTemporalType) {
				case DATE -> "cast(?2 as timestamp)";
				case TIME -> "timestamp('1970-01-01',?2)";
				default -> "?2";
			};
			toExpression = switch (toTemporalType) {
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

	@Override @SuppressWarnings("deprecation")
	@SPI({ USE, IMPLEMENT })
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		final var pattern = new StringBuilder();
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
			@SuppressWarnings("deprecation")
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
	public void appendDateTimeLiteral(
			SqlAppender appender,
			Date date,
			@SuppressWarnings("deprecation")
			TemporalType precision,
			TimeZone jdbcTimeZone) {
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
			@SuppressWarnings("deprecation")
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
		return request.unique() ? " exclude null keys" : "";
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		return org.hibernate.dialect.sequence.spi.SequenceSupports.db2();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public RowLevelSecurity getRowLevelSecurity() {
		return DB2RowLevelSecurity.INSTANCE;
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SEQUENCE_INFORMATION_EXTRACTOR;
	}

	@Override
	public LockingSupport getLockingSupport() {
		return lockingSupport;
	}








	@Override
	public SubquerySupport getSubquerySupport() {
		return SubquerySupport.builder()
				.feature( SubquerySupport.Feature.EXISTS_IN_SELECT, false )
				.features( SubquerySupport.Feature.OFFSET, SubquerySupport.Feature.LATERAL )
				.build();
	}

	@Override
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

	public static String selectNullString(int sqlType) {
		final String literal = switch (sqlType) {
			case Types.VARCHAR, Types.CHAR -> "''";
			case Types.DATE -> "'2000-1-1'";
			case Types.TIME -> "'00:00:00'";
			case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> "'2000-1-1 00:00:00'";
			default -> "0";
		};
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
		return "alter column " + request.columnName() + " set data type " + request.columnType();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized IfExistsSupport getIfExistsSupport() {
		final var placement = getVersion().isSameOrAfter( 11, 5 )
				? ExistenceCheckPlacement.BEFORE_NAME
				: ExistenceCheckPlacement.NONE;
		if ( ifExistsSupport == null ) {
			ifExistsSupport = new IfExistsSupport(
				ExistenceCheckPlacement.NONE,
				placement,
				ExistenceCheckPlacement.NONE,
				placement
		);
		}
		return ifExistsSupport;
	}

	@Override
	public MultiTableMutationSupport getMultiTableMutationSupport() {
		return MultiTableMutationSupport.CTE;
	}

	@Override
	public TemporaryTableStrategy getGlobalTemporaryTableStrategy() {
		return TemporaryTableStrategies.db2Global();
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

		final var jdbcTypeRegistry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();

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
		return getVersion().isSameOrAfter( 11 )
				? DB2AggregateSupport.JSON_INSTANCE
				: DB2AggregateSupport.INSTANCE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public CallableStatementSupport getCallableStatementSupport() {
		return CallableStatementSupports.db2();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendBinaryLiteral(SqlAppender appender, byte[] bytes) {
		appender.appendSql( "BX'" );
		PrimitiveByteArrayJavaType.INSTANCE.appendString( appender, bytes );
		appender.appendSql( '\'' );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return new TemplatedViolatedConstraintNameExtractor(
				sqle -> switch ( extractErrorCode( sqle ) ) {
					case -803 -> {
						// Unique constraint
						final String constraintWithKind = extractUsingTemplate( "SQLERRMC=", ",", sqle.getMessage() );
						// strip off "1;" for PK, or "2;" for other UK
						yield constraintWithKind == null ? null : constraintWithKind.substring(2);
					}
					case -543, -545, -530,-531 ->
						// Foreign key or check constraint
							extractUsingTemplate( "SQLERRMC=", ",", sqle.getMessage() );
					default -> null;
				}
		);
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) ->
				switch ( extractErrorCode( sqlException ) ) {
					case -952 ->
							new LockTimeoutException( message, sqlException, sql );
					case -803 ->
							new ConstraintViolationException( message, sqlException, sql, ConstraintKind.UNIQUE,
									getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
					case -530,-531 ->
							new ConstraintViolationException( message, sqlException, sql, ConstraintKind.FOREIGN_KEY,
									getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
					case -407 ->
							new ConstraintViolationException( message, sqlException, sql, ConstraintKind.NOT_NULL,
									getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
					case -543,-545 ->
							new ConstraintViolationException( message, sqlException, sql, ConstraintKind.CHECK,
									getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
					default -> null;
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
				return new DB2SqlAstTranslator<>( request );
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
				.recursiveFeatures( CteSupport.RecursiveFeature.RECURSIVE )
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
		return switch (unit) {
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
		appender.appendSql( bool );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String extractPattern(TemporalUnit unit) {
		switch ( unit ) {
			case WEEK:
				// Not sure why, but `extract(week from '2019-05-27')`
				// wrongly returns 21 and week_iso behaves correct
				return "week_iso(?2)";
			case DAY_OF_YEAR:
				return "dayofyear(?2)";
			case DAY_OF_WEEK:
				return "dayofweek(?2)";
			case QUARTER:
				return "quarter(?2)";
		}
		return TemporalOperationSupports.standard().extractPattern( unit );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String castPattern(CastType from, CastType to) {
		return from == CastType.STRING && to == CastType.BOOLEAN
				? "cast(?1 as ?2)"
				: super.castPattern( from, to );
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
			appender.appendSql( switch ( request.generatedExpression() ) {
				case "transaction start id" -> " not null generated always as transaction start id";
				case "row start" -> " not null generated always as row begin";
				case "row end" -> " not null generated always as row end";
				default -> " generated always as (" + request.generatedExpression() + ")";
			} );
			return;
		}
		if ( !request.nullable() ) {
			appender.appendSql( " not null" );
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuildRequest request) {
		final var builder = request.builder();
		builder.setAutoQuoteInitialUnderscore( true );
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
		return MutationSyntaxSupport.of( MutationKind.UPDATE, MutationSyntaxCapability.FROM_CLAUSE );
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

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalTableSupport getTemporalTableSupport() {
		return new DB2TemporalTableSupport( this );
	}
}
