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
import javax.persistence.ParameterMode;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.SQLQuery;
import org.hibernate.TypeMismatchException;
import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.engine.query.spi.NamedParameterDescriptor;
import org.hibernate.engine.query.spi.OrdinalParameterDescriptor;
import org.hibernate.engine.query.spi.ParameterMetadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.hql.internal.QueryExecutionRequestException;
import org.hibernate.internal.SQLQueryImpl;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.HibernateQuery;
import org.hibernate.jpa.internal.util.ConfigurationHelper;
import org.hibernate.jpa.internal.util.LockModeTypeHelper;
import org.hibernate.jpa.spi.AbstractEntityManagerImpl;
import org.hibernate.jpa.spi.AbstractQueryImpl;
import org.hibernate.jpa.spi.ParameterBind;
import org.hibernate.jpa.spi.ParameterRegistration;
import org.hibernate.type.CompositeCustomType;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

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

	@Override
	protected boolean isNativeSqlQuery() {
		return SQLQuery.class.isInstance( query );
	}

	@Override
	protected boolean isSelectQuery() {
		if ( isNativeSqlQuery() ) {
			throw new IllegalStateException( "Cannot tell if native SQL query is SELECT query" );
		}

		return org.hibernate.internal.QueryImpl.class.cast( query ).isSelect();
	}

	@SuppressWarnings({ "unchecked", "RedundantCast" })
	private void extractParameterInfo(Map<String,Class> namedParameterTypeRedefinition) {
		if ( ! org.hibernate.internal.AbstractQueryImpl.class.isInstance( query ) ) {
			throw new IllegalStateException( "Unknown query type for parameter extraction" );
		}

		boolean hadJpaPositionalParameters = false;

		final ParameterMetadata parameterMetadata = org.hibernate.internal.AbstractQueryImpl.class.cast( query ).getParameterMetadata();

		// extract named params
		for ( String name : (Set<String>) parameterMetadata.getNamedParameterNames() ) {
			final NamedParameterDescriptor descriptor = parameterMetadata.getNamedParameterDescriptor( name );
			Class javaType = namedParameterTypeRedefinition.get( name );
			if ( javaType != null && mightNeedRedefinition( javaType, descriptor.getExpectedType() ) ) {
				descriptor.resetExpectedType(
						sfi().getTypeResolver().heuristicType( javaType.getName() )
				);
			}
			else if ( descriptor.getExpectedType() != null ) {
				javaType = descriptor.getExpectedType().getReturnedClass();
			}

			if ( descriptor.isJpaStyle() ) {
				hadJpaPositionalParameters = true;
				final Integer position = Integer.valueOf( name );
				registerParameter( new JpaPositionalParameterRegistrationImpl( this, query, position, javaType ) );
			}
			else {
				registerParameter( new ParameterRegistrationImpl( this, query, name, javaType ) );
			}
		}

		if ( hadJpaPositionalParameters ) {
			if ( parameterMetadata.getOrdinalParameterCount() > 0 ) {
				throw new IllegalArgumentException(
						"Cannot mix JPA positional parameters and native Hibernate positional/ordinal parameters"
				);
			}
		}

		// extract Hibernate native positional parameters
		for ( int i = 0, max = parameterMetadata.getOrdinalParameterCount(); i < max; i++ ) {
			final OrdinalParameterDescriptor descriptor = parameterMetadata.getOrdinalParameterDescriptor( i + 1 );
			Class javaType = descriptor.getExpectedType() == null ? null : descriptor.getExpectedType().getReturnedClass();
			registerParameter( new ParameterRegistrationImpl( this, query, i+1, javaType ) );
		}
	}

	private SessionFactoryImplementor sfi() {
		return (SessionFactoryImplementor) getEntityManager().getFactory().getSessionFactory();
	}

	private boolean mightNeedRedefinition(Class javaType, Type expectedType) {
		// only redefine dates/times/timestamps that are not wrapped in a CompositeCustomType
		if ( expectedType == null )
			return java.util.Date.class.isAssignableFrom( javaType );
		else
			return java.util.Date.class.isAssignableFrom( javaType )
					&& !CompositeCustomType.class.isAssignableFrom( expectedType.getClass() );
	}

	private static class ParameterRegistrationImpl<T> implements ParameterRegistration<T> {
		private final Query jpaQuery;
		private final org.hibernate.Query nativeQuery;

		private final String name;
		private final Integer position;
		private final Class<T> javaType;

		private ParameterBind<T> bind;

		protected ParameterRegistrationImpl(
				Query jpaQuery,
				org.hibernate.Query nativeQuery,
				String name,
				Class<T> javaType) {
			this.jpaQuery = jpaQuery;
			this.nativeQuery = nativeQuery;
			this.name = name;
			this.javaType = javaType;
			this.position = null;
		}

		protected ParameterRegistrationImpl(
				Query jpaQuery,
				org.hibernate.Query nativeQuery,
				Integer position,
				Class<T> javaType) {
			this.jpaQuery = jpaQuery;
			this.nativeQuery = nativeQuery;
			this.position = position;
			this.javaType = javaType;
			this.name = null;
		}

		@Override
		public boolean isJpaPositionalParameter() {
			return false;
		}

		@Override
		public Query getQuery() {
			return jpaQuery;
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
					nativeQuery.setParameterList( name, (Collection) value );
				}
				else {
					nativeQuery.setParameter( name, value );
				}
			}
			else {
				nativeQuery.setParameter( position - 1, value );
			}

			bind = new ParameterBindImpl<T>( value, null );
		}

		@Override
		public void bindValue(T value, TemporalType specifiedTemporalType) {
			validateBinding( getParameterType(), value, specifiedTemporalType );

			if ( Date.class.isInstance( value ) ) {
				if ( name != null ) {
					if ( specifiedTemporalType == DATE ) {
						nativeQuery.setDate( name, (Date) value );
					}
					else if ( specifiedTemporalType == TIME ) {
						nativeQuery.setTime( name, (Date) value );
					}
					else if ( specifiedTemporalType == TIMESTAMP ) {
						nativeQuery.setTimestamp( name, (Date) value );
					}
				}
				else {
					if ( specifiedTemporalType == DATE ) {
						nativeQuery.setDate( position - 1, (Date) value );
					}
					else if ( specifiedTemporalType == TIME ) {
						nativeQuery.setTime( position - 1, (Date) value );
					}
					else if ( specifiedTemporalType == TIMESTAMP ) {
						nativeQuery.setTimestamp( position - 1, (Date) value );
					}
				}
			}
			else if ( Calendar.class.isInstance( value ) ) {
				if ( name != null ) {
					if ( specifiedTemporalType == DATE ) {
						nativeQuery.setCalendarDate( name, (Calendar) value );
					}
					else if ( specifiedTemporalType == TIME ) {
						throw new IllegalArgumentException( "not yet implemented" );
					}
					else if ( specifiedTemporalType == TIMESTAMP ) {
						nativeQuery.setCalendar( name, (Calendar) value );
					}
				}
				else {
					if ( specifiedTemporalType == DATE ) {
						nativeQuery.setCalendarDate( position - 1, (Calendar) value );
					}
					else if ( specifiedTemporalType == TIME ) {
						throw new IllegalArgumentException( "not yet implemented" );
					}
					else if ( specifiedTemporalType == TIMESTAMP ) {
						nativeQuery.setCalendar( position - 1, (Calendar) value );
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

	/**
	 * Specialized handling for JPA "positional parameters".
	 *
	 * @param <T> The parameter type type.
	 */
	public static class JpaPositionalParameterRegistrationImpl<T> extends ParameterRegistrationImpl<T> {
		final Integer position;

		protected JpaPositionalParameterRegistrationImpl(
				Query jpaQuery,
				org.hibernate.Query nativeQuery,
				Integer position,
				Class<T> javaType) {
			super( jpaQuery, nativeQuery, position.toString(), javaType );
			this.position = position;
		}

		@Override
		public String getName() {
			return null;
		}

		@Override
		public Integer getPosition() {
			return position;
		}

		@Override
		public boolean isJpaPositionalParameter() {
			return true;
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
		checkTransaction();
		beforeQuery();
		try {
			return list();
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
	 * For JPA native SQL queries, we may need to perform a flush before executing the query.
	 */
	private void beforeQuery() {
		final org.hibernate.Query query = getHibernateQuery();
		if ( ! SQLQuery.class.isInstance( query ) ) {
			// this need only exists for native SQL queries, not JPQL or Criteria queries (both of which do
			// partial auto flushing already).
			return;
		}

		final SQLQuery sqlQuery = (SQLQuery) query;
		if ( sqlQuery.getSynchronizedQuerySpaces() != null && ! sqlQuery.getSynchronizedQuerySpaces().isEmpty() ) {
			// The application defined query spaces on the Hibernate native SQLQuery which means the query will already
			// perform a partial flush according to the defined query spaces, no need to do a full flush.
			return;
		}

		// otherwise we need to flush.  the query itself is not required to execute in a transaction; if there is
		// no transaction, the flush would throw a TransactionRequiredException which would potentially break existing
		// apps, so we only do the flush if a transaction is in progress.
		if ( getEntityManager().isTransactionInProgress() ) {
			getEntityManager().flush();
		}
	}

	@Override
	@SuppressWarnings({ "unchecked", "RedundantCast" })
	public X getSingleResult() {
		getEntityManager().checkOpen( true );
		checkTransaction();
		beforeQuery();
		try {
			final List<X> result = list();

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
	@SuppressWarnings({ "unchecked" })
	public <T> T unwrap(Class<T> tClass) {
		if ( org.hibernate.Query.class.isAssignableFrom( tClass ) ) {
			return (T) query;
		}
		if ( QueryImpl.class.isAssignableFrom( tClass ) ) {
			return (T) this;
		}
		if ( HibernateQuery.class.isAssignableFrom( tClass ) ) {
			return (T) this;
		}

		throw new PersistenceException(
				String.format(
						"Unsure how to unwrap %s impl [%s] as requested type [%s]",
						Query.class.getSimpleName(),
						this.getClass().getName(),
						tClass.getName()
				)
		);
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

	private List<X> list() {
		if (getEntityGraphQueryHint() != null) {
			SessionImplementor sessionImpl = (SessionImplementor) getEntityManager().getSession();
			HQLQueryPlan entityGraphQueryPlan = new HQLQueryPlan( getHibernateQuery().getQueryString(), false,
					sessionImpl.getEnabledFilters(), sessionImpl.getFactory(), getEntityGraphQueryHint() );
			// Safe to assume QueryImpl at this point.
			unwrap( org.hibernate.internal.QueryImpl.class ).setQueryPlan( entityGraphQueryPlan );
		}
		return query.list();
	}

}
