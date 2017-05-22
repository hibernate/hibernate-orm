/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.produce.sqm.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.EntityGraphImplementor;
import org.hibernate.graph.spi.AttributeNodeContainer;
import org.hibernate.hql.internal.ast.tree.OrderByClause;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEntity;
import org.hibernate.metamodel.model.domain.spi.EntityTypeImplementor;
import org.hibernate.metamodel.model.domain.spi.JoinablePersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.query.spi.EntityGraphQueryHint;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.JoinedTableGroupContext;
import org.hibernate.sql.ast.produce.metamodel.spi.RootTableGroupContext;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.produce.metamodel.spi.TableGroupResolver;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.consume.spi.BaseSemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.SqmQuerySpec;
import org.hibernate.query.sqm.tree.SqmSelectStatement;
import org.hibernate.query.sqm.tree.SqmUpdateStatement;
import org.hibernate.query.sqm.tree.expression.BinaryArithmeticSqmExpression;
import org.hibernate.query.sqm.tree.expression.CaseSearchedSqmExpression;
import org.hibernate.query.sqm.tree.expression.CaseSimpleSqmExpression;
import org.hibernate.query.sqm.tree.expression.CoalesceSqmExpression;
import org.hibernate.query.sqm.tree.expression.ConcatSqmExpression;
import org.hibernate.query.sqm.tree.expression.ConstantEnumSqmExpression;
import org.hibernate.query.sqm.tree.expression.ConstantFieldSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralBigDecimalSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralBigIntegerSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralCharacterSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralDoubleSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralFalseSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralFloatSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralIntegerSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralLongSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralNullSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralStringSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralTrueSqmExpression;
import org.hibernate.query.sqm.tree.expression.NamedParameterSqmExpression;
import org.hibernate.query.sqm.tree.expression.NullifSqmExpression;
import org.hibernate.query.sqm.tree.expression.PositionalParameterSqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.UnaryOperationSqmExpression;
import org.hibernate.query.sqm.tree.expression.domain.SqmAttributeReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmPluralAttributeReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReference;
import org.hibernate.query.sqm.tree.expression.function.AvgFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.CountFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.CountStarFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.MaxFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.MinFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.SumFunctionSqmExpression;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmFromElementSpace;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.order.SqmOrderByClause;
import org.hibernate.query.sqm.tree.order.SqmSortSpecification;
import org.hibernate.query.sqm.tree.paging.SqmLimitOffsetClause;
import org.hibernate.query.sqm.tree.predicate.AndSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.BetweenSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.GroupedSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.InListSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.InSubQuerySqmPredicate;
import org.hibernate.query.sqm.tree.predicate.LikeSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.NegatedSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.NullnessSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.OrSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.RelationalPredicateOperator;
import org.hibernate.query.sqm.tree.predicate.RelationalSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationArgument;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationTarget;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.consume.results.internal.SqlSelectionImpl;
import org.hibernate.sql.ast.consume.results.spi.SqlSelectionGroup;
import org.hibernate.sql.ast.produce.SyntaxException;
import org.hibernate.sql.ast.produce.internal.SqlSelectPlanImpl;
import org.hibernate.sql.ast.produce.result.internal.FetchCompositeAttributeImpl;
import org.hibernate.sql.ast.produce.result.internal.FetchEntityAttributeImpl;
import org.hibernate.sql.ast.produce.result.spi.FetchParent;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.result.spi.QueryResultDynamicInstantiation;
import org.hibernate.sql.ast.produce.result.spi.SqlSelectionResolver;
import org.hibernate.sql.ast.produce.spi.FromClauseIndex;
import org.hibernate.sql.ast.produce.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.produce.spi.SqlAstBuildingContext;
import org.hibernate.sql.ast.produce.spi.SqlSelectPlan;
import org.hibernate.sql.ast.produce.sqm.internal.FetchGraphBuilder;
import org.hibernate.sql.ast.tree.spi.Clause;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.SelectStatement;
import org.hibernate.sql.ast.tree.spi.expression.AvgFunction;
import org.hibernate.sql.ast.tree.spi.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.spi.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.spi.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.tree.spi.expression.CoalesceFunction;
import org.hibernate.sql.ast.tree.spi.expression.ConcatFunction;
import org.hibernate.sql.ast.tree.spi.expression.CountFunction;
import org.hibernate.sql.ast.tree.spi.expression.CountStarFunction;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.MaxFunction;
import org.hibernate.sql.ast.tree.spi.expression.MinFunction;
import org.hibernate.sql.ast.tree.spi.expression.NamedParameter;
import org.hibernate.sql.ast.tree.spi.expression.NonStandardFunction;
import org.hibernate.sql.ast.tree.spi.expression.NullifFunction;
import org.hibernate.sql.ast.tree.spi.expression.PositionalParameter;
import org.hibernate.sql.ast.tree.spi.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.spi.expression.SumFunction;
import org.hibernate.sql.ast.tree.spi.expression.UnaryOperation;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceSource;
import org.hibernate.sql.ast.tree.spi.expression.instantiation.DynamicInstantiation;
import org.hibernate.sql.ast.tree.spi.from.EntityTableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;
import org.hibernate.sql.ast.tree.spi.predicate.BetweenPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.GroupedPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.ast.tree.spi.predicate.LikePredicate;
import org.hibernate.sql.ast.tree.spi.predicate.NegatedPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.sql.ast.tree.spi.predicate.RelationalPredicate;
import org.hibernate.sql.ast.tree.spi.select.SelectClause;
import org.hibernate.sql.ast.tree.spi.select.Selection;
import org.hibernate.sql.ast.tree.spi.select.SqlSelectable;
import org.hibernate.sql.ast.tree.spi.select.SqlSelection;
import org.hibernate.sql.ast.tree.spi.sort.SortSpecification;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.StandardSpiBasicTypes;

import org.jboss.logging.Logger;

/**
 * Interprets an SqmSelectStatement as a SQL-AST SelectQuery.
 *
 * @author Steve Ebersole
 * @author John O'Hara
 */
@SuppressWarnings("unchecked")
public class SqmSelectToSqlAstConverter
		extends BaseSemanticQueryWalker
		implements SqlSelectionResolver, QueryResultCreationContext {
	private static final Logger log = Logger.getLogger( SqmSelectToSqlAstConverter.class );

	private FetchGraphBuilder fetchGraphBuilder;

	/**
	 * Main entry point into SQM SelectStatement interpretation
	 *
	 * @param statement The SQM SelectStatement to interpret
	 * @param queryOptions The options to be applied to the interpretation
	 * @param sqlAstBuildingContext Contextual information ("parameter object") for
	 * information needed as a SQL AST is produced
	 *
	 * @return The interpretation
	 */
	public static SqlSelectPlan interpret(
			SqmSelectStatement statement,
			QueryOptions queryOptions,
			SqlAstBuildingContext sqlAstBuildingContext) {
		return new SqmSelectToSqlAstConverter( queryOptions, sqlAstBuildingContext )
				.interpret( statement );
	}


	private final SqlAstBuildingContext sqlAstBuildingContext;
	private final QueryOptions queryOptions;


	private final FromClauseIndex fromClauseIndex = new FromClauseIndex();
	private final SqlAliasBaseManager sqlAliasBaseManager = new SqlAliasBaseManager();
	private final Stack<Clause> currentClauseStack = new Stack<>();
	private final Stack<QuerySpec> querySpecStack = new Stack<>();
	private final Stack<Shallowness> shallownessStack = new Stack<>( Shallowness.NONE );

	private final List<QueryResult> queryResults = new ArrayList<>();

	private enum Shallowness {
		NONE,
		CTOR,
		FUNCTION,
		SUBQUERY;
	}


	private SqmSelectToSqlAstConverter(
			QueryOptions queryOptions,
			SqlAstBuildingContext sqlAstBuildingContext) {
		this.queryOptions = queryOptions;
		this.sqlAstBuildingContext = sqlAstBuildingContext;
		this.fetchDepthLimit = sqlAstBuildingContext.getSessionFactory().getSessionFactoryOptions().getMaximumFetchDepth();
		this.entityGraphQueryHintType = queryOptions.getEntityGraphQueryHint().getType();
	}

	private SqlSelectPlan interpret(SqmSelectStatement statement) {
		return new SqlSelectPlanImpl(
				visitSelectStatement( statement ),
				queryResults
		);
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return sqlAstBuildingContext.getSessionFactory();
	}

	@Override
	public ColumnReferenceSource currentColumnReferenceSource() {
		return tableGroupStack.getCurrent();
	}

	@Override
	public NavigablePath currentNavigablePath() {
		return navigablePathStack.getCurrent();
	}

	@Override
	public boolean shouldCreateShallowEntityResult() {
		// todo (6.0) : we also need to vary this for ctor result based on ctor sigs + user option
		return shallownessStack.getCurrent() != Shallowness.NONE;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// walker


	@Override
	public Object visitUpdateStatement(SqmUpdateStatement statement) {
		throw new AssertionFailure( "Not expecting UpdateStatement" );
	}

	@Override
	public Object visitDeleteStatement(SqmDeleteStatement statement) {
		throw new AssertionFailure( "Not expecting DeleteStatement" );
	}

	@Override
	public Object visitInsertSelectStatement(SqmInsertSelectStatement statement) {
		throw new AssertionFailure( "Not expecting DeleteStatement" );
	}

	@Override
	public SelectStatement visitSelectStatement(SqmSelectStatement statement) {
		final QuerySpec querySpec = visitQuerySpec( statement.getQuerySpec() );

		return new SelectStatement( querySpec );
	}

	@Override
	public OrderByClause visitOrderByClause(SqmOrderByClause orderByClause) {
		throw new AssertionFailure( "Unexpected visitor call" );
	}

	@Override
	public SortSpecification visitSortSpecification(SqmSortSpecification sortSpecification) {
		return new SortSpecification(
				(Expression) sortSpecification.getSortExpression().accept( this ),
				sortSpecification.getCollation(),
				sortSpecification.getSortOrder()
		);
	}

	@Override
	public QuerySpec visitQuerySpec(SqmQuerySpec querySpec) {
		final QuerySpec astQuerySpec = new QuerySpec( querySpecStack.isEmpty() );
		querySpecStack.push( astQuerySpec );
		fromClauseIndex.pushFromClause( astQuerySpec.getFromClause() );

		try {
			// we want to visit the from-clause first
			visitFromClause( querySpec.getFromClause() );

			final SqmSelectClause selectClause = querySpec.getSelectClause();
			if ( selectClause != null ) {
				visitSelectClause( selectClause );
			}

			final SqmWhereClause whereClause = querySpec.getWhereClause();
			if ( whereClause != null ) {
				currentClauseStack.push( Clause.WHERE );
				try {
					astQuerySpec.setWhereClauseRestrictions(
							(Predicate) whereClause.getPredicate().accept( this )
					);
				}
				finally {
					currentClauseStack.pop();
				}
			}

			// todo : group-by
			// todo : having

			if ( querySpec.getOrderByClause() != null ) {
				currentClauseStack.push( Clause.ORDER );
				try {
                    for ( SqmSortSpecification sortSpecification : querySpec.getOrderByClause().getSortSpecifications() ) {
						astQuerySpec.addSortSpecification( visitSortSpecification( sortSpecification ) );
                    }
                }
				finally {
                    currentClauseStack.pop();
                }
			}

			final SqmLimitOffsetClause limitOffsetClause = querySpec.getLimitOffsetClause();
			if ( limitOffsetClause != null ) {
				currentClauseStack.push( Clause.LIMIT );
				try {
                    if ( limitOffsetClause.getLimitExpression() != null ) {
                        astQuerySpec.setLimitClauseExpression(
                                (Expression) limitOffsetClause.getLimitExpression().accept( this )
                        );
                    }
                    if ( limitOffsetClause.getOffsetExpression() != null ) {
                        astQuerySpec.setOffsetClauseExpression(
                                (Expression) limitOffsetClause.getOffsetExpression().accept( this )
                        );
                    }
				}
				finally {
					currentClauseStack.pop();
				}
			}

			return astQuerySpec;
		}
		finally {
			assert querySpecStack.pop() == astQuerySpec;
			assert fromClauseIndex.popFromClause() == astQuerySpec.getFromClause();
		}
	}

	@Override
	public Void visitFromClause(SqmFromClause fromClause) {
		currentClauseStack.push( Clause.FROM );
		try {
			fromClause.getFromElementSpaces().forEach( this::visitFromElementSpace );
		}
		finally {
			currentClauseStack.pop();
		}
		return null;
	}

	private TableSpace tableSpace;

	@Override
	public TableSpace visitFromElementSpace(SqmFromElementSpace fromElementSpace) {
		tableSpace = fromClauseIndex.currentFromClause().makeTableSpace();
		try {
			visitRootEntityFromElement( fromElementSpace.getRoot() );
			for ( SqmJoin sqmJoin : fromElementSpace.getJoins() ) {
				tableSpace.addJoinedTableGroup( (TableGroupJoin) sqmJoin.accept( this ) );
			}
			return tableSpace;
		}
		finally {
			tableSpace = null;
		}
	}

	@Override
	public Object visitRootEntityFromElement(SqmRoot sqmRoot) {
		log.tracef( "Starting resolution of SqmRoot [%s] to TableGroup", sqmRoot );

		if ( fromClauseIndex.isResolved( sqmRoot ) ) {
			final TableGroup resolvedTableGroup = fromClauseIndex.findResolvedTableGroup( sqmRoot );
			log.tracef( "SqmRoot [%s] resolved to existing TableGroup [%s]", sqmRoot, resolvedTableGroup );
			return resolvedTableGroup;
		}

		final SqmEntityReference binding = sqmRoot.getNavigableReference();
		final EntityTypeImplementor entityMetadata = (EntityTypeImplementor) binding.getReferencedNavigable();
		final EntityTableGroup group = entityMetadata.createRootTableGroup(
				new RootTableGroupContext() {
					@Override
					public QuerySpec getQuerySpec() {
						return currentQuerySpec();
					}

					@Override
					public TableSpace getTableSpace() {
						return tableSpace;
					}

					@Override
					public String getUniqueIdentifier() {
						return sqmRoot.getUniqueIdentifier();
					}

					@Override
					public String getIdentificationVariable() {
						return sqmRoot.getIdentificationVariable();
					}

					@Override
					public EntityTypeImplementor getIntrinsicSubclassEntityMetadata() {
						return sqmRoot.getIntrinsicSubclassEntityMetadata();
					}

					@Override
					public void addRestriction(Predicate predicate) {
						currentQuerySpec().addRestriction( predicate );
					}

					@Override
					public TableGroupResolver getTableGroupResolver() {
						return fromClauseIndex;
					}

					@Override
					public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
						return sqlAliasBaseManager;
					}

					@Override
					public JoinType getTableReferenceJoinType() {
						// TableReferences within the TableGroup can be
						// inner-joined (unless they are optional, which is handled
						// inside the producers)
						return JoinType.INNER;
					}
				}
		);
		tableSpace.setRootTableGroup( group );
		tableGroupStack.push( group );

		log.tracef( "Resolved SqmRoot [%s] to new TableGroup [%s]", sqmRoot, group );

		return group;
	}

	@Override
	public Object visitQualifiedAttributeJoinFromElement(SqmAttributeJoin joinedFromElement) {
		if ( fromClauseIndex.isResolved( joinedFromElement ) ) {
			return fromClauseIndex.findResolvedTableGroup( joinedFromElement );
		}

		final QuerySpec querySpec = currentQuerySpec();
		final JoinablePersistentAttribute joinableAttribute = (JoinablePersistentAttribute) joinedFromElement.getAttributeReference()
				.getReferencedNavigable();
		final TableGroupJoin tableGroupJoin = joinableAttribute.createTableGroupJoin(
				joinedFromElement.getNavigableReference(),
				joinedFromElement.getJoinType(),
				new JoinedTableGroupContext() {
					@Override
					public QuerySpec getQuerySpec() {
						return querySpec;
					}

					@Override
					public TableSpace getTableSpace() {
						return tableSpace;
					}

					@Override
					public String getUniqueIdentifier() {
						return joinedFromElement.getUniqueIdentifier();
					}

					@Override
					public String getIdentificationVariable() {
						return joinedFromElement.getIdentificationVariable();
					}

					@Override
					public EntityTypeImplementor getIntrinsicSubclassEntityMetadata() {
						return joinedFromElement.getIntrinsicSubclassEntityMetadata();
					}

					@Override
					public TableGroupResolver getTableGroupResolver() {
						return fromClauseIndex;
					}

					@Override
					public JoinType getTableReferenceJoinType() {
						// TableReferences within the joined TableGroup can be
						// inner-joined (unless they are optional, which is handled
						// inside the producers)
						return JoinType.INNER;
					}

					@Override
					public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
						return sqlAliasBaseManager;
					}
				}
		);

		// add any additional join restrictions
		if ( joinedFromElement.getOnClausePredicate() != null ) {
			currentQuerySpec().addRestriction(
					(Predicate) joinedFromElement.getOnClausePredicate().accept( this )
			);
		}

		tableGroupStack.push( tableGroupJoin.getJoinedGroup() );

		return tableGroupJoin;
	}

	@Override
	public TableGroupJoin visitCrossJoinedFromElement(SqmCrossJoin joinedFromElement) {
		final QuerySpec querySpec = currentQuerySpec();
		final EntityTypeImplementor entityPersister = joinedFromElement.getIntrinsicSubclassEntityMetadata();
		final EntityTableGroup group = entityPersister.createRootTableGroup(
				new RootTableGroupContext() {
					@Override
					public QuerySpec getQuerySpec() {
						return querySpec;
					}

					@Override
					public TableSpace getTableSpace() {
						return tableSpace;
					}

					@Override
					public String getUniqueIdentifier() {
						return joinedFromElement.getUniqueIdentifier();
					}

					@Override
					public String getIdentificationVariable() {
						return null;
					}

					@Override
					public EntityTypeImplementor getIntrinsicSubclassEntityMetadata() {
						return null;
					}

					@Override
					public TableGroupResolver getTableGroupResolver() {
						return fromClauseIndex;
					}

					@Override
					public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
						return sqlAliasBaseManager;
					}

					@Override
					public JoinType getTableReferenceJoinType() {
						// TableReferences within the cross-joined TableGroup can be
						// inner-joined (unless they are optional, which is handled
						// inside the producers)
						return JoinType.INNER;
					}

					@Override
					public void addRestriction(Predicate predicate) {
						log.debugf(
								"Adding restriction [%s] to where-clause for cross-join [%s]",
								predicate,
								joinedFromElement.getNavigableReference()
						);
						querySpec.addRestriction( predicate );
					}
				}
		);

		tableGroupStack.push( group );

		return new TableGroupJoin( SqmJoinType.CROSS, group, null );
	}

	@Override
	public Object visitQualifiedEntityJoinFromElement(SqmEntityJoin joinedFromElement) {
		throw new NotYetImplementedException();
	}

	@Override
	public SelectClause visitSelectClause(SqmSelectClause selectClause) {
		currentClauseStack.push( Clause.SELECT );
		shallownessStack.push( Shallowness.SUBQUERY );
		try {
			super.visitSelectClause( selectClause );
			currentQuerySpec().getSelectClause().makeDistinct( selectClause.isDistinct() );
			return currentQuerySpec().getSelectClause();
		}
		finally {
			shallownessStack.pop();
			currentClauseStack.pop();
		}
	}

	private QuerySpec currentQuerySpec() {
		return querySpecStack.getCurrent();
	}

	@Override
	public Selection visitSelection(SqmSelection sqmSelection) {
		final Expression expression = (Expression) sqmSelection.getExpression().accept( this );
		final Selection selection = expression.getSelectable().createSelection(
				expression,
				sqmSelection.getAlias()
		);
		currentQuerySpec().getSelectClause().selection( selection );

		// the call to Expression to resolve the ColumnReferenceResolver
		//		allows polymorphic input per Expression type.  E.g.
		//		a functions
		//
		// todo (6.0) : just pass TableGroupResolver into Selection#createQueryResult
		//		still allows access to TableGroup/ColumnReference resolution for
		//		Selection/Expression/Selectables that need it
		final QueryResult queryResult = selection.createQueryResult( this, this );
		queryResults.add( queryResult );
		applyFetches( queryResult );

		return selection;
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Fetches

	private final EntityGraphQueryHint.Type entityGraphQueryHintType;

	private final int fetchDepthLimit;

	private Stack<FetchParent> fetchParentStack = new Stack<>();
	private Stack<NavigablePath> navigablePathStack = new Stack<>();
	private final Stack<TableGroup> tableGroupStack = new Stack<>();
	private Stack<SqmFrom> sqmFromStack = new Stack<>();
	private Stack<AttributeNodeContainer> entityGraphNodeStack = new Stack<>();


	protected void applyFetches(QueryResult queryReturn) {
		if ( !FetchParent.class.isInstance( queryReturn ) ) {
			return;
		}

		applyFetches( (FetchParent) queryReturn );
	}

	protected void applyFetches(FetchParent fetchParent) {
		fetchParentStack.push( fetchParent );
		navigablePathStack.push( fetchParent.getNavigablePath() );

		fetchParent.
		try {

		}
		finally {
			navigablePathStack.pop();
			fetchParentStack.pop();
		}

		final FetchGraphBuilder fetchGraphBuilder = new FetchGraphBuilder(
				sqlAstBuildingContext,
				this,
				fetchParent,
				queryOptions.getEntityGraphQueryHint(),
				navigablePathStack.getCurrent(),
				this,
				this
		);

		fetchParent.getNavigableContainerReference().getNavigable().visitNavigable( fetchGraphBuilder );

		if ( queryReturn instanceof QueryResultDynamicInstantiation ) {
			// todo (6.0) : ? - dynamic-instantiations *if* the dynamic-instantiation takes the entity as an argument?
			//		wuold be nice, but not critical
		}

		// otherwise, nothing to do
	}

	private EntityGraphImplementor extractEntityGraph() {
		if ( queryOptions.getEntityGraphQueryHint() == null ) {
			return null;
		}
		else {
			return queryOptions.getEntityGraphQueryHint().getHintedGraph();
		}
	}

	private Set<String> alreadyProcessedFetchParentTableGroupUids = new HashSet<>();

	private void applyFetches(FetchParent fetchParent, EntityGraphImplementor entityGraph) {
		// todo (6.0) : SUPERSEDES ALL NOTES BELOW!!!
		//		I think this is now as simple as:
		//			1) adding a Stack<FetchParent> field
		//			2) adding the incoming FetchParent to that Stack
		//			3) create a specialized NavigableVisitationStrategy and use walking
		//			4) keep some mapping of "fetch parent" -> query-defined fetches.
		//				currently `FromClauseIndex#sqmFetchesByParentUid` was the intended wy
		// 				to handle this, but it would be better if SQM builder could
		//				inject these into the SQM nodes.  We'd need some form of
		//				"FetchableSqmNode" and "FetchableContainerSqmNode".  Criteria
		//				already has this structure so it already works from there, especially
		//				if we have Criteria nodes either:
		//					a) extend the SQM nodes
		//					b) internally build the SQM nodes (simple delegation from the JPA APIs)
		//				we'd have to link them via HQL processing - probably via a FetchableContainerSqmNode
		//				Stack in that code and linking them together as we process each fetch
		//
		//		this allows us to clean up FromClauseIndex even more
		//
		//		in this design the NavigableVisitationStrategy would be the thing responsible for
		//			keeping the depth in sync in terms of:
		// 				1) navigable we are processing
		//				2) corresponding EntityGraph


		fetchParent.getNavigableContainerReference().getNavigable().visitNavigables(  );


		final String uniqueIdentifier = tableGroupStack.getCurrent().getUniqueIdentifier();
		if ( !alreadyProcessedFetchParentTableGroupUids.add( uniqueIdentifier ) ) {
			log.errorf( "Found duplicate tableGroupUid as FetchParent [%s]", uniqueIdentifier );
			return;
		}

		// todo : to do this where we are going to need a way to get all of the attributes related to this FetchParent
		//		that way we can drive this process across all of the attributes defined for the
		//		FetchParent type at once (one iteration)

		// todo : look to define a vistor-based walker
		//		I think this (^^) helps too with recognizing graph circularities.  Peek at how load-plans
		//		recognize such circularities.
		//
		//		Possibly add a AttributeNodeImplementor#applyFetches method (returning Subgraphs?)

		// todo : fetches coming from an EntityGraph most likely need a "from element" (TableGroup)

		final List<SqmAttributeJoin> fetchedJoins = fromClauseIndex.findFetchesByUniqueIdentifier( uniqueIdentifier );


		for ( SqmAttributeJoin fetchedJoin : fetchedJoins ) {
			final SqmAttributeReference fetchedAttributeBinding = fetchedJoin.getAttributeReference();
			// todo  : need this method added to EntityGraphImplementor
			//final String attributeName = fetchedAttributeBinding.getAttribute().getAttributeName();
			//final AttributeNodeImplementor attributeNode = entityGraphImplementor.findAttributeNode( attributeName );
			final AttributeNodeImplementor attributeNode = null;
			applyFetches( fetchParent, fetchedJoin, attributeNode );
		}
	}

	private void applyFetches(FetchParent fetchParent, SqmAttributeJoin attributeJoin, AttributeNodeImplementor attributeNode) {
		if ( attributeJoin.getAttributeReference() instanceof SqmPluralAttributeReference ) {
			// apply the plural attribute fetch join
			final SqmPluralAttributeReference pluralAttributeBinding = (SqmPluralAttributeReference) attributeJoin.getAttributeReference();

			// todo : work out how to model collection fetches...
			//		mainly... do we need a "grouping" fetch?  And if so, should
			// 		CollectionAttributeFetch expose its element versus key fetches individually?  Or
			// 		does it represent each by itself?

			throw new NotYetImplementedException( "collection fetched not yet implemented" );
		}
		else if ( attributeJoin.getAttributeReference() instanceof SqmSingularAttributeReference ) {
			// apply the singular attribute fetch join
			final SqmSingularAttributeReference attributeBinding = (SqmSingularAttributeReference) attributeJoin.getAttributeReference();
			final SingularPersistentAttribute boundAttribute = (SingularPersistentAttribute) attributeBinding.getReferencedNavigable();

			switch ( boundAttribute.getAttributeTypeClassification() ) {
				case ANY:
				case BASIC: {
					throw new SyntaxException( "Attributes of BASIC or ANY type cannot be joined" );
				}
				case EMBEDDED: {
					final FetchCompositeAttributeImpl fetch = new FetchCompositeAttributeImpl(
							fetchParent,
							(SingularPersistentAttributeEmbedded) boundAttribute,
							new FetchStrategy( FetchTiming.IMMEDIATE, FetchStyle.JOIN )
					);
					fetchParent.addFetch( fetch );
					applyFetches( fetch, null );
					break;
				}
				case ONE_TO_ONE:
				case MANY_TO_ONE: {
					final SingularPersistentAttributeEntity boundAttributeAsEntity = (SingularPersistentAttributeEntity) boundAttribute;
					final FetchEntityAttributeImpl fetch = new FetchEntityAttributeImpl(
							fetchParent,
							currentNavigablePath().append( attributeBinding.getReferencedNavigable().getName() ),
							attributeJoin.getUniqueIdentifier(),
							boundAttributeAsEntity,
							boundAttributeAsEntity.getAssociatedEntityDescriptor(),
							new FetchStrategy( FetchTiming.IMMEDIATE, FetchStyle.JOIN )
					);
					fetchParent.addFetch( fetch );
					applyFetches( fetch, null );
				}
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public DynamicInstantiation visitDynamicInstantiation(SqmDynamicInstantiation dynamicInstantiation) {
		final Class target = interpret( dynamicInstantiation.getInstantiationTarget() );
		final DynamicInstantiation sqlTree = new DynamicInstantiation( target );

		for ( SqmDynamicInstantiationArgument argument : dynamicInstantiation.getArguments() ) {
			validateDynamicInstantiationArgument( target, argument );

			// generate the SqlSelections (if any) and get the SQL AST Expression
			final Expression expr = (Expression) argument.getExpression().accept( this );

			// now build the ArgumentReader and inject into the SQL AST DynamicInstantiation
			sqlTree.addArgument( argument.getAlias(), expr );
		}

		sqlTree.complete();

		return sqlTree;
	}

	@SuppressWarnings("unused")
	private void validateDynamicInstantiationArgument(Class target, SqmDynamicInstantiationArgument argument) {
		// validate use of aliases
		// todo : I think this ^^ is lready handled elsewhere
	}

	private Class interpret(SqmDynamicInstantiationTarget instantiationTarget) {
		if ( instantiationTarget.getNature() == SqmDynamicInstantiationTarget.Nature.LIST ) {
			return List.class;
		}
		if ( instantiationTarget.getNature() == SqmDynamicInstantiationTarget.Nature.MAP ) {
			return Map.class;
		}
		return instantiationTarget.getJavaType();
	}


//	@Override
//	public DomainReferenceExpression visitAttributeReferenceExpression(AttributeBinding attributeBinding) {
//		if ( attributeBinding instanceof PluralAttributeBinding ) {
//			return getCurrentDomainReferenceExpressionBuilder().buildPluralAttributeExpression(
//					this,
//					(PluralAttributeBinding) attributeBinding
//			);
//		}
//		else {
//			return getCurrentDomainReferenceExpressionBuilder().buildSingularAttributeExpression(
//					this,
//					(SingularAttributeBinding) attributeBinding
//			);
//		}
//	}

	@Override
	public QueryLiteral visitLiteralStringExpression(LiteralStringSqmExpression expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				resolveType( expression.getExpressionType(), StandardSpiBasicTypes.STRING ),
				currentClauseStack.getCurrent() == Clause.SELECT
		);
	}

	private BasicValuedExpressableType resolveType(
			BasicValuedExpressableType expressionType,
			BasicType defaultType) {
		return expressionType != null
				? expressionType
				: defaultType;
	}

	@Override
	public QueryLiteral visitLiteralCharacterExpression(LiteralCharacterSqmExpression expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				resolveType( expression.getExpressionType(), StandardSpiBasicTypes.CHARACTER ),
				currentClauseStack.getCurrent() == Clause.SELECT
		);
	}

	@Override
	public QueryLiteral visitLiteralDoubleExpression(LiteralDoubleSqmExpression expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				resolveType( expression.getExpressionType(), StandardSpiBasicTypes.DOUBLE ),
				currentClauseStack.getCurrent() == Clause.SELECT
		);
	}

	@Override
	public QueryLiteral visitLiteralIntegerExpression(LiteralIntegerSqmExpression expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				resolveType( expression.getExpressionType(), StandardSpiBasicTypes.INTEGER ),
				currentClauseStack.getCurrent() == Clause.SELECT
		);
	}

	@Override
	public QueryLiteral visitLiteralBigIntegerExpression(LiteralBigIntegerSqmExpression expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				resolveType( expression.getExpressionType(), StandardSpiBasicTypes.BIG_INTEGER ),
				currentClauseStack.getCurrent() == Clause.SELECT
		);
	}

	@Override
	public QueryLiteral visitLiteralBigDecimalExpression(LiteralBigDecimalSqmExpression expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				resolveType( expression.getExpressionType(), StandardSpiBasicTypes.BIG_DECIMAL ),
				currentClauseStack.getCurrent() == Clause.SELECT
		);
	}

	@Override
	public QueryLiteral visitLiteralFloatExpression(LiteralFloatSqmExpression expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				resolveType( expression.getExpressionType(), StandardSpiBasicTypes.FLOAT ),
				currentClauseStack.getCurrent() == Clause.SELECT
		);
	}

	@Override
	public QueryLiteral visitLiteralLongExpression(LiteralLongSqmExpression expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				resolveType( expression.getExpressionType(), StandardSpiBasicTypes.LONG ),
				currentClauseStack.getCurrent() == Clause.SELECT
		);
	}

	@Override
	public QueryLiteral visitLiteralTrueExpression(LiteralTrueSqmExpression expression) {
		return new QueryLiteral(
				Boolean.TRUE,
				resolveType( expression.getExpressionType(), StandardSpiBasicTypes.BOOLEAN ),
				currentClauseStack.getCurrent() == Clause.SELECT
		);
	}

	@Override
	public QueryLiteral visitLiteralFalseExpression(LiteralFalseSqmExpression expression) {
		return new QueryLiteral(
				Boolean.FALSE,
				resolveType( expression.getExpressionType(), StandardSpiBasicTypes.BOOLEAN ),
				currentClauseStack.getCurrent() == Clause.SELECT
		);
	}

	@Override
	public QueryLiteral visitLiteralNullExpression(LiteralNullSqmExpression expression) {
		return new QueryLiteral(
				null,
				expression.getExpressionType(),
				currentClauseStack.getCurrent() == Clause.SELECT
		);
	}

	@Override
	public Object visitConstantEnumExpression(ConstantEnumSqmExpression expression) {
		return new QueryLiteral(
				expression.getValue(),
				expression.getExpressionType(),
				currentClauseStack.getCurrent() == Clause.SELECT
		);
	}

	@Override
	public Object visitConstantFieldExpression(ConstantFieldSqmExpression expression) {
		return new QueryLiteral(
				expression.getValue(),
				expression.getExpressionType(),
				currentClauseStack.getCurrent() == Clause.SELECT
		);
	}

	@Override
	public NamedParameter visitNamedParameterExpression(NamedParameterSqmExpression expression) {
		return new NamedParameter(
				expression.getName(),
				expression.getExpressionType()
		);
	}

	@Override
	public PositionalParameter visitPositionalParameterExpression(PositionalParameterSqmExpression expression) {
		return new PositionalParameter(
				expression.getPosition(),
				expression.getExpressionType()
		);
	}

	@Override
	public AvgFunction visitAvgFunction(AvgFunctionSqmExpression expression) {
		shallownessStack.push( Shallowness.FUNCTION );

		try {
			return new AvgFunction(
					(Expression) expression.getArgument().accept( this ),
					expression.isDistinct(),
					expression.getExpressionType()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public MaxFunction visitMaxFunction(MaxFunctionSqmExpression expression) {
		shallownessStack.push( Shallowness.FUNCTION );

		try {
			return new MaxFunction(
					(Expression) expression.getArgument().accept( this ),
					expression.isDistinct(),
					expression.getExpressionType()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public MinFunction visitMinFunction(MinFunctionSqmExpression expression) {
		shallownessStack.push( Shallowness.FUNCTION );

		try {
			return new MinFunction(
					(Expression) expression.getArgument().accept( this ),
					expression.isDistinct(),
					expression.getExpressionType()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public SumFunction visitSumFunction(SumFunctionSqmExpression expression) {
		shallownessStack.push( Shallowness.FUNCTION );

		try {
			return new SumFunction(
					(Expression) expression.getArgument().accept( this ),
					expression.isDistinct(),
					expression.getExpressionType()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public CountFunction visitCountFunction(CountFunctionSqmExpression expression) {
		shallownessStack.push( Shallowness.FUNCTION );

		try {
			return new CountFunction(
					(Expression) expression.getArgument().accept( this ),
					expression.isDistinct(),
					expression.getExpressionType()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public CountStarFunction visitCountStarFunction(CountStarFunctionSqmExpression expression) {
		shallownessStack.push( Shallowness.FUNCTION );

		try {
			return new CountStarFunction(
					expression.isDistinct(),
					expression.getExpressionType()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public Object visitUnaryOperationExpression(UnaryOperationSqmExpression expression) {
		shallownessStack.push( Shallowness.FUNCTION );

		try {
			return new UnaryOperation(
					interpret( expression.getOperation() ),
					(Expression) expression.getOperand().accept( this ),
					expression.getExpressionType()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	private UnaryOperation.Operator interpret(UnaryOperationSqmExpression.Operation operation) {
		switch ( operation ) {
			case PLUS: {
				return UnaryOperation.Operator.PLUS;
			}
			case MINUS: {
				return UnaryOperation.Operator.MINUS;
			}
		}

		throw new IllegalStateException( "Unexpected UnaryOperationExpression Operation : " + operation );
	}

	@Override
	public Expression visitBinaryArithmeticExpression(BinaryArithmeticSqmExpression expression) {
		shallownessStack.push( Shallowness.FUNCTION );

		try {
			if ( expression.getOperation() == BinaryArithmeticSqmExpression.Operation.MODULO ) {
				return new NonStandardFunction(
						"mod",
						null, //(BasicType) extractOrmType( expression.getExpressionType() ),
						(Expression) expression.getLeftHandOperand().accept( this ),
						(Expression) expression.getRightHandOperand().accept( this )
				);
			}
			return new BinaryArithmeticExpression(
					interpret( expression.getOperation() ),
					(Expression) expression.getLeftHandOperand().accept( this ),
					(Expression) expression.getRightHandOperand().accept( this ),
					(BasicValuedExpressableType) expression.getExpressionType()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	private BinaryArithmeticExpression.Operation interpret(BinaryArithmeticSqmExpression.Operation operation) {
		switch ( operation ) {
			case ADD: {
				return BinaryArithmeticExpression.Operation.ADD;
			}
			case SUBTRACT: {
				return BinaryArithmeticExpression.Operation.SUBTRACT;
			}
			case MULTIPLY: {
				return BinaryArithmeticExpression.Operation.MULTIPLY;
			}
			case DIVIDE: {
				return BinaryArithmeticExpression.Operation.DIVIDE;
			}
			case QUOT: {
				return BinaryArithmeticExpression.Operation.QUOT;
			}
		}

		throw new IllegalStateException( "Unexpected BinaryArithmeticExpression Operation : " + operation );
	}

	@Override
	public CoalesceFunction visitCoalesceExpression(CoalesceSqmExpression expression) {
		final CoalesceFunction result = new CoalesceFunction();
		for ( SqmExpression value : expression.getValues() ) {
			result.value( (Expression) value.accept( this ) );
		}

		return result;
	}

	@Override
	public CaseSimpleExpression visitSimpleCaseExpression(CaseSimpleSqmExpression expression) {
		final CaseSimpleExpression result = new CaseSimpleExpression(
				expression.getExpressionType(),
				(Expression) expression.getFixture().accept( this )
		);

		for ( CaseSimpleSqmExpression.WhenFragment whenFragment : expression.getWhenFragments() ) {
			result.when(
					(Expression) whenFragment.getCheckValue().accept( this ),
					(Expression) whenFragment.getResult().accept( this )
			);
		}

		result.otherwise( (Expression) expression.getOtherwise().accept( this ) );

		return result;
	}

	@Override
	public CaseSearchedExpression visitSearchedCaseExpression(CaseSearchedSqmExpression expression) {
		final CaseSearchedExpression result = new CaseSearchedExpression( expression.getExpressionType() );

		for ( CaseSearchedSqmExpression.WhenFragment whenFragment : expression.getWhenFragments() ) {
			result.when(
					(Predicate) whenFragment.getPredicate().accept( this ),
					(Expression) whenFragment.getResult().accept( this )
			);
		}

		result.otherwise( (Expression) expression.getOtherwise().accept( this ) );

		return result;
	}

	@Override
	public NullifFunction visitNullifExpression(NullifSqmExpression expression) {
		return new NullifFunction(
				(Expression) expression.getFirstArgument().accept( this ),
				(Expression) expression.getSecondArgument().accept( this )
		);
	}

	@Override
	public ConcatFunction visitConcatExpression(ConcatSqmExpression expression) {
		return new ConcatFunction(
				(Expression) expression.getLeftHandOperand().accept( this ),
				(Expression) expression.getLeftHandOperand().accept( this ),
				expression.getExpressionType()
		);
	}

//	@Override
//	public Object visitPluralAttributeElementBinding(PluralAttributeElementBinding binding) {
//		final TableGroup resolvedTableGroup = fromClauseIndex.findResolvedTableGroup( binding.getFromElement() );
//
//		return getCurrentDomainReferenceExpressionBuilder().buildPluralAttributeElementReferenceExpression(
//				binding,
//				resolvedTableGroup,
//				PersisterHelper.convert( binding.getNavigablePath() )
//		);
//	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Predicates


	@Override
	public GroupedPredicate visitGroupedPredicate(GroupedSqmPredicate predicate) {
		return new GroupedPredicate ( (Predicate ) predicate.getSubPredicate().accept( this ) );
	}

	@Override
	public Junction visitAndPredicate(AndSqmPredicate predicate) {
		final Junction conjunction = new Junction( Junction.Nature.CONJUNCTION );
		conjunction.add( (Predicate) predicate.getLeftHandPredicate().accept( this ) );
		conjunction.add( (Predicate) predicate.getRightHandPredicate().accept( this ) );
		return conjunction;
	}

	@Override
	public Junction visitOrPredicate(OrSqmPredicate predicate) {
		final Junction disjunction = new Junction( Junction.Nature.DISJUNCTION );
		disjunction.add( (Predicate) predicate.getLeftHandPredicate().accept( this ) );
		disjunction.add( (Predicate) predicate.getRightHandPredicate().accept( this ) );
		return disjunction;
	}

	@Override
	public NegatedPredicate visitNegatedPredicate(NegatedSqmPredicate predicate) {
		return new NegatedPredicate(
				(Predicate) predicate.getWrappedPredicate().accept( this )
		);
	}

	@Override
	public RelationalPredicate visitRelationalPredicate(RelationalSqmPredicate predicate) {
		return new RelationalPredicate(
				interpret( predicate.getOperator() ),
				(Expression) predicate.getLeftHandExpression().accept( this ),
				(Expression) predicate.getRightHandExpression().accept( this )
		);
	}

	private RelationalPredicate.Operator interpret(RelationalPredicateOperator operator) {
		switch ( operator ) {
			case EQUAL: {
				return RelationalPredicate.Operator.EQUAL;
			}
			case NOT_EQUAL: {
				return RelationalPredicate.Operator.NOT_EQUAL;
			}
			case GREATER_THAN_OR_EQUAL: {
				return RelationalPredicate.Operator.GE;
			}
			case GREATER_THAN: {
				return RelationalPredicate.Operator.GT;
			}
			case LESS_THAN_OR_EQUAL: {
				return RelationalPredicate.Operator.LE;
			}
			case LESS_THAN: {
				return RelationalPredicate.Operator.LT;
			}
		}

		throw new IllegalStateException( "Unexpected RelationalPredicate Type : " + operator );
	}

	@Override
	public BetweenPredicate visitBetweenPredicate(BetweenSqmPredicate predicate) {
		return new BetweenPredicate(
				(Expression) predicate.getExpression().accept( this ),
				(Expression) predicate.getLowerBound().accept( this ),
				(Expression) predicate.getUpperBound().accept( this ),
				predicate.isNegated()
		);
	}

	@Override
	public LikePredicate visitLikePredicate(LikeSqmPredicate predicate) {
		final Expression escapeExpression = predicate.getEscapeCharacter() == null
				? null
				: (Expression) predicate.getEscapeCharacter().accept( this );

		return new LikePredicate(
				(Expression) predicate.getMatchExpression().accept( this ),
				(Expression) predicate.getPattern().accept( this ),
				escapeExpression,
				predicate.isNegated()
		);
	}

	@Override
	public NullnessPredicate visitIsNullPredicate(NullnessSqmPredicate predicate) {
		return new NullnessPredicate(
				(Expression) predicate.getExpression().accept( this ),
				predicate.isNegated()
		);
	}

	@Override
	public InListPredicate visitInListPredicate(InListSqmPredicate predicate) {
		final InListPredicate inPredicate = new InListPredicate(
				(Expression) predicate.getTestExpression().accept( this ),
				predicate.isNegated()
		);
		for ( SqmExpression expression : predicate.getListExpressions() ) {
			inPredicate.addExpression( (Expression) expression.accept( this ) );
		}
		return inPredicate;
	}

	@Override
	public InSubQueryPredicate visitInSubQueryPredicate(InSubQuerySqmPredicate predicate) {
		return new InSubQueryPredicate(
				(Expression) predicate.getTestExpression().accept( this ),
				(QuerySpec) predicate.getSubQueryExpression().accept( this ),
				predicate.isNegated()
		);
	}

	private final Map<QuerySpec,Map<SqlSelectable,SqlSelection>> sqlSelectionMapByQuerySpec = new HashMap<>();

	@Override
	public SqlSelectionGroup resolveSqlSelectionGroup(Navigable navigable) {
		// need something like:
		final Map<QuerySpec,Map<Navigable,SqlSelectionGroup>> sqlSelectionGroupMapByQuerySpec = new HashMap<>();
		// but that requires exposing SqlSelectables from Navigable or being able to ask Navigable
		//		for a "SqlSelectableGroup"
		// or ask it to build the SqlSelectionGroup passing in this (as SqlSelectionResolver)
		//		and having it resolve the individual selectables building a SqlSelectionGroup
		return null;
	}

	@Override
	public SqlSelection resolveSqlSelection(SqlSelectable sqlSelectable) {
		// todo (6.0) : this needs to be relative to some notion of a particular TableGroup
		//		SqlSelectable e.g. would be a ColumnReference

		final Map<SqlSelectable,SqlSelection> sqlSelectionMap = sqlSelectionMapByQuerySpec.computeIfAbsent(
				currentQuerySpec(),
				k -> new HashMap<>()
		);

		final SqlSelection existing = sqlSelectionMap.get( sqlSelectable );
		if ( existing != null ) {
			return existing;
		}

		final SqlSelection sqlSelection = new SqlSelectionImpl( sqlSelectable, sqlSelectionMap.size() );
		currentQuerySpec().getSelectClause().addSqlSelection( sqlSelection );
		sqlSelectionMap.put( sqlSelectable, sqlSelection );

		return sqlSelection;
	}
}
