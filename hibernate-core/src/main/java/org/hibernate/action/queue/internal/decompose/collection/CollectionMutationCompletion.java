/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.collection;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.action.queue.internal.PreparedCollectionMutation;
import org.hibernate.action.queue.spi.CollectionMutationId;
import org.hibernate.action.queue.spi.bind.PostExecutionCallback;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.internal.FlushProcessingContext;
import org.hibernate.persister.collection.CollectionPersister;

/// Tracks execution and successful-completion obligations for one semantic
/// collection mutation.
///
/// The completion is deliberately independent of statement grouping. Each
/// physical operation reports against this object after direct execution or
/// after its containing JDBC batch succeeds. Callback-only work is retained as
/// a completion handler and does not require a synthetic SQL operation.
///
/// @since 8.0
/// @author Steve Ebersole
public final class CollectionMutationCompletion {
	public enum State {
		DISCOVERED,
		REGISTERING,
		SEALED,
		EXECUTING,
		COMPLETED,
		FAILED
	}

	public enum Kind {
		CREATE,
		UPDATE,
		REMOVE,
		UNSPECIFIED
	}

	private final CollectionMutationId id;
	private final PersistentCollection<?> collection;
	private final List<PostExecutionCallback> completionHandlers = new ArrayList<>();
	private final Map<FlushOperation, Boolean> fixupReservations = new IdentityHashMap<>();

	private State state = State.DISCOVERED;
	private Kind kind = Kind.UNSPECIFIED;
	private CollectionPersister persister;
	private Object key;
	private Object affectedOwner;
	private Object affectedOwnerId;
	private int obligationCount;
	private int completedObligationCount;
	private boolean flushSuccessRequired;

	public CollectionMutationCompletion(
			CollectionMutationId id,
			PersistentCollection<?> collection) {
		this.id = id;
		this.collection = collection;
		state = State.REGISTERING;
	}

	public CollectionMutationId getId() {
		return id;
	}

	public State getState() {
		return state;
	}

	public Kind getKind() {
		return kind;
	}

	public PersistentCollection<?> getCollection() {
		return collection;
	}

	public CollectionPersister getPersister() {
		return persister;
	}

	public Object getKey() {
		return key;
	}

	public Object getAffectedOwner() {
		return affectedOwner;
	}

	public Object getAffectedOwnerId() {
		return affectedOwnerId;
	}

	public int getObligationCount() {
		return obligationCount;
	}

	public void configure(PreparedCollectionMutation mutation) {
		checkRegistering();
		persister = mutation.getPersister();
		key = mutation.getKey();
		kind = determineKind( mutation );
		affectedOwner = mutation.affectedOwner();
		affectedOwnerId = mutation.affectedOwnerId();
	}

	public void registerOperation(FlushOperation operation) {
		checkRegistering();
		obligationCount++;
		operation.setCollectionMutationCompletion( this );
	}

	public void registerCompletionHandler(PostExecutionCallback completionHandler) {
		checkRegistering();
		completionHandlers.add( completionHandler );
	}

	/// Prevents successful semantic completion until the enclosing flush succeeds.
	///
	/// Queued inverse-collection work may be physically owned by an entity mutation and
	/// therefore have no collection operation which can represent its final dependency.
	public void requireFlushSuccess() {
		checkRegistering();
		flushSuccessRequired = true;
	}

	public void seal(SessionImplementor session) {
		checkRegistering();
		state = State.SEALED;
		completeIfReady( session );
	}

	/// Reserves the completion obligation for a fixup identified by planning.
	public void reserveFixup(FlushOperation sourceOperation) {
		if ( state != State.SEALED ) {
			throw new AssertionFailure( "Fixup reservation requires a sealed collection mutation" );
		}
		if ( fixupReservations.put( sourceOperation, Boolean.TRUE ) == null ) {
			obligationCount++;
			sourceOperation.setCollectionMutationFixupReserved( true );
		}
	}

	/// Transfers a source operation's reserved obligation to its synthesized fixup.
	public void attachReservedFixup(FlushOperation sourceOperation, FlushOperation fixupOperation) {
		if ( fixupReservations.remove( sourceOperation ) == null ) {
			throw new AssertionFailure( "No collection mutation fixup was reserved" );
		}
		sourceOperation.setCollectionMutationFixupReserved( false );
		fixupOperation.setCollectionMutationCompletion( this );
	}

	/// Satisfies a reserved fixup obligation when binding produced no deferred value.
	public void releaseReservedFixup(FlushOperation sourceOperation, SessionImplementor session) {
		if ( fixupReservations.remove( sourceOperation ) != null ) {
			sourceOperation.setCollectionMutationFixupReserved( false );
			completedObligationCount++;
			completeIfReady( session );
		}
	}

	public void operationSucceeded(SessionImplementor session) {
		if ( state == State.FAILED ) {
			return;
		}
		if ( state != State.SEALED && state != State.EXECUTING ) {
			throw new AssertionFailure( "Collection operation completed before its mutation was sealed" );
		}
		state = State.EXECUTING;
		completedObligationCount++;
		if ( completedObligationCount > obligationCount ) {
			throw new AssertionFailure( "Collection mutation completed more obligations than were registered" );
		}
		completeIfReady( session );
	}

	public void operationFailed(SessionImplementor session) {
		if ( state != State.COMPLETED ) {
			state = State.FAILED;
			if ( affectedOwner != null ) {
				final var tracker = session.getPersistenceContextInternal().getCollectionFlushActionTracker();
				if ( tracker instanceof FlushProcessingContext flushProcessingContext ) {
					flushProcessingContext.ownerMutationFailed( affectedOwner );
				}
			}
		}
	}

	/// Releases a completion which was waiting for successful completion of the whole flush.
	public void flushSucceeded(SessionImplementor session) {
		if ( state == State.FAILED || state == State.COMPLETED ) {
			return;
		}
		flushSuccessRequired = false;
		completeIfReady( session );
	}

	private void completeIfReady(SessionImplementor session) {
		if ( (state == State.SEALED || state == State.EXECUTING)
				&& !flushSuccessRequired
				&& completedObligationCount == obligationCount ) {
			try {
				for ( var completionHandler : completionHandlers ) {
					completionHandler.handle( session );
				}
				reportOwnerCollectionCompletion( session, () -> recordStatistics( session ) );
				state = State.COMPLETED;
			}
			catch (RuntimeException | Error failure) {
				state = State.FAILED;
				throw failure;
			}
		}
	}

	private void reportOwnerCollectionCompletion(SessionImplementor session, Runnable successfulCompletionHandler) {
		if ( affectedOwner != null && persister != null ) {
			final var tracker = session.getPersistenceContextInternal().getCollectionFlushActionTracker();
			if ( tracker instanceof FlushProcessingContext flushProcessingContext ) {
				flushProcessingContext.ownerCollectionMutationCompleted(
						affectedOwner,
						persister.isInverse(),
						successfulCompletionHandler
				);
				return;
			}
		}
		successfulCompletionHandler.run();
	}

	private void recordStatistics(SessionImplementor session) {
		if ( persister == null ) {
			return;
		}
		final var statistics = session.getFactory().getStatistics();
		if ( !statistics.isStatisticsEnabled() ) {
			return;
		}
		switch ( kind ) {
			case CREATE -> statistics.recreateCollection( persister.getRole() );
			case UPDATE -> statistics.updateCollection( persister.getRole() );
			case REMOVE -> statistics.removeCollection( persister.getRole() );
			case UNSPECIFIED -> {
			}
		}
	}

	private void checkRegistering() {
		if ( state != State.REGISTERING ) {
			throw new AssertionFailure( "Collection mutation is no longer registering obligations" );
		}
	}

	private static Kind determineKind(PreparedCollectionMutation mutation) {
		return switch ( mutation.kind() ) {
			case CREATE -> Kind.CREATE;
			case UPDATE -> Kind.UPDATE;
			case REMOVE -> Kind.REMOVE;
			case QUEUED_OPERATIONS -> Kind.UNSPECIFIED;
		};
	}
}
