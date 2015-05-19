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

