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
import org.hibernate.dialect.function.AvgWithArgumentCastFunction;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.dialect.identity.DB2IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.dialect.unique.DB2UniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.hql.spi.id.IdTableSupportStandardImpl;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.global.GlobalTemporaryTableBulkIdStrategy;
import org.hibernate.hql.spi.id.local.AfterUseAction;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.type.spi.StandardSpiBasicTypes;
import org.hibernate.type.descriptor.sql.spi.SmallIntSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * An SQL dialect for DB2.
 *
 * @author Gavin King
 */
public class DB2Dialect extends Dialect {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( DB2Dialect.class );

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

	/**
	 * Constructs a DB2Dialect
	 */
	public DB2Dialect() {
		super();
		registerColumnType( Types.BIT, "smallint" );
		registerColumnType( Types.BIGINT, "bigint" );
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.TINYINT, "smallint" );
		registerColumnType( Types.INTEGER, "integer" );
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.DOUBLE, "double" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.VARBINARY, "varchar($l) for bit data" );
		registerColumnType( Types.NUMERIC, "numeric($p,$s)" );
		registerColumnType( Types.BLOB, "blob($l)" );
		registerColumnType( Types.CLOB, "clob($l)" );
		registerColumnType( Types.LONGVARCHAR, "long varchar" );
		registerColumnType( Types.LONGVARBINARY, "long varchar for bit data" );
		registerColumnType( Types.BINARY, "varchar($l) for bit data" );
		registerColumnType( Types.BINARY, 254, "char($l) for bit data" );
		registerColumnType( Types.BOOLEAN, "smallint" );

		registerFunction( "avg", new AvgWithArgumentCastFunction( "double" ) );

		registerFunction( "abs", new StandardSQLFunction( "abs" ) );
		registerFunction( "absval", new StandardSQLFunction( "absval" ) );
		registerFunction( "sign", new StandardSQLFunction( "sign", StandardSpiBasicTypes.INTEGER ) );

		registerFunction( "ceiling", new StandardSQLFunction( "ceiling" ) );
		registerFunction( "ceil", new StandardSQLFunction( "ceil" ) );
		registerFunction( "floor", new StandardSQLFunction( "floor" ) );
		registerFunction( "round", new StandardSQLFunction( "round" ) );

		registerFunction( "acos", new StandardSQLFunction( "acos", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "asin", new StandardSQLFunction( "asin", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "atan", new StandardSQLFunction( "atan", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "cos", new StandardSQLFunction( "cos", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "cot", new StandardSQLFunction( "cot", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "degrees", new StandardSQLFunction( "degrees", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "exp", new StandardSQLFunction( "exp", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "float", new StandardSQLFunction( "float", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "hex", new StandardSQLFunction( "hex", StandardSpiBasicTypes.STRING ) );
		registerFunction( "ln", new StandardSQLFunction( "ln", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "log", new StandardSQLFunction( "log", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "log10", new StandardSQLFunction( "log10", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "radians", new StandardSQLFunction( "radians", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "rand", new NoArgSQLFunction( "rand", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "sin", new StandardSQLFunction( "sin", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "soundex", new StandardSQLFunction( "soundex", StandardSpiBasicTypes.STRING ) );
		registerFunction( "sqrt", new StandardSQLFunction( "sqrt", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "stddev", new StandardSQLFunction( "stddev", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "tan", new StandardSQLFunction( "tan", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "variance", new StandardSQLFunction( "variance", StandardSpiBasicTypes.DOUBLE ) );

		registerFunction( "julian_day", new StandardSQLFunction( "julian_day", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "microsecond", new StandardSQLFunction( "microsecond", StandardSpiBasicTypes.INTEGER ) );
		registerFunction(
				"midnight_seconds",
				new StandardSQLFunction( "midnight_seconds", StandardSpiBasicTypes.INTEGER )
		);
		registerFunction( "minute", new StandardSQLFunction( "minute", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "month", new StandardSQLFunction( "month", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "monthname", new StandardSQLFunction( "monthname", StandardSpiBasicTypes.STRING ) );
		registerFunction( "quarter", new StandardSQLFunction( "quarter", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "hour", new StandardSQLFunction( "hour", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "second", new StandardSQLFunction( "second", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "current_date", new NoArgSQLFunction( "current date", StandardSpiBasicTypes.DATE, false ) );
		registerFunction( "date", new StandardSQLFunction( "date", StandardSpiBasicTypes.DATE ) );
		registerFunction( "day", new StandardSQLFunction( "day", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "dayname", new StandardSQLFunction( "dayname", StandardSpiBasicTypes.STRING ) );
		registerFunction( "dayofweek", new StandardSQLFunction( "dayofweek", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "dayofweek_iso", new StandardSQLFunction( "dayofweek_iso", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "dayofyear", new StandardSQLFunction( "dayofyear", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "days", new StandardSQLFunction( "days", StandardSpiBasicTypes.LONG ) );
		registerFunction( "current_time", new NoArgSQLFunction( "current time", StandardSpiBasicTypes.TIME, false ) );
		registerFunction( "time", new StandardSQLFunction( "time", StandardSpiBasicTypes.TIME ) );
		registerFunction(
				"current_timestamp",
				new NoArgSQLFunction( "current timestamp", StandardSpiBasicTypes.TIMESTAMP, false )
		);
		registerFunction( "timestamp", new StandardSQLFunction( "timestamp", StandardSpiBasicTypes.TIMESTAMP ) );
		registerFunction( "timestamp_iso", new StandardSQLFunction( "timestamp_iso", StandardSpiBasicTypes.TIMESTAMP ) );
		registerFunction( "week", new StandardSQLFunction( "week", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "week_iso", new StandardSQLFunction( "week_iso", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "year", new StandardSQLFunction( "year", StandardSpiBasicTypes.INTEGER ) );

		registerFunction( "double", new StandardSQLFunction( "double", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "varchar", new StandardSQLFunction( "varchar", StandardSpiBasicTypes.STRING ) );
		registerFunction( "real", new StandardSQLFunction( "real", StandardSpiBasicTypes.FLOAT ) );
		registerFunction( "bigint", new StandardSQLFunction( "bigint", StandardSpiBasicTypes.LONG ) );
		registerFunction( "char", new StandardSQLFunction( "char", StandardSpiBasicTypes.CHARACTER ) );
		registerFunction( "integer", new StandardSQLFunction( "integer", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "smallint", new StandardSQLFunction( "smallint", StandardSpiBasicTypes.SHORT ) );

		registerFunction( "digits", new StandardSQLFunction( "digits", StandardSpiBasicTypes.STRING ) );
		registerFunction( "chr", new StandardSQLFunction( "chr", StandardSpiBasicTypes.CHARACTER ) );
		registerFunction( "upper", new StandardSQLFunction( "upper" ) );
		registerFunction( "lower", new StandardSQLFunction( "lower" ) );
		registerFunction( "ucase", new StandardSQLFunction( "ucase" ) );
		registerFunction( "lcase", new StandardSQLFunction( "lcase" ) );
		registerFunction( "ltrim", new StandardSQLFunction( "ltrim" ) );
		registerFunction( "rtrim", new StandardSQLFunction( "rtrim" ) );
		registerFunction( "substr", new StandardSQLFunction( "substr", StandardSpiBasicTypes.STRING ) );
		registerFunction( "posstr", new StandardSQLFunction( "posstr", StandardSpiBasicTypes.INTEGER ) );

		registerFunction( "substring", new StandardSQLFunction( "substr", StandardSpiBasicTypes.STRING ) );
		registerFunction( "bit_length", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "length(?1)*8" ) );
		registerFunction( "trim", new SQLFunctionTemplate( StandardSpiBasicTypes.STRING, "trim(?1 ?2 ?3 ?4)" ) );

		registerFunction( "concat", new VarArgsSQLFunction( StandardSpiBasicTypes.STRING, "", "||", "" ) );

		registerFunction( "str", new SQLFunctionTemplate( StandardSpiBasicTypes.STRING, "rtrim(char(?1))" ) );

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
	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName + " restrict";
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
		return "select seqname from sysibm.syssequences";
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
			case Types.TIMESTAMP:
				literal = "'2000-1-1 00:00:00'";
				break;
			case Types.TIME:
				literal = "'00:00:00'";
				break;
			default:
				literal = "0";
		}
		return "nullif(" + literal + ',' + literal + ')';
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
	public MultiTableBulkIdStrategy getDefaultMultiTableBulkIdStrategy() {
		return new GlobalTemporaryTableBulkIdStrategy(
				new IdTableSupportStandardImpl() {
					@Override
					public String generateIdTableName(String baseName) {
						return "session." + super.generateIdTableName( baseName );
					}

					@Override
					public String getCreateIdTableCommand() {
						return "declare global temporary table";
					}

					@Override
					public String getCreateIdTableStatementOptions() {
						return "not logged";
					}
				},
				AfterUseAction.CLEAN
		);
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
		return ", ";
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
	protected SqlTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {
		return sqlCode == Types.BOOLEAN ? SmallIntSqlDescriptor.INSTANCE : super.getSqlTypeDescriptorOverride( sqlCode );
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
	 * @return
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
}
