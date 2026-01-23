/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import jakarta.persistence.metamodel.Type;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.Timeouts;
import org.hibernate.engine.spi.ExceptionConverter;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphParser;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.internal.RootGraphImpl;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jpa.internal.util.FlushModeTypeHelper;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.QueryArgumentException;
import org.hibernate.query.QueryFlushMode;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.TypedParameterValue;
import org.hibernate.query.named.NamedQueryMemento;
import org.hibernate.query.named.NamedSelectionMemento;
import org.hibernate.query.spi.CommonQueryContractImplementor;
import org.hibernate.query.spi.MutableQueryOptions;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryInterpretationCache;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.type.BindableType;
import org.hibernate.type.descriptor.converter.internal.ConverterHelper;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;
import static java.util.Locale.ROOT;
import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;
import static org.hibernate.jpa.HibernateHints.HINT_CACHEABLE;
import static org.hibernate.jpa.HibernateHints.HINT_CACHE_MODE;
import static org.hibernate.jpa.HibernateHints.HINT_CACHE_REGION;
import static org.hibernate.jpa.HibernateHints.HINT_CALLABLE_FUNCTION;
import static org.hibernate.jpa.HibernateHints.HINT_CALLABLE_FUNCTION_RETURN_TYPE;
import static org.hibernate.jpa.HibernateHints.HINT_COMMENT;
import static org.hibernate.jpa.HibernateHints.HINT_FETCH_PROFILE;
import static org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE;
import static org.hibernate.jpa.HibernateHints.HINT_FLUSH_MODE;
import static org.hibernate.jpa.HibernateHints.HINT_FOLLOW_ON_LOCKING;
import static org.hibernate.jpa.HibernateHints.HINT_FOLLOW_ON_STRATEGY;
import static org.hibernate.jpa.HibernateHints.HINT_NATIVE_LOCK_MODE;
import static org.hibernate.jpa.HibernateHints.HINT_NATIVE_SPACES;
import static org.hibernate.jpa.HibernateHints.HINT_QUERY_DATABASE;
import static org.hibernate.jpa.HibernateHints.HINT_QUERY_PLAN_CACHEABLE;
import static org.hibernate.jpa.HibernateHints.HINT_TIMEOUT;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_CACHE_RETRIEVE_MODE;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_CACHE_STORE_MODE;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_FETCH_GRAPH;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_LOAD_GRAPH;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_LOCK_TIMEOUT;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_QUERY_TIMEOUT;
import static org.hibernate.jpa.QueryHints.HINT_NATIVE_LOCKMODE;
import static org.hibernate.jpa.QueryHints.HINT_READONLY;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_CACHE_RETRIEVE_MODE;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_CACHE_STORE_MODE;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_FETCH_GRAPH;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_LOAD_GRAPH;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_LOCK_TIMEOUT;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_QUERY_TIMEOUT;
import static org.hibernate.jpa.internal.util.ConfigurationHelper.getBoolean;
import static org.hibernate.jpa.internal.util.ConfigurationHelper.getCacheMode;
import static org.hibernate.jpa.internal.util.ConfigurationHelper.getInteger;
import static org.hibernate.query.QueryLogging.QUERY_MESSAGE_LOGGER;
import static org.hibernate.query.internal.QueryArguments.areInstances;
import static org.hibernate.query.internal.QueryArguments.isInstance;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCommonQueryContract implements CommonQueryContractImplementor {
	protected final SharedSessionContractImplementor session;
	protected final MutableQueryOptions queryOptions;

	public AbstractCommonQueryContract(SharedSessionContractImplementor session) {
		this.session = session;
		this.queryOptions = new QueryOptionsImpl();

		var defaultLockOptions = session.getDefaultLockOptions();
		if ( defaultLockOptions.getLockMode().greaterThan( LockMode.READ ) ) {
			queryOptions.getLockOptions().overlay( defaultLockOptions );
		}

		var defaultLockTimeout = session.getDefaultLockTimeout();
		if ( defaultLockTimeout != null ) {
			queryOptions.getLockOptions().setTimeout( defaultLockTimeout );
		}

		var defaultTimeout = session.getDefaultTimeout();
		if ( defaultTimeout != null ) {
			queryOptions.setTimeout( defaultTimeout );
		}
	}

	protected AbstractCommonQueryContract(AbstractCommonQueryContract original) {
		this.session = original.session;
		this.queryOptions = original.queryOptions;
	}

	protected AbstractCommonQueryContract(
			SharedSessionContractImplementor session,
			MutableQueryOptions queryOptions) {
		this.session = session;
		this.queryOptions = queryOptions;
	}

	protected void applyMementoOptions(NamedQueryMemento<?> memento) {
		if ( memento.getHints() != null ) {
			memento.getHints().forEach( this::setHint );
		}

		if ( memento.getFlushMode() != null ) {
			queryOptions.setFlushMode( memento.getFlushMode() );
		}

		if ( memento.getTimeout() != null ) {
			queryOptions.setTimeout( memento.getTimeout() );
		}

		if ( StringHelper.isNotEmpty( memento.getComment() ) ) {
			queryOptions.setComment( memento.getComment() );
		}

		if ( memento instanceof NamedSelectionMemento<?> selectionMemento ) {
			if ( selectionMemento.getCacheable() != null ) {
				queryOptions.setResultCachingEnabled( selectionMemento.getCacheable() );
			}

			if ( selectionMemento.getCacheRegion() != null ) {
				queryOptions.setResultCacheRegionName( selectionMemento.getCacheRegion() );
			}

			if ( selectionMemento.getCacheMode() != null ) {
				queryOptions.setCacheMode( selectionMemento.getCacheMode() );
			}

			if ( selectionMemento.getReadOnly() != null ) {
				queryOptions.setReadOnly( selectionMemento.getReadOnly() );
			}

			if ( selectionMemento.getFetchSize() != null ) {
				queryOptions.setFetchSize( selectionMemento.getFetchSize() );
			}
		}
	}

	@Override
	public Integer getTimeout() {
		return Timeouts.getEffectiveTimeoutInSeconds( queryOptions.getTimeout() );
	}

	@Override
	public CommonQueryContractImplementor setTimeout(int timeout) {
		session.checkOpen();
		queryOptions.setTimeout( timeout );
		return this;
	}

	@Override
	public CommonQueryContractImplementor setTimeout(Integer timeout) {
		session.checkOpen();
		queryOptions.setTimeout( timeout );
		return this;
	}

	@Override
	public CommonQueryContractImplementor setTimeout(Timeout timeout) {
		session.checkOpen();
		queryOptions.setTimeout( timeout );
		return this;
	}

	@Override
	public String getComment() {
		return queryOptions.getComment();
	}

	@Override
	public CommonQueryContractImplementor setComment(String comment) {
		session.checkOpen();
		queryOptions.setComment( comment );
		return this;
	}

	@Override
	public CommonQueryContractImplementor addQueryHint(String hint) {
		session.checkOpen();
		queryOptions.addDatabaseHint( hint );
		return this;
	}

	@Override
	public CommonQueryContractImplementor setLockMode(LockModeType lockMode) {
		session.checkOpen();
		queryOptions.getLockOptions().setLockMode( LockMode.fromJpaLockMode( lockMode ) );
		return this;
	}

	@Override
	public LockModeType getLockMode() {
		return queryOptions.getLockOptions().getLockMode().toJpaLockMode();
	}

	@Override
	public FlushModeType getFlushMode() {
		session.checkOpen();
		return FlushModeTypeHelper.getFlushModeType( getEffectiveFlushMode() );
	}

	@Override
	public CommonQueryContractImplementor setFlushMode(FlushModeType flushMode) {
		session.checkOpen();
		queryOptions.setFlushMode( FlushMode.fromJpaFlushMode( flushMode ) );
		return this;
	}

	@Override
	public QueryFlushMode getQueryFlushMode() {
		final FlushMode flushMode = queryOptions.getFlushMode();
		if ( flushMode == null ) {
			return QueryFlushMode.DEFAULT;
		}
		else if ( flushMode == FlushMode.ALWAYS || flushMode == FlushMode.AUTO ) {
			return QueryFlushMode.FLUSH;
		}
		else {
			return QueryFlushMode.NO_FLUSH;
		}
	}

	@Override
	public CommonQueryContractImplementor setQueryFlushMode(QueryFlushMode queryFlushMode) {
		session.checkOpen();
		queryOptions.setFlushMode( FlushModeTypeHelper.getFlushMode( queryFlushMode ) );
		return this;
	}

	@Override
	public FlushMode getEffectiveFlushMode() {
		final FlushMode flushMode = queryOptions.getFlushMode();
		return flushMode == null ? getSession().getHibernateFlushMode() : flushMode;
	}

	@Override
	public CacheRetrieveMode getCacheRetrieveMode() {
		return queryOptions.getCacheRetrieveMode();
	}

	@Override
	public CommonQueryContractImplementor setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
		session.checkOpen();
		queryOptions.setCacheRetrieveMode( cacheRetrieveMode );
		return this;
	}

	@Override
	public CacheStoreMode getCacheStoreMode() {
		return queryOptions.getCacheStoreMode();
	}

	@Override
	public CommonQueryContractImplementor setCacheStoreMode(CacheStoreMode cacheStoreMode) {
		session.checkOpen();
		queryOptions.setCacheStoreMode( cacheStoreMode );
		return this;
	}

	@Override
	public CommonQueryContractImplementor setMaxResults(int maxResult) {
		session.checkOpen();
		queryOptions.getLimit().setMaxRows( maxResult );
		return this;
	}

	@Override
	public int getMaxResults() {
		return queryOptions.getLimit().getMaxRowsJpa();
	}

	@Override
	public CommonQueryContractImplementor setFirstResult(int startPosition) {
		session.checkOpen();
		queryOptions.getLimit().setFirstRow( startPosition );
		return this;
	}

	@Override
	public int getFirstResult() {
		return queryOptions.getLimit().getFirstRowJpa();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Hints

	@Override
	public final Map<String, Object> getHints() {
		// According to the JPA spec, this should force a rollback, but that's insane :)
		// If the TCK ever adds a check for this, we may need to change this behavior
		session.checkOpen( false );
		final Map<String,Object> hints = new HashMap<>();
		collectHints( hints );
		return hints;
	}

	@SuppressWarnings("deprecation")
	protected void collectHints(Map<String, Object> hints) {
		if ( Timeouts.isRealTimeout( queryOptions.getTimeout() ) ) {
			hints.put( HINT_TIMEOUT, Timeouts.getTimeoutInSeconds( queryOptions.getTimeout() ) );
			hints.put( HINT_SPEC_QUERY_TIMEOUT, queryOptions.getTimeout().milliseconds() );
		}

		putIfNotNull( hints, HINT_COMMENT, queryOptions.getComment() );
		putIfNotNull( hints, HINT_FLUSH_MODE,  queryOptions.getFlushMode() );

		putIfNotNull( hints, HINT_READONLY, queryOptions.isReadOnly() );
		putIfNotNull( hints, HINT_FETCH_SIZE, queryOptions.getFetchSize() );
		putIfNotNull( hints, HINT_CACHEABLE, queryOptions.isResultCachingEnabled() );
		putIfNotNull( hints, HINT_CACHE_REGION, queryOptions.getResultCacheRegionName() );
		putIfNotNull( hints, HINT_CACHE_MODE, queryOptions.getCacheMode() );
		putIfNotNull( hints, HINT_QUERY_PLAN_CACHEABLE, queryOptions.getQueryPlanCachingEnabled() );

		putIfNotNull( hints, HINT_SPEC_CACHE_RETRIEVE_MODE, queryOptions.getCacheRetrieveMode() );
		putIfNotNull( hints, HINT_JAVAEE_CACHE_RETRIEVE_MODE, queryOptions.getCacheRetrieveMode() );

		putIfNotNull( hints, HINT_SPEC_CACHE_STORE_MODE, queryOptions.getCacheStoreMode() );
		putIfNotNull( hints, HINT_JAVAEE_CACHE_STORE_MODE, queryOptions.getCacheStoreMode() );

		final var appliedGraph = queryOptions.getAppliedGraph();
		if ( appliedGraph != null ) {
			final var semantic = appliedGraph.getSemantic();
			if ( semantic != null ) {
				hints.put( semantic.getJakartaHintName(), appliedGraph.getGraph() );
				hints.put( semantic.getJpaHintName(), appliedGraph.getGraph() );
			}
		}

		final var lockOptions = queryOptions.getLockOptions();
		if ( lockOptions.getLockMode() != LockMode.READ ) {
			final var lockMode = lockOptions.getLockMode();
			if ( lockMode != null && lockMode != LockMode.NONE ) {
				hints.put( HINT_NATIVE_LOCKMODE, lockMode );
			}

			if ( lockOptions.getFollowOnStrategy() != null ) {
				hints.put( HINT_FOLLOW_ON_STRATEGY, lockOptions.getFollowOnStrategy() );
			}

			if ( lockOptions.getTimeout().milliseconds() != Timeouts.WAIT_FOREVER_MILLI ) {
				hints.put( HINT_SPEC_LOCK_TIMEOUT, lockOptions.getTimeout() );
				hints.put( HINT_JAVAEE_LOCK_TIMEOUT, lockOptions.getTimeout() );
			}
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

	@Override
	public CommonQueryContractImplementor setHint(String hintName, Object value) {
		try {
			if ( !applyHint( hintName, value ) ) {
				QUERY_MESSAGE_LOGGER.ignoringUnrecognizedQueryHint( hintName );
			}
		}
		catch (IllegalArgumentException jpaExpected) {
			throw jpaExpected;
		}
		catch (Exception e) {
			var jpaExpected = new IllegalArgumentException( String.format( ROOT,
					"(error applying query hint `%s`) %s",
					hintName,
					e.getMessage()
			) );
			jpaExpected.addSuppressed( e );
			throw jpaExpected;
		}
		return this;
	}

	/**
	 * This method takes responsibility to recognize all known hints
	 * and collecting the values.
	 * <p>
	 * However, not all hints are valid for all types of queries which
	 * is beyond the understanding of this context.
	 * <p>
	 * Hints which are always relevant are handled by directly applying
	 * the corresponding option.  These include:<ul>
	 *     <li>{@linkplain QueryOptions#getFlushMode() flush mode}
	 *     <li>{@linkplain QueryOptions#getTimeout() timeout}
	 *     <li>{@linkplain QueryOptions#getComment() comment}
	 *     <li>{@linkplain QueryOptions#getDatabaseHints() database hints}
	 * </ul>
	 * <p>
	 * The others are handled by calling "apply hint" methods which allow
	 * subtypes to throw an exception for hints they do not support<ul>
	 *     <li>{@linkplain #applyReadOnlyHint}
	 *     <li>{@linkplain #applyFetchSizeHint}
	 *     <li>{@linkplain #applyResultCachingHint}
	 *     <li>{@linkplain #applyResultCacheModeHint}
	 *     <li>{@linkplain #applyResultCachingRetrieveModeHint}
	 *     <li>{@linkplain #applyResultCachingStoreModeHint}
	 *     <li>{@linkplain #applyResultCachingRegionHint}
	 *     <li>{@linkplain #applyEntityGraphHint}
	 *     <li>{@linkplain #applyEnabledFetchProfileHint}
	 *     <li>{@linkplain #applyLockModeHint}
	 *     <li>{@linkplain #applyLockTimeoutHint}
	 *     <li>{@linkplain #applyFollowOnStrategyHint}
	 *     <li>{@linkplain #applyQueryPlanCachingHint}
	 * </ul>
	 * <p>
	 * For a few, because they only affect a single type of query, we
	 * invert that handling and throw the exception here expecting the
	 * singular subtype to override it to allow -<ul>
	 *     <li>{@linkplain #applySynchronizeSpacesHint}
	 *     <li>{@linkplain #applyCallableFunctionHint}
	 *     <li>{@linkplain #applyCallableFunctionTypeHint}
	 * </ul>
	 *
	 * @see #setHint(String, Object)
	 * @see #queryOptions
	 */
	protected final boolean applyHint(String hintName, Object value) {
		getSession().checkOpen( true );
		try {
			switch ( hintName ) {
				// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
				// always relevant
				case HINT_FLUSH_MODE:
					queryOptions.setFlushMode( FlushMode.fromHint( value ) );
					return true;
				case HINT_TIMEOUT:
					queryOptions.setTimeout( Timeouts.fromHibernateHint( value ) );
					return true;
				case HINT_JAVAEE_QUERY_TIMEOUT:
					DEPRECATION_LOGGER.deprecatedSetting( HINT_JAVAEE_QUERY_TIMEOUT, HINT_SPEC_QUERY_TIMEOUT );
					//fall through:
				case HINT_SPEC_QUERY_TIMEOUT:
					queryOptions.setTimeout( Timeouts.fromJpaHint( value ) );
					return true;
				case HINT_COMMENT:
					queryOptions.setComment( (String) value );
					return true;
				case HINT_QUERY_DATABASE:
					queryOptions.addDatabaseHint( (String) value );
					return true;
				// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
				// sometimes relevant
				case HINT_QUERY_PLAN_CACHEABLE:
					applyQueryPlanCachingHint( hintName, value );
				case HINT_READONLY:
					applyReadOnlyHint( hintName, value );
					return true;
				case HINT_FETCH_SIZE:
					applyFetchSizeHint( hintName, value );
					return true;
				case HINT_CACHEABLE:
					applyResultCachingHint( hintName, value );
					return true;
				case HINT_CACHE_MODE:
					applyResultCacheModeHint( hintName, value );
					return true;
				case HINT_JAVAEE_CACHE_RETRIEVE_MODE:
					DEPRECATION_LOGGER.deprecatedHint( HINT_JAVAEE_CACHE_RETRIEVE_MODE, HINT_SPEC_CACHE_RETRIEVE_MODE );
					//fall through to:
				case HINT_SPEC_CACHE_RETRIEVE_MODE:
					applyResultCachingRetrieveModeHint( hintName, value );
					return true;
				case HINT_JAVAEE_CACHE_STORE_MODE:
					DEPRECATION_LOGGER.deprecatedHint( HINT_JAVAEE_CACHE_STORE_MODE, HINT_SPEC_CACHE_STORE_MODE );
					//fall through to:
				case HINT_SPEC_CACHE_STORE_MODE:
					applyResultCachingStoreModeHint( hintName, value );
					return true;
				case HINT_CACHE_REGION:
					applyResultCachingRegionHint( hintName, value );
					return true;
				case HINT_JAVAEE_FETCH_GRAPH:
					DEPRECATION_LOGGER.deprecatedHint( HINT_JAVAEE_FETCH_GRAPH, HINT_SPEC_FETCH_GRAPH );
					//fall through to:
				case HINT_SPEC_FETCH_GRAPH:
					applyEntityGraphHint( GraphSemantic.FETCH, value, hintName );
					return true;
				case HINT_JAVAEE_LOAD_GRAPH:
					DEPRECATION_LOGGER.deprecatedHint( HINT_JAVAEE_LOAD_GRAPH, HINT_SPEC_LOAD_GRAPH );
					//fall through to:
				case HINT_SPEC_LOAD_GRAPH:
					applyEntityGraphHint( GraphSemantic.LOAD, value, hintName );
					return true;
				case HINT_FETCH_PROFILE:
					applyEnabledFetchProfileHint( hintName, value );
					return true;
				case HINT_NATIVE_SPACES:
					applySynchronizeSpacesHint( value );
					return true;
				case HINT_NATIVE_LOCKMODE:
					applyLockModeHint( HINT_NATIVE_LOCK_MODE, value );
					return true;
				case HINT_JAVAEE_LOCK_TIMEOUT:
					DEPRECATION_LOGGER.deprecatedHint( HINT_JAVAEE_LOCK_TIMEOUT, HINT_SPEC_LOCK_TIMEOUT );
					//fall through to:
				case HINT_SPEC_LOCK_TIMEOUT:
					applyLockTimeoutHint( hintName, value );
					return true;
				case HINT_FOLLOW_ON_STRATEGY:
					applyFollowOnStrategyHint( value );
					return true;
				case HINT_FOLLOW_ON_LOCKING:
					applyFollowOnLockingHint( getBoolean( value ) );
					return true;
				case HINT_CALLABLE_FUNCTION:
					applyCallableFunctionHint( hintName, value );
					return true;
				case HINT_CALLABLE_FUNCTION_RETURN_TYPE:
					applyCallableFunctionTypeHint( hintName, value );
				default:
					if ( hintName.startsWith( HINT_NATIVE_LOCK_MODE ) ) {
						// out-of-date support for specifying alias-specific lockmodes
						applyLockModeHint( HINT_NATIVE_LOCK_MODE, value );
						return true;
					}
					return false;
			}
		}
		catch ( ClassCastException e ) {
			throw new IllegalArgumentException( "Incorrect value for query hint: " + hintName, e );
		}
	}

	protected void applyQueryPlanCachingHint(String hintName, Object value) {
		queryOptions.setQueryPlanCachingEnabled( getBoolean( value ) );
	}

	protected void applyReadOnlyHint(String hintName, Object value) {
		queryOptions.setReadOnly( getBoolean( value ) );
	}

	protected void applyFetchSizeHint(String hintName, Object value) {
		queryOptions.setFetchSize( getInteger( value ) );
	}

	protected void applyResultCachingHint(String hintName, Object value) {
		queryOptions.setResultCachingEnabled( getBoolean( value ) );
	}

	protected void applyResultCacheModeHint(String hintName, Object value) {
		queryOptions.setCacheMode( getCacheMode( value ) );
	}

	protected void applyResultCachingRetrieveModeHint(String hintName, Object value) {
		final var retrieveMode = value == null ? null : CacheRetrieveMode.valueOf( value.toString().toUpperCase( ROOT ) );
		queryOptions.setCacheRetrieveMode( retrieveMode );
	}

	protected void applyResultCachingStoreModeHint(String hintName, Object value) {
		final var storeMode = value == null ? null : CacheStoreMode.valueOf( value.toString().toUpperCase( ROOT ) );
		queryOptions.setCacheStoreMode( storeMode );
	}

	protected void applyResultCachingRegionHint(String hintName, Object value) {
		queryOptions.setResultCacheRegionName( (String) value );
	}

	protected void applyEntityGraphHint(GraphSemantic graphSemantic, Object value, String hintName) {
		if ( value instanceof RootGraphImplementor<?> rootGraphImplementor ) {
			applyGraph( rootGraphImplementor, graphSemantic );
		}
		else if ( value instanceof String string ) {
			// try and interpret it as the name of a @NamedEntityGraph
			try {
				applyGraph( getSession().getEntityGraph( string ), graphSemantic );
				// getEntityGraph throws an exception if not found.  but since we got here, it was found
				return;
			}
			catch (IllegalArgumentException ignore) {
				// fall through...
			}

			// try and parse it in the entity graph language
			try {
				applyGraph( parseGraph( string ), graphSemantic );
			}
			catch ( IllegalArgumentException e ) {
				throw new IllegalArgumentException( "The string value of the hint '" + hintName
													+ "' must be the name of a named EntityGraph, or a representation understood by GraphParser" );
			}
		}
		else {
			throw new IllegalArgumentException( "The value of the hint '" + hintName
												+ "' must be an instance of EntityGraph, the string name of a named EntityGraph, or a string representation understood by GraphParser" );
		}
	}

	protected void applyEnabledFetchProfileHint(String hintName, Object value) {
		queryOptions.enableFetchProfile( (String) value );
	}

	protected RootGraphImplementor<?> parseGraph(String graphString) {
		final int separatorPosition = graphString.indexOf( '(' );
		final int terminatorPosition = graphString.lastIndexOf( ')' );
		if ( separatorPosition < 0 || terminatorPosition < 0 ) {
			throw new IllegalArgumentException(
					String.format(
							ROOT,
							"Invalid entity-graph definition '%s'; expected form '${EntityName}( ${property1} ... )'",
							graphString
					)
			);
		}

		final var factory = getSessionFactory();

		final String entityName =
				factory.getMappingMetamodel()
						.getImportedName( graphString.substring( 0, separatorPosition ).trim() );
		final String graphNodes = graphString.substring( separatorPosition + 1, terminatorPosition );

		final var rootGraph = new RootGraphImpl<>( null, factory.getJpaMetamodel().entity( entityName ) );
		GraphParser.parseInto( (EntityGraph<?>) rootGraph, graphNodes, getSessionFactory() );
		return rootGraph;
	}


	private void applyGraph(RootGraphImplementor<?> entityGraph, GraphSemantic graphSemantic) {
		queryOptions.applyGraph( entityGraph, graphSemantic );
	}

	private void applyLockModeHint(String hintName, Object value) {
		if ( value instanceof LockMode lockMode ) {
			applyLockModeHint( hintName, lockMode );
		}
		else if ( value instanceof LockModeType lockModeType ) {
			applyLockModeHint( hintName, LockMode.fromJpaLockMode( lockModeType ) );
		}
		else if ( value instanceof String string ) {
			applyLockModeHint( hintName, LockMode.fromExternalForm( string ) );
		}
		else {
			throw new IllegalArgumentException(
					String.format(
							"Native lock-mode hint [%s] must specify %s or %s. Encountered type: %s",
							HINT_NATIVE_LOCKMODE,
							LockMode.class.getName(),
							LockModeType.class.getName(),
							value.getClass().getName()
					)
			);
		}
	}

	protected void applyLockModeHint(String hintName, LockMode value) {
		queryOptions.getLockOptions().setLockMode( value );
	}

	protected void applyLockTimeoutHint(String hintName, Object timeout) {
		queryOptions.getLockOptions().setTimeout( Timeouts.fromJpaHint( timeout ) );
	}

	protected void applyFollowOnStrategyHint(Object value) {
		queryOptions.getLockOptions().setFollowOnStrategy( Locking.FollowOn.fromHint( value ) );
	}

	protected void applyFollowOnLockingHint(Boolean followOnLocking) {
		DEPRECATION_LOGGER.deprecatedHint( HINT_FOLLOW_ON_LOCKING, HINT_FOLLOW_ON_STRATEGY );
		applyFollowOnStrategyHint( Locking.FollowOn.fromLegacyValue( followOnLocking ) );
	}

	protected void applySynchronizeSpacesHint(Object value) {
		throw new IllegalArgumentException( "Query spaces hint was specified for non-native query" );
	}

	protected void applyCallableFunctionHint(String hintName, Object value) {
		throw new IllegalArgumentException( String.format( ROOT,
				"Query hint `%s` is only relevant for ProcedureCall queries",
				hintName
		) );
	}

	protected void applyCallableFunctionTypeHint(String hintName, Object value) {
		throw new IllegalArgumentException( String.format( ROOT,
				"Query hint `%s` is only relevant for ProcedureCall queries",
				hintName
		) );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Utilities

	public final SharedSessionContractImplementor getSession() {
		return session;
	}

	public final SessionFactoryImplementor getSessionFactory() {
		return session.getFactory();
	}

	public final MappingMetamodelImplementor getMappingMetamodel() {
		return session.getFactory().getMappingMetamodel();
	}

	public final TypeConfiguration getTypeConfiguration() {
		return session.getFactory().getTypeConfiguration();
	}

	protected QueryInterpretationCache getInterpretationCache() {
		return session.getFactory().getQueryEngine().getInterpretationCache();
	}

	protected final ExceptionConverter getExceptionConverter() {
		return session.getExceptionConverter();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter handling

	@Override
	public abstract ParameterMetadataImplementor getParameterMetadata();

	protected abstract QueryParameterBindings getQueryParameterBindings();

	protected abstract boolean resolveJdbcParameterTypeIfNecessary();

	@Override
	public Set<Parameter<?>> getParameters() {
		session.checkOpen( false );
		return unmodifiableSet( getParameterMetadata().getRegistrations() );
	}

	@Override
	public boolean isBound(Parameter<?> param) {
		session.checkOpen();
		final var parameter = getParameterMetadata().resolve( param );
		return parameter != null
			&& getQueryParameterBindings().isBound( getQueryParameter( parameter ) );
	}

	@Override
	public QueryParameterImplementor<?> getParameter(String name) {
		session.checkOpen( false );
		try {
			return getParameterMetadata().getQueryParameter( name );
		}
		catch ( HibernateException e ) {
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	public <T> QueryParameterImplementor<T> getParameter(String name, Class<T> type) {
		session.checkOpen( false );
		try {
			final var parameter = getParameterMetadata().getQueryParameter( name );
			final var parameterType = parameter.getParameterType();
			if ( !type.isAssignableFrom( parameterType ) ) {
				throw new IllegalArgumentException(
						"Type specified for parameter named '" + name + "' is incompatible"
						+ " (" + parameterType.getName() + " is not assignable to " + type.getName() + ")"
				);
			}
			@SuppressWarnings("unchecked") // safe, just checked
			var castParameter = (QueryParameterImplementor<T>) parameter;
			return castParameter;
		}
		catch ( HibernateException e ) {
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	public QueryParameterImplementor<?> getParameter(int position) {
		session.checkOpen( false );
		try {
			return getParameterMetadata().getQueryParameter( position );
		}
		catch ( HibernateException e ) {
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	public <T> QueryParameterImplementor<T> getParameter(int position, Class<T> type) {
		session.checkOpen( false );
		try {
			final var parameter = getParameterMetadata().getQueryParameter( position );
			final var parameterType = parameter.getParameterType();
			if ( !type.isAssignableFrom( parameterType ) ) {
				throw new IllegalArgumentException(
						"Type specified for parameter at position " + position + " is incompatible"
						+ " (" + parameterType.getName() + " is not assignable to " + type.getName() + ")"
				);
			}
			@SuppressWarnings("unchecked") // safe, just checked
			var castParameter = (QueryParameterImplementor<T>) parameter;
			return castParameter;
		}
		catch ( HibernateException e ) {
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	public <T> T getParameterValue(Parameter<T> param) {
		session.checkOpen( false );
		final var parameter = getParameterMetadata().resolve( param );
		if ( parameter == null ) {
			throw new IllegalArgumentException( "The parameter [" + param + "] is not part of this Query" );
		}
		final var binding =
				getQueryParameterBindings()
						.getBinding( getQueryParameter( parameter ) );
		if ( binding == null || !binding.isBound() ) {
			throw new IllegalStateException( "Parameter value not yet bound : " + param );
		}
		if ( binding.isMultiValued() ) {
			// TODO: THIS IS UNSOUND, we should really throw in this case
			//noinspection unchecked
			return (T) binding.getBindValues();
		}
		else {
			return binding.getBindValue();
		}
	}

	@Override
	public Object getParameterValue(String name) {
		session.checkOpen( false );
		final var binding = getQueryParameterBindings().getBinding( name );
		if ( !binding.isBound() ) {
			throw new IllegalStateException( "The parameter named '" + name + "' has no argument" );
		}
		return binding.isMultiValued() ? binding.getBindValues() : binding.getBindValue();
	}

	@Override
	public Object getParameterValue(int position) {
		session.checkOpen( false );
		final var binding = getQueryParameterBindings().getBinding( position );
		if ( !binding.isBound() ) {
			throw new IllegalStateException( "The parameter at position" + position + " has no argument" );
		}
		return binding.isMultiValued() ? binding.getBindValues() : binding.getBindValue();
	}

	private <P> JavaType<P> getJavaType(Class<P> javaType) {
		return getTypeConfiguration().getJavaTypeRegistry()
				.resolveDescriptor( javaType );
	}

	private <P> Type<P> getParamType(Class<P> javaType) {
		final var basicType = getTypeConfiguration().standardBasicTypeForJavaType( javaType );
		if ( basicType != null ) {
			return basicType;
		}
		else {
			final var managedDomainType =
					getSessionFactory().getJpaMetamodel()
							.managedType( javaType );
			if ( managedDomainType != null ) {
				return managedDomainType;
			}
			else {
				throw new HibernateException( "Unable to determine Type: " + javaType.getName() );
			}
		}
	}

	protected QueryParameterBinding<?> locateBinding(Parameter<?> parameter) {
		if ( parameter instanceof QueryParameterImplementor<?> parameterImplementor ) {
			session.checkOpen();
			return getQueryParameterBindings().getBinding( getQueryParameter( parameterImplementor ) );
		}
		else {
			return locateBinding( parameter.getName(), parameter.getPosition() );
		}
	}

	private @NonNull QueryParameterBinding<?> locateBinding(String name, Integer position) {
		final var bindings = getQueryParameterBindings();
		if ( name != null ) {
			final var binding = bindings.getBinding( name );
			if ( binding == null ) {
				// should never occur
				throw new IllegalArgumentException( "No binding for given parameter named '" + name + "'" );
			}
			return binding;
		}
		else if ( position != null ) {
			final var binding = bindings.getBinding( position );
			if ( binding == null ) {
				// should never occur
				throw new IllegalArgumentException( "No binding for given parameter at position " + position );
			}
			return binding;
		}
		else {
			throw new IllegalArgumentException( "Parameter must have either a name or a position" );
		}
	}

	protected QueryParameterBinding<?> locateBinding(String name) {
		session.checkOpen();
		return getQueryParameterBindings().getBinding( name );
	}

	protected QueryParameterBinding<?> locateBinding(int position) {
		session.checkOpen();
		return getQueryParameterBindings().getBinding( position );
	}

	protected <T> void setTypedParameter(int position, TypedParameterValue<T> typedValue) {
		setParameter( position, typedValue.value(), typedValue.type() );
	}

	protected <T> void setTypedParameter(String name, TypedParameterValue<T> typedValue) {
		setParameter( name, typedValue.value(), typedValue.type() );
	}

	private boolean multipleBinding(QueryParameter<?> parameter, Object value){
		if ( parameter.allowsMultiValuedBinding()
			&& value instanceof Collection<?> values
			// this check only needed for some native queries
			&& !isRegisteredAsBasicType( value.getClass() ) ) {
			final var hibernateType = parameter.getHibernateType();
			return hibernateType == null
				|| values.isEmpty()
				|| !isInstance( hibernateType, value, getNodeBuilder() )
				|| isInstance( hibernateType, values.iterator().next(), getNodeBuilder() );
		}
		else {
			return false;
		}
	}

	protected NodeBuilder getNodeBuilder() {
		return getSessionFactory().getQueryEngine().getCriteriaBuilder();
	}

	protected boolean isRegisteredAsBasicType(Class<?> valueClass) {
		return getTypeConfiguration().getBasicTypeForJavaType( valueClass ) != null;
	}

	protected <P> QueryParameterImplementor<P> getQueryParameter(QueryParameterImplementor<P> parameter) {
		return parameter;
	}


	@Override
	public CommonQueryContractImplementor setParameter(String name, Object value) {
		session.checkOpen( false );
		if ( value instanceof TypedParameterValue<?> typedParameterValue ) {
			setTypedParameter( name, typedParameterValue );
		}
		else {
			final var binding = getQueryParameterBindings().getBinding( name );
			if ( multipleBinding( binding.getQueryParameter(), value ) ) {
				setParameterList( name, (Collection<?>) value );
			}
			else {
				binding.setBindValue( value, resolveJdbcParameterTypeIfNecessary() );
			}
		}
		return this;
	}


	@Override
	public <P> CommonQueryContractImplementor setParameter(String name, P value, Class<P> javaType) {
		final var javaDescriptor = getJavaType( javaType );
		if ( javaDescriptor == null ) {
			setParameter( name, value );
		}
		else {
			setParameter( name, value, getParamType( javaType ) );
		}
		return this;
	}

	@Override
	public <P> CommonQueryContractImplementor setParameter(String name, P value, Type<P> type) {
		locateBinding( name ).setBindValue( value, (BindableType<P>) type );
		return this;
	}

	@Override @Deprecated(since = "7")
	public CommonQueryContractImplementor setParameter(String name, Instant value, TemporalType temporalType) {
		locateBinding( name ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public CommonQueryContractImplementor setParameter(int position, Object value) {
		session.checkOpen( false );

		if ( value instanceof TypedParameterValue<?> typedParameterValue ) {
			setTypedParameter( position, typedParameterValue );
		}
		else {
			final var binding = getQueryParameterBindings().getBinding( position );
			if ( multipleBinding( binding.getQueryParameter(), value ) ) {
				setParameterList( position, (Collection<?>) value );
			}
			else {
				binding.setBindValue( value, resolveJdbcParameterTypeIfNecessary() );
			}
		}
		return this;
	}

	@Override
	public <P> CommonQueryContractImplementor setParameter(int position, P value, Class<P> javaType) {
		final var javaDescriptor = getJavaType( javaType );
		if ( javaDescriptor == null ) {
			setParameter( position, value );
		}
		else {
			setParameter( position, value, getParamType( javaType ) );
		}
		return this;
	}

	@Override
	public <P> CommonQueryContractImplementor setParameter(int position, P value, Type<P> type) {
		locateBinding( position ).setBindValue( value, (BindableType<P>) type );
		return this;
	}

	@Override
	public <P> CommonQueryContractImplementor setConvertedParameter(String name, P value, Class<? extends AttributeConverter<P, ?>> converterClass) {
		//noinspection unchecked,rawtypes
		final JpaAttributeConverter<P,Object> converter = ConverterHelper.createJpaAttributeConverter(
				(Class) converterClass,
				session.getFactory().getServiceRegistry(),
				session.getFactory().getTypeConfiguration()
		);

		var bindValue = converter.getConverterBean().getBeanInstance().convertToDatabaseColumn( value );

		var bindValueJavaType = converter.getRelationalJavaType();
		var bindValueClass = bindValueJavaType.getJavaTypeClass();
		var bindValueModelType = getParamType( bindValueClass );

		//noinspection unchecked,rawtypes
		locateBinding( name ).setBindValue( bindValue, (BindableType) bindValueModelType );

		return this;
	}

	@Override
	public <P> CommonQueryContractImplementor setConvertedParameter(int position, P value, Class<? extends AttributeConverter<P, ?>> converterClass) {
		//noinspection unchecked,rawtypes
		final JpaAttributeConverter<P,Object> converter = ConverterHelper.createJpaAttributeConverter(
				(Class) converterClass,
				session.getFactory().getServiceRegistry(),
				session.getFactory().getTypeConfiguration()
		);

		var bindValue = converter.getConverterBean().getBeanInstance().convertToDatabaseColumn( value );

		var bindValueJavaType = converter.getRelationalJavaType();
		var bindValueClass = bindValueJavaType.getJavaTypeClass();
		var bindValueModelType = getParamType( bindValueClass );

		//noinspection unchecked,rawtypes
		locateBinding( position ).setBindValue( bindValue, (BindableType) bindValueModelType );

		return this;
	}

	@Override
	public CommonQueryContractImplementor setParameter(int position, Instant value, TemporalType temporalType) {
		locateBinding( position ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public <P> CommonQueryContractImplementor setParameter(QueryParameter<P> parameter, P value) {
		locateBinding( parameter ).setBindValue( value, resolveJdbcParameterTypeIfNecessary() );
		return this;
	}

	@Override
	public <P> CommonQueryContractImplementor setParameter(QueryParameter<P> parameter, P value, Class<P> javaType) {
		final var javaDescriptor = getJavaType( javaType );
		if ( javaDescriptor == null ) {
			setParameter( parameter, value );
		}
		else {
			setParameter( parameter, value, getParamType( javaType ) );
		}
		return this;
	}

	@Override
	public <P> CommonQueryContractImplementor setParameter(QueryParameter<P> parameter, P value, Type<P> type) {
		locateBinding( parameter ).setBindValue( value, (BindableType<P>) type );
		return this;
	}

	@Override
	public <P> CommonQueryContractImplementor setParameter(Parameter<P> parameter, P value) {
		if ( value instanceof TypedParameterValue<?> typedParameterValue ) {
			final var parameterType = parameter.getParameterType();
			final var type = typedParameterValue.type();
			if ( type == null ) {
				throw new IllegalArgumentException( "TypedParameterValue has no type" );
			}
			if ( !parameterType.isAssignableFrom( type.getJavaType() ) ) {
				throw new QueryArgumentException( "Given TypedParameterValue is not assignable to given Parameter type",
						parameterType, typedParameterValue.value() );
			}
			@SuppressWarnings("unchecked") // safe, because we just checked
			final var typedValue = (TypedParameterValue<P>) value;
			final var castValue = typedValue.value();
			final var castType = typedValue.type();
			if ( parameter instanceof QueryParameter<P> queryParameter ) {
				setParameter( queryParameter, castValue, castType );
			}
			else {
				locateBinding( parameter ).setBindValue( castValue, castType );
			}
		}
		else {
			locateBinding( parameter ).setBindValue( value, resolveJdbcParameterTypeIfNecessary() );
		}
		return this;
	}

	@Override
	public CommonQueryContractImplementor setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		locateBinding( param ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public CommonQueryContractImplementor setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		locateBinding( param ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public CommonQueryContractImplementor setParameter(String name, Calendar value, TemporalType temporalType) {
		locateBinding( name ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public CommonQueryContractImplementor setParameter(String name, Date value, TemporalType temporalType) {
		locateBinding( name ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public CommonQueryContractImplementor setParameter(int position, Calendar value, TemporalType temporalType) {
		locateBinding( position ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public CommonQueryContractImplementor setParameter(int position, Date value, TemporalType temporalType) {
		locateBinding( position ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public CommonQueryContractImplementor setParameterList(String name, @SuppressWarnings("rawtypes") Collection values) {
		getQueryParameterBindings().getBinding( name ).setBindValues( values );
		return this;
	}

	public <P> CommonQueryContractImplementor setParameterList(String name, Collection<? extends P> values, Class<P> javaType) {
		final var javaDescriptor = getJavaType( javaType );
		if ( javaDescriptor == null ) {
			setParameterList( name, values );
		}
		else {
			setParameterList( name, values, getParamType( javaType ) );
		}
		return this;
	}

	@Override
	public <P> CommonQueryContractImplementor setParameterList(String name, Collection<? extends P> values, Type<P> type) {
		locateBinding( name ).setBindValues( values, (BindableType<P>) type );
		return this;
	}

	@Override
	public CommonQueryContractImplementor setParameterList(String name, Object[] values) {
		final var binding = getQueryParameterBindings().getBinding( name );
		setParameterValues( values, binding );
		return this;
	}

	private <P> void setParameterValues(Object[] values, QueryParameterBinding<P> binding) {
		final var parameterType = binding.getBindType();
		if ( parameterType != null
			&& !areInstances( parameterType, values, getNodeBuilder() ) ) {
			throw new QueryArgumentException( "Argument to query parameter has an incompatible type",
					parameterType.getJavaType(), values.getClass().getComponentType(), values );
		}
		@SuppressWarnings("unchecked") // safe, just checked
		final var castArray = (P[]) values;
		binding.setBindValues( List.of( castArray ) );
	}

	@Override
	public <P> CommonQueryContractImplementor setParameterList(String name, P[] values, Class<P> javaType) {
		final var javaDescriptor = getJavaType( javaType );
		if ( javaDescriptor == null ) {
			setParameterList( name, values );
		}
		else {
			setParameterList( name, values, getParamType( javaType ) );
		}
		return this;
	}

	public <P> CommonQueryContractImplementor setParameterList(String name, P[] values, Type<P> type) {
		locateBinding( name ).setBindValues( List.of( values ), (BindableType<P>) type );
		return this;
	}

	@Override
	public CommonQueryContractImplementor setParameterList(int position, @SuppressWarnings("rawtypes") Collection values) {
		//TODO: type checking?
		getQueryParameterBindings().getBinding( position ).setBindValues( values );
		return this;
	}

	@Override
	public <P> CommonQueryContractImplementor setParameterList(int position, Collection<? extends P> values, Class<P> javaType) {
		final var javaDescriptor = getJavaType( javaType );
		if ( javaDescriptor == null ) {
			setParameterList( position, values );
		}
		else {
			setParameterList( position, values, getParamType( javaType ) );
		}
		return this;
	}

	@Override
	public <P> CommonQueryContractImplementor setParameterList(int position, Collection<? extends P> values, Type<P> type) {
		locateBinding( position ).setBindValues( values, (BindableType<P>) type );
		return this;
	}

	@Override
	public CommonQueryContractImplementor setParameterList(int position, Object[] values) {
		final var binding = getQueryParameterBindings().getBinding( position );
		setParameterValues( values, binding );
		return this;
	}

	@Override
	public <P> CommonQueryContractImplementor setParameterList(int position, P[] values, Class<P> javaType) {
		final var javaDescriptor = getJavaType( javaType );
		if ( javaDescriptor == null ) {
			setParameterList( position, values );
		}
		else {
			setParameterList( position, values, getParamType( javaType ) );
		}
		return this;
	}

	public <P> CommonQueryContractImplementor setParameterList(int position, P[] values, Type<P> type) {
		locateBinding( position ).setBindValues( List.of( values ), (BindableType<P>) type );
		return this;
	}

	@Override
	public <P> CommonQueryContractImplementor setParameterList(QueryParameter<P> parameter, Collection<? extends P> values) {
		locateBinding( parameter ).setBindValues( values );
		return this;
	}

	@Override
	public <P> CommonQueryContractImplementor setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType) {
		final var javaDescriptor = getJavaType( javaType );
		if ( javaDescriptor == null ) {
			setParameterList( parameter, values );
		}
		else {
			setParameterList( parameter, values, getParamType( javaType ) );
		}
		return this;
	}

	@Override
	public <P> CommonQueryContractImplementor setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Type<P> type) {
		locateBinding( parameter ).setBindValues( values, (BindableType<P>) type );
		return this;
	}

	@Override
	public <P> CommonQueryContractImplementor setParameterList(QueryParameter<P> parameter, P[] values) {
		locateBinding( parameter ).setBindValues( List.of( values ) );
		return this;
	}

	@Override
	public <P> CommonQueryContractImplementor setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType) {
		final var javaDescriptor = getJavaType( javaType );
		if ( javaDescriptor == null ) {
			setParameterList( parameter, values );
		}
		else {
			setParameterList( parameter, values, getParamType( javaType ) );
		}
		return this;
	}

	@Override
	public <P> CommonQueryContractImplementor setParameterList(QueryParameter<P> parameter, P[] values, Type<P> type) {
		locateBinding( parameter ).setBindValues( List.of( values ), (BindableType<P>) type );
		return this;
	}

	@Override
	public CommonQueryContractImplementor setProperties(@SuppressWarnings("rawtypes") Map map) {
		for ( String paramName : getParameterMetadata().getNamedParameterNames() ) {
			final Object object = map.get( paramName );
			if ( object == null ) {
				if ( map.containsKey( paramName ) ) {
					setParameter( paramName, null,
							determineType( paramName, null ) );
				}
			}
			else {
				if ( object instanceof Collection<?> collection ) {
					setParameterList( paramName, collection );
				}
				else if ( object instanceof Object[] array ) {
					setParameterList( paramName, array );
				}
				else {
					setParameter( paramName, object,
							determineType( paramName, object.getClass() ) );
				}
			}
		}
		return this;
	}

	@Override
	public CommonQueryContractImplementor setProperties(Object bean) {
		final var beanClass = bean.getClass();
		for ( String paramName : getParameterMetadata().getNamedParameterNames() ) {
			try {
				final var getter =
						BuiltInPropertyAccessStrategies.BASIC.getStrategy()
								.buildPropertyAccess( beanClass, paramName, true )
								.getGetter();
				final var returnType = getter.getReturnTypeClass();
				final Object object = getter.get( bean );
				if ( Collection.class.isAssignableFrom( returnType ) ) {
					setParameterList( paramName, (Collection<?>) object );
				}
				else if ( returnType.isArray() ) {
					setParameterList( paramName, (Object[]) object );
				}
				else {
					setParameter( paramName, object, determineType( paramName, returnType ) );
				}
			}
			catch (PropertyNotFoundException pnfe) {
				// ignore
			}
		}
		return this;
	}

	protected <T> Type<T> determineType(String namedParam, Class<? extends T> retType) {
		var type = getQueryParameterBindings().getBinding( namedParam ).getBindType();
		if ( type == null ) {
			type = getParameterMetadata().getQueryParameter( namedParam ).getHibernateType();
		}
		if ( type == null && retType != null ) {
			type = getSessionFactory().getMappingMetamodel().resolveParameterBindType( retType );
		}
		if ( retType != null && !retType.isAssignableFrom( type.getJavaType() ) ) {
			// TODO: is this really the correct exception type?
			throw new IllegalStateException( "Parameter not of expected type: " + retType.getName() );
		}
		//noinspection unchecked
		return (Type<T>) type;
	}




	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Execution

	private FlushMode sessionFlushMode;
	private CacheMode sessionCacheMode;

	protected abstract QueryOptions getQueryOptions();

	/// hook for subtypes
	protected abstract void prepareForExecution();

	protected abstract <X> List<X> doList();

	protected abstract int doExecuteUpdate();

	protected HashSet<String> beforeQueryHandlingFetchProfiles() {
		beforeQuery();
		final var options = getQueryOptions();
		return getSession().getLoadQueryInfluencers()
				.adjustFetchProfiles( options.getDisabledFetchProfiles(), options.getEnabledFetchProfiles() );
	}

	protected void beforeQuery() {
		getQueryParameterBindings().validate();
		final var session = getSession();
		final var options = getQueryOptions();
		session.prepareForQueryExecution( requiresTxn( options.getLockOptions().getLockMode() ) );
		prepareForExecution();
		prepareSessionFlushMode( session );
		prepareSessionCacheMode( session );
	}


	/*
	 * Used by Hibernate Reactive
	 */
	protected void prepareSessionFlushMode(SharedSessionContractImplementor session) {
		assert sessionFlushMode == null;
		sessionFlushMode = adjustFlushMode( getEffectiveFlushMode() );
	}

	/*
	 * Used by Hibernate Reactive
	 */
	protected void prepareSessionCacheMode(SharedSessionContractImplementor session) {
		assert sessionCacheMode == null;
		sessionCacheMode = adjustCacheMode( getQueryOptions().getCacheMode() );
	}

	protected void afterQueryHandlingFetchProfiles(boolean success, HashSet<String> fetchProfiles) {
		resetFetchProfiles( fetchProfiles );
		afterQuery( success );
	}

	protected void afterQueryHandlingFetchProfiles(HashSet<String> fetchProfiles) {
		resetFetchProfiles( fetchProfiles );
		afterQuery();
	}

	private void resetFetchProfiles(HashSet<String> fetchProfiles) {
		getSession().getLoadQueryInfluencers().setEnabledFetchProfileNames( fetchProfiles );
	}

	protected void afterQuery(boolean success) {
		afterQuery();

		final var session = getSession();
		if ( !session.isTransactionInProgress() ) {
			session.getJdbcCoordinator().getLogicalConnection().afterTransaction();
		}
		session.afterOperation( success );
	}

	protected void afterQuery() {
		if ( sessionFlushMode != null
			&& getSession() instanceof SessionImplementor statefulSession ) {
			statefulSession.setHibernateFlushMode( sessionFlushMode );
			sessionFlushMode = null;
		}
		if ( sessionCacheMode != null ) {
			getSession().setCacheMode( sessionCacheMode );
			sessionCacheMode = null;
		}
	}


	protected CacheMode adjustCacheMode(CacheMode queryCacheMode) {
		if ( queryCacheMode != null && this.session instanceof SessionImplementor session ) {
			var sessionCacheMode = session.getCacheMode();
			if ( queryCacheMode != sessionCacheMode ) {
				session.setCacheMode( queryCacheMode );
				return sessionCacheMode;
			}
		}

		return null;
	}

	private FlushMode adjustFlushMode(FlushMode effectiveFlushMode) {
		assert effectiveFlushMode != null;

		// NOTE: returning null says that the FlushMode on Session was not set,
		// and so we should not reset it after.

		if ( this.session instanceof SessionImplementor session ) {
			final var sessionFlushMode = session.getHibernateFlushMode();
			if ( effectiveFlushMode != sessionFlushMode ) {
				session.setHibernateFlushMode( effectiveFlushMode );
				return sessionFlushMode;
			}
		}

		// nothing to readjust by default
		return null;
	}


	@Override
	public List<?> getResultList() {
		final var fetchProfiles = beforeQueryHandlingFetchProfiles();
		boolean success = false;
		try {
			final List<?> result = doList();
			success = true;
			return result;
		}
		catch (IllegalQueryOperationException e) {
			throw new IllegalStateException( e );
		}
		catch (HibernateException he) {
			throw getExceptionConverter().convert( he, getQueryOptions().getLockOptions() );
		}
		finally {
			afterQueryHandlingFetchProfiles( success, fetchProfiles );
		}
	}


	protected boolean requiresTxn(LockMode lockMode) {
		return lockMode != null && lockMode.greaterThan( LockMode.READ );
	}

	@Override
	public int executeUpdate() throws HibernateException {
		//TODO: refactor copy/paste of QuerySqmImpl.executeUpdate()
		getSession().checkTransactionNeededForUpdateOperation( "No active transaction for update or delete query" );
		final var fetchProfiles = beforeQueryHandlingFetchProfiles();
		boolean success = false;
		try {
			final int result = doExecuteUpdate();
			success = true;
			return result;
		}
		catch (IllegalQueryOperationException e) {
			throw new IllegalStateException( e );
		}
		catch (HibernateException e) {
			throw getExceptionConverter().convert( e );
		}
		finally {
			afterQueryHandlingFetchProfiles( success, fetchProfiles );
		}
	}


}
