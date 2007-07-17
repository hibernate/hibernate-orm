//$Id: WrongClassException.java 5685 2005-02-12 07:19:50Z steveebersole $
package org.hibernate;

import java.io.Serializable;

/**
 * Thrown when <tt>Session.load()</tt> selects a row with
 * the given primary key (identifier value) but the row's
 * discriminator value specifies a subclass that is not
 * assignable to the class requested by the user.
 *
 * @author Gavin King
 */
public class WrongClassException extends HibernateException {

	private final Serializable identifier;
	private final String entityName;

	public WrongClassException(String msg, Serializable identifier, String clazz) {
		super(msg);
		this.identifier = identifier;
		this.entityName = clazz;
	}
	public Serializable getIdentifier() {
		return identifier;
	}

	public String getMessage() {
		return "Object with id: " +
			identifier +
			" was not of the specified subclass: " +
			entityName +
			" (" + super.getMessage() + ")" ;
	}

	public String getEntityName() {
		return entityName;
	}

}







