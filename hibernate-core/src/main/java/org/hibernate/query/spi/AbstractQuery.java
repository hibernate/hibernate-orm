/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import java.io.Serializable;
import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.internal.EntityManagerMessageLogger;
import org.hibernate.internal.HEMLogging;
import org.hibernate.jpa.AvailableHints;
import org.hibernate.jpa.internal.util.FlushModeTypeHelper;
import org.hibernate.jpa.internal.util.LockModeTypeHelper;
import org.hibernate.query.BindableType;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.KeyedPage;
import org.hibernate.query.KeyedResultList;
import org.hibernate.query.Order;
import org.hibernate.query.Query;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.named.NamedQueryMemento;

import static org.hibernate.LockOptions.WAIT_FOREVER;
import static org.hibernate.jpa.HibernateHints.HINT_CACHEABLE;
import static org.hibernate.jpa.HibernateHints.HINT_CACHE_MODE;
import static org.hibernate.jpa.HibernateHints.HINT_CACHE_REGION;
import static org.hibernate.jpa.HibernateHints.HINT_COMMENT;
import static org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE;
import static org.hibernate.jpa.HibernateHints.HINT_FLUSH_MODE;
import static org.hibernate.jpa.HibernateHints.HINT_NATIVE_LOCK_MODE;
import static org.hibernate.jpa.HibernateHints.HINT_READ_ONLY;
import static org.hibernate.jpa.HibernateHints.HINT_TIMEOUT;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_CACHE_RETRIEVE_MODE;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_CACHE_STORE_MODE;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_LOCK_SCOPE;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_LOCK_TIMEOUT;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_QUERY_TIMEOUT;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_CACHE_RETRIEVE_MODE;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_CACHE_STORE_MODE;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_LOCK_SCOPE;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_LOCK_TIMEOUT;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_QUERY_TIMEOUT;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractQuery<R>
		extends AbstractSelectionQuery<R>
		implements QueryImplementor<R> {
	protected static final EntityManagerMessageLogger log = HEMLogging.messageLogger( AbstractQuery.class );

	public AbstractQuery(SharedSessionContractImplementor session) {
		super( session );
	}

	@Override
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

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// QueryOptions handling

	@Override
	public QueryImplementor<R> setHint(String hintName, Object value) {
		super.setHint( hintName, value );
		return this;
	}

	@Override
	public QueryImplementor<R> setEntityGraph(EntityGraph<R> graph, GraphSemantic semantic) {
		super.setEntityGraph( graph, semantic );
		return this;
	}

	@Override
	public QueryImplementor<R> enableFetchProfile(String profileName) {
		super.enableFetchProfile( profileName );
		return this;
	}

	@Override
	public QueryImplementor<R> disableFetchProfile(String profileName) {
		super.disableFetchProfile( profileName );
		return this;
	}

	@Override
	public MutableQueryOptions getQueryOptions() {
		return super.getQueryOptions();
	}


	@Override
	public int getMaxResults() {
		return super.getMaxResults();
	}

	@Override
	public QueryImplementor<R> setMaxResults(int maxResult) {
		super.setMaxResults( maxResult );
		return this;
	}

	@Override
	public int getFirstResult() {
		return super.getFirstResult();
	}

	@Override
	public QueryImplementor<R> setFirstResult(int startPosition) {
		super.setFirstResult( startPosition );
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
	public QueryImplementor<R> setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
		super.setCacheRetrieveMode( cacheRetrieveMode );
		return this;
	}

	@Override
	public QueryImplementor<R> setCacheStoreMode(CacheStoreMode cacheStoreMode) {
		super.setCacheStoreMode( cacheStoreMode );
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
	public QueryImplementor<R> setQueryPlanCacheable(boolean queryPlanCacheable) {
		super.setQueryPlanCacheable( queryPlanCacheable );
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
		return super.getLockMode();
	}

	@Override
	public QueryImplementor<R> setLockOptions(LockOptions lockOptions) {
		getQueryOptions().getLockOptions().overlay( lockOptions );
		return this;
	}

	@Override
	public QueryImplementor<R> setLockMode(String alias, LockMode lockMode) {
		super.setLockMode( alias, lockMode );
		return this;
	}

	@Override
	public QueryImplementor<R> setLockMode(LockModeType lockModeType) {
		getSession().checkOpen();
		super.setHibernateLockMode( LockModeTypeHelper.getLockMode( lockModeType ) );
		return this;
	}

	@Override
	public Query<R> setOrder(List<Order<? super R>> orders) {
		throw new UnsupportedOperationException( "Should be implemented by " + this.getClass().getName() );
	}

	@Override
	public Query<R> setOrder(Order<? super R> order) {
		throw new UnsupportedOperationException( "Should be implemented by " + this.getClass().getName() );
	}

	@Override
	public String getComment() {
		return super.getComment();
	}

	@Override
	public QueryImplementor<R> setComment(String comment) {
		super.setComment( comment );
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
			hints.put( HINT_SPEC_LOCK_SCOPE, getLockOptions().getLockScope() );
			hints.put( HINT_JAVAEE_LOCK_SCOPE, getLockOptions().getLockScope() );
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



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// QueryParameter handling

	protected boolean resolveJdbcParameterTypeIfNecessary() {
		return true;
	}

	@Override
	@SuppressWarnings( {"unchecked", "rawtypes"} )
	public Set<Parameter<?>> getParameters() {
		return super.getParameters();
	}

	@Override
	public QueryImplementor<R> setParameter(String name, Object value) {
		super.setParameter( name, value );
		return this;
	}

	@Override
	public <P> QueryImplementor<R> setParameter(String name, P value, Class<P> javaTypeClass) {
		super.setParameter( name, value, javaTypeClass );
		return this;
	}

	@Override
	public <P> QueryImplementor<R> setParameter(String name, P value, BindableType<P> type) {
		super.setParameter( name, value, type );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameter(String name, Instant value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameter(int position, Object value) {
		super.setParameter( position, value );
		return this;
	}

	@Override
	public <P> QueryImplementor<R> setParameter(int position, P value, Class<P> javaTypeClass) {
		super.setParameter( position, value, javaTypeClass );
		return this;
	}

	@Override
	public <P> QueryImplementor<R> setParameter(int position, P value, BindableType<P> type) {
		super.setParameter( position, value, type );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameter(int position, Instant value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}



	@Override
	public <P> QueryImplementor<R> setParameter(QueryParameter<P> parameter, P value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	public <P> QueryImplementor<R> setParameter(QueryParameter<P> parameter, P value, Class<P> javaTypeClass) {
		super.setParameter( parameter, value, javaTypeClass );
		return this;
	}

	@Override
	public <P> QueryImplementor<R> setParameter(QueryParameter<P> parameter, P value, BindableType<P> type) {
		super.setParameter( parameter, value, type );
		return this;
	}


	@Override
	public <P> QueryImplementor<R> setParameter(Parameter<P> parameter, P value) {
		super.setParameter( parameter, value );
		return this;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter list

	@Override
	public QueryImplementor<R> setParameterList(String name, @SuppressWarnings("rawtypes") Collection values) {
		super.setParameterList( name, values );
		return this;
	}

	public <P> QueryImplementor<R> setParameterList(String name, Collection<? extends P> values, Class<P> javaTypeClass) {
		super.setParameterList( name, values, javaTypeClass );
		return this;
	}


	@Override
	public <P> QueryImplementor<R> setParameterList(String name, Collection<? extends P> values, BindableType<P> type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameterList(String name, Object[] values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	public <P> QueryImplementor<R> setParameterList(String name, P[] values, Class<P> javaTypeClass) {
		super.setParameterList( name, values, javaTypeClass );
		return this;
	}

	public <P> QueryImplementor<R> setParameterList(String name, P[] values, BindableType<P> type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameterList(int position, @SuppressWarnings("rawtypes") Collection values) {
		super.setParameterList( position, values );
		return this;
	}

	@Override
	public <P> QueryImplementor<R> setParameterList(int position, Collection<? extends P> values, Class<P> javaTypeClass) {
		super.setParameterList( position, values, javaTypeClass );
		return this;
	}

	@Override
	public <P> QueryImplementor<R> setParameterList(int position, Collection<? extends P> values, BindableType<P> type) {
		super.setParameterList( position, values, type );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameterList(int position, Object[] values) {
		super.setParameterList( position, values );
		return this;
	}

	@Override
	public <P> QueryImplementor<R> setParameterList(int position, P[] values, Class<P> javaTypeClass) {
		super.setParameterList( position, values, javaTypeClass );
		return this;
	}

	public <P> QueryImplementor<R> setParameterList(int position, P[] values, BindableType<P> type) {
		super.setParameterList( position, values, type );
		return this;
	}

	@Override
	public <P> QueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	public <P> QueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaTypeClass) {
		super.setParameterList( parameter, values, javaTypeClass );
		return this;
	}

	@Override
	public <P> QueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, BindableType<P> type) {
		super.setParameterList( parameter, values, type );
		return this;
	}

	@Override
	public <P> QueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	public <P> QueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaTypeClass) {
		super.setParameterList( parameter, values, javaTypeClass );
		return this;
	}


	@Override
	public <P> QueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values, BindableType<P> type) {
		super.setParameterList( parameter, values, type );
		return this;
	}


	@Override
	public QueryImplementor<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameter(String name, Calendar value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameter(String name, Date value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameter(int position, Calendar value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public QueryImplementor<R> setParameter(int position, Date value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public QueryImplementor<R> setProperties(Object bean) {
		super.setProperties( bean );
		return this;
	}

	@Override
	public QueryImplementor<R> setProperties(@SuppressWarnings("rawtypes") Map map) {
		super.setProperties( map );
		return this;
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// execution

	protected void prepareForExecution() {
	}

	@Override
	public int executeUpdate() throws HibernateException {
		getSession().checkTransactionNeededForUpdateOperation( "Executing an update/delete query" );
		final HashSet<String> fetchProfiles = beforeQueryHandlingFetchProfiles();
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
			throw getSession().getExceptionConverter().convert( e );
		}
		finally {
			afterQueryHandlingFetchProfiles( success, fetchProfiles );
		}
	}

	protected abstract int doExecuteUpdate();

	@Override
	public KeyedResultList<R> getKeyedResultList(KeyedPage<R> keyedPage) {
		throw new UnsupportedOperationException("Getting keyed result list is not supported by this query.");
	}

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
