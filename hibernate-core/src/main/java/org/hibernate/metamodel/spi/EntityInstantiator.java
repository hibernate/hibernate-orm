package org.hibernate.metamodel.spi;

import jakarta.annotation.Nonnull;

/**
 * Contract for instantiating entity values
 */
public interface EntityInstantiator extends Instantiator {
	/**
	 * Create an instance of managed entity
	 */
	@Nonnull
	Object instantiate();

	/**
	 * Can this entity be instantiated?
	 */
	default boolean canBeInstantiated() {
		return true;
	}
}
