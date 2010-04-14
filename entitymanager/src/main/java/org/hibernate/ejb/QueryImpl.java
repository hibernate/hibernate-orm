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
package org.hibernate.ejb;

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import static javax.persistence.TemporalType.DATE;
import static javax.persistence.TemporalType.TIME;
import static javax.persistence.TemporalType.TIMESTAMP;
import javax.persistence.TypedQuery;
import javax.persistence.PersistenceException;
import javax.persistence.TransactionRequiredException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.QueryParameterException;
import org.hibernate.TypeMismatchException;
import org.hibernate.SQLQuery;
import org.hibernate.ejb.util.LockModeTypeHelper;
import org.hibernate.engine.query.NamedParameterDescriptor;
import org.hibernate.engine.query.OrdinalParameterDescriptor;
import org.hibernate.hql.QueryExecutionRequestException;
import org.hibernate.impl.AbstractQueryImpl;
import org.hibernate.type.TypeFactory;

/**
 * Hibernate implementation of both the {@link Query} and {@link TypedQuery} contracts.
 *
 * @author <a href="mailto:gavin@hibernate.org">Gavin King</a>
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class QueryImpl<X> extends org.hibernate.ejb.AbstractQueryImpl<X> implements TypedQuery<X>, HibernateQuery {
	private static final Logger log = LoggerFactory.getLogger( QueryImpl.class );

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
		if ( ! AbstractQueryImpl.class.isInstance( query ) ) {
			throw new IllegalStateException( "Unknown query type for parameter extraction" );
		}

		HashSet<Parameter<?>> parameters = new HashSet<Parameter<?>>();
		AbstractQueryImpl queryImpl = AbstractQueryImpl.class.cast( query );

		// extract named params
		for ( String name : (Set<String>) queryImpl.getParameterMetadata().getNamedParameterNames() ) {
			final NamedParameterDescriptor descriptor =
					queryImpl.getParameterMetadata().getNamedParameterDescriptor( name );
			Class javaType = namedParameterTypeRedefinition.get( name );
			if ( javaType != null && mightNeedRedefinition( javaType ) ) {
				descriptor.resetExpectedType(
						TypeFactory.heuristicType( javaType.getName() )
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
			if ( jpaPositionalIndices != null && jpaPositionalIndices.contains( position ) ) {
				log.warn( "Parameter position [" + position + "] occurred as both JPA and Hibernate positional parameter" );
			}
		}

		this.parameters = java.util.Collections.unmodifiableSet( parameters );
	}

	private boolean mightNeedRedefinition(Class javaType) {
		// for now, only really no for dates/times/timestamps
		return java.util.Date.class.isAssignableFrom( javaType );
	}

	private static class ParameterImpl implements Parameter {
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
	}

	public org.hibernate.Query getHibernateQuery() {
		return query;
	}

	protected int internalExecuteUpdate() {
		return query.executeUpdate();
	}

	protected void applyMaxResults(int maxResults) {
		query.setMaxResults( maxResults );
	}

	protected void applyFirstResult(int firstResult) {
		query.setFirstResult( firstResult );
	}

	protected void applyTimeout(int timeout) {
		query.setTimeout( timeout );
	}

	protected void applyComment(String comment) {
		query.setComment( comment );
	}

	protected void applyFetchSize(int fetchSize) {
		query.setFetchSize( fetchSize );
	}

	protected void applyCacheable(boolean isCacheable) {
		query.setCacheable( isCacheable );
	}

	protected void applyCacheRegion(String regionName) {
		query.setCacheRegion( regionName );
	}

	protected void applyReadOnly(boolean isReadOnly) {
		query.setReadOnly( isReadOnly );
	}

	protected void applyCacheMode(CacheMode cacheMode) {
		query.setCacheMode( cacheMode );
	}

	protected void applyFlushMode(FlushMode flushMode) {
		query.setFlushMode( flushMode );
	}

	protected boolean canApplyLockModes() {
		return org.hibernate.impl.QueryImpl.class.isInstance( query );
	}

	@Override
	protected void applyAliasSpecificLockMode(String alias, LockMode lockMode) {
		( (org.hibernate.impl.QueryImpl) query ).getLockOptions().setAliasSpecificLockMode( alias, lockMode );
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked", "RedundantCast" })
	public List<X> getResultList() {
		try {
			return (List<X>) query.list();
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

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked", "RedundantCast" })
	public X getSingleResult() {
		try {
			boolean mucked = false;
			// IMPL NOTE : the mucking with max results here is attempting to help the user from shooting themselves
			//		in the foot in the case where they have a large query by limiting the query results to 2 max
			//    SQLQuery cannot be safely paginated, leaving the user's choice here.
			if ( getSpecifiedMaxResults() != 1 &&
					! ( SQLQuery.class.isAssignableFrom( query.getClass() ) ) ) {
				mucked = true;
				query.setMaxResults( 2 ); //avoid OOME if the list is huge
			}
			List<X> result = (List<X>) query.list();
			if ( mucked ) {
				query.setMaxResults( getSpecifiedMaxResults() );
			}

			if ( result.size() == 0 ) {
				NoResultException nre = new NoResultException( "No entity found for query" );
				getEntityManager().handlePersistenceException( nre );
				throw nre;
			}
			else if ( result.size() > 1 ) {
				Set<X> uniqueResult = new HashSet<X>(result);
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

	public <T> TypedQuery<X> setParameter(Parameter<T> param, T value) {
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

	public TypedQuery<X> setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
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

	public TypedQuery<X> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
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

	/**
	 * {@inheritDoc}
	 */
	public TypedQuery<X> setParameter(String name, Object value) {
		try {
			if ( value instanceof Collection ) {
				query.setParameterList( name, (Collection) value );
			}
			else {
				query.setParameter( name, value );
			}
			registerParameterBinding( getParameter( name ), value );
			return this;
		}
		catch (QueryParameterException e) {
			throw new IllegalArgumentException( e );
		}
		catch (HibernateException he) {
			throw getEntityManager().convert( he );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public TypedQuery<X> setParameter(String name, Date value, TemporalType temporalType) {
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
			registerParameterBinding( getParameter( name ), value );
			return this;
		}
		catch (QueryParameterException e) {
			throw new IllegalArgumentException( e );
		}
		catch (HibernateException he) {
			throw getEntityManager().convert( he );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public TypedQuery<X> setParameter(String name, Calendar value, TemporalType temporalType) {
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
			registerParameterBinding( getParameter(name), value );
			return this;
		}
		catch (QueryParameterException e) {
			throw new IllegalArgumentException( e );
		}
		catch (HibernateException he) {
			throw getEntityManager().convert( he );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public TypedQuery<X> setParameter(int position, Object value) {
		try {
			if ( isJpaPositionalParameter( position ) ) {
				this.setParameter( Integer.toString( position ), value );
			}
			else {
				query.setParameter( position - 1, value );
				registerParameterBinding( getParameter( position ), value );
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

	/**
	 * {@inheritDoc}
	 */
	public TypedQuery<X> setParameter(int position, Date value, TemporalType temporalType) {
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
				registerParameterBinding( getParameter( position ), value );
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

	/**
	 * {@inheritDoc}
	 */
	public TypedQuery<X> setParameter(int position, Calendar value, TemporalType temporalType) {
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
				registerParameterBinding( getParameter( position ), value );
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

	/**
	 * {@inheritDoc}
	 */
	public Set<Parameter<?>> getParameters() {
		return parameters;
	}

	/**
	 * {@inheritDoc}
	 */
	public Parameter<?> getParameter(String name) {
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

	/**
	 * {@inheritDoc}
	 */
	public Parameter<?> getParameter(int position) {
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

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked" })
	public <T> Parameter<T> getParameter(String name, Class<T> type) {
		Parameter param = getParameter( name );
		if ( param.getParameterType() != null ) {
			// we were able to determine the expected type during analysis, so validate it here
			throw new IllegalArgumentException(
					"Parameter type [" + param.getParameterType().getName() +
							"] is not assignment compatible with requested type [" +
							type.getName() + "]"
			);
		}
		return param;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked" })
	public <T> Parameter<T> getParameter(int position, Class<T> type) {
		Parameter param = getParameter( position );
		if ( param.getParameterType() != null ) {
			// we were able to determine the expected type during analysis, so validate it here
			throw new IllegalArgumentException(
					"Parameter type [" + param.getParameterType().getName() +
							"] is not assignment compatible with requested type [" +
							type.getName() + "]"
			);
		}
		return param;
	}

	/**
	 * {@inheritDoc}
	 */
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

	@SuppressWarnings({ "unchecked" })
	public TypedQuery<X> setLockMode(javax.persistence.LockModeType lockModeType) {
		if (! getEntityManager().isTransactionInProgress()) {
			throw new TransactionRequiredException( "no transaction is in progress" );
		}
		if ( ! canApplyLockModes() ) {
			throw new IllegalStateException( "Not a JPAQL/Criteria query" );
		}
		this.jpaLockMode = lockModeType;
		( (org.hibernate.impl.QueryImpl) query ).getLockOptions().setLockMode(
				LockModeTypeHelper.getLockMode( lockModeType )
		);
		return this;
	}

	public javax.persistence.LockModeType getLockMode() {
		return jpaLockMode;
	}

}
