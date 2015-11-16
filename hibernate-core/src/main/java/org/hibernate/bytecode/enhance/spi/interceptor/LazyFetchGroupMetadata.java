/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.spi.interceptor;

/**
  * Information about a particular bytecode lazy attribute grouping.
 *
 * @author Steve Ebersole
 */
public interface LazyFetchGroupMetadata {
	/**
	 * Access to the name of the fetch group.
	 *
	 * @return The fetch group name
	 */
	String getName();
}
