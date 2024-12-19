/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.Objects;

import org.hibernate.engine.FetchTiming;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.sql.results.graph.EntityGraphTraversalState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;

/**
 * @author Nathan Xu
 */
public class StandardEntityGraphTraversalStateImpl implements EntityGraphTraversalState {

	private final GraphSemantic graphSemantic;
	private final JpaMetamodel metamodel;
	private GraphImplementor<?> currentGraphContext;

	public StandardEntityGraphTraversalStateImpl(
			GraphSemantic graphSemantic,
			RootGraphImplementor<?> rootGraphImplementor,
			JpaMetamodel metamodel) {
		Objects.requireNonNull( graphSemantic, "graphSemantic cannot be null" );
		Objects.requireNonNull( rootGraphImplementor, "rootGraphImplementor cannot be null" );
		this.graphSemantic = graphSemantic;
		this.currentGraphContext = rootGraphImplementor;
		this.metamodel = metamodel;
	}

	@Override
	public void backtrack(TraversalResult previousContext) {
		currentGraphContext = previousContext.getGraph();
	}

	@Override
	public TraversalResult traverse(FetchParent fetchParent, Fetchable fetchable, boolean exploreKeySubgraph) {
		assert !( fetchable instanceof CollectionPart );
		if ( fetchable instanceof NonAggregatedIdentifierMapping ) {
			return new TraversalResult( currentGraphContext, new FetchStrategy( FetchTiming.IMMEDIATE, true ) );
		}

		final GraphImplementor<?> previousContextRoot = currentGraphContext;
		final AttributeNodeImplementor<?> attributeNode = getAttributeNode( fetchParent, fetchable );

		currentGraphContext = null;
		final FetchStrategy fetchStrategy;
		if ( attributeNode != null ) {
			fetchStrategy = new FetchStrategy( FetchTiming.IMMEDIATE, true );
			Class<?> subgraphType = null;
			final var attributeNodeSubgraph = attributeNode.getSubgraph();
			if ( fetchable instanceof PluralAttributeMapping ) {
				final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) fetchable;
				if ( exploreKeySubgraph ) {
					final var keySubgraphMap = attributeNode.getKeySubGraphMap();
					final var subgraphMapKey = getEntityCollectionPartJavaClass( pluralAttributeMapping.getIndexDescriptor() );
					currentGraphContext = keySubgraphMap.get( subgraphMapKey );
				}
				else {
					subgraphType = getEntityCollectionPartJavaClass( pluralAttributeMapping.getElementDescriptor() );
				}
			}
			else {
				assert !exploreKeySubgraph;
				subgraphType = fetchable.getJavaType().getJavaTypeClass();
			}

			if ( attributeNodeSubgraph != null && subgraphType != null ) {
				if ( subgraphType.equals( attributeNodeSubgraph.getClassType() ) ) {
					currentGraphContext = attributeNodeSubgraph;
				}
				else {
					currentGraphContext = attributeNodeSubgraph.getSubclassSubgraph( (Class) subgraphType );
				}
			}
		}
		else if ( graphSemantic == GraphSemantic.FETCH ) {
			fetchStrategy = new FetchStrategy( FetchTiming.DELAYED, false );
		}
		else {
			fetchStrategy = null;
		}
		return new TraversalResult( previousContextRoot, fetchStrategy );
	}

	private AttributeNodeImplementor<?> getAttributeNode(FetchParent fetchParent, Fetchable fetchable) {
		if ( !appliesTo( fetchParent ) ) {
			return null;
		}

		AttributeNodeImplementor<?> attributeNode = currentGraphContext.findAttributeNode( fetchable.getFetchableName() );

		if ( attributeNode != null ) {
			return attributeNode;
		}

		if ( !( fetchable instanceof AttributeMapping ) ) {
			return null;
		}


		AttributeMapping attributeMapping = (AttributeMapping) fetchable;
		var declaringType = attributeMapping.getDeclaringType().getJavaType().getJavaTypeClass();
		ManagedDomainType managedDeclaringType = metamodel.findManagedType( declaringType );

		if ( managedDeclaringType == null ) {
			return null;
		}

		var currentGraphSubTypes = currentGraphContext.getGraphedType().getSubTypes();

		if ( !currentGraphSubTypes.contains( managedDeclaringType ) ) {
			return null;
		}

		var subclassSubgraph = currentGraphContext.getSubclassSubgraph( managedDeclaringType.getJavaType() );

		if ( subclassSubgraph == null ) {
			return null;
		}

		return subclassSubgraph.findAttributeNode( fetchable.getFetchableName() );
	}


	private Class<?> getEntityCollectionPartJavaClass(CollectionPart collectionPart) {
		if ( collectionPart instanceof EntityCollectionPart ) {
			EntityCollectionPart entityCollectionPart = (EntityCollectionPart) collectionPart;
			return entityCollectionPart.getEntityMappingType().getJavaType().getJavaTypeClass();
		}
		else {
			return null;
		}
	}

	private boolean appliesTo(FetchParent fetchParent) {
		return currentGraphContext != null && fetchParent.appliesTo( currentGraphContext, metamodel );
	}

}
