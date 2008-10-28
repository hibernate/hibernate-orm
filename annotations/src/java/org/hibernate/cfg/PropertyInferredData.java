//$Id$
package org.hibernate.cfg;

import org.hibernate.MappingException;
import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.Target;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;

/**
 * Retrieve all inferred data from an annnoted element
 *
 * @author Emmanuel Bernard
 * @author Paolo Perrotta
 */
public class PropertyInferredData implements PropertyData {
	private final String defaultAccess;

	private final XProperty property;
	private final ReflectionManager reflectionManager;

	/**
	 * Take the annoted element for lazy process
	 */
	public PropertyInferredData(XProperty property, String propertyAccessor, ReflectionManager reflectionManager) {
		this.property = property;
		this.defaultAccess = propertyAccessor;
		this.reflectionManager = reflectionManager;
	}

	public String getDefaultAccess() throws MappingException {
		// if(skip())
		// return defaultAccess;
		AccessType access = property.getAnnotation( AccessType.class );
		return access != null ? access.value() : defaultAccess;
	}

	public String getPropertyName() throws MappingException {
		return property.getName();
	}

	public XClass getPropertyClass() throws MappingException {
		if ( property.isAnnotationPresent( Target.class ) ) {
			return reflectionManager.toXClass( property.getAnnotation( Target.class ).value() );
		}
		else {
			return property.getType();
		}
	}

	public XClass getClassOrElement() throws MappingException {
		if ( property.isAnnotationPresent( Target.class ) ) {
			return reflectionManager.toXClass( property.getAnnotation( Target.class ).value() );
		}
		else {
			return property.getClassOrElementClass();
		}
	}

	public String getClassOrElementName() throws MappingException {
		return getClassOrElement().getName();
	}

	public String getTypeName() throws MappingException {
		return getPropertyClass().getName();
	}

	public XProperty getProperty() {
		return property;
	}
}
