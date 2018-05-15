/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.query.Limit;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.spi.MutableQueryOptions;

/**
 * @todo split there into org.hibernate.query.QueryOptions and org.hibernate.sql.exec.spi.JdbcOperationOptions
 * 		^^ package boundaries
 * 		^^ really comes down to how we want to view JdbcOperation "execution".  E.g. for a SELECT does that
 * 			return the ResultSet or the List<T> ?  If the former, then the split maybe makes sense; if the
 * 			latter then then split probably does not make sense.
 *
 *
 * @author Steve Ebersole
 */
public class QueryOptionsImpl implements MutableQueryOptions, AppliedGraph {

	// todo (6.0) - handle `MutableQueryOptions` responsibility as a builder

	private Integer timeout;
	private FlushMode flushMode;
	private String comment;
	private List<String> databaseHints;

	// only valid for (non-native) select queries
	private final Limit limit = new Limit();
	private final LockOptions lockOptions = new LockOptions();
	private Integer fetchSize;
	private CacheMode cacheMode;
	private Boolean resultCachingEnabled;
	private String resultCacheRegionName;
	private Boolean readOnlyEnabled;

	private TupleTransformer tupleTransformer;
	private ResultListTransformer resultListTransformer;

	private RootGraphImplementor<?> rootGraph;
	private GraphSemantic graphSemantic;

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

	public void setFlushMode(FlushMode flushMode) {
		this.flushMode = flushMode;
	}

	@Override
	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	@Override
	public List<String> getDatabaseHints() {
		return databaseHints == null ? Collections.emptyList() : databaseHints;
	}

	@Override
	public void addDatabaseHint(String hint) {
		if ( databaseHints == null ) {
			databaseHints = new ArrayList<String>();
		}
		databaseHints.add( hint );
	}

	@Override
	public void setTupleTransformer(TupleTransformer transformer) {
		this.tupleTransformer = transformer;
	}

	@Override
	public void setResultListTransformer(ResultListTransformer transformer) {
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
		return cacheMode;
	}

	public void setCacheMode(CacheMode cacheMode) {
		this.cacheMode = cacheMode;
	}

	@Override
	public Boolean isResultCachingEnabled() {
		return resultCachingEnabled;
	}

	public void setResultCachingEnabled(boolean resultCachingEnabled) {
		this.resultCachingEnabled = resultCachingEnabled;
	}

	@Override
	public String getResultCacheRegionName() {
		return resultCacheRegionName;
	}

	@Override
	public TupleTransformer getTupleTransformer() {
		return tupleTransformer;
	}

	@Override
	public ResultListTransformer getResultListTransformer() {
		return resultListTransformer;
	}

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
	public void setReadOnly(boolean readOnly) {
		this.readOnlyEnabled = readOnly;
	}

	@Override
	public Boolean isReadOnly() {
		return readOnlyEnabled;
	}

	public void applyGraph(RootGraphImplementor<?> rootGraph, GraphSemantic graphSemantic) {
		this.rootGraph = rootGraph;
		this.graphSemantic = graphSemantic;
	}

	@Override
	public AppliedGraph getAppliedGraph() {
		return this;
	}

	@Override
	public RootGraphImplementor<?> getGraph() {
		return rootGraph;
	}

	@Override
	public GraphSemantic getSemantic() {
		return graphSemantic;
	}
}
