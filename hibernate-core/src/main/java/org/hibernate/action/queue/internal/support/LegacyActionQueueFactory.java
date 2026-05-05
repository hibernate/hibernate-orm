/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.support;

import org.hibernate.action.queue.spi.ActionQueue;
import org.hibernate.action.queue.spi.ActionQueueFactory;
import org.hibernate.action.queue.spi.QueueType;
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
	public QueueType getConfiguredQueueType() {
		return QueueType.LEGACY;
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
