//$Id: CustomerId.java 7239 2005-06-20 09:44:54Z oneovthafew $
package org.hibernate.test.idclass;

import java.io.Serializable;

public class CustomerId implements Serializable {
	
	private String orgName;
	private String customerName;

	public CustomerId() {
		super();
	}

	public CustomerId(String orgName, String custName) {
		this.orgName = orgName;
		this.customerName = custName;
	}

	public String getCustomerName() {
		return customerName;
	}

	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	public String getOrgName() {
		return orgName;
	}

	public void setOrgName(String orgName) {
		this.orgName = orgName;
	}

}
