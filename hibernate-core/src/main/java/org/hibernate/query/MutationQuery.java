/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.hibernate.FlushMode;
import org.hibernate.Incubating;

import jakarta.persistence.FlushModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;

/**
 * Within the context of an active {@linkplain org.hibernate.Session session},
 * an instance of this type represents an executable mutation query, that is,
 * an {@code insert}, {@code update}, or {@code delete}. It is a slimmed-down
 * version of {@link Query}, providing only methods relevant to mutation queries.
 * <p>
 * A {@code MutationQuery} may be obtained from the {@link org.hibernate.Session}
 * by calling:
 * <ul>
 * <li>{@link QueryProducer#createMutationQuery(String)}, passing the HQL as a
 *     string,
 * <li>{@link QueryProducer#createNativeMutationQuery(String)}, passing native
 *     SQL as a string,
 * <li>{@link QueryProducer#createMutationQuery(jakarta.persistence.criteria.CriteriaUpdate)} or
 *     {@link QueryProducer#createMutationQuery(jakarta.persistence.criteria.CriteriaDelete)},
 *     passing a criteria update or delete object, or
 * <li>{@link QueryProducer#createNamedMutationQuery(String)}, passing the
 *     name of a query defined using {@link jakarta.persistence.NamedQuery} or
 *     {@link jakarta.persistence.NamedNativeQuery}.
 * </ul>
 * <p>
 * A {@code MutationQuery} controls how the mutation query is executed, and
 * allows arguments to be bound to its parameters.
 * <ul>
 * <li>Mutation queries must be executed using {@link #executeUpdate()}.
 * <li>The various overloads of {@link #setParameter(String, Object)} and
 *     {@link #setParameter(int, Object)} allow arguments to be bound to named
 *     and ordinal parameters defined by the query.
 * </ul>
 *
 * @author Steve Ebersole
 */
@Incubating
public interface MutationQuery extends CommonQueryContract {

	/**
	 * Execute an insert, update, or delete statement, and return the
	 * number of affected entities.
	 * <p>
	 * For use with instances of {@code MutationQuery} created using
	 * {@link QueryProducer#createMutationQuery(String)},
	 * {@link QueryProducer#createNamedMutationQuery(String)},
	 * {@link QueryProducer#createNativeMutationQuery(String)},
	 * {@link QueryProducer#createQuery(jakarta.persistence.criteria.CriteriaUpdate)}, or
	 * {@link QueryProducer#createQuery(jakarta.persistence.criteria.CriteriaDelete)}.
	 *
	 * @return the number of affected entity instances
	 *         (may differ from the number of affected rows)
	 *
	 * @see QueryProducer#createMutationQuery(String)
	 * @see QueryProducer#createNamedMutationQuery(String)
	 * @see QueryProducer#createNativeMutationQuery(String)
	 *
	 * @see jakarta.persistence.Query#executeUpdate()
	 */
	int executeUpdate();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Covariant returns

	@Override @Deprecated(since = "7")
	MutationQuery setFlushMode(FlushModeType flushMode);

	@Override @Deprecated(since = "7")
	MutationQuery setHibernateFlushMode(FlushMode flushMode);

	@Override
	MutationQuery setTimeout(int timeout);

	@Override
	MutationQuery setComment(String comment);

	@Override
	MutationQuery setHint(String hintName, Object value);

	@Override
	MutationQuery setParameter(String name, Object value);

	@Override
	<P> MutationQuery setParameter(String name, P value, Class<P> type);

	@Override
	<P> MutationQuery setParameter(String name, P value, BindableType<P> type);

	@Override
	MutationQuery setParameter(String name, Instant value, TemporalType temporalType);

	@Override
	MutationQuery setParameter(String name, Calendar value, TemporalType temporalType);

	@Override
	MutationQuery setParameter(String name, Date value, TemporalType temporalType);

	@Override
	MutationQuery setParameter(int position, Object value);

	@Override
	<P> MutationQuery setParameter(int position, P value, Class<P> type);

	@Override
	<P> MutationQuery setParameter(int position, P value, BindableType<P> type);

	@Override
	MutationQuery setParameter(int position, Instant value, TemporalType temporalType);

	@Override
	MutationQuery setParameter(int position, Date value, TemporalType temporalType);

	@Override
	MutationQuery setParameter(int position, Calendar value, TemporalType temporalType);

	@Override
	<T> MutationQuery setParameter(QueryParameter<T> parameter, T value);

	@Override
	<P> MutationQuery setParameter(QueryParameter<P> parameter, P value, Class<P> type);

	@Override
	<P> MutationQuery setParameter(QueryParameter<P> parameter, P val, BindableType<P> type);

	@Override
	<T> MutationQuery setParameter(Parameter<T> param, T value);

	@Override
	MutationQuery setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override
	MutationQuery setParameter(Parameter<Date> param, Date value, TemporalType temporalType);

	@Override
	MutationQuery setParameterList(String name, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> MutationQuery setParameterList(String name, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> MutationQuery setParameterList(String name, Collection<? extends P> values, BindableType<P> type);

	@Override
	MutationQuery setParameterList(String name, Object[] values);

	@Override
	<P> MutationQuery setParameterList(String name, P[] values, Class<P> javaType);

	@Override
	<P> MutationQuery setParameterList(String name, P[] values, BindableType<P> type);

	@Override
	MutationQuery setParameterList(int position, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> MutationQuery setParameterList(int position, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> MutationQuery setParameterList(int position, Collection<? extends P> values, BindableType<P> type);

	@Override
	MutationQuery setParameterList(int position, Object[] values);

	@Override
	<P> MutationQuery setParameterList(int position, P[] values, Class<P> javaType);

	@Override
	<P> MutationQuery setParameterList(int position, P[] values, BindableType<P> type);

	@Override
	<P> MutationQuery setParameterList(QueryParameter<P> parameter, Collection<? extends P> values);

	@Override
	<P> MutationQuery setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> MutationQuery setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, BindableType<P> type);

	@Override
	<P> MutationQuery setParameterList(QueryParameter<P> parameter, P[] values);

	@Override
	<P> MutationQuery setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType);

	@Override
	<P> MutationQuery setParameterList(QueryParameter<P> parameter, P[] values, BindableType<P> type);

	@Override
	MutationQuery setProperties(Object bean);

	@Override
	MutationQuery setProperties(@SuppressWarnings("rawtypes") Map bean);

	@Override
	MutationQuery setQueryFlushMode(QueryFlushMode queryFlushMode);
}
