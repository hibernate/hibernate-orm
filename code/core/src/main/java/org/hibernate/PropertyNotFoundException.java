//$Id: PropertyNotFoundException.java 3890 2004-06-03 16:31:32Z steveebersole $
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






