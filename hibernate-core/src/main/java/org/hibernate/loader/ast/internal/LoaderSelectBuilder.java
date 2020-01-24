/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.loader.ast.spi.Loadable;
import org.hibernate.loader.ast.spi.Loader;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.SimpleForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ordering.OrderByFragment;
import org.hibernate.query.ComparisonOperator;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.spi.SimpleFromClauseAccessImpl;
import org.hibernate.sql.ast.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.FetchableContainer;
import org.hibernate.sql.results.graph.collection.internal.CollectionDomainResult;
import org.hibernate.sql.results.internal.SqlSelectionImpl;

import org.jboss.logging.Logger;

import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;

/**
 * Builder for SQL AST trees used by {@link Loader} implementations.
 *
 * @author Steve Ebersole
 */
public class LoaderSelectBuilder {
	private static final Logger log = Logger.getLogger( LoaderSelectBuilder.class );

	/**
	 * Create a SQL AST select-statement based on matching one-or-more keys
	 *
	 * @param loadable The root Loadable
	 * @param partsToSelect Parts of the Loadable to select.  Null/empty indicates to select the Loadable itself
	 * @param restrictedPart Part to base the where-clause restriction on
	 * @param cachedDomainResult DomainResult to be used.  Null indicates to generate the DomainResult
	 * @param numberOfKeysToLoad How many keys should be accounted for in the where-clause restriction?
	 * @param loadQueryInfluencers Any influencers (entity graph, fetch profile) to account for
	 * @param lockOptions Pessimistic lock options to apply
	 * @param jdbcParameterConsumer Consumer for all JdbcParameter references created
	 * @param sessionFactory The SessionFactory
	 */
	public static SelectStatement createSelect(
			Loadable loadable,
			List<? extends ModelPart> partsToSelect,
			ModelPart restrictedPart,
			DomainResult cachedDomainResult,
			int numberOfKeysToLoad,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions,
			Consumer<JdbcParameter> jdbcParameterConsumer,
			SessionFactoryImplementor sessionFactory) {
		final LoaderSelectBuilder process = new LoaderSelectBuilder(
				sessionFactory,
				loadable,
				partsToSelect,
				restrictedPart,
				cachedDomainResult,
				numberOfKeysToLoad,
				loadQueryInfluencers,
				lockOptions,
				jdbcParameterConsumer
		);

		return process.generateSelect();
	}

	/**
	 * Create a SQL AST select-statement used for subselect-based CollectionLoader
	 *
	 * @see CollectionLoaderSubSelectFetch
	 *
	 * @param attributeMapping The plural-attribute being loaded
	 * @param subselect The subselect details to apply
	 * @param cachedDomainResult DomainResult to be used.  Null indicates to generate the DomainResult?
	 * @param loadQueryInfluencers Any influencers (entity graph, fetch profile) to account for
	 * @param lockOptions Pessimistic lock options to apply
	 * @param jdbcParameterConsumer Consumer for all JdbcParameter references created
	 * @param sessionFactory The SessionFactory
	 */
	public static SelectStatement createSubSelectFetchSelect(
			PluralAttributeMapping attributeMapping,
			SubselectFetch subselect,
			DomainResult cachedDomainResult,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions,
			Consumer<JdbcParameter> jdbcParameterConsumer,
			SessionFactoryImplementor sessionFactory) {
		final LoaderSelectBuilder process = new LoaderSelectBuilder(
				sessionFactory,
				attributeMapping,
				null,
				attributeMapping.getKeyDescriptor(),
				cachedDomainResult,
				-1,
				loadQueryInfluencers,
				lockOptions,
				jdbcParameterConsumer
		);

		return process.generateSelect( subselect );
	}

	private final SqlAstCreationContext creationContext;
	private final Loadable loadable;
	private final List<? extends ModelPart> partsToSelect;
	private final ModelPart restrictedPart;
	private final DomainResult cachedDomainResult;
	private final int numberOfKeysToLoad;
	private final LoadQueryInfluencers loadQueryInfluencers;
	private final LockOptions lockOptions;
	private final Consumer<JdbcParameter> jdbcParameterConsumer;


	private LoaderSelectBuilder(
			SqlAstCreationContext creationContext,
			Loadable loadable,
			List<? extends ModelPart> partsToSelect,
			ModelPart restrictedPart,
			DomainResult cachedDomainResult,
			int numberOfKeysToLoad,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions,
			Consumer<JdbcParameter> jdbcParameterConsumer) {
		this.creationContext = creationContext;
		this.loadable = loadable;
		this.partsToSelect = partsToSelect;
		this.restrictedPart = restrictedPart;
		this.cachedDomainResult = cachedDomainResult;
		this.numberOfKeysToLoad = numberOfKeysToLoad;
		this.loadQueryInfluencers = loadQueryInfluencers;
		this.lockOptions = lockOptions != null ? lockOptions : LockOptions.NONE;
		this.jdbcParameterConsumer = jdbcParameterConsumer;
	}

	private SelectStatement generateSelect() {
		final NavigablePath rootNavigablePath = new NavigablePath( loadable.getRootPathName() );

		final QuerySpec rootQuerySpec = new QuerySpec( true );
		final List<DomainResult> domainResults;

		final LoaderSqlAstCreationState sqlAstCreationState = new LoaderSqlAstCreationState(
				rootQuerySpec,
				new SqlAliasBaseManager(),
				new SimpleFromClauseAccessImpl(),
				lockOptions,
				this::visitFetches,
				creationContext
		);

		final TableGroup rootTableGroup = loadable.createRootTableGroup(
				rootNavigablePath,
				null,
				true,
				lockOptions.getLockMode(),
				sqlAstCreationState.getSqlAliasBaseManager(),
				sqlAstCreationState.getSqlExpressionResolver(),
				() -> rootQuerySpec::applyPredicate,
				creationContext
		);

		rootQuerySpec.getFromClause().addRoot( rootTableGroup );
		sqlAstCreationState.getFromClauseAccess().registerTableGroup( rootNavigablePath, rootTableGroup );

		if ( loadable instanceof PluralAttributeMapping ) {
			applyOrdering( rootTableGroup, (PluralAttributeMapping) loadable );
		}

		if ( partsToSelect != null && !partsToSelect.isEmpty() ) {
			domainResults = new ArrayList<>();
			for ( ModelPart part : partsToSelect ) {
				final NavigablePath navigablePath = rootNavigablePath.append( part.getPartName() );
				domainResults.add(
						part.createDomainResult(
								navigablePath,
								rootTableGroup,
								null,
								sqlAstCreationState
						)
				);
			}
		}
		else {
			// use the one passed to the constructor or create one (maybe always create and pass?)
			//		allows re-use as they can be re-used to save on memory - they
			//		do not share state between
			final DomainResult domainResult;
			if ( this.cachedDomainResult != null ) {
				// used the one passed to the constructor
				domainResult = this.cachedDomainResult;
			}
			else {
				// create one
				domainResult = loadable.createDomainResult(
						rootNavigablePath,
						rootTableGroup,
						null,
						sqlAstCreationState
				);
			}

			domainResults = Collections.singletonList( domainResult );
		}

		final int numberOfKeyColumns = restrictedPart.getJdbcTypeCount(
				creationContext.getDomainModel().getTypeConfiguration()
		);

		applyKeyRestriction(
				rootQuerySpec,
				rootNavigablePath,
				rootTableGroup,
				restrictedPart,
				numberOfKeyColumns,
				jdbcParameterConsumer,
				sqlAstCreationState
		);

		if ( orderByFragments != null ) {
			orderByFragments.forEach(
					(orderByFragment, tableGroup) -> orderByFragment.apply( rootQuerySpec, tableGroup, sqlAstCreationState )
			);
		}

		return new SelectStatement( rootQuerySpec, domainResults );
	}

	private void applyKeyRestriction(
			QuerySpec rootQuerySpec,
			NavigablePath rootNavigablePath,
			TableGroup rootTableGroup,
			ModelPart keyPart,
			int numberOfKeyColumns,
			Consumer<JdbcParameter> jdbcParameterConsumer,
			LoaderSqlAstCreationState sqlAstCreationState) {
		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();

		if ( numberOfKeyColumns == 1 ) {
			assert keyPart instanceof BasicValuedModelPart;
			final BasicValuedModelPart basicKeyPart = (BasicValuedModelPart) keyPart;

			final JdbcMapping jdbcMapping = basicKeyPart.getJdbcMapping();

			final String tableExpression = basicKeyPart.getContainingTableExpression();
			final String columnExpression = basicKeyPart.getMappedColumnExpression();
			final TableReference tableReference = rootTableGroup.resolveTableReference( tableExpression );
			final ColumnReference columnRef = (ColumnReference) sqlExpressionResolver.resolveSqlExpression(
					createColumnReferenceKey( tableReference, columnExpression ),
					p -> new ColumnReference(
							tableReference,
							columnExpression,
							jdbcMapping,
							creationContext.getSessionFactory()
					)
			);

			if ( numberOfKeysToLoad == 1 ) {
				final JdbcParameter jdbcParameter = new JdbcParameterImpl( jdbcMapping );
				jdbcParameterConsumer.accept( jdbcParameter );

				rootQuerySpec.applyPredicate(
						new ComparisonPredicate( columnRef, ComparisonOperator.EQUAL, jdbcParameter )
				);
			}
			else {
				final InListPredicate predicate = new InListPredicate( columnRef );
				for ( int i = 0; i < numberOfKeysToLoad; i++ ) {
					for ( int j = 0; j < numberOfKeyColumns; j++ ) {
						final JdbcParameter jdbcParameter = new JdbcParameterImpl( columnRef.getJdbcMapping() );
						jdbcParameterConsumer.accept( jdbcParameter );
						predicate.addExpression( jdbcParameter );
					}
				}
				rootQuerySpec.applyPredicate( predicate );
			}
		}
		else {
			final List<ColumnReference> columnReferences = new ArrayList<>( numberOfKeyColumns );

			keyPart.visitColumns(
					(columnExpression, containingTableExpression, jdbcMapping) -> {
						final TableReference tableReference = rootTableGroup.resolveTableReference( containingTableExpression );
						columnReferences.add(
								(ColumnReference) sqlExpressionResolver.resolveSqlExpression(
										createColumnReferenceKey( tableReference, columnExpression ),
										p -> new ColumnReference(
												tableReference,
												columnExpression,
												jdbcMapping,
												creationContext.getSessionFactory()
										)
								)
						);
					}
			);

			final SqlTuple tuple = new SqlTuple( columnReferences, keyPart );
			final InListPredicate predicate = new InListPredicate( tuple );

			for ( int i = 0; i < numberOfKeysToLoad; i++ ) {
				final List<JdbcParameter> tupleParams = new ArrayList<>(  );
				for ( int j = 0; j < numberOfKeyColumns; j++ ) {
					final ColumnReference columnReference = columnReferences.get( j );
					final JdbcParameter jdbcParameter = new JdbcParameterImpl( columnReference.getJdbcMapping() );
					jdbcParameterConsumer.accept( jdbcParameter );
					tupleParams.add( jdbcParameter );
				}
				final SqlTuple paramTuple = new SqlTuple( tupleParams, keyPart );
				predicate.addExpression( paramTuple );
			}

			rootQuerySpec.applyPredicate( predicate );
		}
	}

	private Map<OrderByFragment,TableGroup> orderByFragments;

	private void applyOrdering(TableGroup tableGroup, PluralAttributeMapping pluralAttributeMapping) {
		if ( pluralAttributeMapping.getOrderByFragment() != null ) {
			applyOrdering( tableGroup, pluralAttributeMapping.getOrderByFragment() );
		}

		if ( pluralAttributeMapping.getManyToManyOrderByFragment() != null ) {
			applyOrdering( tableGroup, pluralAttributeMapping.getManyToManyOrderByFragment() );
		}
	}

	private void applyOrdering(
			TableGroup tableGroup,
			OrderByFragment orderByFragment) {
		if ( orderByFragments == null ) {
			orderByFragments = new LinkedHashMap<>();
		}
		orderByFragments.put( orderByFragment, tableGroup );
	}

	private int fetchDepth = 0;

	private List<Fetch> visitFetches(FetchParent fetchParent, QuerySpec querySpec, LoaderSqlAstCreationState creationState) {
		log.tracef( "Starting visitation of FetchParent's Fetchables : %s", fetchParent.getNavigablePath() );

		final List<Fetch> fetches = new ArrayList<>();

		final Consumer<Fetchable> processor = fetchable -> {
			final NavigablePath fetchablePath = fetchParent.getNavigablePath().append( fetchable.getFetchableName() );

			final Fetch biDirectionalFetch = fetchable.resolveCircularFetch(
					fetchablePath,
					fetchParent,
					creationState
			);

			if ( biDirectionalFetch != null ) {
				fetches.add( biDirectionalFetch );
				return;
			}

			LockMode lockMode = LockMode.READ;
			FetchTiming fetchTiming = fetchable.getMappedFetchStrategy().getTiming();
			boolean joined = fetchable.getMappedFetchStrategy().getStyle() == FetchStyle.JOIN;

//			if ( loadable instanceof PluralValuedNavigable ) {
//				// processing a collection-loader
//
//				// if the `fetchable` is the "collection owner" and the collection owner is available in Session - don't join
//				final String collectionMappedByProperty = ( (PluralValuedNavigable) rootContainer ).getCollectionDescriptor()
//						.getMappedByProperty();
//				if ( collectionMappedByProperty != null && collectionMappedByProperty.equals( fetchable.getNavigableName() ) ) {
//					joined = false;
//				}
//			}

			final Integer maximumFetchDepth = creationContext.getMaximumFetchDepth();

			if ( maximumFetchDepth != null ) {
				if ( fetchDepth == maximumFetchDepth ) {
					joined = false;
				}
				else if ( fetchDepth > maximumFetchDepth ) {
					return;
				}
			}

			try {
				if ( ! (fetchable instanceof BasicValuedModelPart) ) {
					fetchDepth++;
				}
				Fetch fetch = fetchable.generateFetch(
						fetchParent,
						fetchablePath,
						fetchTiming,
						joined,
						lockMode,
						null,
						creationState
				);
				fetches.add( fetch );

				if ( fetchable instanceof PluralAttributeMapping && fetchTiming == FetchTiming.IMMEDIATE ) {
					applyOrdering(
							querySpec,
							fetchablePath,
							( (PluralAttributeMapping) fetchable ),
							creationState
					);
				}
			}
			finally {
				if ( ! (fetchable instanceof BasicValuedModelPart) ) {
					fetchDepth--;
				}
			}
		};

		final FetchableContainer referencedMappingContainer = fetchParent.getReferencedMappingContainer();
		referencedMappingContainer.visitKeyFetchables( processor, null );
		referencedMappingContainer.visitFetchables( processor, null );

		return fetches;
	}

	private void applyOrdering(
			QuerySpec ast,
			NavigablePath navigablePath,
			PluralAttributeMapping pluralAttributeMapping,
			LoaderSqlAstCreationState sqlAstCreationState) {
		assert pluralAttributeMapping.getAttributeName().equals( navigablePath.getLocalName() );

		final TableGroup tableGroup = sqlAstCreationState.getFromClauseAccess().getTableGroup( navigablePath );
		assert tableGroup != null;

		applyOrdering( tableGroup, pluralAttributeMapping );
	}

	private SelectStatement generateSelect(SubselectFetch subselect) {
		// todo (6.0) : i think we may even be able to convert this to a join by piecing together
		//		parts from the subselect-fetch sql-ast..

		// todo (6.0) : ^^ another interesting idea is to use `partsToSelect` here relative to the owner
		//		- so `loadable` is the owner entity-descriptor and the `partsToSelect` is the collection

		assert loadable instanceof PluralAttributeMapping;

		final PluralAttributeMapping attributeMapping = (PluralAttributeMapping) loadable;

		final QuerySpec rootQuerySpec = new QuerySpec( true );

		final NavigablePath rootNavigablePath = new NavigablePath( loadable.getRootPathName() );

		final LoaderSqlAstCreationState sqlAstCreationState = new LoaderSqlAstCreationState(
				rootQuerySpec,
				new SqlAliasBaseManager(),
				new SimpleFromClauseAccessImpl(),
				lockOptions,
				this::visitFetches,
				creationContext
		);

		// todo (6.0) : I think we want to continue to assign aliases to these table-references.  we just want
		//  	to control how that gets rendered in the walker

		final TableGroup rootTableGroup = loadable.createRootTableGroup(
				rootNavigablePath,
				null,
				true,
				lockOptions.getLockMode(),
				sqlAstCreationState.getSqlAliasBaseManager(),
				sqlAstCreationState.getSqlExpressionResolver(),
				() -> rootQuerySpec::applyPredicate,
				creationContext
		);

		rootQuerySpec.getFromClause().addRoot( rootTableGroup );
		sqlAstCreationState.getFromClauseAccess().registerTableGroup( rootNavigablePath, rootTableGroup );

		// NOTE : no need to check - we are explicitly processing a plural-attribute
		applyOrdering( rootTableGroup, (PluralAttributeMapping) loadable );

		// generate and apply the restriction
		applySubSelectRestriction(
				rootQuerySpec,
				rootNavigablePath,
				rootTableGroup,
				subselect,
				sqlAstCreationState
		);

		// register the jdbc-parameters
		subselect.getLoadingJdbcParameters().forEach( jdbcParameterConsumer );

		return new SelectStatement(
				rootQuerySpec,
				Collections.singletonList(
						new CollectionDomainResult(
								rootNavigablePath,
								attributeMapping,
								null,
								rootTableGroup,
								sqlAstCreationState
						)
				)
		);
	}

	private void applySubSelectRestriction(
			QuerySpec querySpec,
			NavigablePath rootNavigablePath,
			TableGroup rootTableGroup,
			SubselectFetch subselect,
			LoaderSqlAstCreationState sqlAstCreationState) {
		final SqlAstCreationContext sqlAstCreationContext = sqlAstCreationState.getCreationContext();
		final SessionFactoryImplementor sessionFactory = sqlAstCreationContext.getSessionFactory();

		assert loadable instanceof PluralAttributeMapping;
		assert restrictedPart == null || restrictedPart instanceof ForeignKeyDescriptor;

		final PluralAttributeMapping attributeMapping = (PluralAttributeMapping) loadable;
		final ForeignKeyDescriptor fkDescriptor = attributeMapping.getKeyDescriptor();

		final Expression fkExpression;

		final int jdbcTypeCount = fkDescriptor.getJdbcTypeCount( sessionFactory.getTypeConfiguration() );
		if ( jdbcTypeCount == 1 ) {
			assert fkDescriptor instanceof SimpleForeignKeyDescriptor;
			final SimpleForeignKeyDescriptor simpleFkDescriptor = (SimpleForeignKeyDescriptor) fkDescriptor;
			fkExpression = sqlAstCreationState.getSqlExpressionResolver().resolveSqlExpression(
					createColumnReferenceKey(
							simpleFkDescriptor.getContainingTableExpression(),
							simpleFkDescriptor.getMappedColumnExpression()
					),
					sqlAstProcessingState -> new ColumnReference(
							rootTableGroup.resolveTableReference( simpleFkDescriptor.getContainingTableExpression() ),
							simpleFkDescriptor.getMappedColumnExpression(),
							simpleFkDescriptor.getJdbcMapping(),
							this.creationContext.getSessionFactory()
					)
			);
		}
		else {
			final List<ColumnReference> columnReferences = new ArrayList<>( jdbcTypeCount );
			fkDescriptor.visitColumns(
					(containingTableExpression, columnExpression, jdbcMapping) -> {
						columnReferences.add(
								(ColumnReference) sqlAstCreationState.getSqlExpressionResolver().resolveSqlExpression(
										createColumnReferenceKey( containingTableExpression, columnExpression ),
										sqlAstProcessingState -> new ColumnReference(
												rootTableGroup.resolveTableReference( containingTableExpression ),
												columnExpression,
												jdbcMapping,
												this.creationContext.getSessionFactory()
										)
								)
						);
					}
			);

			fkExpression = new SqlTuple( columnReferences, fkDescriptor );
		}

		querySpec.applyPredicate(
				new InSubQueryPredicate(
						fkExpression,
						generateSubSelect(
								attributeMapping,
								rootTableGroup,
								subselect,
								jdbcTypeCount,
								sqlAstCreationState,
								sessionFactory
						),
						false
				)
		);
	}

	private QuerySpec generateSubSelect(
			PluralAttributeMapping attributeMapping,
			TableGroup rootTableGroup,
			SubselectFetch subselect,
			int jdbcTypeCount,
			LoaderSqlAstCreationState creationState,
			SessionFactoryImplementor sessionFactory) {
		final ForeignKeyDescriptor fkDescriptor = attributeMapping.getKeyDescriptor();

		final QuerySpec subQuery = new QuerySpec( false );

		final QuerySpec loadingSqlAst = subselect.getLoadingSqlAst();

		// todo (6.0) : we need to find the owner's TableGroup in the `loadingSqlAst`
		final TableGroup ownerTableGroup = subselect.getOwnerTableGroup();

		// transfer the from-clause
		loadingSqlAst.getFromClause().visitRoots( subQuery.getFromClause()::addRoot );

		final SqlExpressionResolver sqlExpressionResolver = creationState.getSqlExpressionResolver();

		final AtomicInteger count = new AtomicInteger();
		fkDescriptor.visitTargetColumns(
				(containingTableExpression, columnExpression, jdbcMapping) -> {
					// for each column, resolve a SqlSelection and add it to the sub-query select-clause
					final TableReference tableReference = ownerTableGroup.resolveTableReference( containingTableExpression );
					final Expression expression = sqlExpressionResolver.resolveSqlExpression(
							createColumnReferenceKey( tableReference, columnExpression ),
							sqlAstProcessingState -> new ColumnReference(
									tableReference,
									columnExpression,
									jdbcMapping,
									sessionFactory
							)
					);
					final int valuesPosition = count.getAndIncrement();
					subQuery.getSelectClause().addSqlSelection(
							new SqlSelectionImpl(
									valuesPosition + 1,
									valuesPosition,
									expression
							)
					);
				}
		);

		// transfer the restriction
		subQuery.applyPredicate( loadingSqlAst.getWhereClauseRestrictions() );

		return subQuery;
	}
}

