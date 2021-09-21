package org.hibernate.jpamodelgen.test.collectionbasictype.extras;

import java.io.Serializable;

/**
 * @author Vlad Mihalcea
 */
public class Phone implements Serializable {

	private String phoneNumber;

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
}
