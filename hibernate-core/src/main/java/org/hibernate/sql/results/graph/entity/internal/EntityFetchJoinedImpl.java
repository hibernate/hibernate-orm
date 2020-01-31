/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.sql.results.graph.entity.AbstractNonLazyEntityFetch;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.entity.EntityValuedFetchable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Initializer;

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
			Consumer<Initializer> collector,
			AssemblerCreationState creationState) {
		return new EntityInitializerJoinedFetch(
				entityResult,
				getNavigablePath(),
				lockMode,
				entityResult.getIdentifierResult(),
				entityResult.getDiscriminatorResult(),
				entityResult.getVersionResult(),
				collector,
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

}
