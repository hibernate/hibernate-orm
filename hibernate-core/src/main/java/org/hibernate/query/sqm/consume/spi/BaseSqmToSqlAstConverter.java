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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.annotations.Remove;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.metamodel.model.domain.spi.BasicValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.EmbeddedValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.query.BinaryArithmeticOperator;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.UnaryArithmeticOperator;
import org.hibernate.query.criteria.sqm.JpaParameterSqmWrapper;
import org.hibernate.query.spi.ComparisonOperator;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmCaseSearched;
import org.hibernate.query.sqm.tree.expression.SqmCaseSimple;
import org.hibernate.query.sqm.tree.expression.SqmConcat;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;
import org.hibernate.query.sqm.tree.expression.SqmSubQuery;
import org.hibernate.query.sqm.tree.expression.SqmUnaryOperation;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmPluralAttributeReference;
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
import org.hibernate.query.sqm.tree.expression.function.SqmStrFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmSubstringFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmSumFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmTrimFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmUpperFunction;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.predicate.AndSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.BetweenSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.GroupedSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.InListSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.InSubQuerySqmPredicate;
import org.hibernate.query.sqm.tree.predicate.LikeSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.NegatedSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.NullnessSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.OrSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmComparisonPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.internal.SqlAstQuerySpecProcessingStateImpl;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.produce.ordering.internal.SqmColumnReference;
import org.hibernate.sql.ast.produce.spi.FromClauseAccess;
import org.hibernate.sql.ast.produce.spi.FromClauseIndex;
import org.hibernate.sql.ast.produce.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationState;
import org.hibernate.sql.ast.produce.spi.SqlAstFunctionProducer;
import org.hibernate.sql.ast.produce.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.produce.spi.SqlAstQuerySpecProcessingState;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.produce.spi.TableGroupJoinProducer;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.ast.produce.sqm.spi.SqmSelectToSqlAstConverter;
import org.hibernate.sql.ast.produce.sqm.spi.SqmToSqlAstConverter;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.SelectStatement;
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
import org.hibernate.sql.ast.tree.spi.expression.NonStandardFunction;
import org.hibernate.sql.ast.tree.spi.expression.NullifFunction;
import org.hibernate.sql.ast.tree.spi.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.spi.expression.SqlTuple;
import org.hibernate.sql.ast.tree.spi.expression.SubQuery;
import org.hibernate.sql.ast.tree.spi.expression.SubstrFunction;
import org.hibernate.sql.ast.tree.spi.expression.SumFunction;
import org.hibernate.sql.ast.tree.spi.expression.TrimFunction;
import org.hibernate.sql.ast.tree.spi.expression.UnaryOperation;
import org.hibernate.sql.ast.tree.spi.expression.UpperFunction;
import org.hibernate.sql.ast.tree.spi.expression.domain.BasicValuedNavigableReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.EmbeddableValuedNavigableReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.EntityValuedNavigableReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableContainerReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.PluralAttributeReference;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.spi.predicate.BetweenPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.GroupedPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.ast.tree.spi.predicate.LikePredicate;
import org.hibernate.sql.ast.tree.spi.predicate.NegatedPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.sql.ast.tree.spi.select.SelectClause;
import org.hibernate.sql.ast.tree.spi.sort.SortSpecification;
import org.hibernate.sql.exec.internal.JdbcParametersImpl;
import org.hibernate.sql.exec.internal.StandardJdbcParameterImpl;
import org.hibernate.sql.exec.spi.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameters;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;

import org.jboss.logging.Logger;

import static org.hibernate.query.BinaryArithmeticOperator.ADD;
import static org.hibernate.query.BinaryArithmeticOperator.DIVIDE;
import static org.hibernate.query.BinaryArithmeticOperator.MODULO;
import static org.hibernate.query.BinaryArithmeticOperator.MULTIPLY;
import static org.hibernate.query.BinaryArithmeticOperator.QUOT;
import static org.hibernate.query.BinaryArithmeticOperator.SUBTRACT;

/**
 * @author Steve Ebersole
 */
public abstract class BaseSqmToSqlAstConverter
		extends BaseSemanticQueryWalker
		implements SqmToSqlAstConverter, SqlAstCreationState {

	private static final Logger log = Logger.getLogger( BaseSqmToSqlAstConverter.class );

	protected enum Shallowness {
		NONE,
		CTOR,
		FUNCTION,
		SUBQUERY
	}

	private final SqlAstCreationContext creationContext;
	private final QueryOptions queryOptions;
	private final LoadQueryInfluencers loadQueryInfluencers;
	private final Callback callback;

	private final SqlAliasBaseManager sqlAliasBaseManager = new SqlAliasBaseManager();

	private final FromClauseIndex fromClauseIndex = new FromClauseIndex();

	private final Stack<SqlAstProcessingState> processingStateStack = new StandardStack<>();

	private final Stack<Clause> currentClauseStack = new StandardStack<>();
	private final Stack<SqmSelectToSqlAstConverter.Shallowness> shallownessStack = new StandardStack<>( SqmSelectToSqlAstConverter.Shallowness.NONE );


	@Remove
	private final Stack<TableGroup> tableGroupStack = new StandardStack<>();
	@Remove
	private final Stack<NavigableReference> navigableReferenceStack = new StandardStack<>();

	public BaseSqmToSqlAstConverter(
			SqlAstCreationContext creationContext,
			QueryOptions queryOptions,
			LoadQueryInfluencers loadQueryInfluencers,
			Callback callback) {
		super( creationContext.getDomainModel().getTypeConfiguration(), creationContext.getServiceRegistry() );
		this.creationContext = creationContext;
		this.queryOptions = queryOptions;
		this.loadQueryInfluencers = loadQueryInfluencers;
		this.callback = callback;
	}

	protected Stack<SqlAstProcessingState> getProcessingStateStack() {
		return processingStateStack;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlAstCreationState

	@Override
	public SqlAstCreationContext getCreationContext() {
		return creationContext;
	}

	@Override
	public SqlAstProcessingState getCurrentProcessingState() {
		return processingStateStack.getCurrent();
	}

	@Override
	public SqlExpressionResolver getSqlExpressionResolver() {
		return getCurrentProcessingState().getSqlExpressionResolver();
	}

	@Override
	public FromClauseAccess getFromClauseAccess() {
		return fromClauseIndex;
	}

	@Override
	public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
		return sqlAliasBaseManager;
	}

	@Override
	public LockMode determineLockMode(String identificationVariable) {
		return queryOptions.getLockOptions().getEffectiveLockMode( identificationVariable );
	}

	@Override
	public List<Fetch> visitFetches(FetchParent fetchParent) {
		return Collections.emptyList();
	}

	protected QuerySpec currentQuerySpec() {
		return ( (SqlAstQuerySpecProcessingState) processingStateStack.getCurrent() ).getInflightQuerySpec();
	}

	public QueryOptions getQueryOptions() {
		return queryOptions;
	}

	public FromClauseIndex getFromClauseIndex() {
		return fromClauseIndex;
	}

	public Stack<Clause> getCurrentClauseStack() {
		return currentClauseStack;
	}

	public Stack<TableGroup> getTableGroupStack() {
		return tableGroupStack;
	}

	public Stack<NavigableReference> getNavigableReferenceStack() {
		return navigableReferenceStack;
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



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Statements

	@Override
	public Object visitUpdateStatement(SqmUpdateStatement statement) {
		throw new AssertionFailure( "UpdateStatement not supported" );
	}

	@Override
	public Object visitDeleteStatement(SqmDeleteStatement statement) {
		throw new AssertionFailure( "DeleteStatement not supported" );
	}

	@Override
	public Object visitInsertSelectStatement(SqmInsertSelectStatement statement) {
		throw new AssertionFailure( "InsertStatement not supported" );
	}

	@Override
	public SelectStatement visitSelectStatement(SqmSelectStatement statement) {
		throw new AssertionFailure( "SelectStatement not supported" );
	}


	@Override
	public QuerySpec visitQuerySpec(SqmQuerySpec sqmQuerySpec) {
		final QuerySpec sqlQuerySpec = new QuerySpec( processingStateStack.isEmpty() );

		processingStateStack.push(
				new SqlAstQuerySpecProcessingStateImpl(
						sqlQuerySpec,
						processingStateStack.getCurrent(),
						this,
						currentClauseStack::getCurrent,
						() -> (expression) -> {},
						() -> sqlQuerySpec.getSelectClause()::addSqlSelection
				)
		);

		try {
			// we want to visit the from-clause first
			visitFromClause( sqmQuerySpec.getFromClause() );

			final SqmSelectClause selectClause = sqmQuerySpec.getSelectClause();
			if ( selectClause != null ) {
				visitSelectClause( selectClause );
			}

			final SqmWhereClause whereClause = sqmQuerySpec.getWhereClause();
			if ( whereClause != null && whereClause.getPredicate() != null ) {
				currentClauseStack.push( Clause.WHERE );
				try {
					sqlQuerySpec.setWhereClauseRestrictions(
							(Predicate) whereClause.getPredicate().accept( this )
					);
				}
				finally {
					currentClauseStack.pop();
				}
			}

			// todo : group-by
			// todo : having

			if ( sqmQuerySpec.getOrderByClause() != null ) {
				currentClauseStack.push( Clause.ORDER );
				try {
					for ( SqmSortSpecification sortSpecification : sqmQuerySpec.getOrderByClause().getSortSpecifications() ) {
						sqlQuerySpec.addSortSpecification( visitSortSpecification( sortSpecification ) );
					}
				}
				finally {
					currentClauseStack.pop();
				}
			}

			sqlQuerySpec.setLimitClauseExpression( visitLimitExpression( sqmQuerySpec.getLimitExpression() ) );
			sqlQuerySpec.setOffsetClauseExpression( visitOffsetExpression( sqmQuerySpec.getOffsetExpression() ) );

			return sqlQuerySpec;
		}
		finally {
			processingStateStack.pop();
		}
	}

	@Override
	public Void visitOrderByClause(SqmOrderByClause orderByClause) {
		super.visitOrderByClause( orderByClause );
		return null;
	}

	@Override
	public SortSpecification visitSortSpecification(SqmSortSpecification sortSpecification) {
		return new SortSpecification(
				toExpression( sortSpecification.getSortExpression().accept( this ) ),
				sortSpecification.getCollation(),
				sortSpecification.getSortOrder()
		);
	}

	@Override
	public Expression visitOffsetExpression(SqmExpression expression) {
		if ( expression == null ) {
			return null;
		}

		currentClauseStack.push( Clause.OFFSET );
		try {
			return (Expression) expression.accept( this );
		}
		finally {
			currentClauseStack.pop();
		}
	}

	@Override
	public Expression visitLimitExpression(SqmExpression expression) {
		if ( expression == null ) {
			return null;
		}

		currentClauseStack.push( Clause.LIMIT );
		try {
			return (Expression) expression.accept( this );
		}
		finally {
			currentClauseStack.pop();
		}
	}


	@Override
	public Void visitFromClause(SqmFromClause sqmFromClause) {
		currentClauseStack.push( Clause.FROM );

		try {
			sqmFromClause.visitRoots(
					sqmRoot -> {
						final NavigableReference rootReference = visitRootEntityFromElement( sqmRoot );
						assert rootReference instanceof TableGroup;
						currentQuerySpec().getFromClause().addRoot( (TableGroup) rootReference );
					}
			);
		}
		finally {
			currentClauseStack.pop();
		}
		return null;
	}


	@Override
	public NavigableReference visitRootEntityFromElement(SqmRoot sqmRoot) {
		log.tracef( "Starting resolution of SqmRoot [%s] to TableGroup", sqmRoot );

		if ( fromClauseIndex.isResolved( sqmRoot ) ) {
			final TableGroup resolvedTableGroup = fromClauseIndex.findResolvedTableGroup( sqmRoot );
			log.tracef( "SqmRoot [%s] resolved to existing TableGroup [%s]", sqmRoot, resolvedTableGroup );
			return resolvedTableGroup;
		}

		final EntityTypeDescriptor entityDescriptor = sqmRoot.getReferencedNavigable().getEntityDescriptor();
		final TableGroup group = entityDescriptor.createRootTableGroup(
				sqmRoot.getUniqueIdentifier(),
				sqmRoot.getNavigablePath(),
				sqmRoot.getExplicitAlias(),
				JoinType.INNER,
				LockMode.NONE,
				this
		);

		fromClauseIndex.crossReference( sqmRoot, group );

		log.tracef( "Resolved SqmRoot [%s] to new TableGroup [%s]", sqmRoot, group );

		sqmRoot.visitJoins(
				sqmJoin -> {
					final TableGroupJoin tableGroupJoin = (TableGroupJoin) sqmJoin.accept( this );
					if ( tableGroupJoin != null ) {
						group.addTableGroupJoin( tableGroupJoin );
					}
				}
		);

		return group;
	}

	@Override
	public NavigableReference visitRootEntityReference(SqmEntityReference sqmEntityReference) {
		return fromClauseIndex.getTableGroup( sqmEntityReference.getNavigablePath() );
	}

	@Override
	public TableGroupJoin visitQualifiedAttributeJoinFromElement(SqmNavigableJoin sqmJoin) {
		final NavigableContainer<?> joinedNavigable = sqmJoin.as( NavigableContainer.class );

		final TableGroupJoin tableJoinJoin = fromClauseIndex.findTableGroupJoin( sqmJoin.getNavigablePath() );
		if ( tableJoinJoin != null ) {
			return tableJoinJoin;
		}

		final TableGroup lhsTableGroup = fromClauseIndex.findTableGroup( sqmJoin.getLhs().getNavigablePath() );

		if ( joinedNavigable instanceof EmbeddedValuedNavigable ) {
			// we need some special handling for embeddables...

			// Above we checked for a TableGroupJoin associated with the `sqmJoin` path - but for
			// an embeddable, check for its LHS too
			final TableGroupJoin lhsTableGroupJoin = fromClauseIndex.findTableGroupJoin( sqmJoin.getNavigablePath() );
			if ( lhsTableGroupJoin != null ) {
				fromClauseIndex.register( sqmJoin, lhsTableGroupJoin );
				return lhsTableGroupJoin;
			}

			// Next, although we don't want an actual TableGroup/TableGroupJoin created just for the
			// embeddable, we do need to register a TableGroup against its NavigablePath.
			// Specifically the TableGroup associated with the embeddable's LHS
			fromClauseIndex.registerTableGroup( sqmJoin.getNavigablePath(), lhsTableGroup );

			// we also still want to process its joins, adding them to the LHS TableGroup
			sqmJoin.visitJoins(
					sqmJoinJoin -> {
						final TableGroupJoin tableGroupJoin = (TableGroupJoin) sqmJoinJoin.accept( this );
						if ( tableGroupJoin != null ) {
							lhsTableGroup.addTableGroupJoin( tableGroupJoin );
						}
					}
			);

			// return null - there is no TableGroupJoin that needs to be added
			return null;
		}


		final TableGroupJoinProducer joinProducer = joinedNavigable.as( TableGroupJoinProducer.class );

		final TableGroupJoin tableGroupJoin = joinProducer.createTableGroupJoin(
				sqmJoin.getUniqueIdentifier(),
				sqmJoin.getNavigablePath(),
				fromClauseIndex.getTableGroup( sqmJoin.getLhs().getNavigablePath() ),
				sqmJoin.getExplicitAlias(),
				sqmJoin.getJoinType().getCorrespondingSqlJoinType(),
				LockMode.NONE,
				this
		);

		fromClauseIndex.register( sqmJoin, tableGroupJoin );
		lhsTableGroup.addTableGroupJoin( tableGroupJoin );

		// add any additional join restrictions
		if ( sqmJoin.getJoinPredicate() != null ) {
			currentQuerySpec().addRestriction(
					(Predicate) sqmJoin.getJoinPredicate().accept( this )
			);
		}


		return tableGroupJoin;
	}

	@Override
	public TableGroup visitCrossJoinedFromElement(SqmCrossJoin sqmJoin) {
		final EntityTypeDescriptor entityMetadata = sqmJoin.getReferencedNavigable().getEntityDescriptor();
		final TableGroup group = entityMetadata.createRootTableGroup(
				sqmJoin.getUniqueIdentifier(),
				sqmJoin.getNavigablePath(),
				sqmJoin.getExplicitAlias(),
				JoinType.INNER,
				LockMode.NONE,
				this
		);

		fromClauseIndex.crossReference( sqmJoin, group );

		sqmJoin.visitJoins(
				sqmJoinJoin -> {
					final TableGroupJoin tableGroupJoin = (TableGroupJoin) sqmJoinJoin.accept( this );
					if ( tableGroupJoin != null ) {
						group.addTableGroupJoin( tableGroupJoin );
					}
				}
		);

		return new TableGroupJoin( JoinType.CROSS, group, null ).getJoinedGroup();
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

	@Override
	public BasicValuedNavigableReference visitBasicValuedPath(SqmBasicValuedSimplePath path) {
		return new BasicValuedNavigableReference(
				path.getNavigablePath(),
				path.getReferencedNavigable(),
				this
		);
	}

	@Override
	public EmbeddableValuedNavigableReference visitEmbeddableValuedPath(SqmEmbeddedValuedSimplePath path) {
		return new EmbeddableValuedNavigableReference(
				path.getNavigablePath(),
				path.getReferencedNavigable(),
				determineLockMode( path.getIdentificationVariable() ),
				this
		);
	}

	@Override
	public Object visitEntityValuedPath(SqmEntityValuedSimplePath path) {
		return new EntityValuedNavigableReference(
				path.getNavigablePath(),
				path.getReferencedNavigable(),
				determineLockMode( path.getIdentificationVariable() ),
				this
		);
	}

	@Override
	public Object visitPluralValuedPath(SqmPluralValuedSimplePath path) {
		return super.visitPluralValuedPath( path );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Expressions

	@Override
	public PluralAttributeReference visitPluralAttribute(SqmPluralAttributeReference reference) {

		// todo (6.0) : most likely how we execute this depends on the context - where is it used?

		final PluralPersistentAttribute referencedCollection = reference.getReferencedNavigable();

		final NavigableContainerReference containerReference = (NavigableContainerReference) getNavigableReferenceStack().getCurrent();

		final NavigablePath navigablePath = containerReference.getNavigablePath().append( referencedCollection.getNavigableName() );

		final PluralAttributeReference result;

		final NavigableReference resolvedNavigableReference = fromClauseIndex.findResolvedNavigableReference( navigablePath );
		if ( resolvedNavigableReference != null ) {
			assert resolvedNavigableReference instanceof PluralAttributeReference;
			result = ( PluralAttributeReference ) resolvedNavigableReference;
		}
		else {
			result = new PluralAttributeReference(
					containerReference,
					referencedCollection,
					navigablePath,
					containerReference.getColumnReferenceQualifier(),
					getQueryOptions().getLockOptions().getEffectiveLockMode( reference.getIdentificationVariable() )
			);
		}

		navigableReferenceStack.push( result );

		return result;
	}

	@Override
	public Object visitLiteral(SqmLiteral literal) {
		return new QueryLiteral(
				literal.getLiteralValue(),
				literal.getExpressableType().getSqlExpressableType(),
				getCurrentClauseStack().getCurrent()
		);
	}

	private final Map<SqmParameter,List<JdbcParameter>> jdbcParamsBySqmParam = new TreeMap<>(
			(o1, o2) -> {
				if ( o1 instanceof SqmNamedParameter ) {
					final SqmNamedParameter one = (SqmNamedParameter) o1;
					final SqmNamedParameter another = (SqmNamedParameter) o2;

					return one.getName().compareTo( another.getName() );
				}
				else if ( o1 instanceof SqmPositionalParameter ) {
					final SqmPositionalParameter one = (SqmPositionalParameter) o1;
					final SqmPositionalParameter another = (SqmPositionalParameter) o2;

					return one.getPosition().compareTo( another.getPosition() );
				}

				throw new HibernateException( "Unexpected SqmParameter type for comparison : " + o1 + " & " + o2 );
			}
	);

	public Map<SqmParameter, List<JdbcParameter>> getJdbcParamsBySqmParam() {
		return jdbcParamsBySqmParam;
	}

	/**
	 * Ultimately used for "parameter metadata" and as key for a JdbcParameterBinding
	 */
	private final JdbcParameters jdbcParameters = new JdbcParametersImpl();

	public JdbcParameters getJdbcParameters() {
		return jdbcParameters;
	}

	@Override
	public Expression visitNamedParameterExpression(SqmNamedParameter expression) {
		final List<JdbcParameter> jdbcParameterList;

		// todo (6.0) : see note on `SqmExpression#getExpressableType` regarding the role of `#getExpressableType` and possibly adding a separate method triggering the "resolution"
		//		here is where we would use that new one... as we walk that SQM - we know all inferences have been set
		List<JdbcParameter> existing = this.jdbcParamsBySqmParam.get( expression );

		AllowableParameterType expressableType = expression.getExpressableType();


		if ( existing != null ) {
			if ( expressableType != null ) {
				final int number = expressableType.getNumberOfJdbcParametersNeeded();
				assert existing.size() == number;
			}
			jdbcParameterList = existing;
		}
		else {
			jdbcParameterList = new ArrayList<>();
			if ( expressableType == null ) {
				jdbcParameterList.add(
						new StandardJdbcParameterImpl(
								jdbcParameters.getJdbcParameters().size(),
								null,
								currentClauseStack.getCurrent(),
								getCreationContext().getDomainModel().getTypeConfiguration()
						)
				);
			}
			else {
				//noinspection Convert2Lambda
				expressableType.visitJdbcTypes(
						new Consumer<SqlExpressableType>() {
							@Override
							public void accept(SqlExpressableType type) {
								jdbcParameterList.add(
										new StandardJdbcParameterImpl(
												jdbcParameters.getJdbcParameters().size(),
												type,
												currentClauseStack.getCurrent(),
												getCreationContext().getDomainModel().getTypeConfiguration()
										)
								);
							}
						},
						currentClauseStack.getCurrent(),
						getCreationContext().getDomainModel().getTypeConfiguration()
				);
			}

			jdbcParamsBySqmParam.put( expression, jdbcParameterList );
			jdbcParameters.addParameters( jdbcParameterList );
		}

		if ( jdbcParameterList.size() > 1 ) {
			return new SqlTuple( jdbcParameterList );
		}
		else {
			return jdbcParameterList.get( 0 );
		}
	}

	@Override
	public Object visitPositionalParameterExpression(SqmPositionalParameter expression) {
		final List<JdbcParameter> jdbcParameterList;

		List<JdbcParameter> existing = this.jdbcParamsBySqmParam.get( expression );
		if ( existing != null ) {
			final int number = expression.getExpressableType().getNumberOfJdbcParametersNeeded();
			assert existing.size() == number;
			jdbcParameterList = existing;
		}
		else {
			jdbcParameterList = new ArrayList<>();

			//noinspection Convert2Lambda
			expression.getExpressableType().visitJdbcTypes(
					new Consumer<SqlExpressableType>() {
						@Override
						public void accept(SqlExpressableType type) {
							jdbcParameterList.add(
									new StandardJdbcParameterImpl(
											jdbcParameters.getJdbcParameters().size(),
											type,
											currentClauseStack.getCurrent(),
											getCreationContext().getDomainModel().getTypeConfiguration()
									)
							);
						}
					},
					currentClauseStack.getCurrent(),
					getCreationContext().getDomainModel().getTypeConfiguration()
			);

			jdbcParamsBySqmParam.put( expression, jdbcParameterList );
			jdbcParameters.addParameters( jdbcParameterList );
		}

		if ( jdbcParameterList.size() > 1 ) {
			return new SqlTuple( jdbcParameterList );
		}
		else {
			return jdbcParameterList.get( 0 );
		}
	}

	@Override
	public Object visitJpaParameterWrapper(JpaParameterSqmWrapper expression) {
		final List<JdbcParameter> jdbcParameterList;

		List<JdbcParameter> existing = this.jdbcParamsBySqmParam.get( expression );
		if ( existing != null ) {
			final int number = expression.getExpressableType().getNumberOfJdbcParametersNeeded();
			assert existing.size() == number;
			jdbcParameterList = existing;
		}
		else {
			jdbcParameterList = new ArrayList<>();

			//noinspection Convert2Lambda
			expression.getExpressableType().visitJdbcTypes(
					new Consumer<SqlExpressableType>() {
						@Override
						public void accept(SqlExpressableType type) {
							jdbcParameterList.add(
									new StandardJdbcParameterImpl(
											jdbcParameters.getJdbcParameters().size(),
											type,
											currentClauseStack.getCurrent(),
											getCreationContext().getDomainModel().getTypeConfiguration()
									)
							);
						}
					},
					currentClauseStack.getCurrent(),
					getCreationContext().getDomainModel().getTypeConfiguration()
			);

			jdbcParamsBySqmParam.put( expression, jdbcParameterList );
			jdbcParameters.addParameters( jdbcParameterList );
		}

		if ( jdbcParameterList.size() > 1 ) {
			return new SqlTuple( jdbcParameterList );
		}
		else {
			return jdbcParameterList.get( 0 );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// non-standard functions

	@Override
	public Object visitGenericFunction(SqmGenericFunction expression) {
		shallownessStack.push( Shallowness.FUNCTION );
		try {
			return new NonStandardFunction(
					expression.getFunctionName(),
					( (BasicValuedExpressableType) expression.getExpressableType() ).getSqlExpressableType(),
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
					expression.getExpressableType().getSqlExpressableType()
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
					( (BasicValuedExpressableType) function.getExpressableType() ).getSqlExpressableType()
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
					( (BasicValuedExpressableType) expression.getExpressableType() ).getSqlExpressableType(),
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
					toExpression( expression.getArgument().accept( this ) ),
					expression.isDistinct(),
					getCreationContext().getDomainModel().getTypeConfiguration()
							.getBasicTypeRegistry()
							.getBasicType( Long.class )
							.getSqlExpressableType( getCreationContext().getDomainModel().getTypeConfiguration() )
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
				getCreationContext().getDomainModel().getTypeConfiguration()
						.getBasicTypeRegistry()
						.getBasicType( String.class )
						.getSqlExpressableType( getCreationContext().getDomainModel().getTypeConfiguration() )
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

		sqmExpressions.forEach( sqmExpression -> {

			final Object expression = sqmExpression.accept( this );
			if ( BasicValuedNavigableReference.class.isInstance( expression ) ) {
				final BasicValuedNavigableReference navigableReference = (BasicValuedNavigableReference) expression;
				results.add(
						getSqlExpressionResolver().resolveSqlExpression(
								navigableReference.getColumnReferenceQualifier(),
								navigableReference.getNavigable().getBoundColumn()
						)
				);
			}
			else {
				results.add( (Expression) expression );
			}
		} );
		return results;
	}

	@Override
	public CurrentDateFunction visitCurrentDateFunction(SqmCurrentDateFunction function) {
		return new CurrentDateFunction(
				( (BasicValuedExpressableType) function.getExpressableType() ).getSqlExpressableType()
		);
	}

	@Override
	public CurrentTimeFunction visitCurrentTimeFunction(SqmCurrentTimeFunction function) {
		return new CurrentTimeFunction(
				( (BasicValuedExpressableType) function.getExpressableType() ).getSqlExpressableType()
		);
	}

	@Override
	public CurrentTimestampFunction visitCurrentTimestampFunction(SqmCurrentTimestampFunction function) {
		return new CurrentTimestampFunction(
				( (BasicValuedExpressableType) function.getExpressableType() ).getSqlExpressableType()
		);
	}

	@Override
	public ExtractFunction visitExtractFunction(SqmExtractFunction function) {
		shallownessStack.push( Shallowness.FUNCTION );

		try {
			return new ExtractFunction(
					(Expression) function.getUnitToExtract().accept( this ),
					(Expression) function.getExtractionSource().accept( this ),
					( (BasicValuedExpressableType) function.getExpressableType() ).getSqlExpressableType()
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
					getCreationContext().getDomainModel().getTypeConfiguration()
							.getBasicTypeRegistry()
							.getBasicType( Long.class )
							.getSqlExpressableType( getCreationContext().getDomainModel().getTypeConfiguration() )
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
					toExpression( function.getArgument().accept( this ) ),
					getCreationContext().getDomainModel().getTypeConfiguration()
							.getBasicTypeRegistry()
							.getBasicType( Long.class )
							.getSqlExpressableType( getCreationContext().getDomainModel().getTypeConfiguration() )

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
					toExpression( function.getArgument().accept( this ) ),
					( (BasicValuedExpressableType) function.getExpressableType() ).getSqlExpressableType()
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
					toExpression( expression.getArgument().accept( this ) ),
					expression.isDistinct(),
					expression.getExpressableType().getSqlExpressableType()
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
					toExpression( expression.getArgument().accept( this ) ),
					expression.isDistinct(),
					expression.getExpressableType().getSqlExpressableType()
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
					( (BasicValuedExpressableType) function.getExpressableType() ).getSqlExpressableType()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public Object visitSubstringFunction(SqmSubstringFunction expression) {
		shallownessStack.push( Shallowness.FUNCTION );

		try {
			List<Expression> expressionList = new ArrayList<>();
			expressionList.add( toExpression( expression.getSource().accept( this ) ) );
			expressionList.add( toExpression( expression.getStartPosition().accept( this ) ) );
			expressionList.add( toExpression( expression.getLength().accept( this ) ) );

			return new SubstrFunction(
					expression.getFunctionName(),
					expressionList,
					( (BasicValuedExpressableType) expression.getExpressableType() ).getSqlExpressableType()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public Object visitStrFunction(SqmStrFunction expression) {
		shallownessStack.push( Shallowness.FUNCTION );

		try {
			return new CastFunction(
					toExpression( expression.getArgument().accept( this ) ),
					( (BasicValuedExpressableType) expression.getExpressableType() ).getSqlExpressableType(),
					null
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
					toExpression( expression.getArgument().accept( this ) ),
					expression.isDistinct(),
					expression.getExpressableType().getSqlExpressableType()

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
					expression.getExpressableType().getSqlExpressableType()

			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	private UnaryArithmeticOperator interpret(UnaryArithmeticOperator operator) {
		return operator;
	}

	@Override
	public Expression visitBinaryArithmeticExpression(SqmBinaryArithmetic expression) {
		shallownessStack.push( Shallowness.NONE );

		try {
			if ( expression.getOperator() == MODULO ) {
				return new NonStandardFunction(
						"mod",
						null, //(BasicType) extractOrmType( expression.getExpressableType() ),
						(Expression) expression.getLeftHandOperand().accept( this ),
						(Expression) expression.getRightHandOperand().accept( this )
				);
			}
			return new BinaryArithmeticExpression(
					(Expression) expression.getLeftHandOperand().accept( this ), interpret( expression.getOperator() ),
					(Expression) expression.getRightHandOperand().accept( this ),
					expression.getExpressableType().getSqlExpressableType()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	private BinaryArithmeticOperator interpret(BinaryArithmeticOperator operator) {
		switch ( operator ) {
			case ADD: {
				return ADD;
			}
			case SUBTRACT: {
				return SUBTRACT;
			}
			case MULTIPLY: {
				return MULTIPLY;
			}
			case DIVIDE: {
				return DIVIDE;
			}
			case QUOT: {
				return QUOT;
			}
		}

		throw new IllegalStateException( "Unexpected BinaryArithmeticOperator : " + operator );
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
				( (BasicValuedExpressableType) expression.getExpressableType() ).getSqlExpressableType(),
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
		final CaseSearchedExpression result = new CaseSearchedExpression(
				( (BasicValuedExpressableType) expression.getExpressableType() ).getSqlExpressableType()
		);

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
				( (BasicValuedExpressableType) expression.getExpressableType() ).getSqlExpressableType()
		);
	}

	@Override
	public Object visitTrimFunction(SqmTrimFunction expression) {
		return new TrimFunction(
				expression.getSpecification(),
				(Expression) expression.getTrimCharacter().accept( this ),
				(Expression) expression.getSource().accept( this ),
				getCreationContext()
		);
	}

	@Override
	public Object visitUpperFunction(SqmUpperFunction sqmFunction) {
		return new UpperFunction(
				toExpression( sqmFunction.getArgument().accept( this ) ),
				( (BasicValuedExpressableType) sqmFunction.getExpressableType() ).getSqlExpressableType()
		);

	}

	@Override
	public ConcatFunction visitConcatExpression(SqmConcat expression) {
		return new ConcatFunction(
				Arrays.asList(
						(Expression)expression.getLeftHandOperand().accept( this ),
						(Expression) expression.getRightHandOperand().accept( this )
				),
				expression.getExpressableType().getSqlExpressableType()
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
	public ComparisonPredicate visitComparisonPredicate(SqmComparisonPredicate predicate) {
		final Expression lhs = toExpression( predicate.getLeftHandExpression().accept( this ) );
		final Expression rhs = toExpression( predicate.getRightHandExpression().accept( this ) );

		return new ComparisonPredicate(
				lhs, interpret( predicate.getOperator() ),
				rhs
		);
	}

	@SuppressWarnings("unchecked")
	private Expression toExpression(Object value) {
		if ( value instanceof NavigableReference ) {
			final NavigableReference navigableReference = (NavigableReference) value;
			final TableGroup tableGroup;

			if ( navigableReference.getNavigable() instanceof BasicValuedNavigable ) {
				// maybe we should register the LHS TableGroup for the basic value
				// under its NavigablePath, similar to what we do for embeddables
				tableGroup = fromClauseIndex.resolveTableGroup( navigableReference.getNavigablePath().getParent() );
			}
			else {
				// for embeddable-, entity- and plural-valued Navigables we maybe do not have a TableGroup
				final TableGroup thisTableGroup = fromClauseIndex.resolveTableGroup( navigableReference.getNavigablePath() );
				if ( thisTableGroup != null ) {
					tableGroup = thisTableGroup;
				}
				else {
					final NavigablePath lhsNavigablePath = navigableReference.getNavigablePath().getParent();
					if ( lhsNavigablePath == null ) {
						throw new ParsingException( "Could not find TableGroup to use - " + navigableReference.getNavigablePath().getFullPath() );
					}
					tableGroup = fromClauseIndex.resolveTableGroup( lhsNavigablePath );
				}
			}

			final List list = navigableReference.getNavigable().resolveColumnReferences( tableGroup, this );
			if ( list.size() == 1 ) {
				assert list.get( 0 ) instanceof Expression;
				return (Expression) list.get( 0 );
			}
			return new SqlTuple( list );
		}
		else if ( value instanceof SqmSubQuery ) {
			final SqmSubQuery sqmSubQuery = (SqmSubQuery) value;
			final QuerySpec subQuerySpec = visitQuerySpec( sqmSubQuery.getQuerySpec() );

			SqlExpressableType subQueryType = null;
			if ( sqmSubQuery.getExpressableType() instanceof BasicValuedExpressableType ) {
				subQueryType = ( (BasicValuedExpressableType) sqmSubQuery.getExpressableType() ).getSqlExpressableType();
			}

			return new SubQuery( subQuerySpec, subQueryType );
		}

		// any other special cases?

		return (Expression) value;
	}

	private ComparisonOperator interpret(ComparisonOperator operator) {
		switch ( operator ) {
			case EQUAL: {
				return ComparisonOperator.EQUAL;
			}
			case NOT_EQUAL: {
				return ComparisonOperator.NOT_EQUAL;
			}
			case GREATER_THAN_OR_EQUAL: {
				return ComparisonOperator.GREATER_THAN_OR_EQUAL;
			}
			case GREATER_THAN: {
				return ComparisonOperator.GREATER_THAN;
			}
			case LESS_THAN_OR_EQUAL: {
				return ComparisonOperator.LESS_THAN_OR_EQUAL;
			}
			case LESS_THAN: {
				return ComparisonOperator.LESS_THAN;
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
				toExpression( predicate.getExpression().accept( this ) ),
				predicate.isNegated()
		);
	}

	@Override
	public InListPredicate visitInListPredicate(InListSqmPredicate predicate) {
		final InListPredicate inPredicate = new InListPredicate(
				toExpression( predicate.getTestExpression().accept( this ) ),
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
