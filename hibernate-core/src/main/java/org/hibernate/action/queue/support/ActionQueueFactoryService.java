/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.support;

import org.hibernate.action.queue.ActionQueueFactory;
import org.hibernate.action.queue.QueueType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.Service;

import java.io.Serializable;

import static org.hibernate.action.internal.ActionLogging.ACTION_LOGGER;

/// Service for managing resolution of [ActionQueueFactory] implementation to use.
/// Yes, yes... in many ways a factory-factory... but helps avoid some chicken-egg
/// problems when bootstrapping a SessionFactory.
///
/// @author Steve Ebersole
public record ActionQueueFactoryService(QueueType queueType) implements Service, Serializable {
	public ActionQueueFactoryService(QueueType queueType) {
		this.queueType = queueType;
		ACTION_LOGGER.usingActionQueue( queueType.name() );
	}

	public ActionQueueFactory buildActionQueueFactory(SessionFactoryImplementor sessionFactory) {
		return switch ( queueType ) {
			case LEGACY -> new LegacyActionQueueFactory();
			case GRAPH -> new GraphBasedActionQueueFactory( sessionFactory );
		};
	}
}
