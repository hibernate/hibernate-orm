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

import java.io.Serializable;
import java.lang.reflect.Method;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.bytecode.BasicProxyFactory;
import org.hibernate.bytecode.ReflectionOptimizer;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.property.BackrefPropertyAccessor;
import org.hibernate.property.Getter;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.PropertyAccessorFactory;
import org.hibernate.property.Setter;
import org.hibernate.tuple.Instantiator;
import org.hibernate.tuple.PojoInstantiator;
import org.hibernate.util.ReflectHelper;

/**
 * A {@link ComponentTuplizer} specific to the pojo entity mode.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class PojoComponentTuplizer extends AbstractComponentTuplizer {
	private final Class componentClass;
	private ReflectionOptimizer optimizer;
	private final Getter parentGetter;
	private final Setter parentSetter;

	public PojoComponentTuplizer(Component component) {
		super( component );

		this.componentClass = component.getComponentClass();

		String[] getterNames = new String[propertySpan];
		String[] setterNames = new String[propertySpan];
		Class[] propTypes = new Class[propertySpan];
		for ( int i = 0; i < propertySpan; i++ ) {
			getterNames[i] = getters[i].getMethodName();
			setterNames[i] = setters[i].getMethodName();
			propTypes[i] = getters[i].getReturnType();
		}

		final String parentPropertyName = component.getParentProperty();
		if ( parentPropertyName == null ) {
			parentSetter = null;
			parentGetter = null;
		}
		else {
			PropertyAccessor pa = PropertyAccessorFactory.getPropertyAccessor( null );
			parentSetter = pa.getSetter( componentClass, parentPropertyName );
			parentGetter = pa.getGetter( componentClass, parentPropertyName );
		}

		if ( hasCustomAccessors || !Environment.useReflectionOptimizer() ) {
			optimizer = null;
		}
		else {
			// TODO: here is why we need to make bytecode provider global :(
			// TODO : again, fix this after HHH-1907 is complete
			optimizer = Environment.getBytecodeProvider().getReflectionOptimizer(
					componentClass, getterNames, setterNames, propTypes
			);
		}
	}

	public Class getMappedClass() {
		return componentClass;
	}

	public Object[] getPropertyValues(Object component) throws HibernateException {
		if ( component == BackrefPropertyAccessor.UNKNOWN ) {
			return new Object[propertySpan];
		}
		if ( optimizer != null && optimizer.getAccessOptimizer() != null ) {
			return optimizer.getAccessOptimizer().getPropertyValues( component );
		}
		else {
			return super.getPropertyValues( component );
		}
	}

	public void setPropertyValues(Object component, Object[] values) throws HibernateException {
		if ( optimizer != null && optimizer.getAccessOptimizer() != null ) {
			optimizer.getAccessOptimizer().setPropertyValues( component, values );
		}
		else {
			super.setPropertyValues( component, values );
		}
	}

	public Object getParent(Object component) {
		return parentGetter.get( component );
	}

	public boolean hasParentProperty() {
		return parentGetter != null;
	}

	public boolean isMethodOf(Method method) {
		for ( int i = 0; i < propertySpan; i++ ) {
			final Method getterMethod = getters[i].getMethod();
			if ( getterMethod != null && getterMethod.equals( method ) ) {
				return true;
			}
		}
		return false;
	}

	public void setParent(Object component, Object parent, SessionFactoryImplementor factory) {
		parentSetter.set( component, parent, factory );
	}

	protected Instantiator buildInstantiator(Component component) {
		if ( component.isEmbedded() && ReflectHelper.isAbstractClass( component.getComponentClass() ) ) {
			return new ProxiedInstantiator( component );
		}
		if ( optimizer == null ) {
			return new PojoInstantiator( component, null );
		}
		else {
			return new PojoInstantiator( component, optimizer.getInstantiationOptimizer() );
		}
	}

	protected Getter buildGetter(Component component, Property prop) {
		return prop.getGetter( component.getComponentClass() );
	}

	protected Setter buildSetter(Component component, Property prop) {
		return prop.getSetter( component.getComponentClass() );
	}

	private static class ProxiedInstantiator implements Instantiator {
		private final Class proxiedClass;
		private final BasicProxyFactory factory;

		public ProxiedInstantiator(Component component) {
			proxiedClass = component.getComponentClass();
			if ( proxiedClass.isInterface() ) {
				factory = Environment.getBytecodeProvider()
						.getProxyFactoryFactory()
						.buildBasicProxyFactory( null, new Class[] { proxiedClass } );
			}
			else {
				factory = Environment.getBytecodeProvider()
						.getProxyFactoryFactory()
						.buildBasicProxyFactory( proxiedClass, null );
			}
		}

		public Object instantiate(Serializable id) {
			throw new AssertionFailure( "ProxiedInstantiator can only be used to instantiate component" );
		}

		public Object instantiate() {
			return factory.getProxy();
		}

		public boolean isInstance(Object object) {
			return proxiedClass.isInstance( object );
		}
	}
}
