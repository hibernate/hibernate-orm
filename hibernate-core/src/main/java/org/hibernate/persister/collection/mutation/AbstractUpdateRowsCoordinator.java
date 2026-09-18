/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import jakarta.annotation.Nonnull;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.AbstractCollectionPersister;

import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractUpdateRowsCoordinator implements UpdateRowsCoordinator {
	private final AbstractCollectionPersister mutationTarget;
	private final SessionFactoryImplementor sessionFactory;

	public AbstractUpdateRowsCoordinator(@Nonnull AbstractCollectionPersister mutationTarget, @Nonnull SessionFactoryImplementor sessionFactory) {
		this.mutationTarget = mutationTarget;
		this.sessionFactory = sessionFactory;
	}

	@Nonnull
	@Override
	public String toString() {
		return "UpdateRowsCoordinator(" + getMutationTarget().getRolePath() + ")";
	}

	@Nonnull
	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	@Nonnull
	@Override
	public AbstractCollectionPersister getMutationTarget() {
		// exposes AbstractCollectionPersister until we can drop MutationTarget in favor of GraphMutationTarget
		return mutationTarget;
	}

	@Override
	public void updateRows(@Nonnull Object key, @Nonnull PersistentCollection<?> collection, @Nonnull SharedSessionContractImplementor session) {
		MODEL_MUTATION_LOGGER.updatingCollectionRows( mutationTarget.getRolePath(), key );

		// update all the modified entries
		final int count = doUpdate( key, collection, session );

		MODEL_MUTATION_LOGGER.updatedCollectionRows( count, mutationTarget.getRolePath(), key );
	}

	protected abstract int doUpdate(@Nonnull Object key, @Nonnull PersistentCollection<?> collection, @Nonnull SharedSessionContractImplementor session);

	@Nonnull
	protected Object resolveDeleteRowValue(@Nonnull PersistentCollection<?> collection, @Nonnull Object entry, int entryPosition) {
		final var attributeMapping = getMutationTarget().getTargetPart();
		final var identifierDescriptor = attributeMapping.getIdentifierDescriptor();
		if ( identifierDescriptor != null ) {
			return collection.getIdentifier( entry, entryPosition );
		}
		else if ( getMutationTarget().hasPhysicalIndexColumn() && attributeMapping.getIndexDescriptor() != null ) {
			return collection.getIndex( entry, entryPosition, attributeMapping.getCollectionDescriptor() );
		}
		else {
			return collection.getSnapshotElement( entry, entryPosition );
		}
	}
}
