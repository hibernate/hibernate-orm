/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.hibernate.LockOptions;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.EffectiveEntityGraph;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.loader.ast.spi.Loadable;
import org.hibernate.loader.ast.spi.Loader;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.Restrictable;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.metamodel.mapping.internal.SimpleForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.mapping.ordering.OrderByFragment;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.spi.EntityIdentifierNavigablePath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.AliasCollector;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SimpleFromClauseAccessImpl;
import org.hibernate.sql.ast.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.PluralTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.InArrayPredicate;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.predicate.PredicateContainer;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.results.graph.BiDirectionalFetch;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.EntityGraphTraversalState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.FetchableContainer;
import org.hibernate.sql.results.graph.collection.internal.CollectionDomainResult;
import org.hibernate.sql.results.graph.entity.EntityValuedFetchable;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.internal.StandardEntityGraphTraversalStateImpl;

import org.jboss.logging.Logger;

import static java.util.Collections.singletonList;
import static org.hibernate.query.results.ResultsHelper.attributeName;

/**
 * Builder for SQL AST trees used by {@link Loader} implementations.
 *
 * @author Steve Ebersole
 * @author Nahtan Xu
 */
public class LoaderSelectBuilder {
	private static final Logger log = Logger.getLogger( LoaderSelectBuilder.class );

	/**
	 * Create an SQL AST select-statement for loading by unique key
	 *
	 * @param loadable The root Loadable
	 * @param partsToSelect Parts of the Loadable to select.  Null/empty indicates to select the Loadable itself
	 * @param restrictedPart Part to base the where-clause restriction on
	 * @param cachedDomainResult DomainResult to be used.  Null indicates to generate the DomainResult
	 * @param loadQueryInfluencers Any influencers (entity graph, fetch profile) to account for
	 * @param lockOptions Pessimistic lock options to apply
	 * @param jdbcParameterConsumer Consumer for all JdbcParameter references created
	 * @param sessionFactory The SessionFactory
	 */
	public static SelectStatement createSelectByUniqueKey(
			Loadable loadable,
			List<? extends ModelPart> partsToSelect,
			ModelPart restrictedPart,
			DomainResult<?> cachedDomainResult,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions,
			Consumer<JdbcParameter> jdbcParameterConsumer,
			SessionFactoryImplementor sessionFactory) {
		final LoaderSelectBuilder process = new LoaderSelectBuilder(
				sessionFactory,
				loadable,
				partsToSelect,
				singletonList( restrictedPart ),
				cachedDomainResult,
				1,
				loadQueryInfluencers,
				lockOptions,
				determineGraphTraversalState( loadQueryInfluencers, sessionFactory ),
				true,
				jdbcParameterConsumer
		);

		return process.generateSelect();
	}

	/**
	 * Create a select-statement (SQL AST) for loading by multiple keys using a single SQL ARRAY parameter
	 */
	public static SelectStatement createSelectBySingleArrayParameter(
			Loadable loadable,
			ValuedModelPart restrictedPart,
			LoadQueryInfluencers influencers,
			LockOptions lockOptions,
			JdbcParameter jdbcArrayParameter,
			SessionFactoryImplementor sessionFactory) {
		final LoaderSelectBuilder builder = new LoaderSelectBuilder(
				sessionFactory,
				loadable,
				null,
				singletonList( restrictedPart ),
				null,
				-1,
				influencers,
				lockOptions,
				determineGraphTraversalState( influencers, sessionFactory ),
				true,
				null
		);

		final QuerySpec rootQuerySpec = new QuerySpec( true );
		final LoaderSqlAstCreationState sqlAstCreationState = builder.createSqlAstCreationState( rootQuerySpec );

		final NavigablePath rootNavigablePath = new NavigablePath( loadable.getRootPathName() );
		final TableGroup rootTableGroup = builder.buildRootTableGroup( rootNavigablePath, rootQuerySpec, sqlAstCreationState );

		final DomainResult<?> domainResult = loadable.createDomainResult(
				rootNavigablePath,
				rootTableGroup,
				null,
				sqlAstCreationState
		);

		final List<DomainResult<?>> domainResults = singletonList( domainResult );

		applyArrayParamRestriction(
				rootQuerySpec,
				rootNavigablePath,
				rootTableGroup,
				restrictedPart,
				jdbcArrayParameter,
				sqlAstCreationState
		);


		if ( loadable instanceof PluralAttributeMapping ) {
			final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) loadable;
			builder.applyFiltering( rootQuerySpec, rootTableGroup, pluralAttributeMapping, sqlAstCreationState );
			builder.applyOrdering( rootQuerySpec, rootTableGroup, pluralAttributeMapping, sqlAstCreationState );
		}
		else {
			builder.applyFiltering( rootQuerySpec, rootTableGroup, (Restrictable) loadable, sqlAstCreationState );
		}

		return new SelectStatement( rootQuerySpec, domainResults );
	}

	private static void applyArrayParamRestriction(
			QuerySpec rootQuerySpec,
			NavigablePath rootNavigablePath,
			TableGroup rootTableGroup,
			ValuedModelPart restrictedPart,
			JdbcParameter jdbcArrayParameter,
			LoaderSqlAstCreationState sqlAstCreationState) {
		assert restrictedPart.getJdbcTypeCount() == 1;
		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		final SelectableMapping restrictedPartMapping = restrictedPart.getSelectable( 0 );
		final NavigablePath restrictionPath = rootNavigablePath.append( restrictedPart.getNavigableRole().getNavigableName() );
		final TableReference tableReference = rootTableGroup.resolveTableReference( restrictionPath, restrictedPartMapping.getContainingTableExpression() );
		final ColumnReference columnRef = (ColumnReference) sqlExpressionResolver.resolveSqlExpression(
				tableReference,
				restrictedPartMapping
		);

		rootQuerySpec.applyPredicate( new InArrayPredicate( columnRef, jdbcArrayParameter ) );
	}

	/**
	 * Create an SQL AST select-statement based on matching one-or-more keys
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
			DomainResult<?> cachedDomainResult,
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

	public static SelectStatement createSelect(
			Loadable loadable,
			List<? extends ModelPart> partsToSelect,
			List<ModelPart> restrictedParts,
			DomainResult<?> cachedDomainResult,
			int numberOfKeysToLoad,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions,
			Consumer<JdbcParameter> jdbcParameterConsumer,
			SessionFactoryImplementor sessionFactory) {
		final LoaderSelectBuilder process = new LoaderSelectBuilder(
				sessionFactory,
				loadable,
				partsToSelect,
				restrictedParts,
				cachedDomainResult,
				numberOfKeysToLoad,
				loadQueryInfluencers,
				lockOptions,
				jdbcParameterConsumer
		);

		return process.generateSelect();
	}

	// TODO: this method is probably unnecessary if we make
	// determineWhetherToForceIdSelection() a bit smarter
	static SelectStatement createSelect(
			Loadable loadable,
			List<ModelPart> partsToSelect,
			boolean forceIdentifierSelection,
			List<ModelPart> restrictedParts,
			DomainResult<?> cachedDomainResult,
			int numberOfKeysToLoad,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions,
			Consumer<JdbcParameter> jdbcParameterConsumer,
			SessionFactoryImplementor sessionFactory) {
		final LoaderSelectBuilder process = new LoaderSelectBuilder(
				sessionFactory,
				loadable,
				partsToSelect,
				restrictedParts,
				cachedDomainResult,
				numberOfKeysToLoad,
				loadQueryInfluencers,
				lockOptions,
				determineGraphTraversalState( loadQueryInfluencers, sessionFactory ),
				forceIdentifierSelection,
				jdbcParameterConsumer
		);

		return process.generateSelect();
	}

	/**
	 * Create an SQL AST select-statement used for subselect-based CollectionLoader
	 *
	 * @param attributeMapping The plural-attribute being loaded
	 * @param subselect The subselect details to apply
	 * @param cachedDomainResult DomainResult to be used.  Null indicates to generate the DomainResult?
	 * @param loadQueryInfluencers Any influencers (entity graph, fetch profile) to account for
	 * @param lockOptions Pessimistic lock options to apply
	 * @param jdbcParameterConsumer Consumer for all JdbcParameter references created
	 * @param sessionFactory The SessionFactory
	 *
	 * @see CollectionLoaderSubSelectFetch
	 */
	public static SelectStatement createSubSelectFetchSelect(
			PluralAttributeMapping attributeMapping,
			SubselectFetch subselect,
			DomainResult<?> cachedDomainResult,
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
	private final List<ModelPart> restrictedParts;
	private final DomainResult<?> cachedDomainResult;
	private final int numberOfKeysToLoad;
	private final boolean forceIdentifierSelection;
	private final LoadQueryInfluencers loadQueryInfluencers;
	private final LockOptions lockOptions;
	private final Consumer<JdbcParameter> jdbcParameterConsumer;
	private final EntityGraphTraversalState entityGraphTraversalState;

	private int fetchDepth;
	private RowCardinality rowCardinality = RowCardinality.SINGLE;

	private LoaderSelectBuilder(
			SqlAstCreationContext creationContext,
			Loadable loadable,
			List<? extends ModelPart> partsToSelect,
			List<ModelPart> restrictedParts,
			DomainResult<?> cachedDomainResult,
			int numberOfKeysToLoad,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions,
			EntityGraphTraversalState entityGraphTraversalState,
			boolean forceIdentifierSelection,
			Consumer<JdbcParameter> jdbcParameterConsumer) {
		this.creationContext = creationContext;
		this.loadable = loadable;
		this.partsToSelect = partsToSelect;
		this.restrictedParts = restrictedParts;
		this.cachedDomainResult = cachedDomainResult;
		this.numberOfKeysToLoad = numberOfKeysToLoad;
		this.loadQueryInfluencers = loadQueryInfluencers;
		this.lockOptions = lockOptions;
		this.entityGraphTraversalState = entityGraphTraversalState;
		this.forceIdentifierSelection = forceIdentifierSelection;
		this.jdbcParameterConsumer = jdbcParameterConsumer;
		if ( loadable instanceof PluralAttributeMapping ) {
			final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) loadable;
			if ( pluralAttributeMapping.getMappedType()
					.getCollectionSemantics()
					.getCollectionClassification() == CollectionClassification.BAG ) {
				rowCardinality = RowCardinality.BAG;
			}
		}
	}

	private LoaderSelectBuilder(
			SqlAstCreationContext creationContext,
			Loadable loadable,
			List<? extends ModelPart> partsToSelect,
			List<ModelPart> restrictedParts,
			DomainResult<?> cachedDomainResult,
			int numberOfKeysToLoad,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions,
			Consumer<JdbcParameter> jdbcParameterConsumer) {
		this(
				creationContext,
				loadable,
				partsToSelect,
				restrictedParts,
				cachedDomainResult,
				numberOfKeysToLoad,
				loadQueryInfluencers,
				lockOptions != null ? lockOptions : LockOptions.NONE,
				determineGraphTraversalState( loadQueryInfluencers, creationContext.getSessionFactory() ),
				determineWhetherToForceIdSelection( numberOfKeysToLoad, restrictedParts ),
				jdbcParameterConsumer
		);
	}

	private LoaderSelectBuilder(
			SqlAstCreationContext creationContext,
			Loadable loadable,
			List<? extends ModelPart> partsToSelect,
			ModelPart restrictedPart,
			DomainResult<?> cachedDomainResult,
			int numberOfKeysToLoad,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions,
			Consumer<JdbcParameter> jdbcParameterConsumer) {
		this(
				creationContext,
				loadable,
				partsToSelect,
				singletonList( restrictedPart ),
				cachedDomainResult,
				numberOfKeysToLoad,
				loadQueryInfluencers,
				lockOptions,
				jdbcParameterConsumer
		);
	}

	private static boolean determineWhetherToForceIdSelection(int numberOfKeysToLoad, List<ModelPart> restrictedParts) {
		if ( numberOfKeysToLoad > 1 ) {
			return true;
		}

		if ( restrictedParts.size() == 1 ) {
			final ModelPart restrictedPart = restrictedParts.get( 0 );
			if ( Objects.equals( restrictedPart.getPartName(), NaturalIdMapping.PART_NAME ) ) {
				return true;
			}
		}

		for ( ModelPart restrictedPart : restrictedParts ) {
			if ( restrictedPart instanceof ForeignKeyDescriptor || restrictedPart instanceof NonAggregatedIdentifierMapping ) {
				return true;
			}
		}

		return false;
	}

	private static EntityGraphTraversalState determineGraphTraversalState(
			LoadQueryInfluencers loadQueryInfluencers,
			SessionFactoryImplementor sessionFactory) {
		if ( loadQueryInfluencers != null ) {
			final EffectiveEntityGraph effectiveEntityGraph = loadQueryInfluencers.getEffectiveEntityGraph();
			if ( effectiveEntityGraph != null ) {
				final GraphSemantic graphSemantic = effectiveEntityGraph.getSemantic();
				final RootGraphImplementor<?> rootGraphImplementor = effectiveEntityGraph.getGraph();
				if ( graphSemantic != null && rootGraphImplementor != null ) {
					return new StandardEntityGraphTraversalStateImpl(
							graphSemantic,
							rootGraphImplementor,
							sessionFactory.getJpaMetamodel()
					);
				}
			}
		}
		return null;
	}

	private SelectStatement generateSelect() {
		final NavigablePath rootNavigablePath = new NavigablePath( loadable.getRootPathName() );

		final QuerySpec rootQuerySpec = new QuerySpec( true );
		final LoaderSqlAstCreationState sqlAstCreationState = createSqlAstCreationState( rootQuerySpec );

		final TableGroup rootTableGroup = buildRootTableGroup( rootNavigablePath, rootQuerySpec, sqlAstCreationState );

		final List<DomainResult<?>> domainResults;
		if ( partsToSelect != null && !partsToSelect.isEmpty() ) {
			domainResults = buildRequestedDomainResults( rootNavigablePath, sqlAstCreationState, rootTableGroup );
		}
		else if ( this.cachedDomainResult != null ) {
			domainResults = singletonList( this.cachedDomainResult );
		}
		else {
			final DomainResult<?> domainResult = loadable.createDomainResult(
					rootNavigablePath,
					rootTableGroup,
					null,
					sqlAstCreationState
			);
			domainResults = singletonList( domainResult );
		}

		for ( ModelPart restrictedPart : restrictedParts ) {
			final int numberOfRestrictionColumns = restrictedPart.getJdbcTypeCount();

			applyRestriction(
					rootQuerySpec,
					rootNavigablePath,
					rootTableGroup,
					restrictedPart,
					numberOfRestrictionColumns,
					jdbcParameterConsumer,
					sqlAstCreationState
			);
		}

		if ( loadable instanceof PluralAttributeMapping ) {
			final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) loadable;
			applyFiltering( rootQuerySpec, rootTableGroup, pluralAttributeMapping, sqlAstCreationState );
			applyOrdering( rootQuerySpec, rootTableGroup, pluralAttributeMapping, sqlAstCreationState );
		}
		else {
			applyFiltering( rootQuerySpec, rootTableGroup, (Restrictable) loadable, sqlAstCreationState );
		}

		return new SelectStatement( rootQuerySpec, domainResults );
	}

	private List<DomainResult<?>> buildRequestedDomainResults(NavigablePath rootNavigablePath, LoaderSqlAstCreationState sqlAstCreationState, TableGroup rootTableGroup) {
		final List<DomainResult<?>> domainResults;
		domainResults = new ArrayList<>( partsToSelect.size() );
		for ( ModelPart part : partsToSelect ) {
			final NavigablePath navigablePath = rootNavigablePath.append( part.getPartName() );
			final TableGroup tableGroup;
			if ( part instanceof TableGroupJoinProducer ) {
				final TableGroupJoinProducer tableGroupJoinProducer = (TableGroupJoinProducer) part;
				final TableGroupJoin tableGroupJoin = tableGroupJoinProducer.createTableGroupJoin(
						navigablePath,
						rootTableGroup,
						null,
						null,
						SqlAstJoinType.LEFT,
						true,
						false,
						sqlAstCreationState
				);
				rootTableGroup.addTableGroupJoin( tableGroupJoin );
				tableGroup = tableGroupJoin.getJoinedGroup();
				sqlAstCreationState.getFromClauseAccess().registerTableGroup( navigablePath, tableGroup );
				registerPluralTableGroupParts( sqlAstCreationState.getFromClauseAccess(), tableGroup );
			}
			else {
				tableGroup = rootTableGroup;
			}
			domainResults.add(
					part.createDomainResult(
							navigablePath,
							tableGroup,
							null,
							sqlAstCreationState
					)
			);
		}
		return domainResults;
	}

	private TableGroup buildRootTableGroup(NavigablePath rootNavigablePath, QuerySpec rootQuerySpec, LoaderSqlAstCreationState sqlAstCreationState) {
		final TableGroup rootTableGroup = loadable.createRootTableGroup(
				true,
				rootNavigablePath,
				null,
				null,
				() -> rootQuerySpec::applyPredicate,
				sqlAstCreationState
		);

		rootQuerySpec.getFromClause().addRoot( rootTableGroup );
		sqlAstCreationState.getFromClauseAccess().registerTableGroup( rootNavigablePath, rootTableGroup );
		registerPluralTableGroupParts( sqlAstCreationState.getFromClauseAccess(), rootTableGroup );
		return rootTableGroup;
	}

	private LoaderSqlAstCreationState createSqlAstCreationState(QuerySpec rootQuerySpec) {
		return new LoaderSqlAstCreationState(
				rootQuerySpec,
				new SqlAliasBaseManager(),
				new SimpleFromClauseAccessImpl(),
				lockOptions,
				this::visitFetches,
				forceIdentifierSelection,
				loadQueryInfluencers,
				creationContext
		);
	}

	private void applyRestriction(
			QuerySpec rootQuerySpec,
			NavigablePath rootNavigablePath,
			TableGroup rootTableGroup,
			ModelPart restrictedPart,
			int numberColumns,
			Consumer<JdbcParameter> jdbcParameterConsumer,
			LoaderSqlAstCreationState sqlAstCreationState) {
		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		final NavigablePath navigablePath = rootNavigablePath.append( restrictedPart.getNavigableRole().getNavigableName() );

		if ( numberColumns == 1 ) {
			restrictedPart.forEachSelectable(
					(columnIndex, selection) -> {
						final TableReference tableReference = rootTableGroup.resolveTableReference(
								navigablePath, selection.getContainingTableExpression() );
						final ColumnReference columnRef =
								(ColumnReference) sqlExpressionResolver.resolveSqlExpression(
										tableReference,
										selection
								);
						if ( numberOfKeysToLoad == 1 ) {
							final JdbcParameter jdbcParameter = new JdbcParameterImpl( selection.getJdbcMapping() );
							jdbcParameterConsumer.accept( jdbcParameter );

							rootQuerySpec.applyPredicate(
									new ComparisonPredicate( columnRef, ComparisonOperator.EQUAL, jdbcParameter )
							);
						}
						else {
							final InListPredicate predicate = new InListPredicate( columnRef );
							for ( int i = 0; i < numberOfKeysToLoad; i++ ) {
								for ( int j = 0; j < numberColumns; j++ ) {
									final JdbcParameter jdbcParameter = new JdbcParameterImpl( columnRef.getJdbcMapping() );
									jdbcParameterConsumer.accept( jdbcParameter );
									predicate.addExpression( jdbcParameter );
								}
							}
							rootQuerySpec.applyPredicate( predicate );
						}
					}
			);

		}
		else {
			final List<ColumnReference> columnReferences = new ArrayList<>( numberColumns );

			restrictedPart.forEachSelectable(
					(columnIndex, selection) -> {
						final TableReference tableReference = rootTableGroup.resolveTableReference( navigablePath, selection.getContainingTableExpression() );
						columnReferences.add(
								(ColumnReference) sqlExpressionResolver.resolveSqlExpression(
										tableReference,
										selection
								)
						);
					}
			);

			final SqlTuple tuple = new SqlTuple( columnReferences, restrictedPart );
			final InListPredicate predicate = new InListPredicate( tuple );

			for ( int i = 0; i < numberOfKeysToLoad; i++ ) {
				final List<JdbcParameter> tupleParams = new ArrayList<>( numberColumns );
				for ( int j = 0; j < numberColumns; j++ ) {
					final ColumnReference columnReference = columnReferences.get( j );
					final JdbcParameter jdbcParameter = new JdbcParameterImpl( columnReference.getJdbcMapping() );
					jdbcParameterConsumer.accept( jdbcParameter );
					tupleParams.add( jdbcParameter );
				}
				final SqlTuple paramTuple = new SqlTuple( tupleParams, restrictedPart );
				predicate.addExpression( paramTuple );
			}

			rootQuerySpec.applyPredicate( predicate );
		}
	}

	private void applyFiltering(
			QuerySpec querySpec,
			TableGroup tableGroup,
			PluralAttributeMapping pluralAttributeMapping,
			SqlAstCreationState astCreationState) {
		// Only apply restrictions for root table groups,
		// because for table group joins the restriction is applied via PluralAttributeMappingImpl.createTableGroupJoin
		assert tableGroup.getNavigablePath().getParent() == null;
		pluralAttributeMapping.applyBaseRestrictions(
				querySpec::applyPredicate,
				tableGroup,
				true,
				loadQueryInfluencers.getEnabledFilters(),
				false,
				null,
				astCreationState
		);
		pluralAttributeMapping.applyBaseManyToManyRestrictions(
				querySpec::applyPredicate,
				tableGroup,
				true,
				loadQueryInfluencers.getEnabledFilters(),
				null,
				astCreationState
		);
	}

	private void applyFiltering(
			PredicateContainer predicateContainer,
			TableGroup tableGroup,
			Restrictable restrictable,
			SqlAstCreationState astCreationState) {
		restrictable.applyBaseRestrictions(
				predicateContainer::applyPredicate,
				tableGroup,
				true,
				loadQueryInfluencers.getEnabledFilters(),
				true,
				null,
				astCreationState
		);
	}

	private void applyOrdering(
			QuerySpec querySpec,
			TableGroup tableGroup,
			PluralAttributeMapping pluralAttributeMapping,
			SqlAstCreationState astCreationState) {
		if ( pluralAttributeMapping.getOrderByFragment() != null ) {
			applyOrdering( querySpec, tableGroup, pluralAttributeMapping.getOrderByFragment(), astCreationState );
		}

		if ( pluralAttributeMapping.getManyToManyOrderByFragment() != null ) {
			applyOrdering(
					querySpec,
					tableGroup,
					pluralAttributeMapping.getManyToManyOrderByFragment(),
					astCreationState
			);
		}
	}

	private void applyOrdering(
			QuerySpec querySpec,
			TableGroup tableGroup,
			OrderByFragment orderByFragment,
			SqlAstCreationState astCreationState) {
		orderByFragment.apply( querySpec, tableGroup, astCreationState );
	}

	private ImmutableFetchList visitFetches(FetchParent fetchParent, LoaderSqlAstCreationState creationState) {
		if ( log.isTraceEnabled() ) {
			log.tracef( "Starting visitation of FetchParent's Fetchables : %s", fetchParent.getNavigablePath() );
		}

		final ImmutableFetchList.Builder fetches = new ImmutableFetchList.Builder( fetchParent.getReferencedMappingContainer() );
		final FetchableConsumer processor = createFetchableConsumer( fetchParent, creationState, fetches );

		final FetchableContainer referencedMappingContainer = fetchParent.getReferencedMappingContainer();
		if ( fetchParent.getNavigablePath().getParent() != null ) {
			final int size = referencedMappingContainer.getNumberOfKeyFetchables();
			for ( int i = 0; i < size; i++ ) {
				processor.accept( referencedMappingContainer.getKeyFetchable( i ), true, false );
			}
		}

		final int size = referencedMappingContainer.getNumberOfFetchables();
		List<Fetchable> bagFetchables = null;
		for ( int i = 0; i < size; i++ ) {
			final Fetchable fetchable = referencedMappingContainer.getFetchable( i );
			if ( isBag( fetchable ) ) {
				if ( bagFetchables == null ) {
					bagFetchables = new ArrayList<>();
				}
				// Delay processing of bag fetchables at last since they cannot be joined and will create subsequent selects
				bagFetchables.add( fetchable );
			}
			else {
				processor.accept( fetchable, false, false );
			}
		}
		if ( bagFetchables != null ) {
			for ( Fetchable fetchable : bagFetchables ) {
				processor.accept( fetchable, false, true );
			}
		}
		return fetches.build();
	}

	private boolean isBag(Fetchable fetchable) {
		return isPluralAttributeMapping( fetchable ) && ( (PluralAttributeMapping) fetchable ).getMappedType()
					.getCollectionSemantics()
					.getCollectionClassification() == CollectionClassification.BAG;
	}

	private boolean isPluralAttributeMapping(Fetchable fetchable) {
		final AttributeMapping attributeMapping = fetchable.asAttributeMapping();
		return attributeMapping != null && attributeMapping.isPluralAttributeMapping();
	}

	@FunctionalInterface
	private interface FetchableConsumer {
		void accept(Fetchable fetchable, boolean isKeyFetchable, boolean isABag);
	}

	private FetchableConsumer createFetchableConsumer(
			FetchParent fetchParent,
			LoaderSqlAstCreationState creationState,
			ImmutableFetchList.Builder fetches) {
		return (fetchable, isKeyFetchable, isABag) -> {
			if ( !fetchable.isSelectable() ) {
				return;
			}

			final NavigablePath fetchablePath;

			if ( isKeyFetchable ) {
				final EntityIdentifierMapping identifierMapping;
				if ( fetchParent instanceof BiDirectionalFetch ) {
					final BiDirectionalFetch parentAsBiDirectionalFetch = (BiDirectionalFetch) fetchParent;
					final Fetchable biDirectionalFetchedMapping = parentAsBiDirectionalFetch.getFetchedMapping();
					if ( biDirectionalFetchedMapping instanceof EntityValuedFetchable ) {
						identifierMapping = ( (EntityValuedFetchable) biDirectionalFetchedMapping )
								.getEntityMappingType()
								.getIdentifierMapping();
					}
					else {
						identifierMapping = null;
					}
				}
				else {
					final FetchableContainer fetchableContainer = fetchParent.getReferencedMappingContainer();
					if ( fetchableContainer instanceof EntityValuedModelPart ) {
						final EntityValuedModelPart entityValuedModelPart = (EntityValuedModelPart) fetchableContainer;
						identifierMapping = entityValuedModelPart.getEntityMappingType().getIdentifierMapping();
					}
					else {
						identifierMapping = null;
					}
				}

				if ( identifierMapping != null ) {
					fetchablePath = new EntityIdentifierNavigablePath(
							fetchParent.getNavigablePath(),
							attributeName( identifierMapping )
					);
				}
				else {
					fetchablePath = fetchParent.resolveNavigablePath( fetchable );
				}
			}
			else {
				fetchablePath = fetchParent.resolveNavigablePath( fetchable );
			}

			FetchTiming fetchTiming = fetchable.getMappedFetchOptions().getTiming();
			boolean joined = fetchable.getMappedFetchOptions().getStyle() == FetchStyle.JOIN;
			boolean explicitFetch = false;
			EntityGraphTraversalState.TraversalResult traversalResult = null;

			final boolean isFetchablePluralAttributeMapping = isABag || isPluralAttributeMapping( fetchable );
			final Integer maximumFetchDepth = creationContext.getMaximumFetchDepth();

			if ( !( fetchable instanceof CollectionPart ) ) {
				// 'entity graph' takes precedence over 'fetch profile'
				if ( entityGraphTraversalState != null ) {
					traversalResult = entityGraphTraversalState.traverse( fetchParent, fetchable, isKeyFetchable );
					EntityGraphTraversalState.FetchStrategy fetchStrategy = traversalResult.getFetchStrategy();
					if ( fetchStrategy != null ) {
						fetchTiming = fetchStrategy.getFetchTiming();
						joined = fetchStrategy.isJoined();
						explicitFetch = shouldExplicitFetch( maximumFetchDepth, fetchable, creationState );
					}
				}
				else if ( loadQueryInfluencers.hasEnabledFetchProfiles() ) {
					// There is no point in checking the fetch profile if it can't affect this fetchable
					if ( fetchTiming != FetchTiming.IMMEDIATE || fetchable.incrementFetchDepth() ) {
						final String fetchableRole = fetchable.getNavigableRole().getFullPath();

						for ( String enabledFetchProfileName : loadQueryInfluencers.getEnabledFetchProfileNames() ) {
							final FetchProfile enabledFetchProfile = creationContext.getSessionFactory()
									.getFetchProfile( enabledFetchProfileName );
							final org.hibernate.engine.profile.Fetch profileFetch =
									enabledFetchProfile.getFetchByRole( fetchableRole );
							if ( profileFetch != null ) {
								fetchTiming = profileFetch.getTiming();
								joined = joined || profileFetch.getMethod() == FetchStyle.JOIN;
								explicitFetch = shouldExplicitFetch( maximumFetchDepth, fetchable, creationState );
							}
						}
					}
				}
				else if ( loadQueryInfluencers.getEnabledCascadingFetchProfile() != null ) {
					final CascadeStyle cascadeStyle = fetchable.asAttributeMapping() != null ?
							fetchable.asAttributeMapping().getAttributeMetadata().getCascadeStyle() :
							null;
					final CascadingAction<?> cascadingAction =
							loadQueryInfluencers.getEnabledCascadingFetchProfile().getCascadingAction();
					if ( cascadeStyle == null || cascadeStyle.doCascade( cascadingAction ) ) {
						fetchTiming = FetchTiming.IMMEDIATE;
						// In 5.x the CascadeEntityJoinWalker only join fetched the first collection fetch
						joined = !isFetchablePluralAttributeMapping || rowCardinality == RowCardinality.SINGLE;
					}
				}
			}

			if ( joined && isFetchablePluralAttributeMapping ) {
				switch ( rowCardinality ) {
					case SET:
						joined = !isABag;
						break;
					case BAG:
						joined = false;
						break;
				}
			}

			try {
				if ( fetchable.incrementFetchDepth() ) {
					fetchDepth++;
				}

				// There is no need to check for circular fetches if this is an explicit fetch
				if ( !explicitFetch && !creationState.isResolvingCircularFetch() ) {
					final Fetch biDirectionalFetch = fetchable.resolveCircularFetch(
							fetchablePath,
							fetchParent,
							fetchTiming,
							creationState
					);

					if ( biDirectionalFetch != null ) {
						fetches.add( biDirectionalFetch );
						return;
					}
				}

				if ( maximumFetchDepth != null ) {
					if ( fetchDepth == maximumFetchDepth + 1 ) {
						joined = false;
					}
					else if ( fetchDepth > maximumFetchDepth + 1 ) {
						// We can preserve the existing value of joined for basic and embedded values
						if ( fetchable.asBasicValuedModelPart() == null && !( fetchable instanceof EmbeddedAttributeMapping ) ) {
							joined = false;
						}
					}
				}

				if ( joined && isFetchablePluralAttributeMapping ) {
					rowCardinality = isABag ? RowCardinality.BAG : RowCardinality.SET;
				}

				final Fetch fetch = fetchParent.generateFetchableFetch(
						fetchable,
						fetchablePath,
						fetchTiming,
						joined,
						null,
						creationState
				);

				if ( fetch.getTiming() == FetchTiming.IMMEDIATE && joined ) {
					if ( isFetchablePluralAttributeMapping ) {
						final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) fetchable;
						final QuerySpec querySpec = creationState.getInflightQueryPart().getFirstQuerySpec();
						applyOrdering(
								querySpec,
								fetchablePath,
								pluralAttributeMapping,
								creationState
						);
					}
				}

				fetches.add( fetch );
			}
			finally {
				if ( fetchable.incrementFetchDepth() ) {
					fetchDepth--;
				}
				if ( entityGraphTraversalState != null && traversalResult != null ) {
					entityGraphTraversalState.backtrack( traversalResult );
				}
			}
		};
	}

	private boolean shouldExplicitFetch(Integer maxFetchDepth, Fetchable fetchable, LoaderSqlAstCreationState creationState) {
		/*
			Forcing the value of explicitFetch to true will disable the fetch circularity check and
			for already visited association or collection this will cause a StackOverflow if maxFetchDepth is null, see HHH-15391.
		 */
		if ( maxFetchDepth == null ) {
			if ( fetchable instanceof ToOneAttributeMapping ) {
				return !creationState.isAssociationKeyVisited(
						( (ToOneAttributeMapping) fetchable ).getForeignKeyDescriptor().getAssociationKey()
				);
			}
			else if ( fetchable instanceof PluralAttributeMapping ) {
				return !creationState.isAssociationKeyVisited(
						( (PluralAttributeMapping) fetchable ).getKeyDescriptor().getAssociationKey()
				);
			}
		}

		return true;
	}

	private void applyOrdering(
			QuerySpec querySpec,
			NavigablePath navigablePath,
			PluralAttributeMapping pluralAttributeMapping,
			LoaderSqlAstCreationState sqlAstCreationState) {
		assert pluralAttributeMapping.getAttributeName().equals( navigablePath.getLocalName() );

		final TableGroup tableGroup = sqlAstCreationState.getFromClauseAccess().getTableGroup( navigablePath );
		assert tableGroup != null;

		applyOrdering( querySpec, tableGroup, pluralAttributeMapping, sqlAstCreationState );
	}

	private SelectStatement generateSelect(SubselectFetch subselect) {

		// todo (6.0) : we could even convert this to a join by piecing together
		//		parts from the subselect-fetch sql-ast.  e.g. today we do:
		// 			select ...
		// 			from collection_table c
		// 			where c.fk in (
		//      		select o.id
		//				from owner_table o
		//				where <original restriction>
		// 			)
		//  	but instead could do:
		// 			select ...
		// 			from owner_table o
		//				left join collection_table c on c.fk = o.id
		// 			where <original restriction>

		// just like with other load-paths, bag-mappings can potentially be problematic here

		// todo (6.0) : ^^ another interesting idea is to use `partsToSelect` here relative to the owner
		//		- so `loadable` is the owner entity-descriptor and the `partsToSelect` is the collection

		assert loadable instanceof PluralAttributeMapping;

		final PluralAttributeMapping attributeMapping = (PluralAttributeMapping) loadable;

		final QuerySpec rootQuerySpec = new QuerySpec( true );

		final NavigablePath rootNavigablePath = new NavigablePath( loadable.getRootPathName() );

		// We need to initialize the acronymMap based on subselect.getLoadingSqlAst() to avoid alias collisions
		final Map<String, TableReference> tableReferences = AliasCollector.getTableReferences( subselect.getLoadingSqlAst() );
		final LoaderSqlAstCreationState sqlAstCreationState = new LoaderSqlAstCreationState(
				rootQuerySpec,
				new SqlAliasBaseManager( tableReferences.keySet() ),
				new SimpleFromClauseAccessImpl(),
				lockOptions,
				this::visitFetches,
				numberOfKeysToLoad > 1,
				loadQueryInfluencers,
				creationContext
		);

		final TableGroup rootTableGroup = buildRootTableGroup( rootNavigablePath, rootQuerySpec, sqlAstCreationState );

		// generate and apply the restriction
		applySubSelectRestriction(
				rootQuerySpec,
				rootTableGroup,
				subselect,
				sqlAstCreationState
		);

		// NOTE : no need to check - we are explicitly processing a plural-attribute
		applyFiltering( rootQuerySpec, rootTableGroup, attributeMapping, sqlAstCreationState );
		applyOrdering( rootQuerySpec, rootTableGroup, attributeMapping, sqlAstCreationState );

		// register the jdbc-parameters
		// todo (6.0) : analyzing the call paths, it seems like `jdbcParameterConsumer`
		//		never does anything for sub-select-fetch select building.
		//subselect.getLoadingJdbcParameters().forEach( jdbcParameterConsumer );

		return new SelectStatement(
				rootQuerySpec,
				singletonList(
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
			TableGroup rootTableGroup,
			SubselectFetch subselect,
			LoaderSqlAstCreationState sqlAstCreationState) {
		assert loadable instanceof PluralAttributeMapping;

		final PluralAttributeMapping attributeMapping = (PluralAttributeMapping) loadable;
		final ForeignKeyDescriptor fkDescriptor = attributeMapping.getKeyDescriptor();

		final Expression fkExpression;

		if ( !fkDescriptor.isEmbedded() ) {
			assert fkDescriptor instanceof SimpleForeignKeyDescriptor;
			final SimpleForeignKeyDescriptor simpleFkDescriptor = (SimpleForeignKeyDescriptor) fkDescriptor;
			final TableReference tableReference = rootTableGroup.resolveTableReference(
					null,
					fkDescriptor,
					simpleFkDescriptor.getContainingTableExpression()
			);
			fkExpression = sqlAstCreationState.getSqlExpressionResolver().resolveSqlExpression(
					tableReference,
					simpleFkDescriptor
			);
		}
		else {
			final List<ColumnReference> columnReferences = new ArrayList<>( fkDescriptor.getJdbcTypeCount() );
			fkDescriptor.forEachSelectable(
					(columnIndex, selection) -> {
						final TableReference tableReference = rootTableGroup.resolveTableReference(
								null,
								fkDescriptor,
								selection.getContainingTableExpression()
						);
						columnReferences.add(
								(ColumnReference) sqlAstCreationState.getSqlExpressionResolver()
										.resolveSqlExpression( tableReference, selection )
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
								subselect,
								sqlAstCreationState
						),
						false
				)
		);
	}

	private QueryPart generateSubSelect(
			PluralAttributeMapping attributeMapping,
			SubselectFetch subselect,
			LoaderSqlAstCreationState creationState) {
		final ForeignKeyDescriptor fkDescriptor = attributeMapping.getKeyDescriptor();
		final QuerySpec subQuery = new QuerySpec( false );
		final QuerySpec loadingSqlAst = subselect.getLoadingSqlAst();
		final TableGroup ownerTableGroup = subselect.getOwnerTableGroup();

		// transfer the from-clause
		loadingSqlAst.getFromClause().visitRoots( subQuery.getFromClause()::addRoot );

		final SqlExpressionResolver sqlExpressionResolver = creationState.getSqlExpressionResolver();

		fkDescriptor.visitTargetSelectables(
				(valuesPosition, selection) -> {
					// for each column, resolve a SqlSelection and add it to the sub-query select-clause
					final TableReference tableReference = ownerTableGroup.resolveTableReference( null, fkDescriptor, selection.getContainingTableExpression() );
					final Expression expression = sqlExpressionResolver.resolveSqlExpression(
							tableReference,
							selection
					);
					subQuery.getSelectClause().addSqlSelection(
							new SqlSelectionImpl(
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

	private void registerPluralTableGroupParts(FromClauseAccess fromClauseAccess, TableGroup tableGroup) {
		if ( tableGroup instanceof PluralTableGroup ) {
			final PluralTableGroup pluralTableGroup = (PluralTableGroup) tableGroup;
			if ( pluralTableGroup.getElementTableGroup() != null ) {
				fromClauseAccess.registerTableGroup(
						pluralTableGroup.getElementTableGroup().getNavigablePath(),
						pluralTableGroup.getElementTableGroup()
				);
			}
			if ( pluralTableGroup.getIndexTableGroup() != null ) {
				fromClauseAccess.registerTableGroup(
						pluralTableGroup.getIndexTableGroup().getNavigablePath(),
						pluralTableGroup.getIndexTableGroup()
				);
			}
		}
	}

	/**
	 * Describes the JDBC result set cardinality per entity result object.
	 */
	private enum RowCardinality {
		/**
		 * Means that there is a single JDBC result row per entity result object.
		 */
		SINGLE,
		/**
		 * Means there are multiple JDBC result rows per entity result object,
		 * but the aggregation of rows is not affected the result cardinality.
		 */
		SET,
		/**
		 * Means there are multiple JDBC result rows per entity result object,
		 * but the aggregation of rows is dependent on the result cardinality.
		 */
		BAG
	}
}

