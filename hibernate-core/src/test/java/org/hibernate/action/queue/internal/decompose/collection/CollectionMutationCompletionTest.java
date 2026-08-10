/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.collection;

import org.hibernate.action.queue.spi.CollectionMutationId;
import org.hibernate.action.queue.spi.bind.PostExecutionCallback;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.engine.spi.SessionImplementor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/// Tests semantic collection mutation sealing and completion accounting.
///
/// @author Steve Ebersole
class CollectionMutationCompletionTest {
	private final SessionImplementor session = mock( SessionImplementor.class );

	@Test
	void waitsForEveryRegisteredOperation() {
		final var completion = completion( 1 );
		final var first = mock( FlushOperation.class );
		final var second = mock( FlushOperation.class );
		final var handler = mock( PostExecutionCallback.class );

		completion.registerOperation( first );
		completion.registerOperation( second );
		completion.registerCompletionHandler( handler );
		completion.seal( session );

		completion.operationSucceeded( session );
		verify( handler, never() ).handle( session );
		assertThat( completion.getState() ).isEqualTo( CollectionMutationCompletion.State.EXECUTING );

		completion.operationSucceeded( session );
		verify( handler ).handle( session );
		assertThat( completion.getState() ).isEqualTo( CollectionMutationCompletion.State.COMPLETED );
		verify( first ).setCollectionMutationCompletion( completion );
		verify( second ).setCollectionMutationCompletion( completion );
	}

	@Test
	void zeroOperationMutationCompletesWhenSealed() {
		final var completion = completion( 2 );
		final var handler = mock( PostExecutionCallback.class );
		completion.registerCompletionHandler( handler );

		completion.seal( session );

		verify( handler ).handle( session );
		assertThat( completion.getState() ).isEqualTo( CollectionMutationCompletion.State.COMPLETED );
	}

	@Test
	void failureSuppressesSuccessfulCompletion() {
		final var completion = completion( 3 );
		final var handler = mock( PostExecutionCallback.class );
		completion.registerOperation( mock( FlushOperation.class ) );
		completion.registerOperation( mock( FlushOperation.class ) );
		completion.registerCompletionHandler( handler );
		completion.seal( session );

		completion.operationSucceeded( session );
		completion.operationFailed( session );
		completion.operationSucceeded( session );

		verify( handler, never() ).handle( session );
		assertThat( completion.getState() ).isEqualTo( CollectionMutationCompletion.State.FAILED );
	}

	@Test
	void plannedFixupParticipatesInCompletion() {
		final var completion = completion( 4 );
		final var source = mock( FlushOperation.class );
		final var fixup = mock( FlushOperation.class );
		final var handler = mock( PostExecutionCallback.class );
		completion.registerOperation( source );
		completion.registerCompletionHandler( handler );
		completion.seal( session );
		completion.reserveFixup( source );
		completion.attachReservedFixup( source, fixup );

		completion.operationSucceeded( session );
		verify( handler, never() ).handle( session );

		completion.operationSucceeded( session );
		verify( handler ).handle( session );
		verify( fixup ).setCollectionMutationCompletion( completion );
	}

	@Test
	void statementGroupingDoesNotMergeMutationIdentity() {
		final var first = completion( 5 );
		final var second = completion( 6 );
		final var firstHandler = mock( PostExecutionCallback.class );
		final var secondHandler = mock( PostExecutionCallback.class );
		first.registerOperation( mock( FlushOperation.class ) );
		second.registerOperation( mock( FlushOperation.class ) );
		first.registerCompletionHandler( firstHandler );
		second.registerCompletionHandler( secondHandler );
		first.seal( session );
		second.seal( session );

		second.operationSucceeded( session );
		verify( secondHandler ).handle( session );
		verify( firstHandler, never() ).handle( session );

		first.operationSucceeded( session );
		verify( firstHandler ).handle( session );
		assertThat( first.getId() ).isNotEqualTo( second.getId() );
	}

	private static CollectionMutationCompletion completion(long id) {
		return new CollectionMutationCompletion( new CollectionMutationId( id ), null );
	}
}
