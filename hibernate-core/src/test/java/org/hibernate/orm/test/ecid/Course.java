/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ecid;
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
