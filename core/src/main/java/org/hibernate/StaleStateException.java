//$Id: StaleStateException.java 5685 2005-02-12 07:19:50Z steveebersole $
package org.hibernate;

/**
 * Thrown when a version number or timestamp check failed, indicating that the
 * <tt>Session</tt> contained stale data (when using long transactions
 * with versioning). Also occurs if we try delete or update a row that does
 * not exist.<br>
 * <br>
 * Note that this exception often indicates that the user failed to specify the
 * correct <tt>unsaved-value</tt> strategy for a class!
 *
 * @see StaleObjectStateException
 * @author Gavin King
 */
public class StaleStateException extends HibernateException {

	public StaleStateException(String s) {
		super(s);
	}
}
