/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

import java.util.Calendar;
import java.util.Date;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.RowSelection;

/**
 * Represents an HQL/JPQL query or a compiled Criteria query.  Also acts as the Hibernate
 * extension to the JPA Query/TypedQuery contract
 * <p/>
 * NOTE: {@link org.hibernate.Query} is deprecated, and slated for removal in 6.0.
 * For the time being we leave all methods defined on {@link org.hibernate.Query}
 * rather than here because it was previously the public API so we want to leave that
 * unchanged in 5.x.  For 6.0 we will move those methods here and then delete that class.
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
@Incubating
@SuppressWarnings("UnusedDeclaration")
public interface Query<R> extends TypedQuery<R>, org.hibernate.Query<R>, BasicQueryContract {
	/**
	 * Get the QueryProducer this Query originates from.
	 */
	QueryProducer getProducer();

	/**
	 * "QueryOptions" is a better name, I think, than "RowSelection" -> 6.0
	 *
	 * @return Return the encapsulation of this query's options, which includes access to
	 * firstRow, maxRows, timeout and fetchSize.   Important because this gives access to
	 * those values in their Integer form rather than the primitive form (int) required by JPA.
	 */
	RowSelection getQueryOptions();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// covariant overrides


	@Override
	Query<R> setMaxResults(int maxResult);

	@Override
	Query<R> setFirstResult(int startPosition);

	@Override
	Query<R> setHint(String hintName, Object value);

	@Override
	<T> Query<R> setParameter(Parameter<T> param, T value);

	@Override
	Query<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override
	Query<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType);

	@Override
	Query<R> setParameter(String name, Object value);

	@Override
	Query<R> setParameter(String name, Calendar value, TemporalType temporalType);

	@Override
	Query<R> setParameter(String name, Date value, TemporalType temporalType);

	@Override
	Query<R> setParameter(int position, Object value);

	@Override
	Query<R> setParameter(int position, Calendar value, TemporalType temporalType);

	@Override
	Query<R> setParameter(int position, Date value, TemporalType temporalType);

	@Override
	Query<R> setFlushMode(FlushModeType flushMode);

	@Override
	Query<R> setLockMode(LockModeType lockMode);

	@Override
	Query<R> setReadOnly(boolean readOnly);

	@Override
	Query<R> setHibernateFlushMode(FlushMode flushMode);

	@Override
	default Query<R> setFlushMode(FlushMode flushMode) {
		setHibernateFlushMode( flushMode );
		return this;
	}

	@Override
	Query<R> setCacheMode(CacheMode cacheMode);

	@Override
	Query<R> setCacheable(boolean cacheable);

	@Override
	Query<R> setCacheRegion(String cacheRegion);

	@Override
	Query<R> setTimeout(int timeout);

	@Override
	Query<R> setFetchSize(int fetchSize);

	@Override
	Query<R> setLockOptions(LockOptions lockOptions);

	@Override
	Query<R> setLockMode(String alias, LockMode lockMode);

	@Override
	Query<R> setComment(String comment);

	@Override
	Query<R> addQueryHint(String hint);
}
