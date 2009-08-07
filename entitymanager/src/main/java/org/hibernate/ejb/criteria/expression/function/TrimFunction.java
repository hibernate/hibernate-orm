package org.hibernate.ejb.criteria.expression.function;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.QueryBuilder.Trimspec;
import org.hibernate.ejb.criteria.QueryBuilderImpl;
import org.hibernate.ejb.criteria.expression.LiteralExpression;

/**
 * Models the ANSI SQL <tt>TRIM</tt> function.
 *
 * @author Steve Ebersole
 */
public class TrimFunction extends BasicFunctionExpression<String> {
	public static final String NAME = "trim";
	public static final Trimspec DEFAULT_TRIMSPEC = Trimspec.BOTH;
	public static final char DEFAULT_TRIM_CHAR = ' ';

	private final Trimspec trimspec;
	private final Expression<Character> trimCharacter;
	private final Expression<String> trimSource;

	public TrimFunction(
			QueryBuilderImpl queryBuilder,
			Trimspec trimspec,
			Expression<Character> trimCharacter,
			Expression<String> trimSource) {
		super( queryBuilder, String.class, NAME );
		this.trimspec = trimspec;
		this.trimCharacter = trimCharacter;
		this.trimSource = trimSource;
	}

	public TrimFunction(
			QueryBuilderImpl queryBuilder,
			Trimspec trimspec,
			char trimCharacter,
			Expression<String> trimSource) {
		super( queryBuilder, String.class, NAME );
		this.trimspec = trimspec;
		this.trimCharacter = new LiteralExpression<Character>( queryBuilder, trimCharacter );
		this.trimSource = trimSource;
	}

	public TrimFunction(
			QueryBuilderImpl queryBuilder,
			Expression<String> trimSource) {
		this( queryBuilder, DEFAULT_TRIMSPEC, DEFAULT_TRIM_CHAR, trimSource );
	}

	public TrimFunction(
			QueryBuilderImpl queryBuilder,
			Expression<Character> trimCharacter,
			Expression<String> trimSource) {
		this( queryBuilder, DEFAULT_TRIMSPEC, trimCharacter, trimSource );
	}

	public TrimFunction(
			QueryBuilderImpl queryBuilder,
			char trimCharacter,
			Expression<String> trimSource) {
		this( queryBuilder, DEFAULT_TRIMSPEC, trimCharacter, trimSource );
	}

	public TrimFunction(
			QueryBuilderImpl queryBuilder,
			Trimspec trimspec,
			Expression<String> trimSource) {
		this( queryBuilder, trimspec, DEFAULT_TRIM_CHAR, trimSource );
	}

	public Expression<Character> getTrimCharacter() {
		return trimCharacter;
	}

	public Expression<String> getTrimSource() {
		return trimSource;
	}

	public Trimspec getTrimspec() {
		return trimspec;
	}

}
