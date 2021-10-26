/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph;

import java.util.function.Consumer;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.query.NavigablePath;

/**
 * Provides access to information about the owner/parent of a fetch
 * in relation to the current "row" being processed.
 *
 * @author Steve Ebersole
 */
public interface FetchParentAccess extends Initializer {
	/**
	 * Find the first entity access up the fetch parent graph
	 */
	FetchParentAccess findFirstEntityDescriptorAccess();

	Object getParentKey();

	NavigablePath getNavigablePath();

	EntityKey getEntityKey();

	/**
	 * Register a listener to be notified when the parent is "resolved"
	 *
	 * @apiNote If already resolved, the callback is triggered immediately
	 */
	void registerResolutionListener(Consumer<Object> resolvedParentConsumer);
}
