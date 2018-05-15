/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.type.descriptor.java.spi.ImmutableMutabilityPlan;

/**
 * Descriptor for the Java side of a value mapping.
 *
 * @author Steve Ebersole
 */
public interface JavaTypeDescriptor<T> extends Serializable {

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
	 * The Java type (Class) represented by this descriptor.
	 * <p/>
	 * May be {@code null} in the case of dynamic models ({@link org.hibernate.EntityMode#MAP} e.g.).
	 */
	Class<T> getJavaType();

	/**
	 * Retrieve the mutability plan for this Java type.
	 */
	@SuppressWarnings("unchecked")
	default MutabilityPlan<T> getMutabilityPlan() {
		return ImmutableMutabilityPlan.INSTANCE;
	}

	/**
	 * Retrieve the natural comparator for this type.
	 */
	default Comparator<T> getComparator() {
		return Comparable.class.isAssignableFrom( Comparable.class ) ? ComparableComparator.INSTANCE : null;
	}

	/**
	 * Extract a proper hash code for this value.
	 *
	 * @param value The value for which to extract a hash code.
	 *
	 * @return The extracted hash code.
	 */
	default int extractHashCode(T value) {
		if ( value == null ) {
			throw new IllegalArgumentException( "Value to extract hashCode from cannot be null" );
		}
		return value.hashCode();
	}

	/**
	 * Determine if two instances are equal
	 *
	 * @param one One instance
	 * @param another The other instance
	 *
	 * @return True if the two are considered equal; false otherwise.
	 */
	default boolean areEqual(T one, T another) {
		return Objects.deepEquals( one, another );
	}

	/**
	 * Extract a loggable representation of the value.
	 *
	 * @param value The value for which to extract a loggable representation.
	 *
	 * @return The loggable representation
	 */
	String extractLoggableRepresentation(T value);
}
