/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.ordering.ast;

import org.hibernate.query.NullPrecedence;
import org.hibernate.query.SortDirection;
import org.hibernate.query.spi.QueryEngine;
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

	String toDescriptiveText();

	/**
	 * Apply the SQL AST sort-specifications associated with this ordering-expression
	 */
	void apply(
			QuerySpec ast,
			TableGroup tableGroup,
			String collation,
			String modelPartName,
			SortDirection sortOrder,
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
			final QueryEngine queryEngine =
					creationState.getCreationContext().getSessionFactory()
							.getQueryEngine();
			final SqmToSqlAstConverter converter =
					creationState instanceof SqmToSqlAstConverter sqmToSqlAstConverter
							? sqmToSqlAstConverter
							: new FakeSqmToSqlAstConverter(creationState);
			sortExpression =
					queryEngine.getSqmFunctionRegistry()
							.findFunctionDescriptor( "collate" )
							.generateSqmExpression(
									new SqmSelfRenderingExpression<>( walker -> expression, null, null ),
									null,
									queryEngine
							)
							.convertToSqlAst( converter );
		}
		return sortExpression;
	}
}
