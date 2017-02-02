/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.spi;

/**
 * Represents reflection optimization for a particular class.
 *
 * @author Steve Ebersole
 */
public interface ReflectionOptimizer {
	/**
	 * Retrieve the optimizer for calling an entity's constructor via reflection.
	 *
	 * @return The optimizer for instantiation
	 */
	public InstantiationOptimizer getInstantiationOptimizer();

	/**
	 * Retrieve the optimizer for accessing the entity's persistent state.
	 *
	 * @return The optimizer for persistent state access
	 */
	public AccessOptimizer getAccessOptimizer();

	/**
	 * Represents optimized entity instantiation.
	 */
	public static interface InstantiationOptimizer {
		/**
		 * Perform instantiation of an instance of the underlying class.
		 *
		 * @return The new instance.
		 */
		public Object newInstance();
	}

	/**
	 * Represents optimized entity property access.
	 *
	 * @author Steve Ebersole
	 */
	public interface AccessOptimizer {
		/**
		 * Get the name of all properties.
		 *
		 * @return The name of all properties.
		 */
		public String[] getPropertyNames();

		/**
		 * Get the value of all properties from the given entity
		 *
		 * @param object The entity from which to extract values.
		 *
		 * @return The values.
		 */
		public Object[] getPropertyValues(Object object);

		/**
		 * Set all property values into an entity instance.
		 *
		 * @param object The entity instance
		 * @param values The values to inject
		 */
		public void setPropertyValues(Object object, Object[] values);
	}
}
