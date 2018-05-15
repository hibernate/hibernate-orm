/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query.spi;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.TemporalType;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.query.Query;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;

/**
 * @author Steve Ebersole
 */
@Incubating
public interface QueryImplementor<R> extends Query<R> {
	void setOptionalId(Serializable id);

	void setOptionalEntityName(String entityName);

	void setOptionalObject(Object optionalObject);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// covariant overrides


	@Override
	ParameterMetadataImplementor<QueryParameterImplementor<?>> getParameterMetadata();

	@Override
	SharedSessionContractImplementor getSession();

	@Override
	QueryImplementor<R> setHibernateFlushMode(FlushMode flushMode);

	@Override
	QueryImplementor<R> setCacheMode(CacheMode cacheMode);

	@Override
	QueryImplementor<R> setCacheable(boolean cacheable);

	@Override
	QueryImplementor<R> setCacheRegion(String cacheRegion);

	@Override
	QueryImplementor<R> setTimeout(int timeout);

	@Override
	QueryImplementor<R> setFetchSize(int fetchSize);

	@Override
	QueryImplementor<R> setReadOnly(boolean readOnly);

	@Override
	QueryImplementor<R> setComment(String comment);

	@Override
	QueryImplementor<R> addQueryHint(String hint);

	@Override
	QueryImplementor<R> setLockOptions(LockOptions lockOptions);

	@Override
	QueryImplementor<R> setLockMode(String alias, LockMode lockMode);

	@Override
	QueryImplementor<R> setMaxResults(int maxResult);

	@Override
	QueryImplementor<R> setFirstResult(int startPosition);

	@Override
	QueryImplementor<R> setHint(String hintName, Object value);

	@Override
	QueryImplementor<R> setFlushMode(FlushModeType flushMode);

	@Override
	QueryImplementor<R> setLockMode(LockModeType lockMode);

	@Override
	QueryImplementor<R> setTupleTransformer(TupleTransformer transformer);

	@Override
	QueryImplementor<R> setResultListTransformer(ResultListTransformer transformer);

	@Override
	QueryImplementor<R> setParameter(String name, Object value);

	@Override
	QueryImplementor<R> setParameter(int position, Object value);

	@Override
	<T> QueryImplementor<R> setParameter(Parameter<T> param, T value);

	@Override
	<T> QueryImplementor<R> setParameter(QueryParameter<T> parameter, T val);

	@Override
	QueryImplementor<R> setParameter(String name, Object val, AllowableParameterType type);

	@Override
	QueryImplementor<R> setParameter(int position, Object val, AllowableParameterType type);

	@Override
	<P> QueryImplementor<R> setParameter(QueryParameter<P> parameter, P val, AllowableParameterType type);

	@Override
	<P> QueryImplementor<R> setParameter(String name, P val, TemporalType temporalType);

	@Override
	<P> QueryImplementor<R> setParameter(int position, P val, TemporalType temporalType);

	@Override
	<P> QueryImplementor<R> setParameter(
			QueryParameter<P> parameter,
			P val,
			TemporalType temporalType);

	@Override
	QueryImplementor<R> setParameter(String name, Instant value, TemporalType temporalType);

	@Override
	QueryImplementor<R> setParameter(int position, Instant value, TemporalType temporalType);

	@Override
	QueryImplementor<R> setParameter(
			Parameter<Instant> param,
			Instant value,
			TemporalType temporalType);

	@Override
	QueryImplementor<R> setParameter(
			String name,
			LocalDateTime value,
			TemporalType temporalType);

	@Override
	QueryImplementor<R> setParameter(
			int position,
			LocalDateTime value,
			TemporalType temporalType);

	@Override
	QueryImplementor<R> setParameter(
			Parameter<LocalDateTime> param,
			LocalDateTime value,
			TemporalType temporalType);

	@Override
	QueryImplementor<R> setParameter(
			String name,
			ZonedDateTime value,
			TemporalType temporalType);

	@Override
	QueryImplementor<R> setParameter(
			int position,
			ZonedDateTime value,
			TemporalType temporalType);

	@Override
	QueryImplementor<R> setParameter(
			Parameter<ZonedDateTime> param,
			ZonedDateTime value,
			TemporalType temporalType);

	@Override
	QueryImplementor<R> setParameter(
			String name,
			OffsetDateTime value,
			TemporalType temporalType);

	@Override
	QueryImplementor<R> setParameter(
			int position,
			OffsetDateTime value,
			TemporalType temporalType);

	@Override
	QueryImplementor<R> setParameter(
			Parameter<OffsetDateTime> param,
			OffsetDateTime value,
			TemporalType temporalType);
}
