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

import javax.persistence.criteria.ParameterExpression;

import org.hibernate.ejb.criteria.ParameterRegistry;
import org.hibernate.ejb.criteria.QueryBuilderImpl;

/**
 * Defines a parameter specification, or the information about a parameter (where it occurs, what is
 * its type, etc).
 *
 * @author Steve Ebersole
 */
public class ParameterExpressionImpl<T> extends ExpressionImpl<T> implements ParameterExpression<T> {
	private final String name;
	private final Integer position;

	public ParameterExpressionImpl(
			QueryBuilderImpl queryBuilder,
			Class<T> javaType,
			String name) {
		super( queryBuilder, javaType );
		this.name = name;
		this.position = null;
	}

	public ParameterExpressionImpl(
			QueryBuilderImpl queryBuilder,
			Class<T> javaType,
			Integer position) {
		super( queryBuilder, javaType );
		this.name = null;
		this.position = position;
	}

	public ParameterExpressionImpl(
			QueryBuilderImpl queryBuilder,
			Class<T> javaType) {
		super( queryBuilder, javaType );
		this.name = null;
		this.position = null;
	}

	public String getName() {
		return name;
	}

	public Integer getPosition() {
		return position;
	}

	public void registerParameters(ParameterRegistry registry) {
		registry.registerParameter( this );
	}

}
