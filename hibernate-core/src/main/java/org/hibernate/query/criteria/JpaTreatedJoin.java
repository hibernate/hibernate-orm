/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import org.hibernate.metamodel.model.domain.EntityDomainType;

/**
 * @author Steve Ebersole
 */
public interface JpaTreatedJoin<L,R,R1 extends R> extends JpaTreatedFrom<L,R,R1>, JpaJoin<L,R1> {
	@Override
	<S extends R1> JpaTreatedJoin<L, R1, S> treatAs(Class<S> treatJavaType);

	@Override
	<S extends R1> JpaTreatedJoin<L, R1, S> treatAs(EntityDomainType<S> treatJavaType);
}
