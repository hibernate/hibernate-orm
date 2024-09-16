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

import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.results.Builders;
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import static org.hibernate.query.results.ResultsHelper.impl;

/**
 * @author Steve Ebersole
 */
public class ImplicitFetchBuilderEntity implements ImplicitFetchBuilder {
	private final NavigablePath fetchPath;
	private final ToOneAttributeMapping fetchable;
	private final Map<NavigablePath, FetchBuilder> fetchBuilders;

	public ImplicitFetchBuilderEntity(
			NavigablePath fetchPath,
			ToOneAttributeMapping fetchable,
			DomainResultCreationState creationState) {
		this.fetchPath = fetchPath;
		this.fetchable = fetchable;
		final DomainResultCreationStateImpl creationStateImpl = impl( creationState );
		final Map.Entry<String, NavigablePath> relativePath = creationStateImpl.getCurrentRelativePath();
		final Function<String, FetchBuilder> fetchBuilderResolver = creationStateImpl.getCurrentExplicitFetchMementoResolver();
		ForeignKeyDescriptor foreignKeyDescriptor = fetchable.getForeignKeyDescriptor();
		final String associationKeyPropertyName;
		final NavigablePath associationKeyFetchPath;
		if ( fetchable.getReferencedPropertyName() == null ) {
			associationKeyPropertyName = fetchable.getEntityMappingType().getIdentifierMapping().getPartName();
			associationKeyFetchPath = relativePath.getValue().append( associationKeyPropertyName );
		}
		else {
			associationKeyPropertyName = fetchable.getReferencedPropertyName();
			NavigablePath path = relativePath.getValue();
			for ( String part : StringHelper.split( ".", associationKeyPropertyName ) ) {
				path = path.append( part );
			}
			associationKeyFetchPath = path;
		}
		final FetchBuilder explicitAssociationKeyFetchBuilder = fetchBuilderResolver
				.apply( relativePath.getKey() + "." + associationKeyPropertyName );
		final Map<NavigablePath, FetchBuilder> fetchBuilders;
		if ( explicitAssociationKeyFetchBuilder == null ) {
			final MappingType partMappingType = foreignKeyDescriptor.getPartMappingType();
			if ( partMappingType instanceof EmbeddableMappingType ) {
				final EmbeddableMappingType embeddableValuedModelPart = (EmbeddableMappingType) partMappingType;
				final int size = embeddableValuedModelPart.getNumberOfFetchables();
				fetchBuilders = CollectionHelper.linkedMapOfSize( size );
				for ( int i = 0; i < size; i++ ) {
					final Fetchable subFetchable = embeddableValuedModelPart.getFetchable( i );
					final NavigablePath subFetchPath = associationKeyFetchPath.append( subFetchable.getFetchableName() );
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
			}
			else {
				fetchBuilders = Collections.emptyMap();
			}
		}
		else {
			fetchBuilders = Collections.singletonMap( associationKeyFetchPath, explicitAssociationKeyFetchBuilder );
		}
		this.fetchBuilders = fetchBuilders;
	}

	private ImplicitFetchBuilderEntity(ImplicitFetchBuilderEntity original) {
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
		return new ImplicitFetchBuilderEntity( this );
	}

	@Override
	public Fetch buildFetch(
			FetchParent parent,
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState creationState) {
		final Fetch fetch = parent.generateFetchableFetch(
				fetchable,
				fetchPath,
				fetchable.getMappedFetchOptions().getTiming(),
				false,
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
	public void visitFetchBuilders(BiConsumer<String, FetchBuilder> consumer) {
		fetchBuilders.forEach( (k, v) -> consumer.accept( k.getLocalName(), v ) );
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
