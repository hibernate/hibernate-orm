/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;

import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.metamodel.mapping.AssociationKey;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.sql.internal.SqlAstQueryPartProcessingStateImpl;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.spi.SqlAstQueryPartProcessingState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;

/**
 * Helper used when generating the database-snapshot select query
 */
public class LoaderSqlAstCreationState
		implements SqlAstQueryPartProcessingState, SqlAstCreationState, DomainResultCreationState, QueryOptions {
	public interface FetchProcessor {
		ImmutableFetchList visitFetches(FetchParent fetchParent, LoaderSqlAstCreationState creationState);
	}

	private final SqlAliasBaseManager sqlAliasBaseManager;
	private final boolean forceIdentifierSelection;
	private final LoadQueryInfluencers loadQueryInfluencers;
	private final SqlAstCreationContext sf;
	private final SqlAstQueryPartProcessingStateImpl processingState;
	private final FromClauseAccess fromClauseAccess;
	private final LockOptions lockOptions;
	private final FetchProcessor fetchProcessor;

	private boolean resolvingCircularFetch;
	private ForeignKeyDescriptor.Nature currentlyResolvingForeignKeySide;
	private final Set<AssociationKey> visitedAssociationKeys = new HashSet<>();

	public LoaderSqlAstCreationState(
			QueryPart queryPart,
			SqlAliasBaseManager sqlAliasBaseManager,
			FromClauseAccess fromClauseAccess,
			LockOptions lockOptions,
			FetchProcessor fetchProcessor,
			boolean forceIdentifierSelection,
			LoadQueryInfluencers loadQueryInfluencers,
			SqlAstCreationContext sf) {
		this.sqlAliasBaseManager = sqlAliasBaseManager;
		this.fromClauseAccess = fromClauseAccess;
		this.lockOptions = lockOptions;
		this.fetchProcessor = fetchProcessor;
		this.forceIdentifierSelection = forceIdentifierSelection;
		this.loadQueryInfluencers = loadQueryInfluencers;
		this.sf = sf;
		this.processingState = new SqlAstQueryPartProcessingStateImpl(
				queryPart,
				null,
				this,
				() -> Clause.IRRELEVANT,
				true
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
	public QueryPart getInflightQueryPart() {
		return processingState.getInflightQueryPart();
	}

	@Override
	public FromClause getFromClause() {
		return processingState.getFromClause();
	}

	@Override
	public void applyPredicate(Predicate predicate) {
		processingState.applyPredicate( predicate );
	}

	@Override
	public void registerTreatedFrom(SqmFrom<?, ?> sqmFrom) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void registerFromUsage(SqmFrom<?, ?> sqmFrom, boolean downgradeTreatUses) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<SqmFrom<?, ?>, Boolean> getFromRegistrations() {
		return Collections.emptyMap();
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
	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return loadQueryInfluencers;
	}

	@Override
	public boolean applyOnlyLoadByKeyFilters() {
		return true;
	}

	@Override
	public void registerLockMode(String identificationVariable, LockMode explicitLockMode) {
		throw new UnsupportedOperationException( "Registering lock modes should only be done for result set mappings" );
	}

	@Override
	public ImmutableFetchList visitFetches(FetchParent fetchParent) {
		return fetchProcessor.visitFetches( fetchParent, this );
	}

	@Override
	public <R> R withNestedFetchParent(FetchParent fetchParent, Function<FetchParent, R> action) {
		final FetchParent nestingFetchParent = processingState.getNestingFetchParent();
		processingState.setNestingFetchParent( fetchParent );
		final R result = action.apply( fetchParent );
		processingState.setNestingFetchParent( nestingFetchParent );
		return result;
	}

	@Override
	public boolean isResolvingCircularFetch() {
		return resolvingCircularFetch;
	}

	@Override
	public void setResolvingCircularFetch(boolean resolvingCircularFetch) {
		this.resolvingCircularFetch = resolvingCircularFetch;
	}

	@Override
	public ForeignKeyDescriptor.Nature getCurrentlyResolvingForeignKeyPart() {
		return currentlyResolvingForeignKeySide;
	}

	@Override
	public void setCurrentlyResolvingForeignKeyPart(ForeignKeyDescriptor.Nature currentlyResolvingForeignKeySide) {
		this.currentlyResolvingForeignKeySide = currentlyResolvingForeignKeySide;
	}

	@Override
	public boolean forceIdentifierSelection() {
		return forceIdentifierSelection;
	}

	@Override
	public SqlAstCreationState getSqlAstCreationState() {
		return this;
	}

	@Override
	public boolean registerVisitedAssociationKey(AssociationKey associationKey) {
		return visitedAssociationKeys.add( associationKey );
	}

	@Override
	public void removeVisitedAssociationKey(AssociationKey associationKey) {
		visitedAssociationKeys.remove( associationKey );
	}

	@Override
	public boolean isAssociationKeyVisited(AssociationKey associationKey) {
		return visitedAssociationKeys.contains( associationKey );
	}

	@Override
	public boolean isRegisteringVisitedAssociationKeys(){
		return true;
	}

	@Override
	public ModelPart resolveModelPart(NavigablePath navigablePath) {
		// for now, let's assume that the navigable-path refers to TableGroup
		return fromClauseAccess.findTableGroup( navigablePath ).getModelPart();
	}

	@Override
	public SqlAstProcessingState getParentState() {
		return null;
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
	public TupleTransformer<?> getTupleTransformer() {
		return null;
	}

	@Override
	public ResultListTransformer<?> getResultListTransformer() {
		return null;
	}

	@Override
	public Boolean isResultCachingEnabled() {
		return false;
	}

	@Override
	public Boolean getQueryPlanCachingEnabled() {
		return null;
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

	@Override
	public Set<String> getEnabledFetchProfiles() {
		return null;
	}

	@Override
	public Set<String> getDisabledFetchProfiles() {
		return null;
	}
}
