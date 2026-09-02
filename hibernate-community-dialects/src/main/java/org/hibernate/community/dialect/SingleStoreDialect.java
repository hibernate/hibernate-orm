/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.identifier.spi.KeywordRegistration;

import org.hibernate.dialect.temporaltype.spi.TemporalValueSemantics;

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
import org.hibernate.Length;
import org.hibernate.PessimisticLockException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.community.dialect.function.json.SingleStoreJsonArrayAggFunction;
import org.hibernate.community.dialect.function.json.SingleStoreJsonArrayAppendFunction;
import org.hibernate.community.dialect.function.json.SingleStoreJsonArrayFunction;
import org.hibernate.community.dialect.function.json.SingleStoreJsonArrayInsertFunction;
import org.hibernate.community.dialect.function.json.SingleStoreJsonExistsFunction;
import org.hibernate.community.dialect.function.json.SingleStoreJsonMergepatchFunction;
import org.hibernate.community.dialect.function.json.SingleStoreJsonObjectAggFunction;
import org.hibernate.community.dialect.function.json.SingleStoreJsonObjectFunction;
import org.hibernate.community.dialect.function.json.SingleStoreJsonQueryFunction;
import org.hibernate.community.dialect.function.json.SingleStoreJsonRemoveFunction;
import org.hibernate.community.dialect.function.json.SingleStoreJsonSetFunction;
import org.hibernate.community.dialect.function.json.SingleStoreJsonValueFunction;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.jdbc.spi.ParameterLimits;
import org.hibernate.dialect.sql.ast.spi.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.aggregate.spi.FunctionalDependencyAnalysisSupport;
import org.hibernate.dialect.sql.ast.spi.NullOrdering;
import org.hibernate.dialect.function.spi.Replacer;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;
import org.hibernate.dialect.lock.spi.RowLockStrategy;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.community.dialect.identity.internal.MySQLIdentityColumnSupport;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.StandardLockingSupports;
import org.hibernate.dialect.namespace.spi.NamespaceSupport;
import org.hibernate.dialect.namespace.spi.NamespaceSupports;
import org.hibernate.dialect.schema.spi.ColumnDefinitionRequest;
import org.hibernate.dialect.schema.spi.ConstraintDropMode;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.schema.spi.IfExistsSupport;
import org.hibernate.dialect.schema.spi.IndexNameQualification;
import org.hibernate.dialect.schema.spi.SchemaDropSupport;
import org.hibernate.dialect.schema.spi.TableCreationKind;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.dialect.lob.spi.LobSupport;
import org.hibernate.dialect.lob.spi.LobSupports;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.LimitLimitHandler;
import org.hibernate.community.dialect.temptable.internal.SingleStoreLocalTemporaryTableStrategy;
import org.hibernate.dialect.temptable.spi.TemporaryTableStrategy;
import org.hibernate.dialect.type.spi.SizeStrategy;
import org.hibernate.dialect.type.spi.StandardSizeStrategy;
import org.hibernate.dialect.type.spi.EnumSupport;
import org.hibernate.dialect.type.spi.EnumSupports;
import org.hibernate.dialect.unique.spi.UniqueDelegate;
import org.hibernate.dialect.unique.spi.UniqueDelegates;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.dialect.identifier.spi.IdentifierHelperBuildRequest;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.jdbc.env.spi.SchemaNameResolver;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.jdbc.spi.JdbcExceptionHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.mapping.ForeignKey;
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
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.NullType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.EnumJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.NullJdbcType;
import org.hibernate.type.descriptor.jdbc.OrdinalEnumJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.Types;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.NUMERIC;
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
import static org.hibernate.type.SqlTypes.INTEGER;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.LONG32NVARCHAR;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.LONG32VARCHAR;
import static org.hibernate.type.SqlTypes.NCHAR;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.NVARCHAR;
import static org.hibernate.type.SqlTypes.SMALLINT;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.SqlTypes.VARCHAR;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsDate;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsLocalTime;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTimestampWithMicros;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTimestampWithMillis;
import static org.hibernate.dialect.literal.spi.ZeroOffsetLiteralStyle.NUMERIC_OFFSET;

/**
 * An SQL dialect for SingleStore.
 * <p>
 * The following are some of the key aspects and limitations of SingleStore that may affect Hibernate functionality:
 * </p>
 * <ul>
 *   <li>SingleStore supports two table types: COLUMNSTORE and ROWSTORE. Explicit table type can be configured by setting 'hibernate.dialect.singlestore.table_type' property. Refer to {@link SingleStoreTableType} for details.</li>
 *   <li>SingleStore has a random order for SELECT queries, which may impact the predictability of query results.</li>
 *   <li>SingleStore does not support foreign keys and referential integrity, which could affect the design of your database schema.</li>
 *   <li>The SingleStore dialect ignores unique key constraints. See {@link UniqueDelegates#none()} for more information.</li>
 *   <li>SingleStore does not support zoned timestamps, which might require adjustments to how you handle time-related data.</li>
 *   <li>Updating primary keys in SingleStore is restricted because every primary key is also a unique key and shard key.</li>
 *   <li>SingleStore does not support the ALL/ANY clause in SQL queries.</li>
 *   <li>Sub-selects with references to outer table fields are not supported in SingleStore.</li>
 *   <li>SingleStore does not support the 'FOR UPDATE' clause for table locking with distributed joins. It's disabled by default, can be enabled by setting 'hibernate.dialect.singlestore.for_update_lock_enabled' property {@link SingleStoreDialect#SINGLE_STORE_FOR_UPDATE_LOCK_ENABLED}.</li>
 *   <li>The LIKE clause in SingleStore is case-insensitive, which might differ from other SQL implementations.</li>
 * </ul>
 *
 * @author Oleksandr Yeliseiev
 */
public class SingleStoreDialect extends Dialect implements CurrentTemporalSupport, TemporalFormatSupport, TemporalOperationSupport {
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
	private final TypeSizingProfile typeSizingProfile = TypeSizingProfile.builder( super.getTypeSizingProfile() )
			.defaultDecimalPrecision( 65 ).defaultLobLength( Length.LONG32 ).floatPrecision( 23 )
			.maxVarcharLength( 21_844 ).maxVarcharCapacity( 21_844 )
			.maxNVarcharLength( 21_844 ).maxNVarcharCapacity( 21_844 )
			.maxVarbinaryLength( 65_533 ).maxVarbinaryCapacity( 65_533 )
			.build();

	@Override public TypeSizingProfile getTypeSizingProfile() { return typeSizingProfile; }

	private static final int PARAM_LIST_SIZE_LIMIT = 1_048_576;
	private static final String[] NO_COMMANDS = new String[0];
	private static final EmptyExporter NOOP_EXPORTER = new EmptyExporter();
	private static final UniqueDelegate NOOP_UNIQUE_DELEGATE = UniqueDelegates.none();
	private static final DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 8, 0 );

	private final SingleStoreTableType explicitTableType;
	private final boolean isForUpdateLockingEnabled;

	private final LockingSupport lockingSupport;

	public SingleStoreDialect() {
		this( MINIMUM_VERSION, null, false );
	}

	public SingleStoreDialect(DialectResolutionInfo info) {
		this( createVersion( info ), getTableType( info ), getUpdateForEnabled( info ) );
	}

	public SingleStoreDialect(
			DatabaseVersion version, SingleStoreTableType explicitTableType, boolean isForUpdateLockingEnabled) {
		super( version );
		this.explicitTableType = explicitTableType;
		this.isForUpdateLockingEnabled = isForUpdateLockingEnabled;
		this.lockingSupport = StandardLockingSupports.parameterized(
				isForUpdateLockingEnabled ? PessimisticLockStyle.CLAUSE : PessimisticLockStyle.NONE,
				RowLockStrategy.NONE,
				LockTimeoutType.NONE,
				LockTimeoutType.NONE,
				LockTimeoutType.NONE,
				OuterJoinLockingType.UNSUPPORTED
		);
	}

	private static DatabaseVersion createVersion(DialectResolutionInfo info) {
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
		return info.makeCopyOrDefault( MINIMUM_VERSION );
	}

	private static SingleStoreTableType getTableType(DialectResolutionInfo info) {
		String value = ConfigurationHelper.getString( SINGLE_STORE_TABLE_TYPE, info.getConfigurationValues() );
		return value == null ? null : SingleStoreTableType.fromValue( value );
	}

	private static boolean getUpdateForEnabled(DialectResolutionInfo info) {
		return ConfigurationHelper.getBoolean(
				SINGLE_STORE_FOR_UPDATE_LOCK_ENABLED,
				info.getConfigurationValues()
		);
	}

	private final SizeStrategy sizeStrategy = new StandardSizeStrategy( this ) {
		@Override
		public Size resolveSize(
				JdbcType jdbcType, JavaType<?> javaType, Integer precision, Integer scale, Long length) {
			switch ( jdbcType.getDdlTypeCode() ) {
				case BIT:
					if ( length != null ) {
						return Size.length( Math.min( Math.max( length, 1 ), 64 ) );
					}
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

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_VERSION;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public LobSupport getLobSupport() {
		return LobSupports.noCapacityPromotion();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String extractPattern(TemporalUnit unit) {
		return switch ( unit ) {
			case SECOND -> "(second(?2)+microsecond(?2)/1e6)";
			case WEEK -> "weekofyear(?2)";
			case DAY_OF_WEEK -> "dayofweek(?2)";
			case DAY_OF_MONTH -> "dayofmonth(?2)";
			case DAY_OF_YEAR -> "dayofyear(?2)";
			case EPOCH -> "unix_timestamp(?2)";
			default -> "?1(?2)";
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		if ( temporalType == TemporalType.TIME ) {
			switch ( unit ) {
				case NANOSECOND:
					return "time(timestampadd(microsecond,(?2)/1e3,to_timestamp(?3, 'HH24:MI:SS.FF6')))";
				case NATIVE:
					return "time(timestampadd(microsecond, ?2, to_timestamp(?3, 'HH24:MI:SS.FF6')))";
				case SECOND:
					return "time(timestampadd(microsecond, ?2 * 1000000, to_timestamp(?3, 'HH24:MI:SS.FF6')))"; // to handle seconds fraction part
				default:
					return "time(timestampadd(?1, ?2, to_timestamp(?3, 'HH24:MI:SS.FF6')))";
			}
		}
		switch ( unit ) {
			case NANOSECOND:
				return "timestampadd(microsecond,(?2)/1e3,?3)";
			case NATIVE:
				return "timestampadd(microsecond,?2,?3)";
			case SECOND:
				return "timestampadd(microsecond,?2 * 1000000,?3)"; // to handle seconds fraction part
			default:
				return "timestampadd(?1,?2,?3)";
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		String fromType = fromTemporalType == TemporalType.TIME ? "to_timestamp(?2, 'HH24:MI:SS.FF6')" : "?2";
		String toType = toTemporalType == TemporalType.TIME ? "to_timestamp(?3, 'HH24:MI:SS.FF6')" : "?3";
		return switch ( unit ) {
			case NANOSECOND -> String.format( "timestampdiff(microsecond,%s,%s)*1e3", fromType, toType );
			case NATIVE -> String.format( "timestampdiff(microsecond,%s,%s)", fromType, toType );
			default -> String.format( "timestampdiff(?1,%s,%s)", fromType, toType );
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendDateTimeLiteral(
			SqlAppender appender, TemporalAccessor temporalAccessor, TemporalType precision, TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( "date('" );
				appendAsDate( appender, temporalAccessor );
				appender.appendSql( "')" );
				break;
			case TIME:
				appender.appendSql( "time('" );
				appendAsLocalTime( appender, temporalAccessor );
				appender.appendSql( "')" );
				break;
			case TIMESTAMP:
				if ( temporalAccessor instanceof ZonedDateTime ) {
					temporalAccessor = ((ZonedDateTime) temporalAccessor).toOffsetDateTime();
				}
				appender.appendSql( "timestamp('" );
				appendAsTimestampWithMicros(
						appender,
						temporalAccessor,
						getTemporalValueSemantics().supportsLiteralOffset(),
						jdbcTimeZone,
						NUMERIC_OFFSET
				);
				appender.appendSql( "')" );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendDateTimeLiteral(SqlAppender appender, Date date, TemporalType precision, TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( "date('" );
				appendAsDate( appender, date );
				appender.appendSql( "')" );
				break;
			case TIME:
				appender.appendSql( "time('" );
				appendAsLocalTime( appender, date );
				appender.appendSql( "')" );
				break;
			case TIMESTAMP:
				appender.appendSql( "timestamp('" );
				appendAsTimestampWithMicros( appender, date, jdbcTimeZone );
				appender.appendSql( "')" );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendDateTimeLiteral(
			SqlAppender appender, Calendar calendar, TemporalType precision, TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( "date('" );
				appendAsDate( appender, calendar );
				appender.appendSql( "')" );
				break;
			case TIME:
				appender.appendSql( "time('" );
				appendAsLocalTime( appender, calendar );
				appender.appendSql( "')" );
				break;
			case TIMESTAMP:
				appender.appendSql( "timestamp('" );
				appendAsTimestampWithMillis( appender, calendar, jdbcTimeZone );
				appender.appendSql( "')" );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	//Creating an index on an ENUM column on columnstore tables is not supported.
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

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}

	private static final ViolatedConstraintNameExtractor EXTRACTOR = new TemplatedViolatedConstraintNameExtractor(
			sqle -> {
				final String sqlState = JdbcExceptionHelper.extractSqlState( sqle );
				if ( sqlState != null ) {
					if ( Integer.parseInt( sqlState ) == 23000 ) {
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
	@SPI({ IMPLEMENT, SUPPLY })
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( JSON, "json", this ) );

		final int maxTinyLobLen = 225;
		final int maxLobLen = 65_535;
		final int maxMediumLobLen = 16_777_215;

		final DdlTypeBuilder varcharBuilder = StandardDdlTypes.builder( VARCHAR, columnType( CLOB ), this )
				.lobKind( DdlTypeBuilder.LobKind.BIGGEST )
				.castTypeNamePattern( columnType( CHAR ) )
				.castTypeName( castType( CHAR ) )
				.withTypeCapacity( getTypeSizingProfile().maxVarcharLength(), "varchar($l)" )
				.withTypeCapacity( maxMediumLobLen, "mediumtext" );
		if ( getTypeSizingProfile().maxVarcharLength() < maxLobLen ) {
			varcharBuilder.withTypeCapacity( maxLobLen, "text" );
		}
		ddlTypeRegistry.addDescriptor( varcharBuilder.build() );

		// SingleStore doesn't support nchar/nvarchar/ntext
		final DdlTypeBuilder nvarcharBuilder = StandardDdlTypes.builder( NVARCHAR, columnType( NCLOB ), this )
				.lobKind( DdlTypeBuilder.LobKind.BIGGEST )
				.castTypeNamePattern( columnType( NCHAR ) )
				.castTypeName( castType( NCHAR ) )
				.withTypeCapacity( getTypeSizingProfile().maxVarcharLength(), "varchar($l) character set utf8" ).withTypeCapacity(
				maxMediumLobLen,
				"mediumtext character set utf8"
		);
		if ( getTypeSizingProfile().maxVarcharLength() < maxLobLen ) {
			nvarcharBuilder.withTypeCapacity( maxLobLen, "text character set utf8" );
		}
		ddlTypeRegistry.addDescriptor( nvarcharBuilder.build() );

		final DdlTypeBuilder varbinaryBuilder = StandardDdlTypes.builder( VARBINARY, columnType( BLOB ), this )
				.lobKind( DdlTypeBuilder.LobKind.BIGGEST )
				.castTypeNamePattern( columnType( BINARY ) )
				.castTypeName( castType( BINARY ) )
				.withTypeCapacity( getTypeSizingProfile().maxVarbinaryLength(), "varbinary($l)" ).withTypeCapacity(
				maxMediumLobLen,
				"mediumblob"
		);
		if ( getTypeSizingProfile().maxVarbinaryLength() < maxLobLen ) {
			varbinaryBuilder.withTypeCapacity( maxLobLen, "blob" );
		}
		ddlTypeRegistry.addDescriptor( varbinaryBuilder.build() );

		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple(
				LONG32VARBINARY,
				columnType( BLOB ),
				castType( BINARY ),
				this
		) );
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( LONG32VARCHAR, columnType( CLOB ), castType( CHAR ), this ) );
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( LONG32NVARCHAR, columnType( CLOB ), castType( CHAR ), this ) );

		ddlTypeRegistry.addDescriptor( StandardDdlTypes.builder( BLOB, columnType( BLOB ), this ).castTypeName( castType( BINARY ) )
				.withTypeCapacity( maxTinyLobLen, "tinyblob" )
				.withTypeCapacity( maxMediumLobLen, "mediumblob" )
				.withTypeCapacity( maxLobLen, "blob" )
				.build() );

		ddlTypeRegistry.addDescriptor( StandardDdlTypes.builder( CLOB, columnType( CLOB ), this ).castTypeName( castType( CHAR ) )
				.withTypeCapacity( maxTinyLobLen, "tinytext" )
				.withTypeCapacity( maxMediumLobLen, "mediumtext" )
				.withTypeCapacity( maxLobLen, "text" )
				.build() );

		ddlTypeRegistry.addDescriptor( StandardDdlTypes.builder( NCLOB, columnType( NCLOB ), this ).castTypeName( castType( NCHAR ) )
				.withTypeCapacity(
						maxTinyLobLen,
						"tinytext character set utf8"
				)
				.withTypeCapacity( maxMediumLobLen, "mediumtext character set utf8" )
				.withTypeCapacity( maxLobLen, "text character set utf8" )
				.build() );

		ddlTypeRegistry.addDescriptor( StandardDdlTypes.nativeEnum( this ) );
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.nativeOrdinalEnum( this ) );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry( functionContributions );
		CommonFunctionFactory commonFunctionFactory = new CommonFunctionFactory( functionContributions );
		commonFunctionFactory.windowFunctions();
		commonFunctionFactory.radians();
		commonFunctionFactory.degrees();
		commonFunctionFactory.cot();
		commonFunctionFactory.log();
		commonFunctionFactory.log2();
		commonFunctionFactory.log10();
		commonFunctionFactory.trim2();
		commonFunctionFactory.octetLength();
		commonFunctionFactory.reverse();
		commonFunctionFactory.pad_space();
		commonFunctionFactory.md5();
		commonFunctionFactory.yearMonthDay();
		commonFunctionFactory.hourMinuteSecond();
		commonFunctionFactory.dayofweekmonthyear();
		commonFunctionFactory.weekQuarter();
		commonFunctionFactory.daynameMonthname();
		commonFunctionFactory.lastDay();
		commonFunctionFactory.date();
		commonFunctionFactory.timestamp();
		commonFunctionFactory.utcDateTimeTimestamp();
		commonFunctionFactory.rand();
		commonFunctionFactory.crc32();
		commonFunctionFactory.sha1();
		commonFunctionFactory.sha2();
		commonFunctionFactory.sha();
		commonFunctionFactory.octetLength();
		commonFunctionFactory.ascii();
		commonFunctionFactory.instr();
		commonFunctionFactory.substr();
		commonFunctionFactory.position();
		commonFunctionFactory.nowCurdateCurtime();
		commonFunctionFactory.trunc_truncate();
		commonFunctionFactory.bitandorxornot_operator();
		commonFunctionFactory.bitAndOr();
		commonFunctionFactory.stddev();
		commonFunctionFactory.stddevPopSamp();
		commonFunctionFactory.variance();
		commonFunctionFactory.varPopSamp();
		commonFunctionFactory.datediff();
		commonFunctionFactory.adddateSubdateAddtimeSubtime();
		commonFunctionFactory.format_dateFormat();
		commonFunctionFactory.makedateMaketime();
		commonFunctionFactory.localtimeLocaltimestamp();
		commonFunctionFactory.hypotheticalOrderedSetAggregates_windowEmulation();
		commonFunctionFactory.inverseDistributionOrderedSetAggregates_windowEmulation();
		commonFunctionFactory.listagg_groupConcat();
		SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();
		final TypeConfiguration typeConfiguration = functionContributions.getTypeConfiguration();
		BasicTypeRegistry basicTypeRegistry = functionContributions.getTypeConfiguration().getBasicTypeRegistry();
		functionRegistry.namedDescriptorBuilder( "time" )
				.setExactArgumentCount( 1 )
				.setInvariantType( basicTypeRegistry.resolve( StandardBasicTypes.STRING ) )
				.register();
		functionRegistry.patternDescriptorBuilder( "median", "median(?1) over ()" )
				.setInvariantType( basicTypeRegistry.resolve( StandardBasicTypes.DOUBLE ) )
				.setExactArgumentCount( 1 )
				.setParameterTypes( NUMERIC )
				.register();
		functionRegistry.noArgsBuilder( "localtime" )
				.setInvariantType( basicTypeRegistry.resolve( StandardBasicTypes.TIMESTAMP ) )
				.setUseParenthesesWhenNoArgs( false )
				.register();
		functionRegistry.patternDescriptorBuilder( "pi", "pi() :> double" ).setInvariantType( basicTypeRegistry.resolve(
				StandardBasicTypes.DOUBLE ) ).setExactArgumentCount( 0 ).setArgumentListSignature( "" ).register();
		functionRegistry.patternDescriptorBuilder( "chr", "char(?1 using utf8mb4)" )
				.setInvariantType( basicTypeRegistry.resolve( StandardBasicTypes.CHARACTER ) )
				.setExactArgumentCount( 1 )
				.setParameterTypes( FunctionParameterType.INTEGER )
				.register();
		functionRegistry.registerAlternateKey( "char", "chr" );
		functionRegistry.register( "json_object", new SingleStoreJsonObjectFunction( typeConfiguration ) );
		functionRegistry.register( "json_array", new SingleStoreJsonArrayFunction( typeConfiguration ) );
		functionRegistry.register( "json_value", new SingleStoreJsonValueFunction( typeConfiguration ) );
		functionRegistry.register( "json_exists", new SingleStoreJsonExistsFunction( typeConfiguration ) );
		functionRegistry.register( "json_query", new SingleStoreJsonQueryFunction( typeConfiguration ) );
		functionRegistry.register( "json_arrayagg", new SingleStoreJsonArrayAggFunction( typeConfiguration ) );
		functionRegistry.register( "json_objectagg", new SingleStoreJsonObjectAggFunction( typeConfiguration ) );
		functionRegistry.register( "json_set", new SingleStoreJsonSetFunction( typeConfiguration ) );
		functionRegistry.register( "json_remove", new SingleStoreJsonRemoveFunction( typeConfiguration ) );
		functionRegistry.register( "json_mergepatch", new SingleStoreJsonMergepatchFunction( typeConfiguration ) );
		functionRegistry.register( "json_array_append", new SingleStoreJsonArrayAppendFunction( typeConfiguration ) );
		functionRegistry.register( "json_array_insert", new SingleStoreJsonArrayInsertFunction( typeConfiguration ) );
		commonFunctionFactory.regexpLike_regexp();
	}


	@Override
	@SPI({ USE, IMPLEMENT })
	public String createTableCommand(TableCreationKind kind) {
		return explicitTableType == null ? "create table" : String.format(
				"create %s table",
				explicitTableType.name().toLowerCase()
		);
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );

		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();

		typeContributions.contributeJdbcType( NullJdbcType.INSTANCE );

		// Until we remove StandardBasicTypes, we have to keep this
		typeContributions.contributeType( new NullType(
				NullJdbcType.INSTANCE,
				typeContributions.getTypeConfiguration()
						.getJavaTypeRegistry()
						.resolveDescriptor( Object.class )
		) );

		jdbcTypeRegistry.addDescriptor( EnumJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( OrdinalEnumJdbcType.INSTANCE );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public JdbcType resolveSqlTypeDescriptor(
			String columnTypeName, int jdbcTypeCode, int precision, int scale, JdbcTypeRegistry jdbcTypeRegistry) {
		switch ( jdbcTypeCode ) {
			case Types.BIT:
				return jdbcTypeRegistry.getDescriptor( Types.TINYINT );
			case Types.OTHER:
				if ( "GEOGRAPHY".equals( columnTypeName ) || "GEOGRAPHYPOINT".equals( columnTypeName ) ) {
					jdbcTypeCode = VARCHAR;
				}
				break;
		}
		return super.resolveSqlTypeDescriptor( columnTypeName, jdbcTypeCode, precision, scale, jdbcTypeRegistry );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		return switch ( sqlTypeCode ) {
			case BOOLEAN -> "bit";
			case TIMESTAMP -> "datetime($p)";
			case TIMESTAMP_WITH_TIMEZONE -> "timestamp($p)";
			case TIME_WITH_TIMEZONE -> "time($p)";
			case SqlTypes.NUMERIC -> columnType( DECIMAL );
			// Avoid using float type because
			// SingleStore has potential inaccuracy when using the = or != comparison operators on FLOAT columns in WHERE clause
			case FLOAT -> columnType( DOUBLE );
			case NCHAR -> "char($l) character set utf8";
			case NVARCHAR -> "varchar($l) character set utf8";
			case BLOB -> "longblob";
			case NCLOB -> "longtext character set utf8";
			case CLOB -> "longtext";
			default -> super.columnType( sqlTypeCode );
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String castPattern(CastType from, CastType to) {
		if ( CastType.FLOAT == to || CastType.DOUBLE == to || CastType.OTHER == to ) {
			return "?1 :> ?2";
		}
		return super.castPattern( from, to );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public int getPreferredSqlTypeCodeForBoolean() {
		return Types.BIT;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String castType(int sqlTypeCode) {
		return switch ( sqlTypeCode ) {
			//special case for casting to Boolean
			case BOOLEAN, BIT -> "unsigned";
			case TINYINT, SMALLINT, INTEGER, BIGINT -> "signed";
			case CHAR, VARCHAR, LONG32VARCHAR -> "char";
			case NCHAR, NVARCHAR, LONG32NVARCHAR -> "char character set utf8";
			case BINARY, VARBINARY, LONG32VARBINARY ->  "binary";
			default -> super.castType( sqlTypeCode );
		};
	}

	@Override
	public SizeStrategy getSizeStrategy() {
		return sizeStrategy;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTimestamp() {
		return "current_timestamp(6)";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public long fractionalSecondPrecisionInNanos() {
		return 1_000; //microseconds
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public int resolveSqlTypeLength(
			String columnTypeName, int jdbcTypeCode, int precision, int scale, int displaySize) {
		if ( jdbcTypeCode == Types.CHAR && precision <= 4 ) {
			return displaySize;
		}
		else {
			return precision;
		}
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new SingleStoreSqlAstTranslator<>( request, SingleStoreDialect.this );
			}
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SchemaNameResolver getSchemaNameResolver() {
		return (connection, dialect) -> "";
	}

	@Override
	public ParameterLimits getParameterLimits() {
		return ParameterLimits.of( PARAM_LIST_SIZE_LIMIT );
	}

	/**
	 * The biggest size value that can be supplied as argument
	 */
	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendDefinition(SqlAppender appender, ColumnDefinitionRequest request) {
		super.appendDefinition( appender, request );
		if ( request.nullable()
				&& request.sqlType().regionMatches( true, 0, "timestamp", 0, "timestamp".length() ) ) {
			appender.appendSql( " null" );
		}
	}

	/**
	 * Feature 'Check constraints' is not supported by SingleStore.
	 */
	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean supports(org.hibernate.dialect.constraint.spi.CheckConstraintPlacement placement) {
		return false;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalValueSemantics getTemporalValueSemantics() {
		return TemporalValueSemantics.TRUNCATING;
	}

	@Override
	public WindowFunctionSupport getWindowFunctionSupport() {
		return WindowFunctionSupport.builder()
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
				.placement( CteSupport.Placement.TOP_LEVEL )
				.recursiveFeatures( CteSupport.RecursiveFeature.RECURSIVE )
				.mutationFeatures( CteSupport.MutationFeature.NON_QUERY )
				.build();
	}

	@Override
	public SetOperationSupport getSetOperationSupport() {
		return SetOperationSupport.builder()
				.operator( SetOperator.INTERSECT_ALL, false )
				.operator( SetOperator.EXCEPT_ALL, false )
				.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized SchemaDropSupport getSchemaDropSupport() {
		if ( schemaDropSupport == null ) {
			schemaDropSupport = new SchemaDropSupport( List.of(), ConstraintDropMode.IMPLICIT, "" );
		}
		return schemaDropSupport;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendLiteral(SqlAppender appender, String literal) {
		appender.appendSql( '\'' );
		for ( int i = 0; i < literal.length(); i++ ) {
			final char c = literal.charAt( i );
			if ( c == '\'' ) {
				appender.appendSql( '\'' );
			}
			else if ( c == '\\' ) {
				appender.appendSql( '\\' );
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
		return new Replacer( format, "'", "" ).replace( "%", "%%" )

				//year
				.replace( "yyyy", "%Y" )
				.replace( "yyy", "%Y" )
				.replace( "yy", "%y" )
				.replace( "y", "%Y" )

				//month of year
				.replace( "MMMM", "%M" )
				.replace( "MMM", "%b" )
				.replace( "MM", "%m" )
				.replace( "M", "%c" )

				//week of year
				.replace( "ww", "%v" )
				.replace( "w", "%v" )
				//year for week
				.replace( "YYYY", "%x" )
				.replace( "YYY", "%x" )
				.replace( "YY", "%x" )
				.replace( "Y", "%x" )

				//week of month
				//????

				//day of week
				.replace( "EEEE", "%W" )
				.replace( "EEE", "%a" )
				.replace( "ee", "%w" )
				.replace( "e", "%w" )

				//day of month
				.replace( "dd", "%d" )
				.replace( "d", "%e" )

				//day of year
				.replace( "DDD", "%j" )
				.replace( "DD", "%j" )
				.replace( "D", "%j" )

				//am pm
				.replace( "a", "%p" )

				//hour
				.replace( "hh", "%I" )
				.replace( "HH", "%H" )
				.replace( "h", "%l" )
				.replace( "H", "%k" )

				//minute
				.replace( "mm", "%i" )
				.replace( "m", "%i" )

				//second
				.replace( "ss", "%S" )
				.replace( "s", "%S" )

				//fractional seconds
				.replace( "SSSSSS", "%f" )
				.replace( "SSSSS", "%f" )
				.replace( "SSSS", "%f" )
				.replace( "SSS", "%f" )
				.replace( "SS", "%f" )
				.replace( "S", "%f" );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean supportsAlterTableConstraints() {
		return false;
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
		return SingleStoreLocalTemporaryTableStrategy.INSTANCE;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public int getMaxAliasLength() {
		return 64;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public int getMaxIdentifierLength() {
		return 64;
	}

	@Override
	public PredicateSupport getPredicateSupport() {
		return PredicateSupport.builder( super.getPredicateSupport() )
				.capability( PredicateSupport.Capability.TRUTHNESS, true )
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public CurrentTimestampSelection getCurrentTimestampSelection() {
		return CurrentTimestampSelection.prepared( "select now()" );
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
				case 1062:
					String constraintName = getViolatedConstraintNameExtractor().extractConstraintName( sqlException );
					return new ConstraintViolationException(
							message,
							sqlException,
							sql,
							ConstraintViolationException.ConstraintKind.UNIQUE,
							constraintName
					);
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
		builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		builder.setQuotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		return super.buildIdentifierHelper( request );
	}







	@Override
	public SubquerySupport getSubquerySupport() {
		return SubquerySupport.builder()
				.feature( SubquerySupport.Feature.OFFSET, true )
				.feature( SubquerySupport.Feature.NESTED_CORRELATION, false )
				.feature( SubquerySupport.Feature.MUTATION_TARGET_REFERENCE, false )
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	protected void contributeKeywords(KeywordRegistration registration) {
		super.contributeKeywords( registration );
		registration.registerKeyword( "key" );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public FunctionalDependencyAnalysisSupport getFunctionalDependencyAnalysisSupport() {
		return FunctionalDependencyAnalysisSupport.TABLE_GROUP;
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
		return action == org.hibernate.annotations.OnDeleteAction.NO_ACTION;
	}

	@Override
	public Exporter<Sequence> getSequenceExporter() {
		return NOOP_EXPORTER;
	}

	/**
	 * SingleStore does not support foreign keys and referential integrity
	 */
	@Override
	public Exporter<ForeignKey> getForeignKeyExporter() {
		return NOOP_EXPORTER;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public UniqueDelegate getUniqueDelegate() {
		return NOOP_UNIQUE_DELEGATE;
	}

	/**
	 * A no-op {@link Exporter} which is responsible for returning empty Create and Drop SQL strings.
	 */
	static class EmptyExporter<T extends Exportable> implements Exporter<T> {

		@Override
		public String[] getSqlCreateStrings(T exportable, Metadata metadata, SqlStringGenerationContext context) {
			return NO_COMMANDS;
		}

		@Override
		public String[] getSqlDropStrings(T exportable, Metadata metadata, SqlStringGenerationContext context) {
			return NO_COMMANDS;
		}
	}

	/**
	 * The default table type in SingleStore is 'columnstore'. The default can be changed to 'rowstore' by updating the
	 * 'default_table_type' engine variable to 'rowstore' or specify explicitly by property : 'hibernate.dialect.singlestore.table_type'.
	 * <a href="https://docs.singlestore.com/cloud/create-a-database/choosing-a-table-storage-type/">Choosing a Table Storage Type</a>
	 */
	public enum SingleStoreTableType {
		COLUMNSTORE, ROWSTORE;

		public static SingleStoreTableType fromValue(String value) {
			return Arrays.stream( values() )
					.filter( v -> v.name().equalsIgnoreCase( value.trim() ) )
					.findAny()
					.orElseThrow( () -> new IllegalArgumentException( "Wrong table type" ) );
		}
	}

	public SingleStoreTableType getExplicitTableType() {
		return explicitTableType;
	}

	public boolean isForUpdateLockingEnabled() {
		return isForUpdateLockingEnabled;
	}

	/**
	 * Specifies SingleStore explicit table type.
	 *
	 * @settingDefault {@code null}
	 */
	public static final String SINGLE_STORE_TABLE_TYPE = "hibernate.dialect.singlestore.table_type";
	/**
	 * Specifies SingleStore FOR UPDATE clause lock enable.
	 *
	 * @settingDefault {@code false}
	 */
	public static final String SINGLE_STORE_FOR_UPDATE_LOCK_ENABLED = "hibernate.dialect.singlestore.for_update_lock_enabled";

	@Override
	public SingleRowTableSupport getSingleRowTableSupport() {
		return SingleRowTableSupport.builder( super.getSingleRowTableSupport() )
				.tableExpression( "dual" )
				.build();
	}

	@Override
	public RowValueSupport getRowValueSupport() {
		return RowValueSupport.NONE;
	}

}
