/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
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
