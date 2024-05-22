/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.spi;

import org.hibernate.query.sqm.tree.expression.Conversion;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.AggregateFunctionExpression;
import org.hibernate.sql.ast.tree.expression.Any;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Distinct;
import org.hibernate.sql.ast.tree.expression.Duration;
import org.hibernate.sql.ast.tree.expression.DurationUnit;
import org.hibernate.sql.ast.tree.expression.EmbeddableTypeLiteral;
import org.hibernate.sql.ast.tree.expression.EntityTypeLiteral;
import org.hibernate.sql.ast.tree.expression.Every;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.ExtractUnit;
import org.hibernate.sql.ast.tree.expression.Format;
import org.hibernate.sql.ast.tree.expression.FunctionExpression;
import org.hibernate.sql.ast.tree.expression.JdbcLiteral;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.ModifiedSubQueryExpression;
import org.hibernate.sql.ast.tree.expression.Over;
import org.hibernate.sql.ast.tree.expression.Overflow;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;
import org.hibernate.sql.ast.tree.expression.SqlSelectionExpression;
import org.hibernate.sql.ast.tree.expression.Star;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.expression.TrimSpecification;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.FunctionTableReference;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.QueryPartTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.from.ValuesTableReference;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.predicate.ExistsPredicate;
import org.hibernate.sql.ast.tree.predicate.FilterPredicate;
import org.hibernate.sql.ast.tree.predicate.InArrayPredicate;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;

/**
 * A simple walker that checks for aggregate functions.
 *
 * @author Christian Beikov
 */
public class AggregateFunctionChecker extends AbstractSqlAstWalker {

	private static final AggregateFunctionChecker INSTANCE = new AggregateFunctionChecker();

	private static class AggregateFunctionException extends RuntimeException {}

	public static boolean hasAggregateFunctions(Expression expression) {
		try {
			expression.accept( INSTANCE );
			return false;
		}
		catch (AggregateFunctionException ex) {
			return true;
		}
	}

	public static boolean hasAggregateFunctions(QuerySpec querySpec) {
		try {
			querySpec.getSelectClause().accept( INSTANCE );
			querySpec.visitSortSpecifications( INSTANCE::visitSortSpecification );
			return false;
		}
		catch (AggregateFunctionException ex) {
			return true;
		}
	}

	@Override
	public void visitSelfRenderingExpression(SelfRenderingExpression expression) {
		if ( expression instanceof AggregateFunctionExpression ) {
			throw new AggregateFunctionException();
		}
		else if ( expression instanceof FunctionExpression ) {
			for ( SqlAstNode argument : ( (FunctionExpression) expression ).getArguments() ) {
				argument.accept( this );
			}
		}
	}

	@Override
	public void visitOver(Over<?> over) {
		// Only need to visit the expression over which the window is created as the window definition can't have aggregates
		// If the expression is an aggregate function, this means the aggregate is used as window function, which is fine
		// We only care about actually aggregating functions, which might be an argument of this function though
		if ( over.getExpression() instanceof AggregateFunctionExpression ) {
			final AggregateFunctionExpression aggregate = (AggregateFunctionExpression) over.getExpression();
			for ( SqlAstNode argument : aggregate.getArguments() ) {
				argument.accept( this );
			}
			if ( aggregate.getFilter() != null ) {
				aggregate.getFilter().accept( this );
			}
		}
		else {
			over.getExpression().accept( this );
		}
	}

	// --- to ignore ---
	// There is no need to look into the following AST nodes as the aggregate check is only for the top level

	@Override
	public void visitSelectStatement(SelectStatement statement) {
	}

	@Override
	public void visitDeleteStatement(DeleteStatement statement) {
	}

	@Override
	public void visitUpdateStatement(UpdateStatement statement) {
	}

	@Override
	public void visitInsertStatement(InsertSelectStatement statement) {
	}

	@Override
	public void visitAssignment(Assignment assignment) {
	}

	@Override
	public void visitQueryGroup(QueryGroup queryGroup) {
	}

	@Override
	public void visitQuerySpec(QuerySpec querySpec) {
	}

	@Override
	public void visitColumnReference(ColumnReference columnReference) {
	}

	@Override
	public void visitExtractUnit(ExtractUnit extractUnit) {
	}

	@Override
	public void visitFormat(Format format) {
	}

	@Override
	public void visitDistinct(Distinct distinct) {
	}

	@Override
	public void visitOverflow(Overflow overflow) {
	}

	@Override
	public void visitStar(Star star) {
	}

	@Override
	public void visitOffsetFetchClause(QueryPart querySpec) {
	}

	@Override
	public void visitTrimSpecification(TrimSpecification trimSpecification) {
	}

	@Override
	public void visitCastTarget(CastTarget castTarget) {
	}

	@Override
	public void visitDurationUnit(DurationUnit durationUnit) {
	}

	@Override
	public void visitDuration(Duration duration) {
	}

	@Override
	public void visitConversion(Conversion conversion) {
	}

	@Override
	public void visitInListPredicate(InListPredicate inListPredicate) {
	}

	@Override
	public void visitInArrayPredicate(InArrayPredicate predicate) {
	}

	@Override
	public void visitInSubQueryPredicate(InSubQueryPredicate inSubQueryPredicate) {
	}

	@Override
	public void visitModifiedSubQueryExpression(ModifiedSubQueryExpression expression) {
	}

	@Override
	public void visitAny(Any any) {
	}

	@Override
	public void visitEvery(Every every) {
	}

	@Override
	public void visitExistsPredicate(ExistsPredicate existsPredicate) {
	}

	@Override
	public void visitFilterPredicate(FilterPredicate filterPredicate) {
	}

	@Override
	public void visitParameter(JdbcParameter jdbcParameter) {
	}

	@Override
	public void visitJdbcLiteral(JdbcLiteral<?> jdbcLiteral) {
	}

	@Override
	public void visitQueryLiteral(QueryLiteral<?> queryLiteral) {
	}

	@Override
	public void visitSummarization(Summarization every) {
	}

	@Override
	public void visitEntityTypeLiteral(EntityTypeLiteral expression) {
	}

	@Override
	public void visitEmbeddableTypeLiteral(EmbeddableTypeLiteral expression) {
	}

	@Override
	public void visitSqlSelectionExpression(SqlSelectionExpression expression) {
	}

	@Override
	public void visitNamedTableReference(NamedTableReference tableReference) {
	}

	@Override
	public void visitValuesTableReference(ValuesTableReference tableReference) {
	}

	@Override
	public void visitQueryPartTableReference(QueryPartTableReference tableReference) {
	}

	@Override
	public void visitFunctionTableReference(FunctionTableReference tableReference) {
	}

	@Override
	public void visitTableReferenceJoin(TableReferenceJoin tableReferenceJoin) {
	}

	@Override
	public void visitFromClause(FromClause fromClause) {
	}

	@Override
	public void visitTableGroup(TableGroup tableGroup) {
	}

	@Override
	public void visitTableGroupJoin(TableGroupJoin tableGroupJoin) {
	}
}
