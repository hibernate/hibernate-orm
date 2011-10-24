/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.tuple.entity;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;

import org.hibernate.EntityMode;
import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.bytecode.instrumentation.internal.FieldInterceptionHelper;
import org.hibernate.bytecode.instrumentation.spi.FieldInterceptor;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.cfg.Environment;
import org.hibernate.classic.Lifecycle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Subclass;
import org.hibernate.metamodel.binding.AttributeBinding;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.property.Getter;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.PropertyAccessorFactory;
import org.hibernate.property.Setter;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.tuple.Instantiator;
import org.hibernate.tuple.PojoInstantiator;
import org.hibernate.type.CompositeType;

/**
 * An {@link EntityTuplizer} specific to the pojo entity mode.
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class PojoEntityTuplizer extends AbstractEntityTuplizer {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, PojoEntityTuplizer.class.getName());

	private final Class mappedClass;
	private final Class proxyInterface;
	private final boolean lifecycleImplementor;
	private final Set lazyPropertyNames = new HashSet();
	private final ReflectionOptimizer optimizer;
	private final boolean isInstrumented;

	public PojoEntityTuplizer(EntityMetamodel entityMetamodel, PersistentClass mappedEntity) {
		super( entityMetamodel, mappedEntity );
		this.mappedClass = mappedEntity.getMappedClass();
		this.proxyInterface = mappedEntity.getProxyInterface();
		this.lifecycleImplementor = Lifecycle.class.isAssignableFrom( mappedClass );
		this.isInstrumented = entityMetamodel.isInstrumented();

		Iterator iter = mappedEntity.getPropertyClosureIterator();
		while ( iter.hasNext() ) {
			Property property = (Property) iter.next();
			if ( property.isLazy() ) {
				lazyPropertyNames.add( property.getName() );
			}
		}

		String[] getterNames = new String[propertySpan];
		String[] setterNames = new String[propertySpan];
		Class[] propTypes = new Class[propertySpan];
		for ( int i = 0; i < propertySpan; i++ ) {
			getterNames[i] = getters[i].getMethodName();
			setterNames[i] = setters[i].getMethodName();
			propTypes[i] = getters[i].getReturnType();
		}

		if ( hasCustomAccessors || !Environment.useReflectionOptimizer() ) {
			optimizer = null;
		}
		else {
			// todo : YUCK!!!
			optimizer = Environment.getBytecodeProvider().getReflectionOptimizer( mappedClass, getterNames, setterNames, propTypes );
//			optimizer = getFactory().getSettings().getBytecodeProvider().getReflectionOptimizer(
//					mappedClass, getterNames, setterNames, propTypes
//			);
		}

	}

	public PojoEntityTuplizer(EntityMetamodel entityMetamodel, EntityBinding mappedEntity) {
		super( entityMetamodel, mappedEntity );
		this.mappedClass = mappedEntity.getEntity().getClassReference();
		this.proxyInterface = mappedEntity.getProxyInterfaceType().getValue();
		this.lifecycleImplementor = Lifecycle.class.isAssignableFrom( mappedClass );
		this.isInstrumented = entityMetamodel.isInstrumented();

		for ( AttributeBinding property : mappedEntity.getAttributeBindingClosure() ) {
			if ( property.isLazy() ) {
				lazyPropertyNames.add( property.getAttribute().getName() );
			}
		}

		String[] getterNames = new String[propertySpan];
		String[] setterNames = new String[propertySpan];
		Class[] propTypes = new Class[propertySpan];
		for ( int i = 0; i < propertySpan; i++ ) {
			getterNames[i] = getters[ i ].getMethodName();
			setterNames[i] = setters[ i ].getMethodName();
			propTypes[i] = getters[ i ].getReturnType();
		}

		if ( hasCustomAccessors || ! Environment.useReflectionOptimizer() ) {
			optimizer = null;
		}
		else {
			// todo : YUCK!!!
			optimizer = Environment.getBytecodeProvider().getReflectionOptimizer(
					mappedClass, getterNames, setterNames, propTypes
			);
//			optimizer = getFactory().getSettings().getBytecodeProvider().getReflectionOptimizer(
//					mappedClass, getterNames, setterNames, propTypes
//			);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    protected ProxyFactory buildProxyFactory(PersistentClass persistentClass, Getter idGetter, Setter idSetter) {
		// determine the id getter and setter methods from the proxy interface (if any)
        // determine all interfaces needed by the resulting proxy
		HashSet<Class> proxyInterfaces = new HashSet<Class>();
		proxyInterfaces.add( HibernateProxy.class );

		Class mappedClass = persistentClass.getMappedClass();
		Class proxyInterface = persistentClass.getProxyInterface();

		if ( proxyInterface!=null && !mappedClass.equals( proxyInterface ) ) {
			if ( !proxyInterface.isInterface() ) {
				throw new MappingException(
						"proxy must be either an interface, or the class itself: " + getEntityName()
				);
			}
			proxyInterfaces.add( proxyInterface );
		}

		if ( mappedClass.isInterface() ) {
			proxyInterfaces.add( mappedClass );
		}

		Iterator subclasses = persistentClass.getSubclassIterator();
		while ( subclasses.hasNext() ) {
			final Subclass subclass = ( Subclass ) subclasses.next();
			final Class subclassProxy = subclass.getProxyInterface();
			final Class subclassClass = subclass.getMappedClass();
			if ( subclassProxy!=null && !subclassClass.equals( subclassProxy ) ) {
				if ( !subclassProxy.isInterface() ) {
					throw new MappingException(
							"proxy must be either an interface, or the class itself: " + subclass.getEntityName()
					);
				}
				proxyInterfaces.add( subclassProxy );
			}
		}

		Iterator properties = persistentClass.getPropertyIterator();
		Class clazz = persistentClass.getMappedClass();
		while ( properties.hasNext() ) {
			Property property = (Property) properties.next();
			Method method = property.getGetter(clazz).getMethod();
			if ( method != null && Modifier.isFinal( method.getModifiers() ) ) {
                LOG.gettersOfLazyClassesCannotBeFinal(persistentClass.getEntityName(), property.getName());
			}
			method = property.getSetter(clazz).getMethod();
            if ( method != null && Modifier.isFinal( method.getModifiers() ) ) {
                LOG.settersOfLazyClassesCannotBeFinal(persistentClass.getEntityName(), property.getName());
			}
		}

		Method idGetterMethod = idGetter==null ? null : idGetter.getMethod();
		Method idSetterMethod = idSetter==null ? null : idSetter.getMethod();

		Method proxyGetIdentifierMethod = idGetterMethod==null || proxyInterface==null ?
				null :
		        ReflectHelper.getMethod(proxyInterface, idGetterMethod);
		Method proxySetIdentifierMethod = idSetterMethod==null || proxyInterface==null  ?
				null :
		        ReflectHelper.getMethod(proxyInterface, idSetterMethod);

		ProxyFactory pf = buildProxyFactoryInternal( persistentClass, idGetter, idSetter );
		try {
			pf.postInstantiate(
					getEntityName(),
					mappedClass,
					proxyInterfaces,
					proxyGetIdentifierMethod,
					proxySetIdentifierMethod,
					persistentClass.hasEmbeddedIdentifier() ?
			                (CompositeType) persistentClass.getIdentifier().getType() :
			                null
			);
		}
		catch ( HibernateException he ) {
            LOG.unableToCreateProxyFactory(getEntityName(), he);
			pf = null;
		}
		return pf;
	}

	protected ProxyFactory buildProxyFactoryInternal(PersistentClass persistentClass, Getter idGetter, Setter idSetter) {
		// TODO : YUCK!!!  fix after HHH-1907 is complete
		return Environment.getBytecodeProvider().getProxyFactoryFactory().buildProxyFactory();
//		return getFactory().getSettings().getBytecodeProvider().getProxyFactoryFactory().buildProxyFactory();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    protected Instantiator buildInstantiator(PersistentClass persistentClass) {
		if ( optimizer == null ) {
			return new PojoInstantiator( persistentClass, null );
		}
		else {
			return new PojoInstantiator( persistentClass, optimizer.getInstantiationOptimizer() );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ProxyFactory buildProxyFactory(EntityBinding entityBinding, Getter idGetter, Setter idSetter) {
		// determine the id getter and setter methods from the proxy interface (if any)
		// determine all interfaces needed by the resulting proxy
		HashSet<Class> proxyInterfaces = new HashSet<Class>();
		proxyInterfaces.add( HibernateProxy.class );

		Class mappedClass = entityBinding.getEntity().getClassReference();
		Class proxyInterface = entityBinding.getProxyInterfaceType().getValue();

		if ( proxyInterface!=null && !mappedClass.equals( proxyInterface ) ) {
			if ( ! proxyInterface.isInterface() ) {
				throw new MappingException(
						"proxy must be either an interface, or the class itself: " + getEntityName()
				);
			}
			proxyInterfaces.add( proxyInterface );
		}

		if ( mappedClass.isInterface() ) {
			proxyInterfaces.add( mappedClass );
		}

		for ( EntityBinding subEntityBinding : entityBinding.getPostOrderSubEntityBindingClosure() ) {
			final Class subclassProxy = subEntityBinding.getProxyInterfaceType().getValue();
			final Class subclassClass = subEntityBinding.getClassReference();
			if ( subclassProxy!=null && !subclassClass.equals( subclassProxy ) ) {
				if ( ! subclassProxy.isInterface() ) {
					throw new MappingException(
							"proxy must be either an interface, or the class itself: " + subEntityBinding.getEntity().getName()
					);
				}
				proxyInterfaces.add( subclassProxy );
			}
		}

		for ( AttributeBinding property : entityBinding.attributeBindings() ) {
			Method method = getGetter( property ).getMethod();
			if ( method != null && Modifier.isFinal( method.getModifiers() ) ) {
				LOG.gettersOfLazyClassesCannotBeFinal(entityBinding.getEntity().getName(), property.getAttribute().getName());
			}
			method = getSetter( property ).getMethod();
			if ( method != null && Modifier.isFinal( method.getModifiers() ) ) {
				LOG.settersOfLazyClassesCannotBeFinal(entityBinding.getEntity().getName(), property.getAttribute().getName());
			}
		}

		Method idGetterMethod = idGetter==null ? null : idGetter.getMethod();
		Method idSetterMethod = idSetter==null ? null : idSetter.getMethod();

		Method proxyGetIdentifierMethod = idGetterMethod==null || proxyInterface==null ?
				null :
		        ReflectHelper.getMethod(proxyInterface, idGetterMethod);
		Method proxySetIdentifierMethod = idSetterMethod==null || proxyInterface==null  ?
				null :
		        ReflectHelper.getMethod(proxyInterface, idSetterMethod);

		ProxyFactory pf = buildProxyFactoryInternal( entityBinding, idGetter, idSetter );
		try {
			pf.postInstantiate(
					getEntityName(),
					mappedClass,
					proxyInterfaces,
					proxyGetIdentifierMethod,
					proxySetIdentifierMethod,
					entityBinding.getHierarchyDetails().getEntityIdentifier().isEmbedded()
							? ( CompositeType ) entityBinding
									.getHierarchyDetails()
									.getEntityIdentifier()
									.getValueBinding()
									.getHibernateTypeDescriptor()
									.getResolvedTypeMapping()
							: null
			);
		}
		catch ( HibernateException he ) {
			LOG.unableToCreateProxyFactory(getEntityName(), he);
			pf = null;
		}
		return pf;
	}

	protected ProxyFactory buildProxyFactoryInternal(EntityBinding entityBinding, Getter idGetter, Setter idSetter) {
		// TODO : YUCK!!!  fix after HHH-1907 is complete
		return Environment.getBytecodeProvider().getProxyFactoryFactory().buildProxyFactory();
//		return getFactory().getSettings().getBytecodeProvider().getProxyFactoryFactory().buildProxyFactory();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Instantiator buildInstantiator(EntityBinding entityBinding) {
		if ( optimizer == null ) {
			return new PojoInstantiator( entityBinding, null );
		}
		else {
			return new PojoInstantiator( entityBinding, optimizer.getInstantiationOptimizer() );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public void setPropertyValues(Object entity, Object[] values) throws HibernateException {
		if ( !getEntityMetamodel().hasLazyProperties() && optimizer != null && optimizer.getAccessOptimizer() != null ) {
			setPropertyValuesWithOptimizer( entity, values );
		}
		else {
			super.setPropertyValues( entity, values );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public Object[] getPropertyValues(Object entity) throws HibernateException {
		if ( shouldGetAllProperties( entity ) && optimizer != null && optimizer.getAccessOptimizer() != null ) {
			return getPropertyValuesWithOptimizer( entity );
		}
		else {
			return super.getPropertyValues( entity );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public Object[] getPropertyValuesToInsert(Object entity, Map mergeMap, SessionImplementor session) throws HibernateException {
		if ( shouldGetAllProperties( entity ) && optimizer != null && optimizer.getAccessOptimizer() != null ) {
			return getPropertyValuesWithOptimizer( entity );
		}
		else {
			return super.getPropertyValuesToInsert( entity, mergeMap, session );
		}
	}

	protected void setPropertyValuesWithOptimizer(Object object, Object[] values) {
		optimizer.getAccessOptimizer().setPropertyValues( object, values );
	}

	protected Object[] getPropertyValuesWithOptimizer(Object object) {
		return optimizer.getAccessOptimizer().getPropertyValues( object );
	}

	/**
	 * {@inheritDoc}
	 */
	public EntityMode getEntityMode() {
		return EntityMode.POJO;
	}

	/**
	 * {@inheritDoc}
	 */
	public Class getMappedClass() {
		return mappedClass;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public boolean isLifecycleImplementor() {
		return lifecycleImplementor;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    protected Getter buildPropertyGetter(Property mappedProperty, PersistentClass mappedEntity) {
		return mappedProperty.getGetter( mappedEntity.getMappedClass() );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    protected Setter buildPropertySetter(Property mappedProperty, PersistentClass mappedEntity) {
		return mappedProperty.getSetter( mappedEntity.getMappedClass() );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Getter buildPropertyGetter(AttributeBinding mappedProperty) {
		return getGetter( mappedProperty );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Setter buildPropertySetter(AttributeBinding mappedProperty) {
		return getSetter( mappedProperty );
	}

	private Getter getGetter(AttributeBinding mappedProperty)  throws PropertyNotFoundException, MappingException {
		return getPropertyAccessor( mappedProperty ).getGetter(
				mappedProperty.getContainer().getClassReference(),
				mappedProperty.getAttribute().getName()
		);
	}

	private Setter getSetter(AttributeBinding mappedProperty) throws PropertyNotFoundException, MappingException {
		return getPropertyAccessor( mappedProperty ).getSetter(
				mappedProperty.getContainer().getClassReference(),
				mappedProperty.getAttribute().getName()
		);
	}

	private PropertyAccessor getPropertyAccessor(AttributeBinding mappedProperty) throws MappingException {
		// TODO: Fix this then backrefs are working in new metamodel
		return PropertyAccessorFactory.getPropertyAccessor(
				mappedProperty.getContainer().getClassReference(),
				mappedProperty.getPropertyAccessorName()
		);
	}

	/**
	 * {@inheritDoc}
	 */
	public Class getConcreteProxyClass() {
		return proxyInterface;
	}

    //TODO: need to make the majority of this functionality into a top-level support class for custom impl support

	/**
	 * {@inheritDoc}
	 */
	@Override
    public void afterInitialize(Object entity, boolean lazyPropertiesAreUnfetched, SessionImplementor session) {
		if ( isInstrumented() ) {
			Set lazyProps = lazyPropertiesAreUnfetched && getEntityMetamodel().hasLazyProperties() ?
					lazyPropertyNames : null;
			//TODO: if we support multiple fetch groups, we would need
			//      to clone the set of lazy properties!
			FieldInterceptionHelper.injectFieldInterceptor( entity, getEntityName(), lazyProps, session );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public boolean hasUninitializedLazyProperties(Object entity) {
		if ( getEntityMetamodel().hasLazyProperties() ) {
			FieldInterceptor callback = FieldInterceptionHelper.extractFieldInterceptor( entity );
			return callback != null && !callback.isInitialized();
		}
		else {
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isInstrumented() {
		return isInstrumented;
	}

	/**
	 * {@inheritDoc}
	 */
	public String determineConcreteSubclassEntityName(Object entityInstance, SessionFactoryImplementor factory) {
		final Class concreteEntityClass = entityInstance.getClass();
		if ( concreteEntityClass == getMappedClass() ) {
			return getEntityName();
		}
		else {
			String entityName = getEntityMetamodel().findEntityNameByEntityClass( concreteEntityClass );
			if ( entityName == null ) {
				throw new HibernateException(
						"Unable to resolve entity name from Class [" + concreteEntityClass.getName() + "]"
								+ " expected instance/subclass of [" + getEntityName() + "]"
				);
			}
			return entityName;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public EntityNameResolver[] getEntityNameResolvers() {
		return null;
	}
}
