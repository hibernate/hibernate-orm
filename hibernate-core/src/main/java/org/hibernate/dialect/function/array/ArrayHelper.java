/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.array;

import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.FunctionExpression;

public class ArrayHelper {
	public static boolean isNullable(Expression expression) {
		if ( expression instanceof FunctionExpression ) {
			final FunctionExpression functionExpression = (FunctionExpression) expression;
			// An array function literal is never null
			return !"array".equals( functionExpression.getFunctionName() );
		}
		return true;
	}
}
