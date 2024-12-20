/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Coordinates the deleting of an entity.
 *
 * @author Steve Ebersole
 * @see #delete
 */
public interface DeleteCoordinator extends MutationCoordinator {
	/**
	 * Delete a persistent instance.
	 */
	void delete(Object entity, Object id, Object version, SharedSessionContractImplementor session);
}
