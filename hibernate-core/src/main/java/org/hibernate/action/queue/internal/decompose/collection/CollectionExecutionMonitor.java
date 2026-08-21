/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.collection;

import jakarta.annotation.Nullable;

import org.hibernate.action.queue.spi.bind.OperationExecutionMonitor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.monitor.spi.DiagnosticEvent;

/// Coalesces the physical operations for one collection action into a single
/// collection diagnostic event.
///
/// @author Steve Ebersole
final class CollectionExecutionMonitor implements OperationExecutionMonitor {
	private final Kind kind;
	private final Object key;
	private final String role;
	private final int operationCount;

	private @Nullable DiagnosticEvent event;
	private int successfulOperationCount;
	private boolean started;
	private boolean completed;

	CollectionExecutionMonitor(Kind kind, Object key, String role, int operationCount) {
		this.kind = kind;
		this.key = key;
		this.role = role;
		this.operationCount = operationCount;
	}

	@Override
	public void beforeExecution(SessionImplementor session) {
		if ( !started ) {
			event = kind.begin( session );
			started = true;
		}
	}

	@Override
	public void afterSuccessfulExecution(SessionImplementor session) {
		if ( completed || !started ) {
			return;
		}
		if ( ++successfulOperationCount == operationCount ) {
			complete( true, session );
		}
	}

	@Override
	public void afterFailedExecution(SessionImplementor session) {
		if ( started && !completed ) {
			complete( false, session );
		}
	}

	private void complete(boolean success, SessionImplementor session) {
		completed = true;
		kind.complete( event, key, role, success, session );
	}

	enum Kind {
		CREATE {
			@Override
			@Nullable DiagnosticEvent begin(SessionImplementor session) {
				return session.getEventMonitor().beginCollectionRecreateEvent();
			}

			@Override
			void complete(
					@Nullable DiagnosticEvent event,
					Object key,
					String role,
					boolean success,
					SessionImplementor session) {
				session.getEventMonitor().completeCollectionRecreateEvent( event, key, role, success, session );
			}
		},
		UPDATE {
			@Override
			@Nullable DiagnosticEvent begin(SessionImplementor session) {
				return session.getEventMonitor().beginCollectionUpdateEvent();
			}

			@Override
			void complete(
					@Nullable DiagnosticEvent event,
					Object key,
					String role,
					boolean success,
					SessionImplementor session) {
				session.getEventMonitor().completeCollectionUpdateEvent( event, key, role, success, session );
			}
		},
		REMOVE {
			@Override
			@Nullable DiagnosticEvent begin(SessionImplementor session) {
				return session.getEventMonitor().beginCollectionRemoveEvent();
			}

			@Override
			void complete(
					@Nullable DiagnosticEvent event,
					Object key,
					String role,
					boolean success,
					SessionImplementor session) {
				session.getEventMonitor().completeCollectionRemoveEvent( event, key, role, success, session );
			}
		};

		abstract @Nullable DiagnosticEvent begin(SessionImplementor session);

		abstract void complete(
				@Nullable DiagnosticEvent event,
				Object key,
				String role,
				boolean success,
				SessionImplementor session);
	}
}
