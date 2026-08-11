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
/// The legacy queue uses this adapter to preserve its executable-action representation. Lifecycle
/// preparation remains queue-neutral and is never repeated by the resulting actions. The graph
/// queue consumes prepared mutations directly.
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
		CollectionMutationPreparer.prepareAll(
				inputs,
				List.of(),
				session,
				mutation -> actionConsumer.accept( lower( mutation, session ) )
		);
	}

	private static CollectionAction lower(
			PreparedCollectionMutation mutation,
			EventSource session) {
		final var endpoint = mutation.endpoint();
		return switch ( mutation.kind() ) {
				case CREATE -> new CollectionRecreateAction(
						requireCollection( mutation ),
						endpoint.persister(),
						endpoint.key(),
						session,
						mutation.affectedOwner(),
						mutation.affectedOwnerId(),
						mutation.interpretation()
				);
				case REMOVE -> new CollectionRemoveAction(
						mutation.collection(),
						endpoint.persister(),
						endpoint.key(),
						mutation.emptySnapshot(),
						session,
						mutation.affectedOwner(),
						mutation.affectedOwnerId(),
						mutation.interpretation()
				);
				case UPDATE -> new CollectionUpdateAction(
						requireCollection( mutation ),
						endpoint.persister(),
						endpoint.key(),
						mutation.emptySnapshot(),
						session,
						mutation.affectedOwner(),
						mutation.affectedOwnerId(),
						mutation.interpretation()
				);
				case QUEUED_OPERATIONS -> new QueuedOperationCollectionAction(
						requireCollection( mutation ),
						endpoint.persister(),
						endpoint.key(),
						session,
						mutation.interpretation()
				);
		};
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
