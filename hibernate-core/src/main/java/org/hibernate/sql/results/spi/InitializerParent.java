/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

/**
 * Intended to give a Fetch Initializer access to the FK value that links it
 * to its FetchParent's Initializer
 *
 * @author Steve Ebersole
 */
public interface InitializerParent extends Initializer {
	/**
	 * Get the object instance that is the parent of this fetch
	 */
	Object getParentInstance();
}
