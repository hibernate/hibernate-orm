/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
