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
package org.hibernate.jpa.criteria.predicate;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.criteria.Expression;

import org.hibernate.jpa.criteria.CriteriaBuilderImpl;
import org.hibernate.jpa.criteria.ParameterRegistry;
import org.hibernate.jpa.criteria.Renderable;
import org.hibernate.jpa.criteria.compile.RenderingContext;
import org.hibernate.jpa.criteria.expression.LiteralExpression;
import org.hibernate.jpa.criteria.path.PluralAttributePath;

/**
 * Models an <tt>[NOT] MEMBER OF</tt> restriction
 *
 * @author Steve Ebersole
 */
public class MemberOfPredicate<E, C extends Collection<E>>
		extends AbstractSimplePredicate
		implements Serializable {

	private final Expression<E> elementExpression;
	private final PluralAttributePath<C> collectionPath;

	public MemberOfPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<E> elementExpression,
			PluralAttributePath<C> collectionPath) {
		super( criteriaBuilder );
		this.elementExpression = elementExpression;
		this.collectionPath = collectionPath;
	}

	public MemberOfPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			E element,
			PluralAttributePath<C> collectionPath) {
		this(
				criteriaBuilder,
				new LiteralExpression<E>( criteriaBuilder, element ),
				collectionPath
		);
	}

	public PluralAttributePath<C> getCollectionPath() {
		return collectionPath;
	}

	public Expression<E> getElementExpression() {
		return elementExpression;
	}

	@Override
	public void registerParameters(ParameterRegistry registry) {
		Helper.possibleParameter( getCollectionPath(), registry );
		Helper.possibleParameter( getElementExpression(), registry );
	}

	@Override
	public String render(boolean isNegated, RenderingContext renderingContext) {
		return ( (Renderable) elementExpression ).render( renderingContext )
				+ ( isNegated ? " not" : "" ) + " member of "
				+ getCollectionPath().render( renderingContext );
	}
}
