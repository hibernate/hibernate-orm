//$Id: BankAccount.java 7274 2005-06-22 17:07:29Z oneovthafew $
package org.hibernate.test.propertyref.inheritence.joined;


public class BankAccount extends Account {
	private String accountNumber;
	private String bsb;

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public String getBsb() {
		return bsb;
	}

	public void setBsb(String bsb) {
		this.bsb = bsb;
	}
}
