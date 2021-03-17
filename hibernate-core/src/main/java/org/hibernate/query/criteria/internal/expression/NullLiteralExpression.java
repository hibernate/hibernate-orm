/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.expression;

import java.io.Serializable;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.query.criteria.internal.compile.RenderingContext;
import org.hibernate.query.criteria.internal.expression.function.CastFunction;
import org.hibernate.sql.ast.Clause;

/**
 * Represents a <tt>NULL</tt>literal expression.
 *
 * @author Steve Ebersole
 */
public class NullLiteralExpression<T> extends ExpressionImpl<T> implements Serializable {
	public NullLiteralExpression(CriteriaBuilderImpl criteriaBuilder, Class<T> type) {
		super( criteriaBuilder, type );
	}

	public void registerParameters(ParameterRegistry registry) {
		// nothing to do
	}

	public String render(RenderingContext renderingContext) {
		if ( renderingContext.getClauseStack().getCurrent() == Clause.SELECT ) {
			// in the select clause render the ``null` using a cast so the db analyzer/optimizer
			// understands the type
			return CastFunction.CAST_NAME + "( 	null  as " + renderingContext.getCastType( getJavaType() ) + ')';
		}

		// otherwise, just render `null`
		return "null";
	}
}
