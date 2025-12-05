/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.fetchprofiles.join;

/**
 * @author Steve Ebersole
 */
public class Enrollment {
	private Integer id;
	private CourseOffering offering;
	private Student student;
	private int finalGrade;

	public Enrollment() {
	}

	public Enrollment(Integer id, CourseOffering offering, Student student) {
		this.id = id;
		this.offering = offering;
		this.student = student;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public CourseOffering getOffering() {
		return offering;
	}

	public void setOffering(CourseOffering offering) {
		this.offering = offering;
	}

	public Student getStudent() {
		return student;
	}

	public void setStudent(Student student) {
		this.student = student;
	}

	public int getFinalGrade() {
		return finalGrade;
	}

	public void setFinalGrade(int finalGrade) {
		this.finalGrade = finalGrade;
	}
}
