package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Model part which can be soft-deleted
 *
 * @author Steve Ebersole
 */
public interface SoftDeletableModelPart extends ModelPartContainer {
	/**
	 * Get the mapping of the soft-delete indicator
	 */
	@Nullable
	SoftDeleteMapping getSoftDeleteMapping();

	/**
	 * Details about the table which holds the soft-delete column.
	 */
	@Nonnull
	TableDetails getSoftDeleteTableDetails();
}
