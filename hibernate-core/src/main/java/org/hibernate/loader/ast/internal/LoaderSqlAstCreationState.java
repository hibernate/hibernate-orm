package org.hibernate.loader.ast.internal;

import jakarta.annotation.Nullable;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Timeout;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.QueryFlushMode;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.FetchOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.metamodel.mapping.AssociationKey;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ordering.spi.OrderByFragment;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.sql.internal.SqlAstQueryPartProcessingStateImpl;
import org.hibernate.query.sqm.tree.spi.from.SqmFrom;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.sql.ast.spi.creation.FromClauseAccess;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.creation.SqlAstProcessingState;
import org.hibernate.sql.ast.spi.creation.SqlAstQueryPartProcessingState;
import org.hibernate.sql.ast.spi.creation.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.query.from.FromClause;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.ast.spi.query.select.QueryPart;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.Collections.emptyList;

/**
 * Helper used when generating the database-snapshot select query
 */
public class LoaderSqlAstCreationState
		implements SqlAstQueryPartProcessingState, SqlAstCreationState, DomainResultCreationState, QueryOptions {
	public interface FetchProcessor {
		@Nonnull
		ImmutableFetchList visitFetches(@Nonnull FetchParent fetchParent, @Nonnull LoaderSqlAstCreationState creationState);
	}

	private final SqlAliasBaseGenerator sqlAliasBaseManager;
	private final boolean forceIdentifierSelection;
	private final LoadQueryInfluencers loadQueryInfluencers;
	private final SqlAstCreationContext sf;
	private final SqlAstQueryPartProcessingStateImpl processingState;
	private final FromClauseAccess fromClauseAccess;
	private final LockOptions lockOptions;
	private final FetchProcessor fetchProcessor;

	private boolean resolvingCircularFetch;
	@Nullable
	private ForeignKeyDescriptor.Nature currentlyResolvingForeignKeySide;
	private final Set<AssociationKey> visitedAssociationKeys = new HashSet<>();
	@Nullable
	private Map<NavigablePath, FetchOptions> fetchOptions;

	public LoaderSqlAstCreationState(
			@Nonnull QueryPart queryPart,
			@Nonnull SqlAliasBaseGenerator sqlAliasBaseManager,
			@Nonnull FromClauseAccess fromClauseAccess,
			@Nonnull LockOptions lockOptions,
			@Nonnull FetchProcessor fetchProcessor,
			boolean forceIdentifierSelection,
			@Nonnull LoadQueryInfluencers loadQueryInfluencers,
			@Nonnull SqlAstCreationContext sf) {
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
	public void applyOrdering(@Nonnull TableGroup tableGroup, @Nonnull OrderByFragment orderByFragment) {
		final QuerySpec querySpec = getInflightQueryPart().getFirstQuerySpec();
		assert querySpec.isRoot() : "Illegal attempt to apply order-by fragment to a non-root query spec";
		orderByFragment.apply( querySpec, tableGroup, this );
	}

	@Nonnull
	@Override
	public SqlAstCreationContext getCreationContext() {
		return sf;
	}

	@Nonnull
	@Override
	public SqlAstProcessingState getCurrentProcessingState() {
		return this;
	}

	@Nonnull
	@Override
	public QueryPart getInflightQueryPart() {
		return processingState.getInflightQueryPart();
	}

	@Nonnull
	@Override
	public FromClause getFromClause() {
		return processingState.getFromClause();
	}

	@Override
	public void applyPredicate(@Nonnull Predicate predicate) {
		processingState.applyPredicate( predicate );
	}

	@Override
	public void registerTreatedFrom(@Nonnull SqmFrom<?, ?> sqmFrom) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void registerFromUsage(@Nonnull SqmFrom<?, ?> sqmFrom, boolean downgradeTreatUses) {
		throw new UnsupportedOperationException();
	}

	@Nonnull
	@Override
	public Map<SqmFrom<?, ?>, Boolean> getFromRegistrations() {
		return Collections.emptyMap();
	}

	@Nonnull
	@Override
	public SqlExpressionResolver getSqlExpressionResolver() {
		return processingState;
	}

	@Nonnull
	@Override
	public FromClauseAccess getFromClauseAccess() {
		return fromClauseAccess;
	}

	@Nonnull
	@Override
	public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
		return sqlAliasBaseManager;
	}

	@Nonnull
	@Override
	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return loadQueryInfluencers;
	}

	@Override
	public boolean applyOnlyLoadByKeyFilters() {
		return true;
	}

	@Override
	public void registerLockMode(@Nonnull String identificationVariable, @Nonnull LockMode explicitLockMode) {
		throw new UnsupportedOperationException( "Registering lock modes should only be done for result set mappings" );
	}

	@Nonnull
	@Override
	public ImmutableFetchList visitFetches(@Nonnull FetchParent fetchParent) {
		return fetchProcessor.visitFetches( fetchParent, this );
	}

	@Nonnull
	@Override
	public <R> R withNestedFetchParent(@Nonnull FetchParent fetchParent, @Nonnull Function<FetchParent, R> action) {
		final var nestingFetchParent = processingState.getNestingFetchParent();
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

	@Nullable
	@Override
	public ForeignKeyDescriptor.Nature getCurrentlyResolvingForeignKeyPart() {
		return currentlyResolvingForeignKeySide;
	}

	@Override
	public void setCurrentlyResolvingForeignKeyPart(@Nonnull ForeignKeyDescriptor.Nature currentlyResolvingForeignKeySide) {
		this.currentlyResolvingForeignKeySide = currentlyResolvingForeignKeySide;
	}

	@Override
	public void registerFetchOptions(@Nonnull NavigablePath fetchablePath, @Nonnull FetchOptions fetchOptions) {
		if ( fetchOptions.hasOptions() ) {
			if ( this.fetchOptions == null ) {
				this.fetchOptions = new HashMap<>();
			}
			this.fetchOptions.put( fetchablePath, fetchOptions );
		}
	}

	@Nonnull
	@Override
	public FetchOptions getFetchOptions(@Nonnull NavigablePath fetchablePath) {
		return fetchOptions == null ? FetchOptions.NONE : fetchOptions.getOrDefault( fetchablePath, FetchOptions.NONE );
	}

	@Override
	public boolean forceIdentifierSelection() {
		return forceIdentifierSelection;
	}

	@Nonnull
	@Override
	public SqlAstCreationState getSqlAstCreationState() {
		return this;
	}

	@Override
	public boolean registerVisitedAssociationKey(@Nonnull AssociationKey associationKey) {
		return visitedAssociationKeys.add( associationKey );
	}

	@Override
	public void removeVisitedAssociationKey(@Nonnull AssociationKey associationKey) {
		visitedAssociationKeys.remove( associationKey );
	}

	@Override
	public boolean isAssociationKeyVisited(@Nonnull AssociationKey associationKey) {
		return visitedAssociationKeys.contains( associationKey );
	}

	@Override
	public boolean isRegisteringVisitedAssociationKeys(){
		return true;
	}

	@Nonnull
	@Override
	public ModelPart resolveModelPart(@Nonnull NavigablePath navigablePath) {
		// for now, let's assume that the navigable-path refers to TableGroup
		return fromClauseAccess.findTableGroup( navigablePath ).getModelPart();
	}

	@Nullable
	@Override
	public SqlAstProcessingState getParentState() {
		return null;
	}

	@Nullable
	@Override
	public Timeout getTimeout() {
		return null;
	}

	@Override
	@Nonnull
	public QueryFlushMode getQueryFlushMode() {
		return QueryFlushMode.DEFAULT;
	}

	@Nullable
	@Override
	public Boolean isReadOnly() {
		return null;
	}

	@Nullable
	@Override
	public AppliedGraph getAppliedGraph() {
		// todo (6.0) : use this from the "load settings" (Hibernate method args, map passed to JPA methods)
		//   the legacy approach is to temporarily set this on the Session's "load query influencers"
		return null;
	}

	@Nullable
	@Override
	public TupleTransformer<?> getTupleTransformer() {
		return null;
	}

	@Nullable
	@Override
	public ResultListTransformer<?> getResultListTransformer() {
		return null;
	}

	@Nullable
	@Override
	public Boolean isResultCachingEnabled() {
		return false;
	}

	@Nullable
	@Override
	public Boolean getQueryPlanCachingEnabled() {
		return null;
	}

	@Nullable
	@Override
	public CacheRetrieveMode getCacheRetrieveMode() {
		return CacheRetrieveMode.BYPASS;
	}

	@Nullable
	@Override
	public CacheStoreMode getCacheStoreMode() {
		return CacheStoreMode.BYPASS;
	}

	@Nullable
	@Override
	public String getResultCacheRegionName() {
		return null;
	}

	@Override
	@Nonnull
	public LockOptions getLockOptions() {
		return lockOptions;
	}

	@Nullable
	@Override
	public String getComment() {
		return null;
	}

	@Override
	@Nonnull
	public List<String> getDatabaseHints() {
		return emptyList();
	}

	@Nullable
	@Override
	public Integer getFetchSize() {
		return null;
	}

	@Override
	@Nonnull
	public Limit getLimit() {
		return Limit.NONE;
	}

	@Nullable
	@Override
	public Set<String> getEnabledFetchProfiles() {
		return null;
	}

	@Nullable
	@Override
	public Set<String> getDisabledFetchProfiles() {
		return null;
	}
}
