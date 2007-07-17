//$Id: InterbaseDialect.java 7746 2005-08-03 23:29:32Z oneovthafew $
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.Hibernate;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.VarArgsSQLFunction;

/**
 * An SQL dialect for Interbase.
 * @author Gavin King
 */
public class InterbaseDialect extends Dialect {

	public InterbaseDialect() {
		super();
		registerColumnType( Types.BIT, "smallint" );
		registerColumnType( Types.BIGINT, "numeric(18,0)" );
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.TINYINT, "smallint" );
		registerColumnType( Types.INTEGER, "integer" );
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.DOUBLE, "double precision" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.VARBINARY, "blob" );
		registerColumnType( Types.NUMERIC, "numeric($p,$s)" );
		registerColumnType( Types.BLOB, "blob" );
		registerColumnType( Types.CLOB, "blob sub_type 1" );
		
		registerFunction( "concat", new VarArgsSQLFunction( Hibernate.STRING, "(","||",")" ) );

		getDefaultProperties().setProperty(Environment.STATEMENT_BATCH_SIZE, NO_BATCH);
	}

	public String getAddColumnString() {
		return "add";
	}

	public String getSequenceNextValString(String sequenceName) {
		return "select " + getSelectSequenceNextValString( sequenceName ) + " from RDB$DATABASE";
	}

	public String getSelectSequenceNextValString(String sequenceName) {
		return "gen_id( " + sequenceName + ", 1 )";
	}

	public String getCreateSequenceString(String sequenceName) {
		return "create generator " + sequenceName;
	}

	public String getDropSequenceString(String sequenceName) {
		return "delete from RDB$GENERATORS where RDB$GENERATOR_NAME = '" + sequenceName.toUpperCase() + "'";
	}

	public String getQuerySequencesString() {
		return "select RDB$GENERATOR_NAME from RDB$GENERATORS";
	}
	
	public String getForUpdateString() {
		return " with lock";
	}
	public String getForUpdateString(String aliases) {
		return " for update of " + aliases + " with lock";
	}

	public boolean supportsSequences() {
		return true;
	}

	public boolean supportsLimit() {
		return true;
	}

	public String getLimitString(String sql, boolean hasOffset) {
		return new StringBuffer( sql.length()+15 )
			.append(sql)
			.append(hasOffset ? " rows ? to ?" : " rows ?")
			.toString();
	}

	public boolean bindLimitParametersFirst() {
		return false;
	}

	public boolean bindLimitParametersInReverseOrder() {
		return false;
	}

	public String getCurrentTimestampCallString() {
		// TODO : not sure which (either?) is correct, could not find docs on how to do this.
		// did find various blogs and forums mentioning that select CURRENT_TIMESTAMP
		// does not work...
		return "{?= call CURRENT_TIMESTAMP }";
//		return "select CURRENT_TIMESTAMP from RDB$DATABASE";
	}

	public boolean isCurrentTimestampSelectStringCallable() {
		return true;
	}
}