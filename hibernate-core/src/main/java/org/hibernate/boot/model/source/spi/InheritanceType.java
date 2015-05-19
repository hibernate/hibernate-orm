/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

/**
 * The inheritance type for a given entity hierarchy
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
public enum InheritanceType {
	NO_INHERITANCE,
	DISCRIMINATED,
	JOINED,
	UNION;
}
