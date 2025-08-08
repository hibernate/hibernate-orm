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
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.ValuesTableReference;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;

import java.util.List;

public class InterSystemsIRISqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {
    protected InterSystemsIRISqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
        super(sessionFactory, statement);
    }


    @Override
    public boolean supportsRowValueConstructorSyntax() {
        return false;
    }

    @Override
    public boolean supportsRowValueConstructorSyntaxInSet() {
        return false;
    }

    @Override
    public boolean supportsRowValueConstructorSyntaxInInList() {
        return false;
    }

    @Override
    protected boolean supportsRowValueConstructorSyntaxInQuantifiedPredicates() {
        return false;
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
        } finally {
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
        } else {

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
        } finally {
            clauseStack.pop();
        }
    }

    @Override
    protected String determineColumnReferenceQualifier(ColumnReference columnReference) {
        final DmlTargetColumnQualifierSupport qualifierSupport = getDialect().getDmlTargetColumnQualifierSupport();
        final MutationStatement currentDmlStatement = getCurrentDmlStatement();

        if (currentDmlStatement == null) {
            return columnReference.getQualifier();
        }

        final String dmlAlias = currentDmlStatement.getTargetTable().getIdentificationVariable();

        if (dmlAlias != null && dmlAlias.equals(columnReference.getQualifier())) {
            return dmlAlias;
        }

        if (qualifierSupport != DmlTargetColumnQualifierSupport.NONE) {
            return dmlAlias;
        }

        return columnReference.getQualifier();
    }

}
