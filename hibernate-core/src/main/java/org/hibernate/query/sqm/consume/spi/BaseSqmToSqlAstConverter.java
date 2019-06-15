/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.AssertionFailure;
import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.dialect.function.TimestampaddFunction;
import org.hibernate.dialect.function.TimestampdiffFunction;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.metamodel.model.domain.spi.EmbeddedValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.query.BinaryArithmeticOperator;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.UnaryArithmeticOperator;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.spi.ComparisonOperator;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.function.SqmByUnit;
import org.hibernate.query.sqm.tree.expression.SqmCaseSearched;
import org.hibernate.query.sqm.tree.expression.SqmCaseSimple;
import org.hibernate.query.sqm.tree.expression.SqmCriteriaParameter;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmFormat;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;
import org.hibernate.query.sqm.tree.expression.SqmTuple;
import org.hibernate.query.sqm.tree.expression.SqmUnaryOperation;
import org.hibernate.query.sqm.tree.expression.function.SqmCastTarget;
import org.hibernate.query.sqm.tree.expression.function.SqmDistinct;
import org.hibernate.query.sqm.tree.expression.function.SqmDurationUnit;
import org.hibernate.query.sqm.tree.expression.function.SqmExtractUnit;
import org.hibernate.query.sqm.tree.expression.function.SqmStar;
import org.hibernate.query.sqm.tree.expression.function.SqmToDuration;
import org.hibernate.query.sqm.tree.expression.function.SqmTrimSpecification;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.predicate.SqmLikePredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNegatedPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNullnessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmAndPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmBetweenPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmComparisonPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmGroupedPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInListPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInSubQueryPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmOrPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.internal.SqlAstQuerySpecProcessingStateImpl;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.produce.spi.FromClauseAccess;
import org.hibernate.sql.ast.produce.spi.FromClauseIndex;
import org.hibernate.sql.ast.produce.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.produce.spi.SqmFunction;
import org.hibernate.sql.ast.produce.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.produce.spi.SqlAstQuerySpecProcessingState;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.produce.spi.TableGroupJoinProducer;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.ast.produce.sqm.spi.JdbcParameterBySqmParameterAccess;
import org.hibernate.sql.ast.produce.sqm.spi.SqmExpressionInterpretation;
import org.hibernate.sql.ast.produce.sqm.spi.SqmSelectToSqlAstConverter;
import org.hibernate.sql.ast.produce.sqm.spi.SqmToSqlAstConverter;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.Conversion;
import org.hibernate.sql.ast.tree.expression.Distinct;
import org.hibernate.sql.ast.tree.expression.Duration;
import org.hibernate.sql.ast.tree.expression.DurationUnit;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.ExtractUnit;
import org.hibernate.sql.ast.tree.expression.Format;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Star;
import org.hibernate.sql.ast.tree.expression.SubQuery;
import org.hibernate.sql.ast.tree.expression.TrimSpecification;
import org.hibernate.sql.ast.tree.expression.UnaryOperation;
import org.hibernate.sql.ast.tree.expression.domain.BasicValuedNavigableReference;
import org.hibernate.sql.ast.tree.expression.domain.EmbeddableValuedNavigableReference;
import org.hibernate.sql.ast.tree.expression.domain.EntityValuedNavigableReference;
import org.hibernate.sql.ast.tree.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.predicate.BetweenPredicate;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.GroupedPredicate;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.LikePredicate;
import org.hibernate.sql.ast.tree.predicate.NegatedPredicate;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.sort.SortSpecification;
import org.hibernate.sql.exec.internal.JdbcParametersImpl;
import org.hibernate.sql.exec.internal.StandardJdbcParameterImpl;
import org.hibernate.sql.exec.spi.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameters;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.type.spi.StandardSpiBasicTypes;

import org.hibernate.type.spi.TypeConfiguration;
import org.jboss.logging.Logger;

import static org.hibernate.query.BinaryArithmeticOperator.MULTIPLY;
import static org.hibernate.query.BinaryArithmeticOperator.SUBTRACT;
import static org.hibernate.query.TemporalUnit.DAY;
import static org.hibernate.query.TemporalUnit.NANOSECOND;
import static org.hibernate.query.TemporalUnit.NATIVE;
import static org.hibernate.query.UnaryArithmeticOperator.UNARY_MINUS;
import static org.hibernate.type.spi.TypeConfiguration.isDuration;

/**
 * @author Steve Ebersole
 */
public abstract class BaseSqmToSqlAstConverter
		extends BaseSemanticQueryWalker<Object>
		implements SqmToSqlAstConverter<Object>, JdbcParameterBySqmParameterAccess {

	private static final Logger log = Logger.getLogger( BaseSqmToSqlAstConverter.class );

	protected enum Shallowness {
		NONE,
		CTOR,
		FUNCTION,
		SUBQUERY
	}

	private final SqlAstCreationContext creationContext;
	private final QueryOptions queryOptions;
	private final DomainParameterXref domainParameterXref;
	private final QueryParameterBindings domainParameterBindings;
	private final LoadQueryInfluencers loadQueryInfluencers;
	private final Callback callback;

	private final SqlAliasBaseManager sqlAliasBaseManager = new SqlAliasBaseManager();

	private final FromClauseIndex fromClauseIndex = new FromClauseIndex();

	private final Stack<SqlAstProcessingState> processingStateStack = new StandardStack<>();

	private final Stack<Clause> currentClauseStack = new StandardStack<>();
	private final Stack<SqmSelectToSqlAstConverter.Shallowness> shallownessStack = new StandardStack<>( SqmSelectToSqlAstConverter.Shallowness.NONE );

	private SqmByUnit appliedByUnit;
	private Expression adjustedTimestamp;
	private Expression adjustmentScale;
	private boolean negativeAdjustment;

	public BaseSqmToSqlAstConverter(
			SqlAstCreationContext creationContext,
			QueryOptions queryOptions,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings,
			LoadQueryInfluencers loadQueryInfluencers,
			Callback callback) {
		super( creationContext.getServiceRegistry() );
		this.creationContext = creationContext;
		this.queryOptions = queryOptions;
		this.domainParameterXref = domainParameterXref;
		this.domainParameterBindings = domainParameterBindings;
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

	private QuerySpec currentQuerySpec() {
		return ( (SqlAstQuerySpecProcessingState) processingStateStack.getCurrent() ).getInflightQuerySpec();
	}

	public QueryOptions getQueryOptions() {
		return queryOptions;
	}

	protected FromClauseIndex getFromClauseIndex() {
		return fromClauseIndex;
	}

	protected Stack<Clause> getCurrentClauseStack() {
		return currentClauseStack;
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
			final SqmFromClause fromClause = sqmQuerySpec.getFromClause();
			if ( fromClause != null) {
				visitFromClause( fromClause );
			}

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
				toSqlExpression( sortSpecification.getSortExpression().accept( this ) ),
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
						final NavigableReference rootReference = visitRootPath( sqmRoot );
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
	public NavigableReference visitRootPath(SqmRoot<?> sqmRoot) {
		log.tracef( "Starting resolution of SqmRoot [%s] to TableGroup", sqmRoot );

		if ( fromClauseIndex.isResolved( sqmRoot ) ) {
			final TableGroup resolvedTableGroup = fromClauseIndex.findTableGroup( sqmRoot.getNavigablePath() );
			log.tracef( "SqmRoot [%s] resolved to existing TableGroup [%s]", sqmRoot, resolvedTableGroup );
			return resolvedTableGroup;
		}

		final EntityTypeDescriptor entityDescriptor = sqmRoot.getReferencedNavigable().getEntityDescriptor();
		final TableGroup group = entityDescriptor.createRootTableGroup(
				sqmRoot.getNavigablePath(),
				sqmRoot.getExplicitAlias(),
				JoinType.INNER,
				LockMode.NONE,
				this
		);

		fromClauseIndex.register( sqmRoot, group );

		log.tracef( "Resolved SqmRoot [%s] to new TableGroup [%s]", sqmRoot, group );

		sqmRoot.visitSqmJoins(
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
	public TableGroupJoin visitQualifiedAttributeJoin(SqmAttributeJoin<?, ?> sqmJoin) {
		final TableGroup lhsTableGroup = fromClauseIndex.findTableGroup( sqmJoin.getLhs().getNavigablePath() );

		final NavigableContainer<?> joinedNavigable = sqmJoin.sqmAs( NavigableContainer.class );
		if ( joinedNavigable instanceof EmbeddedValuedNavigable ) {
			// register the LHS TableGroup as the embedded's TableGroup
			fromClauseIndex.registerTableGroup( sqmJoin.getNavigablePath(), lhsTableGroup );

			// we also still want to process its joins, adding them to the LHS TableGroup
			sqmJoin.visitSqmJoins(
					sqmJoinJoin -> {
						final TableGroupJoin tableGroupJoin = (TableGroupJoin) sqmJoinJoin.accept( this );
						if ( tableGroupJoin != null ) {
							lhsTableGroup.addTableGroupJoin( tableGroupJoin );
						}
					}
			);

			return null;
		}

		final TableGroupJoin tableJoinJoin = fromClauseIndex.findTableGroupJoin( sqmJoin.getNavigablePath() );
		if ( tableJoinJoin != null ) {
			return tableJoinJoin;
		}

		final TableGroupJoinProducer joinProducer = joinedNavigable.as( TableGroupJoinProducer.class );

		final TableGroupJoin tableGroupJoin = joinProducer.createTableGroupJoin(
				sqmJoin.getNavigablePath(),
				fromClauseIndex.getTableGroup( sqmJoin.getLhs().getNavigablePath() ),
				sqmJoin.getExplicitAlias(),
				sqmJoin.getSqmJoinType().getCorrespondingSqlJoinType(),
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
	public TableGroup visitCrossJoin(SqmCrossJoin<?> sqmJoin) {
		final EntityTypeDescriptor entityMetadata = sqmJoin.getReferencedNavigable().getEntityDescriptor();
		final TableGroup group = entityMetadata.createRootTableGroup(
				sqmJoin.getNavigablePath(),
				sqmJoin.getExplicitAlias(),
				JoinType.INNER,
				LockMode.NONE,
				this
		);

		fromClauseIndex.register( sqmJoin, group );

		sqmJoin.visitSqmJoins(
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
	public Object visitQualifiedEntityJoin(SqmEntityJoin<?> joinedFromElement) {
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
	public Object visitBasicValuedPath(SqmBasicValuedSimplePath path) {

		BasicValuedNavigableReference navigableReference = new BasicValuedNavigableReference(
				path.getNavigablePath(),
				path.getReferencedNavigable(),
				this
		);

		if ( isDuration( path.getExpressableType() ) ) {

			// Durations are stored (at least by default)
			// in a BIGINTEGER column full of nanoseconds
			// which we need to convert to the given unit
			//
			// This does not work at all for a Duration
			// mapped to a VARCHAR column, in which case
			// we would need to parse the weird format
			// defined by java.time.Duration (a bit hard
			// to do without some custom function).
			// Nor does it work for databases which have
			// a well-defined INTERVAL type, but that is
			// something we could implement.

			//first let's apply the propagated scale
			Expression scaledExpression = applyScale( toSqlExpression( navigableReference ) );

			// we use NANOSECOND, not NATIVE, as the unit
			// because that's how a Duration is persisted
			// in a database table column, and how it's
			// returned to a Java client

			if ( adjustedTimestamp != null ) {
				if ( appliedByUnit != null ) {
					throw new IllegalStateException();
				}
				// we're adding this variable duration to the
				// given date or timestamp, producing an
				// adjusted date or timestamp
				return timestampadd().expression(
						new DurationUnit( NANOSECOND, basicType( Long.class ) ),
						scaledExpression,
						adjustedTimestamp
				);
			}
			else if ( appliedByUnit != null ) {
				// we're applying the 'by unit' operator,
				// producing a literal scalar value, so
				// we must convert this duration from
				// nanoseconds to the given unit

				SqlExpressableType durationType = scaledExpression.getType();
				Duration duration = new Duration( scaledExpression, NANOSECOND, durationType);

				TemporalUnit appliedUnit = appliedByUnit.getUnit().getUnit();
				TypeConfiguration typeConfiguration =
						getCreationContext().getDomainModel().getTypeConfiguration();
				SqlExpressableType scalarType =
						appliedByUnit.getExpressableType()
								.getSqlExpressableType( typeConfiguration );
				return new Conversion( duration, appliedUnit, scalarType );
			}
			else {
				// a "bare" Duration value in nanoseconds
				return scaledExpression;
			}
		}

		return navigableReference;
	}

	@Override
	public EmbeddableValuedNavigableReference visitEmbeddableValuedPath(SqmEmbeddedValuedSimplePath path) {
		return new EmbeddableValuedNavigableReference(
				path.getNavigablePath(),
				path.getReferencedNavigable(),
				determineLockMode( path.getExplicitAlias() ),
				this
		);
	}

	@Override
	public Object visitEntityValuedPath(SqmEntityValuedSimplePath path) {
		return new EntityValuedNavigableReference(
				path.getNavigablePath(),
				path.getReferencedNavigable(),
				determineLockMode( path.getExplicitAlias() ),
				this
		);
	}

	@Override
	public Object visitPluralValuedPath(SqmPluralValuedSimplePath path) {
		throw new NotYetImplementedFor6Exception();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Expressions

	@Override
	public Object visitLiteral(SqmLiteral literal) {
		return new QueryLiteral(
				literal.getLiteralValue(),
				literal.getExpressableType().getSqlExpressableType( creationContext.getDomainModel().getTypeConfiguration() ),
				getCurrentClauseStack().getCurrent()
		);
	}

	@Override
	public Object visitTuple(SqmTuple sqmTuple) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public GraphImplementor getCurrentResultGraphNode() {
		return null;
	}

	private final Map<SqmParameter,List<JdbcParameter>> jdbcParamsBySqmParam = new IdentityHashMap<>();
	private final JdbcParameters jdbcParameters = new JdbcParametersImpl();

	@Override
	public Map<SqmParameter, List<JdbcParameter>> getJdbcParamsBySqmParam() {
		return jdbcParamsBySqmParam;
	}

	@Override
	public Expression visitNamedParameterExpression(SqmNamedParameter expression) {
		return consumeSqmParameter( expression );
	}

	private Expression consumeSqmParameter(SqmParameter sqmParameter) {
		final List<JdbcParameter> jdbcParametersForSqm = new ArrayList<>();

		if ( jdbcParamsBySqmParam.containsKey( sqmParameter ) ) {
			final SqmParameter copy = sqmParameter.copy();
			//Note: this is not necessarily only for Criteria
			//      queries ... can also happen when we have
			//      fancy function emulation
			domainParameterXref.addCriteriaAdjustment(
					domainParameterXref.getQueryParameter( sqmParameter ),
					sqmParameter,
					copy
			);

			sqmParameter = copy;
		}

		resolveSqmParameter( sqmParameter, jdbcParametersForSqm::add );

		jdbcParameters.addParameters( jdbcParametersForSqm );
		jdbcParamsBySqmParam.put( sqmParameter, jdbcParametersForSqm );

		if ( jdbcParametersForSqm.size() > 1 ) {
			return new SqlTuple( jdbcParametersForSqm, sqmParameter.getExpressableType() );
		}
		else {
			return jdbcParametersForSqm.get( 0 );
		}
	}

	private void resolveSqmParameter(SqmParameter expression, Consumer<JdbcParameter> jdbcParameterConsumer) {
		AllowableParameterType expressableType = expression.getExpressableType();

		if ( expressableType == null ) {
			final QueryParameterImplementor<?> queryParameter = domainParameterXref.getQueryParameter( expression );
			final QueryParameterBinding binding = domainParameterBindings.getBinding( queryParameter );
			expressableType = QueryHelper.determineParameterType( binding, queryParameter, creationContext.getDomainModel().getTypeConfiguration() );

			if ( expressableType == null ) {
				log.debugf( "Could not determine ExpressableType for parameter [%s], falling back to Object-handling", expression );
				expressableType = StandardSpiBasicTypes.OBJECT_TYPE;
			}

			expression.applyInferableType( expressableType );
		}

		expressableType.visitJdbcTypes(
				type -> {
					final StandardJdbcParameterImpl jdbcParameter = new StandardJdbcParameterImpl(
							jdbcParameters.getJdbcParameters().size(),
							type,
							currentClauseStack.getCurrent(),
							getCreationContext().getDomainModel().getTypeConfiguration()
					);
					jdbcParameterConsumer.accept( jdbcParameter );
				},
				currentClauseStack.getCurrent(),
				getCreationContext().getDomainModel().getTypeConfiguration()
		);
	}

	@Override
	public Object visitPositionalParameterExpression(SqmPositionalParameter expression) {
		return consumeSqmParameter( expression );
	}

	@Override
	public Object visitCriteriaParameter(SqmCriteriaParameter expression) {
		return consumeSqmParameter( expression );
	}

	Expression applyScale(Expression magnitude) {
		boolean negate = negativeAdjustment;

		if ( magnitude instanceof UnaryOperation ) {
			UnaryOperation unary = (UnaryOperation) magnitude;
			if ( unary.getOperator() == UNARY_MINUS ) {
				// if it's already negated, don't
				// wrap it in another unary minus,
				// just throw away the one we have
				// (OTOH, if it *is* negated, shift
				// the operator to left of scale)
				negate = !negate;
			}
			magnitude = unary.getOperand();
		}

		if ( adjustmentScale != null ) {
			if ( isOne( adjustmentScale ) ) {
				//no work to do
			}
			else {
				if ( isOne( magnitude ) ) {
					magnitude = adjustmentScale;
				}
				else {
					magnitude = new BinaryArithmeticExpression(
							adjustmentScale,
							MULTIPLY,
							magnitude,
							magnitude.getType()
					);
				}
			}
		}

		if ( negate ) {
			magnitude = new UnaryOperation(
					UNARY_MINUS,
					magnitude,
					magnitude.getType()
			);
		}

		return magnitude;
	}

	static boolean isOne(Expression scale) {
		return scale instanceof QueryLiteral
			&& ((QueryLiteral) scale).getValue().toString().equals("1");
	}

	@Override
	public Object visitToDuration(SqmToDuration toDuration) {
		//TODO: do we need to temporarily set appliedByUnit
		//      to null before we recurse down the tree?
		//      and what about scale?
		Expression magnitude = toSqlExpression( toDuration.getMagnitude().accept(this) );
		DurationUnit unit = (DurationUnit) toDuration.getUnit().accept(this);

		// let's start by applying the propagated scale
		// so we don't forget to do it in what follows
		Expression scaledMagnitude = applyScale( magnitude );

		if ( adjustedTimestamp != null ) {
			// we're adding this literal duration to the
			// given date or timestamp, producing an
			// adjusted date or timestamp
			if ( appliedByUnit != null ) {
				throw new IllegalStateException();
			}
			return timestampadd().expression( unit, scaledMagnitude, adjustedTimestamp );
		}
		else {
			SqlExpressableType durationType = toDuration.getExpressableType().getSqlExpressableType();
			Duration duration = new Duration( scaledMagnitude, unit.getUnit(), durationType );

			if ( appliedByUnit != null ) {
				// we're applying the 'by unit' operator,
				// producing a literal scalar value in
				// the given unit
				TemporalUnit appliedUnit = appliedByUnit.getUnit().getUnit();
				TypeConfiguration typeConfiguration =
						getCreationContext().getDomainModel().getTypeConfiguration();
				SqlExpressableType scalarType =
						appliedByUnit.getExpressableType()
								.getSqlExpressableType( typeConfiguration );
				return new Conversion( duration, appliedUnit, scalarType );
			}
			else {
				// a "bare" Duration value (gets rendered as nanoseconds)
				return duration;
			}
		}
	}

	@Override
	public Object visitByUnit(SqmByUnit byUnit) {
		SqmByUnit outer = appliedByUnit;
		appliedByUnit = byUnit;
		try {
			return byUnit.getDuration().accept( this );
		}
		finally {
			appliedByUnit = outer;
		}
	}

	@Override
	public Object visitFunction(SqmFunction sqmFunction) {
		shallownessStack.push( Shallowness.FUNCTION );
		try {
			return sqmFunction.convertToSqlAst( this );
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public Object visitFormat(SqmFormat sqmFormat) {
		return new Format(
				sqmFormat.getLiteralValue(),
				sqmFormat.getExpressableType().getSqlExpressableType()
		);
	}

	@Override
	public Object visitTrimSpecification(SqmTrimSpecification specification) {
		shallownessStack.push( Shallowness.FUNCTION );
		try {
			return new TrimSpecification(
					specification.getSpecification()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public Object visitCastTarget(SqmCastTarget target) {
		shallownessStack.push( Shallowness.FUNCTION );
		try {
			return new CastTarget(
					target.getType().getSqlExpressableType(),
					target.getLength(),
					target.getPrecision(),
					target.getScale()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public Object visitExtractUnit(SqmExtractUnit unit) {
		shallownessStack.push( Shallowness.FUNCTION );
		try {
			return new ExtractUnit(
					unit.getUnit(),
					unit.getType().getSqlExpressableType()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public Object visitDurationUnit(SqmDurationUnit unit) {
		shallownessStack.push( Shallowness.FUNCTION );
		try {
			return new DurationUnit(
					unit.getUnit(),
					unit.getType().getSqlExpressableType()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public Object visitDistinct(SqmDistinct distinct) {
		return new Distinct( toSqlExpression( distinct.getExpression().accept(this) ) );
	}

	@Override
	public Object visitStar(SqmStar star) {
		return new Star();
	}

	@Override
	public Object visitUnaryOperationExpression(SqmUnaryOperation expression) {
		shallownessStack.push( Shallowness.NONE );

		try {
			return new UnaryOperation(
					interpret( expression.getOperation() ),
					toSqlExpression( expression.getOperand().accept( this ) ),
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

	private <J> SqlExpressableType basicType(Class<J> javaType) {
		return creationContext.getDomainModel().getTypeConfiguration().standardExpressableTypeForJavaType( javaType ).getSqlExpressableType();
	}

	@Override
	public Object visitBinaryArithmeticExpression(SqmBinaryArithmetic expression) {
		shallownessStack.push( Shallowness.NONE );

		try {
			SqmExpression leftOperand = expression.getLeftHandOperand();
			SqmExpression rightOperand = expression.getRightHandOperand();

			boolean durationToRight = isDuration( rightOperand.getExpressableType() );
			TypeConfiguration typeConfiguration = getCreationContext().getDomainModel().getTypeConfiguration();
			boolean temporalTypeToLeft = typeConfiguration.isTemporalType( leftOperand.getExpressableType() );
			boolean temporalTypeToRight = typeConfiguration.isTemporalType( rightOperand.getExpressableType() );
			boolean temporalTypeSomewhereToLeft = adjustedTimestamp != null || temporalTypeToLeft;

			if (temporalTypeToLeft && durationToRight) {
				if (adjustmentScale != null || negativeAdjustment) {
					//we can't distribute a scale over a date/timestamp
					throw new SemanticException("scalar multiplication of temporal value");
				}
			}

			if (durationToRight && temporalTypeSomewhereToLeft) {
				return transformDurationArithmetic( expression );
			}
			else if (temporalTypeToLeft && temporalTypeToRight) {
				return transformDatetimeArithmetic( expression );
			}
			else if (durationToRight && appliedByUnit!=null) {
				return new BinaryArithmeticExpression(
						toSqlExpression( leftOperand.accept(this) ),
						expression.getOperator(),
						toSqlExpression( rightOperand.accept(this) ),
						//after distributing the 'by unit' operator
						//we get a Long value back
						appliedByUnit.getExpressableType()
								.getSqlExpressableType( typeConfiguration )
				);
			}
			else {
				return new BinaryArithmeticExpression(
						toSqlExpression( leftOperand.accept(this) ),
						expression.getOperator(),
						toSqlExpression( rightOperand.accept(this) ),
						expression.getExpressableType().getSqlExpressableType()
				);
			}
		}
		finally {
			shallownessStack.pop();
		}
	}

	private Object transformDurationArithmetic(SqmBinaryArithmetic<?> expression) {
		BinaryArithmeticOperator operator = expression.getOperator();

		// we have a date or timestamp somewhere to
		// the right of us, so we need to restructure
		// the expression tree
		switch ( operator ) {
			case ADD:
			case SUBTRACT:
				// the only legal binary operations involving
				// a duration with a date or timestamp are
				// addition and subtraction with the duration
				// on the right and the date or timestamp on
				// the left, producing a date or timestamp
				//
				// ts + d or ts - d
				//
				// the only legal binary operations involving
				// two durations are addition and subtraction,
				// producing a duration
				//
				// d1 + d2

				// re-express addition of non-leaf duration
				// expressions to a date or timestamp as
				// addition of leaf durations to a date or
				// timestamp
				// ts + x * (d1 + d2) => (ts + x * d1) + x * d2
				// ts - x * (d1 + d2) => (ts - x * d1) - x * d2
				// ts + x * (d1 - d2) => (ts + x * d1) - x * d2
				// ts - x * (d1 - d2) => (ts - x * d1) + x * d2

				Expression timestamp = adjustedTimestamp;
				adjustedTimestamp = toSqlExpression( expression.getLeftHandOperand().accept( this ) );
				if (operator == SUBTRACT) {
					negativeAdjustment = !negativeAdjustment;
				}
				try {
					return expression.getRightHandOperand().accept( this );
				}
				finally {
					if (operator == SUBTRACT) {
						negativeAdjustment = !negativeAdjustment;
					}
					adjustedTimestamp = timestamp;
				}
			case MULTIPLY:
				// finally, we can multiply a duration on the
				// right by a scalar value on the left
				// scalar multiplication produces a duration
				// x * d

				// distribute scalar multiplication over the
				// terms, not forgetting the propagated scale
				// x * (d1 + d2) => x * d1 + x * d2
				// x * (d1 - d2) => x * d1 - x * d2
				// -x * (d1 + d2) => - x * d1 - x * d2
				// -x * (d1 - d2) => - x * d1 + x * d2
				Expression duration = toSqlExpression( expression.getLeftHandOperand().accept(this) );
				Expression scale = adjustmentScale;
				boolean negate = negativeAdjustment;
				adjustmentScale = applyScale( duration );
				negativeAdjustment = false; //was sucked into the scale
				try {
					return expression.getRightHandOperand().accept( this );
				}
				finally {
					adjustmentScale = scale;
					negativeAdjustment = negate;
				}
			default:
				throw new SemanticException("illegal operator for a duration " + operator);
		}
	}

	private Object transformDatetimeArithmetic(SqmBinaryArithmetic expression) {
		BinaryArithmeticOperator operator = expression.getOperator();

		// the only kind of algebra we know how to
		// do on dates/timestamps is subtract them,
		// producing a duration - all other binary
		// operator expressions with two dates or
		// timestamps are ill-formed
		if ( operator != SUBTRACT ) {
			throw new SemanticException("illegal operator for temporal type: " + operator);
		}

		// a difference between two dates or two
		// timestamps is a leaf duration, so we
		// must apply the scale, and the 'by unit'
		// ts1 - ts2

		Expression left = cleanly(() -> toSqlExpression( expression.getLeftHandOperand().accept(this) ));
		Expression right = cleanly(() -> toSqlExpression( expression.getRightHandOperand().accept(this) ));

		TypeConfiguration typeConfiguration = getCreationContext().getDomainModel().getTypeConfiguration();
		boolean leftTimestamp = typeConfiguration.isTimestampType( expression.getLeftHandOperand().getExpressableType() ) ;
		boolean rightTimestamp = typeConfiguration.isTimestampType( expression.getRightHandOperand().getExpressableType() );

		// when we're dealing with Dates, we use
		// DAY as the smallest unit, otherwise we
		// use a platform-specific granularity

		TemporalUnit baseUnit = rightTimestamp || leftTimestamp ? NATIVE : DAY;

		if (adjustedTimestamp != null) {
			if ( appliedByUnit != null ) {
				throw new IllegalStateException();
			}
			// we're using the resulting duration to
			// adjust a date or timestamp on the left

			// baseUnit is the finest resolution for the
			// temporal type, so we must use it for both
			// the diff, and then the subsequent add

			DurationUnit unit = new DurationUnit( baseUnit, basicType(Integer.class) );
			Expression scaledMagnitude = applyScale( timestampdiff().expression( unit, right, left ) );
			return timestampadd().expression( unit, scaledMagnitude, adjustedTimestamp );
		}
		else if (appliedByUnit != null) {
			// we're immediately converting the resulting
			// duration to a scalar in the given unit

			DurationUnit unit = (DurationUnit) appliedByUnit.getUnit().accept(this);
			return applyScale( timestampdiff().expression( unit, right, left ) );
		}
		else {
			// a plain "bare" Duration
			DurationUnit unit = new DurationUnit( baseUnit, basicType(Integer.class) );
			SqlExpressableType durationType = expression.getExpressableType().getSqlExpressableType();
			Expression scaledMagnitude = applyScale( timestampdiff().expression( unit, right, left)  );
			return new Duration( scaledMagnitude, baseUnit, durationType );
		}
	}

	private TimestampaddFunction timestampadd() {
		return (TimestampaddFunction)
				getCreationContext().getQueryEngine().getSqmFunctionRegistry()
						.findFunctionTemplate("timestampadd");
	}

	private TimestampdiffFunction timestampdiff() {
		return (TimestampdiffFunction)
				getCreationContext().getQueryEngine().getSqmFunctionRegistry()
						.findFunctionTemplate("timestampdiff");
	}

	private <T> T cleanly(Supplier<T> supplier) {
		SqmByUnit byUnit = appliedByUnit;
		Expression timestamp = adjustedTimestamp;
		Expression scale = adjustmentScale;
		boolean negate = negativeAdjustment;
		adjustmentScale = null;
		negativeAdjustment = false;
		appliedByUnit = null;
		adjustedTimestamp = null;
		try {
			return supplier.get();
		}
		finally {
			appliedByUnit = byUnit;
			adjustedTimestamp = timestamp;
			adjustmentScale = scale;
			negativeAdjustment = negate;
		}
	}

	@Override
	public Object visitSubQueryExpression(SqmSubQuery sqmSubQuery) {
		final QuerySpec subQuerySpec = visitQuerySpec( sqmSubQuery.getQuerySpec() );

		final ExpressableType<?> expressableType = sqmSubQuery.getExpressableType();

		return new SubQuery(
				subQuerySpec,
				expressableType instanceof BasicValuedExpressableType<?>
						? ( (BasicValuedExpressableType) expressableType ).getSqlExpressableType( getCreationContext().getDomainModel().getTypeConfiguration() )
						: null,
				expressableType
		);
	}

	@Override
	public CaseSimpleExpression visitSimpleCaseExpression(SqmCaseSimple<?,?> expression) {
		final CaseSimpleExpression result = new CaseSimpleExpression(
				expression.getExpressableType().getSqlExpressableType(),
				toSqlExpression( expression.getFixture().accept( this ) )
		);

		for ( SqmCaseSimple.WhenFragment whenFragment : expression.getWhenFragments() ) {
			result.when(
					toSqlExpression( whenFragment.getCheckValue().accept( this ) ),
					toSqlExpression( whenFragment.getResult().accept( this ) )
			);
		}

		result.otherwise( toSqlExpression( expression.getOtherwise().accept( this ) ) );

		return result;
	}

	@Override
	public CaseSearchedExpression visitSearchedCaseExpression(SqmCaseSearched<?> expression) {
		final CaseSearchedExpression result = new CaseSearchedExpression(
				( (BasicValuedExpressableType) expression.getExpressableType() ).getSqlExpressableType()
		);

		for ( SqmCaseSearched.WhenFragment whenFragment : expression.getWhenFragments() ) {
			result.when(
					(Predicate) whenFragment.getPredicate().accept( this ),
					toSqlExpression( whenFragment.getResult().accept( this ) )
			);
		}

		result.otherwise( toSqlExpression( expression.getOtherwise().accept( this ) ) );

		return result;
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
	public GroupedPredicate visitGroupedPredicate(SqmGroupedPredicate predicate) {
		return new GroupedPredicate ( (Predicate ) predicate.getSubPredicate().accept( this ) );
	}

	@Override
	public Junction visitAndPredicate(SqmAndPredicate predicate) {
		final Junction conjunction = new Junction( Junction.Nature.CONJUNCTION );
		conjunction.add( (Predicate) predicate.getLeftHandPredicate().accept( this ) );
		conjunction.add( (Predicate) predicate.getRightHandPredicate().accept( this ) );
		return conjunction;
	}

	@Override
	public Junction visitOrPredicate(SqmOrPredicate predicate) {
		final Junction disjunction = new Junction( Junction.Nature.DISJUNCTION );
		disjunction.add( (Predicate) predicate.getLeftHandPredicate().accept( this ) );
		disjunction.add( (Predicate) predicate.getRightHandPredicate().accept( this ) );
		return disjunction;
	}

	@Override
	public NegatedPredicate visitNegatedPredicate(SqmNegatedPredicate predicate) {
		return new NegatedPredicate(
				(Predicate) predicate.getWrappedPredicate().accept( this )
		);
	}

	@Override
	public ComparisonPredicate visitComparisonPredicate(SqmComparisonPredicate predicate) {
		final Expression lhs = toSqlExpression( predicate.getLeftHandExpression().accept( this ) );
		final Expression rhs = toSqlExpression( predicate.getRightHandExpression().accept( this ) );

		return new ComparisonPredicate(
				lhs,
				interpret( predicate.getSqmOperator() ),
				rhs
		);
	}

	private Expression toSqlExpression(Object value) {
		if ( value instanceof SqmExpressionInterpretation ) {
			return ( (SqmExpressionInterpretation) value ).toSqlExpression( this );
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
	public BetweenPredicate visitBetweenPredicate(SqmBetweenPredicate predicate) {
		final Expression expression = toSqlExpression( predicate.getExpression().accept( this ) );
		final Expression lowerBound = toSqlExpression( predicate.getLowerBound().accept( this ) );
		final Expression upperBound = toSqlExpression( predicate.getUpperBound().accept( this ) );

		return new BetweenPredicate(
				expression,
				lowerBound,
				upperBound,
				predicate.isNegated()
		);
	}

	@Override
	public LikePredicate visitLikePredicate(SqmLikePredicate predicate) {
		final Expression escapeExpression = predicate.getEscapeCharacter() == null
				? null
				: toSqlExpression( predicate.getEscapeCharacter().accept( this ) );

		return new LikePredicate(
				toSqlExpression( predicate.getMatchExpression().accept( this ) ),
				toSqlExpression( predicate.getPattern().accept( this ) ),
				escapeExpression,
				predicate.isNegated()
		);
	}

	@Override
	public NullnessPredicate visitIsNullPredicate(SqmNullnessPredicate predicate) {
		return new NullnessPredicate(
				toSqlExpression( predicate.getExpression().accept( this ) ),
				predicate.isNegated()
		);
	}

	@Override
	public InListPredicate visitInListPredicate(SqmInListPredicate<?> predicate) {
		// special case:
		//		if there is just a single element and it is an SqmParameter
		//		and the corresponding QueryParameter binding is multi-valued...
		//		lets expand the SQL AST for each bind value
		if ( predicate.getListExpressions().size() == 1 ) {
			final SqmExpression<?> sqmExpression = predicate.getListExpressions().get( 0 );
			if ( sqmExpression instanceof SqmParameter ) {
				final SqmParameter sqmParameter = (SqmParameter) sqmExpression;
				final QueryParameterImplementor<?> domainParam = domainParameterXref.getQueryParameter( sqmParameter );
				final QueryParameterBinding domainParamBinding = domainParameterBindings.getBinding( domainParam );

				if ( domainParamBinding.isMultiValued() ) {
					final InListPredicate inListPredicate = new InListPredicate(
							toSqlExpression( predicate.getTestExpression().accept( this ) )
					);

					boolean first = true;
					for ( Object bindValue : domainParamBinding.getBindValues() ) {
						final SqmParameter sqmParamToConsume;
						// for each bind value do the following:
						//		1) create a pseudo-SqmParameter (though re-use the original for the first value)
						if ( first ) {
							sqmParamToConsume = sqmParameter;
							first = false;
						}
						else {
							sqmParamToConsume = sqmParameter.copy();
							domainParameterXref.addExpansion( domainParam, sqmParameter, sqmParamToConsume );
						}

						inListPredicate.addExpression( consumeSqmParameter( sqmParamToConsume ) );
					}

					return inListPredicate;
				}
			}
		}


		final InListPredicate inPredicate = new InListPredicate(
				toSqlExpression( predicate.getTestExpression().accept( this ) ),
				predicate.isNegated()
		);

		for ( SqmExpression expression : predicate.getListExpressions() ) {
			inPredicate.addExpression( toSqlExpression( expression.accept( this ) ) );
		}

		return inPredicate;
	}

	@Override
	public InSubQueryPredicate visitInSubQueryPredicate(SqmInSubQueryPredicate predicate) {
		return new InSubQueryPredicate(
				toSqlExpression( predicate.getTestExpression().accept( this ) ),
				(QuerySpec) predicate.getSubQueryExpression().accept( this ),
				predicate.isNegated()
		);
	}
}
