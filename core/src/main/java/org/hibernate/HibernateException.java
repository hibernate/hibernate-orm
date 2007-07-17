//$Id: HibernateException.java 5683 2005-02-12 03:09:22Z oneovthafew $
package org.hibernate;

import org.hibernate.exception.NestableRuntimeException;

/**
 * Any exception that occurs inside the persistence layer
 * or JDBC driver. <tt>SQLException</tt>s are always wrapped
 * by instances of <tt>JDBCException</tt>.
 *
 * @see JDBCException
 * @author Gavin King
 */

public class HibernateException extends NestableRuntimeException {

	public HibernateException(Throwable root) {
		super(root);
	}

	public HibernateException(String string, Throwable root) {
		super(string, root);
	}

	public HibernateException(String s) {
		super(s);
	}
}






