/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql;

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
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.BinaryArithmeticOperator;
import org.hibernate.query.UnaryArithmeticOperator;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.function.SqmFunction;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.spi.BaseSemanticQueryWalker;
import org.hibernate.query.sqm.spi.JdbcParameterBySqmParameterAccess;
import org.hibernate.query.sqm.sql.internal.SqlAstQuerySpecProcessingStateImpl;
import org.hibernate.query.sqm.sql.internal.SqmExpressionInterpretation;
import org.hibernate.query.sqm.sql.internal.SqmPathInterpretation;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmCaseSearched;
import org.hibernate.query.sqm.tree.expression.SqmCaseSimple;
import org.hibernate.query.sqm.tree.expression.SqmCriteriaParameter;
import org.hibernate.query.sqm.tree.expression.SqmEnumLiteral;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmFieldLiteral;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;
import org.hibernate.query.sqm.tree.expression.SqmUnaryOperation;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.predicate.SqmAndPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmBetweenPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmComparisonPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmGroupedPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInListPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInSubQueryPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmLikePredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNegatedPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNullnessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmOrPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.UnaryOperation;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;
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
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.sql.exec.internal.JdbcParametersImpl;
import org.hibernate.sql.exec.spi.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameters;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;

import org.jboss.logging.Logger;

import static org.hibernate.internal.util.NullnessHelper.coalesce;
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
		implements SqmToSqlAstConverter, JdbcParameterBySqmParameterAccess {

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
	//private final Callback callback;

	private final DomainParameterXref domainParameterXref;
	private final QueryParameterBindings domainParameterBindings;

	private final SqlAliasBaseManager sqlAliasBaseManager = new SqlAliasBaseManager();

	private final FromClauseIndex fromClauseIndex = new FromClauseIndex();

	private final Stack<SqlAstProcessingState> processingStateStack = new StandardStack<>();

	private final Stack<Clause> currentClauseStack = new StandardStack<>();
	private final Stack<Shallowness> shallownessStack = new StandardStack<>( Shallowness.NONE );


	public BaseSqmToSqlAstConverter(
			SqlAstCreationContext creationContext,
			QueryOptions queryOptions,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings,
//			LoadQueryInfluencers loadQueryInfluencers,
//			Callback callback) {
			LoadQueryInfluencers loadQueryInfluencers) {
		super( creationContext.getServiceRegistry() );
		this.creationContext = creationContext;
		this.queryOptions = queryOptions;
		this.domainParameterXref = domainParameterXref;
		this.domainParameterBindings = domainParameterBindings;
		this.loadQueryInfluencers = loadQueryInfluencers;
//		this.callback = callback;
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

	public QueryOptions getQueryOptions() {
		return queryOptions;
	}

	public FromClauseIndex getFromClauseIndex() {
		return fromClauseIndex;
	}

	public Stack<Clause> getCurrentClauseStack() {
		return currentClauseStack;
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
					sqlQuerySpec.applyPredicate( (Predicate) whereClause.getPredicate().accept( this ) );
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
						final TableGroup rootTableGroup = visitRootPath( sqmRoot );

						currentQuerySpec().getFromClause().addRoot( rootTableGroup );
						getFromClauseIndex().register( sqmRoot, rootTableGroup );
					}
			);
		}
		finally {
			currentClauseStack.pop();
		}

		return null;
	}


	@Override
	public TableGroup visitRootPath(SqmRoot<?> sqmRoot) {
		log.tracef( "Starting resolution of SqmRoot [%s] to TableGroup", sqmRoot );

		final TableGroup resolvedTableGroup = fromClauseIndex.findTableGroup( sqmRoot.getNavigablePath() );
		if ( resolvedTableGroup != null ) {
			log.tracef( "SqmRoot [%s] resolved to existing TableGroup [%s]", sqmRoot, resolvedTableGroup );
			return resolvedTableGroup;
		}

		final EntityPersister entityDescriptor = resolveEntityPersister( sqmRoot.getReferencedPathSource() );

		final TableGroup tableGroup = entityDescriptor.createRootTableGroup(
				sqmRoot.getNavigablePath(),
				sqmRoot.getExplicitAlias(),
				JoinType.INNER,
				LockMode.NONE,
				sqlAliasBaseManager,
				creationContext
		);


		fromClauseIndex.register( sqmRoot, tableGroup );

		log.tracef( "Resolved SqmRoot [%s] to new TableGroup [%s]", sqmRoot, tableGroup );

		visitExplicitJoins( sqmRoot, tableGroup );
		visitImplicitJoins( sqmRoot, tableGroup );

		return tableGroup;
	}

	private EntityPersister resolveEntityPersister(EntityDomainType<?> entityDomainType) {
		return creationContext.getDomainModel().getEntityDescriptor( entityDomainType.getHibernateEntityName() );
	}

	private void visitExplicitJoins(SqmFrom<?,?> sqmFrom, TableGroup tableGroup) {
		log.tracef( "Visiting explicit joins for `%s`", sqmFrom.getNavigablePath() );

		sqmFrom.visitSqmJoins(
				sqmJoin -> {
					final TableGroupJoin tableGroupJoin = (TableGroupJoin) sqmJoin.accept( this );
					if ( tableGroupJoin != null ) {
						tableGroup.addTableGroupJoin( tableGroupJoin );
						getFromClauseIndex().register( sqmFrom, tableGroup );
					}
				}
		);
	}

	private void visitImplicitJoins(SqmPath<?> sqmPath, TableGroup tableGroup) {
		log.tracef( "Visiting implicit joins for `%s`", sqmPath.getNavigablePath() );

		sqmPath.visitImplicitJoinPaths(
				joinedPath -> {
					log.tracef( "Starting implicit join handling for `%s`", joinedPath.getNavigablePath() );
				}
		);
	}

	@Override
	public TableGroupJoin visitQualifiedAttributeJoin(SqmAttributeJoin<?, ?> sqmJoin) {
		final TableGroupJoin tableJoinJoin = fromClauseIndex.findTableGroupJoin( sqmJoin.getNavigablePath() );
		if ( tableJoinJoin != null ) {
			return tableJoinJoin;
		}

		final SqmPathSource<?> pathSource = sqmJoin.getReferencedPathSource();

		final TableGroup lhsTableGroup = fromClauseIndex.findTableGroup( sqmJoin.getLhs().getNavigablePath() );

		if ( pathSource.getSqmPathType() instanceof EmbeddableDomainType<?> ) {
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
			sqmJoin.visitSqmJoins(
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

		final TableGroupJoin tableGroupJoin = ( (TableGroupJoinProducer) pathSource ).createTableGroupJoin(
				sqmJoin.getNavigablePath(),
				lhsTableGroup,
				sqmJoin.getExplicitAlias(),
				sqmJoin.getSqmJoinType().getCorrespondingSqlJoinType(),
				LockMode.READ,
				sqlAliasBaseManager,
				creationContext
		);

		fromClauseIndex.register( sqmJoin, tableGroupJoin );
		lhsTableGroup.addTableGroupJoin( tableGroupJoin );

		// add any additional join restrictions
		if ( sqmJoin.getJoinPredicate() != null ) {
			currentQuerySpec().applyPredicate(
					(Predicate) sqmJoin.getJoinPredicate().accept( this )
			);
		}


		return tableGroupJoin;
	}

	private QuerySpec currentQuerySpec() {
		final SqlAstQuerySpecProcessingState processingState = (SqlAstQuerySpecProcessingState) getProcessingStateStack().getCurrent();
		return processingState.getInflightQuerySpec();
	}

	@Override
	public TableGroupJoin visitCrossJoin(SqmCrossJoin<?> sqmJoin) {
		final EntityPersister entityDescriptor = resolveEntityPersister( sqmJoin.getReferencedPathSource() );

		final TableGroup tableGroup = entityDescriptor.createRootTableGroup(
				sqmJoin.getNavigablePath(),
				sqmJoin.getExplicitAlias(),
				JoinType.INNER,
				LockMode.NONE,
				sqlAliasBaseManager,
				getCreationContext()
		);

		fromClauseIndex.register( sqmJoin, tableGroup );

		sqmJoin.visitSqmJoins(
				sqmJoinJoin -> {
					final TableGroupJoin tableGroupJoin = (TableGroupJoin) sqmJoinJoin.accept( this );
					if ( tableGroupJoin != null ) {
						tableGroup.addTableGroupJoin( tableGroupJoin );
					}
				}
		);

		return new TableGroupJoin( sqmJoin.getNavigablePath(), JoinType.CROSS, tableGroup, null );
	}

	@Override
	public Object visitQualifiedEntityJoin(SqmEntityJoin joinedFromElement) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public SqmPathInterpretation visitBasicValuedPath(SqmBasicValuedSimplePath sqmPath) {
		final SqmPath<?> lhs = sqmPath.getLhs();
		assert lhs != null;

		return (SqmPathInterpretation) sqmPath;
	}

	@Override
	public SqmPathInterpretation visitEmbeddableValuedPath(SqmEmbeddedValuedSimplePath sqmPath) {
		final SqmPath<?> lhs = sqmPath.getLhs();
		assert lhs != null;

		return (SqmPathInterpretation) sqmPath;
	}

	@Override
	public SqmPathInterpretation visitEntityValuedPath(SqmEntityValuedSimplePath sqmPath) {
		final SqmPath<?> lhs = sqmPath.getLhs();
		assert lhs != null;

		return (SqmPathInterpretation) sqmPath;
	}

	@Override
	public SqmPathInterpretation visitPluralValuedPath(SqmPluralValuedSimplePath sqmPath) {
		return (SqmPathInterpretation) sqmPath;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Expressions

	@Override
	public SqmExpressionInterpretation visitLiteral(SqmLiteral literal) {
		throw new NotYetImplementedFor6Exception( getClass() );
//		final ExpressableType<?> expressableType = determineExpressableType( literal );
//		if ( expressableType instanceof BasicValuedExpressableType<?> ) {
//			return new QueryLiteral(
//					literal.getLiteralValue(),
//					( (BasicValuedExpressableType<?>) expressableType ).getSqlExpressableType( getTypeConfiguration() ),
//					getCurrentClauseStack().getCurrent()
//			);
//		}
//
//		return new QueryLiteral(
//				literal.getLiteralValue(),
//				literal.getExpressableType().getSqlExpressableType( getTypeConfiguration() ),
//				getCurrentClauseStack().getCurrent()
//		);
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
		final MappingModelExpressable valueMapping = determineValueMapping( sqmParameter );

		resolveSqmParameter( sqmParameter, valueMapping, jdbcParametersForSqm::add );

		jdbcParameters.addParameters( jdbcParametersForSqm );
		jdbcParamsBySqmParam.put( sqmParameter, jdbcParametersForSqm );

		if ( jdbcParametersForSqm.size() > 1 ) {
			return new SqlTuple( jdbcParametersForSqm, valueMapping );
		}
		else {
			return jdbcParametersForSqm.get( 0 );
		}
	}

	protected MappingModelExpressable<?> determineValueMapping(SqmExpression<?> sqmExpression) {
		final SqmExpressable<?> nodeType = sqmExpression.getNodeType();

		MappingModelExpressable valueMapping = getCreationContext().getDomainModel().resolveMappingExpressable( nodeType );
		// alternative
		// sqmExpression.resolveMappingExpressable( getCreationContext(), "inferableTypeAccessStack" );


		if ( valueMapping == null ) {
			final Supplier<MappingModelExpressable> currentExpressableSupplier = inferableTypeAccessStack.getCurrent();
			if ( currentExpressableSupplier != null ) {
				valueMapping = currentExpressableSupplier.get();
			}
		}

		if ( valueMapping == null ) {
			throw new ConversionException( "Could not determine ValueMapping for SqmParameter: " + sqmExpression );
		}

		return valueMapping;
	}


	private final Stack<Supplier<MappingModelExpressable>> inferableTypeAccessStack = new StandardStack<>(
			() -> () -> null
	);

	private void resolveSqmParameter(SqmParameter expression, MappingModelExpressable valueMapping, Consumer<JdbcParameter> jdbcParameterConsumer) {
		throw new NotYetImplementedFor6Exception( getClass() );
//		if ( valueMapping == null ) {
//			final StandardJdbcParameterImpl jdbcParameter = new StandardJdbcParameterImpl(
//					jdbcParameters.getJdbcParameters().size(),
//					null,
//					currentClauseStack.getCurrent(),
//					getCreationContext().getDomainModel().getTypeConfiguration()
//			);
//
//			jdbcParameterConsumer.accept( jdbcParameter );
//		}
//		else {
//			expressableType.visitJdbcTypes(
//					type -> {
//						final StandardJdbcParameterImpl jdbcParameter = new StandardJdbcParameterImpl(
//								jdbcParameters.getJdbcParameters().size(),
//								type,
//								currentClauseStack.getCurrent(),
//								getCreationContext().getDomainModel().getTypeConfiguration()
//						);
//						jdbcParameterConsumer.accept( jdbcParameter );
//					},
//					currentClauseStack.getCurrent(),
//					getCreationContext().getDomainModel().getTypeConfiguration()
//			);
//		}
	}

	@Override
	public Object visitPositionalParameterExpression(SqmPositionalParameter expression) {
		return consumeSqmParameter( expression );
	}

	@Override
	public Object visitCriteriaParameter(SqmCriteriaParameter expression) {
		return consumeSqmParameter( expression );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// non-standard functions


	@Override
	public Object visitFunction(SqmFunction sqmFunction) {
		throw new NotYetImplementedFor6Exception( getClass() );
//		shallownessStack.push( Shallowness.FUNCTION );
//		try {
//			return new NonStandardFunction(
//					sqmFunction.getFunctionName(),
//					determineValueMapping( sqmFunction ),
//					visitArguments( sqmFunction.getArguments() )
//			);
//		}
//		finally {
//			shallownessStack.pop();
//		}
	}

//	private List<Expression> visitArguments(List<SqmExpression> sqmArguments) {
//		if ( sqmArguments == null || sqmArguments.isEmpty() ) {
//			return Collections.emptyList();
//		}
//
//		final ArrayList<Expression> sqlAstArguments = new ArrayList<>();
//		for ( SqmExpression sqmArgument : sqmArguments ) {
//			sqlAstArguments.add( (Expression) sqmArgument.accept( this ) );
//		}
//
//		return sqlAstArguments;
//	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// standard functions


//	@Override
//	public Object visitAbsFunction(SqmAbsFunction function) {
//		shallownessStack.push( Shallowness.FUNCTION );
//
//		try {
//			return new AbsFunction( (Expression) function.getArgument().accept( this ) );
//		}
//		finally {
//			shallownessStack.pop();
//		}
//	}
//
//	@Override
//	public AvgFunction visitAvgFunction(SqmAvgFunction expression) {
//		shallownessStack.push( Shallowness.FUNCTION );
//
//		try {
//			return new AvgFunction(
//					(Expression) expression.getArgument().accept( this ),
//					expression.isDistinct(),
//					expression.getExpressableType().getSqlExpressableType()
//			);
//		}
//		finally {
//			shallownessStack.pop();
//		}
//	}
//
//	@Override
//	public Object visitBitLengthFunction(SqmBitLengthFunction function) {
//		shallownessStack.push( Shallowness.FUNCTION );
//
//		try {
//			return new BitLengthFunction(
//					(Expression) function.getArgument().accept( this ),
//					( (BasicValuedExpressableType) function.getExpressableType() ).getSqlExpressableType()
//			);
//		}
//		finally {
//			shallownessStack.pop();
//		}
//	}
//
//	@Override
//	public Object visitCastFunction(SqmCastFunction expression) {
//		shallownessStack.push( Shallowness.FUNCTION );
//
//		try {
//			return new CastFunction(
//					(Expression) expression.getExpressionToCast().accept( this ),
//					( (BasicValuedExpressableType) expression.getExpressableType() ).getSqlExpressableType(),
//					expression.getExplicitSqlCastTarget()
//			);
//		}
//		finally {
//			shallownessStack.pop();
//		}
//	}
//
//	@Override
//	public CountFunction visitCountFunction(SqmCountFunction expression) {
//		shallownessStack.push( Shallowness.FUNCTION );
//
//		try {
//			return new CountFunction(
//					toSqlExpression( expression.getArgument().accept( this ) ),
//					expression.isDistinct(),
//					getCreationContext().getDomainModel().getTypeConfiguration()
//							.getBasicTypeRegistry()
//							.getBasicType( Long.class )
//							.getSqlExpressableType( getCreationContext().getDomainModel().getTypeConfiguration() )
//			);
//		}
//		finally {
//			shallownessStack.pop();
//		}
//	}
//
//	@Override
//	public ConcatFunction visitConcatFunction(SqmConcatFunction function) {
//		return new ConcatFunction(
//				collectionExpressions( function.getExpressions() ),
//				getCreationContext().getDomainModel().getTypeConfiguration()
//						.getBasicTypeRegistry()
//						.getBasicType( String.class )
//						.getSqlExpressableType( getCreationContext().getDomainModel().getTypeConfiguration() )
//		);
//	}
//
//	private List<Expression> collectionExpressions(List<SqmExpression> sqmExpressions) {
//		if ( sqmExpressions == null || sqmExpressions.isEmpty() ) {
//			return Collections.emptyList();
//		}
//
//		if ( sqmExpressions.size() == 1 ) {
//			return Collections.singletonList( (Expression) sqmExpressions.get( 0 ).accept( this ) );
//		}
//
//		final List<Expression> results = new ArrayList<>();
//
//		sqmExpressions.forEach( sqmExpression -> {
//
//			final Object expression = sqmExpression.accept( this );
//			if ( expression instanceof BasicValuedNavigableReference ) {
//				final BasicValuedNavigableReference navigableReference = (BasicValuedNavigableReference) expression;
//				results.add(
//						getSqlExpressionResolver().resolveSqlExpression(
//								fromClauseIndex.getTableGroup( navigableReference.getNavigablePath().getParent() ),
//								navigableReference.getNavigable().getBoundColumn()
//						)
//				);
//			}
//			else {
//				results.add( (Expression) expression );
//			}
//		} );
//		return results;
//	}
//
//	@Override
//	public CurrentDateFunction visitCurrentDateFunction(SqmCurrentDateFunction function) {
//		return new CurrentDateFunction(
//				( (BasicValuedExpressableType) function.getExpressableType() ).getSqlExpressableType()
//		);
//	}
//
//	@Override
//	public CurrentTimeFunction visitCurrentTimeFunction(SqmCurrentTimeFunction function) {
//		return new CurrentTimeFunction(
//				( (BasicValuedExpressableType) function.getExpressableType() ).getSqlExpressableType()
//		);
//	}
//
//	@Override
//	public CurrentTimestampFunction visitCurrentTimestampFunction(SqmCurrentTimestampFunction function) {
//		return new CurrentTimestampFunction(
//				( (BasicValuedExpressableType) function.getExpressableType() ).getSqlExpressableType()
//		);
//	}
//
//	@Override
//	public ExtractFunction visitExtractFunction(SqmExtractFunction function) {
//		shallownessStack.push( Shallowness.FUNCTION );
//
//		try {
//			return new ExtractFunction(
//					(Expression) function.getUnitToExtract().accept( this ),
//					(Expression) function.getExtractionSource().accept( this ),
//					( (BasicValuedExpressableType) function.getExpressableType() ).getSqlExpressableType()
//			);
//		}
//		finally {
//			shallownessStack.pop();
//		}
//	}
//
//	@Override
//	public CountStarFunction visitCountStarFunction(SqmCountStarFunction expression) {
//		shallownessStack.push( Shallowness.FUNCTION );
//
//		try {
//			return new CountStarFunction(
//					expression.isDistinct(),
//					getCreationContext().getDomainModel().getTypeConfiguration()
//							.getBasicTypeRegistry()
//							.getBasicType( Long.class )
//							.getSqlExpressableType( getCreationContext().getDomainModel().getTypeConfiguration() )
//			);
//		}
//		finally {
//			shallownessStack.pop();
//		}
//	}
//
//	@Override
//	public LengthFunction visitLengthFunction(SqmLengthFunction function) {
//		shallownessStack.push( Shallowness.FUNCTION );
//
//		try {
//			return new LengthFunction(
//					toSqlExpression( function.getArgument().accept( this ) ),
//					getCreationContext().getDomainModel().getTypeConfiguration()
//							.getBasicTypeRegistry()
//							.getBasicType( Long.class )
//							.getSqlExpressableType( getCreationContext().getDomainModel().getTypeConfiguration() )
//
//			);
//		}
//		finally {
//			shallownessStack.pop();
//		}
//	}
//
//	@Override
//	public LocateFunction visitLocateFunction(SqmLocateFunction function) {
//		shallownessStack.push( Shallowness.FUNCTION );
//
//		try {
//			return new LocateFunction(
//					(Expression) function.getPatternString().accept( this ),
//					(Expression) function.getStringToSearch().accept( this ),
//					function.getStartPosition() == null
//							? null
//							: (Expression) function.getStartPosition().accept( this )
//			);
//		}
//		finally {
//			shallownessStack.pop();
//		}
//	}
//
//	@Override
//	public Object visitLowerFunction(SqmLowerFunction function) {
//		shallownessStack.push( Shallowness.FUNCTION );
//
//		try {
//			return new LowerFunction(
//					toSqlExpression( function.getArgument().accept( this ) ),
//					( (BasicValuedExpressableType) function.getExpressableType() ).getSqlExpressableType()
//			);
//		}
//		finally {
//			shallownessStack.pop();
//		}
//	}
//
//	@Override
//	public MaxFunction visitMaxFunction(SqmMaxFunction expression) {
//		shallownessStack.push( Shallowness.FUNCTION );
//
//		try {
//			return new MaxFunction(
//					toSqlExpression( expression.getArgument().accept( this ) ),
//					expression.isDistinct(),
//					expression.getExpressableType().getSqlExpressableType()
//			);
//		}
//		finally {
//			shallownessStack.pop();
//		}
//	}
//
//	@Override
//	public MinFunction visitMinFunction(SqmMinFunction expression) {
//		shallownessStack.push( Shallowness.FUNCTION );
//
//		try {
//			return new MinFunction(
//					toSqlExpression( expression.getArgument().accept( this ) ),
//					expression.isDistinct(),
//					expression.getExpressableType().getSqlExpressableType()
//			);
//		}
//		finally {
//			shallownessStack.pop();
//		}
//	}
//
//	@Override
//	public Object visitModFunction(SqmModFunction function) {
//		shallownessStack.push( Shallowness.FUNCTION );
//
//		final Expression dividend = (Expression) function.getDividend().accept( this );
//		final Expression divisor = (Expression) function.getDivisor().accept( this );
//		try {
//			return new ModFunction(
//					dividend,
//					divisor,
//					( (BasicValuedExpressableType) function.getExpressableType() ).getSqlExpressableType()
//			);
//		}
//		finally {
//			shallownessStack.pop();
//		}
//	}
//
//	@Override
//	public Object visitSubstringFunction(SqmSubstringFunction expression) {
//		shallownessStack.push( Shallowness.FUNCTION );
//
//		try {
//			List<Expression> expressionList = new ArrayList<>();
//			expressionList.add( toSqlExpression( expression.getSource().accept( this ) ) );
//			expressionList.add( toSqlExpression( expression.getStartPosition().accept( this ) ) );
//			expressionList.add( toSqlExpression( expression.getLength().accept( this ) ) );
//
//			return new SubstrFunction(
//					expression.getFunctionName(),
//					expressionList,
//					( (BasicValuedExpressableType) expression.getExpressableType() ).getSqlExpressableType()
//			);
//		}
//		finally {
//			shallownessStack.pop();
//		}
//	}
//
//	@Override
//	public Object visitStrFunction(SqmStrFunction expression) {
//		shallownessStack.push( Shallowness.FUNCTION );
//
//		try {
//			return new CastFunction(
//					toSqlExpression( expression.getArgument().accept( this ) ),
//					( (BasicValuedExpressableType) expression.getExpressableType() ).getSqlExpressableType(),
//					null
//			);
//		}
//		finally {
//			shallownessStack.pop();
//		}
//	}
//
//	@Override
//	public SumFunction visitSumFunction(SqmSumFunction expression) {
//		shallownessStack.push( Shallowness.FUNCTION );
//
//		try {
//			return new SumFunction(
//					toSqlExpression( expression.getArgument().accept( this ) ),
//					expression.isDistinct(),
//					expression.getExpressableType().getSqlExpressableType()
//
//			);
//		}
//		finally {
//			shallownessStack.pop();
//		}
//	}
//
//	@Override
//	public CoalesceFunction visitCoalesceFunction(SqmCoalesceFunction expression) {
//		final CoalesceFunction result = new CoalesceFunction();
//		for ( SqmExpression value : expression.getArguments() ) {
//			result.value( (Expression) value.accept( this ) );
//		}
//
//		return result;
//	}
//	@Override
//	public NullifFunction visitNullifFunction(SqmNullifFunction expression) {
//		return new NullifFunction(
//				(Expression) expression.getFirstArgument().accept( this ),
//				(Expression) expression.getSecondArgument().accept( this ),
//				( (BasicValuedExpressableType) expression.getExpressableType() ).getSqlExpressableType()
//		);
//	}
//
//	@Override
//	public Object visitTrimFunction(SqmTrimFunction expression) {
//		return new TrimFunction(
//				expression.getSpecification(),
//				(Expression) expression.getTrimCharacter().accept( this ),
//				(Expression) expression.getSource().accept( this ),
//				getCreationContext()
//		);
//	}
//
//	@Override
//	public Object visitUpperFunction(SqmUpperFunction sqmFunction) {
//		return new UpperFunction(
//				toSqlExpression( sqmFunction.getArgument().accept( this ) ),
//				( (BasicValuedExpressableType) sqmFunction.getExpressableType() ).getSqlExpressableType()
//		);
//
//	}
//
//	@Override
//	public ConcatFunction visitConcatExpression(SqmConcat expression) {
//		return new ConcatFunction(
//				Arrays.asList(
//						(Expression)expression.getLeftHandOperand().accept( this ),
//						(Expression) expression.getRightHandOperand().accept( this )
//				),
//				expression.getExpressableType().getSqlExpressableType()
//		);
//	}

	@Override
	public Object visitUnaryOperationExpression(SqmUnaryOperation expression) {
		shallownessStack.push( Shallowness.NONE );

		try {
			return new UnaryOperation(
					interpret( expression.getOperation() ),
					(Expression) expression.getOperand().accept( this ),
					determineValueMapping( expression )

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
				throw new NotYetImplementedFor6Exception( getClass() );
//				return new NonStandardFunction(
//						"mod",
//						null, //(BasicType) extractOrmType( expression.getExpressableType() ),
//						(Expression) expression.getLeftHandOperand().accept( this ),
//						(Expression) expression.getRightHandOperand().accept( this )
//				);
			}
			return new BinaryArithmeticExpression(
					(Expression) expression.getLeftHandOperand().accept( this ), interpret( expression.getOperator() ),
					(Expression) expression.getRightHandOperand().accept( this ),
					determineValueMapping( expression )
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
	public Object visitSubQueryExpression(SqmSubQuery sqmSubQuery) {
		throw new NotYetImplementedFor6Exception( getClass() );

//		final QuerySpec subQuerySpec = visitQuerySpec( sqmSubQuery.getQuerySpec() );
//
//		final ExpressableType<?> expressableType = determineExpressableType( sqmSubQuery );
//
//		return new SubQuery(
//				subQuerySpec,
//				expressableType instanceof BasicValuedExpressableType<?>
//						? ( (BasicValuedExpressableType) expressableType ).getSqlExpressableType( getTypeConfiguration() )
//						: null,
//				expressableType
//		);
	}

	@Override
	public CaseSimpleExpression visitSimpleCaseExpression(SqmCaseSimple<?,?> expression) {
		final CaseSimpleExpression result = new CaseSimpleExpression(
				determineValueMapping( expression ),
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
	public CaseSearchedExpression visitSearchedCaseExpression(SqmCaseSearched<?> expression) {
		final CaseSearchedExpression result = new CaseSearchedExpression(
				determineValueMapping( expression )
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
	public Object visitEnumLiteral(SqmEnumLiteral sqmEnumLiteral) {
		return new QueryLiteral(
				sqmEnumLiteral.getEnumValue(),
				determineValueMapping( sqmEnumLiteral ),
				getCurrentClauseStack().getCurrent()
		);
	}

	@Override
	public Object visitFieldLiteral(SqmFieldLiteral sqmFieldLiteral) {
		return new QueryLiteral(
				sqmFieldLiteral.getValue(),
				determineValueMapping( sqmFieldLiteral ),
				getCurrentClauseStack().getCurrent()
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
//
//	@Override
//	public ColumnReference visitExplicitColumnReference(SqmColumnReference sqmColumnReference) {
//		final TableGroup tableGroup = fromClauseIndex.findTableGroup(
//				sqmColumnReference.getSqmFromBase().getNavigablePath()
//		);
//
//		final ColumnReference columnReference = tableGroup.locateColumnReferenceByName( sqmColumnReference.getColumnName() );
//
//		if ( columnReference == null ) {
//			throw new HibernateException( "Could not resolve ColumnReference" );
//		}
//
//		return columnReference;
//	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Predicates


	@Override
	public GroupedPredicate visitGroupedPredicate(SqmGroupedPredicate predicate) {
		return new GroupedPredicate( (Predicate) predicate.getSubPredicate().accept( this ) );
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
		inferableTypeAccessStack.push( () -> determineValueMapping( predicate.getRightHandExpression() ) );

		final Expression lhs;
		try {
			lhs = (Expression) predicate.getLeftHandExpression().accept( this );
		}
		finally {
			inferableTypeAccessStack.pop();
		}

		inferableTypeAccessStack.push( () -> determineValueMapping( predicate.getLeftHandExpression() ) );

		final Expression rhs;
		try {
			rhs = (Expression) predicate.getRightHandExpression().accept( this );
		}
		finally {
			inferableTypeAccessStack.pop();
		}

		return new ComparisonPredicate( lhs, predicate.getSqmOperator(), rhs );
	}

//	@SuppressWarnings("unchecked")
//	private Expression toSqlExpression(Object value) {
//		if ( value instanceof SqmExpressionInterpretation ) {
//			return ( (SqmExpressionInterpretation) value ).toSqlExpression( this );
//		}
//
//		// any other special cases?
//
//		return (Expression) value;
//	}

	@Override
	public BetweenPredicate visitBetweenPredicate(SqmBetweenPredicate predicate) {
		final Expression expression;
		final Expression lowerBound;
		final Expression upperBound;

		inferableTypeAccessStack.push(
				() -> coalesce(
						determineValueMapping( predicate.getLowerBound() ),
						determineValueMapping( predicate.getUpperBound() )
				)
		);

		try {
			expression = (Expression) predicate.getExpression().accept( this );
		}
		finally {
			inferableTypeAccessStack.pop();
		}

		inferableTypeAccessStack.push(
				() -> coalesce(
						determineValueMapping( predicate.getExpression() ),
						determineValueMapping( predicate.getUpperBound() )
				)
		);
		try {
			lowerBound = (Expression) predicate.getLowerBound().accept( this );
		}
		finally {
			inferableTypeAccessStack.pop();
		}

		inferableTypeAccessStack.push(
				() -> coalesce(
						determineValueMapping( predicate.getExpression() ),
						determineValueMapping( predicate.getLowerBound() )
				)
		);
		try {
			upperBound = (Expression) predicate.getUpperBound().accept( this );
		}
		finally {
			inferableTypeAccessStack.pop();
		}

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
				: (Expression) predicate.getEscapeCharacter().accept( this );

		return new LikePredicate(
				(Expression) predicate.getMatchExpression().accept( this ),
				(Expression) predicate.getPattern().accept( this ),
				escapeExpression,
				predicate.isNegated()
		);
	}

	@Override
	public NullnessPredicate visitIsNullPredicate(SqmNullnessPredicate predicate) {
		return new NullnessPredicate(
				(Expression) predicate.getExpression().accept( this ),
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
			final SqmExpression sqmExpression = predicate.getListExpressions().get( 0 );
			if ( sqmExpression instanceof SqmParameter ) {
				final SqmParameter sqmParameter = (SqmParameter) sqmExpression;
				final QueryParameterImplementor<?> domainParam = domainParameterXref.getQueryParameter( sqmParameter );
				final QueryParameterBinding domainParamBinding = domainParameterBindings.getBinding( domainParam );

				if ( domainParamBinding.isMultiValued() ) {
					final InListPredicate inListPredicate = new InListPredicate(
							(Expression) predicate.getTestExpression().accept( this )
					);

					inferableTypeAccessStack.push(
							() -> determineValueMapping( predicate.getTestExpression() ) );

					try {
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
					}
					finally {
						inferableTypeAccessStack.pop();
					}

					return inListPredicate;
				}
			}
		}


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
	public InSubQueryPredicate visitInSubQueryPredicate(SqmInSubQueryPredicate predicate) {
		return new InSubQueryPredicate(
				(Expression) predicate.getTestExpression().accept( this ),
				(QuerySpec) predicate.getSubQueryExpression().accept( this ),
				predicate.isNegated()
		);
	}
}
