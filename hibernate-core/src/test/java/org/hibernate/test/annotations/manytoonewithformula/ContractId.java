/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
