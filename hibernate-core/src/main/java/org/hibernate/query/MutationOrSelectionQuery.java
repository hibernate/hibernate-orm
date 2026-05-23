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
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.Statement;
import jakarta.persistence.StatementOrTypedQuery;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.metamodel.Type;
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
import java.util.Set;
import java.util.stream.Stream;

/**
 * Unifies {@link StatementOrTypedQuery} with {@link MutationQuery}
 * and {@link SelectionQuery}, allowing backward compatibility with
 * older versions of Hibernate.
 *
 * @author Gavin King
 * @since 8.0
 */
public interface MutationOrSelectionQuery
		extends StatementOrTypedQuery, MutationQuery, SelectionQuery<Object> {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Casts

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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecated operations

	@Override @Deprecated
	int execute();

	@Override @Deprecated
	@SuppressWarnings("removal")
	int executeUpdate();

	@Override @Deprecated
	@SuppressWarnings({"unchecked","rawtypes", "removal"})
	List getResultList();

	@Override @Deprecated
	@SuppressWarnings("removal")
	Object getSingleResult();

	@Override @Deprecated
	@SuppressWarnings("removal")
	Object getSingleResultOrNull();

	@Override @Deprecated
	@SuppressWarnings({"unchecked","rawtypes", "removal"})
	Stream getResultStream();

	@Override @Deprecated
	@SuppressWarnings({"unchecked","rawtypes"})
	List list();

	@Override @Deprecated
	@SuppressWarnings({"unchecked","rawtypes"})
	ScrollableResults scroll();

	@Override @Deprecated
	@SuppressWarnings({"unchecked","rawtypes"})
	ScrollableResults scroll(ScrollMode scrollMode);

	@Override @Deprecated
	@SuppressWarnings({"unchecked","rawtypes"})
	Stream stream();

	@Override @Deprecated
	Object uniqueResult();

	@Override @Deprecated
	@SuppressWarnings({"unchecked","rawtypes"})
	Optional uniqueResultOptional();

	@Override @SuppressWarnings({"unchecked","rawtypes"})
	Set getOptions();

	@Override
	MutationOrSelectionQuery addOption(Statement.Option option);

	@Override
	MutationOrSelectionQuery setFlushMode(FlushModeType flushMode);

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

	@Override @Deprecated
	MutationOrSelectionQuery setParameter(String name, Instant value, TemporalType temporalType);

	@Override @Deprecated
	MutationOrSelectionQuery setParameter(String name, Calendar value, TemporalType temporalType);

	@Override @Deprecated
	MutationOrSelectionQuery setParameter(String name, Date value, TemporalType temporalType);

	@Override @Deprecated
	MutationOrSelectionQuery setParameter(int position, Instant value, TemporalType temporalType);

	@Override @Deprecated
	MutationOrSelectionQuery setParameter(int position, Date value, TemporalType temporalType);

	@Override @Deprecated
	MutationOrSelectionQuery setParameter(int position, Calendar value, TemporalType temporalType);

	@Override
	<P> MutationOrSelectionQuery setParameter(QueryParameter<P> parameter, P value);

	@Override
	<P> MutationOrSelectionQuery setParameter(QueryParameter<P> parameter, P value, Class<P> type);

	@Override
	<P> MutationOrSelectionQuery setParameter(QueryParameter<P> parameter, P val, Type<P> type);

	@Override
	<P> MutationOrSelectionQuery setParameter(Parameter<P> param, P value);

	@Override @Deprecated
	MutationOrSelectionQuery setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override @Deprecated
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

	@Override
	MutationOrSelectionQuery setQueryFlushMode(QueryFlushMode queryFlushMode);

	@Override
	MutationOrSelectionQuery addOption(TypedQuery.Option option);

	@Override @Deprecated
	@SuppressWarnings("removal")
	MutationOrSelectionQuery setEntityGraph(EntityGraph<? super Object> entityGraph);

	@Override @Deprecated
	@SuppressWarnings("removal")
	MutationOrSelectionQuery setEntityGraph(EntityGraph<? super Object> graph, GraphSemantic semantic);

	@Override @Deprecated
	MutationOrSelectionQuery enableFetchProfile(String profileName);

	@Override @Deprecated
	MutationOrSelectionQuery disableFetchProfile(String profileName);

	@Override @Deprecated
	MutationOrSelectionQuery setFetchSize(int fetchSize);

	@Override @Deprecated
	MutationOrSelectionQuery setReadOnly(boolean readOnly);

	@Override @Deprecated @SuppressWarnings("removal")
	MutationOrSelectionQuery setMaxResults(int maxResults);

	@Override @Deprecated @SuppressWarnings("removal")
	MutationOrSelectionQuery setFirstResult(int startPosition);

	@Override @Deprecated
	MutationOrSelectionQuery setPage(Page page);

	@Override @Deprecated
	MutationOrSelectionQuery setCacheMode(CacheMode cacheMode);

	@Override @Deprecated
	@SuppressWarnings("removal")
	MutationOrSelectionQuery setCacheStoreMode(CacheStoreMode cacheStoreMode);

	@Override @Deprecated
	@SuppressWarnings("removal")
	MutationOrSelectionQuery setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode);

	@Override @Deprecated
	MutationOrSelectionQuery setQueryPlanCacheable(boolean queryPlanCacheable);

	@Override @Deprecated
	MutationOrSelectionQuery setCacheRegion(String cacheRegion);

	@Override @Deprecated @SuppressWarnings("removal")
	MutationOrSelectionQuery setLockMode(LockModeType lockMode);

	@Override @Deprecated
	MutationOrSelectionQuery setHibernateLockMode(LockMode lockMode);

	@Override @Deprecated
	MutationOrSelectionQuery setLockScope(PessimisticLockScope lockScope);

	@Override @Deprecated
	MutationOrSelectionQuery setLockTimeout(Timeout lockTimeout);

	@Override @Deprecated
	MutationOrSelectionQuery setFollowOnStrategy(Locking.FollowOn followOnStrategy);

	@Override @Deprecated
	MutationOrSelectionQuery setCacheable(boolean cacheable);
}
