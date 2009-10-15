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
package org.hibernate.ejb.criteria.expression;

import javax.persistence.criteria.Subquery;
import org.hibernate.ejb.criteria.ParameterRegistry;
import org.hibernate.ejb.criteria.QueryBuilderImpl;

/**
 * Represents a {@link Modifier#ALL}, {@link Modifier#ANY}, {@link Modifier#SOME} modifier appplied to a subquery as
 * part of a comparison.
 *
 * @author Steve Ebersole
 */
public class SubqueryComparisonModifierExpression<Y> extends ExpressionImpl<Y> {
	public static enum Modifier { ALL, SOME, ANY }

	private final Subquery<Y> subquery;
	private final Modifier modifier;

	public SubqueryComparisonModifierExpression(
			QueryBuilderImpl queryBuilder,
			Class<Y> javaType,
			Subquery<Y> subquery,
			Modifier modifier) {
		super(queryBuilder, javaType);
		this.subquery = subquery;
		this.modifier = modifier;
	}

	public Modifier getModifier() {
		return modifier;
	}

	public Subquery<Y> getSubquery() {
		return subquery;
	}

	public void registerParameters(ParameterRegistry registry) {
		// nothign to do (the subquery should be handled directly, and the modified itself is not parameterized)
	}

}
