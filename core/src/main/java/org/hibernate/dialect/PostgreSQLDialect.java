/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.dialect;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.Hibernate;
import org.hibernate.LockOptions;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.PositionSubstringFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.exception.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.ViolatedConstraintNameExtracter;
import org.hibernate.id.SequenceGenerator;

/**
 * An SQL dialect for Postgres
 * @author Gavin King
 */
public class PostgreSQLDialect extends Dialect {

	public PostgreSQLDialect() {
		super();
		registerColumnType( Types.BIT, "bool" );
		registerColumnType( Types.BIGINT, "int8" );
		registerColumnType( Types.SMALLINT, "int2" );
		registerColumnType( Types.TINYINT, "int2" );
		registerColumnType( Types.INTEGER, "int4" );
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.FLOAT, "float4" );
		registerColumnType( Types.DOUBLE, "float8" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.VARBINARY, "bytea" );
		registerColumnType( Types.LONGVARCHAR, "text" );
		registerColumnType( Types.LONGVARBINARY, "bytea" );
		registerColumnType( Types.CLOB, "text" );
		registerColumnType( Types.BLOB, "oid" );
		registerColumnType( Types.NUMERIC, "numeric($p, $s)" );

		registerFunction( "abs", new StandardSQLFunction("abs") );
		registerFunction( "sign", new StandardSQLFunction("sign", Hibernate.INTEGER) );

		registerFunction( "acos", new StandardSQLFunction("acos", Hibernate.DOUBLE) );
		registerFunction( "asin", new StandardSQLFunction("asin", Hibernate.DOUBLE) );
		registerFunction( "atan", new StandardSQLFunction("atan", Hibernate.DOUBLE) );
		registerFunction( "cos", new StandardSQLFunction("cos", Hibernate.DOUBLE) );
		registerFunction( "cot", new StandardSQLFunction("cot", Hibernate.DOUBLE) );
		registerFunction( "exp", new StandardSQLFunction("exp", Hibernate.DOUBLE) );
		registerFunction( "ln", new StandardSQLFunction("ln", Hibernate.DOUBLE) );
		registerFunction( "log", new StandardSQLFunction("log", Hibernate.DOUBLE) );
		registerFunction( "sin", new StandardSQLFunction("sin", Hibernate.DOUBLE) );
		registerFunction( "sqrt", new StandardSQLFunction("sqrt", Hibernate.DOUBLE) );
		registerFunction( "cbrt", new StandardSQLFunction("cbrt", Hibernate.DOUBLE) );
		registerFunction( "tan", new StandardSQLFunction("tan", Hibernate.DOUBLE) );
		registerFunction( "radians", new StandardSQLFunction("radians", Hibernate.DOUBLE) );
		registerFunction( "degrees", new StandardSQLFunction("degrees", Hibernate.DOUBLE) );

		registerFunction( "stddev", new StandardSQLFunction("stddev", Hibernate.DOUBLE) );
		registerFunction( "variance", new StandardSQLFunction("variance", Hibernate.DOUBLE) );

		registerFunction( "random", new NoArgSQLFunction("random", Hibernate.DOUBLE) );

		registerFunction( "round", new StandardSQLFunction("round") );
		registerFunction( "trunc", new StandardSQLFunction("trunc") );
		registerFunction( "ceil", new StandardSQLFunction("ceil") );
		registerFunction( "floor", new StandardSQLFunction("floor") );

		registerFunction( "chr", new StandardSQLFunction("chr", Hibernate.CHARACTER) );
		registerFunction( "lower", new StandardSQLFunction("lower") );
		registerFunction( "upper", new StandardSQLFunction("upper") );
		registerFunction( "substr", new StandardSQLFunction("substr", Hibernate.STRING) );
		registerFunction( "initcap", new StandardSQLFunction("initcap") );
		registerFunction( "to_ascii", new StandardSQLFunction("to_ascii") );
		registerFunction( "quote_ident", new StandardSQLFunction("quote_ident", Hibernate.STRING) );
		registerFunction( "quote_literal", new StandardSQLFunction("quote_literal", Hibernate.STRING) );
		registerFunction( "md5", new StandardSQLFunction("md5") );
		registerFunction( "ascii", new StandardSQLFunction("ascii", Hibernate.INTEGER) );
		registerFunction( "char_length", new StandardSQLFunction("char_length", Hibernate.LONG) );
		registerFunction( "bit_length", new StandardSQLFunction("bit_length", Hibernate.LONG) );
		registerFunction( "octet_length", new StandardSQLFunction("octet_length", Hibernate.LONG) );

		registerFunction( "age", new StandardSQLFunction("age") );
		registerFunction( "current_date", new NoArgSQLFunction("current_date", Hibernate.DATE, false) );
		registerFunction( "current_time", new NoArgSQLFunction("current_time", Hibernate.TIME, false) );
		registerFunction( "current_timestamp", new NoArgSQLFunction("current_timestamp", Hibernate.TIMESTAMP, false) );
		registerFunction( "date_trunc", new StandardSQLFunction( "date_trunc", Hibernate.TIMESTAMP ) );
		registerFunction( "localtime", new NoArgSQLFunction("localtime", Hibernate.TIME, false) );
		registerFunction( "localtimestamp", new NoArgSQLFunction("localtimestamp", Hibernate.TIMESTAMP, false) );
		registerFunction( "now", new NoArgSQLFunction("now", Hibernate.TIMESTAMP) );
		registerFunction( "timeofday", new NoArgSQLFunction("timeofday", Hibernate.STRING) );

		registerFunction( "current_user", new NoArgSQLFunction("current_user", Hibernate.STRING, false) );
		registerFunction( "session_user", new NoArgSQLFunction("session_user", Hibernate.STRING, false) );
		registerFunction( "user", new NoArgSQLFunction("user", Hibernate.STRING, false) );
		registerFunction( "current_database", new NoArgSQLFunction("current_database", Hibernate.STRING, true) );
		registerFunction( "current_schema", new NoArgSQLFunction("current_schema", Hibernate.STRING, true) );
		
		registerFunction( "to_char", new StandardSQLFunction("to_char", Hibernate.STRING) );
		registerFunction( "to_date", new StandardSQLFunction("to_date", Hibernate.DATE) );
		registerFunction( "to_timestamp", new StandardSQLFunction("to_timestamp", Hibernate.TIMESTAMP) );
		registerFunction( "to_number", new StandardSQLFunction("to_number", Hibernate.BIG_DECIMAL) );

		registerFunction( "concat", new VarArgsSQLFunction( Hibernate.STRING, "(","||",")" ) );

		registerFunction( "locate", new PositionSubstringFunction() );

		registerFunction( "str", new SQLFunctionTemplate(Hibernate.STRING, "cast(?1 as varchar)") );

		getDefaultProperties().setProperty(Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE);
	}

	public String getAddColumnString() {
		return "add column";
	}

	public String getSequenceNextValString(String sequenceName) {
		return "select " + getSelectSequenceNextValString( sequenceName );
	}

	public String getSelectSequenceNextValString(String sequenceName) {
		return "nextval ('" + sequenceName + "')";
	}

	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName; //starts with 1, implicitly
	}

	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName;
	}

	public String getCascadeConstraintsString() {
		return "";//" cascade";
	}
	public boolean dropConstraints() {
		return true;
	}

	public boolean supportsSequences() {
		return true;
	}

	public String getQuerySequencesString() {
		return "select relname from pg_class where relkind='S'";
	}

	public boolean supportsLimit() {
		return true;
	}

	public String getLimitString(String sql, boolean hasOffset) {
		return new StringBuffer( sql.length()+20 )
				.append( sql )
				.append( hasOffset ? " limit ? offset ?" : " limit ?" )
				.toString();
	}

	public boolean bindLimitParametersInReverseOrder() {
		return true;
	}

	public boolean supportsIdentityColumns() {
		return true;
	}

	public String getForUpdateString(String aliases) {
		return getForUpdateString() + " of " + aliases;
	}

	public String getIdentitySelectString(String table, String column, int type) {
		return new StringBuffer().append("select currval('")
			.append(table)
			.append('_')
			.append(column)
			.append("_seq')")
			.toString();
	}

	public String getIdentityColumnString(int type) {
		return type==Types.BIGINT ?
			"bigserial not null" :
			"serial not null";
	}

	public boolean hasDataTypeInIdentityColumn() {
		return false;
	}

	public String getNoColumnsInsertString() {
		return "default values";
	}

	public Class getNativeIdentifierGeneratorClass() {
		return SequenceGenerator.class;
	}

	public boolean supportsOuterJoinForUpdate() {
		return false;
	}
	
	public boolean useInputStreamToInsertBlob() {
		return false;
	}

	public boolean supportsUnionAll() {
		return true;
	}

	/**
	 * Workaround for postgres bug #1453
	 */
	public String getSelectClauseNullString(int sqlType) {
		String typeName = getTypeName(sqlType, 1, 1, 0);
		//trim off the length/precision/scale
		int loc = typeName.indexOf('(');
		if (loc>-1) {
			typeName = typeName.substring(0, loc);
		}
		return "null::" + typeName;
	}

	public boolean supportsCommentOn() {
		return true;
	}

	public boolean supportsTemporaryTables() {
		return true;
	}

	public String getCreateTemporaryTableString() {
		return "create temporary table";
	}

	public String getCreateTemporaryTablePostfix() {
		return "on commit drop";
	}

	/*public boolean dropTemporaryTableAfterUse() {
		//we have to, because postgres sets current tx
		//to rollback only after a failed create table
		return true;
	}*/

	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	public String getCurrentTimestampSelectString() {
		return "select now()";
	}

	public String toBooleanValueString(boolean bool) {
		return bool ? "true" : "false";
	}

	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
		return EXTRACTER;
	}

	/**
	 * Constraint-name extractor for Postgres contraint violation exceptions.
	 * Orginally contributed by Denny Bartelt.
	 */
	private static ViolatedConstraintNameExtracter EXTRACTER = new TemplatedViolatedConstraintNameExtracter() {
		public String extractConstraintName(SQLException sqle) {
			try {
				int sqlState = Integer.valueOf( JDBCExceptionHelper.extractSqlState(sqle)).intValue();
				switch (sqlState) {
					// CHECK VIOLATION
					case 23514: return extractUsingTemplate("violates check constraint \"","\"", sqle.getMessage());
					// UNIQUE VIOLATION
					case 23505: return extractUsingTemplate("violates unique constraint \"","\"", sqle.getMessage());
					// FOREIGN KEY VIOLATION
					case 23503: return extractUsingTemplate("violates foreign key constraint \"","\"", sqle.getMessage());
					// NOT NULL VIOLATION
					case 23502: return extractUsingTemplate("null value in column \"","\" violates not-null constraint", sqle.getMessage());
					// TODO: RESTRICT VIOLATION
					case 23001: return null;
					// ALL OTHER
					default: return null;
				}
			} catch (NumberFormatException nfe) {
				return null;
			}
		}
	};
	
	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		// Register the type of the out param - PostgreSQL uses Types.OTHER
		statement.registerOutParameter(col, Types.OTHER);
		col++;
		return col;
	}

	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
		ps.execute();
		ResultSet rs = (ResultSet) ps.getObject(1);
		return rs;
	}

	public boolean supportsPooledSequences() {
		return true;
	}

	//only necessary for postgre < 7.4
	//http://anoncvs.postgresql.org/cvsweb.cgi/pgsql/doc/src/sgml/ref/create_sequence.sgml
	protected String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize) {
		return getCreateSequenceString( sequenceName ) + " start " + initialValue + " increment " + incrementSize;
	}
	
	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

// seems to not really...
//	public boolean supportsRowValueConstructorSyntax() {
//		return true;
//	}

	public boolean supportsEmptyInList() {
		return false;
	}

	public boolean supportsExpectedLobUsagePattern() {
		// seems to have spotty LOB suppport
		return false;
	}

	// locking support
	public String getForUpdateString() {
		return " for update";
	}

	public String getWriteLockString(int timeout) {
		if ( timeout == LockOptions.NO_WAIT )
			return " for update nowait";
		else
			return " for update";
	}

	public String getReadLockString(int timeout) {
		if ( timeout == LockOptions.NO_WAIT )
			return " for share nowait";
		else
			return " for share";
	}

}
