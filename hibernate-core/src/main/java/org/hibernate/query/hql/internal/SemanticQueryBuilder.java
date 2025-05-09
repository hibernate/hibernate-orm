/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.cfg.QuerySettings;
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
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PathSource;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.internal.AnyDiscriminatorSqmPath;
import org.hibernate.metamodel.model.domain.internal.EntitySqmPathSource;
import org.hibernate.query.NullPrecedence;
import org.hibernate.query.ParameterLabelException;
import org.hibernate.query.PathException;
import org.hibernate.query.SemanticException;
import org.hibernate.query.SortDirection;
import org.hibernate.query.SyntaxException;
import org.hibernate.query.criteria.JpaCteCriteria;
import org.hibernate.query.criteria.JpaCteCriteriaAttribute;
import org.hibernate.query.criteria.JpaCteCriteriaType;
import org.hibernate.query.criteria.JpaJsonExistsNode;
import org.hibernate.query.criteria.JpaJsonQueryNode;
import org.hibernate.query.criteria.JpaJsonTableColumnsNode;
import org.hibernate.query.criteria.JpaJsonValueNode;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSearchOrder;
import org.hibernate.query.criteria.JpaXmlTableColumnNode;
import org.hibernate.query.sqm.tree.domain.SqmEntityDomainType;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleType;
import org.hibernate.query.hql.HqlLogging;
import org.hibernate.query.hql.spi.DotIdentifierConsumer;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmCreationOptions;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.hql.spi.SqmPathRegistry;
import org.hibernate.query.sqm.BinaryArithmeticOperator;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.common.FrameExclusion;
import org.hibernate.query.common.FrameKind;
import org.hibernate.query.common.FrameMode;
import org.hibernate.query.sqm.LiteralNumberFormatException;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.SqmTreeCreationLogger;
import org.hibernate.query.sqm.StrictJpaComplianceViolation;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.TerminalPathException;
import org.hibernate.query.sqm.TrimSpec;
import org.hibernate.query.sqm.UnaryArithmeticOperator;
import org.hibernate.query.sqm.UnknownEntityException;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.function.NamedSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmSetReturningFunctionDescriptor;
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
import org.hibernate.query.sqm.tree.domain.SqmFkExpression;
import org.hibernate.query.sqm.tree.domain.SqmFunctionRoot;
import org.hibernate.query.sqm.tree.domain.SqmIndexAggregateFunction;
import org.hibernate.query.sqm.tree.domain.SqmListJoin;
import org.hibernate.query.sqm.tree.domain.SqmMapEntryReference;
import org.hibernate.query.sqm.tree.domain.SqmMapJoin;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPolymorphicRootDescriptor;
import org.hibernate.query.sqm.tree.expression.*;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmCteJoin;
import org.hibernate.query.sqm.tree.from.SqmDerivedJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmFunctionJoin;
import org.hibernate.query.sqm.tree.from.SqmJoin;
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
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.descriptor.java.spi.UnknownBasicJavaType;
import org.hibernate.type.descriptor.jdbc.ObjectJdbcType;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

import jakarta.persistence.criteria.Expression;
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
import static org.hibernate.query.common.TemporalUnit.DATE;
import static org.hibernate.query.common.TemporalUnit.DAY_OF_MONTH;
import static org.hibernate.query.common.TemporalUnit.DAY_OF_WEEK;
import static org.hibernate.query.common.TemporalUnit.DAY_OF_YEAR;
import static org.hibernate.query.common.TemporalUnit.NANOSECOND;
import static org.hibernate.query.common.TemporalUnit.OFFSET;
import static org.hibernate.query.common.TemporalUnit.TIME;
import static org.hibernate.query.common.TemporalUnit.TIMEZONE_HOUR;
import static org.hibernate.query.common.TemporalUnit.TIMEZONE_MINUTE;
import static org.hibernate.query.common.TemporalUnit.WEEK_OF_MONTH;
import static org.hibernate.query.common.TemporalUnit.WEEK_OF_YEAR;
import static org.hibernate.query.sqm.internal.SqmUtil.resolveExpressibleJavaTypeClass;
import static org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation.forClassInstantiation;
import static org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation.forListInstantiation;
import static org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation.forMapInstantiation;
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
	private static final Set<String> JPA_STANDARD_FUNCTIONS = Set.of(
			"avg",
			"max",
			"min",
			"sum",
			"count",
			"length",
			"locate",
			"abs",
			"sqrt",
			"mod",
			"size",
			"index",
			"current_date",
			"current_time",
			"current_timestamp",
			"concat",
			"substring",
			"trim",
			"lower",
			"upper",
			"coalesce",
			"nullif",
			"left",
			"right",
			"replace"
	);

	private static final BasicTypeImpl<Object> OBJECT_BASIC_TYPE =
			new BasicTypeImpl<>( new UnknownBasicJavaType<>(Object.class), ObjectJdbcType.INSTANCE );

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

	private final Stack<ParameterDeclarationContext> parameterDeclarationContextStack = new StandardStack<>();
	private final Stack<SqmCreationProcessingState> processingStateStack = new StandardStack<>();

	private final BasicType<Integer> integerDomainType;
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
				new BasicDotIdentifierConsumer( this )
		);
		this.parameterStyle = creationOptions.useStrictJpaCompliance()
				? ParameterStyle.UNKNOWN
				: ParameterStyle.MIXED;

		final TypeConfiguration typeConfiguration = creationContext.getTypeConfiguration();
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
			if ( parseTree instanceof HqlParser.SelectStatementContext selectStatementContext ) {
				final SqmSelectStatement<R> selectStatement = visitSelectStatement( selectStatementContext );
				selectStatement.getQueryPart().validateQueryStructureAndFetchOwners();
				return selectStatement;
			}
			else if ( parseTree instanceof HqlParser.InsertStatementContext insertStatementContext ) {
				return visitInsertStatement( insertStatementContext );
			}
			else if ( parseTree instanceof HqlParser.UpdateStatementContext updateStatementContext ) {
				return visitUpdateStatement( updateStatementContext );
			}
			else if ( parseTree instanceof HqlParser.DeleteStatementContext deleteStatementContext ) {
				return visitDeleteStatement( deleteStatementContext );
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
						final Set<String> possibleEnumTypes;
						final SqmExpression<?> value;
						if ( isEnum
								&& valuesContext.getChild( 0 ) instanceof HqlParser.ExpressionContext expressionContext
								&& ( possibleEnumTypes = getPossibleEnumTypes( expressionContext ) ) != null ) {
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
			updateAction.where( visitWhereClause( conflictActionContext.whereClause() ) );
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
				if ( subCtx instanceof HqlParser.AssignmentContext assignmentContext ) {
					updateStatement.applyAssignment( visitAssignment( assignmentContext ) );
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
		final Set<String> possibleEnumValues;
		final SqmExpression<?> value;
		if ( isEnum
				&& rightSide.getChild( 0 ) instanceof HqlParser.ExpressionContext expressionContext
				&& ( possibleEnumValues = getPossibleEnumTypes( expressionContext ) ) != null ) {
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
		final SqmDmlCreationProcessingState sqmDeleteCreationState =
				new SqmDmlCreationProcessingState( deleteStatement, this );
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

		final HqlParser.QueryExpressionContext queryExpressionContext =
				(HqlParser.QueryExpressionContext) ctx.getChild( queryExpressionIndex );
		final SqmSelectQuery<Object> cte =
				cteContainer instanceof SqmSubQuery<?> subQuery
						? new SqmSubQuery<>( subQuery.getParent(), creationContext.getNodeBuilder() )
						: new SqmSelectStatement<>( creationContext.getNodeBuilder() );
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
			if ( queryExpressionContext instanceof HqlParser.SetQueryGroupContext setContext ) {
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
							final SqmSelectStatement<Object> recursivePart =
									new SqmSelectStatement<>( creationContext.getNodeBuilder() );

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
							if ( lastChild instanceof HqlParser.CycleClauseContext cycleClauseContext ) {
								applyCycleClause( cteDefinition, cycleClauseContext );
								potentialSearchClause = ctx.getChild( ctx.getChildCount() - 2 );
							}
							else {
								potentialSearchClause = lastChild;
							}
							if ( potentialSearchClause instanceof HqlParser.SearchClauseContext searchClauseContext ) {
								applySearchClause( cteDefinition, searchClauseContext );
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
			cyclePathAttributeName =
					lastChild instanceof HqlParser.IdentifierContext identifierContext
							? visitIdentifier( identifierContext )
							: null;
		}
		else {
			cyclePathAttributeName = null;
			cycleValue = Boolean.TRUE;
			noCycleValue = Boolean.FALSE;
		}

		cteDefinition.cycleUsing( cycleMarkAttributeName, cyclePathAttributeName, cycleValue, noCycleValue, cycleAttributes );
	}

	private void applySearchClause(JpaCteCriteria<?> cteDefinition, HqlParser.SearchClauseContext ctx) {
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
					final Token symbol = ((TerminalNode) sortCtx.getChild( 0 )).getSymbol();
					sortOrder = switch ( symbol.getType() ) {
						case HqlParser.ASC -> SortDirection.ASCENDING;
						case HqlParser.DESC -> SortDirection.DESCENDING;
						default -> throw new UnsupportedOperationException(
								"Unrecognized sort ordering: " + sortCtx.getText() );
					};
					index++;
				}
				if ( index < specCtx.getChildCount() ) {
					final HqlParser.NullsPrecedenceContext nullsPrecedenceContext = specCtx.nullsPrecedence();
					final Token symbol = ((TerminalNode) nullsPrecedenceContext.getChild( 1 )).getSymbol();
					nullPrecedence = switch ( symbol.getType() ) {
						case HqlParser.FIRST -> NullPrecedence.FIRST;
						case HqlParser.LAST -> NullPrecedence.LAST;
						default -> throw new UnsupportedOperationException(
								"Unrecognized null precedence: " + nullsPrecedenceContext.getText() );
					};
				}
			}
			searchOrders.add( creationContext.getNodeBuilder().search( attribute, sortOrder, nullPrecedence ) );
		}
		cteDefinition.search( getCteSearchClauseKind( ctx ), searchAttributeName, searchOrders );
	}

	private static CteSearchClauseKind getCteSearchClauseKind(HqlParser.SearchClauseContext ctx) {
		return ctx.BREADTH() != null ? CteSearchClauseKind.BREADTH_FIRST : CteSearchClauseKind.DEPTH_FIRST;
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
		final SqmQueryPart<?> firstQueryPart = (SqmQueryPart<?>) children.get( firstIndex ).accept( this );
		SqmQueryGroup<?> queryGroup =
				firstQueryPart instanceof SqmQueryGroup<?> sqmQueryGroup
						? sqmQueryGroup
						: new SqmQueryGroup<>( firstQueryPart );
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
		return switch ( token.getType() ) {
			case UNION -> all ? SetOperator.UNION_ALL : SetOperator.UNION;
			case INTERSECT -> all ? SetOperator.INTERSECT_ALL : SetOperator.INTERSECT;
			case EXCEPT -> all ? SetOperator.EXCEPT_ALL : SetOperator.EXCEPT;
			default -> throw new ParsingException( "Unrecognized set operator: " + token.getText() );
		};
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
						new SqmRoot<>( entityDescriptor, "_0", false, creationContext.getNodeBuilder() );
				processingStateStack.getCurrent().getPathRegistry().register( sqmRoot );
				fromClause.addRoot( sqmRoot );
			}
			return fromClause;
		}
	}

	private EntityDomainType<R> getResultEntity() {
		final JpaMetamodel jpaMetamodel = creationContext.getJpaMetamodel();
		if ( expectedResultEntity != null ) {
			final EntityDomainType<?> entityDescriptor = jpaMetamodel.findEntityType( expectedResultEntity );
			if ( entityDescriptor == null ) {
				throw new SemanticException( "Query has no 'from' clause, and the result type '"
						+ expectedResultEntity + "' is not an entity type", query );
			}
			//noinspection unchecked
			return (EntityDomainType<R>) entityDescriptor;
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

		for ( SqmRoot<?> sqmRoot : fromClause.getRoots() ) {
			if ( "this".equals( sqmRoot.getExplicitAlias() ) ) {
				// we found an entity with the alias 'this'
				// assigned explicitly, JPA says we should
				// infer the select list 'select this'
				final SqmSelectClause selectClause =
						new SqmSelectClause( false, 1, nodeBuilder );
				selectClause.addSelection( new SqmSelection<>( sqmRoot, "this", nodeBuilder) );
				return selectClause;
			}
		}

		if ( expectedResultType == null ) {
			if ( processingStateStack.getCurrent().getProcessingQuery() instanceof SqmSubQuery ) {
				// a subquery ... the following is a bit arbitrary
				final SqmSelectClause selectClause = new SqmSelectClause( false, nodeBuilder );
				fromClause.visitRoots( sqmRoot -> {
					selectClause.addSelection( new SqmSelection<>( sqmRoot, sqmRoot.getAlias(), nodeBuilder) );
					applyJoinsToInferredSelectClause( sqmRoot, selectClause );
				} );
				return selectClause;
			}
			else {
				// no result type was specified (and this isn't a subquery)
				// if there's a single root entity with no non-fetch joins,
				// we may safely assume the query returns that entity
				if ( fromClause.getNumberOfRoots() == 1 ) {
					final SqmRoot<?> sqmRoot = fromClause.getRoots().get(0);
					if ( sqmRoot.hasImplicitlySelectableJoin() ) {
						// the entity has joins, and doesn't explicitly have
						// the alias 'this', so the 'select' list cannot be
						// inferred
						throw new SemanticException( "Query has no 'select' clause, and joins, but no result type was given"
								+ " (pass an explicit result type to 'createQuery()')", query );
					}
					// exactly one root entity, and no joins - this includes
					// the case where JPA says the entity has an implicit alias
					// 'this', and that we should infer 'select this', but we
					// accept even the case where the entity has an explicit
					// alias, and infer 'select explicit_alias'
					final SqmSelectClause selectClause =
							new SqmSelectClause( false, 1, nodeBuilder );
					selectClause.addSelection( new SqmSelection<>( sqmRoot, sqmRoot.getAlias(), nodeBuilder) );
					return selectClause;
				}
				else {
					// there's more than one entity, and no entity is 'this',
					// therefore the 'select' list cannot be inferred
					throw new SemanticException( "Query has no 'select' clause, and multiple root entities, but no result type was given"
							+ " (pass an explicit result type to 'createQuery()')", query );
				}
			}
		}
		else {
			// we have an explicit result type, so we can use that to
			// help infer the 'select' list
			if ( creationContext.getJpaMetamodel().findEntityType( expectedResultType ) != null ) {
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
						final SqmSelectClause selectClause =
								new SqmSelectClause( false, 1, nodeBuilder );
						selectClause.addSelection( new SqmSelection<>( sqmRoot, sqmRoot.getAlias(), nodeBuilder) );
						return selectClause;
					}
				}
			}
			else {
				// the result type is not an entity class, and so
				// it must be some sort of object which packages
				// a multi-element projection list - let's return
				// all root entities and non-fetch joins, and see
				// later on if the result type can really hold them
				final SqmSelectClause selectClause = new SqmSelectClause( false, nodeBuilder );
				fromClause.visitRoots( sqmRoot -> {
					selectClause.addSelection( new SqmSelection<>( sqmRoot, sqmRoot.getAlias(), nodeBuilder) );
					applyJoinsToInferredSelectClause( sqmRoot, selectClause );
				} );
				return selectClause;
			}
		}
	}

	private void applyJoinsToInferredSelectClause(SqmFrom<?,?> sqm, SqmSelectClause selectClause) {
		sqm.visitSqmJoins( sqmJoin -> {
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
				new SqmSelectClause( ctx.DISTINCT() != null, creationContext.getNodeBuilder() );
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
			if ( sqmExpression instanceof SqmPath<?> sqmPath
					&& sqmPath.getReferencedPathSource() instanceof PluralPersistentAttribute ) {
				// for plural-attribute selections, use the element path as the selection
				//		- this is not strictly JPA compliant
				if ( creationOptions.useStrictJpaCompliance() ) {
					SqmTreeCreationLogger.LOGGER.debugf(
							"Raw selection of plural attribute not supported by JPA."
								+ " Use 'value(%s)' or 'key(%s)' to indicate what part of the collection to select",
							sqmPath.getAlias(),
							sqmPath.getAlias()
					);
				}

				final SqmPath<?> elementPath =
						sqmPath.resolvePathPart( CollectionPart.Nature.ELEMENT.getName(), true, this );
				processingStateStack.getCurrent().getPathRegistry().register( elementPath );
				return elementPath;
			}

			return sqmExpression;
		}
		return (SqmSelectableNode<?>) subCtx.accept( this );
	}

	@Override
	public SqmDynamicInstantiation<?> visitInstantiation(HqlParser.InstantiationContext ctx) {
		final SqmDynamicInstantiation<?> dynamicInstantiation = visitInstantiationTarget( ctx.instantiationTarget() );
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

	@Override
	public SqmDynamicInstantiation<?> visitInstantiationTarget(HqlParser.InstantiationTargetContext ctx) {
		if ( ctx.MAP() != null ) {
			return forMapInstantiation( mapJavaType, creationContext.getNodeBuilder() );
		}
		else if ( ctx.LIST() != null ) {
			return forListInstantiation( listJavaType, creationContext.getNodeBuilder() );
		}
		else {
			final HqlParser.SimplePathContext simplePath = ctx.simplePath();
			if ( simplePath == null ) {
				throw new SyntaxException( "Missing instantiation target type" );
			}
			final String className = instantiationClassName( simplePath );
			try {
				return forClassInstantiation( resolveInstantiationTargetType( className ), creationContext.getNodeBuilder() );
			}
			catch (ClassLoadingException e) {
				throw new SemanticException( "Could not resolve class '" + className + "' named for instantiation", query );
			}
		}
	}

	private String instantiationClassName(HqlParser.SimplePathContext ctx) {
		final String name = ctx.getText();
		return expectedResultTypeName != null && expectedResultTypeShortName.equals( name )
				? expectedResultTypeName
				: name;
	}

	private JavaType<?> resolveInstantiationTargetType(String className) {
		final String qualifiedName = creationContext.getJpaMetamodel().qualifyImportableName( className );
		final Class<?> targetJavaType = creationContext.classForName( qualifiedName );
		return creationContext.getTypeConfiguration().getJavaTypeRegistry().resolveDescriptor( targetJavaType );
	}

	@Override
	public SqmDynamicInstantiationArgument<?> visitInstantiationArgument(HqlParser.InstantiationArgumentContext ctx) {
		final HqlParser.VariableContext variable = ctx.variable();
		final String alias = variable == null ? null : extractAlias( variable );
		final SqmSelectableNode<?> argExpression =
				(SqmSelectableNode<?>) ctx.instantiationArgumentExpression().accept( this );
		final SqmDynamicInstantiationArgument<?> argument =
				new SqmDynamicInstantiationArgument<>( argExpression, alias, creationContext.getNodeBuilder() );
		if ( !(argExpression instanceof SqmDynamicInstantiation) ) {
			processingStateStack.getCurrent().getPathRegistry().register( argument );
		}
		return argument;
	}

	@Override
	public SqmPath<?> visitJpaSelectObjectSyntax(HqlParser.JpaSelectObjectSyntaxContext ctx) {
		final String alias = ctx.getChild( 2 ).getText();
		final SqmFrom<?, ?> sqmFromByAlias =
				processingStateStack.getCurrent().getPathRegistry()
						.findFromByAlias( alias, true );
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
		if ( processingQuery instanceof SqmInsertSelectStatement<?> insertSelectStatement ) {
			queryPart = insertSelectStatement.getSelectQueryPart();
		}
		else if ( processingQuery instanceof SqmSelectQuery<?> selectQuery ) {
			queryPart = selectQuery.getQueryPart();
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

			return new SqmAliasedNodeRef( position, integerDomainType.resolveExpressible( nodeBuilder ), nodeBuilder);
		}
		else if ( child instanceof HqlParser.IdentifierContext identifierContext ) {
			final String identifierText = visitIdentifier( identifierContext );
			if ( queryPart instanceof SqmQueryGroup<?> ) {
				// If the current query part is a query group, check if the text matches
				// an attribute name of one of the selected SqmFrom elements or the path source name of a SqmPath
				SqmFrom<?, ?> found = null;
				int sqmPosition = 0;
				final List<SqmSelection<?>> selections =
						queryPart.getFirstQuerySpec().getSelectClause().getSelections();
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
					if ( selectableNode instanceof SqmFrom<?, ?> fromElement ) {
						if ( fromElement.getReferencedPathSource().findSubPathSource( identifierText ) != null ) {
							if ( sqmPosition != 0 ) {
								throw new IllegalStateException(
										"Multiple from elements expose unqualified attribute: " + identifierText );
							}
							found = fromElement;
							sqmPosition = i + 1;
						}
					}
					else if ( selectableNode instanceof SqmPath<?> path ) {
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
					return new SqmAliasedNodeRef( correspondingPosition, integerDomainType.resolveExpressible( nodeBuilder ), nodeBuilder );
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
			if ( parseTree instanceof HqlParser.SortSpecificationContext sortSpecificationContext ) {
				orderByClause.addSortSpecification( visitSortSpecification(
						sortSpecificationContext,
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
		final SqmExpression<?> sortExpression = visitSortExpression( ctx.sortExpression(), allowPositionalOrAliases );
		if ( sortExpression == null ) {
			throw new SemanticException( "Could not resolve sort expression: '" + ctx.sortExpression().getText() + "'",
					query );
		}
		if ( sortExpression instanceof SqmLiteral || sortExpression instanceof SqmParameter ) {
			HqlLogging.QUERY_LOGGER.debugf( "Questionable sorting by constant value : %s", sortExpression );
		}
		return new SqmSortSpecification( sortExpression, sortOrder( ctx ), nullPrecedence( ctx ) );
	}

	private static SortDirection sortOrder(HqlParser.SortSpecificationContext ctx) {
		return ctx.sortDirection() == null || ctx.sortDirection().DESC() == null
				? SortDirection.ASCENDING
				: SortDirection.DESCENDING;
	}

	private static NullPrecedence nullPrecedence(HqlParser.SortSpecificationContext ctx) {
		if ( ctx.nullsPrecedence() == null ) {
			return NullPrecedence.NONE;
		}
		else {
			if ( ctx.nullsPrecedence().FIRST() != null ) {
				return NullPrecedence.FIRST;
			}
			else if ( ctx.nullsPrecedence().LAST() != null ) {
				return NullPrecedence.LAST;
			}
			else {
				throw new ParsingException( "Unrecognized null precedence" );
			}
		}
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
		if ( processingQuery instanceof SqmInsertSelectStatement<?> insertSelectStatement ) {
			return insertSelectStatement.getSelectQueryPart().getLastQuerySpec();
		}
		else if ( processingQuery instanceof SqmSelectQuery<?> selectQuery ) {
			return selectQuery.getQueryPart().getLastQuerySpec();
		}
		else {
			throw new AssertionFailure( "Unrecognized SqmQuery type" );
		}
	}

	private <X> void setCurrentQueryPart(SqmQueryPart<X> queryPart) {
		@SuppressWarnings("unchecked")
		final SqmQuery<X> processingQuery = (SqmQuery<X>) processingStateStack.getCurrent().getProcessingQuery();
		if ( processingQuery instanceof SqmInsertSelectStatement<X> insertSelectStatement ) {
			insertSelectStatement.setSelectQueryPart( queryPart );
		}
		else if ( processingQuery instanceof AbstractSqmSelectQuery<X> selectQuery ) {
			selectQuery.setQueryPart( queryPart );
		}
		else {
			throw new AssertionFailure( "Unrecognized SqmQuery type" );
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
		return part instanceof DomainPathPart domainPathPart ? domainPathPart.getSqmExpression() : part;
	}

	@Override
	public Object visitGeneralPathExpression(HqlParser.GeneralPathExpressionContext ctx) {
		final SemanticPathPart part = visitGeneralPathFragment( ctx.generalPathFragment() );
		return part instanceof DomainPathPart domainPathPart ? domainPathPart.getSqmExpression() : part;
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
		if ( ctx.getChild( 0 ) instanceof TerminalNode firstChild ) {
			return switch ( firstChild.getSymbol().getType() ) {
				case HqlParser.INTEGER_LITERAL -> integerLiteral( ctx.getChild( 0 ).getText() );
				case HqlParser.FLOAT_LITERAL -> floatLiteral( ctx.getChild( 0 ).getText() );
				case HqlParser.DOUBLE_LITERAL -> doubleLiteral( ctx.getChild( 0 ).getText() );
				default -> throw new UnsupportedOperationException( "Unsupported literal: " + ctx.getChild( 0 ).getText() );
			};
		}
		return (SqmExpression<?>) ctx.getChild( 0 ).accept( this );
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
		final EntityDomainType<?> entityReference = getJpaMetamodel().getHqlEntityReference( entityName );
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
			if ( parseTree instanceof HqlParser.CrossJoinContext crossJoinContext ) {
				consumeCrossJoin( crossJoinContext, sqmRoot );
			}
			else if ( parseTree instanceof HqlParser.JoinContext joinContext ) {
				consumeJoin( joinContext, sqmRoot );
			}
			else if ( parseTree instanceof HqlParser.JpaCollectionJoinContext jpaCollectionJoinContext ) {
				consumeJpaCollectionJoin( jpaCollectionJoinContext, sqmRoot );
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
			if ( correlation instanceof SqmCorrelation<?, ?> sqmCorrelation ) {
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
				return sqmCorrelation.getCorrelatedRoot();
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
		return state.getProcessingQuery() instanceof SqmCteContainer container
				? container.getCteStatement( n )
				: null;
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
	public SqmRoot<?> visitRootFunction(HqlParser.RootFunctionContext ctx) {
		if ( getCreationOptions().useStrictJpaCompliance() ) {
			throw new StrictJpaComplianceViolation(
					"The JPA specification does not support functions in the from clause. " +
							"Please disable the JPA query compliance if you want to use this feature.",
					StrictJpaComplianceViolation.Type.FROM_FUNCTION
			);
		}

		final SqmSetReturningFunction<?> function = (SqmSetReturningFunction<?>) ctx.setReturningFunction().accept( this );
		final String alias = extractAlias( ctx.variable() );
		final SqmFunctionRoot<?> sqmRoot = new SqmFunctionRoot<>( function, alias );
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

	@Override
	public final SqmCrossJoin<?> visitCrossJoin(HqlParser.CrossJoinContext ctx) {
		throw new UnsupportedOperationException( "Unexpected call to #visitCrossJoin, see #consumeCrossJoin" );
	}

	protected <T> void consumeCrossJoin(HqlParser.CrossJoinContext parserJoin, SqmRoot<T> sqmRoot) {
		final String name = getEntityName( parserJoin.entityName() );

		SqmTreeCreationLogger.LOGGER.debugf( "Handling root path - %s", name );

		final EntityDomainType<T> entityDescriptor = getJpaMetamodel().resolveHqlEntityReference( name );

		if ( entityDescriptor instanceof SqmPolymorphicRootDescriptor ) {
			throw new SemanticException( "Unmapped polymorphic reference cannot be used as a target of 'cross join'",
					query );
		}
		final SqmCrossJoin<T> join = new SqmCrossJoin<>(
				(SqmEntityDomainType<T>) entityDescriptor,
				extractAlias( parserJoin.variable() ),
				sqmRoot
		);

		processingStateStack.getCurrent().getPathRegistry().register( join );

		// CROSS joins are always added to the root
		sqmRoot.addSqmJoin( join );
	}

	private JpaMetamodel getJpaMetamodel() {
		return getCreationContext().getJpaMetamodel();
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
			final SqmJoin<X, ?> join = getJoin( sqmRoot, joinType, qualifiedJoinTargetContext, alias, fetch );
			final HqlParser.JoinRestrictionContext joinRestrictionContext = parserJoin.joinRestriction();
			if ( join instanceof SqmEntityJoin<?,?> || join instanceof SqmDerivedJoin<?> || join instanceof SqmCteJoin<?> ) {
				sqmRoot.addSqmJoin( join );
			}
			else if ( join instanceof SqmAttributeJoin<?, ?> attributeJoin ) {
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
		if ( joinTargetContext instanceof HqlParser.JoinPathContext joinPathContext ) {
			return joinPathContext.variable();
		}
		else if ( joinTargetContext instanceof HqlParser.JoinSubqueryContext joinSubqueryContext ) {
			return joinSubqueryContext.variable();
		}
		else if ( joinTargetContext instanceof HqlParser.JoinFunctionContext joinFunctionContext ) {
			return joinFunctionContext.variable();
		}
		else {
			throw new ParsingException( "unexpected join type" );
		}
	}

	@SuppressWarnings("unchecked")
	private <X> SqmJoin<X, ?> getJoin(
			SqmRoot<X> sqmRoot,
			SqmJoinType joinType,
			HqlParser.JoinTargetContext joinTargetContext,
			String alias,
			boolean fetch) {
		if ( joinTargetContext instanceof HqlParser.JoinPathContext joinPathContext ) {
			return (SqmJoin<X, ?>) joinPathContext.path().accept( this );
		}
		else if ( joinTargetContext instanceof HqlParser.JoinSubqueryContext joinSubqueryContext ) {
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

			final boolean lateral = joinSubqueryContext.LATERAL() != null;
			final DotIdentifierConsumer identifierConsumer = dotIdentifierConsumerStack.pop();
			final SqmSubQuery<X> subQuery = (SqmSubQuery<X>) joinSubqueryContext.subquery().accept( this );
			dotIdentifierConsumerStack.push( identifierConsumer );
			final SqmJoin<X, ?> join = new SqmDerivedJoin<>( subQuery, alias, joinType, lateral, sqmRoot );
			processingStateStack.getCurrent().getPathRegistry().register( join );
			return join;
		}
		else if ( joinTargetContext instanceof HqlParser.JoinFunctionContext joinFunctionContext ) {
			if ( fetch ) {
				throw new SemanticException( "The 'from' clause of a set returning function has a 'fetch' join", query );
			}
			if ( getCreationOptions().useStrictJpaCompliance() ) {
				throw new StrictJpaComplianceViolation(
						"The JPA specification does not support functions in the from clause. " +
								"Please disable the JPA query compliance if you want to use this feature.",
						StrictJpaComplianceViolation.Type.FROM_FUNCTION
				);
			}

			final boolean lateral = joinFunctionContext.LATERAL() != null;
			final DotIdentifierConsumer identifierConsumer = dotIdentifierConsumerStack.pop();
			final SqmSetReturningFunction<?> function = (SqmSetReturningFunction<?>) joinFunctionContext.setReturningFunction().accept( this );
			dotIdentifierConsumerStack.push( identifierConsumer );
			final SqmFunctionJoin<?> join = new SqmFunctionJoin<>(
					function,
					alias,
					joinType,
					lateral,
					(SqmRoot<Object>) sqmRoot
			);
			processingStateStack.getCurrent().getPathRegistry().register( join );
			sqmRoot.addSqmJoin( (SqmJoin<X, ?>) join );
			return (SqmJoin<X, ?>) join;
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
			return switch ( firstChild.getSymbol().getType() ) {
				case HqlParser.FULL -> SqmJoinType.FULL;
				case HqlParser.RIGHT -> SqmJoinType.RIGHT;
				// For some reason, we also support `outer join` syntax..
				case HqlParser.OUTER, HqlParser.LEFT -> SqmJoinType.LEFT;
				default -> SqmJoinType.INNER;
			};
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
		if ( lhs instanceof SqmJunctionPredicate junction ) {
			if ( junction.getOperator() == operator ) {
				junction.getPredicates().add( rhs );
				return junction;
			}
		}
		if ( rhs instanceof SqmJunctionPredicate junction ) {
			if ( junction.getOperator() == operator ) {
				junction.getPredicates().add( 0, lhs );
				return junction;
			}
		}
		return new SqmJunctionPredicate( operator, lhs, rhs, creationContext.getNodeBuilder() );
	}

	@Override
	public SqmPredicate visitNegatedPredicate(HqlParser.NegatedPredicateContext ctx) {
		final SqmPredicate predicate = (SqmPredicate) ctx.predicate().accept( this );
		if ( predicate instanceof SqmNegatablePredicate negatablePredicate ) {
			negatablePredicate.negate();
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
		if ( expression instanceof SqmPluralValuedSimplePath<?> pluralValuedSimplePath ) {
			return new SqmEmptinessPredicate(
					pluralValuedSimplePath,
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
		return switch ( firstToken.getSymbol().getType() ) {
			case HqlLexer.EQUAL -> ComparisonOperator.EQUAL;
			case HqlLexer.NOT_EQUAL -> ComparisonOperator.NOT_EQUAL;
			case HqlLexer.LESS -> ComparisonOperator.LESS_THAN;
			case HqlLexer.LESS_EQUAL -> ComparisonOperator.LESS_THAN_OR_EQUAL;
			case HqlLexer.GREATER -> ComparisonOperator.GREATER_THAN;
			case HqlLexer.GREATER_EQUAL -> ComparisonOperator.GREATER_THAN_OR_EQUAL;
			default -> throw new ParsingException( "Unrecognized comparison operator" );
		};
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
			if ( l instanceof AnyDiscriminatorSqmPath<?> anyDiscriminatorPath && r instanceof SqmLiteralEntityType ) {
				left = l;
				right = createDiscriminatorValue( anyDiscriminatorPath, rightExpressionContext );
			}
			else if ( r instanceof AnyDiscriminatorSqmPath<?> anyDiscriminatorPath && l instanceof SqmLiteralEntityType ) {
				left = createDiscriminatorValue( anyDiscriminatorPath, leftExpressionContext );
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
				anyDiscriminatorTypeSqmPath.getExpressible().getPathType(),
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

				if ( ctx instanceof HqlParser.SimplePathContext simplePathContext ) {
					final int size = simplePathContext.simplePathElement().size();
					return size == 0
							? simplePathContext.getText()
							: simplePathContext.simplePathElement( size - 1 ).identifier().getText();
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
	public SqmExpression<?> visitJsonValueFunction(HqlParser.JsonValueFunctionContext ctx) {
		checkJsonFunctionsEnabled( ctx );
		final SqmExpression<?> jsonDocument = (SqmExpression<?>) ctx.expression( 0 ).accept( this );
		final SqmExpression<?> jsonPath = (SqmExpression<?>) ctx.expression( 1 ).accept( this );
		final HqlParser.JsonValueReturningClauseContext returningClause = ctx.jsonValueReturningClause();
		final SqmCastTarget<?> castTarget = returningClause == null
				? null
				: (SqmCastTarget<?>) returningClause.castTarget().accept( this );

		final SqmJsonValueExpression<?> jsonValue = (SqmJsonValueExpression<?>) getFunctionDescriptor( "json_value" ).generateSqmExpression(
				castTarget == null
						? asList( jsonDocument, jsonPath )
						: asList( jsonDocument, jsonPath, castTarget ),
				null,
				creationContext.getQueryEngine()
		);
		visitJsonValueOnErrorOrEmptyClause( jsonValue, ctx.jsonValueOnErrorOrEmptyClause() );
		final HqlParser.JsonPassingClauseContext passingClause = ctx.jsonPassingClause();
		if ( passingClause != null ) {
			final List<HqlParser.ExpressionOrPredicateContext> expressionContexts = passingClause.expressionOrPredicate();
			final List<HqlParser.IdentifierContext> identifierContexts = passingClause.identifier();
			for ( int i = 0; i < expressionContexts.size(); i++ ) {
				jsonValue.passing(
						visitIdentifier( identifierContexts.get( i ) ),
						(SqmExpression<?>) expressionContexts.get( i ).accept( this )
				);
			}
		}
		return jsonValue;
	}

	private void visitJsonValueOnErrorOrEmptyClause(JpaJsonValueNode<?> jsonValue, List<HqlParser.JsonValueOnErrorOrEmptyClauseContext> errorOrEmptyClauseContexts) {
		for ( HqlParser.JsonValueOnErrorOrEmptyClauseContext subCtx : errorOrEmptyClauseContexts ) {
			final TerminalNode firstToken = (TerminalNode) subCtx.getChild( 0 );
			final TerminalNode lastToken = (TerminalNode) subCtx.getChild( subCtx.getChildCount() - 1 );
			if ( lastToken.getSymbol().getType() == HqlParser.ERROR ) {
				switch ( firstToken.getSymbol().getType() ) {
					case HqlParser.NULL -> jsonValue.nullOnError();
					case HqlParser.ERROR -> jsonValue.errorOnError();
					case HqlParser.DEFAULT ->
							jsonValue.defaultOnError( (SqmExpression<?>) subCtx.expression().accept( this ) );
				}
			}
			else {
				switch ( firstToken.getSymbol().getType() ) {
					case HqlParser.NULL -> jsonValue.nullOnEmpty();
					case HqlParser.ERROR -> jsonValue.errorOnEmpty();
					case HqlParser.DEFAULT ->
							jsonValue.defaultOnEmpty( (SqmExpression<?>) subCtx.expression().accept( this ) );
				}
			}
		}
	}

	@Override
	public SqmExpression<?> visitJsonQueryFunction(HqlParser.JsonQueryFunctionContext ctx) {
		checkJsonFunctionsEnabled( ctx );
		final SqmExpression<?> jsonDocument = (SqmExpression<?>) ctx.expression( 0 ).accept( this );
		final SqmExpression<?> jsonPath = (SqmExpression<?>) ctx.expression( 1 ).accept( this );
		final SqmJsonQueryExpression jsonQuery = (SqmJsonQueryExpression) getFunctionDescriptor( "json_query" ).<String>generateSqmExpression(
				asList( jsonDocument, jsonPath ),
				null,
				creationContext.getQueryEngine()
		);
		visitJsonQueryWrapperClause( jsonQuery, ctx.jsonQueryWrapperClause() );
		visitJsonQueryOnErrorOrEmptyClause( jsonQuery, ctx.jsonQueryOnErrorOrEmptyClause() );
		final HqlParser.JsonPassingClauseContext passingClause = ctx.jsonPassingClause();
		if ( passingClause != null ) {
			final List<HqlParser.ExpressionOrPredicateContext> expressionContexts = passingClause.expressionOrPredicate();
			final List<HqlParser.IdentifierContext> identifierContexts = passingClause.identifier();
			for ( int i = 0; i < expressionContexts.size(); i++ ) {
				jsonQuery.passing(
						visitIdentifier( identifierContexts.get( i ) ),
						(SqmExpression<?>) expressionContexts.get( i ).accept( this )
				);
			}
		}
		return jsonQuery;
	}

	private static void visitJsonQueryWrapperClause(JpaJsonQueryNode jsonQuery, HqlParser.JsonQueryWrapperClauseContext wrapperClause) {
		if ( wrapperClause != null ) {
			final TerminalNode firstToken = (TerminalNode) wrapperClause.getChild( 0 );
			if ( firstToken.getSymbol().getType() == HqlParser.WITH ) {
				final TerminalNode secondToken = (TerminalNode) wrapperClause.getChild( 1 );
				if ( wrapperClause.getChildCount() > 2 && secondToken.getSymbol().getType() == HqlParser.CONDITIONAL ) {
					jsonQuery.withConditionalWrapper();
				}
				else {
					jsonQuery.withWrapper();
				}
			}
			else {
				jsonQuery.withoutWrapper();
			}
		}
	}

	private static void visitJsonQueryOnErrorOrEmptyClause(JpaJsonQueryNode jsonQuery, List<HqlParser.JsonQueryOnErrorOrEmptyClauseContext> jsonQueryOnErrorOrEmptyClauseContexts) {
		for ( HqlParser.JsonQueryOnErrorOrEmptyClauseContext subCtx : jsonQueryOnErrorOrEmptyClauseContexts ) {
			final TerminalNode firstToken = (TerminalNode) subCtx.getChild( 0 );
			final TerminalNode lastToken = (TerminalNode) subCtx.getChild( subCtx.getChildCount() - 1 );
			if ( lastToken.getSymbol().getType() == HqlParser.ERROR ) {
				switch ( firstToken.getSymbol().getType() ) {
					case HqlParser.NULL -> jsonQuery.nullOnError();
					case HqlParser.ERROR -> jsonQuery.errorOnError();
					case HqlParser.EMPTY -> {
						final TerminalNode secondToken = (TerminalNode) subCtx.getChild( 1 );
						if ( secondToken.getSymbol().getType() == HqlParser.OBJECT ) {
							jsonQuery.emptyObjectOnError();
						}
						else {
							jsonQuery.emptyArrayOnError();
						}
					}
				}
			}
			else {
				switch ( firstToken.getSymbol().getType() ) {
					case HqlParser.NULL -> jsonQuery.nullOnEmpty();
					case HqlParser.ERROR -> jsonQuery.errorOnEmpty();
					case HqlParser.EMPTY -> {
						final TerminalNode secondToken = (TerminalNode) subCtx.getChild( 1 );
						if ( secondToken.getSymbol().getType() == HqlParser.OBJECT ) {
							jsonQuery.emptyObjectOnEmpty();
						}
						else {
							jsonQuery.emptyArrayOnEmpty();
						}
					}
				}
			}
		}
	}

	@Override
	public SqmExpression<?> visitJsonExistsFunction(HqlParser.JsonExistsFunctionContext ctx) {
		checkJsonFunctionsEnabled( ctx );
		final SqmExpression<?> jsonDocument = (SqmExpression<?>) ctx.expression( 0 ).accept( this );
		final SqmExpression<?> jsonPath = (SqmExpression<?>) ctx.expression( 1 ).accept( this );

		final SqmJsonExistsExpression jsonExists = (SqmJsonExistsExpression) getFunctionDescriptor( "json_exists" ).<Boolean>generateSqmExpression(
				asList( jsonDocument, jsonPath ),
				null,
				creationContext.getQueryEngine()
		);
		final HqlParser.JsonExistsOnErrorClauseContext subCtx = ctx.jsonExistsOnErrorClause();
		if ( subCtx != null ) {
			final TerminalNode firstToken = (TerminalNode) subCtx.getChild( 0 );
			switch ( firstToken.getSymbol().getType() ) {
				case HqlParser.ERROR -> jsonExists.errorOnError();
				case HqlParser.TRUE -> jsonExists.trueOnError();
				case HqlParser.FALSE -> jsonExists.falseOnError();
			}
		}
		final HqlParser.JsonPassingClauseContext passingClause = ctx.jsonPassingClause();
		if ( passingClause != null ) {
			final List<HqlParser.ExpressionOrPredicateContext> expressionContexts = passingClause.expressionOrPredicate();
			final List<HqlParser.IdentifierContext> identifierContexts = passingClause.identifier();
			for ( int i = 0; i < expressionContexts.size(); i++ ) {
				jsonExists.passing(
						visitIdentifier( identifierContexts.get( i ) ),
						(SqmExpression<?>) expressionContexts.get( i ).accept( this )
				);
			}
		}
		return jsonExists;
	}

	@Override
	public SqmExpression<?> visitJsonArrayFunction(HqlParser.JsonArrayFunctionContext ctx) {
		checkJsonFunctionsEnabled( ctx );
		final HqlParser.JsonNullClauseContext subCtx = ctx.jsonNullClause();
		final List<HqlParser.ExpressionOrPredicateContext> argumentContexts = ctx.expressionOrPredicate();
		int count = argumentContexts.size();
		final List<SqmTypedNode<?>> arguments = new ArrayList<>( count + (subCtx == null ? 0 : 1 ) );
		for ( int i = 0; i < count; i++ ) {
			arguments.add( (SqmTypedNode<?>) argumentContexts.get(i).accept( this ) );
		}
		if ( subCtx != null ) {
			final TerminalNode firstToken = (TerminalNode) subCtx.getChild( 0 );
			arguments.add(
					firstToken.getSymbol().getType() == HqlParser.ABSENT
							? SqmJsonNullBehavior.ABSENT
							: SqmJsonNullBehavior.NULL
			);
		}
		return getFunctionDescriptor( "json_array" ).generateSqmExpression(
				arguments,
				null,
				creationContext.getQueryEngine()
		);
	}

	@Override
	public SqmExpression<?> visitJsonObjectFunction(HqlParser.JsonObjectFunctionContext ctx) {
		checkJsonFunctionsEnabled( ctx );
		final HqlParser.JsonObjectFunctionEntriesContext entries = ctx.jsonObjectFunctionEntries();
		final List<SqmTypedNode<?>> arguments;
		if ( entries == null ) {
			arguments = Collections.emptyList();
		}
		else {
			final HqlParser.JsonNullClauseContext subCtx = ctx.jsonNullClause();
			final List<HqlParser.ExpressionOrPredicateContext> argumentContexts = entries.expressionOrPredicate();
			int count = argumentContexts.size();
			arguments = new ArrayList<>( count + ( subCtx == null ? 0 : 1 ) );
			for ( int i = 0; i < count; i++ ) {
				arguments.add( (SqmTypedNode<?>) argumentContexts.get( i ).accept( this ) );
			}
			if ( subCtx != null ) {
				final TerminalNode firstToken = (TerminalNode) subCtx.getChild( 0 );
				arguments.add(
						firstToken.getSymbol().getType() == HqlParser.ABSENT
								? SqmJsonNullBehavior.ABSENT
								: SqmJsonNullBehavior.NULL
				);
			}
		}
		return getFunctionDescriptor( "json_object" ).generateSqmExpression(
				arguments,
				null,
				creationContext.getQueryEngine()
		);
	}

	@Override
	public Object visitJsonArrayAggFunction(HqlParser.JsonArrayAggFunctionContext ctx) {
		checkJsonFunctionsEnabled( ctx );
		final HqlParser.JsonNullClauseContext jsonNullClauseContext = ctx.jsonNullClause();
		final ArrayList<SqmTypedNode<?>> arguments = new ArrayList<>( jsonNullClauseContext == null ? 1 : 2 );
		arguments.add( (SqmTypedNode<?>) ctx.expressionOrPredicate().accept( this ) );
		if ( jsonNullClauseContext != null ) {
			final TerminalNode firstToken = (TerminalNode) jsonNullClauseContext.getChild( 0 );
			arguments.add(
					firstToken.getSymbol().getType() == HqlParser.ABSENT
							? SqmJsonNullBehavior.ABSENT
							: SqmJsonNullBehavior.NULL
			);
		}
		return getFunctionDescriptor( "json_arrayagg" ).generateOrderedSetAggregateSqmExpression(
				arguments,
				getFilterExpression( ctx ),
				ctx.orderByClause() == null
						? null
						: visitOrderByClause( ctx.orderByClause(), false ),
				null,
				creationContext.getQueryEngine()
		);
	}

	@Override
	public Object visitJsonObjectAggFunction(HqlParser.JsonObjectAggFunctionContext ctx) {
		checkJsonFunctionsEnabled( ctx );
		final HqlParser.JsonNullClauseContext jsonNullClauseContext = ctx.jsonNullClause();
		final HqlParser.JsonUniqueKeysClauseContext jsonUniqueKeysClauseContext = ctx.jsonUniqueKeysClause();
		final ArrayList<SqmTypedNode<?>> arguments = new ArrayList<>( 4 );
		for ( HqlParser.ExpressionOrPredicateContext subCtx : ctx.expressionOrPredicate() ) {
			arguments.add( (SqmTypedNode<?>) subCtx.accept( this ) );
		}
		if ( jsonNullClauseContext != null ) {
			final TerminalNode firstToken = (TerminalNode) jsonNullClauseContext.getChild( 0 );
			arguments.add(
					firstToken.getSymbol().getType() == HqlParser.ABSENT
							? SqmJsonNullBehavior.ABSENT
							: SqmJsonNullBehavior.NULL
			);
		}
		if ( jsonUniqueKeysClauseContext != null ) {
			final TerminalNode firstToken = (TerminalNode) jsonUniqueKeysClauseContext.getChild( 0 );
			arguments.add(
					firstToken.getSymbol().getType() == HqlParser.WITH
							? SqmJsonObjectAggUniqueKeysBehavior.WITH
							: SqmJsonObjectAggUniqueKeysBehavior.WITHOUT
			);
		}
		return getFunctionDescriptor( "json_objectagg" ).generateAggregateSqmExpression(
				arguments,
				getFilterExpression( ctx ),
				null,
				creationContext.getQueryEngine()
		);
	}

	@Override
	public Object visitJsonTableFunction(HqlParser.JsonTableFunctionContext ctx) {
		checkJsonFunctionsEnabled( ctx );
		final List<HqlParser.ExpressionContext> argumentsContexts = ctx.expression();
		final SqmExpression<?> jsonDocument = (SqmExpression<?>) argumentsContexts.get( 0 ).accept( this );
		final SqmJsonTableFunction<?> jsonTable;
		if ( argumentsContexts.size() == 1 ) {
			jsonTable = creationContext.getNodeBuilder().jsonTable( jsonDocument );
		}
		else {
			//noinspection unchecked
			final SqmExpression<String> jsonPath = (SqmExpression<String>) argumentsContexts.get( 1 ).accept( this );
			jsonTable = creationContext.getNodeBuilder().jsonTable( jsonDocument, jsonPath );
		}
		final HqlParser.JsonPassingClauseContext passingClauseContext = ctx.jsonPassingClause();
		if ( passingClauseContext != null ) {
			final List<HqlParser.ExpressionOrPredicateContext> expressionContexts = passingClauseContext.expressionOrPredicate();
			final List<HqlParser.IdentifierContext> identifierContexts = passingClauseContext.identifier();
			for ( int i = 0; i < expressionContexts.size(); i++ ) {
				jsonTable.passing(
						visitIdentifier( identifierContexts.get( i ) ),
						(SqmExpression<?>) expressionContexts.get( i ).accept( this )
				);
			}
		}
		visitColumns( jsonTable, ctx.jsonTableColumnsClause().jsonTableColumns().jsonTableColumn() );

		final HqlParser.JsonTableErrorClauseContext errorClauseContext = ctx.jsonTableErrorClause();
		if ( errorClauseContext != null ) {
			if ( ( (TerminalNode) errorClauseContext.getChild( 0 ) ).getSymbol().getType() == HqlParser.ERROR ) {
				jsonTable.errorOnError();
			}
			else {
				jsonTable.nullOnError();
			}
		}
		return jsonTable;
	}

	private void visitColumns(JpaJsonTableColumnsNode columnsNode, List<HqlParser.JsonTableColumnContext> columnContexts) {
		for ( HqlParser.JsonTableColumnContext columnContext : columnContexts ) {
			if ( columnContext instanceof HqlParser.JsonTableQueryColumnContext queryContext ) {
				final String attributeName = visitIdentifier( queryContext.identifier() );
				final TerminalNode jsonPath = queryContext.STRING_LITERAL();
				final JpaJsonQueryNode queryNode = jsonPath == null
						? columnsNode.queryColumn( attributeName )
						: columnsNode.queryColumn( attributeName, unquoteStringLiteral( jsonPath.getText() ) );
				visitJsonQueryOnErrorOrEmptyClause( queryNode, queryContext.jsonQueryOnErrorOrEmptyClause() );
			}
			else if ( columnContext instanceof HqlParser.JsonTableValueColumnContext valueContext ) {
				final String attributeName = visitIdentifier( valueContext.identifier() );
				final SqmCastTarget<?> sqmCastTarget = visitCastTarget( valueContext.castTarget() );
				final TerminalNode jsonPath = valueContext.STRING_LITERAL();
				final JpaJsonValueNode<?> valueNode =
						jsonPath == null
								? columnsNode.valueColumn( attributeName, sqmCastTarget )
								: columnsNode.valueColumn( attributeName, sqmCastTarget,
										unquoteStringLiteral( jsonPath.getText() ) );
				visitJsonValueOnErrorOrEmptyClause( valueNode, valueContext.jsonValueOnErrorOrEmptyClause() );
			}
			else if ( columnContext instanceof HqlParser.JsonTableOrdinalityColumnContext ordinalityContext ) {
				columnsNode.ordinalityColumn( visitIdentifier( ordinalityContext.identifier() ) );
			}
			else if ( columnContext instanceof HqlParser.JsonTableExistsColumnContext existsContext ) {
				final String attributeName = visitIdentifier( existsContext.identifier() );
				final TerminalNode jsonPath = existsContext.STRING_LITERAL();
				final JpaJsonExistsNode existsNode = jsonPath == null
						? columnsNode.existsColumn( attributeName )
						: columnsNode.existsColumn( attributeName, unquoteStringLiteral( jsonPath.getText() ) );
				final HqlParser.JsonExistsOnErrorClauseContext errorClauseContext = existsContext.jsonExistsOnErrorClause();
				if ( errorClauseContext != null ) {
					switch ( ( (TerminalNode) errorClauseContext.getChild( 0 ) ).getSymbol().getType() ) {
						case HqlParser.ERROR -> existsNode.errorOnError();
						case HqlParser.TRUE -> existsNode.trueOnError();
						case HqlParser.FALSE -> existsNode.falseOnError();
					}
				}
			}
			else {
				final HqlParser.JsonTableNestedColumnContext nestedColumnContext = (HqlParser.JsonTableNestedColumnContext) columnContext;
				visitColumns(
						columnsNode.nested( unquoteStringLiteral( nestedColumnContext.STRING_LITERAL().getText() ) ),
						nestedColumnContext.jsonTableColumnsClause().jsonTableColumns().jsonTableColumn()
				);
			}
		}
	}

	private void checkJsonFunctionsEnabled(ParserRuleContext ctx) {
		if ( !creationOptions.isJsonFunctionsEnabled() ) {
			throw new SemanticException(
					"Can't use function '" + ctx.children.get( 0 ).getText() +
							"', because tech preview JSON functions are not enabled. To enable, set the '" + QuerySettings.JSON_FUNCTIONS_ENABLED + "' setting to 'true'.",
					query
			);
		}
	}

	@Override
	public SqmExpression<?> visitXmlelementFunction(HqlParser.XmlelementFunctionContext ctx) {
		checkXmlFunctionsEnabled( ctx );
		final String elementName = visitIdentifier( ctx.identifier() );
		final SqmXmlElementExpression xmlelement = creationContext.getNodeBuilder().xmlelement( elementName );
		final HqlParser.XmlattributesFunctionContext attributeCtx = ctx.xmlattributesFunction();
		if ( attributeCtx != null ) {
			final List<HqlParser.ExpressionOrPredicateContext> expressions = attributeCtx.expressionOrPredicate();
			final List<HqlParser.IdentifierContext> attributeNames = attributeCtx.identifier();
			for ( int i = 0; i < expressions.size(); i++ ) {
				xmlelement.attribute(
						visitIdentifier( attributeNames.get( i ) ),
						(Expression<?>) expressions.get( i ).accept( this )
				);
			}
		}
		xmlelement.content( visitExpressions( ctx ) );
		return xmlelement;
	}

	@Override
	public SqmExpression<?> visitXmlforestFunction(HqlParser.XmlforestFunctionContext ctx) {
		checkXmlFunctionsEnabled( ctx );
		final ArrayList<SqmExpression<?>> elementExpressions = new ArrayList<>( ctx.getChildCount() >> 1 );
		for ( int i = 2; i < ctx.getChildCount(); i++ ) {
			if ( ctx.getChild( i ) instanceof HqlParser.ExpressionOrPredicateContext exprCtx ) {
				final SqmExpression<?> expression = (SqmExpression<?>) exprCtx.accept( this );
				if ( i + 2 < ctx.getChildCount() && ctx.getChild( i + 2 ) instanceof HqlParser.IdentifierContext identifierContext ) {
					final String name = visitIdentifier( identifierContext );
					elementExpressions.add( new SqmNamedExpression<>( expression, name ) );
					i += 2;
				}
				else {
					if ( !( expression instanceof SqmPath<?> path )
							|| !( path.getModel() instanceof PersistentAttribute<?, ?> attribute ) ) {
						throw new SemanticException(
								"Can't use expression '" + exprCtx.getText() + " without explicit name in xmlforest function" +
										", because XML element names can only be derived from path expressions.",
								query
						);
					}
					elementExpressions.add( new SqmNamedExpression<>( expression, attribute.getName() ) );
				}
			}
		}
		return creationContext.getNodeBuilder().xmlforest( elementExpressions );
	}

	@Override
	public SqmExpression<?> visitXmlpiFunction(HqlParser.XmlpiFunctionContext ctx) {
		checkXmlFunctionsEnabled( ctx );
		final String name = visitIdentifier( ctx.identifier() );
		final HqlParser.ExpressionContext exprCtx = ctx.expression();
		//noinspection unchecked
		return exprCtx == null
				? creationContext.getNodeBuilder().xmlpi( name )
				: creationContext.getNodeBuilder().xmlpi( name, (Expression<String>) exprCtx.accept( this ) );
	}

	@Override
	public SqmExpression<?> visitXmlqueryFunction(HqlParser.XmlqueryFunctionContext ctx) {
		checkXmlFunctionsEnabled( ctx );
		final SqmExpression<String> query = (SqmExpression<String>) ctx.expression( 0 ).accept( this );
		final SqmExpression<?> xmlDocument = (SqmExpression<?>) ctx.expression( 1 ).accept( this );
		return creationContext.getNodeBuilder().xmlquery( query, xmlDocument );
	}

	@Override
	public SqmExpression<?> visitXmlexistsFunction(HqlParser.XmlexistsFunctionContext ctx) {
		checkXmlFunctionsEnabled( ctx );
		final SqmExpression<String> query = (SqmExpression<String>) ctx.expression( 0 ).accept( this );
		final SqmExpression<?> xmlDocument = (SqmExpression<?>) ctx.expression( 1 ).accept( this );
		return creationContext.getNodeBuilder().xmlexists( query, xmlDocument );
	}

	@Override
	public SqmExpression<?> visitXmlaggFunction(HqlParser.XmlaggFunctionContext ctx) {
		checkXmlFunctionsEnabled( ctx );
		final ArrayList<SqmTypedNode<?>> arguments = new ArrayList<>( 1 );
		arguments.add( (SqmTypedNode<?>) ctx.expression().accept( this ) );

		return applyOverClause(
				ctx.overClause(),
				getFunctionDescriptor( "xmlagg" ).generateOrderedSetAggregateSqmExpression(
						arguments,
						getFilterExpression( ctx ),
						ctx.orderByClause() == null
								? null
								: visitOrderByClause( ctx.orderByClause(), false ),
						null,
						creationContext.getQueryEngine()
				)
		);
	}

	@Override
	public Object visitXmltableFunction(HqlParser.XmltableFunctionContext ctx) {
		checkXmlFunctionsEnabled( ctx );
		final List<HqlParser.ExpressionContext> argumentsContexts = ctx.expression();
		//noinspection unchecked
		final SqmExpression<String> xpath = (SqmExpression<String>) argumentsContexts.get( 0 ).accept( this );
		final SqmExpression<?> document = (SqmExpression<?>) argumentsContexts.get( 1 ).accept( this );
		final SqmXmlTableFunction<?> xmlTable = creationContext.getNodeBuilder().xmlTable( xpath, document);
		visitColumns( xmlTable, ctx.xmltableColumnsClause().xmltableColumn() );
		return xmlTable;
	}

	private void visitColumns(SqmXmlTableFunction<?> xmlTable, List<HqlParser.XmltableColumnContext> columnContexts) {
		for ( HqlParser.XmltableColumnContext columnContext : columnContexts ) {
			if ( columnContext instanceof HqlParser.XmlTableQueryColumnContext queryColumnContext ) {
				final String columnName = visitIdentifier( queryColumnContext.identifier() );
				final TerminalNode pathNode = queryColumnContext.STRING_LITERAL();
				final String xpath = pathNode == null ? null : unquoteStringLiteral( pathNode.getText() );
				final JpaXmlTableColumnNode<String> node = xmlTable.queryColumn( columnName, xpath );
				final HqlParser.XmltableDefaultClauseContext defaultClause = queryColumnContext.xmltableDefaultClause();
				if ( defaultClause != null ) {
					//noinspection unchecked
					node.defaultExpression( (Expression<String>) defaultClause.expression().accept( this ) );
				}
			}
			else if ( columnContext instanceof HqlParser.XmlTableValueColumnContext valueColumnContext ) {
				final String columnName = visitIdentifier( valueColumnContext.identifier() );
				//noinspection unchecked
				final SqmCastTarget<Object> castTarget = (SqmCastTarget<Object>) visitCastTarget( valueColumnContext.castTarget() );
				final TerminalNode pathNode = valueColumnContext.STRING_LITERAL();
				final String xpath = pathNode == null ? null : unquoteStringLiteral( pathNode.getText() );
				final JpaXmlTableColumnNode<Object> node = xmlTable.valueColumn( columnName, castTarget, xpath );
				final HqlParser.XmltableDefaultClauseContext defaultClause = valueColumnContext.xmltableDefaultClause();
				if ( defaultClause != null ) {
					//noinspection unchecked
					node.defaultExpression( (Expression<Object>) defaultClause.expression().accept( this ) );
				}
			}
			else {
				final HqlParser.XmlTableOrdinalityColumnContext ordinalityColumnContext
						= (HqlParser.XmlTableOrdinalityColumnContext) columnContext;
				xmlTable.ordinalityColumn( visitIdentifier( ordinalityColumnContext.identifier() ) );
			}
		}
	}

	private void checkXmlFunctionsEnabled(ParserRuleContext ctx) {
		if ( !creationOptions.isXmlFunctionsEnabled() ) {
			throw new SemanticException(
					"Can't use function '" + ctx.children.get( 0 ).getText() +
							"', because tech preview XML functions are not enabled. To enable, set the '" + QuerySettings.XML_FUNCTIONS_ENABLED + "' setting to 'true'.",
					query
			);
		}
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
					"First operand for includes predicate must be a basic plural type expression, but found: "
							+ lhsExpressible.getSqmType(),
					query
			);
		}
		if ( rhsExpressible != null && !( rhsExpressible.getSqmType() instanceof BasicPluralType<?, ?>) ) {
			throw new SemanticException(
					"Second operand for includes predicate must be a basic plural type expression, but found: "
							+ rhsExpressible.getSqmType(),
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
					"First operand for intersects predicate must be a basic plural type expression, but found: "
							+ lhsExpressible.getSqmType(),
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
		final BasicType<Character> characterType = creationContext.getNodeBuilder().getCharacterType();
		if ( parameter instanceof HqlParser.NamedParameterContext namedParameterContext ) {
			return visitNamedParameter( namedParameterContext, characterType );
		}
		else if ( parameter instanceof HqlParser.PositionalParameterContext positionalParameterContext ) {
			return visitPositionalParameter( positionalParameterContext, characterType );
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
			return new SqmLiteral<>( escape.charAt( 0 ), characterType, creationContext.getNodeBuilder() );
		}
	}

	@Override
	public SqmPredicate visitMemberOfPredicate(HqlParser.MemberOfPredicateContext ctx) {
		final boolean negated = ctx.NOT() != null;
		final SqmPath<?> sqmPluralPath = consumeDomainPath( ctx.path() );
		if ( sqmPluralPath instanceof SqmPluralValuedSimplePath<?> pluralValuedSimplePath ) {
			return new SqmMemberOfPredicate(
					(SqmExpression<?>) ctx.expression().accept( this ),
					pluralValuedSimplePath,
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
		if ( inListContext instanceof HqlParser.ExplicitTupleInListContext tupleExpressionListContext ) {
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
						final Set<String> possibleEnumTypes;
						if ( isEnum && child instanceof HqlParser.ExpressionContext expressionContext
								&& ( possibleEnumTypes = getPossibleEnumTypes( expressionContext ) ) != null ) {
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
		else if ( inListContext instanceof HqlParser.ParamInListContext tupleExpressionListContext ) {
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
		else if ( inListContext instanceof HqlParser.SubqueryInListContext subQueryOrParamInListContext ) {
			return new SqmInSubQueryPredicate(
					testExpression,
					visitSubquery( subQueryOrParamInListContext.subquery() ),
					negated,
					creationContext.getNodeBuilder()
			);
		}
		else if ( inListContext instanceof HqlParser.PersistentCollectionReferenceInListContext collectionReferenceInListContext ) {
			if ( getCreationOptions().useStrictJpaCompliance() ) {
				throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.HQL_COLLECTION_FUNCTION );
			}
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
		else if ( inListContext instanceof HqlParser.ArrayInListContext arrayInListContext ) {
			if ( getCreationOptions().useStrictJpaCompliance() ) {
				throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.HQL_COLLECTION_FUNCTION );
			}

			final SqmExpression<?> arrayExpr = (SqmExpression<?>) arrayInListContext.expression().accept( this );
			final SqmExpressible<?> arrayExpressible = arrayExpr.getExpressible();
			if ( arrayExpressible != null ) {
				if ( !(arrayExpressible.getSqmType() instanceof BasicPluralType<?, ?> pluralType) ) {
					throw new SemanticException(
							"Right operand for in-array predicate must be a basic plural type expression, but found: "
								+ arrayExpressible.getSqmType(),
							query
					);
				}
				testExpression.applyInferableType( pluralType.getElementType() );
			}
			final SelfRenderingSqmFunction<Boolean> contains = getFunctionDescriptor( "array_contains" ).generateSqmExpression(
					asList( arrayExpr, testExpression ),
					null,
					creationContext.getQueryEngine()
			);
			return new SqmBooleanExpressionPredicate( contains, negated, creationContext.getNodeBuilder() );
		}
		else {
			throw new ParsingException( "Unexpected IN predicate type [" + ctx.getClass().getSimpleName() + "] : "
					+ ctx.getText() );
		}
	}

	@Override
	public SqmPredicate visitExistsCollectionPartPredicate(HqlParser.ExistsCollectionPartPredicateContext ctx) {
		final SqmSubQuery<Object> subQuery = createCollectionReferenceSubQuery( ctx.simplePath(), null );
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
		final DomainType<?> sqmPathType = sqmPath.getReferencedPathSource().getPathType();
		if ( sqmPathType instanceof IdentifiableDomainType<?> identifiableType ) {
			final PathSource<?> identifierDescriptor = identifiableType.getIdentifierDescriptor();
			if ( identifierDescriptor == null ) {
				// mainly for benefit of Hibernate Processor
				throw new FunctionArgumentException( "Argument '" + sqmPath.getNavigablePath()
						+ "' of 'id()' is a '" + identifiableType.getTypeName()
						+ "' and does not have a well-defined '@Id' attribute" );
			}
			return sqmPath.get( identifierDescriptor.getPathName(), true );
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
		final DomainType<?> sqmPathType = sqmPath.getReferencedPathSource().getPathType();
		if ( sqmPathType instanceof IdentifiableDomainType<?> identifiableType ) {
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

		if ( sqmPath.getReferencedPathSource().getPathType() instanceof IdentifiableDomainType<?> identifiableType ) {
			final List<? extends PersistentAttribute<?, ?>> attributes = identifiableType.findNaturalIdAttributes();
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

			final SingularAttribute<?, ?> naturalIdAttribute = (SingularAttribute<?, ?>) attributes.get(0);
			return sqmPath.get( (SingularAttribute) naturalIdAttribute );
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
		final boolean validToOneRef =
				toOneReference.getBindableType() == Bindable.BindableType.SINGULAR_ATTRIBUTE
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
		return new SqmFkExpression<>( sqmPath );
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
		final Token symbol = ((TerminalNode) ctx.getChild( 0 )).getSymbol();
		return switch ( symbol.getType() ) {
			case HqlParser.PLUS -> UnaryArithmeticOperator.UNARY_PLUS;
			case HqlParser.MINUS -> UnaryArithmeticOperator.UNARY_MINUS;
			default -> throw new ParsingException( "Unrecognized sign operator" );
		};
	}

	@Override
	public Object visitAdditiveOperator(HqlParser.AdditiveOperatorContext ctx) {
		final Token symbol = ((TerminalNode) ctx.getChild( 0 )).getSymbol();
		return switch ( symbol.getType() ) {
			case HqlParser.PLUS -> BinaryArithmeticOperator.ADD;
			case HqlParser.MINUS -> BinaryArithmeticOperator.SUBTRACT;
			default -> throw new ParsingException( "Unrecognized additive operator" );
		};
	}

	@Override
	public Object visitMultiplicativeOperator(HqlParser.MultiplicativeOperatorContext ctx) {
		final Token symbol = ((TerminalNode) ctx.getChild( 0 )).getSymbol();
		return switch ( symbol.getType() ) {
			case HqlParser.ASTERISK -> BinaryArithmeticOperator.MULTIPLY;
			case HqlParser.PERCENT_OP -> BinaryArithmeticOperator.MODULO;
			case HqlParser.SLASH ->
					creationOptions.isPortableIntegerDivisionEnabled()
							? BinaryArithmeticOperator.DIVIDE_PORTABLE
							: BinaryArithmeticOperator.DIVIDE;
			default -> throw new ParsingException( "Unrecognized multiplicative operator" );
		};
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
		final Token symbol = ((TerminalNode) ctx.getChild( 0 ).getChild( 0 )).getSymbol();
		final String text = symbol.getType() == HqlParser.MINUS ? "-" + node.getText() : node.getText();
		return switch ( node.getSymbol().getType() ) {
			case HqlParser.INTEGER_LITERAL -> integerLiteral( text );
			case HqlParser.LONG_LITERAL -> longLiteral( text );
			case HqlParser.BIG_INTEGER_LITERAL -> bigIntegerLiteral( text );
			case HqlParser.HEX_LITERAL -> hexLiteral( text );
			case HqlParser.FLOAT_LITERAL -> floatLiteral( text );
			case HqlParser.DOUBLE_LITERAL -> doubleLiteral( text );
			case HqlParser.BIG_DECIMAL_LITERAL -> bigDecimalLiteral( text );
			default -> throw new ParsingException( "Unexpected terminal node [" + text + "]" );
		};
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
		return switch ( node.getSymbol().getType() ) {
			case HqlParser.STRING_LITERAL -> stringLiteral( node.getText() );
			case HqlParser.JAVA_STRING_LITERAL -> javaStringLiteral( node.getText() );
			case HqlParser.INTEGER_LITERAL -> integerLiteral( node.getText() );
			case HqlParser.LONG_LITERAL -> longLiteral( node.getText() );
			case HqlParser.BIG_INTEGER_LITERAL -> bigIntegerLiteral( node.getText() );
			case HqlParser.HEX_LITERAL -> hexLiteral( node.getText() );
			case HqlParser.FLOAT_LITERAL -> floatLiteral( node.getText() );
			case HqlParser.DOUBLE_LITERAL -> doubleLiteral( node.getText() );
			case HqlParser.BIG_DECIMAL_LITERAL -> bigDecimalLiteral( node.getText() );
			case HqlParser.FALSE -> booleanLiteral( false );
			case HqlParser.TRUE -> booleanLiteral( true );
			case HqlParser.NULL -> new SqmLiteralNull<>( creationContext.getNodeBuilder() );
			case HqlParser.BINARY_LITERAL -> binaryLiteral( node.getText() );
			default -> throw new ParsingException( "Unexpected terminal node [" + node.getText() + "]" );
		};
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

		return new SqmHqlNumericLiteral<>( text, integerDomainType, creationContext.getNodeBuilder() );
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
	public SqmSetReturningFunction<?> visitSimpleSetReturningFunction(HqlParser.SimpleSetReturningFunctionContext ctx) {
		final String functionName = visitIdentifier( ctx.identifier() );
		final HqlParser.GenericFunctionArgumentsContext argumentsContext = ctx.genericFunctionArguments();
		@SuppressWarnings("unchecked")
		final List<SqmTypedNode<?>> functionArguments =
				argumentsContext == null
						? emptyList()
						: (List<SqmTypedNode<?>>) argumentsContext.accept(this);

		SqmSetReturningFunctionDescriptor functionTemplate = getSetReturningFunctionDescriptor( functionName );
		if ( functionTemplate == null ) {
			throw new SemanticException(
					"The %s() set-returning function was not registered for the dialect".formatted( functionName ),
					query
			);
		}
		return functionTemplate.generateSqmExpression( functionArguments, creationContext.getQueryEngine() );
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
		final SqmFunction<?> function = switch ( functionTemplate.getFunctionKind() ) {
			case ORDERED_SET_AGGREGATE -> functionTemplate.generateOrderedSetAggregateSqmExpression(
					functionArguments,
					filterExpression,
					ctx.withinGroupClause() == null
							? null // this is allowed for e.g. rank(), but not for all
							: visitOrderByClause( ctx.withinGroupClause().orderByClause(), false ),
					null,
					creationContext.getQueryEngine()
			);
			case AGGREGATE -> functionTemplate.generateAggregateSqmExpression(
					functionArguments,
					filterExpression,
					null,
					creationContext.getQueryEngine()
			);
			case WINDOW -> functionTemplate.generateWindowSqmExpression(
					functionArguments,
					filterExpression,
					null,
					null,
					null,
					creationContext.getQueryEngine()
			);
			default -> functionTemplate.generateSqmExpression(
					functionArguments,
					null,
					creationContext.getQueryEngine()
			);
		};
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

	private SqmSetReturningFunctionDescriptor getSetReturningFunctionDescriptor(String name) {
		return creationContext.getQueryEngine().getSqmFunctionRegistry().findSetReturningFunctionDescriptor( name );
	}

	@Override
	public SqmExtractUnit<?> visitDatetimeField(HqlParser.DatetimeFieldContext ctx) {
		final NodeBuilder nodeBuilder = creationContext.getNodeBuilder();
		final Token symbol = ((TerminalNode) ctx.getChild( 0 )).getSymbol();
		return switch ( symbol.getType() ) {
			case HqlParser.DAY -> new SqmExtractUnit<>(
					TemporalUnit.DAY,
					resolveExpressibleTypeBasic( Integer.class ),
					nodeBuilder
			);
			case HqlParser.MONTH -> new SqmExtractUnit<>(
					TemporalUnit.MONTH,
					resolveExpressibleTypeBasic( Integer.class ),
					nodeBuilder
			);
			case HqlParser.YEAR -> new SqmExtractUnit<>(
					TemporalUnit.YEAR,
					resolveExpressibleTypeBasic( Integer.class ),
					nodeBuilder
			);
			case HqlParser.HOUR -> new SqmExtractUnit<>(
					TemporalUnit.HOUR,
					resolveExpressibleTypeBasic( Integer.class ),
					nodeBuilder
			);
			case HqlParser.MINUTE -> new SqmExtractUnit<>(
					TemporalUnit.MINUTE,
					resolveExpressibleTypeBasic( Integer.class ),
					nodeBuilder
			);
			case HqlParser.SECOND -> new SqmExtractUnit<>(
					TemporalUnit.SECOND,
					resolveExpressibleTypeBasic( Float.class ),
					nodeBuilder
			);
			case HqlParser.NANOSECOND -> new SqmExtractUnit<>(
					NANOSECOND,
					resolveExpressibleTypeBasic( Long.class ),
					nodeBuilder
			);
			case HqlParser.WEEK -> new SqmExtractUnit<>(
					TemporalUnit.WEEK,
					resolveExpressibleTypeBasic( Integer.class ),
					nodeBuilder
			);
			case HqlParser.QUARTER -> new SqmExtractUnit<>(
					TemporalUnit.QUARTER,
					resolveExpressibleTypeBasic( Integer.class ),
					nodeBuilder
			);
			case HqlParser.EPOCH -> new SqmExtractUnit<>(
					TemporalUnit.EPOCH,
					resolveExpressibleTypeBasic( Long.class ),
					nodeBuilder
			);
			default -> throw new ParsingException( "Unsupported datetime field [" + ctx.getText() + "]" );
		};
	}

	@Override
	public Object visitDayField(HqlParser.DayFieldContext ctx) {
		final NodeBuilder nodeBuilder = creationContext.getNodeBuilder();
		final Token symbol = ((TerminalNode) ctx.getChild( 2 )).getSymbol();
		return switch ( symbol.getType() ) {
			case HqlParser.MONTH ->
					new SqmExtractUnit<>( DAY_OF_MONTH, resolveExpressibleTypeBasic( Integer.class ), nodeBuilder );
			case HqlParser.WEEK ->
					new SqmExtractUnit<>( DAY_OF_WEEK, resolveExpressibleTypeBasic( Integer.class ), nodeBuilder );
			case HqlParser.YEAR ->
					new SqmExtractUnit<>( DAY_OF_YEAR, resolveExpressibleTypeBasic( Integer.class ), nodeBuilder );
			default -> throw new ParsingException( "Unsupported day field [" + ctx.getText() + "]" );
		};
	}

	@Override
	public Object visitWeekField(HqlParser.WeekFieldContext ctx) {
		final NodeBuilder nodeBuilder = creationContext.getNodeBuilder();
		final Token symbol = ((TerminalNode) ctx.getChild( 2 )).getSymbol();
		return switch ( symbol.getType() ) {
			case HqlParser.MONTH ->
				//this is computed from DAY_OF_MONTH/7
					new SqmExtractUnit<>( WEEK_OF_MONTH, resolveExpressibleTypeBasic( Integer.class ), nodeBuilder );
			case HqlParser.YEAR ->
				//this is computed from DAY_OF_YEAR/7
					new SqmExtractUnit<>( WEEK_OF_YEAR, resolveExpressibleTypeBasic( Integer.class ), nodeBuilder );
			default -> throw new ParsingException( "Unsupported week field [" + ctx.getText() + "]" );
		};
	}

	@Override
	public Object visitDateOrTimeField(HqlParser.DateOrTimeFieldContext ctx) {
		final NodeBuilder nodeBuilder = creationContext.getNodeBuilder();
		final Token symbol = ((TerminalNode) ctx.getChild( 0 )).getSymbol();
		return switch ( symbol.getType() ) {
			case HqlParser.DATE -> isExtractingJdbcTemporalType
					? new SqmExtractUnit<>( DATE, resolveExpressibleTypeBasic( Date.class ), nodeBuilder )
					: new SqmExtractUnit<>( DATE, resolveExpressibleTypeBasic( LocalDate.class ), nodeBuilder );
			case HqlParser.TIME -> isExtractingJdbcTemporalType
					? new SqmExtractUnit<>( TIME, resolveExpressibleTypeBasic( Time.class ), nodeBuilder )
					: new SqmExtractUnit<>( TIME, resolveExpressibleTypeBasic( LocalTime.class ), nodeBuilder );
			default -> throw new ParsingException( "Unsupported date or time field [" + ctx.getText() + "]" );
		};
	}

	@Override
	public Object visitTimeZoneField(HqlParser.TimeZoneFieldContext ctx) {
		final NodeBuilder nodeBuilder = creationContext.getNodeBuilder();
		final Token symbol = ((TerminalNode) ctx.getChild( ctx.getChildCount() - 1 )).getSymbol();
		// should never happen
		return switch ( symbol.getType() ) {
			case HqlParser.TIMEZONE_HOUR, HqlParser.HOUR ->
					new SqmExtractUnit<>( TIMEZONE_HOUR, resolveExpressibleTypeBasic( Integer.class ), nodeBuilder );
			case HqlParser.TIMEZONE_MINUTE, HqlParser.MINUTE ->
					new SqmExtractUnit<>( TIMEZONE_MINUTE, resolveExpressibleTypeBasic( Integer.class ), nodeBuilder );
			case HqlParser.OFFSET ->
					new SqmExtractUnit<>( OFFSET, resolveExpressibleTypeBasic( ZoneOffset.class ), nodeBuilder );
			default -> throw new ParsingException( "Unsupported time zone field [" + ctx.getText() + "]" );
		};
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
		while ( !(lhs instanceof AbstractSqmFrom<?, ?> correlationBase) ) {
			implicitJoinPaths.add( lhs.getNavigablePath().getLocalName() );
			lhs = lhs.getLhs();
		}
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
			final String partName = switch ( collectionReferenceCtx.getSymbol().getType() ) {
				case HqlParser.ELEMENTS -> CollectionPart.Nature.ELEMENT.getName();
				case HqlParser.INDICES -> CollectionPart.Nature.INDEX.getName();
				default -> throw new ParsingException( "Unexpected collection reference: "
						+ collectionReferenceCtx.getText() );
			};
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
			final Token symbol = ((TerminalNode) frameClause.getChild( 0 )).getSymbol();
			mode = switch ( symbol.getType() ) {
				case HqlParser.RANGE -> FrameMode.RANGE;
				case HqlParser.ROWS -> FrameMode.ROWS;
				case HqlParser.GROUPS -> FrameMode.GROUPS;
				default -> throw new IllegalArgumentException( "Unexpected frame mode: " + frameClause.getChild( 0 ) );
			};
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
				final Token token = ((TerminalNode) lastChild.getChild( 1 )).getSymbol();
				exclusion = switch ( token.getType() ) {
					case HqlParser.CURRENT -> FrameExclusion.CURRENT_ROW;
					case HqlParser.GROUP -> FrameExclusion.GROUP;
					case HqlParser.TIES -> FrameExclusion.TIES;
					case HqlParser.NO -> FrameExclusion.NO_OTHERS;
					default -> throw new IllegalArgumentException( "Unexpected frame exclusion: " + lastChild );
				};
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
		final Token symbol = ((TerminalNode) child.getChild( 1 )).getSymbol();
		return switch ( symbol.getType() ) {
			case HqlParser.PRECEDING ->
					child.getChild( 0 ) instanceof TerminalNode
							? FrameKind.UNBOUNDED_PRECEDING
							: FrameKind.OFFSET_PRECEDING;
			case HqlParser.FOLLOWING ->
					child.getChild( 0 ) instanceof TerminalNode
							? FrameKind.UNBOUNDED_FOLLOWING
							: FrameKind.OFFSET_FOLLOWING;
			case HqlParser.ROW -> FrameKind.CURRENT_ROW;
			default -> throw new IllegalArgumentException( "Illegal frame kind: " + child );
		};
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
		final Token symbol = ((TerminalNode) ctx.getChild( 0 )).getSymbol();
		return switch ( symbol.getType() ) {
			case HqlParser.LEADING -> new SqmTrimSpecification( TrimSpec.LEADING, creationContext.getNodeBuilder() );
			case HqlParser.TRAILING -> new SqmTrimSpecification( TrimSpec.TRAILING, creationContext.getNodeBuilder() );
			default -> throw new ParsingException( "Unsupported pad specification [" + ctx.getText() + "]" );
		};
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
		final SqmTrimSpecification trimSpec = visitTrimSpecification( ctx.trimSpecification() );
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
		final Token symbol = ((TerminalNode) ctx.getChild( 0 )).getSymbol();
		return switch ( symbol.getType() ) {
			case HqlParser.LEADING -> TrimSpec.LEADING;
			case HqlParser.TRAILING -> TrimSpec.TRAILING;
			case HqlParser.BOTH -> TrimSpec.BOTH;
			default -> throw new ParsingException( "Unrecognized trim specification" );
		};
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

		CollectionPart.Nature nature = switch ( firstNode.getSymbol().getType() ) {
			case ELEMENTS -> CollectionPart.Nature.ELEMENT;
			case INDICES -> CollectionPart.Nature.INDEX;
			default -> throw new ParsingException( "Impossible symbol" );
		};

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
			if ( lhsExpressible == null ) {
				throw new SemanticException( "Slice operator applied to expression of unknown type", query );
			}
			else if ( lhsExpressible.getSqmType() instanceof BasicPluralType<?, ?> ) {
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
			else if ( lhsExpressible.getRelationalJavaType() instanceof StringJavaType
					&& !(lhs instanceof SqmPluralValuedSimplePath) ) {
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
			else {
				throw new SemanticException( "Slice operator applied to expression which is not a string or SQL array", query );
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
		if ( consumer instanceof QualifiedJoinPathConsumer qualifiedJoinPathConsumer ) {
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
		final String importableName = getJpaMetamodel().qualifyImportableName( treatTargetName );
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
		if ( consumer instanceof QualifiedJoinPathConsumer qualifiedJoinPathConsumer ) {
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
		if ( consumer instanceof QualifiedJoinPathConsumer qualifiedJoinPathConsumer ) {
			if ( madeNested && !hasContinuation ) {
				// Reset the nested state before consuming the terminal identifier
				qualifiedJoinPathConsumer.setNested( false );
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
			if ( consumer instanceof QualifiedJoinPathConsumer ) {
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
		if ( consumer instanceof QualifiedJoinPathConsumer qualifiedJoinPathConsumer ) {
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
		final TerminalNode firstNode = (TerminalNode) ctx.indexKeyQuantifier().getChild( 0 );
		if ( firstNode.getSymbol().getType() == HqlParser.INDEX
				&& referencedPathSource instanceof AnonymousTupleType<?> tupleType ) {
			if ( tupleType.findSubPathSource( CollectionPart.Nature.INDEX.getName() ) == null ) {
				throw new FunctionArgumentException(
						String.format(
								"The set-returning from node '%s' does not specify an index/ordinality",
								sqmPath.getNavigablePath()
						)
				);
			}
		}
		else {
			checkPluralPath( sqmPath, referencedPathSource, firstNode );
		}

		if ( getCreationOptions().useStrictJpaCompliance() ) {
			final PluralPersistentAttribute<?, ?, ?> attribute = (PluralPersistentAttribute<?, ?, ?>) referencedPathSource;
			if ( attribute.getCollectionClassification() != CollectionClassification.MAP
					&& firstNode.getSymbol().getType() == HqlParser.KEY ) {
				throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.KEY_FUNCTION_ON_NON_MAP );
			}
		}

		SqmPath<?> result;
		if ( sqmPath instanceof SqmMapJoin<?, ?, ?> sqmMapJoin ) {
			if ( consumer instanceof QualifiedJoinPathConsumer pathConsumer ) {
				if ( madeNested && !hasContinuation ) {
					// Reset the nested state before consuming the terminal identifier
					pathConsumer.setNested( false );
				}
				consumer.consumeIdentifier( CollectionPart.Nature.INDEX.getName(), false, !hasContinuation );
				result = (SqmPath<?>) consumer.getConsumedPart();
			}
			else {
				result = sqmMapJoin.key();
			}
		}
		else if ( sqmPath instanceof SqmListJoin<?, ?> listJoin ) {
			if ( hasContinuation ) {
				throw new TerminalPathException("List index has no attributes");
			}
			result = listJoin.resolvePathPart( CollectionPart.Nature.INDEX.getName(), true, this );
		}
		else if ( sqmPath instanceof SqmFunctionRoot<?> functionRoot ) {
			if ( hasContinuation ) {
				throw new TerminalPathException("List index has no attributes");
			}
			result = functionRoot.index();
		}
		else if ( sqmPath instanceof SqmFunctionJoin<?> functionJoin ) {
			if ( hasContinuation ) {
				throw new TerminalPathException("List index has no attributes");
			}
			result = functionJoin.index();
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
			if ( consumer instanceof QualifiedJoinPathConsumer ) {
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
		if ( !( referencedPathSource instanceof PluralPersistentAttribute<?, ?, ?> ) ) {
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
		if ( consumedPart instanceof SqmPath<?> sqmPath ) {
			return sqmPath;
		}
		else {
			throw new PathException( "Expecting domain-model path, but found: " + consumedPart );
		}
	}


	private SqmPath<?> consumeDomainPath(HqlParser.SimplePathContext sequence) {
		final SemanticPathPart consumedPart = (SemanticPathPart) sequence.accept( this );
		if ( consumedPart instanceof SqmPath<?> sqmPath ) {
			return sqmPath;
		}
		else {
			throw new PathException( "Expecting domain-model path, but found: " + consumedPart );
		}
	}

	private SqmPath<?> consumeManagedTypeReference(HqlParser.PathContext parserPath) {
		final SqmPath<?> sqmPath = consumeDomainPath( parserPath );
		final SqmPathSource<?> pathSource = sqmPath.getReferencedPathSource();
		if ( pathSource.getPathType() instanceof ManagedDomainType<?> ) {
			return sqmPath;
		}
		else {
			throw new PathException( "Expecting ManagedType valued path [" + sqmPath.getNavigablePath()
					+ "], but found: " + pathSource.getPathType() );
		}
	}

	private SqmPath<?> consumePluralAttributeReference(HqlParser.PathContext parserPath) {
		final SqmPath<?> sqmPath = consumeDomainPath( parserPath );
		if ( sqmPath.getReferencedPathSource() instanceof PluralPersistentAttribute ) {
			return sqmPath;
		}
		else {
			throw new PathException( "Expecting plural attribute valued path [" + sqmPath.getNavigablePath()
					+ "], but found: " + sqmPath.getReferencedPathSource().getPathType() );
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
