//$Id: PropertyAccessor.java 3890 2004-06-03 16:31:32Z steveebersole $
package org.hibernate.property;

import org.hibernate.PropertyNotFoundException;

/**
 * Abstracts the notion of a "property". Defines a strategy for accessing the
 * value of an attribute.
 * @author Gavin King
 */
public interface PropertyAccessor {
	/**
	 * Create a "getter" for the named attribute
	 */
	public Getter getGetter(Class theClass, String propertyName) throws PropertyNotFoundException;
	/**
	 * Create a "setter" for the named attribute
	 */
	public Setter getSetter(Class theClass, String propertyName) throws PropertyNotFoundException;
}
