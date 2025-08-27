/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.cascade;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import jakarta.persistence.Access;

@Entity
@Access(AccessType.FIELD)
public class Teacher {

	@Id @GeneratedValue
	Long id;

	String name;

	@OneToMany(mappedBy="primaryTeacher", cascade={CascadeType.MERGE, CascadeType.PERSIST})
	private Set<Student> students = new HashSet<Student>();

	@OneToOne(mappedBy="favoriteTeacher", cascade={CascadeType.MERGE, CascadeType.PERSIST})
	private Student favoriteStudent;

	public  Teacher() {
	}

	public Student getFavoriteStudent() {
		return favoriteStudent;
	}

	public void setFavoriteStudent(
			Student contributionOrBenefitParameters) {
		this.favoriteStudent = contributionOrBenefitParameters;
	}

	public Set<Student> getStudents() {
		return students;
	}

	public void setStudents(
			Set<Student> todoCollection) {
		this.students = todoCollection;
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
