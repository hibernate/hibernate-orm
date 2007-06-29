//$Id: TransactionException.java 10312 2006-08-23 12:43:54Z steve.ebersole@jboss.com $
package org.hibernate;

/**
 * Indicates that a transaction could not be begun, committed
 * or rolled back.
 *
 * @see Transaction
 * @author Anton van Straaten
 */

public class TransactionException extends HibernateException {

	public TransactionException(String message, Throwable root) {
		super(message,root);
	}

	public TransactionException(String message) {
		super(message);
	}

}
