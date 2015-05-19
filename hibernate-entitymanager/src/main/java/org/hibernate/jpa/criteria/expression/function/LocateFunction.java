/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
