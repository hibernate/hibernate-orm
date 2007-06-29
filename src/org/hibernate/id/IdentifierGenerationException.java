//$Id$
package org.hibernate.id;

import org.hibernate.HibernateException;

/**
 * Thrown by <tt>IdentifierGenerator</tt> implementation class when
 * ID generation fails.
 *
 * @see IdentifierGenerator
 * @author Gavin King
 */

public class IdentifierGenerationException extends HibernateException {

	public IdentifierGenerationException(String msg) {
		super(msg);
	}

	public IdentifierGenerationException(String msg, Throwable t) {
		super(msg, t);
	}

}






