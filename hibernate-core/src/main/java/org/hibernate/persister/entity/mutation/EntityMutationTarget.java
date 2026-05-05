/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.Incubating;
import org.hibernate.action.queue.spi.decompose.entity.GraphEntityMutationTarget;
import org.hibernate.action.queue.spi.meta.EntityTableDescriptor;
import org.hibernate.sql.model.MutationTarget;

/**
 * Bridge interface combining legacy and graph-based entity mutation target contracts.
 * <p>
 * This interface exists to maintain compatibility during the transition from the
 * legacy sequential action queue to the graph-based action queue. Entity persisters
 * implement this interface to support both queue implementations.
 * <p>
 * New code should prefer using the more specific {@link LegacyEntityMutationTarget}
 * for legacy coordinators or {@link GraphEntityMutationTarget} for graph-based decomposers.
 *
 * @see LegacyEntityMutationTarget
 * @see GraphEntityMutationTarget
 *
 * @author Steve Ebersole
 *
 * @deprecated Transitional interface - use {@link LegacyEntityMutationTarget} for legacy
 * coordinators or {@link GraphEntityMutationTarget} for graph-based decomposers
 */
@Deprecated(since = "8.0", forRemoval = true)
@Incubating
public interface EntityMutationTarget
		extends MutationTarget<EntityTableMapping, EntityTableDescriptor>,
				LegacyEntityMutationTarget,
				GraphEntityMutationTarget {

	// Bridge interface - all methods inherited from LegacyEntityMutationTarget and GraphEntityMutationTarget
}
