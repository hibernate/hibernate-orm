/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.AnnotationException;
import org.hibernate.boot.model.NamedGraphCreator;
import org.hibernate.graph.internal.RootGraphImpl;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphParserEntityClassResolver;
import org.hibernate.graph.spi.GraphParserEntityNameResolver;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.service.ServiceRegistry;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.nullIfEmpty;

/**
 * @author Steve Ebersole
 */
class NamedGraphCreatorJpa implements NamedGraphCreator {
	private final String name;
	private final NamedEntityGraph annotation;
	private final String jpaEntityName;
	private final List<FetchGraphContribution> fetchContributions;

	NamedGraphCreatorJpa(NamedEntityGraph annotation, String jpaEntityName) {
		this( annotation, jpaEntityName, emptyList() );
	}

	NamedGraphCreatorJpa(NamedEntityGraph annotation, String jpaEntityName, List<FetchGraphContribution> fetchContributions) {
		final String name = nullIfEmpty( annotation.name() );
		this.name = name == null ? jpaEntityName : name;
		this.annotation = annotation;
		this.jpaEntityName = jpaEntityName;
		this.fetchContributions = fetchContributions;
	}

	@Override
	public RootGraphImplementor<?> createEntityGraph(
			GraphParserEntityClassResolver entityDomainClassResolver,
			GraphParserEntityNameResolver entityDomainNameResolver,
			ServiceRegistry serviceRegistry) {
		return createGraph( (EntityDomainType<?>)
				entityDomainNameResolver.resolveEntityName( jpaEntityName ) );
	}

	private <T> @NonNull RootGraphImplementor<T> createGraph(EntityDomainType<T> rootEntityType) {
		validateFetchContributions();
		final var entityGraph =
				createRootGraph( name, rootEntityType, annotation.includeAllAttributes() );
		final var subclassSubgraphs = annotation.subclassSubgraphs();
		if ( subclassSubgraphs != null ) {
			for ( var subclassSubgraph : subclassSubgraphs ) {
				final var subgraphType = subclassSubgraph.type();
				final var graphJavaType = entityGraph.getGraphedType().getJavaType();
				if ( !graphJavaType.isAssignableFrom( subgraphType ) ) {
					throw new AnnotationException( "Named subgraph type '" + subgraphType.getName()
								+ "' is not a subtype of the graph type '" + graphJavaType.getName() + "'" );
				}
				applyNamedAttributeNodes( subclassSubgraph.attributeNodes(), annotation,
						entityGraph.addTreatedSubgraph( subgraphType.asSubclass( graphJavaType ) ), "" );
			}
		}
		if ( annotation.attributeNodes() != null ) {
			applyNamedAttributeNodes( annotation.attributeNodes(), annotation, entityGraph, null );
		}
		else {
			applyFetchContributions( entityGraph, null );
		}
		return entityGraph;
	}

	private static <T> RootGraphImplementor<T> createRootGraph(
			String name,
			EntityDomainType<T> rootEntityType,
			boolean includeAllAttributes) {
		final var entityGraph = new RootGraphImpl<>( name, rootEntityType );
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
			GraphImplementor<?> graphNode,
			String graphNodeName) {
		for ( var namedAttributeNode : namedAttributeNodes ) {
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
		applyFetchContributions( graphNode, graphNodeName );
	}

	private <T,E,K> void applyNamedSubgraphs(
			NamedEntityGraph namedEntityGraph,
			String subgraphName,
			AttributeNodeImplementor<T,E,K> attributeNode,
			boolean isKeySubGraph) {
		for ( var namedSubgraph : namedEntityGraph.subgraphs() ) {
			if ( subgraphName.equals( namedSubgraph.name() ) ) {
				applyNamedAttributeNodes( namedSubgraph.attributeNodes(), namedEntityGraph,
						createSubgraph( attributeNode, isKeySubGraph, namedSubgraph.type() ), subgraphName );
			}
		}
	}

	private void validateFetchContributions() {
		validateConflictingFetchContributions();
		for ( var fetchContribution : fetchContributions ) {
			if ( fetchContribution.appliesTo( name ) ) {
				for ( var subgraphName : fetchContribution.subgraphNames() ) {
					if ( !isNamedSubgraph( subgraphName ) ) {
						throw new AnnotationException(
								"Attribute '" + fetchContribution.attributeName()
										+ "' is annotated '@Fetch' for graph '" + name
										+ "' with an unknown subgraph '" + subgraphName + "'" );
					}
				}
			}
		}
	}

	private void validateConflictingFetchContributions() {
		for ( int i = 0; i < fetchContributions.size(); i++ ) {
			final var first = fetchContributions.get( i );
			if ( !first.appliesTo( name ) ) {
				continue;
			}
			for ( int j = i + 1; j < fetchContributions.size(); j++ ) {
				final var second = fetchContributions.get( j );
				if ( sameFetchTarget( first, second ) ) {
					validateCompatibleOptions( first, second );
				}
			}
		}
	}

	private static boolean sameFetchTarget(FetchGraphContribution first, FetchGraphContribution second) {
		return first.graphName().equals( second.graphName() )
			&& first.attributeName().equals( second.attributeName() )
			&& targetsSameGraphNode( first.subgraphNames(), second.subgraphNames() );
	}

	private static boolean targetsSameGraphNode(String[] firstSubgraphs, String[] secondSubgraphs) {
		if ( firstSubgraphs.length == 0 || secondSubgraphs.length == 0 ) {
			return firstSubgraphs.length == secondSubgraphs.length;
		}
		return Arrays.stream( firstSubgraphs ).anyMatch( first -> Arrays.asList( secondSubgraphs ).contains( first ) );
	}

	private static void validateCompatibleOptions(FetchGraphContribution first, FetchGraphContribution second) {
		for ( var firstOption : first.options() ) {
			for ( var secondOption : second.options() ) {
				if ( firstOption.getClass() == secondOption.getClass() && !firstOption.equals( secondOption ) ) {
					throw new AnnotationException(
							"Attribute '" + first.attributeName()
									+ "' has conflicting '@Fetch' options for graph '"
									+ first.graphName() + "': " + firstOption + " and " + secondOption );
				}
			}
		}
	}

	private boolean isNamedSubgraph(String subgraphName) {
		for ( var namedSubgraph : annotation.subgraphs() ) {
			if ( subgraphName.equals( namedSubgraph.name() ) ) {
				return true;
			}
		}
		return false;
	}

	private void applyFetchContributions(GraphImplementor<?> graphNode, String subgraphName) {
		for ( var fetchContribution : fetchContributions ) {
			if ( fetchContribution.appliesTo( name ) && fetchContribution.appliesToSubgraph( subgraphName ) ) {
				final var attributeNode =
						(AttributeNodeImplementor<?, ?, ?>)
								graphNode.addAttributeNode( fetchContribution.attributeName() );
				applyFetchOptions( attributeNode, fetchContribution );
			}
		}
	}

	private static void applyFetchOptions(
			AttributeNodeImplementor<?, ?, ?> attributeNode,
			FetchGraphContribution fetchContribution) {
		for ( var option : fetchContribution.options() ) {
			attributeNode.addOption( option );
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
		final var attributeValueType =
				attributeNode.getAttributeDescriptor()
						.getValueGraphType().getJavaType();
		if ( !attributeValueType.isAssignableFrom( subgraphType ) ) {
			throw new AnnotationException( "Named subgraph type '" + subgraphType.getName()
						+ "' is not a subtype of the value type '" + attributeValueType.getName() + "'" );
		}
		return attributeNode.addValueSubgraph().addTreatedSubgraph(
				subgraphType.asSubclass( attributeNode.getValueSubgraph().getClassType() ) );
	}

	private static <T, E, K> SubGraphImplementor<?> makeAttributeNodeKeySubgraph(
			AttributeNodeImplementor<T, E, K> attributeNode, Class<?> subgraphType) {
		final var attributeKeyType =
				attributeNode.getAttributeDescriptor()
						.getKeyGraphType().getJavaType();
		if ( !attributeKeyType.isAssignableFrom( subgraphType ) ) {
			throw new AnnotationException( "Named subgraph type '" + subgraphType.getName()
						+ "' is not a subtype of the key type '" + attributeKeyType.getName() + "'" );
		}
		return attributeNode.addKeySubgraph().addTreatedSubgraph(
				subgraphType.asSubclass( attributeNode.getKeySubgraph().getClassType() ) );
	}
}
