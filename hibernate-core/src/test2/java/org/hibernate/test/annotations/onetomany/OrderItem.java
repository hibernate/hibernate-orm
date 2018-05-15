/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.onetomany;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table( name = "OrderItem_tbl" )
@IdClass( OrderItemID.class )
public class OrderItem {
	String schoolId;
	Integer academicYear;
	Integer dayNo;
	String dayName;
	private Order order;

	@Id
	public Integer getDayNo() {
		return dayNo;
	}

	public void setDayNo(Integer dayNo) {
		this.dayNo = dayNo;
	}

	@Id
	public String getSchoolId() {
		return schoolId;
	}

	public void setSchoolId(String schoolId) {
		this.schoolId = schoolId;
	}

	@Id
	public Integer getAcademicYear() {
		return academicYear;
	}

	public void setAcademicYear(Integer academicYear) {
		this.academicYear = academicYear;
	}

	@Column( name = "Day_Name" )
	public String getDayName() {
		return dayName;
	}

	public void setDayName(String dayName) {
		this.dayName = dayName;
	}

	@ManyToOne( fetch = FetchType.LAZY )
	@JoinColumns( {
	@JoinColumn( name = "School_Id", referencedColumnName = "School_Id", insertable = false, updatable = false ),
	@JoinColumn( name = "Academic_Yr", referencedColumnName = "Academic_Yr", insertable = false, updatable = false )
			} )
	public Order getOrder() {
		return this.order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}

}
