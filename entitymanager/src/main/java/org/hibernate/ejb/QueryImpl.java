//$Id$
package org.hibernate.ejb;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import javax.persistence.FlushModeType;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import static javax.persistence.TemporalType.*;
import javax.persistence.TransactionRequiredException;
import javax.persistence.LockModeType;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.QueryParameterException;
import org.hibernate.TypeMismatchException;
import org.hibernate.impl.AbstractQueryImpl;
import org.hibernate.ejb.util.ConfigurationHelper;
import org.hibernate.hql.QueryExecutionRequestException;

/**
 * @author <a href="mailto:gavin@hibernate.org">Gavin King</a>
 * @author Emmanuel Bernard
 */
public class QueryImpl implements Query, HibernateQuery {
	private org.hibernate.Query query;
	private HibernateEntityManagerImplementor em;
	private Boolean isPositional = null;
	private int maxResults = -1;
	private int firstResult;

	public QueryImpl(org.hibernate.Query query, AbstractEntityManagerImpl em) {
		this.query = query;
		this.em = em;
	}

	public org.hibernate.Query getHibernateQuery() {
		return query;
	}

	public int executeUpdate() {
		try {
			if ( ! em.isTransactionInProgress() ) {
				em.throwPersistenceException( new TransactionRequiredException( "Executing an update/delete query" ) );
				return 0;
			}
			return query.executeUpdate();
		}
		catch (QueryExecutionRequestException he) {
			throw new IllegalStateException(he);
		}
		catch( TypeMismatchException e ) {
			throw new IllegalArgumentException(e);
		}
		catch (HibernateException he) {
			em.throwPersistenceException( he );
			return 0;
		}
	}

	public List getResultList() {
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
			em.throwPersistenceException( he );
			return null;
		}
	}

	public Object getSingleResult() {
		try {
			List result;
			/* Avoid OOME if the list() is huge (user faulty query) by limiting the query to 2 elements max */
			//FIXME: get rid of this impl binding (HHH-3432)
			if ( query instanceof AbstractQueryImpl ) {
				if (maxResults != 1) query.setMaxResults( 2 ); //avoid OOME if the list is huge
				result = query.list();
				if ( maxResults != -1 ) {
					query.setMaxResults( maxResults ); //put back the original value
				}
				else {
					AbstractQueryImpl queryImpl = AbstractQueryImpl.class.cast( query );
					queryImpl.getSelection().setMaxRows( null );
				}
			}
			else {
				//we can't do much because we cannot reset the maxResults => do the full list call
				//Not tremendously bad as the user is doing a fault here anyway by calling getSingleREsults on a big list
				result = query.list();
			}

			if ( result.size() == 0 ) {
				em.throwPersistenceException( new NoResultException( "No entity found for query" ) );
			}
			else if ( result.size() > 1 ) {
				Set uniqueResult = new HashSet(result);
				if ( uniqueResult.size() > 1 ) {
					em.throwPersistenceException( new NonUniqueResultException( "result returns more than one elements") );
				}
				else {
					return uniqueResult.iterator().next();
				}

			}
			else {
				return result.get(0);
			}
			return null; //should never happen
		}
		catch (QueryExecutionRequestException he) {
			throw new IllegalStateException(he);
		}
		catch( TypeMismatchException e ) {
			throw new IllegalArgumentException(e);
		}
		catch (HibernateException he) {
			em.throwPersistenceException( he );
			return null;
		}
	}

	public Query setMaxResults(int maxResult) {
		if ( maxResult < 0 ) {
			throw new IllegalArgumentException(
					"Negative ("
							+ maxResult
							+ ") parameter passed in to setMaxResults"
			);
		}
		this.maxResults = maxResult;
		query.setMaxResults( maxResult );
		return this;
	}

	public int getMaxResults() {
		return maxResults == -1 ? Integer.MAX_VALUE : maxResults; //stupid spec MAX_VALUE??
	}

	public Query setFirstResult(int firstResult) {
		if ( firstResult < 0 ) {
			throw new IllegalArgumentException(
					"Negative ("
							+ firstResult
							+ ") parameter passed in to setFirstResult"
			);
		}
		query.setFirstResult( firstResult );
		this.firstResult = firstResult;
		return this;
	}

	public int getFirstResult() {
		return firstResult;
	}

	public Query setHint(String hintName, Object value) {
		try {
			if ( "org.hibernate.timeout".equals( hintName ) ) {
				query.setTimeout( ConfigurationHelper.getInteger( value ) );
			}
			else if ( "org.hibernate.comment".equals( hintName ) ) {
				query.setComment( (String) value );
			}
			else if ( "org.hibernate.fetchSize".equals( hintName ) ) {
				query.setFetchSize( ConfigurationHelper.getInteger( value ) );
			}
			else if ( "org.hibernate.cacheRegion".equals( hintName ) ) {
				query.setCacheRegion( (String) value );
			}
			else if ( "org.hibernate.cacheable".equals( hintName ) ) {
				query.setCacheable( ConfigurationHelper.getBoolean( value ) );
			}
			else if ( "org.hibernate.readOnly".equals( hintName ) ) {
				query.setReadOnly( ConfigurationHelper.getBoolean( value ) );
			}
			else if ( "org.hibernate.cacheMode".equals( hintName ) ) {
				query.setCacheMode( ConfigurationHelper.getCacheMode( value ) );
			}
			else if ( "org.hibernate.flushMode".equals( hintName ) ) {
				query.setFlushMode( ConfigurationHelper.getFlushMode( value ) );
			}
			//TODO:
			/*else if ( "org.hibernate.lockMode".equals( hintName ) ) {
				query.setLockMode( alias, lockMode );
			}*/
		}
		catch (ClassCastException e) {
			throw new IllegalArgumentException( "Value for hint" );
		}
		return this;
	}

	public Map<String, Object> getHints() {
		//FIXME
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public Set<String> getSupportedHints() {
		//FIXME
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public Query setParameter(String name, Object value) {
		try {
			if ( value instanceof Collection ) {
				query.setParameterList( name, (Collection) value );
			}
			else {
				query.setParameter( name, value );
			}
			return this;
		}
		catch (QueryParameterException e) {
			throw new IllegalArgumentException( e );
		}
		catch (HibernateException he) {
			em.throwPersistenceException( he );
			return null;
		}
	}

	public Query setParameter(String name, Date value, TemporalType temporalType) {
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
			return this;
		}
		catch (QueryParameterException e) {
			throw new IllegalArgumentException( e );
		}
		catch (HibernateException he) {
			em.throwPersistenceException( he );
			return null;
		}
	}

	public Query setParameter(String name, Calendar value, TemporalType temporalType) {
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
			return this;
		}
		catch (QueryParameterException e) {
			throw new IllegalArgumentException( e );
		}
		catch (HibernateException he) {
			em.throwPersistenceException( he );
			return null;
		}
	}

	public Query setParameter(int position, Object value) {
		try {
			if ( isPositionalParameter() ) {
				this.setParameter( Integer.toString( position ), value );
			}
			else {
				query.setParameter( position - 1, value );
			}
			return this;
		}
		catch (QueryParameterException e) {
			throw new IllegalArgumentException( e );
		}
		catch (HibernateException he) {
			em.throwPersistenceException( he );
			return null;
		}
	}

	private boolean isPositionalParameter() {
		if (isPositional == null) {
			//compute it
			String queryString = query.getQueryString();
			int index = queryString.indexOf( '?' );
			//there is a ? and the following char is a digit
			if (index == -1) {
				//no ?
				isPositional = true;
			}
			else if ( index == queryString.length() - 1 ) {
				// "... ?"
				isPositional = false;
			}
			else {
				isPositional = Character.isDigit( queryString.charAt( index + 1 ) );
			}
		}
		return isPositional;
	}

	public Query setParameter(int position, Date value, TemporalType temporalType) {
		try {
			if ( isPositionalParameter() ) {
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
			}
			return this;
		}
		catch (QueryParameterException e) {
			throw new IllegalArgumentException( e );
		}
		catch (HibernateException he) {
			em.throwPersistenceException( he );
			return null;
		}
	}

	public Query setParameter(int position, Calendar value, TemporalType temporalType) {
		try {
			if ( isPositionalParameter() ) {
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
			}
			return this;
		}
		catch (QueryParameterException e) {
			throw new IllegalArgumentException( e );
		}
		catch (HibernateException he) {
			em.throwPersistenceException( he );
			return null;
		}
	}

	public Map<String, Object> getNamedParameters() {
		//FIXME
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public List getPositionalParameters() {
		//FIXME
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public Query setFlushMode(FlushModeType flushMode) {
		if ( flushMode == FlushModeType.AUTO ) {
			query.setFlushMode( FlushMode.AUTO );
		}
		else if ( flushMode == FlushModeType.COMMIT ) {
			query.setFlushMode( FlushMode.COMMIT );
		}
		return this;
	}

	public FlushModeType getFlushMode() {
		//FIXME
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public Query setLockMode(LockModeType lockModeType) {
		//FIXME
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public LockModeType getLockMode() {
		//FIXME
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public <T> T unwrap(Class<T> tClass) {
		//FIXME
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}
}
