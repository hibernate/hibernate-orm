/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
import jakarta.persistence.QueryFlushMode;
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
	@Nonnull
	SharedSessionContractImplementor getSession();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Options

	@Override
	@Nonnull
	ProcedureCallImplementor<R> setFlushMode(@Nonnull FlushModeType flushMode);

	@Override
	@Nonnull
	ProcedureCallImplementor<R> setQueryFlushMode(@Nonnull QueryFlushMode queryFlushMode);

	@Override
	@Nonnull
	ProcedureCallImplementor<R> setTimeout(@Nullable Integer timeout);
	@Override
	@Nonnull
	ProcedureCallImplementor<R> setTimeout(int timeout);
	@Override
	@Nonnull
	ProcedureCallImplementor<R> setTimeout(@Nullable Timeout timeout);

	@Override
	@Nonnull
	ProcedureCallImplementor<R> setComment(@Nullable String comment);

	@Override
	@Nonnull
	ProcedureCallImplementor<R> addQueryHint(@Nonnull String hint);

	@Override
	@Nonnull
	ProcedureCallImplementor<R> setCacheRetrieveMode(@Nonnull CacheRetrieveMode cacheRetrieveMode);

	@Override
	@Nonnull
	ProcedureCallImplementor<R> setCacheStoreMode(@Nonnull CacheStoreMode cacheStoreMode);

	@Override
	@Nonnull
	ProcedureCallImplementor<R> setMaxResults(int maxResults);

	@Override
	@Nonnull
	ProcedureCallImplementor<R> setFirstResult(int startPosition);

	@Override
	@Nonnull
	ProcedureCallImplementor<R> setLockMode(@Nonnull LockModeType lockMode);

	@Nonnull
	ProcedureCallImplementor<R> setHint(@Nonnull String hintName, @Nullable Object value);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter handling

	@Override
	@Nonnull
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
	@Nonnull
	<T> ProcedureCallImplementor<R> setParameter(@Nonnull Parameter<T> param, @Nullable T value);

	@Override @Deprecated
	@Nonnull
	ProcedureCallImplementor<R> setParameter(@Nonnull Parameter<Calendar> param, @Nullable Calendar value, @Nonnull TemporalType temporalType);

	@Override @Deprecated
	@Nonnull
	ProcedureCallImplementor<R> setParameter(@Nonnull Parameter<Date> param, @Nullable Date value, @Nonnull TemporalType temporalType);

	@Override
	@Nonnull
	ProcedureCallImplementor<R> setParameter(@Nonnull String name, @Nullable Object value);

	@Override @Deprecated
	@Nonnull
	ProcedureCallImplementor<R> setParameter(@Nonnull String name, @Nullable Calendar value, @Nonnull TemporalType temporalType);

	@Override @Deprecated
	@Nonnull
	ProcedureCallImplementor<R> setParameter(@Nonnull String name, @Nullable Date value, @Nonnull TemporalType temporalType);

	@Override
	@Nonnull
	<P> ProcedureCallImplementor<R> setParameter(@Nonnull String name, @Nullable P value, @Nonnull Class<P> type);

	@Override
	@Nonnull
	<P> ProcedureCallImplementor<R> setParameter(@Nonnull String name, @Nullable P value, @Nonnull Type<P> type);

	@Override
	@Nonnull
	ProcedureCallImplementor<R> setParameter(int position, @Nullable Object value);

	@Override
	@Nonnull
	<P> ProcedureCallImplementor<R> setParameter(int position, @Nullable P value, @Nonnull Class<P> type);

	@Override
	@Nonnull
	<P> ProcedureCallImplementor<R> setParameter(int position, @Nullable P value, @Nonnull Type<P> type);

	@Override @Deprecated
	@Nonnull
	ProcedureCallImplementor<R> setParameter(int position, @Nullable Calendar value, @Nonnull TemporalType temporalType);

	@Override @Deprecated
	@Nonnull
	ProcedureCallImplementor<R> setParameter(int position, @Nullable Date value, @Nonnull TemporalType temporalType);

	@Override
	@Nonnull
	ProcedureCallImplementor<R> setProperties(@Nonnull Object bean);

	@Override
	@Nonnull
	ProcedureCallImplementor<R> setProperties(@SuppressWarnings("rawtypes") @Nonnull Map bean);

	@Override
	@Nonnull
	<P> ProcedureCallImplementor<R> setConvertedParameter(@Nonnull String name, @Nullable P value, @Nonnull Class<? extends AttributeConverter<P, ?>> converter);

	@Override
	@Nonnull
	<P> ProcedureCallImplementor<R> setConvertedParameter(int position, @Nullable P value, @Nonnull Class<? extends AttributeConverter<P, ?>> converter);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Execution

	@Override
	@Nonnull
	List<R> getResultList();

	@Override
	R getSingleResult();

	@Override
	NamedCallableQueryMemento toMemento(String name);
}
