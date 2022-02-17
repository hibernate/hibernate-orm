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
import org.hibernate.query.spi.NavigablePath;
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

		assert fetchedAttribute.getNotFoundAction() == null;

		this.keyResult = keyResult;
		this.selectByUniqueKey = selectByUniqueKey;
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
	public DomainResultAssembler<?> createAssembler(FetchParentAccess parentAccess, AssemblerCreationState creationState) {
		final EntityInitializer initializer = (EntityInitializer) creationState.resolveInitializer(
				getNavigablePath(),
				getFetchedMapping(),
				() -> {

					EntityPersister entityPersister = getReferencedMappingContainer().getEntityPersister();

					final ToOneAttributeMapping fetchedAttribute = (ToOneAttributeMapping) getFetchedMapping();
					if ( selectByUniqueKey ) {
						return new EntitySelectFetchByUniqueKeyInitializer(
								parentAccess,
								fetchedAttribute,
								getNavigablePath(),
								entityPersister,
								keyResult.createResultAssembler( parentAccess, creationState )
						);
					}
					if ( entityPersister.isBatchLoadable() ) {
						return new BatchEntitySelectFetchInitializer(
								parentAccess,
								fetchedAttribute,
								getNavigablePath(),
								entityPersister,
								keyResult.createResultAssembler( parentAccess, creationState )
						);
					}
					else {
						return new EntitySelectFetchInitializer(
								parentAccess,
								fetchedAttribute,
								getNavigablePath(),
								entityPersister,
								keyResult.createResultAssembler( parentAccess, creationState )
						);
					}
				}
		);

		return new EntityAssembler( getResultJavaType(), initializer );
	}
}
