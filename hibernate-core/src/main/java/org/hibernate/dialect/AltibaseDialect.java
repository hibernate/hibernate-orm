/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.CallableStatement;

import org.hibernate.dialect.pagination.LegacyLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.DecodeCaseFragment;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.type.StandardBasicTypes;

/**
 * An SQL dialect for Altibase
 *
 * @author Youn Jung Park
 */
public class AltibaseDialect extends Dialect {
	/**
	 * Constructs a AltibaseDialect
	 */
	public AltibaseDialect() {
		super();

		registerCharacterTypeMappings();
		registerNumericTypeMappings();
		registerDateTimeTypeMappings();
		registerBooleanTypeMapping();
		registerLargeObjectTypeMappings();
		registerFunctions();
		registerDefaultProperties();
	}

	protected void registerCharacterTypeMappings() {
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.VARCHAR, 32767, "varchar($l)" );
		registerColumnType( Types.VARCHAR, "clob" );
		registerColumnType( Types.LONGVARCHAR, "varchar(32000)" );
	}

	protected void registerNumericTypeMappings() {
		registerColumnType( Types.BIT, "number(1,0)" );
		registerColumnType( Types.BIGINT, "bigint" );
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.TINYINT, "number(3,0)" );
		registerColumnType( Types.INTEGER, "integer" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.DOUBLE, "double" );
		registerColumnType( Types.NUMERIC, "number($p,$s)" );
		registerColumnType( Types.DECIMAL, "number($p,$s)" );
	}

	protected void registerDateTimeTypeMappings() {
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "date" );
		registerColumnType( Types.TIMESTAMP, "date" );
	}

	protected void registerBooleanTypeMapping() {
		registerColumnType( Types.BOOLEAN, "char(1)" );
	}

	protected void registerLargeObjectTypeMappings() {
		registerColumnType( Types.VARBINARY, "byte" );
		registerColumnType( Types.BLOB, "blob" );
		registerColumnType( Types.CLOB, "clob" );
	}

	protected void registerFunctions() {
		registerFunction( "abs", new StandardSQLFunction("abs", StandardBasicTypes.DOUBLE) );
		registerFunction( "sign", new StandardSQLFunction("sign", StandardBasicTypes.INTEGER) );

		registerFunction( "acos", new StandardSQLFunction("acos", StandardBasicTypes.DOUBLE) );
		registerFunction( "asin", new StandardSQLFunction("asin", StandardBasicTypes.DOUBLE) );
		registerFunction( "atan", new StandardSQLFunction("atan", StandardBasicTypes.DOUBLE) );
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
		registerFunction( "upper", new StandardSQLFunction("upper") );
		registerFunction( "ascii", new StandardSQLFunction("ascii", StandardBasicTypes.INTEGER) );

		registerFunction( "to_char", new StandardSQLFunction("to_char", StandardBasicTypes.STRING) );
		registerFunction( "to_date", new StandardSQLFunction("to_date", StandardBasicTypes.TIMESTAMP) );

		registerFunction( "last_day", new StandardSQLFunction("last_day", StandardBasicTypes.DATE) );
		registerFunction( "sysdate", new NoArgSQLFunction("sysdate", StandardBasicTypes.DATE, false) );
		registerFunction( "uid", new NoArgSQLFunction("user_id", StandardBasicTypes.INTEGER, false) );
		registerFunction( "user", new NoArgSQLFunction("user_name", StandardBasicTypes.STRING, false) );

		registerFunction( "rownum", new NoArgSQLFunction("rownum", StandardBasicTypes.LONG, false) );

		registerFunction( "concat", new VarArgsSQLFunction(StandardBasicTypes.STRING, "", "||", "") );
		registerFunction( "instr", new StandardSQLFunction("instr", StandardBasicTypes.INTEGER) );
		registerFunction( "instrb", new StandardSQLFunction("instrb", StandardBasicTypes.INTEGER) );
		registerFunction( "lpad", new StandardSQLFunction("lpad", StandardBasicTypes.STRING) );
		registerFunction( "replace", new StandardSQLFunction("replace", StandardBasicTypes.STRING) );
		registerFunction( "rpad", new StandardSQLFunction("rpad", StandardBasicTypes.STRING) );
		registerFunction( "substr", new StandardSQLFunction("substr", StandardBasicTypes.STRING) );
		registerFunction( "substrb", new StandardSQLFunction("substrb", StandardBasicTypes.STRING) );
		registerFunction( "translate", new StandardSQLFunction("translate", StandardBasicTypes.STRING) );

		registerFunction( "substring", new StandardSQLFunction("substr", StandardBasicTypes.STRING) );

		registerFunction( "atan2", new StandardSQLFunction("atan2", StandardBasicTypes.FLOAT) );
		registerFunction( "log", new StandardSQLFunction("log", StandardBasicTypes.INTEGER) );
		registerFunction( "mod", new StandardSQLFunction("mod", StandardBasicTypes.INTEGER) );
		registerFunction( "nvl", new StandardSQLFunction("nvl") );
		registerFunction( "nvl2", new StandardSQLFunction("nvl2") );
		registerFunction( "power", new StandardSQLFunction("power", StandardBasicTypes.FLOAT) );

		registerFunction( "add_months", new StandardSQLFunction("add_months", StandardBasicTypes.DATE) );
		registerFunction( "months_between", new StandardSQLFunction("months_between", StandardBasicTypes.FLOAT) );
		registerFunction( "next_day", new StandardSQLFunction("next_day", StandardBasicTypes.DATE) );

		registerFunction( "str", new StandardSQLFunction("to_char", StandardBasicTypes.STRING) );
	}

	protected void registerDefaultProperties() {
		getDefaultProperties().setProperty( Environment.USE_STREAMS_FOR_BINARY, "true" );
		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );
		getDefaultProperties().setProperty( Environment.USE_GET_GENERATED_KEYS, "false" );
	}

	@Override
	public String getCrossJoinSeparator() {
		return ", ";
	}

	@Override
	public CaseFragment createCaseFragment() {
		return new DecodeCaseFragment();
	}

	@Override
	public String getSelectClauseNullString(int sqlType) {
		switch (sqlType) {
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

	@Override
	public String getAddColumnString() {
		return "add (";
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
		return "create sequence " + sequenceName;
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
	public String getForUpdateString(String aliases) {
		return getForUpdateString();
	}

	@Override
	public String getForUpdateNowaitString(String aliases) {
		return getForUpdateString();
	}

	@Override
	public boolean forUpdateOfColumns() {
		return true;
	}

	@Override
	public String getQuerySequencesString() {
		return " select table_name from system_.sys_tables_ where table_type='S'"
				+ "  union"
				+ " select synonym_name from system_.sys_synonyms_ where "
				+ " object_name in (select table_name from system_.sys_tables_ where table_type='S')";
	}

	@Override
	public LimitHandler buildLimitHandler(String sql, RowSelection selection) {
		return new LegacyLimitHandler( this, sql, selection );
	}

	public boolean supportsLimit() {
		return true;
	}

	public boolean supportsLimitOffset() {
		return true;
	}

	public boolean bindLimitParametersInReverseOrder() {
		return false;
	}

	public boolean useMaxForLimit() {
		return true;
	}

	public boolean bindLimitParametersFirst() {
		return true;
	}

	public boolean supportsVariableLimit() {
		return false;
	}

	public String getLimitString(String query, int offset, int limit) {
		StringBuilder sb = new StringBuilder( query.length() + 20 );
		sb.append( query );
		if ( offset <= 0 ) {
			sb.append(" limit 1, ").append(limit - offset);
		}
		else if ( limit - offset <= 0 ) {
			sb.append( " limit 1" );
		}
		else {
			sb.append( " limit " ).append( offset ).append(", ").append( limit - offset );
		}

		return sb.toString();
	}


	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		return col;
	}

	@Override
	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
		ps.execute();
		return (ResultSet)ps.getObject( 1 );
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
}
