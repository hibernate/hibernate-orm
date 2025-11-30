/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;
import jakarta.persistence.metamodel.Type;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.Timeouts;
import org.hibernate.engine.spi.ExceptionConverter;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphParser;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.internal.RootGraphImpl;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.jpa.internal.util.ConfigurationHelper;
import org.hibernate.jpa.internal.util.FlushModeTypeHelper;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies;
import org.hibernate.query.CommonQueryContract;
import org.hibernate.query.QueryArgumentException;
import org.hibernate.query.QueryFlushMode;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.TypedParameterValue;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.type.BindableType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.Math.round;
import static java.util.Collections.unmodifiableSet;
import static java.util.Locale.ROOT;
import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;
import static org.hibernate.jpa.HibernateHints.HINT_CACHEABLE;
import static org.hibernate.jpa.HibernateHints.HINT_CACHE_MODE;
import static org.hibernate.jpa.HibernateHints.HINT_CACHE_REGION;
import static org.hibernate.jpa.HibernateHints.HINT_COMMENT;
import static org.hibernate.jpa.HibernateHints.HINT_FETCH_PROFILE;
import static org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE;
import static org.hibernate.jpa.HibernateHints.HINT_FLUSH_MODE;
import static org.hibernate.jpa.HibernateHints.HINT_FOLLOW_ON_LOCKING;
import static org.hibernate.jpa.HibernateHints.HINT_FOLLOW_ON_STRATEGY;
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

/**
 * Base implementation of {@link CommonQueryContract}.
 *
 * @apiNote This class is now considered internal implementation
 * and will move to an internal package in a future version.
 * Application programs should never depend directly on this class.
 *
 * @author Steve Ebersole
 */
@Internal
public abstract class AbstractCommonQueryContract implements CommonQueryContract {
	private final SharedSessionContractImplementor session;
	private final QueryOptionsImpl queryOptions;

	public AbstractCommonQueryContract(SharedSessionContractImplementor session) {
		this.session = session;
		this.queryOptions = new QueryOptionsImpl();
	}

	protected AbstractCommonQueryContract(AbstractCommonQueryContract original) {
		this.session = original.session;
		this.queryOptions = original.queryOptions;
	}

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

	protected int getIntegerLiteral(JpaExpression<Number> expression, int defaultValue) {
		if ( expression == null ) {
			return defaultValue;
		}
		else if ( expression instanceof SqmLiteral<?> ) {
			return ( (SqmLiteral<Number>) expression ).getLiteralValue().intValue();
		}
		else if ( expression instanceof Parameter<?> ) {
			final Number parameterValue = getParameterValue( (Parameter<Number>) expression );
			return parameterValue == null ? defaultValue : parameterValue.intValue();
		}
		throw new IllegalArgumentException( "Can't get integer literal value from: " + expression );
	}

	protected int getMaxRows(SqmSelectStatement<?> selectStatement, int size) {
		final var fetchExpression = selectStatement.getFetch();
		if ( fetchExpression != null ) {
			final var fetchValue = fetchValue( fetchExpression );
			if ( fetchValue != null ) {
				// Note that we can never have ties because this is only used when we deduplicate results
				return switch ( selectStatement.getFetchClauseType() ) {
					case ROWS_ONLY, ROWS_WITH_TIES -> fetchValue.intValue();
					case PERCENT_ONLY, PERCENT_WITH_TIES ->
							(int) Math.ceil( (((double) size) * fetchValue.doubleValue()) / 100d );
				};
			}
		}
		return -1;
	}

	private Number fetchValue(JpaExpression<Number> expression) {
		if ( expression instanceof SqmLiteral<?> ) {
			return ((SqmLiteral<Number>) expression).getLiteralValue();
		}
		else if ( expression instanceof SqmParameter<?> ) {
			return getParameterValue( (Parameter<Number>) expression );
		}
		else {
			throw new IllegalArgumentException( "Can't get max rows value from: " + expression );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Hints

	public Map<String, Object> getHints() {
		// According to the JPA spec, this should force a rollback, but that's insane :)
		// If the TCK ever adds a check for this, we may need to change this behavior
		checkOpenNoRollback();
		final Map<String,Object> hints = new HashMap<>();
		collectHints( hints );
		return hints;
	}

	@SuppressWarnings("deprecation")
	protected void collectHints(Map<String, Object> hints) {
		final var queryOptions = getQueryOptions();

		if ( queryOptions.getTimeout() != null ) {
			hints.put( HINT_TIMEOUT, queryOptions.getTimeout() );
			hints.put( HINT_SPEC_QUERY_TIMEOUT, queryOptions.getTimeout() * 1000 );
		}

		putIfNotNull( hints, HINT_COMMENT, getComment() );
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

		final var lockOptions = getLockOptions();
		if ( !lockOptions.isEmpty() ) {
			final var lockMode = lockOptions.getLockMode();
			if ( lockMode != null && lockMode != LockMode.NONE ) {
				hints.put( HINT_NATIVE_LOCKMODE, lockMode );
			}

			if ( lockOptions.getFollowOnStrategy() != null ) {
				hints.put( HINT_FOLLOW_ON_STRATEGY, lockOptions.getFollowOnStrategy() );
			}

			if ( lockOptions.getTimeout().milliseconds() != Timeouts.WAIT_FOREVER_MILLI ) {
				hints.put( HINT_SPEC_LOCK_TIMEOUT, lockOptions.getTimeOut() );
				hints.put( HINT_JAVAEE_LOCK_TIMEOUT, lockOptions.getTimeOut() );
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
	public CommonQueryContract setHint(String hintName, Object value) {
		if ( !applyHint( hintName, value ) ) {
			QUERY_MESSAGE_LOGGER.ignoringUnrecognizedQueryHint( hintName );
		}
		return this;
	}

	public final boolean applyHint(String hintName, Object value) {
		getSession().checkOpen( true );
		try {
			switch ( hintName ) {
				case HINT_FLUSH_MODE:
					applyFlushModeHint( ConfigurationHelper.getFlushMode( value ) );
					return true;
				case HINT_TIMEOUT:
					applyTimeoutHint( getInteger( value ) );
					return true;
				case HINT_JAVAEE_QUERY_TIMEOUT:
					DEPRECATION_LOGGER.deprecatedSetting( HINT_JAVAEE_QUERY_TIMEOUT, HINT_SPEC_QUERY_TIMEOUT );
					//fall through:
				case HINT_SPEC_QUERY_TIMEOUT:
					// convert milliseconds to seconds
					final int timeout = (int) round( getInteger( value ).doubleValue() / 1000.0 );
					applyTimeoutHint( timeout );
					return true;
				case HINT_COMMENT:
					applyCommentHint( (String) value );
					return true;
				case HINT_NATIVE_SPACES:
					applySynchronizeSpacesHint( value );
					return true;
				case HINT_QUERY_DATABASE:
					applyDatabaseHint( (String) value );
					return true;
				default:
					return applySelectionHint( hintName, value );
			}
		}
		catch ( ClassCastException e ) {
			throw new IllegalArgumentException( "Incorrect value for query hint: " + hintName, e );
		}
	}

	protected void applySynchronizeSpacesHint(Object value) {
		throw new IllegalArgumentException( "Query spaces hint was specified for non-native query" );
	}

	protected final boolean applySelectionHint(String hintName, Object value) {
		if ( applyLockingHint( hintName, value ) ) {
			return true;
		}
		else {
			final var queryOptions = getQueryOptions();
			switch ( hintName ) {
				case HINT_READONLY:
					queryOptions.setReadOnly( getBoolean( value ) );
					return true;
				case HINT_FETCH_SIZE:
					queryOptions.setFetchSize( getInteger( value ) );
					return true;
				case HINT_QUERY_PLAN_CACHEABLE:
					queryOptions.setQueryPlanCachingEnabled( getBoolean( value ) );
					return true;
				case HINT_CACHEABLE:
					queryOptions.setResultCachingEnabled( getBoolean( value ) );
					return true;
				case HINT_CACHE_REGION:
					queryOptions.setResultCacheRegionName( (String) value );
					return true;
				case HINT_CACHE_MODE:
					queryOptions.setCacheMode( getCacheMode( value ) );
					return true;
				case HINT_JAVAEE_CACHE_RETRIEVE_MODE:
					DEPRECATION_LOGGER.deprecatedHint( HINT_JAVAEE_CACHE_RETRIEVE_MODE, HINT_SPEC_CACHE_RETRIEVE_MODE );
					//fall through to:
				case HINT_SPEC_CACHE_RETRIEVE_MODE:
					final var retrieveMode = value == null ? null : CacheRetrieveMode.valueOf( value.toString() );
					queryOptions.setCacheRetrieveMode( retrieveMode );
					return true;
				case HINT_JAVAEE_CACHE_STORE_MODE:
					DEPRECATION_LOGGER.deprecatedHint( HINT_JAVAEE_CACHE_STORE_MODE, HINT_SPEC_CACHE_STORE_MODE );
					//fall through to:
				case HINT_SPEC_CACHE_STORE_MODE:
					final var storeMode = value == null ? null : CacheStoreMode.valueOf( value.toString() );
					queryOptions.setCacheStoreMode( storeMode );
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
					queryOptions.enableFetchProfile( (String) value );
				default:
					// unrecognized hint
					return false;
			}
		}
	}

	protected void applyEntityGraphHint(GraphSemantic graphSemantic, Object value, String hintName) {
		if ( value instanceof RootGraphImplementor<?> rootGraphImplementor ) {
			applyGraph( rootGraphImplementor, graphSemantic );
		}
		else if ( value instanceof String string ) {
			// try and interpret it as the name of a @NamedEntityGraph
			final var entityGraph = getEntityGraph( string );
			if ( entityGraph == null ) {
				try {
					// try and parse it in the entity graph language
					applyGraph( string, graphSemantic );
				}
				catch ( IllegalArgumentException e ) {
					throw new IllegalArgumentException( "The string value of the hint '" + hintName
							+ "' must be the name of a named EntityGraph, or a representation understood by GraphParser" );
				}
			}
			else {
				applyGraph( entityGraph, graphSemantic );
			}
		}
		else {
			throw new IllegalArgumentException( "The value of the hint '" + hintName
					+ "' must be an instance of EntityGraph, the string name of a named EntityGraph, or a string representation understood by GraphParser" );
		}
	}

	private RootGraphImplementor<?> getEntityGraph(String string) {
		try {
			return getSession().getEntityGraph( string );
		}
		catch ( IllegalArgumentException e ) {
			return null;
		}
	}

	protected void applyGraph(String graphString, GraphSemantic graphSemantic) {
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
		applyGraph( rootGraph, graphSemantic );
	}

	protected void applyGraph(RootGraphImplementor<?> entityGraph, GraphSemantic graphSemantic) {
		getQueryOptions().applyGraph( entityGraph, graphSemantic );
	}

	private boolean applyLockingHint(String hintName, Object value) {
		switch ( hintName ) {
			case HINT_JAVAEE_LOCK_TIMEOUT:
				DEPRECATION_LOGGER.deprecatedHint( HINT_JAVAEE_LOCK_TIMEOUT, HINT_SPEC_LOCK_TIMEOUT );
				//fall through to:
			case HINT_SPEC_LOCK_TIMEOUT:
				if ( value != null ) {
					applyLockTimeoutHint( getInteger( value ) );
					return true;
				}
				else {
					return false;
				}
			case HINT_FOLLOW_ON_STRATEGY:
				if ( value == null ) {
					applyFollowOnStrategyHint( Locking.FollowOn.ALLOW );
				}
				if ( value instanceof Locking.FollowOn strategyValue ) {
					applyFollowOnStrategyHint( strategyValue );
				}
				else {
					applyFollowOnStrategyHint( Locking.FollowOn.valueOf( value.toString() ) );
				}
				return true;
			case HINT_FOLLOW_ON_LOCKING:
				applyFollowOnLockingHint( getBoolean( value ) );
				return true;
			case HINT_NATIVE_LOCKMODE:
				applyLockModeHint( value );
				return true;
			default:
				if ( hintName.startsWith( HINT_NATIVE_LOCKMODE ) ) {
					applyLockModeHint( value );
					return true;
				}
				return false;
		}
	}

	protected void applyLockTimeoutHint(Integer timeout) {
		if ( timeout != null ) {
			applyLockTimeoutHint( (int) timeout );
		}
	}

	private LockOptions getLockOptions() {
		return getQueryOptions().getLockOptions();
	}

	protected void applyLockTimeoutHint(int timeout) {
		getLockOptions().setTimeOut( timeout );
	}

	protected void applyHibernateLockMode(LockMode value) {
		getLockOptions().setLockMode( value );
	}

	protected void applyLockModeType(LockModeType value) {
		applyHibernateLockMode( LockMode.fromJpaLockMode( value ) );
	}

	protected final void applyLockModeHint(Object value) {
		if ( value instanceof LockMode lockMode ) {
			applyHibernateLockMode( lockMode );
		}
		else if ( value instanceof LockModeType lockModeType ) {
			applyLockModeType( lockModeType );
		}
		else if ( value instanceof String string ) {
			applyHibernateLockMode( LockMode.fromExternalForm( string ) );
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

	protected void applyFollowOnStrategyHint(Locking.FollowOn followOnStrategy) {
		getLockOptions().setFollowOnStrategy( followOnStrategy );
	}

	protected void applyFollowOnLockingHint(Boolean followOnLocking) {
		DEPRECATION_LOGGER.deprecatedHint( HINT_FOLLOW_ON_LOCKING, HINT_FOLLOW_ON_STRATEGY );
		applyFollowOnStrategyHint( Locking.FollowOn.fromLegacyValue( followOnLocking ) );
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
		final FlushMode flushMode = getQueryOptions().getFlushMode();
		return flushMode == null ? getSession().getHibernateFlushMode() : flushMode;
	}

	@Override
	public CommonQueryContract setHibernateFlushMode(FlushMode flushMode) {
		getQueryOptions().setFlushMode( flushMode );
		return this;
	}

	@Override
	public QueryFlushMode getQueryFlushMode() {
		return FlushModeTypeHelper.getForcedFlushMode( getQueryOptions().getFlushMode() );
	}

	@Override
	public CommonQueryContract setQueryFlushMode(QueryFlushMode queryFlushMode) {
		getQueryOptions().setFlushMode( FlushModeTypeHelper.getFlushMode(queryFlushMode) );
		return this;
	}

	protected void applyTimeoutHint(int timeout) {
		setTimeout( timeout );
	}

	protected void applyCommentHint(String comment) {
		setComment( comment );
	}

	protected void applyFlushModeHint(FlushMode flushMode) {
		setHibernateFlushMode( flushMode );
	}

	protected void applyDatabaseHint(String hint) {
		getQueryOptions().addDatabaseHint( hint );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Options

	public MutableQueryOptions getQueryOptions() {
		return queryOptions;
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

	private void getCheckOpen() {
		getSession().checkOpen();
	}

	private void checkOpenNoRollback() {
		getSession().checkOpen( false );
	}

	public int getMaxResults() {
		getCheckOpen();
		return getQueryOptions().getLimit().getMaxRowsJpa();
	}

	public int getFirstResult() {
		getCheckOpen();
		return getQueryOptions().getLimit().getFirstRowJpa();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter handling

	@Override
	public abstract ParameterMetadataImplementor getParameterMetadata();

	public Set<Parameter<?>> getParameters() {
		checkOpenNoRollback();
		return unmodifiableSet( getParameterMetadata().getRegistrations() );
	}

	public QueryParameterImplementor<?> getParameter(String name) {
		checkOpenNoRollback();
		try {
			return getParameterMetadata().getQueryParameter( name );
		}
		catch ( HibernateException e ) {
			throw getExceptionConverter().convert( e );
		}
	}

	public <T> QueryParameterImplementor<T> getParameter(String name, Class<T> type) {
		checkOpenNoRollback();
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

	public QueryParameterImplementor<?> getParameter(int position) {
		checkOpenNoRollback();
		try {
			return getParameterMetadata().getQueryParameter( position );
		}
		catch ( HibernateException e ) {
			throw getExceptionConverter().convert( e );
		}
	}

	public <T> QueryParameterImplementor<T> getParameter(int position, Class<T> type) {
		checkOpenNoRollback();
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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter binding handling

	@Internal // Made public to work around this bug: https://bugs.openjdk.org/browse/JDK-8340443
	public abstract QueryParameterBindings getQueryParameterBindings();
	protected abstract boolean resolveJdbcParameterTypeIfNecessary();

	private <P> JavaType<P> getJavaType(Class<P> javaType) {
		return getTypeConfiguration().getJavaTypeRegistry()
				.resolveDescriptor( javaType );
	}

	protected <P> QueryParameterBinding<P> locateBinding(Parameter<P> parameter, P value) {
		if ( parameter instanceof QueryParameterImplementor<P> parameterImplementor ) {
			return locateBinding( parameterImplementor );
		}
		else {
			final String name = parameter.getName();
			final Integer position = parameter.getPosition();
			final var parameterType = parameter.getParameterType();
			if ( name != null ) {
				return locateBinding( name, parameterType, value );
			}
			else if ( position != null ) {
				return locateBinding( position, parameterType, value );
			}
		}

		throw getExceptionConverter().convert(
				new IllegalArgumentException( "Could not resolve binding for given parameter reference [" + parameter + "]" )
		);
	}

	protected <P> QueryParameterBinding<P> locateBinding(Parameter<P> parameter, Collection<? extends P> values) {
		if ( parameter instanceof QueryParameterImplementor<P> parameterImplementor ) {
			return locateBinding( parameterImplementor );
		}
		else {
			final String name = parameter.getName();
			final Integer position = parameter.getPosition();
			final var parameterType = parameter.getParameterType();
			if ( name != null ) {
				return locateBinding( name, parameterType, values );
			}
			else if ( position != null ) {
				return locateBinding( position, parameterType, values );
			}
		}

		throw getExceptionConverter().convert(
				new IllegalArgumentException( "Could not resolve binding for given parameter reference [" + parameter + "]" )
		);
	}

	protected <P> QueryParameterBinding<P> locateBinding(QueryParameterImplementor<P> parameter) {
		getCheckOpen();
		return getQueryParameterBindings()
				.getBinding( getQueryParameter( parameter ) );
	}

	protected <P> QueryParameterBinding<P> locateBinding(String name, Class<P> javaType, P value) {
		getCheckOpen();
		final var binding = getQueryParameterBindings().getBinding( name );
		final var parameterType = binding.getBindType();
		if ( parameterType != null ) {
			final var parameterJavaType = parameterType.getJavaType();
			if ( !parameterJavaType.isAssignableFrom( javaType )
					&& !isInstance( parameterType, value ) ) {
				throw new QueryArgumentException(
						"Argument to parameter named '" + name + "' has an incompatible type",
						parameterJavaType, javaType, value );
			}
		}
		@SuppressWarnings("unchecked") // safe, just checked
		var castBinding = (QueryParameterBinding<P>) binding;
		return castBinding;
	}

	protected <P> QueryParameterBinding<P> locateBinding(int position, Class<P> javaType, P value) {
		getCheckOpen();
		final var binding = getQueryParameterBindings().getBinding( position );
		final var parameterType = binding.getBindType();
		if ( parameterType != null ) {
			final var parameterJavaType = parameterType.getJavaType();
			if ( !parameterJavaType.isAssignableFrom( javaType )
					&& !isInstance( parameterType, value ) ) {
				throw new QueryArgumentException(
						"Argument to parameter at position " + position + " has an incompatible type",
						parameterJavaType, javaType, value );
			}
		}
		@SuppressWarnings("unchecked") // safe, just checked
		var castBinding = (QueryParameterBinding<P>) binding;
		return castBinding;
	}

	protected <P> QueryParameterBinding<P> locateBinding(String name, Class<P> javaType, Collection<? extends P> values) {
		getCheckOpen();
		final var binding = getQueryParameterBindings().getBinding( name );
		final var parameterType = binding.getBindType();
		if ( parameterType != null ) {
			final var parameterJavaType = parameterType.getJavaType();
			if ( !parameterJavaType.isAssignableFrom( javaType )
					&& !areInstances( parameterType, values ) ) {
				throw new QueryArgumentException(
						"Argument to parameter named '" + name + "' has an incompatible type",
						parameterJavaType, javaType, values );
			}
		}
		@SuppressWarnings("unchecked") // safe, just checked
		var castBinding = (QueryParameterBinding<P>) binding;
		return castBinding;
	}

	protected <P> QueryParameterBinding<P> locateBinding(int position, Class<P> javaType, Collection<? extends P> values) {
		getCheckOpen();
		final var binding = getQueryParameterBindings().getBinding( position );
		final var parameterType = binding.getBindType();
		if ( parameterType != null ) {
			final var parameterJavaType = parameterType.getJavaType();
			if ( !parameterJavaType.isAssignableFrom( javaType )
					&& !areInstances( parameterType, values ) ) {
				throw new QueryArgumentException(
						"Argument to parameter at position " + position + " has an incompatible type",
						parameterJavaType, javaType, values );
			}
		}
		@SuppressWarnings("unchecked") // safe, just checked
		var castBinding = (QueryParameterBinding<P>) binding;
		return castBinding;
	}

	protected <P> QueryParameterImplementor<P> getQueryParameter(QueryParameterImplementor<P> parameter) {
		return parameter;
	}

	public boolean isBound(Parameter<?> param) {
		getCheckOpen();
		final var parameter = getParameterMetadata().resolve( param );
		return parameter != null
			&& getQueryParameterBindings().isBound( getQueryParameter( parameter ) );
	}

	public <T> T getParameterValue(Parameter<T> param) {
		checkOpenNoRollback();
		final var parameter = getParameterMetadata().resolve( param );
		if ( parameter == null ) {
			throw new IllegalArgumentException( "The parameter [" + param + "] is not part of this Query" );
		}
		final var binding =
				getQueryParameterBindings()
						.getBinding( getQueryParameter( parameter ) );
		if ( binding == null || !binding.isBound() ) {
			throw new IllegalStateException( "Parameter value not yet bound : " + param.toString() );
		}
		if ( binding.isMultiValued() ) {
			// TODO: THIS IS UNSOUND
			//noinspection unchecked
			return (T) binding.getBindValues();
		}
		else {
			return binding.getBindValue();
		}
	}

	public Object getParameterValue(String name) {
		checkOpenNoRollback();
		final var binding = getQueryParameterBindings().getBinding( name );
		if ( !binding.isBound() ) {
			throw new IllegalStateException( "The parameter [" + name + "] has not yet been bound" );
		}
		return binding.isMultiValued() ? binding.getBindValues() : binding.getBindValue();
	}

	public Object getParameterValue(int position) {
		checkOpenNoRollback();
		final var binding = getQueryParameterBindings().getBinding( position );
		if ( !binding.isBound() ) {
			throw new IllegalStateException( "The parameter [" + position + "] has not yet been bound" );
		}
		return binding.isMultiValued() ? binding.getBindValues() : binding.getBindValue();
	}

	private <P> void setParameterValue(Object value, QueryParameterBinding<P> binding) {
		final var parameterType = binding.getBindType();
		if ( parameterType != null
				&& !isInstanceOrAreInstances( value, binding, parameterType ) ) {
			throw new QueryArgumentException( "Argument to query parameter has an incompatible type",
					parameterType.getJavaType(), value.getClass(), value );
		}
		@SuppressWarnings("unchecked") // safe, just checked
		final var castValue = (P) value;
		binding.setBindValue( castValue, resolveJdbcParameterTypeIfNecessary() );
	}

	private <P> boolean isInstanceOrAreInstances(
			Object value, QueryParameterBinding<P> binding, BindableType<? super P> parameterType) {
		return binding.isMultiValued() && value instanceof Collection<?> values
				? areInstances( parameterType, values )
				: isInstance( parameterType, value );
	}

	@Override
	public CommonQueryContract setParameter(String name, Object value) {
		checkOpenNoRollback();
		if ( value instanceof TypedParameterValue<?> typedParameterValue ) {
			setTypedParameter( name, typedParameterValue );
		}
		else {
			final var binding = getQueryParameterBindings().getBinding( name );
			if ( multipleBinding( binding.getQueryParameter(), value ) ) {
				setParameterList( name, (Collection<?>) value );
			}
			else {
				setParameterValue( value, binding );
			}
		}
		return this;
	}

	private boolean multipleBinding(QueryParameter<?> parameter, Object value){
		if ( parameter.allowsMultiValuedBinding()
				&& value instanceof Collection<?> values
				// this check only needed for some native queries
				&& !isRegisteredAsBasicType( value.getClass() ) ) {
			final var hibernateType = parameter.getHibernateType();
			return hibernateType == null
				|| values.isEmpty()
				|| !isInstance( hibernateType, value )
				|| isInstance( hibernateType, values.iterator().next() );
		}
		else {
			return false;
		}
	}

	private <T> void setTypedParameter(String name, TypedParameterValue<T> typedValue) {
		setParameter( name, typedValue.value(), typedValue.type() );
	}

	private <T> void setTypedParameter(int position, TypedParameterValue<T> typedValue) {
		setParameter( position, typedValue.value(), typedValue.type() );
	}

	private boolean isInstance(Type<?> parameterType, Object value) {
		if ( value == null ) {
			return true;
		}
		final var sqmExpressible = getNodeBuilder().resolveExpressible( parameterType );
		assert sqmExpressible != null;
		final var javaType = sqmExpressible.getExpressibleJavaType();
		if ( !javaType.isInstance( value ) ) {
			try {
				// if this succeeds, we are good
				javaType.wrap( value, session );
			}
			catch ( HibernateException|UnsupportedOperationException e) {
				return false;
			}
		}
		return true;
	}

	private boolean areInstances(Type<?> parameterType, Collection<?> values) {
		if ( values.isEmpty() ) {
			return true;
		}
		final var sqmExpressible = getNodeBuilder().resolveExpressible( parameterType );
		assert sqmExpressible != null;
		final var javaType = sqmExpressible.getExpressibleJavaType();
		for ( Object value : values ) {
			if ( !javaType.isInstance( value ) ) {
				try {
					// if this succeeds, we are good
					javaType.wrap( value, session );
				}
				catch (HibernateException | UnsupportedOperationException e) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean areInstances(Type<?> parameterType, Object[] values) {
		if ( values.length == 0 ) {
			return true;
		}
		final var sqmExpressible = getNodeBuilder().resolveExpressible( parameterType );
		assert sqmExpressible != null;
		final var javaType = sqmExpressible.getExpressibleJavaType();
		for ( Object value : values ) {
			if ( !javaType.isInstance( value ) ) {
				try {
					// if this succeeds, we are good
					javaType.wrap( value, session );
				}
				catch (HibernateException | UnsupportedOperationException e) {
					return false;
				}
			}
		}
		return true;
	}

	private NodeBuilder getNodeBuilder() {
		return getSessionFactory().getQueryEngine().getCriteriaBuilder();
	}

	@Override
	public <P> CommonQueryContract setParameter(String name, P value, Class<P> javaType) {
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
	public <P> CommonQueryContract setParameter(String name, P value, Type<P> type) {
		locateBinding( name, type.getJavaType(), value ).setBindValue( value, (BindableType<P>) type );
		return this;
	}

	@Override @Deprecated(since = "7")
	public CommonQueryContract setParameter(String name, Instant value, TemporalType temporalType) {
		locateBinding( name, Instant.class, value ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public CommonQueryContract setParameter(int position, Object value) {
		checkOpenNoRollback();
		if ( value instanceof TypedParameterValue<?> typedParameterValue ) {
			setTypedParameter( position, typedParameterValue );
		}
		else {
			final var binding = getQueryParameterBindings().getBinding( position );
			if ( multipleBinding( binding.getQueryParameter(), value ) ) {
				setParameterList( position, (Collection<?>) value );
			}
			else {
				setParameterValue( value, binding );
			}
		}
		return this;
	}

	private boolean isRegisteredAsBasicType(Class<?> valueClass) {
		return getTypeConfiguration().getBasicTypeForJavaType( valueClass ) != null;
	}

	@Override
	public <P> CommonQueryContract setParameter(int position, P value, Class<P> javaType) {
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
	public <P> CommonQueryContract setParameter(int position, P value, Type<P> type) {
		locateBinding( position, type.getJavaType(), value ).setBindValue( value, (BindableType<P>) type );
		return this;
	}

	@Override @Deprecated(since = "7")
	public CommonQueryContract setParameter(int position, Instant value, TemporalType temporalType) {
		locateBinding( position, Instant.class, value ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public <P> CommonQueryContract setParameter(QueryParameter<P> parameter, P value) {
		locateBinding( parameter, value )
				.setBindValue( value, resolveJdbcParameterTypeIfNecessary() );
		return this;
	}

	@Override
	public <P> CommonQueryContract setParameter(QueryParameter<P> parameter, P value, Class<P> javaType) {
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
	public <P> CommonQueryContract setParameter(QueryParameter<P> parameter, P value, Type<P> type) {
		locateBinding( parameter, value )
				.setBindValue( value, (BindableType<P>) type );
		return this;
	}


	@Override
	public <P> CommonQueryContract setParameter(Parameter<P> parameter, P value) {
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
				locateBinding( parameter, castValue )
						.setBindValue( castValue, castType );
			}
		}
		else {
			locateBinding( parameter, value )
					.setBindValue( value, resolveJdbcParameterTypeIfNecessary() );
		}
		return this;
	}

	@Override @Deprecated
	public CommonQueryContract setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		locateBinding( param, value ).setBindValue( value, temporalType );
		return this;
	}

	@Override @Deprecated
	public CommonQueryContract setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		locateBinding( param, value ).setBindValue( value, temporalType );
		return this;
	}

	@Override @Deprecated
	public CommonQueryContract setParameter(String name, Calendar value, TemporalType temporalType) {
		locateBinding( name, Calendar.class, value ).setBindValue( value, temporalType );
		return this;
	}

	@Override @Deprecated
	public CommonQueryContract setParameter(String name, Date value, TemporalType temporalType) {
		locateBinding( name, Date.class, value ).setBindValue( value, temporalType );
		return this;
	}

	@Override @Deprecated
	public CommonQueryContract setParameter(int position, Calendar value, TemporalType temporalType) {
		locateBinding( position, Calendar.class, value ).setBindValue( value, temporalType );
		return this;
	}

	@Override @Deprecated
	public CommonQueryContract setParameter(int position, Date value, TemporalType temporalType) {
		locateBinding( position, Date.class, value ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public CommonQueryContract setParameterList(String name, @SuppressWarnings("rawtypes") Collection values) {
		getQueryParameterBindings().getBinding( name ).setBindValues( values );
		return this;
	}

	public <P> CommonQueryContract setParameterList(String name, Collection<? extends P> values, Class<P> javaType) {
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
	public <P> CommonQueryContract setParameterList(String name, Collection<? extends P> values, Type<P> type) {
		locateBinding( name, type.getJavaType(), values )
				.setBindValues( values, (BindableType<P>) type );
		return this;
	}

	@Override
	public CommonQueryContract setParameterList(String name, Object[] values) {
		final var binding = getQueryParameterBindings().getBinding( name );
		setParameterValues( values, binding );
		return this;
	}

	private <P> void setParameterValues(Object[] values, QueryParameterBinding<P> binding) {
		final var parameterType = binding.getBindType();
		if ( parameterType != null
				&& !areInstances( values, parameterType ) ) {
			throw new QueryArgumentException( "Argument to query parameter has an incompatible type",
					parameterType.getJavaType(), values.getClass().getComponentType(), values );
		}
		@SuppressWarnings("unchecked") // safe, just checked
		final var castArray = (P[]) values;
		binding.setBindValues( List.of( castArray ) );
	}

	private <P> boolean areInstances(Object[] values, BindableType<? super P> parameterType) {
		return parameterType.getJavaType().isAssignableFrom( values.getClass().getComponentType() )
			|| areInstances( parameterType, values );
	}

	@Override
	public <P> CommonQueryContract setParameterList(String name, P[] values, Class<P> javaType) {
		final var javaDescriptor = getJavaType( javaType );
		if ( javaDescriptor == null ) {
			setParameterList( name, values );
		}
		else {
			setParameterList( name, values, getParamType( javaType ) );
		}
		return this;
	}

	public <P> CommonQueryContract setParameterList(String name, P[] values, Type<P> type) {
		final var list = List.of( values );
		locateBinding( name, type.getJavaType(), list )
				.setBindValues( list, (BindableType<P>) type );
		return this;
	}

	@Override
	public CommonQueryContract setParameterList(int position, @SuppressWarnings("rawtypes") Collection values) {
		//TODO: type checking?
		getQueryParameterBindings().getBinding( position ).setBindValues( values );
		return this;
	}

	@Override
	public <P> CommonQueryContract setParameterList(int position, Collection<? extends P> values, Class<P> javaType) {
		final var javaDescriptor = getJavaType( javaType );
		if ( javaDescriptor == null ) {
			setParameterList( position, values );
		}
		else {
			setParameterList( position, values, getParamType( javaType ) );
		}
		return this;
	}

	private <P> Type<P> getParamType(Class<P> javaType) {
		final var basicType =
				getTypeConfiguration()
						.standardBasicTypeForJavaType( javaType );
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

	@Override
	public <P> CommonQueryContract setParameterList(int position, Collection<? extends P> values, Type<P> type) {
		locateBinding( position, type.getJavaType(), values )
				.setBindValues( values, (BindableType<P>) type );
		return this;
	}

	@Override
	public CommonQueryContract setParameterList(int position, Object[] values) {
		final var binding = getQueryParameterBindings().getBinding( position );
		setParameterValues( values, binding );
		return this;
	}

	@Override
	public <P> CommonQueryContract setParameterList(int position, P[] values, Class<P> javaType) {
		final var javaDescriptor = getJavaType( javaType );
		if ( javaDescriptor == null ) {
			setParameterList( position, values );
		}
		else {
			setParameterList( position, values, getParamType( javaType ) );
		}
		return this;
	}

	public <P> CommonQueryContract setParameterList(int position, P[] values, Type<P> type) {
		final var list = List.of( values );
		locateBinding( position, type.getJavaType(), list )
				.setBindValues( list, (BindableType<P>) type );
		return this;
	}


	@Override
	public <P> CommonQueryContract setParameterList(QueryParameter<P> parameter, Collection<? extends P> values) {
		locateBinding( parameter, values ).setBindValues( values );
		return this;
	}

	@Override
	public <P> CommonQueryContract setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType) {
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
	public <P> CommonQueryContract setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Type<P> type) {
		locateBinding( parameter, values )
				.setBindValues( values, (BindableType<P>) type );
		return this;
	}

	@Override
	public <P> CommonQueryContract setParameterList(QueryParameter<P> parameter, P[] values) {
		final var list = List.of( values );
		locateBinding( parameter, list ).setBindValues( list );
		return this;
	}

	@Override
	public <P> CommonQueryContract setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType) {
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
	public <P> CommonQueryContract setParameterList(QueryParameter<P> parameter, P[] values, Type<P> type) {
		final var list = List.of( values );
		locateBinding( parameter, list )
				.setBindValues( list, (BindableType<P>) type );
		return this;
	}

	@Override
	public CommonQueryContract setProperties(@SuppressWarnings("rawtypes") Map map) {
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

	@Override
	public CommonQueryContract setProperties(Object bean) {
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
}
