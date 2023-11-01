/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import jakarta.persistence.criteria.JoinType;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;

/**
 * @author Steve Ebersole
 */
public interface SqmJoin<L, R> extends SqmFrom<L, R> {
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
	SqmJoin<L, R> copy(SqmCopyContext context);

	@Override
	<S extends R> SqmJoin<L, S> treatAs(Class<S> treatAsType);

	@Override
	<S extends R> SqmJoin<L, S> treatAs(EntityDomainType<S> treatAsType);

	@Override
	<S extends R> SqmJoin<L, S> treatAs(Class<S> treatJavaType, String alias);

	@Override
	<S extends R> SqmJoin<L, S> treatAs(EntityDomainType<S> treatTarget, String alias);
}
