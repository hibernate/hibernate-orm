/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetomany;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table( name = "Order_tbl" )
@IdClass( OrderID.class )
public class Order {
	private String schoolId;
	private Integer schoolIdSort;
	private Integer academicYear;

	private List<OrderItem> itemList = new ArrayList<OrderItem>();

	public boolean equals(Object obj) {
		return super.equals( obj );
	}

	public int hashCode() {
		return 10;
	}

	@Id
	public Integer getAcademicYear() {
		return this.academicYear;
	}

	protected void setAcademicYear(Integer academicYear) {
		this.academicYear = academicYear;
	}

	@Id
	public String getSchoolId() {
		return this.schoolId;
	}

	protected void setSchoolId(String schoolId) {
		this.schoolId = schoolId;
	}

	@OneToMany( mappedBy = "order" )
	@OrderBy( "dayNo desc" )
	public List<OrderItem> getItemList() {
		return this.itemList;
	}

	public void setItemList(List<OrderItem> itemList) {
		this.itemList = itemList;
	}

	public Integer getSchoolIdSort() {
		return this.schoolIdSort;
	}

	public void setSchoolIdSort(Integer schoolIdSort) {
		this.schoolIdSort = schoolIdSort;
	}


}
