/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.StatementOrTypedQuery;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import jakarta.persistence.metamodel.Type;
import jakarta.persistence.sql.ResultSetMapping;
import org.hibernate.CacheMode;
import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.SharedSessionContract;
import org.hibernate.graph.GraphSemantic;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Unifies {@link StatementOrTypedQuery} with {@link Query}, allowing backward
 * compatibility with older versions of Hibernate and JPA which did not carefully
 * distinguish {@linkplain SelectionQuery selection queries} from insert, update,
 * and delete {@linkplain MutationQuery mutation statements}.
 * <p>
 * An instance of this interface is returned by
 * {@link SharedSessionContract#createQuery(String)} or
 * {@link SharedSessionContract#createNamedQuery(String)}. But in newly written
 * code, this interface should not be used to directly execute a query or statement.
 * Instead:
 * <ul>
 * <li>to obtain a {@link SelectionQuery}, call {@link #ofType(Class)},
 *     {@link #asSelectionQuery()}, or {@link #asSelectionQuery(Class)},
 * <li>to specify an {@linkplain EntityGraph entity graph}, call
 *     {@link #withEntityGraph(EntityGraph)},
 * <li>to specify a {@linkplain ResultSetMapping result set mapping}, call
 *     {@link #withResultSetMapping(ResultSetMapping)}, or
 * <li>to obtain a {@link MutationQuery}, call {@link #asStatement()} or
 *     {@link #asMutationQuery()}.
 * </ul>
 * <p>
 * A typical idiom is the following:
 * <pre>
 * List&lt;Book&gt; matchingBooks =
 *         session.createQuery("from Book where title like :titlePattern")
 *                 .ofType(Book.class)
 *                 .setParameter("titlePattern", pattern)
 *                 .setMaxResults(pageSize)
 *                 .setCacheStoreMode(CacheStoreMode.BYPASS)
 *                 .getResultList();
 * </pre>
 *
 * @see StatementOrTypedQuery
 * @see SelectionQuery
 * @see MutationQuery
 * @see SharedSessionContract#createQuery(String)
 * @see SharedSessionContract#createNamedQuery(String)
 *
 * @author Gavin King
 * @since 8.0
 */
@Incubating
public interface MutationOrSelectionQuery
		extends StatementOrTypedQuery, Query<Object> {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Casts

	boolean isSelectionQuery();

	boolean isMutationQuery();

	/**
	 * Casts this query as a {@code SelectionQuery}.
	 *
	 * @throws IllegalSelectQueryException If the query is not a select query.
	 */
	@Nonnull
	SelectionQuery<?> asSelectionQuery();

	/**
	 * Casts this query as a {@code SelectionQuery} with the given result type.
	 *
	 * @throws IllegalSelectQueryException If the query is not a select query.
	 * @throws IllegalArgumentException If the given {@code type} is not compatible with the query's defined result type.
	 */
	@Nonnull
	<R> SelectionQuery<R> asSelectionQuery(Class<R> type);

	/**
	 * Casts this query as a {@code SelectionQuery} with the given result graph.
	 *
	 * @throws IllegalSelectQueryException If the query is not a selection query.
	 * @throws IllegalArgumentException If the given graph result type is not compatible with the {@code Query} type parameter.
	 */
	@Nonnull
	<X> SelectionQuery<X> asSelectionQuery(EntityGraph<X> entityGraph);

	/**
	 * Overload of {@linkplain #asSelectionQuery(EntityGraph)} allowing a specific semantic
	 * (load/fetch) for the graph.
	 *
	 * @param entityGraph The entity graph.
	 * @param graphSemantic The load/fetch semantic.
	 * @return The cast/converted query.
	 *
	 * @see SharedSessionContract#createSelectionQuery(String, EntityGraph)
	 * @see SharedSessionContract#createQuery(String, EntityGraph)
	 * @see #asSelectionQuery(Class)
	 */
	@Nonnull
	<X> SelectionQuery<X> asSelectionQuery(EntityGraph<X> entityGraph, GraphSemantic graphSemantic);

	/**
	 * Casts this query as a mutation query.
	 *
	 * @throws IllegalMutationQueryException If the query is not a mutation query.
	 */
	@Nonnull
	MutationQuery asMutationQuery();

	/**
	 * {@inheritDoc}
	 *
	 * @param resultType The Java class of the query result type
	 */
	@Override
	@Nonnull
	<R> SelectionQuery<R> ofType(@Nonnull Class<R> resultType);

	/**
	 * {@inheritDoc}
	 * @param graph The entity graph, interpreted as a load graph
	 */
	@Override
	@Nonnull
	<R> SelectionQuery<R> withEntityGraph(@Nonnull EntityGraph<R> graph);

	/**
	 * {@inheritDoc}
	 * @param mapping The result set mapping
	 */
	@Override
	@Nonnull
	<R> SelectionQuery<R> withResultSetMapping(@Nonnull ResultSetMapping<R> mapping);

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Nonnull
	MutationQuery asStatement();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Return type overrides

	@Override
	@Nonnull
	MutationOrSelectionQuery setQueryFlushMode(@Nonnull QueryFlushMode queryFlushMode);

	@Override
	@Deprecated
	@Nonnull
	MutationOrSelectionQuery setQueryPlanCacheable(boolean queryPlanCacheable);

	@Override
	@Nonnull
	MutationOrSelectionQuery addQueryHint(@Nonnull String hint);

	@Override
	@Nonnull
	MutationOrSelectionQuery setTimeout(int timeout);

	@Override
	@Nonnull
	MutationOrSelectionQuery setTimeout(@Nullable Integer timeout);

	@Override
	@Nonnull
	MutationOrSelectionQuery setTimeout(@Nullable Timeout timeout);

	@Override
	@Nonnull
	MutationOrSelectionQuery setComment(@Nullable String comment);

	@Override
	@Nonnull
	MutationOrSelectionQuery setHint(@Nonnull String hintName, @Nullable Object value);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecated operations

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	@Nonnull
	MutationOrSelectionQuery setFlushMode(@Nonnull FlushModeType flushMode);

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	@Nonnull
	MutationOrSelectionQuery setEntityGraph(@Nonnull EntityGraph<? super Object> graph, @Nonnull GraphSemantic semantic);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecated operations that inherently depend on the query type

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	int executeUpdate();

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings({"unchecked", "rawtypes", "removal"})
	@Nonnull
	List getResultList();

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	Object getSingleResult();

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	@Nullable
	Object getSingleResultOrNull();

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings({"unchecked", "rawtypes", "removal"})
	@Nonnull
	Stream getResultStream();

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings({"unchecked", "rawtypes", "removal"})
	@Nonnull
	List list();

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings({"unchecked", "rawtypes", "removal"})
	@Nonnull
	ScrollableResults scroll();

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings({"unchecked", "rawtypes", "removal"})
	@Nonnull
	ScrollableResults scroll(@Nonnull ScrollMode scrollMode);

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings({"unchecked", "rawtypes", "removal"})
	@Nonnull
	Stream stream();

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	@Nullable
	Object uniqueResult();

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings({"unchecked", "rawtypes", "removal"})
	@Nonnull
	Optional uniqueResultOptional();

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	@Nonnull
	MutationOrSelectionQuery setFetchSize(int fetchSize);

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	@Nonnull
	MutationOrSelectionQuery setReadOnly(boolean readOnly);

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	@Nonnull
	MutationOrSelectionQuery setMaxResults(int maxResults);

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	@Nonnull
	MutationOrSelectionQuery setFirstResult(int startPosition);

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	@Nonnull
	MutationOrSelectionQuery setCacheMode(@Nonnull CacheMode cacheMode);

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	@Nonnull
	MutationOrSelectionQuery setCacheStoreMode(@Nonnull CacheStoreMode cacheStoreMode);

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	@Nonnull
	MutationOrSelectionQuery setCacheRetrieveMode(@Nonnull CacheRetrieveMode cacheRetrieveMode);

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	MutationOrSelectionQuery setCacheRegion(@Nullable String cacheRegion);

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	@Nonnull
	MutationOrSelectionQuery setLockMode(@Nonnull LockModeType lockMode);

	@Override
	@Deprecated(forRemoval = true)
	@Nonnull
	MutationOrSelectionQuery setHibernateLockMode(@Nonnull LockMode lockMode);

	@Override
	@Deprecated(forRemoval = true)
	@Nonnull
	MutationOrSelectionQuery setFollowOnStrategy(@Nonnull Locking.FollowOn followOnStrategy);

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	@Nonnull
	MutationOrSelectionQuery setCacheable(boolean cacheable);

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	@Nonnull
	MutationOrSelectionQuery enableFetchProfile(@Nonnull String profileName);

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	@Nonnull
	MutationOrSelectionQuery disableFetchProfile(@Nonnull String profileName);

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	@Nonnull
	MutationOrSelectionQuery setResultListTransformer(@Nonnull ResultListTransformer<Object> transformer);

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	@Nonnull
	<X> Query<X> setTupleTransformer(@Nonnull TupleTransformer<X> transformer);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter handling

	@Override
	@Nonnull
	<P> MutationOrSelectionQuery setParameter(@Nonnull String name, @Nullable P value, @Nonnull Class<P> type);

	@Override
	@Nonnull
	<P> MutationOrSelectionQuery setParameter(@Nonnull String name, @Nullable P value, @Nonnull Type<P> type);

	@Override
	@Nonnull
	MutationOrSelectionQuery setParameter(@Nonnull String name, @Nullable Object value);

	@Override
	@Nonnull
	MutationOrSelectionQuery setParameter(int position, @Nullable Object value);

	@Override
	@Nonnull
	MutationOrSelectionQuery setParameters(@Nonnull Object... arguments);

	@Override
	@Nonnull
	<P> MutationOrSelectionQuery setParameter(int position, @Nullable P value, @Nonnull Class<P> type);

	@Override
	@Nonnull
	<P> MutationOrSelectionQuery setParameter(int position, @Nullable P value, @Nonnull Type<P> type);

	@Override
	@Nonnull
	<P> MutationOrSelectionQuery setConvertedParameter(@Nonnull String name, @Nullable P value, @Nonnull Class<? extends AttributeConverter<P, ?>> converter);

	@Override
	@Nonnull
	<P> MutationOrSelectionQuery setConvertedParameter(int position, @Nullable P value, @Nonnull Class<? extends AttributeConverter<P, ?>> converter);

	@Override
	@Deprecated
	@Nonnull
	MutationOrSelectionQuery setParameter(@Nonnull String name, @Nullable Instant value, @Nonnull TemporalType temporalType);

	@Override
	@Deprecated
	@Nonnull
	MutationOrSelectionQuery setParameter(@Nonnull String name, @Nullable Calendar value, @Nonnull TemporalType temporalType);

	@Override
	@Deprecated
	@Nonnull
	MutationOrSelectionQuery setParameter(@Nonnull String name, @Nullable Date value, @Nonnull TemporalType temporalType);

	@Override
	@Deprecated
	@Nonnull
	MutationOrSelectionQuery setParameter(int position, @Nullable Instant value, @Nonnull TemporalType temporalType);

	@Override
	@Deprecated
	@Nonnull
	MutationOrSelectionQuery setParameter(int position, @Nullable Date value, @Nonnull TemporalType temporalType);

	@Override
	@Deprecated
	@Nonnull
	MutationOrSelectionQuery setParameter(int position, @Nullable Calendar value, @Nonnull TemporalType temporalType);

	@Override
	@Nonnull
	<P> MutationOrSelectionQuery setParameter(@Nonnull QueryParameter<P> parameter, @Nullable P value);

	@Override
	@Nonnull
	<P> MutationOrSelectionQuery setParameter(@Nonnull QueryParameter<P> parameter, @Nullable P value, @Nonnull Class<P> type);

	@Override
	@Nonnull
	<P> MutationOrSelectionQuery setParameter(@Nonnull QueryParameter<P> parameter, @Nullable P val, @Nonnull Type<P> type);

	@Override
	@Nonnull
	<P> MutationOrSelectionQuery setParameter(@Nonnull Parameter<P> param, @Nullable P value);

	@Override
	@Deprecated
	@Nonnull
	MutationOrSelectionQuery setParameter(@Nonnull Parameter<Calendar> param, @Nullable Calendar value, @Nonnull TemporalType temporalType);

	@Override
	@Deprecated
	@Nonnull
	MutationOrSelectionQuery setParameter(@Nonnull Parameter<Date> param, @Nullable Date value, @Nonnull TemporalType temporalType);

	@Override
	@Nonnull
	MutationOrSelectionQuery setParameterList(@Nonnull String name, @SuppressWarnings("rawtypes") @Nonnull Collection values);

	@Override
	@Nonnull
	<P> MutationOrSelectionQuery setParameterList(@Nonnull String name, @Nonnull Collection<? extends P> values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> MutationOrSelectionQuery setParameterList(@Nonnull String name, @Nonnull Collection<? extends P> values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	MutationOrSelectionQuery setParameterList(@Nonnull String name, @Nonnull Object[] values);

	@Override
	@Nonnull
	<P> MutationOrSelectionQuery setParameterList(@Nonnull String name, @Nonnull P[] values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> MutationOrSelectionQuery setParameterList(@Nonnull String name, @Nonnull P[] values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	MutationOrSelectionQuery setParameterList(int position, @SuppressWarnings("rawtypes") @Nonnull Collection values);

	@Override
	@Nonnull
	<P> MutationOrSelectionQuery setParameterList(int position, @Nonnull Collection<? extends P> values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> MutationOrSelectionQuery setParameterList(int position, @Nonnull Collection<? extends P> values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	MutationOrSelectionQuery setParameterList(int position, @Nonnull Object[] values);

	@Override
	@Nonnull
	<P> MutationOrSelectionQuery setParameterList(int position, @Nonnull P[] values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> MutationOrSelectionQuery setParameterList(int position, @Nonnull P[] values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	<P> MutationOrSelectionQuery setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull Collection<? extends P> values);

	@Override
	@Nonnull
	<P> MutationOrSelectionQuery setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull Collection<? extends P> values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> MutationOrSelectionQuery setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull Collection<? extends P> values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	<P> MutationOrSelectionQuery setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values);

	@Override
	@Nonnull
	<P> MutationOrSelectionQuery setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> MutationOrSelectionQuery setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	MutationOrSelectionQuery setProperties(@Nonnull Object bean);

	@Override
	@Nonnull
	MutationOrSelectionQuery setProperties(@SuppressWarnings("rawtypes") @Nonnull Map bean);
}
