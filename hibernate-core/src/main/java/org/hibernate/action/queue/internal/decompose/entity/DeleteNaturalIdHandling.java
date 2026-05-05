/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.entity;


import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/// Natural-id bookkeeping for graph-based entity deletes.
///
/// @author Steve Ebersole
public class DeleteNaturalIdHandling {
	public static Object removeLocalResolution(
			EntityDeleteAction action,
			SharedSessionContractImplementor session) {
		final var persister = action.getPersister();
		final var naturalIdMapping = persister.getNaturalIdMapping();
		if ( naturalIdMapping == null ) {
			return null;
		}

		return session.getPersistenceContextInternal()
				.getNaturalIdResolutions()
				.removeLocalResolution(
						action.getId(),
						naturalIdMapping.extractNaturalIdFromEntityState( action.getState() ),
						persister
				);
	}
}
