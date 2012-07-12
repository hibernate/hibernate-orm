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

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.ParameterExpression;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Query;
import org.hibernate.ejb.HibernateQuery;
import org.hibernate.jpa.internal.QueryImpl;

/**
 * @author Steve Ebersole
 */
public class CriteriaQueryTypeQueryAdapter<X> implements TypedQuery<X>, HibernateQuery {
	private final QueryImpl<X> jpaqlQuery;
	private final Map<ParameterExpression<?>, String> explicitParameterMapping;
	private final Map<String, ParameterExpression<?>> explicitParameterNameMapping;

	public CriteriaQueryTypeQueryAdapter(
			QueryImpl<X> jpaqlQuery,
			Map<ParameterExpression<?>, String> explicitParameterMapping,
			Map<String, ParameterExpression<?>> explicitParameterNameMapping) {
		this.jpaqlQuery = jpaqlQuery;
		this.explicitParameterMapping = explicitParameterMapping;
		this.explicitParameterNameMapping = explicitParameterNameMapping;
	}

	@Override
	public Query getHibernateQuery() {
		return jpaqlQuery.getHibernateQuery();
	}

	public List<X> getResultList() {
		return jpaqlQuery.getResultList();
	}

	public X getSingleResult() {
		return jpaqlQuery.getSingleResult();
	}

	public int getMaxResults() {
		return jpaqlQuery.getMaxResults();
	}

	public TypedQuery<X> setMaxResults(int i) {
		jpaqlQuery.setMaxResults( i );
		return this;
	}

	public int getFirstResult() {
		return jpaqlQuery.getFirstResult();
	}

	public TypedQuery<X> setFirstResult(int i) {
		jpaqlQuery.setFirstResult( i );
		return this;
	}

	public Map<String, Object> getHints() {
		return jpaqlQuery.getHints();
	}

	public TypedQuery<X> setHint(String name, Object value) {
		jpaqlQuery.setHint( name, value);
		return this;
	}

	public FlushModeType getFlushMode() {
		return jpaqlQuery.getFlushMode();
	}

	public TypedQuery<X> setFlushMode(FlushModeType flushModeType) {
		jpaqlQuery.setFlushMode( flushModeType );
		return this;
	}

	public LockModeType getLockMode() {
		return jpaqlQuery.getLockMode();
	}

	public TypedQuery<X> setLockMode(LockModeType lockModeType) {
		jpaqlQuery.setLockMode( lockModeType );
		return this;
	}

	@SuppressWarnings({ "unchecked" })
	public Set getParameters() {
		return explicitParameterMapping.keySet();
	}

	public boolean isBound(Parameter<?> param) {
		return jpaqlQuery.isBound( param );
	}

	@SuppressWarnings({ "unchecked" })
	public <T> T getParameterValue(Parameter<T> param) {
		return ( T ) jpaqlQuery.getParameterValue( mapToNamedParameter( param ) );
	}

	@SuppressWarnings({ "unchecked" })
	public <T> TypedQuery<X> setParameter(Parameter<T> param, T t) {
		jpaqlQuery.setParameter( mapToNamedParameter( param ), t );
		return this;
	}

	@SuppressWarnings({ "RedundantCast" })
	private Parameter mapToNamedParameter(Parameter criteriaParameter) {
		return jpaqlQuery.getParameter(
				explicitParameterMapping.get( criteriaParameter )
		);
	}

	@SuppressWarnings({ "unchecked" })
	public TypedQuery<X> setParameter(Parameter<Calendar> param, Calendar calendar, TemporalType temporalType) {
		jpaqlQuery.setParameter( mapToNamedParameter( param ), calendar, temporalType );
		return this;
	}

	@SuppressWarnings({ "unchecked" })
	public TypedQuery<X> setParameter(Parameter<Date> param, Date date, TemporalType temporalType) {
		jpaqlQuery.setParameter( mapToNamedParameter( param ), date, temporalType );
		return this;
	}

	public <T> T unwrap(Class<T> cls) {
		return jpaqlQuery.unwrap( cls );
	}

	@SuppressWarnings({ "unchecked" })
	public Object getParameterValue(String name) {
		return getParameterValue( resolveExplicitCriteriaParameterName( name ) );
	}

	private Parameter resolveExplicitCriteriaParameterName(String name) {
		Parameter parameter = explicitParameterNameMapping.get( name );
		if ( parameter == null ) {
			throw new IllegalArgumentException( "Named parameter [" + name + "] not encountered" );
		}
		return parameter;
	}

	public Parameter<?> getParameter(String name) {
		return mapToNamedParameter( resolveExplicitCriteriaParameterName( name ) );
	}

	@SuppressWarnings({ "unchecked" })
	public <T> Parameter<T> getParameter(String name, Class<T> type) {
		Parameter parameter = resolveExplicitCriteriaParameterName( name );
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
		setParameter(
				resolveExplicitCriteriaParameterName( name, value ),
				value
		);
		return this;
	}

	private Parameter resolveExplicitCriteriaParameterName(String name, Object value) {
		Parameter parameter = resolveExplicitCriteriaParameterName( name );
		// todo : is null valid?
		if ( value != null ) {
			if ( ! parameter.getParameterType().isInstance( value ) ) {
				throw new IllegalArgumentException(
						"Named parameter [" + name + "] type mismatch; expecting ["
								+ parameter.getParameterType().getName() + "], found ["
								+ value.getClass().getName() + "]"
				);
			}
		}
		return parameter;
	}

	@SuppressWarnings({ "unchecked" })
	public TypedQuery<X> setParameter(String name, Calendar calendar, TemporalType temporalType) {
		Parameter parameter = resolveExplicitCriteriaParameterName( name );
		if ( ! Calendar.class.isAssignableFrom( parameter.getParameterType() ) ) {
			throw new IllegalArgumentException(
					"Named parameter [" + name + "] type mismatch; expecting ["
							+ Calendar.class.getName() + "], found ["
							+ parameter.getParameterType().getName() + "]"
			);
		}
		setParameter( parameter, calendar, temporalType );
		return this;
	}

	@SuppressWarnings({ "unchecked" })
	public TypedQuery<X> setParameter(String name, Date date, TemporalType temporalType) {
		Parameter parameter = resolveExplicitCriteriaParameterName( name );
		if ( ! Date.class.isAssignableFrom( parameter.getParameterType() ) ) {
			throw new IllegalArgumentException(
					"Named parameter [" + name + "] type mismatch; expecting ["
							+ Date.class.getName() + "], found ["
							+ parameter.getParameterType().getName() + "]"
			);
		}
		setParameter( parameter, date, temporalType );
		return this;
	}


	// unsupported stuff ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public int executeUpdate() {
		throw new IllegalArgumentException( "Criteria queries do not support update queries" );
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
