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

import org.hibernate.dialect.type.spi.EnumSupport;
import org.hibernate.dialect.type.spi.EnumSupports;
import org.hibernate.dialect.type.spi.ObjectNullBindingStrategy;
import org.hibernate.dialect.rowid.spi.RowIdSupport;
import org.hibernate.dialect.rowid.spi.RowIdSupports;

import org.hibernate.SPI;
import static org.hibernate.SPI.Role.USE;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import org.hibernate.dialect.type.spi.StandardDdlTypes;

import org.hibernate.dialect.type.spi.TypeSizingProfile;

import org.hibernate.dialect.mutation.spi.MultiTableMutationSupport;


import org.hibernate.dialect.array.spi.ArraySupport;
import org.hibernate.dialect.function.spi.Replacer;
import org.hibernate.dialect.function.spi.TupleCountSupport;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;

import org.hibernate.dialect.type.spi.TimeZoneSupport;

import org.hibernate.dialect.aggregate.spi.FunctionalDependencyAnalysisSupport;


import org.hibernate.dialect.sql.ast.spi.DmlTargetColumnQualifierSupport;

import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.NullOrderingSupport;
import org.hibernate.dialect.sql.ast.spi.PredicateSupport;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.SingleRowTableSupport;
import org.hibernate.dialect.sql.ast.spi.SetOperationSupport;
import org.hibernate.dialect.sql.ast.spi.ValuesListSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;

import java.sql.Types;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import jakarta.annotation.Nullable;
import org.hibernate.PessimisticLockException;
import org.hibernate.QueryTimeoutException;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.*;
import org.hibernate.dialect.aggregate.spi.AggregateSupport;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.community.dialect.identity.internal.H2FinalTableIdentityColumnSupport;
import org.hibernate.community.dialect.identity.internal.H2IdentityColumnSupport;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.StandardLockingSupports;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.LimitOffsetLimitHandler;
import org.hibernate.dialect.pagination.spi.OffsetFetchLimitHandler;
import org.hibernate.community.dialect.sequence.H2V1SequenceSupport;
import org.hibernate.community.dialect.sequence.CommunitySequenceSupports;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.schema.spi.ColumnDefinitionRequest;
import org.hibernate.dialect.schema.spi.ConstraintControlMode;
import org.hibernate.dialect.schema.spi.ConstraintDropMode;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.schema.spi.IfExistsSupport;
import org.hibernate.dialect.schema.spi.SchemaDropSupport;
import org.hibernate.dialect.temptable.spi.StandardLocalTemporaryTableStrategy;
import org.hibernate.dialect.temptable.spi.TemporaryTableStrategy;
import org.hibernate.dialect.type.spi.H2JdbcTypes;
import org.hibernate.dialect.unique.spi.UniqueDelegates;
import org.hibernate.dialect.unique.spi.UniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.jdbc.spi.JdbcExceptionHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.dialect.sql.ast.spi.FetchClauseSupport;
import org.hibernate.dialect.temporaltype.spi.IntervalType;
import org.hibernate.dialect.sql.ast.spi.NullOrdering;
import org.hibernate.query.common.TemporalUnit;
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
import org.hibernate.type.descriptor.jdbc.EnumJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.OrdinalEnumJdbcType;
import org.hibernate.type.descriptor.jdbc.TimeAsTimestampWithTimeZoneJdbcType;
import org.hibernate.type.descriptor.jdbc.TimeUtcAsJdbcTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimeUtcAsOffsetTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsInstantJdbcType;
import org.hibernate.type.descriptor.jdbc.UUIDJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;
import org.jboss.logging.Logger;

import jakarta.persistence.TemporalType;

import static org.hibernate.query.common.TemporalUnit.SECOND;
import static org.hibernate.type.SqlTypes.ARRAY;
import static org.hibernate.type.SqlTypes.BIGINT;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.DECIMAL;
import static org.hibernate.type.SqlTypes.DOUBLE;
import static org.hibernate.type.SqlTypes.FLOAT;
import static org.hibernate.type.SqlTypes.GEOMETRY;
import static org.hibernate.type.SqlTypes.INTERVAL_SECOND;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.LONG32NVARCHAR;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.LONG32VARCHAR;
import static org.hibernate.type.SqlTypes.NCHAR;
import static org.hibernate.type.SqlTypes.NUMERIC;
import static org.hibernate.type.SqlTypes.NVARCHAR;
import static org.hibernate.type.SqlTypes.OTHER;
import static org.hibernate.type.SqlTypes.TIMESTAMP_UTC;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.UUID;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.SqlTypes.VARCHAR;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsDate;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsLocalTime;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTime;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTimestampWithMillis;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTimestampWithNanos;

/**
 * A legacy {@linkplain Dialect SQL dialect} for H2.
 *
 * @author Thomas Mueller
 * @author Jürgen Kreitler
 * @author Yoobin Yoon
 */
public class H2LegacyDialect extends Dialect implements CurrentTemporalSupport, TemporalFormatSupport, TemporalOperationSupport {
	private static final Logger LOG = Logger.getLogger( H2LegacyDialect.class );
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

	@Override public TypeSizingProfile getTypeSizingProfile() { return typeSizingProfile; }
	private static final ArraySupport VERSION_2_ARRAY_SUPPORT = ArraySupport.builder( ArraySupport.STANDARD )
			.multiValuedParameterStrategy( ArraySupport.MultiValuedParameterStrategy.EXPANDED )
			.build();

	private final LimitHandler limitHandler;

	private final boolean ansiSequence;
	private final boolean cascadeConstraints;
	private final boolean useLocalTime;

	private final SequenceInformationExtractor sequenceInformationExtractor;
	private final UniqueDelegate uniqueDelegate = new org.hibernate.dialect.unique.spi.DelegatingUniqueDelegate(
			UniqueDelegates.createTable( this ) ) {
		@Override
		public boolean supportsNullsNotDistinct() {
			return true;
		}
	};
	private static final SequenceInformationExtractor LEGACY_SEQUENCE_INFORMATION_EXTRACTOR =
			SequenceInformationExtractors.builder( "select * from INFORMATION_SCHEMA.SEQUENCES" )
					.withoutStartValue()
					.minimumValueColumn( "min_value" )
					.maximumValueColumn( "max_value" )
					.build();
	private static final SequenceInformationExtractor SEQUENCE_INFORMATION_EXTRACTOR =
			SequenceInformationExtractors.builder( "select * from INFORMATION_SCHEMA.SEQUENCES" ).build();

	public H2LegacyDialect(DialectResolutionInfo info) {
		this( parseVersion( info ) );
	}

	public H2LegacyDialect() {
		this( SimpleDatabaseVersion.ZERO_VERSION );
	}

	public H2LegacyDialect(DatabaseVersion version) {
		super(version);

		// https://github.com/h2database/h2database/commit/b2cdf84e0b84eb8a482fa7dccdccc1ab95241440
		limitHandler = version.isSameOrAfter( 1, 4, 195 )
				? OffsetFetchLimitHandler.INSTANCE
				: LimitOffsetLimitHandler.OFFSET_ONLY_INSTANCE;

		if ( version.isBefore( 1, 2, 139 ) ) {
			LOG.warnf(
					"The %s.%s.%s version of H2 implements temporary table creation such that it commits current "
							+ "transaction;multi-table, bulk HQL/JPQL will not work properly",
					version.getMajor(),
					version.getMinor(),
					version.getMicro()
			);
		}

//		supportsTuplesInSubqueries = version.isSameOrAfter( 1, 4, 198 );

		// Prior to 1.4.200 there was no support for 'current value for sequence_name'
		// After 2.0.202 there is no support for 'sequence_name.nextval' and 'sequence_name.currval'
		ansiSequence = version.isSameOrAfter( 1, 4, 200 );

		// Prior to 1.4.200 the 'cascade' in 'drop table' was implicit
		cascadeConstraints = version.isSameOrAfter( 1, 4, 200 );
		// 1.4.200 introduced changes in current_time and current_timestamp
		useLocalTime = version.isSameOrAfter( 1, 4, 200 );

		if ( version.isSameOrAfter( 1, 4, 32 ) ) {
			this.sequenceInformationExtractor = version.isSameOrAfter( 1, 4, 201 )
					? SEQUENCE_INFORMATION_EXTRACTOR
					: LEGACY_SEQUENCE_INFORMATION_EXTRACTOR;
		}
		else {
			this.sequenceInformationExtractor = SequenceInformationExtractors.none();
		}
	}

	private static DatabaseVersion parseVersion(DialectResolutionInfo info) {
		return DatabaseVersion.make( info.getMajor(), info.getMinor(), parseBuildId( info ) );
	}

	private static int parseBuildId(DialectResolutionInfo info) {
		final String databaseVersion = info.getDatabaseVersion();
		if ( databaseVersion == null ) {
			return 0;
		}

		final String[] bits = StringHelper.split( ". ", databaseVersion );
		return bits.length > 2 ? Integer.parseInt( bits[2] ) : 0;
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
		return getVersion().isSameOrAfter( 2 ) ? VERSION_2_ARRAY_SUPPORT : ArraySupport.NONE;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		switch ( sqlTypeCode ) {
			// prior to version 2.0, H2 reported NUMERIC columns as DECIMAL,
			// which caused problems for schema update tool
			case NUMERIC:
				return getVersion().isBefore( 2 ) ? columnType( DECIMAL ) : super.columnType( sqlTypeCode );
			// Support was only added in 2.0
			case TIME_WITH_TIMEZONE:
				return getVersion().isBefore( 2 ) ? columnType( TIMESTAMP_WITH_TIMEZONE ) : super.columnType( sqlTypeCode );
			case NCHAR:
				return columnType( CHAR );
			case NVARCHAR:
				return columnType( VARCHAR );
			default:
				return super.columnType( sqlTypeCode );
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String castType(int sqlTypeCode) {
		switch ( sqlTypeCode ) {
			case CHAR:
			case NCHAR:
				return "char";
			case VARCHAR:
			case NVARCHAR:
			case LONG32VARCHAR:
			case LONG32NVARCHAR:
				return "varchar";
			case BINARY:
			case VARBINARY:
			case LONG32VARBINARY:
				return "varbinary";
		}
		return super.castType( sqlTypeCode );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		if ( getVersion().isBefore( 2 ) ) {
			ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( ARRAY, "array", this ) );
		}
		if ( getVersion().isSameOrAfter( 1, 4, 197 ) ) {
			ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( UUID, "uuid", this ) );
			ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( GEOMETRY, "geometry", this ) );
			if ( getVersion().isSameOrAfter( 1, 4, 198 ) ) {
				ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( INTERVAL_SECOND, "interval second($p,$s)", this ) );
			}
			if ( getVersion().isSameOrAfter( 1, 4, 200 ) ) {
				ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( JSON, "json", this ) );
			}
		}
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.nativeEnum( this ) );
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.nativeOrdinalEnum( this ) );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );

		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration()
				.getJdbcTypeRegistry();

		if ( getVersion().isBefore( 2 ) ) {
			// Support for TIME_WITH_TIMEZONE was only added in 2.0
			jdbcTypeRegistry.addDescriptor( TimeAsTimestampWithTimeZoneJdbcType.INSTANCE );
			jdbcTypeRegistry.addDescriptor( TimeUtcAsJdbcTimeJdbcType.INSTANCE );
		}
		else {
			jdbcTypeRegistry.addDescriptor( TimeUtcAsOffsetTimeJdbcType.INSTANCE );
		}
		jdbcTypeRegistry.addDescriptor( TIMESTAMP_UTC, TimestampUtcAsInstantJdbcType.INSTANCE );
		if ( getVersion().isSameOrAfter( 1, 4, 197 ) ) {
			jdbcTypeRegistry.addDescriptorIfAbsent( UUIDJdbcType.INSTANCE );
		}
		if ( getVersion().isSameOrAfter( 1, 4, 198 ) ) {
			jdbcTypeRegistry.addDescriptorIfAbsent( H2JdbcTypes.durationIntervalSecond() );
		}
		if ( getVersion().isSameOrAfter( 1, 4, 200 ) ) {
			jdbcTypeRegistry.addDescriptorIfAbsent( H2JdbcTypes.json() );
			// Replace the standard array constructor
			jdbcTypeRegistry.addTypeConstructor( H2JdbcTypes.jsonArrayConstructor() );
		}
		jdbcTypeRegistry.addDescriptor( EnumJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( OrdinalEnumJdbcType.INSTANCE );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public AggregateSupport getAggregateSupport() {
		return new H2Dialect( getVersion() ).getAggregateSupport();
	}

	public boolean hasOddDstBehavior() {
		// H2 1.4.200 has a bug: https://github.com/h2database/h2database/issues/3184
		return getVersion().isSameOrAfter( 1, 4, 200 );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);

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
		if ( useLocalTime ) {
			functionFactory.localtimeLocaltimestamp();
		}
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
		if ( getVersion().isSame( 1, 4, 200 ) ) {
			// See https://github.com/h2database/h2database/issues/2518
			functionFactory.format_toChar();
		}
		else {
			functionFactory.format_formatdatetime();
		}
		functionFactory.rownum();
		if ( getVersion().isSameOrAfter( 1, 4, 200 ) ) {
			functionFactory.windowFunctions();
			functionFactory.inverseDistributionOrderedSetAggregates();
			functionFactory.hypotheticalOrderedSetAggregates();
			if ( getVersion().isSameOrAfter( 2 ) ) {
				functionFactory.listagg( null );
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

				if ( getVersion().isSameOrAfter( 2, 2, 220 ) ) {
					functionFactory.jsonValue_h2();
					functionFactory.jsonQuery_h2();
					functionFactory.jsonExists_h2();
					functionFactory.jsonArrayAgg_h2();
					functionFactory.jsonObjectAgg_h2();
				}
			}
			else {
				functionFactory.jsonObject_h2();
				functionFactory.jsonArray_h2();

				// Use group_concat until 2.x as listagg was buggy
				functionFactory.listagg_groupConcat();
			}

			functionFactory.xmlelement_h2();
			functionFactory.xmlcomment();
			functionFactory.xmlforest_h2();
			functionFactory.xmlconcat_h2();
			functionFactory.xmlpi_h2();
		}
		else {
			functionFactory.listagg_groupConcat();
		}

		functionFactory.unnest_h2( getMaximumArraySize() );
		functionFactory.generateSeries_h2( getMaximumSeriesSize() );
		functionFactory.jsonTable_h2( getMaximumArraySize() );

		if ( getVersion().isSameOrAfter( 1, 4, 193 ) ) {
			functionFactory.regexpLike();
		}
	}

	/**
	 * H2 requires a very special emulation, because {@code unnest} is pretty much useless,
	 * due to https://github.com/h2database/h2database/issues/1815.
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
		if ( getVersion().isSameOrAfter( 2 ) ) {
			tableTypesList.add( "BASE TABLE" );
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected Integer resolveSqlTypeCode(String columnTypeName, TypeConfiguration typeConfiguration) {
		switch ( columnTypeName ) {
			case "FLOAT(24)":
				// Use REAL instead of FLOAT to get Float as recommended Java type
				return Types.REAL;
		}
		return super.resolveSqlTypeCode( columnTypeName, typeConfiguration );
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
		switch ( baseTypeName ) {
			case "CHARACTER VARYING":
				return VARCHAR;
		}
		return super.resolveSqlTypeCode( typeName, baseTypeName, typeConfiguration );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTime() {
		return useLocalTime ? "localtime" : CurrentTemporalSupport.super.currentTime();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTimestamp() {
		return useLocalTime ? "localtimestamp" : CurrentTemporalSupport.super.currentTimestamp();
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
				return new H2LegacySqlAstTranslator<>( request );
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

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		if ( intervalType != null ) {
			return "(?2+?3)";
		}
		return "dateadd(?1,?2,?3)";
	}

	@Override
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
	public void appendDateTimeLiteral(SqlAppender appender, Date date, TemporalType precision, TimeZone jdbcTimeZone) {
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
		return getVersion().isSameOrAfter( 1, 4, 200 );
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
	@SPI({ USE, IMPLEMENT })
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		appender.appendSql( bool );
	}

	@Override
	public LimitHandler getLimitHandler() {
		return limitHandler;
	}

	@Override
	public LockingSupport getLockingSupport() {
		return StandardLockingSupports.h2( getVersion() );
	}

	@Override
	public PredicateSupport getPredicateSupport() {
		final PredicateSupport.Builder builder = PredicateSupport.builder( super.getPredicateSupport() )
				.capability( PredicateSupport.Capability.DISTINCT_FROM, true );
		if ( getVersion().isSameOrAfter( 1, 4, 194 ) ) {
			builder.caseInsensitiveLikeOperator( "ilike" );
		}
		return builder.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized IfExistsSupport getIfExistsSupport() {
		if ( ifExistsSupport == null ) {
			ifExistsSupport = new IfExistsSupport(
				cascadeConstraints ? ExistenceCheckPlacement.BEFORE_NAME : ExistenceCheckPlacement.NONE,
				cascadeConstraints ? ExistenceCheckPlacement.BEFORE_NAME : ExistenceCheckPlacement.AFTER_NAME,
				ExistenceCheckPlacement.BEFORE_NAME,
				ExistenceCheckPlacement.NONE
		);
		}
		return ifExistsSupport;
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
			schemaDropSupport = new SchemaDropSupport(
				List.of(),
				ConstraintDropMode.IMPLICIT,
				cascadeConstraints ? " cascade " : ""
		);
		}
		return schemaDropSupport;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		return ansiSequence ? CommunitySequenceSupports.h2v2() : H2V1SequenceSupport.INSTANCE;
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return sequenceInformationExtractor;
	}

	@Override
	public NullOrderingSupport getNullOrderingSupport() {
		return NullOrderingSupport.builder( super.getNullOrderingSupport() )
				.defaultOrdering( NullOrdering.SMALLEST )
				.capability(
						NullOrderingSupport.Capability.NULLS_FIRST_LAST,
						getVersion().isSameOrAfter( 2 )
				)
				.build();
	}

	@Override
	public MultiTableMutationSupport getMultiTableMutationSupport() {
		return MultiTableMutationSupport.LOCAL_TEMPORARY_TABLE;
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
			new TemplatedViolatedConstraintNameExtractor( sqle -> {
				// 23000: Check constraint violation: {0}
				// 23001: Unique index or primary key violation: {0}
				if ( sqle.getSQLState().startsWith( "23" ) ) {
					final String message = sqle.getMessage();
					final int idx = message.indexOf( "violation: " );
					if ( idx > 0 ) {
						String constraintName = message.substring( idx + "violation: ".length() );
						if ( sqle.getSQLState().equals( "23506" ) ) {
							constraintName = constraintName.substring( 1, constraintName.indexOf( ':' ) );
						}
						return constraintName;
					}
				}
				return null;
			} );

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );
			final String constraintName;

			switch (errorCode) {
				case 23505:
					// Unique constraint violation
					constraintName = getViolatedConstraintNameExtractor().extractConstraintName(sqlException);
					return new ConstraintViolationException(
							message,
							sqlException,
							sql,
							ConstraintViolationException.ConstraintKind.UNIQUE,
							constraintName
					);
				case 40001:
					// DEADLOCK DETECTED
					return new LockAcquisitionException(message, sqlException, sql);
				case 50200:
					// LOCK NOT AVAILABLE
					return new PessimisticLockException(message, sqlException, sql);
				case 90006:
					// NULL not allowed for column [90006-145]
					constraintName = getViolatedConstraintNameExtractor().extractConstraintName(sqlException);
					return new ConstraintViolationException(message, sqlException, sql, constraintName);
				case 57014:
					return new QueryTimeoutException( message, sqlException, sql );
			}

			return null;
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
		return getVersion().isBefore( 1, 4, 200 )
				? WindowFunctionSupport.NONE
				: WindowFunctionSupport.builder()
						.features( WindowFunctionSupport.Feature.values() )
						.build();
	}

	@Override
	public CteSupport getCteSupport() {
		return CteSupport.builder()
				.placement( CteSupport.Placement.TOP_LEVEL )
				.recursiveFeature(
						CteSupport.RecursiveFeature.RECURSIVE,
						getVersion().isSameOrAfter( 1, 4, 196 )
				)
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
		return getVersion().isSameOrAfter( 1, 4, 198 )
				? FetchClauseSupport.ALL
				: FetchClauseSupport.NONE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public FunctionalDependencyAnalysisSupport getFunctionalDependencyAnalysisSupport() {
		return FunctionalDependencyAnalysisSupport.TABLE_GROUP_AND_CONSTANTS;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return getVersion().isSameOrAfter( 2 ) ? H2FinalTableIdentityColumnSupport.INSTANCE : H2IdentityColumnSupport.INSTANCE;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String getQueryHintString(String query, String hints) {
		return org.hibernate.dialect.queryhint.spi.QueryHints.addUseIndexHint( query, hints );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendFormat(SqlAppender appender, String format) {
		if ( getVersion().isSame( 1, 4, 200 ) ) {
			// See https://github.com/h2database/h2database/issues/2518
			appender.appendSql( OracleDialect.datetimeFormat( format, true, true ).result() );
		}
		else {
			appender.appendSql(
					new Replacer( format, "'", "''" )
					.replace("e", "u")
					.replace( "xxx", "XXX" )
					.replace( "xx", "XX" )
					.replace( "x", "X" )
					.result()
			);
		}
	}

	@SPI({ USE, IMPLEMENT })
	public String translateExtractField(TemporalUnit unit) {
		switch ( unit ) {
			case DAY_OF_MONTH: return "day";
			case WEEK: return "iso_week";
			default: return unit.toString();
		}
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
		return EnumSupports.inline();
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
	@SPI({ IMPLEMENT, SUPPLY })
	public ObjectNullBindingStrategy getObjectNullBindingStrategy() {
		return ObjectNullBindingStrategy.SET_NULL_WITH_NULL_TYPE;
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
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
		return getVersion().isSameOrAfter( 1, 4, 197 );
	}

	@Override
	public RowValueSupport getRowValueSupport() {
		final RowValueSupport.Builder builder = RowValueSupport.builder( RowValueSupport.NONE );
		if ( getVersion().isSameOrAfter( 1, 4, 197 ) ) {
			builder.features(
					RowValueSupport.Feature.EQUALITY_COMPARISON,
					RowValueSupport.Feature.ORDERING_COMPARISON,
					RowValueSupport.Feature.IN_LIST,
					RowValueSupport.Feature.IN_SUBQUERY,
					RowValueSupport.Feature.QUANTIFIED_COMPARISON
			);
		}
		builder.feature(
				RowValueSupport.Feature.DISTINCTNESS_COMPARISON,
				getVersion().isSameOrAfter( 1, 4, 200 )
		);
		builder.feature( RowValueSupport.Feature.ROW_CONSTRUCTOR, getVersion().isSameOrAfter( 2 ) );
		return builder.build();
	}

}
