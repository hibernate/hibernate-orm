/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.query.sqm.SqmPathSource;

public interface SqmSingularPersistentAttribute<D, J>
		extends SingularPersistentAttribute<D, J>, SqmPathSource<J> {
	@Override
	SqmPathSource<J> getPathSource();
}
