package org.hibernate.persister.collection.mutation;

import jakarta.annotation.Nonnull;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public interface UpdateRowsCoordinator extends CollectionOperationCoordinator {
	void updateRows(@Nonnull Object key, @Nonnull PersistentCollection<?> collection, @Nonnull SharedSessionContractImplementor session);
}
