/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.implicit;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.query.results.internal.Builders;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.internal.DomainResultCreationStateImpl;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.hibernate.internal.util.StringHelper.split;
import static org.hibernate.internal.util.collections.CollectionHelper.linkedMapOfSize;
import static org.hibernate.query.results.internal.ResultsHelper.impl;

/**
 * @author Steve Ebersole
 */
public class ImplicitFetchBuilderEntity implements ImplicitFetchBuilder {
	private final NavigablePath fetchPath;
	private final ToOneAttributeMapping fetchable;
	private final Map<Fetchable, FetchBuilder> fetchBuilders;

	public ImplicitFetchBuilderEntity(
			NavigablePath fetchPath,
			ToOneAttributeMapping fetchable,
			DomainResultCreationState creationState) {
		this.fetchPath = fetchPath;
		this.fetchable = fetchable;
		final DomainResultCreationStateImpl creationStateImpl = impl( creationState );
		final Function<Fetchable, FetchBuilder> fetchBuilderResolver = creationStateImpl.getCurrentExplicitFetchMementoResolver();
		final ForeignKeyDescriptor foreignKeyDescriptor = fetchable.getForeignKeyDescriptor();
		final String associationKeyPropertyName;
		final NavigablePath associationKeyFetchPath;
		final Fetchable associationKey;
		if ( fetchable.getReferencedPropertyName() == null ) {
			associationKeyPropertyName = fetchable.getEntityMappingType().getIdentifierMapping().getPartName();
			associationKey = (Fetchable) fetchable.findSubPart( associationKeyPropertyName );		}
		else {
			associationKeyPropertyName = fetchable.getReferencedPropertyName();
			String keyName = associationKeyPropertyName;
			for ( String part : split( ".", associationKeyPropertyName ) ) {
				keyName = part;
			}
			associationKey = (Fetchable) fetchable.findSubPart( keyName );
		}
		final FetchBuilder explicitAssociationKeyFetchBuilder =
				fetchBuilderResolver.apply( fetchable);
		if ( explicitAssociationKeyFetchBuilder == null ) {
			if ( foreignKeyDescriptor.getPartMappingType() instanceof EmbeddableMappingType embeddableType ) {
				fetchBuilders = fetchBuilderMap(
						fetchPath,
						fetchBuilderResolver,
						creationStateImpl,
						embeddableType
				);
			}
			else {
				fetchBuilders = emptyMap();
			}
		}
		else {
			fetchBuilders = singletonMap( associationKey, explicitAssociationKeyFetchBuilder );
		}
	}

	private static Map<Fetchable, FetchBuilder> fetchBuilderMap(
			NavigablePath fetchPath,
			Function<Fetchable, FetchBuilder> fetchBuilderResolver,
			DomainResultCreationStateImpl creationStateImpl,
			EmbeddableMappingType embeddableValuedModelPart) {
		final int size = embeddableValuedModelPart.getNumberOfFetchables();
		final Map<Fetchable, FetchBuilder> fetchBuilders = linkedMapOfSize( size );
		for ( int i = 0; i < size; i++ ) {
			final Fetchable subFetchable = embeddableValuedModelPart.getFetchable( i );
			final FetchBuilder explicitFetchBuilder = fetchBuilderResolver.apply( subFetchable );
			fetchBuilders.put( subFetchable,
					explicitFetchBuilder == null
							? Builders.implicitFetchBuilder( fetchPath, subFetchable, creationStateImpl )
							: explicitFetchBuilder );
		}
		return fetchBuilders;
	}

	private ImplicitFetchBuilderEntity(ImplicitFetchBuilderEntity original) {
		this.fetchPath = original.fetchPath;
		this.fetchable = original.fetchable;
		if ( original.fetchBuilders.isEmpty() ) {
			fetchBuilders = emptyMap();
		}
		else {
			fetchBuilders = new HashMap<>( original.fetchBuilders.size() );
			for ( Map.Entry<Fetchable, FetchBuilder> entry : original.fetchBuilders.entrySet() ) {
				fetchBuilders.put( entry.getKey(), entry.getValue().cacheKeyInstance() );
			}
		}
	}

	@Override
	public FetchBuilder cacheKeyInstance() {
		return new ImplicitFetchBuilderEntity( this );
	}

	@Override
	public Fetch buildFetch(
			FetchParent parent,
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			DomainResultCreationState creationState) {
		return parent.generateFetchableFetch(
				fetchable,
				fetchPath,
				fetchable.getMappedFetchOptions().getTiming(),
				false,
				null,
				creationState
		);
	}

	@Override
	public void visitFetchBuilders(BiConsumer<Fetchable, FetchBuilder> consumer) {
		fetchBuilders.forEach( (k, v) -> consumer.accept( k, v ) );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final ImplicitFetchBuilderEntity that = (ImplicitFetchBuilderEntity) o;
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
		return "ImplicitFetchBuilderEntity(" + fetchPath + ")";
	}
}
