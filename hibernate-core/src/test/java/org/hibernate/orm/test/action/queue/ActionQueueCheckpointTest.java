/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue;

import java.util.Map;

import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.internal.EntityInsertAction;
import org.hibernate.action.internal.EntityUpdateAction;
import org.hibernate.action.internal.OrphanRemovalAction;
import org.hibernate.action.queue.spi.ActionQueue;
import org.hibernate.action.queue.spi.ActionQueueCheckpoint;
import org.hibernate.action.queue.spi.CollectionEndpoint;
import org.hibernate.action.queue.spi.CollectionMutationInput;
import org.hibernate.action.queue.spi.CollectionTransition;
import org.hibernate.action.queue.internal.GraphBasedActionQueue;
import org.hibernate.action.queue.internal.constraint.ConstraintModel;
import org.hibernate.action.queue.spi.PlanningOptions;
import org.hibernate.engine.internal.FlushProcessingContext;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.ActionQueueLegacy;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Contract tests for speculative-flush [ActionQueue] checkpoints.
///
/// @author Steve Ebersole
@DomainModel
@SessionFactory
public class ActionQueueCheckpointTest {
	@Test
	void checkpointContract(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final EventSource eventSource = session.unwrap( EventSource.class );
			verifyContract( eventSource.getActionQueue(), eventSource );
			verifyContract( new ActionQueueLegacy( eventSource ), eventSource );
		} );
	}

	@Test
	void postCheckpointOrphanCollectionRemovalIsDurable() {
		final var session = mock( EventSource.class );
		final var sessionFactory = mock( SessionFactoryImplementor.class );
		final var sessionFactoryOptions = mock( SessionFactoryOptions.class );
		final var persistenceContext = mock( PersistenceContext.class );
		final var ownerEntry = mock( EntityEntry.class );
		final var owner = new Object();
		when( session.getPersistenceContextInternal() ).thenReturn( persistenceContext );
		when( session.getFactory() ).thenReturn( sessionFactory );
		when( sessionFactory.getSessionFactoryOptions() ).thenReturn( sessionFactoryOptions );
		when( persistenceContext.getEntry( owner ) ).thenReturn( ownerEntry );
		when( ownerEntry.getStatus() ).thenReturn( Status.DELETED );
		when( persistenceContext.getCollectionFlushActionTracker() )
				.thenReturn( new FlushProcessingContext( session, true ) );

		verifyOrphanCollectionRemoval(
				new ActionQueueLegacy( session ),
				owner
		);
		verifyOrphanCollectionRemoval(
				new GraphBasedActionQueue(
						mock( ConstraintModel.class ),
						mock( PlanningOptions.class ),
						Map.of(),
						false,
						session
				),
				owner
		);
	}

	private static void verifyOrphanCollectionRemoval(ActionQueue actionQueue, Object owner) {
		final var checkpoint = actionQueue.checkpoint();
		final var orphanRemoval = mock( OrphanRemovalAction.class );
		when( orphanRemoval.getInstance() ).thenReturn( owner );
		actionQueue.addAction( orphanRemoval );

		actionQueue.addCollectionMutation( CollectionMutationInput.wrapperlessRemoval(
				mock( CollectionPersister.class ),
				1L,
				owner,
				1L
		) );

		actionQueue.restore( checkpoint );

		assertEquals( 1, actionQueue.numberOfDeletions() );
		assertEquals( 1, actionQueue.numberOfCollectionRemovals() );
	}

	private static void verifyContract(ActionQueue actionQueue, EventSource eventSource) {
		verifyPreCheckpointWorkSurvives( actionQueue, eventSource );
		verifyRegenerableWorkIsDiscarded( actionQueue, eventSource );
		verifyDurableWorkSurvives( actionQueue );
		verifySemanticCollectionInputs( actionQueue, eventSource );
		verifyCheckpointOwnership( actionQueue );
	}

	private static void verifySemanticCollectionInputs(ActionQueue actionQueue, EventSource eventSource) {
		actionQueue.clear();
		withFlushProcessingContext( eventSource, () -> {
			actionQueue.addCollectionMutation( mutationInput( CollectionTransition.UPDATE ) );
			final var checkpoint = actionQueue.checkpoint();
			actionQueue.addCollectionMutation( mutationInput( CollectionTransition.REMOVE_AND_CREATE ) );

			assertEquals( 1, actionQueue.numberOfCollectionUpdates() );
			assertEquals( 1, actionQueue.numberOfCollectionRemovals() );
			assertEquals( 1, actionQueue.numberOfCollectionCreations() );
			assertTrue( actionQueue.areTablesToBeUpdated( java.util.Set.of( "collection_table" ) ) );

			actionQueue.restore( checkpoint );

			assertEquals( 1, actionQueue.numberOfCollectionUpdates() );
			assertEquals( 0, actionQueue.numberOfCollectionRemovals() );
			assertEquals( 0, actionQueue.numberOfCollectionCreations() );
		} );
	}

	private static void verifyPreCheckpointWorkSurvives(ActionQueue actionQueue, EventSource eventSource) {
		actionQueue.clear();
		withFlushProcessingContext( eventSource, () -> {
			actionQueue.addAction( mock( EntityUpdateAction.class ) );
			actionQueue.addCollectionMutation( mutationInput( CollectionTransition.REMOVE ) );
			actionQueue.addCollectionMutation( mutationInput( CollectionTransition.UPDATE ) );
			actionQueue.addCollectionMutation( mutationInput( CollectionTransition.CREATE ) );

			final var checkpoint = actionQueue.checkpoint();

			actionQueue.addAction( mock( EntityUpdateAction.class ) );
			actionQueue.addCollectionMutation( mutationInput( CollectionTransition.REMOVE ) );
			actionQueue.addCollectionMutation( mutationInput( CollectionTransition.UPDATE ) );
			actionQueue.addCollectionMutation( mutationInput( CollectionTransition.CREATE ) );
			actionQueue.restore( checkpoint );
		} );

		assertEquals( 1, actionQueue.numberOfUpdates() );
		assertEquals( 1, actionQueue.numberOfCollectionRemovals() );
		assertEquals( 1, actionQueue.numberOfCollectionUpdates() );
		assertEquals( 1, actionQueue.numberOfCollectionCreations() );
	}

	private static void verifyRegenerableWorkIsDiscarded(ActionQueue actionQueue, EventSource eventSource) {
		actionQueue.clear();
		final var checkpoint = actionQueue.checkpoint();
		withFlushProcessingContext( eventSource, () -> {
			actionQueue.addAction( mock( EntityUpdateAction.class ) );
			actionQueue.addCollectionMutation( mutationInput( CollectionTransition.REMOVE ) );
			actionQueue.addCollectionMutation( mutationInput( CollectionTransition.UPDATE ) );
			actionQueue.addCollectionMutation( mutationInput( CollectionTransition.CREATE ) );
			actionQueue.addCollectionMutation( mutationInput( CollectionTransition.NONE, true ) );
		} );

		actionQueue.restore( checkpoint );

		assertEquals( 0, actionQueue.numberOfUpdates() );
		assertEquals( 0, actionQueue.numberOfCollectionRemovals() );
		assertEquals( 0, actionQueue.numberOfCollectionUpdates() );
		assertEquals( 0, actionQueue.numberOfCollectionCreations() );
		assertFalse( actionQueue.hasAnyQueuedActions() );
	}

	private static void verifyDurableWorkSurvives(ActionQueue actionQueue) {
		actionQueue.clear();
		final var checkpoint = actionQueue.checkpoint();
		actionQueue.addAction( insertAction() );
		actionQueue.addAction( mock( EntityDeleteAction.class ) );
		actionQueue.addAction( mock( OrphanRemovalAction.class ) );
		actionQueue.addCollectionMutation( mutationInput( CollectionTransition.UPDATE ) );

		actionQueue.restore( checkpoint );

		assertEquals( 1, actionQueue.numberOfInsertions() );
		assertEquals( 2, actionQueue.numberOfDeletions() );
		assertEquals( 1, actionQueue.numberOfCollectionUpdates() );
		assertTrue( actionQueue.hasAnyQueuedActions() );
	}

	private static void verifyCheckpointOwnership(ActionQueue actionQueue) {
		actionQueue.clear();
		assertThrows(
				IllegalArgumentException.class,
				() -> actionQueue.restore( new ActionQueueCheckpoint() {} )
		);
	}

	private static EntityInsertAction insertAction() {
		final var action = mock( EntityInsertAction.class );
		when( action.isEarlyInsert() ).thenReturn( false );
		when( action.isVeto() ).thenReturn( false );
		when( action.getInstance() ).thenReturn( new Object() );
		return action;
	}

	private static CollectionMutationInput mutationInput(CollectionTransition transition) {
		return mutationInput( transition, false );
	}

	private static CollectionMutationInput mutationInput(
			CollectionTransition transition,
			boolean hasQueuedOperations) {
		final var persister = mock( CollectionPersister.class );
		when( persister.getCollectionSpaces() ).thenReturn( new String[] { "collection_table" } );
		final var endpoint = new CollectionEndpoint( persister, 1L );
		return new CollectionMutationInput(
				mock( PersistentCollection.class ),
				transition,
				endpoint,
				endpoint,
				false,
				hasQueuedOperations
		);
	}

	private static void withFlushProcessingContext(EventSource eventSource, Runnable work) {
		final var persistenceContext = eventSource.getPersistenceContextInternal();
		final var previous = persistenceContext.getCollectionFlushActionTracker();
		persistenceContext.setCollectionFlushActionTracker( new FlushProcessingContext( eventSource, true ) );
		try {
			work.run();
		}
		finally {
			persistenceContext.setCollectionFlushActionTracker( previous );
		}
	}
}
