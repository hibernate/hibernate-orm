/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public interface UpdateRowsCoordinator extends CollectionOperationCoordinator {
	void updateRows(Object key, PersistentCollection<?> collection, SharedSessionContractImplementor session);
}
