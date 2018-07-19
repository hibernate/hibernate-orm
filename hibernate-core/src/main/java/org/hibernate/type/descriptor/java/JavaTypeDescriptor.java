/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for the Java side of a value mapping.
 *
 * @author Steve Ebersole
 */
public interface JavaTypeDescriptor<T> extends Serializable {
	/**
	 * Retrieve the Java type handled here.
	 *
	 * @return The Java type.
	 *
	 * @deprecated Use {@link #getJavaType()} instead
	 */
	@Deprecated
	Class<T> getJavaTypeClass();

	/**
	 * Get the Java type described
	 */
	default Class<T> getJavaType() {
		// default on this side since #getJavaTypeClass is the currently implemented method
		return getJavaTypeClass();
	}

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
	default String extractLoggableRepresentation(T value) {
		return toString( value );
	}

	default String toString(T value) {
		return value == null ? "null" : value.toString();
	}

	T fromString(String string);

	/**
	 * Unwrap an instance of our handled Java type into the requested type.
	 * <p/>
	 * As an example, if this is a {@code JavaTypeDescriptor<Integer>} and we are asked to unwrap
	 * the {@code Integer value} as a {@code Long} we would return something like
	 * <code>Long.valueOf( value.longValue() )</code>.
	 * <p/>
	 * Intended use is during {@link java.sql.PreparedStatement} binding.
	 *
	 * @param value The value to unwrap
	 * @param type The type as which to unwrap
	 * @param options The options
	 * @param <X> The conversion type.
	 *
	 * @return The unwrapped value.
	 */
	<X> X unwrap(T value, Class<X> type, WrapperOptions options);

	/**
	 * Wrap a value as our handled Java type.
	 * <p/>
	 * Intended use is during {@link java.sql.ResultSet} extraction.
	 *
	 * @param value The value to wrap.
	 * @param options The options
	 * @param <X> The conversion type.
	 *
	 * @return The wrapped value.
	 */
	<X> T wrap(X value, WrapperOptions options);
}
