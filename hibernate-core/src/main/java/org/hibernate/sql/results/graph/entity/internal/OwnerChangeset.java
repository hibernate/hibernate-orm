package org.hibernate.sql.results.graph.entity.internal;

import jakarta.annotation.Nullable;

import org.hibernate.collection.spi.AbstractPersistentCollection;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import static org.hibernate.metamodel.mapping.internal.CurrentState.hasHistory;

/**
 * Resolves an association, read by an initializer in an all-changesets session,
 * at the changeset of the snapshot which owns it, or as current state.
 */
public final class OwnerChangeset {
	private OwnerChangeset() {
	}

	/**
	 * Performs the action at the changeset where a to-one with the given target resolves.
	 * In an all-changesets session that is the changeset of the snapshot that owns it
	 * when the target has history, and current state otherwise, since a current version,
	 * or an entity without history, only has current versions to refer to. Without an
	 * owner, as in the load of a snapshot, it resolves where the load reads.
	 */
	static void resolveAtOwnerChangeset(
			@Nullable InitializerParent<?> parent,
			EntityMappingType target,
			RowProcessingState rowProcessingState,
			Runnable action) {
		resolveAtOwnerChangeset( parent, hasHistory( target ), rowProcessingState, action );
	}

	/**
	 * Performs the action at the changeset where an association resolves: in an
	 * all-changesets session, the changeset of the snapshot owning it when the
	 * association resolves per snapshot, and current state otherwise. Without an
	 * owner, it resolves where the load reads.
	 */
	public static void resolveAtOwnerChangeset(
			@Nullable Initializer<?> parent,
			boolean perSnapshot,
			RowProcessingState rowProcessingState,
			Runnable action) {
		final var influencers = rowProcessingState.getSession().getLoadQueryInfluencers();
		final var owner = influencers.isAllChangesetsSession() ? owningInitializer( parent ) : null;
		if ( owner != null ) {
			influencers.withTemporalIdentifier(
					perSnapshot ? changesetId( owner, rowProcessingState ) : null,
					action
			);
		}
		else {
			action.run();
		}
	}

	/**
	 * The nearest entity or collection initializer holding an association.
	 */
	private static @Nullable Initializer<?> owningInitializer(@Nullable Initializer<?> parent) {
		for ( var initializer = parent; initializer != null; initializer = initializer.getParent() ) {
			if ( initializer instanceof EntityInitializerImpl || initializer instanceof CollectionInitializer<?> ) {
				return initializer;
			}
		}
		return null;
	}

	/**
	 * The changeset of the snapshot held by the given entity or collection
	 * initializer, or {@code null} for current state.
	 */
	private static @Nullable Object changesetId(Initializer<?> owner, RowProcessingState rowProcessingState) {
		if ( owner instanceof EntityInitializerImpl entityInitializer ) {
			final var ownerKey = entityInitializer.getData( rowProcessingState ).entityKey;
			return ownerKey == null ? null : ownerKey.getChangesetId();
		}
		else if ( owner instanceof CollectionInitializer<?> collectionInitializer
				&& collectionInitializer.getCollectionInstance( rowProcessingState )
						instanceof AbstractPersistentCollection<?> collection ) {
			return collection.getTemporalIdentifier();
		}
		else {
			return null;
		}
	}
}
