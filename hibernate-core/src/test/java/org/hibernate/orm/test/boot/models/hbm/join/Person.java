/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.boot.models.hbm.join;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Basic;
import jakarta.persistence.ManyToOne;

/**
 *
 * @author Steve Ebersole
 */
@Entity
public class Person {
	@Id
	private Integer id;
	@Basic
	private String name;
	private String stuff;
	@ManyToOne
	private SupplementalDetails details;
	@Embedded
	private Data data;
	private String datum;

	protected Person() {
		// for Hibernate use
	}

	public Person(Integer id, String name) {
		this.id = id;
		this.name = name;
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
}
