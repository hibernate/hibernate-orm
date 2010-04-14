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

import java.io.Serializable;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.ejb.HibernateEntityManagerImplementor;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;
import org.hibernate.util.StringHelper;

/**
 * Compiles a JPA criteria query into an executable {@link TypedQuery}.  Its single contract is the {@link #compile}
 * method.
 * <p/>
 * NOTE : This is a temporary implementation which simply translates the criteria query into a JPAQL query string.  A
 * better, long-term solution is being implemented as part of refactoring the JPAQL/HQL translator.
 *
 * @author Steve Ebersole
 */
public class CriteriaQueryCompiler implements Serializable {
	private static final Logger log = LoggerFactory.getLogger( CriteriaQueryCompiler.class );

	/**
	 * Used to describe implicit (not defined in criteria query) parameters.
	 */
	public static interface ImplicitParameterBinding {
		/**
		 * Retrieve the generated name of the implicit parameter.
		 *
		 * @return The parameter name.
		 */
		public String getParameterName();

		/**
		 * Get the java type of the "thing" that led to the implicit parameter.  Used from
		 * {@link org.hibernate.ejb.HibernateEntityManagerImplementor.Options#getNamedParameterExplicitTypes()}
		 * in determining "guessed type" overriding.
		 *
		 * @return The java type
		 */
		public Class getJavaType();

		/**
		 * Bind the implicit parameter's value to the JPA query.
		 *
		 * @param typedQuery The JPA query.
		 */
		public void bind(TypedQuery typedQuery);
	}

	/**
	 * Used to provide a context and services to the rendering.
	 */
	public static interface RenderingContext {
		/**
		 * Generate a correlation name.
		 *
		 * @return The generated correlation name
		 */
		public String generateAlias();

		/**
		 * Generate a name for a parameter into the JPAQL query.
		 *
		 * @return The generated para name
		 */
		public String generateParameterName();

		/**
		 * Register parameters explicitly encountered in the criteria query.
		 *
		 * @param criteriaQueryParameter The parameter expression
		 * @param jpaqlParameterName The generated name for the parameter
		 */
		public void registerExplicitParameter(ParameterExpression<?> criteriaQueryParameter, String jpaqlParameterName);

		/**
		 * Register a parameter that was not part of the criteria query (at least not as a parameter).
		 *
		 * @param binding The parameter description.
		 */
		public void registerImplicitParameterBinding(ImplicitParameterBinding binding);

		/**
		 * Given a java type, determine the proper cast type name.
		 *
		 * @param javaType The java type.
		 *
		 * @return The cast type name.
		 */
		public String getCastType(Class javaType);
	}

	public static interface RenderedCriteriaQuery {
		public String getQueryString();
		public List<ValueHandlerFactory.ValueHandler> getValueHandlers();
		public HibernateEntityManagerImplementor.Options.ResultMetadataValidator getResultMetadataValidator();
	}

	private final HibernateEntityManagerImplementor entityManager;

	public CriteriaQueryCompiler(HibernateEntityManagerImplementor entityManager) {
		this.entityManager = entityManager;
	}

	public <T> TypedQuery<T> compile(CriteriaQuery<T> criteriaQuery) {
		CriteriaQueryImpl<T> criteriaQueryImpl = ( CriteriaQueryImpl<T> ) criteriaQuery;
		criteriaQueryImpl.validate();

		final Map<ParameterExpression<?>,String> explicitParameterMapping = new HashMap<ParameterExpression<?>,String>();
		final Map<String,ParameterExpression<?>> explicitParameterNameMapping = new HashMap<String,ParameterExpression<?>>();
		final List<ImplicitParameterBinding> implicitParameterBindings = new ArrayList<ImplicitParameterBinding>();
		final Map<String,Class> implicitParameterTypes = new HashMap<String, Class>();

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
				if ( StringHelper.isNotEmpty( criteriaQueryParameter.getName() ) ) {
					explicitParameterNameMapping.put(
							criteriaQueryParameter.getName(),
							criteriaQueryParameter
					);
				}
			}

			public void registerImplicitParameterBinding(ImplicitParameterBinding binding) {
				implicitParameterBindings.add( binding );
				implicitParameterTypes.put(
						binding.getParameterName(),
						binding.getJavaType()
				);
			}

			public String getCastType(Class javaType) {
				SessionFactoryImplementor factory =
						( SessionFactoryImplementor ) entityManager.getFactory().getSessionFactory();
				Type hibernateType = TypeFactory.heuristicType( javaType.getName() );
				if ( hibernateType == null ) {
					throw new IllegalArgumentException(
							"Could not convert java type [" + javaType.getName() + "] to Hibernate type"
					);
				}
				int[] sqlTypeCodes = hibernateType.sqlTypes( factory );
				if ( sqlTypeCodes.length != 1 ) {
					throw new IllegalArgumentException(
							"Invalid Hibernate Type [" + hibernateType.getName() +
									"] for cast : more than one column spanned"
					);
				}
				return factory.getDialect().getCastTypeName( sqlTypeCodes[0] );
			}
		};

		final RenderedCriteriaQuery renderedCriteriaQuery = criteriaQueryImpl.render( renderingContext );

		log.debug( "Rendered criteria query -> {}", renderedCriteriaQuery.getQueryString() );

		TypedQuery<T> jpaqlQuery = entityManager.createQuery(
				renderedCriteriaQuery.getQueryString(),
				criteriaQuery.getResultType(),
				criteriaQuery.getSelection(),
				new HibernateEntityManagerImplementor.Options() {
					public List<ValueHandlerFactory.ValueHandler> getValueHandlers() {
						return renderedCriteriaQuery.getValueHandlers();
					}

					public Map<String, Class> getNamedParameterExplicitTypes() {
						return implicitParameterTypes;
					}

					public ResultMetadataValidator getResultMetadataValidator() {
						return renderedCriteriaQuery.getResultMetadataValidator();
					}
				}
		);

		for ( ImplicitParameterBinding implicitParameterBinding : implicitParameterBindings ) {
			implicitParameterBinding.bind( jpaqlQuery );
		}

		return wrap( jpaqlQuery, explicitParameterMapping, explicitParameterNameMapping );
	}

	private <X> TypedQuery<X> wrap(
			final TypedQuery<X> jpaqlQuery,
			final Map<ParameterExpression<?>, String> explicitParameterMapping,
			final Map<String, ParameterExpression<?>> explicitParameterNameMapping) {
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
						explicitParameterMapping.get( (ParameterExpression) criteriaParameter )
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
					return (Parameter<T>) parameter;
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
		};
	}
}
