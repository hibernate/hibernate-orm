//$Id: Account.java 7274 2005-06-22 17:07:29Z oneovthafew $
package org.hibernate.test.propertyref.inheritence.joined;
import java.io.Serializable;

/**
 * @author Gavin King
 */
public class Account implements Serializable {
	private String accountId;
	private char type;

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
