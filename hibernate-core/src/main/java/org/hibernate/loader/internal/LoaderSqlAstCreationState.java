/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.internal;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.persistence.CacheRetrieveMode;
import javax.persistence.CacheStoreMode;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.query.Limit;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.sql.internal.SqlAstQuerySpecProcessingStateImpl;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;

/**
 * Helper used when generating the database-snapshot select query
 */
public class LoaderSqlAstCreationState
		implements SqlAstProcessingState, SqlAstCreationState, DomainResultCreationState, QueryOptions {
	private final SqlAliasBaseManager sqlAliasBaseManager;
	private final SqlAstCreationContext sf;
	private final SqlAstQuerySpecProcessingStateImpl processingState;
	private final FromClauseAccess fromClauseAccess;
	private final LockOptions lockOptions;
	private final BiFunction<FetchParent, LoaderSqlAstCreationState, List<Fetch>> fetchProcessor;

	public LoaderSqlAstCreationState(
			QuerySpec querySpec,
			SqlAliasBaseManager sqlAliasBaseManager,
			FromClauseAccess fromClauseAccess,
			LockOptions lockOptions,
			BiFunction<FetchParent, LoaderSqlAstCreationState, List<Fetch>> fetchProcessor,
			SqlAstCreationContext sf) {
		this.sqlAliasBaseManager = sqlAliasBaseManager;
		this.fromClauseAccess = fromClauseAccess;
		this.lockOptions = lockOptions;
		this.fetchProcessor = fetchProcessor;
		this.sf = sf;
		processingState = new SqlAstQuerySpecProcessingStateImpl(
				querySpec,
				this,
				this,
				() -> Clause.IRRELEVANT
		);
	}


	public LoaderSqlAstCreationState(
			QuerySpec querySpec,
			SqlAliasBaseManager sqlAliasBaseManager,
			LockOptions lockOptions,
			SessionFactoryImplementor sf) {
		this(
				querySpec,
				sqlAliasBaseManager,
				new FromClauseIndex(),
				lockOptions,
				(fetchParent,state) -> Collections.emptyList(),
				sf
		);
	}

	@Override
	public SqlAstCreationContext getCreationContext() {
		return sf;
	}

	@Override
	public SqlAstProcessingState getCurrentProcessingState() {
		return this;
	}

	@Override
	public SqlExpressionResolver getSqlExpressionResolver() {
		return processingState;
	}

	@Override
	public FromClauseAccess getFromClauseAccess() {
		return fromClauseAccess;
	}

	@Override
	public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
		return sqlAliasBaseManager;
	}

	@Override
	public LockMode determineLockMode(String identificationVariable) {
		return lockOptions.getEffectiveLockMode( identificationVariable );
	}

	@Override
	public List<Fetch> visitFetches(FetchParent fetchParent) {
		return fetchProcessor.apply( fetchParent, this );
	}

	@Override
	public SqlAstCreationState getSqlAstCreationState() {
		return this;
	}

	@Override
	public SqlAstProcessingState getParentState() {
		return null;
	}

	private static class FromClauseIndex implements FromClauseAccess {
		private TableGroup tableGroup;

		@Override
		public TableGroup findTableGroup(NavigablePath navigablePath) {
			if ( tableGroup != null ) {
				if ( tableGroup.getNavigablePath().equals( navigablePath ) ) {
					return tableGroup;
				}

				throw new IllegalArgumentException(
						"NavigablePath [" + navigablePath + "] did not match base TableGroup ["
								+ tableGroup.getNavigablePath() + "]"
				);
			}

			return null;
		}

		@Override
		public void registerTableGroup(NavigablePath navigablePath, TableGroup tableGroup) {
			assert tableGroup.getNavigablePath().equals( navigablePath );

			if ( this.tableGroup != null ) {
				if ( this.tableGroup != tableGroup ) {
					throw new IllegalArgumentException(
							"Base TableGroup [" + tableGroup.getNavigablePath() + "] already set - " + navigablePath
					);
				}
				assert this.tableGroup.getNavigablePath().equals( navigablePath );
			}
			else {
				this.tableGroup = tableGroup;
			}
		}
	}

	@Override
	public Integer getTimeout() {
		return null;
	}

	@Override
	public FlushMode getFlushMode() {
		return null;
	}

	@Override
	public Boolean isReadOnly() {
		return null;
	}

	@Override
	public AppliedGraph getAppliedGraph() {
		// todo (6.0) : use this from the "load settings" (Hibernate method args, map passed to JPA methods)
		//   the legacy approach is to temporarily set this on the Session's "load query influencers"
		return null;
	}

	@Override
	public TupleTransformer getTupleTransformer() {
		return null;
	}

	@Override
	public ResultListTransformer getResultListTransformer() {
		return null;
	}

	@Override
	public Boolean isResultCachingEnabled() {
		return false;
	}

	@Override
	public CacheRetrieveMode getCacheRetrieveMode() {
		return CacheRetrieveMode.BYPASS;
	}

	@Override
	public CacheStoreMode getCacheStoreMode() {
		return CacheStoreMode.BYPASS;
	}

	@Override
	public String getResultCacheRegionName() {
		return null;
	}

	@Override
	public LockOptions getLockOptions() {
		return lockOptions;
	}

	@Override
	public String getComment() {
		return null;
	}

	@Override
	public List<String> getDatabaseHints() {
		return null;
	}

	@Override
	public Integer getFetchSize() {
		return null;
	}

	@Override
	public Limit getLimit() {
		return null;
	}
}
