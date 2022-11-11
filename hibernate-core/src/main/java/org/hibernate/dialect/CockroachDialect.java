/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.persistence.TemporalType;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.PessimisticLockException;
import org.hibernate.QueryTimeoutException;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.FormatFunction;
import org.hibernate.dialect.identity.CockroachDBIdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.LimitHandler;
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
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.sqm.NullOrdering;
import org.hibernate.query.sqm.TemporalUnit;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.InstantAsTimestampWithTimeZoneJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.ObjectNullAsBinaryTypeJdbcType;
import org.hibernate.type.descriptor.jdbc.UUIDJdbcType;
import org.hibernate.type.descriptor.jdbc.VarbinaryJdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.internal.Scale6IntervalSecondDdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.query.sqm.TemporalUnit.DAY;
import static org.hibernate.query.sqm.TemporalUnit.NATIVE;
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
 * A {@linkplain Dialect SQL dialect} for CockroachDB.
 *
 * @author Gavin King
 */
public class CockroachDialect extends Dialect {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, CockroachDialect.class.getName() );
	private static final CockroachDBIdentityColumnSupport IDENTITY_COLUMN_SUPPORT = new CockroachDBIdentityColumnSupport();
	// KNOWN LIMITATIONS:
	// * no support for java.sql.Clob

	// Pre-compile and reuse pattern
	private static final Pattern CRDB_VERSION_PATTERN = Pattern.compile( "v[\\d]+(\\.[\\d]+)?(\\.[\\d]+)?" );

	private static final DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 19, 2 );

	private final PostgreSQLDriverKind driverKind;

	public CockroachDialect() {
		this( MINIMUM_VERSION );
	}

	public CockroachDialect(DialectResolutionInfo info) {
		this( fetchDataBaseVersion( info ), PostgreSQLDriverKind.determineKind( info ) );
		registerKeywords( info );
	}

	public CockroachDialect(DatabaseVersion version) {
		super(version);
		driverKind = PostgreSQLDriverKind.PG_JDBC;
	}

	public CockroachDialect(DatabaseVersion version, PostgreSQLDriverKind driverKind) {
		super(version);
		this.driverKind = driverKind;
	}

	protected static DatabaseVersion fetchDataBaseVersion( DialectResolutionInfo info ) {
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
				String[] versionParts = m.group().substring( 1 ).split( "\\." );
				// if we got to this point, there is at least a major version, so no need to check [].length > 0
				int majorVersion = Integer.parseInt( versionParts[0] );
				int minorVersion = versionParts.length > 1 ? Integer.parseInt( versionParts[1] ) : 0;
				int microVersion = versionParts.length > 2 ? Integer.parseInt( versionParts[2] ) : 0;

				databaseVersion=  new SimpleDatabaseVersion( majorVersion, minorVersion, microVersion);
		}
		if ( databaseVersion == null ) {
			LOG.unableToDetermineCockroachDatabaseVersion(
					MINIMUM_VERSION.getDatabaseMajorVersion() + "." +
							MINIMUM_VERSION.getDatabaseMinorVersion() + "." +
							MINIMUM_VERSION.getDatabaseMicroVersion()
			);
			databaseVersion = MINIMUM_VERSION;
		}
		return databaseVersion;
	}

	@Override
	protected String columnType(int sqlTypeCode) {
		switch ( sqlTypeCode ) {
			case TINYINT:
				return "smallint"; //no tinyint
			case INTEGER:
				return "int4";

			case NCHAR:
				return columnType( CHAR );
			case NVARCHAR:
				return columnType( VARCHAR );

			case NCLOB:
			case CLOB:
				return "string";

			case BINARY:
			case VARBINARY:
			case BLOB:
				return "bytes";

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
				return "string";
			case BINARY:
			case VARBINARY:
			case LONG32VARBINARY:
				return "bytes";
		}
		return super.castType( sqlTypeCode );
	}

	@Override
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( UUID, "uuid", this ) );
		if ( PostgreSQLPGObjectJdbcType.isUsable() ) {
			// The following DDL types require that the PGobject class is usable/visible
			ddlTypeRegistry.addDescriptor( new DdlTypeImpl( GEOMETRY, "geometry", this ) );
			ddlTypeRegistry.addDescriptor( new DdlTypeImpl( GEOGRAPHY, "geography", this ) );
			ddlTypeRegistry.addDescriptor( new Scale6IntervalSecondDdlType( this ) );

			// Prefer jsonb if possible
			if ( getVersion().isSameOrAfter( 20 ) ) {
				ddlTypeRegistry.addDescriptor( new DdlTypeImpl( INET, "inet", this ) );
				ddlTypeRegistry.addDescriptor( new DdlTypeImpl( JSON, "jsonb", this ) );
			}
			else {
				ddlTypeRegistry.addDescriptor( new DdlTypeImpl( JSON, "json", this ) );
			}
		}
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
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );

		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration()
				.getJdbcTypeRegistry();
		jdbcTypeRegistry.addDescriptor( TIMESTAMP_UTC, InstantAsTimestampWithTimeZoneJdbcType.INSTANCE );
		if ( driverKind == PostgreSQLDriverKind.PG_JDBC ) {
			jdbcTypeRegistry.addDescriptorIfAbsent( UUIDJdbcType.INSTANCE );
			if ( PostgreSQLPGObjectJdbcType.isUsable() ) {
				jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLIntervalSecondJdbcType.INSTANCE );

				if ( getVersion().isSameOrAfter( 20, 0 ) ) {
					jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLInetJdbcType.INSTANCE );
					jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJsonbJdbcType.INSTANCE );
				}
				else {
					jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLJsonJdbcType.INSTANCE );
				}
			}
		}

		// Force Blob binding to byte[] for CockroachDB
		jdbcTypeRegistry.addDescriptor( Types.BLOB, VarbinaryJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( Types.CLOB, VarcharJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( Types.NCLOB, VarcharJdbcType.INSTANCE );

		// The next two contributions are the same as for Postgresql
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

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry(queryEngine);

		final CommonFunctionFactory functionFactory = new CommonFunctionFactory( queryEngine );
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
		functionFactory.trunc(); //TODO: emulate second arg
		functionFactory.log();
		functionFactory.log10_log();

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

		queryEngine.getSqmFunctionRegistry().register(
				"format",
				new FormatFunction( "experimental_strftime", queryEngine.getTypeConfiguration() )
		);
		functionFactory.windowFunctions();
		functionFactory.listagg_stringAgg( "string" );
		functionFactory.inverseDistributionOrderedSetAggregates();
		functionFactory.hypotheticalOrderedSetAggregates_windowEmulation();
	}

	@Override
	public TimeZoneSupport getTimeZoneSupport() {
		return TimeZoneSupport.NORMALIZE;
	}

	@Override
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		appender.appendSql( bool );
	}

	@Override
	public String getCascadeConstraintsString() {
		return " cascade";
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
	public boolean supportsIfExistsBeforeConstraintName() {
		return true;
	}

	@Override
	public boolean supportsIfExistsAfterAlterTable() {
		return true;
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
	public boolean supportsNullPrecedence() {
		// Not yet implemented: https://www.cockroachlabs.com/docs/v20.2/null-handling.html#nulls-and-sorting
		return false;
	}

	@Override
	public NullOrdering getNullOrdering() {
		return NullOrdering.SMALLEST;
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
	public String getNativeIdentifierGeneratorStrategy() {
		return "sequence";
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return PostgreSQLSequenceSupport.INSTANCE;
	}

	@Override
	public String getQuerySequencesString() {
		return "select sequence_name,sequence_schema,sequence_catalog,start_value,minimum_value,maximum_value,increment from information_schema.sequences";
	}

	@Override
	public boolean supportsLobValueChangePropagation() {
		return false;
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new CockroachSqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	@Override
	public NationalizationSupport getNationalizationSupport() {
		// TEXT / STRING inherently support nationalized data
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
	public boolean supportsTemporalLiteralOffset() {
		return true;
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
				appendAsTimestampWithMicros( appender,date, jdbcTimeZone );
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

	/**
	 * The {@code extract()} function returns {@link TemporalUnit#DAY_OF_WEEK}
	 * numbered from 0 to 6. This isn't consistent with what most other
	 * databases do, so here we adjust the result by generating
	 * {@code (extract(dayofweek,arg)+1))}.
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

	@Override
	public String translateExtractField(TemporalUnit unit) {
		switch ( unit ) {
			case DAY_OF_MONTH: return "day";
			case DAY_OF_YEAR: return "dayofyear";
			case DAY_OF_WEEK: return "dayofweek";
			default: return super.translateExtractField( unit );
		}
	}

	/**
	 * {@code microsecond} is the smallest unit for an {@code interval},
	 * and the highest precision for a {@code timestamp}.
	 */
	@Override
	public long getFractionalSecondPrecisionInNanos() {
		return 1_000; //microseconds
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
				return "(?3+(?2)*interval '1 microsecond')";
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
		switch (unit) {
			case YEAR:
				return "(extract(year from ?3)-extract(year from ?2))";
			case QUARTER:
				return "(extract(year from ?3)*4-extract(year from ?2)*4+extract(month from ?3)//3-extract(month from ?2)//3)";
			case MONTH:
				return "(extract(year from ?3)*12-extract(year from ?2)*12+extract(month from ?3)-extract(month from ?2))";
		}
		if ( toTemporalType != TemporalType.TIMESTAMP && fromTemporalType != TemporalType.TIMESTAMP ) {
			// special case: subtraction of two dates
			// results in an integer number of days
			// instead of an INTERVAL
			return "(?3-?2)" + DAY.conversionFactor( unit, this );
		}
		else {
			switch (unit) {
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

	@Override
	public String translateDurationField(TemporalUnit unit) {
		return unit==NATIVE
				? "microsecond"
				: super.translateDurationField(unit);
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
		appender.appendSql( SpannerDialect.datetimeFormat( format ).result() );
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
	public String getForUpdateString(LockOptions lockOptions) {
		// Support was added in 20.1: https://www.cockroachlabs.com/docs/v20.1/select-for-update.html
		if ( getVersion().isBefore( 20, 1 ) ) {
			return "";
		}
		return super.getForUpdateString( lockOptions );
	}

	@Override
	public String getForUpdateString(String aliases, LockOptions lockOptions) {
		// Support was added in 20.1: https://www.cockroachlabs.com/docs/v20.1/select-for-update.html
		if ( getVersion().isBefore( 20, 1 ) ) {
			return "";
		}
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

	private String withTimeout(String lockString, int timeout) {
		switch (timeout) {
			case LockOptions.NO_WAIT: {
				return supportsNoWait() ? lockString + " nowait" : lockString;
			}
			case LockOptions.SKIP_LOCKED: {
				return supportsSkipLocked() ? lockString + " skip locked" : lockString;
			}
			default: {
				return lockString;
			}
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
				? getForUpdateString() + " nowait"
				: getForUpdateString();
	}

	@Override
	public String getForUpdateNowaitString(String aliases) {
		return supportsNoWait()
				? getForUpdateString( aliases ) + " nowait"
				: getForUpdateString( aliases );
	}

	@Override
	public String getForUpdateSkipLockedString() {
		return supportsSkipLocked()
				? getForUpdateString() + " skip locked"
				: getForUpdateString();
	}

	@Override
	public String getForUpdateSkipLockedString(String aliases) {
		return supportsSkipLocked()
				? getForUpdateString( aliases ) + " skip locked"
				: getForUpdateString( aliases );
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
	public boolean supportsOffsetInSubquery() {
		return true;
	}

	@Override
	public boolean supportsWindowFunctions() {
		return true;
	}

	@Override
	public boolean supportsLateral() {
		return getVersion().isSameOrAfter( 20, 1 );
	}

	@Override
	public boolean supportsNoWait() {
		return getVersion().isSameOrAfter( 20, 1 );
	}

	@Override
	public boolean supportsWait() {
		return false;
	}

	@Override
	public boolean supportsSkipLocked() {
		// See https://www.cockroachlabs.com/docs/stable/select-for-update.html#wait-policies
		return false;
	}

	@Override
	public RowLockStrategy getWriteRowLockStrategy() {
		return getVersion().isSameOrAfter( 20, 1 ) ? RowLockStrategy.TABLE : RowLockStrategy.NONE;
	}

	@Override
	public NameQualifierSupport getNameQualifierSupport() {
		// This method is overridden so the correct value will be returned when
		// DatabaseMetaData is not available.
		return NameQualifierSupport.SCHEMA;
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
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			final String sqlState = JdbcExceptionHelper.extractSqlState( sqlException );
			if ( sqlState == null ) {
				return null;
			}
			switch ( sqlState ) {
				case "40P01":
					// DEADLOCK DETECTED
					return new LockAcquisitionException( message, sqlException, sql);
				case "55P03":
					// LOCK NOT AVAILABLE
					return new PessimisticLockException( message, sqlException, sql);
				case "57014":
					return new QueryTimeoutException( message, sqlException, sql );
				default:
					// returning null allows other delegates to operate
					return null;
			}
		};
	}

// CockroachDB doesn't support this by default. See sql.multiple_modifications_of_table.enabled
//
//	@Override
//	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
//			EntityMappingType rootEntityDescriptor,
//			RuntimeModelCreationContext runtimeModelCreationContext) {
//		return new CteMutationStrategy( rootEntityDescriptor, runtimeModelCreationContext );
//	}
//
//	@Override
//	public SqmMultiTableInsertStrategy getFallbackSqmInsertStrategy(
//			EntityMappingType rootEntityDescriptor,
//			RuntimeModelCreationContext runtimeModelCreationContext) {
//		return new CteInsertStrategy( rootEntityDescriptor, runtimeModelCreationContext );
//	}
}
