/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.instantiation;

/**
 * Unified contract for injecting a single argument for a dynamic instantiation
 * result, whether that is constructor-based or setter-based.
 *
 * @author Steve Ebersole
 */
interface BeanInjector<T> {
	void inject(T target, Object value);
}
