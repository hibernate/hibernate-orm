/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.spi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.tree.SqmQuerySpec;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmCaseSearched;
import org.hibernate.query.sqm.tree.expression.SqmCaseSimple;
import org.hibernate.query.sqm.tree.expression.SqmConcat;
import org.hibernate.query.sqm.tree.expression.SqmConstantEnum;
import org.hibernate.query.sqm.tree.expression.SqmConstantFieldReference;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmLiteralBigDecimal;
import org.hibernate.query.sqm.tree.expression.SqmLiteralBigInteger;
import org.hibernate.query.sqm.tree.expression.SqmLiteralCharacter;
import org.hibernate.query.sqm.tree.expression.SqmLiteralDouble;
import org.hibernate.query.sqm.tree.expression.SqmLiteralFalse;
import org.hibernate.query.sqm.tree.expression.SqmLiteralFloat;
import org.hibernate.query.sqm.tree.expression.SqmLiteralInteger;
import org.hibernate.query.sqm.tree.expression.SqmLiteralLong;
import org.hibernate.query.sqm.tree.expression.SqmLiteralNull;
import org.hibernate.query.sqm.tree.expression.SqmLiteralString;
import org.hibernate.query.sqm.tree.expression.SqmLiteralTrue;
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;
import org.hibernate.query.sqm.tree.expression.SqmUnaryOperation;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityIdentifierReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmPluralAttributeReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReferenceAny;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReferenceBasic;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReferenceEmbedded;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReferenceEntity;
import org.hibernate.query.sqm.tree.expression.function.SqmAbsFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmAvgFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmBitLengthFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCastFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCoalesceFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmConcatFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCountFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCountStarFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCurrentDateFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCurrentTimeFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCurrentTimestampFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmExtractFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmGenericFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmLengthFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmLocateFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmLowerFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmMaxFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmMinFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmModFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmNullifFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmSumFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmTrimFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmUpperFunction;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;
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
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.produce.ordering.internal.SqmColumnReference;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.FromClauseIndex;
import org.hibernate.sql.ast.produce.spi.JoinedTableGroupContext;
import org.hibernate.sql.ast.produce.spi.NonQualifiableSqlExpressable;
import org.hibernate.sql.ast.produce.spi.QualifiableSqlExpressable;
import org.hibernate.sql.ast.produce.spi.RootTableGroupContext;
import org.hibernate.sql.ast.produce.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.produce.spi.SqlAstBuildingContext;
import org.hibernate.sql.ast.produce.spi.SqlAstFunctionProducer;
import org.hibernate.sql.ast.produce.spi.SqlExpressable;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.produce.spi.SqlSelectionExpression;
import org.hibernate.sql.ast.produce.spi.TableGroupJoinProducer;
import org.hibernate.sql.ast.produce.sqm.spi.SqmSelectToSqlAstConverter;
import org.hibernate.sql.ast.produce.sqm.spi.SqmToSqlAstConverter;
import org.hibernate.sql.ast.tree.spi.Clause;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.expression.AbsFunction;
import org.hibernate.sql.ast.tree.spi.expression.AvgFunction;
import org.hibernate.sql.ast.tree.spi.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.spi.expression.BitLengthFunction;
import org.hibernate.sql.ast.tree.spi.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.spi.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.tree.spi.expression.CastFunction;
import org.hibernate.sql.ast.tree.spi.expression.CoalesceFunction;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.ConcatFunction;
import org.hibernate.sql.ast.tree.spi.expression.CountFunction;
import org.hibernate.sql.ast.tree.spi.expression.CountStarFunction;
import org.hibernate.sql.ast.tree.spi.expression.CurrentDateFunction;
import org.hibernate.sql.ast.tree.spi.expression.CurrentTimeFunction;
import org.hibernate.sql.ast.tree.spi.expression.CurrentTimestampFunction;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.ExtractFunction;
import org.hibernate.sql.ast.tree.spi.expression.LengthFunction;
import org.hibernate.sql.ast.tree.spi.expression.LocateFunction;
import org.hibernate.sql.ast.tree.spi.expression.LowerFunction;
import org.hibernate.sql.ast.tree.spi.expression.MaxFunction;
import org.hibernate.sql.ast.tree.spi.expression.MinFunction;
import org.hibernate.sql.ast.tree.spi.expression.ModFunction;
import org.hibernate.sql.ast.tree.spi.expression.NamedParameter;
import org.hibernate.sql.ast.tree.spi.expression.NonStandardFunction;
import org.hibernate.sql.ast.tree.spi.expression.NullifFunction;
import org.hibernate.sql.ast.tree.spi.expression.PositionalParameter;
import org.hibernate.sql.ast.tree.spi.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.spi.expression.SqlTuple;
import org.hibernate.sql.ast.tree.spi.expression.SumFunction;
import org.hibernate.sql.ast.tree.spi.expression.TrimFunction;
import org.hibernate.sql.ast.tree.spi.expression.UnaryOperation;
import org.hibernate.sql.ast.tree.spi.expression.UpperFunction;
import org.hibernate.sql.ast.tree.spi.expression.domain.AnyValuedNavigableReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.BasicValuedNavigableReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.EmbeddableValuedNavigableReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.EntityIdentifierReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.EntityValuedNavigableReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableContainerReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.PluralAttributeReference;
import org.hibernate.sql.ast.tree.spi.from.EntityTableGroup;
import org.hibernate.sql.ast.tree.spi.from.FromClause;
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
import org.hibernate.sql.ast.tree.spi.sort.SortSpecification;
import org.hibernate.sql.results.spi.QueryResultProducer;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.sql.results.spi.SqlSelectionGroupResolutionContext;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.StandardSpiBasicTypes;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public abstract class BaseSqmToSqlAstConverter
		extends BaseSemanticQueryWalker
		implements SqmToSqlAstConverter, SqlExpressionResolver, SqlSelectionGroupResolutionContext {

	private static final Logger log = Logger.getLogger( BaseSqmToSqlAstConverter.class );

	protected enum Shallowness {
		NONE,
		CTOR,
		FUNCTION,
		SUBQUERY
	}

	private final SqlAstBuildingContext sqlAstBuildingContext;
	private final QueryOptions queryOptions;

	private final SqlAliasBaseManager sqlAliasBaseManager = new SqlAliasBaseManager();

	private final Map<QuerySpec,Map<SqlExpressable,SqlSelection>> sqlSelectionMapByQuerySpec = new HashMap<>();

	private final FromClauseIndex fromClauseIndex = new FromClauseIndex();

	private final Stack<Clause> currentClauseStack = new Stack<>();
	private final Stack<QuerySpec> querySpecStack = new Stack<>();
	private final Stack<TableGroup> tableGroupStack = new Stack<>();
	private final Stack<SqmSelectToSqlAstConverter.Shallowness> shallownessStack = new Stack<>( SqmSelectToSqlAstConverter.Shallowness.NONE );
	private final Stack<NavigableReference> navigableReferenceStack = new Stack<>();

	private final Set<String> affectedTableNames = new HashSet<>();

	public BaseSqmToSqlAstConverter(
			SqlAstBuildingContext sqlAstBuildingContext,
			QueryOptions queryOptions) {
		super( sqlAstBuildingContext.getSessionFactory() );
		this.sqlAstBuildingContext = sqlAstBuildingContext;
		this.queryOptions = queryOptions;
	}

	public SqlAstBuildingContext getSqlAstBuildingContext() {
		return sqlAstBuildingContext;
	}

	protected Set<String> affectedTableNames() {
		return affectedTableNames;
	}

	protected QuerySpec currentQuerySpec() {
		return querySpecStack.getCurrent();
	}

	public QueryOptions getQueryOptions() {
		return queryOptions;
	}

	public SqlAliasBaseManager getSqlAliasBaseManager() {
		return sqlAliasBaseManager;
	}

	public FromClauseIndex getFromClauseIndex() {
		return fromClauseIndex;
	}

	public Stack<Clause> getCurrentClauseStack() {
		return currentClauseStack;
	}

	public Stack<QuerySpec> getQuerySpecStack() {
		return querySpecStack;
	}

	public Stack<TableGroup> getTableGroupStack() {
		return tableGroupStack;
	}

	public Stack<Shallowness> getShallownessStack() {
		return shallownessStack;
	}

	public Stack<NavigableReference> getNavigableReferenceStack() {
		return navigableReferenceStack;
	}

	public Stack<FromClause> getFromClauseStack() {
		return fromClauseStack;
	}

	protected void primeQuerySpecStack(QuerySpec querySpec) {
		primeStack( querySpecStack, querySpec );
	}

	protected <T> void primeStack(Stack<T> stack, T initialValue) {
		verifyCanBePrimed( stack );
		stack.push( initialValue );
	}

	private static void verifyCanBePrimed(Stack stack) {
		if ( !stack.isEmpty() ) {
			throw new IllegalStateException( "Cannot prime an already populated Stack" );
		}
	}

	protected void primeNavigableReferenceStack(NavigableReference initial) {
		primeStack( navigableReferenceStack, initial );
	}

	// NOTE : assumes that Expression impls "properly" implement equals/hashCode which may not be the
	//		best option.

	// todo (6.0) : decide if we want to do the caching/unique-ing of Expressions here.
	//		its really only needed for top-level select clauses, not sub-queries.
	//		its "ok" to do it for sub-queries as well - just wondering about the overhead.
	private Map<QuerySpec,Map<Expression, SqlSelection>> sqlExpressionToSqlSelectionMapByQuerySpec;

	private Map<QuerySpec,Map<ColumnReferenceQualifier,Map<QualifiableSqlExpressable,SqlSelection>>> qualifiedSExpressionToSqlSelectionMapByQuerySpec;

	// todo (6.0) : is there ever a time when resolving a sqlSelectable relative to a qualifier ought to return different expressions for multiple references?

	@Override
	public Expression resolveSqlExpression(ColumnReferenceQualifier qualifier, QualifiableSqlExpressable sqlSelectable) {
		return normalizeSqlExpression( qualifier.qualify( sqlSelectable ) );
	}

	private Expression normalizeSqlExpression(Expression expression) {
		if ( getCurrentClauseStack().getCurrent() == Clause.ORDER
				|| getCurrentClauseStack().getCurrent() == Clause.GROUP
				|| getCurrentClauseStack().getCurrent() == Clause.HAVING ) {
			// see if this (Sql)Expression is used as a selection, and if so
			// wrap the (Sql)Expression in a special wrapper with access to both
			// the (Sql)Expression and the SqlSelection.
			//
			// This is used for databases which prefer to use the position of a
			// selected expression (within the select-clause) as the
			// order-by, group-by or having expression
			if ( sqlExpressionToSqlSelectionMapByQuerySpec != null ) {
				final Map<Expression, SqlSelection> sqlExpressionToSqlSelectionMap =
						sqlExpressionToSqlSelectionMapByQuerySpec.get( currentQuerySpec() );
				if ( sqlExpressionToSqlSelectionMap != null ) {
					final SqlSelection selection = sqlExpressionToSqlSelectionMap.get( expression );
					if ( selection != null ) {
						return new SqlSelectionExpression( selection, expression );
					}
				}
			}
		}

		return expression;
	}

	@Override
	public Expression resolveSqlExpression(NonQualifiableSqlExpressable sqlSelectable) {
		return normalizeSqlExpression( sqlSelectable.createExpression() );
	}

	@Override
	public SqlExpressionResolver getSqlSelectionResolver() {
		return this;
	}

	@Override
	public boolean shouldCreateShallowEntityResult() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public SqlSelection resolveSqlSelection(Expression expression) {
		final Map<SqlExpressable,SqlSelection> sqlSelectionMap = sqlSelectionMapByQuerySpec.computeIfAbsent(
				currentQuerySpec(),
				k -> new HashMap<>()
		);

		final SqlSelection existing = sqlSelectionMap.get( expression.getExpressable() );
		if ( existing != null ) {
			return existing;
		}

		final SqlSelection sqlSelection = expression.createSqlSelection( sqlSelectionMap.size() );
		currentQuerySpec().getSelectClause().addSqlSelection( sqlSelection );
		sqlSelectionMap.put( expression.getExpressable(), sqlSelection );

		return sqlSelection;
	}


	@Override
	public Void visitOrderByClause(SqmOrderByClause orderByClause) {
		super.visitOrderByClause( orderByClause );
		return null;
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
		fromClauseStack.push( astQuerySpec.getFromClause() );

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
			assert fromClauseStack.pop() == astQuerySpec.getFromClause();
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

	final Stack<FromClause> fromClauseStack = new Stack<>();
	private TableSpace tableSpace;

	@Override
	public TableSpace visitFromElementSpace(SqmFromElementSpace fromElementSpace) {
		tableSpace = fromClauseStack.getCurrent().makeTableSpace();
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
		final EntityDescriptor entityMetadata = (EntityDescriptor) binding.getReferencedNavigable();
		final EntityTableGroup group = entityMetadata.createRootTableGroup(
				sqmRoot,
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
					public void addRestriction(Predicate predicate) {
						currentQuerySpec().addRestriction( predicate );
					}

					@Override
					public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
						return BaseSqmToSqlAstConverter.this.getSqlAliasBaseManager();
					}

					@Override
					public JoinType getTableReferenceJoinType() {
						// TableReferences within the TableGroup can be
						// inner-joined (unless they are optional, which is handled
						// inside the producers)
						return JoinType.INNER;
					}

					@Override
					public QueryOptions getQueryOptions() {
						return queryOptions;
					}
				}
		);

		tableSpace.setRootTableGroup( group );
		tableGroupStack.push( group );
		fromClauseIndex.crossReference( sqmRoot, group );

		group.applyAffectedTableNames( affectedTableNames::add );

		navigableReferenceStack.push( group.getNavigableReference() );

		log.tracef( "Resolved SqmRoot [%s] to new TableGroup [%s]", sqmRoot, group );

		return group;
	}

	@Override
	public NavigableReference visitRootEntityReference(SqmEntityReference sqmEntityReference) {
		return fromClauseIndex.findResolvedTableGroup( sqmEntityReference.getExportedFromElement() )
				.getNavigableReference();
	}

	@Override
	public Object visitQualifiedAttributeJoinFromElement(SqmNavigableJoin joinedFromElement) {
		final TableGroup existing = fromClauseIndex.findResolvedTableGroup( joinedFromElement );
		if ( existing != null ) {
			return existing;
		}

		final QuerySpec querySpec = currentQuerySpec();
		final TableGroupJoinProducer joinableAttribute = (TableGroupJoinProducer) joinedFromElement.getAttributeReference()
				.getReferencedNavigable();

		final TableGroup lhsTableGroup = fromClauseIndex.findResolvedTableGroup( joinedFromElement.getLhs() );



		final TableGroupJoin tableGroupJoin = joinableAttribute.createTableGroupJoin(
				joinedFromElement,
				joinedFromElement.getJoinType().getCorrespondingSqlJoinType(),
				new JoinedTableGroupContext() {
					@Override
					public TableGroup getLhs() {
						return lhsTableGroup;
					}

					@Override
					public ColumnReferenceQualifier getColumnReferenceQualifier() {
						return getLhs();
					}

					@Override
					public NavigablePath getNavigablePath() {
						return joinedFromElement.getNavigableReference().getNavigablePath();
					}

					@Override
					public QuerySpec getQuerySpec() {
						return querySpec;
					}

					@Override
					public TableSpace getTableSpace() {
						return tableSpace;
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
						return BaseSqmToSqlAstConverter.this.getSqlAliasBaseManager();
					}

					@Override
					public QueryOptions getQueryOptions() {
						return queryOptions;
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
		fromClauseIndex.crossReference( joinedFromElement, tableGroupJoin.getJoinedGroup() );

		return tableGroupJoin;
	}

	@Override
	public TableGroupJoin visitCrossJoinedFromElement(SqmCrossJoin joinedFromElement) {
		final QuerySpec querySpec = currentQuerySpec();
		final EntityDescriptor entityPersister = joinedFromElement.getIntrinsicSubclassEntityMetadata();
		final EntityTableGroup group = entityPersister.createRootTableGroup(
				joinedFromElement,
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
					public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
						return BaseSqmToSqlAstConverter.this.getSqlAliasBaseManager();
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

					@Override
					public QueryOptions getQueryOptions() {
						return queryOptions;
					}
				}
		);

		group.applyAffectedTableNames( affectedTableNames::add );

		tableGroupStack.push( group );
		fromClauseIndex.crossReference( joinedFromElement, group );

		return new TableGroupJoin( JoinType.CROSS, group, null );
	}

	@Override
	public Object visitQualifiedEntityJoinFromElement(SqmEntityJoin joinedFromElement) {
		throw new NotYetImplementedFor6Exception();
	}



	@Override
	public SelectClause visitSelectClause(SqmSelectClause selectClause) {
		currentClauseStack.push( Clause.SELECT );
		shallownessStack.push( SqmSelectToSqlAstConverter.Shallowness.SUBQUERY );
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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Expressions

	@Override
	public QueryResultProducer visitEntityIdentifierReference(SqmEntityIdentifierReference expression) {
		return new EntityIdentifierReference(
				(EntityValuedNavigableReference) navigableReferenceStack.getCurrent(),
				expression.getReferencedNavigable(),
				expression.getNavigablePath()
		);
	}

	@Override
	public BasicValuedNavigableReference visitBasicValuedSingularAttribute(SqmSingularAttributeReferenceBasic sqmAttributeReference) {
		return new BasicValuedNavigableReference(
				(NavigableContainerReference) navigableReferenceStack.getCurrent(),
				sqmAttributeReference.getReferencedNavigable(),
				sqmAttributeReference.getNavigablePath()
		);
	}

	@Override
	public EntityValuedNavigableReference visitEntityValuedSingularAttribute(SqmSingularAttributeReferenceEntity sqmAttributeReference) {
		final TableGroup resolvedTableGroup = fromClauseIndex.findResolvedTableGroup( sqmAttributeReference.getExportedFromElement() );
		if ( resolvedTableGroup != null ) {
			return (EntityValuedNavigableReference) resolvedTableGroup.getNavigableReference();
		}

		final NavigableContainerReference containerReference = (NavigableContainerReference) navigableReferenceStack.getCurrent();
		return new EntityValuedNavigableReference(
				containerReference,
				sqmAttributeReference.getReferencedNavigable(),
				sqmAttributeReference.getNavigablePath(),
				// todo (6.0) : need the qualifier covering both FK tables
				containerReference.getSqlExpressionQualifier(),
				queryOptions.getLockOptions().getEffectiveLockMode( sqmAttributeReference.getIdentificationVariable() )
		);
	}

	@Override
	public EmbeddableValuedNavigableReference visitEmbeddableValuedSingularAttribute(SqmSingularAttributeReferenceEmbedded sqmAttributeReference) {
		final NavigableContainerReference containerReference = (NavigableContainerReference) navigableReferenceStack.getCurrent();
		return new EmbeddableValuedNavigableReference(
				containerReference,
				sqmAttributeReference.getReferencedNavigable(),
				sqmAttributeReference.getNavigablePath(),
				LockMode.READ
		);
	}

	@Override
	public AnyValuedNavigableReference visitAnyValuedSingularAttribute(SqmSingularAttributeReferenceAny sqmAttributeReference) {
		final NavigableContainerReference containerReference = (NavigableContainerReference) navigableReferenceStack.getCurrent();
		return new AnyValuedNavigableReference(
				containerReference,
				sqmAttributeReference.getReferencedNavigable(),
				sqmAttributeReference.getNavigablePath(),
				containerReference.getSqlExpressionQualifier()
		);
	}

	@Override
	public PluralAttributeReference visitPluralAttribute(SqmPluralAttributeReference reference) {
		final NavigableContainerReference containerReference = (NavigableContainerReference) getNavigableReferenceStack().getCurrent();
		final PluralAttributeReference attributeReference = new PluralAttributeReference(
				containerReference,
				reference.getReferencedNavigable(),
				containerReference.getNavigablePath().append( reference.getReferencedNavigable().getNavigableName() )
		);

		navigableReferenceStack.push( attributeReference );

		return attributeReference;
	}

	@Override
	public QueryLiteral visitLiteralStringExpression(SqmLiteralString expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				resolveType( expression.getExpressableType(), StandardSpiBasicTypes.STRING ),
				getCurrentClauseStack().getCurrent() == Clause.SELECT
		);
	}

	protected BasicValuedExpressableType resolveType(
			BasicValuedExpressableType expressionType,
			BasicType defaultType) {
		return expressionType != null
				? expressionType
				: defaultType;
	}

	@Override
	public QueryLiteral visitLiteralCharacterExpression(SqmLiteralCharacter expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				resolveType( expression.getExpressableType(), StandardSpiBasicTypes.CHARACTER ),
				getCurrentClauseStack().getCurrent() == Clause.SELECT
		);
	}

	@Override
	public QueryLiteral visitLiteralDoubleExpression(SqmLiteralDouble expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				resolveType( expression.getExpressableType(), StandardSpiBasicTypes.DOUBLE ),
				getCurrentClauseStack().getCurrent() == Clause.SELECT
		);
	}

	@Override
	public QueryLiteral visitLiteralIntegerExpression(SqmLiteralInteger expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				resolveType( expression.getExpressableType(), StandardSpiBasicTypes.INTEGER ),
				getCurrentClauseStack().getCurrent() == Clause.SELECT
		);
	}

	@Override
	public QueryLiteral visitLiteralBigIntegerExpression(SqmLiteralBigInteger expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				resolveType( expression.getExpressableType(), StandardSpiBasicTypes.BIG_INTEGER ),
				getCurrentClauseStack().getCurrent() == Clause.SELECT
		);
	}

	@Override
	public QueryLiteral visitLiteralBigDecimalExpression(SqmLiteralBigDecimal expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				resolveType( expression.getExpressableType(), StandardSpiBasicTypes.BIG_DECIMAL ),
				getCurrentClauseStack().getCurrent() == Clause.SELECT
		);
	}

	@Override
	public QueryLiteral visitLiteralFloatExpression(SqmLiteralFloat expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				resolveType( expression.getExpressableType(), StandardSpiBasicTypes.FLOAT ),
				getCurrentClauseStack().getCurrent() == Clause.SELECT
		);
	}

	@Override
	public QueryLiteral visitLiteralLongExpression(SqmLiteralLong expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				resolveType( expression.getExpressableType(), StandardSpiBasicTypes.LONG ),
				getCurrentClauseStack().getCurrent() == Clause.SELECT
		);
	}

	@Override
	public QueryLiteral visitLiteralTrueExpression(SqmLiteralTrue expression) {
		return new QueryLiteral(
				Boolean.TRUE,
				resolveType( expression.getExpressableType(), StandardSpiBasicTypes.BOOLEAN ),
				getCurrentClauseStack().getCurrent() == Clause.SELECT
		);
	}

	@Override
	public QueryLiteral visitLiteralFalseExpression(SqmLiteralFalse expression) {
		return new QueryLiteral(
				Boolean.FALSE,
				resolveType( expression.getExpressableType(), StandardSpiBasicTypes.BOOLEAN ),
				getCurrentClauseStack().getCurrent() == Clause.SELECT
		);
	}

	@Override
	public QueryLiteral visitLiteralNullExpression(SqmLiteralNull expression) {
		return new QueryLiteral(
				null,
				expression.getExpressableType(),
				getCurrentClauseStack().getCurrent() == Clause.SELECT
		);
	}

	@Override
	public Object visitConstantEnumExpression(SqmConstantEnum expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				expression.getExpressableType(),
				getCurrentClauseStack().getCurrent() == Clause.SELECT
		);
	}

	@Override
	public Object visitConstantFieldReference(SqmConstantFieldReference expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				expression.getExpressableType(),
				getCurrentClauseStack().getCurrent() == Clause.SELECT
		);
	}

	@Override
	public NamedParameter visitNamedParameterExpression(SqmNamedParameter expression) {
		return new NamedParameter(
				expression.getName(),
				(AllowableParameterType) expression.getExpressableType()
		);
	}

	@Override
	public PositionalParameter visitPositionalParameterExpression(SqmPositionalParameter expression) {
		return new PositionalParameter(
				expression.getPosition(),
				(AllowableParameterType) expression.getExpressableType()
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// non-standard functions

	@Override
	public Object visitGenericFunction(SqmGenericFunction expression) {
		shallownessStack.push( Shallowness.FUNCTION );
		try {
			return new NonStandardFunction(
					expression.getFunctionName(),
					expression.getExpressableType(),
					visitArguments( expression.getArguments() )
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	private List<Expression> visitArguments(List<SqmExpression> sqmArguments) {
		if ( sqmArguments == null || sqmArguments.isEmpty() ) {
			return Collections.emptyList();
		}

		final ArrayList<Expression> sqlAstArguments = new ArrayList<>();
		for ( SqmExpression sqmArgument : sqmArguments ) {
			sqlAstArguments.add( (Expression) sqmArgument.accept( this ) );
		}

		return sqlAstArguments;
	}

	@Override
	public Object visitSqlAstFunctionProducer(SqlAstFunctionProducer sqlAstFunctionProducer) {
		shallownessStack.push( Shallowness.FUNCTION );
		try {
			return sqlAstFunctionProducer.convertToSqlAst( this );
		}
		finally {
			shallownessStack.pop();
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// standard functions

	@Override
	public Object visitAbsFunction(SqmAbsFunction function) {
		shallownessStack.push( Shallowness.FUNCTION );

		try {
			return new AbsFunction( (Expression) function.getArgument().accept( this ) );
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public AvgFunction visitAvgFunction(SqmAvgFunction expression) {
		shallownessStack.push( Shallowness.FUNCTION );

		try {
			return new AvgFunction(
					(Expression) expression.getArgument().accept( this ),
					expression.isDistinct(),
					expression.getExpressableType()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public Object visitBitLengthFunction(SqmBitLengthFunction function) {
		shallownessStack.push( Shallowness.FUNCTION );

		try {
			return new BitLengthFunction(
					(Expression) function.getArgument().accept( this ),
					(BasicValuedExpressableType) function.getExpressableType()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public Object visitCastFunction(SqmCastFunction expression) {
		shallownessStack.push( Shallowness.FUNCTION );

		try {
			return new CastFunction(
					(Expression) expression.getExpressionToCast().accept( this ),
					expression.getExpressableType(),
					expression.getExplicitSqlCastTarget()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public CountFunction visitCountFunction(SqmCountFunction expression) {
		shallownessStack.push( Shallowness.FUNCTION );

		try {
			return new CountFunction(
					(Expression) expression.getArgument().accept( this ),
					expression.isDistinct(),
					expression.getExpressableType()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public ConcatFunction visitConcatFunction(SqmConcatFunction function) {
		return new ConcatFunction(
				collectionExpressions( function.getExpressions() ),
				(BasicValuedExpressableType) function.getExpressableType()
		);
	}

	private List<Expression> collectionExpressions(List<SqmExpression> sqmExpressions) {
		if ( sqmExpressions == null || sqmExpressions.isEmpty() ) {
			return Collections.emptyList();
		}

		if ( sqmExpressions.size() == 1 ) {
			return Collections.singletonList( (Expression) sqmExpressions.get( 0 ).accept( this ) );
		}

		final List<Expression> results = new ArrayList<>();
		sqmExpressions.forEach( sqmExpression -> results.add( (Expression) sqmExpression.accept( this ) ) );
		return results;
	}

	@Override
	public CurrentDateFunction visitCurrentDateFunction(SqmCurrentDateFunction function) {
		return new CurrentDateFunction( (BasicValuedExpressableType) function.getExpressableType() );
	}

	@Override
	public CurrentTimeFunction visitCurrentTimeFunction(SqmCurrentTimeFunction function) {
		return new CurrentTimeFunction( (BasicValuedExpressableType) function.getExpressableType() );
	}

	@Override
	public CurrentTimestampFunction visitCurrentTimestampFunction(SqmCurrentTimestampFunction function) {
		return new CurrentTimestampFunction( (BasicValuedExpressableType) function.getExpressableType() );
	}

	@Override
	public ExtractFunction visitExtractFunction(SqmExtractFunction function) {
		shallownessStack.push( Shallowness.FUNCTION );

		try {
			return new ExtractFunction(
					(Expression) function.getUnitToExtract().accept( this ),
					(Expression) function.getExtractionSource().accept( this ),
					(BasicValuedExpressableType) function.getExpressableType()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public CountStarFunction visitCountStarFunction(SqmCountStarFunction expression) {
		shallownessStack.push( Shallowness.FUNCTION );

		try {
			return new CountStarFunction(
					expression.isDistinct(),
					expression.getExpressableType()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public LengthFunction visitLengthFunction(SqmLengthFunction function) {
		shallownessStack.push( Shallowness.FUNCTION );

		try {
			return new LengthFunction(
					(Expression) function.getArgument().accept( this ),
					function.getExpressableType()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public LocateFunction visitLocateFunction(SqmLocateFunction function) {
		shallownessStack.push( Shallowness.FUNCTION );

		try {
			return new LocateFunction(
					(Expression) function.getPatternString().accept( this ),
					(Expression) function.getStringToSearch().accept( this ),
					function.getStartPosition() == null
							? null
							: (Expression) function.getStartPosition().accept( this )
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public Object visitLowerFunction(SqmLowerFunction function) {
		shallownessStack.push( Shallowness.FUNCTION );

		try {
			return new LowerFunction(
					(Expression) function.getArgument().accept( this ),
					(BasicValuedExpressableType) function.getExpressableType()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public MaxFunction visitMaxFunction(SqmMaxFunction expression) {
		shallownessStack.push( Shallowness.FUNCTION );

		try {
			return new MaxFunction(
					(Expression) expression.getArgument().accept( this ),
					expression.isDistinct(),
					expression.getExpressableType()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public MinFunction visitMinFunction(SqmMinFunction expression) {
		shallownessStack.push( Shallowness.FUNCTION );

		try {
			return new MinFunction(
					(Expression) expression.getArgument().accept( this ),
					expression.isDistinct(),
					expression.getExpressableType()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public Object visitModFunction(SqmModFunction function) {
		shallownessStack.push( Shallowness.FUNCTION );

		final Expression dividend = (Expression) function.getDividend().accept( this );
		final Expression divisor = (Expression) function.getDivisor().accept( this );
		try {
			return new ModFunction(
					dividend,
					divisor,
					(BasicValuedExpressableType) function.getExpressableType()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public SumFunction visitSumFunction(SqmSumFunction expression) {
		shallownessStack.push( Shallowness.FUNCTION );

		try {
			return new SumFunction(
					(Expression) expression.getArgument().accept( this ),
					expression.isDistinct(),
					expression.getExpressableType()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public Object visitUnaryOperationExpression(SqmUnaryOperation expression) {
		shallownessStack.push( Shallowness.NONE );

		try {
			return new UnaryOperation(
					interpret( expression.getOperation() ),
					(Expression) expression.getOperand().accept( this ),
					expression.getExpressableType()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	private UnaryOperation.Operator interpret(SqmUnaryOperation.Operation operation) {
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
	public Expression visitBinaryArithmeticExpression(SqmBinaryArithmetic expression) {
		shallownessStack.push( Shallowness.NONE );

		try {
			if ( expression.getOperation() == SqmBinaryArithmetic.Operation.MODULO ) {
				return new NonStandardFunction(
						"mod",
						null, //(BasicType) extractOrmType( expression.getExpressableType() ),
						(Expression) expression.getLeftHandOperand().accept( this ),
						(Expression) expression.getRightHandOperand().accept( this )
				);
			}
			return new BinaryArithmeticExpression(
					interpret( expression.getOperation() ),
					(Expression) expression.getLeftHandOperand().accept( this ),
					(Expression) expression.getRightHandOperand().accept( this ),
					(BasicValuedExpressableType) expression.getExpressableType()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	private BinaryArithmeticExpression.Operation interpret(SqmBinaryArithmetic.Operation operation) {
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
	public CoalesceFunction visitCoalesceFunction(SqmCoalesceFunction expression) {
		final CoalesceFunction result = new CoalesceFunction();
		for ( SqmExpression value : expression.getArguments() ) {
			result.value( (Expression) value.accept( this ) );
		}

		return result;
	}

	@Override
	public CaseSimpleExpression visitSimpleCaseExpression(SqmCaseSimple expression) {
		final CaseSimpleExpression result = new CaseSimpleExpression(
				expression.getExpressableType(),
				(Expression) expression.getFixture().accept( this )
		);

		for ( SqmCaseSimple.WhenFragment whenFragment : expression.getWhenFragments() ) {
			result.when(
					(Expression) whenFragment.getCheckValue().accept( this ),
					(Expression) whenFragment.getResult().accept( this )
			);
		}

		result.otherwise( (Expression) expression.getOtherwise().accept( this ) );

		return result;
	}

	@Override
	public CaseSearchedExpression visitSearchedCaseExpression(SqmCaseSearched expression) {
		final CaseSearchedExpression result = new CaseSearchedExpression( expression.getExpressableType() );

		for ( SqmCaseSearched.WhenFragment whenFragment : expression.getWhenFragments() ) {
			result.when(
					(Predicate) whenFragment.getPredicate().accept( this ),
					(Expression) whenFragment.getResult().accept( this )
			);
		}

		result.otherwise( (Expression) expression.getOtherwise().accept( this ) );

		return result;
	}

	@Override
	public NullifFunction visitNullifFunction(SqmNullifFunction expression) {
		return new NullifFunction(
				(Expression) expression.getFirstArgument().accept( this ),
				(Expression) expression.getSecondArgument().accept( this ),
				expression.getExpressableType()
		);
	}

	@Override
	public Object visitTrimFunction(SqmTrimFunction expression) {
		return new TrimFunction(
				expression.getSpecification(),
				(Expression) expression.getTrimCharacter().accept( this ),
				(Expression) expression.getSource().accept( this )
		);
	}

	@Override
	public Object visitUpperFunction(SqmUpperFunction sqmFunction) {
		return new UpperFunction(
				(Expression) sqmFunction.getArgument().accept( this ),
				(BasicValuedExpressableType) sqmFunction.getExpressableType()
		);

	}

	@Override
	public ConcatFunction visitConcatExpression(SqmConcat expression) {
		return new ConcatFunction(
				Arrays.asList(
						(Expression)expression.getLeftHandOperand().accept( this ),
						(Expression) expression.getRightHandOperand().accept( this )
				),
				expression.getExpressableType()
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

	@Override
	public ColumnReference visitExplicitColumnReference(SqmColumnReference sqmColumnReference) {
		final TableGroup tableGroup = fromClauseIndex.findResolvedTableGroup(
				sqmColumnReference.getSqmFromBase()
		);

		final ColumnReference columnReference = tableGroup.locateColumnReferenceByName( sqmColumnReference.getColumnName() );

		if ( columnReference == null ) {
			throw new HibernateException( "Could not resolve ColumnReference" );
		}

		return columnReference;
	}


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
		final Expression lhs = toExpression( predicate.getLeftHandExpression().accept( this ) );
		final Expression rhs = toExpression( predicate.getRightHandExpression().accept( this ) );

		return new RelationalPredicate(
				interpret( predicate.getOperator() ),
				lhs,
				rhs
		);
	}

	@SuppressWarnings("unchecked")
	private Expression toExpression(Object value) {
		if ( value instanceof NavigableReference ) {
			final NavigableReference navigableReference = (NavigableReference) value;
			final List list = navigableReference.getNavigable().resolveColumnReferences(
					navigableReference.getSqlExpressionQualifier(),
					this
			);
			if ( list.size() == 1 ) {
				assert list.get( 0 ) instanceof Expression;
				return (Expression) list.get( 0 );
			}
			return new SqlTuple( list );
		}

		// any other special cases?

		return (Expression) value;
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
}
