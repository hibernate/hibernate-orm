/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property.access.spi;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * The contract for setting a persistent property value into its container/owner
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface Setter extends Serializable {

	void set(Object target, Object value);

	/**
	 * Optional operation (may return {@code null})
	 */
	String getMethodName();

	/**
	 * Optional operation (may return {@code null})
	 */
	Method getMethod();
}
