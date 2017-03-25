/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NonUniqueResultException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.QueryParameterException;
import org.hibernate.ScrollMode;
import org.hibernate.TypeMismatchException;
import org.hibernate.engine.query.spi.EntityGraphQueryHint;
import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.engine.spi.ExceptionConverter;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.hql.internal.QueryExecutionRequestException;
import org.hibernate.internal.EmptyScrollableResults;
import org.hibernate.internal.EntityManagerMessageLogger;
import org.hibernate.internal.HEMLogging;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.EmptyIterator;
import org.hibernate.jpa.QueryHints;
import org.hibernate.jpa.TypedParameterValue;
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
import org.hibernate.query.spi.QueryParameterListBinding;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.Type;

import static org.hibernate.LockOptions.WAIT_FOREVER;
import static org.hibernate.cfg.AvailableSettings.JPA_LOCK_SCOPE;
import static org.hibernate.cfg.AvailableSettings.JPA_LOCK_TIMEOUT;
import static org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_RETRIEVE_MODE;
import static org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_STORE_MODE;
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
public abstract class AbstractProducedQuery<R> implements QueryImplementor<R> {
	private static final EntityManagerMessageLogger log = HEMLogging.messageLogger( AbstractProducedQuery.class );

	private final SharedSessionContractImplementor producer;
	private final ParameterMetadata parameterMetadata;
	private final QueryParameterBindingsImpl queryParameterBindings;

	private FlushMode flushMode;
	private CacheStoreMode cacheStoreMode;
	private CacheRetrieveMode cacheRetrieveMode;
	private boolean cacheable;
	private String cacheRegion;
	private Boolean readOnly;

	private LockOptions lockOptions = new LockOptions();

	private String comment;
	private final List<String> dbHints = new ArrayList<>();

	private ResultTransformer resultTransformer;
	private RowSelection queryOptions = new RowSelection();

	private EntityGraphQueryHint entityGraphQueryHint;

	private Object optionalObject;
	private Serializable optionalId;
	private String optionalEntityName;

	private Boolean passDistinctThrough;

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
		return ( flushMode == null ?
				getProducer().getFlushMode() :
				FlushModeTypeHelper.getFlushModeType( flushMode )
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setFlushMode(FlushModeType flushModeType) {
		setHibernateFlushMode( FlushModeTypeHelper.getFlushMode( flushModeType ) );
		return this;
	}

	@Override
	public CacheMode getCacheMode() {
		return CacheModeHelper.interpretCacheMode( cacheStoreMode, cacheRetrieveMode );
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setCacheMode(CacheMode cacheMode) {
		this.cacheStoreMode = CacheModeHelper.interpretCacheStoreMode( cacheMode );
		this.cacheRetrieveMode = CacheModeHelper.interpretCacheRetrieveMode( cacheMode );
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
		return queryOptions.getTimeout();
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setTimeout(int timeout) {
		queryOptions.setTimeout( timeout );
		return this;
	}

	@Override
	public Integer getFetchSize() {
		return queryOptions.getFetchSize();
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setFetchSize(int fetchSize) {
		queryOptions.setFetchSize( fetchSize );
		return this;
	}

	@Override
	public boolean isReadOnly() {
		return ( readOnly == null ?
				producer.getPersistenceContext().isDefaultReadOnly() :
				readOnly
		);
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
		this.lockOptions.setFollowOnLocking( lockOptions.getFollowOnLocking() );
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
		if ( !LockModeType.NONE.equals( lockModeType ) ) {
			if ( !isSelect() ) {
				throw new IllegalStateException( "Illegal attempt to set lock mode on a non-SELECT query" );
			}
		}
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
	public <P> QueryImplementor setParameter(QueryParameter<P> parameter, P value) {
		locateBinding( parameter ).setBindValue( value );
		return this;
	}

	@SuppressWarnings("unchecked")
	private <P> QueryParameterBinding<P> locateBinding(Parameter<P> parameter) {
		if ( parameter instanceof QueryParameter ) {
			return queryParameterBindings.getBinding( (QueryParameter) parameter );
		}
		else if ( parameter.getName() != null ) {
			return queryParameterBindings.getBinding( parameter.getName() );
		}
		else if ( parameter.getPosition() != null ) {
			return queryParameterBindings.getBinding( parameter.getPosition() );
		}

		throw getExceptionConverter().convert(
				new IllegalArgumentException( "Could not resolve binding for given parameter reference [" + parameter + "]" )
		);
	}

	private  <P> QueryParameterBinding<P> locateBinding(QueryParameter<P> parameter) {
		return queryParameterBindings.getBinding( parameter );
	}

	private <P> QueryParameterBinding<P> locateBinding(String name) {
		return queryParameterBindings.getBinding( name );
	}

	private <P> QueryParameterBinding<P> locateBinding(int position) {
		return queryParameterBindings.getBinding( position );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <P> QueryImplementor setParameter(Parameter<P> parameter, P value) {
		if ( value instanceof TypedParameterValue ) {
			setParameter( parameter, ( (TypedParameterValue) value ).getValue(), ( (TypedParameterValue) value ).getType() );
		}
		else if ( value instanceof Collection ) {
			locateListBinding( parameter ).setBindValues( (Collection) value );
		}
		else {
			locateBinding( parameter ).setBindValue( value );
		}

		return this;
	}

	@SuppressWarnings("unchecked")
	private <P> void setParameter(Parameter<P> parameter, Object value, Type type) {
		if ( parameter instanceof QueryParameter ) {
			setParameter( (QueryParameter) parameter, value, type );
		}
		else if ( value == null ) {
			locateBinding( parameter ).setBindValue( null, type );
		}
		else if ( value instanceof Collection ) {
			locateListBinding( parameter ).setBindValues( (Collection) value, type );
		}
		else {
			locateBinding( parameter ).setBindValue( (P) value, type );
		}
	}

	private QueryParameterListBinding locateListBinding(Parameter parameter) {
		if ( parameter instanceof QueryParameter ) {
			return queryParameterBindings.getQueryParameterListBinding( (QueryParameter) parameter );
		}
		else {
			return queryParameterBindings.getQueryParameterListBinding( parameter.getName() );
		}
	}

	private QueryParameterListBinding locateListBinding(String name) {
		return queryParameterBindings.getQueryParameterListBinding( name );
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(String name, Object value) {
		if ( value instanceof TypedParameterValue ) {
			final TypedParameterValue  typedValueWrapper = (TypedParameterValue) value;
			setParameter( name, typedValueWrapper.getValue(), typedValueWrapper.getType() );
		}
		else if ( value instanceof Collection ) {
			setParameterList( name, (Collection) value );
		}
		else {
			queryParameterBindings.getBinding( name ).setBindValue( value );
		}

		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(int position, Object value) {
		if ( value instanceof TypedParameterValue ) {
			final TypedParameterValue typedParameterValue = (TypedParameterValue) value;
			setParameter( position, typedParameterValue.getValue(), typedParameterValue.getType() );
		}
		if ( value instanceof Collection ) {
			setParameterList( Integer.toString( position ), (Collection) value );
		}
		else {
			queryParameterBindings.getBinding( position ).setBindValue( value );
		}
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <P> QueryImplementor setParameter(QueryParameter<P> parameter, P value, Type type) {
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
	public <P> QueryImplementor setParameter(QueryParameter<P> parameter, P value, TemporalType temporalType) {
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
	public <P> QueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<P> values) {
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
	public QueryImplementor setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		queryParameterBindings.getBinding( (QueryParameter) param ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
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
		return getParameterMetadata().collectAllParametersJpa();
	}

	@Override
	public Parameter<?> getParameter(String name) {
		try {
			return getParameterMetadata().getQueryParameter( name );
		}
		catch ( HibernateException e ) {
			throw getExceptionConverter().convert( e );
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
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	public Parameter<?> getParameter(int position) {
		// It is important to understand that there are 2 completely distinct conceptualization of
		// "positional parameters" in play here:
		//		1) The legacy Hibernate concept is akin to JDBC PreparedStatement parameters.  Very limited and
		//			deprecated since 5.x.  These are numbered starting from 0 and kept in the
		//			ParameterMetadata positional-parameter array keyed by this zero-based position
		//		2) JPA's definition is really just a named parameter, but expected to explicitly be
		//			sequential integers starting from 1 (ONE); they can repeat.
		//
		// It is considered illegal to mix positional-parameter with named parameters of any kind.  So therefore.
		// if ParameterMetadata reports that it has any positional-parameters it is talking about the
		// legacy Hibernate concept.
		// lookup jpa-based positional parameters first by name.
		try {
			if ( getParameterMetadata().getPositionalParameterCount() == 0 ) {
				try {
					return getParameterMetadata().getQueryParameter( Integer.toString( position ) );
				}
				catch (HibernateException e) {
					throw new QueryParameterException( "could not locate parameter at position [" + position + "]" );
				}
			}
			// fallback to ordinal lookup
			return getParameterMetadata().getQueryParameter( position );
		}
		catch (HibernateException e) {
			throw getExceptionConverter().convert( e );
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
			throw getExceptionConverter().convert( e );
		}
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
			type = getParameterMetadata().getQueryParameter( namedParam ).getType();
		}
		if ( type == null ) {
			type = getProducer().getFactory().resolveParameterBindType( retType );
		}
		return type;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setProperties(Map map) {
		final String[] namedParameterNames = getNamedParameters();
		for ( String paramName : namedParameterNames ) {
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

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setResultTransformer(ResultTransformer transformer) {
		this.resultTransformer = transformer;
		return this;
	}

	@Override
	public RowSelection getQueryOptions() {
		return queryOptions;
	}

	@Override
	public int getMaxResults() {
		// to be JPA compliant this method returns an int - specifically the "magic number" Integer.MAX_VALUE defined by the spec.
		// For access to the Integer (for checking), use #getQueryOptions#getMaxRows instead
		return queryOptions.getMaxRows() == null ? Integer.MAX_VALUE : queryOptions.getMaxRows();
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setMaxResults(int maxResult) {
		if ( maxResult < 0 ) {
			// treat zero and negatives specially as meaning no limit...
			queryOptions.setMaxRows( null );
		}
		else {
			queryOptions.setMaxRows( maxResult );
		}
		return this;
	}

	@Override
	public int getFirstResult() {
		// to be JPA compliant this method returns an int - specifically the "magic number" 0 (ZERO) defined by the spec.
		// For access to the Integer (for checking), use #getQueryOptions#getFirstRow instead
		return queryOptions.getFirstRow() == null ? 0 : queryOptions.getFirstRow();
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setFirstResult(int startPosition) {
		queryOptions.setFirstRow( startPosition );
		return this;
	}

	@SuppressWarnings( {"UnusedDeclaration"})
	public Set<String> getSupportedHints() {
		return QueryHints.getDefinedHints();
	}

	@Override
	public Map<String, Object> getHints() {
		// Technically this should rollback, but that's insane :)
		// If the TCK ever adds a check for this, we may need to change this behavior
		getProducer().checkOpen( false );

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

		if ( getLockOptions().hasAliasSpecificLockModes() && canApplyAliasSpecificLockModeHints() ) {
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

		if ( cacheStoreMode != null || cacheRetrieveMode != null ) {
			putIfNotNull( hints, HINT_CACHE_MODE, CacheModeHelper.interpretCacheMode( cacheStoreMode, cacheRetrieveMode ) );
			putIfNotNull( hints, JPA_SHARED_CACHE_RETRIEVE_MODE, cacheRetrieveMode );
			putIfNotNull( hints, JPA_SHARED_CACHE_STORE_MODE, cacheStoreMode );
		}

		if ( isCacheable() ) {
			hints.put( HINT_CACHEABLE, true );
			putIfNotNull( hints, HINT_CACHE_REGION, getCacheRegion() );
		}

		if ( isReadOnly() ) {
			hints.put( HINT_READONLY, true );
		}

		if ( entityGraphQueryHint != null ) {
			hints.put( entityGraphQueryHint.getHintName(), entityGraphQueryHint.getOriginEntityGraph() );
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
				if ( canApplyAliasSpecificLockModeHints() ) {
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
				if (value instanceof EntityGraphImpl ) {
					applyEntityGraphQueryHint( new EntityGraphQueryHint( hintName, (EntityGraphImpl) value ) );
				}
				else {
					log.warnf( "The %s hint was set, but the value was not an EntityGraph!", hintName );
				}
				applied = true;
			}
			else if ( HINT_FOLLOW_ON_LOCKING.equals( hintName ) ) {
				applied = applyFollowOnLockingHint( ConfigurationHelper.getBoolean( value ) );
			}
			else if ( QueryHints.HINT_PASS_DISTINCT_THROUGH.equals( hintName ) ) {
				applied = applyPassDistinctThrough( ConfigurationHelper.getBoolean( value ) );
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

	protected boolean applyJpaCacheRetrieveMode(CacheRetrieveMode mode) {
		this.cacheRetrieveMode = mode;
		return true;
	}

	protected boolean applyJpaCacheStoreMode(CacheStoreMode storeMode) {
		this.cacheStoreMode = storeMode;
		return true;
	}

	protected boolean applyNativeQueryLockMode(Object value) {
		if ( !isNativeQuery() ) {
			throw new IllegalStateException(
					"Illegal attempt to set lock mode on non-native query via hint; use Query#setLockMode instead"
			);
		}

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
	protected void applyEntityGraphQueryHint(EntityGraphQueryHint hint) {
		this.entityGraphQueryHint = hint;
	}

	/**
	 * Apply the follow-on-locking hint.
	 *
	 * @param followOnLocking The follow-on-locking strategy.
	 */
	protected boolean applyFollowOnLockingHint(Boolean followOnLocking) {
		getLockOptions().setFollowOnLocking( followOnLocking );
		return true;
	}

	/**
	 * Apply the follow-on-locking hint.
	 *
	 * @param passDistinctThrough the query passes {@code distinct} to the database
	 */
	protected boolean applyPassDistinctThrough(boolean passDistinctThrough) {
		this.passDistinctThrough = passDistinctThrough;
		return true;
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

		throw new HibernateException( "Could not unwrap this [" + toString() + "] as requested Java type [" + cls.getName() + "]" );
//		throw new IllegalArgumentException( "Could not unwrap this [" + toString() + "] as requested Java type [" + cls.getName() + "]" );
	}

	public QueryParameters getQueryParameters() {
		final HQLQueryPlan entityGraphHintedQueryPlan;
		if ( entityGraphQueryHint == null) {
			entityGraphHintedQueryPlan = null;
		}
		else {
			queryParameterBindings.verifyParametersBound( false );

			// todo : ideally we'd update the instance state related to queryString but that is final atm

			final String expandedQuery = queryParameterBindings.expandListValuedParameters( getQueryString(), getProducer() );
			entityGraphHintedQueryPlan = new HQLQueryPlan(
					expandedQuery,
					false,
					getProducer().getLoadQueryInfluencers().getEnabledFilters(),
					getProducer().getFactory(),
					entityGraphQueryHint
			);
		}

		QueryParameters queryParameters = new QueryParameters(
				getPositionalParameterTypes(),
				getPositionalParameterValues(),
				getNamedParameterMap(),
				getLockOptions(),
				queryOptions,
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
		if ( passDistinctThrough != null ) {
			queryParameters.setPassDistinctThrough( passDistinctThrough );
		}
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
		final CacheMode effectiveCacheMode = CacheModeHelper.effectiveCacheMode( cacheStoreMode, cacheRetrieveMode );
		if ( effectiveCacheMode != null ) {
			sessionCacheMode = getProducer().getCacheMode();
			getProducer().setCacheMode( effectiveCacheMode );
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
	public Iterator<R> iterate() {
		beforeQuery();
		try {
			return doIterate();
		}
		finally {
			afterQuery();
		}
	}

	@SuppressWarnings("unchecked")
	protected Iterator<R> doIterate() {
		if (getMaxResults() == 0){
			return EmptyIterator.INSTANCE;
		}
		return getProducer().iterate(
				queryParameterBindings.expandListValuedParameters( getQueryString(), getProducer() ),
				getQueryParameters()
		);
	}

	@Override
	public ScrollableResultsImplementor scroll() {
		return scroll( getProducer().getJdbcServices().getJdbcEnvironment().getDialect().defaultScrollMode() );
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

	protected ScrollableResultsImplementor doScroll(ScrollMode scrollMode) {
		if (getMaxResults() == 0){
			return EmptyScrollableResults.INSTANCE;
		}
		final String query = queryParameterBindings.expandListValuedParameters( getQueryString(), getProducer() );
		QueryParameters queryParameters = getQueryParameters();
		queryParameters.setScrollMode( scrollMode );
		return getProducer().scroll( query, queryParameters );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Stream<R> stream() {
		if (getMaxResults() == 0){
			final Spliterator<R> spliterator = Spliterators.emptySpliterator();
			return StreamSupport.stream( spliterator, false );
		}
		final ScrollableResultsImplementor scrollableResults = scroll( ScrollMode.FORWARD_ONLY );
		final ScrollableResultsIterator<R> iterator = new ScrollableResultsIterator<>( scrollableResults );
		final Spliterator<R> spliterator = Spliterators.spliteratorUnknownSize( iterator, Spliterator.NONNULL );

		final Stream<R> stream = StreamSupport.stream( spliterator, false );
		stream.onClose( scrollableResults::close );

		return stream;
	}

	@Override
	public Optional<R> uniqueResultOptional() {
		return Optional.ofNullable( uniqueResult() );
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
			throw getExceptionConverter().convert( he );
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
		if ( getMaxResults() == 0 ) {
			return Collections.EMPTY_LIST;
		}
		if ( lockOptions.getLockMode() != null && lockOptions.getLockMode() != LockMode.NONE ) {
			if ( !getProducer().isTransactionInProgress() ) {
				throw new TransactionRequiredException( "no transaction is in progress" );
			}
		}

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
			if ( getProducer().getFactory().getSessionFactoryOptions().isJpaBootstrap() ) {
				throw getExceptionConverter().convert( e );
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
		for ( int i = 1; i < size; i++ ) {
			if ( list.get( i ) != first ) {
				throw new NonUniqueResultException( list.size() );
			}
		}
		return first;
	}

	@Override
	public int executeUpdate() throws HibernateException {
		if ( ! getProducer().isTransactionInProgress() ) {
			throw getProducer().getExceptionConverter().convert(
					new TransactionRequiredException(
							"Executing an update/delete query"
					)
			);
		}
		beforeQuery();
		try {
			return doExecuteUpdate();
		}
		catch ( QueryExecutionRequestException e) {
			throw new IllegalStateException( e );
		}
		catch( TypeMismatchException e ) {
			throw new IllegalArgumentException( e );
		}
		catch ( HibernateException e) {
			if ( getProducer().getFactory().getSessionFactoryOptions().isJpaBootstrap() ) {
				throw getExceptionConverter().convert( e );
			}
			else {
				throw e;
			}
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

	private boolean isSelect() {
		return getProducer().getFactory().getQueryPlanCache()
				.getHQLQueryPlan( getQueryString(), false, Collections.<String, Filter>emptyMap() )
				.isSelect();
	}

	protected ExceptionConverter getExceptionConverter(){
		return producer.getExceptionConverter();
	}
}
