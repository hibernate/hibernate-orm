/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.spi;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Descriptor for the Java side of a value mapping.  We abstract from
 * {@link Class} because domain types might be "virtual".
 *
 * Note however that we do require that all basic types be based on
 * a real Java {@link Class}
 *
 * @author Steve Ebersole
 */
public interface JavaTypeDescriptor<T> extends org.hibernate.type.descriptor.java.JavaTypeDescriptor<T>  {
	// todo (6.0) : Use this as a cache for reflection look-ups on the Java type

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
	 * Unwrap an instance of our handled Java type into the requested type.
	 * <p/>
	 * As an example, if this is a {@code JavaTypeDescriptor<Integer>} and we are asked to unwrap
	 * the {@code Integer value} as a {@code Long} we would return something like
	 * <code>Long.valueOf( value.longValue() )</code>.
	 * <p/>
	 * Intended use is during {@link java.sql.PreparedStatement} binding.
	 *
	 * @param <X> The conversion type.
	 *
	 * @param value The value to unwrap
	 * @param type The type as which to unwrap
	 * @param session The Session
	 *
	 * @return The unwrapped value.
	 */
	<X> X unwrap(T value, Class<X> type, SharedSessionContractImplementor session);

	/**
	 * Wrap a value as our handled Java type.
	 * <p/>
	 * Intended use is during {@link java.sql.ResultSet} extraction.
	 *
	 * @param <X> The conversion type.
	 *
	 * @param value The value to wrap.
	 * @param session The options
	 * @return The wrapped value.
	 */
	<X> T wrap(X value, SharedSessionContractImplementor session);

	/**
	 * To be honest we have no idea when this is useful.  But older versions
	 * defined it, so in the interest of easier migrations we will keep it here.
	 * However, from our perspective it is the same as {@link #unwrap} - so
	 * the default impl here does exactly that.
	 */
	default String toString(T value) {
		return unwrap( value, String.class, null );
	}

	/**
	 * The inverse of {@link #toString}.  See discussion there.
	 */
	default T fromString(String value) {
		return wrap( value, null );
	}

	default boolean isInstance(Object value) {
		return getJavaType().isInstance( value );
	}

	default boolean isAssignableFrom(Class checkType) {
		return getJavaType().isAssignableFrom( checkType );
	}
}
