/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.ast.expression.instantiation;

import org.hibernate.sql.sqm.ast.expression.Expression;

/**
 * @author Steve Ebersole
 */
public class DynamicInstantiationArgument {
	private final String alias;
	private final Expression expression;

	public DynamicInstantiationArgument(String alias, Expression expression) {
		this.alias = alias;
		this.expression = expression;
	}

	public String getAlias() {
		return alias;
	}

	public Expression getExpression() {
		return expression;
	}
}
