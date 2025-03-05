/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.graphs;

import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
@NamedEntityGraphs({
	@NamedEntityGraph(
		name = "Student.Full",
		attributeNodes = {
			@NamedAttributeNode(value = Student_.COURSES)
		}
	)
})
@NamedQueries({
	@NamedQuery(name="LIST_OF_STD", query="select std from Student std")
})
public class Student {
	@Id
	private int id;

	private String name;

	@ManyToMany(cascade=CascadeType.PERSIST)
	@JoinTable(
		name="STUDENT_COURSES",
		joinColumns=@JoinColumn(referencedColumnName="ID", name="STUDENT_ID"),
		inverseJoinColumns=@JoinColumn(referencedColumnName="ID", name="COURSE_ID"),
		uniqueConstraints={@UniqueConstraint(columnNames={"STUDENT_ID", "COURSE_ID"})}
	)
	private Set<Course> courses;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Set<Course> getCourses() {
		return courses;
	}

	public void setCourses(Set<Course> courses) {
		this.courses = courses;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Student [name=" + name + "]";
	}
}
