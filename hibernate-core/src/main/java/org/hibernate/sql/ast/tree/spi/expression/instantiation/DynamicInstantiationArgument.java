/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.spi.expression.instantiation;

import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.produce.result.spi.ReturnResolutionContext;
import org.hibernate.sql.ast.consume.results.internal.instantiation.ArgumentReader;

/**
 * @author Steve Ebersole
 */
public class DynamicInstantiationArgument {
	private final Expression expression;
	private final String alias;

	public DynamicInstantiationArgument(Expression expression, String alias) {
		this.expression = expression;
		this.alias = alias;
	}

	public Expression getExpression() {
		return expression;
	}

	public String getAlias() {
		return alias;
	}

	public ArgumentReader buildArgumentReader(ReturnResolutionContext resolutionContext) {
		return new ArgumentReader(
				expression.getSelectable().toQueryReturn( resolutionContext, alias ).getReturnAssembler(),
				alias
		);
	}
}
