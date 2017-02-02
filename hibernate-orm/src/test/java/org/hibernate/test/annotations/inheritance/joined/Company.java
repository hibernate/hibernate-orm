/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.inheritance.joined;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;

/**
 * @author Sharath Reddy
 *
 */
@Entity
@Table(name = "Company")
@SecondaryTable(name = "CompanyAddress")
public class Company extends Customer {

	private String companyName;	
	private String companyAddress;

	@Column
	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	@Column(table = "CompanyAddress")
	public String getCompanyAddress() {
		return companyAddress;
	}

	public void setCompanyAddress(String companyAddress) {
		this.companyAddress = companyAddress;
	}

	
	
	
	
	
	
}
