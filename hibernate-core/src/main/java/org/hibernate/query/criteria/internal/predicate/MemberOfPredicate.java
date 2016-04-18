/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.predicate;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.criteria.Expression;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.query.criteria.internal.Renderable;
import org.hibernate.query.criteria.internal.compile.RenderingContext;
import org.hibernate.query.criteria.internal.expression.LiteralExpression;
import org.hibernate.query.criteria.internal.path.PluralAttributePath;

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
