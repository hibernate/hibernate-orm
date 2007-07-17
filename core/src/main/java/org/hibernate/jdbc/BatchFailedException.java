package org.hibernate.jdbc;

import org.hibernate.HibernateException;

/**
 * Indicates a failed batch entry (-3 return).
 *
 * @author Steve Ebersole
 */
public class BatchFailedException extends HibernateException {
	public BatchFailedException(String s) {
		super( s );
	}

	public BatchFailedException(String string, Throwable root) {
		super( string, root );
	}
}
