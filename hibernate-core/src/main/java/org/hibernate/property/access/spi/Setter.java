/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property.access.spi;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * The contract for setting a persistent property value into its container/owner
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface Setter extends Serializable {
	/**
	 * Set the property value from the given instance
	 *
	 * @param target The instance upon which to set the given value.
	 * @param value The value to be set on the target.
	 * @param factory The session factory from which this request originated.
	 *
	 * @throws org.hibernate.HibernateException
	 */
	void set(Object target, Object value, SessionFactoryImplementor factory);

	/**
	 * Optional operation (may return {@code null})
	 */
	String getMethodName();

	/**
	 * Optional operation (may return {@code null})
	 */
	Method getMethod();
}
