package org.hibernate.test.annotations.manytomany.defaults;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.ManyToMany;

import org.hibernate.test.annotations.manytomany.PhoneNumber;

@Embeddable
public class ContactInfo {
//	@ManyToOne
//	Address address; // Unidirectional

	List<org.hibernate.test.annotations.manytomany.PhoneNumber> phoneNumbers; // Bidirectional

	@ManyToMany(cascade= CascadeType.ALL)
	public List<org.hibernate.test.annotations.manytomany.PhoneNumber> getPhoneNumbers() {
		return phoneNumbers;
	}

	public void setPhoneNumbers(List<PhoneNumber> phoneNumbers) {
		this.phoneNumbers = phoneNumbers;
	}
	
}
