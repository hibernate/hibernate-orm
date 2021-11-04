/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import org.hibernate.engine.FetchTiming;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
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
	private final String sourceAlias;

	public EntityFetchJoinedImpl(
			FetchParent fetchParent,
			EntityValuedFetchable fetchedAttribute,
			TableGroup tableGroup,
			boolean nullable,
			NavigablePath navigablePath,
			DomainResultCreationState creationState) {
		super( fetchParent, fetchedAttribute, navigablePath, nullable );
		this.sourceAlias = tableGroup.getSourceAlias();
		this.entityResult = new EntityResultImpl(
				navigablePath,
				fetchedAttribute,
				tableGroup,
				null,
				creationState
		);
		this.entityResult.afterInitialize( this, creationState );
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
						creationState.determineEffectiveLockMode( sourceAlias ),
						entityResult.getIdentifierFetch(),
						entityResult.getDiscriminatorFetch(),
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

}
