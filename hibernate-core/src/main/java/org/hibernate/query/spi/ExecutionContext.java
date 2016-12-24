/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query.spi;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.engine.spi.ExceptionConverter;

/**
 * A context for Query execution.
 * <p/>
 * Really this should be fulfilled by the Session.  But because this
 * PoC is being developed in isolation from ORM we need to define this separately
 *
 * @author Steve Ebersole
 */
public interface ExecutionContext extends QueryParameterBindingTypeResolver {
	FlushMode getHibernateFlushMode();

	void setHibernateFlushMode(FlushMode effectiveFlushMode);

	CacheMode getCacheMode();

	void setCacheMode(CacheMode effectiveCacheMode);

	boolean isDefaultReadOnly();

	ExceptionConverter getExceptionConverter();

	boolean isTransactionInProgress();

	void checkOpen(boolean rollbackIfNot);

	void prepareForQueryExecution(boolean requiresTxn);

	QueryInterpretations getQueryInterpretations();

	void flush();
}
