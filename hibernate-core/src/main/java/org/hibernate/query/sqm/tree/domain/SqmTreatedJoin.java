/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaTreatedJoin;

/**
 * @author Steve Ebersole
 */
public interface SqmTreatedJoin<L,R,R1 extends R> extends SqmTreatedFrom<L,R,R1>, JpaTreatedJoin<L,R,R1> {
	@Override
	<S extends R1> SqmTreatedJoin<L, R1, S> treatAs(Class<S> treatJavaType);

	@Override
	<S extends R1> SqmTreatedJoin<L, R1, S> treatAs(EntityDomainType<S> treatTarget);

	@Override
	<S extends R1> SqmTreatedJoin<L, R1, S> treatAs(Class<S> treatJavaType, String alias);

	@Override
	<S extends R1> SqmTreatedJoin<L, R1, S> treatAs(EntityDomainType<S> treatTarget, String alias);
}
