/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchTiming;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.entity.AbstractNonLazyEntityFetch;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.entity.EntityValuedFetchable;

/**
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
public class EntityFetchJoinedImpl extends AbstractNonLazyEntityFetch {

	private final EntityResultImpl entityResult;
	private final LockMode lockMode;

	public EntityFetchJoinedImpl(
			FetchParent fetchParent,
			EntityValuedFetchable fetchedAttribute,
			LockMode lockMode,
			boolean nullable,
			NavigablePath navigablePath,
			DomainResultCreationState creationState) {
		super( fetchParent, fetchedAttribute, navigablePath, nullable );
		this.lockMode = lockMode;
		entityResult = new EntityResultImpl(
				navigablePath,
				fetchedAttribute,
				null,
				creationState
		);
	}

	@Override
	protected EntityInitializer getEntityInitializer(
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		return (EntityInitializer) creationState.resolveInitializer(
				getNavigablePath(),
				getEntityValuedModelPart(),
				() -> new EntityJoinedFetchInitializer(
						entityResult,
						getReferencedModePart(),
						getNavigablePath(),
						lockMode,
						entityResult.getIdentifierResult(),
						entityResult.getDiscriminatorResult(),
						entityResult.getVersionResult(),
						creationState
				)
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

	public EntityResultImpl getEntityResult() {
		return entityResult;
	}

	public LockMode getLockMode() {
		return lockMode;
	}
}
