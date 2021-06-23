/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.LockOptions;
import org.hibernate.query.NullPrecedence;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.DB2FormatEmulation;
import org.hibernate.dialect.identity.DB2IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.DB2LimitHandler;
import org.hibernate.dialect.pagination.LegacyDB2LimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.sequence.DB2SequenceSupport;
import org.hibernate.dialect.sequence.LegacyDB2SequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.unique.DB2UniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.mutation.internal.cte.CteStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorDB2DatabaseImpl;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.*;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;
import javax.persistence.TemporalType;

/**
 * An SQL dialect for DB2.
 *
 * @author Gavin King
 */
public class DB2Dialect extends Dialect {

	private static final String FOR_READ_ONLY_SQL = " for read only with rs";
	private static final String FOR_SHARE_SQL = FOR_READ_ONLY_SQL + " use and keep share locks";
	private static final String FOR_UPDATE_SQL = FOR_READ_ONLY_SQL + " use and keep update locks";
	private static final String SKIP_LOCKED_SQL = " skip locked data";
	private static final String FOR_SHARE_SKIP_LOCKED_SQL = FOR_SHARE_SQL + SKIP_LOCKED_SQL;
	private static final String FOR_UPDATE_SKIP_LOCKED_SQL = FOR_UPDATE_SQL + SKIP_LOCKED_SQL;

	private final int version;

	private final LimitHandler limitHandler;
	private final UniqueDelegate uniqueDelegate;

	public DB2Dialect(DialectResolutionInfo info) {
		this( info.getDatabaseMajorVersion() * 100 + info.getDatabaseMinorVersion() * 10 );
	}

	public DB2Dialect() {
		this( 900 );
	}

	public DB2Dialect(int version) {
		super();
		this.version = version;

		registerColumnType( Types.TINYINT, "smallint" ); //no tinyint

		//HHH-12827: map them both to the same type to
		//           avoid problems with schema update
//		registerColumnType( Types.DECIMAL, "decimal($p,$s)" );
		registerColumnType( Types.NUMERIC, "decimal($p,$s)" );

		if ( getVersion() < 1100 ) {
			registerColumnType( Types.BINARY, "varchar($l) for bit data" ); //should use 'binary' since version 11
			registerColumnType( Types.BINARY, 254, "char($l) for bit data" ); //should use 'binary' since version 11
			registerColumnType( Types.VARBINARY, "varchar($l) for bit data" ); //should use 'varbinary' since version 11

			//prior to DB2 11, the 'boolean' type existed,
			//but was not allowed as a column type
			registerColumnType( Types.BOOLEAN, "smallint" );
		}

		registerColumnType( Types.BLOB, "blob($l)" );
		registerColumnType( Types.CLOB, "clob($l)" );

		registerColumnType( Types.TIMESTAMP_WITH_TIMEZONE, "timestamp($p)" );
		registerColumnType( Types.TIME_WITH_TIMEZONE, "time" );

		registerColumnType( Types.LONGVARCHAR, "long varchar" );

		//not keywords, at least not in DB2 11,
		//but perhaps they were in older versions?
		registerKeyword( "current" );
		registerKeyword( "date" );
		registerKeyword( "time" );
		registerKeyword( "timestamp" );
		registerKeyword( "fetch" );
		registerKeyword( "first" );
		registerKeyword( "rows" );
		registerKeyword( "only" );

		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, NO_BATCH );

		uniqueDelegate = createUniqueDelegate();

		limitHandler = getVersion() < 1110
				? LegacyDB2LimitHandler.INSTANCE
				: DB2LimitHandler.INSTANCE;
	}

	protected UniqueDelegate createUniqueDelegate() {
		return new DB2UniqueDelegate( this );
	}

	@Override
	public int getDefaultDecimalPrecision() {
		//this is the maximum allowed in DB2
		return 31;
	}

	@Override
	public int getVersion() {
		return version;
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		CommonFunctionFactory.cot( queryEngine );
		CommonFunctionFactory.degrees( queryEngine );
		CommonFunctionFactory.log( queryEngine );
		CommonFunctionFactory.log10( queryEngine );
		CommonFunctionFactory.radians( queryEngine );
		CommonFunctionFactory.rand( queryEngine );
		CommonFunctionFactory.soundex( queryEngine );
		CommonFunctionFactory.trim2( queryEngine );
		CommonFunctionFactory.space( queryEngine );
		CommonFunctionFactory.repeat( queryEngine );
		CommonFunctionFactory.substr( queryEngine );
		//also natively supports ANSI-style substring()
		CommonFunctionFactory.translate( queryEngine );
		CommonFunctionFactory.bitand( queryEngine );
		CommonFunctionFactory.bitor( queryEngine );
		CommonFunctionFactory.bitxor( queryEngine );
		CommonFunctionFactory.bitnot( queryEngine );
		CommonFunctionFactory.yearMonthDay( queryEngine );
		CommonFunctionFactory.hourMinuteSecond( queryEngine );
		CommonFunctionFactory.dayofweekmonthyear( queryEngine );
		CommonFunctionFactory.weekQuarter( queryEngine );
		CommonFunctionFactory.daynameMonthname( queryEngine );
		CommonFunctionFactory.lastDay( queryEngine );
		CommonFunctionFactory.toCharNumberDateTimestamp( queryEngine );
		CommonFunctionFactory.dateTimeTimestamp( queryEngine );
		CommonFunctionFactory.concat_pipeOperator( queryEngine );
		CommonFunctionFactory.octetLength( queryEngine );
		CommonFunctionFactory.ascii( queryEngine );
		CommonFunctionFactory.char_chr( queryEngine );
		CommonFunctionFactory.position( queryEngine );
		CommonFunctionFactory.trunc( queryEngine );
		CommonFunctionFactory.truncate( queryEngine );
		CommonFunctionFactory.insert( queryEngine );
		CommonFunctionFactory.overlayCharacterLength_overlay( queryEngine );
		CommonFunctionFactory.median( queryEngine );
		CommonFunctionFactory.stddev( queryEngine );
		CommonFunctionFactory.stddevPopSamp( queryEngine );
		CommonFunctionFactory.regrLinearRegressionAggregates( queryEngine );
		CommonFunctionFactory.variance( queryEngine );
		CommonFunctionFactory.stdevVarianceSamp( queryEngine );
		CommonFunctionFactory.addYearsMonthsDaysHoursMinutesSeconds( queryEngine );
		CommonFunctionFactory.yearsMonthsDaysHoursMinutesSecondsBetween( queryEngine );
		CommonFunctionFactory.dateTrunc( queryEngine );

		queryEngine.getSqmFunctionRegistry().register( "format", new DB2FormatEmulation() );

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "posstr" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature("(string, pattern)")
				.register();
	}

	@Override
	public String[] getDropSchemaCommand(String schemaName) {
		return new String[] {"drop schema " + schemaName + " restrict"};
	}

	/**
	 * Since we're using {@code seconds_between()} and
	 * {@code add_seconds()}, it makes sense to use
	 * seconds as the "native" precision.
	 */
	@Override
	public long getFractionalSecondPrecisionInNanos() {
		//Note that DB2 actually supports all the way up to
		//thousands-of-nanoseconds precision for timestamps!
		//i.e. timestamp(12)
		return 1_000_000_000; //seconds
	}

	@Override
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		StringBuilder pattern = new StringBuilder();
		boolean castFrom = fromTemporalType != TemporalType.TIMESTAMP && !unit.isDateUnit();
		boolean castTo = toTemporalType != TemporalType.TIMESTAMP && !unit.isDateUnit();
		switch (unit) {
			case NATIVE:
			case NANOSECOND:
				pattern.append("(seconds_between(");
				break;
			//note: DB2 does have weeks_between()
			case MONTH:
			case QUARTER:
				// the months_between() function results
				// in a non-integral value, so trunc() it
				pattern.append("trunc(months_between(");
				break;
			default:
				pattern.append("?1s_between(");
		}
		if (castTo) {
			pattern.append("cast(?3 as timestamp)");
		}
		else {
			pattern.append("?3");
		}
		pattern.append(",");
		if (castFrom) {
			pattern.append("cast(?2 as timestamp)");
		}
		else {
			pattern.append("?2");
		}
		pattern.append(")");
		switch (unit) {
			case NATIVE:
				pattern.append("+(microsecond(?3)-microsecond(?2))/1e6)");
				break;
			case NANOSECOND:
				pattern.append("*1e9+(microsecond(?3)-microsecond(?2))*1e3)");
				break;
			case MONTH:
				pattern.append(")");
				break;
			case QUARTER:
				pattern.append("/3)");
				break;
		}
		return pattern.toString();
	}

	@Override
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType) {
		final StringBuilder pattern = new StringBuilder();
		final boolean castTo;
		if ( unit.isDateUnit() ) {
			castTo = temporalType == TemporalType.TIME;
		}
		else {
			castTo = temporalType == TemporalType.DATE;
		}
		if (castTo) {
			pattern.append("cast(?3 as timestamp)");
		}
		else {
			pattern.append("?3");
		}
		pattern.append("+(");
		// DB2 supports temporal arithmetic. See https://www.ibm.com/support/knowledgecenter/en/SSEPGG_9.7.0/com.ibm.db2.luw.sql.ref.doc/doc/r0023457.html
		switch (unit) {
			case NATIVE:
				// AFAICT the native format is seconds with fractional parts after the decimal point
				pattern.append("?2) seconds");
				break;
			case NANOSECOND:
				pattern.append("(?2)/1e9) seconds");
				break;
			case WEEK:
				pattern.append("(?2)*7) days");
				break;
			case QUARTER:
				pattern.append("(?2)*3) months");
				break;
			default:
				pattern.append("?2) ?1s");
		}
		return pattern.toString();
	}

	@Override
	public String getLowercaseFunction() {
		return getVersion() < 970 ? "lcase" : super.getLowercaseFunction();
	}

	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return getVersion() < 970
				? LegacyDB2SequenceSupport.INSTANCE
				: DB2SequenceSupport.INSTANCE;
	}

	@Override
	public String getQuerySequencesString() {
		return "select * from syscat.sequences";
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		if ( getQuerySequencesString() == null ) {
			return SequenceInformationExtractorNoOpImpl.INSTANCE;
		}
		else {
			return SequenceInformationExtractorDB2DatabaseImpl.INSTANCE;
		}
	}

	@Override
	public String getForUpdateString() {
		return FOR_UPDATE_SQL;
	}

	@Override
	public boolean supportsSkipLocked() {
		// Introduced in 11.5: https://www.ibm.com/docs/en/db2/11.5?topic=statement-concurrent-access-resolution-clause
		return getVersion() >= 1150;
	}

	@Override
	public String getForUpdateSkipLockedString() {
		return supportsSkipLocked()
				? FOR_UPDATE_SKIP_LOCKED_SQL
				: FOR_UPDATE_SQL;
	}

	@Override
	public String getForUpdateSkipLockedString(String aliases) {
		return getForUpdateSkipLockedString();
	}

	@Override
	public String getWriteLockString(int timeout) {
		return timeout == LockOptions.SKIP_LOCKED && supportsSkipLocked()
				? FOR_UPDATE_SKIP_LOCKED_SQL
				: FOR_UPDATE_SQL;
	}

	@Override
	public String getReadLockString(int timeout) {
		return timeout == LockOptions.SKIP_LOCKED && supportsSkipLocked()
				? FOR_SHARE_SKIP_LOCKED_SQL
				: FOR_SHARE_SQL;
	}

	@Override
	public boolean supportsOuterJoinForUpdate() {
		return false;
	}

	@Override
	public boolean supportsExistsInSelect() {
		return false;
	}

	@Override
	public boolean supportsLockTimeouts() {
		//as far as I know, DB2 doesn't support this
		return false;
	}

	@Override
	public String getSelectClauseNullString(int sqlType) {
		return selectNullString(sqlType);
	}

	static String selectNullString(int sqlType) {
		String literal;
		switch ( sqlType ) {
			case Types.VARCHAR:
			case Types.CHAR:
				literal = "''";
				break;
			case Types.DATE:
				literal = "'2000-1-1'";
				break;
			case Types.TIME:
				literal = "'00:00:00'";
				break;
			case Types.TIMESTAMP:
			case Types.TIMESTAMP_WITH_TIMEZONE:
				literal = "'2000-1-1 00:00:00'";
				break;
			default:
				literal = "0";
		}
		return "nullif(" + literal + ", " + literal + ')';
	}

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		return col;
	}

	@Override
	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
		boolean isResultSet = ps.execute();
		// This assumes you will want to ignore any update counts
		while ( !isResultSet && ps.getUpdateCount() != -1 ) {
			isResultSet = ps.getMoreResults();
		}

		return ps.getResultSet();
	}

	@Override
	public boolean supportsCommentOn() {
		return true;
	}

	@Override
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new CteStrategy( rootEntityDescriptor, runtimeModelCreationContext );
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "values current timestamp";
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * NOTE : DB2 is known to support parameters in the <tt>SELECT</tt> clause, but only in casted form
	 * (see {@link #requiresCastingOfParametersInSelectClause()}).
	 */
	@Override
	public boolean supportsParametersInInsertSelect() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * DB2 in fact does require that parameters appearing in the select clause be wrapped in cast() calls
	 * to tell the DB parser the type of the select value.
	 */
	@Override
	public boolean requiresCastingOfParametersInSelectClause() {
		return true;
	}

	@Override
	public boolean supportsResultSetPositionQueryMethodsOnForwardOnlyCursor() {
		return false;
	}

	@Override
	public String getCrossJoinSeparator() {
		//DB2 v9.1 doesn't support 'cross join' syntax
		//DB2 9.7 and later support "cross join"
		return getVersion() < 970 ? ", " : super.getCrossJoinSeparator();
	}


	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsEmptyInList() {
		return false;
	}

	@Override
	public boolean supportsLobValueChangePropagation() {
		return false;
	}

	@Override
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		return true;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		return false;
	}

	@Override
	public String getFromDual() {
		return "from sysibm.dual";
	}

	@Override
	public boolean supportsSelectQueryWithoutFromClause() {
		return false;
	}

	@Override
	protected JdbcTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {
		final int version = getVersion();

		if ( version < 1100 && sqlCode == Types.BOOLEAN ) {
			return SmallIntTypeDescriptor.INSTANCE;
		}
		else if ( version < 1100 && sqlCode == Types.VARBINARY ) {
			// Binary literals were only added in 11. See https://www.ibm.com/support/knowledgecenter/SSEPGG_11.1.0/com.ibm.db2.luw.sql.ref.doc/doc/r0000731.html#d79816e393
			return VarbinaryTypeDescriptor.INSTANCE_WITHOUT_LITERALS;
		}
		else if ( version < 970 ) {
			return sqlCode == Types.NUMERIC
					? DecimalTypeDescriptor.INSTANCE
					: super.getSqlTypeDescriptorOverride(sqlCode);
		}
		else {
			// See HHH-12753
			// It seems that DB2's JDBC 4.0 support as of 9.5 does not
			// support the N-variant methods like NClob or NString.
			// Therefore here we overwrite the sql type descriptors to
			// use the non-N variants which are supported.
			switch ( sqlCode ) {
				case Types.NCHAR:
					return CharTypeDescriptor.INSTANCE;
				case Types.NCLOB:
					return useInputStreamToInsertBlob()
							? ClobTypeDescriptor.STREAM_BINDING
							: ClobTypeDescriptor.CLOB_BINDING;
				case Types.NVARCHAR:
					return VarcharTypeDescriptor.INSTANCE;
				case Types.NUMERIC:
					return DecimalTypeDescriptor.INSTANCE;
				default:
					return super.getSqlTypeDescriptorOverride(sqlCode);
			}
		}
	}

	@Override
	public String formatBinaryLiteral(byte[] bytes) {
		return "BX'" + StandardBasicTypes.BINARY.toString( bytes ) + "'";
	}

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			final String sqlState = JdbcExceptionHelper.extractSqlState( sqlException );
			final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );

			if ( -952 == errorCode && "57014".equals( sqlState ) ) {
				throw new LockTimeoutException( message, sqlException, sql );
			}
			return null;
		};
	}

	@Override
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	@Override
	public String getNotExpression( String expression ) {
		return "not (" + expression + ")";
	}

	@Override
	public LimitHandler getLimitHandler() {
		return limitHandler;
	}

	@Override
	public boolean supportsNullPrecedence() {
		return false;
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new DB2SqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	/**
	 * Handle DB2 "support" for null precedence...
	 *
	 * @param expression The SQL order expression. In case of {@code @OrderBy} annotation user receives property placeholder
	 * (e.g. attribute name enclosed in '{' and '}' signs).
	 * @param collation Collation string in format {@code collate IDENTIFIER}, or {@code null}
	 * if expression has not been explicitly specified.
	 * @param order Order direction. Possible values: {@code asc}, {@code desc}, or {@code null}
	 * if expression has not been explicitly specified.
	 * @param nullPrecedence Nulls precedence. Default value: {@link NullPrecedence#NONE}.
	 *
	 * @return SQL string.
	 */
	@Override
	public String renderOrderByElement(String expression, String collation, String order, NullPrecedence nullPrecedence) {
		if ( nullPrecedence == null || nullPrecedence == NullPrecedence.NONE ) {
			return super.renderOrderByElement( expression, collation, order, NullPrecedence.NONE );
		}

		// DB2 FTW!  A null precedence was explicitly requested, but DB2 "support" for null precedence
		// is a joke.  Basically it supports combos that align with what it does anyway.  Here is the
		// support matrix:
		//		* ASC + NULLS FIRST -> case statement
		//		* ASC + NULLS LAST -> just drop the NULLS LAST from sql fragment
		//		* DESC + NULLS FIRST -> just drop the NULLS FIRST from sql fragment
		//		* DESC + NULLS LAST -> case statement

		if ( ( nullPrecedence == NullPrecedence.FIRST  && "desc".equalsIgnoreCase( order ) )
				|| ( nullPrecedence == NullPrecedence.LAST && "asc".equalsIgnoreCase( order ) ) ) {
			// we have one of:
			//		* ASC + NULLS LAST
			//		* DESC + NULLS FIRST
			// so just drop the null precedence.  *NOTE*: we could pass along the null precedence here,
			// but only DB2 9.7 or greater understand it; dropping it is more portable across DB2 versions
			return super.renderOrderByElement( expression, collation, order, NullPrecedence.NONE );
		}

		return String.format(
				Locale.ENGLISH,
				"case when %s is null then %s else %s end, %s %s",
				expression,
				nullPrecedence == NullPrecedence.FIRST ? "0" : "1",
				nullPrecedence == NullPrecedence.FIRST ? "1" : "0",
				expression,
				order == null ? "asc" : order
		);
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new DB2IdentityColumnSupport();
	}

	@Override
	public boolean supportsValuesList() {
		return true;
	}

	@Override
	public boolean supportsRowValueConstructorSyntaxInInList() {
		return false;
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
	public boolean supportsOffsetInSubquery() {
		return true;
	}

	@Override
	public boolean supportsWindowFunctions() {
		return true;
	}

	@Override
	public GroupBySummarizationRenderingStrategy getGroupBySummarizationRenderingStrategy() {
		return GroupBySummarizationRenderingStrategy.FUNCTION;
	}

	@Override
	public GroupByConstantRenderingStrategy getGroupByConstantRenderingStrategy() {
		return GroupByConstantRenderingStrategy.EMPTY_GROUPING;
	}

	@Override
	public String translateDatetimeFormat(String format) {
		//DB2 does not need nor support FM
		return OracleDialect.datetimeFormat( format, false, false ).result();
	}

	@Override
	public String translateExtractField(TemporalUnit unit) {
		switch ( unit ) {
			//WEEK means the ISO week number on DB2
			case DAY_OF_MONTH: return "day";
			case DAY_OF_YEAR: return "doy";
			case DAY_OF_WEEK: return "dow";
			default: return super.translateExtractField( unit );
		}
	}

	@Override
	public String toBooleanValueString(boolean bool) {
		if ( getVersion() < 1100 ) {
			return bool ? "1" : "0";
		}
		else {
			return bool ? "true" : "false";
		}
	}

	@Override
	public String extractPattern(TemporalUnit unit) {
		if ( unit == TemporalUnit.WEEK ) {
			// Not sure why, but `extract(week from '2019-05-27')` wrongly returns 21 and week_iso behaves correct
			return "week_iso(?2)";
		}
		else {
			return super.extractPattern( unit );
		}
	}

}
