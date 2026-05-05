/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.entity;

import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.queue.bind.PreExecutionCallback;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.PreDeleteEvent;

/// Pre-execution callback for entity delete actions.
///
/// @author Steve Ebersole
public class PreDeleteHandling implements PreExecutionCallback {
	private final EntityDeleteAction action;
	private boolean invoked;
	private boolean veto;

	public PreDeleteHandling(EntityDeleteAction action) {
		this.action = action;
	}

	@Override
	public boolean beforeExecution(SessionImplementor session) {
		if ( !invoked ) {
			veto = action.getInstance() != null && firePreDelete( session );
			invoked = true;
		}
		return !veto;
	}

	private boolean firePreDelete(SessionImplementor session) {
		final var listenerGroup = session.getFactory().getEventListenerGroups().eventListenerGroup_PRE_DELETE;
		if ( listenerGroup.isEmpty() ) {
			return false;
		}

		final PreDeleteEvent event = new PreDeleteEvent(
				action.getInstance(),
				action.getId(),
				action.getState(),
				action.getPersister(),
				session
		);
		boolean veto = false;
		for ( var listener : listenerGroup.listeners() ) {
			veto |= listener.onPreDelete( event );
		}
		return veto;
	}

	public boolean isVeto() {
		return veto;
	}
}
