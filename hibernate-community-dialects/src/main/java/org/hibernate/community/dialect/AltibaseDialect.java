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
import static org.hibernate.SPI.Role.USE;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import org.hibernate.dialect.type.spi.StandardDdlTypes;

import org.hibernate.dialect.type.spi.TypeSizingProfile;

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
import org.hibernate.dialect.sql.ast.spi.SetOperationSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;

import jakarta.persistence.TemporalType;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.community.dialect.pagination.AltibaseLimitHandler;
import org.hibernate.community.dialect.sequence.AltibaseSequenceSupport;
import org.hibernate.dialect.type.spi.BooleanDecoder;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.jdbc.spi.ParameterLimits;
import org.hibernate.dialect.sql.ast.spi.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.type.spi.NationalizationSupport;
import org.hibernate.dialect.sql.ast.spi.NullOrdering;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.lock.spi.RowLockStrategy;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.OracleTruncFunction;
import org.hibernate.dialect.function.TrimFunction;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.StandardLockingSupports;
import org.hibernate.dialect.namespace.spi.NamespaceSupport;
import org.hibernate.dialect.namespace.spi.NamespaceSupports;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.schema.spi.ConstraintDropMode;
import org.hibernate.dialect.schema.spi.IndexNameQualification;
import org.hibernate.dialect.schema.spi.SchemaDropSupport;
import org.hibernate.dialect.temporaltype.spi.IntervalType;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.dialect.identifier.spi.IdentifierHelperBuildRequest;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.jdbc.spi.JdbcExceptionHelper;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.query.sqm.TrimSpec;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
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
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JsonJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.SQLException;
import java.sql.Types;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.List;
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
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTimestampWithMicros;

/**
 * An SQL dialect for Altibase 7.1 and above.
 *
 * @author Geoffrey Park
 */
public class AltibaseDialect extends Dialect implements CurrentTemporalSupport, TemporalFormatSupport, TemporalOperationSupport {
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
			.maxVarcharLength( 32_000 ).maxVarcharCapacity( 32_000 )
			.maxNVarcharLength( 32_000 ).maxNVarcharCapacity( 32_000 )
			.maxVarbinaryLength( 32_000 ).maxVarbinaryCapacity( 32_000 )
			.build();

	@Override public TypeSizingProfile getTypeSizingProfile() { return typeSizingProfile; }

	private static final DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 7, 1 );
	// Altibase's IN-list limit is around 1,020 elements; use 1,000 to stay below
	// that limit and avoid "Calculation stack overflow" during prepare.
	private static final int IN_LIST_SIZE_LIMIT = 1000;
	private static final Pattern CHECK_CONSTRAINT_NAME_PATTERN = Pattern.compile( "Check constraint (.+) violated" );
	private static final Pattern FOR_CONSTRAINT_NAME_PATTERN = Pattern.compile( ".*\\sfor\\s+([^.]+)\\.?" );
	private static final Pattern COLON_CONSTRAINT_NAME_PATTERN = Pattern.compile( ".*:\\s*(.+)" );
	private static final LockingSupport LOCKING_SUPPORT = StandardLockingSupports.parameterized(
			PessimisticLockStyle.CLAUSE,
			RowLockStrategy.NONE,
			LockTimeoutType.QUERY,
			LockTimeoutType.QUERY,
			LockTimeoutType.NONE,
			OuterJoinLockingType.UNSUPPORTED
	);

	@SuppressWarnings("unused")
	public AltibaseDialect() {
		this( MINIMUM_VERSION );
	}

	public AltibaseDialect(DialectResolutionInfo info) {
		this( info.makeCopyOrDefault( MINIMUM_VERSION ) );
	}

	public AltibaseDialect(DatabaseVersion version) {
		super( version );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
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
	@SPI({ IMPLEMENT, SUPPLY })
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		ddlTypeRegistry.addDescriptor(
				StandardDdlTypes.builder( BINARY, columnType( LONGVARBINARY ), this )
						.withTypeCapacity( getTypeSizingProfile().maxVarbinaryLength(), columnType( BINARY ) )
						.build()
		);
		ddlTypeRegistry.addDescriptor(
				StandardDdlTypes.builder( BIT, columnType( LONGVARBINARY ), this )
						.withTypeCapacity( 64000, columnType( BIT ) )
						.build()
		);
		if ( supportsNativeJsonType() ) {
			ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( JSON, "json", this ) );
		}
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
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
	@SPI({ USE, IMPLEMENT })
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
	@SPI({ IMPLEMENT, SUPPLY })
	protected void contributeDefaultProperties(java.util.Properties properties) {
		super.contributeDefaultProperties( properties );
		properties.setProperty( org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE, Integer.toString( 15 ) );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String trimPattern(TrimSpec specification, boolean isWhitespace) {
		return switch ( specification ) {
			case BOTH -> isWhitespace ? "trim(?1)" : "trim(?1,?2)";
			case LEADING -> isWhitespace ? "ltrim(?1)" : "ltrim(?1,?2)";
			case TRAILING -> isWhitespace ? "rtrim(?1)": "rtrim(?1,?2)";
		};
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
	@SPI({ USE, IMPLEMENT })
	public String currentDate() {
		return currentTimestamp();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTime() {
		return currentTimestamp();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTimestamp() {
		return "sysdate";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentLocalTime() {
		return currentLocalTimestamp();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentLocalTimestamp() {
		// Drop microseconds, because sysdate comes with microseconds.
		return "trunc(sysdate,'second')";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTimestampWithTimeZone() {
		return currentTimestamp();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new AltibaseSqlAstTranslator<>( request );
			}
		};
	}

	/**
	 * In Altibase, `timestampadd` and `datediff` with microseconds have limitations,
	 * so use seconds as the native precision.
	 */
	@Override
	@SPI({ USE, IMPLEMENT })
	public long fractionalSecondPrecisionInNanos() {
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
	@SPI({ USE, IMPLEMENT })
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
			default ->  TemporalOperationSupports.standard().extractPattern( unit );
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		return switch (unit) {
			case NANOSECOND -> "timestampadd(MICROSECOND,(?2)/1e3,?3)";
			case NATIVE -> "timestampadd(SECOND, ?2, ?3)";
			default ->  "timestampadd(?1, ?2, ?3)";
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		return switch (unit) {
			case SECOND, NATIVE -> "datediff(?2, ?3, 'SECOND')";
			case NANOSECOND -> "datediff(?2, ?3, 'MICROSECOND')*1e3";
			default -> "datediff(?2, ?3, '?1')";
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendBinaryLiteral(SqlAppender appender, byte[] bytes) {
		appender.appendSql( "VARBYTE'" );
		PrimitiveByteArrayJavaType.INSTANCE.appendString( appender, bytes );
		appender.appendSql( '\'' );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendFormat(SqlAppender appender, String format) {
		appender.appendSql( OracleDialect.datetimeFormat( format, false, false ).result() );
	}

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
	@SPI({ USE, IMPLEMENT })
	public void appendDateTimeLiteral(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		if (precision == TemporalType.TIMESTAMP) {
			appender.appendSql("{ts '");
			appendAsTimestampWithMicros(appender, temporalAccessor, getTemporalValueSemantics().supportsLiteralOffset(), jdbcTimeZone);
			appender.appendSql("'}");
			return;
		}
		super.appendDateTimeLiteral(appender, temporalAccessor, precision, jdbcTimeZone);
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendDateTimeLiteral(SqlAppender appender, Date date, TemporalType precision, TimeZone jdbcTimeZone) {
		if (precision == TemporalType.TIMESTAMP) {
			appender.appendSql("{ts '");
			appendAsTimestampWithMicros( appender, date, jdbcTimeZone );
			appender.appendSql("'}");
			return;
		}
		super.appendDateTimeLiteral(appender, date, precision, jdbcTimeZone);
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String translateDurationField(TemporalUnit unit) {
		//use microsecond as the "native" precision
		if ( unit == TemporalUnit.NATIVE ) {
			return "microsecond";
		}
		return TemporalOperationSupports.standard().translateDurationField( unit );
	}

	@Override
	public NullOrderingSupport getNullOrderingSupport() {
		return NullOrderingSupport.builder( super.getNullOrderingSupport() )
				.defaultOrdering( NullOrdering.LAST )
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String addColumnPrefix() {
		return "add column (";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String addColumnSuffix() {
		return ")";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public int getMaxIdentifierLength() {
		return 40;
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuildRequest request) {
		final var builder = request.builder();
		// Any use of keywords as identifiers will result in syntax error, so enable auto quote always
		builder.setAutoQuoteKeywords( true );
		builder.setAutoQuoteInitialUnderscore( false );

		return super.buildIdentifierHelper( request );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NameQualifierSupport getNameQualifierSupport() {
		return NameQualifierSupport.SCHEMA;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NamespaceSupport getNamespaceSupport() {
		return NamespaceSupports.none();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public IndexNameQualification nameQualification() {
		return IndexNameQualification.UNQUALIFIED;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public org.hibernate.dialect.schema.spi.SchemaCommentSupport getSchemaCommentSupport() {
		return org.hibernate.dialect.schema.spi.SchemaCommentSupports.commentOn();
	}

	@Override
	public TupleCountSupport getTupleCountSupport() {
		return TupleCountSupport.NONE;
	}

	@Override
	public SubquerySupport getSubquerySupport() {
		return SubquerySupport.builder()
				.feature( SubquerySupport.Feature.EXISTS_IN_SELECT, false )
				.feature( SubquerySupport.Feature.ORDER_BY, false )
				.build();
	}

	@Override
	public MutationSyntaxSupport getMutationSyntaxSupport() {
		return MutationSyntaxSupport.builder()
				.capability( MutationKind.UPDATE, MutationSyntaxCapability.FROM_CLAUSE )
				.capability( MutationKind.DELETE, MutationSyntaxCapability.JOIN )
				.build();
	}

	@Override
	public LockingSupport getLockingSupport() {
		return LOCKING_SUPPORT;
	}









	@Override
	public boolean supportsCrossJoin() {
		return false;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		return AltibaseSequenceSupport.INSTANCE;
	}

	private static final SequenceInformationExtractor SEQUENCE_INFORMATION_EXTRACTOR =
			SequenceInformationExtractors.builder(
					"SELECT a.user_name USER_NAME, b.table_name SEQUENCE_NAME, c.current_seq CURRENT_VALUE, "
				+ "c.start_seq START_VALUE, c.min_seq MIN_VALUE, c.max_seq MAX_VALUE, c.increment_seq INCREMENT_BY, "
				+ "c.flag CYCLE_, c.sync_interval CACHE_SIZE "
				+ "FROM system_.sys_users_ a, system_.sys_tables_ b, x$seq c "
				+ "WHERE a.user_id = b.user_id AND b.table_oid = c.seq_oid AND a.user_name <> 'SYSTEM_' AND b.table_type = 'S' "
				+ "ORDER BY 1,2"
			)
			.withoutCatalog()
			.withoutSchema()
			.sequenceNameColumn( "sequence_name" )
			.startValueColumn( "start_value" )
			.minimumValueColumn( "min_value" )
			.maximumValueColumn( "max_value" )
			.incrementValueColumn( "increment_by" )
			.build();

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SEQUENCE_INFORMATION_EXTRACTOR;
	}

	@Override
	public LimitHandler getLimitHandler() {
		return AltibaseLimitHandler.INSTANCE;
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public CurrentTimestampSelection getCurrentTimestampSelection() {
		return CurrentTimestampSelection.prepared( "select sysdate from dual" );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized SchemaDropSupport getSchemaDropSupport() {
		if ( schemaDropSupport == null ) {
			schemaDropSupport = new SchemaDropSupport( List.of(), ConstraintDropMode.EXPLICIT, " cascade constraints" );
		}
		return schemaDropSupport;
	}

	@Override
	public ParameterLimits getParameterLimits() {
		return ParameterLimits.of( IN_LIST_SIZE_LIMIT );
	}

	@Override
	public WindowFunctionSupport getWindowFunctionSupport() {
		return WindowFunctionSupport.builder()
				.features(
						WindowFunctionSupport.Feature.WINDOW_FUNCTIONS,
						WindowFunctionSupport.Feature.PARTITION_BY
				)
				.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NationalizationSupport getNationalizationSupport() {
		return NationalizationSupport.IMPLICIT;
	}

	@Override
	public PredicateSupport getPredicateSupport() {
		return PredicateSupport.builder( super.getPredicateSupport() )
				.capability( PredicateSupport.Capability.EXPRESSION_PLACEMENT, false )
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String translateExtractField(TemporalUnit unit) {
		return switch ( unit ) {
			case DAY_OF_MONTH -> "day";
			case DAY_OF_YEAR -> "dayofyear";
			case DAY_OF_WEEK -> "dayofweek";
			default -> TemporalOperationSupports.standard().translateExtractField( unit );
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
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
				.operator( SetOperator.INTERSECT_ALL, false )
				.operator( SetOperator.EXCEPT_ALL, false )
				.capability( SetOperationSupport.Capability.SIMPLE_QUERY_GROUPING, false )
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String getSetOperatorSqlString(SetOperator setOperator) {
		return setOperator == SetOperator.EXCEPT
				? "minus"
				: super.getSetOperatorSqlString( setOperator );
	}

	@Override
	public CteSupport getCteSupport() {
		return CteSupport.builder()
				.placement( CteSupport.Placement.TOP_LEVEL )
				.requiresRecursiveKeyword( false )
				.build();
	}

	@Override
	public RowValueSupport getRowValueSupport() {
		return RowValueSupport.builder( super.getRowValueSupport() )
				.feature( RowValueSupport.Feature.QUANTIFIED_COMPARISON, false )
				.build();
	}

}
