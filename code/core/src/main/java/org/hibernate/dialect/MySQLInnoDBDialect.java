//$Id: MySQLInnoDBDialect.java 7118 2005-06-12 21:55:12Z oneovthafew $
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
