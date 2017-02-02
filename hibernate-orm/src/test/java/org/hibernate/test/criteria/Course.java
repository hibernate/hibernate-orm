/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Course.java 5686 2005-02-12 07:27:32Z steveebersole $
package org.hibernate.test.criteria;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Gavin King
 */
public class Course {
	private String courseCode;
	private String description;
	private Set courseMeetings = new HashSet();
	private Set crossListedAs;

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
	public Set getCrossListedAs() {
		return crossListedAs;
	}
	public void setCrossListedAs(Set crossListedAs) {
		this.crossListedAs = crossListedAs;
	}
}
