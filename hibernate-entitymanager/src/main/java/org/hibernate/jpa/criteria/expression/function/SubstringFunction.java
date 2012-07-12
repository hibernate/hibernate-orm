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
package org.hibernate.jpa.criteria.expression.function;

import java.io.Serializable;
import javax.persistence.criteria.Expression;

import org.hibernate.jpa.criteria.CriteriaBuilderImpl;
import org.hibernate.jpa.criteria.ParameterRegistry;
import org.hibernate.jpa.criteria.Renderable;
import org.hibernate.jpa.criteria.compile.RenderingContext;
import org.hibernate.jpa.criteria.expression.LiteralExpression;

/**
 * Models the ANSI SQL <tt>SUBSTRING</tt> function.
 *
 * @author Steve Ebersole
 */
public class SubstringFunction
		extends BasicFunctionExpression<String>
		implements Serializable {
	public static final String NAME = "substring";

	private final Expression<String> value;
	private final Expression<Integer> start;
	private final Expression<Integer> length;

	public SubstringFunction(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<String> value,
			Expression<Integer> start,
			Expression<Integer> length) {
		super( criteriaBuilder, String.class, NAME );
		this.value = value;
		this.start = start;
		this.length = length;
	}

	@SuppressWarnings({ "RedundantCast" })
	public SubstringFunction(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<String> value, 
			Expression<Integer> start) {
		this( criteriaBuilder, value, start, (Expression<Integer>)null );
	}

	public SubstringFunction(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<String> value,
			int start) {
		this(
				criteriaBuilder,
				value,
				new LiteralExpression<Integer>( criteriaBuilder, start )
		);
	}

	public SubstringFunction(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<String> value,
			int start,
			int length) {
		this(
				criteriaBuilder,
				value,
				new LiteralExpression<Integer>( criteriaBuilder, start ),
				new LiteralExpression<Integer>( criteriaBuilder, length )
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

	public String render(RenderingContext renderingContext) {
		StringBuilder buffer = new StringBuilder();
		buffer.append( "substring(" )
				.append( ( (Renderable) getValue() ).render( renderingContext ) )
				.append( ',' )
				.append( ( (Renderable) getStart() ).render( renderingContext ) );
		if ( getLength() != null ) {
			buffer.append( ',' )
					.append( ( (Renderable) getLength() ).render( renderingContext ) );
		}
		buffer.append( ')' );
		return buffer.toString();
	}
}
