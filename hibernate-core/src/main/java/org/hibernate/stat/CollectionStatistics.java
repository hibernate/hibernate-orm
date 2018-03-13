/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.stat;

import java.io.Serializable;

/**
 * Collection related statistics
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface CollectionStatistics extends CacheableDataStatistics, Serializable {
	/**
	 * Number of times (since last Statistics clearing) this collection
	 * has been loaded
	 */
	long getLoadCount();

	/**
	 * Number of times (since last Statistics clearing) this collection
	 * has been fetched
	 */
	long getFetchCount();

	/**
	 * Number of times (since last Statistics clearing) this collection
	 * has been recreated (rows potentially deleted and then rows (re-)inserted)
	 */
	long getRecreateCount();

	/**
	 * Number of times (since last Statistics clearing) this collection
	 * has been removed
	 */
	long getRemoveCount();

	/**
	 * Number of times (since last Statistics clearing) this collection
	 * has been updated
	 */
	long getUpdateCount();
}
