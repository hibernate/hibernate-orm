//$Id: MckoiDialect.java 9328 2006-02-23 17:32:47Z steveebersole $
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.UpdateLockingStrategy;
import org.hibernate.dialect.lock.SelectLockingStrategy;
import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.MckoiCaseFragment;

/**
 * An SQL dialect compatible with McKoi SQL
 * @author Doug Currie, Gabe Hicks
 */
public class MckoiDialect extends Dialect {
	public MckoiDialect() {
		super();
		registerColumnType( Types.BIT, "bit" );
		registerColumnType( Types.BIGINT, "bigint" );
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.TINYINT, "tinyint" );
		registerColumnType( Types.INTEGER, "integer" );
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.DOUBLE, "double" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.VARBINARY, "varbinary" );
		registerColumnType( Types.NUMERIC, "numeric" );
		registerColumnType( Types.BLOB, "blob" );
		registerColumnType( Types.CLOB, "clob" );

		registerFunction( "upper", new StandardSQLFunction("upper") );
		registerFunction( "lower", new StandardSQLFunction("lower") );
		registerFunction( "sqrt", new StandardSQLFunction("sqrt", Hibernate.DOUBLE) );
		registerFunction( "abs", new StandardSQLFunction("abs") );
		registerFunction( "sign", new StandardSQLFunction( "sign", Hibernate.INTEGER ) );
		registerFunction( "length", new StandardSQLFunction( "length", Hibernate.INTEGER ) );
		registerFunction( "round", new StandardSQLFunction( "round", Hibernate.INTEGER ) );
		registerFunction( "mod", new StandardSQLFunction( "mod", Hibernate.INTEGER ) );
		registerFunction( "least", new StandardSQLFunction("least") );
		registerFunction( "greatest", new StandardSQLFunction("greatest") );
		registerFunction( "user", new StandardSQLFunction( "user", Hibernate.STRING ) );
		registerFunction( "concat", new StandardSQLFunction( "concat", Hibernate.STRING ) );

		getDefaultProperties().setProperty(Environment.STATEMENT_BATCH_SIZE, NO_BATCH);
	}

	public String getAddColumnString() {
		return "add column";
	}

	public String getSequenceNextValString(String sequenceName) {
		return "select " + getSelectSequenceNextValString( sequenceName );
	}

	public String getSelectSequenceNextValString(String sequenceName) {
		return "nextval('" + sequenceName + "')";
	}

	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName;
	}

	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName;
	}

	public String getForUpdateString() {
		return "";
	}

	public boolean supportsSequences() {
		return true;
	}

	public CaseFragment createCaseFragment() {
		return new MckoiCaseFragment();
	}

	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		// Mckoi has no known variation of a "SELECT ... FOR UPDATE" syntax...
		if ( lockMode.greaterThan( LockMode.READ ) ) {
			return new UpdateLockingStrategy( lockable, lockMode );
		}
		else {
			return new SelectLockingStrategy( lockable, lockMode );
		}
	}
}
