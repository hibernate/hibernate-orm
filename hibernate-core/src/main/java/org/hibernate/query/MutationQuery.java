/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.Statement;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import jakarta.persistence.metamodel.Type;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Incubating;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * Within the context of an active {@linkplain org.hibernate.Session session},
 * an instance of this type represents an executable mutation query, that is,
 * an {@code insert}, {@code update}, or {@code delete}. It is a slimmed-down
 * version of {@link Query}, providing only methods relevant to mutation queries.
 * <p>
 * A {@code MutationQuery} may be obtained from the {@link org.hibernate.Session}
 * by calling:
 * <ul>
 * <li>{@link org.hibernate.SharedSessionContract#createMutationQuery(String)}, passing the HQL as a
 *     string,
 * <li>{@link org.hibernate.SharedSessionContract#createNativeMutationQuery(String)}, passing native
 *     SQL as a string,
 * <li>{@link org.hibernate.SharedSessionContract#createMutationQuery(jakarta.persistence.criteria.CriteriaUpdate)} or
 *     {@link org.hibernate.SharedSessionContract#createMutationQuery(jakarta.persistence.criteria.CriteriaDelete)},
 *     passing a criteria update or delete object, or
 * <li>{@link org.hibernate.SharedSessionContract#createNamedMutationQuery(String)}, passing the
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
 * <pre>
 * session.createMutationQuery("delete Draft where lastUpdated &lt; local date - ?1 year")
 *         .setParameter(1, years)
 *         .executeUpdate();
 * </pre>
 *
 * @author Steve Ebersole
 */
@Incubating
public interface MutationQuery extends CommonQueryContract, Statement {
	/**
	 * The HQL or native-SQL string, or {@code null} in the case of a criteria query.
	 */
	String getMutationString();

	/**
	 * The Java type of the thing being mutated, if known.
	 */
	@Nullable
	Class<?> getTargetType();

	/**
	 * Execute an insert, update, or delete statement, and return the
	 * number of affected entities.
	 * <p>
	 * For use with instances of {@code MutationQuery} created using
	 * {@link org.hibernate.SharedSessionContract#createMutationQuery(String)},
	 * {@link org.hibernate.SharedSessionContract#createNamedMutationQuery(String)},
	 * {@link org.hibernate.SharedSessionContract#createNativeMutationQuery(String)},
	 * {@link org.hibernate.SharedSessionContract#createQuery(jakarta.persistence.criteria.CriteriaUpdate)}, or
	 * {@link org.hibernate.SharedSessionContract#createQuery(jakarta.persistence.criteria.CriteriaDelete)}.
	 *
	 * @return the number of affected entity instances
	 *         (may differ from the number of affected rows)
	 *
	 * @see org.hibernate.SharedSessionContract#createMutationQuery(String)
	 * @see org.hibernate.SharedSessionContract#createNamedMutationQuery(String)
	 * @see org.hibernate.SharedSessionContract#createNativeMutationQuery(String)
	 *
	 * @see jakarta.persistence.Query#executeUpdate()
	 */
	@Override
	int executeUpdate();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Covariant returns

	@Override @Deprecated(since = "7")
	MutationQuery setFlushMode(FlushModeType flushMode);

	@Override
	MutationQuery setTimeout(int timeout);

	@Override
	MutationQuery setTimeout(Integer timeout);

	@Override
	MutationQuery setTimeout(Timeout timeout);

	@Override
	MutationQuery setComment(String comment);

	@Override
	MutationQuery setHint(String hintName, Object value);

	@Override
	<P> MutationQuery setConvertedParameter(String name, P value, Class<? extends AttributeConverter<P, ?>> converter);

	@Override
	<P> MutationQuery setConvertedParameter(int position, P value, Class<? extends AttributeConverter<P, ?>> converter);

	@Override
	MutationQuery setParameter(String name, Object value);

	@Override
	<P> MutationQuery setParameter(String name, P value, Class<P> type);

	@Override
	<P> MutationQuery setParameter(String name, P value, Type<P> type);

	@Override @Deprecated(since = "7")
	MutationQuery setParameter(String name, Instant value, TemporalType temporalType);

	@Override @Deprecated(since = "7")
	MutationQuery setParameter(String name, Calendar value, TemporalType temporalType);

	@Override @Deprecated(since = "7")
	MutationQuery setParameter(String name, Date value, TemporalType temporalType);

	@Override
	MutationQuery setParameter(int position, Object value);

	@Override
	<P> MutationQuery setParameter(int position, P value, Class<P> type);

	@Override
	<P> MutationQuery setParameter(int position, P value, Type<P> type);

	@Override @Deprecated(since = "7")
	MutationQuery setParameter(int position, Instant value, TemporalType temporalType);

	@Override @Deprecated(since = "7")
	MutationQuery setParameter(int position, Date value, TemporalType temporalType);

	@Override @Deprecated(since = "7")
	MutationQuery setParameter(int position, Calendar value, TemporalType temporalType);

	@Override
	<P> MutationQuery setParameter(QueryParameter<P> parameter, P value);

	@Override
	<P> MutationQuery setParameter(QueryParameter<P> parameter, P value, Class<P> type);

	@Override
	<P> MutationQuery setParameter(QueryParameter<P> parameter, P val, Type<P> type);

	@Override
	<P> MutationQuery setParameter(Parameter<P> param, P value);

	@Override @Deprecated(since = "7")
	MutationQuery setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override @Deprecated(since = "7")
	MutationQuery setParameter(Parameter<Date> param, Date value, TemporalType temporalType);

	@Override
	MutationQuery setParameterList(String name, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> MutationQuery setParameterList(String name, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> MutationQuery setParameterList(String name, Collection<? extends P> values, Type<P> type);

	@Override
	MutationQuery setParameterList(String name, Object[] values);

	@Override
	<P> MutationQuery setParameterList(String name, P[] values, Class<P> javaType);

	@Override
	<P> MutationQuery setParameterList(String name, P[] values, Type<P> type);

	@Override
	MutationQuery setParameterList(int position, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> MutationQuery setParameterList(int position, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> MutationQuery setParameterList(int position, Collection<? extends P> values, Type<P> type);

	@Override
	MutationQuery setParameterList(int position, Object[] values);

	@Override
	<P> MutationQuery setParameterList(int position, P[] values, Class<P> javaType);

	@Override
	<P> MutationQuery setParameterList(int position, P[] values, Type<P> type);

	@Override
	<P> MutationQuery setParameterList(QueryParameter<P> parameter, Collection<? extends P> values);

	@Override
	<P> MutationQuery setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> MutationQuery setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Type<P> type);

	@Override
	<P> MutationQuery setParameterList(QueryParameter<P> parameter, P[] values);

	@Override
	<P> MutationQuery setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType);

	@Override
	<P> MutationQuery setParameterList(QueryParameter<P> parameter, P[] values, Type<P> type);

	@Override
	MutationQuery setProperties(Object bean);

	@Override
	MutationQuery setProperties(@SuppressWarnings("rawtypes") Map bean);

	@Override
	MutationQuery setQueryFlushMode(QueryFlushMode queryFlushMode);
}
