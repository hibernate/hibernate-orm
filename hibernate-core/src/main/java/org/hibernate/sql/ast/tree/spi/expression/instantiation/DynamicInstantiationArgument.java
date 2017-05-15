/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.instantiation;

import org.hibernate.sql.ast.consume.results.internal.instantiation.ArgumentReader;
import org.hibernate.sql.ast.consume.results.spi.QueryResultAssembler;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.result.spi.SqlSelectionResolver;
import org.hibernate.sql.ast.tree.spi.expression.Expression;

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

	public ArgumentReader buildArgumentReader(
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext resolutionContext) {
		final QueryResultAssembler queryResultAssembler = expression.getSelectable()
				.createSelection( expression, alias )
				.createQueryResult( sqlSelectionResolver, resolutionContext )
				.getResultAssembler();

		return new ArgumentReader( queryResultAssembler, alias );
	}
}
