package org.hibernate.test.annotations.override;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;


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
