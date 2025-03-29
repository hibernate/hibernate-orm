/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.querycache;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Gavin King
 */
public class Course implements Serializable {
	private String courseCode;
	private String description;
	private Set courseMeetings = new HashSet();

	public String getCourseCode() {
		return courseCode;
	}
	public void setCourseCode(String courseCode) {
		this.courseCode = courseCode;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Set getCourseMeetings() {
		return courseMeetings;
	}
	public void setCourseMeetings(Set courseMeetings) {
		this.courseMeetings = courseMeetings;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || ! ( o instanceof Course ) ) {
			return false;
		}

		Course course = ( Course ) o;

		if ( courseCode != null ? !courseCode.equals( course.getCourseCode() ) : course.getCourseCode() != null ) {
			return false;
		}
		if ( description != null ? !description.equals( course.getDescription() ) : course.getDescription() != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result = courseCode != null ? courseCode.hashCode() : 0;
		result = 31 * result + ( description != null ? description.hashCode() : 0 );
		return result;
	}
}
