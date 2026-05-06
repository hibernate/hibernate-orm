/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.audit.spi;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.hibernate.Session;
import org.hibernate.annotations.ChangesetEntity;
import org.hibernate.audit.EntityTrackingChangesetListener;
import org.hibernate.audit.ModificationType;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TransactionCompletionCallbacks;
import org.hibernate.persister.collection.CollectionPersister;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * Transaction-scoped queue for deferred audit row writes.
 * <p>
 * Mutation coordinators enqueue audit entries instead of writing
 * them inline during flush. Entries are keyed by
 * {@link EntityKey}. When the same entity is modified
 * multiple times within a transaction, entries are merged
 * according to the following merge rules:
 * <ul>
 *     <li>ADD + MOD -> ADD (with latest state)
 *     <li>ADD + DEL -> entries cancel out (no audit row)
 *     <li>MOD + MOD -> MOD (with latest state)
 *     <li>MOD + DEL -> DEL
 *     <li>DEL + ADD -> MOD (entity re-created with potentially different state)
 * </ul>
 * All audit rows (INSERT + optional REVEND UPDATE) are written
 * at {@code beforeTransactionCompletion}.
 *
 * @see AuditWriter
 * @since 7.4
 */
public class AuditWorkQueue implements TransactionCompletionCallbacks.BeforeCompletionCallback {

	/**
	 * A queued audit entry, holding the entity state and
	 * modification type, plus a writer callback.
	 */
	private static class QueuedEntry {
		Object entity;
		Object[] values;
		ModificationType modificationType;
		final AuditWriter writer;

		QueuedEntry(
				Object entity, Object[] values,
				ModificationType modificationType, AuditWriter writer) {
			this.entity = entity;
			this.values = values;
			this.modificationType = modificationType;
			this.writer = writer;
		}
	}

	/**
	 * A queued collection audit entry, holding the original snapshot
	 * captured before the first flush.
	 */
	private record QueuedCollectionEntry(
			PersistentCollection<?> collection,
			Object ownerId,
			Object originalSnapshot,
			CollectionAuditWriter writer) {
	}

	private final Map<EntityKey, QueuedEntry> entries = new LinkedHashMap<>();
	private final Map<CollectionKey, QueuedCollectionEntry> collectionEntries = new LinkedHashMap<>();
	private EntityTrackingChangesetListener trackingListener;
	private Object changesetEntity;
	private @Nullable Session changesetSession;
	private boolean registered;

	/**
	 * Enqueue an audit entry for deferred writing. If an entry
	 * for the same entity already exists, the entries are merged.
	 *
	 * @param entityKey the entity key (reused from the persistence context)
	 * @param entity the entity instance (may be null for delete)
	 * @param values the entity state
	 * @param modificationType the modification type (ADD/MOD/DEL)
	 * @param writer callback to perform the actual write
	 * @param session the current session
	 */
	public void enqueue(
			EntityKey entityKey,
			Object entity,
			Object[] values,
			ModificationType modificationType,
			AuditWriter writer,
			SharedSessionContractImplementor session) {
		if ( !registered ) {
			session.getTransactionCompletionCallbacks().registerCallback( this );
			trackingListener = resolveTrackingListener( session );
			registered = true;
		}

		final var existing = entries.get( entityKey );
		if ( existing == null ) {
			entries.put( entityKey, new QueuedEntry( entity, values, modificationType, writer ) );
		}
		else {
			merge( entityKey, existing, entity, values, modificationType );
		}
	}

	/**
	 * Enqueue a collection for deferred audit row writing.
	 * On first enqueue, the current snapshot is captured. Subsequent
	 * enqueues for the same collection are ignored; the diff will be
	 * computed at transaction completion against the original snapshot.
	 *
	 * @param collectionPersister the collection persister
	 * @param collection the persistent collection
	 * @param ownerId the owning entity's identifier
	 * @param originalSnapshot the collection snapshot before this flush
	 * @param writer callback to compute diff and write audit rows
	 * @param session the current session
	 */
	public void enqueueCollection(
			CollectionPersister collectionPersister,
			PersistentCollection<?> collection,
			Object ownerId,
			Object originalSnapshot,
			CollectionAuditWriter writer,
			SharedSessionContractImplementor session) {
		if ( !registered ) {
			session.getTransactionCompletionCallbacks().registerCallback( this );
			registered = true;
		}

		final var key = new CollectionKey( collectionPersister, ownerId );
		// Only store the first snapshot; subsequent flushes are ignored,
		// the diff at completion will use original vs final state
		collectionEntries.putIfAbsent(
				key,
				new QueuedCollectionEntry( collection, ownerId, originalSnapshot, writer )
		);
	}

	private void merge(
			EntityKey key,
			QueuedEntry existing,
			Object entity,
			Object[] newValues,
			ModificationType incoming) {
		final var merged = mergeModificationType( existing.modificationType, incoming );
		if ( merged == null ) {
			// ADD + DEL = cancel out, no entity audit row.
			// Collection entries may remain (orphaned)
			// and the orphaned rows are unreachable by any query.
			entries.remove( key );
		}
		else {
			existing.modificationType = merged;
			existing.values = newValues;
			if ( entity != null ) {
				existing.entity = entity;
			}
		}
	}

	/**
	 * Merge two modification types according to the merge matrix.
	 *
	 * @return the merged type, or {@code null} if the entries cancel out
	 */
	private static ModificationType mergeModificationType(
			ModificationType existing,
			ModificationType incoming) {
		return switch ( existing ) {
			case ADD -> switch ( incoming ) {
				case ADD -> ModificationType.ADD;
				case MOD -> ModificationType.ADD;   // ADD + MOD -> ADD (with latest state)
				case DEL -> null;                    // ADD + DEL -> cancel
			};
			case MOD -> switch ( incoming ) {
				case ADD -> ModificationType.MOD;
				case MOD -> ModificationType.MOD;   // MOD + MOD -> MOD (with latest state)
				case DEL -> ModificationType.DEL;   // MOD + DEL -> DEL
			};
			case DEL -> switch ( incoming ) {
				case ADD -> ModificationType.MOD;   // DEL + ADD -> MOD (re-created)
				case MOD, DEL -> ModificationType.DEL;
			};
		};
	}

	/**
	 * Store the changeset entity and the child session used to
	 * persist it. The child session is kept open for deferred
	 * flush of {@code @ElementCollection} changes (e.g.
	 * {@link ChangesetEntity.ModifiedEntities @ModifiedEntities}).
	 * Called from {@link ChangesetEntitySupplier#generateIdentifier}.
	 */
	public void setChangesetContext(Object changesetEntity, Session changesetSession) {
		this.changesetEntity = changesetEntity;
		this.changesetSession = changesetSession;
	}

	@Override
	public void doBeforeTransactionCompletion(SharedSessionContractImplementor session) {
		try {
			// Entity audit rows first
			for ( var mapEntry : entries.entrySet() ) {
				final var entityKey = mapEntry.getKey();
				final var entry = mapEntry.getValue();
				entry.writer.writeAuditRow(
						entityKey,
						entry.entity,
						entry.values,
						entry.modificationType,
						session
				);
				if ( trackingListener != null ) {
					trackingListener.entityChanged(
							entityKey.getPersister().getMappedClass(),
							entityKey.getIdentifier(),
							entry.modificationType,
							changesetEntity
					);
				}
			}
			// Collection audit rows (diff original snapshot vs final state)
			for ( var entry : collectionEntries.values() ) {
				entry.writer.writeCollectionAuditRows(
						entry.collection,
						entry.ownerId,
						entry.originalSnapshot,
						session
				);
			}
			// Populate @ModifiedEntities on the changeset entity
			populateModifiedEntityNames( session );
		}
		finally {
			entries.clear();
			collectionEntries.clear();
			trackingListener = null;
			changesetEntity = null;
			changesetSession = null;
			registered = false;
		}
	}

	private void populateModifiedEntityNames(SharedSessionContractImplementor session) {
		final var supplier = ChangesetEntitySupplier.resolve( session.getFactory().getServiceRegistry() );
		if ( supplier != null && supplier.getModifiedEntitiesProperty() != null ) {
			final var persister = session.getEntityPersister(
					supplier.getChangesetEntityClass().getName(),
					changesetEntity
			);
			final var attr = persister.findAttributeMapping( supplier.getModifiedEntitiesProperty() );
			//noinspection unchecked
			var entityNames = (Set<String>) persister.getValue( changesetEntity, attr.getStateArrayPosition() );
			if ( entityNames == null ) {
				entityNames = new HashSet<>();
				persister.setValue( changesetEntity, attr.getStateArrayPosition(), entityNames );
			}
			for ( var entityKey : entries.keySet() ) {
				entityNames.add( entityKey.getEntityName() );
			}
			castNonNull( changesetSession ).flush();
		}
	}

	private static EntityTrackingChangesetListener resolveTrackingListener(
			SharedSessionContractImplementor session) {
		final var supplier = ChangesetEntitySupplier.resolve( session.getFactory().getServiceRegistry() );
		if ( supplier != null && supplier.getListener() instanceof EntityTrackingChangesetListener etrl ) {
			return etrl;
		}
		return null;
	}
}
