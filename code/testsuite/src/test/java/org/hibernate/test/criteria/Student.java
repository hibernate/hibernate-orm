//$Id: Student.java 9116 2006-01-23 21:21:01Z steveebersole $
package org.hibernate.test.criteria;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Gavin King
 */
public class Student {
	private long studentNumber;
	private String name;
	private Course preferredCourse;
	private Set enrolments = new HashSet();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getStudentNumber() {
		return studentNumber;
	}

	public void setStudentNumber(long studentNumber) {
		this.studentNumber = studentNumber;
	}

	public Course getPreferredCourse() {
		return preferredCourse;
	}

	public void setPreferredCourse(Course preferredCourse) {
		this.preferredCourse = preferredCourse;
	}

	public Set getEnrolments() {
		return enrolments;
	}

	public void setEnrolments(Set employments) {
		this.enrolments = employments;
	}
}
