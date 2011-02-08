package org.hibernate.test.annotations.manytomany;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.ManyToMany;

@Embeddable
public class ContactInfo {
//	@ManyToOne
//	Address address; // Unidirectional

	List<PhoneNumber> phoneNumbers; // Bidirectional

	@ManyToMany(cascade= CascadeType.ALL)
	public List<PhoneNumber> getPhoneNumbers() {
		return phoneNumbers;
	}

	public void setPhoneNumbers(List<PhoneNumber> phoneNumbers) {
		this.phoneNumbers = phoneNumbers;
	}
	
}
