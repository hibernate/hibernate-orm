/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import java.util.function.Function;

import org.hibernate.Incubating;
import org.hibernate.query.sqm.tree.SqmStatement;

/**
 * A cache for QueryPlans used (and produced) by the translation
 * and execution of a query.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface QueryInterpretationCache {
	interface Key {
	}

	SelectQueryPlan getSelectQueryPlan(Key key);
	void cacheSelectQueryPlan(Key key, SelectQueryPlan plan);

	NonSelectQueryPlan getNonSelectQueryPlan(Key key);
	void cacheNonSelectQueryPlan(Key key, NonSelectQueryPlan plan);

	SqmStatement resolveSqmStatement(String queryString, Function<String, SqmStatement<?>> creator);
	SqmStatement getSqmStatement(String queryString);
	void cacheSqmStatement(String key, SqmStatement sqmStatement);

	boolean isEnabled();

	/**
	 * Close the cache when the SessionFactory is closed.
	 * <p>
	 * Note that depending on the cache strategy implementation chosen, clearing the cache might not reclaim all the
	 * memory.
	 * <p>
	 * Typically, when using LIRS, clearing the cache only invalidates the entries but the outdated entries are kept in
	 * memory until they are replaced by others. It is not considered a memory leak as the cache is bounded.
	 */
	void close();
}
