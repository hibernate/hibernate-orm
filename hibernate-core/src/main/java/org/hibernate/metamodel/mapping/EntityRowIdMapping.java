package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;

/**
 * Mapping of a row-id
 *
 * @see org.hibernate.annotations.RowId
 */
public interface EntityRowIdMapping<J> extends BasicValuedModelPart<J, J, J>, VirtualModelPart<J, J, J>, SelectableMapping {
	@Nonnull
	String getRowIdName();
}
