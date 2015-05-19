/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.criteria.predicate;

import java.io.Serializable;
import java.util.Collection;

import org.hibernate.jpa.criteria.CriteriaBuilderImpl;
import org.hibernate.jpa.criteria.ParameterRegistry;
import org.hibernate.jpa.criteria.compile.RenderingContext;
import org.hibernate.jpa.criteria.expression.UnaryOperatorExpression;
import org.hibernate.jpa.criteria.path.PluralAttributePath;

/**
 * Models an <tt>IS [NOT] EMPTY</tt> restriction
 *
 * @author Steve Ebersole
 */
public class IsEmptyPredicate<C extends Collection>
		extends AbstractSimplePredicate
		implements UnaryOperatorExpression<Boolean>, Serializable {

	private final PluralAttributePath<C> collectionPath;

	public IsEmptyPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			PluralAttributePath<C> collectionPath) {
		super( criteriaBuilder );
		this.collectionPath = collectionPath;
	}

	@Override
	public PluralAttributePath<C> getOperand() {
		return collectionPath;
	}

	@Override
	public void registerParameters(ParameterRegistry registry) {
		// nothing to do
	}

	@Override
	public String render(boolean isNegated, RenderingContext renderingContext) {
		final String operator = isNegated ? " is not empty" : " is empty";
		return getOperand().render( renderingContext ) + operator;
	}
}
