/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.ejb.criteria;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import javax.persistence.TypedQuery;
import javax.persistence.Parameter;
import javax.persistence.TemporalType;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;

import org.hibernate.ejb.HibernateEntityManagerImplementor;

/**
 * Compiles a JPA criteria query into an executable {@link TypedQuery}.  Its single contract is the {@link #compile}
 * method.
 * <p/>
 * NOTE : This is a temporary implementation which simply translates the criteria query into a JPAQL query string.  A
 * better, long-term solution is being implemented as part of refactoring the JPAQL/HQL translator.
 *
 * @author Steve Ebersole
 */
public class CriteriaQueryCompiler {
	public static interface ImplicitParameterBinding {
		public void bind(TypedQuery typedQuery);
	}

	public static interface RenderingContext {
		public String generateAlias();
		public String generateParameterName();

		public void registerExplicitParameter(ParameterExpression<?> criteriaQueryParameter, String jpaqlParameterName);
		public void registerImplicitParameterBinding(ImplicitParameterBinding binding);
	}

	public static interface RenderedCriteriaQuery {
		public String getQueryString();
	}

	private final HibernateEntityManagerImplementor entityManager;

	public CriteriaQueryCompiler(HibernateEntityManagerImplementor entityManager) {
		this.entityManager = entityManager;
	}

	public <T> TypedQuery<T> compile(CriteriaQuery<T> criteriaQuery) {
		CriteriaQueryImpl<T> criteriaQueryImpl = ( CriteriaQueryImpl<T> ) criteriaQuery;
		criteriaQueryImpl.validate();

		final Map<ParameterExpression<?>,String> explicitParameterMapping = new HashMap<ParameterExpression<?>,String>();
		final List<ImplicitParameterBinding> implicitParameterBindings = new ArrayList<ImplicitParameterBinding>();
		RenderingContext renderingContext = new RenderingContext() {
			private int aliasCount = 0;
			private int explicitParameterCount = 0;

			public String generateAlias() {
				return "generatedAlias" + aliasCount++;
			}

			public String generateParameterName() {
				return "param" + explicitParameterCount++;
			}

			public void registerExplicitParameter(ParameterExpression<?> criteriaQueryParameter, String jpaqlParameterName) {
				explicitParameterMapping.put( criteriaQueryParameter, jpaqlParameterName );
			}

			public void registerImplicitParameterBinding(ImplicitParameterBinding binding) {
				implicitParameterBindings.add( binding );
			}
		};

		RenderedCriteriaQuery renderedCriteriaQuery = criteriaQueryImpl.render( renderingContext );

		TypedQuery<T> jpaqlQuery = entityManager.createQuery(
				renderedCriteriaQuery.getQueryString(),
				criteriaQuery.getResultType()
		);
		for ( ImplicitParameterBinding implicitParameterBinding : implicitParameterBindings ) {
			implicitParameterBinding.bind( jpaqlQuery );
		}

		return wrap( jpaqlQuery, explicitParameterMapping );
	}

	private <X> TypedQuery<X> wrap(
			final TypedQuery<X> jpaqlQuery,
			final Map<ParameterExpression<?>, String> explicitParameterMapping) {
		return new TypedQuery<X>() {

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
				return jpaqlQuery.setMaxResults( i );
			}

			public int getFirstResult() {
				return jpaqlQuery.getFirstResult();
			}

			public TypedQuery<X> setFirstResult(int i) {
				return jpaqlQuery.setFirstResult( i );
			}

			public Map<String, Object> getHints() {
				return jpaqlQuery.getHints();
			}

			public TypedQuery<X> setHint(String name, Object value) {
				return jpaqlQuery.setHint( name, value);
			}

			public FlushModeType getFlushMode() {
				return jpaqlQuery.getFlushMode();
			}

			public TypedQuery<X> setFlushMode(FlushModeType flushModeType) {
				return jpaqlQuery.setFlushMode( flushModeType );
			}

			public LockModeType getLockMode() {
				return jpaqlQuery.getLockMode();
			}

			public TypedQuery<X> setLockMode(LockModeType lockModeType) {
				return jpaqlQuery.setLockMode( lockModeType );
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
				return jpaqlQuery.setParameter( mapToNamedParameter( param ), t );
			}

			@SuppressWarnings({ "RedundantCast" })
			private Parameter mapToNamedParameter(Parameter criteriaParameter) {
				return jpaqlQuery.getParameter(
						explicitParameterMapping.get( (ParameterExpression) criteriaParameter )
				);
			}

			@SuppressWarnings({ "unchecked" })
			public TypedQuery<X> setParameter(Parameter<Calendar> param, Calendar calendar, TemporalType temporalType) {
				return jpaqlQuery.setParameter( mapToNamedParameter( param ), calendar, temporalType );
			}

			@SuppressWarnings({ "unchecked" })
			public TypedQuery<X> setParameter(Parameter<Date> param, Date date, TemporalType temporalType) {
				return jpaqlQuery.setParameter( mapToNamedParameter( param ), date, temporalType );
			}

			public <T> T unwrap(Class<T> cls) {
				return jpaqlQuery.unwrap( cls );
			}


			// unsupported stuff ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

			public int executeUpdate() {
				throw new IllegalArgumentException( "Criteria queries do not support update queries" );
			}

			public TypedQuery<X> setParameter(String s, Object o) {
				throw new IllegalArgumentException( "Criteria queries do not support named parameters" );
			}

			public TypedQuery<X> setParameter(String s, Calendar calendar, TemporalType temporalType) {
				throw new IllegalArgumentException( "Criteria queries do not support named parameters" );
			}

			public TypedQuery<X> setParameter(String s, Date date, TemporalType temporalType) {
				throw new IllegalArgumentException( "Criteria queries do not support named parameters" );
			}

			public Object getParameterValue(String name) {
				throw new IllegalArgumentException( "Criteria queries do not support named parameters" );
			}

			public Parameter<?> getParameter(String name) {
				throw new IllegalArgumentException( "Criteria queries do not support named parameters" );
			}

			public <T> Parameter<T> getParameter(String name, Class<T> type) {
				throw new IllegalArgumentException( "Criteria queries do not support named parameters" );
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
		};
	}
}
