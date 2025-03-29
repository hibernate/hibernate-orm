/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query;

import org.hibernate.spi.NavigablePath;
import org.hibernate.query.named.FetchMemento;
import org.hibernate.query.internal.ResultSetMappingResolutionContext;
import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.named.ResultMemento;

/**
 * Describes the mapping for a fetch as part of a {@link NamedResultSetMappingDescriptor}
 */
public interface FetchDescriptor {
	/**
	 * Resolve the descriptor into a memento capable of being stored in the
	 * {@link NamedObjectRepository}
	 */
	FetchMemento resolve(ResultSetMappingResolutionContext resolutionContext);

	ResultMemento asResultMemento(
			NavigablePath path,
			ResultSetMappingResolutionContext resolutionContext);
}
