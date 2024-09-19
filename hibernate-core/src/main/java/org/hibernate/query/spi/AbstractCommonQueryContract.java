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
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.jpa.internal.util.ConfigurationHelper;
import org.hibernate.jpa.internal.util.FlushModeTypeHelper;
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
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.expression.NullSqmExpressible;
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

import static java.lang.Boolean.TRUE;
import static java.util.Locale.ROOT;
import static org.hibernate.LockOptions.WAIT_FOREVER;
import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;
import static org.hibernate.jpa.HibernateHints.HINT_CACHEABLE;
import static org.hibernate.jpa.HibernateHints.HINT_CACHE_MODE;
import static org.hibernate.jpa.HibernateHints.HINT_CACHE_REGION;
import static org.hibernate.jpa.HibernateHints.HINT_COMMENT;
import static org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE;
import static org.hibernate.jpa.HibernateHints.HINT_FLUSH_MODE;
import static org.hibernate.jpa.HibernateHints.HINT_FOLLOW_ON_LOCKING;
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
import static org.hibernate.jpa.internal.util.LockModeTypeHelper.interpretLockMode;

/**
 * @author Steve Ebersole
 */
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
			hints.put( HINT_SPEC_QUERY_TIMEOUT, getQueryOptions().getTimeout() * 1000 );
		}

		putIfNotNull( hints, HINT_COMMENT, getComment() );
		putIfNotNull( hints, HINT_FLUSH_MODE, getHibernateFlushMode() );

		putIfNotNull( hints, HINT_READONLY, getQueryOptions().isReadOnly() );
		putIfNotNull( hints, HINT_FETCH_SIZE, getQueryOptions().getFetchSize() );
		putIfNotNull( hints, HINT_CACHEABLE, getQueryOptions().isResultCachingEnabled() );
		putIfNotNull( hints, HINT_CACHE_REGION, getQueryOptions().getResultCacheRegionName() );
		putIfNotNull( hints, HINT_CACHE_MODE, getQueryOptions().getCacheMode() );
		putIfNotNull( hints, HINT_QUERY_PLAN_CACHEABLE, getQueryOptions().getQueryPlanCachingEnabled() );

		putIfNotNull( hints, HINT_SPEC_CACHE_RETRIEVE_MODE, getQueryOptions().getCacheRetrieveMode() );
		putIfNotNull( hints, HINT_JAVAEE_CACHE_RETRIEVE_MODE, getQueryOptions().getCacheRetrieveMode() );

		putIfNotNull( hints, HINT_SPEC_CACHE_STORE_MODE, getQueryOptions().getCacheStoreMode() );
		putIfNotNull( hints, HINT_JAVAEE_CACHE_STORE_MODE, getQueryOptions().getCacheStoreMode() );

		final AppliedGraph appliedGraph = getQueryOptions().getAppliedGraph();
		if ( appliedGraph != null && appliedGraph.getSemantic() != null ) {
			hints.put( appliedGraph.getSemantic().getJakartaHintName(), appliedGraph.getGraph() );
			hints.put( appliedGraph.getSemantic().getJpaHintName(), appliedGraph.getGraph() );
		}

		final LockOptions lockOptions = getQueryOptions().getLockOptions();
		if ( ! lockOptions.isEmpty() ) {
			final LockMode lockMode = lockOptions.getLockMode();
			if ( lockMode != null && lockMode != LockMode.NONE ) {
				hints.put( HINT_NATIVE_LOCKMODE, lockMode );
			}

			if ( lockOptions.hasAliasSpecificLockModes() ) {
				for ( Map.Entry<String, LockMode> entry : lockOptions.getAliasSpecificLocks() ) {
					hints.put(
							HINT_NATIVE_LOCKMODE + "." + entry.getKey(),
							entry.getValue()
					);
				}
			}

			if ( lockOptions.getFollowOnLocking() == TRUE ) {
				hints.put( HINT_FOLLOW_ON_LOCKING, TRUE );
			}

			if ( lockOptions.getTimeOut() != WAIT_FOREVER ) {
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
		applyHint( hintName, value );
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
					int timeout = (int) Math.round( getInteger( value ).doubleValue() / 1000.0 );
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
					if ( applySelectionHint( hintName, value ) || applyAdditionalPossibleHints( hintName, value ) ) {
						return true;
					}
					else {
						QueryLogging.QUERY_MESSAGE_LOGGER.ignoringUnrecognizedQueryHint( hintName );
						return false;
					}
			}
		}
		catch ( ClassCastException e ) {
			throw new IllegalArgumentException( "Incorrect value for query hint: " + hintName, e );
		}
	}

	protected void applySynchronizeSpacesHint(Object value) {
		QueryLogging.QUERY_LOGGER.debug( "Query spaces hint was specified for non-native query; ignoring" );
	}

	protected final boolean applySelectionHint(String hintName, Object value) {
		if ( applyLockingHint( hintName, value ) ) {
			return true;
		}
		else {
			switch ( hintName ) {
				case HINT_READONLY:
					applyReadOnlyHint( getBoolean( value ) );
					return true;
				case HINT_FETCH_SIZE:
					applyFetchSizeHint( getInteger( value ) );
					return true;
				case HINT_QUERY_PLAN_CACHEABLE:
					applyQueryPlanCacheableHint( getBoolean( value ) );
					return true;
				case HINT_CACHEABLE:
					applyCacheableHint( getBoolean( value ) );
					return true;
				case HINT_CACHE_REGION:
					applyCacheRegionHint( (String) value );
					return true;
				case HINT_CACHE_MODE:
					applyCacheModeHint( getCacheMode( value ) );
					return true;
				case HINT_JAVAEE_CACHE_RETRIEVE_MODE:
					DEPRECATION_LOGGER.deprecatedSetting( HINT_JAVAEE_CACHE_RETRIEVE_MODE, HINT_SPEC_CACHE_RETRIEVE_MODE );
					//fall through to:
				case HINT_SPEC_CACHE_RETRIEVE_MODE:
					applyJpaCacheRetrieveModeHint( value != null ? CacheRetrieveMode.valueOf( value.toString() ) : null );
					return true;
				case HINT_JAVAEE_CACHE_STORE_MODE:
					DEPRECATION_LOGGER.deprecatedSetting( HINT_JAVAEE_CACHE_STORE_MODE, HINT_SPEC_CACHE_STORE_MODE );
					//fall through to:
				case HINT_SPEC_CACHE_STORE_MODE:
					applyJpaCacheStoreModeHint( value != null ? CacheStoreMode.valueOf( value.toString() ) : null );
					return true;
				case HINT_JAVAEE_FETCH_GRAPH:
					DEPRECATION_LOGGER.deprecatedSetting( HINT_JAVAEE_FETCH_GRAPH, HINT_SPEC_FETCH_GRAPH );
					//fall through to:
				case HINT_SPEC_FETCH_GRAPH:
					applyEntityGraphHint( hintName, value );
					return true;
				case HINT_JAVAEE_LOAD_GRAPH:
					DEPRECATION_LOGGER.deprecatedSetting( HINT_JAVAEE_LOAD_GRAPH, HINT_SPEC_LOAD_GRAPH );
					//fall through to:
				case HINT_SPEC_LOAD_GRAPH:
					applyEntityGraphHint( hintName, value );
					return true;
				default:
					// unrecognized hint
					return false;
			}
		}
	}

	protected void applyFetchSizeHint(int fetchSize) {
		getQueryOptions().setFetchSize( fetchSize );
	}

	protected void applyQueryPlanCacheableHint(boolean isCacheable) {
		getQueryOptions().setQueryPlanCachingEnabled( isCacheable );
	}

	protected void applyCacheModeHint(CacheMode cacheMode) {
		getQueryOptions().setCacheMode( cacheMode );
	}

	protected void applyCacheableHint(boolean isCacheable) {
		getQueryOptions().setResultCachingEnabled( isCacheable );
	}

	protected void applyCacheRegionHint(String regionName) {
		getQueryOptions().setResultCacheRegionName( regionName );
	}

	private void applyReadOnlyHint(Boolean readOnly) {
		getQueryOptions().setReadOnly( readOnly );
	}

	protected void applyEntityGraphHint(String hintName, Object value) {
		final GraphSemantic graphSemantic = GraphSemantic.fromHintName( hintName );
		if ( value instanceof RootGraphImplementor ) {
			applyGraph( (RootGraphImplementor<?>) value, graphSemantic );
		}
		else if ( value instanceof String ) {
			applyGraph( (String) value, graphSemantic );
		}
		else {
			throw new IllegalArgumentException( "The value of the hint '" + hintName
					+ "' must be an instance of EntityGraph or the string name of a named EntityGraph" );
		}
	}

	protected void applyGraph(String graphString, GraphSemantic graphSemantic) {
		final int separatorPosition = graphString.indexOf( '(' );
		final int terminatorPosition = graphString.lastIndexOf( ')' );
		if ( separatorPosition < 0 || terminatorPosition < 0 ) {
			throw new IllegalArgumentException(
					String.format(
							ROOT,
							"Invalid entity-graph definition `%s`; expected form `${EntityName}( ${property1} ... )",
							graphString
					)
			);
		}

		final RuntimeMetamodels runtimeMetamodels = getSession().getFactory().getRuntimeMetamodels();
		final JpaMetamodel jpaMetamodel = runtimeMetamodels.getJpaMetamodel();

		final String entityName = runtimeMetamodels.getImportedName( graphString.substring( 0, separatorPosition ).trim() );
		final String graphNodes = graphString.substring( separatorPosition + 1, terminatorPosition );

		final RootGraphImpl<?> rootGraph = new RootGraphImpl<>( null, jpaMetamodel.entity( entityName ) );
		GraphParser.parseInto( (EntityGraph<?>) rootGraph, graphNodes, getSession().getSessionFactory() );
		applyGraph( rootGraph, graphSemantic );
	}

	protected void applyGraph(RootGraphImplementor<?> entityGraph, GraphSemantic graphSemantic) {
		getQueryOptions().applyGraph( entityGraph, graphSemantic );
	}

	private boolean applyLockingHint(String hintName, Object value) {
		switch ( hintName ) {
			case HINT_JAVAEE_LOCK_TIMEOUT:
				DEPRECATION_LOGGER.deprecatedSetting( HINT_JAVAEE_LOCK_TIMEOUT, HINT_SPEC_LOCK_TIMEOUT );
				//fall through to:
			case HINT_SPEC_LOCK_TIMEOUT:
				if ( value != null ) {
					applyLockTimeoutHint( getInteger( value ) );
					return true;
				}
				else {
					return false;
				}
			case HINT_FOLLOW_ON_LOCKING:
				applyFollowOnLockingHint( getBoolean( value ) );
				return true;
			case HINT_NATIVE_LOCKMODE:
				applyLockModeHint( value );
				return true;
			default:
				if ( hintName.startsWith( HINT_NATIVE_LOCKMODE ) ) {
					applyAliasSpecificLockModeHint( hintName, value );
					return true;
				}
				else {
					return false;
				}
		}
	}

	protected void applyLockTimeoutHint(Integer timeout) {
		if ( timeout != null ) {
			applyLockTimeoutHint( (int) timeout );
		}
	}

	protected void applyLockTimeoutHint(int timeout) {
		getQueryOptions().getLockOptions().setTimeOut( timeout );
	}

	protected void applyHibernateLockMode(LockMode value) {
		getQueryOptions().getLockOptions().setLockMode( value );
	}

	protected void applyLockModeType(LockModeType value) {
		applyHibernateLockMode( LockMode.fromJpaLockMode( value ) );
	}

	protected final void applyLockModeHint(Object value) {
		if ( value instanceof LockMode ) {
			applyHibernateLockMode( (LockMode) value );
		}
		else if ( value instanceof LockModeType ) {
			applyLockModeType( (LockModeType) value );
		}
		else if ( value instanceof String ) {
			applyHibernateLockMode( interpretLockMode( value ) );
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
	}

	protected void applyAliasSpecificLockModeHint(String hintName, Object value) {
		// extract the alias
		final String alias = hintName.substring( HINT_NATIVE_LOCKMODE.length() + 1 );
		// determine the LockMode
		try {
			getQueryOptions().getLockOptions().setAliasSpecificLockMode( alias, interpretLockMode( value ) );
		}
		catch ( Exception e ) {
			QueryLogging.QUERY_MESSAGE_LOGGER.unableToDetermineLockModeValue( hintName, value );
		}
	}

	protected void applyFollowOnLockingHint(Boolean followOnLocking) {
		getQueryOptions().getLockOptions().setFollowOnLocking( followOnLocking );
	}

	protected boolean applyAdditionalPossibleHints(String hintName, Object value) {
		return false;
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

	protected boolean applyJpaCacheRetrieveModeHint(CacheRetrieveMode retrieveMode) {
		getQueryOptions().setCacheRetrieveMode( retrieveMode );
		return true;
	}

	protected boolean applyJpaCacheStoreModeHint(CacheStoreMode storeMode) {
		getQueryOptions().setCacheStoreMode( storeMode );
		return true;
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

	public boolean applyTupleTransformer(TupleTransformer<?> transformer) {
		getQueryOptions().setTupleTransformer( transformer );
		return true;
	}

	public boolean applyResultListTransformer(ResultListTransformer<?> transformer) {
		getQueryOptions().setResultListTransformer( transformer );
		return true;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter handling

	protected abstract ParameterMetadataImplementor getParameterMetadata();

	@SuppressWarnings({"unchecked", "rawtypes"})
	public Set<Parameter<?>> getParameters() {
		getSession().checkOpen( false );
		return (Set) getParameterMetadata().getRegistrations();
	}

	public QueryParameterImplementor<?> getParameter(String name) {
		getSession().checkOpen( false );

		try {
			return getParameterMetadata().getQueryParameter( name );
		}
		catch ( HibernateException e ) {
			throw getSession().getExceptionConverter().convert( e );
		}
	}

	@SuppressWarnings("unchecked")
	public <T> QueryParameterImplementor<T> getParameter(String name, Class<T> type) {
		getSession().checkOpen( false );

		try {
			//noinspection rawtypes
			final QueryParameterImplementor parameter = getParameterMetadata().getQueryParameter( name );
			if ( !type.isAssignableFrom( parameter.getParameterType() ) ) {
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

	public QueryParameterImplementor<?> getParameter(int position) {
		getSession().checkOpen( false );

		try {
			return getParameterMetadata().getQueryParameter( position );
		}
		catch ( HibernateException e ) {
			throw getSession().getExceptionConverter().convert( e );
		}
	}

	@SuppressWarnings( {"unchecked", "rawtypes"} )
	public <T> QueryParameterImplementor<T> getParameter(int position, Class<T> type) {
		getSession().checkOpen( false );

		try {
			final QueryParameterImplementor parameter = getParameterMetadata().getQueryParameter( position );
			if ( !type.isAssignableFrom( parameter.getParameterType() ) ) {
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

	protected <P> QueryParameterBinding<P> locateBinding(String name) {
		getSession().checkOpen();
		return getQueryParameterBindings().getBinding( name );
	}

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

		final QueryParameterBinding<Object> binding = getQueryParameterBindings().getBinding( name );
		if ( !binding.isBound() ) {
			throw new IllegalStateException( "The parameter [" + name + "] has not yet been bound" );
		}

		if ( binding.isMultiValued() ) {
			return binding.getBindValues();
		}
		else {
			return binding.getBindValue();
		}
	}

	public Object getParameterValue(int position) {
		getSession().checkOpen( false );

		final QueryParameterBinding<Object> binding = getQueryParameterBindings().getBinding( position );
		if ( !binding.isBound() ) {
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
		getSession().checkOpen( false );

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

		final QueryParameterBinding<Object> binding = getQueryParameterBindings().getBinding( name );
		final QueryParameter<Object> param = binding.getQueryParameter();
		if ( param.allowsMultiValuedBinding() ) {
			final BindableType<?> hibernateType = param.getHibernateType();
			if ( hibernateType == null || isInstance( hibernateType, value ) ) {
				if ( value instanceof Collection && !isRegisteredAsBasicType( value.getClass() ) ) {
					//noinspection rawtypes
					return setParameterList( name, (Collection) value );
				}
			}
		}

		binding.setBindValue( value, resolveJdbcParameterTypeIfNecessary() );

		return this;
	}


	private boolean isInstance(BindableType<?> parameterType, Object value) {
		final SqmExpressible<?> sqmExpressible = parameterType.resolveExpressible( getSession().getFactory() );
		assert sqmExpressible != null;

		return sqmExpressible.getExpressibleJavaType().isInstance( value );
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
			final BasicType<P> basicType = getSession().getFactory()
					.getTypeConfiguration()
					.standardBasicTypeForJavaType( javaType );
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
		getSession().checkOpen( false );

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

		final QueryParameterBinding<Object> binding = getQueryParameterBindings().getBinding( position );
		final QueryParameter<Object> param = binding.getQueryParameter();
		if ( param.allowsMultiValuedBinding() ) {
			final BindableType<?> hibernateType = param.getHibernateType();
			if ( hibernateType == null || hibernateType instanceof NullSqmExpressible || isInstance( hibernateType, value ) ) {
				if ( value instanceof Collection && !isRegisteredAsBasicType( value.getClass() ) ) {
					//noinspection rawtypes
					return setParameterList( position, (Collection) value );
				}
			}
		}

		binding.setBindValue( value, resolveJdbcParameterTypeIfNecessary() );
		return this;
	}

	private boolean isRegisteredAsBasicType(Class<?> valueClass) {
		return getSession().getTypeConfiguration().getBasicTypeForJavaType( valueClass ) != null;
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
			final BasicType<P> basicType = getSession().getFactory()
					.getTypeConfiguration()
					.standardBasicTypeForJavaType( javaType );
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
			final BasicType<P> basicType = getSession().getFactory()
					.getTypeConfiguration()
					.standardBasicTypeForJavaType( javaType );
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
	public CommonQueryContract setProperties(@SuppressWarnings("rawtypes") Map map) {
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

	protected BindableType<Object> determineType(String namedParam, Class<?> retType) {
		BindableType<?> type = locateBinding( namedParam ).getBindType();
		if ( type == null ) {
			type = getParameterMetadata().getQueryParameter( namedParam ).getHibernateType();
		}
		if ( type == null && retType != null ) {
			type = getSession().getFactory().getMappingMetamodel().resolveParameterBindType( retType );
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
