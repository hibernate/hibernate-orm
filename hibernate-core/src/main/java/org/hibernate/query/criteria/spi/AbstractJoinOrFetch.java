/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;

import org.hibernate.metamodel.model.domain.spi.PersistentAttributeDescriptor;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.PathException;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractJoinOrFetch<O,T>
		extends AbstractFrom<O,T>
		implements JoinImplementor<O,T>, FetchImplementor<O,T> {

	private final JoinType joinType;

	private Predicate suppliedJoinCondition;

	public AbstractJoinOrFetch(
			PersistentAttributeDescriptor<O,T> navigable,
			PathSourceImplementor<O> pathSource,
			JoinType joinType,
			CriteriaNodeBuilder criteriaBuilder) {
		super( navigable, pathSource, criteriaBuilder );
		this.joinType = joinType;
	}

	@Override
	public PersistentAttributeDescriptor<? super O, ?> getAttribute() {
		return (PersistentAttributeDescriptor<? super O, ?>) getNavigable();
	}

	@Override
	public JoinType getJoinType() {
		return joinType;
	}

	@Override
	public Predicate getOn() {
		return suppliedJoinCondition;
	}

	protected void verifyRestriction() {
		// nothing to do.  here mainly for JpaCompliance checking wrt restricting fetches
	}

	protected void applyJoinRestriction(JpaExpression<Boolean> restriction) {
		verifyRestriction();
		this.suppliedJoinCondition = nodeBuilder().wrap( restriction );
	}

	protected void applyJoinRestriction(JpaPredicate... restrictions) {
		verifyRestriction();

		// no matter what, a call to this method replaces any previously set values...
		this.suppliedJoinCondition = null;

		if ( restrictions != null && restrictions.length > 0 ) {
			this.suppliedJoinCondition = nodeBuilder().and( restrictions );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <S extends T> AbstractJoinOrFetch<O,S> treatAs(Class<S> treatJavaType) throws PathException {
		return (AbstractJoinOrFetch) super.treatAs( treatJavaType );
	}
}
