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
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class EntityTypeExpression<T> extends ExpressionImpl<T> implements Serializable {
	public EntityTypeExpression(CriteriaBuilderImpl criteriaBuilder, Class<T> javaType) {
		super( criteriaBuilder, javaType );
	}

	public void registerParameters(ParameterRegistry registry) {
		// nothign to do
	}

	public String render(RenderingContext renderingContext) {
		// todo : is it valid for this to get rendered into the query itself?
		throw new IllegalArgumentException( "Unexpected call on EntityTypeExpression#render" );
	}

	public String renderProjection(RenderingContext renderingContext) {
		return render( renderingContext );
	}
}
