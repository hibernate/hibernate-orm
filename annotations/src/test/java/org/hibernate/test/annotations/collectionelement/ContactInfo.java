package org.hibernate.test.annotations.collectionelement;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import java.util.List;


@Embeddable
public class ContactInfo {
	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name="address_id_fk")
	Address address;

	@ManyToMany(cascade = CascadeType.ALL)
	List<PhoneNumber> phoneNumbers;

	@Embedded
	SocialTouchPoints social;

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
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
