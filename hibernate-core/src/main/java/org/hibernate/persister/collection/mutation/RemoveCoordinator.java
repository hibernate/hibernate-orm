/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Removes the collection:<ul>
 *     <li>
 *         For collections with a collection-table, this will execute a DELETE based
 *         on the {@linkplain org.hibernate.engine.spi.CollectionKey collection-key}
 *     </li>
 *     <li>
 *         For one-to-many collections, this executes an UPDATE to unset the collection-key
 *         on the association table
 *     </li>
 * </ul>
 *
 * @see org.hibernate.persister.collection.CollectionPersister#remove
 *
 * @author Steve Ebersole
 */
public interface RemoveCoordinator extends CollectionOperationCoordinator {
	/**
	 * The SQL used to perform the removal
	 */
	String getSqlString();

	/**
	 * Delete all rows based on the collection-key
	 */
	void deleteAllRows(Object key, SharedSessionContractImplementor session);
}
