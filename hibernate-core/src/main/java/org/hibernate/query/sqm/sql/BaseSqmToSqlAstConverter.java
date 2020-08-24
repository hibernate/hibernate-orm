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
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.dialect.function.TimestampaddFunction;
import org.hibernate.dialect.function.TimestampdiffFunction;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.metamodel.model.domain.AllowableParameterType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.internal.CompositeSqmPathSource;
import org.hibernate.metamodel.model.domain.internal.EmbeddedSqmPathSource;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.BinaryArithmeticOperator;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.QueryLogging;
import org.hibernate.query.SemanticException;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.UnaryArithmeticOperator;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.InterpretationException;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmMappingModelHelper;
import org.hibernate.query.sqm.spi.BaseSemanticQueryWalker;
import org.hibernate.query.sqm.spi.JdbcParameterBySqmParameterAccess;
import org.hibernate.query.sqm.sql.internal.BasicValuedPathInterpretation;
import org.hibernate.query.sqm.sql.internal.EmbeddableValuedPathInterpretation;
import org.hibernate.query.sqm.sql.internal.EntityValuedPathInterpretation;
import org.hibernate.query.sqm.sql.internal.PluralValuedSimplePathInterpretation;
import org.hibernate.query.sqm.sql.internal.SqlAstProcessingStateImpl;
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
import org.hibernate.query.sqm.tree.expression.Conversion;
import org.hibernate.query.sqm.tree.expression.JpaCriteriaParameter;
import org.hibernate.query.sqm.tree.expression.SqmAny;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmByUnit;
import org.hibernate.query.sqm.tree.expression.SqmCaseSearched;
import org.hibernate.query.sqm.tree.expression.SqmCaseSimple;
import org.hibernate.query.sqm.tree.expression.SqmCastTarget;
import org.hibernate.query.sqm.tree.expression.SqmDistinct;
import org.hibernate.query.sqm.tree.expression.SqmDurationUnit;
import org.hibernate.query.sqm.tree.expression.SqmEnumLiteral;
import org.hibernate.query.sqm.tree.expression.SqmEvery;
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
import org.hibernate.query.sqm.tree.expression.SqmToDuration;
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
import org.hibernate.query.sqm.tree.insert.SqmInsertValuesStatement;
import org.hibernate.query.sqm.tree.predicate.SqmAndPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmBetweenPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmComparisonPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmEmptinessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmExistsPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmGroupedPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInListPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInSubQueryPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmLikePredicate;
import org.hibernate.query.sqm.tree.predicate.SqmMemberOfPredicate;
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
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.spi.SqlAstQuerySpecProcessingState;
import org.hibernate.sql.ast.spi.SqlAstTreeHelper;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.cte.CteColumn;
import org.hibernate.sql.ast.tree.cte.CteConsumer;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.cte.CteTable;
import org.hibernate.sql.ast.tree.expression.Any;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.Distinct;
import org.hibernate.sql.ast.tree.expression.Duration;
import org.hibernate.sql.ast.tree.expression.DurationUnit;
import org.hibernate.sql.ast.tree.expression.Every;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.ExtractUnit;
import org.hibernate.sql.ast.tree.expression.Format;
import org.hibernate.sql.ast.tree.expression.JdbcLiteral;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.Star;
import org.hibernate.sql.ast.tree.expression.TrimSpecification;
import org.hibernate.sql.ast.tree.expression.UnaryOperation;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.predicate.BetweenPredicate;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.ExistsPredicate;
import org.hibernate.sql.ast.tree.predicate.GroupedPredicate;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.LikePredicate;
import org.hibernate.sql.ast.tree.predicate.MemberOfPredicate;
import org.hibernate.sql.ast.tree.predicate.NegatedPredicate;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.internal.JdbcParametersImpl;
import org.hibernate.sql.exec.spi.JdbcParameters;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

import static org.hibernate.internal.util.NullnessHelper.coalesceSuppliedValues;
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
		extends BaseSemanticQueryWalker
		implements SqmToSqlAstConverter, JdbcParameterBySqmParameterAccess, FromClauseAccess, DomainResultCreationState {

	private static final Logger log = Logger.getLogger( BaseSqmToSqlAstConverter.class );

	protected enum Shallowness {
		NONE,
		CTOR,
		FUNCTION,
		SUBQUERY
	}

	private final SqlAstCreationContext creationContext;
	private final SessionFactoryImplementor sessionFactory;

	private final QueryOptions queryOptions;
	private final LoadQueryInfluencers loadQueryInfluencers;

	private final DomainParameterXref domainParameterXref;
	private final QueryParameterBindings domainParameterBindings;
	private final Map<JpaCriteriaParameter<?>,Supplier<SqmJpaCriteriaParameterWrapper<?>>> jpaCriteriaParamResolutions;


	private final SqlAliasBaseManager sqlAliasBaseManager = new SqlAliasBaseManager();

	private final FromClauseIndex fromClauseIndex = new FromClauseIndex();

	private final Stack<SqlAstProcessingState> processingStateStack = new StandardStack<>();

	private final Stack<Clause> currentClauseStack = new StandardStack<>();
	private final Stack<Shallowness> shallownessStack = new StandardStack<>( Shallowness.NONE );

	private SqmByUnit appliedByUnit;
	private Expression adjustedTimestamp;
	private SqmExpressable<?> adjustedTimestampType; //TODO: remove this once we can get a Type directly from adjustedTimestamp
	private Expression adjustmentScale;
	private boolean negativeAdjustment;

	public BaseSqmToSqlAstConverter(
			SqlAstCreationContext creationContext,
			QueryOptions queryOptions,
			LoadQueryInfluencers loadQueryInfluencers,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings) {
		super( creationContext.getServiceRegistry() );

		this.creationContext = creationContext;
		this.sessionFactory = creationContext.getSessionFactory();

		this.queryOptions = queryOptions;
		this.loadQueryInfluencers = loadQueryInfluencers;
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

	public QueryOptions getQueryOptions() {
		return queryOptions;
	}

	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return loadQueryInfluencers;
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
	public Object visitUpdateStatement(SqmUpdateStatement<?> statement) {
		throw new AssertionFailure( "UpdateStatement not supported" );
	}

	@Override
	public Object visitDeleteStatement(SqmDeleteStatement<?> statement) {
		throw new AssertionFailure( "DeleteStatement not supported" );
	}

	@Override
	public Object visitInsertSelectStatement(SqmInsertSelectStatement<?> statement) {
		throw new AssertionFailure( "InsertStatement not supported" );
	}

	@Override
	public Object visitInsertValuesStatement(SqmInsertValuesStatement<?> statement) {
		throw new AssertionFailure( "InsertStatement not supported" );
	}

	@Override
	public SelectStatement visitSelectStatement(SqmSelectStatement<?> statement) {
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

		for ( final SqmCteTableColumn sqmCteTableColumn : sqmCteColumns ) {
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
				sortSpecification.getSortOrder(),
				sortSpecification.getNullPrecedence()
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

		if ( fromClauseIndex.isResolved( sqmRoot ) ) {
			log.tracef( "Already resolved SqmRoot [%s] to TableGroup", sqmRoot );
		}

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
		if ( log.isTraceEnabled() ) {
			log.tracef( "Visiting explicit joins for `%s`", sqmFrom.getNavigablePath() );
		}

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

		final SqmPathSource<?> pathSource = sqmJoin.getReferencedPathSource();

		final TableGroupJoin joinedTableGroupJoin;
		final TableGroup joinedTableGroup;

		if ( pathSource instanceof PluralPersistentAttribute ) {
			final ModelPart pluralPart = lhsTableGroup.getModelPart().findSubPart(
					sqmJoin.getReferencedPathSource().getPathName(),
					SqmMappingModelHelper.resolveExplicitTreatTarget( sqmJoin, this )
			);

			assert pluralPart instanceof PluralAttributeMapping;

			final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) pluralPart;

			final NavigablePath elementPath = sqmJoin.getNavigablePath().append( CollectionPart.Nature.ELEMENT.getName() );

			joinedTableGroupJoin = pluralAttributeMapping.createTableGroupJoin(
					elementPath,
					lhsTableGroup,
					sqmJoin.getExplicitAlias(),
					sqmJoin.getSqmJoinType().getCorrespondingSqlJoinType(),
					determineLockMode( sqmJoin.getExplicitAlias() ),
					this
			);
			joinedTableGroup = joinedTableGroupJoin.getJoinedGroup();

			lhsTableGroup.addTableGroupJoin( joinedTableGroupJoin );

			fromClauseIndex.register( sqmJoin, joinedTableGroup );
			fromClauseIndex.registerTableGroup( elementPath, joinedTableGroup );
		}
		else if ( pathSource instanceof EmbeddedSqmPathSource ) {
			final ModelPart joinedPart = lhsTableGroup.getModelPart().findSubPart(
					pathSource.getPathName(),
					SqmMappingModelHelper.resolveExplicitTreatTarget( sqmJoin, this )
			);

			assert joinedPart instanceof TableGroupJoinProducer;

			final NavigablePath joinedPath;
			final String explicitAlias = sqmJoin.getExplicitAlias();
			if ( explicitAlias == null ) {
				joinedPath = sqmJoin.getNavigablePath();
			}
			else {
				joinedPath = sqmJoin.getNavigablePath().getParent().append( sqmJoin.getAttribute().getName() );
			}
			joinedTableGroupJoin = ( (TableGroupJoinProducer) joinedPart ).createTableGroupJoin(
					joinedPath,
					lhsTableGroup,
					sqmJoin.getExplicitAlias(),
					sqmJoin.getSqmJoinType().getCorrespondingSqlJoinType(),
					determineLockMode( sqmJoin.getExplicitAlias() ),
					this
			);

			joinedTableGroup = joinedTableGroupJoin.getJoinedGroup();

			lhsTableGroup.addTableGroupJoin( joinedTableGroupJoin );

			fromClauseIndex.register( sqmJoin, joinedTableGroup );
		}
		else {
			if ( lhsTableGroup.getModelPart() instanceof PluralAttributeMapping ) {
				fromClauseIndex.register( sqmJoin, lhsTableGroup );

				joinedTableGroupJoin = null;
				joinedTableGroup = lhsTableGroup;
			}
			else {
				final ModelPart joinedPart = lhsTableGroup.getModelPart().findSubPart(
						pathSource.getPathName(),
						SqmMappingModelHelper.resolveExplicitTreatTarget( sqmJoin, this )
				);

				if ( ! TableGroupJoinProducer.class.isInstance( joinedPart ) ) {
					throw new HibernateException( "Expecting joined model part to implement TableGroupJoinProducer - " + joinedPart );
				}

				final NavigablePath joinedPath;
				final String explicitAlias = sqmJoin.getExplicitAlias();
				if ( explicitAlias == null ) {
					joinedPath = sqmJoin.getNavigablePath();
				}
				else {
					joinedPath = sqmJoin.getNavigablePath().getParent().append( sqmJoin.getAttribute().getName() );
				}

				joinedTableGroupJoin = ( (TableGroupJoinProducer) joinedPart ).createTableGroupJoin(
						joinedPath,
						lhsTableGroup,
						sqmJoin.getExplicitAlias(),
						sqmJoin.getSqmJoinType().getCorrespondingSqlJoinType(),
						determineLockMode( sqmJoin.getExplicitAlias() ),
						this
				);

				joinedTableGroup = joinedTableGroupJoin.getJoinedGroup();

				lhsTableGroup.addTableGroupJoin( joinedTableGroupJoin );

				fromClauseIndex.register( sqmJoin, joinedTableGroup );
			}
		}

		// add any additional join restrictions
		if ( sqmJoin.getJoinPredicate() != null ) {
			if ( sqmJoin.isFetched() ) {
				QueryLogging.QUERY_MESSAGE_LOGGER.debugf( "Join fetch [" + sqmJoin.getNavigablePath() + "] is restricted" );
			}

			if ( joinedTableGroupJoin == null ) {
				throw new IllegalStateException(  );
			}

			joinedTableGroupJoin.applyPredicate(
					(Predicate) sqmJoin.getJoinPredicate().accept( this )
			);
		}

		consumeExplicitJoins( sqmJoin, joinedTableGroup );
		consumeImplicitJoins( sqmJoin, joinedTableGroup );
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
		if ( log.isTraceEnabled() ) {
			log.tracef( "Visiting implicit joins for `%s`", sqmPath.getNavigablePath() );
		}

		sqmPath.visitImplicitJoinPaths(
				joinedPath -> {
					if ( log.isTraceEnabled() ) {
						log.tracef( "Starting implicit join handling for `%s`", joinedPath.getNavigablePath() );
					}

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
							this
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
	public Expression visitBasicValuedPath(SqmBasicValuedSimplePath<?> sqmPath) {
		BasicValuedPathInterpretation<?> path = BasicValuedPathInterpretation.from( sqmPath, this, this );

		if ( isDuration( sqmPath.getNodeType() ) ) {

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
			Expression scaledExpression = applyScale( toSqlExpression( path ) );

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
						(AllowableFunctionReturnType<?>) adjustedTimestampType,
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

				MappingModelExpressable durationType = scaledExpression.getExpressionType();
				Duration duration = new Duration( scaledExpression, NANOSECOND, (BasicValuedMapping) durationType);

				TemporalUnit appliedUnit = appliedByUnit.getUnit().getUnit();
				BasicValuedMapping scalarType = (BasicValuedMapping) appliedByUnit.getNodeType();
				return new Conversion( duration, appliedUnit, scalarType );
			}
			else {
				// a "bare" Duration value in nanoseconds
				return scaledExpression;
			}
		}

		return path;
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
		return PluralValuedSimplePathInterpretation.from( sqmPath, this );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// General expressions

	@Override
	public Expression visitLiteral(SqmLiteral literal) {
		if ( literal instanceof SqmLiteralNull ) {
			return new QueryLiteral<>( null, (BasicValuedMapping) inferableTypeAccessStack.getCurrent().get() );
		}

		return new QueryLiteral<>(
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

	private void resolveSqmParameter(
			SqmParameter expression,
			MappingModelExpressable valueMapping,
			Consumer<JdbcParameter> jdbcParameterConsumer) {
		if ( valueMapping instanceof EntityValuedModelPart ) {
			( (EntityValuedModelPart) valueMapping ).getEntityMappingType().getIdentifierMapping().visitJdbcTypes(
					jdbcMapping -> jdbcParameterConsumer.accept( new JdbcParameterImpl( jdbcMapping ) ),
					getCurrentClauseStack().getCurrent(),
					getCreationContext().getDomainModel().getTypeConfiguration()
			);
		}
		else {
			valueMapping.visitJdbcTypes(
					jdbcMapping -> jdbcParameterConsumer.accept( new JdbcParameterImpl( jdbcMapping ) ),
					getCurrentClauseStack().getCurrent(),
					getCreationContext().getDomainModel().getTypeConfiguration()
			);
		}
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
		shallownessStack.push( Shallowness.FUNCTION );
		try {
			//noinspection unchecked
			return sqmFunction.convertToSqlAst(this);
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
			return new CastTarget(
					(BasicValuedMapping) target.getType(),
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
					(BasicValuedMapping) unit.getType()
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
					toSqlExpression( expression.getOperand().accept(this) ),
					(BasicValuedMapping) determineValueMapping( expression.getOperand() )
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
	public Object visitBinaryArithmeticExpression(SqmBinaryArithmetic expression) {
		shallownessStack.push( Shallowness.NONE );

		try {
			SqmExpression leftOperand = expression.getLeftHandOperand();
			SqmExpression rightOperand = expression.getRightHandOperand();

			boolean durationToRight = TypeConfiguration.isDuration( rightOperand.getNodeType() );
			TypeConfiguration typeConfiguration = getCreationContext().getDomainModel().getTypeConfiguration();
			boolean temporalTypeToLeft = typeConfiguration.isSqlTemporalType( leftOperand.getNodeType() );
			boolean temporalTypeToRight = typeConfiguration.isSqlTemporalType( rightOperand.getNodeType() );
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
						//we always get a Long value back
						(BasicValuedMapping) appliedByUnit.getNodeType()
				);
			}
			else {
				return new BinaryArithmeticExpression(
						toSqlExpression( leftOperand.accept( this ) ),
						expression.getOperator(),
						toSqlExpression( rightOperand.accept( this ) ),
						getExpressionType( expression )
				);
			}
		}
		finally {
			shallownessStack.pop();
		}
	}

	private BasicValuedMapping getExpressionType(SqmBinaryArithmetic expression) {
		SqmExpressable leftHandOperandType = expression.getLeftHandOperand().getNodeType();
		if ( leftHandOperandType instanceof BasicValuedMapping ) {
			return (BasicValuedMapping) leftHandOperandType;
		}
		else {
			return (BasicValuedMapping) expression.getRightHandOperand().getNodeType();
		}
	}

	private Expression toSqlExpression(Object value) {
		return (Expression) value;
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
				SqmExpressable<?> timestampType = adjustedTimestampType;
				adjustedTimestamp = toSqlExpression( expression.getLeftHandOperand().accept( this ) );
				MappingModelExpressable type = adjustedTimestamp.getExpressionType();
				if (type instanceof SqmExpressable) {
					adjustedTimestampType = (SqmExpressable) type;
				}
//				else if (type instanceof BasicValuedMapping) {
//					adjustedTimestampType = ((BasicValuedMapping) type).getBasicType();
//				}
				else {
					// else we know it has not been transformed
					adjustedTimestampType = expression.getLeftHandOperand().getNodeType();
				}
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
					adjustedTimestampType = timestampType;
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
		boolean leftTimestamp = typeConfiguration.isSqlTimestampType( expression.getLeftHandOperand().getNodeType() ) ;
		boolean rightTimestamp = typeConfiguration.isSqlTimestampType( expression.getRightHandOperand().getNodeType() );

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
			Expression scaledMagnitude = applyScale( timestampdiff().expression(
					(AllowableFunctionReturnType<?>) expression.getNodeType(),
					unit, right, left ) );
			return timestampadd().expression(
					(AllowableFunctionReturnType<?>) adjustedTimestampType, //TODO should be adjustedTimestamp.getType()
					unit, scaledMagnitude, adjustedTimestamp );
		}
		else if (appliedByUnit != null) {
			// we're immediately converting the resulting
			// duration to a scalar in the given unit

			DurationUnit unit = (DurationUnit) appliedByUnit.getUnit().accept(this);
			return applyScale( timestampdiff().expression(
					(AllowableFunctionReturnType<?>) expression.getNodeType(),
					unit, right, left ) );
		}
		else {
			// a plain "bare" Duration
			DurationUnit unit = new DurationUnit( baseUnit, basicType(Integer.class) );
			BasicValuedMapping durationType = (BasicValuedMapping) expression.getNodeType();
			Expression scaledMagnitude = applyScale( timestampdiff().expression(
					(AllowableFunctionReturnType<?>) expression.getNodeType(),
					unit, right, left)  );
			return new Duration( scaledMagnitude, baseUnit, durationType );
		}
	}

	private <J> BasicValuedMapping basicType(Class<J> javaType) {
		return creationContext.getDomainModel().getTypeConfiguration().getBasicTypeForJavaType( javaType );
	}

	private TimestampaddFunction timestampadd() {
		return (TimestampaddFunction)
				getCreationContext().getSessionFactory()
						.getQueryEngine().getSqmFunctionRegistry()
						.findFunctionDescriptor("timestampadd");
	}

	private TimestampdiffFunction timestampdiff() {
		return (TimestampdiffFunction)
				getCreationContext().getSessionFactory()
						.getQueryEngine().getSqmFunctionRegistry()
						.findFunctionDescriptor("timestampdiff");
	}

	private <T> T cleanly(Supplier<T> supplier) {
		SqmByUnit byUnit = appliedByUnit;
		Expression timestamp = adjustedTimestamp;
		SqmExpressable<?> timestampType = adjustedTimestampType;
		Expression scale = adjustmentScale;
		boolean negate = negativeAdjustment;
		adjustmentScale = null;
		negativeAdjustment = false;
		appliedByUnit = null;
		adjustedTimestamp = null;
		adjustedTimestampType = null;
		try {
			return supplier.get();
		}
		finally {
			appliedByUnit = byUnit;
			adjustedTimestamp = timestamp;
			adjustedTimestampType = timestampType;
			adjustmentScale = scale;
			negativeAdjustment = negate;
		}
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
							(BasicValuedMapping) magnitude.getExpressionType()
					);
				}
			}
		}

		if ( negate ) {
			magnitude = new UnaryOperation(
					UNARY_MINUS,
					magnitude,
					(BasicValuedMapping) magnitude.getExpressionType()
			);
		}

		return magnitude;
	}

	@SuppressWarnings("unchecked")
	static boolean isOne(Expression scale) {
		return scale instanceof QueryLiteral
				&& ((QueryLiteral<Number>) scale).getLiteralValue().longValue() == 1L;
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
			return timestampadd().expression(
					(AllowableFunctionReturnType<?>) adjustedTimestampType, //TODO should be adjustedTimestamp.getType()
					unit, scaledMagnitude, adjustedTimestamp );
		}
		else {
			BasicValuedMapping durationType = (BasicValuedMapping) toDuration.getNodeType();
			Duration duration = new Duration( scaledMagnitude, unit.getUnit(), durationType );

			if ( appliedByUnit != null ) {
				// we're applying the 'by unit' operator,
				// producing a literal scalar value in
				// the given unit
				TemporalUnit appliedUnit = appliedByUnit.getUnit().getUnit();
				BasicValuedMapping scalarType = (BasicValuedMapping) appliedByUnit.getNodeType();
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
	public Object visitAny(SqmAny<?> sqmAny) {
		return new Any(
				visitSubQueryExpression( sqmAny.getSubquery() ),
				null //resolveMappingExpressable( sqmAny.getNodeType() )
		);
	}

	@Override
	public Object visitEvery(SqmEvery<?> sqmEvery) {
		return new Every(
				visitSubQueryExpression( sqmEvery.getSubquery() ),
				null //resolveMappingExpressable( sqmEvery.getNodeType() )
		);
	}

	@Override
	public Object visitEnumLiteral(SqmEnumLiteral sqmEnumLiteral) {
		return new QueryLiteral<>(
				sqmEnumLiteral.getEnumValue(),
				(BasicValuedMapping) determineValueMapping( sqmEnumLiteral )
		);
	}

	@Override
	public Object visitFieldLiteral(SqmFieldLiteral sqmFieldLiteral) {
		return new QueryLiteral<>(
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
	public MemberOfPredicate visitMemberOfPredicate(SqmMemberOfPredicate predicate) {
		final SqmPath<?> pluralPath = predicate.getPluralPath();
		final PluralAttributeMapping mappingModelExpressable = (PluralAttributeMapping) determineValueMapping(pluralPath);

		if ( mappingModelExpressable.getElementDescriptor() instanceof EntityCollectionPart ) {
			inferableTypeAccessStack.push(
					() -> ( (EntityCollectionPart) mappingModelExpressable.getElementDescriptor() ).getEntityMappingType()
							.getIdentifierMapping() );
		}
		else {
			inferableTypeAccessStack.push( () -> mappingModelExpressable );
		}

		final Expression lhs;
		try {
			lhs = (Expression) predicate.getLeftHandExpression().accept( this );
		}
		finally {
			inferableTypeAccessStack.pop();
		}

		return new MemberOfPredicate(
				lhs,
				predicate.isNegated(),
				createMemberOfSubQuery( pluralPath, mappingModelExpressable )
		);
	}

	private QuerySpec createMemberOfSubQuery(SqmPath<?> pluralPath, PluralAttributeMapping mappingModelExpressable) {
		final QuerySpec querySpec = new QuerySpec( true );
		processingStateStack.push(
				new SqlAstQuerySpecProcessingStateImpl(
						querySpec,
						processingStateStack.getCurrent(),
						this,
						currentClauseStack::getCurrent
				)
		);
		try {

			final TableGroup rootTableGroup = mappingModelExpressable.createRootTableGroup(
					pluralPath.getNavigablePath(),
					null,
					true,
					LockOptions.NONE.getLockMode(),
					sqlAliasBaseManager,
					getSqlExpressionResolver(),
					() -> querySpec::applyPredicate,
					creationContext
			);

			fromClauseIndex.registerTableGroup( pluralPath.getNavigablePath(), rootTableGroup );

			querySpec.getFromClause().addRoot( rootTableGroup );

			final CollectionPart elementDescriptor = mappingModelExpressable.getElementDescriptor();

			elementDescriptor.createDomainResult(
					pluralPath.getNavigablePath(),
					rootTableGroup,
					null,
					this
			);

			final Predicate predicate = mappingModelExpressable.getKeyDescriptor().generateJoinPredicate(
					getFromClauseAccess().findTableGroup( pluralPath.getNavigablePath().getParent() ),
					rootTableGroup,
					null,
					getSqlExpressionResolver(),
					creationContext
			);
			querySpec.applyPredicate( predicate );
		}
		finally {
			processingStateStack.pop();
		}

		return querySpec;
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

	@Override
	public Object visitIsEmptyPredicate(SqmEmptinessPredicate predicate) {
		final QuerySpec subQuerySpec = new QuerySpec( false, 1 );

		final SqlAstProcessingState parentState = getProcessingStateStack().getCurrent();

		final SqlAstProcessingStateImpl subQueryState = new SqlAstProcessingStateImpl(
				parentState,
				this,
				currentClauseStack::getCurrent
		);

		getProcessingStateStack().push( subQueryState );

		final SqmPluralValuedSimplePath<?> sqmPluralPath = predicate.getPluralPath();

		final NavigablePath pluralPathNavPath = sqmPluralPath.getNavigablePath();
		final NavigablePath parentNavPath = pluralPathNavPath.getParent();
		assert parentNavPath != null;

		final TableGroup parentTableGroup = parentState
				.getSqlAstCreationState()
				.getFromClauseAccess()
				.getTableGroup( parentNavPath );

		subQueryState.getSqlAstCreationState().getFromClauseAccess().registerTableGroup( parentNavPath, parentTableGroup );

		final SqmPathInterpretation<?> sqmPathInterpretation = visitPluralValuedPath( sqmPluralPath );


		final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) sqmPathInterpretation.getExpressionType();

		// note : do not add to `parentTableGroup` as a join

		final TableGroupJoin tableGroupJoin = pluralAttributeMapping.createTableGroupJoin(
				pluralPathNavPath,
				parentTableGroup,
				sqmPluralPath.getExplicitAlias(),
				SqlAstJoinType.LEFT,
				LockMode.NONE,
				sqlAliasBaseManager,
				subQueryState,
				creationContext
		);

		final TableGroup collectionTableGroup = tableGroupJoin.getJoinedGroup();

		subQuerySpec.getFromClause().addRoot( collectionTableGroup );
		subQuerySpec.applyPredicate( tableGroupJoin.getPredicate() );

		final ForeignKeyDescriptor collectionKeyDescriptor = pluralAttributeMapping.getKeyDescriptor();
		final int jdbcTypeCount = collectionKeyDescriptor.getJdbcTypeCount( sessionFactory.getTypeConfiguration() );
		assert jdbcTypeCount > 0;

		final JdbcLiteral<Integer> jdbcLiteral = new JdbcLiteral<>( 1, StandardBasicTypes.INTEGER );
		subQuerySpec.getSelectClause().addSqlSelection(
				new SqlSelectionImpl(1,0, jdbcLiteral )
		);

		return new ExistsPredicate( subQuerySpec );
	}

	@Override
	public BetweenPredicate visitBetweenPredicate(SqmBetweenPredicate predicate) {
		final Expression expression;
		final Expression lowerBound;
		final Expression upperBound;

		inferableTypeAccessStack.push(
				() -> coalesceSuppliedValues(
						() -> determineValueMapping( predicate.getLowerBound() ),
						() -> determineValueMapping( predicate.getUpperBound() )
				)
		);

		try {
			expression = (Expression) predicate.getExpression().accept( this );
		}
		finally {
			inferableTypeAccessStack.pop();
		}

		inferableTypeAccessStack.push(
				() -> coalesceSuppliedValues(
						() -> determineValueMapping( predicate.getExpression() ),
						() -> determineValueMapping( predicate.getUpperBound() )
				)
		);
		try {
			lowerBound = (Expression) predicate.getLowerBound().accept( this );
		}
		finally {
			inferableTypeAccessStack.pop();
		}

		inferableTypeAccessStack.push(
				() -> coalesceSuppliedValues(
						() -> determineValueMapping( predicate.getExpression() ),
						() -> determineValueMapping( predicate.getLowerBound() )
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

	@Override
	public Object visitExistsPredicate(SqmExistsPredicate predicate) {
		return new ExistsPredicate( (QuerySpec) predicate.getExpression().accept( this ) );
	}

	@Override
	public SqlAstCreationState getSqlAstCreationState() {
		return this;
	}

	@Override
	public List<Fetch> visitFetches(FetchParent fetchParent) {
		return Collections.emptyList();
	}
}
