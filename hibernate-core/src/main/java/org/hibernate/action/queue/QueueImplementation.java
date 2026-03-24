/*
 * SPDX-License-Identifier: Apache-2.0
 *  Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue;

/// Defines the valid values for [org.hibernate.cfg.FlushSettings#FLUSH_QUEUE_IMPL].
///
/// @author Steve Ebersole
public enum QueueImplementation {
	GRAPH,
	LEGACY;

	public static QueueImplementation fromSetting(String setting) {
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
