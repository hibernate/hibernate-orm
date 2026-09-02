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
import org.hibernate.dialect.type.spi.EnumSupport;
import org.hibernate.dialect.type.spi.EnumSupports;
import org.hibernate.dialect.type.spi.ObjectNullBindingStrategy;
import org.hibernate.dialect.rowid.spi.RowIdSupport;
import org.hibernate.dialect.rowid.spi.RowIdSupports;

import org.hibernate.dialect.mutation.spi.MultiTableMutationSupport;


import org.hibernate.dialect.array.spi.ArraySupport;
import org.hibernate.dialect.function.spi.Replacer;

import org.hibernate.dialect.type.spi.TimeZoneSupport;

import org.hibernate.dialect.aggregate.spi.FunctionalDependencyAnalysisSupport;

import org.hibernate.dialect.sql.ast.spi.NullOrdering;


import org.hibernate.dialect.sql.ast.spi.DmlTargetColumnQualifierSupport;

import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.PredicateSupport;
import org.hibernate.dialect.sql.ast.spi.NullOrderingSupport;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.SingleRowTableSupport;
import org.hibernate.dialect.sql.ast.spi.SetOperationSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;
import org.hibernate.dialect.sql.ast.spi.ValuesListSupport;

import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import jakarta.annotation.Nullable;
import org.hibernate.QueryTimeoutException;
import org.hibernate.Timeouts;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.aggregate.spi.AggregateSupport;
import org.hibernate.dialect.aggregate.internal.H2AggregateSupport;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;
import org.hibernate.dialect.function.spi.TupleCountSupport;
import org.hibernate.dialect.generated.spi.GeneratedValuesSupport;
import org.hibernate.dialect.identity.internal.H2FinalTableIdentityColumnSupport;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.lock.internal.H2LockingSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.namespace.spi.NamespaceSupport;
import org.hibernate.dialect.namespace.spi.NamespaceSupports;
import org.hibernate.dialect.schema.spi.AlterColumnTypeRequest;
import org.hibernate.dialect.schema.spi.ColumnDefinitionRequest;
import org.hibernate.dialect.schema.spi.ConstraintControlMode;
import org.hibernate.dialect.schema.spi.ConstraintDropMode;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.schema.spi.IfExistsSupport;
import org.hibernate.dialect.schema.spi.SchemaDropSupport;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.OffsetFetchLimitHandler;
import org.hibernate.dialect.sequence.internal.H2V2SequenceSupport;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.sql.ast.internal.H2SqlAstTranslator;
import org.hibernate.dialect.temptable.internal.H2GlobalTemporaryTableStrategy;
import org.hibernate.dialect.temptable.spi.StandardLocalTemporaryTableStrategy;
import org.hibernate.dialect.temptable.spi.TemporaryTableStrategy;
import org.hibernate.dialect.type.spi.H2JdbcTypes;
import org.hibernate.dialect.type.spi.PostgreSQLJdbcTypes;
import org.hibernate.dialect.unique.spi.UniqueDelegates;
import org.hibernate.dialect.unique.spi.UniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.dialect.sql.ast.spi.FetchClauseSupport;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.dialect.temporaltype.spi.IntervalType;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.sql.spi.ParameterMarkerStrategy;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.dialect.sql.ast.spi.OptionalTableUpdateOperationRequest;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractors;
import org.hibernate.type.descriptor.jdbc.EnumJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.OrdinalEnumJdbcType;
import org.hibernate.type.descriptor.jdbc.TimeUtcAsOffsetTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsInstantJdbcType;
import org.hibernate.type.descriptor.jdbc.UUIDJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;


import static org.hibernate.dialect.array.spi.ArraySupport.MultiValuedParameterStrategy.EXPANDED;
import java.sql.Types;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.jdbc.spi.JdbcExceptionHelper.extractErrorCode;
import static org.hibernate.internal.util.StringHelper.split;
import static org.hibernate.query.common.TemporalUnit.SECOND;
import static org.hibernate.type.SqlTypes.BIGINT;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.DOUBLE;
import static org.hibernate.type.SqlTypes.FLOAT;
import static org.hibernate.type.SqlTypes.GEOMETRY;
import static org.hibernate.type.SqlTypes.INTERVAL_SECOND;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.LONG32NVARCHAR;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.LONG32VARCHAR;
import static org.hibernate.type.SqlTypes.NCHAR;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.NVARCHAR;
import static org.hibernate.type.SqlTypes.OTHER;
import static org.hibernate.type.SqlTypes.UUID;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.SqlTypes.VARCHAR;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsDate;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsLocalTime;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTime;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTimestampWithMillis;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTimestampWithNanos;

/**
 * A {@linkplain Dialect SQL dialect} for H2.
 * <p>
 * Please refer to the
 * <a href="http://www.h2database.com/html/main.html">H2 documentation</a>.
 *
 *
 * @author Thomas Mueller
 * @author Jürgen Kreitler
 * @author Yoobin Yoon
 */
public class H2Dialect extends Dialect implements CurrentTemporalSupport, TemporalFormatSupport, TemporalOperationSupport {
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
	private final TypeSizingProfile typeSizingProfile = TypeSizingProfile.builder( super.getTypeSizingProfile() )
			.maxVarcharLength( 1_048_576 ).maxVarcharCapacity( 1_048_576 )
			.maxNVarcharLength( 1_048_576 ).maxNVarcharCapacity( 1_048_576 )
			.maxVarbinaryLength( 1_048_576 ).maxVarbinaryCapacity( 1_048_576 )
			.build();

	@Override
	public TypeSizingProfile getTypeSizingProfile() {
		return typeSizingProfile;
	}
	private static final ArraySupport ARRAY_SUPPORT = ArraySupport.builder( ArraySupport.STANDARD )
			.multiValuedParameterStrategy( EXPANDED )
			.build();
	private static final DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 2, 1, 214 );

	private final SequenceInformationExtractor sequenceInformationExtractor;
	private final UniqueDelegate uniqueDelegate = new org.hibernate.dialect.unique.spi.DelegatingUniqueDelegate(
			UniqueDelegates.createTable( this ) ) {
		@Override
		public boolean supportsNullsNotDistinct() {
			return true;
		}
	};

	public H2Dialect(DialectResolutionInfo info) {
		this( staticDetermineDatabaseVersion( info ) );
	}

	public H2Dialect() {
		this( MINIMUM_VERSION );
	}

	public H2Dialect(DatabaseVersion version) {
		super( version );

		sequenceInformationExtractor = SequenceInformationExtractors.builder(
				"select * from INFORMATION_SCHEMA.SEQUENCES"
		).build();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public DatabaseVersion determineDatabaseVersion(DialectResolutionInfo info) {
		return staticDetermineDatabaseVersion( info );
	}

	// Static version necessary to call from constructor
	private static DatabaseVersion staticDetermineDatabaseVersion(DialectResolutionInfo info) {
		final DatabaseVersion version = info.makeCopyOrDefault( MINIMUM_VERSION );
		return info.getDatabaseVersion() != null
				? DatabaseVersion.make( version.getMajor(), version.getMinor(), parseBuildId( info ) )
				: version;
	}

	private static int parseBuildId(DialectResolutionInfo info) {
		final String databaseVersion = info.getDatabaseVersion();
		if ( databaseVersion == null ) {
			return 0;
		}
		else {
			final String[] bits = split( ". -", databaseVersion );
			return bits.length > 2 ? Integer.parseInt( bits[2] ) : 0;
		}
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_VERSION;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void contributeDefaultProperties(java.util.Properties properties) {
		super.contributeDefaultProperties( properties );
		// http://code.google.com/p/h2database/issues/detail?id=235
		properties.setProperty( org.hibernate.cfg.AvailableSettings.NON_CONTEXTUAL_LOB_CREATION, "true" );
		properties.setProperty( org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE, "15" );
	}

	@Override
	public ArraySupport getArraySupport() {
		// Array binding performs worse than parameter expansion on H2
		return ARRAY_SUPPORT;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		return switch (sqlTypeCode) {
			// h2 recognizes NCHAR and NCLOB as aliases
			// but, according to the docs, not NVARCHAR
			// so just normalize all these types
			case NCHAR -> columnType( CHAR );
			case NVARCHAR -> columnType( VARCHAR );
			case NCLOB -> columnType( CLOB );
			default -> super.columnType( sqlTypeCode );
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String castType(int sqlTypeCode) {
		return switch (sqlTypeCode) {
			case CHAR, NCHAR -> "char";
			case VARCHAR, NVARCHAR, LONG32VARCHAR, LONG32NVARCHAR -> "varchar";
			case BINARY, VARBINARY, LONG32VARBINARY -> "varbinary";
			default -> super.castType( sqlTypeCode );
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String narrowCastType(int sqlTypeCode) {
		// H2 misbehaves when casting to clob/blob (in particular, array_agg
		// on clob produces funky results), so use unsized varchar/varbinary
		// in narrow cast positions instead — consistent with castType().
		return switch (sqlTypeCode) {
			case CLOB, NCLOB -> "varchar";
			case BLOB -> "varbinary";
			default -> super.narrowCastType( sqlTypeCode );
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final var ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( UUID, "uuid", this ) );
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( GEOMETRY, "geometry", this ) );
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( INTERVAL_SECOND, "interval second($p,$s)", this ) );
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( JSON, "json", this ) );
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.nativeEnum( this ) );
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.nativeOrdinalEnum( this ) );
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.namedNativeEnum() );
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.namedNativeOrdinalEnum() );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );
		final var jdbcTypeRegistry =
				typeContributions.getTypeConfiguration()
						.getJdbcTypeRegistry();
		jdbcTypeRegistry.addDescriptor( TimeUtcAsOffsetTimeJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( TimestampUtcAsInstantJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptorIfAbsent( UUIDJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptorIfAbsent( H2JdbcTypes.durationIntervalSecond() );
		jdbcTypeRegistry.addDescriptorIfAbsent( H2JdbcTypes.json() );
		// Replace the standard array constructor
		jdbcTypeRegistry.addTypeConstructor( H2JdbcTypes.jsonArrayConstructor() );
		jdbcTypeRegistry.addDescriptor( EnumJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( OrdinalEnumJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( PostgreSQLJdbcTypes.enumType() );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public AggregateSupport getAggregateSupport() {
		return H2AggregateSupport.valueOf( this );
	}

	public boolean hasOddDstBehavior() {
		// H2 1.4.200 has a bug: https://github.com/h2database/h2database/issues/3184
		return true;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry( functionContributions );

		final var functionFactory = new CommonFunctionFactory( functionContributions );

		// H2 needs an actual argument type for aggregates like SUM, AVG, MIN, MAX to determine the result type
		functionFactory.aggregates( this, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		// AVG by default uses the input type, so we possibly need to cast the argument type, hence a special function
		functionFactory.avg_castingNonDoubleArguments( this, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );

		functionFactory.pi();
		functionFactory.cot();
		functionFactory.radians();
		functionFactory.degrees();
		functionFactory.log10();
		functionFactory.mod_operator();
		functionFactory.rand();
		functionFactory.soundex();
		functionFactory.translate();
		functionFactory.bitand();
		functionFactory.bitor();
		functionFactory.bitxor();
		functionFactory.bitnot();
		functionFactory.bitAndOr();
		functionFactory.yearMonthDay();
		functionFactory.hourMinuteSecond();
		functionFactory.dayOfWeekMonthYear();
		functionFactory.weekQuarter();
		functionFactory.daynameMonthname();

		functionFactory.localtimeLocaltimestamp();

		functionFactory.trunc_dateTrunc();
		functionFactory.dateTrunc();
		functionFactory.bitLength();
		functionFactory.octetLength();
		functionFactory.ascii();
		functionFactory.octetLength();
		functionFactory.space();
		functionFactory.repeat();
		functionFactory.chr_char();
		functionFactory.instr();
		functionFactory.substr();
		//also natively supports ANSI-style substring()
		functionFactory.position();
		functionFactory.trim1();
		functionFactory.concat_pipeOperator();
		functionFactory.nowCurdateCurtime();
		functionFactory.sysdate();
		functionFactory.insert();
//		functionFactory.everyAny(); //this would work too
		functionFactory.everyAny_boolAndOr();
		functionFactory.median();
		functionFactory.stddevPopSamp();
		functionFactory.varPopSamp();
		functionFactory.format_formatdatetime();
		functionFactory.rownum();
		functionFactory.windowFunctions();
		functionFactory.listagg( null );
		functionFactory.inverseDistributionOrderedSetAggregates();
		functionFactory.hypotheticalOrderedSetAggregates();
		functionFactory.array();
		functionFactory.arrayAggregate();
		functionFactory.arrayPosition_h2( getMaximumArraySize() );
		functionFactory.arrayPositions_h2( getMaximumArraySize() );
		functionFactory.arrayLength_cardinality();
		functionFactory.arrayConcat_operator();
		functionFactory.arrayPrepend_operator();
		functionFactory.arrayAppend_operator();
		functionFactory.arrayContains_h2( getMaximumArraySize() );
		functionFactory.arrayIntersects_h2( getMaximumArraySize() );
		functionFactory.arrayGet_h2();
		functionFactory.arraySet_h2( getMaximumArraySize() );
		functionFactory.arrayRemove_h2( getMaximumArraySize() );
		functionFactory.arrayRemoveIndex_h2( getMaximumArraySize() );
		functionFactory.arraySlice();
		functionFactory.arrayReplace_h2( getMaximumArraySize() );
		functionFactory.arrayTrim_trim_array();
		functionFactory.arrayReverse_h2( getMaximumArraySize() );
		functionFactory.arraySort_h2( getMaximumArraySize() );
		functionFactory.arrayFill_h2();
		functionFactory.arrayToString_h2( getMaximumArraySize() );

		functionFactory.jsonObject_h2();
		functionFactory.jsonArray_h2();
		if ( getVersion().isSameOrAfter( 2, 2, 220 ) ) {
			functionFactory.jsonValue_h2();
			functionFactory.jsonQuery_h2();
			functionFactory.jsonExists_h2();
			functionFactory.jsonArrayAgg_h2();
			functionFactory.jsonObjectAgg_h2();
		}

		functionFactory.xmlelement_h2();
		functionFactory.xmlcomment();
		functionFactory.xmlforest_h2();
		functionFactory.xmlconcat_h2();
		functionFactory.xmlpi_h2();

		functionFactory.unnest_h2( getMaximumArraySize() );
		functionFactory.generateSeries_h2( getMaximumSeriesSize() );
		functionFactory.jsonTable_h2( getMaximumArraySize() );

		functionFactory.hex( "rawtohex(?1)" );
		functionFactory.sha( "hash('SHA-256', ?1)" );
		functionFactory.md5( "hash('MD5', ?1)" );

		functionFactory.regexpLike();
	}

	/**
	 * H2 requires a very special emulation, because {@code unnest} is pretty much useless,
	 * due to <a href="https://github.com/h2database/h2database/issues/1815">issue 1815</a>.
	 * This emulation uses {@code array_get}, {@code array_length} and {@code system_range} functions to roughly achieve the same,
	 * but requires that {@code system_range} is fed with a "maximum array size".
	 */
	protected int getMaximumArraySize() {
		return 1000;
	}

	/**
	 * Since H2 doesn't support ordinality for the {@code system_range} function or {@code lateral},
	 * it's impossible to use {@code system_range} for non-constant cases.
	 * Luckily, correlation can be emulated, but requires that there is an upper bound on the amount
	 * of elements that the series can return.
	 */
	protected int getMaximumSeriesSize() {
		return 10000;
	}

	@Override
	public @Nullable String getDefaultOrdinalityColumnName() {
		return "nord";
	}

	@Override
	@SPI(IMPLEMENT)
	public void augmentPhysicalTableTypes(List<String> tableTypesList) {
		tableTypesList.add( "BASE TABLE" );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected Integer resolveSqlTypeCode(String columnTypeName, TypeConfiguration typeConfiguration) {
		return switch (columnTypeName) {
			// Use REAL instead of FLOAT to get Float as recommended Java type
			case "FLOAT(24)" -> Types.REAL;
			default -> super.resolveSqlTypeCode( columnTypeName, typeConfiguration );
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public JdbcType resolveSqlTypeDescriptor(
			String columnTypeName,
			int jdbcTypeCode,
			int precision,
			int scale,
			JdbcTypeRegistry jdbcTypeRegistry) {
		// As of H2 2.0 we get a FLOAT type code even though it is a DOUBLE
		switch ( jdbcTypeCode ) {
			case FLOAT:
				if ( "DOUBLE PRECISION".equals( columnTypeName ) ) {
					return jdbcTypeRegistry.getDescriptor( DOUBLE );
				}
				break;
			case OTHER:
				if ( "GEOMETRY".equals( columnTypeName ) ) {
					return jdbcTypeRegistry.getDescriptor( GEOMETRY );
				}
				else if ( "JSON".equals( columnTypeName ) ) {
					return jdbcTypeRegistry.getDescriptor( JSON );
				}
				break;
		}
		return super.resolveSqlTypeDescriptor( columnTypeName, jdbcTypeCode, precision, scale, jdbcTypeRegistry );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected Integer resolveSqlTypeCode(String typeName, String baseTypeName, TypeConfiguration typeConfiguration) {
		return switch (baseTypeName) {
			case "CHARACTER VARYING" -> VARCHAR;
			default -> super.resolveSqlTypeCode( typeName, baseTypeName, typeConfiguration );
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTime() {
		return  "localtime";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTimestamp() {
		return "localtimestamp";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTimestampWithTimeZone() {
		return "current_timestamp";
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new H2SqlAstTranslator<>( request );
			}
		};
	}

	/**
	 * In H2, the extract() function does not return
	 * fractional seconds for the field
	 * {@link TemporalUnit#SECOND}. We work around
	 * this here with two calls to extract().
	 */
	@Override
	@SPI({ USE, IMPLEMENT })
	public String extractPattern(TemporalUnit unit) {
		return unit == SECOND
				? "(" + TemporalOperationSupports.standard().extractPattern(unit) + "+extract(nanosecond from ?2)/1e9)"
				: TemporalOperationSupports.standard().extractPattern(unit);
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

	@Override @SuppressWarnings("deprecation")
	@SPI({ USE, IMPLEMENT })
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		if ( intervalType != null ) {
			return "(?2+?3)";
		}
		return unit == SECOND
				//TODO: if we have an integral number of seconds
				//      (the common case) this is unnecessary
				? "dateadd(nanosecond,?2*1e9,?3)"
				: "dateadd(?1,?2,?3)";
	}

	@Override @SuppressWarnings("deprecation")
	@SPI({ USE, IMPLEMENT })
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		if ( unit == null ) {
			return "(?3-?2)";
		}
		return "datediff(?1,?2,?3)";
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
				if ( supportsTimeLiteralOffset() && temporalAccessor.isSupported( ChronoField.OFFSET_SECONDS )  ) {
					appender.appendSql( "time with time zone '" );
					appendAsTime( appender, temporalAccessor, getTemporalValueSemantics().supportsLiteralOffset(), jdbcTimeZone );
				}
				else {
					appender.appendSql( "time '" );
					appendAsLocalTime( appender, temporalAccessor );
				}
				appender.appendSql( '\'' );
				break;
			case TIMESTAMP:
				if ( getTemporalValueSemantics().supportsLiteralOffset() && temporalAccessor.isSupported( ChronoField.OFFSET_SECONDS ) ) {
					appender.appendSql( "timestamp with time zone '" );
					appendAsTimestampWithNanos( appender, temporalAccessor, true, jdbcTimeZone );
					appender.appendSql( '\'' );
				}
				else {
					appender.appendSql( "timestamp '" );
					appendAsTimestampWithNanos( appender, temporalAccessor, false, jdbcTimeZone );
					appender.appendSql( '\'' );
				}
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
				if ( supportsTimeLiteralOffset() ) {
					appender.appendSql( "time with time zone '" );
					appendAsTime( appender, date, jdbcTimeZone );
				}
				else {
					appender.appendSql( "time '" );
					appendAsLocalTime( appender, date );
				}
				appender.appendSql( '\'' );
				break;
			case TIMESTAMP:
				appender.appendSql( "timestamp with time zone '" );
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
				if ( supportsTimeLiteralOffset() ) {
					appender.appendSql( "time with time zone '" );
					appendAsTime( appender, calendar, jdbcTimeZone );
				}
				else {
					appender.appendSql( "time '" );
					appendAsLocalTime( appender, calendar );
				}
				appender.appendSql( '\'' );
				break;
			case TIMESTAMP:
				appender.appendSql( "timestamp with time zone '" );
				appendAsTimestampWithMillis( appender, calendar, jdbcTimeZone );
				appender.appendSql( '\'' );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	public boolean supportsTimeLiteralOffset() {
		return true;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalValueSemantics getTemporalValueSemantics() {
		return TemporalValueSemantics.OFFSET_LITERALS;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TimeZoneSupport getTimeZoneSupport() {
		return TimeZoneSupport.NATIVE;
	}

	@Override
	public PredicateSupport getPredicateSupport() {
		return PredicateSupport.builder( super.getPredicateSupport() )
				.caseInsensitiveLikeOperator( "ilike" )
				.capabilities(
						PredicateSupport.Capability.DISTINCT_FROM,
						PredicateSupport.Capability.TRUTHNESS
				)
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		appender.appendSql( bool );
	}

	@Override
	public LimitHandler getLimitHandler() {
		return OffsetFetchLimitHandler.INSTANCE;
	}

	@Override
	public LockingSupport getLockingSupport() {
		return getVersion().isSameOrAfter( 2, 2, 220 ) ? H2LockingSupport.INSTANCE : H2LockingSupport.LEGACY_INSTANCE;
	}




	private String withRealTimeout(String lockString, Timeout timeout) {
		assert Timeouts.isRealTimeout( timeout );
		return lockString + " wait " + Timeouts.getTimeoutInSeconds( timeout );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized IfExistsSupport getIfExistsSupport() {
		if ( ifExistsSupport == null ) {
			ifExistsSupport = new IfExistsSupport(
				ExistenceCheckPlacement.BEFORE_NAME,
				ExistenceCheckPlacement.BEFORE_NAME,
				ExistenceCheckPlacement.BEFORE_NAME,
				ExistenceCheckPlacement.BEFORE_NAME
		);
		}
		return ifExistsSupport;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String alterColumnType(AlterColumnTypeRequest request) {
		return "alter column " + request.columnName() + " set data type " + request.columnType();
		// if only altering the type, no need to specify the whole definition
//		return "alter column " + columnName + " " + columnDefinition;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public org.hibernate.dialect.schema.spi.SchemaCommentSupport getSchemaCommentSupport() {
		return org.hibernate.dialect.schema.spi.SchemaCommentSupports.commentOn();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized SchemaDropSupport getSchemaDropSupport() {
		if ( schemaDropSupport == null ) {
			schemaDropSupport = new SchemaDropSupport( List.of(), ConstraintDropMode.IMPLICIT, " cascade " );
		}
		return schemaDropSupport;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		return H2V2SequenceSupport.getInstance();
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return sequenceInformationExtractor;
	}

	@Override
	public NullOrderingSupport getNullOrderingSupport() {
		return NullOrderingSupport.builder( super.getNullOrderingSupport() )
				.defaultOrdering( NullOrdering.SMALLEST )
				.build();
	}

	@Override
	public MultiTableMutationSupport getMultiTableMutationSupport() {
		return MultiTableMutationSupport.GLOBAL_TEMPORARY_TABLE;
	}

	@Override
	public TemporaryTableStrategy getGlobalTemporaryTableStrategy() {
		return H2GlobalTemporaryTableStrategy.INSTANCE;
	}

	@Override
	public TemporaryTableStrategy getLocalTemporaryTableStrategy() {
		return StandardLocalTemporaryTableStrategy.INSTANCE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}

	private static final ViolatedConstraintNameExtractor EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor( sqle -> switch ( extractErrorCode( sqle ) ) {
				case 23505 -> {
					// Unique index or primary key violation
					final String constraint =
							extractUsingTemplate( "violation: \"", "\"", sqle.getMessage() );
					final int onIndex = constraint == null ? -1 : constraint.indexOf( " ON " );
					yield onIndex > 0 ? constraint.substring( 0, onIndex ) : constraint;
				}
				case 23502 ->
					// NULL not allowed for column
						extractUsingTemplate( "column \"", "\"", sqle.getMessage() );
				case 23503, 23506, 23513, 23514 ->
					// Referential integrity or check constraint violation
						extractUsingTemplate( "constraint violation: \"", ":", sqle.getMessage() );
				default -> null;
			} );

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) ->
				switch ( extractErrorCode( sqlException ) ) {
					case 40001 ->
						// DEADLOCK DETECTED
							new LockAcquisitionException(message, sqlException, sql);
					case 50200 ->
						// LOCK NOT AVAILABLE
							new LockTimeoutException(message, sqlException, sql);
					case 23505 ->
						// Unique index or primary key violation
							new ConstraintViolationException( message, sqlException, sql, ConstraintKind.UNIQUE,
									getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
					case 23502 ->
						// NULL not allowed for column
							new ConstraintViolationException( message, sqlException, sql, ConstraintKind.NOT_NULL,
									getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
					case 23503, 23506 ->
						// Referential integrity constraint violation
							new ConstraintViolationException( message, sqlException, sql, ConstraintKind.FOREIGN_KEY,
									getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
					case 23513, 23514 ->
						// Check constraint violation
							new ConstraintViolationException( message, sqlException, sql, ConstraintKind.CHECK,
									getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
					case 57014 ->
						// QUERY CANCELLED
							new QueryTimeoutException( message, sqlException, sql );
					default -> null;
				};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public CurrentTimestampSelection getCurrentTimestampSelection() {
		return CurrentTimestampSelection.prepared( "call current_timestamp()" );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean isCurrentTimestampStable() {
		return true;
	}


	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public TupleCountSupport getTupleCountSupport() {
		return TupleCountSupport.builder()
				.nonDistinctSyntax( TupleCountSupport.Syntax.PARENTHESIZED_TUPLE )
				.distinctSyntax( TupleCountSupport.Syntax.PARENTHESIZED_TUPLE )
				.build();
	}

	@Override
	public SubquerySupport getSubquerySupport() {
		return SubquerySupport.builder()
				.feature( SubquerySupport.Feature.OFFSET, true )
				.feature( SubquerySupport.Feature.MUTATION_JOIN, false )
				.build();
	}

	@Override
	public WindowFunctionSupport getWindowFunctionSupport() {
		return WindowFunctionSupport.builder()
				.features( WindowFunctionSupport.Feature.values() )
				.build();
	}

	@Override
	public CteSupport getCteSupport() {
		return CteSupport.builder()
				.placement( CteSupport.Placement.TOP_LEVEL )
				.recursiveFeatures( CteSupport.RecursiveFeature.RECURSIVE )
				.build();
	}

	@Override
	public SetOperationSupport getSetOperationSupport() {
		return SetOperationSupport.builder()
				.operator( SetOperator.INTERSECT_ALL, false )
				.operator( SetOperator.EXCEPT_ALL, false )
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public FetchClauseSupport getFetchClauseSupport() {
		return FetchClauseSupport.ALL;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public FunctionalDependencyAnalysisSupport getFunctionalDependencyAnalysisSupport() {
		return FunctionalDependencyAnalysisSupport.TABLE_GROUP_AND_CONSTANTS;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return H2FinalTableIdentityColumnSupport.INSTANCE;
	}

	/**
	 * @return {@code true} because we can use {@code select ... from final table (insert .... )}
	 */
	@Override
	public GeneratedValuesSupport getGeneratedValuesSupport() {
		return GeneratedValuesSupport.builder( super.getGeneratedValuesSupport() )
				.enable(
						GeneratedValuesSupport.Capability.INSERT_RETURNING,
						GeneratedValuesSupport.Capability.UPDATE_RETURNING,
						GeneratedValuesSupport.Capability.ARBITRARY_GENERATED_KEYS
				)
				.unquoteGeneratedKeyColumnNames( true )
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String getQueryHintString(String query, String hints) {
		return org.hibernate.dialect.queryhint.spi.QueryHints.addUseIndexHint( query, hints );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendFormat(SqlAppender appender, String format) {
		appender.appendSql(
				new Replacer( format, "'", "''" )
				.replace("e", "u")
				.replace( "xxx", "XXX" )
				.replace( "xx", "XX" )
				.replace( "x", "X" )
				.result()
		);
	}

	@SPI({ USE, IMPLEMENT })
	public String translateExtractField(TemporalUnit unit) {
		return switch (unit) {
			case DAY_OF_MONTH -> "day";
			case WEEK -> "iso_week";
			default -> unit.toString();
		};
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
	@SPI({ USE, IMPLEMENT })
	public ConstraintControlMode constraintControlMode() {
		return ConstraintControlMode.GLOBAL;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public List<String> enableCommands() {
		return List.of( "set referential_integrity true" );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public EnumSupport getEnumSupport() {
		return EnumSupports.h2();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public List<String> disableCommands() {
		return List.of( "set referential_integrity false" );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public RowIdSupport getRowIdSupport() {
		return RowIdSupports.fixed( "_rowid_", BIGINT );
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public MutationOperation createOptionalTableUpdateOperation(
			OptionalTableUpdateOperationRequest request) {
		final var optionalTableUpdate = request.update();
		final var factory = request.sessionFactory();
		return new H2SqlAstTranslator<>( new SqlAstTranslationRequest.ModelMutation<>( factory, optionalTableUpdate ) )
				.createMergeOperation( optionalTableUpdate );
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public ParameterMarkerStrategy getNativeParameterMarkerStrategy() {
		return OrdinalParameterMarkerStrategy.INSTANCE;
	}

	public static class OrdinalParameterMarkerStrategy implements ParameterMarkerStrategy {
		/**
		 * Singleton access
		 */
		public static final OrdinalParameterMarkerStrategy INSTANCE = new OrdinalParameterMarkerStrategy();

		@Override
		public String createMarker(int position, JdbcType jdbcType) {
			return "?" + position;
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public ObjectNullBindingStrategy getObjectNullBindingStrategy() {
		return ObjectNullBindingStrategy.SET_NULL_WITH_NULL_TYPE;
	}

	@Override
	public ValuesListSupport getValuesListSupport() {
		return ValuesListSupport.STANDARD;
	}

	@Override
	public SingleRowTableSupport getSingleRowTableSupport() {
		return SingleRowTableSupport.builder( super.getSingleRowTableSupport() )
				.tableExpression( "dual" )
				.build();
	}

	@Override
	public boolean supportsFilterClause() {
		// Introduction of FILTER clause https://github.com/h2database/h2database/commit/9e6dbf3baa57000f670826ede431dc7fb4cd9d9c
		return true;
	}

	@Override
	public RowValueSupport getRowValueSupport() {
		return RowValueSupport.builder( super.getRowValueSupport() )
				.feature( RowValueSupport.Feature.ROW_CONSTRUCTOR, true )
				.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NamespaceSupport getNamespaceSupport() {
		return NamespaceSupports.standard( true, true );
	}

}
