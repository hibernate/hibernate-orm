/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.filter.subclass.joined2;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "dogs")
@PrimaryKeyJoinColumn(name = "id_dog", referencedColumnName = "id_animal")
public class Dog extends Animal {
    private String breed;
	@ManyToOne @JoinColumn(name = "id_owner")
    private Owner owner;
}
