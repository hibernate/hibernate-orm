/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.Statement;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import jakarta.persistence.criteria.CriteriaStatement;
import jakarta.persistence.metamodel.Type;
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
	@Nullable
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
	@Nonnull
	MutationQuery setQueryFlushMode(@Nonnull QueryFlushMode queryFlushMode);

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Deprecated(since = "7")
	@Nonnull
	MutationQuery setFlushMode(@Nonnull FlushModeType flushMode);

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Nonnull
	MutationQuery setTimeout(int timeout);

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Nonnull
	MutationQuery setTimeout(@Nullable Integer timeout);

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Nonnull
	MutationQuery setTimeout(@Nullable Timeout timeout);

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Nonnull
	MutationQuery setComment(@Nullable String comment);

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Nonnull
	MutationQuery setHint(@Nonnull String hintName, @Nullable Object value);

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Nonnull
	MutationQuery addOption(@Nonnull Option option);

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Nonnull
	MutationQuery addQueryHint(@Nonnull String hint);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter Handling

	@Override
	@Nonnull
	<P> MutationQuery setConvertedParameter(@Nonnull String name, @Nullable P value, @Nonnull Class<? extends AttributeConverter<P, ?>> converter);

	@Override
	@Nonnull
	<P> MutationQuery setConvertedParameter(int position, @Nullable P value, @Nonnull Class<? extends AttributeConverter<P, ?>> converter);

	@Override
	@Nonnull
	MutationQuery setParameter(@Nonnull String name, @Nullable Object value);

	@Override
	@Nonnull
	<P> MutationQuery setParameter(@Nonnull String name, @Nullable P value, @Nonnull Class<P> type);

	@Override
	@Nonnull
	<P> MutationQuery setParameter(@Nonnull String name, @Nullable P value, @Nonnull Type<P> type);

	@Override @Deprecated(since = "7")
	@Nonnull
	MutationQuery setParameter(@Nonnull String name, @Nullable Instant value, @Nonnull TemporalType temporalType);

	@Override @Deprecated(since = "7")
	@Nonnull
	MutationQuery setParameter(@Nonnull String name, @Nullable Calendar value, @Nonnull TemporalType temporalType);

	@Override @Deprecated(since = "7")
	@Nonnull
	MutationQuery setParameter(@Nonnull String name, @Nullable Date value, @Nonnull TemporalType temporalType);

	@Override
	@Nonnull
	MutationQuery setParameter(int position, @Nullable Object value);

	@Override
	@Nonnull
	MutationQuery setParameters(@Nonnull Object... arguments);

	@Override
	@Nonnull
	<P> MutationQuery setParameter(int position, @Nullable P value, @Nonnull Class<P> type);

	@Override
	@Nonnull
	<P> MutationQuery setParameter(int position, @Nullable P value, @Nonnull Type<P> type);

	@Override @Deprecated(since = "7")
	@Nonnull
	MutationQuery setParameter(int position, @Nullable Instant value, @Nonnull TemporalType temporalType);

	@Override @Deprecated(since = "7")
	@Nonnull
	MutationQuery setParameter(int position, @Nullable Date value, @Nonnull TemporalType temporalType);

	@Override @Deprecated(since = "7")
	@Nonnull
	MutationQuery setParameter(int position, @Nullable Calendar value, @Nonnull TemporalType temporalType);

	@Override
	@Nonnull
	<P> MutationQuery setParameter(@Nonnull QueryParameter<P> parameter, @Nullable P value);

	@Override
	@Nonnull
	<P> MutationQuery setParameter(@Nonnull QueryParameter<P> parameter, @Nullable P value, @Nonnull Class<P> type);

	@Override
	@Nonnull
	<P> MutationQuery setParameter(@Nonnull QueryParameter<P> parameter, @Nullable P val, @Nonnull Type<P> type);

	@Override
	@Nonnull
	<P> MutationQuery setParameter(@Nonnull Parameter<P> param, @Nullable P value);

	@Override @Deprecated(since = "7")
	@Nonnull
	MutationQuery setParameter(@Nonnull Parameter<Calendar> param, @Nullable Calendar value, @Nonnull TemporalType temporalType);

	@Override @Deprecated(since = "7")
	@Nonnull
	MutationQuery setParameter(@Nonnull Parameter<Date> param, @Nullable Date value, @Nonnull TemporalType temporalType);

	@Override
	@Nonnull
	MutationQuery setParameterList(@Nonnull String name, @SuppressWarnings("rawtypes") @Nonnull Collection values);

	@Override
	@Nonnull
	<P> MutationQuery setParameterList(@Nonnull String name, @Nonnull Collection<? extends P> values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> MutationQuery setParameterList(@Nonnull String name, @Nonnull Collection<? extends P> values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	MutationQuery setParameterList(@Nonnull String name, @Nonnull Object[] values);

	@Override
	@Nonnull
	<P> MutationQuery setParameterList(@Nonnull String name, @Nonnull P[] values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> MutationQuery setParameterList(@Nonnull String name, @Nonnull P[] values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	MutationQuery setParameterList(int position, @SuppressWarnings("rawtypes") @Nonnull Collection values);

	@Override
	@Nonnull
	<P> MutationQuery setParameterList(int position, @Nonnull Collection<? extends P> values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> MutationQuery setParameterList(int position, @Nonnull Collection<? extends P> values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	MutationQuery setParameterList(int position, @Nonnull Object[] values);

	@Override
	@Nonnull
	<P> MutationQuery setParameterList(int position, @Nonnull P[] values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> MutationQuery setParameterList(int position, @Nonnull P[] values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	<P> MutationQuery setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull Collection<? extends P> values);

	@Override
	@Nonnull
	<P> MutationQuery setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull Collection<? extends P> values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> MutationQuery setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull Collection<? extends P> values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	<P> MutationQuery setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values);

	@Override
	@Nonnull
	<P> MutationQuery setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> MutationQuery setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	MutationQuery setProperties(@Nonnull Object bean);

	@Override
	@Nonnull
	MutationQuery setProperties(@SuppressWarnings("rawtypes") @Nonnull Map bean);
}
