package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nullable;


import org.hibernate.property.access.spi.PropertyAccess;

/**
 * Describes an attribute with a property access.
 *
 * @author Christian Beikov
 */
public interface PropertyBasedMapping {

	/**
	 * The property access, or {@code null} for a synthetic mapping without a Java property.
	 */
	@Nullable
	PropertyAccess getPropertyAccess();
}
