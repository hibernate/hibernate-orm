/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query;

import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.named.ResultMemento;
import org.hibernate.query.internal.ResultSetMappingResolutionContext;

/**
 * Describes the mapping for a result as part of a {@link NamedResultSetMappingDescriptor}
 *
 * @author Steve Ebersole
 */
public interface ResultDescriptor {
	/**
	 * Resolve the descriptor into a memento capable of being stored in the
	 * {@link NamedObjectRepository}
	 */
	ResultMemento resolve(ResultSetMappingResolutionContext resolutionContext);
}
