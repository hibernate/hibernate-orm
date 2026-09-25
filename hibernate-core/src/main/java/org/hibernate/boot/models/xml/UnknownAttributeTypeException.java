package org.hibernate.boot.models.xml;

import org.hibernate.HibernateException;

/**
 * Indicates a problem resolving an attribute's type details - typically with dynamic models.
 *
 * @author Steve Ebersole
 */
public class UnknownAttributeTypeException extends HibernateException {
	public UnknownAttributeTypeException(String message) {
		super( message );
	}
}
