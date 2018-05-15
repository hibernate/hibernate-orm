/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.fetchprofiles.join;
import java.util.HashSet;
import java.util.Set;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class CourseOffering {
	private Long id;
	private Course course;
	private int semester;
	private int year;
	private Set enrollments = new HashSet();

	public CourseOffering() {
	}

	public CourseOffering(Course course, int semester, int year) {
		this.course = course;
		this.semester = semester;
		this.year = year;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
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

	public Set getEnrollments() {
		return enrollments;
	}

	public void setEnrollments(Set enrollments) {
		this.enrollments = enrollments;
	}
}
