// $Id: PojoEntityTuplizer.java 9210 2006-02-03 22:15:19Z steveebersole $
package org.hibernate.tuple.entity;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.tuple.entity.AbstractEntityTuplizer;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.tuple.Instantiator;
import org.hibernate.tuple.PojoInstantiator;
import org.hibernate.bytecode.ReflectionOptimizer;
import org.hibernate.cfg.Environment;
import org.hibernate.classic.Lifecycle;
import org.hibernate.classic.Validatable;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.intercept.FieldInterceptor;
import org.hibernate.intercept.FieldInterceptionHelper;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Subclass;
import org.hibernate.property.Getter;
import org.hibernate.property.Setter;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.type.AbstractComponentType;
import org.hibernate.util.ReflectHelper;

/**
 * An {@link EntityTuplizer} specific to the pojo entity mode.
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class PojoEntityTuplizer extends AbstractEntityTuplizer {

	static final Log log = LogFactory.getLog( PojoEntityTuplizer.class );

	private final Class mappedClass;
	private final Class proxyInterface;
	private final boolean lifecycleImplementor;
	private final boolean validatableImplementor;
	private final Set lazyPropertyNames = new HashSet();
	private ReflectionOptimizer optimizer;

	public PojoEntityTuplizer(EntityMetamodel entityMetamodel, PersistentClass mappedEntity) {
		super( entityMetamodel, mappedEntity );
		this.mappedClass = mappedEntity.getMappedClass();
		this.proxyInterface = mappedEntity.getProxyInterface();
		this.lifecycleImplementor = Lifecycle.class.isAssignableFrom( mappedClass );
		this.validatableImplementor = Validatable.class.isAssignableFrom( mappedClass );

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

	protected ProxyFactory buildProxyFactory(PersistentClass persistentClass, Getter idGetter, Setter idSetter) {
		// determine the id getter and setter methods from the proxy interface (if any)
        // determine all interfaces needed by the resulting proxy
		HashSet proxyInterfaces = new HashSet();
		proxyInterfaces.add( HibernateProxy.class );
		
		Class mappedClass = persistentClass.getMappedClass();
		Class proxyInterface = persistentClass.getProxyInterface();

		if ( proxyInterface!=null && !mappedClass.equals( proxyInterface ) ) {
			if ( !proxyInterface.isInterface() ) {
				throw new MappingException(
				        "proxy must be either an interface, or the class itself: " + 
				        getEntityName()
					);
			}
			proxyInterfaces.add( proxyInterface );
		}

		if ( mappedClass.isInterface() ) {
			proxyInterfaces.add( mappedClass );
		}

		Iterator iter = persistentClass.getSubclassIterator();
		while ( iter.hasNext() ) {
			Subclass subclass = ( Subclass ) iter.next();
			Class subclassProxy = subclass.getProxyInterface();
			Class subclassClass = subclass.getMappedClass();
			if ( subclassProxy!=null && !subclassClass.equals( subclassProxy ) ) {
				if ( !proxyInterface.isInterface() ) {
					throw new MappingException(
					        "proxy must be either an interface, or the class itself: " + 
					        subclass.getEntityName()
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
				log.error(
						"Getters of lazy classes cannot be final: " + persistentClass.getEntityName() + 
						"." + property.getName() 
					);
			}
			method = property.getSetter(clazz).getMethod();
            if ( method != null && Modifier.isFinal( method.getModifiers() ) ) {
				log.error(
						"Setters of lazy classes cannot be final: " + persistentClass.getEntityName() + 
						"." + property.getName() 
					);
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
			                (AbstractComponentType) persistentClass.getIdentifier().getType() :
			                null
			);
		}
		catch ( HibernateException he ) {
			log.warn( "could not create proxy factory for:" + getEntityName(), he );
			pf = null;
		}
		return pf;
	}

	protected ProxyFactory buildProxyFactoryInternal(PersistentClass persistentClass, Getter idGetter, Setter idSetter) {
		// TODO : YUCK!!!  finx after HHH-1907 is complete
		return Environment.getBytecodeProvider().getProxyFactoryFactory().buildProxyFactory();
//		return getFactory().getSettings().getBytecodeProvider().getProxyFactoryFactory().buildProxyFactory();
	}

	protected Instantiator buildInstantiator(PersistentClass persistentClass) {
		if ( optimizer == null ) {
			return new PojoInstantiator( persistentClass, null );
		}
		else {
			return new PojoInstantiator( persistentClass, optimizer.getInstantiationOptimizer() );
		}
	}

	public void setPropertyValues(Object entity, Object[] values) throws HibernateException {
		if ( !getEntityMetamodel().hasLazyProperties() && optimizer != null && optimizer.getAccessOptimizer() != null ) {
			setPropertyValuesWithOptimizer( entity, values );
		}
		else {
			super.setPropertyValues( entity, values );
		}
	}

	public Object[] getPropertyValues(Object entity) throws HibernateException {
		if ( shouldGetAllProperties( entity ) && optimizer != null && optimizer.getAccessOptimizer() != null ) {
			return getPropertyValuesWithOptimizer( entity );
		}
		else {
			return super.getPropertyValues( entity );
		}
	}

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

	public EntityMode getEntityMode() {
		return EntityMode.POJO;
	}

	public Class getMappedClass() {
		return mappedClass;
	}

	public boolean isLifecycleImplementor() {
		return lifecycleImplementor;
	}

	public boolean isValidatableImplementor() {
		return validatableImplementor;
	}

	protected Getter buildPropertyGetter(Property mappedProperty, PersistentClass mappedEntity) {
		return mappedProperty.getGetter( mappedEntity.getMappedClass() );
	}

	protected Setter buildPropertySetter(Property mappedProperty, PersistentClass mappedEntity) {
		return mappedProperty.getSetter( mappedEntity.getMappedClass() );
	}

	public Class getConcreteProxyClass() {
		return proxyInterface;
	}

    //TODO: need to make the majority of this functionality into a top-level support class for custom impl support

	public void afterInitialize(Object entity, boolean lazyPropertiesAreUnfetched, SessionImplementor session) {
		if ( isInstrumented() ) {
			Set lazyProps = lazyPropertiesAreUnfetched && getEntityMetamodel().hasLazyProperties() ?
					lazyPropertyNames : null;
			//TODO: if we support multiple fetch groups, we would need
			//      to clone the set of lazy properties!
			FieldInterceptionHelper.injectFieldInterceptor( entity, getEntityName(), lazyProps, session );
		}
	}

	public boolean hasUninitializedLazyProperties(Object entity) {
		if ( getEntityMetamodel().hasLazyProperties() ) {
			FieldInterceptor callback = FieldInterceptionHelper.extractFieldInterceptor( entity );
			return callback != null && !callback.isInitialized();
		}
		else {
			return false;
		}
	}

	public boolean isInstrumented() {
		return FieldInterceptionHelper.isInstrumented( getMappedClass() );
	}

}
