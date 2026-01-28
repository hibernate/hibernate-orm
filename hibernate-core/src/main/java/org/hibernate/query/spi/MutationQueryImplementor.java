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
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import jakarta.persistence.metamodel.Type;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.ScrollMode;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.QueryFlushMode;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.named.StatementReferenceProducer;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * SPI form of {@linkplain MutationQuery}
 *
 * @author Steve Ebersole
 */
public interface MutationQueryImplementor<T> extends MutationQuery, QueryImplementor<T>, StatementReferenceProducer {
	@Override
	default String getMutationString() {
		return getQueryString();
	}

	@Override @Nullable
	Class<T> getTargetType();

	@Override
	default MutationQueryImplementor<T> asStatement() {
		return this;
	}

	@Override
	default MutationQueryImplementor<T> asMutationQuery() {
		return this;
	}

	@Override
	MutationQueryImplementor<T> setTimeout(int timeout);

	@Override
	MutationQueryImplementor<T> setTimeout(Integer integer);

	@Override
	MutationQueryImplementor<T> setTimeout(Timeout timeout);

	@Override
	MutationQueryImplementor<T> setComment(String comment);

	@Override
	MutationQueryImplementor<T> setQueryFlushMode(QueryFlushMode queryFlushMode);

	@Override
	MutationQueryImplementor<T> addQueryHint(String hint);

	@Override
	MutationQueryImplementor<T> setHint(String hintName, Object value);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameters

	@Override
	ParameterMetadataImplementor getParameterMetadata();

	@Override
	MutationQueryImplementor<T> setParameter(String name, Object value);

	@Override
	<P> MutationQueryImplementor<T> setParameter(String name, P value, Class<P> type);

	@Override
	<P> MutationQueryImplementor<T> setParameter(String name, P value, Type<P> type);

	@Override
	MutationQueryImplementor<T> setParameter(int position, Object value);

	@Override
	<P> MutationQueryImplementor<T> setParameter(int position, P value, Class<P> type);

	@Override
	<P> MutationQueryImplementor<T> setParameter(int position, P value, Type<P> type);

	@Override
	<P> MutationQueryImplementor<T> setParameter(QueryParameter<P> parameter, P value);

	@Override
	<P> MutationQueryImplementor<T> setParameter(QueryParameter<P> parameter, P value, Class<P> type);

	@Override
	<P> MutationQueryImplementor<T> setParameter(QueryParameter<P> parameter, P val, Type<P> type);

	@Override
	<P> MutationQueryImplementor<T> setParameter(Parameter<P> param, P value);

	@Override
	MutationQueryImplementor<T> setParameterList(String name, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> MutationQueryImplementor<T> setParameterList(String name, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> MutationQueryImplementor<T> setParameterList(String name, Collection<? extends P> values, Type<P> type);

	@Override
	MutationQueryImplementor<T> setParameterList(String name, Object[] values);

	@Override
	<P> MutationQueryImplementor<T> setParameterList(String name, P[] values, Class<P> javaType);

	@Override
	<P> MutationQueryImplementor<T> setParameterList(String name, P[] values, Type<P> type);

	@Override
	MutationQueryImplementor<T> setParameterList(int position, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> MutationQueryImplementor<T> setParameterList(int position, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> MutationQueryImplementor<T> setParameterList(int position, Collection<? extends P> values, Type<P> type);

	@Override
	MutationQueryImplementor<T> setParameterList(int position, Object[] values);

	@Override
	<P> MutationQueryImplementor<T> setParameterList(int position, P[] values, Class<P> javaType);

	@Override
	<P> MutationQueryImplementor<T> setParameterList(int position, P[] values, Type<P> type);

	@Override
	<P> MutationQueryImplementor<T> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values);

	@Override
	<P> MutationQueryImplementor<T> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> MutationQueryImplementor<T> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Type<P> type);

	@Override
	<P> MutationQueryImplementor<T> setParameterList(QueryParameter<P> parameter, P[] values);

	@Override
	<P> MutationQueryImplementor<T> setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType);

	@Override
	<P> MutationQueryImplementor<T> setParameterList(QueryParameter<P> parameter, P[] values, Type<P> type);

	@Override
	MutationQueryImplementor<T> setProperties(Object bean);

	@Override
	MutationQueryImplementor<T> setProperties(@SuppressWarnings("rawtypes") Map bean);

	@Override
	<P> MutationQueryImplementor<T> setConvertedParameter(String name, P value, Class<? extends AttributeConverter<P, ?>> converter);

	@Override
	<P> MutationQueryImplementor<T> setConvertedParameter(int position, P value, Class<? extends AttributeConverter<P, ?>> converter);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SelectionQuery stuff

	@Override
	default <X> SelectionQueryImplementor<X> ofType(Class<X> aClass) {
		throw new IllegalStateException( "MutationQuery cannot be treated as a SelectionQuery - " + getMutationString() );
	}

	@Override
	default<X> SelectionQueryImplementor<X> withEntityGraph(EntityGraph<X> entityGraph) {
		// IllegalStateException is the type required by JPA
		throw new IllegalStateException( "MutationQuery cannot be treated as a SelectionQuery - " + getMutationString() );
	}

	@Override @SuppressWarnings("removal")
	default LockModeType getLockMode() {
		// IllegalStateException is the type required by JPA
		throw new IllegalStateException( "MutationQuery cannot be treated as a SelectionQuery - " + getMutationString() );
	}

	@Override
	default QueryImplementor<T> setLockMode(LockModeType lockMode) {
		// IllegalStateException is the type required by JPA
		throw new IllegalStateException( "MutationQuery cannot be treated as a SelectionQuery - " + getMutationString() );
	}

	@Override
	default QueryImplementor<T> setLockScope(PessimisticLockScope lockScope) {
		// IllegalStateException is the type required by JPA
		throw new IllegalStateException( "MutationQuery cannot be treated as a SelectionQuery - " + getMutationString() );
	}

	@Override @SuppressWarnings("removal")
	default CacheStoreMode getCacheStoreMode() {
		throw new IllegalStateException( "MutationQuery cannot be treated as a SelectionQuery - " + getMutationString() );
	}

	@Override
	default QueryImplementor<T> setCacheStoreMode(CacheStoreMode cacheStoreMode) {
		// IllegalStateException is the type required by JPA
		throw new IllegalStateException( "MutationQuery cannot be treated as a SelectionQuery - " + getMutationString() );
	}

	@Override @SuppressWarnings("removal")
	default CacheRetrieveMode getCacheRetrieveMode() {
		throw new IllegalStateException( "MutationQuery cannot be treated as a SelectionQuery - " + getMutationString() );
	}

	@Override
	default QueryImplementor<T> setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
		// IllegalStateException is the type required by JPA
		throw new IllegalStateException( "MutationQuery cannot be treated as a SelectionQuery - " + getMutationString() );
	}

	@Override @SuppressWarnings("removal")
	default int getMaxResults() {
		throw new IllegalStateException( "MutationQuery cannot be treated as a SelectionQuery - " + getMutationString() );
	}

	@Override
	default QueryImplementor<T> setMaxResults(int maxResults) {
		// IllegalStateException is the type required by JPA
		throw new IllegalStateException( "MutationQuery cannot be treated as a SelectionQuery - " + getMutationString() );
	}


	@Override @SuppressWarnings("removal")
	default int getFirstResult() {
		throw new IllegalStateException( "MutationQuery cannot be treated as a SelectionQuery - " + getMutationString() );
	}

	@Override
	default QueryImplementor<T> setFirstResult(int startPosition) {
		// IllegalStateException is the type required by JPA
		throw new IllegalStateException( "MutationQuery cannot be treated as a SelectionQuery - " + getMutationString() );
	}

	@Override
	default ScrollableResultsImplementor<T> scroll() {
		// IllegalStateException is the type required by JPA
		throw new IllegalStateException( "MutationQuery cannot be treated as a SelectionQuery - " + getMutationString() );
	}

	@Override
	default ScrollableResultsImplementor<T> scroll(ScrollMode scrollMode) {
		// IllegalStateException is the type required by JPA
		throw new IllegalStateException( "MutationQuery cannot be treated as a SelectionQuery - " + getMutationString() );
	}

	@Override
	default List<T> list() {
		// IllegalStateException is the type required by JPA
		throw new IllegalStateException( "MutationQuery cannot be treated as a SelectionQuery - " + getMutationString() );
	}

	@Override @SuppressWarnings("deprecation")
	default List<T> getResultList() {
		// IllegalStateException is the type required by JPA
		throw new IllegalStateException( "MutationQuery cannot be treated as a SelectionQuery - " + getMutationString()  );	}

	@Override @SuppressWarnings("deprecation")
	default Stream<T> getResultStream() {
		// IllegalStateException is the type required by JPA
		throw new IllegalStateException( "MutationQuery cannot be treated as a SelectionQuery - " + getMutationString()  );	}


	@Override @SuppressWarnings("deprecation")
	default Stream<T> stream() {
		// IllegalStateException is the type required by JPA
		throw new IllegalStateException( "MutationQuery cannot be treated as a SelectionQuery - " + getMutationString()  );
	}

	@Override
	default T uniqueResult() {
		// IllegalStateException is the type required by JPA
		throw new IllegalStateException( "MutationQuery cannot be treated as a SelectionQuery - " + getMutationString()  );
	}

	@Override
	default Optional<T> uniqueResultOptional() {
		// IllegalStateException is the type required by JPA
		throw new IllegalStateException( "MutationQuery cannot be treated as a SelectionQuery - " + getMutationString()  );
	}

	@Override
	default T getSingleResult() {
		// IllegalStateException is the type required by JPA
		throw new IllegalStateException( "MutationQuery cannot be treated as a SelectionQuery - " + getMutationString()  );
	}


	@Override @SuppressWarnings("removal")
	default T getSingleResultOrNull() {
		// IllegalStateException is the type required by JPA
		throw new IllegalStateException( "MutationQuery cannot be treated as a SelectionQuery - " + getMutationString()  );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// deprecations


	@Override @SuppressWarnings("deprecation")
	MutationQueryImplementor<T> setFlushMode(FlushModeType flushMode);

	@Override @Deprecated
	MutationQueryImplementor<T> setParameter(String name, Instant value, TemporalType temporalType);

	@Override @Deprecated
	MutationQueryImplementor<T> setParameter(String name, Calendar value, TemporalType temporalType);

	@Override @Deprecated
	MutationQueryImplementor<T> setParameter(String name, Date value, TemporalType temporalType);

	@Override @Deprecated
	MutationQueryImplementor<T> setParameter(int position, Instant value, TemporalType temporalType);

	@Override @Deprecated
	MutationQueryImplementor<T> setParameter(int position, Date value, TemporalType temporalType);

	@Override @Deprecated
	MutationQueryImplementor<T> setParameter(int position, Calendar value, TemporalType temporalType);

	@Override @Deprecated
	MutationQueryImplementor<T> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override @Deprecated
	MutationQueryImplementor<T> setParameter(Parameter<Date> param, Date value, TemporalType temporalType);
}
