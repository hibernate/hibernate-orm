/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.serial.internal;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.NamedEntityGraphDefinition;
import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.pipeline.internal.FunctionRegistryCoordinator;
import org.hibernate.boot.pipeline.internal.MappingCustomizations;
import org.hibernate.boot.pipeline.internal.MappingResolutionOptionsImpl;
import org.hibernate.boot.query.NamedHqlQueryDefinition;
import org.hibernate.boot.query.NamedNativeQueryDefinition;
import org.hibernate.boot.query.NamedProcedureCallDefinition;
import org.hibernate.boot.query.NamedResultSetMappingDescriptor;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.mapping.DiscriminatorType;
import org.hibernate.metamodel.mapping.EmbeddableDiscriminatorConverter;
import org.hibernate.metamodel.mapping.internal.DiscriminatorTypeImpl;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.internal.util.StringHelper.qualify;
import static org.hibernate.metamodel.mapping.EntityDiscriminatorMapping.DISCRIMINATOR_ROLE_NAME;

/// Declarative serialized state of the completed metadata graph.
///
/// This archive representation deliberately excludes the bootstrap context and
/// service-backed mapping options. Those are recreated only during explicit
/// restoration.
///
/// @since 9.0
/// @author Steve Ebersole
public final class MetadataState implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	private final UUID uuid;
	private final Map<String, PersistentClass> entityBindingMap;
	private final List<Component> composites;
	private final Map<Class<?>, Component> genericComponentsMap;
	private final Map<Class<?>, MappedSuperclass> mappedSuperclassMap;
	private final Map<String, Collection> collectionBindingMap;
	private final Map<String, TypeDefinitionRestorationRecipe> typeDefinitionRecipes;
	private final Map<String, FilterDefinitionRestorationRecipe> filterDefinitionRecipes;
	private final Map<String, FetchProfile> fetchProfileMap;
	private final Map<String, String> imports;
	private final Map<String, IdentifierGeneratorDefinition> idGeneratorDefinitionMap;
	private final Map<String, NamedHqlQueryDefinition<?>> namedQueryMap;
	private final Map<String, NamedNativeQueryDefinition<?>> namedNativeQueryMap;
	private final Map<String, NamedProcedureCallDefinition> namedProcedureCallMap;
	private final Map<String, NamedResultSetMappingDescriptor> sqlResultSetMappingMap;
	private final Map<String, NamedEntityGraphDefinition> namedEntityGraphMap;
	private final Database database;

	private MetadataState(MetadataImpl metadata) {
		uuid = metadata.getUUID();
		entityBindingMap = metadata.getEntityBindingMap();
		composites = metadata.getComposites();
		genericComponentsMap = metadata.getGenericComponentsMap();
		mappedSuperclassMap = metadata.getMappedSuperclassMap();
		collectionBindingMap = metadata.getCollectionBindingMap();
		typeDefinitionRecipes = new HashMap<>();
		final Map<TypeDefinition, TypeDefinitionRestorationRecipe> recipesByDefinition = new IdentityHashMap<>();
		metadata.getTypeDefinitionMap().forEach(
				(key, definition) -> typeDefinitionRecipes.put(
						key,
						recipesByDefinition.computeIfAbsent(
								definition,
								TypeDefinitionRestorationRecipe::from
						)
				)
		);
		filterDefinitionRecipes = new HashMap<>();
		metadata.getFilterDefinitions().forEach(
				(key, definition) -> filterDefinitionRecipes.put(
						key,
						FilterDefinitionRestorationRecipe.from( definition )
				)
		);
		fetchProfileMap = metadata.getFetchProfileMap();
		imports = metadata.getImports();
		idGeneratorDefinitionMap = metadata.getIdGeneratorDefinitionMap();
		namedQueryMap = metadata.getNamedQueryMap();
		namedNativeQueryMap = metadata.getNamedNativeQueryMap();
		namedProcedureCallMap = metadata.getNamedProcedureCallMap();
		sqlResultSetMappingMap = metadata.getSqlResultSetMappingMap();
		namedEntityGraphMap = metadata.getNamedEntityGraphMap();
		database = metadata.getDatabase();
	}

	public static MetadataState from(MetadataImpl metadata) {
		return new MetadataState( metadata );
	}

	MetadataImpl restore(StandardServiceRegistry serviceRegistry, ModelsContext modelsContext) {
		final var restoredOptions = new MappingResolutionOptionsImpl( serviceRegistry );
		final var restoredContext = new BootstrapContextImpl(
				serviceRegistry,
				restoredOptions,
				new TypeConfiguration(),
				modelsContext
		);
		restoredOptions.setBootstrapContext( restoredContext );
		final TypeConfiguration restoredTypeConfiguration = restoredContext.getTypeConfiguration();
		final Map<String, TypeDefinition> restoredTypeDefinitionMap = restoreTypeDefinitions(
				restoredContext.getClassLoaderService()::classForTypeName
		);
		final var restorationBuildingContext = new RestoredMetadataBuildingContext(
				restoredContext,
				restoredOptions,
				restoredTypeDefinitionMap.values()
		);
		restoredContext.getTypeConfiguration().scope( restorationBuildingContext );
		final Map<String, FilterDefinition> restoredFilterDefinitionMap = new HashMap<>();
		filterDefinitionRecipes.forEach(
				(key, recipe) -> restoredFilterDefinitionMap.put(
						key,
						recipe.resolve( restorationBuildingContext )
				)
		);
		entityBindingMap.values().forEach(
				binding -> binding.reattachClassLoaderAccess( restoredContext.getClassLoaderAccess() )
		);
		collectionBindingMap.values().forEach(
				binding -> binding.reattachRuntimeServices(
						restoredContext.getClassLoaderAccess(),
						restoredContext.getManagedBeanRegistry(),
						restoredOptions.isAllowExtensionsInCdi()
				)
		);
		database.reattach( restoredOptions );
		reattachMappingValues(
				restoredContext.getTypeConfiguration(),
				restoredContext.getClassLoaderAccess(),
				restoredContext.getManagedBeanRegistry(),
				modelsContext,
				restoredOptions.isAllowExtensionsInCdi()
		);
		restoredFilterDefinitionMap.values().forEach( definition -> definition.reattachParameterResolvers(
				restoredContext.getClassLoaderAccess(),
				restoredContext.getManagedBeanRegistry()
		) );
		final SqmFunctionRegistry restoredFunctionRegistry = FunctionRegistryCoordinator.create();
		FunctionRegistryCoordinator.populate(
				restoredFunctionRegistry,
				MappingCustomizations.NONE,
				serviceRegistry,
				restoredContext.getTypeConfiguration()
		);
		namedEntityGraphMap.values().forEach(
				definition -> definition.graphCreator().reattachModelsContext( modelsContext )
		);
		final Map<Class<?>, DiscriminatorType<?>> restoredEmbeddableDiscriminatorTypes =
				restoreEmbeddableDiscriminatorTypes( restoredTypeConfiguration, modelsContext );
		return new MetadataImpl(
				uuid, restoredOptions, entityBindingMap, composites, genericComponentsMap,
				restoredEmbeddableDiscriminatorTypes, mappedSuperclassMap, collectionBindingMap,
				restoredTypeDefinitionMap, restoredFilterDefinitionMap, fetchProfileMap, imports,
				idGeneratorDefinitionMap, namedQueryMap, namedNativeQueryMap,
				namedProcedureCallMap, sqlResultSetMappingMap, namedEntityGraphMap,
				restoredFunctionRegistry, Map.of(), database, restoredContext
		);
	}

	private Map<String, TypeDefinition> restoreTypeDefinitions(
			Function<String, Class<?>> classResolver) {
		final Map<String, TypeDefinition> restoredDefinitions = new HashMap<>();
		final Map<TypeDefinitionRestorationRecipe, TypeDefinition> definitionsByRecipe = new IdentityHashMap<>();
		typeDefinitionRecipes.forEach(
				(key, recipe) -> restoredDefinitions.put(
						key,
						definitionsByRecipe.computeIfAbsent( recipe, ignored -> recipe.resolve( classResolver ) )
				)
		);
		return restoredDefinitions;
	}

	private Map<Class<?>, DiscriminatorType<?>> restoreEmbeddableDiscriminatorTypes(
			TypeConfiguration typeConfiguration,
			ModelsContext modelsContext) {
		final Map<Class<?>, DiscriminatorType<?>> restoredTypes = new HashMap<>();
		final var domainJavaType = typeConfiguration.getJavaTypeRegistry().resolveDescriptor( Class.class );
		final var relationalType = typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING );
		for ( Component component : composites ) {
			if ( !component.isPolymorphic() ) {
				continue;
			}
			final var discriminatorType = restoredTypes.computeIfAbsent(
					component.getComponentClass(),
					embeddableClass -> new DiscriminatorTypeImpl<>(
							relationalType,
							EmbeddableDiscriminatorConverter.fromValueMappings(
									qualify( component.getComponentClassName(), DISCRIMINATOR_ROLE_NAME ),
									domainJavaType,
									relationalType,
									component.getDiscriminatorValues(),
									className -> modelsContext.getClassDetailsRegistry()
											.resolveClassDetails( className )
											.toJavaClass()
							)
					)
			);
			component.reattachDiscriminatorType( discriminatorType );
		}
		return restoredTypes;
	}

	private void reattachMappingValues(
			TypeConfiguration typeConfiguration,
			ClassLoaderAccess classLoaderAccess,
			ManagedBeanRegistry managedBeanRegistry,
			ModelsContext modelsContext,
			boolean allowExtensionsInCdi) {
		final Set<Value> visited = java.util.Collections.newSetFromMap( new IdentityHashMap<>() );
		for ( PersistentClass entityBinding : entityBindingMap.values() ) {
			reattachValue( entityBinding.getIdentifier(), typeConfiguration, modelsContext, visited );
			reattachValue( entityBinding.getDiscriminator(), typeConfiguration, modelsContext, visited );
			reattachValue( entityBinding.getIdentifierMapper(), typeConfiguration, modelsContext, visited );
			reattachProperties( entityBinding.getProperties(), typeConfiguration, modelsContext, visited );
			entityBinding.getJoins().forEach( join -> {
				reattachValue( join.getKey(), typeConfiguration, modelsContext, visited );
				reattachProperties( join.getProperties(), typeConfiguration, modelsContext, visited );
			} );
		}
		if ( mappedSuperclassMap != null ) {
			for ( MappedSuperclass mappedSuperclass : mappedSuperclassMap.values() ) {
				reattachProperties( mappedSuperclass.getDeclaredProperties(), typeConfiguration, modelsContext, visited );
				reattachValue( mappedSuperclass.getIdentifierMapper(), typeConfiguration, modelsContext, visited );
			}
		}
		for ( Collection collection : collectionBindingMap.values() ) {
			reattachValue( collection.getKey(), typeConfiguration, modelsContext, visited );
			reattachValue( collection.getElement(), typeConfiguration, modelsContext, visited );
			if ( collection instanceof IndexedCollection indexedCollection ) {
				reattachValue( indexedCollection.getIndex(), typeConfiguration, modelsContext, visited );
			}
			if ( collection instanceof IdentifierCollection identifierCollection ) {
				reattachValue( identifierCollection.getIdentifier(), typeConfiguration, modelsContext, visited );
			}
		}
		composites.forEach( component -> reattachValue( component, typeConfiguration, modelsContext, visited ) );
		visited.stream()
				.filter( BasicValue.class::isInstance )
				.map( BasicValue.class::cast )
				.forEach( basicValue -> {
					basicValue.reattachExplicitCustomType( classLoaderAccess );
					basicValue.reattachTypeAnnotation( modelsContext );
				} );
		visited.stream()
				.filter( SimpleValue.class::isInstance )
				.map( SimpleValue.class::cast )
				.forEach( simpleValue -> {
					simpleValue.getCustomIdGeneratorCreator().reattachModelsContext( modelsContext );
				} );
		visited.stream()
				.filter( Component.class::isInstance )
				.map( Component.class::cast )
				.forEach( component -> component.reattachCompositeUserType(
						classLoaderAccess,
						managedBeanRegistry,
						allowExtensionsInCdi
				) );
		visited.stream()
				.filter( Any.class::isInstance )
				.map( Any.class::cast )
				.forEach( any -> any.reattachImplicitDiscriminatorStrategy( classLoaderAccess, managedBeanRegistry ) );
	}

	private static void reattachProperties(
			List<Property> properties,
			TypeConfiguration typeConfiguration,
			ModelsContext modelsContext,
			Set<Value> visited) {
		properties.forEach( property -> {
			final var generatorCreator = property.getValueGeneratorCreator();
			if ( generatorCreator != null ) {
				generatorCreator.reattachModelsContext( modelsContext );
			}
			reattachValue( property.getValue(), typeConfiguration, modelsContext, visited );
		} );
	}

	private static void reattachValue(
			Value value,
			TypeConfiguration typeConfiguration,
			ModelsContext modelsContext,
			Set<Value> visited) {
		if ( value == null || !visited.add( value ) ) {
			return;
		}
		if ( value instanceof ManyToOne manyToOne ) {
			manyToOne.reattachTypeConfiguration( typeConfiguration );
		}
		else if ( value instanceof OneToMany oneToMany ) {
			oneToMany.reattachTypeConfiguration( typeConfiguration );
		}
		else if ( value instanceof OneToOne oneToOne ) {
			oneToOne.reattachTypeConfiguration( typeConfiguration );
		}
		else if ( value instanceof Any any ) {
			any.reattachTypeConfiguration( typeConfiguration );
		}
		if ( value instanceof Component component ) {
			reattachProperties(
					component.getProperties(),
					typeConfiguration,
					modelsContext,
					visited
			);
			reattachValue( component.getDiscriminator(), typeConfiguration, modelsContext, visited );
		}
	}
}
