/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.spi;

/**
 * A {@link ParameterRegistration} that allows providing Java type information when
 * binding a null value for a parameter when there is no other available type information
 * for that parameter.
 *
 * @author Gail Badner
 */
public interface NullTypeBindableParameterRegistration<T> extends ParameterRegistration<T> {

	/**
	 * If bindable, bind a null value using the provided parameter type.
	 * This method is only valid if {@link #getParameterType} returns {@code null}.
	 *
	 * @param nullParameterType the Java type to be used for binding the null value;
	 * must be non-null.
	 *
	 * @throws IllegalArgumentException {@code parameterType} is null or if
	 * {@link #getParameterType} does not return null.
	 */
	public void bindNullValue(Class<?> nullParameterType);
}
