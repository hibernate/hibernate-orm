/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.internal;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.internal.EntityManagerMessageLogger;
import org.hibernate.internal.HEMLogging;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.model.domain.AbstractIdentifiableType;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.MappedSuperclassDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.internal.AttributeContainer;
import org.hibernate.metamodel.model.domain.internal.BasicTypeImpl;
import org.hibernate.metamodel.model.domain.internal.EmbeddableTypeImpl;
import org.hibernate.metamodel.model.domain.internal.EntityTypeImpl;
import org.hibernate.metamodel.model.domain.internal.MappedSuperclassTypeImpl;
import org.hibernate.metamodel.model.domain.internal.MappingMetamodelImpl;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.Type;

/**
 * Defines a context for storing information during the building of the {@link MappingMetamodelImpl}.
 * <p/>
 * This contextual information includes data needing to be processed in a second pass as well as
 * cross-references into the built metamodel classes.
 * <p/>
 * At the end of the day, clients are interested in the {@link #getEntityTypeMap} and {@link #getEmbeddableTypeSet}
 * results, which represent all the registered {@linkplain #registerEntityType entities} and
 * {@linkplain #registerEmbeddableType embeddables} respectively.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
@Internal
public class MetadataContext {
	private static final EntityManagerMessageLogger LOG = HEMLogging.messageLogger( MetadataContext.class );

	private final JpaMetamodel jpaMetamodel;
	private final RuntimeModelCreationContext runtimeModelCreationContext;

	private Set<MappedSuperclass> knownMappedSuperclasses;
	private TypeConfiguration typeConfiguration;
	private final JpaStaticMetaModelPopulationSetting jpaStaticMetaModelPopulationSetting;
	private final AttributeFactory attributeFactory = new AttributeFactory( this );

	private Map<Class<?>, EntityDomainType<?>> entityTypes = new HashMap<>();
	private Map<String, EntityDomainType<?>> entityTypesByEntityName = new HashMap<>();
	private Map<PersistentClass, EntityDomainType<?>> entityTypesByPersistentClass = new HashMap<>();

	private Map<Class, EmbeddableDomainType<?>> embeddables = new HashMap<>();
	private Map<Class, List<EmbeddableDomainType<?>>> embeddablesToProcess = new HashMap<>();
	private Map<EmbeddableDomainType<?>, Component> componentByEmbeddable = new HashMap<>();

	private Map<MappedSuperclass, MappedSuperclassDomainType<?>> mappedSuperclassByMappedSuperclassMapping = new HashMap<>();
	private Map<MappedSuperclassDomainType<?>, PersistentClass> mappedSuperClassTypeToPersistentClass = new HashMap<>();

	//this list contains MappedSuperclass and EntityTypes ordered by superclass first
	private List<Object> orderedMappings = new ArrayList<>();

	/**
	 * Stack of PersistentClass being processed. Last in the list is the highest in the stack.
	 */
	private List<PersistentClass> stackOfPersistentClassesBeingProcessed = new ArrayList<>();
	private MappingMetamodel metamodel;

	public MetadataContext(
			JpaMetamodel jpaMetamodel,
			MappingMetamodel mappingMetamodel,
			MetadataImplementor bootMetamodel,
			JpaStaticMetaModelPopulationSetting jpaStaticMetaModelPopulationSetting,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		this.jpaMetamodel = jpaMetamodel;
		this.metamodel = mappingMetamodel;
		this.knownMappedSuperclasses = bootMetamodel.getMappedSuperclassMappingsCopy();
		this.typeConfiguration = runtimeModelCreationContext.getTypeConfiguration();
		this.jpaStaticMetaModelPopulationSetting = jpaStaticMetaModelPopulationSetting;
		this.runtimeModelCreationContext = runtimeModelCreationContext;
	}

	public RuntimeModelCreationContext getRuntimeModelCreationContext() {
		return runtimeModelCreationContext;
	}

	public JpaMetamodel getJpaMetamodel() {
		return jpaMetamodel;
	}

	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	public JavaTypeRegistry getJavaTypeDescriptorRegistry(){
		return typeConfiguration.getJavaTypeDescriptorRegistry();
	}

	MappingMetamodel getMetamodel() {
		return metamodel;
	}

	/**
	 * Retrieves the {@linkplain Class java type} to {@link EntityTypeImpl} map.
	 *
	 * @return The {@linkplain Class java type} to {@link EntityTypeImpl} map.
	 */
	public Map<Class<?>, EntityDomainType<?>> getEntityTypeMap() {
		return Collections.unmodifiableMap( entityTypes );
	}

	public Set<EmbeddableDomainType<?>> getEmbeddableTypeSet() {
		return new HashSet<>( embeddables.values() );
	}

	public Map<Class<?>, MappedSuperclassDomainType<?>> getMappedSuperclassTypeMap() {
		// we need to actually build this map...
		final Map<Class<?>, MappedSuperclassDomainType<?>> mappedSuperClassTypeMap = CollectionHelper.mapOfSize(
				mappedSuperclassByMappedSuperclassMapping.size()
		);

		for ( MappedSuperclassDomainType mappedSuperclassType : mappedSuperclassByMappedSuperclassMapping.values() ) {
			mappedSuperClassTypeMap.put(
					mappedSuperclassType.getJavaType(),
					mappedSuperclassType
			);
		}

		return mappedSuperClassTypeMap;
	}

	public  void registerEntityType(PersistentClass persistentClass, EntityTypeImpl<?> entityType) {
		if ( entityType.getBindableJavaType() != null ) {
			entityTypes.put( entityType.getBindableJavaType(), entityType );
		}

		entityTypesByEntityName.put( persistentClass.getEntityName(), entityType );
		entityTypesByPersistentClass.put( persistentClass, entityType );
		orderedMappings.add( persistentClass );
	}

	public void registerEmbeddableType(
			EmbeddableDomainType<?> embeddableType,
			Component bootDescriptor) {
		assert embeddableType.getJavaType() != null;
		assert ! Map.class.isAssignableFrom( embeddableType.getJavaType() );

		embeddablesToProcess
				.computeIfAbsent( embeddableType.getJavaType(), k -> new ArrayList<>( 1 ) )
				.add( embeddableType );
		registerComponentByEmbeddable( embeddableType, bootDescriptor );
	}

	public void registerComponentByEmbeddable(
			EmbeddableDomainType<?> embeddableType,
			Component bootDescriptor) {
		componentByEmbeddable.put( embeddableType, bootDescriptor );
	}

	public Component getEmbeddableBootDescriptor(EmbeddableDomainType<?> embeddableType) {
		return componentByEmbeddable.get( embeddableType );
	}

	public void registerMappedSuperclassType(
			MappedSuperclass mappedSuperclass,
			MappedSuperclassDomainType<?> mappedSuperclassType) {
		mappedSuperclassByMappedSuperclassMapping.put( mappedSuperclass, mappedSuperclassType );
		orderedMappings.add( mappedSuperclass );
		mappedSuperClassTypeToPersistentClass.put( mappedSuperclassType, getEntityWorkedOn() );

		knownMappedSuperclasses.remove( mappedSuperclass );
	}

	/**
	 * Given a Hibernate {@link PersistentClass}, locate the corresponding JPA {@link org.hibernate.type.EntityType}
	 * implementation.  May return null if the given {@link PersistentClass} has not yet been processed.
	 *
	 * @param persistentClass The Hibernate (config time) metamodel instance representing an entity.
	 *
	 * @return Tne corresponding JPA {@link org.hibernate.type.EntityType}, or null if not yet processed.
	 */
	@SuppressWarnings("WeakerAccess")
	public EntityDomainType<?> locateEntityType(PersistentClass persistentClass) {
		return entityTypesByPersistentClass.get( persistentClass );
	}

	/**
	 * Given a Java {@link Class}, locate the corresponding JPA {@link org.hibernate.type.EntityType}.  May
	 * return null which could mean that no such mapping exists at least at this time.
	 *
	 * @param javaType The java class.
	 *
	 * @return The corresponding JPA {@link org.hibernate.type.EntityType}, or null.
	 */
	public EntityDomainType<?> locateEntityType(Class<?> javaType) {
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
	@SuppressWarnings({"unchecked", "WeakerAccess"})
	public <E> EntityDomainType<E> locateEntityType(String entityName) {
		return (EntityDomainType) entityTypesByEntityName.get( entityName );
	}

	@SuppressWarnings("WeakerAccess")
	public Map<String, EntityDomainType<?>> getEntityTypesByEntityName() {
		return Collections.unmodifiableMap( entityTypesByEntityName );
	}

	@SuppressWarnings({"unchecked", "WeakerAccess"})
	public void wrapUp() {
		if ( LOG.isTraceEnabled() ) {
			LOG.trace( "Wrapping up metadata context..." );
		}

		boolean staticMetamodelScanEnabled = this.jpaStaticMetaModelPopulationSetting != JpaStaticMetaModelPopulationSetting.DISABLED;

		//we need to process types from superclasses to subclasses
		for ( Object mapping : orderedMappings ) {
			if ( PersistentClass.class.isAssignableFrom( mapping.getClass() ) ) {
				final PersistentClass safeMapping = (PersistentClass) mapping;
				if ( LOG.isTraceEnabled() ) {
					LOG.trace( "Starting entity [" + safeMapping.getEntityName() + ']' );
				}
				try {
					final EntityDomainType<?> jpaMapping = entityTypesByPersistentClass.get( safeMapping );

					applyIdMetadata( safeMapping, jpaMapping );
					applyVersionAttribute( safeMapping, jpaMapping );

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
						final PersistentAttribute attribute = attributeFactory.buildAttribute(
								jpaMapping,
								property
						);
						if ( attribute != null ) {
							( (AttributeContainer) jpaMapping ).getInFlightAccess().addAttribute( attribute );
						}
					}

					( (AttributeContainer) jpaMapping ).getInFlightAccess().finishUp();

					if ( staticMetamodelScanEnabled ) {
						populateStaticMetamodel( jpaMapping );
					}
				}
				finally {
					if ( LOG.isTraceEnabled() ) {
						LOG.trace( "Completed entity [" + safeMapping.getEntityName() + ']' );
					}
				}
			}
			else if ( MappedSuperclass.class.isAssignableFrom( mapping.getClass() ) ) {
				final MappedSuperclass safeMapping = (MappedSuperclass) mapping;
				if ( LOG.isTraceEnabled() ) {
					LOG.trace( "Starting mapped superclass [" + safeMapping.getMappedClass().getName() + ']' );
				}
				try {
					final MappedSuperclassDomainType<?> jpaType = mappedSuperclassByMappedSuperclassMapping.get( safeMapping );

					applyIdMetadata( safeMapping, jpaType );
					applyVersionAttribute( safeMapping, jpaType );

					Iterator<Property> properties = safeMapping.getDeclaredPropertyIterator();
					while ( properties.hasNext() ) {
						final Property property = properties.next();
						if ( safeMapping.isVersioned() && property == safeMapping.getVersion() ) {
							// skip the version property, it was already handled previously.
							continue;
						}
						final PersistentAttribute attribute = attributeFactory.buildAttribute( jpaType, property );
						if ( attribute != null ) {
							( (AttributeContainer) jpaType ).getInFlightAccess().addAttribute( attribute );
						}
					}

					( (AttributeContainer) jpaType ).getInFlightAccess().finishUp();

					if ( staticMetamodelScanEnabled ) {
						populateStaticMetamodel( jpaType );
					}
				}
				finally {
					if ( LOG.isTraceEnabled() ) {
						LOG.trace( "Completed mapped superclass [" + safeMapping.getMappedClass().getName() + ']' );
					}
				}
			}
			else {
				throw new AssertionFailure( "Unexpected mapping type: " + mapping.getClass() );
			}
		}


		while ( ! embeddablesToProcess.isEmpty() ) {
			final ArrayList<EmbeddableDomainType<?>> processingEmbeddables = new ArrayList<>( embeddablesToProcess.size() );
			for ( List<EmbeddableDomainType<?>> embeddableDomainTypes : embeddablesToProcess.values() ) {
				processingEmbeddables.addAll( embeddableDomainTypes );
			}

			embeddablesToProcess.clear();

			for ( EmbeddableDomainType<?> embeddable : processingEmbeddables ) {
				final Component component = componentByEmbeddable.get( embeddable );
				final Iterator<Property> propertyItr = component.getPropertyIterator();
				while ( propertyItr.hasNext() ) {
					final Property property = propertyItr.next();
					final PersistentAttribute attribute = attributeFactory.buildAttribute( embeddable, property );
					if ( attribute != null ) {
						( ( AttributeContainer) embeddable ).getInFlightAccess().addAttribute( attribute );
					}
				}

				( ( AttributeContainer) embeddable ).getInFlightAccess().finishUp();
				embeddables.put( embeddable.getJavaType(), embeddable );

				if ( staticMetamodelScanEnabled ) {
					populateStaticMetamodel( embeddable );
				}
			}
		}
	}


	// 1) create the part
	// 2) register the part (mapping role)
	// 3) somehow get the mapping role "into" the part (setter, ?)

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void applyIdMetadata(PersistentClass persistentClass, IdentifiableDomainType<?> identifiableType) {
		if ( persistentClass.hasIdentifierProperty() ) {
			final Property declaredIdentifierProperty = persistentClass.getDeclaredIdentifierProperty();
			if ( declaredIdentifierProperty != null ) {
				final SingularPersistentAttribute<?, Object> idAttribute = attributeFactory.buildIdAttribute(
						identifiableType,
						declaredIdentifierProperty
				);

				( ( AttributeContainer) identifiableType ).getInFlightAccess().applyIdAttribute( idAttribute );
			}
		}
		else {
			// we have a non-aggregated composite-id

			//noinspection RedundantClassCall
			if ( ! Component.class.isInstance( persistentClass.getIdentifier() ) ) {
				throw new MappingException( "Expecting Component for id mapping with no id-attribute" );
			}

			// Handle the actual id-attributes
			final Component cidValue = (Component) persistentClass.getIdentifier();
			final Iterator<Property> cidPropertyItr;
			final int propertySpan;
			final EmbeddableTypeImpl<?> idClassType;
			final Component identifierMapper = persistentClass.getIdentifierMapper();
			if ( identifierMapper != null ) {
				cidPropertyItr = identifierMapper.getPropertyIterator();
				propertySpan = identifierMapper.getPropertySpan();
				idClassType = applyIdClassMetadata( (Component) persistentClass.getIdentifier() );
			}
			else {
				cidPropertyItr = cidValue.getPropertyIterator();
				propertySpan = cidValue.getPropertySpan();
				idClassType = null;
			}

			assert cidValue.isEmbedded();

			AbstractIdentifiableType idType = (AbstractIdentifiableType) entityTypesByEntityName.get( cidValue.getOwner().getEntityName() );
			Set idAttributes = idType.getIdClassAttributesSafely();
			if ( idAttributes == null ) {
				idAttributes = new HashSet<>( propertySpan );
				while ( cidPropertyItr.hasNext() ) {
					final Property cidSubProperty = cidPropertyItr.next();
					final SingularPersistentAttribute<?, Object> cidSubAttr = attributeFactory.buildIdAttribute(
							idType,
							cidSubProperty
					);

					idAttributes.add( cidSubAttr );
				}
			}


			( ( AttributeContainer) identifiableType ).getInFlightAccess().applyNonAggregatedIdAttributes( idAttributes, idClassType );
		}
	}

	private EmbeddableTypeImpl<?> applyIdClassMetadata(Component idClassComponent) {
		final JavaTypeRegistry registry = getTypeConfiguration()
				.getJavaTypeDescriptorRegistry();
		final Class<?> componentClass = idClassComponent.getComponentClass();
		final JavaType<?> javaTypeDescriptor = registry.resolveManagedTypeDescriptor( componentClass );

		final EmbeddableTypeImpl<?> embeddableType = new EmbeddableTypeImpl<>(
				javaTypeDescriptor,
				false,
				getJpaMetamodel()
		);
		registerEmbeddableType( embeddableType, idClassComponent );
		return embeddableType;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private <X> void applyIdMetadata(MappedSuperclass mappingType, MappedSuperclassDomainType<X> jpaMappingType) {
		if ( mappingType.hasIdentifierProperty() ) {
			final Property declaredIdentifierProperty = mappingType.getDeclaredIdentifierProperty();
			if ( declaredIdentifierProperty != null ) {
				final SingularPersistentAttribute<X, Object> attribute = attributeFactory.buildIdAttribute( jpaMappingType, declaredIdentifierProperty );
				//noinspection unchecked
				( ( AttributeContainer) jpaMappingType ).getInFlightAccess().applyIdAttribute( attribute );
			}
		}
		//a MappedSuperclass can have no identifier if the id is set below in the hierarchy
		else if ( mappingType.getIdentifierMapper() != null ) {
			Set<SingularPersistentAttribute<? super X, ?>> attributes = buildIdClassAttributes(
					jpaMappingType,
					mappingType.getIdentifierMapper().getPropertyIterator()
			);
			//noinspection unchecked
			( ( AttributeContainer<X>) jpaMappingType ).getInFlightAccess().applyIdClassAttributes( attributes );
		}
	}

	private <X> void applyVersionAttribute(PersistentClass persistentClass, EntityDomainType<X> jpaEntityType) {
		final Property declaredVersion = persistentClass.getDeclaredVersion();
		if ( declaredVersion != null ) {
			//noinspection unchecked
			( ( AttributeContainer<X>) jpaEntityType ).getInFlightAccess().applyVersionAttribute(
					attributeFactory.buildVersionAttribute( jpaEntityType, declaredVersion )
			);
		}
	}

	private <X> void applyVersionAttribute(MappedSuperclass mappingType, MappedSuperclassDomainType<X> jpaMappingType) {
		final Property declaredVersion = mappingType.getDeclaredVersion();
		if ( declaredVersion != null ) {
			//noinspection unchecked
			( ( AttributeContainer<X>) jpaMappingType ).getInFlightAccess().applyVersionAttribute(
					attributeFactory.buildVersionAttribute( jpaMappingType, declaredVersion )
			);
		}
	}

	private <X> Set<SingularPersistentAttribute<? super X, ?>> buildIdClassAttributes(
			IdentifiableDomainType<X> ownerType,
			Iterator<Property> propertyIterator) {
		if ( LOG.isTraceEnabled() ) {
			LOG.trace( "Building old-school composite identifier [" + ownerType.getJavaType().getName() + ']' );
		}
		Set<SingularPersistentAttribute<? super X, ?>> attributes = new HashSet<>();
		while ( propertyIterator.hasNext() ) {
			attributes.add( attributeFactory.buildIdAttribute( ownerType, propertyIterator.next() ) );
		}
		return attributes;
	}

	private <X> void populateStaticMetamodel(ManagedDomainType<X> managedType) {
		final Class<X> managedTypeClass = managedType.getJavaType();
		if ( managedTypeClass == null ) {
			// should indicate MAP entity mode, skip...
			return;
		}
		final String metamodelClassName = managedTypeClass.getName() + '_';
		try {
			final Class metamodelClass = Class.forName( metamodelClassName, true, managedTypeClass.getClassLoader() );
			// we found the class; so populate it...
			registerAttributes( metamodelClass, managedType );
		}
		catch (ClassNotFoundException ignore) {
			// nothing to do...
		}

		// todo : this does not account for @MappedSuperclass, mainly because this is not being tracked in our
		// internal metamodel as populated from the annotations properly
		ManagedDomainType<? super X> superType = managedType.getSuperType();
		if ( superType != null ) {
			populateStaticMetamodel( superType );
		}
	}

	private final Set<Class> processedMetamodelClasses = new HashSet<>();

	private <X> void registerAttributes(Class metamodelClass, ManagedDomainType<X> managedType) {
		if ( !processedMetamodelClasses.add( metamodelClass ) ) {
			return;
		}

		// push the attributes on to the metamodel class...
		for ( Attribute<X, ?> attribute : managedType.getDeclaredAttributes() ) {
			registerAttribute( metamodelClass, attribute );
		}

		if ( managedType instanceof IdentifiableType ) {
			final AbstractIdentifiableType<X> entityType = (AbstractIdentifiableType<X>) managedType;

			// handle version
			if ( entityType.hasDeclaredVersionAttribute() ) {
				registerAttribute( metamodelClass, entityType.getDeclaredVersion() );
			}

			// handle id-class mappings specially
			if ( entityType.hasIdClass() ) {
				final Set<SingularPersistentAttribute<? super X, ?>> attributes = entityType.getIdClassAttributesSafely();
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
			// As a result, in the case of embeddable classes we simply use getField rather than
			// getDeclaredField
			final boolean allowNonDeclaredFieldReference =
					attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.EMBEDDED
							|| attribute.getDeclaringType().getPersistenceType() == Type.PersistenceType.EMBEDDABLE;

			final Field field = allowNonDeclaredFieldReference
					? metamodelClass.getField( name )
					: metamodelClass.getDeclaredField( name );
			try {
				// should be public anyway, but to be sure...
				ReflectHelper.ensureAccessibility( field );
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
				// mismatch in how the annotation processor interpreted the attribute and how our metamodel
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

	public MappedSuperclassDomainType<?> locateMappedSuperclassType(MappedSuperclass mappedSuperclass) {
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
		return new HashSet<>( knownMappedSuperclasses );
	}

	private final Map<Class<?>,BasicDomainType<?>> basicDomainTypeMap = new HashMap<>();

	public <J> BasicDomainType<J> resolveBasicType(Class<J> javaType) {
		//noinspection unchecked
		return (BasicDomainType) basicDomainTypeMap.computeIfAbsent(
				javaType,
				jt -> {
					final JavaTypeRegistry registry =
							getTypeConfiguration()
							.getJavaTypeDescriptorRegistry();
					return new BasicTypeImpl<>( registry.resolveDescriptor( javaType ) );
				}
		);
	}

	public <J> EmbeddableDomainType<J> locateEmbeddable(Class<J> embeddableClass, Component component) {
		//noinspection unchecked
		EmbeddableDomainType<J> domainType = (EmbeddableDomainType<J>) embeddables.get( embeddableClass );
		if ( domainType == null ) {
			final List<EmbeddableDomainType<?>> embeddableDomainTypes = embeddablesToProcess.get( embeddableClass );
			if ( embeddableDomainTypes != null ) {
				for ( EmbeddableDomainType<?> embeddableDomainType : embeddableDomainTypes ) {
					final Component cachedComponent = componentByEmbeddable.get( embeddableDomainType );
					if ( cachedComponent.isSame( component ) ) {
						//noinspection unchecked
						domainType = (EmbeddableDomainType<J>) embeddableDomainType;
						break;
					}
					else if ( cachedComponent.getComponentClass().equals( component.getComponentClass() ) ) {
						final int cachedComponentPropertySpan = cachedComponent.getPropertySpan();
						if ( cachedComponentPropertySpan != component.getPropertySpan() ) {
							throw new MappingException(
									"Encountered multiple component mappings for the same java class "
											+ embeddableClass.getName() +
											" with different property mappings. Every property mapping combination should have its own java class" );
						}
						else {
							for ( int i = 0; i < cachedComponentPropertySpan; i++ ) {
								if ( !cachedComponent.getProperty( i ).getName()
										.equals( component.getProperty( i ).getName() ) ) {
									throw new MappingException(
											"Encountered multiple component mappings for the same java class "
													+ embeddableClass.getName() +
													" with different property mappings. Every property mapping combination should have its own java class" );
								}
							}
						}
						//noinspection unchecked
						domainType = (EmbeddableDomainType<J>) embeddableDomainType;
						break;
					}
					else {
						throw new MappingException( "Encountered multiple component mappings for the same java class "
															+ embeddableClass.getName() +
															" with different property mappings. Every property mapping combination should have its own java class" );
					}
				}
			}
		}
		return domainType;
	}
}
