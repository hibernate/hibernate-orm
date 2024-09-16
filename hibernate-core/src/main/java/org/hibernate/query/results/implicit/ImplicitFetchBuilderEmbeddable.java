/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.implicit;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.engine.FetchTiming;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.query.results.Builders;
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.embeddable.EmbeddableValuedFetchable;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import static org.hibernate.query.results.ResultsHelper.impl;

/**
 * @author Steve Ebersole
 */
public class ImplicitFetchBuilderEmbeddable implements ImplicitFetchBuilder {
	private final NavigablePath fetchPath;
	private final EmbeddableValuedFetchable fetchable;
	private final Map<NavigablePath, FetchBuilder> fetchBuilders;

	public ImplicitFetchBuilderEmbeddable(
			NavigablePath fetchPath,
			EmbeddableValuedFetchable fetchable,
			DomainResultCreationState creationState) {
		this.fetchPath = fetchPath;
		this.fetchable = fetchable;
		final DomainResultCreationStateImpl creationStateImpl = impl( creationState );
		final Map.Entry<String, NavigablePath> relativePath = creationStateImpl.getCurrentRelativePath();
		final Function<String, FetchBuilder> fetchBuilderResolver = creationStateImpl.getCurrentExplicitFetchMementoResolver();
		final int size = fetchable.getNumberOfFetchables();
		final Map<NavigablePath, FetchBuilder> fetchBuilders = CollectionHelper.linkedMapOfSize( size );
		for ( int i = 0; i < size; i++ ) {
			final Fetchable subFetchable = fetchable.getFetchable( i );
			final NavigablePath subFetchPath = relativePath.getValue().append( subFetchable.getFetchableName() );
			final FetchBuilder explicitFetchBuilder = fetchBuilderResolver.apply( subFetchPath.getFullPath() );
			if ( explicitFetchBuilder == null ) {
				fetchBuilders.put(
						subFetchPath,
						Builders.implicitFetchBuilder( fetchPath, subFetchable, creationStateImpl )
				);
			}
			else {
				fetchBuilders.put( subFetchPath, explicitFetchBuilder );
			}
		}
		this.fetchBuilders = fetchBuilders;
	}

	private ImplicitFetchBuilderEmbeddable(ImplicitFetchBuilderEmbeddable original) {
		this.fetchPath = original.fetchPath;
		this.fetchable = original.fetchable;
		final Map<NavigablePath, FetchBuilder> fetchBuilders;
		if ( original.fetchBuilders.isEmpty() ) {
			fetchBuilders = Collections.emptyMap();
		}
		else {
			fetchBuilders = new HashMap<>( original.fetchBuilders.size() );
			for ( Map.Entry<NavigablePath, FetchBuilder> entry : original.fetchBuilders.entrySet() ) {
				fetchBuilders.put( entry.getKey(), entry.getValue().cacheKeyInstance() );
			}
		}
		this.fetchBuilders = fetchBuilders;
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
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState creationState) {
		final DomainResultCreationStateImpl creationStateImpl = impl( creationState );

		final TableGroup tableGroup = creationStateImpl.getFromClauseAccess().resolveTableGroup(
				fetchPath,
				navigablePath -> {
					final TableGroup parentTableGroup = creationStateImpl
							.getFromClauseAccess()
							.getTableGroup( parent.getNavigablePath() );
					final TableGroupJoin tableGroupJoin = fetchable.createTableGroupJoin(
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

		final Fetch fetch = parent.generateFetchableFetch(
				fetchable,
				fetchPath,
				FetchTiming.IMMEDIATE,
				true,
				null,
				creationState
		);
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

		return fetch;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final ImplicitFetchBuilderEmbeddable that = (ImplicitFetchBuilderEmbeddable) o;
		return fetchPath.equals( that.fetchPath )
				&& fetchable.equals( that.fetchable )
				&& fetchBuilders.equals( that.fetchBuilders );
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
	public void visitFetchBuilders(BiConsumer<String, FetchBuilder> consumer) {
		fetchBuilders.forEach( (k, v) -> consumer.accept( k.getLocalName(), v ) );
	}
}
