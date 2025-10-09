/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.fetchprofiles.join;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Steve Ebersole
 */
public class CourseOffering {
	private Integer id;
	private Course course;
	private int semester;
	private int year;
	private Set<Enrollment> enrollments = new HashSet<>();

	public CourseOffering() {
	}

	public CourseOffering(Integer id, Course course, int semester, int year) {
		this.id = id;
		this.course = course;
		this.semester = semester;
		this.year = year;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Course getCourse() {
		return course;
	}

	public void setCourse(Course course) {
		this.course = course;
	}

	public int getSemester() {
		return semester;
	}

	public void setSemester(int semester) {
		this.semester = semester;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public Set<Enrollment> getEnrollments() {
		return enrollments;
	}

	public void setEnrollments(Set<Enrollment> enrollments) {
		this.enrollments = enrollments;
	}
}
