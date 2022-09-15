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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.Bindable;
import jakarta.persistence.metamodel.SingularAttribute;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.QueryException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.grammars.hql.HqlLexer;
import org.hibernate.grammars.hql.HqlParser;
import org.hibernate.grammars.hql.HqlParserBaseVisitor;
import org.hibernate.internal.util.CharSequenceHelper;
import org.hibernate.internal.util.QuotingHelper;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.internal.AnyDiscriminatorSqmPath;
import org.hibernate.metamodel.model.domain.internal.EntitySqmPathSource;
import org.hibernate.query.PathException;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.SemanticException;
import org.hibernate.query.hql.HqlLogging;
import org.hibernate.query.hql.spi.DotIdentifierConsumer;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmCreationOptions;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.hql.spi.SqmPathRegistry;
import org.hibernate.query.sqm.BinaryArithmeticOperator;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.FetchClauseType;
import org.hibernate.query.sqm.FrameExclusion;
import org.hibernate.query.sqm.FrameKind;
import org.hibernate.query.sqm.FrameMode;
import org.hibernate.query.sqm.LiteralNumberFormatException;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.NullPrecedence;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.query.sqm.SortOrder;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.SqmTreeCreationLogger;
import org.hibernate.query.sqm.StrictJpaComplianceViolation;
import org.hibernate.query.sqm.TemporalUnit;
import org.hibernate.query.sqm.TrimSpec;
import org.hibernate.query.sqm.UnaryArithmeticOperator;
import org.hibernate.query.sqm.UnknownEntityException;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.function.NamedSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.internal.ParameterCollector;
import org.hibernate.query.sqm.internal.SqmCreationProcessingStateImpl;
import org.hibernate.query.sqm.internal.SqmDmlCreationProcessingState;
import org.hibernate.query.sqm.internal.SqmQueryPartCreationProcessingStateStandardImpl;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctions;
import org.hibernate.query.sqm.spi.ParameterDeclarationContext;
import org.hibernate.query.sqm.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.SqmQuery;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.AbstractSqmFrom;
import org.hibernate.query.sqm.tree.domain.SqmCorrelation;
import org.hibernate.query.sqm.tree.domain.SqmDerivedRoot;
import org.hibernate.query.sqm.tree.domain.SqmElementAggregateFunction;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmFkExpression;
import org.hibernate.query.sqm.tree.domain.SqmIndexAggregateFunction;
import org.hibernate.query.sqm.tree.domain.SqmListJoin;
import org.hibernate.query.sqm.tree.domain.SqmMapEntryReference;
import org.hibernate.query.sqm.tree.domain.SqmMapJoin;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPolymorphicRootDescriptor;
import org.hibernate.query.sqm.tree.expression.SqmAliasedNodeRef;
import org.hibernate.query.sqm.tree.expression.SqmAny;
import org.hibernate.query.sqm.tree.expression.SqmAnyDiscriminatorValue;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmByUnit;
import org.hibernate.query.sqm.tree.expression.SqmCaseSearched;
import org.hibernate.query.sqm.tree.expression.SqmCaseSimple;
import org.hibernate.query.sqm.tree.expression.SqmCastTarget;
import org.hibernate.query.sqm.tree.expression.SqmCollation;
import org.hibernate.query.sqm.tree.expression.SqmCollectionSize;
import org.hibernate.query.sqm.tree.expression.SqmDistinct;
import org.hibernate.query.sqm.tree.expression.SqmDurationUnit;
import org.hibernate.query.sqm.tree.expression.SqmEvery;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmExtractUnit;
import org.hibernate.query.sqm.tree.expression.SqmFormat;
import org.hibernate.query.sqm.tree.expression.SqmFunction;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.SqmLiteralEntityType;
import org.hibernate.query.sqm.tree.expression.SqmLiteralNull;
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmOver;
import org.hibernate.query.sqm.tree.expression.SqmOverflow;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.expression.SqmParameterizedEntityType;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;
import org.hibernate.query.sqm.tree.expression.SqmStar;
import org.hibernate.query.sqm.tree.expression.SqmSummarization;
import org.hibernate.query.sqm.tree.expression.SqmToDuration;
import org.hibernate.query.sqm.tree.expression.SqmTrimSpecification;
import org.hibernate.query.sqm.tree.expression.SqmTuple;
import org.hibernate.query.sqm.tree.expression.SqmUnaryOperation;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmDerivedJoin;
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
import org.hibernate.query.sqm.tree.predicate.SqmBetweenPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmBooleanExpressionPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmComparisonPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmEmptinessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmExistsPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmGroupedPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInListPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInSubQueryPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmJunctionPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmLikePredicate;
import org.hibernate.query.sqm.tree.predicate.SqmMemberOfPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNegatablePredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNegatedPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNullnessPredicate;
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
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;

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
import static org.hibernate.grammars.hql.HqlParser.ELEMENTS;
import static org.hibernate.grammars.hql.HqlParser.EXCEPT;
import static org.hibernate.grammars.hql.HqlParser.IDENTIFIER;
import static org.hibernate.grammars.hql.HqlParser.INDICES;
import static org.hibernate.grammars.hql.HqlParser.INTERSECT;
import static org.hibernate.grammars.hql.HqlParser.ListaggFunctionContext;
import static org.hibernate.grammars.hql.HqlParser.OnOverflowClauseContext;
import static org.hibernate.grammars.hql.HqlParser.PLUS;
import static org.hibernate.grammars.hql.HqlParser.UNION;
import static org.hibernate.query.sqm.TemporalUnit.DATE;
import static org.hibernate.query.sqm.TemporalUnit.DAY_OF_MONTH;
import static org.hibernate.query.sqm.TemporalUnit.DAY_OF_WEEK;
import static org.hibernate.query.sqm.TemporalUnit.DAY_OF_YEAR;
import static org.hibernate.query.sqm.TemporalUnit.NANOSECOND;
import static org.hibernate.query.sqm.TemporalUnit.OFFSET;
import static org.hibernate.query.sqm.TemporalUnit.TIME;
import static org.hibernate.query.sqm.TemporalUnit.TIMEZONE_HOUR;
import static org.hibernate.query.sqm.TemporalUnit.TIMEZONE_MINUTE;
import static org.hibernate.query.sqm.TemporalUnit.WEEK_OF_MONTH;
import static org.hibernate.query.sqm.TemporalUnit.WEEK_OF_YEAR;
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
	private static final Set<String> JPA_STANDARD_FUNCTIONS;

	static {
		final Set<String> jpaStandardFunctions = new HashSet<>();
		// Extracted from the BNF in JPA spec 4.14.
		jpaStandardFunctions.add( StandardFunctions.AVG );
		jpaStandardFunctions.add( StandardFunctions.MAX );
		jpaStandardFunctions.add( StandardFunctions.MIN );
		jpaStandardFunctions.add( StandardFunctions.SUM );
		jpaStandardFunctions.add( StandardFunctions.COUNT );
		jpaStandardFunctions.add( StandardFunctions.LENGTH );
		jpaStandardFunctions.add( StandardFunctions.LOCATE );
		jpaStandardFunctions.add( StandardFunctions.ABS );
		jpaStandardFunctions.add( StandardFunctions.SQRT );
		jpaStandardFunctions.add( StandardFunctions.MOD );
		jpaStandardFunctions.add( StandardFunctions.CURRENT_DATE );
		jpaStandardFunctions.add( StandardFunctions.CURRENT_TIME );
		jpaStandardFunctions.add( StandardFunctions.CURRENT_TIMESTAMP );
		jpaStandardFunctions.add( StandardFunctions.CONCAT );
		jpaStandardFunctions.add( StandardFunctions.SUBSTRING );
		jpaStandardFunctions.add( StandardFunctions.TRIM );
		jpaStandardFunctions.add( StandardFunctions.LOWER );
		jpaStandardFunctions.add( StandardFunctions.UPPER );
		jpaStandardFunctions.add( StandardFunctions.COALESCE );
		jpaStandardFunctions.add( StandardFunctions.NULLIF );
		JPA_STANDARD_FUNCTIONS = jpaStandardFunctions;
	}

	/**
	 * Main entry point into analysis of HQL/JPQL parse tree - producing
	 * a semantic model of the query.
	 */
	public static <R> SqmStatement<R> buildSemanticModel(
			HqlParser.StatementContext hqlParseTree,
			Class<R> expectedResultType,
			SqmCreationOptions creationOptions,
			SqmCreationContext creationContext) {
		return new SemanticQueryBuilder<R>( expectedResultType, creationOptions, creationContext ).visitStatement( hqlParseTree );
	}

	private final Class<R> expectedResultType;
	private final SqmCreationOptions creationOptions;
	private final SqmCreationContext creationContext;

	private final Stack<DotIdentifierConsumer> dotIdentifierConsumerStack;

	private final Stack<ParameterDeclarationContext> parameterDeclarationContextStack = new StandardStack<>();
	private final Stack<SqmCreationProcessingState> processingStateStack = new StandardStack<>();

	private final BasicDomainType<Integer> integerDomainType;
	private final JavaType<List<?>> listJavaType;
	private final JavaType<Map<?,?>> mapJavaType;

	private ParameterCollector parameterCollector;
	private ParameterStyle parameterStyle;

	public SemanticQueryBuilder(
			Class<R> expectedResultType,
			SqmCreationOptions creationOptions,
			SqmCreationContext creationContext) {
		this.expectedResultType = expectedResultType;
		this.creationOptions = creationOptions;
		this.creationContext = creationContext;
		this.dotIdentifierConsumerStack = new StandardStack<>( new BasicDotIdentifierConsumer( this ) );
		this.parameterStyle = creationOptions.useStrictJpaCompliance()
				? ParameterStyle.UNKNOWN
				: ParameterStyle.MIXED;

		this.integerDomainType = creationContext
				.getNodeBuilder()
				.getTypeConfiguration()
				.standardBasicTypeForJavaType( Integer.class );
		this.listJavaType = creationContext
				.getNodeBuilder()
				.getTypeConfiguration()
				.getJavaTypeRegistry()
				.resolveDescriptor( List.class );
		this.mapJavaType = creationContext
				.getNodeBuilder()
				.getTypeConfiguration()
				.getJavaTypeRegistry()
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

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Grammar rules

	@Override
	public SqmStatement<R> visitStatement(HqlParser.StatementContext ctx) {
		// parameters allow multi-valued bindings only in very limited cases, so for
		// the base case here we say false
		parameterDeclarationContextStack.push( () -> false );

		try {
			final ParseTree parseTree = ctx.getChild( 0 );
			if ( parseTree instanceof HqlParser.SelectStatementContext ) {
				final SqmSelectStatement<R> selectStatement = visitSelectStatement( (HqlParser.SelectStatementContext) parseTree );
				selectStatement.getQueryPart().validateQueryStructureAndFetchOwners();
				return selectStatement;
			}
			else if ( parseTree instanceof HqlParser.InsertStatementContext ) {
				return visitInsertStatement( (HqlParser.InsertStatementContext) parseTree );
			}
			else if ( parseTree instanceof HqlParser.UpdateStatementContext ) {
				return visitUpdateStatement( (HqlParser.UpdateStatementContext) parseTree );
			}
			else if ( parseTree instanceof HqlParser.DeleteStatementContext ) {
				return visitDeleteStatement( (HqlParser.DeleteStatementContext) parseTree );
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
				new SqmQueryPartCreationProcessingStateStandardImpl(
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
	public SqmRoot<R> visitTargetEntity(HqlParser.TargetEntityContext dmlTargetContext) {
		final HqlParser.EntityNameContext entityNameContext = (HqlParser.EntityNameContext) dmlTargetContext.getChild( 0 );
		final String identificationVariable;
		if ( dmlTargetContext.getChildCount() == 1 ) {
			identificationVariable = null;
		}
		else {
			identificationVariable = applyJpaCompliance(
					visitVariable(
							(HqlParser.VariableContext) dmlTargetContext.getChild( 1 )
					)
			);
		}
		//noinspection unchecked
		return new SqmRoot<>(
				(EntityDomainType<R>) visitEntityName( entityNameContext ),
				identificationVariable,
				false,
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmInsertStatement<R> visitInsertStatement(HqlParser.InsertStatementContext ctx) {
		final int dmlTargetIndex;
		if ( ctx.getChild( 1 ) instanceof HqlParser.TargetEntityContext ) {
			dmlTargetIndex = 1;
		}
		else {
			dmlTargetIndex = 2;
		}
		final HqlParser.TargetEntityContext dmlTargetContext = (HqlParser.TargetEntityContext) ctx.getChild( dmlTargetIndex );
		final HqlParser.TargetFieldsContext targetFieldsSpecContext = (HqlParser.TargetFieldsContext) ctx.getChild(
				dmlTargetIndex + 1
		);
		final SqmRoot<R> root = visitTargetEntity( dmlTargetContext );
		if ( root.getModel() instanceof SqmPolymorphicRootDescriptor<?> ) {
			throw new SemanticException(
					String.format(
							"Target type '%s' in insert statement is not an entity",
							root.getModel().getHibernateEntityName()
					)
			);
		}

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
					for ( HqlParser.SimplePathContext stateFieldCtx : targetFieldsSpecContext.simplePath() ) {
						final SqmPath<?> stateField = (SqmPath<?>) visitSimplePath( stateFieldCtx );
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
				final HqlParser.ValuesListContext valuesListContext = ctx.valuesList();
				for ( int i = 1; i < valuesListContext.getChildCount(); i += 2 ) {
					final ParseTree values = valuesListContext.getChild( i );
					final SqmValues sqmValues = new SqmValues();
					for ( int j = 1; j < values.getChildCount(); j += 2 ) {
						sqmValues.getExpressions().add( (SqmExpression<?>) values.getChild( j ).accept( this ) );
					}
					insertStatement.getValuesList().add( sqmValues );
				}

				for ( HqlParser.SimplePathContext stateFieldCtx : targetFieldsSpecContext.simplePath() ) {
					final SqmPath<?> stateField = (SqmPath<?>) visitSimplePath( stateFieldCtx );
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
		final boolean versioned = !( ctx.getChild( 1 ) instanceof HqlParser.TargetEntityContext );
		final int dmlTargetIndex = versioned ? 2 : 1;
		final HqlParser.TargetEntityContext dmlTargetContext = (HqlParser.TargetEntityContext) ctx.getChild( dmlTargetIndex );
		final SqmRoot<R> root = visitTargetEntity( dmlTargetContext );
		if ( root.getModel() instanceof SqmPolymorphicRootDescriptor<?> ) {
			throw new SemanticException(
					String.format(
							"Target type '%s' in update statement is not an entity",
							root.getModel().getHibernateEntityName()
					)
			);
		}

		final SqmUpdateStatement<R> updateStatement = new SqmUpdateStatement<>( root, creationContext.getNodeBuilder() );
		parameterCollector = updateStatement;
		final SqmDmlCreationProcessingState processingState = new SqmDmlCreationProcessingState(
				updateStatement,
				this
		);
		processingStateStack.push( processingState );
		processingState.getPathRegistry().register( root );

		try {
			updateStatement.versioned( versioned );
			final HqlParser.SetClauseContext setClauseCtx = (HqlParser.SetClauseContext) ctx.getChild( dmlTargetIndex + 1 );
			for ( ParseTree subCtx : setClauseCtx.children ) {
				if ( subCtx instanceof HqlParser.AssignmentContext ) {
					final HqlParser.AssignmentContext assignmentContext = (HqlParser.AssignmentContext) subCtx;
					//noinspection unchecked
					updateStatement.applyAssignment(
							(SqmPath<Object>) consumeDomainPath( (HqlParser.SimplePathContext) assignmentContext.getChild( 0 ) ),
							(SqmExpression<?>) assignmentContext.getChild( 2 ).accept( this )
					);
				}
			}

			if ( dmlTargetIndex + 2 <= ctx.getChildCount() ) {
				updateStatement.applyPredicate(
						visitWhereClause( (HqlParser.WhereClauseContext) ctx.getChild( dmlTargetIndex + 2 ) )
				);
			}

			return updateStatement;
		}
		finally {
			processingStateStack.pop();
		}
	}

	@Override
	public SqmDeleteStatement<R> visitDeleteStatement(HqlParser.DeleteStatementContext ctx) {
		final int dmlTargetIndex;
		if ( ctx.getChild( 1 ) instanceof HqlParser.TargetEntityContext ) {
			dmlTargetIndex = 1;
		}
		else {
			dmlTargetIndex = 2;
		}
		final HqlParser.TargetEntityContext dmlTargetContext = (HqlParser.TargetEntityContext) ctx.getChild( dmlTargetIndex );
		final SqmRoot<R> root = visitTargetEntity( dmlTargetContext );

		final SqmDeleteStatement<R> deleteStatement = new SqmDeleteStatement<>( root, SqmQuerySource.HQL, creationContext.getNodeBuilder() );

		parameterCollector = deleteStatement;

		final SqmDmlCreationProcessingState sqmDeleteCreationState = new SqmDmlCreationProcessingState(
				deleteStatement,
				this
		);

		sqmDeleteCreationState.getPathRegistry().register( root );

		processingStateStack.push( sqmDeleteCreationState );
		try {
			if ( dmlTargetIndex + 1 <= ctx.getChildCount() ) {
				deleteStatement.applyPredicate(
						visitWhereClause( (HqlParser.WhereClauseContext) ctx.getChild( dmlTargetIndex + 1 ) )
				);
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
		return (SqmQueryPart<Object>) ctx.getChild( 0 ).accept( this );
	}

	@Override
	public SqmQueryPart<Object> visitQuerySpecExpression(HqlParser.QuerySpecExpressionContext ctx) {
		final List<ParseTree> children = ctx.children;
		final SqmQueryPart<Object> queryPart = visitQuery( (HqlParser.QueryContext) children.get( 0 ) );
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
			final SqmCreationProcessingState firstProcessingState = processingStateStack.pop();
			processingStateStack.push(
					new SqmQueryPartCreationProcessingStateStandardImpl(
							processingStateStack.getCurrent(),
							firstProcessingState.getProcessingQuery(),
							this
					)
			);
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
		final int size = children.size();
		final SqmCreationProcessingState firstProcessingState = processingStateStack.pop();
		for ( int i = 1; i < size; i += 2 ) {
			final SetOperator operator = visitSetOperator( (HqlParser.SetOperatorContext) children.get( i ) );
			final HqlParser.OrderedQueryContext simpleQueryCtx =
					(HqlParser.OrderedQueryContext) children.get( i + 1 );
			final List<SqmQueryPart<Object>> queryParts;
			processingStateStack.push(
					new SqmQueryPartCreationProcessingStateStandardImpl(
							processingStateStack.getCurrent(),
							firstProcessingState.getProcessingQuery(),
							this
					)
			);
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
				final List<ParseTree> subChildren = simpleQueryCtx.children;
				if ( subChildren.get( 0 ) instanceof HqlParser.QueryContext ) {
					final SqmQuerySpec<Object> querySpec = new SqmQuerySpec<>( creationContext.getNodeBuilder() );
					queryParts.add( querySpec );
					visitQuerySpecExpression( (HqlParser.QuerySpecExpressionContext) simpleQueryCtx );
				}
				else {
					try {
						final SqmSelectStatement<Object> selectStatement = new SqmSelectStatement<>( creationContext.getNodeBuilder() );
						processingStateStack.push(
								new SqmQueryPartCreationProcessingStateStandardImpl(
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
		final HqlParser.OrderByClauseContext orderByClauseContext = (HqlParser.OrderByClauseContext) ctx.getChild( 0 );
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

		int currentIndex = 1;
		final HqlParser.LimitClauseContext limitClauseContext;
		if ( currentIndex < ctx.getChildCount() && ctx.getChild( currentIndex ) instanceof HqlParser.LimitClauseContext ) {
			limitClauseContext = (HqlParser.LimitClauseContext) ctx.getChild( currentIndex++ );
		}
		else {
			limitClauseContext = null;
		}
		final HqlParser.OffsetClauseContext offsetClauseContext;
		if ( currentIndex < ctx.getChildCount() && ctx.getChild( currentIndex ) instanceof HqlParser.OffsetClauseContext ) {
			offsetClauseContext = (HqlParser.OffsetClauseContext) ctx.getChild( currentIndex++ );
		}
		else {
			offsetClauseContext = null;
		}
		final HqlParser.FetchClauseContext fetchClauseContext;
		if ( currentIndex < ctx.getChildCount() && ctx.getChild( currentIndex ) instanceof HqlParser.FetchClauseContext ) {
			fetchClauseContext = (HqlParser.FetchClauseContext) ctx.getChild( currentIndex++ );
		}
		else {
			fetchClauseContext = null;
		}
		if ( currentIndex != 1 ) {
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
				throw new SemanticException("Can't use both limit and fetch clause" );
			}
		}
	}

	@Override
	public SqmQuerySpec<Object> visitQuery(HqlParser.QueryContext ctx) {
		//noinspection unchecked
		final SqmQuerySpec<Object> sqmQuerySpec = (SqmQuerySpec<Object>) currentQuerySpec();
		final int fromIndex;
		if ( ctx.getChild( 0 ) instanceof HqlParser.FromClauseContext ) {
			fromIndex = 0;
		}
		else {
			fromIndex = 1;
		}

		// visit from-clause first!!!
		visitFromClause( (HqlParser.FromClauseContext) ctx.getChild( fromIndex ) );

		final SqmSelectClause selectClause;
		if ( fromIndex == 1 ) {
			selectClause = visitSelectClause( (HqlParser.SelectClauseContext) ctx.getChild( 0 ) );
		}
		else if ( ctx.getChild( ctx.getChildCount() - 1 ) instanceof HqlParser.SelectClauseContext ) {
			selectClause = visitSelectClause( (HqlParser.SelectClauseContext) ctx.getChild( ctx.getChildCount() - 1 ) );
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

		int currentIndex = fromIndex + 1;
		final SqmWhereClause whereClause = new SqmWhereClause( creationContext.getNodeBuilder() );
		if ( currentIndex < ctx.getChildCount() && ctx.getChild( currentIndex ) instanceof HqlParser.WhereClauseContext ) {
			whereClause.setPredicate( (SqmPredicate) ctx.getChild( currentIndex++ ).accept( this ) );
		}
		sqmQuerySpec.setWhereClause( whereClause );

		if ( currentIndex < ctx.getChildCount() && ctx.getChild( currentIndex ) instanceof HqlParser.GroupByClauseContext ) {
			sqmQuerySpec.setGroupByClauseExpressions(
					visitGroupByClause( (HqlParser.GroupByClauseContext) ctx.getChild( currentIndex++ ) )
			);
		}
		if ( currentIndex < ctx.getChildCount() && ctx.getChild( currentIndex ) instanceof HqlParser.HavingClauseContext ) {
			sqmQuerySpec.setHavingClausePredicate(
					visitHavingClause( (HqlParser.HavingClauseContext) ctx.getChild( currentIndex ) )
			);
		}

		return sqmQuerySpec;
	}

	protected SqmSelectClause buildInferredSelectClause(SqmFromClause fromClause) {
		// for now, this is slightly different than the legacy behavior where
		// the root and each non-fetched-join was selected.  For now, here, we simply
		// select the root
		final SqmSelectClause selectClause;

		final boolean expectingArray = expectedResultType != null && expectedResultType.isArray();
		if ( expectingArray ) {
			// triggers legacy interpretation of returning all roots
			// and non-fetched joins
			selectClause = new SqmSelectClause(
					false,
					creationContext.getNodeBuilder()
			);
		}
		else {
			selectClause = new SqmSelectClause(
					false,
					fromClause.getNumberOfRoots(),
					creationContext.getNodeBuilder()
			);
		}

		fromClause.visitRoots( (sqmRoot) -> {
			selectClause.addSelection( new SqmSelection<>( sqmRoot, sqmRoot.getAlias(), creationContext.getNodeBuilder() ) );
			if ( expectingArray ) {
				applyJoinsToInferredSelectClause( sqmRoot, selectClause );
			}
		} );

		return selectClause;
	}

	private void applyJoinsToInferredSelectClause(SqmFrom<?,?> sqm, SqmSelectClause selectClause) {
		sqm.visitSqmJoins( (sqmJoin) -> {
			selectClause.addSelection( new SqmSelection<>( sqmJoin, sqmJoin.getAlias(), creationContext.getNodeBuilder() ) );
			applyJoinsToInferredSelectClause( sqmJoin, selectClause );
		} );
	}

	@Override
	public SqmSelectClause visitSelectClause(HqlParser.SelectClauseContext ctx) {
		// todo (6.0) : primer a select-clause-specific SemanticPathPart into the stack
		final int selectionListIndex;
		if ( ctx.getChild( 1 ) instanceof HqlParser.SelectionListContext ) {
			selectionListIndex = 1;
		}
		else {
			selectionListIndex = 2;
		}

		final SqmSelectClause selectClause = new SqmSelectClause(
				selectionListIndex == 2,
				creationContext.getNodeBuilder()
		);
		final HqlParser.SelectionListContext selectionListContext = (HqlParser.SelectionListContext) ctx.getChild(
				selectionListIndex
		);
		for ( ParseTree subCtx : selectionListContext.children ) {
			if ( subCtx instanceof HqlParser.SelectionContext ) {
				selectClause.addSelection( visitSelection( (HqlParser.SelectionContext) subCtx ) );
			}
		}
		return selectClause;
	}

	@Override
	public SqmSelection<?> visitSelection(HqlParser.SelectionContext ctx) {
		final String resultIdentifier;
		if ( ctx.getChildCount() == 1 ) {
			resultIdentifier = null;
		}
		else {
			resultIdentifier = applyJpaCompliance(
					visitVariable( (HqlParser.VariableContext) ctx.getChild( 1 ) )
			);
		}
		final SqmSelectableNode<?> selectableNode = visitSelectableNode( ctx );

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
		final ParseTree subCtx = ctx.getChild( 0 ).getChild( 0 );
		if ( subCtx instanceof HqlParser.ExpressionOrPredicateContext ) {
			final SqmExpression<?> sqmExpression = (SqmExpression<?>) subCtx.accept( this );
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

					final SqmPath<?> elementPath = sqmPath.resolvePathPart( CollectionPart.Nature.ELEMENT.getName(), true, this );
					processingStateStack.getCurrent().getPathRegistry().register( elementPath );
					return elementPath;
				}
			}

			return sqmExpression;
		}
		return (SqmSelectableNode<?>) subCtx.accept( this );
	}

	@Override
	public SqmDynamicInstantiation<?> visitInstantiation(HqlParser.InstantiationContext ctx) {
		final SqmDynamicInstantiation<?> dynamicInstantiation;
		final ParseTree instantiationTarget = ctx.instantiationTarget().getChild( 0 );
		if ( instantiationTarget instanceof HqlParser.SimplePathContext ) {
			final String className = instantiationTarget.getText();
			try {
				final JavaType<?> jtd = resolveInstantiationTargetJtd( className );
				dynamicInstantiation = SqmDynamicInstantiation.forClassInstantiation(
						jtd,
						creationContext.getNodeBuilder()
				);
			}
			catch (ClassLoadingException e) {
				throw new SemanticException( "Could not resolve class '" + className + "' named for instantiation" );
			}
		}
		else {
			final TerminalNode terminalNode = (TerminalNode) instantiationTarget;
			switch ( terminalNode.getSymbol().getType() ) {
				case HqlParser.MAP:
					dynamicInstantiation = SqmDynamicInstantiation.forMapInstantiation(
							mapJavaType,
							creationContext.getNodeBuilder()
					);
					break;
				case HqlParser.LIST:
					dynamicInstantiation = SqmDynamicInstantiation.forListInstantiation(
							listJavaType,
							creationContext.getNodeBuilder()
					);
					break;
				default:
					throw new UnsupportedOperationException( "Unsupported instantiation target: " + terminalNode );
			}
		}

		for ( HqlParser.InstantiationArgumentContext arg : ctx.instantiationArguments().instantiationArgument() ) {
			dynamicInstantiation.addArgument( visitInstantiationArgument( arg ) );
		}

		return dynamicInstantiation;
	}

	private JavaType<?> resolveInstantiationTargetJtd(String className) {
		final Class<?> targetJavaType = classForName( creationContext.getJpaMetamodel().qualifyImportableName( className ) );
		return creationContext.getJpaMetamodel()
				.getTypeConfiguration()
				.getJavaTypeRegistry()
				.resolveDescriptor( targetJavaType );
	}

	private Class<?> classForName(String className) {
		return creationContext.getServiceRegistry().getService( ClassLoaderService.class ).classForName( className );
	}

	@Override
	public SqmDynamicInstantiationArgument<?> visitInstantiationArgument(HqlParser.InstantiationArgumentContext ctx) {
		final String alias;
		if ( ctx.getChildCount() > 1 ) {
			alias = visitVariable( (HqlParser.VariableContext) ctx.getChild( ctx.getChildCount() - 1 ) );
		}
		else {
			alias = null;
		}

		final SqmSelectableNode<?> argExpression = (SqmSelectableNode<?>) ctx.getChild( 0 ).accept( this );

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
	public SqmPath<?> visitJpaSelectObjectSyntax(HqlParser.JpaSelectObjectSyntaxContext ctx) {
		final String alias = ctx.getChild( 2 ).getText();
		final SqmFrom<?, ?> sqmFromByAlias = processingStateStack.getCurrent().getPathRegistry().findFromByAlias(
				alias,
				true
		);
		if ( sqmFromByAlias == null ) {
			throw new SemanticException( "Unable to resolve alias [" +  alias + "] in selection [" + ctx.getText() + "]" );
		}
		return sqmFromByAlias;
	}

	@Override
	public List<SqmExpression<?>> visitGroupByClause(HqlParser.GroupByClauseContext ctx) {
		final int size = ctx.getChildCount();
		// Shift 1 bit instead of division by 2
		final int estimateExpressionsCount = ( size >> 1 ) - 1;
		final List<SqmExpression<?>> expressions = new ArrayList<>( estimateExpressionsCount );
		for ( int i = 0; i < size; i++ ) {
			final ParseTree parseTree = ctx.getChild( i );
			if ( parseTree instanceof HqlParser.GroupByExpressionContext ) {
				expressions.add( (SqmExpression<?>) parseTree.accept( this ) );
			}
		}
		return expressions;
	}

	private SqmExpression<?> resolveOrderByOrGroupByExpression(ParseTree child, boolean definedCollate) {
		final SqmCreationProcessingState processingState = getCurrentProcessingState();
		final SqmQuery<?> processingQuery = processingState.getProcessingQuery();
		final SqmQueryPart<?> queryPart;
		if ( processingQuery instanceof SqmInsertSelectStatement<?> ) {
			queryPart = ( (SqmInsertSelectStatement<?>) processingQuery ).getSelectQueryPart();
		}
		else if ( processingQuery instanceof SqmSelectQuery<?> ) {
			queryPart = ( (SqmSelectQuery<?>) processingQuery ).getQueryPart();
		}
		else {
			queryPart = null;
		}
		if ( child instanceof TerminalNode ) {
			if ( definedCollate ) {
				// This is syntactically disallowed
				throw new ParsingException( "COLLATE is not allowed for position based order-by or group-by items" );
			}

			final int position = Integer.parseInt( child.getText() );

			// make sure this selection exists
			final SqmAliasedNode<?> nodeByPosition;
			if ( queryPart instanceof SqmQueryGroup<?> ) {
				final List<SqmSelection<?>> selections = queryPart.getFirstQuerySpec().getSelectClause().getSelections();
				nodeByPosition = position <= selections.size() ? selections.get( position - 1 ) : null;
			}
			else {
				nodeByPosition = processingState.getPathRegistry().findAliasedNodeByPosition( position );
			}
			if ( nodeByPosition == null ) {
				throw new ParsingException( "Numeric literal '" + position + "' used in group-by does not match a registered select-item" );
			}

			return new SqmAliasedNodeRef( position, integerDomainType, creationContext.getNodeBuilder() );
		}
		else if ( child instanceof HqlParser.IdentifierContext ) {
			final String identifierText = visitIdentifier( (HqlParser.IdentifierContext) child );
			if ( queryPart instanceof SqmQueryGroup<?> ) {
				// If the current query part is a query group, check if the text matches
				// an attribute name of one of the selected SqmFrom elements or the path source name of a SqmPath
				SqmFrom<?, ?> found = null;
				int sqmPosition = 0;
				final List<SqmSelection<?>> selections = queryPart.getFirstQuerySpec().getSelectClause().getSelections();
				for ( int i = 0; i < selections.size(); i++ ) {
					final SqmSelection<?> sqmSelection = selections.get( i );
					if ( identifierText.equals( sqmSelection.getAlias() ) ) {
						return new SqmAliasedNodeRef(
								i + 1,
								creationContext.getNodeBuilder().getIntegerType(),
								creationContext.getNodeBuilder()
						);
					}
					final SqmSelectableNode<?> selectableNode = sqmSelection.getSelectableNode();
					if ( selectableNode instanceof SqmFrom<?, ?> ) {
						final SqmFrom<?, ?> fromElement = (SqmFrom<?, ?>) selectableNode;
						final SqmPathSource<?> pathSource = fromElement.getReferencedPathSource();
						if ( pathSource.findSubPathSource( identifierText ) != null ) {
							if ( sqmPosition != 0 ) {
								throw new IllegalStateException(
										"Multiple from-elements expose unqualified attribute : " + identifierText );
							}
							found = fromElement;
							sqmPosition = i + 1;
						}
					}
					else if ( selectableNode instanceof SqmPath<?> ) {
						final SqmPath<?> path = (SqmPath<?>) selectableNode;
						if ( identifierText.equals( path.getReferencedPathSource().getPathName() ) ) {
							if ( sqmPosition != 0 ) {
								throw new IllegalStateException(
										"Multiple from-elements expose unqualified attribute : " + identifierText );
							}
							sqmPosition = i + 1;
						}
					}
				}
				if ( found != null ) {
					return new SqmAliasedNodeRef(
							sqmPosition,
							found.get( identifierText ).getNavigablePath(),
							creationContext.getNodeBuilder().getIntegerType(),
							creationContext.getNodeBuilder()
					);
				}
				else if ( sqmPosition != 0 ) {
					return new SqmAliasedNodeRef(
							sqmPosition,
							creationContext.getNodeBuilder().getIntegerType(),
							creationContext.getNodeBuilder()
					);
				}
			}
			else {
				final Integer correspondingPosition = processingState.getPathRegistry()
						.findAliasedNodePosition( identifierText );
				if ( correspondingPosition != null ) {
					if ( definedCollate ) {
						// This is syntactically disallowed
						throw new ParsingException( "COLLATE is not allowed for alias based order-by or group-by items" );
					}
					return new SqmAliasedNodeRef(
							correspondingPosition,
							integerDomainType,
							creationContext.getNodeBuilder()
					);
				}

				final SqmFrom<?, ?> sqmFrom = processingState.getPathRegistry().findFromByAlias(
						identifierText,
						true
				);
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
		}

		return (SqmExpression<?>) child.accept( this );
	}

	@Override
	public SqmExpression<?> visitGroupByExpression(HqlParser.GroupByExpressionContext ctx) {
		return resolveOrderByOrGroupByExpression( ctx.getChild( 0 ), ctx.getChildCount() > 1 );
	}

	@Override
	public SqmPredicate visitHavingClause(HqlParser.HavingClauseContext ctx) {
		return (SqmPredicate) ctx.getChild( 1 ).accept( this );
	}

	@Override
	public SqmOrderByClause visitOrderByClause(HqlParser.OrderByClauseContext ctx) {
		final int size = ctx.getChildCount();
		// Shift 1 bit instead of division by 2
		final int estimateExpressionsCount = ( size >> 1 ) - 1;
		final SqmOrderByClause orderByClause = new SqmOrderByClause( estimateExpressionsCount );
		for ( int i = 0; i < size; i++ ) {
			final ParseTree parseTree = ctx.getChild( i );
			if ( parseTree instanceof HqlParser.SortSpecificationContext ) {
				orderByClause.addSortSpecification(
						visitSortSpecification( (HqlParser.SortSpecificationContext) parseTree )
				);
			}
		}
		return orderByClause;
	}

	@Override
	public SqmSortSpecification visitSortSpecification(HqlParser.SortSpecificationContext ctx) {
		final SqmExpression<?> sortExpression = visitSortExpression( (HqlParser.SortExpressionContext) ctx.getChild( 0 ) );
		if ( sortExpression == null ) {
			throw new ParsingException( "Could not resolve sort-expression : " + ctx.getChild( 0 ).getText() );
		}
		if ( sortExpression instanceof SqmLiteral || sortExpression instanceof SqmParameter ) {
			HqlLogging.QUERY_LOGGER.debugf( "Questionable sorting by constant value : %s", sortExpression );
		}

		final SortOrder sortOrder;
		final NullPrecedence nullPrecedence;
		int nextIndex = 1;
		if ( nextIndex < ctx.getChildCount() ) {
			ParseTree parseTree = ctx.getChild( nextIndex );
			if ( parseTree instanceof HqlParser.SortDirectionContext ) {
				switch ( ( (TerminalNode) parseTree.getChild( 0 ) ).getSymbol().getType() ) {
					case HqlParser.ASC:
						sortOrder = SortOrder.ASCENDING;
						break;
					case HqlParser.DESC:
						sortOrder = SortOrder.DESCENDING;
						break;
					default:
						throw new SemanticException( "Unrecognized sort ordering: " + parseTree.getText() );
				}
				nextIndex++;
			}
			else {
				sortOrder = null;
			}
			parseTree = ctx.getChild( nextIndex );
			if ( parseTree instanceof HqlParser.NullsPrecedenceContext ) {
				switch ( ( (TerminalNode) parseTree.getChild( 1 ) ).getSymbol().getType() ) {
					case HqlParser.FIRST:
						nullPrecedence = NullPrecedence.FIRST;
						break;
					case HqlParser.LAST:
						nullPrecedence = NullPrecedence.LAST;
						break;
					default:
						throw new SemanticException( "Unrecognized null precedence: " + parseTree.getText() );
				}
			}
			else {
				nullPrecedence = null;
			}
		}
		else {
			sortOrder = null;
			nullPrecedence = null;
		}

		return new SqmSortSpecification( sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public SqmExpression<?> visitSortExpression(HqlParser.SortExpressionContext ctx) {
		return resolveOrderByOrGroupByExpression( ctx.getChild( 0 ), ctx.getChildCount() > 1 );
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

	private <X> void setCurrentQueryPart(SqmQueryPart<X> queryPart) {
		@SuppressWarnings("unchecked")
		final SqmQuery<X> processingQuery = (SqmQuery<X>) processingStateStack.getCurrent().getProcessingQuery();
		if ( processingQuery instanceof SqmInsertSelectStatement<?> ) {
			( (SqmInsertSelectStatement<X>) processingQuery ).setSelectQueryPart( queryPart );
		}
		else {
			( (AbstractSqmSelectQuery<X>) processingQuery ).setQueryPart( queryPart );
		}
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
		final int thirdSymbolType = ( (TerminalNode) ctx.getChild( 3 ) ).getSymbol().getType();
		final int lastSymbolType = ( (TerminalNode) ctx.getChild( ctx.getChildCount() - 1 ) ).getSymbol().getType();
		if ( lastSymbolType == HqlParser.TIES ) {
			return thirdSymbolType == HqlParser.PERCENT ? FetchClauseType.PERCENT_WITH_TIES : FetchClauseType.ROWS_WITH_TIES;
		}
		else {
			return thirdSymbolType == HqlParser.PERCENT ? FetchClauseType.PERCENT_ONLY : FetchClauseType.ROWS_ONLY;
		}
	}

	@Override
	public Object visitSyntacticPathExpression(HqlParser.SyntacticPathExpressionContext ctx) {
		SemanticPathPart part = visitSyntacticDomainPath( (HqlParser.SyntacticDomainPathContext) ctx.getChild( 0 ) );
		if ( ctx.getChildCount() == 2 ) {
			dotIdentifierConsumerStack.push(
					new BasicDotIdentifierConsumer( part, this ) {
						@Override
						protected void reset() {
						}
					}
			);
			try {
				part = (SemanticPathPart) ctx.getChild( 1 ).accept( this );
			}
			finally {
				dotIdentifierConsumerStack.pop();
			}
		}
		if ( part instanceof DomainPathPart ) {
			return ( (DomainPathPart) part ).getSqmExpression();
		}
		return part;
	}

	@Override
	public Object visitGeneralPathExpression(HqlParser.GeneralPathExpressionContext ctx) {
		final SemanticPathPart part = visitGeneralPathFragment( (HqlParser.GeneralPathFragmentContext) ctx.getChild( 0 ) );
		if ( part instanceof DomainPathPart ) {
			return ( (DomainPathPart) part ).getSqmExpression();
		}
		return part;
	}

	@Override
	public SqmExpression<?> visitFunctionExpression(HqlParser.FunctionExpressionContext ctx) {
		return (SqmExpression<?>) ctx.getChild( 0 ).accept( this );
	}

	@Override
	public SqmExpression<?> visitParameterOrIntegerLiteral(HqlParser.ParameterOrIntegerLiteralContext ctx) {
		final ParseTree firstChild = ctx.getChild( 0 );
		if ( firstChild instanceof TerminalNode ) {
			return integerLiteral( firstChild.getText() );
		}
		return (SqmExpression<?>) firstChild.accept( this );
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
		final ParseTree firstChild = ctx.getChild( 0 );
		if ( firstChild instanceof TerminalNode ) {
			switch ( ( (TerminalNode) firstChild ).getSymbol().getType() ) {
				case HqlParser.INTEGER_LITERAL:
					return integerLiteral( firstChild.getText() );
				case HqlParser.FLOAT_LITERAL:
					return floatLiteral( firstChild.getText() );
				case HqlParser.DOUBLE_LITERAL:
					return doubleLiteral( firstChild.getText() );
				default:
					throw new UnsupportedOperationException( "Unsupported literal: " + firstChild.getText() );
			}
		}
		return (SqmExpression<?>) firstChild.accept( this );
	}

	public String getEntityName(HqlParser.EntityNameContext parserEntityName) {
		final StringBuilder sb = new StringBuilder();
		final int end = parserEntityName.getChildCount();
		sb.append( visitIdentifier( (HqlParser.IdentifierContext) parserEntityName.getChild( 0 ) ) );
		for ( int i = 2; i < end; i += 2 ) {
			sb.append( '.' );
			sb.append( visitIdentifier( (HqlParser.IdentifierContext) parserEntityName.getChild( i ) ) );
		}
		return sb.toString();
	}

	@Override
	public String visitIdentifier(HqlParser.IdentifierContext ctx) {
		final ParseTree child = ctx.getChild( 0 );
		if ( child instanceof TerminalNode ) {
			return child.getText();
		}
		return visitNakedIdentifier( (HqlParser.NakedIdentifierContext) child );
	}

	@Override
	public String visitNakedIdentifier(HqlParser.NakedIdentifierContext ctx) {
		final TerminalNode node = (TerminalNode) ctx.getChild( 0 );
		if ( node.getSymbol().getType() == HqlParser.QUOTED_IDENTIFIER ) {
			return QuotingHelper.unquoteIdentifier( node.getText() );
		}
		return node.getText();
	}

	@Override
	public EntityDomainType<?> visitEntityName(HqlParser.EntityNameContext parserEntityName) {
		final String entityName = getEntityName( parserEntityName );
		final EntityDomainType<?> entityReference = getCreationContext()
				.getJpaMetamodel()
				.getHqlEntityReference( entityName );
		if ( entityReference == null ) {
			throw new UnknownEntityException( "Could not resolve target entity '" + entityName + "'", entityName );
		}
		checkFQNEntityNameJpaComplianceViolationIfNeeded( entityName, entityReference );
		if ( entityReference instanceof SqmPolymorphicRootDescriptor<?> && getCreationOptions().useStrictJpaCompliance() ) {
			throw new StrictJpaComplianceViolation(
					"Encountered the use of a non entity name [" + entityName + "], " +
							"but strict JPQL compliance was requested which doesn't allow this",
					StrictJpaComplianceViolation.Type.NON_ENTITY_NAME
			);
		}
		return entityReference;
	}

	@Override
	public SqmFromClause visitFromClause(HqlParser.FromClauseContext parserFromClause) {
		final SqmFromClause fromClause;
		if ( parserFromClause == null ) {
			fromClause = new SqmFromClause();
			currentQuerySpec().setFromClause( fromClause );
		}
		else {
			final int size = parserFromClause.getChildCount();
			// Shift 1 bit instead of division by 2
			final int estimatedSize = size >> 1;
			fromClause = new SqmFromClause( estimatedSize );
			currentQuerySpec().setFromClause( fromClause );
			for ( int i = 0; i < size; i++ ) {
				final ParseTree parseTree = parserFromClause.getChild( i );
				if ( parseTree instanceof HqlParser.EntityWithJoinsContext ) {
					visitEntityWithJoins( (HqlParser.EntityWithJoinsContext) parseTree );
				}
			}
		}
		return fromClause;
	}

	@Override
	public SqmRoot<?> visitEntityWithJoins(HqlParser.EntityWithJoinsContext parserSpace) {
		final SqmRoot<?> sqmRoot = (SqmRoot<?>) parserSpace.getChild( 0 ).accept( this );
		final SqmFromClause fromClause = currentQuerySpec().getFromClause();
		// Correlations are implicitly added to the from clause
		if ( !( sqmRoot instanceof SqmCorrelation<?, ?> ) ) {
			fromClause.addRoot( sqmRoot );
		}
		final int size = parserSpace.getChildCount();
		for ( int i = 1; i < size; i++ ) {
			final ParseTree parseTree = parserSpace.getChild( i );
			if ( parseTree instanceof HqlParser.CrossJoinContext ) {
				consumeCrossJoin( (HqlParser.CrossJoinContext) parseTree, sqmRoot );
			}
			else if ( parseTree instanceof HqlParser.JoinContext ) {
				consumeJoin( (HqlParser.JoinContext) parseTree, sqmRoot );
			}
			else if ( parseTree instanceof HqlParser.JpaCollectionJoinContext ) {
				consumeJpaCollectionJoin( (HqlParser.JpaCollectionJoinContext) parseTree, sqmRoot );
			}
		}

		return sqmRoot;
	}

	@Override
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public SqmRoot<?> visitRootEntity(HqlParser.RootEntityContext ctx) {
		final HqlParser.EntityNameContext entityNameContext = (HqlParser.EntityNameContext) ctx.getChild( 0 );
		final List<ParseTree> entityNameParseTreeChildren = entityNameContext.children;
		final String name = getEntityName( entityNameContext );

		log.debugf( "Handling root path - %s", name );
		final EntityDomainType entityDescriptor = getCreationContext()
				.getJpaMetamodel()
				.getHqlEntityReference( name );

		final HqlParser.VariableContext identificationVariableDefContext;
		if ( ctx.getChildCount() > 1 ) {
			identificationVariableDefContext = (HqlParser.VariableContext) ctx.getChild( 1 );
		}
		else {
			identificationVariableDefContext = null;
		}
		final String alias = applyJpaCompliance(
				visitVariable( identificationVariableDefContext )
		);

		final SqmCreationProcessingState processingState = processingStateStack.getCurrent();
		final SqmPathRegistry pathRegistry = processingState.getPathRegistry();
		if ( entityDescriptor == null ) {
			final int size = entityNameParseTreeChildren.size();
			// Handle the use of a correlation path in subqueries
			if ( processingStateStack.depth() > 1 && size > 2 ) {
				final String parentAlias = entityNameParseTreeChildren.get( 0 ).getText();
				final AbstractSqmFrom<?, ?> correlation = processingState.getPathRegistry()
						.findFromByAlias( parentAlias, true );
				if ( correlation instanceof SqmCorrelation<?, ?> ) {
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
					return ( (SqmCorrelation<?, ?>) correlation ).getCorrelatedRoot();
				}
				throw new SemanticException( "Could not resolve entity or correlation path '" + name + "'" );
			}
			throw new UnknownEntityException( "Could not resolve root entity '" + name + "'", name );
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
						"Illegal implicit-polymorphic domain path in subquery '" + entityDescriptor.getName() +"'"
				);
			}
		}

		final SqmRoot<?> sqmRoot = new SqmRoot<>( entityDescriptor, alias, true, creationContext.getNodeBuilder() );

		pathRegistry.register( sqmRoot );

		return sqmRoot;
	}

	@Override
	public SqmRoot<?> visitRootSubquery(HqlParser.RootSubqueryContext ctx) {
		if ( getCreationOptions().useStrictJpaCompliance() ) {
			throw new StrictJpaComplianceViolation(
					"The JPA specification does not support subqueries in the from clause. " +
							"Please disable the JPA query compliance if you want to use this feature.",
					StrictJpaComplianceViolation.Type.FROM_SUBQUERY
			);
		}
		final ParseTree firstChild = ctx.getChild( 0 );
		final boolean lateral = ( (TerminalNode) firstChild ).getSymbol().getType() == HqlParser.LATERAL;
		final int subqueryIndex = lateral ? 2 : 1;
		final SqmSubQuery<?> subQuery = (SqmSubQuery<?>) ctx.getChild( subqueryIndex ).accept( this );

		final ParseTree lastChild = ctx.getChild( ctx.getChildCount() - 1 );
		final HqlParser.VariableContext identificationVariableDefContext;
		if ( lastChild instanceof HqlParser.VariableContext ) {
			identificationVariableDefContext = (HqlParser.VariableContext) lastChild;
		}
		else {
			identificationVariableDefContext = null;
		}
		final String alias = applyJpaCompliance(
				visitVariable( identificationVariableDefContext )
		);

		final SqmCreationProcessingState processingState = processingStateStack.getCurrent();
		final SqmPathRegistry pathRegistry = processingState.getPathRegistry();
		final SqmRoot<?> sqmRoot = new SqmDerivedRoot<>( subQuery, alias, lateral );

		pathRegistry.register( sqmRoot );

		return sqmRoot;
	}

	@Override
	public String visitVariable(HqlParser.VariableContext ctx) {
		if ( ctx == null ) {
			return null;
		}
		final ParseTree lastChild = ctx.getChild( ctx.getChildCount() - 1 );
		if ( lastChild instanceof HqlParser.IdentifierContext ) {
			final HqlParser.IdentifierContext identifierContext = (HqlParser.IdentifierContext) lastChild;
			// in this branch, the alias could be a reserved word ("keyword as identifier")
			// which JPA disallows...
			if ( getCreationOptions().useStrictJpaCompliance() ) {
				final Token identificationVariableToken = identifierContext.getStart();
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
			return visitIdentifier( identifierContext );
		}
		else {
			final HqlParser.NakedIdentifierContext identifierContext = (HqlParser.NakedIdentifierContext) lastChild;
			// in this branch, the alias could be a reserved word ("keyword as identifier")
			// which JPA disallows...
			if ( getCreationOptions().useStrictJpaCompliance() ) {
				final Token identificationVariableToken = identifierContext.getStart();
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
			return visitNakedIdentifier( identifierContext );
		}
	}

	private String applyJpaCompliance(String text) {
		if ( text == null ) {
			return null;
		}

		if ( getCreationOptions().useStrictJpaCompliance() ) {
			return text.toLowerCase( Locale.getDefault() );
		}

		return text;
	}

	@Override
	public final SqmCrossJoin<?> visitCrossJoin(HqlParser.CrossJoinContext ctx) {
		throw new UnsupportedOperationException( "Unexpected call to #visitCrossJoin, see #consumeCrossJoin" );
	}

	private <T> void consumeCrossJoin(HqlParser.CrossJoinContext parserJoin, SqmRoot<T> sqmRoot) {
		final HqlParser.EntityNameContext entityNameContext = (HqlParser.EntityNameContext) parserJoin.getChild( 2 );
		final String name = getEntityName( entityNameContext );

		SqmTreeCreationLogger.LOGGER.debugf( "Handling root path - %s", name );

		final EntityDomainType<T> entityDescriptor = getCreationContext().getJpaMetamodel()
				.resolveHqlEntityReference( name );

		if ( entityDescriptor instanceof SqmPolymorphicRootDescriptor ) {
			throw new SemanticException( "Unmapped polymorphic reference cannot be used as a CROSS JOIN target" );
		}
		final HqlParser.VariableContext identificationVariableDefContext;
		if ( parserJoin.getChildCount() > 3 ) {
			identificationVariableDefContext = (HqlParser.VariableContext) parserJoin.getChild( 3 );
		}
		else {
			identificationVariableDefContext = null;
		}
		final SqmCrossJoin<T> join = new SqmCrossJoin<>(
				entityDescriptor,
				visitVariable( identificationVariableDefContext ),
				sqmRoot
		);

		processingStateStack.getCurrent().getPathRegistry().register( join );

		// CROSS joins are always added to the root
		sqmRoot.addSqmJoin( join );
	}

	@Override
	public final SqmJoin<?, ?> visitJoin(HqlParser.JoinContext parserJoin) {
		throw new UnsupportedOperationException( "Unexpected call to #visitJoin, see #consumeJoin" );
	}

	protected <X> void consumeJoin(HqlParser.JoinContext parserJoin, SqmRoot<X> sqmRoot) {
		final SqmJoinType joinType;
		final int firstJoinTypeSymbolType;
		if ( parserJoin.getChild( 0 ) instanceof HqlParser.JoinTypeContext
				&& parserJoin.getChild( 0 ).getChildCount() != 0 ) {
			firstJoinTypeSymbolType = ( (TerminalNode) parserJoin.getChild( 0 ).getChild( 0 ) ).getSymbol().getType();
		}
		else {
			firstJoinTypeSymbolType = HqlParser.INNER;
		}
		switch ( firstJoinTypeSymbolType ) {
			case HqlParser.FULL:
				joinType = SqmJoinType.FULL;
				break;
			case HqlParser.RIGHT:
				joinType = SqmJoinType.RIGHT;
				break;
			// For some reason, we also support `outer join` syntax..
			case HqlParser.OUTER:
			case HqlParser.LEFT:
				joinType = SqmJoinType.LEFT;
				break;
			default:
				joinType = SqmJoinType.INNER;
				break;
		}

		final HqlParser.JoinTargetContext qualifiedJoinTargetContext = parserJoin.joinTarget();
		final ParseTree lastChild = qualifiedJoinTargetContext.getChild( qualifiedJoinTargetContext.getChildCount() - 1 );
		final HqlParser.VariableContext identificationVariableDefContext;
		if ( lastChild instanceof HqlParser.VariableContext ) {
			identificationVariableDefContext = (HqlParser.VariableContext) lastChild;
		}
		else {
			identificationVariableDefContext = null;
		}
		final String alias = visitVariable( identificationVariableDefContext );
		final boolean fetch = parserJoin.getChild( 2 ) instanceof TerminalNode;

		if ( fetch && processingStateStack.depth() > 1 ) {
			throw new SemanticException( "fetch not allowed in subquery from-elements" );
		}

		dotIdentifierConsumerStack.push(
				new QualifiedJoinPathConsumer(
						sqmRoot,
						joinType,
						fetch,
						alias,
						this
				)
		);
		try {
			final SqmQualifiedJoin<X, ?> join;
			if ( qualifiedJoinTargetContext instanceof HqlParser.JoinPathContext ) {
				//noinspection unchecked
				join = (SqmQualifiedJoin<X, ?>) qualifiedJoinTargetContext.getChild( 0 ).accept( this );
			}
			else {
				if ( fetch ) {
					throw new SemanticException( "fetch not allowed for subquery join" );
				}
				if ( getCreationOptions().useStrictJpaCompliance() ) {
					throw new StrictJpaComplianceViolation(
							"The JPA specification does not support subqueries in the from clause. " +
									"Please disable the JPA query compliance if you want to use this feature.",
							StrictJpaComplianceViolation.Type.FROM_SUBQUERY
					);
				}
				final TerminalNode terminalNode = (TerminalNode) qualifiedJoinTargetContext.getChild( 0 );
				final boolean lateral = terminalNode.getSymbol().getType() == HqlParser.LATERAL;
				final int subqueryIndex = lateral ? 2 : 1;
				final DotIdentifierConsumer identifierConsumer = dotIdentifierConsumerStack.pop();
				final SqmSubQuery<?> subQuery = (SqmSubQuery<?>) qualifiedJoinTargetContext.getChild( subqueryIndex ).accept( this );
				dotIdentifierConsumerStack.push( identifierConsumer );
				//noinspection unchecked,rawtypes
				join = new SqmDerivedJoin( subQuery, alias, joinType, lateral, sqmRoot );
				processingStateStack.getCurrent().getPathRegistry().register( join );
			}

			final HqlParser.JoinRestrictionContext qualifiedJoinRestrictionContext = parserJoin.joinRestriction();
			if ( join instanceof SqmEntityJoin<?> || join instanceof SqmDerivedJoin<?> ) {
				sqmRoot.addSqmJoin( join );
			}
			else if ( join instanceof SqmAttributeJoin<?, ?> ) {
				final SqmAttributeJoin<?, ?> attributeJoin = (SqmAttributeJoin<?, ?>) join;
				if ( getCreationOptions().useStrictJpaCompliance() ) {
					if ( join.getExplicitAlias() != null ) {
						if ( attributeJoin.isFetched() ) {
							throw new StrictJpaComplianceViolation(
									"Encountered aliased fetch join, but strict JPQL compliance was requested",
									StrictJpaComplianceViolation.Type.ALIASED_FETCH_JOIN
							);
						}
					}
				}
				if ( qualifiedJoinRestrictionContext != null && attributeJoin.isFetched() ) {
					throw new SemanticException( "with-clause not allowed on fetched associations; use filters" );
				}
			}

			if ( qualifiedJoinRestrictionContext != null ) {
				dotIdentifierConsumerStack.push( new QualifiedJoinPredicatePathConsumer( join, this ) );
				try {
					join.setJoinPredicate( (SqmPredicate) qualifiedJoinRestrictionContext.getChild( 1 ).accept( this ) );
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
	public SqmJoin<?, ?> visitJpaCollectionJoin(HqlParser.JpaCollectionJoinContext ctx) {
		throw new UnsupportedOperationException();
	}

	protected void consumeJpaCollectionJoin(
			HqlParser.JpaCollectionJoinContext ctx,
			SqmRoot<?> sqmRoot) {
		final HqlParser.VariableContext identificationVariableDefContext;
		if ( ctx.getChildCount() > 5 ) {
			identificationVariableDefContext = (HqlParser.VariableContext) ctx.getChild( 5 );
		}
		else {
			identificationVariableDefContext = null;
		}
		final String alias = visitVariable( identificationVariableDefContext );
		dotIdentifierConsumerStack.push(
				new QualifiedJoinPathConsumer(
						sqmRoot,
						// According to JPA spec 4.4.6 this is an inner join
						SqmJoinType.INNER,
						false,
						alias,
						this
				)
		);

		try {
			consumePluralAttributeReference( (HqlParser.PathContext) ctx.getChild( 3 ) );
		}
		finally {
			dotIdentifierConsumerStack.pop();
		}
	}


	// Predicates (and `whereClause`)

	@Override
	public SqmPredicate visitWhereClause(HqlParser.WhereClauseContext ctx) {
		if ( ctx == null || ctx.getChildCount() != 2 ) {
			return null;
		}

		return (SqmPredicate) ctx.getChild( 1 ).accept( this );

	}

	@Override
	public SqmGroupedPredicate visitGroupedPredicate(HqlParser.GroupedPredicateContext ctx) {
		return new SqmGroupedPredicate(
				(SqmPredicate) ctx.getChild( 1 ).accept( this ),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmPredicate visitAndPredicate(HqlParser.AndPredicateContext ctx) {
		return junction(
				Predicate.BooleanOperator.AND,
				(SqmPredicate) ctx.getChild( 0 ).accept( this ),
				(SqmPredicate) ctx.getChild( 2 ).accept( this )
		);
	}

	@Override
	public SqmPredicate visitOrPredicate(HqlParser.OrPredicateContext ctx) {
		return junction(
				Predicate.BooleanOperator.OR,
				(SqmPredicate) ctx.getChild( 0 ).accept( this ),
				(SqmPredicate) ctx.getChild( 2 ).accept( this )
		);
	}

	private SqmPredicate junction(Predicate.BooleanOperator operator, SqmPredicate lhs, SqmPredicate rhs) {
		if ( lhs instanceof SqmJunctionPredicate ) {
			final SqmJunctionPredicate junction = (SqmJunctionPredicate) lhs;
			if ( junction.getOperator() == operator ) {
				junction.getPredicates().add( rhs );
				return junction;
			}
		}
		if ( rhs instanceof SqmJunctionPredicate ) {
			final SqmJunctionPredicate junction = (SqmJunctionPredicate) rhs;
			if ( junction.getOperator() == operator ) {
				junction.getPredicates().add( 0, lhs );
				return junction;
			}
		}
		return new SqmJunctionPredicate(
				operator,
				lhs,
				rhs,
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmPredicate visitNegatedPredicate(HqlParser.NegatedPredicateContext ctx) {
		SqmPredicate predicate = (SqmPredicate) ctx.getChild( 1 ).accept( this );
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
		final boolean negated = ( (TerminalNode) ctx.getChild( 1 ) ).getSymbol().getType() == HqlParser.NOT;
		final int startIndex = negated ? 3 : 2;
		return new SqmBetweenPredicate(
				(SqmExpression<?>) ctx.getChild( 0 ).accept( this ),
				(SqmExpression<?>) ctx.getChild( startIndex ).accept( this ),
				(SqmExpression<?>) ctx.getChild( startIndex + 2 ).accept( this ),
				negated,
				creationContext.getNodeBuilder()
		);
	}


	@Override
	public SqmNullnessPredicate visitIsNullPredicate(HqlParser.IsNullPredicateContext ctx) {
		final boolean negated = ctx.getChildCount() == 4;
		return new SqmNullnessPredicate(
				(SqmExpression<?>) ctx.getChild( 0 ).accept( this ),
				negated,
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmEmptinessPredicate visitIsEmptyPredicate(HqlParser.IsEmptyPredicateContext ctx) {
		final boolean negated = ctx.getChildCount() == 4;
		return new SqmEmptinessPredicate(
				(SqmPluralValuedSimplePath<?>) ctx.getChild( 0 ).accept( this ),
				negated,
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
		final ComparisonOperator comparisonOperator = (ComparisonOperator) ctx.getChild( 1 ).accept( this );
		final SqmExpression<?> left;
		final SqmExpression<?> right;
		final HqlParser.ExpressionContext leftExpressionContext = (HqlParser.ExpressionContext) ctx.getChild( 0 );
		final HqlParser.ExpressionContext rightExpressionContext = (HqlParser.ExpressionContext) ctx.getChild( 2 );
		switch (comparisonOperator) {
			case EQUAL:
			case NOT_EQUAL:
			case DISTINCT_FROM:
			case NOT_DISTINCT_FROM: {
				Map<Class<?>, Enum<?>> possibleEnumValues;
				if ( ( possibleEnumValues = getPossibleEnumValues( leftExpressionContext ) ) != null ) {
					right = (SqmExpression<?>) rightExpressionContext.accept( this );
					left = resolveEnumShorthandLiteral(
							leftExpressionContext,
							possibleEnumValues,
							right.getJavaType()
					);
					break;
				}
				else if ( ( possibleEnumValues = getPossibleEnumValues( rightExpressionContext ) ) != null ) {
					left = (SqmExpression<?>) leftExpressionContext.accept( this );
					right = resolveEnumShorthandLiteral(
							rightExpressionContext,
							possibleEnumValues,
							left.getJavaType()
					);
					break;
				}
				final SqmExpression<?> l = (SqmExpression<?>) leftExpressionContext.accept( this );
				final SqmExpression<?> r = (SqmExpression<?>) rightExpressionContext.accept( this );
				if ( l instanceof AnyDiscriminatorSqmPath && r instanceof SqmLiteralEntityType ) {
					left = l;
					right = createDiscriminatorValue( (AnyDiscriminatorSqmPath) left, rightExpressionContext );
				}
				else if ( r instanceof AnyDiscriminatorSqmPath && l instanceof SqmLiteralEntityType ) {
					left = createDiscriminatorValue( (AnyDiscriminatorSqmPath) r, leftExpressionContext );
					right = r;
				}
				else {
					left = l;
					right = r;
				}

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
				left = (SqmExpression<?>) leftExpressionContext.accept( this );
				right = (SqmExpression<?>) rightExpressionContext.accept( this );
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

	private <T> SqmExpression<T> createDiscriminatorValue(
			AnyDiscriminatorSqmPath anyDiscriminatorTypeSqmPath,
			HqlParser.ExpressionContext valueExpressionContext) {
		return new SqmAnyDiscriminatorValue<>(
				anyDiscriminatorTypeSqmPath.getNodeType().getPathName(),
				creationContext.getJpaMetamodel().resolveHqlEntityReference( valueExpressionContext.getText() ),
				anyDiscriminatorTypeSqmPath.getExpressible().getSqmPathType(),
				creationContext.getNodeBuilder()
		);
	}

	private SqmExpression<?> resolveEnumShorthandLiteral(HqlParser.ExpressionContext expressionContext, Map<Class<?>, Enum<?>> possibleEnumValues, Class<?> enumType) {
		final Enum<?> enumValue;
		if ( possibleEnumValues != null && ( enumValue = possibleEnumValues.get( enumType ) ) != null ) {
			DotIdentifierConsumer dotIdentifierConsumer = dotIdentifierConsumerStack.getCurrent();
			dotIdentifierConsumer.consumeIdentifier( enumValue.getClass().getCanonicalName(), true, false );
			dotIdentifierConsumer.consumeIdentifier( enumValue.name(), false, true );
			return (SqmExpression<?>) dotIdentifierConsumerStack.getCurrent().getConsumedPart();
		}
		else {
			return (SqmExpression<?>) expressionContext.accept( this );
		}
	}

	private Map<Class<?>, Enum<?>> getPossibleEnumValues(HqlParser.ExpressionContext expressionContext) {
		ParseTree ctx;
		// Traverse the expression structure according to the grammar
		if ( expressionContext instanceof HqlParser.BarePrimaryExpressionContext && expressionContext.getChildCount() == 1 ) {
			ctx = expressionContext.getChild( 0 );

			while ( ctx instanceof HqlParser.PrimaryExpressionContext && ctx.getChildCount() == 1 ) {
				ctx = ctx.getChild( 0 );
			}

			if ( ctx instanceof HqlParser.GeneralPathFragmentContext && ctx.getChildCount() == 1 ) {
				ctx = ctx.getChild( 0 );

				if ( ctx instanceof HqlParser.SimplePathContext ) {
					return creationContext.getJpaMetamodel().getAllowedEnumLiteralTexts().get( ctx.getText() );
				}
			}
		}

		return null;
	}

	@Override
	public SqmPredicate visitLikePredicate(HqlParser.LikePredicateContext ctx) {
		final boolean negated = ( (TerminalNode) ctx.getChild( 1 ) ).getSymbol().getType() == HqlParser.NOT;
		final int startIndex = negated ? 3 : 2;
		final boolean caseSensitive = ( (TerminalNode) ctx.getChild( negated ? 2 : 1 ) ).getSymbol()
				.getType() == HqlParser.LIKE;
		if ( ctx.getChildCount() == startIndex + 2 ) {
			return new SqmLikePredicate(
					(SqmExpression<?>) ctx.getChild( 0 ).accept( this ),
					(SqmExpression<?>) ctx.getChild( startIndex ).accept( this ),
					(SqmExpression<?>) ctx.getChild( startIndex + 1 ).accept( this ),
					negated,
					caseSensitive,
					creationContext.getNodeBuilder()
			);
		}
		else {
			return new SqmLikePredicate(
					(SqmExpression<?>) ctx.getChild( 0 ).accept( this ),
					(SqmExpression<?>) ctx.getChild( startIndex ).accept( this ),
					negated,
					caseSensitive,
					creationContext.getNodeBuilder()
			);
		}
	}

	@Override
	public Object visitLikeEscape(HqlParser.LikeEscapeContext ctx) {
		final ParseTree child = ctx.getChild( 1 );
		if ( child instanceof HqlParser.NamedParameterContext ) {
			return visitNamedParameter(
					(HqlParser.NamedParameterContext) child,
					creationContext.getNodeBuilder().getCharacterType()
			);
		}
		else if ( child instanceof HqlParser.PositionalParameterContext ) {
			return visitPositionalParameter(
					(HqlParser.PositionalParameterContext) child,
					creationContext.getNodeBuilder().getCharacterType()
			);
		}
		else {
			assert child instanceof TerminalNode;
			final TerminalNode terminalNode = (TerminalNode) child;
			final String escape = QuotingHelper.unquoteStringLiteral( terminalNode.getText() );
			if ( escape.length() != 1 ) {
				throw new SemanticException(
						"Escape character literals must have exactly a single character, but found: " + escape
				);
			}
			return new SqmLiteral<>(
					escape.charAt( 0 ),
					creationContext.getNodeBuilder().getCharacterType(),
					creationContext.getNodeBuilder()
			);
		}
	}

	@Override
	public SqmPredicate visitMemberOfPredicate(HqlParser.MemberOfPredicateContext ctx) {
		final boolean negated = ctx.NOT() != null;
		final SqmPath<?> sqmPluralPath = consumeDomainPath(
				(HqlParser.PathContext) ctx.getChild( ctx.getChildCount() - 1 )
		);

		if ( sqmPluralPath.getReferencedPathSource() instanceof PluralPersistentAttribute ) {
			return new SqmMemberOfPredicate(
					(SqmExpression<?>) ctx.getChild( 0 ).accept( this ),
					sqmPluralPath,
					negated,
					creationContext.getNodeBuilder()
			);
		}
		else {
			throw new SemanticException( "Path argument to MEMBER OF must be a plural attribute" );
		}
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public SqmPredicate visitInPredicate(HqlParser.InPredicateContext ctx) {
		final boolean negated = ctx.getChildCount() == 4;
		final SqmExpression<?> testExpression = (SqmExpression<?>) ctx.getChild( 0 ).accept( this );
		final HqlParser.InListContext inListContext = (HqlParser.InListContext) ctx.getChild( ctx.getChildCount() - 1 );
		if ( inListContext instanceof HqlParser.ExplicitTupleInListContext ) {
			final HqlParser.ExplicitTupleInListContext tupleExpressionListContext = (HqlParser.ExplicitTupleInListContext) inListContext;
			final int size = tupleExpressionListContext.getChildCount();
			final int estimatedSize = size >> 1;
			final Class<?> testExpressionJavaType = testExpression.getJavaType();
			final boolean isEnum = testExpressionJavaType != null && testExpressionJavaType.isEnum();
			// Multi-valued bindings are only allowed if there is a single list item, hence size 3 (LP, RP and param)
			parameterDeclarationContextStack.push( () -> size == 3 );
			try {
				final List<SqmExpression<?>> listExpressions = new ArrayList<>( estimatedSize );
				for ( int i = 1; i < size; i++ ) {
					final ParseTree parseTree = tupleExpressionListContext.getChild( i );
					if ( parseTree instanceof HqlParser.ExpressionOrPredicateContext ) {
						final ParseTree child = parseTree.getChild( 0 );
						final HqlParser.ExpressionContext expressionContext;
						final Map<Class<?>, Enum<?>> possibleEnumValues;
						if ( isEnum && child instanceof HqlParser.ExpressionContext
								&& ( possibleEnumValues = getPossibleEnumValues( expressionContext = (HqlParser.ExpressionContext) child ) ) != null ) {
							listExpressions.add(
									resolveEnumShorthandLiteral(
											expressionContext,
											possibleEnumValues,
											testExpressionJavaType
									)
							);
						}
						else {
							listExpressions.add( (SqmExpression<?>) child.accept( this ) );
						}
					}
				}

				return new SqmInListPredicate(
						testExpression,
						listExpressions,
						negated,
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
				return new SqmInListPredicate(
						testExpression,
						Collections.singletonList( tupleExpressionListContext.getChild( 0 ).accept( this ) ),
						negated,
						creationContext.getNodeBuilder()
				);
			}
			finally {
				parameterDeclarationContextStack.pop();
			}
		}
		else if ( inListContext instanceof HqlParser.SubqueryInListContext ) {
			final HqlParser.SubqueryInListContext subQueryOrParamInListContext = (HqlParser.SubqueryInListContext) inListContext;
			return new SqmInSubQueryPredicate(
					testExpression,
					visitSubquery( (HqlParser.SubqueryContext) subQueryOrParamInListContext.getChild( 1 ) ),
					negated,
					creationContext.getNodeBuilder()
			);
		}
		else if ( inListContext instanceof HqlParser.PersistentCollectionReferenceInListContext ) {
			if ( getCreationOptions().useStrictJpaCompliance() ) {
				throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.HQL_COLLECTION_FUNCTION );
			}
			final HqlParser.PersistentCollectionReferenceInListContext collectionReferenceInListContext = (HqlParser.PersistentCollectionReferenceInListContext) inListContext;
			return new SqmInSubQueryPredicate<>(
					testExpression,
					createCollectionReferenceSubQuery(
							(HqlParser.SimplePathContext) collectionReferenceInListContext.getChild( 2 ),
							(TerminalNode) collectionReferenceInListContext.getChild( 0 )
					),
					negated,
					creationContext.getNodeBuilder()
			);
		}
		else {
			throw new ParsingException( "Unexpected IN predicate type [" + ctx.getClass().getSimpleName() + "] : " + ctx.getText() );
		}
	}

	@Override
	public SqmPredicate visitExistsCollectionPartPredicate(HqlParser.ExistsCollectionPartPredicateContext ctx) {
		final SqmSubQuery<Object> subQuery = createCollectionReferenceSubQuery(
				(HqlParser.SimplePathContext) ctx.getChild( 3 ),
				null
		);
		return new SqmExistsPredicate( subQuery, creationContext.getNodeBuilder() );
	}

	@Override
	public SqmPredicate visitExistsPredicate(HqlParser.ExistsPredicateContext ctx) {
		final SqmExpression<?> expression = (SqmExpression<?>) ctx.getChild( 1 ).accept( this );
		return new SqmExistsPredicate( expression, creationContext.getNodeBuilder() );
	}

	@Override @SuppressWarnings("rawtypes")
	public SqmPredicate visitBooleanExpressionPredicate(HqlParser.BooleanExpressionPredicateContext ctx) {
		final SqmExpression expression = (SqmExpression) ctx.expression().accept( this );
		if ( expression.getJavaType() != Boolean.class ) {
			throw new SemanticException( "Non-boolean expression used in predicate context: " + ctx.getText() );
		}
		return new SqmBooleanExpressionPredicate( expression, creationContext.getNodeBuilder() );
	}

	@Override
	public Object visitEntityTypeExpression(HqlParser.EntityTypeExpressionContext ctx) {
		final ParseTree pathOrParameter = ctx.getChild( 0 ).getChild( 2 );
		// can be one of 2 forms:
		//		1) TYPE( some.path )
		//		2) TYPE( :someParam )
		if ( pathOrParameter instanceof HqlParser.ParameterContext ) {
			// we have form (2)
			return new SqmParameterizedEntityType<>(
					(SqmParameter<?>) pathOrParameter.accept( this ),
					creationContext.getNodeBuilder()
			);
		}
		else if ( pathOrParameter instanceof HqlParser.PathContext ) {
			// we have form (1)
			return ( (SqmPath<?>) pathOrParameter.accept( this ) ).type();
		}

		throw new ParsingException( "Could not interpret grammar context as 'entity type' expression : " + ctx.getText() );
	}

	@Override
	public SqmExpression<?> visitEntityIdExpression(HqlParser.EntityIdExpressionContext ctx) {
		return visitEntityIdReference( (HqlParser.EntityIdReferenceContext) ctx.getChild( 0 ) );
	}

	@Override
	public SqmPath<?> visitEntityIdReference(HqlParser.EntityIdReferenceContext ctx) {
		final SqmPath<Object> sqmPath = consumeDomainPath( (HqlParser.PathContext) ctx.getChild( 2 ) );
		final DomainType<?> sqmPathType = sqmPath.getReferencedPathSource().getSqmPathType();

		if ( sqmPathType instanceof IdentifiableDomainType<?> ) {
			final SqmPathSource<?> identifierDescriptor = ( (IdentifiableDomainType<?>) sqmPathType ).getIdentifierDescriptor();
			final SqmPath<?> idPath = sqmPath.get( identifierDescriptor.getPathName() );

			if ( ctx.getChildCount() != 5 ) {
				return idPath;
			}
			final HqlParser.PathContinuationContext pathContinuationContext = (HqlParser.PathContinuationContext) ctx.getChild( 4 );

			throw new NotYetImplementedFor6Exception( "Path continuation from `id()` reference not yet implemented" );
		}

		throw new SemanticException( "Path does not resolve to an entity type '" + sqmPath.getNavigablePath() + "'" );
	}

	@Override
	public SqmExpression<?> visitEntityVersionExpression(HqlParser.EntityVersionExpressionContext ctx) {
		return visitEntityVersionReference( (HqlParser.EntityVersionReferenceContext) ctx.getChild( 0 ) );
	}

	@Override
	public SqmPath<?> visitEntityVersionReference(HqlParser.EntityVersionReferenceContext ctx) {
		final SqmPath<Object> sqmPath = consumeDomainPath( (HqlParser.PathContext) ctx.getChild( 2 ) );
		final DomainType<?> sqmPathType = sqmPath.getReferencedPathSource().getSqmPathType();

		if ( sqmPathType instanceof IdentifiableDomainType<?> ) {
			@SuppressWarnings("unchecked")
			final IdentifiableDomainType<Object> identifiableType = (IdentifiableDomainType<Object>) sqmPathType;
			final SingularPersistentAttribute<Object, ?> versionAttribute = identifiableType.findVersionAttribute();
			if ( versionAttribute == null ) {
				throw new SemanticException(
						String.format(
								"Path '%s' resolved to entity type '%s' which does not define a version",
								sqmPath.getNavigablePath(),
								identifiableType.getTypeName()
						)
				);
			}

			return sqmPath.get( versionAttribute );
		}

		throw new SemanticException( "Path does not resolve to an entity type '" + sqmPath.getNavigablePath() + "'" );
	}

	@Override
	public SqmPath<?> visitEntityNaturalIdExpression(HqlParser.EntityNaturalIdExpressionContext ctx) {
		return visitEntityNaturalIdReference( (HqlParser.EntityNaturalIdReferenceContext) ctx.getChild( 0 ) );
	}

	@Override
	public SqmPath<?> visitEntityNaturalIdReference(HqlParser.EntityNaturalIdReferenceContext ctx) {
		final SqmPath<Object> sqmPath = consumeDomainPath( (HqlParser.PathContext) ctx.getChild( 2 ) );
		final DomainType<?> sqmPathType = sqmPath.getReferencedPathSource().getSqmPathType();

		if ( sqmPathType instanceof IdentifiableDomainType<?> ) {
			@SuppressWarnings("unchecked")
			final IdentifiableDomainType<Object> identifiableType = (IdentifiableDomainType<? super Object>) sqmPathType;
			final List<? extends PersistentAttribute<Object, ?>> attributes = identifiableType.findNaturalIdAttributes();
			if ( attributes == null ) {
				throw new SemanticException(
						String.format(
								"Path '%s' resolved to entity type '%s' which does not define a natural id",
								sqmPath.getNavigablePath(),
								identifiableType.getTypeName()
						)
				);
			}
			else if ( attributes.size() >1 ) {
				throw new SemanticException(
						String.format(
								"Path '%s' resolved to entity type '%s' which defines multiple natural ids",
								sqmPath.getNavigablePath(),
								identifiableType.getTypeName()
						)
				);
			}

			@SuppressWarnings("unchecked")
			SingularAttribute<Object, ?> naturalIdAttribute
					= (SingularAttribute<Object, ?>) attributes.get(0);
			return sqmPath.get( naturalIdAttribute );
		}

		throw new SemanticException( "Path does not resolve to an entity type '" + sqmPath.getNavigablePath() + "'" );
	}

	@Override
	public Object visitToOneFkExpression(HqlParser.ToOneFkExpressionContext ctx) {
		return visitToOneFkReference( (HqlParser.ToOneFkReferenceContext) ctx.getChild( 0 ) );
	}

	@Override
	public SqmFkExpression<?> visitToOneFkReference(HqlParser.ToOneFkReferenceContext ctx) {
		final SqmPath<Object> sqmPath = consumeDomainPath( (HqlParser.PathContext) ctx.getChild( 2 ) );
		final SqmPathSource<?> toOneReference = sqmPath.getReferencedPathSource();

		final boolean validToOneRef = toOneReference.getBindableType() == Bindable.BindableType.SINGULAR_ATTRIBUTE
				&& toOneReference instanceof EntitySqmPathSource;

		if ( !validToOneRef ) {
			throw new SemanticException(
					String.format(
							Locale.ROOT,
							"`%s` used in `fk()` only supported for to-one mappings, but found `%s`",
							sqmPath.getNavigablePath(),
							toOneReference
					)
			);

		}

		return new SqmFkExpression( (SqmEntityValuedSimplePath<?>) sqmPath, creationContext.getNodeBuilder() );
	}

	@Override
	public SqmMapEntryReference<?, ?> visitMapEntrySelection(HqlParser.MapEntrySelectionContext ctx) {
		return new SqmMapEntryReference<>(
				consumePluralAttributeReference( (HqlParser.PathContext) ctx.getChild( 2 ) ),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmExpression<?> visitConcatenationExpression(HqlParser.ConcatenationExpressionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw nonCompliantFunctionException( "concat operator '||'" );
		}
		if ( ctx.getChildCount() != 3 ) {
			throw new ParsingException( "Expecting 2 operands to the concat operator" );
		}
		return getFunctionDescriptor( StandardFunctions.CONCAT ).generateSqmExpression(
				asList(
						(SqmExpression<?>) ctx.getChild( 0 ).accept( this ),
						(SqmExpression<?>) ctx.getChild( 2 ).accept( this )
				),
				null,
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public Object visitSignOperator(HqlParser.SignOperatorContext ctx) {
		switch ( ( (TerminalNode) ctx.getChild( 0 ) ).getSymbol().getType() ) {
			case HqlParser.PLUS:
				return UnaryArithmeticOperator.UNARY_PLUS;
			case HqlParser.MINUS:
				return UnaryArithmeticOperator.UNARY_MINUS;
			default:
				throw new QueryException( "missing operator" );
		}
	}

	@Override
	public Object visitAdditiveOperator(HqlParser.AdditiveOperatorContext ctx) {
		switch ( ( (TerminalNode) ctx.getChild( 0 ) ).getSymbol().getType() ) {
			case HqlParser.PLUS:
				return BinaryArithmeticOperator.ADD;
			case HqlParser.MINUS:
				return BinaryArithmeticOperator.SUBTRACT;
			default:
				throw new QueryException( "missing operator" );
		}
	}

	@Override
	public Object visitMultiplicativeOperator(HqlParser.MultiplicativeOperatorContext ctx) {
		switch ( ( (TerminalNode) ctx.getChild( 0 ) ).getSymbol().getType() ) {
			case HqlParser.ASTERISK:
				return BinaryArithmeticOperator.MULTIPLY;
			case HqlParser.SLASH:
				return BinaryArithmeticOperator.DIVIDE;
			case HqlParser.PERCENT_OP:
				return BinaryArithmeticOperator.MODULO;
			default:
				throw new QueryException( "missing operator" );
		}
	}

	@Override
	public Object visitAdditionExpression(HqlParser.AdditionExpressionContext ctx) {
		if ( ctx.getChildCount() != 3 ) {
			throw new ParsingException( "Expecting 2 operands to the additive operator" );
		}

		return new SqmBinaryArithmetic<>(
				(BinaryArithmeticOperator) ctx.getChild( 1 ).accept( this ),
				(SqmExpression<?>) ctx.getChild( 0 ).accept( this ),
				(SqmExpression<?>) ctx.getChild( 2 ).accept( this ),
				creationContext.getJpaMetamodel(),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public Object visitMultiplicationExpression(HqlParser.MultiplicationExpressionContext ctx) {
		if ( ctx.getChildCount() != 3 ) {
			throw new ParsingException( "Expecting 2 operands to the multiplicative operator" );
		}

		final SqmExpression<?> left = (SqmExpression<?>) ctx.getChild( 0 ).accept( this );
		final SqmExpression<?> right = (SqmExpression<?>) ctx.getChild( 2 ).accept( this );
		final BinaryArithmeticOperator operator = (BinaryArithmeticOperator) ctx.getChild( 1 ).accept( this );

		if ( operator == BinaryArithmeticOperator.MODULO ) {
			return getFunctionDescriptor( StandardFunctions.MOD ).generateSqmExpression(
					asList( left, right ),
					null,
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
				(SqmExpression<?>) ctx.getChild( 0 ).accept( this ),
				toDurationUnit( (SqmExtractUnit<?>) ctx.getChild( 1 ).accept( this ) ),
				resolveExpressibleTypeBasic( Duration.class ),
				creationContext.getNodeBuilder()
		);
	}

	private SqmDurationUnit<Long> toDurationUnit(SqmExtractUnit<?> extractUnit) {
		return new SqmDurationUnit<>(
				extractUnit.getUnit(),
				resolveExpressibleTypeBasic( Long.class ),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public Object visitFromDurationExpression(HqlParser.FromDurationExpressionContext ctx) {
		return new SqmByUnit(
				toDurationUnit( (SqmExtractUnit<?>) ctx.getChild( 2 ).accept( this ) ),
				(SqmExpression<?>) ctx.getChild( 0 ).accept( this ),
				resolveExpressibleTypeBasic( Long.class ),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmUnaryOperation<?> visitUnaryExpression(HqlParser.UnaryExpressionContext ctx) {
		return new SqmUnaryOperation<>(
				(UnaryArithmeticOperator) ctx.getChild( 0 ).accept( this ),
				(SqmExpression<?>) ctx.getChild( 1 ).accept( this )
		);
	}

	@Override
	public Object visitGroupedExpression(HqlParser.GroupedExpressionContext ctx) {
		return ctx.getChild( 1 ).accept( this );
	}

	@Override
	public Object visitCollateFunction(HqlParser.CollateFunctionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw new StrictJpaComplianceViolation(
					StrictJpaComplianceViolation.Type.COLLATIONS
			);
		}

		final SqmExpression<?> expressionToCollate = (SqmExpression<?>) ctx.getChild( 2 ).accept( this );
		final SqmCollation castTargetExpression = (SqmCollation) ctx.getChild( 4 ).accept( this );

		return getFunctionDescriptor( StandardFunctions.COLLATE ).generateSqmExpression(
				asList( expressionToCollate, castTargetExpression ),
				null, //why not string?
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public Object visitCollation(HqlParser.CollationContext ctx) {
		return new SqmCollation(
				ctx.getChild( 0 ).getText(),
				null,
				creationContext.getNodeBuilder() );
	}

	@Override
	public Object visitTupleExpression(HqlParser.TupleExpressionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw new StrictJpaComplianceViolation(
					StrictJpaComplianceViolation.Type.TUPLES
			);
		}
		final List<SqmExpression<?>> expressions = visitExpressions( ctx );
		return new SqmTuple<>( expressions, creationContext.getNodeBuilder() );
	}

	private List<SqmExpression<?>> visitExpressions(ParserRuleContext parentContext) {
		final int size = parentContext.getChildCount();
		// Shift 1 bit instead of division by 2
		final int estimateExpressionsCount = ( size >> 1 ) - 1;
		final List<SqmExpression<?>> expressions = new ArrayList<>( estimateExpressionsCount );
		for ( int i = 0; i < size; i++ ) {
			final ParseTree parseTree = parentContext.getChild( i );
			if ( parseTree instanceof HqlParser.ExpressionOrPredicateContext ) {
				expressions.add( (SqmExpression<?>) parseTree.accept( this ) );
			}
		}
		return expressions;
	}

	@Override
	public Object visitCaseExpression(HqlParser.CaseExpressionContext ctx) {
		return ctx.getChild( 0 ).accept( this );
	}

	@Override
	public SqmCaseSimple<?, ?> visitSimpleCaseList(HqlParser.SimpleCaseListContext ctx) {
		final int size = ctx.getChildCount();
		//noinspection unchecked
		final SqmCaseSimple<Object, Object> caseExpression = new SqmCaseSimple<>(
				(SqmExpression<Object>) ctx.getChild( 1 ).accept( this ),
				null,
				size - 3,
				creationContext.getNodeBuilder()
		);

		for ( int i = 2; i < size; i++ ) {
			final ParseTree parseTree = ctx.getChild( i );
			if ( parseTree instanceof HqlParser.SimpleCaseWhenContext ) {
				//noinspection unchecked
				caseExpression.when(
						(SqmExpression<Object>) parseTree.getChild( 1 ).accept( this ),
						(SqmExpression<Object>) parseTree.getChild( 3 ).accept( this )
				);
			}
		}

		final ParseTree lastChild = ctx.getChild( ctx.getChildCount() - 2 );
		if ( lastChild instanceof HqlParser.CaseOtherwiseContext ) {
			//noinspection unchecked
			caseExpression.otherwise( (SqmExpression<Object>) lastChild.getChild( 1 ).accept( this ) );
		}

		return caseExpression;
	}

	@Override
	public SqmCaseSearched<?> visitSearchedCaseList(HqlParser.SearchedCaseListContext ctx) {
		final int size = ctx.getChildCount();
		final SqmCaseSearched<Object> caseExpression = new SqmCaseSearched<>(
				null,
				size - 2,
				creationContext.getNodeBuilder()
		);

		for ( int i = 1; i < size; i++ ) {
			final ParseTree parseTree = ctx.getChild( i );
			if ( parseTree instanceof HqlParser.SearchedCaseWhenContext ) {
				//noinspection unchecked
				caseExpression.when(
						(SqmPredicate) parseTree.getChild( 1 ).accept( this ),
						(SqmExpression<Object>) parseTree.getChild( 3 ).accept( this )
				);
			}
		}

		final ParseTree lastChild = ctx.getChild( ctx.getChildCount() - 2 );
		if ( lastChild instanceof HqlParser.CaseOtherwiseContext ) {
			//noinspection unchecked
			caseExpression.otherwise( (SqmExpression<Object>) lastChild.getChild( 1 ).accept( this ) );
		}

		return caseExpression;
	}

	@Override
	public SqmExpression<?> visitCurrentDateFunction(HqlParser.CurrentDateFunctionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance()
				&& ( (TerminalNode) ctx.getChild( 0 ) ).getSymbol().getType() == HqlParser.CURRENT ) {
			throw nonCompliantFunctionException( "current date" );
		}
		return getFunctionDescriptor( StandardFunctions.CURRENT_DATE )
				.generateSqmExpression(
						resolveExpressibleTypeBasic( Date.class ),
						creationContext.getQueryEngine(),
						creationContext.getJpaMetamodel().getTypeConfiguration()
				);
	}

	@Override
	public SqmExpression<?> visitCurrentTimeFunction(HqlParser.CurrentTimeFunctionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance()
				&& ( (TerminalNode) ctx.getChild( 0 ) ).getSymbol().getType() == HqlParser.CURRENT ) {
			throw nonCompliantFunctionException( "current time" );
		}
		return getFunctionDescriptor( StandardFunctions.CURRENT_TIME )
				.generateSqmExpression(
						resolveExpressibleTypeBasic( Time.class ),
						creationContext.getQueryEngine(),
						creationContext.getJpaMetamodel().getTypeConfiguration()
				);
	}

	@Override
	public SqmExpression<?> visitCurrentTimestampFunction(HqlParser.CurrentTimestampFunctionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance()
				&& ( (TerminalNode) ctx.getChild( 0 ) ).getSymbol().getType() == HqlParser.CURRENT ) {
			throw nonCompliantFunctionException( "current timestamp" );
		}
		return getFunctionDescriptor( StandardFunctions.CURRENT_TIMESTAMP )
				.generateSqmExpression(
						resolveExpressibleTypeBasic( Timestamp.class ),
						creationContext.getQueryEngine(),
						creationContext.getJpaMetamodel().getTypeConfiguration()
				);
	}

	@Override
	public SqmExpression<?> visitInstantFunction(HqlParser.InstantFunctionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw nonCompliantFunctionException( StandardFunctions.INSTANT );
		}
		return getFunctionDescriptor( StandardFunctions.INSTANT )
				.generateSqmExpression(
						resolveExpressibleTypeBasic( Instant.class ),
						creationContext.getQueryEngine(),
						creationContext.getJpaMetamodel().getTypeConfiguration()
				);
	}

	@Override
	public SqmExpression<?> visitLocalDateFunction(HqlParser.LocalDateFunctionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance()
				&& ( (TerminalNode) ctx.getChild( 0 ) ).getSymbol().getType() == HqlParser.LOCAL_DATE ) {
			throw nonCompliantFunctionException( StandardFunctions.LOCAL_DATE );
		}
		return getFunctionDescriptor( StandardFunctions.LOCAL_DATE )
				.generateSqmExpression(
						resolveExpressibleTypeBasic( LocalDate.class ),
						creationContext.getQueryEngine(),
						creationContext.getJpaMetamodel().getTypeConfiguration()
				);
	}

	@Override
	public SqmExpression<?> visitLocalTimeFunction(HqlParser.LocalTimeFunctionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance()
				&& ( (TerminalNode) ctx.getChild( 0 ) ).getSymbol().getType() == HqlParser.LOCAL_TIME ) {
			throw nonCompliantFunctionException( StandardFunctions.LOCAL_TIME );
		}
		return getFunctionDescriptor( StandardFunctions.LOCAL_TIME )
				.generateSqmExpression(
						resolveExpressibleTypeBasic( LocalTime.class ),
						creationContext.getQueryEngine(),
						creationContext.getJpaMetamodel().getTypeConfiguration()
				);
	}

	@Override
	public SqmExpression<?> visitLocalDateTimeFunction(HqlParser.LocalDateTimeFunctionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance()
				&& ( (TerminalNode) ctx.getChild( 0 ) ).getSymbol().getType() == HqlParser.LOCAL_DATETIME ) {
			throw nonCompliantFunctionException( StandardFunctions.LOCAL_DATETIME );
		}
		return getFunctionDescriptor( StandardFunctions.LOCAL_DATETIME )
				.generateSqmExpression(
						resolveExpressibleTypeBasic( LocalDateTime.class ),
						creationContext.getQueryEngine(),
						creationContext.getJpaMetamodel().getTypeConfiguration()
				);
	}

	@Override
	public SqmExpression<?> visitOffsetDateTimeFunction(HqlParser.OffsetDateTimeFunctionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw nonCompliantFunctionException( StandardFunctions.OFFSET_DATETIME );
		}
		return getFunctionDescriptor( StandardFunctions.OFFSET_DATETIME )
				.generateSqmExpression(
						resolveExpressibleTypeBasic( OffsetDateTime.class ),
						creationContext.getQueryEngine(),
						creationContext.getJpaMetamodel().getTypeConfiguration()
				);
	}

	@Override
	public SqmExpression<?> visitLiteralExpression(HqlParser.LiteralExpressionContext ctx) {
		return (SqmExpression<?>) ctx.getChild( 0 ).accept( this );
	}

	@Override
	public SqmExpression<?> visitUnaryNumericLiteralExpression(HqlParser.UnaryNumericLiteralExpressionContext ctx) {
		final TerminalNode node = (TerminalNode) ctx.getChild( 1 ).getChild( 0 );
		final String text;
		if ( ( (TerminalNode) ctx.getChild( 0 ).getChild( 0 ) ).getSymbol().getType() == HqlParser.MINUS ) {
			text = "-" + node.getText();
		}
		else {
			text = node.getText();
		}
		switch ( node.getSymbol().getType() ) {
			case HqlParser.INTEGER_LITERAL:
				return integerOrLongLiteral( text );
			case HqlParser.LONG_LITERAL:
				return longLiteral( text );
			case HqlParser.BIG_INTEGER_LITERAL:
				return bigIntegerLiteral( text );
			case HqlParser.HEX_LITERAL:
				return hexLiteral( text );
			case HqlParser.FLOAT_LITERAL:
				return floatLiteral( text );
			case HqlParser.DOUBLE_LITERAL:
				return doubleLiteral( text );
			case HqlParser.BIG_DECIMAL_LITERAL:
				return bigDecimalLiteral( text );
			default:
				throw new ParsingException("Unexpected terminal node [" + text + "]");
		}
	}

	@Override
	public Object visitBinaryLiteral(HqlParser.BinaryLiteralContext ctx) {
		final TerminalNode firstNode = (TerminalNode) ctx.getChild( 0 );
		if ( firstNode.getSymbol().getType() == HqlParser.BINARY_LITERAL ) {
			return binaryLiteral( firstNode.getText() );
		}
		else {
			final StringBuilder text = new StringBuilder( "x'" );
			final int size = ctx.getChildCount();
			for ( int i = 0; i < size; i++ ) {
				final TerminalNode hex = (TerminalNode) ctx.getChild( i );
				if ( hex.getSymbol().getType() == HqlParser.HEX_LITERAL ) {
					final String hexText = hex.getText();
					if ( hexText.length() != 4 ) {
						throw new LiteralNumberFormatException( "not a byte: " + hexText );
					}
					text.append( hexText, 2, hexText.length() );
				}
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
				return integerOrLongLiteral( node.getText() );
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
	public Object visitDateTimeLiteral(HqlParser.DateTimeLiteralContext ctx) {
		return ctx.getChild( 1 ).accept( this );
	}

	@Override
	public Object visitDateLiteral(HqlParser.DateLiteralContext ctx) {
		return ctx.getChild( 1 ).accept( this );
	}

	@Override
	public Object visitTimeLiteral(HqlParser.TimeLiteralContext ctx) {
		return ctx.getChild( 1 ).accept( this );
	}

	@Override
	public Object visitJdbcTimestampLiteral(HqlParser.JdbcTimestampLiteralContext ctx) {
		final ParseTree parseTree = ctx.getChild( 1 );
		if ( parseTree instanceof HqlParser.DateTimeContext ) {
			return parseTree.accept( this );
		}
		else {
			return sqlTimestampLiteralFrom( parseTree.getText() );
		}
	}

	@Override
	public Object visitJdbcDateLiteral(HqlParser.JdbcDateLiteralContext ctx) {
		final ParseTree parseTree = ctx.getChild( 1 );
		if ( parseTree instanceof HqlParser.DateContext ) {
			return parseTree.accept( this );
		}
		else {
			return sqlDateLiteralFrom( parseTree.getText() );
		}
	}

	@Override
	public Object visitJdbcTimeLiteral(HqlParser.JdbcTimeLiteralContext ctx) {
		final ParseTree parseTree = ctx.getChild( 1 );
		if ( parseTree instanceof HqlParser.TimeContext ) {
			return parseTree.accept( this );
		}
		else {
			return sqlTimeLiteralFrom( parseTree.getText() );
		}
	}

	@Override
	public Object visitDateTime(HqlParser.DateTimeContext ctx) {
		final ParseTree parseTree = ctx.getChild( 2 );
		if ( parseTree instanceof HqlParser.ZoneIdContext || parseTree == null ) {
			return dateTimeLiteralFrom(
					(HqlParser.DateContext) ctx.getChild( 0 ),
					(HqlParser.TimeContext) ctx.getChild( 1 ),
					(HqlParser.ZoneIdContext) parseTree
			);
		}
		else {
			return offsetDatetimeLiteralFrom(
					(HqlParser.DateContext) ctx.getChild( 0 ),
					(HqlParser.TimeContext) ctx.getChild( 1 ),
					(HqlParser.OffsetContext) parseTree
			);
		}
	}

	private SqmLiteral<?> dateTimeLiteralFrom(
			HqlParser.DateContext date,
			HqlParser.TimeContext time,
			HqlParser.ZoneIdContext timezone) {
		if ( timezone == null ) {
			return new SqmLiteral<>(
					LocalDateTime.of( localDate( date ), localTime( time ) ),
					resolveExpressibleTypeBasic( LocalDateTime.class ),
					creationContext.getNodeBuilder()
			);
		}
		else {
			final ZoneId zoneId = visitZoneId( timezone );
			return new SqmLiteral<>(
					ZonedDateTime.of( localDate( date ), localTime( time ), zoneId ),
					resolveExpressibleTypeBasic( ZonedDateTime.class ),
					creationContext.getNodeBuilder()
			);
		}
	}

	@Override
	public ZoneId visitZoneId(HqlParser.ZoneIdContext ctx) {
		final TerminalNode firstChild = (TerminalNode) ctx.getChild( 0 );
		final String timezoneText;
		if ( firstChild.getSymbol().getType() == HqlParser.STRING_LITERAL ) {
			timezoneText = QuotingHelper.unquoteStringLiteral( ctx.getText() );
		}
		else {
			timezoneText = ctx.getText();
		}
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
				resolveExpressibleTypeBasic( OffsetDateTime.class ),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public Object visitDate(HqlParser.DateContext ctx) {
		return new SqmLiteral<>(
				localDate( ctx ),
				resolveExpressibleTypeBasic( LocalDate.class ),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public Object visitTime(HqlParser.TimeContext ctx) {
		return new SqmLiteral<>(
				localTime( ctx ),
				resolveExpressibleTypeBasic( LocalTime.class ),
				creationContext.getNodeBuilder()
		);
	}

	private static LocalTime localTime(HqlParser.TimeContext ctx) {
		final int hour = Integer.parseInt( ctx.getChild( 0 ).getText() );
		final int minute = Integer.parseInt( ctx.getChild( 2 ).getText() );
		if ( ctx.getChildCount() == 5 ) {
			final String secondText = ctx.getChild( 4 ).getText();
			final int index = secondText.indexOf( '.');
			if ( index < 0 ) {
				return LocalTime.of(
						hour,
						minute,
						Integer.parseInt( secondText )
				);
			}
			else {
				return LocalTime.of(
						hour,
						minute,
						Integer.parseInt( secondText.substring( 0, index ) ),
						Integer.parseInt( secondText.substring( index + 1 ) )
				);
			}
		}
		else {
			return LocalTime.of( hour, minute );
		}
	}

	private static LocalDate localDate(HqlParser.DateContext ctx) {
		return LocalDate.of(
				Integer.parseInt( ctx.getChild( 0 ).getText() ),
				Integer.parseInt( ctx.getChild( 2 ).getText() ),
				Integer.parseInt( ctx.getChild( 4 ).getText() )
		);
	}

	private static ZoneOffset zoneOffset(HqlParser.OffsetContext offset) {
		final int factor = ( (TerminalNode) offset.getChild( 0 ) ).getSymbol().getType() == PLUS ? 1 : -1;
		final int hour = factor * Integer.parseInt( offset.getChild( 1 ).getText() );
		if ( offset.getChildCount() == 2 ) {
			return ZoneOffset.ofHours( hour );
		}
		return ZoneOffset.ofHoursMinutes(
				hour,
				factor * Integer.parseInt( offset.getChild( 3 ).getText() )
		);
	}

//	private SqmLiteral<OffsetDateTime> offsetDatetimeLiteralFrom(String literalText) {
//		TemporalAccessor parsed = OFFSET_DATE_TIME.parse( literalText );
//		return new SqmLiteral<>(
//				OffsetDateTime.from( parsed ),
//				resolveExpressibleTypeBasic( OffsetDateTime.class ),
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
//					resolveExpressibleTypeBasic( ZonedDateTime.class ),
//					creationContext.getNodeBuilder()
//			);
//		}
//		catch (DateTimeException dte) {
//			return new SqmLiteral<>(
//					LocalDateTime.from( parsed ),
//					resolveExpressibleTypeBasic( LocalDateTime.class ),
//					creationContext.getNodeBuilder()
//			);
//		}
//	}
//
//	private SqmLiteral<LocalDate> localDateLiteralFrom(String literalText) {
//		return new SqmLiteral<>(
//				LocalDate.from( ISO_LOCAL_DATE.parse( literalText ) ),
//				resolveExpressibleTypeBasic( LocalDate.class ),
//				creationContext.getNodeBuilder()
//		);
//	}
//
//	private SqmLiteral<LocalTime> localTimeLiteralFrom(String literalText) {
//		return new SqmLiteral<>(
//				LocalTime.from( ISO_LOCAL_TIME.parse( literalText ) ),
//				resolveExpressibleTypeBasic( LocalTime.class ),
//				creationContext.getNodeBuilder()
//		);
//	}

	private SqmLiteral<?> sqlTimestampLiteralFrom(String literalText) {
		final TemporalAccessor parsed = DATE_TIME.parse( literalText.subSequence( 1, literalText.length() - 1 ) );
		try {
			final ZonedDateTime zonedDateTime = ZonedDateTime.from( parsed );
			final Calendar literal = GregorianCalendar.from( zonedDateTime );
			return new SqmLiteral<>(
					literal,
					resolveExpressibleTypeBasic( Calendar.class ),
					creationContext.getNodeBuilder()
			);
		}
		catch (DateTimeException dte) {
			final LocalDateTime localDateTime = LocalDateTime.from( parsed );
			final Timestamp literal = Timestamp.valueOf( localDateTime );
			return new SqmLiteral<>(
					literal,
					resolveExpressibleTypeBasic( Timestamp.class ),
					creationContext.getNodeBuilder()
			);
		}
	}

	private SqmLiteral<Date> sqlDateLiteralFrom(String literalText) {
		final LocalDate localDate = LocalDate.from( ISO_LOCAL_DATE.parse( literalText.subSequence( 1, literalText.length() - 1 ) ) );
		final Date literal = Date.valueOf( localDate );
		return new SqmLiteral<>(
				literal,
				resolveExpressibleTypeBasic( Date.class ),
				creationContext.getNodeBuilder()
		);
	}

	private SqmLiteral<Time> sqlTimeLiteralFrom(String literalText) {
		final LocalTime localTime = LocalTime.from( ISO_LOCAL_TIME.parse( literalText.subSequence( 1, literalText.length() - 1 ) ) );
		final Time literal = Time.valueOf( localTime );
		return new SqmLiteral<>(
				literal,
				resolveExpressibleTypeBasic( Time.class ),
				creationContext.getNodeBuilder()
		);
	}

	private SqmLiteral<Boolean> booleanLiteral(boolean value) {
		return new SqmLiteral<>(
				value,
				resolveExpressibleTypeBasic( Boolean.class ),
				creationContext.getNodeBuilder()
		);
	}

	private SqmLiteral<String> stringLiteral(String text) {
		return new SqmLiteral<>(
				QuotingHelper.unquoteStringLiteral( text ),
				resolveExpressibleTypeBasic( String.class ),
				creationContext.getNodeBuilder()
		);
	}

	private SqmLiteral<byte[]> binaryLiteral(String text) {
		return new SqmLiteral<>(
				PrimitiveByteArrayJavaType.INSTANCE.fromString(
						CharSequenceHelper.subSequence( text, 2, text.length() - 1 )
				),
				resolveExpressibleTypeBasic( byte[].class ),
				creationContext.getNodeBuilder()
		);
	}

	private SqmLiteral<? extends Number> integerOrLongLiteral(String text) {
		try {
			final Integer value = Integer.valueOf( text );
			return new SqmLiteral<>(
					value,
					resolveExpressibleTypeBasic( Integer.class ),
					creationContext.getNodeBuilder()
			);
		}
		catch (NumberFormatException e) {
			// This is at least what 5.x did
			try {
				final Long value = Long.valueOf( text );
				return new SqmLiteral<>(
						value,
						resolveExpressibleTypeBasic( Long.class ),
						creationContext.getNodeBuilder()
				);
			}
			catch (NumberFormatException e2) {
				e.addSuppressed( e2 );
				throw new LiteralNumberFormatException(
						"Unable to convert sqm literal [" + text + "] to Integer",
						e
				);
			}
		}
	}

	private SqmLiteral<Integer> integerLiteral(String text) {
		try {
			final Integer value = Integer.valueOf( text );
			return new SqmLiteral<>(
					value,
					resolveExpressibleTypeBasic( Integer.class ),
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
					resolveExpressibleTypeBasic( Long.class ),
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
				type = resolveExpressibleTypeBasic( Long.class );
			}
			else {
				value = Integer.parseUnsignedInt( text, 16 );
				type = resolveExpressibleTypeBasic( Integer.class );
			}
			//noinspection unchecked
			return new SqmLiteral<>(
					value,
					type,
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
					resolveExpressibleTypeBasic( BigInteger.class  ),
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
					resolveExpressibleTypeBasic( Float.class ),
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
					resolveExpressibleTypeBasic( Double.class ),
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
					resolveExpressibleTypeBasic( BigDecimal.class ),
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

	private <J> BasicType<J> resolveExpressibleTypeBasic(Class<J> javaType) {
		return creationContext.getJpaMetamodel().getTypeConfiguration().standardBasicTypeForJavaType( javaType );
	}

	@Override
	public Object visitParameterExpression(HqlParser.ParameterExpressionContext ctx) {
		return ctx.getChild( 0 ).accept( this );
	}

	@Override
	public SqmNamedParameter<?> visitNamedParameter(HqlParser.NamedParameterContext ctx) {
		return visitNamedParameter( ctx, null );
	}

	private <T> SqmNamedParameter<T> visitNamedParameter(
			HqlParser.NamedParameterContext ctx,
			SqmExpressible<T> expressibleType) {
		parameterStyle = parameterStyle.withNamed();
		final SqmNamedParameter<T> param = new SqmNamedParameter<>(
				ctx.getChild( 1 ).getText(),
				parameterDeclarationContextStack.getCurrent().isMultiValuedBindingAllowed(),
				expressibleType,
				creationContext.getNodeBuilder()
		);
		parameterCollector.addParameter( param );
		return param;
	}

	@Override
	public SqmPositionalParameter<?> visitPositionalParameter(HqlParser.PositionalParameterContext ctx) {
		return visitPositionalParameter( ctx, null );
	}

	private <T> SqmPositionalParameter<T> visitPositionalParameter(
			HqlParser.PositionalParameterContext ctx,
			SqmExpressible<T> expressibleType) {
		if ( ctx.getChildCount() == 1 ) {
			throw new SemanticException( "Unlabeled ordinal parameter ('?' rather than ?1)" );
		}
		parameterStyle = parameterStyle.withPositional();
		final SqmPositionalParameter<T> param = new SqmPositionalParameter<>(
				Integer.parseInt( ctx.getChild( 1 ).getText() ),
				parameterDeclarationContextStack.getCurrent().isMultiValuedBindingAllowed(),
				expressibleType,
				creationContext.getNodeBuilder()
		);
		parameterCollector.addParameter( param );
		return param;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Functions

	@Override
	public SqmExpression<?> visitJpaNonstandardFunction(HqlParser.JpaNonstandardFunctionContext ctx) {
		final String functionName = QuotingHelper.unquoteStringLiteral( ctx.getChild( 2 ).getText() ).toLowerCase();
		final List<SqmTypedNode<?>> functionArguments;
		if ( ctx.getChildCount() > 4 ) {
			//noinspection unchecked
			functionArguments = (List<SqmTypedNode<?>>) ctx.getChild( 4 ).accept( this );
		}
		else {
			functionArguments = emptyList();
		}

		SqmFunctionDescriptor functionTemplate = getFunctionDescriptor( functionName );
		if (functionTemplate == null) {
			functionTemplate = new NamedSqmFunctionDescriptor(
					functionName,
					true,
					null,
					StandardFunctionReturnTypeResolvers.invariant(
							resolveExpressibleTypeBasic( Object.class )
					),
					null
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
	public String visitGenericFunctionName(HqlParser.GenericFunctionNameContext ctx) {
		StringBuilder functionName = new StringBuilder( visitIdentifier( ctx.simplePath().identifier() ) );
		for ( HqlParser.SimplePathElementContext sp: ctx.simplePath().simplePathElement() ) {
			// allow function names of form foo.bar to be located in the registry
			functionName.append('.').append( visitIdentifier( sp.identifier() ) );
		}
		return functionName.toString();
	}

	@Override
	public Object visitGenericFunction(HqlParser.GenericFunctionContext ctx) {
		final String originalFunctionName = visitGenericFunctionName( ctx.genericFunctionName() );
		final String functionName = originalFunctionName.toLowerCase();
		if ( creationOptions.useStrictJpaCompliance() && !JPA_STANDARD_FUNCTIONS.contains( functionName ) ) {
			throw nonCompliantFunctionException( originalFunctionName );
		}

		final ParseTree argumentChild = ctx.getChild( 2 );
		final List<SqmTypedNode<?>> functionArguments;
		if ( argumentChild instanceof HqlParser.GenericFunctionArgumentsContext ) {
			functionArguments = (List<SqmTypedNode<?>>) argumentChild.accept( this );
		}
		else if ( "*".equals( argumentChild.getText() ) ) {
			functionArguments = Collections.singletonList( new SqmStar( getCreationContext().getNodeBuilder() ) );
		}
		else {
			functionArguments = emptyList();
		}

		final Boolean fromFirst = getFromFirst( ctx );
		final Boolean respectNulls = getRespectNullsClause( ctx );
		final SqmOrderByClause withinGroup = getWithinGroup( ctx );
		final SqmPredicate filterExpression = getFilterExpression( ctx );
		final boolean hasOverClause = ctx.getChild( ctx.getChildCount() - 1 ) instanceof HqlParser.OverClauseContext;
		SqmFunctionDescriptor functionTemplate = getFunctionDescriptor( functionName );
		if ( functionTemplate == null ) {
			FunctionKind functionKind = FunctionKind.NORMAL;
			if ( withinGroup != null ) {
				functionKind = FunctionKind.ORDERED_SET_AGGREGATE;
			}
			else if ( hasOverClause ) {
				functionKind = FunctionKind.WINDOW;
			}
			else if ( filterExpression != null ) {
				functionKind = FunctionKind.AGGREGATE;
			}
			functionTemplate = new NamedSqmFunctionDescriptor(
					functionName,
					true,
					null,
					StandardFunctionReturnTypeResolvers.invariant(
							resolveExpressibleTypeBasic( Object.class )
					),
					null,
					functionName,
					functionKind,
					null,
					SqlAstNodeRenderingMode.DEFAULT
			);
		}
		else {
			if ( hasOverClause && functionTemplate.getFunctionKind() == FunctionKind.NORMAL ) {
				throw new SemanticException( "OVER clause is illegal for normal function: " + functionName );
			}
			else if ( !hasOverClause && functionTemplate.getFunctionKind() == FunctionKind.WINDOW ) {
				throw new SemanticException( "OVER clause is mandatory for window-only function: " + functionName );
			}
			if ( respectNulls != null ) {
				switch ( functionName ) {
					case StandardFunctions.LAG:
					case StandardFunctions.LEAD:
					case StandardFunctions.FIRST_VALUE:
					case StandardFunctions.LAST_VALUE:
					case StandardFunctions.NTH_VALUE:
						break;
					default:
						throw new SemanticException( "RESPECT/IGNORE NULLS is illegal for function: " + functionName );
				}
			}
			if ( fromFirst != null && !StandardFunctions.NTH_VALUE.equals( functionName ) ) {
				throw new SemanticException( "FROM FIRST/LAST is illegal for function: " + functionName );
			}
		}

		final SqmFunction<?> function;
		switch ( functionTemplate.getFunctionKind() ) {
			case ORDERED_SET_AGGREGATE:
				function = functionTemplate.generateOrderedSetAggregateSqmExpression(
						functionArguments,
						filterExpression,
						withinGroup,
						null,
						creationContext.getQueryEngine(),
						creationContext.getJpaMetamodel().getTypeConfiguration()
				);
				break;
			case AGGREGATE:
				function = functionTemplate.generateAggregateSqmExpression(
						functionArguments,
						filterExpression,
						null,
						creationContext.getQueryEngine(),
						creationContext.getJpaMetamodel().getTypeConfiguration()
				);
				break;
			case WINDOW:
				function = functionTemplate.generateWindowSqmExpression(
						functionArguments,
						filterExpression,
						null,
						null,
						null,
						creationContext.getQueryEngine(),
						creationContext.getJpaMetamodel().getTypeConfiguration()
				);
				break;
			default:
				if ( filterExpression != null ) {
					throw new ParsingException( "Illegal use of a FILTER clause for non-aggregate function: " + originalFunctionName );
				}
				function = functionTemplate.generateSqmExpression(
						functionArguments,
						null,
						creationContext.getQueryEngine(),
						creationContext.getJpaMetamodel().getTypeConfiguration()
				);
				break;
		}
		return applyOverClause( ctx, function );
	}

	private StrictJpaComplianceViolation nonCompliantFunctionException(String originalFunctionName) {
		return new StrictJpaComplianceViolation(
				"Encountered non-compliant non-standard function call [" +
						originalFunctionName + "], but strict JPA " +
						"compliance was requested; use JPA's FUNCTION(functionName[,...]) " +
						"syntax name instead",
				StrictJpaComplianceViolation.Type.FUNCTION_CALL
		);
	}

	@Override
	public Object visitListaggFunction(ListaggFunctionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw nonCompliantFunctionException( StandardFunctions.LISTAGG );
		}
		final SqmFunctionDescriptor functionTemplate = getFunctionDescriptor( StandardFunctions.LISTAGG );
		if ( functionTemplate == null ) {
			throw new SemanticException(
					"The listagg function was not registered for the dialect"
			);
		}
		final int argumentStartIndex;
		final ParseTree thirdChild = ctx.getChild( 2 );
		final boolean distinct;
		if ( thirdChild instanceof TerminalNode ) {
			distinct = true;
			argumentStartIndex = 3;
		}
		else {
			distinct = false;
			argumentStartIndex = 2;
		}
		final SqmExpression<?> firstArgument = (SqmExpression<?>) ctx.getChild( argumentStartIndex ).accept( this );
		final SqmExpression<?> secondArgument = (SqmExpression<?>) ctx.getChild( argumentStartIndex + 2 ).accept( this );
		final ParseTree overflowCtx = ctx.getChild( argumentStartIndex + 3 );
		final List<SqmTypedNode<?>> functionArguments = new ArrayList<>( 3 );
		if ( distinct ) {
			functionArguments.add( new SqmDistinct<>( firstArgument, creationContext.getNodeBuilder() ) );
		}
		else {
			functionArguments.add( firstArgument );
		}
		if ( overflowCtx instanceof OnOverflowClauseContext ) {
			if ( overflowCtx.getChildCount() > 3 ) {
				// ON OVERFLOW TRUNCATE
				final TerminalNode countNode = (TerminalNode) overflowCtx.getChild( overflowCtx.getChildCount() - 2 );
				final boolean withCount = countNode.getSymbol().getType() == HqlParser.WITH;
				final SqmExpression<?> fillerExpression;
				if ( overflowCtx.getChildCount() == 6 ) {
					fillerExpression = (SqmExpression<?>) overflowCtx.getChild( 3 ).accept( this );
				}
				else {
					// The SQL standard says the default is three periods `...`
					fillerExpression = new SqmLiteral<>(
							"...",
							secondArgument.getNodeType(),
							secondArgument.nodeBuilder()
					);
				}
				//noinspection unchecked,rawtypes
				functionArguments.add( new SqmOverflow( secondArgument, fillerExpression, withCount ) );
			}
			else {
				// ON OVERFLOW ERROR
				functionArguments.add( new SqmOverflow<>( secondArgument, null, false ) );
			}
		}
		else {
			functionArguments.add( secondArgument );
		}
		final SqmOrderByClause withinGroup = getWithinGroup( ctx );
		final SqmPredicate filterExpression = getFilterExpression( ctx );
		return applyOverClause(
				ctx,
				functionTemplate.generateOrderedSetAggregateSqmExpression(
						functionArguments,
						filterExpression,
						withinGroup,
						null,
						creationContext.getQueryEngine(),
						creationContext.getJpaMetamodel().getTypeConfiguration()
				)
		);
	}

	@Override
	public List<SqmTypedNode<?>> visitGenericFunctionArguments(HqlParser.GenericFunctionArgumentsContext ctx) {
		final int size = ctx.getChildCount();
		final int lastIndex = size - 1;
		// Shift 1 bit instead of division by 2
		final int estimateArgumentCount = size >> 1;
		final List<SqmTypedNode<?>> arguments = new ArrayList<>( estimateArgumentCount );
		int i = 0;

		boolean distinct = false;
		final ParseTree firstChild = ctx.getChild( 0 );
		if ( firstChild instanceof HqlParser.DatetimeFieldContext ) {
			arguments.add( toDurationUnit( (SqmExtractUnit<?>) firstChild.accept( this ) ) );
			i += 2;
		}
		else if ( firstChild instanceof TerminalNode ) {
			distinct = true;
			i++;
		}

		for ( ; i < size; i += 2 ) {
			// we handle the final argument differently...
			if ( i == lastIndex ) {
				arguments.add( visitFinalFunctionArgument( ctx.getChild( i ) ) );
			}
			else {
				arguments.add( (SqmTypedNode<?>) ctx.getChild( i ).accept( this ) );
			}
		}

		if ( distinct ) {
			final NodeBuilder nodeBuilder = getCreationContext().getNodeBuilder();
			if ( arguments.size() == 1 ) {
				arguments.set( 0, new SqmDistinct<>( (SqmExpression<?>) arguments.get( 0 ), nodeBuilder ) );
			}
			else {
				final List<SqmTypedNode<?>> newArguments = new ArrayList<>( 1 );
				//noinspection unchecked
				newArguments.add(
						new SqmDistinct<>(
								new SqmTuple<>( (List<SqmExpression<?>>) (List<?>) arguments, nodeBuilder ), nodeBuilder
						)
				);
				return newArguments;
			}
		}
		return arguments;
	}

	private SqmExpression<?> visitFinalFunctionArgument(ParseTree expression) {
		// the final argument to a function may accept multi-value parameter (varargs),
		// 		but only if we are operating in non-strict JPA mode
		parameterDeclarationContextStack.push( () -> !creationOptions.useStrictJpaCompliance() );
		try {
			return (SqmExpression<?>) expression.accept( this );
		}
		finally {
			parameterDeclarationContextStack.pop();
		}
	}

	private SqmFunctionDescriptor getFunctionDescriptor(String name) {
		return creationContext.getQueryEngine().getSqmFunctionRegistry().findFunctionDescriptor( name );
	}

	@Override
	public SqmExtractUnit<?> visitDatetimeField(HqlParser.DatetimeFieldContext ctx) {
		final NodeBuilder nodeBuilder = creationContext.getNodeBuilder();
		switch ( ( (TerminalNode) ctx.getChild( 0 ) ).getSymbol().getType() ) {
			case HqlParser.DAY:
				return new SqmExtractUnit<>(
						TemporalUnit.DAY,
						resolveExpressibleTypeBasic( Integer.class ),
						nodeBuilder
				);
			case HqlParser.MONTH:
				return new SqmExtractUnit<>(
						TemporalUnit.MONTH,
						resolveExpressibleTypeBasic( Integer.class ),
						nodeBuilder
				);
			case HqlParser.YEAR:
				return new SqmExtractUnit<>(
						TemporalUnit.YEAR,
						resolveExpressibleTypeBasic( Integer.class ),
						nodeBuilder
				);
			case HqlParser.HOUR:
				return new SqmExtractUnit<>(
						TemporalUnit.HOUR,
						resolveExpressibleTypeBasic( Integer.class ),
						nodeBuilder
				);
			case HqlParser.MINUTE:
				return new SqmExtractUnit<>(
						TemporalUnit.MINUTE,
						resolveExpressibleTypeBasic( Integer.class ),
						nodeBuilder
				);
			case HqlParser.SECOND:
				return new SqmExtractUnit<>(
						TemporalUnit.SECOND,
						resolveExpressibleTypeBasic( Float.class ),
						nodeBuilder
				);
			case HqlParser.NANOSECOND:
				return new SqmExtractUnit<>(
						NANOSECOND,
						resolveExpressibleTypeBasic( Long.class ),
						nodeBuilder
				);
			case HqlParser.WEEK:
				return new SqmExtractUnit<>(
						TemporalUnit.WEEK,
						resolveExpressibleTypeBasic( Integer.class ),
						nodeBuilder
				);
			case HqlParser.QUARTER:
				return new SqmExtractUnit<>(
						TemporalUnit.QUARTER,
						resolveExpressibleTypeBasic( Integer.class ),
						nodeBuilder
				);
		}
		throw new ParsingException( "Unsupported datetime field [" + ctx.getText() + "]" );
	}

	@Override
	public Object visitDayField(HqlParser.DayFieldContext ctx) {
		final NodeBuilder nodeBuilder = creationContext.getNodeBuilder();
		switch ( ( (TerminalNode) ctx.getChild( 2 ) ).getSymbol().getType() ) {
			case HqlParser.MONTH:
				return new SqmExtractUnit<>( DAY_OF_MONTH, resolveExpressibleTypeBasic( Integer.class ), nodeBuilder );
			case HqlParser.WEEK:
				return new SqmExtractUnit<>( DAY_OF_WEEK, resolveExpressibleTypeBasic( Integer.class ), nodeBuilder );
			case HqlParser.YEAR:
				return new SqmExtractUnit<>( DAY_OF_YEAR, resolveExpressibleTypeBasic( Integer.class ), nodeBuilder );
		}
		throw new ParsingException("Unsupported day field [" + ctx.getText() + "]");
	}

	@Override
	public Object visitWeekField(HqlParser.WeekFieldContext ctx) {
		final NodeBuilder nodeBuilder = creationContext.getNodeBuilder();
		switch ( ( (TerminalNode) ctx.getChild( 2 ) ).getSymbol().getType() ) {
			case HqlParser.MONTH:
				//this is computed from DAY_OF_MONTH/7
				return new SqmExtractUnit<>( WEEK_OF_MONTH, resolveExpressibleTypeBasic( Integer.class ), nodeBuilder );
			case HqlParser.YEAR:
				//this is computed from DAY_OF_YEAR/7
				return new SqmExtractUnit<>( WEEK_OF_YEAR, resolveExpressibleTypeBasic( Integer.class ), nodeBuilder );
		}
		throw new ParsingException("Unsupported week field [" + ctx.getText() + "]");
	}

	@Override
	public Object visitDateOrTimeField(HqlParser.DateOrTimeFieldContext ctx) {
		final NodeBuilder nodeBuilder = creationContext.getNodeBuilder();
		switch ( ( (TerminalNode) ctx.getChild( 0 ) ).getSymbol().getType() ) {
			case HqlParser.DATE:
				return isExtractingJdbcTemporalType
						? new SqmExtractUnit<>( DATE, resolveExpressibleTypeBasic( Date.class ), nodeBuilder )
						: new SqmExtractUnit<>( DATE, resolveExpressibleTypeBasic( LocalDate.class ), nodeBuilder );
			case HqlParser.TIME:
				return isExtractingJdbcTemporalType
						? new SqmExtractUnit<>( TIME, resolveExpressibleTypeBasic( Time.class ), nodeBuilder )
						: new SqmExtractUnit<>( TIME, resolveExpressibleTypeBasic( LocalTime.class ), nodeBuilder );
		}
		throw new ParsingException("Unsupported date or time field [" + ctx.getText() + "]");
	}

	@Override
	public Object visitTimeZoneField(HqlParser.TimeZoneFieldContext ctx) {
		final NodeBuilder nodeBuilder = creationContext.getNodeBuilder();
		switch ( ( (TerminalNode) ctx.getChild( ctx.getChildCount() - 1 ) ).getSymbol().getType() ) {
			case HqlParser.TIMEZONE_HOUR:
				return new SqmExtractUnit<>( TIMEZONE_HOUR, resolveExpressibleTypeBasic( Integer.class ), nodeBuilder );
			case HqlParser.TIMEZONE_MINUTE:
				return new SqmExtractUnit<>(
						TIMEZONE_MINUTE,
						resolveExpressibleTypeBasic( Integer.class ),
						nodeBuilder
				);
			default:
				return new SqmExtractUnit<>( OFFSET, resolveExpressibleTypeBasic( ZoneOffset.class ), nodeBuilder );
		}
	}

	private boolean isExtractingJdbcTemporalType;

	@Override
	public Object visitExtractFunction(HqlParser.ExtractFunctionContext ctx) {
		final SqmExpression<?> expressionToExtract = (SqmExpression<?>) ctx.getChild( ctx.getChildCount() - 2 )
				.accept( this );

		// visitDateOrTimeField() needs to know if we're extracting from a
		// JDBC Timestamp or from a java.time LocalDateTime/OffsetDateTime
		isExtractingJdbcTemporalType = isJdbcTemporalType( expressionToExtract.getNodeType() );

		final SqmExtractUnit<?> extractFieldExpression;
		if ( ctx.getChild( 0 ) instanceof TerminalNode ) {
			//for the case of the full ANSI syntax "extract(field from arg)"
			extractFieldExpression = (SqmExtractUnit<?>) ctx.getChild( 2 ).accept(this);
		}
		else {
			//for the shorter legacy Hibernate syntax "field(arg)"
			extractFieldExpression = (SqmExtractUnit<?>) ctx.getChild( 0 ).accept(this);
//			//Prefer an existing native version if available
//			final SqmFunctionDescriptor functionDescriptor = getFunctionDescriptor( extractFieldExpression.getUnit().name() );
//			if ( functionDescriptor != null ) {
//				return functionDescriptor.generateSqmExpression(
//						expressionToExtract,
//						null,
//						creationContext.getQueryEngine(),
//						creationContext.getJpaMetamodel().getTypeConfiguration()
//				);
//			}
		}

		return getFunctionDescriptor( StandardFunctions.EXTRACT ).generateSqmExpression(
				asList( extractFieldExpression, expressionToExtract ),
				extractFieldExpression.getType(),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public Object visitFormat(HqlParser.FormatContext ctx) {
		final String format = QuotingHelper.unquoteStringLiteral( ctx.getChild( 0 ).getText() );
		return new SqmFormat(
				format,
				resolveExpressibleTypeBasic( String.class ),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmExpression<?> visitFormatFunction(HqlParser.FormatFunctionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw nonCompliantFunctionException( StandardFunctions.FORMAT );
		}
		final SqmExpression<?> expressionToCast = (SqmExpression<?>) ctx.getChild( 2 ).accept( this );
		final SqmLiteral<?> format = (SqmLiteral<?>) ctx.getChild( 4 ).accept( this );

		return getFunctionDescriptor( StandardFunctions.FORMAT ).generateSqmExpression(
				asList( expressionToCast, format ),
				null, //why not string?
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmExpression<?> visitCastFunction(HqlParser.CastFunctionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw nonCompliantFunctionException( StandardFunctions.CAST );
		}
		final SqmExpression<?> expressionToCast = (SqmExpression<?>) ctx.getChild( 2 ).accept( this );
		final SqmCastTarget<?> castTargetExpression = (SqmCastTarget<?>) ctx.getChild( 4 ).accept( this );

		return getFunctionDescriptor( StandardFunctions.CAST ).generateSqmExpression(
				asList( expressionToCast, castTargetExpression ),
				castTargetExpression.getType(),
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmCastTarget<?> visitCastTarget(HqlParser.CastTargetContext castTargetContext) {
		final HqlParser.CastTargetTypeContext castTargetTypeContext = (HqlParser.CastTargetTypeContext) castTargetContext.getChild( 0 );
		final String targetName = castTargetTypeContext.fullTargetName;

		Long length = null;
		Integer precision = null;
		Integer scale = null;
		switch ( castTargetTypeContext.getChildCount() ) {
			case 6:
				scale = Integer.valueOf( castTargetTypeContext.getChild( 4 ).getText() );
			case 4:
				length = Long.valueOf( castTargetTypeContext.getChild( 2 ).getText() );
				precision = length.intValue();
				break;
		}

		return new SqmCastTarget<>(
				(ReturnableType<?>)
						creationContext.getJpaMetamodel().getTypeConfiguration()
								.resolveCastTargetType( targetName ),
				//TODO: is there some way to interpret as length vs precision/scale here at this point?
				length,
				precision,
				scale,
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public Object visitPositionFunction(HqlParser.PositionFunctionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw nonCompliantFunctionException( StandardFunctions.POSITION );
		}
		final SqmExpression<?> pattern = (SqmExpression<?>) ctx.getChild( 2 ).accept( this );
		final SqmExpression<?> string = (SqmExpression<?>) ctx.getChild( 4 ).accept( this );

		return getFunctionDescriptor( StandardFunctions.POSITION ).generateSqmExpression(
				asList( pattern, string ),
				null,
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public Object visitOverlayFunction(HqlParser.OverlayFunctionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw nonCompliantFunctionException( StandardFunctions.OVERLAY );
		}
		final SqmExpression<?> string = (SqmExpression<?>) ctx.getChild( 2 ).accept( this );
		final SqmExpression<?> replacement = (SqmExpression<?>) ctx.getChild( 4 ).accept( this );
		final SqmExpression<?> start = (SqmExpression<?>) ctx.getChild( 6 ).accept( this );
		final SqmExpression<?> length;
		if ( ctx.getChildCount() == 10 ) {
			length = (SqmExpression<?>) ctx.getChild( 8 ).accept( this );
		}
		else {
			length = null;
		}

		return getFunctionDescriptor( StandardFunctions.OVERLAY ).generateSqmExpression(
				length == null
						? asList( string, replacement, start )
						: asList( string, replacement, start, length ),
				null,
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmExpression<?> visitEveryFunction(HqlParser.EveryFunctionContext ctx) {
		final SqmPredicate filterExpression = getFilterExpression( ctx );
		final ParseTree argumentChild = ctx.getChild( 2 );
		if ( argumentChild instanceof HqlParser.SubqueryContext ) {
			final SqmSubQuery<?> subquery = (SqmSubQuery<?>) argumentChild.accept( this );
			return new SqmEvery<>( subquery, creationContext.getNodeBuilder() );
		}
		else if ( argumentChild instanceof HqlParser.PredicateContext ) {
			if ( creationOptions.useStrictJpaCompliance() ) {
				throw nonCompliantFunctionException( StandardFunctions.EVERY );
			}
			final SqmExpression<?> argument = (SqmExpression<?>) argumentChild.accept( this );

			return applyOverClause(
					ctx,
					getFunctionDescriptor( StandardFunctions.EVERY ).generateAggregateSqmExpression(
							singletonList( argument ),
							filterExpression,
							resolveExpressibleTypeBasic( Boolean.class ),
							creationContext.getQueryEngine(),
							creationContext.getJpaMetamodel().getTypeConfiguration()
					)
			);
		}
		else {
			if ( getCreationOptions().useStrictJpaCompliance() ) {
				throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.HQL_COLLECTION_FUNCTION );
			}
			return new SqmEvery<>(
					createCollectionReferenceSubQuery(
							(HqlParser.SimplePathContext) ctx.getChild( 3 ),
							(TerminalNode) ctx.getChild( 1 )
					),
					creationContext.getNodeBuilder()
			);
		}
	}

	@Override
	public SqmExpression<?> visitAnyFunction(HqlParser.AnyFunctionContext ctx) {
		final SqmPredicate filterExpression = getFilterExpression( ctx );
		final ParseTree argumentChild = ctx.getChild( 2 );
		if ( argumentChild instanceof HqlParser.SubqueryContext ) {
			final SqmSubQuery<?> subquery = (SqmSubQuery<?>) argumentChild.accept( this );
			return new SqmAny<>( subquery, creationContext.getNodeBuilder() );
		}
		else if ( argumentChild instanceof HqlParser.PredicateContext ) {
			if ( creationOptions.useStrictJpaCompliance() ) {
				throw nonCompliantFunctionException( StandardFunctions.ANY );
			}
			final SqmExpression<?> argument = (SqmExpression<?>) argumentChild.accept( this );

			return applyOverClause(
					ctx,
					getFunctionDescriptor( StandardFunctions.ANY ).generateAggregateSqmExpression(
							singletonList( argument ),
							filterExpression,
							resolveExpressibleTypeBasic( Boolean.class ),
							creationContext.getQueryEngine(),
							creationContext.getJpaMetamodel().getTypeConfiguration()
					)
			);
		}
		else {
			if ( getCreationOptions().useStrictJpaCompliance() ) {
				throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.HQL_COLLECTION_FUNCTION );
			}
			return new SqmAny<>(
					createCollectionReferenceSubQuery(
							(HqlParser.SimplePathContext) ctx.getChild( 3 ),
							(TerminalNode) ctx.getChild( 1 )
					),
					creationContext.getNodeBuilder()
			);
		}
	}

	private <X> SqmSubQuery<X> createCollectionReferenceSubQuery(
			HqlParser.SimplePathContext pathCtx,
			TerminalNode collectionReferenceCtx) {
		final SqmPath<?> pluralAttributePath = consumeDomainPath( pathCtx );
		final SqmPathSource<?> referencedPathSource = pluralAttributePath.getReferencedPathSource();

		if ( !(referencedPathSource instanceof PluralPersistentAttribute ) ) {
			throw new PathException(
					"Path is not a plural path '" + pluralAttributePath.getNavigablePath() + "'"
			);
		}
		final SqmSubQuery<?> subQuery = new SqmSubQuery<>(
				processingStateStack.getCurrent().getProcessingQuery(),
				creationContext.getNodeBuilder()
		);
		final SqmSelectClause selectClause = new SqmSelectClause( false, 1, creationContext.getNodeBuilder() );
		final SqmFromClause fromClause = new SqmFromClause( 1 );
		SqmPath<?> lhs = pluralAttributePath.getLhs();
		final List<String> implicitJoinPaths = new ArrayList<>();
		while ( !( lhs instanceof AbstractSqmFrom<?, ?> ) ) {
			implicitJoinPaths.add( lhs.getNavigablePath().getLocalName() );
			lhs = lhs.getLhs();
		}
		final AbstractSqmFrom<?, ?> correlationBase = (AbstractSqmFrom<?, ?>) lhs;
		final SqmCorrelation<?, ?> correlation = correlationBase.createCorrelation();
		SqmFrom<?, ?> joinBase = correlation;
		for ( int i = implicitJoinPaths.size() - 1; i >= 0; i-- ) {
			joinBase = joinBase.join( implicitJoinPaths.get( i ) );
		}
		final SqmAttributeJoin<?, ?> collectionJoin = joinBase.join( pluralAttributePath.getNavigablePath().getLocalName() );
		fromClause.addRoot( correlation.getCorrelatedRoot() );
		if ( collectionReferenceCtx == null ) {
			final SqmLiteral<Integer> literal = new SqmLiteral<>(
					1,
					creationContext.getNodeBuilder().getIntegerType(),
					creationContext.getNodeBuilder()
			);
			subQuery.applyInferableType( literal.getNodeType() );
			selectClause.setSelection( literal );
		}
		else {
			final String partName;
			switch ( collectionReferenceCtx.getSymbol().getType() ) {
				case HqlParser.ELEMENTS:
					partName = CollectionPart.Nature.ELEMENT.getName();
					break;
				case HqlParser.INDICES:
					partName = CollectionPart.Nature.INDEX.getName();
					break;
				default:
					throw new ParsingException( "Unexpected collection reference : " + collectionReferenceCtx.getText() );
			}
			final SqmPath<?> path = collectionJoin.resolvePathPart( partName, true, this );
			subQuery.applyInferableType( path.getNodeType() );
			selectClause.setSelection( path );
		}
		final SqmQuerySpec<?> querySpec = subQuery.getQuerySpec();
		querySpec.setFromClause( fromClause );
		querySpec.setSelectClause( selectClause );
		//noinspection unchecked
		return (SqmSubQuery<X>) subQuery;
	}

	private SqmOrderByClause getWithinGroup(ParseTree functionCtx) {
		HqlParser.WithinGroupClauseContext ctx = null;
		for ( int i = functionCtx.getChildCount() - 3; i < functionCtx.getChildCount(); i++ ) {
			final ParseTree child = functionCtx.getChild( i );
			if ( child instanceof HqlParser.WithinGroupClauseContext ) {
				ctx = (HqlParser.WithinGroupClauseContext) child;
				break;
			}
		}
		if ( ctx != null ) {
			return visitOrderByClause( (HqlParser.OrderByClauseContext) ctx.getChild( 3 ) );
		}
		return null;
	}

	private Boolean getFromFirst(ParseTree functionCtx) {
		// The clause is either on index 3 or 4 is where the
		final int end = Math.min( functionCtx.getChildCount(), 5 );
		for ( int i = 3; i < end; i++ ) {
			final ParseTree child = functionCtx.getChild( i );
			if ( child instanceof HqlParser.NthSideClauseContext ) {
				final HqlParser.NthSideClauseContext subCtx = (HqlParser.NthSideClauseContext) child.getChild( 6 );
				return ( (TerminalNode) subCtx.getChild( 1 ) ).getSymbol().getType() == HqlParser.FIRST;
			}
		}
		return null;
	}

	private Boolean getRespectNullsClause(ParseTree functionCtx) {
		for ( int i = functionCtx.getChildCount() - 3; i < functionCtx.getChildCount(); i++ ) {
			final ParseTree child = functionCtx.getChild( i );
			if ( child instanceof HqlParser.NullsClauseContext ) {
				return ( (TerminalNode) child.getChild( 0 ) ).getSymbol().getType() == HqlParser.RESPECT;
			}
		}
		return null;
	}

	private SqmPredicate getFilterExpression(ParseTree functionCtx) {
		for ( int i = functionCtx.getChildCount() - 2; i < functionCtx.getChildCount(); i++ ) {
			final ParseTree child = functionCtx.getChild( i );
			if ( child instanceof HqlParser.FilterClauseContext ) {
				return (SqmPredicate) child.getChild( 2 ).getChild( 1 ).accept( this );
			}
		}
		return null;
	}

	private SqmExpression<?> applyOverClause(ParseTree functionCtx, SqmFunction<?> function) {
		final ParseTree lastChild = functionCtx.getChild( functionCtx.getChildCount() - 1 );
		if ( lastChild instanceof HqlParser.OverClauseContext ) {
			return applyOverClause( (HqlParser.OverClauseContext) lastChild, function );
		}
		return function;
	}

	private SqmExpression<?> applyOverClause(HqlParser.OverClauseContext ctx, SqmFunction<?> function) {
		final List<SqmExpression<?>> partitions;
		final List<SqmSortSpecification> orderList;
		final FrameMode mode;
		final FrameKind startKind;
		final SqmExpression<?> startExpression;
		final FrameKind endKind;
		final SqmExpression<?> endExpression;
		final FrameExclusion exclusion;
		int index = 2;
		if ( ctx.getChild( index ) instanceof HqlParser.PartitionClauseContext ) {
			final ParseTree subCtx = ctx.getChild( index );
			partitions = new ArrayList<>( ( subCtx.getChildCount() >> 1 ) - 1 );
			for ( int i = 2; i < subCtx.getChildCount(); i += 2 ) {
				partitions.add( (SqmExpression<?>) subCtx.getChild( i ).accept( this ) );
			}
			index++;
		}
		else {
			partitions = Collections.emptyList();
		}
		if ( index < ctx.getChildCount() && ctx.getChild( index ) instanceof HqlParser.OrderByClauseContext ) {
			orderList = visitOrderByClause( (HqlParser.OrderByClauseContext) ctx.getChild( index ) ).getSortSpecifications();
			index++;
		}
		else {
			orderList = Collections.emptyList();
		}
		if ( index < ctx.getChildCount() && ctx.getChild( index ) instanceof HqlParser.FrameClauseContext ) {
			final ParseTree frameCtx = ctx.getChild( index );
			switch ( ( (TerminalNode) frameCtx.getChild( 0 ) ).getSymbol().getType() ) {
				case HqlParser.RANGE:
					mode = FrameMode.RANGE;
					break;
				case HqlParser.ROWS:
					mode = FrameMode.ROWS;
					break;
				case HqlParser.GROUPS:
					mode = FrameMode.GROUPS;
					break;
				default:
					throw new IllegalArgumentException( "Unexpected frame mode: " + frameCtx.getChild( 0 ) );
			}
			final int frameStartIndex;
			if ( frameCtx.getChild( 1 ) instanceof TerminalNode ) {
				frameStartIndex = 2;
				endKind = getFrameKind( frameCtx.getChild( 4 ) );
				endExpression = endKind == FrameKind.OFFSET_FOLLOWING || endKind == FrameKind.OFFSET_PRECEDING
						? (SqmExpression<?>) frameCtx.getChild( 4 ).getChild( 0 ).accept( this )
						: null;
			}
			else {
				frameStartIndex = 1;
				endKind = FrameKind.CURRENT_ROW;
				endExpression = null;
			}
			startKind = getFrameKind( frameCtx.getChild( frameStartIndex ) );
			startExpression = startKind == FrameKind.OFFSET_FOLLOWING || startKind == FrameKind.OFFSET_PRECEDING
					? (SqmExpression<?>) frameCtx.getChild( frameStartIndex ).getChild( 0 ).accept( this )
					: null;
			final ParseTree lastChild = frameCtx.getChild( frameCtx.getChildCount() - 1 );
			if ( lastChild instanceof HqlParser.FrameExclusionContext ) {
				switch ( ( (TerminalNode) lastChild.getChild( 1 ) ).getSymbol().getType() ) {
					case HqlParser.CURRENT:
						exclusion = FrameExclusion.CURRENT_ROW;
						break;
					case HqlParser.GROUP:
						exclusion = FrameExclusion.GROUP;
						break;
					case HqlParser.TIES:
						exclusion = FrameExclusion.TIES;
						break;
					case HqlParser.NO:
						exclusion = FrameExclusion.NO_OTHERS;
						break;
					default:
						throw new IllegalArgumentException( "Unexpected frame exclusion: " + lastChild );
				}
			}
			else {
				exclusion = FrameExclusion.NO_OTHERS;
			}
		}
		else {
			mode = FrameMode.ROWS;
			startKind = FrameKind.UNBOUNDED_PRECEDING;
			startExpression = null;
			endKind = FrameKind.CURRENT_ROW;
			endExpression = null;
			exclusion = FrameExclusion.NO_OTHERS;
		}
		return new SqmOver<>(
				function,
				partitions,
				orderList,
				mode,
				startKind,
				startExpression,
				endKind,
				endExpression,
				exclusion
		);
	}

	private FrameKind getFrameKind(ParseTree child) {
		switch ( ( (TerminalNode) child.getChild( 1 ) ).getSymbol().getType() ) {
			case HqlParser.PRECEDING:
				if ( child.getChild( 0 ) instanceof TerminalNode ) {
					return FrameKind.UNBOUNDED_PRECEDING;
				}
				else {
					return FrameKind.OFFSET_PRECEDING;
				}
			case HqlParser.FOLLOWING:
				if ( child.getChild( 0 ) instanceof TerminalNode ) {
					return FrameKind.UNBOUNDED_FOLLOWING;
				}
				else {
					return FrameKind.OFFSET_FOLLOWING;
				}
			case HqlParser.ROW:
				return FrameKind.CURRENT_ROW;
			default:
				throw new IllegalArgumentException( "Illegal frame kind: " + child );
		}
	}

	@Override
	public SqmExpression<?> visitCube(HqlParser.CubeContext ctx) {
		return new SqmSummarization<>(
				SqmSummarization.Kind.CUBE,
				visitExpressions( ctx ),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmExpression<?> visitRollup(HqlParser.RollupContext ctx) {
		return new SqmSummarization<>(
				SqmSummarization.Kind.ROLLUP,
				visitExpressions( ctx ),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmExpression<?> visitSubstringFunction(HqlParser.SubstringFunctionContext ctx) {
		final SqmExpression<?> source = (SqmExpression<?>) ctx.getChild( 2 ).accept( this );
		final SqmExpression<?> start = (SqmExpression<?>) ctx.getChild( 4 ).accept( this );
		final SqmExpression<?> length;
		if ( ctx.getChildCount() == 8 ) {
			length = (SqmExpression<?>) ctx.getChild( 6 ).accept( this );
		}
		else {
			length = null;
		}

		return getFunctionDescriptor( StandardFunctions.SUBSTRING ).generateSqmExpression(
				length == null ? asList( source, start ) : asList( source, start, length ),
				null,
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmExpression<?> visitPadFunction(HqlParser.PadFunctionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw nonCompliantFunctionException( StandardFunctions.PAD );
		}
		final SqmExpression<?> source = (SqmExpression<?>) ctx.getChild( 2 ).accept( this );
		final SqmExpression<?> length = (SqmExpression<?>) ctx.getChild( 4 ).accept(this);
		final SqmTrimSpecification padSpec = visitPadSpecification( (HqlParser.PadSpecificationContext) ctx.getChild( 5 ) );
		final SqmLiteral<Character> padChar;
		if ( ctx.getChildCount() == 8 ) {
			padChar = visitPadCharacter( (HqlParser.PadCharacterContext) ctx.getChild( 6 ) );
		}
		else {
			padChar = null;
		}
		return getFunctionDescriptor( StandardFunctions.PAD ).generateSqmExpression(
				padChar != null
						? asList( source, length, padSpec, padChar )
						: asList( source, length, padSpec ),
				null,
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmTrimSpecification visitPadSpecification(HqlParser.PadSpecificationContext ctx) {
		switch ( ( (TerminalNode) ctx.getChild( 0 ) ).getSymbol().getType() ) {
			case HqlParser.LEADING:
				return new SqmTrimSpecification( TrimSpec.LEADING, creationContext.getNodeBuilder() );
			case HqlParser.TRAILING:
				return new SqmTrimSpecification( TrimSpec.TRAILING, creationContext.getNodeBuilder() );
		}
		throw new ParsingException("Unsupported pad specification [" + ctx.getText() + "]");
	}

	@Override
	public SqmLiteral<Character> visitPadCharacter(HqlParser.PadCharacterContext ctx) {
		final String padCharText = ctx.STRING_LITERAL().getText();

		if ( padCharText.length() != 3 ) {
			throw new SemanticException( "Pad character for pad() function must be single character, found '" + padCharText + "'" );
		}

		return new SqmLiteral<>(
				padCharText.charAt( 1 ),
				resolveExpressibleTypeBasic( Character.class ),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmExpression<?> visitTrimFunction(HqlParser.TrimFunctionContext ctx) {
		final SqmExpression<?> source = (SqmExpression<?>) ctx.getChild( ctx.getChildCount() - 2 ).accept( this );
		final SqmTrimSpecification trimSpec;
		final SqmLiteral<Character> trimChar;
		int index = 2;
		ParseTree parseTree = ctx.getChild( index );
		if ( parseTree instanceof HqlParser.TrimSpecificationContext ) {
			trimSpec = visitTrimSpecification( (HqlParser.TrimSpecificationContext) parseTree );
			index = 3;
		}
		else {
			trimSpec = visitTrimSpecification( null );
		}
		parseTree = ctx.getChild( index );
		if ( parseTree instanceof HqlParser.TrimCharacterContext ) {
			trimChar = visitTrimCharacter( (HqlParser.TrimCharacterContext) parseTree );
		}
		else {
			trimChar = visitTrimCharacter( null );
		}

		return getFunctionDescriptor( StandardFunctions.TRIM ).generateSqmExpression(
				asList(
						trimSpec,
						trimChar,
						source
				),
				null,
				creationContext.getQueryEngine(),
				creationContext.getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmTrimSpecification visitTrimSpecification(HqlParser.TrimSpecificationContext ctx) {
		TrimSpec spec = TrimSpec.BOTH;	// JPA says the default is BOTH

		if ( ctx != null ) {
			switch ( ( (TerminalNode) ctx.getChild( 0 ) ).getSymbol().getType() ) {
				case HqlParser.LEADING:
					spec = TrimSpec.LEADING;
					break;
				case HqlParser.TRAILING:
					spec = TrimSpec.TRAILING;
					break;
			}
		}

		return new SqmTrimSpecification( spec, creationContext.getNodeBuilder() );
	}

	@Override
	public SqmLiteral<Character> visitTrimCharacter(HqlParser.TrimCharacterContext ctx) {
		final String trimCharText = ctx != null
				? QuotingHelper.unquoteStringLiteral( ctx.getText() )
				: " "; // JPA says space is the default

		if ( trimCharText.length() != 1 ) {
			throw new SemanticException( "Trim character for trim() function must be single character, found '" + trimCharText + "'" );
		}

		return new SqmLiteral<>(
				trimCharText.charAt( 0 ),
				resolveExpressibleTypeBasic( Character.class ),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmCollectionSize visitCollectionSizeFunction(HqlParser.CollectionSizeFunctionContext ctx) {
		return new SqmCollectionSize(
				consumeDomainPath( (HqlParser.PathContext) ctx.getChild( 2 ) ),
				resolveExpressibleTypeBasic( Integer.class ),
				creationContext.getNodeBuilder()
		);
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	private boolean isIndexedPluralAttribute(SqmPath<?> path) {
		return path.getReferencedPathSource() instanceof PluralPersistentAttribute;
	}

	@Override
	public SqmPath<?> visitCollectionFunctionMisuse(HqlParser.CollectionFunctionMisuseContext ctx) {

		// Note: this is a total misuse of the elements() and indices() functions,
		//       which are supposed to be a shortcut way to write a subquery!
		//       used this way, they're just a worse way to write value()/index()

		if ( getCreationOptions().useStrictJpaCompliance() ) {
			throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.HQL_COLLECTION_FUNCTION );
		}

		final SqmPath<?> pluralAttributePath = consumeDomainPath( (HqlParser.PathContext) ctx.getChild( 2 ) );
		final SqmPathSource<?> referencedPathSource = pluralAttributePath.getReferencedPathSource();
		final TerminalNode firstNode = (TerminalNode) ctx.getChild( 0 );

		if ( !(referencedPathSource instanceof PluralPersistentAttribute<?, ?, ?> ) ) {
			throw new PathException(
					String.format(
							"Argument of '%s' is not a plural path '%s'",
							firstNode.getSymbol().getText(),
							pluralAttributePath.getNavigablePath()
					)
			);
		}

		CollectionPart.Nature nature;
		switch ( firstNode.getSymbol().getType() ) {
			case ELEMENTS:
				nature = CollectionPart.Nature.ELEMENT;
				break;
			case INDICES:
				nature = CollectionPart.Nature.INDEX;
				break;
			default:
				throw new ParsingException("Impossible symbol");
		}

		return pluralAttributePath.resolvePathPart( nature.getName(), true, this );
	}

	@Override
	public SqmElementAggregateFunction<?> visitElementAggregateFunction(HqlParser.ElementAggregateFunctionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.HQL_COLLECTION_FUNCTION );
		}

		SqmPath<?> pluralPath = consumePluralAttributeReference( ctx.path() );
		if ( !(pluralPath instanceof SqmPluralValuedSimplePath) ) {
			throw new SemanticException( "Path '" + ctx.path().getText() + "' did not resolve to a many-valued attribute" );
		}

		String functionName = ctx.getChild(0).getText().substring(0, 3);
		return new SqmElementAggregateFunction<>( pluralPath, functionName );
	}

	@Override
	public SqmIndexAggregateFunction<?> visitIndexAggregateFunction(HqlParser.IndexAggregateFunctionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.HQL_COLLECTION_FUNCTION );
		}

		final SqmPath<?> pluralPath = consumePluralAttributeReference( ctx.path() );
		if ( !(pluralPath instanceof SqmPluralValuedSimplePath) ) {
			throw new SemanticException( "Path '" + ctx.path().getText() + "' did not resolve to a many-valued attribute" );
		}
		if ( !isIndexedPluralAttribute( pluralPath ) ) {
			throw new SemanticException(
					"maxindex() function can only be applied to path expressions which resolve to an " +
							"indexed collection (list,map); specified path [" + ctx.path() +
							"] resolved to " + pluralPath.getReferencedPathSource()
			);
		}

		String functionName = ctx.getChild(0).getText().substring(0, 3);
		return new SqmIndexAggregateFunction<>( pluralPath, functionName );
	}

	@Override
	public SqmSubQuery<?> visitSubqueryExpression(HqlParser.SubqueryExpressionContext ctx) {
		return visitSubquery( (HqlParser.SubqueryContext) ctx.getChild( 1 ) );
	}

	@Override
	public SqmSubQuery<?> visitSubquery(HqlParser.SubqueryContext ctx) {
		final HqlParser.QueryExpressionContext queryExpressionContext = (HqlParser.QueryExpressionContext) ctx.getChild( 0 );
		final SqmSubQuery<?> subQuery = new SqmSubQuery<>(
				processingStateStack.getCurrent().getProcessingQuery(),
				creationContext.getNodeBuilder()
		);

		processingStateStack.push(
				new SqmQueryPartCreationProcessingStateStandardImpl(
						processingStateStack.getCurrent(),
						subQuery,
						this
				)
		);

		try {
			queryExpressionContext.accept( this );

			final List<SqmSelection<?>> selections = subQuery.getQuerySpec().getSelectClause().getSelections();
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
		final ParseTree firstChild = ctx.getChild( 0 );
		if ( firstChild instanceof HqlParser.SyntacticDomainPathContext ) {
			final SemanticPathPart syntacticNavigablePathResult = visitSyntacticDomainPath( (HqlParser.SyntacticDomainPathContext) firstChild );
			if ( ctx.getChildCount() == 2 ) {
				dotIdentifierConsumerStack.push(
						new BasicDotIdentifierConsumer( syntacticNavigablePathResult, this ) {
							@Override
							protected void reset() {
							}
						}
				);
				try {
					return (SemanticPathPart) ctx.getChild( 1 ).accept( this );
				}
				finally {
					dotIdentifierConsumerStack.pop();
				}
			}
			return syntacticNavigablePathResult;
		}
		else if ( firstChild instanceof HqlParser.GeneralPathFragmentContext ) {
			return (SemanticPathPart) firstChild.accept( this );
		}

		throw new ParsingException( "Unrecognized `path` rule branch" );
	}

	@Override
	public SemanticPathPart visitGeneralPathFragment(HqlParser.GeneralPathFragmentContext ctx) {
		return visitIndexedPathAccessFragment(
				(HqlParser.SimplePathContext) ctx.getChild( 0 ),
				ctx.getChildCount() == 1 ? null : (HqlParser.IndexedPathAccessFragmentContext) ctx.getChild( 1 )
		);
	}

	@Override
	public SemanticPathPart visitSyntacticDomainPath(HqlParser.SyntacticDomainPathContext ctx) {
		final ParseTree firstChild = ctx.getChild( 0 );
		if ( firstChild instanceof HqlParser.TreatedNavigablePathContext ) {
			return visitTreatedNavigablePath( (HqlParser.TreatedNavigablePathContext) firstChild );
		}
		else if ( firstChild instanceof HqlParser.CollectionValueNavigablePathContext ) {
			return visitCollectionValueNavigablePath( (HqlParser.CollectionValueNavigablePathContext) firstChild );
		}
		else if ( firstChild instanceof HqlParser.MapKeyNavigablePathContext ) {
			return visitMapKeyNavigablePath( (HqlParser.MapKeyNavigablePathContext) firstChild );
		}
		else if ( firstChild instanceof HqlParser.SimplePathContext && ctx.getChildCount() == 2 ) {
			return visitIndexedPathAccessFragment(
					(HqlParser.SimplePathContext) firstChild,
					(HqlParser.IndexedPathAccessFragmentContext) ctx.getChild( 1 )
			);
		}

		throw new ParsingException( "Unsure how to process `syntacticDomainPath` over : " + ctx.getText() );
	}

	private SemanticPathPart visitIndexedPathAccessFragment(
			HqlParser.SimplePathContext ctx,
			HqlParser.IndexedPathAccessFragmentContext idxCtx) {
		final SemanticPathPart pathPart = visitSimplePath( ctx );

		if ( idxCtx == null ) {
			return pathPart;
		}

		final SqmExpression<?> indexExpression = (SqmExpression<?>) idxCtx.getChild( 1 ).accept(this );
		final boolean hasIndexContinuation = idxCtx.getChildCount() == 5;
		final SqmPath<?> indexedPath = pathPart.resolveIndexedAccess( indexExpression, !hasIndexContinuation, this );

		if ( hasIndexContinuation ) {
			dotIdentifierConsumerStack.push(
					new BasicDotIdentifierConsumer( indexedPath, this ) {
						@Override
						protected void reset() {
						}
					}
			);
			try {
				return (SemanticPathPart) idxCtx.getChild( 4 ).accept( this );
			}
			finally {
				dotIdentifierConsumerStack.pop();
			}
		}
		return indexedPath;
	}

	@Override
	public SemanticPathPart visitIndexedPathAccessFragment(HqlParser.IndexedPathAccessFragmentContext idxCtx) {
		throw new UnsupportedOperationException( "Should be handled by #visitIndexedPathAccessFragment" );
	}

	@Override
	public SemanticPathPart visitSimplePath(HqlParser.SimplePathContext ctx) {
		final int numberOfContinuations = ctx.getChildCount() - 1;
		final boolean hasContinuations = numberOfContinuations != 0;

		final DotIdentifierConsumer dotIdentifierConsumer = dotIdentifierConsumerStack.getCurrent();
		final HqlParser.IdentifierContext identifierContext = (HqlParser.IdentifierContext) ctx.getChild( 0 );
		assert identifierContext.getChildCount() == 1;

		dotIdentifierConsumer.consumeIdentifier(
				visitIdentifier( identifierContext ),
				true,
				! hasContinuations
		);

		if ( hasContinuations ) {
			for ( int i = 1; i < ctx.getChildCount(); i++ ) {
				final HqlParser.SimplePathElementContext continuation = (HqlParser.SimplePathElementContext) ctx.getChild( i );
				final HqlParser.IdentifierContext identifier = (HqlParser.IdentifierContext) continuation.getChild( 1 );
				assert identifier.getChildCount() == 1;
				dotIdentifierConsumer.consumeIdentifier(
						visitIdentifier( identifier ),
						false,
						i >= numberOfContinuations
				);
			}
		}

		return dotIdentifierConsumer.getConsumedPart();
	}

	@Override
	public SqmPath<?> visitTreatedNavigablePath(HqlParser.TreatedNavigablePathContext ctx) {
		final DotIdentifierConsumer consumer = dotIdentifierConsumerStack.getCurrent();
		final boolean madeNested;
		if ( consumer instanceof QualifiedJoinPathConsumer) {
			final QualifiedJoinPathConsumer qualifiedJoinPathConsumer = (QualifiedJoinPathConsumer) consumer;
			madeNested = !qualifiedJoinPathConsumer.isNested();
			if ( madeNested ) {
				qualifiedJoinPathConsumer.setNested( true );
			}
		}
		else {
			madeNested = false;
		}
		consumeManagedTypeReference( (HqlParser.PathContext) ctx.getChild( 2 ) );

		final String treatTargetName = ctx.getChild( 4 ).getText();
		final String treatTargetEntityName = getCreationContext().getJpaMetamodel().qualifyImportableName( treatTargetName );

		final boolean hasContinuation = ctx.getChildCount() == 7;
		consumer.consumeTreat( treatTargetEntityName, !hasContinuation );
		SqmPath<?> result = (SqmPath<?>) consumer.getConsumedPart();

		if ( hasContinuation ) {
			if ( madeNested ) {
				// Reset the nested state before consuming the terminal identifier
				( (QualifiedJoinPathConsumer) consumer ).setNested( false );
			}
			final boolean addConsumer = !( consumer instanceof QualifiedJoinPathConsumer );
			if ( addConsumer ) {
				dotIdentifierConsumerStack.push(
						new BasicDotIdentifierConsumer( result, this ) {
							@Override
							protected void reset() {
							}
						}
				);
			}
			try {
				result = consumeDomainPath( (HqlParser.SimplePathContext) ctx.getChild( 6 ).getChild( 1 ) );
			}
			finally {
				if ( addConsumer ) {
					dotIdentifierConsumerStack.pop();
				}
			}
		}

		return result;
	}

	@Override
	public SqmPath<?> visitCollectionValueNavigablePath(HqlParser.CollectionValueNavigablePathContext ctx) {
		final DotIdentifierConsumer consumer = dotIdentifierConsumerStack.getCurrent();
		final boolean madeNested;
		if ( consumer instanceof QualifiedJoinPathConsumer) {
			final QualifiedJoinPathConsumer qualifiedJoinPathConsumer = (QualifiedJoinPathConsumer) consumer;
			madeNested = !qualifiedJoinPathConsumer.isNested();
			if ( madeNested ) {
				qualifiedJoinPathConsumer.setNested( true );
			}
		}
		else {
			madeNested = false;
		}
		final SqmPath<?> sqmPath = consumeDomainPath( (HqlParser.PathContext) ctx.getChild( 2 ) );
		final boolean hasContinuation = ctx.getChildCount() == 5;

		final SqmPathSource<?> referencedPathSource = sqmPath.getReferencedPathSource();
		final TerminalNode firstNode = (TerminalNode) ctx.getChild( 0 );
		checkPluralPath( sqmPath, referencedPathSource, firstNode );

		if ( getCreationOptions().useStrictJpaCompliance() ) {
			final PluralPersistentAttribute<?, ?, ?> attribute = (PluralPersistentAttribute<?, ?, ?>) referencedPathSource;
			if ( attribute.getCollectionClassification() != CollectionClassification.MAP
					&& firstNode.getSymbol().getType() == HqlParser.VALUE ) {
				throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.VALUE_FUNCTION_ON_NON_MAP );
			}
		}

		SqmPath<?> result;
		if ( consumer instanceof QualifiedJoinPathConsumer) {
			if ( madeNested && !hasContinuation ) {
				// Reset the nested state before consuming the terminal identifier
				( (QualifiedJoinPathConsumer) consumer ).setNested( false );
			}
			consumer.consumeIdentifier( CollectionPart.Nature.ELEMENT.getName(), false, !hasContinuation );
			result = (SqmPath<?>) consumer.getConsumedPart();
		}
		else {
			result = sqmPath.resolvePathPart( CollectionPart.Nature.ELEMENT.getName(), true, this );
		}

		if ( hasContinuation ) {
			if ( madeNested ) {
				// Reset the nested state before consuming the terminal identifier
				( (QualifiedJoinPathConsumer) consumer ).setNested( false );
			}
			final HqlParser.SimplePathContext identCtx = (HqlParser.SimplePathContext) ctx.getChild( 4 )
					.getChild( 1 );
			if ( consumer instanceof QualifiedJoinPathConsumer) {
				result = consumeDomainPath( identCtx );
			}
			else {
				dotIdentifierConsumerStack.push(
						new BasicDotIdentifierConsumer( result, this ) {
							@Override
							protected void reset() {
							}
						}
				);
				try {
					result = consumeDomainPath( identCtx );
				}
				finally {
					dotIdentifierConsumerStack.pop();
				}
			}
		}

		return result;
	}


	@Override
	public SqmPath<?> visitMapKeyNavigablePath(HqlParser.MapKeyNavigablePathContext ctx) {
		final DotIdentifierConsumer consumer = dotIdentifierConsumerStack.getCurrent();
		final boolean madeNested;
		if ( consumer instanceof QualifiedJoinPathConsumer) {
			final QualifiedJoinPathConsumer qualifiedJoinPathConsumer = (QualifiedJoinPathConsumer) consumer;
			madeNested = !qualifiedJoinPathConsumer.isNested();
			if ( madeNested ) {
				qualifiedJoinPathConsumer.setNested( true );
			}
		}
		else {
			madeNested = false;
		}
		final SqmPath<?> sqmPath = consumeDomainPath( (HqlParser.PathContext) ctx.getChild( 2 ) );
		final boolean hasContinuation = ctx.getChildCount() == 5;

		final SqmPathSource<?> referencedPathSource = sqmPath.getReferencedPathSource();
		final TerminalNode firstNode = (TerminalNode) ctx.getChild( 0 );
		checkPluralPath( sqmPath, referencedPathSource, firstNode );

		if ( getCreationOptions().useStrictJpaCompliance() ) {
			final PluralPersistentAttribute<?, ?, ?> attribute = (PluralPersistentAttribute<?, ?, ?>) referencedPathSource;
			if ( attribute.getCollectionClassification() != CollectionClassification.MAP
					&& firstNode.getSymbol().getType() == HqlParser.KEY ) {
				throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.KEY_FUNCTION_ON_NON_MAP );
			}
		}

		SqmPath<?> result;
		if ( sqmPath instanceof SqmMapJoin ) {
			final SqmMapJoin<?, ?, ?> sqmMapJoin = (SqmMapJoin<?, ?, ?>) sqmPath;
			if ( consumer instanceof QualifiedJoinPathConsumer) {
				if ( madeNested && !hasContinuation ) {
					// Reset the nested state before consuming the terminal identifier
					( (QualifiedJoinPathConsumer) consumer ).setNested( false );
				}
				consumer.consumeIdentifier( CollectionPart.Nature.INDEX.getName(), false, !hasContinuation );
				result = (SqmPath<?>) consumer.getConsumedPart();
			}
			else {
				result = sqmMapJoin.key();
			}
		}
		else if ( sqmPath instanceof SqmListJoin ) {
			if ( hasContinuation ) {
				throw new SemanticException("list index may not be dereferenced");
			}
			SqmListJoin<?,?> listJoin = (SqmListJoin<?,?>) sqmPath;
			result = listJoin.resolvePathPart( CollectionPart.Nature.INDEX.getName(), true, this );
		}
		else {
			assert sqmPath instanceof SqmPluralValuedSimplePath;
			final SqmPluralValuedSimplePath<?> mapPath = (SqmPluralValuedSimplePath<?>) sqmPath;
			result = mapPath.resolvePathPart( CollectionPart.Nature.INDEX.getName(), !hasContinuation, this );
		}

		if ( hasContinuation ) {
			if ( madeNested ) {
				// Reset the nested state before consuming the terminal identifier
				( (QualifiedJoinPathConsumer) consumer ).setNested( false );
			}
			final HqlParser.SimplePathContext identCtx = (HqlParser.SimplePathContext) ctx.getChild( 4 )
					.getChild( 1 );
			if ( consumer instanceof QualifiedJoinPathConsumer) {
				result = consumeDomainPath( identCtx );
			}
			else {
				dotIdentifierConsumerStack.push(
						new BasicDotIdentifierConsumer( result, this ) {
							@Override
							protected void reset() {
							}
						}
				);
				try {
					result = consumeDomainPath( identCtx );
				}
				finally {
					dotIdentifierConsumerStack.pop();
				}
			}
		}

		return result;
	}

	private void checkPluralPath(SqmPath<?> pluralAttributePath, SqmPathSource<?> referencedPathSource, TerminalNode firstNode) {
		if ( !(referencedPathSource instanceof PluralPersistentAttribute<?, ?, ?> ) ) {
			throw new PathException(
					String.format(
							"Argument of '%s' is not a plural path '%s'",
							firstNode.getSymbol().getText(),
							pluralAttributePath.getNavigablePath()
					)
			);
		}
	}

	private <X> SqmPath<X> consumeDomainPath(HqlParser.PathContext parserPath) {
		final SemanticPathPart consumedPart = (SemanticPathPart) parserPath.accept( this );
		if ( consumedPart instanceof SqmPath ) {
			//noinspection unchecked
			return (SqmPath<X>) consumedPart;
		}

		throw new SemanticException( "Expecting domain-model path, but found : " + consumedPart );
	}


	private SqmPath<?> consumeDomainPath(HqlParser.SimplePathContext sequence) {
		final SemanticPathPart consumedPart = (SemanticPathPart) sequence.accept( this );
		if ( consumedPart instanceof SqmPath ) {
			return (SqmPath<?>) consumedPart;
		}

		throw new SemanticException( "Expecting domain-model path, but found : " + consumedPart );
	}

	private SqmPath<?> consumeManagedTypeReference(HqlParser.PathContext parserPath) {
		final SqmPath<?> sqmPath = consumeDomainPath( parserPath );

		final SqmPathSource<?> pathSource = sqmPath.getReferencedPathSource();
		if ( pathSource.getSqmPathType() instanceof ManagedDomainType<?> ) {
			return sqmPath;
		}
		throw new SemanticException( "Expecting ManagedType valued path [" + sqmPath.getNavigablePath() + "], but found : " + pathSource.getSqmPathType() );
	}

	private SqmPath<?> consumePluralAttributeReference(HqlParser.PathContext parserPath) {
		final SqmPath<?> sqmPath = consumeDomainPath( parserPath );

		if ( sqmPath.getReferencedPathSource() instanceof PluralPersistentAttribute ) {
			return sqmPath;
		}

		throw new SemanticException( "Expecting plural attribute valued path [" + sqmPath.getNavigablePath() + "], but found : "
				+ sqmPath.getReferencedPathSource().getSqmPathType() );
	}

	private void checkFQNEntityNameJpaComplianceViolationIfNeeded(String name, EntityDomainType<?> entityDescriptor) {
		if ( getCreationOptions().useStrictJpaCompliance() && ! name.equals( entityDescriptor.getName() ) ) {
			// FQN is the only possible reason
			throw new StrictJpaComplianceViolation("Encountered FQN entity name [" + name + "], " +
					"but strict JPQL compliance was requested ( [" + entityDescriptor.getName() + "] should be used instead )",
					StrictJpaComplianceViolation.Type.FQN_ENTITY_NAME
			);
		}
	}

	private enum ParameterStyle {
		UNKNOWN {
			@Override
			ParameterStyle withNamed() {
				return NAMED;
			}

			@Override
			ParameterStyle withPositional() {
				return POSITIONAL;
			}
		},
		NAMED {
			@Override
			ParameterStyle withNamed() {
				return NAMED;
			}

			@Override
			ParameterStyle withPositional() {
				throw new StrictJpaComplianceViolation(
						"Cannot mix ordinal and named parameters",
						StrictJpaComplianceViolation.Type.MIXED_POSITIONAL_NAMED_PARAMETERS
				);
			}
		},
		POSITIONAL {
			@Override
			ParameterStyle withNamed() {
				throw new StrictJpaComplianceViolation(
						"Cannot mix positional and named parameters",
						StrictJpaComplianceViolation.Type.MIXED_POSITIONAL_NAMED_PARAMETERS
				);
			}

			@Override
			ParameterStyle withPositional() {
				return POSITIONAL;
			}
		},
		MIXED {
			@Override
			ParameterStyle withNamed() {
				return MIXED;
			}

			@Override
			ParameterStyle withPositional() {
				return MIXED;
			}
		};
		abstract ParameterStyle withNamed();
		abstract ParameterStyle withPositional();
	}
}
