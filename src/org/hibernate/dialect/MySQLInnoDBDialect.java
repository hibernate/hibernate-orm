//$Id$
package org.hibernate.dialect;

/**
 * @author Gavin King
 */
public class MySQLInnoDBDialect extends MySQLDialect {

	public boolean supportsCascadeDelete() {
		return true;
	}
	
	public String getTableTypeString() {
		return " type=InnoDB";
	}

	public boolean hasSelfReferentialForeignKeyBug() {
		return true;
	}
	
}
