/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model;

import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.metamodel.Attribute;
import org.hibernate.AnnotationException;
import org.hibernate.graph.internal.RootGraphImpl;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.internal.util.NullnessHelper;
import org.hibernate.metamodel.model.domain.EntityDomainType;

import java.util.function.Function;

import static org.hibernate.internal.util.StringHelper.isNotEmpty;

/**
 * @author Steve Ebersole
 */
public class NamedGraphCreatorJpa implements NamedGraphCreator {
	private final String name;
	private final NamedEntityGraph annotation;
	private final String jpaEntityName;

	public NamedGraphCreatorJpa(NamedEntityGraph annotation, String jpaEntityName) {
		this.name = NullnessHelper.coalesceSuppliedValues( annotation::name, () -> jpaEntityName );
		this.annotation = annotation;
		this.jpaEntityName = jpaEntityName;
	}

	@Override
	public <T> RootGraphImplementor<T> createEntityGraph(
			Function<Class<T>, EntityDomainType<?>> entityDomainClassResolver,
			Function<String, EntityDomainType<?>> entityDomainNameResolver) {
		//noinspection unchecked
		final EntityDomainType<T> rootEntityType = (EntityDomainType<T>) entityDomainNameResolver.apply( jpaEntityName );
		final RootGraphImplementor<T> entityGraph = createRootGraph( name, rootEntityType, annotation.includeAllAttributes() );

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
			for ( Attribute<? super T, ?> attribute : rootEntityType.getAttributes() ) {
				entityGraph.addAttributeNodes( attribute );
			}
		}
		return entityGraph;
	}
	private <T> void applyNamedAttributeNodes(
			NamedAttributeNode[] namedAttributeNodes,
			NamedEntityGraph namedEntityGraph,
			GraphImplementor<?> graphNode) {
		for ( NamedAttributeNode namedAttributeNode : namedAttributeNodes ) {
			final String value = namedAttributeNode.value();
			final AttributeNodeImplementor<?,?,?> attributeNode =
					(AttributeNodeImplementor<?,?,?>) graphNode.addAttributeNode( value );

			if ( isNotEmpty( namedAttributeNode.subgraph() ) ) {
				applyNamedSubgraphs(
						namedEntityGraph,
						namedAttributeNode.subgraph(),
						attributeNode,
						false
				);
			}
			if ( isNotEmpty( namedAttributeNode.keySubgraph() ) ) {
				applyNamedSubgraphs(
						namedEntityGraph,
						namedAttributeNode.keySubgraph(),
						attributeNode,
						true
				);
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
				final Class<?> subgraphType = namedSubgraph.type();
				final SubGraphImplementor<?> subgraph;
				if ( subgraphType.equals( void.class ) ) { // unspecified
					subgraph = attributeNode.addValueSubgraph();
				}
				else {
					subgraph = isKeySubGraph
							? makeAttributeNodeKeySubgraph( attributeNode, subgraphType )
							: makeAttributeNodeValueSubgraph( attributeNode, subgraphType );
				}
				applyNamedAttributeNodes( namedSubgraph.attributeNodes(), namedEntityGraph, subgraph );
			}
		}
	}

	private static <T, E, K> SubGraphImplementor<?> makeAttributeNodeValueSubgraph(
			AttributeNodeImplementor<T, E, K> attributeNode, Class<?> subgraphType) {
		final Class<?> attributeValueType =
				attributeNode.getAttributeDescriptor().getValueGraphType().getBindableJavaType();
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
				attributeNode.getAttributeDescriptor().getKeyGraphType().getBindableJavaType();
		if ( !attributeKeyType.isAssignableFrom( subgraphType ) ) {
			throw new AnnotationException( "Named subgraph type '" + subgraphType.getName()
										+ "' is not a subtype of the key type '" + attributeKeyType.getName() + "'" );
		}
		@SuppressWarnings("unchecked") // Safe, because we just checked
		final Class<? extends K> castType = (Class<? extends K>) subgraphType;
		return attributeNode.addKeySubgraph().addTreatedSubgraph( castType );
	}
}
