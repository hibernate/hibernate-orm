/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associationmanagement;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Steve Ebersole
 */
@Entity(name = "Student")
class Student {
	@Id
	Integer id;

	@ManyToMany
	@JoinTable(name = "student_course")
	Set<Course> courses = new HashSet<>();

	Student() {
	}

	Student(Integer id) {
		this.id = id;
	}
}
