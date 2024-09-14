/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.inheritance.hhh16711.otherPackage;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class Inherited {
	@Id
	protected String id;
	protected String name;

	protected Inherited(String id, String name) {
		this.id = id;
		this.name = name;
	}

	protected Inherited() {
	}

	protected String getId() {
		return id;
	}

	protected String getName() {
		return name;
	}

	protected void setId(String id) {
		this.id = id;
	}

	protected void setName(String name) {
		this.name = name;
	}
}
