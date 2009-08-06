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
package org.hibernate.ejb.criteria.expression.function;

import javax.persistence.criteria.Expression;

import org.hibernate.ejb.criteria.QueryBuilderImpl;

/**
 * Implementation of a <tt>AVG</tt> function providing convenience in construction.
 * <p/>
 * Parameterized as {@link Double} because thats what JPA states that the return
 * from <tt>AVG</tt> should be.
 *
 * @author Steve Ebersole
 */
public class AverageAggregrateFunction extends BasicFunctionExpression<Double> {
	public AverageAggregrateFunction(
			QueryBuilderImpl queryBuilder,
			Expression<? extends Number> expression) {
		super( queryBuilder, Double.class, "avg", expression );
	}
}
