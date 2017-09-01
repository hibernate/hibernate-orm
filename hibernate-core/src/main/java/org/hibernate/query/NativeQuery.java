/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.TemporalType;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.SQLQuery;
import org.hibernate.SynchronizeableQuery;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public interface NativeQuery<T> extends Query<T>, SQLQuery<T>, SynchronizeableQuery<T> {

	@Override
	NativeQuery<T> setFlushMode(FlushMode flushMode);

	@Override
	NativeQuery<T> setResultSetMapping(String name);

	@Override
	<P> NativeQuery<T> setParameter(QueryParameter<P> parameter, P val);

	@Override
	<P> NativeQuery<T> setParameter(Parameter<P> param, P value);

	@Override
	<P> NativeQuery<T> setParameter(QueryParameter<P> parameter, P val, Type type);

	@Override
	NativeQuery<T> setParameter(String name, Object val, Type type);

	@Override
	NativeQuery<T> setParameter(int position, Object val, Type type);

	@Override
	<P> NativeQuery<T> setParameter(QueryParameter<P> parameter, P val, TemporalType temporalType);

	@Override
	NativeQuery<T> setParameter(String name, Object val, TemporalType temporalType);

	@Override
	NativeQuery<T> setParameter(int position, Object val, TemporalType temporalType);

	@Override
	NativeQuery<T> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override
	NativeQuery<T> setParameter(Parameter<Date> param, Date value, TemporalType temporalType);

	@Override
	NativeQuery<T> setParameter(String name, Object value);

	@Override
	NativeQuery<T> setParameter(String name, Calendar value, TemporalType temporalType);

	@Override
	NativeQuery<T> setParameter(String name, Date value, TemporalType temporalType);

	@Override
	NativeQuery<T> setParameter(int position, Object value);

	@Override
	NativeQuery<T> setParameter(int position, Calendar value, TemporalType temporalType);

	@Override
	NativeQuery<T> setParameter(int position, Date value, TemporalType temporalType);

	@Override
	NativeQuery<T> setParameter(Parameter<Instant> param, Instant value, TemporalType temporalType);

	@Override
	NativeQuery<T> setParameter(Parameter<LocalDateTime> param, LocalDateTime value, TemporalType temporalType);

	@Override
	NativeQuery<T> setParameter(Parameter<ZonedDateTime> param, ZonedDateTime value, TemporalType temporalType);

	@Override
	NativeQuery<T> setParameter(Parameter<OffsetDateTime> param, OffsetDateTime value, TemporalType temporalType);

	@Override
	NativeQuery<T> setParameter(String name, Instant value, TemporalType temporalType);

	@Override
	NativeQuery<T> setParameter(String name, LocalDateTime value, TemporalType temporalType);

	@Override
	NativeQuery<T> setParameter(String name, ZonedDateTime value, TemporalType temporalType);

	@Override
	NativeQuery<T> setParameter(String name, OffsetDateTime value, TemporalType temporalType);

	@Override
	NativeQuery<T> setParameter(int position, Instant value, TemporalType temporalType);

	@Override
	NativeQuery<T> setParameter(int position, LocalDateTime value, TemporalType temporalType);

	@Override
	NativeQuery<T> setParameter(int position, ZonedDateTime value, TemporalType temporalType);

	@Override
	NativeQuery<T> setParameter(int position, OffsetDateTime value, TemporalType temporalType);

	@Override
	<P> NativeQuery<T> setParameterList(QueryParameter<P> parameter, Collection<P> values);

	@Override
	NativeQuery<T> setParameterList(String name, Collection values);

	@Override
	NativeQuery<T> setParameterList(String name, Collection values, Type type);

	@Override
	NativeQuery<T> setParameterList(String name, Object[] values, Type type);

	@Override
	NativeQuery<T> setParameterList(String name, Object[] values);

	@Override
	NativeQuery<T> setProperties(Object bean);

	@Override
	NativeQuery<T> setProperties(Map bean);

	@Override
	NativeQuery<T> addSynchronizedQuerySpace(String querySpace);

	@Override
	NativeQuery<T> addSynchronizedEntityName(String entityName) throws MappingException;

	@Override
	NativeQuery<T> addSynchronizedEntityClass(Class entityClass) throws MappingException;

	@Override
	boolean isCallable();

	@Override
	NativeQuery<T> addScalar(String columnAlias);

	@Override
	NativeQuery<T> addScalar(String columnAlias, Type type);

	@Override
	RootReturn addRoot(String tableAlias, String entityName);

	@Override
	RootReturn addRoot(String tableAlias, Class entityType);

	@Override
	NativeQuery<T> addEntity(String entityName);

	@Override
	NativeQuery<T> addEntity(String tableAlias, String entityName);

	@Override
	NativeQuery<T> addEntity(String tableAlias, String entityName, LockMode lockMode);

	@Override
	NativeQuery<T> addEntity(Class entityType);

	@Override
	NativeQuery<T> addEntity(String tableAlias, Class entityType);

	@Override
	NativeQuery<T> addEntity(String tableAlias, Class entityClass, LockMode lockMode);

	@Override
	FetchReturn addFetch(String tableAlias, String ownerTableAlias, String joinPropertyName);

	@Override
	NativeQuery<T> addJoin(String tableAlias, String path);

	@Override
	NativeQuery<T> addJoin(String tableAlias, String ownerTableAlias, String joinPropertyName);

	@Override
	NativeQuery<T> addJoin(String tableAlias, String path, LockMode lockMode);

	@Override
	NativeQuery<T> setHibernateFlushMode(FlushMode flushMode);

	@Override
	NativeQuery<T> setFlushMode(FlushModeType flushMode);

	@Override
	NativeQuery<T> setCacheMode(CacheMode cacheMode);

	@Override
	NativeQuery<T> setCacheable(boolean cacheable);

	@Override
	NativeQuery<T> setCacheRegion(String cacheRegion);

	@Override
	NativeQuery<T> setTimeout(int timeout);

	@Override
	NativeQuery<T> setFetchSize(int fetchSize);

	@Override
	NativeQuery<T> setReadOnly(boolean readOnly);

	@Override
	NativeQuery<T> setLockOptions(LockOptions lockOptions);

	@Override
	NativeQuery<T> setLockMode(String alias, LockMode lockMode);

	@Override
	NativeQuery<T> setComment(String comment);

	@Override
	NativeQuery<T> addQueryHint(String hint);

	@Override
	NativeQuery<T> setMaxResults(int maxResult);

	@Override
	NativeQuery<T> setFirstResult(int startPosition);

	@Override
	NativeQuery<T> setHint(String hintName, Object value);

	@Override
	NativeQuery<T> setLockMode(LockModeType lockMode);
}
