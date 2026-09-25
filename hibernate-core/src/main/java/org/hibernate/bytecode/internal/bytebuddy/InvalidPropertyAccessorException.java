package org.hibernate.bytecode.internal.bytebuddy;

import org.hibernate.HibernateException;

public class InvalidPropertyAccessorException extends HibernateException {

	public InvalidPropertyAccessorException(String message) {
		super( message );
	}
}
