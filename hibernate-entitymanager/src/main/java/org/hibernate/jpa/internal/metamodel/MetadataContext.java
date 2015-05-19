/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal.metamodel;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.MappedSuperclassType;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.internal.EntityManagerMessageLogger;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import static org.hibernate.jpa.internal.HEMLogging.messageLogger;

/**
 * Defines a context for storing information during the building of the {@link MetamodelImpl}.
 * <p/>
 * This contextual information includes data needing to be processed in a second pass as well as
 * cross-references into the built metamodel classes.
 * <p/>
 * At the end of the day, clients are interested in the {@link #getEntityTypeMap} and {@link #getEmbeddableTypeMap}
 * results, which represent all the registered {@linkplain #registerEntityType entities} and
 * {@linkplain #registerEmbeddedableType embeddables} respectively.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
class MetadataContext {
	private static final EntityManagerMessageLogger LOG = messageLogger( MetadataContext.class );

	private final SessionFactoryImplementor sessionFactory;
	private Set<MappedSuperclass> knownMappedSuperclasses;
	private final boolean ignoreUnsupported;
	private final AttributeFactory attributeFactory = new AttributeFactory( this );

	private Map<Class<?>, EntityTypeImpl<?>> entityTypes
			= new HashMap<Class<?>, EntityTypeImpl<?>>();
	private Map<String, EntityTypeImpl<?>> entityTypesByEntityName
			= new HashMap<String, EntityTypeImpl<?>>();
	private Map<PersistentClass, EntityTypeImpl<?>> entityTypesByPersistentClass
			= new HashMap<PersistentClass, EntityTypeImpl<?>>();
	private Map<Class<?>, EmbeddableTypeImpl<?>> embeddables
			= new HashMap<Class<?>, EmbeddableTypeImpl<?>>();
	private Map<MappedSuperclass, MappedSuperclassTypeImpl<?>> mappedSuperclassByMappedSuperclassMapping
			= new HashMap<MappedSuperclass, MappedSuperclassTypeImpl<?>>();
	//this list contains MappedSuperclass and EntityTypes ordered by superclass first
	private List<Object> orderedMappings = new ArrayList<Object>();
	/**
	 * Stack of PersistentClass being process. Last in the list is the highest in the stack.
	 */
	private List<PersistentClass> stackOfPersistentClassesBeingProcessed
			= new ArrayList<PersistentClass>();
	private Map<MappedSuperclassTypeImpl<?>, PersistentClass> mappedSuperClassTypeToPersistentClass
			= new HashMap<MappedSuperclassTypeImpl<?>, PersistentClass>();

	public MetadataContext(
			SessionFactoryImplementor sessionFactory,
			Set<MappedSuperclass> mappedSuperclasses,
			boolean ignoreUnsupported) {
		this.sessionFactory = sessionFactory;
		this.knownMappedSuperclasses = mappedSuperclasses;
		this.ignoreUnsupported = ignoreUnsupported;
	}

	/*package*/ SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	/*package*/ boolean isIgnoreUnsupported() {
		return ignoreUnsupported;
	}

	/**
	 * Retrieves the {@linkplain Class java type} to {@link EntityTypeImpl} map.
	 *
	 * @return The {@linkplain Class java type} to {@link EntityTypeImpl} map.
	 */
	public Map<Class<?>, EntityTypeImpl<?>> getEntityTypeMap() {
		return Collections.unmodifiableMap( entityTypes );
	}

	public Map<Class<?>, EmbeddableTypeImpl<?>> getEmbeddableTypeMap() {
		return Collections.unmodifiableMap( embeddables );
	}

	public Map<Class<?>, MappedSuperclassType<?>> getMappedSuperclassTypeMap() {
		// we need to actually build this map...
		final Map<Class<?>, MappedSuperclassType<?>> mappedSuperClassTypeMap = CollectionHelper.mapOfSize(
				mappedSuperclassByMappedSuperclassMapping.size()
		);

		for ( MappedSuperclassTypeImpl mappedSuperclassType : mappedSuperclassByMappedSuperclassMapping.values() ) {
			mappedSuperClassTypeMap.put(
					mappedSuperclassType.getJavaType(),
					mappedSuperclassType
			);
		}

		return mappedSuperClassTypeMap;
	}

	/*package*/ void registerEntityType(PersistentClass persistentClass, EntityTypeImpl<?> entityType) {
		entityTypes.put( entityType.getBindableJavaType(), entityType );
		entityTypesByEntityName.put( persistentClass.getEntityName(), entityType );
		entityTypesByPersistentClass.put( persistentClass, entityType );
		orderedMappings.add( persistentClass );
	}

	/*package*/ void registerEmbeddedableType(EmbeddableTypeImpl<?> embeddableType) {
		embeddables.put( embeddableType.getJavaType(), embeddableType );
	}

	/*package*/ void registerMappedSuperclassType(
			MappedSuperclass mappedSuperclass,
			MappedSuperclassTypeImpl<?> mappedSuperclassType) {
		mappedSuperclassByMappedSuperclassMapping.put( mappedSuperclass, mappedSuperclassType );
		orderedMappings.add( mappedSuperclass );
		mappedSuperClassTypeToPersistentClass.put( mappedSuperclassType, getEntityWorkedOn() );

		knownMappedSuperclasses.remove( mappedSuperclass );
	}

	/**
	 * Given a Hibernate {@link PersistentClass}, locate the corresponding JPA {@link org.hibernate.type.EntityType}
	 * implementation.  May retur null if the given {@link PersistentClass} has not yet been processed.
	 *
	 * @param persistentClass The Hibernate (config time) metamodel instance representing an entity.
	 *
	 * @return Tne corresponding JPA {@link org.hibernate.type.EntityType}, or null if not yet processed.
	 */
	public EntityTypeImpl<?> locateEntityType(PersistentClass persistentClass) {
		return entityTypesByPersistentClass.get( persistentClass );
	}

	/**
	 * Given a Java {@link Class}, locate the corresponding JPA {@link org.hibernate.type.EntityType}.  May
	 * return null which could means that no such mapping exists at least at this time.
	 *
	 * @param javaType The java class.
	 *
	 * @return The corresponding JPA {@link org.hibernate.type.EntityType}, or null.
	 */
	public EntityTypeImpl<?> locateEntityType(Class<?> javaType) {
		return entityTypes.get( javaType );
	}

	/**
	 * Given an entity-name, locate the corresponding JPA {@link org.hibernate.type.EntityType}.  May
	 * return null which could means that no such mapping exists at least at this time.
	 *
	 * @param entityName The entity-name.
	 *
	 * @return The corresponding JPA {@link org.hibernate.type.EntityType}, or null.
	 */
	public EntityTypeImpl<?> locateEntityType(String entityName) {
		return entityTypesByEntityName.get( entityName );
	}

	public Map<String, EntityTypeImpl<?>> getEntityTypesByEntityName() {
		return Collections.unmodifiableMap( entityTypesByEntityName );
	}

	@SuppressWarnings({"unchecked"})
	public void wrapUp() {
		LOG.trace( "Wrapping up metadata context..." );

		//we need to process types from superclasses to subclasses
		for ( Object mapping : orderedMappings ) {
			if ( PersistentClass.class.isAssignableFrom( mapping.getClass() ) ) {
				@SuppressWarnings("unchecked")
				final PersistentClass safeMapping = (PersistentClass) mapping;
				LOG.trace( "Starting entity [" + safeMapping.getEntityName() + "]" );
				try {
					final EntityTypeImpl<?> jpa2Mapping = entityTypesByPersistentClass.get( safeMapping );
					applyIdMetadata( safeMapping, jpa2Mapping );
					applyVersionAttribute( safeMapping, jpa2Mapping );
					Iterator<Property> properties = safeMapping.getDeclaredPropertyIterator();
					while ( properties.hasNext() ) {
						final Property property = properties.next();
						if ( property.getValue() == safeMapping.getIdentifierMapper() ) {
							// property represents special handling for id-class mappings but we have already
							// accounted for the embedded property mappings in #applyIdMetadata &&
							// #buildIdClassAttributes
							continue;
						}
						if ( safeMapping.isVersioned() && property == safeMapping.getVersion() ) {
							// skip the version property, it was already handled previously.
							continue;
						}
						final Attribute attribute = attributeFactory.buildAttribute( jpa2Mapping, property );
						if ( attribute != null ) {
							jpa2Mapping.getBuilder().addAttribute( attribute );
						}
					}
					jpa2Mapping.lock();
					populateStaticMetamodel( jpa2Mapping );
				}
				finally {
					LOG.trace( "Completed entity [" + safeMapping.getEntityName() + "]" );
				}
			}
			else if ( MappedSuperclass.class.isAssignableFrom( mapping.getClass() ) ) {
				@SuppressWarnings("unchecked")
				final MappedSuperclass safeMapping = (MappedSuperclass) mapping;
				LOG.trace( "Starting mapped superclass [" + safeMapping.getMappedClass().getName() + "]" );
				try {
					final MappedSuperclassTypeImpl<?> jpa2Mapping = mappedSuperclassByMappedSuperclassMapping.get(
							safeMapping
					);
					applyIdMetadata( safeMapping, jpa2Mapping );
					applyVersionAttribute( safeMapping, jpa2Mapping );
					Iterator<Property> properties = safeMapping.getDeclaredPropertyIterator();
					while ( properties.hasNext() ) {
						final Property property = properties.next();
						if ( safeMapping.isVersioned() && property == safeMapping.getVersion() ) {
							// skip the version property, it was already handled previously.
							continue;
						}
						final Attribute attribute = attributeFactory.buildAttribute( jpa2Mapping, property );
						if ( attribute != null ) {
							jpa2Mapping.getBuilder().addAttribute( attribute );
						}
					}
					jpa2Mapping.lock();
					populateStaticMetamodel( jpa2Mapping );
				}
				finally {
					LOG.trace( "Completed mapped superclass [" + safeMapping.getMappedClass().getName() + "]" );
				}
			}
			else {
				throw new AssertionFailure( "Unexpected mapping type: " + mapping.getClass() );
			}
		}

		for ( EmbeddableTypeImpl embeddable : embeddables.values() ) {
			populateStaticMetamodel( embeddable );
		}
	}


	private <X> void applyIdMetadata(PersistentClass persistentClass, EntityTypeImpl<X> jpaEntityType) {
		if ( persistentClass.hasIdentifierProperty() ) {
			final Property declaredIdentifierProperty = persistentClass.getDeclaredIdentifierProperty();
			if ( declaredIdentifierProperty != null ) {
				jpaEntityType.getBuilder().applyIdAttribute(
						attributeFactory.buildIdAttribute( jpaEntityType, declaredIdentifierProperty )
				);
			}
		}
		else if ( persistentClass.hasIdentifierMapper() ) {
			@SuppressWarnings("unchecked")
			Iterator<Property> propertyIterator = persistentClass.getIdentifierMapper().getPropertyIterator();
			Set<SingularAttribute<? super X, ?>> attributes = buildIdClassAttributes( jpaEntityType, propertyIterator );
			jpaEntityType.getBuilder().applyIdClassAttributes( attributes );
		}
		else {
			final KeyValue value = persistentClass.getIdentifier();
			if ( value instanceof Component ) {
				final Component component = (Component) value;
				if ( component.getPropertySpan() > 1 ) {
					//FIXME we are an Hibernate embedded id (ie not type)
				}
				else {
					//FIXME take care of declared vs non declared property
					jpaEntityType.getBuilder().applyIdAttribute(
							attributeFactory.buildIdAttribute(
									jpaEntityType,
									(Property) component.getPropertyIterator().next()
							)
					);
				}
			}
		}
	}

	private <X> void applyIdMetadata(MappedSuperclass mappingType, MappedSuperclassTypeImpl<X> jpaMappingType) {
		if ( mappingType.hasIdentifierProperty() ) {
			final Property declaredIdentifierProperty = mappingType.getDeclaredIdentifierProperty();
			if ( declaredIdentifierProperty != null ) {
				jpaMappingType.getBuilder().applyIdAttribute(
						attributeFactory.buildIdAttribute( jpaMappingType, declaredIdentifierProperty )
				);
			}
		}
		//an MappedSuperclass can have no identifier if the id is set below in the hierarchy
		else if ( mappingType.getIdentifierMapper() != null ) {
			@SuppressWarnings("unchecked")
			Iterator<Property> propertyIterator = mappingType.getIdentifierMapper().getPropertyIterator();
			Set<SingularAttribute<? super X, ?>> attributes = buildIdClassAttributes(
					jpaMappingType,
					propertyIterator
			);
			jpaMappingType.getBuilder().applyIdClassAttributes( attributes );
		}
	}

	private <X> void applyVersionAttribute(PersistentClass persistentClass, EntityTypeImpl<X> jpaEntityType) {
		final Property declaredVersion = persistentClass.getDeclaredVersion();
		if ( declaredVersion != null ) {
			jpaEntityType.getBuilder().applyVersionAttribute(
					attributeFactory.buildVersionAttribute( jpaEntityType, declaredVersion )
			);
		}
	}

	private <X> void applyVersionAttribute(MappedSuperclass mappingType, MappedSuperclassTypeImpl<X> jpaMappingType) {
		final Property declaredVersion = mappingType.getDeclaredVersion();
		if ( declaredVersion != null ) {
			jpaMappingType.getBuilder().applyVersionAttribute(
					attributeFactory.buildVersionAttribute( jpaMappingType, declaredVersion )
			);
		}
	}

	private <X> Set<SingularAttribute<? super X, ?>> buildIdClassAttributes(
			AbstractIdentifiableType<X> ownerType,
			Iterator<Property> propertyIterator) {
		LOG.trace( "Building old-school composite identifier [" + ownerType.getJavaType().getName() + "]" );
		Set<SingularAttribute<? super X, ?>> attributes = new HashSet<SingularAttribute<? super X, ?>>();
		while ( propertyIterator.hasNext() ) {
			attributes.add( attributeFactory.buildIdAttribute( ownerType, propertyIterator.next() ) );
		}
		return attributes;
	}

	private <X> void populateStaticMetamodel(AbstractManagedType<X> managedType) {
		final Class<X> managedTypeClass = managedType.getJavaType();
		if ( managedTypeClass == null ) {
			// should indicate MAP entity mode, skip...
			return;
		}
		final String metamodelClassName = managedTypeClass.getName() + "_";
		try {
			final Class metamodelClass = Class.forName( metamodelClassName, true, managedTypeClass.getClassLoader() );
			// we found the class; so populate it...
			registerAttributes( metamodelClass, managedType );
		}
		catch (ClassNotFoundException ignore) {
			// nothing to do...
		}

		// todo : this does not account for @MappeSuperclass, mainly because this is not being tracked in our
		// internal metamodel as populated from the annotatios properly
		AbstractManagedType<? super X> superType = managedType.getSupertype();
		if ( superType != null ) {
			populateStaticMetamodel( superType );
		}
	}

	private final Set<Class> processedMetamodelClasses = new HashSet<Class>();

	private <X> void registerAttributes(Class metamodelClass, AbstractManagedType<X> managedType) {
		if ( !processedMetamodelClasses.add( metamodelClass ) ) {
			return;
		}

		// push the attributes on to the metamodel class...
		for ( Attribute<X, ?> attribute : managedType.getDeclaredAttributes() ) {
			registerAttribute( metamodelClass, attribute );
		}

		if ( IdentifiableType.class.isInstance( managedType ) ) {
			final AbstractIdentifiableType<X> entityType = (AbstractIdentifiableType<X>) managedType;

			// handle version
			if ( entityType.hasDeclaredVersionAttribute() ) {
				registerAttribute( metamodelClass, entityType.getDeclaredVersion() );
			}

			// handle id-class mappings specially
			if ( entityType.hasIdClass() ) {
				final Set<SingularAttribute<? super X, ?>> attributes = entityType.getIdClassAttributesSafely();
				if ( attributes != null ) {
					for ( SingularAttribute<? super X, ?> attribute : attributes ) {
						registerAttribute( metamodelClass, attribute );
					}
				}
			}
		}
	}

	private <X> void registerAttribute(Class metamodelClass, Attribute<X, ?> attribute) {
		final String name = attribute.getName();
		try {
			// there is a shortcoming in the existing Hibernate code in terms of the way MappedSuperclass
			// support was bolted on which comes to bear right here when the attribute is an embeddable type
			// defined on a MappedSuperclass.  We do not have the correct information to determine the
			// appropriate attribute declarer in such cases and so the incoming metamodelClass most likely
			// does not represent the declarer in such cases.
			//
			// As a result, in the case of embeddable classes we simply use getField rather than get
			// getDeclaredField
			final Field field = attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.EMBEDDED
					? metamodelClass.getField( name )
					: metamodelClass.getDeclaredField( name );
			try {
				// should be public anyway, but to be sure...
				field.setAccessible( true );
				field.set( null, attribute );
			}
			catch (IllegalAccessException e) {
				// todo : exception type?
				throw new AssertionFailure(
						"Unable to inject static metamodel attribute : " + metamodelClass.getName() + '#' + name,
						e
				);
			}
			catch (IllegalArgumentException e) {
				// most likely a mismatch in the type we are injecting and the defined field; this represents a
				// mismatch in how the annotation processor interpretted the attribute and how our metamodel
				// and/or annotation binder did.

//              This is particularly the case as arrays are nto handled propery by the StaticMetamodel generator

//				throw new AssertionFailure(
//						"Illegal argument on static metamodel field injection : " + metamodelClass.getName() + '#' + name
//								+ "; expected type :  " + attribute.getClass().getName()
//								+ "; encountered type : " + field.getType().getName()
//				);
				LOG.illegalArgumentOnStaticMetamodelFieldInjection(
						metamodelClass.getName(),
						name,
						attribute.getClass().getName(),
						field.getType().getName()
				);
			}
		}
		catch (NoSuchFieldException e) {
			LOG.unableToLocateStaticMetamodelField( metamodelClass.getName(), name );
//			throw new AssertionFailure(
//					"Unable to locate static metamodel field : " + metamodelClass.getName() + '#' + name
//			);
		}
	}

	public MappedSuperclassTypeImpl<?> locateMappedSuperclassType(MappedSuperclass mappedSuperclass) {
		return mappedSuperclassByMappedSuperclassMapping.get( mappedSuperclass );
	}

	public void pushEntityWorkedOn(PersistentClass persistentClass) {
		stackOfPersistentClassesBeingProcessed.add( persistentClass );
	}

	public void popEntityWorkedOn(PersistentClass persistentClass) {
		final PersistentClass stackTop = stackOfPersistentClassesBeingProcessed.remove(
				stackOfPersistentClassesBeingProcessed.size() - 1
		);
		if ( stackTop != persistentClass ) {
			throw new AssertionFailure(
					"Inconsistent popping: "
							+ persistentClass.getEntityName() + " instead of " + stackTop.getEntityName()
			);
		}
	}

	private PersistentClass getEntityWorkedOn() {
		return stackOfPersistentClassesBeingProcessed.get(
				stackOfPersistentClassesBeingProcessed.size() - 1
		);
	}

	public PersistentClass getPersistentClassHostingProperties(MappedSuperclassTypeImpl<?> mappedSuperclassType) {
		final PersistentClass persistentClass = mappedSuperClassTypeToPersistentClass.get( mappedSuperclassType );
		if ( persistentClass == null ) {
			throw new AssertionFailure(
					"Could not find PersistentClass for MappedSuperclassType: "
							+ mappedSuperclassType.getJavaType()
			);
		}
		return persistentClass;
	}

	public Set<MappedSuperclass> getUnusedMappedSuperclasses() {
		return new HashSet<MappedSuperclass>( knownMappedSuperclasses );
	}
}
