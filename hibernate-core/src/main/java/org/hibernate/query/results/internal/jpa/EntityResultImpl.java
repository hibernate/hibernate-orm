/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.jpa;

import org.hibernate.annotations.NotFoundAction;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.query.results.spi.FetchBuilder;
import org.hibernate.query.results.spi.FetchBuilderBasicValued;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.FetchableContainer;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.graph.entity.internal.EntityAssembler;
import org.hibernate.sql.results.graph.entity.internal.EntityInitializerImpl;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.descriptor.java.JavaType;

import java.util.Map;

/// [EntityResult] implementation for handling [jakarta.persistence.sql.EntityMapping].
///
/// @see EntityBuilder
///
/// @author Steve Ebersole
class EntityResultImpl<E> implements EntityResult<E> {
	private final EntityMappingType entityDescriptor;

	private final JavaType<E> javaType;
	private final NavigablePath navigablePath;

	private final Fetch identifierFetch;
	private final BasicFetch<?> discriminatorFetch;
	private final ImmutableFetchList fetches;

	public EntityResultImpl(
			EntityMappingType entityDescriptor,
			JavaType<E> javaType,
			NavigablePath navigablePath,
			FetchBuilder identifierFetchBuilder,
			FetchBuilderBasicValued discriminatorFetchBuilder,
			Map<String, FetchBuilder> attributeFetchBuilders,
			JdbcValuesMetadata jdbcResultsMetadata,
			DomainResultCreationState creationState) {
		this.entityDescriptor = entityDescriptor;
		this.javaType = javaType;
		this.navigablePath = navigablePath;

		this.identifierFetch = identifierFetchBuilder.buildFetch(
				this,
				navigablePath.append( entityDescriptor.getIdentifierMapping().getPartName() ),
				jdbcResultsMetadata,
				creationState
		);

		this.discriminatorFetch = discriminatorFetchBuilder == null
				? null
				: discriminatorFetchBuilder.buildFetch(
						this,
						navigablePath.append( entityDescriptor.getDiscriminatorMapping().getPartName() ),
						jdbcResultsMetadata,
						creationState
				);

		final ImmutableFetchList.Builder fetchCollector = new ImmutableFetchList.Builder( entityDescriptor );
		entityDescriptor.forEachAttributeMapping( (position, attribute) -> {
			final FetchBuilder fetchBuilder = attributeFetchBuilders.get( attribute.getAttributeName() );
			if ( fetchBuilder != null ) {
				fetchCollector.add( fetchBuilder.buildFetch(
						this,
						navigablePath.append( attribute.getPartName() ),
						jdbcResultsMetadata,
						creationState
				) );
			}
			else {
				fetchCollector.add( attribute.generateFetch(
						this,
						navigablePath.append( attribute.getPartName() ),
						attribute.getMappedFetchOptions().getTiming(),
						false,
						null,
						creationState
				) );
			}
		} );
		this.fetches = fetchCollector.build();
	}

	@Override
	public DomainResultAssembler<E> createResultAssembler(
			InitializerParent<?> parent,
			AssemblerCreationState creationState) {
		return new EntityAssembler<>(
				getResultJavaType(),
				creationState.resolveInitializer( navigablePath, entityDescriptor,
								() -> createInitializer( parent, creationState ) )
						.asEntityInitializer()
		);
	}

	@Override
	public Initializer<?> createInitializer(
			InitializerParent<?> parent,
			AssemblerCreationState creationState) {
		return new EntityInitializerImpl(
				this,
				getResultVariable(),
				identifierFetch,
				discriminatorFetch,
				null,
				null,
				NotFoundAction.EXCEPTION,
				false,
				null,
				true,
				creationState
		);
	}

	@Override
	public JavaType<E> getResultJavaType() {
		return javaType;
	}

	@Override
	public String getResultVariable() {
		return "";
	}

	@Override
	public FetchableContainer getReferencedMappingType() {
		return entityDescriptor;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public ImmutableFetchList getFetches() {
		return fetches;
	}

	@Override
	public Fetch findFetch(Fetchable fetchable) {
		return fetches.get( fetchable );
	}

	@Override
	public boolean hasJoinFetches() {
		return fetches.hasJoinFetches();
	}

	@Override
	public boolean containsCollectionFetches() {
		return fetches.containsCollectionFetches();
	}

	@Override
	public EntityValuedModelPart getEntityValuedModelPart() {
		return entityDescriptor;
	}
}
