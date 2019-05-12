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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.JDBCException;
import org.hibernate.QueryTimeoutException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.NvlCoalesceEmulation;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.naming.Identifier;
import org.hibernate.procedure.internal.StandardCallableStatementSupport;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.mutation.spi.idtable.StandardIdTableSupport;
import org.hibernate.query.sqm.mutation.spi.SqmMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.idtable.GlobalTempTableExporter;
import org.hibernate.query.sqm.mutation.spi.idtable.GlobalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.spi.idtable.IdTable;
import org.hibernate.query.sqm.mutation.spi.idtable.IdTableSupport;
import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.DecodeCaseFragment;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.OracleJoinFragment;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorOracleDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.descriptor.sql.spi.BitSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * A dialect for Oracle 8i.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("deprecation")
public class Oracle8iDialect extends Dialect {

	private static final Pattern DISTINCT_KEYWORD_PATTERN = Pattern.compile( "\\bdistinct\\b" );

	private static final Pattern GROUP_BY_KEYWORD_PATTERN = Pattern.compile( "\\bgroup\\sby\\b" );

	private static final Pattern ORDER_BY_KEYWORD_PATTERN = Pattern.compile( "\\border\\sby\\b" );

	private static final Pattern UNION_KEYWORD_PATTERN = Pattern.compile( "\\bunion\\b" );

	private static final Pattern SQL_STATEMENT_TYPE_PATTERN = Pattern.compile("^(?:\\/\\*.*?\\*\\/)?\\s*(select|insert|update|delete)\\s+.*?");

	private static final AbstractLimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {
		@Override
		public String processSql(String sql, RowSelection selection) {
			final boolean hasOffset = LimitHelper.hasFirstRow( selection );
			sql = sql.trim();
			boolean isForUpdate = false;
			if (sql.toLowerCase(Locale.ROOT).endsWith( " for update" )) {
				sql = sql.substring( 0, sql.length() - 11 );
				isForUpdate = true;
			}

			final StringBuilder pagingSelect = new StringBuilder( sql.length() + 100 );
			if (hasOffset) {
				pagingSelect.append( "select * from ( select row_.*, rownum rownum_ from ( " );
			}
			else {
				pagingSelect.append( "select * from ( " );
			}
			pagingSelect.append( sql );
			if (hasOffset) {
				pagingSelect.append( " ) row_ ) where rownum_ <= ? and rownum_ > ?" );
			}
			else {
				pagingSelect.append( " ) where rownum <= ?" );
			}

			if (isForUpdate) {
				pagingSelect.append( " for update" );
			}

			return pagingSelect.toString();
		}

		@Override
		public boolean supportsLimit() {
			return true;
		}

		@Override
		public boolean bindLimitParametersInReverseOrder() {
			return true;
		}

		@Override
		public boolean useMaxForLimit() {
			return true;
		}
	};

	private static final int PARAM_LIST_SIZE_LIMIT = 1000;

	/**
	 * Constructs a Oracle8iDialect
	 */
	public Oracle8iDialect() {
		super();
		registerCharacterTypeMappings();
		registerNumericTypeMappings();
		registerDateTimeTypeMappings();
		registerBinaryTypeMappings();
		registerReverseHibernateTypeMappings();
		registerDefaultProperties();
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		CommonFunctionFactory.cosh( queryEngine );
		CommonFunctionFactory.sinh( queryEngine );
		CommonFunctionFactory.tanh( queryEngine );
		CommonFunctionFactory.trunc( queryEngine );
		CommonFunctionFactory.log( queryEngine );
		CommonFunctionFactory.soundex( queryEngine );
		CommonFunctionFactory.trim2( queryEngine );
		CommonFunctionFactory.pad( queryEngine );
		CommonFunctionFactory.initcap( queryEngine );
		CommonFunctionFactory.instr( queryEngine );
		CommonFunctionFactory.substr( queryEngine );
		CommonFunctionFactory.substring_substr( queryEngine );
		CommonFunctionFactory.leftRight_substr( queryEngine );
		CommonFunctionFactory.translate( queryEngine );
		CommonFunctionFactory.bitand( queryEngine );
		CommonFunctionFactory.lastDay( queryEngine );
		CommonFunctionFactory.toCharNumberDateTimestamp( queryEngine );
		CommonFunctionFactory.ceiling_ceil( queryEngine );
		CommonFunctionFactory.concat_pipeOperator( queryEngine );
		CommonFunctionFactory.rownumRowid( queryEngine );
		CommonFunctionFactory.sysdate( queryEngine );
		CommonFunctionFactory.systimestamp( queryEngine );
		CommonFunctionFactory.characterLength_length( queryEngine );

		CommonFunctionFactory.median( queryEngine );
		CommonFunctionFactory.stddev( queryEngine );
		CommonFunctionFactory.stddevPopSamp( queryEngine );
		CommonFunctionFactory.variance( queryEngine );
		CommonFunctionFactory.varPopSamp( queryEngine );

		queryEngine.getSqmFunctionRegistry().register( "coalesce", new NvlCoalesceEmulation() );

		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern("locate", StandardSpiBasicTypes.INTEGER, "instr(?2, ?1)", "instr(?2, ?1, ?3)");

	}

	protected void registerCharacterTypeMappings() {
		registerColumnType( Types.VARCHAR, 4000, "varchar2($l)" );
		registerColumnType( Types.VARCHAR, "long" );
	}

	protected void registerNumericTypeMappings() {
		registerColumnType( Types.BIT, 1, "number(1,0)" );
		registerColumnType( Types.BIT, "number(3,0)" );
		registerColumnType( Types.BOOLEAN, "number(1,0)" );

		registerColumnType( Types.BIGINT, "number(19,0)" );
		registerColumnType( Types.SMALLINT, "number(5,0)" );
		registerColumnType( Types.TINYINT, "number(3,0)" );
		registerColumnType( Types.INTEGER, "number(10,0)" );

		registerColumnType( Types.NUMERIC, "number($p,$s)" );
		registerColumnType( Types.DECIMAL, "number($p,$s)" );
	}

	protected void registerDateTimeTypeMappings() {
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "date" );
		registerColumnType( Types.TIMESTAMP, "date" );
	}

	protected void registerBinaryTypeMappings() {
		registerColumnType( Types.BINARY, 2000, "raw($l)" );
		registerColumnType( Types.BINARY, "long raw" );

		registerColumnType( Types.VARBINARY, 2000, "raw($l)" );
		registerColumnType( Types.VARBINARY, "long raw" );

		registerColumnType( Types.LONGVARCHAR, "long" );
		registerColumnType( Types.LONGVARBINARY, "long raw" );
	}

	protected void registerReverseHibernateTypeMappings() {
	}

	protected void registerDefaultProperties() {
		getDefaultProperties().setProperty( Environment.USE_STREAMS_FOR_BINARY, "true" );
		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );
		// Oracle driver reports to support getGeneratedKeys(), but they only
		// support the version taking an array of the names of the columns to
		// be returned (via its RETURNING clause).  No other driver seems to
		// support this overloaded version.
		getDefaultProperties().setProperty( Environment.USE_GET_GENERATED_KEYS, "false" );
		getDefaultProperties().setProperty( Environment.BATCH_VERSIONED_DATA, "false" );
	}

	@Override
	protected SqlTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {
		return sqlCode == Types.BOOLEAN ? BitSqlDescriptor.INSTANCE : super.getSqlTypeDescriptorOverride( sqlCode );
	}


	// features which change between 8i, 9i, and 10g ~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public JoinFragment createOuterJoinFragment() {
		return new OracleJoinFragment();
	}

	@Override
	public String getCrossJoinSeparator() {
		return ", ";
	}

	/**
	 * Map case support to the Oracle DECODE function.  Oracle did not
	 * add support for CASE until 9i.
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public CaseFragment createCaseFragment() {
		return new DecodeCaseFragment();
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LIMIT_HANDLER;
	}

	@Override
	public String getLimitString(String sql, boolean hasOffset) {
		sql = sql.trim();
		boolean isForUpdate = false;
		if ( sql.toLowerCase(Locale.ROOT).endsWith( " for update" ) ) {
			sql = sql.substring( 0, sql.length()-11 );
			isForUpdate = true;
		}

		final StringBuilder pagingSelect = new StringBuilder( sql.length()+100 );
		if (hasOffset) {
			pagingSelect.append( "select * from ( select row_.*, rownum rownum_ from ( " );
		}
		else {
			pagingSelect.append( "select * from ( " );
		}
		pagingSelect.append( sql );
		if (hasOffset) {
			pagingSelect.append( " ) row_ ) where rownum_ <= ? and rownum_ > ?" );
		}
		else {
			pagingSelect.append( " ) where rownum <= ?" );
		}

		if ( isForUpdate ) {
			pagingSelect.append( " for update" );
		}

		return pagingSelect.toString();
	}

	/**
	 * Allows access to the basic {@link Dialect#getSelectClauseNullString}
	 * implementation...
	 *
	 * @param sqlType The {@link java.sql.Types} mapping type code
	 * @return The appropriate select cluse fragment
	 */
	public String getBasicSelectClauseNullString(int sqlType) {
		return super.getSelectClauseNullString( sqlType );
	}

	@Override
	public String getSelectClauseNullString(int sqlType) {
		switch(sqlType) {
			case Types.VARCHAR:
			case Types.CHAR:
				return "to_char(null)";
			case Types.DATE:
			case Types.TIMESTAMP:
			case Types.TIME:
				return "to_date(null)";
			default:
				return "to_number(null)";
		}
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "select sysdate from dual";
	}

	@Override
	public String getCurrentTimestampSQLFunctionName() {
		return "sysdate";
	}


	// features which remain constant across 8i, 9i, and 10g ~~~~~~~~~~~~~~~~~~

	@Override
	public String getAddColumnString() {
		return "add";
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "select " + getSelectSequenceNextValString( sequenceName ) + " from dual";
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return sequenceName + ".nextval";
	}

	@Override
	public String getCreateSequenceString(String sequenceName) {
		//starts with 1, implicitly
		return "create sequence " + sequenceName;
	}

	@Override
	protected String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize) {
		if ( initialValue < 0 && incrementSize > 0 ) {
			return
				String.format(
						"%s minvalue %d start with %d increment by %d",
						getCreateSequenceString( sequenceName ),
						initialValue,
						initialValue,
						incrementSize
				);
		}
		else if ( initialValue > 0 && incrementSize < 0 ) {
			return
				String.format(
						"%s maxvalue %d start with %d increment by %d",
						getCreateSequenceString( sequenceName ),
						initialValue,
						initialValue,
						incrementSize
				);
		}
		else {
			return
				String.format(
						"%s start with %d increment by  %d",
						getCreateSequenceString( sequenceName ),
						initialValue,
						incrementSize
				);
		}
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName;
	}

	@Override
	public String getCascadeConstraintsString() {
		return " cascade constraints";
	}

	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public String getForUpdateNowaitString() {
		return " for update nowait";
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
	public boolean supportsLimit() {
		return true;
	}

	@Override
	public String getForUpdateString(String aliases) {
		return getForUpdateString() + " of " + aliases;
	}

	@Override
	public String getForUpdateNowaitString(String aliases) {
		return getForUpdateString() + " of " + aliases + " nowait";
	}

	@Override
	public boolean bindLimitParametersInReverseOrder() {
		return true;
	}

	@Override
	public boolean useMaxForLimit() {
		return true;
	}

	@Override
	public boolean forUpdateOfColumns() {
		return true;
	}

	@Override
	public String getQuerySequencesString() {
		return "select * from all_sequences";
	}

	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceInformationExtractorOracleDatabaseImpl.INSTANCE;
	}

	@Override
	public String getSelectGUIDString() {
		return "select rawtohex(sys_guid()) from dual";
	}

	@Override
	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
		return EXTRACTER;
	}

	private static final ViolatedConstraintNameExtracter EXTRACTER = new TemplatedViolatedConstraintNameExtracter() {

		/**
		 * Extract the name of the violated constraint from the given SQLException.
		 *
		 * @param sqle The exception that was the result of the constraint violation.
		 * @return The extracted constraint name.
		 */
		@Override
		protected String doExtractConstraintName(SQLException sqle) throws NumberFormatException {
			final int errorCode = JdbcExceptionHelper.extractErrorCode( sqle );
			if ( errorCode == 1 || errorCode == 2291 || errorCode == 2292 ) {
				return extractUsingTemplate( "(", ")", sqle.getMessage() );
			}
			else if ( errorCode == 1400 ) {
				// simple nullability constraint
				return null;
			}
			else {
				return null;
			}
		}

	};

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return new SQLExceptionConversionDelegate() {
			@Override
			public JDBCException convert(SQLException sqlException, String message, String sql) {
				// interpreting Oracle exceptions is much much more precise based on their specific vendor codes.

				final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );


				// lock timeouts ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

				if ( errorCode == 30006 ) {
					// ORA-30006: resource busy; acquire with WAIT timeout expired
					throw new LockTimeoutException( message, sqlException, sql );
				}
				else if ( errorCode == 54 ) {
					// ORA-00054: resource busy and acquire with NOWAIT specified or timeout expired
					throw new LockTimeoutException( message, sqlException, sql );
				}
				else if ( 4021 == errorCode ) {
					// ORA-04021 timeout occurred while waiting to lock object
					throw new LockTimeoutException( message, sqlException, sql );
				}


				// deadlocks ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

				if ( 60 == errorCode ) {
					// ORA-00060: deadlock detected while waiting for resource
					return new LockAcquisitionException( message, sqlException, sql );
				}
				else if ( 4020 == errorCode ) {
					// ORA-04020 deadlock detected while trying to lock object
					return new LockAcquisitionException( message, sqlException, sql );
				}


				// query cancelled ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

				if ( 1013 == errorCode ) {
					// ORA-01013: user requested cancel of current operation
					throw new QueryTimeoutException(  message, sqlException, sql );
				}


				// data integrity violation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

				if ( 1407 == errorCode ) {
					// ORA-01407: cannot update column to NULL
					final String constraintName = getViolatedConstraintNameExtracter().extractConstraintName( sqlException );
					return new ConstraintViolationException( message, sqlException, sql, constraintName );
				}

				return null;
			}
		};
	}

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		//	register the type of the out param - an Oracle specific type
		statement.registerOutParameter( col, OracleTypesHelper.INSTANCE.getOracleCursorTypeSqlType() );
		col++;
		return col;
	}

	@Override
	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
		ps.execute();
		return (ResultSet) ps.getObject( 1 );
	}

	@Override
	public boolean supportsUnionAll() {
		return true;
	}

	@Override
	public boolean supportsCommentOn() {
		return true;
	}

	@Override
	public SqmMutationStrategy getDefaultIdTableStrategy() {
		return new GlobalTemporaryTableStrategy( generateIdTableSupport() );
	}

	private IdTableSupport generateIdTableSupport() {
		return new StandardIdTableSupport( generateIdTableExporter() ) {
			@Override
			protected Identifier determineIdTableName(Identifier baseName) {
				final Identifier name = super.determineIdTableName( baseName );
				return name.getText().length() > 30
						? new Identifier( name.getText().substring( 0, 30 ), false )
						: name;
			}
		};
	}

	private Exporter<IdTable> generateIdTableExporter() {
		return new GlobalTempTableExporter() {
			@Override
			protected String getCreateOptions() {
				return "on commit delete rows";
			}
		};
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
	public boolean supportsEmptyInList() {
		return false;
	}
	
	@Override
	public boolean supportsExistsInSelect() {
		return false;
	}

	@Override
	public int getInExpressionCountLimit() {
		return PARAM_LIST_SIZE_LIMIT;
	}
	
	@Override
	public boolean forceLobAsLastValue() {
		return true;
	}

	@Override
	public boolean isEmptyStringTreatedAsNull() {
		return true;
	}

	/**
	 * For Oracle, the FOR UPDATE clause cannot be applied when using ORDER BY, DISTINCT or views.
	 *
	 * @see <a href="https://docs.oracle.com/database/121/SQLRF/statements_10002.htm#SQLRF01702">Oracle FOR UPDATE restrictions</a>
	 */
	@Override
	public boolean useFollowOnLocking(String sql, QueryOptions queryOptions) {
		if ( StringHelper.isEmpty( sql ) || queryOptions == null ) {
			return true;
		}

		sql = sql.toLowerCase( Locale.ROOT );

		return DISTINCT_KEYWORD_PATTERN.matcher( sql ).find()
				|| GROUP_BY_KEYWORD_PATTERN.matcher( sql ).find()
				|| UNION_KEYWORD_PATTERN.matcher( sql ).find()
				|| (
						queryOptions.hasLimit()
								&& (
										ORDER_BY_KEYWORD_PATTERN.matcher( sql ).find()
												|| queryOptions.getLimit().getFirstRow() != null
								)
				);
	}
	
	@Override
	public String getNotExpression( String expression ) {
		return "not (" + expression + ")";
	}
	
	@Override
	public String getQueryHintString(String sql, String hints) {
		String statementType = statementType(sql);

		final int pos = sql.indexOf( statementType );
		if ( pos > -1 ) {
			final StringBuilder buffer = new StringBuilder( sql.length() + hints.length() + 8 );
			if ( pos > 0 ) {
				buffer.append( sql.substring( 0, pos ) );
			}
			buffer
			.append( statementType )
			.append( " /*+ " )
			.append( hints )
			.append( " */" )
			.append( sql.substring( pos + statementType.length() ) );
			sql = buffer.toString();
		}

		return sql;
	}
	
	@Override
	public int getMaxAliasLength() {
		// Oracle's max identifier length is 30, but Hibernate needs to add "uniqueing info" so we account for that,
		return 20;
	}

	@Override
	public CallableStatementSupport getCallableStatementSupport() {
		// Oracle supports returning cursors
		return StandardCallableStatementSupport.REF_CURSOR_INSTANCE;
	}

	@Override
	public boolean canCreateSchema() {
		return false;
	}

	@Override
	public String getCurrentSchemaCommand() {
		return "SELECT SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') FROM DUAL";
	}

	@Override
	public boolean supportsPartitionBy() {
		return true;
	}

	protected String statementType(String sql) {
		Matcher matcher = SQL_STATEMENT_TYPE_PATTERN.matcher( sql );

		if(matcher.matches() && matcher.groupCount() == 1) {
			return matcher.group(1);
		}

		throw new IllegalArgumentException( "Can't determine SQL statement type for statement: " + sql );
	}

	@Override
	public boolean supportsNoWait() {
		return true;
	}
}
