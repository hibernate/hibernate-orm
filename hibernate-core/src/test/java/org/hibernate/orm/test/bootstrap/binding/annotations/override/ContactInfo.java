/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.override;

import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;


@Embeddable
public class ContactInfo {
	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name="address_id_fk")
	Addr address;

	@ManyToMany(cascade = CascadeType.ALL)
	List<PhoneNumber> phoneNumbers;

	@Embedded
	SocialTouchPoints social;

	public Addr getAddress() {
		return address;
	}

	public void setAddr(Addr address) {
		this.address = address;
	}

	public List<PhoneNumber> getPhoneNumbers() {
		return phoneNumbers;
	}

	public void setPhoneNumbers(List<PhoneNumber> phoneNumbers) {
		this.phoneNumbers = phoneNumbers;
	}

	public SocialTouchPoints getSocial() {
		return social;
	}

	public void setSocial(SocialTouchPoints social) {
		this.social = social;
	}

}
