/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import jakarta.annotation.Nonnull;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.Timeout;
import jakarta.annotation.Nullable;

import org.hibernate.CacheMode;
import org.hibernate.LockOptions;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.spi.MutableQueryOptions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.hibernate.query.internal.QueryLogging.QUERY_LOGGER;

/**
 * @author Steve Ebersole
 */
public class QueryOptionsImpl implements MutableQueryOptions, AppliedGraph {
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Valid for all query types
	private QueryFlushMode queryFlushMode = QueryFlushMode.DEFAULT;
	private Timeout timeout;
	private String comment;
	private List<String> databaseHints;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Valid for select query types
	private Integer fetchSize;
	private Boolean readOnlyEnabled;

	private Boolean resultCachingEnabled;
	private CacheRetrieveMode cacheRetrieveMode;
	private CacheStoreMode cacheStoreMode;
	private String resultCacheRegionName;
	private boolean refreshSession;

	private Boolean queryPlanCachingEnabled;
	private Boolean limitInMemoryEnabled;

	private final Limit limit;
	private final LockOptions lockOptions;

	private TupleTransformer<?> tupleTransformer;
	private ResultListTransformer<?> resultListTransformer;

	private RootGraphImplementor<?> rootGraph;
	private GraphSemantic graphSemantic;
	private Set<String> enabledFetchProfiles;
	private Set<String> disabledFetchProfiles;

	public QueryOptionsImpl() {
		this.limit = new Limit();
		this.lockOptions = new LockOptions();
	}

	/**
	 * Copy constructor.
	 * @see #makeCopy()
	 */
	public QueryOptionsImpl(@Nonnull QueryOptionsImpl original) {
		this.queryFlushMode = original.queryFlushMode;
		this.timeout = original.timeout;
		this.comment = original.comment;
		this.databaseHints = copy( original.databaseHints );
		this.fetchSize = original.fetchSize;
		this.readOnlyEnabled = original.readOnlyEnabled;
		this.resultCachingEnabled = original.resultCachingEnabled;
		this.cacheRetrieveMode = original.cacheRetrieveMode;
		this.cacheStoreMode = original.cacheStoreMode;
		this.resultCacheRegionName = original.resultCacheRegionName;
		this.refreshSession = original.refreshSession;
		this.queryPlanCachingEnabled = original.queryPlanCachingEnabled;
		this.limitInMemoryEnabled = original.limitInMemoryEnabled;
		this.limit = original.limit.makeCopy();
		this.lockOptions = original.lockOptions.makeCopy();
		this.tupleTransformer = original.tupleTransformer;
		this.resultListTransformer = original.resultListTransformer;
		this.rootGraph = original.rootGraph;
		this.graphSemantic = original.graphSemantic;
		this.enabledFetchProfiles = copy( original.enabledFetchProfiles );
		this.disabledFetchProfiles = copy( original.disabledFetchProfiles );
	}

	private <E> Set<E> copy(Set<E> original) {
		if ( original == null ) {
			return null;
		}
		return new HashSet<>( original );
	}

	private <E> List<E> copy(@Nullable List<E> original) {
		return original == null ? null : new ArrayList<>( original );
	}

	@Override
	@Nullable
	public Timeout getTimeout() {
		return timeout;
	}

	@Override
	public void setTimeout(int timeout) {
		setTimeout( Timeout.seconds( timeout ) );
	}

	public void setTimeout(@Nullable Integer timeout) {
		setTimeout( timeout == null ? null : Timeout.seconds( timeout ) );
	}

	@Override
	public void setTimeout(@Nullable Timeout timeout) {
		this.timeout = timeout;
	}

	@Override
	@Nonnull
	public QueryFlushMode getQueryFlushMode() {
		return queryFlushMode;
	}

	@Override
	public void setQueryFlushMode(@Nonnull QueryFlushMode queryFlushMode) {
		requireNonNull( queryFlushMode );
		this.queryFlushMode = queryFlushMode;
	}

	@Override
	@Nullable
	public String getComment() {
		return comment;
	}

	@Override
	public void setComment(@Nullable String comment) {
		this.comment = comment;
	}

	@Override
	@Nonnull
	public List<String> getDatabaseHints() {
		return databaseHints == null ? emptyList() : databaseHints;
	}

	@Override
	public void addDatabaseHint(@Nonnull String hint) {
		if ( databaseHints == null ) {
			databaseHints = new ArrayList<>();
		}
		databaseHints.add( hint );
	}

	@Override
	public void setTupleTransformer(@Nonnull TupleTransformer<?> transformer) {
		this.tupleTransformer = transformer;
	}

	@Override
	public void setResultListTransformer(@Nonnull ResultListTransformer<?> transformer) {
		this.resultListTransformer = transformer;
	}

	@Override
	@Nonnull
	public Limit getLimit() {
		return limit;
	}

	@Override
	@Nonnull
	public LockOptions getLockOptions() {
		return lockOptions;
	}

	@Override
	@Nullable
	public Integer getFetchSize() {
		return fetchSize;
	}

	public void setFetchSize(@Nullable Integer fetchSize) {
		this.fetchSize = fetchSize;
	}

	@Override
	public CacheMode getCacheMode() {
		final var cacheMode = CacheMode.fromJpaModes( cacheRetrieveMode, cacheStoreMode );
		return refreshSession && cacheMode == CacheMode.REFRESH
				? CacheMode.REFRESH_SESSION
				: cacheMode;
	}

	@Override
	@Nullable
	public CacheRetrieveMode getCacheRetrieveMode() {
		return cacheRetrieveMode;
	}

	@Override
	@Nullable
	public CacheStoreMode getCacheStoreMode() {
		return cacheStoreMode;
	}

	@Override
	public void setCacheRetrieveMode(@Nullable CacheRetrieveMode retrieveMode) {
		this.cacheRetrieveMode = retrieveMode;
		this.refreshSession = false;
	}

	@Override
	public void setCacheStoreMode(@Nullable CacheStoreMode storeMode) {
		this.cacheStoreMode = storeMode;
		this.refreshSession = false;
	}

	@Override
	public void setCacheMode(@Nullable CacheMode cacheMode) {
		if ( cacheMode == null ) {
			QUERY_LOGGER.debug( "Null CacheMode passed to #setCacheMode; falling back to 'NORMAL'" );
			cacheMode = CacheMode.NORMAL;
		}

		this.cacheRetrieveMode = cacheMode.getJpaRetrieveMode();
		this.cacheStoreMode = cacheMode.getJpaStoreMode();
		this.refreshSession = cacheMode == CacheMode.REFRESH_SESSION;
	}

	@Override
	@Nullable
	public Boolean isResultCachingEnabled() {
		return resultCachingEnabled;
	}

	@Override
	public void setResultCachingEnabled(boolean resultCachingEnabled) {
		this.resultCachingEnabled = resultCachingEnabled;
	}

	@Override
	@Nullable
	public String getResultCacheRegionName() {
		return resultCacheRegionName;
	}

	@Override
	@Nullable
	public Boolean getQueryPlanCachingEnabled() {
		return queryPlanCachingEnabled;
	}

	@Override
	@Nullable
	public Boolean isLimitInMemoryEnabled() {
		return limitInMemoryEnabled;
	}

	@Override
	public void setQueryPlanCachingEnabled(@Nullable Boolean queryPlanCachingEnabled) {
		this.queryPlanCachingEnabled = queryPlanCachingEnabled;
	}

	@Override
	@Nullable
	public TupleTransformer<?> getTupleTransformer() {
		return tupleTransformer;
	}

	@Override
	@Nullable
	public ResultListTransformer<?> getResultListTransformer() {
		return resultListTransformer;
	}

	@Override
	public void setResultCacheRegionName(@Nullable String resultCacheRegionName) {
		this.resultCacheRegionName = resultCacheRegionName;
	}

	@Override
	public void setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
	}

	@Override
	public void setLimitInMemory(boolean limitInMemory) {
		this.limitInMemoryEnabled = limitInMemory;
	}

	@Override
	public void setReadOnly(boolean readOnly) {
		this.readOnlyEnabled = readOnly;
	}

	@Override
	@Nullable
	public Boolean isReadOnly() {
		return readOnlyEnabled;
	}

	@Override
	public void applyGraph(@Nonnull RootGraphImplementor<?> rootGraph, @Nonnull GraphSemantic graphSemantic) {
		this.rootGraph = rootGraph;
		this.graphSemantic = graphSemantic;
	}

	@Override
	public void enableFetchProfile(@Nonnull String profileName) {
		if ( enabledFetchProfiles == null ) {
			enabledFetchProfiles = new HashSet<>();
		}
		enabledFetchProfiles.add( profileName );
		if ( disabledFetchProfiles != null ) {
			disabledFetchProfiles.remove( profileName );
		}
	}

	@Override
	public void disableFetchProfile(@Nonnull String profileName) {
		if ( disabledFetchProfiles == null ) {
			disabledFetchProfiles = new HashSet<>();
		}
		disabledFetchProfiles.add( profileName );
		if ( enabledFetchProfiles != null ) {
			enabledFetchProfiles.remove( profileName );
		}
	}

	@Override
	@Nullable
	public Set<String> getEnabledFetchProfiles() {
		return enabledFetchProfiles;
	}

	@Override
	@Nullable
	public Set<String> getDisabledFetchProfiles() {
		return disabledFetchProfiles;
	}

	@Override
	@Nullable
	public AppliedGraph getAppliedGraph() {
		return this;
	}

	@Override
	@Nullable
	public RootGraphImplementor<?> getGraph() {
		return rootGraph;
	}

	@Override
	@Nullable
	public GraphSemantic getSemantic() {
		return graphSemantic;
	}

	@Override
	@Nonnull
	public MutableQueryOptions makeCopy() {
		return new QueryOptionsImpl( this );
	}
}
