/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;


import org.hibernate.query.DynamicInstantiationNature;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Represents the thing-to-be-instantiated in a dynamic instantiation expression.  Hibernate
 * supports 3 "natures" of dynamic instantiation target; see {@link DynamicInstantiationNature} for further details.
 *
 * @author Steve Ebersole
 */
public interface SqmDynamicInstantiationTarget<T> extends SqmExpressable<T> {

	/**
	 * Retrieves the enum describing the nature of this target.
	 *
	 * @return The nature of this target.
	 */
	DynamicInstantiationNature getNature();

	JavaTypeDescriptor<T> getTargetTypeDescriptor();

	/**
	 * For {@link DynamicInstantiationNature#CLASS} this will return the Class to be instantiated.  For
	 * {@link DynamicInstantiationNature#MAP} and {@link DynamicInstantiationNature#LIST} this will return {@code Map.class}
	 * and {@code List.class} respectively.
	 *
	 * @return The type to be instantiated.
	 */
	default Class getJavaType() {
		return getTargetTypeDescriptor().getJavaType();
	}
}
