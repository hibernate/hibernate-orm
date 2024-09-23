/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.hibernate.LockOptions;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DB2GetObjectExtractor;
import org.hibernate.dialect.DB2StructJdbcType;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.dialect.aggregate.DB2AggregateSupport;
import org.hibernate.dialect.function.CastingConcatFunction;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.CountFunction;
import org.hibernate.dialect.function.DB2FormatEmulation;
import org.hibernate.dialect.function.DB2PositionFunction;
import org.hibernate.dialect.function.DB2SubstringFunction;
import org.hibernate.dialect.function.TrimFunction;
import org.hibernate.dialect.identity.DB2IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.DB2LimitHandler;
import org.hibernate.dialect.pagination.LegacyDB2LimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.sequence.DB2SequenceSupport;
import org.hibernate.dialect.sequence.LegacyDB2SequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.unique.AlterTableUniqueIndexDelegate;
import org.hibernate.dialect.unique.SkipNullableUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.mapping.Column;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.procedure.internal.DB2CallableStatementSupport;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.sqm.TemporalUnit;
import org.hibernate.query.sqm.mutation.internal.cte.CteInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.cte.CteMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorDB2DatabaseImpl;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.CharJdbcType;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;
import org.hibernate.type.descriptor.jdbc.DecimalJdbcType;
import org.hibernate.type.descriptor.jdbc.InstantJdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateJdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.LocalTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.ObjectNullResolvingJdbcType;
import org.hibernate.type.descriptor.jdbc.OffsetDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.OffsetTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.SmallIntJdbcType;
import org.hibernate.type.descriptor.jdbc.VarbinaryJdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.type.descriptor.jdbc.XmlJdbcType;
import org.hibernate.type.descriptor.jdbc.ZonedDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.internal.CapacityDependentDdlType;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
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
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsDate;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsLocalTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithMillis;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithNanos;

/**
 * A {@linkplain Dialect SQL dialect} for DB2.
 *
 * @author Gavin King
 */
public class DB2LegacyDialect extends Dialect {

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

	public DB2LegacyDialect() {
		this( DatabaseVersion.make( 9, 0 ) );
	}

	public DB2LegacyDialect(DialectResolutionInfo info) {
		super( info );
	}

	public DB2LegacyDialect(DatabaseVersion version) {
		super( version );
	}

	@Override
	protected void registerDefaultKeywords() {
		super.registerDefaultKeywords();
		//not keywords, at least not in DB2 11,
		//but perhaps they were in older versions?
		registerKeyword( "current" );
		registerKeyword( "date" );
		registerKeyword( "time" );
		registerKeyword( "timestamp" );
		registerKeyword( "fetch" );
		registerKeyword( "first" );
		registerKeyword( "rows" );
		registerKeyword( "only" );
	}

	/**
	 * DB2 LUW Version
	 */
	public DatabaseVersion getDB2Version() {
		return this.getVersion();
	}

	@Override
	public int getDefaultStatementBatchSize() {
		return 0;
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return getDB2Version().isBefore( 11 ) ? Types.SMALLINT : Types.BOOLEAN;
	}

	@Override
	protected String columnType(int sqlTypeCode) {
		switch ( sqlTypeCode ) {
			case BOOLEAN:
				// prior to DB2 11, the 'boolean' type existed,
				// but was not allowed as a column type
				return getDB2Version().isBefore( 11 ) ? "smallint" : super.columnType( sqlTypeCode );
			case TINYINT:
				// no tinyint
				return "smallint";
			case NUMERIC:
				// HHH-12827: map them both to the same type to avoid problems with schema update
				// Note that 31 is the maximum precision DB2 supports
				return columnType( DECIMAL );
			case BLOB:
				return "blob";
			case CLOB:
				return "clob";
			case TIMESTAMP_WITH_TIMEZONE:
				return "timestamp($p)";
			case TIME:
			case TIME_WITH_TIMEZONE:
				return "time";
			case BINARY:
				// should use 'binary' since version 11
				return getDB2Version().isBefore( 11 ) ? "char($l) for bit data" : super.columnType( sqlTypeCode );
			case VARBINARY:
				// should use 'varbinary' since version 11
				return getDB2Version().isBefore( 11 ) ? "varchar($l) for bit data" : super.columnType( sqlTypeCode );
			default:
				return super.columnType( sqlTypeCode );
		}
	}

	@Override
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( SQLXML, "xml", this ) );

		if ( getDB2Version().isBefore( 11 ) ) {
			// should use 'binary' since version 11
			ddlTypeRegistry.addDescriptor(
					CapacityDependentDdlType.builder( BINARY, columnType( VARBINARY ), this )
							.withTypeCapacity( 254, columnType( BINARY ) )
							.build()
			);
		}
	}

	protected UniqueDelegate createUniqueDelegate() {
		return getDB2Version().isSameOrAfter(10,5)
				//use 'create unique index ... exclude null keys'
				? new AlterTableUniqueIndexDelegate( this )
				//ignore unique keys on nullable columns in earlier versions
				: new SkipNullableUniqueDelegate( this );
	}

	@Override
	public int getMaxVarcharLength() {
		return 32_672;
	}

	@Override
	public int getDefaultDecimalPrecision() {
		//this is the maximum allowed in DB2
		return 31;
	}

	@Override
	protected boolean supportsPredicateAsExpression() {
		return getDB2Version().isSameOrAfter( 11 );
	}

	@Override
	public boolean supportsDistinctFromPredicate() {
		return getDB2Version().isSameOrAfter( 11, 1 );
	}

	@Override
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
		}
	}

	@Override
	public String[] getDropSchemaCommand(String schemaName) {
		return new String[] {"drop schema " + schemaName + " restrict"};
	}

	/**
	 * Since we're using {@code seconds_between()} and
	 * {@code add_seconds()}, it makes sense to use
	 * seconds as the "native" precision.
	 */
	@Override
	public long getFractionalSecondPrecisionInNanos() {
		//Note that DB2 actually supports all the way up to
		//thousands-of-nanoseconds precision for timestamps!
		//i.e. timestamp(12)
		return 1_000_000_000; //seconds
	}

	@Override
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		if ( getDB2Version().isBefore( 11 ) ) {
			return DB2Dialect.timestampdiffPatternV10( unit, fromTemporalType, toTemporalType );
		}
		final StringBuilder pattern = new StringBuilder();
		final String fromExpression;
		final String toExpression;
		if ( unit.isDateUnit() ) {
			fromExpression = "?2";
			toExpression = "?3";
		}
		else {
			switch ( fromTemporalType ) {
				case DATE:
					fromExpression = "cast(?2 as timestamp)";
					break;
				case TIME:
					fromExpression = "timestamp('1970-01-01',?2)";
					break;
				default:
					fromExpression = "?2";
					break;
			}
			switch ( toTemporalType ) {
				case DATE:
					toExpression = "cast(?3 as timestamp)";
					break;
				case TIME:
					toExpression = "timestamp('1970-01-01',?3)";
					break;
				default:
					toExpression = "?3";
					break;
			}
		}
		switch ( unit ) {
			case NATIVE:
			case NANOSECOND:
				pattern.append( "(seconds_between(" );
				break;
			//note: DB2 does have weeks_between()
			case MONTH:
			case QUARTER:
				// the months_between() function results
				// in a non-integral value, so trunc() it
				pattern.append( "trunc(months_between(" );
				break;
			default:
				pattern.append( "?1s_between(" );
		}
		pattern.append( toExpression );
		pattern.append( ',' );
		pattern.append( fromExpression );
		pattern.append( ')' );
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

	@Override
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
	public String getLowercaseFunction() {
		return getDB2Version().isBefore( 9, 7 ) ? "lcase" : super.getLowercaseFunction();
	}

	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public String getCreateIndexTail(boolean unique, List<Column> columns) {
		// we only create unique indexes, as opposed to unique constraints,
		// when the column is nullable, so safe to infer unique => nullable
		return unique ? " exclude null keys" : "";
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return getDB2Version().isBefore( 9, 7 )
				? LegacyDB2SequenceSupport.INSTANCE
				: DB2SequenceSupport.INSTANCE;
	}

	@Override
	public String getQuerySequencesString() {
		return "select * from syscat.sequences";
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		if ( getQuerySequencesString() == null ) {
			return SequenceInformationExtractorNoOpImpl.INSTANCE;
		}
		else {
			return SequenceInformationExtractorDB2DatabaseImpl.INSTANCE;
		}
	}

	@Override
	public String getForUpdateString() {
		return FOR_UPDATE_SQL;
	}

	@Override
	public boolean supportsSkipLocked() {
		// Introduced in 11.5: https://www.ibm.com/docs/en/db2/11.5?topic=statement-concurrent-access-resolution-clause
		return getDB2Version().isSameOrAfter( 11, 5 );
	}

	@Override
	public String getForUpdateSkipLockedString() {
		return supportsSkipLocked()
				? FOR_UPDATE_SKIP_LOCKED_SQL
				: FOR_UPDATE_SQL;
	}

	@Override
	public String getForUpdateSkipLockedString(String aliases) {
		return getForUpdateSkipLockedString();
	}

	@Override
	public String getWriteLockString(int timeout) {
		return timeout == LockOptions.SKIP_LOCKED && supportsSkipLocked()
				? FOR_UPDATE_SKIP_LOCKED_SQL
				: FOR_UPDATE_SQL;
	}

	@Override
	public String getReadLockString(int timeout) {
		return timeout == LockOptions.SKIP_LOCKED && supportsSkipLocked()
				? FOR_SHARE_SKIP_LOCKED_SQL
				: FOR_SHARE_SQL;
	}

	@Override
	public boolean supportsOuterJoinForUpdate() {
		return false;
	}

	@Override
	public boolean supportsExistsInSelect() {
		return false;
	}

	@Override
	public boolean supportsLockTimeouts() {
		//as far as I know, DB2 doesn't support this
		return false;
	}

	@Override
	public boolean requiresCastForConcatenatingNonStrings() {
		return true;
	}

	@Override
	public String getSelectClauseNullString(int sqlType, TypeConfiguration typeConfiguration) {
		return selectNullString(sqlType);
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
	public Boolean supportsRefCursors() {
		// DB2 supports the binding with Types.REF_CURSOR but doesn't support statement.getObject(position, ResultSet.class)
		return false;
	}

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		statement.registerOutParameter( col++, Types.REF_CURSOR );
		return col;
	}

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, String name) throws SQLException {
		statement.registerOutParameter( name, Types.REF_CURSOR );
		return 1;
	}

	@Override
	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
		boolean isResultSet = ps.execute();
		// This assumes you will want to ignore any update counts
		while ( !isResultSet && ps.getUpdateCount() != -1 ) {
			isResultSet = ps.getMoreResults();
		}

		return ps.getResultSet();
	}

	@Override
	public ResultSet getResultSet(CallableStatement statement, int position) throws SQLException {
		return (ResultSet) statement.getObject( position );
	}

	@Override
	public ResultSet getResultSet(CallableStatement statement, String name) throws SQLException {
		return (ResultSet) statement.getObject( name );
	}

	@Override
	public boolean supportsCommentOn() {
		return true;
	}

	@Override
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new CteMutationStrategy( rootEntityDescriptor, runtimeModelCreationContext );
	}

	@Override
	public SqmMultiTableInsertStrategy getFallbackSqmInsertStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new CteInsertStrategy( rootEntityDescriptor, runtimeModelCreationContext );
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "values current timestamp";
	}

	@Override
	public boolean doesRoundTemporalOnOverflow() {
		// DB2 does truncation
		return false;
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public boolean supportsResultSetPositionQueryMethodsOnForwardOnlyCursor() {
		return false;
	}


	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsLobValueChangePropagation() {
		return false;
	}

	@Override
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		return true;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		return false;
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );

		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();

		if ( getDB2Version().isBefore( 11 ) ) {
			jdbcTypeRegistry.addDescriptor( Types.BOOLEAN, SmallIntJdbcType.INSTANCE );
			// Binary literals were only added in 11. See https://www.ibm.com/support/knowledgecenter/SSEPGG_11.1.0/com.ibm.db2.luw.sql.ref.doc/doc/r0000731.html#d79816e393
			jdbcTypeRegistry.addDescriptor( Types.VARBINARY, VarbinaryJdbcType.INSTANCE_WITHOUT_LITERALS );
			if ( getDB2Version().isBefore( 9, 7 ) ) {
				jdbcTypeRegistry.addDescriptor( Types.NUMERIC, DecimalJdbcType.INSTANCE );
			}
		}
		// See HHH-12753
		// It seems that DB2's JDBC 4.0 support as of 9.5 does not
		// support the N-variant methods like NClob or NString.
		// Therefore here we overwrite the sql type descriptors to
		// use the non-N variants which are supported.
		jdbcTypeRegistry.addDescriptor( Types.NCHAR, CharJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( Types.NCLOB, ClobJdbcType.STREAM_BINDING );
		jdbcTypeRegistry.addDescriptor( Types.NVARCHAR, VarcharJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( Types.NUMERIC, DecimalJdbcType.INSTANCE );

		jdbcTypeRegistry.addDescriptor( XmlJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( DB2StructJdbcType.INSTANCE );

		// DB2 requires a custom binder for binding untyped nulls that resolves the type through the statement
		typeContributions.contributeJdbcType( ObjectNullResolvingJdbcType.INSTANCE );

		// Until we remove StandardBasicTypes, we have to keep this
		typeContributions.contributeType(
				new JavaObjectType(
						ObjectNullResolvingJdbcType.INSTANCE,
						typeContributions.getTypeConfiguration()
								.getJavaTypeRegistry()
								.getDescriptor( Object.class )
				)
		);

		typeContributions.contributeJdbcType( new InstantJdbcType() {
			@Override
			public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
				return new DB2GetObjectExtractor<>( javaType, this, Instant.class );
			}
		} );
		typeContributions.contributeJdbcType( new LocalDateTimeJdbcType() {
			@Override
			public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
				return new DB2GetObjectExtractor<>( javaType, this, LocalDateTime.class );
			}
		} );
		typeContributions.contributeJdbcType( new LocalDateJdbcType() {
			@Override
			public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
				return new DB2GetObjectExtractor<>( javaType, this, LocalDate.class );
			}
		} );
		typeContributions.contributeJdbcType( new LocalTimeJdbcType() {
			@Override
			public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
				return new DB2GetObjectExtractor<>( javaType, this, LocalTime.class );
			}
		} );
		typeContributions.contributeJdbcType( new OffsetDateTimeJdbcType() {
			@Override
			public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
				return new DB2GetObjectExtractor<>( javaType, this, OffsetDateTime.class );
			}
		} );
		typeContributions.contributeJdbcType( new OffsetTimeJdbcType() {
			@Override
			public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
				return new DB2GetObjectExtractor<>( javaType, this, OffsetTime.class );
			}
		} );
		typeContributions.contributeJdbcType( new ZonedDateTimeJdbcType() {
			@Override
			public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
				return new DB2GetObjectExtractor<>( javaType, this, ZonedDateTime.class );
			}
		} );
	}

	@Override
	public AggregateSupport getAggregateSupport() {
		return DB2AggregateSupport.INSTANCE;
	}

	@Override
	public CallableStatementSupport getCallableStatementSupport() {
		return DB2CallableStatementSupport.INSTANCE;
	}

	@Override
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
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	@Override
	public int getMaxIdentifierLength() {
		return 128;
	}

	@Override
	public LimitHandler getLimitHandler() {
		return limitHandler;
	}

	@Override
	public boolean supportsNullPrecedence() {
		return false;
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new DB2LegacySqlAstTranslator<>( sessionFactory, statement );
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
	public boolean supportsInsertReturning() {
		return true;
	}

	@Override
	public boolean supportsInsertReturningRowId() {
		return false;
	}

	@Override
	public boolean supportsValuesList() {
		return true;
	}

	@Override
	public boolean supportsPartitionBy() {
		return true;
	}

	@Override
	public boolean supportsNonQueryWithCTE() {
		return true;
	}

	@Override
	public boolean supportsRecursiveCTE() {
		// Supported at last since 9.7
		return getDB2Version().isSameOrAfter( 9, 7 );
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
	public boolean supportsLateral() {
		return getDB2Version().isSameOrAfter( 9, 1 );
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
		//DB2 does not need nor support FM
		appender.appendSql( OracleDialect.datetimeFormat( format, false, false ).result() );
	}

	@Override
	public String translateExtractField(TemporalUnit unit) {
		switch ( unit ) {
			//WEEK means the ISO week number on DB2
			case DAY_OF_MONTH: return "day";
			case DAY_OF_YEAR: return "doy";
			case DAY_OF_WEEK: return "dow";
			default: return super.translateExtractField( unit );
		}
	}

	@Override
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		if ( getDB2Version().isBefore( 11 ) ) {
			appender.appendSql( bool ? '1' : '0' );
		}
		else {
			appender.appendSql( bool );
		}
	}

	@Override
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
		return super.extractPattern( unit );
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

	@Override
	public int getInExpressionCountLimit() {
		return BIND_PARAMETERS_NUMBER_LIMIT;
	}

	@Override
	public String generatedAs(String generatedAs) {
		return " generated always as (" + generatedAs + ")";
	}

	@Override
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuilder builder, DatabaseMetaData dbMetaData)
			throws SQLException {
		builder.setAutoQuoteInitialUnderscore(true);
		return super.buildIdentifierHelper(builder, dbMetaData);
	}

	@Override
	public boolean canDisableConstraints() {
		return true;
	}

	@Override
	public String getDisableConstraintStatement(String tableName, String name) {
		return "alter table " + tableName + " alter foreign key " + name + " not enforced";
	}

	@Override
	public String getEnableConstraintStatement(String tableName, String name) {
		return "alter table " + tableName + " alter foreign key " + name + " enforced";
	}

	@Override
	public String getTruncateTableStatement(String tableName) {
		return super.getTruncateTableStatement(tableName) + " immediate";
	}

	@Override
	public String getCreateUserDefinedTypeExtensionsString() {
		return " instantiable mode db2sql";
	}

	/**
	 * The more "standard" syntax is {@code rid_bit(alias)} but here we use {@code alias.rowid}.
	 * <p>
	 * There is also an alternative {@code rid()} of type {@code bigint}, but it cannot be used
	 * with partitioning.
	 */
	@Override
	public String rowId(String rowId) {
		return "rowid";
	}

	@Override
	public int rowIdSqlType() {
		return VARBINARY;
	}

	@Override
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}

	@Override
	public boolean supportsFromClauseInUpdate() {
		return getDB2Version().isSameOrAfter( 11 );
	}

	@Override
	public String getDual() {
		return "sysibm.dual";
	}

	@Override
	public String getFromDualForSelectOnly() {
		return " from " + getDual();
	}
}
