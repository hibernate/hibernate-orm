package org.hibernate.procedure;

import org.hibernate.HibernateException;

/**
 * Thrown to indicate a misuse of a parameter
 *
 * @author Steve Ebersole
 */
public class ParameterMisuseException extends HibernateException {
	public ParameterMisuseException(String message) {
		super( message );
	}
}
