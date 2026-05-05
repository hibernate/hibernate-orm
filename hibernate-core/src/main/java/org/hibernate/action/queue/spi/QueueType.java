/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.spi;

import org.hibernate.Incubating;

/// Indicates [ActionQueue] implementations.
/// Defines the valid values for [org.hibernate.cfg.FlushSettings#FLUSH_QUEUE_TYPE].
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
public enum QueueType {
	/// Indicates the [graph-based][org.hibernate.action.queue.internal.GraphBasedActionQueue] ActionQueue
	/// implementation,	which handles planning automatically based on defined constraints (foreign key dependencies,
	/// unique keys, ...).
	GRAPH,

	/// Indicates the [traditional][org.hibernate.engine.spi.ActionQueueLegacy] ActionQueue implementation,
	/// which requires hard-coded ordering and manual sorting of actions
	LEGACY;

	public static QueueType fromSetting(Object setting) {
		if ( setting != null ) {
			if ( setting instanceof QueueType queueType ) {
				return queueType;
			}
			return QueueType.fromSetting( setting.toString() );
		}

		return GRAPH;
	}

	public static QueueType fromSetting(String setting) {
		return switch ( setting.toLowerCase() ) {
			case "legacy" -> LEGACY;
			case "graph" -> GRAPH;
			default -> throw new IllegalArgumentException(
					"Unknown ActionQueue implementation: " + setting +
					". Valid values are 'graph' and 'legacy'."
			);
		};
	}
}
