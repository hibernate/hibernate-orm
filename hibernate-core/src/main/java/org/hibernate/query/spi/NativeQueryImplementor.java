/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.spi;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import javax.persistence.Parameter;
import javax.persistence.TemporalType;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.QueryParameter;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
@Incubating
public interface NativeQueryImplementor<T> extends QueryImplementor<T>, NativeQuery<T> {
	NativeQueryImplementor setCollectionKey(Serializable key);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// overrides

	@Override
	NativeQueryImplementor<T> addScalar(String columnAlias);

	@Override
	NativeQueryImplementor<T> addScalar(String columnAlias, Type type);

	@Override
	RootReturn addRoot(String tableAlias, String entityName);

	@Override
	NativeQueryImplementor<T> addEntity(String entityName);

	@Override
	NativeQueryImplementor<T> addEntity(String tableAlias, String entityName);

	@Override
	NativeQueryImplementor<T> addEntity(String tableAlias, String entityName, LockMode lockMode);

	@Override
	NativeQueryImplementor<T> addEntity(Class entityType);

	@Override
	NativeQueryImplementor<T> addEntity(String tableAlias, Class entityType);

	@Override
	NativeQueryImplementor<T> addEntity(String tableAlias, Class entityClass, LockMode lockMode);

	@Override
	NativeQueryImplementor<T> addJoin(String tableAlias, String path);

	@Override
	NativeQueryImplementor<T> addJoin(String tableAlias, String ownerTableAlias, String joinPropertyName);

	@Override
	NativeQueryImplementor<T> addJoin(String tableAlias, String path, LockMode lockMode);

	@Override
	NativeQueryImplementor<T> setHibernateFlushMode(FlushMode flushMode);

	@Override
	NativeQueryImplementor<T> setCacheMode(CacheMode cacheMode);

	@Override
	NativeQueryImplementor<T> setCacheable(boolean cacheable);

	@Override
	NativeQueryImplementor<T> setCacheRegion(String cacheRegion);

	@Override
	NativeQueryImplementor<T> setTimeout(int timeout);

	@Override
	NativeQueryImplementor<T> setFetchSize(int fetchSize);

	@Override
	NativeQueryImplementor<T> setReadOnly(boolean readOnly);

	@Override
	NativeQueryImplementor<T> setLockOptions(LockOptions lockOptions);

	@Override
	NativeQueryImplementor<T> setLockMode(String alias, LockMode lockMode);

	@Override
	NativeQueryImplementor<T> setComment(String comment);

	@Override
	NativeQueryImplementor<T> addQueryHint(String hint);

	@Override
	<P> NativeQueryImplementor<T> setParameter(QueryParameter<P> parameter, P val);

	@Override
	<P> NativeQueryImplementor<T> setParameter(Parameter<P> param, P value);

	@Override
	NativeQueryImplementor<T> setParameter(String name, Object val);

	@Override
	<P> NativeQueryImplementor<T> setParameter(QueryParameter<P> parameter, P val, Type type);

	@Override
	NativeQueryImplementor<T> setParameter(int position, Object val);

	@Override
	NativeQueryImplementor<T> setParameter(String name, Object val, Type type);

	@Override
	NativeQueryImplementor<T> setParameter(int position, Object val, Type type);

	@Override
	<P> NativeQueryImplementor<T> setParameter(QueryParameter<P> parameter, P val, TemporalType temporalType);

	@Override
	NativeQueryImplementor<T> setParameter(String name, Object val, TemporalType temporalType);

	@Override
	NativeQueryImplementor<T> setParameter(int position, Object val, TemporalType temporalType);

	@Override
	<P> NativeQueryImplementor<T> setParameterList(QueryParameter<P> parameter, Collection<P> values);

	@Override
	NativeQueryImplementor<T> setParameterList(String name, Collection values);

	@Override
	NativeQueryImplementor<T> setParameterList(String name, Collection values, Type type);

	@Override
	NativeQueryImplementor<T> setParameterList(String name, Object[] values, Type type);

	@Override
	NativeQueryImplementor<T> setParameterList(String name, Object[] values);

	@Override
	NativeQueryImplementor<T> setProperties(Object bean);

	@Override
	NativeQueryImplementor<T> setProperties(Map bean);

	@Override
	NativeQueryImplementor<T> setParameter(Parameter<Date> param, Date value, TemporalType temporalType);

	@Override
	NativeQueryImplementor<T> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override
	NativeQueryImplementor<T> setParameter(String name, Date value, TemporalType temporalType);

	@Override
	NativeQueryImplementor<T> setParameter(String name, Calendar value, TemporalType temporalType);

	@Override
	NativeQueryImplementor<T> setParameter(int position, Calendar value, TemporalType temporalType);

	@Override
	NativeQueryImplementor<T> setParameter(int position, Date value, TemporalType temporalType);

	@Override
	NativeQueryImplementor<T> addSynchronizedQuerySpace(String querySpace);

	@Override
	NativeQueryImplementor<T> addSynchronizedEntityName(String entityName) throws MappingException;

	@Override
	NativeQueryImplementor<T> setFlushMode(FlushMode flushMode);

	@Override
	NativeQueryImplementor<T> addSynchronizedEntityClass(Class entityClass) throws MappingException;
}
