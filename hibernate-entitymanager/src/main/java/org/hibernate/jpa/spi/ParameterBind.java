/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.spi;

import javax.persistence.TemporalType;

/**
 * Represents the value currently bound to a particular (bindable) parameter.
 *
 * @param <T>
 *
 * @author Steve Ebersole
 */
public interface ParameterBind<T> {
	/**
	 * Access the bound value
	 *
	 * @return The bound value
	 */
	public T getValue();

	/**
	 * The temporal type that will be used to "interpret" Date-like values (if applicable).
	 *
	 * @return The temporal type, or {@code null}
	 */
	public TemporalType getSpecifiedTemporalType();
}
