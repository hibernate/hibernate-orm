/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.support;

import org.hibernate.action.queue.ActionQueueFactory;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import static org.hibernate.cfg.FlushSettings.FLUSH_QUEUE_IMPL;
import static org.hibernate.engine.config.spi.StandardConverters.STRING;

/// Interprets flush-related settings and builds the appropriate ActionQueueFactory.
///
/// @author Steve Ebersole
public class ActionQueueSupport {
	public static ActionQueueFactory resolveActionQueueSupport(SessionFactoryImplementor factory) {
		var configurationService = factory.getServiceRegistry().requireService( ConfigurationService.class );
		final var implementation = configurationService.getSetting( FLUSH_QUEUE_IMPL, STRING, "legacy" );

		return switch ( implementation.toLowerCase() ) {
			case "legacy" -> new LegacyActionQueueFactory();
			case "graph" -> new GraphBasedActionQueueFactory( factory );
			default -> throw new IllegalArgumentException(
					"Unknown ActionQueue implementation: " + implementation +
					". Valid values are 'graph' and 'legacy'."
			);
		};

	}
}
