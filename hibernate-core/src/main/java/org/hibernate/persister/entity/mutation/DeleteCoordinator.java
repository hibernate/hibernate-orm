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
	 * Delete a persistent instance. The instance is {@code null} for an unloaded reference.
	 */
	void delete(@Nullable Object entity, @Nonnull Object id, @Nullable Object version, @Nonnull SharedSessionContractImplementor session);
}
