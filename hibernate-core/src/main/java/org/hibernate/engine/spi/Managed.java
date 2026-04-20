/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

/**
 * Contract for classes (specifically, entities and components/embeddables) that are "managed".
 * Developers can choose to either have their classes manually implement these interfaces, or
 * Hibernate can enhance their classes to implement these interfaces via built-time or run-time
 * enhancement.
 * <p>
 * The term <em>managed</em> is used in two senses:<ul>
 *     <li>
 *         A class is considered managed if it belongs to the persistence unit. This is
 *         represented by the class implementing {@code Managed}.
 *     </li>
 *     <li>
 *         An instance is considered managed if it is associated with a persistence context.
 *         Any such association is represented by the state exposed via operations of this
 *         interface.
 *     </li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public interface Managed extends PrimeAmongSecondarySupertypes {

	/**
	 * Special internal contract to optimize type checking
	 * @see PrimeAmongSecondarySupertypes
	 * @return this same instance
	 */
	@Override
	default Managed asManaged() {
		return this;
	}

}
