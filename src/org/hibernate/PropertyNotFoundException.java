//$Id$
package org.hibernate;

/**
 * Indicates that an expected getter or setter method could not be
 * found on a class.
 *
 * @author Gavin King
 */
public class PropertyNotFoundException extends MappingException {

	public PropertyNotFoundException(String s) {
		super(s);
	}

}






