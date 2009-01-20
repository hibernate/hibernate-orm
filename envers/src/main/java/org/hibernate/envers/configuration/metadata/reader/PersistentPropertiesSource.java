package org.hibernate.envers.configuration.metadata.reader;

import org.hibernate.mapping.Property;
import org.hibernate.annotations.common.reflection.XClass;

import java.util.Iterator;

/**
 * A source of data on persistent properties of a class or component.
 * @author Adam Warski (adam at warski dot org)
 */
public interface PersistentPropertiesSource {
	Iterator<Property> getPropertyIterator();
	Property getProperty(String propertyName);
	XClass getXClass();
}
