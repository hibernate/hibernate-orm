/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query;

import java.io.Serializable;

import jakarta.annotation.Nonnull;
import org.hibernate.query.named.spi.FetchMemento;
import org.hibernate.query.internal.ResultSetMappingResolutionContext;
import org.hibernate.query.named.spi.NamedObjectRepository;

/**
 * Describes the mapping for a fetch as part of a {@link NamedResultSetMappingDescriptor}
 */
public interface FetchDescriptor extends Serializable {
	/**
	 * Resolve the descriptor into a memento capable of being stored in the
	 * {@link NamedObjectRepository}
	 */
	@Nonnull
	FetchMemento resolve(@Nonnull ResultSetMappingResolutionContext resolutionContext);

//	ResultMemento asResultMemento(
//			NavigablePath path,
//			ResultSetMappingResolutionContext resolutionContext);
}
