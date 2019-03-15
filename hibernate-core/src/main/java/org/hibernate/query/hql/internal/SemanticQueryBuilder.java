/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.HibernateException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.SortOrder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.metamodel.model.domain.spi.CollectionElement;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralValuedNavigable;
import org.hibernate.query.BinaryArithmeticOperator;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.UnaryArithmeticOperator;
import org.hibernate.query.hql.DotIdentifierConsumer;
import org.hibernate.query.spi.ComparisonOperator;
import org.hibernate.query.sqm.LiteralNumberFormatException;
import org.hibernate.query.sqm.NotYetImplementedException;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.StrictJpaComplianceViolation;
import org.hibernate.query.sqm.UnknownEntityException;
import org.hibernate.query.sqm.produce.SqmCreationProcessingState;
import org.hibernate.query.sqm.produce.SqmProductionException;
import org.hibernate.query.sqm.produce.SqmQuerySpecCreationProcessingState;
import org.hibernate.query.sqm.produce.SqmTreeCreationLogger;
import org.hibernate.query.sqm.produce.function.SqmFunctionTemplate;
import org.hibernate.query.sqm.produce.internal.SqmDmlCreationProcessingState;
import org.hibernate.query.sqm.produce.internal.SqmQuerySpecCreationProcessingStateStandardImpl;
import org.hibernate.query.sqm.produce.internal.UniqueIdGenerator;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.ImplicitAliasGenerator;
import org.hibernate.query.sqm.produce.spi.ParameterDeclarationContext;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.produce.spi.SqmCreationOptions;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.produce.spi.TrimSpecificationExpressionWrapper;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.SqmIndexedCollectionAccessPath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPolymorphicRootDescriptor;
import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;
import org.hibernate.query.sqm.tree.expression.InferableTypeSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralHelper;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmCaseSearched;
import org.hibernate.query.sqm.tree.expression.SqmCaseSimple;
import org.hibernate.query.sqm.tree.expression.SqmCollectionSize;
import org.hibernate.query.sqm.tree.expression.SqmConcat;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.SqmLiteralNull;
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.expression.SqmParameterizedEntityType;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;
import org.hibernate.query.sqm.tree.expression.SqmSubQuery;
import org.hibernate.query.sqm.tree.expression.SqmUnaryOperation;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionElementReferenceBasic;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionElementReferenceEmbedded;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionElementReferenceEntity;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityTypedReference;
import org.hibernate.query.sqm.tree.domain.SqmMapEntryReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmMaxElementReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmMaxElementReferenceBasic;
import org.hibernate.query.sqm.tree.expression.domain.SqmMaxElementReferenceEmbedded;
import org.hibernate.query.sqm.tree.expression.domain.SqmMaxElementReferenceEntity;
import org.hibernate.query.sqm.tree.expression.domain.SqmMaxIndexReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmMaxIndexReferenceBasic;
import org.hibernate.query.sqm.tree.expression.domain.SqmMaxIndexReferenceEmbedded;
import org.hibernate.query.sqm.tree.expression.domain.SqmMaxIndexReferenceEntity;
import org.hibernate.query.sqm.tree.expression.domain.SqmMinElementReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmMinElementReferenceBasic;
import org.hibernate.query.sqm.tree.expression.domain.SqmMinElementReferenceEmbedded;
import org.hibernate.query.sqm.tree.expression.domain.SqmMinElementReferenceEntity;
import org.hibernate.query.sqm.tree.expression.domain.SqmMinIndexReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmMinIndexReferenceBasic;
import org.hibernate.query.sqm.tree.expression.domain.SqmMinIndexReferenceEmbeddable;
import org.hibernate.query.sqm.tree.expression.domain.SqmMinIndexReferenceEntity;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmPluralAttributeReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReference;
import org.hibernate.query.sqm.tree.expression.function.Distinctable;
import org.hibernate.query.sqm.tree.expression.function.SqmAbsFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmAggregateFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmAvgFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCastFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCoalesceFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmConcatFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCountFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCountStarFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmGenericFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmLengthFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmLowerFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmMaxFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmMinFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmNullifFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmStrFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmSubstringFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmSumFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmTrimFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmUpperFunction;
import org.hibernate.query.sqm.tree.from.DowncastLocation;
import org.hibernate.query.sqm.tree.from.MutableUsageDetails;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;
import org.hibernate.query.sqm.tree.from.SqmQualifiedJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.internal.ParameterCollector;
import org.hibernate.query.sqm.tree.predicate.AndSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.BetweenSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.EmptinessSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.GroupedSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.InListSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.InSubQuerySqmPredicate;
import org.hibernate.query.sqm.tree.predicate.LikeSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.MemberOfSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.NegatableSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.NegatedSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.NullnessSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.OrSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmComparisonPredicate;
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
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.EntityValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.tree.spi.TrimSpecification;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.spi.StandardSpiBasicTypes;
import org.hibernate.type.spi.StandardSpiBasicTypes.StandardBasicType;

import org.jboss.logging.Logger;

import org.antlr.v4.runtime.Token;

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

	private final ImplicitAliasGenerator implicitAliasGenerator;
	private final UniqueIdGenerator uidGenerator;

	private final Stack<DotIdentifierConsumer> identifierConsumerStack;

	private final Stack<TreatHandler> treatHandlerStack = new StandardStack<>( new TreatHandlerNormal() );

	private Map<NavigablePath,SqmNavigableReference> navigableReferenceByPath;

	private final Stack<ParameterDeclarationContext> parameterDeclarationContextStack = new StandardStack<>();
	private final Stack<SqmCreationProcessingState> processingStateStack = new StandardStack<>();

	private ParameterCollector parameterCollector;


	public Stack<SqmCreationProcessingState> getProcessingStateStack() {
		return processingStateStack;
	}

	protected SemanticQueryBuilder(SqmCreationOptions creationOptions, SqmCreationContext creationContext) {
		this.creationOptions = creationOptions;
		this.creationContext = creationContext;

		this.implicitAliasGenerator = new ImplicitAliasGenerator();
		this.uidGenerator = new UniqueIdGenerator();

		this.identifierConsumerStack = new StandardStack<>(
				new BasicDotIdentifierConsumer( processingStateStack::getCurrent )
		);
	}

	@Override
	public SqmCreationContext getCreationContext() {
		return creationContext;
	}

	@Override
	public SqmCreationOptions getCreationOptions() {
		return creationOptions;
	}

	@Override
	public ImplicitAliasGenerator getImplicitAliasGenerator() {
		return implicitAliasGenerator;
	}

	@Override
	public String generateUniqueIdentifier() {
		return uidGenerator.generateUniqueId();
	}

	@Override
	public void cacheNavigableReference(SqmNavigableReference reference) {
		assert reference.getSourceReference() != null;

		if ( navigableReferenceByPath == null ) {
			navigableReferenceByPath = new HashMap<>();
		}

		final SqmNavigableReference previous = navigableReferenceByPath.put( reference.getNavigablePath(), reference );
		if ( previous != null ) {
			// todo (6.0) : should this be an exception instead?
			log.debugf(
					"Caching SqmNavigableReference [%s] over-wrote previous [%s]",
					reference,
					previous
			);
		}
	}

	@Override
	public SqmNavigableReference getCachedNavigableReference(SqmNavigableContainerReference source, Navigable navigable) {
		if ( navigableReferenceByPath == null ) {
			return null;
		}

		final NavigablePath path = source.getNavigablePath().append( navigable.getNavigableName() );
		return navigableReferenceByPath.get( path );
	}

	public Map<NavigablePath, Set<SqmNavigableJoin>> fetchesByParentPath;

	@Override
	public void registerFetch(SqmNavigableContainerReference sourceReference, SqmNavigableJoin navigableJoin) {
		Set<SqmNavigableJoin> joins = null;

		if ( fetchesByParentPath == null ) {
			fetchesByParentPath = new HashMap<>();
		}
		else {
			joins = fetchesByParentPath.get( sourceReference.getNavigablePath() );
		}

		if ( joins == null ) {
			joins = new HashSet<>();
			fetchesByParentPath.put( sourceReference.getNavigablePath(), joins );
		}

		joins.add( navigableJoin );
	}

	@Override
	public SqmQuerySpecCreationProcessingState getCurrentQuerySpecProcessingState() {
		final SqmCreationProcessingState current = processingStateStack.getCurrent();
		if ( current instanceof SqmQuerySpecCreationProcessingState ) {
			return (SqmQuerySpecCreationProcessingState) current;
		}

		throw new UnsupportedOperationException( "Current processing state is not related to a SqmQuerySpec : " + current );
	}

	protected <T> void primeStack(Stack<T> stack, T initialValue) {
		stack.push( initialValue );
	}

	protected Stack<ParameterDeclarationContext> getParameterDeclarationContextStack() {
		return parameterDeclarationContextStack;
	}

	public Stack<DotIdentifierConsumer> getIdentifierConsumerStack() {
		return identifierConsumerStack;
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

		final SqmSelectStatement selectStatement = new SqmSelectStatement();
		parameterCollector = selectStatement;

		selectStatement.setQuerySpec( visitQuerySpec( ctx.querySpec() ) );
		selectStatement.applyFetchJoinsByParentPath( fetchesByParentPath );

		return selectStatement;
	}

	@Override
	public SqmInsertSelectStatement visitInsertStatement(HqlParser.InsertStatementContext ctx) {
		processingStateStack.push( new SqmDmlCreationProcessingState( this ) );

		try {
			final EntityTypeDescriptor<?> targetType = visitEntityName( ctx.insertSpec().intoSpec().entityName() );


			String alias = getImplicitAliasGenerator().generateUniqueImplicitAlias();
			log.debugf(
					"Generated implicit alias [%s] for INSERT target [%s]",
					alias,
					targetType.getEntityName()
			);

			final SqmRoot root = new SqmRoot( generateUniqueIdentifier(), null, targetType );
			processingStateStack.getCurrent().getPathRegistry().register( root );

			// for now we only support the INSERT-SELECT form
			final SqmInsertSelectStatement insertStatement = new SqmInsertSelectStatement( root );
			parameterCollector = insertStatement;

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
		processingStateStack.push( new SqmDmlCreationProcessingState( this ) );
		try {
			final SqmRoot root = new SqmRoot(
					generateUniqueIdentifier(),
					visitIdentificationVariableDef( ctx.identificationVariableDef() ),
					visitEntityName( ctx.entityName() )
			);
			final SqmUpdateStatement updateStatement = new SqmUpdateStatement( root );

			parameterCollector = updateStatement;
			updateStatement.getWhereClause().setPredicate(
					(SqmPredicate) ctx.whereClause().predicate().accept( this )
			);

			for ( HqlParser.AssignmentContext assignmentContext : ctx.setClause().assignment() ) {
				final SqmSingularAttributeReference stateField = (SqmSingularAttributeReference) visitDotIdentifierSequence( assignmentContext.dotIdentifierSequence() );
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
		processingStateStack.push( new SqmDmlCreationProcessingState( this ) );
		try {
			final SqmRoot root = new SqmRoot(
					generateUniqueIdentifier(),
					visitIdentificationVariableDef( ctx.identificationVariableDef() ),
					visitEntityName( ctx.entityName() )
			);

			final SqmDeleteStatement deleteStatement = new SqmDeleteStatement( root );

			parameterCollector = deleteStatement;

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
		final SqmQuerySpec sqmQuerySpec = new SqmQuerySpec();

		processingStateStack.push(
				new SqmQuerySpecCreationProcessingStateStandardImpl(
						processingStateStack.getCurrent(),
						this
				)
		);

		try {
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

			final SqmWhereClause whereClause = new SqmWhereClause();
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

				sqmQuerySpec.setOffsetExpression( visitOffsetClause( ctx.offsetClause() ) );
				sqmQuerySpec.setLimitExpression( visitLimitClause( ctx.limitClause() ) );
			}

			return sqmQuerySpec;
		}
		finally {
			processingStateStack.pop();
		}
	}

	@SuppressWarnings("WeakerAccess")
	protected SqmSelectClause buildInferredSelectClause(SqmFromClause fromClause) {
		// for now, this is slightly different than the legacy behavior where
		// the root and each non-fetched-join was selected.  For now, here, we simply
		// select the root
		final SqmSelectClause selectClause = new SqmSelectClause( false );
		fromClause.visitRoots(
				sqmRoot -> selectClause.addSelection( new SqmSelection( sqmRoot ) )
		);
		return selectClause;
	}

	@Override
	public SqmSelectClause visitSelectClause(HqlParser.SelectClauseContext ctx) {
		treatHandlerStack.push( new TreatHandlerNormal( DowncastLocation.SELECT ) );

		try {
			final SqmSelectClause selectClause = new SqmSelectClause( ctx.DISTINCT() != null );
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
		final String resultIdentifier = interpretResultIdentifier( ctx.resultIdentifier() );
		SqmSelectableNode selectableNode = visitSelectableNode( ctx );

		if ( selectableNode instanceof SqmPluralAttributeReference ) {
			final SqmPluralAttributeReference pluralAttributeBinding = (SqmPluralAttributeReference) selectableNode;
			final CollectionElement elementReference = pluralAttributeBinding.getReferencedNavigable().getPersistentCollectionDescriptor().getElementDescriptor();
			switch ( elementReference.getClassification() ) {
				case ANY: {
					throw new NotYetImplementedException(  );
				}
				case BASIC: {
					selectableNode = new SqmCollectionElementReferenceBasic( pluralAttributeBinding );
					break;
				}
				case EMBEDDABLE: {
					selectableNode = new SqmCollectionElementReferenceEmbedded( pluralAttributeBinding );
					break;
				}
				case ONE_TO_MANY:
				case MANY_TO_MANY: {
					selectableNode = new SqmCollectionElementReferenceEntity( pluralAttributeBinding );
					break;
				}
			}
		}

		final SqmSelection selection = new SqmSelection( selectableNode, resultIdentifier );
		getCurrentQuerySpecProcessingState().registerSelection( selection );
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

	private String interpretResultIdentifier(HqlParser.ResultIdentifierContext resultIdentifierContext) {
		if ( resultIdentifierContext != null ) {
			final String explicitAlias;
			if ( resultIdentifierContext.AS() != null ) {
				final Token aliasToken = resultIdentifierContext.identifier().getStart();
				explicitAlias = aliasToken.getText();

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
			}
			else {
				explicitAlias = resultIdentifierContext.getText();
			}
			return explicitAlias;
		}

		return getImplicitAliasGenerator().generateUniqueImplicitAlias();
	}

	private JavaTypeDescriptor<List> listJavaTypeDescriptor;
	private JavaTypeDescriptor<Map> mapJavaTypeDescriptor;

	@Override
	public SqmDynamicInstantiation visitDynamicInstantiation(HqlParser.DynamicInstantiationContext ctx) {
		final SqmDynamicInstantiation dynamicInstantiation;

		if ( ctx.dynamicInstantiationTarget().MAP() != null ) {
			if ( mapJavaTypeDescriptor == null ) {
				mapJavaTypeDescriptor = creationContext.getDomainModel()
						.getTypeConfiguration()
						.getJavaTypeDescriptorRegistry()
						.getDescriptor( Map.class );
			}
			dynamicInstantiation = SqmDynamicInstantiation.forMapInstantiation( mapJavaTypeDescriptor );
		}
		else if ( ctx.dynamicInstantiationTarget().LIST() != null ) {
			if ( listJavaTypeDescriptor == null ) {
				listJavaTypeDescriptor = creationContext.getDomainModel()
						.getTypeConfiguration()
						.getJavaTypeDescriptorRegistry()
						.getDescriptor( List.class );
			}
			dynamicInstantiation = SqmDynamicInstantiation.forListInstantiation( listJavaTypeDescriptor );
		}
		else {
			final String className = ctx.dynamicInstantiationTarget().dotIdentifierSequence().getText();
			try {
				final Class<?> targetJavaType = classForName( className );
				final JavaTypeDescriptor jtd = creationContext.getDomainModel()
						.getTypeConfiguration()
						.getJavaTypeDescriptorRegistry()
						.getOrMakeJavaDescriptor( targetJavaType );
				dynamicInstantiation = SqmDynamicInstantiation.forClassInstantiation( jtd );
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

	private Class classForName(String className) {
		return creationContext.getServiceRegistry().getService( ClassLoaderService.class ).classForName( className );
	}

	@Override
	public SqmDynamicInstantiationArgument visitDynamicInstantiationArg(HqlParser.DynamicInstantiationArgContext ctx) {
		return new SqmDynamicInstantiationArgument(
				visitDynamicInstantiationArgExpression( ctx.dynamicInstantiationArgExpression() ),
				ctx.identifier() == null ? null : ctx.identifier().getText()
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
		final SqmExpression sortExpression = (SqmExpression) ctx.expression().accept( this );
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
		return new SqmSortSpecification( sortExpression, collation, sortOrder );
	}

	@Override
	public SqmExpression visitLimitClause(HqlParser.LimitClauseContext ctx) {
		if ( ctx == null ) {
			return null;
		}

		SqmExpression sqmExpression = (SqmExpression) ctx.parameterOrNumberLiteral().accept( this );
		applyImpliedType( sqmExpression, StandardSpiBasicTypes.INTEGER );
		return sqmExpression;
	}

	@Override
	public SqmExpression visitOffsetClause(HqlParser.OffsetClauseContext ctx) {
		if ( ctx == null ) {
			return null;
		}

		SqmExpression sqmExpression = (SqmExpression) ctx.parameterOrNumberLiteral().accept( this );
		applyImpliedType( sqmExpression, StandardSpiBasicTypes.INTEGER );
		return sqmExpression;
	}

	@SuppressWarnings("SameParameterValue")
	private void applyImpliedType(SqmExpression sqmExpression, StandardBasicType impliedType) {
		if ( sqmExpression instanceof InferableTypeSqmExpression ) {
			( (InferableTypeSqmExpression) sqmExpression ).impliedType( () -> impliedType );
		}
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
	public EntityTypeDescriptor<?> visitEntityName(HqlParser.EntityNameContext parserEntityName) {
		final String entityName = parserEntityName.dotIdentifierSequence().getText();
		final EntityValuedExpressableType entityReference = resolveEntityReference( entityName );
		if ( entityReference == null ) {
			throw new UnknownEntityException( "Could not resolve entity name [" + entityName + "] as DML target", entityName );
		}

		return entityReference.getEntityDescriptor();
	}

	private EntityValuedExpressableType resolveEntityReference(String entityName) {
		log.debugf( "Attempting to resolve path [%s] as entity reference...", entityName );
		EntityValuedExpressableType reference = null;
		try {
			reference = creationContext.getDomainModel().resolveEntityReference( entityName );
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
		final SqmRoot sqmPathRoot = visitPathRoot( parserSpace.pathRoot() );

		for ( HqlParser.CrossJoinContext parserJoin : parserSpace.crossJoin() ) {
			// CROSS joins are always added to the root
			sqmPathRoot.addJoin( visitCrossJoin( parserJoin ) );
		}

		for ( HqlParser.QualifiedJoinContext parserJoin : parserSpace.qualifiedJoin() ) {
			consumeQualifiedJoin( parserJoin, sqmPathRoot );
		}

		for ( HqlParser.JpaCollectionJoinContext parserJoin : parserSpace.jpaCollectionJoin() ) {
			consumeJpaCollectionJoin( parserJoin );
		}

		return sqmPathRoot;
	}


	@Override
	public SqmRoot visitPathRoot(HqlParser.PathRootContext ctx) {
		final String name = ctx.entityName().getText();

		log.debugf( "Handling root path - %s", name );

		final EntityTypeDescriptor entityDescriptor = getCreationContext().getDomainModel().resolveEntityReference( name );

		if ( entityDescriptor instanceof SqmPolymorphicRootDescriptor ) {
			if ( getCreationOptions().useStrictJpaCompliance() ) {
				throw new StrictJpaComplianceViolation(
						"Encountered unmapped polymorphic reference [" + entityDescriptor.getEntityName()
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

		final SqmRoot sqmRoot = new SqmRoot(
				generateUniqueIdentifier(),
				alias,
				entityDescriptor
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
	public SqmCrossJoin visitCrossJoin(HqlParser.CrossJoinContext ctx) {
		final String name = ctx.pathRoot().entityName().getText();

		SqmTreeCreationLogger.LOGGER.debugf( "Handling root path - %s", name );

		final EntityValuedExpressableType entityType = getCreationContext().getDomainModel().resolveEntityReference( name );

		if ( entityType instanceof SqmPolymorphicRootDescriptor ) {
			throw new SemanticException( "Unmapped polymorphic reference cannot be used as a CROSS JOIN target" );
		}

		final EntityTypeDescriptor entityDescriptor = entityType.getEntityDescriptor();

		final SqmCrossJoin join = new SqmCrossJoin(
				generateUniqueIdentifier(),
				visitIdentificationVariableDef( ctx.pathRoot().identificationVariableDef() ), entityDescriptor
		);

		processingStateStack.getCurrent().getPathRegistry().register( join );

		return join;

	}

	@Override
	public SqmQualifiedJoin visitQualifiedJoin(HqlParser.QualifiedJoinContext parserJoin) {
		throw new UnsupportedOperationException( "Unexpected call to #visitQualifiedJoin, see #handleQUalifiedJoin" );
	}

	@SuppressWarnings("WeakerAccess")
	protected void consumeQualifiedJoin(
			HqlParser.QualifiedJoinContext parserJoin,
			SqmRoot sqmPathRoot) {
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


		final SqmQualifiedJoin join = consumeQualifiedJoinRhs(
				parserJoin.qualifiedJoinRhs(),
				joinType,
				parserJoin.FETCH() != null
		);

		if ( join instanceof SqmEntityJoin ) {
			sqmPathRoot.addJoin( join );
		}
		else {
			if ( getCreationOptions().useStrictJpaCompliance() ) {
				if ( join.getExplicitAlias() != null ){
					if ( ( (SqmNavigableJoin) join ).isFetched() ) {
						throw new StrictJpaComplianceViolation(
								"Encountered aliased fetch join, but strict JPQL compliance was requested",
								StrictJpaComplianceViolation.Type.ALIASED_FETCH_JOIN
						);
					}
				}
			}
		}

		// IMPORTANT : register before processing the join-predicate so that handling the
		// predicate has access to it...

		processingStateStack.getCurrent().getPathRegistry().register( join );

		if ( parserJoin.qualifiedJoinPredicate() != null ) {
			join.setJoinPredicate( visitQualifiedJoinPredicate( parserJoin.qualifiedJoinPredicate() ) );
		}

	}

	private SqmQualifiedJoin consumeQualifiedJoinRhs(
			HqlParser.QualifiedJoinRhsContext qualifiedJoinRhs,
			SqmJoinType joinType,
			boolean fetched) {
		final String alias = visitIdentificationVariableDef( qualifiedJoinRhs.identificationVariableDef() );

		final DotIdentifierConsumer identifierConsumer = new QualifiedJoinPathIdentifierConsumer(
				joinType,
				fetched,
				alias,
				processingStateStack.getCurrent()
		);

		identifierConsumerStack.push( identifierConsumer );

		try {
			 final SqmQualifiedJoin join = (SqmQualifiedJoin) qualifiedJoinRhs.path().accept( this );

			// we need to set the alias here because the path could be treated - the treat operator is
			// not consumed by the identifierConsumer
			join.setExplicitAlias( alias );

			return join;
		}
		finally {
			identifierConsumerStack.pop();
		}
	}

	@Override
	public SqmPredicate visitQualifiedJoinPredicate(HqlParser.QualifiedJoinPredicateContext ctx) {
		identifierConsumerStack.push( new BasicDotIdentifierConsumer( processingStateStack::getCurrent ) );
		try {
			return (SqmPredicate) ctx.predicate().accept( this );
		}
		finally {
			identifierConsumerStack.pop();
		}
	}

	@Override
	public SqmJoin visitJpaCollectionJoin(HqlParser.JpaCollectionJoinContext ctx) {
		throw new UnsupportedOperationException();
	}

	public void consumeJpaCollectionJoin(HqlParser.JpaCollectionJoinContext ctx) {
		// todo (6.0) : this should always make sure the collection tables are joined
		//		ideally via specialized DotIdentifierConsumer
		identifierConsumerStack.push( new BasicDotIdentifierConsumer( processingStateStack::getCurrent ) );
		try {
			consumePluralAttributeReference( ctx.path() );
		}
		finally {
			identifierConsumerStack.pop();
		}
	}


	// Predicates (and `whereClause`)

	@Override
	public SqmPredicate visitWhereClause(HqlParser.WhereClauseContext ctx) {
		return (SqmPredicate) ctx.predicate().accept( this );
	}

	@Override
	public GroupedSqmPredicate visitGroupedPredicate(HqlParser.GroupedPredicateContext ctx) {
		return new GroupedSqmPredicate( (SqmPredicate) ctx.predicate().accept( this ) );
	}

	@Override
	public SqmPredicate visitAndPredicate(HqlParser.AndPredicateContext ctx) {
		return new AndSqmPredicate(
				(SqmPredicate) ctx.predicate( 0 ).accept( this ),
				(SqmPredicate) ctx.predicate( 1 ).accept( this )
		);
	}

	@Override
	public SqmPredicate visitOrPredicate(HqlParser.OrPredicateContext ctx) {
		return new OrSqmPredicate(
				(SqmPredicate) ctx.predicate( 0 ).accept( this ),
				(SqmPredicate) ctx.predicate( 1 ).accept( this )
		);
	}

	@Override
	public SqmPredicate visitNegatedPredicate(HqlParser.NegatedPredicateContext ctx) {
		SqmPredicate predicate = (SqmPredicate) ctx.predicate().accept( this );
		if ( predicate instanceof NegatableSqmPredicate ) {
			( (NegatableSqmPredicate) predicate ).negate();
			return predicate;
		}
		else {
			return new NegatedSqmPredicate( predicate );
		}
	}

	@Override
	public NullnessSqmPredicate visitIsNullPredicate(HqlParser.IsNullPredicateContext ctx) {
		return new NullnessSqmPredicate(
				(SqmExpression) ctx.expression().accept( this ),
				ctx.NOT() != null
		);
	}

	@Override
	public EmptinessSqmPredicate visitIsEmptyPredicate(HqlParser.IsEmptyPredicateContext ctx) {
		return new EmptinessSqmPredicate(
				(SqmPluralAttributeReference) ctx.expression().accept( this ),
				ctx.NOT() != null
		);
	}

	@Override
	public SqmComparisonPredicate visitEqualityPredicate(HqlParser.EqualityPredicateContext ctx) {
		final SqmExpression lhs = (SqmExpression) ctx.expression().get( 0 ).accept( this );
		final SqmExpression rhs = (SqmExpression) ctx.expression().get( 1 ).accept( this );

		// todo (6.0) : the problem with doing this in the HQL -> SQM translation is that we'd need to duplicate it for criteria
		//		Do we need these types really prior to interpreting the SQM?  Couldn't we make the SQM walker
		//		responsible for handling this?
		//
		// An expression has 3 associated types:
		//
		//		1) explicit type - the user has (somehow) explicitly told us the type.  (where does this exist?)
		//		2) fallback type - generally this is one of the standard basic type
		//		3) inferred type - a type that is inferred by its surroundings

		if ( lhs.getInferableType() != null ) {
			if ( rhs instanceof InferableTypeSqmExpression ) {
				( (InferableTypeSqmExpression) rhs ).impliedType( lhs.getInferableType() );
			}
		}

		if ( rhs.getInferableType() != null ) {
			if ( lhs instanceof InferableTypeSqmExpression ) {
				( (InferableTypeSqmExpression) lhs ).impliedType( rhs.getInferableType() );
			}
		}

		return new SqmComparisonPredicate( lhs, ComparisonOperator.EQUAL, rhs );
	}

	@Override
	public Object visitInequalityPredicate(HqlParser.InequalityPredicateContext ctx) {
		final SqmExpression lhs = (SqmExpression) ctx.expression().get( 0 ).accept( this );
		final SqmExpression rhs = (SqmExpression) ctx.expression().get( 1 ).accept( this );

		return new SqmComparisonPredicate( lhs, ComparisonOperator.NOT_EQUAL, rhs );
	}

	@Override
	public Object visitGreaterThanPredicate(HqlParser.GreaterThanPredicateContext ctx) {
		final SqmExpression lhs = (SqmExpression) ctx.expression().get( 0 ).accept( this );
		final SqmExpression rhs = (SqmExpression) ctx.expression().get( 1 ).accept( this );

		return new SqmComparisonPredicate( lhs, ComparisonOperator.GREATER_THAN, rhs );
	}

	@Override
	public Object visitGreaterThanOrEqualPredicate(HqlParser.GreaterThanOrEqualPredicateContext ctx) {
		final SqmExpression lhs = (SqmExpression) ctx.expression().get( 0 ).accept( this );
		final SqmExpression rhs = (SqmExpression) ctx.expression().get( 1 ).accept( this );

		return new SqmComparisonPredicate( lhs, ComparisonOperator.GREATER_THAN_OR_EQUAL, rhs );
	}

	@Override
	public Object visitLessThanPredicate(HqlParser.LessThanPredicateContext ctx) {
		final SqmExpression lhs = (SqmExpression) ctx.expression().get( 0 ).accept( this );
		final SqmExpression rhs = (SqmExpression) ctx.expression().get( 1 ).accept( this );

		return new SqmComparisonPredicate( lhs, ComparisonOperator.LESS_THAN, rhs );
	}

	@Override
	public Object visitLessThanOrEqualPredicate(HqlParser.LessThanOrEqualPredicateContext ctx) {
		final SqmExpression lhs = (SqmExpression) ctx.expression().get( 0 ).accept( this );
		final SqmExpression rhs = (SqmExpression) ctx.expression().get( 1 ).accept( this );

		return new SqmComparisonPredicate( lhs, ComparisonOperator.LESS_THAN_OR_EQUAL, rhs );
	}

	@Override
	public Object visitBetweenPredicate(HqlParser.BetweenPredicateContext ctx) {
		final SqmExpression expression = (SqmExpression) ctx.expression().get( 0 ).accept( this );
		final SqmExpression lowerBound = (SqmExpression) ctx.expression().get( 1 ).accept( this );
		final SqmExpression upperBound = (SqmExpression) ctx.expression().get( 2 ).accept( this );

		if ( expression.getInferableType() != null ) {
			if ( lowerBound instanceof InferableTypeSqmExpression ) {
				( (InferableTypeSqmExpression) lowerBound ).impliedType( expression.getInferableType() );
			}
			if ( upperBound instanceof InferableTypeSqmExpression ) {
				( (InferableTypeSqmExpression) upperBound ).impliedType( expression.getInferableType() );
			}
		}
		else if ( lowerBound.getInferableType() != null ) {
			if ( expression instanceof InferableTypeSqmExpression ) {
				( (InferableTypeSqmExpression) expression ).impliedType( lowerBound.getInferableType() );
			}
			if ( upperBound instanceof InferableTypeSqmExpression ) {
				( (InferableTypeSqmExpression) upperBound ).impliedType( lowerBound.getInferableType() );
			}
		}
		else if ( upperBound.getInferableType() != null ) {
			if ( expression instanceof InferableTypeSqmExpression ) {
				( (InferableTypeSqmExpression) expression ).impliedType( upperBound.getInferableType() );
			}
			if ( lowerBound instanceof InferableTypeSqmExpression ) {
				( (InferableTypeSqmExpression) lowerBound ).impliedType( upperBound.getInferableType() );
			}
		}

		return new BetweenSqmPredicate(
				expression,
				lowerBound,
				upperBound,
				false
		);
	}

	@Override
	public SqmPredicate visitLikePredicate(HqlParser.LikePredicateContext ctx) {
		if ( ctx.likeEscape() != null ) {
			return new LikeSqmPredicate(
					(SqmExpression) ctx.expression().get( 0 ).accept( this ),
					(SqmExpression) ctx.expression().get( 1 ).accept( this ),
					(SqmExpression) ctx.likeEscape().expression().accept( this )
			);
		}
		else {
			return new LikeSqmPredicate(
					(SqmExpression) ctx.expression().get( 0 ).accept( this ),
					(SqmExpression) ctx.expression().get( 1 ).accept( this )
			);
		}
	}

	@Override
	public SqmPredicate visitMemberOfPredicate(HqlParser.MemberOfPredicateContext ctx) {
		final SqmNavigableReference pathResolution = (SqmNavigableReference) ctx.path().accept( this );
		if ( pathResolution == null ) {
			throw new SemanticException( "Could not resolve path [" + ctx.path().getText() + "] as a plural attribute reference" );
		}

		if ( !SqmPluralAttributeReference.class.isInstance( pathResolution ) ) {
			throw new SemanticException( "Path argument to MEMBER OF must be a plural attribute" );
		}

		return new MemberOfSqmPredicate( (SqmPluralAttributeReference) pathResolution );
	}

	@Override
	public SqmPredicate visitInPredicate(HqlParser.InPredicateContext ctx) {
		final SqmExpression testExpression = (SqmExpression) ctx.expression().accept( this );

		if ( HqlParser.ExplicitTupleInListContext.class.isInstance( ctx.inList() ) ) {
			final HqlParser.ExplicitTupleInListContext tupleExpressionListContext = (HqlParser.ExplicitTupleInListContext) ctx.inList();

			parameterDeclarationContextStack.push( () -> tupleExpressionListContext.expression().size() == 1 );
			try {
				final List<SqmExpression> listExpressions = new ArrayList<>( tupleExpressionListContext.expression().size() );
				for ( HqlParser.ExpressionContext expressionContext : tupleExpressionListContext.expression() ) {
					final SqmExpression listItemExpression = (SqmExpression) expressionContext.accept( this );

					if ( testExpression.getInferableType() != null ) {
						if ( listItemExpression instanceof InferableTypeSqmExpression ) {
							( (InferableTypeSqmExpression) listItemExpression ).impliedType( testExpression.getInferableType() );
						}
					}

					listExpressions.add( listItemExpression );
				}

				return new InListSqmPredicate( testExpression, listExpressions );
			}
			finally {
				parameterDeclarationContextStack.pop();
			}
		}
		else if ( HqlParser.SubQueryInListContext.class.isInstance( ctx.inList() ) ) {
			final HqlParser.SubQueryInListContext subQueryContext = (HqlParser.SubQueryInListContext) ctx.inList();
			final SqmExpression subQueryExpression = (SqmExpression) subQueryContext.expression().accept( this );

			if ( !SqmSubQuery.class.isInstance( subQueryExpression ) ) {
				throw new ParsingException(
						"Was expecting a SubQueryExpression, but found " + subQueryExpression.getClass().getSimpleName()
								+ " : " + subQueryContext.expression().toString()
				);
			}

			return new InSubQuerySqmPredicate( testExpression, (SqmSubQuery) subQueryExpression );
		}

		// todo : handle PersistentCollectionReferenceInList labeled branch

		throw new ParsingException( "Unexpected IN predicate type [" + ctx.getClass().getSimpleName() + "] : " + ctx.getText() );
	}

	@Override
	public Object visitEntityTypeExpression(HqlParser.EntityTypeExpressionContext ctx) {
		// can be one of 2 forms:
		//		1) TYPE( some.path )
		//		2) TYPE( :someParam )
		if ( ctx.entityTypeReference().parameter() != null ) {
			// we have form (2)
			return new SqmParameterizedEntityType(
					(SqmParameter) ctx.entityTypeReference().parameter().accept( this )
			);
		}
		else if ( ctx.entityTypeReference().path() != null ) {
			final SqmNavigableReference binding = (SqmNavigableReference) ctx.entityTypeReference().path().accept( this );
			return binding.as( EntityValuedNavigable.class )
					.getEntityDescriptor()
					.getHierarchy()
					.getDiscriminatorDescriptor()
					.createSqmExpression( null, this );
		}

		throw new ParsingException( "Could not interpret grammar context as 'entity type' expression : " + ctx.getText() );
	}

	private void validateBindingAsEntityTypeExpression(SqmNavigableReference binding) {
		if ( binding instanceof SqmEntityTypedReference ) {
			// its ok
			return;
		}

		throw new SemanticException(
				"Path used in TYPE() resolved to a non-EntityBinding : " + binding
		);
	}


	@Override
	@SuppressWarnings("unchecked")
	public SqmMapEntryReference visitMapEntrySelection(HqlParser.MapEntrySelectionContext ctx) {
		final SqmNavigableJoin pathResolution = consumePluralAttributeReference( ctx.path() );

		return new SqmMapEntryReference(
				pathResolution,
				(BasicJavaDescriptor) creationContext.getDomainModel().getTypeConfiguration()
						.getJavaTypeDescriptorRegistry()
						.getDescriptor( Map.Entry.class )
		);
	}




	@Override
	public SqmConcat visitConcatenationExpression(HqlParser.ConcatenationExpressionContext ctx) {
		if ( ctx.expression().size() != 2 ) {
			throw new ParsingException( "Expecting 2 operands to the concat operator" );
		}
		return new SqmConcat(
				(SqmExpression) ctx.expression( 0 ).accept( this ),
				(SqmExpression) ctx.expression( 1 ).accept( this )
		);
	}

	@Override
	public Object visitAdditionExpression(HqlParser.AdditionExpressionContext ctx) {
		if ( ctx.expression().size() != 2 ) {
			throw new ParsingException( "Expecting 2 operands to the + operator" );
		}

		final SqmExpression firstOperand = (SqmExpression) ctx.expression( 0 ).accept( this );
		final SqmExpression secondOperand = (SqmExpression) ctx.expression( 1 ).accept( this );
		return new SqmBinaryArithmetic(
				firstOperand,
				BinaryArithmeticOperator.ADD,
				secondOperand,
				creationContext.getDomainModel().getTypeConfiguration().resolveArithmeticType(
						(BasicValuedExpressableType) firstOperand.getExpressableType(),
						(BasicValuedExpressableType) secondOperand.getExpressableType(),
						BinaryArithmeticOperator.ADD
				)
		);
	}

	@Override
	public Object visitSubtractionExpression(HqlParser.SubtractionExpressionContext ctx) {
		if ( ctx.expression().size() != 2 ) {
			throw new ParsingException( "Expecting 2 operands to the - operator" );
		}

		final SqmExpression firstOperand = (SqmExpression) ctx.expression( 0 ).accept( this );
		final SqmExpression secondOperand = (SqmExpression) ctx.expression( 1 ).accept( this );
		return new SqmBinaryArithmetic(
				firstOperand,
				BinaryArithmeticOperator.SUBTRACT,
				secondOperand,
				creationContext.getDomainModel().getTypeConfiguration().resolveArithmeticType(
						(BasicValuedExpressableType) firstOperand.getExpressableType(),
						(BasicValuedExpressableType) secondOperand.getExpressableType(),
						BinaryArithmeticOperator.SUBTRACT
				)
		);
	}

	@Override
	public Object visitMultiplicationExpression(HqlParser.MultiplicationExpressionContext ctx) {
		if ( ctx.expression().size() != 2 ) {
			throw new ParsingException( "Expecting 2 operands to the * operator" );
		}

		final SqmExpression firstOperand = (SqmExpression) ctx.expression( 0 ).accept( this );
		final SqmExpression secondOperand = (SqmExpression) ctx.expression( 1 ).accept( this );
		return new SqmBinaryArithmetic(
				firstOperand,
				BinaryArithmeticOperator.MULTIPLY,
				secondOperand,
				creationContext.getDomainModel().getTypeConfiguration().resolveArithmeticType(
						(BasicValuedExpressableType) firstOperand.getExpressableType(),
						(BasicValuedExpressableType) secondOperand.getExpressableType(),
						BinaryArithmeticOperator.MULTIPLY
				)
		);
	}

	@Override
	public Object visitDivisionExpression(HqlParser.DivisionExpressionContext ctx) {
		if ( ctx.expression().size() != 2 ) {
			throw new ParsingException( "Expecting 2 operands to the / operator" );
		}

		final SqmExpression firstOperand = (SqmExpression) ctx.expression( 0 ).accept( this );
		final SqmExpression secondOperand = (SqmExpression) ctx.expression( 1 ).accept( this );
		return new SqmBinaryArithmetic(
				firstOperand,
				BinaryArithmeticOperator.DIVIDE,
				secondOperand,
				creationContext.getDomainModel().getTypeConfiguration().resolveArithmeticType(
						(BasicValuedExpressableType) firstOperand.getExpressableType(),
						(BasicValuedExpressableType) secondOperand.getExpressableType(),
						BinaryArithmeticOperator.DIVIDE
				)
		);
	}

	@Override
	public Object visitModuloExpression(HqlParser.ModuloExpressionContext ctx) {
		if ( ctx.expression().size() != 2 ) {
			throw new ParsingException( "Expecting 2 operands to the % operator" );
		}

		final SqmExpression firstOperand = (SqmExpression) ctx.expression( 0 ).accept( this );
		final SqmExpression secondOperand = (SqmExpression) ctx.expression( 1 ).accept( this );
		return new SqmBinaryArithmetic(
				firstOperand,
				BinaryArithmeticOperator.MODULO,
				secondOperand,
				creationContext.getDomainModel().getTypeConfiguration().resolveArithmeticType(
						(BasicValuedExpressableType) firstOperand.getExpressableType(),
						(BasicValuedExpressableType) secondOperand.getExpressableType(),
						BinaryArithmeticOperator.MODULO
				)
		);
	}

	@Override
	public Object visitUnaryPlusExpression(HqlParser.UnaryPlusExpressionContext ctx) {
		return new SqmUnaryOperation(
				UnaryArithmeticOperator.UNARY_PLUS,
				(SqmExpression) ctx.expression().accept( this )
		);
	}

	@Override
	public Object visitUnaryMinusExpression(HqlParser.UnaryMinusExpressionContext ctx) {
		return new SqmUnaryOperation(
				UnaryArithmeticOperator.UNARY_MINUS,
				(SqmExpression) ctx.expression().accept( this )
		);
	}

	@Override
	public SqmCaseSimple visitSimpleCaseStatement(HqlParser.SimpleCaseStatementContext ctx) {
		final SqmCaseSimple caseExpression = new SqmCaseSimple(
				(SqmExpression) ctx.expression().accept( this )
		);

		for ( HqlParser.SimpleCaseWhenContext simpleCaseWhen : ctx.simpleCaseWhen() ) {
			caseExpression.when(
					(SqmExpression) simpleCaseWhen.expression( 0 ).accept( this ),
					(SqmExpression) simpleCaseWhen.expression( 1 ).accept( this )
			);
		}

		if ( ctx.caseOtherwise() != null ) {
			caseExpression.otherwise( (SqmExpression) ctx.caseOtherwise().expression().accept( this ) );
		}

		return caseExpression;
	}

	@Override
	public SqmCaseSearched visitSearchedCaseStatement(HqlParser.SearchedCaseStatementContext ctx) {
		final SqmCaseSearched caseExpression = new SqmCaseSearched();

		for ( HqlParser.SearchedCaseWhenContext whenFragment : ctx.searchedCaseWhen() ) {
			caseExpression.when(
					(SqmPredicate) whenFragment.predicate().accept( this ),
					(SqmExpression) whenFragment.expression().accept( this )
			);
		}

		if ( ctx.caseOtherwise() != null ) {
			caseExpression.otherwise( (SqmExpression) ctx.caseOtherwise().expression().accept( this ) );
		}

		return caseExpression;
	}

	@Override
	public SqmCoalesceFunction visitCoalesceExpression(HqlParser.CoalesceExpressionContext ctx) {
		SqmCoalesceFunction coalesceExpression = new SqmCoalesceFunction();
		for ( HqlParser.ExpressionContext expressionContext : ctx.coalesce().expression() ) {
			coalesceExpression.value( (SqmExpression) expressionContext.accept( this ) );
		}
		return coalesceExpression;
	}

	@Override
	public SqmNullifFunction visitNullIfExpression(HqlParser.NullIfExpressionContext ctx) {
		return new SqmNullifFunction(
				(SqmExpression) ctx.nullIf().expression( 0 ).accept( this ),
				(SqmExpression) ctx.nullIf().expression( 1 ).accept( this )
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
			return new SqmLiteralNull();
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
		final BasicValuedExpressableType expressionType = resolveExpressableTypeBasic( Boolean.class );
		return new SqmLiteral<>( value, expressionType );
	}

	private SqmLiteral<Character> characterLiteral(String text) {
		if ( text.length() > 1 ) {
			// todo : or just treat it as a String literal?
			throw new ParsingException( "Value for CHARACTER_LITERAL token was more than 1 character" );
		}
		return new SqmLiteral<>(
				text.charAt( 0 ),
				resolveExpressableTypeBasic( Character.class )
		);
	}

	private SqmLiteral<String> stringLiteral(String text) {
		return new SqmLiteral<>(
				text,
				creationContext.getDomainModel().getTypeConfiguration().resolveStandardBasicType( StandardBasicTypes.STRING )
		);
	}

	@SuppressWarnings("WeakerAccess")
	protected SqmLiteral<Integer> integerLiteral(String text) {
		try {
			final Integer value = Integer.valueOf( text );
			return new SqmLiteral<>(
					value,
					resolveExpressableTypeBasic( Integer.class )
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
					resolveExpressableTypeBasic( Long.class )
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
					resolveExpressableTypeBasic( BigInteger.class  )
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
					resolveExpressableTypeBasic( Float.class )
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
					resolveExpressableTypeBasic( Double.class )
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
					resolveExpressableTypeBasic( BigDecimal.class )
			);
		}
		catch (NumberFormatException e) {
			throw new LiteralNumberFormatException(
					"Unable to convert sqm literal [" + originalText + "] to BigDecimal",
					e
			);
		}
	}

	@SuppressWarnings("unchecked")
	private <J> BasicValuedExpressableType<J> resolveExpressableTypeBasic(Class<J> javaType) {
		return creationContext.getDomainModel().getTypeConfiguration().standardExpressableTypeForJavaType( javaType );
	}

	@Override
	public Object visitParameterExpression(HqlParser.ParameterExpressionContext ctx) {
		return ctx.parameter().accept( this );
	}

	@Override
	public SqmNamedParameter visitNamedParameter(HqlParser.NamedParameterContext ctx) {
		final SqmNamedParameter param = new SqmNamedParameter(
				ctx.identifier().getText(),
				parameterDeclarationContextStack.getCurrent().isMultiValuedBindingAllowed()
		);
		parameterCollector.addParameter( param );
		return param;
	}

	@Override
	public SqmPositionalParameter visitPositionalParameter(HqlParser.PositionalParameterContext ctx) {
		if ( ctx.INTEGER_LITERAL() == null ) {
			throw new SemanticException( "Encountered positional parameter which did not declare position (? instead of, e.g., ?1)" );
		}
		final SqmPositionalParameter param = new SqmPositionalParameter(
				Integer.valueOf( ctx.INTEGER_LITERAL().getText() ),
				parameterDeclarationContextStack.getCurrent().isMultiValuedBindingAllowed()
		);
		parameterCollector.addParameter( param );
		return param;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Functions

	@Override
	public SqmGenericFunction visitJpaNonStandardFunction(HqlParser.JpaNonStandardFunctionContext ctx) {
		final String functionName = ctx.jpaNonStandardFunctionName().STRING_LITERAL().getText();
		final List<SqmExpression> functionArguments = visitNonStandardFunctionArguments( ctx.nonStandardFunctionArguments() );

		// todo : integrate some form of SqlFunction look-up using the ParsingContext so we can resolve the "type"
		return new SqmGenericFunction( functionName, null, functionArguments );
	}

	@Override
	public SqmGenericFunction visitNonStandardFunction(HqlParser.NonStandardFunctionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw new StrictJpaComplianceViolation(
					"Encountered non-compliant non-standard function call [" +
							ctx.nonStandardFunctionName() + "], but strict JPQL compliance was requested; use JPA's FUNCTION(functionName[,...]) syntax name instead",
					StrictJpaComplianceViolation.Type.FUNCTION_CALL
			);
		}

		final String functionName = ctx.nonStandardFunctionName().getText();
		final List<SqmExpression> functionArguments = visitNonStandardFunctionArguments( ctx.nonStandardFunctionArguments() );

		// todo : integrate some form of SqlFunction look-up using the ParsingContext so we can resolve the "type"
		return new SqmGenericFunction( functionName, null, functionArguments );
	}

	@Override
	public List<SqmExpression> visitNonStandardFunctionArguments(HqlParser.NonStandardFunctionArgumentsContext ctx) {
		final List<SqmExpression> arguments = new ArrayList<>();

		for ( int i=0, x=ctx.expression().size(); i<x; i++ ) {
			// we handle the final argument differently...
			if ( i == x-1 ) {
				arguments.add( visitFinalFunctionArgument( ctx.expression( i ) ) );
			}
			else {
				arguments.add( (SqmExpression) ctx.expression( i ).accept( this ) );
			}
		}

		return arguments;
	}

	private SqmExpression visitFinalFunctionArgument(HqlParser.ExpressionContext expression) {
		// the final argument to a function may accept multi-value parameter (varargs),
		// 		but only if we are operating in non-strict JPA mode
		parameterDeclarationContextStack.push( () -> creationOptions.useStrictJpaCompliance() );
		try {
			return (SqmExpression) expression.accept( this );
		}
		finally {
			parameterDeclarationContextStack.pop();
		}
	}

	@Override
	public SqmAggregateFunction visitAggregateFunction(HqlParser.AggregateFunctionContext ctx) {
		return (SqmAggregateFunction) super.visitAggregateFunction( ctx );
	}

	@Override
	public SqmExpression visitAbsFunction(HqlParser.AbsFunctionContext ctx) {
		return generateSingleArgFunction(
				(sqmFunctionTemplate, sqmArgument) -> sqmFunctionTemplate.makeSqmFunctionExpression(
						Collections.singletonList( sqmArgument ),
						(AllowableFunctionReturnType) sqmArgument.getExpressableType()
				),
				arguments -> new SqmAbsFunction( arguments, null ),
				SqmAbsFunction.NAME,
				ctx.expression()
		);
	}

	@Override
	public SqmExpression visitAvgFunction(HqlParser.AvgFunctionContext ctx) {
		return generateAggregateFunction(
				(sqmFunctionTemplate, sqmArgument) -> sqmFunctionTemplate.makeSqmFunctionExpression(
						Collections.singletonList( sqmArgument ),
						(AllowableFunctionReturnType) sqmArgument.getExpressableType()
				),
				sqmArgument -> new SqmAvgFunction(
						sqmArgument,
						(AllowableFunctionReturnType) sqmArgument.getExpressableType()
				),
				SqmAvgFunction.NAME,
				ctx.DISTINCT() != null,
				ctx.expression()
		);
	}

	@Override
	public SqmExpression visitCastFunction(HqlParser.CastFunctionContext ctx) {
		final SqmFunctionTemplate template = creationContext.getFunctionResolver().apply( SqmCastFunction.NAME );

		final SqmExpression expressionToCast = (SqmExpression) ctx.expression().accept( this );
		final SqmExpression castTargetExpression = interpretCastTarget( ctx.castTarget() );

		//getSessionFactory().getTypeConfiguration().resolveCastTargetType( ctx.dataType().IDENTIFIER().getText() )

		if ( !AllowableFunctionReturnType.class.isInstance( castTargetExpression ) ) {
			throw new SqmProductionException( "Found cast target expression [%s] which is not allowed as a function return" );
		}

		if ( template == null ) {
			// use the standard CAST support
			return new SqmCastFunction(
					expressionToCast,
					(AllowableFunctionReturnType) castTargetExpression,
					castTargetExpression.getExpressableType().toString()
			);
		}
		else {
			return template.makeSqmFunctionExpression(
					Arrays.asList( expressionToCast, castTargetExpression ),
					(AllowableFunctionReturnType) castTargetExpression.getExpressableType()
			);
		}
	}

	private SqmExpression interpretCastTarget(HqlParser.CastTargetContext castTargetContext) {
		// todo (6.0) : what are the allowable forms of specifying cast-target?
		// 		the exactness of this is being discussed on the dev ml.  Most
		//		likely we will accept either:
		//			1) a String, which might represent:
		//				a) a Java type name
		//				b) a java.sql.Types field name (e.g. VARCHAR...)
		//				c) (not huge fan of this...) a "pass-thru" value
		//			2) an int signifying the SqlTypeDescriptor (JDBC type code)

		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public SqmConcatFunction visitConcatFunction(HqlParser.ConcatFunctionContext ctx) {
		final List<SqmExpression> arguments = new ArrayList<>();
		for ( HqlParser.ExpressionContext argument : ctx.expression() ) {
			arguments.add( (SqmExpression) argument.accept( this ) );
		}

		return new SqmConcatFunction( (BasicValuedExpressableType) arguments.get( 0 ).getExpressableType(), arguments );
	}

	@Override
	public SqmExpression visitCountFunction(HqlParser.CountFunctionContext ctx) {
		final boolean isCountStar = ctx.ASTERISK() != null;
		final BasicValuedExpressableType longType = resolveExpressableTypeBasic( Long.class );

		return generateAggregateFunction(
				(sqmFunctionTemplate, sqmArgument) -> {
					final List<SqmExpression> arguments = isCountStar
							? Collections.singletonList( SqmCountStarFunction.STAR )
							: Collections.singletonList( sqmArgument );
					return sqmFunctionTemplate.makeSqmFunctionExpression(
							arguments,
							(AllowableFunctionReturnType) sqmArgument.getExpressableType()
					);
				},
				sqmArgument -> isCountStar
						? new SqmCountStarFunction( longType )
						:  new SqmCountFunction( sqmArgument, longType ),
				SqmCountFunction.NAME,
				ctx.DISTINCT() != null, ctx.expression()
		);
	}

	@Override
	public Object visitLengthFunction(HqlParser.LengthFunctionContext ctx) {
		final SqmExpression sqmExpression = (SqmExpression) ctx.expression().accept( this );
		return new SqmLengthFunction( sqmExpression, resolveExpressableTypeBasic( Long.class ) );
	}

	@Override
	public Object visitLocateFunction(HqlParser.LocateFunctionContext ctx) {
		return super.visitLocateFunction( ctx );
	}

	@Override
	public SqmExpression visitMaxFunction(HqlParser.MaxFunctionContext ctx) {
		return generateAggregateFunction(
				(template, sqmArgument) -> template.makeSqmFunctionExpression(
						Collections.singletonList( sqmArgument ),
						(BasicValuedExpressableType) sqmArgument.getExpressableType()
				),
				sqmArgument -> new SqmMaxFunction(
						sqmArgument,
						(BasicValuedExpressableType) sqmArgument.getExpressableType()
				),
				SqmCountFunction.NAME,
				ctx.DISTINCT() != null, ctx.expression()
		);
	}

	@Override
	public Object visitStrFunction(HqlParser.StrFunctionContext ctx) {
		final SqmExpression sqmExpression = (SqmExpression) ctx.expression().accept( this );
		return new SqmStrFunction( sqmExpression, resolveExpressableTypeBasic( String.class ) );
	}

	private SqmExpression generateAggregateFunction(
			BiFunction<SqmFunctionTemplate, SqmExpression, SqmExpression> templatedGenerator,
			Function<SqmExpression, SqmExpression> nonTemplatedGenerator,
			String name,
			boolean isDistinct,
			HqlParser.ExpressionContext antlrArgumentExpression) {
		final SqmFunctionTemplate template = creationContext.getFunctionResolver().apply( name );

		final SqmExpression sqmArgument = (SqmExpression) antlrArgumentExpression.accept( this );

		final SqmExpression result;
		if ( template == null ) {
			result = nonTemplatedGenerator.apply( sqmArgument );
		}
		else {
			result = templatedGenerator.apply( template, sqmArgument );
		}

		if ( isDistinct ) {
			applyDistinct( result );
		}

		return result;
	}

	private void applyDistinct(SqmExpression result) {
		if ( result instanceof Distinctable ) {
			( (Distinctable) result ).makeDistinct();
		}
		else {
			log.debugf( "COUNT SqmFunction result [%s] did not implement %s; cannot apply DISTINCT", result, Distinctable.class.getName() );
		}
	}

	private SqmExpression generateSingleArgFunction(
			BiFunction<SqmFunctionTemplate, SqmExpression, SqmExpression> templatedGenerator,
			Function<SqmExpression, SqmExpression> nonTemplatedGenerator,
			String name,
			HqlParser.ExpressionContext antlrArgument) {
		final SqmFunctionTemplate template = creationContext.getFunctionResolver().apply( name );

		final SqmExpression sqmArgument = (SqmExpression) antlrArgument.accept( this );

		if ( template == null ) {
			return nonTemplatedGenerator.apply( sqmArgument );
		}
		else {
			return templatedGenerator.apply( template, sqmArgument );
		}
	}

	@Override
	public SqmExpression visitMinFunction(HqlParser.MinFunctionContext ctx) {
		return generateAggregateFunction(
				(sqmFunctionTemplate, sqmArgument) -> sqmFunctionTemplate.makeSqmFunctionExpression(
						Collections.singletonList( sqmArgument ),
						(AllowableFunctionReturnType) sqmArgument.getExpressableType()
				),
				sqmArgument -> new SqmMinFunction(
						sqmArgument,
						(AllowableFunctionReturnType) sqmArgument.getExpressableType()
				),
				SqmCountFunction.NAME,
				ctx.DISTINCT() != null,
				ctx.expression()
		);
	}

	@Override
	public SqmExpression visitSubstringFunction(HqlParser.SubstringFunctionContext ctx) {
		final SqmExpression source = (SqmExpression) ctx.expression().accept( this );
		final SqmExpression start = (SqmExpression) ctx.substringFunctionStartArgument().accept( this );
		final SqmExpression length = ctx.substringFunctionLengthArgument() == null
				? null
				: (SqmExpression) ctx.substringFunctionLengthArgument().accept( this );
		return new SqmSubstringFunction( (BasicValuedExpressableType) source.getExpressableType(), source, start, length );
	}

	@Override
	public SqmExpression visitSumFunction(HqlParser.SumFunctionContext ctx) {
		return generateAggregateFunction(
				(sqmFunctionTemplate, sqmArgument) -> sqmFunctionTemplate.makeSqmFunctionExpression(
						Collections.singletonList( sqmArgument ),
						(AllowableFunctionReturnType) sqmArgument.getExpressableType()
				),
				sqmArgument -> new SqmSumFunction(
						sqmArgument,
						(AllowableFunctionReturnType) sqmArgument.getExpressableType()
				),
				SqmSumFunction.NAME,
				ctx.DISTINCT() != null,
				ctx.expression()
		);
	}

	@Override
	public SqmExpression visitTrimFunction(HqlParser.TrimFunctionContext ctx) {
		final SqmExpression source = (SqmExpression) ctx.expression().accept( this );

		final SqmFunctionTemplate trimFunctionTemplate = creationContext.getFunctionResolver().apply( "trim" );

		if ( trimFunctionTemplate != null ) {
			return trimFunctionTemplate.makeSqmFunctionExpression(
					Arrays.asList(
							TrimSpecificationExpressionWrapper.wrap( visitTrimSpecification( ctx.trimSpecification() ) ),
							visitTrimCharacter( ctx.trimCharacter() ),
							source
					),
					resolveExpressableTypeBasic( String.class )
			);
		}

		return new SqmTrimFunction(
				(BasicValuedExpressableType) source.getExpressableType(),
				visitTrimSpecification( ctx.trimSpecification() ),
				visitTrimCharacter( ctx.trimCharacter() ),
				source
		);
	}

	@Override
	public TrimSpecification visitTrimSpecification(HqlParser.TrimSpecificationContext ctx) {
		if ( ctx.LEADING() != null ) {
			return TrimSpecification.LEADING;
		}
		else if ( ctx.TRAILING() != null ) {
			return TrimSpecification.TRAILING;
		}

		// JPA says the default is BOTH
		return TrimSpecification.BOTH;
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
					resolveExpressableTypeBasic( Character.class )
			);
		}
		if ( ctx.STRING_LITERAL() != null ) {
			final String trimCharText = ctx.STRING_LITERAL().getText();
			if ( trimCharText.length() != 1 ) {
				throw new SemanticException( "Expecting [trim character] for TRIM function to be  single character, found : " + trimCharText );
			}
			return new SqmLiteral<>(
					trimCharText.charAt( 0 ),
					resolveExpressableTypeBasic( Character.class ));
		}

		// JPA says space is the default
		return new SqmLiteral<>(
				' ',
				resolveExpressableTypeBasic( Character.class )
		);
	}

	@Override
	public SqmUpperFunction visitUpperFunction(HqlParser.UpperFunctionContext ctx) {
		final SqmExpression expression = (SqmExpression) ctx.expression().accept( this );
		return new SqmUpperFunction(
				(BasicValuedExpressableType) expression.getExpressableType(),
				expression
		);
	}

	@Override
	public SqmLowerFunction visitLowerFunction(HqlParser.LowerFunctionContext ctx) {
		// todo (6.0) : why pass both the expression and its expression-type?
		//			can't we just pass the expression?
		final SqmExpression expression = (SqmExpression) ctx.expression().accept( this );
		return new SqmLowerFunction(
				(BasicValuedExpressableType) expression.getExpressableType(),
				expression
		);
	}

	@Override
	public SqmCollectionSize visitCollectionSizeFunction(HqlParser.CollectionSizeFunctionContext ctx) {
		return new SqmCollectionSize(
				consumeDomainPath( ctx.path() ),
				resolveExpressableTypeBasic( Integer.class )
		);
	}

	private SqmPluralAttributeReference asPluralAttribute(SqmNavigableReference attributeBinding) {
		if ( !SqmPluralAttributeReference.class.isInstance( attributeBinding ) ) {
			throw new SemanticException( "Expecting plural-attribute, but found : " + attributeBinding );
		}

		return (SqmPluralAttributeReference) attributeBinding;
	}

	@Override
	public SqmPath visitCollectionIndexFunction(HqlParser.CollectionIndexFunctionContext ctx) {
		final String alias = ctx.identifier().getText();

		final SqmFrom sqmFrom = processingStateStack.getCurrent().getPathRegistry().findFromByAlias( alias );

		if ( sqmFrom == null ) {
			throw new ParsingException( "Could not resolve identification variable [" + alias + "] to SqmFrom" );
		}

		final PluralValuedNavigable pluralValuedNavigable = sqmFrom.as(
				PluralValuedNavigable.class,
				() -> new ParsingException( "Could not resolve identification variable [" + alias + "] as plural-attribute" )
		);

		if ( pluralValuedNavigable.getCollectionDescriptor().getIndexDescriptor() == null ) {
			throw new SemanticException(
					"index() function can only be applied to identification variables which resolve to an " +
							"indexed collection (map,list); specified identification variable [" + alias +
							"] resolved to " + sqmFrom
			);
		}

		return pluralValuedNavigable.getCollectionDescriptor()
				.getIndexDescriptor()
				.createSqmExpression( sqmFrom, this );
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	private boolean isIndexedPluralAttribute(SqmPluralAttributeReference attributeBinding) {
		return attributeBinding.getReferencedNavigable().getPersistentCollectionDescriptor().getCollectionClassification() == CollectionClassification.MAP
				|| attributeBinding.getReferencedNavigable().getPersistentCollectionDescriptor().getCollectionClassification() == CollectionClassification.LIST;
	}

	@Override
	public SqmMaxElementReference visitMaxElementFunction(HqlParser.MaxElementFunctionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.HQL_COLLECTION_FUNCTION );
		}

		final SqmPluralAttributeReference pluralAttributeBinding = asPluralAttribute( (SqmNavigableReference) ctx.path().accept( this ) );
		switch ( pluralAttributeBinding.getReferencedNavigable().getPersistentCollectionDescriptor().getElementDescriptor().getClassification() ) {
			case BASIC: {
				return new SqmMaxElementReferenceBasic( pluralAttributeBinding );
			}
			case EMBEDDABLE: {
				return new SqmMaxElementReferenceEmbedded( pluralAttributeBinding );
			}
			case ONE_TO_MANY:
			case MANY_TO_MANY: {
				return new SqmMaxElementReferenceEntity( pluralAttributeBinding );
			}
			default: {
				throw new NotYetImplementedException( "min-element function not yet supported for ANY elements" );
			}
		}

	}

	@Override
	public SqmMinElementReference visitMinElementFunction(HqlParser.MinElementFunctionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.HQL_COLLECTION_FUNCTION );
		}

		final SqmPluralAttributeReference pluralAttributeBinding = asPluralAttribute( (SqmNavigableReference) ctx.path().accept( this ) );
		switch ( pluralAttributeBinding.getReferencedNavigable().getPersistentCollectionDescriptor().getElementDescriptor().getClassification() ) {
			case BASIC: {
				return new SqmMinElementReferenceBasic( pluralAttributeBinding );
			}
			case EMBEDDABLE: {
				return new SqmMinElementReferenceEmbedded( pluralAttributeBinding );
			}
			case ONE_TO_MANY:
			case MANY_TO_MANY: {
				return new SqmMinElementReferenceEntity( pluralAttributeBinding );
			}
			default: {
				throw new NotYetImplementedException( "min-element function not yet supported for ANY elements" );
			}
		}
	}

	@Override
	public SqmMaxIndexReference visitMaxIndexFunction(HqlParser.MaxIndexFunctionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.HQL_COLLECTION_FUNCTION );
		}

		final SqmPluralAttributeReference attributeBinding = asPluralAttribute( (SqmNavigableReference) ctx.path().accept( this ) );
		if ( !isIndexedPluralAttribute( attributeBinding ) ) {
			throw new SemanticException(
					"maxindex() function can only be applied to path expressions which resolve to an " +
							"indexed collection (list,map); specified path [" + ctx.path().getText() +
							"] resolved to " + attributeBinding.getReferencedNavigable()
			);
		}

		switch ( attributeBinding.getReferencedNavigable().getPersistentCollectionDescriptor().getIndexDescriptor().getClassification() ) {
			case BASIC: {
				return new SqmMaxIndexReferenceBasic( attributeBinding );
			}
			case EMBEDDABLE: {
				return new SqmMaxIndexReferenceEmbedded( attributeBinding );
			}
			case ONE_TO_MANY:
			case MANY_TO_MANY: {
				return new SqmMaxIndexReferenceEntity( attributeBinding );
			}
			default: {
				throw new NotYetImplementedException(  );
			}
		}
	}

	@Override
	public SqmMinIndexReference visitMinIndexFunction(HqlParser.MinIndexFunctionContext ctx) {
		if ( creationOptions.useStrictJpaCompliance() ) {
			throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.HQL_COLLECTION_FUNCTION );
		}

		final SqmPluralAttributeReference attributeBinding = asPluralAttribute( (SqmNavigableReference) ctx.path().accept( this ) );
		if ( !isIndexedPluralAttribute( attributeBinding ) ) {
			throw new SemanticException(
					"minindex() function can only be applied to path expressions which resolve to an " +
							"indexed collection (list,map); specified path [" + ctx.path().getText() +
							"] resolved to " + attributeBinding.getReferencedNavigable()
			);
		}

		switch ( attributeBinding.getReferencedNavigable().getPersistentCollectionDescriptor().getIndexDescriptor().getClassification() ) {
			case BASIC: {
				return new SqmMinIndexReferenceBasic( attributeBinding );
			}
			case EMBEDDABLE: {
				return new SqmMinIndexReferenceEmbeddable( attributeBinding );
			}
			case ONE_TO_MANY:
			case MANY_TO_MANY: {
				return new SqmMinIndexReferenceEntity( attributeBinding );
			}
			default: {
				throw new NotYetImplementedException(  );
			}
		}
	}

	@Override
	public SqmSubQuery visitSubQueryExpression(HqlParser.SubQueryExpressionContext ctx) {
		if ( ctx.querySpec().selectClause() == null ) {
			throw new SemanticException( "Sub-query cannot use implicit select-clause : " + ctx.getText() );
		}

		final SqmQuerySpec querySpec = visitQuerySpec( ctx.querySpec() );
		return new SqmSubQuery( querySpec, determineTypeDescriptor( querySpec.getSelectClause() ) );
	}

	private static ExpressableType determineTypeDescriptor(SqmSelectClause selectClause) {
		if ( selectClause.getSelections().size() != 1 ) {
			return null;
		}

		final SqmSelectableNode selectableNode = selectClause.getSelections().get( 0 ).getSelectableNode();
		if ( SqmDynamicInstantiation.class.isInstance( selectableNode ) ) {
			throw new HibernateException( "Illegal use of dynamic-instantiation in sub-query" );
		}

		return ( (SqmExpression) selectableNode ).getExpressableType();
	}
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Path structures

	@Override
	public SemanticPathPart visitPath(HqlParser.PathContext ctx) {
		if ( ctx.syntacticDomainPath() != null ) {
			final SemanticPathPart syntacticNavigablePathResult = visitSyntacticDomainPath( ctx.syntacticDomainPath() );
			if ( ctx.pathContinuation() != null ) {
				identifierConsumerStack.push(
						new BasicDotIdentifierConsumer( syntacticNavigablePathResult, processingStateStack::getCurrent ) {
							@Override
							protected void reset() {
							}
						}
				);
				try {
					return (SemanticPathPart) ctx.pathContinuation().accept( this );
				}
				finally {
					identifierConsumerStack.pop();
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
			final SqmNavigableJoin indexedJoinPath = (SqmNavigableJoin) ctx.dotIdentifierSequence().accept( this );
			return new SqmIndexedCollectionAccessPath(
					indexedJoinPath,
					(SqmExpression) ctx.indexedPathAccessFragment().accept( this )
			);
		}

		throw new ParsingException( "Unsure how to process `syntacticDomainPath` over : " + ctx.getText() );
	}


	@Override
	public SemanticPathPart visitDotIdentifierSequence(HqlParser.DotIdentifierSequenceContext ctx) {
		final int numberOfContinuations = ctx.dotIdentifierSequenceContinuation().size();
		final boolean hasContinuations = numberOfContinuations != 0;

		final DotIdentifierConsumer dotIdentifierConsumer = identifierConsumerStack.getCurrent();

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
	public SqmPath visitTreatedNavigablePath(HqlParser.TreatedNavigablePathContext ctx) {
		// todo (6.0) : we just do not have enough info here to do this...
		//		much of this depends on where the TREAT occurs:
		//			* is the downcast intrinsic or incidental?
		//			* DowncastLocation


		final SqmPath sqmPath = consumeNavigableContainerReference( ctx.path() );
		final String treatTargetName = ctx.dotIdentifierSequence().getText();
		final EntityTypeDescriptor<Object> treatTarget = getCreationContext().getDomainModel().getEntityDescriptor( treatTargetName );
		return new SqmTreatedPath( sqmPath, treatTarget );
	}

	@Override
	public SqmPath visitCollectionElementNavigablePath(HqlParser.CollectionElementNavigablePathContext ctx) {
		final SqmPath sqmPath = consumeNavigableContainerReference( ctx.path() );
		final PluralValuedNavigable pluralValuedNavigable = sqmPath.as( PluralValuedNavigable.class );

		if ( getCreationOptions().useStrictJpaCompliance() ) {
			if ( pluralValuedNavigable.getCollectionDescriptor().getCollectionClassification() != CollectionClassification.MAP ) {
				throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.VALUE_FUNCTION_ON_NON_MAP );
			}
		}

		SqmPath result = pluralValuedNavigable.getCollectionDescriptor()
				.getElementDescriptor()
				.createSqmExpression( sqmPath, this );

		if ( ctx.pathContinuation() != null ) {
			result = consumeDomainPath( ctx.path() );
		}

		return result;
	}


	@Override
	public SqmPath visitMapKeyNavigablePath(HqlParser.MapKeyNavigablePathContext ctx) {
		final SqmPath sqmPath = consumeDomainPath( ctx.path() );
		final SqmPath result = sqmPath.as( PersistentCollectionDescriptor.class )
				.getIndexDescriptor()
				.createSqmExpression( sqmPath, this );

		if ( ctx.pathContinuation() != null ) {
			return consumeDomainPath( ctx.path() );
		}

		return result;
	}


	private SqmPath consumeDomainPath(HqlParser.PathContext parserPath) {
		parserPath.accept( this );
		final DotIdentifierConsumer dotIdentifierConsumer = identifierConsumerStack.getCurrent();
		final SemanticPathPart consumedPart = dotIdentifierConsumer.getConsumedPart();
		if ( consumedPart instanceof SqmPath ) {
			return (SqmPath) consumedPart;
		}

		throw new SemanticException( "Expecting domain-model path, but found : " + consumedPart );
	}

	private SqmPath consumeNavigableContainerReference(HqlParser.PathContext parserPath) {
		final SqmPath sqmPath = consumeDomainPath( parserPath );

		final Navigable navigable = sqmPath.getReferencedNavigable();
		if ( navigable instanceof NavigableContainer ) {
			return sqmPath;
		}

		throw new SemanticException( "Expecting NavigableContainer-valued path, but found : " + navigable );
	}

	private SqmNavigableJoin consumePluralAttributeReference(HqlParser.PathContext parserPath) {
		final SqmPath sqmPath = consumeDomainPath( parserPath );

		final Navigable navigable = sqmPath.getReferencedNavigable();

		try {
			sqmPath.as( PluralValuedNavigable.class );
			return (SqmNavigableJoin) sqmPath;
		}
		catch (Exception e) {
			throw new SemanticException( "Expecting PluralAttribute-valued path, but found : " + navigable );
		}
	}

	private interface TreatHandler {
		void addDowncast(SqmFrom sqmFrom, IdentifiableTypeDescriptor downcastTarget);
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
				IdentifiableTypeDescriptor downcastTarget) {
			( (MutableUsageDetails) sqmFrom.getUsageDetails() ).addDownCast( false, downcastTarget, downcastLocation );
		}
	}

	private static class TreatHandlerFromClause implements TreatHandler {
		@Override
		public void addDowncast(
				SqmFrom sqmFrom,
				IdentifiableTypeDescriptor downcastTarget) {
			( (MutableUsageDetails) sqmFrom.getUsageDetails() ).addDownCast( true, downcastTarget, DowncastLocation.FROM );
		}
	}
}
