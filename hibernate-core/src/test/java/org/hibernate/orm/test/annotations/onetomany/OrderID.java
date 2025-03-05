/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetomany;
import java.io.Serializable;
import jakarta.persistence.Column;

/**
 * @author Emmanuel Bernard
 */
public class OrderID implements Serializable {
	private String schoolId;
	private Integer academicYear;

	@Column( name = "Academic_Yr" )
	public Integer getAcademicYear() {
		return this.academicYear;
	}

	public void setAcademicYear(Integer academicYear) {
		this.academicYear = academicYear;
	}

	@Column( name = "School_Id" )
	public String getSchoolId() {
		return this.schoolId;
	}

	public void setSchoolId(String schoolId) {
		this.schoolId = schoolId;
	}
}
