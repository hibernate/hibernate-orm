//$Id$
package org.hibernate;

import java.io.Serializable;

/**
 * Thrown when the user tries to do something illegal with a deleted
 * object.
 *
 * @author Gavin King
 */
public class ObjectDeletedException extends UnresolvableObjectException {

	public ObjectDeletedException(String message, Serializable identifier, String clazz) {
		super(message, identifier, clazz);
	}

}







