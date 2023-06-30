/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect;

import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.BinaryArithmeticOperator;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.from.FunctionTableReference;
import org.hibernate.sql.ast.tree.from.QueryPartTableReference;
import org.hibernate.sql.ast.tree.insert.Values;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.model.internal.TableInsertStandard;

/**
 * A SQL AST translator for HANA.
 *
 * @author Christian Beikov
 */
public class HANASqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	private boolean inLateral;

	public HANASqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	protected boolean shouldEmulateFetchClause(QueryPart queryPart) {
		// HANA only supports the LIMIT + OFFSET syntax but also window functions
		// Check if current query part is already row numbering to avoid infinite recursion
		return useOffsetFetchClause( queryPart ) && getQueryPartForRowNumbering() != queryPart
				&& !isRowsOnlyFetchClauseType( queryPart );
	}

	@Override
	protected boolean supportsWithClauseInSubquery() {
		// HANA doesn't seem to support correlation, so we just report false here for simplicity
		return false;
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
	protected SqlAstNodeRenderingMode getParameterRenderingMode() {
		// HANA does not support parameters in lateral subqueries for some reason, so inline all the parameters in this case
		return inLateral ? SqlAstNodeRenderingMode.INLINE_ALL_PARAMETERS : super.getParameterRenderingMode();
	}

	@Override
	public void visitFunctionTableReference(FunctionTableReference tableReference) {
		tableReference.getFunctionExpression().accept( this );
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
		renderComparisonEmulateIntersect( lhs, operator, rhs );
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
	protected boolean supportsRowValueConstructorSyntaxInQuantifiedPredicates() {
		return false;
	}

	@Override
	protected boolean supportsRowValueConstructorGtLtSyntax() {
		return false;
	}

	@Override
	protected String getFromDual() {
		return " from sys.dummy";
	}

	@Override
	protected String getFromDualForSelectOnly() {
		return getFromDual();
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
}
