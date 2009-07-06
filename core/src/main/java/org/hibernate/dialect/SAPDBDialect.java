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

import java.sql.Types;

import org.hibernate.Hibernate;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.DecodeCaseFragment;
import org.hibernate.sql.OracleJoinFragment;
import org.hibernate.sql.JoinFragment;
import org.hibernate.util.StringHelper;

/**
 * An SQL dialect compatible with SAP DB.
 * @author Brad Clow
 */
public class SAPDBDialect extends Dialect {

	public SAPDBDialect() {
		super();
		registerColumnType( Types.BIT, "boolean" );
		registerColumnType( Types.BIGINT, "fixed(19,0)" );
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.TINYINT, "fixed(3,0)" );
		registerColumnType( Types.INTEGER, "int" );
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.DOUBLE, "double precision" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.VARBINARY, "long byte" );
		registerColumnType( Types.NUMERIC, "fixed($p,$s)" );
		registerColumnType( Types.CLOB, "long varchar" );
		registerColumnType( Types.BLOB, "long byte" );
		
		registerFunction( "abs", new StandardSQLFunction("abs") );
		registerFunction( "sign", new StandardSQLFunction("sign", Hibernate.INTEGER) );

		registerFunction( "exp", new StandardSQLFunction("exp", Hibernate.DOUBLE) );
		registerFunction( "ln", new StandardSQLFunction("ln", Hibernate.DOUBLE) );
		registerFunction( "log", new StandardSQLFunction("ln", Hibernate.DOUBLE) );
		registerFunction( "pi", new NoArgSQLFunction("pi", Hibernate.DOUBLE) );
		registerFunction( "power", new StandardSQLFunction("power") );
		registerFunction( "acos", new StandardSQLFunction("acos", Hibernate.DOUBLE) );
		registerFunction( "asin", new StandardSQLFunction("asin", Hibernate.DOUBLE) );
		registerFunction( "atan", new StandardSQLFunction("atan", Hibernate.DOUBLE) );
		registerFunction( "cos", new StandardSQLFunction("cos", Hibernate.DOUBLE) );
		registerFunction( "cosh", new StandardSQLFunction("cosh", Hibernate.DOUBLE) );
		registerFunction( "cot", new StandardSQLFunction("cos", Hibernate.DOUBLE) );
		registerFunction( "sin", new StandardSQLFunction("sin", Hibernate.DOUBLE) );
		registerFunction( "sinh", new StandardSQLFunction("sinh", Hibernate.DOUBLE) );
		registerFunction( "tan", new StandardSQLFunction("tan", Hibernate.DOUBLE) );
		registerFunction( "tanh", new StandardSQLFunction("tanh", Hibernate.DOUBLE) );
		registerFunction( "radians", new StandardSQLFunction("radians", Hibernate.DOUBLE) );
		registerFunction( "degrees", new StandardSQLFunction("degrees", Hibernate.DOUBLE) );
		registerFunction( "atan2", new StandardSQLFunction("atan2", Hibernate.DOUBLE) );

		registerFunction( "round", new StandardSQLFunction("round") );
		registerFunction( "trunc", new StandardSQLFunction("trunc") );
		registerFunction( "ceil", new StandardSQLFunction("ceil") );
		registerFunction( "floor", new StandardSQLFunction("floor") );
		registerFunction( "greatest", new StandardSQLFunction("greatest") );
		registerFunction( "least", new StandardSQLFunction("least") );

		registerFunction("time", new StandardSQLFunction("time", Hibernate.TIME) );
		registerFunction("timestamp", new StandardSQLFunction("timestamp", Hibernate.TIMESTAMP) );
		registerFunction("date", new StandardSQLFunction("date", Hibernate.DATE) );
		registerFunction("microsecond", new StandardSQLFunction("microsecond", Hibernate.INTEGER) );

		registerFunction( "second", new SQLFunctionTemplate(Hibernate.INTEGER, "second(?1)") );
		registerFunction( "minute", new SQLFunctionTemplate(Hibernate.INTEGER, "minute(?1)") );
		registerFunction( "hour", new SQLFunctionTemplate(Hibernate.INTEGER, "hour(?1)") );
		registerFunction( "day", new SQLFunctionTemplate(Hibernate.INTEGER, "day(?1)") );
		registerFunction( "month", new SQLFunctionTemplate(Hibernate.INTEGER, "month(?1)") );
		registerFunction( "year", new SQLFunctionTemplate(Hibernate.INTEGER, "year(?1)") );

		registerFunction( "extract", new SQLFunctionTemplate(Hibernate.INTEGER, "?1(?3)") );

		registerFunction("dayname", new StandardSQLFunction("dayname", Hibernate.STRING) );
		registerFunction("monthname", new StandardSQLFunction("monthname", Hibernate.STRING) );
		registerFunction("dayofmonth", new StandardSQLFunction("dayofmonth", Hibernate.INTEGER) );
		registerFunction("dayofweek", new StandardSQLFunction("dayofweek", Hibernate.INTEGER) );
		registerFunction("dayofyear", new StandardSQLFunction("dayofyear", Hibernate.INTEGER) );
		registerFunction("weekofyear", new StandardSQLFunction("weekofyear", Hibernate.INTEGER) );

		registerFunction( "replace", new StandardSQLFunction("replace", Hibernate.STRING) );
		registerFunction( "translate", new StandardSQLFunction("translate", Hibernate.STRING) );
		registerFunction( "lpad", new StandardSQLFunction("lpad", Hibernate.STRING) );
		registerFunction( "rpad", new StandardSQLFunction("rpad", Hibernate.STRING) );
		registerFunction( "substr", new StandardSQLFunction("substr", Hibernate.STRING) );
		registerFunction( "initcap", new StandardSQLFunction("initcap", Hibernate.STRING) );
		registerFunction( "lower", new StandardSQLFunction("lower", Hibernate.STRING) );
		registerFunction( "ltrim", new StandardSQLFunction("ltrim", Hibernate.STRING) );
		registerFunction( "rtrim", new StandardSQLFunction("rtrim", Hibernate.STRING) );
		registerFunction( "lfill", new StandardSQLFunction("ltrim", Hibernate.STRING) );
		registerFunction( "rfill", new StandardSQLFunction("rtrim", Hibernate.STRING) );
		registerFunction( "soundex", new StandardSQLFunction("soundex", Hibernate.STRING) );
		registerFunction( "upper", new StandardSQLFunction("upper", Hibernate.STRING) );
		registerFunction( "ascii", new StandardSQLFunction("ascii", Hibernate.STRING) );
		registerFunction( "index", new StandardSQLFunction("index", Hibernate.INTEGER) );

		registerFunction( "value", new StandardSQLFunction( "value" ) );
		
		registerFunction( "concat", new VarArgsSQLFunction( Hibernate.STRING, "(", "||", ")" ) );
		registerFunction( "substring", new StandardSQLFunction( "substr", Hibernate.STRING ) );
		registerFunction( "locate", new StandardSQLFunction("index", Hibernate.INTEGER) );
		registerFunction( "coalesce", new StandardSQLFunction( "value" ) );

		getDefaultProperties().setProperty(Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE);

	}

	public boolean dropConstraints() {
		return false;
	}

	public String getAddColumnString() {
		return "add";
	}

	public String getAddForeignKeyConstraintString(
			String constraintName, 
			String[] foreignKey, 
			String referencedTable, 
			String[] primaryKey, boolean referencesPrimaryKey
	) {
		StringBuffer res = new StringBuffer(30)
			.append(" foreign key ")
			.append(constraintName)
			.append(" (")
			.append( StringHelper.join(", ", foreignKey) )
			.append(") references ")
			.append(referencedTable);
		
		if(!referencesPrimaryKey) {
			res.append(" (")
			   .append( StringHelper.join(", ", primaryKey) )
			   .append(')');
		}
			
		return res.toString();
	}

	public String getAddPrimaryKeyConstraintString(String constraintName) {
		return " primary key ";
	}

	public String getNullColumnString() {
		return " null";
	}

	public String getSequenceNextValString(String sequenceName) {
		return "select " + getSelectSequenceNextValString( sequenceName ) + " from dual";
	}

	public String getSelectSequenceNextValString(String sequenceName) {
		return sequenceName + ".nextval";
	}

	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName;
	}

	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName;
	}

	public String getQuerySequencesString() {
		return "select sequence_name from domain.sequences";
	}

	public boolean supportsSequences() {
		return true;
	}

	public CaseFragment createCaseFragment() {
		return new DecodeCaseFragment();
	}

	public boolean supportsTemporaryTables() {
		return true;
	}

	public String getCreateTemporaryTablePostfix() {
		return "ignore rollback";
	}

	public String generateTemporaryTableName(String baseTableName) {
		return "temp." + super.generateTemporaryTableName(baseTableName);
	}

}
