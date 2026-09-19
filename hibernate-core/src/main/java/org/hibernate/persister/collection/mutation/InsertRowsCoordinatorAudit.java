/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.function.UnaryOperator;

import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationTarget;
import org.hibernate.audit.spi.AuditWorkQueue;
import org.hibernate.audit.spi.CollectionAuditWriter;
import org.hibernate.collection.spi.PersistentArrayHolder;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.mutation.CollectionAuditSupport.AuditCollectionChange;

/**
 * InsertRowsCoordinator for audited collections.
 */
public class InsertRowsCoordinatorAudit implements InsertRowsCoordinator, CollectionAuditWriter {
	private final CollectionMutationTarget mutationTarget;
	private final InsertRowsCoordinator currentInsertCoordinator;
	private final SessionFactoryImplementor sessionFactory;
	private final MutationExecutorService mutationExecutorService;
	private final BasicBatchKey auditBatchKey;
	@Nullable
	private final boolean[] indexColumnIsSettable;
	private final boolean[] elementColumnIsSettable;
	private final UnaryOperator<Object> indexIncrementer;

	@Nullable
	private CollectionAuditSupport auditMutationSupport;

	public InsertRowsCoordinatorAudit(
			@Nonnull CollectionMutationTarget mutationTarget,
			@Nonnull InsertRowsCoordinator currentInsertCoordinator,
			@Nullable boolean[] indexColumnIsSettable,
			@Nonnull boolean[] elementColumnIsSettable,
			@Nonnull UnaryOperator<Object> indexIncrementer,
			@Nonnull SessionFactoryImplementor sessionFactory) {
		this.mutationTarget = mutationTarget;
		this.currentInsertCoordinator = currentInsertCoordinator;
		this.sessionFactory = sessionFactory;
		this.indexColumnIsSettable = indexColumnIsSettable;
		this.elementColumnIsSettable = elementColumnIsSettable;
		this.indexIncrementer = indexIncrementer;
		this.auditBatchKey = new BasicBatchKey( mutationTarget.getRolePath() + "#AUDIT_INSERT" );
		this.mutationExecutorService = sessionFactory.getServiceRegistry().getService( MutationExecutorService.class );
	}

	@Nonnull
	@Override
	public CollectionMutationTarget getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public void insertRows(
			@Nonnull PersistentCollection<?> collection,
			@Nonnull Object id,
			@Nullable EntryFilter entryChecker,
			@Nonnull SharedSessionContractImplementor session) {
		currentInsertCoordinator.insertRows( collection, id, entryChecker, session );

		// Capture the snapshot before it's replaced by the flush, and enqueue
		final var snapshot = resolveSnapshot( collection, id, session );
		final var collectionDescriptor = mutationTarget.getTargetPart().getCollectionDescriptor();
		session.getAuditWorkQueue().enqueueCollection(
				collectionDescriptor,
				collection,
				id,
				snapshot,
				this,
				session
		);
	}

	@Nullable
	private Object resolveSnapshot(
			@Nonnull PersistentCollection<?> collection,
			@Nonnull Object id,
			@Nonnull SharedSessionContractImplementor session) {
		final var persistenceContext = session.getPersistenceContextInternal();
		final var collectionEntry = persistenceContext.getCollectionEntry( collection );
		if ( collectionEntry != null && collectionEntry.getLoadedPersister() != null ) {
			return collection.getStoredSnapshot();
		}
		else if ( collection instanceof PersistentArrayHolder<?> ) {
			// Array holders are always newly wrapped references, need to retrieve the old instance from the PC
			final var collectionDescriptor = mutationTarget.getTargetPart().getCollectionDescriptor();
			final var oldCollection = persistenceContext.getCollection( new CollectionKey( collectionDescriptor, id ) );
			return oldCollection != null ? oldCollection.getStoredSnapshot() : null;
		}
		return null;
	}

	/**
	 * Called by {@link AuditWorkQueue} at transaction completion.
	 * Computes the diff between the original snapshot and the current
	 * collection state, then writes ADD/DEL audit rows.
	 */
	@Override
	public void writeCollectionAuditRows(
			@Nonnull PersistentCollection<?> collection,
			@Nonnull Object id,
			@Nonnull Object originalSnapshot,
			@Nonnull SharedSessionContractImplementor session) {
		final var auditMutationSupport = getAuditMutationSupport();
		final var operationGroup = auditMutationSupport.getAuditInsertOperationGroup();
		if ( operationGroup == null ) {
			return;
		}

		final var mutationExecutor = mutationExecutorService.createExecutor(
				() -> auditBatchKey,
				operationGroup,
				session
		);

		try {
			final var changes = auditMutationSupport.resolveChanges( collection, originalSnapshot );
			updateElementTransactionEnd( collection, id, changes, session );
			for ( var change : changes ) {
				auditMutationSupport.bindAuditInsertValues(
						collection,
						id,
						change,
						session,
						mutationExecutor.getJdbcValueBindings()
				);
				mutationExecutor.execute( change.rawEntry(), null, null, null, session );
			}
		}
		finally {
			mutationExecutor.release();
		}
	}

	/**
	 * For each DEL entry in the diff, update the corresponding previous
	 * audit row's REVEND to mark it as superseded.
	 */
	private void updateElementTransactionEnd(
			@Nonnull PersistentCollection<?> collection,
			@Nonnull Object ownerId,
			@Nonnull List<AuditCollectionChange> changes,
			@Nonnull SharedSessionContractImplementor session) {
		final var auditMutationSupport = getAuditMutationSupport();
		final var updateGroup = auditMutationSupport.getTransactionEndUpdateGroup();
		if ( updateGroup == null ) {
			return;
		}
		final var mutationExecutor = mutationExecutorService.createExecutor(
				() -> auditBatchKey,
				updateGroup,
				session
		);
		try {
			// Update REVEND for ALL changes (not just DEL) to cover both removed and replaced elements
			for ( var change : changes ) {
				auditMutationSupport.bindTransactionEndValues(
						collection,
						ownerId,
						change,
						session,
						mutationExecutor.getJdbcValueBindings()
				);

				// 0 rows is valid (element might not have a previous audit row)
				mutationExecutor.execute( null, null, null, (s, c, b) -> true, session );
			}
		}
		finally {
			mutationExecutor.release();
		}
	}

	@Nonnull
	private CollectionAuditSupport getAuditMutationSupport() {
		if ( auditMutationSupport == null ) {
			auditMutationSupport = new CollectionAuditSupport(
					mutationTarget,
					sessionFactory,
					indexColumnIsSettable,
					elementColumnIsSettable,
					indexIncrementer,
					mutationTarget.getTargetPart().getAuditMapping()
			);
		}
		return auditMutationSupport;
	}
}
