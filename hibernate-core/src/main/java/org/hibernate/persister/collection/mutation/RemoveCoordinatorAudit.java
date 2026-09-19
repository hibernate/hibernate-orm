/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import jakarta.annotation.Nonnull;

import java.util.function.UnaryOperator;

import jakarta.annotation.Nullable;
import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationTarget;
import org.hibernate.audit.ModificationType;
import org.hibernate.audit.spi.CollectionAuditWriter;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * RemoveCoordinator for audited collections.
 * Delegates the bulk removal to the standard coordinator, then writes
 * DEL audit rows to the collection's audit table for each entry that
 * was in the collection.
 */
public class RemoveCoordinatorAudit implements RemoveCoordinator, CollectionAuditWriter {
	private final RemoveCoordinator standardCoordinator;
	private final CollectionMutationTarget mutationTarget;
	private final SessionFactoryImplementor sessionFactory;
	private final BasicBatchKey auditBatchKey;
	private final MutationExecutorService mutationExecutorService;
	@Nullable
	private final boolean[] indexColumnIsSettable;
	private final boolean[] elementColumnIsSettable;
	private final UnaryOperator<Object> indexIncrementer;

	@Nullable
	private AuditCollectionHelper auditHelper;

	public RemoveCoordinatorAudit(
			@Nonnull CollectionMutationTarget mutationTarget,
			@Nonnull RemoveCoordinator standardCoordinator,
			@Nullable boolean[] indexColumnIsSettable,
			@Nonnull boolean[] elementColumnIsSettable,
			@Nonnull UnaryOperator<Object> indexIncrementer,
			@Nonnull SessionFactoryImplementor sessionFactory) {
		this.mutationTarget = mutationTarget;
		this.standardCoordinator = standardCoordinator;
		this.sessionFactory = sessionFactory;
		this.indexColumnIsSettable = indexColumnIsSettable;
		this.elementColumnIsSettable = elementColumnIsSettable;
		this.indexIncrementer = indexIncrementer;
		this.auditBatchKey = new BasicBatchKey( mutationTarget.getRolePath() + "#AUDIT_REMOVE" );
		this.mutationExecutorService = sessionFactory.getServiceRegistry()
				.getService( MutationExecutorService.class );
	}

	@Nonnull
	@Override
	public CollectionMutationTarget getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public @Nullable String getSqlString() {
		return standardCoordinator.getSqlString();
	}

	@Override
	public void deleteAllRows(@Nonnull Object key, @Nonnull SharedSessionContractImplementor session) {
		final var collectionDescriptor = mutationTarget.getTargetPart().getCollectionDescriptor();

		// Get the collection from the persistence context BEFORE bulk removal
		final var collectionKey = new CollectionKey( collectionDescriptor, key );
		final var collection = session.getPersistenceContextInternal().getCollection( collectionKey );
		if ( collection != null && !collection.wasInitialized() ) {
			collection.forceInitialization();
		}

		// Delegate to standard coordinator
		standardCoordinator.deleteAllRows( key, session );

		// Only write DEL audit rows for entity deletion (all snapshot elements are truly removed)
		if ( collection != null && isEntityDeletion( key, session ) ) {
			// Enqueue with snapshot = null to signal "write DEL for all current entries"
			session.getAuditWorkQueue().enqueueCollection(
					collectionDescriptor,
					collection,
					key,
					null,
					this,
					session
			);
		}
	}

	@Override
	public void writeCollectionAuditRows(
			@Nonnull PersistentCollection<?> collection,
			@Nonnull Object ownerId,
			@Nonnull Object originalSnapshot,
			@Nonnull SharedSessionContractImplementor session) {
		final var operationGroup = getAuditHelper().getAuditInsertOperationGroup();
		if ( operationGroup == null ) {
			return;
		}

		final var collectionDescriptor = mutationTarget.getTargetPart().getCollectionDescriptor();
		final var mutationExecutor = mutationExecutorService.createExecutor(
				() -> auditBatchKey,
				operationGroup,
				session
		);
		try {
			final var bindings = getAuditHelper().getRowMutationHelper();
			final var entries = collection.entries( collectionDescriptor );
			int entryCount = 0;
			while ( entries.hasNext() ) {
				final Object entry = entries.next();
				bindings.bindInsertValues(
						collection,
						ownerId,
						entry,
						entryCount,
						ModificationType.DEL,
						session,
						mutationExecutor.getJdbcValueBindings()
				);
				mutationExecutor.execute( entry, null, null, null, session );
				entryCount++;
			}
		}
		finally {
			mutationExecutor.release();
		}
	}

	private boolean isEntityDeletion(@Nonnull Object key, @Nonnull SharedSessionContractImplementor session) {
		final var pc = session.getPersistenceContextInternal();
		final var collectionDescriptor = mutationTarget.getTargetPart().getCollectionDescriptor();
		final var owner = pc.getCollectionOwner( key, collectionDescriptor );
		if ( owner != null ) {
			final var ownerEntry = pc.getEntry( owner );
			return ownerEntry != null && ownerEntry.getStatus().isDeletedOrGone();
		}
		return true;
	}

	@Nonnull
	private AuditCollectionHelper getAuditHelper() {
		if ( auditHelper == null ) {
			auditHelper = new AuditCollectionHelper(
					mutationTarget,
					sessionFactory,
					indexColumnIsSettable,
					elementColumnIsSettable,
					indexIncrementer,
					mutationTarget.getTargetPart().getAuditMapping()
			);
		}
		return auditHelper;
	}
}
