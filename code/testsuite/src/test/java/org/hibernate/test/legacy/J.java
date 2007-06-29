//$Id: J.java 7203 2005-06-19 02:01:05Z oneovthafew $
package org.hibernate.test.legacy;

/**
 * @author Gavin King
 */
public class J extends I {
	private float amount;

	void setAmount(float amount) {
		this.amount = amount;
	}

	float getAmount() {
		return amount;
	}
}
