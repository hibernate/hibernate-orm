/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.criteria.expression;

import java.io.Serializable;
import java.util.Collection;

import org.hibernate.jpa.criteria.CriteriaBuilderImpl;
import org.hibernate.jpa.criteria.ParameterRegistry;
import org.hibernate.jpa.criteria.compile.RenderingContext;
import org.hibernate.jpa.criteria.path.PluralAttributePath;

/**
 * Represents a "size of" expression in regards to a persistent collection; the implication is
 * that of a subquery.
 *
 * @author Steve Ebersole
 */
public class SizeOfCollectionExpression<C extends Collection>
		extends ExpressionImpl<Integer>
		implements Serializable {
	private final PluralAttributePath<C> collectionPath;

	public SizeOfCollectionExpression(
			CriteriaBuilderImpl criteriaBuilder,
			PluralAttributePath<C> collectionPath) {
		super( criteriaBuilder, Integer.class);
		this.collectionPath = collectionPath;
	}

	public PluralAttributePath<C> getCollectionPath() {
		return collectionPath;
	}

	public void registerParameters(ParameterRegistry registry) {
		// nothing to do
	}

	public String render(RenderingContext renderingContext) {
		return "size(" + getCollectionPath().render( renderingContext ) + ")";
	}

	public String renderProjection(RenderingContext renderingContext) {
		return render( renderingContext );
	}
}
