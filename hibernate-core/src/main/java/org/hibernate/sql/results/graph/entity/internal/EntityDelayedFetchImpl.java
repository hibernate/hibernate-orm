/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.entity.EntityInitializer;

/**
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
public class EntityDelayedFetchImpl extends AbstractNonJoinedEntityFetch {

	private final DomainResult keyResult;

	public EntityDelayedFetchImpl(
			FetchParent fetchParent,
			ToOneAttributeMapping fetchedAttribute,
			NavigablePath navigablePath,
			DomainResult keyResult) {
		super( navigablePath, fetchedAttribute, fetchParent );
		this.keyResult = keyResult;
	}

	@Override
	public FetchTiming getTiming() {
		return FetchTiming.DELAYED;
	}

	@Override
	public boolean hasTableGroup() {
		return false;
	}

	@Override
	public DomainResultAssembler createAssembler(
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		final NavigablePath navigablePath = getNavigablePath();
		final EntityInitializer entityInitializer = (EntityInitializer) creationState.resolveInitializer(
				navigablePath,
				getEntityValuedModelPart(),
				() -> new EntityDelayedFetchInitializer(
						navigablePath,
						(ToOneAttributeMapping) getEntityValuedModelPart(),
						keyResult.createResultAssembler( creationState )
				)
		);

		return new EntityAssembler( getFetchedMapping().getJavaTypeDescriptor(), entityInitializer );
	}
}
