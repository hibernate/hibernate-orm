/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

/**
 * Contract for classes (specifically, entities and components/embeddables) that are "managed".  Developers can
 * choose to either have their classes manually implement these interfaces or Hibernate can enhance their classes
 * to implement these interfaces via built-time or run-time enhancement.
 * <p>
 * The term managed here is used to describe both:<ul>
 *     <li>
 *         the fact that they are known to the persistence provider (this is defined by the interface itself)
 *     </li>
 *     <li>
 *         its association with Session/EntityManager (this is defined by the state exposed through the interface)
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
