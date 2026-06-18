/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.domain;

import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.spi.SqmJoinable;

@Incubating
public interface SqmSingularPersistentAttribute<D, J>
		extends SingularPersistentAttribute<D, J>, SqmPersistentAttribute<D, J>,
				SqmJoinable<D,J>, SqmPathSource<J> {
	SqmPathSource<J> getSqmPathSource();
}
