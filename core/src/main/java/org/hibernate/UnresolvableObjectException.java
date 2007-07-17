//$Id: UnresolvableObjectException.java 5685 2005-02-12 07:19:50Z steveebersole $
package org.hibernate;

import java.io.Serializable;

import org.hibernate.pretty.MessageHelper;

/**
 * Thrown when Hibernate could not resolve an object by id, especially when
 * loading an association.
 *
 * @author Gavin King
 */
public class UnresolvableObjectException extends HibernateException {

	private final Serializable identifier;
	private final String entityName;

	public UnresolvableObjectException(Serializable identifier, String clazz) {
		this("No row with the given identifier exists", identifier, clazz);
	}
	UnresolvableObjectException(String message, Serializable identifier, String clazz) {
		super(message);
		this.identifier = identifier;
		this.entityName = clazz;
	}
	public Serializable getIdentifier() {
		return identifier;
	}

	public String getMessage() {
		return super.getMessage() + ": " +
			MessageHelper.infoString(entityName, identifier);
	}

	public String getEntityName() {
		return entityName;
	}

	public static void throwIfNull(Object o, Serializable id, String clazz)
	throws UnresolvableObjectException {
		if (o==null) throw new UnresolvableObjectException(id, clazz);
	}

}







