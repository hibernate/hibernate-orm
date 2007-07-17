//$Id: PropertyAccessException.java 3890 2004-06-03 16:31:32Z steveebersole $
package org.hibernate;

import org.hibernate.util.StringHelper;

/**
 * A problem occurred accessing a property of an instance of a
 * persistent class by reflection, or via CGLIB. There are a
 * number of possible underlying causes, including
 * <ul>
 * <li>failure of a security check
 * <li>an exception occurring inside the getter or setter method
 * <li>a nullable database column was mapped to a primitive-type property
 * <li>the Hibernate type was not castable to the property type (or vice-versa)
 * </ul>
 * @author Gavin King
 */
public class PropertyAccessException extends HibernateException {

	private final Class persistentClass;
	private final String propertyName;
	private final boolean wasSetter;

	public PropertyAccessException(Throwable root, String s, boolean wasSetter, Class persistentClass, String propertyName) {
		super(s, root);
		this.persistentClass = persistentClass;
		this.wasSetter = wasSetter;
		this.propertyName = propertyName;
	}

	public Class getPersistentClass() {
		return persistentClass;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public String getMessage() {
		return super.getMessage() +
		( wasSetter ? " setter of " : " getter of ") +
		StringHelper.qualify( persistentClass.getName(), propertyName );
	}
}






