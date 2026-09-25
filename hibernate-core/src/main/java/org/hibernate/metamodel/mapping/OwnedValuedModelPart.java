package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nullable;

/**
 * Marker interface for valued model parts that have a declaring/owner type.
 */
public interface OwnedValuedModelPart extends ValuedModelPart {
	@Nullable
	MappingType getDeclaringType();
}
