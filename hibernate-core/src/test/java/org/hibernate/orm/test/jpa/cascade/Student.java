/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.cascade;

import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import jakarta.persistence.Access;

@Entity
@Access(AccessType.FIELD)
public class Student {

	@Id @GeneratedValue
	Long id;

	String name;

	@ManyToOne(cascade={CascadeType.MERGE, CascadeType.PERSIST})
	private Teacher primaryTeacher;

	@OneToOne(cascade={CascadeType.MERGE, CascadeType.PERSIST})
	private Teacher favoriteTeacher;

	public  Student() {
	}

	public Teacher getFavoriteTeacher() {
		return favoriteTeacher;
	}

	public void setFavoriteTeacher(Teacher lifeCover) {
		this.favoriteTeacher = lifeCover;
	}

	public Teacher getPrimaryTeacher() {
		return primaryTeacher;
	}

	public void setPrimaryTeacher(Teacher relativeTo) {
		this.primaryTeacher = relativeTo;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
