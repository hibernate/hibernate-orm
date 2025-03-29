/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytoonewithformula;
import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ManufacturerId implements Serializable {

	private static final long serialVersionUID = 1L;

	private Integer companyCode;

	private Integer manufacturerCode;

	public ManufacturerId(Integer companyCode, Integer manufacturerCode) {
		this.companyCode = companyCode;
		this.manufacturerCode = manufacturerCode;
	}

	public ManufacturerId() {
	}

	@Column(name = "MFG_COMPANY_CODE")
	public Integer getCompanyCode() {
		return companyCode;
	}

	public void setCompanyCode(Integer companyCode) {
		this.companyCode = companyCode;
	}

	@Column(name = "MFG_CODE")
	public Integer getManufacturerCode() {
		return manufacturerCode;
	}

	public void setManufacturerCode(Integer manufacturerCode) {
		this.manufacturerCode = manufacturerCode;
	}

}
