/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.identifier.spi.IdentifierHelperBuildRequest;
import org.hibernate.dialect.identifier.spi.KeywordRegistration;

import org.hibernate.dialect.temporaltype.spi.CurrentTimestampSelection;

import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupport;

import org.hibernate.dialect.temporaltype.spi.TemporalFormatSupport;

import org.hibernate.dialect.temporaltype.spi.CurrentTemporalSupport;

import org.hibernate.SPI;
import static org.hibernate.SPI.Role.USE;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import org.hibernate.dialect.type.spi.DdlTypeBuilder;

import org.hibernate.dialect.type.spi.StandardDdlTypes;

import org.hibernate.dialect.type.spi.TypeSizingProfile;

import org.hibernate.dialect.function.spi.TupleCountSupport;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;

import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.sql.ast.spi.MutationKind;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxCapability;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxSupport;
import org.hibernate.dialect.sql.ast.spi.NullOrderingSupport;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.SingleRowTableSupport;
import org.hibernate.dialect.sql.ast.spi.ValuesListSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;

import java.sql.CallableStatement;
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
import org.hibernate.community.dialect.identity.internal.CUBRIDIdentityColumnSupport;
import org.hibernate.community.dialect.sequence.CUBRIDSequenceSupport;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.sql.ast.spi.NullOrdering;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.type.spi.DirectJavaTimeJdbcSupport;
import org.hibernate.dialect.type.spi.DirectJavaTimeJdbcSupports;
import org.hibernate.dialect.type.spi.MySQLJdbcTypes;
import org.hibernate.dialect.type.spi.NationalizationSupport;
import org.hibernate.dialect.type.spi.ObjectNullBindingStrategy;
import org.hibernate.dialect.type.spi.SizeStrategy;
import org.hibernate.dialect.type.spi.StandardSizeStrategy;
import org.hibernate.dialect.type.spi.TimeZoneSupport;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.TruncFunction;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.literal.spi.ZeroOffsetLiteralStyle;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lob.spi.LobSupport;
import org.hibernate.dialect.lob.spi.LobSupports;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.RowLockStrategy;
import org.hibernate.dialect.lock.spi.StandardLockingSupports;
import org.hibernate.dialect.namespace.spi.NamespaceSupport;
import org.hibernate.dialect.namespace.spi.NamespaceSupports;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.LimitLimitHandler;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.schema.spi.AlterColumnTypeRequest;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.schema.spi.IfExistsSupport;
import org.hibernate.dialect.schema.spi.IndexNameQualification;
import org.hibernate.dialect.jdbc.spi.ParameterLimits;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.jdbc.env.spi.SchemaNameResolver;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.query.SemanticException;
import org.hibernate.dialect.temporaltype.spi.IntervalType;
import org.hibernate.dialect.temporaltype.spi.TemporalValueSemantics;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractors;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BlobJdbcType;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.descriptor.jdbc.VarcharUUIDJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

import static org.hibernate.cfg.AvailableSettings.NON_CONTEXTUAL_LOB_CREATION;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsDate;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsLocalTime;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTimestampWithMicros;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTimestampWithMillis;
import static org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE;
import static org.hibernate.cfg.AvailableSettings.USE_GET_GENERATED_KEYS;
import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.jdbc.spi.JdbcExceptionHelper.extractErrorCode;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.LONGVARBINARY;
import static org.hibernate.type.SqlTypes.UUID;
import static org.hibernate.type.SqlTypes.DOUBLE;
import static org.hibernate.type.SqlTypes.FLOAT;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.REAL;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.VARBINARY;

/**
 * An SQL dialect for CUBRID 10.2 and above.
 *
 * @author Seok Jeong Il
 */
public class CUBRIDDialect extends Dialect implements CurrentTemporalSupport, TemporalFormatSupport, TemporalOperationSupport {
	private final org.hibernate.dialect.unique.spi.UniqueDelegate uniqueDelegate =
			new org.hibernate.dialect.unique.spi.DelegatingUniqueDelegate(
					org.hibernate.dialect.unique.spi.UniqueDelegates.alterTable( this ) ) {
		@Override
		public String getAlterTableToDropUniqueKeyCommand(
				org.hibernate.mapping.UniqueKey uniqueKey,
				org.hibernate.boot.Metadata metadata,
				org.hibernate.boot.model.relational.SqlStringGenerationContext context) {
			return delegate().getAlterTableToDropUniqueKeyCommand( uniqueKey, metadata, context )
					.replace( "drop constraint", "drop index" );
		}
	};
	private IfExistsSupport ifExistsSupport;


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
			.defaultTimestampPrecision( 3 )
			.maxTimestampPrecision( 3 )
			.floatPrecision( 21 )
			.maxVarcharLength( 1_073_741_823 ).maxVarcharCapacity( 1_073_741_823 )
			.maxNVarcharLength( 1_073_741_823 ).maxNVarcharCapacity( 1_073_741_823 )
			.maxVarbinaryLength( 1_073_741_823 ).maxVarbinaryCapacity( 1_073_741_823 )
			.build();

	@Override public TypeSizingProfile getTypeSizingProfile() { return typeSizingProfile; }

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
	@SPI({ IMPLEMENT, SUPPLY })
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_VERSION;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		return switch ( sqlTypeCode ) {
			//'bit' is a fixed-length bit string that rejects boolean host variables
			case BOOLEAN -> "smallint";
			case TINYINT -> "smallint";
			//'time' does not accept an explicit precision
			case TIME -> "time";
			//'timestamp' has a very limited range
			//'datetime' does not support explicit precision
			//(always 3, millisecond precision)
			case TIMESTAMP -> "datetime";
			case TIME_WITH_TIMEZONE, TIMESTAMP_WITH_TIMEZONE -> "datetimetz";
			//CUBRID has no national character LOB
			case NCLOB -> "clob";
			default -> super.columnType( sqlTypeCode );
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String castType(int sqlTypeCode) {
		return switch ( sqlTypeCode ) {
			//CUBRID rejects an explicit binary precision on a cast target, e.g. float(53)
			case FLOAT, REAL, DOUBLE -> "double";
			default -> super.castType( sqlTypeCode );
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String castPattern(CastType from, CastType to) {
		//the native temporal-to-string cast is locale-dependent, so render ISO 8601 explicitly
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
	@SPI({ IMPLEMENT, SUPPLY })
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( UUID, "varchar(36)", this ) );
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( JSON, "json", this ) );
		ddlTypeRegistry.addDescriptor( new BinaryFloatCastAsDouble( StandardDdlTypes.binaryFloat( this ) ) );

		//CUBRID has no 'binary' nor 'varbinary', but 'bit' is
		//intended to be used for binary data (unfortunately the
		//length parameter is measured in bits, not bytes)
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( BINARY, "bit($l)", this ) );
		ddlTypeRegistry.addDescriptor(
				StandardDdlTypes.builder( VARBINARY, columnType( BLOB ), this )
						.lobKind( DdlTypeBuilder.LobKind.BIGGEST )
						.withTypeCapacity( getTypeSizingProfile().maxVarbinaryLength(), "bit varying($l)" )
						.build()
		);
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );
		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();

		//the driver has no stream-based LOB binding, so materialize BLOB/CLOB to byte[]/String;
		//CLOB additionally reads back through getClob(), because the driver's getString() skips its
		//wasNull() bookkeeping on the LOB branch and would discard a value read after a null column
		jdbcTypeRegistry.addDescriptor( Types.BLOB, BlobJdbcType.MATERIALIZED );
		jdbcTypeRegistry.addDescriptor( Types.CLOB, CUBRIDClobJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( Types.NCLOB, CUBRIDClobJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( JSON, MySQLJdbcTypes.castingJson() );
		jdbcTypeRegistry.addTypeConstructorIfAbsent( MySQLJdbcTypes.castingJsonArrayConstructor() );

		//CUBRID has no native UUID and its driver cannot bind one to 'bit varying', so store the
		//canonical 36-character text form
		typeContributions.contributeJdbcType( VarcharUUIDJdbcType.INSTANCE );
	}

	/**
	 * Binds a CLOB as a {@code String}, since the CUBRID JDBC driver has no stream-based LOB binding,
	 * but reads it back with {@code getClob()}. The driver's {@code getString()} returns the value
	 * without updating its {@code wasNull} flag when the column is a LOB, so a LOB read after a null
	 * column would be discarded as null.
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

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	protected void contributeKeywords(KeywordRegistration registration) {
		super.contributeKeywords( registration );
		registration.registerKeyword( "ABSOLUTE" );
		registration.registerKeyword( "ACCESS" );
		registration.registerKeyword( "ACTION" );
		registration.registerKeyword( "ADD_MONTHS" );
		registration.registerKeyword( "AFTER" );
		registration.registerKeyword( "ALIAS" );
		registration.registerKeyword( "ASC" );
		registration.registerKeyword( "ASSERTION" );
		registration.registerKeyword( "ATTACH" );
		registration.registerKeyword( "ATTRIBUTE" );
		registration.registerKeyword( "AVG" );
		registration.registerKeyword( "BEFORE" );
		registration.registerKeyword( "BIT" );
		registration.registerKeyword( "BIT_LENGTH" );
		registration.registerKeyword( "BOOLEAN" );
		registration.registerKeyword( "BREADTH" );
		registration.registerKeyword( "CASCADE" );
		registration.registerKeyword( "CATALOG" );
		registration.registerKeyword( "CHANGE" );
		registration.registerKeyword( "CLASS" );
		registration.registerKeyword( "CLASSES" );
		registration.registerKeyword( "COALESCE" );
		registration.registerKeyword( "CONNECTION" );
		registration.registerKeyword( "CONNECT_BY_ISCYCLE" );
		registration.registerKeyword( "CONNECT_BY_ISLEAF" );
		registration.registerKeyword( "CONNECT_BY_ROOT" );
		registration.registerKeyword( "CONSTRAINTS" );
		registration.registerKeyword( "CONVERT" );
		registration.registerKeyword( "COUNT" );
		registration.registerKeyword( "CURRENT_DATETIME" );
		registration.registerKeyword( "DATA" );
		registration.registerKeyword( "DATABASE" );
		registration.registerKeyword( "DATETIME" );
		registration.registerKeyword( "DAY_HOUR" );
		registration.registerKeyword( "DAY_MILLISECOND" );
		registration.registerKeyword( "DAY_MINUTE" );
		registration.registerKeyword( "DAY_SECOND" );
		registration.registerKeyword( "DEFERRABLE" );
		registration.registerKeyword( "DEFERRED" );
		registration.registerKeyword( "DEPTH" );
		registration.registerKeyword( "DESC" );
		registration.registerKeyword( "DESCRIPTOR" );
		registration.registerKeyword( "DIAGNOSTICS" );
		registration.registerKeyword( "DICTIONARY" );
		registration.registerKeyword( "DIFFERENCE" );
		registration.registerKeyword( "DISTINCTROW" );
		registration.registerKeyword( "DIV" );
		registration.registerKeyword( "DOMAIN" );
		registration.registerKeyword( "DUPLICATE" );
		registration.registerKeyword( "ELSEIF" );
		registration.registerKeyword( "EQUALS" );
		registration.registerKeyword( "EVALUATE" );
		registration.registerKeyword( "EXCEPTION" );
		registration.registerKeyword( "EXTRACT" );
		registration.registerKeyword( "FILE" );
		registration.registerKeyword( "FIRST" );
		registration.registerKeyword( "FOUND" );
		registration.registerKeyword( "GENERAL" );
		registration.registerKeyword( "GO" );
		registration.registerKeyword( "GOTO" );
		registration.registerKeyword( "HOUR_MILLISECOND" );
		registration.registerKeyword( "HOUR_MINUTE" );
		registration.registerKeyword( "HOUR_SECOND" );
		registration.registerKeyword( "IGNORE" );
		registration.registerKeyword( "INDEX" );
		registration.registerKeyword( "INHERIT" );
		registration.registerKeyword( "INITIALLY" );
		registration.registerKeyword( "INTERSECTION" );
		registration.registerKeyword( "ISOLATION" );
		registration.registerKeyword( "JSON" );
		registration.registerKeyword( "KEY" );
		registration.registerKeyword( "LAST" );
		registration.registerKeyword( "LESS" );
		registration.registerKeyword( "LEVEL" );
		registration.registerKeyword( "LIMIT" );
		registration.registerKeyword( "LIST" );
		registration.registerKeyword( "LOCAL_TRANSACTION_ID" );
		registration.registerKeyword( "LOWER" );
		registration.registerKeyword( "MAX" );
		registration.registerKeyword( "MILLISECOND" );
		registration.registerKeyword( "MIN" );
		registration.registerKeyword( "MINUTE_MILLISECOND" );
		registration.registerKeyword( "MINUTE_SECOND" );
		registration.registerKeyword( "MOD" );
		registration.registerKeyword( "MODIFY" );
		registration.registerKeyword( "MULTISET_OF" );
		registration.registerKeyword( "NA" );
		registration.registerKeyword( "NAMES" );
		registration.registerKeyword( "NEXT" );
		registration.registerKeyword( "NULLIF" );
		registration.registerKeyword( "OBJECT" );
		registration.registerKeyword( "OCTET_LENGTH" );
		registration.registerKeyword( "OFF" );
		registration.registerKeyword( "OID" );
		registration.registerKeyword( "OPTIMIZATION" );
		registration.registerKeyword( "OPTION" );
		registration.registerKeyword( "PARAMETERS" );
		registration.registerKeyword( "PARTIAL" );
		registration.registerKeyword( "POSITION" );
		registration.registerKeyword( "PRESERVE" );
		registration.registerKeyword( "PRIOR" );
		registration.registerKeyword( "PRIVILEGES" );
		registration.registerKeyword( "QUERY" );
		registration.registerKeyword( "READ" );
		registration.registerKeyword( "RELATIVE" );
		registration.registerKeyword( "RENAME" );
		registration.registerKeyword( "REPLACE" );
		registration.registerKeyword( "RESTRICT" );
		registration.registerKeyword( "ROLE" );
		registration.registerKeyword( "ROUTINE" );
		registration.registerKeyword( "ROWNUM" );
		registration.registerKeyword( "SCHEMA" );
		registration.registerKeyword( "SECOND_MILLISECOND" );
		registration.registerKeyword( "SECTION" );
		registration.registerKeyword( "SEQUENCE" );
		registration.registerKeyword( "SEQUENCE_OF" );
		registration.registerKeyword( "SERIALIZABLE" );
		registration.registerKeyword( "SESSION" );
		registration.registerKeyword( "SESSION_USER" );
		registration.registerKeyword( "SETEQ" );
		registration.registerKeyword( "SET_OF" );
		registration.registerKeyword( "SHARED" );
		registration.registerKeyword( "SIBLINGS" );
		registration.registerKeyword( "SIZE" );
		registration.registerKeyword( "SQLCODE" );
		registration.registerKeyword( "SQLERROR" );
		registration.registerKeyword( "STATISTICS" );
		registration.registerKeyword( "STRING" );
		registration.registerKeyword( "SUBCLASS" );
		registration.registerKeyword( "SUBSET" );
		registration.registerKeyword( "SUBSETEQ" );
		registration.registerKeyword( "SUBSTRING" );
		registration.registerKeyword( "SUM" );
		registration.registerKeyword( "SUPERCLASS" );
		registration.registerKeyword( "SUPERSET" );
		registration.registerKeyword( "SUPERSETEQ" );
		registration.registerKeyword( "SYSDATE" );
		registration.registerKeyword( "SYSDATETIME" );
		registration.registerKeyword( "SYSTIME" );
		registration.registerKeyword( "SYS_CONNECT_BY_PATH" );
		registration.registerKeyword( "SYS_DATE" );
		registration.registerKeyword( "SYS_DATETIME" );
		registration.registerKeyword( "SYS_TIME" );
		registration.registerKeyword( "SYS_TIMESTAMP" );
		registration.registerKeyword( "SYS_USER" );
		registration.registerKeyword( "TEMPORARY" );
		registration.registerKeyword( "TEST" );
		registration.registerKeyword( "TIMEZONE" );
		registration.registerKeyword( "TRANSACTION" );
		registration.registerKeyword( "TRANSLATE" );
		registration.registerKeyword( "TRIM" );
		registration.registerKeyword( "TRUNCATE" );
		registration.registerKeyword( "UNDER" );
		registration.registerKeyword( "UPPER" );
		registration.registerKeyword( "USAGE" );
		registration.registerKeyword( "USE" );
		registration.registerKeyword( "UTIME" );
		registration.registerKeyword( "VARIABLE" );
		registration.registerKeyword( "VCLASS" );
		registration.registerKeyword( "VIEW" );
		registration.registerKeyword( "WORK" );
		registration.registerKeyword( "WRITE" );
		registration.registerKeyword( "XOR" );
		registration.registerKeyword( "YEAR_MONTH" );
		registration.registerKeyword( "ZONE" );
	}

	public CUBRIDDialect(DialectResolutionInfo info) {
		super( info );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void contributeDefaultProperties(java.util.Properties properties) {
		super.contributeDefaultProperties( properties );
		properties.setProperty( STATEMENT_BATCH_SIZE, "15" );
		//LOBs are always materialized, never created through the connection
		properties.setProperty( NON_CONTEXTUAL_LOB_CREATION, "true" );
		//the driver reports support for getGeneratedKeys() but returns a result set whose internal
		//connection and statement are unset, so the identity value is read with a select instead
		properties.setProperty( USE_GET_GENERATED_KEYS, "false" );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public LobSupport getLobSupport() {
		//the CUBRID JDBC driver has no stream-based LOB binding
		return LobSupports.nonStreaming();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public ObjectNullBindingStrategy getObjectNullBindingStrategy() {
		//setNull() ignores the given SQL type, and this avoids the unimplemented getParameterMetaData()
		return ObjectNullBindingStrategy.SET_NULL_WITH_NULL_TYPE;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
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
	@SPI({ IMPLEMENT, SUPPLY })
	public int getPreferredSqlTypeCodeForBoolean() {
		return Types.SMALLINT;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public DirectJavaTimeJdbcSupport getDirectJavaTimeJdbcSupport() {
		//the CUBRID JDBC driver does not implement the JDBC 4.2 java.time binding and extraction
		return DirectJavaTimeJdbcSupports.none();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NationalizationSupport getNationalizationSupport() {
		//CUBRID has no nvarchar/nclob, so nationalized types map to the regular varchar/clob
		return NationalizationSupport.IMPLICIT;
	}

	//not used for anything right now, but it
	//could be used for timestamp literal format
	@Override
	@SPI({ IMPLEMENT, SUPPLY })
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
		//CUBRID's rand() returns an integer; drand() returns a double in [0,1) like HQL rand()
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
		//TODO: CUBRID also has systime()/sysdate() returning TIME/DATE
		functionFactory.localtimeLocaltimestamp();
		functionFactory.hourMinuteSecond();
		functionFactory.yearMonthDay();
		functionFactory.dayofweekmonthyear();
		functionFactory.lastDay();
		functionFactory.weekQuarter();
		//octet_length()/bit_length() only accept character or bit strings, so measure a LOB with clob_length()
		functionFactory.octetLength_pattern( "octet_length(?1)", "clob_length(?1)" );
		functionFactory.bitLength_pattern( "bit_length(?1)", "clob_length(?1)*8" );
		functionFactory.md5();
		//the native trunc() truncates dates only to day granularity, so emulate datetime truncation
		//down to second by formatting via to_char and parsing back with to_datetime
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
		//bit_and/or/xor are aggregates rather than the scalar two-argument form HQL bitand(x,y) needs,
		//and there is no bit_not, so use the &|^~ operators
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
		//CUBRID rejects extract(millisecond from <time>), so handle the TIME case separately
		functionRegistry.register( "extract", new CUBRIDExtractFunction( this, typeConfiguration ) );

		//the base maps local_time to CUBRID's localtime, which is a DATETIME rather than a TIME,
		//so a time=local_time comparison fails; current_time is a real TIME
		functionRegistry.noArgsBuilder( "local_time", "current_time" )
				.setInvariantType( typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.LOCAL_TIME ) )
				.setUseParenthesesWhenNoArgs( false )
				.register();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean supports(org.hibernate.dialect.constraint.spi.CheckConstraintPlacement placement) {
		return false;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		return CUBRIDSequenceSupport.INSTANCE;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String renderDropConstraint(
			org.hibernate.dialect.constraint.spi.ForeignKeyDropRequest request) {
		return switch ( request.ifExistsPlacement() ) {
			case NONE -> "drop foreign key " + request.constraintName();
			case BEFORE_NAME -> "drop foreign key if exists " + request.constraintName();
			case AFTER_NAME -> "drop foreign key " + request.constraintName() + " if exists";
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public org.hibernate.dialect.unique.spi.UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public IndexNameQualification nameQualification() {
		return IndexNameQualification.UNQUALIFIED;
	}

	@Override
	public SubquerySupport getSubquerySupport() {
		return SubquerySupport.builder()
				.feature( SubquerySupport.Feature.EXISTS_IN_SELECT, false )
				.feature( SubquerySupport.Feature.OFFSET, true )
				.feature( SubquerySupport.Feature.LATERAL, true )
				.build();
	}

	private static final SequenceInformationExtractor SEQUENCE_INFORMATION_EXTRACTOR =
			SequenceInformationExtractors.builder( "select * from db_serial" )
					.withoutCatalog()
					.withoutSchema()
					.sequenceNameColumn( "name" )
					.startValueColumn( "started" )
					.minimumValueColumn( "min_val" )
					.maximumValueColumn( "max_val" )
					.incrementValueColumn( "increment_val" )
					.build();

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SEQUENCE_INFORMATION_EXTRACTOR;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
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
	@SPI({ IMPLEMENT, SUPPLY })
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return VIOLATED_CONSTRAINT_NAME_EXTRACTOR;
	}

	//the constraint name is only in the message text, so parse it out by template (English, best-effort)
	private static final ViolatedConstraintNameExtractor VIOLATED_CONSTRAINT_NAME_EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor( sqle -> switch ( extractErrorCode( sqle ) ) {
				case -670, -886, -564 -> extractUsingTemplate( "INDEX ", "(", sqle.getMessage() );
				case -922, -924 -> extractUsingTemplate( "foreign key '", "'", sqle.getMessage() );
				default -> null;
			} );

	private static final LockingSupport LOCKING_SUPPORT = StandardLockingSupports.simple(
			PessimisticLockStyle.CLAUSE,
			RowLockStrategy.NONE,
			LockTimeoutType.NONE,
			OuterJoinLockingType.FULL,
			ConnectionLockTimeoutStrategy.NONE,
			// CUBRID uses multiversion concurrency control
			false
	);

	@Override
	public LockingSupport getLockingSupport() {
		return LOCKING_SUPPORT;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public CurrentTimestampSelection getCurrentTimestampSelection() {
		return CurrentTimestampSelection.prepared( "select now()" );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTimestamp() {
		//current_timestamp is a second-precision TIMESTAMP; sys_datetime is a millisecond-precision
		//DATETIME, matching how TIMESTAMP columns are mapped
		return "sys_datetime";
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized IfExistsSupport getIfExistsSupport() {
		if ( ifExistsSupport == null ) {
			ifExistsSupport = new IfExistsSupport(
				ExistenceCheckPlacement.NONE,
				ExistenceCheckPlacement.BEFORE_NAME,
				ExistenceCheckPlacement.NONE,
				ExistenceCheckPlacement.NONE
		);
		}
		return ifExistsSupport;
	}

	@Override
	public TupleCountSupport getTupleCountSupport() {
		return TupleCountSupport.NONE;
	}

	@Override
	public WindowFunctionSupport getWindowFunctionSupport() {
		// CUBRID has window functions but no 'over' frame clause (rows/range)
		return WindowFunctionSupport.builder()
				.features(
						WindowFunctionSupport.Feature.WINDOW_FUNCTIONS,
						WindowFunctionSupport.Feature.PARTITION_BY
				)
				.build();
	}

	@Override
	public CteSupport getCteSupport() {
		return CteSupport.builder()
				.placement( CteSupport.Placement.SUBQUERY )
				.recursiveFeatures( CteSupport.RecursiveFeature.RECURSIVE )
				.mutationFeatures( CteSupport.MutationFeature.NON_QUERY )
				.build();
	}

	@Override
	public ValuesListSupport getValuesListSupport() {
		return ValuesListSupport.STANDARD;
	}

	//CUBRID cannot change only the column type, so emit the full column definition
	@Override
	@SPI({ USE, IMPLEMENT })
	public String alterColumnType(AlterColumnTypeRequest request) {
		return "modify column " + request.columnName() + " " + request.columnDefinition().trim();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NamespaceSupport getNamespaceSupport() {
		return NamespaceSupports.none();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SchemaNameResolver getSchemaNameResolver() {
		//the CUBRID JDBC driver throws from Connection.getSchema(), which would abort the whole
		//JDBC metadata extraction; CUBRID has no schema qualification anyway
		return (connection, dialect) -> null;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public int getMaxIdentifierLength() {
		//the driver metadata reports 254, but CUBRID rejects a class name over 222 bytes
		return 222;
	}

	@Override
	public NullOrderingSupport getNullOrderingSupport() {
		return NullOrderingSupport.builder( super.getNullOrderingSupport() )
				.defaultOrdering( NullOrdering.SMALLEST )
				.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new CUBRIDSqlAstTranslator<>( request );
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
	@SPI({ USE, IMPLEMENT })
	public void appendFormat(SqlAppender appender, String format) {
		//CUBRID rejects the FM fill-mode modifier
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
				if ( temporalAccessor instanceof ZonedDateTime zonedDateTime ) {
					temporalAccessor = zonedDateTime.toOffsetDateTime();
				}
				appender.appendSql( "datetime '" );
				appendAsTimestampWithMicros(
						appender,
						temporalAccessor,
						getTemporalValueSemantics().supportsLiteralOffset(),
						jdbcTimeZone,
						ZeroOffsetLiteralStyle.NUMERIC_OFFSET
				);
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
				appender.appendSql( "datetime '" );
				appendAsTimestampWithMicros( appender, date, jdbcTimeZone );
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
				appender.appendSql( "datetime '" );
				appendAsTimestampWithMillis( appender, calendar, jdbcTimeZone );
				appender.appendSql( '\'' );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public long fractionalSecondPrecisionInNanos() {
		return 1_000_000; //milliseconds
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendUUIDLiteral(SqlAppender appender, java.util.UUID literal) {
		//CUBRID has no uuid type, so render the text form rather than cast(... as uuid)
		appender.appendSql( '\'' );
		appender.appendSql( literal.toString() );
		appender.appendSql( '\'' );
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
	@SPI({ USE, IMPLEMENT })
	public String extractPattern(TemporalUnit unit) {
		return switch (unit) {
			case SECOND -> "(second(?2)+extract(millisecond from ?2)/1e3)";
			case DAY_OF_WEEK -> "dayofweek(?2)";
			case DAY_OF_MONTH ->"dayofmonth(?2)";
			case DAY_OF_YEAR -> "dayofyear(?2)";
			case WEEK -> "week(?2,3)"; //mode 3 is the ISO week
			//CUBRID has no 'epoch' field
			case EPOCH -> "unix_timestamp(?2)";
			default -> "?1(?2)";
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TimeZoneSupport getTimeZoneSupport() {
		//the CUBRID JDBC driver has no java.time support, so route temporal binding
		//through java.sql.Timestamp by normalizing to the JDBC timezone
		return TimeZoneSupport.NORMALIZE;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		if ( temporalType == TemporalType.TIME ) {
			final String seconds = intervalInSeconds( unit );
			if ( seconds != null ) {
				//adddate() rejects a 'time' operand, so add in seconds and wrap back into a single day
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

	private static String intervalInSeconds(TemporalUnit unit) {
		return switch ( unit ) {
			case NANOSECOND -> "(?2)/1e9";
			case NATIVE -> "(?2)/1e3";
			case SECOND -> "(?2)";
			case MINUTE -> "(?2)*60";
			case HOUR -> "(?2)*3600";
			default -> null;
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		return switch ( unit ) {
			//note: datediff() is backwards on CUBRID
			case DAY -> "datediff(?3,?2)";
			case YEAR -> "(year(?3)-year(?2))";
			case MONTH -> "((year(?3)-year(?2))*12+(month(?3)-month(?2)))";
			case QUARTER -> "(((year(?3)-year(?2))*12+(month(?3)-month(?2)))/3)";
			case WEEK -> "(datediff(?3,?2)/7)";
			case HOUR -> "(" + wholeSecondDiff( fromTemporalType, toTemporalType ) + "/3600)";
			case MINUTE -> "(" + wholeSecondDiff( fromTemporalType, toTemporalType ) + "/60)";
			case SECOND -> wholeSecondDiff( fromTemporalType, toTemporalType );
			//a sub-second difference cannot be computed portably: current_timestamp is a second-precision
			//TIMESTAMP and extract(millisecond) rejects it, so the sub-second digits are always 0 here
			case NATIVE -> "(" + wholeSecondDiff( fromTemporalType, toTemporalType ) + "*1e3)";
			case NANOSECOND -> "(" + wholeSecondDiff( fromTemporalType, toTemporalType ) + "*1e9)";
			default -> throw new SemanticException( "unsupported temporal unit for CUBRID: " + unit );
		};
	}

	/**
	 * Renders the difference in whole seconds between {@code ?2} (from) and {@code ?3} (to) without
	 * {@code timediff()}, which is limited to CUBRID's 24-hour TIME range. The whole-day part comes from
	 * {@code datediff()} and the time-of-day part from {@code time_to_sec()}; each is omitted for an
	 * operand that carries no date (a TIME) or no time (a DATE), since those functions reject such a value.
	 */
	private static String wholeSecondDiff(TemporalType fromTemporalType, TemporalType toTemporalType) {
		final boolean spansWholeDays = fromTemporalType != TemporalType.TIME && toTemporalType != TemporalType.TIME;
		final boolean toHasTimeOfDay = toTemporalType != TemporalType.DATE;
		final boolean fromHasTimeOfDay = fromTemporalType != TemporalType.DATE;
		final StringBuilder pattern = new StringBuilder( "(" );
		String separator = "";
		if ( spansWholeDays ) {
			//note: datediff() is backwards on CUBRID and ignores the time component
			pattern.append( "datediff(?3,?2)*86400" );
			separator = "+";
		}
		if ( toHasTimeOfDay ) {
			pattern.append( separator ).append( "time_to_sec(?3)" );
		}
		if ( fromHasTimeOfDay ) {
			if ( pattern.length() == 1 ) {
				pattern.append( "0" );
			}
			pattern.append( "-time_to_sec(?2)" );
		}
		return pattern.append( ")" ).toString();
	}

	@Override
	public SingleRowTableSupport getSingleRowTableSupport() {
		//TODO: is this really needed?
		//TODO: would "from table({0})" be better?
		final String tableExpression = "db_root";
		return SingleRowTableSupport.builder( super.getSingleRowTableSupport() )
				.tableExpression( tableExpression )
				.selectOnlyFromClause( " from " + tableExpression )
				.build();
	}

	private final SizeStrategy sizeStrategy = new StandardSizeStrategy( this ) {
		@Override
		public Size resolveSize(
				JdbcType jdbcType,
				JavaType<?> javaType,
				Integer precision,
				Integer scale,
				Long length) {
			final Size size = super.resolveSize( jdbcType, javaType, precision, scale, length );
			//CUBRID measures 'bit'/'bit varying' length in bits, so scale the byte length up to bits
			final int ddlTypeCode = jdbcType.getDdlTypeCode();
			if ( ( ddlTypeCode == BINARY || ddlTypeCode == VARBINARY || ddlTypeCode == LONGVARBINARY )
					&& size.getLength() != null ) {
				size.setLength( size.getLength() * 8 );
			}
			return size;
		}
	};

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SizeStrategy getSizeStrategy() {
		return sizeStrategy;
	}

	/**
	 * Keeps the binary-to-decimal precision conversion of the standard binary float descriptor for DDL,
	 * but casts to {@code double}, since CUBRID rejects a large explicit precision such as {@code float(53)}.
	 */
	private record BinaryFloatCastAsDouble(DdlType delegate) implements DdlType {
		@Override
		public int getSqlTypeCode() {
			return delegate.getSqlTypeCode();
		}

		@Override
		public String getTypeName(Size columnSize, Type type, DdlTypeRegistry ddlTypeRegistry) {
			return delegate.getTypeName( columnSize, type, ddlTypeRegistry );
		}

		@Override
		public String getCastTypeName(Size columnSize, SqlExpressible type, DdlTypeRegistry ddlTypeRegistry) {
			return "double";
		}

		@Override
		public boolean isLob(Size size) {
			return delegate.isLob( size );
		}

		@Override
		public String[] getRawTypeNames() {
			return delegate.getRawTypeNames();
		}
	}

	@Override
	public RowValueSupport getRowValueSupport() {
		//before 11.0 CUBRID parses (a, b) as a collection literal rather than a row value
		return getVersion().isSameOrAfter( 11, 0 )
				? RowValueSupport.builder( super.getRowValueSupport() )
						.feature( RowValueSupport.Feature.QUANTIFIED_COMPARISON, false )
						.build()
				: RowValueSupport.NONE;
	}

	@Override
	public MutationSyntaxSupport getMutationSyntaxSupport() {
		return MutationSyntaxSupport.builder()
				.capability( MutationKind.UPDATE, MutationSyntaxCapability.FROM_CLAUSE )
				.capability( MutationKind.DELETE, MutationSyntaxCapability.JOIN )
				.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		//a joined DELETE/UPDATE requires the table alias to qualify columns
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuildRequest request) {
		final var builder = request.builder();
		//must precede super: applyReservedWords() silently discards every word while this flag is false,
		//and the caller seeds it from a setting that defaults to false
		builder.setAutoQuoteKeywords( true );
		builder.setAutoQuoteDollar( true );
		super.buildIdentifierHelper( request );

		//must follow super: super initializes both strategies from the JDBC metadata, and the casing
		//CUBRID's DatabaseMetaData reports does not match how it actually stores identifiers
		builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.LOWER );
		builder.setQuotedCaseStrategy( IdentifierCaseStrategy.LOWER );

		return builder.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NameQualifierSupport getNameQualifierSupport() {
		return NameQualifierSupport.NONE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public ParameterLimits getParameterLimits() {
		//CUBRID rejects an expression nested beyond 400 levels. Before 11.0 there is no row value
		//constructor, so core emulates a multi-key batch as one 'or' per key tuple; this bound
		//keeps a two-column key at 250 tuples, safely under the limit
		return ParameterLimits.of( 500 );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalValueSemantics getTemporalValueSemantics() {
		//CUBRID truncates the sub-millisecond part rather than rounding it
		return TemporalValueSemantics.TRUNCATING;
	}

	@Override
	public boolean causesRollback(SQLException sqlException) {
		// ER_LK_UNILATERALLY_ABORTED identifies the transaction chosen as a deadlock victim.
		return extractErrorCode( sqlException ) == -72;
	}

}
