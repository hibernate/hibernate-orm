/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.convert.spi;

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
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.EntityGraphImplementor;
import org.hibernate.hql.internal.ast.tree.OrderByClause;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.common.internal.PersisterHelper;
import org.hibernate.persister.common.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.persister.common.internal.SingularPersistentAttributeEntity;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.JoinColumnMapping;
import org.hibernate.persister.common.spi.JoinablePersistentAttribute;
import org.hibernate.persister.common.spi.PluralPersistentAttribute;
import org.hibernate.persister.common.spi.SingularPersistentAttribute;
import org.hibernate.persister.embedded.spi.EmbeddedReference;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.QuerySpec;
import org.hibernate.sql.ast.SelectQuery;
import org.hibernate.sql.ast.expression.AvgFunction;
import org.hibernate.sql.ast.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.expression.CoalesceExpression;
import org.hibernate.sql.ast.expression.ColumnBindingExpression;
import org.hibernate.sql.ast.expression.ConcatExpression;
import org.hibernate.sql.ast.expression.CountFunction;
import org.hibernate.sql.ast.expression.CountStarFunction;
import org.hibernate.sql.ast.expression.Expression;
import org.hibernate.sql.ast.expression.MaxFunction;
import org.hibernate.sql.ast.expression.MinFunction;
import org.hibernate.sql.ast.expression.NamedParameter;
import org.hibernate.sql.ast.expression.NonStandardFunctionExpression;
import org.hibernate.sql.ast.expression.NullifExpression;
import org.hibernate.sql.ast.expression.PositionalParameter;
import org.hibernate.sql.ast.expression.QueryLiteral;
import org.hibernate.sql.ast.expression.SumFunction;
import org.hibernate.sql.ast.expression.UnaryOperationExpression;
import org.hibernate.sql.ast.expression.instantiation.DynamicInstantiation;
import org.hibernate.sql.ast.from.EntityTableGroup;
import org.hibernate.sql.ast.from.TableGroup;
import org.hibernate.sql.ast.from.TableGroupJoin;
import org.hibernate.sql.ast.from.TableSpace;
import org.hibernate.sql.ast.predicate.BetweenPredicate;
import org.hibernate.sql.ast.predicate.GroupedPredicate;
import org.hibernate.sql.ast.predicate.InListPredicate;
import org.hibernate.sql.ast.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.predicate.Junction;
import org.hibernate.sql.ast.predicate.LikePredicate;
import org.hibernate.sql.ast.predicate.NegatedPredicate;
import org.hibernate.sql.ast.predicate.NullnessPredicate;
import org.hibernate.sql.ast.predicate.Predicate;
import org.hibernate.sql.ast.predicate.RelationalPredicate;
import org.hibernate.sql.ast.select.SelectClause;
import org.hibernate.sql.ast.select.Selectable;
import org.hibernate.sql.ast.select.Selection;
import org.hibernate.sql.ast.select.SqlSelectable;
import org.hibernate.sql.ast.select.SqlSelection;
import org.hibernate.sql.ast.sort.SortSpecification;
import org.hibernate.sql.convert.ConversionException;
import org.hibernate.sql.convert.SyntaxException;
import org.hibernate.sql.convert.expression.internal.DomainReferenceExpressionBuilderImpl;
import org.hibernate.sql.convert.expression.spi.DomainReferenceExpressionBuilder;
import org.hibernate.sql.convert.internal.FromClauseIndex;
import org.hibernate.sql.convert.internal.SqlAliasBaseManager;
import org.hibernate.sql.convert.internal.SqmSelectInterpretationImpl;
import org.hibernate.sql.convert.results.internal.FetchCompositeAttributeImpl;
import org.hibernate.sql.convert.results.internal.FetchEntityAttributeImpl;
import org.hibernate.sql.convert.results.spi.FetchParent;
import org.hibernate.sql.convert.results.spi.Return;
import org.hibernate.sql.convert.results.spi.ReturnDynamicInstantiation;
import org.hibernate.sql.convert.results.spi.ReturnResolutionContext;
import org.hibernate.sql.exec.results.process.internal.SqlSelectionImpl;
import org.hibernate.sqm.BaseSemanticQueryWalker;
import org.hibernate.sqm.domain.SqmExpressableType;
import org.hibernate.sqm.query.SqmDeleteStatement;
import org.hibernate.sqm.query.SqmInsertSelectStatement;
import org.hibernate.sqm.query.SqmQuerySpec;
import org.hibernate.sqm.query.SqmSelectStatement;
import org.hibernate.sqm.query.SqmUpdateStatement;
import org.hibernate.sqm.query.expression.BinaryArithmeticSqmExpression;
import org.hibernate.sqm.query.expression.CaseSearchedSqmExpression;
import org.hibernate.sqm.query.expression.CaseSimpleSqmExpression;
import org.hibernate.sqm.query.expression.CoalesceSqmExpression;
import org.hibernate.sqm.query.expression.ConcatSqmExpression;
import org.hibernate.sqm.query.expression.ConstantEnumSqmExpression;
import org.hibernate.sqm.query.expression.ConstantFieldSqmExpression;
import org.hibernate.sqm.query.expression.LiteralBigDecimalSqmExpression;
import org.hibernate.sqm.query.expression.LiteralBigIntegerSqmExpression;
import org.hibernate.sqm.query.expression.LiteralCharacterSqmExpression;
import org.hibernate.sqm.query.expression.LiteralDoubleSqmExpression;
import org.hibernate.sqm.query.expression.LiteralFalseSqmExpression;
import org.hibernate.sqm.query.expression.LiteralFloatSqmExpression;
import org.hibernate.sqm.query.expression.LiteralIntegerSqmExpression;
import org.hibernate.sqm.query.expression.LiteralLongSqmExpression;
import org.hibernate.sqm.query.expression.LiteralNullSqmExpression;
import org.hibernate.sqm.query.expression.LiteralStringSqmExpression;
import org.hibernate.sqm.query.expression.LiteralTrueSqmExpression;
import org.hibernate.sqm.query.expression.NamedParameterSqmExpression;
import org.hibernate.sqm.query.expression.NullifSqmExpression;
import org.hibernate.sqm.query.expression.PositionalParameterSqmExpression;
import org.hibernate.sqm.query.expression.SqmExpression;
import org.hibernate.sqm.query.expression.UnaryOperationSqmExpression;
import org.hibernate.sqm.query.expression.domain.SqmAttributeBinding;
import org.hibernate.sqm.query.expression.domain.SqmEntityBinding;
import org.hibernate.sqm.query.expression.domain.SqmPluralAttributeBinding;
import org.hibernate.sqm.query.expression.domain.SqmSingularAttributeBinding;
import org.hibernate.sqm.query.expression.function.AvgFunctionSqmExpression;
import org.hibernate.sqm.query.expression.function.CountFunctionSqmExpression;
import org.hibernate.sqm.query.expression.function.CountStarFunctionSqmExpression;
import org.hibernate.sqm.query.expression.function.MaxFunctionSqmExpression;
import org.hibernate.sqm.query.expression.function.MinFunctionSqmExpression;
import org.hibernate.sqm.query.expression.function.SumFunctionSqmExpression;
import org.hibernate.sqm.query.from.SqmAttributeJoin;
import org.hibernate.sqm.query.from.SqmCrossJoin;
import org.hibernate.sqm.query.from.SqmEntityJoin;
import org.hibernate.sqm.query.from.SqmFromClause;
import org.hibernate.sqm.query.from.SqmFromElementSpace;
import org.hibernate.sqm.query.from.SqmJoin;
import org.hibernate.sqm.query.from.SqmRoot;
import org.hibernate.sqm.query.order.SqmOrderByClause;
import org.hibernate.sqm.query.order.SqmSortSpecification;
import org.hibernate.sqm.query.paging.SqmLimitOffsetClause;
import org.hibernate.sqm.query.predicate.AndSqmPredicate;
import org.hibernate.sqm.query.predicate.BetweenSqmPredicate;
import org.hibernate.sqm.query.predicate.GroupedSqmPredicate;
import org.hibernate.sqm.query.predicate.InListSqmPredicate;
import org.hibernate.sqm.query.predicate.InSubQuerySqmPredicate;
import org.hibernate.sqm.query.predicate.LikeSqmPredicate;
import org.hibernate.sqm.query.predicate.NegatedSqmPredicate;
import org.hibernate.sqm.query.predicate.NullnessSqmPredicate;
import org.hibernate.sqm.query.predicate.OrSqmPredicate;
import org.hibernate.sqm.query.predicate.RelationalPredicateOperator;
import org.hibernate.sqm.query.predicate.RelationalSqmPredicate;
import org.hibernate.sqm.query.predicate.SqmWhereClause;
import org.hibernate.sqm.query.select.SqmDynamicInstantiation;
import org.hibernate.sqm.query.select.SqmDynamicInstantiationArgument;
import org.hibernate.sqm.query.select.SqmDynamicInstantiationTarget;
import org.hibernate.sqm.query.select.SqmSelectClause;
import org.hibernate.sqm.query.select.SqmSelection;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.Type;

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
		implements DomainReferenceExpressionBuilder.BuildingContext, ReturnResolutionContext {
	private static final Logger log = Logger.getLogger( SqmSelectToSqlAstConverter.class );

	/**
	 * Main entry point into SQM SelectStatement interpretation
	 *
	 * @param statement The SQM SelectStatement to interpret
	 * @param queryOptions The options to be applied to the interpretation
	 * @param callback to be formally defined
	 * @param isShallow {@code true} if the interpretation is initiated from Query#iterate; all
	 * other forms of Query execution would pass {@code false}
	 *
	 * @return The interpretation
	 */
	public static SqmSelectInterpretation interpret(
			SqmSelectStatement statement,
			SharedSessionContractImplementor persistenceContext,
			QueryOptions queryOptions,
			boolean isShallow,
			Callback callback) {
		final SqmSelectToSqlAstConverter walker = new SqmSelectToSqlAstConverter(
				persistenceContext.getFactory(),
				queryOptions,
				isShallow,
				callback
		);
		return walker.interpret( statement );
	}

	private final SessionFactoryImplementor factory;
	private final QueryOptions queryOptions;
	private final boolean isShallow;
	private final Callback callback;

	private final FromClauseIndex fromClauseIndex = new FromClauseIndex();
	private final SqlAliasBaseManager sqlAliasBaseManager = new SqlAliasBaseManager();
	private final Stack<DomainReferenceExpressionBuilder> domainExpressionBuilderStack = new Stack<>();
	private final Stack<Clause> currentClauseStack = new Stack<>();
	private final Stack<QuerySpec> querySpecStack = new Stack<>();
	private int querySpecDepth = 0;

	private final List<Return> queryReturns = new ArrayList<>();

	private SqmSelectToSqlAstConverter(
			SessionFactoryImplementor factory,
			QueryOptions queryOptions,
			boolean isShallow,
			Callback callback) {
		this.factory = factory;
		this.queryOptions = queryOptions;
		this.isShallow = isShallow;
		this.callback = callback;
		pushDomainExpressionBuilder( isShallow );
	}

	private SqmSelectInterpretation interpret(SqmSelectStatement statement) {
		return new SqmSelectInterpretationImpl(
				visitSelectStatement( statement ),
				queryReturns
		);
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
	public SelectQuery visitSelectStatement(SqmSelectStatement statement) {
		final QuerySpec querySpec = visitQuerySpec( statement.getQuerySpec() );
		final SelectQuery sqlAst = new SelectQuery( querySpec );

		return sqlAst;
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
		final QuerySpec astQuerySpec = new QuerySpec();
		querySpecStack.push( astQuerySpec );
		querySpecDepth++;

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
			querySpecDepth--;
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
		if ( fromClauseIndex.isResolved( sqmRoot ) ) {
			final TableGroup resolvedTableGroup = fromClauseIndex.findResolvedTableGroup( sqmRoot );
			return resolvedTableGroup;
		}

		final SqmEntityBinding binding = sqmRoot.getBinding();
		final EntityPersister entityPersister = (EntityPersister) binding.getBoundNavigable();
		final EntityTableGroup group = entityPersister.buildTableGroup(
				sqmRoot,
				tableSpace,
				sqlAliasBaseManager,
				fromClauseIndex
		);
		tableSpace.setRootTableGroup( group );

		return group;
	}

	@Override
	public Object visitQualifiedAttributeJoinFromElement(SqmAttributeJoin joinedFromElement) {
		if ( fromClauseIndex.isResolved( joinedFromElement ) ) {
			return fromClauseIndex.findResolvedTableGroup( joinedFromElement );
		}

		final TableGroup joinedTableGroup = resolveTableGroupProducer( joinedFromElement ).buildTableGroup(
				joinedFromElement,
				tableSpace,
				sqlAliasBaseManager,
				fromClauseIndex
		);

		final TableGroup ownerTableGroup = fromClauseIndex.findResolvedTableGroup(
				joinedFromElement.getAttributeBinding().getSourceBinding().getExportedFromElement()
		);
		final Junction predicate = new Junction( Junction.Nature.CONJUNCTION );

		final JoinablePersistentAttribute<?,?> joinableAttribute = (JoinablePersistentAttribute) joinedFromElement.getAttributeBinding().getBoundNavigable();
		for ( JoinColumnMapping joinColumnMapping : joinableAttribute.getJoinColumnMappings() ) {
			// if the joinedAttribute ois a collection, we need to flip the JoinColumnMapping..
			//		this has to do with "foreign-key directionality"
			final Column joinLhsColumn;
			final Column joinRhsColumn;
			if ( joinableAttribute instanceof PluralPersistentAttribute ) {
				joinLhsColumn = joinColumnMapping.getRightHandSideColumn();
				joinRhsColumn = joinColumnMapping.getLeftHandSideColumn();
			}
			else {
				joinLhsColumn = joinColumnMapping.getLeftHandSideColumn();
				joinRhsColumn = joinColumnMapping.getRightHandSideColumn();
			}
			predicate.add(
					new RelationalPredicate(
							RelationalPredicate.Operator.EQUAL,
							new ColumnBindingExpression(
									ownerTableGroup.resolveColumnBinding( joinLhsColumn )
							),
							new ColumnBindingExpression(
									joinedTableGroup.resolveColumnBinding( joinRhsColumn )
							)
					)
			);
		}

		// add any additional join restrictions
		if ( joinedFromElement.getOnClausePredicate() != null ) {
			predicate.add( (Predicate) joinedFromElement.getOnClausePredicate().accept( this ) );
		}

		return new TableGroupJoin( joinedFromElement.getJoinType(), joinedTableGroup, predicate );
	}

	private TableGroupProducer resolveTableGroupProducer(SqmAttributeJoin joinedFromElement) {
		if ( joinedFromElement.getAttributeBinding().getBoundNavigable() instanceof CollectionPersister ) {
			return (CollectionPersister) joinedFromElement.getAttributeBinding().getBoundNavigable();
		}

		if ( joinedFromElement.getAttributeBinding().getBoundNavigable() instanceof SingularPersistentAttributeEntity ) {
			return ( (SingularPersistentAttributeEntity) joinedFromElement.getAttributeBinding().getBoundNavigable() ).getAssociatedEntityPersister();
		}

		if ( joinedFromElement.getAttributeBinding().getBoundNavigable() instanceof EmbeddedReference ) {
			return ( (EmbeddedReference) joinedFromElement.getAttributeBinding().getBoundNavigable() ).resolveTableGroupProducer();
		}
		// otherwise - we have an exception condition

		// todo : we could handle composites as well - another good argument for EmbeddedReference
		//		or just walking the composite's AttributeContainer until we hit a collection or entity

		throw new ConversionException( "Could not resolve TableGroupProducer from SqmAttributeJoin [" + joinedFromElement + "]" );

	}

	@Override
	public TableGroupJoin visitCrossJoinedFromElement(SqmCrossJoin joinedFromElement) {
		// todo : this cast will not ultimately work.
		// 		Instead we will need to resolve the Bindable+intrinsicSubclassIndicator to its ImprovedEntityPersister/EntityPersister
		final EntityPersister entityPersister = (EntityPersister) joinedFromElement.getIntrinsicSubclassIndicator();
		TableGroup group = entityPersister.buildTableGroup(
				joinedFromElement,
				tableSpace,
				sqlAliasBaseManager,
				fromClauseIndex
		);
		return new TableGroupJoin( joinedFromElement.getJoinType(), group, null );
	}

	@Override
	public Object visitQualifiedEntityJoinFromElement(SqmEntityJoin joinedFromElement) {
		throw new NotYetImplementedException();
	}

	@Override
	public SelectClause visitSelectClause(SqmSelectClause selectClause) {
		currentClauseStack.push( Clause.SELECT );
		pushDomainExpressionBuilder( querySpecDepth > 1 || isShallow );
		try {
			super.visitSelectClause( selectClause );
			currentQuerySpec().getSelectClause().makeDistinct( selectClause.isDistinct() );
			return currentQuerySpec().getSelectClause();
		}
		finally {
			domainExpressionBuilderStack.pop();
			currentClauseStack.pop();
		}
	}

	private QuerySpec currentQuerySpec() {
		return querySpecStack.getCurrent();
	}

	@Override
	public Selection visitSelection(SqmSelection sqmSelection) {
		final Expression expression = (Expression) sqmSelection.getExpression().accept( this );
		final Selectable selectable = expression.getSelectable();
		final Return queryReturn = selectable.toQueryReturn( this, sqmSelection.getAlias() );

		if ( querySpecDepth == 1 ) {
			queryReturns.add( queryReturn );
		}
		final Selection selection = new Selection(
				this,
				expression.getSelectable(),
				expression,
				sqmSelection.getAlias()
		);

		if ( !domainExpressionBuilderStack.getCurrent().isShallow() ) {
			applyFetchesAndEntityGraph( queryReturn );
		}

		currentQuerySpec().getSelectClause().selection( selection );

		return selection;
	}

	private void applyFetchesAndEntityGraph(Return queryReturn) {
		if ( queryReturn instanceof FetchParent ) {
			applyFetchesAndEntityGraph( (FetchParent) queryReturn, extractEntityGraph() );
		}

		// todo : dynamic-instantiations *if* the dynamic-instantiation takes the entity as an argument
		if ( queryReturn instanceof ReturnDynamicInstantiation ) {

		}

		// otherwise, nothing to do
	}

	private EntityGraphImplementor extractEntityGraph() {
		if ( queryOptions.getEntityGraphQueryHint() == null ) {
			return null;
		}
		else {
			return (EntityGraphImplementor) queryOptions.getEntityGraphQueryHint().getOriginEntityGraph();
		}
	}

	private Set<String> alreadyProcessedFetchParentTableGroupUids = new HashSet<>();

	private void applyFetchesAndEntityGraph(FetchParent fetchParent, EntityGraphImplementor entityGraph) {
		final String uniqueIdentifier = fetchParent.getTableGroupUniqueIdentifier();
		if ( !alreadyProcessedFetchParentTableGroupUids.add( uniqueIdentifier ) ) {
			log.errorf( "Found duplicate tableGroupUid as FetchParent [%s]", uniqueIdentifier );
			return;
		}

		// todo : to do this well we are going to need a way to get all of the attributes related to this FetchParent
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
			final SqmAttributeBinding fetchedAttributeBinding = fetchedJoin.getAttributeBinding();
			// todo  : need this method added to EntityGraphImplementor
			//final String attributeName = fetchedAttributeBinding.getAttribute().getAttributeName();
			//final AttributeNodeImplementor attributeNode = entityGraphImplementor.findAttributeNode( attributeName );
			final AttributeNodeImplementor attributeNode = null;
			applyFetchesAndEntityGraph( fetchParent, fetchedJoin, attributeNode );
		}
	}

	private void applyFetchesAndEntityGraph(FetchParent fetchParent, SqmAttributeJoin attributeJoin, AttributeNodeImplementor attributeNode) {
		if ( attributeJoin.getAttributeBinding() instanceof SqmPluralAttributeBinding ) {
			// apply the plural attribute fetch join
			final SqmPluralAttributeBinding pluralAttributeBinding = (SqmPluralAttributeBinding) attributeJoin.getAttributeBinding();

			// todo : work out how to model collection fetches...
			//		mainly... do we need a "grouping" fetch?  And if so, should
			// 		CollectionAttributeFetch expose its element versus key fetches individually?  Or
			// 		does it represent each by itself?

		}
		else if ( attributeJoin.getAttributeBinding() instanceof SqmSingularAttributeBinding ) {
			// apply the singular attribute fetch join
			final SqmSingularAttributeBinding attributeBinding = (SqmSingularAttributeBinding) attributeJoin.getAttributeBinding();
			final SingularPersistentAttribute boundAttribute = (SingularPersistentAttribute) attributeBinding.getBoundNavigable();

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
					applyFetchesAndEntityGraph( fetch, null );
					break;
				}
				case ONE_TO_ONE:
				case MANY_TO_ONE: {
					final SingularPersistentAttributeEntity boundAttributeAsEntity = (SingularPersistentAttributeEntity) boundAttribute;
					final FetchEntityAttributeImpl fetch = new FetchEntityAttributeImpl(
							fetchParent,
							PersisterHelper.convert( attributeBinding.getPropertyPath() ),
							attributeJoin.getUniqueIdentifier(),
							boundAttributeAsEntity,
							boundAttributeAsEntity.getAssociatedEntityPersister(),
							new FetchStrategy( FetchTiming.IMMEDIATE, FetchStyle.JOIN )
					);
					fetchParent.addFetch( fetch );
					applyFetchesAndEntityGraph( fetch, null );
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
				extractOrmType( expression.getExpressionType(), StandardBasicTypes.STRING ),
				currentClauseStack.getCurrent() == Clause.SELECT
		);
	}

	private Type extractOrmType(SqmExpressableType expressableType, Type fallbackType) {
		final Type resolvedType = extractOrmType( expressableType );
		if ( resolvedType != null ) {
			return resolvedType;
		}
		return fallbackType;
	}


	private Type extractOrmType(SqmExpressableType expressableType) {
		return (Type) expressableType.getExportedDomainType();
	}

	@Override
	public QueryLiteral visitLiteralCharacterExpression(LiteralCharacterSqmExpression expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				extractOrmType( expression.getExpressionType(), StandardBasicTypes.CHARACTER ),
				currentClauseStack.getCurrent() == Clause.SELECT
		);
	}

	@Override
	public QueryLiteral visitLiteralDoubleExpression(LiteralDoubleSqmExpression expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				extractOrmType( expression.getExpressionType(), StandardBasicTypes.DOUBLE ),
				currentClauseStack.getCurrent() == Clause.SELECT
		);
	}

	@Override
	public QueryLiteral visitLiteralIntegerExpression(LiteralIntegerSqmExpression expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				extractOrmType( expression.getExpressionType(), StandardBasicTypes.INTEGER ),
				currentClauseStack.getCurrent() == Clause.SELECT
		);
	}

	@Override
	public QueryLiteral visitLiteralBigIntegerExpression(LiteralBigIntegerSqmExpression expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				extractOrmType( expression.getExpressionType(), StandardBasicTypes.BIG_INTEGER ),
				currentClauseStack.getCurrent() == Clause.SELECT
		);
	}

	@Override
	public QueryLiteral visitLiteralBigDecimalExpression(LiteralBigDecimalSqmExpression expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				extractOrmType( expression.getExpressionType(), StandardBasicTypes.BIG_DECIMAL ),
				currentClauseStack.getCurrent() == Clause.SELECT
		);
	}

	@Override
	public QueryLiteral visitLiteralFloatExpression(LiteralFloatSqmExpression expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				extractOrmType( expression.getExpressionType(), StandardBasicTypes.FLOAT ),
				currentClauseStack.getCurrent() == Clause.SELECT
		);
	}

	@Override
	public QueryLiteral visitLiteralLongExpression(LiteralLongSqmExpression expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				extractOrmType( expression.getExpressionType(), StandardBasicTypes.LONG ),
				currentClauseStack.getCurrent() == Clause.SELECT
		);
	}

	@Override
	public QueryLiteral visitLiteralTrueExpression(LiteralTrueSqmExpression expression) {
		return new QueryLiteral(
				Boolean.TRUE,
				extractOrmType( expression.getExpressionType(), StandardBasicTypes.BOOLEAN ),
				currentClauseStack.getCurrent() == Clause.SELECT
		);
	}

	@Override
	public QueryLiteral visitLiteralFalseExpression(LiteralFalseSqmExpression expression) {
		return new QueryLiteral(
				Boolean.FALSE,
				extractOrmType( expression.getExpressionType(), StandardBasicTypes.BOOLEAN ),
				currentClauseStack.getCurrent() == Clause.SELECT
		);
	}

	@Override
	public QueryLiteral visitLiteralNullExpression(LiteralNullSqmExpression expression) {
		return new QueryLiteral(
				null,
				extractOrmType( expression.getExpressionType() ),
				currentClauseStack.getCurrent() == Clause.SELECT
		);
	}

	@Override
	public Object visitConstantEnumExpression(ConstantEnumSqmExpression expression) {
		return new QueryLiteral(
				expression.getValue(),
				// todo : how do I resolve the Type for an enum?
				extractOrmType( expression.getExpressionType(), expression.getValue().getClass() ),
				currentClauseStack.getCurrent() == Clause.SELECT
		);
	}

	@Override
	public Object visitConstantFieldExpression(ConstantFieldSqmExpression expression) {
		return new QueryLiteral(
				expression.getValue(),
				extractOrmType( expression.getExpressionType(), expression.getValue().getClass() ),
				currentClauseStack.getCurrent() == Clause.SELECT
		);
	}

	@Override
	public NamedParameter visitNamedParameterExpression(NamedParameterSqmExpression expression) {
		return new NamedParameter(
				expression.getName(),
				extractOrmType( expression.getExpressionType() )
		);
	}

	@Override
	public PositionalParameter visitPositionalParameterExpression(PositionalParameterSqmExpression expression) {
		return new PositionalParameter(
				expression.getPosition(),
				extractOrmType( expression.getExpressionType() )
		);
	}

	@Override
	public AvgFunction visitAvgFunction(AvgFunctionSqmExpression expression) {
		pushDomainExpressionBuilder( true );
		try {
			return new AvgFunction(
					(Expression) expression.getArgument().accept( this ),
					expression.isDistinct(),
					(BasicType) extractOrmType( expression.getExpressionType() )
			);
		}
		finally {
			domainExpressionBuilderStack.pop();
		}
	}

	private void pushDomainExpressionBuilder(boolean shallow) {
		domainExpressionBuilderStack.push( new DomainReferenceExpressionBuilderImpl( shallow ) );
	}

	@Override
	public MaxFunction visitMaxFunction(MaxFunctionSqmExpression expression) {
		pushDomainExpressionBuilder( true );
		try {
			return new MaxFunction(
					(Expression) expression.getArgument().accept( this ),
					expression.isDistinct(),
					(BasicType) extractOrmType( expression.getExpressionType() )
			);
		}
		finally {
			domainExpressionBuilderStack.pop();
		}
	}

	@Override
	public MinFunction visitMinFunction(MinFunctionSqmExpression expression) {
		pushDomainExpressionBuilder( true );
		try {
			return new MinFunction(
					(Expression) expression.getArgument().accept( this ),
					expression.isDistinct(),
					(BasicType) extractOrmType( expression.getExpressionType() )
			);
		}
		finally {
			domainExpressionBuilderStack.pop();
		}
	}

	@Override
	public SumFunction visitSumFunction(SumFunctionSqmExpression expression) {
		pushDomainExpressionBuilder( true );
		try {
			return new SumFunction(
					(Expression) expression.getArgument().accept( this ),
					expression.isDistinct(),
					(BasicType) extractOrmType( expression.getExpressionType() )
			);
		}
		finally {
			domainExpressionBuilderStack.pop();
		}
	}

	@Override
	public CountFunction visitCountFunction(CountFunctionSqmExpression expression) {
		pushDomainExpressionBuilder( true );
		try {
			return new CountFunction(
					(Expression) expression.getArgument().accept( this ),
					expression.isDistinct(),
					(BasicType) extractOrmType( expression.getExpressionType() )
			);
		}
		finally {
			domainExpressionBuilderStack.pop();
		}
	}

	@Override
	public CountStarFunction visitCountStarFunction(CountStarFunctionSqmExpression expression) {
		pushDomainExpressionBuilder( true );
		try {
			return new CountStarFunction(
					expression.isDistinct(),
					(BasicType) extractOrmType( expression.getExpressionType() )
			);
		}
		finally {
			domainExpressionBuilderStack.pop();
		}
	}

	@Override
	public Object visitUnaryOperationExpression(UnaryOperationSqmExpression expression) {
		pushDomainExpressionBuilder( true );
		try {
			return new UnaryOperationExpression(
					interpret( expression.getOperation() ),
					(Expression) expression.getOperand().accept( this ),
					(BasicType) extractOrmType( expression.getExpressionType() )
			);
		}
		finally {
			domainExpressionBuilderStack.pop();
		}
	}

	private UnaryOperationExpression.Operation interpret(UnaryOperationSqmExpression.Operation operation) {
		switch ( operation ) {
			case PLUS: {
				return UnaryOperationExpression.Operation.PLUS;
			}
			case MINUS: {
				return UnaryOperationExpression.Operation.MINUS;
			}
		}

		throw new IllegalStateException( "Unexpected UnaryOperationExpression Operation : " + operation );
	}

	@Override
	public Expression visitBinaryArithmeticExpression(BinaryArithmeticSqmExpression expression) {
		pushDomainExpressionBuilder( true );
		try {
			if ( expression.getOperation() == BinaryArithmeticSqmExpression.Operation.MODULO ) {
				return new NonStandardFunctionExpression(
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
					null //(BasicType) extractOrmType( expression.getExpressionType() )
			);
		}
		finally {
			domainExpressionBuilderStack.pop();
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
	public CoalesceExpression visitCoalesceExpression(CoalesceSqmExpression expression) {
		final CoalesceExpression result = new CoalesceExpression();
		for ( SqmExpression value : expression.getValues() ) {
			result.value( (Expression) value.accept( this ) );
		}

		return result;
	}

	@Override
	public CaseSimpleExpression visitSimpleCaseExpression(CaseSimpleSqmExpression expression) {
		final CaseSimpleExpression result = new CaseSimpleExpression(
				extractOrmType( expression.getExpressionType() ),
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
		final CaseSearchedExpression result = new CaseSearchedExpression( extractOrmType( expression.getExpressionType() ) );

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
	public NullifExpression visitNullifExpression(NullifSqmExpression expression) {
		return new NullifExpression(
				(Expression) expression.getFirstArgument().accept( this ),
				(Expression) expression.getSecondArgument().accept( this )
		);
	}

	@Override
	public ConcatExpression visitConcatExpression(ConcatSqmExpression expression) {
		return new ConcatExpression(
				(Expression) expression.getLeftHandOperand().accept( this ),
				(Expression) expression.getLeftHandOperand().accept( this ),
				(BasicType) extractOrmType( expression.getExpressionType() )
		);
	}

//	@Override
//	public Object visitPluralAttributeElementBinding(PluralAttributeElementBinding binding) {
//		final TableGroup resolvedTableGroup = fromClauseIndex.findResolvedTableGroup( binding.getFromElement() );
//
//		return getCurrentDomainReferenceExpressionBuilder().buildPluralAttributeElementReferenceExpression(
//				binding,
//				resolvedTableGroup,
//				PersisterHelper.convert( binding.getPropertyPath() )
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
				(org.hibernate.sql.ast.QuerySpec) predicate.getSubQueryExpression().accept( this ),
				predicate.isNegated()
		);
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return factory;
	}

	@Override
	public DomainReferenceExpressionBuilder getCurrentDomainReferenceExpressionBuilder() {
		return domainExpressionBuilderStack.getCurrent();
	}

	@Override
	public FromClauseIndex getFromClauseIndex() {
		return fromClauseIndex;
	}

	@Override
	public boolean isShallowQuery() {
		return isShallow;
	}

	@Override
	public Clause getCurrentStatementClause() {
		return currentClauseStack.getCurrent();
	}

	@Override
	public ReturnResolutionContext getReturnResolutionContext() {
		return this;
	}

	private final Map<QuerySpec,Map<SqlSelectable,SqlSelection>> sqlSelectionMapByQuerySpec = new HashMap<>();

	@Override
	public SqlSelection resolveSqlSelection(SqlSelectable sqlSelectable) {
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
