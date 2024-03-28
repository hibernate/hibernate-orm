/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.basic.BasicResultAssembler;
import org.hibernate.sql.results.graph.entity.EntityInitializer;

/**
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
public class EntityDelayedFetchImpl extends AbstractNonJoinedEntityFetch {
	public EntityDelayedFetchImpl(
			FetchParent fetchParent,
			ToOneAttributeMapping fetchedAttribute,
			NavigablePath navigablePath,
			DomainResult<?> keyResult,
			boolean selectByUniqueKey,
			DomainResultCreationState creationState) {
		super(
				navigablePath,
				fetchedAttribute,
				fetchParent,
				keyResult,
				fetchedAttribute.getEntityMappingType().getEntityPersister().isConcreteProxy(),
				selectByUniqueKey,
				creationState
		);
	}

	@Override
	public FetchTiming getTiming() {
		return FetchTiming.DELAYED;
	}

	@Override
	public EntityInitializer createInitializer(FetchParentAccess parentAccess, AssemblerCreationState creationState) {
		return new EntityDelayedFetchInitializer(
				parentAccess,
				getNavigablePath(),
				getEntityValuedModelPart(),
				isSelectByUniqueKey(),
				getKeyResult().createResultAssembler( parentAccess, creationState ),
				getDiscriminatorFetch() != null
						? (BasicResultAssembler<?>) getDiscriminatorFetch().createResultAssembler( parentAccess, creationState )
						: null
		);
	}
}
