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
package org.hibernate.jpa.spi;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.CacheRetrieveMode;
import javax.persistence.CacheStoreMode;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.TemporalType;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.QueryParameterException;
import org.hibernate.engine.query.spi.EntityGraphQueryHint;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.QueryHints;
import org.hibernate.jpa.graph.internal.EntityGraphImpl;
import org.hibernate.jpa.internal.EntityManagerMessageLogger;
import org.hibernate.jpa.internal.util.CacheModeHelper;
import org.hibernate.jpa.internal.util.ConfigurationHelper;
import org.hibernate.jpa.internal.util.LockModeTypeHelper;
import org.hibernate.procedure.NoSuchParameterException;
import org.hibernate.procedure.ParameterStrategyException;

import org.jboss.logging.Logger;

import static org.hibernate.jpa.QueryHints.HINT_CACHEABLE;
import static org.hibernate.jpa.QueryHints.HINT_CACHE_MODE;
import static org.hibernate.jpa.QueryHints.HINT_CACHE_REGION;
import static org.hibernate.jpa.QueryHints.HINT_COMMENT;
import static org.hibernate.jpa.QueryHints.HINT_FETCHGRAPH;
import static org.hibernate.jpa.QueryHints.HINT_FETCH_SIZE;
import static org.hibernate.jpa.QueryHints.HINT_FLUSH_MODE;
import static org.hibernate.jpa.QueryHints.HINT_LOADGRAPH;
import static org.hibernate.jpa.QueryHints.HINT_NATIVE_LOCKMODE;
import static org.hibernate.jpa.QueryHints.HINT_READONLY;
import static org.hibernate.jpa.QueryHints.HINT_TIMEOUT;
import static org.hibernate.jpa.QueryHints.SPEC_HINT_TIMEOUT;

/**
 * Intended as the base class for all {@link javax.persistence.Query} implementations, including
 * {@link javax.persistence.TypedQuery} and {@link javax.persistence.StoredProcedureQuery}.  Care should be taken
 * that all changes here fit with all those usages.
 *
 * @author Steve Ebersole
 */
public abstract class BaseQueryImpl implements Query {
	private static final EntityManagerMessageLogger LOG = Logger.getMessageLogger(
			EntityManagerMessageLogger.class,
			AbstractQueryImpl.class.getName()
	);

	private final HibernateEntityManagerImplementor entityManager;

	private int firstResult;
	private int maxResults = -1;
	private Map<String, Object> hints;

	private EntityGraphQueryHint entityGraphQueryHint;

	public BaseQueryImpl(HibernateEntityManagerImplementor entityManager) {
		this.entityManager = entityManager;
	}

	protected HibernateEntityManagerImplementor entityManager() {
		return entityManager;
	}

	protected void checkOpen(boolean markForRollbackIfClosed) {
		entityManager.checkOpen( markForRollbackIfClosed );
	}


	// Limits (first and max results) ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Apply the given first-result value.
	 *
	 * @param firstResult The specified first-result value.
	 */
	protected abstract void applyFirstResult(int firstResult);

	@Override
	public BaseQueryImpl setFirstResult(int firstResult) {
		checkOpen( true );

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
		checkOpen( false ); // technically should rollback
		return firstResult;
	}

	/**
	 * Apply the given max results value.
	 *
	 * @param maxResults The specified max results
	 */
	protected abstract void applyMaxResults(int maxResults);

	@Override
	public BaseQueryImpl setMaxResults(int maxResult) {
		checkOpen( true );
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
		checkOpen( false ); // technically should rollback
		return maxResults == -1
				? Integer.MAX_VALUE // stupid spec... MAX_VALUE??
				: maxResults;
	}


	// Hints ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@SuppressWarnings( {"UnusedDeclaration"})
	public Set<String> getSupportedHints() {
		return QueryHints.getDefinedHints();
	}

	@Override
	public Map<String, Object> getHints() {
		checkOpen( false ); // technically should rollback
		return hints;
	}

	/**
	 * Apply the query timeout hint.
	 *
	 * @param timeout The timeout (in seconds!) specified as a hint
	 *
	 * @return {@code true} if the hint was "applied"
	 */
	protected abstract boolean applyTimeoutHint(int timeout);

	/**
	 * Apply the lock timeout (in seconds!) hint
	 *
	 * @param timeout The timeout (in seconds!) specified as a hint
	 *
	 * @return {@code true} if the hint was "applied"
	 */
	protected abstract boolean applyLockTimeoutHint(int timeout);

	/**
	 * Apply the comment hint.
	 *
	 * @param comment The comment specified as a hint
	 *
	 * @return {@code true} if the hint was "applied"
	 */
	protected abstract boolean applyCommentHint(String comment);

	/**
	 * Apply the fetch size hint
	 *
	 * @param fetchSize The fetch size specified as a hint
	 *
	 * @return {@code true} if the hint was "applied"
	 */
	protected abstract boolean applyFetchSizeHint(int fetchSize);

	/**
	 * Apply the cacheable (true/false) hint.
	 *
	 * @param isCacheable The value specified as hint
	 *
	 * @return {@code true} if the hint was "applied"
	 */
	protected abstract boolean applyCacheableHint(boolean isCacheable);

	/**
	 * Apply the cache region hint
	 *
	 * @param regionName The name of the cache region specified as a hint
	 *
	 * @return {@code true} if the hint was "applied"
	 */
	protected abstract boolean applyCacheRegionHint(String regionName);

	/**
	 * Apply the read-only (true/false) hint.
	 *
	 * @param isReadOnly The value specified as hint
	 *
	 * @return {@code true} if the hint was "applied"
	 */
	protected abstract boolean applyReadOnlyHint(boolean isReadOnly);

	/**
	 * Apply the CacheMode hint.
	 *
	 * @param cacheMode The CacheMode value specified as a hint.
	 *
	 * @return {@code true} if the hint was "applied"
	 */
	protected abstract boolean applyCacheModeHint(CacheMode cacheMode);

	/**
	 * Apply the FlushMode hint.
	 *
	 * @param flushMode The FlushMode value specified as hint
	 *
	 * @return {@code true} if the hint was "applied"
	 */
	protected abstract boolean applyFlushModeHint(FlushMode flushMode);

	/**
	 * Can alias-specific lock modes be applied?
	 *
	 * @return {@code true} indicates they can be applied, {@code false} otherwise.
	 */
	protected abstract boolean canApplyAliasSpecificLockModeHints();

	/**
	 * Apply the alias specific lock modes.  Assumes {@link #canApplyAliasSpecificLockModeHints()} has already been
	 * called and returned {@code true}.
	 *
	 * @param alias The alias to apply the 'lockMode' to.
	 * @param lockMode The LockMode to apply.
	 */
	protected abstract void applyAliasSpecificLockModeHint(String alias, LockMode lockMode);

	@Override
	@SuppressWarnings( {"deprecation"})
	public BaseQueryImpl setHint(String hintName, Object value) {
		checkOpen( true );
		boolean applied = false;
		try {
			if ( HINT_TIMEOUT.equals( hintName ) ) {
				applied = applyTimeoutHint( ConfigurationHelper.getInteger( value ) );
			}
			else if ( SPEC_HINT_TIMEOUT.equals( hintName ) ) {
				// convert milliseconds to seconds
				int timeout = (int)Math.round(ConfigurationHelper.getInteger( value ).doubleValue() / 1000.0 );
				applied = applyTimeoutHint( timeout );
			}
			else if ( AvailableSettings.LOCK_TIMEOUT.equals( hintName ) ) {
				applied = applyLockTimeoutHint( ConfigurationHelper.getInteger( value ) );
			}
			else if ( HINT_COMMENT.equals( hintName ) ) {
				applied = applyCommentHint( (String) value );
			}
			else if ( HINT_FETCH_SIZE.equals( hintName ) ) {
				applied = applyFetchSizeHint( ConfigurationHelper.getInteger( value ) );
			}
			else if ( HINT_CACHEABLE.equals( hintName ) ) {
				applied = applyCacheableHint( ConfigurationHelper.getBoolean( value ) );
			}
			else if ( HINT_CACHE_REGION.equals( hintName ) ) {
				applied = applyCacheRegionHint( (String) value );
			}
			else if ( HINT_READONLY.equals( hintName ) ) {
				applied = applyReadOnlyHint( ConfigurationHelper.getBoolean( value ) );
			}
			else if ( HINT_CACHE_MODE.equals( hintName ) ) {
				applied = applyCacheModeHint( ConfigurationHelper.getCacheMode( value ) );
			}
			else if ( HINT_FLUSH_MODE.equals( hintName ) ) {
				applied = applyFlushModeHint( ConfigurationHelper.getFlushMode( value ) );
			}
			else if ( AvailableSettings.SHARED_CACHE_RETRIEVE_MODE.equals( hintName ) ) {
				final CacheRetrieveMode retrieveMode = (CacheRetrieveMode) value;

				CacheStoreMode storeMode = hints != null
						? (CacheStoreMode) hints.get( AvailableSettings.SHARED_CACHE_STORE_MODE )
						: null;
				if ( storeMode == null ) {
					storeMode = (CacheStoreMode) entityManager.getProperties().get( AvailableSettings.SHARED_CACHE_STORE_MODE );
				}
				applied = applyCacheModeHint( CacheModeHelper.interpretCacheMode( storeMode, retrieveMode ) );
			}
			else if ( AvailableSettings.SHARED_CACHE_STORE_MODE.equals( hintName ) ) {
				final CacheStoreMode storeMode = (CacheStoreMode) value;

				CacheRetrieveMode retrieveMode = hints != null
						? (CacheRetrieveMode) hints.get( AvailableSettings.SHARED_CACHE_RETRIEVE_MODE )
						: null;
				if ( retrieveMode == null ) {
					retrieveMode = (CacheRetrieveMode) entityManager.getProperties().get( AvailableSettings.SHARED_CACHE_RETRIEVE_MODE );
				}
				applied = applyCacheModeHint(
						CacheModeHelper.interpretCacheMode( storeMode, retrieveMode )
				);
			}
			else if ( QueryHints.HINT_NATIVE_LOCKMODE.equals( hintName ) ) {
				if ( !isNativeSqlQuery() ) {
					throw new IllegalStateException(
							"Illegal attempt to set lock mode on non-native query via hint; use Query#setLockMode instead"
					);
				}
				if ( LockMode.class.isInstance( value ) ) {
					internalApplyLockMode( LockModeTypeHelper.getLockModeType( (LockMode) value ) );
				}
				else if ( LockModeType.class.isInstance( value ) ) {
					internalApplyLockMode( (LockModeType) value );
				}
				else {
					throw new IllegalArgumentException(
							String.format(
									"Native lock-mode hint [%s] must specify %s or %s.  Encountered type : %s",
									HINT_NATIVE_LOCKMODE,
									LockMode.class.getName(),
									LockModeType.class.getName(),
									value.getClass().getName()
							)
					);
				}
				applied = true;
			}
			else if ( hintName.startsWith( AvailableSettings.ALIAS_SPECIFIC_LOCK_MODE ) ) {
				if ( canApplyAliasSpecificLockModeHints() ) {
					// extract the alias
					final String alias = hintName.substring( AvailableSettings.ALIAS_SPECIFIC_LOCK_MODE.length() + 1 );
					// determine the LockMode
					try {
						final LockMode lockMode = LockModeTypeHelper.interpretLockMode( value );
						applyAliasSpecificLockModeHint( alias, lockMode );
					}
					catch ( Exception e ) {
						LOG.unableToDetermineLockModeValue( hintName, value );
						applied = false;
					}
				}
				else {
					applied = false;
				}
			}
			else if ( HINT_FETCHGRAPH.equals( hintName ) || HINT_LOADGRAPH.equals( hintName ) ) {
				if (value instanceof EntityGraphImpl) {
					entityGraphQueryHint = new EntityGraphQueryHint( (EntityGraphImpl) value );
				}
				else {
					LOG.warnf( "The %s hint was set, but the value was not an EntityGraph!", hintName );
				}
				applied = true;
			}
			else {
				LOG.ignoringUnrecognizedQueryHint( hintName );
			}
		}
		catch ( ClassCastException e ) {
			throw new IllegalArgumentException( "Value for hint" );
		}

		if ( applied ) {
			if ( hints == null ) {
				hints = new HashMap<String,Object>();
			}
			hints.put( hintName, value );
		}
		else {
			LOG.debugf( "Skipping unsupported query hint [%s]", hintName );
		}

		return this;
	}

	/**
	 * Is the query represented here a native SQL query?
	 *
	 * @return {@code true} if it is a native SQL query; {@code false} otherwise
	 */
	protected abstract boolean isNativeSqlQuery();

	/**
	 * Is the query represented here a SELECT query?
	 *
	 * @return {@code true} if the query is a SELECT; {@code false} otherwise.
	 */
	protected abstract boolean isSelectQuery();

	protected abstract void internalApplyLockMode(javax.persistence.LockModeType lockModeType);

	// FlushMode ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private FlushModeType jpaFlushMode;

	@Override
	public BaseQueryImpl setFlushMode(FlushModeType jpaFlushMode) {
		checkOpen( true );
		this.jpaFlushMode = jpaFlushMode;
		// TODO : treat as hint?
		if ( jpaFlushMode == FlushModeType.AUTO ) {
			applyFlushModeHint( FlushMode.AUTO );
		}
		else if ( jpaFlushMode == FlushModeType.COMMIT ) {
			applyFlushModeHint( FlushMode.COMMIT );
		}
		return this;
	}

	@SuppressWarnings( {"UnusedDeclaration"})
	protected FlushModeType getSpecifiedFlushMode() {
		return jpaFlushMode;
	}

	@Override
	public FlushModeType getFlushMode() {
		checkOpen( false );
		return jpaFlushMode != null
				? jpaFlushMode
				: entityManager.getFlushMode();
	}


	// Parameters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private Set<ParameterRegistration<?>> parameterRegistrations;

	protected <X> ParameterRegistration<X> findParameterRegistration(Parameter<X> parameter) {
		if ( ParameterRegistration.class.isInstance( parameter ) ) {
			final ParameterRegistration<X> reg = (ParameterRegistration<X>) parameter;
			// validate the parameter source
			if ( reg.getQuery() != this ) {
				throw new IllegalArgumentException( "Passed Parameter was from different Query" );
			}
			return reg;
		}
		else {
			if ( parameter.getName() != null ) {
				return findParameterRegistration( parameter.getName() );
			}
			else if ( parameter.getPosition() != null ) {
				return findParameterRegistration( parameter.getPosition() );
			}
		}

		throw new IllegalArgumentException( "Unable to resolve incoming parameter [" + parameter + "] to registration" );
	}

	@SuppressWarnings("unchecked")
	protected <X> ParameterRegistration<X> findParameterRegistration(String parameterName) {
		final Integer jpaPositionalParameter = toNumberOrNull( parameterName );

		if ( parameterRegistrations != null ) {
			for ( ParameterRegistration<?> param : parameterRegistrations ) {
				if ( parameterName.equals( param.getName() ) ) {
					return (ParameterRegistration<X>) param;
				}

				// legacy allowance of the application to access the parameter using the position as a String
				if ( param.isJpaPositionalParameter() && jpaPositionalParameter != null ) {
					if ( jpaPositionalParameter.equals( param.getPosition() ) ) {
						LOG.deprecatedJpaPositionalParameterAccess( jpaPositionalParameter );
					}
					return (ParameterRegistration<X>) param;
				}
			}
		}
		throw new IllegalArgumentException( "Parameter with that name [" + parameterName + "] did not exist" );
	}

	private Integer toNumberOrNull(String parameterName) {
		try {
			return Integer.valueOf( parameterName );
		}
		catch (NumberFormatException e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	protected <X> ParameterRegistration<X> findParameterRegistration(int parameterPosition) {
		if ( parameterRegistrations != null ) {
			for ( ParameterRegistration<?> param : parameterRegistrations ) {
				if ( param.getPosition() == null ) {
					continue;
				}
				if ( parameterPosition == param.getPosition() ) {
					return (ParameterRegistration<X>) param;
				}
			}
		}
		throw new IllegalArgumentException( "Parameter with that position [" + parameterPosition + "] did not exist" );
	}

	protected static class ParameterBindImpl<T> implements ParameterBind<T> {
		private final T value;
		private final TemporalType specifiedTemporalType;

		public ParameterBindImpl(T value, TemporalType specifiedTemporalType) {
			this.value = value;
			this.specifiedTemporalType = specifiedTemporalType;
		}

		public T getValue() {
			return value;
		}

		public TemporalType getSpecifiedTemporalType() {
			return specifiedTemporalType;
		}
	}

	private Set<ParameterRegistration<?>> parameterRegistrations() {
		if ( parameterRegistrations == null ) {
			// todo : could se use an identity set here?
			parameterRegistrations = new HashSet<ParameterRegistration<?>>();
		}
		return parameterRegistrations;
	}

	protected void registerParameter(ParameterRegistration parameter) {
		if ( parameter == null ) {
			throw new IllegalArgumentException( "parameter cannot be null" );
		}

		if ( parameterRegistrations().contains( parameter ) ) {
			LOG.debug( "Parameter registered multiple times : " + parameter );
			return;
		}

		parameterRegistrations().add( parameter );
	}

	@Override
	public <T> BaseQueryImpl setParameter(Parameter<T> param, T value) {
		checkOpen( true );

		try {
			findParameterRegistration( param ).bindValue( value );
		}
		catch (QueryParameterException e) {
			entityManager().markForRollbackOnly();
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch (HibernateException he) {
			throw entityManager.convert( he );
		}

		return this;
	}

	@Override
	public BaseQueryImpl setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		checkOpen( true );

		try {
			findParameterRegistration( param ).bindValue( value, temporalType );
		}
		catch (QueryParameterException e) {
			entityManager().markForRollbackOnly();
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch (HibernateException he) {
			throw entityManager.convert( he );
		}

		return this;
	}

	@Override
	public BaseQueryImpl setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		checkOpen( true );

		try {
			findParameterRegistration( param ).bindValue( value, temporalType );
		}
		catch (QueryParameterException e) {
			entityManager().markForRollbackOnly();
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch (HibernateException he) {
			throw entityManager.convert( he );
		}

		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public BaseQueryImpl setParameter(String name, Object value) {
		checkOpen( true );

		try {
			findParameterRegistration( name ).bindValue( value );
		}
		catch (QueryParameterException e) {
			entityManager().markForRollbackOnly();
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch (HibernateException he) {
			throw entityManager.convert( he );
		}

		return this;
	}

	@Override
	public BaseQueryImpl setParameter(String name, Calendar value, TemporalType temporalType) {
		checkOpen( true );

		try {
			findParameterRegistration( name ).bindValue( value, temporalType );
		}
		catch (QueryParameterException e) {
			entityManager().markForRollbackOnly();
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch (HibernateException he) {
			throw entityManager.convert( he );
		}

		return this;
	}

	@Override
	public BaseQueryImpl setParameter(String name, Date value, TemporalType temporalType) {
		checkOpen( true );

		try {
			findParameterRegistration( name ).bindValue( value, temporalType );
		}
		catch (QueryParameterException e) {
			entityManager().markForRollbackOnly();
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch (HibernateException he) {
			throw entityManager.convert( he );
		}

		return this;
	}

	@Override
	public BaseQueryImpl setParameter(int position, Object value) {
		checkOpen( true );

		try {
			findParameterRegistration( position ).bindValue( value );
		}
		catch (QueryParameterException e) {
			entityManager().markForRollbackOnly();
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch (HibernateException he) {
			throw entityManager.convert( he );
		}

		return this;
	}

	@Override
	public BaseQueryImpl setParameter(int position, Calendar value, TemporalType temporalType) {
		checkOpen( true );

		try {
			findParameterRegistration( position ).bindValue( value, temporalType );
		}
		catch (QueryParameterException e) {
			entityManager().markForRollbackOnly();
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch (HibernateException he) {
			throw entityManager.convert( he );
		}

		return this;
	}

	@Override
	public BaseQueryImpl setParameter(int position, Date value, TemporalType temporalType) {
		checkOpen( true );

		try {
			findParameterRegistration( position ).bindValue( value, temporalType );
		}
		catch (ParameterStrategyException e) {
			entityManager().markForRollbackOnly();
			throw new IllegalArgumentException( "Invalid mix of named and positional parameters", e );
		}
		catch (NoSuchParameterException e) {
			entityManager().markForRollbackOnly();
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch (QueryParameterException e) {
			entityManager().markForRollbackOnly();
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch (HibernateException he) {
			throw entityManager.convert( he );
		}

		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set getParameters() {
		checkOpen( false );
		return parameterRegistrations();
	}

	@Override
	public Parameter<?> getParameter(String name) {
		checkOpen( false );
		return findParameterRegistration( name );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Parameter<T> getParameter(String name, Class<T> type) {
		checkOpen( false );
		Parameter param = findParameterRegistration( name );

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
		return (Parameter<T>) param;
	}

	@Override
	public Parameter<?> getParameter(int position) {
		checkOpen( false );
		return findParameterRegistration( position );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Parameter<T> getParameter(int position, Class<T> type) {
		checkOpen( false );

		Parameter param = findParameterRegistration( position );

		if ( param.getParameterType() != null ) {
			// we were able to determine the expected type during analysis, so validate it here
			if ( ! param.getParameterType().isAssignableFrom( type ) ) {
				throw new IllegalArgumentException(
						String.format(
								"Parameter type [%s] is not assignment compatible with requested type [%s] for parameter at position [%s]",
								param.getParameterType().getName(),
								type.getName(),
								position
						)
				);
			}
		}
		return (Parameter<T>) param;
	}

	@Override
	public boolean isBound(Parameter<?> param) {
		checkOpen( false );
		final ParameterRegistration registration = findParameterRegistration( param );
		return registration != null && registration.isBindable() && registration.getBind() != null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getParameterValue(Parameter<T> param) {
		checkOpen( false );

		final ParameterRegistration<T> registration = findParameterRegistration( param );
		if ( registration == null ) {
			throw new IllegalArgumentException( "Passed parameter [" + param + "] is not a (registered) parameter of this query" );
		}

		if ( ! registration.isBindable() ) {
			throw new IllegalStateException( "Passed parameter [" + param + "] is not bindable" );
		}

		final ParameterBind<T> bind = registration.getBind();
		if ( bind == null ) {
			throw new IllegalStateException( "Parameter [" + param + "] has not yet been bound" );
		}

		return bind.getValue();
	}

	@Override
	public Object getParameterValue(String name) {
		checkOpen( false );
		return getParameterValue( getParameter( name ) );
	}

	@Override
	public Object getParameterValue(int position) {
		checkOpen( false );
		return getParameterValue( getParameter( position ) );
	}
	
	protected EntityGraphQueryHint getEntityGraphQueryHint() {
		return entityGraphQueryHint;
	}

















	protected static void validateBinding(Class parameterType, Object bind, TemporalType temporalType) {
		if ( bind == null || parameterType == null ) {
			// nothing we can check
			return;
		}

		if ( Collection.class.isInstance( bind ) && ! Collection.class.isAssignableFrom( parameterType ) ) {
			// we have a collection passed in where we are expecting a non-collection.
			// 		NOTE : this can happen in Hibernate's notion of "parameter list" binding
			// 		NOTE2 : the case of a collection value and an expected collection (if that can even happen)
			//			will fall through to the main check.
			validateCollectionValuedParameterBinding( parameterType, (Collection) bind, temporalType );
		}
		else if ( bind.getClass().isArray() ) {
			validateArrayValuedParameterBinding( parameterType, bind, temporalType );
		}
		else {
			if ( ! isValidBindValue( parameterType, bind, temporalType ) ) {
				throw new IllegalArgumentException(
						String.format(
								"Parameter value [%s] did not match expected type [%s (%s)]",
								bind,
								parameterType.getName(),
								extractName( temporalType )
						)
				);
			}
		}
	}

	private static String extractName(TemporalType temporalType) {
		return temporalType == null ? "n/a" : temporalType.name();
	}

	private static void validateCollectionValuedParameterBinding(
			Class parameterType,
			Collection value,
			TemporalType temporalType) {
		// validate the elements...
		for ( Object element : value ) {
			if ( ! isValidBindValue( parameterType, element, temporalType ) ) {
				throw new IllegalArgumentException(
						String.format(
								"Parameter value element [%s] did not match expected type [%s (%s)]",
								element,
								parameterType.getName(),
								extractName( temporalType )
						)
				);
			}
		}
	}

	private static void validateArrayValuedParameterBinding(
			Class parameterType,
			Object value,
			TemporalType temporalType) {
		if ( ! parameterType.isArray() ) {
			throw new IllegalArgumentException(
					String.format(
							"Encountered array-valued parameter binding, but was expecting [%s (%s)]",
							parameterType.getName(),
							extractName( temporalType )
					)
			);
		}

		if ( value.getClass().getComponentType().isPrimitive() ) {
			// we have a primitive array.  we validate that the actual array has the component type (type of elements)
			// we expect based on the component type of the parameter specification
			if ( ! parameterType.getComponentType().isAssignableFrom( value.getClass().getComponentType() ) ) {
				throw new IllegalArgumentException(
						String.format(
								"Primitive array-valued parameter bind value type [%s] did not match expected type [%s (%s)]",
								value.getClass().getComponentType().getName(),
								parameterType.getName(),
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
				if ( ! isValidBindValue( parameterType.getComponentType(), element, temporalType ) ) {
					throw new IllegalArgumentException(
							String.format(
									"Array-valued parameter value element [%s] did not match expected type [%s (%s)]",
									element,
									parameterType.getName(),
									extractName( temporalType )
							)
					);
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




}
