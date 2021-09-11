/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.*;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.SQLServerFormatEmulation;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.SQLServerIdentityColumnSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.SQLServer2005LimitHandler;
import org.hibernate.dialect.pagination.SQLServer2012LimitHandler;
import org.hibernate.dialect.pagination.TopLimitHandler;
import org.hibernate.dialect.sequence.NoSequenceSupport;
import org.hibernate.dialect.sequence.SQLServerSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.query.CastType;
import org.hibernate.query.FetchClauseType;
import org.hibernate.query.NullPrecedence;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.SmallIntTypeDescriptor;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.regex.Pattern;

import javax.persistence.TemporalType;

import static java.util.regex.Pattern.compile;
import static org.hibernate.query.TemporalUnit.NANOSECOND;

/**
 * A dialect for Microsoft SQL Server 2000 and above
 *
 * @author Gavin King
 */
public class SQLServerDialect extends AbstractTransactSQLDialect {
	private static final int PARAM_LIST_SIZE_LIMIT = 2100;

	private final int version;

	public SQLServerDialect(DialectResolutionInfo info) {
		this( info.getDatabaseMajorVersion() );
	}

	public SQLServerDialect() {
		this( 8 );
	}

	public SQLServerDialect(int version) {
		super();
		this.version = version;

		//there is no 'double' type in SQL server
		//but 'float' is double precision by default
		registerColumnType( Types.DOUBLE, "float" );

		if ( getVersion() >= 10 ) {
			registerColumnType( Types.DATE, "date" );
			registerColumnType( Types.TIME, "time" );
			registerColumnType( Types.TIMESTAMP, "datetime2($p)" );
			registerColumnType( Types.TIMESTAMP_WITH_TIMEZONE, "datetimeoffset($p)" );
		}

		registerColumnType( Types.VARCHAR, 8000, "varchar($l)" );
		registerColumnType( Types.NVARCHAR, 4000, "nvarchar($l)" );
		registerColumnType( Types.VARBINARY, 8000, "varbinary($l)" );

		if ( getVersion() < 9 ) {
			registerColumnType( Types.VARBINARY, "image" );
			registerColumnType( Types.VARCHAR, "text" );
		}
		else {

			// Use 'varchar(max)' and 'varbinary(max)' instead
			// the deprecated TEXT and IMAGE types. Note that
			// the length of a VARCHAR or VARBINARY column must
			// be either between 1 and 8000 or exactly MAX, and
			// the length of an NVARCHAR column must be either
			// between 1 and 4000 or exactly MAX.

			// See http://www.sql-server-helper.com/faq/sql-server-2005-varchar-max-p01.aspx
			// See HHH-3965

			registerColumnType( Types.BLOB, "varbinary(max)" );
			registerColumnType( Types.VARBINARY, "varbinary(max)" );

			registerColumnType( Types.CLOB, "varchar(max)" );
			registerColumnType( Types.NCLOB, "nvarchar(max)" ); // HHH-8435 fix
			registerColumnType( Types.VARCHAR, "varchar(max)" );
			registerColumnType( Types.NVARCHAR, "nvarchar(max)" );
		}

		registerKeyword( "top" );
		registerKeyword( "key" );
	}

	@Override
	public int getVersion() {
		return version;
	}

	@Override
	public boolean supportsTimezoneTypes() {
		return getVersion() >= 10;
	}

	@Override
	public long getDefaultLobLength() {
		// this is essentially the only legal length for
		// a "lob" in SQL Server, i.e. the value of MAX
		// (caveat: for NVARCHAR it is half this value)
		return 2_147_483_647;
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes(typeContributions, serviceRegistry);

		typeContributions.getTypeConfiguration().getJdbcTypeDescriptorRegistry()
				.addDescriptor(Types.TINYINT, SmallIntTypeDescriptor.INSTANCE);
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry(queryEngine);

		CommonFunctionFactory.truncate_round( queryEngine );
		CommonFunctionFactory.everyAny_sumIif( queryEngine );
		CommonFunctionFactory.bitLength_pattern( queryEngine, "datalength(?1) * 8" );

		if ( getVersion() >= 10 ) {
			CommonFunctionFactory.locate_charindex( queryEngine );
			CommonFunctionFactory.stddevPopSamp_stdevp( queryEngine );
			CommonFunctionFactory.varPopSamp_varp( queryEngine );
		}

		if ( getVersion() >= 11 ) {
			queryEngine.getSqmFunctionRegistry().register( "format", new SQLServerFormatEmulation( this ) );

			//actually translate() was added in 2017 but
			//it's not worth adding a new dialect for that!
			CommonFunctionFactory.translate( queryEngine );

			CommonFunctionFactory.median_percentileCont( queryEngine, true );

			queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "datefromparts" )
					.setInvariantType( StandardBasicTypes.DATE )
					.setExactArgumentCount( 3 )
					.register();
			queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "timefromparts" )
					.setInvariantType( StandardBasicTypes.TIME )
					.setExactArgumentCount( 5 )
					.register();
			queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "smalldatetimefromparts" )
					.setInvariantType( StandardBasicTypes.TIMESTAMP )
					.setExactArgumentCount( 5 )
					.register();
			queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "datetimefromparts" )
					.setInvariantType( StandardBasicTypes.TIMESTAMP )
					.setExactArgumentCount( 7 )
					.register();
			queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "datetime2fromparts" )
					.setInvariantType( StandardBasicTypes.TIMESTAMP )
					.setExactArgumentCount( 8 )
					.register();
			queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "datetimeoffsetfromparts" )
					.setInvariantType( StandardBasicTypes.TIMESTAMP )
					.setExactArgumentCount( 10 )
					.register();
		}
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new SQLServerSqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	@Override
	public String castPattern(CastType from, CastType to) {
		if ( to == CastType.STRING ) {
			switch ( from ) {
				case TIMESTAMP:
					// SQL Server uses yyyy-MM-dd HH:mm:ss.nnnnnnn by default when doing a cast, but only need second precision
					return "format(?1,'yyyy-MM-dd HH:mm:ss')";
				case TIME:
					// SQL Server uses HH:mm:ss.nnnnnnn by default when doing a cast, but only need second precision
					// SQL Server requires quoting of ':' in time formats and the use of 'hh' instead of 'HH'
					return "format(?1,'hh\\:mm\\:ss')";
			}
		}
		return super.castPattern( from, to );
	}

	@Override
	public String currentTimestamp() {
		return "sysdatetime()";
	}

	@Override
	public IdentifierHelper buildIdentifierHelper(
			IdentifierHelperBuilder builder, DatabaseMetaData dbMetaData) throws SQLException {

		if ( dbMetaData == null ) {
			builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.MIXED );
			builder.setQuotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		}

		return super.buildIdentifierHelper( builder, dbMetaData );
	}

	@Override
	public String currentTime() {
		return "convert(time,getdate())";
	}

	@Override
	public String currentDate() {
		return currentTimestamp();
	}

	@Override
	public String currentTimestampWithTimeZone() {
		return "sysdatetimeoffset()";
	}

	@Override
	public String getNoColumnsInsertString() {
		return "default values";
	}

	@Override
	public LimitHandler getLimitHandler() {
		if ( getVersion() >= 11 ) {
			return SQLServer2012LimitHandler.INSTANCE;
		}
		else if ( getVersion() >= 9 ) {
			//this is a stateful class, don't cache
			//it in the Dialect!
			return new SQLServer2005LimitHandler();
		}
		else {
			return new TopLimitHandler(false);
		}
	}

	@Override
	public boolean supportsValuesList() {
		return getVersion() >= 10;
	}

	@Override
	public char closeQuote() {
		return ']';
	}

	@Override
	public String getCurrentSchemaCommand() {
		return "select schema_name()";
	}

	@Override
	public char openQuote() {
		return '[';
	}

	@Override
	public String appendLockHint(LockOptions lockOptions, String tableName) {
		if ( getVersion() >= 9 ) {
			LockMode lockMode = lockOptions.getAliasSpecificLockMode( tableName );
			if (lockMode == null) {
				lockMode = lockOptions.getLockMode();
			}

			final String writeLockStr = lockOptions.getTimeOut() == LockOptions.SKIP_LOCKED ? "updlock" : "updlock,holdlock";
			final String readLockStr = lockOptions.getTimeOut() == LockOptions.SKIP_LOCKED ? "updlock" : "holdlock";

			final String noWaitStr = lockOptions.getTimeOut() == LockOptions.NO_WAIT ? ",nowait" : "";
			final String skipLockStr = lockOptions.getTimeOut() == LockOptions.SKIP_LOCKED ? ",readpast" : "";

			switch ( lockMode ) {
				//noinspection deprecation
				case UPGRADE:
				case PESSIMISTIC_WRITE:
				case WRITE:
					return tableName + " with (" + writeLockStr + ",rowlock" + noWaitStr + skipLockStr + ")";
				case PESSIMISTIC_READ:
					return tableName + " with (" + readLockStr + ",rowlock" + noWaitStr + skipLockStr + ")";
				case UPGRADE_SKIPLOCKED:
					return tableName + " with (updlock,rowlock,readpast" + noWaitStr + ")";
				case UPGRADE_NOWAIT:
					return tableName + " with (updlock,holdlock,rowlock,nowait)";
				default:
					return tableName;
			}
		}
		else {
			switch ( lockOptions.getLockMode() ) {
				//noinspection deprecation
				case UPGRADE:
				case UPGRADE_NOWAIT:
				case PESSIMISTIC_WRITE:
				case WRITE:
					return tableName + " with (updlock,rowlock)";
				case PESSIMISTIC_READ:
					return tableName + " with (holdlock,rowlock)";
				case UPGRADE_SKIPLOCKED:
					return tableName + " with (updlock,rowlock,readpast)";
				default:
					return tableName;
			}
		}
	}


	/**
	 * The current_timestamp is more accurate, but only known to be supported in SQL Server 7.0 and later and
	 * Sybase not known to support it at all
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public String getCurrentTimestampSelectString() {
		return "select current_timestamp";
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean areStringComparisonsCaseInsensitive() {
		return true;
	}

	@Override
	public boolean supportsResultSetPositionQueryMethodsOnForwardOnlyCursor() {
		return false;
	}

	@Override
	public boolean supportsCircularCascadeDeleteConstraints() {
		// SQL Server (at least up through 2005) does not support defining
		// cascade delete constraints which can circle back to the mutating
		// table
		return false;
	}

	@Override
	public boolean supportsLobValueChangePropagation() {
		// note: at least my local SQL Server 2005 Express shows this not working...
		return false;
	}

	@Override
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		// here assume SQLServer2005 using snapshot isolation, which does not have this problem
		return false;
	}

	@Override
	public boolean doesRepeatableReadCauseReadersToBlockWriters() {
		// here assume SQLServer2005 using snapshot isolation, which does not have this problem
		return false;
	}

	@Override
	public int getInExpressionCountLimit() {
		return PARAM_LIST_SIZE_LIMIT;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new SQLServerIdentityColumnSupport();
	}

	@Override
	public boolean supportsNonQueryWithCTE() {
		return getVersion() >= 9;
	}

	@Override
	public boolean supportsSkipLocked() {
		return getVersion() >= 9;
	}

	@Override
	public boolean supportsNoWait() {
		return getVersion() >= 9;
	}

	@Override
	public boolean supportsWait() {
		return false;
	}

	@Override
	public GroupBySummarizationRenderingStrategy getGroupBySummarizationRenderingStrategy() {
		return GroupBySummarizationRenderingStrategy.CLAUSE;
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return getVersion() < 11
				? NoSequenceSupport.INSTANCE
				: SQLServerSequenceSupport.INSTANCE;
	}

	@Override
	public String getQuerySequencesString() {
		return getVersion() < 11
				? super.getQuerySequencesString() //null
				// The upper-case name should work on both case-sensitive
				// and case-insensitive collations.
				: "select * from INFORMATION_SCHEMA.SEQUENCES";
	}

	@Override
	public String getQueryHintString(String sql, String hints) {
		if ( getVersion() < 11 ) {
			return super.getQueryHintString( sql, hints );
		}

		final StringBuilder buffer = new StringBuilder(
				sql.length() + hints.length() + 12
		);
		final int pos = sql.indexOf( ";" );
		if ( pos > -1 ) {
			buffer.append( sql, 0, pos );
		}
		else {
			buffer.append( sql );
		}
		buffer.append( " OPTION (" ).append( hints ).append( ")" );
		if ( pos > -1 ) {
			buffer.append( ";" );
		}
		sql = buffer.toString();

		return sql;
	}

	@Override
	public boolean supportsNullPrecedence() {
		return getVersion() < 10;
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
	public boolean supportsFetchClause(FetchClauseType type) {
		return getVersion() >= 11;
	}

	@Override
	public String renderOrderByElement(String expression, String collation, String order, NullPrecedence nulls) {
		if ( getVersion() < 10 ) {
			return super.renderOrderByElement( expression, collation, order, nulls );
		}

		final StringBuilder orderByElement = new StringBuilder();

		if ( nulls != null && !NullPrecedence.NONE.equals( nulls ) ) {
			// Workaround for NULLS FIRST / LAST support.
			orderByElement.append( "case when " ).append( expression ).append( " is null then " );
			if ( NullPrecedence.FIRST.equals( nulls ) ) {
				orderByElement.append( "0 else 1" );
			}
			else {
				orderByElement.append( "1 else 0" );
			}
			orderByElement.append( " end," );
		}

		// Nulls precedence has already been handled so passing NONE value.
		orderByElement.append( super.renderOrderByElement( expression, collation, order, NullPrecedence.NONE ) );

		return orderByElement.toString();
	}

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		if ( getVersion() < 9 ) {
			return super.buildSQLExceptionConversionDelegate(); //null
		}
		return (sqlException, message, sql) -> {
			final String sqlState = JdbcExceptionHelper.extractSqlState( sqlException );
			final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );
			if ( "HY008".equals( sqlState ) ) {
				throw new QueryTimeoutException( message, sqlException, sql );
			}
			if ( 1222 == errorCode ) {
				throw new LockTimeoutException( message, sqlException, sql );
			}
			return null;
		};
	}

	/**
	 * SQL server supports up to 7 decimal digits of
	 * fractional second precision in a datetime2,
	 * but since its duration arithmetic functions
	 * try to fit durations into an int,
	 * which is impossible with such high precision,
	 * so default to generating {@code datetime2(3)}
	 * columns.
	 */
	@Override
	public int getDefaultTimestampPrecision() {
		return 6; //microseconds!
	}

	/**
	 * SQL server supports up to 7 decimal digits of
	 * fractional second precision in a datetime2,
	 * but unfortunately its duration arithmetic
	 * functions have a nasty habit of overflowing.
	 * So to give ourselves a little extra headroom,
	 * we will use {@code microsecond} as the native
	 * unit of precision (but even then we have to
	 * use tricks when calling {@code dateadd()}).
	 */
	@Override
	public long getFractionalSecondPrecisionInNanos() {
		return 1_000; //microseconds!
	}

	@Override
	public String extractPattern(TemporalUnit unit) {
		switch (unit) {
			case TIMEZONE_HOUR:
				return "(datepart(tz,?2)/60)";
			case TIMEZONE_MINUTE:
				return "(datepart(tz,?2)%60)";
			//currently Dialect.extract() doesn't need
			//to handle NANOSECOND (might change that?)
//			case NANOSECOND:
//				//this should evaluate to a bigint type
//				return "(datepart(second,?2)*1000000000+datepart(nanosecond,?2))";
			case SECOND:
				//this should evaluate to a floating point type
				return "(datepart(second,?2)+datepart(nanosecond,?2)/1e9)";
			case WEEK:
				// Thanks https://www.sqlservercentral.com/articles/a-simple-formula-to-calculate-the-iso-week-number
				if ( getVersion() < 10 ) {
					return "(DATEPART(dy,DATEADD(dd,DATEDIFF(dd,'17530101',?2)/7*7,'17530104'))+6)/7)";
				}
			default:
				return "datepart(?1,?2)";
		}
	}

	@Override
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType) {
		// dateadd() supports only especially small magnitudes
		// since it casts its argument to int (and unfortunately
		// there's no dateadd_big()) so here we need to use two
		// calls to dateadd() to add a whole duration
		switch (unit) {
			case NANOSECOND:
				//Java Durations are usually the only thing
				//we find expressed in nanosecond precision,
				//and they can easily be very large
				return "dateadd(nanosecond,?2%1000000000,dateadd(second,?2/1000000000,?3))";
			case NATIVE:
				//microsecond is the "native" precision
				return "dateadd(microsecond,?2%1000000,dateadd(second,?2/1000000,?3))";
			default:
				return "dateadd(?1,?2,?3)";
		}
	}

	@Override
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		switch (unit) {
			case NATIVE:
				//use microsecond as the "native" precision
				return "datediff_big(microsecond,?2,?3)";
			default:
				//datediff() returns an int, and can easily
				//overflow when dealing with "physical"
				//durations, so use datediff_big()
				return unit.normalized() == NANOSECOND
						? "datediff_big(?1,?2,?3)"
						: "datediff(?1,?2,?3)";
		}
	}

	@Override
	public String translateDurationField(TemporalUnit unit) {
		switch (unit) {
			//use microsecond as the "native" precision
			case NATIVE: return "microsecond";
			default: return super.translateDurationField(unit);
		}
	}

	@Override
	public String translateExtractField(TemporalUnit unit) {
		switch ( unit ) {
			//the ISO week number (behavior of "week" depends on a system property)
			case WEEK: return "isowk";
			case OFFSET: return "tz";
			default: return super.translateExtractField(unit);
		}
	}

	@Override
	public String translateDatetimeFormat(String format) {
		return datetimeFormat(format).result();
	}

	public static Replacer datetimeFormat(String format) {
		return new Replacer( format, "'", "\"" )
				//era
				.replace("G", "g")

				//y nothing to do
				//M nothing to do

				//w no equivalent
				//W no equivalent
				//Y no equivalent

				//day of week
				.replace("EEEE", "dddd")
				.replace("EEE", "ddd")
				//e no equivalent

				//d nothing to do
				//D no equivalent

				//am pm
				.replace("aa", "tt")
				.replace("a", "tt")

				//h nothing to do
				//H nothing to do

				//m nothing to do
				//s nothing to do

				//fractional seconds
				.replace("S", "F")

				//timezones
				.replace("XXX", "K") //UTC represented as "Z"
				.replace("xxx", "zzz")
				.replace("x", "zz");
	}

	private static final Pattern OFFSET_PATTERN = compile(".*[-+]\\d{2}(:\\d{2})?$");

	@Override
	public String formatBinaryLiteral(byte[] bytes) {
		return "0x" + StandardBasicTypes.BINARY.toString( bytes );
	}

	@Override
	protected String wrapTimestampLiteral(String timestamp) {
		//needed because the {ts ... } JDBC escape chokes on microseconds
		return OFFSET_PATTERN.matcher( timestamp ).matches()
				? "cast('" + timestamp + "' as datetimeoffset)"
				: "cast('" + timestamp + "' as datetime2)";
	}

	@Override
	protected String wrapTimeLiteral(String time) {
		//needed because the {t ... } JDBC is just buggy
		return "cast('" + time + "' as time)";
	}

	@Override
	protected String wrapDateLiteral(String date) {
		//possibly not needed
		return "cast('" + date + "' as date)";
	}

	@Override
	public String getCreateTemporaryTableColumnAnnotation(int sqlTypeCode) {
		switch (sqlTypeCode) {
			case Types.CHAR:
			case Types.NCHAR:
			case Types.VARCHAR:
			case Types.NVARCHAR:
			case Types.LONGVARCHAR:
			case Types.LONGNVARCHAR:
				return "collate database_default";
			default:
				return "";
		}
	}

	@Override
	public GroupByConstantRenderingStrategy getGroupByConstantRenderingStrategy() {
		return GroupByConstantRenderingStrategy.EMPTY_GROUPING;
	}

}
