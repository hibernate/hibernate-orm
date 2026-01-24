/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.spi;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import jakarta.persistence.metamodel.Type;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.NamedParametersNotSupportedException;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.QueryFlushMode;
import org.hibernate.query.named.NameableQuery;
import org.hibernate.query.spi.ProcedureParameterMetadataImplementor;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.QueryParameterBindings;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Steve Ebersole
 */
public interface ProcedureCallImplementor<R> extends ProcedureCall, QueryImplementor<R>, NameableQuery {
	@Override
	SharedSessionContractImplementor getSession();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Options

	@Override
	ProcedureCallImplementor<R> setFlushMode(FlushModeType flushMode);

	@Override
	ProcedureCallImplementor<R> setQueryFlushMode(QueryFlushMode queryFlushMode);

	@Override
	ProcedureCallImplementor<R> setTimeout(Integer timeout);
	@Override
	ProcedureCallImplementor<R> setTimeout(int timeout);
	@Override
	ProcedureCallImplementor<R> setTimeout(Timeout timeout);

	@Override
	ProcedureCallImplementor<R> setComment(String comment);

	@Override
	ProcedureCallImplementor<R> addQueryHint(String hint);

	@Override
	ProcedureCallImplementor<R> setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode);

	@Override
	ProcedureCallImplementor<R> setCacheStoreMode(CacheStoreMode cacheStoreMode);

	@Override
	ProcedureCallImplementor<R> setMaxResults(int maxResults);

	@Override
	ProcedureCallImplementor<R> setFirstResult(int startPosition);

	@Override
	ProcedureCallImplementor<R> setLockMode(LockModeType lockMode);

	ProcedureCallImplementor<R> setHint(String hintName, Object value);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter handling

	@Override
	ProcedureParameterMetadataImplementor getParameterMetadata();

	ParameterStrategy getParameterStrategy();

	@Override
	QueryParameterBindings getParameterBindings();

	@Override
	FunctionReturnImplementor<?> getFunctionReturn();

	@Override
	<T> FunctionReturnImplementor<T> registerResultParameter(Class<T> resultType);

	@Override
	ProcedureCallImplementor<R> registerStoredProcedureParameter(int position, Class<?> type, ParameterMode mode);

	@Override
	ProcedureCallImplementor<R> registerStoredProcedureParameter(String parameterName, Class<?> type, ParameterMode mode);

	@Override
	ProcedureCallImplementor<R> registerStoredProcedureParameter(int position, Type<?> type, ParameterMode mode);

	@Override
	ProcedureCallImplementor<R> registerStoredProcedureParameter(String parameterName, Type<?> type, ParameterMode mode);

	@Override
	<T> ProcedureParameterImplementor<T> registerConvertedParameter(int position, Class<? extends AttributeConverter<T, ?>> converter, ParameterMode mode);

	@Override
	<T> ProcedureParameterImplementor<T> registerConvertedParameter(String parameterName, Class<? extends AttributeConverter<T, ?>> converter, ParameterMode mode);

	@Override
	<T> ProcedureParameterImplementor<T> registerParameter(int position, Class<T> type, ParameterMode mode);

	@Override
	<T> ProcedureParameterImplementor<T> registerParameter(int position, Type<T> type, ParameterMode mode);

	@Override
	<T> ProcedureParameterImplementor<T> registerParameter(String parameterName, Class<T> type, ParameterMode mode)
			throws NamedParametersNotSupportedException;

	@Override
	<T> ProcedureParameterImplementor<T> registerParameter(String parameterName, Type<T> type, ParameterMode mode)
			throws NamedParametersNotSupportedException;

	@Override
	ProcedureParameterImplementor<?> getParameterRegistration(int position);

	@Override
	ProcedureParameterImplementor<?> getParameterRegistration(String name);

	@Override
	<T> ProcedureCallImplementor<R> setParameter(Parameter<T> param, T value);

	@Override @Deprecated
	ProcedureCallImplementor<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override @Deprecated
	ProcedureCallImplementor<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType);

	@Override
	ProcedureCallImplementor<R> setParameter(String name, Object value);

	@Override @Deprecated
	ProcedureCallImplementor<R> setParameter(String name, Calendar value, TemporalType temporalType);

	@Override @Deprecated
	ProcedureCallImplementor<R> setParameter(String name, Date value, TemporalType temporalType);

	@Override
	<P> ProcedureCallImplementor<R> setParameter(String name, P value, Class<P> type);

	@Override
	<P> ProcedureCallImplementor<R> setParameter(String name, P value, Type<P> type);

	@Override
	ProcedureCallImplementor<R> setParameter(int position, Object value);

	@Override
	<P> ProcedureCallImplementor<R> setParameter(int position, P value, Class<P> type);

	@Override
	<P> ProcedureCallImplementor<R> setParameter(int position, P value, Type<P> type);

	@Override @Deprecated
	ProcedureCallImplementor<R> setParameter(int position, Calendar value, TemporalType temporalType);

	@Override @Deprecated
	ProcedureCallImplementor<R> setParameter(int position, Date value, TemporalType temporalType);

	@Override
	ProcedureCallImplementor<R> setProperties(Object bean);

	@Override
	ProcedureCallImplementor<R> setProperties(Map bean);

	@Override
	<P> ProcedureCallImplementor<R> setConvertedParameter(String name, P value, Class<? extends AttributeConverter<P, ?>> converter);

	@Override
	<P> ProcedureCallImplementor<R> setConvertedParameter(int position, P value, Class<? extends AttributeConverter<P, ?>> converter);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Execution

	@Override
	List<R> getResultList();

	@Override
	R getSingleResult();

	@Override
	NamedCallableQueryMemento toMemento(String name);
}
