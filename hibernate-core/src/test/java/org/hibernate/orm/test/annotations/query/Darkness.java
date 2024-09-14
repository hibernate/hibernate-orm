/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.query;
import jakarta.persistence.MappedSuperclass;

@org.hibernate.annotations.NamedQuery(
		name = "night.olderThan",
		query = "select n from Night n where n.date <= :date"
)

@MappedSuperclass
public class Darkness {

}
