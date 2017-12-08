/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.domain;

/**
 * Contract for "managed types", which is taken from the JPA term for commonality between
 * entity, embeddable, and "mapped superclass" types.
 *
 * @author Chris Cranford
 */
public interface ManagedJavaTypeMapping<T> extends JavaTypeMapping<T> {
	/**
	 * Obtain the super-type for this java type mapping.
	 */
	ManagedJavaTypeMapping<? super T> getSuperType();
}
