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
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.query.sqm.consume.multitable.internal.StandardIdTableSupport;
import org.hibernate.query.sqm.consume.multitable.spi.IdTableStrategy;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.GlobalTempTableExporter;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.GlobalTemporaryTableStrategy;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.IdTable;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.IdTableSupport;
import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.spi.AnsiTrimFunctionTemplate;
import org.hibernate.query.sqm.produce.function.spi.ConcatFunctionTemplate;
import org.hibernate.query.sqm.produce.function.spi.StandardAnsiSqlSqmAggregationFunctionTemplates.AvgFunctionTemplate;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorDB2DatabaseImpl;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.descriptor.sql.spi.DecimalSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.SmallIntSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.StandardSpiBasicTypes;

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
		// DB2 converts numeric to decimal under the hood
		// Note that the type returned by DB2 for a numeric column will be Types.DECIMAL. Thus, we have an issue when
		// comparing the types during the schema validation, defining the type to decimal here as the type names will
		// also be compared and there will be a match. See HHH-12827 for the details.
		registerColumnType( Types.NUMERIC, "decimal($p,$s)" );
		registerColumnType( Types.DECIMAL, "decimal($p,$s)" );
		registerColumnType( Types.BLOB, "blob($l)" );
		registerColumnType( Types.CLOB, "clob($l)" );
		registerColumnType( Types.LONGVARCHAR, "long varchar" );
		registerColumnType( Types.LONGVARBINARY, "long varchar for bit data" );
		registerColumnType( Types.BINARY, "varchar($l) for bit data" );
		registerColumnType( Types.BINARY, 254, "char($l) for bit data" );
		registerColumnType( Types.BOOLEAN, "smallint" );

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
	public void initializeFunctionRegistry(SqmFunctionRegistry registry) {
		super.initializeFunctionRegistry( registry );

		registry.register( "avg", new AvgFunctionTemplate( "double" ) );

		CommonFunctionFactory.abs( registry );
		CommonFunctionFactory.sign( registry );
		CommonFunctionFactory.ceiling( registry );
		CommonFunctionFactory.ceil( registry );
		CommonFunctionFactory.floor( registry );
		CommonFunctionFactory.round( registry );
		CommonFunctionFactory.sqrt( registry );

		registry.namedTemplateBuilder( "absval" )
				.setExactArgumentCount( 1 )
				.register();

		CommonFunctionFactory.acos( registry );
		CommonFunctionFactory.asin( registry );
		CommonFunctionFactory.atan( registry );
		CommonFunctionFactory.cos( registry );
		CommonFunctionFactory.cot( registry );
		CommonFunctionFactory.degrees( registry );
		CommonFunctionFactory.exp( registry );
		CommonFunctionFactory.ln( registry );
		CommonFunctionFactory.log( registry );
		CommonFunctionFactory.log10( registry );
		CommonFunctionFactory.radians( registry );
		CommonFunctionFactory.sin( registry );
		CommonFunctionFactory.radians( registry );
		CommonFunctionFactory.tan( registry );

		registry.namedTemplateBuilder( "float" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "hex" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
		CommonFunctionFactory.rand( registry );

		CommonFunctionFactory.soundex( registry );

		CommonFunctionFactory.stddev( registry );
		CommonFunctionFactory.variance( registry );

		registry.namedTemplateBuilder( "julian_day" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "microsecond" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "midnight_seconds" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "minute" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "month" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "monthname" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "quarter" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "hour" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "second" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setArgumentCountBetween( 1, 2 )
				.register();
		registry.registerNoArgs( "current_date", StandardSpiBasicTypes.DATE );
		registry.namedTemplateBuilder( "date" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "day" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "dayname" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "dayofweek" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "dayofweek_iso" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "dayofyear" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "days" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();
		registry.registerNoArgs( "current_time", StandardSpiBasicTypes.TIME );
		registry.namedTemplateBuilder( "time" )
				.setInvariantType( StandardSpiBasicTypes.TIME )
				.setExactArgumentCount( 1 )
				.register();
		registry.registerNoArgs( "current_timestamp", StandardSpiBasicTypes.TIMESTAMP );
		registry.namedTemplateBuilder( "timestamp" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setArgumentCountBetween( 1, 2 )
				.register();
		registry.namedTemplateBuilder( "timestamp_iso" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "week" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "week_iso" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "year" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();

		registry.namedTemplateBuilder( "double" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "varchar" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentsValidator( StandardArgumentsValidators.min( 1 ) )
				.register();
		registry.namedTemplateBuilder( "real" )
				.setInvariantType( StandardSpiBasicTypes.FLOAT )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "bigint" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "char" )
				.setArgumentsValidator( StandardArgumentsValidators.min( 1 ) )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "integer" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "smallint" )
				.setInvariantType( StandardSpiBasicTypes.SHORT )
				.setExactArgumentCount( 1 )
				.register();

		registry.namedTemplateBuilder( "digits" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "chr" )
				.setInvariantType( StandardSpiBasicTypes.CHARACTER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "upper" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 1, 3 )
				.register();
		registry.namedTemplateBuilder( "lower" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 1, 3 )
				.register();
		registry.registerAlternateKey( "ucase", "upper" );
		registry.registerAlternateKey( "lcase", "lower" );
		registry.namedTemplateBuilder( "ltrim" )
				.setArgumentCountBetween( 1, 2 )
				.register();
		registry.namedTemplateBuilder( "rtrim" )
				.setArgumentCountBetween( 1, 2 )
				.register();
		registry.namedTemplateBuilder( "substr" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 1, 3 )
				.register();
		registry.namedTemplateBuilder( "posstr" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 2 )
				.register();

		registry.namedTemplateBuilder( "substring", "substr" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 2, 4 )
				.register();
		registry.registerPattern( "bit_length", "length(?1)*8", StandardSpiBasicTypes.INTEGER );
		registry.register( "trim", AnsiTrimFunctionTemplate.INSTANCE );

		registry.register( "concat", ConcatFunctionTemplate.INSTANCE );

		registry.registerPattern( "str", "rtrim(char(?1))", StandardSpiBasicTypes.STRING );
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
			case Types.TIMESTAMP:
				literal = "'2000-1-1 00:00:00'";
				break;
			case Types.TIME:
				literal = "'00:00:00'";
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
	public IdTableStrategy getDefaultIdTableStrategy() {
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
			protected String determineIdTableName(String baseName) {
				return "session." + super.determineIdTableName( baseName );
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
		if ( sqlCode == Types.BOOLEAN ) {
			return SmallIntSqlDescriptor.INSTANCE;
		}
		else if ( sqlCode == Types.NUMERIC ) {
			return DecimalSqlDescriptor.INSTANCE;
		}

		return super.getSqlTypeDescriptorOverride( sqlCode );
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
