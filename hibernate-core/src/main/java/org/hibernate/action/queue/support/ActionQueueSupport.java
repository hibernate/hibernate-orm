/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue;

import org.hibernate.action.queue.constraint.ConstraintModel;
import org.hibernate.action.queue.constraint.ConstraintModelBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import static org.hibernate.cfg.FlushSettings.FLUSH_QUEUE_IMPL;

/**
 * @author Steve Ebersole
 */
public class ActionQueueSupport {
	public static ActionQueueFactory resolveActionQueueSupport(SessionFactoryImplementor factory) {
		final var implementation = factory
				.getServiceRegistry()
				.requireService( org.hibernate.engine.config.spi.ConfigurationService.class )
				.getSetting( FLUSH_QUEUE_IMPL, String.class, "legacy" );

		return switch ( implementation.toLowerCase() ) {
			case "legacy" -> new ActionQueueLegacy( this );
			case "graph" -> new GraphBasedActionQueueFactory( constraintModel, planningOptions, this );
			default -> throw new IllegalArgumentException(
					"Unknown ActionQueue implementation: " + implementation +
					". Valid values are 'graph' and 'legacy'."
			);
		};

	}

	private static GraphBasedActionQueueFactory buildGraphBasedActionQueueFactory(SessionFactoryImplementor factory) {
		ConstraintModel model = new ConstraintModelBuilder().build( factory.getMappingMetamodel() );
	}
}
