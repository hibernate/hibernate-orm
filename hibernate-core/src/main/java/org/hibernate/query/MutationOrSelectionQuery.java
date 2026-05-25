/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

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
	SelectionQuery<?> asSelectionQuery();

	/**
	 * Casts this query as a {@code SelectionQuery} with the given result type.
	 *
	 * @throws IllegalSelectQueryException If the query is not a select query.
	 * @throws IllegalArgumentException If the given {@code type} is not compatible with the query's defined result type.
	 */
	<R> SelectionQuery<R> asSelectionQuery(Class<R> type);

	/**
	 * Casts this query as a {@code SelectionQuery} with the given result graph.
	 *
	 * @throws IllegalSelectQueryException If the query is not a selection query.
	 * @throws IllegalArgumentException If the given graph result type is not compatible with the {@code Query} type parameter.
	 */
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
	<X> SelectionQuery<X> asSelectionQuery(EntityGraph<X> entityGraph, GraphSemantic graphSemantic);

	/**
	 * Casts this query as a mutation query.
	 *
	 * @throws IllegalMutationQueryException If the query is not a mutation query.
	 */
	MutationQuery asMutationQuery();

	/**
	 * {@inheritDoc}
	 *
	 * @param resultType The Java class of the query result type
	 */
	@Override
	<R> SelectionQuery<R> ofType(Class<R> resultType);

	/**
	 * {@inheritDoc}
	 * @param graph The entity graph, interpreted as a load graph
	 */
	@Override
	<R> SelectionQuery<R> withEntityGraph(EntityGraph<R> graph);

	/**
	 * {@inheritDoc}
	 * @param mapping The result set mapping
	 */
	@Override
	<R> SelectionQuery<R> withResultSetMapping(ResultSetMapping<R> mapping);

	/**
	 * {@inheritDoc}
	 */
	@Override
	MutationQuery asStatement();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Return type overrides

	@Override
	MutationOrSelectionQuery setQueryFlushMode(QueryFlushMode queryFlushMode);

	@Override
	@Deprecated
	MutationOrSelectionQuery setQueryPlanCacheable(boolean queryPlanCacheable);

	@Override
	MutationOrSelectionQuery addQueryHint(String hint);

	@Override
	MutationOrSelectionQuery setTimeout(int timeout);

	@Override
	MutationOrSelectionQuery setTimeout(Integer timeout);

	@Override
	MutationOrSelectionQuery setTimeout(Timeout timeout);

	@Override
	MutationOrSelectionQuery setComment(String comment);

	@Override
	MutationOrSelectionQuery setHint(String hintName, Object value);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecated operations

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	MutationOrSelectionQuery setFlushMode(FlushModeType flushMode);

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	MutationOrSelectionQuery setEntityGraph(EntityGraph<? super Object> graph, GraphSemantic semantic);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecated operations that inherently depend on the query type

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	int executeUpdate();

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings({"unchecked", "rawtypes", "removal"})
	List getResultList();

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	Object getSingleResult();

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	Object getSingleResultOrNull();

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings({"unchecked", "rawtypes", "removal"})
	Stream getResultStream();

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings({"unchecked", "rawtypes", "removal"})
	List list();

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings({"unchecked", "rawtypes", "removal"})
	ScrollableResults scroll();

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings({"unchecked", "rawtypes", "removal"})
	ScrollableResults scroll(ScrollMode scrollMode);

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings({"unchecked", "rawtypes", "removal"})
	Stream stream();

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	Object uniqueResult();

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings({"unchecked", "rawtypes", "removal"})
	Optional uniqueResultOptional();

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	MutationOrSelectionQuery setFetchSize(int fetchSize);

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	MutationOrSelectionQuery setReadOnly(boolean readOnly);

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	MutationOrSelectionQuery setMaxResults(int maxResults);

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	MutationOrSelectionQuery setFirstResult(int startPosition);

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	MutationOrSelectionQuery setCacheMode(CacheMode cacheMode);

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	MutationOrSelectionQuery setCacheStoreMode(CacheStoreMode cacheStoreMode);

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	MutationOrSelectionQuery setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode);

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	MutationOrSelectionQuery setCacheRegion(String cacheRegion);

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	MutationOrSelectionQuery setLockMode(LockModeType lockMode);

	@Override
	@Deprecated(forRemoval = true)
	MutationOrSelectionQuery setHibernateLockMode(LockMode lockMode);

	@Override
	@Deprecated(forRemoval = true)
	MutationOrSelectionQuery setFollowOnStrategy(Locking.FollowOn followOnStrategy);

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	MutationOrSelectionQuery setCacheable(boolean cacheable);

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	MutationOrSelectionQuery enableFetchProfile(String profileName);

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	MutationOrSelectionQuery disableFetchProfile(String profileName);

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	MutationOrSelectionQuery setResultListTransformer(ResultListTransformer<Object> transformer);

	@Override
	@Deprecated(forRemoval = true)
	@SuppressWarnings("removal")
	<X> Query<X> setTupleTransformer(TupleTransformer<X> transformer);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter handling

	@Override
	<P> MutationOrSelectionQuery setParameter(String name, P value, Class<P> type);

	@Override
	<P> MutationOrSelectionQuery setParameter(String name, P value, Type<P> type);

	@Override
	MutationOrSelectionQuery setParameter(String name, Object value);

	@Override
	MutationOrSelectionQuery setParameter(int position, Object value);

	@Override
	<P> MutationOrSelectionQuery setParameter(int position, P value, Class<P> type);

	@Override
	<P> MutationOrSelectionQuery setParameter(int position, P value, Type<P> type);

	@Override
	<P> MutationOrSelectionQuery setConvertedParameter(String name, P value, Class<? extends AttributeConverter<P, ?>> converter);

	@Override
	<P> MutationOrSelectionQuery setConvertedParameter(int position, P value, Class<? extends AttributeConverter<P, ?>> converter);

	@Override
	@Deprecated
	MutationOrSelectionQuery setParameter(String name, Instant value, TemporalType temporalType);

	@Override
	@Deprecated
	MutationOrSelectionQuery setParameter(String name, Calendar value, TemporalType temporalType);

	@Override
	@Deprecated
	MutationOrSelectionQuery setParameter(String name, Date value, TemporalType temporalType);

	@Override
	@Deprecated
	MutationOrSelectionQuery setParameter(int position, Instant value, TemporalType temporalType);

	@Override
	@Deprecated
	MutationOrSelectionQuery setParameter(int position, Date value, TemporalType temporalType);

	@Override
	@Deprecated
	MutationOrSelectionQuery setParameter(int position, Calendar value, TemporalType temporalType);

	@Override
	<P> MutationOrSelectionQuery setParameter(QueryParameter<P> parameter, P value);

	@Override
	<P> MutationOrSelectionQuery setParameter(QueryParameter<P> parameter, P value, Class<P> type);

	@Override
	<P> MutationOrSelectionQuery setParameter(QueryParameter<P> parameter, P val, Type<P> type);

	@Override
	<P> MutationOrSelectionQuery setParameter(Parameter<P> param, P value);

	@Override
	@Deprecated
	MutationOrSelectionQuery setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override
	@Deprecated
	MutationOrSelectionQuery setParameter(Parameter<Date> param, Date value, TemporalType temporalType);

	@Override
	MutationOrSelectionQuery setParameterList(String name, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> MutationOrSelectionQuery setParameterList(String name, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> MutationOrSelectionQuery setParameterList(String name, Collection<? extends P> values, Type<P> type);

	@Override
	MutationOrSelectionQuery setParameterList(String name, Object[] values);

	@Override
	<P> MutationOrSelectionQuery setParameterList(String name, P[] values, Class<P> javaType);

	@Override
	<P> MutationOrSelectionQuery setParameterList(String name, P[] values, Type<P> type);

	@Override
	MutationOrSelectionQuery setParameterList(int position, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> MutationOrSelectionQuery setParameterList(int position, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> MutationOrSelectionQuery setParameterList(int position, Collection<? extends P> values, Type<P> type);

	@Override
	MutationOrSelectionQuery setParameterList(int position, Object[] values);

	@Override
	<P> MutationOrSelectionQuery setParameterList(int position, P[] values, Class<P> javaType);

	@Override
	<P> MutationOrSelectionQuery setParameterList(int position, P[] values, Type<P> type);

	@Override
	<P> MutationOrSelectionQuery setParameterList(QueryParameter<P> parameter, Collection<? extends P> values);

	@Override
	<P> MutationOrSelectionQuery setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> MutationOrSelectionQuery setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Type<P> type);

	@Override
	<P> MutationOrSelectionQuery setParameterList(QueryParameter<P> parameter, P[] values);

	@Override
	<P> MutationOrSelectionQuery setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType);

	@Override
	<P> MutationOrSelectionQuery setParameterList(QueryParameter<P> parameter, P[] values, Type<P> type);

	@Override
	MutationOrSelectionQuery setProperties(Object bean);

	@Override
	MutationOrSelectionQuery setProperties(@SuppressWarnings("rawtypes") Map bean);
}
