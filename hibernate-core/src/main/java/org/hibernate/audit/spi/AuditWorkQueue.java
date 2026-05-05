/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.audit.spi;

import java.util.HashSet;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.hibernate.Session;
import org.hibernate.annotations.Changelog;
import org.hibernate.audit.EntityTrackingChangesetListener;
import org.hibernate.audit.ModificationType;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TransactionCompletionCallbacks;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.mutation.AuditMutationWriter;

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
	private final AuditChangeSet<AuditWriter, CollectionAuditWriter> changeSet = new AuditChangeSet<>();
	private EntityTrackingChangesetListener trackingListener;
	private Object changelog;
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
		changeSet.addEntityChange( entityKey, entity, values, modificationType, writer );
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
		// Only store the first snapshot; subsequent flushes are ignored,
		// the diff at completion will use original vs final state
		changeSet.addCollectionChange( collectionPersister, collection, ownerId, originalSnapshot, writer );

		final var ownerPersister = collectionPersister.getOwnerEntityPersister();
		if ( ownerPersister.getAuditMapping() != null ) {
			final var ownerEntityKey = session.generateEntityKey( ownerId, ownerPersister );
			if ( trackingListener == null ) {
				trackingListener = resolveTrackingListener( session );
			}
			final var persistenceContextOwner =
					session.getPersistenceContextInternal().getCollectionOwner( ownerId, collectionPersister );
			final var collectionOwner = persistenceContextOwner != null ? persistenceContextOwner : collection.getOwner();
			final var owner = collectionOwner != null
					? collectionOwner
					: session.getPersistenceContextInternal().getEntity( ownerEntityKey );
			if ( owner != null ) {
				changeSet.addEntityChange(
						ownerEntityKey,
						owner,
						ownerPersister.getValues( owner ),
						ModificationType.MOD,
						new AuditMutationWriter( ownerPersister, session.getFactory() )
				);
			}
		}
	}

	/**
	 * Store the changelog entity and the child session used to
	 * persist it. The child session is kept open for deferred
	 * flush of {@code @ElementCollection} changes (e.g.
	 * {@link Changelog.ModifiedEntities @ModifiedEntities}).
	 * Called from {@link ChangelogSupplier#generateIdentifier}.
	 */
	public void setChangesetContext(Object changelog, Session changesetSession) {
		this.changelog = changelog;
		this.changesetSession = changesetSession;
	}

	@Override
	public void doBeforeTransactionCompletion(SharedSessionContractImplementor session) {
		try {
			// Entity audit rows first
			for ( var entry : changeSet.entityChanges() ) {
				final var entityKey = entry.entityKey();
				entry.entityAuditHandler().writeAuditRow(
						entityKey,
						entry.entity(),
						entry.values(),
						entry.modificationType(),
						session
				);
				if ( trackingListener != null ) {
					trackingListener.entityChanged(
							entityKey.getPersister().getMappedClass(),
							entityKey.getIdentifier(),
							entry.modificationType(),
							changelog
					);
				}
			}
			// Collection audit rows (diff original snapshot vs final state)
			for ( var entry : changeSet.collectionChanges() ) {
				entry.collectionAuditHandler().writeCollectionAuditRows(
						entry.collection(),
						entry.ownerId(),
						entry.originalSnapshot(),
						session
				);
			}
			// Populate @ModifiedEntities on the changelog entity
			populateModifiedEntityNames( session );
		}
		finally {
			changeSet.clear();
			trackingListener = null;
			changelog = null;
			changesetSession = null;
			registered = false;
		}
	}

	private void populateModifiedEntityNames(SharedSessionContractImplementor session) {
		final var supplier = ChangelogSupplier.resolve( session.getFactory().getServiceRegistry() );
		if ( supplier != null && supplier.getModifiedEntitiesProperty() != null ) {
			final var persister = session.getEntityPersister(
					supplier.getChangelogClass().getName(),
					changelog
			);
			final var attr = persister.findAttributeMapping( supplier.getModifiedEntitiesProperty() );
			//noinspection unchecked
			var entityNames = (Set<String>) persister.getValue( changelog, attr.getStateArrayPosition() );
			if ( entityNames == null ) {
				entityNames = new HashSet<>();
				persister.setValue( changelog, attr.getStateArrayPosition(), entityNames );
			}
			for ( var change : changeSet.entityChanges() ) {
				entityNames.add( change.entityKey().getEntityName() );
			}
			castNonNull( changesetSession ).flush();
		}
	}

	private static EntityTrackingChangesetListener resolveTrackingListener(
			SharedSessionContractImplementor session) {
		final var supplier = ChangelogSupplier.resolve( session.getFactory().getServiceRegistry() );
		if ( supplier != null && supplier.getListener() instanceof EntityTrackingChangesetListener etrl ) {
			return etrl;
		}
		return null;
	}
}
