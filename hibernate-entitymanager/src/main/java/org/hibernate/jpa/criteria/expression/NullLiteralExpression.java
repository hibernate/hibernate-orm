/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.criteria.expression;

import java.io.Serializable;

import org.hibernate.jpa.criteria.CriteriaBuilderImpl;
import org.hibernate.jpa.criteria.ParameterRegistry;
import org.hibernate.jpa.criteria.compile.RenderingContext;

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
		return "null";
	}

	public String renderProjection(RenderingContext renderingContext) {
		return render( renderingContext );
	}
}
