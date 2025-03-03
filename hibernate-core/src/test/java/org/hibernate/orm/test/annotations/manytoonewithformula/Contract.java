/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytoonewithformula;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinFormula;

@Entity
@Table(name = "CONTRACT")
public class Contract {
	private String contractNumber;

	// private Integer companyCode;

	private ContractId id;

	private Manufacturer manufacturer;

	private Model model;

	public Contract() {
	}

	@Id
	public ContractId getId() {
		return id;
	}

	public void setId(ContractId id) {
		this.id = id;
	}

	@ManyToOne
	@JoinColumnOrFormula(column = @JoinColumn(name = "CDT_MDL_CODE", referencedColumnName = "MDL_CODE"))
	@JoinColumnOrFormula(formula = @JoinFormula(value = "CDT_MFG_CODE", referencedColumnName = "MDL_MFG_CODE"))
	@JoinColumnOrFormula(formula = @JoinFormula(value = "CDT_COMPANY_CODE", referencedColumnName = "MDL_COMPANY_CODE"))
	public Model getModel() {
		return model;
	}

	public void setModel(Model model) {
		this.model = model;
	}

	@Column(name = "CDT_CONTRACT_NBR")
	public String getContractNumber() {
		return contractNumber;
	}

	public void setContractNumber(String contractNumber) {
		this.contractNumber = contractNumber;
	}

	@ManyToOne
	@JoinColumnOrFormula(column = @JoinColumn(name = "CDT_MFG_CODE", referencedColumnName = "MFG_CODE"))
	@JoinColumnOrFormula(formula = @JoinFormula(value = "CDT_COMPANY_CODE", referencedColumnName = "MFG_COMPANY_CODE"))
	public Manufacturer getManufacturer() {
		return manufacturer;
	}

	public void setManufacturer(Manufacturer manufacturer) {
		this.manufacturer = manufacturer;
	}

	@Override
	public String toString() {
		return contractNumber;
	}

}
