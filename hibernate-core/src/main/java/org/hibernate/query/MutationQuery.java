/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.Statement;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import jakarta.persistence.criteria.CriteriaStatement;
import jakarta.persistence.metamodel.Type;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Incubating;
import org.hibernate.SharedSessionContract;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * Within the context of an active {@linkplain org.hibernate.Session session},
 * an instance of this type represents an executable mutation query, that is,
 * an {@code insert}, {@code update}, or {@code delete}. This interface extends
 * the JPA-defined {@link Statement} interface, adding additional operations.
 * <p>
 * A {@code MutationQuery} may be obtained from the {@link org.hibernate.Session}
 * by calling:
 * <ul>
 * <li>{@link SharedSessionContract#createStatement(String)} or
 *     {@link SharedSessionContract#createMutationQuery(String)}, passing the HQL
 *     insert, update, or delete statement as a string,
 * <li>{@link SharedSessionContract#createNativeStatement(String)} or
 *     {@link SharedSessionContract#createNativeMutationQuery(String)}, passing
 *     the native SQL statement as a string,
 * <li>{@link SharedSessionContract#createStatement(CriteriaStatement)} or
 *     {@link SharedSessionContract#createMutationQuery(CriteriaStatement)},
 *     passing a criteria update or delete object, or
 * <li>{@link SharedSessionContract#createNamedStatement(String)} or
 *     {@link SharedSessionContract#createNamedMutationQuery(String)}, passing
 *     the name of a statement declared using {@link jakarta.persistence.NamedQuery}
 *     or {@link jakarta.persistence.NamedNativeQuery}.
 * </ul>
 * <p>
 * A {@code MutationQuery} controls how the mutation query is executed, and
 * allows arguments to be bound to its parameters.
 * <ul>
 * <li>Mutation queries should be executed using {@link #execute()}, since
 *     {@link #executeUpdate()} is now deprecated.
 * <li>The various overloads of {@link #setParameter(String, Object)} and
 *     {@link #setParameter(int, Object)} allow arguments to be bound to named
 *     and ordinal parameters defined by the query.
 * </ul>
 * <pre>
 * session.createStatement("delete Draft where lastUpdated &lt; local date - ?1 year")
 *         .setParameter(1, years)
 *         .executeUpdate();
 * </pre>
 *
 * @see jakarta.persistence.Statement
 * @see org.hibernate.Session#createStatement(String)
 * @see org.hibernate.Session#createMutationQuery(String)
 * @see org.hibernate.Session#createNativeStatement(String)
 * @see org.hibernate.Session#createNativeMutationQuery(String)
 * @see org.hibernate.Session#createMutationQuery(CriteriaStatement)
 * @see org.hibernate.Session#createStatement(CriteriaStatement)
 * @see org.hibernate.Session#createNamedMutationQuery(String)
 * @see org.hibernate.Session#createNamedStatement(String)
 * @see MutationOrSelectionQuery#asMutationQuery()
 *
 * @author Steve Ebersole
 * @since 6.0
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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Execution

	/**
	 * Execute an insert, update, or delete statement and return the
	 * number of affected entities.
	 *
	 * @return the number of affected entity instances
	 *         (may differ from the number of affected rows)
	 */
	@Override
	int execute();

	/**
	 * Execute an insert, update, or delete statement and return the
	 * number of affected entities.
	 *
	 * @return the number of affected entity instances
	 *         (may differ from the number of affected rows)
	 *
	 * @deprecated Use {@link #execute()} instead.
	 */
	@Override @Deprecated(since = "8")
	@SuppressWarnings("removal")
	int executeUpdate();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Options

	/**
	 * {@inheritDoc}
	 */
	@Override
	MutationQuery setQueryFlushMode(QueryFlushMode queryFlushMode);

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Deprecated(since = "7")
	MutationQuery setFlushMode(FlushModeType flushMode);

	/**
	 * {@inheritDoc}
	 */
	@Override
	MutationQuery setTimeout(int timeout);

	/**
	 * {@inheritDoc}
	 */
	@Override
	MutationQuery setTimeout(Integer timeout);

	/**
	 * {@inheritDoc}
	 */
	@Override
	MutationQuery setTimeout(Timeout timeout);

	/**
	 * {@inheritDoc}
	 */
	@Override
	MutationQuery setComment(String comment);

	/**
	 * {@inheritDoc}
	 */
	@Override
	MutationQuery setHint(String hintName, Object value);

	/**
	 * {@inheritDoc}
	 */
	@Override
	MutationQuery addOption(Option option);

	/**
	 * {@inheritDoc}
	 */
	@Override
	MutationQuery addQueryHint(String hint);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter Handling

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
}
