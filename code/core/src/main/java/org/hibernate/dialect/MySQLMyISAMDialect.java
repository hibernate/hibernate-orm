//$Id: MySQLMyISAMDialect.java 5685 2005-02-12 07:19:50Z steveebersole $
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
