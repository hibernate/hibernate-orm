/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.json;

import org.hibernate.Internal;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.sqm.CastType;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;

@Internal
public class ExpressionTypeHelper {

	public static boolean isBoolean(SqlAstNode node) {
		final Expression expression = (Expression) node;
		final JdbcMappingContainer expressionType = expression.getExpressionType();
		return expressionType.getJdbcTypeCount() == 1
				&& isBoolean( expressionType.getSingleJdbcMapping().getCastType() );
	}

	public static boolean isNonNativeBoolean(SqlAstNode node) {
		final Expression expression = (Expression) node;
		final JdbcMappingContainer expressionType = expression.getExpressionType();
		return expressionType.getJdbcTypeCount() == 1
				&& isNonNativeBoolean( expressionType.getSingleJdbcMapping().getCastType() );
	}

	public static boolean isJson(SqlAstNode node) {
		final Expression expression = (Expression) node;
		final JdbcMappingContainer expressionType = expression.getExpressionType();
		return expressionType.getJdbcTypeCount() == 1
				&& expressionType.getSingleJdbcMapping().getJdbcType().isJson();
	}

	public static boolean isBoolean(CastType castType) {
		switch ( castType ) {
			case BOOLEAN:
			case TF_BOOLEAN:
			case YN_BOOLEAN:
			case INTEGER_BOOLEAN:
				return true;
			default:
				return false;
		}
	}

	public static boolean isNonNativeBoolean(CastType castType) {
		switch ( castType ) {
			case TF_BOOLEAN:
			case YN_BOOLEAN:
			case INTEGER_BOOLEAN:
				return true;
			default:
				return false;
		}
	}
}
