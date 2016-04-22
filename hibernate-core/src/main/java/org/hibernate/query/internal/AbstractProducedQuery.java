/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.CacheRetrieveMode;
import javax.persistence.CacheStoreMode;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.TemporalType;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NonUniqueResultException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.TypeMismatchException;
import org.hibernate.engine.query.spi.EntityGraphQueryHint;
import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.hql.internal.QueryExecutionRequestException;
import org.hibernate.internal.EntityManagerMessageLogger;
import org.hibernate.internal.HEMLogging;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.QueryHints;
import org.hibernate.jpa.graph.internal.EntityGraphImpl;
import org.hibernate.jpa.internal.util.CacheModeHelper;
import org.hibernate.jpa.internal.util.ConfigurationHelper;
import org.hibernate.jpa.internal.util.FlushModeTypeHelper;
import org.hibernate.jpa.internal.util.LockModeTypeHelper;
import org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

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
 * @author Steve Ebersole
 */
public abstract class AbstractProducedQuery<R> implements QueryImplementor<R> {
	private static final EntityManagerMessageLogger log = HEMLogging.messageLogger( AbstractProducedQuery.class );

	private final SharedSessionContractImplementor producer;
	private final ParameterMetadata parameterMetadata;
	private final QueryParameterBindingsImpl queryParameterBindings;

	private FlushMode flushMode;
	private CacheMode cacheMode;
	private Integer timeout;
	private boolean cacheable;
	private String cacheRegion;
	private boolean readOnly;

	private LockOptions lockOptions = new LockOptions();

	private Integer fetchSize;

	private String comment;
	private final List<String> dbHints = new ArrayList<>();
	private Map<String, Object> hints;

	private ResultTransformer resultTransformer;
	private RowSelection selection = new RowSelection();
	private HQLQueryPlan entityGraphHintedQueryPlan;

	private Object optionalObject;
	private Serializable optionalId;
	private String optionalEntityName;

	public AbstractProducedQuery(
			SharedSessionContractImplementor producer,
			ParameterMetadata parameterMetadata) {
		this.producer = producer;
		this.parameterMetadata = parameterMetadata;
		this.queryParameterBindings = QueryParameterBindingsImpl.from( parameterMetadata, producer.getFactory() );
	}

	@Override
	public SharedSessionContractImplementor getProducer() {
		return producer;
	}

	@Override
	public FlushMode getHibernateFlushMode() {
		return flushMode;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setHibernateFlushMode(FlushMode flushMode) {
		this.flushMode = flushMode;
		return this;
	}

	@Override
	public QueryImplementor setFlushMode(FlushMode flushMode) {
		return setHibernateFlushMode( flushMode );
	}

	@Override
	public FlushModeType getFlushMode() {
		return FlushModeTypeHelper.getFlushModeType( flushMode );
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setFlushMode(FlushModeType flushModeType) {
		setHibernateFlushMode( FlushModeTypeHelper.getFlushMode( flushModeType ) );
		return this;
	}

	@Override
	public CacheMode getCacheMode() {
		return cacheMode;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setCacheMode(CacheMode cacheMode) {
		this.cacheMode = cacheMode;
		return this;
	}

	@Override
	public boolean isCacheable() {
		return cacheable;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setCacheable(boolean cacheable) {
		this.cacheable = cacheable;
		return this;
	}

	@Override
	public String getCacheRegion() {
		return cacheRegion;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setCacheRegion(String cacheRegion) {
		this.cacheRegion = cacheRegion;
		return this;
	}

	@Override
	public Integer getTimeout() {
		return timeout;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setTimeout(int timeout) {
		this.timeout = timeout;
		return this;
	}

	@Override
	public Integer getFetchSize() {
		return fetchSize;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
		return this;
	}

	@Override
	public boolean isReadOnly() {
		return readOnly;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
		return this;
	}

	@Override
	public LockOptions getLockOptions() {
		return lockOptions;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setLockOptions(LockOptions lockOptions) {
		this.lockOptions.setLockMode( lockOptions.getLockMode() );
		this.lockOptions.setScope( lockOptions.getScope() );
		this.lockOptions.setTimeOut( lockOptions.getTimeOut() );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setLockMode(String alias, LockMode lockMode) {
		lockOptions.setAliasSpecificLockMode( alias, lockMode );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setLockMode(LockModeType lockModeType) {
		lockOptions.setLockMode( LockModeTypeHelper.getLockMode( lockModeType ) );
		return this;
	}

	@Override
	public String getComment() {
		return comment;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setComment(String comment) {
		this.comment = comment;
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor addQueryHint(String hint) {
		this.dbHints.add( hint );
		return this;
	}

	@Override
	public ParameterMetadata getParameterMetadata() {
		return parameterMetadata;
	}

	@Override
	public String[] getNamedParameters() {
		return ArrayHelper.toStringArray( getParameterMetadata().getNamedParameterNames() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(QueryParameter parameter, Object value) {
		queryParameterBindings.getBinding( parameter ).setBindValue( value );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(Parameter parameter, Object value) {
		queryParameterBindings.getBinding( (QueryParameter) parameter ).setBindValue( value );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(String name, Object value) {
		queryParameterBindings.getBinding( name ).setBindValue( value );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(int position, Object value) {
		queryParameterBindings.getBinding( position ).setBindValue( value );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(QueryParameter parameter, Object value, Type type) {
		queryParameterBindings.getBinding( parameter ).setBindValue( value, type );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(String name, Object value, Type type) {
		queryParameterBindings.getBinding( name ).setBindValue( value, type );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(int position, Object value, Type type) {
		queryParameterBindings.getBinding( position ).setBindValue( value, type );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(QueryParameter parameter, Object value, TemporalType temporalType) {
		queryParameterBindings.getBinding( parameter ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(String name, Object value, TemporalType temporalType) {
		queryParameterBindings.getBinding( name ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(int position, Object value, TemporalType temporalType) {
		queryParameterBindings.getBinding( position ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameterList(QueryParameter parameter, Collection values) {
		queryParameterBindings.getQueryParameterListBinding( parameter ).setBindValues( values );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameterList(String name, Collection values) {
		queryParameterBindings.getQueryParameterListBinding( name ).setBindValues( values );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameterList(String name, Collection values, Type type) {
		queryParameterBindings.getQueryParameterListBinding( name ).setBindValues( values, type );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameterList(String name, Object[] values, Type type) {
		queryParameterBindings.getQueryParameterListBinding( name ).setBindValues( Arrays.asList( values ), type );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameterList(String name, Object[] values) {
		queryParameterBindings.getQueryParameterListBinding( name ).setBindValues( Arrays.asList( values ) );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(Parameter param, Calendar value, TemporalType temporalType) {
		queryParameterBindings.getBinding( (QueryParameter) param ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(Parameter param, Date value, TemporalType temporalType) {
		queryParameterBindings.getBinding( (QueryParameter) param ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(String name, Calendar value, TemporalType temporalType) {
		queryParameterBindings.getBinding( name ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(String name, Date value, TemporalType temporalType) {
		queryParameterBindings.getBinding( name ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(int position, Calendar value, TemporalType temporalType) {
		queryParameterBindings.getBinding( position ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(int position, Date value, TemporalType temporalType) {
		queryParameterBindings.getBinding( position ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public Set<Parameter<?>> getParameters() {
		return parameterMetadata.collectAllParametersJpa();
	}

	@Override
	public Parameter<?> getParameter(String name) {
		return parameterMetadata.getQueryParameter( name );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Parameter<T> getParameter(String name, Class<T> type) {
		final QueryParameter parameter = parameterMetadata.getQueryParameter( name );
		if ( !parameter.getParameterType().isAssignableFrom( type ) ) {
			throw new IllegalArgumentException(
					"The type [" + parameter.getParameterType().getName() +
							"] associated with the parameter corresponding to name [" + name +
							"] is not assignable to requested Java type [" + type.getName() + "]"
			);
		}
		return parameter;
	}

	@Override
	public Parameter<?> getParameter(int position) {
		return parameterMetadata.getQueryParameter( position );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Parameter<T> getParameter(int position, Class<T> type) {
		final QueryParameter parameter = parameterMetadata.getQueryParameter( position );
		if ( !parameter.getParameterType().isAssignableFrom( type ) ) {
			throw new IllegalArgumentException(
					"The type [" + parameter.getParameterType().getName() +
							"] associated with the parameter corresponding to position [" + position +
							"] is not assignable to requested Java type [" + type.getName() + "]"
			);
		}
		return parameter;
	}

	@Override
	public boolean isBound(Parameter<?> parameter) {
		return queryParameterBindings.isBound( (QueryParameter) parameter );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getParameterValue(Parameter<T> parameter) {
		return (T) queryParameterBindings.getBinding( (QueryParameter) parameter ).getBindValue();
	}

	@Override
	public Object getParameterValue(String name) {
		return queryParameterBindings.getBinding( name ).getBindValue();
	}

	@Override
	public Object getParameterValue(int position) {
		return queryParameterBindings.getBinding( position ).getBindValue();
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setProperties(Object bean) {
		Class clazz = bean.getClass();
		String[] params = getNamedParameters();
		for ( String namedParam : params ) {
			try {
				final PropertyAccess propertyAccess = BuiltInPropertyAccessStrategies.BASIC.getStrategy().buildPropertyAccess(
						clazz,
						namedParam
				);
				final Getter getter = propertyAccess.getGetter();
				final Class retType = getter.getReturnType();
				final Object object = getter.get( bean );
				if ( Collection.class.isAssignableFrom( retType ) ) {
					setParameterList( namedParam, (Collection) object );
				}
				else if ( retType.isArray() ) {
					setParameterList( namedParam, (Object[]) object );
				}
				else {
					Type type = determineType( namedParam, retType );
					setParameter( namedParam, object, type );
				}
			}
			catch (PropertyNotFoundException pnfe) {
				// ignore
			}
		}
		return this;
	}

	protected Type determineType(String namedParam, Class retType) {
		Type type = queryParameterBindings.getBinding( namedParam ).getBindType();
		if ( type == null ) {
			type = parameterMetadata.getQueryParameter( namedParam ).getType();
		}
		if ( type == null ) {
			type = StandardBasicTypes.SERIALIZABLE;
		}
		return type;
	}


	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setProperties(Map map) {
		String[] namedParameterNames = getNamedParameters();
		for ( String paramName : namedParameterNames ) {
			final Object object = map.get( paramName );
			if ( object == null ) {
				setParameter( paramName, null, determineType( paramName, null ) );
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

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setResultTransformer(ResultTransformer transformer) {
		this.resultTransformer = transformer;
		return this;
	}

	@Override
	public int getMaxResults() {
		return selection.getMaxRows();
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setMaxResults(int maxResult) {
		if ( maxResult <= 0 ) {
			// treat zero and negatives specially as meaning no limit...
			selection.setMaxRows( null );
		}
		else {
			selection.setMaxRows( maxResult );
		}
		return this;
	}

	@Override
	public int getFirstResult() {
		return selection.getFirstRow();
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setFirstResult(int startPosition) {
		selection.setFirstRow( startPosition );
		return this;
	}

	@SuppressWarnings( {"UnusedDeclaration"})
	public Set<String> getSupportedHints() {
		return QueryHints.getDefinedHints();
	}

	@Override
	public Map<String, Object> getHints() {
		getProducer().checkOpen( false ); // technically should rollback
		return hints;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setHint(String hintName, Object value) {
		getProducer().checkOpen( true );
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
				final CacheRetrieveMode retrieveMode = value != null ? CacheRetrieveMode.valueOf( value.toString() ) : null;
				final CacheStoreMode storeMode = getHint( AvailableSettings.SHARED_CACHE_STORE_MODE, CacheStoreMode.class );
				applied = applyCacheModeHint( CacheModeHelper.interpretCacheMode( storeMode, retrieveMode ) );
			}
			else if ( AvailableSettings.SHARED_CACHE_STORE_MODE.equals( hintName ) ) {
				final CacheStoreMode storeMode = value != null ? CacheStoreMode.valueOf( value.toString() ) : null;
				final CacheRetrieveMode retrieveMode = getHint( AvailableSettings.SHARED_CACHE_RETRIEVE_MODE, CacheRetrieveMode.class );
				applied = applyCacheModeHint( CacheModeHelper.interpretCacheMode( storeMode, retrieveMode ) );
			}
			else if ( QueryHints.HINT_NATIVE_LOCKMODE.equals( hintName ) ) {
				if ( !isNativeQuery() ) {
					throw new IllegalStateException(
							"Illegal attempt to set lock mode on non-native query via hint; use Query#setLockMode instead"
					);
				}
				if ( LockMode.class.isInstance( value ) ) {
					applyHibernateLockModeHint( (LockMode) value );
				}
				else if ( LockModeType.class.isInstance( value ) ) {
					applyLockModeTypeHint( (LockModeType) value );
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
						log.unableToDetermineLockModeValue( hintName, value );
						applied = false;
					}
				}
				else {
					applied = false;
				}
			}
			else if ( HINT_FETCHGRAPH.equals( hintName ) || HINT_LOADGRAPH.equals( hintName ) ) {
				if (value instanceof EntityGraphImpl ) {
					applyEntityGraphQueryHint( new EntityGraphQueryHint( (EntityGraphImpl) value ) );
				}
				else {
					log.warnf( "The %s hint was set, but the value was not an EntityGraph!", hintName );
				}
				applied = true;
			}
			else {
				log.ignoringUnrecognizedQueryHint( hintName );
			}
		}
		catch ( ClassCastException e ) {
			throw new IllegalArgumentException( "Value for hint" );
		}

		if ( applied ) {
			if ( hints == null ) {
				hints = new HashMap<>();
			}
			hints.put( hintName, value );
		}
		else {
			log.debugf( "Skipping unsupported query hint [%s]", hintName );
		}

		return this;
	}

	private <T extends Enum<T>> T getHint(String key, Class<T> hintClass) {
		Object hint = hints != null ? hints.get( key ) : null;

// todo : we need this
//		if ( hint == null ) {
//			hint = getProducer().getProperties().get( key );
//		}

		return hint != null ? Enum.valueOf( hintClass, hint.toString() ) : null;
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
		setFlushMode( flushMode );
		return true;
	}

	/**
	 * Can alias-specific lock modes be applied?
	 *
	 * @return {@code true} indicates they can be applied, {@code false} otherwise.
	 */
	protected boolean canApplyAliasSpecificLockModeHints() {
		// only procedure/function calls cannot i believe
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
	 * Apply the alias specific lock modes.  Assumes {@link #canApplyAliasSpecificLockModeHints()} has already been
	 * called and returned {@code true}.
	 *
	 * @param alias The alias to apply the 'lockMode' to.
	 * @param lockMode The LockMode to apply.
	 */
	protected void applyAliasSpecificLockModeHint(String alias, LockMode lockMode) {
		getLockOptions().setAliasSpecificLockMode( alias, lockMode );
	}

	/**
	 * Used from HEM code as a (hopefully temporary) means to apply a custom query plan
	 * in regards to a JPA entity graph.
	 *
	 * @param hint The entity graph hint object
	 */
	public void applyEntityGraphQueryHint(EntityGraphQueryHint hint) {
		queryParameterBindings.verifyParametersBound( false );

		// todo : ideally we'd update the instance state related to queryString but that is final atm

		final String expandedQuery = queryParameterBindings.expandListValuedParameters( getQueryString(), getProducer() );
		this.entityGraphHintedQueryPlan = new HQLQueryPlan(
				expandedQuery,
				false,
				getProducer().getLoadQueryInfluencers().getEnabledFilters(),
				getProducer().getFactory(),
				hint
		);
	}

	/**
	 * Is the query represented here a native (SQL) query?
	 *
	 * @return {@code true} if it is a native query; {@code false} otherwise
	 */
	protected abstract boolean isNativeQuery();

	@Override
	public LockModeType getLockMode() {
		return LockModeTypeHelper.getLockModeType( lockOptions.getLockMode() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> cls) {
		if ( cls.isInstance( getProducer() ) ) {
			return (T) getProducer();
		}
		if ( cls.isInstance( getParameterMetadata() ) ) {
			return (T) getParameterMetadata();
		}
		if ( cls.isInstance( queryParameterBindings ) ) {
			return (T) queryParameterBindings;
		}
		if ( cls.isInstance( this ) ) {
			return (T) this;
		}

		throw new IllegalArgumentException( "Could not unwrap this [" + toString() + "] as requested Java type [" + cls.getName() + "]" );
	}

	public QueryParameters getQueryParameters() {
		QueryParameters queryParameters = new QueryParameters(
				getPositionalParameterTypes(),
				getPositionalParameterValues(),
				getNamedParameterMap(),
				getLockOptions(),
				selection,
				true,
				isReadOnly(),
				cacheable,
				cacheRegion,
				comment,
				dbHints,
				null,
				optionalObject,
				optionalEntityName,
				optionalId,
				resultTransformer
		);
		queryParameters.setQueryPlan( entityGraphHintedQueryPlan );
		return queryParameters;
	}

	@SuppressWarnings("deprecation")
	protected Type[] getPositionalParameterTypes() {
		return queryParameterBindings.collectPositionalBindTypes();
	}

	@SuppressWarnings("deprecation")
	protected Object[] getPositionalParameterValues() {
		return queryParameterBindings.collectPositionalBindValues();
	}

	@SuppressWarnings("deprecation")
	protected Map<String, TypedValue> getNamedParameterMap() {
		return queryParameterBindings.collectNamedParameterBindings();
	}

	private FlushMode sessionFlushMode;
	private CacheMode sessionCacheMode;

	protected void beforeQuery() {
		queryParameterBindings.verifyParametersBound( isCallable() );

		assert sessionFlushMode == null;
		assert sessionCacheMode == null;

		if ( flushMode != null ) {
			sessionFlushMode = getProducer().getHibernateFlushMode();
			getProducer().setHibernateFlushMode( flushMode );
		}
		if ( cacheMode != null ) {
			sessionCacheMode = getProducer().getCacheMode();
			getProducer().setCacheMode( cacheMode );
		}
	}

	protected void afterQuery() {
		if ( sessionFlushMode != null ) {
			getProducer().setHibernateFlushMode( sessionFlushMode );
			sessionFlushMode = null;
		}
		if ( sessionCacheMode != null ) {
			getProducer().setCacheMode( sessionCacheMode );
			sessionCacheMode = null;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator<R> iterate() {
		beforeQuery();
		try {
			return getProducer().iterate(
					queryParameterBindings.expandListValuedParameters( getQueryString(), getProducer() ),
					getQueryParameters()
			);
		}
		finally {
			afterQuery();
		}
	}

	@Override
	public ScrollableResults scroll() {
		return scroll( getProducer().getJdbcServices().getJdbcEnvironment().getDialect().defaultScrollMode() );
	}

	@Override
	public ScrollableResults scroll(ScrollMode scrollMode) {
		beforeQuery();
		try {
			QueryParameters queryParameters = getQueryParameters();
			queryParameters.setScrollMode( scrollMode );
			return getProducer().scroll(
					queryParameterBindings.expandListValuedParameters( getQueryString(), getProducer() ),
					queryParameters
			);
		}
		finally {
			afterQuery();
		}
	}

	@Override
	public List<R> list() {
		beforeQuery();
		try {
			return doList();
		}
		catch (QueryExecutionRequestException he) {
			throw new IllegalStateException( he );
		}
		catch (TypeMismatchException e) {
			throw new IllegalArgumentException( e );
		}
		catch (HibernateException he) {
			throw getProducer().convert( he );
		}
		finally {
			afterQuery();
		}
	}

	protected boolean isCallable() {
		return false;
	}

	@SuppressWarnings("unchecked")
	protected List<R> doList() {
		return getProducer().list(
				queryParameterBindings.expandListValuedParameters( getQueryString(), getProducer() ),
				getQueryParameters()
		);
	}

	public QueryParameterBindingsImpl getQueryParameterBindings() {
		return queryParameterBindings;
	}

	@Override
	public R uniqueResult() {
		return uniqueElement( list() );
	}

	public static <R> R uniqueElement(List<R> list) throws NonUniqueResultException {
		int size = list.size();
		if ( size == 0 ) {
			return null;
		}
		R first = list.get( 0 );
		for ( int i = 1; i < size; i++ ) {
			if ( list.get( i ) != first ) {
				throw new NonUniqueResultException( list.size() );
			}
		}
		return first;
	}

	@Override
	public int executeUpdate() throws HibernateException {
		beforeQuery();
		try {
			return doExecuteUpdate();
		}
		finally {
			afterQuery();
		}
	}

	protected int doExecuteUpdate() {
		return getProducer().executeUpdate(
				queryParameterBindings.expandListValuedParameters( getQueryString(), getProducer() ),
				getQueryParameters()
		);
	}

	protected String resolveEntityName(Object val) {
		if ( val == null ) {
			throw new IllegalArgumentException( "entity for parameter binding cannot be null" );
		}
		return getProducer().bestGuessEntityName( val );
	}

	@Override
	public void setOptionalEntityName(String optionalEntityName) {
		this.optionalEntityName = optionalEntityName;
	}

	@Override
	public void setOptionalId(Serializable optionalId) {
		this.optionalId = optionalId;
	}

	@Override
	public void setOptionalObject(Object optionalObject) {
		this.optionalObject = optionalObject;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Type determineProperBooleanType(String name, Object value, Type defaultType) {
		final QueryParameterBinding binding = getQueryParameterBindings().getBinding( name );
		return binding.getBindType() != null
				? binding.getBindType()
				: defaultType;
	}

	@Override
	public Type determineProperBooleanType(int position, Object value, Type defaultType) {
		final QueryParameterBinding binding = getQueryParameterBindings().getBinding( position );
		return binding.getBindType() != null
				? binding.getBindType()
				: defaultType;
	}
}
