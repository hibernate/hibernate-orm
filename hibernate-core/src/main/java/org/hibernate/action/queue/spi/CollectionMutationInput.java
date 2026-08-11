/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.spi;

import java.io.Serializable;
import java.util.Set;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Incubating;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.persister.collection.CollectionPersister;

/// Queue-neutral collection mutation input produced by flush visitation.
///
/// This input deliberately precedes lifecycle preparation and delta freezing. In particular,
/// retaining it while deciding whether a speculative auto-flush is required has no callback or
/// event side effects. [#findAffectedQuerySpace(Set)] performs the conservative query-space
/// test needed for that decision without materializing an intermediate set.
///
/// @param collection The collection wrapper being processed
/// @param transition The selected logical transition
/// @param loadedEndpoint The endpoint associated with loaded state, if any
/// @param currentEndpoint The endpoint associated with current state, if any
/// @param emptySnapshot Whether the loaded snapshot is known to be empty
/// @param removalSkipped Whether physical removal is suppressed by database cascade
/// @param hasQueuedOperations Whether an uninitialized collection has delayed operations
/// @param affectedOwner Explicit affected owner when it cannot be derived from a collection wrapper
/// @param affectedOwnerId Explicit affected-owner identifier when it cannot be derived from a wrapper
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
public record CollectionMutationInput(
		@Nullable PersistentCollection<?> collection,
		@Nonnull CollectionTransition transition,
		@Nullable CollectionEndpoint loadedEndpoint,
		@Nullable CollectionEndpoint currentEndpoint,
		boolean emptySnapshot,
		boolean removalSkipped,
		boolean hasQueuedOperations,
		@Nullable Object affectedOwner,
		@Nullable Object affectedOwnerId) {

	/// Creates input whose affected owner will be derived from its collection wrapper.
	public CollectionMutationInput(
			@Nonnull PersistentCollection<?> collection,
			@Nonnull CollectionTransition transition,
			@Nullable CollectionEndpoint loadedEndpoint,
			@Nullable CollectionEndpoint currentEndpoint,
			boolean emptySnapshot,
			boolean hasQueuedOperations) {
		this(
				collection,
				transition,
				loadedEndpoint,
				currentEndpoint,
				emptySnapshot,
				false,
				hasQueuedOperations,
				null,
				null
		);
	}

	public CollectionMutationInput {
		if ( transition == CollectionTransition.CREATE && currentEndpoint == null
				|| transition == CollectionTransition.REMOVE && loadedEndpoint == null
				|| transition == CollectionTransition.UPDATE && currentEndpoint == null
				|| transition == CollectionTransition.REMOVE_AND_CREATE
						&& (loadedEndpoint == null || currentEndpoint == null) ) {
			throw new IllegalArgumentException( "Collection transition has incomplete endpoint information" );
		}
		if ( transition == CollectionTransition.NONE && !hasQueuedOperations ) {
			throw new IllegalArgumentException( "Collection mutation input contains no work" );
		}
		if ( collection == null && transition != CollectionTransition.REMOVE ) {
			throw new IllegalArgumentException( "Only collection removal may omit the collection wrapper" );
		}
		if ( collection == null && hasQueuedOperations ) {
			throw new IllegalArgumentException( "Queued collection operations require a collection wrapper" );
		}
		if ( removalSkipped
				&& transition != CollectionTransition.REMOVE
				&& transition != CollectionTransition.REMOVE_AND_CREATE ) {
			throw new IllegalArgumentException( "Physical removal may be skipped only for a remove transition" );
		}
	}

	/// Creates a wrapper-less removal input with the existing no-collection-event lifecycle.
	///
	/// @since 8.0
	public static CollectionMutationInput wrapperlessRemoval(
			@Nonnull CollectionPersister persister,
			@Nonnull Object key,
			@Nullable Object affectedOwner,
			@Nullable Object affectedOwnerId) {
		return new CollectionMutationInput(
				null,
				CollectionTransition.REMOVE,
				new CollectionEndpoint( persister, key ),
				null,
				false,
				false,
				false,
				affectedOwner,
				affectedOwnerId
		);
	}

	/// Finds a query space potentially affected by this input.
	///
	/// Both the loaded and current endpoints are considered because a role change may affect
	/// either side.  No intermediate collection of query spaces is allocated.
	///
	/// @param tables The query spaces relevant to the pending operation
	///
	/// @return a matching query space, or {@code null} when this input does not affect any
	public @Nullable Serializable findAffectedQuerySpace(Set<? extends Serializable> tables) {
		final Serializable loadedSpace = findAffectedQuerySpace( loadedEndpoint, tables );
		return loadedSpace != null
				? loadedSpace
				: findAffectedQuerySpace( currentEndpoint, tables );
	}

	private static @Nullable Serializable findAffectedQuerySpace(
			@Nullable CollectionEndpoint endpoint,
			Set<? extends Serializable> tables) {
		if ( endpoint != null ) {
			for ( var space : endpoint.persister().getCollectionSpaces() ) {
				if ( tables.contains( space ) ) {
					return space;
				}
			}
		}
		return null;
	}
}
