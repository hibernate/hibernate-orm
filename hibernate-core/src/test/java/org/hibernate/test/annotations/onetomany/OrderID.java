//$Id$
package org.hibernate.test.annotations.onetomany;
import java.io.Serializable;
import javax.persistence.Column;

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
