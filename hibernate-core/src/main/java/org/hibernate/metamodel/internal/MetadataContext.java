/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.Type;
import org.hibernate.AssertionFailure;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.model.domain.internal.AbstractIdentifiableType;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
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
import org.hibernate.metamodel.model.domain.internal.PrimitiveBasicTypeImpl;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.EntityJavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static java.util.Collections.unmodifiableMap;
import static org.hibernate.internal.util.collections.CollectionHelper.mapOfSize;
import static org.hibernate.metamodel.internal.InjectionHelper.injectField;

/**
 * Defines a context for storing information during the building of the {@link MappingMetamodelImpl}.
 * <p>
 * This contextual information includes data needing to be processed in a second pass as well as
 * cross-references into the built metamodel classes.
 * <p>
 * At the end of the day, clients are interested in the {@link #getEntityTypeMap} and {@link #getEmbeddableTypeSet}
 * results, which represent all the registered {@linkplain #registerEntityType entities} and
 * {@linkplain #registerEmbeddableType embeddables} respectively.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
@Internal
public class MetadataContext {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( MetadataContext.class );

	private final JpaMetamodelImplementor jpaMetamodel;
	private final RuntimeModelCreationContext runtimeModelCreationContext;

	private final Set<MappedSuperclass> knownMappedSuperclasses;
	private final TypeConfiguration typeConfiguration;
	private final JpaStaticMetamodelPopulationSetting jpaStaticMetaModelPopulationSetting;
	private final JpaMetamodelPopulationSetting jpaMetaModelPopulationSetting;
	private final AttributeFactory attributeFactory = new AttributeFactory( this );

	private final Map<Class<?>, EntityDomainType<?>> entityTypes = new HashMap<>();
	private final Map<String, IdentifiableDomainType<?>> identifiableTypesByName = new HashMap<>();
	private final Map<PersistentClass, EntityDomainType<?>> entityTypesByPersistentClass = new HashMap<>();

	private final Map<Class<?>, EmbeddableDomainType<?>> embeddables = new HashMap<>();
	private final Map<Class<?>, List<EmbeddableDomainType<?>>> embeddablesToProcess = new HashMap<>();
	private final Map<EmbeddableDomainType<?>, Component> componentByEmbeddable = new HashMap<>();

	private final Map<MappedSuperclass, MappedSuperclassDomainType<?>> mappedSuperclassByMappedSuperclassMapping = new HashMap<>();
	private final Map<MappedSuperclassDomainType<?>, PersistentClass> mappedSuperClassTypeToPersistentClass = new HashMap<>();

	//this list contains MappedSuperclass and EntityTypes ordered by superclass first
	private final List<Object> orderedMappings = new ArrayList<>();

	/**
	 * Stack of PersistentClass being processed. Last in the list is the highest in the stack.
	 */
	private final List<PersistentClass> stackOfPersistentClassesBeingProcessed = new ArrayList<>();
	private final MappingMetamodel metamodel;
	private final ClassLoaderService classLoaderService;

	public MetadataContext(
			JpaMetamodelImplementor jpaMetamodel,
			MappingMetamodel mappingMetamodel,
			MetadataImplementor bootMetamodel,
			JpaStaticMetamodelPopulationSetting jpaStaticMetaModelPopulationSetting,
			JpaMetamodelPopulationSetting jpaMetaModelPopulationSetting,
			RuntimeModelCreationContext runtimeModelCreationContext,
			ClassLoaderService classLoaderService) {
		this.jpaMetamodel = jpaMetamodel;
		this.classLoaderService = classLoaderService;
		this.metamodel = mappingMetamodel;
		this.knownMappedSuperclasses = bootMetamodel.getMappedSuperclassMappingsCopy();
		this.typeConfiguration = runtimeModelCreationContext.getTypeConfiguration();
		this.jpaStaticMetaModelPopulationSetting = jpaStaticMetaModelPopulationSetting;
		this.jpaMetaModelPopulationSetting = jpaMetaModelPopulationSetting;
		this.runtimeModelCreationContext = runtimeModelCreationContext;
	}

	public RuntimeModelCreationContext getRuntimeModelCreationContext() {
		return runtimeModelCreationContext;
	}

	public JpaMetamodelImplementor getJpaMetamodel() {
		return jpaMetamodel;
	}

	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	public JavaTypeRegistry getJavaTypeRegistry(){
		return typeConfiguration.getJavaTypeRegistry();
	}

	MappingMetamodel getMetamodel() {
		return metamodel;
	}

	private <T> PersistentAttribute<T, ?> buildAttribute(ManagedDomainType<T> domainType, Property property) {
		return attributeFactory.buildAttribute( domainType, property );
	}

	private <T> SingularPersistentAttribute<T, ?> buildIdAttribute(IdentifiableDomainType<T> type, Property property) {
		return attributeFactory.buildIdAttribute( type, property );
	}

	private <T> SingularPersistentAttribute<T, ?> buildVersionAttribute(IdentifiableDomainType<T> type, Property property) {
		return attributeFactory.buildVersionAttribute( type, property );
	}

	/**
	 * Retrieves the {@linkplain Class java type} to {@link EntityTypeImpl} map.
	 *
	 * @return The {@linkplain Class java type} to {@link EntityTypeImpl} map.
	 */
	public Map<Class<?>, EntityDomainType<?>> getEntityTypeMap() {
		return unmodifiableMap( entityTypes );
	}

	public Set<EmbeddableDomainType<?>> getEmbeddableTypeSet() {
		return componentByEmbeddable.keySet();
	}

	public Map<Class<?>, MappedSuperclassDomainType<?>> getMappedSuperclassTypeMap() {
		// we need to actually build this map...
		final Map<Class<?>, MappedSuperclassDomainType<?>> mappedSuperClassTypeMap =
				mapOfSize( mappedSuperclassByMappedSuperclassMapping.size() );
		for ( var mappedSuperclassType : mappedSuperclassByMappedSuperclassMapping.values() ) {
			mappedSuperClassTypeMap.put( mappedSuperclassType.getJavaType(), mappedSuperclassType );
		}
		return mappedSuperClassTypeMap;
	}

	public  void registerEntityType(PersistentClass persistentClass, EntityTypeImpl<?> entityType) {
		final Class<?> javaType = entityType.getJavaType();
		if ( javaType != null && javaType != Map.class ) {
			entityTypes.put( javaType, entityType );
		}

		identifiableTypesByName.put( persistentClass.getEntityName(), entityType );
		entityTypesByPersistentClass.put( persistentClass, entityType );
		orderedMappings.add( persistentClass );
	}

	public void registerEmbeddableType(
			EmbeddableDomainType<?> embeddableType,
			Component bootDescriptor) {
		final var  javaType = embeddableType.getJavaType();
		assert javaType != null;
		assert !Map.class.isAssignableFrom( javaType );
		embeddablesToProcess.computeIfAbsent( javaType, k -> new ArrayList<>( 1 ) )
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
		identifiableTypesByName.put( mappedSuperclassType.getTypeName(), mappedSuperclassType );
		mappedSuperclassByMappedSuperclassMapping.put( mappedSuperclass, mappedSuperclassType );
		orderedMappings.add( mappedSuperclass );
		if ( !stackOfPersistentClassesBeingProcessed.isEmpty() ) {
			mappedSuperClassTypeToPersistentClass.put( mappedSuperclassType, getEntityWorkedOn() );
		}

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
	public IdentifiableDomainType<?> locateIdentifiableType(String entityName) {
		return identifiableTypesByName.get( entityName );
	}

	public Map<String, IdentifiableDomainType<?>> getIdentifiableTypesByName() {
		return unmodifiableMap( identifiableTypesByName );
	}

	private <X> PersistentAttribute<X, ?> buildAttribute(
			Property property,
			IdentifiableDomainType<X> entityType,
			BiFunction<IdentifiableDomainType<X>, Property, PersistentAttribute<X, ?>> factoryFunction) {
		if ( property.getValue() instanceof Component component && component.isGeneric() ) {
			// This is an embeddable property that uses generics, we have to retrieve the generic
			// component previously registered and create the concrete attribute
			final Component genericComponent =
					runtimeModelCreationContext.getMetadata()
							.getGenericComponent( component.getComponentClass() );
			final Property genericProperty = property.copy();
			genericProperty.setValue( genericComponent );
			genericProperty.setGeneric( true );
			final var attribute = factoryFunction.apply( entityType, genericProperty );
			if ( !property.isGeneric() ) {
				final var concreteAttribute = factoryFunction.apply( entityType, property );
				if ( concreteAttribute != null ) {
					final var managedType = (ManagedDomainType<X>) entityType;
					final var attributeContainer = (AttributeContainer<X>) managedType;
					attributeContainer.getInFlightAccess().addConcreteGenericAttribute( concreteAttribute );
				}
			}
			return attribute;
		}
		else {
			return factoryFunction.apply( entityType, property );
		}
	}

	public void wrapUp() {
		if ( log.isTraceEnabled() ) {
			log.trace( "Wrapping up metadata context..." );
		}

		final boolean staticMetamodelScanEnabled =
				jpaStaticMetaModelPopulationSetting != JpaStaticMetamodelPopulationSetting.DISABLED;
		final Set<String> processedMetamodelClasses = new HashSet<>();

		//we need to process types from superclasses to subclasses
		for ( Object mapping : orderedMappings ) {
			if ( mapping instanceof PersistentClass persistentClass ) {
				if ( log.isTraceEnabled() ) {
					log.trace( "Starting entity [" + persistentClass.getEntityName() + ']' );
				}
				try {
					final var jpaMapping = entityTypesByPersistentClass.get( persistentClass );

					applyIdMetadata( persistentClass, jpaMapping );
					applyVersionAttribute( persistentClass, jpaMapping );
					applyGenericProperties( persistentClass, jpaMapping );

					for ( Property property : persistentClass.getDeclaredProperties() ) {
						if ( property.getValue() == persistentClass.getIdentifierMapper() ) {
							// property represents special handling for id-class mappings but we have already
							// accounted for the embedded property mappings in #applyIdMetadata &&
							// #buildIdClassAttributes
							continue;
						}
						if ( persistentClass.isVersioned() && property == persistentClass.getVersion() ) {
							// skip the version property, it was already handled previously.
							continue;
						}
						buildAttribute( property, jpaMapping );
					}

					( (AttributeContainer<?>) jpaMapping ).getInFlightAccess().finishUp();

					if ( staticMetamodelScanEnabled ) {
						populateStaticMetamodel( jpaMapping, processedMetamodelClasses );
					}
				}
				finally {
					if ( log.isTraceEnabled() ) {
						log.trace( "Completed entity [" + persistentClass.getEntityName() + ']' );
					}
				}
			}
			else if ( mapping instanceof MappedSuperclass mappedSuperclass ) {
				if ( log.isTraceEnabled() ) {
					log.trace( "Starting mapped superclass [" + mappedSuperclass.getMappedClass().getName() + ']' );
				}
				try {
					final var jpaType = mappedSuperclassByMappedSuperclassMapping.get( mappedSuperclass );

					applyIdMetadata( mappedSuperclass, jpaType );
					applyVersionAttribute( mappedSuperclass, jpaType );
//					applyNaturalIdAttribute( safeMapping, jpaType );

					for ( Property property : mappedSuperclass.getDeclaredProperties() ) {
						if ( isIdentifierProperty( property, mappedSuperclass ) ) {
							// property represents special handling for id-class mappings but we have already
							// accounted for the embedded property mappings in #applyIdMetadata &&
							// #buildIdClassAttributes
							continue;
						}
						else if ( mappedSuperclass.isVersioned() && property == mappedSuperclass.getVersion() ) {
							// skip the version property, it was already handled previously.
							continue;
						}
						buildAttribute( property, jpaType );
					}

					( (AttributeContainer<?>) jpaType ).getInFlightAccess().finishUp();

					if ( staticMetamodelScanEnabled ) {
						populateStaticMetamodel( jpaType, processedMetamodelClasses );
					}
				}
				finally {
					if ( log.isTraceEnabled() ) {
						log.trace( "Completed mapped superclass [" + mappedSuperclass.getMappedClass().getName() + ']' );
					}
				}
			}
			else {
				throw new AssertionFailure( "Unexpected mapping type: " + mapping.getClass() );
			}
		}


		while ( ! embeddablesToProcess.isEmpty() ) {
			final List<EmbeddableDomainType<?>> processingEmbeddables =
					new ArrayList<>( embeddablesToProcess.size() );
			for ( var embeddableDomainTypes : embeddablesToProcess.values() ) {
				processingEmbeddables.addAll( embeddableDomainTypes );
			}

			embeddablesToProcess.clear();

			for ( var embeddable : processingEmbeddables ) {
				final Component component = componentByEmbeddable.get( embeddable );
				for ( Property property : component.getProperties() ) {
					if ( !component.isPolymorphic()
							|| embeddable.getTypeName().equals( component.getPropertyDeclaringClass( property ) ) ) {
						addAttribute( embeddable, property, component );
					}
				}

				( ( AttributeContainer<?>) embeddable ).getInFlightAccess().finishUp();
				// Do not process embeddables for entity types i.e. id-classes or
				// generic component embeddables used just for concrete type resolution
				if ( !component.isGeneric()
						&& !( embeddable.getExpressibleJavaType() instanceof EntityJavaType<?> ) ) {
					embeddables.put( embeddable.getJavaType(), embeddable );
					if ( staticMetamodelScanEnabled ) {
						populateStaticMetamodel( embeddable, processedMetamodelClasses );
					}
				}
			}
		}
	}

	private static boolean isIdentifierProperty(Property property, MappedSuperclass mappedSuperclass) {
		final Component identifierMapper = mappedSuperclass.getIdentifierMapper();
		return identifierMapper != null
			&& ArrayHelper.contains( identifierMapper.getPropertyNames(), property.getName() );
	}

	private <T> void addAttribute(EmbeddableDomainType<T> embeddable, Property property, Component component) {
		final var attribute = buildAttribute( embeddable, property);
		if ( attribute != null ) {
			final Property superclassProperty =
					getMappedSuperclassProperty( property.getName(),
							component.getMappedSuperclass() );
			if ( superclassProperty != null && superclassProperty.isGeneric() ) {
				final var managedType = (ManagedDomainType<T>) embeddable;
				final var attributeContainer = (AttributeContainer<T>) managedType;
				attributeContainer.getInFlightAccess().addConcreteGenericAttribute( attribute );
			}
			else {
				addAttribute( embeddable, attribute );
			}
		}
	}

	private <T> void buildAttribute(Property property, IdentifiableDomainType<T> jpaType) {
		final var attribute = buildAttribute( property, jpaType, this::buildAttribute );
		if ( attribute != null ) {
			addAttribute( jpaType, attribute );
			if ( property.isNaturalIdentifier() ) {
				final var managedType = (ManagedDomainType<T>) jpaType;
				final var attributeContainer = (AttributeContainer<T>) managedType;
				attributeContainer.getInFlightAccess().applyNaturalIdAttribute( attribute );
			}
		}
	}

	private <T> void addAttribute(ManagedDomainType<T> type, PersistentAttribute<T, ?> attribute) {
		final var container = (AttributeContainer<T>) type;
		final var inFlightAccess = container.getInFlightAccess();
		final boolean virtual =
				attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.EMBEDDED
						&& attribute.getAttributeJavaType() instanceof EntityJavaType<?>;
		if ( virtual ) {
			@SuppressWarnings("unchecked")
			final var embeddableDomainType = (EmbeddableDomainType<T>) attribute.getValueGraphType();
			final Component component = componentByEmbeddable.get( embeddableDomainType );
			for ( Property property : component.getProperties() ) {
				final var subAttribute = buildAttribute( embeddableDomainType, property );
				if ( subAttribute != null ) {
					inFlightAccess.addAttribute( subAttribute );
				}
			}
			if ( jpaMetaModelPopulationSetting != JpaMetamodelPopulationSetting.ENABLED ) {
				return;
			}
		}
		inFlightAccess.addAttribute( attribute );
	}

	// 1) create the part
	// 2) register the part (mapping role)
	// 3) somehow get the mapping role "into" the part (setter, ?)

	private <T> void applyIdMetadata(PersistentClass persistentClass, IdentifiableDomainType<T> identifiableType) {
		if ( persistentClass.hasIdentifierProperty() ) {
			final var managedType = (ManagedDomainType<T>) identifiableType;
			final var attributeContainer = (AttributeContainer<T>) managedType;
			final Property declaredIdentifierProperty = persistentClass.getDeclaredIdentifierProperty();
			if ( declaredIdentifierProperty != null ) {
				final var idAttribute =
						(SingularPersistentAttribute<T, ?>)
								buildAttribute( declaredIdentifierProperty, identifiableType,
										this::buildIdAttribute );
				attributeContainer.getInFlightAccess().applyIdAttribute( idAttribute );
			}
			else {
				final Property superclassIdentifier = getMappedSuperclassIdentifier( persistentClass );
				if ( superclassIdentifier != null && superclassIdentifier.isGeneric() ) {
					// If the superclass identifier is generic, we have to build the attribute to register the concrete type
					final var concreteIdentifier =
							buildIdAttribute( identifiableType, persistentClass.getIdentifierProperty() );
					attributeContainer.getInFlightAccess().addConcreteGenericAttribute( concreteIdentifier );
				}
			}
		}
		else {
			// we have a non-aggregated composite-id
			if ( !( persistentClass.getIdentifier() instanceof Component compositeId ) ) {
				throw new MappingException( "Expecting Component for id mapping with no id-attribute" );
			}

			applyIdAttributes( persistentClass, identifiableType, compositeId );
		}
	}

	private <T> void applyIdAttributes(
			PersistentClass persistentClass,
			IdentifiableDomainType<T> identifiableType,
			Component compositeId) {
		// Handle the actual id attributes
		final List<Property> cidProperties;
		final int propertySpan;
		final EmbeddableTypeImpl<?> idClassType;
		final Component identifierMapper = persistentClass.getIdentifierMapper();
		if ( identifierMapper != null ) {
			cidProperties = identifierMapper.getProperties();
			propertySpan = identifierMapper.getPropertySpan();
			idClassType =
					identifierMapper.getComponentClassName() == null
							? null  // support for no id-class, especially for dynamic models
							: applyIdClassMetadata( (Component) persistentClass.getIdentifier() );
		}
		else {
			cidProperties = compositeId.getProperties();
			propertySpan = compositeId.getPropertySpan();
			idClassType = null;
		}

		assert compositeId.isEmbedded();

		final var idDomainType = identifiableTypesByName.get( compositeId.getOwner().getEntityName() );
		@SuppressWarnings("unchecked")
		final var idType = (AbstractIdentifiableType<T>) idDomainType;
		applyIdAttributes( identifiableType, idType, propertySpan, cidProperties, idClassType );
	}

	private <T> void applyIdAttributes(
			IdentifiableDomainType<T> identifiableType,
			AbstractIdentifiableType<T> idType,
			int propertySpan,
			List<Property> cidProperties,
			EmbeddableTypeImpl<?> idClassType) {
		final var idAttributes = idClassAttributes( idType, propertySpan, cidProperties );
		final var managedType = (ManagedDomainType<T>) identifiableType;
		final var container = (AttributeContainer<T>) managedType;
		container.getInFlightAccess().applyNonAggregatedIdAttributes( idAttributes, idClassType);
	}

	private <T> Set<SingularPersistentAttribute<? super T, ?>> idClassAttributes(
			AbstractIdentifiableType<T> idType,
			int propertySpan,
			List<Property> cidProperties) {
		final var idAttributes = idType.getIdClassAttributesSafely();
		if ( idAttributes != null ) {
			return idAttributes;
		}
		else {
			final Set<SingularPersistentAttribute<? super T, ?>> result = new HashSet<>( propertySpan );
			for ( Property cidSubproperty : cidProperties ) {
				result.add( buildIdAttribute( idType, cidSubproperty ) );
			}
			return result;
		}
	}

	private Property getMappedSuperclassIdentifier(PersistentClass persistentClass) {
		MappedSuperclass mappedSuperclass = getMappedSuperclass( persistentClass );
		while ( mappedSuperclass != null ) {
			final Property declaredIdentifierProperty = mappedSuperclass.getDeclaredIdentifierProperty();
			if ( declaredIdentifierProperty != null ) {
				return declaredIdentifierProperty;
			}
			mappedSuperclass = getMappedSuperclass( mappedSuperclass );
		}
		return null;
	}

	private <Y> EmbeddableTypeImpl<Y> applyIdClassMetadata(Component idClassComponent) {
		final var embeddableType =
				new EmbeddableTypeImpl<Y>(
						getJavaTypeRegistry().resolveManagedTypeDescriptor( idClassComponent.getComponentClass() ),
						getMappedSuperclassDomainType( idClassComponent ),
						null,
						false,
						getJpaMetamodel()
				);
		registerEmbeddableType( embeddableType, idClassComponent );
		return embeddableType;
	}

	@SuppressWarnings("unchecked")
	private <Y> MappedSuperclassDomainType<? super Y> getMappedSuperclassDomainType(Component idClassComponent) {
		final MappedSuperclass mappedSuperclass = idClassComponent.getMappedSuperclass();
		return mappedSuperclass == null ? null
				: (MappedSuperclassDomainType<? super Y>)
						locateMappedSuperclassType( mappedSuperclass );
	}

	private <X> void applyIdMetadata(MappedSuperclass mappingType, MappedSuperclassDomainType<X> jpaMappingType) {
		final var managedType = (ManagedDomainType<X>) jpaMappingType;
		final var attributeContainer = (AttributeContainer<X>) managedType;
		if ( mappingType.hasIdentifierProperty() ) {
			final Property declaredIdentifierProperty = mappingType.getDeclaredIdentifierProperty();
			if ( declaredIdentifierProperty != null ) {
				final var attribute =
						(SingularPersistentAttribute<X, ?>)
								buildAttribute( declaredIdentifierProperty, jpaMappingType,
										this::buildIdAttribute );
				attributeContainer.getInFlightAccess().applyIdAttribute( attribute );
			}
		}
		//a MappedSuperclass can have no identifier if the id is set below in the hierarchy
		else if ( mappingType.getIdentifierMapper() != null ) {
			final var attributes =
					buildIdClassAttributes( jpaMappingType,
							mappingType.getIdentifierMapper().getProperties() );
			attributeContainer.getInFlightAccess().applyIdClassAttributes( attributes );
		}
	}

	private <X> void applyVersionAttribute(PersistentClass persistentClass, EntityDomainType<X> jpaEntityType) {
		final Property declaredVersion = persistentClass.getDeclaredVersion();
		if ( declaredVersion != null ) {
			final var managedType = (ManagedDomainType<X>) jpaEntityType;
			final var attributeContainer = (AttributeContainer<X>) managedType;
			attributeContainer.getInFlightAccess()
					.applyVersionAttribute( buildVersionAttribute( jpaEntityType, declaredVersion ) );
		}
	}

	private <X> void applyVersionAttribute(MappedSuperclass mappingType, MappedSuperclassDomainType<X> jpaMappingType) {
		final Property declaredVersion = mappingType.getDeclaredVersion();
		if ( declaredVersion != null ) {
			final var managedType = (ManagedDomainType<X>) jpaMappingType;
			final var attributeContainer = (AttributeContainer<X>) managedType;
			attributeContainer.getInFlightAccess()
					.applyVersionAttribute( buildVersionAttribute( jpaMappingType, declaredVersion ) );
		}
	}

	private <X> void applyGenericProperties(PersistentClass persistentClass, EntityDomainType<X> entityType) {
		var mappedSuperclass = getMappedSuperclass( persistentClass );
		while ( mappedSuperclass != null ) {
			for ( Property superclassProperty : mappedSuperclass.getDeclaredProperties() ) {
				if ( superclassProperty.isGeneric() ) {
					final Property property = persistentClass.getProperty( superclassProperty.getName() );
					final var managedType = (ManagedDomainType<X>) entityType;
					final var attributeContainer = (AttributeContainer<X>) managedType;
					attributeContainer.getInFlightAccess()
							.addConcreteGenericAttribute( buildAttribute( entityType, property ) );
				}
			}
			mappedSuperclass = getMappedSuperclass( mappedSuperclass );
		}
	}

	private MappedSuperclass getMappedSuperclass(PersistentClass persistentClass) {
		while ( persistentClass != null ) {
			final var mappedSuperclass = persistentClass.getSuperMappedSuperclass();
			if ( mappedSuperclass != null ) {
				return mappedSuperclass;
			}
			persistentClass = persistentClass.getSuperclass();
		}
		return null;
	}

	private MappedSuperclass getMappedSuperclass(MappedSuperclass mappedSuperclass) {
		final var superMappedSuperclass = mappedSuperclass.getSuperMappedSuperclass();
		return superMappedSuperclass == null
				? getMappedSuperclass( mappedSuperclass.getSuperPersistentClass() )
				: superMappedSuperclass;
	}

	private Property getMappedSuperclassProperty(String propertyName, MappedSuperclass mappedSuperclass) {
		if ( mappedSuperclass == null ) {
			return null;
		}

		for ( Property property : mappedSuperclass.getDeclaredProperties() ) {
			if ( property.getName().equals( propertyName ) ) {
				return property;
			}
		}

		final Property property =
				getMappedSuperclassProperty( propertyName,
						mappedSuperclass.getSuperMappedSuperclass() );
		if ( property != null ) {
			return property;
		}
		else {
			final var superclass = mappedSuperclass.getSuperPersistentClass();
			return superclass != null ? superclass.getProperty( propertyName ) : null;
		}
	}

	private <X> Set<SingularPersistentAttribute<? super X, ?>> buildIdClassAttributes(
			IdentifiableDomainType<X> ownerType,
			List<Property> properties) {
		if ( log.isTraceEnabled() ) {
			log.trace( "Building old-school composite identifier [" + ownerType.getJavaType().getName() + ']' );
		}
		final Set<SingularPersistentAttribute<? super X, ?>> attributes = new HashSet<>();
		for ( Property property : properties ) {
			attributes.add( buildIdAttribute( ownerType, property ) );
		}
		return attributes;
	}

	private <X> void populateStaticMetamodel(ManagedDomainType<X> managedType, Set<String> processedMetamodelClassName) {
		final Class<X> managedTypeClass = managedType.getJavaType();
		if ( managedTypeClass != null // can be null for MAP entity mode, so skip...
				&& processedMetamodelClassName.add( metamodelClassName( managedType ) ) ) {
			final Class<?> metamodelClass = metamodelClass( managedType );
			if ( metamodelClass != null ) {
				registerAttributes( metamodelClass, managedType );
				injectManagedType( managedType, metamodelClass );
			}
			// todo : this does not account for @MappedSuperclass, mainly
			//        because this is not being tracked in our internal
			//        metamodel as populated from the annotations properly
			final var superType = managedType.getSuperType();
			if ( superType != null ) {
				populateStaticMetamodel( superType, processedMetamodelClassName );
			}
		}
	}

	private static <X> void injectManagedType(ManagedDomainType<X> managedType, Class<?> metamodelClass) {
		try {
			injectField( metamodelClass, "class_", managedType, false );
		}
		catch ( NoSuchFieldException e ) {
			// ignore
		}
	}

	private static String metamodelClassName(ManagedDomainType<?> managedTypeClass) {
		return metamodelClassName( managedTypeClass.getJavaType() );
	}

	private static String metamodelClassName(Class<?> javaType) {
		return javaType.isMemberClass()
				? metamodelClassName( javaType.getEnclosingClass() ) + "$" + javaType.getSimpleName() + "_"
				: javaType.getName() + '_';
	}

	public Class<?> metamodelClass(ManagedDomainType<?> managedDomainType) {
		if ( managedDomainType == null ) {
			return null;
		}
		else {
			final String metamodelClassName = metamodelClassName( managedDomainType );
			try {
				return classLoaderService.classForName( metamodelClassName );
			}
			catch ( ClassLoadingException ignore ) {
				return null;
			}
		}
	}

	private <X> void registerAttributes(Class<?> metamodelClass, ManagedDomainType<X> managedType) {
		// push the attributes on to the metamodel class...
		for ( var attribute : managedType.getDeclaredAttributes() ) {
			registerAttribute( metamodelClass, attribute );
		}

		if ( managedType instanceof AbstractIdentifiableType<X> entityType ) {
			// handle version
			if ( entityType.hasDeclaredVersionAttribute() ) {
				registerAttribute( metamodelClass, entityType.getDeclaredVersion() );
			}

			// handle id-class mappings specially
			if ( entityType.hasIdClass() ) {
				final var attributes = entityType.getIdClassAttributesSafely();
				if ( attributes != null ) {
					for ( var attribute : attributes ) {
						registerAttribute( metamodelClass, attribute );
					}
				}
			}
		}
	}

	private <X> void registerAttribute(Class<?> metamodelClass, Attribute<X, ?> attribute) {
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
			injectField( metamodelClass, name, attribute, allowNonDeclaredFieldReference );
		}
		catch (NoSuchFieldException e) {
			log.unableToLocateStaticMetamodelField( metamodelClass.getName(), name );
//			throw new AssertionFailure(
//					"Unable to locate static metamodel field: " + metamodelClass.getName() + '#' + name
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
			throw new AssertionFailure( "Inconsistent popping: "
					+ persistentClass.getEntityName() + " instead of " + stackTop.getEntityName() );
		}
	}

	private PersistentClass getEntityWorkedOn() {
		return stackOfPersistentClassesBeingProcessed.get(
				stackOfPersistentClassesBeingProcessed.size() - 1
		);
	}

	PersistentClass getPersistentClassHostingProperties(MappedSuperclassTypeImpl<?> mappedSuperclassType) {
		return mappedSuperClassTypeToPersistentClass.get( mappedSuperclassType );
	}

	public Set<MappedSuperclass> getUnusedMappedSuperclasses() {
		return new HashSet<>( knownMappedSuperclasses );
	}

	private final Map<Class<?>,BasicDomainType<?>> basicDomainTypeMap = new HashMap<>();

	public <J> BasicDomainType<J> resolveBasicType(Class<J> javaType) {
		@SuppressWarnings("unchecked")
		final var domainType = (BasicDomainType<J>) basicDomainTypeMap.get( javaType );
		if ( domainType == null ) {
			// we cannot use getTypeConfiguration().standardBasicTypeForJavaType(javaType)
			// because that doesn't return the right thing for primitive types
			basicDomainTypeMap.put( javaType, basicDomainType( javaType ) );
			return basicDomainType( javaType );
		}
		else {
			return domainType;
		}
	}

	private <J> BasicDomainType<J> basicDomainType(Class<J> javaType) {
		final JavaType<J> javaTypeDescriptor = getJavaTypeRegistry().resolveDescriptor( javaType );
		final JdbcType jdbcType =
				javaTypeDescriptor.getRecommendedJdbcType( typeConfiguration.getCurrentBaseSqlTypeIndicators() );
		return javaType.isPrimitive()
				? new PrimitiveBasicTypeImpl<>( javaTypeDescriptor, jdbcType, javaType )
				: new BasicTypeImpl<>( javaTypeDescriptor, jdbcType );
	}

	public EmbeddableDomainType<?> locateEmbeddable(Class<?> embeddableClass, Component component) {
		final var domainType = embeddables.get( embeddableClass );
		if ( domainType != null ) {
			return domainType;
		}
		else {
			final var embeddableDomainTypes = embeddablesToProcess.get( embeddableClass );
			if ( embeddableDomainTypes != null ) {
				for ( var embeddableDomainType : embeddableDomainTypes ) {
					final Component cachedComponent = componentByEmbeddable.get( embeddableDomainType );
					if ( cachedComponent.isSame( component ) ) {
						return embeddableDomainType;
					}
					else if ( cachedComponent.getComponentClass().equals( component.getComponentClass() ) ) {
						final int cachedComponentPropertySpan = cachedComponent.getPropertySpan();
						if ( cachedComponentPropertySpan != component.getPropertySpan() ) {
							throw new MappingException( "Encountered multiple component mappings for the same java class "
											+ embeddableClass.getName() +
											" with different property mappings. Every property mapping combination should have its own java class" );
						}
						else {
							for ( int i = 0; i < cachedComponentPropertySpan; i++ ) {
								if ( !cachedComponent.getProperty( i ).getName()
										.equals( component.getProperty( i ).getName() ) ) {
									throw new MappingException( "Encountered multiple component mappings for the same java class "
													+ embeddableClass.getName() +
													" with different property mappings. Every property mapping combination should have its own java class" );
								}
							}
						}
						return embeddableDomainType;
					}
					else {
						throw new MappingException( "Encountered multiple component mappings for the same java class "
										+ embeddableClass.getName() +
										" with different property mappings. Every property mapping combination should have its own java class" );
					}
				}
			}
			return null;
		}
	}
}
