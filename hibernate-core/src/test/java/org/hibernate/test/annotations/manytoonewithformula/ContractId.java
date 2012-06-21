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
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class ContractId implements Serializable {

	private static final long serialVersionUID = 1L;

	private Integer companyCode;

	private Long contractNumber;

	private Integer contractSequenceNumber;

	public ContractId() {
	}

	public ContractId(Integer companyCode, Long contractNumber,
			Integer contractSequenceNumber) {
		this.companyCode = companyCode;
		this.contractNumber = contractNumber;
		this.contractSequenceNumber = contractSequenceNumber;
	}

	@Column(name = "CDT_COMPANY_CODE")
	public Integer getCompanyCode() {
		return companyCode;
	}

	public void setCompanyCode(Integer companyCode) {
		this.companyCode = companyCode;
	}

	@Column(name="CDT_NBR")
	public Long getContractNumber() {
		return contractNumber;
	}

	public void setContractNumber(Long contractNumber) {
		this.contractNumber = contractNumber;
	}

	@Column(name="CDT_SEQ_NBR")
	public Integer getContractSequenceNumber() {
		return contractSequenceNumber;
	}

	public void setContractSequenceNumber(Integer contractSequenceNumber) {
		this.contractSequenceNumber = contractSequenceNumber;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((companyCode == null) ? 0 : companyCode.hashCode());
		result = prime * result
				+ ((contractNumber == null) ? 0 : contractNumber.hashCode());
		result = prime
				* result
				+ ((contractSequenceNumber == null) ? 0
						: contractSequenceNumber.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ContractId other = (ContractId) obj;
		if (companyCode == null) {
			if (other.companyCode != null)
				return false;
		} else if (!companyCode.equals(other.companyCode))
			return false;
		if (contractNumber == null) {
			if (other.contractNumber != null)
				return false;
		} else if (!contractNumber.equals(other.contractNumber))
			return false;
		if (contractSequenceNumber == null) {
			if (other.contractSequenceNumber != null)
				return false;
		} else if (!contractSequenceNumber.equals(other.contractSequenceNumber))
			return false;
		return true;
	}

}
