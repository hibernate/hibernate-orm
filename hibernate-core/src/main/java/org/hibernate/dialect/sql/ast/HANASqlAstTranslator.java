/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast;

import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.dialect.HANADialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.from.DerivedTableReference;
import org.hibernate.sql.ast.tree.from.FunctionTableReference;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.QueryPartTableReference;
import org.hibernate.sql.ast.tree.from.ValuesTableReference;
import org.hibernate.sql.ast.tree.insert.ConflictClause;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.insert.Values;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.model.internal.TableInsertStandard;

import static org.hibernate.dialect.sql.ast.SybaseASESqlAstTranslator.isLob;

/**
 * An SQL AST translator for HANA.
 *
 * @author Christian Beikov
 */
public class HANASqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	private boolean inLateral;

	public HANASqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	@Override
	public void visitBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression) {
		if ( isIntegerDivisionEmulationRequired( arithmeticExpression ) ) {
			appendSql( "cast(" );
			visitArithmeticOperand( arithmeticExpression.getLeftHandOperand() );
			appendSql( arithmeticExpression.getOperator().getOperatorSqlTextString() );
			visitArithmeticOperand( arithmeticExpression.getRightHandOperand() );
			appendSql( " as int)" );
		}
		else {
			super.visitBinaryArithmeticExpression( arithmeticExpression );
		}
	}

	@Override
	protected void visitArithmeticOperand(Expression expression) {
		render( expression, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
	}

	private boolean isHanaCloud() {
		return ( (HANADialect) getDialect() ).isCloud();
	}

	@Override
	protected void visitInsertStatementOnly(InsertSelectStatement statement) {
		if ( statement.getConflictClause() == null || statement.getConflictClause().isDoNothing() ) {
			// Render plain insert statement and possibly run into unique constraint violation
			super.visitInsertStatementOnly( statement );
		}
		else {
			visitInsertStatementEmulateMerge( statement );
		}
	}

	@Override
	protected void visitUpdateStatementOnly(UpdateStatement statement) {
		// HANA Cloud does not support the FROM clause in UPDATE statements
		if ( isHanaCloud() && hasNonTrivialFromClause( statement.getFromClause() ) ) {
			visitUpdateStatementEmulateMerge( statement );
		}
		else {
			super.visitUpdateStatementOnly( statement );
		}
	}

	@Override
	protected void renderUpdateClause(UpdateStatement updateStatement) {
		// HANA Cloud does not support the FROM clause in UPDATE statements
		if ( isHanaCloud() ) {
			super.renderUpdateClause( updateStatement );
		}
		else {
			appendSql( "update" );
			final Stack<Clause> clauseStack = getClauseStack();
			try {
				clauseStack.push( Clause.UPDATE );
				renderTableReferenceIdentificationVariable( updateStatement.getTargetTable() );
			}
			finally {
				clauseStack.pop();
			}
		}
	}

	@Override
	protected void renderFromClauseAfterUpdateSet(UpdateStatement statement) {
		// HANA Cloud does not support the FROM clause in UPDATE statements
		if ( !isHanaCloud() ) {
			if ( statement.getFromClause().getRoots().isEmpty() ) {
				appendSql( " from " );
				renderDmlTargetTableExpression( statement.getTargetTable() );
			}
			else {
				visitFromClause( statement.getFromClause() );
			}
		}
	}

	@Override
	protected void renderDmlTargetTableExpression(NamedTableReference tableReference) {
		super.renderDmlTargetTableExpression( tableReference );
		if ( getClauseStack().getCurrent() != Clause.INSERT ) {
			renderTableReferenceIdentificationVariable( tableReference );
		}
	}

	@Override
	protected void visitConflictClause(ConflictClause conflictClause) {
		if ( conflictClause != null ) {
			if ( conflictClause.isDoUpdate() && conflictClause.getConstraintName() != null ) {
				throw new IllegalQueryOperationException( "Insert conflict 'do update' clause with constraint name is not supported" );
			}
		}
	}

	protected boolean shouldEmulateFetchClause(QueryPart queryPart) {
		// HANA only supports the LIMIT + OFFSET syntax but also window functions
		// Check if current query part is already row numbering to avoid infinite recursion
		return useOffsetFetchClause( queryPart ) && getQueryPartForRowNumbering() != queryPart
				&& !isRowsOnlyFetchClauseType( queryPart );
	}

	@Override
	protected boolean isCorrelated(CteStatement cteStatement) {
		// Report false here, because apparently HANA does not need the "lateral" keyword to correlate a from clause subquery in a subquery
		return false;
	}

	@Override
	public void visitQueryGroup(QueryGroup queryGroup) {
		if ( shouldEmulateFetchClause( queryGroup ) ) {
			emulateFetchOffsetWithWindowFunctions( queryGroup, true );
		}
		else {
			super.visitQueryGroup( queryGroup );
		}
	}

	@Override
	public void visitQuerySpec(QuerySpec querySpec) {
		if ( shouldEmulateFetchClause( querySpec ) ) {
			emulateFetchOffsetWithWindowFunctions( querySpec, true );
		}
		else {
			super.visitQuerySpec( querySpec );
		}
	}

	@Override
	public void visitQueryPartTableReference(QueryPartTableReference tableReference) {
		if ( tableReference.isLateral() && !inLateral ) {
			inLateral = true;
			emulateQueryPartTableReferenceColumnAliasing( tableReference );
			inLateral = false;
		}
		else {
			emulateQueryPartTableReferenceColumnAliasing( tableReference );
		}
	}

	@Override
	protected void renderDerivedTableReference(DerivedTableReference tableReference) {
		if ( tableReference instanceof FunctionTableReference && tableReference.isLateral() ) {
			// No need for a lateral keyword for functions
			tableReference.accept( this );
		}
		else {
			super.renderDerivedTableReference( tableReference );
		}
	}

	@Override
	public void renderNamedSetReturningFunction(String functionName, List<? extends SqlAstNode> sqlAstArguments, AnonymousTupleTableGroupProducer tupleType, String tableIdentifierVariable, SqlAstNodeRenderingMode argumentRenderingMode) {
		final ModelPart ordinalitySubPart = tupleType.findSubPart( CollectionPart.Nature.INDEX.getName(), null );
		if ( ordinalitySubPart != null ) {
			appendSql( "(select t.*, row_number() over() " );
			appendSql( ordinalitySubPart.asBasicValuedModelPart().getSelectionExpression() );
			appendSql( " from " );
			renderSimpleNamedFunction( functionName, sqlAstArguments, argumentRenderingMode );
			append( " t)" );
		}
		else {
			super.renderNamedSetReturningFunction( functionName, sqlAstArguments, tupleType, tableIdentifierVariable, argumentRenderingMode );
		}
	}

	@Override
	protected SqlAstNodeRenderingMode getParameterRenderingMode() {
		// HANA does not support parameters in lateral subqueries for some reason, so inline all the parameters in this case
		return inLateral ? SqlAstNodeRenderingMode.INLINE_ALL_PARAMETERS : super.getParameterRenderingMode();
	}

	@Override
	protected void renderDerivedTableReferenceIdentificationVariable(DerivedTableReference tableReference) {
		renderTableReferenceIdentificationVariable( tableReference );
	}

	@Override
	public void visitOffsetFetchClause(QueryPart queryPart) {
		if ( !isRowNumberingCurrentQueryPart() ) {
			renderLimitOffsetClause( queryPart );
		}
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		// In SAP HANA, LOBs are not "comparable", so we have to use a like predicate for comparison
		final boolean isLob = isLob( lhs.getExpressionType() );
		if ( operator == ComparisonOperator.DISTINCT_FROM || operator == ComparisonOperator.NOT_DISTINCT_FROM ) {
			if ( isLob ) {
				switch ( operator ) {
					case DISTINCT_FROM:
						appendSql( "case when " );
						lhs.accept( this );
						appendSql( " like " );
						rhs.accept( this );
						appendSql( " or " );
						lhs.accept( this );
						appendSql( " is null and " );
						rhs.accept( this );
						appendSql( " is null then 0 else 1 end=1" );
						return;
					case NOT_DISTINCT_FROM:
						appendSql( "case when " );
						lhs.accept( this );
						appendSql( " like " );
						rhs.accept( this );
						appendSql( " or " );
						lhs.accept( this );
						appendSql( " is null and " );
						rhs.accept( this );
						appendSql( " is null then 0 else 1 end=0" );
						return;
					default:
						// Fall through
						break;
				}
			}
			// HANA does not support plain parameters in the select clause of the intersect emulation
			withParameterRenderingMode(
					SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER,
					() -> renderComparisonEmulateIntersect( lhs, operator, rhs )
			);
		}
		else {
			if ( isLob ) {
				switch ( operator ) {
					case EQUAL:
						lhs.accept( this );
						appendSql( " like " );
						rhs.accept( this );
						return;
					case NOT_EQUAL:
						lhs.accept( this );
						appendSql( " not like " );
						rhs.accept( this );
						return;
					default:
						// Fall through
						break;
				}
			}
			renderComparisonStandard( lhs, operator, rhs );
		}
	}

	@Override
	protected void renderPartitionItem(Expression expression) {
		if ( expression instanceof Literal ) {
			appendSql( "grouping sets (())" );
		}
		else if ( expression instanceof Summarization ) {
			throw new UnsupportedOperationException( "Summarization is not supported by DBMS" );
		}
		else {
			expression.accept( this );
		}
	}

	@Override
	protected void renderInsertIntoNoColumns(TableInsertStandard tableInsert) {
		throw new MappingException(
				String.format(
						"The INSERT statement for table [%s] contains no column, and this is not supported by [%s]",
						tableInsert.getMutatingTable().getTableId(),
						getDialect()
				)
		);
	}

	@Override
	protected void visitValuesList(List<Values> valuesList) {
		visitValuesListEmulateSelectUnion( valuesList );
	}

	@Override
	public void visitValuesTableReference(ValuesTableReference tableReference) {
		emulateValuesTableReferenceColumnAliasing( tableReference );
	}
}
