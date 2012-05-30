/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010 by Red Hat Inc and/or its affiliates or by
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.persistence.CacheRetrieveMode;
import javax.persistence.CacheStoreMode;
import javax.persistence.FlushModeType;
import javax.persistence.Parameter;
import javax.persistence.TransactionRequiredException;
import javax.persistence.TypedQuery;

import org.jboss.logging.Logger;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.TypeMismatchException;
import org.hibernate.ejb.internal.EntityManagerMessageLogger;
import org.hibernate.ejb.util.CacheModeHelper;
import org.hibernate.ejb.util.ConfigurationHelper;
import org.hibernate.ejb.util.LockModeTypeHelper;
import org.hibernate.hql.internal.QueryExecutionRequestException;

import static org.hibernate.ejb.QueryHints.HINT_CACHEABLE;
import static org.hibernate.ejb.QueryHints.HINT_CACHE_MODE;
import static org.hibernate.ejb.QueryHints.HINT_CACHE_REGION;
import static org.hibernate.ejb.QueryHints.HINT_COMMENT;
import static org.hibernate.ejb.QueryHints.HINT_FETCH_SIZE;
import static org.hibernate.ejb.QueryHints.HINT_FLUSH_MODE;
import static org.hibernate.ejb.QueryHints.HINT_READONLY;
import static org.hibernate.ejb.QueryHints.HINT_TIMEOUT;
import static org.hibernate.ejb.QueryHints.SPEC_HINT_TIMEOUT;

/**
 * Intended as a base class providing convenience in implementing both {@link javax.persistence.Query} and
 * {@link javax.persistence.TypedQuery}.
 * <p/>
 * IMPL NOTE : This issue, and the reason for this distinction, is that criteria and hl.sql queries share no
 * commonality currently in Hibernate internals.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractQueryImpl<X> implements TypedQuery<X> {

    private static final EntityManagerMessageLogger LOG = Logger.getMessageLogger(EntityManagerMessageLogger.class,
                                                                           AbstractQueryImpl.class.getName());

	private final HibernateEntityManagerImplementor entityManager;

	public AbstractQueryImpl(HibernateEntityManagerImplementor entityManager) {
		this.entityManager = entityManager;
	}

	protected HibernateEntityManagerImplementor getEntityManager() {
		return entityManager;
	}

	/**
	 * Actually execute the update; all pre-requisites have been checked.
	 *
	 * @return The number of "affected rows".
	 */
	protected abstract int internalExecuteUpdate();

	@Override
	@SuppressWarnings({ "ThrowableInstanceNeverThrown" })
	public int executeUpdate() {
		try {
			if ( ! entityManager.isTransactionInProgress() ) {
				entityManager.throwPersistenceException( new TransactionRequiredException( "Executing an update/delete query" ) );
				return 0;
			}
			return internalExecuteUpdate();
		}
		catch ( QueryExecutionRequestException he) {
			throw new IllegalStateException(he);
		}
		catch( TypeMismatchException e ) {
			throw new IllegalArgumentException(e);
		}
		catch ( HibernateException he) {
			entityManager.throwPersistenceException( he );
			return 0;
		}
	}

	private int maxResults = -1;

	/**
	 * Apply the given max results value.
	 *
	 * @param maxResults The specified max results
	 */
	protected abstract void applyMaxResults(int maxResults);

	@Override
	public TypedQuery<X> setMaxResults(int maxResult) {
		if ( maxResult < 0 ) {
			throw new IllegalArgumentException(
					"Negative value (" + maxResult + ") passed to setMaxResults"
			);
		}
		this.maxResults = maxResult;
		applyMaxResults( maxResult );
		return this;
	}

	public int getSpecifiedMaxResults() {
		return maxResults;
	}

	@Override
	public int getMaxResults() {
		return maxResults == -1
				? Integer.MAX_VALUE // stupid spec... MAX_VALUE??
				: maxResults;
	}

	private int firstResult;

	/**
	 * Apply the given first-result value.
	 *
	 * @param firstResult The specified first-result value.
	 */
	protected abstract void applyFirstResult(int firstResult);

	@Override
	public TypedQuery<X> setFirstResult(int firstResult) {
		if ( firstResult < 0 ) {
			throw new IllegalArgumentException(
					"Negative value (" + firstResult + ") passed to setFirstResult"
			);
		}
		this.firstResult = firstResult;
		applyFirstResult( firstResult );
		return this;
	}

	@Override
	public int getFirstResult() {
		return firstResult;
	}

	private Map<String, Object> hints;

	@Override
	public Map<String, Object> getHints() {
		return hints;
	}

	protected abstract void applyTimeout(int timeout);

	protected abstract void applyLockTimeout(int timeout);

	protected abstract void applyComment(String comment);

	protected abstract void applyFetchSize(int fetchSize);

	protected abstract void applyCacheable(boolean isCacheable);

	protected abstract void applyCacheRegion(String regionName);

	protected abstract void applyReadOnly(boolean isReadOnly);

	protected abstract void applyCacheMode(CacheMode cacheMode);

	protected abstract void applyFlushMode(FlushMode flushMode);

	protected abstract boolean canApplyLockModes();

	protected abstract void applyAliasSpecificLockMode(String alias, LockMode lockMode);

	@Override
	@SuppressWarnings( {"deprecation"})
	public TypedQuery<X> setHint(String hintName, Object value) {
		boolean skipped = false;
		try {
			if ( HINT_TIMEOUT.equals( hintName ) ) {
				applyTimeout( ConfigurationHelper.getInteger( value ) );
			}
			else if ( SPEC_HINT_TIMEOUT.equals( hintName ) ) {
				// convert milliseconds to seconds
				int timeout = (int)Math.round(ConfigurationHelper.getInteger( value ).doubleValue() / 1000.0 );
				applyTimeout( timeout );
			}
			else if ( AvailableSettings.LOCK_TIMEOUT.equals( hintName ) ) {
				applyLockTimeout( ConfigurationHelper.getInteger( value ) );
			}
			else if ( HINT_COMMENT.equals( hintName ) ) {
				applyComment( (String) value );
			}
			else if ( HINT_FETCH_SIZE.equals( hintName ) ) {
				applyFetchSize( ConfigurationHelper.getInteger( value ) );
			}
			else if ( HINT_CACHEABLE.equals( hintName ) ) {
				applyCacheable( ConfigurationHelper.getBoolean( value ) );
			}
			else if ( HINT_CACHE_REGION.equals( hintName ) ) {
				applyCacheRegion( (String) value );
			}
			else if ( HINT_READONLY.equals( hintName ) ) {
				applyReadOnly( ConfigurationHelper.getBoolean( value ) );
			}
			else if ( HINT_CACHE_MODE.equals( hintName ) ) {
				applyCacheMode( ConfigurationHelper.getCacheMode( value ) );
			}
			else if ( HINT_FLUSH_MODE.equals( hintName ) ) {
				applyFlushMode( ConfigurationHelper.getFlushMode( value ) );
			}
			else if ( AvailableSettings.SHARED_CACHE_RETRIEVE_MODE.equals( hintName ) ) {
				final CacheRetrieveMode retrieveMode = (CacheRetrieveMode) value;

				CacheStoreMode storeMode = hints != null
						? (CacheStoreMode) hints.get( AvailableSettings.SHARED_CACHE_STORE_MODE )
						: null;
				if ( storeMode == null ) {
					storeMode = (CacheStoreMode) entityManager.getProperties()
							.get( AvailableSettings.SHARED_CACHE_STORE_MODE );
				}
				applyCacheMode(
						CacheModeHelper.interpretCacheMode( storeMode, retrieveMode )
				);
			}
			else if ( AvailableSettings.SHARED_CACHE_STORE_MODE.equals( hintName ) ) {
				final CacheStoreMode storeMode = (CacheStoreMode) value;

				CacheRetrieveMode retrieveMode = hints != null
						? (CacheRetrieveMode) hints.get( AvailableSettings.SHARED_CACHE_RETRIEVE_MODE )
						: null;
				if ( retrieveMode == null ) {
					retrieveMode = (CacheRetrieveMode) entityManager.getProperties()
							.get( AvailableSettings.SHARED_CACHE_RETRIEVE_MODE );
				}
				applyCacheMode(
						CacheModeHelper.interpretCacheMode( storeMode, retrieveMode )
				);
			}
			else if ( hintName.startsWith( AvailableSettings.ALIAS_SPECIFIC_LOCK_MODE ) ) {
				if ( ! canApplyLockModes() ) {
					skipped = true;
				}
				else {
					// extract the alias
					final String alias = hintName.substring( AvailableSettings.ALIAS_SPECIFIC_LOCK_MODE.length() + 1 );
					// determine the LockMode
					try {
						final LockMode lockMode = LockModeTypeHelper.interpretLockMode( value );
						applyAliasSpecificLockMode( alias, lockMode );
					}
					catch ( Exception e ) {
                        LOG.unableToDetermineLockModeValue(hintName, value);
						skipped = true;
					}
				}
			}
			else {
				skipped = true;
                LOG.ignoringUnrecognizedQueryHint(hintName);
			}
		}
		catch ( ClassCastException e ) {
			throw new IllegalArgumentException( "Value for hint" );
		}

		if ( !skipped ) {
			if ( hints == null ) {
				hints = new HashMap<String,Object>();
			}
			hints.put( hintName, value );
		}

		return this;
	}

	@SuppressWarnings( {"UnusedDeclaration"})
	public Set<String> getSupportedHints() {
		return QueryHints.getDefinedHints();
	}

	@Override
	public abstract TypedQuery<X> setLockMode(javax.persistence.LockModeType lockModeType);

	@Override
	public abstract javax.persistence.LockModeType getLockMode();

	private FlushModeType jpaFlushMode;

	@Override
	public TypedQuery<X> setFlushMode(FlushModeType jpaFlushMode) {
		this.jpaFlushMode = jpaFlushMode;
		// TODO : treat as hint?
		if ( jpaFlushMode == FlushModeType.AUTO ) {
			applyFlushMode( FlushMode.AUTO );
		}
		else if ( jpaFlushMode == FlushModeType.COMMIT ) {
			applyFlushMode( FlushMode.COMMIT );
		}
		return this;
	}

	@SuppressWarnings( {"UnusedDeclaration"})
	protected FlushModeType getSpecifiedFlushMode() {
		return jpaFlushMode;
	}

	@Override
	public FlushModeType getFlushMode() {
		return jpaFlushMode != null
				? jpaFlushMode
				: entityManager.getFlushMode();
	}

	private Map parameterBindings;

	@SuppressWarnings( {"unchecked"})
	protected void registerParameterBinding(Parameter parameter, Object value) {
		if ( parameter == null ) {
			throw new IllegalArgumentException( "parameter cannot be null" );
		}

		validateParameterBinding( parameter, value );

		if ( parameterBindings == null ) {
			parameterBindings = new HashMap();
		}
		parameterBindings.put( parameter, value );
	}

	private void validateParameterBinding(Parameter parameter, Object value) {
		if ( value == null || parameter.getParameterType() == null ) {
			// nothing we can check
			return;
		}

		if ( Collection.class.isInstance( value )
				&& ! Collection.class.isAssignableFrom( parameter.getParameterType() ) ) {
			// we have a collection passed in where we are expecting a non-collection.
			// 		NOTE : this can happen in Hibernate's notion of "parameter list" binding
			// 		NOTE2 : the case of a collection value and an expected collection (if that can even happen)
			//			will fall through to the main check.
			validateCollectionValuedParameterMultiBinding( parameter, (Collection) value );
		}
		else if ( value.getClass().isArray() ) {
			validateArrayValuedParameterBinding( parameter, value );
		}
		else {
			if ( ! parameter.getParameterType().isInstance( value ) ) {
				throw new IllegalArgumentException(
						String.format(
								"Parameter value [%s] did not match expected type [%s]",
								value,
								parameter.getParameterType().getName()
						)
				);
			}
		}
	}

	private void validateCollectionValuedParameterMultiBinding(Parameter parameter, Collection value) {
		// validate the elements...
		for ( Object element : value ) {
			if ( ! parameter.getParameterType().isInstance( element ) ) {
				throw new IllegalArgumentException(
						String.format(
								"Parameter value element [%s] did not match expected type [%s]",
								element,
								parameter.getParameterType().getName()
						)
				);
			}
		}
	}

	private void validateArrayValuedParameterBinding(Parameter parameter, Object value) {
		if ( ! parameter.getParameterType().isArray() ) {
			throw new IllegalArgumentException(
					String.format(
							"Encountered array-valued parameter binding, but was expecting [%s]",
							parameter.getParameterType().getName()
					)
			);
		}

		if ( value.getClass().getComponentType().isPrimitive() ) {
			// we have a primitive array.  we validate that the actual array has the component type (type odf elements)
			// we expect based on the component type of the parameter specification
			if ( ! parameter.getParameterType().getComponentType().isAssignableFrom( value.getClass().getComponentType() ) ) {
				throw new IllegalArgumentException(
						String.format(
								"Primitive array-valued parameter bind value type [%s] did not match expected type [%s]",
								value.getClass().getComponentType().getName(),
								parameter.getParameterType().getName()
						)
				);
			}
		}
		else {
			// we have an object array.  Here we loop over the array and physically check each element against
			// the type we expect based on the component type of the parameter specification
			final Object[] array = (Object[]) value;
			for ( Object element : array ) {
				if ( ! parameter.getParameterType().getComponentType().isInstance( element ) ) {
					throw new IllegalArgumentException(
							String.format(
									"Array-valued parameter value element [%s] did not match expected type [%s]",
									element,
									parameter.getParameterType().getName()
							)
					);
				}
			}
		}
	}

	@Override
    public boolean isBound(Parameter<?> param) {
		return parameterBindings != null && parameterBindings.containsKey( param );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <T> T getParameterValue(Parameter<T> param) {
		if ( parameterBindings == null ) {
			throw new IllegalStateException( "No parameters have been bound" );
		}
		try {
			T value = (T) parameterBindings.get( param );
			if ( value == null ) {
				throw new IllegalStateException( "Parameter has not been bound" );
			}
			return value;
		}
		catch ( ClassCastException cce ) {
			throw new IllegalStateException( "Encountered a parameter value type exception" );
		}
	}

	@Override
	public Object getParameterValue(String name) {
		return getParameterValue( getParameter( name ) );
	}

	@Override
	public Object getParameterValue(int position) {
		return getParameterValue( getParameter( position ) );
	}
}
