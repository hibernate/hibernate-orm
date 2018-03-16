/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jpa;


/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class MyEntity {
	private Long id;
	private String name;
	private String surname;
	private MyEntity other;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public MyEntity getOther() {
		return other;
	}

	public void setOther(MyEntity other) {
		this.other = other;
	}
}
