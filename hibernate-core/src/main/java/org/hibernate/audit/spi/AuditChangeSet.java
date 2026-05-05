/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.audit.spi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.audit.ModificationType;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.persister.collection.CollectionPersister;

/// Transaction-scoped audit change set with shared audit merge semantics.
///
/// This type deliberately knows nothing about how audit work is executed. It
/// only records entity and collection audit changes and merges repeated entity
/// changes for the same [EntityKey]. Legacy audit infrastructure may
/// consume the resulting changes by invoking legacy writers directly, while the
/// graph action queue may consume the same merged changes by materializing graph
/// flush operations.
///
/// Entity changes are merged because there can only be one audit row for a given
/// entity key in a transaction. Collection changes are keyed by collection role
/// and owner id and are recorded once, preserving the first snapshot captured for
/// the collection during the transaction.
///
/// @param <E> the execution-specific entity audit handler carried with entity changes
/// @param <C> the execution-specific collection audit handler carried with collection changes
///
/// @author Marco Belladelli
/// @author Steve Ebersole
///
/// @since 8.0
public class AuditChangeSet<E, C> {
	private final Map<EntityKey, MutableEntityChange<E>> entityChanges = new LinkedHashMap<>();
	private final Map<CollectionKey, CollectionChange<C>> collectionChanges = new LinkedHashMap<>();

	/// Add or merge an entity audit change.
	///
	/// If this is the first change for `entityKey`, it is stored as-is.
	/// Otherwise, the existing and incoming modification types are collapsed using
	/// audit semantics: for example ADD followed by MOD remains ADD, MOD followed
	/// by DEL becomes DEL, and ADD followed by DEL removes the audit change.
	public void addEntityChange(
			EntityKey entityKey,
			Object entity,
			Object[] values,
			ModificationType modificationType,
			E entityAuditHandler) {
		final MutableEntityChange<E> existing = entityChanges.get( entityKey );
		if ( existing == null ) {
			entityChanges.put( entityKey, new MutableEntityChange<>( entity, values, modificationType, entityAuditHandler ) );
		}
		else {
			merge( entityKey, existing, entity, values, modificationType );
		}
	}

	/// Add a collection audit change if one has not already been captured.
	///
	/// The first collection change wins because it carries the original snapshot
	/// needed to compute the audit row diff at transaction completion. Later
	/// collection changes for the same role and owner id are represented by the
	/// collection wrapper's final state, not by replacing the stored snapshot.
	public void addCollectionChange(
			CollectionPersister collectionPersister,
			PersistentCollection<?> collection,
			Object ownerId,
			Object originalSnapshot,
			C collectionAuditHandler) {
		final var key = new CollectionKey( collectionPersister, ownerId );
		collectionChanges.putIfAbsent(
				key,
				new CollectionChange<>( collection, ownerId, originalSnapshot, collectionAuditHandler )
		);
	}

	/// Return the merged entity audit changes in encounter order.
	public List<EntityChange<E>> entityChanges() {
		final List<EntityChange<E>> changes = new ArrayList<>( entityChanges.size() );
		for ( var entry : entityChanges.entrySet() ) {
			final MutableEntityChange<E> change = entry.getValue();
			changes.add( new EntityChange<>(
					entry.getKey(),
					change.entity,
					change.values,
					change.modificationType,
					change.entityAuditHandler
			) );
		}
		return changes;
	}

	/// Return the recorded collection audit changes in encounter order.
	public List<CollectionChange<C>> collectionChanges() {
		return List.copyOf( collectionChanges.values() );
	}

	public boolean isEmpty() {
		return entityChanges.isEmpty() && collectionChanges.isEmpty();
	}

	public void clear() {
		entityChanges.clear();
		collectionChanges.clear();
	}

	private void merge(
			EntityKey entityKey,
			MutableEntityChange<E> existing,
			Object entity,
			Object[] newValues,
			ModificationType incoming) {
		final ModificationType merged = mergeModificationType( existing.modificationType, incoming );
		if ( merged == null ) {
			entityChanges.remove( entityKey );
		}
		else {
			existing.modificationType = merged;
			existing.values = newValues;
			if ( entity != null ) {
				existing.entity = entity;
			}
		}
	}

	private static ModificationType mergeModificationType(
			ModificationType existing,
			ModificationType incoming) {
		return switch ( existing ) {
			case ADD -> switch ( incoming ) {
				case ADD, MOD -> ModificationType.ADD;
				case DEL -> null;
			};
			case MOD -> switch ( incoming ) {
				case ADD -> ModificationType.ADD;
				case MOD -> ModificationType.MOD;
				case DEL -> ModificationType.DEL;
			};
			case DEL -> switch ( incoming ) {
				case ADD -> ModificationType.MOD;
				case MOD, DEL -> ModificationType.DEL;
			};
		};
	}

	/// A merged entity audit change.
	///
	/// @param entityKey the persistence-context key of the audited entity
	/// @param entity the entity instance, when still available
	/// @param values the entity state to write into the audit row
	/// @param modificationType the merged audit modification type
	/// @param entityAuditHandler the execution-specific handler used to consume this change
	/// @param <E> the entity audit handler type
	public record EntityChange<E>(
			EntityKey entityKey,
			Object entity,
			Object[] values,
			ModificationType modificationType,
			E entityAuditHandler) {
	}

	/// A collection audit change.
	///
	/// The change stores the original snapshot captured before flush replaces it.
	/// Consumers compare that snapshot with the collection wrapper's final state
	/// to produce row-level ADD and DEL audit mutations.
	///
	/// @param collection the collection wrapper whose final state will be audited
	/// @param ownerId the collection owner identifier
	/// @param originalSnapshot the original collection snapshot, or `null` for a new collection
	/// @param collectionAuditHandler the execution-specific handler used to consume this change
	/// @param <C> the collection audit handler type
	public record CollectionChange<C>(
			PersistentCollection<?> collection,
			Object ownerId,
			Object originalSnapshot,
			C collectionAuditHandler) {
	}

	private static class MutableEntityChange<E> {
		Object entity;
		Object[] values;
		ModificationType modificationType;
		final E entityAuditHandler;

		private MutableEntityChange(
				Object entity,
				Object[] values,
				ModificationType modificationType,
				E entityAuditHandler) {
			this.entity = entity;
			this.values = values;
			this.modificationType = modificationType;
			this.entityAuditHandler = entityAuditHandler;
		}
	}
}
