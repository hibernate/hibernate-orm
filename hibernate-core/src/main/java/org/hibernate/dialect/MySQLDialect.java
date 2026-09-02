/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import org.hibernate.dialect.identifier.spi.KeywordRegistration;

import org.hibernate.dialect.temporaltype.spi.TemporalValueSemantics;

import org.hibernate.dialect.temporaltype.spi.CurrentTimestampSelection;

import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupport;

import org.hibernate.dialect.temporaltype.spi.TemporalFormatSupport;

import org.hibernate.dialect.temporaltype.spi.CurrentTemporalSupport;

import org.hibernate.dialect.type.spi.DdlTypeBuilder;

import org.hibernate.dialect.type.spi.StandardDdlTypes;

import org.hibernate.dialect.type.spi.TypeSizingProfile;
import org.hibernate.dialect.type.spi.EnumSupport;
import org.hibernate.dialect.type.spi.EnumSupports;
import org.hibernate.dialect.type.spi.ObjectNullBindingStrategy;
import org.hibernate.dialect.type.spi.StringValueSemantics;
import org.hibernate.dialect.lob.spi.LobSupport;
import org.hibernate.dialect.lob.spi.LobSupports;

import org.hibernate.dialect.mutation.spi.MultiTableMutationSupport;


import org.hibernate.dialect.function.spi.Replacer;

import org.hibernate.dialect.schema.spi.MyISAMStorageEngine;

import org.hibernate.dialect.schema.spi.InnoDBStorageEngine;

import org.hibernate.dialect.schema.spi.MySQLStorageEngine;

import org.hibernate.dialect.jdbc.spi.MySQLServerConfiguration;

import org.hibernate.dialect.aggregate.spi.FunctionalDependencyAnalysisSupport;

import org.hibernate.dialect.sql.ast.spi.NullOrdering;


import org.hibernate.dialect.sql.ast.spi.DmlTargetColumnQualifierSupport;

import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.MutationKind;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxCapability;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxSupport;
import org.hibernate.dialect.sql.ast.spi.PredicateSupport;
import org.hibernate.dialect.sql.ast.spi.NullOrderingSupport;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.SingleRowTableSupport;
import org.hibernate.dialect.sql.ast.spi.SetOperationSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;

import jakarta.persistence.TemporalType;
import org.hibernate.Length;
import org.hibernate.QueryTimeoutException;
import org.hibernate.SPI;
import static org.hibernate.SPI.Role.USE;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.aggregate.spi.AggregateSupport;
import org.hibernate.dialect.aggregate.internal.MySQLAggregateSupport;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;
import org.hibernate.dialect.identity.internal.MySQLIdentityColumnSupport;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.namespace.spi.NamespaceSupport;
import org.hibernate.dialect.namespace.spi.NamespaceSupports;
import org.hibernate.dialect.schema.spi.AlterColumnTypeRequest;
import org.hibernate.dialect.schema.spi.ColumnDefinitionRequest;
import org.hibernate.dialect.schema.spi.ConstraintControlMode;
import org.hibernate.dialect.schema.spi.ConstraintDropMode;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.schema.spi.IfExistsSupport;
import org.hibernate.dialect.schema.spi.IndexNameQualification;
import org.hibernate.dialect.schema.spi.SchemaDropSupport;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.LimitLimitHandler;
import org.hibernate.dialect.sequence.internal.NoSequenceSupport;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.sql.ast.internal.MySQLSqlAstTranslator;
import org.hibernate.dialect.temporal.spi.TemporalTableSupport;
import org.hibernate.dialect.temporal.spi.TemporalTableSupports;
import org.hibernate.dialect.temptable.spi.TemporaryTableStrategies;
import org.hibernate.dialect.temptable.spi.TemporaryTableStrategy;
import org.hibernate.dialect.type.spi.MySQLJdbcTypes;
import org.hibernate.dialect.type.spi.SizeStrategy;
import org.hibernate.dialect.type.spi.StandardSizeStrategy;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.dialect.identifier.spi.IdentifierHelperBuildRequest;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.dialect.temporaltype.spi.IntervalType;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.dialect.sql.ast.spi.OptionalTableUpdateOperationRequest;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.InformationExtractor;
import org.hibernate.tool.schema.extract.spi.InformationExtractors;
import org.hibernate.type.NullType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.EnumJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.NullJdbcType;
import org.hibernate.type.descriptor.jdbc.OrdinalEnumJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import java.sql.Types;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static java.lang.Integer.parseInt;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.cfg.SchemaToolingSettings.STORAGE_ENGINE;
import static org.hibernate.dialect.lock.internal.MySQLLockingSupport.MYSQL_LOCKING_SUPPORT;
import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.jdbc.spi.JdbcExceptionHelper.extractSqlState;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.split;
import static org.hibernate.type.SqlTypes.BIGINT;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BIT;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.DECIMAL;
import static org.hibernate.type.SqlTypes.DOUBLE;
import static org.hibernate.type.SqlTypes.FLOAT;
import static org.hibernate.type.SqlTypes.GEOMETRY;
import static org.hibernate.type.SqlTypes.INTEGER;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.LONG32NVARCHAR;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.LONG32VARCHAR;
import static org.hibernate.type.SqlTypes.NCHAR;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.NUMERIC;
import static org.hibernate.type.SqlTypes.NVARCHAR;
import static org.hibernate.type.SqlTypes.REAL;
import static org.hibernate.type.SqlTypes.SMALLINT;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.SqlTypes.VARCHAR;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsDate;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsLocalTime;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTimestampWithMicros;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTimestampWithMillis;
import static org.hibernate.dialect.literal.spi.ZeroOffsetLiteralStyle.NUMERIC_OFFSET;

/// A {@linkplain Dialect SQL dialect} for MySQL 8 and above.
///
/// Please refer to the
/// <a href="https://dev.mysql.com/doc/refman/9.1/en/">MySQL documentation</a>.
///
/// This class is also the supported family base for provider Dialects derived
/// from MySQL. Provider subclasses must invoke a constructor classified
/// {@link SPI.Role#IMPLEMENT IMPLEMENT}. The generated SPI inventory identifies
/// the constructors and members covered by this type-level implementation
/// contract; unclassified implementation details are not provider extension
/// points.
///
/// @author Gavin King
/// @since 8.0
@SPI({ USE, IMPLEMENT })
public class MySQLDialect extends Dialect implements CurrentTemporalSupport, TemporalFormatSupport, TemporalOperationSupport {
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
	@SPI(IMPLEMENT)
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

	private static final DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 8 );

	/**
	 * On MySQL, 1GB or {@code 2^30 - 1} is the maximum size that a char value can be casted.
	 */
	private static final int MAX_CHAR_SIZE = (1 << 30) - 1;

	private final MySQLStorageEngine storageEngine;
	private final SchemaDropSupport schemaDropSupport;

	private final SizeStrategy sizeStrategy = new StandardSizeStrategy( this ) {
		@Override
		public Size resolveSize(
				JdbcType jdbcType,
				JavaType<?> javaType,
				Integer precision,
				Integer scale,
				Long length) {
			switch ( jdbcType.getDdlTypeCode() ) {
				case BIT:
					// MySQL allows BIT with a length up to 64 (less the default length 255)
					if ( length != null ) {
						return Size.length( Math.min( Math.max( length, 1 ), 64 ) );
					}
				case FLOAT:
				case DOUBLE:
				case REAL:
					//MySQL doesn't let you cast to DOUBLE/FLOAT
					//but don't just return 'decimal' because
					//the default scale is 0 (no decimal places)
					Size size = super.resolveSize( jdbcType, javaType, precision, scale, length );
					//cast() on MySQL does not behave sensibly if
					//we set scale > 20
					size.setScale( Math.min( size.getPrecision(), 20 ) );
					return size;
				case BLOB:
				case NCLOB:
				case CLOB:
					return super.resolveSize(
							jdbcType,
							javaType,
							precision,
							scale,
							length == null ? getTypeSizingProfile().defaultLobLength() : length
					);
				default:
					return super.resolveSize( jdbcType, javaType, precision, scale, length );
			}
		}
	};

	private final int maxVarcharLength;
	private final int maxVarbinaryLength;
	private final TypeSizingProfile typeSizingProfile;

	private final boolean noBackslashEscapesEnabled;

	@SPI( IMPLEMENT )
	public MySQLDialect() {
		this( MINIMUM_VERSION );
	}

	@SPI( IMPLEMENT )
	public MySQLDialect(DatabaseVersion version) {
		this(
				version,
				new MySQLServerConfiguration(
						4,
						false,
						Environment.getProperties().getProperty( STORAGE_ENGINE )
				)
		);
	}

	@SPI( IMPLEMENT )
	public MySQLDialect(DatabaseVersion version, MySQLServerConfiguration serverConfiguration) {
		super( version );
		maxVarcharLength = maxVarcharLength( getMySQLVersion(), serverConfiguration.getBytesPerCharacter() ); //conservative assumption
		maxVarbinaryLength = maxVarbinaryLength( getMySQLVersion() );
		typeSizingProfile = TypeSizingProfile.builder( super.getTypeSizingProfile() )
				.defaultLobLength( Length.LONG32 )
				.floatPrecision( 23 )
				.maxVarcharLength( maxVarcharLength ).maxVarcharCapacity( maxVarcharLength )
				.maxNVarcharLength( maxVarcharLength ).maxNVarcharCapacity( maxVarcharLength )
				.maxVarbinaryLength( maxVarbinaryLength ).maxVarbinaryCapacity( maxVarbinaryLength )
				.build();
		noBackslashEscapesEnabled = serverConfiguration.isNoBackslashEscapesEnabled();
		storageEngine = createStorageEngine( serverConfiguration.getConfiguredStorageEngine() );
		schemaDropSupport = new SchemaDropSupport(
				List.of(),
				storageEngine.dropConstraints() ? ConstraintDropMode.EXPLICIT : ConstraintDropMode.IMPLICIT,
				""
		);
	}

	@SPI( IMPLEMENT )
	public MySQLDialect(DialectResolutionInfo info) {
		this( createVersion( info, MINIMUM_VERSION ),
				MySQLServerConfiguration.fromDialectResolutionInfo( info ) );
	}

	@Override
	public TypeSizingProfile getTypeSizingProfile() {
		return typeSizingProfile;
	}

	protected static DatabaseVersion createVersion(DialectResolutionInfo info, DatabaseVersion defaultVersion) {
		final String versionString = info.getDatabaseVersion();
		if ( versionString != null ) {
			final String[] components = split( ".-", versionString );
			if ( components.length >= 3 ) {
				try {
					final int majorVersion = parseInt( components[0] );
					final int minorVersion = parseInt( components[1] );
					final int patchLevel = parseInt( components[2] );
					return DatabaseVersion.make( majorVersion, minorVersion, patchLevel );
				}
				catch (NumberFormatException ex) {
					// Ignore
				}
			}
		}
		return info.makeCopyOrDefault( defaultVersion );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_VERSION;
	}

	private MySQLStorageEngine createStorageEngine(String configuredStorageEngine) {
		return configuredStorageEngine == null
				? getDefaultMySQLStorageEngine()
				: switch ( configuredStorageEngine.toLowerCase(Locale.ROOT) ) {
					case "innodb" -> InnoDBStorageEngine.INSTANCE;
					case "myisam" -> MyISAMStorageEngine.INSTANCE;
					default -> throw new UnsupportedOperationException(
							"The '" + storageEngine + "' storage engine is not supported" );
				};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		return switch (sqlTypeCode) {
			// HHH-6935: Don't use "boolean" i.e. tinyint(1) due to JDBC ResultSetMetaData
			case BOOLEAN -> "bit";

			case TIMESTAMP -> "datetime($p)";
			case TIMESTAMP_WITH_TIMEZONE -> "timestamp($p)";

			case NUMERIC -> columnType( DECIMAL ); // it's just a synonym

			// MySQL strips space characters from any value stored in a char column, which
			// is especially pathological in the case of storing characters in char(1)
			case CHAR -> "varchar($l)";

			// on MySQL 8, the nchar/nvarchar types use a deprecated character set
			case NCHAR, NVARCHAR -> "varchar($l) character set utf8mb4";

			// the maximum long LOB length is 4_294_967_295, bigger than any Java string
			case BLOB -> "longblob";
			case NCLOB -> "longtext character set utf8mb4";
			case CLOB -> "longtext";

			default -> super.columnType( sqlTypeCode );
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public LobSupport getLobSupport() {
		return LobSupports.noCapacityPromotion();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public StringValueSemantics getStringValueSemantics() {
		return StringValueSemantics.CHAR_TRAILING_SPACES_STRIPPED;
	}
	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		// Use the true/false constants since these evaluate to true/false literals in JSON functions
		appender.appendSql( bool );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String castType(int sqlTypeCode) {
		return switch (sqlTypeCode) {
			// special case for casting to Boolean
			case BOOLEAN, BIT -> "unsigned";
			// MySQL doesn't let you cast to INTEGER/BIGINT/TINYINT
			case TINYINT, SMALLINT, INTEGER, BIGINT -> "signed";
			// MySQL doesn't let you cast to DOUBLE/FLOAT
			// but don't just return 'decimal' because
			// the default scale is 0 (no decimal places)
			case FLOAT, REAL, DOUBLE -> getMySQLVersion().isSameOrAfter( 8, 0, 17 )
					// In newer versions of MySQL, casting to float/double is supported
					? super.castType( sqlTypeCode )
					: "decimal($p,$s)";
			// MySQL doesn't let you cast to TEXT/LONGTEXT
			case CHAR, VARCHAR, LONG32VARCHAR, CLOB -> "char";
			case NCHAR, NVARCHAR, LONG32NVARCHAR, NCLOB -> "char character set utf8mb4";
			// MySQL doesn't let you cast to BLOB/TINYBLOB/LONGBLOB
			case BINARY, VARBINARY, LONG32VARBINARY, BLOB -> "binary";
			default -> super.castType(sqlTypeCode);
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final var ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		// MySQL 5.7 brings JSON native support with a dedicated datatype
		// https://dev.mysql.com/doc/refman/5.7/en/json.html
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( JSON, "json", this ) );

		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( GEOMETRY, "geometry", this ) );

		// MySQL has approximately one million text and blob types. We have
		// already registered longtext + longblob via the regular method,
		// but we still need to do the rest of them here.

		final int maxTinyLobLen = 255;
		final int maxLobLen = 65_535;
		final int maxMediumLobLen = 16_777_215;

		ddlTypeRegistry.addDescriptor(
				StandardDdlTypes.builder( CHAR, columnType( CHAR ), this )
						.lobKind( DdlTypeBuilder.LobKind.NONE )
						.castTypeNamePattern( "char($l)" )
						.castTypeName( castType( CHAR ) )
						.build()
		);

		final var varcharBuilder =
				StandardDdlTypes.builder( VARCHAR, columnType( CLOB ), this )
						.lobKind( DdlTypeBuilder.LobKind.BIGGEST )
						.parameterizedCastTypeName( this::charCastType )
						.castTypeName( castType( CHAR ) )
						.withTypeCapacity( getTypeSizingProfile().maxVarcharLength(), "varchar($l)" )
						.withTypeCapacity( maxMediumLobLen, "mediumtext" );
		if ( getTypeSizingProfile().maxVarcharLength() < maxLobLen ) {
			varcharBuilder.withTypeCapacity( maxLobLen, "text" );
		}
		ddlTypeRegistry.addDescriptor( varcharBuilder.build() );

		// do not use nchar/nvarchar/ntext because these
		// types use a deprecated character set on MySQL 8
		final var nvarcharBuilder =
				StandardDdlTypes.builder( NVARCHAR, columnType( NCLOB ), this )
						.lobKind( DdlTypeBuilder.LobKind.BIGGEST )
						.parameterizedCastTypeName( this::charCastType )
						.castTypeName( castType( NCHAR ) )
						.withTypeCapacity( getTypeSizingProfile().maxVarcharLength(), "varchar($l) character set utf8mb4" )
						.withTypeCapacity( maxMediumLobLen, "mediumtext character set utf8mb4" );
		if ( getTypeSizingProfile().maxVarcharLength() < maxLobLen ) {
			nvarcharBuilder.withTypeCapacity( maxLobLen, "text character set utf8mb4" );
		}
		ddlTypeRegistry.addDescriptor( nvarcharBuilder.build() );

		final var varbinaryBuilder =
				StandardDdlTypes.builder( VARBINARY, columnType( BLOB ), this )
						.lobKind( DdlTypeBuilder.LobKind.BIGGEST )
						.castTypeNamePattern( columnType( BINARY ) )
						.castTypeName( castType( BINARY ) )
						.withTypeCapacity( getTypeSizingProfile().maxVarbinaryLength(), "varbinary($l)" )
						.withTypeCapacity( maxMediumLobLen, "mediumblob" );
		if ( getTypeSizingProfile().maxVarbinaryLength() < maxLobLen ) {
			varbinaryBuilder.withTypeCapacity( maxLobLen, "blob" );
		}
		ddlTypeRegistry.addDescriptor( varbinaryBuilder.build() );

		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( LONG32VARBINARY,
				columnType( BLOB ), castType( BINARY ), this ) );
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( LONG32VARCHAR,
				columnType( CLOB ), castType( CHAR ), this ) );
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( LONG32NVARCHAR,
				columnType( CLOB ), castType( CHAR ), this ) );

		ddlTypeRegistry.addDescriptor(
				StandardDdlTypes.builder( BLOB, columnType( BLOB ), this ).castTypeName( castType( BINARY ) )
						.withTypeCapacity( maxTinyLobLen, "tinyblob" )
						.withTypeCapacity( maxMediumLobLen, "mediumblob" )
						.withTypeCapacity( maxLobLen, "blob" )
						.build()
		);

		ddlTypeRegistry.addDescriptor(
				StandardDdlTypes.builder( CLOB, columnType( CLOB ), this ).castTypeName( castType( CHAR ) )
						.withTypeCapacity( maxTinyLobLen, "tinytext" )
						.withTypeCapacity( maxMediumLobLen, "mediumtext" )
						.withTypeCapacity( maxLobLen, "text" )
						.build()
		);

		ddlTypeRegistry.addDescriptor(
				StandardDdlTypes.builder( NCLOB, columnType( NCLOB ), this ).castTypeName( castType( NCHAR ) )
						.withTypeCapacity( maxTinyLobLen, "tinytext character set utf8mb4" )
						.withTypeCapacity( maxMediumLobLen, "mediumtext character set utf8mb4" )
						.withTypeCapacity( maxLobLen, "text character set utf8mb4" )
						.build()
		);

		ddlTypeRegistry.addDescriptor( StandardDdlTypes.nativeEnum( this, "char", this::charCastType ) );
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.nativeOrdinalEnum( this ) );
	}

	private String charCastType(int length) {
		return length > MAX_CHAR_SIZE ? "char" : "char(" + length + ")";
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public AggregateSupport getAggregateSupport() {
		return MySQLAggregateSupport.forMySQL( this );
	}

	private static int maxVarbinaryLength(DatabaseVersion version) {
		return 65_535;
	}

	private static int maxVarcharLength(DatabaseVersion version, int bytesPerCharacter) {
		return switch (bytesPerCharacter) {
			case 1 -> 65_535;
			case 2 -> 32_767;
			case 3 -> 21_844;
			default -> 16_383;
		};
	}

	public boolean isNoBackslashEscapesEnabled() {
		return noBackslashEscapesEnabled;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendDefinition(SqlAppender appender, ColumnDefinitionRequest request) {
		super.appendDefinition( appender, request );
		// Good job MySQL https://dev.mysql.com/doc/refman/8.0/en/timestamp-initialization.html
		// If the explicit_defaults_for_timestamp system variable is enabled, TIMESTAMP columns
		// permit NULL values only if declared with the NULL attribute.
		if ( request.nullable()
				&& request.sqlType().regionMatches( true, 0, "timestamp", 0, "timestamp".length() ) ) {
			appender.appendSql( " null" );
		}
	}

	public DatabaseVersion getMySQLVersion() {
		return super.getVersion();
	}

	@Override
	public SizeStrategy getSizeStrategy() {
		return sizeStrategy;
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
			case Types.BIT:
				return jdbcTypeRegistry.getDescriptor( Types.BOOLEAN );
			case Types.BINARY:
				if ( "GEOMETRY".equals( columnTypeName ) ) {
					jdbcTypeCode = GEOMETRY;
				}
				break;
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
	@SPI({ USE, IMPLEMENT })
	public int resolveSqlTypeLength(
			String columnTypeName,
			int jdbcTypeCode,
			int precision,
			int scale,
			int displaySize) {
		// It seems MariaDB/MySQL return the precision in bytes depending on the charset,
		// so to detect whether we have a single character here, we check the display size
		return jdbcTypeCode == Types.CHAR && precision <= 4 ? displaySize : precision;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public int getPreferredSqlTypeCodeForBoolean() {
		return Types.BIT;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public int getPreferredSqlTypeCodeForArray() {
		return SqlTypes.JSON_ARRAY;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		final var functionFactory = new CommonFunctionFactory( functionContributions );
		final var basicTypeRegistry = functionContributions.getTypeConfiguration().getBasicTypeRegistry();
		final var functionRegistry = functionContributions.getFunctionRegistry();

		functionFactory.soundex();
		functionFactory.radians();
		functionFactory.degrees();
		functionFactory.cot();
		functionFactory.log();
		functionFactory.log2();
		functionFactory.log10();
		functionFactory.trim2();
		functionFactory.octetLength();
		functionFactory.reverse();
		functionFactory.space();
		functionFactory.repeat();
		functionFactory.pad_space();
		functionFactory.yearMonthDay();
		functionFactory.hourMinuteSecond();
		functionFactory.dayofweekmonthyear();
		functionFactory.weekQuarter();
		functionFactory.daynameMonthname();
		functionFactory.lastDay();
		functionFactory.date();
		functionFactory.timestamp();
		time(functionContributions);

		functionFactory.utcDateTimeTimestamp();
		functionFactory.rand();
		functionFactory.crc32();
		functionFactory.sha1();
		functionFactory.sha2();
		functionFactory.bitLength();
		functionFactory.octetLength();
		functionFactory.ascii();
		functionFactory.instr();
		functionFactory.substr();
		//also natively supports ANSI-style substring()
		functionFactory.position();
		functionFactory.nowCurdateCurtime();
		functionFactory.trunc_truncate();
		functionFactory.insert();
		functionFactory.bitandorxornot_operator();
		functionFactory.bitAndOr();
		functionFactory.stddev();
		functionFactory.stddevPopSamp();
		functionFactory.variance();
		functionFactory.varPopSamp();
		functionFactory.datediff();
		functionFactory.adddateSubdateAddtimeSubtime();
		functionFactory.format_dateFormat();
		functionFactory.makedateMaketime();
		functionFactory.localtimeLocaltimestamp();

		// pi() produces a value with 7 digits unless we're explicit
		functionRegistry.patternDescriptorBuilder( "pi", "cast(pi() as double)" )
				.setInvariantType( basicTypeRegistry.resolve( StandardBasicTypes.DOUBLE ) )
				.setExactArgumentCount( 0 )
				.setArgumentListSignature( "" )
				.register();

		// By default char() produces a binary string, not a character string.
		// (Note also that char() is actually a variadic function in MySQL.)
		functionRegistry.patternDescriptorBuilder( "chr", "char(?1 using ascii)" )
				.setInvariantType(basicTypeRegistry.resolve( StandardBasicTypes.CHARACTER ))
				.setExactArgumentCount(1)
				.setParameterTypes(FunctionParameterType.INTEGER)
				.register();
		functionRegistry.registerAlternateKey( "char", "chr" );

		// MySQL timestamp type defaults to precision 0 (seconds) but
		// we want the standard default precision of 6 (microseconds)
		functionFactory.sysdateExplicitMicros();
		if ( getMySQLVersion().isSameOrAfter( 8, 0, 2 ) ) {
			functionFactory.windowFunctions();
			if ( getMySQLVersion().isSameOrAfter( 8, 0, 11 ) ) {
				functionFactory.hypotheticalOrderedSetAggregates_windowEmulation();
			}
		}

		functionFactory.listagg_groupConcat();

		registerJsonFunctions( functionFactory );

		functionFactory.unnest_emulated();

		// TODO: which one is correct??
		functionFactory.regexpLike_regexp();
		functionFactory.regexpLike();

		if ( getCteSupport().supports( CteSupport.RecursiveFeature.RECURSIVE ) ) {
			functionFactory.generateSeries_recursive( getMaximumSeriesSize(), false, false );
		}

		functionFactory.hex( "hex(?1)" );
		functionFactory.sha( "unhex(sha2(?1, 256))" );
		functionFactory.md5( "unhex(md5(?1))" );
	}

	protected static void registerJsonFunctions(CommonFunctionFactory functionFactory) {
		functionFactory.jsonValue_mysql();
		functionFactory.jsonQuery_mysql();
		functionFactory.jsonExists_mysql();
		functionFactory.jsonObject_mysql();
		functionFactory.jsonArray_mysql();
		functionFactory.jsonArrayAgg_mysql();
		functionFactory.jsonObjectAgg_mysql();
		functionFactory.jsonSet_mysql();
		functionFactory.jsonRemove_mysql();
		functionFactory.jsonReplace_mysql();
		functionFactory.jsonInsert_mysql();
		functionFactory.jsonMergepatch_mysql();
		functionFactory.jsonArrayAppend_mysql();
		functionFactory.jsonArrayInsert_mysql();
		functionFactory.jsonTable_mysql();
	}

	/**
	 * MySQL doesn't support the {@code generate_series} function or {@code lateral} recursive CTEs,
	 * so it has to be emulated with a top level recursive CTE which requires an upper bound on the amount
	 * of elements that the series can return.
	 */
	protected int getMaximumSeriesSize() {
		// The maximum recursion depth of MySQL
		return 1000;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );

		final var jdbcTypeRegistry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();

		jdbcTypeRegistry.addDescriptorIfAbsent( SqlTypes.JSON, MySQLJdbcTypes.castingJson() );
		jdbcTypeRegistry.addTypeConstructorIfAbsent( MySQLJdbcTypes.castingJsonArrayConstructor() );

		// MySQL requires a custom binder for binding untyped nulls with the NULL type
		typeContributions.contributeJdbcType( NullJdbcType.INSTANCE );

		// Until we remove StandardBasicTypes, we have to keep this
		typeContributions.contributeType(
				new NullType(
						NullJdbcType.INSTANCE,
						typeContributions.getTypeConfiguration()
								.getJavaTypeRegistry()
								.resolveDescriptor( Object.class )
				)
		);

		jdbcTypeRegistry.addDescriptor( EnumJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( OrdinalEnumJdbcType.INSTANCE );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new MySQLSqlAstTranslator<>( request, MySQLDialect.this );
			}
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String castPattern(CastType from, CastType to) {
		if ( to == CastType.INTEGER_BOOLEAN ) {
			switch ( from ) {
				case STRING:
				case INTEGER:
				case LONG:
				case YN_BOOLEAN:
				case TF_BOOLEAN:
				case BOOLEAN:
					break;
				default:
					// MySQL/MariaDB don't support casting to bit
					return "abs(sign(?1))";
			}
		}
		return super.castPattern( from, to );
	}

	private void time(FunctionContributions queryEngine) {
		queryEngine.getFunctionRegistry()
				.namedDescriptorBuilder( "time" )
				.setExactArgumentCount( 1 )
				.setInvariantType( queryEngine.getTypeConfiguration().getBasicTypeRegistry()
						.resolve( StandardBasicTypes.STRING ) )
				.register();
	}

	/**
	 * MySQL 5.7 precision defaults to seconds, but microseconds is better
	 */
	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTimestamp() {
		return "current_timestamp(6)";
	}

	// for consistency, we could do this: but I decided not to
	// because it seems to me that fractional seconds can't possibly
	// be meaningful in a time, as opposed to a timestamp
//	@Override
//	public String currentTime() {
//		return getMySQLVersion().isBefore( 5, 7 ) ? super.currentTimestamp() : "current_time(6)";
//	}

	/**
	 * {@code microsecond} is the smallest unit for
	 * {@code timestampadd()} and {@code timestampdiff()},
	 * and the highest precision for a {@code timestamp}.
	 */
	@Override
	@SPI({ USE, IMPLEMENT })
	public long fractionalSecondPrecisionInNanos() {
		return 1_000; //microseconds
	}

	/**
	 * MySQL supports a limited list of temporal fields in the
	 * extract() function, but we can emulate some of them by
	 * using the appropriate named functions instead of
	 * extract().
	 * <p>
	 * Thus, the additional supported fields are
	 * {@link TemporalUnit#DAY_OF_YEAR},
	 * {@link TemporalUnit#DAY_OF_MONTH},
	 * {@link TemporalUnit#DAY_OF_YEAR}.
	 * <p>
	 * In addition, the field {@link TemporalUnit#SECOND} is
	 * redefined to include microseconds.
	 */
	@Override
	@SPI({ USE, IMPLEMENT })
	public String extractPattern(TemporalUnit unit) {
		return switch (unit) {
			case SECOND -> "(second(?2)+microsecond(?2)/1e6)";
			case WEEK -> "weekofyear(?2)"; // same as week(?2,3), the ISO week
			case DAY_OF_WEEK -> "dayofweek(?2)";
			case DAY_OF_MONTH -> "dayofmonth(?2)";
			case DAY_OF_YEAR -> "dayofyear(?2)";
			//TODO: case WEEK_YEAR: yearweek(?2, 3)/100
			case EPOCH -> "unix_timestamp(?2)";
			default -> "?1(?2)";
		};
	}

	@Override @SuppressWarnings("deprecation")
	@SPI({ USE, IMPLEMENT })
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		return switch (unit) {
			case NANOSECOND -> "timestampadd(microsecond,(?2)/1e3,?3)";
			case NATIVE -> "timestampadd(microsecond,?2,?3)";
			default -> "timestampadd(?1,?2,?3)";
		};
	}

	@Override @SuppressWarnings("deprecation")
	@SPI({ USE, IMPLEMENT })
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		return switch (unit) {
			case NANOSECOND -> "timestampdiff(microsecond,?2,?3)*1e3";
			case NATIVE -> "timestampdiff(microsecond,?2,?3)";
			default -> "timestampdiff(?1,?2,?3)";
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalValueSemantics getTemporalValueSemantics() {
		return getMySQLVersion().isSameOrAfter( 8, 0, 19 )
				? TemporalValueSemantics.OFFSET_LITERALS
				: TemporalValueSemantics.STANDARD;
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
				appender.appendSql( "timestamp '" );
				appendAsTimestampWithMicros(
						appender,
						temporalAccessor,
						getTemporalValueSemantics().supportsLiteralOffset(),
						jdbcTimeZone,
						NUMERIC_OFFSET
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
				appender.appendSql( "timestamp '" );
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
				appender.appendSql( "timestamp '" );
				appendAsTimestampWithMillis( appender, calendar, jdbcTimeZone );
				appender.appendSql( '\'' );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean supports(org.hibernate.dialect.constraint.spi.CheckConstraintPlacement placement) {
		return placement == org.hibernate.dialect.constraint.spi.CheckConstraintPlacement.TABLE
				|| getMySQLVersion().isSameOrAfter( 8, 0, 16 );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public EnumSupport getEnumSupport() {
		return EnumSupports.inline();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String getQueryHintString(String query, String hints) {
		return org.hibernate.dialect.queryhint.spi.QueryHints.addUseIndexHint( query, hints );
	}

	/**
	 * No support for sequences.
	 */
	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		return NoSequenceSupport.getInstance();
	}

	@SPI({ IMPLEMENT, SUPPLY })
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}

	private static final ViolatedConstraintNameExtractor EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor( sqle -> switch ( sqle.getErrorCode() ) {
				case 1062 -> extractUsingTemplate( " for key '", "'", sqle.getMessage() );
				case 1451, 1452 -> extractUsingTemplate( " CONSTRAINT `", "`", sqle.getMessage() );
				case 3819-> extractUsingTemplate( " constraint '", "'", sqle.getMessage() );
				case 1048 -> extractUsingTemplate( "Column '", "'", sqle.getMessage() );
				case 1364 -> extractUsingTemplate( "Field '", "'", sqle.getMessage() );
				default -> null;
			} );

	@Override
	@SPI({ USE, IMPLEMENT })
	public IndexNameQualification nameQualification() {
		return IndexNameQualification.UNQUALIFIED;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String renderAddConstraint(
			org.hibernate.dialect.constraint.spi.ForeignKeyConstraintRequest request) {
		if ( request.isExplicitDefinition() ) {
			return super.renderAddConstraint( request );
		}
		final String cols = String.join( ", ", request.sourceColumnNames() );
		final String referencedCols = String.join( ", ", request.targetColumnNames() );
		return String.format(
				"add constraint %s foreign key (%s) references %s (%s)",
				request.constraintName(),
				cols,
				request.referencedTableName(),
				referencedCols
		);
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
	public String alterColumnType(AlterColumnTypeRequest request) {
		// no way to change just the column type, leaving other attributes intact
		return "modify column " + request.columnName() + " " + request.columnDefinition().trim();
	}

	@Override
	public LimitHandler getLimitHandler() {
		//also supports LIMIT n OFFSET m
		return LimitLimitHandler.INSTANCE;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public char closeQuote() {
		return '`';
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public char openQuote() {
		return '`';
	}

	/// Treat MySQL databases as catalogs for namespace lifecycle operations.
	///
	/// Although MySQL accepts `create schema` as a synonym for `create database`,
	/// Hibernate follows the JDBC driver's catalog model because
	/// [java.sql.DatabaseMetaData#supportsSchemasInDataManipulation()] returns false.
	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NamespaceSupport getNamespaceSupport() {
		return NamespaceSupports.catalogsAsDatabases();
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
	@SPI({ USE, IMPLEMENT })
	public boolean addPartitionKeyToPrimaryKey() {
		return true;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public org.hibernate.dialect.schema.spi.SchemaCommentSupport getSchemaCommentSupport() {
		return org.hibernate.dialect.schema.spi.SchemaCommentSupports.mysqlInline();
	}

	@Override
	public NullOrderingSupport getNullOrderingSupport() {
		return NullOrderingSupport.builder( super.getNullOrderingSupport() )
				.defaultOrdering( NullOrdering.SMALLEST )
				.capability( NullOrderingSupport.Capability.NULLS_FIRST_LAST, false )
				.build();
	}

	@Override
	public MultiTableMutationSupport getMultiTableMutationSupport() {
		return MultiTableMutationSupport.LOCAL_TEMPORARY_TABLE;
	}

	@Override
	public TemporaryTableStrategy getLocalTemporaryTableStrategy() {
		return TemporaryTableStrategies.mysqlLocal();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public int getMaxAliasLength() {
		// Max alias length is 256, but Hibernate needs to add "uniqueing info" so we account for that
		return 246;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public int getMaxIdentifierLength() {
		return 64;
	}

	@Override
	public PredicateSupport getPredicateSupport() {
		return PredicateSupport.builder( super.getPredicateSupport() )
				.capabilities(
						PredicateSupport.Capability.DISTINCT_FROM,
						PredicateSupport.Capability.TRUTHNESS
				)
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public CurrentTimestampSelection getCurrentTimestampSelection() {
		return CurrentTimestampSelection.prepared( "select now()" );
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			switch ( sqlException.getErrorCode() ) {
				case 1205: // ER_LOCK_WAIT_TIMEOUT
					return new LockTimeoutException( message, sqlException, sql );
				case 3572: // ER_LOCK_NOWAIT
				case 1207: // ER_READ_ONLY_TRANSACTION
				case 1206: // ER_LOCK_TABLE_FULL
					return new LockAcquisitionException( message, sqlException, sql );
				case 3024: // ER_QUERY_TIMEOUT
				case 1317: // ER_QUERY_INTERRUPTED
					return new QueryTimeoutException( message, sqlException, sql );
				case 1062:
					// Unique constraint violation
					return new ConstraintViolationException( message, sqlException, sql, ConstraintKind.UNIQUE,
							getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
				case 1048, 1364:
					// Null constraint violation
					return new ConstraintViolationException( message, sqlException, sql, ConstraintKind.NOT_NULL,
							getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
				case 1451, 1452:
					// Foreign key constraint violation
					return new ConstraintViolationException( message, sqlException, sql, ConstraintKind.FOREIGN_KEY,
							getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
				case 3819:
					// Check constraint violation
					return new ConstraintViolationException( message, sqlException, sql, ConstraintKind.CHECK,
							getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
			}

			final String sqlState = extractSqlState( sqlException );
			if ( sqlState != null ) {
				switch ( sqlState ) {
					case "41000":
						return new LockTimeoutException( message, sqlException, sql );
					case "40001":
						return new LockAcquisitionException( message, sqlException, sql );
				}
			}

			return null;
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NameQualifierSupport getNameQualifierSupport() {
		return NameQualifierSupport.CATALOG;
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuildRequest request) {
		final var builder = request.builder();
		if ( !request.jdbcMetadata().isJdbcMetadataAccessible() ) {
			builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.MIXED );
			builder.setQuotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		}
		return super.buildIdentifierHelper( request );
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return MySQLIdentityColumnSupport.INSTANCE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public boolean isJdbcLogWarningsEnabledByDefault() {
		return false;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean supportsOnDeleteAction(org.hibernate.annotations.OnDeleteAction action) {
		return storageEngine.supportsOnDeleteAction( action );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String tableCreationOptions() {
		return storageEngine.getTableTypeString( "engine" );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean requiresSelfReferentialForeignKeyNullification() {
		return storageEngine.requiresSelfReferentialForeignKeyNullification();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SchemaDropSupport getSchemaDropSupport() {
		return schemaDropSupport;
	}

	/// Select the storage engine used when none is explicitly configured.
	///
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	protected MySQLStorageEngine getDefaultMySQLStorageEngine() {
		return InnoDBStorageEngine.INSTANCE;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendLiteral(SqlAppender appender, String literal) {
		appender.appendSql( '\'' );
		for ( int i = 0; i < literal.length(); i++ ) {
			final char c = literal.charAt( i );
			switch ( c ) {
				case '\'':
					appender.appendSql( '\'' );
					break;
				case '\\':
					if ( !noBackslashEscapesEnabled ) {
						// See https://dev.mysql.com/doc/refman/8.0/en/sql-mode.html#sqlmode_no_backslash_escapes
						appender.appendSql( '\\' );
					}
					break;
			}
			appender.appendSql( c );
		}
		appender.appendSql( '\'' );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendFormat(SqlAppender appender, String format) {
		appender.appendSql( datetimeFormat( format ).result() );
	}

	public static Replacer datetimeFormat(String format) {
		return new Replacer( format, "'", "" )
				.replace("%", "%%")

				//year
				.replace("yyyy", "%Y")
				.replace("yyy", "%Y")
				.replace("yy", "%y")
				.replace("y", "%Y")

				//month of year
				.replace("MMMM", "%M")
				.replace("MMM", "%b")
				.replace("MM", "%m")
				.replace("M", "%c")

				//week of year
				.replace("ww", "%v")
				.replace("w", "%v")
				//year for week
				.replace("YYYY", "%x")
				.replace("YYY", "%x")
				.replace("YY", "%x")
				.replace("Y", "%x")

				//week of month
				//????

				//day of week
				.replace("EEEE", "%W")
				.replace("EEE", "%a")
				.replace("ee", "%w")
				.replace("e", "%w")

				//day of month
				.replace("dd", "%d")
				.replace("d", "%e")

				//day of year
				.replace("DDD", "%j")
				.replace("DD", "%j")
				.replace("D", "%j")

				//am pm
				.replace("a", "%p")

				//hour
				.replace("hh", "%I")
				.replace("HH", "%H")
				.replace("h", "%l")
				.replace("H", "%k")

				//minute
				.replace("mm", "%i")
				.replace("m", "%i")

				//second
				.replace("ss", "%S")
				.replace("s", "%S")

				//fractional seconds
				.replace("SSSSSS", "%f")
				.replace("SSSSS", "%f")
				.replace("SSSS", "%f")
				.replace("SSS", "%f")
				.replace("SS", "%f")
				.replace("S", "%f");
	}













	@Override
	public LockingSupport getLockingSupport() {
		return MYSQL_LOCKING_SUPPORT;
	}




	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SubquerySupport getSubquerySupport() {
		return SubquerySupport.builder()
				.feature( SubquerySupport.Feature.OFFSET, true )
				.feature( SubquerySupport.Feature.LATERAL, getMySQLVersion().isSameOrAfter( 8, 0, 14 ) )
				.feature( SubquerySupport.Feature.NESTED_CORRELATION, false )
				.feature( SubquerySupport.Feature.MUTATION_TARGET_REFERENCE, false )
				.build();
	}

	@Override
	public WindowFunctionSupport getWindowFunctionSupport() {
		return getMySQLVersion().isBefore( 8, 0, 2 )
				? WindowFunctionSupport.NONE
				: WindowFunctionSupport.builder()
						.features(
								WindowFunctionSupport.Feature.WINDOW_FUNCTIONS,
								WindowFunctionSupport.Feature.PARTITION_BY,
								WindowFunctionSupport.Feature.ROWS_FRAME,
								WindowFunctionSupport.Feature.RANGE_FRAME
						)
						.build();
	}

	@Override
	public CteSupport getCteSupport() {
		return CteSupport.builder()
				.recursiveFeature(
						CteSupport.RecursiveFeature.RECURSIVE,
						getMySQLVersion().isSameOrAfter( 8, 0, 14 )
				)
				.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SetOperationSupport getSetOperationSupport() {
		if ( getMySQLVersion().isSameOrAfter( 8, 0, 31 ) ) {
			return SetOperationSupport.STANDARD;
		}
		return SetOperationSupport.builder()
				.operator( SetOperator.INTERSECT, false )
				.operator( SetOperator.INTERSECT_ALL, false )
				.operator( SetOperator.EXCEPT, false )
				.operator( SetOperator.EXCEPT_ALL, false )
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	protected void contributeKeywords(KeywordRegistration registration) {
		super.contributeKeywords( registration );
		registration.registerKeyword( "key" );
	}

	protected boolean supportsForShare() {
		return true;
	}

	protected boolean supportsAliasLocks() {
		return true;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public FunctionalDependencyAnalysisSupport getFunctionalDependencyAnalysisSupport() {
		return FunctionalDependencyAnalysisSupport.TABLE_GROUP;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public ConstraintControlMode constraintControlMode() {
		return ConstraintControlMode.GLOBAL;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public List<String> disableCommands() {
		return List.of( "set foreign_key_checks = 0" );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public List<String> enableCommands() {
		return List.of( "set foreign_key_checks = 1" );
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}

	@Override
	public MutationSyntaxSupport getMutationSyntaxSupport() {
		return MutationSyntaxSupport.builder()
				.capability( MutationKind.UPDATE, MutationSyntaxCapability.FROM_CLAUSE )
				.capability( MutationKind.DELETE, MutationSyntaxCapability.JOIN )
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String render(org.hibernate.dialect.constraint.spi.CheckConstraintRenderRequest request) {
		final String rendered = super.render( request );
		return isNotEmpty( request.options() ) ? rendered + " " + request.options() : rendered;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public ObjectNullBindingStrategy getObjectNullBindingStrategy() {
		return ObjectNullBindingStrategy.SET_NULL_WITH_NULL_TYPE;
	}

	@Override
	public SingleRowTableSupport getSingleRowTableSupport() {
		return SingleRowTableSupport.builder( super.getSingleRowTableSupport() )
				.tableExpression( "dual" )
				.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public RowValueSupport getRowValueSupport() {
		return RowValueSupport.builder( super.getRowValueSupport() )
				.feature( RowValueSupport.Feature.QUANTIFIED_COMPARISON, false )
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public MutationOperation createOptionalTableUpdateOperation(OptionalTableUpdateOperationRequest request) {
		final var optionalTableUpdate = request.update();
		if ( optionalTableUpdate.getNumberOfOptimisticLockBindings() == 0 ) {
			final MySQLSqlAstTranslator<?> translator = new MySQLSqlAstTranslator<>( new SqlAstTranslationRequest.ModelMutation<>( request.sessionFactory(), optionalTableUpdate ), MySQLDialect.this );
			return translator.createMergeOperation( optionalTableUpdate );
		}
		return super.createOptionalTableUpdateOperation( request );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public InformationExtractor getInformationExtractor(ExtractionContext extractionContext) {
		return InformationExtractors.mysql( extractionContext );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalTableSupport getTemporalTableSupport() {
		return TemporalTableSupports.mysql(
				getTypeSizingProfile().defaultTimestampPrecision(),
				getCheckConstraintSupport().supports(
						org.hibernate.dialect.constraint.spi.CheckConstraintPlacement.TABLE
				)
		);
	}
}
