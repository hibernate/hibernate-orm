package org.hibernate.ejb.criteria.expression.function;

import javax.persistence.criteria.Expression;
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


}
