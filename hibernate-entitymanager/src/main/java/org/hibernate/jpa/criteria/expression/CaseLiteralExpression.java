/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.criteria.expression;

import org.hibernate.jpa.criteria.CriteriaBuilderImpl;
import org.hibernate.jpa.criteria.compile.RenderingContext;
import org.hibernate.jpa.criteria.expression.function.CastFunction;

/**
 * @author Andrea Boriero
 */
public class CaseLiteralExpression<T> extends LiteralExpression<T> {

	public CaseLiteralExpression(CriteriaBuilderImpl criteriaBuilder, Class<T> type, T literal) {
		super( criteriaBuilder, type, literal );
	}

	@Override
	public String render(RenderingContext renderingContext) {
		// There's no need to cast a boolean value and it actually breaks on
		// MySQL and MariaDB because they don't support casting to bit.
		// Skip the cast for a boolean literal.
		if ( getJavaType() == Boolean.class && Boolean.class.isInstance( getLiteral() ) ) {
			return super.render( renderingContext );
		}

		// wrapping the result in a cast to determine the node type during the antlr hql parsing phase
		return CastFunction.CAST_NAME + '(' +
				super.render( renderingContext ) +
				" as " +
				renderingContext.getCastType( getJavaType() ) +
				')';
	}
}
