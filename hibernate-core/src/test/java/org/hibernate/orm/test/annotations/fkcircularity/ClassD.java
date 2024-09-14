/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.fkcircularity;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/**
 * Test entities ANN-730.
 *
 * @author Hardy Ferentschik
 *
 */
@Entity
@Table(name = "class_1d")
@PrimaryKeyJoinColumn(name = "id", referencedColumnName = "id")
public class ClassD extends ClassC {
}
