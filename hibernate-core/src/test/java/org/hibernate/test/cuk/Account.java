//$Id: Account.java 4592 2004-09-26 00:39:43Z oneovthafew $
package org.hibernate.test.cuk;

import java.io.Serializable;

/**
 * @author Gavin King
 */
public class Account implements Serializable {
	private String accountId;
	private Person user;
	private char type;
	/**
	 * @return Returns the user.
	 */
	public Person getUser() {
		return user;
	}
	/**
	 * @param user The user to set.
	 */
	public void setUser(Person user) {
		this.user = user;
	}
	/**
	 * @return Returns the accountId.
	 */
	public String getAccountId() {
		return accountId;
	}
	/**
	 * @param accountId The accountId to set.
	 */
	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}
	/**
	 * @return Returns the type.
	 */
	public char getType() {
		return type;
	}
	/**
	 * @param type The type to set.
	 */
	public void setType(char type) {
		this.type = type;
	}

}
