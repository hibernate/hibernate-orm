/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ejb.criteria.predicate;

import java.io.Serializable;
import javax.persistence.criteria.Expression;

import org.hibernate.ejb.criteria.ParameterRegistry;
import org.hibernate.ejb.criteria.CriteriaBuilderImpl;
import org.hibernate.ejb.criteria.CriteriaQueryCompiler;
import org.hibernate.ejb.criteria.Renderable;

/**
 * Models a <tt>BETWEEN</tt> {@link javax.persistence.criteria.Predicate}.
 *
 * @author Steve Ebersole
 */
public class BetweenPredicate<Y>
		extends AbstractSimplePredicate
		implements Serializable {
	private final Expression<? extends Y> expression;
	private final Expression<? extends Y> lowerBound;
	private final Expression<? extends Y> upperBound;

	public BetweenPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<? extends Y> expression,
			Y lowerBound,
			Y upperBound) {
		this(
				criteriaBuilder,
				expression,
				criteriaBuilder.literal( lowerBound ),
				criteriaBuilder.literal( upperBound )
		);
	}

	public BetweenPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<? extends Y> expression,
			Expression<? extends Y> lowerBound,
			Expression<? extends Y> upperBound) {
		super( criteriaBuilder );
		this.expression = expression;
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}

	public Expression<? extends Y> getExpression() {
		return expression;
	}

	public Expression<? extends Y> getLowerBound() {
		return lowerBound;
	}

	public Expression<? extends Y> getUpperBound() {
		return upperBound;
	}

	public void registerParameters(ParameterRegistry registry) {
		Helper.possibleParameter( getExpression(), registry );
		Helper.possibleParameter( getLowerBound(), registry );
		Helper.possibleParameter( getUpperBound(), registry );
	}

	public String render(CriteriaQueryCompiler.RenderingContext renderingContext) {
		final String operator = isNegated() ? " not between " : " between ";
		return ( (Renderable) getExpression() ).render( renderingContext )
				+ operator
				+ ( (Renderable) getLowerBound() ).render( renderingContext )
				+ " and "
				+ ( (Renderable) getUpperBound() ).render( renderingContext );
	}

	public String renderProjection(CriteriaQueryCompiler.RenderingContext renderingContext) {
		return render( renderingContext );
	}
}
