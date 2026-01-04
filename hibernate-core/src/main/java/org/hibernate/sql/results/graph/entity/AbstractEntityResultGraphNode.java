/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.entity;

import java.util.BitSet;

import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.AbstractFetchParent;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchableContainer;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.type.descriptor.java.JavaType;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * AbstractFetchParent sub-class for entity-valued graph nodes
 *
 * @author Steve Ebersole
 */
public abstract class AbstractEntityResultGraphNode extends AbstractFetchParent implements EntityResultGraphNode {
	private @Nullable Fetch identifierFetch;
	private BasicFetch<?> discriminatorFetch;
	private DomainResult<Object> rowIdResult;
	private final EntityValuedModelPart fetchContainer;

	public AbstractEntityResultGraphNode(EntityValuedModelPart referencedModelPart, NavigablePath navigablePath) {
		super( navigablePath );
		this.fetchContainer = referencedModelPart;
	}

	@Override
	public void afterInitialize(FetchParent fetchParent, DomainResultCreationState creationState) {
		final var navigablePath = getNavigablePath();
		final var entityTableGroup =
				creationState.getSqlAstCreationState().getFromClauseAccess()
						.getTableGroup( navigablePath );

		final var entityResultGraphNode = (EntityResultGraphNode) fetchParent;
		identifierFetch = identifierFetch( creationState, entityResultGraphNode, navigablePath );
		discriminatorFetch = creationState.visitDiscriminatorFetch( entityResultGraphNode );

		rowIdResult = rowIdResult( creationState, navigablePath, entityTableGroup );

		super.afterInitialize( fetchParent, creationState );
	}

	private DomainResult<Object> rowIdResult(
			DomainResultCreationState creationState,
			NavigablePath navigablePath,
			TableGroup entityTableGroup) {
		final var rowIdMapping =
				getEntityValuedModelPart().getEntityMappingType()
						.getRowIdMapping();
		if ( rowIdMapping == null ) {
			return null;
		}
		else {
			return rowIdMapping.createDomainResult(
					navigablePath.append( rowIdMapping.getRowIdName() ),
					entityTableGroup,
					AbstractEntityPersister.ROWID_ALIAS,
					creationState
			);
		}
	}

	private Fetch identifierFetch(
			DomainResultCreationState creationState,
			EntityResultGraphNode entityResultGraphNode,
			NavigablePath navigablePath) {
		final var idFetch = creationState.visitIdentifierFetch( entityResultGraphNode );
		if ( navigablePath.getParent() == null
				&& !creationState.forceIdentifierSelection()
				&& ( idFetch.asFetchParent() == null || !idFetch.asFetchParent().containsCollectionFetches() ) ) {
			return null;
		}
		else {
			return idFetch;
		}
	}

	@Override
	public EntityMappingType getReferencedMappingContainer() {
		return getEntityValuedModelPart().getEntityMappingType();
	}

	@Override
	public EntityValuedModelPart getEntityValuedModelPart() {
		return this.fetchContainer;
	}

	@Override
	public FetchableContainer getFetchContainer() {
		return this.fetchContainer;
	}

	@Override
	public JavaType<?> getResultJavaType() {
		return getEntityValuedModelPart().getEntityMappingType().getMappedJavaType();
	}

	public @Nullable Fetch getIdentifierFetch() {
		return identifierFetch;
	}

	public BasicFetch<?> getDiscriminatorFetch() {
		return discriminatorFetch;
	}

	public DomainResult<Object> getRowIdResult() {
		return rowIdResult;
	}

	@Override
	public void collectValueIndexesToCache(BitSet valueIndexes) {
		final var entityPersister = fetchContainer.getEntityMappingType().getEntityPersister();
		if ( identifierFetch != null ) {
			identifierFetch.collectValueIndexesToCache( valueIndexes );
		}
		if ( !entityPersister.useShallowQueryCacheLayout() ) {
			if ( discriminatorFetch != null ) {
				discriminatorFetch.collectValueIndexesToCache( valueIndexes );
			}
			if ( rowIdResult != null ) {
				rowIdResult.collectValueIndexesToCache( valueIndexes );
			}
			super.collectValueIndexesToCache( valueIndexes );
		}
		else if ( entityPersister.storeDiscriminatorInShallowQueryCacheLayout()
					&& discriminatorFetch != null ) {
			discriminatorFetch.collectValueIndexesToCache( valueIndexes );
		}
	}

}
