//$Id$
package org.hibernate.test.annotations.onetomany;
import java.io.Serializable;
import javax.persistence.Column;

/**
 * @author Emmanuel Bernard
 */
public class OrderItemID implements Serializable {
	String schoolId;
	Integer academicYear;
	Integer dayNo;

	@Column( name = "Academic_Yr" )
	public Integer getAcademicYear() {
		return this.academicYear;
	}

	public void setAcademicYear(Integer academicYear) {
		this.academicYear = academicYear;
	}

	@Column( name = "Day_No" )
	public Integer getDayNo() {
		return this.dayNo;
	}

	public void setDayNo(Integer dayNo) {
		this.dayNo = dayNo;
	}

	@Column( name = "School_Id" )
	public String getSchoolId() {
		return this.schoolId;
	}

	public void setSchoolId(String schoolId) {
		this.schoolId = schoolId;
	}
}
