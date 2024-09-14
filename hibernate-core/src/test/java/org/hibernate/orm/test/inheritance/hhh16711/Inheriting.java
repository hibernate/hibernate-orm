/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.inheritance.hhh16711;

import org.hibernate.orm.test.inheritance.hhh16711.otherPackage.Inherited;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "inheriting")
class Inheriting extends Inherited {

	Inheriting(String id, String name) {
		super(id, name);
	}

	Inheriting() {
	}
}
