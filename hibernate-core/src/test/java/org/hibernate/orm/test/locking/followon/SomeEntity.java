/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.locking.followon;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Basic;
import jakarta.persistence.ManyToOne;

@Entity
public class SomeEntity {
	@Id
	private Integer id;
	@Basic
	private String name;
	@ManyToOne
	private DependentEntity dependent;

	protected SomeEntity() {
		// for Hibernate use
	}

	public SomeEntity(Integer id, String name, DependentEntity dependent) {
		this.id = id;
		this.name = name;
		this.dependent = dependent;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public DependentEntity getDependent() {
		return dependent;
	}

	public void setDependent(DependentEntity dependent) {
		this.dependent = dependent;
	}
}