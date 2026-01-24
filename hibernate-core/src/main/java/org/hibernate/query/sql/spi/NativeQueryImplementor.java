/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.spi;

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
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.Type;
import org.hibernate.CacheMode;
import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.MappingException;
import org.hibernate.ScrollMode;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Page;
import org.hibernate.query.QueryFlushMode;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.named.NameableQuery;
import org.hibernate.query.named.NamedMutationMemento;
import org.hibernate.query.named.NamedNativeQueryMemento;
import org.hibernate.query.named.internal.NativeSelectionMementoImpl;
import org.hibernate.query.results.internal.dynamic.DynamicResultBuilderEntityStandard;
import org.hibernate.query.spi.MutationQueryImplementor;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.spi.SelectionQueryImplementor;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author Steve Ebersole
 */
@Incubating
public interface NativeQueryImplementor<R>
		extends SelectionQueryImplementor<R>, MutationQueryImplementor<R>, NativeQuery<R>, NameableQuery {

	/**
	 * Best guess whether this is a select query.  {@code null}
	 * indicates unknown
	 */
	Boolean isSelectQuery();

	@Override
	NamedNativeQueryMemento<?> toMemento(String name);

	@Override
	NamedMutationMemento<?> toMutationMemento(String name);

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Casts

	@Override
	default NativeQueryImplementor<R> asMutationQuery() {
		return (NativeQueryImplementor<R>) MutationQueryImplementor.super.asMutationQuery();
	}

	@Override
	default NativeQueryImplementor<R> asStatement() {
		return (NativeQueryImplementor<R>) MutationQueryImplementor.super.asStatement();
	}

	@Override
	NativeQueryImplementor<R> asSelectionQuery();

	@Override
	<X> NativeQueryImplementor<X> asSelectionQuery(Class<X> type);

	@Override
	<X> NativeQueryImplementor<X> asSelectionQuery(EntityGraph<X> entityGraph);

	@Override
	<X> NativeQueryImplementor<X> ofType(Class<X> type);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Executions

	@Override
	List<R> list();

	@Override
	default List<R> getResultList() {
		return SelectionQueryImplementor.super.getResultList();
	}

	@Override
	ScrollableResultsImplementor<R> scroll();

	@Override
	ScrollableResultsImplementor<R> scroll(ScrollMode scrollMode);

	@Override
	default Stream<R> getResultStream() {
		return SelectionQueryImplementor.super.getResultStream();
	}

	@Override
	default Stream<R> stream() {
		return SelectionQueryImplementor.super.stream();
	}

	@Override
	R uniqueResult();

	@Override
	Optional<R> uniqueResultOptional();

	@Override
	R getSingleResult();

	@Override
	R getSingleResultOrNull();

	@Override
	int execute();

	@Override
	int executeUpdate();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// covariant overrides - NativeQuery

	@Override
	NativeSelectionMementoImpl<R> toSelectionMemento(String name);

	@Override
	NativeQueryImplementor<R> addSynchronizedQuerySpace(String querySpace);

	@Override
	NativeQueryImplementor<R> addSynchronizedEntityName(String entityName) throws MappingException;

	@Override
	NativeQueryImplementor<R> addSynchronizedEntityClass(@SuppressWarnings("rawtypes") Class entityClass) throws MappingException;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// covariant overrides - Query / QueryImplementor

	@Override
	NativeQueryImplementor<R> setHint(String hintName, Object value);

	@Override
	NativeQueryImplementor<R> setTimeout(int timeout);

	@Override
	NativeQueryImplementor<R> setTimeout(Integer timeout);

	@Override
	NativeQueryImplementor<R> setTimeout(Timeout timeout);

	@Override
	NativeQueryImplementor<R> setQueryFlushMode(QueryFlushMode queryFlushMode);

	@Override @Deprecated(since = "7")
	NativeQueryImplementor<R> setFlushMode(FlushModeType flushMode);

	@Override
	NativeQueryImplementor<R> setQueryPlanCacheable(boolean queryPlanCacheable);

	@Override
	NativeQueryImplementor<R> setCacheable(boolean cacheable);

	@Override
	NativeQueryImplementor<R> setCacheMode(CacheMode cacheMode);

	@Override
	CacheMode getCacheMode();

	@Override
	CacheStoreMode getCacheStoreMode();

	@Override
	CacheRetrieveMode getCacheRetrieveMode();

	@Override
	NativeQueryImplementor<R> setCacheRegion(String cacheRegion);

	@Override
	NativeQueryImplementor<R> setFetchSize(int fetchSize);

	@Override
	NativeQueryImplementor<R> setReadOnly(boolean readOnly);

	@Override
	LockMode getHibernateLockMode();

	@Override
	NativeQueryImplementor<R> setHibernateLockMode(LockMode lockMode);

	@Override
	LockModeType getLockMode();

	@Override
	NativeQueryImplementor<R> setLockMode(LockModeType lockMode);

	@Override
	NativeQueryImplementor<R> setComment(String comment);

	@Override
	int getMaxResults();

	@Override
	NativeQueryImplementor<R> setMaxResults(int maxResults);

	@Override
	int getFirstResult();

	@Override
	NativeQueryImplementor<R> setFirstResult(int startPosition);

	@Override
	NativeQueryImplementor<R> addQueryHint(String hint);

	@Override
	<X> NativeQueryImplementor<X> setTupleTransformer(TupleTransformer<X> transformer);

	@Override
	NativeQueryImplementor<R> setResultListTransformer(ResultListTransformer<R> transformer);

	@Override
	<X> NativeQueryImplementor<X> withEntityGraph(EntityGraph<X> entityGraph);

	@Override
	NativeQueryImplementor<R> setEntityGraph(EntityGraph<? super R> entityGraph);

	@Override
	NativeQueryImplementor<R> setEntityGraph(EntityGraph<? super R> graph, GraphSemantic semantic);

	@Override
	NativeQueryImplementor<R> disableFetchProfile(String profileName);

	@Override
	NativeQueryImplementor<R> enableFetchProfile(String profileName);

	@Override
	NativeQueryImplementor<R> setPage(Page page);

	@Override
	NativeQueryImplementor<R> setCacheStoreMode(CacheStoreMode cacheStoreMode);

	@Override
	NativeQueryImplementor<R> setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode);

	@Override
	NativeQueryImplementor<R> setFollowOnStrategy(Locking.FollowOn followOnStrategy);

	@Override
	NativeQueryImplementor<R> setLockScope(PessimisticLockScope lockScope);

	@Override
	NativeQueryImplementor<R> setParameter(String name, Object val);

	@Override
	<P> NativeQueryImplementor<R> setParameter(String name, P val, Type<P> type);

	@Override
	<P> NativeQueryImplementor<R> setParameter(String name, P val, Class<P> type);

	@Override @Deprecated
	NativeQueryImplementor<R> setParameter(String name, Instant value, TemporalType temporalType);

	@Override @Deprecated
	NativeQueryImplementor<R> setParameter(String name, Date value, TemporalType temporalType);

	@Override @Deprecated
	NativeQueryImplementor<R> setParameter(String name, Calendar value, TemporalType temporalType);

	@Override
	NativeQueryImplementor<R> setParameter(int position, Object val);

	@Override
	<P> NativeQueryImplementor<R> setParameter(int position, P val, Class<P> type);

	@Override
	<P> NativeQueryImplementor<R> setParameter(int position, P val, Type<P> type);

	@Override @Deprecated
	NativeQueryImplementor<R> setParameter(int position, Instant value, TemporalType temporalType);

	@Override @Deprecated
	NativeQueryImplementor<R> setParameter(int position, Date value, TemporalType temporalType);

	@Override @Deprecated
	NativeQueryImplementor<R> setParameter(int position, Calendar value, TemporalType temporalType);

	@Override
	<P> NativeQueryImplementor<R> setParameter(QueryParameter<P> parameter, P val);

	@Override
	<P> NativeQueryImplementor<R> setParameter(QueryParameter<P> parameter, P val, Class<P> type);

	@Override
	<P> NativeQueryImplementor<R> setParameter(QueryParameter<P> parameter, P val, Type<P> type);

	@Override
	<P> NativeQueryImplementor<R> setParameter(Parameter<P> param, P value);

	@Override @Deprecated
	NativeQueryImplementor<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType);

	@Override @Deprecated
	NativeQueryImplementor<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override
	NativeQueryImplementor<R> setParameterList(String name, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> NativeQueryImplementor<R> setParameterList(String name, Collection<? extends P> values, Class<P> type);

	@Override
	<P> NativeQueryImplementor<R> setParameterList(String name, Collection<? extends P> values, Type<P> type);

	@Override
	NativeQueryImplementor<R> setParameterList(String name, Object[] values);

	@Override
	<P> NativeQueryImplementor<R> setParameterList(String name, P[] values, Class<P> type);

	@Override
	<P> NativeQueryImplementor<R> setParameterList(String name, P[] values, Type<P> type);

	@Override
	NativeQueryImplementor<R> setParameterList(int position, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> NativeQueryImplementor<R> setParameterList(int position, Collection<? extends P> values, Class<P> type);

	@Override
	<P> NativeQueryImplementor<R> setParameterList(int position, Collection<? extends P> values, Type<P> type);

	@Override
	NativeQueryImplementor<R> setParameterList(int position, Object[] values);

	@Override
	<P> NativeQueryImplementor<R> setParameterList(int position, P[] values, Class<P> javaType);

	@Override
	<P> NativeQueryImplementor<R> setParameterList(int position, P[] values, Type<P> type);


	@Override
	<P> NativeQueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values);

	@Override
	<P> NativeQueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> NativeQueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Type<P> type);

	@Override
	<P> NativeQueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values);

	@Override
	<P> NativeQueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType);

	@Override
	<P> NativeQueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values, Type<P> type);

	@Override
	NativeQueryImplementor<R> setProperties(Object bean);

	@Override
	NativeQueryImplementor<R> setProperties(@SuppressWarnings("rawtypes") Map bean);

	@Override
	<P> NativeQueryImplementor<R> setConvertedParameter(String name, P value, Class<? extends AttributeConverter<P, ?>> converter);

	@Override
	<P> NativeQueryImplementor<R> setConvertedParameter(int position, P value, Class<? extends AttributeConverter<P, ?>> converter);

	@SuppressWarnings("unused") // Used by Hibernate Reactive
	void addResultTypeClass(Class<?> resultClass);



	@Override
	NativeQueryImplementor<R> addScalar(String columnAlias);

	@Override
	NativeQueryImplementor<R> addScalar(String columnAlias, @SuppressWarnings("rawtypes") BasicDomainType type);

	@Override
	NativeQueryImplementor<R> addScalar(String columnAlias, @SuppressWarnings("rawtypes") Class javaType);

	@Override
	<C> NativeQueryImplementor<R> addScalar(String columnAlias, Class<C> relationalJavaType, AttributeConverter<?,C> converter);

	@Override
	<O, J> NativeQueryImplementor<R> addScalar(String columnAlias, Class<O> domainJavaType, Class<J> jdbcJavaType, AttributeConverter<O, J> converter);

	@Override
	<C> NativeQueryImplementor<R> addScalar(String columnAlias, Class<C> relationalJavaType, Class<? extends AttributeConverter<?,C>> converter);

	@Override
	<O, J> NativeQueryImplementor<R> addScalar(
			String columnAlias,
			Class<O> domainJavaType,
			Class<J> jdbcJavaType,
			Class<? extends AttributeConverter<O, J>> converter);

	@Override
	NativeQueryImplementor<R> addAttributeResult(String columnAlias, @SuppressWarnings("rawtypes") Class entityJavaType, String attributePath);

	@Override
	NativeQueryImplementor<R> addAttributeResult(String columnAlias, String entityName, String attributePath);

	@Override
	NativeQueryImplementor<R> addAttributeResult(String columnAlias, @SuppressWarnings("rawtypes") SingularAttribute attribute);

	@Override
	DynamicResultBuilderEntityStandard addRoot(String tableAlias, String entityName);

	@Override
	NativeQueryImplementor<R> addEntity(String entityName);

	@Override
	NativeQueryImplementor<R> addEntity(String tableAlias, String entityName);

	@Override
	NativeQueryImplementor<R> addEntity(String tableAlias, String entityName, LockMode lockMode);

	@Override
	NativeQueryImplementor<R> addEntity(@SuppressWarnings("rawtypes") Class entityType);

	NativeQueryImplementor<R> addEntity(Class<R> entityType, LockMode lockMode);

	@Override
	NativeQueryImplementor<R> addEntity(String tableAlias, @SuppressWarnings("rawtypes") Class entityType);

	@Override
	NativeQueryImplementor<R> addEntity(String tableAlias, @SuppressWarnings("rawtypes") Class entityClass, LockMode lockMode);

	@Override
	NativeQueryImplementor<R> addJoin(String tableAlias, String path);

	@Override
	NativeQueryImplementor<R> addJoin(
			String tableAlias,
			String ownerTableAlias,
			String joinPropertyName);

	@Override
	NativeQueryImplementor<R> addJoin(String tableAlias, String path, LockMode lockMode);
}
