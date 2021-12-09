/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.spi;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.QueryProducer;
import org.hibernate.query.sql.spi.NativeQueryImplementor;

/**
 * The internal contract for QueryProducer implementations.  Acts as the value passed to
 * produced queries and provides them with access to needed functionality.
 *
 * @author Steve Ebersole
 */
public interface QueryProducerImplementor extends QueryProducer {
	SessionFactoryImplementor getFactory();

	FlushMode getHibernateFlushMode();
	CacheMode getCacheMode();

	// todo : define list/scroll/iterate methods here...

	@Override
	<R> QueryImplementor<R> getNamedQuery(String queryName);

	@Override
	<R> QueryImplementor<R> createQuery(String queryString);

	@Override
	<R> QueryImplementor<R> createQuery(String queryString, Class<R> resultClass);

	@Override
	<R> QueryImplementor<R> createNamedQuery(String name);

	@Override
	<R> QueryImplementor<R> createNamedQuery(String name, Class<R> resultClass);

	@Override
	<R> NativeQueryImplementor<R> createNativeQuery(String sqlString);

	@Override
	<R> NativeQueryImplementor<R> createNativeQuery(String sqlString, Class<R> resultClass);

	@Override
	<R> NativeQueryImplementor<R> createNativeQuery(String sqlString, String resultSetMappingName);

	@Override
	<R> NativeQueryImplementor<R> getNamedNativeQuery(String name);

	@Override
	<R> NativeQueryImplementor<R> getNamedNativeQuery(String name, String resultSetMapping);
}
