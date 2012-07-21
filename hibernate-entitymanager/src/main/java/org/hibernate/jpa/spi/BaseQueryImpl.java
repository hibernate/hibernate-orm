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

import javax.persistence.CacheRetrieveMode;
import javax.persistence.CacheStoreMode;
import javax.persistence.FlushModeType;
import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.QueryHints;
import org.hibernate.jpa.internal.EntityManagerMessageLogger;
import org.hibernate.jpa.internal.util.CacheModeHelper;
import org.hibernate.jpa.internal.util.ConfigurationHelper;
import org.hibernate.jpa.internal.util.LockModeTypeHelper;

import static org.hibernate.jpa.QueryHints.HINT_CACHEABLE;
import static org.hibernate.jpa.QueryHints.HINT_CACHE_MODE;
import static org.hibernate.jpa.QueryHints.HINT_CACHE_REGION;
import static org.hibernate.jpa.QueryHints.HINT_COMMENT;
import static org.hibernate.jpa.QueryHints.HINT_FETCH_SIZE;
import static org.hibernate.jpa.QueryHints.HINT_FLUSH_MODE;
import static org.hibernate.jpa.QueryHints.HINT_READONLY;
import static org.hibernate.jpa.QueryHints.HINT_TIMEOUT;
import static org.hibernate.jpa.QueryHints.SPEC_HINT_TIMEOUT;

/**
 * Intended as the base class for all {@link javax.persistence.Query} implementations, including {@link javax.persistence.TypedQuery} and
 * {@link javax.persistence.StoredProcedureQuery}.  Care should be taken that all changes here fit with all
 * those usages.
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


	public BaseQueryImpl(HibernateEntityManagerImplementor entityManager) {
		this.entityManager = entityManager;
	}

	protected HibernateEntityManagerImplementor entityManager() {
		return entityManager;
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

	/**
	 * Apply the given max results value.
	 *
	 * @param maxResults The specified max results
	 */
	protected abstract void applyMaxResults(int maxResults);

	@Override
	public BaseQueryImpl setMaxResults(int maxResult) {
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


	// Hints ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@SuppressWarnings( {"UnusedDeclaration"})
	public Set<String> getSupportedHints() {
		return QueryHints.getDefinedHints();
	}

	@Override
	public Map<String, Object> getHints() {
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
	protected abstract boolean applyFetchSize(int fetchSize);

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
	protected abstract boolean canApplyLockModesHints();

	/**
	 * Apply the alias specific lock modes.  Assumes {@link #canApplyLockModesHints()} has already been called and
	 * returned {@code true}.
	 *
	 * @param alias The alias to apply the 'lockMode' to.
	 * @param lockMode The LockMode to apply.
	 */
	protected abstract void applyAliasSpecificLockModeHint(String alias, LockMode lockMode);

	@Override
	@SuppressWarnings( {"deprecation"})
	public BaseQueryImpl setHint(String hintName, Object value) {
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
				applied = applyFetchSize( ConfigurationHelper.getInteger( value ) );
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
			else if ( hintName.startsWith( AvailableSettings.ALIAS_SPECIFIC_LOCK_MODE ) ) {
				if ( canApplyLockModesHints() ) {
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


	// FlushMode ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private FlushModeType jpaFlushMode;

	@Override
	public BaseQueryImpl setFlushMode(FlushModeType jpaFlushMode) {
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
		return jpaFlushMode != null
				? jpaFlushMode
				: entityManager.getFlushMode();
	}


	// Parameters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private List<ParameterImplementor> parameters;

	/**
	 * Hibernate specific extension to the JPA {@link javax.persistence.Parameter} contract.
	 */
	protected static interface ParameterImplementor<T> extends Parameter<T> {
		public boolean isBindable();

		public ParameterValue getBoundValue();
	}

	protected static class ParameterValue {
		private final Object value;
		private final TemporalType specifiedTemporalType;

		public ParameterValue(Object value, TemporalType specifiedTemporalType) {
			this.value = value;
			this.specifiedTemporalType = specifiedTemporalType;
		}

		public Object getValue() {
			return value;
		}

		public TemporalType getSpecifiedTemporalType() {
			return specifiedTemporalType;
		}
	}

	private Map<ParameterImplementor<?>,ParameterValue> parameterBindingMap;

	private Map<ParameterImplementor<?>,ParameterValue> parameterBindingMap() {
		if ( parameterBindingMap == null ) {
			parameterBindingMap = new HashMap<ParameterImplementor<?>, ParameterValue>();
		}
		return parameterBindingMap;
	}

	protected void registerParameter(ParameterImplementor parameter) {
		if ( parameter == null ) {
			throw new IllegalArgumentException( "parameter cannot be null" );
		}

		if ( parameterBindingMap().containsKey( parameter ) ) {
			return;
		}

		parameterBindingMap().put( parameter, null );
	}

	@SuppressWarnings("unchecked")
	protected void registerParameterBinding(Parameter parameter, ParameterValue bindValue) {
		validateParameterBinding( (ParameterImplementor) parameter, bindValue );
		parameterBindingMap().put( (ParameterImplementor) parameter, bindValue );
	}

	protected void validateParameterBinding(ParameterImplementor parameter, ParameterValue bindValue) {
		if ( parameter == null ) {
			throw new IllegalArgumentException( "parameter cannot be null" );
		}

		if ( ! parameter.isBindable() ) {
			throw new IllegalArgumentException( "Parameter [" + parameter + "] not valid for binding" );
		}

		if ( ! parameterBindingMap().containsKey( parameter ) ) {
			throw new IllegalArgumentException( "Unknown parameter [" + parameter + "] specified for value binding" );
		}

		if ( isBound( parameter ) ) {
			throw new IllegalArgumentException( "Parameter [" + parameter + "] already had bound value" );
		}

		validateParameterBindingTypes( parameter, bindValue );
	}

	protected abstract void validateParameterBindingTypes(ParameterImplementor parameter, ParameterValue bindValue);

	protected ParameterValue makeBindValue(Object value) {
		return new ParameterValue( value, null );
	}

	protected ParameterValue makeBindValue(Calendar value, TemporalType temporalType) {
		return new ParameterValue( value, temporalType );
	}

	protected ParameterValue makeBindValue(Date value, TemporalType temporalType) {
		return new ParameterValue( value, temporalType );
	}

	@Override
	public <T> BaseQueryImpl setParameter(Parameter<T> param, T value) {
		registerParameterBinding( param, makeBindValue( value ) );
		return this;
	}

	@Override
	public BaseQueryImpl setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		registerParameterBinding( param, makeBindValue( value, temporalType ) );
		return this;
	}

	@Override
	public BaseQueryImpl setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		registerParameterBinding( param, makeBindValue( value, temporalType ) );
		return this;
	}

	@Override
	public BaseQueryImpl setParameter(String name, Object value) {
		registerParameterBinding( getParameter( name ), makeBindValue( value ) );
		return this;
	}

	@Override
	public BaseQueryImpl setParameter(String name, Calendar value, TemporalType temporalType) {
		registerParameterBinding( getParameter( name ), makeBindValue( value, temporalType ) );
		return this;
	}

	@Override
	public BaseQueryImpl setParameter(String name, Date value, TemporalType temporalType) {
		registerParameterBinding( getParameter( name ), makeBindValue( value, temporalType ) );
		return this;
	}

	@Override
	public BaseQueryImpl setParameter(int position, Object value) {
		registerParameterBinding( getParameter( position ), makeBindValue( value ) );
		return this;
	}

	@Override
	public BaseQueryImpl setParameter(int position, Calendar value, TemporalType temporalType) {
		registerParameterBinding( getParameter( position ), makeBindValue( value, temporalType ) );
		return this;
	}

	@Override
	public BaseQueryImpl setParameter(int position, Date value, TemporalType temporalType) {
		registerParameterBinding( getParameter( position ), makeBindValue( value, temporalType ) );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set getParameters() {
		return parameterBindingMap().keySet();
	}

	@Override
	public Parameter<?> getParameter(String name) {
		if ( parameterBindingMap() != null ) {
			for ( ParameterImplementor<?> param : parameterBindingMap.keySet() ) {
				if ( name.equals( param.getName() ) ) {
					return param;
				}
			}
		}
		throw new IllegalArgumentException( "Parameter with that name [" + name + "] did not exist" );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Parameter<T> getParameter(String name, Class<T> type) {
		return (Parameter<T>) getParameter( name );
	}

	@Override
	public Parameter<?> getParameter(int position) {
		if ( parameterBindingMap() != null ) {
			for ( ParameterImplementor<?> param : parameterBindingMap.keySet() ) {
				if ( position == param.getPosition() ) {
					return param;
				}
			}
		}
		throw new IllegalArgumentException( "Parameter with that position [" + position + "] did not exist" );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Parameter<T> getParameter(int position, Class<T> type) {
		return (Parameter<T>) getParameter( position );
	}

	@Override
	public boolean isBound(Parameter<?> param) {
		return parameterBindingMap() != null
				&& parameterBindingMap.get( (ParameterImplementor) param ) != null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getParameterValue(Parameter<T> param) {
		if ( parameterBindingMap != null ) {
			final ParameterValue boundValue = parameterBindingMap.get( (ParameterImplementor) param );
			if ( boundValue != null ) {
				return (T) boundValue.getValue();
			}
		}
		throw new IllegalStateException( "Parameter [" + param + "] has not yet been bound" );
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
