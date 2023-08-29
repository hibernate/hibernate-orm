/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import org.hibernate.LockMode;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.entity.AbstractNonLazyEntityFetch;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.entity.EntityResultGraphNode;
import org.hibernate.sql.results.graph.entity.EntityValuedFetchable;

/**
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
public class EntityFetchJoinedImpl extends AbstractNonLazyEntityFetch {
	private final EntityResultImpl entityResult;
	private final DomainResult<?> keyResult;
	private final NotFoundAction notFoundAction;

	private final String sourceAlias;

	public EntityFetchJoinedImpl(
			FetchParent fetchParent,
			ToOneAttributeMapping toOneMapping,
			TableGroup tableGroup,
			DomainResult<?> keyResult,
			NavigablePath navigablePath,
			DomainResultCreationState creationState) {
		super( fetchParent, toOneMapping, navigablePath );
		this.keyResult = keyResult;
		this.notFoundAction = toOneMapping.getNotFoundAction();
		this.sourceAlias = tableGroup.getSourceAlias();

		this.entityResult = new EntityResultImpl(
				navigablePath,
				toOneMapping,
				tableGroup,
				null
		);

		this.entityResult.afterInitialize( this, creationState );
	}

	public EntityFetchJoinedImpl(
			FetchParent fetchParent,
			EntityCollectionPart collectionPart,
			TableGroup tableGroup,
			NavigablePath navigablePath,
			DomainResultCreationState creationState) {
		super( fetchParent, collectionPart, navigablePath );
		this.notFoundAction = null;
		this.keyResult = null;
		this.sourceAlias = tableGroup.getSourceAlias();

		this.entityResult = new EntityResultImpl(
				navigablePath,
				collectionPart,
				tableGroup,
				null
		);

		this.entityResult.afterInitialize( this, creationState );
	}

	/**
	 * For Hibernate Reactive
	 */
	protected EntityFetchJoinedImpl(EntityFetchJoinedImpl original ) {
		super( original.getFetchParent(), original.getReferencedModePart(), original.getNavigablePath() );
		this.entityResult = original.entityResult;
		this.keyResult = original.keyResult;
		this.notFoundAction = original.notFoundAction;
		this.sourceAlias = original.sourceAlias;
	}

	@Override
	protected EntityInitializer getEntityInitializer(
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		return creationState.resolveInitializer(
				getNavigablePath(),
				getReferencedModePart(),
				() -> buildEntityJoinedFetchInitializer(
						entityResult,
						getReferencedModePart(),
						getNavigablePath(),
						creationState.determineEffectiveLockMode( sourceAlias ),
						notFoundAction,
						keyResult,
						entityResult.getRowIdResult(),
						entityResult.getIdentifierFetch(),
						entityResult.getDiscriminatorFetch(),
						creationState
				)
		).asEntityInitializer();
	}

	/**
	 * For Hibernate Reactive
	 */
	protected Initializer buildEntityJoinedFetchInitializer(
			EntityResultGraphNode resultDescriptor,
			EntityValuedFetchable referencedFetchable,
			NavigablePath navigablePath,
			LockMode lockMode,
			NotFoundAction notFoundAction,
			DomainResult<?> keyResult,
			DomainResult<Object> rowIdResult,
			Fetch identifierFetch,
			Fetch discriminatorFetch,
			AssemblerCreationState creationState) {
		return new EntityJoinedFetchInitializer(
				resultDescriptor,
				referencedFetchable,
				navigablePath,
				lockMode,
				notFoundAction,
				keyResult,
				rowIdResult,
				identifierFetch,
				discriminatorFetch,
				creationState
		);
	}

	@Override
	public FetchTiming getTiming() {
		return FetchTiming.IMMEDIATE;
	}

	@Override
	public boolean hasTableGroup() {
		return true;
	}

	@Override
	public boolean containsCollectionFetches() {
		return entityResult.containsCollectionFetches();
	}

	public EntityResultImpl getEntityResult() {
		return entityResult;
	}

}
