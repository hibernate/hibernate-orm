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
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.bytecode.spi.BasicProxyFactory;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.mapping.Component;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.EmbeddableBinding;
import org.hibernate.metamodel.spi.binding.EntityIdentifier;
import org.hibernate.property.BackrefPropertyAccessor;
import org.hibernate.property.Getter;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.PropertyAccessorFactory;
import org.hibernate.property.Setter;
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

	public PojoComponentTuplizer(
			ServiceRegistry serviceRegistry,
			EmbeddableBinding component,
			boolean isIdentifierMapper) {
		super( serviceRegistry, component, isIdentifierMapper );

		final ClassLoaderService cls = serviceRegistry.getService( ClassLoaderService.class );
		if ( isIdentifierMapper ) {
			final EntityIdentifier idInfo = component.seekEntityBinding()
					.getHierarchyDetails()
					.getEntityIdentifier();
			final EntityIdentifier.NonAggregatedCompositeIdentifierBinding idBinding =
					(EntityIdentifier.NonAggregatedCompositeIdentifierBinding) idInfo.getEntityIdentifierBinding();
			final EntityIdentifier.IdClassMetadata idClassMetadata = idBinding.getIdClassMetadata();
			this.componentClass = cls.classForName(
					idClassMetadata.getIdClassType().getName().toString()
			);
		}
		else {
			this.componentClass = cls.classForName(
					component.getAttributeContainer().getDescriptor().getName().toString()
			);
		}

		String[] getterNames = new String[ propertySpan() ];
		String[] setterNames = new String[ propertySpan() ];
		Class[] propTypes = new Class[ propertySpan() ];
		for ( int i = 0; i < propertySpan(); i++ ) {
			getterNames[i] = getters()[i].getMethodName();
			setterNames[i] = setters()[i].getMethodName();
			propTypes[i] = getters()[i].getReturnType();
		}

		final String parentPropertyName =
				component.getParentReference() == null ? null : component.getParentReference().getName();
		if ( parentPropertyName == null ) {
			parentSetter = null;
			parentGetter = null;
		}
		else {
			PropertyAccessor pa = PropertyAccessorFactory.getPropertyAccessor( null );
			parentSetter = pa.getSetter( componentClass, parentPropertyName );
			parentGetter = pa.getGetter( componentClass, parentPropertyName );
		}

		if ( hasCustomAccessors() || !Environment.useReflectionOptimizer() ) {
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
			return new Object[propertySpan()];
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
		for ( int i = 0; i < propertySpan(); i++ ) {
			final Method getterMethod = getters()[i].getMethod();
			if ( getterMethod != null && getterMethod.equals( method ) ) {
				return true;
			}
		}
		return false;
	}

	public void setParent(Object component, Object parent, SessionFactoryImplementor factory) {
		parentSetter.set( component, parent, factory );
	}

	@Override
	protected Instantiator buildInstantiator(
			EmbeddableBinding embeddableBinding,
			boolean isIdentifierMapper) {
		final Class clazz = classForName(
				embeddableBinding.getAttributeContainer().getDescriptor().getName().toString()
		);

		if ( !embeddableBinding.isAggregated() && ReflectHelper.isAbstractClass( clazz ) ) {
			return new ProxiedInstantiator( clazz );
		}

		if ( optimizer == null ) {
			return new PojoInstantiator(
					serviceRegistry(),
					embeddableBinding,
					isIdentifierMapper,
					null
			);
		}
		else {
			return new PojoInstantiator(
					serviceRegistry(),
					embeddableBinding,
					isIdentifierMapper,
					optimizer.getInstantiationOptimizer()
			);
		}
	}

	@Override
	protected Getter buildGetter(
			EmbeddableBinding embeddableBinding,
			boolean isIdentifierMapper,
			AttributeBinding attributeBinding) {
		// TODO: when compositeAttributeBinding is wrapped for an identifier mapper
		//       there will be no need for PropertyFactory.getIdentifierMapperGetter()
		//       and PropertyFactory.getIdentifierMapperSetter

		// this.componentClass is not set yet because of ctor calls - yucky bad design

		if ( isIdentifierMapper ) {
			// (steve w/ metamodel) : utterly confused here.  What exactly is the
			//		thing we are trying to accomplish here?  it *seems* like we
			// 		are trying to build a getter for the id class (isIdentifierMapper == true),
			// 		so why do we pass in the composite representing	the virtual,
			// 		non-aggregated id?  why not just pass in the IdClass composite?
			// 		so confusing :)

			final EntityIdentifier idInfo = embeddableBinding.seekEntityBinding()
					.getHierarchyDetails()
					.getEntityIdentifier();
			final EntityIdentifier.NonAggregatedCompositeIdentifierBinding idBinding =
					(EntityIdentifier.NonAggregatedCompositeIdentifierBinding) idInfo.getEntityIdentifierBinding();
			final EntityIdentifier.IdClassMetadata idClassMetadata = idBinding.getIdClassMetadata();

			final Class componentClass = classForName(
					idClassMetadata.getIdClassType().getName().toString()
			);
			return getGetter(
					componentClass,
					attributeBinding.getAttribute().getName(),
					PropertyAccessorFactory.getPropertyAccessor(
							idClassMetadata.getAccessStrategy( attributeBinding.getAttribute().getName() )
					)
			);
		}
		else {
			final Class clazz = classForName(
					embeddableBinding.getAttributeContainer().getDescriptor().getName().toString()
			);
			return getGetter(
					clazz,
					attributeBinding.getAttribute().getName(),
					PropertyAccessorFactory.getPropertyAccessor( attributeBinding, EntityMode.POJO )
			);
		}
	}

	@Override
	protected Setter buildSetter(
			EmbeddableBinding embeddableBinding,
			boolean isIdentifierMapper,
			AttributeBinding attributeBinding) {
		if ( isIdentifierMapper ) {
			// see discussion about confusing in #buildGetter

			final EntityIdentifier idInfo = embeddableBinding.seekEntityBinding()
					.getHierarchyDetails()
					.getEntityIdentifier();
			final EntityIdentifier.NonAggregatedCompositeIdentifierBinding idBinding =
					(EntityIdentifier.NonAggregatedCompositeIdentifierBinding) idInfo.getEntityIdentifierBinding();
			final EntityIdentifier.IdClassMetadata idClassMetadata = idBinding.getIdClassMetadata();

			final Class componentClass = classForName(
					idClassMetadata.getIdClassType().getName().toString()
			);
			return getSetter(
					componentClass,
					attributeBinding.getAttribute().getName(),
					PropertyAccessorFactory.getPropertyAccessor(
							idClassMetadata.getAccessStrategy( attributeBinding.getAttribute().getName() )
					)
			);
		}
		else {
			final Class clazz = classForName(
					embeddableBinding.getAttributeContainer().getDescriptor().getName().toString()
			);
			return getSetter(
					clazz,
					attributeBinding.getAttribute().getName(),
					PropertyAccessorFactory.getPropertyAccessor( attributeBinding, EntityMode.POJO )
			);
		}
	}

	private Getter getGetter(Class clazz, String name, PropertyAccessor propertyAccessor) {
		return propertyAccessor.getGetter( clazz, name );
	}

	private Setter getSetter(Class clazz, String name, PropertyAccessor propertyAccessor) {
		return propertyAccessor.getSetter(clazz, name);
	}

	private static class ProxiedInstantiator implements Instantiator {
		private final Class proxiedClass;
		private final BasicProxyFactory factory;

		public ProxiedInstantiator(Component component) {
			this( component.getComponentClass() );
		}

		private ProxiedInstantiator(Class<?> proxiedClass) {
			this.proxiedClass = proxiedClass;
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
