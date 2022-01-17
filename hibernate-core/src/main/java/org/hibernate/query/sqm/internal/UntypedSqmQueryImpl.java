/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.BindableType;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.UntypedQuery;
import org.hibernate.query.spi.AbstractSqmQuery;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.spi.SqmQuery;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;

import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("rawtypes")
public class UntypedSqmQueryImpl extends AbstractSqmQuery implements UntypedQuery, SqmQuery {

	private final Class resultType;

	public UntypedSqmQueryImpl(
			String hql,
			HqlInterpretation hqlInterpretation,
			SharedSessionContractImplementor session) {
		super( hql, hqlInterpretation, session );

		final SqmSelectStatement sqm = (SqmSelectStatement) hqlInterpretation.getSqmStatement();

		this.resultType = sqm.getQuerySpec().getSelectClause().getSelections().size() > 1
				? Object[].class
				: Object.class;
	}


	protected SessionFactoryImplementor getSessionFactory() {
		return getSession().getSessionFactory();
	}

	@Override
	public SqmSelectStatement getSqmStatement() {
		return (SqmSelectStatement) super.getSqmStatement();
	}

	@Override
	public LockOptions getLockOptions() {
		return getQueryOptions().getLockOptions();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// InterpretationsKeySource

	@Override
	public Class<?> getResultType() {
		return resultType;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// execution

	@Override
	public List list() {
		return super.list();
	}

	@Override
	public ScrollableResultsImplementor scroll() {
		return super.scroll();
	}

	@Override
	public ScrollableResultsImplementor scroll(ScrollMode scrollMode) {
		return super.scroll( scrollMode );
	}

	@Override
	public Object uniqueResult() {
		return super.uniqueResult();
	}

	@Override
	public Object getSingleResult() {
		return super.getSingleResult();
	}

	@Override
	public Optional uniqueResultOptional() {
		return super.uniqueResultOptional();
	}

	@Override
	public Stream stream() {
		return super.stream();
	}

	@Override
	public int executeUpdate() {
		throw new IllegalStateException( SqmUtil.expectingNonSelect( getSqmStatement(), getQueryString() ) );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// overrides + covariant returns

	@Override
	public FlushModeType getFlushMode() {
		return getJpaFlushMode();
	}

	@Override
	public UntypedQuery setFlushMode(FlushModeType flushModeType) {
		applyJpaFlushMode( flushModeType );
		return this;
	}

	@Override
	public LockModeType getLockMode() {
		return getJpaLockMode();
	}

	@Override
	public UntypedQuery setLockMode(LockModeType lockModeType) {
		applyJpaLockMode( lockModeType );
		return this;
	}

	@Override
	public UntypedSqmQueryImpl setHibernateFlushMode(FlushMode flushMode) {
		super.setHibernateFlushMode( flushMode );
		return this;
	}

	@Override
	public UntypedSqmQueryImpl setCacheMode(CacheMode cacheMode) {
		super.setCacheMode( cacheMode );
		return this;
	}

	@Override
	public UntypedSqmQueryImpl setCacheable(boolean cacheable) {
		super.setCacheable( cacheable );
		return this;
	}

	@Override
	public UntypedSqmQueryImpl setCacheRegion(String cacheRegion) {
		super.setCacheRegion( cacheRegion );
		return this;
	}

	@Override
	public UntypedSqmQueryImpl setTimeout(int timeout) {
		super.setTimeout( timeout );
		return this;
	}

	@Override
	public UntypedSqmQueryImpl setFetchSize(int fetchSize) {
		super.setFetchSize( fetchSize );
		return this;
	}

	@Override
	public UntypedSqmQueryImpl setReadOnly(boolean readOnly) {
		super.setReadOnly( readOnly );
		return this;
	}

	@Override
	public UntypedSqmQueryImpl setMaxResults(int maxResult) {
		applyMaxResults( maxResult );
		return this;
	}

	@Override
	public UntypedQuery setFirstResult(int startPosition) {
		applyFirstResult( startPosition );
		return this;
	}

	@Override
	public UntypedQuery setHint(String hintName, Object value) {
		applyHint( hintName, value );
		return this;
	}

	@Override
	public UntypedSqmQueryImpl setParameter(String name, Object value) {
		super.setParameter( name, value );
		return this;
	}

	@Override
	public <P> UntypedSqmQueryImpl setParameter(String name, P value, Class<P> type) {
		super.setParameter( name, value, type );
		return this;
	}

	@Override
	public <P> UntypedSqmQueryImpl setParameter(String name, P value, BindableType<P> type) {
		super.setParameter( name, value, type );
		return this;
	}

	@Override
	public UntypedSqmQueryImpl setParameter(String name, Instant value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public UntypedSqmQueryImpl setParameter(String name, Calendar value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public UntypedSqmQueryImpl setParameter(String name, Date value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public UntypedSqmQueryImpl setParameter(int position, Object value) {
		super.setParameter( position, value );
		return this;
	}

	@Override
	public <P> UntypedSqmQueryImpl setParameter(int position, P value, Class<P> type) {
		super.setParameter( position, value, type );
		return this;
	}

	@Override
	public <P> UntypedSqmQueryImpl setParameter(int position, P value, BindableType<P> type) {
		super.setParameter( position, value, type );
		return this;
	}

	@Override
	public UntypedSqmQueryImpl setParameter(int position, Instant value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public UntypedSqmQueryImpl setParameter(int position, Date value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public UntypedSqmQueryImpl setParameter(int position, Calendar value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public <T> UntypedSqmQueryImpl setParameter(QueryParameter<T> parameter, T value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	public <P> UntypedSqmQueryImpl setParameter(QueryParameter<P> parameter, P value, Class<P> type) {
		super.setParameter( parameter, value, type );
		return this;
	}

	@Override
	public <P> UntypedSqmQueryImpl setParameter(QueryParameter<P> parameter, P val, BindableType<P> type) {
		super.setParameter( parameter, val, type );
		return this;
	}

	@Override
	public <T> UntypedSqmQueryImpl setParameter(Parameter<T> param, T value) {
		super.setParameter( param, value );
		return this;
	}

	@Override
	public UntypedSqmQueryImpl setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public UntypedSqmQueryImpl setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public UntypedSqmQueryImpl setParameterList(String name, Collection values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	public <P> UntypedSqmQueryImpl setParameterList(String name, Collection<? extends P> values, Class<P> javaType) {
		super.setParameterList( name, values, javaType );
		return this;
	}

	@Override
	public <P> UntypedSqmQueryImpl setParameterList(String name, Collection<? extends P> values, BindableType<P> type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	public UntypedSqmQueryImpl setParameterList(String name, Object[] values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	public <P> UntypedSqmQueryImpl setParameterList(String name, P[] values, Class<P> javaType) {
		super.setParameterList( name, values, javaType );
		return this;
	}

	@Override
	public <P> UntypedSqmQueryImpl setParameterList(String name, P[] values, BindableType<P> type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	public UntypedSqmQueryImpl setParameterList(int position, Collection values) {
		super.setParameterList( position, values );
		return this;
	}

	@Override
	public <P> UntypedSqmQueryImpl setParameterList(int position, Collection<? extends P> values, Class<P> javaType) {
		super.setParameterList( position, values, javaType );
		return this;
	}

	@Override
	public <P> UntypedSqmQueryImpl setParameterList(int position, Collection<? extends P> values, BindableType<P> type) {
		super.setParameterList( position, values, type );
		return this;
	}

	@Override
	public UntypedSqmQueryImpl setParameterList(int position, Object[] values) {
		super.setParameterList( position, values );
		return this;
	}

	@Override
	public <P> UntypedSqmQueryImpl setParameterList(int position, P[] values, Class<P> javaType) {
		super.setParameterList( position, values, javaType );
		return this;
	}

	@Override
	public <P> UntypedSqmQueryImpl setParameterList(int position, P[] values, BindableType<P> type) {
		super.setParameterList( position, values, type );
		return this;
	}

	@Override
	public <P> UntypedSqmQueryImpl setParameterList(QueryParameter<P> parameter, Collection<? extends P> values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	public <P> UntypedSqmQueryImpl setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType) {
		super.setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	public <P> UntypedSqmQueryImpl setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, BindableType<P> type) {
		super.setParameterList( parameter, values, type );
		return this;
	}

	@Override
	public <P> UntypedSqmQueryImpl setParameterList(QueryParameter<P> parameter, P[] values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	public <P> UntypedSqmQueryImpl setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType) {
		super.setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	public <P> UntypedSqmQueryImpl setParameterList(QueryParameter<P> parameter, P[] values, BindableType<P> type) {
		super.setParameterList( parameter, values, type );
		return this;
	}

	@Override
	public UntypedSqmQueryImpl setProperties(Object bean) {
		super.setProperties( bean );
		return this;
	}

	@Override
	public UntypedSqmQueryImpl setProperties(Map bean) {
		super.setProperties( bean );
		return this;
	}
}
