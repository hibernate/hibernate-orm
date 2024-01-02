/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.sql.model.MutationOperationGroup;

/**
 * Coordinates the mutation operations of an entity.
 *
 * @see InsertCoordinator
 * @see DeleteCoordinator
 * @see UpdateCoordinator
 * @see MergeCoordinator
 *
 * @author Marco Belladelli
 */
public interface MutationCoordinator {
	/**
	 * The operation group used to perform the mutation unless some form
	 * of dynamic mutation is necessary.
	 */
	MutationOperationGroup getStaticMutationOperationGroup();
}
