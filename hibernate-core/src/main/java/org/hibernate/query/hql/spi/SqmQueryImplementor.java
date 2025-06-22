/*
 * SPDX-License-Identifier: Apache-2.0
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
import org.hibernate.query.QueryParameter;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.named.NameableQuery;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.SqmQuery;
import org.hibernate.query.sqm.spi.NamedSqmQueryMemento;
import org.hibernate.transform.ResultTransformer;

import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;
import jakarta.persistence.metamodel.Type;

/**
 * {@link QueryImplementor} specialization for SQM-based Query references
 *
 * @author Steve Ebersole
 */
public interface SqmQueryImplementor<R> extends QueryImplementor<R>, SqmQuery<R>, NameableQuery {
	@Override
	NamedSqmQueryMemento<R> toMemento(String name);

	@Override
	ParameterMetadataImplementor getParameterMetadata();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// covariance

	@Override
	SqmQueryImplementor<R> setCacheMode(CacheMode cacheMode);

	@Override
	SqmQueryImplementor<R> setCacheable(boolean cacheable);

	@Override
	SqmQueryImplementor<R> setCacheRegion(String cacheRegion);

	@Override
	SqmQueryImplementor<R> setTimeout(int timeout);

	@Override
	SqmQueryImplementor<R> setFetchSize(int fetchSize);

	@Override
	SqmQueryImplementor<R> setReadOnly(boolean readOnly);

	@Override
	SqmQueryImplementor<R> applyGraph(@SuppressWarnings("rawtypes") RootGraph graph, GraphSemantic semantic);

	@Override @Deprecated
	default SqmQueryImplementor<R> applyFetchGraph(@SuppressWarnings("rawtypes") RootGraph graph) {
		QueryImplementor.super.applyFetchGraph( graph );
		return this;
	}

	@Override @Deprecated
	default SqmQueryImplementor<R> applyLoadGraph(@SuppressWarnings("rawtypes") RootGraph graph) {
		QueryImplementor.super.applyLoadGraph( graph );
		return this;
	}

	@Override
	SqmQueryImplementor<R> setComment(String comment);

	@Override
	SqmQueryImplementor<R> addQueryHint(String hint);

	@Override @Deprecated
	SqmQueryImplementor<R> setLockOptions(LockOptions lockOptions);

	@Override
	SqmQueryImplementor<R> setLockMode(String alias, LockMode lockMode);

	@Override
	<T> SqmQueryImplementor<T> setTupleTransformer(TupleTransformer<T> transformer);

	@Override
	SqmQueryImplementor<R> setResultListTransformer(ResultListTransformer<R> transformer);

	@Override @Deprecated(since = "5.2")
	default <T> SqmQueryImplementor<T> setResultTransformer(ResultTransformer<T> transformer) {
		return setTupleTransformer( transformer ).setResultListTransformer( transformer );
	}

	@Override @Deprecated(since = "7")
	SqmQueryImplementor<R> setHibernateFlushMode(FlushMode flushMode);

	@Override
	SqmQueryImplementor<R> setQueryFlushMode(QueryFlushMode queryFlushMode);

	@Override
	SqmQueryImplementor<R> setMaxResults(int maxResults);

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
	<P> SqmQueryImplementor<R> setParameter(String name, P value, Type<P> type);

	@Override @Deprecated
	SqmQueryImplementor<R> setParameter(String name, Instant value, TemporalType temporalType);

	@Override @Deprecated
	SqmQueryImplementor<R> setParameter(String name, Calendar value, TemporalType temporalType);

	@Override @Deprecated
	SqmQueryImplementor<R> setParameter(String name, Date value, TemporalType temporalType);

	@Override
	SqmQueryImplementor<R> setParameter(int position, Object value);

	@Override
	<P> SqmQueryImplementor<R> setParameter(int position, P value, Class<P> type);

	@Override
	<P> SqmQueryImplementor<R> setParameter(int position, P value, Type<P> type);

	@Override @Deprecated
	SqmQueryImplementor<R> setParameter(int position, Instant value, TemporalType temporalType);

	@Override @Deprecated
	SqmQueryImplementor<R> setParameter(int position, Date value, TemporalType temporalType);

	@Override @Deprecated
	SqmQueryImplementor<R> setParameter(int position, Calendar value, TemporalType temporalType);

	@Override
	<T> SqmQueryImplementor<R> setParameter(QueryParameter<T> parameter, T value);

	@Override
	<P> SqmQueryImplementor<R> setParameter(QueryParameter<P> parameter, P value, Class<P> type);

	@Override
	<P> SqmQueryImplementor<R> setParameter(QueryParameter<P> parameter, P val, Type<P> type);

	@Override
	<T> SqmQueryImplementor<R> setParameter(Parameter<T> param, T value);

	@Override @Deprecated
	SqmQueryImplementor<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override @Deprecated
	SqmQueryImplementor<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType);

	@Override
	SqmQueryImplementor<R> setParameterList(String name, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> SqmQueryImplementor<R> setParameterList(String name, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> SqmQueryImplementor<R> setParameterList(String name, Collection<? extends P> values, Type<P> type);

	@Override
	SqmQueryImplementor<R> setParameterList(String name, Object[] values);

	@Override
	<P> SqmQueryImplementor<R> setParameterList(String name, P[] values, Class<P> javaType);

	@Override
	<P> SqmQueryImplementor<R> setParameterList(String name, P[] values, Type<P> type);

	@Override
	SqmQueryImplementor<R> setParameterList(int position, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> SqmQueryImplementor<R> setParameterList(int position, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> SqmQueryImplementor<R> setParameterList(int position, Collection<? extends P> values, Type<P> type);

	@Override
	SqmQueryImplementor<R> setParameterList(int position, Object[] values);

	@Override
	<P> SqmQueryImplementor<R> setParameterList(int position, P[] values, Class<P> javaType);

	@Override
	<P> SqmQueryImplementor<R> setParameterList(int position, P[] values, Type<P> type);

	@Override
	<P> SqmQueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values);

	@Override
	<P> SqmQueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> SqmQueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Type<P> type);

	@Override
	<P> SqmQueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values);

	@Override
	<P> SqmQueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType);

	@Override
	<P> SqmQueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values, Type<P> type);

	@Override
	SqmQueryImplementor<R> setProperties(Object bean);

	@Override
	SqmQueryImplementor<R> setProperties(@SuppressWarnings("rawtypes") Map bean);
}
