/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import jakarta.persistence.GenerationType;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Length;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.QueryTimeoutException;
import org.hibernate.Timeouts;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.community.dialect.identity.GaussDBIdentityColumnSupport;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.community.dialect.lock.internal.GaussDBLockingSupport;
import org.hibernate.community.dialect.sequence.GaussDBMModeSequenceInformationExtractor;
import org.hibernate.community.dialect.sequence.GaussDBMModeSequenceSupport;
import org.hibernate.community.dialect.sequence.GaussDBSequenceSupport;
import org.hibernate.dialect.BooleanDecoder;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.FunctionalDependencyAnalysisSupport;
import org.hibernate.dialect.FunctionalDependencyAnalysisSupportImpl;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.NationalizationSupport;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.dialect.SelectItemReferenceStrategy;
import org.hibernate.dialect.TimeZoneSupport;
import org.hibernate.dialect.temptable.StandardLocalTemporaryTableStrategy;
import org.hibernate.dialect.temptable.TemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
import org.hibernate.community.dialect.aggregate.GaussDBAggregateSupport;
import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.MySQLIdentityColumnSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitLimitHandler;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.unique.CreateTableUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.procedure.internal.PostgreSQLCallableStatementSupport;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.query.SemanticException;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.sqm.mutation.internal.cte.CteInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.cte.CteMutationStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.internal.OptionalTableUpdate;
import org.hibernate.sql.model.jdbc.OptionalTableUpdateWithUpsertOperation;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.internal.StandardTableExporter;
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
import org.hibernate.type.descriptor.sql.internal.ArrayDdlTypeImpl;
import org.hibernate.type.descriptor.sql.internal.CapacityDependentDdlType;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.internal.NamedNativeEnumDdlTypeImpl;
import org.hibernate.type.descriptor.sql.internal.NamedNativeOrdinalEnumDdlTypeImpl;
import org.hibernate.type.descriptor.sql.internal.Scale6IntervalSecondDdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
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
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsDate;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsLocalTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithMicros;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithMillis;

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
public class GaussDBDialect extends Dialect {
	protected final static DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 2 );

	/**
	 * Configuration key to explicitly set the GaussDB compatibility mode (e.g. {@code =M}). Used when
	 * JDBC metadata is unavailable on boot ({@code hibernate.temp.use_jdbc_metadata_defaults=false}),
	 * where the {@code datcompatibility} probe cannot run; the gaussdb test profile sets it to {@code M}.
	 */
	public final static String GAUSSDB_COMPATIBILITY_MODE = "hibernate.dialect.gaussdb.compatibility_mode";

	// GaussDB compatibility mode of the target database (pg_database.datcompatibility):
	// "A" = Oracle-compatible, "B"/"M" = MySQL-compatible, "pg" = PostgreSQL-compatible.
	// Defaults to "A"; detection runs from the DialectResolutionInfo constructor.
	private String compatibilityMode = "A";

	private final UniqueDelegate uniqueDelegate = new CreateTableUniqueDelegate(this);
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

	public GaussDBDialect() {
		this(MINIMUM_VERSION);
	}

	public GaussDBDialect(DialectResolutionInfo info) {
		this( info.makeCopyOrDefault( MINIMUM_VERSION ));
		detectCompatibilityMode( info );
		registerKeywords( info );
	}

	public GaussDBDialect(DatabaseVersion version) {
		super( version );
		// M mode reserves `excluded` as a keyword (PostgreSQL treats it as non-reserved, usable as a column name),
		// so identifiers named "excluded" must be quoted. GaussDB upserts use ON DUPLICATE KEY (not ON CONFLICT's
		// EXCLUDED alias), so quoting the identifier is safe.
		registerKeyword( "excluded" );
	}

	private void detectCompatibilityMode(DialectResolutionInfo info) {
		// The compatibility mode (A=Oracle, M/B=MySQL) drives nearly every M-mode override. Prefer an
		// explicit `hibernate.dialect.gaussdb.compatibility_mode` config value: it works even when JDBC
		// metadata is disallowed on boot (hibernate.temp.use_jdbc_metadata_defaults=false, e.g. the
		// SchemaUpdate tests), where metadata is null and the datcompatibility probe below cannot run —
		// without it the dialect would silently default to "A" and break information_schema.sequences
		// extraction (M mode has no such view). Fall back to probing datcompatibility when no explicit
		// mode is configured.
		final Map<String, Object> configValues = info.getConfigurationValues();
		if ( configValues != null ) {
			final Object configured = configValues.get( GAUSSDB_COMPATIBILITY_MODE );
			if ( configured != null ) {
				final String mode = configured.toString().trim();
				if ( !mode.isEmpty() ) {
					applyCompatibilityMode( mode );
					return;
				}
			}
		}
		final DatabaseMetaData metaData = info.getDatabaseMetadata();
		if ( metaData == null ) {
			return;
		}
		try ( java.sql.Statement statement = metaData.getConnection().createStatement();
				ResultSet rs = statement.executeQuery(
						"select datcompatibility from pg_database where datname = current_database()" ) ) {
			if ( rs.next() ) {
				final String mode = rs.getString( 1 );
				if ( mode != null ) {
					applyCompatibilityMode( mode.trim() );
				}
			}
		}
		catch (SQLException e) {
			// keep default ("A") on detection failure
		}
	}

	private void applyCompatibilityMode(String mode) {
		this.compatibilityMode = mode;
		// initDefaultProperties() ran during super() construction while compatibilityMode was still the
		// default "A", so re-sync the USE_GET_GENERATED_KEYS default now that the real mode is known.
		// M mode must disable getGeneratedKeys: gsjdbc4 implements it by rewriting the INSERT with a
		// RETURNING clause, which single-node M mode rejects ("Unsupported function. only supported in
		// distributed database").
		getDefaultProperties().setProperty(
				AvailableSettings.USE_GET_GENERATED_KEYS,
				Boolean.toString( !isMMode() )
		);
	}

	/**
	 * Whether the target database runs in MySQL-compatible mode (datcompatibility "B" or "M").
	 */
	public boolean isMMode() {
		return "M".equals( compatibilityMode ) || "B".equals( compatibilityMode );
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
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_VERSION;
	}

	@Override
	public boolean getDefaultNonContextualLobCreation() {
		return true;
	}

	@Override
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
	protected boolean isLob(int sqlTypeCode) {
		return switch ( sqlTypeCode ) {
			case LONG32VARCHAR, LONG32NVARCHAR, LONG32VARBINARY -> false;
			default -> super.isLob( sqlTypeCode );
		};
	}

	@Override
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
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		// We need to configure that the array type uses the raw element type for casts
		ddlTypeRegistry.addDescriptor( new ArrayDdlTypeImpl( this, true ) );

		// Register this type to be able to support Float[]
		// The issue is that the JDBC driver can't handle createArrayOf( "float(24)", ... )
		// It requires the use of "real" or "float4"
		// Alternatively we could introduce a new API in Dialect for creating such base names
		ddlTypeRegistry.addDescriptor(
				CapacityDependentDdlType.builder( FLOAT, columnType( FLOAT ), castType( FLOAT ), this )
						.withTypeCapacity( 24, "float4" )
						.build()
		);

		// M mode (MySQL-compatible) has no native `xml` type (syntax error on `x xml`); store XML as `text`.
		// A mode (openGauss PG kernel) keeps `xml`.
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( SQLXML, isMMode() ? "text" : "xml", this ) );
		// M mode (MySQL-compatible) has no native `uuid` type (syntax error on `theuuid uuid`);
		// store UUID as `varchar(36)` paired with VarcharUUIDJdbcType. A mode keeps `uuid`.
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( UUID, isMMode() ? "varchar(36)" : "uuid", this ) );
		// M mode (MySQL-compatible) has no native `inet` type (syntax error on `x inet`); store IPv4/IPv6
		// addresses as `varchar(45)` (PG inet is text-based, GaussDBCastingInetJdbcType binds/reads strings).
		// A mode (openGauss PG kernel) keeps `inet`.
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( INET, isMMode() ? "varchar(45)" : "inet", this ) );
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( GEOMETRY, "geometry", this ) );
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( GEOGRAPHY, "geography", this ) );
		if ( isMMode() ) {
			// M mode (MySQL-compatible) has no native interval column type (syntax error on
			// `interval second(6)`); store Duration seconds as numeric($p,$s) instead.
			// A mode (openGauss PG kernel) keeps the native `interval second($s)` type.
			ddlTypeRegistry.addDescriptor( new DdlTypeImpl( INTERVAL_SECOND, "numeric($p,$s)", this ) );
		}
		else {
			ddlTypeRegistry.addDescriptor( new Scale6IntervalSecondDdlType( this ) );
		}

		// GaussDB in MySQL-compatibility (M) mode does not support creating `jsonb` tables
		// (syntax error at "JSONB"), so use the `json` type instead
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( JSON, "json", this ) );

		ddlTypeRegistry.addDescriptor( new NamedNativeEnumDdlTypeImpl( this ) );
		ddlTypeRegistry.addDescriptor( new NamedNativeOrdinalEnumDdlTypeImpl( this ) );
	}

	@Override
	public int getMaxVarcharLength() {
		// M mode (MySQL-compatible) caps varchar at 16383 bytes; longer strings must
		// use text (the LONG32VARCHAR fallback maps to "text"). A mode (Oracle-compatible)
		// supports the larger 10MB varchar. Mirrors MySQLDialect#getMaxVarcharLength.
		return isMMode() ? 16_383 : 10_485_760;
	}

	@Override
	public int getMaxVarcharCapacity() {
		// 1GB-85-4 according to GaussDB docs
		return 1_073_741_727;
	}

	@Override
	public int getMaxVarbinaryLength() {
		// M mode (MySQL-compatible) varbinary tops out at 65535 bytes (like MySQL). Return 65535 so
		// longer byte[] upgrades to LONG32VARBINARY -> longblob (see columnType); returning LONG32
		// makes long32/lob columns render varbinary(2147483647) which M mode rejects on CREATE TABLE.
		// A mode (bytea, unbounded) keeps LONG32.
		return isMMode() ? 65_535 : Length.LONG32;
	}

	@Override
	public int getDefaultStatementBatchSize() {
		return 15;
	}

	@Override
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
				final SqlTypedJdbcType descriptor = jdbcTypeRegistry.findSqlTypedDescriptor(
						// Skip the schema
						columnTypeName.substring( columnTypeName.indexOf( '.' ) + 1 )
				);
				if ( descriptor != null ) {
					return descriptor;
				}
				break;
		}
		return jdbcTypeRegistry.getDescriptor( jdbcTypeCode );
	}

	@Override
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
	public String getEnumTypeDeclaration(String name, String[] values) {
		return name;
	}

	@Override
	public String[] getCreateEnumTypeCommand(String name, String[] values) {
		StringBuilder type = new StringBuilder();
		type.append( "create type " )
				.append( name )
				.append( " as enum (" );
		String separator = "";
		for ( String value : values ) {
			type.append( separator ).append('\'').append( value ).append('\'');
			separator = ",";
		}
		type.append( ')' );
		String cast1 = "create cast (varchar as " +
				name +
				") with inout as implicit";
		String cast2 = "create cast (" +
				name +
				" as varchar) with inout as implicit";
		return new String[] { type.toString(), cast1, cast2 };
	}

	@Override
	public String[] getDropEnumTypeCommand(String name) {
		return new String[] { "drop type if exists " + name + " cascade" };
	}

	@Override
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
	public String currentTime() {
		return "localtime";
	}

	@Override
	public String currentTimestamp() {
		if ( isMMode() ) {
			// M mode (MySQL kernel): bare `localtimestamp` is second-precision, so two timestamps
			// produced within the same second compare equal. @CurrentTimestamp/@CreationTimestamp/
			// @UpdateTimestamp tests that update an entity ~10ms apart then fail, even though
			// getDefaultTimestampPrecision()==6 advertises microsecond precision (via the
			// CurrentTimestampHasMicrosecondPrecision feature check). Emit `now(6)` to match the
			// datetime($p) column type and yield microsecond precision. A mode (openGauss PG kernel)
			// keeps bare `localtimestamp` (microsecond by default) — zero regression.
			return "now(" + getDefaultTimestampPrecision() + ")";
		}
		return "localtimestamp";
	}

	@Override
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
			return "now(" + getDefaultTimestampPrecision() + ")";
		}
		return "current_timestamp";
	}

	@Override
	public String getColumnDefaultString(String defaultValue) {
		if ( isMMode() ) {
			// M mode requires a datetime(N) column's current-timestamp default to carry matching
			// precision (N); the bare CURRENT_TIMESTAMP that @ColumnDefault produces is rejected
			// ("The default value of the ... type is invalid"). now(getDefaultTimestampPrecision())
			// matches the datetime($p) columnType, whose $p defaults to getDefaultTimestampPrecision()
			// (6). A mode keeps the super behaviour.
			final String upper = defaultValue.trim().toUpperCase( Locale.ROOT );
			if ( upper.equals( "CURRENT_TIMESTAMP" ) || upper.equals( "CURRENT_TIMESTAMP()" )
					|| upper.equals( "LOCALTIMESTAMP" ) || upper.equals( "LOCALTIMESTAMP()" )
					|| upper.equals( "NOW" ) || upper.equals( "NOW()" ) ) {
				return "now(" + getDefaultTimestampPrecision() + ")";
			}
		}
		return super.getColumnDefaultString( defaultValue );
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
			case DAY_OF_WEEK -> "(" + super.extractPattern( unit ) + "+1)";
			default -> super.extractPattern(unit);
		};
	}

	@Override
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
	public long getFractionalSecondPrecisionInNanos() {
		return 1_000_000_000; //seconds
	}

	@Override @SuppressWarnings("deprecation")
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
	public TimeZoneSupport getTimeZoneSupport() {
		return TimeZoneSupport.NORMALIZE;
	}

	@Override
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
	public NameQualifierSupport getNameQualifierSupport() {
		// This method is overridden so the correct value will be returned when
		// DatabaseMetaData is not available.
		return NameQualifierSupport.SCHEMA;
	}

	@Override
	public String getCurrentSchemaCommand() {
		return "select current_schema()";
	}

	@Override
	public boolean supportsDistinctFromPredicate() {
		return true;
	}

	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return true;
	}

	@Override
	public boolean supportsIfExistsBeforeTypeName() {
		return true;
	}

	@Override
	public boolean supportsIfExistsBeforeConstraintName() {
		return true;
	}

	@Override
	public boolean supportsIfExistsAfterAlterTable() {
		// M mode rejects `alter table if exists <table> ...` with a syntax error; under PostgreSQL
		// transaction semantics that aborts the whole schema-generation transaction, so subsequent
		// DDL (create sequence/table) is silently ignored. A mode accepts the IF EXISTS clause.
		return !isMMode();
	}

	@Override
	public String getBeforeDropStatement() {
		// NOTICE: table "nonexistent" does not exist, skipping
		// as a JDBC SQLWarning
		return "set client_min_messages = WARNING";
	}

	@Override
	public String getAlterColumnTypeString(String columnName, String columnType, String columnDefinition) {
		// would need multiple statements to 'set not null'/'drop not null', 'set default'/'drop default', 'set generated', etc
		if ( isMMode() ) {
			// M mode (MySQL-compatible) rejects `alter column ... set data type` (DB2/standard syntax)
			// with "syntax error at or near data". MySQL's `modify column` redefines the whole column,
			// so pass the full columnDefinition to preserve null/default attributes (same as MySQLDialect).
			return "modify column " + columnName + " " + columnDefinition.trim();
		}
		return "alter column " + columnName + " set data type " + columnType;
	}

	@Override
	public boolean supportsAlterColumnType() {
		return true;
	}

	@Override
	public boolean supportsValuesList() {
		// A mode (openGauss PG kernel) supports PostgreSQL `values (..),(..)` derived tables.
		// M mode (MySQL-compatible) rejects that syntax — `values (true),(false)` raises
		// "syntax error at or near '('". Return false so buildStringToBooleanCast (cast string
		// as boolean/YesNo/TrueFalse) and other callers fall back to union all.
		return !isMMode();
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
	public boolean supportsConflictClauseForInsertCTE() {
		return true;
	}

	@Override
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
	public String getCascadeConstraintsString() {
		return " cascade";
	}

	@Override
	public String getQuerySequencesString() {
		// M mode has no information_schema.sequences view; query pg_class (relkind='S') for sequence
		// names and pg_sequence_parameters(oid) for the increment value (its "increment" field), so
		// the extractor can detect existing sequences (avoiding redundant create-sequence calls that
		// fail with "Relation already exists" — M mode rejects `create sequence if not exists`) AND
		// expose the increment value for SequenceInformation.getIncrementValue() and HHH-12973
		// sequence-mismatch detection. pg_class exposes only relname, hence the function call.
		if ( isMMode() ) {
			return "select c.relname as relname, (pg_sequence_parameters(c.oid)).increment as increment from pg_class c where c.relkind='S'";
		}
		return "select * from information_schema.sequences";
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		// M mode: pg_class exposes only relname (no catalog/schema/start/min/max/increment columns that
		// information_schema.sequences provides), so use a extractor that reads relname and skips the rest.
		if ( isMMode() ) {
			return GaussDBMModeSequenceInformationExtractor.INSTANCE;
		}
		return super.getSequenceInformationExtractor();
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LimitLimitHandler.INSTANCE;
	}

	@Override
	public String getForUpdateString(String aliases) {
		return getForUpdateString() + " of " + aliases;
	}

	@Override
	public String getForUpdateString(String aliases, LockOptions lockOptions) {
		// parent's implementation for (aliases, lockOptions) ignores aliases
		if ( aliases.isEmpty() ) {
			LockMode lockMode = lockOptions.getLockMode();
			for ( Map.Entry<String, LockMode> entry : lockOptions.getAliasSpecificLocks() ) {
				// seek the highest lock mode
				if ( entry.getValue().greaterThan(lockMode) ) {
					aliases = entry.getKey();
				}
			}
		}
		LockMode lockMode = lockOptions.getAliasSpecificLockMode( aliases );
		if ( lockMode == null ) {
			lockMode = lockOptions.getLockMode();
		}
		return switch (lockMode) {
			case PESSIMISTIC_READ -> getReadLockString( aliases, lockOptions.getTimeOut() );
			case PESSIMISTIC_WRITE -> getWriteLockString( aliases, lockOptions.getTimeOut() );
			case UPGRADE_NOWAIT, PESSIMISTIC_FORCE_INCREMENT -> getForUpdateNowaitString( aliases );
			case UPGRADE_SKIPLOCKED -> getForUpdateSkipLockedString( aliases );
			default -> "";
		};
	}

	@Override
	public String getNoColumnsInsertString() {
		// M mode (MySQL-compatible) rejects `default values`; use `() values ()`. A mode
		// (Oracle-compatible) supports `default values`.
		return isMMode() ? "() values ()" : "default values";
	}

	@Override
	public String getCaseInsensitiveLike(){
		return "ilike";
	}

	@Override
	public boolean supportsCaseInsensitiveLike() {
		// A mode (openGauss PG kernel) supports ILIKE natively. M mode (MySQL-compatible) does not,
		// so fall back to the lower(col) like lower(pattern) emulation rendered by
		// AbstractSqlAstTranslator#visitLikePredicate.
		return !isMMode();
	}

	@Override
	public GenerationType getNativeValueGenerationStrategy() {
		return GenerationType.SEQUENCE;
	}

	@Override
	public boolean supportsOuterJoinForUpdate() {
		return false;
	}

	@Override
	public boolean useInputStreamToInsertBlob() {
		return false;
	}

	@Override
	public boolean useConnectionToCreateLob() {
		return false;
	}

	@Override
	public String getSelectClauseNullString(int sqlType, TypeConfiguration typeConfiguration) {
		// TODO: adapt this to handle named enum types!
		if ( isMMode() ) {
			// M mode (MySQL-compatible) CAST only accepts MySQL type names; getRawTypeName() returns the
			// PG column type (text/varchar/integer/bigint/numeric/...) which M mode rejects. castType()
			// returns the MySQL name, but may carry a $l/$p/$s size pattern — irrelevant for a null
			// literal, so strip the (...) suffix (MySQL accepts char/decimal/binary without a size).
			return "cast(null as " + castType( sqlType ).replaceAll( "\\(.*\\)", "" ) + ")";
		}
		return "cast(null as " + typeConfiguration.getDdlTypeRegistry().getDescriptor( sqlType ).getRawTypeName() + ")";
	}

	@Override
	public String quoteCollation(String collation) {
		return '\"' + collation + '\"';
	}

	@Override
	public boolean supportsCommentOn() {
		return true;
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
		return "select now()";
	}

	@Override
	public boolean supportsTupleCounts() {
		return true;
	}

	@Override
	public boolean supportsIsTrue() {
		return true;
	}

	@Override
	public boolean requiresParensForTupleDistinctCounts() {
		return true;
	}

	@Override
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		appender.appendSql( bool );
	}

	/**
	 * Words reserved by GaussDB M mode that PostgreSQL treats as non-reserved, so Hibernate would otherwise
	 * emit them unquoted as identifiers. Auto-quoting ({@code hibernate.auto_quote_keyword=true}) is scoped to
	 * this curated set rather than the full ANSI/PostgreSQL keyword set ({@code Dialect#sqlKeywords}), which
	 * over-reports words M mode actually permits as identifiers (e.g. {@code aggregate}) and would false-quote
	 * legitimate identifier-named columns. {@code excluded} is also registered via {@link #registerKeyword} so
	 * it is recognized as a keyword in SQL templates ({@code Template} uses {@code #getKeywords()}).
	 * {@code match} is a MySQL reserved word (M mode) but non-reserved in PostgreSQL, so an unquoted
	 * {@code create table Match (...)} raises a syntax error in M mode; it is added here only for M mode
	 * (A mode keeps {@link #A_MODE_RESERVED_KEYWORDS} so existing A-mode behavior is unchanged).
	 */
	private static final Set<String> M_MODE_RESERVED_KEYWORDS = Set.of( "excluded", "match" );
	private static final Set<String> A_MODE_RESERVED_KEYWORDS = Set.of( "excluded" );

	@Override
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuilder builder, DatabaseMetaData dbMetaData)
			throws SQLException {

		if ( dbMetaData == null ) {
			builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.LOWER );
			builder.setQuotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		}
		// Replicate Dialect#buildIdentifierHelper but apply only the curated M-mode reserved words instead of
		// the full inherited sqlKeywords (which over-reports ANSI/PG words M mode permits, e.g. `aggregate`).
		builder.applyIdentifierCasing( dbMetaData );
		builder.applyReservedWords( isMMode() ? M_MODE_RESERVED_KEYWORDS : A_MODE_RESERVED_KEYWORDS );
		builder.setNameQualifierSupport( getNameQualifierSupport() );
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
	public TemporaryTableStrategy getLocalTemporaryTableStrategy() {
		if ( isMMode() ) {
			// M mode (openGauss PG kernel, MySQL-compatible) supports session-local temporary tables
			// (`CREATE LOCAL TEMPORARY TABLE`), used by LocalTemporaryTableMutationStrategy as the staging
			// area for multi-table mutations — M mode has no RETURNING, so CTE DML is unavailable.
			// StandardLocalTemporaryTableStrategy creates the temp table before each use
			// (BeforeUseAction.CREATE — directly `create local temporary table`, no drop-if-exists) and by
			// default does NOT drop after use (AfterUseAction.NONE), relying on the session to auto-drop.
			// But GaussDB's local temp table is connection-scoped and the connection pool reuses
			// connections, so NONE leaves HT_* tables behind — the next mutation on a reused connection
			// hits "Relation HT_X already exists" and aborts the transaction (patient-zero pollution:
			// JoinedSubclassBulkManipTest passes single-class but fails full-suite with HT_Person already
			// exists + transaction aborted). Override AfterUseAction to DROP so the temp table is dropped
			// after each use. A mode keeps the base default (null — CteMutationStrategy, no temp tables).
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
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		if ( isMMode() ) {
			// M mode (MySQL-compatible, single-node) has no `INSERT/UPDATE/DELETE ... RETURNING`
			// (RETURNING is a distributed-db-only feature here), so `CteMutationStrategy` — which
			// renders `with dml_cte (..) as (dml ... returning ..)` — can't execute. Use a session-local
			// temporary table as the staging area instead: `CREATE TEMPORARY TABLE` is supported in M
			// mode and auto-cleaned per session. The base Dialect temporary-table config (afterUse=CLEAN,
			// beforeUse=NONE) matches the openGauss PG kernel semantics. A mode keeps the CTE strategy.
			return new LocalTemporaryTableMutationStrategy( rootEntityDescriptor, runtimeModelCreationContext );
		}
		return new CteMutationStrategy( rootEntityDescriptor, runtimeModelCreationContext );
	}

	@Override
	public SqmMultiTableInsertStrategy getFallbackSqmInsertStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		if ( isMMode() ) {
			// See getFallbackSqmMutationStrategy — M mode has no RETURNING, so avoid CteInsertStrategy.
			return new LocalTemporaryTableInsertStrategy( rootEntityDescriptor, runtimeModelCreationContext );
		}
		return new CteInsertStrategy( rootEntityDescriptor, runtimeModelCreationContext );
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new GaussDBSqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	@Override
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

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		// Register the type of the out param - GaussDB uses Types.OTHER
		statement.registerOutParameter( col++, Types.OTHER );
		return col;
	}

	@Override
	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
		ps.execute();
		return (ResultSet) ps.getObject( 1 );
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsLobValueChangePropagation() {
		return false;
	}

	@Override
	public boolean supportsUnboundedLobLocatorMaterialization() {
		return false;
	}

	@Override
	public SelectItemReferenceStrategy getGroupBySelectItemReferenceStrategy() {
		return SelectItemReferenceStrategy.POSITION;
	}

	@Override
	public CallableStatementSupport getCallableStatementSupport() {
		// GaussDB M mode is PostgreSQL-compatible for callable statements; reuse the PG support
		// so no-arg function calls render as `select func()` instead of `{?=call func(?)}`,
		// which (via GaussDBCallableStatementSupport, based on Oracle) passes a spurious null parameter.
		return PostgreSQLCallableStatementSupport.INSTANCE;
	}

	@Override
	public ResultSet getResultSet(CallableStatement statement, int position) throws SQLException {
		if ( position != 1 ) {
			throw new UnsupportedOperationException( "GaussDB only supports REF_CURSOR parameters as the first parameter" );
		}
		return (ResultSet) statement.getObject( 1 );
	}

	@Override
	public ResultSet getResultSet(CallableStatement statement, String name) throws SQLException {
		throw new UnsupportedOperationException( "GaussDB only supports accessing REF_CURSOR parameters by position" );
	}

	@Override
	public boolean qualifyIndexName() {
		return false;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		// M mode (MySQL-compatible) rejects bigserial/serial; use MySQL-style
		// `bigint not null auto_increment` + last_insert_id() (verified). A mode keeps
		// the PG-style bigserial (Oracle-compatible, supports it).
		return isMMode() ? MySQLIdentityColumnSupport.INSTANCE : GaussDBIdentityColumnSupport.INSTANCE;
	}

	@Override
	public boolean getDefaultUseGetGeneratedKeys() {
		// M mode: the gsjdbc4 (PostgreSQL) driver implements getGeneratedKeys() by
		// rewriting the INSERT with a RETURNING clause, which single-node GaussDB M mode
		// rejects. Disable it so IdentityGenerator falls back to BasicSelectingDelegate,
		// which runs a plain INSERT then `select last_insert_id()` (verified). A mode
		// keeps the default true (RETURNING works on the openGauss PG kernel).
		return !isMMode();
	}

	@Override
	public boolean supportsExpectedLobUsagePattern() {
		return false;
	}

	@Override
	public NationalizationSupport getNationalizationSupport() {
		return NationalizationSupport.IMPLICIT;
	}

	@Override
	public int getMaxIdentifierLength() {
		return 63;
	}

	@Override
	public boolean supportsStandardArrays() {
		// M mode (MySQL-compatible) has no array column type (syntax error on `type[]`);
		// A mode (Oracle-compatible, openGauss PG kernel) supports arrays. Returning false
		// for M mode also makes the base Dialect.getPreferredSqlTypeCodeForArray() fall back
		// to VARBINARY, so @RequiresDialectFeature(SupportsStructuralArrays) skips array tests.
		return !isMMode();
	}

	@Override
	public boolean supportsJdbcConnectionLobCreation(DatabaseMetaData databaseMetaData) {
		return false;
	}

	@Override
	public boolean supportsMaterializedLobAccess() {
		// Prefer using text and bytea over oid (LOB), because oid is very restricted.
		// If someone really wants a type bigger than 1GB, they should ask for it by using @Lob explicitly
		return false;
	}

	@Override
	public boolean supportsTemporalLiteralOffset() {
		// A mode (openGauss PG kernel) supports `timestamp with time zone '...'` literals; M mode
		// (MySQL-compatible) rejects them (syntax error at "time"), so report false and let
		// appendDateTimeLiteral render a plain `timestamp '...'` literal instead.
		return !isMMode();
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
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
	public String translateExtractField(TemporalUnit unit) {
		return switch (unit) {
			//WEEK means the ISO week number
			case DAY_OF_MONTH -> "day";
			case DAY_OF_YEAR -> "doy";
			case DAY_OF_WEEK -> "dow";
			default -> super.translateExtractField( unit );
		};
	}

	@Override
	public AggregateSupport getAggregateSupport() {
		return GaussDBAggregateSupport.valueOf( this );
	}

	@Override
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
				if ( supportsTemporalLiteralOffset() && temporalAccessor.isSupported( ChronoField.OFFSET_SECONDS ) ) {
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
				if ( supportsTemporalLiteralOffset() && temporalAccessor.isSupported( ChronoField.OFFSET_SECONDS ) ) {
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
	public void appendUUIDLiteral(SqlAppender appender, UUID literal) {
		if ( isMMode() ) {
			// M mode stores UUID as varchar(36) + VarcharUUIDJdbcType (see columnType/registerColumnTypes).
			// Render the literal as a plain string literal instead of the default `cast('...' as uuid)`,
			// which M mode rejects (no native uuid type). A mode keeps the default cast.
			appender.appendSql( "'" );
			appender.appendSql( literal.toString() );
			appender.appendSql( "'" );
			return;
		}
		super.appendUUIDLiteral( appender, literal );
	}

	private String withTimeout(String lockString, Timeout timeout) {
		return switch (timeout.milliseconds()) {
			case Timeouts.NO_WAIT_MILLI -> supportsNoWait() ? lockString + " nowait" : lockString;
			case Timeouts.SKIP_LOCKED_MILLI -> supportsSkipLocked() ? lockString + " skip locked" : lockString;
			default -> lockString;
		};
	}

	@Override
	public String getWriteLockString(Timeout timeout) {
		return withTimeout( getForUpdateString(), timeout );
	}

	@Override
	public String getWriteLockString(String aliases, Timeout timeout) {
		return withTimeout( getForUpdateString( aliases ), timeout );
	}

	@Override
	public String getReadLockString(Timeout timeout) {
		return withTimeout(" for share", timeout );
	}

	@Override
	public String getReadLockString(String aliases, Timeout timeout) {
		return withTimeout(" for share of " + aliases, timeout );
	}

	private String withTimeout(String lockString, int timeout) {
		return switch (timeout) {
			case Timeouts.NO_WAIT_MILLI -> supportsNoWait() ? lockString + " nowait" : lockString;
			case Timeouts.SKIP_LOCKED_MILLI -> supportsSkipLocked() ? lockString + " skip locked" : lockString;
			default -> lockString;
		};
	}

	@Override
	public String getWriteLockString(int timeout) {
		return withTimeout( getForUpdateString(), timeout );
	}

	@Override
	public String getWriteLockString(String aliases, int timeout) {
		return withTimeout( getForUpdateString( aliases ), timeout );
	}

	@Override
	public String getReadLockString(int timeout) {
		return withTimeout(" for share", timeout );
	}

	@Override
	public String getReadLockString(String aliases, int timeout) {
		return withTimeout(" for share of " + aliases, timeout );
	}

	@Override
	public LockingSupport getLockingSupport() {
		return GaussDBLockingSupport.LOCKING_SUPPORT;
	}

	@Override
	public String getForUpdateNowaitString() {
		return supportsNoWait()
				? " for update nowait"
				: getForUpdateString();
	}

	@Override
	public String getForUpdateNowaitString(String aliases) {
		return supportsNoWait()
				? " for update of " + aliases + " nowait"
				: getForUpdateString(aliases);
	}

	@Override
	public String getForUpdateSkipLockedString() {
		return supportsSkipLocked()
				? " for update skip locked"
				: getForUpdateString();
	}

	@Override
	public String getForUpdateSkipLockedString(String aliases) {
		return supportsSkipLocked()
				? " for update of " + aliases + " skip locked"
				: getForUpdateString( aliases );
	}

	@Override
	public boolean supportsNoWait() {
		return true;
	}

	@Override
	public boolean supportsWait() {
		return false;
	}

	@Override
	public boolean supportsSkipLocked() {
		return true;
	}

	@Override
	public boolean supportsInsertReturning() {
		// M mode (MySQL-compatible) rejects INSERT ... RETURNING ("Unsupported function.
		// only supported in distributed database" — single-node GaussDB). A mode
		// (Oracle-compatible, openGauss PG kernel) supports RETURNING.
		return !isMMode();
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
		return false;
	}

	@Override
	public boolean supportsRecursiveCTE() {
		return false;
	}

	@Override
	public boolean supportsOrderByInSubquery() {
		return false;
	}

	@Override
	public boolean supportsFetchClause(FetchClauseType type) {
		return false;
	}

	@Override
	public String getForUpdateString() {
		return " for update";
	}

	@Override
	public boolean supportsFilterClause() {
		return false;
	}

	@Override
	public FunctionalDependencyAnalysisSupport getFunctionalDependencyAnalysisSupport() {
		return FunctionalDependencyAnalysisSupportImpl.TABLE_REFERENCE;
	}

	@Override
	public RowLockStrategy getWriteRowLockStrategy() {
		return RowLockStrategy.TABLE;
	}

	@Override
	public void augmentRecognizedTableTypes(List<String> tableTypesList) {
		super.augmentRecognizedTableTypes( tableTypesList );
		tableTypesList.add( "MATERIALIZED VIEW" );
		tableTypesList.add( "PARTITIONED TABLE" );
	}

	@Override
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
		jdbcTypeRegistry.addDescriptorIfAbsent( GaussDBStructuredJdbcType.INSTANCE );
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
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	@Override
	public Exporter<Table> getTableExporter() {
		return gaussDBTableExporter;
	}

	/**
	 * @return {@code true}, but only because we can "batch" truncate
	 */
	@Override
	public boolean canBatchTruncate() {
		return true;
	}

	@Override
	public String getQueryHintString(String sql, String hints) {
		return "/*+ " + hints + " */ " + sql;
	}

	@Override
	public String addSqlHintOrComment(String sql, QueryOptions queryOptions, boolean commentsEnabled) {
		// GaussDB's extension pg_hint_plan needs the hint to be the first comment
		if ( commentsEnabled && queryOptions.getComment() != null ) {
			sql = prependComment( sql, queryOptions.getComment() );
		}
		if ( queryOptions.getDatabaseHints() != null && !queryOptions.getDatabaseHints().isEmpty() ) {
			sql = getQueryHintString( sql, queryOptions.getDatabaseHints() );
		}
		return sql;
	}

	@Override
	public MutationOperation createOptionalTableUpdateOperation(
			EntityMutationTarget mutationTarget,
			OptionalTableUpdate optionalTableUpdate,
			SessionFactoryImplementor factory) {
		if ( isMMode() ) {
			// M mode (MySQL-compatible): GaussDB MERGE does not support `WHEN MATCHED AND <condition>`,
			// which the base translator's createMergeOperation emits for the optional-table delete branch.
			// Follow an upsert-based path instead: an operation whose insert ignores primary key collisions
			// through GaussDBSqlAstTranslator#visitStandardTableInsert (ON DUPLICATE KEY UPDATE col=col),
			// without relying on MERGE.
			return new OptionalTableUpdateWithUpsertOperation( mutationTarget, optionalTableUpdate, factory );
		}
		// A mode (openGauss Oracle-compatible): use the default OptionalTableUpdateOperation, which does a
		// plain insert and catches the unique-violation to fall back to update. A mode does not support
		// ON CONFLICT, and the M-mode upsert operation above relies on ON DUPLICATE KEY UPDATE, which A
		// mode rejects when it touches primary/unique key columns.
		return super.createOptionalTableUpdateOperation( mutationTarget, optionalTableUpdate, factory );
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
	public int getDefaultIntervalSecondScale() {
		// The maximum scale for `interval second` is 6 unfortunately
		return 6;
	}

	@Override
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}

	@Override
	public boolean supportsFromClauseInUpdate() {
		return true;
	}

	@Override
	public boolean supportsJoinsInDelete() {
		// M mode (MySQL-compatible) natively supports `delete alias from t join ...`,
		// so the joins are rendered directly in the DELETE clause (see GaussDBSqlAstTranslator)
		// and the restriction is used as-is. A mode (openGauss PG kernel) keeps the base
		// behavior, which emulates delete-joins via an `exists(select 1 from (values(0)) d_ ...)`
		// subquery — that emulation relies on the PG-style `(values(0))` dual expression.
		return isMMode();
	}

	@Override
	public boolean supportsBindingNullSqlTypeForSetNull() {
		return true;
	}
}
