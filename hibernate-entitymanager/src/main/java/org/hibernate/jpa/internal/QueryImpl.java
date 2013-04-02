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
package org.hibernate.jpa.internal;

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Parameter;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.TransactionRequiredException;
import javax.persistence.TypedQuery;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.QueryParameterException;
import org.hibernate.TypeMismatchException;
import org.hibernate.engine.query.spi.NamedParameterDescriptor;
import org.hibernate.engine.query.spi.OrdinalParameterDescriptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.QueryExecutionRequestException;
import org.hibernate.internal.SQLQueryImpl;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.HibernateQuery;
import org.hibernate.jpa.internal.util.ConfigurationHelper;
import org.hibernate.jpa.internal.util.LockModeTypeHelper;
import org.hibernate.jpa.spi.AbstractEntityManagerImpl;
import org.hibernate.jpa.spi.AbstractQueryImpl;

import static javax.persistence.TemporalType.DATE;
import static javax.persistence.TemporalType.TIME;
import static javax.persistence.TemporalType.TIMESTAMP;

/**
 * Hibernate implementation of both the {@link Query} and {@link TypedQuery} contracts.
 *
 * @author <a href="mailto:gavin@hibernate.org">Gavin King</a>
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class QueryImpl<X> extends AbstractQueryImpl<X> implements TypedQuery<X>, HibernateQuery, org.hibernate.ejb.HibernateQuery {

    public static final EntityManagerMessageLogger LOG = Logger.getMessageLogger(EntityManagerMessageLogger.class, QueryImpl.class.getName());

	private org.hibernate.Query query;
	private Set<Integer> jpaPositionalIndices;
	private Set<Parameter<?>> parameters;

	public QueryImpl(org.hibernate.Query query, AbstractEntityManagerImpl em) {
		this( query, em, Collections.<String, Class>emptyMap() );
	}

	public QueryImpl(
			org.hibernate.Query query,
			AbstractEntityManagerImpl em,
			Map<String,Class> namedParameterTypeRedefinitions) {
		super( em );
		this.query = query;
		extractParameterInfo( namedParameterTypeRedefinitions );
	}

	@SuppressWarnings({ "unchecked", "RedundantCast" })
	private void extractParameterInfo(Map<String,Class> namedParameterTypeRedefinition) {
		if ( ! org.hibernate.internal.AbstractQueryImpl.class.isInstance( query ) ) {
			throw new IllegalStateException( "Unknown query type for parameter extraction" );
		}

		HashSet<Parameter<?>> parameters = new HashSet<Parameter<?>>();
		org.hibernate.internal.AbstractQueryImpl queryImpl = org.hibernate.internal.AbstractQueryImpl.class.cast( query );

		// extract named params
		for ( String name : (Set<String>) queryImpl.getParameterMetadata().getNamedParameterNames() ) {
			final NamedParameterDescriptor descriptor =
					queryImpl.getParameterMetadata().getNamedParameterDescriptor( name );
			Class javaType = namedParameterTypeRedefinition.get( name );
			if ( javaType != null && mightNeedRedefinition( javaType ) ) {
				descriptor.resetExpectedType(
						sfi().getTypeResolver().heuristicType( javaType.getName() )
				);
			}
			else if ( descriptor.getExpectedType() != null ) {
				javaType = descriptor.getExpectedType().getReturnedClass();
			}
			final ParameterImpl parameter = new ParameterImpl( name, javaType );
			parameters.add( parameter );
			if ( descriptor.isJpaStyle() ) {
				if ( jpaPositionalIndices == null ) {
					jpaPositionalIndices = new HashSet<Integer>();
				}
				jpaPositionalIndices.add( Integer.valueOf( name ) );
			}
		}

		// extract positional parameters
		for ( int i = 0, max = queryImpl.getParameterMetadata().getOrdinalParameterCount(); i < max; i++ ) {
			final OrdinalParameterDescriptor descriptor =
					queryImpl.getParameterMetadata().getOrdinalParameterDescriptor( i+1 );
			ParameterImpl parameter = new ParameterImpl(
					i + 1,
					descriptor.getExpectedType() == null
							? null
							: descriptor.getExpectedType().getReturnedClass()
			);
			parameters.add( parameter );
			Integer position = descriptor.getOrdinalPosition();
            if (jpaPositionalIndices != null && jpaPositionalIndices.contains(position)) LOG.parameterPositionOccurredAsBothJpaAndHibernatePositionalParameter(position);
		}

		this.parameters = java.util.Collections.unmodifiableSet( parameters );
	}

	private SessionFactoryImplementor sfi() {
		return (SessionFactoryImplementor) getEntityManager().getFactory().getSessionFactory();
	}

	private boolean mightNeedRedefinition(Class javaType) {
		// for now, only really no for dates/times/timestamps
		return java.util.Date.class.isAssignableFrom( javaType );
	}

	private static class ParameterImpl implements ParameterImplementor {
		private final String name;
		private final Integer position;
		private final Class javaType;

		private ParameterImpl(String name, Class javaType) {
			this.name = name;
			this.javaType = javaType;
			this.position = null;
		}

		private ParameterImpl(Integer position, Class javaType) {
			this.position = position;
			this.javaType = javaType;
			this.name = null;
		}

		public String getName() {
			return name;
		}

		public Integer getPosition() {
			return position;
		}

		public Class getParameterType() {
			return javaType;
		}

		@Override
		public void validateBinding(Object bind, TemporalType temporalType) {
			if ( bind == null || getParameterType() == null ) {
				// nothing we can check
				return;
			}

			if ( Collection.class.isInstance( bind ) && ! Collection.class.isAssignableFrom( getParameterType() ) ) {
				// we have a collection passed in where we are expecting a non-collection.
				// 		NOTE : this can happen in Hibernate's notion of "parameter list" binding
				// 		NOTE2 : the case of a collection value and an expected collection (if that can even happen)
				//			will fall through to the main check.
				validateCollectionValuedParameterBinding( (Collection) bind, temporalType );
			}
			else if ( bind.getClass().isArray() ) {
				validateArrayValuedParameterBinding( bind, temporalType );
			}
			else {
				if ( ! isValidBindValue( getParameterType(), bind, temporalType ) ) {
					throw new IllegalArgumentException(
							String.format(
									"Parameter value [%s] did not match expected type [%s (%s)]",
									bind,
									getParameterType().getName(),
									extractName( temporalType )
							)
					);
				}
			}
		}

		private String extractName(TemporalType temporalType) {
			return temporalType == null ? "n/a" : temporalType.name();
		}

		private void validateCollectionValuedParameterBinding(Collection value, TemporalType temporalType) {
			// validate the elements...
			for ( Object element : value ) {
				if ( ! isValidBindValue( getParameterType(), element, temporalType ) ) {
					throw new IllegalArgumentException(
							String.format(
									"Parameter value element [%s] did not match expected type [%s (%s)]",
									element,
									getParameterType().getName(),
									extractName( temporalType )
							)
					);
				}
			}
		}

		private void validateArrayValuedParameterBinding(Object value, TemporalType temporalType) {
			if ( ! getParameterType().isArray() ) {
				throw new IllegalArgumentException(
						String.format(
								"Encountered array-valued parameter binding, but was expecting [%s (%s)]",
								getParameterType().getName(),
								extractName( temporalType )
						)
				);
			}

			if ( value.getClass().getComponentType().isPrimitive() ) {
				// we have a primitive array.  we validate that the actual array has the component type (type of elements)
				// we expect based on the component type of the parameter specification
				if ( ! getParameterType().getComponentType().isAssignableFrom( value.getClass().getComponentType() ) ) {
					throw new IllegalArgumentException(
							String.format(
									"Primitive array-valued parameter bind value type [%s] did not match expected type [%s (%s)]",
									value.getClass().getComponentType().getName(),
									getParameterType().getName(),
									extractName( temporalType )
							)
					);
				}
			}
			else {
				// we have an object array.  Here we loop over the array and physically check each element against
				// the type we expect based on the component type of the parameter specification
				final Object[] array = (Object[]) value;
				for ( Object element : array ) {
					if ( ! isValidBindValue( getParameterType().getComponentType(), element, temporalType ) ) {
						throw new IllegalArgumentException(
								String.format(
										"Array-valued parameter value element [%s] did not match expected type [%s (%s)]",
										element,
										getParameterType().getName(),
										extractName( temporalType )
								)
						);
					}
				}
			}
		}
	}


	private static boolean isValidBindValue(Class expectedType, Object value, TemporalType temporalType) {
		if ( expectedType.isInstance( value ) ) {
			return true;
		}

		if ( temporalType != null ) {
			final boolean parameterDeclarationIsTemporal = Date.class.isAssignableFrom( expectedType )
					|| Calendar.class.isAssignableFrom( expectedType );
			final boolean bindIsTemporal = Date.class.isInstance( value )
					|| Calendar.class.isInstance( value );

			if ( parameterDeclarationIsTemporal && bindIsTemporal ) {
				return true;
			}
		}

		return false;
	}

	public org.hibernate.Query getHibernateQuery() {
		return query;
	}

	@Override
    protected int internalExecuteUpdate() {
		return query.executeUpdate();
	}

	@Override
    protected void applyMaxResults(int maxResults) {
		query.setMaxResults( maxResults );
	}

	@Override
    protected void applyFirstResult(int firstResult) {
		query.setFirstResult( firstResult );
	}

	@Override
    protected void applyTimeout(int timeout) {
		query.setTimeout( timeout );
	}

	@Override
    protected void applyComment(String comment) {
		query.setComment( comment );
	}

	@Override
    protected void applyFetchSize(int fetchSize) {
		query.setFetchSize( fetchSize );
	}

	@Override
    protected void applyCacheable(boolean isCacheable) {
		query.setCacheable( isCacheable );
	}

	@Override
    protected void applyCacheRegion(String regionName) {
		query.setCacheRegion( regionName );
	}

	@Override
    protected void applyReadOnly(boolean isReadOnly) {
		query.setReadOnly( isReadOnly );
	}

	@Override
    protected void applyCacheMode(CacheMode cacheMode) {
		query.setCacheMode( cacheMode );
	}

	@Override
    protected void applyFlushMode(FlushMode flushMode) {
		query.setFlushMode( flushMode );
	}

	@Override
    protected boolean canApplyLockModes() {
		return org.hibernate.internal.QueryImpl.class.isInstance( query )
				|| SQLQueryImpl.class.isInstance( query );
	}

	@Override
	protected void applyAliasSpecificLockMode(String alias, LockMode lockMode) {
		query.getLockOptions().setAliasSpecificLockMode( alias, lockMode );
	}

	@Override
	@SuppressWarnings({ "unchecked", "RedundantCast" })
	public List<X> getResultList() {
		getEntityManager().checkOpen( true );
		try {
			return query.list();
		}
		catch (QueryExecutionRequestException he) {
			throw new IllegalStateException(he);
		}
		catch( TypeMismatchException e ) {
			throw new IllegalArgumentException(e);
		}
		catch (HibernateException he) {
			throw getEntityManager().convert( he );
		}
	}

	@Override
	@SuppressWarnings({ "unchecked", "RedundantCast" })
	public X getSingleResult() {
		getEntityManager().checkOpen( true );
		try {
			final List<X> result = query.list();

			if ( result.size() == 0 ) {
				NoResultException nre = new NoResultException( "No entity found for query" );
				getEntityManager().handlePersistenceException( nre );
				throw nre;
			}
			else if ( result.size() > 1 ) {
				final Set<X> uniqueResult = new HashSet<X>(result);
				if ( uniqueResult.size() > 1 ) {
					NonUniqueResultException nure = new NonUniqueResultException( "result returns more than one elements" );
					getEntityManager().handlePersistenceException( nure );
					throw nure;
				}
				else {
					return uniqueResult.iterator().next();
				}
			}
			else {
				return result.get( 0 );
			}
		}
		catch (QueryExecutionRequestException he) {
			throw new IllegalStateException(he);
		}
		catch( TypeMismatchException e ) {
			throw new IllegalArgumentException(e);
		}
		catch (HibernateException he) {
			throw getEntityManager().convert( he );
		}
	}

	@Override
	public <T> TypedQuery<X> setParameter(Parameter<T> param, T value) {
		getEntityManager().checkOpen( true );
		if ( ! parameters.contains( param ) ) {
			throw new IllegalArgumentException( "Specified parameter was not found in query" );
		}
		if ( param.getName() != null ) {
			// a named param, for not delegate out.  Eventually delegate *into* this method...
			setParameter( param.getName(), value );
		}
		else {
			setParameter( param.getPosition(), value );
		}
		return this;
	}

	@Override
	public TypedQuery<X> setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		getEntityManager().checkOpen( true );
		if ( ! parameters.contains( param ) ) {
			throw new IllegalArgumentException( "Specified parameter was not found in query" );
		}
		if ( param.getName() != null ) {
			// a named param, for not delegate out.  Eventually delegate *into* this method...
			setParameter( param.getName(), value, temporalType );
		}
		else {
			setParameter( param.getPosition(), value, temporalType );
		}
		return this;
	}

	@Override
	public TypedQuery<X> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		getEntityManager().checkOpen( true );
		if ( ! parameters.contains( param ) ) {
			throw new IllegalArgumentException( "Specified parameter was not found in query" );
		}
		if ( param.getName() != null ) {
			// a named param, for not delegate out.  Eventually delegate *into* this method...
			setParameter( param.getName(), value, temporalType );
		}
		else {
			setParameter( param.getPosition(), value, temporalType );
		}
		return this;
	}

	@Override
	public TypedQuery<X> setParameter(String name, Object value) {
		getEntityManager().checkOpen( true );
		try {
			if ( value instanceof Collection ) {
				query.setParameterList( name, (Collection) value );
			}
			else {
				query.setParameter( name, value );
			}
			registerParameterBinding( getParameter( name ), value, null );
			return this;
		}
		catch (QueryParameterException e) {
			throw new IllegalArgumentException( e );
		}
		catch (HibernateException he) {
			throw getEntityManager().convert( he );
		}
	}

	@Override
	public TypedQuery<X> setParameter(String name, Date value, TemporalType temporalType) {
		getEntityManager().checkOpen( true );
		try {
			if ( temporalType == DATE ) {
				query.setDate( name, value );
			}
			else if ( temporalType == TIME ) {
				query.setTime( name, value );
			}
			else if ( temporalType == TIMESTAMP ) {
				query.setTimestamp( name, value );
			}
			registerParameterBinding( getParameter( name ), value, temporalType );
			return this;
		}
		catch (QueryParameterException e) {
			throw new IllegalArgumentException( e );
		}
		catch (HibernateException he) {
			throw getEntityManager().convert( he );
		}
	}

	@Override
	public TypedQuery<X> setParameter(String name, Calendar value, TemporalType temporalType) {
		getEntityManager().checkOpen( true );
		try {
			if ( temporalType == DATE ) {
				query.setCalendarDate( name, value );
			}
			else if ( temporalType == TIME ) {
				throw new IllegalArgumentException( "not yet implemented" );
			}
			else if ( temporalType == TIMESTAMP ) {
				query.setCalendar( name, value );
			}
			registerParameterBinding( getParameter(name), value, temporalType );
			return this;
		}
		catch (QueryParameterException e) {
			throw new IllegalArgumentException( e );
		}
		catch (HibernateException he) {
			throw getEntityManager().convert( he );
		}
	}

	@Override
	public TypedQuery<X> setParameter(int position, Object value) {
		getEntityManager().checkOpen( true );
		try {
			if ( isJpaPositionalParameter( position ) ) {
				this.setParameter( Integer.toString( position ), value );
			}
			else {
				query.setParameter( position - 1, value );
				registerParameterBinding( getParameter( position ), value, null );
			}
			return this;
		}
		catch (QueryParameterException e) {
			throw new IllegalArgumentException( e );
		}
		catch (HibernateException he) {
			throw getEntityManager().convert( he );
		}
	}

	private boolean isJpaPositionalParameter(int position) {
		return jpaPositionalIndices != null && jpaPositionalIndices.contains( position );
	}

	@Override
	public TypedQuery<X> setParameter(int position, Date value, TemporalType temporalType) {
		getEntityManager().checkOpen( true );
		try {
			if ( isJpaPositionalParameter( position ) ) {
				String name = Integer.toString( position );
				this.setParameter( name, value, temporalType );
			}
			else {
				if ( temporalType == DATE ) {
					query.setDate( position - 1, value );
				}
				else if ( temporalType == TIME ) {
					query.setTime( position - 1, value );
				}
				else if ( temporalType == TIMESTAMP ) {
					query.setTimestamp( position - 1, value );
				}
				registerParameterBinding( getParameter( position ), value, temporalType );
			}
			return this;
		}
		catch (QueryParameterException e) {
			throw new IllegalArgumentException( e );
		}
		catch (HibernateException he) {
			throw getEntityManager().convert( he );
		}
	}

	@Override
	public TypedQuery<X> setParameter(int position, Calendar value, TemporalType temporalType) {
		getEntityManager().checkOpen( true );
		try {
			if ( isJpaPositionalParameter( position ) ) {
				String name = Integer.toString( position );
				this.setParameter( name, value, temporalType );
			}
			else {
				if ( temporalType == DATE ) {
					query.setCalendarDate( position - 1, value );
				}
				else if ( temporalType == TIME ) {
					throw new IllegalArgumentException( "not yet implemented" );
				}
				else if ( temporalType == TIMESTAMP ) {
					query.setCalendar( position - 1, value );
				}
				registerParameterBinding( getParameter( position ), value, temporalType );
			}
			return this;
		}
		catch (QueryParameterException e) {
			throw new IllegalArgumentException( e );
		}
		catch (HibernateException he) {
			throw getEntityManager().convert( he );
		}
	}

	@Override
	public Set<Parameter<?>> getParameters() {
		getEntityManager().checkOpen( false );
		return parameters;
	}

	@Override
	public Parameter<?> getParameter(String name) {
		getEntityManager().checkOpen( false );
		if ( name == null ) {
			throw new IllegalArgumentException( "Name of parameter to locate cannot be null" );
		}
		for ( Parameter parameter : parameters ) {
			if ( name.equals( parameter.getName() ) ) {
				return parameter;
			}
		}
		throw new IllegalArgumentException( "Unable to locate parameter named [" + name + "]" );
	}

	@Override
	public Parameter<?> getParameter(int position) {
		getEntityManager().checkOpen( false );
		if ( isJpaPositionalParameter( position ) ) {
			return getParameter( Integer.toString( position ) );
		}
		else {
			for ( Parameter parameter : parameters ) {
				if ( parameter.getPosition() != null && position == parameter.getPosition() ) {
					return parameter;
				}
			}
			throw new IllegalArgumentException( "Unable to locate parameter with position [" + position + "]" );
		}
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <T> Parameter<T> getParameter(String name, Class<T> type) {
		getEntityManager().checkOpen( false );
		Parameter param = getParameter( name );
		if ( param.getParameterType() != null ) {
			// we were able to determine the expected type during analysis, so validate it here
			if ( ! param.getParameterType().isAssignableFrom( type ) ) {
				throw new IllegalArgumentException(
						String.format(
								"Parameter type [%s] is not assignment compatible with requested type [%s] for parameter named [%s]",
								param.getParameterType().getName(),
								type.getName(),
								name
						)
				);
			}
		}
		return param;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <T> Parameter<T> getParameter(int position, Class<T> type) {
		getEntityManager().checkOpen( false );
		Parameter param = getParameter( position );
		if ( param.getParameterType() != null ) {
			// we were able to determine the expected type during analysis, so validate it here
			if ( ! param.getParameterType().isAssignableFrom( type ) ) {
				throw new IllegalArgumentException(
						String.format(
								"Parameter type [%s] is not assignment compatible with requested type [%s] for parameter position [%s]",
								param.getParameterType().getName(),
								type.getName(),
								position
						)
				);
			}
		}
		return param;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <T> T unwrap(Class<T> tClass) {
		if ( org.hibernate.Query.class.isAssignableFrom( tClass ) ) {
			return (T) query;
		}
		else {
			try {
				return (T) this;
			}
			catch ( ClassCastException cce ) {
				PersistenceException pe = new PersistenceException(
						"Unsupported unwrap target type [" + tClass.getName() + "]"
				);
				//It's probably against the spec to not mark the tx for rollback but it will be easier for people
				//getEntityManager().handlePersistenceException( pe );
				throw pe;
			}
		}
	}

	private javax.persistence.LockModeType jpaLockMode = javax.persistence.LockModeType.NONE;

	@Override
    @SuppressWarnings({ "unchecked" })
	public TypedQuery<X> setLockMode(javax.persistence.LockModeType lockModeType) {
		getEntityManager().checkOpen( true );
		if (! getEntityManager().isTransactionInProgress()) {
			throw new TransactionRequiredException( "no transaction is in progress" );
		}
		if ( ! canApplyLockModes() ) {
			throw new IllegalStateException( "Not a JPAQL/Criteria query" );
		}
		this.jpaLockMode = lockModeType;
		query.getLockOptions().setLockMode( LockModeTypeHelper.getLockMode( lockModeType ) );
		if ( getHints() != null && getHints().containsKey( AvailableSettings.LOCK_TIMEOUT ) ) {
			applyLockTimeout( ConfigurationHelper.getInteger( getHints().get( AvailableSettings.LOCK_TIMEOUT ) ) );
		}
		return this;
	}

	@Override
	protected void applyLockTimeout(int timeout) {
		query.getLockOptions().setTimeOut( timeout );
	}

	@Override
    public javax.persistence.LockModeType getLockMode() {
		getEntityManager().checkOpen( false );
		return jpaLockMode;
	}

}
