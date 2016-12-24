/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.expression.instantiation;

import org.hibernate.sql.ast.expression.Expression;
import org.hibernate.sql.convert.results.spi.ReturnResolutionContext;
import org.hibernate.sql.exec.results.internal.instantiation.ArgumentReader;

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
