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
package org.hibernate.ejb.criteria.expression;

import javax.persistence.criteria.Expression;
import org.hibernate.ejb.criteria.ParameterContainer;
import org.hibernate.ejb.criteria.ParameterRegistry;
import org.hibernate.ejb.criteria.QueryBuilderImpl;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class ConcatExpression extends ExpressionImpl<String> {
	private Expression<String> string1;
	private Expression<String> string2;

	public ConcatExpression(
			QueryBuilderImpl queryBuilder,
			Expression<String> expression1,
			Expression<String> expression2) {
		super( queryBuilder, String.class );
		this.string1 = expression1;
		this.string2 = expression2;
	}

	public ConcatExpression(
			QueryBuilderImpl queryBuilder, 
			Expression<String> string1, 
			String string2) {
		this( queryBuilder, string1, wrap(queryBuilder, string2) );
	}

	private static Expression<String> wrap(QueryBuilderImpl queryBuilder, String string) {
		return new LiteralExpression<String>( queryBuilder, string );
	}

	public ConcatExpression(
			QueryBuilderImpl queryBuilder,
			String string1,
			Expression<String> string2) {
		this( queryBuilder, wrap(queryBuilder, string1), string2 );
	}

	public Expression<String> getString1() {
		return string1;
	}

	public Expression<String> getString2() {
		return string2;
	}

	public void registerParameters(ParameterRegistry registry) {
		Helper.possibleParameter( getString1(), registry );
		Helper.possibleParameter( getString2(), registry );
	}

}
