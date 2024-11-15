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
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.dialect.function.SqlColumn;
import org.hibernate.grammars.hql.HqlLexer;
import org.hibernate.grammars.hql.HqlParser;
import org.hibernate.grammars.hql.HqlParserBaseVisitor;
import org.hibernate.internal.util.CharSequenceHelper;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.internal.AnyKeyPart;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.internal.AnyDiscriminatorSqmPath;
import org.hibernate.metamodel.model.domain.internal.EntitySqmPathSource;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;
import org.hibernate.query.NullPrecedence;
import org.hibernate.query.ParameterLabelException;
import org.hibernate.query.PathException;
import org.hibernate.query.SemanticException;
import org.hibernate.query.SortDirection;
import org.hibernate.query.SyntaxException;
import org.hibernate.query.criteria.JpaCteCriteria;
import org.hibernate.query.criteria.JpaCteCriteriaAttribute;
import org.hibernate.query.criteria.JpaCteCriteriaType;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSearchOrder;
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
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.SqmTreeCreationLogger;
import org.hibernate.query.sqm.StrictJpaComplianceViolation;
import org.hibernate.query.sqm.TemporalUnit;
import org.hibernate.query.sqm.TerminalPathException;
import org.hibernate.query.sqm.TrimSpec;
import org.hibernate.query.sqm.UnaryArithmeticOperator;
import org.hibernate.query.sqm.UnknownEntityException;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.function.NamedSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.internal.ParameterCollector;
import org.hibernate.query.sqm.internal.SqmCreationProcessingStateImpl;
import org.hibernate.query.sqm.internal.SqmDmlCreationProcessingState;
import org.hibernate.query.sqm.internal.SqmQueryPartCreationProcessingStateStandardImpl;
import org.hibernate.query.sqm.internal.TypecheckUtil;
import org.hibernate.query.sqm.produce.function.FunctionArgumentException;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.spi.ParameterDeclarationContext;
import org.hibernate.query.sqm.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.SqmQuery;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.cte.SqmCteContainer;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.AbstractSqmFrom;
import org.hibernate.query.sqm.tree.domain.SqmAnyValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmCorrelation;
import org.hibernate.query.sqm.tree.domain.SqmCteRoot;
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
import org.hibernate.query.sqm.tree.expression.AbstractSqmParameter;
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
import org.hibernate.query.sqm.tree.expression.SqmHqlNumericLiteral;
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
import org.hibernate.query.sqm.tree.from.SqmCteJoin;
import org.hibernate.query.sqm.tree.from.SqmDerivedJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.from.SqmQualifiedJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.insert.SqmConflictClause;
import org.hibernate.query.sqm.tree.insert.SqmConflictUpdateAction;
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
import org.hibernate.query.sqm.tree.predicate.SqmTruthnessPredicate;
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
import org.hibernate.query.sqm.tree.update.SqmAssignment;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.tree.cte.CteMaterialization;
import org.hibernate.sql.ast.tree.cte.CteSearchClauseKind;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.descriptor.java.spi.UnknownBasicJavaType;
import org.hibernate.type.descriptor.jdbc.ObjectJdbcType;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.Bindable;
import jakarta.persistence.metamodel.SingularAttribute;
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
import static org.hibernate.grammars.hql.HqlParser.INDICES;
import static org.hibernate.grammars.hql.HqlParser.INTERSECT;
import static org.hibernate.grammars.hql.HqlParser.ListaggFunctionContext;
import static org.hibernate.grammars.hql.HqlParser.OnOverflowClauseContext;
import static org.hibernate.grammars.hql.HqlParser.PLUS;
import static org.hibernate.grammars.hql.HqlParser.QUOTED_IDENTIFIER;
import static org.hibernate.grammars.hql.HqlParser.UNION;
import static org.hibernate.internal.util.QuotingHelper.unquoteIdentifier;
import static org.hibernate.internal.util.QuotingHelper.unquoteJavaStringLiteral;
import static org.hibernate.internal.util.QuotingHelper.unquoteStringLiteral;
import static org.hibernate.query.hql.internal.SqmTreeCreationHelper.extractJpaCompliantAlias;
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
import static org.hibernate.query.sqm.internal.SqmUtil.resolveExpressibleJavaTypeClass;
import static org.hibernate.type.descriptor.DateTimeUtils.DATE_TIME;
import static org.hibernate.type.spi.TypeConfiguration.isJdbcTemporalType;

/**
 * Responsible for producing an SQM using visitation over an HQL parse tree generated by
 * ANTLR via {@link HqlParseTreeBuilder}.
 *
 * @author Steve Ebersole
 */
public class SemanticQueryBuilder<R> extends HqlParserBaseVisitor<Object> implements SqmCreationState {

	private static final Logger log = Logger.getLogger( SemanticQueryBuilder.class );
	private static final Set<String> JPA_STANDARD_FUNCTIONS;

	private static final BasicTypeImpl<Object> OBJECT_BASIC_TYPE =
			new BasicTypeImpl<>( new UnknownBasicJavaType<>(Object.class), ObjectJdbcType.INSTANCE );

	static {
		final Set<String> jpaStandardFunctions = new HashSet<>();
		// Extracted from the BNF in JPA spec 4.14.
		jpaStandardFunctions.add( "avg" );
		jpaStandardFunctions.add( "max" );
		jpaStandardFunctions.add( "min" );
		jpaStandardFunctions.add( "sum" );
		jpaStandardFunctions.add( "count" );
		jpaStandardFunctions.add( "length" );
		jpaStandardFunctions.add( "locate" );
		jpaStandardFunctions.add( "abs" );
		jpaStandardFunctions.add( "sqrt" );
		jpaStandardFunctions.add( "mod" );
		jpaStandardFunctions.add( "size" );
		jpaStandardFunctions.add( "index" );
		jpaStandardFunctions.add( "current_date" );
		jpaStandardFunctions.add( "current_time" );
		jpaStandardFunctions.add( "current_timestamp" );
		jpaStandardFunctions.add( "concat" );
		jpaStandardFunctions.add( "substring" );
		jpaStandardFunctions.add( "trim" );
		jpaStandardFunctions.add( "lower" );
		jpaStandardFunctions.add( "upper" );
		jpaStandardFunctions.add( "coalesce" );
		jpaStandardFunctions.add( "nullif" );
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
			SqmCreationContext creationContext,
			String query) {
		return new SemanticQueryBuilder<>( expectedResultType, creationOptions, creationContext, query )
				.visitStatement( hqlParseTree );
	}

	private final Class<R> expectedResultType;
	private final String expectedResultTypeName;
	private final String expectedResultTypeShortName;
	private final String expectedResultEntity;
	private final SqmCreationOptions creationOptions;
	private final SqmCreationContext creationContext;
	private final String query;

	private final Stack<DotIdentifierConsumer> dotIdentifierConsumerStack;

	private final Stack<ParameterDeclarationContext> parameterDeclarationContextStack = new StandardStack<>( ParameterDeclarationContext.class );
	private final Stack<SqmCreationProcessingState> processingStateStack = new StandardStack<>( SqmCreationProcessingState.class );

	private final BasicDomainType<Integer> integerDomainType;
	private final JavaType<List<?>> listJavaType;
	private final JavaType<Map<?,?>> mapJavaType;

	private ParameterCollector parameterCollector;
	private ParameterStyle parameterStyle;
	private Map<Object, AbstractSqmParameter<?>> parameters;

	private boolean isExtractingJdbcTemporalType;
	// Provides access to the current CTE that is being processed, which is potentially recursive
	// This is necessary, so that the recursive query part of a CTE can access its own structure.
	// Note that the structure is based on the non-recursive query part, so there is no cycle
	private JpaCteCriteria<?> currentPotentialRecursiveCte;

	public SemanticQueryBuilder(
			Class<R> expectedResultType,
			SqmCreationOptions creationOptions,
			SqmCreationContext creationContext,
			String query) {
		this( expectedResultType,
				expectedResultType == null ? null : expectedResultType.getTypeName(),
				expectedResultType == null ? null : expectedResultType.getSimpleName(),
				null, creationOptions, creationContext, query );
	}

	public SemanticQueryBuilder(
			String expectedResultTypeName,
			String expectedResultTypeShortName,
			String expectedResultEntity,
			SqmCreationOptions creationOptions,
			SqmCreationContext creationContext,
			String query) {
		this( null,
				expectedResultTypeName,
				expectedResultTypeShortName,
				expectedResultEntity,
				creationOptions, creationContext,
				query );
	}

	public SemanticQueryBuilder(
			String expectedResultTypeName,
			String expectedResultTypeShortName,
			Class<R> expectedResultType,
			SqmCreationOptions creationOptions,
			SqmCreationContext creationContext,
			String query) {
		this( expectedResultType,
				expectedResultTypeName,
				expectedResultTypeShortName,
				null,
				creationOptions, creationContext,
				query );
	}

	private SemanticQueryBuilder(
			Class<R> expectedResultType,
			String expectedResultTypeName,
			String expectedResultTypeShortName,
			String expectedResultEntity,
			SqmCreationOptions creationOptions,
			SqmCreationContext creationContext,
			String query) {
		this.expectedResultType = expectedResultType;
		this.expectedResultTypeName = expectedResultTypeName;
		this.expectedResultTypeShortName = expectedResultTypeShortName;
		this.expectedResultEntity = expectedResultEntity;
		this.creationOptions = creationOptions;
		this.creationContext = creationContext;
		this.query = query;
		this.dotIdentifierConsumerStack = new StandardStack<>(
				DotIdentifierConsumer.class,
				new BasicDotIdentifierConsumer( this )
		);
		this.parameterStyle = creationOptions.useStrictJpaCompliance()
				? ParameterStyle.UNKNOWN
				: ParameterStyle.MIXED;

		final TypeConfiguration typeConfiguration = creationContext.getNodeBuilder().getTypeConfiguration();
		final JavaTypeRegistry javaTypeRegistry = typeConfiguration.getJavaTypeRegistry();
		this.integerDomainType = typeConfiguration.standardBasicTypeForJavaType( Integer.class );
		this.listJavaType = javaTypeRegistry.resolveDescriptor( List.class );
		this.mapJavaType = javaTypeRegistry.resolveDescriptor( Map.class );
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
		// parameters allow multivalued bindings only in very limited cases, so for
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
		//noinspection unchecked
		return new SqmRoot<>(
				(EntityDomainType<R>) visitEntityName( dmlTargetContext.entityName() ),
				extractAlias( dmlTargetContext.variable() ),
				false,
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmInsertStatement<R> visitInsertStatement(HqlParser.InsertStatementContext ctx) {
		final HqlParser.TargetEntityContext dmlTargetContext = ctx.targetEntity();
		final HqlParser.TargetFieldsContext targetFieldsSpecContext = ctx.targetFields();
		final SqmRoot<R> root = visitTargetEntity( dmlTargetContext );
		if ( root.getModel() instanceof SqmPolymorphicRootDescriptor<?> ) {
			throw new SemanticException(
					"Target type '" + root.getModel().getHibernateEntityName()
							+ "' in 'insert' statement is not an entity",
					query
			);
		}

		final HqlParser.QueryExpressionContext queryExpressionContext = ctx.queryExpression();
		if ( queryExpressionContext != null ) {
			final SqmInsertSelectStatement<R> insertStatement =
					new SqmInsertSelectStatement<>( root, creationContext.getNodeBuilder() );
			parameterCollector = insertStatement;
			final SqmDmlCreationProcessingState processingState = new SqmDmlCreationProcessingState(
					insertStatement,
					this
			);

			processingStateStack.push( processingState );

			try {
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

				queryExpressionContext.accept( this );

				insertStatement.onConflict( visitConflictClause( ctx.conflictClause() ) );
				insertStatement.validate( query );
				return insertStatement;
			}
			finally {
				processingStateStack.pop();
			}

		}
		else {
			final SqmInsertValuesStatement<R> insertStatement =
					new SqmInsertValuesStatement<>( root, creationContext.getNodeBuilder() );
			parameterCollector = insertStatement;
			final SqmDmlCreationProcessingState processingState = new SqmDmlCreationProcessingState(
					insertStatement,
					this
			);

			processingStateStack.push( processingState );
			processingState.getPathRegistry().register( root );

			try {
				for ( HqlParser.SimplePathContext stateFieldCtx : targetFieldsSpecContext.simplePath() ) {
					final SqmPath<?> stateField = (SqmPath<?>) visitSimplePath( stateFieldCtx );
					insertStatement.addInsertTargetStateField( stateField );
				}

				final ArrayList<SqmValues> valuesList = new ArrayList<>();
				final HqlParser.ValuesListContext valuesListContext = ctx.valuesList();
				for ( int i = 1; i < valuesListContext.getChildCount(); i += 2 ) {
					final ParseTree values = valuesListContext.getChild( i );
					final ArrayList<SqmExpression<?>> valuesExpressions = new ArrayList<>();
					final Iterator<SqmPath<?>> iterator = insertStatement.getInsertionTargetPaths().iterator();
					for ( int j = 1; j < values.getChildCount(); j += 2 ) {
						final SqmPath<?> targetPath = iterator.next();
						final String targetPathJavaType = targetPath.getJavaTypeName();
						final boolean isEnum = targetPath.isEnum();
						final ParseTree valuesContext = values.getChild( j );
						final HqlParser.ExpressionContext expressionContext;
						final Set<String> possibleEnumTypes;
						final SqmExpression<?> value;
						if ( isEnum && valuesContext.getChild( 0 ) instanceof HqlParser.ExpressionContext
								&& ( possibleEnumTypes = getPossibleEnumTypes( expressionContext = (HqlParser.ExpressionContext) valuesContext.getChild( 0 ) ) ) != null ) {
							value = resolveEnumShorthandLiteral(
									expressionContext,
									getPossibleEnumValue( expressionContext ),
									targetPathJavaType,
									possibleEnumTypes
							);
						}
						else {
							value = (SqmExpression<?>) valuesContext.accept( this );
						}
						valuesExpressions.add( value );
					}
					valuesList.add( new SqmValues( valuesExpressions ) );
				}

				insertStatement.values( valuesList );
				insertStatement.onConflict( visitConflictClause( ctx.conflictClause() ) );
				insertStatement.validate( query );
				return insertStatement;
			}
			finally {
				processingStateStack.pop();
			}
		}
	}

	@Override
	public SqmConflictClause<R> visitConflictClause(HqlParser.ConflictClauseContext ctx) {
		if ( ctx == null ) {
			return null;
		}
		final SqmCreationProcessingState processingState = processingStateStack.getCurrent();
		@SuppressWarnings("unchecked")
		final SqmInsertStatement<R> statement = (SqmInsertStatement<R>) processingState.getProcessingQuery();
		final SqmConflictClause<R> conflictClause = new SqmConflictClause<>( statement );
		final HqlParser.ConflictTargetContext conflictTargetContext = ctx.conflictTarget();
		if ( conflictTargetContext != null ) {
			final HqlParser.IdentifierContext identifierCtx = conflictTargetContext.identifier();
			if ( identifierCtx != null ) {
				conflictClause.conflictOnConstraint( visitIdentifier( identifierCtx ) );
			}
			else {
				final List<SqmPath<?>> constraintAttributes = new ArrayList<>();
				for ( HqlParser.SimplePathContext pathContext : conflictTargetContext.simplePath() ) {
					constraintAttributes.add( consumeDomainPath( pathContext ) );
				}
				conflictClause.conflictOnConstraintPaths( constraintAttributes );
			}
		}
		final HqlParser.ConflictActionContext conflictActionContext = ctx.conflictAction();
		final HqlParser.SetClauseContext setClauseContext = conflictActionContext.setClause();
		if ( setClauseContext != null ) {
			processingState.getPathRegistry().registerByAliasOnly( conflictClause.getExcludedRoot() );
			final SqmConflictUpdateAction<R> updateAction = conflictClause.onConflictDoUpdate();
			for ( HqlParser.AssignmentContext assignmentContext : setClauseContext.assignment() ) {
				updateAction.addAssignment( visitAssignment( assignmentContext ) );
			}
			final SqmPredicate sqmPredicate = visitWhereClause( conflictActionContext.whereClause() );
			updateAction.where( sqmPredicate );
		}
		return conflictClause;
	}

	@Override
	public SqmUpdateStatement<R> visitUpdateStatement(HqlParser.UpdateStatementContext ctx) {
		final SqmUpdateStatement<R> updateStatement = new SqmUpdateStatement<>( creationContext.getNodeBuilder() );
		parameterCollector = updateStatement;
		final SqmDmlCreationProcessingState processingState = new SqmDmlCreationProcessingState(
				updateStatement,
				this
		);
		processingStateStack.push( processingState );

		try {
			updateStatement.versioned( ctx.VERSIONED() != null );
			//noinspection unchecked
			updateStatement.setTarget( (JpaRoot<R>) visitEntityWithJoins( ctx.entityWithJoins() ) );
			final HqlParser.SetClauseContext setClauseCtx = ctx.setClause();
			for ( ParseTree subCtx : setClauseCtx.children ) {
				if ( subCtx instanceof HqlParser.AssignmentContext ) {
					updateStatement.applyAssignment( visitAssignment( (HqlParser.AssignmentContext) subCtx ) );
				}
			}

			final HqlParser.WhereClauseContext whereClauseContext = ctx.whereClause();
			if ( whereClauseContext != null ) {
				updateStatement.applyPredicate( visitWhereClause( whereClauseContext ) );
			}

			updateStatement.validate( query );
			return updateStatement;
		}
		finally {
			processingStateStack.pop();
		}
	}

	@Override
	public SqmAssignment<?> visitAssignment(HqlParser.AssignmentContext ctx) {
		//noinspection unchecked
		final SqmPath<Object> targetPath = (SqmPath<Object>) consumeDomainPath( ctx.simplePath() );
		final String targetPathJavaType = targetPath.getJavaTypeName();
		final boolean isEnum = targetPath.isEnum();
		final ParseTree rightSide = ctx.getChild( 2 );
		final HqlParser.ExpressionContext expressionContext;
		final Set<String> possibleEnumValues;
		final SqmExpression<?> value;
		if ( isEnum && rightSide.getChild( 0 ) instanceof HqlParser.ExpressionContext
				&& ( possibleEnumValues = getPossibleEnumTypes( expressionContext = (HqlParser.ExpressionContext) rightSide.getChild( 0 ) ) ) != null ) {
			value = resolveEnumShorthandLiteral(
					expressionContext,
					getPossibleEnumValue( expressionContext ),
					targetPathJavaType,
					possibleEnumValues
			);
		}
		else {
			value = (SqmExpression<?>) rightSide.accept( this );
		}
		return new SqmAssignment<>( targetPath, value );
	}

	@Override
	public SqmDeleteStatement<R> visitDeleteStatement(HqlParser.DeleteStatementContext ctx) {
		final SqmDeleteStatement<R> deleteStatement = new SqmDeleteStatement<>( creationContext.getNodeBuilder() );

		parameterCollector = deleteStatement;

		final SqmDmlCreationProcessingState sqmDeleteCreationState = new SqmDmlCreationProcessingState(
				deleteStatement,
				this
		);

		processingStateStack.push( sqmDeleteCreationState );
		try {
			//noinspection unchecked
			deleteStatement.setTarget( (JpaRoot<R>) visitEntityWithJoins( ctx.entityWithJoins() ) );
			final HqlParser.WhereClauseContext whereClauseContext = ctx.whereClause();
			if ( whereClauseContext != null ) {
				deleteStatement.applyPredicate( visitWhereClause( whereClauseContext ) );
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
	public Object visitWithClause(HqlParser.WithClauseContext ctx) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw new StrictJpaComplianceViolation(
					StrictJpaComplianceViolation.Type.CTES
			);
		}
		final List<ParseTree> children = ctx.children;
		for ( int i = 1; i < children.size(); i += 2 ) {
			visitCte( (HqlParser.CteContext) children.get( i ) );
		}
		return null;
	}

	@Override
	public Object visitCte(HqlParser.CteContext ctx) {
		final SqmCteContainer cteContainer = (SqmCteContainer) processingStateStack.getCurrent().getProcessingQuery();
		final String name = visitIdentifier( (HqlParser.IdentifierContext) ctx.children.get( 0 ) );
		final TerminalNode thirdChild = (TerminalNode) ctx.getChild( 2 );
		final int queryExpressionIndex;
		final CteMaterialization materialization;
		switch ( thirdChild.getSymbol().getType() ) {
			case HqlParser.NOT:
				materialization = CteMaterialization.NOT_MATERIALIZED;
				queryExpressionIndex = 5;
				break;
			case HqlParser.MATERIALIZED:
				materialization = CteMaterialization.MATERIALIZED;
				queryExpressionIndex = 4;
				break;
			default:
				materialization = null;
				queryExpressionIndex = 3;
				break;
		}

		final HqlParser.QueryExpressionContext queryExpressionContext = (HqlParser.QueryExpressionContext) ctx.getChild( queryExpressionIndex );
		final SqmSelectQuery<Object> cte;
		if ( cteContainer instanceof SqmSubQuery<?> ) {
			cte = new SqmSubQuery<>(
					processingStateStack.getCurrent().getProcessingQuery(),
					creationContext.getNodeBuilder()
			);
		}
		else {
			cte = new SqmSelectStatement<>( creationContext.getNodeBuilder() );
		}
		processingStateStack.push(
				new SqmQueryPartCreationProcessingStateStandardImpl(
						processingStateStack.getCurrent(),
						cte,
						this
				)
		);
		final JpaCteCriteria<?> oldCte = currentPotentialRecursiveCte;
		try {
			currentPotentialRecursiveCte = null;
			if ( queryExpressionContext instanceof HqlParser.SetQueryGroupContext ) {
				final HqlParser.SetQueryGroupContext setContext = (HqlParser.SetQueryGroupContext) queryExpressionContext;
				// A recursive query is only possible if the child count is lower than 5 e.g. `withClause? q1 op q2`
				if ( setContext.getChildCount() < 5 ) {
					final SetOperator setOperator = (SetOperator) setContext.getChild( setContext.getChildCount() - 2 )
							.accept( this );
					switch ( setOperator ) {
						case UNION:
						case UNION_ALL:
							final HqlParser.OrderedQueryContext nonRecursiveQueryContext;
							final HqlParser.OrderedQueryContext recursiveQueryContext;
							// On count == 4, we have a withClause at index 0
							if ( setContext.getChildCount() == 4 ) {
								nonRecursiveQueryContext = (HqlParser.OrderedQueryContext) setContext.getChild( 1 );
								recursiveQueryContext = (HqlParser.OrderedQueryContext) setContext.getChild( 3 );
							}
							else {
								nonRecursiveQueryContext = (HqlParser.OrderedQueryContext) setContext.getChild( 0 );
								recursiveQueryContext = (HqlParser.OrderedQueryContext) setContext.getChild( 2 );
							}
							// First visit the non-recursive part
							nonRecursiveQueryContext.accept( this );

							// Visiting the possibly recursive part must happen within the call to SqmCteContainer.with,
							// because in there, the SqmCteStatement/JpaCteCriteria is available for use in the recursive part.
							// The structure (SqmCteTable) for the SqmCteStatement is based on the non-recursive part,
							// which is necessary to have, so that the SqmCteRoot/SqmCteJoin can resolve sub-paths.
							final SqmSelectStatement<Object> recursivePart = new SqmSelectStatement<>( creationContext.getNodeBuilder() );

							processingStateStack.pop();
							processingStateStack.push(
									new SqmQueryPartCreationProcessingStateStandardImpl(
											processingStateStack.getCurrent(),
											recursivePart,
											this
									)
							);
							final JpaCteCriteria<Object> cteDefinition;
							if ( setOperator == SetOperator.UNION ) {
								cteDefinition = cteContainer.withRecursiveUnionDistinct(
										name,
										cte,
										cteCriteria -> {
											currentPotentialRecursiveCte = cteCriteria;
											recursiveQueryContext.accept( this );
											return recursivePart;
										}
								);
							}
							else {
								cteDefinition = cteContainer.withRecursiveUnionAll(
										name,
										cte,
										cteCriteria -> {
											currentPotentialRecursiveCte = cteCriteria;
											recursiveQueryContext.accept( this );
											return recursivePart;
										}
								);
							}
							if ( materialization != null ) {
								cteDefinition.setMaterialization( materialization );
							}
							final ParseTree lastChild = ctx.getChild( ctx.getChildCount() - 1 );
							final ParseTree potentialSearchClause;
							if ( lastChild instanceof HqlParser.CycleClauseContext ) {
								applyCycleClause( cteDefinition, (HqlParser.CycleClauseContext) lastChild );
								potentialSearchClause = ctx.getChild( ctx.getChildCount() - 2 );
							}
							else {
								potentialSearchClause = lastChild;
							}
							if ( potentialSearchClause instanceof HqlParser.SearchClauseContext ) {
								applySearchClause( cteDefinition, (HqlParser.SearchClauseContext) potentialSearchClause );
							}
							return null;
					}
				}
			}
			queryExpressionContext.accept( this );
			final JpaCteCriteria<Object> cteDefinition = cteContainer.with( name, cte );
			if ( materialization != null ) {
				cteDefinition.setMaterialization( materialization );
			}
		}
		finally {
			processingStateStack.pop();
			currentPotentialRecursiveCte = oldCte;
		}
		return null;
	}

	private void applyCycleClause(JpaCteCriteria<?> cteDefinition, HqlParser.CycleClauseContext ctx) {
		final HqlParser.CteAttributesContext attributesContext = ctx.cteAttributes();
		final String cycleMarkAttributeName = visitIdentifier( (HqlParser.IdentifierContext) ctx.getChild( 3 ) );
		final List<JpaCteCriteriaAttribute> cycleAttributes = new ArrayList<>( ( attributesContext.getChildCount() + 1 ) >> 1 );
		final List<ParseTree> children = attributesContext.children;
		final JpaCteCriteriaType<?> type = cteDefinition.getType();
		for ( int i = 0; i < children.size(); i += 2 ) {
			final String attributeName = visitIdentifier( (HqlParser.IdentifierContext) children.get( i ) );
			final JpaCteCriteriaAttribute attribute = type.getAttribute( attributeName );
			if ( attribute == null ) {
				throw new SemanticException(
						String.format(
								"Cycle attribute '%s' not found in the CTE %s",
								attributeName,
								cteDefinition.getName()
						),
						query
				);
			}
			cycleAttributes.add( attribute );
		}

		final String cyclePathAttributeName;
		final Object cycleValue;
		final Object noCycleValue;
		if ( ctx.getChildCount() > 4 ) {
			if ( ctx.getChildCount() > 6 ) {
				final SqmLiteral<?> cycleLiteral = (SqmLiteral<?>) visitLiteral( (HqlParser.LiteralContext) ctx.getChild( 5 ) );
				final SqmLiteral<?> noCycleLiteral = (SqmLiteral<?>) visitLiteral( (HqlParser.LiteralContext) ctx.getChild( 7 ) );
				cycleValue = cycleLiteral.getLiteralValue();
				noCycleValue = noCycleLiteral.getLiteralValue();
			}
			else {
				cycleValue = Boolean.TRUE;
				noCycleValue = Boolean.FALSE;
			}
			final ParseTree lastChild = ctx.getChild( ctx.getChildCount() - 1 );
			if ( lastChild instanceof HqlParser.IdentifierContext ) {
				cyclePathAttributeName = visitIdentifier( (HqlParser.IdentifierContext) lastChild );
			}
			else {
				cyclePathAttributeName = null;
			}
		}
		else {
			cyclePathAttributeName = null;
			cycleValue = Boolean.TRUE;
			noCycleValue = Boolean.FALSE;
		}

		cteDefinition.cycleUsing( cycleMarkAttributeName, cyclePathAttributeName, cycleValue, noCycleValue, cycleAttributes );
	}

	private void applySearchClause(JpaCteCriteria<?> cteDefinition, HqlParser.SearchClauseContext ctx) {
		final CteSearchClauseKind kind;
		if ( ( (TerminalNode) ctx.getChild( 1 ) ).getSymbol().getType() == HqlParser.BREADTH ) {
			kind = CteSearchClauseKind.BREADTH_FIRST;
		}
		else {
			kind = CteSearchClauseKind.DEPTH_FIRST;
		}
		final String searchAttributeName = visitIdentifier( ctx.identifier() );
		final HqlParser.SearchSpecificationsContext searchCtx = ctx.searchSpecifications();
		final List<JpaSearchOrder> searchOrders = new ArrayList<>( ( searchCtx.getChildCount() + 1 ) >> 1 );
		final List<ParseTree> children = searchCtx.children;
		final JpaCteCriteriaType<?> type = cteDefinition.getType();
		for ( int i = 0; i < children.size(); i += 2 ) {
			final HqlParser.SearchSpecificationContext specCtx = (HqlParser.SearchSpecificationContext) children.get( i );
			final String attributeName = visitIdentifier( specCtx.identifier() );
			final JpaCteCriteriaAttribute attribute = type.getAttribute( attributeName );
			if ( attribute == null ) {
				throw new SemanticException(
						String.format(
								"Search attribute '%s' not found in the CTE %s",
								attributeName,
								cteDefinition.getName()
						),
						query
				);
			}
			SortDirection sortOrder = SortDirection.ASCENDING;
			NullPrecedence nullPrecedence = NullPrecedence.NONE;
			int index = 1;
			if ( index < specCtx.getChildCount() ) {
				if ( specCtx.getChild( index ) instanceof HqlParser.SortDirectionContext ) {
					final HqlParser.SortDirectionContext sortCtx = specCtx.sortDirection();
					switch ( ( (TerminalNode) sortCtx.getChild( 0 ) ).getSymbol().getType() ) {
						case HqlParser.ASC:
							sortOrder = SortDirection.ASCENDING;
							break;
						case HqlParser.DESC:
							sortOrder = SortDirection.DESCENDING;
							break;
						default:
							throw new UnsupportedOperationException( "Unrecognized sort ordering: " + sortCtx.getText() );
					}
					index++;
				}
				if ( index < specCtx.getChildCount() ) {
					final HqlParser.NullsPrecedenceContext nullsPrecedenceContext = specCtx.nullsPrecedence();
					switch ( ( (TerminalNode) nullsPrecedenceContext.getChild( 1 ) ).getSymbol().getType() ) {
						case HqlParser.FIRST:
							nullPrecedence = NullPrecedence.FIRST;
							break;
						case HqlParser.LAST:
							nullPrecedence = NullPrecedence.LAST;
							break;
						default:
							throw new UnsupportedOperationException( "Unrecognized null precedence: " + nullsPrecedenceContext.getText() );
					}
				}
			}
			searchOrders.add( creationContext.getNodeBuilder().search( attribute, sortOrder, nullPrecedence ) );
		}
		cteDefinition.search( kind, searchAttributeName, searchOrders );
	}

	@Override
	public SqmQueryPart<?> visitSimpleQueryGroup(HqlParser.SimpleQueryGroupContext ctx) {
		final int lastChild = ctx.getChildCount() - 1;
		if ( lastChild != 0 ) {
			ctx.getChild( 0 ).accept( this );
		}
		return (SqmQueryPart<?>) ctx.getChild( lastChild ).accept( this );
	}

	@Override
	public SqmQueryPart<?> visitQueryOrderExpression(HqlParser.QueryOrderExpressionContext ctx) {
		final SqmQuerySpec<?> sqmQuerySpec = currentQuerySpec();
		final SqmFromClause fromClause = buildInferredFromClause(null);
		sqmQuerySpec.setFromClause( fromClause );
		sqmQuerySpec.setSelectClause( buildInferredSelectClause( fromClause ) );
		visitQueryOrder( sqmQuerySpec, ctx.queryOrder() );
		return sqmQuerySpec;
	}

	@Override
	public SqmQueryPart<?> visitQuerySpecExpression(HqlParser.QuerySpecExpressionContext ctx) {
		final SqmQueryPart<?> queryPart = visitQuery( ctx.query() );
		final HqlParser.QueryOrderContext queryOrderContext = ctx.queryOrder();
		if ( queryOrderContext != null ) {
			visitQueryOrder( queryPart, queryOrderContext );
		}
		return queryPart;
	}

	@Override
	public SqmQueryPart<?> visitNestedQueryExpression(HqlParser.NestedQueryExpressionContext ctx) {
		final SqmQueryPart<?> queryPart = (SqmQueryPart<?>) ctx.queryExpression().accept( this );
		final HqlParser.QueryOrderContext queryOrderContext = ctx.queryOrder();
		if ( queryOrderContext != null ) {
			final SqmCreationProcessingState firstProcessingState = processingStateStack.pop();
			processingStateStack.push(
					new SqmQueryPartCreationProcessingStateStandardImpl(
							processingStateStack.getCurrent(),
							firstProcessingState.getProcessingQuery(),
							this
					)
			);
			visitQueryOrder( queryPart, queryOrderContext);
		}
		return queryPart;
	}

	@Override
	public SqmQueryGroup<?> visitSetQueryGroup(HqlParser.SetQueryGroupContext ctx) {
		final List<ParseTree> children = ctx.children;
		final int firstIndex;
		if ( children.get( 0 ) instanceof HqlParser.WithClauseContext ) {
			children.get( 0 ).accept( this );
			firstIndex = 1;
		}
		else {
			firstIndex = 0;
		}
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw new StrictJpaComplianceViolation(
					StrictJpaComplianceViolation.Type.SET_OPERATIONS
			);
		}
		final SqmQueryPart<?> firstQueryPart = (SqmQueryPart<?>) children.get( firstIndex ).accept( this );
		SqmQueryGroup<?> queryGroup;
		if ( firstQueryPart instanceof SqmQueryGroup<?>) {
			queryGroup = (SqmQueryGroup<?>) firstQueryPart;
		}
		else {
			queryGroup = new SqmQueryGroup<>( firstQueryPart );
		}
		setCurrentQueryPart( queryGroup );
		final int size = children.size();
		final SqmCreationProcessingState firstProcessingState = processingStateStack.pop();
		for ( int i = firstIndex + 1; i < size; i += 2 ) {
			final SetOperator operator = visitSetOperator( (HqlParser.SetOperatorContext) children.get(i) );
			final HqlParser.OrderedQueryContext simpleQueryCtx =
					(HqlParser.OrderedQueryContext) children.get( i + 1 );
			queryGroup = getSqmQueryGroup( operator, simpleQueryCtx, queryGroup, size, firstProcessingState, i );
		}
		processingStateStack.push( firstProcessingState );

		return queryGroup;
	}

	private <X> SqmQueryGroup<X> getSqmQueryGroup(
			SetOperator operator,
			HqlParser.OrderedQueryContext simpleQueryCtx,
			SqmQueryGroup<X> queryGroup,
			int size,
			SqmCreationProcessingState firstProcessingState,
			int i) {

		final List<SqmQueryPart<X>> queryParts;
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
			queryGroup = new SqmQueryGroup<>( creationContext.getNodeBuilder(), operator, queryParts );
			setCurrentQueryPart( queryGroup );
		}

		try {
			final List<ParseTree> subChildren = simpleQueryCtx.children;
			if ( subChildren.get( 0 ) instanceof HqlParser.QueryContext ) {
				final SqmQuerySpec<X> querySpec = new SqmQuerySpec<>( creationContext.getNodeBuilder() );
				queryParts.add( querySpec );
				visitQuerySpecExpression( (HqlParser.QuerySpecExpressionContext) simpleQueryCtx );
			}
			else {
				try {
					final SqmSelectStatement<Object> selectStatement =
							new SqmSelectStatement<>( creationContext.getNodeBuilder() );
					processingStateStack.push(
							new SqmQueryPartCreationProcessingStateStandardImpl(
									processingStateStack.getCurrent(),
									selectStatement,
									this
							)
					);
					@SuppressWarnings("unchecked")
					final SqmQueryPart<X> queryPart = (SqmQueryPart<X>)
							visitNestedQueryExpression( (HqlParser.NestedQueryExpressionContext) simpleQueryCtx );
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
			default:
				throw new ParsingException( "Unrecognized set operator: " + token.getText() );
		}
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
						"A 'limit', 'offset', or 'fetch' clause requires an 'order by' clause when used in a subquery",
						query
				);
			}

			setOffsetFetchLimit(sqmQueryPart, limitClauseContext, offsetClauseContext, fetchClauseContext);
		}
	}

	@SuppressWarnings("unchecked")
	private void setOffsetFetchLimit(SqmQueryPart<?> sqmQueryPart, HqlParser.LimitClauseContext limitClauseContext, HqlParser.OffsetClauseContext offsetClauseContext, HqlParser.FetchClauseContext fetchClauseContext) {
		// these casts are all fine because the parser only accepts literals and parameters
		sqmQueryPart.setOffsetExpression( (SqmExpression<? extends Number>) visitOffsetClause(offsetClauseContext) );
		if ( limitClauseContext == null ) {
			sqmQueryPart.setFetchExpression(
					(SqmExpression<? extends Number>) visitFetchClause(fetchClauseContext),
					visitFetchClauseType(fetchClauseContext)
			);
		}
		else if ( fetchClauseContext == null ) {
			sqmQueryPart.setFetchExpression( (SqmExpression<? extends Number>) visitLimitClause(limitClauseContext) );
		}
		else {
			throw new SemanticException("The 'limit' and 'fetch' clauses may not be used together", query );
		}
	}

	@Override
	public SqmQuerySpec<?> visitQuery(HqlParser.QueryContext ctx) {
		final SqmQuerySpec<?> sqmQuerySpec = currentQuerySpec();

		// visit from clause first!!!
		final SqmFromClause fromClause =
				ctx.fromClause() == null
						? buildInferredFromClause( ctx.selectClause() )
						: visitFromClause( ctx.fromClause() );
		sqmQuerySpec.setFromClause( fromClause );

		final SqmSelectClause selectClause =
				ctx.selectClause() == null
						? buildInferredSelectClause( fromClause )
						: visitSelectClause( ctx.selectClause() );
		sqmQuerySpec.setSelectClause( selectClause );

		final SqmWhereClause whereClause = new SqmWhereClause( creationContext.getNodeBuilder() );
		if ( ctx.whereClause() != null ) {
			whereClause.setPredicate( (SqmPredicate) ctx.whereClause().accept( this ) );
		}
		sqmQuerySpec.setWhereClause( whereClause );

		if ( ctx.groupByClause() != null ) {
			sqmQuerySpec.setGroupByClauseExpressions( visitGroupByClause( ctx.groupByClause() ) );
		}
		if ( ctx.havingClause() != null ) {
			sqmQuerySpec.setHavingClausePredicate( visitHavingClause( ctx.havingClause() ) );
		}

		return sqmQuerySpec;
	}

	private SqmFromClause buildInferredFromClause(HqlParser.SelectClauseContext selectClauseContext) {
		if ( selectClauseContext != null || processingStateStack.depth() > 1  ) {
			// when there's an explicit 'select', or in a subquery, we never infer the 'from'
			return new SqmFromClause();
		}
		else {
			if ( creationOptions.useStrictJpaCompliance() ) {
				throw new StrictJpaComplianceViolation(
						"Encountered implicit 'from' clause, but strict JPQL compliance was requested",
						StrictJpaComplianceViolation.Type.IMPLICIT_FROM
				);
			}

			final SqmFromClause fromClause = new SqmFromClause();
			final EntityDomainType<R> entityDescriptor = getResultEntity();
			if ( entityDescriptor != null ) {
				final SqmRoot<R> sqmRoot =
						new SqmRoot<>( entityDescriptor, null, false, creationContext.getNodeBuilder() );
				processingStateStack.getCurrent().getPathRegistry().register( sqmRoot );
				fromClause.addRoot( sqmRoot );
			}
			return fromClause;
		}
	}

	private EntityDomainType<R> getResultEntity() {
		final JpaMetamodelImplementor jpaMetamodel = creationContext.getJpaMetamodel();
		if ( expectedResultEntity != null ) {
			final EntityDomainType<R> entityDescriptor = jpaMetamodel.entity( expectedResultEntity );
			if ( entityDescriptor == null ) {
				throw new SemanticException( "Query has no 'from' clause, and the result type '"
						+ expectedResultEntity + "' is not an entity type", query );
			}
			return entityDescriptor;
		}
		else if ( expectedResultType != null ) {
			final EntityDomainType<R> entityDescriptor = jpaMetamodel.findEntityType( expectedResultType );
			if ( entityDescriptor == null ) {
				throw new SemanticException( "Query has no 'from' clause, and the result type '"
						+ expectedResultTypeShortName + "' is not an entity type", query );
			}
			return entityDescriptor;
		}
		else {
			return null;
		}
	}

	protected SqmSelectClause buildInferredSelectClause(SqmFromClause fromClause) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw new StrictJpaComplianceViolation(
					"Encountered implicit 'select' clause, but strict JPQL compliance was requested",
					StrictJpaComplianceViolation.Type.IMPLICIT_SELECT
			);
		}

		if ( fromClause.getNumberOfRoots() == 0 ) {
			throw new SemanticException( "query has no 'select' clause, and no root entities"
					+ " (every selection query must have an explicit 'select', an explicit 'from', or an explicit entity result type)",
					query );
		}

		final NodeBuilder nodeBuilder = creationContext.getNodeBuilder();

		final SqmSelectClause selectClause;
		final boolean singleEntityResult;
		if ( expectedResultType == null ) {
			// no result type was specified
			// - if there is a single root entity return the entity,
			//   even if it has non-fetch joins (ugh!)
			// - otherwise, return all entities in an Object[] array,
			//   including non-fetch joins
			selectClause = new SqmSelectClause( false, nodeBuilder );
			singleEntityResult = fromClause.getNumberOfRoots() == 1;
		}
		else {
			singleEntityResult = creationContext.getJpaMetamodel().findEntityType( expectedResultType ) != null;
			if ( singleEntityResult ) {
				// the result type is an entity class
				if ( fromClause.getNumberOfRoots() > 1 ) {
					// multiple root entities
					throw new SemanticException( "Query has no 'select' clause, and multiple root entities, but query result type is an entity class"
							+ " (specify an explicit 'select' list, or a different result type, for example, 'Object[].class')",
							query );
				}
				else {
					final SqmRoot<?> sqmRoot = fromClause.getRoots().get(0);
					if ( sqmRoot instanceof SqmCteRoot ) {
						throw new SemanticException( "Query has no 'select' clause, and the 'from' clause refers to a CTE, but query result type is an entity class"
								+ " (specify an explicit 'select' list)",
								query );
					}
					else {
						// exactly one root entity, return it
						// (joined entities are not returned)
						selectClause = new SqmSelectClause( false, 1, nodeBuilder );
					}
				}
			}
			else {
				// the result type is not an entity class
				// return all root entities and non-fetch joins
				selectClause = new SqmSelectClause( false, nodeBuilder );
			}
		}

		fromClause.visitRoots( (sqmRoot) -> {
			selectClause.addSelection( new SqmSelection<>( sqmRoot, sqmRoot.getAlias(), nodeBuilder) );
			if ( !singleEntityResult ) {
				applyJoinsToInferredSelectClause( sqmRoot, selectClause );
			}
		} );

		return selectClause;
	}

	private void applyJoinsToInferredSelectClause(SqmFrom<?,?> sqm, SqmSelectClause selectClause) {
		sqm.visitSqmJoins( (sqmJoin) -> {
			if ( sqmJoin.isImplicitlySelectable() ) {
				selectClause.addSelection( new SqmSelection<>( sqmJoin, sqmJoin.getAlias(), creationContext.getNodeBuilder() ) );
				applyJoinsToInferredSelectClause( sqmJoin, selectClause );
			}
		} );
	}

	@Override
	public SqmSelectClause visitSelectClause(HqlParser.SelectClauseContext ctx) {
		// todo (6.0) : primer a select-clause-specific SemanticPathPart into the stack
		final SqmSelectClause selectClause =
				new SqmSelectClause(ctx.DISTINCT() != null, creationContext.getNodeBuilder() );
		final HqlParser.SelectionListContext selectionListContext = ctx.selectionList();
		for ( HqlParser.SelectionContext selectionContext : selectionListContext.selection() ) {
			selectClause.addSelection( visitSelection( selectionContext ) );
		}
		return selectClause;
	}

	@Override
	public SqmSelection<?> visitSelection(HqlParser.SelectionContext ctx) {
		final SqmSelectableNode<?> selectableNode = visitSelectableNode( ctx );
		final String resultIdentifier = extractJpaCompliantAlias( ctx.variable(), this );

		final SqmSelection<?> selection = new SqmSelection<>(
				selectableNode,
				// NOTE : SqmSelection forces the alias down to its selectableNode.
				//		- no need to do that here
				resultIdentifier,
				creationContext.getNodeBuilder()
		);

		// if the node is not a dynamic-instantiation, register it with
		// the path-registry
		if ( !(selectableNode instanceof SqmDynamicInstantiation) ) {
			processingStateStack.getCurrent().getPathRegistry().register( selection );
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
			String className = instantiationTarget.getText();
			if ( expectedResultTypeName != null && expectedResultTypeShortName.equals( className ) ) {
				className = expectedResultTypeName;
			}
			try {
				dynamicInstantiation = SqmDynamicInstantiation.forClassInstantiation(
						resolveInstantiationTargetJtd( className ),
						creationContext.getNodeBuilder()
				);
			}
			catch (ClassLoadingException e) {
				throw new SemanticException( "Could not resolve class '" + className + "' named for instantiation",
						query );
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

		if ( !dynamicInstantiation.checkInstantiation( creationContext.getTypeConfiguration() ) ) {
			final String typeName = dynamicInstantiation.getJavaType().getSimpleName();
			if ( dynamicInstantiation.isFullyAliased() ) {
				throw new SemanticException( "Missing constructor or attributes for injection into type '" + typeName + "'", query );
			}
			else {
				throw new SemanticException( "Missing constructor for type '" + typeName  + "'", query );
			}
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
		return creationContext.getServiceRegistry().requireService( ClassLoaderService.class ).classForName( className );
	}

	@Override
	public SqmDynamicInstantiationArgument<?> visitInstantiationArgument(HqlParser.InstantiationArgumentContext ctx) {
		final HqlParser.VariableContext variable = ctx.variable();
		final String alias = variable == null ? null : extractAlias( variable );

		final SqmSelectableNode<?> argExpression =
				(SqmSelectableNode<?>) ctx.instantiationArgumentExpression().accept( this );

		final SqmDynamicInstantiationArgument<?> argument = new SqmDynamicInstantiationArgument<>(
				argExpression,
				alias,
				creationContext.getNodeBuilder()
		);

		if ( !(argExpression instanceof SqmDynamicInstantiation) ) {
			processingStateStack.getCurrent().getPathRegistry().register( argument );
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
			throw new SemanticException( "Could not resolve alias '" +  alias
					+ "' in selection [" + ctx.getText() + "]",
					query );
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

	private SqmExpression<?> resolveOrderByOrGroupByExpression(
			ParseTree child,
			boolean definedCollate,
			boolean allowPositionalOrAliases) {
		final NodeBuilder nodeBuilder = creationContext.getNodeBuilder();
		final SqmCreationProcessingState processingState = processingStateStack.getCurrent();
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
				throw new SyntaxException( "'collate' is not allowed for position based 'order by' or 'group by' items" );
			}
			else if ( !allowPositionalOrAliases ) {
				// This is syntactically disallowed
				throw new SyntaxException( "Position based 'order by' is not allowed in 'over' or 'within group' clauses" );
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
				throw new SemanticException( "Numeric literal '" + position
						+ "' used in 'group by' does not match a registered select item",
						query );
			}

			return new SqmAliasedNodeRef( position, integerDomainType, nodeBuilder);
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
								nodeBuilder.getIntegerType(),
								nodeBuilder
						);
					}
					final SqmSelectableNode<?> selectableNode = sqmSelection.getSelectableNode();
					if ( selectableNode instanceof SqmFrom<?, ?> ) {
						final SqmFrom<?, ?> fromElement = (SqmFrom<?, ?>) selectableNode;
						final SqmPathSource<?> pathSource = fromElement.getReferencedPathSource();
						if ( pathSource.findSubPathSource( identifierText ) != null ) {
							if ( sqmPosition != 0 ) {
								throw new IllegalStateException(
										"Multiple from elements expose unqualified attribute: " + identifierText );
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
										"Multiple from elements expose unqualified attribute: " + identifierText );
							}
							sqmPosition = i + 1;
						}
					}
				}
				if ( found != null ) {
					return new SqmAliasedNodeRef(
							sqmPosition,
							found.get( identifierText ).getNavigablePath(),
							nodeBuilder.getIntegerType(),
							nodeBuilder
					);
				}
				else if ( sqmPosition != 0 ) {
					return new SqmAliasedNodeRef( sqmPosition,  nodeBuilder.getIntegerType(), nodeBuilder );
				}
			}
			else {
				final Integer correspondingPosition =
						allowPositionalOrAliases
								? processingState.getPathRegistry()
										.findAliasedNodePosition( identifierText )
								: null;
				if ( correspondingPosition != null ) {
					if ( definedCollate ) {
						// This is syntactically disallowed
						throw new SyntaxException( "'collate' is not allowed for alias-based 'order by' or 'group by' items" );
					}
					return new SqmAliasedNodeRef( correspondingPosition, integerDomainType, nodeBuilder );
				}

				final SqmFrom<?, ?> sqmFrom =
						processingState.getPathRegistry()
								.findFromByAlias( identifierText, true );
				if ( sqmFrom != null ) {
					if ( definedCollate ) {
						// This is syntactically disallowed
						throw new SyntaxException( "'collate' is not allowed for alias-based 'order by' or 'group by' items" );
					}
					// this will group-by all the sub-parts in the from-element's model part
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
		return resolveOrderByOrGroupByExpression( ctx.getChild( 0 ), ctx.getChildCount() > 1, true );
	}

	@Override
	public SqmPredicate visitHavingClause(HqlParser.HavingClauseContext ctx) {
		return (SqmPredicate) ctx.getChild( 1 ).accept( this );
	}

	@Override
	public SqmOrderByClause visitOrderByClause(HqlParser.OrderByClauseContext ctx) {
		return visitOrderByClause( ctx, true );
	}

	private SqmOrderByClause visitOrderByClause(HqlParser.OrderByClauseContext ctx, boolean allowPositionalOrAliases) {
		final int size = ctx.getChildCount();
		// Shift 1 bit instead of division by 2
		final int estimateExpressionsCount = ( size >> 1 ) - 1;
		final SqmOrderByClause orderByClause = new SqmOrderByClause( estimateExpressionsCount );
		for ( int i = 0; i < size; i++ ) {
			final ParseTree parseTree = ctx.getChild( i );
			if ( parseTree instanceof HqlParser.SortSpecificationContext ) {
				orderByClause.addSortSpecification( visitSortSpecification(
						(HqlParser.SortSpecificationContext) parseTree,
						allowPositionalOrAliases
				) );
			}
		}
		return orderByClause;
	}

	@Override
	public SqmSortSpecification visitSortSpecification(HqlParser.SortSpecificationContext ctx) {
		return visitSortSpecification( ctx, true );
	}

	private SqmSortSpecification visitSortSpecification(
			HqlParser.SortSpecificationContext ctx,
			boolean allowPositionalOrAliases) {
		final SqmExpression<?> sortExpression = visitSortExpression(
				ctx.sortExpression(),
				allowPositionalOrAliases
		);
		if ( sortExpression == null ) {
			throw new SemanticException( "Could not resolve sort expression: '" + ctx.sortExpression().getText() + "'",
					query );
		}
		if ( sortExpression instanceof SqmLiteral || sortExpression instanceof SqmParameter ) {
			HqlLogging.QUERY_LOGGER.debugf( "Questionable sorting by constant value : %s", sortExpression );
		}

		final SortDirection sortOrder =
				ctx.sortDirection() == null || ctx.sortDirection().DESC() == null
						? SortDirection.ASCENDING
						: SortDirection.DESCENDING;
		final NullPrecedence nullPrecedence;
		if ( ctx.nullsPrecedence() == null ) {
			nullPrecedence = NullPrecedence.NONE;
		}
		else {
			if ( ctx.nullsPrecedence().FIRST() != null ) {
				nullPrecedence = NullPrecedence.FIRST;
			}
			else if ( ctx.nullsPrecedence().LAST() != null ) {
				nullPrecedence = NullPrecedence.LAST;
			}
			else {
				throw new ParsingException( "Unrecognized null precedence" );
			}
		}

		return new SqmSortSpecification( sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public SqmExpression<?> visitSortExpression(HqlParser.SortExpressionContext ctx) {
		return visitSortExpression( ctx, true );
	}

	public SqmExpression<?> visitSortExpression(HqlParser.SortExpressionContext ctx, boolean allowPositionalOrAliases) {
		return resolveOrderByOrGroupByExpression( ctx.getChild( 0 ), ctx.getChildCount() > 1, allowPositionalOrAliases );
	}

	private SqmQuerySpec<?> currentQuerySpec() {
		final SqmQuery<?> processingQuery = processingStateStack.getCurrent().getProcessingQuery();
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

		return (SqmExpression<?>) ctx.parameterOrIntegerLiteral().accept( this );
	}

	@Override
	public SqmExpression<?> visitOffsetClause(HqlParser.OffsetClauseContext ctx) {
		if ( ctx == null ) {
			return null;
		}

		return (SqmExpression<?>) ctx.parameterOrIntegerLiteral().accept( this );
	}

	@Override
	public SqmExpression<?> visitFetchClause(HqlParser.FetchClauseContext ctx) {
		if ( ctx == null ) {
			return null;
		}

		final HqlParser.FetchCountOrPercentContext fetchCountOrPercent = ctx.fetchCountOrPercent();
		if ( fetchCountOrPercent.PERCENT() == null ) {
			return (SqmExpression<?>) fetchCountOrPercent.parameterOrIntegerLiteral().accept( this );
		}
		else {
			return (SqmExpression<?>) fetchCountOrPercent.parameterOrNumberLiteral().accept( this );
		}
	}

	private FetchClauseType visitFetchClauseType(HqlParser.FetchClauseContext ctx) {
		if ( ctx == null ) {
			return FetchClauseType.ROWS_ONLY;
		}
		else if ( ctx.fetchCountOrPercent().PERCENT() == null ) {
			return ctx.TIES() == null ? FetchClauseType.ROWS_ONLY : FetchClauseType.ROWS_WITH_TIES;
		}
		else {
			return ctx.TIES() == null ? FetchClauseType.PERCENT_ONLY : FetchClauseType.PERCENT_WITH_TIES;
		}
	}

	@Override
	public Object visitSyntacticPathExpression(HqlParser.SyntacticPathExpressionContext ctx) {
		SemanticPathPart part = visitSyntacticDomainPath( ctx.syntacticDomainPath() );
		if ( ctx.getChildCount() == 2 ) {
			dotIdentifierConsumerStack.push(
					new BasicDotIdentifierConsumer( part, this ) {
						@Override
						protected void reset() {
						}
					}
			);
			try {
				part = (SemanticPathPart) ctx.pathContinuation().simplePath().accept( this );
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
		final SemanticPathPart part = visitGeneralPathFragment( ctx.generalPathFragment() );
		if ( part instanceof DomainPathPart ) {
			return ( (DomainPathPart) part ).getSqmExpression();
		}
		else {
			return part;
		}
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
		else {
			return (SqmExpression<?>) ctx.parameter().accept( this );
		}
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
		return child instanceof TerminalNode
				? child.getText()
				: visitNakedIdentifier( (HqlParser.NakedIdentifierContext) child );
	}

	@Override
	public String visitNakedIdentifier(HqlParser.NakedIdentifierContext ctx) {
		final TerminalNode node = (TerminalNode) ctx.getChild( 0 );
		final String text = node.getText();
		return node.getSymbol().getType() == QUOTED_IDENTIFIER
				? unquoteIdentifier( text )
				: text;
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
		if ( entityReference instanceof SqmPolymorphicRootDescriptor<?>
				&& getCreationOptions().useStrictJpaCompliance() ) {
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
		final List<HqlParser.EntityWithJoinsContext> roots = parserFromClause.entityWithJoins();
		final SqmFromClause fromClause = new SqmFromClause( roots.size() );

		//have to do this here because visiting the from-elements needs it
		currentQuerySpec().setFromClause( fromClause );

		if ( creationOptions.useStrictJpaCompliance() && roots.size() > 1 ) {
			// for multiple roots, JPA says that the secondary roots should be
			// treated with semantics effectively akin to CROSS joins
			// (specifically in regard to alias scoping and join ordering).
			final SqmRoot<?> sqmRoot = visitEntityWithJoins( roots.get( 0 ) );
			fromClause.addRoot( sqmRoot );

			for ( int i = 1; i < roots.size(); i++ ) {
				final HqlParser.EntityWithJoinsContext secondaryRoot = roots.get( i );
				SqmTreeCreationHelper.handleRootAsCrossJoin( secondaryRoot, sqmRoot, this );
			}
		}
		else {
			for ( HqlParser.EntityWithJoinsContext root : roots ) {
				final SqmRoot<?> sqmRoot = visitEntityWithJoins( root );
				// Correlations are implicitly added to the from-clause
				if ( !( sqmRoot instanceof SqmCorrelation<?, ?> ) ) {
					fromClause.addRoot( sqmRoot );
				}
			}
		}

		return fromClause;
	}

	@Override
	public SqmRoot<?> visitEntityWithJoins(HqlParser.EntityWithJoinsContext parserSpace) {
		final SqmRoot<?> sqmRoot = (SqmRoot<?>) parserSpace.fromRoot().accept( this );
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
	public SqmRoot<?> visitRootEntity(HqlParser.RootEntityContext ctx) {
		final HqlParser.EntityNameContext entityNameContext = ctx.entityName();
		final String name = getEntityName( entityNameContext );

		final EntityDomainType<?> entityDescriptor = creationContext.getJpaMetamodel().getHqlEntityReference( name );

		final String alias = extractAlias( ctx.variable() );

		final SqmCreationProcessingState processingState = processingStateStack.getCurrent();
		final SqmPathRegistry pathRegistry = processingState.getPathRegistry();
		if ( entityDescriptor == null ) {
			return resolveRootEntity( entityNameContext, name, alias, processingState, pathRegistry );
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
						"Implicitly-polymorphic domain path in subquery '" + entityDescriptor.getName() + "'",
						query
				);
			}
		}

		final SqmRoot<?> sqmRoot =
				new SqmRoot<>( entityDescriptor, alias, true, creationContext.getNodeBuilder() );
		pathRegistry.register( sqmRoot );
		return sqmRoot;
	}

	private SqmRoot<?> resolveRootEntity(
			HqlParser.EntityNameContext entityNameContext,
			String name, String alias,
			SqmCreationProcessingState processingState,
			SqmPathRegistry pathRegistry) {
		final List<ParseTree> entityNameParseTreeChildren = entityNameContext.children;
		final int size = entityNameParseTreeChildren.size();
		// Handle the use of a correlation path in subqueries
		if ( processingStateStack.depth() > 1 && size > 2 ) {
			final String parentAlias = entityNameParseTreeChildren.get( 0 ).getText();
			final AbstractSqmFrom<?, ?> correlation =
					processingState.getPathRegistry().findFromByAlias( parentAlias, true );
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
				return ((SqmCorrelation<?, ?>) correlation).getCorrelatedRoot();
			}
			throw new SemanticException( "Could not resolve entity or correlation path '" + name + "'", query );
		}
		final SqmCteStatement<?> cteStatement = findCteStatement( name );
		if ( cteStatement != null ) {
			final SqmCteRoot<?> root = new SqmCteRoot<>( cteStatement, alias);
			pathRegistry.register( root );
			return root;
		}
		throw new UnknownEntityException( "Could not resolve root entity '" + name + "'", name);
	}

	@Override
	public SqmCteStatement<?> findCteStatement(String name) {
		if ( currentPotentialRecursiveCte != null && name.equals( currentPotentialRecursiveCte.getName() ) ) {
			return (SqmCteStatement<?>) currentPotentialRecursiveCte;
		}
		return processingStateStack.findCurrentFirstWithParameter( name, SemanticQueryBuilder::matchCteStatement );
	}

	private static SqmCteStatement<?> matchCteStatement(SqmCreationProcessingState state, String n) {
		if ( state.getProcessingQuery() instanceof SqmCteContainer ) {
			final SqmCteContainer container = (SqmCteContainer) state.getProcessingQuery();
			return container.getCteStatement( n );
		}
		return null;
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

		final SqmSubQuery<?> subQuery = (SqmSubQuery<?>) ctx.subquery().accept( this );
		final String alias = extractAlias( ctx.variable() );
		final SqmRoot<?> sqmRoot = new SqmDerivedRoot<>( subQuery, alias );
		processingStateStack.getCurrent().getPathRegistry().register( sqmRoot );
		return sqmRoot;
	}

	@Override
	public String visitVariable(HqlParser.VariableContext ctx) {
		return extractAlias( ctx );
	}

	protected String extractAlias(HqlParser.VariableContext ctx) {
		return SqmTreeCreationHelper.extractAlias( ctx, this );
	}

	protected String applyJpaCompliance(String text) {
		return SqmTreeCreationHelper.applyJpaCompliance( text, this );
	}

	@Override
	public final SqmCrossJoin<?> visitCrossJoin(HqlParser.CrossJoinContext ctx) {
		throw new UnsupportedOperationException( "Unexpected call to #visitCrossJoin, see #consumeCrossJoin" );
	}

	protected <T> void consumeCrossJoin(HqlParser.CrossJoinContext parserJoin, SqmRoot<T> sqmRoot) {
		final String name = getEntityName( parserJoin.entityName() );

		SqmTreeCreationLogger.LOGGER.debugf( "Handling root path - %s", name );

		final EntityDomainType<T> entityDescriptor = getCreationContext().getJpaMetamodel()
				.resolveHqlEntityReference( name );

		if ( entityDescriptor instanceof SqmPolymorphicRootDescriptor ) {
			throw new SemanticException( "Unmapped polymorphic reference cannot be used as a target of 'cross join'",
					query );
		}
		final SqmCrossJoin<T> join = new SqmCrossJoin<>(
				entityDescriptor,
				extractAlias( parserJoin.variable() ),
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
		final SqmJoinType joinType = getSqmJoinType( parserJoin.joinType() );
		final HqlParser.JoinTargetContext qualifiedJoinTargetContext = parserJoin.joinTarget();
		final String alias = extractAlias( getVariable( qualifiedJoinTargetContext ) );
		final boolean fetch = parserJoin.FETCH() != null;

		if ( fetch && processingStateStack.depth() > 1 ) {
			throw new SemanticException( "The 'from' clause of a subquery has a 'fetch'", query );
		}

		dotIdentifierConsumerStack.push( new QualifiedJoinPathConsumer( sqmRoot, joinType, fetch, alias, this ) );
		try {
			final SqmQualifiedJoin<X, ?> join = getJoin( sqmRoot, joinType, qualifiedJoinTargetContext, alias, fetch );
			final HqlParser.JoinRestrictionContext joinRestrictionContext = parserJoin.joinRestriction();
			if ( join instanceof SqmEntityJoin<?> || join instanceof SqmDerivedJoin<?> || join instanceof SqmCteJoin<?> ) {
				sqmRoot.addSqmJoin( join );
			}
			else if ( join instanceof SqmAttributeJoin<?, ?> ) {
				final SqmAttributeJoin<?, ?> attributeJoin = (SqmAttributeJoin<?, ?>) join;
				if ( getCreationOptions().useStrictJpaCompliance() ) {
					if ( join.getExplicitAlias() != null && attributeJoin.isFetched() ) {
						throw new StrictJpaComplianceViolation(
								"Encountered aliased fetch join, but strict JPQL compliance was requested",
								StrictJpaComplianceViolation.Type.ALIASED_FETCH_JOIN
						);
					}
				}
				if ( joinRestrictionContext != null && attributeJoin.isFetched() ) {
					throw new SemanticException( "Fetch join has a 'with' clause (use a filter instead)", query );
				}
			}

			if ( joinRestrictionContext != null ) {
				dotIdentifierConsumerStack.push( new QualifiedJoinPredicatePathConsumer( join, this ) );
				try {
					join.setJoinPredicate( (SqmPredicate) joinRestrictionContext.getChild( 1 ).accept( this ) );
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

	private static HqlParser.VariableContext getVariable(HqlParser.JoinTargetContext joinTargetContext) {
		if ( joinTargetContext instanceof HqlParser.JoinPathContext ) {
			return ((HqlParser.JoinPathContext) joinTargetContext).variable();
		}
		else if ( joinTargetContext instanceof HqlParser.JoinSubqueryContext ) {
			return ((HqlParser.JoinSubqueryContext) joinTargetContext).variable();
		}
		else {
			throw new ParsingException( "unexpected join type" );
		}
	}

	@SuppressWarnings("unchecked")
	private <X> SqmQualifiedJoin<X, ?> getJoin(
			SqmRoot<X> sqmRoot,
			SqmJoinType joinType,
			HqlParser.JoinTargetContext joinTargetContext,
			String alias,
			boolean fetch) {
		if ( joinTargetContext instanceof HqlParser.JoinPathContext ) {
			final HqlParser.JoinPathContext joinPathContext = (HqlParser.JoinPathContext) joinTargetContext;
			return (SqmQualifiedJoin<X, ?>) joinPathContext.path().accept( this );
		}
		else if ( joinTargetContext instanceof HqlParser.JoinSubqueryContext ) {
			if ( fetch ) {
				throw new SemanticException( "The 'from' clause of a subquery has a 'fetch' join", query );
			}
			if ( getCreationOptions().useStrictJpaCompliance() ) {
				throw new StrictJpaComplianceViolation(
						"The JPA specification does not support subqueries in the from clause. " +
								"Please disable the JPA query compliance if you want to use this feature.",
						StrictJpaComplianceViolation.Type.FROM_SUBQUERY
				);
			}

			final HqlParser.JoinSubqueryContext joinSubqueryContext = (HqlParser.JoinSubqueryContext) joinTargetContext;
			final boolean lateral = joinSubqueryContext.LATERAL() != null;
			final DotIdentifierConsumer identifierConsumer = dotIdentifierConsumerStack.pop();
			final SqmSubQuery<X> subQuery = (SqmSubQuery<X>) joinSubqueryContext.subquery().accept( this );
			dotIdentifierConsumerStack.push( identifierConsumer );
			final SqmQualifiedJoin<X, ?> join = new SqmDerivedJoin<>( subQuery, alias, joinType, lateral, sqmRoot );
			processingStateStack.getCurrent().getPathRegistry().register( join );
			return join;
		}
		else {
			throw new ParsingException( "unexpected join type" );
		}
	}

	private static SqmJoinType getSqmJoinType(HqlParser.JoinTypeContext joinTypeContext) {
		if ( joinTypeContext == null || joinTypeContext.getChildCount() == 0 ) {
			return SqmJoinType.INNER;
		}
		else {
			final TerminalNode firstChild = (TerminalNode) joinTypeContext.getChild(0);
			switch ( firstChild.getSymbol().getType() ) {
				case HqlParser.FULL:
					return SqmJoinType.FULL;
				case HqlParser.RIGHT:
					return SqmJoinType.RIGHT;
				// For some reason, we also support `outer join` syntax..
				case HqlParser.OUTER:
				case HqlParser.LEFT:
					return SqmJoinType.LEFT;
				default:
					return SqmJoinType.INNER;
			}
		}
	}

	@Override
	public SqmJoin<?, ?> visitJpaCollectionJoin(HqlParser.JpaCollectionJoinContext ctx) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Deprecated syntax dating back to EJB-QL prior to EJB 3, required by JPA, never documented in Hibernate
	 */
	protected void consumeJpaCollectionJoin(HqlParser.JpaCollectionJoinContext ctx, SqmRoot<?> sqmRoot) {
		final String alias = extractAlias( ctx.variable() );
		dotIdentifierConsumerStack.push(
				// According to JPA spec 4.4.6 this is an inner join
				new QualifiedJoinPathConsumer( sqmRoot, SqmJoinType.INNER, false, alias, this )
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
		else {
			return (SqmPredicate) ctx.predicate().accept( this );
		}

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
		return junction(
				Predicate.BooleanOperator.AND,
				(SqmPredicate) ctx.predicate( 0 ).accept( this ),
				(SqmPredicate) ctx.predicate( 1 ).accept( this )
		);
	}

	@Override
	public SqmPredicate visitOrPredicate(HqlParser.OrPredicateContext ctx) {
		return junction(
				Predicate.BooleanOperator.OR,
				(SqmPredicate) ctx.predicate( 0 ).accept( this ),
				(SqmPredicate) ctx.predicate( 1 ).accept( this )
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
				(SqmExpression<?>) ctx.expression( 0 ).accept( this ),
				(SqmExpression<?>) ctx.expression( 1 ).accept( this ),
				(SqmExpression<?>) ctx.expression( 2 ).accept( this ),
				ctx.NOT() != null,
				creationContext.getNodeBuilder()
		);
	}


	@Override
	public SqmNullnessPredicate visitIsNullPredicate(HqlParser.IsNullPredicateContext ctx) {
		return new SqmNullnessPredicate(
				(SqmExpression<?>) ctx.expression().accept( this ),
				ctx.NOT() != null,
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmEmptinessPredicate visitIsEmptyPredicate(HqlParser.IsEmptyPredicateContext ctx) {
		SqmExpression<?> expression = (SqmExpression<?>) ctx.expression().accept(this);
		if ( expression instanceof SqmPluralValuedSimplePath ) {
			return new SqmEmptinessPredicate(
					(SqmPluralValuedSimplePath<?>) expression,
					ctx.NOT() != null,
					creationContext.getNodeBuilder()
			);
		}
		else {
			throw new SemanticException( "Operand of 'is empty' operator must be a plural path", query );
		}
	}

	@Override
	public Object visitIsTruePredicate(HqlParser.IsTruePredicateContext ctx) {
		return new SqmTruthnessPredicate(
				(SqmExpression<?>) ctx.expression().accept( this ),
				true,
				ctx.NOT() != null,
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public Object visitIsFalsePredicate(HqlParser.IsFalsePredicateContext ctx) {
		return new SqmTruthnessPredicate(
				(SqmExpression<?>) ctx.expression().accept( this ),
				false,
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
			default:
				throw new ParsingException("Unrecognized comparison operator");
		}
	}

	@Override
	public SqmPredicate visitComparisonPredicate(HqlParser.ComparisonPredicateContext ctx) {
		final ComparisonOperator comparisonOperator = (ComparisonOperator) ctx.comparisonOperator().accept( this );
		final HqlParser.ExpressionContext leftExpressionContext = ctx.expression( 0 );
		final HqlParser.ExpressionContext rightExpressionContext = ctx.expression( 1 );
		return createComparisonPredicate( comparisonOperator, leftExpressionContext, rightExpressionContext );
	}

	@Override
	public SqmPredicate visitIsDistinctFromPredicate(HqlParser.IsDistinctFromPredicateContext ctx) {
		final HqlParser.ExpressionContext leftExpressionContext = ctx.expression( 0 );
		final HqlParser.ExpressionContext rightExpressionContext = ctx.expression( 1 );
		final ComparisonOperator comparisonOperator = ctx.NOT() == null
				? ComparisonOperator.DISTINCT_FROM
				: ComparisonOperator.NOT_DISTINCT_FROM;
		return createComparisonPredicate( comparisonOperator, leftExpressionContext, rightExpressionContext );
	}

	private SqmComparisonPredicate createComparisonPredicate(
			ComparisonOperator comparisonOperator,
			HqlParser.ExpressionContext leftExpressionContext,
			HqlParser.ExpressionContext rightExpressionContext) {
		final SqmExpression<?> right;
		final SqmExpression<?> left;
		Set<String> possibleEnumTypes;
		if ( ( possibleEnumTypes = getPossibleEnumTypes( leftExpressionContext ) ) != null ) {
			right = (SqmExpression<?>) rightExpressionContext.accept( this );
			left = resolveEnumShorthandLiteral(
					leftExpressionContext,
					getPossibleEnumValue( leftExpressionContext ),
					right.getJavaTypeName(),
					possibleEnumTypes
			);
		}
		else if ( ( possibleEnumTypes = getPossibleEnumTypes( rightExpressionContext ) ) != null ) {
			left = (SqmExpression<?>) leftExpressionContext.accept( this );
			right = resolveEnumShorthandLiteral(
					rightExpressionContext,
					getPossibleEnumValue( rightExpressionContext ),
					left.getJavaTypeName(),
					possibleEnumTypes
			);
		}
		else {
			final SqmExpression<?> l = (SqmExpression<?>) leftExpressionContext.accept( this );
			final SqmExpression<?> r = (SqmExpression<?>) rightExpressionContext.accept( this );
			if ( l instanceof AnyDiscriminatorSqmPath && r instanceof SqmLiteralEntityType ) {
				left = l;
				right = createDiscriminatorValue( (AnyDiscriminatorSqmPath<?>) left, rightExpressionContext );
			}
			else if ( r instanceof AnyDiscriminatorSqmPath && l instanceof SqmLiteralEntityType ) {
				left = createDiscriminatorValue( (AnyDiscriminatorSqmPath<?>) r, leftExpressionContext );
				right = r;
			}
			else {
				left = l;
				right = r;
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
			AnyDiscriminatorSqmPath<T> anyDiscriminatorTypeSqmPath,
			HqlParser.ExpressionContext valueExpressionContext) {
		return new SqmAnyDiscriminatorValue<>(
				anyDiscriminatorTypeSqmPath.getNodeType().getPathName(),
				creationContext.getJpaMetamodel().resolveHqlEntityReference( valueExpressionContext.getText() ),
				anyDiscriminatorTypeSqmPath.getExpressible().getSqmPathType(),
				creationContext.getNodeBuilder()
		);
	}

	private SqmExpression<?> resolveEnumShorthandLiteral(
			HqlParser.ExpressionContext expressionContext,
			String enumValue, String enumType, Set<String> enumTypes) {
		if ( enumValue != null && enumType != null && enumTypes.contains(enumType) ) {
			DotIdentifierConsumer dotIdentifierConsumer = dotIdentifierConsumerStack.getCurrent();
			dotIdentifierConsumer.consumeIdentifier( enumType, true, false );
			dotIdentifierConsumer.consumeIdentifier( enumValue, false, true );
			return (SqmExpression<?>) dotIdentifierConsumerStack.getCurrent().getConsumedPart();
		}
		else {
			return (SqmExpression<?>) expressionContext.accept( this );
		}
	}

	private Set<String> getPossibleEnumTypes(HqlParser.ExpressionContext expressionContext) {
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
					return creationContext.getJpaMetamodel().getEnumTypesForValue( ctx.getText() );
				}
			}
		}

		return null;
	}

	private String getPossibleEnumValue(HqlParser.ExpressionContext expressionContext) {
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
					HqlParser.SimplePathContext simplePathContext = (HqlParser.SimplePathContext) ctx;
					int size = simplePathContext.simplePathElement().size();
					if ( size==0 ) {
						return simplePathContext.getText();
					}
					else {
						return simplePathContext.simplePathElement(size -1)
								.identifier().getText();
					}
				}
			}
		}

		return null;
	}

	@Override
	public SqmPredicate visitContainsPredicate(HqlParser.ContainsPredicateContext ctx) {
		final boolean negated = ctx.NOT() != null;
		final SqmExpression<?> lhs = (SqmExpression<?>) ctx.expression( 0 ).accept( this );
		final SqmExpression<?> rhs = (SqmExpression<?>) ctx.expression( 1 ).accept( this );
		final SqmExpressible<?> lhsExpressible = lhs.getExpressible();
		if ( lhsExpressible != null && !( lhsExpressible.getSqmType() instanceof BasicPluralType<?, ?>) ) {
			throw new SemanticException(
					"First operand for contains predicate must be a basic plural type expression, but found: " + lhsExpressible.getSqmType(),
					query
			);
		}
		final SelfRenderingSqmFunction<Boolean> contains = getFunctionDescriptor( "array_contains" ).generateSqmExpression(
				asList( lhs, rhs ),
				null,
				creationContext.getQueryEngine()
		);
		return new SqmBooleanExpressionPredicate( contains, negated, creationContext.getNodeBuilder() );
	}

	@Override
	public SqmPredicate visitIncludesPredicate(HqlParser.IncludesPredicateContext ctx) {
		final boolean negated = ctx.NOT() != null;
		final SqmExpression<?> lhs = (SqmExpression<?>) ctx.expression( 0 ).accept( this );
		final SqmExpression<?> rhs = (SqmExpression<?>) ctx.expression( 1 ).accept( this );
		final SqmExpressible<?> lhsExpressible = lhs.getExpressible();
		final SqmExpressible<?> rhsExpressible = rhs.getExpressible();
		if ( lhsExpressible != null && !( lhsExpressible.getSqmType() instanceof BasicPluralType<?, ?>) ) {
			throw new SemanticException(
					"First operand for includes predicate must be a basic plural type expression, but found: " + lhsExpressible.getSqmType(),
					query
			);
		}
		if ( rhsExpressible != null && !( rhsExpressible.getSqmType() instanceof BasicPluralType<?, ?>) ) {
			throw new SemanticException(
					"Second operand for includes predicate must be a basic plural type expression, but found: " + rhsExpressible.getSqmType(),
					query
			);
		}
		final SelfRenderingSqmFunction<Boolean> contains = getFunctionDescriptor( "array_includes" ).generateSqmExpression(
				asList( lhs, rhs ),
				null,
				creationContext.getQueryEngine()
		);
		return new SqmBooleanExpressionPredicate( contains, negated, creationContext.getNodeBuilder() );
	}

	@Override
	public SqmPredicate visitIntersectsPredicate(HqlParser.IntersectsPredicateContext ctx) {
		final boolean negated = ctx.NOT() != null;
		final SqmExpression<?> lhs = (SqmExpression<?>) ctx.expression( 0 ).accept( this );
		final SqmExpression<?> rhs = (SqmExpression<?>) ctx.expression( 1 ).accept( this );
		final SqmExpressible<?> lhsExpressible = lhs.getExpressible();
		if ( lhsExpressible != null && !( lhsExpressible.getSqmType() instanceof BasicPluralType<?, ?>) ) {
			throw new SemanticException(
					"First operand for intersects predicate must be a basic plural type expression, but found: " + lhsExpressible.getSqmType(),
					query
			);
		}
		final SelfRenderingSqmFunction<Boolean> contains = getFunctionDescriptor( "array_intersects" ).generateSqmExpression(
				asList( lhs, rhs ),
				null,
				creationContext.getQueryEngine()
		);
		return new SqmBooleanExpressionPredicate( contains, negated, creationContext.getNodeBuilder() );
	}

	@Override
	public SqmPredicate visitLikePredicate(HqlParser.LikePredicateContext ctx) {
		final boolean negated = ctx.NOT() != null;
		final boolean caseSensitive = ctx.LIKE() != null;
		if ( ctx.likeEscape() == null ) {
			return new SqmLikePredicate(
					(SqmExpression<?>) ctx.expression(0).accept( this ),
					(SqmExpression<?>) ctx.expression(1).accept( this ),
					negated,
					caseSensitive,
					creationContext.getNodeBuilder()
			);
		}
		else {
			return new SqmLikePredicate(
					(SqmExpression<?>) ctx.expression(0).accept( this ),
					(SqmExpression<?>) ctx.expression(1).accept( this ),
					(SqmExpression<?>) ctx.likeEscape().accept( this ),
					negated,
					caseSensitive,
					creationContext.getNodeBuilder()
			);
		}
	}

	@Override
	public Object visitLikeEscape(HqlParser.LikeEscapeContext ctx) {
		final HqlParser.ParameterContext parameter = ctx.parameter();
		if ( parameter instanceof HqlParser.NamedParameterContext ) {
			return visitNamedParameter(
					(HqlParser.NamedParameterContext) parameter,
					creationContext.getNodeBuilder().getCharacterType()
			);
		}
		else if ( parameter instanceof HqlParser.PositionalParameterContext ) {
			return visitPositionalParameter(
					(HqlParser.PositionalParameterContext) parameter,
					creationContext.getNodeBuilder().getCharacterType()
			);
		}
		else {
			final TerminalNode terminalNode = (TerminalNode) ctx.getChild( 1 );
			final String escape = unquoteStringLiteral( terminalNode.getText() );
			if ( escape.length() != 1 ) {
				throw new SemanticException(
						"Escape character literals must have exactly a single character, but found: " + escape,
						query
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
		final SqmPath<?> sqmPluralPath = consumeDomainPath( ctx.path() );
		if ( sqmPluralPath instanceof SqmPluralValuedSimplePath ) {
			return new SqmMemberOfPredicate(
					(SqmExpression<?>) ctx.expression().accept( this ),
					(SqmPluralValuedSimplePath<?>) sqmPluralPath,
					negated,
					creationContext.getNodeBuilder()
			);
		}
		else {
			throw new SemanticException( "Operand of 'member of' operator must be a plural path", query );
		}
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public SqmPredicate visitInPredicate(HqlParser.InPredicateContext ctx) {
		final boolean negated = ctx.getChildCount() == 4;
		final SqmExpression<?> testExpression = (SqmExpression<?>) ctx.expression().accept( this );
		final HqlParser.InListContext inListContext = ctx.inList();
		if ( inListContext instanceof HqlParser.ExplicitTupleInListContext ) {
			final HqlParser.ExplicitTupleInListContext tupleExpressionListContext = (HqlParser.ExplicitTupleInListContext) inListContext;
			final int size = tupleExpressionListContext.getChildCount();
			final int estimatedSize = size >> 1;
			final String testExpressionJavaType = testExpression.getJavaTypeName();
			final boolean isEnum = testExpression.isEnum();
			// Multivalued bindings are only allowed if there is a single list item, hence size 3 (LP, RP and param)
			parameterDeclarationContextStack.push( () -> size == 3 );
			try {
				final List<SqmExpression<?>> listExpressions = new ArrayList<>( estimatedSize );
				for ( int i = 1; i < size; i++ ) {
					final ParseTree parseTree = tupleExpressionListContext.getChild( i );
					if ( parseTree instanceof HqlParser.ExpressionOrPredicateContext ) {
						final ParseTree child = parseTree.getChild( 0 );
						final HqlParser.ExpressionContext expressionContext;
						final Set<String> possibleEnumTypes;
						if ( isEnum && child instanceof HqlParser.ExpressionContext
								&& ( possibleEnumTypes = getPossibleEnumTypes( expressionContext = (HqlParser.ExpressionContext) child ) ) != null ) {
							listExpressions.add(
									resolveEnumShorthandLiteral(
											expressionContext,
											getPossibleEnumValue( expressionContext ),
											testExpressionJavaType,
											possibleEnumTypes
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
						singletonList( tupleExpressionListContext.parameter().accept( this ) ),
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
			final SqmSubQuery<?> subquery = visitSubquery( subQueryOrParamInListContext.subquery() );
			return new SqmInSubQueryPredicate(
					testExpression,
					subquery,
					negated,
					creationContext.getNodeBuilder()
			);
		}
		else if ( inListContext instanceof HqlParser.PersistentCollectionReferenceInListContext ) {
			if ( getCreationOptions().useStrictJpaCompliance() ) {
				throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.HQL_COLLECTION_FUNCTION );
			}
			final HqlParser.PersistentCollectionReferenceInListContext collectionReferenceInListContext =
					(HqlParser.PersistentCollectionReferenceInListContext) inListContext;
			return new SqmInSubQueryPredicate<>(
					testExpression,
					createCollectionReferenceSubQuery(
							collectionReferenceInListContext.simplePath(),
							(TerminalNode) collectionReferenceInListContext.collectionQuantifier().getChild(0).getChild(0)
					),
					negated,
					creationContext.getNodeBuilder()
			);
		}
		else if ( inListContext instanceof HqlParser.ArrayInListContext ) {
			if ( getCreationOptions().useStrictJpaCompliance() ) {
				throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.HQL_COLLECTION_FUNCTION );
			}
			final HqlParser.ArrayInListContext arrayInListContext =
					(HqlParser.ArrayInListContext) inListContext;

			final SqmExpression<?> arrayExpr = (SqmExpression<?>) arrayInListContext.expression().accept( this );
			final SqmExpressible<?> arrayExpressible = arrayExpr.getExpressible();
			if ( arrayExpressible != null ) {
				if ( !(arrayExpressible.getSqmType() instanceof BasicPluralType<?, ?>) ) {
					throw new SemanticException(
							"Right operand for in-array predicate must be a basic plural type expression, but found: " + arrayExpressible.getSqmType(),
							query
					);
				}
				testExpression.applyInferableType( ( (BasicPluralType<?, ?>) arrayExpressible.getSqmType() ).getElementType() );
			}
			final SelfRenderingSqmFunction<Boolean> contains = getFunctionDescriptor( "array_contains" ).generateSqmExpression(
					asList( arrayExpr, testExpression ),
					null,
					creationContext.getQueryEngine()
			);
			return new SqmBooleanExpressionPredicate( contains, negated, creationContext.getNodeBuilder() );
		}
		else {
			throw new ParsingException( "Unexpected IN predicate type [" + ctx.getClass().getSimpleName() + "] : " + ctx.getText() );
		}
	}

	@Override
	public SqmPredicate visitExistsCollectionPartPredicate(HqlParser.ExistsCollectionPartPredicateContext ctx) {
		final SqmSubQuery<Object> subQuery = createCollectionReferenceSubQuery(
				ctx.simplePath(),
				null
		);
		return new SqmExistsPredicate( subQuery, creationContext.getNodeBuilder() );
	}

	@Override
	public SqmPredicate visitExistsPredicate(HqlParser.ExistsPredicateContext ctx) {
		final SqmExpression<?> expression = (SqmExpression<?>) ctx.expression().accept( this );
		return new SqmExistsPredicate( expression, creationContext.getNodeBuilder() );
	}

	@Override
	public SqmPredicate visitBooleanExpressionPredicate(HqlParser.BooleanExpressionPredicateContext ctx) {
		final SqmExpression<?> expression = (SqmExpression<?>) ctx.expression().accept( this );
		if ( resolveExpressibleJavaTypeClass( expression ) != Boolean.class ) {
			throw new SemanticException(
					"Non-boolean expression used in predicate context: " + ctx.getText(),
					query
			);
		}
		@SuppressWarnings("unchecked")
		final SqmExpression<Boolean> booleanExpression = (SqmExpression<Boolean>) expression;
		return new SqmBooleanExpressionPredicate( booleanExpression, creationContext.getNodeBuilder() );
	}

	@Override
	public Object visitEntityTypeExpression(HqlParser.EntityTypeExpressionContext ctx) {
		final HqlParser.EntityTypeReferenceContext entityTypeReferenceContext = ctx.entityTypeReference();
		// can be one of 2 forms:
		//		1) TYPE( some.path )
		//		2) TYPE( :someParam )
		final HqlParser.ParameterContext parameter = entityTypeReferenceContext.parameter();
		final HqlParser.PathContext path = entityTypeReferenceContext.path();
		if ( parameter != null ) {
			// we have form (2)
			return new SqmParameterizedEntityType<>(
					(SqmParameter<?>) parameter.accept( this ),
					creationContext.getNodeBuilder()
			);
		}
		else if ( path != null ) {
			// we have form (1)
			final SqmPath<?> sqmPath = (SqmPath<?>) path.accept( this );
			return sqmPath.type();
		}
		else {
			throw new ParsingException( "Could not interpret grammar context as entity type expression: " + ctx.getText() );
		}
	}

	@Override
	public SqmExpression<?> visitEntityIdExpression(HqlParser.EntityIdExpressionContext ctx) {
		return visitEntityIdReference( ctx.entityIdReference() );
	}

	@Override
	public SqmPath<?> visitEntityIdReference(HqlParser.EntityIdReferenceContext ctx) {
		if ( ctx.pathContinuation() != null ) {
			throw new UnsupportedOperationException( "Path continuation from 'id()' reference not yet implemented" );
		}

		final SqmPath<?> sqmPath = consumeDomainPath( ctx.path() );
		final DomainType<?> sqmPathType = sqmPath.getReferencedPathSource().getSqmPathType();
		if ( sqmPathType instanceof IdentifiableDomainType<?> ) {
			final IdentifiableDomainType<?> identifiableType = (IdentifiableDomainType<?>) sqmPathType;
			final SqmPathSource<?> identifierDescriptor = identifiableType.getIdentifierDescriptor();
			if ( identifierDescriptor == null ) {
				// mainly for benefit of Hibernate Processor
				throw new FunctionArgumentException( "Argument '" + sqmPath.getNavigablePath()
						+ "' of 'id()' is a '" + identifiableType.getTypeName()
						+ "' and does not have a well-defined '@Id' attribute" );
			}
			return sqmPath.get( identifierDescriptor.getPathName() );
		}
		else if ( sqmPath instanceof SqmAnyValuedSimplePath<?> ) {
			return sqmPath.resolvePathPart( AnyKeyPart.KEY_NAME, true, processingStateStack.getCurrent().getCreationState() );
		}
		else {
			throw new FunctionArgumentException( "Argument '" + sqmPath.getNavigablePath()
					+ "' of 'id()' does not resolve to an entity type" );
		}
	}

	@Override
	public SqmExpression<?> visitEntityVersionExpression(HqlParser.EntityVersionExpressionContext ctx) {
		return visitEntityVersionReference( ctx.entityVersionReference() );
	}

	@Override
	public SqmPath<?> visitEntityVersionReference(HqlParser.EntityVersionReferenceContext ctx) {
		final SqmPath<?> sqmPath = consumeDomainPath( ctx.path() );
		final DomainType<?> sqmPathType = sqmPath.getReferencedPathSource().getSqmPathType();
		if ( sqmPathType instanceof IdentifiableDomainType<?> ) {
			final IdentifiableDomainType<?> identifiableType = (IdentifiableDomainType<?>) sqmPathType;
			if ( !identifiableType.hasVersionAttribute() ) {
				throw new FunctionArgumentException( "Argument '" + sqmPath.getNavigablePath()
						+ "' of 'version()' is a '" + identifiableType.getTypeName()
						+ "' and does not have a '@Version' attribute" );
			}
			@SuppressWarnings("unchecked")
			final SingularPersistentAttribute<Object, ?> versionAttribute =
					(SingularPersistentAttribute<Object, ?>) identifiableType.findVersionAttribute();
			return sqmPath.get( versionAttribute );
		}
		else {
			throw new FunctionArgumentException( "Argument '" + sqmPath.getNavigablePath()
					+ "' of 'version()' does not resolve to an entity type" );
		}
	}

	@Override
	public SqmPath<?> visitEntityNaturalIdExpression(HqlParser.EntityNaturalIdExpressionContext ctx) {
		return visitEntityNaturalIdReference( ctx.entityNaturalIdReference() );
	}

	@Override
	public SqmPath<?> visitEntityNaturalIdReference(HqlParser.EntityNaturalIdReferenceContext ctx) {
		if ( ctx.pathContinuation() != null ) {
			throw new UnsupportedOperationException( "Path continuation from 'naturalid()' reference not yet implemented" );
		}

		final SqmPath<?> sqmPath = consumeDomainPath( ctx.path() );
		final DomainType<?> sqmPathType = sqmPath.getReferencedPathSource().getSqmPathType();

		if ( sqmPathType instanceof IdentifiableDomainType<?> ) {
			@SuppressWarnings("unchecked")
			final IdentifiableDomainType<Object> identifiableType = (IdentifiableDomainType<Object>) sqmPathType;
			final List<? extends PersistentAttribute<Object, ?>> attributes = identifiableType.findNaturalIdAttributes();
			if ( attributes == null ) {
				throw new FunctionArgumentException( "Argument '" + sqmPath.getNavigablePath()
						+ "' of 'naturalid()' is a '" + identifiableType.getTypeName()
						+ "' and does not have a natural id"
				);
			}
			else if ( attributes.size() >1 ) {
				throw new FunctionArgumentException( "Argument '" + sqmPath.getNavigablePath()
						+ "' of 'naturalid()' is a '" + identifiableType.getTypeName()
						+ "' and has a composite natural id"
				);
			}

			@SuppressWarnings("unchecked")
			final SingularAttribute<Object, ?> naturalIdAttribute
					= (SingularAttribute<Object, ?>) attributes.get(0);
			return sqmPath.get( naturalIdAttribute );
		}

		throw new FunctionArgumentException( "Argument '" + sqmPath.getNavigablePath()
				+ "' of 'naturalid()' does not resolve to an entity type" );
	}
//
//	@Override
//	public Object visitToOneFkExpression(HqlParser.ToOneFkExpressionContext ctx) {
//		return visitToOneFkReference( (HqlParser.ToOneFkReferenceContext) ctx.getChild( 0 ) );
//	}

	@Override
	public SqmFkExpression<?> visitToOneFkReference(HqlParser.ToOneFkReferenceContext ctx) {
		final SqmPath<?> sqmPath = consumeDomainPath( (HqlParser.PathContext) ctx.getChild( 2 ) );
		final SqmPathSource<?> toOneReference = sqmPath.getReferencedPathSource();

		final boolean validToOneRef = toOneReference.getBindableType() == Bindable.BindableType.SINGULAR_ATTRIBUTE
				&& toOneReference instanceof EntitySqmPathSource;

		if ( !validToOneRef ) {
			throw new FunctionArgumentException(
					String.format(
							Locale.ROOT,
							"Argument '%s' of 'fk()' function is not a single-valued association",
							sqmPath.getNavigablePath()
					)
			);

		}

		return new SqmFkExpression<>( (SqmEntityValuedSimplePath<?>) sqmPath );
	}

	@Override
	public SqmMapEntryReference<?, ?> visitMapEntrySelection(HqlParser.MapEntrySelectionContext ctx) {
		return new SqmMapEntryReference<>(
				consumePluralAttributeReference( ctx.path() ),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmExpression<?> visitConcatenationExpression(HqlParser.ConcatenationExpressionContext ctx) {
		if ( ctx.getChildCount() != 3 ) {
			throw new SyntaxException( "Expecting two operands to the '||' operator" );
		}
		final SqmExpression<?> lhs = (SqmExpression<?>) ctx.expression( 0 ).accept( this );
		final SqmExpression<?> rhs = (SqmExpression<?>) ctx.expression( 1 ).accept( this );
		final SqmExpressible<?> lhsExpressible = lhs.getExpressible();
		final SqmExpressible<?> rhsExpressible = rhs.getExpressible();
		if ( lhsExpressible != null && lhsExpressible.getSqmType() instanceof BasicPluralType<?, ?> ) {
			if ( rhsExpressible == null || rhsExpressible.getSqmType() instanceof BasicPluralType<?, ?> ) {
				// Both sides are array, so use array_concat
				return getFunctionDescriptor( "array_concat" ).generateSqmExpression(
						asList( lhs, rhs ),
						null,
						creationContext.getQueryEngine()
				);
			}
			else {
				// The RHS seems to be of the element type, so use array_append
				return getFunctionDescriptor( "array_append" ).generateSqmExpression(
						asList( lhs, rhs ),
						null,
						creationContext.getQueryEngine()
				);
			}
		}
		else if ( rhsExpressible != null && rhsExpressible.getSqmType() instanceof BasicPluralType<?, ?> ) {
			if ( lhsExpressible == null ) {
				// The RHS is an array and the LHS doesn't have a clear type, so use array_concat
				return getFunctionDescriptor( "array_concat" ).generateSqmExpression(
						asList( lhs, rhs ),
						null,
						creationContext.getQueryEngine()
				);
			}
			else {
				// The LHS seems to be of the element type, so use array_prepend
				return getFunctionDescriptor( "array_prepend" ).generateSqmExpression(
						asList( lhs, rhs ),
						null,
						creationContext.getQueryEngine()
				);
			}
		}
		else {
			return getFunctionDescriptor( "concat" ).generateSqmExpression(
					asList( lhs, rhs ),
					null,
					creationContext.getQueryEngine()
			);
		}
	}

	@Override
	public Object visitSignOperator(HqlParser.SignOperatorContext ctx) {
		switch ( ( (TerminalNode) ctx.getChild( 0 ) ).getSymbol().getType() ) {
			case HqlParser.PLUS:
				return UnaryArithmeticOperator.UNARY_PLUS;
			case HqlParser.MINUS:
				return UnaryArithmeticOperator.UNARY_MINUS;
			default:
				throw new ParsingException("Unrecognized sign operator");
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
				throw new ParsingException("Unrecognized additive operator");
		}
	}

	@Override
	public Object visitMultiplicativeOperator(HqlParser.MultiplicativeOperatorContext ctx) {
		switch ( ( (TerminalNode) ctx.getChild( 0 ) ).getSymbol().getType() ) {
			case HqlParser.ASTERISK:
				return BinaryArithmeticOperator.MULTIPLY;
			case HqlParser.SLASH:
				return this.creationOptions.isPortableIntegerDivisionEnabled()
						? BinaryArithmeticOperator.DIVIDE_PORTABLE
						: BinaryArithmeticOperator.DIVIDE;
			case HqlParser.PERCENT_OP:
				return BinaryArithmeticOperator.MODULO;
			default:
				throw new ParsingException("Unrecognized multiplicative operator");
		}
	}

	@Override
	public Object visitAdditionExpression(HqlParser.AdditionExpressionContext ctx) {
		if ( ctx.getChildCount() != 3 ) {
			throw new SyntaxException( "Expecting two operands to the additive operator" );
		}

		final SqmExpression<?> left = (SqmExpression<?>) ctx.expression(0).accept(this);
		final SqmExpression<?> right = (SqmExpression<?>) ctx.expression(1).accept(this);
		final BinaryArithmeticOperator operator = (BinaryArithmeticOperator) ctx.additiveOperator().accept(this);
		TypecheckUtil.assertOperable( left, right, operator );

		return new SqmBinaryArithmetic<>(
				operator,
				left,
				right,
				creationContext.getJpaMetamodel(),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public Object visitMultiplicationExpression(HqlParser.MultiplicationExpressionContext ctx) {
		if ( ctx.getChildCount() != 3 ) {
			throw new SyntaxException( "Expecting two operands to the multiplicative operator" );
		}

		final SqmExpression<?> left = (SqmExpression<?>) ctx.expression(0).accept( this );
		final SqmExpression<?> right = (SqmExpression<?>) ctx.expression(1).accept( this );
		final BinaryArithmeticOperator operator = (BinaryArithmeticOperator) ctx.multiplicativeOperator().accept( this );
		TypecheckUtil.assertOperable( left, right, operator );

		if ( operator == BinaryArithmeticOperator.MODULO ) {
			return getFunctionDescriptor("mod").generateSqmExpression(
					asList( left, right ),
					null,
					creationContext.getQueryEngine()
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
				(SqmExpression<?>) ctx.expression().accept( this ),
				toDurationUnit( (SqmExtractUnit<?>) ctx.datetimeField().accept( this ) ),
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
		SqmExpression<?> expression = (SqmExpression<?>) ctx.expression().accept( this );
		TypecheckUtil.assertDuration( expression );
		return new SqmByUnit(
				toDurationUnit( (SqmExtractUnit<?>) ctx.datetimeField().accept( this ) ),
				expression,
				resolveExpressibleTypeBasic( Long.class ),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmUnaryOperation<?> visitUnaryExpression(HqlParser.UnaryExpressionContext ctx) {
		final SqmExpression<?> expression = (SqmExpression<?>) ctx.expression().accept(this);
		final UnaryArithmeticOperator operator = (UnaryArithmeticOperator) ctx.signOperator().accept(this);
		TypecheckUtil.assertNumeric( expression, operator );
		return new SqmUnaryOperation<>( operator, expression );
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

		final SqmExpression<?> expressionToCollate = (SqmExpression<?>) ctx.expression().accept( this );
		final SqmCollation castTargetExpression = (SqmCollation) ctx.collation().accept( this );

		return getFunctionDescriptor("collate").generateSqmExpression(
				asList( expressionToCollate, castTargetExpression ),
				null, //why not string?
				creationContext.getQueryEngine()
		);
	}

	@Override
	public Object visitCollation(HqlParser.CollationContext ctx) {
		final StringBuilder collation = new StringBuilder();
		final HqlParser.SimplePathContext simplePathContext = ctx.simplePath();
		final boolean quoted = simplePathContext.getStart().getType() == QUOTED_IDENTIFIER;
		if ( quoted ) {
			collation.append("\"");
		}
		collation.append( visitIdentifier( simplePathContext.identifier() ) );
		for ( HqlParser.SimplePathElementContext pathElementContext
				: simplePathContext.simplePathElement() ) {
			collation.append( visitIdentifier( pathElementContext.identifier() ) );
		}
		if ( quoted ) {
			collation.append("\"");
		}
		return new SqmCollation( collation.toString(), null,
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

	@Override @SuppressWarnings({"rawtypes", "unchecked"})
	public SqmCaseSimple<?, ?> visitSimpleCaseList(HqlParser.SimpleCaseListContext ctx) {
		final int size = ctx.simpleCaseWhen().size();
		final SqmExpression<?> expression = (SqmExpression<?>) ctx.expressionOrPredicate().accept(this);
		final SqmCaseSimple caseExpression = new SqmCaseSimple<>( expression, size, creationContext.getNodeBuilder() );

		for ( int i = 0; i < size; i++ ) {
			final HqlParser.SimpleCaseWhenContext simpleCaseWhenContext = ctx.simpleCaseWhen( i );
			final HqlParser.ExpressionContext testExpression = simpleCaseWhenContext.expression();
			final SqmExpression<?> test;
			final Set<String> possibleEnumTypes;
			if ( ( possibleEnumTypes = getPossibleEnumTypes( testExpression ) ) != null ) {
				test = resolveEnumShorthandLiteral(
						testExpression,
						getPossibleEnumValue( testExpression ),
						expression.getJavaTypeName(),
						possibleEnumTypes
				);
			}
			else {
				test = (SqmExpression<?>) testExpression.accept( this );
			}
			final SqmExpression<?> result =
					(SqmExpression<?>) simpleCaseWhenContext.expressionOrPredicate().accept(this);
			caseExpression.when( test, result );
		}

		final HqlParser.CaseOtherwiseContext caseOtherwiseContext = ctx.caseOtherwise();
		if ( caseOtherwiseContext != null ) {
			caseExpression.otherwise( (SqmExpression<?>) caseOtherwiseContext.expressionOrPredicate().accept( this ) );
		}

		return caseExpression;
	}

	@Override @SuppressWarnings({"rawtypes", "unchecked"})
	public SqmCaseSearched<?> visitSearchedCaseList(HqlParser.SearchedCaseListContext ctx) {
		final int size = ctx.searchedCaseWhen().size();
		final SqmCaseSearched caseExpression = new SqmCaseSearched<>( size, creationContext.getNodeBuilder() );

		for ( int i = 0; i < size; i++ ) {
			final HqlParser.SearchedCaseWhenContext searchedCaseWhenContext = ctx.searchedCaseWhen( i );
			caseExpression.when(
					(SqmPredicate) searchedCaseWhenContext.predicate().accept( this ),
					(SqmExpression<?>) searchedCaseWhenContext.expressionOrPredicate().accept( this )
			);
		}

		final HqlParser.CaseOtherwiseContext caseOtherwiseContext = ctx.caseOtherwise();
		if ( caseOtherwiseContext != null ) {
			caseExpression.otherwise( (SqmExpression<?>) caseOtherwiseContext.expressionOrPredicate().accept( this ) );
		}

		return caseExpression;
	}

	@Override
	public SqmExpression<?> visitCurrentDateFunction(HqlParser.CurrentDateFunctionContext ctx) {
		return getFunctionDescriptor("current_date")
				.generateSqmExpression(
						resolveExpressibleTypeBasic( Date.class ),
						creationContext.getQueryEngine()
				);
	}

	@Override
	public SqmExpression<?> visitCurrentTimeFunction(HqlParser.CurrentTimeFunctionContext ctx) {
		return getFunctionDescriptor("current_time")
				.generateSqmExpression(
						resolveExpressibleTypeBasic( Time.class ),
						creationContext.getQueryEngine()
				);
	}

	@Override
	public SqmExpression<?> visitCurrentTimestampFunction(HqlParser.CurrentTimestampFunctionContext ctx) {
		return getFunctionDescriptor("current_timestamp")
				.generateSqmExpression(
						resolveExpressibleTypeBasic( Timestamp.class ),
						creationContext.getQueryEngine()
				);
	}

	@Override
	public SqmExpression<?> visitInstantFunction(HqlParser.InstantFunctionContext ctx) {
		return getFunctionDescriptor("instant")
				.generateSqmExpression(
						resolveExpressibleTypeBasic( Instant.class ),
						creationContext.getQueryEngine()
				);
	}

	@Override
	public SqmExpression<?> visitLocalDateFunction(HqlParser.LocalDateFunctionContext ctx) {
		return getFunctionDescriptor("local_date")
				.generateSqmExpression(
						resolveExpressibleTypeBasic( LocalDate.class ),
						creationContext.getQueryEngine()
				);
	}

	@Override
	public SqmExpression<?> visitLocalTimeFunction(HqlParser.LocalTimeFunctionContext ctx) {
		return getFunctionDescriptor("local_time")
				.generateSqmExpression(
						resolveExpressibleTypeBasic( LocalTime.class ),
						creationContext.getQueryEngine()
				);
	}

	@Override
	public SqmExpression<?> visitLocalDateTimeFunction(HqlParser.LocalDateTimeFunctionContext ctx) {
		return getFunctionDescriptor("local_datetime")
				.generateSqmExpression(
						resolveExpressibleTypeBasic( LocalDateTime.class ),
						creationContext.getQueryEngine()
				);
	}

	@Override
	public SqmExpression<?> visitOffsetDateTimeFunction(HqlParser.OffsetDateTimeFunctionContext ctx) {
		return getFunctionDescriptor("offset_datetime")
				.generateSqmExpression(
						resolveExpressibleTypeBasic( OffsetDateTime.class ),
						creationContext.getQueryEngine()
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
				return integerLiteral( text );
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
	public Object visitArrayLiteral(HqlParser.ArrayLiteralContext ctx) {
		final List<HqlParser.ExpressionContext> expressionContexts = ctx.expression();
		int count = expressionContexts.size();
		final List<SqmTypedNode<?>> arguments = new ArrayList<>( count );
		for ( HqlParser.ExpressionContext expressionContext : expressionContexts ) {
			arguments.add( (SqmTypedNode<?>) expressionContext.accept( this ) );
		}
		return getFunctionDescriptor( "array" ).generateSqmExpression(
				arguments,
				null,
				creationContext.getQueryEngine()
		);
	}

	@Override
	public Object visitGeneralizedLiteral(HqlParser.GeneralizedLiteralContext ctx) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SqmExpression<?> visitTerminal(TerminalNode node) {
		if ( node.getSymbol().getType() == HqlLexer.EOF ) {
			return null;
		}
		switch ( node.getSymbol().getType() ) {
			case HqlParser.STRING_LITERAL:
				return stringLiteral( node.getText() );
			case HqlParser.JAVA_STRING_LITERAL:
				return javaStringLiteral( node.getText() );
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
				return new SqmLiteralNull<>( creationContext.getNodeBuilder() );
			case HqlParser.BINARY_LITERAL:
				return binaryLiteral( node.getText() );
			default:
				throw new ParsingException("Unexpected terminal node [" + node.getText() + "]");
		}
	}

	@Override
	public Object visitDateTimeLiteral(HqlParser.DateTimeLiteralContext ctx) {
		return ctx.getChild( 0 ).accept( this );
	}

	@Override
	public Object visitLocalDateTimeLiteral(HqlParser.LocalDateTimeLiteralContext ctx) {
		return ctx.localDateTime().accept( this );
	}

	@Override
	public Object visitZonedDateTimeLiteral(HqlParser.ZonedDateTimeLiteralContext ctx) {
		return ctx.zonedDateTime().accept( this );
	}

	@Override
	public Object visitOffsetDateTimeLiteral(HqlParser.OffsetDateTimeLiteralContext ctx) {
		if ( ctx.offsetDateTime() != null ) {
			return ctx.offsetDateTime().accept(this);
		}
		else if ( ctx.offsetDateTimeWithMinutes() != null ) {
			return ctx.offsetDateTimeWithMinutes().accept(this);
		}
		else {
			return null;
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
		final HqlParser.DateTimeContext dateTime = ctx.dateTime();
		if ( dateTime != null ) {
			return dateTime.accept( this );
		}
		else {
			return sqlTimestampLiteralFrom( ctx.genericTemporalLiteralText().getText() );
		}
	}

	@Override
	public Object visitJdbcDateLiteral(HqlParser.JdbcDateLiteralContext ctx) {
		final HqlParser.DateContext date = ctx.date();
		if ( date != null ) {
			return date.accept( this );
		}
		else {
			return sqlDateLiteralFrom( ctx.genericTemporalLiteralText().getText() );
		}
	}

	@Override
	public Object visitJdbcTimeLiteral(HqlParser.JdbcTimeLiteralContext ctx) {
		final HqlParser.TimeContext time = ctx.time();
		if ( time != null ) {
			return time.accept( this );
		}
		else {
			return sqlTimeLiteralFrom( ctx.genericTemporalLiteralText().getText() );
		}
	}

	@Override
	public Object visitDateTime(HqlParser.DateTimeContext ctx) {
		return ctx.getChild( 0 ).accept( this );
	}

	@Override
	public Object visitLocalDateTime(HqlParser.LocalDateTimeContext ctx) {
		return localDateTimeLiteralFrom( ctx.date(), ctx.time() );
	}

	@Override
	public Object visitOffsetDateTime(HqlParser.OffsetDateTimeContext ctx) {
		return offsetDatetimeLiteralFrom( ctx.date(), ctx.time(), ctx.offset() );
	}

	@Override
	public Object visitOffsetDateTimeWithMinutes(HqlParser.OffsetDateTimeWithMinutesContext ctx) {
		return offsetDatetimeLiteralFrom( ctx.date(), ctx.time(), ctx.offsetWithMinutes() );
	}

	@Override
	public Object visitZonedDateTime(HqlParser.ZonedDateTimeContext ctx) {
		return zonedDateTimeLiteralFrom( ctx.date(), ctx.time(), ctx.zoneId() );
	}

	private SqmLiteral<?> localDateTimeLiteralFrom(
			HqlParser.DateContext date,
			HqlParser.TimeContext time) {
		return new SqmLiteral<>(
				LocalDateTime.of( localDate( date ), localTime( time ) ),
				resolveExpressibleTypeBasic( LocalDateTime.class ),
				creationContext.getNodeBuilder()
		);
	}

	private SqmLiteral<?> zonedDateTimeLiteralFrom(
			HqlParser.DateContext date,
			HqlParser.TimeContext time,
			HqlParser.ZoneIdContext timezone) {
		return new SqmLiteral<>(
				ZonedDateTime.of( localDate( date ), localTime( time ), visitZoneId( timezone ) ),
				resolveExpressibleTypeBasic( ZonedDateTime.class ),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public ZoneId visitZoneId(HqlParser.ZoneIdContext ctx) {
		final TerminalNode firstChild = (TerminalNode) ctx.getChild( 0 );
		final String timezoneText;
		if ( firstChild.getSymbol().getType() == HqlParser.STRING_LITERAL ) {
			timezoneText = unquoteStringLiteral( ctx.getText() );
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

	private SqmLiteral<?> offsetDatetimeLiteralFrom(
			HqlParser.DateContext date,
			HqlParser.TimeContext time,
			HqlParser.OffsetWithMinutesContext offset) {
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
		final int hour = Integer.parseInt( ctx.hour().getText() );
		final int minute = Integer.parseInt( ctx.minute().getText() );
		final HqlParser.SecondContext secondContext = ctx.second();
		if ( secondContext != null ) {
			final String secondText = secondContext.getText();
			final int index = secondText.indexOf( '.' );
			if ( index < 0 ) {
				return LocalTime.of(
						hour,
						minute,
						Integer.parseInt( secondText )
				);
			}
			else {
				final String secondFractions = secondText.substring( index + 1 );
				return LocalTime.of(
						hour,
						minute,
						Integer.parseInt( secondText.substring( 0, index ) ),
						Integer.parseInt( secondFractions ) * (int) Math.pow( 10, 9 - secondFractions.length() )
				);
			}
		}
		else {
			return LocalTime.of( hour, minute );
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
		final int factor = ( (TerminalNode) offset.getChild( 0 ) ).getSymbol().getType() == PLUS ? 1 : -1;
		final int hour = factor * Integer.parseInt( offset.hour().getText() );
		if ( offset.getChildCount() == 2 ) {
			return ZoneOffset.ofHours( hour );
		}
		return ZoneOffset.ofHoursMinutes(
				hour,
				factor * Integer.parseInt( offset.minute().getText() )
		);
	}

	private static ZoneOffset zoneOffset(HqlParser.OffsetWithMinutesContext offset) {
		final int factor = ( (TerminalNode) offset.getChild( 0 ) ).getSymbol().getType() == PLUS ? 1 : -1;
		final int hour = factor * Integer.parseInt( offset.hour().getText() );
		if ( offset.getChildCount() == 2 ) {
			return ZoneOffset.ofHours( hour );
		}
		return ZoneOffset.ofHoursMinutes(
				hour,
				factor * Integer.parseInt( offset.minute().getText() )
		);
	}

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
				unquoteStringLiteral( text ),
				resolveExpressibleTypeBasic( String.class ),
				creationContext.getNodeBuilder()
		);
	}

	private SqmLiteral<String> javaStringLiteral(String text) {
		String unquoted = unquoteJavaStringLiteral( text );
		return new SqmLiteral<>(
				unquoted,
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

	private SqmHqlNumericLiteral<Integer> integerLiteral(String text) {
		text = text.replace( "_", "" );

		// special handling for octal and hexadecimal literals
		if ( isHexOrOctal( text ) ) {
			final int intValue = Integer.decode( text );
			text = Integer.toString( intValue );
		}

		return new SqmHqlNumericLiteral<>(
				text,
				integerDomainType,
				creationContext.getNodeBuilder()
		);
	}

	@SuppressWarnings("RedundantIfStatement")
	private boolean isHexOrOctal(String text) {
		if ( text.startsWith( "0x" ) || text.startsWith( "-0x" ) ) {
			return true;
		}

		if ( ( text.startsWith( "0" ) && text.length() > 1 )
				|| ( text.startsWith( "-0" ) && text.length() > 2 ) ) {
			return true;
		}

		return false;
	}

	private SqmHqlNumericLiteral<Long> longLiteral(String text) {
		assert text.endsWith( "l" ) || text.endsWith( "L" );
		return new SqmHqlNumericLiteral<>(
				text.substring( 0, text.length() - 1 ).replace( "_", "" ),
				resolveExpressibleTypeBasic( Long.class ),
				creationContext.getNodeBuilder()
		);
	}

	private SqmHqlNumericLiteral<BigInteger> bigIntegerLiteral(String text) {
		assert text.endsWith( "bi" ) || text.endsWith( "BI" );
		return new SqmHqlNumericLiteral<>(
				text.substring( 0, text.length() - 2 ).replace( "_", "" ),
				resolveExpressibleTypeBasic( BigInteger.class ),
				creationContext.getNodeBuilder()
		);
	}

	private SqmHqlNumericLiteral<? extends Number> floatLiteral(String text) {
		assert text.endsWith( "f" ) || text.endsWith( "F" );
		return new SqmHqlNumericLiteral<>(
				text.substring( 0, text.length() - 1 ).replace( "_", "" ),
				resolveExpressibleTypeBasic( Float.class ),
				creationContext.getNodeBuilder()
		);
	}

	private SqmHqlNumericLiteral<Double> doubleLiteral(String text) {
		if ( text.endsWith( "d" ) || text.endsWith( "D" ) ) {
			text = text.substring( 0, text.length() - 1 );
		}
		return new SqmHqlNumericLiteral<>(
				text.replace( "_", "" ),
				resolveExpressibleTypeBasic( Double.class ),
				creationContext.getNodeBuilder()
		);
	}

	private SqmHqlNumericLiteral<BigDecimal> bigDecimalLiteral(String text) {
		assert text.endsWith( "bd" ) || text.endsWith( "BD" );
		return new SqmHqlNumericLiteral<>(
				text.substring( 0, text.length() - 2 ).replace( "_", "" ),
				resolveExpressibleTypeBasic( BigDecimal.class ),
				creationContext.getNodeBuilder()
		);
	}

	private SqmHqlNumericLiteral<? extends Number> hexLiteral(String text) {
		if ( text.endsWith( "l" ) || text.endsWith( "L" ) ) {
			return longLiteral( text );
		}
		else {
			return integerLiteral( text );
		}
	}

	private <J> BasicType<J> resolveExpressibleTypeBasic(Class<J> javaType) {
		return creationContext.getTypeConfiguration().standardBasicTypeForJavaType( javaType );
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
		return resolveParameter(
				new SqmNamedParameter<>(
						ctx.getChild( 1 ).getText(),
						parameterDeclarationContextStack.getCurrent().isMultiValuedBindingAllowed(),
						expressibleType,
						creationContext.getNodeBuilder()
				)
		);
	}

	@Override
	public SqmPositionalParameter<?> visitPositionalParameter(HqlParser.PositionalParameterContext ctx) {
		return visitPositionalParameter( ctx, null );
	}

	private <T> SqmPositionalParameter<T> visitPositionalParameter(
			HqlParser.PositionalParameterContext ctx,
			SqmExpressible<T> expressibleType) {
		if ( ctx.getChildCount() == 1 ) {
			throw new ParameterLabelException( "Unlabeled ordinal parameter ('?' rather than ?1)" );
		}
		parameterStyle = parameterStyle.withPositional();
		return resolveParameter(
				new SqmPositionalParameter<>(
						Integer.parseInt( ctx.getChild( 1 ).getText() ),
						parameterDeclarationContextStack.getCurrent().isMultiValuedBindingAllowed(),
						expressibleType,
						creationContext.getNodeBuilder()
				)
		);
	}

	private <T extends AbstractSqmParameter<?>> T resolveParameter(T parameter) {
		if ( parameters == null ) {
			parameters = new HashMap<>();
		}
		final Object key = parameter.getName() == null ? parameter.getPosition() : parameter.getName();
		final AbstractSqmParameter<?> existingParameter = parameters.putIfAbsent( key, parameter );
		if ( existingParameter == null ) {
			parameterCollector.addParameter( parameter );
			return parameter;
		}
		else if ( existingParameter.allowMultiValuedBinding() && !parameter.allowMultiValuedBinding() ) {
			existingParameter.disallowMultiValuedBinding();
		}
		//noinspection unchecked
		return (T) existingParameter;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Functions

	private String toName(HqlParser.JpaNonstandardFunctionNameContext ctx) {
		return ctx.STRING_LITERAL() == null
				? ctx.identifier().getText().toLowerCase()
				: unquoteStringLiteral( ctx.STRING_LITERAL().getText() ).toLowerCase();
	}

	@Override
	public SqmExpression<?> visitJpaNonstandardFunction(HqlParser.JpaNonstandardFunctionContext ctx) {
		final String functionName = toName( ctx.jpaNonstandardFunctionName() );
		final HqlParser.GenericFunctionArgumentsContext argumentsContext = ctx.genericFunctionArguments();
		@SuppressWarnings("unchecked")
		final List<SqmTypedNode<?>> functionArguments =
				argumentsContext == null
						? emptyList()
						: (List<SqmTypedNode<?>>) argumentsContext.accept(this);

		final BasicType<?> returnableType = returnType( ctx.castTarget() );
		SqmFunctionDescriptor functionTemplate = getFunctionDescriptor( functionName );
		if ( functionTemplate == null ) {
			functionTemplate = new NamedSqmFunctionDescriptor(
					functionName,
					true,
					null,
					StandardFunctionReturnTypeResolvers.invariant(returnableType),
					null
			);
		}
		return functionTemplate.generateSqmExpression(
				functionArguments,
				returnableType,
				creationContext.getQueryEngine()
		);
	}

	@Override
	public SqmExpression<?> visitColumnFunction(HqlParser.ColumnFunctionContext ctx) {
		final String columnName = toName( ctx.jpaNonstandardFunctionName() );
		final SemanticPathPart semanticPathPart = visitPath( ctx.path() );
		final BasicType<?> resultType = returnType( ctx.castTarget() );
		return new SqlColumn( columnName, resultType ).generateSqmExpression(
				(SqmTypedNode<?>) semanticPathPart,
				resultType,
				creationContext.getQueryEngine()
		);
	}

	private BasicType<?> returnType(HqlParser.CastTargetContext castTarget) {
		if ( castTarget == null ) {
			return OBJECT_BASIC_TYPE;
		}
		else {
			return (BasicType<?>) visitCastTarget( castTarget ).getType();
		}
	}

	@Override
	public String visitGenericFunctionName(HqlParser.GenericFunctionNameContext ctx) {
		final StringBuilder functionName = new StringBuilder( visitIdentifier( ctx.simplePath().identifier() ) );
		for ( HqlParser.SimplePathElementContext sp: ctx.simplePath().simplePathElement() ) {
			// allow function names of form foo.bar to be located in the registry
			functionName.append('.').append( visitIdentifier( sp.identifier() ) );
		}
		return functionName.toString();
	}

	private String getFunctionName(HqlParser.GenericFunctionContext ctx) {
		final String originalFunctionName = visitGenericFunctionName( ctx.genericFunctionName() );
		final String functionName = originalFunctionName.toLowerCase();
		if ( creationOptions.useStrictJpaCompliance() && !JPA_STANDARD_FUNCTIONS.contains( functionName ) ) {
			throw new StrictJpaComplianceViolation(
					"Encountered non-compliant non-standard function call [" +
							originalFunctionName + "], but strict JPA " +
							"compliance was requested; use FUNCTION(functionName[,...]) " +
							"syntax name instead",
					StrictJpaComplianceViolation.Type.FUNCTION_CALL
			);
		}
		return functionName;
	}

	@Override
	public Object visitGenericFunction(HqlParser.GenericFunctionContext ctx) {
		final SqmFunctionDescriptor functionTemplate = getFunctionTemplate(  ctx );

		final List<SqmTypedNode<?>> functionArguments = getFunctionArguments( ctx );
		final SqmPredicate filterExpression = getFilterExpression( ctx );
		final SqmFunction<?> function;
		switch ( functionTemplate.getFunctionKind() ) {
			case ORDERED_SET_AGGREGATE:
				function = functionTemplate.generateOrderedSetAggregateSqmExpression(
						functionArguments,
						filterExpression,
						ctx.withinGroupClause() == null
								? null // this is allowed for e.g. rank(), but not for all
								: visitOrderByClause( ctx.withinGroupClause().orderByClause(), false ),
						null,
						creationContext.getQueryEngine()
				);
				break;
			case AGGREGATE:
				function = functionTemplate.generateAggregateSqmExpression(
						functionArguments,
						filterExpression,
						null,
						creationContext.getQueryEngine()
				);
				break;
			case WINDOW:
				function = functionTemplate.generateWindowSqmExpression(
						functionArguments,
						filterExpression,
						null,
						null,
						null,
						creationContext.getQueryEngine()
				);
				break;
			default:
				function = functionTemplate.generateSqmExpression(
						functionArguments,
						null,
						creationContext.getQueryEngine()
				);
				break;
		}
		return applyOverClause( ctx.overClause(), function );
	}

	private SqmFunctionDescriptor getFunctionTemplate(HqlParser.GenericFunctionContext ctx) {

		final String functionName = getFunctionName( ctx );

		final SqmFunctionDescriptor functionTemplate = getFunctionDescriptor( functionName );
		if ( functionTemplate == null ) {
			return new NamedSqmFunctionDescriptor(
					functionName,
					true,
					null,
					StandardFunctionReturnTypeResolvers.invariant(
							resolveExpressibleTypeBasic( Object.class )
					),
					null,
					functionName,
					inferFunctionKind( ctx ),
					null,
					SqlAstNodeRenderingMode.DEFAULT
			);
		}
		else {
			final FunctionKind functionKind = functionTemplate.getFunctionKind();
			if ( ctx.filterClause() != null && functionKind == FunctionKind.NORMAL ) {
				throw new SemanticException( "'FILTER' clause is illegal for non-aggregate function: "
						+ functionName,
						query );
			}
			if ( ctx.overClause() != null && functionKind == FunctionKind.NORMAL ) {
				throw new SemanticException( "'OVER' clause is illegal for non-aggregate function: "
						+ functionName,
						query );
			}
			if ( ctx.withinGroupClause() != null && functionKind == FunctionKind.NORMAL ) {
				throw new SemanticException( "'WITHIN' GROUP clause is illegal for non-aggregate function: "
						+ functionName,
						query );
			}
			if ( ctx.overClause() == null && functionKind == FunctionKind.WINDOW ) {
				throw new SemanticException( "'OVER' clause is mandatory for window-only function: "
						+ functionName,
						query );
			}
			if ( ctx.withinGroupClause() == null && ctx.overClause() == null
					&& functionKind == FunctionKind.ORDERED_SET_AGGREGATE ) {
				throw new SemanticException( "'WITHIN GROUP' or 'OVER' clause is mandatory for ordered set aggregate function: "
						+ functionName,
						query );
			}

			if ( ctx.nullsClause() != null ) {
				switch ( functionName ) {
					case "lag":
					case "lead":
					case "first_value":
					case "last_value":
					case "nth_value":
						break;
					default:
						throw new SemanticException( "'RESPECT NULLS' or 'IGNORE NULLS' are illegal for function: "
								+ functionName,
								query );
				}
			}
			if ( ctx.nthSideClause() != null && !"nth_value".equals( functionName ) ) {
				throw new SemanticException( "'FROM FIRST' or 'FROM LAST' are illegal for function: "
						+ functionName,
						query );
			}
			return functionTemplate;
		}
	}

	private static FunctionKind inferFunctionKind(HqlParser.GenericFunctionContext ctx) {
		if ( ctx.withinGroupClause() != null ) {
			return FunctionKind.ORDERED_SET_AGGREGATE;
		}
		else if ( ctx.overClause() != null ) {
			return FunctionKind.WINDOW;
		}
		else if ( ctx.filterClause() != null ) {
			return FunctionKind.AGGREGATE;
		}
		else {
			return FunctionKind.NORMAL;
		}
	}

	private List<SqmTypedNode<?>> getFunctionArguments(HqlParser.GenericFunctionContext ctx) {
		if ( ctx.genericFunctionArguments() != null ) {
			@SuppressWarnings("unchecked")
			final List<SqmTypedNode<?>> node = (List<SqmTypedNode<?>>)
					ctx.genericFunctionArguments().accept(this);
			return node;
		}
		else if ( ctx.ASTERISK() != null ) {
			return singletonList( new SqmStar( getCreationContext().getNodeBuilder() ) );
		}
		else {
			return emptyList();
		}
	}

	@Override
	public Object visitListaggFunction(ListaggFunctionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw new StrictJpaComplianceViolation(
					"Encountered non-compliant non-standard function call [listagg], but strict JPA " +
							"compliance was requested; use FUNCTION(functionName[,...]) " +
							"syntax name instead",
					StrictJpaComplianceViolation.Type.FUNCTION_CALL
			);
		}

		final SqmFunctionDescriptor functionTemplate = getFunctionDescriptor( "listagg" );
		if ( functionTemplate == null ) {
			throw new SemanticException( "The listagg() function was not registered for the dialect", query );
		}

		return applyOverClause(
				ctx.overClause(),
				functionTemplate.generateOrderedSetAggregateSqmExpression(
						getListaggArguments( ctx ),
						getFilterExpression( ctx ),
						ctx.withinGroupClause() == null
								? null // this is allowed
								: visitOrderByClause( ctx.withinGroupClause().orderByClause(), false ),
						null,
						creationContext.getQueryEngine()
				)
		);
	}

	private List<SqmTypedNode<?>> getListaggArguments(ListaggFunctionContext ctx) {
		final SqmExpression<?> firstArgument = (SqmExpression<?>) ctx.expressionOrPredicate(0).accept( this );
		final SqmExpression<?> secondArgument = (SqmExpression<?>) ctx.expressionOrPredicate(1).accept( this );
		final OnOverflowClauseContext overflowCtx = ctx.onOverflowClause();
		final List<SqmTypedNode<?>> functionArguments = new ArrayList<>( 3 );
		if ( ctx.DISTINCT() != null ) {
			functionArguments.add( new SqmDistinct<>( firstArgument, creationContext.getNodeBuilder() ) );
		}
		else {
			functionArguments.add( firstArgument );
		}
		if ( overflowCtx != null ) {
			if ( overflowCtx.ERROR() != null ) {
				// ON OVERFLOW ERROR
				functionArguments.add( new SqmOverflow<>( secondArgument, null, false ) );
			}
			else {
				// ON OVERFLOW TRUNCATE
				final SqmExpression<?> fillerExpression;
				if ( overflowCtx.expression() != null ) {
					fillerExpression = (SqmExpression<?>) overflowCtx.expression().accept( this );
				}
				else {
					// The SQL standard says the default is three periods `...`
					fillerExpression = new SqmLiteral<>(
							"...",
							resolveExpressibleTypeBasic( String.class ),
							secondArgument.nodeBuilder()
					);
				}
				final boolean withCount = overflowCtx.WITH() != null;
				//noinspection unchecked,rawtypes
				functionArguments.add( new SqmOverflow( secondArgument, fillerExpression, withCount ) );
			}
		}
		else {
			functionArguments.add( secondArgument );
		}
		return functionArguments;
	}

	@Override
	public List<SqmTypedNode<?>> visitGenericFunctionArguments(HqlParser.GenericFunctionArgumentsContext ctx) {
		final List<HqlParser.ExpressionOrPredicateContext> argumentContexts = ctx.expressionOrPredicate();
		int count = argumentContexts.size();
		final List<SqmTypedNode<?>> arguments = new ArrayList<>( count+1 );
		final HqlParser.DatetimeFieldContext datetimeFieldContext = ctx.datetimeField();
		if ( datetimeFieldContext != null  ) {
			arguments.add( toDurationUnit( (SqmExtractUnit<?>) datetimeFieldContext.accept( this ) ) );
		}
		for ( int i = 0; i < count-1; i++ ) {
			final HqlParser.ExpressionOrPredicateContext argumentContext = argumentContexts.get(i);
			arguments.add( (SqmTypedNode<?>) argumentContext.accept( this ) );
		}
		// we handle the last argument differently...
		final HqlParser.ExpressionOrPredicateContext argumentContext = argumentContexts.get( count-1 );
		arguments.add( visitFinalFunctionArgument( argumentContext ) );

		if ( ctx.DISTINCT() != null ) {
			final NodeBuilder nodeBuilder = getCreationContext().getNodeBuilder();
			if ( arguments.size() == 1 ) {
				arguments.set( 0, new SqmDistinct<>( (SqmExpression<?>) arguments.get( 0 ), nodeBuilder ) );
			}
			else {
				final List<SqmTypedNode<?>> newArguments = new ArrayList<>( 1 );
				@SuppressWarnings("unchecked")
				final List<SqmExpression<?>> expressions = (List<SqmExpression<?>>) (List<?>) arguments;
				newArguments.add( new SqmDistinct<>( new SqmTuple<>( expressions, nodeBuilder ), nodeBuilder ) );
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
			case HqlParser.EPOCH:
				return new SqmExtractUnit<>(
						TemporalUnit.EPOCH,
						resolveExpressibleTypeBasic( Long.class ),
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
			case HqlParser.HOUR:
				return new SqmExtractUnit<>( TIMEZONE_HOUR, resolveExpressibleTypeBasic( Integer.class ), nodeBuilder );
			case HqlParser.TIMEZONE_MINUTE:
			case HqlParser.MINUTE:
				return new SqmExtractUnit<>(
						TIMEZONE_MINUTE,
						resolveExpressibleTypeBasic( Integer.class ),
						nodeBuilder
				);
			case HqlParser.OFFSET:
				return new SqmExtractUnit<>( OFFSET, resolveExpressibleTypeBasic( ZoneOffset.class ), nodeBuilder );
			default:
				// should never happen
				throw new ParsingException("Unsupported time zone field [" + ctx.getText() + "]");
		}
	}

	@Override
	public Object visitExtractFunction(HqlParser.ExtractFunctionContext ctx) {
		final SqmExtractUnit<?> extractFieldExpression;
		if ( ctx.extractField() != null ) {
			//for the case of the full ANSI syntax "extract(field from arg)"
			extractFieldExpression = (SqmExtractUnit<?>) ctx.extractField().accept(this);
		}
		else {
			//for the shorter legacy Hibernate syntax "field(arg)"
			extractFieldExpression = (SqmExtractUnit<?>) ctx.datetimeField().accept(this);
//			//Prefer an existing native version if available
//			final SqmFunctionDescriptor functionDescriptor = getFunctionDescriptor( extractFieldExpression.getUnit().name() );
//			if ( functionDescriptor != null ) {
//				return functionDescriptor.generateSqmExpression(
//						expressionToExtract,
//						null,
//						creationContext.getQueryEngine(),
//						creationContext.getTypeConfiguration()
//				);
//			}
		}

		final SqmExpression<?> expressionToExtract = (SqmExpression<?>) ctx.expression().accept( this );
		// visitDateOrTimeField() needs to know if we're extracting from a
		// JDBC Timestamp or from a java.time LocalDateTime/OffsetDateTime
		isExtractingJdbcTemporalType = isJdbcTemporalType( expressionToExtract.getNodeType() );

		return getFunctionDescriptor("extract").generateSqmExpression(
				asList( extractFieldExpression, expressionToExtract ),
				extractFieldExpression.getType(),
				creationContext.getQueryEngine()
		);
	}

	@Override
	public Object visitTruncFunction(HqlParser.TruncFunctionContext ctx) {
		final SqmExpression<?> expression = (SqmExpression<?>) ctx.expression(0).accept( this );
		final SqmTypedNode<?> secondArg;
		if ( ctx.expression().size() > 1 ) {
			secondArg = (SqmTypedNode<?>) ctx.expression(1).accept( this );
		}
		else if ( ctx.datetimeField() != null ) {
			secondArg = (SqmTypedNode<?>) ctx.datetimeField().accept( this );
		}
		else {
			secondArg = null;
		}

		return getFunctionDescriptor( "trunc" ).generateSqmExpression(
				secondArg == null ? singletonList( expression ) : asList( expression, secondArg ),
				null,
				creationContext.getQueryEngine()
		);
	}

	@Override
	public Object visitFormat(HqlParser.FormatContext ctx) {
		final String format = unquoteStringLiteral( ctx.STRING_LITERAL().getText() );
		return new SqmFormat(
				format,
				resolveExpressibleTypeBasic( String.class ),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmExpression<?> visitFormatFunction(HqlParser.FormatFunctionContext ctx) {
		final SqmExpression<?> expressionToCast = (SqmExpression<?>) ctx.expression().accept( this );
		final SqmLiteral<?> format = (SqmLiteral<?>) ctx.format().accept( this );

		return getFunctionDescriptor("format").generateSqmExpression(
				asList( expressionToCast, format ),
				null, //why not string?
				creationContext.getQueryEngine()
		);
	}

	@Override
	public SqmExpression<?> visitCastFunction(HqlParser.CastFunctionContext ctx) {
		final SqmExpression<?> expressionToCast = (SqmExpression<?>) ctx.expression().accept( this );
		final SqmCastTarget<?> castTargetExpression = (SqmCastTarget<?>) ctx.castTarget().accept( this );

		return getFunctionDescriptor("cast").generateSqmExpression(
				asList( expressionToCast, castTargetExpression ),
				castTargetExpression.getType(),
				creationContext.getQueryEngine()
		);
	}

	@Override
	public SqmCastTarget<?> visitCastTarget(HqlParser.CastTargetContext castTargetContext) {
		final HqlParser.CastTargetTypeContext castTargetTypeContext = castTargetContext.castTargetType();
		final String targetName = castTargetTypeContext.fullTargetName;

		final TerminalNode firstArg = castTargetContext.INTEGER_LITERAL(0);
		final TerminalNode secondArg = castTargetContext.INTEGER_LITERAL(1);
		final Long length = firstArg == null ? null : Long.valueOf( firstArg.getText() );
		final Integer precision = firstArg == null ? null : Integer.valueOf( firstArg.getText() );
		final Integer scale = secondArg == null ? null : Integer.valueOf( secondArg.getText() );

		return new SqmCastTarget<>(
				creationContext.getTypeConfiguration()
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
		final SqmExpression<?> patternOrElement = (SqmExpression<?>) ctx.positionFunctionPatternArgument().accept( this );
		final SqmExpression<?> stringOrArray = (SqmExpression<?>) ctx.positionFunctionStringArgument().accept( this );

		final SqmExpressible<?> stringOrArrayExpressible = stringOrArray.getExpressible();
		if ( stringOrArrayExpressible != null && stringOrArrayExpressible.getSqmType() instanceof BasicPluralType<?, ?> ) {
			return getFunctionDescriptor( "array_position" ).generateSqmExpression(
					asList( stringOrArray, patternOrElement ),
					null,
					creationContext.getQueryEngine()
			);
		}
		else {
			return getFunctionDescriptor( "position" ).generateSqmExpression(
					asList( patternOrElement, stringOrArray ),
					null,
					creationContext.getQueryEngine()
			);
		}
	}

	@Override
	public Object visitOverlayFunction(HqlParser.OverlayFunctionContext ctx) {
		final SqmExpression<?> string = (SqmExpression<?>) ctx.overlayFunctionStringArgument().accept( this );
		final SqmExpression<?> replacement = (SqmExpression<?>) ctx.overlayFunctionReplacementArgument().accept( this );
		final SqmExpression<?> start = (SqmExpression<?>) ctx.overlayFunctionStartArgument().accept( this );
		final SqmExpression<?> length = ctx.overlayFunctionLengthArgument() != null
				? (SqmExpression<?>) ctx.overlayFunctionLengthArgument().accept( this )
				: null;

		return getFunctionDescriptor("overlay").generateSqmExpression(
				length == null
						? asList( string, replacement, start )
						: asList( string, replacement, start, length ),
				null,
				creationContext.getQueryEngine()
		);
	}

	@Override
	public SqmExpression<?> visitEveryFunction(HqlParser.EveryFunctionContext ctx) {
		if ( ctx.subquery() != null ) {
			final SqmSubQuery<?> subquery = (SqmSubQuery<?>) ctx.subquery().accept( this );
			return new SqmEvery<>( subquery, creationContext.getNodeBuilder() );
		}
		else if ( ctx.predicate() != null ) {
			if ( getCreationOptions().useStrictJpaCompliance() ) {
				throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.FUNCTION_CALL );
			}
			final SqmExpression<?> argument = (SqmExpression<?>) ctx.predicate().accept( this );
			return applyOverClause(
					ctx.overClause(),
					getFunctionDescriptor( "every" ).generateAggregateSqmExpression(
							singletonList( argument ),
							getFilterExpression( ctx ),
							resolveExpressibleTypeBasic( Boolean.class ),
							creationContext.getQueryEngine()
					)
			);
		}
		else {
			if ( getCreationOptions().useStrictJpaCompliance() ) {
				throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.HQL_COLLECTION_FUNCTION );
			}
			return new SqmEvery<>(
					createCollectionReferenceSubQuery(
							ctx.simplePath(),
							(TerminalNode) ctx.collectionQuantifier().getChild(0).getChild(0)
					),
					creationContext.getNodeBuilder()
			);
		}
	}

	@Override
	public SqmExpression<?> visitAnyFunction(HqlParser.AnyFunctionContext ctx) {
		if ( ctx.subquery() != null ) {
			final SqmSubQuery<?> subquery = (SqmSubQuery<?>) ctx.subquery().accept( this );
			return new SqmAny<>( subquery, creationContext.getNodeBuilder() );
		}
		else if ( ctx.predicate() != null ) {
			if ( getCreationOptions().useStrictJpaCompliance() ) {
				throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.FUNCTION_CALL );
			}
			final SqmExpression<?> argument = (SqmExpression<?>) ctx.predicate().accept( this );
			return applyOverClause(
					ctx.overClause(),
					getFunctionDescriptor( "any" ).generateAggregateSqmExpression(
							singletonList( argument ),
							getFilterExpression( ctx ),
							resolveExpressibleTypeBasic( Boolean.class ),
							creationContext.getQueryEngine()
					)
			);
		}
		else {
			if ( getCreationOptions().useStrictJpaCompliance() ) {
				throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.HQL_COLLECTION_FUNCTION );
			}
			return new SqmAny<>(
					createCollectionReferenceSubQuery(
							ctx.simplePath(),
							(TerminalNode) ctx.collectionQuantifier().getChild(0).getChild(0)
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
			//TODO: improve this message
			throw new SemanticException( "Path is not a plural path '" + pluralAttributePath.getNavigablePath() + "'",
					query );
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

	private SqmPredicate getFilterExpression(ParseTree functionCtx) {
		for ( int i = functionCtx.getChildCount() - 2; i < functionCtx.getChildCount(); i++ ) {
			final ParseTree child = functionCtx.getChild( i );
			if ( child instanceof HqlParser.FilterClauseContext ) {
				return (SqmPredicate) child.getChild( 2 ).getChild( 1 ).accept( this );
			}
		}
		return null;
	}

	private SqmExpression<?> applyOverClause(HqlParser.OverClauseContext ctx, SqmFunction<?> function) {
		if ( ctx == null) {
			return function;
		}

		final List<SqmExpression<?>> partitions;
		if ( ctx.partitionClause() != null ) {
			final HqlParser.PartitionClauseContext partitionClause = ctx.partitionClause();
			partitions = new ArrayList<>( ( partitionClause.getChildCount() >> 1 ) - 1 );
			for ( int i = 2; i < partitionClause.getChildCount(); i += 2 ) {
				partitions.add( (SqmExpression<?>) partitionClause.getChild( i ).accept( this ) );
			}
		}
		else {
			partitions = emptyList();
		}

		final List<SqmSortSpecification> orderList = ctx.orderByClause() != null
				? visitOrderByClause( ctx.orderByClause(), false ).getSortSpecifications()
				: emptyList();

		final FrameMode mode;
		final FrameKind startKind;
		final SqmExpression<?> startExpression;
		final FrameKind endKind;
		final SqmExpression<?> endExpression;
		final FrameExclusion exclusion;
		final HqlParser.FrameClauseContext frameClause = ctx.frameClause();
		if ( frameClause != null ) {
			switch ( ( (TerminalNode) frameClause.getChild( 0 ) ).getSymbol().getType() ) {
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
					throw new IllegalArgumentException( "Unexpected frame mode: " + frameClause.getChild( 0 ) );
			}
			final int frameStartIndex;
			if ( frameClause.getChild( 1 ) instanceof TerminalNode ) {
				frameStartIndex = 2;
				endKind = getFrameKind( frameClause.getChild( 4 ) );
				endExpression = endKind == FrameKind.OFFSET_FOLLOWING || endKind == FrameKind.OFFSET_PRECEDING
						? (SqmExpression<?>) frameClause.getChild( 4 ).getChild( 0 ).accept( this )
						: null;
			}
			else {
				frameStartIndex = 1;
				endKind = FrameKind.CURRENT_ROW;
				endExpression = null;
			}
			startKind = getFrameKind( frameClause.getChild( frameStartIndex ) );
			startExpression = startKind == FrameKind.OFFSET_FOLLOWING || startKind == FrameKind.OFFSET_PRECEDING
					? (SqmExpression<?>) frameClause.getChild( frameStartIndex ).getChild( 0 ).accept( this )
					: null;
			final ParseTree lastChild = frameClause.getChild( frameClause.getChildCount() - 1 );
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
			mode = FrameMode.RANGE;
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
		final SqmExpression<?> source = (SqmExpression<?>) ctx.expression().accept( this );
		final SqmExpression<?> start = (SqmExpression<?>) ctx.substringFunctionStartArgument().accept( this );
		final SqmExpression<?> length = ctx.substringFunctionLengthArgument() != null
				? (SqmExpression<?>) ctx.substringFunctionLengthArgument().accept( this )
				: null;

		return getFunctionDescriptor("substring").generateSqmExpression(
				length == null ? asList( source, start ) : asList( source, start, length ),
				null,
				creationContext.getQueryEngine()
		);
	}

	@Override
	public SqmExpression<?> visitPadFunction(HqlParser.PadFunctionContext ctx) {
		final SqmExpression<?> source = (SqmExpression<?>) ctx.expression().accept( this );
		final SqmExpression<?> length = (SqmExpression<?>) ctx.padLength().accept(this);
		final SqmTrimSpecification padSpec = visitPadSpecification( ctx.padSpecification() );
		final SqmLiteral<Character> padChar = ctx.padCharacter() != null
				? visitPadCharacter( ctx.padCharacter() )
				: null;

		return getFunctionDescriptor("pad").generateSqmExpression(
				padChar != null
						? asList( source, length, padSpec, padChar )
						: asList( source, length, padSpec ),
				null,
				creationContext.getQueryEngine()
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
			throw new SemanticException( "Pad character for pad() function must be single character, found '"
					+ padCharText + "'",
					query );
		}

		return new SqmLiteral<>(
				padCharText.charAt( 1 ),
				resolveExpressibleTypeBasic( Character.class ),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmExpression<?> visitTrimFunction(HqlParser.TrimFunctionContext ctx) {
		final SqmExpression<?> source = (SqmExpression<?>) ctx.expression().accept( this );
		final SqmTrimSpecification trimSpec = visitTrimSpecification( ctx.trimSpecification() );;
		final SqmExpression<Character> trimChar = visitTrimCharacter( ctx.trimCharacter() );

		return getFunctionDescriptor("trim").generateSqmExpression(
				asList(
						trimSpec,
						trimChar,
						source
				),
				null,
				creationContext.getQueryEngine()
		);
	}

	@Override
	public SqmTrimSpecification visitTrimSpecification(HqlParser.TrimSpecificationContext ctx) {
		// JPA says the default is BOTH
		final TrimSpec spec = ctx != null ? trimSpec( ctx ) : TrimSpec.BOTH;
		return new SqmTrimSpecification( spec, creationContext.getNodeBuilder() );
	}

	private static TrimSpec trimSpec(HqlParser.TrimSpecificationContext ctx) {
		switch ( ( (TerminalNode) ctx.getChild( 0 ) ).getSymbol().getType() ) {
			case HqlParser.LEADING:
				return TrimSpec.LEADING;
			case HqlParser.TRAILING:
				return TrimSpec.TRAILING;
			case HqlParser.BOTH:
				return TrimSpec.BOTH;
			default:
				throw new ParsingException( "Unrecognized trim specification" );
		}
	}

	@Override
	public SqmExpression<Character> visitTrimCharacter(HqlParser.TrimCharacterContext ctx) {
		final String trimCharText;
		if ( ctx == null ) {
			// JPA says space is the default
			trimCharText = " ";
		}
		else {
			final ParseTree child = ctx.getChild( 0 );
			if ( child instanceof HqlParser.ParameterContext ) {
				//noinspection unchecked
				return (SqmExpression<Character>) child.accept( this );
			}
			else {
				trimCharText = unquoteStringLiteral( ctx.getText() );
				if ( trimCharText.length() != 1 ) {
					throw new SemanticException( "Trim character for trim() function must be single character, found '"
							+ trimCharText + "'",
							query );
				}
			}
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

	private boolean isIndexedPluralAttribute(SqmPath<?> path) {
		return path.getReferencedPathSource() instanceof PluralPersistentAttribute;
	}

	@Override
	public SqmPath<?> visitCollectionFunctionMisuse(HqlParser.CollectionFunctionMisuseContext ctx) {

		// Note: this is a total misuse of the elements() and indices() functions,
		//       which are supposed to be a shortcut way to write a subquery!
		//       used this way, they're just a worse way to write value()/index()
		log.warn("Misuse of HQL elements() or indices() function, use element() or index() instead");

		if ( getCreationOptions().useStrictJpaCompliance() ) {
			throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.HQL_COLLECTION_FUNCTION );
		}

		final SqmPath<?> pluralAttributePath = consumeDomainPath( ctx.path() );
		final SqmPathSource<?> referencedPathSource = pluralAttributePath.getReferencedPathSource();
		final TerminalNode firstNode = (TerminalNode) ctx.getChild( 0 ).getChild( 0 );

		if ( !(referencedPathSource instanceof PluralPersistentAttribute<?, ?, ?> ) ) {
			throw new FunctionArgumentException(
					String.format(
							"Argument '%s' of '%s()' function is not a plural path ",
							pluralAttributePath.getNavigablePath(),
							firstNode.getSymbol().getText()
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
	public SqmExpression<?> visitElementAggregateFunction(HqlParser.ElementAggregateFunctionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.HQL_COLLECTION_FUNCTION );
		}

		// the actual function name might be 'minelement' or 'maxelement', so trim it
		final String functionName = ctx.getChild(0).getText().substring(0, 3);

		final SqmPath<?> pluralPath = consumePluralAttributeReference( ctx.path() );
		if ( pluralPath instanceof SqmPluralValuedSimplePath ) {
			return new SqmElementAggregateFunction<>( pluralPath, functionName );
		}
		else {
			// elements() and values() and only apply to compound paths
			if ( pluralPath instanceof SqmMapJoin ) {
				throw new FunctionArgumentException( "Path '" + ctx.path().getText()
						+ "' resolved to a joined map instead of a compound path" );
			}
			else if ( pluralPath instanceof SqmListJoin ) {
				throw new FunctionArgumentException( "Path '" + ctx.path().getText()
						+ "' resolved to a joined list instead of a compound path" );
			}
			else {
				throw new FunctionArgumentException( "Path '" + ctx.path().getText()
						+ "' did not resolve to a many-valued attribute" );
			}
		}
	}

	@Override
	public SqmExpression<?> visitIndexAggregateFunction(HqlParser.IndexAggregateFunctionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.HQL_COLLECTION_FUNCTION );
		}

		// the actual function name might be 'minindex' or 'maxindex', so trim it
		final String functionName = ctx.getChild(0).getText().substring(0, 3);

		final SqmPath<?> pluralPath = consumePluralAttributeReference( ctx.path() );
		if ( pluralPath instanceof SqmPluralValuedSimplePath ) {
			if ( isIndexedPluralAttribute( pluralPath ) ) {
				return new SqmIndexAggregateFunction<>( pluralPath, functionName );
			}
			else {
				throw new FunctionArgumentException( "Path '" + ctx.path()
						+  "' resolved to '"
						+ pluralPath.getReferencedPathSource()
						+ "' which is not an indexed collection" );
			}
		}
		else {
			// indices() and keys() only apply to compound paths
			if ( pluralPath instanceof SqmMapJoin ) {
				throw new FunctionArgumentException( "Path '" + ctx.path().getText()
						+ "' resolved to a joined map instead of a compound path" );
			}
			else if ( pluralPath instanceof SqmListJoin ) {
				throw new FunctionArgumentException( "Path '" + ctx.path().getText()
						+ "' resolved to a joined list instead of a compound path" );
			}
			else {
				throw new FunctionArgumentException( "Path '" + ctx.path().getText()
						+ "' did not resolve to a many-valued attribute" );
			}
		}
	}

	@Override
	public SqmSubQuery<?> visitSubqueryExpression(HqlParser.SubqueryExpressionContext ctx) {
		return visitSubquery( (HqlParser.SubqueryContext) ctx.getChild( 1 ) );
	}

	@Override
	public SqmSubQuery<?> visitSubquery(HqlParser.SubqueryContext ctx) {
		final HqlParser.QueryExpressionContext queryExpressionContext = ctx.queryExpression();
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
				final SqmExpressible<?> expressible = selections.get( 0 ).getExpressible();
				if ( expressible != null ) {
					subQuery.applyInferableType( expressible.getSqmType() );
				}
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
		final HqlParser.SyntacticDomainPathContext syntacticDomainPath = ctx.syntacticDomainPath();
		final HqlParser.GeneralPathFragmentContext generalPathFragment = ctx.generalPathFragment();
		if ( syntacticDomainPath != null ) {
			return visitPathContinuation( visitSyntacticDomainPath( syntacticDomainPath ), ctx.pathContinuation() );
		}
		else if (generalPathFragment != null) {
			return (SemanticPathPart) generalPathFragment.accept(this);
		}
		else {
			throw new ParsingException("Illegal path '" + ctx.getText() + "'");
		}
	}

	@Override
	public SemanticPathPart visitGeneralPathFragment(HqlParser.GeneralPathFragmentContext ctx) {
		return visitIndexedPathAccessFragment( visitSimplePath( ctx.simplePath() ), ctx.indexedPathAccessFragment() );
	}

	@Override
	public SemanticPathPart visitSyntacticDomainPath(HqlParser.SyntacticDomainPathContext ctx) {
		if ( ctx.treatedNavigablePath() != null ) {
			return visitTreatedNavigablePath( ctx.treatedNavigablePath() );
		}
		else if ( ctx.collectionValueNavigablePath() != null ) {
			return visitCollectionValueNavigablePath( ctx.collectionValueNavigablePath() );
		}
		else if ( ctx.mapKeyNavigablePath() != null ) {
			return visitMapKeyNavigablePath( ctx.mapKeyNavigablePath() );
		}
		else if ( ctx.toOneFkReference() != null ) {
			return visitToOneFkReference( ctx.toOneFkReference() );
		}
		else if ( ctx.function() != null ) {
			final HqlParser.SlicedPathAccessFragmentContext slicedFragmentsCtx = ctx.slicedPathAccessFragment();
			if ( slicedFragmentsCtx != null ) {
				final List<HqlParser.ExpressionContext> slicedFragments = slicedFragmentsCtx.expression();
				return getFunctionDescriptor( "array_slice" ).generateSqmExpression(
						List.of(
								(SqmTypedNode<?>) visitFunction( ctx.function() ),
								(SqmTypedNode<?>) slicedFragments.get( 0 ).accept( this ),
								(SqmTypedNode<?>) slicedFragments.get( 1 ).accept( this )
						),
						null,
						creationContext.getQueryEngine()
				);
			}
			else {
				return visitPathContinuation(
						visitIndexedPathAccessFragment(
								(SemanticPathPart) visitFunction( ctx.function() ),
								ctx.indexedPathAccessFragment()
						),
						ctx.pathContinuation()
				);
			}
		}
		else if ( ctx.simplePath() != null && ctx.indexedPathAccessFragment() != null ) {
			return visitIndexedPathAccessFragment( visitSimplePath( ctx.simplePath() ), ctx.indexedPathAccessFragment() );
		}
		else if ( ctx.simplePath() != null && ctx.slicedPathAccessFragment() != null ) {
			final List<HqlParser.ExpressionContext> slicedFragments = ctx.slicedPathAccessFragment().expression();
			final SqmTypedNode<?> lhs = (SqmTypedNode<?>) visitSimplePath( ctx.simplePath() );
			final SqmExpressible<?> lhsExpressible = lhs.getExpressible();
			if ( lhsExpressible != null && lhsExpressible.getSqmType() instanceof BasicPluralType<?, ?> ) {
				return getFunctionDescriptor( "array_slice" ).generateSqmExpression(
						List.of(
								lhs,
								(SqmTypedNode<?>) slicedFragments.get( 0 ).accept( this ),
								(SqmTypedNode<?>) slicedFragments.get( 1 ).accept( this )
						),
						null,
						creationContext.getQueryEngine()
				);
			}
			else {
				final SqmExpression<?> start = (SqmExpression<?>) slicedFragments.get( 0 ).accept( this );
				final SqmExpression<?> end = (SqmExpression<?>) slicedFragments.get( 1 ).accept( this );
				return getFunctionDescriptor( "substring" ).generateSqmExpression(
						List.of(
								lhs,
								start,
								new SqmBinaryArithmetic<>(
										BinaryArithmeticOperator.ADD,
										new SqmBinaryArithmetic<>(
												BinaryArithmeticOperator.SUBTRACT,
												end,
												start,
												creationContext.getJpaMetamodel(),
												creationContext.getNodeBuilder()
										),
										new SqmLiteral<>(
												1,
												creationContext.getNodeBuilder().getIntegerType(),
												creationContext.getNodeBuilder()
										),
										creationContext.getJpaMetamodel(),
										creationContext.getNodeBuilder()
								)
						),
						null,
						creationContext.getQueryEngine()
				);
			}
		}
		else {
			throw new ParsingException( "Illegal domain path '" + ctx.getText() + "'" );
		}
	}

	private SemanticPathPart visitIndexedPathAccessFragment(
			SemanticPathPart pathPart,
			HqlParser.IndexedPathAccessFragmentContext idxCtx) {
		if ( idxCtx == null ) {
			return pathPart;
		}

		final SqmExpression<?> indexExpression = (SqmExpression<?>) idxCtx.expression().accept(this );
		final boolean hasIndexContinuation = idxCtx.DOT() != null;
		final SqmPath<?> indexedPath =
				pathPart.resolveIndexedAccess( indexExpression, !hasIndexContinuation, this );
		if ( hasIndexContinuation ) {
			dotIdentifierConsumerStack.push(
					new BasicDotIdentifierConsumer( indexedPath, this ) {
						@Override
						protected void reset() {
						}
					}
			);
			try {
				return (SemanticPathPart) idxCtx.generalPathFragment().accept( this );
			}
			finally {
				dotIdentifierConsumerStack.pop();
			}
		}
		return indexedPath;
	}

	private SemanticPathPart visitPathContinuation(
			SemanticPathPart pathPart,
			HqlParser.PathContinuationContext pathContinuation) {
		if ( pathContinuation == null ) {
			return pathPart;
		}
		dotIdentifierConsumerStack.push(
				new BasicDotIdentifierConsumer( pathPart, this ) {
					@Override
					protected void reset() {
					}
				}
		);
		try {
			return (SemanticPathPart) pathContinuation.simplePath().accept( this );
		}
		finally {
			dotIdentifierConsumerStack.pop();
		}
	}

	@Override
	public SemanticPathPart visitIndexedPathAccessFragment(HqlParser.IndexedPathAccessFragmentContext idxCtx) {
		throw new UnsupportedOperationException( "Should be handled by #visitIndexedPathAccessFragment" );
	}

	@Override
	public SemanticPathPart visitSimplePath(HqlParser.SimplePathContext ctx) {
		final int numberOfContinuations = ctx.simplePathElement().size();

		final DotIdentifierConsumer dotIdentifierConsumer = dotIdentifierConsumerStack.getCurrent();
		final HqlParser.IdentifierContext identifierContext = ctx.identifier();
		assert identifierContext.getChildCount() == 1;

		dotIdentifierConsumer.consumeIdentifier(
				visitIdentifier( identifierContext ),
				true,
				numberOfContinuations == 0
		);

		for ( int i = 0; i < numberOfContinuations; i++ ) {
			final HqlParser.SimplePathElementContext continuation = ctx.simplePathElement( i );
			final HqlParser.IdentifierContext identifier = continuation.identifier();
			assert identifier.getChildCount() == 1;
			dotIdentifierConsumer.consumeIdentifier(
					visitIdentifier( identifier ),
					false,
					i+1 == numberOfContinuations
			);
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
		consumeManagedTypeReference( ctx.path() );

		final String treatTargetName = ctx.simplePath().getText();
		final String importableName = getCreationContext().getJpaMetamodel().qualifyImportableName( treatTargetName );
		if ( importableName == null ) {
			throw new SemanticException( "Could not resolve treat target type '" + treatTargetName + "'", query );
		}

		final boolean hasContinuation = ctx.getChildCount() == 7;
		consumer.consumeTreat( importableName, !hasContinuation );
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
				result = consumeDomainPath( ctx.pathContinuation().simplePath() );
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
		final SqmPath<?> sqmPath = consumeDomainPath( ctx.path() );
		final boolean hasContinuation = ctx.getChildCount() == 5;

		final SqmPathSource<?> referencedPathSource = sqmPath.getReferencedPathSource();
		final TerminalNode firstNode = (TerminalNode) ctx.elementValueQuantifier().getChild(0);
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
			final HqlParser.SimplePathContext identCtx = ctx.pathContinuation().simplePath();
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
		final SqmPath<?> sqmPath = consumeDomainPath( ctx.path() );
		final boolean hasContinuation = ctx.getChildCount() == 5;

		final SqmPathSource<?> referencedPathSource = sqmPath.getReferencedPathSource();
		final TerminalNode firstNode = (TerminalNode) ctx.indexKeyQuantifier().getChild(0);
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
				throw new TerminalPathException("List index has no attributes");
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
			final HqlParser.SimplePathContext identCtx = ctx.pathContinuation().simplePath();
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
			throw new FunctionArgumentException(
					String.format(
							"Argument of '%s' is not a plural path '%s'",
							firstNode.getSymbol().getText(),
							pluralAttributePath.getNavigablePath()
					)
			);
		}
	}

	private SqmPath<?> consumeDomainPath(HqlParser.PathContext parserPath) {
		final SemanticPathPart consumedPart = (SemanticPathPart) parserPath.accept( this );
		if ( consumedPart instanceof SqmPath ) {
			return (SqmPath<?>) consumedPart;
		}
		else {
			throw new PathException( "Expecting domain-model path, but found: " + consumedPart );
		}
	}


	private SqmPath<?> consumeDomainPath(HqlParser.SimplePathContext sequence) {
		final SemanticPathPart consumedPart = (SemanticPathPart) sequence.accept( this );
		if ( consumedPart instanceof SqmPath ) {
			return (SqmPath<?>) consumedPart;
		}
		else {
			throw new PathException( "Expecting domain-model path, but found: " + consumedPart );
		}
	}

	private SqmPath<?> consumeManagedTypeReference(HqlParser.PathContext parserPath) {
		final SqmPath<?> sqmPath = consumeDomainPath( parserPath );
		final SqmPathSource<?> pathSource = sqmPath.getReferencedPathSource();
		if ( pathSource.getSqmPathType() instanceof ManagedDomainType<?> ) {
			return sqmPath;
		}
		else {
			throw new PathException( "Expecting ManagedType valued path [" + sqmPath.getNavigablePath() + "], but found: " + pathSource.getSqmPathType() );
		}
	}

	private SqmPath<?> consumePluralAttributeReference(HqlParser.PathContext parserPath) {
		final SqmPath<?> sqmPath = consumeDomainPath( parserPath );
		if ( sqmPath.getReferencedPathSource() instanceof PluralPersistentAttribute ) {
			return sqmPath;
		}
		else {
			throw new PathException( "Expecting plural attribute valued path [" + sqmPath.getNavigablePath() + "], but found: "
					+ sqmPath.getReferencedPathSource().getSqmPathType() );
		}
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
