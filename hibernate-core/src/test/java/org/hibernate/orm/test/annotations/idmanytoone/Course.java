/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.idmanytoone;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * @author Alex Kalashnikov
 */
@Entity
@Table(name = "idmanytoone_course")
public class Course implements Serializable {

	@Id
	@GeneratedValue
	private int id;

	private String name;

	@OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
	private Set<CourseStudent> students = new HashSet<>();

	public Course() {
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<CourseStudent> getStudents() {
		return students;
	}

	public void setStudents(Set<CourseStudent> students) {
		this.students = students;
	}
}
