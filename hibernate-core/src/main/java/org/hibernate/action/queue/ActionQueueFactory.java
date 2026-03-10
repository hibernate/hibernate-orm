/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.support;

import org.hibernate.action.queue.ActionQueue;
import org.hibernate.engine.spi.SessionImplementor;

/// Factory for ActionQueue instances.
///
/// @author Steve Ebersole
public interface ActionQueueFactory {
	/// Build an ActionQueue instance for the given Session.
	ActionQueue buildActionQueue(SessionImplementor session);
}
