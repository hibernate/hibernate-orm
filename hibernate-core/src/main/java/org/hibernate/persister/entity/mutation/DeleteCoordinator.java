package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
	void delete(@Nonnull Object entity, @Nonnull Object id, @Nullable Object version, @Nonnull SharedSessionContractImplementor session);
}
