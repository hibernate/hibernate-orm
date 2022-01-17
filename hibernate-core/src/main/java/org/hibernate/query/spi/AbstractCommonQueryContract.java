/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphParser;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.internal.RootGraphImpl;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.jpa.QueryHints;
import org.hibernate.jpa.internal.util.ConfigurationHelper;
import org.hibernate.jpa.internal.util.FlushModeTypeHelper;
import org.hibernate.jpa.internal.util.LockModeTypeHelper;
import org.hibernate.metamodel.RuntimeMetamodels;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.BindableType;
import org.hibernate.query.CommonQueryContract;
import org.hibernate.query.QueryLogging;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.TypedParameterValue;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;

import static java.util.Locale.ROOT;
import static org.hibernate.annotations.QueryHints.NATIVE_LOCKMODE;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_SHARED_CACHE_RETRIEVE_MODE;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_SHARED_CACHE_STORE_MODE;
import static org.hibernate.cfg.AvailableSettings.JPA_LOCK_TIMEOUT;
import static org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_RETRIEVE_MODE;
import static org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_STORE_MODE;
import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;
import static org.hibernate.jpa.QueryHints.HINT_CACHEABLE;
import static org.hibernate.jpa.QueryHints.HINT_CACHE_MODE;
import static org.hibernate.jpa.QueryHints.HINT_CACHE_REGION;
import static org.hibernate.jpa.QueryHints.HINT_COMMENT;
import static org.hibernate.jpa.QueryHints.HINT_FETCHGRAPH;
import static org.hibernate.jpa.QueryHints.HINT_FETCH_SIZE;
import static org.hibernate.jpa.QueryHints.HINT_FLUSH_MODE;
import static org.hibernate.jpa.QueryHints.HINT_FOLLOW_ON_LOCKING;
import static org.hibernate.jpa.QueryHints.HINT_LOADGRAPH;
import static org.hibernate.jpa.QueryHints.HINT_NATIVE_LOCKMODE;
import static org.hibernate.jpa.QueryHints.HINT_NATIVE_SPACES;
import static org.hibernate.jpa.QueryHints.HINT_READONLY;
import static org.hibernate.jpa.QueryHints.HINT_TIMEOUT;
import static org.hibernate.jpa.QueryHints.JAKARTA_HINT_FETCH_GRAPH;
import static org.hibernate.jpa.QueryHints.JAKARTA_HINT_LOAD_GRAPH;
import static org.hibernate.jpa.QueryHints.SPEC_HINT_TIMEOUT;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCommonQueryContract implements CommonQueryContract {
	private final SharedSessionContractImplementor session;
	private final QueryOptionsImpl queryOptions = new QueryOptionsImpl();

	public AbstractCommonQueryContract(SharedSessionContractImplementor session) {
		this.session = session;
	}

	public SharedSessionContractImplementor getSession() {
		return session;
	}

	protected int getIntegerLiteral(JpaExpression<Number> expression, int defaultValue) {
		if ( expression == null ) {
			return defaultValue;
		}

		if ( expression instanceof SqmLiteral<?> ) {
			return ( (SqmLiteral<Number>) expression ).getLiteralValue().intValue();
		}
		else if ( expression instanceof Parameter<?> ) {
			final Number parameterValue = getParameterValue( (Parameter<Number>) expression );
			return parameterValue == null ? defaultValue : parameterValue.intValue();
		}
		throw new IllegalArgumentException( "Can't get integer literal value from: " + expression );
	}

	protected int getMaxRows(SqmSelectStatement<?> selectStatement, int size) {
		final JpaExpression<Number> expression = selectStatement.getFetch();
		if ( expression == null ) {
			return -1;
		}

		final Number fetchValue;
		if ( expression instanceof SqmLiteral<?> ) {
			fetchValue = ( (SqmLiteral<Number>) expression ).getLiteralValue();
		}
		else if ( expression instanceof SqmParameter<?> ) {
			fetchValue = getParameterValue( (Parameter<Number>) expression );
			if ( fetchValue == null ) {
				return -1;
			}
		}
		else {
			throw new IllegalArgumentException( "Can't get max rows value from: " + expression );
		}
		// Note that we can never have ties because this is only used when we de-duplicate results
		switch ( selectStatement.getFetchClauseType() ) {
			case ROWS_ONLY:
			case ROWS_WITH_TIES:
				return fetchValue.intValue();
			case PERCENT_ONLY:
			case PERCENT_WITH_TIES:
				return (int) Math.ceil( ( ( (double) size ) * fetchValue.doubleValue() ) / 100d );
		}
		throw new UnsupportedOperationException( "Unsupported fetch clause type: " + selectStatement.getFetchClauseType() );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Hints

	public Map<String, Object> getHints() {
		// According to the JPA spec, this should technically force a rollback, but
		// that's insane :)
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
			hints.put( SPEC_HINT_TIMEOUT, getQueryOptions().getTimeout() * 1000 );
		}

		putIfNotNull( hints, HINT_FETCH_SIZE, getQueryOptions().getFetchSize() );
		putIfNotNull( hints, HINT_FLUSH_MODE, getHibernateFlushMode() );

		if ( getCacheMode() != null ) {
			putIfNotNull( hints, HINT_CACHE_MODE, getCacheMode() );
			putIfNotNull( hints, JAKARTA_SHARED_CACHE_RETRIEVE_MODE, getQueryOptions().getCacheRetrieveMode() );
			putIfNotNull( hints, JAKARTA_SHARED_CACHE_STORE_MODE, getQueryOptions().getCacheStoreMode() );
			putIfNotNull( hints, JPA_SHARED_CACHE_RETRIEVE_MODE, getQueryOptions().getCacheRetrieveMode() );
			putIfNotNull( hints, JPA_SHARED_CACHE_STORE_MODE, getQueryOptions().getCacheStoreMode() );
		}

		if ( isCacheable() ) {
			hints.put( HINT_CACHEABLE, true );
			putIfNotNull( hints, HINT_CACHE_REGION, getCacheRegion() );
		}

		if ( isReadOnly() ) {
			hints.put( HINT_READONLY, true );
		}

		putIfNotNull( hints, HINT_COMMENT, getComment() );

		if ( getQueryOptions().getAppliedGraph() != null && getQueryOptions().getAppliedGraph().getSemantic() != null ) {
			hints.put(
					getQueryOptions().getAppliedGraph().getSemantic().getJpaHintName(),
					getQueryOptions().getAppliedGraph().getGraph()
			);
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

	@SuppressWarnings("deprecation")
	public boolean applyHint(String hintName, Object value) {
		getSession().checkOpen( true );

		boolean applied = false;
		try {
			if ( HINT_TIMEOUT.equals( hintName ) ) {
				applied = applyTimeout( ConfigurationHelper.getInteger( value ) );
			}
			else if ( SPEC_HINT_TIMEOUT.equals( hintName ) ) {
				// convert milliseconds to seconds
				int timeout = (int)Math.round( ConfigurationHelper.getInteger( value ).doubleValue() / 1000.0 );
				applied = applyTimeout( timeout );
			}
			else if ( JPA_LOCK_TIMEOUT.equals( hintName ) ) {
				applied = applyLockTimeout( ConfigurationHelper.getInteger( value ) );
			}
			else if ( HINT_COMMENT.equals( hintName ) ) {
				applied = applyComment( (String) value );
			}
			else if ( HINT_FETCH_SIZE.equals( hintName ) ) {
				applied = applyFetchSize( ConfigurationHelper.getInteger( value ) );
			}
			else if ( HINT_CACHEABLE.equals( hintName ) ) {
				applied = applyCacheable( ConfigurationHelper.getBoolean( value ) );
			}
			else if ( HINT_CACHE_REGION.equals( hintName ) ) {
				applied = applyCacheRegion( (String) value );
			}
			else if ( HINT_READONLY.equals( hintName ) ) {
				applied = applyReadOnly( ConfigurationHelper.getBoolean( value ) );
			}
			else if ( HINT_FLUSH_MODE.equals( hintName ) ) {
				applied = applyFlushMode( ConfigurationHelper.getFlushMode( value ) );
			}
			else if ( HINT_CACHE_MODE.equals( hintName ) ) {
				applied = applyCacheMode( ConfigurationHelper.getCacheMode( value ) );
			}
			else if ( JAKARTA_SHARED_CACHE_RETRIEVE_MODE.equals( hintName ) ) {
				final CacheRetrieveMode retrieveMode = value != null ? CacheRetrieveMode.valueOf( value.toString() ) : null;
				applied = applyJpaCacheRetrieveMode( retrieveMode );
			}
			else if ( JAKARTA_SHARED_CACHE_STORE_MODE.equals( hintName ) ) {
				final CacheStoreMode storeMode = value != null ? CacheStoreMode.valueOf( value.toString() ) : null;
				applied = applyJpaCacheStoreMode( storeMode );
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
			else if ( hintName.startsWith( NATIVE_LOCKMODE ) ) {
				// extract the alias
				final String alias = hintName.substring( NATIVE_LOCKMODE.length() + 1 );
				// determine the LockMode
				try {
					final LockMode lockMode = LockModeTypeHelper.interpretLockMode( value );
					applied = applyAliasSpecificLockMode( alias, lockMode );
				}
				catch ( Exception e ) {
					QueryLogging.QUERY_MESSAGE_LOGGER.unableToDetermineLockModeValue( hintName, value );
				}
			}
			else if ( JAKARTA_HINT_FETCH_GRAPH.equals( hintName ) || JAKARTA_HINT_LOAD_GRAPH.equals( hintName ) ) {
				if ( value instanceof RootGraphImplementor ) {
					applied = applyEntityGraphQuery( hintName, (RootGraphImplementor<?>) value );
				}
				else if ( value instanceof String ) {
					// https://hibernate.atlassian.net/browse/HHH-14855
					applied = applyEntityGraphQuery( hintName, (String) value );
				}
				else {
					QueryLogging.QUERY_LOGGER.debugf( "The %s hint was set, but the value was neither an EntityGraph nor String", hintName );
				}
			}
			else if ( HINT_FETCHGRAPH.equals( hintName ) || HINT_LOADGRAPH.equals( hintName ) ) {
				if ( HINT_FETCHGRAPH.equals( hintName ) ) {
					DEPRECATION_LOGGER.deprecatedSetting( HINT_FETCHGRAPH, JAKARTA_HINT_FETCH_GRAPH );
				}
				else {
					DEPRECATION_LOGGER.deprecatedSetting( HINT_FETCHGRAPH, JAKARTA_HINT_FETCH_GRAPH );
				}

				if ( value instanceof RootGraphImplementor ) {
					applied = applyEntityGraphQuery( hintName, (RootGraphImplementor<?>) value );
				}
				else {
					QueryLogging.QUERY_LOGGER.debugf( "The %s hint was set, but the value was not an EntityGraph!", hintName );
				}
			}
			else if ( HINT_FOLLOW_ON_LOCKING.equals( hintName ) ) {
				applied = applyFollowOnLocking( ConfigurationHelper.getBoolean( value ) );
			}
			else if ( HINT_NATIVE_SPACES.equals( hintName ) ) {
				applied = applySynchronizeSpaces( value );
			}
			else {
				QueryLogging.QUERY_MESSAGE_LOGGER.ignoringUnrecognizedQueryHint( hintName );
			}
		}
		catch ( ClassCastException e ) {
			throw new IllegalArgumentException( "Value for Query hint", e );
		}

		if ( !applied ) {
			QueryLogging.QUERY_LOGGER.debugf( "Skipping unsupported query hint [%s]", hintName );
		}

		return applied;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Options

	public String getComment() {
		return getQueryOptions().getComment();
	}

	public CommonQueryContract setComment(String comment) {
		getQueryOptions().setComment( comment );
		return this;
	}

	@Override
	public FlushMode getHibernateFlushMode() {
		return getQueryOptions().getFlushMode();
	}

	@Override
	public CommonQueryContract setHibernateFlushMode(FlushMode flushMode) {
		getQueryOptions().setFlushMode( flushMode );
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
		if ( value instanceof LockMode ) {
			applyHibernateLockMode( (LockMode) value );
		}
		else if ( value instanceof LockModeType ) {
			applyLockModeType( (LockModeType) value );
		}
		else if ( value instanceof String ) {
			applyHibernateLockMode( LockModeTypeHelper.interpretLockMode( value ) );
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

		return true;
	}

	protected boolean applySynchronizeSpaces(Object value) {
		throw new UnsupportedOperationException( "Explicit query-spaces not supported for non-native queries" );
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
	 * Apply the comment hint.
	 *
	 * @param comment The comment specified as a hint
	 * @return {@code true} if the hint was "applied"
	 */
	@SuppressWarnings("WeakerAccess")
	protected boolean applyComment(String comment) {
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
	protected boolean applyFetchSize(int fetchSize) {
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
	protected boolean applyCacheable(boolean isCacheable) {
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
	protected boolean applyCacheRegion(String regionName) {
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
	protected boolean applyReadOnly(boolean isReadOnly) {
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
	protected boolean applyCacheMode(CacheMode cacheMode) {
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
	protected boolean applyFlushMode(FlushMode flushMode) {
		setHibernateFlushMode( flushMode );
		return true;
	}

	@SuppressWarnings("UnusedReturnValue")
	protected boolean applyLockModeType(LockModeType lockModeType) {
		applyJpaLockMode( lockModeType );
		return true;
	}

	@SuppressWarnings({ "UnusedReturnValue" })
	protected boolean applyHibernateLockMode(LockMode lockMode) {
		applyLockMode( lockMode );
		return true;
	}

	@SuppressWarnings({ "UnusedReturnValue" })
	protected boolean applyLockTimeout(int timeout) {
		getQueryOptions().getLockOptions().setTimeOut( timeout );
		return true;
	}

	@SuppressWarnings("WeakerAccess")
	protected boolean applyAliasSpecificLockMode(String alias, LockMode lockMode) {
		applyLockMode( alias, lockMode );
		return true;
	}

	/**
	 * Apply the follow-on-locking hint.
	 *
	 * @param followOnLocking The follow-on-locking strategy.
	 */
	protected boolean applyFollowOnLocking(Boolean followOnLocking) {
		getQueryOptions().getLockOptions().setFollowOnLocking( followOnLocking );
		return true;
	}

	protected boolean applyEntityGraphQuery(String hintName, RootGraphImplementor<?> entityGraph) {
		final GraphSemantic graphSemantic = GraphSemantic.fromJpaHintName( hintName );
		return applyGraph( entityGraph, graphSemantic );
	}

	protected boolean applyGraph(RootGraphImplementor<?> entityGraph, GraphSemantic graphSemantic) {
		getQueryOptions().applyGraph( entityGraph, graphSemantic );
		return true;
	}

	protected boolean applyEntityGraphQuery(String hintName, String entityGraphString) {
		final int separatorPosition = entityGraphString.indexOf( '(' );
		final int terminatorPosition = entityGraphString.lastIndexOf( ')' );
		if ( separatorPosition < 0 || terminatorPosition < 0 ) {
			throw new IllegalArgumentException(
					String.format(
							ROOT,
							"Invalid entity-graph definition `%s`; expected form `${EntityName}( ${property1} ... )",
							entityGraphString
					)
			);
		}

		final RuntimeMetamodels runtimeMetamodels = getSession().getFactory().getRuntimeMetamodels();
		final JpaMetamodel jpaMetamodel = runtimeMetamodels.getJpaMetamodel();

		final GraphSemantic graphSemantic = GraphSemantic.fromJpaHintName( hintName );
		final String entityName = runtimeMetamodels.getImportedName( entityGraphString.substring( 0, separatorPosition ).trim() );
		final String graphNodes = entityGraphString.substring( separatorPosition + 1, terminatorPosition );

		final RootGraphImpl<?> rootGraph = new RootGraphImpl<>( null, jpaMetamodel.entity( entityName ), jpaMetamodel );
		GraphParser.parseInto( (EntityGraph<?>) rootGraph, graphNodes, getSession().getSessionFactory() );
		applyGraph( rootGraph, graphSemantic );

		return true;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Options

	protected MutableQueryOptions getQueryOptions() {
		return queryOptions;
	}

	public int getMaxResults() {
		getSession().checkOpen();
		return getQueryOptions().getLimit().getMaxRowsJpa();
	}

	public void applyMaxResults(int maxResult) {
		if ( maxResult < 0 ) {
			throw new IllegalArgumentException( "max-results cannot be negative" );
		}
		getSession().checkOpen();
		getQueryOptions().getLimit().setMaxRows( maxResult );
	}

	public int getFirstResult() {
		getSession().checkOpen();
		return getQueryOptions().getLimit().getFirstRowJpa();
	}

	public void applyFirstResult(int startPosition) {
		if ( startPosition < 0 ) {
			throw new IllegalArgumentException( "first-result value cannot be negative : " + startPosition );
		}

		getSession().checkOpen();
		getQueryOptions().getLimit().setFirstRow( startPosition );
	}

	protected FlushModeType getJpaFlushMode() {
		getSession().checkOpen();
		final FlushMode flushMode = getQueryOptions().getFlushMode() == null
				? getSession().getHibernateFlushMode()
				: getQueryOptions().getFlushMode();
		return FlushModeTypeHelper.getFlushModeType( flushMode );
	}

	protected void applyJpaFlushMode(FlushModeType flushModeType) {
		getSession().checkOpen();
		setHibernateFlushMode( FlushModeTypeHelper.getFlushMode( flushModeType ) );
	}

	@Override
	public CacheMode getCacheMode() {
		return getQueryOptions().getCacheMode();
	}

	@Override
	public CommonQueryContract setCacheMode(CacheMode cacheMode) {
		getQueryOptions().setCacheMode( cacheMode );
		return this;
	}

	@Override
	public boolean isCacheable() {
		return getQueryOptions().isResultCachingEnabled() == Boolean.TRUE;
	}

	@Override
	public CommonQueryContract setCacheable(boolean cacheable) {
		getQueryOptions().setResultCachingEnabled( cacheable );
		return this;
	}

	@Override
	public String getCacheRegion() {
		return getQueryOptions().getResultCacheRegionName();
	}

	@Override
	public CommonQueryContract setCacheRegion(String cacheRegion) {
		getQueryOptions().setResultCacheRegionName( cacheRegion );
		return this;
	}

	@Override
	public Integer getTimeout() {
		return getQueryOptions().getTimeout();
	}

	@Override
	public CommonQueryContract setTimeout(int timeout) {
		getQueryOptions().setTimeout( timeout );
		return this;
	}

	protected LockModeType getJpaLockMode() {
		getSession().checkOpen( false );
		return LockModeTypeHelper.getLockModeType( getQueryOptions().getLockOptions().getLockMode() );
	}

	protected void applyJpaLockMode(LockModeType lockModeType) {
		getSession().checkOpen();
		getQueryOptions().getLockOptions().setLockMode( LockModeTypeHelper.getLockMode( lockModeType ) );
	}

	protected void applyLockOptions(LockOptions lockOptions) {
		getQueryOptions().getLockOptions().setLockMode( lockOptions.getLockMode() );
		getQueryOptions().getLockOptions().setScope( lockOptions.getScope() );
		getQueryOptions().getLockOptions().setTimeOut( lockOptions.getTimeOut() );
		getQueryOptions().getLockOptions().setFollowOnLocking( lockOptions.getFollowOnLocking() );
	}

	protected void applyLockMode(LockMode lockMode) {
		getQueryOptions().getLockOptions().setLockMode( lockMode );
	}

	protected void applyLockMode(String alias, LockMode lockMode) {
		getQueryOptions().getLockOptions().setAliasSpecificLockMode( alias, lockMode );
	}

	@Override
	public Integer getFetchSize() {
		return getQueryOptions().getFetchSize();
	}

	@Override
	public CommonQueryContract setFetchSize(int fetchSize) {
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
	public CommonQueryContract setReadOnly(boolean readOnly) {
		getQueryOptions().setReadOnly( readOnly );
		return this;
	}

	@SuppressWarnings( "rawtypes" )
	public void applyTupleTransformer(TupleTransformer transformer) {
		getQueryOptions().setTupleTransformer( transformer );
	}

	public void applyResultListTransformer(ResultListTransformer transformer) {
		getQueryOptions().setResultListTransformer( transformer );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter handling

	protected abstract ParameterMetadataImplementor getParameterMetadata();

	@SuppressWarnings( {"unchecked", "rawtypes"} )
	public Set<Parameter<?>> getParameters() {
		getSession().checkOpen( false );
		return (Set) getParameterMetadata().getRegistrations();
	}

	public QueryParameter<?> getParameter(String name) {
		getSession().checkOpen( false );

		try {
			return getParameterMetadata().getQueryParameter( name );
		}
		catch ( HibernateException e ) {
			throw getSession().getExceptionConverter().convert( e );
		}
	}

	@SuppressWarnings("unchecked")
	public <T> QueryParameter<T> getParameter(String name, Class<T> type) {
		getSession().checkOpen( false );

		try {
			//noinspection rawtypes
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

	public QueryParameter<?> getParameter(int position) {
		getSession().checkOpen( false );

		try {
			return getParameterMetadata().getQueryParameter( position );
		}
		catch ( HibernateException e ) {
			throw getSession().getExceptionConverter().convert( e );
		}
	}

	@SuppressWarnings( {"unchecked", "rawtypes"} )
	public <T> QueryParameter<T> getParameter(int position, Class<T> type) {
		getSession().checkOpen( false );

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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter binding handling

	protected abstract QueryParameterBindings getQueryParameterBindings();
	protected abstract boolean resolveJdbcParameterTypeIfNecessary();

	protected <P> QueryParameterBinding<P> locateBinding(Parameter<P> parameter) {
		if ( parameter instanceof QueryParameterImplementor ) {
			return locateBinding( (QueryParameterImplementor<P>) parameter );
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

	public boolean isBound(Parameter<?> param) {
		getSession().checkOpen();
		final QueryParameterImplementor<?> qp = getParameterMetadata().resolve( param );
		return qp != null && getQueryParameterBindings().isBound( qp );
	}

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
	public CommonQueryContract setParameter(String name, Object value) {
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
	public <P> CommonQueryContract setParameter(String name, P value, Class<P> javaType) {
		final JavaType<P> javaDescriptor = getSession().getFactory()
				.getTypeConfiguration()
				.getJavaTypeRegistry()
				.getDescriptor( javaType );
		if ( javaDescriptor == null ) {
			setParameter( name, value );
		}
		else {
			final BindableType<P> paramType;
			final BasicType<P> basicType = getSession().getFactory().getTypeConfiguration().standardBasicTypeForJavaType( javaType );
			if ( basicType != null ) {
				paramType = basicType;
			}
			else {
				final ManagedDomainType<P> managedDomainType = getSession().getFactory()
						.getRuntimeMetamodels()
						.getJpaMetamodel()
						.managedType( javaType );
				if ( managedDomainType != null ) {
					paramType = managedDomainType;
				}
				else {
					throw new HibernateException( "Unable to determine BindableType : " + javaType.getName() );
				}
			}

			setParameter( name, value, paramType );
		}

		return this;
	}

	@Override
	public <P> CommonQueryContract setParameter(String name, P value, BindableType<P> type) {
		this.<P>locateBinding( name ).setBindValue( value, type );
		return this;
	}

	@Override
	public CommonQueryContract setParameter(String name, Instant value, TemporalType temporalType) {
		this.locateBinding( name ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public CommonQueryContract setParameter(int position, Object value) {
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
	public <P> CommonQueryContract setParameter(int position, P value, Class<P> javaType) {
		final JavaType<P> javaDescriptor = getSession().getFactory()
				.getTypeConfiguration()
				.getJavaTypeRegistry()
				.getDescriptor( javaType );
		if ( javaDescriptor == null ) {
			setParameter( position, value );
		}
		else {
			final BindableType<P> paramType;
			final BasicType<P> basicType = getSession().getFactory().getTypeConfiguration().standardBasicTypeForJavaType( javaType );
			if ( basicType != null ) {
				paramType = basicType;
			}
			else {
				final ManagedDomainType<P> managedDomainType = getSession().getFactory()
						.getRuntimeMetamodels()
						.getJpaMetamodel()
						.managedType( javaType );
				if ( managedDomainType != null ) {
					paramType = managedDomainType;
				}
				else {
					throw new HibernateException( "Unable to determine BindableType : " + javaType.getName() );
				}
			}

			setParameter( position, value, paramType );
		}

		return this;
	}

	@Override
	public <P> CommonQueryContract setParameter(int position, P value, BindableType<P> type) {
		this.<P>locateBinding( position ).setBindValue( value, type );
		return this;
	}

	@Override
	public CommonQueryContract setParameter(int position, Instant value, TemporalType temporalType) {
		this.locateBinding( position ).setBindValue( value, temporalType );
		return this;
	}



	@Override
	public <P> CommonQueryContract setParameter(QueryParameter<P> parameter, P value) {
		locateBinding( parameter ).setBindValue( value, resolveJdbcParameterTypeIfNecessary() );
		return this;
	}

	@Override
	public <P> CommonQueryContract setParameter(QueryParameter<P> parameter, P value, Class<P> javaType) {
		final JavaType<P> javaDescriptor = getSession().getFactory()
				.getTypeConfiguration()
				.getJavaTypeRegistry()
				.getDescriptor( javaType );
		if ( javaDescriptor == null ) {
			setParameter( parameter, value );
		}
		else {
			final BindableType<P> paramType;
			final BasicType<P> basicType = getSession().getFactory().getTypeConfiguration().standardBasicTypeForJavaType( javaType );
			if ( basicType != null ) {
				paramType = basicType;
			}
			else {
				final ManagedDomainType<P> managedDomainType = getSession().getFactory()
						.getRuntimeMetamodels()
						.getJpaMetamodel()
						.managedType( javaType );
				if ( managedDomainType != null ) {
					paramType = managedDomainType;
				}
				else {
					throw new HibernateException( "Unable to determine BindableType : " + javaType.getName() );
				}
			}

			setParameter( parameter, value, paramType );
		}

		return this;
	}

	@Override
	public <P> CommonQueryContract setParameter(QueryParameter<P> parameter, P value, BindableType<P> type) {
		locateBinding( parameter ).setBindValue( value,  type );
		return this;
	}


	@Override
	public <P> CommonQueryContract setParameter(Parameter<P> parameter, P value) {
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


	@Override
	public CommonQueryContract setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		locateBinding( param ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public CommonQueryContract setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		locateBinding( param ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public CommonQueryContract setParameter(String name, Calendar value, TemporalType temporalType) {
		locateBinding( name ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public CommonQueryContract setParameter(String name, Date value, TemporalType temporalType) {
		locateBinding( name ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public CommonQueryContract setParameter(int position, Calendar value, TemporalType temporalType) {
		locateBinding( position ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public CommonQueryContract setParameter(int position, Date value, TemporalType temporalType) {
		locateBinding( position ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public CommonQueryContract setParameterList(String name, @SuppressWarnings("rawtypes") Collection values) {
		locateBinding( name ).setBindValues( values );
		return this;
	}

	public <P> CommonQueryContract setParameterList(String name, Collection<? extends P> values, Class<P> javaType) {
		final JavaType<P> javaDescriptor = getSession().getFactory()
				.getTypeConfiguration()
				.getJavaTypeRegistry()
				.getDescriptor( javaType );
		if ( javaDescriptor == null ) {
			setParameterList( name, values );
		}
		else {
			final BindableType<P> paramType;
			final BasicType<P> basicType = getSession().getFactory().getTypeConfiguration().standardBasicTypeForJavaType( javaType );
			if ( basicType != null ) {
				paramType = basicType;
			}
			else {
				final ManagedDomainType<P> managedDomainType = getSession().getFactory()
						.getRuntimeMetamodels()
						.getJpaMetamodel()
						.managedType( javaType );
				if ( managedDomainType != null ) {
					paramType = managedDomainType;
				}
				else {
					throw new HibernateException( "Unable to determine BindableType : " + javaType.getName() );
				}
			}

			setParameterList( name, values, paramType );
		}

		return this;
	}


	@Override
	public <P> CommonQueryContract setParameterList(String name, Collection<? extends P> values, BindableType<P> type) {
		this.<P>locateBinding( name ).setBindValues( values, type );
		return this;
	}

	@Override
	public CommonQueryContract setParameterList(String name, Object[] values) {
		locateBinding( name ).setBindValues( Arrays.asList( values ) );
		return this;
	}

	@Override
	public <P> CommonQueryContract setParameterList(String name, P[] values, Class<P> javaType) {
		final JavaType<P> javaDescriptor = getSession().getFactory()
				.getTypeConfiguration()
				.getJavaTypeRegistry()
				.getDescriptor( javaType );
		if ( javaDescriptor == null ) {
			setParameterList( name, values );
		}
		else {
			final BindableType<P> paramType;
			final BasicType<P> basicType = getSession().getFactory().getTypeConfiguration().standardBasicTypeForJavaType( javaType );
			if ( basicType != null ) {
				paramType = basicType;
			}
			else {
				final ManagedDomainType<P> managedDomainType = getSession().getFactory()
						.getRuntimeMetamodels()
						.getJpaMetamodel()
						.managedType( javaType );
				if ( managedDomainType != null ) {
					paramType = managedDomainType;
				}
				else {
					throw new HibernateException( "Unable to determine BindableType : " + javaType.getName() );
				}
			}

			setParameterList( name, values, paramType );
		}

		return this;
	}

	public <P> CommonQueryContract setParameterList(String name, P[] values, BindableType<P> type) {
		this.<P>locateBinding( name ).setBindValues( Arrays.asList( values ), type );
		return this;
	}

	@Override
	public CommonQueryContract setParameterList(int position, @SuppressWarnings("rawtypes") Collection values) {
		locateBinding( position ).setBindValues( values );
		return this;
	}

	@Override
	public <P> CommonQueryContract setParameterList(int position, Collection<? extends P> values, Class<P> javaType) {
		final JavaType<P> javaDescriptor = getSession().getFactory()
				.getTypeConfiguration()
				.getJavaTypeRegistry()
				.getDescriptor( javaType );
		if ( javaDescriptor == null ) {
			setParameterList( position, values );
		}
		else {
			final BindableType<P> paramType;
			final BasicType<P> basicType = getSession().getFactory().getTypeConfiguration().standardBasicTypeForJavaType( javaType );
			if ( basicType != null ) {
				paramType = basicType;
			}
			else {
				final ManagedDomainType<P> managedDomainType = getSession().getFactory()
						.getRuntimeMetamodels()
						.getJpaMetamodel()
						.managedType( javaType );
				if ( managedDomainType != null ) {
					paramType = managedDomainType;
				}
				else {
					throw new HibernateException( "Unable to determine BindableType : " + javaType.getName() );
				}
			}

			setParameterList( position, values, paramType );
		}

		return this;
	}

	@Override
	public <P> CommonQueryContract setParameterList(int position, Collection<? extends P> values, BindableType<P> type) {
		this.<P>locateBinding( position ).setBindValues( values, type );
		return this;
	}

	@Override
	public CommonQueryContract setParameterList(int position, Object[] values) {
		locateBinding( position ).setBindValues( Arrays.asList( values ) );
		return this;
	}

	@Override
	public <P> CommonQueryContract setParameterList(int position, P[] values, Class<P> javaType) {
		final JavaType<P> javaDescriptor = getSession().getFactory()
				.getTypeConfiguration()
				.getJavaTypeRegistry()
				.getDescriptor( javaType );
		if ( javaDescriptor == null ) {
			setParameterList( position, values );
		}
		else {
			final BindableType<P> paramType;
			final BasicType<P> basicType = getSession().getFactory().getTypeConfiguration().standardBasicTypeForJavaType( javaType );
			if ( basicType != null ) {
				paramType = basicType;
			}
			else {
				final ManagedDomainType<P> managedDomainType = getSession().getFactory()
						.getRuntimeMetamodels()
						.getJpaMetamodel()
						.managedType( javaType );
				if ( managedDomainType != null ) {
					paramType = managedDomainType;
				}
				else {
					throw new HibernateException( "Unable to determine BindableType : " + javaType.getName() );
				}
			}

			setParameterList( position, values, paramType );
		}

		return this;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <P> CommonQueryContract setParameterList(int position, P[] values, BindableType<P> type) {
		locateBinding( position ).setBindValues( Arrays.asList( values ), (BindableType) type );
		return this;
	}


	@Override
	public <P> CommonQueryContract setParameterList(QueryParameter<P> parameter, Collection<? extends P> values) {
		locateBinding( parameter ).setBindValues( values );
		return this;
	}

	@Override
	public <P> CommonQueryContract setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType) {
		final JavaType<P> javaDescriptor = getSession().getFactory()
				.getTypeConfiguration()
				.getJavaTypeRegistry()
				.getDescriptor( javaType );
		if ( javaDescriptor == null ) {
			setParameterList( parameter, values );
		}
		else {
			final BindableType<P> paramType;
			final BasicType<P> basicType = getSession().getFactory().getTypeConfiguration().standardBasicTypeForJavaType( javaType );
			if ( basicType != null ) {
				paramType = basicType;
			}
			else {
				final ManagedDomainType<P> managedDomainType = getSession().getFactory()
						.getRuntimeMetamodels()
						.getJpaMetamodel()
						.managedType( javaType );
				if ( managedDomainType != null ) {
					paramType = managedDomainType;
				}
				else {
					throw new HibernateException( "Unable to determine BindableType : " + javaType.getName() );
				}
			}

			setParameterList( parameter, values, paramType );
		}

		return this;
	}

	@Override
	public <P> CommonQueryContract setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, BindableType<P> type) {
		locateBinding( parameter ).setBindValues( values, type );
		return this;
	}

	@Override
	public <P> CommonQueryContract setParameterList(QueryParameter<P> parameter, P[] values) {
		locateBinding( parameter ).setBindValues( values == null ? null : Arrays.asList( values ) );
		return this;
	}

	@Override
	public <P> CommonQueryContract setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType) {
		final JavaType<P> javaDescriptor = getSession().getFactory()
				.getTypeConfiguration()
				.getJavaTypeRegistry()
				.getDescriptor( javaType );
		if ( javaDescriptor == null ) {
			setParameterList( parameter, values );
		}
		else {
			final BindableType<P> paramType;
			final BasicType<P> basicType = getSession().getFactory().getTypeConfiguration().standardBasicTypeForJavaType( javaType );
			if ( basicType != null ) {
				paramType = basicType;
			}
			else {
				final ManagedDomainType<P> managedDomainType = getSession().getFactory()
						.getRuntimeMetamodels()
						.getJpaMetamodel()
						.managedType( javaType );
				if ( managedDomainType != null ) {
					paramType = managedDomainType;
				}
				else {
					throw new HibernateException( "Unable to determine BindableType : " + javaType.getName() );
				}
			}

			setParameterList( parameter, values, paramType );
		}

		return this;
	}


	@Override
	public <P> CommonQueryContract setParameterList(QueryParameter<P> parameter, P[] values, BindableType<P> type) {
		locateBinding( parameter ).setBindValues( Arrays.asList( values ), type );
		return this;
	}

	@Override
	public CommonQueryContract setProperties(Map map) {
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
	public CommonQueryContract setProperties(Object bean) {
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
}
