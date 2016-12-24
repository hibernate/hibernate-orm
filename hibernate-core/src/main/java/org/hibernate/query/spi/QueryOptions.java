/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query.spi;

import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.query.Limit;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;

/**
 * @author Steve Ebersole
 */
public interface QueryOptions {
	Limit getLimit();
	Integer getFetchSize();
	String getComment();
	LockOptions getLockOptions();
	List<String> getDatabaseHints();

	Integer getTimeout();
	FlushMode getFlushMode();
	Boolean isReadOnly();
	CacheMode getCacheMode();
	Boolean isResultCachingEnabled();
	String getResultCacheRegionName();

	EntityGraphQueryHint getEntityGraphQueryHint();

	TupleTransformer getTupleTransformer();
	ResultListTransformer getResultListTransformer();
}
