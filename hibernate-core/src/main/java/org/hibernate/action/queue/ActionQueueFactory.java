/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.service.Service;

import java.io.IOException;
import java.io.ObjectInputStream;

/// Factory for ActionQueue instances.
///
/// @author Steve Ebersole
public interface ActionQueueFactory extends Service {
	/// Reports which [queue][ActionQueue] type was configured to be used.
	QueueType getConfiguredQueueType();

	/// Build an ActionQueue instance for the given Session.
	ActionQueue buildActionQueue(SessionImplementor session);

	/// Support for deserializing the ActionQueue as part of Session deserialization.
	ActionQueue deserialize(
			ObjectInputStream ois,
			SessionImplementor session) throws IOException, ClassNotFoundException;
}
