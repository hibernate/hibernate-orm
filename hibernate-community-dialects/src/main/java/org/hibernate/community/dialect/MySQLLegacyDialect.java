/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.identifier.spi.KeywordRegistration;

import org.hibernate.dialect.temporaltype.spi.CurrentTimestampSelection;

import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupport;

import org.hibernate.dialect.temporaltype.spi.TemporalFormatSupport;

import org.hibernate.dialect.temporaltype.spi.CurrentTemporalSupport;

import org.hibernate.dialect.type.spi.EnumSupport;
import org.hibernate.dialect.type.spi.EnumSupports;
import org.hibernate.dialect.type.spi.ObjectNullBindingStrategy;
import org.hibernate.dialect.type.spi.StringValueSemantics;
import org.hibernate.dialect.lob.spi.LobSupport;
import org.hibernate.dialect.lob.spi.LobSupports;

import org.hibernate.dialect.type.spi.DdlTypeBuilder;

import org.hibernate.dialect.type.spi.StandardDdlTypes;

import org.hibernate.dialect.type.spi.TypeSizingProfile;

import org.hibernate.dialect.mutation.spi.MultiTableMutationSupport;
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
import org.hibernate.PessimisticLockException;
import org.hibernate.SPI;
import static org.hibernate.SPI.Role.USE;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.sql.ast.spi.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.aggregate.spi.FunctionalDependencyAnalysisSupport;
import org.hibernate.dialect.schema.spi.InnoDBStorageEngine;
import org.hibernate.dialect.schema.spi.MyISAMStorageEngine;
import org.hibernate.dialect.jdbc.spi.MySQLServerConfiguration;
import org.hibernate.dialect.schema.spi.MySQLStorageEngine;
import org.hibernate.dialect.sql.ast.spi.NullOrdering;
import org.hibernate.dialect.function.spi.Replacer;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;
import org.hibernate.dialect.aggregate.spi.AggregateSupport;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.community.dialect.identity.internal.MySQLIdentityColumnSupport;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.StandardLockingSupports;
import org.hibernate.dialect.namespace.spi.NamespaceSupport;
import org.hibernate.dialect.namespace.spi.NamespaceSupports;
import org.hibernate.dialect.schema.spi.ColumnDefinitionRequest;
import org.hibernate.dialect.schema.spi.ConstraintControlMode;
import org.hibernate.dialect.schema.spi.ConstraintDropMode;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.schema.spi.IfExistsSupport;
import org.hibernate.dialect.schema.spi.IndexNameQualification;
import org.hibernate.dialect.schema.spi.SchemaDropSupport;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.LimitLimitHandler;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
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
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.jdbc.spi.JdbcExceptionHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.dialect.unique.spi.UniqueDelegate;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.dialect.temporaltype.spi.IntervalType;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.NullType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.NullJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
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

/**
 * A {@linkplain Dialect SQL dialect} for MySQL 5 and above.
 *
 * @author Gavin King
 */
public class MySQLLegacyDialect extends Dialect implements CurrentTemporalSupport, TemporalFormatSupport, TemporalOperationSupport {
	private final UniqueDelegate uniqueDelegate = new org.hibernate.dialect.unique.spi.DelegatingUniqueDelegate(
			org.hibernate.dialect.unique.spi.UniqueDelegates.alterTable( this ) ) {
		@Override
		public String getAlterTableToDropUniqueKeyCommand(
				UniqueKey uniqueKey,
				Metadata metadata,
				SqlStringGenerationContext context) {
			return delegate().getAlterTableToDropUniqueKeyCommand( uniqueKey, metadata, context )
					.replace( "drop constraint", "drop index" );
		}
	};
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

	private static final DatabaseVersion DEFAULT_VERSION = DatabaseVersion.make( 5, 0 );

	private final MySQLStorageEngine storageEngine = createStorageEngine();
	private final SizeStrategy sizeStrategy = new StandardSizeStrategy( this ) {
		@Override
		public Size resolveSize(
				JdbcType jdbcType,
				JavaType<?> javaType,
				Integer precision,
				Integer scale,
				Long length) {
			switch ( jdbcType.getDefaultSqlTypeCode() ) {
				case Types.BIT:
					// MySQL allows BIT with a length up to 64 (less the default length 255)
					if ( length != null ) {
						return Size.length( Math.min( Math.max( length, 1 ), 64 ) );
					}
			}
			return super.resolveSize( jdbcType, javaType, precision, scale, length );
		}
	};

	private final int maxVarcharLength;
	private final int maxVarbinaryLength;
	private final TypeSizingProfile typeSizingProfile;

	private final boolean noBackslashEscapesEnabled;

	private final LockingSupport lockingSupport;

	public MySQLLegacyDialect() {
		this( DEFAULT_VERSION );
	}

	public MySQLLegacyDialect(DatabaseVersion version) {
		this( version, 4 );
	}

	public MySQLLegacyDialect(DatabaseVersion version, int bytesPerCharacter) {
		this( version, bytesPerCharacter, false );
	}

	public MySQLLegacyDialect(DatabaseVersion version, MySQLServerConfiguration serverConfiguration) {
		this( version, serverConfiguration.getBytesPerCharacter(), serverConfiguration.isNoBackslashEscapesEnabled() );
	}

	public MySQLLegacyDialect(DatabaseVersion version, int bytesPerCharacter, boolean noBackslashEscapes) {
		super( version );
		maxVarcharLength = maxVarcharLength( getMySQLVersion(), bytesPerCharacter ); //conservative assumption
		maxVarbinaryLength = maxVarbinaryLength( getMySQLVersion() );
		typeSizingProfile = TypeSizingProfile.builder( super.getTypeSizingProfile() )
				.defaultLobLength( 16_777_215 )
				.floatPrecision( 23 )
				.maxVarcharLength( maxVarcharLength ).maxVarcharCapacity( maxVarcharLength )
				.maxNVarcharLength( maxVarcharLength ).maxNVarcharCapacity( maxVarcharLength )
				.maxVarbinaryLength( maxVarbinaryLength ).maxVarbinaryCapacity( maxVarbinaryLength )
				.build();
		noBackslashEscapesEnabled = noBackslashEscapes;

		lockingSupport = buildLockingSupport();
	}

	public MySQLLegacyDialect(DialectResolutionInfo info) {
		this( createVersion( info ), MySQLServerConfiguration.fromDialectResolutionInfo( info ) );
	}

	@Override
	public TypeSizingProfile getTypeSizingProfile() {
		return typeSizingProfile;
	}

	protected static DatabaseVersion createVersion(DialectResolutionInfo info) {
		final String versionString = info.getDatabaseVersion();
		if ( versionString != null ) {
			final String[] components = StringHelper.split( ".", versionString );
			if ( components.length >= 3 ) {
				try {
					final int majorVersion = Integer.parseInt( components[0] );
					final int minorVersion = Integer.parseInt( components[1] );
					final int patchLevel = Integer.parseInt( components[2] );
					return DatabaseVersion.make( majorVersion, minorVersion, patchLevel );
				}
				catch (NumberFormatException ex) {
					// Ignore
				}
			}
		}
		return info.makeCopyOrDefault( DEFAULT_VERSION );
	}

	protected LockingSupport buildLockingSupport() {
		return StandardLockingSupports.mysql( getMySQLVersion() );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void contributeDefaultProperties(java.util.Properties properties) {
		super.contributeDefaultProperties( properties );
		properties.setProperty( Environment.MAX_FETCH_DEPTH, "2" );
	}

	private MySQLStorageEngine createStorageEngine() {
		String storageEngine = Environment.getProperties().getProperty( Environment.STORAGE_ENGINE );
		if (storageEngine == null) {
			storageEngine = System.getProperty( Environment.STORAGE_ENGINE );
		}
		if (storageEngine == null) {
			return getDefaultMySQLStorageEngine();
		}
		else if( "innodb".equalsIgnoreCase( storageEngine ) ) {
			return InnoDBStorageEngine.INSTANCE;
		}
		else if( "myisam".equalsIgnoreCase( storageEngine ) ) {
			return MyISAMStorageEngine.INSTANCE;
		}
		else {
			throw new UnsupportedOperationException( "The " + storageEngine + " storage engine is not supported" );
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		switch ( sqlTypeCode ) {
			case BOOLEAN:
				// HHH-6935: Don't use "boolean" i.e. tinyint(1) due to JDBC ResultSetMetaData
				return "bit";

			case TIMESTAMP:
				return getMySQLVersion().isBefore( 5, 7 )
						? "datetime" : "datetime($p)";
			case TIMESTAMP_WITH_TIMEZONE:
				return getMySQLVersion().isBefore( 5, 7 )
						? "timestamp" : "timestamp($p)";
			case NUMERIC:
				// it's just a synonym
				return columnType( DECIMAL );
			// the maximum long LOB length is 4_294_967_295, bigger than any Java string
			case BLOB:
				return "longblob";
			case NCLOB:
			case CLOB:
				return "longtext";

			default:
				return super.columnType( sqlTypeCode );
		}
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
	protected String castType(int sqlTypeCode) {
		switch ( sqlTypeCode ) {
			case BOOLEAN:
			case BIT:
				//special case for casting to Boolean
				return "unsigned";
			case TINYINT:
			case SMALLINT:
			case INTEGER:
			case BIGINT:
				//MySQL doesn't let you cast to INTEGER/BIGINT/TINYINT
				return "signed";
			case FLOAT:
			case REAL:
			case DOUBLE:
				//MySQL doesn't let you cast to DOUBLE/FLOAT
				//but don't just return 'decimal' because
				//the default scale is 0 (no decimal places)
				return getMySQLVersion().isSameOrAfter( 8, 0, 17 )
					// In newer versions of MySQL, casting to float/double is supported
					? super.castType( sqlTypeCode )
					: "decimal($p,$s)";
			case CHAR:
			case NCHAR:
			case VARCHAR:
			case NVARCHAR:
			case LONG32VARCHAR:
			case LONG32NVARCHAR:
				//MySQL doesn't let you cast to TEXT/LONGTEXT
				return "char";
			case BINARY:
			case VARBINARY:
			case LONG32VARBINARY:
				//MySQL doesn't let you cast to BLOB/TINYBLOB/LONGBLOB
				return "binary";
		}
		return super.castType( sqlTypeCode );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		if ( getMySQLVersion().isSameOrAfter( 5, 7 ) ) {
			// MySQL 5.7 brings JSON native support with a dedicated datatype
			// https://dev.mysql.com/doc/refman/5.7/en/json.html
			ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( JSON, "json", this ) );
		}

		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( GEOMETRY, "geometry", this ) );

		// MySQL has approximately one million text and blob types. We have
		// already registered longtext + longblob via the regular method,
		// but we still need to do the rest of them here.

		final int maxTinyLobLen = 255;
		final int maxLobLen = 65_535;
		final int maxMediumLobLen = 16_777_215;

		final DdlTypeBuilder varcharBuilder =
				StandardDdlTypes.builder( VARCHAR, columnType( CLOB ), this )
						.lobKind( DdlTypeBuilder.LobKind.BIGGEST )
						.castTypeNamePattern( columnType( CHAR ) )
						.castTypeName( castType( CHAR ) )
						.withTypeCapacity( getTypeSizingProfile().maxVarcharLength(), "varchar($l)" )
						.withTypeCapacity( maxMediumLobLen, "mediumtext" );
		if ( getTypeSizingProfile().maxVarcharLength() < maxLobLen ) {
			varcharBuilder.withTypeCapacity( maxLobLen, "text" );
		}
		ddlTypeRegistry.addDescriptor( varcharBuilder.build() );

		final DdlTypeBuilder nvarcharBuilder =
				StandardDdlTypes.builder( NVARCHAR, columnType( NCLOB ), this )
						.lobKind( DdlTypeBuilder.LobKind.BIGGEST )
						.castTypeNamePattern( columnType( NCHAR ) )
						.castTypeName( castType( NCHAR ) )
						.withTypeCapacity( getTypeSizingProfile().maxVarcharLength(), "varchar($l)" )
						.withTypeCapacity( maxMediumLobLen, "mediumtext" );
		if ( getTypeSizingProfile().maxVarcharLength() < maxLobLen ) {
			nvarcharBuilder.withTypeCapacity( maxLobLen, "text" );
		}
		ddlTypeRegistry.addDescriptor( nvarcharBuilder.build() );

		final DdlTypeBuilder varbinaryBuilder =
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
				columnType( CLOB ), castType( NCHAR ), this ) );

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
						.withTypeCapacity( maxTinyLobLen, "tinytext" )
						.withTypeCapacity( maxMediumLobLen, "mediumtext" )
						.withTypeCapacity( maxLobLen, "text" )
						.build()
		);

		ddlTypeRegistry.addDescriptor( StandardDdlTypes.nativeEnum( this ) );
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.nativeOrdinalEnum( this ) );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public AggregateSupport getAggregateSupport() {
		return new MySQLDialect( getVersion() ).getAggregateSupport();
	}

	@Deprecated
	protected static int getCharacterSetBytesPerCharacter(DatabaseMetaData databaseMetaData) {
		if ( databaseMetaData != null ) {
			try (java.sql.Statement s = databaseMetaData.getConnection().createStatement() ) {
				final ResultSet rs = s.executeQuery( "SELECT @@character_set_database" );
				if ( rs.next() ) {
					final String characterSet = rs.getString( 1 );
					final int collationIndex = characterSet.indexOf( '_' );
					// According to https://dev.mysql.com/doc/refman/8.0/en/charset-charsets.html
					switch ( collationIndex == -1 ? characterSet : characterSet.substring( 0, collationIndex ) ) {
						case "utf16":
						case "utf16le":
						case "utf32":
						case "utf8mb4":
						case "gb18030":
							return 4;
						case "utf8":
						case "utf8mb3":
						case "eucjpms":
						case "ujis":
							return 3;
						case "ucs2":
						case "cp932":
						case "big5":
						case "euckr":
						case "gb2312":
						case "gbk":
						case "sjis":
							return 2;
						default:
							return 1;
					}
				}
			}
			catch (SQLException ex) {
				// Ignore
			}
		}
		return 4;
	}

	private static int maxVarbinaryLength(DatabaseVersion version) {
		return version.isBefore( 5 ) ? 255 : 65_535;
	}

	private static int maxVarcharLength(DatabaseVersion version, int bytesPerCharacter) {
		// max length for VARCHAR changed in 5.0.3
		if ( version.isBefore( 5 ) ) {
			return 255;
		}
		else {
			switch ( bytesPerCharacter ) {
				case 1:
					return 65_535;
				case 2:
					return 32_767;
				case 3:
					return 21_844;
				case 4:
				default:
					return 16_383;
			}
		}
	}

	public boolean isNoBackslashEscapesEnabled() {
		return noBackslashEscapesEnabled;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendDefinition(SqlAppender appender, ColumnDefinitionRequest request) {
		super.appendDefinition( appender, request );
		// Good job MySQL https://dev.mysql.com/doc/refman/8.0/en/timestamp-initialization.html
		// If the explicit_defaults_for_timestamp system variable is enabled, TIMESTAMP columns permit NULL values only if declared with the NULL attribute.
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
		if ( jdbcTypeCode == Types.CHAR && precision <= 4 ) {
			return displaySize;
		}
		else {
			return precision;
		}
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public int getPreferredSqlTypeCodeForBoolean() {
		return Types.BIT;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public int getPreferredSqlTypeCodeForArray() {
		return getMySQLVersion().isSameOrAfter( 5, 7 ) ? SqlTypes.JSON_ARRAY : super.getPreferredSqlTypeCodeForArray();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);

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
		functionFactory.md5();
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
		functionFactory.sha();
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

		BasicTypeRegistry basicTypeRegistry = functionContributions.getTypeConfiguration().getBasicTypeRegistry();

		SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();

		// pi() produces a value with 7 digits unless we're explicit
		if ( getMySQLVersion().isSameOrAfter( 8 ) ) {
			functionRegistry.patternDescriptorBuilder( "pi", "cast(pi() as double)" )
					.setInvariantType( basicTypeRegistry.resolve( StandardBasicTypes.DOUBLE ) )
					.setExactArgumentCount( 0 )
					.setArgumentListSignature( "" )
					.register();
		}
		else {
			// But before MySQL 8, it's not possible to cast to double. Double has a default precision of 53
			// and since the internal representation of pi has only 15 decimal places, we cast to decimal(53,15)
			functionRegistry.patternDescriptorBuilder( "pi", "cast(pi() as decimal(53,15))" )
					.setInvariantType( basicTypeRegistry.resolve( StandardBasicTypes.DOUBLE ) )
					.setExactArgumentCount( 0 )
					.setArgumentListSignature( "" )
					.register();

			if ( getMySQLVersion().isBefore( 5, 7 ) ) {
				functionFactory.sysdateParens();
			}
			else {
				// MySQL timestamp type defaults to precision 0 (seconds) but
				// we want the standard default precision of 6 (microseconds)
				functionFactory.sysdateExplicitMicros();
				if ( getMySQLVersion().isSameOrAfter( 8, 0, 2 ) ) {
					functionFactory.windowFunctions();
					if ( getMySQLVersion().isSameOrAfter( 8, 0, 11 ) ) {
						functionFactory.hypotheticalOrderedSetAggregates_windowEmulation();
					}
				}
			}
		}

		// By default char() produces a binary string, not a character string.
		// (Note also that char() is actually a variadic function in MySQL.)
		functionRegistry.patternDescriptorBuilder( "chr", "char(?1 using ascii)" )
				.setInvariantType(basicTypeRegistry.resolve( StandardBasicTypes.CHARACTER ))
				.setExactArgumentCount(1)
				.setParameterTypes(FunctionParameterType.INTEGER)
				.register();
		functionRegistry.registerAlternateKey( "char", "chr" );

		functionFactory.listagg_groupConcat();
		functionFactory.regexpLike_regexp();

		if ( getMySQLVersion().isSameOrAfter( 5, 7 ) ) {
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

			if ( getMySQLVersion().isSameOrAfter( 8 ) ) {
				functionFactory.unnest_emulated();
				functionFactory.jsonTable_mysql();
				functionFactory.regexpLike();
			}
			if ( getCteSupport().supports( CteSupport.RecursiveFeature.RECURSIVE ) ) {
				functionFactory.generateSeries_recursive( getMaximumSeriesSize(), false, false );
			}
		}
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

		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();

		if ( getMySQLVersion().isSameOrAfter( 5, 7 ) ) {
			jdbcTypeRegistry.addDescriptorIfAbsent( SqlTypes.JSON, MySQLJdbcTypes.castingJson() );
			jdbcTypeRegistry.addTypeConstructorIfAbsent( MySQLJdbcTypes.castingJsonArrayConstructor() );
		}

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
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new MySQLLegacySqlAstTranslator<>( request, MySQLLegacyDialect.this );
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
		queryEngine.getFunctionRegistry().namedDescriptorBuilder( "time" )
				.setExactArgumentCount( 1 )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
				.register();
	}

	/**
	 * MySQL 5.7 precision defaults to seconds, but microseconds is better
	 */
	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTimestamp() {
		return getMySQLVersion().isBefore( 5, 7 ) ? CurrentTemporalSupport.super.currentTimestamp() : "current_timestamp(6)";
	}

	// for consistency, we could do this: but I decided not to
	// because it seems to me that fractional seconds can't possibly
	// be meaningful in a time, as opposed to a timestamp
//	@Override
//	public String currentTime() {
//		return getMySQLVersion().isBefore( 5, 7 ) ? CurrentTemporalSupport.super.currentTimestamp() : "current_time(6)";
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
	 *
	 * Thus, the additional supported fields are
	 * {@link TemporalUnit#DAY_OF_YEAR},
	 * {@link TemporalUnit#DAY_OF_MONTH},
	 * {@link TemporalUnit#DAY_OF_YEAR}.
	 *
	 * In addition, the field {@link TemporalUnit#SECOND} is
	 * redefined to include microseconds.
	 */
	@Override
	@SPI({ USE, IMPLEMENT })
	public String extractPattern(TemporalUnit unit) {
		switch (unit) {
			case SECOND:
				return "(second(?2)+microsecond(?2)/1e6)";
			case WEEK:
				return "weekofyear(?2)"; //same as week(?2,3), the ISO week
			case DAY_OF_WEEK:
				return "dayofweek(?2)";
			case DAY_OF_MONTH:
				return "dayofmonth(?2)";
			case DAY_OF_YEAR:
				return "dayofyear(?2)";
			//TODO: case WEEK_YEAR: yearweek(?2, 3)/100
			default:
				return "?1(?2)";
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		switch (unit) {
			case NANOSECOND:
				return "timestampadd(microsecond,(?2)/1e3,?3)";
			case NATIVE:
				return "timestampadd(microsecond,?2,?3)";
			default:
				return "timestampadd(?1,?2,?3)";
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		switch (unit) {
			case NANOSECOND:
				return "timestampdiff(microsecond,?2,?3)*1e3";
			case NATIVE:
				return "timestampdiff(microsecond,?2,?3)";
			default:
				return "timestampdiff(?1,?2,?3)";
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
		return getMySQLVersion().isBefore( 5 )
				? super.getQueryHintString( query, hints )
				: org.hibernate.dialect.queryhint.spi.QueryHints.addUseIndexHint( query, hints );
	}

	/**
	 * No support for sequences.
	 */
	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		return org.hibernate.dialect.sequence.spi.SequenceSupports.none();
	}

	@SPI({ IMPLEMENT, SUPPLY })
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return getMySQLVersion().isBefore( 5 ) ? super.getViolatedConstraintNameExtractor() : EXTRACTOR;
	}

	private static final ViolatedConstraintNameExtractor EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor( sqle -> {
				final String sqlState = JdbcExceptionHelper.extractSqlState( sqle );
				if ( sqlState != null ) {
					switch ( Integer.parseInt( sqlState ) ) {
						case 23000:
							return extractUsingTemplate( " for key '", "'", sqle.getMessage() );
					}
				}
				return null;
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
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
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
	@SPI({ USE, IMPLEMENT })
	public CurrentTimestampSelection getCurrentTimestampSelection() {
		return CurrentTimestampSelection.prepared( "select now()" );
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public ObjectNullBindingStrategy getObjectNullBindingStrategy() {
		return ObjectNullBindingStrategy.SET_NULL_WITH_NULL_TYPE;
	}

	@Override
	public LockingSupport getLockingSupport() {
		return lockingSupport;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			switch ( sqlException.getErrorCode() ) {
				case 1205:
				case 3572:
					return new PessimisticLockException( message, sqlException, sql );
				case 1207:
				case 1206:
					return new LockAcquisitionException( message, sqlException, sql );
			}

			final String sqlState = JdbcExceptionHelper.extractSqlState( sqlException );
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
		String engineKeyword = getMySQLVersion().isBefore( 5 ) ? "type" : "engine";
		return storageEngine.getTableTypeString( engineKeyword );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean requiresSelfReferentialForeignKeyNullification() {
		return storageEngine.requiresSelfReferentialForeignKeyNullification();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized SchemaDropSupport getSchemaDropSupport() {
		if ( schemaDropSupport == null ) {
			schemaDropSupport = new SchemaDropSupport(
				List.of(),
				storageEngine.dropConstraints() ? ConstraintDropMode.EXPLICIT : ConstraintDropMode.IMPLICIT,
				""
		);
		}
		return schemaDropSupport;
	}

	@SPI(IMPLEMENT)
	protected MySQLStorageEngine getDefaultMySQLStorageEngine() {
		return getMySQLVersion().isBefore( 5, 5 ) ? MyISAMStorageEngine.INSTANCE : InnoDBStorageEngine.INSTANCE;
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
					appender.appendSql( '\\' );
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
				.placement(
						getVersion().isSameOrAfter( 8 )
								? CteSupport.Placement.NESTED
								: CteSupport.Placement.NONE
				)
				.recursiveFeature(
						CteSupport.RecursiveFeature.RECURSIVE,
						getMySQLVersion().isSameOrAfter( 8, 0, 14 )
				)
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	protected void contributeKeywords(KeywordRegistration registration) {
		super.contributeKeywords( registration );
		registration.registerKeyword( "key" );
	}

	boolean supportsForShare() {
		return getMySQLVersion().isSameOrAfter( 8 );
	}

	boolean supportsAliasLocks() {
		return getMySQLVersion().isSameOrAfter( 8 );
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
		return StringHelper.isNotEmpty( request.options() ) ? rendered + " " + request.options() : rendered;
	}
	@Override
	public SingleRowTableSupport getSingleRowTableSupport() {
		final String tableExpression = "dual";
		return SingleRowTableSupport.builder( super.getSingleRowTableSupport() )
				.tableExpression( tableExpression )
				.selectOnlyFromClause( getVersion().isSameOrAfter( 8 ) ? "" : " from " + tableExpression )
				.build();
	}

	@Override
	public PredicateSupport getPredicateSupport() {
		// It supports a proprietary DISTINCT FROM operator
		return PredicateSupport.builder( super.getPredicateSupport() )
				.capability( PredicateSupport.Capability.DISTINCT_FROM, true )
				.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SetOperationSupport getSetOperationSupport() {
		return SetOperationSupport.builder()
				.operator( SetOperator.UNION_ALL, getMySQLVersion().isSameOrAfter( 5 ) )
				.operator( SetOperator.INTERSECT, false )
				.operator( SetOperator.INTERSECT_ALL, false )
				.operator( SetOperator.EXCEPT, false )
				.operator( SetOperator.EXCEPT_ALL, false )
				.capability(
						SetOperationSupport.Capability.UNION_IN_SUBQUERY,
						getMySQLVersion().isSameOrAfter( 5 )
				)
				.capability(
						SetOperationSupport.Capability.SIMPLE_QUERY_GROUPING,
						getVersion().isSameOrAfter( 8 )
				)
				.build();
	}

	@Override
	public RowValueSupport getRowValueSupport() {
		final boolean supportsIn = getVersion().isSameOrAfter( 5, 7 );
		return RowValueSupport.builder( super.getRowValueSupport() )
				.feature( RowValueSupport.Feature.IN_LIST, supportsIn )
				.feature( RowValueSupport.Feature.IN_SUBQUERY, supportsIn )
				.feature( RowValueSupport.Feature.QUANTIFIED_COMPARISON, false )
				.build();
	}

}
