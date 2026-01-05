/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast;

import java.util.List;

import org.hibernate.Locking;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.from.DerivedTableReference;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.QueryPartTableReference;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.InArrayPredicate;
import org.hibernate.sql.ast.tree.predicate.LikePredicate;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * A SQL AST translator for Spanner.
 *
 * @author Christian Beikov
 */
public class SpannerSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	// Spanner lacks the lateral keyword and instead has an unnest/array mechanism
	private boolean correlated;

	public SpannerSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	@Override
	protected LockStrategy determineLockingStrategy(
			QuerySpec querySpec,
			Locking.FollowOn followOnLocking) {
		return LockStrategy.NONE;
	}

	@Override
	public void visitOffsetFetchClause(QueryPart queryPart) {
		renderLimitOffsetClause( queryPart );
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		renderComparisonEmulateIntersect( lhs, operator, rhs );
	}

	@Override
	protected void renderSelectTupleComparison(
			List<SqlSelection> lhsExpressions,
			SqlTuple tuple,
			ComparisonOperator operator) {
		emulateSelectTupleComparison( lhsExpressions, tuple.getExpressions(), operator, true );
	}

	@Override
	protected void renderPartitionItem(Expression expression) {
		if ( expression instanceof Literal ) {
			appendSql( "'0' || '0'" );
		}
		else if ( expression instanceof Summarization ) {
			// This could theoretically be emulated by rendering all grouping variations of the query and
			// connect them via union all but that's probably pretty inefficient and would have to happen
			// on the query spec level
			throw new UnsupportedOperationException( "Summarization is not supported by DBMS" );
		}
		else {
			expression.accept( this );
		}
	}

	@Override
	public void visitSelectClause(SelectClause selectClause) {
		getClauseStack().push( Clause.SELECT );

		try {
			appendSql( "select " );
			if ( correlated ) {
				appendSql( "as struct " );
			}
			if ( selectClause.isDistinct() ) {
				appendSql( "distinct " );
			}
			visitSqlSelections( selectClause );
		}
		finally {
			getClauseStack().pop();
		}
	}

	@Override
	protected void renderDerivedTableReference(DerivedTableReference tableReference) {
		final boolean correlated = tableReference.isLateral();
		final boolean oldCorrelated = this.correlated;
		if ( correlated ) {
			this.correlated = true;
			appendSql( "unnest(array" );
		}
		tableReference.accept( this );
		if ( correlated ) {
			this.correlated = oldCorrelated;
			appendSql( CLOSE_PARENTHESIS );
			// Spanner requires the alias to be outside the parentheses UNNEST(... ) alias
			super.renderTableReferenceIdentificationVariable( tableReference );
		}
	}

	@Override
	protected void visitDeleteStatementOnly(DeleteStatement statement) {
		// Spanner requires a WHERE in delete clause so we add "where true" if there is none
		if ( !hasWhere( statement.getRestriction() ) ) {
			renderDeleteClause( statement );
			appendSql( " where true" );
			visitReturningColumns( statement.getReturningColumns() );
		}
		else {
			super.visitDeleteStatementOnly( statement );
		}
	}

	@Override
	protected void visitUpdateStatementOnly(UpdateStatement statement) {
		// Spanner requires a WHERE in update clause so we add "where true" if there is none
		if ( !hasWhere( statement.getRestriction() ) ) {
			renderUpdateClause( statement );
			renderSetClause( statement.getAssignments() );
			appendSql( " where true" );
			visitReturningColumns( statement.getReturningColumns() );
		}
		else {
			super.visitUpdateStatementOnly( statement );
		}
	}

	@Override
	protected void renderTableReferenceIdentificationVariable(TableReference tableReference) {
		// Spanner requires `UNNEST(...) alias`. Standard rendering places the alias
		// inside the parentheses UNNEST(... alias). We suppress it here to manually
		// render it outside the UNNEST wrapper in `renderDerivedTableReference`.
		if ( correlated
			&& tableReference instanceof DerivedTableReference
			&& ((DerivedTableReference) tableReference).isLateral() ) {
			return;
		}
		super.renderTableReferenceIdentificationVariable( tableReference );
	}

	@Override
	protected void renderDmlTargetTableExpression(NamedTableReference tableReference) {
		super.renderDmlTargetTableExpression( tableReference );
		if ( getClauseStack().getCurrent() != Clause.INSERT ) {
			renderTableReferenceIdentificationVariable( tableReference );
		}
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
	public void visitInArrayPredicate(InArrayPredicate inArrayPredicate) {
		inArrayPredicate.getTestExpression().accept( this );
		appendSql( " in unnest(" );
		inArrayPredicate.getArrayParameter().accept( this );
		appendSql( ')' );
	}

	@Override
	public void visitLikePredicate(LikePredicate likePredicate) {
		if ( likePredicate.getEscapeCharacter() != null ) {
			throw new UnsupportedOperationException( "Escape character is not supported by Spanner" );
		}
		else {
			super.visitLikePredicate( likePredicate );
		}
	}

}
