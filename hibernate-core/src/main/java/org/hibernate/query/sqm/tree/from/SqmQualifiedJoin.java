/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaJoinedFrom;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;

/**
 * Common contract for qualified/restricted/predicated joins.
 *
 * @author Steve Ebersole
 */
public interface SqmQualifiedJoin<L, R> extends SqmJoin<L,R>, JpaJoinedFrom<L,R> {
	/**
	 * Obtain the join predicate
	 *
	 * @return The join predicate
	 */
	SqmPredicate getJoinPredicate();

	/**
	 * Inject the join predicate
	 *
	 * @param predicate The join predicate
	 */
	void setJoinPredicate(SqmPredicate predicate);

	@Override
	<S extends R> SqmQualifiedJoin<L, S> treatAs(Class<S> treatAsType);

	@Override
	<S extends R> SqmQualifiedJoin<L, S> treatAs(EntityDomainType<S> treatAsType);

	@Override
	<S extends R> SqmQualifiedJoin<L, S> treatAs(Class<S> treatJavaType, String alias);

	@Override
	<S extends R> SqmQualifiedJoin<L, S> treatAs(EntityDomainType<S> treatTarget, String alias);
}
