/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.support;

import org.hibernate.action.queue.ActionQueue;
import org.hibernate.action.queue.ActionQueueFactory;
import org.hibernate.engine.spi.ActionQueueLegacy;
import org.hibernate.engine.spi.SessionImplementor;

/// ActionQueueFactory for building ActionQueueLegacy instances.
///
/// @author Steve Ebersole
public class LegacyActionQueueFactory implements ActionQueueFactory {
	@Override
	public ActionQueue buildActionQueue(SessionImplementor session) {
		return new ActionQueueLegacy( session );
	}
}
