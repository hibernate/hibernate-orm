/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.metamodel.mapping.DiscriminatorValueDetails;
import org.hibernate.metamodel.mapping.internal.DiscriminatedAssociationMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.InitializerProducer;
import org.hibernate.sql.results.graph.entity.AbstractDiscriminatedEntityResultGraphNode;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Join fetching of {@link org.hibernate.annotations.Any} mappings.
 *
 * @author Gavin King
 */
public class JoinedDiscriminatedEntityFetch extends AbstractDiscriminatedEntityResultGraphNode implements Fetch,
		InitializerProducer<JoinedDiscriminatedEntityFetch> {
	private final FetchTiming fetchTiming;
	private final FetchParent fetchParent;
	private final List<EntityResultImpl<?>> concreteEntityResults;

	public JoinedDiscriminatedEntityFetch(
			NavigablePath navigablePath,
			JavaType<?> baseAssociationJtd,
			DiscriminatedAssociationModelPart fetchedPart,
			FetchTiming fetchTiming,
			FetchParent fetchParent,
			List<DiscriminatorValueDetails> valueDetails,
			DomainResultCreationState creationState) {
		super( navigablePath, fetchedPart, baseAssociationJtd );
		this.fetchTiming = fetchTiming;
		this.fetchParent = fetchParent;

		afterInitialize( creationState );

		final var fromClauseAccess = creationState.getSqlAstCreationState().getFromClauseAccess();
		this.concreteEntityResults = new ArrayList<>( valueDetails.size() );
		for ( DiscriminatorValueDetails valueDetail : valueDetails ) {
			final var entityMapping = valueDetail.getIndicatedEntity();
			final NavigablePath concretePath = DiscriminatedAssociationMapping.concreteEntityPath( navigablePath, entityMapping );
			final TableGroup tableGroup = fromClauseAccess.getTableGroup( concretePath );
			final var entityResult = new EntityResultImpl<>( concretePath, entityMapping, tableGroup, null );
			entityResult.afterInitialize( entityResult, creationState );
			concreteEntityResults.add( entityResult );
		}
	}

	List<EntityResultImpl<?>> getConcreteEntityResults() {
		return concreteEntityResults;
	}

	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public DiscriminatedAssociationModelPart getFetchedMapping() {
		return getReferencedMappingContainer();
	}

	@Override
	public FetchTiming getTiming() {
		return fetchTiming;
	}

	@Override
	public boolean hasTableGroup() {
		return true;
	}

	@Override
	public DomainResultAssembler<?> createAssembler(
			InitializerParent<?> parent,
			AssemblerCreationState creationState) {
		return new EntityAssembler<>(
				getReferencedMappingContainer().getJavaType(),
				creationState.resolveInitializer( this, parent, this ).asEntityInitializer()
		);
	}

	@Override
	public Initializer<?> createInitializer(
			JoinedDiscriminatedEntityFetch resultGraphNode,
			InitializerParent<?> parent,
			AssemblerCreationState creationState) {
		return resultGraphNode.createInitializer( parent, creationState );
	}

	@Override
	public Initializer<?> createInitializer(InitializerParent<?> parent, AssemblerCreationState creationState) {
		return new JoinedDiscriminatedEntityInitializer(
				parent,
				getReferencedMappingType(),
				getNavigablePath(),
				getDiscriminatorValueFetch(),
				getKeyValueFetch(),
				fetchTiming == FetchTiming.IMMEDIATE,
				false,
				concreteEntityResults,
				creationState
		);
	}

	@Override
	public boolean containsCollectionFetches() {
		for ( var entityResult : concreteEntityResults ) {
			if ( entityResult.containsCollectionFetches() ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void collectValueIndexesToCache(BitSet valueIndexes) {
		super.collectValueIndexesToCache( valueIndexes );
		for ( var entityResult : concreteEntityResults ) {
			entityResult.collectValueIndexesToCache( valueIndexes );
		}
	}

	@Override
	public FetchParent asFetchParent() {
		return this;
	}
}
