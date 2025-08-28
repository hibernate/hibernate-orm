/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metadata;
import jakarta.persistence.Basic;
import jakarta.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class Address {

	private String address1;

	private String address2;
	private String city;

	@Basic(optional = true)
	public String getAddress1() {
		return address1;
	}

	public void setAddress1(String address1) {
		this.address1 = address1;
	}

	@Basic(optional = false)
	public String getAddress2() {
		return address2;
	}

	public void setAddress2(String address2) {
		this.address2 = address2;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}
}
