package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;

/**
 * Mapping of a row-id
 *
 * @see org.hibernate.annotations.RowId
 */
public interface EntityRowIdMapping extends BasicValuedModelPart, VirtualModelPart, SelectableMapping {
	@Nonnull
	String getRowIdName();
}
