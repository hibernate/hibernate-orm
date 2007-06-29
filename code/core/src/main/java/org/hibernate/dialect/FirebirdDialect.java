//$Id: FirebirdDialect.java 4202 2004-08-09 06:38:52Z oneovthafew $
package org.hibernate.dialect;

/**
 * An SQL dialect for Firebird.
 * @author Reha CENANI
 */
public class FirebirdDialect extends InterbaseDialect {

	public String getDropSequenceString(String sequenceName) {
		return "drop generator " + sequenceName;
	}

	public String getLimitString(String sql, boolean hasOffset) {
		return new StringBuffer( sql.length()+20 )
			.append(sql)
			.insert(6, hasOffset ? " first ? skip ?" : " first ?")
			.toString();
	}

	public boolean bindLimitParametersFirst() {
		return true;
	}

	public boolean bindLimitParametersInReverseOrder() {
		return true;
	}

}