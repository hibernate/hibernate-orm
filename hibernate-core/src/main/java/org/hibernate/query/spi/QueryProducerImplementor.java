/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.QueryProducer;
import org.hibernate.query.sql.spi.NativeQueryImplementor;

import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;

/**
 * The internal contract for {@link QueryProducer} implementations.
 *
 * @deprecated This interface is no longer needed and will be removed.
 *
 * @author Steve Ebersole
 */
@Deprecated(since = "7.0", forRemoval = true)
public interface QueryProducerImplementor extends QueryProducer {
	SessionFactoryImplementor getFactory();

	FlushMode getHibernateFlushMode();
	CacheMode getCacheMode();

	@Override @SuppressWarnings("rawtypes")
	QueryImplementor getNamedQuery(String queryName);

	@Override @Deprecated @SuppressWarnings("rawtypes")
	QueryImplementor createQuery(String queryString);

	@Override
	<R> QueryImplementor<R> createQuery(String queryString, Class<R> resultClass);

	@Override
	<R> QueryImplementor<R> createQuery(TypedQueryReference<R> typedQueryReference);

	@Override @Deprecated @SuppressWarnings("rawtypes")
	QueryImplementor createNamedQuery(String name);

	@Override
	<R> QueryImplementor<R> createNamedQuery(String name, Class<R> resultClass);

	@Override @Deprecated @SuppressWarnings("rawtypes")
	NativeQueryImplementor createNativeQuery(String sqlString);

	@Override
	<R> NativeQueryImplementor<R> createNativeQuery(String sqlString, Class<R> resultClass);

	@Override
	<R> NativeQueryImplementor<R> createNativeQuery(String sqlString, Class<R> resultClass, String tableAlias);

	@Override @Deprecated @SuppressWarnings("rawtypes")
	NativeQueryImplementor createNativeQuery(String sqlString, String resultSetMappingName);

	@Override
	<R> NativeQueryImplementor<R> createNativeQuery(String sqlString, String resultSetMappingName, Class<R> resultClass);

	@Override @Deprecated @SuppressWarnings("rawtypes")
	NativeQueryImplementor getNamedNativeQuery(String name);

	@Override @Deprecated @SuppressWarnings("rawtypes")
	NativeQueryImplementor getNamedNativeQuery(String name, String resultSetMapping);

	@Override
	MutationQuery createMutationQuery(String statementString);

	@Override
	MutationQuery createNamedMutationQuery(String name);

	@Override
	MutationQuery createNativeMutationQuery(String sqlString);

	@Override
	MutationQuery createMutationQuery(@SuppressWarnings("rawtypes") CriteriaUpdate updateQuery);

	@Override
	MutationQuery createMutationQuery(@SuppressWarnings("rawtypes") CriteriaDelete deleteQuery);

	@Override
	<R> QueryImplementor<R> createQuery(CriteriaQuery<R> criteriaQuery);

	@Override @Deprecated @SuppressWarnings("rawtypes")
	QueryImplementor createQuery(CriteriaUpdate updateQuery);

	@Override @Deprecated @SuppressWarnings("rawtypes")
	QueryImplementor createQuery(CriteriaDelete deleteQuery);
}
