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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.NullPrecedence;
import org.hibernate.SortOrder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.MapPersistentAttribute;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.query.BinaryArithmeticOperator;
import org.hibernate.query.ComparisonOperator;
import org.hibernate.query.PathException;
import org.hibernate.query.QueryLogger;
import org.hibernate.query.SemanticException;
import org.hibernate.query.TrimSpec;
import org.hibernate.query.UnaryArithmeticOperator;
import org.hibernate.query.hql.HqlInterpretationException;
import org.hibernate.query.hql.spi.DotIdentifierConsumer;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.sqm.LiteralNumberFormatException;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.StrictJpaComplianceViolation;
import org.hibernate.query.sqm.UnknownEntityException;
import org.hibernate.query.sqm.function.SqmCastTarget;
import org.hibernate.query.sqm.function.SqmDistinct;
import org.hibernate.query.sqm.function.SqmExtractUnit;
import org.hibernate.query.sqm.function.SqmStar;
import org.hibernate.query.sqm.function.SqmTrimSpecification;
import org.hibernate.query.sqm.internal.ParameterCollector;
import org.hibernate.query.sqm.SqmTreeCreationLogger;
import org.hibernate.query.sqm.produce.function.SqmFunctionTemplate;
import org.hibernate.query.sqm.produce.function.spi.NamedSqmFunctionTemplate;
import org.hibernate.query.sqm.internal.SqmDmlCreationProcessingState;
import org.hibernate.query.sqm.internal.SqmQuerySpecCreationProcessingStateStandardImpl;
import org.hibernate.query.sqm.spi.ParameterDeclarationContext;
import org.hibernate.query.sqm.spi.SqmCreationContext;
import org.hibernate.query.hql.spi.SqmCreationOptions;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.SqmIndexedCollectionAccessPath;
import org.hibernate.query.sqm.tree.domain.SqmMapEntryReference;
import org.hibernate.query.sqm.tree.domain.SqmMaxElementPath;
import org.hibernate.query.sqm.tree.domain.SqmMaxIndexPath;
import org.hibernate.query.sqm.tree.domain.SqmMinElementPath;
import org.hibernate.query.sqm.tree.domain.SqmMinIndexPath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPolymorphicRootDescriptor;
import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;
import org.hibernate.query.sqm.tree.expression.LiteralHelper;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmCaseSearched;
import org.hibernate.query.sqm.tree.expression.SqmCaseSimple;
import org.hibernate.query.sqm.tree.expression.SqmCollectionSize;
import org.hibernate.query.sqm.tree.expression.SqmEntityType;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.SqmLiteralNull;
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;
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
import org.hibernate.query.sqm.tree.predicate.SqmAndPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmBetweenPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmComparisonPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmEmptinessPredicate;
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
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationArgument;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

import org.jboss.logging.Logger;

import org.antlr.v4.runtime.Token;

import static java.util.Arrays.asList;

/**
 * Responsible for producing an SQM using visitation over an HQL parse tree generated by
 * Antlr via {@link HqlParseTreeBuilder}.
 *
 * @author Steve Ebersole
 */
public class SemanticQueryBuilder extends HqlParserBaseVisitor implements SqmCreationState {

	private static final Logger log = Logger.getLogger( SemanticQueryBuilder.class );

	/**
	 * Main entry point into analysis of HQL/JPQL parse tree - producing a semantic model of the
	 * query.
	 *
	 * @param statement The statement to analyze.
	 * @param creationContext Access to things needed to perform the analysis
	 *
	 * @return The semantic query model
	 */
	public static SqmStatement buildSemanticModel(
			HqlParser.StatementContext statement,
			SqmCreationOptions creationOptions,
			SqmCreationContext creationContext) {
		return new SemanticQueryBuilder( creationOptions, creationContext ).visitStatement( statement );
	}

	private final SqmCreationOptions creationOptions;
	private final SqmCreationContext creationContext;

	private final Stack<DotIdentifierConsumer> dotIdentifierConsumerStack;

	private final Stack<TreatHandler> treatHandlerStack = new StandardStack<>( new TreatHandlerNormal() );

	private final Stack<ParameterDeclarationContext> parameterDeclarationContextStack = new StandardStack<>();
	private final Stack<SqmCreationProcessingState> processingStateStack = new StandardStack<>();

	private ParameterCollector parameterCollector;


	public Stack<SqmCreationProcessingState> getProcessingStateStack() {
		return processingStateStack;
	}

	protected SemanticQueryBuilder(SqmCreationOptions creationOptions, SqmCreationContext creationContext) {
		this.creationOptions = creationOptions;
		this.creationContext = creationContext;

		this.dotIdentifierConsumerStack = new StandardStack<>( new BasicDotIdentifierConsumer( this ) );
	}

	@Override
	public SqmCreationContext getCreationContext() {
		return creationContext;
	}

	@Override
	public SqmCreationOptions getCreationOptions() {
		return creationOptions;
	}

	protected Stack<ParameterDeclarationContext> getParameterDeclarationContextStack() {
		return parameterDeclarationContextStack;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Grammar rules

	@Override
	public SqmStatement visitStatement(HqlParser.StatementContext ctx) {
		// parameters allow multi-valued bindings only in very limited cases, so for
		// the base case here we say false
		parameterDeclarationContextStack.push( () -> false );

		try {
			if ( ctx.selectStatement() != null ) {
				return visitSelectStatement( ctx.selectStatement() );
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
	// To-level statements

	@Override
	public SqmSelectStatement visitSelectStatement(HqlParser.SelectStatementContext ctx) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			if ( ctx.querySpec().selectClause() == null ) {
				throw new StrictJpaComplianceViolation(
						"Encountered implicit select-clause, but strict JPQL compliance was requested",
						StrictJpaComplianceViolation.Type.IMPLICIT_SELECT
				);
			}
		}

		final SqmSelectStatement selectStatement = new SqmSelectStatement( creationContext.getNodeBuilder() );

		parameterCollector = selectStatement;

		processingStateStack.push(
				new SqmQuerySpecCreationProcessingStateStandardImpl(
						processingStateStack.getCurrent(),
						selectStatement,
						this
				)
		);

		try {
			//noinspection unchecked
			selectStatement.setQuerySpec( visitQuerySpec( ctx.querySpec() ) );
		}
		finally {
			processingStateStack.pop();
		}

		return selectStatement;
	}

	@Override
	public SqmInsertSelectStatement visitInsertStatement(HqlParser.InsertStatementContext ctx) {
		final EntityDomainType<?> targetType = visitEntityName( ctx.insertSpec().intoSpec().entityName() );

		final SqmRoot<?> root = new SqmRoot<>( targetType, null, creationContext.getNodeBuilder() );
		processingStateStack.getCurrent().getPathRegistry().register( root );

		// for now we only support the INSERT-SELECT form
		final SqmInsertSelectStatement insertStatement = new SqmInsertSelectStatement<>(
				root,
				creationContext.getQueryEngine().getCriteriaBuilder()
		);
		parameterCollector = insertStatement;

		processingStateStack.push( new SqmDmlCreationProcessingState( insertStatement, this ) );

		try {
			insertStatement.setSelectQuerySpec( visitQuerySpec( ctx.querySpec() ) );

			for ( HqlParser.DotIdentifierSequenceContext stateFieldCtx : ctx.insertSpec().targetFieldsSpec().dotIdentifierSequence() ) {
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

	@Override
	public SqmUpdateStatement visitUpdateStatement(HqlParser.UpdateStatementContext ctx) {
		final SqmRoot<?> root = new SqmRoot<>(
				visitEntityName( ctx.entityName() ),
				visitIdentificationVariableDef( ctx.identificationVariableDef() ),
				creationContext.getNodeBuilder()
		);
		final SqmUpdateStatement<?> updateStatement = new SqmUpdateStatement<>( root, creationContext.getNodeBuilder() );

		parameterCollector = updateStatement;

		processingStateStack.push( new SqmDmlCreationProcessingState( updateStatement, this ) );
		try {
			updateStatement.getWhereClause().setPredicate(
					(SqmPredicate) ctx.whereClause().predicate().accept( this )
			);

			for ( HqlParser.AssignmentContext assignmentContext : ctx.setClause().assignment() ) {
				final SqmPath stateField = consumeDomainPath( assignmentContext.dotIdentifierSequence() );
				// todo : validate "state field" expression
				updateStatement.getSetClause().addAssignment(
						stateField,
						(SqmExpression) assignmentContext.expression().accept( this )
				);
			}

			return updateStatement;
		}
		finally {
			processingStateStack.pop();
		}
	}

	@Override
	public SqmDeleteStatement visitDeleteStatement(HqlParser.DeleteStatementContext ctx) {
		final SqmRoot<?> root = new SqmRoot<>(
				visitEntityName( ctx.entityName() ),
				visitIdentificationVariableDef( ctx.identificationVariableDef() ),
				creationContext.getNodeBuilder()
		);

		final SqmDeleteStatement<?> deleteStatement = new SqmDeleteStatement<>( root, creationContext.getNodeBuilder() );

		parameterCollector = deleteStatement;

		processingStateStack.push( new SqmDmlCreationProcessingState( deleteStatement, this ) );
		try {
			if ( ctx.whereClause() != null && ctx.whereClause().predicate() != null ) {
				deleteStatement.getWhereClause().setPredicate(
						(SqmPredicate) ctx.whereClause().predicate().accept( this )
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
	public SqmQuerySpec visitQuerySpec(HqlParser.QuerySpecContext ctx) {
		final SqmQuerySpec sqmQuerySpec = new SqmQuerySpec( creationContext.getNodeBuilder() );

		// visit from-clause first!!!
		treatHandlerStack.push( new TreatHandlerFromClause() );
		try {
			sqmQuerySpec.setFromClause( visitFromClause( ctx.fromClause() ) );
		}
		finally {
			treatHandlerStack.pop();
		}

		final SqmSelectClause selectClause;
		if ( ctx.selectClause() != null ) {
			selectClause = visitSelectClause( ctx.selectClause() );
		}
		else {
			log.debugf( "Encountered implicit select clause : " + ctx.getText() );
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

		final SqmOrderByClause orderByClause;
		if ( ctx.orderByClause() != null ) {
			if ( creationOptions.useStrictJpaCompliance() && processingStateStack.depth() > 1 ) {
				throw new StrictJpaComplianceViolation(
						StrictJpaComplianceViolation.Type.SUBQUERY_ORDER_BY
				);
			}

			orderByClause = visitOrderByClause( ctx.orderByClause() );
		}
		else {
			orderByClause = new SqmOrderByClause();
		}
		sqmQuerySpec.setOrderByClause( orderByClause );


		if ( ctx.limitClause() != null || ctx.offsetClause() != null ) {
			if ( getCreationOptions().useStrictJpaCompliance() ) {
				throw new StrictJpaComplianceViolation(
						StrictJpaComplianceViolation.Type.LIMIT_OFFSET_CLAUSE
				);
			}

			if ( processingStateStack.depth() > 1 && orderByClause == null ) {
				throw new SemanticException( "limit and offset clause require an order-by clause when used in sub-query" );
			}

			//noinspection unchecked
			sqmQuerySpec.setOffsetExpression( visitOffsetClause( ctx.offsetClause() ) );
			//noinspection unchecked
			sqmQuerySpec.setLimitExpression( visitLimitClause( ctx.limitClause() ) );
		}

		return sqmQuerySpec;
	}

	@SuppressWarnings("WeakerAccess")
	protected SqmSelectClause buildInferredSelectClause(SqmFromClause fromClause) {
		// for now, this is slightly different than the legacy behavior where
		// the root and each non-fetched-join was selected.  For now, here, we simply
		// select the root
		final SqmSelectClause selectClause = new SqmSelectClause( false, creationContext.getNodeBuilder() );
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
		final String resultIdentifier = visitResultIdentifier( ctx.resultIdentifier() );
		final SqmSelectableNode selectableNode = visitSelectableNode( ctx );

		//noinspection unchecked
		final SqmSelection selection = new SqmSelection(
				selectableNode,
				resultIdentifier,
				creationContext.getNodeBuilder()
		);

		getProcessingStateStack().getCurrent().getPathRegistry().register( selection );

		return selection;
	}

	private SqmSelectableNode visitSelectableNode(HqlParser.SelectionContext ctx) {
		if ( ctx.selectExpression().dynamicInstantiation() != null ) {
			return visitDynamicInstantiation( ctx.selectExpression().dynamicInstantiation() );
		}
		else if ( ctx.selectExpression().jpaSelectObjectSyntax() != null ) {
			return visitJpaSelectObjectSyntax( ctx.selectExpression().jpaSelectObjectSyntax() );
		}
		else if ( ctx.selectExpression().mapEntrySelection() != null ) {
			return visitMapEntrySelection( ctx.selectExpression().mapEntrySelection() );
		}
		else if ( ctx.selectExpression().expression() != null ) {
			return (SqmExpression) ctx.selectExpression().expression().accept( this );
		}

		throw new ParsingException( "Unexpected selection rule type : " + ctx.getText() );
	}

	@Override
	public String visitResultIdentifier(HqlParser.ResultIdentifierContext resultIdentifierContext) {
		if ( resultIdentifierContext != null ) {
			if ( resultIdentifierContext.AS() != null ) {
				final Token aliasToken = resultIdentifierContext.identifier().getStart();
				final String explicitAlias = aliasToken.getText();

				if ( aliasToken.getType() != HqlParser.IDENTIFIER ) {
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

	private JavaTypeDescriptor<List> listJavaTypeDescriptor;
	private JavaTypeDescriptor<Map> mapJavaTypeDescriptor;

	@Override
	public SqmDynamicInstantiation visitDynamicInstantiation(HqlParser.DynamicInstantiationContext ctx) {
		final SqmDynamicInstantiation dynamicInstantiation;

		if ( ctx.dynamicInstantiationTarget().MAP() != null ) {
			if ( mapJavaTypeDescriptor == null ) {
				mapJavaTypeDescriptor = creationContext.getJpaMetamodel()
						.getTypeConfiguration()
						.getJavaTypeDescriptorRegistry()
						.getDescriptor( Map.class );
			}
			dynamicInstantiation = SqmDynamicInstantiation.forMapInstantiation(
					mapJavaTypeDescriptor,
					creationContext.getNodeBuilder()
			);
		}
		else if ( ctx.dynamicInstantiationTarget().LIST() != null ) {
			if ( listJavaTypeDescriptor == null ) {
				listJavaTypeDescriptor = creationContext.getJpaMetamodel()
						.getTypeConfiguration()
						.getJavaTypeDescriptorRegistry()
						.getDescriptor( List.class );
			}
			dynamicInstantiation = SqmDynamicInstantiation.forListInstantiation(
					listJavaTypeDescriptor,
					creationContext.getNodeBuilder()
			);
		}
		else {
			final String className = ctx.dynamicInstantiationTarget().dotIdentifierSequence().getText();
			try {
				final JavaTypeDescriptor jtd = resolveInstantiationTargetJtd( className );
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

	private JavaTypeDescriptor resolveInstantiationTargetJtd(String className) {
		final Class<?> targetJavaType = classForName( creationContext.getJpaMetamodel().qualifyImportableName( className ) );
		return creationContext.getJpaMetamodel()
				.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.resolveDescriptor( targetJavaType );
	}

	private Class classForName(String className) {
		return creationContext.getServiceRegistry().getService( ClassLoaderService.class ).classForName( className );
	}

	@Override
	public SqmDynamicInstantiationArgument visitDynamicInstantiationArg(HqlParser.DynamicInstantiationArgContext ctx) {
		//noinspection unchecked
		return new SqmDynamicInstantiationArgument(
				visitDynamicInstantiationArgExpression( ctx.dynamicInstantiationArgExpression() ),
				ctx.identifier() == null ? null : ctx.identifier().getText(),
				creationContext.getNodeBuilder()
		);
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
	public Object visitGroupByClause(HqlParser.GroupByClauseContext ctx) {
		return super.visitGroupByClause( ctx );
	}

	@Override
	public Object visitHavingClause(HqlParser.HavingClauseContext ctx) {
		return super.visitHavingClause( ctx );
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
		final SqmExpression sortExpression = visitSortExpression( ctx.sortExpression() );
		if ( sortExpression == null ) {
			throw new ParsingException( "Could not resolve sort-expression : " + ctx.sortExpression().getText() );
		}
		if ( sortExpression instanceof SqmLiteral
				|| sortExpression instanceof SqmParameter ) {
			QueryLogger.QUERY_LOGGER.debugf( "Questionable sorting by constant value : %s", sortExpression );
		}

		final String collation;
		if ( ctx.collationSpecification() != null && ctx.collationSpecification().collateName() != null ) {
			collation = ctx.collationSpecification().collateName().dotIdentifierSequence().getText();
		}
		else {
			collation = null;
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

		// todo (6.0) : NullPrecedence
		final NullPrecedence nullPrecedence = null;

		return new SqmSortSpecification( sortExpression, collation, sortOrder, nullPrecedence );
	}

	@Override
	public SqmExpression visitSortExpression(HqlParser.SortExpressionContext ctx) {
		if ( ctx.INTEGER_LITERAL() != null ) {
			final int position = Integer.parseInt( ctx.INTEGER_LITERAL().getText() );
			final SqmSelection selection = getCurrentProcessingState().getPathRegistry().findSelectionByPosition( position );
			if ( selection != null ) {
				final SqmSelectableNode selectableNode = selection.getSelectableNode();
				if ( selectableNode instanceof SqmExpression ) {
					return (SqmExpression) selectableNode;
				}
			}

			return new SqmLiteral<>( position, resolveExpressableTypeBasic( Integer.class ), creationContext.getNodeBuilder() );
		}

		if ( ctx.identifier() != null ) {
			final SqmSelection selection = getCurrentProcessingState().getPathRegistry().findSelectionByAlias( ctx.identifier().getText() );
			if ( selection != null ) {
				final SqmSelectableNode selectableNode = selection.getSelectableNode();
				if ( selectableNode instanceof SqmExpression ) {
					return (SqmExpression) selectableNode;
				}
			}

			final SqmFrom sqmFrom = getCurrentProcessingState().getPathRegistry().findFromByAlias( ctx.identifier().getText() );
			if ( sqmFrom != null ) {
				return sqmFrom;
			}
		}

		return (SqmExpression) ctx.expression().accept( this );
	}

	@Override
	public SqmExpression visitLimitClause(HqlParser.LimitClauseContext ctx) {
		if ( ctx == null ) {
			return null;
		}

		return (SqmExpression) ctx.parameterOrNumberLiteral().accept( this );
	}

	@Override
	public SqmExpression visitOffsetClause(HqlParser.OffsetClauseContext ctx) {
		if ( ctx == null ) {
			return null;
		}

		return (SqmExpression) ctx.parameterOrNumberLiteral().accept( this );
	}

	@Override
	public Object visitPathExpression(HqlParser.PathExpressionContext ctx) {
		return ctx.path().accept( this );
	}

	@Override
	public SqmExpression visitParameterOrNumberLiteral(HqlParser.ParameterOrNumberLiteralContext ctx) {
		if ( ctx.INTEGER_LITERAL() != null ) {
			return integerLiteral( ctx.INTEGER_LITERAL().getText() );
		}
		if ( ctx.parameter() != null ) {
			return (SqmExpression) ctx.parameter().accept( this );
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
		final String entityName = parserEntityName.dotIdentifierSequence().getText();
		final EntityDomainType entityReference = resolveEntityReference( entityName );
		if ( entityReference == null ) {
			throw new UnknownEntityException( "Could not resolve entity name [" + entityName + "] as DML target", entityName );
		}

		return entityReference;
	}

	private EntityDomainType resolveEntityReference(String entityName) {
		log.debugf( "Attempting to resolve path [%s] as entity reference...", entityName );
		EntityDomainType reference = null;
		try {
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
			final SqmFromClause fromClause = new SqmFromClause();
			for ( HqlParser.FromClauseSpaceContext parserSpace : parserFromClause.fromClauseSpace() ) {
				final SqmRoot sqmPathRoot = visitFromClauseSpace( parserSpace );
				fromClause.addRoot( sqmPathRoot );
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
	public SqmRoot visitPathRoot(HqlParser.PathRootContext ctx) {
		final String name = ctx.entityName().getText();

		log.debugf( "Handling root path - %s", name );

		final EntityDomainType entityDescriptor = getCreationContext().getJpaMetamodel().resolveHqlEntityReference(
				name
		);

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

		final String alias = visitIdentificationVariableDef( ctx.identificationVariableDef() );

		//noinspection unchecked
		final SqmRoot sqmRoot = new SqmRoot(
				entityDescriptor,
				alias,
				creationContext.getNodeBuilder()
		);

		processingStateStack.getCurrent().getPathRegistry().register( sqmRoot );

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
				if ( identificationVariableToken.getType() != HqlParser.IDENTIFIER ) {
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

	@Override
	public final SqmCrossJoin visitCrossJoin(HqlParser.CrossJoinContext ctx) {
		throw new UnsupportedOperationException( "Unexpected call to #visitCrossJoin, see #consumeCrossJoin" );
	}

	@SuppressWarnings("unchecked")
	private void consumeCrossJoin(HqlParser.CrossJoinContext parserJoin, SqmRoot sqmRoot) {
		final String name = parserJoin.pathRoot().entityName().getText();

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
		if ( joinTypeQualifier.OUTER() != null ) {
			// for outer joins, only left outer joins are currently supported
			if ( joinTypeQualifier.FULL() != null ) {
				throw new SemanticException( "FULL OUTER joins are not yet supported : " + parserJoin.getText() );
			}
			if ( joinTypeQualifier.RIGHT() != null ) {
				throw new SemanticException( "RIGHT OUTER joins are not yet supported : " + parserJoin.getText() );
			}

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
			final SqmQualifiedJoin join = (SqmQualifiedJoin) parserJoin.qualifiedJoinRhs().path().accept( this );

			// we need to set the alias here because the path could be treated - the treat operator is
			// not consumed by the identifierConsumer
			join.setExplicitAlias( alias );

			if ( join instanceof SqmEntityJoin ) {
				//noinspection unchecked
				sqmRoot.addSqmJoin( join );
			}
			else {
				if ( getCreationOptions().useStrictJpaCompliance() ) {
					if ( join.getExplicitAlias() != null ){
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
				(SqmPath) ctx.expression().accept( this ),
				ctx.NOT() != null,
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmComparisonPredicate visitEqualityPredicate(HqlParser.EqualityPredicateContext ctx) {
		final SqmExpression lhs = (SqmExpression) ctx.expression().get( 0 ).accept( this );
		final SqmExpression rhs = (SqmExpression) ctx.expression().get( 1 ).accept( this );

		return new SqmComparisonPredicate( lhs, ComparisonOperator.EQUAL, rhs, creationContext.getNodeBuilder() );
	}

	@Override
	public Object visitInequalityPredicate(HqlParser.InequalityPredicateContext ctx) {
		final SqmExpression lhs = (SqmExpression) ctx.expression().get( 0 ).accept( this );
		final SqmExpression rhs = (SqmExpression) ctx.expression().get( 1 ).accept( this );

		return new SqmComparisonPredicate( lhs, ComparisonOperator.NOT_EQUAL, rhs, creationContext.getNodeBuilder() );
	}

	@Override
	public Object visitGreaterThanPredicate(HqlParser.GreaterThanPredicateContext ctx) {
		final SqmExpression lhs = (SqmExpression) ctx.expression().get( 0 ).accept( this );
		final SqmExpression rhs = (SqmExpression) ctx.expression().get( 1 ).accept( this );

		return new SqmComparisonPredicate( lhs, ComparisonOperator.GREATER_THAN, rhs, creationContext.getNodeBuilder() );
	}

	@Override
	public Object visitGreaterThanOrEqualPredicate(HqlParser.GreaterThanOrEqualPredicateContext ctx) {
		final SqmExpression lhs = (SqmExpression) ctx.expression().get( 0 ).accept( this );
		final SqmExpression rhs = (SqmExpression) ctx.expression().get( 1 ).accept( this );

		return new SqmComparisonPredicate( lhs, ComparisonOperator.GREATER_THAN_OR_EQUAL, rhs, creationContext.getNodeBuilder() );
	}

	@Override
	public Object visitLessThanPredicate(HqlParser.LessThanPredicateContext ctx) {
		final SqmExpression lhs = (SqmExpression) ctx.expression().get( 0 ).accept( this );
		final SqmExpression rhs = (SqmExpression) ctx.expression().get( 1 ).accept( this );

		return new SqmComparisonPredicate( lhs, ComparisonOperator.LESS_THAN, rhs, creationContext.getNodeBuilder() );
	}

	@Override
	public Object visitLessThanOrEqualPredicate(HqlParser.LessThanOrEqualPredicateContext ctx) {
		final SqmExpression lhs = (SqmExpression) ctx.expression().get( 0 ).accept( this );
		final SqmExpression rhs = (SqmExpression) ctx.expression().get( 1 ).accept( this );

		return new SqmComparisonPredicate( lhs, ComparisonOperator.LESS_THAN_OR_EQUAL, rhs, creationContext.getNodeBuilder() );
	}

	@Override
	public SqmPredicate visitLikePredicate(HqlParser.LikePredicateContext ctx) {
		if ( ctx.likeEscape() != null ) {
			return new SqmLikePredicate(
					(SqmExpression) ctx.expression().get( 0 ).accept( this ),
					(SqmExpression) ctx.expression().get( 1 ).accept( this ),
					(SqmExpression) ctx.likeEscape().expression().accept( this ),
					creationContext.getNodeBuilder()
			);
		}
		else {
			return new SqmLikePredicate(
					(SqmExpression) ctx.expression().get( 0 ).accept( this ),
					(SqmExpression) ctx.expression().get( 1 ).accept( this ),
					creationContext.getNodeBuilder()
			);
		}
	}

	@Override
	public SqmPredicate visitMemberOfPredicate(HqlParser.MemberOfPredicateContext ctx) {
		final SqmPath sqmPluralPath = consumeDomainPath( ctx.path() );

		if ( sqmPluralPath.getReferencedPathSource() instanceof PluralPersistentAttribute ) {
			return new SqmMemberOfPredicate( sqmPluralPath, creationContext.getNodeBuilder() );
		}
		else {
			throw new SemanticException( "Path argument to MEMBER OF must be a plural attribute" );
		}
	}

	@Override
	public SqmPredicate visitInPredicate(HqlParser.InPredicateContext ctx) {
		final SqmExpression testExpression = (SqmExpression) ctx.expression().accept( this );

		if ( ctx.inList() instanceof HqlParser.ExplicitTupleInListContext ) {
			final HqlParser.ExplicitTupleInListContext tupleExpressionListContext = (HqlParser.ExplicitTupleInListContext) ctx.inList();

			parameterDeclarationContextStack.push( () -> tupleExpressionListContext.expression().size() == 1 );
			try {
				final List<SqmExpression<?>> listExpressions = new ArrayList<>( tupleExpressionListContext.expression().size() );
				for ( HqlParser.ExpressionContext expressionContext : tupleExpressionListContext.expression() ) {
					final SqmExpression listItemExpression = (SqmExpression) expressionContext.accept( this );

					listExpressions.add( listItemExpression );
				}

				//noinspection unchecked
				return new SqmInListPredicate( testExpression, listExpressions, creationContext.getNodeBuilder() );
			}
			finally {
				parameterDeclarationContextStack.pop();
			}
		}
		else if ( ctx.inList() instanceof HqlParser.SubQueryInListContext ) {
			final HqlParser.SubQueryInListContext subQueryContext = (HqlParser.SubQueryInListContext) ctx.inList();
			final SqmExpression subQueryExpression = (SqmExpression) subQueryContext.expression().accept( this );

			if ( !(subQueryExpression instanceof SqmSubQuery) ) {
				throw new ParsingException(
						"Was expecting a SubQueryExpression, but found " + subQueryExpression.getClass().getSimpleName()
								+ " : " + subQueryContext.expression().toString()
				);
			}

			//noinspection unchecked
			return new SqmInSubQueryPredicate(
					testExpression,
					(SqmSubQuery) subQueryExpression,
					creationContext.getNodeBuilder()
			);
		}

		// todo : handle PersistentCollectionReferenceInList labeled branch

		throw new ParsingException( "Unexpected IN predicate type [" + ctx.getClass().getSimpleName() + "] : " + ctx.getText() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object visitEntityTypeExpression(HqlParser.EntityTypeExpressionContext ctx) {
		// can be one of 2 forms:
		//		1) TYPE( some.path )
		//		2) TYPE( :someParam )
		if ( ctx.entityTypeReference().parameter() != null ) {
			// we have form (2)
			return new SqmEntityType(
					(SqmParameter) ctx.entityTypeReference().parameter().accept( this ),
					creationContext.getNodeBuilder()
			);
		}
		else if ( ctx.entityTypeReference().path() != null ) {
			// we have form (1)
			return new SqmEntityType(
					(SqmPath<?>) ctx.entityTypeReference().path().accept( this ),
					creationContext.getNodeBuilder()
			);
		}

		throw new ParsingException( "Could not interpret grammar context as 'entity type' expression : " + ctx.getText() );
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

		//noinspection unchecked
		return getFunctionTemplate( "concat" ).makeSqmFunctionExpression(
				(List) asList(
						ctx.expression( 0 ).accept( this ),
						ctx.expression( 1 ).accept( this )
				),
				resolveExpressableTypeBasic( String.class ),
				creationContext.getQueryEngine()
		);
	}

	@Override
	public SqmBinaryArithmetic<?> visitAdditionExpression(HqlParser.AdditionExpressionContext ctx) {
		if ( ctx.expression().size() != 2 ) {
			throw new ParsingException( "Expecting 2 operands to the + operator" );
		}

		return new SqmBinaryArithmetic<>(
				BinaryArithmeticOperator.ADD,
				(SqmExpression) ctx.expression( 0 ).accept( this ),
				(SqmExpression) ctx.expression( 1 ).accept( this ),
				creationContext.getJpaMetamodel(),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmBinaryArithmetic<?> visitSubtractionExpression(HqlParser.SubtractionExpressionContext ctx) {
		if ( ctx.expression().size() != 2 ) {
			throw new ParsingException( "Expecting 2 operands to the - operator" );
		}

		return new SqmBinaryArithmetic<>(
				BinaryArithmeticOperator.SUBTRACT,
				(SqmExpression) ctx.expression( 0 ).accept( this ),
				(SqmExpression) ctx.expression( 1 ).accept( this ),
				creationContext.getJpaMetamodel(),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmBinaryArithmetic visitMultiplicationExpression(HqlParser.MultiplicationExpressionContext ctx) {
		if ( ctx.expression().size() != 2 ) {
			throw new ParsingException( "Expecting 2 operands to the * operator" );
		}

		return new SqmBinaryArithmetic<>(
				BinaryArithmeticOperator.MULTIPLY,
				(SqmExpression) ctx.expression( 0 ).accept( this ),
				(SqmExpression) ctx.expression( 1 ).accept( this ),
				creationContext.getJpaMetamodel(),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmBinaryArithmetic<?> visitDivisionExpression(HqlParser.DivisionExpressionContext ctx) {
		if ( ctx.expression().size() != 2 ) {
			throw new ParsingException( "Expecting 2 operands to the / operator" );
		}

		//noinspection unchecked
		return new SqmBinaryArithmetic(
				BinaryArithmeticOperator.DIVIDE,
				(SqmExpression) ctx.expression( 0 ).accept( this ),
				(SqmExpression) ctx.expression( 1 ).accept( this ),
				creationContext.getJpaMetamodel(),
				creationContext.getNodeBuilder()
		);
	}

	@Override
	public SqmExpression<?> visitModuloExpression(HqlParser.ModuloExpressionContext ctx) {
		if ( ctx.expression().size() != 2 ) {
			throw new ParsingException( "Expecting 2 operands to the % operator" );
		}

		SqmExpression<?> dividend = (SqmExpression) ctx.expression( 0).accept( this);
		SqmExpression<?> divisor = (SqmExpression) ctx.expression( 1 ).accept( this );

		//noinspection unchecked
		return getFunctionTemplate("mod").makeSqmFunctionExpression(
				asList( dividend, divisor ),
				(AllowableFunctionReturnType) dividend.getNodeType(),
				creationContext.getQueryEngine()
		);
	}

	@Override
	public SqmUnaryOperation<?> visitUnaryPlusExpression(HqlParser.UnaryPlusExpressionContext ctx) {
		return new SqmUnaryOperation<>(
				UnaryArithmeticOperator.UNARY_PLUS,
				(SqmExpression<?>) ctx.expression().accept( this )
		);
	}

	@Override
	public SqmUnaryOperation visitUnaryMinusExpression(HqlParser.UnaryMinusExpressionContext ctx) {
		return new SqmUnaryOperation<>(
				UnaryArithmeticOperator.UNARY_MINUS,
				(SqmExpression<?>) ctx.expression().accept( this )
		);
	}

	@Override
	public SqmCaseSimple visitSimpleCaseStatement(HqlParser.SimpleCaseStatementContext ctx) {
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
	public SqmCaseSearched visitSearchedCaseStatement(HqlParser.SearchedCaseStatementContext ctx) {
		final SqmCaseSearched<?> caseExpression = new SqmCaseSearched<>( creationContext.getNodeBuilder());

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
		return getFunctionTemplate("current_date")
				.makeSqmFunctionExpression(
						resolveExpressableTypeBasic( Date.class ),
						creationContext.getQueryEngine()
				);
	}

	@Override
	public SqmExpression visitCurrentTimeFunction(HqlParser.CurrentTimeFunctionContext ctx) {
		return getFunctionTemplate("current_time")
				.makeSqmFunctionExpression(
						resolveExpressableTypeBasic( Time.class ),
						creationContext.getQueryEngine()
				);
	}

	@Override
	public SqmExpression visitCurrentTimestampFunction(HqlParser.CurrentTimestampFunctionContext ctx) {
		return getFunctionTemplate("current_timestamp")
				.makeSqmFunctionExpression(
						resolveExpressableTypeBasic( Timestamp.class ),
						creationContext.getQueryEngine()
				);
	}

	@Override
	public SqmExpression visitCurrentInstantFunction(HqlParser.CurrentInstantFunctionContext ctx) {
		return getFunctionTemplate("current_timestamp")
				.makeSqmFunctionExpression(
						resolveExpressableTypeBasic( Instant.class ),
						creationContext.getQueryEngine()
				);
	}

	@Override
	public Object visitLeastFunction(HqlParser.LeastFunctionContext ctx) {
		final List<SqmTypedNode<?>> arguments = new ArrayList<>();

		SqmExpressable<?> type = null;

		for ( HqlParser.ExpressionContext argument : ctx.expression() ) {
			SqmTypedNode arg = (SqmTypedNode) argument.accept( this);
			arguments.add(arg);
			//TODO: do something better here!
			type = arg.getNodeType();
		}

		return getFunctionTemplate("least")
				.makeSqmFunctionExpression(
						arguments,
						(AllowableFunctionReturnType<?>) type,
						creationContext.getQueryEngine()
				);
	}

	@Override
	public Object visitGreatestFunction(HqlParser.GreatestFunctionContext ctx) {
		final List<SqmTypedNode<?>> arguments = new ArrayList<>();

		SqmExpressable<?> type = null;

		for ( HqlParser.ExpressionContext argument : ctx.expression() ) {
			SqmTypedNode arg = (SqmTypedNode) argument.accept( this);
			arguments.add(arg);
			//TODO: do something better here!
			type = arg.getNodeType();
		}

		return getFunctionTemplate("greatest")
				.makeSqmFunctionExpression(
						arguments,
						(AllowableFunctionReturnType<?>) type,
						creationContext.getQueryEngine()
				);
	}

	@Override
	public SqmExpression visitCoalesceExpression(HqlParser.CoalesceExpressionContext ctx) {
		final List<SqmTypedNode<?>> arguments = new ArrayList<>();

		SqmExpressable<?> type = null;

		for ( HqlParser.ExpressionContext argument : ctx.coalesce().expression() ) {
			SqmTypedNode arg = (SqmTypedNode) argument.accept( this);
			arguments.add(arg);
			//TODO: do something better here!
			type = arg.getNodeType();
		}

		return getFunctionTemplate("coalesce")
				.makeSqmFunctionExpression(
						arguments,
						(AllowableFunctionReturnType<?>) type,
						creationContext.getQueryEngine()
				);
	}

	@Override
	public SqmExpression visitNullIfExpression(HqlParser.NullIfExpressionContext ctx) {
		final SqmExpression arg1 = (SqmExpression) ctx.nullIf().expression( 0 ).accept( this );
		final SqmExpression arg2 = (SqmExpression) ctx.nullIf().expression( 1 ).accept( this );

		//noinspection unchecked
		return getFunctionTemplate("nullif").makeSqmFunctionExpression(
				asList( arg1, arg2 ),
				(AllowableFunctionReturnType) arg1.getNodeType(),
				creationContext.getQueryEngine()
		);
	}

	@Override
	public SqmLiteral visitLiteralExpression(HqlParser.LiteralExpressionContext ctx) {
		if ( ctx.literal().CHARACTER_LITERAL() != null ) {
			return characterLiteral( ctx.literal().CHARACTER_LITERAL().getText() );
		}
		else if ( ctx.literal().STRING_LITERAL() != null ) {
			return stringLiteral( ctx.literal().STRING_LITERAL().getText() );
		}
		else if ( ctx.literal().INTEGER_LITERAL() != null ) {
			return integerLiteral( ctx.literal().INTEGER_LITERAL().getText() );
		}
		else if ( ctx.literal().LONG_LITERAL() != null ) {
			return longLiteral( ctx.literal().LONG_LITERAL().getText() );
		}
		else if ( ctx.literal().BIG_INTEGER_LITERAL() != null ) {
			return bigIntegerLiteral( ctx.literal().BIG_INTEGER_LITERAL().getText() );
		}
		else if ( ctx.literal().HEX_LITERAL() != null ) {
			final String text = ctx.literal().HEX_LITERAL().getText();
			if ( text.endsWith( "l" ) || text.endsWith( "L" ) ) {
				return longLiteral( text );
			}
			else {
				return integerLiteral( text );
			}
		}
		else if ( ctx.literal().OCTAL_LITERAL() != null ) {
			final String text = ctx.literal().OCTAL_LITERAL().getText();
			if ( text.endsWith( "l" ) || text.endsWith( "L" ) ) {
				return longLiteral( text );
			}
			else {
				return integerLiteral( text );
			}
		}
		else if ( ctx.literal().FLOAT_LITERAL() != null ) {
			return floatLiteral( ctx.literal().FLOAT_LITERAL().getText() );
		}
		else if ( ctx.literal().DOUBLE_LITERAL() != null ) {
			return doubleLiteral( ctx.literal().DOUBLE_LITERAL().getText() );
		}
		else if ( ctx.literal().BIG_DECIMAL_LITERAL() != null ) {
			return bigDecimalLiteral( ctx.literal().BIG_DECIMAL_LITERAL().getText() );
		}
		else if ( ctx.literal().FALSE() != null ) {
			return booleanLiteral( false );
		}
		else if ( ctx.literal().TRUE() != null ) {
			return booleanLiteral( true );
		}
		else if ( ctx.literal().NULL() != null ) {
			return new SqmLiteralNull( creationContext.getQueryEngine().getCriteriaBuilder() );
		}
		else if ( ctx.literal().timestampLiteral() != null ) {
			return LiteralHelper.timestampLiteralFrom( ctx.literal().timestampLiteral().dateTimeLiteralText().getText(), this );
		}
		else if ( ctx.literal().dateLiteral() != null ) {
			return LiteralHelper.dateLiteralFrom( ctx.literal().dateLiteral().dateTimeLiteralText().getText(), this );
		}
		else if ( ctx.literal().timeLiteral() != null ) {
			return LiteralHelper.timeLiteralFrom( ctx.literal().timeLiteral().dateTimeLiteralText().getText(), this );
		}

		// otherwise we have a problem
		throw new ParsingException( "Unexpected literal expression type [" + ctx.getText() + "]" );
	}

	private SqmLiteral<Boolean> booleanLiteral(boolean value) {
		final SqmExpressable expressionType = resolveExpressableTypeBasic( Boolean.class );
		//noinspection unchecked
		return new SqmLiteral<>( value, expressionType, creationContext.getQueryEngine().getCriteriaBuilder() );
	}

	private SqmLiteral<Character> characterLiteral(String text) {
		if ( text.length() > 1 ) {
			// todo : or just treat it as a String literal?
			throw new ParsingException( "Value for CHARACTER_LITERAL token was more than 1 character" );
		}
		return new SqmLiteral<>(
				text.charAt( 0 ),
				resolveExpressableTypeBasic( Character.class ),
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

	@SuppressWarnings("WeakerAccess")
	protected SqmLiteral<Integer> integerLiteral(String text) {
		try {
			final Integer value = Integer.valueOf( text );
			return new SqmLiteral<>(
					value,
					resolveExpressableTypeBasic( Integer.class ),
					creationContext.getQueryEngine().getCriteriaBuilder()
			);
		}
		catch (NumberFormatException e) {
			throw new LiteralNumberFormatException(
					"Unable to convert sqm literal [" + text + "] to Integer",
					e
			);
		}
	}

	@SuppressWarnings("WeakerAccess")
	protected SqmLiteral<Long> longLiteral(String text) {
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

	@SuppressWarnings("WeakerAccess")
	protected SqmLiteral<BigInteger> bigIntegerLiteral(String text) {
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

	@SuppressWarnings("WeakerAccess")
	protected SqmLiteral<Float> floatLiteral(String text) {
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

	@SuppressWarnings("WeakerAccess")
	protected SqmLiteral<Double> doubleLiteral(String text) {
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

	@SuppressWarnings("WeakerAccess")
	protected SqmLiteral<BigDecimal> bigDecimalLiteral(String text) {
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
		//noinspection unchecked
		return creationContext.getJpaMetamodel().getTypeConfiguration().standardBasicTypeForJavaType( javaType );
	}

	@Override
	public Object visitParameterExpression(HqlParser.ParameterExpressionContext ctx) {
		return ctx.parameter().accept( this );
	}

	@Override
	public SqmNamedParameter visitNamedParameter(HqlParser.NamedParameterContext ctx) {
		final SqmNamedParameter<?> param = new SqmNamedParameter<>(
				ctx.identifier().getText(),
				parameterDeclarationContextStack.getCurrent().isMultiValuedBindingAllowed(),
				creationContext.getNodeBuilder()
		);
		parameterCollector.addParameter( param );
		return param;
	}

	@Override
	public SqmPositionalParameter visitPositionalParameter(HqlParser.PositionalParameterContext ctx) {
		if ( ctx.INTEGER_LITERAL() == null ) {
			throw new SemanticException( "Encountered positional parameter which did not declare position (? instead of, e.g., ?1)" );
		}
		final SqmPositionalParameter<?> param = new SqmPositionalParameter<>(
				Integer.valueOf( ctx.INTEGER_LITERAL().getText() ),
				parameterDeclarationContextStack.getCurrent().isMultiValuedBindingAllowed(),
				creationContext.getNodeBuilder()
		);
		parameterCollector.addParameter( param );
		return param;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Functions

	@Override
	public SqmExpression visitJpaNonStandardFunction(HqlParser.JpaNonStandardFunctionContext ctx) {
		final String functionName = ctx.jpaNonStandardFunctionName().STRING_LITERAL().getText().toLowerCase();
		final List<SqmTypedNode<?>> functionArguments = visitNonStandardFunctionArguments( ctx.nonStandardFunctionArguments() );

		SqmFunctionTemplate functionTemplate = getFunctionTemplate( functionName );
		if (functionTemplate == null) {
			functionTemplate = new NamedSqmFunctionTemplate( functionName, true, null, null );
		}
		return functionTemplate.makeSqmFunctionExpression(
				functionArguments,
				null,
				creationContext.getQueryEngine()
		);
	}

	@Override
	public SqmExpression visitNonStandardFunction(HqlParser.NonStandardFunctionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw new StrictJpaComplianceViolation(
					"Encountered non-compliant non-standard function call [" +
							ctx.nonStandardFunctionName() + "], but strict JPQL compliance was requested; use JPA's FUNCTION(functionName[,...]) syntax name instead",
					StrictJpaComplianceViolation.Type.FUNCTION_CALL
			);
		}

		final String functionName = ctx.nonStandardFunctionName().getText().toLowerCase();
		final List<SqmTypedNode<?>> functionArguments = visitNonStandardFunctionArguments( ctx.nonStandardFunctionArguments() );

		SqmFunctionTemplate functionTemplate = getFunctionTemplate( functionName);
		if (functionTemplate == null) {
			functionTemplate = new NamedSqmFunctionTemplate( functionName, true, null, null );
		}
		return functionTemplate.makeSqmFunctionExpression(
				functionArguments,
				null,
				creationContext.getQueryEngine()
		);
	}

	@Override
	public List<SqmTypedNode<?>> visitNonStandardFunctionArguments(HqlParser.NonStandardFunctionArgumentsContext ctx) {
		final List<SqmTypedNode<?>> arguments = new ArrayList<>();

		for ( int i=0, size=ctx.expression().size(); i<size; i++ ) {
			// we handle the final argument differently...
			if ( i == size-1 ) {
				arguments.add( visitFinalFunctionArgument( ctx.expression( i ) ) );
			}
			else {
				arguments.add( (SqmTypedNode) ctx.expression( i ).accept( this ) );
			}
		}

		return arguments;
	}

	private SqmExpression visitFinalFunctionArgument(HqlParser.ExpressionContext expression) {
		// the final argument to a function may accept multi-value parameter (varargs),
		// 		but only if we are operating in non-strict JPA mode
		parameterDeclarationContextStack.push( creationOptions::useStrictJpaCompliance );
		try {
			return (SqmExpression) expression.accept( this );
		}
		finally {
			parameterDeclarationContextStack.pop();
		}
	}

	@Override
	public SqmExpression visitCeilingFunction(HqlParser.CeilingFunctionContext ctx) {
		final SqmExpression arg = (SqmExpression) ctx.expression().accept( this );

		//noinspection unchecked
		return getFunctionTemplate("ceiling").makeSqmFunctionExpression(
				arg,
				(AllowableFunctionReturnType) resolveExpressableTypeBasic( Long.class ),
				creationContext.getQueryEngine()
		);
	}

	@Override
	public SqmExpression visitFloorFunction(HqlParser.FloorFunctionContext ctx) {
		final SqmExpression arg = (SqmExpression) ctx.expression().accept( this );

		//noinspection unchecked
		return getFunctionTemplate("floor").makeSqmFunctionExpression(
				arg,
				(AllowableFunctionReturnType) resolveExpressableTypeBasic( Long.class ),
				creationContext.getQueryEngine()
		);
	}

	private SqmFunctionTemplate getFunctionTemplate(String name) {
		return creationContext.getQueryEngine().getSqmFunctionRegistry().findFunctionTemplate( name );
	}

	@Override
	public SqmExpression visitAbsFunction(HqlParser.AbsFunctionContext ctx) {
		final SqmExpression arg = (SqmExpression) ctx.expression().accept( this );

		//noinspection unchecked
		return getFunctionTemplate("abs").makeSqmFunctionExpression(
				arg,
				(AllowableFunctionReturnType) arg.getNodeType(),
				creationContext.getQueryEngine()
		);
	}

	@Override
	public SqmExpression visitSignFunction(HqlParser.SignFunctionContext ctx) {
		final SqmExpression arg = (SqmExpression) ctx.expression().accept( this );

		//noinspection unchecked
		return getFunctionTemplate("sign").makeSqmFunctionExpression(
				arg,
				(AllowableFunctionReturnType) resolveExpressableTypeBasic( Integer.class ),
				creationContext.getQueryEngine()
		);
	}

	@Override
	public SqmExpression visitModFunction(HqlParser.ModFunctionContext ctx) {
		final SqmExpression dividend = (SqmExpression) ctx.modDividendArgument().accept( this );
		final SqmExpression divisor = (SqmExpression) ctx.modDivisorArgument().accept( this );

		//noinspection unchecked
		return getFunctionTemplate("mod").makeSqmFunctionExpression(
				asList( dividend, divisor ),
				(AllowableFunctionReturnType) dividend.getNodeType(),
				creationContext.getQueryEngine()
		);
	}

	@Override
	public SqmExpression visitPowerFunction(HqlParser.PowerFunctionContext ctx) {
		final SqmExpression base = (SqmExpression) ctx.powerBaseArgument().accept( this );
		final SqmExpression power = (SqmExpression) ctx.powerPowerArgument().accept( this );

		//noinspection unchecked
		return getFunctionTemplate("power").makeSqmFunctionExpression(
				asList( base, power ),
				(AllowableFunctionReturnType) base.getNodeType(),
				creationContext.getQueryEngine()
		);
	}

	@Override
	public SqmExpression visitTrigFunction(HqlParser.TrigFunctionContext ctx) {
		final SqmExpression arg = (SqmExpression) ctx.expression().accept( this );

		return getFunctionTemplate( ctx.trigFunctionName().getText() ).makeSqmFunctionExpression(
				arg,
				resolveExpressableTypeBasic( Double.class ),
				creationContext.getQueryEngine()
		);
	}

	@Override
	public SqmExpression visitSqrtFunction(HqlParser.SqrtFunctionContext ctx) {
		final SqmExpression arg = (SqmExpression) ctx.expression().accept( this );

		//noinspection unchecked
		return getFunctionTemplate("sqrt").makeSqmFunctionExpression(
				arg,
				(AllowableFunctionReturnType) arg.getNodeType(),
				creationContext.getQueryEngine()
		);
	}

	@Override
	public SqmExpression visitRoundFunction(HqlParser.RoundFunctionContext ctx) {
		final SqmExpression arg = (SqmExpression) ctx.expression().accept( this );
		final SqmExpression precision = (SqmExpression) ctx.roundFunctionPrecision().expression().accept( this );

		//noinspection unchecked
		return getFunctionTemplate("round").makeSqmFunctionExpression(
				asList(arg, precision),
				(AllowableFunctionReturnType) arg.getNodeType(),
				creationContext.getQueryEngine()
		);
	}

	@Override
	public SqmExpression visitAtan2Function(HqlParser.Atan2FunctionContext ctx) {
		final SqmExpression sin = (SqmExpression) ctx.expression().get( 0).accept( this );
		final SqmExpression cos = (SqmExpression) ctx.expression().get( 1).accept( this );

		//noinspection unchecked
		return getFunctionTemplate("atan2").makeSqmFunctionExpression(
				asList(sin, cos),
				(AllowableFunctionReturnType) sin.getNodeType(),
				creationContext.getQueryEngine()
		);
	}

	@Override
	public SqmExpression visitLnFunction(HqlParser.LnFunctionContext ctx) {
		final SqmExpression arg = (SqmExpression) ctx.expression().accept( this );

		//noinspection unchecked
		return getFunctionTemplate("ln").makeSqmFunctionExpression(
				arg,
				(AllowableFunctionReturnType) arg.getNodeType(),
				creationContext.getQueryEngine()
		);

	}

	@Override
	public SqmExpression visitExpFunction(HqlParser.ExpFunctionContext ctx) {
		final SqmExpression arg = (SqmExpression) ctx.expression().accept( this );

		//noinspection unchecked
		return getFunctionTemplate("exp").makeSqmFunctionExpression(
				arg,
				(AllowableFunctionReturnType) arg.getNodeType(),
				creationContext.getQueryEngine()
		);
	}

	@Override
	public Object visitDatetimeField(HqlParser.DatetimeFieldContext ctx) {
		NodeBuilder nodeBuilder = creationContext.getNodeBuilder();
		if (ctx.DAY()!=null) {
			return new SqmExtractUnit<>(
					"day",
					resolveExpressableTypeBasic( Integer.class ),
					nodeBuilder
			);
		}
		if (ctx.MONTH()!=null) {
			return new SqmExtractUnit<>(
					"month",
					resolveExpressableTypeBasic( Integer.class ),
					nodeBuilder
			);
		}
		if (ctx.YEAR()!=null) {
			return new SqmExtractUnit<>(
					"year",
					resolveExpressableTypeBasic( Integer.class ),
					nodeBuilder
			);
		}
		if (ctx.HOUR()!=null) {
			return new SqmExtractUnit<>(
					"hour",
					resolveExpressableTypeBasic( Integer.class ),
					nodeBuilder
			);
		}
		if (ctx.MINUTE()!=null) {
			return new SqmExtractUnit<>(
					"minute",
					resolveExpressableTypeBasic( Integer.class ),
					nodeBuilder
			);
		}
		if (ctx.SECOND()!=null) {
			return new SqmExtractUnit<>(
					"second",
					resolveExpressableTypeBasic( Float.class ),
					nodeBuilder
			);
		}
		if (ctx.WEEK()!=null) {
			return new SqmExtractUnit<>(
					"week",
					resolveExpressableTypeBasic( Integer.class ),
					nodeBuilder
			);
		}
		if (ctx.QUARTER()!=null) {
			return new SqmExtractUnit<>(
					"quarter",
					resolveExpressableTypeBasic( Integer.class ),
					nodeBuilder
			);
		}

		return super.visitDatetimeField(ctx);
	}

	@Override
	public Object visitSecondsField(HqlParser.SecondsFieldContext ctx) {
		NodeBuilder nodeBuilder = creationContext.getNodeBuilder();
		if (ctx.MICROSECOND()!=null) {
			//TODO: need to go back to the dialect, it's called "microseconds" on some
			return new SqmExtractUnit<>(
					"microsecond",
					resolveExpressableTypeBasic( Integer.class ),
					nodeBuilder
			);
		}
		if (ctx.MILLISECOND()!=null) {
			//TODO: need to go back to the dialect, it's called "milliseconds" on some
			return new SqmExtractUnit<>(
					"millisecond",
					resolveExpressableTypeBasic( Integer.class ),
					nodeBuilder
			);
		}
		return super.visitSecondsField(ctx);
	}

	@Override
	public Object visitTimeZoneField(HqlParser.TimeZoneFieldContext ctx) {
		NodeBuilder nodeBuilder = creationContext.getNodeBuilder();
		if (ctx.TIMEZONE_HOUR()!=null) {
			return new SqmExtractUnit<>(
					"timezone_hour",
					resolveExpressableTypeBasic( Integer.class ),
					nodeBuilder
			);
		}
		if (ctx.TIMEZONE_MINUTE()!=null) {
			return new SqmExtractUnit<>(
					"timezone_minute",
					resolveExpressableTypeBasic( Integer.class ),
					nodeBuilder
			);
		}
		return super.visitTimeZoneField(ctx);
	}

	@Override
	public Object visitExtractFunction(HqlParser.ExtractFunctionContext ctx) {

		final SqmExpression<?> expressionToExtract = (SqmExpression) ctx.expression().accept( this );
		final SqmExtractUnit<?> extractFieldExpression;
		if ( ctx.extractField() != null ) {
			extractFieldExpression = (SqmExtractUnit) ctx.extractField().accept( this);
		}
		else if ( ctx.datetimeField() != null ) {
			extractFieldExpression = (SqmExtractUnit) ctx.datetimeField().accept( this);
		}
		else {
			return expressionToExtract;
		}

		//noinspection unchecked
		return getFunctionTemplate("extract").makeSqmFunctionExpression(
				asList( extractFieldExpression, expressionToExtract ),
				extractFieldExpression.getType(),
				creationContext.getQueryEngine()
		);
	}

	@Override
	public SqmExpression visitCastFunction(HqlParser.CastFunctionContext ctx) {

		final SqmExpression<?> expressionToCast = (SqmExpression) ctx.expression().accept( this );
		final SqmCastTarget<?> castTargetExpression = interpretCastTarget( ctx.castTarget() );

		//getSessionFactory().getTypeConfiguration().resolveCastTargetType( ctx.dataType().IDENTIFIER().getText() )

//		if ( !AllowableFunctionReturnType.class.isInstance( castTargetExpression ) ) {
//			throw new SqmProductionException( "Found cast target expression [%s] which is not allowed as a function return" );
//		}

		return getFunctionTemplate("cast").makeSqmFunctionExpression(
				asList( expressionToCast, castTargetExpression ),
				castTargetExpression.getType(),
				creationContext.getQueryEngine()
		);
	}

	private SqmCastTarget<?> interpretCastTarget(HqlParser.CastTargetContext castTargetContext) {
		// todo (6.0) : what are the allowable forms of specifying cast-target?
		// 		the exactness of this is being discussed on the dev ml.  Most
		//		likely we will accept either:
		//			1) a String, which might represent:
		//				a) a Java type name
		//				b) a java.sql.Types field name (e.g. VARCHAR...)
		//				c) (not huge fan of this...) a "pass-thru" value
		//			2) an int signifying the SqlTypeDescriptor (JDBC type code)

		String text = castTargetContext.identifier().getText();
		NodeBuilder nodeBuilder = creationContext.getNodeBuilder();
		switch (text.toLowerCase()) {
			case "string": {
				return new SqmCastTarget<>(
						resolveExpressableTypeBasic( String.class ),
						nodeBuilder
				);
			}
			case "integer": {
				return new SqmCastTarget<>(
						resolveExpressableTypeBasic( Integer.class ),
						nodeBuilder
				);
			}
			case "long": {
				return new SqmCastTarget<>(
						resolveExpressableTypeBasic( Long.class ), nodeBuilder
				);
			}
			case "float": {
				return new SqmCastTarget<>(
						resolveExpressableTypeBasic( Float.class ),
						nodeBuilder
				);
			}
			case "double": {
				return new SqmCastTarget<>(
						resolveExpressableTypeBasic( Double.class ),
						nodeBuilder
				);
			}
			case "time": {
				return new SqmCastTarget<>(
						resolveExpressableTypeBasic( Time.class ),
						nodeBuilder
				);
			}
			case "date": {
				return new SqmCastTarget<>(
						resolveExpressableTypeBasic( Date.class ),
						nodeBuilder
				);
			}
			case "timestamp": {
				return new SqmCastTarget<>(
						resolveExpressableTypeBasic( Timestamp.class ),
						nodeBuilder
				);
			}
			default: {
				throw new HqlInterpretationException( "unrecognized cast target type: " + text);
			}
		}
	}

	@Override
	public SqmExpression visitUpperFunction(HqlParser.UpperFunctionContext ctx) {
		final SqmExpression expression = (SqmExpression) ctx.expression().accept( this );
		//noinspection unchecked
		return getFunctionTemplate("upper").makeSqmFunctionExpression(
				expression,
				(AllowableFunctionReturnType) expression.getNodeType(),
				creationContext.getQueryEngine()
		);
	}

	@Override
	public SqmExpression visitLowerFunction(HqlParser.LowerFunctionContext ctx) {
		// todo (6.0) : why pass both the expression and its expression-type?
		//			can't we just pass the expression?
		final SqmExpression expression = (SqmExpression) ctx.expression().accept( this );
		//noinspection unchecked
		return getFunctionTemplate("lower").makeSqmFunctionExpression(
				expression,
				(AllowableFunctionReturnType) expression.getNodeType(),
				creationContext.getQueryEngine()
		);
	}

	@Override
	public SqmExpression visitConcatFunction(HqlParser.ConcatFunctionContext ctx) {
		final List<SqmTypedNode<?>> arguments = new ArrayList<>();
		for ( HqlParser.ExpressionContext argument : ctx.expression() ) {
			arguments.add( (SqmTypedNode<?>) argument.accept( this ) );
		}

		return getFunctionTemplate("concat").makeSqmFunctionExpression(
				arguments,
				resolveExpressableTypeBasic( String.class ),
				creationContext.getQueryEngine()
		);
	}

	@Override
	public Object visitLengthFunction(HqlParser.LengthFunctionContext ctx) {
		final SqmExpression arg = (SqmExpression) ctx.expression().accept( this );

		return getFunctionTemplate("character_length").makeSqmFunctionExpression(
				arg,
				resolveExpressableTypeBasic( Integer.class ),
				creationContext.getQueryEngine()
		);
	}

	@Override
	public Object visitPositionFunction(HqlParser.PositionFunctionContext ctx) {
		final SqmExpression<?> string = (SqmExpression) ctx.positionFunctionStringArgument().accept( this );
		final SqmExpression<?> pattern = (SqmExpression) ctx.positionFunctionPatternArgument().accept( this );

		return getFunctionTemplate("locate").makeSqmFunctionExpression(
				asList( pattern, string ),
				resolveExpressableTypeBasic( Integer.class ),
				creationContext.getQueryEngine()
		);
	}

	@Override
	public Object visitLocateFunction(HqlParser.LocateFunctionContext ctx) {
		final SqmExpression<?> string = (SqmExpression) ctx.locateFunctionStringArgument().accept( this );
		final SqmExpression<?> pattern = (SqmExpression) ctx.locateFunctionPatternArgument().accept( this );
		final SqmExpression<?> start = ctx.locateFunctionStartArgument() == null
				? null
				: (SqmExpression) ctx.locateFunctionStartArgument().accept( this );

		return getFunctionTemplate("locate").makeSqmFunctionExpression(
				start == null
						? asList( pattern, string )
						: asList( pattern, string, start ),
				resolveExpressableTypeBasic( Integer.class ),
				creationContext.getQueryEngine()
		);
	}

	@Override
	public Object visitReplaceFunction(HqlParser.ReplaceFunctionContext ctx) {

		final SqmExpression<?> string = (SqmExpression) ctx.replaceFunctionStringArgument().accept( this );
		final SqmExpression<?> pattern = (SqmExpression) ctx.replaceFunctionPatternArgument().accept( this );
		final SqmExpression<?> replacement = (SqmExpression) ctx.replaceFunctionReplacementArgument().accept( this );

		return getFunctionTemplate("replace").makeSqmFunctionExpression(
				asList( string, pattern, replacement ),
				resolveExpressableTypeBasic( String.class ),
				creationContext.getQueryEngine()
		);
	}

	@Override
	public Object visitStrFunction(HqlParser.StrFunctionContext ctx) {
		final SqmExpression<?> arg = (SqmExpression) ctx.expression().accept( this );
		AllowableFunctionReturnType<String> type = resolveExpressableTypeBasic( String.class );
		return getFunctionTemplate("cast").makeSqmFunctionExpression(
				asList( arg, new SqmCastTarget<>( type, creationContext.getNodeBuilder() ) ),
				type,
				creationContext.getQueryEngine()
		);
	}

	@Override
	public SqmExpression visitMaxFunction(HqlParser.MaxFunctionContext ctx) {
		final SqmExpression<?> arg = (SqmExpression) ctx.expression().accept( this );
		//ignore DISTINCT
		return getFunctionTemplate("max").makeSqmFunctionExpression(
				arg,
				(AllowableFunctionReturnType<?>) arg.getNodeType(),
				creationContext.getQueryEngine()
		);
	}

	@Override
	public SqmExpression visitMinFunction(HqlParser.MinFunctionContext ctx) {
		final SqmExpression<?> arg = (SqmExpression) ctx.expression().accept( this );
		//ignore DISTINCT
		return getFunctionTemplate("min").makeSqmFunctionExpression(
				arg,
				(AllowableFunctionReturnType<?>) arg.getNodeType(),
				creationContext.getQueryEngine()
		);
	}

	@Override
	public SqmExpression visitSumFunction(HqlParser.SumFunctionContext ctx) {

		final SqmExpression<?> arg = (SqmExpression) ctx.expression().accept( this );
		SqmTypedNode<?> argument = ctx.DISTINCT() != null ? new SqmDistinct<>( arg, getCreationContext().getNodeBuilder()) : arg;

		return getFunctionTemplate("sum").makeSqmFunctionExpression(
				argument,
				(AllowableFunctionReturnType<?>) arg.getNodeType(),
				creationContext.getQueryEngine()
		);
	}

	@Override
	public SqmExpression visitAvgFunction(HqlParser.AvgFunctionContext ctx) {

		final SqmExpression<?> arg = (SqmExpression) ctx.expression().accept( this );
		SqmTypedNode<?> argument = ctx.DISTINCT() != null ? new SqmDistinct<>( arg, getCreationContext().getNodeBuilder()) : arg;

		return getFunctionTemplate("avg").makeSqmFunctionExpression(
				argument,
				resolveExpressableTypeBasic( Double.class ),
				creationContext.getQueryEngine()
		);
	}

	@Override
	public SqmExpression visitCountFunction(HqlParser.CountFunctionContext ctx) {

		final SqmExpression<?> arg = ctx.ASTERISK() != null
				? new SqmStar( getCreationContext().getNodeBuilder() )
				: (SqmExpression) ctx.expression().accept( this );
		SqmTypedNode<?> argument = ctx.DISTINCT() != null ? new SqmDistinct<>( arg, getCreationContext().getNodeBuilder() ) : arg;

		return getFunctionTemplate("count").makeSqmFunctionExpression(
				argument,
				resolveExpressableTypeBasic( Long.class ),
				creationContext.getQueryEngine()
		);
	}

	@Override
	public SqmExpression visitSubstringFunction(HqlParser.SubstringFunctionContext ctx) {
		final SqmExpression source = (SqmExpression) ctx.expression().accept( this );
		final SqmExpression start = (SqmExpression) ctx.substringFunctionStartArgument().accept( this );
		final SqmExpression length = ctx.substringFunctionLengthArgument() == null
				? null
				: (SqmExpression) ctx.substringFunctionLengthArgument().accept( this );

		//noinspection unchecked
		return getFunctionTemplate("substring").makeSqmFunctionExpression(
				length==null ? asList( source, start ) : asList( source, start, length ),
				resolveExpressableTypeBasic( String.class ),
				creationContext.getQueryEngine()
		);

	}

	@Override
	public SqmExpression visitTrimFunction(HqlParser.TrimFunctionContext ctx) {
		final SqmExpression source = (SqmExpression) ctx.expression().accept( this );

		//noinspection unchecked
		return getFunctionTemplate("trim").makeSqmFunctionExpression(
				asList(
						interpretTrimSpecification( ctx.trimSpecification() ),
						visitTrimCharacter( ctx.trimCharacter() ),
						source
				),
				resolveExpressableTypeBasic( String.class ),
				creationContext.getQueryEngine()
		);
	}

	private SqmTrimSpecification interpretTrimSpecification(HqlParser.TrimSpecificationContext ctx) {
		TrimSpec spec = TrimSpec.BOTH;	// JPA says the default is BOTH

		if ( ctx.LEADING() != null ) {
			spec = TrimSpec.LEADING;
		}
		else if ( ctx.TRAILING() != null ) {
			spec = TrimSpec.TRAILING;
		}

		return new SqmTrimSpecification( spec, creationContext.getNodeBuilder() );
	}

	@Override
	public SqmLiteral<Character> visitTrimCharacter(HqlParser.TrimCharacterContext ctx) {
		// todo (6.0) : we should delay this until we are walking the SQM

		if ( ctx.CHARACTER_LITERAL() != null ) {
			final String trimCharText = ctx.CHARACTER_LITERAL().getText();
			if ( trimCharText.length() != 1 ) {
				throw new SemanticException( "Expecting [trim character] for TRIM function to be  single character, found : " + trimCharText );
			}
			return new SqmLiteral<>(
					trimCharText.charAt( 0 ),
					resolveExpressableTypeBasic( Character.class ),
					creationContext.getNodeBuilder()
			);
		}

		if ( ctx.STRING_LITERAL() != null ) {
			final String trimCharText = ctx.STRING_LITERAL().getText();
			if ( trimCharText.length() != 1 ) {
				throw new SemanticException( "Expecting [trim character] for TRIM function to be  single character, found : " + trimCharText );
			}
			return new SqmLiteral<>(
					trimCharText.charAt( 0 ),
					resolveExpressableTypeBasic( Character.class ),
					creationContext.getNodeBuilder()
			);
		}

		// JPA says space is the default
		return new SqmLiteral<>(
				' ',
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
	public SqmPath visitCollectionIndexFunction(HqlParser.CollectionIndexFunctionContext ctx) {
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
		return ( (PluralPersistentAttribute) pluralAttribute ).getIndexPathSource().createSqmPath(
				sqmFrom,
				this
		);
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	private boolean isIndexedPluralAttribute(SqmPath path) {
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
	public SqmSubQuery visitSubQueryExpression(HqlParser.SubQueryExpressionContext ctx) {
		return visitSubQuery( ctx.subQuery() );
	}

	@Override
	public SqmSubQuery visitSubQuery(HqlParser.SubQueryContext ctx) {
		if ( ctx.querySpec().selectClause() == null ) {
			throw new SemanticException( "Sub-query cannot use implicit select-clause : " + ctx.getText() );
		}

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
			//noinspection unchecked
			subQuery.setQuerySpec( visitQuerySpec( ctx.querySpec() ) );
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
			final SqmAttributeJoin indexedJoinPath = (SqmAttributeJoin) ctx.dotIdentifierSequence().accept( this );
			//noinspection unchecked
			return new SqmIndexedCollectionAccessPath(
					indexedJoinPath,
					(SqmExpression) ctx.indexedPathAccessFragment().accept( this )
			);
		}

		throw new ParsingException( "Unsure how to process `syntacticDomainPath` over : " + ctx.getText() );
	}


//	@Override
//	public SemanticPathPart visitDotIdentifierSequence(HqlParser.DotIdentifierSequenceContext ctx) {
//		final int numberOfContinuations = ctx.dotIdentifierSequenceContinuation().size();
//		final boolean hasContinuations = numberOfContinuations != 0;
//
//		final SemanticPathPart currentPathPart = semanticPathPartStack.getCurrent();
//
//		SemanticPathPart result = currentPathPart.resolvePathPart(
//				ctx.identifier().getText(),
//				!hasContinuations,
//				this
//		);
//
//		if ( hasContinuations ) {
//			int i = 1;
//			for ( HqlParser.DotIdentifierSequenceContinuationContext continuation : ctx.dotIdentifierSequenceContinuation() ) {
//				result = result.resolvePathPart(
//						continuation.identifier().getText(),
//						i++ >= numberOfContinuations,
//						this
//				);
//			}
//		}
//
//		return result;
//	}


	@Override
	public SemanticPathPart visitDotIdentifierSequence(HqlParser.DotIdentifierSequenceContext ctx) {
		final int numberOfContinuations = ctx.dotIdentifierSequenceContinuation().size();
		final boolean hasContinuations = numberOfContinuations != 0;

		final DotIdentifierConsumer dotIdentifierConsumer = dotIdentifierConsumerStack.getCurrent();

		dotIdentifierConsumer.consumeIdentifier(
				ctx.identifier().getText(),
				true,
				! hasContinuations
		);

		if ( hasContinuations ) {
			int i = 1;
			for ( HqlParser.DotIdentifierSequenceContinuationContext continuation : ctx.dotIdentifierSequenceContinuation() ) {
				dotIdentifierConsumer.consumeIdentifier(
						continuation.identifier().getText(),
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
		final EntityDomainType<?> treatTarget = getCreationContext().getJpaMetamodel().entity( treatTargetName );

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

		SqmPath result = attribute.getElementPathSource().createSqmPath(
				pluralAttributePath,
				this
		);

		if ( ctx.pathContinuation() != null ) {
			result = consumeDomainPath( ctx.path() );
		}

		return result;
	}


	@Override
	public SqmPath visitMapKeyNavigablePath(HqlParser.MapKeyNavigablePathContext ctx) {
		final SqmPath<?> sqmPath = consumeDomainPath( ctx.path() );
		final SqmPathSource<?> referencedPathSource = sqmPath.getReferencedPathSource();

		if ( ! (referencedPathSource instanceof MapPersistentAttribute ) ) {
			throw new PathException(
					"SqmPath#referencedPathSource [" + sqmPath + "] does not refer"
			);
		}

		final MapPersistentAttribute attribute = (MapPersistentAttribute) referencedPathSource;

		//noinspection unchecked
		final SqmPath result = attribute.getKeyPathSource().createSqmPath( sqmPath, this );

		if ( ctx.pathContinuation() != null ) {
			return consumeDomainPath( ctx.path() );
		}

		return result;
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
}
