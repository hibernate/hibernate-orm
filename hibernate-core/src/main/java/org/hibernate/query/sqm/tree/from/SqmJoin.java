/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import jakarta.persistence.criteria.JoinType;

import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;

/**
 * @author Steve Ebersole
 */
public interface SqmJoin<O,T> extends SqmFrom<O,T> {
	/**
	 * The type of join - inner, cross, etc
	 */
	SqmJoinType getSqmJoinType();

	/**
	 * When applicable, whether this join should be included in an implicit select clause
	 */
	boolean isImplicitlySelectable();

	@Override
	<X, Y> SqmAttributeJoin<X, Y> join(String attributeName);

	@Override
	<X, Y> SqmAttributeJoin<X, Y> join(String attributeName, JoinType jt);

	@Override
	SqmJoin<O, T> copy(SqmCopyContext context);
}
