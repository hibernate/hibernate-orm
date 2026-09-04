/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.community.dialect.function.CUBRIDExtractFunction;
import org.hibernate.community.dialect.identity.CUBRIDIdentityColumnSupport;
import org.hibernate.community.dialect.sequence.CUBRIDSequenceSupport;
import org.hibernate.community.dialect.sequence.SequenceInformationExtractorCUBRIDDatabaseImpl;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.NationalizationSupport;
import org.hibernate.dialect.NullOrdering;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.TimeZoneSupport;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.TruncFunction;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.internal.LockingSupportSimple;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitLimitHandler;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.type.MySQLCastingJsonArrayJdbcTypeConstructor;
import org.hibernate.dialect.type.MySQLCastingJsonJdbcType;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.jdbc.env.spi.SchemaNameResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.query.SemanticException;
import org.hibernate.dialect.type.IntervalType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BlobJdbcType;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharUUIDJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.internal.BinaryFloatDdlType;
import org.hibernate.type.descriptor.sql.internal.CapacityDependentDdlType;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.internal.util.JdbcExceptionHelper.extractErrorCode;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.DOUBLE;
import static org.hibernate.type.SqlTypes.FLOAT;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.LONGVARBINARY;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.REAL;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.UUID;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsDate;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsLocalTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithMicros;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithMillis;

/**
 * An SQL dialect for CUBRID 10.2 and above.
 *
 * @author Seok Jeong Il
 */
public class CUBRIDDialect extends Dialect {

	private static final DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 10, 2 );

	/**
	 * Constructs a CUBRIDDialect
	 */
	public CUBRIDDialect() {
		this( MINIMUM_VERSION );
	}

	public CUBRIDDialect(DatabaseVersion version) {
		super( version );
	}

	@Override
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_VERSION;
	}

	@Override
	protected String columnType(int sqlTypeCode) {
		return switch ( sqlTypeCode ) {
			//CUBRID's 'bit' is a fixed-length bit string that rejects boolean host
			//variables, so map boolean to a numeric type instead
			case BOOLEAN -> "smallint";
			case TINYINT -> "smallint";
			//CUBRID's 'time' does not accept an explicit precision (e.g. time(0))
			case TIME -> "time";
			//'timestamp' has a very limited range
			//'datetime' does not support explicit precision
			//(always 3, millisecond precision)
			case TIMESTAMP -> "datetime";
			case TIME_WITH_TIMEZONE, TIMESTAMP_WITH_TIMEZONE -> "datetimetz";
			// CUBRID has no national CLOB, so map NCLOB (and LONG32NVARCHAR, which resolves via NCLOB) to CLOB
			case NCLOB -> "clob";
			default -> super.columnType( sqlTypeCode );
		};
	}

	@Override
	protected String castType(int sqlTypeCode) {
		//CUBRID rejects an explicit binary precision (e.g. float(53)) as a cast target,
		//so cast floating-point types to 'double' instead of float($p)
		return switch ( sqlTypeCode ) {
			case FLOAT, REAL, DOUBLE -> "double";
			default -> super.castType( sqlTypeCode );
		};
	}

	@Override
	public String castPattern(CastType from, CastType to) {
		// CUBRID's default temporal-to-string cast uses a locale format; render ISO via to_char
		if ( to == CastType.STRING ) {
			switch ( from ) {
				case DATE:
					return "to_char(?1,'YYYY-MM-DD')";
				case TIME:
					return "to_char(?1,'HH24:MI:SS')";
				case TIMESTAMP:
					return "to_char(?1,'YYYY-MM-DD HH24:MI:SS.FF')";
			}
		}
		return super.castPattern( from, to );
	}

	@Override
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( UUID, "varchar(36)", this ) );

		//keep BinaryFloatDdlType's binary->decimal precision conversion for DDL, but cast
		//floating-point to 'double' since CUBRID rejects float($p) with a large precision (e.g. float(53))
		ddlTypeRegistry.addDescriptor( new BinaryFloatDdlType( this ) {
			@Override
			public String getCastTypeName(Size columnSize, SqlExpressible type, DdlTypeRegistry registry) {
				return "double";
			}
		} );
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( JSON, "json", this ) );

		//CUBRID has no 'binary' nor 'varbinary', but 'bit' is
		//intended to be used for binary data (unfortunately the
		//length parameter is measured in bits, not bytes)
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( BINARY, "bit($l)", this ) );
		ddlTypeRegistry.addDescriptor(
				CapacityDependentDdlType.builder(
								VARBINARY,
								CapacityDependentDdlType.LobKind.BIGGEST_LOB,
								columnType( BLOB ),
								this
						)
						.withTypeCapacity( getMaxVarbinaryLength(), "bit varying($l)" )
						.build()
		);
	}

	@Override
	protected void registerDefaultKeywords() {
		super.registerDefaultKeywords();
		registerKeyword( "ABSOLUTE" );
		registerKeyword( "ACCESS" );
		registerKeyword( "ACTION" );
		registerKeyword( "ADD_MONTHS" );
		registerKeyword( "AFTER" );
		registerKeyword( "ALIAS" );
		registerKeyword( "ASC" );
		registerKeyword( "ASSERTION" );
		registerKeyword( "ATTACH" );
		registerKeyword( "ATTRIBUTE" );
		registerKeyword( "AVG" );
		registerKeyword( "BEFORE" );
		registerKeyword( "BIT" );
		registerKeyword( "BIT_LENGTH" );
		registerKeyword( "BOOLEAN" );
		registerKeyword( "BREADTH" );
		registerKeyword( "CASCADE" );
		registerKeyword( "CATALOG" );
		registerKeyword( "CHANGE" );
		registerKeyword( "CLASS" );
		registerKeyword( "CLASSES" );
		registerKeyword( "COALESCE" );
		registerKeyword( "CONNECTION" );
		registerKeyword( "CONNECT_BY_ISCYCLE" );
		registerKeyword( "CONNECT_BY_ISLEAF" );
		registerKeyword( "CONNECT_BY_ROOT" );
		registerKeyword( "CONSTRAINTS" );
		registerKeyword( "CONVERT" );
		registerKeyword( "COUNT" );
		registerKeyword( "CURRENT_DATETIME" );
		registerKeyword( "DATA" );
		registerKeyword( "DATABASE" );
		registerKeyword( "DATETIME" );
		registerKeyword( "DAY_HOUR" );
		registerKeyword( "DAY_MILLISECOND" );
		registerKeyword( "DAY_MINUTE" );
		registerKeyword( "DAY_SECOND" );
		registerKeyword( "DEFERRABLE" );
		registerKeyword( "DEFERRED" );
		registerKeyword( "DEPTH" );
		registerKeyword( "DESC" );
		registerKeyword( "DESCRIPTOR" );
		registerKeyword( "DIAGNOSTICS" );
		registerKeyword( "DICTIONARY" );
		registerKeyword( "DIFFERENCE" );
		registerKeyword( "DISTINCTROW" );
		registerKeyword( "DIV" );
		registerKeyword( "DOMAIN" );
		registerKeyword( "DUPLICATE" );
		registerKeyword( "ELSEIF" );
		registerKeyword( "EQUALS" );
		registerKeyword( "EVALUATE" );
		registerKeyword( "EXCEPTION" );
		registerKeyword( "EXTRACT" );
		registerKeyword( "FILE" );
		registerKeyword( "FIRST" );
		registerKeyword( "FOUND" );
		registerKeyword( "GENERAL" );
		registerKeyword( "GO" );
		registerKeyword( "GOTO" );
		registerKeyword( "HOUR_MILLISECOND" );
		registerKeyword( "HOUR_MINUTE" );
		registerKeyword( "HOUR_SECOND" );
		registerKeyword( "IGNORE" );
		registerKeyword( "INDEX" );
		registerKeyword( "INHERIT" );
		registerKeyword( "INITIALLY" );
		registerKeyword( "INTERSECTION" );
		registerKeyword( "ISOLATION" );
		registerKeyword( "JSON" );
		registerKeyword( "KEY" );
		registerKeyword( "LAST" );
		registerKeyword( "LESS" );
		registerKeyword( "LEVEL" );
		registerKeyword( "LIMIT" );
		registerKeyword( "LIST" );
		registerKeyword( "LOCAL_TRANSACTION_ID" );
		registerKeyword( "LOWER" );
		registerKeyword( "MAX" );
		registerKeyword( "MILLISECOND" );
		registerKeyword( "MIN" );
		registerKeyword( "MINUTE_MILLISECOND" );
		registerKeyword( "MINUTE_SECOND" );
		registerKeyword( "MOD" );
		registerKeyword( "MODIFY" );
		registerKeyword( "MULTISET_OF" );
		registerKeyword( "NA" );
		registerKeyword( "NAMES" );
		registerKeyword( "NEXT" );
		registerKeyword( "NULLIF" );
		registerKeyword( "OBJECT" );
		registerKeyword( "OCTET_LENGTH" );
		registerKeyword( "OFF" );
		registerKeyword( "OID" );
		registerKeyword( "OPTIMIZATION" );
		registerKeyword( "OPTION" );
		registerKeyword( "PARAMETERS" );
		registerKeyword( "PARTIAL" );
		registerKeyword( "POSITION" );
		registerKeyword( "PRESERVE" );
		registerKeyword( "PRIOR" );
		registerKeyword( "PRIVILEGES" );
		registerKeyword( "QUERY" );
		registerKeyword( "READ" );
		registerKeyword( "RELATIVE" );
		registerKeyword( "RENAME" );
		registerKeyword( "REPLACE" );
		registerKeyword( "RESTRICT" );
		registerKeyword( "ROLE" );
		registerKeyword( "ROUTINE" );
		registerKeyword( "ROWNUM" );
		registerKeyword( "SCHEMA" );
		registerKeyword( "SECOND_MILLISECOND" );
		registerKeyword( "SECTION" );
		registerKeyword( "SEQUENCE" );
		registerKeyword( "SEQUENCE_OF" );
		registerKeyword( "SERIALIZABLE" );
		registerKeyword( "SESSION" );
		registerKeyword( "SESSION_USER" );
		registerKeyword( "SETEQ" );
		registerKeyword( "SET_OF" );
		registerKeyword( "SHARED" );
		registerKeyword( "SIBLINGS" );
		registerKeyword( "SIZE" );
		registerKeyword( "SQLCODE" );
		registerKeyword( "SQLERROR" );
		registerKeyword( "STATISTICS" );
		registerKeyword( "STRING" );
		registerKeyword( "SUBCLASS" );
		registerKeyword( "SUBSET" );
		registerKeyword( "SUBSETEQ" );
		registerKeyword( "SUBSTRING" );
		registerKeyword( "SUM" );
		registerKeyword( "SUPERCLASS" );
		registerKeyword( "SUPERSET" );
		registerKeyword( "SUPERSETEQ" );
		registerKeyword( "SYSDATE" );
		registerKeyword( "SYSDATETIME" );
		registerKeyword( "SYSTIME" );
		registerKeyword( "SYS_CONNECT_BY_PATH" );
		registerKeyword( "SYS_DATE" );
		registerKeyword( "SYS_DATETIME" );
		registerKeyword( "SYS_TIME" );
		registerKeyword( "SYS_TIMESTAMP" );
		registerKeyword( "SYS_USER" );
		registerKeyword( "TEMPORARY" );
		registerKeyword( "TEST" );
		//reserved through CUBRID 11.0 but not documented in the keyword list
		registerKeyword( "TIMEZONE" );
		registerKeyword( "TRANSACTION" );
		registerKeyword( "TRANSLATE" );
		registerKeyword( "TRIM" );
		registerKeyword( "TRUNCATE" );
		registerKeyword( "UNDER" );
		registerKeyword( "UPPER" );
		registerKeyword( "USAGE" );
		registerKeyword( "USE" );
		registerKeyword( "UTIME" );
		registerKeyword( "VARIABLE" );
		registerKeyword( "VCLASS" );
		registerKeyword( "VIEW" );
		registerKeyword( "WORK" );
		registerKeyword( "WRITE" );
		registerKeyword( "XOR" );
		registerKeyword( "YEAR_MONTH" );
		registerKeyword( "ZONE" );
	}

	public CUBRIDDialect(DialectResolutionInfo info) {
		super( info );
	}

	@Override
	public int getDefaultStatementBatchSize() {
		return 15;
	}

	@Override
	public boolean getDefaultUseGetGeneratedKeys() {
		//the CUBRID JDBC driver reports support for getGeneratedKeys() but returns a result set
		//whose internal connection and statement are unset, so read the identity with a select
		return false;
	}

	@Override
	public int getMaxVarcharLength() {
		return 1_073_741_823;
	}

	@Override
	public int getMaxVarbinaryLength() {
		//note that the length of BIT VARYING in CUBRID is actually in bits
		return 1_073_741_823;
	}

	@Override
	public SizeStrategy getSizeStrategy() {
		return new SizeStrategyImpl() {
			@Override
			public Size resolveSize(
					JdbcType jdbcType,
					JavaType<?> javaType,
					Integer precision,
					Integer scale,
					Long length) {
				final Size size = super.resolveSize( jdbcType, javaType, precision, scale, length );
				// CUBRID measures 'bit'/'bit varying' length in bits, so scale the byte length up to bits
				final int ddlTypeCode = jdbcType.getDdlTypeCode();
				if ( ( ddlTypeCode == BINARY || ddlTypeCode == VARBINARY || ddlTypeCode == LONGVARBINARY )
						&& size.getLength() != null ) {
					size.setLength( size.getLength() * 8 );
				}
				return size;
			}
		};
	}

	@Override
	public JdbcType resolveSqlTypeDescriptor(
			String columnTypeName,
			int jdbcTypeCode,
			int precision,
			int scale,
			JdbcTypeRegistry jdbcTypeRegistry) {
		if ( jdbcTypeCode == Types.BIT ) {
			return jdbcTypeRegistry.getDescriptor( Types.BOOLEAN );
		}
		return super.resolveSqlTypeDescriptor(
				columnTypeName,
				jdbcTypeCode,
				precision,
				scale,
				jdbcTypeRegistry
		);
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		//CUBRID has no native boolean; store as smallint
		return Types.SMALLINT;
	}

	//not used for anything right now, but it
	//could be used for timestamp literal format
	@Override
	public int getDefaultTimestampPrecision() {
		return 3;
	}

	@Override
	public boolean doesRoundTemporalOnOverflow() {
		//CUBRID truncates the sub-millisecond part rather than rounding it
		return false;
	}

	@Override
	public int getFloatPrecision() {
		return 21; // -> 7 decimal digits
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );
		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();

		//the CUBRID JDBC driver has no stream-based LOB binding, so materialize BLOB/CLOB to
		//byte[]/String (setBytes/setString) instead; CLOB additionally reads back through
		//getClob(), because the driver's getString() skips its wasNull() bookkeeping on the
		//LOB branch and would discard a value read after a null column
		jdbcTypeRegistry.addDescriptor( Types.BLOB, BlobJdbcType.MATERIALIZED );
		jdbcTypeRegistry.addDescriptor( Types.CLOB, CUBRIDClobJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( Types.NCLOB, CUBRIDClobJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( JSON, MySQLCastingJsonJdbcType.INSTANCE );
		jdbcTypeRegistry.addTypeConstructorIfAbsent( MySQLCastingJsonArrayJdbcTypeConstructor.INSTANCE );

		//CUBRID has no native UUID and its driver cannot bind one to 'bit varying'; store the
		//canonical 36-character text form instead
		typeContributions.contributeJdbcType( VarcharUUIDJdbcType.INSTANCE );
	}

	@Override
	public boolean useInputStreamToInsertBlob() {
		//the CUBRID JDBC driver has no stream-based LOB binding
		return false;
	}

	@Override
	public boolean supportsLobValueChangePropagation() {
		//the CUBRID JDBC driver's Clob/Blob are read-only: truncate() and setString() throw
		return false;
	}

	@Override
	public boolean useConnectionToCreateLob() {
		//the base default is !useInputStreamToInsertBlob(), which would ask the connection for a
		//Blob; the CUBRID JDBC driver reports JDBC major version 3, so Hibernate would refuse the
		//contextual creation anyway
		return false;
	}

	@Override
	public boolean getDefaultNonContextualLobCreation() {
		//follows from useConnectionToCreateLob(): LOBs are materialized, never created contextually
		return true;
	}

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		//CUBRID exposes no SQLState for constraint violations, so classify on the server error code
		return (sqlException, message, sql) -> switch ( extractErrorCode( sqlException ) ) {
			case -670, -886, -564 -> new ConstraintViolationException( message, sqlException, sql,
					ConstraintViolationException.ConstraintKind.UNIQUE,
					getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
			case -922, -924 -> new ConstraintViolationException( message, sqlException, sql,
					ConstraintViolationException.ConstraintKind.FOREIGN_KEY,
					getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
			case -631, -225 -> new ConstraintViolationException( message, sqlException, sql,
					ConstraintViolationException.ConstraintKind.NOT_NULL,
					getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
			case -493 -> new SQLGrammarException( message, sqlException, sql );
			default -> null;
		};
	}

	@Override
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}

	//the constraint name is only in the message text, so parse it out by template (English, best-effort)
	private static final ViolatedConstraintNameExtractor EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor( sqle -> switch ( extractErrorCode( sqle ) ) {
				case -670, -886, -564 -> extractUsingTemplate( "INDEX ", "(", sqle.getMessage() );
				case -922, -924 -> extractUsingTemplate( "foreign key '", "'", sqle.getMessage() );
				default -> null;
			} );

	@Override
	public boolean supportsJoinsInDelete() {
		//CUBRID supports multi-table/joined DELETE (e.g. DELETE c FROM t c JOIN ... )
		return true;
	}

	@Override
	public boolean supportsFromClauseInUpdate() {
		//CUBRID supports multi-table/joined UPDATE (e.g. UPDATE t c JOIN ... SET ... )
		return true;
	}

	@Override
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		//joined DELETE/UPDATE require the table alias to qualify columns
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}

	@Override
	public boolean supportsLateral() {
		return true;
	}

	@Override
	public NameQualifierSupport getNameQualifierSupport() {
		return NameQualifierSupport.NONE;
	}

	@Override
	public SchemaNameResolver getSchemaNameResolver() {
		//the CUBRID JDBC driver throws from Connection.getSchema(), which would abort the whole
		//JDBC metadata extraction; CUBRID has no schema qualification anyway
		return (connection, dialect) -> null;
	}

	@Override
	public NationalizationSupport getNationalizationSupport() {
		//CUBRID has no nvarchar/nclob types; map nationalized types to the regular varchar/clob
		return NationalizationSupport.IMPLICIT;
	}

	@Override
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuilder builder, DatabaseMetaData metadata)
			throws SQLException {
		builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.LOWER );
		builder.setQuotedCaseStrategy( IdentifierCaseStrategy.LOWER );
		builder.setAutoQuoteKeywords( true );
		builder.setAutoQuoteDollar( true );
		//pass no metadata: the casing CUBRID's DatabaseMetaData reports does not match how it
		//actually stores identifiers, and applyIdentifierCasing() would overwrite the settings above
		return super.buildIdentifierHelper( builder, null );
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);
		functionFactory.trim2();
		functionFactory.space();
		functionFactory.reverse();
		functionFactory.repeat();
		functionFactory.crc32();
		functionFactory.cot();
		functionFactory.log2();
		functionFactory.log10();
		functionFactory.pi();
		// CUBRID's rand() returns an integer; drand() returns a double in [0,1) like HQL rand()
		functionContributions.getFunctionRegistry().namedDescriptorBuilder( "rand", "drand" )
				.setArgumentCountBetween( 0, 1 )
				.setInvariantType(
						functionContributions.getTypeConfiguration().getBasicTypeRegistry()
								.resolve( StandardBasicTypes.DOUBLE ) )
				.setUseParenthesesWhenNoArgs( true )
				.register();
		functionFactory.radians();
		functionFactory.degrees();
		functionFactory.systimestamp();
		functionFactory.localtimeLocaltimestamp();
		functionFactory.hourMinuteSecond();
		functionFactory.yearMonthDay();
		functionFactory.dayofweekmonthyear();
		functionFactory.lastDay();
		functionFactory.weekQuarter();
		//CUBRID's octet_length()/bit_length() only accept character or bit strings,
		//so measure a LOB argument with clob_length() instead
		functionFactory.octetLength_pattern( "octet_length(?1)", "clob_length(?1)" );
		functionFactory.bitLength_pattern( "bit_length(?1)", "clob_length(?1)*8" );
		functionFactory.md5();
		//CUBRID's native trunc() truncates numbers and dates only to day granularity, so emulate
		//datetime truncation (down to second) by formatting via to_char and parsing back with to_datetime
		functionFactory.format_toChar();
		functionContributions.getFunctionRegistry().register(
				"trunc",
				new TruncFunction(
						"trunc(?1)",
						"trunc(?1,?2)",
						TruncFunction.DatetimeTrunc.FORMAT,
						"to_datetime",
						functionContributions.getTypeConfiguration()
				)
		);
		functionContributions.getFunctionRegistry().registerAlternateKey( "truncate", "trunc" );
		functionFactory.toCharNumberDateTimestamp();
		functionFactory.substr();
		//also natively supports ANSI-style substring()
		functionFactory.instr();
		functionFactory.translate();
		functionFactory.ceiling_ceil();
		functionFactory.sha1();
		functionFactory.sha2();
		functionFactory.ascii();
		functionFactory.char_chr();
		functionFactory.position();
//		functionFactory.concat_pipeOperator();
		functionFactory.insert();
		functionFactory.nowCurdateCurtime();
		functionFactory.makedateMaketime();
		//CUBRID's bit_and/or/xor are aggregates (not the scalar 2-arg form HQL bitand(x,y) needs) and there is
		//no bit_not; use the &|^~ operators instead
		functionFactory.bitandorxornot_operator();
		functionFactory.median();
		functionFactory.stddev();
		functionFactory.stddevPopSamp();
		functionFactory.variance();
		functionFactory.varPopSamp();
		functionFactory.datediff();
		functionFactory.adddateSubdateAddtimeSubtime();
		functionFactory.addMonths();
		functionFactory.monthsBetween();
		functionFactory.rownumInstOrderbyGroupbyNum();
		functionFactory.regexpLike_regexp();
		functionFactory.windowFunctions();
		functionFactory.hypotheticalOrderedSetAggregates_windowEmulation();

		final SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();
		final TypeConfiguration typeConfiguration = functionContributions.getTypeConfiguration();
		// CUBRID rejects extract(millisecond from <time>), so register a custom extract for the TIME case
		functionRegistry.register( "extract", new CUBRIDExtractFunction( this, typeConfiguration ) );

		//the base maps local_time to CUBRID's localtime, but CUBRID's localtime is a TIMESTAMP(datetime),
		//so time=local_time comparisons fail; render it as current_time (a real TIME)
		functionRegistry.noArgsBuilder( "local_time", "current_time" )
				.setInvariantType( typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.LOCAL_TIME ) )
				.setUseParenthesesWhenNoArgs( false )
				.register();
	}

	@Override
	public boolean supportsColumnCheck() {
		return false;
	}

	@Override
	public boolean supportsTableCheck() {
		return false;
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return CUBRIDSequenceSupport.INSTANCE;
	}

	@Override
	public String getDropForeignKeyString() {
		return "drop foreign key";
	}

	@Override
	public String getDropUniqueKeyString() {
		return "drop index";
	}

	@Override
	public boolean qualifyIndexName() {
		return false;
	}

	@Override
	public boolean supportsExistsInSelect() {
		//CUBRID accepts 'exists' in a select list only when the whole predicate is parenthesized,
		//which a mapping that writes the SQL itself (a formula) cannot be made to do
		return false;
	}

	@Override
	public String getQuerySequencesString() {
		return "select * from db_serial";
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceInformationExtractorCUBRIDDatabaseImpl.INSTANCE;
	}

	private static final LockingSupport LOCKING_SUPPORT = new LockingSupportSimple(
			PessimisticLockStyle.CLAUSE,
			LockTimeoutType.NONE,
			OuterJoinLockingType.FULL,
			ConnectionLockTimeoutStrategy.NONE
	);

	@Override
	public LockingSupport getLockingSupport() {
		return LOCKING_SUPPORT;
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public String currentTimestamp() {
		//current_timestamp is a second-precision TIMESTAMP; sys_datetime is a millisecond-precision
		//DATETIME, matching how TIMESTAMP columns are mapped (datetime)
		return "sys_datetime";
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "select now()";
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return true;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		return false;
	}

	@Override
	public boolean supportsBindingNullSqlTypeForSetNull() {
		//CUBRID's setNull() ignores the given SQL type, so Types.NULL is fine,
		//and this avoids the unimplemented getParameterMetaData()
		return true;
	}

	@Override
	public boolean supportsOffsetInSubquery() {
		return true;
	}

	@Override
	public boolean supportsTemporaryTables() {
		return false;
	}

	@Override
	public boolean supportsWindowFunctions() {
		return true;
	}

	@Override
	public boolean supportsWindowFrames() {
		// CUBRID has window functions but no 'over' frame clause (rows/range)
		return false;
	}

	@Override
	public boolean supportsWithClauseInSubquery() {
		return true;
	}

	@Override
	public boolean supportsNestedWithClause() {
		//pinned false: derives from supportsWithClauseInSubquery(), but CUBRID rejects a with clause nested in another CTE
		return false;
	}

	@Override
	public boolean supportsRecursiveCTE() {
		return true;
	}

	@Override
	public boolean supportsNonQueryWithCTE() {
		return true;
	}

	@Override
	public boolean supportsValuesList() {
		return true;
	}

	@Override
	public boolean supportsAlterColumnType() {
		return true;
	}

	//CUBRID cannot change only the column type, so emit the full column definition
	@Override
	public String getAlterColumnTypeString(String columnName, String columnType, String columnDefinition) {
		return "modify column " + columnName + " " + columnDefinition.trim();
	}

	@Override
	public boolean canCreateSchema() {
		return false;
	}

	@Override
	public int getMaxIdentifierLength() {
		//the driver metadata reports 254, but CUBRID rejects a class name over 222 bytes
		return 222;
	}

	@Override
	public NullOrdering getNullOrdering() {
		return NullOrdering.SMALLEST;
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new CUBRIDSqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LimitLimitHandler.INSTANCE;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return CUBRIDIdentityColumnSupport.INSTANCE;
	}

	@Override
	public boolean supportsPartitionBy() {
		return true;
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
		// CUBRID rejects the FM fill-mode modifier, so do not emit it
		appender.appendSql(
				OracleDialect.datetimeFormat( format, false, false )
				.replace("SSSSSS", "FF")
				.replace("SSSSS", "FF")
				.replace("SSSS", "FF")
				.replace("SSS", "FF")
				.replace("SS", "FF")
				.replace("S", "FF")
				.result()
		);
	}

	@Override
	public long getFractionalSecondPrecisionInNanos() {
		return 1_000_000; //milliseconds
	}

	// CUBRID's parser rejects the base dialect's JDBC-escape literals ({d/t/ts '...'}), so emit native
	// date/time/datetime literals; TIMESTAMP uses 'datetime' because 'timestamp' literals reject fractional seconds
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
				if ( temporalAccessor instanceof ZonedDateTime zonedDateTime ) {
					temporalAccessor = zonedDateTime.toOffsetDateTime();
				}
				appender.appendSql( "datetime '" );
				appendAsTimestampWithMicros( appender, temporalAccessor, supportsTemporalLiteralOffset(), jdbcTimeZone, false );
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
				appender.appendSql( "datetime '" );
				appendAsTimestampWithMicros( appender, date, jdbcTimeZone );
				appender.appendSql( '\'' );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	public void appendDateTimeLiteral(SqlAppender appender, Calendar calendar, TemporalType precision, TimeZone jdbcTimeZone) {
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
				appender.appendSql( "datetime '" );
				appendAsTimestampWithMillis( appender, calendar, jdbcTimeZone );
				appender.appendSql( '\'' );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	/**
	 * CUBRID supports a limited list of temporal fields in the
	 * extract() function, but we can emulate some of them by
	 * using the appropriate named functions instead of
	 * extract().
	 *
	 * Thus, the additional supported fields are
	 * {@link TemporalUnit#DAY_OF_YEAR},
	 * {@link TemporalUnit#DAY_OF_MONTH},
	 * {@link TemporalUnit#DAY_OF_YEAR}.
	 *
	 * In addition, the field {@link TemporalUnit#SECOND} is
	 * redefined to include milliseconds.
	 */
	@Override
	public String extractPattern(TemporalUnit unit) {
		return switch (unit) {
			case SECOND -> "(second(?2)+extract(millisecond from ?2)/1e3)";
			case DAY_OF_WEEK -> "dayofweek(?2)";
			case DAY_OF_MONTH ->"dayofmonth(?2)";
			case DAY_OF_YEAR -> "dayofyear(?2)";
			case WEEK -> "week(?2,3)"; //mode 3 is the ISO week
			//CUBRID has no 'epoch' field; use unix_timestamp (seconds since 1970-01-01)
			case EPOCH -> "unix_timestamp(?2)";
			default -> "?1(?2)";
		};
	}

	@Override
	public TimeZoneSupport getTimeZoneSupport() {
		//the CUBRID JDBC driver has no java.time support, so route temporal binding
		//through java.sql.Timestamp by normalizing to the JDBC timezone
		return TimeZoneSupport.NORMALIZE;
	}

	@Override
	public void appendUUIDLiteral(SqlAppender appender, java.util.UUID literal) {
		//CUBRID has no uuid type, so render the text form rather than cast(... as uuid)
		appender.appendSql( '\'' );
		appender.appendSql( literal.toString() );
		appender.appendSql( '\'' );
	}

	@Override
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		if ( temporalType == TemporalType.TIME ) {
			//CUBRID cannot add an interval to a 'time' (adddate rejects a time operand), so convert the
			//time to seconds, add the interval, and wrap back into a single day [0,86400) via sec_to_time
			final String seconds = switch ( unit ) {
				case NANOSECOND -> "(?2)/1e9";
				case NATIVE -> "(?2)/1e3";
				case SECOND -> "(?2)";
				case MINUTE -> "(?2)*60";
				case HOUR -> "(?2)*3600";
				default -> null;
			};
			if ( seconds != null ) {
				return "sec_to_time(((time_to_sec(?3)+" + seconds + ") mod 86400+86400) mod 86400)";
			}
		}
		return switch (unit) {
			case NANOSECOND -> "adddate(?3,interval (?2)/1e6 millisecond)";
			case NATIVE -> "adddate(?3,interval ?2 millisecond)";
			//'interval <n> second' takes whole seconds, so scale to milliseconds to keep the fraction
			case SECOND -> "adddate(?3,interval (?2)*1e3 millisecond)";
			default -> "adddate(?3,interval ?2 ?1)";
		};
	}

	@Override
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		switch ( unit ) {
			case DAY:
				//note: datediff() is backwards on CUBRID
				return "datediff(?3,?2)";
			case YEAR:
				return "(year(?3)-year(?2))";
			case MONTH:
				return "((year(?3)-year(?2))*12+(month(?3)-month(?2)))";
			case QUARTER:
				return "(((year(?3)-year(?2))*12+(month(?3)-month(?2)))/3)";
			case WEEK:
				return "(datediff(?3,?2)/7)";
			//CUBRID has no timestampdiff() and timediff() overflows past 24h (the TIME range), so build
			//the difference in whole seconds with datediff()+time_to_sec() (overflow-free) and scale it
			//to the requested unit. Integer '/' truncates on CUBRID, matching timestampdiff semantics.
			case HOUR:
				return "(" + secondDiff( fromTemporalType, toTemporalType ) + "/3600)";
			case MINUTE:
				return "(" + secondDiff( fromTemporalType, toTemporalType ) + "/60)";
			case SECOND:
				return secondDiff( fromTemporalType, toTemporalType );
			case NATIVE:
				//CUBRID's native fractional-second precision is milliseconds. A sub-second diff cannot be
				//computed portably (current_timestamp is a second-precision TIMESTAMP and extract(millisecond)
				//rejects it), so the diff is capped at whole seconds scaled to milliseconds.
				return "(" + secondDiff( fromTemporalType, toTemporalType ) + "*1e3)";
			case NANOSECOND:
				//second-precision cap (see NATIVE): the sub-second digits are always 0
				return "(" + secondDiff( fromTemporalType, toTemporalType ) + "*1e9)";
			default:
				throw new SemanticException("unsupported temporal unit for CUBRID: " + unit);
		}
	}

	/**
	 * Renders the difference in whole seconds between {@code ?2} (from) and {@code ?3} (to) without
	 * {@code timediff()}, which is limited to CUBRID's 24-hour TIME range. The whole-day part comes from
	 * {@code datediff()} and the time-of-day part from {@code time_to_sec()}; each is omitted for an
	 * operand that carries no date (a TIME) or no time (a DATE), since those functions reject such a value.
	 */
	private static String secondDiff(TemporalType fromTemporalType, TemporalType toTemporalType) {
		final boolean wholeDays = fromTemporalType != TemporalType.TIME && toTemporalType != TemporalType.TIME;
		final boolean toHasTime = toTemporalType != TemporalType.DATE;
		final boolean fromHasTime = fromTemporalType != TemporalType.DATE;
		final StringBuilder pattern = new StringBuilder( "(" );
		String separator = "";
		if ( wholeDays ) {
			//note: datediff() is backwards on CUBRID and ignores the time component
			pattern.append( "datediff(?3,?2)*86400" );
			separator = "+";
		}
		if ( toHasTime ) {
			pattern.append( separator ).append( "time_to_sec(?3)" );
		}
		if ( fromHasTime ) {
			if ( pattern.length() == 1 ) {
				pattern.append( "0" );
			}
			pattern.append( "-time_to_sec(?2)" );
		}
		return pattern.append( ")" ).toString();
	}

	@Override
	public String getDual() {
		return "db_root";
	}

	@Override
	public String getFromDualForSelectOnly() {
		return " from " + getDual();
	}

	@Override
	public boolean supportsRowValueConstructorSyntax() {
		// CUBRID supports row value constructors (a,b) only from 11.0; emulate them below that
		return getVersion().isSameOrAfter( 11, 0 );
	}

	@Override
	public boolean supportsRowValueConstructorGtLtSyntax() {
		return getVersion().isSameOrAfter( 11, 0 );
	}

	@Override
	public boolean supportsRowValueConstructorSyntaxInQuantifiedPredicates() {
		return false;
	}

	@Override
	public boolean supportsRowValueConstructorSyntaxInInList() {
		return getVersion().isSameOrAfter( 11, 0 );
	}

	@Override
	public boolean supportsRowValueConstructorSyntaxInInSubQuery() {
		return getVersion().isSameOrAfter( 11, 0 );
	}

	@Override
	public int getInExpressionCountLimit() {
		//CUBRID rejects an expression nested beyond 400 levels. Before 11.0 there is no row value
		//constructor, so core emulates a multi-key batch as one 'or' per key tuple; this bound
		//keeps a two-column key at 250 tuples, safely under the limit
		return 500;
	}

	/**
	 * Binds a CLOB by materializing it to a string, since the CUBRID JDBC driver has no
	 * stream-based LOB binding, but reads it back with {@code getClob()}. The driver's
	 * {@code getString()} returns the value without updating its {@code wasNull} flag when the
	 * column is a LOB, so a LOB read after a null column would be discarded as null.
	 */
	private static class CUBRIDClobJdbcType extends ClobJdbcType {
		static final CUBRIDClobJdbcType INSTANCE = new CUBRIDClobJdbcType();

		@Override
		public String toString() {
			return "ClobTypeDescriptor(CUBRID)";
		}

		@Override
		protected <X> BasicBinder<X> getClobBinder(JavaType<X> javaType) {
			return new BasicBinder<>( javaType, this ) {
				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
						throws SQLException {
					st.setString( index, javaType.unwrap( value, String.class, options ) );
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
						throws SQLException {
					st.setString( name, javaType.unwrap( value, String.class, options ) );
				}
			};
		}
	}

}
