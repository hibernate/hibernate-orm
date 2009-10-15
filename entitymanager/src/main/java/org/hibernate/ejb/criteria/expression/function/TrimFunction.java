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
import javax.persistence.criteria.CriteriaBuilder.Trimspec;
import org.hibernate.ejb.criteria.ParameterRegistry;
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

	@Override
	public void registerParameters(ParameterRegistry registry) {
		Helper.possibleParameter( getTrimCharacter(), registry );
		Helper.possibleParameter( getTrimSource(), registry );
	}

}
