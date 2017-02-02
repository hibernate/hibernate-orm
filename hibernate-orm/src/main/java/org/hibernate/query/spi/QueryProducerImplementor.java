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
import org.hibernate.query.Query;
import org.hibernate.query.QueryProducer;

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


	// overrides...

	@Override
	QueryImplementor getNamedQuery(String queryName);

	@Override
	QueryImplementor createQuery(String queryString);

	@Override
	<R> QueryImplementor<R> createQuery(String queryString, Class<R> resultClass);

	@Override
	Query createNamedQuery(String name);

	@Override
	<R> QueryImplementor<R> createNamedQuery(String name, Class<R> resultClass);

	@Override
	NativeQueryImplementor createNativeQuery(String sqlString);

	@Override
	NativeQueryImplementor createNativeQuery(String sqlString, Class resultClass);

	@Override
	NativeQueryImplementor createNativeQuery(String sqlString, String resultSetMapping);

	@Override
	NativeQueryImplementor getNamedNativeQuery(String name);

	@Override
	default NativeQueryImplementor getNamedSQLQuery(String name) {
		return (NativeQueryImplementor) QueryProducer.super.getNamedSQLQuery( name );
	}

	@Override
	default NativeQueryImplementor createSQLQuery(String queryString) {
		return (NativeQueryImplementor) QueryProducer.super.createSQLQuery( queryString );
	}
}
