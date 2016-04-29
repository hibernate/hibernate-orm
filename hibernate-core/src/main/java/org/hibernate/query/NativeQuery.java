/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import javax.persistence.Parameter;
import javax.persistence.TemporalType;

import org.hibernate.FlushMode;
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
}
