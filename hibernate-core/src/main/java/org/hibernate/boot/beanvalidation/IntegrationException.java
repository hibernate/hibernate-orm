package org.hibernate.boot.beanvalidation;

import org.hibernate.HibernateException;

/**
 * Indicates a problem integrating Hibernate and the Bean Validation spec.
 *
 * @author Steve Ebersole
 */
public class IntegrationException extends HibernateException {
	public IntegrationException(String message) {
		super( message );
	}

	public IntegrationException(String message, Throwable root) {
		super( message, root );
	}
}
