package org.hibernate.procedure;

import org.hibernate.HibernateException;

/**
 * @author Steve Ebersole
 */
public class ParameterStrategyException extends HibernateException {
	public ParameterStrategyException(String message) {
		super( message );
	}
}
