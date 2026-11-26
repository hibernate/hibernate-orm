package org.hibernate.community.dialect;

import java.sql.SQLException;

import org.hibernate.dialect.temporaltype.spi.TemporalValueSemantics;

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
import org.hibernate.dialect.type.spi.EnumSupport;
import org.hibernate.dialect.type.spi.EnumSupports;
import org.hibernate.dialect.type.spi.ObjectNullBindingStrategy;
import org.hibernate.dialect.type.spi.UserDefinedTypeDdlSupport;
import org.hibernate.dialect.lob.spi.LobSupport;
import org.hibernate.dialect.lob.spi.LobSupports;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;

import org.hibernate.dialect.mutation.spi.MultiTableMutationSupport;
import org.hibernate.dialect.array.spi.ArraySupport;
import org.hibernate.dialect.function.spi.TupleCountSupport;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;
import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.MutationKind;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxCapability;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxSupport;
import org.hibernate.dialect.sql.ast.spi.SingleRowTableSupport;
import org.hibernate.dialect.sql.ast.spi.PredicateSupport;
import org.hibernate.dialect.sql.ast.spi.ValuesListSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;

import jakarta.persistence.GenerationType;
import jakarta.persistence.TemporalType;
import jakarta.annotation.Nullable;
import org.hibernate.Length;
import org.hibernate.QueryTimeoutException;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.community.dialect.internal.GaussDBRefCursorSupportFactory;
import org.hibernate.community.dialect.identity.internal.GaussDBIdentityColumnSupport;
import org.hibernate.community.dialect.lock.internal.GaussDBLockingSupport;
import org.hibernate.community.dialect.sequence.GaussDBMModeSequenceInformationExtractor;
import org.hibernate.community.dialect.sequence.GaussDBMModeSequenceSupport;
import org.hibernate.community.dialect.sequence.GaussDBSequenceSupport;
import org.hibernate.dialect.type.spi.BooleanDecoder;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.sql.ast.spi.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.aggregate.internal.AggregateSupportImpl;
import org.hibernate.dialect.aggregate.spi.FunctionalDependencyAnalysisSupport;
import org.hibernate.dialect.aggregate.spi.AggregateSupport;
import org.hibernate.dialect.type.spi.NationalizationSupport;
import org.hibernate.dialect.type.spi.TimeZoneSupport;
import org.hibernate.dialect.type.spi.TypeSizingProfile;
import org.hibernate.dialect.identity.internal.MySQLIdentityColumnSupport;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.generated.spi.GeneratedValuesSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.mutation.spi.MultiTableMutationSupport;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.LimitLimitHandler;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.schema.spi.AlterColumnTypeRequest;
import org.hibernate.dialect.schema.spi.ConstraintDropMode;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.schema.spi.IfExistsSupport;
import org.hibernate.dialect.queryhint.spi.QueryHintPlacement;
import org.hibernate.dialect.schema.spi.IndexNameQualification;
import org.hibernate.dialect.schema.spi.SchemaDropSupport;
import org.hibernate.dialect.schema.spi.TruncateMode;
import org.hibernate.dialect.schema.spi.TruncateRequest;
import org.hibernate.dialect.temptable.spi.StandardLocalTemporaryTableStrategy;
import org.hibernate.dialect.temptable.spi.TemporaryTableStrategy;
import org.hibernate.dialect.unique.spi.UniqueDelegates;
import org.hibernate.dialect.unique.spi.UniqueDelegate;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupportFactory;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupports;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.dialect.identifier.spi.IdentifierHelperBuildRequest;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.jdbc.spi.JdbcExceptionHelper;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UserDefinedType;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.procedure.spi.CallableStatementSupports;
import org.hibernate.query.SemanticException;
import org.hibernate.dialect.sql.ast.spi.FetchClauseSupport;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.CastType;
import org.hibernate.dialect.temporaltype.spi.IntervalType;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.sql.spi.ParameterMarkerStrategy;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.dialect.literal.spi.DelegatingLiteralSupport;
import org.hibernate.dialect.literal.spi.LiteralSupport;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.dialect.sql.ast.spi.OptionalTableUpdateOperationRequest;
import org.hibernate.sql.ast.spi.model.OptionalTableUpdate;
import org.hibernate.sql.spi.mutation.jdbc.OptionalTableUpdateOperation;
import org.hibernate.sql.spi.mutation.jdbc.OptionalTableUpdateWithUpsertOperation;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.InformationExtractor;
import org.hibernate.tool.schema.extract.spi.InformationExtractors;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractors;
import org.hibernate.tool.schema.spi.StandardTableExporter;
import org.hibernate.tool.schema.spi.StandardUserDefinedTypeExporter;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.BlobJdbcType;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.ObjectNullAsBinaryTypeJdbcType;
import org.hibernate.type.descriptor.jdbc.SqlTypedJdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharUUIDJdbcType;
import org.hibernate.type.descriptor.jdbc.XmlJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.Types;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.internal.util.StringHelper.unroot;
import static org.hibernate.query.common.TemporalUnit.EPOCH;
import static org.hibernate.type.SqlTypes.ARRAY;
import static org.hibernate.type.SqlTypes.BIGINT;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BIT;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.DOUBLE;
import static org.hibernate.type.SqlTypes.FLOAT;
import static org.hibernate.type.SqlTypes.GEOGRAPHY;
import static org.hibernate.type.SqlTypes.GEOMETRY;
import static org.hibernate.type.SqlTypes.INET;
import static org.hibernate.type.SqlTypes.INTEGER;
import static org.hibernate.type.SqlTypes.INTERVAL_SECOND;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.LONG32NVARCHAR;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.LONG32VARCHAR;
import static org.hibernate.type.SqlTypes.NCHAR;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.NUMERIC;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.DECIMAL;
import static org.hibernate.type.SqlTypes.NVARCHAR;
import static org.hibernate.type.SqlTypes.OTHER;
import static org.hibernate.type.SqlTypes.REAL;
import static org.hibernate.type.SqlTypes.SMALLINT;
import static org.hibernate.type.SqlTypes.SQLXML;
import static org.hibernate.type.SqlTypes.STRUCT;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_UTC;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_UTC;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.UUID;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.SqlTypes.VARCHAR;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsDate;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsLocalTime;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTime;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTimestampWithMicros;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTimestampWithMillis;

/**
 * A {@linkplain Dialect SQL dialect} for GaussDB V2.0-8.201 and above.
 * <p>
 * Please refer to the
 * <a href="https://support.huaweicloud.com/function-gaussdb/index.html">GaussDB documentation</a>.
 *
 * @author liubao
 * @author chen zhida
 *
 * Notes: Original code of this class is based on PostgreSQLDialect.
 */
public class GaussDBDialect extends Dialect implements CurrentTemporalSupport, TemporalFormatSupport, TemporalOperationSupport {
	private IfExistsSupport ifExistsSupport;
	private SchemaDropSupport schemaDropSupport;

	/**
	 * Configuration key to explicitly set the GaussDB compatibility mode (e.g. {@code =M}). Used when
	 * JDBC metadata is unavailable on boot ({@code hibernate.temp.use_jdbc_metadata_defaults=false}),
	 * where the {@code datcompatibility} probe cannot run; the gaussdb test profile sets it explicitly.
	 */
	public final static String GAUSSDB_COMPATIBILITY_MODE = "hibernate.dialect.gaussdb.compatibility_mode";

	// The compatibility mode of the target database (pg_database.datcompatibility), determined by
	// GaussDBServerConfiguration.fromDialectResolutionInfo() and read into this field by the constructor.
	private final GaussDBServerConfiguration serverConfiguration;

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

	private final TypeSizingProfile aModeTypeSizingProfile = TypeSizingProfile.builder( super.getTypeSizingProfile() )
			.defaultIntervalSecondScale( 6 )
			.maxVarcharLength( 10_485_760 ).maxVarcharCapacity( 1_073_741_727 )
			.maxNVarcharLength( 10_485_760 ).maxNVarcharCapacity( 10_485_760 )
			.maxVarbinaryLength( Length.LONG32 ).maxVarbinaryCapacity( Length.LONG32 )
			.build();

	private final TypeSizingProfile mModeTypeSizingProfile = TypeSizingProfile.builder( super.getTypeSizingProfile() )
			.defaultIntervalSecondScale( 6 )
			.maxVarcharLength( 16_383 ).maxVarcharCapacity( 1_073_741_727 )
			.maxNVarcharLength( 10_485_760 ).maxNVarcharCapacity( 10_485_760 )
			.maxVarbinaryLength( 65_535 ).maxVarbinaryCapacity( Length.LONG32 )
			.build();

	@Override
	public TypeSizingProfile getTypeSizingProfile() {
		// M mode (MySQL-compatible) caps varchar at 16383 bytes and varbinary at 65535 (like MySQL);
		// longer strings must use text (the LONG32VARCHAR fallback maps to "text") and longer byte[]
		// upgrades to LONG32VARBINARY (longblob). A mode (Oracle-compatible) supports the larger
		// 10MB varchar and unbounded bytea.
		return isMMode() ? mModeTypeSizingProfile : aModeTypeSizingProfile;
	}

	private static final ArraySupport ARRAY_SUPPORT = ArraySupport.builder()
			.capabilities( ArraySupport.Capability.STANDARD_ARRAY )
			.multiValuedParameterStrategy( ArraySupport.MultiValuedParameterStrategy.ARRAY )
			.build();

	protected final static DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 2 );
	private static final CallableStatementSupport CALLABLE_STATEMENT_SUPPORT =
			CallableStatementSupports.builder()
					.supportsRefCursors( true )
					.namedParameterRenderer( (sqlAppender, parameterName) -> {
						sqlAppender.appendSql( parameterName );
						sqlAppender.appendSql( " => ?" );
					} )
					.build();
	private static final RefCursorSupportFactory REF_CURSOR_SUPPORT_FACTORY =
			RefCursorSupports.metadataSelected( GaussDBRefCursorSupportFactory.INSTANCE );

	private final UniqueDelegate uniqueDelegate = UniqueDelegates.createTable( this );
	private final StandardTableExporter gaussDBTableExporter = new StandardTableExporter( this ) {
		@Override
		protected void applyAggregateColumnCheck(StringBuilder buf, AggregateColumn aggregateColumn) {
			final JdbcType jdbcType = aggregateColumn.getType().getJdbcType();
			if ( jdbcType.isXml() ) {
				// Requires the use of xmltable which is not supported in check constraints
				return;
			}
			super.applyAggregateColumnCheck( buf, aggregateColumn );
		}
	};
	private final Exporter<UserDefinedType> userDefinedTypeExporter = new StandardUserDefinedTypeExporter(
			this,
			new UserDefinedTypeDdlSupport( "", "", ExistenceCheckPlacement.BEFORE_NAME )
	);

	public GaussDBDialect() {
		this(MINIMUM_VERSION);
	}

	public GaussDBDialect(DialectResolutionInfo info) {
		this( info, GaussDBServerConfiguration.fromDialectResolutionInfo( info ) );
	}

	public GaussDBDialect(DialectResolutionInfo info, GaussDBServerConfiguration serverConfiguration) {
		this( info.makeCopyOrDefault( MINIMUM_VERSION ), serverConfiguration );
	}

	public GaussDBDialect(DatabaseVersion version) {
		this( version, GaussDBServerConfiguration.DEFAULT );
	}

	public GaussDBDialect(DatabaseVersion version, GaussDBServerConfiguration serverConfiguration) {
		super( version );
		this.serverConfiguration = serverConfiguration;
	}

	@Override
	protected void contributeKeywords(org.hibernate.dialect.identifier.spi.KeywordRegistration registration) {
		super.contributeKeywords( registration );
		// M mode reserves `excluded` as a keyword (PostgreSQL treats it as non-reserved, usable as a column name),
		// so identifiers named "excluded" must be quoted. GaussDB upserts use ON DUPLICATE KEY (not ON CONFLICT's
		// EXCLUDED alias), so quoting the identifier is safe. `match` is a MySQL reserved word (M mode) but
		// non-reserved in PostgreSQL, so an unquoted `create table Match (...)` raises a syntax error in M mode;
		// it is registered only for M mode so A-mode behavior is unchanged.
		registration.registerKeyword( "excluded" );
		if ( isMMode() ) {
			registration.registerKeyword( "match" );
		}
	}

	/**
	 * Whether the target database runs in MySQL-compatible mode (datcompatibility "B" or "M").
	 * <p>
	 * {@code serverConfiguration} is assigned only after {@code super()} construction, during which
	 * this method can already be called (e.g. from {@link #getDefaultUseGetGeneratedKeys()} via
	 * {@code initDefaultProperties()}); treat that phase as A mode, matching the historical default,
	 * and re-run {@code initDefaultProperties()} once the real mode is known.
	 */
	public boolean isMMode() {
		return serverConfiguration != null && serverConfiguration.getCompatibilityMode().isMySQLCompatible();
	}

	/**
	 *
	 * M mode (MySQL-compatible) treats double quotes as string literals, not identifier quoting, so
	 * {@code create table "User" (...)} raises a syntax error. Identifiers must be quoted with
	 * backticks (`` ` ``), matching {@link org.hibernate.dialect.MySQLDialect}. A mode keeps the
	 * PostgreSQL-style double quote. This is the single entry point for all identifier quoting
	 * (explicit {@code \"name\"} in mappings, auto-quoting of reserved keywords, DDL/SQL generation),
	 * so overriding it covers every code path (see {@code Dialect#toQuotedIdentifier},
	 * {@code Column#getQuotedName}, {@code sql.Template}).
	 */
	@Override
	public char openQuote() {
		return isMMode() ? '`' : '"';
	}

	@Override
	public char closeQuote() {
		return isMMode() ? '`' : '"';
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
		properties.setProperty( org.hibernate.cfg.AvailableSettings.NON_CONTEXTUAL_LOB_CREATION, "true" );
		properties.setProperty( org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE, "15" );
		if ( isMMode() ) {
			// M mode: the gsjdbc4 (PostgreSQL) driver implements getGeneratedKeys() by
			// rewriting the INSERT with a RETURNING clause, which single-node GaussDB M mode
			// rejects. Disable it so IdentityGenerator falls back to BasicSelectingDelegate,
			// which runs a plain INSERT then `select last_insert_id()` (verified). A mode
			// keeps the default true (RETURNING works on the openGauss PG kernel).
			properties.setProperty( org.hibernate.cfg.AvailableSettings.USE_GET_GENERATED_KEYS, "false" );
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		return switch (sqlTypeCode) {
			// no tinyint
			case TINYINT -> "smallint";

			// M mode (MySQL-compatible) CHAR strips trailing spaces on retrieval (MySQL CHAR
			// semantics), so a single space ' ' stored in char(1) reads back as '' — breaking
			// CharacterTypeTest which expects the space preserved. Use varchar($l), which retains
			// trailing spaces. A mode (Oracle-compatible) keeps the native char type.
			case CHAR -> isMMode() ? "varchar($l)" : super.columnType( CHAR );

			// there are no nchar/nvarchar types
			case NCHAR -> columnType( CHAR );
			case NVARCHAR -> columnType( VARCHAR );

			// M mode (MySQL-compatible) TEXT tops out at 65535 bytes; LONG32/CLOB values (e.g. 240k+
			// chars) overflow. Use `longtext` (4GB) like MySQLDialect. A mode keeps `text` (unbounded).
			case LONG32VARCHAR, LONG32NVARCHAR -> isMMode() ? "longtext" : "text";
			// M mode (MySQL-compatible) has no `clob` type (syntax error at "clob"); use `longtext`.
			// A mode (Oracle-compatible) keeps `clob`.
			case NCLOB -> isMMode() ? "longtext" : "clob";
			case CLOB -> isMMode() ? "longtext" : super.columnType( CLOB );

			// M mode (MySQL-compatible) rejects `bytea`; use binary/varbinary/blob.
			// `binary` without a length defaults to 1 byte in M mode, so BINARY must carry $l
			// (e.g. a UUID stored as BINARY is 16 bytes and would otherwise exceed `binary(1)`).
			// A mode (Oracle-compatible) keeps `bytea`.
			case BINARY -> isMMode() ? "binary($l)" : "bytea";
			case VARBINARY -> isMMode() ? "varbinary($l)" : "bytea";
			case LONG32VARBINARY -> isMMode() ? "longblob" : "bytea"; // longblob (4GB): M mode blob is 65535, LONG32 byte[] (240k+) overflows

			// M mode (MySQL-compatible) has no native `uuid` column type (syntax error on `theuuid uuid`),
			// so map UUID to `varchar(36)` and pair it with VarcharUUIDJdbcType (read/write UUID as varchar).
			// A mode (Oracle-compatible) keeps the native `uuid` type via GaussDBUUIDJdbcType.
			case UUID -> isMMode() ? "varchar(36)" : super.columnType( UUID );

			// M mode has no native `inet` (see registerColumnTypes); keep columnType consistent so
			// sized casts / fallbacks render `varchar(45)` too. A mode keeps the default `inet`.
			case INET -> isMMode() ? "varchar(45)" : super.columnType( INET );

			case TIMESTAMP_UTC -> columnType( TIMESTAMP_WITH_TIMEZONE );

			// M mode: the `timestamp` type rejects dates at or before the epoch (1970-01-01),
			// so map TIMESTAMP to `datetime` (range 1000-9999), matching MySQLDialect. A mode
			// keeps the default `timestamp` (Oracle-compatible, wide range).
			case TIMESTAMP -> isMMode() ? "datetime($p)" : super.columnType( TIMESTAMP );

			// M mode (MySQL-compatible) has no timezone-aware timestamp type: both `timestamptz`
			// and `timestamp with time zone` are syntax errors. Map WITH_TIMEZONE to `datetime`
			// too, relying on the JDBC layer for timezone conversion (as MySQLDialect does).
			case TIMESTAMP_WITH_TIMEZONE -> isMMode() ? "datetime($p)" : super.columnType( TIMESTAMP_WITH_TIMEZONE );

			// M mode (MySQL-compatible) has no `time with time zone` type (syntax error on CREATE TABLE);
			// map TIME_WITH_TIMEZONE to plain `time`, matching MySQLDialect (which also lacks a
			// timezone-aware time type). A mode keeps the default `time with time zone`.
			case TIME_WITH_TIMEZONE -> isMMode() ? "time" : super.columnType( TIME_WITH_TIMEZONE );

			default -> super.columnType( sqlTypeCode );
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String castType(int sqlTypeCode) {
		if ( isMMode() ) {
			// M mode (MySQL-compatible) CAST only accepts MySQL type names (char/signed/unsigned/
			// binary/decimal/datetime/...); PostgreSQL names (varchar/text/integer/bigint/numeric/
			// timestamp/...) are syntax errors in CAST. Sized casts (varchar(15)/numeric(38,2)) must
			// use a $l/$p/$s pattern, otherwise DdlTypeImpl.getCastTypeName falls back to the
			// columnType (varchar($l)/numeric($p,$s)) which M mode rejects; types without a default
			// length (char/clob/text) stay static so the no-size path doesn't leave $l unsubstituted.
			return switch (sqlTypeCode) {
				case BOOLEAN, BIT -> "unsigned";
				case TINYINT, SMALLINT, INTEGER, BIGINT -> "signed";
				case FLOAT, REAL, DOUBLE -> "double";
				case NUMERIC, DECIMAL -> "decimal($p,$s)";
				case TIMESTAMP, TIMESTAMP_WITH_TIMEZONE -> "datetime";
				case VARCHAR, NVARCHAR, CHAR, NCHAR -> "char($l)";
				// CHAR/NCHAR use "char($l)" (not static "char") so sized casts resolve via
				// castType to `char(N)`. DdlTypeImpl forces a Character cast to length=1, and a
				// static castType would fall back to columnType — which is `varchar($l)` in M mode
				// (to preserve trailing spaces for CharacterTypeTest) — producing `cast(x as varchar(1))`,
				// a syntax error (M mode CAST only accepts `char`, not `varchar`).
				case LONG32VARCHAR, LONG32NVARCHAR, CLOB, NCLOB -> "char";
				case UUID, INET -> "char";
				case VARBINARY -> "binary($l)";
				case BINARY, LONG32VARBINARY, BLOB -> "binary";
				default -> super.castType( sqlTypeCode );
			};
		}
		return switch (sqlTypeCode) {
			case CHAR, NCHAR, VARCHAR, NVARCHAR -> "varchar";
			case LONG32VARCHAR, LONG32NVARCHAR -> "text";
			case NCLOB -> "clob";
			case CLOB -> super.castType( CLOB );
			case BINARY, VARBINARY, LONG32VARBINARY -> "bytea";
			default -> super.castType( sqlTypeCode );
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		// We need to configure that the array type uses the raw element type for casts
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.standardArray( this, true ) );

		// Register this type to be able to support Float[]
		// The issue is that the JDBC driver can't handle createArrayOf( "float(24)", ... )
		// It requires the use of "real" or "float4"
		// Alternatively we could introduce a new API in Dialect for creating such base names
		ddlTypeRegistry.addDescriptor(
				StandardDdlTypes.builder( FLOAT, columnType( FLOAT ), this ).castTypeName( castType( FLOAT ) )
						.withTypeCapacity( 24, "float4" )
						.build()
		);

		// M mode (MySQL-compatible) has no native `xml` type (syntax error on `x xml`); store XML as `text`.
		// A mode (openGauss PG kernel) keeps `xml`.
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( SQLXML, isMMode() ? "text" : "xml", this ) );
		// M mode (MySQL-compatible) has no native `uuid` type (syntax error on `theuuid uuid`);
		// store UUID as `varchar(36)` paired with VarcharUUIDJdbcType. A mode keeps `uuid`.
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( UUID, isMMode() ? "varchar(36)" : "uuid", this ) );
		// M mode (MySQL-compatible) has no native `inet` type (syntax error on `x inet`); store IPv4/IPv6
		// addresses as `varchar(45)` (PG inet is text-based, GaussDBCastingInetJdbcType binds/reads strings).
		// A mode (openGauss PG kernel) keeps `inet`.
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( INET, isMMode() ? "varchar(45)" : "inet", this ) );
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( GEOMETRY, "geometry", this ) );
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( GEOGRAPHY, "geography", this ) );
		if ( isMMode() ) {
			// M mode (MySQL-compatible) has no native interval column type (syntax error on
			// `interval second(6)`); store Duration seconds as numeric($p,$s) instead.
			// A mode (openGauss PG kernel) keeps the native `interval second($s)` type.
			ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( INTERVAL_SECOND, "numeric($p,$s)", this ) );
		}
		else {
			ddlTypeRegistry.addDescriptor( StandardDdlTypes.scale6IntervalSecond( this ) );
		}

		// GaussDB in MySQL-compatibility (M) mode does not support creating `jsonb` tables
		// (syntax error at "JSONB"), so use the `json` type instead
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( JSON, "json", this ) );

		ddlTypeRegistry.addDescriptor( StandardDdlTypes.namedNativeEnum() );
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.namedNativeOrdinalEnum() );
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
			case OTHER:
				switch ( columnTypeName ) {
					case "uuid":
						jdbcTypeCode = UUID;
						break;
					case "json":
					case "jsonb":
						jdbcTypeCode = JSON;
						break;
					case "xml":
						jdbcTypeCode = SQLXML;
						break;
					case "inet":
						jdbcTypeCode = INET;
						break;
					case "geometry":
						jdbcTypeCode = GEOMETRY;
						break;
					case "geography":
						jdbcTypeCode = GEOGRAPHY;
						break;
				}
				break;
			case TIME:
				// The GaussDB JDBC driver reports TIME for timetz, but we use it only for mapping OffsetTime to UTC
				if ( "timetz".equals( columnTypeName ) ) {
					jdbcTypeCode = TIME_UTC;
				}
				break;
			case TIMESTAMP:
				// The GaussDB JDBC driver reports TIMESTAMP for timestamptz, but we use it only for mapping Instant
				if ( "timestamptz".equals( columnTypeName ) ) {
					jdbcTypeCode = TIMESTAMP_UTC;
				}
				break;
			case ARRAY:
				// GaussDB names array types by prepending an underscore to the base name
				if ( columnTypeName.charAt( 0 ) == '_' ) {
					final String componentTypeName = columnTypeName.substring( 1 );
					final Integer sqlTypeCode = resolveSqlTypeCode( componentTypeName, jdbcTypeRegistry.getTypeConfiguration() );
					if ( sqlTypeCode != null ) {
						return jdbcTypeRegistry.resolveTypeConstructorDescriptor(
								jdbcTypeCode,
								jdbcTypeRegistry.getDescriptor( sqlTypeCode ),
								ColumnTypeInformation.EMPTY
						);
					}
					final SqlTypedJdbcType elementDescriptor = jdbcTypeRegistry.findSqlTypedDescriptor( componentTypeName );
					if ( elementDescriptor != null ) {
						return jdbcTypeRegistry.resolveTypeConstructorDescriptor(
								jdbcTypeCode,
								elementDescriptor,
								ColumnTypeInformation.EMPTY
						);
					}
				}
				break;
			case STRUCT:
				final var descriptor = jdbcTypeRegistry.findSqlTypedDescriptor(
						// Skip the schema
						unroot( columnTypeName )
				);
				if ( descriptor != null ) {
					return descriptor;
				}
				break;
		}
		return jdbcTypeRegistry.getDescriptor( jdbcTypeCode );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected Integer resolveSqlTypeCode(String columnTypeName, TypeConfiguration typeConfiguration) {
		return switch (columnTypeName) {
			case "bool" -> Types.BOOLEAN;
			case "float4" -> Types.REAL; // Use REAL instead of FLOAT to get Float as recommended Java type
			case "float8" -> Types.DOUBLE;
			case "int2" -> Types.SMALLINT;
			case "int4" -> Types.INTEGER;
			case "int8" -> Types.BIGINT;
			default -> super.resolveSqlTypeCode( columnTypeName, typeConfiguration );
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public EnumSupport getEnumSupport() {
		return EnumSupports.postgresql();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentDate() {
		if ( isMMode() ) {
			// M mode (MySQL kernel): `current_date` is already a pure `date` type carrying no
			// time-of-day (e.g. `2026-07-16`), so it needs no truncation. `trunc(current_date)`
			// is MySQL-incompatible here and returns the zero date `0000-00-00 00:00:00`, which
			// breaks date comparisons such as HQL `local date > all elements(p.repairTimestamps)`.
			return "current_date";
		}
		// A mode (Oracle-compatible, openGauss PG kernel): current_date may carry time-of-day,
		// so truncate to the day for date arithmetic (e.g. `date_trunc('day', localtimestamp) - current_date`).
		return "trunc(current_date)";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTime() {
		return "localtime";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTimestamp() {
		if ( isMMode() ) {
			// M mode (MySQL kernel): bare `localtimestamp` is second-precision, so two timestamps
			// produced within the same second compare equal. @CurrentTimestamp/@CreationTimestamp/
			// @UpdateTimestamp tests that update an entity ~10ms apart then fail, even though
			// getTypeSizingProfile().defaultTimestampPrecision()==6 advertises microsecond precision (via the
			// CurrentTimestampHasMicrosecondPrecision feature check). Emit `now(6)` to match the
			// datetime($p) column type and yield microsecond precision. A mode (openGauss PG kernel)
			// keeps bare `localtimestamp` (microsecond by default) — zero regression.
			return "now(" + getTypeSizingProfile().defaultTimestampPrecision() + ")";
		}
		return "localtimestamp";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTimestampWithTimeZone() {
		if ( isMMode() ) {
			// M mode (MySQL kernel): bare `current_timestamp` is second-precision — same issue as
			// currentTimestamp() above. The `instant` HQL function (= currentTimestampWithTimeZone) fills
			// @Temporal temporal-table effective_from/effective_to; second-precision makes two writes within
			// the same second compare equal, breaking asOf(instant) (the strict `effective_to > instant`
			// filter excludes the historical row, so the current row is returned instead). Emit `now(6)`
			// for microsecond precision, matching currentTimestamp() and the datetime($p) column type.
			// A mode (openGauss PG kernel) keeps bare `current_timestamp` (timestamptz, microsecond by
			// default) — zero regression.
			return "now(" + getTypeSizingProfile().defaultTimestampPrecision() + ")";
		}
		return "current_timestamp";
	}

	@Override
	public void appendDefinition(
			org.hibernate.sql.spi.SqlAppender appender,
			org.hibernate.dialect.schema.spi.ColumnDefinitionRequest request) {
		if ( isMMode() ) {
			// M mode requires a datetime(N) column's current-timestamp default to carry matching
			// precision (N); the bare CURRENT_TIMESTAMP that @ColumnDefault produces is rejected
			// ("The default value of the ... type is invalid"). now(getTypeSizingProfile().defaultTimestampPrecision())
			// matches the datetime($p) columnType, whose $p defaults to the same precision (6).
			// A mode keeps the base rendering.
			final String defaultExpression = request.defaultExpression();
			if ( defaultExpression != null ) {
				final String upper = defaultExpression.trim().toUpperCase( Locale.ROOT );
				if ( upper.equals( "CURRENT_TIMESTAMP" ) || upper.equals( "CURRENT_TIMESTAMP()" )
						|| upper.equals( "LOCALTIMESTAMP" ) || upper.equals( "LOCALTIMESTAMP()" )
						|| upper.equals( "NOW" ) || upper.equals( "NOW()" ) ) {
					request = new org.hibernate.dialect.schema.spi.ColumnDefinitionRequest(
							request.sqlType(),
							request.renderedCollation(),
							request.nullable(),
							"now(" + getTypeSizingProfile().defaultTimestampPrecision() + ")",
							request.generatedExpression()
					);
				}
			}
		}
		super.appendDefinition( appender, request );
	}

	@Override
	public boolean supportsUserDefinedTypes() {
		// A mode (openGauss PG kernel) supports PostgreSQL-style composite types (`create type ... as (...)`)
		// for @Struct mapping; GaussDBStructuredJdbcType handles the read/write. The Dialect base default is
		// false, so override to true for A mode. M mode (MySQL-compatible) rejects CREATE TYPE / DROP TYPE
		// (syntax error at "type"), so disable @Struct mapping there — the schema generator then skips the
		// `create type` DDL that would otherwise fail.
		return !isMMode();
	}

	/**
	 * The {@code extract()} function returns {@link TemporalUnit#DAY_OF_WEEK}
	 * numbered from 0 to 6. This isn't consistent with what most other
	 * databases do, so here we adjust the result by generating
	 * {@code (extract(dow,arg)+1))}.
	 */
	@Override
	@SPI({ USE, IMPLEMENT })
	public String extractPattern(TemporalUnit unit) {
		if ( isMMode() ) {
			// M mode (MySQL-compatible) extract() only supports year/month/day/quarter/week/hour/
			// minute/second; dow/doy/epoch raise "units does not supported" regardless of source type.
			// Mirror MySQLDialect: use the native MySQL functions (dayofweek/dayofyear/unix_timestamp/
			// ...) which accept a DATE source directly, so GaussDBExtractFunction need not cast to
			// timestamp (and `cast(?2 as timestamp)` is itself a syntax error in M mode).
			return switch (unit) {
				case SECOND -> "(second(?2)+microsecond(?2)/1e6)";
				case WEEK -> "weekofyear(?2)";
				case DAY_OF_WEEK -> "dayofweek(?2)";
				case DAY_OF_MONTH -> "dayofmonth(?2)";
				case DAY_OF_YEAR -> "dayofyear(?2)";
				case EPOCH -> "unix_timestamp(?2)";
				default -> "?1(?2)";
			};
		}
		return switch (unit) {
			case DAY_OF_WEEK -> "(" + TemporalOperationSupports.standard().extractPattern( unit ) + "+1)";
			default -> TemporalOperationSupports.standard().extractPattern(unit);
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String castPattern(CastType from, CastType to) {
		if ( from == CastType.STRING && to == CastType.BOOLEAN ) {
			// A mode: openGauss PG kernel natively accepts cast('y' as boolean)=true and
			// raises on cast('bla' as boolean). M mode (MySQL-compatible): cast(?1 as unsigned)
			// only parses leading digits ('1'->1, 'y'->0), losing the 'y'/'t'/'true'->true
			// semantics tested by HHH-18447, and cast('bla' as unsigned)=0 instead of raising.
			// Delegate to the base buildStringToBooleanCast: it maps the recognized spellings
			// through a values/union-all join, and an unrecognized string like 'bla' matches
			// nothing -> two result rows -> NonUniqueResultException, which the HHH-18447
			// invalid-string test expects as a HibernateException.
			if ( !isMMode() ) {
				return "cast(?1 as ?2)";
			}
			return super.castPattern( from, to );
		}

		if ( from == CastType.STRING && to == CastType.DATE ) {
			// M mode's DATE parser rejects `cast('YYYY-MM-DD' as date)` — it truncates the
			// 4-digit year to 2 digits and fails with "invalid value ... for MON". Parse
			// with an explicit format instead (matches appendDateTimeLiteral's DATE branch).
			return "to_date(?1,'YYYY-MM-DD')";
		}

		if ( to == CastType.STRING ) {
			switch ( from ) {
				case BOOLEAN:
				case INTEGER_BOOLEAN:
				case TF_BOOLEAN:
				case YN_BOOLEAN:
					return BooleanDecoder.toString( from );
				case DATE:
					// M mode's to_char() exists but misinterprets Oracle format pictures (returns
					// garbage); use date_format() with MySQL specifiers instead. to_date() is fine.
					return isMMode() ? "date_format(?1,'%Y-%m-%d')" : "to_char(?1,'YYYY-MM-DD')";
				case TIME:
					return "cast(?1 as ?2)";
				case TIMESTAMP:
					return isMMode()
							? "date_format(?1,'%Y-%m-%d %H:%i:%s.%f')"
							: "to_char(?1,'YYYY-MM-DD HH24:MI:SS.FF9')";
				case OFFSET_TIMESTAMP:
					return isMMode()
							? "date_format(?1,'%Y-%m-%d %H:%i:%s.%f')"
							: "to_char(?1,'YYYY-MM-DD HH24:MI:SS.FF9TZH:TZM')";
				case ZONE_TIMESTAMP:
					return isMMode()
							? "date_format(?1,'%Y-%m-%d %H:%i:%s.%f')"
							: "to_char(?1,'YYYY-MM-DD HH24:MI:SS.FF9 TZR')";
			}
		}

		return super.castPattern( from, to );
	}

	/**
	 * {@code microsecond} is the smallest unit for an {@code interval},
	 * and the highest precision for a {@code timestamp}, so we could
	 * use it as the "native" precision, but it's more convenient to use
	 * whole seconds (with the fractional part), since we want to use
	 * {@code extract(epoch from ...)} in our emulation of
	 * {@code timestampdiff()}.
	 */
	@Override
	@SPI({ USE, IMPLEMENT })
	public long fractionalSecondPrecisionInNanos() {
		return 1_000_000_000; //seconds
	}

	@Override @SuppressWarnings("deprecation")
	@SPI({ USE, IMPLEMENT })
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		if ( isMMode() ) {
			// M mode (MySQL-compatible) rejects PostgreSQL interval arithmetic `?3 + (?2)*interval '1 unit'`
			// and `cast(... as timestamp)`; use the native timestampadd(unit, expr, date) function instead.
			return switch (unit) {
				case NANOSECOND -> "timestampadd(microsecond,(?2)/1e3,?3)";
				// NATIVE = second (see timestampdiffPattern); microseconds would scale by 1e6.
				case NATIVE -> "timestampadd(second,?2,?3)";
				default -> "timestampadd(?1,?2,?3)";
			};
		}
		return intervalType != null
				? "(?2+?3)"
				: "cast(?3+" + intervalPattern( unit ) + " as " + temporalType.name().toLowerCase() + ")";
	}

	private static String intervalPattern(TemporalUnit unit) {
		return switch (unit) {
			case NANOSECOND -> "(?2)/1e3*interval '1 microsecond'";
			case NATIVE -> "(?2)*interval '1 second'";
			case QUARTER -> "(?2)*interval '3 month'"; // quarter is not supported in interval literals
			case WEEK -> "(?2)*interval '7 day'"; // week is not supported in interval literals
			default -> "(?2)*interval '1 " + unit + "'";
		};
	}

	@Override @SuppressWarnings("deprecation")
	@SPI({ USE, IMPLEMENT })
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		if ( isMMode() ) {
			// M mode (MySQL-compatible) rejects `extract(unit from ?3-?2)` (interval extract and
			// epoch are unsupported); use the native timestampdiff(unit, from, to) function instead.
			if ( unit == null ) {
				return "timestampdiff(second,?2,?3)";
			}
			return switch (unit) {
				case NANOSECOND -> "timestampdiff(microsecond,?2,?3)*1e3";
				// NATIVE resolves to second via getFractionalSecondPrecisionInNanos()=1e9;
				// returning microseconds here scales every Duration result by 1e6.
				case NATIVE -> "timestampdiff(second,?2,?3)";
				default -> "timestampdiff(?1,?2,?3)";
			};
		}
		if ( unit == null ) {
			return "(?3-?2)";
		}
		return switch (unit) {
			case YEAR -> "extract(year from ?3-?2)";
			case QUARTER -> "(extract(year from ?3-?2)*4+extract(month from ?3-?2)/3)";
			case MONTH -> "(extract(year from ?3-?2)*12+extract(month from ?3-?2))";
			case WEEK -> "(extract(day from ?3-?2)/7)"; // week is not supported by extract() when the argument is a duration
			case DAY -> "extract(day from ?3-?2)";
			// in order to avoid multiple calls to extract(),
			// we use extract(epoch from x - y) * factor for
			// all the following units:
			case HOUR, MINUTE, SECOND, NANOSECOND, NATIVE ->
					"extract(epoch from ?3-?2)" + EPOCH.conversionFactor( unit, this );
			default -> throw new SemanticException( "Unrecognized field: " + unit );
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TimeZoneSupport getTimeZoneSupport() {
		return TimeZoneSupport.NORMALIZE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		GaussDBFunctionRegistry functionRegistry = new GaussDBFunctionRegistry( functionContributions, isMMode() );
		functionRegistry.register();

		// GaussDB M mode DATE (datea) only recognizes year/month/day in extract(); other units
		// (week/doy/dow/epoch/quarter/hour/...) need a timestamp source. See GaussDBExtractFunction.
		functionContributions.getFunctionRegistry().register(
				"extract",
				new GaussDBExtractFunction( this, functionContributions.getTypeConfiguration() )
		);
	}

	@Override
	public @Nullable String getDefaultOrdinalityColumnName() {
		return "ordinality";
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NameQualifierSupport getNameQualifierSupport() {
		// This method is overridden so the correct value will be returned when
		// DatabaseMetaData is not available.
		return NameQualifierSupport.SCHEMA;
	}

	@Override
	public PredicateSupport getPredicateSupport() {
		if ( isMMode() ) {
			// M mode (MySQL-compatible) has no native ILIKE; the base translator emulates
			// case-insensitive LIKE via lower(col) like lower(pattern). A mode (openGauss PG
			// kernel) supports ILIKE natively.
			return PredicateSupport.builder( super.getPredicateSupport() )
					.capabilities(
							PredicateSupport.Capability.DISTINCT_FROM,
							PredicateSupport.Capability.TRUTHNESS
					)
					.build();
		}
		return PredicateSupport.builder( super.getPredicateSupport() )
				.caseInsensitiveLikeOperator( "ilike" )
				.capabilities(
						PredicateSupport.Capability.DISTINCT_FROM,
						PredicateSupport.Capability.TRUTHNESS
				)
				.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized IfExistsSupport getIfExistsSupport() {
		if ( ifExistsSupport == null ) {
			ifExistsSupport = new IfExistsSupport(
				// M mode rejects `alter table if exists <table> ...` with a syntax error; under
				// PostgreSQL transaction semantics that aborts the whole schema-generation
				// transaction, so subsequent DDL (create sequence/table) is silently ignored.
				// A mode accepts the IF EXISTS clause.
				isMMode() ? ExistenceCheckPlacement.NONE : ExistenceCheckPlacement.BEFORE_NAME,
				ExistenceCheckPlacement.BEFORE_NAME,
				ExistenceCheckPlacement.BEFORE_NAME,
				ExistenceCheckPlacement.NONE
		);
		}
		return ifExistsSupport;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String alterColumnType(AlterColumnTypeRequest request) {
		// would need multiple statements to 'set not null'/'drop not null', 'set default'/'drop default', 'set generated', etc
		if ( isMMode() ) {
			// M mode (MySQL-compatible) rejects `alter column ... set data type` (DB2/standard syntax)
			// with "syntax error at or near data". MySQL's `modify column` redefines the whole column,
			// so pass the full columnDefinition to preserve null/default attributes (same as MySQLDialect).
			return "modify column " + request.columnName() + " " + request.columnDefinition().trim();
		}
		return "alter column " + request.columnName() + " set data type " + request.columnType();
	}

	@Override
	public ValuesListSupport getValuesListSupport() {
		// M mode (MySQL-compatible) rejects PostgreSQL `values (..),(..)` derived tables
		// (`values (true),(false)` raises "syntax error at or near '('"), so restrict the
		// support to INSERT values lists; query-context callers fall back to `union all`.
		// A mode (openGauss PG kernel) supports both.
		return isMMode() ? ValuesListSupport.INSERT_ONLY : ValuesListSupport.STANDARD;
	}

	@Override
	public CteSupport getCteSupport() {
		if ( isMMode() ) {
			// M mode (MySQL-compatible) has no `INSERT ... RETURNING`, so DML-as-CTE
			// (mutation features) is unavailable; only plain query CTEs work. The multi-table
			// mutation fallback for M mode uses temporary tables instead.
			return CteSupport.builder().build();
		}
		return CteSupport.builder()
				.mutationFeatures(
						CteSupport.MutationFeature.NON_QUERY,
						CteSupport.MutationFeature.INSERT_CONFLICT
				)
				.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		// M mode: nextval()/currval()/setval() string args are case-sensitive (not folded like PostgreSQL),
		// while CREATE SEQUENCE folds the identifier to lowercase. Lower-casing the call-side name makes
		// nextval('ConcreteOne_SEQ') resolve to the stored concreteone_seq. See GaussDBMModeSequenceSupport.
		if ( isMMode() ) {
			return GaussDBMModeSequenceSupport.INSTANCE;
		}
		return GaussDBSequenceSupport.INSTANCE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized SchemaDropSupport getSchemaDropSupport() {
		if ( schemaDropSupport == null ) {
			schemaDropSupport = new SchemaDropSupport(
				List.of( "set client_min_messages = WARNING" ),
				ConstraintDropMode.EXPLICIT,
				" cascade"
		);
		}
		return schemaDropSupport;
	}

	private static final SequenceInformationExtractor SEQUENCE_INFORMATION_EXTRACTOR =
			SequenceInformationExtractors.builder( "select * from information_schema.sequences" ).build();

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		// M mode: pg_class exposes only relname (no catalog/schema/start/min/max/increment columns that
		// information_schema.sequences provides), so use a extractor that reads relname and skips the rest.
		// pg_sequence_parameters(oid) also exposes the increment value for
		// SequenceInformation.getIncrementValue() and HHH-12973 sequence-mismatch detection.
		if ( isMMode() ) {
			return GaussDBMModeSequenceInformationExtractor.INSTANCE;
		}
		return SEQUENCE_INFORMATION_EXTRACTOR;
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LimitLimitHandler.INSTANCE;
	}



	@Override
	public GenerationType getNativeValueGenerationStrategy() {
		return GenerationType.SEQUENCE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public LobSupport getLobSupport() {
		return LobSupports.postgresql();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String getSelectClauseNullString(SqlTypedMapping sqlTypeMapping, TypeConfiguration typeConfiguration) {
		final var ddlTypeRegistry = typeConfiguration.getDdlTypeRegistry();
		final var jdbcMapping = sqlTypeMapping.getJdbcMapping();
		final String castTypeName = ddlTypeRegistry.getDescriptor( jdbcMapping.getJdbcType().getDdlTypeCode() )
				.getCastTypeName( sqlTypeMapping.toSize(), (SqlExpressible) jdbcMapping, ddlTypeRegistry );
		return "cast(null as " + castTypeName + ")";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String quoteCollation(String collation) {
		return '\"' + collation + '\"';
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public org.hibernate.dialect.schema.spi.SchemaCommentSupport getSchemaCommentSupport() {
		return org.hibernate.dialect.schema.spi.SchemaCommentSupports.commentOn();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public CurrentTimestampSelection getCurrentTimestampSelection() {
		return CurrentTimestampSelection.prepared( "select now()" );
	}

	@Override
	public TupleCountSupport getTupleCountSupport() {
		// A mode (openGauss PG kernel) supports the native tuple form count(distinct (a,b));
		// M mode (MySQL-compatible) rejects it with "Unsupport type", so select UNSUPPORTED to
		// request Hibernate's CountFunction concat-based emulation (registered in
		// GaussDBFunctionRegistry for M mode).
		return isMMode()
				? TupleCountSupport.NONE
				: TupleCountSupport.builder()
						.nonDistinctSyntax( TupleCountSupport.Syntax.PARENTHESIZED_TUPLE )
						.distinctSyntax( TupleCountSupport.Syntax.PARENTHESIZED_TUPLE )
						.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		appender.appendSql( bool );
	}

	/**
	 * Words reserved by GaussDB that PostgreSQL treats as non-reserved, so Hibernate would otherwise
	 * emit them unquoted as identifiers. Keyword auto-quoting is forced {@code true} here (see
	 * {@link #buildIdentifierHelper}) but scoped to this curated set rather than the full
	 * ANSI/PostgreSQL keyword set, which over-reports words the modes permit as identifiers
	 * (e.g. {@code aggregate}), and rather than the driver-reported keyword list
	 * ({@code DatabaseMetaData#getSQLKeywords}, 485 words on gsjdbc4 506.0.0.b058 incl.
	 * {@code document}, {@code event}, {@code text}, {@code comment}), which false-quotes
	 * legitimate identifier-named tables/columns. {@code excluded} is also registered via
	 * {@link #registerKeyword} so it is recognized as a keyword in SQL templates
	 * ({@code Template} uses {@code #getKeywords()}). {@code match} is a MySQL reserved word
	 * (M mode) but non-reserved in PostgreSQL, so an unquoted {@code create table Match (...)}
	 * raises a syntax error in M mode; it is added here only for M mode (A mode keeps
	 * {@link #A_MODE_RESERVED_KEYWORDS} so existing A-mode behavior is unchanged).
	 */
	private static final Set<String> M_MODE_RESERVED_KEYWORDS = Set.of( "excluded", "match" );
	private static final Set<String> A_MODE_RESERVED_KEYWORDS = Set.of( "excluded" );

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuildRequest request) {
		final var builder = request.builder();
		final var jdbcMetadata = request.jdbcMetadata();

		if ( !jdbcMetadata.isJdbcMetadataAccessible() ) {
			builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.LOWER );
			builder.setQuotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		}
		// Replicate the base builder but apply only the curated reserved words (see
		// M_MODE_RESERVED_KEYWORDS / A_MODE_RESERVED_KEYWORDS) instead of the full inherited
		// ANSI/PG keyword set, and force auto-quoting on so the curated words are quoted
		// dialect-side. This deliberately does NOT rely on the global
		// {@code hibernate.auto_quote_keyword} setting: that leaks into every test/service
		// registry (system properties are a settings source) and changes DDL rendering even
		// for tests that explicitly select another dialect offline (e.g. RowLevelSecurityTest
		// with PostgreSQLDialect renders quoted table names there).
		builder.setAutoQuoteKeywords( true );
		builder.setUnquotedCaseStrategy( jdbcMetadata.getUnquotedIdentifierCaseStrategy() );
		builder.setQuotedCaseStrategy( jdbcMetadata.getQuotedIdentifierCaseStrategy() );
		builder.clearReservedWords();
		builder.applyReservedWords( isMMode() ? M_MODE_RESERVED_KEYWORDS : A_MODE_RESERVED_KEYWORDS );
		builder.setNameQualifierSupport( request.nameQualifierSupport() );
		if ( isMMode() ) {
			// M mode (MySQL-compatible) preserves unquoted identifier case — `create table AbCdEf` stores
			// "AbCdEf", not lower-cased. But gsjdbc4 reports storesLowerCaseIdentifiers=true (PG behavior),
			// so applyIdentifierCasing above sets LOWER, which makes schema validation's toMetaDataObjectName
			// lower-case table names (e.g. "testentity") and fail to match the stored "TestEntity", reporting
			// "missing table". Override to MIXED to match M mode's actual case preservation. A mode (PG kernel)
			// genuinely lower-cases unquoted identifiers, so keep the LOWER from applyIdentifierCasing.
			//
			// Known limitation: because gsjdbc4 reports storesLowerCaseIdentifiers=true while M mode actually
			// preserves case, tests that read column metadata through raw JDBC DatabaseMetaData.getColumns
			// (whose search pattern is lower-cased) cannot match a table stored as "Version" — e.g.
			// MigrationTest#testSimpleColumnTypeChange expects getColumns("version") to find the "Version"
			// table. Lowering unquotedCaseStrategy to fix that breaks schema validation (missing table), so
			// MIXED is the correct trade-off and that single test failure is an inherent M-mode limitation.
			builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		}
		return builder.build();
	}

	@Override
	public MultiTableMutationSupport getMultiTableMutationSupport() {
		// M mode (MySQL-compatible, single-node) has no `INSERT/UPDATE/DELETE ... RETURNING`
		// (RETURNING is a distributed-db-only feature here), so CTE-based multi-table mutation
		// cannot execute; use session-local temporary tables as the staging area instead.
		// A mode (openGauss PG kernel) keeps the CTE strategy.
		return isMMode() ? MultiTableMutationSupport.LOCAL_TEMPORARY_TABLE : MultiTableMutationSupport.CTE;
	}

	@Override
	public @Nullable TemporaryTableStrategy getLocalTemporaryTableStrategy() {
		if ( isMMode() ) {
			// M mode (openGauss PG kernel, MySQL-compatible) supports session-local temporary tables
			// (`CREATE LOCAL TEMPORARY TABLE`), used by LocalTemporaryTableMutationStrategy as the staging
			// area for multi-table mutations - M mode has no RETURNING, so CTE DML is unavailable.
			// StandardLocalTemporaryTableStrategy creates the temp table before each use
			// (BeforeUseAction.CREATE - directly `create local temporary table`, no drop-if-exists) and by
			// default does NOT drop after use (AfterUseAction.NONE), relying on the session to auto-drop.
			// But GaussDB's local temp table is connection-scoped and the connection pool reuses
			// connections, so NONE leaves HT_* tables behind - the next mutation on a reused connection
			// hits "Relation HT_X already exists" and aborts the transaction (patient-zero pollution:
			// JoinedSubclassBulkManipTest passes single-class but fails full-suite with HT_Person already
			// exists + transaction aborted). Override AfterUseAction to DROP so the temp table is dropped
			// after each use. A mode keeps the base default (null - CteMutationStrategy, no temp tables).
			return new StandardLocalTemporaryTableStrategy() {
				@Override
				public AfterUseAction getTemporaryTableAfterUseAction() {
					return AfterUseAction.DROP;
				}
			};
		}
		return super.getLocalTemporaryTableStrategy();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new GaussDBSqlAstTranslator<>( request );
			}
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}

	/**
	 * Constraint-name extractor for constraint violation exceptions.
	 * Originally contributed by Denny Bartelt.
	 */
	private static final ViolatedConstraintNameExtractor EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor( sqle -> {
				final String sqlState = JdbcExceptionHelper.extractSqlState( sqle );
				if ( sqlState != null ) {
					int state;
					try {
						state = Integer.parseInt(sqlState);
					}
					catch (NumberFormatException e) {
						state = 23001; // or some default value
					}
					switch ( state ) {
						// CHECK VIOLATION
						case 23514:
							return extractUsingTemplate( "violates check constraint \"", "\"", sqle.getMessage() );
						// UNIQUE VIOLATION
						case 23505:
							return extractUsingTemplate( "violates unique constraint \"", "\"", sqle.getMessage() );
						// FOREIGN KEY VIOLATION
						case 23503:
							return extractUsingTemplate( "violates foreign key constraint \"", "\"", sqle.getMessage() );
						// NOT NULL VIOLATION
						// GaussDB M mode message: `The null value in column "<col>" violates the not-null constraint.`
						// (the leading "The " and the "the " before "not-null" defeat the PG-style end template),
						// so extract the column name between `column "` and the closing quote.
						case 23502:
							return extractUsingTemplate( "column \"", "\"", sqle.getMessage() );
						// TODO: RESTRICT VIOLATION
						case 23001:
							return null;
					}
				}
				return null;
			} );

	@Override
	public boolean causesRollback(SQLException sqlException) {
		return true;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			final String sqlState = JdbcExceptionHelper.extractSqlState( sqlException );
			if ( sqlState != null ) {
				switch ( sqlState ) {
					case "40P01":
						// DEADLOCK DETECTED
						return new LockAcquisitionException( message, sqlException, sql );
					case "55P03":
						// LOCK NOT AVAILABLE
						return new LockTimeoutException( message, sqlException, sql );
					case "57014":
						return new QueryTimeoutException( message, sqlException, sql );
				}
			}
			return null;
		};
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public CallableStatementSupport getCallableStatementSupport() {
		return CALLABLE_STATEMENT_SUPPORT;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public RefCursorSupportFactory getRefCursorSupportFactory() {
		return REF_CURSOR_SUPPORT_FACTORY;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public IndexNameQualification nameQualification() {
		return IndexNameQualification.UNQUALIFIED;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		// M mode (MySQL-compatible) rejects bigserial/serial; use MySQL-style
		// `bigint not null auto_increment` + last_insert_id() (verified). A mode keeps
		// the PG-style bigserial (Oracle-compatible, supports it).
		return isMMode() ? MySQLIdentityColumnSupport.INSTANCE : GaussDBIdentityColumnSupport.INSTANCE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public GeneratedValuesSupport getGeneratedValuesSupport() {
		if ( isMMode() ) {
			// M mode rejects `insert/update/delete ... returning` at the kernel level
			// ("INSERT/UPDATE/DELETE/REPLACE ... RETURNING is not supported"), so report
			// no immediate generated-value retrieval. Note IdentityGenerator's delegate
			// chain tries InsertReturningDelegate before falling back to
			// BasicSelectingDelegate whenever the dialect claims INSERT_RETURNING -
			// regardless of the `hibernate.jdbc.use_get_generated_keys` setting - so
			// claiming it here would break every identity insert and returning-based
			// mutation. With this profile and getGeneratedKeys disabled (see
			// contributeDefaultProperties), identity falls back to BasicSelectingDelegate
			// (plain insert then `select last_insert_id()`), matching MySQLDialect.
			return GeneratedValuesSupport.STANDARD;
		}
		// A mode (openGauss PG kernel) supports native mutation returning
		return GeneratedValuesSupport.builder( super.getGeneratedValuesSupport() )
				.enable(
						GeneratedValuesSupport.Capability.INSERT_RETURNING,
						GeneratedValuesSupport.Capability.UPDATE_RETURNING,
						GeneratedValuesSupport.Capability.INSERT_RETURNING_ROW_ID
				)
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public SingleRowTableSupport getSingleRowTableSupport() {
		if ( isMMode() ) {
			// M mode (MySQL-compatible) rejects the standard `(values(0))` single-row anchor
			// (`syntax error at or near "("`, probe-verified), and `dual` can take neither an
			// alias nor joins (`from dual d_` / `from dual join ...` are syntax errors), so the
			// mutation join-emulation anchor must be a plain derived table: `(select 0)`
			// (probe-verified to work with aliases and joins). A mode (openGauss PG kernel)
			// accepts the standard values-table expression.
			return SingleRowTableSupport.builder( super.getSingleRowTableSupport() )
					.tableExpression( "(select 0)" )
					.build();
		}
		return super.getSingleRowTableSupport();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NationalizationSupport getNationalizationSupport() {
		return NationalizationSupport.IMPLICIT;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public int getMaxIdentifierLength() {
		return 63;
	}

	@Override
	public ArraySupport getArraySupport() {
		// M mode (MySQL-compatible) has no array column type (syntax error on `type[]`).
		// Reporting NONE makes the base array type resolution fall back to VARBINARY, so
		// array tests are skipped for M mode. A mode (openGauss PG kernel) supports arrays.
		return isMMode() ? ArraySupport.NONE : ARRAY_SUPPORT;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalValueSemantics getTemporalValueSemantics() {
		// A mode (openGauss PG kernel) supports `timestamp with time zone '...'` literals; M mode
		// (MySQL-compatible) rejects them (syntax error at "time"), so report UNSUPPORTED offset
		// literals and let appendDateTimeLiteral render a plain `timestamp '...'` literal instead.
		return isMMode() ? TemporalValueSemantics.STANDARD : TemporalValueSemantics.OFFSET_LITERALS;
	}

	@Override
	public LiteralSupport getLiteralSupport() {
		if ( isMMode() ) {
			// M mode stores UUID as varchar(36) + VarcharUUIDJdbcType (see registerColumnTypes).
			// Render the literal as a plain string literal instead of the default
			// `cast('...' as uuid)`, which M mode rejects (no native uuid type).
			// A mode keeps the base literal rendering.
			return new DelegatingLiteralSupport( super.getLiteralSupport() ) {
				@Override
				public void appendUUIDLiteral(SqlAppender appender, UUID literal) {
					appender.appendSql( "'" );
					appender.appendSql( literal.toString() );
					appender.appendSql( "'" );
				}
			};
		}
		return super.getLiteralSupport();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendFormat(SqlAppender appender, String format) {
		if ( isMMode() ) {
			// M mode (MySQL-compatible): format() renders as date_format(), which needs MySQL
			// specifiers. Translate Hibernate/Java format pictures (yyyy-MM-dd HH:mm:ss) to MySQL
			// (%Y-%m-%d %H:%i:%s). Required by DateTruncEmulation (trunc_truncate's FORMAT emulation
			// builds a `format(datetime, pattern)` expression) and by the format() function.
			// Reuse MySQLDialect's well-tested translation.
			appender.appendSql( MySQLDialect.datetimeFormat( format ).result() );
			return;
		}
		throw new UnsupportedOperationException( "GaussDB not support datetime format yet" );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String translateExtractField(TemporalUnit unit) {
		return switch (unit) {
			//WEEK means the ISO week number
			case DAY_OF_MONTH -> "day";
			case DAY_OF_YEAR -> "doy";
			case DAY_OF_WEEK -> "dow";
			default -> TemporalOperationSupports.standard().translateExtractField( unit );
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public AggregateSupport getAggregateSupport() {
		// M mode (MySQL-compatible) has no PG-style aggregate support: CREATE TYPE ... AS (...) is a
		// syntax error, the XML function family is unavailable and the JSON aggregate read/write
		// expressions are not adapted, so report no aggregate support at all (the base implementation;
		// its probes throw UnsupportedOperationException, which the standard feature checks interpret
		// as "not supported"). A mode (openGauss PG kernel) supports them through GaussDBAggregateSupport.
		return isMMode() ? AggregateSupportImpl.INSTANCE : org.hibernate.community.dialect.aggregate.GaussDBAggregateSupport.valueOf( this );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendBinaryLiteral(SqlAppender appender, byte[] bytes) {
		if ( isMMode() ) {
			// M mode (MySQL-compatible) uses the x'hex' binary string literal;
			// PostgreSQL's `bytea '\x..'` is a syntax error in M mode.
			appender.appendSql( "x'" );
			PrimitiveByteArrayJavaType.INSTANCE.appendString( appender, bytes );
			appender.appendSql( '\'' );
			return;
		}
		appender.appendSql( "bytea '\\x" );
		PrimitiveByteArrayJavaType.INSTANCE.appendString( appender, bytes );
		appender.appendSql( '\'' );
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
				// M mode's DATE parser ignores ISO DateStyle and rejects `date 'YYYY-MM-DD'`
				// ("invalid value ... for MON", truncates the 4-digit year to 2). to_date with an
				// explicit format parses correctly and returns the date type.
				appender.appendSql( "to_date('" );
				appendAsDate( appender, temporalAccessor );
				appender.appendSql( "','YYYY-MM-DD')" );
				break;
			case TIME:
				if ( getTemporalValueSemantics().supportsLiteralOffset() && temporalAccessor.isSupported( ChronoField.OFFSET_SECONDS ) ) {
					appender.appendSql( "time with time zone '" );
					appendAsTime( appender, temporalAccessor, true, jdbcTimeZone );
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
					appendAsTimestampWithMicros( appender, temporalAccessor, true, jdbcTimeZone );
					appender.appendSql( '\'' );
				}
				else {
					appender.appendSql( "timestamp '" );
					appendAsTimestampWithMicros( appender, temporalAccessor, false, jdbcTimeZone );
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
				appender.appendSql( "to_date('" );
				appendAsDate( appender, date );
				appender.appendSql( "','YYYY-MM-DD')" );
				break;
			case TIME:
				if ( isMMode() ) {
					// M mode has no timezone-aware time type; appendAsTime(date, tz) renders
					// `time 'HH:mm:ssXXX'` (with offset) which M mode rejects ("...is incorrect").
					// Render local time (no offset), matching the TemporalAccessor overload.
					appender.appendSql( "time '" );
					appendAsLocalTime( appender, date );
				}
				else {
					appender.appendSql( "time with time zone '" );
					appendAsTime( appender, date, jdbcTimeZone );
				}
				appender.appendSql( '\'' );
				break;
			case TIMESTAMP:
				appender.appendSql( isMMode() ? "timestamp '" : "timestamp with time zone '" );
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
				appender.appendSql( "to_date('" );
				appendAsDate( appender, calendar );
				appender.appendSql( "','YYYY-MM-DD')" );
				break;
			case TIME:
				if ( isMMode() ) {
					appender.appendSql( "time '" );
					appendAsLocalTime( appender, calendar );
				}
				else {
					appender.appendSql( "time with time zone '" );
					appendAsTime( appender, calendar, jdbcTimeZone );
				}
				appender.appendSql( '\'' );
				break;
			case TIMESTAMP:
				appender.appendSql( isMMode() ? "timestamp '" : "timestamp with time zone '" );
				appendAsTimestampWithMillis( appender, calendar, jdbcTimeZone );
				appender.appendSql( '\'' );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}











	@Override
	public LockingSupport getLockingSupport() {
		return GaussDBLockingSupport.LOCKING_SUPPORT;
	}





	@Override
	public SubquerySupport getSubquerySupport() {
		return SubquerySupport.builder()
				.feature( SubquerySupport.Feature.OFFSET, true )
				.feature( SubquerySupport.Feature.LATERAL, false )
				.feature( SubquerySupport.Feature.ORDER_BY, false )
				.build();
	}

	@Override
	public WindowFunctionSupport getWindowFunctionSupport() {
		return WindowFunctionSupport.builder()
				.features( WindowFunctionSupport.Feature.values() )
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public FetchClauseSupport getFetchClauseSupport() {
		return FetchClauseSupport.NONE;
	}


	@Override
	public boolean supportsFilterClause() {
		return false;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public FunctionalDependencyAnalysisSupport getFunctionalDependencyAnalysisSupport() {
		return FunctionalDependencyAnalysisSupport.TABLE_REFERENCE;
	}

	@Override
	@SPI(IMPLEMENT)
	public void augmentRecognizedTableTypes(List<String> tableTypesList) {
		super.augmentRecognizedTableTypes( tableTypesList );
		tableTypesList.add( "MATERIALIZED VIEW" );
		tableTypesList.add( "PARTITIONED TABLE" );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes(typeContributions, serviceRegistry);
		contributeGaussDBTypes( typeContributions);
	}

	/**
	 * Allow for extension points to override this only
	 */
	protected void contributeGaussDBTypes(TypeContributions typeContributions) {
		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration()
				.getJdbcTypeRegistry();
		// For how BLOB affects Hibernate, see:
		//     http://in.relation.to/15492.lace
		jdbcTypeRegistry.addDescriptor( Types.BLOB, BlobJdbcType.BLOB_BINDING );
		jdbcTypeRegistry.addDescriptor( Types.CLOB, ClobJdbcType.CLOB_BINDING );
		jdbcTypeRegistry.addDescriptor( XmlJdbcType.INSTANCE );

		jdbcTypeRegistry.addDescriptorIfAbsent( GaussDBCastingInetJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptorIfAbsent( GaussDBCastingIntervalSecondJdbcType.INSTANCE );
		if ( !isMMode() ) {
			// M mode (MySQL-compatible) does not support PG-style composite types
			// (CREATE TYPE ... AS (...) is a syntax error), so don't register the structured
			// type descriptor there; @Struct mappings are disabled anyway (see supportsStructTypes()).
			// A mode (openGauss PG kernel) supports them.
			jdbcTypeRegistry.addDescriptorIfAbsent( GaussDBStructuredJdbcType.INSTANCE );
		}
		jdbcTypeRegistry.addDescriptorIfAbsent( GaussDBCastingJsonJdbcType.JSON_INSTANCE );
		jdbcTypeRegistry.addTypeConstructorIfAbsent( GaussDBCastingJsonArrayJdbcTypeConstructor.JSON_INSTANCE );

		// GaussDB requires a custom binder for binding untyped nulls as VARBINARY
		typeContributions.contributeJdbcType( ObjectNullAsBinaryTypeJdbcType.INSTANCE );

		// M mode exposes DATE as the non-standard `datea` type (JDBC type code OTHER); the gsjdbc4 driver
		// can't convert it via getObject(col, LocalDate.class), so read LOCAL_DATE columns through getDate().
		typeContributions.contributeJdbcType( GaussDBLocalDateJdbcType.INSTANCE );

		// gsjdbc4's getObject(int, Class) cannot convert DATETIME/TIMESTAMP columns to LocalDateTime
		// ("Cannot convert the column of type DATETIME to requested type timestamp"), Instant
		// ("conversion to class java.time.Instant from 93 not supported"), or OffsetDateTime
		// ("conversion to class java.time.OffsetDateTime from datetime not supported"). Read through
		// getTimestamp(); the temporal JavaType.wrap converts the Timestamp. LocalDate is overridden
		// separately (non-standard `datea` type); LocalTime reads correctly through getObject, so only
		// LOCAL_DATE_TIME, INSTANT and OFFSET_DATE_TIME are overridden here. Writing OffsetDateTime through
		// setObject sends a timestamptz expression that M mode datetime rejects, so the binder uses
		// setTimestamp(Timestamp) too. A mode (openGauss PG kernel) reads/writes TIMESTAMP via
		// getTimestamp/setTimestamp correctly too.
		typeContributions.contributeJdbcType( GaussDBLocalDateTimeJdbcType.INSTANCE );
		typeContributions.contributeJdbcType( GaussDBInstantJdbcType.INSTANCE );
		typeContributions.contributeJdbcType( GaussDBOffsetDateTimeJdbcType.INSTANCE );
		// @JdbcTypeCode(TIMESTAMP_WITH_TIMEZONE) resolves to SqlTypes.TIMESTAMP_WITH_TIMEZONE (2014),
		// distinct from OFFSET_DATE_TIME (3012) and INSTANT (3008) registered above. The default
		// TimestampWithTimeZoneJdbcType binds via setObject(.., TIMESTAMP_WITH_TIMEZONE), which gsjdbc4
		// turns into a `timestamp with time zone` expression that M mode datetime rejects (the call does
		// not throw, so the built-in setTimestamp fallback never runs). GaussDBTimestampWithTimeZoneJdbcType
		// binds through setTimestamp directly. A mode (timestamptz columns) round-trips via setTimestamp too.
		typeContributions.contributeJdbcType( GaussDBTimestampWithTimeZoneJdbcType.INSTANCE );

		// M mode stores boolean as int1/uint8; gsjdbc4 MResultSet.getBoolean routes non-BOOLEAN/BIT columns
		// through MBooleanTypeUtils.castToBoolean, which rejects java.math.BigInteger (uint8, returned for
		// CASE expressions like `case when ... then true else false end`) with "Cannot cast to boolean".
		// Read boolean columns via getString and parse, bypassing castToBoolean. A mode keeps BooleanJdbcType.
		if ( isMMode() ) {
			jdbcTypeRegistry.addDescriptor( Types.BOOLEAN, GaussDBBooleanJdbcType.INSTANCE );
			jdbcTypeRegistry.addDescriptor( Types.BIT, GaussDBBooleanJdbcType.INSTANCE );
		}

		// Until we remove StandardBasicTypes, we have to keep this
		typeContributions.contributeType(
				new JavaObjectType(
						ObjectNullAsBinaryTypeJdbcType.INSTANCE,
						typeContributions.getTypeConfiguration()
								.getJavaTypeRegistry()
								.resolveDescriptor( Object.class )
				)
		);

		jdbcTypeRegistry.addDescriptor( GaussDBEnumJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( GaussDBOrdinalEnumJdbcType.INSTANCE );
		// M mode stores UUID as varchar(36) (no native uuid type); A mode uses the native uuid type.
		jdbcTypeRegistry.addDescriptor( isMMode() ? VarcharUUIDJdbcType.INSTANCE : GaussDBUUIDJdbcType.INSTANCE );

		// Replace the standard array constructor
		jdbcTypeRegistry.addTypeConstructor( GaussDBArrayJdbcTypeConstructor.INSTANCE );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	@Override
	public Exporter<Table> getTableExporter() {
		return gaussDBTableExporter;
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public Exporter<UserDefinedType> getUserDefinedTypeExporter() {
		return userDefinedTypeExporter;
	}

	/**
	 * @return {@code true}, but only because we can "batch" truncate
	 */
	@Override
	@SPI({ USE, IMPLEMENT })
	public TruncateMode truncateMode() {
		return TruncateMode.MULTI_TABLE;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public List<String> renderCommands(TruncateRequest request) {
		return request.tableNames().isEmpty()
				? List.of()
				: List.of( "truncate table " + String.join( ", ", request.tableNames() ) );
	}

	@Override
	@SPI(IMPLEMENT)
	public String getQueryHintString(String sql, String hints) {
		return "/*+ " + hints + " */ " + sql;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public QueryHintPlacement getQueryHintPlacement() {
		return QueryHintPlacement.BEFORE_COMMENT;
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public MutationOperation createOptionalTableUpdateOperation(
			OptionalTableUpdateOperationRequest request) {
		if ( isMMode() ) {
			// M mode (MySQL-compatible): GaussDB MERGE does not support `WHEN MATCHED AND <condition>`,
			// which the merge-based path emits for the optional-table delete branch.
			// For tables with updatable value columns use the base update -> insert +
			// unique-violation-fallback operation: the upsert operation's conflict-ignoring
			// insert (`on duplicate key update col = col`) masks duplicate-key violations, so a
			// tenant-filtered upsert of another tenant's row succeeds silently instead of
			// failing (TenantIdMutationTest); the base operation is 7.4-proven on M mode 505.2.
			// For id-only tables the base operation's plain INSERT fallback aborts the
			// transaction on duplicate key (PG error semantics: the swallowed violation still
			// poisons the transaction), so keep the upsert operation there - its insert
			// ignores primary-key collisions via `on duplicate key update col = col`
			// (UpsertTest#testIdOnlySubtype, UpsertWithDiscriminatorTest).
			if ( hasUpdatableValueBindings( request.update() ) ) {
				return new OptionalTableUpdateOperation( request.mutationTarget(), request.update() );
			}
			return new OptionalTableUpdateWithUpsertOperation( request.update(), request.versionedTarget() );
		}
		// A mode (openGauss Oracle-compatible): the merge-based path (PostgreSQLDialect#supportsMerge)
		// is verified to work; keep it.
		return super.createOptionalTableUpdateOperation( request );
	}

	private static boolean hasUpdatableValueBindings( OptionalTableUpdate upsert ) {
		for ( var binding : upsert.getValueBindings() ) {
			if ( binding.isAttributeUpdatable() ) {
				return true;
			}
		}
		return false;
	}

	private static class NativeParameterMarkers implements ParameterMarkerStrategy {
		/**
		 * Singleton access
		 */
		public static final NativeParameterMarkers INSTANCE = new NativeParameterMarkers();

		@Override
		public String createMarker(int position, JdbcType jdbcType) {
			return "$" + position;
		}
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
	@SPI({ IMPLEMENT, SUPPLY })
	public ObjectNullBindingStrategy getObjectNullBindingStrategy() {
		return ObjectNullBindingStrategy.SET_NULL_WITH_NULL_TYPE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public InformationExtractor getInformationExtractor(ExtractionContext extractionContext) {
		// Standard JDBC-metadata extraction, like the pre-8.0 Dialect default: the PostgreSQL
		// bulk-index SQL (information_schema._pg_expandarray SRF join + `i.indoption[ic.n - 1] & 1::smallint`)
		// is rejected by GaussDB's openGauss kernel ("invalid reference to FROM-clause entry for table i"),
		// while the driver's DatabaseMetaData.getIndexInfo works in both compatibility modes.
		return InformationExtractors.jdbcMetadata( extractionContext );
	}
}
