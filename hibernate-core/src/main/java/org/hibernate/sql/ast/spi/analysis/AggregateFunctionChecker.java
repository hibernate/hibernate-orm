/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.analysis;

import org.hibernate.query.sqm.tree.spi.expression.Conversion;
import org.hibernate.sql.ast.spi.AbstractSqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstNode;
import org.hibernate.sql.ast.spi.query.delete.DeleteStatement;
import org.hibernate.sql.ast.spi.query.expression.AggregateFunctionExpression;
import org.hibernate.sql.ast.spi.query.expression.Any;
import org.hibernate.sql.ast.spi.query.expression.CastTarget;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.Distinct;
import org.hibernate.sql.ast.spi.query.expression.Duration;
import org.hibernate.sql.ast.spi.query.expression.DurationUnit;
import org.hibernate.sql.ast.spi.query.expression.EmbeddableTypeLiteral;
import org.hibernate.sql.ast.spi.query.expression.EntityTypeLiteral;
import org.hibernate.sql.ast.spi.query.expression.Every;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.ExtractUnit;
import org.hibernate.sql.ast.spi.query.expression.Format;
import org.hibernate.sql.ast.spi.query.expression.FunctionExpression;
import org.hibernate.sql.ast.spi.query.expression.JdbcLiteral;
import org.hibernate.sql.ast.spi.query.expression.JdbcParameter;
import org.hibernate.sql.ast.spi.query.expression.ModifiedSubQueryExpression;
import org.hibernate.sql.ast.spi.query.expression.Over;
import org.hibernate.sql.ast.spi.query.expression.Overflow;
import org.hibernate.sql.ast.spi.query.expression.QueryLiteral;
import org.hibernate.sql.ast.spi.query.expression.SelfRenderingExpression;
import org.hibernate.sql.ast.spi.query.expression.SqlSelectionExpression;
import org.hibernate.sql.ast.spi.query.expression.Star;
import org.hibernate.sql.ast.spi.query.expression.Summarization;
import org.hibernate.sql.ast.spi.query.expression.TrimSpecification;
import org.hibernate.sql.ast.spi.query.from.FromClause;
import org.hibernate.sql.ast.spi.query.from.FunctionTableReference;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.from.QueryPartTableReference;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupJoin;
import org.hibernate.sql.ast.spi.query.from.TableReferenceJoin;
import org.hibernate.sql.ast.spi.query.from.ValuesTableReference;
import org.hibernate.sql.ast.spi.query.insert.InsertSelectStatement;
import org.hibernate.sql.ast.spi.query.predicate.ExistsPredicate;
import org.hibernate.sql.ast.spi.query.predicate.FilterPredicate;
import org.hibernate.sql.ast.spi.query.predicate.InArrayPredicate;
import org.hibernate.sql.ast.spi.query.predicate.InListPredicate;
import org.hibernate.sql.ast.spi.query.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.spi.query.select.QueryGroup;
import org.hibernate.sql.ast.spi.query.select.QueryPart;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.ast.spi.query.update.Assignment;
import org.hibernate.sql.ast.spi.query.update.UpdateStatement;

/**
 * A simple walker that checks for aggregate functions.
 *
 * @author Christian Beikov
 */
public class AggregateFunctionChecker extends AbstractSqlAstWalker {

	private static final AggregateFunctionChecker INSTANCE = new AggregateFunctionChecker();

	private static class AggregateFunctionException extends RuntimeException {
		@Override
		public Throwable fillInStackTrace() {
			return this;
		}
	}

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
		else if ( expression instanceof FunctionExpression functionExpression ) {
			for ( SqlAstNode argument : functionExpression.getArguments() ) {
				argument.accept( this );
			}
		}
	}

	@Override
	public void visitOver(Over<?> over) {
		// Only need to visit the expression over which the window is created as the window definition can't have aggregates
		// If the expression is an aggregate function, this means the aggregate is used as window function, which is fine
		// We only care about actually aggregating functions, which might be an argument of this function though
		if ( over.getExpression() instanceof AggregateFunctionExpression aggregate ) {
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
