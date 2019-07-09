/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple.component;
import java.util.Map;

import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.property.access.internal.PropertyAccessStrategyMapImpl;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.tuple.DynamicMapInstantiator;
import org.hibernate.tuple.Instantiator;

/**
 * A {@link ComponentTuplizer} specific to the dynamic-map entity mode.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class DynamicMapComponentTuplizer extends AbstractComponentTuplizer {

	public Class getMappedClass() {
		return Map.class;
	}

	protected Instantiator buildInstantiator(Component component) {
		return new DynamicMapInstantiator();
	}

	public DynamicMapComponentTuplizer(Component component) {
		super(component);
	}

	protected Getter buildGetter(Component component, Property prop) {
		return PropertyAccessStrategyMapImpl.INSTANCE.buildPropertyAccess( null, prop.getName() ).getGetter();
	}

	protected Setter buildSetter(Component component, Property prop) {
		return PropertyAccessStrategyMapImpl.INSTANCE.buildPropertyAccess( null, prop.getName() ).getSetter();
	}

}
