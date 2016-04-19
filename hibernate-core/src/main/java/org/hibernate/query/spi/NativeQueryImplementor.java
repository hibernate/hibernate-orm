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
public interface NativeQueryImplementor extends QueryImplementor<Object>, NativeQuery {
	NativeQueryImplementor setCollectionKey(Serializable key);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// overrides

	@Override
	NativeQueryImplementor addScalar(String columnAlias);

	@Override
	NativeQueryImplementor addScalar(String columnAlias, Type type);

	@Override
	RootReturn addRoot(String tableAlias, String entityName);

	@Override
	NativeQueryImplementor addEntity(String entityName);

	@Override
	NativeQueryImplementor addEntity(String tableAlias, String entityName);

	@Override
	NativeQueryImplementor addEntity(String tableAlias, String entityName, LockMode lockMode);

	@Override
	NativeQueryImplementor addEntity(Class entityType);

	@Override
	NativeQueryImplementor addEntity(String tableAlias, Class entityType);

	@Override
	NativeQueryImplementor addEntity(String tableAlias, Class entityClass, LockMode lockMode);

	@Override
	NativeQueryImplementor addJoin(String tableAlias, String path);

	@Override
	NativeQueryImplementor addJoin(String tableAlias, String ownerTableAlias, String joinPropertyName);

	@Override
	NativeQueryImplementor addJoin(String tableAlias, String path, LockMode lockMode);

	@Override
	NativeQueryImplementor setHibernateFlushMode(FlushMode flushMode);

	@Override
	NativeQueryImplementor setCacheMode(CacheMode cacheMode);

	@Override
	NativeQueryImplementor setCacheable(boolean cacheable);

	@Override
	NativeQuery setCacheRegion(String cacheRegion);

	@Override
	NativeQueryImplementor setTimeout(int timeout);

	@Override
	NativeQueryImplementor setFetchSize(int fetchSize);

	@Override
	NativeQueryImplementor setReadOnly(boolean readOnly);

	@Override
	NativeQueryImplementor setLockOptions(LockOptions lockOptions);

	@Override
	NativeQueryImplementor setLockMode(String alias, LockMode lockMode);

	@Override
	NativeQueryImplementor setComment(String comment);

	@Override
	NativeQueryImplementor addQueryHint(String hint);

	@Override
	<T> NativeQueryImplementor setParameter(QueryParameter<T> parameter, T val);

	@Override
	<T> NativeQueryImplementor setParameter(Parameter<T> param, T value);

	@Override
	NativeQueryImplementor setParameter(String name, Object val);

	@Override
	<P> NativeQueryImplementor setParameter(QueryParameter<P> parameter, P val, Type type);

	@Override
	NativeQueryImplementor setParameter(int position, Object val);

	@Override
	NativeQueryImplementor setParameter(String name, Object val, Type type);

	@Override
	NativeQueryImplementor setParameter(int position, Object val, Type type);

	@Override
	<P> NativeQueryImplementor setParameter(QueryParameter<P> parameter, P val, TemporalType temporalType);

	@Override
	NativeQueryImplementor setParameter(String name, Object val, TemporalType temporalType);

	@Override
	NativeQueryImplementor setParameter(int position, Object val, TemporalType temporalType);

	@Override
	<P> NativeQueryImplementor setParameterList(QueryParameter<P> parameter, Collection<P> values);

	@Override
	NativeQueryImplementor setParameterList(String name, Collection values);

	@Override
	NativeQueryImplementor setParameterList(String name, Collection values, Type type);

	@Override
	NativeQueryImplementor setParameterList(String name, Object[] values, Type type);

	@Override
	NativeQueryImplementor setParameterList(String name, Object[] values);

	@Override
	NativeQueryImplementor setProperties(Object bean);

	@Override
	NativeQueryImplementor setProperties(Map bean);

	@Override
	NativeQueryImplementor setParameter(Parameter<Date> param, Date value, TemporalType temporalType);

	@Override
	NativeQueryImplementor setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override
	NativeQueryImplementor setParameter(String name, Date value, TemporalType temporalType);

	@Override
	NativeQueryImplementor setParameter(String name, Calendar value, TemporalType temporalType);

	@Override
	NativeQueryImplementor setParameter(int position, Calendar value, TemporalType temporalType);

	@Override
	NativeQueryImplementor setParameter(int position, Date value, TemporalType temporalType);

	@Override
	NativeQueryImplementor addSynchronizedQuerySpace(String querySpace);

	@Override
	NativeQueryImplementor addSynchronizedEntityName(String entityName) throws MappingException;

	@Override
	NativeQueryImplementor setFlushMode(FlushMode flushMode);

	@Override
	NativeQueryImplementor addSynchronizedEntityClass(Class entityClass) throws MappingException;
}
