/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.internal.hql;

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
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.metamodel.model.domain.spi.CollectionElement;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.LiteralNumberFormatException;
import org.hibernate.query.sqm.NotYetImplementedException;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.StrictJpaComplianceViolation;
import org.hibernate.query.sqm.UnknownEntityException;
import org.hibernate.query.sqm.produce.SqmProductionException;
import org.hibernate.query.sqm.produce.function.SqmFunctionTemplate;
import org.hibernate.query.sqm.produce.internal.QuerySpecProcessingStateDmlImpl;
import org.hibernate.query.sqm.produce.internal.QuerySpecProcessingStateStandardImpl;
import org.hibernate.query.sqm.produce.internal.SqmFromBuilderFromClauseQualifiedJoin;
import org.hibernate.query.sqm.produce.internal.SqmFromBuilderFromClauseStandard;
import org.hibernate.query.sqm.produce.internal.SqmFromBuilderStandard;
import org.hibernate.query.sqm.produce.internal.UniqueIdGenerator;
import org.hibernate.query.sqm.produce.internal.hql.grammar.HqlParser;
import org.hibernate.query.sqm.produce.internal.hql.grammar.HqlParserBaseVisitor;
import org.hibernate.query.sqm.produce.path.internal.SemanticPathPartJoinPredicate;
import org.hibernate.query.sqm.produce.path.internal.SemanticPathPartNamedEntity;
import org.hibernate.query.sqm.produce.path.internal.SemanticPathPartRoot;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.CurrentSqmFromElementSpaceCoordAccess;
import org.hibernate.query.sqm.produce.spi.ImplicitAliasGenerator;
import org.hibernate.query.sqm.produce.spi.ParameterDeclarationContext;
import org.hibernate.query.sqm.produce.spi.QuerySpecProcessingState;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.produce.spi.SqmFromBuilder;
import org.hibernate.query.sqm.produce.spi.TrimSpecificationExpressionWrapper;
import org.hibernate.query.sqm.tree.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.SqmQuerySpec;
import org.hibernate.query.sqm.tree.SqmSelectStatement;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.SqmUpdateStatement;
import org.hibernate.query.sqm.tree.expression.ImpliedTypeSqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmCaseSearched;
import org.hibernate.query.sqm.tree.expression.SqmCaseSimple;
import org.hibernate.query.sqm.tree.expression.SqmCollectionSize;
import org.hibernate.query.sqm.tree.expression.SqmConcat;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
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
import org.hibernate.query.sqm.tree.expression.SqmParameterizedEntityType;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;
import org.hibernate.query.sqm.tree.expression.SqmSubQuery;
import org.hibernate.query.sqm.tree.expression.SqmUnaryOperation;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionElementReferenceBasic;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionElementReferenceEmbedded;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionElementReferenceEntity;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionIndexReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmEmbeddableTypedReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityTypeExpression;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityTypedReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmMapEntryBinding;
import org.hibernate.query.sqm.tree.expression.domain.SqmMaxElementReferenceBasic;
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
import org.hibernate.query.sqm.tree.expression.function.SqmLowerFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmMaxFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmMinFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmNullifFunction;
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
import org.hibernate.query.sqm.tree.from.SqmFromElementSpace;
import org.hibernate.query.sqm.tree.from.SqmFromExporter;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;
import org.hibernate.query.sqm.tree.from.SqmQualifiedJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.internal.ParameterCollector;
import org.hibernate.query.sqm.tree.internal.SqmDeleteStatementImpl;
import org.hibernate.query.sqm.tree.internal.SqmInsertSelectStatementImpl;
import org.hibernate.query.sqm.tree.internal.SqmSelectStatementImpl;
import org.hibernate.query.sqm.tree.internal.SqmUpdateStatementImpl;
import org.hibernate.query.sqm.tree.order.SqmOrderByClause;
import org.hibernate.query.sqm.tree.order.SqmSortOrder;
import org.hibernate.query.sqm.tree.order.SqmSortSpecification;
import org.hibernate.query.sqm.tree.paging.SqmLimitOffsetClause;
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
import org.hibernate.query.sqm.tree.predicate.RelationalPredicateOperator;
import org.hibernate.query.sqm.tree.predicate.RelationalSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationArgument;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.EntityValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.PolymorphicEntityValuedExpressableType;
import org.hibernate.sql.ast.tree.spi.TrimSpecification;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.spi.StandardSpiBasicTypes;

import org.jboss.logging.Logger;

import org.antlr.v4.runtime.Token;

/**
 * @author Steve Ebersole
 */
public class SemanticQueryBuilder
		extends HqlParserBaseVisitor
		implements SqmCreationContext, CurrentSqmFromElementSpaceCoordAccess {

	private static final Logger log = Logger.getLogger( SemanticQueryBuilder.class );

	/**
	 * Main entry point into analysis of HQL/JPQL parse tree - producing a semantic model of the
	 * query.
	 *
	 * @param statement The statement to analyze.
	 * @param sessionFactory Access to things needed to perform the analysis
	 *
	 * @return The semantic query model
	 */
	public static SqmStatement buildSemanticModel(HqlParser.StatementContext statement, SessionFactoryImplementor sessionFactory) {
		return new SemanticQueryBuilder( sessionFactory ).visitStatement( statement );
	}

	private final SessionFactoryImplementor sessionFactory;
	private final ImplicitAliasGenerator implicitAliasGenerator;
	private final UniqueIdGenerator uidGenerator;

	private final SqmFromBuilderStandard standardSqmFromBuilder = new SqmFromBuilderStandard( this );

	private final Stack<SemanticPathPart> semanticPathPartStack = new StandardStack<>( new SemanticPathPartRoot() );
	private final Stack<SqmFromBuilder> fromBuilderStack = new StandardStack<>( standardSqmFromBuilder );
	private final Stack<TreatHandler> treatHandlerStack = new StandardStack<>( new TreatHandlerNormal() );

	private SqmFromElementSpace currentFromElementSpace;

	private Map<NavigablePath,SqmNavigableReference> navigableReferenceByPath;

	private final Stack<ParameterDeclarationContext> parameterDeclarationContextStack = new StandardStack<>();
	private final Stack<QuerySpecProcessingState> querySpecProcessingStateStack = new StandardStack<>();

	private ParameterCollector parameterCollector;


	protected SemanticQueryBuilder(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;

		this.implicitAliasGenerator = new ImplicitAliasGenerator();
		this.uidGenerator = new UniqueIdGenerator();
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
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

	public Map<NavigablePath, Set<SqmNavigableJoin>> fetchJoinsByParentPath;

	@Override
	public void registerFetch(SqmNavigableContainerReference sourceReference, SqmNavigableJoin navigableJoin) {
		Set<SqmNavigableJoin> joins = null;

		if ( fetchJoinsByParentPath == null ) {
			fetchJoinsByParentPath = new HashMap<>();
		}
		else {
			joins = fetchJoinsByParentPath.get( sourceReference.getNavigablePath() );
		}

		if ( joins == null ) {
			joins = new HashSet<>();
			fetchJoinsByParentPath.put( sourceReference.getNavigablePath(), joins );
		}

		joins.add( navigableJoin );
	}

	@Override
	public SqmFromElementSpace getCurrentFromElementSpace() {
		return currentFromElementSpace;
	}

	@Override
	public CurrentSqmFromElementSpaceCoordAccess getCurrentSqmFromElementSpaceCoordAccess() {
		return this;
	}

	@Override
	public void setCurrentSqmFromElementSpace(SqmFromElementSpace space) {
		this.currentFromElementSpace = space;
	}

	@Override
	public SqmFromBuilder getCurrentFromElementBuilder() {
		return fromBuilderStack.getCurrent();
	}

	@Override
	public QuerySpecProcessingState getCurrentQuerySpecProcessingState() {
		return querySpecProcessingStateStack.getCurrent();
	}

	protected <T> void primeStack(Stack<T> stack, T initialValue) {
		stack.push( initialValue );
	}

	protected Stack<ParameterDeclarationContext> getParameterDeclarationContextStack() {
		return parameterDeclarationContextStack;
	}

	protected Stack<QuerySpecProcessingState> getQuerySpecProcessingStateStack() {
		return querySpecProcessingStateStack;
	}

	protected Stack<SemanticPathPart> getSemanticPathPartStack() {
		return semanticPathPartStack;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Grammar rules

	@Override
	public SqmStatement visitStatement(HqlParser.StatementContext ctx) {
		// parameters allow multi-valued bindings only in very limited cases, so for
		// the base case here we say false
		parameterDeclarationContextStack.push( () -> false );

		try {
			if ( ctx.insertStatement() != null ) {
				return visitInsertStatement( ctx.insertStatement() );
			}
			else if ( ctx.updateStatement() != null ) {
				return visitUpdateStatement( ctx.updateStatement() );
			}
			else if ( ctx.deleteStatement() != null ) {
				return visitDeleteStatement( ctx.deleteStatement() );
			}
			else if ( ctx.selectStatement() != null ) {
				return visitSelectStatement( ctx.selectStatement() );
			}
		}
		finally {
			parameterDeclarationContextStack.pop();
		}

		throw new ParsingException( "Unexpected statement type [not INSERT, UPDATE, DELETE or SELECT] : " + ctx.getText() );
	}

	@Override
	public SqmSelectStatement visitSelectStatement(HqlParser.SelectStatementContext ctx) {
		if ( getSessionFactory().getSessionFactoryOptions().isStrictJpaQueryLanguageCompliance() ) {
			if ( ctx.querySpec().selectClause() == null ) {
				throw new StrictJpaComplianceViolation(
						"Encountered implicit select-clause, but strict JPQL compliance was requested",
						StrictJpaComplianceViolation.Type.IMPLICIT_SELECT
				);
			}
		}

		final SqmSelectStatementImpl selectStatement = new SqmSelectStatementImpl();
		parameterCollector = selectStatement;

		try {
			selectStatement.applyQuerySpec( visitQuerySpec( ctx.querySpec() ), fetchJoinsByParentPath );
		}
		finally {
			// todo (6.0) : should this really happen on error?
			selectStatement.wrapUp();
		}

		return selectStatement;
	}

	@Override
	public SqmQuerySpec visitQuerySpec(HqlParser.QuerySpecContext ctx) {
		querySpecProcessingStateStack.push(
				new QuerySpecProcessingStateStandardImpl(
						this,
						querySpecProcessingStateStack.getCurrent()
				)
		);

		try {
			// visit from-clause first!!!
			treatHandlerStack.push( new TreatHandlerFromClause() );
			try {
				visitFromClause( ctx.fromClause() );
			}
			finally {
				treatHandlerStack.pop();
			}

			final SqmSelectClause selectClause;
			if ( ctx.selectClause() != null ) {
				selectClause = visitSelectClause( ctx.selectClause() );
			}
			else {
				log.info( "Encountered implicit select clause which is a deprecated feature : " + ctx.getText() );
				selectClause = buildInferredSelectClause( querySpecProcessingStateStack.getCurrent().getFromClause() );
			}

			final SqmWhereClause whereClause;
			if ( ctx.whereClause() != null ) {
				treatHandlerStack.push( new TreatHandlerNormal( DowncastLocation.WHERE ) );
				try {

					whereClause = new SqmWhereClause( (SqmPredicate) ctx.whereClause().accept( this ) );
				}
				finally {
					treatHandlerStack.pop();
				}
			}
			else {
				whereClause = null;
			}

			final SqmOrderByClause orderByClause;
			if ( ctx.orderByClause() != null ) {
				if ( getSessionFactory().getSessionFactoryOptions().isStrictJpaQueryLanguageCompliance()
						&& querySpecProcessingStateStack.getCurrent().getContainingQueryState() != null ) {
					throw new StrictJpaComplianceViolation(
							StrictJpaComplianceViolation.Type.SUBQUERY_ORDER_BY
					);
				}

				orderByClause = visitOrderByClause( ctx.orderByClause() );
			}
			else {
				orderByClause = null;
			}

			final SqmLimitOffsetClause limitOffsetClause;
			if ( ctx.limitClause() != null || ctx.offsetClause() != null ) {
				if ( getSessionFactory().getSessionFactoryOptions().isStrictJpaQueryLanguageCompliance() ) {
					throw new StrictJpaComplianceViolation(
							StrictJpaComplianceViolation.Type.LIMIT_OFFSET_CLAUSE
					);
				}

				if ( querySpecProcessingStateStack.getCurrent().getContainingQueryState() != null
						&& orderByClause == null ) {
					throw new SemanticException( "limit and offset clause require an order-by clause" );
				}

				final SqmExpression limitExpression;
				if ( ctx.limitClause() != null ) {
					limitExpression = visitLimitClause( ctx.limitClause() );
				}
				else {
					limitExpression = null;
				}

				final SqmExpression offsetExpression;
				if ( ctx.offsetClause() != null ) {
					offsetExpression = visitOffsetClause( ctx.offsetClause() );
				}
				else {
					offsetExpression = null;
				}

				limitOffsetClause = new SqmLimitOffsetClause( limitExpression, offsetExpression );
			}
			else {
				limitOffsetClause = null;
			}

			return new SqmQuerySpec( querySpecProcessingStateStack.getCurrent().getFromClause(), selectClause, whereClause, orderByClause, limitOffsetClause );
		}
		finally {
			querySpecProcessingStateStack.pop();
		}
	}

	@SuppressWarnings("WeakerAccess")
	protected SqmSelectClause buildInferredSelectClause(SqmFromClause fromClause) {
		// for now, this is slightly different than the legacy behavior where
		// the root and each non-fetched-join was selected.  For now, here, we simply
		// select the root
		final SqmSelectClause selectClause = new SqmSelectClause( false );
		final SqmFrom root = fromClause.getFromElementSpaces().get( 0 ).getRoot();
		selectClause.addSelection( new SqmSelection( root.getNavigableReference() ) );
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
		querySpecProcessingStateStack.getCurrent().getAliasRegistry().registerAlias( selection );
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
					if ( getSessionFactory().getSessionFactoryOptions().isStrictJpaQueryLanguageCompliance() ) {
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
				mapJavaTypeDescriptor = getSessionFactory()
						.getTypeConfiguration()
						.getJavaTypeDescriptorRegistry()
						.getDescriptor( Map.class );
			}
			dynamicInstantiation = SqmDynamicInstantiation.forMapInstantiation( mapJavaTypeDescriptor );
		}
		else if ( ctx.dynamicInstantiationTarget().LIST() != null ) {
			if ( listJavaTypeDescriptor == null ) {
				listJavaTypeDescriptor = getSessionFactory()
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
				final JavaTypeDescriptor jtd = getSessionFactory()
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
		return getSessionFactory()
							.getServiceRegistry()
							.getService( ClassLoaderService.class )
							.classForName( className );
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
	public SqmNavigableReference visitJpaSelectObjectSyntax(HqlParser.JpaSelectObjectSyntaxContext ctx) {
		final String alias = ctx.identifier().getText();
		final SqmNavigableReference binding = querySpecProcessingStateStack.getCurrent().getAliasRegistry().findFromElementByAlias( alias );
		if ( binding == null ) {
			throw new SemanticException( "Unable to resolve alias [" +  alias + "] in selection [" + ctx.getText() + "]" );
		}
		return binding;
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
	public GroupedSqmPredicate visitGroupedPredicate(HqlParser.GroupedPredicateContext ctx) {
		return new GroupedSqmPredicate( (SqmPredicate) ctx.predicate().accept( this ) );
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
		final SqmSortOrder sortOrder;
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
		SqmExpression sqmExpression = (SqmExpression) ctx.parameterOrNumberLiteral().accept( this );
		addImpliedTypeIdNecessary( sqmExpression, StandardSpiBasicTypes.INTEGER);
		return sqmExpression;
	}

	@Override
	public SqmExpression visitOffsetClause(HqlParser.OffsetClauseContext ctx) {
		SqmExpression sqmExpression = (SqmExpression) ctx.parameterOrNumberLiteral().accept( this );
		addImpliedTypeIdNecessary( sqmExpression, StandardSpiBasicTypes.INTEGER );
		return sqmExpression;
	}

	private void addImpliedTypeIdNecessary(SqmExpression sqmExpression, ExpressableType impliedType) {
		if ( ImpliedTypeSqmExpression.class.isInstance( sqmExpression ) && sqmExpression.getInferableType() == null ) {
			( (ImpliedTypeSqmExpression) sqmExpression ).impliedType( impliedType );
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

	private SqmSortOrder interpretSortOrder(String value) {
		if ( value == null ) {
			return null;
		}

		if ( value.equalsIgnoreCase( "ascending" ) || value.equalsIgnoreCase( "asc" ) ) {
			return SqmSortOrder.ASCENDING;
		}

		if ( value.equalsIgnoreCase( "descending" ) || value.equalsIgnoreCase( "desc" ) ) {
			return SqmSortOrder.DESCENDING;
		}

		throw new SemanticException( "Unknown sort order : " + value );
	}

	@Override
	public SqmDeleteStatement visitDeleteStatement(HqlParser.DeleteStatementContext ctx) {
		querySpecProcessingStateStack.push( new QuerySpecProcessingStateDmlImpl( this ) );
		try {
			final SqmRoot root = resolveDmlRootEntityReference( ctx.mainEntityPersisterReference() );
			final SqmDeleteStatementImpl deleteStatement = new SqmDeleteStatementImpl( root );

			parameterCollector = deleteStatement;

			try {
				if ( ctx.whereClause() != null && ctx.whereClause().predicate() != null ) {
					deleteStatement.getWhereClause().setPredicate(
							(SqmPredicate) ctx.whereClause().predicate().accept( this )
					);
				}
			}
			finally {
				deleteStatement.wrapUp();
			}

			return deleteStatement;
		}
		finally {
			querySpecProcessingStateStack.pop();
		}
	}

	@SuppressWarnings("WeakerAccess")
	protected SqmRoot resolveDmlRootEntityReference(HqlParser.MainEntityPersisterReferenceContext rootEntityContext) {
		final String entityName = rootEntityContext.dotIdentifierSequence().getText();
		final EntityValuedExpressableType entityReference = resolveEntityReference( entityName );
		if ( entityReference == null ) {
			throw new UnknownEntityException( "Could not resolve entity name [" + entityName + "] as DML target", entityName );
		}

		String alias = interpretIdentificationVariable( rootEntityContext.identificationVariableDef() );
		if ( alias == null ) {
			alias = getImplicitAliasGenerator().generateUniqueImplicitAlias();
			log.debugf(
					"Generated implicit alias [%s] for DML root entity reference [%s]",
					alias,
					entityReference.getEntityName()
			);
		}
		final SqmRoot root = new SqmRoot( null, generateUniqueIdentifier(), alias, entityReference, this );
		querySpecProcessingStateStack.getCurrent().getAliasRegistry().registerAlias( root );
		querySpecProcessingStateStack.getCurrent().getFromClause().getFromElementSpaces().get( 0 ).setRoot( root );
		return root;
	}

	private String interpretIdentificationVariable(HqlParser.IdentificationVariableDefContext identificationVariableDef) {
		if ( identificationVariableDef != null ) {
			final String explicitAlias;
			if ( identificationVariableDef.AS() != null ) {
				final Token identificationVariableToken = identificationVariableDef.identificationVariable().identifier().getStart();
				if ( identificationVariableToken.getType() != HqlParser.IDENTIFIER ) {
					// we have a reserved word used as an identification variable.
					if ( getSessionFactory().getSessionFactoryOptions().isStrictJpaQueryLanguageCompliance() ) {
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
				explicitAlias = identificationVariableToken.getText();
			}
			else {
				explicitAlias = identificationVariableDef.IDENTIFIER().getText();
			}
			return explicitAlias;
		}

		return getImplicitAliasGenerator().generateUniqueImplicitAlias();
	}

	@Override
	public SqmUpdateStatement visitUpdateStatement(HqlParser.UpdateStatementContext ctx) {
		querySpecProcessingStateStack.push( new QuerySpecProcessingStateDmlImpl( this ) );
		try {
			final SqmRoot root = resolveDmlRootEntityReference( ctx.mainEntityPersisterReference() );
			final SqmUpdateStatementImpl updateStatement = new SqmUpdateStatementImpl( root );

			parameterCollector = updateStatement;
			try {
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
			}
			finally {
				updateStatement.wrapUp();
			}

			return updateStatement;
		}
		finally {
			querySpecProcessingStateStack.pop();
		}
	}

	@Override
	public SqmInsertSelectStatement visitInsertStatement(HqlParser.InsertStatementContext ctx) {
		querySpecProcessingStateStack.push( new QuerySpecProcessingStateDmlImpl( this ) );

		try {
			final String entityName = ctx.insertSpec().intoSpec().dotIdentifierSequence().getText();
			final EntityValuedExpressableType entityReference = resolveEntityReference( entityName );
			if ( entityReference == null ) {
				throw new UnknownEntityException( "Could not resolve entity name [" + entityName + "] as INSERT target", entityName );
			}

			String alias = getImplicitAliasGenerator().generateUniqueImplicitAlias();
			log.debugf(
					"Generated implicit alias [%s] for INSERT target [%s]",
					alias,
					entityReference.getEntityName()
			);

			SqmRoot root = new SqmRoot( null, generateUniqueIdentifier(), alias, entityReference, this );
			querySpecProcessingStateStack.getCurrent().getAliasRegistry().registerAlias( root );
			querySpecProcessingStateStack.getCurrent().getFromClause().getFromElementSpaces().get( 0 ).setRoot( root );

			// for now we only support the INSERT-SELECT form
			final SqmInsertSelectStatementImpl insertStatement = new SqmInsertSelectStatementImpl( root );
			parameterCollector = insertStatement;

			try {
				insertStatement.setSelectQuery( visitQuerySpec( ctx.querySpec() ) );

				for ( HqlParser.DotIdentifierSequenceContext stateFieldCtx : ctx.insertSpec().targetFieldsSpec().dotIdentifierSequence() ) {
					final SqmSingularAttributeReference stateField = (SqmSingularAttributeReference) visitDotIdentifierSequence( stateFieldCtx );
					// todo : validate each resolved stateField...
					insertStatement.addInsertTargetStateField( stateField );
				}
			}
			finally {
				insertStatement.wrapUp();
			}

			return insertStatement;
		}
		finally {
			querySpecProcessingStateStack.pop();
		}
	}

	@Override
	public Object visitFromElementSpace(HqlParser.FromElementSpaceContext ctx) {
		currentFromElementSpace = querySpecProcessingStateStack.getCurrent().getFromClause().makeFromElementSpace();

		visitFromElementSpaceRoot( ctx.fromElementSpaceRoot() );

		for ( HqlParser.CrossJoinContext crossJoinContext : ctx.crossJoin() ) {
			visitCrossJoin( crossJoinContext );
		}

		for ( HqlParser.QualifiedJoinContext qualifiedJoinContext : ctx.qualifiedJoin() ) {
			visitQualifiedJoin( qualifiedJoinContext );
		}

		for ( HqlParser.JpaCollectionJoinContext jpaCollectionJoinContext : ctx.jpaCollectionJoin() ) {
			visitJpaCollectionJoin( jpaCollectionJoinContext );
		}


		SqmFromElementSpace rtn = currentFromElementSpace;
		currentFromElementSpace = null;
		return rtn;
	}

	@Override
	public SqmRoot visitFromElementSpaceRoot(HqlParser.FromElementSpaceRootContext ctx) {
		final String entityName = ctx.mainEntityPersisterReference().dotIdentifierSequence().getText();

		// todo (6.0) : perf - pushing to the stack for root and cross join is overkill, see if this has perf impact
		//		just did it this way for consistency

		fromBuilderStack.push(
				new SqmFromBuilderFromClauseStandard(
						interpretIdentificationVariable( ctx.mainEntityPersisterReference().identificationVariableDef() ),
						this
				)
		);

		try {
			final EntityValuedExpressableType entityReference = resolveEntityReference( entityName );
			if ( entityReference == null ) {
				throw new UnknownEntityException(
						"Could not resolve entity name [" + entityName + "] used as root",
						entityName
				);
			}

			if ( PolymorphicEntityValuedExpressableType.class.isInstance( entityReference ) ) {
				if ( getSessionFactory().getSessionFactoryOptions().isStrictJpaQueryLanguageCompliance() ) {
					throw new StrictJpaComplianceViolation(
							"Encountered unmapped polymorphic reference [" + entityReference.getEntityName()
									+ "], but strict JPQL compliance was requested",
							StrictJpaComplianceViolation.Type.UNMAPPED_POLYMORPHISM
					);
				}

				// todo : disallow in subqueries as well
			}

			return getCurrentFromElementBuilder().buildRoot( entityReference.getEntityDescriptor() );
		}
		finally {
			fromBuilderStack.pop();
		}
	}

	private EntityValuedExpressableType resolveEntityReference(String entityName) {
		log.debugf( "Attempting to resolve path [%s] as entity reference...", entityName );
		EntityValuedExpressableType reference = null;
		try {
			reference = getSessionFactory()
					.getMetamodel()
					.resolveEntityReference( entityName );
		}
		catch (Exception ignore) {
		}

		return reference;
	}

	@Override
	public SqmCrossJoin visitCrossJoin(HqlParser.CrossJoinContext ctx) {
		fromBuilderStack.push(
				new SqmFromBuilderFromClauseStandard(
						interpretIdentificationVariable( ctx.mainEntityPersisterReference().identificationVariableDef() ),
						this
				)
		);

		try {

			final String entityName = ctx.mainEntityPersisterReference().dotIdentifierSequence().getText();
			final EntityValuedExpressableType entityReference = resolveEntityReference( entityName );
			if ( entityReference == null ) {
				throw new UnknownEntityException(
						"Could not resolve entity name [" + entityName + "] used as CROSS JOIN target",
						entityName
				);
			}

			if ( PolymorphicEntityValuedExpressableType.class.isInstance( entityReference ) ) {
				throw new SemanticException(
						"Unmapped polymorphic references are only valid as sqm root, not in cross join : " +
								entityReference.getEntityName()
				);
			}

			return fromBuilderStack.getCurrent().buildCrossJoin( entityReference.getEntityDescriptor() );
		}
		finally {
			fromBuilderStack.pop();
		}
	}

	@Override
	public SqmQualifiedJoin visitJpaCollectionJoin(HqlParser.JpaCollectionJoinContext ctx) {
		SqmPluralAttributeReference attributeBinding = asPluralAttribute( (SqmNavigableReference) ctx.path().accept( this ) );
		return attributeBinding.getExportedFromElement();
	}

	@Override
	public SqmQualifiedJoin visitQualifiedJoin(HqlParser.QualifiedJoinContext ctx) {
		final SqmJoinType joinType;
		final HqlParser.JoinTypeQualifierContext joinTypeQualifier = ctx.joinTypeQualifier();
		if ( joinTypeQualifier.OUTER() != null ) {
			// for outer joins, only left outer joins are currently supported
			if ( joinTypeQualifier.FULL() != null ) {
				throw new SemanticException( "FULL OUTER joins are not yet supported : " + ctx.getText() );
			}
			if ( joinTypeQualifier.RIGHT() != null ) {
				throw new SemanticException( "RIGHT OUTER joins are not yet supported : " + ctx.getText() );
			}

			joinType = SqmJoinType.LEFT;
		}
		else {
			joinType = SqmJoinType.INNER;
		}

		final String identificationVariable = interpretIdentificationVariable(
				ctx.qualifiedJoinRhs().identificationVariableDef()
		);

		final boolean fetched = ctx.FETCH() != null;

		fromBuilderStack.push(
				new SqmFromBuilderFromClauseQualifiedJoin( joinType, fetched, identificationVariable, this )
		);

		try {
			// Object because join-target might be either an Entity join (... join Address a on ...)
			// or an attribute-join (... from Person p join p.address a on ...)
			final Object joinRhsResolution = ctx.qualifiedJoinRhs().path().accept( this );

			// either way, we need to resolve it as a SqmNavigableReference
			final SqmNavigableReference navigableReference;

			if ( joinRhsResolution instanceof SqmEmbeddableTypedReference ) {
				final SqmEmbeddableTypedReference compositeReference = (SqmEmbeddableTypedReference) joinRhsResolution;
				navigableReference = compositeReference;
				if ( compositeReference.getExportedFromElement() == null ) {
					// todo : can we create a SqmFrom element specific to a CompositeBinding?
					throw new NotYetImplementedFor6Exception(
							"Support for SqmFrom generation specific to CompositeBinding not yet implemented" );
				}
			}
			else if ( joinRhsResolution instanceof  SqmNavigableReference ) {
				navigableReference = (SqmNavigableReference) joinRhsResolution;
			}
			else if ( joinRhsResolution instanceof SemanticPathPartNamedEntity ) {
				final SemanticPathPartNamedEntity namedEntity = (SemanticPathPartNamedEntity) joinRhsResolution;
				final SqmEntityJoin join = fromBuilderStack.getCurrent().buildEntityJoin( namedEntity.getEntityDescriptor() );
				navigableReference = join.getNavigableReference();
			}
			else {
				throw new ParsingException( "Unexpected qualified-join rhs resolved type : " + joinRhsResolution.getClass().getName() );
			}

			final SqmQualifiedJoin joinedFromElement = (SqmQualifiedJoin) navigableReference.getExportedFromElement();
			currentJoinRhs = joinedFromElement;

			if ( getSessionFactory().getSessionFactoryOptions().isStrictJpaQueryLanguageCompliance() ) {
				if ( !ImplicitAliasGenerator.isImplicitAlias( joinedFromElement.getIdentificationVariable() ) ) {
					if ( SqmSingularAttributeReference.class.isInstance( joinedFromElement.getNavigableReference() )
							&& SqmFromExporter.class.isInstance( joinedFromElement.getNavigableReference() ) ) {
						final SqmNavigableJoin attributeJoin = (SqmNavigableJoin) joinedFromElement;
						if ( attributeJoin.isFetched() ) {
							throw new StrictJpaComplianceViolation(
									"Encountered aliased fetch join, but strict JPQL compliance was requested",
									StrictJpaComplianceViolation.Type.ALIASED_FETCH_JOIN
							);
						}
					}
				}
			}

			if ( ctx.qualifiedJoinPredicate() != null ) {
				fromBuilderStack.push( standardSqmFromBuilder );
				try {
					joinedFromElement.setOnClausePredicate( visitQualifiedJoinPredicate( ctx.qualifiedJoinPredicate() ) );
				}
				finally {
					fromBuilderStack.pop();
				}
			}

			return joinedFromElement;
		}
		finally {
			fromBuilderStack.pop();
			currentJoinRhs = null;
		}
	}

	private SqmQualifiedJoin currentJoinRhs;

	@Override
	public SqmPredicate visitQualifiedJoinPredicate(HqlParser.QualifiedJoinPredicateContext ctx) {
		if ( currentJoinRhs == null ) {
			throw new ParsingException( "Expecting join RHS to be set" );
		}

		semanticPathPartStack.push( new SemanticPathPartJoinPredicate( currentFromElementSpace ) );

		try {
			return (SqmPredicate) ctx.predicate().accept( this );
		}
		finally {
			semanticPathPartStack.pop();
		}
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
	public RelationalSqmPredicate visitEqualityPredicate(HqlParser.EqualityPredicateContext ctx) {
		final SqmExpression lhs = (SqmExpression) ctx.expression().get( 0 ).accept( this );
		final SqmExpression rhs = (SqmExpression) ctx.expression().get( 1 ).accept( this );

		if ( lhs.getInferableType() != null ) {
			if ( rhs instanceof ImpliedTypeSqmExpression ) {
				( (ImpliedTypeSqmExpression) rhs ).impliedType( lhs.getInferableType() );
			}
		}

		if ( rhs.getInferableType() != null ) {
			if ( lhs instanceof ImpliedTypeSqmExpression ) {
				( (ImpliedTypeSqmExpression) lhs ).impliedType( rhs.getInferableType() );
			}
		}

		return new RelationalSqmPredicate( RelationalPredicateOperator.EQUAL, lhs, rhs );
	}

	@Override
	public Object visitInequalityPredicate(HqlParser.InequalityPredicateContext ctx) {
		final SqmExpression lhs = (SqmExpression) ctx.expression().get( 0 ).accept( this );
		final SqmExpression rhs = (SqmExpression) ctx.expression().get( 1 ).accept( this );

		if ( lhs.getInferableType() != null ) {
			if ( rhs instanceof ImpliedTypeSqmExpression ) {
				( (ImpliedTypeSqmExpression) rhs ).impliedType( lhs.getInferableType() );
			}
		}

		if ( rhs.getInferableType() != null ) {
			if ( lhs instanceof ImpliedTypeSqmExpression ) {
				( (ImpliedTypeSqmExpression) lhs ).impliedType( rhs.getInferableType() );
			}
		}

		return new RelationalSqmPredicate( RelationalPredicateOperator.NOT_EQUAL, lhs, rhs );
	}

	@Override
	public Object visitGreaterThanPredicate(HqlParser.GreaterThanPredicateContext ctx) {
		final SqmExpression lhs = (SqmExpression) ctx.expression().get( 0 ).accept( this );
		final SqmExpression rhs = (SqmExpression) ctx.expression().get( 1 ).accept( this );

		if ( lhs.getInferableType() != null ) {
			if ( rhs instanceof ImpliedTypeSqmExpression ) {
				( (ImpliedTypeSqmExpression) rhs ).impliedType( lhs.getInferableType() );
			}
		}

		if ( rhs.getInferableType() != null ) {
			if ( lhs instanceof ImpliedTypeSqmExpression ) {
				( (ImpliedTypeSqmExpression) lhs ).impliedType( rhs.getInferableType() );
			}
		}

		return new RelationalSqmPredicate( RelationalPredicateOperator.GREATER_THAN, lhs, rhs );
	}

	@Override
	public Object visitGreaterThanOrEqualPredicate(HqlParser.GreaterThanOrEqualPredicateContext ctx) {
		final SqmExpression lhs = (SqmExpression) ctx.expression().get( 0 ).accept( this );
		final SqmExpression rhs = (SqmExpression) ctx.expression().get( 1 ).accept( this );

		if ( lhs.getInferableType() != null ) {
			if ( rhs instanceof ImpliedTypeSqmExpression ) {
				( (ImpliedTypeSqmExpression) rhs ).impliedType( lhs.getInferableType() );
			}
		}

		if ( rhs.getInferableType() != null ) {
			if ( lhs instanceof ImpliedTypeSqmExpression ) {
				( (ImpliedTypeSqmExpression) lhs ).impliedType( rhs.getInferableType() );
			}
		}

		return new RelationalSqmPredicate( RelationalPredicateOperator.GREATER_THAN_OR_EQUAL, lhs, rhs );
	}

	@Override
	public Object visitLessThanPredicate(HqlParser.LessThanPredicateContext ctx) {
		final SqmExpression lhs = (SqmExpression) ctx.expression().get( 0 ).accept( this );
		final SqmExpression rhs = (SqmExpression) ctx.expression().get( 1 ).accept( this );

		if ( lhs.getInferableType() != null ) {
			if ( rhs instanceof ImpliedTypeSqmExpression ) {
				( (ImpliedTypeSqmExpression) rhs ).impliedType( lhs.getInferableType() );
			}
		}

		if ( rhs.getInferableType() != null ) {
			if ( lhs instanceof ImpliedTypeSqmExpression ) {
				( (ImpliedTypeSqmExpression) lhs ).impliedType( rhs.getInferableType() );
			}
		}

		return new RelationalSqmPredicate( RelationalPredicateOperator.LESS_THAN, lhs, rhs );
	}

	@Override
	public Object visitLessThanOrEqualPredicate(HqlParser.LessThanOrEqualPredicateContext ctx) {
		final SqmExpression lhs = (SqmExpression) ctx.expression().get( 0 ).accept( this );
		final SqmExpression rhs = (SqmExpression) ctx.expression().get( 1 ).accept( this );

		if ( lhs.getInferableType() != null ) {
			if ( rhs instanceof ImpliedTypeSqmExpression ) {
				( (ImpliedTypeSqmExpression) rhs ).impliedType( lhs.getInferableType() );
			}
		}

		if ( rhs.getInferableType() != null ) {
			if ( lhs instanceof ImpliedTypeSqmExpression ) {
				( (ImpliedTypeSqmExpression) lhs ).impliedType( rhs.getInferableType() );
			}
		}

		return new RelationalSqmPredicate( RelationalPredicateOperator.LESS_THAN_OR_EQUAL, lhs, rhs );
	}

	@Override
	public Object visitBetweenPredicate(HqlParser.BetweenPredicateContext ctx) {
		final SqmExpression expression = (SqmExpression) ctx.expression().get( 0 ).accept( this );
		final SqmExpression lowerBound = (SqmExpression) ctx.expression().get( 1 ).accept( this );
		final SqmExpression upperBound = (SqmExpression) ctx.expression().get( 2 ).accept( this );

		if ( expression.getInferableType() != null ) {
			if ( lowerBound instanceof ImpliedTypeSqmExpression ) {
				( (ImpliedTypeSqmExpression) lowerBound ).impliedType( expression.getInferableType() );
			}
			if ( upperBound instanceof ImpliedTypeSqmExpression ) {
				( (ImpliedTypeSqmExpression) upperBound ).impliedType( expression.getInferableType() );
			}
		}
		else if ( lowerBound.getInferableType() != null ) {
			if ( expression instanceof ImpliedTypeSqmExpression ) {
				( (ImpliedTypeSqmExpression) expression ).impliedType( lowerBound.getInferableType() );
			}
			if ( upperBound instanceof ImpliedTypeSqmExpression ) {
				( (ImpliedTypeSqmExpression) upperBound ).impliedType( lowerBound.getInferableType() );
			}
		}
		else if ( upperBound.getInferableType() != null ) {
			if ( expression instanceof ImpliedTypeSqmExpression ) {
				( (ImpliedTypeSqmExpression) expression ).impliedType( upperBound.getInferableType() );
			}
			if ( lowerBound instanceof ImpliedTypeSqmExpression ) {
				( (ImpliedTypeSqmExpression) lowerBound ).impliedType( upperBound.getInferableType() );
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
						if ( listItemExpression instanceof ImpliedTypeSqmExpression ) {
							( (ImpliedTypeSqmExpression) listItemExpression ).impliedType( testExpression.getInferableType() );
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
			validateBindingAsEntityTypeExpression( binding );
			return new SqmEntityTypeExpression( binding );
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
	public SqmMapEntryBinding visitMapEntrySelection(HqlParser.MapEntrySelectionContext ctx) {
		final SqmPluralAttributeReference pathResolution = asMap( (SqmNavigableReference) ctx.path().accept( this ) );

		return new SqmMapEntryBinding(
				pathResolution,
				(BasicJavaDescriptor) getSessionFactory().getTypeConfiguration()
						.getJavaTypeDescriptorRegistry()
						.getDescriptor( Map.Entry.class )
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Path structures


	@Override
	public Object visitPath(HqlParser.PathContext ctx) {
		if ( ctx.syntacticNavigablePath() != null ) {
			return ctx.syntacticNavigablePath().accept( this );
		}
		else if ( ctx.nonSyntacticNavigablePath() != null ) {
			return ctx.nonSyntacticNavigablePath().accept( this );
		}

		throw new ParsingException( "Unrecognized `path` rule branch" );
	}

	@Override
	public Object visitSyntacticNavigablePath(HqlParser.SyntacticNavigablePathContext ctx) {
		if ( ctx.treatedNavigablePath() != null ) {
			return ctx.treatedNavigablePath().accept( this );
		}
		else if ( ctx.collectionElementNavigablePath() != null ) {
			return ctx.collectionElementNavigablePath().accept( this );
		}
		else if ( ctx.mapKeyNavigablePath() != null ) {
			return ctx.mapKeyNavigablePath().accept( this );
		}

		throw new ParsingException( "Unrecognized `syntacticNavigablePath` rule branch" );
	}

	@Override
	public Object visitTreatedNavigablePath(HqlParser.TreatedNavigablePathContext ctx) {
		// todo (6.0) : we just do not have enough info here to do this...
		//		much of this depends on where the TREAT occurs:
		//			* is the downcast intrinsic or incidental?
		//			* DowncastLocation

		final SqmNavigableReference basePathPart = (SqmNavigableReference) ctx.path().accept( this );

		final String castTargetName = ctx.dotIdentifierSequence().getText();
		final EntityTypeDescriptor castTarget = resolveEntityReference( castTargetName ).getEntityDescriptor();

		treatHandlerStack.getCurrent().addDowncast( basePathPart.getExportedFromElement(), castTarget );;

		if ( ctx.nonSyntacticNavigablePath() != null ) {
			// we have a path continuation
			semanticPathPartStack.push( basePathPart );
			try {
				return ctx.nonSyntacticNavigablePath().accept( this );
			}
			finally {
				semanticPathPartStack.pop();
			}
		}

		// NOTE: the "continuation" block and the following alias block are mutually
		// exclusive.  In reality the "continuation" block would return from this
		// method though, so no need to have the `else`

		final String alias = ctx.identifier() == null ? null : ctx.identifier().getText();

		// todo (6.0) - we'd need to create the "treated navigable reference" and register it under the uid and alias (or generated alias if null)
		return basePathPart;
	}

	@Override
	public Object visitCollectionElementNavigablePath(HqlParser.CollectionElementNavigablePathContext ctx) {
		final SqmPluralAttributeReference collectionReference = (SqmPluralAttributeReference) ctx.path().accept( this );

		if ( getSessionFactory().getSessionFactoryOptions().isStrictJpaQueryLanguageCompliance() ) {
			if ( collectionReference.getReferencedNavigable().getPersistentCollectionDescriptor().getCollectionClassification() != CollectionClassification.MAP ) {
				throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.VALUE_FUNCTION_ON_NON_MAP );
			}
		}

		final SqmNavigableReference elementReference = collectionReference.getReferencedNavigable()
				.getPersistentCollectionDescriptor()
				.getElementDescriptor()
				.createSqmExpression( collectionReference.getExportedFromElement(), collectionReference, this );

		// process the path continuation, if one
		if ( ctx.nonSyntacticNavigablePath() != null ) {
			semanticPathPartStack.push( elementReference );

			try {
				return ctx.nonSyntacticNavigablePath().accept( this );
			}
			finally {
				semanticPathPartStack.pop();
			}
		}

		return elementReference;
	}

	@Override
	public Object visitMapKeyNavigablePath(HqlParser.MapKeyNavigablePathContext ctx) {
		final SqmNavigableReference navigableReference = (SqmNavigableReference) ctx.path().accept( this );

		final SqmPluralAttributeReference mapReference = asMap( navigableReference );

		final SqmNavigableReference mapKeyReference = mapReference.getReferencedNavigable()
				.getPersistentCollectionDescriptor()
				.getIndexDescriptor()
				.createSqmExpression( navigableReference.getExportedFromElement(), mapReference, this );

		// process the path continuation, if one
		if ( ctx.nonSyntacticNavigablePath() != null ) {
			semanticPathPartStack.push( mapKeyReference );
			try {
				return ctx.nonSyntacticNavigablePath().accept( this );
			}
			finally {
				semanticPathPartStack.pop();
			}
		}

		return mapKeyReference;
	}

	@Override
	public Object visitNonSyntacticNavigablePath(HqlParser.NonSyntacticNavigablePathContext ctx) {
		final SemanticPathPart basePathPart = (SemanticPathPart) ctx.dotIdentifierSequence().accept( this );

		if ( ctx.semanticNavigablePathFragment() != null ) {
			// this indicates that the path ends with a fragment that is unequivocally a navigable reference
			// as it starts with a `[selectorExpression]` structure

			semanticPathPartStack.push( basePathPart );

			try {
				// todo (6.0) : set up `basePathPart` as the base for SemanticPathPart resolution of the continuation
				return ctx.semanticNavigablePathFragment().accept( this );
			}
			finally {
				semanticPathPartStack.pop();
			}
		}

		return basePathPart;
	}


	@Override
	public Object visitDotIdentifierSequence(HqlParser.DotIdentifierSequenceContext ctx) {
		log.tracef( "Starting #visitDotIdentifierSequence : " + ctx.getText() );

		final boolean isBaseTerminal = ctx.dotIdentifierSequence() == null || ctx.dotIdentifierSequence().isEmpty();

		final SemanticPathPart base = semanticPathPartStack.getCurrent().resolvePathPart(
				ctx.identifier().getText(),
				ctx.getText(),
				isBaseTerminal,
				this
		);

		if ( !isBaseTerminal ) {
			semanticPathPartStack.push( base );
			try {
				return ctx.dotIdentifierSequence().accept( this );
			}
			finally {
				semanticPathPartStack.pop();
			}
		}

		return base;
	}


	@Override
	public Object visitSemanticNavigablePathFragment(HqlParser.SemanticNavigablePathFragmentContext ctx) {
		final boolean isBaseTerminal = ctx.nonSyntacticNavigablePath() != null;

		final SemanticPathPart base = semanticPathPartStack.getCurrent().resolveIndexedAccess(
				(SqmExpression) ctx.expression().accept( this ),
				ctx.getText(),
				isBaseTerminal,
				this
		);

		if ( isBaseTerminal ) {
			semanticPathPartStack.push( base );
			try {
				return ctx.nonSyntacticNavigablePath().accept( this );
			}
			finally {
				semanticPathPartStack.pop();
			}
		}

		return base;
	}

	private SqmPluralAttributeReference asMap(SqmNavigableReference binding) {
		SqmPluralAttributeReference attributeBinding = asPluralAttribute( binding );
		if ( attributeBinding.getReferencedNavigable().getPersistentCollectionDescriptor().getCollectionClassification() != CollectionClassification.MAP ) {
			throw new SemanticException( "Expecting persistent Map reference, but found : " + binding );
		}

		return attributeBinding;
	}

	@SuppressWarnings("RedundantIfStatement")
	private SqmPluralAttributeReference asPluralAttribute(SqmNavigableReference attributeBinding) {
		if ( !SqmPluralAttributeReference.class.isInstance( attributeBinding ) ) {
			throw new SemanticException( "Expecting plural-attribute, but found : " + attributeBinding );
		}

		return (SqmPluralAttributeReference) attributeBinding;
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
				SqmBinaryArithmetic.Operation.ADD,
				firstOperand,
				secondOperand,
				getSessionFactory().getTypeConfiguration().resolveArithmeticType(
						(BasicValuedExpressableType) firstOperand.getExpressableType(),
						(BasicValuedExpressableType) secondOperand.getExpressableType(),
						SqmBinaryArithmetic.Operation.ADD
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
				SqmBinaryArithmetic.Operation.SUBTRACT,
				firstOperand,
				secondOperand,
				getSessionFactory().getTypeConfiguration().resolveArithmeticType(
						(BasicValuedExpressableType) firstOperand.getExpressableType(),
						(BasicValuedExpressableType) secondOperand.getExpressableType(),
						SqmBinaryArithmetic.Operation.SUBTRACT
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
				SqmBinaryArithmetic.Operation.MULTIPLY,
				firstOperand,
				secondOperand,
				getSessionFactory().getTypeConfiguration().resolveArithmeticType(
						(BasicValuedExpressableType) firstOperand.getExpressableType(),
						(BasicValuedExpressableType) secondOperand.getExpressableType(),
						SqmBinaryArithmetic.Operation.MULTIPLY
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
				SqmBinaryArithmetic.Operation.DIVIDE,
				firstOperand,
				secondOperand,
				getSessionFactory().getTypeConfiguration().resolveArithmeticType(
						(BasicValuedExpressableType) firstOperand.getExpressableType(),
						(BasicValuedExpressableType) secondOperand.getExpressableType(),
						SqmBinaryArithmetic.Operation.DIVIDE
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
				SqmBinaryArithmetic.Operation.MODULO,
				firstOperand,
				secondOperand,
				getSessionFactory().getTypeConfiguration().resolveArithmeticType(
						(BasicValuedExpressableType) firstOperand.getExpressableType(),
						(BasicValuedExpressableType) secondOperand.getExpressableType(),
						SqmBinaryArithmetic.Operation.MODULO
				)
		);
	}

	@Override
	public Object visitUnaryPlusExpression(HqlParser.UnaryPlusExpressionContext ctx) {
		return new SqmUnaryOperation(
				SqmUnaryOperation.Operation.PLUS,
				(SqmExpression) ctx.expression().accept( this )
		);
	}

	@Override
	public Object visitUnaryMinusExpression(HqlParser.UnaryMinusExpressionContext ctx) {
		return new SqmUnaryOperation(
				SqmUnaryOperation.Operation.MINUS,
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
	@SuppressWarnings("UnnecessaryBoxing")
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
			return SqmLiteralTimestamp.from( ctx.literal().timestampLiteral().dateTimeLiteralText().getText(), this );
		}
		else if ( ctx.literal().dateLiteral() != null ) {
			return SqmLiteralDate.from( ctx.literal().dateLiteral().dateTimeLiteralText().getText(), this );
		}
		else if ( ctx.literal().timeLiteral() != null ) {
			return SqmLiteralTime.from( ctx.literal().timeLiteral().dateTimeLiteralText().getText(), this );
		}

		// otherwise we have a problem
		throw new ParsingException( "Unexpected literal expression type [" + ctx.getText() + "]" );
	}

	private SqmLiteral<Boolean> booleanLiteral(boolean value) {
		final BasicValuedExpressableType expressionType = resolveExpressableTypeBasic( Boolean.class );
		return value
				? new SqmLiteralTrue( expressionType )
				: new SqmLiteralFalse( expressionType );
	}

	private SqmLiteralCharacter characterLiteral(String text) {
		if ( text.length() > 1 ) {
			// todo : or just treat it as a String literal?
			throw new ParsingException( "Value for CHARACTER_LITERAL token was more than 1 character" );
		}
		return new SqmLiteralCharacter(
				text.charAt( 0 ),
				resolveExpressableTypeBasic( Character.class )
		);
	}

	private SqmLiteral stringLiteral(String text) {
		return new SqmLiteralString(
				text,
				resolveExpressableTypeBasic( String.class )
		);
	}

	@SuppressWarnings("WeakerAccess")
	protected SqmLiteralInteger integerLiteral(String text) {
		try {
			final Integer value = Integer.valueOf( text );
			return new SqmLiteralInteger(
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
	protected SqmLiteralLong longLiteral(String text) {
		final String originalText = text;
		try {
			if ( text.endsWith( "l" ) || text.endsWith( "L" ) ) {
				text = text.substring( 0, text.length() - 1 );
			}
			final Long value = Long.valueOf( text );
			return new SqmLiteralLong(
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
	protected SqmLiteralBigInteger bigIntegerLiteral(String text) {
		final String originalText = text;
		try {
			if ( text.endsWith( "bi" ) || text.endsWith( "BI" ) ) {
				text = text.substring( 0, text.length() - 2 );
			}
			return new SqmLiteralBigInteger(
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
	protected SqmLiteralFloat floatLiteral(String text) {
		try {
			return new SqmLiteralFloat(
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
	protected SqmLiteralDouble doubleLiteral(String text) {
		try {
			return new SqmLiteralDouble(
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
	protected SqmLiteralBigDecimal bigDecimalLiteral(String text) {
		final String originalText = text;
		try {
			if ( text.endsWith( "bd" ) || text.endsWith( "BD" ) ) {
				text = text.substring( 0, text.length() - 2 );
			}
			return new SqmLiteralBigDecimal(
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

	private <J> BasicValuedExpressableType<J> resolveExpressableTypeBasic(Class<J> javaType) {
		return getSessionFactory().getTypeConfiguration().getBasicTypeRegistry().getBasicType( javaType );
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
		final String functionName = ctx.nonStandardFunctionName().getText();
		final List<SqmExpression> functionArguments = visitNonStandardFunctionArguments( ctx.nonStandardFunctionArguments() );

		// todo : integrate some form of SqlFunction look-up using the ParsingContext so we can resolve the "type"
		return new SqmGenericFunction( functionName, null, functionArguments );
	}

	@Override
	public SqmGenericFunction visitNonStandardFunction(HqlParser.NonStandardFunctionContext ctx) {
		if ( getSessionFactory().getSessionFactoryOptions().isStrictJpaQueryLanguageCompliance() ) {
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
		parameterDeclarationContextStack.push( () -> getSessionFactory().getSessionFactoryOptions().isStrictJpaQueryLanguageCompliance() );
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
		final SqmFunctionTemplate template = getSessionFactory()
				.getQueryEngine()
				.getSqmFunctionRegistry()
				.findFunctionTemplate( SqmCastFunction.NAME );

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

	private SqmExpression generateAggregateFunction(
			BiFunction<SqmFunctionTemplate, SqmExpression, SqmExpression> templatedGenerator,
			Function<SqmExpression, SqmExpression> nonTemplatedGenerator,
			String name,
			boolean isDistinct,
			HqlParser.ExpressionContext antlrArgumentExpression) {
		final SqmFunctionTemplate template = getSessionFactory()
				.getQueryEngine()
				.getSqmFunctionRegistry()
				.findFunctionTemplate( name );

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
		final SqmFunctionTemplate template = getSessionFactory()
				.getQueryEngine()
				.getSqmFunctionRegistry()
				.findFunctionTemplate( name );

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

		final SqmFunctionTemplate trimFunctionTemplate = getSessionFactory()
				.getQueryEngine()
				.getSqmFunctionRegistry()
				.findFunctionTemplate( "trim" );

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
	public SqmLiteralCharacter visitTrimCharacter(HqlParser.TrimCharacterContext ctx) {
		if ( ctx.CHARACTER_LITERAL() != null ) {
			final String trimCharText = ctx.CHARACTER_LITERAL().getText();
			if ( trimCharText.length() != 1 ) {
				throw new SemanticException( "Expecting [trim character] for TRIM function to be  single character, found : " + trimCharText );
			}
			return new SqmLiteralCharacter(
					trimCharText.charAt( 0 ),
					resolveExpressableTypeBasic( Character.class )
			);
		}
		if ( ctx.STRING_LITERAL() != null ) {
			final String trimCharText = ctx.STRING_LITERAL().getText();
			if ( trimCharText.length() != 1 ) {
				throw new SemanticException( "Expecting [trim character] for TRIM function to be  single character, found : " + trimCharText );
			}
			return new SqmLiteralCharacter(
					trimCharText.charAt( 0 ),
					resolveExpressableTypeBasic( Character.class ));
		}

		// JPA says space is the default
		return new SqmLiteralCharacter(
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
		final SqmPluralAttributeReference attributeBinding = asPluralAttribute( (SqmNavigableReference) ctx.path().accept( this ) );
		return new SqmCollectionSize(
				attributeBinding,
				resolveExpressableTypeBasic( Integer.class )
		);
	}

	@Override
	public SqmCollectionIndexReference visitCollectionIndexFunction(HqlParser.CollectionIndexFunctionContext ctx) {
		final String alias = ctx.identifier().getText();

		final SqmNavigableReference binding = querySpecProcessingStateStack.getCurrent().getAliasRegistry().findFromElementByAlias( alias );

		if ( binding == null || !SqmPluralAttributeReference.class.isInstance( binding ) ) {
			// most likely a semantic problem, but not necessarily...
			throw new ParsingException( "Could not resolve identification variable [" + alias + "] as plural-attribute, encountered : " + binding );
		}

		final SqmPluralAttributeReference attributeBinding = (SqmPluralAttributeReference) binding;

		if ( !isIndexedPluralAttribute( attributeBinding ) ) {
			throw new SemanticException(
					"index() function can only be applied to identification variables which resolve to an " +
							"indexed collection (map,list); specified identification variable [" + alias +
							"] resolved to " + attributeBinding
			);
		}

		return (SqmCollectionIndexReference) attributeBinding.getReferencedNavigable()
				.getPersistentCollectionDescriptor()
				.getIndexDescriptor()
				.createSqmExpression( attributeBinding.getExportedFromElement(), attributeBinding, this );
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	private boolean isIndexedPluralAttribute(SqmPluralAttributeReference attributeBinding) {
		return attributeBinding.getReferencedNavigable().getPersistentCollectionDescriptor().getCollectionClassification() == CollectionClassification.MAP
				|| attributeBinding.getReferencedNavigable().getPersistentCollectionDescriptor().getCollectionClassification() == CollectionClassification.LIST;
	}

	@Override
	public SqmMaxElementReferenceBasic visitMaxElementFunction(HqlParser.MaxElementFunctionContext ctx) {
		if ( getSessionFactory().getSessionFactoryOptions().isStrictJpaQueryLanguageCompliance() ) {
			throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.HQL_COLLECTION_FUNCTION );
		}

		return new SqmMaxElementReferenceBasic( asPluralAttribute( (SqmNavigableReference) ctx.path().accept( this ) ) );
	}

	@Override
	public SqmMinElementReference visitMinElementFunction(HqlParser.MinElementFunctionContext ctx) {
		if ( getSessionFactory().getSessionFactoryOptions().isStrictJpaQueryLanguageCompliance() ) {
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
		if ( getSessionFactory().getSessionFactoryOptions().isStrictJpaQueryLanguageCompliance() ) {
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
		if ( getSessionFactory().getSessionFactoryOptions().isStrictJpaQueryLanguageCompliance() ) {
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
