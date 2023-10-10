/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.model.MutationOperationGroup;

/**
 * Coordinates the deleting of an entity.
 *
 * @see #coordinateDelete
 *
 * @author Steve Ebersole
 */
public interface DeleteCoordinator {
	/**
	 * The operation group used to perform the deletion unless some form
	 * of dynamic delete is necessary
	 */
	MutationOperationGroup getStaticDeleteGroup();

	/**
	 * Perform the deletions
	 */
	void coordinateDelete(
			Object entity,
			Object id,
			Object version,
			SharedSessionContractImplementor session);
}
