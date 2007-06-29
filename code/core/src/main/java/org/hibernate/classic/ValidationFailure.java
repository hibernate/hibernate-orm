//$Id: ValidationFailure.java 4112 2004-07-28 03:33:35Z oneovthafew $
package org.hibernate.classic;

import org.hibernate.HibernateException;

/**
 * Thrown from <tt>Validatable.validate()</tt> when an invariant
 * was violated. Some applications might subclass this exception
 * in order to provide more information about the violation.
 *
 * @author Gavin King
 */
public class ValidationFailure extends HibernateException {

	public ValidationFailure(String message) {
		super(message);
	}

	public ValidationFailure(String message, Exception e) {
		super(message, e);
	}

	public ValidationFailure(Exception e) {
		super("A validation failure occurred", e);
	}

}






