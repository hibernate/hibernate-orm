/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.Internal;
import org.hibernate.action.internal.CollectionAction;
import org.hibernate.action.internal.CollectionRecreateAction;
import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.internal.CollectionUpdateAction;
import org.hibernate.action.internal.QueuedOperationCollectionAction;
import org.hibernate.action.queue.spi.CollectionMutationInput;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.event.spi.EventSource;

/// Transitional lowering of prepared collection mutations to legacy executable actions.
///
/// Both queues may use this adapter while their native representations are migrated. Lifecycle
/// preparation remains queue-neutral and is never repeated by the resulting actions.
///
/// @since 8.0
/// @author Steve Ebersole
@Internal
public final class CollectionMutationActionLowering {
	public static void lower(
			CollectionMutationInput input,
			EventSource session,
			Consumer<CollectionAction> actionConsumer) {
		lower( List.of( input ), session, actionConsumer );
	}

	public static void lower(
			List<CollectionMutationInput> inputs,
			EventSource session,
			Consumer<CollectionAction> actionConsumer) {
		for ( var mutation : CollectionMutationPreparer.prepareAll( inputs, session ) ) {
			final var endpoint = mutation.endpoint();
			final var action = switch ( mutation.kind() ) {
				case CREATE -> new CollectionRecreateAction(
						requireCollection( mutation ),
						endpoint.persister(),
						endpoint.key(),
						session,
						mutation.affectedOwner(),
						mutation.affectedOwnerId()
				);
				case REMOVE -> new CollectionRemoveAction(
						mutation.collection(),
						endpoint.persister(),
						endpoint.key(),
						mutation.emptySnapshot(),
						session,
						mutation.affectedOwner(),
						mutation.affectedOwnerId()
				);
				case UPDATE -> new CollectionUpdateAction(
						requireCollection( mutation ),
						endpoint.persister(),
						endpoint.key(),
						mutation.emptySnapshot(),
						session,
						mutation.affectedOwner(),
						mutation.affectedOwnerId()
				);
				case QUEUED_OPERATIONS -> new QueuedOperationCollectionAction(
						requireCollection( mutation ),
						endpoint.persister(),
						endpoint.key(),
						session
				);
			};
			actionConsumer.accept( action );
		}
	}

	private static PersistentCollection<?> requireCollection(PreparedCollectionMutation mutation) {
		final var collection = mutation.collection();
		if ( collection == null ) {
			throw new IllegalArgumentException( mutation.kind() + " requires a collection wrapper" );
		}
		return collection;
	}

	private CollectionMutationActionLowering() {
	}
}
