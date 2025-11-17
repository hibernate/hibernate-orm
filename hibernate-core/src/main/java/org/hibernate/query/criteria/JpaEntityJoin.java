/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.tree.domain.SqmTreatedEntityJoin;

/**
 * @author Steve Ebersole
 */
public interface JpaEntityJoin<L,R> extends JpaJoin<L,R> {
	@Override
	EntityDomainType<R> getModel();

	@Override
	<S extends R> SqmTreatedEntityJoin<L,R,S> treatAs(Class<S> treatAsType);

	@Override
	<S extends R> SqmTreatedEntityJoin<L,R,S> treatAs(EntityDomainType<S> treatAsType);
}
