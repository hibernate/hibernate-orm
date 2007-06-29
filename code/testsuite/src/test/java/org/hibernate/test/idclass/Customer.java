//$Id: Customer.java 7858 2005-08-11 21:46:58Z epbernard $

package org.hibernate.test.idclass;





public class Customer {



	public Customer() {

		super();

	}



	public Customer(String orgName, String custName, String add) {

		this.orgName = orgName;

		this.customerName = custName;

		this.address = add;

	}



	private String orgName;

	private String customerName;

	private String address;



	public String getAddress() {

		return address;

	}



	public void setAddress(String address) {

		this.address = address;

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

