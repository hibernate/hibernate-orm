/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.Internal;
import org.hibernate.LockMode;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.metamodel.mapping.Association;
import org.hibernate.metamodel.mapping.AssociationKey;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.CaseStatementDiscriminatorMappingImpl;
import org.hibernate.spi.EntityIdentifierNavigablePath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.query.results.dynamic.LegacyFetchResolver;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.ResultsLogger;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.FetchableContainer;
import org.hibernate.sql.results.graph.entity.EntityResultGraphNode;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.results.ResultsHelper.attributeName;

/**
 * @author Steve Ebersole
 */
@Internal
public class DomainResultCreationStateImpl
		implements DomainResultCreationState, SqlAstCreationState, SqlAstProcessingState, SqlExpressionResolver {

	private static final String DISCRIMINATOR_ALIAS = "clazz_";
	private final String stateIdentifier;
	private final FromClauseAccessImpl fromClauseAccess;

	private final JdbcValuesMetadata jdbcResultsMetadata;
	private final Consumer<SqlSelection> sqlSelectionConsumer;
	private final LoadQueryInfluencers loadQueryInfluencers;
	private final Map<ColumnReferenceKey, ResultSetMappingSqlSelection> sqlSelectionMap = new HashMap<>();
	private boolean allowPositionalSelections = true;

	private final SqlAliasBaseManager sqlAliasBaseManager;

	private final LegacyFetchResolverImpl legacyFetchResolver;
	private final SessionFactoryImplementor sessionFactory;

	private final Stack<Function> fetchBuilderResolverStack = new StandardStack<>( Function.class, fetchableName -> null );
	private Map<String, LockMode> registeredLockModes;
	private boolean processingKeyFetches = false;
	private boolean resolvingCircularFetch;
	private ForeignKeyDescriptor.Nature currentlyResolvingForeignKeySide;

	public DomainResultCreationStateImpl(
			String stateIdentifier,
			JdbcValuesMetadata jdbcResultsMetadata,
			Map<Fetchable, Map<Fetchable, DynamicFetchBuilderLegacy>> legacyFetchBuilders,
			Consumer<SqlSelection> sqlSelectionConsumer,
			LoadQueryInfluencers loadQueryInfluencers,
			SessionFactoryImplementor sessionFactory) {
		this.stateIdentifier = stateIdentifier;
		this.jdbcResultsMetadata = jdbcResultsMetadata;
		this.sqlSelectionConsumer = sqlSelectionConsumer;
		this.loadQueryInfluencers = loadQueryInfluencers;
		this.fromClauseAccess = new FromClauseAccessImpl();
		this.sqlAliasBaseManager = new SqlAliasBaseManager();

		this.legacyFetchResolver = new LegacyFetchResolverImpl( legacyFetchBuilders );

		this.sessionFactory = sessionFactory;
	}

	public LegacyFetchResolver getLegacyFetchResolver() {
		return legacyFetchResolver;
	}

	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	public int getNumberOfProcessedSelections() {
		return sqlSelectionMap.size();
	}

	public boolean arePositionalSelectionsAllowed() {
		return allowPositionalSelections;
	}

	public void disallowPositionalSelections() {
		ResultsLogger.RESULTS_LOGGER.debugf( "Disallowing positional selections : %s", stateIdentifier );
		this.allowPositionalSelections = false;
	}

	public JdbcValuesMetadata getJdbcResultsMetadata() {
		return jdbcResultsMetadata;
	}

	public void pushExplicitFetchMementoResolver(Function<Fetchable, FetchBuilder> resolver) {
		fetchBuilderResolverStack.push( resolver );
	}

	public Function<Fetchable, FetchBuilder> getCurrentExplicitFetchMementoResolver() {
		return fetchBuilderResolverStack.getCurrent();
	}

	public Function<Fetchable, FetchBuilder> popExplicitFetchMementoResolver() {
		return fetchBuilderResolverStack.pop();
	}

	@SuppressWarnings( "unused" )
	public void withExplicitFetchMementoResolver(Function<Fetchable, FetchBuilder> resolver, Runnable runnable) {
		pushExplicitFetchMementoResolver( resolver );
		try {
			runnable.run();
		}
		finally {
			final Function<Fetchable, FetchBuilder> popped = popExplicitFetchMementoResolver();
			assert popped == resolver;
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// DomainResultCreationState

	@Override
	public FromClauseAccessImpl getFromClauseAccess() {
		return fromClauseAccess;
	}

	@Override
	public DomainResultCreationStateImpl getSqlAstCreationState() {
		return this;
	}

	@Override
	public SqlAliasBaseManager getSqlAliasBaseManager() {
		return sqlAliasBaseManager;
	}

	@Override
	public boolean forceIdentifierSelection() {
		return true;
	}

	@Override
	public boolean registerVisitedAssociationKey(AssociationKey associationKey) {
		return false;
	}

	@Override
	public boolean isAssociationKeyVisited(AssociationKey associationKey) {
		return false;
	}

	@Override
	public ModelPart resolveModelPart(NavigablePath navigablePath) {
		final TableGroup tableGroup = fromClauseAccess.findTableGroup( navigablePath );
		if ( tableGroup != null ) {
			return tableGroup.getModelPart();
		}

		if ( navigablePath.getParent() != null ) {
			final TableGroup parentTableGroup = fromClauseAccess.findTableGroup( navigablePath.getParent() );
			if ( parentTableGroup != null ) {
				return parentTableGroup.getModelPart().findSubPart( navigablePath.getLocalName(), null );
			}
		}

		return null;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlAstCreationState

	@Override
	public DomainResultCreationStateImpl getSqlExpressionResolver() {
		return getCurrentProcessingState();
	}

	@Override
	public void registerLockMode(String identificationVariable, LockMode explicitLockMode) {
		if (registeredLockModes == null ) {
			registeredLockModes = new HashMap<>();
		}
		registeredLockModes.put( identificationVariable, explicitLockMode );
	}

	public Map<String, LockMode> getRegisteredLockModes() {
		return registeredLockModes;
	}

	@Override
	public DomainResultCreationStateImpl getCurrentProcessingState() {
		return this;
	}

	public SqlAstCreationContext getCreationContext() {
		return getSessionFactory();
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

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlAstProcessingState

	@Override
	public SqlAstProcessingState getParentState() {
		return null;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlExpressionResolver

	private FetchParent nestingFetchParent;

	@Override
	public Expression resolveSqlExpression(
			ColumnReferenceKey key,
			Function<SqlAstProcessingState, Expression> creator) {
		final ResultSetMappingSqlSelection existing = sqlSelectionMap.get( key );
		if ( existing != null ) {
			return existing;
		}

		final Expression created = creator.apply( this );

		if ( created instanceof ResultSetMappingSqlSelection ) {
			sqlSelectionMap.put( key, (ResultSetMappingSqlSelection) created );
			sqlSelectionConsumer.accept( (ResultSetMappingSqlSelection) created );
		}
		else if ( created instanceof ColumnReference columnReference) {
			final String selectableName = columnReference.getSelectableName();
			final int valuesArrayPosition;
			if ( nestingFetchParent != null ) {
				valuesArrayPosition = nestingFetchParent.getReferencedMappingType().getSelectableIndex( selectableName );
			}
			else {
				final int jdbcPosition = jdbcResultsMetadata.resolveColumnPosition( selectableName );
				valuesArrayPosition = ResultsHelper.jdbcPositionToValuesArrayPosition( jdbcPosition );
			}

			final ResultSetMappingSqlSelection sqlSelection = new ResultSetMappingSqlSelection(
					valuesArrayPosition,
					columnReference.getJdbcMapping()
			);

			sqlSelectionMap.put( key, sqlSelection );
			sqlSelectionConsumer.accept( sqlSelection );

			return sqlSelection;
		}
		else if ( created instanceof CaseStatementDiscriminatorMappingImpl.CaseStatementDiscriminatorExpression ) {
			final int valuesArrayPosition;
			if ( nestingFetchParent != null ) {
				valuesArrayPosition = nestingFetchParent.getReferencedMappingType().getSelectableIndex( DISCRIMINATOR_ALIAS );
			}
			else {
				final int jdbcPosition = jdbcResultsMetadata.resolveColumnPosition( DISCRIMINATOR_ALIAS );
				valuesArrayPosition = ResultsHelper.jdbcPositionToValuesArrayPosition( jdbcPosition );
			}

			final ResultSetMappingSqlSelection sqlSelection = new ResultSetMappingSqlSelection(
					valuesArrayPosition,
					created.getExpressionType().getSingleJdbcMapping()
			);

			sqlSelectionMap.put( key, sqlSelection );
			sqlSelectionConsumer.accept( sqlSelection );

			return sqlSelection;
		}

		return created;
	}

	@Override
	public SqlSelection resolveSqlSelection(
			Expression expression,
			JavaType<?> javaType,
			FetchParent fetchParent, TypeConfiguration typeConfiguration) {
		if ( expression == null ) {
			throw new IllegalArgumentException( "Expression cannot be null" );
		}
		assert expression instanceof ResultSetMappingSqlSelection;
		return (SqlSelection) expression;
	}

	private static class LegacyFetchResolverImpl implements LegacyFetchResolver {
		private final Map<Fetchable,Map<Fetchable, DynamicFetchBuilderLegacy>> legacyFetchBuilders;

		public LegacyFetchResolverImpl(Map<Fetchable, Map<Fetchable, DynamicFetchBuilderLegacy>> legacyFetchBuilders) {
			this.legacyFetchBuilders = legacyFetchBuilders;
		}

		@Override
		public DynamicFetchBuilderLegacy resolve(String ownerTableAlias, String fetchedPartPath) {
			if ( legacyFetchBuilders == null ) {
				return null;
			}

			final Map<Fetchable, DynamicFetchBuilderLegacy> fetchBuilders = legacyFetchBuilders.get( ownerTableAlias );
			if ( fetchBuilders == null ) {
				return null;
			}

			return fetchBuilders.get( fetchedPartPath );
		}
	}

	@Override
	public <R> R withNestedFetchParent(FetchParent fetchParent, Function<FetchParent, R> action) {
		final FetchParent oldNestingFetchParent = this.nestingFetchParent;
		this.nestingFetchParent = fetchParent;
		final R result = action.apply( fetchParent );
		this.nestingFetchParent = oldNestingFetchParent;
		return result;
	}

	@Override
	public Fetch visitIdentifierFetch(EntityResultGraphNode fetchParent) {
		final EntityValuedModelPart parentModelPart = fetchParent.getEntityValuedModelPart();
		final EntityIdentifierMapping identifierMapping = parentModelPart.getEntityMappingType().getIdentifierMapping();
		final String identifierAttributeName = attributeName( identifierMapping );

		final FetchBuilder explicitFetchBuilder = (FetchBuilder) fetchBuilderResolverStack
				.getCurrent()
				.apply( identifierMapping );
		DynamicFetchBuilderLegacy fetchBuilderLegacy;
		if ( explicitFetchBuilder == null ) {
			fetchBuilderLegacy = legacyFetchResolver.resolve(
					fromClauseAccess.findTableGroup( fetchParent.getNavigablePath() )
							.getPrimaryTableReference()
							.getIdentificationVariable(),
					identifierAttributeName
			);
		}
		else {
			fetchBuilderLegacy = null;
		}

		final EntityIdentifierNavigablePath fetchPath = new EntityIdentifierNavigablePath(
				fetchParent.getNavigablePath(),
				identifierAttributeName
		);

		final boolean processingKeyFetches = this.processingKeyFetches;
		this.processingKeyFetches = true;

		try {
			final FetchBuilder fetchBuilder;
			if ( explicitFetchBuilder != null ) {
				fetchBuilder = explicitFetchBuilder;
			}
			else {
				if ( fetchBuilderLegacy == null ) {
					fetchBuilder = Builders.implicitFetchBuilder( fetchPath, identifierMapping, this );
				}
				else {
					fetchBuilder = fetchBuilderLegacy;
				}
			}
			return fetchBuilder.buildFetch(
					fetchParent,
					fetchPath,
					jdbcResultsMetadata,
					(s, s2) -> {
						throw new UnsupportedOperationException();
					},
					this
			);
		}
		finally {
			this.processingKeyFetches = processingKeyFetches;
		}
	}

	@Override
	public ImmutableFetchList visitFetches(FetchParent fetchParent) {
		final FetchableContainer fetchableContainer = fetchParent.getReferencedMappingContainer();
		final ImmutableFetchList.Builder fetches = new ImmutableFetchList.Builder( fetchableContainer );
		final Consumer<Fetchable> fetchableConsumer = createFetchableConsumer( fetchParent, fetches );
		fetchableContainer.visitKeyFetchables( fetchableConsumer, null );
		fetchableContainer.visitFetchables( fetchableConsumer, null );
		return fetches.build();
	}

	private Consumer<Fetchable> createFetchableConsumer(FetchParent fetchParent, ImmutableFetchList.Builder fetches) {
		return fetchable -> {
			if ( !fetchable.isSelectable() ) {
				return;
			}
			final String fetchableName = fetchable.getFetchableName();
			FetchBuilder explicitFetchBuilder = (FetchBuilder) fetchBuilderResolverStack
					.getCurrent()
					.apply( fetchable );
			DynamicFetchBuilderLegacy fetchBuilderLegacy;
			if ( explicitFetchBuilder == null ) {
				fetchBuilderLegacy = legacyFetchResolver.resolve(
						fromClauseAccess.findTableGroup( fetchParent.getNavigablePath() )
								.getPrimaryTableReference()
								.getIdentificationVariable(),
						fetchableName
				);
			}
			else {
				fetchBuilderLegacy = null;
			}
			if ( fetchable instanceof Association association && fetchable.getMappedFetchOptions().getTiming() == FetchTiming.DELAYED ) {
				final ForeignKeyDescriptor foreignKeyDescriptor = association.getForeignKeyDescriptor();
				// If there are no fetch builders for this association, we only want to fetch the FK
				if ( explicitFetchBuilder == null && fetchBuilderLegacy == null  ) {
					explicitFetchBuilder = (FetchBuilder) fetchBuilderResolverStack
							.getCurrent()
							.apply( foreignKeyDescriptor.getSide( association.getSideNature().inverse() ).getModelPart() );
					if ( explicitFetchBuilder == null ) {
						fetchBuilderLegacy = legacyFetchResolver.resolve(
								fromClauseAccess.findTableGroup( fetchParent.getNavigablePath() )
										.getPrimaryTableReference()
										.getIdentificationVariable(),
								fetchableName
						);
					}
				}
			}
			final NavigablePath fetchPath = fetchParent.resolveNavigablePath( fetchable );
			final FetchBuilder fetchBuilder;
			if ( explicitFetchBuilder != null ) {
				fetchBuilder = explicitFetchBuilder;
			}
			else {
				if ( fetchBuilderLegacy == null ) {
					fetchBuilder = Builders.implicitFetchBuilder( fetchPath, fetchable, this );
				}
				else {
					fetchBuilder = fetchBuilderLegacy;
				}
			}
			final Fetch fetch = fetchBuilder.buildFetch(
					fetchParent,
					fetchPath,
					jdbcResultsMetadata,
					(s, s2) -> {
						throw new UnsupportedOperationException();
					},
					this
			);
			fetches.add( fetch );
		};
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

}
