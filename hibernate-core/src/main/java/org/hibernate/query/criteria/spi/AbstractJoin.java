/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;

import org.hibernate.metamodel.model.domain.spi.PersistentAttributeDescriptor;
import org.hibernate.query.criteria.JpaPredicate;

/**
 * Base class for {@link JoinImplementor} implementations.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractJoin<O, T> extends AbstractJoinOrFetch<O, T> implements JoinImplementor<O, T> {
	private final JoinType joinType;

	private JpaPredicate suppliedJoinCondition;

	public AbstractJoin(
			PathSourceImplementor<O> pathSource,
			PersistentAttributeDescriptor<O, T> joinAttribute,
			JoinType joinType,
			CriteriaNodeBuilder criteriaBuilder) {
		super(
				joinAttribute,
				pathSource,
				joinType,
				criteriaBuilder
		);
		this.joinType = joinType;
	}

	@Override
	@SuppressWarnings("unchecked")
	public PersistentAttributeDescriptor<? super O, T> getNavigable() {
		return (PersistentAttributeDescriptor) super.getNavigable();
	}

	@Override
	public JoinType getJoinType() {
		return joinType;
	}

	@Override
	public AbstractJoin<O, T> on(Predicate... restrictions) {
		// no matter what, a call to this method replaces any previously set values...
		this.suppliedJoinCondition = null;

		if ( restrictions != null && restrictions.length > 0 ) {
			this.suppliedJoinCondition = nodeBuilder().and( restrictions );
		}

		return this;
	}

	@Override
	public AbstractJoin<O, T> on(Expression<Boolean> restriction) {
		this.suppliedJoinCondition = nodeBuilder().wrap( restriction );
		return this;
	}

	@Override
	public Predicate getOn() {
		return suppliedJoinCondition;
	}
}
