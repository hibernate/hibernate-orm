/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.spi.MutableQueryOptions;

import static java.util.Collections.emptyList;
import static org.hibernate.query.QueryLogging.QUERY_LOGGER;

/**
 * @author Steve Ebersole
 */
public class QueryOptionsImpl implements MutableQueryOptions, AppliedGraph {
	private Integer timeout;
	private FlushMode flushMode;
	private String comment;
	private List<String> databaseHints;

	// only valid for (non-native) select queries
	private final Limit limit = new Limit();
	private final LockOptions lockOptions = new LockOptions();
	private Integer fetchSize;
	private CacheRetrieveMode cacheRetrieveMode;
	private CacheStoreMode cacheStoreMode;
	private boolean refreshSession;
	private Boolean resultCachingEnabled;
	private String resultCacheRegionName;
	private Boolean readOnlyEnabled;
	private Boolean queryPlanCachingEnabled;
	private Boolean limitInMemoryEnabled;

	private TupleTransformer<?> tupleTransformer;
	private ResultListTransformer<?> resultListTransformer;

	private RootGraphImplementor<?> rootGraph;
	private GraphSemantic graphSemantic;
	private Set<String> enabledFetchProfiles;
	private Set<String> disabledFetchProfiles;

	@Override
	public Integer getTimeout() {
		return timeout;
	}

	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}

	@Override
	public FlushMode getFlushMode() {
		return flushMode;
	}

	@Override
	public void setFlushMode(FlushMode flushMode) {
		this.flushMode = flushMode;
	}

	@Override
	public String getComment() {
		return comment;
	}

	@Override
	public void setComment(String comment) {
		this.comment = comment;
	}

	@Override
	public List<String> getDatabaseHints() {
		return databaseHints == null ? emptyList() : databaseHints;
	}

	@Override
	public void addDatabaseHint(String hint) {
		if ( databaseHints == null ) {
			databaseHints = new ArrayList<>();
		}
		databaseHints.add( hint );
	}

	@Override
	public void setTupleTransformer(TupleTransformer<?> transformer) {
		this.tupleTransformer = transformer;
	}

	@Override
	public void setResultListTransformer(ResultListTransformer<?> transformer) {
		this.resultListTransformer = transformer;
	}

	@Override
	public Limit getLimit() {
		return limit;
	}

	@Override
	public LockOptions getLockOptions() {
		return lockOptions;
	}

	@Override
	public Integer getFetchSize() {
		return fetchSize;
	}

	public void setFetchSize(Integer fetchSize) {
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
	public CacheRetrieveMode getCacheRetrieveMode() {
		return cacheRetrieveMode;
	}

	@Override
	public CacheStoreMode getCacheStoreMode() {
		return cacheStoreMode;
	}

	@Override
	public void setCacheRetrieveMode(CacheRetrieveMode retrieveMode) {
		this.cacheRetrieveMode = retrieveMode;
		this.refreshSession = false;
	}

	@Override
	public void setCacheStoreMode(CacheStoreMode storeMode) {
		this.cacheStoreMode = storeMode;
		this.refreshSession = false;
	}

	@Override
	public void setCacheMode(CacheMode cacheMode) {
		if ( cacheMode == null ) {
			QUERY_LOGGER.debug( "Null CacheMode passed to #setCacheMode; falling back to 'NORMAL'" );
			cacheMode = CacheMode.NORMAL;
		}

		this.cacheRetrieveMode = cacheMode.getJpaRetrieveMode();
		this.cacheStoreMode = cacheMode.getJpaStoreMode();
		this.refreshSession = cacheMode == CacheMode.REFRESH_SESSION;
	}

	@Override
	public Boolean isResultCachingEnabled() {
		return resultCachingEnabled;
	}

	@Override
	public void setResultCachingEnabled(boolean resultCachingEnabled) {
		this.resultCachingEnabled = resultCachingEnabled;
	}

	@Override
	public String getResultCacheRegionName() {
		return resultCacheRegionName;
	}

	@Override
	public Boolean getQueryPlanCachingEnabled() {
		return queryPlanCachingEnabled;
	}

	@Override
	public Boolean isLimitInMemoryEnabled() {
		return limitInMemoryEnabled;
	}

	@Override
	public void setQueryPlanCachingEnabled(Boolean queryPlanCachingEnabled) {
		this.queryPlanCachingEnabled = queryPlanCachingEnabled;
	}

	@Override
	public TupleTransformer<?> getTupleTransformer() {
		return tupleTransformer;
	}

	@Override
	public ResultListTransformer<?> getResultListTransformer() {
		return resultListTransformer;
	}

	@Override
	public void setResultCacheRegionName(String resultCacheRegionName) {
		this.resultCacheRegionName = resultCacheRegionName;
	}

	@Override
	public void setTimeout(int timeout) {
		this.timeout = timeout;
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
	public Boolean isReadOnly() {
		return readOnlyEnabled;
	}

	@Override
	public void applyGraph(RootGraphImplementor<?> rootGraph, GraphSemantic graphSemantic) {
		this.rootGraph = rootGraph;
		this.graphSemantic = graphSemantic;
	}

	@Override
	public void enableFetchProfile(String profileName) {
		if ( enabledFetchProfiles == null ) {
			enabledFetchProfiles = new HashSet<>();
		}
		enabledFetchProfiles.add( profileName );
		if ( disabledFetchProfiles != null ) {
			disabledFetchProfiles.remove( profileName );
		}
	}

	@Override
	public void disableFetchProfile(String profileName) {
		if ( disabledFetchProfiles == null ) {
			disabledFetchProfiles = new HashSet<>();
		}
		disabledFetchProfiles.add( profileName );
		if ( enabledFetchProfiles != null ) {
			enabledFetchProfiles.remove( profileName );
		}
	}

	@Override
	public Set<String> getEnabledFetchProfiles() {
		return enabledFetchProfiles;
	}

	@Override
	public Set<String> getDisabledFetchProfiles() {
		return disabledFetchProfiles;
	}

	@Override
	public AppliedGraph getAppliedGraph() {
		return this;
	}

	@Override
	public @Nullable RootGraphImplementor<?> getGraph() {
		return rootGraph;
	}

	@Override
	public @Nullable GraphSemantic getSemantic() {
		return graphSemantic;
	}
}
