/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import java.util.List;
import java.util.Locale;

import org.hibernate.metamodel.model.domain.DiscriminatorSqmPath;
import org.hibernate.metamodel.model.domain.internal.AnyDiscriminatorSqmPath;
import org.hibernate.query.QueryLogging;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.cte.SqmCteContainer;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.NonAggregatedCompositeSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmAnyValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmCorrelation;
import org.hibernate.query.sqm.tree.domain.SqmCteRoot;
import org.hibernate.query.sqm.tree.domain.SqmDerivedRoot;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmFkExpression;
import org.hibernate.query.sqm.tree.domain.SqmFunctionPath;
import org.hibernate.query.sqm.tree.domain.SqmFunctionRoot;
import org.hibernate.query.sqm.tree.domain.SqmIndexedCollectionAccessPath;
import org.hibernate.query.sqm.tree.domain.SqmMapEntryReference;
import org.hibernate.query.sqm.tree.domain.SqmElementAggregateFunction;
import org.hibernate.query.sqm.tree.domain.SqmIndexAggregateFunction;
import org.hibernate.query.sqm.tree.domain.SqmPluralPartJoin;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;
import org.hibernate.query.sqm.tree.expression.AsWrapperSqmExpression;
import org.hibernate.query.sqm.tree.expression.JpaCriteriaParameter;
import org.hibernate.query.sqm.tree.expression.SqmAny;
import org.hibernate.query.sqm.tree.expression.SqmAnyDiscriminatorValue;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmByUnit;
import org.hibernate.query.sqm.tree.expression.SqmCaseSearched;
import org.hibernate.query.sqm.tree.expression.SqmCaseSimple;
import org.hibernate.query.sqm.tree.expression.SqmCastTarget;
import org.hibernate.query.sqm.tree.expression.SqmCoalesce;
import org.hibernate.query.sqm.tree.expression.SqmCollation;
import org.hibernate.query.sqm.tree.expression.SqmCollectionSize;
import org.hibernate.query.sqm.tree.expression.SqmDistinct;
import org.hibernate.query.sqm.tree.expression.SqmDurationUnit;
import org.hibernate.query.sqm.tree.expression.SqmEnumLiteral;
import org.hibernate.query.sqm.tree.expression.SqmEvery;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmExtractUnit;
import org.hibernate.query.sqm.tree.expression.SqmFieldLiteral;
import org.hibernate.query.sqm.tree.expression.SqmFormat;
import org.hibernate.query.sqm.tree.expression.SqmFunction;
import org.hibernate.query.sqm.tree.expression.SqmHqlNumericLiteral;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.SqmLiteralEmbeddableType;
import org.hibernate.query.sqm.tree.expression.SqmLiteralEntityType;
import org.hibernate.query.sqm.tree.expression.SqmModifiedSubQueryExpression;
import org.hibernate.query.sqm.tree.expression.SqmNamedExpression;
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmOver;
import org.hibernate.query.sqm.tree.expression.SqmOverflow;
import org.hibernate.query.sqm.tree.expression.SqmParameterizedEntityType;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;
import org.hibernate.query.sqm.tree.expression.SqmSetReturningFunction;
import org.hibernate.query.sqm.tree.expression.SqmStar;
import org.hibernate.query.sqm.tree.expression.SqmSummarization;
import org.hibernate.query.sqm.tree.expression.SqmToDuration;
import org.hibernate.query.sqm.tree.expression.SqmTrimSpecification;
import org.hibernate.query.sqm.tree.expression.SqmTuple;
import org.hibernate.query.sqm.tree.expression.SqmUnaryOperation;
import org.hibernate.query.sqm.tree.expression.SqmWindow;
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
import org.hibernate.query.sqm.tree.predicate.SqmNegatedPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNullnessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmTruthnessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.select.SqmJpaCompoundSelection;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.query.sqm.tree.select.SqmQueryGroup;
import org.hibernate.query.sqm.tree.select.SqmQueryPart;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;
import org.hibernate.query.sqm.tree.update.SqmAssignment;
import org.hibernate.query.sqm.tree.update.SqmSetClause;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;

import org.jboss.logging.Logger;

import jakarta.persistence.criteria.Predicate;

/**
 * Printer for an SQM tree - for debugging purpose
 *
 * @implNote At the top-level (statement) we check against {@link #DEBUG_ENABLED}
 * and decide whether to continue or not.  That's to avoid unnecessary, continued
 * checking of that boolean.  The assumption being that we only ever enter from
 * these statement rules
 *
 * @author Steve Ebersole
 */
public class SqmTreePrinter implements SemanticQueryWalker<Object> {
	private static final Logger log = Logger.getLogger( SqmTreePrinter.class );

	private static final Logger LOGGER = QueryLogging.subLogger( "sqm.ast" );
	private static final boolean DEBUG_ENABLED = LOGGER.isDebugEnabled();

	public static void logTree(SqmQuerySpec sqmQuerySpec, String header) {
		if ( ! DEBUG_ENABLED ) {
			return;
		}

		final SqmTreePrinter treePrinter = new SqmTreePrinter();

		treePrinter.visitQuerySpec( sqmQuerySpec );

		final String title = header != null ? header : "SqmQuerySpec Tree";

		LOGGER.debugf( "%s :%n%s", title, treePrinter.buffer.toString() );
	}

	public static void logTree(SqmStatement sqmStatement) {
		if ( ! DEBUG_ENABLED ) {
			return;
		}

		final SqmTreePrinter printer = new SqmTreePrinter();

		if ( sqmStatement instanceof SqmSelectStatement ) {
			printer.visitSelectStatement( (SqmSelectStatement) sqmStatement );
		}
		else if ( sqmStatement instanceof SqmDeleteStatement<?> ) {
			printer.visitDeleteStatement( (SqmDeleteStatement) sqmStatement );
		}
		else if ( sqmStatement instanceof SqmUpdateStatement ) {
			printer.visitUpdateStatement( (SqmUpdateStatement) sqmStatement );
		}
		else if ( sqmStatement instanceof SqmInsertSelectStatement ) {
			printer.visitInsertSelectStatement( (SqmInsertSelectStatement) sqmStatement );
		}

		LOGGER.debugf( "SqmStatement Tree :%n%s", printer.buffer.toString() );
	}

	private final StringBuffer buffer = new StringBuffer();
	private int depth = 2;

	private void processStanza(String name, Runnable continuation) {
		processStanza( name, false, continuation );
	}

	private void processStanza(String name, String description, Runnable continuation) {
		processStanza( name, description, false, continuation );
	}

	private void processStanza(String name, boolean indentContinuation, Runnable continuation) {
		logWithIndentation( "-> [%s]", name );
		depth++;

		try {
			if ( indentContinuation ) {
				depth++;
			}
			continuation.run();
		}
		catch (Exception e) {
			log.debugf( e, "Error processing stanza {%s}", name );
		}
		finally {
			if ( indentContinuation ) {
				depth--;
			}
		}

		depth--;
		logWithIndentation( "<- [%s]", name );
	}

	private void processStanza(
			String name,
			String description,
			boolean indentContinuation,
			Runnable continuation) {
		final String stanzaLabel = description == null
				? "[" + name + ']'
				: "[" + name + "] - " + description;
		logWithIndentation( "-> " + stanzaLabel );
		depth++;

		try {
			if ( indentContinuation ) {
				depth++;
			}
			continuation.run();
		}
		catch (Exception e) {
			log.debugf( e, "Error processing stanza {%s}", name );
		}
		finally {
			if ( indentContinuation ) {
				depth--;
			}
		}

		depth--;
		logWithIndentation( "<- " + stanzaLabel );
	}

	private void logWithIndentation(Object line) {
		pad( depth );
		buffer.append( line ).append( System.lineSeparator() );
	}

	private void pad(int depth) {
		for ( int i = 0; i < depth; i++ ) {
			buffer.append( "  " );
		}
	}

	private void logWithIndentation(String pattern, Object arg1) {
		logWithIndentation(  String.format( pattern, arg1 ) );
	}

	private void logWithIndentation(String pattern, Object arg1, Object arg2) {
		logWithIndentation(  String.format( pattern, arg1, arg2 ) );
	}

	private void logWithIndentation(String pattern, Object... args) {
		logWithIndentation(  String.format( pattern, args ) );
	}

	private void logIndented(String line) {
		depth++;
		logWithIndentation( line );
		depth--;
	}

	private void logIndented(String pattern, Object arg) {
		depth++;
		logWithIndentation( String.format( Locale.ROOT, pattern, arg ) );
		depth--;
	}

	private void logIndented(String pattern, Object arg1, Object arg2) {
		depth++;
		logWithIndentation( String.format( Locale.ROOT, pattern, arg1, arg2 ) );
		depth--;
	}

	private void logIndented(String pattern, Object... args) {
		depth++;
		logWithIndentation( String.format( Locale.ROOT, pattern, args ) );
		depth--;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// statements

	@Override
	public Object visitDeleteStatement(SqmDeleteStatement<?> statement) {
		if ( DEBUG_ENABLED ) {
			processStanza(
					"delete",
					() -> {
						logWithIndentation( "[target = %s]", statement.getTarget().getNavigablePath() );
						visitWhereClause( statement.getWhereClause() );
					}
			);
		}

		return null;
	}

	@Override
	public Object visitInsertSelectStatement(SqmInsertSelectStatement<?> statement) {
		if ( DEBUG_ENABLED ) {
			processStanza(
					"insert",
					() -> {
						logWithIndentation( "[target = %s]", statement.getTarget().getNavigablePath() );
						processStanza(
								"into",
								() -> statement.getInsertionTargetPaths().forEach( sqmPath -> sqmPath.accept( this ) )
						);
						statement.getSelectQueryPart().accept( this );
					}
			);
		}

		return null;
	}

	@Override
	public Object visitInsertValuesStatement(SqmInsertValuesStatement<?> statement) {
		if ( DEBUG_ENABLED ) {
			processStanza(
					"insert",
					() -> {
						logWithIndentation( "[target = %s]", statement.getTarget().getNavigablePath() );
						processStanza(
								"into",
								() -> statement.getInsertionTargetPaths().forEach( sqmPath -> sqmPath.accept( this ) )
						);
						if ( statement.getConflictClause() != null ) {
							processStanza(
									"on conflict",
									() -> statement.getConflictClause().accept( this )
							);
						}
					}
			);
		}

		return null;
	}

	@Override
	public Object visitConflictClause(SqmConflictClause<?> sqmConflictClause) {
		if ( sqmConflictClause.getConstraintName() != null ) {
			logWithIndentation( "[constraintName = %s]", sqmConflictClause.getConstraintName() );
		}
		else {
			processStanza(
					"constraint attributes",
					() -> sqmConflictClause.getConstraintPaths().forEach( sqmPath -> sqmPath.accept( this ) )
			);
		}
		final SqmConflictUpdateAction<?> updateAction = sqmConflictClause.getConflictAction();
		if ( updateAction == null ) {
			logWithIndentation( "do nothing" );
		}
		else {
			logWithIndentation( "do update " );
			visitSetClause( updateAction.getSetClause() );
			visitWhereClause( updateAction.getWhereClause() );
		}
		return null;
	}

	@Override
	public Object visitSelectStatement(SqmSelectStatement<?> statement) {
		if ( DEBUG_ENABLED ) {
			processStanza(
					"select",
					() -> statement.getQueryPart().accept( this )
			);
		}

		return null;
	}

	@Override
	public Object visitCteStatement(SqmCteStatement sqmCteStatement) {
		if ( DEBUG_ENABLED ) {
			logIndented( "cte" );
		}

		return null;
	}

	@Override
	public Object visitCteContainer(SqmCteContainer consumer) {
		return null;
	}

	@Override
	public Object visitUpdateStatement(SqmUpdateStatement<?> statement) {
		if ( DEBUG_ENABLED ) {
			processStanza(
					statement.isVersioned() ? "update versioned" : "update",
					() -> {
						logWithIndentation( "[target = %s]", statement.getTarget().getNavigablePath() );

						visitSetClause( statement.getSetClause() );

						visitWhereClause( statement.getWhereClause() );
					}
			);
		}

		return null;
	}

	@Override
	public Object visitSetClause(SqmSetClause setClause) {
		processStanza(
				"set",
				() -> setClause.getAssignments().forEach( this::visitAssignment )
		);

		return null;
	}

	@Override
	public Object visitAssignment(SqmAssignment<?> assignment) {
		processStanza(
				"assignment",
				() -> {
					logWithIndentation( "=" );
					depth++;
					logWithIndentation( "[%s]", assignment.getTargetPath().getNavigablePath() );
					assignment.getValue().accept( this );
					depth--;
				}
		);

		return null;
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// query-spec

	@Override
	public Object visitQueryGroup(SqmQueryGroup<?> queryGroup) {
		processStanza(
				"query-group",
				() -> {
					for ( SqmQueryPart<?> queryPart : queryGroup.getQueryParts() ) {
						if ( queryPart instanceof SqmQuerySpec<?> ) {
							visitQuerySpec( (SqmQuerySpec<?>) queryPart );
						}
						else {
							visitQueryGroup( (SqmQueryGroup<?>) queryPart );
						}
					}
				}
		);
		return null;
	}

	@Override
	public Object visitQuerySpec(SqmQuerySpec<?> querySpec) {
		processStanza(
				"query-spec",
				() -> {
					visitSelectClause( querySpec.getSelectClause() );
					visitFromClause( querySpec.getFromClause() );
					visitWhereClause( querySpec.getWhereClause() );

					visitGroupByClause( querySpec.getGroupByClauseExpressions() );
					visitHavingClause( querySpec.getHavingClausePredicate() );

					visitOrderByClause( querySpec.getOrderByClause() );
					visitOffsetExpression( querySpec.getOffsetExpression() );
					visitFetchExpression( querySpec.getFetchExpression() );
				}
		);

		return null;
	}

	@Override
	public Object visitGroupByClause(List<SqmExpression<?>> groupByClauseExpressions) {
		if ( groupByClauseExpressions != null && !groupByClauseExpressions.isEmpty() ) {
			processStanza(
					"group-by",
					() -> groupByClauseExpressions.forEach( e -> e.accept( this ) )
			);
		}

		return null;
	}

	@Override
	public Object visitHavingClause(SqmPredicate predicate) {
		if ( predicate != null ) {
			processStanza(
					"having",
					() -> predicate.accept( this )
			);
		}

		return null;
	}

	@Override
	public Object visitJpaCompoundSelection(SqmJpaCompoundSelection<?> selection) {
		processStanza(
				"JpaCompoundSelection",
				() -> {
					for ( SqmSelectableNode<?> selectionItem : selection.getSelectionItems() ) {
						selectionItem.accept( this );
					}
				}
		);

		return null;
	}

	@Override
	public Object visitFromClause(SqmFromClause fromClause) {
		processStanza(
				"from",
				() -> fromClause.visitRoots( this::visitRootPath )
		);

		return null;
	}

	@Override
	public Object visitRootPath(SqmRoot sqmRoot) {
		processStanza(
				"root",
				"`" + sqmRoot.getNavigablePath() + "`",
				() -> processJoins( sqmRoot )
		);

		return null;
	}

	@Override
	public Object visitRootDerived(SqmDerivedRoot<?> sqmRoot) {
		processStanza(
				"derived",
				"`" + sqmRoot.getNavigablePath() + "`",
				() -> {
					processJoins( sqmRoot );
				}
		);
		return null;
	}

	@Override
	public Object visitRootFunction(SqmFunctionRoot sqmRoot) {
		processStanza(
				"derived",
				"`" + sqmRoot.getNavigablePath() + "`",
				() -> {
					processJoins( sqmRoot );
				}
		);
		return null;
	}

	@Override
	public Object visitRootCte(SqmCteRoot<?> sqmRoot) {
		processStanza(
				"cte",
				"`" + sqmRoot.getNavigablePath() + "`",
				() -> {
					processJoins( sqmRoot );
				}
		);
		return null;
	}

	private void processJoins(SqmFrom<?,?> sqmFrom) {
		if ( !sqmFrom.hasJoins() ) {
			return;
		}

		processStanza(
				"joins",
				() -> sqmFrom.visitSqmJoins( sqmJoin -> sqmJoin.accept( this ) )
		);
	}

	@Override
	public Object visitCrossJoin(SqmCrossJoin joinedFromElement) {
		processStanza(
				"cross",
				"`" + joinedFromElement.getNavigablePath() + "`",
				() -> processJoins( joinedFromElement )
		);

		return null;
	}

	@Override
	public Object visitPluralPartJoin(SqmPluralPartJoin<?, ?> joinedFromElement) {
		processStanza(
				"plural-part",
				"`" + joinedFromElement.getNavigablePath() + "`",
				() -> processJoins( joinedFromElement )
		);

		return null;
	}

	private boolean inJoinPredicate;

	private void processJoinPredicate(SqmJoin<?, ?> joinedFromElement) {
		if ( joinedFromElement.getJoinPredicate() != null ) {
			boolean oldInJoinPredicate = inJoinPredicate;
			inJoinPredicate = true;
			processStanza(
					"on",
					() -> joinedFromElement.getJoinPredicate().accept( this )
			);
			inJoinPredicate = oldInJoinPredicate;
		}
	}

	@Override
	public Object visitQualifiedEntityJoin(SqmEntityJoin joinedFromElement) {
		if ( inJoinPredicate ) {
			logWithIndentation( "-> [joined-path] - `%s`", joinedFromElement.getNavigablePath() );
		}
		else {
			processStanza(
					"entity",
					"`" + joinedFromElement.getNavigablePath() + "`",
					() -> {
						processJoinPredicate( joinedFromElement );
						processJoins( joinedFromElement );
					}
			);
		}
		return null;
	}

	@Override
	public Object visitQualifiedAttributeJoin(SqmAttributeJoin joinedFromElement) {
		if ( inJoinPredicate ) {
			logWithIndentation( "-> [joined-path] - `%s`", joinedFromElement.getNavigablePath() );
		}
		else {
			processStanza(
					"attribute",
					"`" + joinedFromElement.getNavigablePath() + "`",
					() -> {
						logIndented( "[fetched = " + joinedFromElement.isFetched() + ']' );

						processJoinPredicate( joinedFromElement );
						processJoins( joinedFromElement );
					}
			);
		}
		return null;
	}

	@Override
	public Object visitQualifiedDerivedJoin(SqmDerivedJoin<?> joinedFromElement) {
		if ( inJoinPredicate ) {
			logWithIndentation( "-> [joined-path] - `%s`", joinedFromElement.getNavigablePath() );
		}
		else {
			processStanza(
					"derived",
					"`" + joinedFromElement.getNavigablePath() + "`",
					() -> {
						processJoinPredicate( joinedFromElement );
						processJoins( joinedFromElement );
					}
			);
		}
		return null;
	}

	@Override
	public Object visitQualifiedFunctionJoin(SqmFunctionJoin<?> joinedFromElement) {
		if ( inJoinPredicate ) {
			logWithIndentation( "-> [joined-path] - `%s`", joinedFromElement.getNavigablePath() );
		}
		else {
			processStanza(
					"derived",
					"`" + joinedFromElement.getNavigablePath() + "`",
					() -> {
						processJoinPredicate( joinedFromElement );
						processJoins( joinedFromElement );
					}
			);
		}
		return null;
	}

	@Override
	public Object visitQualifiedCteJoin(SqmCteJoin<?> joinedFromElement) {
		if ( inJoinPredicate ) {
			logWithIndentation( "-> [joined-path] - `%s`", joinedFromElement.getNavigablePath() );
		}
		else {
			processStanza(
					"cte",
					"`" + joinedFromElement.getNavigablePath() + "`",
					() -> {
						processJoinPredicate( joinedFromElement );
						processJoins( joinedFromElement );
					}
			);
		}
		return null;
	}

	@Override
	public Object visitBasicValuedPath(SqmBasicValuedSimplePath path) {
		logWithIndentation( "-> [basic-path] - `%s`", path.getNavigablePath() );

		return null;
	}

	@Override
	public Object visitEmbeddableValuedPath(SqmEmbeddedValuedSimplePath path) {
		logWithIndentation( "-> [embedded-path] - `%s`", path.getNavigablePath() );

		return null;
	}

	@Override
	public Object visitAnyValuedValuedPath(SqmAnyValuedSimplePath<?> path) {
		logWithIndentation( "-> [any-path] - `%s`", path.getNavigablePath() );

		return null;
	}

	@Override
	public Object visitNonAggregatedCompositeValuedPath(NonAggregatedCompositeSimplePath<?> path) {
		logWithIndentation( "-> [non-aggregated-composite-path] - `%s`", path.getNavigablePath() );

		return null;
	}

	@Override
	public Object visitFkExpression(SqmFkExpression<?> fkExpression) {
		logWithIndentation( "-> [fk-ref] - `%s`", fkExpression.getToOnePath().getNavigablePath() );

		return null;
	}

	@Override
	public Object visitDiscriminatorPath(DiscriminatorSqmPath<?> sqmPath) {
		logWithIndentation( "-> [discriminator-path] - `%s`", sqmPath.getNavigablePath() );

		return null;
	}

	@Override
	public Object visitEntityValuedPath(SqmEntityValuedSimplePath path) {
		logWithIndentation( "-> [entity-path] - `%s`", path.getNavigablePath() );

		return null;
	}

	@Override
	public Object visitPluralValuedPath(SqmPluralValuedSimplePath path) {
		logWithIndentation( "-> [plural-path] - `%s`", path.getNavigablePath() );

		return null;
	}

	@Override
	public Object visitIndexedPluralAccessPath(SqmIndexedCollectionAccessPath<?> path) {
		return null;
	}

	@Override
	public Object visitTreatedPath(SqmTreatedPath sqmTreatedPath) {
		return null;
	}

	@Override
	public Object visitCorrelation(SqmCorrelation<?, ?> correlation) {
		return null;
	}

	@Override
	public Object visitSelectClause(SqmSelectClause selectClause) {
		processStanza(
				selectClause.isDistinct() ? "select(distinct)" : "select",
				() -> selectClause.getSelections().forEach( this::visitSelection )
		);

		return null;
	}

	@Override
	public Object visitSelection(SqmSelection selection) {
		processStanza(
				selection.getAlias() == null ? "selection" : "selection(" + selection.getAlias() + ")",
				() -> selection.getSelectableNode().accept( this )
		);

		return null;
	}

	@Override
	public Object visitValues(SqmValues values) {
		return null;
	}

	@Override
	public Object visitPositionalParameterExpression(SqmPositionalParameter expression) {
		logWithIndentation( "?%s", expression.getPosition() );

		return null;
	}

	@Override
	public Object visitNamedParameterExpression(SqmNamedParameter expression) {
		logWithIndentation( ":%s", expression.getName() );

		return null;
	}

	@Override
	public Object visitJpaCriteriaParameter(JpaCriteriaParameter expression) {
		return null;
	}

	@Override
	public Object visitEntityTypeLiteralExpression(SqmLiteralEntityType expression) {
		return null;
	}

	@Override
	public Object visitEmbeddableTypeLiteralExpression(SqmLiteralEmbeddableType<?> expression) {
		return null;
	}

	@Override
	public Object visitParameterizedEntityTypeExpression(SqmParameterizedEntityType expression) {
		return null;
	}

	@Override
	public Object visitUnaryOperationExpression(SqmUnaryOperation expression) {
		return null;
	}

	@Override
	public Object visitFunction(SqmFunction<?> tSqmFunction) {
		return null;
	}

	@Override
	public Object visitSetReturningFunction(SqmSetReturningFunction<?> tSqmFunction) {
		return null;
	}

	@Override
	public Object visitCoalesce(SqmCoalesce<?> sqmCoalesce) {
		return null;
	}

	@Override
	public Object visitToDuration(SqmToDuration<?> toDuration) {
		return null;
	}

	@Override
	public Object visitByUnit(SqmByUnit sqmByUnit) {
		return null;
	}

	@Override
	public Object visitExtractUnit(SqmExtractUnit<?> extractUnit) {
		return null;
	}

	@Override
	public Object visitFormat(SqmFormat sqmFormat) {
		return null;
	}

	@Override
	public Object visitCastTarget(SqmCastTarget<?> sqmCastTarget) {
		return null;
	}

	@Override
	public Object visitTrimSpecification(SqmTrimSpecification trimSpecification) {
		return null;
	}

	@Override
	public Object visitDistinct(SqmDistinct<?> distinct) {
		return null;
	}

	@Override
	public Object visitOverflow(SqmOverflow<?> sqmOverflow) {
		return null;
	}

	@Override
	public Object visitDurationUnit(SqmDurationUnit<?> durationUnit) {
		return null;
	}

	@Override
	public Object visitStar(SqmStar sqmStar) {
		return null;
	}

	@Override
	public Object visitOver(SqmOver<?> over) {
		return null;
	}

	@Override
	public Object visitWindow(SqmWindow window) {
		return null;
	}

	@Override
	public Object visitWhereClause(SqmWhereClause whereClause) {
		if ( whereClause != null && whereClause.getPredicate() != null ) {
			processStanza(
					"where",
					() -> whereClause.getPredicate().accept( this )
			);
		}

		return null;
	}

	@Override
	public Object visitGroupedPredicate(SqmGroupedPredicate predicate) {
		processStanza(
				"grouped",
				() -> {
					depth++;
					predicate.getSubPredicate().accept( this );
					depth--;
				}
		);

		return null;
	}

	@Override
	public Object visitJunctionPredicate(SqmJunctionPredicate predicate) {
		processStanza(
				predicate.getOperator() == Predicate.BooleanOperator.AND ? "and" : "or",
				() -> {
					for ( SqmPredicate subPredicate : predicate.getPredicates() ) {
						subPredicate.accept( this );
					}
				}
		);

		return null;
	}

	@Override
	public Object visitComparisonPredicate(SqmComparisonPredicate predicate) {
		processStanza(
				predicate.isNegated() ? predicate.getSqmOperator().negated().name() : predicate.getSqmOperator().name(),
				() -> {
					depth++;
					try {
						predicate.getLeftHandExpression().accept( this );
						predicate.getRightHandExpression().accept( this );
					}
					finally {
						depth--;
					}
				}
		);

		return null;
	}

	@Override
	public Object visitIsEmptyPredicate(SqmEmptinessPredicate predicate) {
		processStanza(
				predicate.isNegated() ? "is-not-empty" : "is-empty",
				() -> {
					depth++;
					predicate.getPluralPath().accept( this );
					depth--;
				}
		);

		return null;
	}

	@Override
	public Object visitIsNullPredicate(SqmNullnessPredicate predicate) {
		processStanza(
				predicate.isNegated() ? "is-not-null" : "is-null",
				true,
				() -> predicate.getExpression().accept( this )
		);

		return null;
	}

	@Override
	public Object visitIsTruePredicate(SqmTruthnessPredicate predicate) {
		processStanza(
				(predicate.isNegated() ? "is-not-" : "is-") + predicate.getBooleanValue(),
				true,
				() -> predicate.getExpression().accept( this )
		);

		return null;
	}

	@Override
	public Object visitBetweenPredicate(SqmBetweenPredicate predicate) {
		processStanza(
				predicate.isNegated() ? "is-not-between" : "is-between",
				() -> {
					predicate.getExpression().accept( this );
					predicate.getLowerBound().accept( this );
					predicate.getUpperBound().accept( this );
				}
		);
		return null;
	}

	@Override
	public Object visitLikePredicate(SqmLikePredicate predicate) {
		final String likeType = predicate.isCaseSensitive() ? "like" : "ilike";
		processStanza(
				( predicate.isNegated() ? "is-not-" : "is-" ) + likeType,
				() -> {
					predicate.getPattern().accept( this );
					predicate.getMatchExpression().accept( this );
					if ( predicate.getEscapeCharacter() != null ) {
						predicate.getEscapeCharacter().accept( this );
					}
				}
		);
		return null;
	}

	@Override
	public Object visitMemberOfPredicate(SqmMemberOfPredicate predicate) {
		return null;
	}

	@Override
	public Object visitNegatedPredicate(SqmNegatedPredicate predicate) {
		return null;
	}

	@Override
	public Object visitInListPredicate(SqmInListPredicate predicate) {
		return null;
	}

	@Override
	public Object visitInSubQueryPredicate(SqmInSubQueryPredicate predicate) {
		return null;
	}

	@Override
	public Object visitBooleanExpressionPredicate(SqmBooleanExpressionPredicate predicate) {
		return null;
	}

	@Override
	public Object visitExistsPredicate(SqmExistsPredicate sqmExistsPredicate) {
		return null;
	}

	@Override
	public Object visitOrderByClause(SqmOrderByClause orderByClause) {
		return null;
	}

	@Override
	public Object visitSortSpecification(SqmSortSpecification sortSpecification) {
		return null;
	}

	@Override
	public Object visitOffsetExpression(SqmExpression expression) {
		return null;
	}

	@Override
	public Object visitFetchExpression(SqmExpression expression) {
		return null;
	}

	@Override
	public Object visitPluralAttributeSizeFunction(SqmCollectionSize function) {
		return null;
	}

	@Override
	public Object visitMapEntryFunction(SqmMapEntryReference<?, ?> function) {
		return null;
	}

	@Override
	public Object visitElementAggregateFunction(SqmElementAggregateFunction binding) {
		return null;
	}

	@Override
	public Object visitIndexAggregateFunction(SqmIndexAggregateFunction path) {
		return null;
	}

	@Override
	public Object visitFunctionPath(SqmFunctionPath<?> functionPath) {
		return null;
	}

	@Override
	public Object visitLiteral(SqmLiteral literal) {
		return null;
	}

	@Override
	public Object visitTuple(SqmTuple sqmTuple) {
		return null;
	}

	@Override
	public Object visitCollation(SqmCollation sqmCollate) {
		return null;
	}

	@Override
	public Object visitBinaryArithmeticExpression(SqmBinaryArithmetic expression) {
		return null;
	}

	@Override
	public Object visitSubQueryExpression(SqmSubQuery expression) {
		return null;
	}

	@Override
	public Object visitSimpleCaseExpression(SqmCaseSimple expression) {
		return null;
	}

	@Override
	public Object visitSearchedCaseExpression(SqmCaseSearched expression) {
		return null;
	}

	@Override
	public Object visitAny(SqmAny<?> sqmAny) {
		return null;
	}

	@Override
	public Object visitEvery(SqmEvery<?> sqmEvery) {
		return null;
	}

	@Override
	public Object visitSummarization(SqmSummarization<?> sqmSummarization) {
		return null;
	}

	@Override
	public Object visitAnyDiscriminatorTypeExpression(AnyDiscriminatorSqmPath<?> expression) {
		return null;
	}

	@Override
	public Object visitAnyDiscriminatorTypeValueExpression(SqmAnyDiscriminatorValue<?> expression) {
		return null;
	}

	@Override
	public Object visitDynamicInstantiation(SqmDynamicInstantiation sqmDynamicInstantiation) {
		processStanza(
				"dynamic-instantiation (" + sqmDynamicInstantiation.getInstantiationTarget().getJavaType() + ')',
				() -> processStanza(
						"arguments",
						() -> ( (SqmDynamicInstantiation<?>) sqmDynamicInstantiation ).getArguments().forEach(
								argument -> processStanza(
										"argument (" + argument.getAlias() + ')',
										() -> {
											depth++;
											argument.getSelectableNode().accept( this );
											depth--;
										}
								)
						)
				)
		);

		return null;
	}

	@Override
	public Object visitEnumLiteral(SqmEnumLiteral<?> sqmEnumLiteral) {
		return null;
	}

	@Override
	public Object visitFieldLiteral(SqmFieldLiteral<?> sqmFieldLiteral) {
		return null;
	}

	@Override
	public <N extends Number> Object visitHqlNumericLiteral(SqmHqlNumericLiteral<N> numericLiteral) {
		return null;
	}

	@Override
	public Object visitFullyQualifiedClass(Class namedClass) {
		return null;
	}

	@Override
	public Object visitAsWrapperExpression(AsWrapperSqmExpression expression) {
		return null;
	}

	@Override
	public Object visitNamedExpression(SqmNamedExpression<?> expression) {
		return null;
	}

	@Override
	public Object visitModifiedSubQueryExpression(SqmModifiedSubQueryExpression expression) {
		return null;
	}
}
