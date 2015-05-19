/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal.metadata.reader;

import java.util.Iterator;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.mapping.Property;

/**
 * A source of data on persistent properties of a class or component.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public interface PersistentPropertiesSource {
	Iterator<Property> getPropertyIterator();

	Property getProperty(String propertyName);

	XClass getXClass();
}
