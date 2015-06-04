/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple.component;
import java.io.Serializable;
import java.lang.reflect.Method;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.bytecode.spi.BasicProxyFactory;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyBasicImpl;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tuple.Instantiator;
import org.hibernate.tuple.PojoInstantiator;

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
			final ServiceRegistry serviceRegistry =
					component.getMetadata().getMetadataBuildingOptions().getServiceRegistry();
			final PropertyAccess propertyAccess = PropertyAccessStrategyBasicImpl.INSTANCE.buildPropertyAccess(
					componentClass,
					parentPropertyName
			);
			parentSetter = propertyAccess.getSetter();
			parentGetter = propertyAccess.getGetter();
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
		if ( component == PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
			return new Object[propertySpan];
		}
		else if ( optimizer != null && optimizer.getAccessOptimizer() != null ) {
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
