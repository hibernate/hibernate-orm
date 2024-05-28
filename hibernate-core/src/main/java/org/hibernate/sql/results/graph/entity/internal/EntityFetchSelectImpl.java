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
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.entity.EntityInitializer;

/**
 * An eager entity fetch performed as a subsequent (n+1) select
 *
 * @author Andrea Boriero
 */
public class EntityFetchSelectImpl extends AbstractNonJoinedEntityFetch {

	public EntityFetchSelectImpl(
			FetchParent fetchParent,
			ToOneAttributeMapping fetchedAttribute,
			NavigablePath navigablePath,
			DomainResult<?> keyResult,
			boolean selectByUniqueKey,
			DomainResultCreationState creationState) {
		super( navigablePath, fetchedAttribute, fetchParent, keyResult, false, selectByUniqueKey, creationState );
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
	}

	@Override
	public FetchTiming getTiming() {
		return FetchTiming.IMMEDIATE;
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
				creationState
		);
	}
}
