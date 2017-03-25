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
import org.hibernate.query.criteria.internal.path.AbstractPathImpl;

/**
 * Used to construct the result of {@link javax.persistence.criteria.Path#type()}
 *
 * @author Steve Ebersole
 */
public class PathTypeExpression<T> extends ExpressionImpl<T> implements Serializable {
	private final AbstractPathImpl<T> pathImpl;

	public PathTypeExpression(CriteriaBuilderImpl criteriaBuilder, Class<T> javaType, AbstractPathImpl<T> pathImpl) {
		super( criteriaBuilder, javaType );
		this.pathImpl = pathImpl;
	}

	public void registerParameters(ParameterRegistry registry) {
		// nothing to do
	}

	public String render(RenderingContext renderingContext) {
		return "type(" + pathImpl.getPathIdentifier() + ")";
	}

	public String renderProjection(RenderingContext renderingContext) {
		return render( renderingContext );
	}
}
