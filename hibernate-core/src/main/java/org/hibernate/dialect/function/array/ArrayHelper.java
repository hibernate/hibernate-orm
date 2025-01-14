/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.FunctionExpression;

public class ArrayHelper {
	public static boolean isNullable(Expression expression) {
		if ( expression instanceof FunctionExpression functionExpression ) {
			// An array function literal is never null
			return !"array".equals( functionExpression.getFunctionName() );
		}
		return true;
	}
}
