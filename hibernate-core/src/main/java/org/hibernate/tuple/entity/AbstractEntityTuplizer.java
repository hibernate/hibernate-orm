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

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.bytecode.instrumentation.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PersistEvent;
import org.hibernate.event.spi.PersistEventListener;
import org.hibernate.id.Assigned;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.binding.AttributeBinding;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.property.Getter;
import org.hibernate.property.Setter;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.tuple.Instantiator;
import org.hibernate.tuple.StandardProperty;
import org.hibernate.tuple.VersionProperty;
import org.hibernate.type.ComponentType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;


/**
 * Support for tuplizers relating to entities.
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public abstract class AbstractEntityTuplizer implements EntityTuplizer {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			AbstractEntityTuplizer.class.getName()
	);

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
	private final CompositeType identifierMapperType;

	public Type getIdentifierMapperType() {
		return identifierMapperType;
	}

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
	 * Build an appropriate Getter for the given property.
	 *
	 *
	 * @param mappedProperty The property to be accessed via the built Getter.
	 * @return An appropriate Getter instance.
	 */
	protected abstract Getter buildPropertyGetter(AttributeBinding mappedProperty);

	/**
	 * Build an appropriate Setter for the given property.
	 *
	 *
	 * @param mappedProperty The property to be accessed via the built Setter.
	 * @return An appropriate Setter instance.
	 */
	protected abstract Setter buildPropertySetter(AttributeBinding mappedProperty);

	/**
	 * Build an appropriate Instantiator for the given mapped entity.
	 *
	 * @param mappingInfo The mapping information regarding the mapped entity.
	 * @return An appropriate Instantiator instance.
	 */
	protected abstract Instantiator buildInstantiator(EntityBinding mappingInfo);

	/**
	 * Build an appropriate ProxyFactory for the given mapped entity.
	 *
	 * @param mappingInfo The mapping information regarding the mapped entity.
	 * @param idGetter The constructed Getter relating to the entity's id property.
	 * @param idSetter The constructed Setter relating to the entity's id property.
	 * @return An appropriate ProxyFactory instance.
	 */
	protected abstract ProxyFactory buildProxyFactory(EntityBinding mappingInfo, Getter idGetter, Setter idSetter);

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

		Iterator itr = mappingInfo.getPropertyClosureIterator();
		boolean foundCustomAccessor=false;
		int i=0;
		while ( itr.hasNext() ) {
			//TODO: redesign how PropertyAccessors are acquired...
			Property property = (Property) itr.next();
			getters[i] = buildPropertyGetter(property, mappingInfo);
			setters[i] = buildPropertySetter(property, mappingInfo);
			if ( !property.isBasicPropertyAccessor() ) {
				foundCustomAccessor = true;
			}
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
		if ( mapper == null ) {
			identifierMapperType = null;
			mappedIdentifierValueMarshaller = null;
		}
		else {
			identifierMapperType = (CompositeType) mapper.getType();
			mappedIdentifierValueMarshaller = buildMappedIdentifierValueMarshaller(
					(ComponentType) entityMetamodel.getIdentifierProperty().getType(),
					(ComponentType) identifierMapperType
			);
		}
	}

	/**
	 * Constructs a new AbstractEntityTuplizer instance.
	 *
	 * @param entityMetamodel The "interpreted" information relating to the mapped entity.
	 * @param mappingInfo The parsed "raw" mapping data relating to the given entity.
	 */
	public AbstractEntityTuplizer(EntityMetamodel entityMetamodel, EntityBinding mappingInfo) {
		this.entityMetamodel = entityMetamodel;

		if ( !entityMetamodel.getIdentifierProperty().isVirtual() ) {
			idGetter = buildPropertyGetter( mappingInfo.getHierarchyDetails().getEntityIdentifier().getValueBinding() );
			idSetter = buildPropertySetter( mappingInfo.getHierarchyDetails().getEntityIdentifier().getValueBinding() );
		}
		else {
			idGetter = null;
			idSetter = null;
		}

		propertySpan = entityMetamodel.getPropertySpan();

		getters = new Getter[ propertySpan ];
		setters = new Setter[ propertySpan ];

		boolean foundCustomAccessor = false;
		int i = 0;
		for ( AttributeBinding property : mappingInfo.getAttributeBindingClosure() ) {
			if ( property == mappingInfo.getHierarchyDetails().getEntityIdentifier().getValueBinding() ) {
				continue; // ID binding processed above
			}

			//TODO: redesign how PropertyAccessors are acquired...
			getters[ i ] = buildPropertyGetter( property );
			setters[ i ] = buildPropertySetter( property );
			if ( ! property.isBasicPropertyAccessor() ) {
				foundCustomAccessor = true;
			}
			i++;
		}
		hasCustomAccessors = foundCustomAccessor;

		instantiator = buildInstantiator( mappingInfo );

		if ( entityMetamodel.isLazy() ) {
			proxyFactory = buildProxyFactory( mappingInfo, idGetter, idSetter );
			if ( proxyFactory == null ) {
				entityMetamodel.setLazy( false );
			}
		}
		else {
			proxyFactory = null;
		}


		// TODO: Fix this when components are working (HHH-6173)
		//Component mapper = mappingInfo.getEntityIdentifier().getIdentifierMapper();
		Component mapper = null;
		if ( mapper == null ) {
			identifierMapperType = null;
			mappedIdentifierValueMarshaller = null;
		}
		else {
			identifierMapperType = ( CompositeType ) mapper.getType();
			mappedIdentifierValueMarshaller = buildMappedIdentifierValueMarshaller(
					( ComponentType ) entityMetamodel.getIdentifierProperty().getType(),
					( ComponentType ) identifierMapperType
			);
		}
	}

	/** Retreives the defined entity-name for the tuplized entity.
	 *
	 * @return The entity-name.
	 */
	protected String getEntityName() {
		return entityMetamodel.getName();
	}

	/**
	 * Retrieves the defined entity-names for any subclasses defined for this
	 * entity.
	 *
	 * @return Any subclass entity-names.
	 */
	protected Set getSubclassEntityNames() {
		return entityMetamodel.getSubclassEntityNames();
	}

	public Serializable getIdentifier(Object entity) throws HibernateException {
		return getIdentifier( entity, null );
	}

	public Serializable getIdentifier(Object entity, SessionImplementor session) {
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
					id = mappedIdentifierValueMarshaller.getIdentifier( entity, getEntityMode(), session );
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
			StringBuilder msg = new StringBuilder( "Identifier classes must be serializable. " );
			if ( id != null ) {
				msg.append( id.getClass().getName() ).append( " is not serializable. " );
			}
			if ( cce.getMessage() != null ) {
				msg.append( cce.getMessage() );
			}
			throw new ClassCastException( msg.toString() );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setIdentifier(Object entity, Serializable id) throws HibernateException {
		// 99% of the time the session is not needed.  Its only needed for certain brain-dead
		// interpretations of JPA 2 "derived identity" support
		setIdentifier( entity, id, null );
	}


	/**
	 * {@inheritDoc}
	 */
	public void setIdentifier(Object entity, Serializable id, SessionImplementor session) {
		if ( entityMetamodel.getIdentifierProperty().isEmbedded() ) {
			if ( entity != id ) {
				CompositeType copier = (CompositeType) entityMetamodel.getIdentifierProperty().getType();
				copier.setPropertyValues( entity, copier.getPropertyValues( id, getEntityMode() ), getEntityMode() );
			}
		}
		else if ( idSetter != null ) {
			idSetter.set( entity, id, getFactory() );
		}
		else if ( identifierMapperType != null ) {
			mappedIdentifierValueMarshaller.setIdentifier( entity, id, getEntityMode(), session );
		}
	}

	private static interface MappedIdentifierValueMarshaller {
		public Object getIdentifier(Object entity, EntityMode entityMode, SessionImplementor session);
		public void setIdentifier(Object entity, Serializable id, EntityMode entityMode, SessionImplementor session);
	}

	private final MappedIdentifierValueMarshaller mappedIdentifierValueMarshaller;

	private static MappedIdentifierValueMarshaller buildMappedIdentifierValueMarshaller(
			ComponentType mappedIdClassComponentType,
			ComponentType virtualIdComponent) {
		// so basically at this point we know we have a "mapped" composite identifier
		// which is an awful way to say that the identifier is represented differently
		// in the entity and in the identifier value.  The incoming value should
		// be an instance of the mapped identifier class (@IdClass) while the incoming entity
		// should be an instance of the entity class as defined by metamodel.
		//
		// However, even within that we have 2 potential scenarios:
		//		1) @IdClass types and entity @Id property types match
		//			- return a NormalMappedIdentifierValueMarshaller
		//		2) They do not match
		//			- return a IncrediblySillyJpaMapsIdMappedIdentifierValueMarshaller
		boolean wereAllEquivalent = true;
		// the sizes being off is a much bigger problem that should have been caught already...
		for ( int i = 0; i < virtualIdComponent.getSubtypes().length; i++ ) {
			if ( virtualIdComponent.getSubtypes()[i].isEntityType()
					&& ! mappedIdClassComponentType.getSubtypes()[i].isEntityType() ) {
				wereAllEquivalent = false;
				break;
			}
		}

		return wereAllEquivalent
				? (MappedIdentifierValueMarshaller) new NormalMappedIdentifierValueMarshaller( virtualIdComponent, mappedIdClassComponentType )
				: (MappedIdentifierValueMarshaller) new IncrediblySillyJpaMapsIdMappedIdentifierValueMarshaller( virtualIdComponent, mappedIdClassComponentType );
	}

	private static class NormalMappedIdentifierValueMarshaller implements MappedIdentifierValueMarshaller {
		private final ComponentType virtualIdComponent;
		private final ComponentType mappedIdentifierType;

		private NormalMappedIdentifierValueMarshaller(ComponentType virtualIdComponent, ComponentType mappedIdentifierType) {
			this.virtualIdComponent = virtualIdComponent;
			this.mappedIdentifierType = mappedIdentifierType;
		}

		public Object getIdentifier(Object entity, EntityMode entityMode, SessionImplementor session) {
			Object id = mappedIdentifierType.instantiate( entityMode );
			final Object[] propertyValues = virtualIdComponent.getPropertyValues( entity, entityMode );
			mappedIdentifierType.setPropertyValues( id, propertyValues, entityMode );
			return id;
		}

		public void setIdentifier(Object entity, Serializable id, EntityMode entityMode, SessionImplementor session) {
			virtualIdComponent.setPropertyValues(
					entity,
					mappedIdentifierType.getPropertyValues( id, session ),
					entityMode
			);
		}
	}

	private static class IncrediblySillyJpaMapsIdMappedIdentifierValueMarshaller implements MappedIdentifierValueMarshaller {
		private final ComponentType virtualIdComponent;
		private final ComponentType mappedIdentifierType;

		private IncrediblySillyJpaMapsIdMappedIdentifierValueMarshaller(ComponentType virtualIdComponent, ComponentType mappedIdentifierType) {
			this.virtualIdComponent = virtualIdComponent;
			this.mappedIdentifierType = mappedIdentifierType;
		}

		public Object getIdentifier(Object entity, EntityMode entityMode, SessionImplementor session) {
			final Object id = mappedIdentifierType.instantiate( entityMode );
			final Object[] propertyValues = virtualIdComponent.getPropertyValues( entity, entityMode );
			final Type[] subTypes = virtualIdComponent.getSubtypes();
			final Type[] copierSubTypes = mappedIdentifierType.getSubtypes();
			final Iterable<PersistEventListener> persistEventListeners = persistEventListeners( session );
			final PersistenceContext persistenceContext = session.getPersistenceContext();
			final int length = subTypes.length;
			for ( int i = 0 ; i < length; i++ ) {
				if ( propertyValues[i] == null ) {
					throw new HibernateException( "No part of a composite identifier may be null" );
				}
				//JPA 2 @MapsId + @IdClass points to the pk of the entity
				if ( subTypes[i].isAssociationType() && ! copierSubTypes[i].isAssociationType() ) {
					// we need a session to handle this use case
					if ( session == null ) {
						throw new AssertionError(
								"Deprecated version of getIdentifier (no session) was used but session was required"
						);
					}
					final Object subId;
					if ( HibernateProxy.class.isInstance( propertyValues[i] ) ) {
						subId = ( (HibernateProxy) propertyValues[i] ).getHibernateLazyInitializer().getIdentifier();
					}
					else {
						EntityEntry pcEntry = session.getPersistenceContext().getEntry( propertyValues[i] );
						if ( pcEntry != null ) {
							subId = pcEntry.getId();
						}
						else {
							LOG.debug( "Performing implicit derived identity cascade" );
							final PersistEvent event = new PersistEvent( null, propertyValues[i], (EventSource) session );
							for ( PersistEventListener listener : persistEventListeners ) {
								listener.onPersist( event );
							}
							pcEntry = persistenceContext.getEntry( propertyValues[i] );
							if ( pcEntry == null || pcEntry.getId() == null ) {
								throw new HibernateException( "Unable to process implicit derived identity cascade" );
							}
							else {
								subId = pcEntry.getId();
							}
						}
					}
					propertyValues[i] = subId;
				}
			}
			mappedIdentifierType.setPropertyValues( id, propertyValues, entityMode );
			return id;
		}

		public void setIdentifier(Object entity, Serializable id, EntityMode entityMode, SessionImplementor session) {
			final Object[] extractedValues = mappedIdentifierType.getPropertyValues( id, entityMode );
			final Object[] injectionValues = new Object[ extractedValues.length ];
			final PersistenceContext persistenceContext = session.getPersistenceContext();
			for ( int i = 0; i < virtualIdComponent.getSubtypes().length; i++ ) {
				final Type virtualPropertyType = virtualIdComponent.getSubtypes()[i];
				final Type idClassPropertyType = mappedIdentifierType.getSubtypes()[i];
				if ( virtualPropertyType.isEntityType() && ! idClassPropertyType.isEntityType() ) {
					if ( session == null ) {
						throw new AssertionError(
								"Deprecated version of getIdentifier (no session) was used but session was required"
						);
					}
					final String associatedEntityName = ( (EntityType) virtualPropertyType ).getAssociatedEntityName();
					final EntityKey entityKey = session.generateEntityKey(
							(Serializable) extractedValues[i],
							session.getFactory().getEntityPersister( associatedEntityName )
					);
					// it is conceivable there is a proxy, so check that first
					Object association = persistenceContext.getProxy( entityKey );
					if ( association == null ) {
						// otherwise look for an initialized version
						association = persistenceContext.getEntity( entityKey );
					}
					injectionValues[i] = association;
				}
				else {
					injectionValues[i] = extractedValues[i];
				}
			}
			virtualIdComponent.setPropertyValues( entity, injectionValues, entityMode );
		}
	}

	private static Iterable<PersistEventListener> persistEventListeners(SessionImplementor session) {
		return session
				.getFactory()
				.getServiceRegistry()
				.getService( EventListenerRegistry.class )
				.getEventListenerGroup( EventType.PERSIST )
				.listeners();
	}

	/**
	 * {@inheritDoc}
	 */
	public void resetIdentifier(Object entity, Serializable currentId, Object currentVersion) {
		// 99% of the time the session is not needed.  Its only needed for certain brain-dead
		// interpretations of JPA 2 "derived identity" support
		resetIdentifier( entity, currentId, currentVersion, null );
	}

	/**
	 * {@inheritDoc}
	 */
	public void resetIdentifier(
			Object entity,
			Serializable currentId,
			Object currentVersion,
			SessionImplementor session) {
		if ( entityMetamodel.getIdentifierProperty().getIdentifierGenerator() instanceof Assigned ) {
		}
		else {
			//reset the id
			Serializable result = entityMetamodel.getIdentifierProperty()
					.getUnsavedValue()
					.getDefaultValue( currentId );
			setIdentifier( entity, result, session );
			//reset the version
			VersionProperty versionProperty = entityMetamodel.getVersionProperty();
			if ( entityMetamodel.isVersioned() ) {
				setPropertyValue(
				        entity,
				        entityMetamodel.getVersionPropertyIndex(),
						versionProperty.getUnsavedValue().getDefaultValue( currentVersion )
				);
			}
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
		String basePropertyName = loc > 0
				? propertyPath.substring( 0, loc )
				: propertyPath;
		//final int index = entityMetamodel.getPropertyIndexOrNull( basePropertyName );
		Integer index = entityMetamodel.getPropertyIndexOrNull( basePropertyName );
		if (index == null) {
			propertyPath = "_identifierMapper." + propertyPath;
			loc = propertyPath.indexOf('.');
			basePropertyName = loc > 0
				? propertyPath.substring( 0, loc )
				: propertyPath;
		}
		index = entityMetamodel.getPropertyIndexOrNull( basePropertyName );
		final Object baseValue = getPropertyValue( entity, index.intValue() );
		if ( loc > 0 ) {
			if ( baseValue == null ) {
				return null;
			}
			return getComponentValue(
					(ComponentType) entityMetamodel.getPropertyTypes()[index.intValue()],
					baseValue,
					propertyPath.substring(loc+1)
			);
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
		final int loc = propertyPath.indexOf( '.' );
		final String basePropertyName = loc > 0
				? propertyPath.substring( 0, loc )
				: propertyPath;
		final int index = findSubPropertyIndex( type, basePropertyName );
		final Object baseValue = type.getPropertyValue( component, index, getEntityMode() );
		if ( loc > 0 ) {
			if ( baseValue == null ) {
				return null;
			}
			return getComponentValue(
					(ComponentType) type.getSubtypes()[index],
					baseValue,
					propertyPath.substring(loc+1)
			);
		}
		else {
			return baseValue;
		}

	}

	private int findSubPropertyIndex(ComponentType type, String subPropertyName) {
		final String[] propertyNames = type.getPropertyNames();
		for ( int index = 0; index<propertyNames.length; index++ ) {
			if ( subPropertyName.equals( propertyNames[index] ) ) {
				return index;
			}
		}
		throw new MappingException( "component property not found: " + subPropertyName );
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
		// 99% of the time the session is not needed.  Its only needed for certain brain-dead
		// interpretations of JPA 2 "derived identity" support
		return instantiate( id, null );
	}

	public final Object instantiate(Serializable id, SessionImplementor session) {
		Object result = getInstantiator().instantiate( id );
		if ( id != null ) {
			setIdentifier( result, id, session );
		}
		return result;
	}

	public final Object instantiate() throws HibernateException {
		return instantiate( null, null );
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

	@Override
    public String toString() {
		return getClass().getName() + '(' + getEntityMetamodel().getName() + ')';
	}

	public Getter getIdentifierGetter() {
		return idGetter;
	}

	public Getter getVersionGetter() {
		if ( getEntityMetamodel().isVersioned() ) {
			return getGetter( getEntityMetamodel().getVersionPropertyIndex() );
		}
		return null;
	}

	public Getter getGetter(int i) {
		return getters[i];
	}
}
