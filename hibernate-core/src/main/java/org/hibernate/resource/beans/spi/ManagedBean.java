/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.beans.spi;

/**
 * Generalized contract for a "ManagedBean" as seen by Hibernate
 *
 * @author Steve Ebersole
 */
public interface ManagedBean<T> {
	/**
	 * The bean Java type
	 */
	Class<T> getBeanClass();

	/**
	 * The bean reference
	 */
	T getBeanInstance();
}
