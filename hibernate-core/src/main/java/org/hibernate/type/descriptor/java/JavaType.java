/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.Objects;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for the Java side of a value mapping.
 *
 * @author Steve Ebersole
 */
public interface JavaType<T> extends Serializable {
	/**
	 * Get the Java type (Type) described
	 *
	 * @see #getJavaTypeClass
	 */
	default Type getJavaType() {
		// default on this side since #getJavaTypeClass is the currently implemented method
		return getJavaTypeClass();
	}

	/**
	 * Get the Java type (Class) described
	 *
	 * @see #getJavaType
	 */
	default Class<T> getJavaTypeClass() {
		return ReflectHelper.getClass( getJavaType() );
	}

	/**
	 * Is the given value an instance of the described type?
	 *
	 * Generally this comes down to {@link #getJavaTypeClass() getJavaTypeClass().}{@link Class#isInstance isInstance()},
	 * though some descriptors (mainly the java.sql.Date, Time and Timestamp descriptors) might need different semantics
	 */
	default boolean isInstance(Object value) {
		return getJavaTypeClass().isInstance( value );
	}

	/**
	 * Retrieve the mutability plan for this Java type.
	 */
	default MutabilityPlan<T> getMutabilityPlan() {
		return ImmutableMutabilityPlan.instance();
	}

	default T getReplacement(T original, T target, SharedSessionContractImplementor session) {
		if ( !getMutabilityPlan().isMutable() || ( target != null && areEqual( original, target ) ) ) {
			return original;
		}
		else {
			return getMutabilityPlan().deepCopy( original );
		}
	}

	/**
	 * Get this Java type's default value.
	 *
	 * @return The default value.
	 */
	default T getDefaultValue() {
		return null;
	}

	/**
	 * Obtain the "recommended" SQL type descriptor for this Java type.  The recommended
	 * aspect comes from the JDBC spec (mostly).
	 *
	 * @param context Contextual information
	 *
	 * @return The recommended SQL type descriptor
	 */
	JdbcType getRecommendedJdbcType(JdbcTypeIndicators context);

	/**
	 * The default column length when this Java type is mapped
	 * to a SQL data type which is parametrized by length, for
	 * example {@link java.sql.Types#VARCHAR}.
	 *
	 * @return {@link Size#DEFAULT_LENGTH} unless overridden
	 */
	default long getDefaultSqlLength(Dialect dialect, JdbcType jdbcType) {
		return Size.DEFAULT_LENGTH;
	}

	/**
	 * The default column length when this Java type is mapped
	 * to a column of type {@link java.sql.Types#LONGVARCHAR}
	 * or {@link java.sql.Types#LONGVARBINARY}.
	 *
	 * @return {@link Size#LONG_LENGTH} unless overridden
	 */
	default long getLongSqlLength() {
		return Size.LONG_LENGTH;
	}

	/**
	 * The default column precision when this Java type is mapped
	 * to a SQL data type which is parametrized by precision, for
	 * example {@link java.sql.Types#DECIMAL}.
	 *
	 * @return {@link Size#DEFAULT_PRECISION} unless overridden
	 */
	default int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
		return Size.DEFAULT_PRECISION;
	}

	/**
	 * The default column scale when this Java type is mapped to a
	 * SQL data type which is parametrized by scale, for example
	 * {@link java.sql.Types#DECIMAL}.
	 *
	 * @return {@link Size#DEFAULT_SCALE} unless overridden
	 */
	default int getDefaultSqlScale(Dialect dialect, JdbcType jdbcType) {
		return Size.DEFAULT_SCALE;
	}

	/**
	 * Retrieve the natural comparator for this type.
	 */
	default Comparator<T> getComparator() {
		//noinspection unchecked
		return Comparable.class.isAssignableFrom( getJavaTypeClass() )
				? ComparableComparator.INSTANCE
				: null;
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

	T fromString(CharSequence string);

	/**
	 * Unwrap an instance of our handled Java type into the requested type.
	 * <p/>
	 * As an example, if this is a {@code JavaType<Integer>} and we are asked to unwrap
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

	/**
	 * Returns whether this java type is wider than the given type
	 * i.e. if the given type can be widened to this java type.
	 */
	default boolean isWider(JavaType<?> javaType) {
		return false;
	}

	interface CoercionContext {
		TypeConfiguration getTypeConfiguration();
	}

	default <X> T coerce(X value, CoercionContext coercionContext) {
		//noinspection unchecked
		return (T) value;
	}

	/**
	 * The check constraint that should be added to the column
	 * definition in generated DDL.
	 *
	 * @param columnName the name of the column
	 * @param sqlType the {@link JdbcType} of the mapped column
	 * @param dialect the SQL {@link Dialect}
	 * @return a check constraint condition or null
	 */
	default String getCheckCondition(String columnName, JdbcType sqlType, Dialect dialect) {
		return null;
	}

	/**
	 * Creates the {@link JavaType} for the given {@link ParameterizedType} based on this {@link JavaType} registered
	 * for the raw type.
	 */
	default JavaType<T> createJavaType(ParameterizedType parameterizedType) {
		return this;
	}
}
