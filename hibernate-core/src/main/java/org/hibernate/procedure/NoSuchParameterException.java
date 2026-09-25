package org.hibernate.procedure;

import org.hibernate.HibernateException;

/**
 * @author Steve Ebersole
 */
public class NoSuchParameterException extends HibernateException {
	public NoSuchParameterException(String message) {
		super( message );
	}
}
