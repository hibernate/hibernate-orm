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
package org.hibernate.ejb;

import static javax.persistence.TemporalType.DATE;
import static javax.persistence.TemporalType.TIME;
import static javax.persistence.TemporalType.TIMESTAMP;

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
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.TransactionRequiredException;
import javax.persistence.TypedQuery;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.QueryParameterException;
import org.hibernate.TypeMismatchException;
import org.hibernate.ejb.internal.EntityManagerMessageLogger;
import org.hibernate.ejb.util.ConfigurationHelper;
import org.hibernate.ejb.util.LockModeTypeHelper;
import org.hibernate.engine.query.spi.NamedParameterDescriptor;
import org.hibernate.engine.query.spi.OrdinalParameterDescriptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.QueryExecutionRequestException;
import org.hibernate.internal.AbstractQueryImpl;
import org.hibernate.internal.SQLQueryImpl;
import org.hibernate.type.CompositeCustomType;
import org.hibernate.type.Type;
import org.jboss.logging.Logger;

/**
 * Hibernate implementation of both the {@link Query} and {@link TypedQuery} contracts.
 *
 * @author <a href="mailto:gavin@hibernate.org">Gavin King</a>
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class QueryImpl<X> extends org.hibernate.ejb.AbstractQueryImpl<X> implements TypedQuery<X>, HibernateQuery {

    public static final EntityManagerMessageLogger LOG = Logger.getMessageLogger(EntityManagerMessageLogger.class, QueryImpl.class.getName());

	private AbstractQueryImpl query;
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
		if ( ! AbstractQueryImpl.class.isInstance( query ) ) {
			throw new IllegalStateException(
					String.format( "Unknown query type [%s]", query.getClass().getName() )
			);
		}
		this.query = (AbstractQueryImpl) query;
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
			if ( javaType != null && mightNeedRedefinition( javaType, descriptor.getExpectedType() ) ) {
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

	private boolean mightNeedRedefinition(Class javaType, Type expectedType) {
		// only redefine dates/times/timestamps that are not wrapped in a CompositeCustomType
		if (expectedType == null)
			return java.util.Date.class.isAssignableFrom( javaType );
		else
			return java.util.Date.class.isAssignableFrom( javaType ) && !CompositeCustomType.class.isAssignableFrom( expectedType.getClass() );
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

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked", "RedundantCast" })
	public List<X> getResultList() {
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

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked", "RedundantCast" })
	public X getSingleResult() {
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

	@Override
    @SuppressWarnings({ "unchecked" })
	public TypedQuery<X> setLockMode(javax.persistence.LockModeType lockModeType) {
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
		return jpaLockMode;
	}

}
