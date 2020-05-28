/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.internal.SingularAssociationAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.entity.EntityInitializer;

/**
 * An eager entity fetch performed as a subsequent (n+1) select
 *
 * @author Andrea Boriero
 */
public class EntityFetchSelectImpl extends AbstractNonJoinedEntityFetch {
	private final boolean nullable;
	private final DomainResult result;

	public EntityFetchSelectImpl(
			FetchParent fetchParent,
			SingularAssociationAttributeMapping fetchedAttribute,
			LockMode lockMode,
			boolean nullable,
			NavigablePath navigablePath,
			DomainResult result,
			DomainResultCreationState creationState) {
		super( navigablePath, fetchedAttribute, fetchParent );
		this.nullable = nullable;
		this.result = result;
	}

	@Override
	public FetchTiming getTiming() {
		return FetchTiming.IMMEDIATE;
	}

	@Override
	public boolean hasTableGroup() {
		return false;
	}

	@Override
	public DomainResultAssembler createAssembler(FetchParentAccess parentAccess, AssemblerCreationState creationState) {
		final EntityInitializer initializer = (EntityInitializer) creationState.resolveInitializer(
				getNavigablePath(),
				() -> new EntitySelectFetchInitializer(
						getNavigablePath(),
						getReferencedMappingContainer().getEntityPersister(),
						result.createResultAssembler( creationState ),
						nullable
				)
		);

		return new EntityAssembler( getResultJavaTypeDescriptor(), initializer );
	}
}
