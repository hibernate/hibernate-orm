/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

/**
 * Type of available polymorphism for a particular entity.
 *
 * @author Emmanuel Bernard
 */
public enum PolymorphismType {
	/**
	 * This entity is retrieved if any of its super entity are retrieved.  The default,
	 */
	IMPLICIT,
	/**
	 * This entity is retrieved only if explicitly asked.
	 */
	EXPLICIT
}
