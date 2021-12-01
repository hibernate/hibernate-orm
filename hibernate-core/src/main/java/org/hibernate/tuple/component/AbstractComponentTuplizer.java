/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple.component;

import java.lang.reflect.Method;
import java.util.Iterator;

import org.hibernate.HibernateException;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.Setter;

/**
 * Support for tuplizers relating to components.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public abstract class AbstractComponentTuplizer implements ComponentTuplizer {
	protected final Getter[] getters;
	protected final Setter[] setters;
	protected final int propertySpan;
	protected final boolean hasCustomAccessors;

	protected abstract Getter buildGetter(Component component, Property prop);
	protected abstract Setter buildSetter(Component component, Property prop);

	protected AbstractComponentTuplizer(Component component) {
		setComponentClass( component );
		propertySpan = component.getPropertySpan();
		getters = new Getter[propertySpan];
		setters = new Setter[propertySpan];

		Iterator iter = component.getPropertyIterator();
		boolean foundCustomAccessor=false;
		int i = 0;
		while ( iter.hasNext() ) {
			Property prop = ( Property ) iter.next();
			getters[i] = buildGetter( component, prop );
			setters[i] = buildSetter( component, prop );
			if ( !prop.isBasicPropertyAccessor() ) {
				foundCustomAccessor = true;
			}
			i++;
		}
		hasCustomAccessors = foundCustomAccessor;
	}

	public Object getPropertyValue(Object component, int i) throws HibernateException {
		return getters[i].get( component );
	}

	public Object[] getPropertyValues(Object component) throws HibernateException {
		Object[] values = new Object[propertySpan];
		for ( int i = 0; i < propertySpan; i++ ) {
			values[i] = getPropertyValue( component, i );
		}
		return values;
	}

	public void setPropertyValues(Object component, Object[] values) throws HibernateException {
		for ( int i = 0; i < propertySpan; i++ ) {
			setters[i].set( component, values[i] );
		}
	}

	public boolean isMethodOf(Method method) {
		return false;
	}

	public Getter getGetter(int i) {
		return getters[i];
	}

	// It should be an abstract method but not sure if this can break any customer extension
	protected void setComponentClass(Component component){
	}
}
