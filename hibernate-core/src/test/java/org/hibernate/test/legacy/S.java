//$Id: S.java 4599 2004-09-26 05:18:27Z oneovthafew $

package org.hibernate.test.legacy;


public class S {
	private String address;
	private int count;

	public S(int countArg, String addressArg) {
		count = countArg;
		address = addressArg;
	}
	
	/**
	 * Gets the address
	 * @return Returns a String
	 */
	public String getAddress() {
		return address;
	}
	/**
	 * Sets the address
	 * @param address The address to set
	 */
	public void setAddress(String address) {
		this.address = address;
	}
	/**
	 * Gets the count
	 * @return Returns a int
	 */
	public int getCount() {
		return count;
	}
	/**
	 * Sets the count
	 * @param count The count to set
	 */
	public void setCount(int count) {
		this.count = count;
	}
}
