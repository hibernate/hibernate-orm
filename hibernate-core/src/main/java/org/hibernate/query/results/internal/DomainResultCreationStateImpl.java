/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Internal;
import org.hibernate.LockMode;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.metamodel.mapping.Association;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.CaseStatementDiscriminatorMappingImpl.CaseStatementDiscriminatorExpression;
import org.hibernate.query.results.spi.FetchBuilder;
import org.hibernate.query.results.spi.LegacyFetchBuilder;
import org.hibernate.query.results.spi.ResultSetMapping;
import org.hibernate.spi.EntityIdentifierNavigablePath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.entity.EntityResultGraphNode;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.hibernate.query.results.internal.Builders.implicitFetchBuilder;
import static org.hibernate.query.results.internal.ResultsHelper.attributeName;
import static org.hibernate.query.results.internal.ResultsHelper.jdbcPositionToValuesArrayPosition;
import static org.hibernate.sql.results.ResultsLogger.RESULTS_LOGGER;

/**
 * Central implementation of {@linkplain DomainResultCreationState},
 * {@linkplain SqlAstCreationState}, {@linkplain SqlAstProcessingState} and
 * {@linkplain SqlExpressionResolver} used while building
 * {@linkplain ResultSetMapping} references.
 *
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

	private final LegacyFetchResolver legacyFetchResolver;
	private final SessionFactoryImplementor sessionFactory;

	private final Stack<Function<Fetchable, FetchBuilder>> fetchBuilderResolverStack = new StandardStack<>( fetchableName -> null );
	private Map<String, LockMode> registeredLockModes;
	private boolean processingKeyFetches = false;
	private boolean resolvingCircularFetch;
	private ForeignKeyDescriptor.Nature currentlyResolvingForeignKeySide;
	private final boolean isProcedureOrNativeQuery;

	public DomainResultCreationStateImpl(
			String stateIdentifier,
			JdbcValuesMetadata jdbcResultsMetadata,
			Map<String, Map<Fetchable, LegacyFetchBuilder>> legacyFetchBuilders,
			Consumer<SqlSelection> sqlSelectionConsumer,
			LoadQueryInfluencers loadQueryInfluencers,
			boolean isProcedureOrNativeQuery,
			SessionFactoryImplementor sessionFactory) {
		this.stateIdentifier = stateIdentifier;
		this.jdbcResultsMetadata = jdbcResultsMetadata;
		this.sqlSelectionConsumer = sqlSelectionConsumer;
		this.loadQueryInfluencers = loadQueryInfluencers;
		this.fromClauseAccess = new FromClauseAccessImpl();
		this.sqlAliasBaseManager = new SqlAliasBaseManager();

		this.legacyFetchResolver = new LegacyFetchResolver( legacyFetchBuilders );

		this.sessionFactory = sessionFactory;

		this.isProcedureOrNativeQuery = isProcedureOrNativeQuery;
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
		RESULTS_LOGGER.debugf( "Disallowing positional selections: %s", stateIdentifier );
		this.allowPositionalSelections = false;
	}

	public JdbcValuesMetadata getJdbcResultsMetadata() {
		return jdbcResultsMetadata;
	}

	public void pushExplicitFetchMementoResolver(Function<Fetchable, FetchBuilder> resolver) {
		fetchBuilderResolverStack.push( resolver );
	}

	public Function<Fetchable, FetchBuilder> getCurrentExplicitFetchMementoResolver() {
		return currentFetchBuilderResolver();
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
			final var popped = popExplicitFetchMementoResolver();
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
	public ModelPart resolveModelPart(NavigablePath navigablePath) {
		final var tableGroup = fromClauseAccess.findTableGroup( navigablePath );
		if ( tableGroup != null ) {
			return tableGroup.getModelPart();
		}

		final var parentPath = navigablePath.getParent();
		if ( parentPath != null ) {
			final var parentTableGroup = fromClauseAccess.findTableGroup( parentPath );
			if ( parentTableGroup != null ) {
				return parentTableGroup.getModelPart()
						.findSubPart( navigablePath.getLocalName(), null );
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
		if ( registeredLockModes == null ) {
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
		return sessionFactory.getSqlTranslationEngine();
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
		final var existing = sqlSelectionMap.get( key );
		return existing != null ? existing : createSqlSelection( key, creator );
	}

	private Expression createSqlSelection(
			ColumnReferenceKey key,
			Function<SqlAstProcessingState, Expression> creator) {
		final var created = creator.apply( this );

		final ResultSetMappingSqlSelection sqlSelection;
		if ( created instanceof ResultSetMappingSqlSelection resultSetMappingSqlSelection ) {
			sqlSelection = resultSetMappingSqlSelection;
		}
		else if ( created instanceof ColumnReference columnReference) {
			sqlSelection =
					new ResultSetMappingSqlSelection(
							valuesArrayPosition( columnReference.getSelectableName() ),
							columnReference.getJdbcMapping()
					);
		}
		else if ( created instanceof CaseStatementDiscriminatorExpression ) {
			sqlSelection = new ResultSetMappingSqlSelection(
					valuesArrayPosition( DISCRIMINATOR_ALIAS ),
					created.getExpressionType().getSingleJdbcMapping()
			);
		}
		else {
			return created;
		}

		sqlSelectionMap.put( key, sqlSelection );
		sqlSelectionConsumer.accept( sqlSelection );
		return sqlSelection;
	}

	private int valuesArrayPosition(String selectableName) {
		return nestingFetchParent != null
				? nestingFetchParent.getReferencedMappingType().getSelectableIndex( selectableName )
				: jdbcPositionToValuesArrayPosition( jdbcResultsMetadata.resolveColumnPosition( selectableName ) );
	}

	@Override
	public SqlSelection resolveSqlSelection(
			Expression expression,
			JavaType<?> javaType,
			FetchParent fetchParent, TypeConfiguration typeConfiguration) {
		if ( expression == null ) {
			throw new IllegalArgumentException( "Expression cannot be null" );
		}
		if ( !(expression instanceof ResultSetMappingSqlSelection sqlSelection) ) {
			throw new IllegalArgumentException( "Expression must be a ResultSetMappingSqlSelection" );
		}
		return sqlSelection;
	}

	private static class LegacyFetchResolver {
		private final Map<String,Map<Fetchable, LegacyFetchBuilder>> legacyFetchBuilders;

		public LegacyFetchResolver(Map<String, Map<Fetchable, LegacyFetchBuilder>> legacyFetchBuilders) {
			this.legacyFetchBuilders = legacyFetchBuilders;
		}

		public LegacyFetchBuilder resolve(String ownerTableAlias, Fetchable fetchedPart) {
			if ( legacyFetchBuilders == null ) {
				return null;
			}
			else {
				final var fetchBuilders = legacyFetchBuilders.get( ownerTableAlias );
				return fetchBuilders == null ? null : fetchBuilders.get( fetchedPart );
			}
		}
	}

	@Override
	public <R> R withNestedFetchParent(FetchParent fetchParent, Function<FetchParent, R> action) {
		final var oldNestingFetchParent = this.nestingFetchParent;
		this.nestingFetchParent = fetchParent;
		final R result = action.apply( fetchParent );
		this.nestingFetchParent = oldNestingFetchParent;
		return result;
	}

	private @NonNull FetchBuilder fetchBuilder(
			Fetchable fetchable,
			FetchBuilder explicitFetchBuilder,
			LegacyFetchBuilder fetchBuilderLegacy,
			NavigablePath fetchPath) {
		if ( explicitFetchBuilder != null ) {
			return explicitFetchBuilder;
		}
		else if ( fetchBuilderLegacy != null ) {
			return fetchBuilderLegacy;
		}
		else {
			return implicitFetchBuilder( fetchPath, fetchable, this );
		}
	}

	private @Nullable LegacyFetchBuilder legacyFetchBuilder(
			Fetchable fetchable, FetchParent fetchParent,
			FetchBuilder explicitFetchBuilder) {
		if ( explicitFetchBuilder == null ) {
			return legacyFetchResolver.resolve(
					fromClauseAccess.findTableGroup( fetchParent.getNavigablePath() )
							.getPrimaryTableReference()
							.getIdentificationVariable(),
					fetchable
			);
		}
		else {
			return null;
		}
	}

	@Override
	public Fetch visitIdentifierFetch(EntityResultGraphNode fetchParent) {
		final var identifierMapping =
				fetchParent.getEntityValuedModelPart()
						.getEntityMappingType().getIdentifierMapping();

		final var explicitFetchBuilder =
				currentFetchBuilderResolver().apply( identifierMapping );

		final var fetchBuilderLegacy =
				legacyFetchBuilder( identifierMapping, fetchParent, explicitFetchBuilder );

		final var fetchPath =
				new EntityIdentifierNavigablePath(
						fetchParent.getNavigablePath(),
						attributeName( identifierMapping )
				);

		final boolean processingKeyFetches = this.processingKeyFetches;
		this.processingKeyFetches = true;

		try {
			return fetchBuilder( identifierMapping, explicitFetchBuilder, fetchBuilderLegacy, fetchPath )
					.buildFetch( fetchParent, fetchPath, jdbcResultsMetadata, this );
		}
		finally {
			this.processingKeyFetches = processingKeyFetches;
		}
	}

	@Override
	public ImmutableFetchList visitFetches(FetchParent fetchParent) {
		final var fetchableContainer = fetchParent.getReferencedMappingContainer();
		final var fetches = new ImmutableFetchList.Builder( fetchableContainer );
		final var fetchableConsumer = createFetchableConsumer( fetchParent, fetches );
		fetchableContainer.visitKeyFetchables( fetchableConsumer, null );
		fetchableContainer.visitFetchables( fetchableConsumer, null );
		return fetches.build();
	}

	private Consumer<Fetchable> createFetchableConsumer(FetchParent fetchParent, ImmutableFetchList.Builder fetches) {
		return fetchable -> {
			if ( !fetchable.isSelectable() ) {
				return;
			}
			FetchBuilder explicitFetchBuilder = currentFetchBuilderResolver().apply( fetchable );
			LegacyFetchBuilder fetchBuilderLegacy =
					legacyFetchBuilder( fetchable, fetchParent, explicitFetchBuilder );
			if ( fetchable instanceof Association association
					&& fetchable.getMappedFetchOptions().getTiming() == FetchTiming.DELAYED ) {
				// If there are no fetch builders for this association, we only want to fetch the FK
				if ( explicitFetchBuilder == null && fetchBuilderLegacy == null  ) {
					final var modelPart = (Fetchable)
							association.getForeignKeyDescriptor()
									.getSide( association.getSideNature().inverse() )
									.getModelPart();
					explicitFetchBuilder = currentFetchBuilderResolver().apply( modelPart );
					if ( explicitFetchBuilder == null ) {
						fetchBuilderLegacy =
								legacyFetchBuilder( fetchable, fetchParent, null );
					}
				}
			}
			final var fetchPath = fetchParent.resolveNavigablePath( fetchable );
			final var fetch =
					fetchBuilder( fetchable, explicitFetchBuilder, fetchBuilderLegacy, fetchPath )
							.buildFetch( fetchParent, fetchPath, jdbcResultsMetadata, this );
			fetches.add( fetch );
		};
	}

	private Function<Fetchable, FetchBuilder> currentFetchBuilderResolver() {
		return fetchBuilderResolverStack.getCurrent();
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
	public boolean isProcedureOrNativeQuery() {
		return isProcedureOrNativeQuery;
	}
}
