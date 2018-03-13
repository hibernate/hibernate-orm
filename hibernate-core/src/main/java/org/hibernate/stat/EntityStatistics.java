/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.stat;

import java.io.Serializable;

/**
 * Entity related statistics
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface EntityStatistics extends CacheableDataStatistics, Serializable {
	/**
	 * Number of times (since last Statistics clearing) this entity
	 * has been deleted
	 */
	long getDeleteCount();

	/**
	 * Number of times (since last Statistics clearing) this entity
	 * has been inserted
	 */
	long getInsertCount();

	/**
	 * Number of times (since last Statistics clearing) this entity
	 * has been updated
	 */
	long getUpdateCount();

	/**
	 * Number of times (since last Statistics clearing) this entity
	 * has been loaded
	 */
	long getLoadCount();

	/**
	 * Number of times (since last Statistics clearing) this entity
	 * has been fetched
	 */
	long getFetchCount();

	/**
	 * Number of times (since last Statistics clearing) this entity
	 * has experienced an optimistic lock failure.
	 */
	long getOptimisticFailureCount();
}
