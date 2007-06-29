//$Id: ObjectNotFoundException.java 9855 2006-05-02 18:55:45Z steve.ebersole@jboss.com $
package org.hibernate;

import java.io.Serializable;

/**
 * Thrown when <tt>Session.load()</tt> fails to select a row with
 * the given primary key (identifier value). This exception might not
 * be thrown when <tt>load()</tt> is called, even if there was no
 * row on the database, because <tt>load()</tt> returns a proxy if
 * possible. Applications should use <tt>Session.get()</tt> to test if
 * a row exists in the database.<br>
 * <br> 
 * Like all Hibernate exceptions, this exception is considered 
 * unrecoverable.
 *
 * @author Gavin King
 */
public class ObjectNotFoundException extends UnresolvableObjectException {

	public ObjectNotFoundException(Serializable identifier, String clazz) {
		super(identifier, clazz);
	}
}
