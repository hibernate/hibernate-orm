/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.ast.spi;

import org.hibernate.engine.spi.SessionImplementor;

/**
 * Encapsulation of the options for loading multiple entities by id
 */
public interface MultiIdLoadOptions extends MultiLoadOptions {
	/**
	 * Check the first-level cache first, and only if the entity is not found in the cache
	 * should Hibernate hit the database.
	 *
	 * @return the session cache is checked first
	 */
	boolean isSessionCheckingEnabled();

	/**
	 * Check the second-level cache first, and only if the entity is not found in the cache
	 * should Hibernate hit the database.
	 *
	 * @return the session factory cache is checked first
	 */
	boolean isSecondLevelCacheCheckingEnabled();

	/**
	 * Should the entities be loaded in read-only mode?
	 */
	Boolean getReadOnly(SessionImplementor session);
}
