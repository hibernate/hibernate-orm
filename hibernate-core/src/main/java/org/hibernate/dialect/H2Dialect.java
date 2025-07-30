/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.QueryTimeoutException;
import org.hibernate.Timeouts;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.dialect.aggregate.H2AggregateSupport;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.identity.H2FinalTableIdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.lock.internal.H2LockingSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.OffsetFetchLimitHandler;
import org.hibernate.dialect.sequence.H2V1SequenceSupport;
import org.hibernate.dialect.sequence.H2V2SequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.sql.ast.H2SqlAstTranslator;
import org.hibernate.dialect.temptable.H2GlobalTemporaryTableStrategy;
import org.hibernate.dialect.temptable.StandardLocalTemporaryTableStrategy;
import org.hibernate.dialect.temptable.TemporaryTableKind;
import org.hibernate.dialect.temptable.TemporaryTableStrategy;
import org.hibernate.dialect.type.H2DurationIntervalSecondJdbcType;
import org.hibernate.dialect.type.H2JsonArrayJdbcTypeConstructor;
import org.hibernate.dialect.type.H2JsonJdbcType;
import org.hibernate.dialect.unique.CreateTableUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.JdbcParameterMetadata;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.internal.OptionalTableUpdate;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorLegacyImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.descriptor.jdbc.EnumJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.OrdinalEnumJdbcType;
import org.hibernate.type.descriptor.jdbc.TimeUtcAsOffsetTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsInstantJdbcType;
import org.hibernate.type.descriptor.jdbc.UUIDJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.internal.NativeEnumDdlTypeImpl;
import org.hibernate.type.descriptor.sql.internal.NativeOrdinalEnumDdlTypeImpl;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.internal.util.JdbcExceptionHelper.extractErrorCode;
import static org.hibernate.internal.util.StringHelper.split;
import static org.hibernate.query.common.TemporalUnit.SECOND;
import static org.hibernate.type.SqlTypes.BIGINT;
import static org.hibernate.type.SqlTypes.BINARY;
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
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsDate;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsLocalTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithMillis;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithNanos;

/**
 * A {@linkplain Dialect SQL dialect} for H2.
 * <p>
 * Please refer to the
 * <a href="http://www.h2database.com/html/main.html">H2 documentation</a>.
 *
 *
 * @author Thomas Mueller
 * @author Jürgen Kreitler
 */
public class H2Dialect extends Dialect {
	private static final DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 2, 1, 214 );

	private final boolean ansiSequence;
	private final boolean cascadeConstraints;
	private final boolean useLocalTime;

	private final SequenceInformationExtractor sequenceInformationExtractor;
	private final String querySequenceString;
	private final UniqueDelegate uniqueDelegate = new CreateTableUniqueDelegate(this);

	public H2Dialect(DialectResolutionInfo info) {
		this( staticDetermineDatabaseVersion( info ) );
		registerKeywords( info );
	}

	public H2Dialect() {
		this( MINIMUM_VERSION );
	}

	public H2Dialect(DatabaseVersion version) {
		super( version );

		// Prior to 1.4.200 there was no support for 'current value for sequence_name'
		// After 2.0.202 there is no support for 'sequence_name.nextval' and 'sequence_name.currval'
		ansiSequence = true;

		// Prior to 1.4.200 the 'cascade' in 'drop table' was implicit
		cascadeConstraints = true;
		// 1.4.200 introduced changes in current_time and current_timestamp
		useLocalTime = true;

		sequenceInformationExtractor = SequenceInformationExtractorLegacyImpl.INSTANCE;
		querySequenceString = "select * from INFORMATION_SCHEMA.SEQUENCES";
	}

	@Override
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
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_VERSION;
	}

	@Override
	public boolean getDefaultNonContextualLobCreation() {
		// http://code.google.com/p/h2database/issues/detail?id=235
		return true;
	}

	@Override
	public boolean supportsStandardArrays() {
		return true;
	}

	@Override
	public boolean useArrayForMultiValuedParameters() {
		// Performance is worse than the in-predicate version
		return false;
	}

	@Override
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
	protected String castType(int sqlTypeCode) {
		return switch (sqlTypeCode) {
			case CHAR, NCHAR -> "char";
			case VARCHAR, NVARCHAR, LONG32VARCHAR, LONG32NVARCHAR -> "varchar";
			case BINARY, VARBINARY, LONG32VARBINARY -> "varbinary";
			default -> super.castType( sqlTypeCode );
		};
	}

	@Override
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( UUID, "uuid", this ) );
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( GEOMETRY, "geometry", this ) );
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( INTERVAL_SECOND, "interval second($p,$s)", this ) );
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( JSON, "json", this ) );
		ddlTypeRegistry.addDescriptor( new NativeEnumDdlTypeImpl( this ) );
		ddlTypeRegistry.addDescriptor( new NativeOrdinalEnumDdlTypeImpl( this ) );
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );
		final JdbcTypeRegistry jdbcTypeRegistry =
				typeContributions.getTypeConfiguration()
						.getJdbcTypeRegistry();
		jdbcTypeRegistry.addDescriptor( TimeUtcAsOffsetTimeJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( TimestampUtcAsInstantJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptorIfAbsent( UUIDJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptorIfAbsent( H2DurationIntervalSecondJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptorIfAbsent( H2JsonJdbcType.INSTANCE );
		// Replace the standard array constructor
		jdbcTypeRegistry.addTypeConstructor( H2JsonArrayJdbcTypeConstructor.INSTANCE );
		jdbcTypeRegistry.addDescriptor( EnumJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( OrdinalEnumJdbcType.INSTANCE );
	}

	@Override
	public AggregateSupport getAggregateSupport() {
		return H2AggregateSupport.valueOf( this );
	}

	@Override
	public int getDefaultStatementBatchSize() {
		return 15;
	}

	public boolean hasOddDstBehavior() {
		// H2 1.4.200 has a bug: https://github.com/h2database/h2database/issues/3184
		return true;
	}

	@Override
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
		functionFactory.arrayFill_h2();
		functionFactory.arrayToString_h2( getMaximumArraySize() );

		functionFactory.jsonObject();
		functionFactory.jsonArray();
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
	public void augmentPhysicalTableTypes(List<String> tableTypesList) {
		tableTypesList.add( "BASE TABLE" );
	}

	@Override
	protected Integer resolveSqlTypeCode(String columnTypeName, TypeConfiguration typeConfiguration) {
		return switch (columnTypeName) {
			// Use REAL instead of FLOAT to get Float as recommended Java type
			case "FLOAT(24)" -> Types.REAL;
			default -> super.resolveSqlTypeCode( columnTypeName, typeConfiguration );
		};
	}

	@Override
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
	protected Integer resolveSqlTypeCode(String typeName, String baseTypeName, TypeConfiguration typeConfiguration) {
		return switch (baseTypeName) {
			case "CHARACTER VARYING" -> VARCHAR;
			default -> super.resolveSqlTypeCode( typeName, baseTypeName, typeConfiguration );
		};
	}

	@Override
	public int getMaxVarcharLength() {
		return 1_048_576;
	}

	@Override
	public String currentTime() {
		return useLocalTime ? "localtime" : super.currentTime();
	}

	@Override
	public String currentTimestamp() {
		return useLocalTime ? "localtimestamp" : super.currentTimestamp();
	}

	@Override
	public String currentTimestampWithTimeZone() {
		return "current_timestamp";
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement, @Nullable JdbcParameterMetadata parameterInfo) {
				return new H2SqlAstTranslator<>( sessionFactory, statement, parameterInfo );
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
	public String extractPattern(TemporalUnit unit) {
		return unit == SECOND
				? "(" + super.extractPattern(unit) + "+extract(nanosecond from ?2)/1e9)"
				: super.extractPattern(unit);
	}

	@Override
	public String castPattern(CastType from, CastType to) {
		if ( from == CastType.STRING && to == CastType.BOOLEAN ) {
			return "cast(?1 as ?2)";
		}
		else {
			return super.castPattern( from, to );
		}
	}

	@Override @SuppressWarnings("deprecation")
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
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		if ( unit == null ) {
			return "(?3-?2)";
		}
		return "datediff(?1,?2,?3)";
	}

	@Override
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
					appendAsTime( appender, temporalAccessor, supportsTemporalLiteralOffset(), jdbcTimeZone );
				}
				else {
					appender.appendSql( "time '" );
					appendAsLocalTime( appender, temporalAccessor );
				}
				appender.appendSql( '\'' );
				break;
			case TIMESTAMP:
				if ( supportsTemporalLiteralOffset() && temporalAccessor.isSupported( ChronoField.OFFSET_SECONDS ) ) {
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
	public boolean supportsTemporalLiteralOffset() {
		return true;
	}

	@Override
	public TimeZoneSupport getTimeZoneSupport() {
		return TimeZoneSupport.NATIVE;
	}

	@Override
	public boolean supportsIsTrue() {
		return true;
	}

	@Override
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		appender.appendSql( bool );
	}

	@Override
	public LimitHandler getLimitHandler() {
		return OffsetFetchLimitHandler.INSTANCE;
	}

	@Override
	public LockingSupport getLockingSupport() {
		return H2LockingSupport.H2_LOCKING_SUPPORT;
	}

	@Override
	public String getForUpdateNowaitString() {
		return getForUpdateString() + " nowait";
	}

	@Override
	public String getForUpdateSkipLockedString() {
		return getForUpdateString() + " skip locked";
	}

	@Override
	public String getForUpdateString(Timeout timeout) {
		return withRealTimeout( getForUpdateString(), timeout );
	}

	private String withRealTimeout(String lockString, Timeout timeout) {
		assert Timeouts.isRealTimeout( timeout );
		return lockString + " wait " + Timeouts.getTimeoutInSeconds( timeout );
	}

	private String withRealTimeout(String lockString, int millis) {
		assert Timeouts.isRealTimeout( millis );
		return lockString + " wait " + Timeouts.getTimeoutInSeconds( millis );
	}

	@Override
	public boolean supportsDistinctFromPredicate() {
		return true;
	}

	@Override
	public boolean supportsIfExistsAfterTableName() {
		return !supportsIfExistsBeforeTableName();
	}

	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return cascadeConstraints;
	}

	@Override
	public boolean supportsIfExistsAfterAlterTable() {
		return cascadeConstraints;
	}

	@Override
	public boolean supportsIfExistsBeforeConstraintName() {
		return true;
	}

	@Override
	public String getCascadeConstraintsString() {
		return cascadeConstraints ? " cascade "
				: super.getCascadeConstraintsString();
	}

	@Override
	public boolean supportsAlterColumnType() {
		return true;
	}

	@Override
	public String getAlterColumnTypeString(String columnName, String columnType, String columnDefinition) {
		return "alter column " + columnName + " set data type " + columnType;
		// if only altering the type, no need to specify the whole definition
//		return "alter column " + columnName + " " + columnDefinition;
	}

	@Override
	public boolean supportsCommentOn() {
		return true;
	}

	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return ansiSequence ? H2V2SequenceSupport.INSTANCE: H2V1SequenceSupport.INSTANCE;
	}

	@Override
	public String getQuerySequencesString() {
		return querySequenceString;
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return sequenceInformationExtractor;
	}

	@Override
	public NullOrdering getNullOrdering() {
		return NullOrdering.SMALLEST;
	}

	@Override
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType entityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new GlobalTemporaryTableMutationStrategy( entityDescriptor, runtimeModelCreationContext );
	}

	@Override
	public SqmMultiTableInsertStrategy getFallbackSqmInsertStrategy(
			EntityMappingType entityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new GlobalTemporaryTableInsertStrategy( entityDescriptor, runtimeModelCreationContext );
	}

	@Override
	public String getTemporaryTableCreateOptions() {
		return H2GlobalTemporaryTableStrategy.INSTANCE.getTemporaryTableCreateOptions();
	}

	@Override
	public TemporaryTableKind getSupportedTemporaryTableKind() {
		return TemporaryTableKind.GLOBAL;
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
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "call current_timestamp()";
	}


	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsLobValueChangePropagation() {
		return false;
	}

	@Override
	public boolean supportsTupleCounts() {
		return true;
	}

	@Override
	public boolean requiresParensForTupleDistinctCounts() {
		return true;
	}

	@Override
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		// see http://groups.google.com/group/h2-database/browse_thread/thread/562d8a49e2dabe99?hl=en
		return true;
	}

	@Override
	public SelectItemReferenceStrategy getGroupBySelectItemReferenceStrategy() {
		return SelectItemReferenceStrategy.ALIAS;
	}

	@Override
	public boolean supportsOffsetInSubquery() {
		return true;
	}

	@Override
	public boolean supportsWindowFunctions() {
		return true;
	}

	@Override
	public boolean supportsRecursiveCTE() {
		return true;
	}

	@Override
	public boolean supportsFetchClause(FetchClauseType type) {
		return true;
	}

	@Override
	public FunctionalDependencyAnalysisSupport getFunctionalDependencyAnalysisSupport() {
		return FunctionalDependencyAnalysisSupportImpl.TABLE_GROUP_AND_CONSTANTS;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return H2FinalTableIdentityColumnSupport.INSTANCE;
	}

	/**
	 * @return {@code true} because we can use {@code select ... from final table (insert .... )}
	 */
	@Override
	public boolean supportsInsertReturning() {
		return true;
	}

	@Override
	public boolean supportsInsertReturningRowId() {
		return false;
	}

	@Override
	public boolean supportsInsertReturningGeneratedKeys() {
		return true;
	}

	@Override
	public boolean unquoteGetGeneratedKeys() {
		return true;
	}

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int position) throws SQLException {
		return position;
	}

	@Override
	public String getQueryHintString(String query, String hints) {
		return addUseIndexQueryHint( query, hints );
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
		appender.appendSql(
				new Replacer( format, "'", "''" )
				.replace("e", "u")
				.replace( "xxx", "XXX" )
				.replace( "xx", "XX" )
				.replace( "x", "X" )
				.result()
		);
	}

	public String translateExtractField(TemporalUnit unit) {
		return switch (unit) {
			case DAY_OF_MONTH -> "day";
			case WEEK -> "iso_week";
			default -> unit.toString();
		};
	}

	@Override
	public String generatedAs(String generatedAs) {
		return " generated always as (" + generatedAs + ")";
	}

	@Override
	public boolean canDisableConstraints() {
		return true;
	}

	@Override
	public String getEnableConstraintsStatement() {
		return "set referential_integrity true";
	}

	@Override
	public String getEnumTypeDeclaration(String name, String[] values) {
		StringBuilder type = new StringBuilder();
		type.append( "enum (" );
		String separator = "";
		for ( String value : values ) {
			type.append( separator ).append('\'').append( value ).append('\'');
			separator = ",";
		}
		return type.append( ')' ).toString();
	}

	@Override
	public String getDisableConstraintsStatement() {
		return "set referential_integrity false";
	}

	@Override
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	@Override
	public String rowId(String rowId) {
		return "_rowid_";
	}

	@Override
	public int rowIdSqlType() {
		return BIGINT;
	}

	@Override
	public MutationOperation createOptionalTableUpdateOperation(
			EntityMutationTarget mutationTarget,
			OptionalTableUpdate optionalTableUpdate,
			SessionFactoryImplementor factory) {
		return new H2SqlAstTranslator<>( factory, optionalTableUpdate )
				.createMergeOperation( optionalTableUpdate );
	}

//	private static MutationOperation withoutMerge(
//			EntityMutationTarget mutationTarget,
//			OptionalTableUpdate optionalTableUpdate,
//			SessionFactoryImplementor factory) {
//		return new OptionalTableUpdateOperation( mutationTarget, optionalTableUpdate, factory );
//	}

	@Override
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
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}

	@Override
	public String getCaseInsensitiveLike() {
		return "ilike";
	}

	@Override
	public boolean supportsCaseInsensitiveLike(){
		return true;
	}

	@Override
	public boolean supportsPartitionBy() {
		return true;
	}

	@Override
	public boolean supportsBindingNullSqlTypeForSetNull() {
		return true;
	}

	@Override
	public boolean supportsValuesList() {
		return true;
	}

	@Override
	public String getDual() {
		return "dual";
	}

	@Override
	public boolean supportsFilterClause() {
		// Introduction of FILTER clause https://github.com/h2database/h2database/commit/9e6dbf3baa57000f670826ede431dc7fb4cd9d9c
		return true;
	}

	@Override
	public boolean supportsRowConstructor() {
		return true;
	}

	@Override
	public boolean supportsArrayConstructor() {
		return true;
	}

	@Override
	public boolean supportsJoinInMutationStatementSubquery() {
		return false;
	}

	@Override
	public boolean supportsRowValueConstructorSyntax() {
		// Just a guess
		return true;
	}

	@Override
	public boolean supportsRowValueConstructorDistinctFromSyntax() {
		return true;
	}

	@Override
	public boolean supportsWithClauseInSubquery() {
		return false;
	}

	@Override
	public boolean supportsRowValueConstructorSyntaxInQuantifiedPredicates() {
		// Just a guess
		return true;
	}

}
