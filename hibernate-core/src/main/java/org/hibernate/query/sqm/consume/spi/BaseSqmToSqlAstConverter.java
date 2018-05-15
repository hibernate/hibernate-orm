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
import java.util.TreeMap;
import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierComposite;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierSimple;
import org.hibernate.metamodel.model.domain.spi.PersistentAttributeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
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
import org.hibernate.query.sqm.tree.expression.SqmLiteralDate;
import org.hibernate.query.sqm.tree.expression.SqmLiteralDouble;
import org.hibernate.query.sqm.tree.expression.SqmLiteralFalse;
import org.hibernate.query.sqm.tree.expression.SqmLiteralFloat;
import org.hibernate.query.sqm.tree.expression.SqmLiteralInteger;
import org.hibernate.query.sqm.tree.expression.SqmLiteralLong;
import org.hibernate.query.sqm.tree.expression.SqmLiteralNull;
import org.hibernate.query.sqm.tree.expression.SqmLiteralString;
import org.hibernate.query.sqm.tree.expression.SqmLiteralTime;
import org.hibernate.query.sqm.tree.expression.SqmLiteralTimestamp;
import org.hibernate.query.sqm.tree.expression.SqmLiteralTrue;
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;
import org.hibernate.query.sqm.tree.expression.SqmSubQuery;
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
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmFromElementSpace;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;
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
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.ConversionException;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.produce.ordering.internal.SqmColumnReference;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.FromClauseIndex;
import org.hibernate.sql.ast.produce.spi.JoinedTableGroupContext;
import org.hibernate.sql.ast.produce.spi.RootTableGroupContext;
import org.hibernate.sql.ast.produce.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.produce.spi.SqlAstFunctionProducer;
import org.hibernate.sql.ast.produce.spi.SqlAstProducerContext;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.produce.spi.SqlSelectionExpression;
import org.hibernate.sql.ast.produce.spi.TableGroupJoinProducer;
import org.hibernate.sql.ast.produce.sqm.spi.SqmSelectToSqlAstConverter;
import org.hibernate.sql.ast.produce.sqm.spi.SqmToSqlAstConverter;
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
import org.hibernate.sql.ast.tree.spi.expression.NonStandardFunction;
import org.hibernate.sql.ast.tree.spi.expression.NullifFunction;
import org.hibernate.sql.ast.tree.spi.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.spi.expression.SqlTuple;
import org.hibernate.sql.ast.tree.spi.expression.SubQuery;
import org.hibernate.sql.ast.tree.spi.expression.SumFunction;
import org.hibernate.sql.ast.tree.spi.expression.TrimFunction;
import org.hibernate.sql.ast.tree.spi.expression.UnaryOperation;
import org.hibernate.sql.ast.tree.spi.expression.UpperFunction;
import org.hibernate.sql.ast.tree.spi.expression.domain.AnyValuedNavigableReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.BasicValuedNavigableReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.CompositeIdentifierReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.EmbeddableValuedNavigableReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.SimpleIdentifierReference;
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
import org.hibernate.sql.exec.internal.JdbcParametersImpl;
import org.hibernate.sql.exec.internal.StandardJdbcParameterImpl;
import org.hibernate.sql.exec.spi.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameters;
import org.hibernate.sql.results.spi.DomainResultProducer;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.spi.StandardSpiBasicTypes;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public abstract class BaseSqmToSqlAstConverter
		extends BaseSemanticQueryWalker
		implements SqmToSqlAstConverter, SqlAstCreationContext {

	private static final Logger log = Logger.getLogger( BaseSqmToSqlAstConverter.class );

	protected enum Shallowness {
		NONE,
		CTOR,
		FUNCTION,
		SUBQUERY
	}

	private final SqlAstProducerContext producerContext;
	private final QueryOptions queryOptions;

	private final SqlAliasBaseManager sqlAliasBaseManager = new SqlAliasBaseManager();

	private final FromClauseIndex fromClauseIndex = new FromClauseIndex();

	private TableSpace tableSpace;

	private final Stack<QuerySpec> querySpecStack = new StandardStack<>();
	private final Stack<FromClause> fromClauseStack = new StandardStack<>();
	private final Stack<TableGroup> tableGroupStack = new StandardStack<>();
	private final Stack<NavigableReference> navigableReferenceStack = new StandardStack<>();

	private final Stack<Clause> currentClauseStack = new StandardStack<>();
	private final Stack<SqmSelectToSqlAstConverter.Shallowness> shallownessStack = new StandardStack<>( SqmSelectToSqlAstConverter.Shallowness.NONE );

	private final Set<String> affectedTableNames = new HashSet<>();

	// todo (6.0) : decide if we want to do the caching/unique-ing of Expressions here.
	//		its really only needed for top-level select clauses, not sub-queries.
	//		its "ok" to do it for sub-queries as well - just wondering about the overhead.
	private Map<QuerySpec,Map<Expression, SqlSelection>> sqlExpressionToSqlSelectionMapByQuerySpec;

	public BaseSqmToSqlAstConverter(
			SqlAstProducerContext producerContext,
			QueryOptions queryOptions) {
		super( producerContext.getSessionFactory() );
		this.producerContext = producerContext;
		this.queryOptions = queryOptions;
	}

	public SqlAstProducerContext getProducerContext() {
		return producerContext;
	}

	@Override
	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return getProducerContext().getLoadQueryInfluencers();
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

	@Override
	public LockOptions getLockOptions() {
		return getQueryOptions().getLockOptions();
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

	// todo (6.0) : is there ever a time when resolving a sqlSelectable relative to a qualifier ought to return different expressions for multiple references?

	protected Expression normalizeSqlExpression(Expression expression) {
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

	protected void collectSelection(Expression expression, SqlSelection selection) {
		final QuerySpec current = querySpecStack.getCurrent();
		if ( current == null ) {
			// should indicate a DML SQM - nothing to do
			return;
		}

		if ( sqlExpressionToSqlSelectionMapByQuerySpec == null ) {
			sqlExpressionToSqlSelectionMapByQuerySpec = new HashMap<>();
		}

		final Map<Expression, SqlSelection> selectionMap = sqlExpressionToSqlSelectionMapByQuerySpec.computeIfAbsent(
				currentQuerySpec(),
				querySpec -> new HashMap<>()
		);

		selectionMap.putIfAbsent( expression, selection );
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

	@Override
	public TableSpace visitFromElementSpace(SqmFromElementSpace fromElementSpace) {
		tableSpace = fromClauseStack.getCurrent().makeTableSpace();
		try {
			visitRootEntityFromElement( fromElementSpace.getRoot() );
			for ( SqmJoin sqmJoin : fromElementSpace.getJoins() ) {
				sqmJoin.accept( this );
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
		final EntityTypeDescriptor entityMetadata = (EntityTypeDescriptor) binding.getReferencedNavigable();
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
					public LockOptions getLockOptions() {
						return queryOptions.getLockOptions();
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
		final TableGroup lhsTableGroup = fromClauseIndex.findResolvedTableGroup( joinedFromElement.getLhs() );

		final PersistentAttributeDescriptor joinedAttribute = joinedFromElement.getAttributeReference().getReferencedNavigable();
		if ( joinedAttribute instanceof SingularPersistentAttributeEmbedded ) {
			return lhsTableGroup;
		}

		final TableGroupJoinProducer joinProducer = (TableGroupJoinProducer) joinedAttribute;

		final TableGroupJoin tableGroupJoin = joinProducer.createTableGroupJoin(
				joinedFromElement,
				joinedFromElement.getJoinType().getCorrespondingSqlJoinType(),
				new JoinedTableGroupContext() {
					@Override
					public NavigableContainerReference getLhs() {
						return (NavigableContainerReference) lhsTableGroup.getNavigableReference();
					}

					@Override
					public ColumnReferenceQualifier getColumnReferenceQualifier() {
						return lhsTableGroup;
					}

					@Override
					public SqlExpressionResolver getSqlExpressionResolver() {
						return BaseSqmToSqlAstConverter.this.getSqlExpressionResolver();
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
					public LockOptions getLockOptions() {
						return queryOptions.getLockOptions();
					}
				}
		);

		// add any additional join restrictions
		if ( joinedFromElement.getOnClausePredicate() != null ) {
			currentQuerySpec().addRestriction(
					(Predicate) joinedFromElement.getOnClausePredicate().accept( this )
			);
		}

		tableSpace.addJoinedTableGroup( tableGroupJoin );

		tableGroupStack.push( tableGroupJoin.getJoinedGroup() );
		fromClauseIndex.crossReference( joinedFromElement, tableGroupJoin.getJoinedGroup() );

		final NavigableReference navigableReference = tableGroupJoin.getJoinedGroup().getNavigableReference();
		if ( ! navigableReferenceStack.isEmpty() ) {
			final NavigableReference parent = navigableReferenceStack.getCurrent();
			assert parent instanceof NavigableContainerReference;
			( (NavigableContainerReference) parent ).addNavigableReference( navigableReference );
			navigableReferenceStack.push( navigableReference );
		}

		return tableGroupJoin;
	}

	protected abstract SqlExpressionResolver getSqlExpressionResolver();

	@Override
	public TableGroupJoin visitCrossJoinedFromElement(SqmCrossJoin joinedFromElement) {
		final QuerySpec querySpec = currentQuerySpec();
		final EntityTypeDescriptor entityDescriptor = joinedFromElement.getIntrinsicSubclassEntityMetadata();
		final EntityTableGroup group = entityDescriptor.createRootTableGroup(
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
					public LockOptions getLockOptions() {
						return queryOptions.getLockOptions();
					}
				}
		);

		group.applyAffectedTableNames( affectedTableNames::add );

		tableGroupStack.push( group );
		fromClauseIndex.crossReference( joinedFromElement, group );

		TableGroupJoin tableGroupJoin = new TableGroupJoin( JoinType.CROSS, group, null );

		navigableReferenceStack.push( tableGroupJoin.getJoinedGroup().getNavigableReference() );

		return tableGroupJoin;
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
	public DomainResultProducer visitEntityIdentifierReference(SqmEntityIdentifierReference expression) {
		final TableGroup resolvedTableGroup = getFromClauseIndex().findResolvedTableGroup( expression.getExportedFromElement() );
		if ( resolvedTableGroup == null ) {
			throw new ConversionException( "Could not find matching resolved TableGroup : " + expression.getExportedFromElement() );
		}

		final EntityValuedNavigableReference entityReference = (EntityValuedNavigableReference) resolvedTableGroup.getNavigableReference();

		final EntityIdentifier identifierDescriptor = entityReference.getNavigable()
				.getEntityDescriptor()
				.getIdentifierDescriptor();

		if ( identifierDescriptor instanceof EntityIdentifierSimple ) {
			return new SimpleIdentifierReference(
					entityReference,
					expression.getReferencedNavigable(),
					expression.getNavigablePath()
			);
		}
		else {
			assert identifierDescriptor instanceof EntityIdentifierComposite;
			return new CompositeIdentifierReference(
					entityReference,
					(EntityIdentifierComposite) expression.getReferencedNavigable(),
					expression.getNavigablePath()
			);
		}
	}

	@Override
	public BasicValuedNavigableReference visitBasicValuedSingularAttribute(SqmSingularAttributeReferenceBasic sqmAttributeReference) {
		final TableGroup resolvedTableGroup = getFromClauseIndex().findResolvedTableGroup( sqmAttributeReference.getExportedFromElement() );
		if ( resolvedTableGroup == null ) {
			throw new ConversionException(
					"Could not find matching resolved TableGroup for " + sqmAttributeReference + " : " + sqmAttributeReference.getExportedFromElement().getUniqueIdentifier()
			);
		}

		return new BasicValuedNavigableReference(
				(NavigableContainerReference) resolvedTableGroup.getNavigableReference(),
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
				containerReference.getColumnReferenceQualifier(),
				queryOptions.getLockOptions().getEffectiveLockMode( sqmAttributeReference.getIdentificationVariable() )
		);
	}

	@Override
	public EmbeddableValuedNavigableReference visitEmbeddableValuedSingularAttribute(SqmSingularAttributeReferenceEmbedded sqmAttributeReference) {
		final TableGroup resolvedTableGroup = getFromClauseIndex().findResolvedTableGroup( sqmAttributeReference.getExportedFromElement() );
		if ( resolvedTableGroup == null ) {
			throw new ConversionException( "Could not find matching resolved TableGroup : " + sqmAttributeReference.getExportedFromElement() );
		}

		// todo (6.0) : this may not be correct in all situations
		//		may need n "embeddable TableGroup"
		return new EmbeddableValuedNavigableReference(
				(NavigableContainerReference) resolvedTableGroup.getNavigableReference(),
				sqmAttributeReference.getReferencedNavigable(),
				sqmAttributeReference.getNavigablePath(),
				LockMode.READ
		);
	}

	@Override
	public AnyValuedNavigableReference visitAnyValuedSingularAttribute(SqmSingularAttributeReferenceAny sqmAttributeReference) {
		final TableGroup resolvedTableGroup = getFromClauseIndex().findResolvedTableGroup( sqmAttributeReference.getExportedFromElement() );
		if ( resolvedTableGroup == null ) {
			throw new ConversionException( "Could not find matching resolved TableGroup : " + sqmAttributeReference.getExportedFromElement() );
		}

		return new AnyValuedNavigableReference(
				(NavigableContainerReference) resolvedTableGroup.getNavigableReference(),
				sqmAttributeReference.getReferencedNavigable(),
				sqmAttributeReference.getNavigablePath(),
				resolvedTableGroup
		);
	}

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
					getLockOptions().getEffectiveLockMode( reference.getIdentificationVariable() )
			);
		}

		navigableReferenceStack.push( result );

		return result;
	}

	@Override
	public QueryLiteral visitLiteralStringExpression(SqmLiteralString expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				resolveSqlExpressableType( expression.getExpressableType(), StandardSpiBasicTypes.STRING ),
				getCurrentClauseStack().getCurrent()
		);
	}

	protected SqlExpressableType resolveSqlExpressableType(
			BasicValuedExpressableType expressionType,
			BasicValuedExpressableType defaultType) {
		return resolveType( expressionType, defaultType ).getBasicType().getSqlExpressableType( getSessionFactory().getTypeConfiguration() );
	}

	protected BasicValuedExpressableType resolveType(
			BasicValuedExpressableType expressionType,
			BasicValuedExpressableType defaultType) {
		return expressionType != null
				? expressionType
				: defaultType;
	}

	@Override
	public QueryLiteral visitLiteralCharacterExpression(SqmLiteralCharacter expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				resolveSqlExpressableType( expression.getExpressableType(), StandardSpiBasicTypes.CHARACTER ),
				getCurrentClauseStack().getCurrent()
		);
	}

	@Override
	public QueryLiteral visitLiteralDoubleExpression(SqmLiteralDouble expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				resolveSqlExpressableType( expression.getExpressableType(), StandardSpiBasicTypes.DOUBLE ),
				getCurrentClauseStack().getCurrent()
		);
	}

	@Override
	public QueryLiteral visitLiteralIntegerExpression(SqmLiteralInteger expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				resolveSqlExpressableType( expression.getExpressableType(), StandardSpiBasicTypes.INTEGER ),
				getCurrentClauseStack().getCurrent()
		);
	}

	@Override
	public QueryLiteral visitLiteralBigIntegerExpression(SqmLiteralBigInteger expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				resolveSqlExpressableType( expression.getExpressableType(), StandardSpiBasicTypes.BIG_INTEGER ),
				getCurrentClauseStack().getCurrent()
		);
	}

	@Override
	public QueryLiteral visitLiteralBigDecimalExpression(SqmLiteralBigDecimal expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				resolveSqlExpressableType( expression.getExpressableType(), StandardSpiBasicTypes.BIG_DECIMAL ),
				getCurrentClauseStack().getCurrent()
		);
	}

	@Override
	public QueryLiteral visitLiteralFloatExpression(SqmLiteralFloat expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				resolveSqlExpressableType( expression.getExpressableType(), StandardSpiBasicTypes.FLOAT ),
				getCurrentClauseStack().getCurrent()
		);
	}

	@Override
	public QueryLiteral visitLiteralLongExpression(SqmLiteralLong expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				resolveSqlExpressableType( expression.getExpressableType(), StandardSpiBasicTypes.LONG ),
				getCurrentClauseStack().getCurrent()
		);
	}

	@Override
	public QueryLiteral visitLiteralTrueExpression(SqmLiteralTrue expression) {
		return new QueryLiteral(
				Boolean.TRUE,
				resolveSqlExpressableType( expression.getExpressableType(), StandardSpiBasicTypes.BOOLEAN ),
				getCurrentClauseStack().getCurrent()
		);
	}

	@Override
	public QueryLiteral visitLiteralFalseExpression(SqmLiteralFalse expression) {
		return new QueryLiteral(
				Boolean.FALSE,
				resolveSqlExpressableType( expression.getExpressableType(), StandardSpiBasicTypes.BOOLEAN ),
				getCurrentClauseStack().getCurrent()
		);
	}

	@Override
	public QueryLiteral visitLiteralNullExpression(SqmLiteralNull expression) {
		return new QueryLiteral(
				null,
				expression.getExpressableType().getBasicType().getSqlExpressableType( getSessionFactory().getTypeConfiguration() ),
				getCurrentClauseStack().getCurrent()
		);
	}

	@Override
	public QueryLiteral visitLiteralTimestampExpression(SqmLiteralTimestamp literal) {
		return new QueryLiteral(
				literal.getLiteralValue(),
				literal.getExpressableType().getBasicType().getSqlExpressableType( getSessionFactory().getTypeConfiguration() ),
				getCurrentClauseStack().getCurrent()
		);
	}

	@Override
	public QueryLiteral visitLiteralDateExpression(SqmLiteralDate literal) {
		return new QueryLiteral(
				literal.getLiteralValue(),
				literal.getExpressableType().getBasicType().getSqlExpressableType( getSessionFactory().getTypeConfiguration() ),
				getCurrentClauseStack().getCurrent()
		);
	}

	@Override
	public QueryLiteral visitLiteralTimeExpression(SqmLiteralTime literal) {
		return new QueryLiteral(
				literal.getLiteralValue(),
				literal.getExpressableType().getBasicType().getSqlExpressableType( getSessionFactory().getTypeConfiguration() ),
				getCurrentClauseStack().getCurrent()
		);
	}

	@Override
	public Object visitConstantEnumExpression(SqmConstantEnum expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				expression.getExpressableType().getBasicType().getSqlExpressableType( getSessionFactory().getTypeConfiguration() ),
				getCurrentClauseStack().getCurrent()
		);
	}

	@Override
	public Object visitConstantFieldReference(SqmConstantFieldReference expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				expression.getExpressableType().getBasicType().getSqlExpressableType( getSessionFactory().getTypeConfiguration() ),
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
								getProducerContext().getSessionFactory().getTypeConfiguration()
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
												getProducerContext().getSessionFactory().getTypeConfiguration()
										)
								);
							}
						},
						currentClauseStack.getCurrent(),
						getProducerContext().getSessionFactory().getTypeConfiguration()
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
											getProducerContext().getSessionFactory().getTypeConfiguration()
									)
							);
						}
					},
					currentClauseStack.getCurrent(),
					getProducerContext().getSessionFactory().getTypeConfiguration()
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
					( (BasicValuedExpressableType) expression.getExpressableType() ).getBasicType().getSqlExpressableType( getSessionFactory().getTypeConfiguration() ),
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
					expression.getExpressableType().getBasicType().getSqlExpressableType( getSessionFactory().getTypeConfiguration() )
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
					( (BasicValuedExpressableType) function.getExpressableType() ).getBasicType().getSqlExpressableType( getSessionFactory().getTypeConfiguration() )
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
					( (BasicValuedExpressableType) expression.getExpressableType() ).getBasicType().getSqlExpressableType( getSessionFactory().getTypeConfiguration() ),
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
					getSessionFactory().getTypeConfiguration()
							.getBasicTypeRegistry()
							.getBasicType( Long.class )
							.getSqlExpressableType( getSessionFactory().getTypeConfiguration() )
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
				getSessionFactory().getTypeConfiguration()
						.getBasicTypeRegistry()
						.getBasicType( String.class )
						.getSqlExpressableType( getSessionFactory().getTypeConfiguration() )
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
				results.add( getSqlSelectionResolver().resolveSqlExpression(
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
				( (BasicValuedExpressableType) function.getExpressableType() ).getBasicType()
						.getSqlExpressableType( getSessionFactory().getTypeConfiguration() )
		);
	}

	@Override
	public CurrentTimeFunction visitCurrentTimeFunction(SqmCurrentTimeFunction function) {
		return new CurrentTimeFunction(
				( (BasicValuedExpressableType) function.getExpressableType() ).getBasicType()
						.getSqlExpressableType( getSessionFactory().getTypeConfiguration() )
		);
	}

	@Override
	public CurrentTimestampFunction visitCurrentTimestampFunction(SqmCurrentTimestampFunction function) {
		return new CurrentTimestampFunction(
				( (BasicValuedExpressableType) function.getExpressableType() ).getBasicType()
						.getSqlExpressableType( getSessionFactory().getTypeConfiguration() )
		);
	}

	@Override
	public ExtractFunction visitExtractFunction(SqmExtractFunction function) {
		shallownessStack.push( Shallowness.FUNCTION );

		try {
			return new ExtractFunction(
					(Expression) function.getUnitToExtract().accept( this ),
					(Expression) function.getExtractionSource().accept( this ),
					( (BasicValuedExpressableType) function.getExpressableType() ).getBasicType().getSqlExpressableType( getSessionFactory().getTypeConfiguration() )
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
					getSessionFactory().getTypeConfiguration()
							.getBasicTypeRegistry()
							.getBasicType( Long.class )
							.getSqlExpressableType( getSessionFactory().getTypeConfiguration() )
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
					getSessionFactory().getTypeConfiguration()
							.getBasicTypeRegistry()
							.getBasicType( Long.class )
							.getSqlExpressableType( getSessionFactory().getTypeConfiguration() )

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
					( (BasicValuedExpressableType) function.getExpressableType() ).getBasicType().getSqlExpressableType( getSessionFactory().getTypeConfiguration() )
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
					expression.getExpressableType()
							.getBasicType()
							.getSqlExpressableType( getSessionFactory().getTypeConfiguration() )
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
					expression.getExpressableType()
							.getBasicType()
							.getSqlExpressableType( getSessionFactory().getTypeConfiguration() )
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
					( (BasicValuedExpressableType) function.getExpressableType() )
							.getBasicType()
							.getSqlExpressableType( getSessionFactory().getTypeConfiguration() )
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
					expression.getExpressableType()
							.getBasicType()
							.getSqlExpressableType( getSessionFactory().getTypeConfiguration() )

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
							.getBasicType()
							.getSqlExpressableType( getSessionFactory().getTypeConfiguration() )

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
					( (BasicValuedExpressableType) expression.getExpressableType() )
							.getBasicType()
							.getSqlExpressableType( getSessionFactory().getTypeConfiguration() )
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
				( (BasicValuedExpressableType) expression.getExpressableType() )
						.getBasicType()
						.getSqlExpressableType( getSessionFactory().getTypeConfiguration() ),
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
				( (BasicValuedExpressableType) expression.getExpressableType() )
						.getBasicType()
						.getSqlExpressableType( getSessionFactory().getTypeConfiguration() )
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
				( (BasicValuedExpressableType) expression.getExpressableType() ).getBasicType().getSqlExpressableType( getSessionFactory().getTypeConfiguration() )
		);
	}

	@Override
	public Object visitTrimFunction(SqmTrimFunction expression) {
		return new TrimFunction(
				expression.getSpecification(),
				(Expression) expression.getTrimCharacter().accept( this ),
				(Expression) expression.getSource().accept( this ),
				this
		);
	}

	@Override
	public Object visitUpperFunction(SqmUpperFunction sqmFunction) {
		return new UpperFunction(
				(Expression) sqmFunction.getArgument().accept( this ),
				( (BasicValuedExpressableType) sqmFunction.getExpressableType() ).getBasicType().getSqlExpressableType( getSessionFactory().getTypeConfiguration() )
		);

	}

	@Override
	public ConcatFunction visitConcatExpression(SqmConcat expression) {
		return new ConcatFunction(
				Arrays.asList(
						(Expression)expression.getLeftHandOperand().accept( this ),
						(Expression) expression.getRightHandOperand().accept( this )
				),
				expression.getExpressableType().getBasicType().getSqlExpressableType( getSessionFactory().getTypeConfiguration() )
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
					navigableReference.getColumnReferenceQualifier(),
					this
			);
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
				subQueryType = ( (BasicValuedExpressableType) sqmSubQuery.getExpressableType() ).getBasicType().getSqlExpressableType( getSessionFactory().getTypeConfiguration() );
			}

			return new SubQuery( subQuerySpec, subQueryType );
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
				toExpression( predicate.getExpression().accept( this ) ),
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
