/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.internal;

import java.util.Map;
import java.util.Objects;

import jakarta.persistence.BatchSize;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.FetchOption;

import org.hibernate.engine.FetchTiming;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
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
		assert !(fetchable instanceof CollectionPart);
		if ( fetchable instanceof NonAggregatedIdentifierMapping ) {
			return new TraversalResult( currentGraphContext,
					new FetchStrategy( FetchTiming.IMMEDIATE, true ) );
		}
		else {
			final var previousContextRoot = currentGraphContext;
			final var attributeNode = appliesTo( fetchParent )
					? currentGraphContext.findNode( fetchable.getFetchableName() )
					: null;
			final var batchSize = getBatchSize( attributeNode );
			currentGraphContext = null;
			return new TraversalResult( previousContextRoot,
					handleFetchType( fetchable, exploreKeySubgraph, attributeNode, batchSize ),
					getCacheStoreMode( attributeNode ),
					getCacheRetrieveMode( attributeNode ),
					batchSize );
		}
	}

	private static CacheStoreMode getCacheStoreMode(AttributeNodeImplementor<?, ?, ?> attributeNode) {
		if ( attributeNode == null ) {
			return null;
		}
		for ( var option : attributeNode.getOptions() ) {
			if ( option instanceof CacheStoreMode cacheStoreMode ) {
				return cacheStoreMode;
			}
		}
		return null;
	}

	private static CacheRetrieveMode getCacheRetrieveMode(AttributeNodeImplementor<?, ?, ?> attributeNode) {
		if ( attributeNode == null ) {
			return null;
		}
		for ( FetchOption option : attributeNode.getOptions() ) {
			if ( option instanceof CacheRetrieveMode cacheRetrieveMode ) {
				return cacheRetrieveMode;
			}
		}
		return null;
	}

	private static Integer getBatchSize(AttributeNodeImplementor<?, ?, ?> attributeNode) {
		if ( attributeNode == null ) {
			return null;
		}
		for ( FetchOption option : attributeNode.getOptions() ) {
			if ( option instanceof BatchSize batchSize && batchSize.batchSize() >= 0 ) {
				return batchSize.batchSize();
			}
		}
		return null;
	}

	private FetchStrategy handleFetchType(
			Fetchable fetchable,
			boolean exploreKeySubgraph,
			AttributeNodeImplementor<?, ?, ?> attributeNode,
			Integer batchSize) {
		final var fetchType = attributeNode == null ? null : attributeNode.getFetchType();
		if ( fetchType != null ) {
			switch ( fetchType ) {
				case EAGER:
					final Map<? extends Class<?>, ? extends SubGraphImplementor<?>> subgraphMap;
					final Class<?> subgraphMapKey;
					if ( fetchable instanceof PluralAttributeMapping pluralAttributeMapping ) {
						if ( exploreKeySubgraph ) {
							subgraphMap = attributeNode.getKeySubGraphs();
							subgraphMapKey = getEntityCollectionPartJavaClass(
									pluralAttributeMapping.getIndexDescriptor() );
						}
						else {
							subgraphMap = attributeNode.getSubGraphs();
							subgraphMapKey = getEntityCollectionPartJavaClass(
									pluralAttributeMapping.getElementDescriptor() );
						}
					}
					else {
						assert !exploreKeySubgraph;
						subgraphMap = attributeNode.getSubGraphs();
						subgraphMapKey = fetchable.getJavaType().getJavaTypeClass();
					}
					if ( subgraphMap != null && subgraphMapKey != null ) {
						currentGraphContext = subgraphMap.get( subgraphMapKey );
					}
					return new FetchStrategy( FetchTiming.IMMEDIATE, batchSize == null );
				case LAZY:
					return new FetchStrategy( FetchTiming.DELAYED, false );
				default:
					return null;
			}
		}
		else if ( graphSemantic == GraphSemantic.FETCH ) {
			return new FetchStrategy( FetchTiming.DELAYED, false );
		}
		else {
			return null;
		}
	}

	private Class<?> getEntityCollectionPartJavaClass(CollectionPart collectionPart) {
		return collectionPart instanceof EntityCollectionPart entityCollectionPart
				? entityCollectionPart.getEntityMappingType().getJavaType().getJavaTypeClass()
				: null;
	}

	private boolean appliesTo(FetchParent fetchParent) {
		return currentGraphContext != null
			&& fetchParent.appliesTo( currentGraphContext, metamodel );
	}

}
