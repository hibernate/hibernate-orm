/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;

import org.hibernate.JDBCException;
import org.hibernate.MappingException;
import org.hibernate.NullPrecedence;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.DB2FormatEmulation;
import org.hibernate.dialect.identity.DB2IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.dialect.unique.DB2UniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.naming.Identifier;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.mutation.spi.SqmMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.idtable.GlobalTempTableExporter;
import org.hibernate.query.sqm.mutation.spi.idtable.GlobalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.spi.idtable.IdTable;
import org.hibernate.query.sqm.mutation.spi.idtable.IdTableSupport;
import org.hibernate.query.sqm.mutation.spi.idtable.StandardIdTableSupport;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorDB2DatabaseImpl;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.descriptor.sql.spi.CharSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.ClobSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.DecimalSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.VarcharSqlDescriptor;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * An SQL dialect for DB2.
 *
 * @author Gavin King
 */
public class DB2Dialect extends Dialect {

	private final int version;

	int getVersion() {
		return version;
	}

	private static final AbstractLimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {
		@Override
		public String processSql(String sql, RowSelection selection) {
			if (LimitHelper.hasFirstRow( selection )) {
				//nest the main query in an outer select
				return "select * from ( select inner2_.*, rownumber() over(order by order of inner2_) as rownumber_ from ( "
						+ sql + " fetch first " + getMaxOrLimit( selection ) + " rows only ) as inner2_ ) as inner1_ where rownumber_ > "
						+ selection.getFirstRow() + " order by rownumber_";
			}
			return sql + " fetch first " + getMaxOrLimit( selection ) +  " rows only";
		}

		@Override
		public boolean supportsLimit() {
			return true;
		}

		@Override
		public boolean useMaxForLimit() {
			return true;
		}

		@Override
		public boolean supportsVariableLimit() {
			return false;
		}
	};

	private final UniqueDelegate uniqueDelegate;

	public DB2Dialect(DialectResolutionInfo info) {
		this( info.getDatabaseMajorVersion() * 100 + info.getDatabaseMinorVersion() * 10 );
	}

	public DB2Dialect() {
		this(900);
	}

	/**
	 * Constructs a DB2Dialect
	 */
	public DB2Dialect(int version) {
		super();
		this.version = version;

		registerColumnType( Types.BIT, 1, "boolean" ); //no bit
		registerColumnType( Types.BIT, "smallint" ); //no bit
		registerColumnType( Types.TINYINT, "smallint" ); //no tinyint

		//HHH-12827: map them both to the same type to
		//           avoid problems with schema update
//		registerColumnType( Types.DECIMAL, "decimal($p,$s)" );
		registerColumnType( Types.NUMERIC, "decimal($p,$s)" );

		registerColumnType( Types.BINARY, "varchar($l) for bit data" ); //should use 'binary' since version 11
		registerColumnType( Types.BINARY, 254, "char($l) for bit data" ); //should use 'binary' since version 11
		registerColumnType( Types.VARBINARY, "varchar($l) for bit data" ); //should use 'varbinary' since version 11
		registerColumnType( Types.LONGVARBINARY, "varchar($l) for bit data" ); //'long varchar' deprecated since at least version 9.8!

		registerColumnType( Types.BLOB, "blob($l)" );
		registerColumnType( Types.CLOB, "clob($l)" );

		registerColumnType( Types.TIMESTAMP_WITH_TIMEZONE, "timestamp($p)" );

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

		uniqueDelegate = new DB2UniqueDelegate( this );
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return Types.BOOLEAN;
	}

	public int getDefaultDecimalPrecision() {
		//this is the maximum allowed in DB2
		return 31;
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
		CommonFunctionFactory.variance( queryEngine );
		CommonFunctionFactory.stdevVarianceSamp( queryEngine );
		CommonFunctionFactory.addYearsMonthsDaysHoursMinutesSeconds( queryEngine );
		CommonFunctionFactory.yearsMonthsDaysHoursMinutesSecondsBetween( queryEngine );
		CommonFunctionFactory.dateTrunc( queryEngine );

		queryEngine.getSqmFunctionRegistry().register( "format", new DB2FormatEmulation() );

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "posstr" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature("(string, pattern)")
				.register();

	}

	@Override
	public void timestampdiff(TemporalUnit unit, Renderer from, Renderer to, Appender sqlAppender, boolean fromTimestamp, boolean toTimestamp) {
		boolean castFrom = !fromTimestamp && !unit.isDateUnit();
		boolean castTo = !toTimestamp && !unit.isDateUnit();
		switch (unit) {
			case NANOSECOND:
				sqlAppender.append("(second");
				break;
			//note: DB2 does have weeks_between()
			case MONTH:
			case QUARTER:
				// the months_between() function results
				// in a non-integral value, so trunc() it
				sqlAppender.append("trunc(month");
				break;
			default:
				sqlAppender.append( unit.toString() );
		}
		sqlAppender.append("s_between(");
		if (castTo) {
			sqlAppender.append("cast(");
		}
		to.render();
		if (castTo) {
			sqlAppender.append(" as timestamp)");
		}
		sqlAppender.append(",");
		if (castFrom) {
			sqlAppender.append("cast(");
		}
		from.render();
		if (castFrom) {
			sqlAppender.append(" as timestamp)");
		}
		sqlAppender.append(")");
		switch (unit) {
			case NANOSECOND:
				sqlAppender.append("*1e9+(microsecond(");
				to.render();
				sqlAppender.append(")-microsecond(");
				from.render();
				sqlAppender.append("))*1e3)");
				break;
			case MONTH:
				sqlAppender.append(")");
				break;
			case QUARTER:
				sqlAppender.append("/3)");
				break;
		}
	}

	@Override
	public void timestampadd(TemporalUnit unit, Renderer magnitude, Renderer to, Appender sqlAppender, boolean timestamp) {
		boolean castTo = !timestamp && !unit.isDateUnit();
		sqlAppender.append("add_");
		switch (unit) {
			case NANOSECOND:
				sqlAppender.append("second");
				break;
			case WEEK:
				//note: DB2 does not have add_weeks()
				sqlAppender.append("day");
				break;
			case QUARTER:
				sqlAppender.append("month");
				break;
			default:
				sqlAppender.append( unit.toString() );
		}
		sqlAppender.append("s(");
		if (castTo) {
			sqlAppender.append("cast(");
		}
		to.render();
		if (castTo) {
			sqlAppender.append(" as timestamp)");
		}
		sqlAppender.append(",");
		switch (unit) {
			case NANOSECOND:
			case WEEK:
			case QUARTER:
				sqlAppender.append("(");
				break;
		}
		magnitude.render();
		switch (unit) {
			case NANOSECOND:
				sqlAppender.append(")/1e9");
				break;
			case WEEK:
				sqlAppender.append(")*7");
				break;
			case QUARTER:
				sqlAppender.append(")*3");
				break;
		}
		sqlAppender.append(")");
	}

	@Override
	public String getLowercaseFunction() {
		return "lcase";
	}

	@Override
	public String getAddColumnString() {
		return "add column";
	}

	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "values nextval for " + sequenceName;
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) throws MappingException {
		return "next value for " + sequenceName;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return super.getDropSequenceString( sequenceName ) + " restrict";
	}

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public boolean supportsPooledSequences() {
		return true;
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
	@SuppressWarnings("deprecation")
	public boolean supportsLimit() {
		return true;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean supportsVariableLimit() {
		return false;
	}

	@Override
	@SuppressWarnings("deprecation")
	public String getLimitString(String sql, int offset, int limit) {
		if ( offset == 0 ) {
			return sql + " fetch first " + limit + " rows only";
		}
		//nest the main query in an outer select
		return "select * from ( select inner2_.*, rownumber() over(order by order of inner2_) as rownumber_ from ( "
				+ sql + " fetch first " + limit + " rows only ) as inner2_ ) as inner1_ where rownumber_ > "
				+ offset + " order by rownumber_";
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 *
	 * DB2 does have a one-based offset, however this was actually already handled in the limit string building
	 * (the '?+1' bit).  To not mess up inheritors, I'll leave that part alone and not touch the offset here.
	 */
	@Override
	@SuppressWarnings("deprecation")
	public int convertToFirstRowValue(int zeroBasedFirstResult) {
		return zeroBasedFirstResult;
	}

	@Override
	@SuppressWarnings("deprecation")
	public String getForUpdateString() {
		return " for read only with rs use and keep update locks";
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean useMaxForLimit() {
		return true;
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
		String literal;
		switch ( sqlType ) {
			case Types.VARCHAR:
			case Types.CHAR:
				literal = "'x'";
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
	public boolean supportsUnionAll() {
		return true;
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
	public SqmMutationStrategy getDefaultIdTableStrategy() {
		// Starting in DB2 9.7, "real" global temporary tables that can be shared between sessions
		// are supported; (obviously) data is not shared between sessions.
		return new GlobalTemporaryTableStrategy( generateIdTableSupport() );
	}

	protected IdTableSupport generateIdTableSupport() {
		// todo (6.0) : come back and verify this - different on 5.3, specifically dropping after each use
		//
		// Prior to DB2 9.7, "real" global temporary tables that can be shared between sessions
		// are *not* supported; even though the DB2 command says to declare a "global" temp table
		// Hibernate treats it as a "local" temp table.

		return new StandardIdTableSupport( new GlobalTempTableExporter() ) {
			@Override
			protected Identifier determineIdTableName(Identifier baseName) {
				Identifier identifier = super.determineIdTableName(baseName);
				return getVersion() < 970
						? new Identifier( "session." + identifier.getText(), false )
						: identifier;
			}
			@Override
			public Exporter<IdTable> getIdTableExporter() {
				return generateIdTableExporter();
			}
		};
	}

	protected Exporter<IdTable> generateIdTableExporter() {
		return new GlobalTempTableExporter() {
			@Override
			protected String getCreateOptions() {
				return "not logged";
			}
		};
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
	 * NOTE : DB2 is know to support parameters in the <tt>SELECT</tt> clause, but only in casted form
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
		// DB2 9.7 and later support "cross join"
		return getVersion() < 970 ? ", " : " cross join ";
	}


	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsEmptyInList() {
		return false;
	}

	@Override
	public boolean supportsLobValueChangePropogation() {
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
	protected SqlTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {
		if ( getVersion() < 970 ) {
			return sqlCode == Types.NUMERIC
					? DecimalSqlDescriptor.INSTANCE
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
					return CharSqlDescriptor.INSTANCE;
				case Types.NCLOB:
					return useInputStreamToInsertBlob()
							? ClobSqlDescriptor.STREAM_BINDING
							: ClobSqlDescriptor.CLOB_BINDING;
				case Types.NVARCHAR:
					return VarcharSqlDescriptor.INSTANCE;
				case Types.NUMERIC:
					return DecimalSqlDescriptor.INSTANCE;
				default:
					return super.getSqlTypeDescriptorOverride(sqlCode);
			}
		}
	}

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return new SQLExceptionConversionDelegate() {
			@Override
			public JDBCException convert(SQLException sqlException, String message, String sql) {
				final String sqlState = JdbcExceptionHelper.extractSqlState( sqlException );
				final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );

				if( -952 == errorCode && "57014".equals( sqlState )){
					throw new LockTimeoutException( message, sqlException, sql );
				}
				return null;
			}
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
		return LIMIT_HANDLER;
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
			// so just drop the null precedence.  *NOTE: we could pass along the null precedence here,
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
				order
		);
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new DB2IdentityColumnSupport();
	}

	@Override
	public boolean supportsPartitionBy() {
		return true;
	}

	@Override
	public String translateDatetimeFormat(String format) {
		//DB2 does not need nor support FM
		return OracleDialect.datetimeFormat( format, false ).result();
	}

	@Override
	public String translateExtractField(TemporalUnit unit) {
		switch ( unit ) {
			//WEEK means the ISO week number on DB2
			case DAY_OF_MONTH: return "day";
			case DAY_OF_YEAR: return "doy";
			case DAY_OF_WEEK: return "dow";
			default: return unit.toString();
		}
	}

}
