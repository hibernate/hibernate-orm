/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.support;

import org.hibernate.action.queue.ActionQueue;
import org.hibernate.action.queue.ActionQueueFactory;
import org.hibernate.action.queue.QueueImplementation;
import org.hibernate.engine.spi.ActionQueueLegacy;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/// ActionQueueFactory for building ActionQueueLegacy instances.
///
/// @author Steve Ebersole
public class LegacyActionQueueFactory implements ActionQueueFactory, Serializable {
	@Override
	public QueueImplementation getConfiguredQueueImplementation() {
		return QueueImplementation.LEGACY;
	}

	@Override
	public ActionQueue buildActionQueue(SessionImplementor session) {
		return new ActionQueueLegacy( session );
	}

	@Override
	public ActionQueue deserialize(
			ObjectInputStream ois,
			SessionImplementor session) throws IOException, ClassNotFoundException {
		return ActionQueueLegacy.deserialize( ois, (EventSource) session );
	}
}
