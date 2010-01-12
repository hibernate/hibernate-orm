package org.hibernate.test.annotations.collectionelement;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import java.util.List;


@Embeddable
public class ContactInfo {
   @ManyToOne(targetEntity=Address.class, cascade=CascadeType.ALL)
	Address address;

	@ManyToMany(targetEntity=PhoneNumber.class, cascade=CascadeType.ALL)
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
