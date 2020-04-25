/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple.entity;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.hibernate.EntityMode;
import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributesMetadata;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.Assigned;
import org.hibernate.loader.PropertyPath;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.tuple.IdentifierProperty;
import org.hibernate.tuple.Instantiator;
import org.hibernate.type.AssociationType;
import org.hibernate.type.BasicType;
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
	 *
	 * @return An appropriate Getter instance.
	 */
	protected abstract Getter buildPropertyGetter(Property mappedProperty, PersistentClass mappedEntity);

	/**
	 * Build an appropriate Setter for the given property.
	 *
	 * @param mappedProperty The property to be accessed via the built Setter.
	 * @param mappedEntity The entity information regarding the mapped entity owning this property.
	 *
	 * @return An appropriate Setter instance.
	 */
	protected abstract Setter buildPropertySetter(Property mappedProperty, PersistentClass mappedEntity);

	/**
	 * Build an appropriate Instantiator for the given mapped entity.
	 *
	 * @param mappingInfo The mapping information regarding the mapped entity.
	 *
	 * @return An appropriate Instantiator instance.
	 */
	protected abstract Instantiator buildInstantiator(EntityMetamodel entityMetamodel, PersistentClass mappingInfo);

	/**
	 * Build an appropriate ProxyFactory for the given mapped entity.
	 *
	 * @param mappingInfo The mapping information regarding the mapped entity.
	 * @param idGetter The constructed Getter relating to the entity's id property.
	 * @param idSetter The constructed Setter relating to the entity's id property.
	 *
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

		Iterator itr = mappingInfo.getPropertyClosureIterator();
		boolean foundCustomAccessor = false;
		int i = 0;
		while ( itr.hasNext() ) {
			//TODO: redesign how PropertyAccessors are acquired...
			Property property = (Property) itr.next();
			getters[i] = buildPropertyGetter( property, mappingInfo );
			setters[i] = buildPropertySetter( property, mappingInfo );
			if ( !property.isBasicPropertyAccessor() ) {
				foundCustomAccessor = true;
			}
			i++;
		}
		hasCustomAccessors = foundCustomAccessor;

		instantiator = buildInstantiator( entityMetamodel, mappingInfo );

//		if ( entityMetamodel.isLazy() && !entityMetamodel.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading() ) {
		if ( entityMetamodel.isLazy() ) {
			proxyFactory = buildProxyFactory( mappingInfo, idGetter, idSetter );
			if ( proxyFactory == null ) {
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
			KeyValue identifier = mappingInfo.getIdentifier();
			mappedIdentifierValueMarshaller = buildMappedIdentifierValueMarshaller(
					getEntityName(),
					getFactory(),
					(ComponentType) entityMetamodel.getIdentifierProperty().getType(),
					(ComponentType) identifierMapperType,
					identifier
			);
		}
	}

	/**
	 * Retrieves the defined entity-name for the tuplized entity.
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

	@Override
	public Serializable getIdentifier(Object entity) throws HibernateException {
		return getIdentifier( entity, null );
	}

	@Override
	public Serializable getIdentifier(Object entity, SharedSessionContractImplementor session) {
		final Object id;
		if ( entityMetamodel.getIdentifierProperty().isEmbedded() ) {
			id = entity;
		}
		else if ( HibernateProxy.class.isInstance( entity ) ) {
			id = ( (HibernateProxy) entity ).getHibernateLazyInitializer().getIdentifier();
		}
		else {
			if ( idGetter == null ) {
				if ( identifierMapperType == null ) {
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
		catch (ClassCastException cce) {
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

	@Override
	public void setIdentifier(Object entity, Serializable id) throws HibernateException {
		// 99% of the time the session is not needed.  Its only needed for certain brain-dead
		// interpretations of JPA 2 "derived identity" support
		setIdentifier( entity, id, null );
	}

	@Override
	public void setIdentifier(Object entity, Serializable id, SharedSessionContractImplementor session) {
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
		public Object getIdentifier(Object entity, EntityMode entityMode, SharedSessionContractImplementor session);

		public void setIdentifier(Object entity, Serializable id, EntityMode entityMode, SharedSessionContractImplementor session);
	}

	private final MappedIdentifierValueMarshaller mappedIdentifierValueMarshaller;

	private static MappedIdentifierValueMarshaller buildMappedIdentifierValueMarshaller(
			String entityName,
			SessionFactoryImplementor sessionFactory,
			ComponentType mappedIdClassComponentType,
			ComponentType virtualIdComponent,
			KeyValue identifier) {
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
					&& !mappedIdClassComponentType.getSubtypes()[i].isEntityType() ) {
				wereAllEquivalent = false;
				break;
			}
		}

		return wereAllEquivalent ?
				new NormalMappedIdentifierValueMarshaller( virtualIdComponent, mappedIdClassComponentType ) :
				new IncrediblySillyJpaMapsIdMappedIdentifierValueMarshaller(
						entityName,
						sessionFactory,
						virtualIdComponent,
						mappedIdClassComponentType,
						identifier
		);
	}

	private static class NormalMappedIdentifierValueMarshaller implements MappedIdentifierValueMarshaller {
		private final ComponentType virtualIdComponent;
		private final ComponentType mappedIdentifierType;

		private NormalMappedIdentifierValueMarshaller(
				ComponentType virtualIdComponent,
				ComponentType mappedIdentifierType) {
			this.virtualIdComponent = virtualIdComponent;
			this.mappedIdentifierType = mappedIdentifierType;
		}

		@Override
		public Object getIdentifier(Object entity, EntityMode entityMode, SharedSessionContractImplementor session) {
			Object id = mappedIdentifierType.instantiate( entityMode );
			final Object[] propertyValues = virtualIdComponent.getPropertyValues( entity, entityMode );
			mappedIdentifierType.setPropertyValues( id, propertyValues, entityMode );
			return id;
		}

		@Override
		public void setIdentifier(Object entity, Serializable id, EntityMode entityMode, SharedSessionContractImplementor session) {
			virtualIdComponent.setPropertyValues(
					entity,
					mappedIdentifierType.getPropertyValues( id, session ),
					entityMode
			);
		}
	}

	private static class IncrediblySillyJpaMapsIdMappedIdentifierValueMarshaller
			implements MappedIdentifierValueMarshaller {
		private final String entityName;
		private final SessionFactoryImplementor sessionFactory;
		private final ComponentType virtualIdComponent;
		private final ComponentType mappedIdentifierType;
		private final KeyValue identifier;

		private IncrediblySillyJpaMapsIdMappedIdentifierValueMarshaller(
				String entityName,
				SessionFactoryImplementor sessionFactory,
				ComponentType virtualIdComponent,
				ComponentType mappedIdentifierType,
				KeyValue identifier) {
			this.sessionFactory = sessionFactory;
			this.entityName = entityName;
			this.virtualIdComponent = virtualIdComponent;
			this.mappedIdentifierType = mappedIdentifierType;
			this.identifier = identifier;
		}

		@Override
		public Object getIdentifier(Object entity, EntityMode entityMode, SharedSessionContractImplementor session) {
			final Object id = mappedIdentifierType.instantiate( entityMode );
			final Object[] propertyValues = virtualIdComponent.getPropertyValues( entity, entityMode );
			final String[] names = virtualIdComponent.getPropertyNames();
			final Type[] subTypes = virtualIdComponent.getSubtypes();
			final Type[] copierSubTypes = mappedIdentifierType.getSubtypes();
			final int length = subTypes.length;
			for ( int i = 0; i < length; i++ ) {
				final Type subType = subTypes[i];
				if ( propertyValues[i] == null ) {
					if ( subType.isAssociationType() ) {
						throw new HibernateException( "No part of a composite identifier may be null" );
					}
					final Property p = ( (Component) identifier ).getProperty( i );
					final SimpleValue v = (SimpleValue) p.getValue();
					if ( v.getIdentifierGenerator() == null ) {
						throw new HibernateException( "No part of a composite identifier may be null" );
					}
				}
				//JPA 2 @MapsId + @IdClass points to the pk of the entity
				if ( subType.isAssociationType() && !copierSubTypes[i].isAssociationType()  ) {
					propertyValues[i] = determineEntityId(
							propertyValues[i],
							(AssociationType) subType,
							session,
							sessionFactory
					);
				}
			}
			mappedIdentifierType.setPropertyValues( id, propertyValues, entityMode );
			return id;
		}

		@Override
		public void setIdentifier(Object entity, Serializable id, EntityMode entityMode, SharedSessionContractImplementor session) {
			final Object[] extractedValues = mappedIdentifierType.getPropertyValues( id, entityMode );
			final Object[] injectionValues = new Object[extractedValues.length];
			final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
			final MetamodelImplementor metamodel = sessionFactory.getMetamodel();
			for ( int i = 0; i < virtualIdComponent.getSubtypes().length; i++ ) {
				final Type virtualPropertyType = virtualIdComponent.getSubtypes()[i];
				final Type idClassPropertyType = mappedIdentifierType.getSubtypes()[i];
				if ( virtualPropertyType.isEntityType() && !idClassPropertyType.isEntityType() ) {
					if ( session == null ) {
						throw new AssertionError(
								"Deprecated version of getIdentifier (no session) was used but session was required"
						);
					}
					final String associatedEntityName = ( (EntityType) virtualPropertyType ).getAssociatedEntityName();
					final EntityKey entityKey = session.generateEntityKey(
							(Serializable) extractedValues[i],
							metamodel.entityPersister( associatedEntityName )
					);
					// it is conceivable there is a proxy, so check that first
					Object association = persistenceContext.getProxy( entityKey );
					if ( association == null ) {
						// otherwise look for an initialized version
						association = persistenceContext.getEntity( entityKey );
						if ( association == null ) {
							// get the association out of the entity itself
							association = metamodel.entityPersister( entityName ).getPropertyValue(
									entity,
									virtualIdComponent.getPropertyNames()[i]
							);
						}
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

	private static Serializable determineEntityId(
			Object entity,
			AssociationType associationType,
			SharedSessionContractImplementor session,
			SessionFactoryImplementor sessionFactory) {
		if ( entity == null ) {
			return null;
		}

		if ( HibernateProxy.class.isInstance( entity ) ) {
			// entity is a proxy, so we know it is not transient; just return ID from proxy
			return ( (HibernateProxy) entity ).getHibernateLazyInitializer().getIdentifier();
		}

		if ( session != null ) {
			final EntityEntry pcEntry = session.getPersistenceContextInternal().getEntry( entity );
			if ( pcEntry != null ) {
				// entity managed; return ID.
				return pcEntry.getId();
			}
		}

		final EntityPersister persister = resolveEntityPersister(
				entity,
				associationType,
				session,
				sessionFactory
		);

		return persister.getIdentifier( entity, session );
	}

	private static EntityPersister resolveEntityPersister(
			Object entity,
			AssociationType associationType,
			SharedSessionContractImplementor session,
			SessionFactoryImplementor sessionFactory) {
		assert sessionFactory != null;

		if ( session != null ) {
			return session.getEntityPersister(
					associationType.getAssociatedEntityName( sessionFactory ),
					entity
			);
		}

		String entityName = null;
		final MetamodelImplementor metamodel = sessionFactory.getMetamodel();
		for ( EntityNameResolver entityNameResolver : metamodel.getEntityNameResolvers() ) {
			entityName = entityNameResolver.resolveEntityName( entity );
			if ( entityName != null ) {
				break;
			}
		}
		if ( entityName == null ) {
			// old fall-back
			entityName = entity.getClass().getName();
		}

		return metamodel.entityPersister( entityName );
	}

	@Override
	public void resetIdentifier(Object entity, Serializable currentId, Object currentVersion) {
		// 99% of the time the session is not needed.  Its only needed for certain brain-dead
		// interpretations of JPA 2 "derived identity" support
		resetIdentifier( entity, currentId, currentVersion, null );
	}

	@Override
	public void resetIdentifier(
			Object entity,
			Serializable currentId,
			Object currentVersion,
			SharedSessionContractImplementor session) {
		//noinspection StatementWithEmptyBody
		final IdentifierProperty identifierProperty = entityMetamodel.getIdentifierProperty();
		if ( identifierProperty.getIdentifierGenerator() instanceof Assigned ) {
		}
		else {
			//reset the id
			Serializable result = identifierProperty
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

	@Override
	public Object getVersion(Object entity) throws HibernateException {
		if ( !entityMetamodel.isVersioned() ) {
			return null;
		}
		return getters[entityMetamodel.getVersionPropertyIndex()].get( entity );
	}

	protected boolean shouldGetAllProperties(Object entity) {
		final BytecodeEnhancementMetadata bytecodeEnhancementMetadata = getEntityMetamodel().getBytecodeEnhancementMetadata();
		if ( !bytecodeEnhancementMetadata.isEnhancedForLazyLoading() ) {
			return true;
		}

		return !bytecodeEnhancementMetadata.hasUnFetchedAttributes( entity );
	}

	@Override
	public Object[] getPropertyValues(Object entity) {
		final BytecodeEnhancementMetadata enhancementMetadata = entityMetamodel.getBytecodeEnhancementMetadata();
		final LazyAttributesMetadata lazyAttributesMetadata = enhancementMetadata.getLazyAttributesMetadata();

		final int span = entityMetamodel.getPropertySpan();
		final String[] propertyNames = entityMetamodel.getPropertyNames();
		final Object[] result = new Object[span];

		for ( int j = 0; j < span; j++ ) {
			final String propertyName = propertyNames[j];
			// if the attribute is not lazy (bytecode sense), we can just use the value from the instance
			// if the attribute is lazy but has been initialized we can just use the value from the instance
			// todo : there should be a third case here when we merge transient instances
			if ( ! lazyAttributesMetadata.isLazyAttribute( propertyName )
					|| enhancementMetadata.isAttributeLoaded( entity, propertyName) ) {
				result[j] = getters[j].get( entity );
			}
			else {
				result[j] = LazyPropertyInitializer.UNFETCHED_PROPERTY;
			}
		}

		return result;
	}

	@Override
	public Object[] getPropertyValuesToInsert(Object entity, Map mergeMap, SharedSessionContractImplementor session) {
		final int span = entityMetamodel.getPropertySpan();
		final Object[] result = new Object[span];

		for ( int j = 0; j < span; j++ ) {
			result[j] = getters[j].getForInsert( entity, mergeMap, session );
		}
		return result;
	}

	@Override
	public Object getPropertyValue(Object entity, int i) throws HibernateException {
		return getters[i].get( entity );
	}

	@Override
	public Object getPropertyValue(Object entity, String propertyPath) throws HibernateException {
		int loc = propertyPath.indexOf( '.' );
		String basePropertyName = loc > 0
				? propertyPath.substring( 0, loc )
				: propertyPath;
		//final int index = entityMetamodel.getPropertyIndexOrNull( basePropertyName );
		Integer index = entityMetamodel.getPropertyIndexOrNull( basePropertyName );
		if ( index == null ) {
			propertyPath = PropertyPath.IDENTIFIER_MAPPER_PROPERTY + "." + propertyPath;
			loc = propertyPath.indexOf( '.' );
			basePropertyName = loc > 0
					? propertyPath.substring( 0, loc )
					: propertyPath;
		}
		index = entityMetamodel.getPropertyIndexOrNull( basePropertyName );
		final Object baseValue = getPropertyValue( entity, index );
		if ( loc > 0 ) {
			if ( baseValue == null ) {
				return null;
			}
			return getComponentValue(
					(ComponentType) entityMetamodel.getPropertyTypes()[index],
					baseValue,
					propertyPath.substring( loc + 1 )
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
	 *
	 * @return The property value extracted.
	 */
	protected Object getComponentValue(ComponentType type, Object component, String propertyPath) {
		final int loc = propertyPath.indexOf( '.' );
		final String basePropertyName = loc > 0
				? propertyPath.substring( 0, loc )
				: propertyPath;
		final int index = findSubPropertyIndex( type, basePropertyName );
		final Object baseValue = type.getPropertyValue( component, index );
		if ( loc > 0 ) {
			if ( baseValue == null ) {
				return null;
			}
			return getComponentValue(
					(ComponentType) type.getSubtypes()[index],
					baseValue,
					propertyPath.substring( loc + 1 )
			);
		}
		else {
			return baseValue;
		}

	}

	private int findSubPropertyIndex(ComponentType type, String subPropertyName) {
		final String[] propertyNames = type.getPropertyNames();
		for ( int index = 0; index < propertyNames.length; index++ ) {
			if ( subPropertyName.equals( propertyNames[index] ) ) {
				return index;
			}
		}
		throw new MappingException( "component property not found: " + subPropertyName );
	}

	@Override
	public void setPropertyValues(Object entity, Object[] values) throws HibernateException {
		boolean setAll = !entityMetamodel.hasLazyProperties();

		final SessionFactoryImplementor factory = getFactory();
		for ( int j = 0; j < entityMetamodel.getPropertySpan(); j++ ) {
			if ( setAll || values[j] != LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
				setters[j].set( entity, values[j], factory );
			}
		}
	}

	@Override
	public void setPropertyValue(Object entity, int i, Object value) throws HibernateException {
		setters[i].set( entity, value, getFactory() );
	}

	@Override
	public void setPropertyValue(Object entity, String propertyName, Object value) throws HibernateException {
		setters[entityMetamodel.getPropertyIndex( propertyName )].set( entity, value, getFactory() );
	}

	@Override
	public final Object instantiate(Serializable id) throws HibernateException {
		// 99% of the time the session is not needed.  Its only needed for certain brain-dead
		// interpretations of JPA 2 "derived identity" support
		return instantiate( id, null );
	}

	@Override
	public final Object instantiate(Serializable id, SharedSessionContractImplementor session) {
		Object result = getInstantiator().instantiate( id );
		if ( id != null ) {
			setIdentifier( result, id, session );
		}
		return result;
	}

	@Override
	public final Object instantiate() throws HibernateException {
		return instantiate( null, null );
	}

	@Override
	public void afterInitialize(Object entity, SharedSessionContractImplementor session) {
	}

	@Override
	public final boolean isInstance(Object object) {
		return getInstantiator().isInstance( object );
	}

	@Override
	public boolean hasProxy() {
		return entityMetamodel.isLazy() && !entityMetamodel.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading();
	}

	@Override
	public final Object createProxy(Serializable id, SharedSessionContractImplementor session) {
		return getProxyFactory().getProxy( id, session );
	}

	@Override
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

	@Override
	public final ProxyFactory getProxyFactory() {
		return proxyFactory;
	}

	@Override
	public String toString() {
		return getClass().getName() + '(' + getEntityMetamodel().getName() + ')';
	}

	@Override
	public Getter getIdentifierGetter() {
		return idGetter;
	}

	@Override
	public Getter getVersionGetter() {
		final EntityMetamodel entityMetamodel = getEntityMetamodel();
		if ( entityMetamodel.isVersioned() ) {
			return getGetter( entityMetamodel.getVersionPropertyIndex() );
		}
		return null;
	}

	@Override
	public Getter getGetter(int i) {
		return getters[i];
	}
}
