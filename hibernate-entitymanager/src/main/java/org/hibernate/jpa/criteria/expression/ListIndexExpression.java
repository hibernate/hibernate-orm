/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.criteria.expression;

import java.io.Serializable;
import javax.persistence.metamodel.ListAttribute;

import org.hibernate.jpa.criteria.CriteriaBuilderImpl;
import org.hibernate.jpa.criteria.ParameterRegistry;
import org.hibernate.jpa.criteria.PathImplementor;
import org.hibernate.jpa.criteria.compile.RenderingContext;


/**
 * An expression for referring to the index of a list.
 *
 * @author Steve Ebersole
 */
public class ListIndexExpression extends ExpressionImpl<Integer> implements Serializable {
	private final PathImplementor origin;
	private final ListAttribute listAttribute;

	public ListIndexExpression(
			CriteriaBuilderImpl criteriaBuilder,
			PathImplementor origin,
			ListAttribute listAttribute) {
		super( criteriaBuilder, Integer.class );
		this.origin = origin;
		this.listAttribute = listAttribute;
	}

	public ListAttribute getListAttribute() {
		return listAttribute;
	}

	public void registerParameters(ParameterRegistry registry) {
		// nothing to do
	}

	public String render(RenderingContext renderingContext) {
		return "index("
				+ origin.getPathIdentifier()
				+ ")";
	}

	public String renderProjection(RenderingContext renderingContext) {
		return render( renderingContext );
	}
}
