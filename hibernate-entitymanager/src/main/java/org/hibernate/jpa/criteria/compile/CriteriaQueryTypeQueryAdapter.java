/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.criteria.compile;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.ParameterExpression;

import org.hibernate.Query;
import org.hibernate.ejb.HibernateQuery;
import org.hibernate.jpa.internal.QueryImpl;
import org.hibernate.jpa.spi.HibernateEntityManagerImplementor;

/**
 * @author Steve Ebersole
 */
public class CriteriaQueryTypeQueryAdapter<X> implements TypedQuery<X>, HibernateQuery {
	private final HibernateEntityManagerImplementor entityManager;
	private final QueryImpl<X> jpqlQuery;
	private final Map<ParameterExpression<?>, ExplicitParameterInfo<?>> explicitParameterInfoMap;

	public CriteriaQueryTypeQueryAdapter(
			HibernateEntityManagerImplementor entityManager,
			QueryImpl<X> jpqlQuery,
			Map<ParameterExpression<?>, ExplicitParameterInfo<?>> explicitParameterInfoMap) {
		this.entityManager = entityManager;
		this.jpqlQuery = jpqlQuery;
		this.explicitParameterInfoMap = explicitParameterInfoMap;
	}

	@Override
	public Query getHibernateQuery() {
		return jpqlQuery.getHibernateQuery();
	}

	public List<X> getResultList() {
		return jpqlQuery.getResultList();
	}

	public X getSingleResult() {
		return jpqlQuery.getSingleResult();
	}

	public int getMaxResults() {
		return jpqlQuery.getMaxResults();
	}

	public TypedQuery<X> setMaxResults(int i) {
		jpqlQuery.setMaxResults( i );
		return this;
	}

	public int getFirstResult() {
		return jpqlQuery.getFirstResult();
	}

	public TypedQuery<X> setFirstResult(int i) {
		jpqlQuery.setFirstResult( i );
		return this;
	}

	public Map<String, Object> getHints() {
		return jpqlQuery.getHints();
	}

	public TypedQuery<X> setHint(String name, Object value) {
		jpqlQuery.setHint( name, value );
		return this;
	}

	public FlushModeType getFlushMode() {
		return jpqlQuery.getFlushMode();
	}

	public TypedQuery<X> setFlushMode(FlushModeType flushModeType) {
		jpqlQuery.setFlushMode( flushModeType );
		return this;
	}

	public LockModeType getLockMode() {
		return jpqlQuery.getLockMode();
	}

	public TypedQuery<X> setLockMode(LockModeType lockModeType) {
		jpqlQuery.setLockMode( lockModeType );
		return this;
	}

	@SuppressWarnings({ "unchecked" })
	public Set<Parameter<?>> getParameters() {
		entityManager.checkOpen( false );
		return new HashSet( explicitParameterInfoMap.values() );
	}

	public boolean isBound(Parameter<?> param) {
		entityManager.checkOpen( false );
		return jpqlQuery.isBound( param );
	}

	@SuppressWarnings({ "unchecked" })
	public <T> T getParameterValue(Parameter<T> param) {
		entityManager.checkOpen( false );
		final ExplicitParameterInfo parameterInfo = resolveParameterInfo( param );
		if ( parameterInfo.isNamed() ) {
			return ( T ) jpqlQuery.getParameterValue( parameterInfo.getName() );
		}
		else {
			return ( T ) jpqlQuery.getParameterValue( parameterInfo.getPosition() );
		}
	}

	private <T> ExplicitParameterInfo resolveParameterInfo(Parameter<T> param) {
		if ( ExplicitParameterInfo.class.isInstance( param ) ) {
			return (ExplicitParameterInfo) param;
		}
		else if ( ParameterExpression.class.isInstance( param ) ) {
			return explicitParameterInfoMap.get( (ParameterExpression) param );
		}
		else {
			for ( ExplicitParameterInfo parameterInfo : explicitParameterInfoMap.values() ) {
				if ( param.getName() != null && param.getName().equals( parameterInfo.getName() ) ) {
					return parameterInfo;
				}
				else if ( param.getPosition() != null && param.getPosition().equals( parameterInfo.getPosition() ) ) {
					return parameterInfo;
				}
			}
		}
		throw new IllegalArgumentException( "Unable to locate parameter [" + param + "] in query" );
	}

	@SuppressWarnings({ "unchecked" })
	public <T> TypedQuery<X> setParameter(Parameter<T> param, T t) {
		entityManager.checkOpen( false );
		final ExplicitParameterInfo parameterInfo = resolveParameterInfo( param );
		if ( parameterInfo.isNamed() ) {
			jpqlQuery.setParameter( parameterInfo.getName(), t );
		}
		else {
			jpqlQuery.setParameter( parameterInfo.getPosition(), t );
		}
		return this;
	}

	@SuppressWarnings({ "unchecked" })
	public TypedQuery<X> setParameter(Parameter<Calendar> param, Calendar calendar, TemporalType temporalType) {
		entityManager.checkOpen( false );
		final ExplicitParameterInfo parameterInfo = resolveParameterInfo( param );
		if ( parameterInfo.isNamed() ) {
			jpqlQuery.setParameter( parameterInfo.getName(), calendar, temporalType );
		}
		else {
			jpqlQuery.setParameter( parameterInfo.getPosition(), calendar, temporalType );
		}
		return this;
	}

	@SuppressWarnings({ "unchecked" })
	public TypedQuery<X> setParameter(Parameter<Date> param, Date date, TemporalType temporalType) {
		entityManager.checkOpen( false );
		final ExplicitParameterInfo parameterInfo = resolveParameterInfo( param );
		if ( parameterInfo.isNamed() ) {
			jpqlQuery.setParameter( parameterInfo.getName(), date, temporalType );
		}
		else {
			jpqlQuery.setParameter( parameterInfo.getPosition(), date, temporalType );
		}
		return this;
	}

	public <T> T unwrap(Class<T> cls) {
		return jpqlQuery.unwrap( cls );
	}

	@SuppressWarnings({ "unchecked" })
	public Object getParameterValue(String name) {
		entityManager.checkOpen( false );
		locateParameterByName( name );
		return jpqlQuery.getParameter( name );
	}

	private ExplicitParameterInfo locateParameterByName(String name) {
		for ( ExplicitParameterInfo parameterInfo : explicitParameterInfoMap.values() ) {
			if ( parameterInfo.isNamed() && parameterInfo.getName().equals( name ) ) {
				return parameterInfo;
			}
		}
		throw new IllegalArgumentException( "Unable to locate parameter registered with that name [" + name + "]" );
	}

	public Parameter<?> getParameter(String name) {
		entityManager.checkOpen( false );
		return locateParameterByName( name );
	}

	@SuppressWarnings({ "unchecked" })
	public <T> Parameter<T> getParameter(String name, Class<T> type) {
		entityManager.checkOpen( false );
		Parameter parameter = locateParameterByName( name );
		if ( type.isAssignableFrom( parameter.getParameterType() ) ) {
			return parameter;
		}
		throw new IllegalArgumentException(
				"Named parameter [" + name + "] type is not assignanle to request type ["
						+ type.getName() + "]"
		);
	}

	@SuppressWarnings({ "unchecked" })
	public TypedQuery<X> setParameter(String name, Object value) {
		entityManager.checkOpen( true );
		ExplicitParameterInfo parameterInfo = locateParameterByName( name );
		parameterInfo.validateBindValue( value );
		jpqlQuery.setParameter( name, value );
		return this;
	}

	@SuppressWarnings({ "unchecked" })
	public TypedQuery<X> setParameter(String name, Calendar calendar, TemporalType temporalType) {
		entityManager.checkOpen( true );
		ExplicitParameterInfo parameterInfo = locateParameterByName( name );
		parameterInfo.validateCalendarBind();
		jpqlQuery.setParameter( name, calendar, temporalType );
		return this;
	}

	@SuppressWarnings({ "unchecked" })
	public TypedQuery<X> setParameter(String name, Date date, TemporalType temporalType) {
		entityManager.checkOpen( true );
		ExplicitParameterInfo parameterInfo = locateParameterByName( name );
		parameterInfo.validateDateBind();
		jpqlQuery.setParameter( name, date, temporalType );
		return this;
	}


	// unsupported stuff ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public int executeUpdate() {
		throw new IllegalStateException( "Typed criteria queries do not support executeUpdate" );
	}

	public TypedQuery<X> setParameter(int i, Object o) {
		throw new IllegalArgumentException( "Criteria queries do not support positioned parameters" );
	}

	public TypedQuery<X> setParameter(int i, Calendar calendar, TemporalType temporalType) {
		throw new IllegalArgumentException( "Criteria queries do not support positioned parameters" );
	}

	public TypedQuery<X> setParameter(int i, Date date, TemporalType temporalType) {
		throw new IllegalArgumentException( "Criteria queries do not support positioned parameters" );
	}

	public Object getParameterValue(int position) {
		throw new IllegalArgumentException( "Criteria queries do not support positioned parameters" );
	}

	public Parameter<?> getParameter(int position) {
		throw new IllegalArgumentException( "Criteria queries do not support positioned parameters" );
	}

	public <T> Parameter<T> getParameter(int position, Class<T> type) {
		throw new IllegalArgumentException( "Criteria queries do not support positioned parameters" );
	}
}
