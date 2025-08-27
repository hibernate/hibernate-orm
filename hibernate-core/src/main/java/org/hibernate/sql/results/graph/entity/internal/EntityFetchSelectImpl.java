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
 * An eager entity fetch performed as a subsequent (n+1) select
 *
 * @author Andrea Boriero
 */
public class EntityFetchSelectImpl extends AbstractNonJoinedEntityFetch {

	private final boolean isAffectedByFilter;

	public EntityFetchSelectImpl(
			FetchParent fetchParent,
			ToOneAttributeMapping fetchedAttribute,
			NavigablePath navigablePath,
			DomainResult<?> keyResult,
			boolean selectByUniqueKey,
			boolean isAffectedByFilter,
			DomainResultCreationState creationState) {
		super( navigablePath, fetchedAttribute, fetchParent, keyResult, false, selectByUniqueKey, creationState );
		this.isAffectedByFilter = isAffectedByFilter;
	}

	/**
	 * For Hibernate Reactive
	 */
	protected EntityFetchSelectImpl(EntityFetchSelectImpl original) {
		super(
				original.getNavigablePath(),
				original.getFetchedMapping(),
				original.getFetchParent(),
				original.getKeyResult(),
				original.getDiscriminatorFetch(),
				original.isSelectByUniqueKey()
		);
		this.isAffectedByFilter = original.isAffectedByFilter();
	}

	@Override
	public FetchTiming getTiming() {
		return FetchTiming.IMMEDIATE;
	}

	public boolean isAffectedByFilter() {
		return isAffectedByFilter;
	}

	@Override
	public EntityInitializer<?> createInitializer(InitializerParent<?> parent, AssemblerCreationState creationState) {
		return EntitySelectFetchInitializerBuilder.createInitializer(
				parent,
				getFetchedMapping(),
				getReferencedMappingContainer().getEntityPersister(),
				getKeyResult(),
				getNavigablePath(),
				isSelectByUniqueKey(),
				isAffectedByFilter(),
				creationState
		);
	}
}
