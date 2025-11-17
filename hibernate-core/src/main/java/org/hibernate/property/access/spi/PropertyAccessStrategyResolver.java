/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.spi;

import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.service.Service;

/**
 * Contract for resolving the {@link PropertyAccessStrategy} to use.
 *
 * @author Steve Ebersole
 */
//TODO: moving forward I'd prefer this not be a service, but instead a
//      strategy on the MetadataBuildingContext or MetadataBuildingOptions
public interface PropertyAccessStrategyResolver extends Service {
	/**
	 * Resolve the PropertyAccessStrategy to use
	 *
	 * @param containerClass The java class of the entity
	 * @param explicitAccessStrategyName The access strategy name explicitly specified, if any.
	 * @param representationMode The entity mode in effect for the property, used to interpret different default strategies.
	 *
	 * @return The resolved PropertyAccessStrategy
	 */
	PropertyAccessStrategy resolvePropertyAccessStrategy(
			Class<?> containerClass,
			String explicitAccessStrategyName,
			RepresentationMode representationMode);
}
