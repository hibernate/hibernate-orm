/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import javax.persistence.criteria.PluralJoin;

import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.query.criteria.JpaJoin;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmJoinable;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmPluralJoin<O,C,E> extends AbstractSqmAttributeJoin<O,E> implements JpaJoin<O,E>, PluralJoin<O,C,E> {

	@SuppressWarnings("WeakerAccess")
	public AbstractSqmPluralJoin(
			SqmFrom<?, O> lhs,
			PluralPersistentAttribute<O,C,E> joinedNavigable,
			String alias,
			SqmJoinType joinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		//noinspection unchecked
		super(
				lhs,
				(SqmJoinable<O, E>) joinedNavigable,
				alias,
				joinType,
				fetched,
				nodeBuilder
		);
	}

	@Override
	public PluralPersistentAttribute<O,C,E> getReferencedPathSource() {
		return (PluralPersistentAttribute<O, C, E>) super.getReferencedPathSource();
	}

	@Override
	public PluralPersistentAttribute<O,C,E> getModel() {
		return getReferencedPathSource();
	}
}
