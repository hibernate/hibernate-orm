package org.hibernate.envers.exception;

import org.hibernate.HibernateException;

/**
 * @author Hern&aacute;n Chanfreau 
 */
public class EnversException extends HibernateException {

	private static final long serialVersionUID = 7117102779944317920L;

	public EnversException(String message) {
        super(message);
    }

    public EnversException(String message, Throwable cause) {
        super(message, cause);
    }

    public EnversException(Throwable cause) {
        super(cause);
    }	
}
