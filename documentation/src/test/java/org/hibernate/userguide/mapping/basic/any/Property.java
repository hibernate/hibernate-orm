/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.basic.any;

//tag::mapping-column-any-property-example[]
public interface Property<T> {

    String getName();

    T getValue();
}
//end::mapping-column-any-property-example[]
