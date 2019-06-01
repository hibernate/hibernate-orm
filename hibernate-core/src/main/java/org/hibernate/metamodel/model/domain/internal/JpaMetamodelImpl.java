/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javax.persistence.EntityGraph;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedSubgraph;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.cfg.annotations.NamedEntityGraphDefinition;
import org.hibernate.graph.internal.RootGraphImpl;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.internal.EntityManagerMessageLogger;
import org.hibernate.internal.HEMLogging;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.internal.InflightRuntimeMetamodel;
import org.hibernate.metamodel.model.domain.AbstractDomainType;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.MappedSuperclassDomainType;
import org.hibernate.query.sqm.tree.domain.SqmPolymorphicRootDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class JpaMetamodelImpl implements JpaMetamodel {
	private static final EntityManagerMessageLogger log = HEMLogging.messageLogger( JpaMetamodel.class );
	private static final String INVALID_IMPORT = "";

	private final TypeConfiguration typeConfiguration;

	private final Map<String, EntityDomainType<?>> entityDescriptorMap;

	private final Map<Class<?>, MappedSuperclassDomainType<?>> mappedSuperclassTypeMap;

	private final Map<Class, EmbeddableDomainType<?>> embeddableDescriptorMap;

	private final Map<String, String> nameToImportNameMap;

	private final transient Map<String, RootGraphImplementor> entityGraphMap = new ConcurrentHashMap<>();

	private final Map<Class, SqmPolymorphicRootDescriptor<?>> polymorphicEntityReferenceMap = new ConcurrentHashMap<>();

	private final Map<Class, String> entityProxyInterfaceMap;


	public JpaMetamodelImpl(
			InflightRuntimeMetamodel runtimeMetamodel,
			java.util.Collection<NamedEntityGraphDefinition> namedEntityGraphDefinitions) {
		this.typeConfiguration = runtimeMetamodel.getTypeConfiguration();

		entityDescriptorMap = runtimeMetamodel.getJpaEntityTypeMap();
		mappedSuperclassTypeMap = runtimeMetamodel.getJpaMappedSuperclassTypeMap();
		embeddableDescriptorMap = runtimeMetamodel.getJpaEmbeddableDescriptorMap();

		entityDescriptorMap.values()
				.forEach( domainType -> ( (AbstractDomainType) domainType ).injectJpaMetamodel( this ) );
		mappedSuperclassTypeMap.values().forEach( domainType -> ( (AbstractDomainType) domainType ).injectJpaMetamodel(
				this ) );
		entityDescriptorMap.values()
				.forEach( domainType -> ( (AbstractDomainType) domainType ).injectJpaMetamodel( this ) );

		nameToImportNameMap = runtimeMetamodel.getNameToImportNameMap();
		entityProxyInterfaceMap = runtimeMetamodel.getEntityProxyInterfaceMap();
		applyNamedEntityGraphs( namedEntityGraphDefinitions );
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	@Override
	public <X> EntityDomainType<X> entity(String entityName) {
		//noinspection unchecked
		return (EntityDomainType) entityDescriptorMap.get( entityName );
	}

	@Override
	public <X> EntityDomainType<X> resolveHqlEntityReference(String entityName) {
		final String rename = resolveImportedName( entityName );
		if ( rename != null ) {
			entityName = rename;
		}

		final EntityDomainType<X> entityDescriptor = entity( entityName );
		if ( entityDescriptor != null ) {
			return entityDescriptor;
		}

		final Class<X> requestedClass = resolveRequestedClass( entityName );
		if ( requestedClass != null ) {
			return resolveEntityReference( requestedClass );
		}

		throw new IllegalArgumentException( "Could not resolve entity reference " + entityName );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitManagedTypes(Consumer<ManagedDomainType<?>> action) {
		visitEntityTypes( (Consumer) action );
		visitEmbeddables( (Consumer) action );
		mappedSuperclassTypeMap.values().forEach( action );
	}

	@Override
	public <X> ManagedDomainType<X> findManagedType(Class<X> cls) {
		ManagedType<?> type = entityDescriptorMap.get( cls );
		if ( type == null ) {
			type = mappedSuperclassTypeMap.get( cls );
		}
		if ( type == null ) {
			type = embeddableDescriptorMap.get( cls );
		}
		if ( type == null ) {
			return null;
		}

		//noinspection unchecked
		return (ManagedDomainType<X>) type;
	}

	@Override
	public void visitEntityTypes(Consumer<EntityDomainType<?>> action) {
		entityDescriptorMap.values().forEach( action );
	}

	@Override
	public <X> EntityDomainType<X> findEntityType(Class<X> cls) {
		final EntityType<?> entityType = entityDescriptorMap.get( cls );
		if ( entityType == null ) {
			return null;
		}
		//noinspection unchecked
		return (EntityDomainType<X>) entityType;
	}

	@Override
	public void visitRootEntityTypes(Consumer<EntityDomainType<?>> action) {
		entityDescriptorMap.values().forEach(
				entityDomainType -> {
					if ( entityDomainType.getSuperType() == null ) {
						action.accept( entityDomainType );
					}
				}
		);
	}

	@Override
	public void visitEmbeddables(Consumer<EmbeddableDomainType<?>> action) {
		embeddableDescriptorMap.values().forEach( action );
	}

	@Override
	public <X> ManagedDomainType<X> managedType(Class<X> cls) {
		ManagedType<?> type = entityDescriptorMap.get( cls );
		if ( type == null ) {
			type = mappedSuperclassTypeMap.get( cls );
		}
		if ( type == null ) {
			type = embeddableDescriptorMap.get( cls );
		}
		if ( type == null ) {
			// per JPA
			throw new IllegalArgumentException( "Not a managed type: " + cls );
		}

		//noinspection unchecked
		return (ManagedDomainType<X>) type;
	}

	@Override
	public <X> EntityDomainType<X> entity(Class<X> cls) {
		final EntityType<?> entityType = entityDescriptorMap.get( cls );
		if ( entityType == null ) {
			throw new IllegalArgumentException( "Not an entity: " + cls );
		}
		//noinspection unchecked
		return (EntityDomainType<X>) entityType;
	}

	@Override
	public <X> EmbeddableDomainType<X> embeddable(Class<X> cls) {
		final EmbeddableDomainType<?> embeddableType = embeddableDescriptorMap.get( cls );
		if ( embeddableType == null ) {
			throw new IllegalArgumentException( "Not an embeddable: " + cls );
		}
		//noinspection unchecked
		return (EmbeddableDomainType<X>) embeddableType;
	}

	@Override
	public Set<ManagedType<?>> getManagedTypes() {
		final int setSize = CollectionHelper.determineProperSizing(
				entityDescriptorMap.size() + mappedSuperclassTypeMap.size() + embeddableDescriptorMap.size()
		);
		final Set<ManagedType<?>> managedTypes = new HashSet<>( setSize );
		managedTypes.addAll( entityDescriptorMap.values() );
		managedTypes.addAll( mappedSuperclassTypeMap.values() );
		managedTypes.addAll( embeddableDescriptorMap.values() );
		return managedTypes;
	}

	@Override
	public Set<EntityType<?>> getEntities() {
		return new HashSet<>( entityDescriptorMap.values() );
	}

	@Override
	public Set<EmbeddableType<?>> getEmbeddables() {
		return new HashSet<>( embeddableDescriptorMap.values() );
	}

	@Override
	public <T> void addNamedEntityGraph(String graphName, RootGraphImplementor<T> entityGraph) {
		final EntityGraph old = entityGraphMap.put(
				graphName,
				entityGraph.makeImmutableCopy( graphName )
		);

		if ( old != null ) {
			log.debugf( "EntityGraph being replaced on EntityManagerFactory for name %s", graphName );
		}
	}

	@Override
	public <T> RootGraphImplementor<T> findEntityGraphByName(String name) {
		//noinspection unchecked
		return entityGraphMap.get( name );
	}

	@Override
	public <T> List<RootGraphImplementor<? super T>> findEntityGraphsByJavaType(Class<T> entityClass) {
		final EntityDomainType<T> entityType = entity( entityClass );
		if ( entityType == null ) {
			throw new IllegalArgumentException( "Given class is not an entity : " + entityClass.getName() );
		}

		final List<RootGraphImplementor<? super T>> results = new ArrayList<>();

		for ( EntityGraph entityGraph : entityGraphMap.values() ) {
			if ( !( entityGraph instanceof RootGraphImplementor ) ) {
				continue;
			}

			final RootGraphImplementor egi = (RootGraphImplementor) entityGraph;
			//noinspection unchecked
			if ( egi.appliesTo( entityType ) ) {
				//noinspection unchecked
				results.add( egi );
			}
		}

		return results;
	}

	private String resolveImportedName(String name) {
		String result = nameToImportNameMap.get( name );
		if ( result == null ) {
			// see if the name is a fully-qualified class name
			try {
				getServiceRegistry().getService( ClassLoaderService.class ).classForName( name );

				// it is a fully-qualified class name - add it to the cache
				//		so we do not keep trying later
				nameToImportNameMap.put( name, name );
				return name;
			}
			catch (ClassLoadingException cnfe) {
				// it is a NOT fully-qualified class name - add a marker entry
				//		so we do not keep trying later
				nameToImportNameMap.put( name, INVALID_IMPORT );
				return null;
			}
		}
		else {
			// explicitly check for same instance
			//noinspection StringEquality
			if ( result == INVALID_IMPORT ) {
				return null;
			}
			else {
				return result;
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void applyNamedEntityGraphs(java.util.Collection<NamedEntityGraphDefinition> namedEntityGraphs) {
		for ( NamedEntityGraphDefinition definition : namedEntityGraphs ) {
			log.debugf(
					"Applying named entity graph [name=%s, entity-name=%s, jpa-entity-name=%s",
					definition.getRegisteredName(),
					definition.getEntityName(),
					definition.getJpaEntityName()
			);
			final EntityDomainType entityType = entity( definition.getEntityName() );
			if ( entityType == null ) {
				throw new IllegalArgumentException(
						"Attempted to register named entity graph [" + definition.getRegisteredName()
								+ "] for unknown entity [" + definition.getEntityName() + "]"

				);
			}

			final RootGraphImpl entityGraph = new RootGraphImpl(
					definition.getRegisteredName(),
					entityType,
					this
			);

			final NamedEntityGraph namedEntityGraph = definition.getAnnotation();

			if ( namedEntityGraph.includeAllAttributes() ) {
				for ( Object attributeObject : entityType.getAttributes() ) {
					entityGraph.addAttributeNodes( (Attribute) attributeObject );
				}
			}

			if ( namedEntityGraph.attributeNodes() != null ) {
				applyNamedAttributeNodes( namedEntityGraph.attributeNodes(), namedEntityGraph, entityGraph );
			}

			entityGraphMap.put( definition.getRegisteredName(), entityGraph );
		}
	}

	private void applyNamedAttributeNodes(
			NamedAttributeNode[] namedAttributeNodes,
			NamedEntityGraph namedEntityGraph,
			GraphImplementor graphNode) {
		for ( NamedAttributeNode namedAttributeNode : namedAttributeNodes ) {
			final String value = namedAttributeNode.value();
			AttributeNodeImplementor attributeNode = graphNode.addAttributeNode( value );
			if ( StringHelper.isNotEmpty( namedAttributeNode.subgraph() ) ) {
				final SubGraphImplementor subgraph = attributeNode.makeSubGraph();
				applyNamedSubgraphs(
						namedEntityGraph,
						namedAttributeNode.subgraph(),
						subgraph
				);
			}
			if ( StringHelper.isNotEmpty( namedAttributeNode.keySubgraph() ) ) {
				final SubGraphImplementor subgraph = attributeNode.makeKeySubGraph();

				applyNamedSubgraphs(
						namedEntityGraph,
						namedAttributeNode.keySubgraph(),
						subgraph
				);
			}
		}
	}

	private void applyNamedSubgraphs(
			NamedEntityGraph namedEntityGraph,
			String subgraphName,
			SubGraphImplementor subgraph) {
		for ( NamedSubgraph namedSubgraph : namedEntityGraph.subgraphs() ) {
			if ( subgraphName.equals( namedSubgraph.name() ) ) {
				applyNamedAttributeNodes(
						namedSubgraph.attributeNodes(),
						namedEntityGraph,
						subgraph
				);
			}
		}
	}

	private <X> Class<X> resolveRequestedClass(String entityName) {
		try {
			return getServiceRegistry().getService( ClassLoaderService.class ).classForName( entityName );
		}
		catch (ClassLoadingException e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public <T> EntityDomainType<T> resolveEntityReference(Class<T> javaType) {
		// try the incoming Java type as a "strict" entity reference
		{
			final EntityDomainType<?> descriptor = entityDescriptorMap.get( javaType );
			if ( descriptor != null ) {
				return (EntityDomainType<T>) descriptor;
			}
		}

		// Next, try it as a proxy interface reference
		{
			final String proxyEntityName = entityProxyInterfaceMap.get( javaType );
			if ( proxyEntityName != null ) {
				return (EntityDomainType<T>) entityDescriptorMap.get( proxyEntityName );
			}
		}

		// otherwise, try to handle it as a polymorphic reference
		{
			if ( polymorphicEntityReferenceMap.containsKey( javaType ) ) {
				return (EntityDomainType<T>) polymorphicEntityReferenceMap.get( javaType );
			}

			final Set<EntityDomainType<?>> matchingDescriptors = new HashSet<>();
			visitEntityTypes(
					entityDomainType -> {
						if ( javaType.isAssignableFrom( entityDomainType.getJavaType() ) ) {
							matchingDescriptors.add( entityDomainType );
						}
					}
			);
			if ( !matchingDescriptors.isEmpty() ) {
				final SqmPolymorphicRootDescriptor descriptor = new SqmPolymorphicRootDescriptor(
						typeConfiguration.getJavaTypeDescriptorRegistry().resolveDescriptor( javaType ),
						matchingDescriptors
				);
				polymorphicEntityReferenceMap.put( javaType, descriptor );
				return descriptor;
			}
		}

		throw new IllegalArgumentException( "Could not resolve entity reference : " + javaType.getName() );
	}
}
