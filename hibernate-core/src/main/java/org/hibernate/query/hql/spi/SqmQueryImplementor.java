/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.hql.spi;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.query.QueryFlushMode;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.query.BindableType;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.named.NameableQuery;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.SqmQuery;
import org.hibernate.query.sqm.spi.NamedSqmQueryMemento;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.transform.ResultTransformer;

import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;

/**
 * {@link QueryImplementor} specialization for SQM-based Query references
 *
 * @author Steve Ebersole
 */
public interface SqmQueryImplementor<R> extends QueryImplementor<R>, SqmQuery, NameableQuery {
	@Override
	NamedSqmQueryMemento<R> toMemento(String name);

	@Override
	ParameterMetadataImplementor getParameterMetadata();

	SqmStatement<R> getSqmStatement();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// covariance

	SqmQueryImplementor<R> setCacheMode(CacheMode cacheMode);

	SqmQueryImplementor<R> setCacheable(boolean cacheable);

	SqmQueryImplementor<R> setCacheRegion(String cacheRegion);

	SqmQueryImplementor<R> setTimeout(int timeout);

	SqmQueryImplementor<R> setFetchSize(int fetchSize);

	SqmQueryImplementor<R> setReadOnly(boolean readOnly);

	@Override
	SqmQueryImplementor<R> applyGraph(@SuppressWarnings("rawtypes") RootGraph graph, GraphSemantic semantic);

	@Override
	default SqmQueryImplementor<R> applyFetchGraph(@SuppressWarnings("rawtypes") RootGraph graph) {
		QueryImplementor.super.applyFetchGraph( graph );
		return this;
	}

	@Override
	default SqmQueryImplementor<R> applyLoadGraph(@SuppressWarnings("rawtypes") RootGraph graph) {
		QueryImplementor.super.applyLoadGraph( graph );
		return this;
	}

	@Override
	SqmQueryImplementor<R> setComment(String comment);

	@Override
	SqmQueryImplementor<R> addQueryHint(String hint);

	@Override
	SqmQueryImplementor<R> setLockOptions(LockOptions lockOptions);

	@Override
	SqmQueryImplementor<R> setLockMode(String alias, LockMode lockMode);

	@Override
	<T> SqmQueryImplementor<T> setTupleTransformer(TupleTransformer<T> transformer);

	@Override
	SqmQueryImplementor<R> setResultListTransformer(ResultListTransformer<R> transformer);

	@Override
	@Deprecated(since = "5.2")
	default <T> SqmQueryImplementor<T> setResultTransformer(ResultTransformer<T> transformer) {
		return setTupleTransformer( transformer ).setResultListTransformer( transformer );
	}

	@Override @Deprecated(since = "7")
	SqmQueryImplementor<R> setHibernateFlushMode(FlushMode flushMode);

	@Override
	SqmQueryImplementor<R> setQueryFlushMode(QueryFlushMode queryFlushMode);

	@Override
	SqmQueryImplementor<R> setMaxResults(int maxResult);

	@Override
	SqmQueryImplementor<R> setFirstResult(int startPosition);

	@Override
	SqmQueryImplementor<R> setHint(String hintName, Object value);

	@Override @Deprecated(since = "7")
	SqmQueryImplementor<R> setFlushMode(FlushModeType flushMode);

	@Override
	SqmQueryImplementor<R> setLockMode(LockModeType lockMode);

	@Override
	SqmQueryImplementor<R> setParameter(String name, Object value);

	@Override
	<P> SqmQueryImplementor<R> setParameter(String name, P value, Class<P> type);

	@Override
	<P> SqmQueryImplementor<R> setParameter(String name, P value, BindableType<P> type);

	@Override
	SqmQueryImplementor<R> setParameter(String name, Instant value, TemporalType temporalType);

	@Override
	SqmQueryImplementor<R> setParameter(String name, Calendar value, TemporalType temporalType);

	@Override
	SqmQueryImplementor<R> setParameter(String name, Date value, TemporalType temporalType);

	@Override
	SqmQueryImplementor<R> setParameter(int position, Object value);

	@Override
	<P> SqmQueryImplementor<R> setParameter(int position, P value, Class<P> type);

	@Override
	<P> SqmQueryImplementor<R> setParameter(int position, P value, BindableType<P> type);

	@Override
	SqmQueryImplementor<R> setParameter(int position, Instant value, TemporalType temporalType);

	@Override
	SqmQueryImplementor<R> setParameter(int position, Date value, TemporalType temporalType);

	@Override
	SqmQueryImplementor<R> setParameter(int position, Calendar value, TemporalType temporalType);

	@Override
	<T> SqmQueryImplementor<R> setParameter(QueryParameter<T> parameter, T value);

	@Override
	<P> SqmQueryImplementor<R> setParameter(QueryParameter<P> parameter, P value, Class<P> type);

	@Override
	<P> SqmQueryImplementor<R> setParameter(QueryParameter<P> parameter, P val, BindableType<P> type);

	@Override
	<T> SqmQueryImplementor<R> setParameter(Parameter<T> param, T value);

	@Override
	SqmQueryImplementor<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override
	SqmQueryImplementor<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType);

	@Override
	SqmQueryImplementor<R> setParameterList(String name, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> SqmQueryImplementor<R> setParameterList(String name, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> SqmQueryImplementor<R> setParameterList(String name, Collection<? extends P> values, BindableType<P> type);

	@Override
	SqmQueryImplementor<R> setParameterList(String name, Object[] values);

	@Override
	<P> SqmQueryImplementor<R> setParameterList(String name, P[] values, Class<P> javaType);

	@Override
	<P> SqmQueryImplementor<R> setParameterList(String name, P[] values, BindableType<P> type);

	@Override
	SqmQueryImplementor<R> setParameterList(int position, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> SqmQueryImplementor<R> setParameterList(int position, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> SqmQueryImplementor<R> setParameterList(int position, Collection<? extends P> values, BindableType<P> type);

	@Override
	SqmQueryImplementor<R> setParameterList(int position, Object[] values);

	@Override
	<P> SqmQueryImplementor<R> setParameterList(int position, P[] values, Class<P> javaType);

	@Override
	<P> SqmQueryImplementor<R> setParameterList(int position, P[] values, BindableType<P> type);

	@Override
	<P> SqmQueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values);

	@Override
	<P> SqmQueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> SqmQueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, BindableType<P> type);

	@Override
	<P> SqmQueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values);

	@Override
	<P> SqmQueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType);

	@Override
	<P> SqmQueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values, BindableType<P> type);

	@Override
	SqmQueryImplementor<R> setProperties(Object bean);

	@Override
	SqmQueryImplementor<R> setProperties(@SuppressWarnings("rawtypes") Map bean);
}
