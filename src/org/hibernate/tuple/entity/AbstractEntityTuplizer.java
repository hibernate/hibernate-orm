// $Id: AbstractEntityTuplizer.java 7516 2005-07-16 22:20:48Z oneovthafew $
package org.hibernate.tuple.entity;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.tuple.Instantiator;
import org.hibernate.tuple.VersionProperty;
import org.hibernate.tuple.StandardProperty;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.id.Assigned;
import org.hibernate.intercept.LazyPropertyInitializer;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.property.Getter;
import org.hibernate.property.Setter;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.type.AbstractComponentType;
import org.hibernate.type.ComponentType;


/**
 * Support for tuplizers relating to entities.
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public abstract class AbstractEntityTuplizer implements EntityTuplizer {

	//TODO: currently keeps Getters and Setters (instead of PropertyAccessors) because of the way getGetter() and getSetter() are implemented currently; yuck!

	private final EntityMetamodel entityMetamodel;

	private final Getter idGetter;
	private final Setter idSetter;

	protected final Getter[] getters;
	protected final Setter[] setters;
	protected final int propertySpan;
	protected final boolean hasCustomAccessors;
	private final Instantiator instantiator;
	private final ProxyFactory proxyFactory;
	private final AbstractComponentType identifierMapperType;


	/**
	 * Return the entity-mode handled by this tuplizer instance.
	 *
	 * @return The entity-mode
	 */
	protected abstract EntityMode getEntityMode();

	/**
	 * Build an appropriate Getter for the given property.
	 *
	 * @param mappedProperty The property to be accessed via the built Getter.
	 * @param mappedEntity The entity information regarding the mapped entity owning this property.
	 * @return An appropriate Getter instance.
	 */
	protected abstract Getter buildPropertyGetter(Property mappedProperty, PersistentClass mappedEntity);

	/**
	 * Build an appropriate Setter for the given property.
	 *
	 * @param mappedProperty The property to be accessed via the built Setter.
	 * @param mappedEntity The entity information regarding the mapped entity owning this property.
	 * @return An appropriate Setter instance.
	 */
	protected abstract Setter buildPropertySetter(Property mappedProperty, PersistentClass mappedEntity);

	/**
	 * Build an appropriate Instantiator for the given mapped entity.
	 *
	 * @param mappingInfo The mapping information regarding the mapped entity.
	 * @return An appropriate Instantiator instance.
	 */
	protected abstract Instantiator buildInstantiator(PersistentClass mappingInfo);

	/**
	 * Build an appropriate ProxyFactory for the given mapped entity.
	 *
	 * @param mappingInfo The mapping information regarding the mapped entity.
	 * @param idGetter The constructed Getter relating to the entity's id property.
	 * @param idSetter The constructed Setter relating to the entity's id property.
	 * @return An appropriate ProxyFactory instance.
	 */
	protected abstract ProxyFactory buildProxyFactory(PersistentClass mappingInfo, Getter idGetter, Setter idSetter);

	/**
	 * Constructs a new AbstractEntityTuplizer instance.
	 *
	 * @param entityMetamodel The "interpreted" information relating to the mapped entity.
	 * @param mappingInfo The parsed "raw" mapping data relating to the given entity.
	 */
	public AbstractEntityTuplizer(EntityMetamodel entityMetamodel, PersistentClass mappingInfo) {
		this.entityMetamodel = entityMetamodel;

		if ( !entityMetamodel.getIdentifierProperty().isVirtual() ) {
			idGetter = buildPropertyGetter( mappingInfo.getIdentifierProperty(), mappingInfo );
			idSetter = buildPropertySetter( mappingInfo.getIdentifierProperty(), mappingInfo );
		}
		else {
			idGetter = null;
			idSetter = null;
		}

		propertySpan = entityMetamodel.getPropertySpan();

        getters = new Getter[propertySpan];
		setters = new Setter[propertySpan];

		Iterator iter = mappingInfo.getPropertyClosureIterator();
		boolean foundCustomAccessor=false;
		int i=0;
		while ( iter.hasNext() ) {
			//TODO: redesign how PropertyAccessors are acquired...
			Property property = (Property) iter.next();
			getters[i] = buildPropertyGetter(property, mappingInfo);
			setters[i] = buildPropertySetter(property, mappingInfo);
			if ( !property.isBasicPropertyAccessor() ) foundCustomAccessor = true;
			i++;
		}
		hasCustomAccessors = foundCustomAccessor;

        instantiator = buildInstantiator( mappingInfo );

		if ( entityMetamodel.isLazy() ) {
			proxyFactory = buildProxyFactory( mappingInfo, idGetter, idSetter );
			if (proxyFactory == null) {
				entityMetamodel.setLazy( false );
			}
		}
		else {
			proxyFactory = null;
		}
		
		Component mapper = mappingInfo.getIdentifierMapper();
		identifierMapperType = mapper==null ? null : (AbstractComponentType) mapper.getType();
	}

	/** Retreives the defined entity-name for the tuplized entity.
	 *
	 * @return The entity-name.
	 */
	protected String getEntityName() {
		return entityMetamodel.getName();
	}

	/**
	 * Retreives the defined entity-names for any subclasses defined for this
	 * entity.
	 *
	 * @return Any subclass entity-names.
	 */
	protected Set getSubclassEntityNames() {
		return entityMetamodel.getSubclassEntityNames();
	}

	public Serializable getIdentifier(Object entity) throws HibernateException {
		final Object id;
		if ( entityMetamodel.getIdentifierProperty().isEmbedded() ) {
			id = entity;
		}
		else {
			if ( idGetter == null ) {
				if (identifierMapperType==null) {
					throw new HibernateException( "The class has no identifier property: " + getEntityName() );
				}
				else {
					ComponentType copier = (ComponentType) entityMetamodel.getIdentifierProperty().getType();
					id = copier.instantiate( getEntityMode() );
					copier.setPropertyValues( id, identifierMapperType.getPropertyValues( entity, getEntityMode() ), getEntityMode() );
				}
			}
			else {
				id = idGetter.get( entity );
			}
		}

		try {
			return (Serializable) id;
		}
		catch ( ClassCastException cce ) {
			StringBuffer msg = new StringBuffer( "Identifier classes must be serializable. " );
			if ( id != null ) {
				msg.append( id.getClass().getName() + " is not serializable. " );
			}
			if ( cce.getMessage() != null ) {
				msg.append( cce.getMessage() );
			}
			throw new ClassCastException( msg.toString() );
		}
	}


	public void setIdentifier(Object entity, Serializable id) throws HibernateException {
		if ( entityMetamodel.getIdentifierProperty().isEmbedded() ) {
			if ( entity != id ) {
				AbstractComponentType copier = (AbstractComponentType) entityMetamodel.getIdentifierProperty().getType();
				copier.setPropertyValues( entity, copier.getPropertyValues( id, getEntityMode() ), getEntityMode() );
			}
		}
		else if ( idSetter != null ) {
			idSetter.set( entity, id, getFactory() );
		}
	}

	public void resetIdentifier(Object entity, Serializable currentId, Object currentVersion) {
		if ( entityMetamodel.getIdentifierProperty().getIdentifierGenerator() instanceof Assigned ) {
			//return currentId;
		}
		else {
			//reset the id
			Serializable result = entityMetamodel.getIdentifierProperty()
					.getUnsavedValue()
					.getDefaultValue( currentId );
			setIdentifier( entity, result );
			//reset the version
			VersionProperty versionProperty = entityMetamodel.getVersionProperty();
			if ( entityMetamodel.isVersioned() ) {
				setPropertyValue(
				        entity,
				        entityMetamodel.getVersionPropertyIndex(),
						versionProperty.getUnsavedValue().getDefaultValue( currentVersion )
					);
			}
			//return the id, so we can use it to reset the proxy id
			//return result;
		}
	}

	public Object getVersion(Object entity) throws HibernateException {
		if ( !entityMetamodel.isVersioned() ) return null;
		return getters[ entityMetamodel.getVersionPropertyIndex() ].get( entity );
	}

	protected boolean shouldGetAllProperties(Object entity) {
		return !hasUninitializedLazyProperties( entity );
	}

	public Object[] getPropertyValues(Object entity) throws HibernateException {
		boolean getAll = shouldGetAllProperties( entity );
		final int span = entityMetamodel.getPropertySpan();
		final Object[] result = new Object[span];

		for ( int j = 0; j < span; j++ ) {
			StandardProperty property = entityMetamodel.getProperties()[j];
			if ( getAll || !property.isLazy() ) {
				result[j] = getters[j].get( entity );
			}
			else {
				result[j] = LazyPropertyInitializer.UNFETCHED_PROPERTY;
			}
		}
		return result;
	}

	public Object[] getPropertyValuesToInsert(Object entity, Map mergeMap, SessionImplementor session) 
	throws HibernateException {
		final int span = entityMetamodel.getPropertySpan();
		final Object[] result = new Object[span];

		for ( int j = 0; j < span; j++ ) {
			result[j] = getters[j].getForInsert( entity, mergeMap, session );
		}
		return result;
	}

	public Object getPropertyValue(Object entity, int i) throws HibernateException {
		return getters[i].get( entity );
	}

	public Object getPropertyValue(Object entity, String propertyPath) throws HibernateException {
		
		int loc = propertyPath.indexOf('.');
		String basePropertyName = loc>0 ?
			propertyPath.substring(0, loc) : propertyPath;
			
		int index = entityMetamodel.getPropertyIndex( basePropertyName );
		Object baseValue = getPropertyValue( entity, index );
		if ( loc>0 ) {
			ComponentType type = (ComponentType) entityMetamodel.getPropertyTypes()[index];
			return getComponentValue( type, baseValue, propertyPath.substring(loc+1) );
		}
		else {
			return baseValue;
		}
	}

	/**
	 * Extract a component property value.
	 *
	 * @param type The component property types.
	 * @param component The component instance itself.
	 * @param propertyPath The property path for the property to be extracted.
	 * @return The property value extracted.
	 */
	protected Object getComponentValue(ComponentType type, Object component, String propertyPath) {
		
		int loc = propertyPath.indexOf('.');
		String basePropertyName = loc>0 ?
			propertyPath.substring(0, loc) : propertyPath;
		
		String[] propertyNames = type.getPropertyNames();
		int index=0;
		for ( ; index<propertyNames.length; index++ ) {
			if ( basePropertyName.equals( propertyNames[index] ) ) break;
		}
		if (index==propertyNames.length) {
			throw new MappingException( "component property not found: " + basePropertyName );
		}
		
		Object baseValue = type.getPropertyValue( component, index, getEntityMode() );
		
		if ( loc>0 ) {
			ComponentType subtype = (ComponentType) type.getSubtypes()[index];
			return getComponentValue( subtype, baseValue, propertyPath.substring(loc+1) );
		}
		else {
			return baseValue;
		}
		
	}

	public void setPropertyValues(Object entity, Object[] values) throws HibernateException {
		boolean setAll = !entityMetamodel.hasLazyProperties();

		for ( int j = 0; j < entityMetamodel.getPropertySpan(); j++ ) {
			if ( setAll || values[j] != LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
				setters[j].set( entity, values[j], getFactory() );
			}
		}
	}

	public void setPropertyValue(Object entity, int i, Object value) throws HibernateException {
		setters[i].set( entity, value, getFactory() );
	}

	public void setPropertyValue(Object entity, String propertyName, Object value) throws HibernateException {
		setters[ entityMetamodel.getPropertyIndex( propertyName ) ].set( entity, value, getFactory() );
	}

	public final Object instantiate(Serializable id) throws HibernateException {
		Object result = getInstantiator().instantiate( id );
		if ( id != null ) {
			setIdentifier( result, id );
		}
		return result;
	}

	public final Object instantiate() throws HibernateException {
		return instantiate( null );
	}

	public void afterInitialize(Object entity, boolean lazyPropertiesAreUnfetched, SessionImplementor session) {}

	public boolean hasUninitializedLazyProperties(Object entity) {
		// the default is to simply not lazy fetch properties for now...
		return false;
	}

	public final boolean isInstance(Object object) {
        return getInstantiator().isInstance( object );
	}

	public boolean hasProxy() {
		return entityMetamodel.isLazy();
	}

	public final Object createProxy(Serializable id, SessionImplementor session)
	throws HibernateException {
		return getProxyFactory().getProxy( id, session );
	}

	public boolean isLifecycleImplementor() {
		return false;
	}

	public boolean isValidatableImplementor() {
		return false;
	}
	
	protected final EntityMetamodel getEntityMetamodel() {
		return entityMetamodel;
	}

	protected final SessionFactoryImplementor getFactory() {
		return entityMetamodel.getSessionFactory();
	}

	protected final Instantiator getInstantiator() {
		return instantiator;
	}

	protected final ProxyFactory getProxyFactory() {
		return proxyFactory;
	}
	
	public String toString() {
		return getClass().getName() + '(' + getEntityMetamodel().getName() + ')';
	}

}
