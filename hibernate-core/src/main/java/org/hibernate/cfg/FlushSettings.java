/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cfg;

/**
 * Settings related to flush behavior
 *
 * @author Steve Ebersole
 */
public interface FlushSettings {

	/**
	 * Specifies the {@link org.hibernate.action.queue.ActionQueue} implementation to use.
	 * <p>
	 * Valid values:
	 * <ul>
	 *     <li>{@code "legacy"} - Uses the traditional ActionQueue implementation
	 *         ({@link org.hibernate.engine.spi.ActionQueueLegacy}), which requires manual
	 *         ordering of actions (default)
	 *     <li>{@code "graph"} - Uses the graph-based ActionQueue implementation
	 *         ({@link org.hibernate.action.queue.GraphBasedActionQueue}), which handles
	 *         foreign key dependencies automatically through graph analysis
	 * </ul>
	 * <p>
	 * The default is {@code "legacy"}. The graph-based implementation is experimental and
	 * provides improved handling of complex foreign key relationships and automatic
	 * dependency resolution.
	 *
	 * @settingDefault {@code "legacy"}
	 *
	 * @since 7.0
	 */
	String FLUSH_QUEUE_IMPL = "hibernate.flush.queue.impl";
}
