/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Descriptor for the Java side of a value mapping.
 *
 * @author Steve Ebersole
 */
public interface JavaTypeDescriptor<T> extends Serializable {
	/**
	 * The Java type (Class) represented by this descriptor.
	 * <p/>
	 * May be {@code null} in the case of dynamic models ({@link org.hibernate.EntityMode#MAP} e.g.).
	 */
	Class<T> getJavaType();

	/**
	 * Get the type name.  This is useful for dynamic models which either will not have
	 * a Java type ({@link #getJavaType()} returns null) or {@link #getJavaType()}
	 * returns a non-indicative value ({@code java.util.Map.class} for a composite value in
	 * {@link org.hibernate.EntityMode#MAP} EntityMode, e.g.).
	 * <p/>
	 * For typed models, this generally returns {@link #getJavaType()}.{@linkplain Class#getName() getName}
	 *
	 * @return The Java type name.
	 */
	String getTypeName();

	/**
	 * Retrieve the mutability plan for this Java type.
	 *
	 * @return The mutability plan
	 */
	MutabilityPlan<T> getMutabilityPlan();

	/**
	 * Retrieve the natural comparator for this type.
	 *
	 * @return The natural comparator.
	 */
	Comparator<T> getComparator();

	/**
	 * Extract a proper hash code for this value.
	 *
	 * @param value The value for which to extract a hash code.
	 *
	 * @return The extracted hash code.
	 */
	int extractHashCode(T value);

	/**
	 * Determine if two instances are equal
	 *
	 * @param one One instance
	 * @param another The other instance
	 *
	 * @return True if the two are considered equal; false otherwise.
	 */
	boolean areEqual(T one, T another);

	/**
	 * Extract a loggable representation of the value.
	 *
	 * @param value The value for which to extract a loggable representation.
	 *
	 * @return The loggable representation
	 */
	String extractLoggableRepresentation(T value);
}
