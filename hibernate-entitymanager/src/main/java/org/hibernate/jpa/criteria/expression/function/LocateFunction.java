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
 * Models the ANSI SQL <tt>LOCATE</tt> function.
 *
 * @author Steve Ebersole
 */
public class LocateFunction
		extends BasicFunctionExpression<Integer>
		implements Serializable {
	public static final String NAME = "locate";

	private final Expression<String> pattern;
	private final Expression<String> string;
	private final Expression<Integer> start;

	public LocateFunction(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<String> pattern,
			Expression<String> string,
			Expression<Integer> start) {
		super( criteriaBuilder, Integer.class, NAME );
		this.pattern = pattern;
		this.string = string;
		this.start = start;
	}

	public LocateFunction(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<String> pattern,
			Expression<String> string) {
		this( criteriaBuilder, pattern, string, null );
	}

	public LocateFunction(CriteriaBuilderImpl criteriaBuilder, String pattern, Expression<String> string) {
		this(
				criteriaBuilder,
				new LiteralExpression<String>( criteriaBuilder, pattern ),
				string,
				null
		);
	}

	public LocateFunction(CriteriaBuilderImpl criteriaBuilder, String pattern, Expression<String> string, int start) {
		this(
				criteriaBuilder,
				new LiteralExpression<String>( criteriaBuilder, pattern ),
				string,
				new LiteralExpression<Integer>( criteriaBuilder, start )
		);
	}

	public Expression<String> getPattern() {
		return pattern;
	}

	public Expression<Integer> getStart() {
		return start;
	}

	public Expression<String> getString() {
		return string;
	}

	@Override
	public void registerParameters(ParameterRegistry registry) {
		Helper.possibleParameter( getPattern(), registry );
		Helper.possibleParameter( getStart(), registry );
		Helper.possibleParameter( getString(), registry );
	}

	@Override
	public String render(RenderingContext renderingContext) {
		StringBuilder buffer = new StringBuilder();
		buffer.append( "locate(" )
				.append( ( (Renderable) getPattern() ).render( renderingContext ) )
				.append( ',' )
				.append( ( (Renderable) getString() ).render( renderingContext ) );
		if ( getStart() != null ) {
			buffer.append( ',' )
					.append( ( (Renderable) getStart() ).render( renderingContext ) );
		}
		buffer.append( ')' );
		return buffer.toString();
	}
}
