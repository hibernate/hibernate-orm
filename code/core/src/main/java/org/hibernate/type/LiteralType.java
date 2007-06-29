//$Id: LiteralType.java 7825 2005-08-10 20:23:55Z oneovthafew $
package org.hibernate.type;

import org.hibernate.dialect.Dialect;

/**
 * A type that may appear as an SQL literal
 * @author Gavin King
 */
public interface LiteralType {
	/**
	 * String representation of the value, suitable for embedding in
	 * an SQL statement.
	 * @param value
	 * @param dialect
	 * @return String the value, as it appears in a SQL query
	 * @throws Exception
	 */
	public String objectToSQLString(Object value, Dialect dialect) throws Exception;

}






