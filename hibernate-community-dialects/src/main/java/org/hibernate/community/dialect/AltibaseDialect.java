/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import org.hibernate.Timeouts;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.community.dialect.pagination.AltibaseLimitHandler;
import org.hibernate.community.dialect.sequence.AltibaseSequenceSupport;
import org.hibernate.community.dialect.sequence.SequenceInformationExtractorAltibaseDatabaseImpl;
import org.hibernate.dialect.BooleanDecoder;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.NationalizationSupport;
import org.hibernate.dialect.NullOrdering;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.OracleTruncFunction;
import org.hibernate.dialect.function.TrimFunction;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.internal.LockingSupportParameterized;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.type.IntervalType;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.TrimSpec;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JsonJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.internal.CapacityDependentDdlType;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BIT;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.DOUBLE;
import static org.hibernate.type.SqlTypes.FLOAT;
import static org.hibernate.type.SqlTypes.LONGVARBINARY;
import static org.hibernate.type.SqlTypes.LONGVARCHAR;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.descriptor.DateTimeUtils.JDBC_ESCAPE_END;
import static org.hibernate.type.descriptor.DateTimeUtils.JDBC_ESCAPE_START_TIMESTAMP;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithMicros;

/**
 * An SQL dialect for Altibase 7.1 and above.
 *
 * @author Geoffrey Park
 */
public class AltibaseDialect extends Dialect {

	private static final DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 7, 1 );
	// Altibase's IN-list limit is around 1,020 elements; use 1,000 to stay below
	// that limit and avoid "Calculation stack overflow" during prepare.
	private static final int IN_LIST_SIZE_LIMIT = 1000;
	private static final Pattern CHECK_CONSTRAINT_NAME_PATTERN = Pattern.compile( "Check constraint (.+) violated" );
	private static final Pattern FOR_CONSTRAINT_NAME_PATTERN = Pattern.compile( ".*\\sfor\\s+([^.]+)\\.?" );
	private static final Pattern COLON_CONSTRAINT_NAME_PATTERN = Pattern.compile( ".*:\\s*(.+)" );
	private static final LockingSupport LOCKING_SUPPORT = new LockingSupportParameterized(
			PessimisticLockStyle.CLAUSE,
			RowLockStrategy.NONE,
			true,
			true,
			false,
			OuterJoinLockingType.UNSUPPORTED
	);

	@SuppressWarnings("unused")
	public AltibaseDialect() {
		this( MINIMUM_VERSION );
	}

	public AltibaseDialect(DialectResolutionInfo info) {
		this( info.makeCopyOrDefault( MINIMUM_VERSION ) );
		registerKeywords( info );
	}

	public AltibaseDialect(DatabaseVersion version) {
		super( version );
	}

	@Override
	protected String columnType(int sqlTypeCode) {
		return switch ( sqlTypeCode ) {
			case BOOLEAN -> "char(1)";
			case FLOAT, DOUBLE -> "double";
			case TINYINT -> "smallint";
			case TIME, TIMESTAMP, TIME_WITH_TIMEZONE, TIMESTAMP_WITH_TIMEZONE -> "date";
			case BINARY -> "byte($l)";
			case VARBINARY -> "varbyte($l)";
			case LONGVARBINARY -> "blob";
			case BIT -> "varbit($l)";
			case LONGVARCHAR, NCLOB -> "clob";
			default -> super.columnType( sqlTypeCode );
		};
	}

	@Override
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		ddlTypeRegistry.addDescriptor(
				CapacityDependentDdlType.builder( BINARY, columnType( LONGVARBINARY ), this )
						.withTypeCapacity( getMaxVarbinaryLength(), columnType( BINARY ) )
						.build()
		);
		ddlTypeRegistry.addDescriptor(
				CapacityDependentDdlType.builder( BIT, columnType( LONGVARBINARY ), this )
						.withTypeCapacity( 64000, columnType( BIT ) )
						.build()
		);
		if ( supportsNativeJsonType() ) {
			ddlTypeRegistry.addDescriptor( new DdlTypeImpl( JSON, "json", this ) );
		}
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );
		if ( supportsNativeJsonType() ) {
			typeContributions.contributeJdbcType( JsonJdbcType.INSTANCE );
		}
	}

	private boolean supportsNativeJsonType() {
		return getVersion().isSameOrAfter( 8, 1 );
	}

	@Override
	public JdbcType resolveSqlTypeDescriptor(
			String columnTypeName,
			int jdbcTypeCode,
			int precision,
			int scale,
			JdbcTypeRegistry jdbcTypeRegistry) {
		if ( supportsNativeJsonType() && "json".equalsIgnoreCase( columnTypeName ) ) {
			return jdbcTypeRegistry.getDescriptor( JSON );
		}
		return super.resolveSqlTypeDescriptor( columnTypeName, jdbcTypeCode, precision, scale, jdbcTypeRegistry );
	}

	@Override
	public int getMaxVarcharLength() {
		return 32_000;
	}

	@Override
	public int getMaxVarbinaryLength() {
		return 32_000;
	}

	@Override
	public int getDefaultStatementBatchSize() {
		return 15;
	}

	@Override
	public String trimPattern(TrimSpec specification, boolean isWhitespace) {
		return switch ( specification ) {
			case BOTH -> isWhitespace ? "trim(?1)" : "trim(?1,?2)";
			case LEADING -> isWhitespace ? "ltrim(?1)" : "ltrim(?1,?2)";
			case TRAILING -> isWhitespace ? "rtrim(?1)": "rtrim(?1,?2)";
		};
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return Types.BIT;
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);
		final TypeConfiguration typeConfiguration = functionContributions.getTypeConfiguration();

		functionContributions.getFunctionRegistry().registerBinaryTernaryPattern(
				"locate",
				typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ),
				"instr(?2,?1)",
				"instr(?2,?1,?3)",
				FunctionParameterType.STRING, FunctionParameterType.STRING, FunctionParameterType.INTEGER,
				typeConfiguration
		).setArgumentListSignature("(pattern, string[, start])");

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);
		functionFactory.ceiling_ceil();
		functionFactory.trim2();
		functionContributions.getFunctionRegistry().register(
				"trim",
				new TrimFunction( this, typeConfiguration, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER )
		);
		functionFactory.stddev();
		functionFactory.variance();
		functionFactory.char_chr();
		functionFactory.concat_pipeOperator( SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		functionFactory.coalesce( SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		functionFactory.length_characterLength( SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		functionFactory.nullif( SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		functionFactory.power( SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		functionFactory.initcap();
		functionFactory.repeat_rpad( SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );

		functionFactory.radians_acos( SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		functionFactory.degrees_acos( SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		functionFactory.trigonometry( SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		functionFactory.hex( "to_char(?1)", SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );

		functionFactory.ascii();
		functionFactory.toCharNumberDateTimestamp();
		functionFactory.lastDay();
		functionFactory.sysdate();
		functionFactory.rownum();
		functionFactory.instr();
		functionFactory.substr();
		functionFactory.cosh();
		functionFactory.sinh();
		functionFactory.tanh();
		functionFactory.log( SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		functionFactory.log10_log();
		functionFactory.leftRight_substr( SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		functionFactory.translate();
		functionFactory.addMonths();
		functionFactory.listagg( null );
		functionFactory.monthsBetween();
		functionFactory.windowFunctions();
		functionFactory.hypotheticalOrderedSetAggregates();
		functionFactory.bitLength_pattern( "bit_length(?1)", "lengthb(?1)*8" );
		functionFactory.octetLength_pattern( "octet_length(?1)", "lengthb(?1)" );
		functionContributions.getFunctionRegistry().register( "trunc", new OracleTruncFunction() );
		functionContributions.getFunctionRegistry().registerAlternateKey( "truncate", "trunc" );

		// Use `numor`, `numand`, and `numxor` because bitwise operators work only in binary columns in Altibase.
		functionContributions.getFunctionRegistry().patternDescriptorBuilder( "bitand", "numand(?1,?2)" )
							.setExactArgumentCount( 2 )
							.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
							.register();
		functionContributions.getFunctionRegistry().patternDescriptorBuilder( "bitor", "numor(?1,?2)" )
							.setExactArgumentCount( 2 )
							.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
							.register();
		functionContributions.getFunctionRegistry().patternDescriptorBuilder( "bitxor", "numxor(?1,?2)" )
							.setExactArgumentCount( 2 )
							.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
							.register();
		functionFactory.regexpLike_predicateFunction();
		functionFactory.pad( SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		functionFactory.replace( SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		functionFactory.substring_substr( SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
	}

	@Override
	public String currentDate() {
		return currentTimestamp();
	}

	@Override
	public String currentTime() {
		return currentTimestamp();
	}

	@Override
	public String currentTimestamp() {
		return "sysdate";
	}

	@Override
	public String currentLocalTime() {
		return currentLocalTimestamp();
	}

	@Override
	public String currentLocalTimestamp() {
		// Drop microseconds, because sysdate comes with microseconds.
		return "trunc(sysdate,'second')";
	}

	@Override
	public String currentTimestampWithTimeZone() {
		return currentTimestamp();
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new AltibaseSqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	/**
	 * In Altibase, `timestampadd` and `datediff` with microseconds have limitations,
	 * so use seconds as the native precision.
	 */
	@Override
	public long getFractionalSecondPrecisionInNanos() {
		return 1_000_000_000; //seconds
	}

	/**
	 * Altibase supports a limited list of temporal fields in the
	 * extract() function, but we can emulate some of them by
	 * using to_char() with a format string instead of extract().
	 * Thus, the additional supported fields are
	 * {@link TemporalUnit#DAY_OF_YEAR},
	 * {@link TemporalUnit#DAY_OF_MONTH},
	 * {@link TemporalUnit#DAY_OF_YEAR},
	 * and {@link TemporalUnit#WEEK}.
	 */
	@Override
	public String extractPattern(TemporalUnit unit) {
		return switch (unit) {
			case DAY_OF_WEEK -> "extract(?2, 'DAYOFWEEK')";
			case DAY_OF_MONTH -> "extract(?2, 'DAY')";
			case DAY_OF_YEAR -> "extract(?2,'DAYOFYEAR')";
			case WEEK -> "to_number(to_char(?2,'IW'))"; //the ISO week number
			case WEEK_OF_YEAR -> "extract(?2, 'WEEK')";
			case EPOCH -> timestampdiffPattern( TemporalUnit.SECOND, TemporalType.TIMESTAMP, TemporalType.TIMESTAMP )
						.replace( "?2", "TO_DATE('1970-01-01 00:00:00','YYYY-MM-DD HH24:MI:SS')" )
						.replace( "?3", "?2" );
			case QUARTER -> "extract(?2, 'QUARTER')";
			default ->  super.extractPattern( unit );
		};
	}

	@Override
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		return switch (unit) {
			case NANOSECOND -> "timestampadd(MICROSECOND,(?2)/1e3,?3)";
			case NATIVE -> "timestampadd(SECOND, ?2, ?3)";
			default ->  "timestampadd(?1, ?2, ?3)";
		};
	}

	@Override
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		return switch (unit) {
			case SECOND, NATIVE -> "datediff(?2, ?3, 'SECOND')";
			case NANOSECOND -> "datediff(?2, ?3, 'MICROSECOND')*1e3";
			default -> "datediff(?2, ?3, '?1')";
		};
	}

	@Override
	public void appendBinaryLiteral(SqlAppender appender, byte[] bytes) {
		appender.appendSql( "VARBYTE'" );
		PrimitiveByteArrayJavaType.INSTANCE.appendString( appender, bytes );
		appender.appendSql( '\'' );
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
		appender.appendSql( OracleDialect.datetimeFormat( format, false, false ).result() );
	}

	@Override
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
					case OFFSET_TIMESTAMP:
					case ZONE_TIMESTAMP:
						return "to_char(?1,'YYYY-MM-DD HH24:MI:SS.FF6')";
				}
				break;
			case CLOB:
				// Altibase doesn't support cast to clob
				return "cast(?1 as varchar(32000))";
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
			case OFFSET_TIMESTAMP:
			case ZONE_TIMESTAMP:
				if ( from == CastType.STRING ) {
					return "to_date(?1,'YYYY-MM-DD HH24:MI:SS.FF6')";
				}
				break;
		}
		return super.castPattern(from, to);
	}

	@Override
	public void appendDateTimeLiteral(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		if (precision == TemporalType.TIMESTAMP) {
			appender.appendSql(JDBC_ESCAPE_START_TIMESTAMP);
			appendAsTimestampWithMicros(appender, temporalAccessor, supportsTemporalLiteralOffset(), jdbcTimeZone);
			appender.appendSql(JDBC_ESCAPE_END);
			return;
		}
		super.appendDateTimeLiteral(appender, temporalAccessor, precision, jdbcTimeZone);
	}

	@Override
	public void appendDateTimeLiteral(SqlAppender appender, Date date, TemporalType precision, TimeZone jdbcTimeZone) {
		if (precision == TemporalType.TIMESTAMP) {
			appender.appendSql(JDBC_ESCAPE_START_TIMESTAMP);
			appendAsTimestampWithMicros( appender, date, jdbcTimeZone );
			appender.appendSql(JDBC_ESCAPE_END);
			return;
		}
		super.appendDateTimeLiteral(appender, date, precision, jdbcTimeZone);
	}

	@Override
	public String translateDurationField(TemporalUnit unit) {
		//use microsecond as the "native" precision
		if ( unit == TemporalUnit.NATIVE ) {
			return "microsecond";
		}
		return super.translateDurationField( unit );
	}

	@Override
	public NullOrdering getNullOrdering() {
		return NullOrdering.LAST;
	}

	@Override
	public String getAddColumnString() {
		return "add column (";
	}

	@Override
	public String getAddColumnSuffixString() {
		return ")";
	}

	@Override
	public int getMaxIdentifierLength() {
		return 40;
	}

	@Override
	public IdentifierHelper buildIdentifierHelper(
			IdentifierHelperBuilder builder,
			DatabaseMetaData metadata) throws SQLException {
		// Any use of keywords as identifiers will result in syntax error, so enable auto quote always
		builder.setAutoQuoteKeywords( true );
		builder.setAutoQuoteInitialUnderscore( false );
		builder.applyReservedWords( metadata );

		return super.buildIdentifierHelper( builder, metadata );
	}

	@Override
	public boolean canCreateSchema() {
		return false;
	}

	@Override
	public NameQualifierSupport getNameQualifierSupport() {
		return NameQualifierSupport.SCHEMA;
	}

	@Override
	public String[] getCreateSchemaCommand(String schemaName) {
		throw new UnsupportedOperationException( "No create schema syntax supported by " + getClass().getName() );
	}

	@Override
	public String[] getDropSchemaCommand(String schemaName) {
		throw new UnsupportedOperationException( "No drop schema syntax supported by " + getClass().getName() );
	}

	@Override
	public boolean qualifyIndexName() {
		return false;
	}

	@Override
	public boolean supportsTruncateWithCast(){
		return false;
	}

	@Override
	public boolean supportsCommentOn() {
		return true;
	}

	@Override
	public boolean supportsUnboundedLobLocatorMaterialization() {
		return false;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		return false;
	}

	@Override
	public boolean supportsExistsInSelect() {
		return false;
	}

	@Override
	public boolean supportsPartitionBy() {
		return true;
	}

	@Override
	public boolean supportsFromClauseInUpdate() {
		return true;
	}

	@Override
	public LockingSupport getLockingSupport() {
		return LOCKING_SUPPORT;
	}

	@Override
	public String getForUpdateNowaitString() {
		return getForUpdateString() + " nowait";
	}

	@Override
	public String getForUpdateNowaitString(String aliases) {
		return getForUpdateString( aliases ) + " nowait";
	}

	@Override
	public String getForUpdateString(Timeout timeout) {
		return withLockTimeout( getForUpdateString(), timeout );
	}

	@Override
	public String getWriteLockString(Timeout timeout) {
		return withLockTimeout( getForUpdateString(), timeout );
	}

	@Override
	public String getWriteLockString(String aliases, Timeout timeout) {
		return withLockTimeout( getForUpdateString( aliases ), timeout );
	}

	@Override
	public String getReadLockString(Timeout timeout) {
		return getWriteLockString( timeout );
	}

	@Override
	public String getReadLockString(String aliases, Timeout timeout) {
		return getWriteLockString( aliases, timeout );
	}

	private String withLockTimeout(String lockString, Timeout timeout) {
		return switch ( timeout.milliseconds() ) {
			case Timeouts.NO_WAIT_MILLI -> LOCKING_SUPPORT.getMetadata().getLockTimeoutType( timeout ) == LockTimeoutType.QUERY
					? lockString + " nowait" : lockString;
			case Timeouts.SKIP_LOCKED_MILLI, Timeouts.WAIT_FOREVER_MILLI -> lockString;
			default -> LOCKING_SUPPORT.getMetadata().getLockTimeoutType( timeout ) == LockTimeoutType.QUERY
					? lockString + " wait " + Timeouts.getTimeoutInSeconds( timeout ) : lockString;
		};
	}

	@Override
	public boolean supportsCrossJoin() {
		return false;
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return AltibaseSequenceSupport.INSTANCE;
	}

	@Override
	public String getQuerySequencesString() {
		return "SELECT a.user_name USER_NAME, b.table_name SEQUENCE_NAME, c.current_seq CURRENT_VALUE, "
				+ "c.start_seq START_VALUE, c.min_seq MIN_VALUE, c.max_seq MAX_VALUE, c.increment_seq INCREMENT_BY, "
				+ "c.flag CYCLE_, c.sync_interval CACHE_SIZE "
				+ "FROM system_.sys_users_ a, system_.sys_tables_ b, x$seq c "
				+ "WHERE a.user_id = b.user_id AND b.table_oid = c.seq_oid AND a.user_name <> 'SYSTEM_' AND b.table_type = 'S' "
				+ "ORDER BY 1,2";
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceInformationExtractorAltibaseDatabaseImpl.INSTANCE;
	}

	@Override
	public LimitHandler getLimitHandler() {
		return AltibaseLimitHandler.INSTANCE;
	}

	@Override
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "select sysdate from dual";
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public String getCascadeConstraintsString() {
		return " cascade constraints";
	}

	@Override
	public boolean supportsOrderByInSubquery() {
		return false;
	}

	@Override
	public int getInExpressionCountLimit() {
		return IN_LIST_SIZE_LIMIT;
	}

	@Override
	public boolean supportsWindowFunctions() {
		return true;
	}

	@Override
	public NationalizationSupport getNationalizationSupport() {
		return NationalizationSupport.IMPLICIT;
	}

	@Override
	public boolean supportsTemporaryTables() {
		return false;
	}

	@Override
	public boolean supportsTemporaryTablePrimaryKey() {
		return false;
	}

	@Override
	protected boolean supportsPredicateAsExpression() {
		return false;
	}

	@Override
	public String translateExtractField(TemporalUnit unit) {
		return switch ( unit ) {
			case DAY_OF_MONTH -> "day";
			case DAY_OF_YEAR -> "dayofyear";
			case DAY_OF_WEEK -> "dayofweek";
			default -> super.translateExtractField( unit );
		};
	}

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			final String constraintName;
			switch ( JdbcExceptionHelper.extractErrorCode( sqlException ) ) {
				case 334393:       // response timeout
				case 4164:         // query timeout
				case 69749:        // lock timeout
					return new LockTimeoutException(message, sqlException, sql );
				case 334421:       // row already exists in a unique index
				case 69720:        // unique constraint violated
					constraintName = extractConstraintName( sqlException );
					return new ConstraintViolationException(
							message,
							sqlException,
							sql,
							ConstraintKind.UNIQUE,
							constraintName
					);
				case 200820:        // Cannot insert NULL or update to NULL
					constraintName = extractConstraintName( sqlException );
					return new ConstraintViolationException(
							message,
							sqlException,
							sql,
							ConstraintKind.NOT_NULL,
							constraintName
					);
				case 200823:        // foreign key constraint violation
				case 200822: 	    // failed on update or delete by foreign key constraint violation
					constraintName = extractConstraintName( sqlException );
					return new ConstraintViolationException(
							message,
							sqlException,
							sql,
							ConstraintKind.FOREIGN_KEY,
							constraintName
					);
				case 201618:        // check constraint violation
					constraintName = extractConstraintName( sqlException );
					return new ConstraintViolationException(
							message,
							sqlException,
							sql,
							ConstraintKind.CHECK,
							constraintName
					);
				default:
					return null;
			}
		};
	}

	private static String extractConstraintName(SQLException sqlException) {
		final String sqlExceptionMessage = sqlException.getMessage();
		if ( sqlExceptionMessage == null ) {
			return null;
		}
		final String checkConstraintName = extractConstraintName( sqlExceptionMessage, CHECK_CONSTRAINT_NAME_PATTERN );
		if ( checkConstraintName != null ) {
			return checkConstraintName;
		}
		final String forConstraintName = extractConstraintName( sqlExceptionMessage, FOR_CONSTRAINT_NAME_PATTERN );
		if ( forConstraintName != null ) {
			return forConstraintName;
		}
		return extractConstraintName( sqlExceptionMessage, COLON_CONSTRAINT_NAME_PATTERN );
	}

	private static String extractConstraintName(String sqlExceptionMessage, Pattern pattern) {
		final Matcher matcher = pattern.matcher( sqlExceptionMessage );
		if ( !matcher.matches() ) {
			return null;
		}
		final String constraintName = matcher.group( 1 ).trim();
		return constraintName.isEmpty() ? null : constraintName;
	}

	@Override
	public String getDual() {
		return "dual";
	}

	@Override
	public String getFromDualForSelectOnly() {
		return " from " + getDual();
	}

	@Override
	public boolean supportsJoinsInDelete() {
		return true;
	}

	@Override
	public boolean supportsSimpleQueryGrouping() {
		return false;
	}

	@Override
	public boolean supportsWithClauseInSubquery() {
		return false;
	}

	@Override
	public boolean supportsRowValueConstructorSyntaxInQuantifiedPredicates() {
		return false;
	}

}
