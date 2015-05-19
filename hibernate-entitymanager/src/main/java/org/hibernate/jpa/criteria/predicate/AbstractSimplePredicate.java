/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.criteria.predicate;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import javax.persistence.criteria.Expression;

import org.hibernate.jpa.criteria.CriteriaBuilderImpl;
import org.hibernate.jpa.criteria.compile.RenderingContext;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSimplePredicate
		extends AbstractPredicateImpl 
		implements Serializable {
	private static final List<Expression<Boolean>> NO_EXPRESSIONS = Collections.emptyList();

	public AbstractSimplePredicate(CriteriaBuilderImpl criteriaBuilder) {
		super( criteriaBuilder );
	}

	@Override
	public boolean isJunction() {
		return false;
	}

	@Override
	public BooleanOperator getOperator() {
		return BooleanOperator.AND;
	}

	@Override
	public final List<Expression<Boolean>> getExpressions() {
		return NO_EXPRESSIONS;
	}

	@Override
	public String render(RenderingContext renderingContext) {
		return render( isNegated(), renderingContext );
	}

	@Override
	public String renderProjection(RenderingContext renderingContext) {
		return render( renderingContext );
	}

}
