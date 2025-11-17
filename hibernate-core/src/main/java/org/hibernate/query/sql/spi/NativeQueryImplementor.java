/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.spi;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.query.QueryFlushMode;
import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.named.NameableQuery;
import org.hibernate.query.results.internal.dynamic.DynamicResultBuilderEntityStandard;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.transform.ResultTransformer;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;
import jakarta.persistence.metamodel.Type;
import jakarta.persistence.metamodel.SingularAttribute;

/**
 * @author Steve Ebersole
 */
@Incubating
public interface NativeQueryImplementor<R> extends QueryImplementor<R>, NativeQuery<R>, NameableQuery {

	/**
	 * Best guess whether this is a select query.  {@code null}
	 * indicates unknown
	 */
	Boolean isSelectQuery();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// covariant overrides - NativeQuery


	@Override @Deprecated @SuppressWarnings("deprecation")
	default <T> NativeQueryImplementor<T> setResultTransformer(ResultTransformer<T> transformer) {
		QueryImplementor.super.setResultTransformer( transformer );
		//noinspection unchecked
		return (NativeQueryImplementor<T>) this;
	}

	@Override
	NamedNativeQueryMemento<R> toMemento(String name);

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

	@Override @Deprecated(since = "7")
	NativeQueryImplementor<R> setHibernateFlushMode(FlushMode flushMode);

	@Override
	NativeQueryImplementor<R> setQueryFlushMode(QueryFlushMode queryFlushMode);

	@Override @Deprecated(since = "7")
	NativeQueryImplementor<R> setFlushMode(FlushModeType flushMode);

	@Override
	NativeQueryImplementor<R> setCacheMode(CacheMode cacheMode);

	@Override
	NativeQueryImplementor<R> setCacheable(boolean cacheable);

	@Override
	NativeQueryImplementor<R> setCacheRegion(String cacheRegion);

	@Override
	NativeQueryImplementor<R> setTimeout(int timeout);

	@Override
	NativeQueryImplementor<R> setFetchSize(int fetchSize);

	@Override
	NativeQueryImplementor<R> setReadOnly(boolean readOnly);

	@Override @Deprecated
	NativeQueryImplementor<R> setLockOptions(LockOptions lockOptions);

	@Override
	NativeQueryImplementor<R> setHibernateLockMode(LockMode lockMode);

	@Override
	NativeQueryImplementor<R> setLockMode(LockModeType lockMode);

	@Override
	NativeQueryImplementor<R> setComment(String comment);

	@Override
	NativeQueryImplementor<R> setMaxResults(int maxResults);

	@Override
	NativeQueryImplementor<R> setFirstResult(int startPosition);

	@Override
	NativeQueryImplementor<R> addQueryHint(String hint);

	@Override
	<T> NativeQueryImplementor<T> setTupleTransformer(TupleTransformer<T> transformer);

	@Override
	NativeQueryImplementor<R> setResultListTransformer(ResultListTransformer<R> transformer);

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

	@SuppressWarnings("unused") // Used by Hibernate Reactive
	void addResultTypeClass(Class<?> resultClass);
}
