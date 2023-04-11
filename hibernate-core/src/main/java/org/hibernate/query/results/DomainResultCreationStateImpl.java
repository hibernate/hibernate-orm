/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import java.util.AbstractMap;
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
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.internal.BasicValuedCollectionPart;
import org.hibernate.metamodel.mapping.internal.CaseStatementDiscriminatorMappingImpl;
import org.hibernate.metamodel.mapping.internal.SingleAttributeIdentifierMapping;
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
	private final Stack<Map.Entry> relativePathStack = new StandardStack<>( Map.Entry.class );
	private Map<String, LockMode> registeredLockModes;
	private boolean processingKeyFetches = false;
	private boolean resolvingCircularFetch;
	private ForeignKeyDescriptor.Nature currentlyResolvingForeignKeySide;

	public DomainResultCreationStateImpl(
			String stateIdentifier,
			JdbcValuesMetadata jdbcResultsMetadata,
			Map<String, Map<String, DynamicFetchBuilderLegacy>> legacyFetchBuilders,
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
		if ( ResultsLogger.DEBUG_ENABLED ) {
			ResultsLogger.RESULTS_LOGGER.debugf( "Disallowing positional selections : %s", stateIdentifier );
		}
		this.allowPositionalSelections = false;
	}

	public JdbcValuesMetadata getJdbcResultsMetadata() {
		return jdbcResultsMetadata;
	}

	public Map.Entry<String, NavigablePath> getCurrentRelativePath() {
		return relativePathStack.getCurrent();
	}

	public void pushExplicitFetchMementoResolver(Function<String, FetchBuilder> resolver) {
		fetchBuilderResolverStack.push( resolver );
	}

	public Function<String, FetchBuilder> getCurrentExplicitFetchMementoResolver() {
		return fetchBuilderResolverStack.getCurrent();
	}

	public Function<String, FetchBuilder> popExplicitFetchMementoResolver() {
		return fetchBuilderResolverStack.pop();
	}

	@SuppressWarnings( "unused" )
	public void withExplicitFetchMementoResolver(Function<String, FetchBuilder> resolver, Runnable runnable) {
		pushExplicitFetchMementoResolver( resolver );
		try {
			runnable.run();
		}
		finally {
			final Function<String, FetchBuilder> popped = popExplicitFetchMementoResolver();
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
		else if ( created instanceof ColumnReference ) {
			final ColumnReference columnReference = (ColumnReference) created;
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
		private final Map<String,Map<String, DynamicFetchBuilderLegacy>> legacyFetchBuilders;

		public LegacyFetchResolverImpl(Map<String, Map<String, DynamicFetchBuilderLegacy>> legacyFetchBuilders) {
			this.legacyFetchBuilders = legacyFetchBuilders;
		}

		@Override
		public DynamicFetchBuilderLegacy resolve(String ownerTableAlias, String fetchedPartPath) {
			if ( legacyFetchBuilders == null ) {
				return null;
			}

			final Map<String, DynamicFetchBuilderLegacy> fetchBuilders = legacyFetchBuilders.get( ownerTableAlias );
			if ( fetchBuilders == null ) {
				return null;
			}

			return fetchBuilders.get( fetchedPartPath );
		}
	}

	@Override
	public ImmutableFetchList visitNestedFetches(FetchParent fetchParent) {
		final FetchParent oldNestingFetchParent = this.nestingFetchParent;
		this.nestingFetchParent = fetchParent;
		final ImmutableFetchList fetches = visitFetches( fetchParent );
		this.nestingFetchParent = oldNestingFetchParent;
		return fetches;
	}

	@Override
	public Fetch visitIdentifierFetch(EntityResultGraphNode fetchParent) {
		final EntityValuedModelPart entityValuedFetchable = fetchParent.getEntityValuedModelPart();
		final EntityIdentifierMapping identifierMapping = entityValuedFetchable.getEntityMappingType().getIdentifierMapping();
		final String identifierAttributeName = attributeName( identifierMapping );
		final Map.Entry<String, NavigablePath> oldEntry = relativePathStack.getCurrent();
		final String fullPath;
		if ( identifierMapping instanceof NonAggregatedIdentifierMapping ) {
			fullPath = oldEntry == null ? "" : oldEntry.getKey();
		}
		else {
			fullPath = oldEntry == null ?
					identifierAttributeName :
					oldEntry.getKey() + "." + identifierAttributeName;
		}

		final Fetchable fetchable = (Fetchable) identifierMapping;
		final FetchBuilder explicitFetchBuilder = (FetchBuilder) fetchBuilderResolverStack
				.getCurrent()
				.apply( fullPath );
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
		if ( identifierMapping instanceof CompositeIdentifierMapping ) {
			relativePathStack.push( new AbstractMap.SimpleEntry<>( fullPath, fetchPath ) );
		}

		try {
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
			if ( identifierMapping instanceof CompositeIdentifierMapping ) {
				this.relativePathStack.pop();
			}
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
			Map.Entry<String, NavigablePath> currentEntry;
			if ( relativePathStack.isEmpty() ) {
				currentEntry = new AbstractMap.SimpleEntry<>(
						getRelativePath( "", fetchable ),
						new NavigablePath( fetchableName )
				);
			}
			else {
				final Map.Entry<String, NavigablePath> oldEntry = relativePathStack.getCurrent();
				final String key = oldEntry.getKey();
				currentEntry = new AbstractMap.SimpleEntry<>(
						getRelativePath( key, fetchable ),
						oldEntry.getValue().append( fetchableName )
				);
			}
			// todo (6.0): figure out if we can somehow create the navigable paths in a better way
			final String fullPath = currentEntry.getKey();
			FetchBuilder explicitFetchBuilder = (FetchBuilder) fetchBuilderResolverStack
					.getCurrent()
					.apply( fullPath );
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
			if ( fetchable instanceof Association && fetchable.getMappedFetchOptions().getTiming() == FetchTiming.DELAYED ) {
				final Association association = (Association) fetchable;
				final ForeignKeyDescriptor foreignKeyDescriptor = association.getForeignKeyDescriptor();

				final String partName = attributeName(
						foreignKeyDescriptor.getSide( association.getSideNature().inverse() ).getModelPart()
				);

				// If there are no fetch builders for this association, we only want to fetch the FK
				if ( explicitFetchBuilder == null && fetchBuilderLegacy == null && partName != null ) {
					currentEntry = new AbstractMap.SimpleEntry<>(
							currentEntry.getKey() + "." + partName,
							currentEntry.getValue().append( partName )
					);
					explicitFetchBuilder = (FetchBuilder) fetchBuilderResolverStack
							.getCurrent()
							.apply( currentEntry.getKey() );
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
			relativePathStack.push( currentEntry );
			try {

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
			}
			finally {
				relativePathStack.pop();
			}

		};
	}

	private String getRelativePath(String oldEntry, Fetchable fetchable) {
		if ( fetchable instanceof AttributeMapping || fetchable instanceof SingleAttributeIdentifierMapping || fetchable instanceof BasicValuedCollectionPart ) {
			if ( !oldEntry.equals( "" ) ) {
				return oldEntry + '.' + fetchable.getFetchableName();
			}
			return fetchable.getFetchableName();
		}
		return oldEntry;
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
