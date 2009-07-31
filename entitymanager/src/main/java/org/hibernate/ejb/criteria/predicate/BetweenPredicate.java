/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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

import javax.persistence.criteria.Expression;

import org.hibernate.ejb.criteria.QueryBuilderImpl;

/**
 * Models a <tt>BETWEEN</tt> {@link javax.persistence.criteria.Predicate}.
 *
 * @author Steve Ebersole
 */
public class BetweenPredicate<Y> extends AbstractSimplePredicate {
	private final Expression<? extends Y> expression;
	private final Expression<? extends Y> lowerBound;
	private final Expression<? extends Y> upperBound;

	public BetweenPredicate(
			QueryBuilderImpl queryBuilder,
			Expression<? extends Y> expression,
			Y lowerBound,
			Y upperBound) {
		this(
				queryBuilder,
				expression,
				queryBuilder.literal( lowerBound ),
				queryBuilder.literal( upperBound )
		);
	}

	public BetweenPredicate(
			QueryBuilderImpl queryBuilder,
			Expression<? extends Y> expression,
			Expression<? extends Y> lowerBound,
			Expression<? extends Y> upperBound) {
		super( queryBuilder );
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
}
