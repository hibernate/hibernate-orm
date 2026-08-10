/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.collection;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.monitor.spi.DiagnosticEvent;
import org.hibernate.event.monitor.spi.EventMonitor;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Tests the physical-execution diagnostic span for a collection mutation.
///
/// @author Steve Ebersole
class CollectionExecutionMonitorTest {
	@Test
	void completesOnceAfterEveryPhysicalOperationSucceeds() {
		final var fixture = new Fixture( 2 );

		fixture.executionMonitor.beforeExecution( fixture.session );
		fixture.executionMonitor.beforeExecution( fixture.session );
		verify( fixture.eventMonitor ).beginCollectionUpdateEvent();

		fixture.executionMonitor.afterSuccessfulExecution( fixture.session );
		verify( fixture.eventMonitor, never() ).completeCollectionUpdateEvent(
				fixture.event,
				fixture.key,
				fixture.role,
				true,
				fixture.session
		);

		fixture.executionMonitor.afterSuccessfulExecution( fixture.session );
		verify( fixture.eventMonitor ).completeCollectionUpdateEvent(
				fixture.event,
				fixture.key,
				fixture.role,
				true,
				fixture.session
		);
	}

	@Test
	void reportsPhysicalExecutionFailureOnce() {
		final var fixture = new Fixture( 2 );

		fixture.executionMonitor.beforeExecution( fixture.session );
		fixture.executionMonitor.afterSuccessfulExecution( fixture.session );
		fixture.executionMonitor.afterFailedExecution( fixture.session );
		fixture.executionMonitor.afterFailedExecution( fixture.session );
		fixture.executionMonitor.afterSuccessfulExecution( fixture.session );

		verify( fixture.eventMonitor ).completeCollectionUpdateEvent(
				fixture.event,
				fixture.key,
				fixture.role,
				false,
				fixture.session
		);
		verify( fixture.eventMonitor, never() ).completeCollectionUpdateEvent(
				fixture.event,
				fixture.key,
				fixture.role,
				true,
				fixture.session
		);
	}

	private static class Fixture {
		private final Object key = 1;
		private final String role = "Entity.values";
		private final SessionImplementor session = mock( SessionImplementor.class );
		private final EventMonitor eventMonitor = mock( EventMonitor.class );
		private final DiagnosticEvent event = mock( DiagnosticEvent.class );
		private final CollectionExecutionMonitor executionMonitor;

		private Fixture(int operationCount) {
			when( session.getEventMonitor() ).thenReturn( eventMonitor );
			when( eventMonitor.beginCollectionUpdateEvent() ).thenReturn( event );
			executionMonitor = new CollectionExecutionMonitor(
					CollectionExecutionMonitor.Kind.UPDATE,
					key,
					role,
					operationCount
			);
		}
	}
}
