/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.temporaltype.spi.CurrentTimestampSelection;

import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupport;
import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupports;

import org.hibernate.dialect.temporaltype.spi.TemporalFormatSupport;

import org.hibernate.dialect.temporaltype.spi.CurrentTemporalSupport;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

import org.hibernate.dialect.type.spi.StandardDdlTypes;

import org.hibernate.dialect.type.spi.TypeSizingProfile;

import org.hibernate.dialect.mutation.spi.MultiTableMutationSupport;
import org.hibernate.dialect.function.spi.TupleCountSupport;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;

import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.MutationKind;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxCapability;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxSupport;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.SingleRowTableSupport;
import org.hibernate.dialect.sql.ast.spi.SetOperationSupport;
import org.hibernate.dialect.sql.ast.spi.ValuesListSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;

import java.sql.Types;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.persistence.GenerationType;
import jakarta.persistence.TemporalType;
import org.hibernate.QueryTimeoutException;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.community.dialect.pagination.LegacyOracleLimitHandler;
import org.hibernate.dialect.type.spi.BooleanDecoder;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.dialect.lob.spi.LobSupport;
import org.hibernate.dialect.lob.spi.LobSupports;
import org.hibernate.dialect.rowid.spi.RowIdSupport;
import org.hibernate.dialect.rowid.spi.RowIdSupports;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.sql.ast.spi.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.jdbc.spi.OracleServerConfiguration;
import org.hibernate.dialect.jdbc.spi.ParameterLimits;
import org.hibernate.dialect.temptable.spi.TemporaryTableStrategies;
import org.hibernate.dialect.temptable.spi.StandardGlobalTemporaryTableStrategy;
import org.hibernate.dialect.temptable.spi.TemporaryTableStrategy;
import org.hibernate.dialect.type.spi.OracleJdbcTypes;
import org.hibernate.dialect.jdbc.spi.OracleTypes;
import org.hibernate.dialect.function.spi.Replacer;
import org.hibernate.dialect.type.spi.TimeZoneSupport;
import org.hibernate.dialect.type.spi.StringValueSemantics;
import org.hibernate.dialect.type.spi.UserDefinedTypeDdlSupport;
import org.hibernate.dialect.aggregate.spi.AggregateSupport;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.ModeStatsModeEmulation;
import org.hibernate.dialect.function.NvlCoalesceEmulation;
import org.hibernate.dialect.function.OracleTruncFunction;
import org.hibernate.community.dialect.identity.internal.Oracle12cIdentityColumnSupport;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.generated.spi.GeneratedValuesSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.StandardLockingSupports;
import org.hibernate.dialect.namespace.spi.NamespaceSupport;
import org.hibernate.dialect.namespace.spi.NamespaceSupports;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.Oracle12LimitHandler;
import org.hibernate.community.dialect.sequence.CommunitySequenceSupports;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.schema.spi.ColumnDefinitionRequest;
import org.hibernate.dialect.schema.spi.ConstraintControlMode;
import org.hibernate.dialect.schema.spi.ConstraintControlRequest;
import org.hibernate.dialect.schema.spi.ConstraintDropMode;
import org.hibernate.dialect.schema.spi.SchemaDropSupport;
import org.hibernate.dialect.schema.spi.OracleSchemaExporters;
import org.hibernate.dialect.unique.spi.UniqueDelegates;
import org.hibernate.dialect.unique.spi.UniqueDelegate;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupportFactory;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupports;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.dialect.identifier.spi.IdentifierHelperBuildRequest;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.jdbc.spi.JdbcExceptionHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UserDefinedType;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.procedure.spi.CallableStatementSupports;
import org.hibernate.query.SemanticException;
import org.hibernate.dialect.sql.ast.spi.FetchClauseSupport;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.dialect.temporaltype.spi.IntervalType;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.InformationExtractor;
import org.hibernate.tool.schema.extract.spi.InformationExtractors;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractors;
import org.hibernate.tool.schema.spi.StandardTableExporter;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.NullType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.BlobJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.NullJdbcType;
import org.hibernate.type.descriptor.jdbc.ObjectNullAsNullTypeJdbcType;
import org.hibernate.type.descriptor.jdbc.OracleJsonBlobJdbcType;
import org.hibernate.type.descriptor.jdbc.SqlTypedJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import static java.lang.String.join;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.query.common.TemporalUnit.DAY;
import static org.hibernate.query.common.TemporalUnit.HOUR;
import static org.hibernate.query.common.TemporalUnit.MINUTE;
import static org.hibernate.query.common.TemporalUnit.MONTH;
import static org.hibernate.query.common.TemporalUnit.SECOND;
import static org.hibernate.query.common.TemporalUnit.YEAR;
import static org.hibernate.type.SqlTypes.ARRAY;
import static org.hibernate.type.SqlTypes.BIGINT;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BIT;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.DATE;
import static org.hibernate.type.SqlTypes.DECIMAL;
import static org.hibernate.type.SqlTypes.DOUBLE;
import static org.hibernate.type.SqlTypes.FLOAT;
import static org.hibernate.type.SqlTypes.GEOMETRY;
import static org.hibernate.type.SqlTypes.INTEGER;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.NUMERIC;
import static org.hibernate.type.SqlTypes.NVARCHAR;
import static org.hibernate.type.SqlTypes.REAL;
import static org.hibernate.type.SqlTypes.SMALLINT;
import static org.hibernate.type.SqlTypes.SQLXML;
import static org.hibernate.type.SqlTypes.STRUCT;
import static org.hibernate.type.SqlTypes.TABLE;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.SqlTypes.VARCHAR;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTimestampWithNanos;
import static org.hibernate.dialect.literal.spi.ZeroOffsetLiteralStyle.NUMERIC_OFFSET;

/**
 * A {@linkplain Dialect SQL dialect} for Oracle 8i and above.
 *
 * @author Steve Ebersole
 * @author Gavin King
 * @author Loïc Lefèvre
 * @author Yoobin Yoon
 */
public class OracleLegacyDialect extends Dialect implements CurrentTemporalSupport, TemporalFormatSupport, TemporalOperationSupport {
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
	private static final RefCursorSupportFactory REF_CURSOR_SUPPORT_FACTORY =
			RefCursorSupports.metadataSelected( RefCursorSupports.jdbcType( OracleTypes.CURSOR ) );
	private final TypeSizingProfile typeSizingProfile = TypeSizingProfile.builder( super.getTypeSizingProfile() )
			.defaultTimestampPrecision( getVersion().isSameOrAfter( 10 ) ? 9 : 6 )
			.maxVarcharLength( 4000 ).maxVarcharCapacity( 4000 )
			.maxNVarcharLength( 4000 ).maxNVarcharCapacity( 4000 )
			.maxVarbinaryLength( 2000 ).maxVarbinaryCapacity( 2000 )
			.build();

	@Override public TypeSizingProfile getTypeSizingProfile() { return typeSizingProfile; }


	private static final Pattern SQL_STATEMENT_TYPE_PATTERN =
			Pattern.compile( "^(?:/\\*.*?\\*/)?\\s*(select|insert|update|delete)\\s+.*?", CASE_INSENSITIVE );

	private static final int PARAM_LIST_SIZE_LIMIT = 1000;

	public static final String PREFER_LONG_RAW = "hibernate.dialect.oracle.prefer_long_raw";

	private static final String yqmSelect =
			"(trunc(%2$s, 'MONTH') + numtoyminterval(%1$s, 'MONTH') + (least(extract(day from %2$s), extract(day from last_day(trunc(%2$s, 'MONTH') + numtoyminterval(%1$s, 'MONTH')))) - 1))";

	private static final String ADD_YEAR_EXPRESSION = String.format( yqmSelect, "?2*12", "?3" );
	private static final String ADD_QUARTER_EXPRESSION = String.format( yqmSelect, "?2*3", "?3" );
	private static final String ADD_MONTH_EXPRESSION = String.format( yqmSelect, "?2", "?3" );

	private final LimitHandler limitHandler = getFetchClauseSupport().supports( org.hibernate.query.common.FetchClauseType.ROWS_ONLY )
			? Oracle12LimitHandler.INSTANCE
			: new LegacyOracleLimitHandler( getVersion() );
	private final Exporter<UserDefinedType> userDefinedTypeExporter = OracleSchemaExporters.userDefinedTypes(
			this,
			new UserDefinedTypeDdlSupport( "object", "", ExistenceCheckPlacement.NONE )
	);
	private final UniqueDelegate uniqueDelegate = UniqueDelegates.createTable( this );
	private final SequenceSupport oracleSequenceSupport = CommunitySequenceSupports.oracle( getVersion() );
	private final StandardTableExporter oracleTableExporter = new StandardTableExporter( this ) {
		@Override
		protected void applyAggregateColumnCheck(StringBuilder buf, AggregateColumn aggregateColumn) {
			final JdbcType jdbcType = aggregateColumn.getType().getJdbcType();
			if ( dialect().getVersion().isBefore( 23, 6 ) && jdbcType.isXml() ) {
				// ORA-00600 when selecting XML columns that have a check constraint was fixed in 23.6
				return;
			}
			super.applyAggregateColumnCheck( buf, aggregateColumn );
		}
	};

	// Is it an Autonomous Database Cloud Service?
	protected final boolean autonomous;

	// Is MAX_STRING_SIZE set to EXTENDED?
	protected final boolean extended;

	// Is the database accessed using a database service protected by Application Continuity.
	protected final boolean applicationContinuity;

	protected final int driverMajorVersion;
	protected final int driverMinorVersion;

	private final LockingSupport lockingSupport;

	public OracleLegacyDialect() {
		this( DatabaseVersion.make( 8, 0 ) );
	}

	public OracleLegacyDialect(DatabaseVersion version) {
		super( version );
		lockingSupport = StandardLockingSupports.oracle( version );
		autonomous = false;
		extended = false;
		applicationContinuity = false;
		driverMajorVersion = 19;
		driverMinorVersion = 0;
	}

	public OracleLegacyDialect(DialectResolutionInfo info) {
		this( info, OracleServerConfiguration.fromDialectResolutionInfo( info ) );
	}

	public OracleLegacyDialect(DialectResolutionInfo info, OracleServerConfiguration serverConfiguration) {
		super( info );
		lockingSupport = StandardLockingSupports.oracle( getVersion() );
		autonomous = serverConfiguration.isAutonomous();
		extended = serverConfiguration.isExtended();
		applicationContinuity = serverConfiguration.isApplicationContinuity();
		this.driverMinorVersion = serverConfiguration.getDriverMinorVersion();
		this.driverMajorVersion = serverConfiguration.getDriverMajorVersion();
	}

	public boolean isAutonomous() {
		return autonomous;
	}

	public boolean isExtended() {
		return extended;
	}

	public boolean isApplicationContinuity() {
		return applicationContinuity;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public int getPreferredSqlTypeCodeForBoolean() {
		return Types.BIT;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);
		final TypeConfiguration typeConfiguration = functionContributions.getTypeConfiguration();

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);
		functionFactory.ascii();
		functionFactory.char_chr();
		functionFactory.cosh();
		functionFactory.sinh();
		functionFactory.tanh();
		functionFactory.log();
		functionFactory.log10_log();
		functionFactory.soundex();
		functionFactory.trim2();
		functionFactory.initcap();
		functionFactory.instr();
		functionFactory.substr();
		functionFactory.substring_substr();
		functionFactory.leftRight_substr();
		functionFactory.translate();
		functionFactory.bitand();
		functionFactory.lastDay();
		functionFactory.toCharNumberDateTimestamp();
		functionFactory.ceiling_ceil();
		functionFactory.concat_pipeOperator();
		functionFactory.rownumRowid();
		functionFactory.sysdate();
		functionFactory.systimestamp();
		functionFactory.addMonths();
		functionFactory.monthsBetween();
		functionFactory.everyAny_minMaxCase();
		functionFactory.repeat_rpad();

		functionFactory.radians_acos();
		functionFactory.degrees_acos();

		functionFactory.median();
		functionFactory.stddev();
		functionFactory.stddevPopSamp();
		functionFactory.variance();
		functionFactory.varPopSamp();
		functionFactory.covarPopSamp();
		functionFactory.corr();
		functionFactory.regrLinearRegressionAggregates();
		functionFactory.characterLength_length( "dbms_lob.getlength(?1)" );
		// Approximate octet and bit length for clobs since exact size determination would require a custom function
		functionFactory.octetLength_pattern( "lengthb(?1)", "dbms_lob.getlength(?1)*2" );
		functionFactory.bitLength_pattern( "lengthb(?1)*8", "dbms_lob.getlength(?1)*16" );

		if ( getVersion().isBefore( 9 ) ) {
			functionContributions.getFunctionRegistry().register( "coalesce", new NvlCoalesceEmulation() );
		}
		else {
			//Oracle has had coalesce() since 9.0.1
			functionFactory.coalesce();
		}

		functionContributions.getFunctionRegistry()
				.patternDescriptorBuilder( "bitor", "(?1+?2-bitand(?1,?2))")
				.setExactArgumentCount( 2 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();
		functionContributions.getFunctionRegistry()
				.patternDescriptorBuilder( "bitxor", "(?1+?2-2*bitand(?1,?2))")
				.setExactArgumentCount( 2 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();

		functionContributions.getFunctionRegistry().registerBinaryTernaryPattern(
				"locate",
				typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ),
				"instr(?2,?1)",
				"instr(?2,?1,?3)",
				FunctionParameterType.STRING, FunctionParameterType.STRING, FunctionParameterType.INTEGER,
				typeConfiguration
		).setArgumentListSignature("(pattern, string[, start])");
		// The within group clause became optional in 18
		if ( getVersion().isSameOrAfter( 18 ) ) {
			functionFactory.listagg( null );
		}
		else {
			functionFactory.listagg( "within group (order by rownum)" );
		}
		functionFactory.windowFunctions();
		functionFactory.hypotheticalOrderedSetAggregates();
		functionFactory.inverseDistributionOrderedSetAggregates();
		// Oracle has a regular aggregate function named stats_mode
		functionContributions.getFunctionRegistry().register(
				"mode",
				new ModeStatsModeEmulation( typeConfiguration )
		);
		functionContributions.getFunctionRegistry().register( "trunc", new OracleTruncFunction() );
		functionContributions.getFunctionRegistry().registerAlternateKey( "truncate", "trunc" );

		functionFactory.array_oracle();
		functionFactory.arrayAggregate_jsonArrayagg();
		functionFactory.arrayPosition_oracle();
		functionFactory.arrayPositions_oracle();
		functionFactory.arrayLength_oracle();
		functionFactory.arrayConcat_oracle();
		functionFactory.arrayPrepend_oracle();
		functionFactory.arrayAppend_oracle();
		functionFactory.arrayContains_oracle();
		functionFactory.arrayIntersects_oracle();
		functionFactory.arrayGet_oracle();
		functionFactory.arraySet_oracle();
		functionFactory.arrayRemove_oracle();
		functionFactory.arrayRemoveIndex_oracle();
		functionFactory.arraySlice_oracle();
		functionFactory.arrayReplace_oracle();
		functionFactory.arrayTrim_oracle();
		functionFactory.arrayReverse_oracle();
		functionFactory.arraySort_oracle();
		functionFactory.arrayFill_oracle();
		functionFactory.arrayToString_oracle();

		if ( getVersion().isSameOrAfter( 12 ) ) {
			functionFactory.jsonValue_oracle();
			functionFactory.jsonQuery_oracle();
			functionFactory.jsonExists_oracle();
			functionFactory.jsonObject_oracle( getVersion().isSameOrAfter( 19 ) );
			functionFactory.jsonArray_oracle();
			functionFactory.jsonArrayAgg_oracle();
			functionFactory.jsonObjectAgg_oracle();
			functionFactory.jsonSet_oracle();
			functionFactory.jsonRemove_oracle();
			functionFactory.jsonReplace_oracle();
			functionFactory.jsonInsert_oracle();
			functionFactory.jsonMergepatch_oracle();
			functionFactory.jsonArrayAppend_oracle();
			functionFactory.jsonArrayInsert_oracle();
			functionFactory.jsonTable_oracle();
		}

		functionFactory.xmlelement();
		functionFactory.xmlcomment();
		functionFactory.xmlforest();
		functionFactory.xmlconcat();
		functionFactory.xmlpi();
		functionFactory.xmlquery_oracle();
		functionFactory.xmlexists();
		functionFactory.xmlagg();
		functionFactory.xmltable_oracle();

		functionFactory.unnest_oracle( getVersion().isSameOrAfter( 21 ) );
		functionFactory.generateSeries_recursive( getMaximumSeriesSize(), true, false );
		functionFactory.regexpLike_predicateFunction();
	}

	/**
	 * Oracle doesn't support the {@code generate_series} function or {@code lateral} recursive CTEs,
	 * so it has to be emulated with a top level recursive CTE which requires an upper bound on the amount
	 * of elements that the series can return.
	 */
	protected int getMaximumSeriesSize() {
		return 10000;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new OracleLegacySqlAstTranslator<>( request );
			}
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentDate() {
		return getVersion().isBefore( 9 ) ? currentTimestamp() : "current_date";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTime() {
		return currentTimestamp();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTimestamp() {
		return getVersion().isBefore( 9 ) ? "sysdate" : currentTimestampWithTimeZone();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentLocalTime() {
		return currentLocalTimestamp();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentLocalTimestamp() {
		return getVersion().isBefore( 9 ) ? currentTimestamp() : "localtimestamp";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTimestampWithTimeZone() {
		return getVersion().isBefore( 9 ) ? currentTimestamp() : "current_timestamp";
	}

	@Override
	public GeneratedValuesSupport getGeneratedValuesSupport() {
		return getVersion().isSameOrAfter( 12 )
				? GeneratedValuesSupport.builder( super.getGeneratedValuesSupport() )
						.enable( GeneratedValuesSupport.Capability.ARBITRARY_GENERATED_KEYS )
						.build()
				: super.getGeneratedValuesSupport();
	}


	/**
	 * Oracle doesn't have any sort of {@link Types#BOOLEAN}
	 * type or {@link Types#TIME} type, and its default behavior
	 * for casting dates and timestamps to and from strings is just awful.
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
			case BOOLEAN:
				result = from == CastType.STRING
						? buildStringToBooleanCastDecode( "true", "false" )
						: BooleanDecoder.toBoolean( from );
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
				switch ( from ) {
					case INTEGER_BOOLEAN:
					case TF_BOOLEAN:
					case YN_BOOLEAN:
						return BooleanDecoder.toString( from );
					case DATE:
						return "to_char(?1,'YYYY-MM-DD')";
					case TIME:
						return "to_char(?1,'HH24:MI:SS')";
					case TIMESTAMP:
						return "to_char(?1,'YYYY-MM-DD HH24:MI:SS.FF9')";
					case OFFSET_TIMESTAMP:
						return "to_char(?1,'YYYY-MM-DD HH24:MI:SS.FF9TZH:TZM')";
					case ZONE_TIMESTAMP:
						return "to_char(?1,'YYYY-MM-DD HH24:MI:SS.FF9 TZR')";
				}
				break;
			case CLOB:
				// Oracle doesn't like casting to clob
				return "to_clob(?1)";
			case DATE:
				if ( from == CastType.STRING ) {
					return "to_date(?1,'YYYY-MM-DD')";
				}
				break;
			case TIME:
				if ( from == CastType.STRING ) {
					return "to_date(?1,'HH24:MI:SS')";
				}
				break;
			case TIMESTAMP:
				if ( from == CastType.STRING ) {
					return "to_timestamp(?1,'YYYY-MM-DD HH24:MI:SS.FF9')";
				}
				break;
			case OFFSET_TIMESTAMP:
				if ( from == CastType.STRING ) {
					return "to_timestamp_tz(?1,'YYYY-MM-DD HH24:MI:SS.FF9TZH:TZM')";
				}
				break;
			case ZONE_TIMESTAMP:
				if ( from == CastType.STRING ) {
					return "to_timestamp_tz(?1,'YYYY-MM-DD HH24:MI:SS.FF9 TZR')";
				}
				break;
			case XML:
				return "xmlparse(document ?1)";
		}
		return super.castPattern(from, to);
	}

	/**
	 * We minimize multiplicative factors by using seconds
	 * (with fractional part) as the "native" precision for
	 * duration arithmetic.
	 */
	@Override
	@SPI({ USE, IMPLEMENT })
	public long fractionalSecondPrecisionInNanos() {
		return 1_000_000_000; //seconds
	}

	/**
	 * Oracle supports a limited list of temporal fields in the
	 * extract() function, but we can emulate some of them by
	 * using to_char() with a format string instead of extract().
	 *
	 * Thus, the additional supported fields are
	 * {@link TemporalUnit#DAY_OF_YEAR},
	 * {@link TemporalUnit#DAY_OF_MONTH},
	 * {@link TemporalUnit#DAY_OF_YEAR},
	 * and {@link TemporalUnit#WEEK}.
	 */
	@Override
	@SPI({ USE, IMPLEMENT })
	public String extractPattern(TemporalUnit unit) {
		switch (unit) {
			case DAY_OF_WEEK:
				return "to_number(to_char(?2,'D'))";
			case DAY_OF_MONTH:
				return "to_number(to_char(?2,'DD'))";
			case DAY_OF_YEAR:
				return "to_number(to_char(?2,'DDD'))";
			case WEEK:
				return "to_number(to_char(?2,'IW'))"; //the ISO week number
			case WEEK_OF_YEAR:
				return "to_number(to_char(?2,'WW'))";
			// Oracle doesn't support extracting the quarter
			case QUARTER:
				return "to_number(to_char(?2,'Q'))";
			// Oracle can't extract time parts from a date column, so we need to cast to timestamp
			// This is because Oracle treats date as ANSI SQL date which has no time part
			// Also see https://docs.oracle.com/cd/B28359_01/server.111/b28286/functions052.htm#SQLRF00639
			case HOUR:
				return "to_number(to_char(?2,'HH24'))";
			case MINUTE:
				return "to_number(to_char(?2,'MI'))";
			case SECOND:
				return "to_number(to_char(?2,'SS'))";
			case EPOCH:
				return "trunc((cast(?2 at time zone 'UTC' as date) - date '1970-1-1')*86400)";
			default:
				return TemporalOperationSupports.standard().extractPattern(unit);
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		final StringBuilder pattern = new StringBuilder();
		switch ( unit ) {
			case YEAR:
				pattern.append( ADD_YEAR_EXPRESSION );
				break;
			case QUARTER:
				pattern.append( ADD_QUARTER_EXPRESSION );
				break;
			case MONTH:
				pattern.append( ADD_MONTH_EXPRESSION );
				break;
			case WEEK:
				if ( temporalType != TemporalType.DATE ) {
					pattern.append( "(?3+numtodsinterval((?2)*7,'day'))" );
				}
				else {
					pattern.append( "(?3+(?2)" ).append( unit.conversionFactor( DAY, this ) ).append( ")" );
				}
				break;
			case DAY:
				if ( temporalType == TemporalType.DATE ) {
					pattern.append( "(?3+(?2))" );
					break;
				}
			case HOUR:
			case MINUTE:
			case SECOND:
				pattern.append( "(?3+numtodsinterval(?2,'?1'))" );
				break;
			case NANOSECOND:
				pattern.append( "(?3+numtodsinterval((?2)/1e9,'second'))" );
				break;
			case NATIVE:
				pattern.append( "(?3+numtodsinterval(?2,'second'))" );
				break;
			default:
				throw new SemanticException( unit + " is not a legal field" );
		}
		return pattern.toString();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		final StringBuilder pattern = new StringBuilder();
		final boolean hasTimePart = toTemporalType != TemporalType.DATE || fromTemporalType != TemporalType.DATE;
		switch ( unit ) {
			case YEAR:
				extractField( pattern, YEAR, unit );
				break;
			case QUARTER:
			case MONTH:
				pattern.append( "(" );
				extractField( pattern, YEAR, unit );
				pattern.append( "+" );
				extractField( pattern, MONTH, unit );
				pattern.append( ")" );
				break;
			case DAY:
				if ( hasTimePart ) {
					pattern.append( "(cast(?3 as date)-cast(?2 as date))" );
				}
				else {
					pattern.append( "(?3-?2)" );
				}
				break;
			case WEEK:
			case MINUTE:
			case SECOND:
			case HOUR:
				if ( hasTimePart ) {
					pattern.append( "((cast(?3 as date)-cast(?2 as date))" );
				}
				else {
					pattern.append( "((?3-?2)" );
				}
				pattern.append( TemporalUnit.DAY.conversionFactor(unit ,this ) );
				pattern.append( ")" );
				break;
			case NATIVE:
			case NANOSECOND:
				if ( hasTimePart ) {
					if ( getSubquerySupport().supports( SubquerySupport.Feature.LATERAL ) ) {
						pattern.append( "(select extract(day from t.i)" ).append( TemporalUnit.DAY.conversionFactor( unit, this ) )
								.append( "+extract(hour from t.i)" ).append( TemporalUnit.HOUR.conversionFactor( unit, this ) )
								.append( "+extract(minute from t.i)" ).append( MINUTE.conversionFactor( unit, this ) )
								.append( "+extract(second from t.i)" ).append( SECOND.conversionFactor( unit, this ) )
								.append( " from(select ?3-?2 i from dual)t" );
					}
					else {
						pattern.append( "(" );
						extractField( pattern, DAY, unit );
						pattern.append( "+" );
						extractField( pattern, HOUR, unit );
						pattern.append( "+" );
						extractField( pattern, MINUTE, unit );
						pattern.append( "+" );
						extractField( pattern, SECOND, unit );
					}
				}
				else {
					pattern.append( "((?3-?2)" );
					pattern.append( TemporalUnit.DAY.conversionFactor( unit, this ) );
				}
				pattern.append( ")" );
				break;
			default:
				throw new SemanticException( "unrecognized field: " + unit );
		}
		return pattern.toString();
	}

	private void extractField(StringBuilder pattern, TemporalUnit unit, TemporalUnit toUnit) {
		pattern.append( "extract(" );
		pattern.append( translateExtractField( unit ) );
		pattern.append( " from (?3-?2)" );
		switch ( unit ) {
			case YEAR:
			case MONTH:
				pattern.append( " year(9) to month" );
				break;
			case DAY:
			case HOUR:
			case MINUTE:
			case SECOND:
				break;
			default:
				throw new SemanticException( unit + " is not a legal field" );
		}
		pattern.append( ")" );
		pattern.append( unit.conversionFactor( toUnit, this ) );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		switch ( sqlTypeCode ) {
			case BOOLEAN:
			case BIT:
				// still, after all these years...
				return "number(1,0)";

			case TINYINT:
				return "number(3,0)";
			case SMALLINT:
				return "number(5,0)";
			case INTEGER:
				return "number(10,0)";
			case BIGINT:
				return "number(19,0)";
			case REAL:
				// Oracle's 'real' type is actually double precision
				return "float(24)";
			case DOUBLE:
				// Oracle's 'double precision' means float(126), and
				// we never need 126 bits (38 decimal digits)
				return "float(53)";

			case NUMERIC:
			case DECIMAL:
				// Note that 38 is the maximum precision Oracle supports
				return "number($p,$s)";

			case DATE:
				return "date";
			case TIME:
				return getVersion().isBefore( 9 ) ? "date" : "timestamp($p)";
			case TIMESTAMP:
				// the only difference between date and timestamp
				// on Oracle is that date has no fractional seconds
			case TIMESTAMP_WITH_TIMEZONE:
				return getVersion().isBefore( 9 ) ? "date" : super.columnType( sqlTypeCode );
			case TIME_WITH_TIMEZONE:
				return getVersion().isBefore( 9 ) ? "date" : "timestamp($p) with time zone";
			case VARCHAR:
				return getVersion().isBefore( 9 ) ? "varchar2($l)" : "varchar2($l char)";
			case NVARCHAR:
				return "nvarchar2($l)";
			case BINARY:
			case VARBINARY:
				return "raw($l)";

			default:
				return super.columnType( sqlTypeCode );
		}
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( SQLXML, "SYS.XMLTYPE", this ) );
		if ( getVersion().isSameOrAfter( 10 ) ) {
			ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( GEOMETRY, "MDSYS.SDO_GEOMETRY", this ) );
			if ( getVersion().isSameOrAfter( 21 ) ) {
				ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( JSON, "json", this ) );
			}
			else if ( getVersion().isSameOrAfter( 12 ) ) {
				ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( JSON, "blob", this ) );
			}
		}

		ddlTypeRegistry.addDescriptor( StandardDdlTypes.standardArray( this, false ) );
		ddlTypeRegistry.addDescriptor( TABLE, StandardDdlTypes.standardArray( this, false ) );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TimeZoneSupport getTimeZoneSupport() {
		return getVersion().isSameOrAfter( 9 ) ? TimeZoneSupport.NATIVE : TimeZoneSupport.NONE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void contributeDefaultProperties(java.util.Properties properties) {
		super.contributeDefaultProperties( properties );
		properties.setProperty( org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE, "15" );
		// Oracle driver reports to support getGeneratedKeys(), but they only
		// support the version taking an array of the names of the columns to
		// be returned (via its RETURNING clause).  No other driver seems to
		// support this overloaded version.
		properties.setProperty(
				org.hibernate.cfg.AvailableSettings.USE_GET_GENERATED_KEYS,
				Boolean.toString( getVersion().isSameOrAfter( 12 ) )
		);
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public JdbcType resolveSqlTypeDescriptor(
			String columnTypeName,
			int jdbcTypeCode,
			int precision,
			int scale,
			JdbcTypeRegistry jdbcTypeRegistry) {
		switch ( jdbcTypeCode ) {
			case OracleTypes.JSON:
				return jdbcTypeRegistry.getDescriptor( JSON );
			case STRUCT:
				if ( "MDSYS.SDO_GEOMETRY".equals( columnTypeName ) ) {
					jdbcTypeCode = SqlTypes.GEOMETRY;
				}
				else {
					final SqlTypedJdbcType descriptor = jdbcTypeRegistry.findSqlTypedDescriptor(
							// Skip the schema
							columnTypeName.substring( columnTypeName.indexOf( '.' ) + 1 )
					);
					if ( descriptor != null ) {
						return descriptor;
					}
				}
				break;
			case ARRAY:
				if ( "MDSYS.SDO_ORDINATE_ARRAY".equals( columnTypeName ) ) {
					return jdbcTypeRegistry.resolveTypeConstructorDescriptor(
							jdbcTypeCode,
							jdbcTypeRegistry.getDescriptor( NUMERIC ),
							ColumnTypeInformation.EMPTY
					);
				}
				else {
					final SqlTypedJdbcType descriptor = jdbcTypeRegistry.findSqlTypedDescriptor(
							// Skip the schema
							columnTypeName.substring( columnTypeName.indexOf( '.' ) + 1 )
					);
					if ( descriptor != null ) {
						return descriptor;
					}
				}
				break;
			case Types.NUMERIC:
				if ( precision > 8 // precision of 0 means something funny
						// For some reason, the Oracle JDBC driver reports
						// FLOAT or DOUBLE as NUMERIC with scale -127
						// (but note that expressions with unknown type
						//  also get reported this way, so take care)
						&& scale == -127 ) {
					if ( precision <= 24 ) {
						// Can be represented as a Java float
						return jdbcTypeRegistry.getDescriptor( FLOAT );
					}
					else if ( precision <= 53 ) {
						// Can be represented as a Java double
						return jdbcTypeRegistry.getDescriptor( DOUBLE );
					}
				}
				//intentional fall-through:
			case Types.DECIMAL:
				if ( scale == 0 && precision != 0 ) {
					// Don't infer TINYINT or SMALLINT on Oracle, since the
					// range of values of a NUMBER(3,0) or NUMBER(5,0) just
					// doesn't really match naturally.
					if ( precision <= 10 ) {
						// A NUMBER(10,0) might not fit in a 32-bit integer,
						// but it's still pretty safe to use INTEGER here,
						// since we can assume the most likely reason to find
						// a column of type NUMBER(10,0) in an Oracle database
						// is that it's intended to store an integer.
						return jdbcTypeRegistry.getDescriptor( Types.INTEGER );
					}
					else if ( precision <= 19 ) {
						return jdbcTypeRegistry.getDescriptor( Types.BIGINT );
					}
				}
		}
		return super.resolveSqlTypeDescriptor( columnTypeName, jdbcTypeCode, precision, scale, jdbcTypeRegistry );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String getArrayTypeName(String javaElementTypeName, String elementTypeName, Integer maxLength) {
		return ( javaElementTypeName == null ? elementTypeName : javaElementTypeName ) + "Array";
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public int getPreferredSqlTypeCodeForArray() {
		// Prefer to resolve to the OracleArrayJdbcType, since that will fall back to XML later if needed
		return ARRAY;
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public Exporter<UserDefinedType> getUserDefinedTypeExporter() {
		return userDefinedTypeExporter;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );

		typeContributions.contributeJdbcType( OracleJdbcTypes.booleanType() );
		typeContributions.contributeJdbcType( OracleJdbcTypes.xml() );
		if ( OracleJdbcTypes.isDriverUsable( serviceRegistry ) ) {
			typeContributions.contributeJdbcType( OracleJdbcTypes.driverStruct( serviceRegistry ) );
		}
		else {
			typeContributions.contributeJdbcType( OracleJdbcTypes.reflectionStruct() );
		}

		if ( getVersion().isSameOrAfter( 12 ) ) {
			// account for Oracle's deprecated support for LONGVARBINARY
			// prefer BLOB, unless the user explicitly opts out
			boolean preferLong = serviceRegistry.requireService( ConfigurationService.class ).getSetting(
					PREFER_LONG_RAW,
					StandardConverters.BOOLEAN,
					false
			);

			BlobJdbcType descriptor = preferLong ?
					BlobJdbcType.PRIMITIVE_ARRAY_BINDING :
					BlobJdbcType.DEFAULT;

			typeContributions.contributeJdbcType( descriptor );

			if ( getVersion().isSameOrAfter( 21 ) ) {
				typeContributions.contributeJdbcType( OracleJdbcTypes.nativeJson() );
				typeContributions.contributeJdbcTypeConstructor( OracleJdbcTypes.nativeJsonArrayConstructor() );
			}
			else {
				typeContributions.contributeJdbcType( OracleJsonBlobJdbcType.INSTANCE );
				typeContributions.contributeJdbcTypeConstructor( OracleJdbcTypes.blobJsonArrayConstructor() );
			}
		}

		if ( OracleJdbcTypes.isDriverUsable( serviceRegistry ) ) {
			typeContributions.contributeJdbcTypeConstructor( OracleJdbcTypes.driverArrayConstructor( serviceRegistry ) );
			typeContributions.contributeJdbcTypeConstructor( OracleJdbcTypes.driverNestedTableConstructor( serviceRegistry ) );
		}
		else {
			typeContributions.contributeJdbcType( OracleJdbcTypes.reflectionStruct() );
		}
		// Oracle requires a custom binder for binding untyped nulls with the NULL type
		typeContributions.contributeJdbcType( NullJdbcType.INSTANCE );
		typeContributions.contributeJdbcType( ObjectNullAsNullTypeJdbcType.INSTANCE );

		// Until we remove StandardBasicTypes, we have to keep this
		typeContributions.contributeType(
				new NullType(
						NullJdbcType.INSTANCE,
						typeContributions.getTypeConfiguration()
								.getJavaTypeRegistry()
								.resolveDescriptor( Object.class )
				)
		);
		typeContributions.contributeType(
				new JavaObjectType(
						ObjectNullAsNullTypeJdbcType.INSTANCE,
						typeContributions.getTypeConfiguration()
								.getJavaTypeRegistry()
								.resolveDescriptor( Object.class )
						)
		);
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public AggregateSupport getAggregateSupport() {
		return new OracleDialect( getVersion() ).getAggregateSupport();
	}

	@Override
	public GenerationType getNativeValueGenerationStrategy() {
		return GenerationType.SEQUENCE;
	}

	// features which change between 8i, 9i, and 10g ~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return getVersion().isBefore( 12 )
				? super.getIdentityColumnSupport()
				: Oracle12cIdentityColumnSupport.INSTANCE;
	}

	@Override
	public LimitHandler getLimitHandler() {
		return limitHandler;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String getSelectClauseNullString(SqlTypedMapping sqlTypeMapping, TypeConfiguration typeConfiguration) {
		if ( getVersion().isSameOrAfter( 9 ) ) {
			return super.getSelectClauseNullString( sqlTypeMapping, typeConfiguration );
		}
		else {
			final int sqlType = sqlTypeMapping.getJdbcMapping().getJdbcType().getDdlTypeCode();
			switch(sqlType) {
				case Types.VARCHAR:
				case Types.CHAR:
					return "to_char(null)";
				case Types.DATE:
				case Types.TIME:
				case Types.TIMESTAMP:
				case Types.TIMESTAMP_WITH_TIMEZONE:
					return "to_date(null)";
				default:
					return "to_number(null)";
			}
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public CurrentTimestampSelection getCurrentTimestampSelection() {
		return CurrentTimestampSelection.prepared( getVersion().isBefore( 9 )
				? "select sysdate from dual"
				: "select systimestamp from dual" );
	}


	// features which remain constant across 8i, 9i, and 10g ~~~~~~~~~~~~~~~~~~

	@Override
	@SPI({ USE, IMPLEMENT })
	public String addColumnPrefix() {
		return "add";
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized SchemaDropSupport getSchemaDropSupport() {
		if ( schemaDropSupport == null ) {
			schemaDropSupport = new SchemaDropSupport( List.of(), ConstraintDropMode.IMPLICIT, " cascade constraints" );
		}
		return schemaDropSupport;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		return oracleSequenceSupport;
	}

	@Override
	public Exporter<Table> getTableExporter() {
		return oracleTableExporter;
	}

	private static final SequenceInformationExtractor SEQUENCE_INFORMATION_EXTRACTOR =
			SequenceInformationExtractors.builder( "select * from all_sequences" )
					.withoutCatalog()
					.withoutSchema()
					.withoutStartValue()
					.minimumValueReader( resultSet -> resultSet.getBigDecimal( "min_value" ) )
					.maximumValueReader( resultSet -> resultSet.getBigDecimal( "max_value" ) )
					.incrementValueReader( resultSet -> resultSet.getBigDecimal( "increment_by" ) )
					.build();

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SEQUENCE_INFORMATION_EXTRACTOR;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}

	private static final ViolatedConstraintNameExtractor EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor( sqle -> {
				switch ( JdbcExceptionHelper.extractErrorCode( sqle ) ) {
					case 1:
					case 2291:
					case 2292:
						return extractUsingTemplate( "(", ")", sqle.getMessage() );
					case 1400:
						// simple nullability constraint
						return null;
					default:
						return null;
				}
			} );

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			final String constraintName;
			// interpreting Oracle exceptions is much much more precise based on their specific vendor codes.
			switch ( JdbcExceptionHelper.extractErrorCode( sqlException ) ) {

				// lock timeouts ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

				case 30006:
					// ORA-30006: resource busy; acquire with WAIT timeout expired
					return new LockTimeoutException(message, sqlException, sql);
				case 54:
					// ORA-00054: resource busy and acquire with NOWAIT specified or timeout expired
					return new LockTimeoutException(message, sqlException, sql);
				case 4021:
					// ORA-04021 timeout occurred while waiting to lock object
					return new LockTimeoutException(message, sqlException, sql);

				// deadlocks ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

				case 60:
					// ORA-00060: deadlock detected while waiting for resource
					return new LockAcquisitionException( message, sqlException, sql );
				case 4020:
					// ORA-04020 deadlock detected while trying to lock object
					return new LockAcquisitionException( message, sqlException, sql );

				// query cancelled ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

				case 1013:
					// ORA-01013: user requested cancel of current operation
					return new QueryTimeoutException(  message, sqlException, sql );

				// data integrity violation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

				case 1:
					// ORA-00001: unique constraint violated
					constraintName = getViolatedConstraintNameExtractor().extractConstraintName( sqlException );
					return new ConstraintViolationException(
							message,
							sqlException,
							sql,
							ConstraintViolationException.ConstraintKind.UNIQUE,
							constraintName
					);
				case 1407:
					// ORA-01407: cannot update column to NULL
					constraintName = getViolatedConstraintNameExtractor().extractConstraintName( sqlException );
					return new ConstraintViolationException( message, sqlException, sql, constraintName );

				default:
					return null;
			}
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public org.hibernate.dialect.schema.spi.SchemaCommentSupport getSchemaCommentSupport() {
		return org.hibernate.dialect.schema.spi.SchemaCommentSupports.commentOn();
	}

	@Override
	public SubquerySupport getSubquerySupport() {
		return SubquerySupport.builder()
				.feature( SubquerySupport.Feature.EXISTS_IN_SELECT, false )
				.feature( SubquerySupport.Feature.OFFSET, true )
				.feature( SubquerySupport.Feature.LATERAL, getVersion().isSameOrAfter( 12, 1 ) )
				.feature( SubquerySupport.Feature.NESTED_CORRELATION, false )
				.build();
	}

	@Override
	public ParameterLimits getParameterLimits() {
		return ParameterLimits.of( PARAM_LIST_SIZE_LIMIT );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public LobSupport getLobSupport() {
		return LobSupports.oracle( isApplicationContinuity(), false );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public StringValueSemantics getStringValueSemantics() {
		return StringValueSemantics.EMPTY_STRING_AS_NULL;
	}

	@Override
	public TemporaryTableStrategy getLocalTemporaryTableStrategy() {
		return TemporaryTableStrategies.oracleLocal();
	}

	@Override
	public TemporaryTableStrategy getGlobalTemporaryTableStrategy() {
		return StandardGlobalTemporaryTableStrategy.INSTANCE;
	}

	@Override
	public MultiTableMutationSupport getMultiTableMutationSupport() {
		return MultiTableMutationSupport.GLOBAL_TEMPORARY_TABLE;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String getQueryHintString(String query, List<String> hintList) {
		if ( hintList.isEmpty() ) {
			return query;
		}
		else {
			final String hints = join( " ", hintList );
			return isEmpty( hints ) ? query : getQueryHintString( query, hints );
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String getQueryHintString(String sql, String hints) {
		final String statementType = statementType( sql );
		final int start = sql.indexOf( statementType );
		if ( start < 0 ) {
			return sql;
		}
		else {
			int end = start + statementType.length();
			return sql.substring( 0, end ) + " /*+ " + hints + " */" + sql.substring( end );
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public int getMaxAliasLength() {
		// Max identifier length is 30 for pre 12.2 versions, and 128 for 12.2+
		// but Hibernate needs to add "uniqueing info" so we account for that
		return getVersion().isSameOrAfter( 12, 2 ) ? 118 : 20;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public int getMaxIdentifierLength() {
		// Since 12.2 version, maximum identifier length is 128
		return getVersion().isSameOrAfter( 12, 2 ) ? 128 : 30;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public CallableStatementSupport getCallableStatementSupport() {
		return CallableStatementSupports.standardWithRefCursors();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public RefCursorSupportFactory getRefCursorSupportFactory() {
		return REF_CURSOR_SUPPORT_FACTORY;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NamespaceSupport getNamespaceSupport() {
		return NamespaceSupports.none();
	}


	private String statementType(String sql) {
		final Matcher matcher = SQL_STATEMENT_TYPE_PATTERN.matcher( sql );
		if ( matcher.matches() && matcher.groupCount() == 1 ) {
			return matcher.group(1);
		}
		else {
			throw new IllegalArgumentException( "Can't determine SQL statement type for statement: " + sql );
		}
	}

	@Override
	public TupleCountSupport getTupleCountSupport() {
		return TupleCountSupport.NONE;
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public FetchClauseSupport getFetchClauseSupport() {
		// Until 12.2 there was a bug in the Oracle query rewriter causing ORA-00918
		// when the query contains duplicate implicit aliases in the select clause
		return getVersion().isSameOrAfter( 12, 2 )
				? FetchClauseSupport.ALL
				: FetchClauseSupport.NONE;
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
	public CteSupport getCteSupport() {
		final CteSupport.Builder builder = CteSupport.builder()
				.placement( CteSupport.Placement.TOP_LEVEL )
				.requiresRecursiveKeyword( false );
		if ( getVersion().isSameOrAfter( 11, 2 ) ) {
			builder.recursiveFeatures(
					CteSupport.RecursiveFeature.RECURSIVE,
					CteSupport.RecursiveFeature.SEARCH,
					CteSupport.RecursiveFeature.CYCLE
			);
		}
		return builder.build();
	}

	@Override
	public LockingSupport getLockingSupport() {
		return lockingSupport;
	}
















	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendDateTimeLiteral(SqlAppender appender, TemporalAccessor temporalAccessor, TemporalType precision, TimeZone jdbcTimeZone) {
		// we usually use the JDBC escape-based syntax
		// because we want to let the JDBC driver handle
		// TIME (a concept which does not exist in Oracle)
		// but for the special case of timestamps with an
		// offset we need to use the ANSI syntax
		if ( precision == TemporalType.TIMESTAMP && temporalAccessor.isSupported( ChronoField.OFFSET_SECONDS ) ) {
			appender.appendSql( "timestamp '" );
			appendAsTimestampWithNanos( appender, temporalAccessor, true, jdbcTimeZone, NUMERIC_OFFSET );
			appender.appendSql( '\'' );
		}
		else {
			super.appendDateTimeLiteral( appender, temporalAccessor, precision, jdbcTimeZone );
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendFormat(SqlAppender appender, String format) {
		// Unlike other databases, Oracle requires an explicit reset for the fm modifier,
		// otherwise all following pattern variables trim zeros
		appender.appendSql( datetimeFormat( format, true, true ).result() );
	}

	public static Replacer datetimeFormat(String format, boolean useFm, boolean resetFm) {
		String fm = useFm ? "fm" : "";
		String fmReset = resetFm ? fm : "";
		return new Replacer( format, "'", "\"" )
				//era
				.replace("GG", "AD")
				.replace("G", "AD")

				//year
				.replace("yyyy", "YYYY")
				.replace("yyy", fm + "YYYY" + fmReset)
				.replace("yy", "YY")
				.replace("y", fm + "YYYY" + fmReset)

				//month of year
				.replace("MMMM", fm + "Month" + fmReset)
				.replace("MMM", "Mon")
				.replace("MM", "MM")
				.replace("M", fm + "MM" + fmReset)

				//week of year
				.replace("ww", "IW")
				.replace("w", fm + "IW" + fmReset)
				//year for week
				.replace("YYYY", "IYYY")
				.replace("YYY", fm + "IYYY" + fmReset)
				.replace("YY", "IY")
				.replace("Y", fm + "IYYY" + fmReset)

				//week of month
				.replace("W", "W")

				//day of week
				.replace("EEEE", fm + "Day" + fmReset)
				.replace("EEE", "Dy")
				.replace("ee", "D")
				.replace("e", fm + "D" + fmReset)

				//day of month
				.replace("dd", "DD")
				.replace("d", fm + "DD" + fmReset)

				//day of year
				.replace("DDD", "DDD")
				.replace("DD", fm + "DDD" + fmReset)
				.replace("D", fm + "DDD" + fmReset)

				//am pm
				.replace("a", "AM")

				//hour
				.replace("hh", "HH12")
				.replace("HH", "HH24")
				.replace("h", fm + "HH12" + fmReset)
				.replace("H", fm + "HH24" + fmReset)

				//minute
				.replace("mm", "MI")
				.replace("m", fm + "MI" + fmReset)

				//second
				.replace("ss", "SS")
				.replace("s", fm + "SS" + fmReset)

				//fractional seconds
				.replace("SSSSSS", "FF6")
				.replace("SSSSS", "FF5")
				.replace("SSSS", "FF4")
				.replace("SSS", "FF3")
				.replace("SS", "FF2")
				.replace("S", "FF1")

				//timezones
				.replace("zzz", "TZR")
				.replace("zz", "TZR")
				.replace("z", "TZR")
				.replace("ZZZ", "TZHTZM")
				.replace("ZZ", "TZHTZM")
				.replace("Z", "TZHTZM")
				.replace("xxx", "TZH:TZM")
				.replace("xx", "TZHTZM")
				.replace("x", "TZH"); //note special case
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendBinaryLiteral(SqlAppender appender, byte[] bytes) {
		appender.appendSql( "hextoraw('" );
		PrimitiveByteArrayJavaType.INSTANCE.appendString( appender, bytes );
		appender.appendSql( "')" );
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
		return List.of( "alter table " + request.tableName() + " disable constraint " + request.constraintName() );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public List<String> enableConstraintCommands(ConstraintControlRequest request) {
		return List.of( "alter table " + request.tableName() + " enable constraint " + request.constraintName() );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public RowIdSupport getRowIdSupport() {
		return RowIdSupports.fixed( "rowid", Types.ROWID );
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}

	@Override
	public ValuesListSupport getValuesListSupport() {
		return ValuesListSupport.NONE;
	}

	@Override
	public MutationSyntaxSupport getMutationSyntaxSupport() {
		return MutationSyntaxSupport.of( MutationKind.UPDATE, MutationSyntaxCapability.FROM_CLAUSE );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String render(org.hibernate.dialect.constraint.spi.CheckConstraintRenderRequest request) {
		final String rendered = super.render( request );
		return StringHelper.isNotEmpty( request.options() ) ? rendered + " " + request.options() : rendered;
	}
	@Override
	public SingleRowTableSupport getSingleRowTableSupport() {
		final String tableExpression = "dual";
		return SingleRowTableSupport.builder( super.getSingleRowTableSupport() )
				.tableExpression( tableExpression )
				.selectOnlyFromClause( " from " + tableExpression )
				.build();
	}

	@Override
	public SetOperationSupport getSetOperationSupport() {
		return SetOperationSupport.builder()
				.operator( SetOperator.INTERSECT_ALL, getVersion().isSameOrAfter( 21 ) )
				.operator( SetOperator.EXCEPT_ALL, getVersion().isSameOrAfter( 21 ) )
				.capability( SetOperationSupport.Capability.DUPLICATE_SELECT_ITEMS, false )
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String getSetOperatorSqlString(SetOperator setOperator) {
		return setOperator == SetOperator.EXCEPT && getVersion().isBefore( 21 )
				? "minus"
				: super.getSetOperatorSqlString( setOperator );
	}

	@Override
	public RowValueSupport getRowValueSupport() {
		return RowValueSupport.builder( RowValueSupport.NONE )
				.feature( RowValueSupport.Feature.IN_LIST, getVersion().isSameOrAfter( 8, 2 ) )
				.feature( RowValueSupport.Feature.IN_SUBQUERY, getVersion().isSameOrAfter( 9 ) )
				.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public InformationExtractor getInformationExtractor(ExtractionContext extractionContext) {
		return InformationExtractors.oracle( extractionContext );
	}
}
