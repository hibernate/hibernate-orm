/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DmlTargetColumnQualifierSupport;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.DerivedTableReference;
import org.hibernate.sql.ast.tree.from.QueryPartTableReference;
import org.hibernate.sql.ast.tree.from.ValuesTableReference;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;

import java.util.List;

public class InterSystemsIRISSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	protected InterSystemsIRISSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super(sessionFactory, statement);
	}


	@Override
	protected void renderDeleteClause(DeleteStatement statement) {
		appendSql("delete");
		final Stack<Clause> clauseStack = getClauseStack();
		try {
			clauseStack.push(Clause.DELETE);
			appendSql(" from ");
			renderDmlTargetTableExpression(statement.getTargetTable());
			renderTableReferenceIdentificationVariable(statement.getTargetTable());
		}
		finally {
			clauseStack.pop();
		}
	}


	@Override
	protected void renderTupleComparisonStandard(
			List<SqlSelection> lhsSelections,
			SqlTuple rhsTuple,
			ComparisonOperator operator
	) {

		if (operator == ComparisonOperator.EQUAL || operator == ComparisonOperator.NOT_EQUAL) {
			emulateTupleComparisonSelections(lhsSelections, rhsTuple, operator);
		}
		else {

			super.renderTupleComparisonStandard(lhsSelections, rhsTuple, operator);
		}
	}

	@SuppressWarnings("unchecked")
	protected void emulateTupleComparisonSelections(
			List<SqlSelection> lhsSelections,
			SqlTuple rhsTuple,
			ComparisonOperator operator
	) {
		final List<Expression> rhsExpressions = (List<Expression>) rhsTuple.getExpressions();

		if (lhsSelections.size() != rhsExpressions.size()) {
			throw new IllegalArgumentException("Tuple size mismatch");
		}

		final String joiner = (operator == ComparisonOperator.EQUAL) ? " and " : " or ";

		appendSql(OPEN_PARENTHESIS);
		for (int i = 0; i < lhsSelections.size(); i++) {
			if (i > 0) appendSql(joiner);

			lhsSelections.get(i).getExpression().accept(this);
			appendSql(operator.sqlText());
			rhsExpressions.get(i).accept(this);
		}
		appendSql(CLOSE_PARENTHESIS);
	}

	@Override
	public void visitValuesTableReference(ValuesTableReference tableReference) {
		emulateValuesTableReferenceColumnAliasing(tableReference);
	}

	@Override
	protected void renderUpdateClause(UpdateStatement updateStatement) {
		appendSql("update");
		final Stack<Clause> clauseStack = getClauseStack();
		try {
			clauseStack.push(Clause.UPDATE);
			append(WHITESPACE);
			renderDmlTargetTableExpression(updateStatement.getTargetTable());
			renderTableReferenceIdentificationVariable(updateStatement.getTargetTable());
		}
		finally {
			clauseStack.pop();
		}
	}


	@Override
	protected String determineColumnReferenceQualifier(ColumnReference columnReference) {
		final DmlTargetColumnQualifierSupport qualifierSupport = getDialect().getDmlTargetColumnQualifierSupport();
		final MutationStatement currentDmlStatement;
		final String dmlAlias;

		if ( getClauseStack().getCurrent() != Clause.SET
			|| !( ( currentDmlStatement = getCurrentDmlStatement() ) instanceof InsertSelectStatement)
			|| ( dmlAlias = currentDmlStatement.getTargetTable().getIdentificationVariable() ) == null
			|| !dmlAlias.equals( columnReference.getQualifier() ) ) {
			return columnReference.getQualifier();
		}
		// Qualify the column reference with the table expression also when in subqueries
		else if ( qualifierSupport != DmlTargetColumnQualifierSupport.NONE || !getQueryPartStack().isEmpty() ) {
			return getCurrentDmlStatement().getTargetTable().getTableExpression();
		}
		else {
			return null;
		}
	}

	@Override
	public void visitBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression) {
		if ( isIntegerDivisionEmulationRequired( arithmeticExpression ) ) {
			visitArithmeticOperand( arithmeticExpression.getLeftHandOperand() );
			appendSql(" \\ ");
			visitArithmeticOperand( arithmeticExpression.getRightHandOperand() );
		}
		else {
			super.visitBinaryArithmeticExpression(arithmeticExpression);
		}
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

	protected boolean shouldEmulateFetchClause(QueryPart queryPart) {
		// Check if current query part is already row numbering to avoid infinite recursion
		if ( getQueryPartForRowNumbering() == queryPart || isRowsOnlyFetchClauseType( queryPart ) ) {
			return false;
		}
		return !getDialect().supportsFetchClause( queryPart.getFetchClauseType() );
	}


	@Override
	protected void renderDerivedTableReferenceIdentificationVariable(DerivedTableReference tableReference) {
		renderTableReferenceIdentificationVariable( tableReference );
	}

	@Override
	public void visitQueryPartTableReference(QueryPartTableReference tableReference) {
		emulateQueryPartTableReferenceColumnAliasing( tableReference );
	}

	@Override
	protected void renderFromClauseAfterUpdateSet(UpdateStatement statement) {
		if ( statement.getFromClause().getRoots().isEmpty() ) {
			appendSql( " from " );
			renderDmlTargetTableExpression( statement.getTargetTable() );
			renderTableReferenceIdentificationVariable(statement.getTargetTable());
		}
		else {
			visitFromClause( statement.getFromClause() );
		}
	}

}
