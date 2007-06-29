//$Id: PointbaseDialect.java 9328 2006-02-23 17:32:47Z steveebersole $
//Created on 04 February 2002, 17:35
package org.hibernate.dialect;

import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.UpdateLockingStrategy;
import org.hibernate.dialect.lock.SelectLockingStrategy;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.LockMode;

import java.sql.Types;

/**
 * A <tt>Dialect</tt> for Pointbase.
 * @author  Ed Mackenzie
 */
public class PointbaseDialect extends org.hibernate.dialect.Dialect {

	/**
	 * Creates new PointbaseDialect
	 */
	public PointbaseDialect() {
		super();
		registerColumnType( Types.BIT, "smallint" ); //no pointbase BIT
		registerColumnType( Types.BIGINT, "bigint" );
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.TINYINT, "smallint" ); //no pointbase TINYINT
		registerColumnType( Types.INTEGER, "integer" );
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.DOUBLE, "double precision" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		//the BLOB type requires a size arguement - this defaults to
		//bytes - no arg defaults to 1 whole byte!
		//other argument mods include K - kilobyte, M - megabyte, G - gigabyte.
		//refer to the PBdevelopers guide for more info.
		registerColumnType( Types.VARBINARY, "blob($l)" );
		registerColumnType( Types.NUMERIC, "numeric($p,$s)" );
	}

	public String getAddColumnString() {
		return "add";
	}

	public boolean dropConstraints() {
		return false;
	}

	public String getCascadeConstraintsString() {
		return " cascade";
	}

	public String getForUpdateString() {
		return "";
	}

	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		// Pointbase has no known variation of a "SELECT ... FOR UPDATE" syntax...
		if ( lockMode.greaterThan( LockMode.READ ) ) {
			return new UpdateLockingStrategy( lockable, lockMode );
		}
		else {
			return new SelectLockingStrategy( lockable, lockMode );
		}
	}
}
