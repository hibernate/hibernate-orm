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
import static org.hibernate.SPI.Role.USE;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import org.hibernate.dialect.type.spi.StandardDdlTypes;

import org.hibernate.dialect.type.spi.TypeSizingProfile;

import org.hibernate.dialect.array.spi.ArraySupport;
import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.NullOrderingSupport;
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
import org.hibernate.PessimisticLockException;
import org.hibernate.QueryTimeoutException;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.lob.spi.LobSupport;
import org.hibernate.dialect.lob.spi.LobSupports;
import org.hibernate.dialect.sql.ast.spi.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.aggregate.spi.FunctionalDependencyAnalysisSupport;
import org.hibernate.dialect.type.spi.NationalizationSupport;
import org.hibernate.dialect.sql.ast.spi.NullOrdering;
import org.hibernate.dialect.jdbc.spi.PostgreSQLDriverKind;
import org.hibernate.dialect.SimpleDatabaseVersion;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.type.spi.TimeZoneSupport;
import org.hibernate.dialect.aggregate.spi.AggregateSupport;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.spi.TupleCountSupport;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;
import org.hibernate.dialect.function.FormatFunction;
import org.hibernate.dialect.function.PostgreSQLTruncFunction;
import org.hibernate.community.dialect.identity.internal.CockroachDBIdentityColumnSupport;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.StandardLockingSupports;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.schema.spi.AlterColumnTypeRequest;
import org.hibernate.dialect.schema.spi.ConstraintDropMode;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.schema.spi.IfExistsSupport;
import org.hibernate.dialect.schema.spi.IndexNameQualification;
import org.hibernate.dialect.schema.spi.SchemaDropSupport;
import org.hibernate.dialect.pagination.spi.OffsetFetchLimitHandler;
import org.hibernate.community.dialect.sequence.PostgreSQLLegacySequenceSupport;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.type.spi.PostgreSQLJdbcTypes;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.dialect.identifier.spi.IdentifierHelperBuildRequest;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.jdbc.spi.JdbcExceptionHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.query.SemanticException;
import org.hibernate.query.common.TemporalUnit;
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
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.descriptor.jdbc.BlobJdbcType;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.ObjectNullAsBinaryTypeJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.query.common.TemporalUnit.DAY;
import static org.hibernate.query.common.TemporalUnit.EPOCH;
import static org.hibernate.query.common.TemporalUnit.NATIVE;
import static org.hibernate.type.SqlTypes.ARRAY;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.GEOGRAPHY;
import static org.hibernate.type.SqlTypes.GEOMETRY;
import static org.hibernate.type.SqlTypes.INET;
import static org.hibernate.type.SqlTypes.INTEGER;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.LONG32NVARCHAR;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.LONG32VARCHAR;
import static org.hibernate.type.SqlTypes.NCHAR;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.NVARCHAR;
import static org.hibernate.type.SqlTypes.OTHER;
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
 * A {@linkplain Dialect SQL dialect} for CockroachDB.
 *
 * @author Gavin King
 * @author Yoobin Yoon
 */
public class CockroachLegacyDialect extends Dialect implements CurrentTemporalSupport, TemporalFormatSupport, TemporalOperationSupport {
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
			.defaultIntervalSecondScale( 6 )
			.build();

	@Override public TypeSizingProfile getTypeSizingProfile() { return typeSizingProfile; }

	// KNOWN LIMITATIONS:
	// * no support for java.sql.Clob

	// Pre-compile and reuse pattern
	private static final Pattern CRDB_VERSION_PATTERN = Pattern.compile( "v[\\d]+(\\.[\\d]+)?(\\.[\\d]+)?" );
	protected static final DatabaseVersion DEFAULT_VERSION = DatabaseVersion.make( 19, 2 );
	protected final PostgreSQLDriverKind driverKind;

	public CockroachLegacyDialect() {
		this( DEFAULT_VERSION );
	}

	public CockroachLegacyDialect(DialectResolutionInfo info) {
		this( fetchDataBaseVersion( info ), PostgreSQLDriverKind.determineKind( info ) );
	}

	public CockroachLegacyDialect(DatabaseVersion version) {
		super(version);
		driverKind = PostgreSQLDriverKind.PG_JDBC;
	}

	public CockroachLegacyDialect(DatabaseVersion version, PostgreSQLDriverKind driverKind) {
		super(version);
		this.driverKind = driverKind;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public DatabaseVersion determineDatabaseVersion(DialectResolutionInfo info) {
		return fetchDataBaseVersion( info );
	}

	protected static DatabaseVersion fetchDataBaseVersion(DialectResolutionInfo info ) {
		String versionString = null;
		if ( info.getDatabaseMetadata() != null ) {
			try (java.sql.Statement s = info.getDatabaseMetadata().getConnection().createStatement() ) {
				final ResultSet rs = s.executeQuery( "SELECT version()" );
				if ( rs.next() ) {
					versionString = rs.getString( 1 );
				}
			}
			catch (SQLException ex) {
				// Ignore
			}
		}
		return parseVersion( versionString );
	}

	protected static DatabaseVersion parseVersion(String versionString ) {
		DatabaseVersion databaseVersion = null;
		// What the DB select returns is similar to "CockroachDB CCL v21.2.10 (x86_64-unknown-linux-gnu, built 2022/05/02 17:38:58, go1.16.6)"
		Matcher m = CRDB_VERSION_PATTERN.matcher( versionString == null ? "" : versionString );
		if ( m.find() ) {
			String[] versionParts = StringHelper.split( ".", m.group().substring( 1 ) );
			// if we got to this point, there is at least a major version, so no need to check [].length > 0
			int majorVersion = Integer.parseInt( versionParts[0] );
			int minorVersion = versionParts.length > 1 ? Integer.parseInt( versionParts[1] ) : 0;
			int microVersion = versionParts.length > 2 ? Integer.parseInt( versionParts[2] ) : 0;

			databaseVersion=  new SimpleDatabaseVersion( majorVersion, minorVersion, microVersion);
		}
		if ( databaseVersion == null ) {
			// Recur to the default version of the no-args constructor
			databaseVersion = DEFAULT_VERSION;
		}
		return databaseVersion;
	}


	@Override
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		return switch ( sqlTypeCode ) {
			case TINYINT -> "smallint"; //no tinyint
			case INTEGER -> "int4";
			case NCHAR -> columnType( CHAR );
			case NVARCHAR -> columnType( VARCHAR );
			case NCLOB, CLOB -> "string";
			case BINARY, VARBINARY, BLOB -> "bytes";

			// We do not use the time with timezone type because PG deprecated it and it lacks certain operations like subtraction
//			case TIME_UTC:
//				return columnType( TIME_WITH_TIMEZONE );

			case TIMESTAMP_UTC -> columnType( TIMESTAMP_WITH_TIMEZONE );

			default -> super.columnType( sqlTypeCode );
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String castType(int sqlTypeCode) {
		return switch ( sqlTypeCode ) {
			case CHAR, NCHAR, VARCHAR, NVARCHAR, LONG32VARCHAR, LONG32NVARCHAR -> "string";
			case BINARY, VARBINARY, LONG32VARBINARY -> "bytes";
			default -> super.castType( sqlTypeCode );
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( UUID, "uuid", this ) );
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( GEOMETRY, "geometry", this ) );
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( GEOGRAPHY, "geography", this ) );
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.scale6IntervalSecond( this ) );

		// Prefer jsonb if possible
		if ( getVersion().isSameOrAfter( 20 ) ) {
			ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( INET, "inet", this ) );
			ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( JSON, "jsonb", this ) );
		}
		else {
			ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( JSON, "json", this ) );
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
			case VARCHAR:
				if ( "text".equals( columnTypeName ) && precision == Integer.MAX_VALUE ) {
					jdbcTypeCode = LONG32VARCHAR;
				}
				break;
			case OTHER:
				switch ( columnTypeName ) {
					case "uuid":
						jdbcTypeCode = UUID;
						break;
					case "json":
					case "jsonb":
						jdbcTypeCode = JSON;
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
				}
				break;
		}
		return jdbcTypeRegistry.getDescriptor( jdbcTypeCode );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean equivalentTypes(int typeCode1, int typeCode2) {
		switch ( typeCode1 ) {
			// On CockroachDB, we use the same DDL type, so treat the types as equivalent
			case LONG32VARCHAR, LONG32NVARCHAR, CLOB, NCLOB:
				switch ( typeCode2 ) {
					case LONG32VARCHAR, LONG32NVARCHAR, CLOB, NCLOB:
						return true;
				}
			default:
				return super.equivalentTypes( typeCode1, typeCode2 );
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected Integer resolveSqlTypeCode(String columnTypeName, TypeConfiguration typeConfiguration) {
		return switch ( columnTypeName ) {
			case "bool" -> Types.BOOLEAN;
			// Use REAL instead of FLOAT to get Float as recommended Java type
			case "float4" -> Types.REAL;
			case "float8" -> Types.DOUBLE;
			case "int2" -> Types.SMALLINT;
			case "int4" -> Types.INTEGER;
			case "int8" -> Types.BIGINT;
			default -> super.resolveSqlTypeCode( columnTypeName, typeConfiguration );
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );
		contributeCockroachTypes( typeContributions, serviceRegistry );
	}

	protected void contributeCockroachTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration()
				.getJdbcTypeRegistry();
		// Don't use this type due to https://github.com/pgjdbc/pgjdbc/issues/2862
		//jdbcTypeRegistry.addDescriptor( TimestampUtcAsOffsetDateTimeJdbcType.INSTANCE );
		if ( driverKind == PostgreSQLDriverKind.PG_JDBC ) {
			jdbcTypeRegistry.addDescriptor( PostgreSQLJdbcTypes.enumType() );
			jdbcTypeRegistry.addDescriptor( PostgreSQLJdbcTypes.ordinalEnumType() );
			jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.uuid() );
			if ( PostgreSQLJdbcTypes.isDriverUsable( serviceRegistry ) ) {
				jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.driverIntervalSecond( serviceRegistry ) );

				if ( getVersion().isSameOrAfter( 20, 0 ) ) {
					jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.driverInet( serviceRegistry ) );
					jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.driverJsonb( serviceRegistry ) );
					jdbcTypeRegistry.addTypeConstructorIfAbsent( PostgreSQLJdbcTypes.driverJsonbArrayConstructor( serviceRegistry ) );
				}
				else {
					jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.driverJson( serviceRegistry ) );
					jdbcTypeRegistry.addTypeConstructorIfAbsent( PostgreSQLJdbcTypes.driverJsonArrayConstructor( serviceRegistry ) );
				}
			}
			else {
				jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.castingIntervalSecond() );
				if ( getVersion().isSameOrAfter( 20, 0 ) ) {
					jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.castingInet() );
					jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.castingJsonb() );
					jdbcTypeRegistry.addTypeConstructorIfAbsent( PostgreSQLJdbcTypes.castingJsonbArrayConstructor() );
				}
				else {
					jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.castingJson() );
					jdbcTypeRegistry.addTypeConstructorIfAbsent( PostgreSQLJdbcTypes.castingJsonArrayConstructor() );
				}
			}
		}
		else {
			jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.uuid() );
			jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.castingIntervalSecond() );
			if ( getVersion().isSameOrAfter( 20, 0 ) ) {
				jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.castingInet() );
				jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.castingJsonb() );
				jdbcTypeRegistry.addTypeConstructorIfAbsent( PostgreSQLJdbcTypes.castingJsonbArrayConstructor() );
			}
			else {
				jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJdbcTypes.castingJson() );
				jdbcTypeRegistry.addTypeConstructorIfAbsent( PostgreSQLJdbcTypes.castingJsonArrayConstructor() );
			}
		}

		// Force Blob binding to byte[] for CockroachDB
		jdbcTypeRegistry.addDescriptor( Types.BLOB, BlobJdbcType.MATERIALIZED );
		jdbcTypeRegistry.addDescriptor( Types.CLOB, ClobJdbcType.MATERIALIZED );
		jdbcTypeRegistry.addDescriptor( Types.NCLOB, ClobJdbcType.MATERIALIZED );

		// The next two contributions are the same as for Postgresql
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
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		final CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);
		functionFactory.ascii();
		functionFactory.char_chr();
		functionFactory.overlay();
		functionFactory.position();
		functionFactory.substringFromFor();
		functionFactory.locate_positionSubstring();
		functionFactory.concat_pipeOperator();
		functionFactory.trim2();
		functionFactory.substr();
		functionFactory.reverse();
		functionFactory.repeat();
		functionFactory.md5();
		functionFactory.sha1();
		functionFactory.octetLength();
		functionFactory.bitLength();
		functionFactory.cbrt();
		functionFactory.cot();
		functionFactory.degrees();
		functionFactory.radians();
		functionFactory.pi();
		functionFactory.log();
		functionFactory.log10_log();
		functionFactory.round();

		functionFactory.bitandorxornot_operator();
		functionFactory.bitAndOr();
		functionFactory.everyAny_boolAndOr();
		functionFactory.median_percentileCont_castDouble();
		functionFactory.stddev();
		functionFactory.stddevPopSamp();
		functionFactory.variance();
		functionFactory.varPopSamp();
		functionFactory.covarPopSamp();
		functionFactory.corr();
		functionFactory.regrLinearRegressionAggregates();

		functionContributions.getFunctionRegistry().register(
				"format",
				new FormatFunction(
						"experimental_strftime",
						false,
						true,
						false,
						functionContributions.getTypeConfiguration()
				)
		);
		functionFactory.windowFunctions();
		functionFactory.listagg_stringAgg( "string" );
		functionFactory.inverseDistributionOrderedSetAggregates();
		functionFactory.hypotheticalOrderedSetAggregates_windowEmulation();
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
		functionFactory.arrayTrim_unnest();
		functionFactory.arrayReverse_unnest();
		functionFactory.arraySort_unnest();
		functionFactory.arrayFill_cockroachdb();
		functionFactory.arrayToString_postgresql();

		functionFactory.jsonValue_cockroachdb();
		functionFactory.jsonQuery_cockroachdb();
		functionFactory.jsonExists_cockroachdb();
		functionFactory.jsonObject_postgresql( false );
		functionFactory.jsonArray_postgresql( false );
		functionFactory.jsonArrayAgg_postgresql( false );
		functionFactory.jsonObjectAgg_postgresql( false );
		functionFactory.jsonSet_postgresql();
		functionFactory.jsonRemove_cockroachdb();
		functionFactory.jsonReplace_postgresql();
		functionFactory.jsonInsert_postgresql();
		// No support for WITH clause in subquery: https://github.com/cockroachdb/cockroach/issues/131011
//		functionFactory.jsonMergepatch_postgresql();
		functionFactory.jsonArrayAppend_postgresql( false );
		functionFactory.jsonArrayInsert_postgresql();

		functionFactory.unnest_postgresql( false );
		functionFactory.generateSeries( null, "ordinality", true );
		functionFactory.jsonTable_cockroachdb();

		// Postgres uses # instead of ^ for XOR
		functionContributions.getFunctionRegistry().patternDescriptorBuilder( "bitxor", "(?1#?2)" )
				.setExactArgumentCount( 2 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();

		functionContributions.getFunctionRegistry().register(
				"trunc",
				new PostgreSQLTruncFunction(
						true,
						getVersion().isSameOrAfter( 22, 2 ),
						functionContributions.getTypeConfiguration()
				)
		);
		functionContributions.getFunctionRegistry().registerAlternateKey( "truncate", "trunc" );
		functionFactory.regexpLike_postgresql( false );
	}

	@Override
	public @Nullable String getDefaultOrdinalityColumnName() {
		return "ordinality";
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TimeZoneSupport getTimeZoneSupport() {
		return TimeZoneSupport.NORMALIZE;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		appender.appendSql( bool );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized SchemaDropSupport getSchemaDropSupport() {
		if ( schemaDropSupport == null ) {
			schemaDropSupport = new SchemaDropSupport( List.of(), ConstraintDropMode.EXPLICIT, " cascade" );
		}
		return schemaDropSupport;
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
				ExistenceCheckPlacement.BEFORE_NAME,
				ExistenceCheckPlacement.BEFORE_NAME,
				ExistenceCheckPlacement.BEFORE_NAME,
				ExistenceCheckPlacement.NONE
		);
		}
		return ifExistsSupport;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public IndexNameQualification nameQualification() {
		return IndexNameQualification.UNQUALIFIED;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return CockroachDBIdentityColumnSupport.INSTANCE;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String alterColumnType(AlterColumnTypeRequest request) {
		// would need multiple statements to 'set not null'/'drop not null', 'set default'/'drop default', 'set generated', etc
		return "alter column " + request.columnName() + " set data type " + request.columnType();
	}

	@Override
	public ValuesListSupport getValuesListSupport() {
		return ValuesListSupport.STANDARD;
	}

	@Override
	public CteSupport getCteSupport() {
		return CteSupport.builder()
				.recursiveFeature(
						CteSupport.RecursiveFeature.RECURSIVE,
						getVersion().isSameOrAfter( 20, 1 )
				)
				.mutationFeatures(
						CteSupport.MutationFeature.NON_QUERY,
						CteSupport.MutationFeature.INSERT_CONFLICT
				)
				.build();
	}

	@Override
	public NullOrderingSupport getNullOrderingSupport() {
		// Not yet implemented: https://www.cockroachlabs.com/docs/v20.2/null-handling.html#nulls-and-sorting
		return NullOrderingSupport.builder( super.getNullOrderingSupport() )
				.defaultOrdering( NullOrdering.SMALLEST )
				.capability( NullOrderingSupport.Capability.NULLS_FIRST_LAST, false )
				.build();
	}

	@Override
	public TupleCountSupport getTupleCountSupport() {
		return TupleCountSupport.builder()
				.nonDistinctSyntax( TupleCountSupport.Syntax.PARENTHESIZED_TUPLE )
				.distinctSyntax( TupleCountSupport.Syntax.PARENTHESIZED_TUPLE )
				.build();
	}

	@Override
	public GenerationType getNativeValueGenerationStrategy() {
		return GenerationType.SEQUENCE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		return PostgreSQLLegacySequenceSupport.INSTANCE;
	}

	private static final SequenceInformationExtractor SEQUENCE_INFORMATION_EXTRACTOR =
			SequenceInformationExtractors.builder(
					"select sequence_name,sequence_schema,sequence_catalog,start_value,minimum_value,maximum_value,increment from information_schema.sequences"
			)
			.build();

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SEQUENCE_INFORMATION_EXTRACTOR;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new CockroachLegacySqlAstTranslator<>( request );
			}
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NationalizationSupport getNationalizationSupport() {
		// TEXT / STRING inherently support nationalized data
		return NationalizationSupport.IMPLICIT;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public AggregateSupport getAggregateSupport() {
		return new CockroachDialect( getVersion() ).getAggregateSupport();
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
				appendAsTimestampWithMicros( appender,date, jdbcTimeZone );
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

	/**
	 * The {@code extract()} function returns {@link TemporalUnit#DAY_OF_WEEK}
	 * numbered from 0 to 6. This isn't consistent with what most other
	 * databases do, so here we adjust the result by generating
	 * {@code (extract(dayofweek,arg)+1))}.
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
	public String translateExtractField(TemporalUnit unit) {
		switch ( unit ) {
			case DAY_OF_MONTH: return "day";
			case DAY_OF_YEAR: return "dayofyear";
			case DAY_OF_WEEK: return "dayofweek";
			default: return TemporalOperationSupports.standard().translateExtractField( unit );
		}
	}

	/**
	 * {@code microsecond} is the smallest unit for an {@code interval},
	 * and the highest precision for a {@code timestamp}.
	 */
	@Override
	@SPI({ USE, IMPLEMENT })
	public long fractionalSecondPrecisionInNanos() {
		return 1_000; //microseconds
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		return intervalType != null
				? "(?2+?3)"
				: "cast(?3+" + intervalPattern( unit ) + " as " + temporalType.name().toLowerCase() + ")";
	}

	private static String intervalPattern(TemporalUnit unit) {
		switch (unit) {
			case NATIVE:
				return "(?2)*interval '1 microsecond'";
			case NANOSECOND:
				return "(?2)/1e3*interval '1 microsecond'";
			case QUARTER: //quarter is not supported in interval literals
				return "(?2)*interval '3 month'";
			case WEEK: //week is not supported in interval literals
				return "(?2)*interval '7 day'";
			default:
				return "(?2)*interval '1 " + unit + "'";
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
					// age only supports timestamptz, so we have to cast the date expressions
					return "extract(" + translateDurationField( unit ) + " from age(cast(?3 as timestamptz),cast(?2 as timestamptz)))";
				default:
					return "(?3-?2)" + DAY.conversionFactor( unit, this );
			}
		}
		else {
			if (getVersion().isSameOrAfter( 20, 1 )) {
				switch (unit) {
					case YEAR:
						return "extract(year from ?3-?2)";
					case QUARTER:
						return "(extract(year from ?3-?2)*4+extract(month from ?3-?2)//3)";
					case MONTH:
						return "(extract(year from ?3-?2)*12+extract(month from ?3-?2))";
					case WEEK: //week is not supported by extract() when the argument is a duration
						return "(extract(day from ?3-?2)/7)";
					case DAY:
						return "extract(day from ?3-?2)";
					//in order to avoid multiple calls to extract(),
					//we use extract(epoch from x - y) * factor for
					//all the following units:

					// Note that CockroachDB also has an extract_duration function which returns an int,
					// but we don't use that here because it is deprecated since v20.
					// We need to use round() instead of cast(... as int) because extract epoch returns
					// float8 which can cause loss-of-precision in some cases
					// https://github.com/cockroachdb/cockroach/issues/72523
					case HOUR:
					case MINUTE:
					case SECOND:
					case NANOSECOND:
					case NATIVE:
						return "round(extract(epoch from ?3-?2)" + EPOCH.conversionFactor( unit, this ) + ")::int";
					default:
						throw new SemanticException( "unrecognized field: " + unit );
				}
			}
			else {
				switch (unit) {
					case YEAR:
						return "extract(year from ?3-?2)";
					case QUARTER:
						return "(extract(year from ?3-?2)*4+extract(month from ?3-?2)//3)";
					case MONTH:
						return "(extract(year from ?3-?2)*12+extract(month from ?3-?2))";
					// Prior to v20, Cockroach didn't support extracting from an interval/duration,
					// so we use the extract_duration function
					case WEEK:
						return "extract_duration(hour from ?3-?2)/168";
					case DAY:
						return "extract_duration(hour from ?3-?2)/24";
					case NANOSECOND:
						return "extract_duration(microsecond from ?3-?2)*1e3";
					default:
						return "extract_duration(?1 from ?3-?2)";
				}
			}
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String translateDurationField(TemporalUnit unit) {
		return unit==NATIVE
				? "microsecond"
				: TemporalOperationSupports.standard().translateDurationField(unit);
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendFormat(SqlAppender appender, String format) {
		appender.appendSql( SpannerDialect.datetimeFormat( format ).result() );
	}

	@Override
	public LimitHandler getLimitHandler() {
		return OffsetFetchLimitHandler.INSTANCE;
	}



















	@Override
	public LockingSupport getLockingSupport() {
		return StandardLockingSupports.cockroach( getVersion() );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public LobSupport getLobSupport() {
		return LobSupports.nonStreaming();
	}

	@Override
	public SubquerySupport getSubquerySupport() {
		return SubquerySupport.builder()
				.feature( SubquerySupport.Feature.OFFSET, true )
				.feature( SubquerySupport.Feature.LATERAL, getVersion().isSameOrAfter( 20, 1 ) )
				.build();
	}

	@Override
	public WindowFunctionSupport getWindowFunctionSupport() {
		return WindowFunctionSupport.builder()
				.features( WindowFunctionSupport.Feature.values() )
				.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public FunctionalDependencyAnalysisSupport getFunctionalDependencyAnalysisSupport() {
		return FunctionalDependencyAnalysisSupport.TABLE_REFERENCE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NameQualifierSupport getNameQualifierSupport() {
		// This method is overridden so the correct value will be returned when
		// DatabaseMetaData is not available.
		return NameQualifierSupport.SCHEMA;
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
	@SPI({ IMPLEMENT, SUPPLY })
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}

	/**
	 * Constraint-name extractor for Postgres constraint violation exceptions.
	 * Originally contributed by Denny Bartelt.
	 */
	private static final ViolatedConstraintNameExtractor EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor( sqle -> {
				final String sqlState = JdbcExceptionHelper.extractSqlState( sqle );
				if ( sqlState == null ) {
					return null;
				}
				switch ( Integer.parseInt( sqlState ) ) {
					// CHECK VIOLATION
					case 23514:
						return extractUsingTemplate( "violates check constraint \"","\"", sqle.getMessage() );
					// UNIQUE VIOLATION
					case 23505:
						return extractUsingTemplate( "violates unique constraint \"","\"", sqle.getMessage() );
					// FOREIGN KEY VIOLATION
					case 23503:
						return extractUsingTemplate( "violates foreign key constraint \"","\"", sqle.getMessage() );
					// NOT NULL VIOLATION
					case 23502:
						return extractUsingTemplate( "null value in column \"","\" violates not-null constraint", sqle.getMessage() );
					// TODO: RESTRICT VIOLATION
					case 23001:
						return null;
					// ALL OTHER
					default:
						return null;
				}
			} );

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			final String sqlState = JdbcExceptionHelper.extractSqlState( sqlException );
			if ( sqlState == null ) {
				return null;
			}
			return switch ( sqlState ) {
				// DEADLOCK DETECTED
				case "40P01" -> new LockAcquisitionException( message, sqlException, sql);
				// LOCK NOT AVAILABLE
				case "55P03" -> new PessimisticLockException( message, sqlException, sql);
				case "57014" -> new QueryTimeoutException( message, sqlException, sql );
				// returning null allows other delegates to operate
				default -> null;
			};
		};
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
	public RowValueSupport getRowValueSupport() {
		return RowValueSupport.builder( super.getRowValueSupport() )
				.feature( RowValueSupport.Feature.ROW_CONSTRUCTOR, true )
				.feature( RowValueSupport.Feature.QUANTIFIED_COMPARISON, false )
				.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public InformationExtractor getInformationExtractor(ExtractionContext extractionContext) {
		return InformationExtractors.postgresql( extractionContext );
	}
}
