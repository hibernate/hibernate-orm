/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.ordering.ast;

import org.hibernate.query.sqm.NullPrecedence;
import org.hibernate.query.sqm.SortOrder;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.StandardFunctions;
import org.hibernate.query.sqm.sql.FakeSqmToSqlAstConverter;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.expression.SqmSelfRenderingExpression;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.select.QuerySpec;

/**
 * Contract for anything that can be a sort expression
 *
 * @author Steve Ebersole
 */
public interface OrderingExpression extends Node {

	SqlAstNode resolve(QuerySpec ast, TableGroup tableGroup, String modelPartName, SqlAstCreationState creationState);

	/**
	 * Apply the SQL AST sort-specifications associated with this ordering-expression
	 */
	void apply(
			QuerySpec ast,
			TableGroup tableGroup,
			String collation,
			String modelPartName,
			SortOrder sortOrder,
			NullPrecedence nullPrecedence,
			SqlAstCreationState creationState);


	static Expression applyCollation(
			Expression expression,
			String collation,
			SqlAstCreationState creationState) {
		final Expression sortExpression;
		if ( collation == null ) {
			sortExpression = expression;
		}
		else {
			final QueryEngine queryEngine = creationState.getCreationContext()
					.getSessionFactory()
					.getQueryEngine();
			final SqmToSqlAstConverter converter;
			if ( creationState instanceof SqmToSqlAstConverter ) {
				converter = (SqmToSqlAstConverter) creationState;
			}
			else {
				converter = new FakeSqmToSqlAstConverter( creationState );
			}
			sortExpression = queryEngine
					.getSqmFunctionRegistry()
					.findFunctionDescriptor( StandardFunctions.COLLATE )
					.generateSqmExpression(
							new SqmSelfRenderingExpression<>( walker -> expression, null, null ),
							null,
							queryEngine,
							queryEngine.getTypeConfiguration()
					)
					.convertToSqlAst( converter );
		}
		return sortExpression;
	}
}
