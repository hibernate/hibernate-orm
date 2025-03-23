/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.domain.SqmTreatedJoin;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * @author Steve Ebersole
 */
public interface SqmTreatedAttributeJoin<L,R,R1 extends R> extends SqmAttributeJoin<L,R1>, SqmTreatedJoin<L,R,R1> {
	@Override
	<S extends R1> SqmTreatedAttributeJoin<L,R1,S> treatAs(Class<S> treatJavaType);

	@Override
	<S extends R1> SqmTreatedAttributeJoin<L,R1,S> treatAs(Class<S> treatJavaType, String alias);

	@Override
	<S extends R1> SqmTreatedAttributeJoin<L,R1,S> treatAs(EntityDomainType<S> treatTarget);

	@Override
	<S extends R1> SqmTreatedAttributeJoin<L,R1,S> treatAs(EntityDomainType<S> treatTarget, String alias);

	@Override
	SqmTreatedAttributeJoin<L,R,R1> on(JpaExpression<Boolean> restriction);

	@Override
	SqmTreatedAttributeJoin<L,R,R1> on(Expression<Boolean> restriction);

	@Override
	SqmTreatedAttributeJoin<L,R,R1> on(JpaPredicate... restrictions);

	@Override
	SqmTreatedAttributeJoin<L,R,R1> on(Predicate... restrictions);

	@Override
	SqmTreatedAttributeJoin<L,R,R1> copy(SqmCopyContext context);
}
