/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

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
import org.hibernate.dialect.type.spi.UserDefinedTypeDdlSupport;
import org.hibernate.dialect.lob.spi.LobSupport;
import org.hibernate.dialect.lob.spi.LobSupports;
import org.hibernate.dialect.rowid.spi.RowIdSupport;
import org.hibernate.dialect.rowid.spi.RowIdSupports;
import org.hibernate.dialect.queryhint.spi.QueryHintPlacement;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;

import org.hibernate.dialect.mutation.spi.MultiTableMutationSupport;
import org.hibernate.dialect.array.spi.ArraySupport;
import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.MutationKind;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxCapability;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxSupport;
import org.hibernate.dialect.sql.ast.spi.PredicateSupport;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.ValuesListSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;

import jakarta.persistence.GenerationType;
import jakarta.persistence.TemporalType;
import jakarta.annotation.Nullable;
import org.hibernate.Length;
import org.hibernate.PessimisticLockException;
import org.hibernate.QueryTimeoutException;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.community.dialect.sequence.PostgreSQLLegacySequenceSupport;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.sql.ast.spi.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.aggregate.spi.FunctionalDependencyAnalysisSupport;
import org.hibernate.dialect.type.spi.NationalizationSupport;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.jdbc.spi.PostgreSQLDriverKind;
import org.hibernate.dialect.function.spi.Replacer;
import org.hibernate.dialect.function.spi.TupleCountSupport;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;
import org.hibernate.dialect.type.spi.TimeZoneSupport;
import org.hibernate.dialect.aggregate.spi.AggregateSupport;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.PostgreSQLMinMaxFunction;
import org.hibernate.dialect.function.PostgreSQLTruncFunction;
import org.hibernate.dialect.function.PostgreSQLTruncRoundFunction;
import org.hibernate.community.dialect.identity.internal.PostgreSQLIdentityColumnSupport;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.StandardLockingSupports;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.LimitOffsetLimitHandler;
import org.hibernate.dialect.pagination.spi.OffsetFetchLimitHandler;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.schema.spi.ColumnDefinitionRequest;
import org.hibernate.dialect.schema.spi.ConstraintDropMode;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.schema.spi.IfExistsSupport;
import org.hibernate.dialect.schema.spi.IndexNameQualification;
import org.hibernate.dialect.schema.spi.SchemaDropSupport;
import org.hibernate.dialect.schema.spi.TruncateMode;
import org.hibernate.dialect.schema.spi.TruncateRequest;
import org.hibernate.dialect.temptable.spi.StandardLocalTemporaryTableStrategy;
import org.hibernate.dialect.temptable.spi.TemporaryTableStrategy;
import org.hibernate.dialect.type.spi.PostgreSQLJdbcTypes;
import org.hibernate.dialect.unique.spi.UniqueDelegates;
import org.hibernate.dialect.unique.spi.UniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupportFactory;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupports;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.dialect.identifier.spi.IdentifierHelperBuildRequest;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.exception.LockAcquisitionException;
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
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.exec.spi.JdbcOperation;
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
import java.util.TimeZone;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.internal.util.StringHelper.unroot;
import static org.hibernate.query.common.TemporalUnit.DAY;
import static org.hibernate.query.common.TemporalUnit.EPOCH;
import static org.hibernate.type.SqlTypes.ARRAY;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.FLOAT;
import static org.hibernate.type.SqlTypes.GEOGRAPHY;
import static org.hibernate.type.SqlTypes.GEOMETRY;
import static org.hibernate.type.SqlTypes.INET;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.LONG32NVARCHAR;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.LONG32VARCHAR;
import static org.hibernate.type.SqlTypes.NCHAR;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.NVARCHAR;
import static org.hibernate.type.SqlTypes.OTHER;
import static org.hibernate.type.SqlTypes.SQLXML;
import static org.hibernate.type.SqlTypes.STRUCT;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_UTC;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
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
 * A {@linkplain Dialect SQL dialect} for PostgreSQL 8 and above.
 *
 * @author Gavin King
 * @author Yoobin Yoon
 */
public class PostgreSQLLegacyDialect extends Dialect implements CurrentTemporalSupport, TemporalFormatSupport, TemporalOperationSupport {
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
	private static final RefCursorSupportFactory REF_CURSOR_SUPPORT_FACTORY = RefCursorSupports.postgresql();
	private final TypeSizingProfile typeSizingProfile = TypeSizingProfile.builder( super.getTypeSizingProfile() )
			.defaultIntervalSecondScale( 6 )
			.maxVarcharLength( 10_485_760 ).maxVarcharCapacity( 1_073_741_824 )
			.maxNVarcharLength( 10_485_760 ).maxNVarcharCapacity( 10_485_760 )
			.maxVarbinaryLength( Length.LONG32 ).maxVarbinaryCapacity( Length.LONG32 )
			.build();

	@Override public TypeSizingProfile getTypeSizingProfile() { return typeSizingProfile; }

	protected final PostgreSQLDriverKind driverKind;
	private final UniqueDelegate uniqueDelegate = new org.hibernate.dialect.unique.spi.DelegatingUniqueDelegate(
			UniqueDelegates.createTable( this ) ) {
		@Override
		public boolean supportsNullsNotDistinct() {
			return getVersion().isSameOrAfter( 15 );
		}
	};
	private final StandardTableExporter postgresqlTableExporter = new StandardTableExporter( this ) {
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
			new UserDefinedTypeDdlSupport(
					"",
					"",
					getVersion().isSameOrAfter( 8, 2 )
							? ExistenceCheckPlacement.BEFORE_NAME
							: ExistenceCheckPlacement.NONE
			)
	);

	private final LockingSupport lockingSupport;

	public PostgreSQLLegacyDialect() {
		this( DatabaseVersion.make( 8, 0 ) );
	}

	public PostgreSQLLegacyDialect(DialectResolutionInfo info) {
		super(info);
		driverKind = PostgreSQLDriverKind.determineKind( info );
		lockingSupport = buildLockingSupport();
	}

	public PostgreSQLLegacyDialect(DatabaseVersion version) {
		super(version);
		driverKind = PostgreSQLDriverKind.PG_JDBC;
		lockingSupport = buildLockingSupport();
	}

	public PostgreSQLLegacyDialect(DatabaseVersion version, PostgreSQLDriverKind driverKind) {
		super(version);
		this.driverKind = driverKind;
		lockingSupport = buildLockingSupport();
	}

	private LockingSupport buildLockingSupport() {
		return StandardLockingSupports.postgresql( getVersion() );
	}

	@Override
	public LockingSupport getLockingSupport() {
		return lockingSupport;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void contributeDefaultProperties(java.util.Properties properties) {
		super.contributeDefaultProperties( properties );
		properties.setProperty( org.hibernate.cfg.AvailableSettings.NON_CONTEXTUAL_LOB_CREATION, "true" );
		properties.setProperty( org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE, "15" );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		return switch ( sqlTypeCode ) {
			// no tinyint, not even in Postgres 11
			case TINYINT -> "smallint";
			// there are no nchar/nvarchar types in Postgres
			case NCHAR -> columnType( CHAR );
			case NVARCHAR -> columnType( VARCHAR );
			// since there's no real difference between TEXT and VARCHAR,
			// except for the length limit, we can just use 'text' for the
			// "long" string types
			case LONG32VARCHAR, LONG32NVARCHAR -> "text";
			// use oid as the blob/clob type on Postgres because
			// the JDBC driver doesn't allow using bytea/text through LOB APIs
			case BLOB, CLOB, NCLOB -> "oid";
			// use bytea as the "long" binary type (that there is no
			// real VARBINARY type in Postgres, so we always use this)
			case BINARY, VARBINARY, LONG32VARBINARY -> "bytea";

			// We do not use the time with timezone type because PG deprecated it and it lacks certain operations like subtraction
//			case TIME_UTC:
//				return columnType( TIME_WITH_TIMEZONE );

			case TIMESTAMP_UTC -> columnType( TIMESTAMP_WITH_TIMEZONE );

			default -> super.columnType( sqlTypeCode );
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public EnumSupport getEnumSupport() {
		return EnumSupports.postgresql();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String castType(int sqlTypeCode) {
		return switch ( sqlTypeCode ) {
			case CHAR, NCHAR, VARCHAR, NVARCHAR -> "varchar";
			case LONG32VARCHAR, LONG32NVARCHAR -> "text";
			case BINARY, VARBINARY, LONG32VARBINARY ->"bytea";
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

		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( SQLXML, "xml", this ) );
		if ( getVersion().isSameOrAfter( 8, 2 ) ) {
			ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( UUID, "uuid", this ) );
		}
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( INET, "inet", this ) );
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( GEOMETRY, "geometry", this ) );
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( GEOGRAPHY, "geography", this ) );
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.scale6IntervalSecond( this ) );

		if ( getVersion().isSameOrAfter( 9, 2 ) ) {
			// Prefer jsonb if possible
			if ( getVersion().isSameOrAfter( 9, 4 ) ) {
				ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( JSON, "jsonb", this ) );
			}
			else {
				ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( JSON, "json", this ) );
			}
		}
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
				// The PostgreSQL JDBC driver reports TIME for timetz, but we use it only for mapping OffsetTime to UTC
				if ( "timetz".equals( columnTypeName ) ) {
					jdbcTypeCode = TIME_UTC;
				}
				break;
			case TIMESTAMP:
				// The PostgreSQL JDBC driver reports TIMESTAMP for timestamptz, but we use it only for mapping Instant
				if ( "timestamptz".equals( columnTypeName ) ) {
					jdbcTypeCode = TIMESTAMP_UTC;
				}
				break;
			case ARRAY:
				// PostgreSQL names array types by prepending an underscore to the base name
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
		switch ( columnTypeName ) {
			case "bool":
				return Types.BOOLEAN;
			case "float4":
				// Use REAL instead of FLOAT to get Float as recommended Java type
				return Types.REAL;
			case "float8":
				return Types.DOUBLE;
			case "int2":
				return Types.SMALLINT;
			case "int4":
				return Types.INTEGER;
			case "int8":
				return Types.BIGINT;
		}
		return super.resolveSqlTypeCode( columnTypeName, typeConfiguration );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTime() {
		return "localtime";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTimestamp() {
		return "localtimestamp";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTimestampWithTimeZone() {
		return "current_timestamp";
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
		switch ( unit ) {
			case DAY_OF_WEEK:
				return "(" + TemporalOperationSupports.standard().extractPattern(unit) + "+1)";
			default:
				return TemporalOperationSupports.standard().extractPattern(unit);
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String castPattern(CastType from, CastType to) {
		if ( from == CastType.STRING && to == CastType.BOOLEAN ) {
			return "cast(?1 as ?2)";
		}
		else {
			return super.castPattern( from, to );
		}
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

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		if ( intervalType != null ) {
			return "(?2+?3)";
		}
		switch ( unit ) {
			case NANOSECOND:
				return "(?3+(?2)/1e3*interval '1 microsecond')";
			case NATIVE:
				return "(?3+(?2)*interval '1 second')";
			case QUARTER: //quarter is not supported in interval literals
				return "(?3+(?2)*interval '3 month')";
			case WEEK: //week is not supported in interval literals
				return "(?3+(?2)*interval '7 day')";
			default:
				return "(?3+(?2)*interval '1 ?1')";
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		if ( unit == null ) {
			return "(?3-?2)";
		}
		if ( toTemporalType == TemporalType.DATE && fromTemporalType == TemporalType.DATE ) {
			// special case: subtraction of two dates
			// results in an integer number of days
			// instead of an INTERVAL
			switch ( unit ) {
				case YEAR:
				case MONTH:
				case QUARTER:
					return "extract(" + translateDurationField( unit ) + " from age(?3,?2))";
				default:
					return "(?3-?2)" + DAY.conversionFactor( unit, this );
			}
		}
		else {
			switch ( unit ) {
				case YEAR:
					return "extract(year from ?3-?2)";
				case QUARTER:
					return "(extract(year from ?3-?2)*4+extract(month from ?3-?2)/3)";
				case MONTH:
					return "(extract(year from ?3-?2)*12+extract(month from ?3-?2))";
				case WEEK: //week is not supported by extract() when the argument is a duration
					return "(extract(day from ?3-?2)/7)";
				case DAY:
					return "extract(day from ?3-?2)";
				//in order to avoid multiple calls to extract(),
				//we use extract(epoch from x - y) * factor for
				//all the following units:
				case HOUR:
				case MINUTE:
				case SECOND:
				case NANOSECOND:
				case NATIVE:
					return "extract(epoch from ?3-?2)" + EPOCH.conversionFactor( unit, this );
				default:
					throw new SemanticException( "unrecognized field: " + unit );
			}
		}
	}

	@Deprecated
	protected void extractField(
			StringBuilder pattern,
			TemporalUnit unit,
			TemporalType fromTimestamp,
			TemporalType toTimestamp,
			TemporalUnit toUnit) {
		pattern.append( "extract(" );
		pattern.append( translateDurationField( unit ) );
		pattern.append( " from " );
		if ( toTimestamp == TemporalType.DATE && fromTimestamp == TemporalType.DATE ) {
			// special case subtraction of two
			// dates results in an integer not
			// an Interval
			pattern.append( "age(?3,?2)" );
		}
		else {
			switch ( unit ) {
				case YEAR:
				case MONTH:
				case QUARTER:
					pattern.append( "age(?3,?2)" );
					break;
				case DAY:
				case HOUR:
				case MINUTE:
				case SECOND:
				case EPOCH:
					pattern.append( "?3-?2" );
					break;
				default:
					throw new SemanticException( unit + " is not a legal field" );
			}
		}
		pattern.append( ")" ).append( unit.conversionFactor( toUnit, this ) );
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

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);

		functionFactory.cot();
		functionFactory.radians();
		functionFactory.degrees();
		functionFactory.log();
		functionFactory.mod_operator();
		if ( getVersion().isSameOrAfter( 12 ) ) {
			functionFactory.log10();
			functionFactory.tanh();
			functionFactory.sinh();
			functionFactory.cosh();
			functionFactory.moreHyperbolic();
		}
		else {
			functionContributions.getFunctionRegistry().registerAlternateKey( "log10", "log" );
		}
		functionFactory.cbrt();
		functionFactory.pi();
		functionFactory.trim2();
		functionFactory.repeat();
		functionFactory.md5();
		functionFactory.initcap();
		functionFactory.substr();
		functionFactory.substring_substr();
		functionFactory.reverse();
		//also natively supports ANSI-style substring()
		functionFactory.translate();
		functionFactory.toCharNumberDateTimestamp();
		functionFactory.concat_pipeOperator( "convert_from(lo_get(?1),pg_client_encoding())" );
		functionFactory.localtimeLocaltimestamp();
		functionFactory.length_characterLength_pattern( "length(lo_get(?1),pg_client_encoding())" );
		functionFactory.bitLength_pattern( "bit_length(?1)", "length(lo_get(?1))*8" );
		functionFactory.octetLength_pattern( "octet_length(?1)", "length(lo_get(?1))" );
		functionFactory.ascii();
		functionFactory.char_chr();
		functionFactory.position();
		functionFactory.bitandorxornot_operator();
		functionFactory.bitAndOr();
		functionFactory.everyAny_boolAndOr();
		functionFactory.median_percentileCont( false );
		functionFactory.stddev();
		functionFactory.stddevPopSamp();
		functionFactory.variance();
		functionFactory.varPopSamp();
		functionFactory.covarPopSamp();
		functionFactory.corr();
		functionFactory.regrLinearRegressionAggregates();
		functionFactory.insert_overlay();
		functionFactory.overlay();
		functionFactory.soundex(); //was introduced in Postgres 9 apparently

		functionFactory.locate_positionSubstring();
		functionFactory.windowFunctions();
		functionFactory.listagg_stringAgg( "varchar" );
		functionFactory.array_postgresql();
		functionFactory.arrayAggregate();
		functionFactory.arrayPosition_postgresql();
		functionFactory.arrayPositions_postgresql();
		functionFactory.arrayLength_cardinality();
		functionFactory.arrayConcat_postgresql();
		functionFactory.arrayPrepend_postgresql();
		functionFactory.arrayAppend_postgresql();
		functionFactory.arrayContains_postgresql();
		functionFactory.arrayIntersects_postgresql();
		functionFactory.arrayGet_bracket();
		functionFactory.arraySet_unnest();
		functionFactory.arrayRemove();
		functionFactory.arrayRemoveIndex_unnest( true );
		functionFactory.arraySlice_operator();
		functionFactory.arrayReplace();
		if ( getVersion().isSameOrAfter( 14 ) ) {
			functionFactory.arrayTrim_trim_array();
		}
		else {
			functionFactory.arrayTrim_unnest();
		}
		if ( getVersion().isSameOrAfter( 18 ) ) {
			functionFactory.arrayReverse();
			functionFactory.arraySort();
		}
		else {
			functionFactory.arrayReverse_unnest();
			functionFactory.arraySort_unnest();
		}
		functionFactory.arrayFill_postgresql();
		functionFactory.arrayToString_postgresql();

		if ( getVersion().isSameOrAfter( 17 ) ) {
			functionFactory.jsonValue_postgresql( true );
			functionFactory.jsonQuery();
			functionFactory.jsonExists();
			functionFactory.jsonObject_postgresql( true );
			functionFactory.jsonArray_postgresql( true );
			functionFactory.jsonArrayAgg_postgresql( true );
			functionFactory.jsonObjectAgg_postgresql( true );
			functionFactory.jsonTable();
		}
		else {
			functionFactory.jsonValue_postgresql( false );
			functionFactory.jsonQuery_postgresql();
			functionFactory.jsonExists_postgresql();
			if ( getVersion().isSameOrAfter( 16 ) ) {
				functionFactory.jsonObject_postgresql( true );
				functionFactory.jsonArray_postgresql( true );
				functionFactory.jsonArrayAgg_postgresql( true );
				functionFactory.jsonObjectAgg_postgresql( true );
			}
			else {
				functionFactory.jsonObject_postgresql( false );
				functionFactory.jsonArray_postgresql( false );
				functionFactory.jsonArrayAgg_postgresql( false );
				functionFactory.jsonObjectAgg_postgresql( false );
			}
			functionFactory.jsonTable_postgresql();
		}
		functionFactory.jsonSet_postgresql();
		functionFactory.jsonRemove_postgresql();
		functionFactory.jsonReplace_postgresql();
		functionFactory.jsonInsert_postgresql();
		if ( getVersion().isSameOrAfter( 13 ) ) {
			// Requires support for WITH clause in subquery which only 13+ provides
			functionFactory.jsonMergepatch_postgresql();
		}
		functionFactory.jsonArrayAppend_postgresql( getVersion().isSameOrAfter( 13 ) );
		functionFactory.jsonArrayInsert_postgresql();

		functionFactory.xmlelement();
		functionFactory.xmlcomment();
		functionFactory.xmlforest();
		functionFactory.xmlconcat();
		functionFactory.xmlpi();
		functionFactory.xmlquery_postgresql();
		functionFactory.xmlexists();
		functionFactory.xmlagg();
		functionFactory.xmltable( true );

		if ( getVersion().isSameOrAfter( 9, 4 ) ) {
			functionFactory.makeDateTimeTimestamp();
			// Note that PostgreSQL doesn't support the OVER clause for ordered set-aggregate functions
			functionFactory.inverseDistributionOrderedSetAggregates();
			functionFactory.hypotheticalOrderedSetAggregates();
		}

		if ( !supportsMinMaxOnUuid() ) {
			functionContributions.getFunctionRegistry().register( "min", new PostgreSQLMinMaxFunction( "min" ) );
			functionContributions.getFunctionRegistry().register( "max", new PostgreSQLMinMaxFunction( "max" ) );
		}

		// Postgres uses # instead of ^ for XOR
		functionContributions.getFunctionRegistry().patternDescriptorBuilder( "bitxor", "(?1#?2)" )
				.setExactArgumentCount( 2 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();

		functionContributions.getFunctionRegistry().register(
				"round", new PostgreSQLTruncRoundFunction( "round", true )
		);
		functionContributions.getFunctionRegistry().register(
				"trunc",
				new PostgreSQLTruncFunction( true, functionContributions.getTypeConfiguration() )
		);
		functionContributions.getFunctionRegistry().registerAlternateKey( "truncate", "trunc" );
		functionFactory.dateTrunc();

		functionFactory.unnest_postgresql( getVersion().isSameOrAfter( 17 ) );
		functionFactory.generateSeries( null, "ordinality", false );

		functionFactory.regexpLike_postgresql( getVersion().isSameOrAfter( 15 ) );
	}

	@Override
	public @Nullable String getDefaultOrdinalityColumnName() {
		return "ordinality";
	}

	/**
	 * Whether PostgreSQL supports `min(uuid)`/`max(uuid)` which it doesn't by default.
	 * Since the emulation is not very performant, this can be overridden by users which
	 * make sure that an aggregate function for uuid exists on their database.
	 *
	 * The following definitions can be used for this purpose:
	 *
	 * <code>
	 * create or replace function min(uuid, uuid)
	 *     returns uuid
	 *     immutable parallel safe
	 *     language plpgsql as
	 * $$
	 * begin
	 *     return least($1, $2);
	 * end
	 * $$;
	 *
	 * create aggregate min(uuid) (
	 *     sfunc = min,
	 *     stype = uuid,
	 *     combinefunc = min,
	 *     parallel = safe,
	 *     sortop = operator (&lt;)
	 *     );
	 *
	 * create or replace function max(uuid, uuid)
	 *     returns uuid
	 *     immutable parallel safe
	 *     language plpgsql as
	 * $$
	 * begin
	 *     return greatest($1, $2);
	 * end
	 * $$;
	 *
	 * create aggregate max(uuid) (
	 *     sfunc = max,
	 *     stype = uuid,
	 *     combinefunc = max,
	 *     parallel = safe,
	 *     sortop = operator (&gt;)
	 *     );
	 * </code>
	 */
	protected boolean supportsMinMaxOnUuid() {
		return false;
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
		return PredicateSupport.builder( super.getPredicateSupport() )
				.caseInsensitiveLikeOperator( "ilike" )
				.capability( PredicateSupport.Capability.DISTINCT_FROM, true )
				.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized IfExistsSupport getIfExistsSupport() {
		if ( ifExistsSupport == null ) {
			ifExistsSupport = new IfExistsSupport(
				getVersion().isSameOrAfter( 9, 2 ) ? ExistenceCheckPlacement.BEFORE_NAME : ExistenceCheckPlacement.NONE,
				getVersion().isSameOrAfter( 8, 2 ) ? ExistenceCheckPlacement.BEFORE_NAME : ExistenceCheckPlacement.NONE,
				getVersion().isSameOrAfter( 9 ) ? ExistenceCheckPlacement.BEFORE_NAME : ExistenceCheckPlacement.NONE,
				ExistenceCheckPlacement.NONE
		);
		}
		return ifExistsSupport;
	}

	@Override
	public ValuesListSupport getValuesListSupport() {
		return getVersion().isSameOrAfter( 8, 2 ) ? ValuesListSupport.STANDARD : ValuesListSupport.INSERT_ONLY;
	}

	@Override
	public CteSupport getCteSupport() {
		final CteSupport.Builder builder = CteSupport.builder()
				.recursiveFeatures( CteSupport.RecursiveFeature.RECURSIVE );
		if ( getVersion().isSameOrAfter( 9, 1 ) ) {
			builder.mutationFeatures( CteSupport.MutationFeature.NON_QUERY );
		}
		if ( getVersion().isSameOrAfter( 9, 5 ) ) {
			builder.mutationFeatures(
					CteSupport.MutationFeature.NON_QUERY,
					CteSupport.MutationFeature.INSERT_CONFLICT
			);
		}
		if ( getVersion().isSameOrAfter( 14 ) ) {
			builder.recursiveFeatures(
					CteSupport.RecursiveFeature.RECURSIVE,
					CteSupport.RecursiveFeature.SEARCH,
					CteSupport.RecursiveFeature.CYCLE,
					CteSupport.RecursiveFeature.CYCLE_USING
			);
		}
		return builder.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		return getVersion().isBefore( 8, 2 )
				? PostgreSQLLegacySequenceSupport.LEGACY_INSTANCE
				: PostgreSQLLegacySequenceSupport.INSTANCE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized SchemaDropSupport getSchemaDropSupport() {
		if ( schemaDropSupport == null ) {
			schemaDropSupport = new SchemaDropSupport( List.of(), ConstraintDropMode.EXPLICIT, " cascade" );
		}
		return schemaDropSupport;
	}

	private static final SequenceInformationExtractor SEQUENCE_INFORMATION_EXTRACTOR =
			SequenceInformationExtractors.builder( "select * from information_schema.sequences" ).build();

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SEQUENCE_INFORMATION_EXTRACTOR;
	}

	@Override
	public LimitHandler getLimitHandler() {
		return getVersion().isBefore( 8, 4 )
				? LimitOffsetLimitHandler.OFFSET_ONLY_INSTANCE
				: OffsetFetchLimitHandler.INSTANCE;
	}



	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendDefinition(SqlAppender appender, ColumnDefinitionRequest request) {
		if ( request.generatedExpression() == null || getVersion().isBefore( 18 ) ) {
			super.appendDefinition( appender, request );
			return;
		}
		appender.appendSql( ' ' );
		appender.appendSql( request.sqlType() );
		if ( request.renderedCollation() != null ) {
			appender.appendSql( " collate " );
			appender.appendSql( request.renderedCollation() );
		}
		if ( request.defaultExpression() != null ) {
			appender.appendSql( " default " );
			appender.appendSql( request.defaultExpression() );
		}
		appender.appendSql( " generated always as (" );
		appender.appendSql( request.generatedExpression() );
		appender.appendSql( ')' );
		if ( !request.nullable() ) {
			appender.appendSql( " not null" );
		}
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
	public String getSelectClauseNullString(SqlTypedMapping sqlType, TypeConfiguration typeConfiguration) {
		final String castTypeName = typeConfiguration.getDdlTypeRegistry()
				.getDescriptor( sqlType.getJdbcMapping().getJdbcType().getDdlTypeCode() )
				.getCastTypeName( sqlType.toSize(), (SqlExpressible) sqlType.getJdbcMapping(), typeConfiguration.getDdlTypeRegistry() );
		return "cast(null as " + castTypeName + ")";
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
	@SPI({ USE, IMPLEMENT })
	public boolean isCurrentTimestampStable() {
		return true;
	}

	@Override
	public TupleCountSupport getTupleCountSupport() {
		return TupleCountSupport.builder()
				.nonDistinctSyntax( TupleCountSupport.Syntax.PARENTHESIZED_TUPLE )
				.distinctSyntax( TupleCountSupport.Syntax.PARENTHESIZED_TUPLE )
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		appender.appendSql( bool );
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuildRequest request) {
		final var builder = request.builder();

		if ( !request.jdbcMetadata().isJdbcMetadataAccessible() ) {
			builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.LOWER );
			builder.setQuotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		}

		return super.buildIdentifierHelper( request );
	}

	@Override
	public MultiTableMutationSupport getMultiTableMutationSupport() {
		return MultiTableMutationSupport.CTE;
	}

	@Override
	public TemporaryTableStrategy getLocalTemporaryTableStrategy() {
		return StandardLocalTemporaryTableStrategy.INSTANCE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new PostgreSQLLegacySqlAstTranslator<>( request );
			}
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}

	/**
	 * Constraint-name extractor for Postgres constraint violation exceptions.
	 * Orginally contributed by Denny Bartelt.
	 */
	private static final ViolatedConstraintNameExtractor EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor( sqle -> {
				final String sqlState = JdbcExceptionHelper.extractSqlState( sqle );
				if ( sqlState != null ) {
					switch ( Integer.parseInt( sqlState ) ) {
						// CHECK VIOLATION
						case 23514:
							return extractUsingTemplate( "violates check constraint \"", "\"", sqle.getMessage() );
						// UNIQUE VIOLATION
						case 23505:
							return extractUsingTemplate( "violates unique constraint \"", "\"", sqle.getMessage() );
						// FOREIGN KEY VIOLATION
						case 23503:
							return extractUsingTemplate(
									"violates foreign key constraint \"",
									"\"",
									sqle.getMessage()
							);
						// NOT NULL VIOLATION
						case 23502:
							return extractUsingTemplate(
									"null value in column \"",
									"\" violates not-null constraint",
									sqle.getMessage()
							);
						// TODO: RESTRICT VIOLATION
						case 23001:
							return null;
					}
				}
				return null;
			} );

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
						return new PessimisticLockException( message, sqlException, sql );
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
		return CallableStatementSupports.postgresql( getVersion().isSameOrAfter( 11 ) );
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
		return PostgreSQLIdentityColumnSupport.INSTANCE;
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
		return ArraySupport.STANDARD;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalValueSemantics getTemporalValueSemantics() {
		return TemporalValueSemantics.OFFSET_LITERALS;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendFormat(SqlAppender appender, String format) {
		appender.appendSql( datetimeFormat( format ).result() );
	}

	public Replacer datetimeFormat(String format) {
		return OracleDialect.datetimeFormat( format, true, false )
				.replace("SSSSSS", "US")
				.replace("SSSSS", "US")
				.replace("SSSS", "US")
				.replace("SSS", "MS")
				.replace("SS", "MS")
				.replace("S", "MS")
				//use ISO day in week, as per DateTimeFormatter
				.replace("ee", "ID")
				.replace("e", "fmID")
				//TZR is TZ in Postgres
				.replace("zzz", "TZ")
				.replace("zz", "TZ")
				.replace("z", "TZ")
				.replace("xxx", "OF")
				.replace("xx", "OF")
				.replace("x", "OF");
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String translateExtractField(TemporalUnit unit) {
		switch ( unit ) {
			//WEEK means the ISO week number on Postgres
			case DAY_OF_MONTH: return "day";
			case DAY_OF_YEAR: return "doy";
			case DAY_OF_WEEK: return "dow";
			default: return TemporalOperationSupports.standard().translateExtractField( unit );
		}
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public AggregateSupport getAggregateSupport() {
		return new PostgreSQLDialect( getVersion() ).getAggregateSupport();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendBinaryLiteral(SqlAppender appender, byte[] bytes) {
		appender.appendSql( "bytea '\\x" );
		PrimitiveByteArrayJavaType.INSTANCE.appendString( appender, bytes );
		appender.appendSql( '\'' );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
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
	public void appendDateTimeLiteral(SqlAppender appender, Date date, TemporalType precision, TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( "date '" );
				appendAsDate( appender, date );
				appender.appendSql( '\'' );
				break;
			case TIME:
				appender.appendSql( "time with time zone '" );
				appendAsTime( appender, date, jdbcTimeZone );
				appender.appendSql( '\'' );
				break;
			case TIMESTAMP:
				appender.appendSql( "timestamp with time zone '" );
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
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( "date '" );
				appendAsDate( appender, calendar );
				appender.appendSql( '\'' );
				break;
			case TIME:
				appender.appendSql( "time with time zone '" );
				appendAsTime( appender, calendar, jdbcTimeZone );
				appender.appendSql( '\'' );
				break;
			case TIMESTAMP:
				appender.appendSql( "timestamp with time zone '" );
				appendAsTimestampWithMillis( appender, calendar, jdbcTimeZone );
				appender.appendSql( '\'' );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}















	@Override
	public SubquerySupport getSubquerySupport() {
		return SubquerySupport.builder()
				.feature( SubquerySupport.Feature.OFFSET, true )
				.feature( SubquerySupport.Feature.LATERAL, getVersion().isSameOrAfter( 9, 3 ) )
				.build();
	}

	@Override
	public WindowFunctionSupport getWindowFunctionSupport() {
		if ( getVersion().isBefore( 8, 4 ) ) {
			return WindowFunctionSupport.NONE;
		}
		final WindowFunctionSupport.Builder builder = WindowFunctionSupport.builder()
				.features(
						WindowFunctionSupport.Feature.WINDOW_FUNCTIONS,
						WindowFunctionSupport.Feature.PARTITION_BY,
						WindowFunctionSupport.Feature.ROWS_FRAME,
						WindowFunctionSupport.Feature.RANGE_FRAME
				);
		if ( getVersion().isSameOrAfter( 11 ) ) {
			builder.features(
					WindowFunctionSupport.Feature.GROUPS_FRAME,
					WindowFunctionSupport.Feature.FRAME_EXCLUSION
			);
		}
		return builder.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public FetchClauseSupport getFetchClauseSupport() {
		return getVersion().isBefore( 8, 4 )
				? FetchClauseSupport.NONE
				: getVersion().isBefore( 13 )
						? FetchClauseSupport.ROWS_ONLY
						: FetchClauseSupport.ROWS;
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
		if ( getVersion().isSameOrAfter( 9, 3 ) ) {
			tableTypesList.add( "MATERIALIZED VIEW" );

			/*
				PostgreSQL 10 and later adds support for Partition table.
			 */
			if ( getVersion().isSameOrAfter( 10 ) ) {
				tableTypesList.add( "PARTITIONED TABLE" );
			}
		}
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );
		contributePostgreSQLTypes( typeContributions, serviceRegistry );
	}

	protected void contributePostgreSQLTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration()
				.getJdbcTypeRegistry();
		// For discussion of BLOB support in Postgres, as of 8.4, have a peek at
		// <a href="http://jdbc.postgresql.org/documentation/84/binary-data.html">http://jdbc.postgresql.org/documentation/84/binary-data.html</a>.
		// For the effects in regards to Hibernate see <a href="http://in.relation.to/15492.lace">http://in.relation.to/15492.lace</a>

		// Force BLOB binding.  Otherwise, byte[] fields annotated
		// with @Lob will attempt to use
		// BlobTypeDescriptor.PRIMITIVE_ARRAY_BINDING.  Since the
		// dialect uses oid for Blobs, byte arrays cannot be used.
		jdbcTypeRegistry.addDescriptor( Types.BLOB, BlobJdbcType.BLOB_BINDING );
		jdbcTypeRegistry.addDescriptor( Types.CLOB, ClobJdbcType.CLOB_BINDING );
		// Don't use this type due to https://github.com/pgjdbc/pgjdbc/issues/2862
		//jdbcTypeRegistry.addDescriptor( TimestampUtcAsOffsetDateTimeJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( XmlJdbcType.INSTANCE );

		if ( driverKind == PostgreSQLDriverKind.PG_JDBC ) {
			if ( PostgreSQLJdbcTypes.isDriverUsable( serviceRegistry ) ) {
				jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.driverInet( serviceRegistry ) );
				jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.driverIntervalSecond( serviceRegistry ) );
				jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.driverStruct( serviceRegistry ) );
			}
			else {
				jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.castingInet() );
				jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.castingIntervalSecond() );
				jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.castingStruct() );
			}

			jdbcTypeRegistry.addDescriptor( PostgreSQLJdbcTypes.enumType() );
			jdbcTypeRegistry.addDescriptor( PostgreSQLJdbcTypes.ordinalEnumType() );
			if ( getVersion().isSameOrAfter( 8, 2 ) ) {
				// HHH-9562 / HHH-14358
				jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.uuid() );
				if ( getVersion().isSameOrAfter( 9, 2 ) ) {
					if ( getVersion().isSameOrAfter( 9, 4 ) ) {
						if ( PostgreSQLJdbcTypes.isDriverUsable( serviceRegistry ) ) {
							jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.driverJsonb( serviceRegistry ) );
							jdbcTypeRegistry.addTypeConstructorIfAbsent( PostgreSQLJdbcTypes.driverJsonbArrayConstructor( serviceRegistry ) );
						}
						else {
							jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.castingJsonb() );
							jdbcTypeRegistry.addTypeConstructorIfAbsent( PostgreSQLJdbcTypes.castingJsonbArrayConstructor() );
						}
					}
					else {
						if ( PostgreSQLJdbcTypes.isDriverUsable( serviceRegistry ) ) {
							jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.driverJson( serviceRegistry ) );
							jdbcTypeRegistry.addTypeConstructorIfAbsent( PostgreSQLJdbcTypes.driverJsonArrayConstructor( serviceRegistry ) );
						}
						else {
							jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.castingJson() );
							jdbcTypeRegistry.addTypeConstructorIfAbsent( PostgreSQLJdbcTypes.castingJsonArrayConstructor() );
						}
					}
				}
			}
		}
		else {
			jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.castingInet() );
			jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.castingIntervalSecond() );
			jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.castingStruct() );

			if ( getVersion().isSameOrAfter( 8, 2 ) ) {
				jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.uuid() );
				if ( getVersion().isSameOrAfter( 9, 2 ) ) {
					if ( getVersion().isSameOrAfter( 9, 4 ) ) {
						jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.castingJsonb() );
						jdbcTypeRegistry.addTypeConstructorIfAbsent( PostgreSQLJdbcTypes.castingJsonbArrayConstructor() );
					}
					else {
						jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.castingJson() );
						jdbcTypeRegistry.addTypeConstructorIfAbsent( PostgreSQLJdbcTypes.castingJsonArrayConstructor() );
					}
				}
			}
		}

		// PostgreSQL requires a custom binder for binding untyped nulls as VARBINARY
		typeContributions.contributeJdbcType( ObjectNullAsBinaryTypeJdbcType.INSTANCE );

		// Until we remove StandardBasicTypes, we have to keep this
		typeContributions.contributeType(
				new JavaObjectType(
						ObjectNullAsBinaryTypeJdbcType.INSTANCE,
						typeContributions.getTypeConfiguration()
								.getJavaTypeRegistry()
								.resolveDescriptor( Object.class )
				)
		);

		// Replace the standard array constructor
		jdbcTypeRegistry.addTypeConstructor( PostgreSQLJdbcTypes.arrayConstructor() );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	@Override
	public Exporter<Table> getTableExporter() {
		return postgresqlTableExporter;
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

	// disabled foreign key constraints still prevent 'truncate table'
	// (these would help if we used 'delete' instead of 'truncate')

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public RowIdSupport getRowIdSupport() {
		return RowIdSupports.fixed( "ctid", OTHER );
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
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}

	@Override
	public MutationSyntaxSupport getMutationSyntaxSupport() {
		return MutationSyntaxSupport.of( MutationKind.UPDATE, MutationSyntaxCapability.FROM_CLAUSE );
	}

	@Override
	public boolean supportsFilterClause() {
		return getVersion().isSameOrAfter( 9, 4 );
	}

	@Override
	public RowValueSupport getRowValueSupport() {
		return RowValueSupport.builder( super.getRowValueSupport() )
				.feature( RowValueSupport.Feature.ROW_CONSTRUCTOR, true )
				.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public InformationExtractor getInformationExtractor(ExtractionContext extractionContext) {
		return InformationExtractors.postgresql( extractionContext );
	}

}
