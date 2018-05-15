/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.internal;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
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
import javax.persistence.CacheRetrieveMode;
import javax.persistence.CacheStoreMode;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.Parameter;
import javax.persistence.TemporalType;
import javax.persistence.TransactionRequiredException;

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
import org.hibernate.jpa.QueryHints;
import org.hibernate.jpa.internal.util.CacheModeHelper;
import org.hibernate.jpa.internal.util.ConfigurationHelper;
import org.hibernate.jpa.internal.util.FlushModeTypeHelper;
import org.hibernate.jpa.internal.util.LockModeTypeHelper;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.TypedParameterValue;
import org.hibernate.query.spi.MutableQueryOptions;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.spi.ManagedJavaDescriptor;

import static org.hibernate.LockOptions.WAIT_FOREVER;
import static org.hibernate.cfg.AvailableSettings.JPA_LOCK_SCOPE;
import static org.hibernate.cfg.AvailableSettings.JPA_LOCK_TIMEOUT;
import static org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_RETRIEVE_MODE;
import static org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_STORE_MODE;
import static org.hibernate.internal.util.NullnessHelper.nullif;
import static org.hibernate.jpa.AvailableSettings.ALIAS_SPECIFIC_LOCK_MODE;
import static org.hibernate.jpa.QueryHints.HINT_CACHEABLE;
import static org.hibernate.jpa.QueryHints.HINT_CACHE_MODE;
import static org.hibernate.jpa.QueryHints.HINT_CACHE_REGION;
import static org.hibernate.jpa.QueryHints.HINT_COMMENT;
import static org.hibernate.jpa.QueryHints.HINT_FETCHGRAPH;
import static org.hibernate.jpa.QueryHints.HINT_FETCH_SIZE;
import static org.hibernate.jpa.QueryHints.HINT_FLUSH_MODE;
import static org.hibernate.jpa.QueryHints.HINT_FOLLOW_ON_LOCKING;
import static org.hibernate.jpa.QueryHints.HINT_LOADGRAPH;
import static org.hibernate.jpa.QueryHints.HINT_READONLY;
import static org.hibernate.jpa.QueryHints.HINT_TIMEOUT;
import static org.hibernate.jpa.QueryHints.SPEC_HINT_TIMEOUT;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractQuery<R> implements QueryImplementor<R> {
	private static final EntityManagerMessageLogger log = HEMLogging.messageLogger( AbstractQuery.class );

	private final SharedSessionContractImplementor session;

	public AbstractQuery(SharedSessionContractImplementor session) {
		this.session = session;
	}

	@Override
	public SharedSessionContractImplementor getSession() {
		return session;
	}

	protected abstract boolean canApplyAliasSpecificLockModes();

	protected abstract void verifySettingLockMode();

	protected abstract void verifySettingAliasSpecificLockModes();

	protected abstract QueryParameterBindings getQueryParameterBindings();

	@Override
	public abstract ParameterMetadataImplementor<QueryParameterImplementor<?>> getParameterMetadata();

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// QueryOptions handling

	@Override
	public abstract MutableQueryOptions getQueryOptions();


	@Override
	public int getMaxResults() {
		return getQueryOptions().getLimit().getMaxRowsJpa();
	}

	@Override
	public QueryImplementor<R> setMaxResults(int maxResult) {
		getQueryOptions().getLimit().setMaxRows( maxResult );
		return this;
	}

	@Override
	public int getFirstResult() {
		return getQueryOptions().getLimit().getFirstRowJpa();
	}

	@Override
	public QueryImplementor<R> setFirstResult(int startPosition) {
		getQueryOptions().getLimit().setFirstRow( startPosition );
		return this;
	}

	@Override
	public QueryImplementor<R> setTupleTransformer(TupleTransformer transformer) {
		getQueryOptions().setTupleTransformer( transformer );
		return this;
	}

	@Override
	public QueryImplementor<R> setResultListTransformer(ResultListTransformer transformer) {
		getQueryOptions().setResultListTransformer( transformer );
		return this;
	}

	@Override
	public LockModeType getLockMode() {
		return LockModeTypeHelper.getLockModeType( getQueryOptions().getLockOptions().getLockMode() );
	}

	@Override
	public FlushMode getHibernateFlushMode() {
		return getQueryOptions().getFlushMode();
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setHibernateFlushMode(FlushMode flushMode) {
		getQueryOptions().setFlushMode( flushMode );
		return this;
	}

	@Override
	public FlushModeType getFlushMode() {
		final FlushMode flushMode = getQueryOptions().getFlushMode() == null
				? getSession().getHibernateFlushMode()
				: getQueryOptions().getFlushMode();
		return FlushModeTypeHelper.getFlushModeType( flushMode );
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setFlushMode(FlushModeType flushModeType) {
		setHibernateFlushMode( FlushModeTypeHelper.getFlushMode( flushModeType ) );
		return this;
	}	@Override
	public CacheMode getCacheMode() {
		return getQueryOptions().getCacheMode();
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setCacheMode(CacheMode cacheMode) {
		getQueryOptions().setCacheMode( cacheMode );
		return this;
	}

	@Override
	public boolean isCacheable() {
		return getQueryOptions().isResultCachingEnabled() == Boolean.TRUE;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setCacheable(boolean cacheable) {
		getQueryOptions().setResultCachingEnabled( cacheable );
		return this;
	}

	@Override
	public String getCacheRegion() {
		return getQueryOptions().getResultCacheRegionName();
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setCacheRegion(String cacheRegion) {
		getQueryOptions().setResultCacheRegionName( cacheRegion );
		return this;
	}

	@Override
	public Integer getTimeout() {
		return getQueryOptions().getTimeout();
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setTimeout(int timeout) {
		getQueryOptions().setTimeout( timeout );
		return this;
	}

	@Override
	public Integer getFetchSize() {
		return getQueryOptions().getFetchSize();
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setFetchSize(int fetchSize) {
		getQueryOptions().setFetchSize( fetchSize );
		return this;
	}

	@Override
	public boolean isReadOnly() {
		return getQueryOptions().isReadOnly() == null
				? getSession().isDefaultReadOnly()
				: getQueryOptions().isReadOnly();
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setReadOnly(boolean readOnly) {
		getQueryOptions().setReadOnly( readOnly );
		return this;
	}

	@Override
	public LockOptions getLockOptions() {
		return getQueryOptions().getLockOptions();
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setLockOptions(LockOptions lockOptions) {
		getQueryOptions().getLockOptions().setLockMode( lockOptions.getLockMode() );
		getQueryOptions().getLockOptions().setScope( lockOptions.getScope() );
		getQueryOptions().getLockOptions().setTimeOut( lockOptions.getTimeOut() );
		getQueryOptions().getLockOptions().setFollowOnLocking( lockOptions.getFollowOnLocking() );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setLockMode(String alias, LockMode lockMode) {
		if ( !LockMode.NONE.equals( lockMode ) ) {
			verifySettingAliasSpecificLockModes();
		}

		getQueryOptions().getLockOptions().setAliasSpecificLockMode( alias, lockMode );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setLockMode(LockModeType lockModeType) {
		if ( !LockModeType.NONE.equals( lockModeType ) ) {
			verifySettingLockMode();
		}

		getQueryOptions().getLockOptions().setLockMode( LockModeTypeHelper.getLockMode( lockModeType ) );
		return this;
	}

	@Override
	public String getComment() {
		return getQueryOptions().getComment();
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setComment(String comment) {
		getQueryOptions().setComment( comment );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor addQueryHint(String hint) {
		getQueryOptions().addDatabaseHint( hint );
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA hint handling


	@SuppressWarnings( {"UnusedDeclaration"})
	public Set<String> getSupportedHints() {
		return QueryHints.getDefinedHints();
	}

	@Override
	public Map<String, Object> getHints() {
		// Technically this should rollback, but that's insane :)
		// If the TCK ever adds a check for this, we may need to change this behavior
		getSession().checkOpen( false );

		final Map<String,Object> hints = new HashMap<>();
		collectBaselineHints( hints );
		collectHints( hints );
		return hints;
	}

	protected void collectBaselineHints(Map<String, Object> hints) {
		// nothing to do in this form
	}

	protected void collectHints(Map<String, Object> hints) {
		if ( getQueryOptions().getTimeout() != null ) {
			hints.put( HINT_TIMEOUT, getQueryOptions().getTimeout() );
			hints.put( SPEC_HINT_TIMEOUT, getQueryOptions().getTimeout() * 1000 );
		}

		if ( getLockOptions().getTimeOut() != WAIT_FOREVER ) {
			hints.put( JPA_LOCK_TIMEOUT, getLockOptions().getTimeOut() );
		}

		if ( getLockOptions().getScope() ) {
			hints.put( JPA_LOCK_SCOPE, getLockOptions().getScope() );
		}

		if ( getLockOptions().hasAliasSpecificLockModes() ) {
			for ( Map.Entry<String, LockMode> entry : getLockOptions().getAliasSpecificLocks() ) {
				hints.put(
						ALIAS_SPECIFIC_LOCK_MODE + '.' + entry.getKey(),
						entry.getValue().name()
				);
			}
		}

		putIfNotNull( hints, HINT_COMMENT, getComment() );
		putIfNotNull( hints, HINT_FETCH_SIZE, getQueryOptions().getFetchSize() );
		putIfNotNull( hints, HINT_FLUSH_MODE, getHibernateFlushMode() );

		if ( getCacheMode() != null ) {
			putIfNotNull( hints, HINT_CACHE_MODE, getCacheMode() );
			putIfNotNull( hints, JPA_SHARED_CACHE_RETRIEVE_MODE, CacheModeHelper.interpretCacheRetrieveMode( getCacheMode() ) );
			putIfNotNull( hints, JPA_SHARED_CACHE_STORE_MODE, CacheModeHelper.interpretCacheStoreMode( getCacheMode() ) );
		}

		if ( isCacheable() ) {
			hints.put( HINT_CACHEABLE, true );
			putIfNotNull( hints, HINT_CACHE_REGION, getCacheRegion() );
		}

		if ( isReadOnly() ) {
			hints.put( HINT_READONLY, true );
		}
	}

	protected void putIfNotNull(Map<String, Object> hints, String hintName, Enum hintValue) {
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

	@Override
	public QueryImplementor<R> setHint(String hintName, Object value) {
		getSession().checkOpen( true );
		boolean applied = false;
		try {
			if ( HINT_TIMEOUT.equals( hintName ) ) {
				applied = applyTimeoutHint( ConfigurationHelper.getInteger( value ) );
			}
			else if ( SPEC_HINT_TIMEOUT.equals( hintName ) ) {
				// convert milliseconds to seconds
				int timeout = (int)Math.round( ConfigurationHelper.getInteger( value ).doubleValue() / 1000.0 );
				applied = applyTimeoutHint( timeout );
			}
			else if ( JPA_LOCK_TIMEOUT.equals( hintName ) ) {
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
			else if ( HINT_FLUSH_MODE.equals( hintName ) ) {
				applied = applyFlushModeHint( ConfigurationHelper.getFlushMode( value ) );
			}
			else if ( HINT_CACHE_MODE.equals( hintName ) ) {
				applied = applyCacheModeHint( ConfigurationHelper.getCacheMode( value ) );
			}
			else if ( JPA_SHARED_CACHE_RETRIEVE_MODE.equals( hintName ) ) {
				final CacheRetrieveMode retrieveMode = value != null ? CacheRetrieveMode.valueOf( value.toString() ) : null;
				applied = applyJpaCacheRetrieveMode( retrieveMode );
			}
			else if ( JPA_SHARED_CACHE_STORE_MODE.equals( hintName ) ) {
				final CacheStoreMode storeMode = value != null ? CacheStoreMode.valueOf( value.toString() ) : null;
				applied = applyJpaCacheStoreMode( storeMode );
			}
			else if ( QueryHints.HINT_NATIVE_LOCKMODE.equals( hintName ) ) {
				applied = applyNativeQueryLockMode( value );
			}
			else if ( hintName.startsWith( ALIAS_SPECIFIC_LOCK_MODE ) ) {
				if ( canApplyAliasSpecificLockModes() ) {
					// extract the alias
					final String alias = hintName.substring( ALIAS_SPECIFIC_LOCK_MODE.length() + 1 );
					// determine the LockMode
					try {
						final LockMode lockMode = LockModeTypeHelper.interpretLockMode( value );
						applyAliasSpecificLockModeHint( alias, lockMode );
					}
					catch ( Exception e ) {
						log.unableToDetermineLockModeValue( hintName, value );
						applied = false;
					}
				}
				else {
					applied = false;
				}
			}
			else if ( HINT_FETCHGRAPH.equals( hintName ) || HINT_LOADGRAPH.equals( hintName ) ) {
				if (value instanceof RootGraphImplementor ) {
					applyEntityGraphQueryHint( hintName, (RootGraphImplementor) value );
				}
				else {
					log.warnf( "The %s hint was set, but the value was not an EntityGraph!", hintName );
				}
				applied = true;
			}
			else if ( HINT_FOLLOW_ON_LOCKING.equals( hintName ) ) {
				applied = applyFollowOnLockingHint( ConfigurationHelper.getBoolean( value ) );
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

	protected boolean applyJpaCacheRetrieveMode(CacheRetrieveMode retrieveMode) {
		final CacheMode currentCacheMode = nullif( getCacheMode(), getSession().getCacheMode() );
		setCacheMode(
				CacheModeHelper.interpretCacheMode(
						CacheModeHelper.interpretCacheStoreMode( currentCacheMode ),
						retrieveMode
				)
		);
		return true;
	}

	protected boolean applyJpaCacheStoreMode(CacheStoreMode storeMode) {
		final CacheMode currentCacheMode = nullif( getCacheMode(), getSession().getCacheMode() );
		setCacheMode(
				CacheModeHelper.interpretCacheMode(
						storeMode,
						CacheModeHelper.interpretCacheRetrieveMode( currentCacheMode )
				)
		);
		return true;
	}

	protected boolean applyNativeQueryLockMode(Object value) {
		return false;
	}

	/**
	 * Apply the query timeout hint.
	 *
	 * @param timeout The timeout (in seconds!) specified as a hint
	 *
	 * @return {@code true} if the hint was "applied"
	 */
	protected boolean applyTimeoutHint(int timeout) {
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
	protected boolean applyFlushModeHint(FlushMode flushMode) {
		setHibernateFlushMode( flushMode );
		return true;
	}

	protected boolean applyLockModeTypeHint(LockModeType lockModeType) {
		getLockOptions().setLockMode( LockModeTypeHelper.getLockMode( lockModeType ) );
		return true;
	}

	protected boolean applyHibernateLockModeHint(LockMode lockMode) {
		getLockOptions().setLockMode( lockMode );
		return true;
	}

	/**
	 * Apply the alias specific lock modes.  Assumes {@link #canApplyAliasSpecificLockModes()} has already been
	 * called and returned {@code true}.
	 *
	 * @param alias The alias to apply the 'lockMode' to.
	 * @param lockMode The LockMode to apply.
	 */
	protected void applyAliasSpecificLockModeHint(String alias, LockMode lockMode) {
		getLockOptions().setAliasSpecificLockMode( alias, lockMode );
	}

	protected abstract void applyEntityGraphQueryHint(String hintName, RootGraphImplementor entityGraph);

	/**
	 * Apply the follow-on-locking hint.
	 *
	 * @param followOnLocking The follow-on-locking strategy.
	 */
	protected boolean applyFollowOnLockingHint(Boolean followOnLocking) {
		getLockOptions().setFollowOnLocking( followOnLocking );
		return true;
	}





	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// QueryParameter handling

	@Override
	@SuppressWarnings("unchecked")
	public Set<Parameter<?>> getParameters() {
		return ( (ParameterMetadata) getParameterMetadata() ).getRegistrations();
	}

	@Override
	public Parameter<?> getParameter(String name) {
		try {
			return getParameterMetadata().getQueryParameter( name );
		}
		catch ( HibernateException e ) {
			throw getSession().getExceptionConverter().convert( e );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Parameter<T> getParameter(String name, Class<T> type) {
		try {
			final QueryParameter parameter = getParameterMetadata().getQueryParameter( name );
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
	public Parameter<?> getParameter(int position) {
		try {
			return getParameterMetadata().getQueryParameter( position );
		}
		catch ( HibernateException e ) {
			throw getSession().getExceptionConverter().convert( e );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Parameter<T> getParameter(int position, Class<T> type) {
		try {
			final QueryParameter parameter = getParameterMetadata().getQueryParameter( position );
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
	public boolean isBound(Parameter<?> param) {
		final QueryParameterImplementor qp = getParameterMetadata().resolve( param );
		return qp != null && getQueryParameterBindings().isBound( qp );
	}

	// todo : rename these #locateParameterBinding

	@SuppressWarnings("unchecked")
	protected <P> QueryParameterBinding<P> locateBinding(Parameter<P> parameter) {
		if ( parameter instanceof QueryParameter ) {
			return locateBinding( (QueryParameter) parameter );
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

	protected <P> QueryParameterBinding<P> locateBinding(QueryParameterImplementor<P> parameter) {
		return getQueryParameterBindings().getBinding( parameter );
	}

	protected <P> QueryParameterBinding<P> locateBinding(String name) {
		return getQueryParameterBindings().getBinding( name );
	}

	protected <P> QueryParameterBinding<P> locateBinding(int position) {
		return getQueryParameterBindings().getBinding( position );
	}

	@Override
	public QueryImplementor<R> setParameter(Parameter<Instant> param, Instant value, TemporalType temporalType) {
		locateBinding( param ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameter(Parameter<LocalDateTime> param, LocalDateTime value, TemporalType temporalType) {
		locateBinding( param ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameter(Parameter<ZonedDateTime> param, ZonedDateTime value, TemporalType temporalType) {
		locateBinding( param ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameter(Parameter<OffsetDateTime> param, OffsetDateTime value, TemporalType temporalType) {
		locateBinding( param ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameter(String name, Instant value, TemporalType temporalType) {
		locateBinding( name ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameter(String name, LocalDateTime value, TemporalType temporalType) {
		locateBinding( name ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameter(String name, ZonedDateTime value, TemporalType temporalType) {
		locateBinding( name ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameter(String name, OffsetDateTime value, TemporalType temporalType) {
		locateBinding( name ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameter(int position, Instant value, TemporalType temporalType) {
		locateBinding( position ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameter(int position, LocalDateTime value, TemporalType temporalType) {
		locateBinding( position ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameter(int position, ZonedDateTime value, TemporalType temporalType) {
		locateBinding( position ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameter(int position, OffsetDateTime value, TemporalType temporalType) {
		locateBinding( position ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <P> QueryImplementor<R> setParameter(QueryParameter<P> parameter, P value) {
		locateBinding( parameter ).setBindValue( value );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <P> QueryImplementor<R> setParameter(Parameter<P> parameter, P value) {
		if ( value instanceof TypedParameterValue ) {
			setParameter( parameter, ( (TypedParameterValue) value ).getValue(), ( (TypedParameterValue) value ).getType() );
		}
		else {
			locateBinding( parameter ).setBindValue( value );
		}

		return this;
	}

	@SuppressWarnings("unchecked")
	private <P> void setParameter(Parameter<P> parameter, Object value, AllowableParameterType type) {
		if ( parameter instanceof QueryParameter ) {
			setParameter( (QueryParameter) parameter, value, type );
		}
		else if ( value == null ) {
			locateBinding( parameter ).setBindValue( null, type );
		}
		else if ( value instanceof Collection ) {
			locateBinding( parameter ).setBindValues( (Collection) value );
		}
		else {
			locateBinding( parameter ).setBindValue( (P) value, type );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor<R> setParameter(String name, Object value) {
		if ( value instanceof TypedParameterValue ) {
			final TypedParameterValue  typedValueWrapper = (TypedParameterValue) value;
			setParameter( name, typedValueWrapper.getValue(), typedValueWrapper.getType() );
		}
		else if ( value instanceof Collection ) {
			setParameterList( name, (Collection) value );
		}
		else {
			locateBinding( name ).setBindValue( value );
		}

		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor<R> setParameter(int position, Object value) {
		if ( value instanceof TypedParameterValue ) {
			final TypedParameterValue typedParameterValue = (TypedParameterValue) value;
			setParameter( position, typedParameterValue.getValue(), typedParameterValue.getType() );
		}
		if ( value instanceof Collection ) {
			setParameterList( Integer.toString( position ), (Collection) value );
		}
		else {
			locateBinding( position ).setBindValue( value );
		}
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <P> QueryImplementor<R> setParameter(QueryParameter<P> parameter, P value, AllowableParameterType type) {
		locateBinding( parameter ).setBindValue( value,  type );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor<R> setParameter(String name, Object value, AllowableParameterType type) {
		locateBinding( name ).setBindValue( value, type );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(int position, Object value, AllowableParameterType type) {
		locateBinding( position ).setBindValue( value, type );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <P> QueryImplementor<R> setParameter(QueryParameter<P> parameter, P value, TemporalType temporalType) {
		locateBinding( parameter ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor<R> setParameter(String name, Object value, TemporalType temporalType) {
		locateBinding( name ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor<R> setParameter(int position, Object value, TemporalType temporalType) {
		locateBinding( position ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <P> QueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<P> values) {
		locateBinding( parameter ).setBindValues( values );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameterList(String name, Collection values) {
		locateBinding( name ).setBindValues( values );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor<R> setParameterList(int position, Collection values) {
		locateBinding( position ).setBindValues( values );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor<R> setParameterList(String name, Collection values, AllowableParameterType type) {
		locateBinding( name ).setBindValues( values, type );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor<R> setParameterList(int position, Collection values, AllowableParameterType type) {
		locateBinding( position ).setBindValues( values, type );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor<R> setParameterList(String name, Object[] values) {
		locateBinding( name ).setBindValues( Arrays.asList( values ) );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameterList(int position, Object[] values) {
		locateBinding( position ).setBindValues( Arrays.asList( values ) );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor<R> setParameterList(String name, Object[] values, AllowableParameterType type) {
		locateBinding( name ).setBindValues( Arrays.asList( values ), type );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor<R> setParameterList(int position, Object[] values, AllowableParameterType type) {
		locateBinding( position ).setBindValues( Arrays.asList( values ), type );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor<R> setParameterList(String name, Collection values, Class type) {
		final JavaTypeDescriptor javaDescriptor = getSession().getFactory()
				.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getDescriptor( type );
		if ( javaDescriptor == null ) {
			setParameterList( name, values );
		}
		else {
			final AllowableParameterType paramType;
			if ( javaDescriptor instanceof BasicJavaDescriptor ) {
				paramType = getSession().getFactory()
						.getTypeConfiguration()
						.getBasicTypeRegistry()
						.getBasicType( type );
			}
			else if ( javaDescriptor instanceof ManagedJavaDescriptor ) {
				paramType = (AllowableParameterType) getSession().getFactory().getMetamodel().managedType( type );
			}
			else {
				throw new HibernateException( "Unable to determine AllowableParameterType : " + type.getName() );
			}

			setParameterList( name, values, paramType );
		}

		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor<R> setParameterList(int position, Collection values, Class type) {
		final JavaTypeDescriptor javaDescriptor = getSession().getFactory()
				.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getDescriptor( type );
		if ( javaDescriptor == null ) {
			setParameterList( position, values );
		}
		else {
			final AllowableParameterType paramType;
			if ( javaDescriptor instanceof BasicJavaDescriptor ) {
				paramType = getSession().getFactory()
						.getTypeConfiguration()
						.getBasicTypeRegistry()
						.getBasicType( type );
			}
			else if ( javaDescriptor instanceof ManagedJavaDescriptor ) {
				paramType = (AllowableParameterType) getSession().getFactory().getMetamodel().managedType( type );
			}
			else {
				throw new HibernateException( "Unable to determine AllowableParameterType : " + type.getName() );
			}

			setParameterList( position, values, paramType );
		}

		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		locateBinding( (QueryParameter) param ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		locateBinding( (QueryParameter) param ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor<R> setParameter(String name, Calendar value, TemporalType temporalType) {
		locateBinding( name ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor<R> setParameter(String name, Date value, TemporalType temporalType) {
		locateBinding( name ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor<R> setParameter(int position, Calendar value, TemporalType temporalType) {
		locateBinding( position ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor<R> setParameter(int position, Date value, TemporalType temporalType) {
		locateBinding( position ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getParameterValue(Parameter<T> param) {
		final QueryParameterImplementor qp = getParameterMetadata().resolve( param );
		if ( qp == null ) {
			throw new IllegalArgumentException( "The parameter [" + param + "] is not part of this Query" );
		}

		final QueryParameterBinding binding = getQueryParameterBindings().getBinding( qp );
		if ( binding == null || !binding.isBound() ) {
			throw new IllegalStateException( "The parameter [" + param + "] has not yet been bound" );
		}

		if ( binding.isMultiValued() ) {
			return (T) binding.getBindValues();
		}
		else {
			return (T) binding.getBindValue();
		}
	}

	@Override
	public Object getParameterValue(String name) {
		final QueryParameterImplementor qp = getParameterMetadata().getQueryParameter( name );
		if ( qp == null ) {
			throw new IllegalArgumentException( "The parameter [" + name + "] is not part of this Query" );
		}

		final QueryParameterBinding binding = getQueryParameterBindings().getBinding( qp );
		if ( binding == null || !binding.isBound() ) {
			throw new IllegalStateException( "The parameter [" + name + "] has not yet been bound" );
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
		final QueryParameterImplementor qp = getParameterMetadata().getQueryParameter( position );
		if ( qp == null ) {
			throw new IllegalArgumentException( "The parameter [" + position + "] is not part of this Query" );
		}

		final QueryParameterBinding binding = getQueryParameterBindings().getBinding( qp );
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



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// execution

	private FlushMode sessionFlushMode;
	private CacheMode sessionCacheMode;

	protected void beforeQuery() {
		getQueryParameterBindings().validate();

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
	@SuppressWarnings("unchecked")
	public QueryImplementor<R> setProperties(Object bean) {
		Class clazz = bean.getClass();
		for ( String paramName : getParameterMetadata().getNamedParameterNames() ) {
			try {
				final PropertyAccess propertyAccess = BuiltInPropertyAccessStrategies.BASIC.getStrategy().buildPropertyAccess(
						clazz,
						paramName
				);
				final Getter getter = propertyAccess.getGetter();
				final Class retType = getter.getReturnType();
				final Object object = getter.get( bean );
				if ( Collection.class.isAssignableFrom( retType ) ) {
					setParameterList( paramName, (Collection) object );
				}
				else if ( retType.isArray() ) {
					setParameterList( paramName, (Object[]) object );
				}
				else {
					AllowableParameterType type = determineType( paramName, retType );
					setParameter( paramName, object, (Type) type );
				}
			}
			catch (PropertyNotFoundException pnfe) {
				// ignore
			}
		}
		return this;
	}

	protected AllowableParameterType determineType(String namedParam, Class retType) {
		AllowableParameterType type = locateBinding( namedParam ).getBindType();
		if ( type == null ) {
			type = getParameterMetadata().getQueryParameter( namedParam ).getHibernateType();
		}
		if ( type == null ) {
			type = getSession().getFactory().resolveParameterBindType( retType );
		}
		return type;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setProperties(Map map) {
		for ( String paramName : getParameterMetadata().getNamedParameterNames() ) {
			final Object object = map.get( paramName );
			if ( object == null ) {
				if ( map.containsKey( paramName ) ) {
					setParameter( paramName, null, determineType( paramName, null ) );
				}
			}
			else {
				Class retType = object.getClass();
				if ( Collection.class.isAssignableFrom( retType ) ) {
					setParameterList( paramName, (Collection) object );
				}
				else if ( retType.isArray() ) {
					setParameterList( paramName, (Object[]) object );
				}
				else {
					setParameter( paramName, object, determineType( paramName, retType ) );
				}
			}
		}
		return this;
	}

	protected void afterQuery() {
		if ( sessionFlushMode != null ) {
			getSession().setHibernateFlushMode( sessionFlushMode );
			sessionFlushMode = null;
		}
		if ( sessionCacheMode != null ) {
			getSession().setCacheMode( sessionCacheMode );
			sessionCacheMode = null;
		}
	}

	@Override
	public List<R> list() {
		beforeQuery();
		try {
			return doList();
		}
		catch (IllegalQueryOperationException e) {
			throw new IllegalStateException( e );
		}
		catch (TypeMismatchException e) {
			throw new IllegalArgumentException( e );
		}
		catch (HibernateException he) {
			throw getSession().getExceptionConverter().convert( he );
		}
		finally {
			afterQuery();
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
			if ( list.size() == 0 ) {
				throw new NoResultException( "No entity found for query" );
			}
			return uniqueElement( list );
		}
		catch ( HibernateException e ) {
			if ( getSession().getFactory().getSessionFactoryOptions().isJpaBootstrap() ) {
				throw getSession().getExceptionConverter().convert( e );
			}
			else {
				throw e;
			}
		}
	}

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
	public ScrollableResultsImplementor scroll() {
		return scroll( getSession().getFactory().getJdbcServices().getJdbcEnvironment().getDialect().defaultScrollMode() );
	}

	@Override
	public ScrollableResultsImplementor scroll(ScrollMode scrollMode) {
		beforeQuery();
		try {
			return doScroll( scrollMode );
		}
		finally {
			afterQuery();
		}
	}

	protected abstract ScrollableResultsImplementor doScroll(ScrollMode scrollMode);

	@Override
	@SuppressWarnings("unchecked")
	public Stream<R> stream() {
		final ScrollableResultsImplementor scrollableResults = scroll( ScrollMode.FORWARD_ONLY );
		final ScrollableResultsIterator<R> iterator = new ScrollableResultsIterator<>( scrollableResults );
		final Spliterator<R> spliterator = Spliterators.spliteratorUnknownSize( iterator, Spliterator.NONNULL );

		final Stream<R> stream = StreamSupport.stream( spliterator, false );
		return stream.onClose( scrollableResults::close );
	}

	@Override
	public int executeUpdate() throws HibernateException {
		if ( !getSession().isTransactionInProgress() ) {
			throw getSession().getExceptionConverter().convert(
					new TransactionRequiredException(
							"Executing an update/delete query"
					)
			);
		}
		beforeQuery();
		try {
			return doExecuteUpdate();
		}
		catch (IllegalQueryOperationException e) {
			throw new IllegalStateException( e );
		}
		catch( TypeMismatchException e ) {
			throw new IllegalArgumentException( e );
		}
		catch ( HibernateException e) {
			if ( getSession().getFactory().getSessionFactoryOptions().isJpaBootstrap() ) {
				throw getSession().getExceptionConverter().convert( e );
			}
			else {
				throw e;
			}
		}
		finally {
			afterQuery();
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
