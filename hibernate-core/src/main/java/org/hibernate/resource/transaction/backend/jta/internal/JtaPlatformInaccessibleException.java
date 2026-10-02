package org.hibernate.resource.transaction.backend.jta.internal;

import org.hibernate.HibernateException;

/**
 * Indicates problems accessing TransactionManager or UserTransaction through the JtaPlatform
 *
 * @author Steve Ebersole
 */
public class JtaPlatformInaccessibleException extends HibernateException {
	public JtaPlatformInaccessibleException(String message) {
		super( message );
	}

	public JtaPlatformInaccessibleException(String message, Throwable cause) {
		super( message, cause );
	}
}
