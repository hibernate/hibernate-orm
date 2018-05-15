/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Course.java 6913 2005-05-25 17:37:51Z oneovthafew $
package org.hibernate.test.ecid;
import java.io.Serializable;

/**
 * @author Gavin King
 */
public class Course implements Serializable {
	
	private String courseCode;
	private String org;
	private String description;

	Course() {}
	Course(String courseCode, String org, String description) {
		this.courseCode = courseCode;
		this.org = org;
		this.description = description;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getCourseCode() {
		return courseCode;
	}
	
	public String getOrg() {
		return org;
	}

}
