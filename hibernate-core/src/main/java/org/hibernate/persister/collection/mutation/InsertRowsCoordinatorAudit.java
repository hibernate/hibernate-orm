/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.hibernate.audit.ModificationType;
import org.hibernate.audit.spi.AuditWorkQueue;
import org.hibernate.audit.spi.CollectionAuditWriter;
import org.hibernate.collection.spi.PersistentArrayHolder;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;

/**
 * InsertRowsCoordinator for audited collections.
 */
public class InsertRowsCoordinatorAudit implements InsertRowsCoordinator, CollectionAuditWriter {
	private final CollectionMutationTarget mutationTarget;
	private final InsertRowsCoordinator currentInsertCoordinator;
	private final SessionFactoryImplementor sessionFactory;
	private final MutationExecutorService mutationExecutorService;
	private final BasicBatchKey auditBatchKey;
	private final boolean[] indexColumnIsSettable;
	private final boolean[] elementColumnIsSettable;
	private final UnaryOperator<Object> indexIncrementer;

	private AuditCollectionHelper auditHelper;

	public InsertRowsCoordinatorAudit(
			CollectionMutationTarget mutationTarget,
			InsertRowsCoordinator currentInsertCoordinator,
			boolean[] indexColumnIsSettable,
			boolean[] elementColumnIsSettable,
			UnaryOperator<Object> indexIncrementer,
			SessionFactoryImplementor sessionFactory) {
		this.mutationTarget = mutationTarget;
		this.currentInsertCoordinator = currentInsertCoordinator;
		this.sessionFactory = sessionFactory;
		this.indexColumnIsSettable = indexColumnIsSettable;
		this.elementColumnIsSettable = elementColumnIsSettable;
		this.indexIncrementer = indexIncrementer;
		this.auditBatchKey = new BasicBatchKey( mutationTarget.getRolePath() + "#AUDIT_INSERT" );
		this.mutationExecutorService = sessionFactory.getServiceRegistry().getService( MutationExecutorService.class );
	}

	@Override
	public CollectionMutationTarget getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public void insertRows(
			PersistentCollection<?> collection,
			Object id,
			EntryFilter entryChecker,
			SharedSessionContractImplementor session) {
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

	private Object resolveSnapshot(
			PersistentCollection<?> collection,
			Object id,
			SharedSessionContractImplementor session) {
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
			PersistentCollection<?> collection,
			Object id,
			Object originalSnapshot,
			SharedSessionContractImplementor session) {
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
			if ( originalSnapshot == null ) {
				// New collection: write ADD for all current entries
				final var entries = collection.entries( collectionDescriptor );
				int entryCount = 0;
				while ( entries.hasNext() ) {
					final Object entry = entries.next();
					bindings.bindInsertValues(
							collection,
							id,
							entry,
							entryCount,
							ModificationType.ADD,
							session,
							mutationExecutor.getJdbcValueBindings()
					);
					mutationExecutor.execute( entry, null, null, null, session );
					entryCount++;
				}
			}
			else {
				// Diff original snapshot vs final collection state
				final var changes = computeCollectionChanges( collection, collectionDescriptor, originalSnapshot );
				// Close previous rows' transaction end for elements being removed/replaced
				updateElementTransactionEnd( collection, id, changes, session );
				// Write ADD/DEL audit rows
				for ( var change : changes ) {
					bindings.bindInsertValues(
							collection,
							id,
							change.rawEntry,
							change.position,
							change.modificationType,
							session,
							mutationExecutor.getJdbcValueBindings()
					);
					mutationExecutor.execute( change.rawEntry, null, null, null, session );
				}
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
			PersistentCollection<?> collection,
			Object ownerId,
			List<AuditChange> changes,
			SharedSessionContractImplementor session) {
		final var updateGroup = getAuditHelper().getTransactionEndUpdateGroup();
		if ( updateGroup == null ) {
			return;
		}
		final var mutationExecutor = mutationExecutorService.createExecutor(
				() -> auditBatchKey,
				updateGroup,
				session
		);
		try {
			final var tableName = getAuditHelper().getAuditTableMapping().getTableName();
			final var txId = session.getCurrentChangesetIdentifier();
			final var auditMapping = mutationTarget.getTargetPart().getAuditMapping();
			final var collectionTableName = mutationTarget.getCollectionTableMapping().getTableName();
			final var revEndMapping = auditMapping.getInvalidatingChangesetIdMapping( collectionTableName );
			final var bindings = getAuditHelper().getRowMutationHelper();

			// Update REVEND for ALL changes (not just DEL) to cover both removed and replaced elements
			for ( var change : changes ) {
				final var jdbcValueBindings = mutationExecutor.getJdbcValueBindings();

				// SET REVEND = :txId
				jdbcValueBindings.bindValue(
						txId,
						tableName,
						revEndMapping.getSelectionExpression(),
						ParameterUsage.SET
				);

				// SET REVEND_TSTMP = :tstmp (if configured)
				final var revEndTsMapping = auditMapping.getInvalidationTimestampMapping( collectionTableName );
				if ( revEndTsMapping != null ) {
					jdbcValueBindings.bindValue(
							java.time.Instant.now(),
							tableName,
							revEndTsMapping.getSelectionExpression(),
							ParameterUsage.SET
					);
				}

				// WHERE: bind key + index/element columns (same as INSERT but RESTRICT)
				bindings.bindRestrictValues(
						collection,
						ownerId,
						change.rawEntry,
						change.position,
						session,
						jdbcValueBindings
				);

				// 0 rows is valid (element might not have a previous audit row)
				mutationExecutor.execute( null, null, null, (s, c, b) -> true, session );
			}
		}
		finally {
			mutationExecutor.release();
		}
	}

	/**
	 * An audit change to write: the raw entry, its position, and the modification type.
	 */
	private record AuditChange(Object rawEntry, int position, ModificationType modificationType) {
	}

	/**
	 * Compute the set of ADD/DEL changes between the collection's snapshot and current
	 * state.
	 * <p>
	 * For indexed collections (maps, lists with {@code @OrderColumn}), uses direct
	 * snapshot lookups by index/key. For non-indexed collections (sets, bags), uses
	 * linear scan.
	 */
	private List<AuditChange> computeCollectionChanges(
			PersistentCollection<?> collection,
			CollectionPersister collectionDescriptor,
			Object snapshot) {
		final Type elementType = collectionDescriptor.getElementType();
		if ( collectionDescriptor.hasIndex() ) {
			return snapshot instanceof Map<?, ?> ?
					computeMapChanges( collection, collectionDescriptor, (Map<?, ?>) snapshot, elementType ) :
					computeListChanges( collection, collectionDescriptor, snapshot, elementType );
		}
		else {
			// Non-indexed (sets, bags): extract snapshot elements into a mutable list
			final Collection<?> snapshotElements = snapshot instanceof Map<?, ?> snapshotMap
					? snapshotMap.values()
					: (Collection<?>) snapshot;
			return computeUnindexedChanges( collection, collectionDescriptor, snapshotElements, elementType );
		}
	}

	/**
	 * Diff for maps: direct lookup by key in snapshot.
	 *
	 * @implNote Uses {@code Map.get()} for O(1) key lookup rather than linear scan. Safe because the map's
	 * own contract requires {@code equals()}/{@code hashCode()} consistency for keys, if a
	 * key is in the map, {@code get()} must find it.
	 */
	private List<AuditChange> computeMapChanges(
			PersistentCollection<?> collection,
			CollectionPersister collectionDescriptor,
			Map<?, ?> snapshot,
			Type elementType) {
		final List<AuditChange> changes = new ArrayList<>();
		final var currentMap = (Map<?, ?>) collection;

		// Current entries not matching snapshot: ADD
		final var entries = collection.entries( collectionDescriptor );
		int i = 0;
		while ( entries.hasNext() ) {
			final var entry = (Map.Entry<?, ?>) entries.next();
			if ( entry.getValue() != null ) {
				final Object snapshotValue = snapshot.get( entry.getKey() );
				if ( snapshotValue == null || !elementType.isSame( entry.getValue(), snapshotValue ) ) {
					changes.add( new AuditChange( entry, i, ModificationType.ADD ) );
				}
			}
			i++;
		}

		// Snapshot entries not in current (or value changed): DEL
		for ( var entry : snapshot.entrySet() ) {
			if ( entry.getValue() != null ) {
				final Object currentValue = currentMap.get( entry.getKey() );
				if ( currentValue == null || !elementType.isSame( entry.getValue(), currentValue ) ) {
					changes.add( new AuditChange( entry, i++, ModificationType.DEL ) );
				}
			}
		}

		return changes;
	}

	/**
	 * Diff for indexed lists and arrays: positional comparison against the snapshot.
	 */
	private List<AuditChange> computeListChanges(
			PersistentCollection<?> collection,
			CollectionPersister collectionDescriptor,
			Object snapshot,
			Type elementType) {
		final List<AuditChange> changes = new ArrayList<>();
		final List<?> snapshotList = snapshot instanceof List<?> list ? list : null;
		final int snapshotSize = snapshotList != null ? snapshotList.size() : Array.getLength( snapshot );

		final var entries = collection.entries( collectionDescriptor );
		int i = 0;
		while ( entries.hasNext() ) {
			final Object current = collection.getElement( entries.next() );
			final Object old = i < snapshotSize ? ( snapshotList != null ? snapshotList.get( i ) : Array.get(
					snapshot,
					i
			) ) : null;
			final boolean same = current != null && old != null && elementType.isSame( current, old );
			if ( current != null && !same ) {
				changes.add( new AuditChange( current, i, ModificationType.ADD ) );
			}
			if ( old != null && !same ) {
				changes.add( new AuditChange( old, i, ModificationType.DEL ) );
			}
			i++;
		}

		// Snapshot positions beyond current size are all DELs
		for ( ; i < snapshotSize; i++ ) {
			final Object old = snapshotList != null ? snapshotList.get( i ) : Array.get( snapshot, i );
			if ( old != null ) {
				changes.add( new AuditChange( old, i, ModificationType.DEL ) );
			}
		}

		return changes;
	}

	/**
	 * Diff for non-indexed collections (sets, bags): linear scan with mutable snapshot copy.
	 */
	private List<AuditChange> computeUnindexedChanges(
			PersistentCollection<?> collection,
			CollectionPersister collectionDescriptor,
			Collection<?> snapshotElements,
			Type elementType) {
		final var remaining = new ArrayList<>( snapshotElements );
		final List<AuditChange> changes = new ArrayList<>();

		final var entries = collection.entries( collectionDescriptor );
		int i = 0;
		while ( entries.hasNext() ) {
			final Object element = collection.getElement( entries.next() );
			if ( element != null ) {
				boolean matched = false;
				for ( var it = remaining.iterator(); it.hasNext(); ) {
					if ( elementType.isSame( element, it.next() ) ) {
						it.remove();
						matched = true;
						break;
					}
				}
				if ( !matched ) {
					changes.add( new AuditChange( element, i, ModificationType.ADD ) );
				}
			}
			i++;
		}

		for ( var element : remaining ) {
			changes.add( new AuditChange( element, i++, ModificationType.DEL ) );
		}

		return changes;
	}

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
