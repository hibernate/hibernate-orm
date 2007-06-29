//$Id$
package org.hibernate.dialect;

/**
 * @author Gavin King
 */
public class MySQLMyISAMDialect extends MySQLDialect {

	public String getTableTypeString() {
		return " type=MyISAM";
	}

	public boolean dropConstraints() {
		return false;
	}

}
