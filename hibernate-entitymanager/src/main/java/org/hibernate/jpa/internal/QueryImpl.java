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
import javax.persistence.ParameterMode;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TemporalType;
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
import org.hibernate.TypeMismatchException;
import org.hibernate.engine.query.spi.NamedParameterDescriptor;
import org.hibernate.engine.query.spi.OrdinalParameterDescriptor;
import org.hibernate.engine.query.spi.ParameterMetadata;
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

		final ParameterMetadata parameterMetadata = org.hibernate.internal.AbstractQueryImpl.class.cast( query ).getParameterMetadata();

		// extract named params
		for ( String name : (Set<String>) parameterMetadata.getNamedParameterNames() ) {
			final NamedParameterDescriptor descriptor = parameterMetadata.getNamedParameterDescriptor( name );
			Class javaType = namedParameterTypeRedefinition.get( name );
			if ( javaType != null && mightNeedRedefinition( javaType ) ) {
				descriptor.resetExpectedType(
						sfi().getTypeResolver().heuristicType( javaType.getName() )
				);
			}
			else if ( descriptor.getExpectedType() != null ) {
				javaType = descriptor.getExpectedType().getReturnedClass();
			}
			registerParameter( new ParameterRegistrationImpl( query, name, javaType ) );
			if ( descriptor.isJpaStyle() ) {
				if ( jpaPositionalIndices == null ) {
					jpaPositionalIndices = new HashSet<Integer>();
				}
				jpaPositionalIndices.add( Integer.valueOf( name ) );
			}
		}

		// extract positional parameters
		for ( int i = 0, max = parameterMetadata.getOrdinalParameterCount(); i < max; i++ ) {
			final OrdinalParameterDescriptor descriptor = parameterMetadata.getOrdinalParameterDescriptor( i + 1 );
			Class javaType = descriptor.getExpectedType() == null ? null : descriptor.getExpectedType().getReturnedClass();
			registerParameter( new ParameterRegistrationImpl( query, i+1, javaType ) );
			Integer position = descriptor.getOrdinalPosition();
            if ( jpaPositionalIndices != null && jpaPositionalIndices.contains(position) ) {
				LOG.parameterPositionOccurredAsBothJpaAndHibernatePositionalParameter(position);
			}
		}
	}

	private SessionFactoryImplementor sfi() {
		return (SessionFactoryImplementor) getEntityManager().getFactory().getSessionFactory();
	}

	private boolean mightNeedRedefinition(Class javaType) {
		// for now, only really no for dates/times/timestamps
		return java.util.Date.class.isAssignableFrom( javaType );
	}



	private static class ParameterRegistrationImpl<T> implements ParameterRegistration<T> {
		private final org.hibernate.Query query;

		private final String name;
		private final Integer position;
		private final Class<T> javaType;

		private ParameterBind<T> bind;

		private ParameterRegistrationImpl(org.hibernate.Query query, String name, Class<T> javaType) {
			this.query = query;
			this.name = name;
			this.javaType = javaType;
			this.position = null;
		}

		private ParameterRegistrationImpl(org.hibernate.Query query, Integer position, Class<T> javaType) {
			this.query = query;
			this.position = position;
			this.javaType = javaType;
			this.name = null;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public Integer getPosition() {
			return position;
		}

		@Override
		public Class<T> getParameterType() {
			return javaType;
		}

		@Override
		public ParameterMode getMode() {
			// implicitly
			return ParameterMode.IN;
		}

		@Override
		public boolean isBindable() {
			// again, implicitly
			return true;
		}

		@Override
		public void bindValue(T value) {
			validateBinding( getParameterType(), value, null );

			if ( name != null ) {
				if ( value instanceof Collection ) {
					query.setParameterList( name, (Collection) value );
				}
				else {
					query.setParameter( name, value );
				}
			}
			else {
				query.setParameter( position-1, value );
			}

			bind = new ParameterBindImpl<T>( value, null );
		}

		@Override
		public void bindValue(T value, TemporalType specifiedTemporalType) {
			validateBinding( getParameterType(), value, specifiedTemporalType );

			if ( Date.class.isInstance( value ) ) {
				if ( name != null ) {
					if ( specifiedTemporalType == DATE ) {
						query.setDate( name, (Date) value );
					}
					else if ( specifiedTemporalType == TIME ) {
						query.setTime( name, (Date) value );
					}
					else if ( specifiedTemporalType == TIMESTAMP ) {
						query.setTimestamp( name, (Date) value );
					}
				}
				else {
					if ( specifiedTemporalType == DATE ) {
						query.setDate( position-1, (Date) value );
					}
					else if ( specifiedTemporalType == TIME ) {
						query.setTime( position-1, (Date) value );
					}
					else if ( specifiedTemporalType == TIMESTAMP ) {
						query.setTimestamp( position-1, (Date) value );
					}
				}
			}
			else if ( Calendar.class.isInstance( value ) ) {
				if ( name != null ) {
					if ( specifiedTemporalType == DATE ) {
						query.setCalendarDate( name, (Calendar) value );
					}
					else if ( specifiedTemporalType == TIME ) {
						throw new IllegalArgumentException( "not yet implemented" );
					}
					else if ( specifiedTemporalType == TIMESTAMP ) {
						query.setCalendar( name, (Calendar) value );
					}
				}
				else {
					if ( specifiedTemporalType == DATE ) {
						query.setCalendarDate( position-1, (Calendar) value );
					}
					else if ( specifiedTemporalType == TIME ) {
						throw new IllegalArgumentException( "not yet implemented" );
					}
					else if ( specifiedTemporalType == TIMESTAMP ) {
						query.setCalendar( position-1, (Calendar) value );
					}
				}
			}
			else {
				throw new IllegalArgumentException(
						"Unexpected type [" + value + "] passed with TemporalType; expecting Date or Calendar"
				);
			}

			bind = new ParameterBindImpl<T>( value, specifiedTemporalType );
		}

		@Override
		public ParameterBind<T> getBind() {
			return bind;
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
    protected boolean applyTimeoutHint(int timeout) {
		query.setTimeout( timeout );
		return true;
	}

	@Override
    protected boolean applyCommentHint(String comment) {
		query.setComment( comment );
		return true;
	}

	@Override
    protected boolean applyFetchSizeHint(int fetchSize) {
		query.setFetchSize( fetchSize );
		return true;
	}

	@Override
    protected boolean applyCacheableHint(boolean isCacheable) {
		query.setCacheable( isCacheable );
		return true;
	}

	@Override
    protected boolean applyCacheRegionHint(String regionName) {
		query.setCacheRegion( regionName );
		return true;
	}

	@Override
    protected boolean applyReadOnlyHint(boolean isReadOnly) {
		query.setReadOnly( isReadOnly );
		return true;
	}

	@Override
    protected boolean applyCacheModeHint(CacheMode cacheMode) {
		query.setCacheMode( cacheMode );
		return true;
	}

	@Override
    protected boolean applyFlushModeHint(FlushMode flushMode) {
		query.setFlushMode( flushMode );
		return true;
	}

	@Override
	protected boolean canApplyAliasSpecificLockModeHints() {
		return org.hibernate.internal.QueryImpl.class.isInstance( query ) || SQLQueryImpl.class.isInstance( query );
	}

	@Override
	protected void applyAliasSpecificLockModeHint(String alias, LockMode lockMode) {
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
	protected boolean isJpaPositionalParameter(int position) {
		return jpaPositionalIndices != null && jpaPositionalIndices.contains( position );
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

	@Override
	protected void internalApplyLockMode(javax.persistence.LockModeType lockModeType) {
		query.getLockOptions().setLockMode( LockModeTypeHelper.getLockMode( lockModeType ) );
		if ( getHints() != null && getHints().containsKey( AvailableSettings.LOCK_TIMEOUT ) ) {
			applyLockTimeoutHint( ConfigurationHelper.getInteger( getHints().get( AvailableSettings.LOCK_TIMEOUT ) ) );
		}
	}

	@Override
	protected boolean applyLockTimeoutHint(int timeout) {
		query.getLockOptions().setTimeOut( timeout );
		return true;
	}


}
