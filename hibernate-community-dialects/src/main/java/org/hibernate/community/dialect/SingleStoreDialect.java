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
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.hibernate.Length;
import org.hibernate.PessimisticLockException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.FunctionalDependencyAnalysisSupport;
import org.hibernate.dialect.FunctionalDependencyAnalysisSupportImpl;
import org.hibernate.dialect.NullOrdering;
import org.hibernate.dialect.Replacer;
import org.hibernate.dialect.SelectItemReferenceStrategy;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.hint.IndexQueryHintHandler;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.MySQLIdentityColumnSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitLimitHandler;
import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableKind;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.jdbc.env.spi.SchemaNameResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
import org.hibernate.query.sqm.mutation.spi.BeforeUseAction;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
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
import org.hibernate.type.descriptor.sql.internal.CapacityDependentDdlType;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.internal.NativeEnumDdlTypeImpl;
import org.hibernate.type.descriptor.sql.internal.NativeOrdinalEnumDdlTypeImpl;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import jakarta.persistence.TemporalType;

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
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsDate;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsLocalTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithMicros;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithMillis;

/**
 * An SQL dialect for SingleStore.
 * <p>
 * The following are some of the key aspects and limitations of SingleStore that may affect Hibernate functionality:
 * </p>
 * <ul>
 *   <li>SingleStore supports two table types: COLUMNSTORE and ROWSTORE. Explicit table type can be configured by setting 'hibernate.dialect.singlestore.table_type' property. Refer to {@link SingleStoreTableType} for details.</li>
 *   <li>SingleStore has a random order for SELECT queries, which may impact the predictability of query results.</li>
 *   <li>SingleStore does not support foreign keys and referential integrity, which could affect the design of your database schema.</li>
 *   <li>The SingleStore dialect ignores unique key constraints. See {@link DoNothingUniqueDelegate} for more information.</li>
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
public class SingleStoreDialect extends Dialect {

	private static final int PARAM_LIST_SIZE_LIMIT = 1_048_576;
	private static final EmptyExporter NOOP_EXPORTER = new EmptyExporter();
	private static final UniqueDelegate NOOP_UNIQUE_DELEGATE = new DoNothingUniqueDelegate();
	private static final DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 8, 0 );

	private final SingleStoreTableType explicitTableType;
	private final boolean isForUpdateLockingEnabled;

	public SingleStoreDialect() {
		this( MINIMUM_VERSION, null, false );
	}

	public SingleStoreDialect(DialectResolutionInfo info) {
		this( createVersion( info ), getTableType( info ), getUpdateForEnabled( info ) );
		registerKeywords( info );
	}

	public SingleStoreDialect(
			DatabaseVersion version, SingleStoreTableType explicitTableType, boolean isForUpdateLockingEnabled) {
		super( version );
		this.explicitTableType = explicitTableType;
		this.isForUpdateLockingEnabled = isForUpdateLockingEnabled;
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
				info.getConfigurationValues(),
				false
		);
	}

	private final SizeStrategy sizeStrategy = new SizeStrategyImpl() {
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
							length == null ? getDefaultLobLength() : length
					);
				default:
					return super.resolveSize( jdbcType, javaType, precision, scale, length );
			}
		}
	};

	@Override
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_VERSION;
	}

	@Override
	public boolean useMaterializedLobWhenCapacityExceeded() {
		return false;
	}

	@Override
	public String extractPattern(TemporalUnit unit) {
		switch ( unit ) {
			case SECOND:
				return "(second(?2)+microsecond(?2)/1e6)";
			case WEEK:
				return "weekofyear(?2)";
			case DAY_OF_WEEK:
				return "dayofweek(?2)";
			case DAY_OF_MONTH:
				return "dayofmonth(?2)";
			case DAY_OF_YEAR:
				return "dayofyear(?2)";
			case EPOCH:
				return "unix_timestamp(?2)";
			default:
				return "?1(?2)";
		}
	}

	@Override
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
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		String fromType = fromTemporalType == TemporalType.TIME ? "to_timestamp(?2, 'HH24:MI:SS.FF6')" : "?2";
		String toType = toTemporalType == TemporalType.TIME ? "to_timestamp(?3, 'HH24:MI:SS.FF6')" : "?3";
		switch ( unit ) {
			case NANOSECOND:
				return String.format( "timestampdiff(microsecond,%s,%s)*1e3", fromType, toType );
			case NATIVE:
				return String.format( "timestampdiff(microsecond,%s,%s)", fromType, toType );
			default:
				return String.format( "timestampdiff(?1,%s,%s)", fromType, toType );
		}
	}

	@Override
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
					temporalAccessor = ( (ZonedDateTime) temporalAccessor ).toOffsetDateTime();
				}
				appender.appendSql( "timestamp('" );
				appendAsTimestampWithMicros(
						appender,
						temporalAccessor,
						supportsTemporalLiteralOffset(),
						jdbcTimeZone,
						false
				);
				appender.appendSql( "')" );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
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

	@Override
	public SelectItemReferenceStrategy getGroupBySelectItemReferenceStrategy() {
		return SelectItemReferenceStrategy.POSITION;
	}

	//Creating an index on an ENUM column on columnstore tables is not supported.
	@Override
	public String getEnumTypeDeclaration(String name, String[] values) {
		StringBuilder type = new StringBuilder();
		type.append( "enum (" );
		String separator = "";
		for ( String value : values ) {
			type.append( separator ).append( '\'' ).append( value ).append( '\'' );
			separator = ",";
		}
		return type.append( ')' ).toString();
	}

	@Override
	public String getQueryHintString(String query, String hints) {
		return IndexQueryHintHandler.INSTANCE.addQueryHints( query, hints );
	}

	@Override
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}

	private static final ViolatedConstraintNameExtractor EXTRACTOR = new TemplatedViolatedConstraintNameExtractor( sqle -> {
		final String sqlState = JdbcExceptionHelper.extractSqlState( sqle );
		if ( sqlState != null ) {
			if ( Integer.parseInt( sqlState ) == 23000 ) {
				return extractUsingTemplate( " for key '", "'", sqle.getMessage() );
			}
		}
		return null;
	} );

	@Override
	public boolean qualifyIndexName() {
		return false;
	}

	@Override
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( JSON, "json", this ) );

		final int maxTinyLobLen = 225;
		final int maxLobLen = 65_535;
		final int maxMediumLobLen = 16_777_215;

		final CapacityDependentDdlType.Builder varcharBuilder = CapacityDependentDdlType.builder(
				VARCHAR,
				CapacityDependentDdlType.LobKind.BIGGEST_LOB,
				columnType( CLOB ),
				columnType( CHAR ),
				castType( CHAR ),
				this
		).withTypeCapacity( getMaxVarcharLength(), "varchar($l)" ).withTypeCapacity( maxMediumLobLen, "mediumtext" );
		if ( getMaxVarcharLength() < maxLobLen ) {
			varcharBuilder.withTypeCapacity( maxLobLen, "text" );
		}
		ddlTypeRegistry.addDescriptor( varcharBuilder.build() );

		// SingleStore doesn't support nchar/nvarchar/ntext
		final CapacityDependentDdlType.Builder nvarcharBuilder = CapacityDependentDdlType.builder(
				NVARCHAR,
				CapacityDependentDdlType.LobKind.BIGGEST_LOB,
				columnType( NCLOB ),
				columnType( NCHAR ),
				castType( NCHAR ),
				this
		).withTypeCapacity( getMaxVarcharLength(), "varchar($l) character set utf8" ).withTypeCapacity(
				maxMediumLobLen,
				"mediumtext character set utf8"
		);
		if ( getMaxVarcharLength() < maxLobLen ) {
			nvarcharBuilder.withTypeCapacity( maxLobLen, "text character set utf8" );
		}
		ddlTypeRegistry.addDescriptor( nvarcharBuilder.build() );

		final CapacityDependentDdlType.Builder varbinaryBuilder = CapacityDependentDdlType.builder(
				VARBINARY,
				CapacityDependentDdlType.LobKind.BIGGEST_LOB,
				columnType( BLOB ),
				columnType( BINARY ),
				castType( BINARY ),
				this
		).withTypeCapacity( getMaxVarbinaryLength(), "varbinary($l)" ).withTypeCapacity(
				maxMediumLobLen,
				"mediumblob"
		);
		if ( getMaxVarbinaryLength() < maxLobLen ) {
			varbinaryBuilder.withTypeCapacity( maxLobLen, "blob" );
		}
		ddlTypeRegistry.addDescriptor( varbinaryBuilder.build() );

		ddlTypeRegistry.addDescriptor( new DdlTypeImpl(
				LONG32VARBINARY,
				columnType( BLOB ),
				castType( BINARY ),
				this
		) );
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( LONG32VARCHAR, columnType( CLOB ), castType( CHAR ), this ) );
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( LONG32NVARCHAR, columnType( CLOB ), castType( CHAR ), this ) );

		ddlTypeRegistry.addDescriptor( CapacityDependentDdlType.builder(
						BLOB,
						columnType( BLOB ),
						castType( BINARY ),
						this
				)
				.withTypeCapacity( maxTinyLobLen, "tinyblob" )
				.withTypeCapacity( maxMediumLobLen, "mediumblob" )
				.withTypeCapacity( maxLobLen, "blob" )
				.build() );

		ddlTypeRegistry.addDescriptor( CapacityDependentDdlType.builder(
						CLOB,
						columnType( CLOB ),
						castType( CHAR ),
						this
				)
				.withTypeCapacity( maxTinyLobLen, "tinytext" )
				.withTypeCapacity( maxMediumLobLen, "mediumtext" )
				.withTypeCapacity( maxLobLen, "text" )
				.build() );

		ddlTypeRegistry.addDescriptor( CapacityDependentDdlType.builder(
				NCLOB,
				columnType( NCLOB ),
				castType( NCHAR ),
				this
		).withTypeCapacity( maxTinyLobLen, "tinytext character set utf8" ).withTypeCapacity(
				maxMediumLobLen,
				"mediumtext character set utf8"
		).withTypeCapacity( maxLobLen, "text character set utf8" ).build() );

		ddlTypeRegistry.addDescriptor( new NativeEnumDdlTypeImpl( this ) );
		ddlTypeRegistry.addDescriptor( new NativeOrdinalEnumDdlTypeImpl( this ) );
	}

	@Override
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
		functionContributions.getFunctionRegistry()
				.namedDescriptorBuilder( "time" )
				.setExactArgumentCount( 1 )
				.setInvariantType( functionContributions.getTypeConfiguration()
				.getBasicTypeRegistry()
				.resolve( StandardBasicTypes.STRING ) )
				.register();
		functionContributions.getFunctionRegistry()
				.patternDescriptorBuilder( "median", "median(?1) over ()" )
				.setInvariantType( functionContributions.getTypeConfiguration()
				.getBasicTypeRegistry()
				.resolve( StandardBasicTypes.DOUBLE ) )
				.setExactArgumentCount( 1 )
				.setParameterTypes( NUMERIC )
				.register();
		BasicTypeRegistry basicTypeRegistry = functionContributions.getTypeConfiguration().getBasicTypeRegistry();
		SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();
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
	}


	@Override
	public String getCreateTableString() {
		return explicitTableType == null ? "create table" : String.format(
				"create %s table",
				explicitTableType.name().toLowerCase()
		);
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );

		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();

		typeContributions.contributeJdbcType( NullJdbcType.INSTANCE );

		// Until we remove StandardBasicTypes, we have to keep this
		typeContributions.contributeType( new NullType(
				NullJdbcType.INSTANCE,
				typeContributions.getTypeConfiguration()
						.getJavaTypeRegistry()
						.getDescriptor( Object.class )
		) );

		jdbcTypeRegistry.addDescriptor( EnumJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( OrdinalEnumJdbcType.INSTANCE );
	}

	@Override
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
	protected String columnType(int sqlTypeCode) {
		switch ( sqlTypeCode ) {
			case BOOLEAN:
				return "bit";
			case TIMESTAMP:
				return "datetime($p)";
			case TIMESTAMP_WITH_TIMEZONE:
				return "timestamp($p)";
			case TIME_WITH_TIMEZONE:
				return "time($p)";
			case SqlTypes.NUMERIC:
				return columnType( DECIMAL );
			case FLOAT:
				// Avoid using float type because
				// SingleStore has potential inaccuracy when using the = or != comparison operators on FLOAT columns in WHERE clause
				return columnType( DOUBLE );
			case NCHAR:
				return "char($l) character set utf8";
			case NVARCHAR:
				return "varchar($l) character set utf8";
			case BLOB:
				return "longblob";
			case NCLOB:
				return "longtext character set utf8";
			case CLOB:
				return "longtext";
			default:
				return super.columnType( sqlTypeCode );
		}
	}

	@Override
	public String castPattern(CastType from, CastType to) {
		if ( CastType.FLOAT == to || CastType.DOUBLE == to || CastType.OTHER == to ) {
			return "?1 :> ?2";
		}
		return super.castPattern( from, to );
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return Types.BIT;
	}

	@Override
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
				return "signed";
			case CHAR:
			case VARCHAR:
			case LONG32VARCHAR:
				return "char";
			case NCHAR:
			case NVARCHAR:
			case LONG32NVARCHAR:
				return "char character set utf8";
			case BINARY:
			case VARBINARY:
			case LONG32VARBINARY:
				return "binary";
		}
		return super.castType( sqlTypeCode );
	}

	@Override
	public SizeStrategy getSizeStrategy() {
		return sizeStrategy;
	}

	@Override
	public int getFloatPrecision() {
		//the maximum precision for 4 bytes
		return 23;
	}

	@Override
	public String currentTimestamp() {
		return "current_timestamp(6)";
	}

	@Override
	public long getFractionalSecondPrecisionInNanos() {
		return 1_000; //microseconds
	}

	@Override
	public long getDefaultLobLength() {
		return Length.LONG32;
	}

	@Override
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
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new SingleStoreSqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	@Override
	public SchemaNameResolver getSchemaNameResolver() {
		return (connection, dialect) -> "";
	}

	@Override
	public int getInExpressionCountLimit() {
		return PARAM_LIST_SIZE_LIMIT;
	}

	/**
	 * The biggest size value that can be supplied as argument
	 */
	@Override
	public int getMaxVarbinaryLength() {
		return 65_533;
	}

	@Override
	public int getMaxVarcharLength() {
		return 21_844;
	}

	@Override
	public String getNullColumnString(String columnType) {
		if ( columnType.regionMatches( true, 0, "timestamp", 0, "timestamp".length() ) ) {
			return " null";
		}
		return super.getNullColumnString( columnType );
	}

	/**
	 * Feature 'Check constraints' is not supported by SingleStore.
	 */
	@Override
	public boolean supportsColumnCheck() {
		return false;
	}

	/**
	 * Feature 'Check constraints' is not supported by SingleStore.
	 */
	public boolean supportsTableCheck() {
		return false;
	}

	@Override
	public int getDefaultDecimalPrecision() {
		return 65;
	}

	@Override
	public boolean doesRoundTemporalOnOverflow() {
		return false;
	}

	@Override
	public boolean supportsWindowFunctions() {
		return true;
	}

	@Override
	public boolean supportsRecursiveCTE() {
		return true;
	}

	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
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
	public void appendDatetimeFormat(SqlAppender appender, String format) {
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
	public String getDropForeignKeyString() {
		throw new UnsupportedOperationException( "SingleStore does not support foreign keys and referential integrity" );
	}

	@Override
	public String getDropUniqueKeyString() {
		return "drop index";
	}

	@Override
	public String getAlterColumnTypeString(String columnName, String columnType, String columnDefinition) {
		// no way to change just the column type, leaving other attributes intact
		return "modify column " + columnName + " " + columnDefinition.trim();
	}

	/**
	 * SingleStore doesn't support modifying column type on columnstore tables.
	 * It only supports modifying column type on rowstore table.
	 */
	@Override
	public boolean supportsAlterColumnType() {
		return false;
	}

	@Override
	public LimitHandler getLimitHandler() {
		//also supports LIMIT n OFFSET m
		return LimitLimitHandler.INSTANCE;
	}

	@Override
	public char closeQuote() {
		return '`';
	}

	@Override
	public char openQuote() {
		return '`';
	}

	@Override
	public boolean canCreateCatalog() {
		return true;
	}

	@Override
	public String[] getCreateCatalogCommand(String catalogName) {
		return new String[] { "create database " + catalogName };
	}

	@Override
	public String[] getDropCatalogCommand(String catalogName) {
		return new String[] { "drop database " + catalogName };
	}

	@Override
	public boolean canCreateSchema() {
		return false;
	}

	@Override
	public String[] getCreateSchemaCommand(String schemaName) {
		throw new UnsupportedOperationException(
				"SingleStore does not support dropping creating/dropping schemas in the JDBC sense" );
	}

	@Override
	public String[] getDropSchemaCommand(String schemaName) {
		throw new UnsupportedOperationException(
				"SingleStore does not support dropping creating/dropping schemas in the JDBC sense" );
	}

	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return true;
	}

	@Override
	public String getSelectGUIDString() {
		return "select uuid()";
	}

	@Override
	public boolean supportsCommentOn() {
		return true;
	}

	@Override
	public String getTableComment(String comment) {
		return " comment='" + comment + "'";
	}

	@Override
	public String getColumnComment(String comment) {
		return " comment '" + comment + "'";
	}

	@Override
	public NullOrdering getNullOrdering() {
		return NullOrdering.SMALLEST;
	}

	@Override
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType rootEntityDescriptor, RuntimeModelCreationContext runtimeModelCreationContext) {

		return new LocalTemporaryTableMutationStrategy( TemporaryTable.createIdTable(
				rootEntityDescriptor,
				basename -> TemporaryTable.ID_TABLE_PREFIX + basename,
				this,
				runtimeModelCreationContext
		), runtimeModelCreationContext.getSessionFactory() );
	}

	@Override
	public SqmMultiTableInsertStrategy getFallbackSqmInsertStrategy(
			EntityMappingType rootEntityDescriptor, RuntimeModelCreationContext runtimeModelCreationContext) {

		return new LocalTemporaryTableInsertStrategy( TemporaryTable.createEntityTable(
				rootEntityDescriptor,
				name -> TemporaryTable.ENTITY_TABLE_PREFIX + name,
				this,
				runtimeModelCreationContext
		), runtimeModelCreationContext.getSessionFactory() );
	}

	@Override
	public TemporaryTableKind getSupportedTemporaryTableKind() {
		return TemporaryTableKind.LOCAL;
	}

	@Override
	public String getTemporaryTableCreateCommand() {
		return "create temporary table if not exists";
	}

	//SingleStore throws an error on drop temporary table if there are uncommited statements within transaction.
	//Just 'drop table' statement causes implicit commit, so using 'delete from'.
	@Override
	public String getTemporaryTableDropCommand() {
		return "delete from";
	}

	@Override
	public AfterUseAction getTemporaryTableAfterUseAction() {
		return AfterUseAction.DROP;
	}

	@Override
	public BeforeUseAction getTemporaryTableBeforeUseAction() {
		return BeforeUseAction.CREATE;
	}

	@Override
	public int getMaxAliasLength() {
		return 64;
	}

	@Override
	public int getMaxIdentifierLength() {
		return 64;
	}

	@Override
	public boolean supportsIsTrue() {
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
	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		throw new UnsupportedOperationException( "SingleStore does not support resultsets via stored procedures." );
	}

	@Override
	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
		boolean isResultSet = ps.execute();
		while ( !isResultSet && ps.getUpdateCount() != -1 ) {
			isResultSet = ps.getMoreResults();
		}
		return ps.getResultSet();
	}

	@Override
	public boolean supportsNullPrecedence() {
		return false;
	}

	@Override
	public boolean supportsLobValueChangePropagation() {
		return false;
	}

	@Override
	public boolean supportsSubqueryOnMutatingTable() {
		return false;
	}

	@Override
	public boolean supportsLockTimeouts() {
		return false;
	}

	@Override
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
	public NameQualifierSupport getNameQualifierSupport() {
		return NameQualifierSupport.CATALOG;
	}

	@Override
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuilder builder, DatabaseMetaData dbMetaData)
			throws SQLException {
		builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		builder.setQuotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		return super.buildIdentifierHelper( builder, dbMetaData );
	}

	@Override
	public String getAddForeignKeyConstraintString(
			String constraintName,
			String[] foreignKey,
			String referencedTable,
			String[] primaryKey,
			boolean referencesPrimaryKey) {
		throw new UnsupportedOperationException( "SingleStore does not support foreign keys and referential integrity." );
	}

	@Override
	public String getAddForeignKeyConstraintString(
			String constraintName, String foreignKeyDefinition) {
		throw new UnsupportedOperationException( "SingleStore does not support foreign keys and referential integrity." );
	}

	@Override
	public String getAddPrimaryKeyConstraintString(String constraintName) {
		throw new UnsupportedOperationException( "SingleStore does not support altering primary key." );
	}

	@Override
	public String getWriteLockString(String aliases, int timeout) {
		return getForUpdateString( aliases );
	}

	@Override
	public String getForUpdateSkipLockedString(String aliases) {
		return getForUpdateString();
	}

	@Override
	public String getForUpdateNowaitString(String aliases) {
		return getForUpdateString();
	}


	@Override
	public String getForUpdateString() {
		return isForUpdateLockingEnabled ? super.getForUpdateString() : "";
	}

	@Override
	public boolean supportsOffsetInSubquery() {
		return true;
	}

	@Override
	public boolean supportsPartitionBy() {
		return true;
	}

	@Override
	public boolean supportsWait() {
		return false;
	}

	@Override
	protected void registerDefaultKeywords() {
		super.registerDefaultKeywords();
		registerKeyword( "key" );
	}

	@Override
	public FunctionalDependencyAnalysisSupport getFunctionalDependencyAnalysisSupport() {
		return FunctionalDependencyAnalysisSupportImpl.TABLE_GROUP;
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
	public boolean supportsCircularCascadeDeleteConstraints() {
		return false;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return MySQLIdentityColumnSupport.INSTANCE;
	}

	@Override
	public boolean isJdbcLogWarningsEnabledByDefault() {
		return false;
	}

	@Override
	public boolean supportsCascadeDelete() {
		return false;
	}

	@Override
	public boolean supportsNonQueryWithCTE() {
		return true;
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
	public UniqueDelegate getUniqueDelegate() {
		return NOOP_UNIQUE_DELEGATE;
	}

	/**
	 * A no-op {@link Exporter} which is responsible for returning empty Create and Drop SQL strings.
	 */
	static class EmptyExporter<T extends Exportable> implements Exporter<T> {

		@Override
		public String[] getSqlCreateStrings(T exportable, Metadata metadata, SqlStringGenerationContext context) {
			return ArrayHelper.EMPTY_STRING_ARRAY;
		}

		@Override
		public String[] getSqlDropStrings(T exportable, Metadata metadata, SqlStringGenerationContext context) {
			return ArrayHelper.EMPTY_STRING_ARRAY;
		}
	}

	/**
	 * Because of hibernate requires that entity tables have primary key separate unique keys are restricted.
	 * SingleStore restrictions:
	 * - Primary key in SingleStore table is unique key and shard key
	 * - SingleStore table allows only single shard key
	 * - SingleStore unique keys must contain all columns of the shard key: <a href="https://docs.singlestore.com/docs/unique-key-restrictions">Unique Key restrictions</a>.
	 * - Shard key fields cannot be updated (or altered) so they must be fields that never change
	 */
	static class DoNothingUniqueDelegate implements UniqueDelegate {

		@Override
		public String getColumnDefinitionUniquenessFragment(Column column, SqlStringGenerationContext context) {
			return "";
		}

		@Override
		public String getTableCreationUniqueConstraintsFragment(Table table, SqlStringGenerationContext context) {
			return "";
		}

		@Override
		public String getAlterTableToAddUniqueKeyCommand(
				UniqueKey uniqueKey, Metadata metadata, SqlStringGenerationContext context) {
			return "";
		}

		@Override
		public String getAlterTableToDropUniqueKeyCommand(
				UniqueKey uniqueKey, Metadata metadata, SqlStringGenerationContext context) {
			return "";
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
}
