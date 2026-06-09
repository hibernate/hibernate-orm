/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associationmanagement;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Steve Ebersole
 */
@Entity(name = "Course")
class Course {
	@Id
	Integer id;

	@ManyToMany(mappedBy = "courses")
	Set<Student> students = new HashSet<>();

	Course() {
	}

	Course(Integer id) {
		this.id = id;
	}
}
