/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.Objects;

import org.hibernate.Incubating;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.CharSequenceHelper;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Descriptor for the Java side of a value mapping. A {@code JavaType} is always
 * coupled with a {@link JdbcType} to describe the typing aspects of an attribute
 * mapping from Java to JDBC.
 * <p>
 * An instance of this interface represents a certain {@linkplain #getJavaType()
 * Java class or interface} which may occur as the type of a persistent property
 * or field of an entity class.
 * <p>
 * A {@code JavaType} decides how instances of the Java type are compared for
 * {@linkplain #areEqual equality} and {@linkplain #getComparator order}, and
 * it knows how to convert {@linkplain #unwrap to} and {@linkplain #wrap from}
 * various different representations that might be requested by its partner
 * {@link JdbcType}.
 * <p>
 * Every {@code JavaType} has a {@link MutabilityPlan} which defines how instances
 * of the type are {@linkplain MutabilityPlan#deepCopy(Object) cloned}, and how
 * they are {@linkplain MutabilityPlan#disassemble disassembled to} and
 * {@linkplain MutabilityPlan#assemble assembled from} their representation in the
 * {@linkplain org.hibernate.Cache second-level cache}.
 * <p>
 * Even though it's strictly only responsible for Java aspects of the mapping, a
 * {@code JavaType} usually does come with a {@linkplain #getRecommendedJdbcType
 * recommendation} for a friendly {@link JdbcType} it works particularly well
 * with, along with a default {@linkplain #getDefaultSqlLength length},
 * {@linkplain #getDefaultSqlPrecision precision}, and
 * {@linkplain #getDefaultSqlScale scale} for mapped columns.
 * <p>
 * A Java type may be selected when mapping an entity attribute using the
 * {@link org.hibernate.annotations.JavaType} annotation, though this is typically
 * unnecessary.
 * <p>
 * Custom implementations should be registered with the
 * {@link org.hibernate.type.descriptor.java.spi.JavaTypeRegistry} at startup.
 * The built-in implementations are registered automatically.
 *
 * @see JdbcType
 *
 * @author Steve Ebersole
 */
public interface JavaType<T> extends Serializable {
	/**
	 * Get the Java type (a {@link Type} object) described by this {@code JavaType}.
	 *
	 * @see #getJavaTypeClass
	 */
	default Type getJavaType() {
		// default on this side since #getJavaTypeClass is the currently implemented method
		return getJavaTypeClass();
	}

	/**
	 * Get the Java type (the {@link Class} object) described by this {@code JavaType}.
	 *
	 * @see #getJavaType
	 */
	default Class<T> getJavaTypeClass() {
		return ReflectHelper.getClass( getJavaType() );
	}

	/**
	 * Get the name of the Java type.
	 */
	default String getTypeName() {
		return getJavaType().getTypeName();
	}

	/**
	 * Is the given value an instance of the described type?
	 * <p>
	 * Usually just {@link #getJavaTypeClass() getJavaTypeClass().}{@link Class#isInstance isInstance(value)},
	 * but some descriptors need specialized semantics, for example, the descriptors for
	 * {@link JdbcDateJavaType java.sql.Date}, {@link JdbcTimeJavaType java.sql.Time}, and
	 * {@link JdbcTimestampJavaType java.sql.Timestamp}.
	 */
	default boolean isInstance(Object value) {
		return getJavaTypeClass().isInstance( value );
	}

	default T cast(Object value) {
		return getJavaTypeClass().cast( value );
	}

	/**
	 * Retrieve the {@linkplain MutabilityPlan mutability plan} for this Java type.
	 */
	default MutabilityPlan<T> getMutabilityPlan() {
		return ImmutableMutabilityPlan.instance();
	}

	default T getReplacement(T original, T target, SharedSessionContractImplementor session) {
		final var mutabilityPlan = getMutabilityPlan();
		return !mutabilityPlan.isMutable()
			|| target != null && areEqual( original, target )
				? original
				: mutabilityPlan.deepCopy( original );
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
	 * Obtain the "recommended" {@link JdbcType SQL type descriptor}
	 * for this Java type. Often, but not always, the source of this
	 * recommendation is the JDBC specification.
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
	 * Extract a proper hash code for the given value.
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
	 * Whether to use {@link Object#equals(Object)} and {@link Object#hashCode()}
	 * or {@link #areEqual(Object, Object)} and {@link #extractHashCode(Object)}
	 * for objects of this java type.
	 * This is useful to avoid mega-morphic callsites.
	 */
	default boolean useObjectEqualsHashCode() {
		return false;
	}

	/**
	 * Extract a loggable representation of the given value.
	 *
	 * @param value The value for which to extract a loggable representation.
	 *
	 * @return The loggable representation
	 */
	default String extractLoggableRepresentation(@Nullable T value) {
		return value == null ? "null" : toString( value );
	}

	default String toString(T value) {
		return value.toString();
	}

	T fromString(CharSequence string);

	/**
	 * Appends the value to the SqlAppender in an encoded format that can be decoded again by {@link #fromEncodedString(CharSequence, int, int)}.
	 * Implementers do not need to care about escaping. This is similar to {@link #toString(Object)},
	 * with the difference that the aim of this method is encoding to the appender.
	 * @since 6.2
	 */
	default void appendEncodedString(SqlAppender sb, T value) {
		sb.append( toString( value ) );
	}

	/**
	 * Reads the encoded value from the char sequence start index until the end index and returns the decoded value.
	 * Implementers do not need to care about escaping. This is similar to {@link #fromString(CharSequence)},
	 * with the difference that the aim of this method is decoding from a range within an existing char sequence.
	 * @since 6.2
	 */
	default T fromEncodedString(CharSequence charSequence, int start, int end) {
		return fromString( CharSequenceHelper.subSequence( charSequence, start, end ) );
	}

	default T fromEncodedString(CharSequence charSequence) {
		return fromEncodedString( charSequence, 0, charSequence.length()  );
	}

	/**
	 * Unwrap an instance of our handled Java type into the requested type.
	 * <p>
	 * As an example, if this is a {@code JavaType<Integer>} and we are asked to
	 * unwrap the {@code Integer value} as a {@code Long}, we would return something
	 * like <code>Long.valueOf( value.longValue() )</code>.
	 * <p>
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
	 * <p>
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
	 * Determines if this Java type is wider than the given Java type,
	 * that is, if the given type can be safely widened to this type.
	 */
	default boolean isWider(JavaType<?> javaType) {
		return false;
	}

	@FunctionalInterface
	@Deprecated(forRemoval = true, since = "7.2")
	interface CoercionContext {
		TypeConfiguration getTypeConfiguration();
	}

	/**
	 * Coerce the given value to this type, if possible.
	 *
	 * @param value The value to coerce
	 * @return The coerced value, or the given value
	 *         if no coercion was possible
	 * @throws CoercionException if coercion fails
	 */
	@Incubating
	default Object coerce(Object value) {
		return value;
	}

	/**
	 * Creates the {@link JavaType} for the given {@link ParameterizedType}
	 * based on this {@link JavaType} registered for the raw type.
	 *
	 * @since 6.1
	 */
	@Incubating
	default JavaType<T> createJavaType(ParameterizedType parameterizedType, TypeConfiguration typeConfiguration) {
		return this;
	}

	/**
	 * Return true if the implementation is an instance of {@link  TemporalJavaType}
	 *
	 * @return true if it is an instance of {@link  TemporalJavaType}; false otherwise
	 */
	default boolean isTemporalType() {
		return false;
	}

	/**
	 * The check constraint that should be added to the column
	 * definition in generated DDL.
	 *
	 * @param columnName the name of the column
	 * @param jdbcType   the {@link JdbcType} of the mapped column
	 * @param converter  the converter, if any, or null
	 * @param dialect    the SQL {@link Dialect}
	 * @return a check constraint condition or null
	 * @since 6.2
	 */
	@Incubating
	default String getCheckCondition(String columnName, JdbcType jdbcType, BasicValueConverter<T, ?> converter, Dialect dialect) {
		return null;
	}
}
