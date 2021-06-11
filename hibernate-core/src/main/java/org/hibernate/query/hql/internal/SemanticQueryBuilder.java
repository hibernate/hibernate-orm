/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.QueryException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.grammars.hql.HqlLexer;
import org.hibernate.grammars.hql.HqlParser;
import org.hibernate.grammars.hql.HqlParserBaseVisitor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.query.BinaryArithmeticOperator;
import org.hibernate.query.ComparisonOperator;
import org.hibernate.query.FetchClauseType;
import org.hibernate.query.NullPrecedence;
import org.hibernate.query.PathException;
import org.hibernate.query.SemanticException;
import org.hibernate.query.SetOperator;
import org.hibernate.query.SortOrder;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.TrimSpec;
import org.hibernate.query.UnaryArithmeticOperator;
import org.hibernate.query.hql.HqlLogging;
import org.hibernate.query.hql.spi.DotIdentifierConsumer;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmCreationOptions;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.hql.spi.SqmPathRegistry;
import org.hibernate.query.sqm.LiteralNumberFormatException;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.SqmTreeCreationLogger;
import org.hibernate.query.sqm.StrictJpaComplianceViolation;
import org.hibernate.query.sqm.UnknownEntityException;
import org.hibernate.query.sqm.function.NamedSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.internal.ParameterCollector;
import org.hibernate.query.sqm.internal.SqmCreationProcessingStateImpl;
import org.hibernate.query.sqm.internal.SqmDmlCreationProcessingState;
import org.hibernate.query.sqm.internal.SqmQuerySpecCreationProcessingStateStandardImpl;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.spi.ParameterDeclarationContext;
import org.hibernate.query.sqm.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.SqmQuery;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.AbstractSqmFrom;
import org.hibernate.query.sqm.tree.domain.SqmCorrelation;
import org.hibernate.query.sqm.tree.domain.SqmIndexedCollectionAccessPath;
import org.hibernate.query.sqm.tree.domain.SqmListJoin;
import org.hibernate.query.sqm.tree.domain.SqmMapEntryReference;
import org.hibernate.query.sqm.tree.domain.SqmMapJoin;
import org.hibernate.query.sqm.tree.domain.SqmMaxElementPath;
import org.hibernate.query.sqm.tree.domain.SqmMaxIndexPath;
import org.hibernate.query.sqm.tree.domain.SqmMinElementPath;
import org.hibernate.query.sqm.tree.domain.SqmMinIndexPath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPolymorphicRootDescriptor;
import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;
import org.hibernate.query.sqm.tree.expression.SqmAliasedNodeRef;
import org.hibernate.query.sqm.tree.expression.SqmAny;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmByUnit;
import org.hibernate.query.sqm.tree.expression.SqmCaseSearched;
import org.hibernate.query.sqm.tree.expression.SqmCaseSimple;
import org.hibernate.query.sqm.tree.expression.SqmCastTarget;
import org.hibernate.query.sqm.tree.expression.SqmCollate;
import org.hibernate.query.sqm.tree.expression.SqmCollectionSize;
import org.hibernate.query.sqm.tree.expression.SqmDistinct;
import org.hibernate.query.sqm.tree.expression.SqmDurationUnit;
import org.hibernate.query.sqm.tree.expression.SqmEvery;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmExtractUnit;
import org.hibernate.query.sqm.tree.expression.SqmFormat;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.SqmLiteralNull;
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.expression.SqmParameterizedEntityType;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;
import org.hibernate.query.sqm.tree.expression.SqmStar;
import org.hibernate.query.sqm.tree.expression.SqmSummarization;
import org.hibernate.query.sqm.tree.expression.SqmToDuration;
import org.hibernate.query.sqm.tree.expression.SqmTrimSpecification;
import org.hibernate.query.sqm.tree.expression.SqmTuple;
import org.hibernate.query.sqm.tree.expression.SqmUnaryOperation;
import org.hibernate.query.sqm.tree.from.DowncastLocation;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.from.SqmQualifiedJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertValuesStatement;
import org.hibernate.query.sqm.tree.insert.SqmValues;
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
import org.hibernate.query.sqm.tree.predicate.SqmNegatablePredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNegatedPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNullnessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmOrPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.select.AbstractSqmSelectQuery;
import org.hibernate.query.sqm.tree.select.SqmAliasedNode;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationArgument;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.query.sqm.tree.select.SqmQueryGroup;
import org.hibernate.query.sqm.tree.select.SqmQueryPart;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.select.SqmSelectQuery;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

import org.jboss.logging.Logger;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hibernate.grammars.hql.HqlParser.EXCEPT;
import static org.hibernate.grammars.hql.HqlParser.IDENTIFIER;
import static org.hibernate.grammars.hql.HqlParser.INTERSECT;
import static org.hibernate.grammars.hql.HqlParser.UNION;
import static org.hibernate.query.TemporalUnit.DATE;
import static org.hibernate.query.TemporalUnit.DAY_OF_MONTH;
import static org.hibernate.query.TemporalUnit.DAY_OF_WEEK;
import static org.hibernate.query.TemporalUnit.DAY_OF_YEAR;
import static org.hibernate.query.TemporalUnit.NANOSECOND;
import static org.hibernate.query.TemporalUnit.OFFSET;
import static org.hibernate.query.TemporalUnit.TIME;
import static org.hibernate.query.TemporalUnit.TIMEZONE_HOUR;
import static org.hibernate.query.TemporalUnit.TIMEZONE_MINUTE;
import static org.hibernate.query.TemporalUnit.WEEK_OF_MONTH;
import static org.hibernate.query.TemporalUnit.WEEK_OF_YEAR;
import static org.hibernate.type.descriptor.DateTimeUtils.DATE_TIME;
import static org.hibernate.type.spi.TypeConfiguration.isJdbcTemporalType;

/**
 * Responsible for producing an SQM using visitation over an HQL parse tree generated by
 * Antlr via {@link HqlParseTreeBuilder}.
 *
 * @author Steve Ebersole
 */
public class SemanticQueryBuilder<R> extends HqlParserBaseVisitor<Object> implements SqmCreationState {

	private static final Logger log = Logger.getLogger( SemanticQueryBuilder.class );

	/**
	 * Main entry point into analysis of HQL/JPQL parse tree - producing a semantic model of the
	 * query.
	 */
	@SuppressWarnings("WeakerAccess")
	public static <R> SqmStatement<R> buildSemanticModel(
			HqlParser.StatementContext hqlParseTree,
			SqmCreationOptions creationOptions,
			SqmCreationContext creationContext) {
		return new SemanticQueryBuilder<R>( creationOptions, creationContext ).visitStatement( hqlParseTree );
	}

	private final SqmCreationOptions creationOptions;
	private final SqmCreationContext creationContext;

	private final Stack<DotIdentifierConsumer> dotIdentifierConsumerStack;

	private final Stack<TreatHandler> treatHandlerStack = new StandardStack<>( new TreatHandlerNormal() );

	private final Stack<ParameterDeclarationContext> parameterDeclarationContextStack = new StandardStack<>();
	private final Stack<SqmCreationProcessingState> processingStateStack = new StandardStack<>();

	private final BasicDomainType<Integer> integerDomainType;
	private final JavaTypeDescriptor<List<?>> listJavaTypeDescriptor;
	private final JavaTypeDescriptor<Map<?,?>> mapJavaTypeDescriptor;

	private ParameterCollector parameterCollector;

	@SuppressWarnings("WeakerAccess")
	public SemanticQueryBuilder(SqmCreationOptions creationOptions, SqmCreationContext creationContext) {
		this.creationOptions = creationOptions;
		this.creationContext = creationContext;
		this.dotIdentifierConsumerStack = new StandardStack<>( new BasicDotIdentifierConsumer( this ) );

		this.integerDomainType = creationContext
				.getNodeBuilder()
				.getTypeConfiguration()
				.standardBasicTypeForJavaType( Integer.class );
		this.listJavaTypeDescriptor = creationContext
				.getNodeBuilder()
				.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.resolveDescriptor( List.class );
		this.mapJavaTypeDescriptor = creationContext
				.getNodeBuilder()
				.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.resolveDescriptor( Map.class );
	}

	@Override
	public SqmCreationContext getCreationContext() {
		return creationContext;
	}

	@Override
	public SqmCreationOptions getCreationOptions() {
		return creationOptions;
	}

	public Stack<SqmCreationProcessingState> getProcessingStateStack() {
		return processingStateStack;
	}

	protected Stack<ParameterDeclarationContext> getParameterDeclarationContextStack() {
		return parameterDeclarationContextStack;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Grammar rules

	@Override
	public SqmStatement<R> visitStatement(HqlParser.StatementContext ctx) {
		// parameters allow multi-valued bindings only in very limited cases, so for
		// the base case here we say false
		parameterDeclarationContextStack.push( () -> false );

		try {
			if ( ctx.selectStatement() != null ) {
				final SqmSelectStatement<R> selectStatement = visitSelectStatement( ctx.selectStatement() );
				selectStatement.getQueryPart().validateQueryGroupFetchStructure();
				return selectStatement;
			}
			else if ( ctx.insertStatement() != null ) {
				return visitInsertStatement( ctx.insertStatement() );
			}
			else if ( ctx.updateStatement() != null ) {
				return visitUpdateStatement( ctx.updateStatement() );
			}
			else if ( ctx.deleteStatement() != null ) {
				return visitDeleteStatement( ctx.deleteStatement() );
			}
		}
		finally {
			parameterDeclarationContextStack.pop();
		}

		throw new ParsingException( "Unexpected statement type [not INSERT, UPDATE, DELETE or SELECT] : " + ctx.getText() );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Top-level statements

	@Override
	public SqmSelectStatement<R> visitSelectStatement(HqlParser.SelectStatementContext ctx) {
		final HqlParser.QueryExpressionContext queryExpressionContext = ctx.queryExpression();
		final SqmSelectStatement<R> selectStatement = new SqmSelectStatement<>( creationContext.getNodeBuilder() );

		parameterCollector = selectStatement;

		processingStateStack.push(
				new SqmQuerySpecCreationProcessingStateStandardImpl(
						processingStateStack.getCurrent(),
						selectStatement,
						this
				)
		);

		try {
			queryExpressionContext.accept( this );
		}
		finally {
			processingStateStack.pop();
		}

		return selectStatement;
	}

	@Override
	public SqmInsertStatement<R> visitInsertStatement(HqlParser.InsertStatementContext ctx) {
		final SqmRoot<R> root = new SqmRoot<>(
				(EntityDomainType<R>) visitEntityName( ctx.dmlTarget().entityName() ),
				applyJpaCompliance( visitIdentificationVariableDef( ctx.dmlTarget().identificationVariableDef() ) ),
				creationContext.getNodeBuilder()
		);

		final HqlParser.QueryExpressionContext queryExpressionContext = ctx.queryExpression();
		if ( queryExpressionContext != null ) {
			final SqmInsertSelectStatement<R> insertStatement = new SqmInsertSelectStatement<>( root, creationContext.getNodeBuilder() );
			parameterCollector = insertStatement;
			final SqmDmlCreationProcessingState processingState = new SqmDmlCreationProcessingState(
					insertStatement,
					this
			);

			processingStateStack.push( processingState );

			try {
				queryExpressionContext.accept( this );

				final SqmCreationProcessingState stateFieldsProcessingState = new SqmCreationProcessingStateImpl(
						insertStatement,
						this
				);
				stateFieldsProcessingState.getPathRegistry().register( root );

				processingStateStack.push( stateFieldsProcessingState );
				try {
					for ( HqlParser.DotIdentifierSequenceContext stateFieldCtx : ctx.targetFieldsSpec().dotIdentifierSequence() ) {
						final SqmPath<?> stateField = (SqmPath<?>) visitDotIdentifierSequence( stateFieldCtx );
						// todo : validate each resolved stateField...
						insertStatement.addInsertTargetStateField( stateField );
					}
				}
				finally {
					processingStateStack.pop();
				}

				return insertStatement;
			}
			finally {
				processingStateStack.pop();
			}

		}
		else {
			final SqmInsertValuesStatement<R> insertStatement = new SqmInsertValuesStatement<>( root, creationContext.getNodeBuilder() );
			parameterCollector = insertStatement;
			final SqmDmlCreationProcessingState processingState = new SqmDmlCreationProcessingState(
					insertStatement,
					this
			);

			processingStateStack.push( processingState );
			processingState.getPathRegistry().register( root );

			try {
				for ( HqlParser.ValuesContext values : ctx.valuesList().values() ) {
					SqmValues sqmValues = new SqmValues();
					for ( HqlParser.ExpressionContext expressionContext : values.expression() ) {
						sqmValues.getExpressions().add( (SqmExpression<?>) expressionContext.accept( this ) );
					}
					insertStatement.getValuesList().add( sqmValues );
				}

				for ( HqlParser.DotIdentifierSequenceContext stateFieldCtx : ctx.targetFieldsSpec().dotIdentifierSequence() ) {
					final SqmPath stateField = (SqmPath) visitDotIdentifierSequence( stateFieldCtx );
					// todo : validate each resolved stateField...
					insertStatement.addInsertTargetStateField( stateField );
				}

				return insertStatement;
			}
			finally {
				processingStateStack.pop();
			}

		}
	}

	@Override
	public SqmUpdateStatement<R> visitUpdateStatement(HqlParser.UpdateStatementContext ctx) {
		final SqmRoot<R> root = new SqmRoot<>(
				(EntityDomainType<R>) visitEntityName( ctx.dmlTarget().entityName() ),
				visitIdentificationVariableDef( ctx.dmlTarget().identificationVariableDef() ),
				creationContext.getNodeBuilder()
		);

		final SqmUpdateStatement<R> updateStatement = new SqmUpdateStatement<>( root, creationContext.getNodeBuilder() );
		parameterCollector = updateStatement;
		final SqmDmlCreationProcessingState processingState = new SqmDmlCreationProcessingState(
				updateStatement,
				this
		);
		processingStateStack.push( processingState );
		processingState.getPathRegistry().register( root );

		try {
			updateStatement.versioned( ctx.VERSIONED() != null );
			for ( HqlParser.AssignmentContext assignmentContext : ctx.setClause().assignment() ) {
				updateStatement.applyAssignment(
						consumeDomainPath( assignmentContext.dotIdentifierSequence() ),
						(SqmExpression<?>) assignmentContext.expression().accept( this )
				);
			}

			updateStatement.applyPredicate( visitWhereClause( ctx.whereClause() ) );

			return updateStatement;
		}
		finally {
			processingStateStack.pop();
		}
	}

	@Override
	public SqmDeleteStatement<R> visitDeleteStatement(HqlParser.DeleteStatementContext ctx) {
		final SqmRoot<R> root = new SqmRoot<>(
				(EntityDomainType<R>) visitEntityName( ctx.dmlTarget().entityName() ),
				visitIdentificationVariableDef( ctx.dmlTarget().identificationVariableDef() ),
				creationContext.getNodeBuilder()
		);

		final SqmDeleteStatement<R> deleteStatement = new SqmDeleteStatement<>( root, SqmQuerySource.HQL, creationContext.getNodeBuilder() );

		parameterCollector = deleteStatement;

		final SqmDmlCreationProcessingState sqmDeleteCreationState = new SqmDmlCreationProcessingState(
				deleteStatement,
				this
		);

		sqmDeleteCreationState.getPathRegistry().register( root );

		processingStateStack.push( sqmDeleteCreationState );
		try {
			if ( ctx.whereClause() != null && ctx.whereClause().predicate() != null ) {
				deleteStatement.applyPredicate( (SqmPredicate) ctx.whereClause().predicate().accept( this ) );
			}

			return deleteStatement;
		}
		finally {
			processingStateStack.pop();
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Query spec

	@Override
	public SqmQueryPart<Object> visitSimpleQueryGroup(HqlParser.SimpleQueryGroupContext ctx) {
		//noinspection unchecked
		return (SqmQueryPart<Object>) ctx.simpleQueryExpression().accept( this );
	}

	@Override
	public SqmQueryPart<Object> visitQuerySpecExpression(HqlParser.QuerySpecExpressionContext ctx) {
		final List<ParseTree> children = ctx.children;
		final SqmQueryPart<Object> queryPart = visitQuerySpec( (HqlParser.QuerySpecContext) children.get( 0 ) );
		if ( children.size() > 1 ) {
			visitQueryOrder( queryPart, (HqlParser.QueryOrderContext) children.get( 1 ) );
		}
		return queryPart;
	}

	@Override
	public SqmQueryPart<Object> visitNestedQueryExpression(HqlParser.NestedQueryExpressionContext ctx) {
		final List<ParseTree> children = ctx.children;
		//noinspection unchecked
		final SqmQueryPart<Object> queryPart = (SqmQueryPart<Object>) children.get( 1 ).accept( this );
		if ( children.size() > 3 ) {
			visitQueryOrder( queryPart, (HqlParser.QueryOrderContext) children.get( 3 ) );
		}
		return queryPart;
	}

	@Override
	public SqmQueryGroup<Object> visitSetQueryGroup(HqlParser.SetQueryGroupContext ctx) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw new StrictJpaComplianceViolation(
					StrictJpaComplianceViolation.Type.SET_OPERATIONS
			);
		}
		final List<ParseTree> children = ctx.children;
		//noinspection unchecked
		final SqmQueryPart<Object> firstQueryPart = (SqmQueryPart<Object>) children.get( 0 ).accept( this );
		SqmQueryGroup<Object> queryGroup;
		if ( firstQueryPart instanceof SqmQueryGroup<?>) {
			queryGroup = (SqmQueryGroup<Object>) firstQueryPart;
		}
		else {
			queryGroup = new SqmQueryGroup<>( firstQueryPart );
		}
		setCurrentQueryPart( queryGroup );
		final List<SqmSelection> firstSelections = firstQueryPart.getFirstQuerySpec().getSelectClause().getSelections();
		final int firstSelectionSize = firstSelections.size();
		final int size = children.size();
		final SqmCreationProcessingState firstProcessingState = processingStateStack.pop();
		for ( int i = 1; i < size; i += 2 ) {
			final SetOperator operator = visitSetOperator( (HqlParser.SetOperatorContext) children.get( i ) );
			final HqlParser.SimpleQueryExpressionContext simpleQueryCtx =
					(HqlParser.SimpleQueryExpressionContext) children.get( i + 1 );
			final List<SqmQueryPart<Object>> queryParts;
			if ( queryGroup.getSetOperator() == null || queryGroup.getSetOperator() == operator ) {
				queryGroup.setSetOperator( operator );
				queryParts = queryGroup.queryParts();
			}
			else {
				queryParts = new ArrayList<>( size - ( i >> 1 ) );
				queryParts.add( queryGroup );
				queryGroup = new SqmQueryGroup<>(
						creationContext.getNodeBuilder(),
						operator,
						queryParts
				);
				setCurrentQueryPart( queryGroup );
			}

			final SqmQueryPart<Object> queryPart;
			try {
				processingStateStack.push(
						new SqmQuerySpecCreationProcessingStateStandardImpl(
								processingStateStack.getCurrent(),
								(SqmSelectQuery<?>) firstProcessingState.getProcessingQuery(),
								this
						)
				);
				final List<ParseTree> subChildren = simpleQueryCtx.children;
				if ( subChildren.get( 0 ) instanceof HqlParser.QuerySpecContext ) {
					final SqmQuerySpec<Object> querySpec = new SqmQuerySpec<>( creationContext.getNodeBuilder() );
					queryParts.add( querySpec );
					queryPart = visitQuerySpecExpression( (HqlParser.QuerySpecExpressionContext) simpleQueryCtx );
				}
				else {
					try {
						final SqmSelectStatement<Object> selectStatement = new SqmSelectStatement<>( creationContext.getNodeBuilder() );
						processingStateStack.push(
								new SqmQuerySpecCreationProcessingStateStandardImpl(
										processingStateStack.getCurrent(),
										selectStatement,
										this
								)
						);
						queryPart = visitNestedQueryExpression( (HqlParser.NestedQueryExpressionContext) simpleQueryCtx );
						queryParts.add( queryPart );
					}
					finally {
						processingStateStack.pop();
					}
				}
			}
			finally {
				processingStateStack.pop();
			}
		}
		processingStateStack.push( firstProcessingState );

		return queryGroup;
	}

	@Override
	public SetOperator visitSetOperator(HqlParser.SetOperatorContext ctx) {
		final Token token = ( (TerminalNode) ctx.getChild( 0 ) ).getSymbol();
		final boolean all = ctx.getChildCount() == 2;
		switch ( token.getType() ) {
			case UNION:
				return all ? SetOperator.UNION_ALL : SetOperator.UNION;
			case INTERSECT:
				return all ? SetOperator.INTERSECT_ALL : SetOperator.INTERSECT;
			case EXCEPT:
				return all ? SetOperator.EXCEPT_ALL : SetOperator.EXCEPT;
		}
		throw new SemanticException( "Illegal set operator token: " + token.getText() );
	}

	protected void visitQueryOrder(SqmQueryPart<?> sqmQueryPart, HqlParser.QueryOrderContext ctx) {
		if ( ctx == null ) {
			return;
		}
		final SqmOrderByClause orderByClause;
		final HqlParser.OrderByClauseContext orderByClauseContext = ctx.orderByClause();
		if ( orderByClauseContext != null ) {
			if ( creationOptions.useStrictJpaCompliance() && processingStateStack.depth() > 1 ) {
				throw new StrictJpaComplianceViolation(
						StrictJpaComplianceViolation.Type.SUBQUERY_ORDER_BY
				);
			}

			orderByClause = visitOrderByClause( orderByClauseContext );
			sqmQueryPart.setOrderByClause( orderByClause );
		}
		else {
			orderByClause = null;
		}

		final HqlParser.LimitClauseContext limitClauseContext = ctx.limitClause();
		final HqlParser.OffsetClauseContext offsetClauseContext = ctx.offsetClause();
		final HqlParser.FetchClauseContext fetchClauseContext = ctx.fetchClause();
		if ( limitClauseContext != null || offsetClauseContext != null || fetchClauseContext != null ) {
			if ( getCreationOptions().useStrictJpaCompliance() ) {
				throw new StrictJpaComplianceViolation(
						StrictJpaComplianceViolation.Type.LIMIT_OFFSET_CLAUSE
				);
			}

			if ( processingStateStack.depth() > 1 && orderByClause == null ) {
				throw new SemanticException(
						"limit, offset and fetch clause require an order-by clause when used in sub-query"
				);
			}

			sqmQueryPart.setOffsetExpression( visitOffsetClause( offsetClauseContext ) );
			if ( limitClauseContext == null ) {
				sqmQueryPart.setFetchExpression( visitFetchClause( fetchClauseContext ), visitFetchClauseType( fetchClauseContext ) );
			}
			else if ( fetchClauseContext == null ) {
				sqmQueryPart.setFetchExpression( visitLimitClause( limitClauseContext ) );
			}
			else {
				throw new SemanticException("Can't use both, limit and fetch clause!" );
			}
		}
	}

	@Override
	public SqmQuerySpec<Object> visitQuerySpec(HqlParser.QuerySpecContext ctx) {
		//noinspection unchecked
		final SqmQuerySpec<Object> sqmQuerySpec = (SqmQuerySpec<Object>) currentQuerySpec();

		// visit from-clause first!!!
		treatHandlerStack.push( new TreatHandlerFromClause() );
		try {
			sqmQuerySpec.setFromClause( visitFromClause( ctx.fromClause() ) );
		}
		finally {
			treatHandlerStack.pop();
		}

		final HqlParser.SelectClauseContext selectClauseContext = ctx.selectClause();
		final SqmSelectClause selectClause;
		if ( selectClauseContext != null ) {
			selectClause = visitSelectClause( selectClauseContext );
		}
		else {
			if ( creationOptions.useStrictJpaCompliance() ) {
				throw new StrictJpaComplianceViolation(
						"Encountered implicit select-clause, but strict JPQL compliance was requested",
						StrictJpaComplianceViolation.Type.IMPLICIT_SELECT
				);
			}
			log.debugf( "Encountered implicit select clause : %s", ctx.getText() );
			selectClause = buildInferredSelectClause( sqmQuerySpec.getFromClause() );
		}
		sqmQuerySpec.setSelectClause( selectClause );

		final SqmWhereClause whereClause = new SqmWhereClause( creationContext.getNodeBuilder() );
		if ( ctx.whereClause() != null ) {
			treatHandlerStack.push( new TreatHandlerNormal( DowncastLocation.WHERE ) );
			try {
				whereClause.setPredicate( (SqmPredicate) ctx.whereClause().accept( this ) );
			}
			finally {
				treatHandlerStack.pop();
			}
		}
		sqmQuerySpec.setWhereClause( whereClause );

		final HqlParser.GroupByClauseContext groupByClauseContext = ctx.groupByClause();
		if ( groupByClauseContext != null ) {
			sqmQuerySpec.setGroupByClauseExpressions( visitGroupByClause( groupByClauseContext ) );
		}
		final HqlParser.HavingClauseContext havingClauseContext = ctx.havingClause();
		if ( havingClauseContext != null ) {
			sqmQuerySpec.setHavingClausePredicate( visitHavingClause( havingClauseContext ) );
		}

		return sqmQuerySpec;
	}

	@SuppressWarnings("WeakerAccess")
	protected SqmSelectClause buildInferredSelectClause(SqmFromClause fromClause) {
		// for now, this is slightly different than the legacy behavior where
		// the root and each non-fetched-join was selected.  For now, here, we simply
		// select the root
		final SqmSelectClause selectClause = new SqmSelectClause(
				false,
				fromClause.getNumberOfRoots(),
				creationContext.getNodeBuilder()
		);

		//noinspection unchecked
		fromClause.visitRoots(
				sqmRoot -> selectClause.addSelection(
						new SqmSelection( sqmRoot, sqmRoot.getExplicitAlias(), creationContext.getNodeBuilder() )
				)
		);
		return selectClause;
	}

	@Override
	public SqmSelectClause visitSelectClause(HqlParser.SelectClauseContext ctx) {
		treatHandlerStack.push( new TreatHandlerNormal( DowncastLocation.SELECT ) );

		// todo (6.0) : primer a select-clause-specific SemanticPathPart into the stack

		try {
			final SqmSelectClause selectClause = new SqmSelectClause(
					ctx.DISTINCT() != null,
					creationContext.getNodeBuilder()
			);
			for ( HqlParser.SelectionContext selectionContext : ctx.selectionList().selection() ) {
				selectClause.addSelection( visitSelection( selectionContext ) );
			}
			return selectClause;
		}
		finally {
			treatHandlerStack.pop();
		}
	}

	@Override
	public SqmSelection visitSelection(HqlParser.SelectionContext ctx) {
		final String resultIdentifier = applyJpaCompliance( visitResultIdentifier( ctx.resultIdentifier() ) );
		final SqmSelectableNode selectableNode = visitSelectableNode( ctx );

		//noinspection unchecked
		final SqmSelection<?> selection = new SqmSelection<>(
				selectableNode,
				// NOTE : SqmSelection forces the alias down to its selectableNode.
				//		- no need to do that here
				resultIdentifier,
				creationContext.getNodeBuilder()
		);

		// if the node is not a dynamic-instantiation, register it with
		// the path-registry
		//noinspection StatementWithEmptyBody
		if ( selectableNode instanceof SqmDynamicInstantiation ) {
			// nothing else to do (avoid kludgy `! ( instanceof )` syntax
		}
		else {
			getCurrentProcessingState().getPathRegistry().register( selection );
		}

		return selection;
	}

	private SqmSelectableNode<?> visitSelectableNode(HqlParser.SelectionContext ctx) {
		if ( ctx.selectExpression().dynamicInstantiation() != null ) {
			return visitDynamicInstantiation( ctx.selectExpression().dynamicInstantiation() );
		}

		if ( ctx.selectExpression().jpaSelectObjectSyntax() != null ) {
			return visitJpaSelectObjectSyntax( ctx.selectExpression().jpaSelectObjectSyntax() );
		}

		if ( ctx.selectExpression().mapEntrySelection() != null ) {
			return visitMapEntrySelection( ctx.selectExpression().mapEntrySelection() );
		}

		if ( ctx.selectExpression().expression() != null ) {
			final SqmExpression<?> sqmExpression = (SqmExpression<?>) ctx.selectExpression().expression().accept( this );
			if ( sqmExpression instanceof SqmPath ) {
				final SqmPath<?> sqmPath = (SqmPath<?>) sqmExpression;
				if ( sqmPath.getReferencedPathSource() instanceof PluralPersistentAttribute ) {
					// for plural-attribute selections, use the element path as the selection
					//		- this is not strictly JPA compliant
					if ( creationOptions.useStrictJpaCompliance() ) {
						SqmTreeCreationLogger.LOGGER.debugf(
								"Raw selection of plural attribute not supported by JPA: %s.  Use `value(%s)` or `key(%s)` to indicate what part of the collection to select",
								sqmPath.getAlias(),
								sqmPath.getAlias(),
								sqmPath.getAlias()
						);
					}

					final PluralPersistentAttribute<?,?,?> pluralAttribute = (PluralPersistentAttribute<?,?,?>) sqmPath.getReferencedPathSource();
					final SqmPath<?> elementPath = pluralAttribute.getElementPathSource().createSqmPath( sqmPath );
					processingStateStack.getCurrent().getPathRegistry().register( elementPath );
					return elementPath;
				}
			}

			return sqmExpression;
		}

		throw new ParsingException( "Unexpected selection rule type : " + ctx.getText() );
	}

	@Override
	public String visitResultIdentifier(HqlParser.ResultIdentifierContext resultIdentifierContext) {
		if ( resultIdentifierContext != null ) {
			if ( resultIdentifierContext.AS() != null ) {
				final Token aliasToken = resultIdentifierContext.identifier().getStart();
				final String explicitAlias = aliasToken.getText();

				if ( aliasToken.getType() != IDENTIFIER ) {
					// we have a reserved word used as an identification variable.
					if ( creationOptions.useStrictJpaCompliance() ) {
						throw new StrictJpaComplianceViolation(
								String.format(
										Locale.ROOT,
										"Strict JPQL compliance was violated : %s [%s]",
										StrictJpaComplianceViolation.Type.RESERVED_WORD_USED_AS_ALIAS.description(),
										explicitAlias
								),
								StrictJpaComplianceViolation.Type.RESERVED_WORD_USED_AS_ALIAS
						);
					}
				}
				return explicitAlias;
			}
			else {
				return  resultIdentifierContext.getText();
			}
		}

		return null;
	}

	@Override
	public SqmDynamicInstantiation<?> visitDynamicInstantiation(HqlParser.DynamicInstantiationContext ctx) {
		final SqmDynamicInstantiation<?> dynamicInstantiation;

		if ( ctx.dynamicInstantiationTarget().MAP() != null ) {
			dynamicInstantiation = SqmDynamicInstantiation.forMapInstantiation(
					mapJavaTypeDescriptor,
					creationContext.getNodeBuilder()
			);
		}
		else if ( ctx.dynamicInstantiationTarget().LIST() != null ) {
			dynamicInstantiation = SqmDynamicInstantiation.forListInstantiation(
					listJavaTypeDescriptor,
					creationContext.getNodeBuilder()
			);
		}
		else {
			final String className = ctx.dynamicInstantiationTarget().dotIdentifierSequence().getText();
			try {
				final JavaTypeDescriptor<?> jtd = resolveInstantiationTargetJtd( className );
				dynamicInstantiation = SqmDynamicInstantiation.forClassInstantiation(
						jtd,
						creationContext.getNodeBuilder()
				);
			}
			catch (ClassLoadingException e) {
				throw new SemanticException( "Unable to resolve class named for dynamic instantiation : " + className );
			}
		}

		for ( HqlParser.DynamicInstantiationArgContext arg : ctx.dynamicInstantiationArgs().dynamicInstantiationArg() ) {
			dynamicInstantiation.addArgument( visitDynamicInstantiationArg( arg ) );
		}

		return dynamicInstantiation;
	}

	private JavaTypeDescriptor<?> resolveInstantiationTargetJtd(String className) {
		final Class<?> targetJavaType = classForName( creationContext.getJpaMetamodel().qualifyImportableName( className ) );
		return creationContext.getJpaMetamodel()
				.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.resolveDescriptor( targetJavaType );
	}

	private Class<?> classForName(String className) {
		return creationContext.getServiceRegistry().getService( ClassLoaderService.class ).classForName( className );
	}

	@Override
	public SqmDynamicInstantiationArgument<?> visitDynamicInstantiationArg(HqlParser.DynamicInstantiationArgContext ctx) {
		final String alias = ctx.identifier() == null ? null : ctx.identifier().getText();

		final SqmSelectableNode<?> argExpression = visitDynamicInstantiationArgExpression( ctx.dynamicInstantiationArgExpression() );

		//noinspection unchecked
		final SqmDynamicInstantiationArgument<?> argument = new SqmDynamicInstantiationArgument<>(
				argExpression,
				alias,
				creationContext.getNodeBuilder()
		);

		//noinspection StatementWithEmptyBody
		if ( argExpression instanceof SqmDynamicInstantiation ) {
			// nothing else to do (avoid kludgy `! ( instanceof )` syntax
		}
		else {
			getCurrentProcessingState().getPathRegistry().register( argument );
		}

		return argument;
	}

	@Override
	public SqmSelectableNode visitDynamicInstantiationArgExpression(HqlParser.DynamicInstantiationArgExpressionContext ctx) {
		if ( ctx.dynamicInstantiation() != null ) {
			return visitDynamicInstantiation( ctx.dynamicInstantiation() );
		}
		else if ( ctx.expression() != null ) {
			return (SqmExpression) ctx.expression().accept( this );
		}

		throw new ParsingException( "Unexpected dynamic-instantiation-argument rule type : " + ctx.getText() );
	}

	@Override
	public SqmPath visitJpaSelectObjectSyntax(HqlParser.JpaSelectObjectSyntaxContext ctx) {
		final String alias = ctx.identifier().getText();
		final SqmFrom sqmFromByAlias = processingStateStack.getCurrent().getPathRegistry().findFromByAlias( alias );
		if ( sqmFromByAlias == null ) {
			throw new SemanticException( "Unable to resolve alias [" +  alias + "] in selection [" + ctx.getText() + "]" );
		}
		return sqmFromByAlias;
	}

	@Override
	public List<SqmExpression<?>> visitGroupByClause(HqlParser.GroupByClauseContext ctx) {
		return visitExpressions( ctx.groupByExpression() );
	}

	private SqmExpression<?> resolveOrderByOrGroupByExpression(
			Supplier<TerminalNode> positionLiteralAccess,
			Supplier<HqlParser.IdentifierContext> identifierAccess,
			Supplier<HqlParser.ExpressionContext> expressionContextAccess,
			boolean definedCollate) {
		if ( positionLiteralAccess.get() != null ) {
			if ( definedCollate ) {
				// This is syntactically disallowed
				throw new ParsingException( "COLLATE is not allowed for position based order-by or group-by items" );
			}

			final int position = Integer.parseInt( positionLiteralAccess.get().getText() );

			// make sure this selection exists
			final SqmAliasedNode<?> nodeByPosition = getCurrentProcessingState()
					.getPathRegistry()
					.findAliasedNodeByPosition( position );
			if ( nodeByPosition == null ) {
				throw new ParsingException( "Numeric literal `" + position + "` used in group-by does not match a registered select-item" );
			}

			return new SqmAliasedNodeRef( position, integerDomainType, creationContext.getNodeBuilder() );
		}

		final HqlParser.IdentifierContext identifier = identifierAccess.get();
		if ( identifier != null ) {
			final String identifierText = identifier.getText();

			final Integer correspondingPosition = getCurrentProcessingState()
					.getPathRegistry()
					.findAliasedNodePosition( identifierText );
			if ( correspondingPosition != null ) {
				if ( definedCollate ) {
					// This is syntactically disallowed
					throw new ParsingException( "COLLATE is not allowed for alias based order-by or group-by items" );
				}
				return new SqmAliasedNodeRef( correspondingPosition, integerDomainType, creationContext.getNodeBuilder() );
			}

			final SqmFrom<?, ?> sqmFrom = getCurrentProcessingState().getPathRegistry().findFromByAlias( identifierText );
			if ( sqmFrom != null ) {
				if ( definedCollate ) {
					// This is syntactically disallowed
					throw new ParsingException( "COLLATE is not allowed for alias based order-by or group-by items" );
				}
				// this will group-by all of the sub-parts in the from-element's model part
				return sqmFrom;
			}

			final DotIdentifierConsumer dotIdentifierConsumer = dotIdentifierConsumerStack.getCurrent();
			dotIdentifierConsumer.consumeIdentifier( identifierText, true, true );
			return (SqmExpression<?>) dotIdentifierConsumer.getConsumedPart();
		}

		return (SqmExpression<?>) expressionContextAccess.get().accept( this );
	}

	@Override
	public SqmExpression<?> visitGroupByExpression(HqlParser.GroupByExpressionContext ctx) {
		return resolveOrderByOrGroupByExpression(
				ctx::INTEGER_LITERAL,
				ctx::identifier,
				ctx::expression,
				ctx.collationSpecification() != null
		);
	}

	@Override
	public SqmPredicate visitHavingClause(HqlParser.HavingClauseContext ctx) {
		return (SqmPredicate) ctx.predicate().accept( this );
	}

	@Override
	public SqmOrderByClause visitOrderByClause(HqlParser.OrderByClauseContext ctx) {
		final SqmOrderByClause orderByClause = new SqmOrderByClause();
		for ( HqlParser.SortSpecificationContext sortSpecificationContext : ctx.sortSpecification() ) {
			orderByClause.addSortSpecification( visitSortSpecification( sortSpecificationContext ) );
		}
		return orderByClause;
	}

	@Override
	public SqmSortSpecification visitSortSpecification(HqlParser.SortSpecificationContext ctx) {
		final SqmExpression<?> sortExpression = visitSortExpression( ctx.sortExpression() );
		if ( sortExpression == null ) {
			throw new ParsingException( "Could not resolve sort-expression : " + ctx.sortExpression().getText() );
		}
		if ( sortExpression instanceof SqmLiteral
				|| sortExpression instanceof SqmParameter ) {
			HqlLogging.QUERY_LOGGER.debugf( "Questionable sorting by constant value : %s", sortExpression );
		}

		final SortOrder sortOrder;
		if ( ctx.orderingSpecification() != null ) {
			final String ordering = ctx.orderingSpecification().getText();
			try {
				sortOrder = interpretSortOrder( ordering );
			}
			catch (IllegalArgumentException e) {
				throw new SemanticException( "Unrecognized sort ordering: " + ordering, e );
			}
		}
		else {
			sortOrder = null;
		}

		final NullPrecedence nullPrecedence;
		if ( ctx.nullsPrecedence() != null ) {
			nullPrecedence = ctx.nullsPrecedence().FIRST() != null ? NullPrecedence.FIRST : NullPrecedence.LAST;
		}
		else {
			nullPrecedence = null;
		}

		return new SqmSortSpecification( sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public SqmExpression<?> visitSortExpression(HqlParser.SortExpressionContext ctx) {
		return resolveOrderByOrGroupByExpression(
				ctx::INTEGER_LITERAL,
				ctx::identifier,
				ctx::expression,
				ctx.collationSpecification() != null
		);
	}

	private SqmQuerySpec<?> currentQuerySpec() {
		SqmQuery<?> processingQuery = processingStateStack.getCurrent().getProcessingQuery();
		if ( processingQuery instanceof SqmInsertSelectStatement<?> ) {
			return ( (SqmInsertSelectStatement<?>) processingQuery ).getSelectQueryPart().getLastQuerySpec();
		}
		else {
			return ( (SqmSelectQuery<?>) processingQuery ).getQueryPart().getLastQuerySpec();
		}
	}

	private void setCurrentQueryPart(SqmQueryPart<?> queryPart) {
		SqmQuery<?> processingQuery = processingStateStack.getCurrent().getProcessingQuery();
		if ( processingQuery instanceof SqmInsertSelectStatement<?> ) {
			( (SqmInsertSelectStatement) processingQuery ).setSelectQueryPart( queryPart );
		}
		else {
			( (AbstractSqmSelectQuery) processingQuery ).setQueryPart( queryPart );
		}
	}

	private int getSelectionPosition(SqmSelection<?> selection) {
		return currentQuerySpec().getSelectClause().getSelections().indexOf( selection ) + 1;
	}

	@Override
	public SqmExpression<?> visitLimitClause(HqlParser.LimitClauseContext ctx) {
		if ( ctx == null ) {
			return null;
		}

		return (SqmExpression<?>) ctx.getChild( 1 ).accept( this );
	}

	@Override
	public SqmExpression<?> visitOffsetClause(HqlParser.OffsetClauseContext ctx) {
		if ( ctx == null ) {
			return null;
		}

		return (SqmExpression<?>) ctx.getChild( 1 ).accept( this );
	}

	@Override
	public SqmExpression<?> visitFetchClause(HqlParser.FetchClauseContext ctx) {
		if ( ctx == null ) {
			return null;
		}

		return (SqmExpression<?>) ctx.getChild( 2 ).accept( this );
	}

	private FetchClauseType visitFetchClauseType(HqlParser.FetchClauseContext ctx) {
		if ( ctx == null ) {
			return FetchClauseType.ROWS_ONLY;
		}
		if ( ctx.TIES() == null ) {
			return ctx.PERCENT() == null ? FetchClauseType.ROWS_ONLY : FetchClauseType.PERCENT_ONLY;
		}
		else {
			return ctx.PERCENT() == null ? FetchClauseType.ROWS_WITH_TIES : FetchClauseType.PERCENT_WITH_TIES;
		}
	}

	@Override
	public SqmExpression<?> visitPathExpression(HqlParser.PathExpressionContext ctx) {
		final HqlParser.PathContext path = ctx.path();
		final Object accept = path.accept( this );
		if ( accept instanceof DomainPathPart ) {
			return ( (DomainPathPart) accept ).getSqmExpression();
		}
		return (SqmExpression<?>) accept;
	}

	@Override
	public SqmExpression<?> visitFunctionExpression(HqlParser.FunctionExpressionContext ctx) {
		return (SqmExpression<?>) ctx.function().accept( this );
	}

	@Override
	public SqmExpression<?> visitParameterOrIntegerLiteral(HqlParser.ParameterOrIntegerLiteralContext ctx) {
		if ( ctx.INTEGER_LITERAL() != null ) {
			return integerLiteral( ctx.INTEGER_LITERAL().getText() );
		}
		if ( ctx.parameter() != null ) {
			return (SqmExpression<?>) ctx.parameter().accept( this );
		}

		return null;
	}

	@Override
	public SqmExpression<?> visitParameterOrNumberLiteral(HqlParser.ParameterOrNumberLiteralContext ctx) {
		if ( ctx.INTEGER_LITERAL() != null ) {
			return integerLiteral( ctx.INTEGER_LITERAL().getText() );
		}
		if ( ctx.FLOAT_LITERAL() != null ) {
			return floatLiteral( ctx.FLOAT_LITERAL().getText() );
		}
		if ( ctx.DOUBLE_LITERAL() != null ) {
			return doubleLiteral( ctx.DOUBLE_LITERAL().getText() );
		}
		if ( ctx.parameter() != null ) {
			return (SqmExpression<?>) ctx.parameter().accept( this );
		}

		return null;
	}

	private SortOrder interpretSortOrder(String value) {
		if ( value == null ) {
			return null;
		}

		if ( value.equalsIgnoreCase( "ascending" ) || value.equalsIgnoreCase( "asc" ) ) {
			return SortOrder.ASCENDING;
		}

		if ( value.equalsIgnoreCase( "descending" ) || value.equalsIgnoreCase( "desc" ) ) {
			return SortOrder.DESCENDING;
		}

		throw new SemanticException( "Unknown sort order : " + value );
	}

	@Override
	public EntityDomainType<?> visitEntityName(HqlParser.EntityNameContext parserEntityName) {
		final String entityName = parserEntityName.fullNameText;
		final EntityDomainType entityReference = resolveEntityReference( entityName );
		if ( entityReference == null ) {
			throw new UnknownEntityException( "Could not resolve entity name [" + entityName + "] as DML target", entityName );
		}
		checkFQNEntityNameJpaComplianceViolationIfNeeded( entityName, entityReference );
		return entityReference;
	}

	private EntityDomainType resolveEntityReference(String entityName) {
		log.debugf( "Attempting to resolve path [%s] as entity reference...", entityName );
		EntityDomainType reference = null;
		try {
			entityName = creationContext.getJpaMetamodel().qualifyImportableName( entityName );
			reference = creationContext.getJpaMetamodel().entity( entityName );
		}
		catch (Exception ignore) {
		}

		return reference;
	}


	@Override
	public SqmFromClause visitFromClause(HqlParser.FromClauseContext parserFromClause) {
		treatHandlerStack.push( new TreatHandlerFromClause() );

		try {
			final SqmFromClause fromClause;
			if ( parserFromClause == null ) {
				fromClause = new SqmFromClause();
			}
			else {
				final List<HqlParser.FromClauseSpaceContext> fromClauseSpaceContexts = parserFromClause.fromClauseSpace();
				fromClause = new SqmFromClause( fromClauseSpaceContexts.size() );
				for ( int i = 0; i < fromClauseSpaceContexts.size(); i++ ) {
					final SqmRoot sqmPathRoot = visitFromClauseSpace( fromClauseSpaceContexts.get( i ) );
					fromClause.addRoot( sqmPathRoot );
				}
			}
			return fromClause;
		}
		finally {
			treatHandlerStack.pop();
		}
	}


	@Override
	public SqmRoot visitFromClauseSpace(HqlParser.FromClauseSpaceContext parserSpace) {
		final SqmRoot sqmRoot = visitPathRoot( parserSpace.pathRoot() );

		for ( HqlParser.CrossJoinContext parserJoin : parserSpace.crossJoin() ) {
			consumeCrossJoin( parserJoin, sqmRoot );
		}

		for ( HqlParser.QualifiedJoinContext parserJoin : parserSpace.qualifiedJoin() ) {
			consumeQualifiedJoin( parserJoin, sqmRoot );
		}

		for ( HqlParser.JpaCollectionJoinContext parserJoin : parserSpace.jpaCollectionJoin() ) {
			consumeJpaCollectionJoin( parserJoin, sqmRoot );
		}

		return sqmRoot;
	}

	@Override
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public SqmRoot visitPathRoot(HqlParser.PathRootContext ctx) {
		final HqlParser.EntityNameContext entityNameContext = ctx.entityName();
		final List<ParseTree> entityNameParseTreeChildren = entityNameContext.children;
		final String name = entityNameContext.fullNameText;

		log.debugf( "Handling root path - %s", name );
		final EntityDomainType entityDescriptor = getCreationContext()
				.getJpaMetamodel()
				.getHqlEntityReference( name );

		final String alias = applyJpaCompliance(
				visitIdentificationVariableDef( ctx.identificationVariableDef() )
		);

		final SqmCreationProcessingState processingState = processingStateStack.getCurrent();
		final SqmPathRegistry pathRegistry = processingState.getPathRegistry();
		if ( entityDescriptor == null ) {
			final int size = entityNameParseTreeChildren.size();
			// Handle the use of a correlation path in subqueries
			if ( processingStateStack.depth() > 1 && size > 2 ) {
				final String parentAlias = entityNameParseTreeChildren.get( 0 ).getText();
				final AbstractSqmFrom<?, ?> correlationBasis = (AbstractSqmFrom<?, ?>) processingState.getParentProcessingState()
						.getPathRegistry()
						.findFromByAlias( parentAlias );
				if ( correlationBasis != null ) {
					final SqmCorrelation<?, ?> correlation = correlationBasis.createCorrelation(
					);
					pathRegistry.register( correlation );
					final DotIdentifierConsumer dotIdentifierConsumer = new QualifiedJoinPathConsumer(
							correlation,
							SqmJoinType.INNER,
							false,
							alias,
							this
					);
					final int lastIdx = size - 1;
					for ( int i = 2; i != lastIdx; i += 2 ) {
						dotIdentifierConsumer.consumeIdentifier(
								entityNameParseTreeChildren.get( i ).getText(),
								false,
								false
						);
					}
					dotIdentifierConsumer.consumeIdentifier(
							entityNameParseTreeChildren.get( lastIdx ).getText(),
							false,
							true
					);
					return correlation.getCorrelatedRoot();
				}
				throw new IllegalArgumentException( "Could not resolve entity reference or correlation path: " + name );
			}
			throw new IllegalArgumentException( "Could not resolve entity reference: " + name );
		}
		checkFQNEntityNameJpaComplianceViolationIfNeeded( name, entityDescriptor );

		if ( entityDescriptor instanceof SqmPolymorphicRootDescriptor ) {
			if ( getCreationOptions().useStrictJpaCompliance() ) {
				throw new StrictJpaComplianceViolation(
						"Encountered unmapped polymorphic reference [" + entityDescriptor.getHibernateEntityName()
								+ "], but strict JPQL compliance was requested",
						StrictJpaComplianceViolation.Type.UNMAPPED_POLYMORPHISM
				);
			}

			if ( processingStateStack.depth() > 1 ) {
				throw new SemanticException(
						"Illegal implicit-polymorphic domain path in sub-query : " + entityDescriptor.getName()
				);
			}
		}

		final SqmRoot sqmRoot = new SqmRoot( entityDescriptor, alias, creationContext.getNodeBuilder() );

		pathRegistry.register( sqmRoot );

		return sqmRoot;
	}

	@Override
	public String visitIdentificationVariableDef(HqlParser.IdentificationVariableDefContext ctx) {
		if ( ctx == null ) {
			return null;
		}

		if ( ctx.AS() != null && ctx.identifier() != null ) {
			// in this branch, the alias could be a reserved word ("keyword as identifier")
			// which JPA disallows...
			if ( getCreationOptions().useStrictJpaCompliance() ) {
				final Token identificationVariableToken = ctx.identifier().getStart();
				if ( identificationVariableToken.getType() != IDENTIFIER ) {
					throw new StrictJpaComplianceViolation(
							String.format(
									Locale.ROOT,
									"Strict JPQL compliance was violated : %s [%s]",
									StrictJpaComplianceViolation.Type.RESERVED_WORD_USED_AS_ALIAS.description(),
									identificationVariableToken.getText()
							),
							StrictJpaComplianceViolation.Type.RESERVED_WORD_USED_AS_ALIAS
					);
				}
			}
			return ctx.identifier().getText();
		}

		if ( ctx.IDENTIFIER() != null ) {
			return ctx.IDENTIFIER().getText();
		}

		return null;
	}

	private String applyJpaCompliance(String text) {
		if ( text == null ) {
			return null;
		}

		if ( getCreationOptions().useStrictJpaCompliance() ) {
			return text.toLowerCase(  Locale.getDefault() );
		}

		return text;
	}

	@Override
	public final SqmCrossJoin visitCrossJoin(HqlParser.CrossJoinContext ctx) {
		throw new UnsupportedOperationException( "Unexpected call to #visitCrossJoin, see #consumeCrossJoin" );
	}

	@SuppressWarnings("unchecked")
	private void consumeCrossJoin(HqlParser.CrossJoinContext parserJoin, SqmRoot sqmRoot) {
		final String name = parserJoin.pathRoot().entityName().fullNameText;

		SqmTreeCreationLogger.LOGGER.debugf( "Handling root path - %s", name );

		final EntityDomainType entityDescriptor = getCreationContext().getJpaMetamodel().resolveHqlEntityReference( name );

		if ( entityDescriptor instanceof SqmPolymorphicRootDescriptor ) {
			throw new SemanticException( "Unmapped polymorphic reference cannot be used as a CROSS JOIN target" );
		}

		final SqmCrossJoin join = new SqmCrossJoin(
				entityDescriptor,
				visitIdentificationVariableDef( parserJoin.pathRoot().identificationVariableDef() ),
				sqmRoot
		);

		processingStateStack.getCurrent().getPathRegistry().register( join );

		// CROSS joins are always added to the root
		sqmRoot.addSqmJoin( join );

	}

	@Override
	public final SqmQualifiedJoin visitQualifiedJoin(HqlParser.QualifiedJoinContext parserJoin) {
		throw new UnsupportedOperationException( "Unexpected call to #visitQualifiedJoin, see #consumeQualifiedJoin" );
	}

	@SuppressWarnings("WeakerAccess")
	protected void consumeQualifiedJoin(HqlParser.QualifiedJoinContext parserJoin, SqmRoot<?> sqmRoot) {
		final SqmJoinType joinType;
		final HqlParser.JoinTypeQualifierContext joinTypeQualifier = parserJoin.joinTypeQualifier();

		if ( joinTypeQualifier.FULL() != null ) {
			throw new SemanticException( "FULL OUTER joins are not yet supported : " + parserJoin.getText() );
		}
		else if ( joinTypeQualifier.RIGHT() != null ) {
			joinType = SqmJoinType.RIGHT;
		}
		else if ( joinTypeQualifier.OUTER() != null || joinTypeQualifier.LEFT() != null ) {
			joinType = SqmJoinType.LEFT;
		}
		else {
			joinType = SqmJoinType.INNER;
		}


		final String alias = visitIdentificationVariableDef( parserJoin.qualifiedJoinRhs().identificationVariableDef() );

		dotIdentifierConsumerStack.push(
				new QualifiedJoinPathConsumer(
						sqmRoot,
						joinType,
						parserJoin.FETCH() != null,
						alias,
						this
				)
		);

		try {
			//noinspection rawtypes
			final SqmQualifiedJoin join = (SqmQualifiedJoin) parserJoin.qualifiedJoinRhs().path().accept( this );

			// we need to set the alias here because the path could be treated - the treat operator is
			// not consumed by the identifierConsumer
			join.setExplicitAlias( alias );

			if ( join instanceof SqmEntityJoin ) {
				//noinspection unchecked
				sqmRoot.addSqmJoin( join );
			}
			else {
				if ( joinType == SqmJoinType.RIGHT ) {
					throw new SemanticException( "RIGHT OUTER attribute-joins are not supported : " + parserJoin.getText() );
				}

				if ( getCreationOptions().useStrictJpaCompliance() ) {
					if ( join.getExplicitAlias() != null ){
						//noinspection rawtypes
						if ( ( (SqmAttributeJoin) join ).isFetched() ) {
							throw new StrictJpaComplianceViolation(
									"Encountered aliased fetch join, but strict JPQL compliance was requested",
									StrictJpaComplianceViolation.Type.ALIASED_FETCH_JOIN
							);
						}
					}
				}
			}

			if ( parserJoin.qualifiedJoinPredicate() != null ) {
				dotIdentifierConsumerStack.push( new QualifiedJoinPredicatePathConsumer( join, this ) );
				try {
					join.setJoinPredicate( (SqmPredicate) parserJoin.qualifiedJoinPredicate().predicate().accept( this ) );
				}
				finally {
					dotIdentifierConsumerStack.pop();
				}
			}
		}
		finally {
			dotIdentifierConsumerStack.pop();
		}
	}

	@Override
	public SqmJoin visitJpaCollectionJoin(HqlParser.JpaCollectionJoinContext ctx) {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("WeakerAccess")
	protected void consumeJpaCollectionJoin(
			HqlParser.JpaCollectionJoinContext ctx,
			SqmRoot sqmRoot) {
		dotIdentifierConsumerStack.push(
				new QualifiedJoinPathConsumer(
						sqmRoot,
						// todo (6.0) : what kind of join is
						SqmJoinType.LEFT,
						false,
						null,
						this
				)
		);

		try {
			consumePluralAttributeReference( ctx.path() );
		}
		finally {
			dotIdentifierConsumerStack.pop();
		}
	}


	// Predicates (and `whereClause`)

	@Override
	public SqmPredicate visitWhereClause(HqlParser.WhereClauseContext ctx) {
		if ( ctx == null || ctx.predicate() == null ) {
			return null;
		}

		return (SqmPredicate) ctx.predicate().accept( this );

	}

	@Override
	public SqmGroupedPredicate visitGroupedPredicate(HqlParser.GroupedPredicateContext ctx) {
		return new SqmGroupedPredicate(
				(SqmPredicate) ctx.predicate().accept( this ),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmPredicate visitAndPredicate(HqlParser.AndPredicateContext ctx) {
		return new SqmAndPredicate(
				(SqmPredicate) ctx.predicate( 0 ).accept( this ),
				(SqmPredicate) ctx.predicate( 1 ).accept( this ),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmPredicate visitOrPredicate(HqlParser.OrPredicateContext ctx) {
		return new SqmOrPredicate(
				(SqmPredicate) ctx.predicate( 0 ).accept( this ),
				(SqmPredicate) ctx.predicate( 1 ).accept( this ),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmPredicate visitNegatedPredicate(HqlParser.NegatedPredicateContext ctx) {
		SqmPredicate predicate = (SqmPredicate) ctx.predicate().accept( this );
		if ( predicate instanceof SqmNegatablePredicate ) {
			( (SqmNegatablePredicate) predicate ).negate();
			return predicate;
		}
		else {
			return new SqmNegatedPredicate( predicate, creationContext.getNodeBuilder() );
		}
	}

	@Override
	public SqmBetweenPredicate visitBetweenPredicate(HqlParser.BetweenPredicateContext ctx) {
		return new SqmBetweenPredicate(
				(SqmExpression) ctx.expression( 0 ).accept( this ),
				(SqmExpression) ctx.expression( 1 ).accept( this ),
				(SqmExpression) ctx.expression( 2 ).accept( this ),
				ctx.NOT() != null,
				creationContext.getNodeBuilder()
		);
	}


	@Override
	public SqmNullnessPredicate visitIsNullPredicate(HqlParser.IsNullPredicateContext ctx) {
		return new SqmNullnessPredicate(
				(SqmExpression) ctx.expression().accept( this ),
				ctx.NOT() != null,
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmEmptinessPredicate visitIsEmptyPredicate(HqlParser.IsEmptyPredicateContext ctx) {
		return new SqmEmptinessPredicate(
				(SqmPluralValuedSimplePath<?>) ctx.expression().accept( this ),
				ctx.NOT() != null,
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public Object visitComparisonOperator(HqlParser.ComparisonOperatorContext ctx) {
		final TerminalNode firstToken = (TerminalNode) ctx.getChild( 0 );
		switch ( firstToken.getSymbol().getType() ) {
			case HqlLexer.EQUAL:
				return ComparisonOperator.EQUAL;
			case HqlLexer.NOT_EQUAL:
				return ComparisonOperator.NOT_EQUAL;
			case HqlLexer.LESS:
				return ComparisonOperator.LESS_THAN;
			case HqlLexer.LESS_EQUAL:
				return ComparisonOperator.LESS_THAN_OR_EQUAL;
			case HqlLexer.GREATER:
				return ComparisonOperator.GREATER_THAN;
			case HqlLexer.GREATER_EQUAL:
				return ComparisonOperator.GREATER_THAN_OR_EQUAL;
			case HqlLexer.IS: {
				final TerminalNode secondToken = (TerminalNode) ctx.getChild( 1 );
				return secondToken.getSymbol().getType() == HqlLexer.NOT
						? ComparisonOperator.NOT_DISTINCT_FROM
						: ComparisonOperator.DISTINCT_FROM;
			}
		}
		throw new QueryException("missing operator");
	}

	@Override
	public SqmPredicate visitComparisonPredicate(HqlParser.ComparisonPredicateContext ctx) {
		final ComparisonOperator comparisonOperator = (ComparisonOperator) ctx.comparisonOperator().accept( this );
		final List<HqlParser.ExpressionContext> expressionContexts = ctx.expression();
		final SqmExpression left;
		final SqmExpression right;
		final HqlParser.ExpressionContext leftExpressionContext = expressionContexts.get( 0 );
		final HqlParser.ExpressionContext rightExpressionContext = expressionContexts.get( 1 );
		switch (comparisonOperator) {
			case EQUAL:
			case NOT_EQUAL:
			case DISTINCT_FROM:
			case NOT_DISTINCT_FROM: {
				Map<Class<?>, Enum<?>> possibleEnumValues;
				if ( ( possibleEnumValues = getPossibleEnumValues( leftExpressionContext ) ) != null ) {
					right = (SqmExpression) rightExpressionContext.accept( this );
					left = resolveEnumShorthandLiteral(
							leftExpressionContext,
							possibleEnumValues,
							right.getJavaType()
					);
					break;
				}
				else if ( ( possibleEnumValues = getPossibleEnumValues( rightExpressionContext ) ) != null ) {
					left = (SqmExpression) leftExpressionContext.accept( this );
					right = resolveEnumShorthandLiteral(
							rightExpressionContext,
							possibleEnumValues,
							left.getJavaType()
					);
					break;
				}
				left = (SqmExpression) leftExpressionContext.accept( this );
				right = (SqmExpression) rightExpressionContext.accept( this );
				// This is something that we used to support before 6 which is also used in our testsuite
				if ( left instanceof SqmLiteralNull<?> ) {
					return new SqmNullnessPredicate(
							right,
							comparisonOperator == ComparisonOperator.NOT_EQUAL
									|| comparisonOperator == ComparisonOperator.DISTINCT_FROM,
							creationContext.getNodeBuilder()
					);
				}
				else if ( right instanceof SqmLiteralNull<?> ) {
					return new SqmNullnessPredicate(
							left,
							comparisonOperator == ComparisonOperator.NOT_EQUAL
									|| comparisonOperator == ComparisonOperator.DISTINCT_FROM,
							creationContext.getNodeBuilder()
					);
				}
				break;
			}
			default: {
				left = (SqmExpression) leftExpressionContext.accept( this );
				right = (SqmExpression) rightExpressionContext.accept( this );
				break;
			}
		}
		return new SqmComparisonPredicate(
				left,
				comparisonOperator,
				right,
				creationContext.getNodeBuilder()
		);
	}

	private SqmExpression resolveEnumShorthandLiteral(HqlParser.ExpressionContext expressionContext, Map<Class<?>, Enum<?>> possibleEnumValues, Class<?> enumType) {
		final Enum<?> enumValue;
		if ( possibleEnumValues != null && ( enumValue = possibleEnumValues.get( enumType ) ) != null ) {
			DotIdentifierConsumer dotIdentifierConsumer = dotIdentifierConsumerStack.getCurrent();
			dotIdentifierConsumer.consumeIdentifier( enumValue.getClass().getCanonicalName(), true, false );
			dotIdentifierConsumer.consumeIdentifier( enumValue.name(), false, true );
			return (SqmExpression) dotIdentifierConsumerStack.getCurrent().getConsumedPart();
		}
		else {
			return (SqmExpression) expressionContext.accept( this );
		}
	}

	private Map<Class<?>, Enum<?>> getPossibleEnumValues(HqlParser.ExpressionContext expressionContext) {
		ParseTree ctx;
		// Traverse the expression structure according to the grammar
		if ( expressionContext instanceof HqlParser.CollateExpressionContext && expressionContext.getChildCount() == 1 ) {
			ctx = expressionContext.getChild( 0 );

			while ( ctx instanceof HqlParser.PrimaryExpressionContext && ctx.getChildCount() == 1 ) {
				ctx = ctx.getChild( 0 );
			}

			if ( ctx instanceof HqlParser.PathContext && ctx.getChildCount() == 1 ) {
				ctx = ctx.getChild( 0 );

				if ( ctx instanceof HqlParser.GeneralPathFragmentContext && ctx.getChildCount() == 1 ) {
					ctx = ctx.getChild( 0 );

					if ( ctx instanceof HqlParser.DotIdentifierSequenceContext ) {
						return creationContext.getJpaMetamodel().getAllowedEnumLiteralTexts().get( ctx.getText() );
					}
				}
			}
		}

//		if ( expressionContext instanceof HqlParser.CollateExpressionContext
//				&& expressionContext.getChildCount() == 1
//				&& ( ctx = expressionContext.getChild( 0 ) ) instanceof HqlParser.PrimaryExpressionContext
//				&& ctx.getChildCount() == 1
//				&& ( ctx = expressionContext.getChild( 0 ) ) instanceof HqlParser.PathContext
//				&& ctx.getChildCount() == 1
//				&& ( ctx = ctx.getChild( 0 ) ) instanceof HqlParser.GeneralPathFragmentContext
//				&& ctx.getChildCount() == 1
//				&& ( ctx = ctx.getChild( 0 ) ) instanceof HqlParser.DotIdentifierSequenceContext
//				// With childCount == 1 we could have a simple enum literal e.g. ENUM_VALUE
//				// With childCount == 2 we could have a qualified enum literal e.g. EnumName.ENUM_VALUE
//				&& ( ctx.getChildCount() == 1 || ctx.getChildCount() == 2 && ctx.getChild( 1 ) instanceof HqlParser.DotIdentifierSequenceContinuationContext )
//				&& ctx.getChild( 0 ) instanceof HqlParser.IdentifierContext
//		) {
//			return creationContext.getJpaMetamodel().getAllowedEnumLiteralTexts().get( ctx.getText() );
//		}

		return null;
	}

	@Override
	public SqmPredicate visitLikePredicate(HqlParser.LikePredicateContext ctx) {
		final List<HqlParser.ExpressionContext> expressionContexts = ctx.expression();
		if ( ctx.likeEscape() != null ) {
			return new SqmLikePredicate(
					(SqmExpression) expressionContexts.get( 0 ).accept( this ),
					(SqmExpression) expressionContexts.get( 1 ).accept( this ),
					(SqmExpression) ctx.likeEscape().expression().accept( this ),
					ctx.NOT() != null,
					creationContext.getNodeBuilder()
			);
		}
		else {
			return new SqmLikePredicate(
					(SqmExpression) expressionContexts.get( 0 ).accept( this ),
					(SqmExpression) expressionContexts.get( 1 ).accept( this ),
					ctx.NOT() != null,
					creationContext.getNodeBuilder()
			);
		}
	}

	@Override
	public SqmPredicate visitMemberOfPredicate(HqlParser.MemberOfPredicateContext ctx) {
		final SqmPath sqmPluralPath = consumeDomainPath( ctx.path() );

		if ( sqmPluralPath.getReferencedPathSource() instanceof PluralPersistentAttribute ) {
			return new SqmMemberOfPredicate( (SqmExpression) ctx.expression().accept( this ), sqmPluralPath, ctx.NOT() != null, creationContext.getNodeBuilder() );
		}
		else {
			throw new SemanticException( "Path argument to MEMBER OF must be a plural attribute" );
		}
	}

	@Override
	public SqmPredicate visitInPredicate(HqlParser.InPredicateContext ctx) {
		final SqmExpression testExpression = (SqmExpression) ctx.expression().accept( this );
		final HqlParser.InListContext inListContext = ctx.inList();
		if ( inListContext instanceof HqlParser.ExplicitTupleInListContext ) {
			final HqlParser.ExplicitTupleInListContext tupleExpressionListContext = (HqlParser.ExplicitTupleInListContext) inListContext;
			final List<HqlParser.ExpressionContext> expressionContexts = tupleExpressionListContext.expression();
			final boolean isEnum = testExpression.getJavaType().isEnum();
			parameterDeclarationContextStack.push( () -> expressionContexts.size() == 1 );
			try {
				final List<SqmExpression<?>> listExpressions = new ArrayList<>( expressionContexts.size() );
				for ( HqlParser.ExpressionContext expressionContext : expressionContexts ) {
					final Map<Class<?>, Enum<?>> possibleEnumValues;
					if ( isEnum && ( possibleEnumValues = getPossibleEnumValues( expressionContext ) ) != null ) {
						listExpressions.add( resolveEnumShorthandLiteral(
								expressionContext,
								possibleEnumValues,
								testExpression.getJavaType()
						) );
					}
					else {
						listExpressions.add( (SqmExpression) expressionContext.accept( this ) );
					}
				}

				//noinspection unchecked
				return new SqmInListPredicate(
						testExpression,
						listExpressions,
						ctx.NOT() != null,
						creationContext.getNodeBuilder()
				);
			}
			finally {
				parameterDeclarationContextStack.pop();
			}
		}
		else if ( inListContext instanceof HqlParser.ParamInListContext ) {
			final HqlParser.ParamInListContext tupleExpressionListContext = (HqlParser.ParamInListContext) inListContext;
			parameterDeclarationContextStack.push( () -> true );
			try {
				//noinspection unchecked
				return new SqmInListPredicate(
						testExpression,
						Collections.singletonList( tupleExpressionListContext.parameter().accept( this ) ),
						ctx.NOT() != null,
						creationContext.getNodeBuilder()
				);
			}
			finally {
				parameterDeclarationContextStack.pop();
			}
		}
		else if ( inListContext instanceof HqlParser.SubQueryInListContext ) {
			final HqlParser.SubQueryInListContext subQueryOrParamInListContext = (HqlParser.SubQueryInListContext) inListContext;
			//noinspection unchecked
			return new SqmInSubQueryPredicate(
					testExpression,
					visitSubQuery( subQueryOrParamInListContext.subQuery() ),
					ctx.NOT() != null,
					creationContext.getNodeBuilder()
			);
		}
		else {
			// todo : handle PersistentCollectionReferenceInList labeled branch

			throw new ParsingException( "Unexpected IN predicate type [" + ctx.getClass().getSimpleName() + "] : " + ctx.getText() );
		}
	}

	@Override
	public SqmPredicate visitExistsPredicate(HqlParser.ExistsPredicateContext ctx) {
		final SqmExpression expression = (SqmExpression) ctx.expression().accept( this );
		return new SqmExistsPredicate( expression, creationContext.getNodeBuilder() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object visitEntityTypeExpression(HqlParser.EntityTypeExpressionContext ctx) {
		final HqlParser.ParameterContext parameterContext = ctx.entityTypeReference().parameter();
		final HqlParser.PathContext pathContext = ctx.entityTypeReference().path();
		// can be one of 2 forms:
		//		1) TYPE( some.path )
		//		2) TYPE( :someParam )
		if ( parameterContext != null ) {
			// we have form (2)
			return new SqmParameterizedEntityType(
					(SqmParameter) parameterContext.accept( this ),
					creationContext.getNodeBuilder()
			);
		}
		else if ( pathContext != null ) {
			// we have form (1)
			return ( (SqmPath<?>) pathContext.accept( this ) ).type();
		}

		throw new ParsingException( "Could not interpret grammar context as 'entity type' expression : " + ctx.getText() );
	}

	@Override
	public SqmExpression<?> visitEntityIdExpression(HqlParser.EntityIdExpressionContext ctx) {
		return visitEntityIdReference( ctx.entityIdReference() );
	}

	@Override
	public SqmPath visitEntityIdReference(HqlParser.EntityIdReferenceContext ctx) {
		final SqmPath sqmPath = consumeDomainPath( ctx.path() );
		final DomainType sqmPathType = sqmPath.getReferencedPathSource().getSqmPathType();

		if ( sqmPathType instanceof IdentifiableDomainType ) {
			//noinspection unchecked
			final SqmPath idPath = ( (IdentifiableDomainType) sqmPathType ).getIdentifierDescriptor().createSqmPath(
					sqmPath
			);

			if ( ctx.pathContinuation() == null ) {
				return idPath;
			}

			throw new NotYetImplementedFor6Exception( "Path continuation from `id()` reference not yet implemented" );
		}

		throw new SemanticException( "Path does not reference an identifiable-type : " + sqmPath.getNavigablePath().getFullPath() );
	}

	@Override
	public SqmExpression<?> visitEntityVersionExpression(HqlParser.EntityVersionExpressionContext ctx) {
		return visitEntityVersionReference( ctx.entityVersionReference() );
	}

	@Override
	public SqmPath visitEntityVersionReference(HqlParser.EntityVersionReferenceContext ctx) {
		final SqmPath sqmPath = consumeDomainPath( ctx.path() );
		final DomainType sqmPathType = sqmPath.getReferencedPathSource().getSqmPathType();

		if ( sqmPathType instanceof IdentifiableDomainType ) {
			final IdentifiableDomainType identifiableType = (IdentifiableDomainType) sqmPathType;
			final SingularPersistentAttribute versionAttribute = identifiableType.findVersionAttribute();
			if ( versionAttribute == null ) {
				throw new SemanticException(
						"`" + sqmPath.getNavigablePath().getFullPath() + "` resolved to an identifiable-type (`" +
								identifiableType.getTypeName() + "`) which does not define a version"
				);
			}

			//noinspection unchecked
			return versionAttribute.createSqmPath( sqmPath );
		}

		throw new SemanticException( "Path does not reference an identifiable-type : " + sqmPath.getNavigablePath().getFullPath() );
	}

	@Override
	public SqmPath visitEntityNaturalIdExpression(HqlParser.EntityNaturalIdExpressionContext ctx) {
		return visitEntityNaturalIdReference( ctx.entityNaturalIdReference() );
	}

	@Override
	public SqmPath visitEntityNaturalIdReference(HqlParser.EntityNaturalIdReferenceContext ctx) {
		throw new NotYetImplementedFor6Exception( "Support for HQL natural-id references not yet implemented" );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmMapEntryReference visitMapEntrySelection(HqlParser.MapEntrySelectionContext ctx) {
		return new SqmMapEntryReference(
				consumePluralAttributeReference( ctx.path() ),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmExpression visitConcatenationExpression(HqlParser.ConcatenationExpressionContext ctx) {
		if ( ctx.expression().size() != 2 ) {
			throw new ParsingException( "Expecting 2 operands to the concat operator" );
		}
		return getFunctionDescriptor( "concat" ).generateSqmExpression(
				asList(
						(SqmExpression<?>) ctx.expression( 0 ).accept( this ),
						(SqmExpression<?>) ctx.expression( 1 ).accept( this )
				),
				resolveExpressableTypeBasic( String.class ),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public Object visitSignOperator(HqlParser.SignOperatorContext ctx) {
		if (ctx.PLUS() != null) {
			return UnaryArithmeticOperator.UNARY_PLUS;
		}
		else if (ctx.MINUS() != null) {
			return UnaryArithmeticOperator.UNARY_MINUS;
		}
		else {
			throw new QueryException("missing operator");
		}
	}

	@Override
	public Object visitAdditiveOperator(HqlParser.AdditiveOperatorContext ctx) {
		if (ctx.PLUS() != null) {
			return BinaryArithmeticOperator.ADD;
		}
		else if (ctx.MINUS() != null) {
			return BinaryArithmeticOperator.SUBTRACT;
		}
		else {
			throw new QueryException("missing operator");
		}
	}

	@Override
	public Object visitMultiplicativeOperator(HqlParser.MultiplicativeOperatorContext ctx) {
		if (ctx.ASTERISK() != null) {
			return BinaryArithmeticOperator.MULTIPLY;
		}
		else if (ctx.SLASH() != null) {
			return BinaryArithmeticOperator.DIVIDE;
		}
		else if (ctx.PERCENT_OP() != null) {
			return BinaryArithmeticOperator.MODULO;
		}
		else {
			throw new QueryException("missing operator");
		}
	}

	@Override
	public Object visitAdditionExpression(HqlParser.AdditionExpressionContext ctx) {
		if ( ctx.expression().size() != 2 ) {
			throw new ParsingException( "Expecting 2 operands to the additive operator" );
		}

		return new SqmBinaryArithmetic<>(
				(BinaryArithmeticOperator) ctx.additiveOperator().accept(this),
				(SqmExpression<?>) ctx.expression( 0 ).accept(this),
				(SqmExpression<?>) ctx.expression( 1 ).accept(this),
				creationContext.getJpaMetamodel(),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public Object visitMultiplicationExpression(HqlParser.MultiplicationExpressionContext ctx) {
		if ( ctx.expression().size() != 2 ) {
			throw new ParsingException( "Expecting 2 operands to the multiplicative operator" );
		}

		SqmExpression<?> left = (SqmExpression<?>) ctx.expression( 0 ).accept(this);
		SqmExpression<?> right = (SqmExpression<?>) ctx.expression( 1 ).accept(this);
		BinaryArithmeticOperator operator = (BinaryArithmeticOperator) ctx.multiplicativeOperator().accept( this );

		if ( operator == BinaryArithmeticOperator.MODULO ) {
			return getFunctionDescriptor("mod").generateSqmExpression(
					asList( left, right ),
					(AllowableFunctionReturnType<?>) left.getNodeType(),
					creationContext.getQueryEngine(),
					creationContext.getJpaMetamodel().getTypeConfiguration()
			);
		}
		else {
			return new SqmBinaryArithmetic<>(
					operator,
					left,
					right,
					creationContext.getJpaMetamodel(),
					creationContext.getNodeBuilder()
			);
		}
	}

	@Override
	public Object visitToDurationExpression(HqlParser.ToDurationExpressionContext ctx) {
		return new SqmToDuration<>(
				(SqmExpression<?>) ctx.expression().accept(this),
				toDurationUnit( (SqmExtractUnit<?>) ctx.datetimeField().accept(this) ),
				resolveExpressableTypeBasic( Duration.class ),
				creationContext.getNodeBuilder()
		);
	}

	private SqmDurationUnit<Long> toDurationUnit(SqmExtractUnit<?> extractUnit) {
		return new SqmDurationUnit<>(
				extractUnit.getUnit(),
				resolveExpressableTypeBasic( Long.class ),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public Object visitFromDurationExpression(HqlParser.FromDurationExpressionContext ctx) {
		return new SqmByUnit(
				toDurationUnit( (SqmExtractUnit<?>) ctx.datetimeField().accept(this) ),
				(SqmExpression<?>) ctx.expression().accept(this),
				resolveExpressableTypeBasic( Long.class ),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmUnaryOperation<?> visitUnaryExpression(HqlParser.UnaryExpressionContext ctx) {
		return new SqmUnaryOperation<>(
				(UnaryArithmeticOperator) ctx.signOperator().accept( this ),
				(SqmExpression<?>) ctx.expression().accept( this )
		);
	}

	@Override
	public Object visitGroupedExpression(HqlParser.GroupedExpressionContext ctx) {
		return ctx.expression().accept(this);
	}

	@Override
	public Object visitCollateExpression(HqlParser.CollateExpressionContext ctx) {
		SqmExpression<?> expression = (SqmExpression<?>) ctx.primaryExpression().accept( this );
		HqlParser.CollationSpecificationContext collationSpecificationContext = ctx.collationSpecification();
		if ( collationSpecificationContext == null ) {
			return expression;
		}
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw new StrictJpaComplianceViolation(
					StrictJpaComplianceViolation.Type.COLLATIONS
			);
		}
		return new SqmCollate<>( expression, collationSpecificationContext.collateName().getText() );
	}

	@Override
	public Object visitTupleExpression(HqlParser.TupleExpressionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw new StrictJpaComplianceViolation(
					StrictJpaComplianceViolation.Type.TUPLES
			);
		}
		final List<SqmExpression<?>> expressions = visitExpressions( ctx.expression() );
		return new SqmTuple<>( expressions, creationContext.getNodeBuilder() );
	}

	private List<SqmExpression<?>> visitExpressions(List<? extends ParserRuleContext> expressionContexts) {
		final int size = expressionContexts.size();
		final List<SqmExpression<?>> expressions = new ArrayList<>( size );
		for ( int i = 0; i < size; i++ ) {
			expressions.add( (SqmExpression<?>) expressionContexts.get( i ).accept( this ) );
		}
		return expressions;
	}

	@Override
	public Object visitCaseExpression(HqlParser.CaseExpressionContext ctx) {
		return ctx.caseList().accept( this );
	}

	@Override
	public SqmCaseSimple visitSimpleCaseList(HqlParser.SimpleCaseListContext ctx) {
		//noinspection unchecked
		final SqmCaseSimple caseExpression = new SqmCaseSimple(
				(SqmExpression) ctx.expression().accept( this ),
				creationContext.getNodeBuilder()
		);

		for ( HqlParser.SimpleCaseWhenContext simpleCaseWhen : ctx.simpleCaseWhen() ) {
			//noinspection unchecked
			caseExpression.when(
					(SqmExpression) simpleCaseWhen.expression( 0 ).accept( this ),
					(SqmExpression) simpleCaseWhen.expression( 1 ).accept( this )
			);
		}

		if ( ctx.caseOtherwise() != null ) {
			//noinspection unchecked
			caseExpression.otherwise( (SqmExpression) ctx.caseOtherwise().expression().accept( this ) );
		}

		return caseExpression;
	}

	@Override
	public SqmCaseSearched visitSearchedCaseList(HqlParser.SearchedCaseListContext ctx) {
		final SqmCaseSearched<?> caseExpression = new SqmCaseSearched<>( creationContext.getNodeBuilder() );

		for ( HqlParser.SearchedCaseWhenContext whenFragment : ctx.searchedCaseWhen() ) {
			//noinspection unchecked
			caseExpression.when(
					(SqmPredicate) whenFragment.predicate().accept( this ),
					(SqmExpression) whenFragment.expression().accept( this )
			);
		}

		if ( ctx.caseOtherwise() != null ) {
			//noinspection unchecked
			caseExpression.otherwise( (SqmExpression) ctx.caseOtherwise().expression().accept( this ) );
		}

		return caseExpression;
	}

	@Override
	public SqmExpression visitCurrentDateFunction(HqlParser.CurrentDateFunctionContext ctx) {
		return getFunctionDescriptor("current_date")
				.generateSqmExpression(
						resolveExpressableTypeBasic( Date.class ),
						creationContext.getQueryEngine(),
						creationContext.getJpaMetamodel().getTypeConfiguration()
				);
	}

	@Override
	public SqmExpression visitCurrentTimeFunction(HqlParser.CurrentTimeFunctionContext ctx) {
		return getFunctionDescriptor("current_time")
				.generateSqmExpression(
						resolveExpressableTypeBasic( Time.class ),
						creationContext.getQueryEngine(),
						creationContext.getJpaMetamodel().getTypeConfiguration()
				);
	}

	@Override
	public SqmExpression visitCurrentTimestampFunction(HqlParser.CurrentTimestampFunctionContext ctx) {
		return getFunctionDescriptor("current_timestamp")
				.generateSqmExpression(
						resolveExpressableTypeBasic( Timestamp.class ),
						creationContext.getQueryEngine(),
						creationContext.getJpaMetamodel().getTypeConfiguration()
				);
	}

	@Override
	public SqmExpression visitInstantFunction(HqlParser.InstantFunctionContext ctx) {
		return getFunctionDescriptor("instant")
				.generateSqmExpression(
						resolveExpressableTypeBasic( Instant.class ),
						creationContext.getQueryEngine(),
						creationContext.getJpaMetamodel().getTypeConfiguration()
				);
	}

	@Override
	public SqmExpression visitLocalDateFunction(HqlParser.LocalDateFunctionContext ctx) {
		return getFunctionDescriptor("local_date")
				.generateSqmExpression(
						resolveExpressableTypeBasic( LocalDate.class ),
						creationContext.getQueryEngine(),
						creationContext.getJpaMetamodel().getTypeConfiguration()
				);
	}

	@Override
	public SqmExpression visitLocalTimeFunction(HqlParser.LocalTimeFunctionContext ctx) {
		return getFunctionDescriptor("local_time")
				.generateSqmExpression(
						resolveExpressableTypeBasic( LocalTime.class ),
						creationContext.getQueryEngine(),
						creationContext.getJpaMetamodel().getTypeConfiguration()
				);
	}

	@Override
	public SqmExpression visitLocalDateTimeFunction(HqlParser.LocalDateTimeFunctionContext ctx) {
		return getFunctionDescriptor("local_datetime")
				.generateSqmExpression(
						resolveExpressableTypeBasic( LocalDateTime.class ),
						creationContext.getQueryEngine(),
						creationContext.getJpaMetamodel().getTypeConfiguration()
				);
	}

	@Override
	public SqmExpression visitOffsetDateTimeFunction(HqlParser.OffsetDateTimeFunctionContext ctx) {
		return getFunctionDescriptor("offset_datetime")
				.generateSqmExpression(
						resolveExpressableTypeBasic( OffsetDateTime.class ),
						creationContext.getQueryEngine(),
						creationContext.getJpaMetamodel().getTypeConfiguration()
				);
	}

	@Override
	public Object visitLeastFunction(HqlParser.LeastFunctionContext ctx) {
		final List<SqmTypedNode<?>> arguments = new ArrayList<>();

		SqmExpressable<?> type = null;

		for ( HqlParser.ExpressionContext argument : ctx.expression() ) {
			SqmTypedNode arg = (SqmTypedNode) argument.accept( this );
			arguments.add(arg);
			//TODO: do something better here!
			type = arg.getNodeType();
		}

		return getFunctionDescriptor("least")
				.generateSqmExpression(
						arguments,
						(AllowableFunctionReturnType<?>) type,
						creationContext.getQueryEngine(),
						creationContext.getJpaMetamodel().getTypeConfiguration()
				);
	}

	@Override
	public Object visitGreatestFunction(HqlParser.GreatestFunctionContext ctx) {
		final List<SqmTypedNode<?>> arguments = new ArrayList<>();

		SqmExpressable<?> type = null;

		for ( HqlParser.ExpressionContext argument : ctx.expression() ) {
			SqmTypedNode arg = (SqmTypedNode) argument.accept( this );
			arguments.add(arg);
			//TODO: do something better here!
			type = arg.getNodeType();
		}

		return getFunctionDescriptor("greatest")
				.generateSqmExpression(
						arguments,
						(AllowableFunctionReturnType<?>) type,
						creationContext.getQueryEngine(),
						creationContext.getJpaMetamodel().getTypeConfiguration()
				);
	}

	@Override
	public SqmExpression visitCoalesceFunction(HqlParser.CoalesceFunctionContext ctx) {
		final List<SqmTypedNode<?>> arguments = new ArrayList<>();

		SqmExpressable<?> type = null;

		for ( HqlParser.ExpressionContext argument : ctx.expression() ) {
			SqmTypedNode arg = (SqmTypedNode) argument.accept( this );
			arguments.add(arg);
			//TODO: do something better here!
			type = arg.getNodeType();
		}

		return getFunctionDescriptor("coalesce")
				.generateSqmExpression(
						arguments,
						(AllowableFunctionReturnType<?>) type,
						creationContext.getQueryEngine(),
						creationContext.getJpaMetamodel().getTypeConfiguration()
				);
	}

	@Override
	public SqmExpression visitNullifFunction(HqlParser.NullifFunctionContext ctx) {
		final SqmExpression arg1 = (SqmExpression) ctx.expression( 0 ).accept( this );
		final SqmExpression arg2 = (SqmExpression) ctx.expression( 1 ).accept( this );

		return getFunctionDescriptor("nullif").generateSqmExpression(
				asList( arg1, arg2 ),
				(AllowableFunctionReturnType) arg1.getNodeType(),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmExpression visitIfnullFunction(HqlParser.IfnullFunctionContext ctx) {
		final SqmExpression arg1 = (SqmExpression) ctx.expression( 0 ).accept( this );
		final SqmExpression arg2 = (SqmExpression) ctx.expression( 1 ).accept( this );

		return getFunctionDescriptor("ifnull").generateSqmExpression(
				asList( arg1, arg2 ),
				(AllowableFunctionReturnType) arg1.getNodeType(),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmExpression<?> visitLiteralExpression(HqlParser.LiteralExpressionContext ctx) {
		return (SqmExpression<?>) ctx.literal().accept( this );
	}

	@Override
	public Object visitBinaryLiteral(HqlParser.BinaryLiteralContext ctx) {
		final TerminalNode binaryLiteral = ctx.BINARY_LITERAL();
		if ( binaryLiteral != null ) {
			return binaryLiteral( binaryLiteral.getText() );
		}
		else {
			final StringBuilder text = new StringBuilder( "x'" );
			for ( TerminalNode hex : ctx.HEX_LITERAL() ) {
				final String hexText = hex.getText();
				if ( hexText.length() != 4 ) {
					throw new LiteralNumberFormatException( "not a byte: " + hexText );
				}
				text.append( hexText.substring( 2 ) );
			}
			return binaryLiteral( text.append( "'" ).toString() );
		}
	}

	@Override
	public Object visitGeneralizedLiteral(HqlParser.GeneralizedLiteralContext ctx) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public SqmExpression<?> visitTerminal(TerminalNode node) {
		if ( node.getSymbol().getType() == HqlLexer.EOF ) {
			return null;
		}
		switch ( node.getSymbol().getType() ) {
			case HqlParser.STRING_LITERAL:
				return stringLiteral( node.getText() );
			case HqlParser.INTEGER_LITERAL:
				return integerLiteral( node.getText() );
			case HqlParser.LONG_LITERAL:
				return longLiteral( node.getText() );
			case HqlParser.BIG_INTEGER_LITERAL:
				return bigIntegerLiteral( node.getText() );
			case HqlParser.HEX_LITERAL:
				return hexLiteral( node.getText() );
			case HqlParser.FLOAT_LITERAL:
				return floatLiteral( node.getText() );
			case HqlParser.DOUBLE_LITERAL:
				return doubleLiteral( node.getText() );
			case HqlParser.BIG_DECIMAL_LITERAL:
				return bigDecimalLiteral( node.getText() );
			case HqlParser.FALSE:
				return booleanLiteral( false );
			case HqlParser.TRUE:
				return booleanLiteral( true );
			case HqlParser.NULL:
				return new SqmLiteralNull<>( creationContext.getQueryEngine().getCriteriaBuilder() );
			case HqlParser.BINARY_LITERAL:
				return binaryLiteral( node.getText() );
			default:
				throw new ParsingException("Unexpected terminal node [" + node.getText() + "]");
		}
	}

	@Override
	public Object visitDateTimeLiteral(HqlParser.DateTimeLiteralContext dateTimeLiteralContext) {
		final HqlParser.DateTimeContext dateTimeContext = dateTimeLiteralContext.dateTime();
		if ( dateTimeContext.offset() == null ) {
			return dateTimeLiteralFrom(
					dateTimeContext.date(),
					dateTimeContext.time(),
					dateTimeContext.zoneId()
			);
		}
		else {
			return offsetDatetimeLiteralFrom(
					dateTimeContext.date(),
					dateTimeContext.time(),
					dateTimeContext.offset()
			);
		}
	}

	@Override
	public Object visitDateLiteral(HqlParser.DateLiteralContext ctx) {
		return ctx.date().accept( this );
	}

	@Override
	public Object visitTimeLiteral(HqlParser.TimeLiteralContext ctx) {
		return ctx.time().accept( this );
	}

	@Override
	public Object visitJdbcTimestampLiteral(HqlParser.JdbcTimestampLiteralContext ctx) {
		final HqlParser.GenericTemporalLiteralTextContext genericTemporalLiteralTextContext = ctx.genericTemporalLiteralText();
		if ( genericTemporalLiteralTextContext == null ) {
			return ctx.dateTime().accept( this );
		}
		else {
			return sqlTimestampLiteralFrom( genericTemporalLiteralTextContext.getText() );
		}
	}

	@Override
	public Object visitJdbcDateLiteral(HqlParser.JdbcDateLiteralContext ctx) {
		final HqlParser.GenericTemporalLiteralTextContext genericTemporalLiteralTextContext = ctx.genericTemporalLiteralText();
		if ( genericTemporalLiteralTextContext == null ) {
			return ctx.date().accept( this );
		}
		else {
			return sqlDateLiteralFrom( genericTemporalLiteralTextContext.getText() );
		}
	}

	@Override
	public Object visitJdbcTimeLiteral(HqlParser.JdbcTimeLiteralContext ctx) {
		final HqlParser.GenericTemporalLiteralTextContext genericTemporalLiteralTextContext = ctx.genericTemporalLiteralText();
		if ( genericTemporalLiteralTextContext == null ) {
			return ctx.time().accept( this );
		}
		else {
			return sqlTimeLiteralFrom( genericTemporalLiteralTextContext.getText() );
		}
	}

	@Override
	public Object visitDateTime(HqlParser.DateTimeContext ctx) {
		return dateTimeLiteralFrom(
				ctx.date(),
				ctx.time(),
				ctx.zoneId()
		);
	}

	private SqmLiteral<?> dateTimeLiteralFrom(
			HqlParser.DateContext date,
			HqlParser.TimeContext time,
			HqlParser.ZoneIdContext timezone) {
		if ( timezone == null ) {
			return new SqmLiteral<>(
					LocalDateTime.of( localDate( date ), localTime( time ) ),
					resolveExpressableTypeBasic( LocalDateTime.class ),
					creationContext.getNodeBuilder()
			);
		}
		else {
			final ZoneId zoneId = visitZoneId( timezone );
			return new SqmLiteral<>(
					ZonedDateTime.of( localDate( date ), localTime( time ), zoneId ),
					resolveExpressableTypeBasic( ZonedDateTime.class ),
					creationContext.getNodeBuilder()
			);
		}
	}

	@Override
	public ZoneId visitZoneId(HqlParser.ZoneIdContext ctx) {
		final String timezoneText = ctx.getText();
		final String timezoneFullName = ZoneId.SHORT_IDS.get( timezoneText );
		if ( timezoneFullName == null ) {
			return ZoneId.of( timezoneText );
		}
		else {
			return ZoneId.of( timezoneFullName );
		}
	}

	private SqmLiteral<?> offsetDatetimeLiteralFrom(
			HqlParser.DateContext date,
			HqlParser.TimeContext time,
			HqlParser.OffsetContext offset) {
		return new SqmLiteral<>(
				OffsetDateTime.of( localDate( date ), localTime( time ), zoneOffset( offset ) ),
				resolveExpressableTypeBasic( OffsetDateTime.class ),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public Object visitDate(HqlParser.DateContext ctx) {
		return new SqmLiteral<>(
				localDate( ctx ),
				resolveExpressableTypeBasic( LocalDate.class ),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public Object visitTime(HqlParser.TimeContext ctx) {
		return new SqmLiteral<>(
				localTime( ctx ),
				resolveExpressableTypeBasic( LocalTime.class ),
				creationContext.getNodeBuilder()
		);
	}

	private static LocalTime localTime(HqlParser.TimeContext ctx) {
		final HqlParser.SecondContext second = ctx.second();
		if ( second != null ) {
			final String secondText = second.getText();
			final int index = secondText.indexOf( '.');
			if ( index < 0 ) {
				return LocalTime.of(
						Integer.parseInt( ctx.hour().getText() ),
						Integer.parseInt( ctx.minute().getText() ),
						Integer.parseInt( secondText )
				);
			}
			else {
				return LocalTime.of(
						Integer.parseInt( ctx.hour().getText() ),
						Integer.parseInt( ctx.minute().getText() ),
						Integer.parseInt( secondText.substring( 0, index ) ),
						Integer.parseInt( secondText.substring( index + 1 ) )
				);
			}
		}
		else {
			return LocalTime.of(
					Integer.parseInt( ctx.hour().getText() ),
					Integer.parseInt( ctx.minute().getText() )
			);
		}
	}

	private static LocalDate localDate(HqlParser.DateContext ctx) {
		return LocalDate.of(
				Integer.parseInt( ctx.year().getText() ),
				Integer.parseInt( ctx.month().getText() ),
				Integer.parseInt( ctx.day().getText() )
		);
	}

	private static ZoneOffset zoneOffset(HqlParser.OffsetContext offset) {
		final HqlParser.MinuteContext minute = offset.minute();
		return minute == null
				? ZoneOffset.ofHours( Integer.parseInt( offset.hour().getText() ) )
				: ZoneOffset.ofHoursMinutes(
				Integer.parseInt( offset.hour().getText() ),
				Integer.parseInt( minute.getText() )
		);
	}

//	private SqmLiteral<OffsetDateTime> offsetDatetimeLiteralFrom(String literalText) {
//		TemporalAccessor parsed = OFFSET_DATE_TIME.parse( literalText );
//		return new SqmLiteral<>(
//				OffsetDateTime.from( parsed ),
//				resolveExpressableTypeBasic( OffsetDateTime.class ),
//				creationContext.getNodeBuilder()
//		);
//	}
//
//	private SqmLiteral<?> dateTimeLiteralFrom(String literalText) {
//		//TO DO: return an OffsetDateTime when appropriate?
//		TemporalAccessor parsed = DATE_TIME.parse( literalText );
//		try {
//			return new SqmLiteral<>(
//					ZonedDateTime.from( parsed ),
//					resolveExpressableTypeBasic( ZonedDateTime.class ),
//					creationContext.getNodeBuilder()
//			);
//		}
//		catch (DateTimeException dte) {
//			return new SqmLiteral<>(
//					LocalDateTime.from( parsed ),
//					resolveExpressableTypeBasic( LocalDateTime.class ),
//					creationContext.getNodeBuilder()
//			);
//		}
//	}
//
//	private SqmLiteral<LocalDate> localDateLiteralFrom(String literalText) {
//		return new SqmLiteral<>(
//				LocalDate.from( ISO_LOCAL_DATE.parse( literalText ) ),
//				resolveExpressableTypeBasic( LocalDate.class ),
//				creationContext.getNodeBuilder()
//		);
//	}
//
//	private SqmLiteral<LocalTime> localTimeLiteralFrom(String literalText) {
//		return new SqmLiteral<>(
//				LocalTime.from( ISO_LOCAL_TIME.parse( literalText ) ),
//				resolveExpressableTypeBasic( LocalTime.class ),
//				creationContext.getNodeBuilder()
//		);
//	}

	private SqmLiteral<?> sqlTimestampLiteralFrom(String literalText) {
		TemporalAccessor parsed = DATE_TIME.parse( literalText );
		try {
			ZonedDateTime zonedDateTime = ZonedDateTime.from( parsed );
			Calendar literal = GregorianCalendar.from( zonedDateTime );
			return new SqmLiteral<>(
					literal,
					resolveExpressableTypeBasic( Calendar.class ),
					creationContext.getNodeBuilder()
			);
		}
		catch (DateTimeException dte) {
			LocalDateTime localDateTime = LocalDateTime.from( parsed );
			Timestamp literal = Timestamp.valueOf( localDateTime );
			return new SqmLiteral<>(
					literal,
					resolveExpressableTypeBasic( Timestamp.class ),
					creationContext.getNodeBuilder()
			);
		}
	}

	private SqmLiteral<Date> sqlDateLiteralFrom(String literalText) {
		final LocalDate localDate = LocalDate.from( ISO_LOCAL_DATE.parse( literalText ) );
		final Date literal = Date.valueOf( localDate );
		return new SqmLiteral<>(
				literal,
				resolveExpressableTypeBasic( Date.class ),
				creationContext.getNodeBuilder()
		);
	}

	private SqmLiteral<Time> sqlTimeLiteralFrom(String literalText) {
		final LocalTime localTime = LocalTime.from( ISO_LOCAL_TIME.parse( literalText ) );
		final Time literal = Time.valueOf( localTime );
		return new SqmLiteral<>(
				literal,
				resolveExpressableTypeBasic( Time.class ),
				creationContext.getNodeBuilder()
		);
	}

	private SqmLiteral<Boolean> booleanLiteral(boolean value) {
		return new SqmLiteral<>(
				value,
				resolveExpressableTypeBasic( Boolean.class ),
				creationContext.getNodeBuilder()
		);
	}

	private SqmLiteral<String> stringLiteral(String text) {
		return new SqmLiteral<>(
				text,
				resolveExpressableTypeBasic( String.class ),
				creationContext.getNodeBuilder()
		);
	}

	private SqmLiteral<byte[]> binaryLiteral(String text) {
		return new SqmLiteral<>(
				StandardBasicTypes.BINARY.fromStringValue( text.substring( 2, text.length()-1 ) ),
				resolveExpressableTypeBasic( byte[].class ),
				creationContext.getNodeBuilder()
		);
	}

	private SqmLiteral<Integer> integerLiteral(String text) {
		try {
			final Integer value = Integer.valueOf( text );
			return new SqmLiteral<>(
					value,
					resolveExpressableTypeBasic( Integer.class ),
					creationContext.getNodeBuilder()
			);
		}
		catch (NumberFormatException e) {
			throw new LiteralNumberFormatException(
					"Unable to convert sqm literal [" + text + "] to Integer",
					e
			);
		}
	}

	private SqmLiteral<Long> longLiteral(String text) {
		final String originalText = text;
		try {
			if ( text.endsWith( "l" ) || text.endsWith( "L" ) ) {
				text = text.substring( 0, text.length() - 1 );
			}
			final Long value = Long.valueOf( text );
			return new SqmLiteral<>(
					value,
					resolveExpressableTypeBasic( Long.class ),
					creationContext.getNodeBuilder()
			);
		}
		catch (NumberFormatException e) {
			throw new LiteralNumberFormatException(
					"Unable to convert sqm literal [" + originalText + "] to Long",
					e
			);
		}
	}

	private SqmLiteral<? extends Number> hexLiteral(String text) {
		final String originalText = text;
		text = text.substring( 2 );
		try {
			final Number value;
			final BasicDomainType<? extends Number> type;
			if ( text.endsWith( "l" ) || text.endsWith( "L" ) ) {
				text = text.substring( 0, text.length() - 1 );
				value = Long.parseUnsignedLong( text, 16 );
				type = resolveExpressableTypeBasic( Long.class );
			}
			else {
				value = Integer.parseUnsignedInt( text, 16 );
				type = resolveExpressableTypeBasic( Integer.class );
			}
			return new SqmLiteral<>(
					value,
					(SqmExpressable<Number>) type,
					creationContext.getNodeBuilder()
			);
		}
		catch (NumberFormatException e) {
			throw new LiteralNumberFormatException(
					"Unable to convert sqm literal [" + originalText + "]",
					e
			);
		}
	}

	private SqmLiteral<BigInteger> bigIntegerLiteral(String text) {
		final String originalText = text;
		try {
			if ( text.endsWith( "bi" ) || text.endsWith( "BI" ) ) {
				text = text.substring( 0, text.length() - 2 );
			}
			return new SqmLiteral<>(
					new BigInteger( text ),
					resolveExpressableTypeBasic( BigInteger.class  ),
					creationContext.getNodeBuilder()
			);
		}
		catch (NumberFormatException e) {
			throw new LiteralNumberFormatException(
					"Unable to convert sqm literal [" + originalText + "] to BigInteger",
					e
			);
		}
	}

	private SqmLiteral<Float> floatLiteral(String text) {
		try {
			return new SqmLiteral<>(
					Float.valueOf( text ),
					resolveExpressableTypeBasic( Float.class ),
					creationContext.getNodeBuilder()
			);
		}
		catch (NumberFormatException e) {
			throw new LiteralNumberFormatException(
					"Unable to convert sqm literal [" + text + "] to Float",
					e
			);
		}
	}

	private SqmLiteral<Double> doubleLiteral(String text) {
		try {
			return new SqmLiteral<>(
					Double.valueOf( text ),
					resolveExpressableTypeBasic( Double.class ),
					creationContext.getNodeBuilder()
			);
		}
		catch (NumberFormatException e) {
			throw new LiteralNumberFormatException(
					"Unable to convert sqm literal [" + text + "] to Double",
					e
			);
		}
	}

	private SqmLiteral<BigDecimal> bigDecimalLiteral(String text) {
		final String originalText = text;
		try {
			if ( text.endsWith( "bd" ) || text.endsWith( "BD" ) ) {
				text = text.substring( 0, text.length() - 2 );
			}
			return new SqmLiteral<>(
					new BigDecimal( text ),
					resolveExpressableTypeBasic( BigDecimal.class ),
					creationContext.getNodeBuilder()
			);
		}
		catch (NumberFormatException e) {
			throw new LiteralNumberFormatException(
					"Unable to convert sqm literal [" + originalText + "] to BigDecimal",
					e
			);
		}
	}

	private <J> BasicDomainType<J> resolveExpressableTypeBasic(Class<J> javaType) {
		return creationContext.getJpaMetamodel().getTypeConfiguration().standardBasicTypeForJavaType( javaType );
	}

	@Override
	public Object visitParameterExpression(HqlParser.ParameterExpressionContext ctx) {
		return ctx.parameter().accept( this );
	}

	@Override
	public SqmNamedParameter<?> visitNamedParameter(HqlParser.NamedParameterContext ctx) {
		final SqmNamedParameter<?> param = new SqmNamedParameter<>(
				ctx.identifier().getText(),
				parameterDeclarationContextStack.getCurrent().isMultiValuedBindingAllowed(),
				creationContext.getNodeBuilder()
		);
		parameterCollector.addParameter( param );
		return param;
	}

	@Override
	public SqmPositionalParameter<?> visitPositionalParameter(HqlParser.PositionalParameterContext ctx) {
		if ( ctx.INTEGER_LITERAL() == null ) {
			throw new SemanticException( "Encountered positional parameter which did not declare position (? instead of, e.g., ?1)" );
		}
		final SqmPositionalParameter<?> param = new SqmPositionalParameter<>(
				Integer.parseInt( ctx.INTEGER_LITERAL().getText() ),
				parameterDeclarationContextStack.getCurrent().isMultiValuedBindingAllowed(),
				creationContext.getNodeBuilder()
		);
		parameterCollector.addParameter( param );
		return param;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Functions

	@Override
	public SqmExpression<?> visitJpaNonStandardFunction(HqlParser.JpaNonStandardFunctionContext ctx) {
		final String functionName = ctx.jpaNonStandardFunctionName().STRING_LITERAL().getText().toLowerCase();
		//noinspection unchecked
		final List<SqmTypedNode<?>> functionArguments = ctx.nonStandardFunctionArguments() == null
				? emptyList()
				: (List<SqmTypedNode<?>>) ctx.nonStandardFunctionArguments().accept( this );

		SqmFunctionDescriptor functionTemplate = getFunctionDescriptor( functionName );
		if (functionTemplate == null) {
			functionTemplate = new NamedSqmFunctionDescriptor(
					functionName,
					true,
					null,
					StandardFunctionReturnTypeResolvers.invariant( StandardBasicTypes.OBJECT_TYPE )
			);
		}
		return functionTemplate.generateSqmExpression(
				functionArguments,
				null,
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmExpression<?> visitNonStandardFunction(HqlParser.NonStandardFunctionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw new StrictJpaComplianceViolation(
					"Encountered non-compliant non-standard function call [" +
							ctx.nonStandardFunctionName() + "], but strict JPA " +
							"compliance was requested; use JPA's FUNCTION(functionName[,...]) " +
							"syntax name instead",
					StrictJpaComplianceViolation.Type.FUNCTION_CALL
			);
		}

		final String functionName = ctx.nonStandardFunctionName().getText().toLowerCase();
		//noinspection unchecked
		final List<SqmTypedNode<?>> functionArguments = ctx.nonStandardFunctionArguments() == null
				? emptyList()
				: (List<SqmTypedNode<?>>) ctx.nonStandardFunctionArguments().accept( this );

		SqmFunctionDescriptor functionTemplate = getFunctionDescriptor( functionName );
		if ( functionTemplate == null ) {
			functionTemplate = new NamedSqmFunctionDescriptor(
					functionName,
					true,
					null,
					StandardFunctionReturnTypeResolvers.invariant( StandardBasicTypes.OBJECT_TYPE )
			);
		}

		return functionTemplate.generateSqmExpression(
				functionArguments,
				null,
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public List<SqmTypedNode<?>> visitNonStandardFunctionArguments(HqlParser.NonStandardFunctionArgumentsContext ctx) {
		final List<SqmTypedNode<?>> arguments = new ArrayList<>();

		if ( ctx.datetimeField() != null ) {
			arguments.add( toDurationUnit( (SqmExtractUnit<?>) ctx.datetimeField().accept( this ) ) );
		}

		for ( int i=0, size=ctx.expression().size(); i<size; i++ ) {
			// we handle the final argument differently...
			if ( i == size-1 ) {
				arguments.add( visitFinalFunctionArgument( ctx.expression( i ) ) );
			}
			else {
				arguments.add( (SqmTypedNode<?>) ctx.expression( i ).accept( this ) );
			}
		}

		return arguments;
	}

	private SqmExpression<?> visitFinalFunctionArgument(HqlParser.ExpressionContext expression) {
		// the final argument to a function may accept multi-value parameter (varargs),
		// 		but only if we are operating in non-strict JPA mode
		parameterDeclarationContextStack.push( creationOptions::useStrictJpaCompliance );
		try {
			return (SqmExpression<?>) expression.accept( this );
		}
		finally {
			parameterDeclarationContextStack.pop();
		}
	}

	@Override
	public SqmExpression<?> visitCeilingFunction(HqlParser.CeilingFunctionContext ctx) {
		final SqmExpression<?> arg = (SqmExpression<?>) ctx.expression().accept( this );

		return getFunctionDescriptor("ceiling").generateSqmExpression(
				arg,
				(AllowableFunctionReturnType<?>) arg.getNodeType(),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmExpression<?> visitFloorFunction(HqlParser.FloorFunctionContext ctx) {
		final SqmExpression<?> arg = (SqmExpression<?>) ctx.expression().accept( this );

		return getFunctionDescriptor("floor").generateSqmExpression(
				arg,
				(AllowableFunctionReturnType<?>) arg.getNodeType(),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	private SqmFunctionDescriptor getFunctionDescriptor(String name) {
		return creationContext.getQueryEngine().getSqmFunctionRegistry().findFunctionDescriptor(name);
	}

	@Override
	public SqmExpression<?> visitAbsFunction(HqlParser.AbsFunctionContext ctx) {
		final SqmExpression<?> arg = (SqmExpression<?>) ctx.expression().accept( this );

		return getFunctionDescriptor("abs").generateSqmExpression(
				arg,
				(AllowableFunctionReturnType<?>) arg.getNodeType(),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmExpression<?> visitSignFunction(HqlParser.SignFunctionContext ctx) {
		final SqmExpression<?> arg = (SqmExpression<?>) ctx.expression().accept( this );

		return getFunctionDescriptor("sign").generateSqmExpression(
				arg,
				resolveExpressableTypeBasic( Integer.class ),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmExpression<?> visitModFunction(HqlParser.ModFunctionContext ctx) {
		final SqmExpression<?> dividend = (SqmExpression<?>) ctx.modDividendArgument().accept( this );
		final SqmExpression<?> divisor = (SqmExpression<?>) ctx.modDivisorArgument().accept( this );

		return getFunctionDescriptor("mod").generateSqmExpression(
				asList( dividend, divisor ),
				(AllowableFunctionReturnType<?>) dividend.getNodeType(),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmExpression<?> visitPowerFunction(HqlParser.PowerFunctionContext ctx) {
		final SqmExpression<?> base = (SqmExpression<?>) ctx.powerBaseArgument().accept( this );
		final SqmExpression<?> power = (SqmExpression<?>) ctx.powerPowerArgument().accept( this );

		return getFunctionDescriptor("power").generateSqmExpression(
				asList( base, power ),
				(AllowableFunctionReturnType<?>) base.getNodeType(),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmExpression<?> visitTrigFunction(HqlParser.TrigFunctionContext ctx) {
		final SqmExpression<?> arg = (SqmExpression<?>) ctx.expression().accept( this );

		return getFunctionDescriptor( ctx.trigFunctionName().getText() ).generateSqmExpression(
				arg,
				resolveExpressableTypeBasic( Double.class ),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmExpression<?> visitSqrtFunction(HqlParser.SqrtFunctionContext ctx) {
		final SqmExpression<?> arg = (SqmExpression<?>) ctx.expression().accept( this );

		return getFunctionDescriptor("sqrt").generateSqmExpression(
				arg,
				null,
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmExpression<?> visitRoundFunction(HqlParser.RoundFunctionContext ctx) {
		final SqmExpression<?> arg = (SqmExpression<?>) ctx.expression().accept( this );
		final SqmExpression<?> precision = (SqmExpression<?>) ctx.roundFunctionPrecision().expression().accept( this );

		return getFunctionDescriptor("round").generateSqmExpression(
				asList(arg, precision),
				(AllowableFunctionReturnType<?>) arg.getNodeType(),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmExpression<?> visitAtan2Function(HqlParser.Atan2FunctionContext ctx) {
		final SqmExpression<?> sin = (SqmExpression<?>) ctx.expression().get(0).accept( this );
		final SqmExpression<?> cos = (SqmExpression<?>) ctx.expression().get(1).accept( this );

		return getFunctionDescriptor("atan2").generateSqmExpression(
				asList(sin, cos),
				(AllowableFunctionReturnType<?>) sin.getNodeType(),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmExpression<?> visitLnFunction(HqlParser.LnFunctionContext ctx) {
		final SqmExpression<?> arg = (SqmExpression<?>) ctx.expression().accept( this );

		return getFunctionDescriptor("ln").generateSqmExpression(
				arg,
				(AllowableFunctionReturnType<?>) arg.getNodeType(),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);

	}

	@Override
	public SqmExpression<?> visitExpFunction(HqlParser.ExpFunctionContext ctx) {
		final SqmExpression<?> arg = (SqmExpression<?>) ctx.expression().accept( this );

		return getFunctionDescriptor("exp").generateSqmExpression(
				arg,
				(AllowableFunctionReturnType<?>) arg.getNodeType(),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public Object visitDatetimeField(HqlParser.DatetimeFieldContext ctx) {
		NodeBuilder nodeBuilder = creationContext.getNodeBuilder();
		if ( ctx.DAY() != null ) {
			return new SqmExtractUnit<>(
					TemporalUnit.DAY,
					resolveExpressableTypeBasic( Integer.class ),
					nodeBuilder
			);
		}
		if ( ctx.MONTH() != null ) {
			return new SqmExtractUnit<>(
					TemporalUnit.MONTH,
					resolveExpressableTypeBasic( Integer.class ),
					nodeBuilder
			);
		}
		if ( ctx.YEAR() != null ) {
			return new SqmExtractUnit<>(
					TemporalUnit.YEAR,
					resolveExpressableTypeBasic( Integer.class ),
					nodeBuilder
			);
		}
		if ( ctx.HOUR() != null ) {
			return new SqmExtractUnit<>(
					TemporalUnit.HOUR,
					resolveExpressableTypeBasic( Integer.class ),
					nodeBuilder
			);
		}
		if ( ctx.MINUTE() != null ) {
			return new SqmExtractUnit<>(
					TemporalUnit.MINUTE,
					resolveExpressableTypeBasic( Integer.class ),
					nodeBuilder
			);
		}
		if ( ctx.SECOND() != null ) {
			return new SqmExtractUnit<>(
					TemporalUnit.SECOND,
					resolveExpressableTypeBasic( Float.class ),
					nodeBuilder
			);
		}
		if (ctx.NANOSECOND()!=null) {
			return new SqmExtractUnit<>(
					NANOSECOND,
					resolveExpressableTypeBasic( Long.class ),
					nodeBuilder
			);
		}
		if ( ctx.WEEK() != null ) {
			return new SqmExtractUnit<>(
					TemporalUnit.WEEK,
					resolveExpressableTypeBasic( Integer.class ),
					nodeBuilder
			);
		}
		if ( ctx.QUARTER() != null ) {
			return new SqmExtractUnit<>(
					TemporalUnit.QUARTER,
					resolveExpressableTypeBasic( Integer.class ),
					nodeBuilder
			);
		}

		return super.visitDatetimeField( ctx );
	}

	@Override
	public Object visitDayField(HqlParser.DayFieldContext ctx) {
		final NodeBuilder nodeBuilder = creationContext.getNodeBuilder();
		if ( ctx.WEEK() != null ) {
			return new SqmExtractUnit<>( DAY_OF_WEEK, resolveExpressableTypeBasic( Integer.class ), nodeBuilder );
		}
		if ( ctx.MONTH() != null ) {
			return new SqmExtractUnit<>( DAY_OF_MONTH, resolveExpressableTypeBasic( Integer.class ), nodeBuilder );
		}
		if ( ctx.YEAR() != null ) {
			return new SqmExtractUnit<>( DAY_OF_YEAR, resolveExpressableTypeBasic( Integer.class ), nodeBuilder );
		}
		return super.visitDayField(ctx);
	}

	@Override
	public Object visitWeekField(HqlParser.WeekFieldContext ctx) {
		final NodeBuilder nodeBuilder = creationContext.getNodeBuilder();
		if ( ctx.MONTH() != null ) {
			//this is computed from DAY_OF_MONTH/7
			return new SqmExtractUnit<>( WEEK_OF_MONTH, resolveExpressableTypeBasic( Integer.class ), nodeBuilder );
		}
		if ( ctx.YEAR() != null ) {
			//this is computed from DAY_OF_YEAR/7
			return new SqmExtractUnit<>( WEEK_OF_YEAR, resolveExpressableTypeBasic( Integer.class ), nodeBuilder );
		}
		return super.visitWeekField(ctx);
	}

	@Override
	public Object visitDateOrTimeField(HqlParser.DateOrTimeFieldContext ctx) {
		NodeBuilder nodeBuilder = creationContext.getNodeBuilder();
		if ( ctx.DATE() != null ) {
			return isExtractingJdbcTemporalType
					? new SqmExtractUnit<>( DATE, resolveExpressableTypeBasic( Date.class ), nodeBuilder )
					: new SqmExtractUnit<>( DATE, resolveExpressableTypeBasic( LocalDate.class ), nodeBuilder );
		}

		if ( ctx.TIME() != null ) {
			return isExtractingJdbcTemporalType
					? new SqmExtractUnit<>( TIME, resolveExpressableTypeBasic( Time.class ), nodeBuilder )
					: new SqmExtractUnit<>( TIME, resolveExpressableTypeBasic( LocalTime.class ), nodeBuilder );
		}

		return super.visitDateOrTimeField(ctx);
	}

	@Override
	public Object visitTimeZoneField(HqlParser.TimeZoneFieldContext ctx) {
		NodeBuilder nodeBuilder = creationContext.getNodeBuilder();
		if ( ctx.HOUR() != null ) {
			return new SqmExtractUnit<>( TIMEZONE_HOUR, resolveExpressableTypeBasic( Integer.class ), nodeBuilder );
		}
		else if ( ctx.MINUTE() != null ) {
			return new SqmExtractUnit<>( TIMEZONE_MINUTE, resolveExpressableTypeBasic( Integer.class ), nodeBuilder );
		}
		else {
			return new SqmExtractUnit<>( OFFSET, resolveExpressableTypeBasic( ZoneOffset.class ), nodeBuilder );
		}
	}

	private boolean isExtractingJdbcTemporalType;

	@Override
	public Object visitExtractFunction(HqlParser.ExtractFunctionContext ctx) {

		final SqmExpression<?> expressionToExtract = (SqmExpression<?>) ctx.expression().accept( this );

		// visitDateOrTimeField() needs to know if we're extracting from a
		// JDBC Timestamp or from a java.time LocalDateTime/OffsetDateTime
		isExtractingJdbcTemporalType = isJdbcTemporalType( expressionToExtract.getNodeType() );

		final SqmExtractUnit<?> extractFieldExpression;
		if ( ctx.extractField() != null ) {
			//for the case of the full ANSI syntax "extract(field from arg)"
			extractFieldExpression = (SqmExtractUnit) ctx.extractField().accept(this);
		}
		else if ( ctx.datetimeField() != null ) {
			//for the shorter legacy Hibernate syntax "field(arg)"
			extractFieldExpression = (SqmExtractUnit) ctx.datetimeField().accept(this);
		}
		else {
			return expressionToExtract;
		}

		return getFunctionDescriptor("extract").generateSqmExpression(
				asList( extractFieldExpression, expressionToExtract ),
				extractFieldExpression.getType(),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	// G era
	// y year in era
	// Y week year (ISO)
	// M month in year
	// w week in year (ISO)
	// W week in month
	// E day name in week
	// e day number in week (*very* inconsistent across DBs)
	// d day in month
	// D day in year
	// a AM/PM
	// H hour of day (0-23)
	// h clock hour of am/pm (1-12)
	// m minute of hour
	// s second of minute
	// S fraction of second
	// z time zone name e.g. PST
	// x zone offset e.g. +03, +0300, +03:00
	// Z zone offset e.g. +0300
	// see https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
	private static final Pattern FORMAT = Pattern.compile("('[^']+'|[:;/,.!@#$^&?~`|()\\[\\]{}<>\\-+*=]|\\s|G{1,2}|[yY]{1,4}|M{1,4}|w{1,2}|W|E{3,4}|e{1,2}|d{1,2}|D{1,3}|a{1,2}|[Hhms]{1,2}|S{1,6}|[zZx]{1,3})*");

	@Override
	public Object visitFormat(HqlParser.FormatContext ctx) {
		String format = ctx.STRING_LITERAL().getText();
		if (!FORMAT.matcher(format).matches()) {
			throw new SemanticException("illegal format pattern: '" + format + "'");
		}
		return new SqmFormat(
				format,
				resolveExpressableTypeBasic( String.class ),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmExpression<?> visitFormatFunction(HqlParser.FormatFunctionContext ctx) {
		final SqmExpression<?> expressionToCast = (SqmExpression<?>) ctx.expression().accept( this );
		final SqmLiteral<?> format = (SqmLiteral<?>) ctx.format().accept( this );

		return getFunctionDescriptor("format").generateSqmExpression(
				asList( expressionToCast, format ),
				resolveExpressableTypeBasic( String.class ),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmExpression<?> visitCastFunction(HqlParser.CastFunctionContext ctx) {
		final SqmExpression<?> expressionToCast = (SqmExpression<?>) ctx.expression().accept( this );
		final SqmCastTarget<?> castTargetExpression = (SqmCastTarget<?>) ctx.castTarget().accept( this );

		return getFunctionDescriptor("cast").generateSqmExpression(
				asList( expressionToCast, castTargetExpression ),
				castTargetExpression.getType(),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmCastTarget<?> visitCastTarget(HqlParser.CastTargetContext castTargetContext) {
		final String targetName = castTargetContext.castTargetType().fullTargetName;

		List<TerminalNode> args = castTargetContext.INTEGER_LITERAL();
		Long length = args.size() == 1 ? Long.valueOf( args.get(0).getText() ) : null;
		Integer precision = args.size()>0 ? Integer.valueOf( args.get(0).getText() ) : null;
		Integer scale = args.size()>1 ? Integer.valueOf( args.get(1).getText() ) : null;

		return new SqmCastTarget<>(
				(AllowableFunctionReturnType<?>)
						creationContext.getJpaMetamodel().getTypeConfiguration()
								.resolveCastTargetType( targetName ),
				//TODO: is there some way to interpret as length vs precision/scale here at this point?
				length,
				precision, scale,
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmExpression<?> visitUpperFunction(HqlParser.UpperFunctionContext ctx) {
		final SqmExpression<?> expression = (SqmExpression<?>) ctx.expression().accept( this );
		return getFunctionDescriptor("upper").generateSqmExpression(
				expression,
				(AllowableFunctionReturnType<?>) expression.getNodeType(),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmExpression<?> visitLowerFunction(HqlParser.LowerFunctionContext ctx) {
		// todo (6.0) : why pass both the expression and its expression-type?
		//			can't we just pass the expression?
		final SqmExpression<?> expression = (SqmExpression<?>) ctx.expression().accept( this );
		return getFunctionDescriptor("lower").generateSqmExpression(
				expression,
				(AllowableFunctionReturnType<?>) expression.getNodeType(),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmExpression<?> visitConcatFunction(HqlParser.ConcatFunctionContext ctx) {
		final List<SqmTypedNode<?>> arguments = new ArrayList<>();
		for ( HqlParser.ExpressionContext argument : ctx.expression() ) {
			arguments.add( (SqmTypedNode<?>) argument.accept( this ) );
		}

		return getFunctionDescriptor("concat").generateSqmExpression(
				arguments,
				resolveExpressableTypeBasic( String.class ),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public Object visitLengthFunction(HqlParser.LengthFunctionContext ctx) {
		final SqmExpression<?> arg = (SqmExpression<?>) ctx.expression().accept( this );

		return getFunctionDescriptor("length").generateSqmExpression(
				arg,
				resolveExpressableTypeBasic( Integer.class ),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public Object visitPositionFunction(HqlParser.PositionFunctionContext ctx) {
		final SqmExpression<?> string = (SqmExpression<?>) ctx.positionFunctionStringArgument().accept( this );
		final SqmExpression<?> pattern = (SqmExpression<?>) ctx.positionFunctionPatternArgument().accept( this );

		return getFunctionDescriptor("position").generateSqmExpression(
				asList( pattern, string ),
				resolveExpressableTypeBasic( Integer.class ),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public Object visitLocateFunction(HqlParser.LocateFunctionContext ctx) {
		final SqmExpression<?> string = (SqmExpression<?>) ctx.locateFunctionStringArgument().accept( this );
		final SqmExpression<?> pattern = (SqmExpression<?>) ctx.locateFunctionPatternArgument().accept( this );
		final SqmExpression<?> start = ctx.locateFunctionStartArgument() == null
				? null
				: (SqmExpression) ctx.locateFunctionStartArgument().accept( this );

		return getFunctionDescriptor("locate").generateSqmExpression(
				start == null
						? asList( pattern, string )
						: asList( pattern, string, start ),
				resolveExpressableTypeBasic( Integer.class ),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public Object visitOverlayFunction(HqlParser.OverlayFunctionContext ctx) {

		final SqmExpression<?> string = (SqmExpression<?>) ctx.overlayFunctionStringArgument().accept( this );
		final SqmExpression<?> replacement = (SqmExpression<?>) ctx.overlayFunctionReplacementArgument().accept( this );
		final SqmExpression<?> start = (SqmExpression<?>) ctx.overlayFunctionStartArgument().accept( this );
		final SqmExpression<?> length = ctx.overlayFunctionLengthArgument() == null
				? null
				: (SqmExpression<?>) ctx.overlayFunctionLengthArgument().accept( this );

		return getFunctionDescriptor("overlay").generateSqmExpression(
				length == null
						? asList( string, replacement, start )
						: asList( string, replacement, start, length ),
				resolveExpressableTypeBasic( String.class ),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public Object visitReplaceFunction(HqlParser.ReplaceFunctionContext ctx) {

		final SqmExpression<?> string = (SqmExpression<?>) ctx.replaceFunctionStringArgument().accept( this );
		final SqmExpression<?> pattern = (SqmExpression<?>) ctx.replaceFunctionPatternArgument().accept( this );
		final SqmExpression<?> replacement = (SqmExpression<?>) ctx.replaceFunctionReplacementArgument().accept( this );

		return getFunctionDescriptor("replace").generateSqmExpression(
				asList( string, pattern, replacement ),
				resolveExpressableTypeBasic( String.class ),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public Object visitStrFunction(HqlParser.StrFunctionContext ctx) {
		final SqmExpression<?> arg = (SqmExpression<?>) ctx.expression().accept( this );
		return getFunctionDescriptor("str").generateSqmExpression(
				singletonList( arg ),
				resolveExpressableTypeBasic( String.class ),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmExpression<?> visitMaxFunction(HqlParser.MaxFunctionContext ctx) {
		final SqmExpression<?> arg = (SqmExpression<?>) ctx.expression().accept( this );
		//ignore DISTINCT
		return getFunctionDescriptor("max").generateAggregateSqmExpression(
				singletonList( arg ),
				getFilterExpression( ctx.filterClause() ),
				(AllowableFunctionReturnType<?>) arg.getNodeType(),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmExpression<?> visitMinFunction(HqlParser.MinFunctionContext ctx) {
		final SqmExpression<?> arg = (SqmExpression<?>) ctx.expression().accept( this );
		//ignore DISTINCT
		return getFunctionDescriptor("min").generateAggregateSqmExpression(
				singletonList( arg ),
				getFilterExpression( ctx.filterClause() ),
				(AllowableFunctionReturnType<?>) arg.getNodeType(),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmExpression<?> visitSumFunction(HqlParser.SumFunctionContext ctx) {
		final SqmExpression<?> arg = (SqmExpression<?>) ctx.expression().accept( this );
		final SqmTypedNode<?> argument = ctx.DISTINCT() != null
				? new SqmDistinct<>(arg, getCreationContext().getNodeBuilder())
				: arg;

		return getFunctionDescriptor("sum").generateAggregateSqmExpression(
				singletonList( argument ),
				getFilterExpression(ctx.filterClause()),
				null,
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmExpression<?> visitEveryFunction(HqlParser.EveryFunctionContext ctx) {

		if ( ctx.subQuery()!=null ) {
			SqmSubQuery<?> subquery = (SqmSubQuery<?>) ctx.subQuery().accept(this);
			return new SqmEvery<>( subquery, creationContext.getNodeBuilder() );
		}

		final SqmExpression<?> argument = (SqmExpression<?>) ctx.predicate().accept( this );

		if ( argument instanceof SqmSubQuery<?> && ctx.filterClause() != null ) {
			throw new SemanticException( "Quantified expression cannot have a filter clause!" );
		}

		return getFunctionDescriptor("every").generateAggregateSqmExpression(
				singletonList( argument ),
				getFilterExpression( ctx.filterClause() ),
				resolveExpressableTypeBasic( Boolean.class ),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmExpression<?> visitAnyFunction(HqlParser.AnyFunctionContext ctx) {

		if ( ctx.subQuery()!=null ) {
			SqmSubQuery<?> subquery = (SqmSubQuery<?>) ctx.subQuery().accept(this);
			return new SqmAny<>( subquery, creationContext.getNodeBuilder() );
		}

		final SqmExpression<?> argument = (SqmExpression<?>) ctx.predicate().accept( this );

		if ( argument instanceof SqmSubQuery<?> && ctx.filterClause() != null ) {
			throw new SemanticException( "Quantified expression cannot have a filter clause!" );
		}

		return getFunctionDescriptor("any").generateAggregateSqmExpression(
				singletonList( argument ),
				getFilterExpression( ctx.filterClause() ),
				resolveExpressableTypeBasic( Boolean.class ),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmExpression<?> visitAvgFunction(HqlParser.AvgFunctionContext ctx) {
		final SqmExpression<?> arg = (SqmExpression<?>) ctx.expression().accept( this );
		final SqmTypedNode<?> argument = ctx.DISTINCT() != null
				? new SqmDistinct<>( arg, getCreationContext().getNodeBuilder() )
				: arg;

		return getFunctionDescriptor("avg").generateAggregateSqmExpression(
				singletonList( argument ),
				getFilterExpression( ctx.filterClause() ),
				resolveExpressableTypeBasic( Double.class ),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmExpression<?> visitCountFunction(HqlParser.CountFunctionContext ctx) {
		final SqmExpression<?> arg = ctx.ASTERISK() != null
				? new SqmStar( getCreationContext().getNodeBuilder() )
				: (SqmExpression<?>) ctx.expression().accept( this );
		final SqmTypedNode<?> argument = ctx.DISTINCT() != null
				? new SqmDistinct<>( arg, getCreationContext().getNodeBuilder() )
				: arg;

		return getFunctionDescriptor("count").generateAggregateSqmExpression(
				singletonList( argument ),
				getFilterExpression( ctx.filterClause() ),
				resolveExpressableTypeBasic( Long.class ),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	private SqmPredicate getFilterExpression(HqlParser.FilterClauseContext filterClauseCtx) {
		if (filterClauseCtx == null) {
			return null;
		}
		return (SqmPredicate) filterClauseCtx.whereClause().predicate().accept( this );
	}

	@Override
	public SqmExpression<?> visitCube(HqlParser.CubeContext ctx) {
		return new SqmSummarization<>(
				SqmSummarization.Kind.CUBE,
				visitExpressions( ctx.expression() ),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmExpression<?> visitRollup(HqlParser.RollupContext ctx) {
		return new SqmSummarization<>(
				SqmSummarization.Kind.ROLLUP,
				visitExpressions( ctx.expression() ),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmExpression<?> visitSubstringFunction(HqlParser.SubstringFunctionContext ctx) {
		final SqmExpression<?> source = (SqmExpression<?>) ctx.expression().accept( this );
		final SqmExpression<?> start = (SqmExpression<?>) ctx.substringFunctionStartArgument().accept( this );
		final SqmExpression<?> length = ctx.substringFunctionLengthArgument() == null
				? null
				: (SqmExpression<?>) ctx.substringFunctionLengthArgument().accept( this );

		return getFunctionDescriptor("substring").generateSqmExpression(
				length==null ? asList( source, start ) : asList( source, start, length ),
				resolveExpressableTypeBasic( String.class ),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmExpression<?> visitLeftFunction(HqlParser.LeftFunctionContext ctx) {
		final SqmExpression<?> source = (SqmExpression<?>) ctx.expression(0).accept( this );
		final SqmExpression<?> length = (SqmExpression<?>) ctx.expression(1).accept( this );

		return getFunctionDescriptor("left").generateSqmExpression(
				asList( source, length ),
				resolveExpressableTypeBasic( String.class ),
				creationContext.getQueryEngine(),
				creationContext.getNodeBuilder().getTypeConfiguration()
		);
	}

	@Override
	public SqmExpression<?> visitRightFunction(HqlParser.RightFunctionContext ctx) {
		final SqmExpression<?> source = (SqmExpression<?>) ctx.expression(0).accept( this );
		final SqmExpression<?> length = (SqmExpression<?>) ctx.expression(1).accept( this );

		return getFunctionDescriptor("right").generateSqmExpression(
				asList( source, length ),
				resolveExpressableTypeBasic( String.class ),
				creationContext.getQueryEngine(),
				creationContext.getNodeBuilder().getTypeConfiguration()
		);
	}

	@Override
	public SqmExpression visitPadFunction(HqlParser.PadFunctionContext ctx) {
		final SqmExpression<?> source = (SqmExpression<?>) ctx.expression().accept( this );
		SqmExpression<?> length = (SqmExpression<?>) ctx.padLength().accept(this);
		SqmTrimSpecification padSpec = visitPadSpecification( ctx.padSpecification() );
		SqmLiteral<Character> padChar = ctx.padCharacter() == null
				? null
				: visitPadCharacter( ctx.padCharacter() );
		return getFunctionDescriptor("pad").generateSqmExpression(
				padChar != null
						? asList( source, length, padSpec, padChar )
						: asList( source, length, padSpec ),
				resolveExpressableTypeBasic( String.class ),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmTrimSpecification visitPadSpecification(HqlParser.PadSpecificationContext ctx) {
		TrimSpec spec;
		if ( ctx.LEADING() != null ) {
			spec = TrimSpec.LEADING;
		}
		else {
			spec = TrimSpec.TRAILING;
		}
		return new SqmTrimSpecification( spec, creationContext.getNodeBuilder() );
	}

	@Override
	public SqmLiteral<Character> visitPadCharacter(HqlParser.PadCharacterContext ctx) {
		// todo (6.0) : we should delay this until we are walking the SQM

		final String padCharText =
				ctx != null && ctx.STRING_LITERAL() != null
						? ctx.STRING_LITERAL().getText()
						: " ";

		if ( padCharText.length() != 1 ) {
			throw new SemanticException( "Pad character for pad() function must be single character, found: " + padCharText );
		}

		return new SqmLiteral<>(
				padCharText.charAt( 0 ),
				resolveExpressableTypeBasic( Character.class ),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmExpression<?> visitTrimFunction(HqlParser.TrimFunctionContext ctx) {
		final SqmExpression<?> source = (SqmExpression<?>) ctx.expression().accept( this );
		SqmTrimSpecification trimSpec = visitTrimSpecification( ctx.trimSpecification() );
		SqmLiteral<Character> trimChar = visitTrimCharacter( ctx.trimCharacter() );

		return getFunctionDescriptor("trim").generateSqmExpression(
				asList(
						trimSpec,
						trimChar,
						source
				),
				resolveExpressableTypeBasic( String.class ),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmTrimSpecification visitTrimSpecification(HqlParser.TrimSpecificationContext ctx) {
		TrimSpec spec = TrimSpec.BOTH;	// JPA says the default is BOTH

		if ( ctx != null ) {
			if ( ctx.LEADING() != null ) {
				spec = TrimSpec.LEADING;
			}
			else if ( ctx.TRAILING() != null ) {
				spec = TrimSpec.TRAILING;
			}
		}

		return new SqmTrimSpecification( spec, creationContext.getNodeBuilder() );
	}

	@Override
	public SqmLiteral<Character> visitTrimCharacter(HqlParser.TrimCharacterContext ctx) {
		// todo (6.0) : we should delay this until we are walking the SQM

		final String trimCharText =
				ctx != null && ctx.STRING_LITERAL() != null
						? ctx.STRING_LITERAL().getText()
						: " "; // JPA says space is the default

		if ( trimCharText.length() != 1 ) {
			throw new SemanticException( "Trim character for trim() function must be single character, found: " + trimCharText );
		}

		return new SqmLiteral<>(
				trimCharText.charAt( 0 ),
				resolveExpressableTypeBasic( Character.class ),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmCollectionSize visitCollectionSizeFunction(HqlParser.CollectionSizeFunctionContext ctx) {
		return new SqmCollectionSize(
				consumeDomainPath( ctx.path() ),
				resolveExpressableTypeBasic( Integer.class ),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmPath<?> visitCollectionIndexFunction(HqlParser.CollectionIndexFunctionContext ctx) {
		final String alias = ctx.identifier().getText();

		final SqmFrom<?,?> sqmFrom = processingStateStack.getCurrent().getPathRegistry().findFromByAlias( alias );

		if ( sqmFrom == null ) {
			throw new ParsingException( "Could not resolve identification variable [" + alias + "] to SqmFrom" );
		}

		final SqmPathSource<?> pluralAttribute = sqmFrom.getReferencedPathSource();

		if ( !( pluralAttribute instanceof PluralPersistentAttribute ) ) {
			throw new ParsingException( "Could not resolve identification variable [" + alias + "] as plural-attribute" );
		}

		//noinspection unchecked
		return ( (PluralPersistentAttribute<?, ?, ?>) pluralAttribute ).getIndexPathSource().createSqmPath(
				sqmFrom
		);
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	private boolean isIndexedPluralAttribute(SqmPath<?> path) {
		return path.getReferencedPathSource() instanceof PluralPersistentAttribute;
	}

	@Override
	public SqmMaxElementPath visitMaxElementFunction(HqlParser.MaxElementFunctionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.HQL_COLLECTION_FUNCTION );
		}

		//noinspection unchecked
		return new SqmMaxElementPath( consumePluralAttributeReference( ctx.path() ) );
	}

	@Override
	public SqmMinElementPath visitMinElementFunction(HqlParser.MinElementFunctionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.HQL_COLLECTION_FUNCTION );
		}

		//noinspection unchecked
		return new SqmMinElementPath( consumePluralAttributeReference( ctx.path() ) );
	}

	@Override
	public SqmMaxIndexPath visitMaxIndexFunction(HqlParser.MaxIndexFunctionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.HQL_COLLECTION_FUNCTION );
		}

		final SqmPath pluralPath = consumePluralAttributeReference( ctx.path() );
		if ( !isIndexedPluralAttribute( pluralPath ) ) {
			throw new SemanticException(
					"maxindex() function can only be applied to path expressions which resolve to an " +
							"indexed collection (list,map); specified path [" + ctx.path().getText() +
							"] resolved to " + pluralPath.getReferencedPathSource()
			);
		}

		//noinspection unchecked
		return new SqmMaxIndexPath( pluralPath );
	}

	@Override
	public SqmMinIndexPath visitMinIndexFunction(HqlParser.MinIndexFunctionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.HQL_COLLECTION_FUNCTION );
		}

		final SqmPath pluralPath = consumePluralAttributeReference( ctx.path() );
		if ( !isIndexedPluralAttribute( pluralPath ) ) {
			throw new SemanticException(
					"minindex() function can only be applied to path expressions which resolve to an " +
							"indexed collection (list,map); specified path [" + ctx.path().getText() +
							"] resolved to " + pluralPath.getReferencedPathSource()
			);
		}

		//noinspection unchecked
		return new SqmMinIndexPath( pluralPath );
	}

	@Override
	public SqmSubQuery<?> visitSubQueryExpression(HqlParser.SubQueryExpressionContext ctx) {
		return visitSubQuery( ctx.subQuery() );
	}

	@Override
	public SqmSubQuery<?> visitSubQuery(HqlParser.SubQueryContext ctx) {
		final HqlParser.QueryExpressionContext queryExpressionContext = ctx.queryExpression();
		final SqmSubQuery<?> subQuery = new SqmSubQuery<>(
				processingStateStack.getCurrent().getProcessingQuery(),
				creationContext.getNodeBuilder()
		);

		processingStateStack.push(
				new SqmQuerySpecCreationProcessingStateStandardImpl(
						processingStateStack.getCurrent(),
						subQuery,
						this
				)
		);

		try {
			queryExpressionContext.accept( this );

			final List<SqmSelection> selections = subQuery.getQuerySpec().getSelectClause().getSelections();
			if ( selections.size() == 1 ) {
				subQuery.applyInferableType( selections.get( 0 ).getNodeType() );
			}

			return subQuery;
		}
		finally {
			processingStateStack.pop();
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Path structures

	@Override
	public SemanticPathPart visitPath(HqlParser.PathContext ctx) {
		if ( ctx.syntacticDomainPath() != null ) {
			final SemanticPathPart syntacticNavigablePathResult = visitSyntacticDomainPath( ctx.syntacticDomainPath() );
			if ( ctx.pathContinuation() != null ) {
				dotIdentifierConsumerStack.push(
						new BasicDotIdentifierConsumer( syntacticNavigablePathResult, this ) {
							@Override
							protected void reset() {
							}
						}
				);
				try {
					return (SemanticPathPart) ctx.pathContinuation().accept( this );
				}
				finally {
					dotIdentifierConsumerStack.pop();
				}
			}
			return syntacticNavigablePathResult;
		}
		else if ( ctx.generalPathFragment() != null ) {
			return (SemanticPathPart) ctx.generalPathFragment().accept( this );
		}

		throw new ParsingException( "Unrecognized `path` rule branch" );
	}

	@Override
	public SemanticPathPart visitGeneralPathFragment(HqlParser.GeneralPathFragmentContext ctx) {
		return visitDotIdentifierSequence( ctx.dotIdentifierSequence() );
	}

	@Override
	public SemanticPathPart visitSyntacticDomainPath(HqlParser.SyntacticDomainPathContext ctx) {
		if ( ctx.treatedNavigablePath() != null ) {
			return visitTreatedNavigablePath( ctx.treatedNavigablePath() );
		}
		else if ( ctx.collectionElementNavigablePath() != null ) {
			return visitCollectionElementNavigablePath( ctx.collectionElementNavigablePath() );
		}
		else if ( ctx.mapKeyNavigablePath() != null ) {
			return visitMapKeyNavigablePath( ctx.mapKeyNavigablePath() );
		}
		else if ( ctx.dotIdentifierSequence() != null && ctx.indexedPathAccessFragment() != null ) {
			dotIdentifierConsumerStack.push(
					new QualifiedJoinPathConsumer(
							(SqmRoot<?>) dotIdentifierConsumerStack.getCurrent().getConsumedPart(),
							SqmJoinType.INNER,
							false,
							null,
							this
					)
			);

			final SqmAttributeJoin<?, ?> indexedJoinPath;
			try {
				indexedJoinPath = (SqmAttributeJoin<?, ?>) ctx.dotIdentifierSequence().accept( this );
			}
			finally {
				dotIdentifierConsumerStack.pop();
			}
			dotIdentifierConsumerStack.push(
					new BasicDotIdentifierConsumer( indexedJoinPath, this ) {
						@Override
						protected void reset() {
						}
					}
			);
			try {
				return (SemanticPathPart) ctx.indexedPathAccessFragment().accept( this );
			}
			finally {
				dotIdentifierConsumerStack.pop();
			}
		}

		throw new ParsingException( "Unsure how to process `syntacticDomainPath` over : " + ctx.getText() );
	}

	@Override
	public SemanticPathPart visitIndexedPathAccessFragment(HqlParser.IndexedPathAccessFragmentContext ctx) {
		final DotIdentifierConsumer consumer = dotIdentifierConsumerStack.pop();
		final SqmExpression<?> indexExpression = (SqmExpression<?>) ctx.expression().accept( this );
		final SqmAttributeJoin<?, ?> attributeJoin = (SqmAttributeJoin<?, ?>) consumer.getConsumedPart();
		final SqmExpression<?> index;
		if ( attributeJoin instanceof SqmListJoin<?, ?> ) {
			index = ( (SqmListJoin<?, ?>) attributeJoin ).index();
		}
		else if ( attributeJoin instanceof SqmMapJoin<?, ?, ?> ) {
			index = ( (SqmMapJoin<?, ?, ?>) attributeJoin ).key();
		}
		else {
			throw new SemanticException( "Index access is only supported on list or map attributes: " + attributeJoin.getNavigablePath() );
		}
		attributeJoin.setJoinPredicate( creationContext.getNodeBuilder().equal( index, indexExpression ) );
		final SqmIndexedCollectionAccessPath<?> path = new SqmIndexedCollectionAccessPath<>(
				attributeJoin,
				indexExpression
		);
		dotIdentifierConsumerStack.push(
				new BasicDotIdentifierConsumer( path, this ) {
					@Override
					protected void reset() {
					}
				}
		);
		HqlParser.GeneralPathFragmentContext generalPathFragmentContext = ctx.generalPathFragment();
		if ( generalPathFragmentContext == null ) {
			return path;
		}
		return (SemanticPathPart) generalPathFragmentContext.accept( this );
	}

	@Override
	public SemanticPathPart visitDotIdentifierSequence(HqlParser.DotIdentifierSequenceContext ctx) {
		final int numberOfContinuations = ctx.dotIdentifierSequenceContinuation().size();
		final boolean hasContinuations = numberOfContinuations != 0;

		final DotIdentifierConsumer dotIdentifierConsumer = dotIdentifierConsumerStack.getCurrent();

		assert ctx.identifier().getChildCount() == 1;

		dotIdentifierConsumer.consumeIdentifier(
				ctx.identifier().getChild( 0 ).getText(),
				true,
				! hasContinuations
		);

		if ( hasContinuations ) {
			int i = 1;
			for ( HqlParser.DotIdentifierSequenceContinuationContext continuation : ctx.dotIdentifierSequenceContinuation() ) {
				assert continuation.identifier().getChildCount() == 1;
				dotIdentifierConsumer.consumeIdentifier(
						continuation.identifier().getChild( 0 ).getText(),
						false,
						i++ >= numberOfContinuations
				);
			}
		}

		return dotIdentifierConsumer.getConsumedPart();
	}

	@Override
	public Object visitDotIdentifierSequenceContinuation(HqlParser.DotIdentifierSequenceContinuationContext ctx) {
		return super.visitDotIdentifierSequenceContinuation( ctx );
	}


	@Override
	public SqmPath<?> visitTreatedNavigablePath(HqlParser.TreatedNavigablePathContext ctx) {
		final SqmPath<?> sqmPath = consumeManagedTypeReference( ctx.path() );

		final String treatTargetName = ctx.dotIdentifierSequence().getText();
		final String treatTargetEntityName = getCreationContext().getJpaMetamodel().qualifyImportableName( treatTargetName );
		final EntityDomainType<?> treatTarget = getCreationContext().getJpaMetamodel().entity( treatTargetEntityName );

		SqmPath<?> result = resolveTreatedPath( sqmPath, treatTarget );

		if ( ctx.pathContinuation() != null ) {
			dotIdentifierConsumerStack.push(
					new BasicDotIdentifierConsumer( result, this ) {
						@Override
						protected void reset() {
						}
					}
			);
			try {
				result = consumeDomainPath( ctx.pathContinuation().dotIdentifierSequence() );
			}
			finally {
				dotIdentifierConsumerStack.pop();
			}
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	private SqmTreatedPath resolveTreatedPath(SqmPath<?> sqmPath, EntityDomainType<?> treatTarget) {
		return sqmPath.treatAs( (EntityDomainType) treatTarget );
	}

	@Override
	public SqmPath<?> visitCollectionElementNavigablePath(HqlParser.CollectionElementNavigablePathContext ctx) {
		final SqmPath<?> pluralAttributePath = consumeDomainPath( ctx.path() );
		final SqmPathSource<?> referencedPathSource = pluralAttributePath.getReferencedPathSource();

		if ( !(referencedPathSource instanceof PluralPersistentAttribute ) ) {
			throw new PathException(
					"Illegal attempt to treat non-plural path as a plural path : " + pluralAttributePath.getNavigablePath()
			);
		}

		final PluralPersistentAttribute attribute = (PluralPersistentAttribute) referencedPathSource;

		if ( getCreationOptions().useStrictJpaCompliance() ) {
			if ( attribute.getCollectionClassification() != CollectionClassification.MAP ) {
				throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.VALUE_FUNCTION_ON_NON_MAP );
			}
		}

		//noinspection unchecked
		SqmPath result = attribute.getElementPathSource().createSqmPath(
				pluralAttributePath
		);

		if ( ctx.pathContinuation() != null ) {
			result = consumeDomainPath( ctx.path() );
		}

		return result;
	}


	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public SqmPath visitMapKeyNavigablePath(HqlParser.MapKeyNavigablePathContext ctx) {
		final SqmPath<?> sqmPath = consumeDomainPath( ctx.path() );

		if ( sqmPath instanceof SqmMapJoin ) {
			final SqmMapJoin sqmMapJoin = (SqmMapJoin) sqmPath;
			return sqmMapJoin.getReferencedPathSource().getIndexPathSource().createSqmPath( sqmMapJoin );
		}
		else {
			assert sqmPath instanceof SqmPluralValuedSimplePath;
			final SqmPluralValuedSimplePath mapPath = (SqmPluralValuedSimplePath) sqmPath;
			final SqmPath keyPath = mapPath.getReferencedPathSource()
					.getIndexPathSource()
					.createSqmPath( mapPath );
			mapPath.registerReusablePath( keyPath );
			return keyPath;
		}
	}

	private SqmPath consumeDomainPath(HqlParser.PathContext parserPath) {
		final SemanticPathPart consumedPart = (SemanticPathPart) parserPath.accept( this );
		if ( consumedPart instanceof SqmPath ) {
			return (SqmPath) consumedPart;
		}

		throw new SemanticException( "Expecting domain-model path, but found : " + consumedPart );
	}


	private SqmPath consumeDomainPath(HqlParser.DotIdentifierSequenceContext sequence) {
		final SemanticPathPart consumedPart = (SemanticPathPart) sequence.accept( this );
		if ( consumedPart instanceof SqmPath ) {
			return (SqmPath) consumedPart;
		}

		throw new SemanticException( "Expecting domain-model path, but found : " + consumedPart );
	}

	private SqmPath consumeManagedTypeReference(HqlParser.PathContext parserPath) {
		final SqmPath sqmPath = consumeDomainPath( parserPath );

		final SqmPathSource pathSource = sqmPath.getReferencedPathSource();

		try {
			// use the `#sqmAs` call to validate the path is a ManagedType
			pathSource.sqmAs( ManagedDomainType.class );
			return sqmPath;
		}
		catch (Exception e) {
			throw new SemanticException( "Expecting ManagedType valued path [" + sqmPath.getNavigablePath() + "], but found : " + pathSource.getSqmPathType() );
		}
	}

	private SqmPath consumePluralAttributeReference(HqlParser.PathContext parserPath) {
		final SqmPath sqmPath = consumeDomainPath( parserPath );

		if ( sqmPath.getReferencedPathSource() instanceof PluralPersistentAttribute ) {
			return sqmPath;
		}

		throw new SemanticException( "Expecting plural attribute valued path [" + sqmPath.getNavigablePath() + "], but found : " + sqmPath.getReferencedPathSource().getSqmPathType() );
	}

	private interface TreatHandler {
		void addDowncast(SqmFrom sqmFrom, IdentifiableDomainType downcastTarget);
	}

	private static class TreatHandlerNormal implements TreatHandler {
		private final DowncastLocation downcastLocation;

		public TreatHandlerNormal() {
			this( DowncastLocation.OTHER );
		}

		public TreatHandlerNormal(DowncastLocation downcastLocation) {
			this.downcastLocation = downcastLocation;
		}

		@Override
		public void addDowncast(
				SqmFrom sqmFrom,
				IdentifiableDomainType downcastTarget) {
//			( (MutableUsageDetails) sqmFrom.getUsageDetails() ).addDownCast( false, downcastTarget, downcastLocation );
			throw new NotYetImplementedFor6Exception();
		}
	}

	private static class TreatHandlerFromClause implements TreatHandler {
		@Override
		public void addDowncast(
				SqmFrom sqmFrom,
				IdentifiableDomainType downcastTarget) {
//			( (MutableUsageDetails) sqmFrom.getUsageDetails() ).addDownCast( true, downcastTarget, DowncastLocation.FROM );
			throw new NotYetImplementedFor6Exception();
		}
	}
	
	private void checkFQNEntityNameJpaComplianceViolationIfNeeded(String name, EntityDomainType entityDescriptor) {
		if ( getCreationOptions().useStrictJpaCompliance() && ! name.equals( entityDescriptor.getName() ) ) {
			// FQN is the only possible reason
			throw new StrictJpaComplianceViolation("Encountered FQN entity name [" + name + "], " +
					"but strict JPQL compliance was requested ( [" + entityDescriptor.getName() + "] should be used instead )",
					StrictJpaComplianceViolation.Type.FQN_ENTITY_NAME
			);
		}
	}
}
