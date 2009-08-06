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

import org.hibernate.ejb.criteria.QueryBuilderImpl;
import org.hibernate.ejb.criteria.expression.ExpressionImpl;

/**
 * Models a <tt>CAST</tt> function.
 *
 * @param <T> The cast result type.
 * @param <Y> The type of the expression to be cast.
 *
 * @author Steve Ebersole
 */
public class CastFunction<T,Y> extends ExpressionImpl<T> implements FunctionExpression<T> {
	public static final String CAST_NAME = "cast";

	private final ExpressionImpl<Y> castSource;

	public CastFunction(
			QueryBuilderImpl queryBuilder,
			Class<T> javaType,
			ExpressionImpl<Y> castSource) {
		super( queryBuilder, javaType );
		this.castSource = castSource;
	}

	public String getFunctionName() {
		return CAST_NAME;
	}

	public boolean isAggregation() {
		return false;
	}

	public ExpressionImpl<Y> getCastSource() {
		return castSource;
	}

}
