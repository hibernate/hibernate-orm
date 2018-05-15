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
public class ModelId implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Integer companyCode;

	private Integer manufacturerCode;

	private String modelCode;

	public ModelId(Integer companyCode, Integer manufacturerCode,
			String modelCode) {
		this.companyCode = companyCode;
		this.manufacturerCode = manufacturerCode;
		this.modelCode = modelCode;
	}

	public ModelId() {
	}

	@Column(name = "MDL_COMPANY_CODE")
	public Integer getCompanyCode() {
		return companyCode;
	}

	public void setCompanyCode(Integer companyCode) {
		this.companyCode = companyCode;
	}

	@Column(name = "MDL_MFG_CODE")
	public Integer getManufacturerCode() {
		return manufacturerCode;
	}

	public void setManufacturerCode(Integer manufacturerCode) {
		this.manufacturerCode = manufacturerCode;
	}

	@Column(name = "MDL_CODE")
	public String getModelCode() {
		return modelCode;
	}

	public void setModelCode(String modelCode) {
		this.modelCode = modelCode;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((companyCode == null) ? 0 : companyCode.hashCode());
		result = prime
				* result
				+ ((manufacturerCode == null) ? 0 : manufacturerCode.hashCode());
		result = prime * result
				+ ((modelCode == null) ? 0 : modelCode.hashCode());
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
		ModelId other = (ModelId) obj;
		if (companyCode == null) {
			if (other.companyCode != null)
				return false;
		} else if (!companyCode.equals(other.companyCode))
			return false;
		if (manufacturerCode == null) {
			if (other.manufacturerCode != null)
				return false;
		} else if (!manufacturerCode.equals(other.manufacturerCode))
			return false;
		if (modelCode == null) {
			if (other.modelCode != null)
				return false;
		} else if (!modelCode.equals(other.modelCode))
			return false;
		return true;
	}

}

