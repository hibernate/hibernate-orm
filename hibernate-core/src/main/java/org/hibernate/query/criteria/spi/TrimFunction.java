/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.io.Serializable;
import javax.persistence.criteria.CriteriaBuilder.Trimspec;

import org.hibernate.query.criteria.JpaExpression;

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
	private final JpaExpression<Character> trimCharacter;
	private final JpaExpression<String> trimSource;

	public TrimFunction(
			Trimspec trimspec,
			JpaExpression<Character> trimCharacter,
			JpaExpression<String> trimSource,
			CriteriaNodeBuilder criteriaBuilder) {
		super( NAME, String.class, criteriaBuilder );

		this.trimspec = trimspec;
		this.trimCharacter = trimCharacter;
		this.trimSource = trimSource;
	}

	public TrimFunction(
			Trimspec trimspec,
			char trimCharacter,
			JpaExpression<String> trimSource,
			CriteriaNodeBuilder criteriaBuilder) {
		super( NAME, String.class, criteriaBuilder );

		this.trimspec = trimspec;
		this.trimCharacter = criteriaBuilder.literal( trimCharacter );
		this.trimSource = trimSource;
	}

	public TrimFunction(
			JpaExpression<String> trimSource,
			CriteriaNodeBuilder criteriaBuilder) {
		this( DEFAULT_TRIMSPEC, DEFAULT_TRIM_CHAR, trimSource, criteriaBuilder );
	}

	public TrimFunction(
			JpaExpression<Character> trimCharacter,
			JpaExpression<String> trimSource,
			CriteriaNodeBuilder criteriaBuilder) {
		this( DEFAULT_TRIMSPEC, trimCharacter, trimSource, criteriaBuilder );
	}

	public TrimFunction(
			char trimCharacter,
			JpaExpression<String> trimSource,
			CriteriaNodeBuilder criteriaBuilder) {
		this( DEFAULT_TRIMSPEC, trimCharacter, trimSource, criteriaBuilder );
	}

	public TrimFunction(
			Trimspec trimspec,
			JpaExpression<String> trimSource,
			CriteriaNodeBuilder criteriaBuilder) {
		this( trimspec, DEFAULT_TRIM_CHAR, trimSource, criteriaBuilder );
	}

	public JpaExpression<Character> getTrimCharacter() {
		return trimCharacter;
	}

	public JpaExpression<String> getTrimSource() {
		return trimSource;
	}

	public Trimspec getTrimspec() {
		return trimspec;
	}

	@Override
	public <R> R accept(JpaCriteriaVisitor visitor) {
		return visitor.visitTrimFunction( this );
	}
}
