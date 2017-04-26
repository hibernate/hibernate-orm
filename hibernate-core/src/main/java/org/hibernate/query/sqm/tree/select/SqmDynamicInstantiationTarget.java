/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

/**
 * Represents the thing-to-be-instantiated in a dynamic instantiation expression.  Hibernate
 * supports 3 "natures" of dynamic instantiation target; see {@link Nature} for further details.
 *
 * @author Steve Ebersole
 */
public interface SqmDynamicInstantiationTarget<T> {
	/**
	 * Represents the type of instantiation indicated.
	 */
	enum Nature {
		/**
		 * The target names a Class to be instantiated.  This is the only form
		 * of dynamic instantiation that is JPA_compliant.
		 */
		CLASS,
		/**
		 * The target identified a {@link java.util.Map} instantiation.  The
		 * result for each "row" will be a Map whose key is the alias (or name
		 * of the selected attribute is no alias) and whose value is the
		 * corresponding value read from the JDBC results.  Similar to JPA's
		 * named-Tuple support.
		 */
		MAP,
		/**
		 * The target identified a {@link java.util.List} instantiation.  The
		 * result for each "row" will be a List rather than an array.  Similar
		 * to JPA's named-Tuple support.
		 */
		LIST
	}

	/**
	 * Retrieves the enum describing the nature of this target.
	 *
	 * @return The nature of this target.
	 */
	Nature getNature();

	/**
	 * For {@link Nature#CLASS} this will return the Class to be instantiated.  For
	 * {@link Nature#MAP} and {@link Nature#LIST} this will return {@code Map.class}
	 * and {@code List.class} respectively.
	 *
	 * @return The type to be instantiated.
	 */
	Class getJavaType();
}
