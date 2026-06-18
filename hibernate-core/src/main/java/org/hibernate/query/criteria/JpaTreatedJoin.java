/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import org.hibernate.metamodel.model.domain.EntityDomainType;

/**
 * @author Steve Ebersole
 */
public interface JpaTreatedJoin<L,R,R1 extends R> extends JpaTreatedFrom<L,R,R1>, JpaJoin<L,R1> {
	/**
	 * Downcast this join to the specified subtype.
	 */
	@Override
	@Nonnull
	<S extends R1> JpaTreatedJoin<L, R1, S> treatAs(@Nonnull Class<S> treatJavaType);

	/**
	 * Downcast this join to the specified subtype.
	 */
	@Override
	@Nonnull
	<S extends R1> JpaTreatedJoin<L, R1, S> treatAs(@Nonnull EntityDomainType<S> treatJavaType);
}
