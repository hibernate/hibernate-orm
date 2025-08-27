/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.fetchprofiles.join;


/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class Enrollment {
	private Long id;
	private CourseOffering offering;
	private Student student;
	private int finalGrade;

	public Enrollment() {
	}

	public Enrollment(CourseOffering offering, Student student) {
		this.offering = offering;
		this.student = student;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
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
