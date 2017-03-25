/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.predicate;

import java.io.Serializable;
import javax.persistence.criteria.Subquery;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.query.criteria.internal.Renderable;
import org.hibernate.query.criteria.internal.compile.RenderingContext;

/**
 * Models an <tt>EXISTS(<subquery>)</tt> predicate
 *
 * @author Steve Ebersole
 */
public class ExistsPredicate
		extends AbstractSimplePredicate
		implements Serializable {
	private final Subquery<?> subquery;

	public ExistsPredicate(CriteriaBuilderImpl criteriaBuilder, Subquery<?> subquery) {
		super( criteriaBuilder );
		this.subquery = subquery;
	}

	public Subquery<?> getSubquery() {
		return subquery;
	}

	@Override
	public void registerParameters(ParameterRegistry registry) {
		// nothing to do here
	}

	@Override
	public String render(boolean isNegated, RenderingContext renderingContext) {
		return ( isNegated ? "not " : "" ) + "exists "
				+ ( (Renderable) getSubquery() ).render( renderingContext );
	}
}
