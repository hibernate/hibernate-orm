/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.descriptor.java;

import java.io.Serializable;
import java.util.Comparator;

import javax.persistence.metamodel.Type;

import org.hibernate.type.spi.descriptor.WrapperOptions;
import org.hibernate.type.spi.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.spi.descriptor.JdbcRecommendedSqlTypeMappingContext;

/**
 * Descriptor for the Java side of a value mapping.
 *
 * @author Steve Ebersole
 */
public interface JavaTypeDescriptor<T> extends Type<T>, Serializable {
	/**
	 * Retrieve the Java type handled here.
	 * <p/>
	 * May be {@code null} in the case of dynamic models ({@link org.hibernate.EntityMode#MAP} e.g.).
	 *
	 * @return The Java type, or {@code null}
	 */
	Class<T> getJavaTypeClass();

	/**
	 * Note that implementations may return {@code null} in the case of dynamic models
	 * ({@link org.hibernate.EntityMode#MAP} e.g.).
	 *
	 * {@inheritDoc}
	 */
	@Override
	default Class<T> getJavaType() {
		return getJavaTypeClass();
	}

	/**
	 * Get the type name.  This is useful for dynamic models which either will not have
	 * a Java type ({@link #getJavaTypeClass()} returns null) or {@link #getJavaTypeClass()}
	 * returns a non-indicative value ({@code java.util.Map.class} for a composite value in
	 * {@link org.hibernate.EntityMode#MAP} EntityMode, e.g.).
	 * <p/>
	 * For typed models, this generally returns {@link #getJavaTypeClass()}.{@linkplain Class#getName() getName}
	 *
	 * @return The Java type name.
	 */
	default String getTypeName() {
		return getJavaTypeClass().getName();
	}

	/**
	 * Obtain the "recommended" SQL type descriptor for this Java type.  The recommended
	 * aspect comes from the JDBC spec (mostly).
	 *
	 * @param context Contextual information
	 *
	 * @return The recommended SQL type descriptor
	 */
	SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context);

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

	String toString(T value);

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
