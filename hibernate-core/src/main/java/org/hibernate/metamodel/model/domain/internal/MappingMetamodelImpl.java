/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.UnknownEntityTypeException;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.type.BindingContext;
import org.hibernate.query.sqm.tuple.TupleType;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;
import org.hibernate.metamodel.spi.EntityRepresentationStrategy;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.spi.PersisterFactory;
import org.hibernate.type.BindableType;
import org.hibernate.query.spi.QueryParameterBindingTypeResolver;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleSimpleSqmPathSource;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleSqmPathSource;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmFieldLiteral;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.type.BasicType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;

import static org.hibernate.internal.util.collections.ArrayHelper.toStringArray;
import static org.hibernate.metamodel.internal.JpaMetamodelPopulationSetting.determineJpaMetaModelPopulationSetting;
import static org.hibernate.metamodel.internal.JpaStaticMetamodelPopulationSetting.determineJpaStaticMetaModelPopulationSetting;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * Implementation of the JPA-defined contract {@link jakarta.persistence.metamodel.Metamodel}.
 * <p>
 * Really more of the {@linkplain MappingMetamodel mapping model} than the domain model, though
 * it does have reference to the {@link org.hibernate.metamodel.model.domain.JpaMetamodel}.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 * @author Andrea Boriero
 */
public class MappingMetamodelImpl
		implements MappingMetamodelImplementor, JpaMetamodel, Metamodel, QueryParameterBindingTypeResolver, BindingContext, Serializable {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JpaMetamodel

	private final JpaMetamodelImpl jpaMetamodel;

	private final Map<Class<?>, String> entityProxyInterfaceMap = new ConcurrentHashMap<>();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// RuntimeModel

	private final EntityPersisterConcurrentMap entityPersisterMap = new EntityPersisterConcurrentMap();
	private final Map<String, CollectionPersister> collectionPersisterMap = new ConcurrentHashMap<>();
	private final Map<String, Set<String>> collectionRolesByEntityParticipant = new ConcurrentHashMap<>();

	private final Map<NavigableRole, EmbeddableValuedModelPart> embeddableValuedModelPart = new ConcurrentHashMap<>();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// DomainMetamodel

	private final Set<EntityNameResolver> entityNameResolvers = new HashSet<>();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// NOTE: Relational/mapping information is not part of the JPA metamodel
	// (type system). However, this relational/mapping info *is* part of the
	// Hibernate metamodel. This is a mismatch. Normally this is not a problem
	// - ignoring Hibernate's representation mode (entity mode), the Class
	// object for an entity (or mapped superclass) always refers to the same
	// JPA EntityType and Hibernate EntityPersister. The problem arises with
	// embeddables. For an embeddable, as with the rest of its metamodel,
	// Hibernate combines the embeddable's relational/mapping while JPA does
	// not. This is perfectly consistent with each paradigm. But it results
	// in a mismatch since JPA expects a single "type descriptor" for a
	// given embeddable class while Hibernate incorporates the
	// relational/mapping info so we have a "type descriptor" for each usage
	// of that embeddable type. (Think embeddable versus embedded.)
	//
	// To account for this, we track both paradigms here.

	// There can be multiple instances of an Embeddable type, each one being relative to its parent entity.

	private final Map<TupleType<?>, MappingModelExpressible<?>> tupleTypeCache = new ConcurrentHashMap<>();

	public MappingMetamodelImpl(TypeConfiguration typeConfiguration, ServiceRegistry serviceRegistry) {
		jpaMetamodel = new JpaMetamodelImpl( typeConfiguration, this, serviceRegistry );
	}

	public JpaMetamodelImplementor getJpaMetamodel() {
		return jpaMetamodel;
	}

	public void finishInitialization(RuntimeModelCreationContext context) {
		final var bootModel = context.getBootModel();
		bootModel.visitRegisteredComponents( Component::prepareForMappingModel );
		bootModel.getMappedSuperclassMappingsCopy()
				.forEach( MappedSuperclass::prepareForMappingModel );
		bootModel.getEntityBindings()
				.forEach( persistentClass -> persistentClass.prepareForMappingModel( context ) );

		final var persisterFactory =
				jpaMetamodel.getServiceRegistry()
						.requireService( PersisterFactory.class );
		final var cache = context.getCache();
		processBootEntities(
				bootModel.getEntityBindings(),
				cache,
				persisterFactory,
				context
		);
		processBootCollections(
				bootModel.getCollectionBindings(),
				cache,
				persisterFactory,
				context
		);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// after *all* persisters and named queries are registered

		MappingModelCreationProcess.process( entityPersisterMap, collectionPersisterMap, context );

		for ( var persister : entityPersisterMap.values() ) {
			persister.postInstantiate();
			registerEntityNameResolvers( persister, entityNameResolvers );
		}

		for ( var persister : entityPersisterMap.values() ) {
			persister.prepareLoaders();
		}

		collectionPersisterMap.values().forEach( CollectionPersister::postInstantiate );

		registerEmbeddableMappingType( bootModel );

		final var settings = context.getSettings();
		jpaMetamodel.processJpa(
				bootModel,
				this,
				entityProxyInterfaceMap,
				determineJpaStaticMetaModelPopulationSetting( settings ),
				determineJpaMetaModelPopulationSetting( settings ),
				bootModel.getNamedEntityGraphs().values(),
				context
		);
	}

	private void registerEmbeddableMappingType(MetadataImplementor bootModel) {
		bootModel.visitRegisteredComponents(
				composite -> {
					final var compositeType = (ComponentType) composite.getType();
					final var mappingModelPart = compositeType.getMappingModelPart();
					embeddableValuedModelPart.put( mappingModelPart.getNavigableRole(), mappingModelPart );
				}
		);
	}

	private void processBootEntities(
			java.util.Collection<PersistentClass> entityBindings,
			CacheImplementor cache,
			PersisterFactory persisterFactory,
			RuntimeModelCreationContext modelCreationContext) {
		for ( final var model : entityBindings ) {
			final var rootEntityRole = new NavigableRole( model.getRootClass().getEntityName() );
			final var entityPersister =
					persisterFactory.createEntityPersister(
							model,
							cache.getEntityRegionAccess( rootEntityRole ),
							cache.getNaturalIdCacheRegionAccessStrategy( rootEntityRole ),
							modelCreationContext
					);
			final String entityName = model.getEntityName();
			entityPersisterMap.put( entityName, entityPersister );
			// Also register the persister under the class name if available,
			// otherwise the getEntityDescriptor(Class) won't work for entities with custom entity names
			final String className = model.getClassName();
			if ( className != null && !className.equals( entityName ) ) {
				// But only if the class name is not registered already,
				// as we can have the same class mapped to multiple entity names
				entityPersisterMap.putIfAbsent( className, entityPersister );
			}

			final var concreteProxyClass = entityPersister.getConcreteProxyClass();
			final var mappedClass = entityPersister.getMappedClass();
			if ( concreteProxyClass != null
					&& concreteProxyClass.isInterface()
					// we exclude Map-based proxy interfaces here because that should indicate MAP entity mode
					&& !Map.class.isAssignableFrom( concreteProxyClass )
					&& mappedClass != concreteProxyClass ) {
				final String existing =
						entityProxyInterfaceMap.put( concreteProxyClass,
								entityPersister.getEntityName() );
				if ( existing != null ) {
					throw new HibernateException(
							String.format(
									Locale.ENGLISH,
									"Multiple entities [%s, %s] named the same interface [%s] as their proxy which is not supported",
									existing,
									entityPersister.getEntityName(),
									concreteProxyClass.getName()
							)
					);
				}
			}
		}
	}

	private void processBootCollections(
			java.util.Collection<Collection> collectionBindings,
			CacheImplementor cacheImplementor,
			PersisterFactory persisterFactory,
			RuntimeModelCreationContext modelCreationContext) {
		for ( final var model : collectionBindings ) {
			final String role = model.getRole();
			final var persister =
					persisterFactory.createCollectionPersister(
							model,
							cacheImplementor.getCollectionRegionAccess( new NavigableRole( role ) ),
							modelCreationContext
					);
			collectionPersisterMap.put( role, persister );
			if ( persister.getIndexType() instanceof org.hibernate.type.EntityType entityType ) {
				registerEntityParticipant( entityType, persister );
			}
			if ( persister.getElementType() instanceof org.hibernate.type.EntityType entityType ) {
				registerEntityParticipant( entityType, persister );
			}
		}
	}

	private void registerEntityParticipant(org.hibernate.type.EntityType entityType, CollectionPersister persister) {
		final String entityName = entityType.getAssociatedEntityName();
		collectionRolesByEntityParticipant.computeIfAbsent( entityName, k -> new HashSet<>() )
				.add( persister.getRole() );
	}

	private static void registerEntityNameResolvers(
			EntityPersister persister,
			Set<EntityNameResolver> entityNameResolvers) {
		final var representationStrategy = persister.getRepresentationStrategy();
		if ( representationStrategy != null ) {
			registerEntityNameResolvers( representationStrategy, entityNameResolvers );
		}
	}

	private static void registerEntityNameResolvers(
			EntityRepresentationStrategy representationStrategy,
			Set<EntityNameResolver> entityNameResolvers) {
		representationStrategy.visitEntityNameResolvers( entityNameResolvers::add );
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return jpaMetamodel.getTypeConfiguration();
	}

	@Override
	public MappingMetamodel getMappingMetamodel() {
		return this;
	}

	public ServiceRegistry getServiceRegistry() {
		return jpaMetamodel.getServiceRegistry();
	}

	@Override
	public void forEachEntityDescriptor(Consumer<EntityPersister> action) {
		for ( var value : entityPersisterMap.values() ) {
			action.accept( value );
		}
	}

	@Override @Deprecated(forRemoval=true) @SuppressWarnings( "removal" )
	public Stream<EntityPersister> streamEntityDescriptors() {
		return Arrays.stream( entityPersisterMap.values() );
	}

	@Override
	public EntityPersister getEntityDescriptor(String entityName) {
		final var entityPersister = entityPersisterMap.get( entityName );
		if ( entityPersister == null ) {
			throw new UnknownEntityTypeException( entityName );
		}
		return entityPersister;
	}

	@Override
	public EntityPersister getEntityDescriptor(NavigableRole name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public EmbeddableValuedModelPart getEmbeddableValuedModelPart(NavigableRole role){
		final var embeddableMappingType = embeddableValuedModelPart.get( role );
		if ( embeddableMappingType == null ) {
			throw new IllegalArgumentException( "Unable to locate EmbeddableValuedModelPart: " + role );
		}
		return embeddableMappingType;
	}

	@Override
	public EntityPersister findEntityDescriptor(String entityName) {
		return entityPersisterMap.get( entityName );
	}

	@Override
	public EntityPersister findEntityDescriptor(Class<?> entityJavaType) {
		return findEntityDescriptor( entityJavaType.getName() );
	}

	@Override
	public boolean isEntityClass(Class<?> entityJavaType) {
		return entityPersisterMap.containsKey( entityJavaType.getName() );
	}

	@Override
	public EntityPersister getEntityDescriptor(Class<?> entityJavaType) {
		var entityPersister = entityPersisterMap.get( entityJavaType.getName() );
		if ( entityPersister == null ) {
			final String mappedEntityName = entityProxyInterfaceMap.get( entityJavaType );
			if ( mappedEntityName != null ) {
				entityPersister = entityPersisterMap.get( mappedEntityName );
			}
		}
		if ( entityPersister == null ) {
			throw new UnknownEntityTypeException( entityJavaType );
		}
		return entityPersister;
	}

	@Override @Deprecated(forRemoval = true) @SuppressWarnings( "removal" )
	public EntityPersister locateEntityDescriptor(Class<?> byClass) {
		var entityPersister = entityPersisterMap.get( byClass.getName() );
		if ( entityPersister == null ) {
			final String mappedEntityName = entityProxyInterfaceMap.get( byClass );
			if ( mappedEntityName != null ) {
				entityPersister = entityPersisterMap.get( mappedEntityName );
			}
		}
		if ( entityPersister == null ) {
			throw new UnknownEntityTypeException( byClass );
		}
		return entityPersister;
	}

	@Override
	public <X> EntityDomainType<X> entity(Class<X> cls) {
		return jpaMetamodel.entity( cls );
	}

	@Override
	public <X> ManagedDomainType<X> managedType(Class<X> cls) {
		return jpaMetamodel.managedType( cls );
	}

	@Override
	public <X> EmbeddableDomainType<X> embeddable(Class<X> cls) {
		return jpaMetamodel.embeddable( cls );
	}

	@Override
	public Set<ManagedType<?>> getManagedTypes() {
		return jpaMetamodel.getManagedTypes();
	}

	@Override
	public Set<EntityType<?>> getEntities() {
		return jpaMetamodel.getEntities();
	}

	@Override
	public Set<EmbeddableType<?>> getEmbeddables() {
		return jpaMetamodel.getEmbeddables();
	}

	@Override
	public @Nullable <X> ManagedDomainType<X> findManagedType(@Nullable String typeName) {
		return jpaMetamodel.findManagedType( typeName );
	}

	@Override
	public <X> ManagedDomainType<X> managedType(String typeName) {
		return jpaMetamodel.managedType( typeName );
	}

	@Override
	public @Nullable EntityDomainType<?> findEntityType(@Nullable String entityName) {
		return jpaMetamodel.findEntityType( entityName );
	}

	@Override
	public EntityDomainType<?> entity(String entityName) {
		return jpaMetamodel.entity( entityName );
	}

	@Override
	public @Nullable EmbeddableDomainType<?> findEmbeddableType(@Nullable String embeddableName) {
		return jpaMetamodel.findEmbeddableType( embeddableName );
	}

	@Override
	public EmbeddableDomainType<?> embeddable(String embeddableName) {
		return jpaMetamodel.embeddable( embeddableName );
	}

	@Override
	public <X> EntityDomainType<X> getHqlEntityReference(String entityName) {
		return jpaMetamodel.getHqlEntityReference( entityName );
	}

	@Override
	public <X> EntityDomainType<X> resolveHqlEntityReference(String entityName) {
		return jpaMetamodel.resolveHqlEntityReference( entityName );
	}

	@Override
	public <X> ManagedDomainType<X> findManagedType(Class<X> cls) {
		return jpaMetamodel.findManagedType( cls );
	}

	@Override
	public <X> EntityDomainType<X> findEntityType(Class<X> cls) {
		return jpaMetamodel.findEntityType( cls );
	}

	@Override
	public @Nullable <X> EmbeddableDomainType<X> findEmbeddableType(Class<X> cls) {
		return jpaMetamodel.findEmbeddableType( cls );
	}

	@Override
	public String qualifyImportableName(String queryName) {
		return jpaMetamodel.qualifyImportableName( queryName );
	}

	@Override
	public Set<String> getEnumTypesForValue(String enumValue) {
		return jpaMetamodel.getEnumTypesForValue(enumValue);
	}

	@Override
	public JavaType<?> getJavaConstantType(String className, String fieldName) {
		return jpaMetamodel.getJavaConstantType( className, fieldName );
	}

	@Override
	public <T> T getJavaConstant(String className, String fieldName) {
		return jpaMetamodel.getJavaConstant( className, fieldName );
	}

	@Override
	public EnumJavaType<?> getEnumType(String className) {
		return jpaMetamodel.getEnumType(className);
	}

	@Override
	public <E extends Enum<E>> E enumValue(EnumJavaType<E> enumType, String enumValueName) {
		return jpaMetamodel.enumValue( enumType, enumValueName );
	}

	@Override
	public String getImportedName(String name) {
		final String qualifiedName = jpaMetamodel.qualifyImportableName( name );
		return qualifiedName == null ? name : qualifiedName;
	}

	@Override
	public void forEachCollectionDescriptor(Consumer<CollectionPersister> action) {
		collectionPersisterMap.values().forEach( action );
	}

	@Override @Deprecated(forRemoval=true) @SuppressWarnings( "removal" )
	public Stream<CollectionPersister> streamCollectionDescriptors() {
		return collectionPersisterMap.values().stream();
	}

	@Override
	public CollectionPersister getCollectionDescriptor(String role) {
		final var collectionPersister = collectionPersisterMap.get( role );
		if ( collectionPersister == null ) {
			throw new IllegalArgumentException( "Unable to locate persister: " + role );
		}
		return collectionPersister;
	}

	@Override
	public CollectionPersister getCollectionDescriptor(NavigableRole role) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CollectionPersister findCollectionDescriptor(NavigableRole role) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CollectionPersister findCollectionDescriptor(String role) {
		return collectionPersisterMap.get( role );
	}

	@Override
	public MappingModelExpressible<?> resolveMappingExpressible(
			SqmExpressible<?> sqmExpressible,
			Function<NavigablePath, TableGroup> tableGroupLocator) {
		if ( sqmExpressible instanceof SqmPath<?> sqmPath ) {
			if ( sqmPath.getResolvedModel().getPathType()
					instanceof MappingModelExpressible<?> mappingExpressible ) {
				return mappingExpressible;
			}
			else {
				final var navigablePath = sqmPath.getNavigablePath();
				final var parent = navigablePath.getParent();
				final var parentTableGroupModelPart = tableGroupLocator.apply( parent ).getModelPart();
				return parent == null
						? parentTableGroupModelPart
						: parentTableGroupModelPart.findSubPart( navigablePath.getLocalName(), null );
			}
		}

		else if ( sqmExpressible instanceof BasicType<?> basicType ) {
			return basicType;
		}

		else if ( sqmExpressible instanceof BasicDomainType<?> ) {
			return getTypeConfiguration()
					.getBasicTypeForJavaType( sqmExpressible.getRelationalJavaType().getJavaTypeClass() );
		}

		else if ( sqmExpressible instanceof BasicSqmPathSource<?>
				|| sqmExpressible instanceof AnonymousTupleSimpleSqmPathSource<?> ) {
			return resolveMappingExpressible( sqmExpressible.getSqmType(), tableGroupLocator );
		}

		else if ( sqmExpressible instanceof SqmFieldLiteral<?> sqmFieldLiteral ) {
			return getTypeConfiguration().getBasicTypeForJavaType( sqmFieldLiteral.getJavaType() );
		}

		else if ( sqmExpressible instanceof CompositeSqmPathSource ) {
			throw new UnsupportedOperationException( "Resolution of embedded-valued SqmExpressible nodes not yet implemented" );
		}

		else if ( sqmExpressible instanceof AnonymousTupleSqmPathSource<?> anonymousTupleSqmPathSource ) {
			return resolveMappingExpressible(
					resolveExpressible( anonymousTupleSqmPathSource.getPathType() ),
					tableGroupLocator
			);
		}

		else if ( sqmExpressible instanceof EntityDomainType<?> entityDomainType ) {
			return getEntityDescriptor( entityDomainType.getHibernateEntityName() );
		}

		else if ( sqmExpressible instanceof TupleType<?> tupleType ) {
			final var mappingModelExpressible = tupleTypeCache.get( sqmExpressible );
			if ( mappingModelExpressible != null ) {
				return mappingModelExpressible;
			}
			else {
				final int componentCount = tupleType.componentCount();
				final var components = new MappingModelExpressible<?>[componentCount];
				for ( int i = 0; i < componentCount; i++ ) {
					components[i] = resolveMappingExpressible( tupleType.get( i ), tableGroupLocator );
				}
				final var tupleMappingModelExpressible =
						new TupleMappingModelExpressible( components );
				final var existingMappingModelExpressible =
						tupleTypeCache.putIfAbsent( tupleType, tupleMappingModelExpressible );
				return existingMappingModelExpressible == null
						? tupleMappingModelExpressible
						: existingMappingModelExpressible;
			}
		}
		else {
			return null;
		}
	}

	@Override
	public Set<String> getCollectionRolesByEntityParticipant(String entityName) {
		return collectionRolesByEntityParticipant.get( entityName );
	}

	@Override
	public java.util.Collection<EntityNameResolver> getEntityNameResolvers() {
		return entityNameResolvers;
	}

	@Override
	public String[] getAllEntityNames() {
		return entityPersisterMap.keys();
	}

	@Override
	public String[] getAllCollectionRoles() {
		return toStringArray( collectionPersisterMap.keySet() );
	}

	@Override
	public <T> BindableType<T> resolveParameterBindType(Class<T> javaType) {
		final var typeConfiguration = getTypeConfiguration();

		final BasicType<T> basicType = typeConfiguration.getBasicTypeForJavaType( javaType );
		// For enums, we simply don't know the exact mapping if there is no basic type registered
		if ( basicType != null || javaType.isEnum() ) {
			return basicType;
		}

		final ManagedDomainType<T> managedType = jpaMetamodel.findManagedType( javaType );
		if ( managedType != null ) {
			return (BindableType<T>) managedType;
		}

		final var javaTypeRegistry = typeConfiguration.getJavaTypeRegistry();
		final JavaType<T> javaTypeDescriptor = javaTypeRegistry.findDescriptor( javaType );
		if ( javaTypeDescriptor != null ) {
			final JdbcType recommendedJdbcType =
					javaTypeDescriptor.getRecommendedJdbcType( typeConfiguration.getCurrentBaseSqlTypeIndicators() );
			if ( recommendedJdbcType != null ) {
				return typeConfiguration.getBasicTypeRegistry().resolve( javaTypeDescriptor, recommendedJdbcType );
			}
		}

		if ( javaType.isArray()
				&& javaTypeRegistry.findDescriptor( javaType.getComponentType() ) != null ) {
			final var resolvedJavaType = javaTypeRegistry.resolveDescriptor( javaType );
			final var recommendedJdbcType =
					resolvedJavaType.getRecommendedJdbcType( typeConfiguration.getCurrentBaseSqlTypeIndicators() );
			if ( recommendedJdbcType != null ) {
				return typeConfiguration.getBasicTypeRegistry().resolve( resolvedJavaType, recommendedJdbcType );
			}
		}

		return null;
	}

	@Override
	public <T> BindableType<? super T> resolveParameterBindType(T bindValue) {
		if ( bindValue == null ) {
			// we can't guess
			return null;
		}

		final Class<T> clazz = unproxiedClass( bindValue );

		// Resolve superclass bindable type if necessary, as we don't register types for e.g. Inet4Address
		Class<? super T> c = clazz;
		do {
			final BindableType<? super T> type = resolveParameterBindType( c );
			if ( type != null ) {
				return type;
			}
			c = c.getSuperclass();
		}
		while ( c != Object.class );

		if ( clazz.isEnum() ) {
			return null; //createEnumType( (Class) clazz );
		}
		else if ( Serializable.class.isAssignableFrom( clazz ) ) {
			final var parameterBindType = resolveParameterBindType( Serializable.class );
			//noinspection unchecked
			return (BindableType<? super T>) parameterBindType;
		}
		else {
			return null;
		}
	}

	private static <T> Class<T> unproxiedClass(T bindValue) {
		final var lazyInitializer = extractLazyInitializer( bindValue );
		final var result =
				lazyInitializer != null
						? lazyInitializer.getPersistentClass()
						: bindValue.getClass();
		//noinspection unchecked
		return (Class<T>) result;
	}
}
