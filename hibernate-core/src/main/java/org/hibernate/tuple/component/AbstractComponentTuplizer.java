/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.tuple.component;
import java.lang.reflect.Method;
import java.util.Iterator;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.property.Getter;
import org.hibernate.property.Setter;
import org.hibernate.tuple.Instantiator;

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
	protected final Instantiator instantiator;
	protected final boolean hasCustomAccessors;

	protected abstract Instantiator buildInstantiator(Component component);
	protected abstract Getter buildGetter(Component component, Property prop);
	protected abstract Setter buildSetter(Component component, Property prop);

	protected AbstractComponentTuplizer(Component component) {
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
		instantiator = buildInstantiator( component );
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

	public boolean isInstance(Object object) {
		return instantiator.isInstance(object);
	}

	public void setPropertyValues(Object component, Object[] values) throws HibernateException {
		for ( int i = 0; i < propertySpan; i++ ) {
			setters[i].set( component, values[i], null );
		}
	}

	/**
	* This method does not populate the component parent
	*/
	public Object instantiate() throws HibernateException {
		return instantiator.instantiate();
	}

	public Object getParent(Object component) {
		return null;
	}

	public boolean hasParentProperty() {
		return false;
	}

	public boolean isMethodOf(Method method) {
		return false;
	}

	public void setParent(Object component, Object parent, SessionFactoryImplementor factory) {
		throw new UnsupportedOperationException();
	}

	public Getter getGetter(int i) {
		return getters[i];
	}
}
