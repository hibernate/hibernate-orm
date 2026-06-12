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
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import jakarta.persistence.metamodel.Type;
import org.hibernate.CacheMode;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.query.IllegalMutationQueryException;
import org.hibernate.query.Page;
import jakarta.persistence.QueryFlushMode;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.named.TypedQueryReferenceProducer;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * SPI form of {@linkplain SelectionQuery}
 *
 * @author Steve Ebersole
 */
public interface SelectionQueryImplementor<R>
		extends SelectionQuery<R>, QueryImplementor<R>, TypedQueryReferenceProducer {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Options

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	SelectionQueryImplementor<R> setEntityGraph(@Nonnull EntityGraph<? super R> entityGraph);

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	SelectionQueryImplementor<R> setEntityGraph(@Nonnull EntityGraph<? super R> graph, @Nonnull GraphSemantic semantic);

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	SelectionQueryImplementor<R> enableFetchProfile(@Nonnull String profileName);

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	SelectionQueryImplementor<R> disableFetchProfile(@Nonnull String profileName);

	@Override
	@Nonnull
	SelectionQueryImplementor<R> setFlushMode(@Nonnull FlushModeType flushMode);

	@Override
	@Nonnull
	SelectionQueryImplementor<R> setQueryFlushMode(@Nonnull QueryFlushMode queryFlushMode);

	@Override
	@Nonnull
	SelectionQueryImplementor<R> setTimeout(int timeout);

	@Override
	@Nonnull
	SelectionQueryImplementor<R> setTimeout(@Nullable Integer timeout);

	@Override
	@Nonnull
	SelectionQueryImplementor<R> setTimeout(@Nullable Timeout timeout);

	@Override
	@Nonnull
	SelectionQueryImplementor<R> setComment(@Nullable String comment);

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	SelectionQueryImplementor<R> setFetchSize(int fetchSize);

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	SelectionQueryImplementor<R> setReadOnly(boolean readOnly);

	@Override
	@Nonnull
	SelectionQueryImplementor<R> setMaxResults(int maxResults);

	@Override
	@Nonnull
	SelectionQueryImplementor<R> setFirstResult(int startPosition);

	@Override
	SelectionQueryImplementor<R> setPage(Page page);

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	SelectionQueryImplementor<R> setCacheMode(@Nonnull CacheMode cacheMode);

	@Override
	@Nonnull
	SelectionQueryImplementor<R> setCacheStoreMode(@Nonnull CacheStoreMode cacheStoreMode);

	@Override
	@Nonnull
	SelectionQueryImplementor<R> setCacheRetrieveMode(@Nonnull CacheRetrieveMode cacheRetrieveMode);

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	SelectionQueryImplementor<R> setCacheable(boolean cacheable);

	@Override
	@Nonnull
	SelectionQueryImplementor<R> setQueryPlanCacheable(boolean queryPlanCacheable);

	@Override
	@SuppressWarnings("removal")
	SelectionQueryImplementor<R> setCacheRegion(@Nullable String cacheRegion);

	@Override
	@Nonnull
	SelectionQueryImplementor<R> setLockMode(@Nonnull LockModeType lockMode);

	@Override
	SelectionQueryImplementor<R> setLockTimeout(Timeout lockTimeout);

	@Override
	@Nonnull
	SelectionQueryImplementor<R> setHibernateLockMode(@Nonnull LockMode lockMode);

	@Override
	@Nonnull
	SelectionQueryImplementor<R> setFollowOnStrategy(@Nonnull Locking.FollowOn followOnStrategy);

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	<T> SelectionQueryImplementor<T> setTupleTransformer(@Nonnull TupleTransformer<T> transformer);

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	SelectionQueryImplementor<R> setResultListTransformer(@Nonnull ResultListTransformer<R> transformer);

	@Override
	@Nonnull
	SelectionQueryImplementor<R> setLockScope(@Nonnull PessimisticLockScope lockScope);

	@Override
	@Nonnull
	SelectionQueryImplementor<R> addQueryHint(@Nonnull String hint);

	@Override
	@Nonnull
	SelectionQueryImplementor<R> setHint(@Nonnull String hintName, @Nullable Object value);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter Handling

	@Override
	@Nonnull
	SelectionQueryImplementor<R> setParameter(@Nonnull String name, @Nullable Object value);

	@Override
	@Nonnull
	<P> SelectionQueryImplementor<R> setParameter(@Nonnull String name, @Nullable P value, @Nonnull Class<P> type);

	@Override
	@Nonnull
	<P> SelectionQueryImplementor<R> setParameter(@Nonnull String name, @Nullable P value, @Nonnull Type<P> type);

	@Override
	@Nonnull
	SelectionQueryImplementor<R> setParameter(int position, @Nullable Object value);

	@Override
	@Nonnull
	SelectionQueryImplementor<R> setParameters(@Nonnull Object... arguments);

	@Override
	@Nonnull
	<P> SelectionQueryImplementor<R> setParameter(int position, @Nullable P value, @Nonnull Class<P> type);

	@Override
	@Nonnull
	<P> SelectionQueryImplementor<R> setParameter(int position, @Nullable P value, @Nonnull Type<P> type);

	@Override
	@Nonnull
	<T> SelectionQueryImplementor<R> setParameter(@Nonnull QueryParameter<T> parameter, @Nullable T value);

	@Override
	@Nonnull
	<P> SelectionQueryImplementor<R> setParameter(@Nonnull QueryParameter<P> parameter, @Nullable P value, @Nonnull Class<P> type);

	@Override
	@Nonnull
	<P> SelectionQueryImplementor<R> setParameter(@Nonnull QueryParameter<P> parameter, @Nullable P val, @Nonnull Type<P> type);

	@Override
	@Nonnull
	<T> SelectionQueryImplementor<R> setParameter(@Nonnull Parameter<T> param, @Nullable T value);

	@Override
	@Nonnull
	SelectionQueryImplementor<R> setProperties(@Nonnull Object bean);

	@Override
	@Nonnull
	SelectionQueryImplementor<R> setProperties(@SuppressWarnings("rawtypes") @Nonnull Map bean);

	@Override
	@Nonnull
	<P> SelectionQueryImplementor<R> setConvertedParameter(@Nonnull String name, @Nullable P value, @Nonnull Class<? extends AttributeConverter<P, ?>> converter);

	@Override
	@Nonnull
	<P> SelectionQueryImplementor<R> setConvertedParameter(int position, @Nullable P value, @Nonnull Class<? extends AttributeConverter<P, ?>> converter);

	@Override
	@Nonnull
	SelectionQueryImplementor<R> setParameterList(@Nonnull String name, @SuppressWarnings("rawtypes") @Nonnull Collection values);

	@Override
	@Nonnull
	<P> SelectionQueryImplementor<R> setParameterList(@Nonnull String name, @Nonnull Collection<? extends P> values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> SelectionQueryImplementor<R> setParameterList(@Nonnull String name, @Nonnull Collection<? extends P> values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	SelectionQueryImplementor<R> setParameterList(@Nonnull String name, @Nonnull Object[] values);

	@Override
	@Nonnull
	<P> SelectionQueryImplementor<R> setParameterList(@Nonnull String name, @Nonnull P[] values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> SelectionQueryImplementor<R> setParameterList(@Nonnull String name, @Nonnull P[] values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	SelectionQueryImplementor<R> setParameterList(int position, @SuppressWarnings("rawtypes") @Nonnull Collection values);

	@Override
	@Nonnull
	<P> SelectionQueryImplementor<R> setParameterList(int position, @Nonnull Collection<? extends P> values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> SelectionQueryImplementor<R> setParameterList(int position, @Nonnull Collection<? extends P> values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	SelectionQueryImplementor<R> setParameterList(int position, @Nonnull Object[] values);

	@Override
	@Nonnull
	<P> SelectionQueryImplementor<R> setParameterList(int position, @Nonnull P[] values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> SelectionQueryImplementor<R> setParameterList(int position, @Nonnull P[] values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	<P> SelectionQueryImplementor<R> setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull Collection<? extends P> values);

	@Override
	@Nonnull
	<P> SelectionQueryImplementor<R> setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull Collection<? extends P> values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> SelectionQueryImplementor<R> setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull Collection<? extends P> values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	<P> SelectionQueryImplementor<R> setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values);

	@Override
	@Nonnull
	<P> SelectionQueryImplementor<R> setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> SelectionQueryImplementor<R> setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values, @Nonnull Type<P> type);

	@Override @Deprecated
	@Nonnull
	SelectionQueryImplementor<R> setParameter(@Nonnull String name, @Nullable Instant value, @Nonnull TemporalType temporalType);

	@Override @Deprecated
	@Nonnull
	SelectionQueryImplementor<R> setParameter(@Nonnull String name, @Nullable Calendar value, @Nonnull TemporalType temporalType);

	@Override @Deprecated
	@Nonnull
	SelectionQueryImplementor<R> setParameter(@Nonnull String name, @Nullable Date value, @Nonnull TemporalType temporalType);

	@Override @Deprecated
	@Nonnull
	SelectionQueryImplementor<R> setParameter(int position, @Nullable Instant value, @Nonnull TemporalType temporalType);

	@Override @Deprecated
	@Nonnull
	SelectionQueryImplementor<R> setParameter(int position, @Nullable Date value, @Nonnull TemporalType temporalType);

	@Override @Deprecated
	@Nonnull
	SelectionQueryImplementor<R> setParameter(int position, @Nullable Calendar value, @Nonnull TemporalType temporalType);

	@Override @Deprecated
	@Nonnull
	SelectionQueryImplementor<R> setParameter(@Nonnull Parameter<Calendar> param, @Nullable Calendar value, @Nonnull TemporalType temporalType);

	@Override @Deprecated
	@Nonnull
	SelectionQueryImplementor<R> setParameter(@Nonnull Parameter<Date> param, @Nullable Date value, @Nonnull TemporalType temporalType);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// MutationQuery Handling

	@Override
	@Nonnull
	default MutationQueryImplementor<R> asMutationQuery() {
		throw new IllegalMutationQueryException( "SelectionQuery cannot be treated as a MutationQuery", getQueryString() );
	}

	@Override
	@Deprecated
	@SuppressWarnings("removal")
	default int executeUpdate() {
		// per JPA, again, needs to be IllegalStateException
		throw new IllegalStateException( "SelectionQuery cannot be treated as a MutationQuery - " + getQueryString() );
	}
}
