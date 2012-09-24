/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.dialect;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.QueryTimeoutException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.NvlFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.DecodeCaseFragment;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.OracleJoinFragment;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.sql.BitTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * A dialect for Oracle 8i.
 *
 * @author Steve Ebersole
 */
public class Oracle8iDialect extends Dialect {
	
	private static final int PARAM_LIST_SIZE_LIMIT = 1000;

	public Oracle8iDialect() {
		super();
		registerCharacterTypeMappings();
		registerNumericTypeMappings();
		registerDateTimeTypeMappings();
		registerLargeObjectTypeMappings();
		registerReverseHibernateTypeMappings();
		registerFunctions();
		registerDefaultProperties();
	}

	protected void registerCharacterTypeMappings() {
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.VARCHAR, 4000, "varchar2($l)" );
		registerColumnType( Types.VARCHAR, "long" );
	}

	protected void registerNumericTypeMappings() {
		registerColumnType( Types.BIT, "number(1,0)" );
		registerColumnType( Types.BIGINT, "number(19,0)" );
		registerColumnType( Types.SMALLINT, "number(5,0)" );
		registerColumnType( Types.TINYINT, "number(3,0)" );
		registerColumnType( Types.INTEGER, "number(10,0)" );

		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.DOUBLE, "double precision" );
		registerColumnType( Types.NUMERIC, "number($p,$s)" );
		registerColumnType( Types.DECIMAL, "number($p,$s)" );

        registerColumnType( Types.BOOLEAN, "number(1,0)" );
	}

	protected void registerDateTimeTypeMappings() {
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "date" );
		registerColumnType( Types.TIMESTAMP, "date" );
	}

	protected void registerLargeObjectTypeMappings() {
		registerColumnType( Types.BINARY, 2000, "raw($l)" );
		registerColumnType( Types.BINARY, "long raw" );

		registerColumnType( Types.VARBINARY, 2000, "raw($l)" );
		registerColumnType( Types.VARBINARY, "long raw" );

		registerColumnType( Types.BLOB, "blob" );
		registerColumnType( Types.CLOB, "clob" );

		registerColumnType( Types.LONGVARCHAR, "long" );
		registerColumnType( Types.LONGVARBINARY, "long raw" );
	}

	protected void registerReverseHibernateTypeMappings() {
	}

	protected void registerFunctions() {
		registerFunction( "abs", new StandardSQLFunction("abs") );
		registerFunction( "sign", new StandardSQLFunction("sign", StandardBasicTypes.INTEGER) );

		registerFunction( "acos", new StandardSQLFunction("acos", StandardBasicTypes.DOUBLE) );
		registerFunction( "asin", new StandardSQLFunction("asin", StandardBasicTypes.DOUBLE) );
		registerFunction( "atan", new StandardSQLFunction("atan", StandardBasicTypes.DOUBLE) );
		registerFunction( "bitand", new StandardSQLFunction("bitand") );
		registerFunction( "cos", new StandardSQLFunction("cos", StandardBasicTypes.DOUBLE) );
		registerFunction( "cosh", new StandardSQLFunction("cosh", StandardBasicTypes.DOUBLE) );
		registerFunction( "exp", new StandardSQLFunction("exp", StandardBasicTypes.DOUBLE) );
		registerFunction( "ln", new StandardSQLFunction("ln", StandardBasicTypes.DOUBLE) );
		registerFunction( "sin", new StandardSQLFunction("sin", StandardBasicTypes.DOUBLE) );
		registerFunction( "sinh", new StandardSQLFunction("sinh", StandardBasicTypes.DOUBLE) );
		registerFunction( "stddev", new StandardSQLFunction("stddev", StandardBasicTypes.DOUBLE) );
		registerFunction( "sqrt", new StandardSQLFunction("sqrt", StandardBasicTypes.DOUBLE) );
		registerFunction( "tan", new StandardSQLFunction("tan", StandardBasicTypes.DOUBLE) );
		registerFunction( "tanh", new StandardSQLFunction("tanh", StandardBasicTypes.DOUBLE) );
		registerFunction( "variance", new StandardSQLFunction("variance", StandardBasicTypes.DOUBLE) );

		registerFunction( "round", new StandardSQLFunction("round") );
		registerFunction( "trunc", new StandardSQLFunction("trunc") );
		registerFunction( "ceil", new StandardSQLFunction("ceil") );
		registerFunction( "floor", new StandardSQLFunction("floor") );

		registerFunction( "chr", new StandardSQLFunction("chr", StandardBasicTypes.CHARACTER) );
		registerFunction( "initcap", new StandardSQLFunction("initcap") );
		registerFunction( "lower", new StandardSQLFunction("lower") );
		registerFunction( "ltrim", new StandardSQLFunction("ltrim") );
		registerFunction( "rtrim", new StandardSQLFunction("rtrim") );
		registerFunction( "soundex", new StandardSQLFunction("soundex") );
		registerFunction( "upper", new StandardSQLFunction("upper") );
		registerFunction( "ascii", new StandardSQLFunction("ascii", StandardBasicTypes.INTEGER) );

		registerFunction( "to_char", new StandardSQLFunction("to_char", StandardBasicTypes.STRING) );
		registerFunction( "to_date", new StandardSQLFunction("to_date", StandardBasicTypes.TIMESTAMP) );

		registerFunction( "current_date", new NoArgSQLFunction("current_date", StandardBasicTypes.DATE, false) );
		registerFunction( "current_time", new NoArgSQLFunction("current_timestamp", StandardBasicTypes.TIME, false) );
		registerFunction( "current_timestamp", new NoArgSQLFunction("current_timestamp", StandardBasicTypes.TIMESTAMP, false) );

		registerFunction( "last_day", new StandardSQLFunction("last_day", StandardBasicTypes.DATE) );
		registerFunction( "sysdate", new NoArgSQLFunction("sysdate", StandardBasicTypes.DATE, false) );
		registerFunction( "systimestamp", new NoArgSQLFunction("systimestamp", StandardBasicTypes.TIMESTAMP, false) );
		registerFunction( "uid", new NoArgSQLFunction("uid", StandardBasicTypes.INTEGER, false) );
		registerFunction( "user", new NoArgSQLFunction("user", StandardBasicTypes.STRING, false) );

		registerFunction( "rowid", new NoArgSQLFunction("rowid", StandardBasicTypes.LONG, false) );
		registerFunction( "rownum", new NoArgSQLFunction("rownum", StandardBasicTypes.LONG, false) );

		// Multi-param string dialect functions...
		registerFunction( "concat", new VarArgsSQLFunction(StandardBasicTypes.STRING, "", "||", "") );
		registerFunction( "instr", new StandardSQLFunction("instr", StandardBasicTypes.INTEGER) );
		registerFunction( "instrb", new StandardSQLFunction("instrb", StandardBasicTypes.INTEGER) );
		registerFunction( "lpad", new StandardSQLFunction("lpad", StandardBasicTypes.STRING) );
		registerFunction( "replace", new StandardSQLFunction("replace", StandardBasicTypes.STRING) );
		registerFunction( "rpad", new StandardSQLFunction("rpad", StandardBasicTypes.STRING) );
		registerFunction( "substr", new StandardSQLFunction("substr", StandardBasicTypes.STRING) );
		registerFunction( "substrb", new StandardSQLFunction("substrb", StandardBasicTypes.STRING) );
		registerFunction( "translate", new StandardSQLFunction("translate", StandardBasicTypes.STRING) );

		registerFunction( "substring", new StandardSQLFunction( "substr", StandardBasicTypes.STRING ) );
		registerFunction( "locate", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "instr(?2,?1)" ) );
		registerFunction( "bit_length", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "vsize(?1)*8" ) );
		registerFunction( "coalesce", new NvlFunction() );

		// Multi-param numeric dialect functions...
		registerFunction( "atan2", new StandardSQLFunction("atan2", StandardBasicTypes.FLOAT) );
		registerFunction( "log", new StandardSQLFunction("log", StandardBasicTypes.INTEGER) );
		registerFunction( "mod", new StandardSQLFunction("mod", StandardBasicTypes.INTEGER) );
		registerFunction( "nvl", new StandardSQLFunction("nvl") );
		registerFunction( "nvl2", new StandardSQLFunction("nvl2") );
		registerFunction( "power", new StandardSQLFunction("power", StandardBasicTypes.FLOAT) );

		// Multi-param date dialect functions...
		registerFunction( "add_months", new StandardSQLFunction("add_months", StandardBasicTypes.DATE) );
		registerFunction( "months_between", new StandardSQLFunction("months_between", StandardBasicTypes.FLOAT) );
		registerFunction( "next_day", new StandardSQLFunction("next_day", StandardBasicTypes.DATE) );

		registerFunction( "str", new StandardSQLFunction("to_char", StandardBasicTypes.STRING) );
	}

	protected void registerDefaultProperties() {
		getDefaultProperties().setProperty( Environment.USE_STREAMS_FOR_BINARY, "true" );
		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );
		// Oracle driver reports to support getGeneratedKeys(), but they only
		// support the version taking an array of the names of the columns to
		// be returned (via its RETURNING clause).  No other driver seems to
		// support this overloaded version.
		getDefaultProperties().setProperty( Environment.USE_GET_GENERATED_KEYS, "false" );
	}

	@Override
	protected SqlTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {
		return sqlCode == Types.BOOLEAN ? BitTypeDescriptor.INSTANCE : super.getSqlTypeDescriptorOverride( sqlCode );
	}


	// features which change between 8i, 9i, and 10g ~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Support for the oracle proprietary join syntax...
	 *
	 * @return The orqacle join fragment
	 */
	@Override
	public JoinFragment createOuterJoinFragment() {
		return new OracleJoinFragment();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getCrossJoinSeparator() {
		return ", ";
	}

	/**
	 * Map case support to the Oracle DECODE function.  Oracle did not
	 * add support for CASE until 9i.
	 *
	 * @return The oracle CASE -> DECODE fragment
	 */
	@Override
	public CaseFragment createCaseFragment() {
		return new DecodeCaseFragment();
	}
	@Override
	public String getLimitString(String sql, boolean hasOffset) {
		sql = sql.trim();
		boolean isForUpdate = false;
		if ( sql.toLowerCase().endsWith(" for update") ) {
			sql = sql.substring( 0, sql.length()-11 );
			isForUpdate = true;
		}

		StringBuilder pagingSelect = new StringBuilder( sql.length()+100 );
		if (hasOffset) {
			pagingSelect.append("select * from ( select row_.*, rownum rownum_ from ( ");
		}
		else {
			pagingSelect.append("select * from ( ");
		}
		pagingSelect.append(sql);
		if (hasOffset) {
			pagingSelect.append(" ) row_ ) where rownum_ <= ? and rownum_ > ?");
		}
		else {
			pagingSelect.append(" ) where rownum <= ?");
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
		return "create sequence " + sequenceName; //starts with 1, implicitly
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
		return    " select sequence_name from all_sequences"
				+ "  union"
				+ " select synonym_name"
				+ "   from all_synonyms us, all_sequences asq"
				+ "  where asq.sequence_name = us.table_name"
				+ "    and asq.sequence_owner = us.table_owner";
	}
	@Override
	public String getSelectGUIDString() {
		return "select rawtohex(sys_guid()) from dual";
	}
	@Override
	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
        return EXTRACTER;
	}

	private static ViolatedConstraintNameExtracter EXTRACTER = new TemplatedViolatedConstraintNameExtracter() {

		/**
		 * Extract the name of the violated constraint from the given SQLException.
		 *
		 * @param sqle The exception that was the result of the constraint violation.
		 * @return The extracted constraint name.
		 */
		public String extractConstraintName(SQLException sqle) {
			int errorCode = JdbcExceptionHelper.extractErrorCode( sqle );
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


				return null;
			}
		};
	}

	public static final String ORACLE_TYPES_CLASS_NAME = "oracle.jdbc.OracleTypes";
	public static final String DEPRECATED_ORACLE_TYPES_CLASS_NAME = "oracle.jdbc.driver.OracleTypes";

	public static final int INIT_ORACLETYPES_CURSOR_VALUE = -99;

	// not final-static to avoid possible classcast exceptions if using different oracle drivers.
	private int oracleCursorTypeSqlType = INIT_ORACLETYPES_CURSOR_VALUE;

	public int getOracleCursorTypeSqlType() {
		if ( oracleCursorTypeSqlType == INIT_ORACLETYPES_CURSOR_VALUE ) {
			// todo : is there really any reason to kkeep trying if this fails once?
			oracleCursorTypeSqlType = extractOracleCursorTypeValue();
		}
		return oracleCursorTypeSqlType;
	}

	protected int extractOracleCursorTypeValue() {
		Class oracleTypesClass;
		try {
			oracleTypesClass = ReflectHelper.classForName( ORACLE_TYPES_CLASS_NAME );
		}
		catch ( ClassNotFoundException cnfe ) {
			try {
				oracleTypesClass = ReflectHelper.classForName( DEPRECATED_ORACLE_TYPES_CLASS_NAME );
			}
			catch ( ClassNotFoundException e ) {
				throw new HibernateException( "Unable to locate OracleTypes class", e );
			}
		}

		try {
			return oracleTypesClass.getField( "CURSOR" ).getInt( null );
		}
		catch ( Exception se ) {
			throw new HibernateException( "Unable to access OracleTypes.CURSOR value", se );
		}
	}
	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		//	register the type of the out param - an Oracle specific type
		statement.registerOutParameter( col, getOracleCursorTypeSqlType() );
		col++;
		return col;
	}
	@Override
	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
		ps.execute();
		return ( ResultSet ) ps.getObject( 1 );
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
	public boolean supportsTemporaryTables() {
		return true;
	}
	@Override
	public String generateTemporaryTableName(String baseTableName) {
		String name = super.generateTemporaryTableName(baseTableName);
		return name.length() > 30 ? name.substring( 1, 30 ) : name;
	}
	@Override
	public String getCreateTemporaryTableString() {
		return "create global temporary table";
	}
	@Override
	public String getCreateTemporaryTablePostfix() {
		return "on commit delete rows";
	}
	@Override
	public boolean dropTemporaryTableAfterUse() {
		return false;
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

	/* (non-Javadoc)
		 * @see org.hibernate.dialect.Dialect#getInExpressionCountLimit()
		 */
	@Override
	public int getInExpressionCountLimit() {
		return PARAM_LIST_SIZE_LIMIT;
	}
	
	@Override
	public boolean supportsNotNullUnique() {
		return false;
	}
	
	@Override
	public boolean forceLobAsLastValue() {
		return true;
	}

}
