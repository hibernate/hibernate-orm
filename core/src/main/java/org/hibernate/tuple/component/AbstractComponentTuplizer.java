//$Id: AbstractComponentTuplizer.java 9210 2006-02-03 22:15:19Z steveebersole $
package org.hibernate.tuple.component;

import java.lang.reflect.Method;
import java.util.Iterator;

import org.hibernate.HibernateException;
import org.hibernate.tuple.Instantiator;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.property.Getter;
import org.hibernate.property.Setter;

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

		String[] getterNames = new String[propertySpan];
		String[] setterNames = new String[propertySpan];
		Class[] propTypes = new Class[propertySpan];
		for ( int j = 0; j < propertySpan; j++ ) {
			getterNames[j] = getters[j].getMethodName();
			setterNames[j] = setters[j].getMethodName();
			propTypes[j] = getters[j].getReturnType();
		}
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

}
