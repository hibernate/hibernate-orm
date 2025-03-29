/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;

/**
 * Consolidates the {@link Join} and {@link Fetch} hierarchies since that is how we implement them.
 * This allows us to treat them polymorphically.
*
* @author Steve Ebersole
*/
public interface JpaJoin<L, R> extends JpaFrom<L,R>, Join<L,R> {
	@Override
	PersistentAttribute<? super L, ?> getAttribute();

	JpaJoin<L, R> on(JpaExpression<Boolean> restriction);

	@Override
	JpaJoin<L, R> on(Expression<Boolean> restriction);

	JpaJoin<L, R> on(JpaPredicate... restrictions);

	@Override
	JpaJoin<L, R> on(Predicate... restrictions);

	@Override
	<S extends R> JpaTreatedJoin<L,R,S> treatAs(Class<S> treatAsType);

	@Override
	<S extends R> JpaTreatedJoin<L,R,S> treatAs(EntityDomainType<S> treatAsType);
}
