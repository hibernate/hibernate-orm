/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.spi;

import org.hibernate.type.descriptor.java.internal.NoWrapperOptions;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.spi.WrapperOptions;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.VersionSupport;

/**
 * Descriptor for the Java side of a value mapping.
 *
 * @author Steve Ebersole
 */
public interface JavaTypeDescriptor<T> extends org.hibernate.type.descriptor.java.JavaTypeDescriptor<T>  {
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
	 * To be honest we have no idea when this is useful.  But older versions
	 * defined it, so in the interest of easier migrations we will keep it here.
	 * However, from our perspective it is the same as {@link #unwrap} - so
	 * the default impl here does exactly that.
	 */
	default String toString(T value) {
		return unwrap( value, String.class, NoWrapperOptions.INSTANCE );
	}

	/**
	 * The inverse of {@link #toString}.  See discussion there.
	 */
	default T fromString(String value) {
		return wrap( value, NoWrapperOptions.INSTANCE );
	}

}
