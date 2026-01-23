/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import jakarta.persistence.metamodel.Type;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.CommonQueryContract;
import org.hibernate.query.QueryFlushMode;
import org.hibernate.query.QueryParameter;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/// SPI form of CommonQueryContract
///
/// @author Steve Ebersole
public interface CommonQueryContractImplementor extends CommonQueryContract {
	@Override
	SharedSessionContractImplementor getSession();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Options

	@Override
	CommonQueryContractImplementor setQueryFlushMode(QueryFlushMode queryFlushMode);

	@Override
	CommonQueryContractImplementor setComment(String comment);

	@Override
	CommonQueryContractImplementor addQueryHint(String hint);

	@Override
	CommonQueryContractImplementor setTimeout(int timeout);

	@Override
	CommonQueryContractImplementor setTimeout(Integer timeout);

	@Override
	CommonQueryContractImplementor setTimeout(Timeout timeout);

	@Override
	CommonQueryContractImplementor setHint(String hintName, Object value);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Casts

	@Override
	SelectionQueryImplementor<?> asSelectionQuery();

	@Override
	<X> SelectionQueryImplementor<X> asSelectionQuery(Class<X> type);

	@Override
	<X> SelectionQueryImplementor<X> asSelectionQuery(EntityGraph<X> entityGraph);

	@Override
	<X> SelectionQueryImplementor<X> ofType(Class<X> type);

	@Override
	<X> SelectionQueryImplementor<X> withEntityGraph(EntityGraph<X> entityGraph);

	@Override
	MutationQueryImplementor<?> asMutationQuery();

	@Override
	MutationQueryImplementor<?> asStatement();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter Handling

	QueryParameterBindings getParameterBindings();

	@Override
	<T> CommonQueryContractImplementor setParameter(QueryParameter<T> parameter, T value);

	@Override
	<T> CommonQueryContractImplementor setParameter(Parameter<T> param, T value);

	@Override
	CommonQueryContractImplementor setParameter(String parameter, Object value);

	@Override
	CommonQueryContractImplementor setParameter(int parameter, Object value);

	@Override
	<P> CommonQueryContractImplementor setParameter(QueryParameter<P> parameter, P value, Class<P> type);

	@Override
	<P> CommonQueryContractImplementor setParameter(String parameter, P value, Class<P> type);

	@Override
	<P> CommonQueryContractImplementor setParameter(int parameter, P value, Class<P> type);

	@Override
	<P> CommonQueryContractImplementor setParameter(QueryParameter<P> parameter, P val, Type<P> type);

	@Override
	<P> CommonQueryContractImplementor setParameter(String parameter, P value, Type<P> type);

	@Override
	<P> CommonQueryContractImplementor setParameter(int parameter, P value, Type<P> type);

	@Override
	CommonQueryContractImplementor setProperties(Object bean);

	@Override
	CommonQueryContractImplementor setProperties(@SuppressWarnings("rawtypes") Map bean);

	@Override
	<P> CommonQueryContractImplementor setConvertedParameter(String name, P value, Class<? extends AttributeConverter<P, ?>> converter);

	@Override
	<P> CommonQueryContractImplementor setConvertedParameter(int position, P value, Class<? extends AttributeConverter<P, ?>> converter);

	@Override
	CommonQueryContractImplementor setParameterList(String parameter, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> CommonQueryContractImplementor setParameterList(String parameter, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> CommonQueryContractImplementor setParameterList(String parameter, Collection<? extends P> values, Type<P> type);

	@Override
	CommonQueryContractImplementor setParameterList(String parameter, Object[] values);

	@Override
	<P> CommonQueryContractImplementor setParameterList(String parameter, P[] values, Class<P> javaType);

	@Override
	<P> CommonQueryContractImplementor setParameterList(String parameter, P[] values, Type<P> type);

	@Override
	CommonQueryContractImplementor setParameterList(int parameter, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> CommonQueryContractImplementor setParameterList(int parameter, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> CommonQueryContractImplementor setParameterList(int parameter, Collection<? extends P> values, Type<P> type);

	@Override
	CommonQueryContractImplementor setParameterList(int parameter, Object[] values);

	@Override
	<P> CommonQueryContractImplementor setParameterList(int parameter, P[] values, Class<P> javaType);

	@Override
	<P> CommonQueryContractImplementor setParameterList(int parameter, P[] values, Type<P> type);

	@Override
	<P> CommonQueryContractImplementor setParameterList(QueryParameter<P> parameter, Collection<? extends P> values);

	@Override
	<P> CommonQueryContractImplementor setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> CommonQueryContractImplementor setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Type<P> type);

	@Override
	<P> CommonQueryContractImplementor setParameterList(QueryParameter<P> parameter, P[] values);

	@Override
	<P> CommonQueryContractImplementor setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType);

	@Override
	<P> CommonQueryContractImplementor setParameterList(QueryParameter<P> parameter, P[] values, Type<P> type);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecated stuff

	@SuppressWarnings("removal")
	@Override
	CommonQueryContractImplementor setMaxResults(int maxResult);

	@SuppressWarnings("removal")
	@Override
	CommonQueryContractImplementor setFirstResult(int startPosition);

	@Override
	CommonQueryContractImplementor setFlushMode(FlushModeType flushMode);

	@SuppressWarnings("removal")
	@Override
	CommonQueryContractImplementor setLockMode(LockModeType lockMode);

	@SuppressWarnings("removal")
	@Override
	CommonQueryContractImplementor setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode);

	@SuppressWarnings("removal")
	@Override
	CommonQueryContractImplementor setCacheStoreMode(CacheStoreMode cacheStoreMode);

	@SuppressWarnings("deprecation")
	@Override
	CommonQueryContractImplementor setParameter(String parameter, Instant value, TemporalType temporalType);

	@SuppressWarnings("deprecation")
	@Override
	CommonQueryContractImplementor setParameter(String parameter, Calendar value, TemporalType temporalType);

	@SuppressWarnings("deprecation")
	@Override
	CommonQueryContractImplementor setParameter(String parameter, Date value, TemporalType temporalType);

	@SuppressWarnings("deprecation")
	@Override
	CommonQueryContractImplementor setParameter(int parameter, Instant value, TemporalType temporalType);

	@SuppressWarnings("deprecation")
	@Override
	CommonQueryContractImplementor setParameter(int parameter, Date value, TemporalType temporalType);

	@SuppressWarnings("deprecation")
	@Override
	CommonQueryContractImplementor setParameter(int parameter, Calendar value, TemporalType temporalType);

	@SuppressWarnings("deprecation")
	@Override
	CommonQueryContractImplementor setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@SuppressWarnings("deprecation")
	@Override
	CommonQueryContractImplementor setParameter(Parameter<Date> param, Date value, TemporalType temporalType);
}
