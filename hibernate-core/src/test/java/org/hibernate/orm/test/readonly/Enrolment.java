/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.readonly;
import java.io.Serializable;

/**
 * @author Gavin King
 */
public class Enrolment implements Serializable {
	private Student student;
	private Course course;
	private long studentNumber;
	private String courseCode;
	private short year;
	private short semester;
	public String getCourseCode() {
		return courseCode;
	}
	public void setCourseCode(String courseId) {
		this.courseCode = courseId;
	}
	public long getStudentNumber() {
		return studentNumber;
	}
	public void setStudentNumber(long studentId) {
		this.studentNumber = studentId;
	}
	public Course getCourse() {
		return course;
	}
	public void setCourse(Course course) {
		this.course = course;
	}
	public Student getStudent() {
		return student;
	}
	public void setStudent(Student student) {
		this.student = student;
	}
	public short getSemester() {
		return semester;
	}
	public void setSemester(short semester) {
		this.semester = semester;
	}
	public short getYear() {
		return year;
	}
	public void setYear(short year) {
		this.year = year;
	}

	public boolean equals(Object other) {
		if ( !(other instanceof Enrolment) ) return false;
		Enrolment that = (Enrolment) other;
		return studentNumber==that.studentNumber &&
			courseCode.equals(that.courseCode);
	}

	public int hashCode() {
		return courseCode.hashCode();
	}
}
