/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.jpa;

import org.hibernate.query.Query;

/**
 * @author Steve Ebersole
 */
public enum CachingType {
	/**
	 * No caching is done
	 */
	NONE,
	/**
	 * Caching is done related to entity, collection and natural-id data
	 */
	DATA,
	/**
	 * Caching is done on the results of {@linkplain Query} executions
	 */
	QUERY,
	/**
	 * Both {@linkplain #DATA} and {@linkplain #QUERY} are enabled.
	 */
	BOTH,
	/**
	 * Implicit setting.  Defaults to {@linkplain #NONE}
	 */
	AUTO
}
