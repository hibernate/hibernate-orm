/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.implicit;

import org.hibernate.engine.FetchTiming;
import org.hibernate.query.results.spi.FetchBuilder;
import org.hibernate.query.results.internal.DomainResultCreationStateImpl;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.embeddable.EmbeddableValuedFetchable;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static java.util.Collections.emptyMap;
import static org.hibernate.internal.util.collections.CollectionHelper.linkedMapOfSize;
import static org.hibernate.query.results.internal.Builders.implicitFetchBuilder;
import static org.hibernate.query.results.internal.ResultsHelper.impl;

/**
 * @author Steve Ebersole
 */
public class ImplicitFetchBuilderEmbeddable implements ImplicitFetchBuilder {
	private final NavigablePath fetchPath;
	private final EmbeddableValuedFetchable fetchable;
	private final Map<Fetchable, FetchBuilder> fetchBuilders;

	public ImplicitFetchBuilderEmbeddable(
			NavigablePath fetchPath,
			EmbeddableValuedFetchable fetchable,
			DomainResultCreationState creationState) {
		this.fetchPath = fetchPath;
		this.fetchable = fetchable;
		this.fetchBuilders = fetchBuilderMap( fetchPath, fetchable, impl( creationState ) );
	}

	private static Map<Fetchable, FetchBuilder> fetchBuilderMap(
			NavigablePath fetchPath,
			EmbeddableValuedFetchable fetchable,
			DomainResultCreationStateImpl creationStateImpl) {
		final var fetchBuilderResolver =
				creationStateImpl.getCurrentExplicitFetchMementoResolver();
		final int size = fetchable.getNumberOfFetchables();
		final Map<Fetchable, FetchBuilder> fetchBuilders = linkedMapOfSize( size );
		for ( int i = 0; i < size; i++ ) {
			final var subFetchable = fetchable.getFetchable( i );
			final var explicitFetchBuilder = fetchBuilderResolver.apply( subFetchable );
			fetchBuilders.put( subFetchable,
					explicitFetchBuilder == null
							? implicitFetchBuilder( fetchPath, subFetchable, creationStateImpl )
							: explicitFetchBuilder );
		}
		return fetchBuilders;
	}

	private ImplicitFetchBuilderEmbeddable(ImplicitFetchBuilderEmbeddable original) {
		this.fetchPath = original.fetchPath;
		this.fetchable = original.fetchable;
		if ( original.fetchBuilders.isEmpty() ) {
			fetchBuilders = emptyMap();
		}
		else {
			fetchBuilders = new HashMap<>( original.fetchBuilders.size() );
			for ( var entry : original.fetchBuilders.entrySet() ) {
				fetchBuilders.put( entry.getKey(), entry.getValue().cacheKeyInstance() );
			}
		}
	}

	@Override
	public FetchBuilder cacheKeyInstance() {
		return new ImplicitFetchBuilderEmbeddable( this );
	}

	@Override
	public Fetch buildFetch(
			FetchParent parent,
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			DomainResultCreationState creationState) {
		final var creationStateImpl = impl( creationState );

		// make sure the TableGroup is available
		tableGroup( parent, fetchPath, creationStateImpl );

		//		final FetchParent fetchParent = (FetchParent) fetch;
//		fetchBuilders.forEach(
//				(subFetchPath, fetchBuilder) -> fetchBuilder.buildFetch(
//						fetchParent,
//						subFetchPath,
//						jdbcResultsMetadata,
//						legacyFetchResolver,
//						creationState
//				)
//		);

		return parent.generateFetchableFetch(
				fetchable,
				fetchPath,
				FetchTiming.IMMEDIATE,
				true,
				null,
				creationState
		);
	}

	private void tableGroup(FetchParent parent, NavigablePath fetchPath, DomainResultCreationStateImpl creationStateImpl) {
		creationStateImpl.getFromClauseAccess().resolveTableGroup(
				fetchPath,
				navigablePath -> {
					final var parentTableGroup =
							creationStateImpl.getFromClauseAccess()
									.getTableGroup( parent.getNavigablePath() );
					final var tableGroupJoin = fetchable.createTableGroupJoin(
							fetchPath,
							parentTableGroup,
							null,
							null,
							SqlAstJoinType.INNER,
							true,
							false,
							creationStateImpl
					);
					parentTableGroup.addTableGroupJoin( tableGroupJoin );
					return tableGroupJoin.getJoinedGroup();
				}
		);
	}

	@Override
	public boolean equals(Object object) {
		if ( this == object ) {
			return true;
		}
		else if ( !( object instanceof ImplicitFetchBuilderEmbeddable that ) ) {
			return false;
		}
		else {
			return fetchPath.equals( that.fetchPath )
				&& fetchable.equals( that.fetchable )
				&& fetchBuilders.equals( that.fetchBuilders );
		}
	}

	@Override
	public int hashCode() {
		int result = fetchPath.hashCode();
		result = 31 * result + fetchable.hashCode();
		result = 31 * result + fetchBuilders.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "ImplicitFetchBuilderEmbeddable(" + fetchPath + ")";
	}

	@Override
	public void visitFetchBuilders(BiConsumer<Fetchable, FetchBuilder> consumer) {
		fetchBuilders.forEach( (k, v) -> consumer.accept( k, v ) );
	}
}
