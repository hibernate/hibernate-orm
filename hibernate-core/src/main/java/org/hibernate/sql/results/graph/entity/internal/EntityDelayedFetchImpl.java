/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.entity.internal;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.InitializerParent;
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
	public EntityInitializer<?> createInitializer(InitializerParent<?> parent, AssemblerCreationState creationState) {
		return new EntityDelayedFetchInitializer(
				parent,
				getNavigablePath(),
				getEntityValuedModelPart(),
				isSelectByUniqueKey(),
				getKeyResult(),
				getDiscriminatorFetch(),
				creationState
		);
	}
}
