/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.annotations.inheritance.joined;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;

/**
 * @author Sharath Reddy
 *
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "Customer")
@SecondaryTable(name = "CustomerDetails")
public class Customer extends LegalEntity {

	public String customerName;
	public String customerCode;

	@Column
	public String getCustomerName() {
		return customerName;
	}

	public void setCustomerName(String val) {
		this.customerName = val;
	}

	@Column(table="CustomerDetails")
	public String getCustomerCode() {
		return customerCode;
	}

	public void setCustomerCode(String val) {
		this.customerCode = val;
	}
}
