/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.cdi.spi;

/**
 * Generalized contract for a CDI ManagedBean as seen by Hibernate
 *
 * @author Steve Ebersole
 */
public interface ManagedBean<T> {
	Class<T> getBeanClass();
	T getBeanInstance();
	void release();
}