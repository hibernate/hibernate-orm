/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import javax.persistence.criteria.CriteriaBuilder.Trimspec;

/**
 * Models the ANSI SQL <tt>TRIM</tt> function.
 *
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public class TrimFunction extends AbstractStandardFunction<String> {
	public static final String NAME = "trim";

	public static final Trimspec DEFAULT_TRIMSPEC = Trimspec.BOTH;
	public static final char DEFAULT_TRIM_CHAR = ' ';

	private final Trimspec trimspec;
	private final ExpressionImplementor<Character> trimCharacter;
	private final ExpressionImplementor<String> trimSource;

	public TrimFunction(
			Trimspec trimspec,
			ExpressionImplementor<Character> trimCharacter,
			ExpressionImplementor<String> trimSource,
			CriteriaNodeBuilder criteriaBuilder) {
		super( NAME, String.class, criteriaBuilder );

		this.trimspec = trimspec;
		this.trimCharacter = trimCharacter;
		this.trimSource = trimSource;
	}

	public ExpressionImplementor<Character> getTrimCharacter() {
		return trimCharacter;
	}

	public ExpressionImplementor<String> getTrimSource() {
		return trimSource;
	}

	public Trimspec getTrimspec() {
		return trimspec;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <R> R accept(CriteriaVisitor visitor) {
		return (R) visitor.visitTrimFunction( this );
	}
}
