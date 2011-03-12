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
package org.hibernate.ejb.criteria.predicate;

import java.io.Serializable;
import java.util.Collection;
import org.hibernate.ejb.criteria.ParameterRegistry;
import org.hibernate.ejb.criteria.CriteriaBuilderImpl;
import org.hibernate.ejb.criteria.CriteriaQueryCompiler;
import org.hibernate.ejb.criteria.expression.UnaryOperatorExpression;
import org.hibernate.ejb.criteria.path.PluralAttributePath;

/**
 * Models an <tt>IS [NOT] EMPTY</tt> restriction
 *
 * @author Steve Ebersole
 */
public class IsEmptyPredicate<C extends Collection>
		extends AbstractSimplePredicate
		implements UnaryOperatorExpression<Boolean>, Serializable {

	private final PluralAttributePath<C> collectionPath;

	public IsEmptyPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			PluralAttributePath<C> collectionPath) {
		super( criteriaBuilder );
		this.collectionPath = collectionPath;
	}

	public PluralAttributePath<C> getOperand() {
		return collectionPath;
	}

	public void registerParameters(ParameterRegistry registry) {
		// nothing to do
	}

	public String render(CriteriaQueryCompiler.RenderingContext renderingContext) {
		final String operator = isNegated() ? " is not empty" : " is empty";
		return getOperand().render( renderingContext ) + operator;
	}

	public String renderProjection(CriteriaQueryCompiler.RenderingContext renderingContext) {
		return render( renderingContext );
	}
}
