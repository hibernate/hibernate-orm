/*
  * Hibernate, Relational Persistence for Idiomatic Java
  *
  * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-
  * party contributors as indicated by the @author tags or express 
  * copyright attribution statements applied by the authors.  
  * All third-party contributions are distributed under license by 
  * Red Hat, Inc.
  *
  * This copyrighted material is made available to anyone wishing to 
  * use, modify, copy, or redistribute it subject to the terms and 
  * conditions of the GNU Lesser General Public License, as published 
  * by the Free Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of 
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU 
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public 
  * License along with this distribution; if not, write to:
  * 
  * Free Software Foundation, Inc.
  * 51 Franklin Street, Fifth Floor
  * Boston, MA  02110-1301  USA
  */

package org.hibernate.test.annotations.manytoonewithformula;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinColumnsOrFormulas;
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
	@JoinColumnsOrFormulas( {
			@JoinColumnOrFormula(column = @JoinColumn(name = "CDT_MDL_CODE", referencedColumnName = "MDL_CODE")),
			@JoinColumnOrFormula(formula = @JoinFormula(value = "CDT_MFG_CODE", referencedColumnName = "MDL_MFG_CODE")),
			@JoinColumnOrFormula(formula = @JoinFormula(value = "CDT_COMPANY_CODE", referencedColumnName = "MDL_COMPANY_CODE"))})
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
	@JoinColumnsOrFormulas( {
			@JoinColumnOrFormula(column = @JoinColumn(name = "CDT_MFG_CODE", referencedColumnName = "MFG_CODE")),
			@JoinColumnOrFormula(formula = @JoinFormula(value = "CDT_COMPANY_CODE", referencedColumnName = "MFG_COMPANY_CODE")) })
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
