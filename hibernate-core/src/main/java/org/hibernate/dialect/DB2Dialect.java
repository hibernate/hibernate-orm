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

import org.hibernate.JDBCException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.constraint.DB2UniqueKeyExporter;
import org.hibernate.dialect.function.AvgWithArgumentCastFunction;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.metamodel.spi.relational.Constraint;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.sql.SmallIntTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * An SQL dialect for DB2.
 *
 * @author Gavin King
 */
public class DB2Dialect extends Dialect {
	
	private final DB2UniqueKeyExporter uniqueKeyExporter = new DB2UniqueKeyExporter( this );

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
		registerFunction( "sign", new StandardSQLFunction( "sign", StandardBasicTypes.INTEGER ) );

		registerFunction( "ceiling", new StandardSQLFunction( "ceiling" ) );
		registerFunction( "ceil", new StandardSQLFunction( "ceil" ) );
		registerFunction( "floor", new StandardSQLFunction( "floor" ) );
		registerFunction( "round", new StandardSQLFunction( "round" ) );

		registerFunction( "acos", new StandardSQLFunction( "acos", StandardBasicTypes.DOUBLE ) );
		registerFunction( "asin", new StandardSQLFunction( "asin", StandardBasicTypes.DOUBLE ) );
		registerFunction( "atan", new StandardSQLFunction( "atan", StandardBasicTypes.DOUBLE ) );
		registerFunction( "cos", new StandardSQLFunction( "cos", StandardBasicTypes.DOUBLE ) );
		registerFunction( "cot", new StandardSQLFunction( "cot", StandardBasicTypes.DOUBLE ) );
		registerFunction( "degrees", new StandardSQLFunction( "degrees", StandardBasicTypes.DOUBLE ) );
		registerFunction( "exp", new StandardSQLFunction( "exp", StandardBasicTypes.DOUBLE ) );
		registerFunction( "float", new StandardSQLFunction( "float", StandardBasicTypes.DOUBLE ) );
		registerFunction( "hex", new StandardSQLFunction( "hex", StandardBasicTypes.STRING ) );
		registerFunction( "ln", new StandardSQLFunction( "ln", StandardBasicTypes.DOUBLE ) );
		registerFunction( "log", new StandardSQLFunction( "log", StandardBasicTypes.DOUBLE ) );
		registerFunction( "log10", new StandardSQLFunction( "log10", StandardBasicTypes.DOUBLE ) );
		registerFunction( "radians", new StandardSQLFunction( "radians", StandardBasicTypes.DOUBLE ) );
		registerFunction( "rand", new NoArgSQLFunction( "rand", StandardBasicTypes.DOUBLE ) );
		registerFunction( "sin", new StandardSQLFunction( "sin", StandardBasicTypes.DOUBLE ) );
		registerFunction( "soundex", new StandardSQLFunction( "soundex", StandardBasicTypes.STRING ) );
		registerFunction( "sqrt", new StandardSQLFunction( "sqrt", StandardBasicTypes.DOUBLE ) );
		registerFunction( "stddev", new StandardSQLFunction( "stddev", StandardBasicTypes.DOUBLE ) );
		registerFunction( "tan", new StandardSQLFunction( "tan", StandardBasicTypes.DOUBLE ) );
		registerFunction( "variance", new StandardSQLFunction( "variance", StandardBasicTypes.DOUBLE ) );

		registerFunction( "julian_day", new StandardSQLFunction( "julian_day", StandardBasicTypes.INTEGER ) );
		registerFunction( "microsecond", new StandardSQLFunction( "microsecond", StandardBasicTypes.INTEGER ) );
		registerFunction(
				"midnight_seconds",
				new StandardSQLFunction( "midnight_seconds", StandardBasicTypes.INTEGER )
		);
		registerFunction( "minute", new StandardSQLFunction( "minute", StandardBasicTypes.INTEGER ) );
		registerFunction( "month", new StandardSQLFunction( "month", StandardBasicTypes.INTEGER ) );
		registerFunction( "monthname", new StandardSQLFunction( "monthname", StandardBasicTypes.STRING ) );
		registerFunction( "quarter", new StandardSQLFunction( "quarter", StandardBasicTypes.INTEGER ) );
		registerFunction( "hour", new StandardSQLFunction( "hour", StandardBasicTypes.INTEGER ) );
		registerFunction( "second", new StandardSQLFunction( "second", StandardBasicTypes.INTEGER ) );
		registerFunction( "current_date", new NoArgSQLFunction( "current date", StandardBasicTypes.DATE, false ) );
		registerFunction( "date", new StandardSQLFunction( "date", StandardBasicTypes.DATE ) );
		registerFunction( "day", new StandardSQLFunction( "day", StandardBasicTypes.INTEGER ) );
		registerFunction( "dayname", new StandardSQLFunction( "dayname", StandardBasicTypes.STRING ) );
		registerFunction( "dayofweek", new StandardSQLFunction( "dayofweek", StandardBasicTypes.INTEGER ) );
		registerFunction( "dayofweek_iso", new StandardSQLFunction( "dayofweek_iso", StandardBasicTypes.INTEGER ) );
		registerFunction( "dayofyear", new StandardSQLFunction( "dayofyear", StandardBasicTypes.INTEGER ) );
		registerFunction( "days", new StandardSQLFunction( "days", StandardBasicTypes.LONG ) );
		registerFunction( "current_time", new NoArgSQLFunction( "current time", StandardBasicTypes.TIME, false ) );
		registerFunction( "time", new StandardSQLFunction( "time", StandardBasicTypes.TIME ) );
		registerFunction(
				"current_timestamp",
				new NoArgSQLFunction( "current timestamp", StandardBasicTypes.TIMESTAMP, false )
		);
		registerFunction( "timestamp", new StandardSQLFunction( "timestamp", StandardBasicTypes.TIMESTAMP ) );
		registerFunction( "timestamp_iso", new StandardSQLFunction( "timestamp_iso", StandardBasicTypes.TIMESTAMP ) );
		registerFunction( "week", new StandardSQLFunction( "week", StandardBasicTypes.INTEGER ) );
		registerFunction( "week_iso", new StandardSQLFunction( "week_iso", StandardBasicTypes.INTEGER ) );
		registerFunction( "year", new StandardSQLFunction( "year", StandardBasicTypes.INTEGER ) );

		registerFunction( "double", new StandardSQLFunction( "double", StandardBasicTypes.DOUBLE ) );
		registerFunction( "varchar", new StandardSQLFunction( "varchar", StandardBasicTypes.STRING ) );
		registerFunction( "real", new StandardSQLFunction( "real", StandardBasicTypes.FLOAT ) );
		registerFunction( "bigint", new StandardSQLFunction( "bigint", StandardBasicTypes.LONG ) );
		registerFunction( "char", new StandardSQLFunction( "char", StandardBasicTypes.CHARACTER ) );
		registerFunction( "integer", new StandardSQLFunction( "integer", StandardBasicTypes.INTEGER ) );
		registerFunction( "smallint", new StandardSQLFunction( "smallint", StandardBasicTypes.SHORT ) );

		registerFunction( "digits", new StandardSQLFunction( "digits", StandardBasicTypes.STRING ) );
		registerFunction( "chr", new StandardSQLFunction( "chr", StandardBasicTypes.CHARACTER ) );
		registerFunction( "upper", new StandardSQLFunction( "upper" ) );
		registerFunction( "lower", new StandardSQLFunction( "lower" ) );
		registerFunction( "ucase", new StandardSQLFunction( "ucase" ) );
		registerFunction( "lcase", new StandardSQLFunction( "lcase" ) );
		registerFunction( "ltrim", new StandardSQLFunction( "ltrim" ) );
		registerFunction( "rtrim", new StandardSQLFunction( "rtrim" ) );
		registerFunction( "substr", new StandardSQLFunction( "substr", StandardBasicTypes.STRING ) );
		registerFunction( "posstr", new StandardSQLFunction( "posstr", StandardBasicTypes.INTEGER ) );

		registerFunction( "substring", new StandardSQLFunction( "substr", StandardBasicTypes.STRING ) );
		registerFunction( "bit_length", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "length(?1)*8" ) );
		registerFunction( "trim", new SQLFunctionTemplate( StandardBasicTypes.STRING, "trim(?1 ?2 ?3 ?4)" ) );

		registerFunction( "concat", new VarArgsSQLFunction( StandardBasicTypes.STRING, "", "||", "" ) );

		registerFunction( "str", new SQLFunctionTemplate( StandardBasicTypes.STRING, "rtrim(char(?1))" ) );

		registerKeyword( "current" );
		registerKeyword( "date" );
		registerKeyword( "time" );
		registerKeyword( "timestamp" );
		registerKeyword( "fetch" );
		registerKeyword( "first" );
		registerKeyword( "rows" );
		registerKeyword( "only" );

		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, NO_BATCH );
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
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public String getIdentitySelectString() {
		return "values identity_val_local()";
	}

	@Override
	public String getIdentityColumnString() {
		return "generated by default as identity";
	}

	@Override
	public String getIdentityInsertString() {
		return "default";
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "values nextval for " + sequenceName;
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
	public String getForUpdateString() {
		return " for read only with rs use and keep update locks";
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
	public boolean supportsTemporaryTables() {
		return true;
	}

	@Override
	public String getCreateTemporaryTableString() {
		return "declare global temporary table";
	}

	@Override
	public String getCreateTemporaryTablePostfix() {
		return "not logged";
	}

	@Override
	public String generateTemporaryTableName(String baseTableName) {
		return "session." + super.generateTemporaryTableName( baseTableName );
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
		return sqlCode == Types.BOOLEAN ? SmallIntTypeDescriptor.INSTANCE : super.getSqlTypeDescriptorOverride( sqlCode );
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
	public Exporter<Constraint> getUniqueKeyExporter() {
		return uniqueKeyExporter;
	}
	
	@Override
	public String getNotExpression( String expression ) {
		return "not (" + expression + ")";
	}
	
	@Override
    public LimitHandler buildLimitHandler(String sql, RowSelection selection) {
        return new AbstractLimitHandler(sql, selection) {
        	@Override
        	public String getProcessedSql() {
        		if ( LimitHelper.hasFirstRow(selection) ) {
            		//nest the main query in an outer select
            		return "select * from ( select inner2_.*, rownumber() over(order by order of inner2_) as rownumber_ from ( "
            				+ sql + " fetch first ? rows only ) as inner2_ ) as inner1_ where rownumber_ > "
            				+ "? order by rownumber_";
        		}
    			return sql + " fetch first ? rows only";
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
    }

}
