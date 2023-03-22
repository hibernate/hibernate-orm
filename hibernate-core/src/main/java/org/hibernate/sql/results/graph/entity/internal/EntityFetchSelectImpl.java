/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Initializer;

/**
 * An eager entity fetch performed as a subsequent (n+1) select
 *
 * @author Andrea Boriero
 */
public class EntityFetchSelectImpl extends AbstractNonJoinedEntityFetch {
	private final DomainResult<?> keyResult;
	private final boolean selectByUniqueKey;

	public EntityFetchSelectImpl(
			FetchParent fetchParent,
			ToOneAttributeMapping fetchedAttribute,
			NavigablePath navigablePath,
			DomainResult<?> keyResult,
			boolean selectByUniqueKey,
			@SuppressWarnings("unused") DomainResultCreationState creationState) {
		super( navigablePath, fetchedAttribute, fetchParent );

		this.keyResult = keyResult;
		this.selectByUniqueKey = selectByUniqueKey;

	}

	/**
	 * For Hibernate Reactive
	 */
	protected EntityFetchSelectImpl(EntityFetchSelectImpl original) {
		super( original.getNavigablePath(), original.getFetchedMapping(), original.getFetchParent() );
		this.keyResult = original.keyResult;
		this.selectByUniqueKey = original.selectByUniqueKey;
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
	public DomainResultAssembler<?> createAssembler(
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		final Initializer initializer = creationState.resolveInitializer(
				getNavigablePath(),
				getFetchedMapping(),
				() -> buildEntitySelectFetchInitializer(
						parentAccess,
						(ToOneAttributeMapping) getFetchedMapping(),
						getReferencedMappingContainer().getEntityPersister(),
						keyResult,
						getNavigablePath(),
						selectByUniqueKey,
						creationState
				)
		);

		return buildEntityAssembler( initializer );
	}

	protected Initializer buildEntitySelectFetchInitializer(
			FetchParentAccess parentAccess,
			ToOneAttributeMapping fetchedMapping,
			EntityPersister entityPersister,
			DomainResult<?> keyResult,
			NavigablePath navigablePath,
			boolean selectByUniqueKey,
			AssemblerCreationState creationState) {
		return EntitySelectFetchInitializerBuilder.createInitializer(
				parentAccess,
				fetchedMapping,
				entityPersister,
				keyResult,
				navigablePath,
				selectByUniqueKey,
				creationState
		);
	}

	protected DomainResultAssembler<?> buildEntityAssembler(Initializer initializer) {
		return new EntityAssembler( getResultJavaType(), initializer.asEntityInitializer() );
	}
}
