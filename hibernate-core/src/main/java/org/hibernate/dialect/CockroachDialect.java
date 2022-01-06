/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.OffsetFetchLimitHandler;
import org.hibernate.dialect.sequence.PostgreSQLSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.IntervalType;
import org.hibernate.query.NullOrdering;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.UUIDJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import jakarta.persistence.TemporalType;

import static org.hibernate.query.TemporalUnit.DAY;
import static org.hibernate.query.TemporalUnit.NATIVE;
import static org.hibernate.type.SqlTypes.*;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsDate;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithMicros;

/**
 * A dialect for CockroachDB.
 *
 * @author Gavin King
 */
public class CockroachDialect extends Dialect {

	// KNOWN LIMITATIONS:

	// * no support for java.sql.Clob

	private final PostgreSQLDriverKind driverKind;

	public CockroachDialect() {
		this( DatabaseVersion.make( 19, 2 ) );
	}

	public CockroachDialect(DialectResolutionInfo info) {
		super(info);
		driverKind = PostgreSQLDriverKind.determineKind( info );
	}

	public CockroachDialect(DatabaseVersion version) {
		super(version);
		driverKind = PostgreSQLDriverKind.PG_JDBC;
	}

	public CockroachDialect(DatabaseVersion version, PostgreSQLDriverKind driverKind) {
		super(version);
		this.driverKind = driverKind;
	}

	@Override
	protected List<Integer> getSupportedJdbcTypeCodes() {
		List<Integer> typeCodes = new ArrayList<>( super.getSupportedJdbcTypeCodes() );
		typeCodes.addAll( List.of(UUID, INTERVAL_SECOND, GEOMETRY, JSON) );
		if ( getVersion().isSameOrAfter( 20 ) ) {
			typeCodes.add(INET);
		}
		return typeCodes;
	}

	@Override
	protected String columnType(int jdbcTypeCode) {
		switch (jdbcTypeCode) {
			case TINYINT:
				return "smallint"; //no tinyint

			case CHAR:
			case NCHAR:
			case VARCHAR:
			case NVARCHAR:
				return "string($l)";

			case NCLOB:
			case CLOB:
				return "string";

			case BINARY:
			case VARBINARY:
			case BLOB:
				return "bytes";

			case INET:
				return "inet";
			case UUID:
				return "uuid";
			case GEOMETRY:
				return "geometry";
			case INTERVAL_SECOND:
				return "interval second($s)";

			case JSON:
				// Prefer jsonb if possible
				return getVersion().isSameOrAfter( 20 ) ? "jsonb" : "json";
			default:
				return super.columnType(jdbcTypeCode);
		}
	}

	@Override
	public String getTypeName(int code, Size size) throws HibernateException {
		// The maximum scale for `interval second` is 6 unfortunately so we have to use numeric by default
		if ( code == SqlTypes.INTERVAL_SECOND ) {
			final Integer scale = size.getScale();
			if ( scale == null || scale > 6 ) {
				return getTypeName( SqlTypes.NUMERIC, size );
			}
		}
		return super.getTypeName( code, size );
	}

	@Override
	public JdbcType resolveSqlTypeDescriptor(
			String columnTypeName,
			int jdbcTypeCode,
			int precision,
			int scale,
			JdbcTypeRegistry jdbcTypeRegistry) {
		if ( jdbcTypeCode == OTHER ) {
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
				case "geography":
					jdbcTypeCode = GEOMETRY;
					break;
			}
		}
		return jdbcTypeRegistry.getDescriptor( jdbcTypeCode );
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );

		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration()
				.getJdbcTypeDescriptorRegistry();
		if ( driverKind == PostgreSQLDriverKind.PG_JDBC ) {
			jdbcTypeRegistry.addDescriptorIfAbsent( UUIDJdbcType.INSTANCE );
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

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry(queryEngine);

		final BasicTypeRegistry basicTypeRegistry = queryEngine.getTypeConfiguration().getBasicTypeRegistry();
		final BasicType<String> stringType = basicTypeRegistry.resolve( StandardBasicTypes.STRING );

		CommonFunctionFactory.ascii( queryEngine );
		CommonFunctionFactory.char_chr( queryEngine );
		CommonFunctionFactory.overlay( queryEngine );
		CommonFunctionFactory.position( queryEngine );
		CommonFunctionFactory.substringFromFor( queryEngine );
		CommonFunctionFactory.locate_positionSubstring( queryEngine );
		CommonFunctionFactory.trim2( queryEngine );
		CommonFunctionFactory.substr( queryEngine );
		CommonFunctionFactory.reverse( queryEngine );
		CommonFunctionFactory.repeat( queryEngine );
		CommonFunctionFactory.md5( queryEngine );
		CommonFunctionFactory.sha1( queryEngine );
		CommonFunctionFactory.octetLength( queryEngine );
		CommonFunctionFactory.bitLength( queryEngine );
		CommonFunctionFactory.cbrt( queryEngine );
		CommonFunctionFactory.cot( queryEngine );
		CommonFunctionFactory.degrees( queryEngine );
		CommonFunctionFactory.radians( queryEngine );
		CommonFunctionFactory.pi( queryEngine );
		CommonFunctionFactory.trunc( queryEngine ); //TODO: emulate second arg

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder("format", "experimental_strftime")
				.setInvariantType( stringType )
				.setArgumentsValidator( CommonFunctionFactory.formatValidator() )
				.setArgumentListSignature("(TEMPORAL datetime as STRING pattern)")
				.register();
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
				appender.appendSql( "time '" );
				appendAsTime( appender, temporalAccessor, supportsTemporalLiteralOffset(), jdbcTimeZone );
				appender.appendSql( '\'' );
				break;
			case TIMESTAMP:
				appender.appendSql( "timestamp with time zone '" );
				appendAsTimestampWithMicros( appender, temporalAccessor, supportsTemporalLiteralOffset(), jdbcTimeZone );
				appender.appendSql( '\'' );
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
				appender.appendSql( "time '" );
				appendAsTime( appender, date );
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
				appender.appendSql( "time '" );
				appendAsTime( appender, calendar );
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
			case SECOND:
				return "(extract(second from ?2)+extract(microsecond from ?2)/1e6)";
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
			//noinspection deprecation
			case UPGRADE:
				return getForUpdateString(aliases);
			case PESSIMISTIC_READ:
				return getReadLockString( aliases, lockOptions.getTimeOut() );
			case PESSIMISTIC_WRITE:
				return getWriteLockString( aliases, lockOptions.getTimeOut() );
			case UPGRADE_NOWAIT:
				//noinspection deprecation
			case FORCE:
			case PESSIMISTIC_FORCE_INCREMENT:
				return getForUpdateNowaitString(aliases);
			case UPGRADE_SKIPLOCKED:
				return getForUpdateSkipLockedString(aliases);
			default:
				return "";
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
	public boolean supportsOuterJoinForUpdate() {
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
		return getVersion().isSameOrAfter( 20, 1 );
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
}
