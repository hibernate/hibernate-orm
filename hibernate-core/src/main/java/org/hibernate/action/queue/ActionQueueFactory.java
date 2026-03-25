/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue;

import org.hibernate.engine.spi.SessionImplementor;

import java.io.IOException;
import java.io.ObjectInputStream;

/// Factory for ActionQueue instances.
///
/// @author Steve Ebersole
public interface ActionQueueFactory {
	/// Reports which [queue][ActionQueue] implementation was configured to be used.
	QueueImplementation getConfiguredQueueImplementation();

	/// Build an ActionQueue instance for the given Session.
	ActionQueue buildActionQueue(SessionImplementor session);

	/// Support for deserializing the ActionQueue as part of Session deserialization.
	ActionQueue deserialize(
			ObjectInputStream ois,
			SessionImplementor session) throws IOException, ClassNotFoundException;
}
