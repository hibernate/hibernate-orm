/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.internal;

import java.lang.reflect.Field;
import java.util.Locale;

import org.hibernate.query.QueryLogger;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmCorrelation;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmIndexedCollectionAccessPath;
import org.hibernate.query.sqm.tree.domain.SqmMapEntryReference;
import org.hibernate.query.sqm.tree.domain.SqmMaxElementPath;
import org.hibernate.query.sqm.tree.domain.SqmMaxIndexPath;
import org.hibernate.query.sqm.tree.domain.SqmMinElementPath;
import org.hibernate.query.sqm.tree.domain.SqmMinIndexPath;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmCaseSearched;
import org.hibernate.query.sqm.tree.expression.SqmCaseSimple;
import org.hibernate.query.sqm.tree.expression.SqmCollectionSize;
import org.hibernate.query.sqm.tree.expression.SqmConcat;
import org.hibernate.query.sqm.tree.expression.SqmCriteriaParameter;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.SqmLiteralEntityType;
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmParameterizedEntityType;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;
import org.hibernate.query.sqm.tree.expression.SqmRestrictedSubQueryExpression;
import org.hibernate.query.sqm.tree.expression.SqmTuple;
import org.hibernate.query.sqm.tree.expression.SqmUnaryOperation;
import org.hibernate.query.sqm.tree.expression.function.SqmAbsFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmAvgFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmBitLengthFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCastFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCastTarget;
import org.hibernate.query.sqm.tree.expression.function.SqmCoalesceFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmConcatFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCountFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCountStarFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCurrentDateFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCurrentInstantFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCurrentTimeFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCurrentTimestampFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmExtractFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmExtractUnit;
import org.hibernate.query.sqm.tree.expression.function.SqmGenericFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmLengthFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmLocateFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmLowerFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmMaxFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmMinFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmModFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmNullifFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmSqrtFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmStrFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmSubstringFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmSumFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmTrimFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmUpperFunction;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.predicate.SqmBooleanExpressionPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmEmptinessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmLikePredicate;
import org.hibernate.query.sqm.tree.predicate.SqmMemberOfPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNegatedPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNullnessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmAndPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmBetweenPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmComparisonPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmGroupedPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInListPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInSubQueryPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmOrPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.select.SqmGroupByClause;
import org.hibernate.query.sqm.tree.select.SqmHavingClause;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;
import org.hibernate.query.sqm.tree.update.SqmAssignment;
import org.hibernate.query.sqm.tree.update.SqmSetClause;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.ast.produce.spi.SqlAstFunctionProducer;

import org.jboss.logging.Logger;

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

	private static final Logger LOGGER = QueryLogger.subLogger( "sqm.sqmTree" );
	private static final boolean DEBUG_ENABLED = LOGGER.isDebugEnabled();

	public static void logTree(SqmStatement sqmStatement) {
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

		LOGGER.debugf( "Semantic Query (SQM) Tree :\n%s", printer.buffer.toString() );
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
		buffer.append( line ).append( '\n' );
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
						logWithIndentation( "[target = %s]", statement.getTarget().getNavigablePath().getFullPath() );
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
						logWithIndentation( "[target = %s]", statement.getTarget().getNavigablePath().getFullPath() );
						processStanza(
								"into",
								() -> statement.getInsertionTargetPaths().forEach( sqmPath -> sqmPath.accept( this ) )
						);
						visitQuerySpec( statement.getSelectQuerySpec() );
					}
			);
		}

		return null;
	}

	@Override
	public Object visitSelectStatement(SqmSelectStatement<?> statement) {
		if ( DEBUG_ENABLED ) {
			processStanza(
					"select",
					() -> visitQuerySpec( statement.getQuerySpec() )
			);
		}

		return null;
	}

	@Override
	public Object visitUpdateStatement(SqmUpdateStatement<?> statement) {
		if ( DEBUG_ENABLED ) {
			processStanza(
					"update",
					() -> {
						logWithIndentation( "[target = %s]", statement.getTarget().getNavigablePath().getFullPath() );

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
	public Object visitAssignment(SqmAssignment assignment) {
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
	public Object visitQuerySpec(SqmQuerySpec querySpec) {
		processStanza(
				"query-spec",
				() -> {
					visitSelectClause( querySpec.getSelectClause() );

					visitFromClause( querySpec.getFromClause() );

					visitGroupByClause( querySpec.getGroupByClause() );
					visitHavingClause( querySpec.getHavingClause() );

					visitWhereClause( querySpec.getWhereClause() );

					visitOrderByClause( querySpec.getOrderByClause() );

					visitLimitExpression( querySpec.getLimitExpression() );
					visitOffsetExpression( querySpec.getOffsetExpression() );
				}
		);

		return null;
	}

	@Override
	public Object visitGroupByClause(SqmGroupByClause clause) {
		if ( clause != null ) {
			processStanza(
					"group-by",
					() -> clause.visitGroupings( this::visitGrouping )
			);
		}

		return null;
	}

	@Override
	public Object visitGrouping(SqmGroupByClause.SqmGrouping grouping) {
		processStanza(
				"grouping",
				() -> grouping.getExpression().accept( this )
		);

		return null;
	}

	@Override
	public Object visitHavingClause(SqmHavingClause clause) {
		if ( clause != null ) {
			processStanza(
					"having",
					() -> clause.getPredicate().accept( this )
			);
		}

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
				'`' + sqmRoot.getNavigablePath().getFullPath() + '`',
				() -> processJoins( sqmRoot )
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
				'`' + joinedFromElement.getNavigablePath().getFullPath() + '`',
				() -> processJoins( joinedFromElement )
		);

		return null;
	}

	@Override
	public Object visitQualifiedEntityJoin(SqmEntityJoin joinedFromElement) {
		processStanza(
				"entity",
				'`' + joinedFromElement.getNavigablePath().getFullPath() + '`',
				() -> {
					if ( joinedFromElement.getJoinPredicate() != null ) {
						processStanza(
								"on",
								() -> joinedFromElement.getJoinPredicate().accept( this )
						);
					}

					processJoins( joinedFromElement );
				}
		);

		return null;
	}

	@Override
	public Object visitQualifiedAttributeJoin(SqmAttributeJoin joinedFromElement) {
		processStanza(
				"attribute",
				'`' + joinedFromElement.getNavigablePath().getFullPath() + '`',
				() -> {
					logIndented( "[fetched = " + joinedFromElement.isFetched() + ']' );

					if ( joinedFromElement.getJoinPredicate() != null ) {
						processStanza(
								"on",
								() -> joinedFromElement.getJoinPredicate().accept( this )
						);
					}

					processJoins( joinedFromElement );
				}
		);

		return null;
	}

	@Override
	public Object visitBasicValuedPath(SqmBasicValuedSimplePath path) {
		logWithIndentation( "-> [basic-path] - `%s`", path.getNavigablePath().getFullPath() );

		return null;
	}

	@Override
	public Object visitEmbeddableValuedPath(SqmEmbeddedValuedSimplePath path) {
		logWithIndentation( "-> [embedded-path] - `%s`", path.getNavigablePath().getFullPath() );

		return null;
	}

	@Override
	public Object visitEntityValuedPath(SqmEntityValuedSimplePath path) {
		logWithIndentation( "-> [entity-path] - `%s`", path.getNavigablePath().getFullPath() );

		return null;
	}

	@Override
	public Object visitPluralValuedPath(SqmPluralValuedSimplePath path) {
		logWithIndentation( "-> [plural-path] - `%s`", path.getNavigablePath().getFullPath() );

		return null;
	}

	@Override
	public Object visitIndexedPluralAccessPath(SqmIndexedCollectionAccessPath path) {
		return null;
	}

	@Override
	public Object visitTreatedPath(SqmTreatedPath sqmTreatedPath) {
		return null;
	}

	@Override
	public Object visitCorrelation(SqmCorrelation correlation) {
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
	public Object visitCriteriaParameter(SqmCriteriaParameter expression) {
		return null;
	}

	@Override
	public Object visitEntityTypeLiteralExpression(SqmLiteralEntityType expression) {
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
	public Object visitGenericFunction(SqmGenericFunction expression) {
		return null;
	}

	@Override
	public Object visitSqlAstFunctionProducer(SqlAstFunctionProducer sqlAstFunctionProducer) {
		return null;
	}

	@Override
	public Object visitAbsFunction(SqmAbsFunction function) {
		return null;
	}

	@Override
	public Object visitAvgFunction(SqmAvgFunction expression) {
		return null;
	}

	@Override
	public Object visitBitLengthFunction(SqmBitLengthFunction sqmBitLengthFunction) {
		return null;
	}

	@Override
	public Object visitCastFunction(SqmCastFunction expression) {
		return null;
	}

	@Override
	public Object visitCoalesceFunction(SqmCoalesceFunction expression) {
		return null;
	}

	@Override
	public Object visitCountFunction(SqmCountFunction expression) {
		return null;
	}

	@Override
	public Object visitCountStarFunction(SqmCountStarFunction expression) {
		return null;
	}

	@Override
	public Object visitCurrentDateFunction(SqmCurrentDateFunction sqmCurrentDate) {
		return null;
	}

	@Override
	public Object visitCurrentTimeFunction(SqmCurrentTimeFunction sqmCurrentTimeFunction) {
		return null;
	}

	@Override
	public Object visitCurrentTimestampFunction(SqmCurrentTimestampFunction sqmCurrentTimestampFunction) {
		return null;
	}

	@Override
	public Object visitCurrentInstantFunction(SqmCurrentInstantFunction function) {
		return null;
	}

	@Override
	public Object visitExtractFunction(SqmExtractFunction function) {
		return null;
	}

	@Override
	public Object visitExtractUnit(SqmExtractUnit extractUnit) {
		return null;
	}

	@Override
	public Object visitCastTarget(SqmCastTarget sqmCastTarget) {
		return null;
	}

	@Override
	public Object visitLengthFunction(SqmLengthFunction sqmLengthFunction) {
		return null;
	}

	@Override
	public Object visitLocateFunction(SqmLocateFunction function) {
		return null;
	}

	@Override
	public Object visitLowerFunction(SqmLowerFunction expression) {
		return null;
	}

	@Override
	public Object visitMaxFunction(SqmMaxFunction expression) {
		return null;
	}

	@Override
	public Object visitMinFunction(SqmMinFunction expression) {
		return null;
	}

	@Override
	public Object visitModFunction(SqmModFunction sqmModFunction) {
		return null;
	}

	@Override
	public Object visitNullifFunction(SqmNullifFunction expression) {
		return null;
	}

	@Override
	public Object visitSqrtFunction(SqmSqrtFunction sqmSqrtFunction) {
		return null;
	}

	@Override
	public Object visitStrFunction(SqmStrFunction sqmStrFunction) {
		return null;
	}

	@Override
	public Object visitSubstringFunction(SqmSubstringFunction expression) {
		return null;
	}

	@Override
	public Object visitSumFunction(SqmSumFunction expression) {
		return null;
	}

	@Override
	public Object visitTrimFunction(SqmTrimFunction expression) {
		return null;
	}

	@Override
	public Object visitUpperFunction(SqmUpperFunction expression) {
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
	public Object visitAndPredicate(SqmAndPredicate predicate) {
		processStanza(
				"and",
				() -> {
					predicate.getLeftHandPredicate().accept( this );
					predicate.getRightHandPredicate().accept( this );
				}
		);

		return null;
	}

	@Override
	public Object visitOrPredicate(SqmOrPredicate predicate) {
		processStanza(
				"or",
				() -> {
					predicate.getLeftHandPredicate().accept( this );
					predicate.getRightHandPredicate().accept( this );
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
		processStanza(
				predicate.isNegated() ? "is-not-like" : "is-like",
				() -> {
					predicate.getPattern().accept( this );
					predicate.getMatchExpression().accept( this );
					predicate.getEscapeCharacter().accept( this );
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
	public Object visitLimitExpression(SqmExpression expression) {
		return null;
	}

	@Override
	public Object visitPluralAttributeSizeFunction(SqmCollectionSize function) {
		return null;
	}

	@Override
	public Object visitMapEntryFunction(SqmMapEntryReference function) {
		return null;
	}

	@Override
	public Object visitMaxElementPath(SqmMaxElementPath binding) {
		return null;
	}

	@Override
	public Object visitMinElementPath(SqmMinElementPath path) {
		return null;
	}

	@Override
	public Object visitMaxIndexPath(SqmMaxIndexPath path) {
		return null;
	}

	@Override
	public Object visitMinIndexPath(SqmMinIndexPath path) {
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
	public Object visitConcatExpression(SqmConcat expression) {
		return null;
	}

	@Override
	public Object visitConcatFunction(SqmConcatFunction expression) {
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
	public Object visitFullyQualifiedField(Field field) {
		return null;
	}

	@Override
	public Object visitFullyQualifiedEnum(Enum value) {
		return null;
	}

	@Override
	public Object visitFullyQualifiedClass(Class namedClass) {
		return null;
	}



	@Override
	public Object visitRestrictedSubQueryExpression(SqmRestrictedSubQueryExpression sqmRestrictedSubQueryExpression) {
		return null;
	}
}
