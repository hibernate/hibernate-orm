/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
import org.hibernate.AnnotationException;
import org.hibernate.boot.model.NamedGraphCreator;
import org.hibernate.graph.internal.RootGraphImpl;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.model.domain.EntityDomainType;

import java.util.function.Function;

import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.nullIfEmpty;

/**
 * @author Steve Ebersole
 */
class NamedGraphCreatorJpa implements NamedGraphCreator {
	private final String name;
	private final NamedEntityGraph annotation;
	private final String jpaEntityName;

	NamedGraphCreatorJpa(NamedEntityGraph annotation, String jpaEntityName) {
		final String name = nullIfEmpty( annotation.name() );
		this.name = name == null ? jpaEntityName : name;
		this.annotation = annotation;
		this.jpaEntityName = jpaEntityName;
	}

	@Override
	public <T> RootGraphImplementor<T> createEntityGraph(
			Function<Class<T>, EntityDomainType<?>> entityDomainClassResolver,
			Function<String, EntityDomainType<?>> entityDomainNameResolver) {
		//noinspection unchecked
		final EntityDomainType<T> rootEntityType =
				(EntityDomainType<T>) entityDomainNameResolver.apply( jpaEntityName );
		final RootGraphImplementor<T> entityGraph =
				createRootGraph( name, rootEntityType, annotation.includeAllAttributes() );

		if ( annotation.subclassSubgraphs() != null ) {
			for ( NamedSubgraph subclassSubgraph : annotation.subclassSubgraphs() ) {
				final Class<?> subgraphType = subclassSubgraph.type();
				final Class<T> graphJavaType = entityGraph.getGraphedType().getJavaType();
				if ( !graphJavaType.isAssignableFrom( subgraphType ) ) {
					throw new AnnotationException( "Named subgraph type '" + subgraphType.getName()
												+ "' is not a subtype of the graph type '" + graphJavaType.getName() + "'" );
				}
				@SuppressWarnings("unchecked") // Safe, because we just checked
				final Class<? extends T> subtype = (Class<? extends T>) subgraphType;
				final GraphImplementor<? extends T> subgraph = entityGraph.addTreatedSubgraph( subtype );
				applyNamedAttributeNodes( subclassSubgraph.attributeNodes(), annotation, subgraph );
			}
		}

		if ( annotation.attributeNodes() != null ) {
			applyNamedAttributeNodes( annotation.attributeNodes(), annotation, entityGraph );
		}

		return entityGraph;
	}

	private static <T> RootGraphImplementor<T> createRootGraph(
			String name,
			EntityDomainType<T> rootEntityType,
			boolean includeAllAttributes) {
		final RootGraphImpl<T> entityGraph = new RootGraphImpl<>( name, rootEntityType );
		if ( includeAllAttributes ) {
			for ( var attribute : rootEntityType.getAttributes() ) {
				entityGraph.addAttributeNodes( attribute );
			}
		}
		return entityGraph;
	}

	private void applyNamedAttributeNodes(
			NamedAttributeNode[] namedAttributeNodes,
			NamedEntityGraph namedEntityGraph,
			GraphImplementor<?> graphNode) {
		for ( NamedAttributeNode namedAttributeNode : namedAttributeNodes ) {
			final var attributeNode =
					(AttributeNodeImplementor<?,?,?>)
							graphNode.addAttributeNode( namedAttributeNode.value() );
			final String subgraph = namedAttributeNode.subgraph();
			if ( isNotEmpty( subgraph ) ) {
				applyNamedSubgraphs( namedEntityGraph, subgraph, attributeNode, false );
			}
			final String keySubgraph = namedAttributeNode.keySubgraph();
			if ( isNotEmpty( keySubgraph ) ) {
				applyNamedSubgraphs( namedEntityGraph, keySubgraph, attributeNode, true );
			}
		}
	}

	private <T,E,K> void applyNamedSubgraphs(
			NamedEntityGraph namedEntityGraph,
			String subgraphName,
			AttributeNodeImplementor<T,E,K> attributeNode,
			boolean isKeySubGraph) {
		for ( NamedSubgraph namedSubgraph : namedEntityGraph.subgraphs() ) {
			if ( subgraphName.equals( namedSubgraph.name() ) ) {
				applyNamedAttributeNodes( namedSubgraph.attributeNodes(), namedEntityGraph,
						createSubgraph( attributeNode, isKeySubGraph, namedSubgraph.type() ) );
			}
		}
	}

	private static SubGraphImplementor<?> createSubgraph(
			AttributeNodeImplementor<?, ?, ?> attributeNode,
			boolean isKeySubGraph, Class<?> subgraphType) {
		if ( void.class.equals( subgraphType ) ) { // unspecified
			return attributeNode.addValueSubgraph();
		}
		else {
			return isKeySubGraph
					? makeAttributeNodeKeySubgraph( attributeNode, subgraphType )
					: makeAttributeNodeValueSubgraph( attributeNode, subgraphType );
		}
	}

	private static <T, E, K> SubGraphImplementor<?> makeAttributeNodeValueSubgraph(
			AttributeNodeImplementor<T, E, K> attributeNode, Class<?> subgraphType) {
		final Class<?> attributeValueType =
				attributeNode.getAttributeDescriptor().getValueGraphType().getJavaType();
		if ( !attributeValueType.isAssignableFrom( subgraphType ) ) {
			throw new AnnotationException( "Named subgraph type '" + subgraphType.getName()
										+ "' is not a subtype of the value type '" + attributeValueType.getName() + "'" );
		}
		@SuppressWarnings("unchecked") // Safe, because we just checked
		final Class<? extends E> castType = (Class<? extends E>) subgraphType;
		return attributeNode.addValueSubgraph().addTreatedSubgraph( castType );
	}

	private static <T, E, K> SubGraphImplementor<?> makeAttributeNodeKeySubgraph(
			AttributeNodeImplementor<T, E, K> attributeNode, Class<?> subgraphType) {
		final Class<?> attributeKeyType =
				attributeNode.getAttributeDescriptor().getKeyGraphType().getJavaType();
		if ( !attributeKeyType.isAssignableFrom( subgraphType ) ) {
			throw new AnnotationException( "Named subgraph type '" + subgraphType.getName()
										+ "' is not a subtype of the key type '" + attributeKeyType.getName() + "'" );
		}
		@SuppressWarnings("unchecked") // Safe, because we just checked
		final Class<? extends K> castType = (Class<? extends K>) subgraphType;
		return attributeNode.addKeySubgraph().addTreatedSubgraph( castType );
	}
}
