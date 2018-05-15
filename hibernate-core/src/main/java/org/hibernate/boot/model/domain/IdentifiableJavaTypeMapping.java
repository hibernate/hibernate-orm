/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.domain;

/**
 * Contract for identifiable java type mappings, which is taken from the JPA term for commonality
 * between entity and "mapped superclass" types.
 *
 * @author Chris Cranford
 */
public interface IdentifiableJavaTypeMapping<T> extends ManagedJavaTypeMapping<T> {
	/**
	 * Overridden to further qualify the super-type as an identifiable-type.
	 */
	@Override
	IdentifiableJavaTypeMapping<? super T> getSuperType();
}
