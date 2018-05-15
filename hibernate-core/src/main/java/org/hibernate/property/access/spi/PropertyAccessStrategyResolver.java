/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property.access.spi;

import org.hibernate.EntityMode;
import org.hibernate.annotations.Remove;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeRepresentationStrategy;
import org.hibernate.service.Service;

/**
 * Contract for resolving the PropertyAccessStrategy to use.
 *
 * @author Steve Ebersole
 *
 * @deprecated Use {@link ManagedTypeRepresentationStrategy#generatePropertyAccess}
 * instead
 */
@Deprecated
@Remove
public interface PropertyAccessStrategyResolver extends Service {
	/**
	 * Resolve the PropertyAccessStrategy to use
	 *
	 * @param containerClass The java class of the entity
	 * @param explicitAccessStrategyName The access strategy name explicitly specified, if any.
	 * @param entityMode The entity mode in effect for the property, used to interpret different default strategies.
	 *
	 * @return The resolved PropertyAccessStrategy
	 */
	PropertyAccessStrategy resolvePropertyAccessStrategy(
			Class containerClass,
			String explicitAccessStrategyName,
			EntityMode entityMode);
}
