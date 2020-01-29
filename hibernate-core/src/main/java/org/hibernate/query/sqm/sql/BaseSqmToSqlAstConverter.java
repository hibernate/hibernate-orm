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
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.model.domain.AllowableParameterType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.internal.CompositeSqmPathSource;
import org.hibernate.metamodel.model.domain.internal.EmbeddedSqmPathSource;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.BinaryArithmeticOperator;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.UnaryArithmeticOperator;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.InterpretationException;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmMappingModelHelper;
import org.hibernate.query.sqm.spi.BaseSemanticQueryWalker;
import org.hibernate.query.sqm.spi.JdbcParameterBySqmParameterAccess;
import org.hibernate.query.sqm.sql.internal.BasicValuedPathInterpretation;
import org.hibernate.query.sqm.sql.internal.EmbeddableValuedPathInterpretation;
import org.hibernate.query.sqm.sql.internal.EntityValuedPathInterpretation;
import org.hibernate.query.sqm.sql.internal.SqlAstQuerySpecProcessingStateImpl;
import org.hibernate.query.sqm.sql.internal.SqmParameterInterpretation;
import org.hibernate.query.sqm.sql.internal.SqmPathInterpretation;
import org.hibernate.query.sqm.tree.cte.SqmCteConsumer;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.cte.SqmCteTable;
import org.hibernate.query.sqm.tree.cte.SqmCteTableColumn;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;
import org.hibernate.query.sqm.tree.expression.JpaCriteriaParameter;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmCaseSearched;
import org.hibernate.query.sqm.tree.expression.SqmCaseSimple;
import org.hibernate.query.sqm.tree.expression.SqmCastTarget;
import org.hibernate.query.sqm.tree.expression.SqmDistinct;
import org.hibernate.query.sqm.tree.expression.SqmEnumLiteral;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmExtractUnit;
import org.hibernate.query.sqm.tree.expression.SqmFieldLiteral;
import org.hibernate.query.sqm.tree.expression.SqmFormat;
import org.hibernate.query.sqm.tree.expression.SqmFunction;
import org.hibernate.query.sqm.tree.expression.SqmJpaCriteriaParameterWrapper;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.SqmLiteralNull;
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;
import org.hibernate.query.sqm.tree.expression.SqmStar;
import org.hibernate.query.sqm.tree.expression.SqmTrimSpecification;
import org.hibernate.query.sqm.tree.expression.SqmUnaryOperation;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmJoin;
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
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.spi.SqlAstQuerySpecProcessingState;
import org.hibernate.sql.ast.spi.SqlAstTreeHelper;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.cte.CteColumn;
import org.hibernate.sql.ast.tree.cte.CteConsumer;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.cte.CteTable;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.Distinct;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.ExtractUnit;
import org.hibernate.sql.ast.tree.expression.Format;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.Star;
import org.hibernate.sql.ast.tree.expression.TrimSpecification;
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
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.internal.JdbcParametersImpl;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameters;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

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
		implements SqmToSqlAstConverter, JdbcParameterBySqmParameterAccess, FromClauseAccess {

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
	private final Map<JpaCriteriaParameter<?>,Supplier<SqmJpaCriteriaParameterWrapper<?>>> jpaCriteriaParamResolutions;


	private final SqlAliasBaseManager sqlAliasBaseManager = new SqlAliasBaseManager();

	private final FromClauseIndex fromClauseIndex = new FromClauseIndex();

	private final Stack<SqlAstProcessingState> processingStateStack = new StandardStack<>();

	private final Stack<Clause> currentClauseStack = new StandardStack<>();
	private final Stack<Shallowness> shallownessStack = new StandardStack<>( Shallowness.NONE );


	public BaseSqmToSqlAstConverter(
			SqlAstCreationContext creationContext,
			QueryOptions queryOptions,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings) {
		super( creationContext.getServiceRegistry() );
		this.creationContext = creationContext;
		this.queryOptions = queryOptions;
		this.domainParameterXref = domainParameterXref;
		this.domainParameterBindings = domainParameterBindings;
		this.jpaCriteriaParamResolutions = domainParameterXref.getParameterResolutions().getJpaCriteriaParamResolutions();
	}

	protected Stack<SqlAstProcessingState> getProcessingStateStack() {
		return processingStateStack;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// FromClauseAccess

	@Override
	public TableGroup findTableGroup(NavigablePath navigablePath) {
		return fromClauseIndex.findTableGroup( navigablePath );
	}

	@Override
	public void registerTableGroup(NavigablePath navigablePath, TableGroup tableGroup) {
		throw new UnsupportedOperationException();
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
	public CteStatement visitCteStatement(SqmCteStatement sqmCteStatement) {
		final CteTable cteTable = createCteTable( sqmCteStatement );

		return new CteStatement(
				visitQuerySpec( sqmCteStatement.getCteDefinition() ),
				sqmCteStatement.getCteLabel(),
				cteTable,
				visitCteConsumer( sqmCteStatement.getCteConsumer() )
		);
	}

	protected CteTable createCteTable(SqmCteStatement sqmCteStatement) {
		final SqmCteTable sqmCteTable = sqmCteStatement.getCteTable();
		final List<SqmCteTableColumn> sqmCteColumns = sqmCteTable.getColumns();
		final List<CteColumn> sqlCteColumns = new ArrayList<>( sqmCteColumns.size() );

		for ( int i = 0; i < sqmCteColumns.size(); i++ ) {
			final SqmCteTableColumn sqmCteTableColumn = sqmCteColumns.get( i );

			sqlCteColumns.add(
					new CteColumn(
							sqmCteTableColumn.getColumnName(),
							sqmCteTableColumn.getType()
					)
			);
		}

		return new CteTable(
				sqlCteColumns,
				getCreationContext().getSessionFactory()
		);
	}

	@Override
	public CteConsumer visitCteConsumer(SqmCteConsumer consumer) {
		return (CteConsumer) super.visitCteConsumer( consumer );
	}

	@Override
	public QuerySpec visitQuerySpec(SqmQuerySpec sqmQuerySpec) {
		final QuerySpec sqlQuerySpec = new QuerySpec( processingStateStack.isEmpty(), sqmQuerySpec.getFromClause().getNumberOfRoots() );

		additionalRestrictions = null;

		processingStateStack.push(
				new SqlAstQuerySpecProcessingStateImpl(
						sqlQuerySpec,
						processingStateStack.getCurrent(),
						this,
						currentClauseStack::getCurrent
				)
		);

		try {
			prepareQuerySpec( sqlQuerySpec );

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

			if ( additionalRestrictions != null ) {
				sqlQuerySpec.applyPredicate( additionalRestrictions );
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

			postProcessQuerySpec( sqlQuerySpec );

			return sqlQuerySpec;
		}
		finally {
			processingStateStack.pop();
		}
	}

	protected void prepareQuerySpec(QuerySpec sqlQuerySpec) {
	}

	protected void postProcessQuerySpec(QuerySpec sqlQuerySpec) {
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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// FROM clause

	@Override
	public Void visitFromClause(SqmFromClause sqmFromClause) {
		currentClauseStack.push( Clause.FROM );

		try {
			sqmFromClause.visitRoots( this::consumeFromClauseRoot );
		}
		finally {
			currentClauseStack.pop();
		}

		return null;
	}

	protected Predicate additionalRestrictions;

	@SuppressWarnings("WeakerAccess")
	protected void consumeFromClauseRoot(SqmRoot<?> sqmRoot) {
		log.tracef( "Resolving SqmRoot [%s] to TableGroup", sqmRoot );

		assert ! fromClauseIndex.isResolved( sqmRoot );

		final EntityPersister entityDescriptor = resolveEntityPersister( sqmRoot.getReferencedPathSource() );

		final TableGroup tableGroup = entityDescriptor.createRootTableGroup(
				sqmRoot.getNavigablePath(),
				sqmRoot.getExplicitAlias(),
				true,
				LockMode.NONE,
				sqlAliasBaseManager,
				getSqlExpressionResolver(),
				() -> predicate -> additionalRestrictions = SqlAstTreeHelper.combinePredicates( additionalRestrictions, predicate ),
				creationContext
		);

		log.tracef( "Resolved SqmRoot [%s] to new TableGroup [%s]", sqmRoot, tableGroup );

		fromClauseIndex.register( sqmRoot, tableGroup );
		currentQuerySpec().getFromClause().addRoot( tableGroup );

		consumeExplicitJoins( sqmRoot, tableGroup );
		consumeImplicitJoins( sqmRoot, tableGroup );
	}

	private EntityPersister resolveEntityPersister(EntityDomainType<?> entityDomainType) {
		return creationContext.getDomainModel().getEntityDescriptor( entityDomainType.getHibernateEntityName() );
	}

	protected void consumeExplicitJoins(SqmFrom<?,?> sqmFrom, TableGroup lhsTableGroup) {
		log.tracef( "Visiting explicit joins for `%s`", sqmFrom.getNavigablePath() );

		sqmFrom.visitSqmJoins(
				sqmJoin -> consumeExplicitJoin( sqmJoin, lhsTableGroup )
		);
	}

	@SuppressWarnings("WeakerAccess")
	protected void consumeExplicitJoin(SqmJoin<?,?> sqmJoin, TableGroup lhsTableGroup) {
		if ( sqmJoin instanceof SqmAttributeJoin ) {
			consumeAttributeJoin( ( (SqmAttributeJoin) sqmJoin ), lhsTableGroup );
		}
		else if ( sqmJoin instanceof SqmCrossJoin ) {
			consumeCrossJoin( ( (SqmCrossJoin) sqmJoin ), lhsTableGroup );
		}
		else if ( sqmJoin instanceof SqmEntityJoin ) {
			consumeEntityJoin( ( (SqmEntityJoin) sqmJoin ), lhsTableGroup );
		}
		else {
			throw new InterpretationException( "Could not resolve SqmJoin [" + sqmJoin.getNavigablePath() + "] to TableGroupJoin" );
		}
	}

	private void consumeAttributeJoin(SqmAttributeJoin<?,?> sqmJoin, TableGroup lhsTableGroup) {
		assert fromClauseIndex.findTableGroup( sqmJoin.getNavigablePath() ) == null;

		final SqmPathSource<?> pathSource = sqmJoin.getReferencedPathSource();

		final AttributeMapping attributeMapping = (AttributeMapping) lhsTableGroup.getModelPart().findSubPart(
				pathSource.getPathName(),
				SqmMappingModelHelper.resolveExplicitTreatTarget( sqmJoin, this )
		);

		assert attributeMapping instanceof TableGroupJoinProducer;
		final TableGroupJoin tableGroupJoin = ( (TableGroupJoinProducer) attributeMapping ).createTableGroupJoin(
				sqmJoin.getNavigablePath(),
				lhsTableGroup,
				sqmJoin.getExplicitAlias(),
				sqmJoin.getSqmJoinType().getCorrespondingSqlJoinType(),
				determineLockMode( sqmJoin.getExplicitAlias() ),
				sqlAliasBaseManager, getSqlExpressionResolver(),
				creationContext
		);

		lhsTableGroup.addTableGroupJoin( tableGroupJoin );
		fromClauseIndex.register( sqmJoin, tableGroupJoin.getJoinedGroup() );

		// add any additional join restrictions
		if ( sqmJoin.getJoinPredicate() != null ) {
			tableGroupJoin.applyPredicate(
					(Predicate) sqmJoin.getJoinPredicate().accept( this )
			);
		}

		consumeExplicitJoins( sqmJoin, tableGroupJoin.getJoinedGroup() );
		consumeImplicitJoins( sqmJoin, tableGroupJoin.getJoinedGroup() );
	}

	private void consumeCrossJoin(SqmCrossJoin sqmJoin, TableGroup lhsTableGroup) {
		final EntityPersister entityDescriptor = resolveEntityPersister( sqmJoin.getReferencedPathSource() );

		final TableGroup tableGroup = entityDescriptor.createRootTableGroup(
				sqmJoin.getNavigablePath(),
				sqmJoin.getExplicitAlias(),
				true,
				determineLockMode( sqmJoin.getExplicitAlias() ),
				sqlAliasBaseManager,
				getSqlExpressionResolver(),
				() -> predicate -> additionalRestrictions = SqlAstTreeHelper.combinePredicates( additionalRestrictions, predicate ),
				getCreationContext()
		);

		final TableGroupJoin tableGroupJoin = new TableGroupJoin(
				sqmJoin.getNavigablePath(),
				SqlAstJoinType.CROSS,
				tableGroup
		);

		lhsTableGroup.addTableGroupJoin( tableGroupJoin );

		fromClauseIndex.register( sqmJoin, tableGroup );

		consumeExplicitJoins( sqmJoin, tableGroupJoin.getJoinedGroup() );
		consumeImplicitJoins( sqmJoin, tableGroupJoin.getJoinedGroup() );
	}

	private void consumeEntityJoin(SqmEntityJoin sqmJoin, TableGroup lhsTableGroup) {
		final EntityPersister entityDescriptor = resolveEntityPersister( sqmJoin.getReferencedPathSource() );

		final TableGroup tableGroup = entityDescriptor.createRootTableGroup(
				sqmJoin.getNavigablePath(),
				sqmJoin.getExplicitAlias(),
				true,
				determineLockMode( sqmJoin.getExplicitAlias() ),
				sqlAliasBaseManager,
				getSqlExpressionResolver(),
				() -> predicate -> additionalRestrictions = SqlAstTreeHelper.combinePredicates( additionalRestrictions, predicate ),
				getCreationContext()
		);
		fromClauseIndex.register( sqmJoin, tableGroup );

		final TableGroupJoin tableGroupJoin = new TableGroupJoin(
				sqmJoin.getNavigablePath(),
				sqmJoin.getSqmJoinType().getCorrespondingSqlJoinType(),
				tableGroup,
				null
		);
		lhsTableGroup.addTableGroupJoin( tableGroupJoin );

		// add any additional join restrictions
		if ( sqmJoin.getJoinPredicate() != null ) {
			tableGroupJoin.applyPredicate(
					(Predicate) sqmJoin.getJoinPredicate().accept( this )
			);
		}

		consumeExplicitJoins( sqmJoin, tableGroupJoin.getJoinedGroup() );
		consumeImplicitJoins( sqmJoin, tableGroupJoin.getJoinedGroup() );
	}

	private void consumeImplicitJoins(SqmPath<?> sqmPath, TableGroup tableGroup) {
		log.tracef( "Visiting implicit joins for `%s`", sqmPath.getNavigablePath() );

		sqmPath.visitImplicitJoinPaths(
				joinedPath -> {
					log.tracef( "Starting implicit join handling for `%s`", joinedPath.getNavigablePath() );

					assert getFromClauseAccess().findTableGroup( joinedPath.getLhs().getNavigablePath() ) == tableGroup;

					final ModelPart subPart = tableGroup.getModelPart().findSubPart(
							joinedPath.getReferencedPathSource().getPathName(),
							sqmPath instanceof SqmTreatedPath
									? resolveEntityPersister( ( (SqmTreatedPath) sqmPath ).getTreatTarget() )
									: null
					);

					assert subPart instanceof TableGroupJoinProducer;
					final TableGroupJoinProducer joinProducer = (TableGroupJoinProducer) subPart;
					final TableGroupJoin tableGroupJoin = joinProducer.createTableGroupJoin(
							joinedPath.getNavigablePath(),
							tableGroup,
							null,
							tableGroup.isInnerJoinPossible() ? SqlAstJoinType.INNER : SqlAstJoinType.LEFT,
							null,
							sqlAliasBaseManager,
							getSqlExpressionResolver(),
							creationContext
					);

					fromClauseIndex.register( joinedPath, tableGroupJoin.getJoinedGroup() );

					consumeImplicitJoins( joinedPath, tableGroupJoin.getJoinedGroup() );
				}
		);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqmPath handling
	//		- Note that SqmFrom references defined in the FROM-clause are already
	//			handled during `#visitFromClause`

	@Override
	public TableGroup visitRootPath(SqmRoot<?> sqmRoot) {
		final TableGroup resolved = fromClauseIndex.findTableGroup( sqmRoot.getNavigablePath() );
		if ( resolved != null ) {
			log.tracef( "SqmRoot [%s] resolved to existing TableGroup [%s]", sqmRoot, resolved );
			return resolved;
		}

		throw new InterpretationException( "SqmRoot not yet resolved to TableGroup" );
	}

	@Override
	public TableGroup visitQualifiedAttributeJoin(SqmAttributeJoin<?, ?> sqmJoin) {
		// todo (6.0) : have this resolve to TableGroup instead?
		//		- trying to remove tracking of TableGroupJoin in the x-refs

		final TableGroup existing = fromClauseIndex.findTableGroup( sqmJoin.getNavigablePath() );
		if ( existing != null ) {
			log.tracef( "SqmAttributeJoin [%s] resolved to existing TableGroup [%s]", sqmJoin, existing );
			return existing;
		}

		throw new InterpretationException( "SqmAttributeJoin not yet resolved to TableGroup" );
	}

	private QuerySpec currentQuerySpec() {
		final SqlAstQuerySpecProcessingState processingState = (SqlAstQuerySpecProcessingState) getProcessingStateStack().getCurrent();
		return processingState.getInflightQuerySpec();
	}

	@Override
	public TableGroup visitCrossJoin(SqmCrossJoin<?> sqmJoin) {
		// todo (6.0) : have this resolve to TableGroup instead?
		//		- trying to remove tracking of TableGroupJoin in the x-refs

		final TableGroup existing = fromClauseIndex.findTableGroup( sqmJoin.getNavigablePath() );
		if ( existing != null ) {
			log.tracef( "SqmCrossJoin [%s] resolved to existing TableGroup [%s]", sqmJoin, existing );
			return existing;
		}

		throw new InterpretationException( "SqmCrossJoin not yet resolved to TableGroup" );
	}

	@Override
	public TableGroup visitQualifiedEntityJoin(SqmEntityJoin sqmJoin) {
		// todo (6.0) : have this resolve to TableGroup instead?
		//		- trying to remove tracking of TableGroupJoin in the x-refs

		final TableGroup existing = fromClauseIndex.findTableGroup( sqmJoin.getNavigablePath() );
		if ( existing != null ) {
			log.tracef( "SqmEntityJoin [%s] resolved to existing TableGroup [%s]", sqmJoin, existing );
			return existing;
		}

		throw new InterpretationException( "SqmEntityJoin not yet resolved to TableGroup" );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqmPath

	@Override
	public SqmPathInterpretation<?> visitBasicValuedPath(SqmBasicValuedSimplePath<?> sqmPath) {
		return BasicValuedPathInterpretation.from( sqmPath, this, this );
	}

	@Override
	public SqmPathInterpretation<?> visitEmbeddableValuedPath(SqmEmbeddedValuedSimplePath<?> sqmPath) {
		return EmbeddableValuedPathInterpretation.from( sqmPath, this, this );
	}

	@Override
	public SqmPathInterpretation<?> visitEntityValuedPath(SqmEntityValuedSimplePath sqmPath) {
		return EntityValuedPathInterpretation.from( sqmPath, this);
	}

	@Override
	public SqmPathInterpretation<?> visitPluralValuedPath(SqmPluralValuedSimplePath sqmPath) {
		return (SqmPathInterpretation) sqmPath;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// General expressions

	@Override
	public Expression visitLiteral(SqmLiteral literal) {
		if ( literal instanceof SqmLiteralNull ) {
			return new QueryLiteral( null, (BasicValuedMapping) inferableTypeAccessStack.getCurrent().get() );
		}

		return new QueryLiteral(
				literal.getLiteralValue(),
				(BasicValuedMapping) SqmMappingModelHelper.resolveMappingModelExpressable(
						literal,
						getCreationContext().getDomainModel(),
						getFromClauseAccess()::findTableGroup
				)
		);
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


	protected Expression consumeSqmParameter(SqmParameter sqmParameter) {
		final MappingModelExpressable valueMapping = determineValueMapping( sqmParameter );
		final List<JdbcParameter> jdbcParametersForSqm = new ArrayList<>();

		resolveSqmParameter(
				sqmParameter,
				valueMapping,
				jdbcParametersForSqm::add
		);

		this.jdbcParameters.addParameters( jdbcParametersForSqm );
		this.jdbcParamsBySqmParam.put( sqmParameter, jdbcParametersForSqm );

		final QueryParameterImplementor<?> queryParameter = domainParameterXref.getQueryParameter( sqmParameter );
		final QueryParameterBinding<?> binding = domainParameterBindings.getBinding( queryParameter );

		return new SqmParameterInterpretation(
				sqmParameter,
				queryParameter,
				jdbcParametersForSqm,
				valueMapping,
				qp -> binding
		);
	}

	protected MappingModelExpressable<?> resolveMappingExpressable(SqmExpressable<?> nodeType) {
		final MappingModelExpressable valueMapping = getCreationContext().getDomainModel().resolveMappingExpressable( nodeType );

		if ( valueMapping == null ) {
			final Supplier<MappingModelExpressable> currentExpressableSupplier = inferableTypeAccessStack.getCurrent();
			if ( currentExpressableSupplier != null ) {
				return currentExpressableSupplier.get();
			}
		}

		if ( valueMapping == null ) {
			throw new ConversionException( "Could not determine ValueMapping for SqmExpressable: " + nodeType );
		}

		return valueMapping;
	}

	protected MappingModelExpressable<?> determineValueMapping(SqmExpression<?> sqmExpression) {
		if ( sqmExpression instanceof SqmParameter ) {
			return determineValueMapping( (SqmParameter) sqmExpression );
		}

		if ( sqmExpression instanceof SqmPath ) {
			log.debugf( "Determining mapping-model type for SqmPath : %s ", sqmExpression );
			return SqmMappingModelHelper.resolveMappingModelExpressable(
					sqmExpression,
					getCreationContext().getDomainModel(),
					getFromClauseAccess()::findTableGroup
			);
		}


		log.debugf( "Determining mapping-model type for generalized SqmExpression : %s", sqmExpression );
		final SqmExpressable<?> nodeType = sqmExpression.getNodeType();
		final MappingModelExpressable valueMapping = getCreationContext().getDomainModel().resolveMappingExpressable( nodeType );

		if ( valueMapping == null ) {
			final Supplier<MappingModelExpressable> currentExpressableSupplier = inferableTypeAccessStack.getCurrent();
			if ( currentExpressableSupplier != null ) {
				return currentExpressableSupplier.get();
			}
		}

		if ( valueMapping == null ) {
			throw new ConversionException( "Could not determine ValueMapping for SqmExpression: " + sqmExpression );
		}

		return valueMapping;
	}

	@SuppressWarnings("WeakerAccess")
	protected MappingModelExpressable<?> determineValueMapping(SqmParameter<?> sqmParameter) {
		log.debugf( "Determining mapping-model type for SqmParameter : %s", sqmParameter );

		final QueryParameterImplementor<?> queryParameter = domainParameterXref.getQueryParameter( sqmParameter );
		final QueryParameterBinding<?> binding = domainParameterBindings.getBinding( queryParameter );

		if ( sqmParameter.getAnticipatedType() == null ) {
			// this should indicate the condition that the user query did not define an
			// explicit type in regard to this parameter.  Here we should prefer the
			// inferable type and fallback to the binding type
			final Supplier<MappingModelExpressable> currentExpressableSupplier = inferableTypeAccessStack.getCurrent();
			if ( currentExpressableSupplier != null ) {
				final MappingModelExpressable inferredMapping = currentExpressableSupplier.get();
				if ( inferredMapping != null ) {
					return inferredMapping;
				}
			}
		}

		AllowableParameterType<?> parameterSqmType = binding.getBindType();
		if ( parameterSqmType == null ) {
			parameterSqmType = queryParameter.getHibernateType();
			if ( parameterSqmType == null ) {
				parameterSqmType = sqmParameter.getAnticipatedType();
			}
		}

		assert parameterSqmType != null;

		if ( parameterSqmType instanceof BasicValuedMapping ) {
			return (BasicValuedMapping) parameterSqmType;
		}

		if ( parameterSqmType instanceof CompositeSqmPathSource ) {
			throw new NotYetImplementedFor6Exception( "Support for embedded-valued parameters not yet implemented" );
		}

		throw new ConversionException( "Could not determine ValueMapping for SqmParameter: " + sqmParameter );
	}

	protected final Stack<Supplier<MappingModelExpressable>> inferableTypeAccessStack = new StandardStack<>(
			() -> null
	);

	private void resolveSqmParameter(SqmParameter expression, MappingModelExpressable valueMapping, Consumer<JdbcParameter> jdbcParameterConsumer) {
		valueMapping.visitJdbcTypes(
				jdbcMapping -> jdbcParameterConsumer.accept( new JdbcParameterImpl( jdbcMapping ) ),
				getCurrentClauseStack().getCurrent(),
				getCreationContext().getDomainModel().getTypeConfiguration()
		);
	}

	@Override
	public Object visitPositionalParameterExpression(SqmPositionalParameter expression) {
		return consumeSqmParameter( expression );
	}

	@Override
	public Object visitJpaCriteriaParameter(JpaCriteriaParameter<?> expression) {
		if ( jpaCriteriaParamResolutions == null ) {
			throw new IllegalStateException( "No JpaCriteriaParameter resolutions registered" );
		}

		final Supplier<SqmJpaCriteriaParameterWrapper<?>> supplier = jpaCriteriaParamResolutions.get( expression );
		if ( supplier == null ) {
			throw new IllegalStateException( "Criteria parameter [" + expression + "] not known to be a parameter of the processing tree" );
		}

		return consumeSqmParameter( supplier.get() );
	}


	@Override
	public Expression visitFunction(SqmFunction sqmFunction) {
		final SqmFunctionDescriptor functionDescriptor = sqmFunction.getFunctionDescriptor();

		shallownessStack.push( Shallowness.FUNCTION );
		try {
			//noinspection unchecked
			return functionDescriptor.generateSqlExpression(
					sqmFunction.getFunctionName(),
					sqmFunction.getArguments(),
					inferableTypeAccessStack.getCurrent(),
					this,
					this
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public Star visitStar(SqmStar sqmStar) {
		return new Star();
	}

	@Override
	public Object visitDistinct(SqmDistinct sqmDistinct) {
		return new Distinct( (Expression) sqmDistinct.getExpression().accept( this ) );
	}

	@Override
	public Object visitTrimSpecification(SqmTrimSpecification specification) {
		shallownessStack.push( Shallowness.FUNCTION );
		try {
			return new TrimSpecification( specification.getSpecification() );
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public Object visitCastTarget(SqmCastTarget target) {
		shallownessStack.push( Shallowness.FUNCTION );
		try {
			final JavaTypeDescriptor jtd = target.getNodeType().getExpressableJavaTypeDescriptor();
			final BasicType basicType = creationContext.getDomainModel()
					.getTypeConfiguration()
					.standardBasicTypeForJavaType( jtd.getJavaType() );
			return new CastTarget( basicType );
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
					unit.getTemporalUnit(),
					(BasicValuedMapping) unit.getType()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public Object visitFormat(SqmFormat sqmFormat) {
		return new Format(
				sqmFormat.getLiteralValue(),
				(BasicValuedMapping) sqmFormat.getNodeType()
		);
	}

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
//					expression.getJdbcMapping().getSqlExpressableType()
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
//					( (BasicValuedExpressableType) function.getJdbcMapping() ).getSqlExpressableType()
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
//					( (BasicValuedExpressableType) expression.getJdbcMapping() ).getSqlExpressableType(),
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
//				( (BasicValuedExpressableType) function.getJdbcMapping() ).getSqlExpressableType()
//		);
//	}
//
//	@Override
//	public CurrentTimeFunction visitCurrentTimeFunction(SqmCurrentTimeFunction function) {
//		return new CurrentTimeFunction(
//				( (BasicValuedExpressableType) function.getJdbcMapping() ).getSqlExpressableType()
//		);
//	}
//
//	@Override
//	public CurrentTimestampFunction visitCurrentTimestampFunction(SqmCurrentTimestampFunction function) {
//		return new CurrentTimestampFunction(
//				( (BasicValuedExpressableType) function.getJdbcMapping() ).getSqlExpressableType()
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
//					( (BasicValuedExpressableType) function.getJdbcMapping() ).getSqlExpressableType()
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
//					( (BasicValuedExpressableType) function.getJdbcMapping() ).getSqlExpressableType()
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
//					expression.getJdbcMapping().getSqlExpressableType()
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
//					expression.getJdbcMapping().getSqlExpressableType()
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
//					( (BasicValuedExpressableType) function.getJdbcMapping() ).getSqlExpressableType()
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
//					( (BasicValuedExpressableType) expression.getJdbcMapping() ).getSqlExpressableType()
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
//					( (BasicValuedExpressableType) expression.getJdbcMapping() ).getSqlExpressableType(),
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
//					expression.getJdbcMapping().getSqlExpressableType()
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
//				( (BasicValuedExpressableType) expression.getJdbcMapping() ).getSqlExpressableType()
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
//				( (BasicValuedExpressableType) sqmFunction.getJdbcMapping() ).getSqlExpressableType()
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
//				expression.getJdbcMapping().getSqlExpressableType()
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
//						null, //(BasicType) extractOrmType( expression.getJdbcMapping() ),
//						(Expression) expression.getLeftHandOperand().accept( this ),
//						(Expression) expression.getRightHandOperand().accept( this )
//				);
			}
			return new BinaryArithmeticExpression(
					(Expression) expression.getLeftHandOperand().accept( this ), interpret( expression.getOperator() ),
					(Expression) expression.getRightHandOperand().accept( this ),
					(BasicValuedMapping) determineValueMapping( expression )
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
	public QuerySpec visitSubQueryExpression(SqmSubQuery sqmSubQuery) {
		return visitQuerySpec( sqmSubQuery.getQuerySpec() );
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
		SqmExpressable<?> resultType = expression.getNodeType();
		List<CaseSimpleExpression.WhenFragment> whenFragments = new ArrayList<>( expression.getWhenFragments().size() );
		for ( SqmCaseSimple.WhenFragment<?, ?> whenFragment : expression.getWhenFragments() ) {
			resultType = QueryHelper.highestPrecedenceType2( resultType, whenFragment.getResult().getNodeType() );
			whenFragments.add(
				new CaseSimpleExpression.WhenFragment(
					(Expression) whenFragment.getCheckValue().accept(this),
					(Expression) whenFragment.getResult().accept(this)
				)
			);
		}

		Expression otherwise = null;
		if ( expression.getOtherwise() != null ) {
			resultType = QueryHelper.highestPrecedenceType2( resultType, expression.getOtherwise().getNodeType() );
			otherwise = (Expression) expression.getOtherwise().accept(this );
		}

		final CaseSimpleExpression result = new CaseSimpleExpression(
				resolveMappingExpressable( resultType ),
				(Expression) expression.getFixture().accept( this ),
				whenFragments,
				otherwise
		);

		return result;
	}

	@Override
	public CaseSearchedExpression visitSearchedCaseExpression(SqmCaseSearched<?> expression) {
		SqmExpressable<?> resultType = expression.getNodeType();
		List<CaseSearchedExpression.WhenFragment> whenFragments = new ArrayList<>( expression.getWhenFragments().size() );
		for ( SqmCaseSearched.WhenFragment<?> whenFragment : expression.getWhenFragments() ) {
			resultType = QueryHelper.highestPrecedenceType2( resultType, whenFragment.getResult().getNodeType() );
			whenFragments.add(
				new CaseSearchedExpression.WhenFragment(
					(Predicate) whenFragment.getPredicate().accept(this),
					(Expression) whenFragment.getResult().accept(this)
				)
			);
		}

		Expression otherwise = null;
		if ( expression.getOtherwise() != null ) {
			resultType = QueryHelper.highestPrecedenceType2( resultType, expression.getOtherwise().getNodeType() );
			otherwise = (Expression) expression.getOtherwise().accept(this );
		}

		final CaseSearchedExpression result = new CaseSearchedExpression(
				resolveMappingExpressable( resultType ),
				whenFragments,
				otherwise
		);

		return result;
	}

	@Override
	public Object visitEnumLiteral(SqmEnumLiteral sqmEnumLiteral) {
		return new QueryLiteral(
				sqmEnumLiteral.getEnumValue(),
				(BasicValuedMapping) determineValueMapping( sqmEnumLiteral )
		);
	}

	@Override
	public Object visitFieldLiteral(SqmFieldLiteral sqmFieldLiteral) {
		return new QueryLiteral(
				sqmFieldLiteral.getValue(),
				(BasicValuedMapping) determineValueMapping( sqmFieldLiteral )
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
