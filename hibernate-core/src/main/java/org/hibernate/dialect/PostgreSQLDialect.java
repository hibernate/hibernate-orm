/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

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
import java.util.Map;
import java.util.TimeZone;

import org.hibernate.Length;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.PessimisticLockException;
import org.hibernate.QueryTimeoutException;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.dialect.aggregate.PostgreSQLAggregateSupport;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.PostgreSQLMinMaxFunction;
import org.hibernate.dialect.function.PostgreSQLTruncFunction;
import org.hibernate.dialect.function.PostgreSQLTruncRoundFunction;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.PostgreSQLIdentityColumnSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.OffsetFetchLimitHandler;
import org.hibernate.dialect.sequence.PostgreSQLSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.unique.CreateTableUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.procedure.internal.PostgreSQLCallableStatementSupport;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.query.SemanticException;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.FetchClauseType;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.sqm.TemporalUnit;
import org.hibernate.query.sqm.mutation.internal.cte.CteInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.cte.CteMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
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
import org.hibernate.sql.model.jdbc.OptionalTableUpdateOperation;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.BlobJdbcType;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.ObjectNullAsBinaryTypeJdbcType;
import org.hibernate.type.descriptor.jdbc.SqlTypedJdbcType;
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

import jakarta.persistence.TemporalType;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.query.sqm.TemporalUnit.DAY;
import static org.hibernate.query.sqm.TemporalUnit.EPOCH;
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
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsDate;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsLocalTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithMicros;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithMillis;

/**
 * A {@linkplain Dialect SQL dialect} for PostgreSQL 11 and above.
 *
 * @author Gavin King
 */
public class PostgreSQLDialect extends Dialect {
	protected final static DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 12 );

	private final UniqueDelegate uniqueDelegate = new CreateTableUniqueDelegate(this);

	protected final PostgreSQLDriverKind driverKind;
	private final OptionalTableUpdateStrategy optionalTableUpdateStrategy;
	private final ParameterMarkerStrategy parameterRenderer;

	public PostgreSQLDialect() {
		this( MINIMUM_VERSION );
	}

	public PostgreSQLDialect(DialectResolutionInfo info) {
		this( info.makeCopyOrDefault( MINIMUM_VERSION ), PostgreSQLDriverKind.determineKind( info ) );
		registerKeywords( info );
	}

	public PostgreSQLDialect(DatabaseVersion version) {
		this( version, PostgreSQLDriverKind.PG_JDBC );
	}

	public PostgreSQLDialect(DatabaseVersion version, PostgreSQLDriverKind driverKind) {
		super( version );

		this.driverKind = driverKind;
		this.optionalTableUpdateStrategy = determineOptionalTableUpdateStrategy( version );
		this.parameterRenderer = driverKind == PostgreSQLDriverKind.VERT_X
				? NativeParameterMarkers.INSTANCE
				: super.getNativeParameterMarkerStrategy();
	}

	private static OptionalTableUpdateStrategy determineOptionalTableUpdateStrategy(DatabaseVersion version) {
		return version.isSameOrAfter( DatabaseVersion.make( 15, 0 ) )
				? PostgreSQLDialect::usingMerge
				: PostgreSQLDialect::withoutMerge;
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
		switch ( sqlTypeCode ) {
			case TINYINT:
				// no tinyint, not even in Postgres 11
				return "smallint";

			// there are no nchar/nvarchar types in Postgres
			case NCHAR:
				return columnType( CHAR );
			case NVARCHAR:
				return columnType( VARCHAR );

			// since there's no real difference between TEXT and VARCHAR,
			// except for the length limit, we can just use 'text' for the
			// "long" string types
			case LONG32VARCHAR:
			case LONG32NVARCHAR:
				return "text";

			case BLOB:
			case CLOB:
			case NCLOB:
				// use oid as the blob/clob type on Postgres because
				// the JDBC driver doesn't allow using bytea/text via
				// LOB APIs
				return "oid";

			// use bytea as the "long" binary type (that there is no
			// real VARBINARY type in Postgres, so we always use this)
			case BINARY:
			case VARBINARY:
			case LONG32VARBINARY:
				return "bytea";

			// We do not use the time with timezone type because PG deprecated it and it lacks certain operations like subtraction
//			case TIME_UTC:
//				return columnType( TIME_WITH_TIMEZONE );

			case TIMESTAMP_UTC:
				return columnType( TIMESTAMP_WITH_TIMEZONE );

			default:
				return super.columnType( sqlTypeCode );
		}
	}

	@Override
	protected String castType(int sqlTypeCode) {
		switch ( sqlTypeCode ) {
			case CHAR:
			case NCHAR:
			case VARCHAR:
			case NVARCHAR:
			case LONG32VARCHAR:
			case LONG32NVARCHAR:
				return "text";
			case BINARY:
			case VARBINARY:
			case LONG32VARBINARY:
				return "bytea";
		}
		return super.castType( sqlTypeCode );
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

		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( SQLXML, "xml", this ) );
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( UUID, "uuid", this ) );
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( INET, "inet", this ) );
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( GEOMETRY, "geometry", this ) );
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( GEOGRAPHY, "geography", this ) );
		ddlTypeRegistry.addDescriptor( new Scale6IntervalSecondDdlType( this ) );

		// Prefer jsonb if possible
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( JSON, "jsonb", this ) );

		ddlTypeRegistry.addDescriptor( new NamedNativeEnumDdlTypeImpl( this ) );
		ddlTypeRegistry.addDescriptor( new NamedNativeOrdinalEnumDdlTypeImpl( this ) );
	}

	@Override
	public int getMaxVarcharLength() {
		return 10_485_760;
	}

	@Override
	public int getMaxVarcharCapacity() {
		// 1GB according to PostgreSQL docs
		return 1_073_741_824;
	}

	@Override
	public int getMaxVarbinaryLength() {
		//postgres has no varbinary-like type
		return Length.LONG32;
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
	public String currentTime() {
		return "localtime";
	}

	@Override
	public String currentTimestamp() {
		return "localtimestamp";
	}

	@Override
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
	public String extractPattern(TemporalUnit unit) {
		switch ( unit ) {
			case DAY_OF_WEEK:
				return "(" + super.extractPattern(unit) + "+1)";
			default:
				return super.extractPattern(unit);
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
	public long getFractionalSecondPrecisionInNanos() {
		return 1_000_000_000; //seconds
	}

	@Override
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		return intervalType != null
				? "(?2+?3)"
				: "cast(?3+" + intervalPattern( unit ) + " as " + temporalType.name().toLowerCase() + ")";
	}

	private static String intervalPattern(TemporalUnit unit) {
		switch (unit) {
			case NANOSECOND:
				return "(?2)/1e3*interval '1 microsecond'";
			case NATIVE:
				return "(?2)*interval '1 second'";
			case QUARTER: //quarter is not supported in interval literals
				return "(?2)*interval '3 month'";
			case WEEK: //week is not supported in interval literals
				return "(?2)*interval '7 day'";
			default:
				return "(?2)*interval '1 " + unit + "'";
		}
	}

	@Override
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
					throw new SemanticException( "Unrecognized field: " + unit );
			}
		}
	}

	/**
	 * @deprecated No longer used, and should be removed
	 */
	@Deprecated(forRemoval = true)
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
	public TimeZoneSupport getTimeZoneSupport() {
		return TimeZoneSupport.NORMALIZE;
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);

		functionFactory.cot();
		functionFactory.radians();
		functionFactory.degrees();
		functionFactory.log();
		functionFactory.mod_operator();
		functionFactory.log10();
		functionFactory.tanh();
		functionFactory.sinh();
		functionFactory.cosh();
		functionFactory.moreHyperbolic();
		functionFactory.cbrt();
		functionFactory.pi();
		functionFactory.trim2();
		functionFactory.repeat();
		functionFactory.md5();
		functionFactory.initcap();
		functionFactory.substr();
		functionFactory.substring_substr();
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
		functionFactory.arrayFill_postgresql();
		functionFactory.arrayToString_postgresql();

		functionFactory.makeDateTimeTimestamp();
		// Note that PostgreSQL doesn't support the OVER clause for ordered set-aggregate functions
		functionFactory.inverseDistributionOrderedSetAggregates();
		functionFactory.hypotheticalOrderedSetAggregates();

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
	}

	/**
	 * Whether PostgreSQL supports {@code min(uuid)}/{@code max(uuid)},
	 * which it doesn't by default. Since the emulation does not perform well,
	 * this method may be overridden by any user who ensures that aggregate
	 * functions for handling uuids exist in the database.
	 * <p>
	 * The following definitions can be used for this purpose:
	 * <code><pre>
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
	 * </pre></code>
	 */
	protected boolean supportsMinMaxOnUuid() {
		return false;
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
		return true;
	}

	@Override
	public String getBeforeDropStatement() {
		// by default, the Postgres driver reports
		// NOTICE: table "nonexistent" does not exist, skipping
		// as a JDBC SQLWarning
		return "set client_min_messages = WARNING";
	}

	@Override
	public String getAlterColumnTypeString(String columnName, String columnType, String columnDefinition) {
		// would need multiple statements to 'set not null'/'drop not null', 'set default'/'drop default', 'set generated', etc
		return "alter column " + columnName + " set data type " + columnType;
	}

	@Override
	public boolean supportsAlterColumnType() {
		return true;
	}

	@Override
	public boolean supportsValuesList() {
		return true;
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
		return PostgreSQLSequenceSupport.INSTANCE;
	}

	@Override
	public String getCascadeConstraintsString() {
		return " cascade";
	}

	@Override
	public String getQuerySequencesString() {
		return "select * from information_schema.sequences";
	}

	@Override
	public LimitHandler getLimitHandler() {
		return OffsetFetchLimitHandler.INSTANCE;
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
		if (lockMode == null ) {
			lockMode = lockOptions.getLockMode();
		}
		switch ( lockMode ) {
			case PESSIMISTIC_READ: {
				return getReadLockString( aliases, lockOptions.getTimeOut() );
			}
			case PESSIMISTIC_WRITE: {
				return getWriteLockString( aliases, lockOptions.getTimeOut() );
			}
			case UPGRADE_NOWAIT:
			case PESSIMISTIC_FORCE_INCREMENT: {
				return getForUpdateNowaitString(aliases);
			}
			case UPGRADE_SKIPLOCKED: {
				return getForUpdateSkipLockedString(aliases);
			}
			default: {
				return "";
			}
		}
	}

	@Override
	public String getNoColumnsInsertString() {
		return "default values";
	}

	@Override
	public String getCaseInsensitiveLike(){
		return "ilike";
	}

	@Override
	public boolean supportsCaseInsensitiveLike() {
		return true;
	}

	@Override
	public String getNativeIdentifierGeneratorStrategy() {
		return "sequence";
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
		// Workaround for postgres bug #1453
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

	@Override
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuilder builder, DatabaseMetaData dbMetaData)
			throws SQLException {

		if ( dbMetaData == null ) {
			builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.LOWER );
			builder.setQuotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		}

		return super.buildIdentifierHelper( builder, dbMetaData );
	}

	@Override
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new CteMutationStrategy( rootEntityDescriptor, runtimeModelCreationContext );
	}

	@Override
	public SqmMultiTableInsertStrategy getFallbackSqmInsertStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new CteInsertStrategy( rootEntityDescriptor, runtimeModelCreationContext );
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new PostgreSQLSqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	@Override
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
							return extractUsingTemplate( "violates foreign key constraint \"", "\"", sqle.getMessage() );
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

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		// Register the type of the out param - PostgreSQL uses Types.OTHER
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
		return PostgreSQLCallableStatementSupport.INSTANCE;
	}

	@Override
	public ResultSet getResultSet(CallableStatement statement, int position) throws SQLException {
		if ( position != 1 ) {
			throw new UnsupportedOperationException( "PostgreSQL only supports REF_CURSOR parameters as the first parameter" );
		}
		return (ResultSet) statement.getObject( 1 );
	}

	@Override
	public ResultSet getResultSet(CallableStatement statement, String name) throws SQLException {
		throw new UnsupportedOperationException( "PostgreSQL only supports accessing REF_CURSOR parameters by position" );
	}

	@Override
	public boolean qualifyIndexName() {
		return false;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return PostgreSQLIdentityColumnSupport.INSTANCE;
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
		return true;
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
		return true;
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
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
	public String translateExtractField(TemporalUnit unit) {
		switch ( unit ) {
			//WEEK means the ISO week number on Postgres
			case DAY_OF_MONTH: return "day";
			case DAY_OF_YEAR: return "doy";
			case DAY_OF_WEEK: return "dow";
			default: return super.translateExtractField( unit );
		}
	}

	@Override
	public AggregateSupport getAggregateSupport() {
		return PostgreSQLAggregateSupport.valueOf( this );
	}

	@Override
	public void appendBinaryLiteral(SqlAppender appender, byte[] bytes) {
		appender.appendSql( "bytea '\\x" );
		PrimitiveByteArrayJavaType.INSTANCE.appendString( appender, bytes );
		appender.appendSql( '\'' );
	}

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

	private String withTimeout(String lockString, int timeout) {
		switch (timeout) {
			case LockOptions.NO_WAIT:
				return supportsNoWait() ? lockString + " nowait" : lockString;
			case LockOptions.SKIP_LOCKED:
				return supportsSkipLocked() ? lockString + " skip locked" : lockString;
			default:
				return lockString;
		}
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
		return true;
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
		return true;
	}

	@Override
	public boolean supportsRecursiveCTE() {
		return true;
	}

	@Override
	public boolean supportsFetchClause(FetchClauseType type) {
		switch ( type ) {
			case ROWS_ONLY:
				return true;
			case PERCENT_ONLY:
			case PERCENT_WITH_TIES:
				return false;
			case ROWS_WITH_TIES:
				return getVersion().isSameOrAfter( 13 );
		}
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

		//PostgreSQL 10 and later adds support for Partition table.
		tableTypesList.add( "PARTITIONED TABLE" );
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes(typeContributions, serviceRegistry);
		contributePostgreSQLTypes(typeContributions, serviceRegistry);
	}

	/**
	 * Allow for extension points to override this only
	 */
	protected void contributePostgreSQLTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration()
				.getJdbcTypeRegistry();
		// For discussion of BLOB support in Postgres, as of 8.4, see:
		//     http://jdbc.postgresql.org/documentation/84/binary-data.html
		// For how this affects Hibernate, see:
		//     http://in.relation.to/15492.lace

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
			if ( PgJdbcHelper.isUsable( serviceRegistry ) ) {
				jdbcTypeRegistry.addDescriptorIfAbsent( PgJdbcHelper.getInetJdbcType( serviceRegistry ) );
				jdbcTypeRegistry.addDescriptorIfAbsent( PgJdbcHelper.getIntervalJdbcType( serviceRegistry ) );
				jdbcTypeRegistry.addDescriptorIfAbsent( PgJdbcHelper.getStructJdbcType( serviceRegistry ) );
				jdbcTypeRegistry.addDescriptorIfAbsent( PgJdbcHelper.getJsonbJdbcType( serviceRegistry ) );
			}
			else {
				jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLCastingInetJdbcType.INSTANCE );
				jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLCastingIntervalSecondJdbcType.INSTANCE );
				jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLStructCastingJdbcType.INSTANCE );
				jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLCastingJsonJdbcType.JSONB_INSTANCE );
			}
		}
		else {
			jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLCastingInetJdbcType.INSTANCE );
			jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLCastingIntervalSecondJdbcType.INSTANCE );
			jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLStructCastingJdbcType.INSTANCE );
			jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLCastingJsonJdbcType.JSONB_INSTANCE );
		}

		// PostgreSQL requires a custom binder for binding untyped nulls as VARBINARY
		typeContributions.contributeJdbcType( ObjectNullAsBinaryTypeJdbcType.INSTANCE );

		// Until we remove StandardBasicTypes, we have to keep this
		typeContributions.contributeType(
				new JavaObjectType(
						ObjectNullAsBinaryTypeJdbcType.INSTANCE,
						typeContributions.getTypeConfiguration()
								.getJavaTypeRegistry()
								.getDescriptor( Object.class )
				)
		);

		jdbcTypeRegistry.addDescriptor( PostgreSQLEnumJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( PostgreSQLOrdinalEnumJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( PostgreSQLUUIDJdbcType.INSTANCE );

		jdbcTypeRegistry.addTypeConstructor( PostgreSQLArrayJdbcTypeConstructor.INSTANCE );
	}

	@Override
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	/**
	 * @return {@code true}, but only because we can "batch" truncate
	 */
	@Override
	public boolean canBatchTruncate() {
		return true;
	}

	// disabled foreign key constraints still prevent 'truncate table'
	// (these would help if we used 'delete' instead of 'truncate')

	@Override
	public String rowId(String rowId) {
		return "ctid";
	}

	@Override
	public int rowIdSqlType() {
		return OTHER;
	}

	@Override
	public String getQueryHintString(String sql, String hints) {
		return "/*+ " + hints + " */ " + sql;
	}

	@Override
	public String addSqlHintOrComment(String sql, QueryOptions queryOptions, boolean commentsEnabled) {
		// PostgreSQL's extension pg_hint_plan needs the hint to be the first comment
		if ( commentsEnabled && queryOptions.getComment() != null ) {
			sql = prependComment( sql, queryOptions.getComment() );
		}
		if ( queryOptions.getDatabaseHints() != null && queryOptions.getDatabaseHints().size() > 0 ) {
			sql = getQueryHintString( sql, queryOptions.getDatabaseHints() );
		}
		return sql;
	}

	@FunctionalInterface
	private interface OptionalTableUpdateStrategy {
		MutationOperation buildMutationOperation(
				EntityMutationTarget mutationTarget,
				OptionalTableUpdate optionalTableUpdate,
				SessionFactoryImplementor factory);
	}

	@Override
	public MutationOperation createOptionalTableUpdateOperation(
			EntityMutationTarget mutationTarget,
			OptionalTableUpdate optionalTableUpdate,
			SessionFactoryImplementor factory) {
		return optionalTableUpdateStrategy.buildMutationOperation( mutationTarget, optionalTableUpdate, factory );
	}

	private static MutationOperation usingMerge(
			EntityMutationTarget mutationTarget,
			OptionalTableUpdate optionalTableUpdate,
			SessionFactoryImplementor factory) {
		final PostgreSQLSqlAstTranslator<?> translator = new PostgreSQLSqlAstTranslator<>( factory, optionalTableUpdate );
		return translator.createMergeOperation( optionalTableUpdate );
	}

	private static MutationOperation withoutMerge(
			EntityMutationTarget mutationTarget,
			OptionalTableUpdate optionalTableUpdate,
			SessionFactoryImplementor factory) {
		return new OptionalTableUpdateOperation( mutationTarget, optionalTableUpdate, factory );
	}

	@Override
	public ParameterMarkerStrategy getNativeParameterMarkerStrategy() {
		return parameterRenderer;
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
}
