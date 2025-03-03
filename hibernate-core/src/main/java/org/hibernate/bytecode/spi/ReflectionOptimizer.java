/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.spi;

/**
 * Represents reflection optimization for a particular class.
 */
public interface ReflectionOptimizer {
	/**
	 * Retrieve the optimizer for calling an entity's constructor via reflection.
	 */
	InstantiationOptimizer getInstantiationOptimizer();

	/**
	 * Retrieve the optimizer for accessing the entity's persistent state.
	 */
	AccessOptimizer getAccessOptimizer();

	/**
	 * Represents optimized entity instantiation.
	 */
	interface InstantiationOptimizer {
		/**
		 * Perform instantiation of an instance of the underlying class.
		 */
		Object newInstance();
	}

	/**
	 * Represents optimized entity property access.
	 */
	interface AccessOptimizer {
		/**
		 * Get the name of all properties.
		 */
		String[] getPropertyNames();

		/**
		 * Get the value of all properties from the given entity
		 */
		Object[] getPropertyValues(Object object);

		/**
		 * Set all property values into an entity instance.
		 */
		void setPropertyValues(Object object, Object[] values);
	}
}
