/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NonUniqueResultException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.ScrollMode;
import org.hibernate.TypeMismatchException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.internal.EntityManagerMessageLogger;
import org.hibernate.internal.HEMLogging;
import org.hibernate.jpa.AvailableHints;
import org.hibernate.jpa.internal.util.ConfigurationHelper;
import org.hibernate.jpa.internal.util.FlushModeTypeHelper;
import org.hibernate.jpa.internal.util.LockModeTypeHelper;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.BindableType;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.QueryLogging;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.TypedParameterValue;
import org.hibernate.query.internal.ScrollableResultsIterator;
import org.hibernate.query.named.NamedQueryMemento;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;

import static org.hibernate.LockMode.UPGRADE;
import static org.hibernate.LockOptions.NONE;
import static org.hibernate.LockOptions.READ;
import static org.hibernate.LockOptions.WAIT_FOREVER;
import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;
import static org.hibernate.jpa.HibernateHints.HINT_CACHEABLE;
import static org.hibernate.jpa.HibernateHints.HINT_CACHE_MODE;
import static org.hibernate.jpa.HibernateHints.HINT_CACHE_REGION;
import static org.hibernate.jpa.HibernateHints.HINT_COMMENT;
import static org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE;
import static org.hibernate.jpa.HibernateHints.HINT_FLUSH_MODE;
import static org.hibernate.jpa.HibernateHints.HINT_FOLLOW_ON_LOCKING;
import static org.hibernate.jpa.HibernateHints.HINT_NATIVE_LOCK_MODE;
import static org.hibernate.jpa.HibernateHints.HINT_NATIVE_SPACES;
import static org.hibernate.jpa.HibernateHints.HINT_READ_ONLY;
import static org.hibernate.jpa.HibernateHints.HINT_TIMEOUT;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_CACHE_RETRIEVE_MODE;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_CACHE_STORE_MODE;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_FETCH_GRAPH;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_LOAD_GRAPH;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_LOCK_SCOPE;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_LOCK_TIMEOUT;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_QUERY_TIMEOUT;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_CACHE_RETRIEVE_MODE;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_CACHE_STORE_MODE;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_FETCH_GRAPH;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_LOAD_GRAPH;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_LOCK_SCOPE;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_LOCK_TIMEOUT;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_QUERY_TIMEOUT;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractQuery<R>
		extends AbstractCommonQueryContract
		implements QueryImplementor<R> {
	protected static final EntityManagerMessageLogger log = HEMLogging.messageLogger( AbstractQuery.class );

	public AbstractQuery(SharedSessionContractImplementor session) {
		super( session );
	}

	protected void applyOptions(NamedQueryMemento memento) {
		if ( memento.getHints() != null ) {
			memento.getHints().forEach( this::setHint );
		}

		if ( memento.getCacheable() != null ) {
			setCacheable( memento.getCacheable() );
		}

		if ( memento.getCacheRegion() != null ) {
			setCacheRegion( memento.getCacheRegion() );
		}

		if ( memento.getCacheMode() != null ) {
			setCacheMode( memento.getCacheMode() );
		}

		if ( memento.getFlushMode() != null ) {
			setHibernateFlushMode( memento.getFlushMode() );
		}

		if ( memento.getReadOnly() != null ) {
			setReadOnly( memento.getReadOnly() );
		}

		if ( memento.getTimeout() != null ) {
			setTimeout( memento.getTimeout() );
		}

		if ( memento.getFetchSize() != null ) {
			setFetchSize( memento.getFetchSize() );
		}

		if ( memento.getComment() != null ) {
			setComment( memento.getComment() );
		}
	}

	protected abstract QueryParameterBindings getQueryParameterBindings();

	@Override
	public abstract ParameterMetadataImplementor getParameterMetadata();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// QueryOptions handling

	@Override
	public MutableQueryOptions getQueryOptions() {
		return super.getQueryOptions();
	}


	@Override
	public int getMaxResults() {
		getSession().checkOpen();
		return getQueryOptions().getLimit().getMaxRowsJpa();
	}

	@Override
	public QueryImplementor<R> setMaxResults(int maxResult) {
		if ( maxResult < 0 ) {
			throw new IllegalArgumentException( "max-results cannot be negative" );
		}

		getSession().checkOpen();

		getQueryOptions().getLimit().setMaxRows( maxResult );

		return this;
	}

	@Override
	public int getFirstResult() {
		getSession().checkOpen();
		return getQueryOptions().getLimit().getFirstRowJpa();
	}

	@Override
	public QueryImplementor<R> setFirstResult(int startPosition) {
		getSession().checkOpen();

		if ( startPosition < 0 ) {
			throw new IllegalArgumentException( "first-result value cannot be negative : " + startPosition );
		}

		getQueryOptions().getLimit().setFirstRow( startPosition );

		return this;
	}

	@Override @SuppressWarnings("unchecked")
	public <T> QueryImplementor<T> setTupleTransformer(TupleTransformer<T> transformer) {
		getQueryOptions().setTupleTransformer( transformer );
		// this is bad, we should really return a new instance:
		return (QueryImplementor<T>) this;
	}

	@Override
	public QueryImplementor<R> setResultListTransformer(ResultListTransformer<R> transformer) {
		getQueryOptions().setResultListTransformer( transformer );
		return this;
	}

	@Override
	public QueryImplementor<R> setHibernateFlushMode(FlushMode flushMode) {
		super.setHibernateFlushMode( flushMode );
		return this;
	}

	@Override
	public FlushModeType getFlushMode() {
		getSession().checkOpen();
		final FlushMode flushMode = getQueryOptions().getFlushMode() == null
				? getSession().getHibernateFlushMode()
				: getQueryOptions().getFlushMode();
		return FlushModeTypeHelper.getFlushModeType( flushMode );
	}

	@Override
	public QueryImplementor<R> setFlushMode(FlushModeType flushModeType) {
		getSession().checkOpen();
		setHibernateFlushMode( FlushModeTypeHelper.getFlushMode( flushModeType ) );
		return this;
	}

	@Override
	public QueryImplementor<R> setCacheMode(CacheMode cacheMode) {
		super.setCacheMode( cacheMode );
		return this;
	}

	@Override
	public boolean isCacheable() {
		return super.isCacheable();
	}

	@Override
	public QueryImplementor<R> setCacheable(boolean cacheable) {
		super.setCacheable( cacheable );
		return this;
	}

	@Override
	public QueryImplementor<R> setCacheRegion(String cacheRegion) {
		super.setCacheRegion( cacheRegion );
		return this;
	}

	@Override
	public QueryImplementor<R> setTimeout(int timeout) {
		super.setTimeout( timeout );
		return this;
	}

	@Override
	public QueryImplementor<R> setFetchSize(int fetchSize) {
		super.setFetchSize( fetchSize );
		return this;
	}

	@Override
	public QueryImplementor<R> setReadOnly(boolean readOnly) {
		super.setReadOnly( readOnly );
		return this;
	}

	@Override
	public LockOptions getLockOptions() {
		return getQueryOptions().getLockOptions();
	}

	@Override
	public LockModeType getLockMode() {
		getSession().checkOpen( false );

		return LockModeTypeHelper.getLockModeType( getQueryOptions().getLockOptions().getLockMode() );
	}

	@Override
	public QueryImplementor<R> setLockOptions(LockOptions lockOptions) {
		getQueryOptions().getLockOptions().setLockMode( lockOptions.getLockMode() );
		getQueryOptions().getLockOptions().setScope( lockOptions.getScope() );
		getQueryOptions().getLockOptions().setTimeOut( lockOptions.getTimeOut() );
		getQueryOptions().getLockOptions().setFollowOnLocking( lockOptions.getFollowOnLocking() );
		return this;
	}

	@Override
	public QueryImplementor<R> setLockMode(String alias, LockMode lockMode) {
		getQueryOptions().getLockOptions().setAliasSpecificLockMode( alias, lockMode );
		return this;
	}

	@Override
	public QueryImplementor<R> setLockMode(LockModeType lockModeType) {
		getSession().checkOpen();
		getQueryOptions().getLockOptions().setLockMode( LockModeTypeHelper.getLockMode( lockModeType ) );
		return this;
	}

	@Override
	public String getComment() {
		return getQueryOptions().getComment();
	}

	@Override
	public QueryImplementor<R> setComment(String comment) {
		getQueryOptions().setComment( comment );
		return this;
	}

	@Override
	public QueryImplementor<R> addQueryHint(String hint) {
		getQueryOptions().addDatabaseHint( hint );
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA hint handling


	@SuppressWarnings( {"UnusedDeclaration"})
	public Set<String> getSupportedHints() {
		return AvailableHints.getDefinedHints();
	}

	@Override
	public Map<String, Object> getHints() {
		// Technically this should rollback, but that's insane :)
		// If the TCK ever adds a check for this, we may need to change this behavior
		getSession().checkOpen( false );

		final Map<String,Object> hints = new HashMap<>();
		collectHints( hints );
		return hints;
	}

	@SuppressWarnings("deprecation")
	protected void collectHints(Map<String, Object> hints) {
		if ( getQueryOptions().getTimeout() != null ) {
			hints.put( HINT_TIMEOUT, getQueryOptions().getTimeout() );
			hints.put( HINT_SPEC_QUERY_TIMEOUT, getQueryOptions().getTimeout() * 1000 );
			hints.put( HINT_JAVAEE_QUERY_TIMEOUT, getQueryOptions().getTimeout() * 1000 );
		}

		if ( getLockOptions().getTimeOut() != WAIT_FOREVER ) {
			hints.put( HINT_SPEC_LOCK_TIMEOUT, getLockOptions().getTimeOut() );
			hints.put( HINT_JAVAEE_LOCK_TIMEOUT, getLockOptions().getTimeOut() );
		}

		if ( getLockOptions().getScope() ) {
			hints.put( HINT_SPEC_LOCK_SCOPE, getLockOptions().getScope() );
			hints.put( HINT_JAVAEE_LOCK_SCOPE, getLockOptions().getScope() );
		}

		if ( getLockOptions().hasAliasSpecificLockModes() ) {
			for ( Map.Entry<String, LockMode> entry : getLockOptions().getAliasSpecificLocks() ) {
				hints.put(
						HINT_NATIVE_LOCK_MODE + '.' + entry.getKey(),
						entry.getValue().name()
				);
			}
		}

		putIfNotNull( hints, HINT_COMMENT, getComment() );
		putIfNotNull( hints, HINT_FETCH_SIZE, getQueryOptions().getFetchSize() );
		putIfNotNull( hints, HINT_FLUSH_MODE, getHibernateFlushMode() );

		if ( getCacheMode() != null ) {
			putIfNotNull( hints, HINT_CACHE_MODE, getCacheMode() );
			putIfNotNull( hints, HINT_SPEC_CACHE_RETRIEVE_MODE, getQueryOptions().getCacheRetrieveMode() );
			putIfNotNull( hints, HINT_SPEC_CACHE_STORE_MODE, getQueryOptions().getCacheStoreMode() );
			putIfNotNull( hints, HINT_JAVAEE_CACHE_RETRIEVE_MODE, getQueryOptions().getCacheRetrieveMode() );
			putIfNotNull( hints, HINT_JAVAEE_CACHE_STORE_MODE, getQueryOptions().getCacheStoreMode() );
		}

		if ( isCacheable() ) {
			hints.put( HINT_CACHEABLE, true );
			putIfNotNull( hints, HINT_CACHE_REGION, getCacheRegion() );
		}

		if ( isReadOnly() ) {
			hints.put( HINT_READ_ONLY, true );
		}
	}

	protected void putIfNotNull(Map<String, Object> hints, String hintName, Enum<?> hintValue) {
		// centralized spot to handle the decision whether to put enums directly into the hints map
		// or whether to put the enum name
		if ( hintValue != null ) {
			hints.put( hintName, hintValue );
//			hints.put( hintName, hintValue.name() );
		}
	}

	protected void putIfNotNull(Map<String, Object> hints, String hintName, Object hintValue) {
		if ( hintValue != null ) {
			hints.put( hintName, hintValue );
		}
	}

	@Override @SuppressWarnings("deprecation")
	public QueryImplementor<R> setHint(String hintName, Object value) {
		getSession().checkOpen( true );

		boolean applied = false;
		try {
			if ( HINT_TIMEOUT.equals( hintName ) ) {
				applied = applyTimeout( ConfigurationHelper.getInteger( value ) );
			}
			else if ( HINT_SPEC_QUERY_TIMEOUT.equals( hintName )
					|| HINT_JAVAEE_QUERY_TIMEOUT.equals( hintName ) ) {
				if ( HINT_JAVAEE_QUERY_TIMEOUT.equals( hintName ) ) {
					DEPRECATION_LOGGER.deprecatedSetting( HINT_JAVAEE_QUERY_TIMEOUT, HINT_SPEC_QUERY_TIMEOUT );
				}
				// convert milliseconds to seconds
				int timeout = (int)Math.round( ConfigurationHelper.getInteger( value ).doubleValue() / 1000.0 );
				applied = applyTimeout( timeout );
			}
			else if ( HINT_SPEC_LOCK_TIMEOUT.equals( hintName )
					|| HINT_JAVAEE_LOCK_TIMEOUT.equals( hintName ) ) {
				if ( HINT_JAVAEE_LOCK_TIMEOUT.equals( hintName ) ) {
					DEPRECATION_LOGGER.deprecatedSetting( HINT_JAVAEE_LOCK_TIMEOUT, HINT_SPEC_LOCK_TIMEOUT );
				}
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
			else if ( HINT_READ_ONLY.equals( hintName ) ) {
				applied = applyReadOnlyHint( ConfigurationHelper.getBoolean( value ) );
			}
			else if ( HINT_FLUSH_MODE.equals( hintName ) ) {
				applied = applyFlushModeHint( ConfigurationHelper.getFlushMode( value ) );
			}
			else if ( HINT_CACHE_MODE.equals( hintName ) ) {
				applied = applyCacheModeHint( ConfigurationHelper.getCacheMode( value ) );
			}
			else if ( HINT_SPEC_CACHE_RETRIEVE_MODE.equals( hintName )
					|| HINT_JAVAEE_CACHE_RETRIEVE_MODE.equals( hintName ) ) {
				if ( HINT_JAVAEE_CACHE_RETRIEVE_MODE.equals( hintName ) ) {
					DEPRECATION_LOGGER.deprecatedSetting( HINT_JAVAEE_CACHE_RETRIEVE_MODE, HINT_SPEC_CACHE_RETRIEVE_MODE );
				}
				final CacheRetrieveMode retrieveMode = value != null ? CacheRetrieveMode.valueOf( value.toString() ) : null;
				applied = applyJpaCacheRetrieveMode( retrieveMode );
			}
			else if ( HINT_SPEC_CACHE_STORE_MODE.equals( hintName )
					|| HINT_JAVAEE_CACHE_STORE_MODE.equals( hintName ) ) {
				if ( HINT_JAVAEE_CACHE_STORE_MODE.equals( hintName ) ) {
					DEPRECATION_LOGGER.deprecatedSetting( HINT_JAVAEE_CACHE_STORE_MODE, HINT_SPEC_CACHE_STORE_MODE );
				}
				final CacheStoreMode storeMode = value != null ? CacheStoreMode.valueOf( value.toString() ) : null;
				applied = applyJpaCacheStoreMode( storeMode );
			}
			else if ( HINT_NATIVE_LOCK_MODE.equals( hintName ) ) {
				applied = applyNativeQueryLockMode( value );
			}
			else if ( hintName.startsWith( HINT_NATIVE_LOCK_MODE ) ) {
				// extract the alias
				final String alias = hintName.substring( HINT_NATIVE_LOCK_MODE.length() + 1 );
				// determine the LockMode
				try {
					final LockMode lockMode = LockModeTypeHelper.interpretLockMode( value );
					applyAliasSpecificLockModeHint( alias, lockMode );
					applied = true;
				}
				catch ( Exception e ) {
					log.unableToDetermineLockModeValue( hintName, value );
					applied = false;
				}
			}
			else if ( HINT_SPEC_FETCH_GRAPH.equals( hintName ) || HINT_SPEC_LOAD_GRAPH.equals( hintName ) ) {
				if ( value instanceof RootGraphImplementor ) {
					applyEntityGraphQueryHint( hintName, (RootGraphImplementor<?>) value );
				}
				else {
					// https://hibernate.atlassian.net/browse/HHH-14855 - accepting a String parseable
					// via the Graph Language parser here would be a nice feature
					log.warnf( "The %s hint was set, but the value was not an EntityGraph!", hintName );
				}
				applied = true;
			}
			else if ( HINT_JAVAEE_FETCH_GRAPH.equals( hintName ) || HINT_JAVAEE_LOAD_GRAPH.equals( hintName ) ) {
				if ( HINT_JAVAEE_FETCH_GRAPH.equals( hintName ) ) {
					DEPRECATION_LOGGER.deprecatedSetting( HINT_JAVAEE_FETCH_GRAPH, HINT_SPEC_FETCH_GRAPH );
				}
				else {
					DEPRECATION_LOGGER.deprecatedSetting( HINT_JAVAEE_LOAD_GRAPH, HINT_SPEC_LOAD_GRAPH );
				}

				if ( value instanceof RootGraphImplementor ) {
					applyEntityGraphQueryHint( hintName, (RootGraphImplementor<?>) value );
				}
				else {
					// https://hibernate.atlassian.net/browse/HHH-14855 - accepting a String parseable
					// via the Graph Language parser here would be a nice feature
					log.warnf( "The %s hint was set, but the value was not an EntityGraph!", hintName );
				}
				applied = true;
			}
			else if ( HINT_FOLLOW_ON_LOCKING.equals( hintName ) ) {
				applied = applyFollowOnLockingHint( ConfigurationHelper.getBoolean( value ) );
			}
			else if ( HINT_NATIVE_SPACES.equals( hintName ) ) {
				applied = applySynchronizeSpacesHint( value );
			}
			else {
				log.ignoringUnrecognizedQueryHint( hintName );
			}
		}
		catch ( ClassCastException e ) {
			throw new IllegalArgumentException( "Value for hint" );
		}

		if ( !applied ) {
			log.debugf( "Skipping unsupported query hint [%s]", hintName );
		}

		return this;
	}

	@SuppressWarnings("WeakerAccess")
	protected boolean applyJpaCacheRetrieveMode(CacheRetrieveMode retrieveMode) {
		getQueryOptions().setCacheRetrieveMode( retrieveMode );
		return true;
	}

	@SuppressWarnings("WeakerAccess")
	protected boolean applyJpaCacheStoreMode(CacheStoreMode storeMode) {
		getQueryOptions().setCacheStoreMode( storeMode );
		return true;
	}

	protected boolean applyNativeQueryLockMode(Object value) {
		return false;
	}

	protected boolean applySynchronizeSpacesHint(Object value) {
		return false;
	}

	/**
	 * Apply the query timeout hint.
	 *
	 * @param timeout The timeout (in seconds!) specified as a hint
	 *
	 * @return {@code true} if the hint was "applied"
	 */
	@SuppressWarnings("WeakerAccess")
	protected boolean applyTimeout(int timeout) {
		setTimeout( timeout );
		return true;
	}

	/**
	 * Apply the lock timeout (in seconds!) hint
	 *
	 * @param timeout The timeout (in seconds!) specified as a hint
	 *
	 * @return {@code true} if the hint was "applied"
	 */
	@SuppressWarnings("WeakerAccess")
	protected boolean applyLockTimeoutHint(int timeout) {
		getLockOptions().setTimeOut( timeout );
		return true;
	}

	/**
	 * Apply the comment hint.
	 *
	 * @param comment The comment specified as a hint
	 *
	 * @return {@code true} if the hint was "applied"
	 */
	@SuppressWarnings("WeakerAccess")
	protected boolean applyCommentHint(String comment) {
		setComment( comment );
		return true;
	}

	/**
	 * Apply the fetch size hint
	 *
	 * @param fetchSize The fetch size specified as a hint
	 *
	 * @return {@code true} if the hint was "applied"
	 */
	@SuppressWarnings("WeakerAccess")
	protected boolean applyFetchSizeHint(int fetchSize) {
		setFetchSize( fetchSize );
		return true;
	}

	/**
	 * Apply the cacheable (true/false) hint.
	 *
	 * @param isCacheable The value specified as hint
	 *
	 * @return {@code true} if the hint was "applied"
	 */
	@SuppressWarnings("WeakerAccess")
	protected boolean applyCacheableHint(boolean isCacheable) {
		setCacheable( isCacheable );
		return true;
	}

	/**
	 * Apply the cache region hint
	 *
	 * @param regionName The name of the cache region specified as a hint
	 *
	 * @return {@code true} if the hint was "applied"
	 */
	@SuppressWarnings("WeakerAccess")
	protected boolean applyCacheRegionHint(String regionName) {
		setCacheRegion( regionName );
		return true;
	}

	/**
	 * Apply the read-only (true/false) hint.
	 *
	 * @param isReadOnly The value specified as hint
	 *
	 * @return {@code true} if the hint was "applied"
	 */
	@SuppressWarnings("WeakerAccess")
	protected boolean applyReadOnlyHint(boolean isReadOnly) {
		setReadOnly( isReadOnly );
		return true;
	}

	/**
	 * Apply the CacheMode hint.
	 *
	 * @param cacheMode The CacheMode value specified as a hint.
	 *
	 * @return {@code true} if the hint was "applied"
	 */
	@SuppressWarnings("WeakerAccess")
	protected boolean applyCacheModeHint(CacheMode cacheMode) {
		setCacheMode( cacheMode );
		return true;
	}

	/**
	 * Apply the FlushMode hint.
	 *
	 * @param flushMode The FlushMode value specified as hint
	 *
	 * @return {@code true} if the hint was "applied"
	 */
	@SuppressWarnings("WeakerAccess")
	protected boolean applyFlushModeHint(FlushMode flushMode) {
		setHibernateFlushMode( flushMode );
		return true;
	}

	@SuppressWarnings( "UnusedReturnValue" )
	protected boolean applyLockModeTypeHint(LockModeType lockModeType) {
		getLockOptions().setLockMode( LockModeTypeHelper.getLockMode( lockModeType ) );
		return true;
	}

	@SuppressWarnings({"UnusedReturnValue", "deprecation"})
	protected boolean applyHibernateLockModeHint(LockMode lockMode) {
		//TODO: this method is a noop. Delete it?
		final LockOptions lockOptions;
		if ( lockMode == LockMode.NONE ) {
			lockOptions = NONE;
		}
		else if ( lockMode == LockMode.READ ) {
			lockOptions = READ;
		}
		else if ( lockMode == UPGRADE || lockMode == LockMode.PESSIMISTIC_WRITE ) {
			lockOptions = LockOptions.UPGRADE;
		}

		return true;
	}

	@SuppressWarnings("WeakerAccess")
	protected void applyAliasSpecificLockModeHint(String alias, LockMode lockMode) {
		setLockMode( alias, lockMode );
	}

	protected abstract void applyEntityGraphQueryHint(String hintName, RootGraphImplementor<?> entityGraph);

	/**
	 * Apply the follow-on-locking hint.
	 *
	 * @param followOnLocking The follow-on-locking strategy.
	 */
	@SuppressWarnings("WeakerAccess")
	protected boolean applyFollowOnLockingHint(Boolean followOnLocking) {
		getLockOptions().setFollowOnLocking( followOnLocking );
		return true;
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// QueryParameter handling

	protected boolean resolveJdbcParameterTypeIfNecessary() {
		return true;
	}

	@Override
	@SuppressWarnings( {"unchecked", "rawtypes"} )
	public Set<Parameter<?>> getParameters() {
		getSession().checkOpen( false );
		return (Set) getParameterMetadata().getRegistrations();
	}

	@Override
	public QueryParameterImplementor<?> getParameter(String name) {
		getSession().checkOpen( false );

		try {
			return getParameterMetadata().getQueryParameter( name );
		}
		catch ( HibernateException e ) {
			throw getSession().getExceptionConverter().convert( e );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> QueryParameterImplementor<T> getParameter(String name, Class<T> type) {
		getSession().checkOpen( false );

		try {
			//noinspection rawtypes
			final QueryParameterImplementor parameter = getParameterMetadata().getQueryParameter( name );
			if ( !parameter.getParameterType().isAssignableFrom( type ) ) {
				throw new IllegalArgumentException(
						"The type [" + parameter.getParameterType().getName() +
								"] associated with the parameter corresponding to name [" + name +
								"] is not assignable to requested Java type [" + type.getName() + "]"
				);
			}
			return parameter;
		}
		catch ( HibernateException e ) {
			throw getSession().getExceptionConverter().convert( e );
		}
	}

	@Override
	public QueryParameterImplementor<?> getParameter(int position) {
		getSession().checkOpen( false );

		try {
			return getParameterMetadata().getQueryParameter( position );
		}
		catch ( HibernateException e ) {
			throw getSession().getExceptionConverter().convert( e );
		}
	}

	@Override
	@SuppressWarnings( {"unchecked", "rawtypes"} )
	public <T> QueryParameterImplementor<T> getParameter(int position, Class<T> type) {
		getSession().checkOpen( false );

		try {
			final QueryParameterImplementor parameter = getParameterMetadata().getQueryParameter( position );
			if ( !parameter.getParameterType().isAssignableFrom( type ) ) {
				throw new IllegalArgumentException(
						"The type [" + parameter.getParameterType().getName() +
								"] associated with the parameter corresponding to position [" + position +
								"] is not assignable to requested Java type [" + type.getName() + "]"
				);
			}
			return parameter;
		}
		catch ( HibernateException e ) {
			throw getSession().getExceptionConverter().convert( e );
		}
	}

	@Override
	public <T> T getParameterValue(Parameter<T> param) {
		QueryLogging.QUERY_LOGGER.tracef( "#getParameterValue(%s)", param );

		getSession().checkOpen( false );

		final QueryParameterImplementor<T> qp = getParameterMetadata().resolve( param );
		if ( qp == null ) {
			throw new IllegalArgumentException( "The parameter [" + param + "] is not part of this Query" );
		}

		final QueryParameterBinding<T> binding = getQueryParameterBindings().getBinding( qp );
		if ( binding == null || !binding.isBound() ) {
			throw new IllegalStateException( "Parameter value not yet bound : " + param.toString() );
		}

		if ( binding.isMultiValued() ) {
			//noinspection unchecked
			return (T) binding.getBindValues();
		}
		else {
			return binding.getBindValue();
		}
	}

	@Override
	public Object getParameterValue(String name) {
		getSession().checkOpen( false );

		final QueryParameterImplementor<?> parameter = getParameterMetadata().getQueryParameter( name );
		if ( parameter == null ) {
			throw new IllegalArgumentException( "Could not resolve parameter by name - " + name );
		}

		final QueryParameterBinding<?> binding = getQueryParameterBindings().getBinding( parameter );
		if ( binding == null || !binding.isBound() ) {
			throw new IllegalStateException( "Parameter value not yet bound : " + parameter );
		}

		if ( binding.isMultiValued() ) {
			return binding.getBindValues();
		}
		else {
			return binding.getBindValue();
		}
	}

	@Override
	public Object getParameterValue(int position) {
		final QueryParameterImplementor<?> parameter = getParameterMetadata().getQueryParameter( position );
		if ( parameter == null ) {
			throw new IllegalArgumentException( "Could not resolve parameter by position - " + position );
		}

		final QueryParameterBinding<?> binding = getQueryParameterBindings().getBinding( parameter );
		if ( binding == null || !binding.isBound() ) {
			throw new IllegalStateException( "The parameter [" + position + "] has not yet been bound" );
		}

		if ( binding.isMultiValued() ) {
			return binding.getBindValues();
		}
		else {
			return binding.getBindValue();
		}
	}

	@Override
	public boolean isBound(Parameter<?> param) {
		getSession().checkOpen();

		final QueryParameterImplementor<?> qp = getParameterMetadata().resolve( param );
		return qp != null && getQueryParameterBindings().isBound( qp );
	}

	@SuppressWarnings( {"WeakerAccess", "unchecked", "rawtypes"} )
	protected <P> QueryParameterBinding<P> locateBinding(Parameter<P> parameter) {
		if ( parameter instanceof QueryParameterImplementor ) {
			return locateBinding( (QueryParameterImplementor) parameter );
		}
		else if ( parameter.getName() != null ) {
			return locateBinding( parameter.getName() );
		}
		else if ( parameter.getPosition() != null ) {
			return locateBinding( parameter.getPosition() );
		}

		throw getSession().getExceptionConverter().convert(
				new IllegalArgumentException( "Could not resolve binding for given parameter reference [" + parameter + "]" )
		);
	}

	@SuppressWarnings("WeakerAccess")
	protected <P> QueryParameterBinding<P> locateBinding(QueryParameterImplementor<P> parameter) {
		getSession().checkOpen();
		return getQueryParameterBindings().getBinding( parameter );
	}

	@SuppressWarnings( {"WeakerAccess"} )
	protected <P> QueryParameterBinding<P> locateBinding(String name) {
		getSession().checkOpen();
		return getQueryParameterBindings().getBinding( name );
	}

	@SuppressWarnings( {"WeakerAccess"} )
	protected <P> QueryParameterBinding<P> locateBinding(int position) {
		getSession().checkOpen();
		return getQueryParameterBindings().getBinding( position );
	}















	@Override
	public QueryImplementor<R> setParameter(String name, Object value) {
		if ( value instanceof TypedParameterValue ) {
			@SuppressWarnings("unchecked")
			final TypedParameterValue<Object> typedValue = (TypedParameterValue<Object>) value;
			final BindableType<Object> type = typedValue.getType();
			if ( type != null ) {
				return setParameter( name, typedValue.getValue(), type );
			}
			else {
				return setParameter( name, typedValue.getValue(), typedValue.getTypeReference() );
			}
		}

		final QueryParameterImplementor<?> param = getParameterMetadata().getQueryParameter( name );

		if ( param == null ) {
			throw new IllegalArgumentException( "Named parameter [" + name + "] is not registered with this procedure call" );
		}

		if ( param.allowsMultiValuedBinding() ) {
			final BindableType<?> hibernateType = param.getHibernateType();
			if ( hibernateType == null || isInstance( hibernateType, value ) ) {
				if ( value instanceof Collection ) {
					//noinspection rawtypes
					setParameterList( name, (Collection) value );
				}
			}
		}

		locateBinding( name ).setBindValue( value, resolveJdbcParameterTypeIfNecessary() );

		return this;
	}

	private boolean isInstance(BindableType<?> parameterType, Object value) {
		final SqmExpressable<?> sqmExpressable = parameterType.resolveExpressable( getSession().getFactory() );
		assert sqmExpressable != null;

		return sqmExpressable.getExpressableJavaType().isInstance( value );
	}

	@Override
	public <P> QueryImplementor<R> setParameter(String name, P value, Class<P> javaTypeClass) {
		final JavaType<P> javaType = getSession().getFactory()
				.getTypeConfiguration()
				.getJavaTypeRegistry()
				.getDescriptor( javaTypeClass );
		if ( javaType == null ) {
			setParameter( name, value );
		}
		else {
			final BindableType<P> paramType;
			final BasicType<P> basicType = getSession().getFactory().getTypeConfiguration().standardBasicTypeForJavaType( javaTypeClass );
			if ( basicType != null ) {
				paramType = basicType;
			}
			else {
				final ManagedDomainType<P> managedDomainType = getSession().getFactory()
						.getRuntimeMetamodels()
						.getJpaMetamodel()
						.managedType( javaTypeClass );
				if ( managedDomainType != null ) {
					paramType = managedDomainType;
				}
				else {
					throw new HibernateException( "Unable to determine BindableType : " + javaTypeClass.getName() );
				}
			}

			setParameter( name, value, paramType );
		}

		return this;
	}

	@Override
	public <P> QueryImplementor<R> setParameter(String name, P value, BindableType<P> type) {
		this.<P>locateBinding( name ).setBindValue( value, type );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameter(String name, Instant value, TemporalType temporalType) {
		this.locateBinding( name ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameter(int position, Object value) {
		if ( value instanceof TypedParameterValue ) {
			@SuppressWarnings("unchecked")
			final TypedParameterValue<Object> typedValue = (TypedParameterValue<Object>) value;
			final BindableType<Object> type = typedValue.getType();
			if ( type != null ) {
				return setParameter( position, typedValue.getValue(), type );
			}
			else {
				return setParameter( position, typedValue.getValue(), typedValue.getTypeReference() );
			}
		}

		final QueryParameterImplementor<?> param = getParameterMetadata().getQueryParameter( position );

		if ( param == null ) {
			throw new IllegalArgumentException( "Positional parameter [" + position + "] is not registered with this procedure call" );
		}

		if ( param.allowsMultiValuedBinding() ) {
			final BindableType<?> hibernateType = param.getHibernateType();
			if ( hibernateType == null || isInstance( hibernateType, value ) ) {
				if ( value instanceof Collection ) {
					//noinspection rawtypes,unchecked
					setParameterList( param, (Collection) value );
				}
			}
		}

		locateBinding( position ).setBindValue( value, resolveJdbcParameterTypeIfNecessary() );
		return this;
	}

	@Override
	public <P> QueryImplementor<R> setParameter(int position, P value, Class<P> javaTypeClass) {
		final JavaType<P> javaType = getSession().getFactory()
				.getTypeConfiguration()
				.getJavaTypeRegistry()
				.getDescriptor( javaTypeClass );
		if ( javaType == null ) {
			setParameter( position, value );
		}
		else {
			final BindableType<P> paramType;
			final BasicType<P> basicType = getSession().getFactory().getTypeConfiguration().standardBasicTypeForJavaType( javaTypeClass );
			if ( basicType != null ) {
				paramType = basicType;
			}
			else {
				final ManagedDomainType<P> managedDomainType = getSession().getFactory()
						.getRuntimeMetamodels()
						.getJpaMetamodel()
						.managedType( javaTypeClass );
				if ( managedDomainType != null ) {
					paramType = managedDomainType;
				}
				else {
					throw new HibernateException( "Unable to determine BindableType : " + javaTypeClass.getName() );
				}
			}

			setParameter( position, value, paramType );
		}

		return this;
	}

	@Override
	public <P> QueryImplementor<R> setParameter(int position, P value, BindableType<P> type) {
		this.<P>locateBinding( position ).setBindValue( value, type );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameter(int position, Instant value, TemporalType temporalType) {
		this.locateBinding( position ).setBindValue( value, temporalType );
		return this;
	}



	@Override
	public <P> QueryImplementor<R> setParameter(QueryParameter<P> parameter, P value) {
		locateBinding( parameter ).setBindValue( value, resolveJdbcParameterTypeIfNecessary() );
		return this;
	}

	@Override
	public <P> QueryImplementor<R> setParameter(QueryParameter<P> parameter, P value, Class<P> javaTypeClass) {
		final JavaType<P> javaType = getSession().getFactory()
				.getTypeConfiguration()
				.getJavaTypeRegistry()
				.getDescriptor( javaTypeClass );
		if ( javaType == null ) {
			setParameter( parameter, value );
		}
		else {
			final BindableType<P> paramType;
			final BasicType<P> basicType = getSession().getFactory().getTypeConfiguration().standardBasicTypeForJavaType( javaTypeClass );
			if ( basicType != null ) {
				paramType = basicType;
			}
			else {
				final ManagedDomainType<P> managedDomainType = getSession().getFactory()
						.getRuntimeMetamodels()
						.getJpaMetamodel()
						.managedType( javaTypeClass );
				if ( managedDomainType != null ) {
					paramType = managedDomainType;
				}
				else {
					throw new HibernateException( "Unable to determine BindableType : " + javaTypeClass.getName() );
				}
			}

			setParameter( parameter, value, paramType );
		}

		return this;
	}

	@Override
	public <P> QueryImplementor<R> setParameter(QueryParameter<P> parameter, P value, BindableType<P> type) {
		locateBinding( parameter ).setBindValue( value,  type );
		return this;
	}


	@Override
	public <P> QueryImplementor<R> setParameter(Parameter<P> parameter, P value) {
		if ( value instanceof TypedParameterValue ) {
			@SuppressWarnings("unchecked")
			final TypedParameterValue<P> typedValue = (TypedParameterValue<P>) value;
			final BindableType<P> type = typedValue.getType();
			if ( type != null ) {
				setParameter( parameter, typedValue.getValue(), type );
			}
			else {
				setParameter( parameter, typedValue.getValue(), typedValue.getTypeReference() );
			}
		}
		else {
			locateBinding( parameter ).setBindValue( value, resolveJdbcParameterTypeIfNecessary() );
		}

		return this;
	}

	private <P> void setParameter(Parameter<P> parameter, P value, BindableType<P> type) {
		if ( parameter instanceof QueryParameter ) {
			setParameter( (QueryParameter<P>) parameter, value, type );
		}
		else if ( value == null ) {
			locateBinding( parameter ).setBindValue( null, type );
		}
		else if ( value instanceof Collection ) {
			//TODO: this looks wrong to me: how can value be both a P and a (Collection<P>)?
			locateBinding( parameter ).setBindValues( (Collection<P>) value );
		}
		else {
			locateBinding( parameter ).setBindValue( value, type );
		}
	}





	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter list

	@Override
	public QueryImplementor<R> setParameterList(String name, @SuppressWarnings("rawtypes") Collection values) {
		locateBinding( name ).setBindValues( values );
		return this;
	}

	public <P> QueryImplementor<R> setParameterList(String name, Collection<? extends P> values, Class<P> javaTypeClass) {
		final JavaType<P> javaType = getSession().getFactory()
				.getTypeConfiguration()
				.getJavaTypeRegistry()
				.getDescriptor( javaTypeClass );
		if ( javaType == null ) {
			setParameterList( name, values );
		}
		else {
			final BindableType<P> paramType;
			final BasicType<P> basicType = getSession().getFactory().getTypeConfiguration().standardBasicTypeForJavaType( javaTypeClass );
			if ( basicType != null ) {
				paramType = basicType;
			}
			else {
				final ManagedDomainType<P> managedDomainType = getSession().getFactory()
						.getRuntimeMetamodels()
						.getJpaMetamodel()
						.managedType( javaTypeClass );
				if ( managedDomainType != null ) {
					paramType = managedDomainType;
				}
				else {
					throw new HibernateException( "Unable to determine BindableType : " + javaTypeClass.getName() );
				}
			}

			setParameterList( name, values, paramType );
		}

		return this;
	}


	@Override
	public <P> QueryImplementor<R> setParameterList(String name, Collection<? extends P> values, BindableType<P> type) {
		this.<P>locateBinding( name ).setBindValues( values, type );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameterList(String name, Object[] values) {
		locateBinding( name ).setBindValues( Arrays.asList( values ) );
		return this;
	}

	@Override
	public <P> QueryImplementor<R> setParameterList(String name, P[] values, Class<P> javaTypeClass) {
		final JavaType<P> javaType = getSession().getFactory()
				.getTypeConfiguration()
				.getJavaTypeRegistry()
				.getDescriptor( javaTypeClass );
		if ( javaType == null ) {
			setParameterList( name, values );
		}
		else {
			final BindableType<P> paramType;
			final BasicType<P> basicType = getSession().getFactory().getTypeConfiguration().standardBasicTypeForJavaType( javaTypeClass );
			if ( basicType != null ) {
				paramType = basicType;
			}
			else {
				final ManagedDomainType<P> managedDomainType = getSession().getFactory()
						.getRuntimeMetamodels()
						.getJpaMetamodel()
						.managedType( javaTypeClass );
				if ( managedDomainType != null ) {
					paramType = managedDomainType;
				}
				else {
					throw new HibernateException( "Unable to determine BindableType : " + javaTypeClass.getName() );
				}
			}

			setParameterList( name, values, paramType );
		}

		return this;
	}

	public <P> QueryImplementor<R> setParameterList(String name, P[] values, BindableType<P> type) {
		this.<P>locateBinding( name ).setBindValues( Arrays.asList( values ), type );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameterList(int position, @SuppressWarnings("rawtypes") Collection values) {
		locateBinding( position ).setBindValues( values );
		return this;
	}

	@Override
	public <P> QueryImplementor<R> setParameterList(int position, Collection<? extends P> values, Class<P> javaTypeClass) {
		final JavaType<P> javaType = getSession().getFactory()
				.getTypeConfiguration()
				.getJavaTypeRegistry()
				.getDescriptor( javaTypeClass );
		if ( javaType == null ) {
			setParameterList( position, values );
		}
		else {
			final BindableType<P> paramType;
			final BasicType<P> basicType = getSession().getFactory().getTypeConfiguration().standardBasicTypeForJavaType( javaTypeClass );
			if ( basicType != null ) {
				paramType = basicType;
			}
			else {
				final ManagedDomainType<P> managedDomainType = getSession().getFactory()
						.getRuntimeMetamodels()
						.getJpaMetamodel()
						.managedType( javaTypeClass );
				if ( managedDomainType != null ) {
					paramType = managedDomainType;
				}
				else {
					throw new HibernateException( "Unable to determine BindableType : " + javaTypeClass.getName() );
				}
			}

			setParameterList( position, values, paramType );
		}

		return this;
	}

	@Override
	public <P> QueryImplementor<R> setParameterList(int position, Collection<? extends P> values, BindableType<P> type) {
		this.<P>locateBinding( position ).setBindValues( values, type );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameterList(int position, Object[] values) {
		locateBinding( position ).setBindValues( Arrays.asList( values ) );
		return this;
	}

	@Override
	public <P> QueryImplementor<R> setParameterList(int position, P[] values, Class<P> javaTypeClass) {
		final JavaType<P> javaType = getSession().getFactory()
				.getTypeConfiguration()
				.getJavaTypeRegistry()
				.getDescriptor( javaTypeClass );
		if ( javaType == null ) {
			setParameterList( position, values );
		}
		else {
			final BindableType<P> paramType;
			final BasicType<P> basicType = getSession().getFactory().getTypeConfiguration().standardBasicTypeForJavaType( javaTypeClass );
			if ( basicType != null ) {
				paramType = basicType;
			}
			else {
				final ManagedDomainType<P> managedDomainType = getSession().getFactory()
						.getRuntimeMetamodels()
						.getJpaMetamodel()
						.managedType( javaTypeClass );
				if ( managedDomainType != null ) {
					paramType = managedDomainType;
				}
				else {
					throw new HibernateException( "Unable to determine BindableType : " + javaTypeClass.getName() );
				}
			}

			setParameterList( position, values, paramType );
		}

		return this;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <P> QueryImplementor<R> setParameterList(int position, P[] values, BindableType<P> type) {
		locateBinding( position ).setBindValues( Arrays.asList( values ), (BindableType) type );
		return this;
	}








	@Override
	public <P> QueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values) {
		locateBinding( parameter ).setBindValues( values );
		return this;
	}

	@Override
	public <P> QueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaTypeClass) {
		final JavaType<P> javaType = getSession().getFactory()
				.getTypeConfiguration()
				.getJavaTypeRegistry()
				.getDescriptor( javaTypeClass );
		if ( javaType == null ) {
			setParameterList( parameter, values );
		}
		else {
			final BindableType<P> paramType;
			final BasicType<P> basicType = getSession().getFactory().getTypeConfiguration().standardBasicTypeForJavaType( javaTypeClass );
			if ( basicType != null ) {
				paramType = basicType;
			}
			else {
				final ManagedDomainType<P> managedDomainType = getSession().getFactory()
						.getRuntimeMetamodels()
						.getJpaMetamodel()
						.managedType( javaTypeClass );
				if ( managedDomainType != null ) {
					paramType = managedDomainType;
				}
				else {
					throw new HibernateException( "Unable to determine BindableType : " + javaTypeClass.getName() );
				}
			}

			setParameterList( parameter, values, paramType );
		}

		return this;
	}

	@Override
	public <P> QueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, BindableType<P> type) {
		locateBinding( parameter ).setBindValues( values, type );
		return this;
	}

	@Override
	public <P> QueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values) {
		locateBinding( parameter ).setBindValues( values == null ? null : Arrays.asList( values ) );
		return this;
	}

	@Override
	public <P> QueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaTypeClass) {
		final JavaType<P> javaType = getSession().getFactory()
				.getTypeConfiguration()
				.getJavaTypeRegistry()
				.getDescriptor( javaTypeClass );
		if ( javaType == null ) {
			setParameterList( parameter, values );
		}
		else {
			final BindableType<P> paramType;
			final BasicType<P> basicType = getSession().getFactory().getTypeConfiguration().standardBasicTypeForJavaType( javaTypeClass );
			if ( basicType != null ) {
				paramType = basicType;
			}
			else {
				final ManagedDomainType<P> managedDomainType = getSession().getFactory()
						.getRuntimeMetamodels()
						.getJpaMetamodel()
						.managedType( javaTypeClass );
				if ( managedDomainType != null ) {
					paramType = managedDomainType;
				}
				else {
					throw new HibernateException( "Unable to determine BindableType : " + javaTypeClass.getName() );
				}
			}

			setParameterList( parameter, values, paramType );
		}

		return this;
	}


	@Override
	public <P> QueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values, BindableType<P> type) {
		locateBinding( parameter ).setBindValues( Arrays.asList( values ), type );
		return this;
	}


	@Override
	public QueryImplementor<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		locateBinding( param ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		locateBinding( param ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameter(String name, Calendar value, TemporalType temporalType) {
		locateBinding( name ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameter(String name, Date value, TemporalType temporalType) {
		locateBinding( name ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameter(int position, Calendar value, TemporalType temporalType) {
		locateBinding( position ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameter(int position, Date value, TemporalType temporalType) {
		locateBinding( position ).setBindValue( value, temporalType );
		return this;
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// execution

	private FlushMode sessionFlushMode;
	private CacheMode sessionCacheMode;

	protected void beforeQuery() {
		getQueryParameterBindings().validate();

		getSession().prepareForQueryExecution(false);

		prepareForExecution();

		assert sessionFlushMode == null;
		assert sessionCacheMode == null;

		final FlushMode effectiveFlushMode = getHibernateFlushMode();
		if ( effectiveFlushMode != null ) {
			sessionFlushMode = getSession().getHibernateFlushMode();
			getSession().setHibernateFlushMode( effectiveFlushMode );
		}

		final CacheMode effectiveCacheMode = getCacheMode();
		if ( effectiveCacheMode != null ) {
			sessionCacheMode = getSession().getCacheMode();
			getSession().setCacheMode( effectiveCacheMode );
		}
	}

	protected void prepareForExecution() {
	}

	@Override
	public QueryImplementor<R> setProperties(Object bean) {
		final Class<?> clazz = bean.getClass();
		for ( String paramName : getParameterMetadata().getNamedParameterNames() ) {
			try {
				final PropertyAccess propertyAccess = BuiltInPropertyAccessStrategies.BASIC.getStrategy().buildPropertyAccess(
						clazz,
						paramName,
						true );
				final Getter getter = propertyAccess.getGetter();
				final Class<?> retType = getter.getReturnTypeClass();
				final Object object = getter.get( bean );
				if ( Collection.class.isAssignableFrom( retType ) ) {
					setParameterList( paramName, (Collection<?>) object );
				}
				else if ( retType.isArray() ) {
					setParameterList( paramName, (Object[]) object );
				}
				else {
					BindableType<Object> type = determineType( paramName, retType );
					setParameter( paramName, object, type );
				}
			}
			catch (PropertyNotFoundException pnfe) {
				// ignore
			}
		}
		return this;
	}

	@SuppressWarnings("WeakerAccess")
	protected BindableType<Object> determineType(String namedParam, Class<?> retType) {
		BindableType<?> type = locateBinding( namedParam ).getBindType();
		if ( type == null ) {
			type = getParameterMetadata().getQueryParameter( namedParam ).getHibernateType();
		}
		if ( type == null && retType != null ) {
			type = getSession().getFactory().resolveParameterBindType( retType );
		}
		//noinspection unchecked
		return (BindableType<Object>) type;
	}

	@Override
	public QueryImplementor<R> setProperties(@SuppressWarnings("rawtypes") Map map) {
		for ( String paramName : getParameterMetadata().getNamedParameterNames() ) {
			final Object object = map.get( paramName );
			if ( object == null ) {
				if ( map.containsKey( paramName ) ) {
					setParameter( paramName, null, determineType( paramName, null ) );
				}
			}
			else {
				if ( object instanceof Collection<?> ) {
					setParameterList( paramName, (Collection<?>) object );
				}
				else if ( object instanceof Object[] ) {
					setParameterList( paramName, (Object[]) object );
				}
				else {
					setParameter( paramName, object, determineType( paramName, object.getClass() ) );
				}
			}
		}
		return this;
	}

	@SuppressWarnings("WeakerAccess")
	protected void afterQuery(boolean success) {
		if ( sessionFlushMode != null ) {
			getSession().setHibernateFlushMode( sessionFlushMode );
			sessionFlushMode = null;
		}
		if ( sessionCacheMode != null ) {
			getSession().setCacheMode( sessionCacheMode );
			sessionCacheMode = null;
		}
		if ( !getSession().isTransactionInProgress() ) {
			getSession().getJdbcCoordinator().getLogicalConnection().afterTransaction();
		}
		getSession().afterOperation( success );
	}

	@Override
	public List<R> list() {
		beforeQuery();
		boolean success = false;
		try {
			final List<R> result = doList();
			success = true;
			return result;
		}
		catch (IllegalQueryOperationException e) {
			throw new IllegalStateException( e );
		}
		catch (TypeMismatchException e) {
			throw new IllegalArgumentException( e );
		}
		catch (HibernateException he) {
			throw getSession().getExceptionConverter().convert( he, getLockOptions() );
		}
		finally {
			afterQuery( success );
		}
	}

	protected abstract List<R> doList();

	@Override
	public R uniqueResult() {
		return uniqueElement( list() );
	}

	@Override
	public R getSingleResult() {
		try {
			final List<R> list = list();
			if ( list.isEmpty() ) {
				throw new NoResultException( "No entity found for query" );
			}
			return uniqueElement( list );
		}
		catch ( HibernateException e ) {
			throw getSession().getExceptionConverter().convert( e, getLockOptions() );
		}
	}

	@SuppressWarnings("WeakerAccess")
	public static <R> R uniqueElement(List<R> list) throws NonUniqueResultException {
		int size = list.size();
		if ( size == 0 ) {
			return null;
		}
		R first = list.get( 0 );
		// todo (6.0) : add a setting here to control whether to perform this validation or not
		for ( int i = 1; i < size; i++ ) {
			if ( list.get( i ) != first ) {
				throw new NonUniqueResultException( list.size() );
			}
		}
		return first;
	}

	@Override
	public Optional<R> uniqueResultOptional() {
		return Optional.ofNullable( uniqueResult() );
	}

	@Override
	public ScrollableResultsImplementor<R> scroll() {
		return scroll( getSession().getFactory().getJdbcServices().getJdbcEnvironment().getDialect().defaultScrollMode() );
	}

	@Override
	@SuppressWarnings( {"unchecked", "rawtypes"} )
	public Stream<R> stream() {
		final ScrollableResultsImplementor scrollableResults = scroll( ScrollMode.FORWARD_ONLY );
		final ScrollableResultsIterator<R> iterator = new ScrollableResultsIterator<>( scrollableResults );
		final Spliterator<R> spliterator = Spliterators.spliteratorUnknownSize( iterator, Spliterator.NONNULL );

		final Stream<R> stream = StreamSupport.stream( spliterator, false );
		return stream.onClose( scrollableResults::close );
	}

	@Override
	public int executeUpdate() throws HibernateException {
		getSession().checkTransactionNeededForUpdateOperation( "Executing an update/delete query" );
		beforeQuery();
		boolean success = false;
		try {
			final int result = doExecuteUpdate();
			success = true;
			return result;
		}
		catch (IllegalQueryOperationException e) {
			throw new IllegalStateException( e );
		}
		catch (TypeMismatchException e) {
			throw new IllegalArgumentException( e );
		}
		catch (HibernateException e) {
			throw getSession().getExceptionConverter().convert( e );
		}
		finally {
			afterQuery( success );
		}
	}

	protected abstract int doExecuteUpdate();



	@Override
	public void setOptionalId(Serializable id) {
		throw new UnsupportedOperationException( "Not sure yet how to handle this in SQM based queries, but for sure it will be different" );
	}

	@Override
	public void setOptionalEntityName(String entityName) {
		throw new UnsupportedOperationException( "Not sure yet how to handle this in SQM based queries, but for sure it will be different" );
	}

	@Override
	public void setOptionalObject(Object optionalObject) {
		throw new UnsupportedOperationException( "Not sure yet how to handle this in SQM based queries, but for sure it will be different" );
	}
}
