/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.spi;

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
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.Type;
import org.hibernate.CacheMode;
import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.MappingException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Page;
import jakarta.persistence.QueryFlushMode;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.named.NameableQuery;
import org.hibernate.query.named.NamedMutationMemento;
import org.hibernate.query.named.NamedNativeQueryMemento;
import org.hibernate.query.named.internal.NativeSelectionMementoImpl;
import org.hibernate.query.results.internal.dynamic.DynamicResultBuilderEntityStandard;
import org.hibernate.query.spi.MutationQueryImplementor;
import org.hibernate.query.spi.SelectionQueryImplementor;

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

	@Override @SuppressWarnings({"unchecked", "rawtypes"})
	Set getOptions();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Casts

	@Override
	default NativeQueryImplementor<R> asMutationQuery() {
		return (NativeQueryImplementor<R>) MutationQueryImplementor.super.asMutationQuery();
	}

	@Override
	NativeQueryImplementor<R> asSelectionQuery();

	@Override
	<X> NativeQueryImplementor<X> asSelectionQuery(Class<X> type);

	@Override
	<X> NativeQueryImplementor<X> asSelectionQuery(EntityGraph<X> entityGraph);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Executions

	@Override
	@SuppressWarnings("deprecation")
	@Nonnull
	List<R> list();

	@Override
	@SuppressWarnings("deprecation")
	@Nonnull
	default List<R> getResultList() {
		return SelectionQueryImplementor.super.getResultList();
	}

	@Override
	@SuppressWarnings("deprecation")
	@Nonnull
	ScrollableResults<R> scroll();

	@Override
	@SuppressWarnings("deprecation")
	@Nonnull
	ScrollableResults<R> scroll(@Nonnull ScrollMode scrollMode);

	@Override
	@SuppressWarnings("deprecation")
	@Nonnull
	default Stream<R> getResultStream() {
		return SelectionQueryImplementor.super.getResultStream();
	}

	@Override
	@SuppressWarnings("deprecation")
	@Nonnull
	default Stream<R> stream() {
		return SelectionQueryImplementor.super.stream();
	}

	@Override
	@SuppressWarnings("deprecation")
	@Nullable
	R uniqueResult();

	@Override
	@SuppressWarnings("deprecation")
	@Nonnull
	Optional<R> uniqueResultOptional();

	@Override
	@SuppressWarnings("deprecation")
	R getSingleResult();

	@Override
	@SuppressWarnings("deprecation")
	@Nullable
	R getSingleResultOrNull();

	@Override
	int execute();

	@Override
	@SuppressWarnings({"deprecation", "removal"})
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
	@Nonnull
	NativeQueryImplementor<R> setHint(@Nonnull String hintName, @Nullable Object value);

	@Override
	@Nonnull
	NativeQueryImplementor<R> setTimeout(int timeout);

	@Override
	@Nonnull
	NativeQueryImplementor<R> setTimeout(@Nullable Integer timeout);

	@Override
	@Nonnull
	NativeQueryImplementor<R> setTimeout(@Nullable Timeout timeout);

	@Override
	@Nonnull
	NativeQueryImplementor<R> setQueryFlushMode(@Nonnull QueryFlushMode queryFlushMode);

	@Override @Deprecated(since = "7")
	@Nonnull
	NativeQueryImplementor<R> setFlushMode(@Nonnull FlushModeType flushMode);

	@Override
	@Nonnull
	NativeQueryImplementor<R> setQueryPlanCacheable(boolean queryPlanCacheable);

	@Override @SuppressWarnings("removal")
	@Nonnull
	NativeQueryImplementor<R> setCacheable(boolean cacheable);

	@Override @SuppressWarnings("removal")
	@Nonnull
	NativeQueryImplementor<R> setCacheMode(@Nonnull CacheMode cacheMode);

	@Override @SuppressWarnings("removal")
	@Nonnull
	CacheMode getCacheMode();

	@Override @SuppressWarnings("deprecation")
	@Nonnull
	CacheStoreMode getCacheStoreMode();

	@Override @SuppressWarnings("deprecation")
	@Nonnull
	CacheRetrieveMode getCacheRetrieveMode();

	@Override @SuppressWarnings("removal")
	NativeQueryImplementor<R> setCacheRegion(@Nullable String cacheRegion);

	@Override @SuppressWarnings("removal")
	@Nonnull
	NativeQueryImplementor<R> setFetchSize(int fetchSize);

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	NativeQueryImplementor<R> setReadOnly(boolean readOnly);

	@Override
	@Nonnull
	LockMode getHibernateLockMode();

	@Override
	@Nonnull
	NativeQueryImplementor<R> setHibernateLockMode(@Nonnull LockMode lockMode);

	@Override @SuppressWarnings("deprecation")
	@Nullable
	LockModeType getLockMode();

	@Override @SuppressWarnings("deprecation")
	@Nonnull
	NativeQueryImplementor<R> setLockMode(@Nonnull LockModeType lockMode);

	@Override
	@Nonnull
	NativeQueryImplementor<R> setComment(@Nullable String comment);

	@Override @SuppressWarnings("deprecation")
	int getMaxResults();

	@Override @SuppressWarnings("deprecation")
	@Nonnull
	NativeQueryImplementor<R> setMaxResults(int maxResults);

	@Override @SuppressWarnings("deprecation")
	int getFirstResult();

	@Override @SuppressWarnings("deprecation")
	@Nonnull
	NativeQueryImplementor<R> setFirstResult(int startPosition);

	@Override
	@Nonnull
	NativeQueryImplementor<R> addQueryHint(@Nonnull String hint);

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	<X> NativeQueryImplementor<X> setTupleTransformer(@Nonnull TupleTransformer<X> transformer);

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	NativeQueryImplementor<R> setResultListTransformer(@Nonnull ResultListTransformer<R> transformer);

	@Override @Deprecated @SuppressWarnings("removal")
	@Nonnull
	NativeQueryImplementor<R> setEntityGraph(@Nonnull EntityGraph<? super R> entityGraph);

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	NativeQueryImplementor<R> setEntityGraph(@Nonnull EntityGraph<? super R> graph, @Nonnull GraphSemantic semantic);

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	NativeQueryImplementor<R> disableFetchProfile(@Nonnull String profileName);

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	NativeQueryImplementor<R> enableFetchProfile(@Nonnull String profileName);

	@Override
	NativeQueryImplementor<R> setPage(Page page);

	@Override @SuppressWarnings("deprecation")
	@Nonnull
	NativeQueryImplementor<R> setCacheStoreMode(@Nonnull CacheStoreMode cacheStoreMode);

	@Override @SuppressWarnings("deprecation")
	@Nonnull
	NativeQueryImplementor<R> setCacheRetrieveMode(@Nonnull CacheRetrieveMode cacheRetrieveMode);

	@Override
	@Nonnull
	NativeQueryImplementor<R> setFollowOnStrategy(@Nonnull Locking.FollowOn followOnStrategy);

	@Override
	@Nonnull
	NativeQueryImplementor<R> setLockScope(@Nonnull PessimisticLockScope lockScope);

	@Override
	@Nonnull
	NativeQueryImplementor<R> setParameter(@Nonnull String name, @Nullable Object val);

	@Override
	@Nonnull
	<P> NativeQueryImplementor<R> setParameter(@Nonnull String name, @Nullable P val, @Nonnull Type<P> type);

	@Override
	@Nonnull
	<P> NativeQueryImplementor<R> setParameter(@Nonnull String name, @Nullable P val, @Nonnull Class<P> type);

	@Override @Deprecated
	@Nonnull
	NativeQueryImplementor<R> setParameter(@Nonnull String name, @Nullable Instant value, @Nonnull TemporalType temporalType);

	@Override @Deprecated
	@Nonnull
	NativeQueryImplementor<R> setParameter(@Nonnull String name, @Nullable Date value, @Nonnull TemporalType temporalType);

	@Override @Deprecated
	@Nonnull
	NativeQueryImplementor<R> setParameter(@Nonnull String name, @Nullable Calendar value, @Nonnull TemporalType temporalType);

	@Override
	@Nonnull
	NativeQueryImplementor<R> setParameter(int position, @Nullable Object val);

	@Override
	@Nonnull
	<P> NativeQueryImplementor<R> setParameter(int position, @Nullable P val, @Nonnull Class<P> type);

	@Override
	@Nonnull
	<P> NativeQueryImplementor<R> setParameter(int position, @Nullable P val, @Nonnull Type<P> type);

	@Override @Deprecated
	@Nonnull
	NativeQueryImplementor<R> setParameter(int position, @Nullable Instant value, @Nonnull TemporalType temporalType);

	@Override @Deprecated
	@Nonnull
	NativeQueryImplementor<R> setParameter(int position, @Nullable Date value, @Nonnull TemporalType temporalType);

	@Override @Deprecated
	@Nonnull
	NativeQueryImplementor<R> setParameter(int position, @Nullable Calendar value, @Nonnull TemporalType temporalType);

	@Override
	@Nonnull
	<P> NativeQueryImplementor<R> setParameter(@Nonnull QueryParameter<P> parameter, @Nullable P val);

	@Override
	@Nonnull
	<P> NativeQueryImplementor<R> setParameter(@Nonnull QueryParameter<P> parameter, @Nullable P val, @Nonnull Class<P> type);

	@Override
	@Nonnull
	<P> NativeQueryImplementor<R> setParameter(@Nonnull QueryParameter<P> parameter, @Nullable P val, @Nonnull Type<P> type);

	@Override
	@Nonnull
	<P> NativeQueryImplementor<R> setParameter(@Nonnull Parameter<P> param, @Nullable P value);

	@Override @Deprecated
	@Nonnull
	NativeQueryImplementor<R> setParameter(@Nonnull Parameter<Date> param, @Nullable Date value, @Nonnull TemporalType temporalType);

	@Override @Deprecated
	@Nonnull
	NativeQueryImplementor<R> setParameter(@Nonnull Parameter<Calendar> param, @Nullable Calendar value, @Nonnull TemporalType temporalType);

	@Override
	@Nonnull
	NativeQueryImplementor<R> setParameterList(@Nonnull String name, @SuppressWarnings("rawtypes") @Nonnull Collection values);

	@Override
	@Nonnull
	<P> NativeQueryImplementor<R> setParameterList(@Nonnull String name, @Nonnull Collection<? extends P> values, @Nonnull Class<P> type);

	@Override
	@Nonnull
	<P> NativeQueryImplementor<R> setParameterList(@Nonnull String name, @Nonnull Collection<? extends P> values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	NativeQueryImplementor<R> setParameterList(@Nonnull String name, @Nonnull Object[] values);

	@Override
	@Nonnull
	<P> NativeQueryImplementor<R> setParameterList(@Nonnull String name, @Nonnull P[] values, @Nonnull Class<P> type);

	@Override
	@Nonnull
	<P> NativeQueryImplementor<R> setParameterList(@Nonnull String name, @Nonnull P[] values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	NativeQueryImplementor<R> setParameterList(int position, @SuppressWarnings("rawtypes") @Nonnull Collection values);

	@Override
	@Nonnull
	<P> NativeQueryImplementor<R> setParameterList(int position, @Nonnull Collection<? extends P> values, @Nonnull Class<P> type);

	@Override
	@Nonnull
	<P> NativeQueryImplementor<R> setParameterList(int position, @Nonnull Collection<? extends P> values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	NativeQueryImplementor<R> setParameterList(int position, @Nonnull Object[] values);

	@Override
	@Nonnull
	<P> NativeQueryImplementor<R> setParameterList(int position, @Nonnull P[] values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> NativeQueryImplementor<R> setParameterList(int position, @Nonnull P[] values, @Nonnull Type<P> type);


	@Override
	@Nonnull
	<P> NativeQueryImplementor<R> setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull Collection<? extends P> values);

	@Override
	@Nonnull
	<P> NativeQueryImplementor<R> setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull Collection<? extends P> values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> NativeQueryImplementor<R> setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull Collection<? extends P> values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	<P> NativeQueryImplementor<R> setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values);

	@Override
	@Nonnull
	<P> NativeQueryImplementor<R> setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> NativeQueryImplementor<R> setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	NativeQueryImplementor<R> setProperties(@Nonnull Object bean);

	@Override
	@Nonnull
	NativeQueryImplementor<R> setProperties(@SuppressWarnings("rawtypes") @Nonnull Map bean);

	@Override
	@Nonnull
	<P> NativeQueryImplementor<R> setConvertedParameter(@Nonnull String name, @Nullable P value, @Nonnull Class<? extends AttributeConverter<P, ?>> converter);

	@Override
	@Nonnull
	<P> NativeQueryImplementor<R> setConvertedParameter(int position, @Nullable P value, @Nonnull Class<? extends AttributeConverter<P, ?>> converter);

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
