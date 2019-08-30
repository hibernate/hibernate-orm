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

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NonUniqueResultException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.QueryParameterException;
import org.hibernate.ScrollMode;
import org.hibernate.TypeMismatchException;
import org.hibernate.engine.query.spi.EntityGraphQueryHint;
import org.hibernate.engine.spi.ExceptionConverter;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.internal.EntityManagerMessageLogger;
import org.hibernate.internal.HEMLogging;
import org.hibernate.jpa.QueryHints;
import org.hibernate.jpa.internal.util.ConfigurationHelper;
import org.hibernate.jpa.internal.util.FlushModeTypeHelper;
import org.hibernate.jpa.internal.util.LockModeTypeHelper;
import org.hibernate.metamodel.model.domain.AllowableParameterType;
import org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.Query;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.TypedParameterValue;
import org.hibernate.query.spi.MutableQueryOptions;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.transform.ResultTransformer;

import org.jboss.logging.Logger;

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
	private static final EntityManagerMessageLogger MSG_LOGGER = HEMLogging.messageLogger( AbstractProducedQuery.class );
	private static final Logger LOGGER = Logger.getLogger( AbstractProducedQuery.class );

	private final SharedSessionContractImplementor producer;
	private final ParameterMetadata parameterMetadata;

	private ResultTransformer resultTransformer;
	private MutableQueryOptions queryOptions = new QueryOptionsImpl();

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
	}


	@Override
	public MutableQueryOptions getQueryOptions() {
		return queryOptions;
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
		getSession().checkOpen();
		return getHibernateFlushMode() == null
				? getSession().getFlushMode()
				: FlushModeTypeHelper.getFlushModeType( getHibernateFlushMode() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setFlushMode(FlushModeType flushModeType) {
		getSession().checkOpen();
		setHibernateFlushMode( FlushModeTypeHelper.getFlushMode( flushModeType ) );
		return this;
	}

	@Override
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
		return getQueryOptions().isResultCachingEnabled();
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
		getQueryOptions().getLockOptions().setAliasSpecificLockMode( alias, lockMode );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setLockMode(LockModeType lockModeType) {
		getSession().checkOpen();
		if ( !LockModeType.NONE.equals( lockModeType ) ) {
			if ( !isSelect() ) {
				throw new IllegalStateException( "Illegal attempt to set lock mode on a non-SELECT query" );
			}
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

	@Override
	public ParameterMetadata getParameterMetadata() {
		return parameterMetadata;
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
		final QueryParameterBinding binding = getQueryParameterBindings().getBinding(
				getParameterMetadata().getQueryParameter( position )
		);

		binding.setBindValue( value, temporalType );

		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <P> QueryImplementor setParameter(QueryParameter<P> parameter, P value) {
		getQueryParameterBindings().getBinding( parameter ).setBindValue( value );
		return this;
	}

	@SuppressWarnings("unchecked")
	private <P> QueryParameterBinding<P> locateBinding(Parameter<P> parameter) {
		if ( parameter instanceof QueryParameterImplementor ) {
			return getQueryParameterBindings().getBinding( (QueryParameterImplementor) parameter );
		}
		else if ( parameter.getName() != null ) {
			return (QueryParameterBinding) getQueryParameterBindings().getBinding( parameter.getName() );
		}
		else if ( parameter.getPosition() != null ) {
			return (QueryParameterBinding) getQueryParameterBindings().getBinding( parameter.getPosition() );
		}

		throw getExceptionConverter().convert(
				new IllegalArgumentException( "Could not resolve binding for given parameter reference [" + parameter + "]" )
		);
	}

	private <P> QueryParameterBinding<P> locateBinding(String name) {
		//noinspection unchecked
		return (QueryParameterBinding) getQueryParameterBindings().getBinding( name );
	}

	private <P> QueryParameterBinding<P> locateBinding(int position) {
		//noinspection unchecked
		return (QueryParameterBinding) getQueryParameterBindings().getBinding( position );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <P> QueryImplementor setParameter(Parameter<P> parameter, P value) {
		getSession().checkOpen();
		if ( value instanceof TypedParameterValue ) {
			setParameter(
					parameter,
					( (TypedParameterValue) value ).getValue(),
					( (TypedParameterValue) value ).getType()
			);
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
		else {
			locateBinding( parameter ).setBindValue( (P) value, type );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(String name, Object value) {
		getSession().checkOpen();
		if ( value instanceof TypedParameterValue ) {
			final TypedParameterValue  typedValueWrapper = (TypedParameterValue) value;
			setParameter( name, typedValueWrapper.getValue(), typedValueWrapper.getType() );
		}
		else if ( value instanceof Collection && !isRegisteredAsBasicType( value.getClass() ) ) {
			setParameterList( name, (Collection) value );
		}
		else {
			getQueryParameterBindings().getBinding( name ).setBindValue( value );
		}

		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(int position, Object value) {
		getSession().checkOpen();
		if ( value instanceof TypedParameterValue ) {
			final TypedParameterValue typedParameterValue = (TypedParameterValue) value;
			setParameter( position, typedParameterValue.getValue(), typedParameterValue.getType() );
		}
		else if ( value instanceof Collection && !isRegisteredAsBasicType( value.getClass() ) ) {
			setParameterList( position, (Collection) value );
		}
		else {
			getQueryParameterBindings().getBinding( position ).setBindValue( value );
		}
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <P> QueryImplementor setParameter(QueryParameter<P> parameter, P value, AllowableParameterType type) {
		getQueryParameterBindings().getBinding( parameter ).setBindValue( value, type );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(String name, Object value, AllowableParameterType type) {
		getQueryParameterBindings().getBinding( name ).setBindValue( value, type );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(int position, Object value, AllowableParameterType type) {
		getQueryParameterBindings().getBinding( position ).setBindValue( value, type );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <P> QueryImplementor setParameter(QueryParameter<P> parameter, P value, TemporalType temporalType) {
		getQueryParameterBindings().getBinding( parameter ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(String name, Object value, TemporalType temporalType) {
		getQueryParameterBindings().getBinding( name ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(int position, Object value, TemporalType temporalType) {
		getQueryParameterBindings().getBinding( position ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public <P> QueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<P> values) {
		getQueryParameterBindings().getBinding( parameter ).setBindValues( values );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameterList(String name, Collection values) {
		getQueryParameterBindings().getBinding( name ).setBindValues( values );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameterList(int position, Collection values) {
		getQueryParameterBindings().getBinding( position ).setBindValues( values );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameterList(String name, Collection values, AllowableParameterType type) {
		getQueryParameterBindings().getBinding( name ).setBindValues( values, type );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameterList(int position, Collection values, AllowableParameterType type) {
		getQueryParameterBindings().getBinding( position ).setBindValues( values, type );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameterList(String name, Object[] values, AllowableParameterType type) {
		getQueryParameterBindings().getBinding( name ).setBindValues( Arrays.asList( values ), type );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameterList(int position, Object[] values, AllowableParameterType type) {
		getQueryParameterBindings().getBinding( position ).setBindValues( Arrays.asList( values ), type );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameterList(String name, Object[] values) {
		getQueryParameterBindings().getBinding( name ).setBindValues( Arrays.asList( values ) );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameterList(int position, Object[] values) {
		getQueryParameterBindings().getBinding( position ).setBindValues( Arrays.asList( values ) );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		getSession().checkOpen();
		getQueryParameterBindings().getBinding( (QueryParameter) param ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		getSession().checkOpen();
		getQueryParameterBindings().getBinding( (QueryParameter) param ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(String name, Calendar value, TemporalType temporalType) {
		getSession().checkOpen();
		getQueryParameterBindings().getBinding( name ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(String name, Date value, TemporalType temporalType) {
		getSession().checkOpen();
		getQueryParameterBindings().getBinding( name ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(int position, Calendar value, TemporalType temporalType) {
		getSession().checkOpen();
		getQueryParameterBindings().getBinding( position ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setParameter(int position, Date value, TemporalType temporalType) {
		getSession().checkOpen();
		getQueryParameterBindings().getBinding( position ).setBindValue( value, temporalType );
		return this;
	}

	@Override
	public Set<Parameter<?>> getParameters() {
		getSession().checkOpen( false );
		//noinspection unchecked
		return (Set) getParameterMetadata().getRegistrations();
	}

	@Override
	public QueryParameter<?> getParameter(String name) {
		getSession().checkOpen( false );
		try {
			return getParameterMetadata().getQueryParameter( name );
		}
		catch ( HibernateException e ) {
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> QueryParameter<T> getParameter(String name, Class<T> type) {
		getSession().checkOpen( false );
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
	public QueryParameter<?> getParameter(int position) {
		getSession().checkOpen( false );
		try {
			return parameterMetadata.getQueryParameter( position );
		}
		catch (HibernateException e) {
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
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
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	public boolean isBound(Parameter<?> parameter) {
		getSession().checkOpen();
		return getQueryParameterBindings().getBinding( (QueryParameterImplementor) parameter ).isBound();
	}

	@Override
	public <T> T getParameterValue(Parameter<T> parameter) {
		LOGGER.tracef( "#getParameterValue(%s)", parameter );

		getSession().checkOpen( false );

		if ( !getParameterMetadata().containsReference( (QueryParameter) parameter ) ) {
			throw new IllegalArgumentException( "Parameter reference [" + parameter + "] did not come from this query" );
		}

		final QueryParameterBinding<T> binding = getQueryParameterBindings().getBinding( (QueryParameter<T>) parameter );
		LOGGER.debugf( "Checking whether parameter reference [%s] is bound : %s", parameter, binding.isBound() );
		if ( !binding.isBound() ) {
			throw new IllegalStateException( "Parameter value not yet bound : " + parameter.toString() );
		}
		return binding.getBindValue();
	}

	@Override
	public Object getParameterValue(String name) {
		getSession().checkOpen( false );

		final QueryParameterBinding binding;
		try {
			binding = getQueryParameterBindings().getBinding( name );
		}
		catch (QueryParameterException e) {
			throw new IllegalArgumentException( "Could not resolve parameter by name - " + name, e );
		}

		LOGGER.debugf( "Checking whether named parameter [%s] is bound : %s", name, binding.isBound() );
		if ( !binding.isBound() ) {
			throw new IllegalStateException( "Parameter value not yet bound : " + name );
		}
		return binding.getBindValue();
	}

	@Override
	public Object getParameterValue(int position) {
		getSession().checkOpen( false );

		final QueryParameterBinding binding;
		try {
			binding = getQueryParameterBindings().getBinding( position );
		}
		catch (QueryParameterException e) {
			throw new IllegalArgumentException( "Could not resolve parameter by position - " + position, e );
		}

		LOGGER.debugf( "Checking whether positional  parameter [%s] is bound : %s", (Integer) position, (Boolean) binding.isBound() );
		if ( !binding.isBound() ) {
			throw new IllegalStateException( "Parameter value not yet bound : " + position );
		}
		return binding.getBindValue();
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setProperties(Object bean) {
		final Class clazz = bean.getClass();
		parameterMetadata.visitRegistrations(
				queryParameter -> {
					final String parameterName = queryParameter.getName();
					if ( parameterName != null ) {
						try {
							final PropertyAccess propertyAccess = BuiltInPropertyAccessStrategies.BASIC.getStrategy().buildPropertyAccess(
									clazz,
									parameterName
							);
							final Getter getter = propertyAccess.getGetter();
							final Class retType = getter.getReturnType();
							final Object object = getter.get( bean );
							if ( Collection.class.isAssignableFrom( retType ) ) {
								setParameterList( parameterName, (Collection) object );
							}
							else if ( retType.isArray() ) {
								setParameterList( parameterName, (Object[]) object );
							}
							else {
								setParameter( parameterName, object, determineType( parameterName, retType ) );
							}
						}
						catch (PropertyNotFoundException e) {
							// ignore
						}
					}
				}
		);

		return this;
	}

	protected AllowableParameterType determineType(String namedParam, Class retType) {
		AllowableParameterType type = getQueryParameterBindings().getBinding( namedParam ).getBindType();
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
		parameterMetadata.visitRegistrations(
				queryParameter -> {
					final String parameterName = queryParameter.getName();
					if ( parameterName != null ) {
						final Object value = map.get( parameterName );

						if ( value == null ) {
							if ( map.containsKey( parameterName ) ) {
								setParameter( parameterName, null, determineType( parameterName, null ) );
							}
						}
						else {
							Class retType = value.getClass();
							if ( Collection.class.isAssignableFrom( retType ) ) {
								setParameterList( parameterName, (Collection) value );
							}
							else if ( retType.isArray() ) {
								setParameterList( parameterName, (Object[]) value );
							}
							else {
								setParameter( parameterName, value, determineType( parameterName, retType ) );
							}
						}
					}
				}
		);

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
		getSession().checkOpen();
		// to be JPA compliant this method returns an int - specifically the "magic number" Integer.MAX_VALUE defined by the spec.
		// For access to the Integer (for checking), use #getQueryOptions#getMaxRows instead
		return queryOptions.getMaxRows() == null ? Integer.MAX_VALUE : queryOptions.getMaxRows();
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setMaxResults(int maxResult) {
		getSession().checkOpen();

		if ( maxResult < 0 ) {
			throw new IllegalArgumentException( "max-results cannot be negative" );
		}
		else {
			queryOptions.getLimit().setMaxRows( maxResult );
		}
		return this;
	}

	@Override
	public int getFirstResult() {
		getSession().checkOpen();
		// to be JPA compliant this method returns an int - specifically the "magic number" 0 (ZERO) defined by the spec.
		// For access to the Integer (for checking), use #getQueryOptions#getFirstRow instead
		return queryOptions.getFirstRow() == null ? 0 : queryOptions.getFirstRow();
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryImplementor setFirstResult(int startPosition) {
		getSession().checkOpen();
		if ( startPosition < 0 ) {
			throw new IllegalArgumentException( "first-result value cannot be negative : " + startPosition );
		}
		queryOptions.getLimit().setFirstRow( startPosition );
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
		final MutableQueryOptions queryOptions = getQueryOptions();
		final Integer queryTimeout = queryOptions.getTimeout();
		if ( queryTimeout != null ) {
			hints.put( HINT_TIMEOUT, queryTimeout );
			hints.put( SPEC_HINT_TIMEOUT, queryTimeout * 1000 );
		}

		final LockOptions lockOptions = getLockOptions();
		final int lockOptionsTimeOut = lockOptions.getTimeOut();
		if ( lockOptionsTimeOut != WAIT_FOREVER ) {
			hints.put( JPA_LOCK_TIMEOUT, lockOptionsTimeOut );
		}

		if ( lockOptions.getScope() ) {
			hints.put( JPA_LOCK_SCOPE, lockOptions.getScope() );
		}

		if ( lockOptions.hasAliasSpecificLockModes() && canApplyAliasSpecificLockModeHints() ) {
			for ( Map.Entry<String, LockMode> entry : lockOptions.getAliasSpecificLocks() ) {
				hints.put(
						ALIAS_SPECIFIC_LOCK_MODE + '.' + entry.getKey(),
						entry.getValue().name()
				);
			}
		}

		putIfNotNull( hints, HINT_COMMENT, getComment() );
		putIfNotNull( hints, HINT_FETCH_SIZE, queryOptions.getFetchSize() );
		putIfNotNull( hints, HINT_FLUSH_MODE, getHibernateFlushMode() );

		final CacheMode cacheMode = getQueryOptions().getCacheMode();
		if ( cacheMode != null ) {
			hints.put( HINT_CACHE_MODE, cacheMode );
			hints.put( JPA_SHARED_CACHE_RETRIEVE_MODE, cacheMode.getJpaRetrieveMode() );
			hints.put( JPA_SHARED_CACHE_STORE_MODE, cacheMode.getJpaStoreMode() );
		}

		if ( isCacheable() ) {
			hints.put( HINT_CACHEABLE, true );
			putIfNotNull( hints, HINT_CACHE_REGION, getCacheRegion() );
		}

		if ( isReadOnly() ) {
			hints.put( HINT_READONLY, true );
		}

		if ( entityGraphQueryHint != null ) {
			hints.put( entityGraphQueryHint.getSemantic().getJpaHintName(), entityGraphQueryHint.getGraph() );
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
				if ( canApplyAliasSpecificLockModeHints() ) {
					// extract the alias
					final String alias = hintName.substring( ALIAS_SPECIFIC_LOCK_MODE.length() + 1 );
					// determine the LockMode
					try {
						final LockMode lockMode = LockModeTypeHelper.interpretLockMode( value );
						applyAliasSpecificLockModeHint( alias, lockMode );
					}
					catch ( Exception e ) {
						MSG_LOGGER.unableToDetermineLockModeValue( hintName, value );
						applied = false;
					}
				}
				else {
					applied = false;
				}
			}
			else if ( HINT_FETCHGRAPH.equals( hintName ) || HINT_LOADGRAPH.equals( hintName ) ) {
				if ( value instanceof RootGraph ) {
					applyGraph( (RootGraph) value, GraphSemantic.fromJpaHintName( hintName ) );
				}
				else {
					MSG_LOGGER.warnf( "The %s hint was set, but the value was not an EntityGraph!", hintName );
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
				MSG_LOGGER.ignoringUnrecognizedQueryHint( hintName );
			}
		}
		catch ( ClassCastException e ) {
			throw new IllegalArgumentException( "Value for hint" );
		}

		if ( !applied ) {
			handleUnrecognizedHint( hintName, value );
		}

		return this;
	}

	protected void handleUnrecognizedHint(String hintName, Object value) {
		MSG_LOGGER.debugf( "Skipping unsupported query hint [%s]", hintName );
	}

	protected boolean applyJpaCacheRetrieveMode(CacheRetrieveMode mode) {
		getQueryOptions().setCacheRetrieveMode( mode );
		return true;
	}

	protected boolean applyJpaCacheStoreMode(CacheStoreMode mode) {
		getQueryOptions().setCacheStoreMode( mode );
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
		setHibernateFlushMode( flushMode );
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

	@Override
	public Query<R> applyGraph(RootGraph graph, GraphSemantic semantic) {
		if ( semantic == null ) {
			this.entityGraphQueryHint = null;
		}
		else {
			if ( graph == null ) {
				throw new IllegalStateException( "Semantic was non-null, but graph was null" );
			}

			applyGraph( (RootGraphImplementor<?>) graph, semantic );
		}

		return this;
	}

	/**
	 * Used from HEM code as a (hopefully temporary) means to apply a custom query plan
	 * in regards to a JPA entity graph.
	 *
	 * @param hint The entity graph hint object
	 *
	 * @deprecated (5.4) Use {@link #applyGraph} instead
	 */
	@Deprecated
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
		getSession().checkOpen( false );
		if ( !isSelect() ) {
			throw new IllegalStateException( "Illegal attempt to get lock mode on a non-SELECT query" );
		}
		return LockModeTypeHelper.getLockModeType( getQueryOptions().getLockOptions().getLockMode() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> cls) {
		if ( cls.isInstance( getSession() ) ) {
			return (T) getSession();
		}
		if ( cls.isInstance( getParameterMetadata() ) ) {
			return (T) getParameterMetadata();
		}
		if ( cls.isInstance( getQueryParameterBindings() ) ) {
			return (T) getQueryParameterBindings();
		}
		if ( cls.isInstance( this ) ) {
			return (T) this;
		}

		throw new HibernateException( "Could not unwrap this [" + toString() + "] as requested Java type [" + cls.getName() + "]" );
//		throw new IllegalArgumentException( "Could not unwrap this [" + toString() + "] as requested Java type [" + cls.getName() + "]" );
	}

	public QueryParameters getQueryParameters() {
		throw new NotYetImplementedFor6Exception( getClass() );
//		final String expandedQuery = getQueryParameterBindings().expandListValuedParameters( getQueryString(), getSession() );
//		return makeQueryParametersForExecution( expandedQuery );
	}

//	protected QueryParameters makeQueryParametersForExecution(String hql) {
//		final HQLQueryPlan entityGraphHintedQueryPlan;
//		if ( entityGraphQueryHint == null) {
//			entityGraphHintedQueryPlan = null;
//		}
//		else {
//			entityGraphHintedQueryPlan = new HQLQueryPlan(
//					hql,
//					false,
//					getSession().getLoadQueryInfluencers().getEnabledFilters(),
//					getSession().getFactory(),
//					entityGraphQueryHint
//			);
//		}
//
//		final QueryParameters queryParameters = new QueryParameters(
//				getQueryParameterBindings(),
//				getLockOptions(),
//				queryOptions,
//				null,
//				optionalObject,
//				optionalEntityName,
//				optionalId,
//				resultTransformer
//		);
//		queryParameters.setQueryPlan( entityGraphHintedQueryPlan );
//		if ( passDistinctThrough != null ) {
//			queryParameters.setPassDistinctThrough( passDistinctThrough );
//		}
//		return queryParameters;
//	}

	private FlushMode sessionFlushMode;
	private CacheMode sessionCacheMode;

	protected void beforeQuery() {
		if ( optionalId == null ) {
			getQueryParameterBindings().validate();
		}

		assert sessionFlushMode == null;
		assert sessionCacheMode == null;

		final FlushMode flushMode = getQueryOptions().getFlushMode();
		if ( flushMode != null ) {
			sessionFlushMode = getSession().getHibernateFlushMode();
			getSession().setHibernateFlushMode( flushMode );
		}

		final CacheMode effectiveCacheMode = CacheMode.fromJpaModes( queryOptions.getCacheRetrieveMode(), queryOptions.getCacheStoreMode() );
		sessionCacheMode = getSession().getCacheMode();
		getSession().setCacheMode( effectiveCacheMode );
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
	public ScrollableResultsImplementor scroll() {
		return scroll( getSession().getJdbcServices().getJdbcEnvironment().getDialect().defaultScrollMode() );
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
		throw new NotYetImplementedFor6Exception( getClass() );

//		if ( getMaxResults() == 0 ) {
//			return EmptyScrollableResults.INSTANCE;
//		}
//
//		final String query = getQueryParameterBindings().expandListValuedParameters( getQueryString(), getSession() );
//		QueryParameters queryParameters = makeQueryParametersForExecution( query );
//		queryParameters.setScrollMode( scrollMode );
//		return getSession().scroll( query, queryParameters );
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
		catch (TypeMismatchException e) {
			throw new IllegalArgumentException( e );
		}
		catch (HibernateException he) {
			throw getExceptionConverter().convert( he, getLockOptions() );
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
		throw new NotYetImplementedFor6Exception( getClass() );

//		if ( getMaxResults() == 0 ) {
//			return Collections.EMPTY_LIST;
//		}
//		if ( lockOptions.getLockMode() != null && lockOptions.getLockMode() != LockMode.NONE ) {
//			if ( !getSession().isTransactionInProgress() ) {
//				throw new TransactionRequiredException( "no transaction is in progress" );
//			}
//		}
//
//		final String expandedQuery = getQueryParameterBindings().expandListValuedParameters( getQueryString(), getSession() );
//		return getSession().list(
//				expandedQuery,
//				makeQueryParametersForExecution( expandedQuery )
//		);
	}

	protected abstract QueryParameterBindings getQueryParameterBindings();

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
			throw getExceptionConverter().convert( e, getLockOptions() );
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
		getSession().checkTransactionNeededForUpdateOperation( "Executing an update/delete query" );

		beforeQuery();
		try {
			return doExecuteUpdate();
		}
		catch( TypeMismatchException e ) {
			throw new IllegalArgumentException( e );
		}
		catch ( HibernateException e) {
			throw getExceptionConverter().convert( e );
		}
		finally {
			afterQuery();
		}
	}

	protected int doExecuteUpdate() {
		throw new NotYetImplementedFor6Exception( getClass() );

//		final String expandedQuery = getQueryParameterBindings().expandListValuedParameters( getQueryString(), getSession() );
//		return getSession().executeUpdate(
//				expandedQuery,
//				makeQueryParametersForExecution( expandedQuery )
//		);
	}

	protected String resolveEntityName(Object val) {
		if ( val == null ) {
			throw new IllegalArgumentException( "entity for parameter binding cannot be null" );
		}
		return getSession().bestGuessEntityName( val );
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

	private boolean isSelect() {
		throw new NotYetImplementedFor6Exception( getClass() );

//		return getSession().getFactory().getQueryInterpretationCache()
//				.getHQLQueryPlan( getQueryString(), false, Collections.<String, Filter>emptyMap() )
//				.isSelect();
	}

	protected ExceptionConverter getExceptionConverter(){
		return producer.getExceptionConverter();
	}

	private boolean isRegisteredAsBasicType(Class cl) {
		return producer.getFactory().getTypeResolver().basic( cl.getName() ) != null;
	}
}
