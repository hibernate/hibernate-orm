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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import jakarta.persistence.TemporalType;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.PessimisticLockException;
import org.hibernate.QueryTimeoutException;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.PostgreSQLMinMaxFunction;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.PostgreSQLIdentityColumnSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitOffsetLimitHandler;
import org.hibernate.dialect.pagination.OffsetFetchLimitHandler;
import org.hibernate.dialect.sequence.PostgreSQLSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
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
import org.hibernate.procedure.internal.PostgresCallableStatementSupport;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.query.SemanticException;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.FetchClauseType;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.sqm.TemporalUnit;
import org.hibernate.query.sqm.mutation.internal.cte.CteInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.cte.CteMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.BlobJdbcType;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;
import org.hibernate.type.descriptor.jdbc.InstantAsTimestampWithTimeZoneJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.ObjectNullAsBinaryTypeJdbcType;
import org.hibernate.type.descriptor.jdbc.UUIDJdbcType;
import org.hibernate.type.descriptor.jdbc.XmlJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.internal.CapacityDependentDdlType;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.internal.Scale6IntervalSecondDdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.query.sqm.TemporalUnit.DAY;
import static org.hibernate.query.sqm.TemporalUnit.EPOCH;
import static org.hibernate.query.sqm.TemporalUnit.MONTH;
import static org.hibernate.query.sqm.TemporalUnit.QUARTER;
import static org.hibernate.query.sqm.TemporalUnit.YEAR;
import static org.hibernate.type.SqlTypes.ARRAY;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.GEOGRAPHY;
import static org.hibernate.type.SqlTypes.FLOAT;
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
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_UTC;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.UUID;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.SqlTypes.VARCHAR;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsDate;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsLocalTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithMicros;

/**
 * A {@linkplain Dialect SQL dialect} for PostgreSQL 8 and above.
 *
 * @author Gavin King
 */
public class PostgreSQLDialect extends Dialect {

	private static final PostgreSQLIdentityColumnSupport IDENTITY_COLUMN_SUPPORT = new PostgreSQLIdentityColumnSupport();

	private final PostgreSQLDriverKind driverKind;

	public PostgreSQLDialect() {
		this( DatabaseVersion.make( 8, 0 ) );
	}

	public PostgreSQLDialect(DialectResolutionInfo info) {
		super(info);
		driverKind = PostgreSQLDriverKind.determineKind( info );
	}

	public PostgreSQLDialect(DatabaseVersion version) {
		super(version);
		driverKind = PostgreSQLDriverKind.PG_JDBC;
	}

	public PostgreSQLDialect(DatabaseVersion version, PostgreSQLDriverKind driverKind) {
		super(version);
		this.driverKind = driverKind;
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
				// the JDBC driver doesn't allow using bytea/text through LOB APIs
				return "oid";
			// use bytea as the "long" binary type (that there is no
			// real VARBINARY type in Postgres, so we always use this)
			case BINARY:
			case VARBINARY:
			case LONG32VARBINARY:
				return "bytea";

			case TIMESTAMP_UTC:
				return columnType( TIMESTAMP_WITH_TIMEZONE );
		}
		return super.columnType( sqlTypeCode );
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
		if ( getVersion().isSameOrAfter( 8, 2 ) ) {
			ddlTypeRegistry.addDescriptor( new DdlTypeImpl( UUID, "uuid", this ) );
		}
		if ( PostgreSQLPGObjectJdbcType.isUsable() ) {
			// The following DDL types require that the PGobject class is usable/visible
			ddlTypeRegistry.addDescriptor( new DdlTypeImpl( INET, "inet", this ) );
			ddlTypeRegistry.addDescriptor( new DdlTypeImpl( GEOMETRY, "geometry", this ) );
			ddlTypeRegistry.addDescriptor( new DdlTypeImpl( GEOGRAPHY, "geography", this ) );
			ddlTypeRegistry.addDescriptor( new Scale6IntervalSecondDdlType( this ) );

			if ( getVersion().isSameOrAfter( 9, 2 ) ) {
				// Prefer jsonb if possible
				if ( getVersion().isSameOrAfter( 9, 4 ) ) {
					ddlTypeRegistry.addDescriptor( new DdlTypeImpl( JSON, "jsonb", this ) );
				}
				else {
					ddlTypeRegistry.addDescriptor( new DdlTypeImpl( JSON, "json", this ) );
				}
			}
		}
	}

	@Override
	public int getMaxVarcharLength() {
		return 10_485_760;
	}

	@Override
	public int getMaxVarbinaryLength() {
		//postgres has no varbinary-like type
		return Integer.MAX_VALUE;
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
			case TIMESTAMP:
				// The PostgreSQL JDBC driver reports TIMESTAMP for timestamptz, but we use it only for mapping Instant
				if ( "timestamptz".equals( columnTypeName ) ) {
					jdbcTypeCode = TIMESTAMP_UTC;
				}
				break;
			case ARRAY:
				final JdbcType jdbcType = jdbcTypeRegistry.getDescriptor( jdbcTypeCode );
				// PostgreSQL names array types by prepending an underscore to the base name
				if ( jdbcType instanceof ArrayJdbcType && columnTypeName.charAt( 0 ) == '_' ) {
					final String componentTypeName = columnTypeName.substring( 1 );
					final Integer sqlTypeCode = resolveSqlTypeCode( componentTypeName, jdbcTypeRegistry.getTypeConfiguration() );
					if ( sqlTypeCode != null ) {
						return ( (ArrayJdbcType) jdbcType ).resolveType(
								jdbcTypeRegistry.getTypeConfiguration(),
								this,
								jdbcTypeRegistry.getDescriptor( sqlTypeCode ),
								null
						);
					}
				}
				return jdbcType;
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
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		if ( unit == null ) {
			return "(?3-?2)";
		}
		if ( toTemporalType != TemporalType.TIMESTAMP && fromTemporalType != TemporalType.TIMESTAMP && unit == DAY ) {
			// special case: subtraction of two dates
			// results in an integer number of days
			// instead of an INTERVAL
			return "(?3-?2)";
		}
		else {
			StringBuilder pattern = new StringBuilder();
			switch ( unit ) {
				case YEAR:
					extractField( pattern, YEAR, fromTemporalType, toTemporalType, unit );
					break;
				case QUARTER:
					pattern.append( "(" );
					extractField( pattern, YEAR, fromTemporalType, toTemporalType, unit );
					pattern.append( "+" );
					extractField( pattern, QUARTER, fromTemporalType, toTemporalType, unit );
					pattern.append( ")" );
					break;
				case MONTH:
					pattern.append( "(" );
					extractField( pattern, YEAR, fromTemporalType, toTemporalType, unit );
					pattern.append( "+" );
					extractField( pattern, MONTH, fromTemporalType, toTemporalType, unit );
					pattern.append( ")" );
					break;
				case WEEK: //week is not supported by extract() when the argument is a duration
				case DAY:
					extractField( pattern, DAY, fromTemporalType, toTemporalType, unit );
					break;
				//in order to avoid multiple calls to extract(),
				//we use extract(epoch from x - y) * factor for
				//all the following units:
				case HOUR:
				case MINUTE:
				case SECOND:
				case NANOSECOND:
				case NATIVE:
					extractField( pattern, EPOCH, fromTemporalType, toTemporalType, unit );
					break;
				default:
					throw new SemanticException( "unrecognized field: " + unit );
			}
			return pattern.toString();
		}
	}

	protected void extractField(
			StringBuilder pattern,
			TemporalUnit unit,
			TemporalType fromTimestamp,
			TemporalType toTimestamp,
			TemporalUnit toUnit) {
		pattern.append( "extract(" );
		pattern.append( translateDurationField( unit ) );
		pattern.append( " from " );
		if ( toTimestamp != TemporalType.TIMESTAMP && fromTimestamp != TemporalType.TIMESTAMP ) {
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
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(queryEngine);

		functionFactory.round_floor(); //Postgres round(x,n) does not accept double
		functionFactory.cot();
		functionFactory.radians();
		functionFactory.degrees();
		functionFactory.trunc();
		functionFactory.log();
		if ( getVersion().isSameOrAfter(12) ) {
			functionFactory.log10();
		}
		else {
			queryEngine.getSqmFunctionRegistry().registerAlternateKey( "log10", "log" );
		}
		functionFactory.cbrt();
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
		functionFactory.dateTrunc();
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

		if ( getVersion().isSameOrAfter( 9, 4 ) ) {
			functionFactory.makeDateTimeTimestamp();
			// Note that PostgreSQL doesn't support the OVER clause for ordered set-aggregate functions
			functionFactory.inverseDistributionOrderedSetAggregates();
			functionFactory.hypotheticalOrderedSetAggregates();
		}

		if ( !supportsMinMaxOnUuid() ) {
			queryEngine.getSqmFunctionRegistry().register( "min", new PostgreSQLMinMaxFunction( "min" ) );
			queryEngine.getSqmFunctionRegistry().register( "max", new PostgreSQLMinMaxFunction( "max" ) );
		}
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
	 *     sortop = operator (<)
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
	 *     sortop = operator (>)
	 *     );
	 * </code>
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
		return getVersion().isSameOrAfter( 8, 2 );
	}

	@Override
	public boolean supportsIfExistsBeforeConstraintName() {
		return getVersion().isSameOrAfter( 9 );
	}

	@Override
	public boolean supportsIfExistsAfterAlterTable() {
		return getVersion().isSameOrAfter( 9, 2 );
	}

	@Override
	public boolean supportsValuesList() {
		return getVersion().isSameOrAfter( 8, 2 );
	}

	@Override
	public boolean supportsPartitionBy() {
		return getVersion().isSameOrAfter( 9, 1 );
	}

	@Override
	public boolean supportsNonQueryWithCTE() {
		return getVersion().isSameOrAfter( 9, 1 );
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return getVersion().isBefore( 8, 2 )
				? PostgreSQLSequenceSupport.LEGACY_INSTANCE
				: PostgreSQLSequenceSupport.INSTANCE;
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
		return getVersion().isBefore( 8, 4 )
				? LimitOffsetLimitHandler.INSTANCE
				: OffsetFetchLimitHandler.INSTANCE;
	}

	@Override
	public String getForUpdateString(String aliases) {
		return getForUpdateString() + " of " + aliases;
	}

	@Override
	public String getForUpdateString(String aliases, LockOptions lockOptions) {
		/*
		 * Parent's implementation for (aliases, lockOptions) ignores aliases.
		 */
		if ( aliases.isEmpty() ) {
			LockMode lockMode = lockOptions.getLockMode();
			final Iterator<Map.Entry<String, LockMode>> itr = lockOptions.getAliasLockIterator();
			while ( itr.hasNext() ) {
				// seek the highest lock mode
				final Map.Entry<String, LockMode> entry = itr.next();
				final LockMode lm = entry.getValue();
				if ( lm.greaterThan( lockMode ) ) {
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
	public String getSelectClauseNullString(int sqlType, TypeConfiguration typeConfiguration) {
		// Workaround for postgres bug #1453
		return "null::" + typeConfiguration.getDdlTypeRegistry().getDescriptor( sqlType ).getRawTypeName();
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
		return PostgresCallableStatementSupport.INSTANCE;
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
		return IDENTITY_COLUMN_SUPPORT;
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
				appendAsTimestampWithMicros( appender, calendar, jdbcTimeZone );
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
		return getVersion().isSameOrAfter( 8, 1 );
	}

	@Override
	public boolean supportsWait() {
		return false;
	}

	@Override
	public boolean supportsSkipLocked() {
		return getVersion().isSameOrAfter( 9, 5 );
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
		return getVersion().isSameOrAfter( 9, 3 );
	}

	@Override
	public boolean supportsFetchClause(FetchClauseType type) {
		switch ( type ) {
			case ROWS_ONLY:
				return getVersion().isSameOrAfter( 8, 4 );
			case PERCENT_ONLY:
			case PERCENT_WITH_TIES:
				return false;
			case ROWS_WITH_TIES:
				return getVersion().isSameOrAfter( 13 );
		}
		return false;
	}

	@Override
	public RowLockStrategy getWriteRowLockStrategy() {
		return RowLockStrategy.TABLE;
	}

	@Override
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
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes(typeContributions, serviceRegistry);

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
		jdbcTypeRegistry.addDescriptor( TIMESTAMP_UTC, InstantAsTimestampWithTimeZoneJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( XmlJdbcType.INSTANCE );

		if ( driverKind == PostgreSQLDriverKind.PG_JDBC ) {
			if ( PostgreSQLPGObjectJdbcType.isUsable() ) {
				jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLInetJdbcType.INSTANCE );
				jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLIntervalSecondJdbcType.INSTANCE );
			}

			if ( getVersion().isSameOrAfter( 8, 2 ) ) {
				// HHH-9562
				jdbcTypeRegistry.addDescriptorIfAbsent( UUIDJdbcType.INSTANCE );
				if ( getVersion().isSameOrAfter( 9, 2 ) ) {
					if ( PostgreSQLPGObjectJdbcType.isUsable() ) {
						jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJsonbJdbcType.INSTANCE );
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
								.getDescriptor( Object.class )
				)
		);
	}
}
