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
package org.hibernate.ejb.criteria.expression.function;

import javax.persistence.criteria.Expression;
import org.hibernate.ejb.criteria.ParameterContainer;
import org.hibernate.ejb.criteria.ParameterRegistry;
import org.hibernate.ejb.criteria.QueryBuilderImpl;
import org.hibernate.ejb.criteria.expression.LiteralExpression;

/**
 * Models the ANSI SQL <tt>SUBSTRING</tt> function.
 *
 * @author Steve Ebersole
 */
public class SubstringFunction extends BasicFunctionExpression<String> {
	public static final String NAME = "substring";

	private final Expression<String> value;
	private final Expression<Integer> start;
	private final Expression<Integer> length;

	public SubstringFunction(
			QueryBuilderImpl queryBuilder,
			Expression<String> value,
			Expression<Integer> start,
			Expression<Integer> length) {
		super( queryBuilder, String.class, NAME );
		this.value = value;
		this.start = start;
		this.length = length;
	}

	public SubstringFunction(
			QueryBuilderImpl queryBuilder,
			Expression<String> value, 
			Expression<Integer> start) {
		this( queryBuilder, value, start, (Expression<Integer>)null );
	}

	public SubstringFunction(
			QueryBuilderImpl queryBuilder,
			Expression<String> value,
			int start) {
		this( 
				queryBuilder,
				value,
				new LiteralExpression<Integer>( queryBuilder, start )
		);
	}

	public SubstringFunction(
			QueryBuilderImpl queryBuilder,
			Expression<String> value,
			int start,
			int length) {
		this(
				queryBuilder,
				value,
				new LiteralExpression<Integer>( queryBuilder, start ),
				new LiteralExpression<Integer>( queryBuilder, length )
		);
	}

	public Expression<Integer> getLength() {
		return length;
	}

	public Expression<Integer> getStart() {
		return start;
	}

	public Expression<String> getValue() {
		return value;
	}

	@Override
	public void registerParameters(ParameterRegistry registry) {
		Helper.possibleParameter( getLength(), registry );
		Helper.possibleParameter( getStart(), registry );
		Helper.possibleParameter( getValue(), registry );
	}

}
