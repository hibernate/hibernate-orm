/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.junit5;

/**
 * todo (6.0) : need to move `DomainModel` to here from hibernate-core/src/test
 * 		- and rename it `DomainModelDescriptor` (avoid name clash)
 * 		- this annotation define `#value` as a would accept that `DomainModelDescriptor`
 *
 * @author Steve Ebersole
 */
public @interface DomainModel {
}
