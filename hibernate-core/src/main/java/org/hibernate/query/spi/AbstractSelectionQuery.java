/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NonUniqueResultException;
import org.hibernate.ScrollMode;
import org.hibernate.UnknownProfileException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.jpa.internal.util.LockModeTypeHelper;
import org.hibernate.query.BindableType;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.internal.ScrollableResultsIterator;
import org.hibernate.query.named.NamedQueryMemento;
import org.hibernate.sql.exec.internal.CallbackImpl;
import org.hibernate.sql.exec.spi.Callback;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;

import static java.util.Spliterators.spliteratorUnknownSize;
import static org.hibernate.CacheMode.fromJpaModes;
import static org.hibernate.FlushMode.fromJpaFlushMode;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_SHARED_CACHE_RETRIEVE_MODE;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_SHARED_CACHE_STORE_MODE;
import static org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_RETRIEVE_MODE;
import static org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_STORE_MODE;
import static org.hibernate.jpa.HibernateHints.HINT_CACHEABLE;
import static org.hibernate.jpa.HibernateHints.HINT_CACHE_MODE;
import static org.hibernate.jpa.HibernateHints.HINT_CACHE_REGION;
import static org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE;
import static org.hibernate.jpa.HibernateHints.HINT_FOLLOW_ON_LOCKING;
import static org.hibernate.jpa.HibernateHints.HINT_READ_ONLY;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSelectionQuery<R>
		extends AbstractCommonQueryContract
		implements SelectionQuery<R>, DomainQueryExecutionContext {
	/**
	 * The value used for {@link #getQueryString} for Criteria-based queries
	 */
	public static final String CRITERIA_HQL_STRING = "<criteria>";

	private Callback callback;

	public AbstractSelectionQuery(SharedSessionContractImplementor session) {
		super( session );
	}

	protected AbstractSelectionQuery(AbstractSelectionQuery<?> original) {
		super( original );
		this.sessionFlushMode = original.sessionFlushMode;
		this.sessionCacheMode = original.sessionCacheMode;
	}

	protected void applyOptions(NamedQueryMemento memento) {
		if ( memento.getHints() != null ) {
			memento.getHints().forEach( this::applyHint );
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

	protected abstract String getQueryString();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// execution

	private FlushMode sessionFlushMode;
	private CacheMode sessionCacheMode;

	@Override
	public List<R> list() {
		final HashSet<String> fetchProfiles = beforeQueryHandlingFetchProfiles();
		boolean success = false;
		try {
			final List<R> result = doList();
			success = true;
			return result;
		}
		catch (IllegalQueryOperationException e) {
			throw new IllegalStateException( e );
		}
		catch (HibernateException he) {
			throw getSession().getExceptionConverter().convert( he, getQueryOptions().getLockOptions() );
		}
		finally {
			afterQueryHandlingFetchProfiles( success, fetchProfiles );
		}
	}

	protected HashSet<String> beforeQueryHandlingFetchProfiles() {
		beforeQuery();
		final MutableQueryOptions options = getQueryOptions();
		return getSession().getLoadQueryInfluencers()
				.adjustFetchProfiles( options.getDisabledFetchProfiles(), options.getEnabledFetchProfiles() );
	}

	protected void beforeQuery() {
		getQueryParameterBindings().validate();

		final SharedSessionContractImplementor session = getSession();
		final MutableQueryOptions options = getQueryOptions();

		session.prepareForQueryExecution( requiresTxn( options.getLockOptions().findGreatestLockMode() ) );
		prepareForExecution();

		assert sessionFlushMode == null;
		assert sessionCacheMode == null;

		final FlushMode effectiveFlushMode = getHibernateFlushMode();
		if ( effectiveFlushMode != null ) {
			sessionFlushMode = session.getHibernateFlushMode();
			session.setHibernateFlushMode( effectiveFlushMode );
		}

		final CacheMode effectiveCacheMode = getCacheMode();
		if ( effectiveCacheMode != null ) {
			sessionCacheMode = session.getCacheMode();
			session.setCacheMode( effectiveCacheMode );
		}
	}

	protected abstract void prepareForExecution();

	protected void afterQueryHandlingFetchProfiles(boolean success, HashSet<String> fetchProfiles) {
		resetFetchProfiles( fetchProfiles );
		afterQuery( success );
	}

	private void afterQueryHandlingFetchProfiles(HashSet<String> fetchProfiles) {
		resetFetchProfiles( fetchProfiles );
		afterQuery();
	}

	private void resetFetchProfiles(HashSet<String> fetchProfiles) {
		getSession().getLoadQueryInfluencers().setEnabledFetchProfileNames( fetchProfiles );
	}

	protected void afterQuery(boolean success) {
		afterQuery();

		final SharedSessionContractImplementor session = getSession();
		if ( !session.isTransactionInProgress() ) {
			session.getJdbcCoordinator().getLogicalConnection().afterTransaction();
		}
		session.afterOperation( success );
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

	protected boolean requiresTxn(LockMode lockMode) {
		return lockMode != null && lockMode.greaterThan( LockMode.READ );
	}

	protected abstract List<R> doList();

	@Override
	public ScrollableResultsImplementor<R> scroll() {
		return scroll( getSessionFactory().getJdbcServices().getDialect().defaultScrollMode() );
	}

	@Override
	public ScrollableResultsImplementor<R> scroll(ScrollMode scrollMode) {
		final HashSet<String> fetchProfiles = beforeQueryHandlingFetchProfiles();
		try {
			return doScroll( scrollMode );
		}
		finally {
			afterQueryHandlingFetchProfiles( fetchProfiles );
		}
	}

	protected abstract ScrollableResultsImplementor<R> doScroll(ScrollMode scrollMode);

	@Override
	public Stream<R> getResultStream() {
		return stream();
	}

	@SuppressWarnings( {"unchecked", "rawtypes"} )
	@Override
	public Stream stream() {
		final ScrollableResultsImplementor scrollableResults = scroll( ScrollMode.FORWARD_ONLY );
		final ScrollableResultsIterator iterator = new ScrollableResultsIterator<>( scrollableResults );
		final Spliterator spliterator = spliteratorUnknownSize( iterator, Spliterator.NONNULL );

		final Stream stream = StreamSupport.stream( spliterator, false );
		return (Stream) stream.onClose( scrollableResults::close );
	}

	@Override
	public R uniqueResult() {
		return uniqueElement( list() );
	}

	@Override
	public R getSingleResult() {
		try {
			final List<R> list = list();
			if ( list.isEmpty() ) {
				throw new NoResultException(
						String.format( "No result found for query [%s]", getQueryString() )
				);
			}
			return uniqueElement( list );
		}
		catch ( HibernateException e ) {
			throw getSession().getExceptionConverter().convert( e, getQueryOptions().getLockOptions() );
		}
	}

	protected static <T> T uniqueElement(List<T> list) throws NonUniqueResultException {
		int size = list.size();
		if ( size == 0 ) {
			return null;
		}
		else {
			final T first = list.get( 0 );
			// todo (6.0) : add a setting here to control whether to perform this validation or not
			for ( int i = 1; i < size; i++ ) {
				if ( list.get( i ) != first ) {
					throw new NonUniqueResultException( list.size() );
				}
			}
			return first;
		}
	}

	@Override
	public Optional<R> uniqueResultOptional() {
		return Optional.ofNullable( uniqueResult() );
	}

	@Override
	public R getSingleResultOrNull() {
		try {
			return uniqueElement( list() );
		}
		catch ( HibernateException e ) {
			throw getSession().getExceptionConverter().convert( e, getLockOptions() );
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// DomainQueryExecutionContext

	@Override
	public Callback getCallback() {
		if ( callback == null ) {
			callback = new CallbackImpl();
		}
		return callback;
	}

	@Override
	public boolean hasCallbackActions() {
		return callback != null && callback.hasAfterLoadActions();
	}

	protected void resetCallback() {
		callback = null;
	}


	@Override
	public FlushModeType getFlushMode() {
		return getQueryOptions().getFlushMode().toJpaFlushMode();
	}

	@Override
	public SelectionQuery<R> setFlushMode(FlushModeType flushMode) {
		getQueryOptions().setFlushMode( fromJpaFlushMode( flushMode ) );
		return this;
	}

	@Override
	public SelectionQuery<R> setMaxResults(int maxResult) {
		super.applyMaxResults( maxResult );
		return this;
	}

	@Override
	public SelectionQuery<R> setFirstResult(int startPosition) {
		getSession().checkOpen();
		if ( startPosition < 0 ) {
			throw new IllegalArgumentException( "first-result value cannot be negative : " + startPosition );
		}
		getQueryOptions().getLimit().setFirstRow( startPosition );
		return this;
	}

	@Override
	public SelectionQuery<R> setHint(String hintName, Object value) {
		super.setHint( hintName, value );
		return this;
	}

	@Override
	public SelectionQuery<R> setEntityGraph(EntityGraph<R> graph, GraphSemantic semantic) {
		applyGraph( (RootGraphImplementor<R>) graph, semantic );
		return this;
	}

	@Override
	public SelectionQuery<R> enableFetchProfile(String profileName) {
		if ( !getSession().getFactory().containsFetchProfileDefinition( profileName ) ) {
			throw new UnknownProfileException( profileName );
		}
		getQueryOptions().enableFetchProfile( profileName );
		return this;
	}

	@Override
	public SelectionQuery<R> disableFetchProfile(String profileName) {
		getQueryOptions().disableFetchProfile( profileName );
		return this;
	}

	@Override
	public LockOptions getLockOptions() {
		return getQueryOptions().getLockOptions();
	}

	@Override
	public LockModeType getLockMode() {
		return LockModeTypeHelper.getLockModeType( getHibernateLockMode() );
	}

	/**
	 * Specify the root LockModeType for the query
	 *
	 * @see #setHibernateLockMode
	 */
	@Override
	public SelectionQuery<R> setLockMode(LockModeType lockMode) {
		setHibernateLockMode( LockModeTypeHelper.getLockMode( lockMode ) );
		return this;
	}

	@Override
	public SelectionQuery<R> setLockMode(String alias, LockMode lockMode) {
		getQueryOptions().getLockOptions().setAliasSpecificLockMode( alias, lockMode );
		return this;
	}

	/**
	 * Get the root LockMode for the query
	 */
	@Override
	public LockMode getHibernateLockMode() {
		return getLockOptions().getLockMode();
	}

	/**
	 * Specify the root LockMode for the query
	 */
	@Override
	public SelectionQuery<R> setHibernateLockMode(LockMode lockMode) {
		getLockOptions().setLockMode( lockMode );
		return this;
	}

	/**
	 * Specify a LockMode to apply to a specific alias defined in the query
	 *
	 * @deprecated use {{@link #setLockMode(String, LockMode)}}
	 */
	@Override @Deprecated
	public SelectionQuery<R> setAliasSpecificLockMode(String alias, LockMode lockMode) {
		getLockOptions().setAliasSpecificLockMode( alias, lockMode );
		return this;
	}

	/**
	 * Specifies whether follow-on locking should be applied?
	 */
	public SelectionQuery<R> setFollowOnLocking(boolean enable) {
		getLockOptions().setFollowOnLocking( enable );
		return this;
	}

	protected void collectHints(Map<String, Object> hints) {
		super.collectHints( hints );

		if ( isReadOnly() ) {
			hints.put( HINT_READ_ONLY, true );
		}

		putIfNotNull( hints, HINT_FETCH_SIZE, getFetchSize() );

		if ( isCacheable() ) {
			hints.put( HINT_CACHEABLE, true );
			putIfNotNull( hints, HINT_CACHE_REGION, getCacheRegion() );

			putIfNotNull( hints, HINT_CACHE_MODE, getCacheMode() );
			putIfNotNull( hints, JAKARTA_SHARED_CACHE_RETRIEVE_MODE, getQueryOptions().getCacheRetrieveMode() );
			putIfNotNull( hints, JAKARTA_SHARED_CACHE_STORE_MODE, getQueryOptions().getCacheStoreMode() );
			//noinspection deprecation
			putIfNotNull( hints, JPA_SHARED_CACHE_RETRIEVE_MODE, getQueryOptions().getCacheRetrieveMode() );
			//noinspection deprecation
			putIfNotNull( hints, JPA_SHARED_CACHE_STORE_MODE, getQueryOptions().getCacheStoreMode() );
		}

		final AppliedGraph appliedGraph = getQueryOptions().getAppliedGraph();
		if ( appliedGraph != null && appliedGraph.getSemantic() != null ) {
			hints.put( appliedGraph.getSemantic().getJakartaHintName(), appliedGraph );
			hints.put( appliedGraph.getSemantic().getJpaHintName(), appliedGraph );
		}

		putIfNotNull( hints, HINT_FOLLOW_ON_LOCKING, getQueryOptions().getLockOptions().getFollowOnLocking() );
	}

	@Override
	public Integer getFetchSize() {
		return getQueryOptions().getFetchSize();
	}

	@Override
	public SelectionQuery<R> setFetchSize(int fetchSize) {
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
	public SelectionQuery<R> setReadOnly(boolean readOnly) {
		getQueryOptions().setReadOnly( readOnly );
		return this;
	}
	@Override
	public CacheMode getCacheMode() {
		return getQueryOptions().getCacheMode();
	}

	@Override
	public CacheStoreMode getCacheStoreMode() {
		return getCacheMode().getJpaStoreMode();
	}

	@Override
	public CacheRetrieveMode getCacheRetrieveMode() {
		return getCacheMode().getJpaRetrieveMode();
	}

	@Override
	public SelectionQuery<R> setCacheMode(CacheMode cacheMode) {
		getQueryOptions().setCacheMode( cacheMode );
		return this;
	}

	@Override
	public SelectionQuery<R> setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
		return setCacheMode( fromJpaModes( cacheRetrieveMode, getQueryOptions().getCacheMode().getJpaStoreMode() ) );
	}

	@Override
	public SelectionQuery<R> setCacheStoreMode(CacheStoreMode cacheStoreMode) {
		return setCacheMode( fromJpaModes( getQueryOptions().getCacheMode().getJpaRetrieveMode(), cacheStoreMode ) );
	}

	@Override
	public boolean isCacheable() {
		return getQueryOptions().isResultCachingEnabled() == Boolean.TRUE;
	}

	@Override
	public SelectionQuery<R> setCacheable(boolean cacheable) {
		getQueryOptions().setResultCachingEnabled( cacheable );
		return this;
	}

	@Override
	public boolean isQueryPlanCacheable() {
		// By default, we assume query plan caching is enabled unless explicitly disabled
		return getQueryOptions().getQueryPlanCachingEnabled() != Boolean.FALSE;
	}

	@Override
	public SelectionQuery<R> setQueryPlanCacheable(boolean queryPlanCacheable) {
		getQueryOptions().setQueryPlanCachingEnabled( queryPlanCacheable );
		return this;
	}

	@Override
	public String getCacheRegion() {
		return getQueryOptions().getResultCacheRegionName();
	}

	@Override
	public SelectionQuery<R> setCacheRegion(String regionName) {
		getQueryOptions().setResultCacheRegionName( regionName );
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// covariance

	@Override
	public SelectionQuery<R> setHibernateFlushMode(FlushMode flushMode) {
		super.setHibernateFlushMode( flushMode );
		return this;
	}

	@Override
	public SelectionQuery<R> setTimeout(int timeout) {
		super.setTimeout( timeout );
		return this;
	}

	@Override
	public SelectionQuery<R> setComment(String comment) {
		super.setComment( comment );
		return this;
	}


	@Override
	public SelectionQuery<R> setParameter(String name, Object value) {
		super.setParameter( name, value );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameter(String name, P value, Class<P> javaType) {
		super.setParameter( name, value, javaType );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameter(String name, P value, BindableType<P> type) {
		super.setParameter( name, value, type );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameter(String name, Instant value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameter(int position, Object value) {
		super.setParameter( position, value );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameter(int position, P value, Class<P> javaType) {
		super.setParameter( position, value, javaType );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameter(int position, P value, BindableType<P> type) {
		super.setParameter( position, value, type );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameter(int position, Instant value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameter(QueryParameter<P> parameter, P value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameter(QueryParameter<P> parameter, P value, Class<P> javaType) {
		super.setParameter( parameter, value, javaType );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameter(QueryParameter<P> parameter, P value, BindableType<P> type) {
		super.setParameter( parameter, value, type );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameter(Parameter<P> parameter, P value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameter(String name, Calendar value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameter(String name, Date value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameter(int position, Calendar value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameter(int position, Date value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameterList(String name, @SuppressWarnings("rawtypes") Collection values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(String name, Collection<? extends P> values, Class<P> javaType) {
		super.setParameterList( name, values, javaType );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(String name, Collection<? extends P> values, BindableType<P> type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameterList(String name, Object[] values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(String name, P[] values, Class<P> javaType) {
		super.setParameterList( name, values, javaType );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(String name, P[] values, BindableType<P> type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameterList(int position, @SuppressWarnings("rawtypes") Collection values) {
		super.setParameterList( position, values );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(int position, Collection<? extends P> values, Class<P> javaType) {
		super.setParameterList( position, values, javaType );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(int position, Collection<? extends P> values, BindableType<P> type) {
		super.setParameterList( position, values, type );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameterList(int position, Object[] values) {
		super.setParameterList( position, values );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(int position, P[] values, Class<P> javaType) {
		super.setParameterList( position, values, javaType );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(int position, P[] values, BindableType<P> type) {
		super.setParameterList( position, values, type );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType) {
		super.setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, BindableType<P> type) {
		super.setParameterList( parameter, values, type );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, P[] values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType) {
		super.setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, P[] values, BindableType<P> type) {
		super.setParameterList( parameter, values, type );
		return this;
	}

	@Override
	public SelectionQuery<R> setProperties(@SuppressWarnings("rawtypes") Map map) {
		super.setProperties( map );
		return this;
	}

	@Override
	public SelectionQuery<R> setProperties(Object bean) {
		super.setProperties( bean );
		return this;
	}

	public SessionFactoryImplementor getSessionFactory() {
		return getSession().getFactory();
	}
}
