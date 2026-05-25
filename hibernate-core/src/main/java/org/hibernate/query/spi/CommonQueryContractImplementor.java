/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import jakarta.persistence.metamodel.Type;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.CommonQueryContract;
import jakarta.persistence.QueryFlushMode;
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
	@Nonnull
	SharedSessionContractImplementor getSession();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Options

	@Override
	@Nonnull
	CommonQueryContractImplementor setQueryFlushMode(@Nonnull QueryFlushMode queryFlushMode);

	@Override
	@Nonnull
	CommonQueryContractImplementor setComment(@Nullable String comment);

	@Override
	@Nonnull
	CommonQueryContractImplementor addQueryHint(@Nonnull String hint);

	@Override
	@Nonnull
	CommonQueryContractImplementor setTimeout(int timeout);

	@Override
	@Nonnull
	CommonQueryContractImplementor setTimeout(@Nullable Integer timeout);

	@Override
	@Nonnull
	CommonQueryContractImplementor setTimeout(@Nullable Timeout timeout);

	@Override
	@Nonnull
	CommonQueryContractImplementor setHint(@Nonnull String hintName, @Nullable Object value);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter Handling

	@Nonnull
	QueryParameterBindings getParameterBindings();

	@Override
	@Nonnull
	<T> CommonQueryContractImplementor setParameter(@Nonnull QueryParameter<T> parameter, @Nullable T value);

	@Override
	@Nonnull
	<T> CommonQueryContractImplementor setParameter(@Nonnull Parameter<T> param, @Nullable T value);

	@Override
	@Nonnull
	CommonQueryContractImplementor setParameter(@Nonnull String parameter, @Nullable Object value);

	@Override
	@Nonnull
	CommonQueryContractImplementor setParameter(int parameter, @Nullable Object value);

	@Override
	@Nonnull
	<P> CommonQueryContractImplementor setParameter(@Nonnull QueryParameter<P> parameter, @Nullable P value, @Nonnull Class<P> type);

	@Override
	@Nonnull
	<P> CommonQueryContractImplementor setParameter(@Nonnull String parameter, @Nullable P value, @Nonnull Class<P> type);

	@Override
	@Nonnull
	<P> CommonQueryContractImplementor setParameter(int parameter, @Nullable P value, @Nonnull Class<P> type);

	@Override
	@Nonnull
	<P> CommonQueryContractImplementor setParameter(@Nonnull QueryParameter<P> parameter, @Nullable P val, @Nonnull Type<P> type);

	@Override
	@Nonnull
	<P> CommonQueryContractImplementor setParameter(@Nonnull String parameter, @Nullable P value, @Nonnull Type<P> type);

	@Override
	@Nonnull
	<P> CommonQueryContractImplementor setParameter(int parameter, @Nullable P value, @Nonnull Type<P> type);

	@Override
	@Nonnull
	CommonQueryContractImplementor setProperties(@Nonnull Object bean);

	@Override
	@Nonnull
	CommonQueryContractImplementor setProperties(@SuppressWarnings("rawtypes") @Nonnull Map bean);

	@Override
	@Nonnull
	<P> CommonQueryContractImplementor setConvertedParameter(@Nonnull String name, @Nullable P value, @Nonnull Class<? extends AttributeConverter<P, ?>> converter);

	@Override
	@Nonnull
	<P> CommonQueryContractImplementor setConvertedParameter(int position, @Nullable P value, @Nonnull Class<? extends AttributeConverter<P, ?>> converter);

	@Override
	@Nonnull
	CommonQueryContractImplementor setParameterList(@Nonnull String parameter, @SuppressWarnings("rawtypes") @Nonnull Collection values);

	@Override
	@Nonnull
	<P> CommonQueryContractImplementor setParameterList(@Nonnull String parameter, @Nonnull Collection<? extends P> values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> CommonQueryContractImplementor setParameterList(@Nonnull String parameter, @Nonnull Collection<? extends P> values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	CommonQueryContractImplementor setParameterList(@Nonnull String parameter, @Nonnull Object[] values);

	@Override
	@Nonnull
	<P> CommonQueryContractImplementor setParameterList(@Nonnull String parameter, @Nonnull P[] values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> CommonQueryContractImplementor setParameterList(@Nonnull String parameter, @Nonnull P[] values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	CommonQueryContractImplementor setParameterList(int parameter, @SuppressWarnings("rawtypes") @Nonnull Collection values);

	@Override
	@Nonnull
	<P> CommonQueryContractImplementor setParameterList(int parameter, @Nonnull Collection<? extends P> values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> CommonQueryContractImplementor setParameterList(int parameter, @Nonnull Collection<? extends P> values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	CommonQueryContractImplementor setParameterList(int parameter, @Nonnull Object[] values);

	@Override
	@Nonnull
	<P> CommonQueryContractImplementor setParameterList(int parameter, @Nonnull P[] values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> CommonQueryContractImplementor setParameterList(int parameter, @Nonnull P[] values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	<P> CommonQueryContractImplementor setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull Collection<? extends P> values);

	@Override
	@Nonnull
	<P> CommonQueryContractImplementor setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull Collection<? extends P> values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> CommonQueryContractImplementor setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull Collection<? extends P> values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	<P> CommonQueryContractImplementor setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values);

	@Override
	@Nonnull
	<P> CommonQueryContractImplementor setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> CommonQueryContractImplementor setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values, @Nonnull Type<P> type);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecated stuff

	@SuppressWarnings("removal")
	@Override
	@Nonnull
	CommonQueryContractImplementor setMaxResults(int maxResult);

	@SuppressWarnings("removal")
	@Override
	@Nonnull
	CommonQueryContractImplementor setFirstResult(int startPosition);

	@Override
	@Nonnull
	CommonQueryContractImplementor setFlushMode(@Nonnull FlushModeType flushMode);

	@SuppressWarnings("removal")
	@Override
	@Nonnull
	CommonQueryContractImplementor setLockMode(@Nonnull LockModeType lockMode);

	@SuppressWarnings("removal")
	@Override
	@Nonnull
	CommonQueryContractImplementor setCacheRetrieveMode(@Nonnull CacheRetrieveMode cacheRetrieveMode);

	@SuppressWarnings("removal")
	@Override
	@Nonnull
	CommonQueryContractImplementor setCacheStoreMode(@Nonnull CacheStoreMode cacheStoreMode);

	@SuppressWarnings("deprecation")
	@Override
	@Nonnull
	CommonQueryContractImplementor setParameter(@Nonnull String parameter, @Nullable Instant value, @Nonnull TemporalType temporalType);

	@SuppressWarnings("deprecation")
	@Override
	@Nonnull
	CommonQueryContractImplementor setParameter(@Nonnull String parameter, @Nullable Calendar value, @Nonnull TemporalType temporalType);

	@SuppressWarnings("deprecation")
	@Override
	@Nonnull
	CommonQueryContractImplementor setParameter(@Nonnull String parameter, @Nullable Date value, @Nonnull TemporalType temporalType);

	@SuppressWarnings("deprecation")
	@Override
	@Nonnull
	CommonQueryContractImplementor setParameter(int parameter, @Nullable Instant value, @Nonnull TemporalType temporalType);

	@SuppressWarnings("deprecation")
	@Override
	@Nonnull
	CommonQueryContractImplementor setParameter(int parameter, @Nullable Date value, @Nonnull TemporalType temporalType);

	@SuppressWarnings("deprecation")
	@Override
	@Nonnull
	CommonQueryContractImplementor setParameter(int parameter, @Nullable Calendar value, @Nonnull TemporalType temporalType);

	@SuppressWarnings("deprecation")
	@Override
	@Nonnull
	CommonQueryContractImplementor setParameter(@Nonnull Parameter<Calendar> param, @Nullable Calendar value, @Nonnull TemporalType temporalType);

	@SuppressWarnings("deprecation")
	@Override
	@Nonnull
	CommonQueryContractImplementor setParameter(@Nonnull Parameter<Date> param, @Nullable Date value, @Nonnull TemporalType temporalType);
}
